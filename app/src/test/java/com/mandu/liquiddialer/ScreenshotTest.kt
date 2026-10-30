package com.mandu.liquiddialer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 1200) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true
            p[K.profileName] = "Mandu Mandal"
            p[K.profileNumber] = "+91 83888 80466"
            p[K.theme] = "daylight"
            p[K.dialerScale] = 1f
        }
        com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        Unit
    }

    @Test
    fun dialerSheet() {
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.contacts = listOf(
            Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null),
            Contact(12, "lk2", "Papa", "", "+91 91234 56789", "MOBILE", false, null),
        )
        act.state.recents = listOf(
            Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null),
        )
        act.state.isDefault = true
        org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        act.state.recents = act.state.recents + Recent(2, "+91 91234 56789", "Papa", 2, System.currentTimeMillis() - 5 * 3600_000, 0, null, null)
        settle()
        saveShot(rule, "02-recents")
        act.state.recents = act.state.recents + (3..9).map { Recent(it.toLong(), "+91 90000 0000$it", "Zed $it", 1, System.currentTimeMillis() - it * 86_400_000L, 30, null, null) }
        settle()
        repeat(4) { rule.onRoot().performTouchInput { swipeUp(startY = height * 0.8f, endY = height * 0.2f) }; settle(400) }
        settle()
        saveShot(rule, "02b-recents-end")

        // open the dialer sheet via the FAB
        rule.onNodeWithTag("dialer_fab").performClick()
        settle()
        act.state.dial = "9876543210"
        settle()
        saveShot(rule, "03-dialer")

        act.state.dial = "9876"
        settle()
        saveShot(rule, "03d-chips")
        // user runs max dialer scale: same tight layout, no dead gaps
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p -> p[K.dialerScale] = 1.3f }
        }
        settle(2500)
        saveShot(rule, "03e-dialer-maxscale")
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p -> p[K.dialerScale] = 1f }
        }
        settle(1000)
        // dark theme + several live suggestions at once (user's phone look)
        act.state.contacts = act.state.contacts + listOf(
            Contact(13, "lk3", "Riya Singh", "", "+91 98765 11111", "MOBILE", false, null),
            Contact(14, "lk4", "Rohan Kumar", "", "+91 98765 22222", "MOBILE", false, null),
        )
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p -> p[K.theme] = "midnight" }
        }
        settle(2000)
        act.state.dial = "98765"
        settle()
        saveShot(rule, "03f-dialer-dark-multi")
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p -> p[K.theme] = "daylight" }
        }
        settle(1000)
        act.state.dial = "9876543210"
        settle()
        // open the three-dot menu (Paste / Pause / Wait)
        rule.onNodeWithTag("dial_menu").performClick()
        settle()
        saveShot(rule, "03b-dialer-menu")

        // add a pause -> number shows trailing comma
        rule.onNodeWithText("Pause add karo (,)").performClick()
        settle()
        saveShot(rule, "03c-dialer-pause")
    }

    /** Contacts tab pixel coverage: GlassRow padding change ke baad bhi rows sahi dikhte hain. */
    @Test
    fun contactsScreen() {
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.isDefault = true
        org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>())
            .grantPermissions(android.Manifest.permission.READ_CONTACTS)
        act.state.contacts = listOf(
            Contact(11, "lk1", "Rahul Sharma", "Sharma Rahul", "+91 98765 43210", "MOBILE", true, null),
            Contact(12, "lk2", "Papa", "Papa", "+91 91234 56789", "MOBILE", false, null),
            Contact(13, "lk3", "Riya Singh Chauhan Verylongname", "Riya", "+91 98765 11111", "WORK", false, null),
            Contact(14, "lk4", "Rohan Kumar", "Rohan", "+91 98765 22222", "MOBILE", false, null),
            Contact(15, "lk5", "Amma", "Amma", "+91 99887 76655", "HOME", true, null),
            Contact(16, "lk6", "Dada ji", "Dada ji", "+91 88776 65544", "MOBILE", false, null),
        )
        settle()
        rule.onAllNodesWithText("Contacts").onFirst().performClick()
        settle(1500)
        saveShot(rule, "05-contacts")
    }

    @Test
    fun volumePageAndNumberMenu() {
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.contacts = listOf(
            Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null),
        )
        act.state.recents = listOf(
            Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null),
        )
        act.state.isDefault = true
        org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        settle()
        // recents row: direct video button beside voice call button
        rule.onAllNodes(androidx.compose.ui.test.hasTestTag("video_mini"), useUnmergedTree = true).onFirst().assertExists()
        saveShot(rule, "02d-recents-row-video")
        // long-press recents row: number menu with direct Video call option
        rule.onNodeWithText("Rahul Sharma", useUnmergedTree = true).performTouchInput { longClick(center) }
        settle()
        saveShot(rule, "02c-number-menu-video")
        rule.onNodeWithText("Video call", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Number copy karo", substring = true, useUnmergedTree = true).performClick()
        settle()

        // Volume page (light, boost off)
        rule.onNodeWithTag("drawer_open").performClick(); settle()
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick(); settle(1800)
        var volOpened = false
        repeat(15) {
            if (rule.onAllNodesWithText("Volume & boost", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                try {
                    rule.onNodeWithText("Volume & boost", substring = true, useUnmergedTree = true).assertIsDisplayed()
                    rule.onNodeWithText("Volume & boost", substring = true, useUnmergedTree = true).performClick()
                    volOpened = true; return@repeat
                } catch (e: AssertionError) { }
            }
            val sc = rule.onAllNodes(androidx.compose.ui.test.hasScrollAction(), useUnmergedTree = true).fetchSemanticsNodes().lastOrNull()
            sc?.config?.getOrElseNullable(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { null }?.action?.invoke(0f, 1400f)
            rule.waitForIdle(); settle(300)
        }
        settle(1500)
        if (!volOpened) {
            val texts = mutableListOf<String>()
            fun walk(n: androidx.compose.ui.semantics.SemanticsNode) {
                n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.let { texts += it.joinToString(" ") }
                n.children.forEach(::walk)
            }
            walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
            error("Volume row not visible; on-screen texts=" + texts.take(50).joinToString(" || "))
        }
        rule.onAllNodesWithText("Media volume boost", substring = true, useUnmergedTree = true).onFirst().assertExists()
        saveShot(rule, "06a-volume-page")

        // dark + boost on with gain slider
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
                p[K.theme] = "midnight"; p[K.volBoost] = true; p[K.volBoostGain] = 55
            }
        }
        settle(2000)
        saveShot(rule, "06b-volume-boost-dark")
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
                p[K.theme] = "daylight"; p.remove(K.volBoost); p.remove(K.volBoostGain)
            }
        }
        settle(800)
    }
}
