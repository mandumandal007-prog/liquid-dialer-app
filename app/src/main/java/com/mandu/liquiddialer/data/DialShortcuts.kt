package com.mandu.liquiddialer.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.mandu.liquiddialer.MainActivity

/** Home screen par ek number ka seedha call button (pinned launcher shortcut).
 *  User khud number details se banata hai - kuch apne aap nahi banta.
 *  Button dabana = usi number ko call: wahi CALLBACK raasta jo reminder
 *  notification ke "Call karo" action ka hai (user ka explicit tap hi call karta hai). */
object DialShortcuts {

    const val CALLBACK_ACTION = "com.mandu.liquiddialer.CALLBACK"

    fun supported(ctx: Context): Boolean =
        try { ShortcutManagerCompat.isRequestPinShortcutSupported(ctx) } catch (e: Exception) { false }

    fun displayName(number: String, name: String): String =
        if (name.isNotBlank()) name else Repo.pretty(number)

    fun info(ctx: Context, number: String, name: String): ShortcutInfoCompat {
        val label = displayName(number, name)
        val intent = Intent(ctx, MainActivity::class.java)
            .setAction(CALLBACK_ACTION)
            .putExtra("number", number)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return ShortcutInfoCompat.Builder(ctx, "dial_" + Repo.key(number))
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithBitmap(initialIcon(label)))
            .setIntent(intent)
            .build()
    }

    /** true = launcher ne request accept ki. false = number khaali / launcher support nahi / fail. */
    fun pin(ctx: Context, number: String, name: String): Boolean {
        if (number.isBlank() || !supported(ctx)) return false
        return try { ShortcutManagerCompat.requestPinShortcut(ctx, info(ctx, number, name), null) } catch (e: Exception) { false }
    }

    /** Naam ke pehle akshar wala gol green button - launcher par turant pehchan. */
    internal fun initialIcon(label: String): Bitmap {
        val size = 108
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFF3DDC84.toInt()
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        p.color = Color.WHITE
        p.textSize = size * 0.52f
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        val ch = (label.firstOrNull { it.isLetterOrDigit() } ?: 'P').uppercase()
        val y = size / 2f - (p.descent() + p.ascent()) / 2
        c.drawText(ch, size / 2f, y, p)
        return bmp
    }
}
