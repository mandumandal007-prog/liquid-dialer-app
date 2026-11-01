package com.mandu.liquiddialer.widget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.R
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Favorites widget ka slot-selection + tap-intent logic (pure, seedha testable).
 *  Number wahi hota hai jo app ka contacts tab choose karta hai (super-primary preference),
 *  aur tap sirf dialer me number BHARTA hai - kabhi call nahi karta. */
object FavWidgetModel {
    const val MAX_SLOTS = 4

    sealed class Slot {
        /** number pakka resolved (single number ya explicit primary) - tap seedha fill karta hai. */
        data class Person(val contact: Contact, val number: String) : Slot()
        /** multi-number bina explicit primary - tap app ke andar number chooser kholta hai, kabhi silent first-row fill nahi. */
        data class Chooser(val contact: Contact) : Slot()
        object NeedPermission : Slot()
        object Empty : Slot()
    }

    /** resolved[contactId] = fill karne wala number; null/gaayab = chooser kholo.
     *  Number kabhi bhi sorted-list ki first row se silently nahi liya jata. */
    fun slots(hasContactsPermission: Boolean, favourites: List<Contact>, resolved: Map<Long, String?>): List<Slot> = when {
        !hasContactsPermission -> listOf(Slot.NeedPermission)
        favourites.isEmpty() -> listOf(Slot.Empty)
        else -> favourites.take(MAX_SLOTS).map { c ->
            val n = resolved[c.id]
            if (n != null) Slot.Person(c, n) else Slot.Chooser(c)
        }
    }

    /** Phones list se canonical fill number: explicit super-primary > single number > null (chooser). */
    fun resolveNumber(phones: List<Pair<String, Boolean>>): String? {
        val superP = phones.firstOrNull { it.second }?.first
        if (superP != null) return superP
        return if (phones.size == 1) phones[0].first else null
    }

    /** Fill-only tap: ACTION_DIAL (dialer me number bharta hai). Hint/permission slot: app ka contacts tab. */
    fun rawIntent(ctx: Context, slot: Slot): Intent = when (slot) {
        is Slot.Person -> Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_DIAL)
            .putExtra("number", slot.number)
            .putExtra("numberContactId", slot.contact.id)
            .putExtra("numberName", slot.contact.name)
        is Slot.Chooser -> Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .putExtra("chooseNumber", slot.contact.id)
            .putExtra("chooseName", slot.contact.name)
        else -> Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .putExtra("tab", "contacts")
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    /** Har slot + har widget instance ka alag request code - PendingIntents kabhi collide nahi hote. */
    fun requestCode(appWidgetId: Int, index: Int): Int = appWidgetId * 16 + index

    fun pendingIntent(ctx: Context, slot: Slot, appWidgetId: Int, index: Int): PendingIntent =
        PendingIntent.getActivity(ctx, requestCode(appWidgetId, index), rawIntent(ctx, slot),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun labelFor(slot: Slot): String = when (slot) {
        is Slot.Person -> firstName(slot.contact.name)
        is Slot.Chooser -> firstName(slot.contact.name)
        Slot.NeedPermission -> "Contacts permission do"
        Slot.Empty -> "Favorites add karo"
    }

    private fun firstName(name: String) = name.split(" ").firstOrNull()?.ifBlank { name } ?: name
}

class FavWidgetProvider : AppWidgetProvider() {

    /** System update broadcast: goAsync process ko zinda rakhta hai jab tak IO + updateAppWidget
     *  poora na ho (warna receiver return karte hi cold process mar sakta hai).
     *  Unit test me broadcast ke bahar goAsync null deta hai -> wahi kaam inline chalta hai. */
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (ids != null) {
                val pr = goAsync()
                val appCtx = ctx.applicationContext
                val mgr = AppWidgetManager.getInstance(appCtx)
                val work = {
                    for (id in ids) try { mgr.updateAppWidget(id, buildViews(appCtx, id)) } catch (_: Exception) { }
                    pr?.finish()
                }
                if (pr != null) LdApp.instance.scope.launch(Dispatchers.IO) { work() } else work()
                return
            }
        }
        super.onReceive(ctx, intent)
    }

    companion object {
        private val SLOT_IDS = listOf(R.id.wslot1, R.id.wslot2, R.id.wslot3, R.id.wslot4)
        private val IMG_IDS = listOf(R.id.wimg1, R.id.wimg2, R.id.wimg3, R.id.wimg4)
        private val LABEL_IDS = listOf(R.id.wlabel1, R.id.wlabel2, R.id.wlabel3, R.id.wlabel4)

        /** App ke andar favorites/contact badalne par refresh (process pehle se zinda hai).
         *  Koi widget nahi to turant no-op. */
        fun refresh(ctx: Context) {
            val mgr = try { AppWidgetManager.getInstance(ctx) } catch (_: Exception) { return }
            val ids = try { mgr.getAppWidgetIds(ComponentName(ctx, FavWidgetProvider::class.java)) } catch (_: Exception) { return }
            if (ids.isEmpty()) return
            ids.forEach { updateOne(ctx, mgr, it) }
        }

        fun updateOne(ctx: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val appCtx = ctx.applicationContext
            LdApp.instance.scope.launch(Dispatchers.IO) {
                val rv = try { buildViews(appCtx, appWidgetId) } catch (_: Exception) { return@launch }
                try { mgr.updateAppWidget(appWidgetId, rv) } catch (_: Exception) { }
            }
        }

        /** Permission + contacts query yahin hoti hai; binding bindViews ko di jaati hai. */
        fun buildViews(ctx: Context, appWidgetId: Int): RemoteViews {
            val hasPerm = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            val favs = if (hasPerm) try {
                Repo.contacts(ctx, LdApp.settings.value.sortByLast).filter { it.starred }
            } catch (_: Exception) { emptyList() } else emptyList()
            // Missing-number/deleted favorite skip (kabhi blank ya purana number fill nahi)
            val resolved = HashMap<Long, String?>()
            val usable = favs.filter { c ->
                val phones = try { Repo.phonesOf(ctx, c.id) } catch (_: Exception) { emptyList() }
                if (phones.isEmpty()) false.also { } else { resolved[c.id] = FavWidgetModel.resolveNumber(phones); true }
            }
            return bindViews(ctx, appWidgetId, FavWidgetModel.slots(hasPerm, usable, resolved), LdApp.settings.value.accent.toInt())
        }

        /** Har call saaf RemoteViews banati hai: har slot ka visibility + visible slot ka text/image/click
         *  hamesha set hota hai, isliye permission revoke ya favorite delete hone par purana
         *  naam/photo/number kabhi retain nahi hota. */
        internal fun bindViews(ctx: Context, appWidgetId: Int, slots: List<FavWidgetModel.Slot>, accent: Int): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_fav)
            for (i in 0 until FavWidgetModel.MAX_SLOTS) {
                if (i >= slots.size) {
                    // GONE slot par bhi purana naam/photo/tap poore tarah saaf (reapply replay par stale content zero)
                    rv.setViewVisibility(SLOT_IDS[i], View.GONE)
                    rv.setTextViewText(LABEL_IDS[i], "")
                    rv.setImageViewBitmap(IMG_IDS[i], null)
                    rv.setOnClickPendingIntent(SLOT_IDS[i], null)
                    continue
                }
                val slot = slots[i]
                rv.setViewVisibility(SLOT_IDS[i], View.VISIBLE)
                rv.setTextViewText(LABEL_IDS[i], FavWidgetModel.labelFor(slot))
                rv.setImageViewBitmap(IMG_IDS[i], when (slot) {
                    is FavWidgetModel.Slot.Person -> avatar(ctx, slot.contact, accent)
                    is FavWidgetModel.Slot.Chooser -> avatar(ctx, slot.contact, accent)
                    FavWidgetModel.Slot.NeedPermission -> glyph("+", 0xFF8A93A6.toInt())
                    FavWidgetModel.Slot.Empty -> glyph("+", accent)
                })
                rv.setOnClickPendingIntent(SLOT_IDS[i], FavWidgetModel.pendingIntent(ctx, slot, appWidgetId, i))
            }
            return rv
        }

        internal fun avatar(ctx: Context, c: Contact, accent: Int, size: Int = 96): Bitmap {
            val photo = c.photo?.let { p ->
                try {
                    ctx.contentResolver.openInputStream(Uri.parse(p))?.use { ins -> BitmapFactory.decodeStream(ins) }
                } catch (_: Exception) { null }
            }
            return if (photo != null) circle(Bitmap.createScaledBitmap(photo, size, size, true))
            else glyph(c.name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "P", accent, size)
        }

        internal fun circle(b: Bitmap): Bitmap {
            val out = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            c.drawCircle(b.width / 2f, b.height / 2f, b.width / 2f, p)
            p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            c.drawBitmap(b, 0f, 0f, p)
            return out
        }

        internal fun glyph(text: String, color: Int, size: Int = 96): Bitmap {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.color = color
            c.drawCircle(size / 2f, size / 2f, size / 2f, p)
            p.color = android.graphics.Color.WHITE
            p.textSize = size * 0.5f
            p.textAlign = Paint.Align.CENTER
            p.isFakeBoldText = true
            val y = size / 2f - (p.descent() + p.ascent()) / 2f
            c.drawText(text, size / 2f, y, p)
            return bmp
        }
    }
}
