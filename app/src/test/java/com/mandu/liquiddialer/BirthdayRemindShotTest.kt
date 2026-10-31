package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.BirthdayReminders
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Birthday reminder settings row: normal 360x800 viewport me row dikhti hai,
 *  toggle karne par persisted ON hota hai. Pixel shot: 54-birthday-remind. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BirthdayRemindShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @After fun tearDown() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { it[K.birthdayRemind] = false }  // JVM-shared sandbox reset
        BirthdayReminders.cancel(ctx)
        Unit
    }

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Test
    fun birthdayRowVisibleAndToggles() {
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
                p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f
            }
            com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
            com.mandu.liquiddialer.telecom.CallManager.bump()
        }
        // onboarding overlay race dismiss
        repeat(15) {
            if (rule.onAllNodesWithText("Baad me", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                rule.onNodeWithText("Baad me", useUnmergedTree = true).performClick(); settle(1200); return@repeat
            }
            if (rule.onAllNodesWithTag("drawer_open", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return@repeat
            rule.mainClock.advanceTimeBy(400); rule.waitForIdle()
        }
        rule.onNodeWithTag("drawer_open").performClick(); settle()
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick(); settle(1800)

        // Settings ek scrolling column hai: row actually viewport me aane tak scroll
        var shown = false
        repeat(40) {
            val nodes = rule.onAllNodes(hasText("Birthday reminder", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                val b = nodes[0].boundsInRoot
                val h = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
                if (b.width > 1f && b.height > 1f && b.top >= 0 && b.bottom <= h) { shown = true; return@repeat }
            }
            rule.onAllNodes(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNodes().lastOrNull()
                ?.config?.getOrElseNullable(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { null }?.action?.invoke(0f, 900f)
            rule.waitForIdle(); settle(300)
        }
        settle(600)
        assert(shown) { "Birthday reminder row viewport me nahi aayi" }
        saveShot(rule, "54-birthday-remind-off")

        // toggle ON -> persisted
        rule.onNode(hasText("Birthday reminder", substring = true), useUnmergedTree = true).performTouchInput { click(center) }
        settle(1200)
        val on = runBlocking { ApplicationProvider.getApplicationContext<LdApp>().ldStore.data.first()[K.birthdayRemind] ?: false }
        assertTrue("tap par birthdayRemind ON persist hona chahiye", on)
        saveShot(rule, "54-birthday-remind-on")
    }
}
