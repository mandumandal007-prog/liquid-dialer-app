package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.BlockLog
import com.mandu.liquiddialer.data.ContactControls
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.ui.BlockHistoryPage
import com.mandu.liquiddialer.ui.LdThemeHost
import com.mandu.liquiddialer.ui.NeverBlockPage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Never-block pixel shots: page day + night-large, settings row day, block-history action sheet day. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NeverBlockShotTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun host(theme: String, font: Float, body: @androidx.compose.runtime.Composable () -> Unit) {
        runBlocking { ctx.ldStore.edit { it[K.onboarded] = true; it[K.theme] = theme; it[K.fontScale] = font } }
        rule.waitUntil(15000) { LdApp.settings.value.theme == theme && LdApp.settings.value.fontScale == font }
        rule.runOnUiThread { rule.activity.setContent { LdThemeHost { body() } } }
        settle()
    }

    @Before fun seed() {
        ctx.getSharedPreferences("contact_controls", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        ContactControls.setBlockNever(ctx, "+91 98765 43210", true)
        ContactControls.setBlockNever(ctx, "0044 20 7946 0018", true)
        BlockLog.clear(ctx)
        BlockLog.append(ctx, "+91 98765 43210", BlockLog.REASON_SERIES, ts = 1730000000000L)
        BlockLog.append(ctx, "", BlockLog.REASON_PRIVATE, ts = 1730000100000L)
        BlockLog.append(ctx, "+44 2025550123", BlockLog.REASON_UNKNOWN, ts = 1730000200000L)
    }

    @Test fun shotPageDay() {
        host("daylight", 1f) { NeverBlockPage({}) }
        rule.waitUntil(15000) { rule.onAllNodesWithTag("neverblock_row").fetchSemanticsNodes().size == 2 }
        settle(600)
        saveShot(rule, "289-neverblock-day")
    }

    @Test fun shotPageNightLarge() {
        host("midnight", 1.3f) { NeverBlockPage({}) }
        rule.waitUntil(15000) { rule.onAllNodesWithTag("neverblock_row").fetchSemanticsNodes().size == 2 }
        settle(600)
        saveShot(rule, "289-neverblock-night-large")
    }

    @Test fun shotBlockHistorySheetDay() {
        host("daylight", 1f) { BlockHistoryPage({}) }
        rule.waitUntil(15000) { rule.onAllNodesWithTag("blockhistory_row").fetchSemanticsNodes().size == 3 }
        rule.onNodeWithText("+91 98765 43210", substring = true).performClick(); settle()
        rule.onNodeWithTag("blockhistory_action_never", useUnmergedTree = true).assertExists()
        settle(400)
        saveShot(rule, "289-blockhistory-sheet-day")
    }

    @Test fun shotSettingsRowDay() {
        runBlocking {
            ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f }
            com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
            com.mandu.liquiddialer.telecom.CallManager.bump()
        }
        repeat(15) {
            if (rule.onAllNodesWithText("Baad me", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                rule.onNodeWithText("Baad me", useUnmergedTree = true).performClick(); settle(1200); return@repeat
            }
            if (rule.onAllNodesWithTag("drawer_open", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return@repeat
            rule.mainClock.advanceTimeBy(400); rule.waitForIdle()
        }
        rule.onNodeWithTag("drawer_open").performClick(); settle()
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick(); settle(1800)
        openSettingsGroup(rule, "Blocking")
        var shown = false
        repeat(40) {
            val nodes = rule.onAllNodes(hasText("Kabhi block mat karo", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
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
        assert(shown) { "Kabhi block mat karo settings row viewport me nahi aayi" }
        saveShot(rule, "289-neverblock-row-day")
    }
}
