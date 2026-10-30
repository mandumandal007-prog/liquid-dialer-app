package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
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
class QuietHoursShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun seedQuiet(on: Boolean) = runBlocking {
        ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f
            p[K.quietHours] = on
        }
        com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        Unit
    }

    private fun waitForNode(tries: Int = 15, cond: () -> Boolean) {
        repeat(tries) {
            if (cond()) return
            rule.mainClock.advanceTimeBy(400); rule.waitForIdle()
        }
    }

    private fun openQuietSection(target: String) {
        // onboarding overlay race: dismiss if still up after seeding
        waitForNode { rule.onAllNodesWithText("Baad me", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithTag("drawer_open", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("Baad me", useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.let {
            rule.onNodeWithText("Baad me", useUnmergedTree = true).performClick(); settle(1200)
        }
        rule.onNodeWithTag("drawer_open").performClick(); settle()
        waitForNode { rule.onAllNodesWithText("Settings", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        if (rule.onAllNodesWithText("Settings", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) {
            val texts = mutableListOf<String>()
            fun walk(n: androidx.compose.ui.semantics.SemanticsNode) {
                n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.joinToString(" ") { it.text }?.let { texts.add(it) }
                n.children.forEach { walk(it) }
            }
            walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
            println("NO-SETTINGS. TEXTS=" + texts.take(60).joinToString(" | "))
        }
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick(); settle(1800)
        // Settings page is one scrolling Column: nodes exist off-screen, so loop until actually displayed
        var shown = false
        repeat(40) {
            val nodes = rule.onAllNodes(hasText(target, substring = true, ignoreCase = true), useUnmergedTree = true).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                val b = nodes[0].boundsInRoot
                val h = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
                if (b.width > 1f && b.height > 1f && b.top >= 0 && b.bottom <= h) { shown = true; return@repeat }
            }
            val sc = rule.onAllNodes(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNodes().lastOrNull()
            sc?.config?.getOrElseNullable(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { null }?.action?.invoke(0f, 900f)
            rule.waitForIdle(); settle(300)
        }
        settle(800)
        assert(shown) { target + " not displayed after scrolling" }
    }

    @Test
    fun seriesBlockPageWithEntries() {
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
                p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f
                p[K.blockedPrefixes] = "140;+911800"
            }
            com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
            com.mandu.liquiddialer.telecom.CallManager.bump()
        }
        openQuietSection("Spam series block")
        val pre = rule.onNode(hasText("Spam series block", substring = true), useUnmergedTree = true).fetchSemanticsNode()
        println("SERIES-ROW bounds=" + pre.boundsInRoot + " root=" + rule.onRoot().fetchSemanticsNode().boundsInRoot)
        rule.onNode(hasText("Spam series block", substring = true), useUnmergedTree = true).performTouchInput { click(center) }; settle(1500)
        run {
            val texts = mutableListOf<String>()
            fun walk(n: androidx.compose.ui.semantics.SemanticsNode) {
                n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.joinToString(" ") { it.text }?.let { texts.add(it) }
                n.children.forEach { walk(it) }
            }
            walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
            println("AFTER-SERIES-CLICK TEXTS=" + texts.take(40).joinToString(" | "))
        }
        rule.onNodeWithText("140\u2026", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("+911800\u2026", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "16-series-block")
    }

    @Test
    fun quietHoursOffCollapsed() {
        seedQuiet(false)
        openQuietSection("Is time me incoming call silent")
        rule.onAllNodes(hasText("Screening role nahi mila", substring = true), useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithTag("Shuru_value", useUnmergedTree = true).assertCountEquals(0)
        saveShot(rule, "15-quiet-hours-off")
    }

    @Test
    fun quietHoursOnExpandedShowsHonestRoleState() {
        seedQuiet(true)
        openQuietSection("Favourites ko bajne do")
        // role not granted in test env: honest warning row must be visible, not a silent ON
        rule.onAllNodes(hasText("Screening role nahi mila", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes().also { assert(it.isNotEmpty()) { "role warning row missing while quiet hours ON without role" } }
        rule.onNodeWithText("Shuru", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Khatam", useUnmergedTree = true).assertExists()
        saveShot(rule, "14-quiet-hours-on")
    }
}
