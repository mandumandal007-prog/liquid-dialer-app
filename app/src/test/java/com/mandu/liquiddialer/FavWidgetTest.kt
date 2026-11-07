package com.mandu.liquiddialer

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.widget.FavWidgetModel
import com.mandu.liquiddialer.widget.FavWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Favorites widget: slot logic (pure), fill-only tap intents, distinct PendingIntents,
 *  manifest registration, aur day-mode pixel shots. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FavWidgetTest {

    private fun c(id: Long, name: String, number: String) =
        Contact(id, "lk$id", name, "", number, "mobile", true, null)

    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun slotsNeedPermissionWhenRevoked() {
        val slots = FavWidgetModel.slots(false, listOf(c(1, "Asha", "111")), emptyMap())
        assertEquals(listOf(FavWidgetModel.Slot.NeedPermission), slots)
    }

    @Test fun slotsEmptyHintWhenNoFavorites() {
        assertEquals(listOf(FavWidgetModel.Slot.Empty), FavWidgetModel.slots(true, emptyList(), emptyMap()))
    }

    @Test fun slotsCapAt4AndPreserveOrder() {
        val favs = (1..7).map { c(it.toLong(), "F$it", "900000000$it") }
        val slots = FavWidgetModel.slots(true, favs, favs.associate { it.id to it.number })
        assertEquals(4, slots.size)
        assertEquals(listOf("F1", "F2", "F3", "F4"), slots.map { (it as FavWidgetModel.Slot.Person).contact.name })
    }

    @Test fun slotsFewerThan4() {
        val favs = listOf(c(1, "Asha Rani", "111"), c(2, "Bittu", "222"))
        val slots = FavWidgetModel.slots(true, favs, favs.associate { it.id to it.number })
        assertEquals(2, slots.size)
        assertEquals("Asha", FavWidgetModel.labelFor(slots[0]))
        assertEquals("Bittu", FavWidgetModel.labelFor(slots[1]))
    }

    @Test fun personTapUsesValidatedWidgetCallAction() {
        val slot = FavWidgetModel.Slot.Person(c(1, "Asha", "+91 98765 43210"), "+91 98765 43210")
        val i = FavWidgetModel.rawIntent(ctx, slot)
        assertEquals(FavWidgetModel.WIDGET_CALL, i.action)
        assertEquals("+91 98765 43210", i.getStringExtra("number"))
        assertNotEquals(Intent.ACTION_CALL, i.action)
        assertEquals(ComponentName(ctx, MainActivity::class.java), i.component)
    }

    @Test fun hintTapOpensContactsTab() {
        val i = FavWidgetModel.rawIntent(ctx, FavWidgetModel.Slot.Empty)
        assertEquals("contacts", i.getStringExtra("tab"))
        assertEquals(Intent.ACTION_MAIN, i.action)
        assertFalse(i.hasExtra("number"))
    }

    @Test fun requestCodesDistinctPerSlotAndWidgetInstance() {
        val codes = mutableSetOf<Int>()
        for (id in listOf(3, 17, 42)) for (idx in 0..3) assertTrue(codes.add(FavWidgetModel.requestCode(id, idx)))
        assertNotEquals(FavWidgetModel.requestCode(1, 0), FavWidgetModel.requestCode(1, 1))
        assertNotEquals(FavWidgetModel.requestCode(1, 0), FavWidgetModel.requestCode(2, 0))
    }

    @Test fun labelForHintsAndLongNames() {
        assertEquals("Contacts permission do", FavWidgetModel.labelFor(FavWidgetModel.Slot.NeedPermission))
        assertEquals("Favorites add karo", FavWidgetModel.labelFor(FavWidgetModel.Slot.Empty))
        assertEquals("Durgeshwari", FavWidgetModel.labelFor(FavWidgetModel.Slot.Person(c(1, "Durgeshwari Prasad Yadav", "1"), "1")))
    }

    @Test fun manifestRegistersWidgetProvider() {
        val pm = ctx.packageManager
        val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_RECEIVERS)
        assertTrue((info.receivers ?: emptyArray()).any { it.name == FavWidgetProvider::class.java.name })
        val i = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .setComponent(ComponentName(ctx, FavWidgetProvider::class.java))
        val resolved = pm.queryBroadcastReceivers(i, 0)
        assertTrue("APPWIDGET_UPDATE receiver resolve hona chahiye", resolved.isNotEmpty())
    }

    @Test fun resolveNumberRules() {
        assertEquals("222", FavWidgetModel.resolveNumber(listOf("111" to false, "222" to true)))
        assertEquals("111", FavWidgetModel.resolveNumber(listOf("111" to false)))
        assertEquals(null, FavWidgetModel.resolveNumber(listOf("111" to false, "222" to false)))
        assertEquals(null, FavWidgetModel.resolveNumber(emptyList()))
    }

    @Test fun multiNumberWithoutPrimaryGetsChooserNeverSilentFirstRow() {
        val fav = c(9, "Rahul Multi", "111")
        val slots = FavWidgetModel.slots(true, listOf(fav), mapOf(9L to null))
        assertTrue(slots[0] is FavWidgetModel.Slot.Chooser)
        val i = FavWidgetModel.rawIntent(ctx, slots[0])
        assertEquals(9L, i.getLongExtra("chooseNumber", -1))
        assertFalse("chooser kabhi number pre-fill nahi karta", i.hasExtra("number"))
        assertNotEquals(Intent.ACTION_CALL, i.action)
        assertNull(i.data)
    }

    @Test fun missingResolvedEntryAlsoChooses() {
        val slots = FavWidgetModel.slots(true, listOf(c(5, "No Map", "111")), emptyMap())
        assertTrue(slots[0] is FavWidgetModel.Slot.Chooser)
    }

    // ---- MainActivity.routeIntent: pure intent decisions ----

    @Test fun routeDialWithNumberFillsNeverCalls() {
        val r = MainActivity.routeIntent(Intent(Intent.ACTION_DIAL).putExtra("number", "+91 98765"))
        assertEquals("+91 98765", r.fill)
        assertEquals("dialer", r.tab)
        assertNull(r.call)
    }

    @Test fun routeViewTelUriFills() {
        val r = MainActivity.routeIntent(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("tel:9876543210")))
        assertEquals("9876543210", r.fill)
        assertNull(r.call)
    }

    @Test fun routeDialWithoutNumberJustOpensDialer() {
        val r = MainActivity.routeIntent(Intent(Intent.ACTION_DIAL))
        assertNull(r.fill)
        assertEquals("dialer", r.tab)
    }

    @Test fun routeTabExtraAndChooser() {
        val r = MainActivity.routeIntent(Intent(Intent.ACTION_MAIN).putExtra("tab", "contacts"))
        assertEquals("contacts", r.tab)
        assertNull(r.fill)
        val rc = MainActivity.routeIntent(Intent(Intent.ACTION_MAIN).putExtra("chooseNumber", 42L))
        assertEquals(42L, rc.chooseNumberFor)
        assertNull("chooser kabhi pre-fill nahi", rc.fill)
        assertNull(rc.call)
        assertNull(rc.tab)
    }

    @Test fun routeCallActionsPreservedButWidgetNeverEmits() {
        // Purana behavior intact: explicit ACTION_CALL/CALLBACK hi call route dete hain
        assertEquals("123", MainActivity.routeIntent(Intent(Intent.ACTION_CALL).putExtra("number", "123")).call)
        assertEquals("123", MainActivity.routeIntent(Intent("com.mandu.liquiddialer.CALLBACK").putExtra("number", "123")).call)
        // Widget ke teeno slot-intents me se koi bhi call route produce nahi karta
        val ctx2 = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (slot in listOf(FavWidgetModel.Slot.Person(c(1, "Asha", "111"), "111"),
            FavWidgetModel.Slot.Chooser(c(2, "Bittu", "222")),
            FavWidgetModel.Slot.Empty, FavWidgetModel.Slot.NeedPermission)) {
            val r = MainActivity.routeIntent(FavWidgetModel.rawIntent(ctx2, slot))
            assertNull("widget intent kabhi call route nahi: $slot", r.call)
        }
    }

    @Test fun fillValidationPreservesFullNumberIdentity() {
        assertEquals(MainActivity.FillReval.Choose,
            MainActivity.revalidateFill("+91 90000 11111", listOf("+44 90000 11111" to false)))
        assertEquals(MainActivity.FillReval.Choose,
            MainActivity.revalidateFill("", listOf("" to false)))
        assertEquals(MainActivity.FillReval.Choose,
            MainActivity.revalidateFill("123#", listOf("123" to false)))
        assertEquals(MainActivity.FillReval.Fill("+44 90000 11111"),
            MainActivity.revalidateFill("+44 90000 11111", listOf("0044 90000 11111" to false)))
        assertEquals(MainActivity.FillReval.Fill("09876500000"),
            MainActivity.revalidateFill("09876500000", listOf("+91 98765 00000" to false)))
    }

    @Test fun revalidateFillRules() {
        // Same number ab bhi hai (format thoda alag ho tab bhi, key-normalized) -> wahi fill
        assertEquals(MainActivity.FillReval.Fill("+91 98765 00000"),
            MainActivity.revalidateFill("+91 98765 00000", listOf("9876500000" to false)))
        // Carried number gaya, single number bacha -> CHOOSER (doosra number kabhi silently substitute nahi)
        assertEquals(MainActivity.FillReval.Choose,
            MainActivity.revalidateFill("9000000000", listOf("9000000001" to false)))
        // Carried gaya, explicit primary hai -> phir bhi CHOOSER (substitution nahi)
        assertEquals(MainActivity.FillReval.Choose,
            MainActivity.revalidateFill("9000000000", listOf("9000000001" to false, "9000000002" to true)))
        // Multi bina primary -> chooser
        assertEquals(MainActivity.FillReval.Choose,
            MainActivity.revalidateFill("9000000000", listOf("9000000001" to false, "9000000002" to false)))
        // Contact hi gaya -> kuch fill nahi
        assertEquals(MainActivity.FillReval.Gone, MainActivity.revalidateFill("9000000000", emptyList()))
    }

    @Test fun widgetPersonIntentCarriesContactIdForRevalidation() {
        val ctx2 = ApplicationProvider.getApplicationContext<android.content.Context>()
        val i = FavWidgetModel.rawIntent(ctx2, FavWidgetModel.Slot.Person(c(7, "Asha", "+91 111"), "+91 111"))
        assertEquals(7L, i.getLongExtra("numberContactId", -1))
        assertEquals("Asha", i.getStringExtra("numberName"))
        val r = MainActivity.routeIntent(i)
        assertEquals(FavWidgetModel.WIDGET_CALL, i.action)
        assertNull("dedicated widget action bypasses generic fill route", r.fill)
        assertNull("dedicated action needs live validation, not generic call route", r.call)
        // Bina contactId wala normal DIAL (system share etc.) direct fill jaisa pehle tha
        val r2 = MainActivity.routeIntent(Intent(Intent.ACTION_DIAL).putExtra("number", "555"))
        assertNull(r2.fillContactId)
        assertEquals("555", r2.fill)
    }

    // ---- Pixel shots (day) ----

    private val SLOT = listOf(R.id.wslot1, R.id.wslot2, R.id.wslot3, R.id.wslot4)
    private val IMG = listOf(R.id.wimg1, R.id.wimg2, R.id.wimg3, R.id.wimg4)
    private val LBL = listOf(R.id.wlabel1, R.id.wlabel2, R.id.wlabel3, R.id.wlabel4)

    internal fun inflateAndBind(widthPx: Int, names: List<String>, hint: String?): View {
        val v = LayoutInflater.from(ctx).inflate(R.layout.widget_fav, null)
        val accent = 0xFF3D7BFF.toInt()
        if (hint != null) {
            v.findViewById<ImageView>(IMG[0]).setImageBitmap(FavWidgetProvider.glyph("+", accent))
            v.findViewById<TextView>(LBL[0]).text = hint
            for (i in 1..3) v.findViewById<View>(SLOT[i]).visibility = View.GONE
        } else {
            names.forEachIndexed { i, name ->
                v.findViewById<ImageView>(IMG[i]).setImageBitmap(
                    FavWidgetProvider.glyph(name.first().uppercase(), accent))
                v.findViewById<TextView>(LBL[i]).text = name
            }
            for (i in names.size..3) v.findViewById<View>(SLOT[i]).visibility = View.GONE
        }
        v.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(180, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        return v
    }

    internal fun saveViewShot(v: View, name: String) {
        val bmp = android.graphics.Bitmap.createBitmap(v.width, v.height, android.graphics.Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        val f = java.io.File((System.getenv("SHOT_DIR") ?: "build/shots") + "/$name.png")
        f.parentFile?.mkdirs()
        f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        println("SAVED $name ${bmp.width}x${bmp.height}")
    }

    @Test fun widgetShot4FavoritesDay() {
        val v = inflateAndBind(720, listOf("Asha", "Bittu", "Chandni", "Durgeshwari"), null)
        saveViewShot(v, "W1-widget-4fav-day")
    }

    @Test fun widgetShotNarrowDay() {
        val v = inflateAndBind(360, listOf("Asha", "Bittu", "Chandni", "Durgeshwari"), null)
        saveViewShot(v, "W2-widget-narrow-day")
    }

    @Test fun widgetShotEmptyHintDay() {
        val v = inflateAndBind(720, emptyList(), "Favorites add karo")
        saveViewShot(v, "W3-widget-empty-day")
    }
}
