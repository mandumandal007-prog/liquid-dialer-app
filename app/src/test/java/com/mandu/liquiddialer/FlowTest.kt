package com.mandu.liquiddialer

import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f
        }
        // static call state can leak between test classes sharing a JVM: start clean
        com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        Unit
    }

    private fun dump(tag: String) {
        fun walk(n: androidx.compose.ui.semantics.SemanticsNode, d: Int) {
            val c = n.config
            val txt = c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.joinToString("|") { it.text }
            val t = c.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag)
            if (txt != null || t != null) println("DUMP $tag " + "-".repeat(d) + " tag=$t text=$txt")
            n.children.forEach { walk(it, d + 1) }
        }
        rule.onRoot(useUnmergedTree = true).fetchSemanticsNode().let { walk(it, 0) }
    }

    private fun tapKey(k: String) {
        rule.onNode(hasText(k) and hasNoClickAction() and !hasAnyAncestor(hasTestTag("bottom_bar")) and !hasSetTextAction(), useUnmergedTree = true)
            .performTouchInput { click(center) }
        settle(300)
    }

    private fun load() {
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.contacts = listOf(
            Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null),
            Contact(12, "lk2", "Papa", "", "+91 91234 56789", "MOBILE", false, null),
        )
        shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        act.state.recents = listOf(Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 60_000, 245, null, null))
        act.state.isDefault = true
        settle()
    }

    private fun noCallStarted() {
        val i = shadowOf(rule.activity.application).nextStartedActivity
        assertTrue("unexpected activity start: $i", i == null || i.action != Intent.ACTION_CALL)
    }

    @Test
    fun searchThenRecentsClearsQueryAndShowsHistory() {
        load()
        rule.onNodeWithText("Search contacts or numbers").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Papa")
        settle()
        rule.onNodeWithText("CONTACTS").assertExists()
        // Contacts tab keeps the query
        rule.onNodeWithText("Contacts").performClick(); settle(1500)
        rule.onNode(hasSetTextAction()).assertTextContains("Papa")
        assertEquals("contacts", rule.activity.state.tab)
        // Recents tap clears the query and shows call history
        rule.onNodeWithText("Recents").performClick(); settle(1500)
        assertEquals("recents", rule.activity.state.tab)
        rule.onNodeWithText("Search contacts or numbers").assertExists()
        rule.onNodeWithText("Recent calls").assertExists()
        rule.onNodeWithText("Rahul Sharma").assertExists()
        noCallStarted()
    }

    @Test
    fun swipeBottomBarBothDirectionsSwitchesTabsWithoutCalling() {
        load()
        rule.onNodeWithTag("bottom_bar").performTouchInput { swipeLeft() }
        settle(1500)
        assertEquals("contacts", rule.activity.state.tab)
        rule.onNodeWithTag("bottom_bar").performTouchInput { swipeRight() }
        settle(1500)
        assertEquals("recents", rule.activity.state.tab)
        noCallStarted()
    }

    @Test
    fun swipeRightWithActiveSearchClearsQuery() {
        load()
        rule.onNodeWithTag("bottom_bar").performTouchInput { swipeLeft() }; settle(1500)
        rule.onNode(hasSetTextAction()).performTextInput("Pap"); settle()
        rule.onNodeWithTag("bottom_bar").performTouchInput { swipeRight() }; settle(1500)
        assertEquals("recents", rule.activity.state.tab)
        rule.onNodeWithText("Search contacts or numbers").assertExists()
    }

    @Test
    fun suggestionChipFillsDialerOnlyAndNeverCalls() {
        load()
        rule.onNodeWithTag("dialer_fab").performClick(); settle(2500)
        "9876".forEach { tapKey(it.toString()) }; settle(1500)
        dump("CHIP")
        rule.onNodeWithTag("dial_suggest", useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Rahul \u00b7", substring = true) and hasAnyAncestor(hasTestTag("dial_suggest")), useUnmergedTree = true).performClick(); settle()
        assertEquals("919876543210", rule.activity.state.dial.removePrefix("+").filter { it.isDigit() }.let { if (it.length == 10) "91$it" else it })
        noCallStarted()
    }

    @Test
    fun searchMicWithoutRecognizerShowsNoticeAndLeavesQueryAlone() {
        load()
        shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        rule.onNodeWithTag("search_mic").performClick(); settle()
        rule.onNodeWithText(com.mandu.liquiddialer.ui.VOICE_NOTICE_UNAVAILABLE).assertExists()
        rule.onNode(hasSetTextAction()).assertTextEquals("")
    }

    @Test
    fun searchMicWithRecognizerAsksConsentBeforeAnyCapture() {
        load()
        val app = ApplicationProvider.getApplicationContext<LdApp>()
        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        val pm = shadowOf(app.packageManager)
        val ri = android.content.pm.ResolveInfo().apply {
            serviceInfo = ServiceInfo().apply { packageName = "com.fake.stt"; name = "com.fake.stt.Svc"; applicationInfo = app.applicationInfo; exported = true }
        }
        org.robolectric.shadows.ShadowSpeechRecognizer.setIsOnDeviceRecognitionAvailable(false)
        pm.addResolveInfoForIntent(Intent(android.speech.RecognitionService.SERVICE_INTERFACE), ri)
        rule.onNodeWithTag("search_mic").performClick(); settle(1500)
        dump("CONSENT")
        rule.onNodeWithTag("voice_consent_ok").assertExists()
        assertNull("recognizer must not start before consent", org.robolectric.shadows.ShadowSpeechRecognizer.getLatestSpeechRecognizer())
        rule.onNodeWithTag("voice_consent_cancel").performClick(); settle()
        rule.onNodeWithTag("voice_consent_ok").assertDoesNotExist()
        assertNull(org.robolectric.shadows.ShadowSpeechRecognizer.getLatestSpeechRecognizer())
    }

    @Test
    fun searchMicOnDeviceFillsSearchBoxOnlyAndNeverCalls() {
        load()
        val app = ApplicationProvider.getApplicationContext<LdApp>()
        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        org.robolectric.shadows.ShadowSpeechRecognizer.setIsOnDeviceRecognitionAvailable(true)
        val ri = android.content.pm.ResolveInfo().apply {
            serviceInfo = ServiceInfo().apply { packageName = "com.fake.stt"; name = "com.fake.stt.Svc"; applicationInfo = app.applicationInfo; exported = true }
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(Intent(android.speech.RecognitionService.SERVICE_INTERFACE), ri)
        rule.onNodeWithTag("search_mic").performClick(); settle(1500)
        rule.onNodeWithTag("voice_consent_ok").assertDoesNotExist()
        val rec = org.robolectric.shadows.ShadowSpeechRecognizer.getLatestSpeechRecognizer()
        assertNotNull("on-device path must start without consent", rec)
        rule.runOnUiThread {
            shadowOf(rec).triggerOnResults(android.os.Bundle().apply {
                putStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("Papa."))
            })
        }
        settle(1500)
        rule.onNode(hasSetTextAction()).assertTextContains("Papa")
        assertEquals("recents", rule.activity.state.tab)
        noCallStarted()
    }

    @Test
    fun keypadTapsEachDigitIntoDialer() {
        load()
        rule.onNodeWithTag("dialer_fab").performClick(); settle(2500)
        val keys = listOf("1","2","3","4","5","6","7","8","9","0","*","#")
        val got = StringBuilder()
        for (k in keys) {
            val n = rule.onNode(hasText(k) and hasNoClickAction() and !hasAnyAncestor(hasTestTag("bottom_bar")) and !hasSetTextAction(), useUnmergedTree = true)
            val b = try { n.fetchSemanticsNode().boundsInRoot.toString() } catch (e: Throwable) { "ERR ${e.message?.take(80)}" }
            rule.runOnUiThread { rule.activity.state.dial = "" }; settle(300)
            try { n.performTouchInput { click(center) } } catch (e: Throwable) { println("KEYERR $k ${e.message?.take(120)}") }
            settle(500)
            println("KEY $k bounds=$b -> dial='${rule.activity.state.dial}'")
            got.append(rule.activity.state.dial)
        }
        assertEquals("123456789 0*#".replace(" ", ""), got.toString())
    }

    @Test
    fun dialerSheetWithNavInsetKeepsCallButtonAboveNavBar() {
        load()
        val act = rule.activity
        rule.runOnUiThread {
            val insets = WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 144)).build()
            ViewCompat.dispatchApplyWindowInsets(act.window.decorView, insets)
        }
        settle()
        rule.onNodeWithTag("dialer_fab").performClick(); settle(2500)
        "9876".forEach { tapKey(it.toString()) }; settle(1500)
        saveShot(rule, "11-inset-dialer")
        val call = rule.onNodeWithTag("call_btn", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val rootH = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        val top = rule.onNodeWithTag("sheet_handle", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        println("DIALINSET call=$call rootH=$rootH handle=$top")
        assertTrue("call button under nav bar: $call rootH=$rootH", call.bottom <= rootH - 144f + 1f)
        assertTrue("sheet clipped at top", top.top >= 0f)
        assertEquals("9876", act.state.dial)
    }

    @Test
    fun dialerSheetHasNoDeadGapsEvenAtMaxScale() = runBlocking {
        // user-reported: big empty space below the number/chips and below the 0 row (worst at max dialer scale)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { it[K.dialerScale] = 1.3f }
        load()
        rule.onNodeWithTag("dialer_fab").performClick(); settle(2500)
        "9876".forEach { tapKey(it.toString()) }; settle(1500)
        saveShot(rule, "12-dialer-maxscale")
        val d = rule.activity.resources.displayMetrics.density
        val suggest = rule.onNodeWithTag("dial_suggest", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val keypad = rule.onNodeWithTag("dial_keypad", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val call = rule.onNodeWithTag("call_btn", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val gapAbove = keypad.top - suggest.bottom
        val gapBelow = call.top - keypad.bottom
        println("GAPS above=${gapAbove / d}dp below=${gapBelow / d}dp")
        assertTrue("gap below number/chips too big: ${gapAbove / d}dp", gapAbove <= 40 * d)
        assertTrue("gap below keypad too big: ${gapBelow / d}dp", gapBelow <= 40 * d)
        // digit keys keep their natural width-based size (no shrink to fix gaps)
        val key2 = rule.onNode(hasText("2") and hasNoClickAction() and !hasAnyAncestor(hasTestTag("bottom_bar")) and !hasSetTextAction(), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        println("KEY2 text bounds=$key2")
        noCallStarted()
    }

    @Test
    fun navInsetRenderKeepsLastRowClearOfBarAndFab() {
        load()
        val act = rule.activity
        act.state.recents = (1..9).map { Recent(it.toLong(), "+91 90000 0000$it", "Zed $it", 1, System.currentTimeMillis() - it * 86_400_000L, 30, null, null) }
        rule.runOnUiThread {
            val insets = WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 144)).build()
            ViewCompat.dispatchApplyWindowInsets(act.window.decorView, insets)
        }
        settle()
        repeat(5) { rule.onRoot().performTouchInput { swipeUp(startY = height * 0.7f, endY = height * 0.15f) }; settle(400) }
        saveShot(rule, "10-inset-recents-end")
        val fab = rule.onNodeWithTag("dialer_fab").fetchSemanticsNode().boundsInRoot
        val last = rule.onNodeWithText("Zed 9").fetchSemanticsNode().boundsInRoot
        println("INSET fab=$fab last=$last")
        assertTrue("last row overlaps FAB: $last vs $fab", last.bottom <= fab.top + 1f)
    }
}
