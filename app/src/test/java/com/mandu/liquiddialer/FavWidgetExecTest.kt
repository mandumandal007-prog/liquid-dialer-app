package com.mandu.liquiddialer

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.data.ldStore
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.widget.FavWidgetModel
import com.mandu.liquiddialer.widget.FavWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver

/** Widget execution-gate: asli RemoteViews bind/apply, provider -> AppWidgetManager pipeline,
 *  stale content clearing, canonical number rule, aur MainActivity fill path (cold + warm). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FavWidgetExecTest {

    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()
    private val app get() = ApplicationProvider.getApplicationContext<LdApp>()
    private fun c(id: Long, name: String, number: String) =
        Contact(id, "lk$id", name, "", number, "mobile", true, null)

    private fun texts(v: View): List<String> = buildList {
        fun walk(x: View) {
            if (x is TextView && x.text.isNotBlank()) add(x.text.toString())
            if (x is ViewGroup) for (i in 0 until x.childCount) walk(x.getChildAt(i))
        }
        walk(v)
    }

    private fun visibilityOf(v: View, id: Int): Int = v.findViewById<View>(id).visibility

    /** Fake contacts provider: controlled Phone rows (Repo.contacts projection order). */
    private class FakePhonesProvider(private val rows: List<Array<Any?>>) : ContentProvider() {
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            if (uri != ContactsContract.CommonDataKinds.Phone.CONTENT_URI) return null
            val cols = projection ?: arrayOf()
            val mc = MatrixCursor(cols)
            val onlyId = selectionArgs?.firstOrNull()?.toLongOrNull()
            rows.filter { onlyId == null || it[0] == onlyId }.forEach { r ->
                // projection ke hisaab se columns map karo (Phone query projection order alag ho sakta hai)
                val full = mapOf("_id" to r[0], "contact_id" to r[0], "lookup" to r[1], "display_name" to r[2], "display_name_alt" to r[3],
                    "data1" to r[4], "data2" to r[5], "data3" to r[6], "starred" to r[7], "photo_thumb_uri" to r[8], "is_super_primary" to r[9])
                mc.addRow(cols.map { c -> full[c] })
            }
            return mc
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun getType(uri: Uri): String? = null
    }

    private fun seedPhones(rows: List<Array<Any?>>) {
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY, FakePhonesProvider(rows))
    }

    /** Repo.contacts projection order: id, lookup, name, altName, number, type, label, starred, photo, superPrimary */
    private fun row(id: Long, name: String, number: String, starred: Int, superPrimary: Int): Array<Any?> =
        arrayOf(id, "lk$id", name, name, number, 2, null, starred, null, superPrimary)

    @Test fun canonicalNumberRuleMatchesContactsTab() {
        seedPhones(listOf(
            row(1, "Asha Rani", "1111111111", 1, 0),
            row(1, "Asha Rani", "2222222222", 1, 1),  // super-primary baad me aaye tab bhi yahi chune
            row(2, "Bittu Kumar", "3333333333", 1, 0),
            row(2, "Bittu Kumar", "4444444444", 1, 0),  // koi super-primary nahi -> contacts-tab wala first row
            row(3, "Chotu", "5555555555", 1, 0),
            row(4, "NotFav", "9999999999", 0, 0),      // unstarred -> widget me nahi
        ))
        val contacts = Repo.contacts(ctx, false)
        assertEquals("2222222222", contacts.first { it.id == 1L }.number)
        assertEquals("3333333333", contacts.first { it.id == 2L }.number)
        // Canonical rule: super-primary direct; single direct; multi bina primary = chooser (kabhi silent first-row nahi)
        val favs = contacts.filter { it.starred }
        val resolved = favs.associate { f -> f.id to FavWidgetModel.resolveNumber(Repo.phonesOf(ctx, f.id)) }
        val slots = FavWidgetModel.slots(true, favs, resolved)
        assertEquals(3, slots.size)
        assertTrue(slots[0] is FavWidgetModel.Slot.Person)
        assertEquals("2222222222", (slots[0] as FavWidgetModel.Slot.Person).number)
        assertTrue("Bittu (2 numbers, koi primary nahi) chooser hona chahiye", slots[1] is FavWidgetModel.Slot.Chooser)
        assertTrue(slots[2] is FavWidgetModel.Slot.Person)
        assertEquals("5555555555", (slots[2] as FavWidgetModel.Slot.Person).number)
    }

    @Test fun remoteViewsBindApplyMatchesModelData() {
        val rv = FavWidgetProvider.bindViews(ctx, 7,
            listOf(FavWidgetModel.Slot.Person(c(1, "Asha Rani", "111"), "111"), FavWidgetModel.Slot.Person(c(2, "Bittu", "222"), "222")),
            0xFF3D7BFF.toInt())
        val applied = rv.apply(ctx, FrameLayout(ctx))
        assertEquals(listOf("Asha", "Bittu"), texts(applied))
        assertEquals(View.VISIBLE, visibilityOf(applied, R.id.wslot1))
        assertEquals(View.VISIBLE, visibilityOf(applied, R.id.wslot2))
        assertEquals(View.GONE, visibilityOf(applied, R.id.wslot3))
        assertEquals(View.GONE, visibilityOf(applied, R.id.wslot4))
        assertNotNull((applied.findViewById<ImageView>(R.id.wimg1)).drawable)
    }

    @Test fun rebindClearsStaleNamesAndNumbers() {
        val parent = FrameLayout(ctx)
        val first = FavWidgetProvider.bindViews(ctx, 7, listOf(FavWidgetModel.Slot.Person(c(1, "Asha Rani", "111"), "111")), 0xFF3D7BFF.toInt())
        first.apply(ctx, parent)
        // Favorite delete / permission revoke ke baad nayi RemoteViews:
        val second = FavWidgetProvider.bindViews(ctx, 7, listOf(FavWidgetModel.Slot.NeedPermission), 0xFF3D7BFF.toInt())
        val applied = second.apply(ctx, parent)
        assertEquals(listOf("Contacts permission do"), texts(applied))
        assertFalse(texts(applied).any { it.contains("Asha") })
        assertEquals(View.GONE, visibilityOf(applied, R.id.wslot2))
    }

    @Test fun providerPipelineEndToEndAndStaleClearing() {
        shadowOf(app).grantPermissions(android.Manifest.permission.READ_CONTACTS)
        seedPhones(listOf(
            row(1, "Asha Rani", "2222222222", 1, 1),
            row(2, "Bittu Kumar", "3333333333", 1, 0),
        ))
        val mgr = AppWidgetManager.getInstance(ctx)
        val shadow = shadowOf(mgr)
        val wid = shadow.createWidget(FavWidgetProvider::class.java, R.layout.widget_fav)

        // Cold-process style system broadcast -> provider -> Repo -> RemoteViews -> manager
        FavWidgetProvider().onReceive(ctx, Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(wid)))
        val v1 = shadow.getViewFor(wid)
        assertEquals(listOf("Asha", "Bittu"), texts(v1))

        // Favorites hatane ke baad refresh: purane naam retain nahi hote
        seedPhones(emptyList())
        FavWidgetProvider().onReceive(ctx, Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(wid)))
        val v2 = shadow.getViewFor(wid)
        assertEquals(listOf("Favorites add karo"), texts(v2))

        // Permission revoke: pehle seed dobara, phir deny -> hint, koi naam nahi
        seedPhones(listOf(row(1, "Asha Rani", "2222222222", 1, 1)))
        FavWidgetProvider().onReceive(ctx, Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(wid)))
        assertEquals(listOf("Asha"), texts(shadow.getViewFor(wid)))
        shadowOf(app).denyPermissions(android.Manifest.permission.READ_CONTACTS)
        FavWidgetProvider().onReceive(ctx, Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(wid)))
        val v3 = shadow.getViewFor(wid)
        assertEquals(listOf("Contacts permission do"), texts(v3))
        assertFalse(texts(v3).any { it.contains("Asha") })
    }

    @Test fun widgetIntentsExposeNoUriAndNoGrants() {
        val p = FavWidgetModel.rawIntent(ctx, FavWidgetModel.Slot.Person(c(1, "Asha", "+91 98765 43210"), "+91 98765 43210"))
        assertNull(p.data)
        assertEquals(0, p.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        val h = FavWidgetModel.rawIntent(ctx, FavWidgetModel.Slot.Empty)
        assertNull(h.data)
        assertEquals(0, h.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
    }

}
