package com.mandu.liquiddialer

import android.content.ComponentName
import android.telecom.PhoneAccountHandle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.ContactControls
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.SimInfo
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Number details ka Contact Control Center section: SIM pin (2 SIM), quiet bypass,
 *  recording, announce switches - sab default OFF, day/night shots 360x800. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class ContactControlsSheetShotTest {

    // Keep frame/recomposition work on the test scheduler when settings emit from Default.
    val rule = createAndroidComposeRule<MainActivity>(effectContext = kotlinx.coroutines.test.StandardTestDispatcher())
    // Freeze the test frame clock before ActivityScenario starts composition, not after startup.
    @get:Rule val startup = org.junit.rules.RuleChain.outerRule(org.junit.rules.TestRule { base, _ ->
        object : org.junit.runners.model.Statement() {
            override fun evaluate() { rule.mainClock.autoAdvance = false; base.evaluate() }
        }
    }).around(rule)
    private val num = "+91 98765 43210"

    private fun settle(ms: Long = 900) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    // Animated semantic scrolling can spin with a paused clock. Use bounded touch frames.
    private fun scrollTo(node: SemanticsNodeInteraction) {
        repeat(8) {
            if (node.isDisplayed()) return
            rule.onRoot().performTouchInput { swipeUp(startY = height * .75f, endY = height * .35f) }
            settle(400)
        }
        node.assertIsDisplayed()
    }

    private fun seed(theme: String) = runBlocking {
        ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = theme; p[K.dialerScale] = 1f; p[K.fontScale] = 1f
        }
        Unit
    }

    @After
    fun tearDown() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.getSharedPreferences("contact_controls", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        ctx.getSharedPreferences("received_caller_names",0).edit().clear().apply()
        runBlocking { ctx.ldStore.edit { it[K.fontScale] = 1f }; Unit }
    }

    private fun openDetails(theme: String) {
        seed(theme)
        settle(2500)
        val ctx0 = ApplicationProvider.getApplicationContext<LdApp>()
        org.robolectric.Shadows.shadowOf(ctx0).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.isDefault = true
        act.state.contacts = listOf(Contact(11, "lk1", "Rahul Sharma", "", num, "MOBILE", true, null))
        act.state.recents = listOf(
            Recent(1, num, "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null),
            Recent(2, num, "Rahul Sharma", 2, System.currentTimeMillis() - 26 * 3_600_000, 130, null, null)
        )
        act.state.sims = listOf(
            SimInfo(PhoneAccountHandle(ComponentName("com.mandu", "A"), "simX"), "Jio", 1),
            SimInfo(PhoneAccountHandle(ComponentName("com.mandu", "A"), "simY"), "Airtel", 2)
        )
        org.robolectric.Shadows.shadowOf(ctx0).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        settle()
        rule.onAllNodesWithText("Rahul Sharma", substring = true, useUnmergedTree = true)[0].performClick()
        settle()
    }

    @Test
    fun controlsSectionDayNightShots() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        // DAY
        openDetails("daylight")
        rule.mainClock.autoAdvance = true
        scrollTo(rule.onNodeWithText("SIM pin karo", useUnmergedTree = true))
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false; settle(500)
        rule.onNodeWithText("SIM pin karo", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Auto (default ya pichli SIM)", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Quiet hours me bajne do", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Is number ki recording", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Is number ka naam bolo", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("2 calls", substring = true, useUnmergedTree = true).assertExists()
        // sab default OFF
        assertFalse(ContactControls.quietAllow(ctx, num))
        assertFalse(ContactControls.recordAlways(ctx, num))
        assertFalse(ContactControls.announceAlways(ctx, num))
        saveShot(rule, "cc_controls_day")
        rule.mainClock.autoAdvance = true
        scrollTo(rule.onNodeWithTag("cc_scope", useUnmergedTree = true))
        rule.waitForIdle(); rule.mainClock.autoAdvance = false; settle(500)
        saveShot(rule, "cc_controls_day2")
        // toggle quiet bypass -> persist ho jata hai
        rule.onNodeWithText("Quiet hours me bajne do", useUnmergedTree = true).performClick(); settle(400)
        assertTrue(ContactControls.quietAllow(ctx, num))
        rule.onNodeWithText("Quiet hours me bajne do", useUnmergedTree = true).performClick(); settle(400)
        assertFalse(ContactControls.quietAllow(ctx, num))
        // SIM pin chooser kholo aur Airtel pin karo
        rule.onNodeWithText("SIM pin karo", useUnmergedTree = true).performClick(); settle(400)
        rule.mainClock.autoAdvance = true
        scrollTo(rule.onNodeWithText("SIM 2 se hamesha", useUnmergedTree = true))
        rule.waitForIdle(); rule.mainClock.autoAdvance = false; settle(400)
        rule.onNodeWithText("SIM 2 se hamesha", useUnmergedTree = true).performClick(); settle(400)
        assertEquals("simY", ContactControls.simPin(ctx, num))
        // stale pin: SIM list se gayab -> picker pe force, reroute nahi
        assertEquals(null, com.mandu.liquiddialer.CallPrep.chooseSim(null, "simGaya", "simX", null, listOf("simX")) { it })
        // NIGHT
        seed("midnight"); settle(1500)
        rule.mainClock.autoAdvance = true
        scrollTo(rule.onNodeWithTag("cc_scope", useUnmergedTree = true))
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false; settle(500)
        saveShot(rule, "cc_controls_night")
    }

    @Test
    fun controlsSectionLargeFont() {
        // bade font (dialerScale 1.3) pe changed rows ka inspection: wrap/clip nahi
        openDetails("daylight")
        runBlocking { ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p -> p[K.fontScale] = 1.3f }; Unit }
        settle(1200)
        rule.mainClock.autoAdvance = true
        scrollTo(rule.onNodeWithTag("cc_scope", useUnmergedTree = true))
        rule.waitForIdle(); rule.mainClock.autoAdvance = false; settle(500)
        rule.onNodeWithText("SIM pin karo", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Quiet hours me bajne do", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Is number ki recording", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Is number ka naam bolo", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("sirf abhi loaded window", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "cc_controls_bigfont")
    }
    @Test fun receivedNameClearDayNightShots() {
        val ctx=ApplicationProvider.getApplicationContext<LdApp>()
        com.mandu.liquiddialer.data.ReceivedNames.learn(ctx,num,"Rahul Network",1,1)
        openDetails("daylight")
        rule.mainClock.autoAdvance=true
        scrollTo(rule.onNodeWithText("Rahul Network",useUnmergedTree=true))
        rule.waitForIdle();rule.mainClock.autoAdvance=false;settle(500)
        saveShot(rule,"N201-clear-day")
        seed("midnight");settle(1000)
        saveShot(rule,"N201-clear-night")
        rule.onNodeWithText("Yaad naam hatao",useUnmergedTree=true).performClick();settle(500)
        org.junit.Assert.assertNull(com.mandu.liquiddialer.data.ReceivedNames.get(ctx,num))
    }

}
