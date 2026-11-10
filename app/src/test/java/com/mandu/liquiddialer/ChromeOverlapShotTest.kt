package com.mandu.liquiddialer

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Bottom chrome overlap regression: bottom_bar / dialer_fab KABHI bhi row ke
 *  call buttons (call_mini) ko cover nahi karte - normal aur fontScale 1.3, dono tabs.
 *  Pixel shots: 57-chrome-contacts / 57b-chrome-recents / 57c-chrome-fontscale. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChromeOverlapShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @After fun tearDown() = runBlocking {
        ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit {
            it[K.fontScale] = 1f
            it[K.confirmCall] = false
        }
        Unit
    }

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun seed(fs: Float) {
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(
            android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_CALL_LOG)
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
                p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f; p[K.fontScale] = fs
            }
        }
        rule.waitForIdle()
        repeat(15) {
            if (rule.onAllNodesWithText("Baad me", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                rule.onNodeWithText("Baad me", useUnmergedTree = true).performClick(); settle(1200); return@repeat
            }
            if (rule.onAllNodesWithTag("drawer_open", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return@repeat
            rule.mainClock.advanceTimeBy(400); rule.waitForIdle()
        }
        settle(1500)
        val names = listOf("Aman", "Bablu", "Chotu", "Deepak", "Esha", "Farhan", "Golu", "Hari", "Ishaan", "Jatin", "Karan", "Lalu")
        val fake = names.mapIndexed { i, n -> Contact(i + 1L, "lk$i", n, n, "90000000%02d".format(i), "Mobile", false, null) }
        val rec = names.take(8).mapIndexed { i, n -> Recent(i + 1L, "90000000%02d".format(i), n, 1, 1000L + i, 30_000L, null, null) }
        rule.runOnUiThread { rule.activity.state.contacts = fake; rule.activity.state.recents = rec }
        settle(1500)
    }

    private fun assertNoChromeOverlap(where: String) {
        val fab = rule.onAllNodesWithTag("dialer_fab", useUnmergedTree = true).fetchSemanticsNodes()
        val bar = rule.onAllNodesWithTag("bottom_bar", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("$where: dialer_fab render hona chahiye", fab.isNotEmpty())
        assertTrue("$where: bottom_bar render hona chahiye", bar.isNotEmpty())
        val fb = fab[0].boundsInRoot; val bb = bar[0].boundsInRoot
        assertTrue("$where: fab aur nav pill ek dusre ko overlap nahi kare (fab=$fb bar=$bb)", !fb.overlaps(bb))
        // scroll-0 invariant: pinned translucent header (top_bar) kisi bhi call button ko
        // block nahi karta - list top padding (86dp) header ke 70dp se badi rehni chahiye.
        // (scrolled state me rows header ke peeche JANA intended design hai; ye sirf rest state check hai.)
        val hdr = rule.onAllNodesWithTag("top_bar", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("$where: top_bar render hona chahiye", hdr.isNotEmpty())
        val hb = hdr[0].boundsInRoot
        val calls = rule.onAllNodesWithTag("call_mini", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("$where: call buttons composed hone chahiye", calls.isNotEmpty())
        calls.forEachIndexed { i, n ->
            val b = n.boundsInRoot
            assertTrue("$where: rest state me call button #$i top_bar se overlap nahi (btn=$b hdr=$hb)", !b.overlaps(hb))
        }
        // Bottom list full-viewport hai (rows floating bar ke peeche dikhti hain - intended,
        // user request 06:17). Safety invariant: last row tak scroll karne par uska call
        // button bottom chrome se CLEAR hona chahiye (list bottom content pad = band+clearance).
        rule.onAllNodesWithTag("main_list", useUnmergedTree = true)[0].performTouchInput {
            repeat(10) { swipeUp(durationMillis = 200); advanceEventTime(100) }
        }
        settle(1200)
        val fb2 = rule.onAllNodesWithTag("dialer_fab", useUnmergedTree = true).fetchSemanticsNodes()[0].boundsInRoot
        val bb2 = rule.onAllNodesWithTag("bottom_bar", useUnmergedTree = true).fetchSemanticsNodes()[0].boundsInRoot
        val lastBtn = rule.onAllNodesWithTag("call_mini", useUnmergedTree = true).fetchSemanticsNodes()
            .maxByOrNull { it.boundsInRoot.top }
        assertTrue("$where: scroll-end par last call button composed hona chahiye", lastBtn != null)
        val lb = lastBtn!!.boundsInRoot
        assertTrue("$where: scroll-end last call button bottom_bar se clear (btn=$lb bar=$bb2)", !lb.overlaps(bb2))
        assertTrue("$where: scroll-end last call button FAB se clear (btn=$lb fab=$fb2)", !lb.overlaps(fb2))
    }

    @Test
    fun contactsTabChromeClear() {
        seed(1f)
        rule.runOnUiThread { rule.activity.state.tab = "contacts" }
        settle(1800)
        assertNoChromeOverlap("contacts")
        saveShot(rule, "57-chrome-contacts")
    }

    @Test
    fun recentsTabChromeClear() {
        seed(1f)
        rule.runOnUiThread { rule.activity.state.tab = "recents" }
        settle(1800)
        assertNoChromeOverlap("recents")
        saveShot(rule, "57b-chrome-recents")
    }

    @Test
    fun fontScale13ChromeClear() {
        seed(1.3f)
        rule.runOnUiThread { rule.activity.state.tab = "contacts" }
        settle(1800)
        assertNoChromeOverlap("contacts@fs1.3")
        saveShot(rule, "57c-chrome-fontscale")
    }

    // ===== touch-interception proof (user 06:17 full-height layout): floating bar/FAB ke
    // PEECHE dikhti row ka call button us jagah tap karne par fire NAHI hona chahiye -
    // upar ka chrome touch rokta hai. confirmCall=true se call ka fake callback milta hai
    // (confirmNumber set), koi real call nahi. =====

    private fun seedRecentsForIntercept() {
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(
            android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_CALL_LOG)
        // 12 rows: v280 se rows thodi chhoti (40dp minis, tight spacing) - itni rows ki scroll ke baad bhi ek row chrome ke peeche aaye
        val rec = (1..12).map { i -> Recent(i + 100L, "90000001%02d".format(i), "Cover $i", 1, 2000L + i * 60_000L, 30_000L, null, null) }
        rule.runOnUiThread {
            rule.activity.state.showOnboarding = false
            rule.activity.state.isDefault = true
            rule.activity.state.contacts = emptyList()
            rule.activity.state.recents = rec
            rule.activity.state.tab = "recents"
            rule.activity.state.confirmNumber = null
        }
        settle(1800)
    }

    private fun enableConfirm() = runBlocking {
        ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { it[K.confirmCall] = true }
        var w = 0
        while (!LdApp.settings.value.confirmCall && w < 100) { Thread.sleep(50); w++ }
        Unit
    }

    private fun coveredRowExists(): Boolean {
        val chrome = (rule.onAllNodesWithTag("bottom_bar", useUnmergedTree = true).fetchSemanticsNodes() +
            rule.onAllNodesWithTag("dialer_fab", useUnmergedTree = true).fetchSemanticsNodes()).map { it.boundsInRoot }
        if (chrome.isEmpty()) return false
        return rule.onAllNodesWithTag("call_mini", useUnmergedTree = true).fetchSemanticsNodes()
            .any { n -> val b = n.boundsInRoot; chrome.any { c -> b.overlaps(c) } }
    }

    private fun ensureViewportProtectsRows() {
        val viewport = rule.onNodeWithTag("home_content_viewport",true).fetchSemanticsNode().boundsInRoot
        val chrome = rule.onNodeWithTag("bottom_bar",true).fetchSemanticsNode().boundsInRoot
        assertTrue("list viewport ends above chrome",viewport.bottom<=chrome.top)
        repeat(3){
            assertTrue("no call button may be covered by bottom chrome",!coveredRowExists())
            rule.onAllNodesWithTag("main_list",true)[0].performTouchInput{swipeUp(durationMillis=200)}
            settle(600)
        }
    }

    private fun stateOf(f: (com.mandu.liquiddialer.MainActivity) -> Boolean): Boolean {
        var r = false
        rule.runOnUiThread { r = f(rule.activity) }
        return r
    }

    private fun stateStr(f: (com.mandu.liquiddialer.MainActivity) -> String): String {
        var r = ""
        rule.runOnUiThread { r = f(rule.activity) }
        return r
    }

    @Test
    fun fabCoordinateTapWithProtectedRowsDoesNotCall() {
        seedRecentsForIntercept()
        enableConfirm()
        ensureViewportProtectsRows()
        rule.onAllNodesWithTag("dialer_fab", useUnmergedTree = true)[0].performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width / 2f, height / 2f))
        }
        settle()
        assertTrue("FAB coordinate tap: dialer khulna chahiye (chrome cover nahi, kaam karta hai)", stateOf { it.state.tab == "dialer" })
        assertTrue("FAB tap neeche wali row ka call trigger nahi karna chahiye", stateOf { it.state.confirmNumber == null })
    }

    @Test
    fun navCoordinateTapWithProtectedRowsNoFallThrough() {
        // Coordinate tap proof (semantics performClick nahi - wo hit-test bypass karta hai):
        // pill ke contacts segment par ASLI tap (1) tab switch kare, (2) neeche covered
        // row tak touch fall-through na kare (na call, na details page).
        seedRecentsForIntercept()
        enableConfirm()
        ensureViewportProtectsRows()
        rule.onAllNodesWithTag("bottom_bar", useUnmergedTree = true)[0].performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width * 0.75f, height / 2f))
        }
        settle()
        assertTrue("pill contacts segment coordinate tap: contacts tab khulna chahiye", stateOf { it.state.tab == "contacts" })
        assertTrue("pill tap neeche wali row ka call trigger nahi karna chahiye", stateOf { it.state.confirmNumber == null })
        assertTrue("pill tap neeche wali row ka details page nahi kholna chahiye (fall-through)",
            rule.onAllNodesWithText("Number details").fetchSemanticsNodes().isEmpty())
        rule.onAllNodesWithTag("bottom_bar", useUnmergedTree = true)[0].performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width * 0.25f, height / 2f))
        }
        settle()
        assertTrue("pill recents segment coordinate tap: recents tab wapas aana chahiye", stateOf { it.state.tab == "recents" })
    }

    @Test
    fun shortListRowsClearAndWorking() {
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(
            android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_CALL_LOG)
        val rec = (1..2).map { i -> Recent(i + 200L, "90000002%02d".format(i), "Short $i", 1, 5000L + i, 30_000L, null, null) }
        rule.runOnUiThread {
            rule.activity.state.showOnboarding = false
            rule.activity.state.isDefault = true
            rule.activity.state.contacts = emptyList()
            rule.activity.state.recents = rec
            rule.activity.state.tab = "recents"
            rule.activity.state.confirmNumber = null
        }
        settle(1800)
        val chrome = (rule.onAllNodesWithTag("bottom_bar", useUnmergedTree = true).fetchSemanticsNodes() +
            rule.onAllNodesWithTag("dialer_fab", useUnmergedTree = true).fetchSemanticsNodes()).map { it.boundsInRoot }
        val calls = rule.onAllNodesWithTag("call_mini", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("short list me dono rows composed honi chahiye", calls.size >= 2)
        calls.forEachIndexed { i, n ->
            val b = n.boundsInRoot
            assertTrue("short list: row #$i rest par chrome se clear honi chahiye (scroll ki zaroorat nahi)", chrome.all { c -> !b.overlaps(c) })
        }
        // uncovered button kaam bhi karta hai (contrast proof)
        enableConfirm()
        rule.onAllNodesWithTag("call_mini", useUnmergedTree = true)[0].performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width / 2f, height / 2f))
        }
        settle()
        assertTrue("uncovered call button tap par confirm card aana chahiye", stateOf { it.state.confirmNumber != null })
    }

}
