package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.AppLock
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.flow.first
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

/** App lock UI: locked gate pe content compose hi nahi hota, unlock ke baad aata hai;
 *  settings row auth success ke baad hi ON hoti hai. Normal viewport 360x800. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppLockUiTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private var authCalls = 0
    private var availCalls = 0
    private var authResult = true

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun waitFor(tries: Int = 15, cond: () -> Boolean) {
        repeat(tries) {
            if (cond()) return
            rule.mainClock.advanceTimeBy(400); rule.waitForIdle()
        }
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p.remove(K.appLock); p.remove(K.appLockDelaySec) }
        AppLock.resetForTests()
        authCalls = 0; availCalls = 0; authResult = true
        AppLock.autoPrompt = false   // auto-prompt off taake locked screen observe ho sake
        AppLock.authenticator = { _, cb -> authCalls++; cb(authResult) }
        AppLock.availability = { availCalls++; true }
        Unit
    }

    @After fun tearDown() = AppLock.resetForTests()

    private fun dismissOnboardingIfNeeded() {
        waitFor { rule.onAllNodesWithText("Baad me", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithTag("drawer_open", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("Baad me", useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.let {
            rule.onNodeWithText("Baad me", useUnmergedTree = true).performClick(); settle(1200)
        }
    }

    @Test
    fun lockedGateBlocksContentUntilAuth() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { it[K.appLock] = true }
        settle(2500)
        // locked: gate dikhta hai, content compose hi nahi hota
        waitFor { rule.onAllNodesWithText("Liquid Dialer lock", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Liquid Dialer lock", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Unlock karo", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Fingerprint ya phone ka PIN/Pattern/Password se kholo", substring = true, useUnmergedTree = true).assertExists()
        assertTrue("locked me recents content compose nahi hona chahiye",
            rule.onAllNodesWithText("Search contacts or numbers", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        saveShot(rule, "52-app-lock-gate-360x800")
        // unlock -> auth success -> content aata hai
        rule.onNodeWithText("Unlock karo", useUnmergedTree = true).performClick()
        settle(1800)
        assertEquals(1, authCalls)
        waitFor { rule.onAllNodesWithText("Search contacts or numbers", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithText("Baad me", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(AppLock.unlockedThisSession)
        Unit
    }

    @Test
    fun failedAuthStaysLocked() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { it[K.appLock] = true }
        authResult = false
        settle(2500)
        waitFor { rule.onAllNodesWithText("Liquid Dialer lock", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Unlock karo", useUnmergedTree = true).performClick()
        settle(1200)
        assertEquals(1, authCalls)
        // abhi bhi locked, content nahi
        rule.onNodeWithText("Liquid Dialer lock", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("Search contacts or numbers", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertFalse(AppLock.unlockedThisSession)
        Unit
    }

    @Test
    fun settingsRowEnablesOnlyAfterAuthSuccess() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        settle(2500)
        dismissOnboardingIfNeeded()
        rule.onNodeWithTag("drawer_open").performClick(); settle()
        waitFor { rule.onAllNodesWithText("Settings", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick(); settle(1800)
        // section top ke paas hai; bounded swipe (break wala) agar zaroorat ho
        var shown = false
        for (i in 1..8) {
            val nodes = rule.onAllNodesWithText("App lock", substring = false, useUnmergedTree = true).fetchSemanticsNodes()
            val rootH = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height
            if (nodes.isNotEmpty() && nodes[0].boundsInRoot.top >= 0 && nodes[0].boundsInRoot.bottom <= rootH) { shown = true; break }
            rule.onAllNodes(hasScrollAction(), useUnmergedTree = true)[0].performTouchInput { swipeUp() }
            rule.mainClock.advanceTimeBy(300); rule.waitForIdle()
        }
        assertTrue("App lock row viewport me aani chahiye", shown)
        // honest caveat dikhti hai
        rule.onNodeWithText("encrypt nahi hote", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "53-app-lock-settings-row")
        // toggle ON -> availability check -> auth -> success -> persist + segmented dikhe
        rule.onNodeWithText("App lock", substring = false, useUnmergedTree = true).performClick()
        settle(1800)
        assertTrue("availability check hona chahiye", availCalls > 0)
        assertEquals("auth ek baar manga", 1, authCalls)
        waitFor { runBlocking { ctx.ldStore.data.first()[K.appLock] == true } }
        assertTrue(ctx.ldStore.data.first()[K.appLock] == true)
        // "1 min" jaise labels settings me kahin aur bhi hain - viewport me DISPLAYED wala match karo
        val rootH = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height
        fun displayed(t: String) = rule.onAllNodesWithText(t, substring = false, useUnmergedTree = true).fetchSemanticsNodes()
            .any { it.boundsInRoot.top >= 0 && it.boundsInRoot.bottom <= rootH }
        waitFor { displayed("Turant") }
        assertTrue(displayed("Turant"))
        assertTrue(displayed("30 sec"))
        assertTrue(displayed("1 min"))
        // section poora frame me: segmented ko semantically scroll karke lao (swipe loop unreliable)
        rule.mainClock.autoAdvance = true   // performScrollTo ko live frames chahiye
        rule.onAllNodesWithText("Turant", useUnmergedTree = true)[0].performScrollTo()
        rule.waitForIdle()
        // chhota nudge: segmented ka bottom edge bhi frame ke andar (card margin ke saath)
        rule.onAllNodes(hasScrollAction(), useUnmergedTree = true)[0].performTouchInput { swipeUp(startY = centerY + 200f, endY = centerY - 100f) }
        rule.waitForIdle()
        settle(400)
        saveShot(rule, "53b-app-lock-enabled")
        Unit
    }

    @Test
    fun settingsRowRefusesWhenNoDeviceCredential() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        AppLock.availability = { availCalls++; false }   // phone me screen lock hi nahi
        settle(2500)
        dismissOnboardingIfNeeded()
        rule.onNodeWithTag("drawer_open").performClick(); settle()
        waitFor { rule.onAllNodesWithText("Settings", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick(); settle(1800)
        var shown2 = false
        for (i in 1..8) {
            val nodes = rule.onAllNodesWithText("App lock", substring = false, useUnmergedTree = true).fetchSemanticsNodes()
            val rootH = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height
            if (nodes.isNotEmpty() && nodes[0].boundsInRoot.top >= 0 && nodes[0].boundsInRoot.bottom <= rootH) { shown2 = true; break }
            rule.onAllNodes(hasScrollAction(), useUnmergedTree = true)[0].performTouchInput { swipeUp() }
            rule.mainClock.advanceTimeBy(300); rule.waitForIdle()
        }
        assertTrue(shown2)
        rule.onNodeWithText("App lock", substring = false, useUnmergedTree = true).performClick()
        settle(1200)
        assertTrue(availCalls > 0)
        assertEquals("no credential = auth kabhi nahi mangna", 0, authCalls)
        assertFalse("persist nahi hona chahiye", ctx.ldStore.data.first()[K.appLock] == true)
        Unit
    }
}
