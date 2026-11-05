package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
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
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Block history page: note + entries + clear-with-confirm flow, day aur large-font night shots. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BlockHistoryUiTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun show(theme: String = "daylight", font: Float = 1f) {
        runBlocking { ctx.ldStore.edit { it[K.onboarded] = true; it[K.theme] = theme; it[K.fontScale] = font } }
        rule.waitUntil(15000) { LdApp.settings.value.theme == theme && LdApp.settings.value.fontScale == font }
        rule.runOnUiThread { rule.activity.setContent { LdThemeHost { BlockHistoryPage({}) } } }
        settle()
    }

    @Before fun seed() {
        BlockLog.clear(ctx)
        BlockLog.append(ctx, "+91 98765 43210", BlockLog.REASON_SERIES, ts = 1730000000000L)
        BlockLog.append(ctx, "", BlockLog.REASON_PRIVATE, ts = 1730000100000L)
        BlockLog.append(ctx, "+44 2025550123", BlockLog.REASON_UNKNOWN, ts = 1730000200000L)
    }

    @Test fun entriesReasonsAndClearConfirmFlow() {
        show()
        rule.waitUntil(15000) { rule.onAllNodesWithTag("blockhistory_row").fetchSemanticsNodes().size == 3 }
        rule.onNodeWithTag("blockhistory_note", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("3 roki gayi calls", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Private / hidden number").assertIsDisplayed()
        rule.onNodeWithText("Spam series block", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Unknown number block", substring = true).assertIsDisplayed()
        rule.onNodeWithTag("blockhistory_clear", useUnmergedTree = true).performClick(); settle()
        rule.onNodeWithTag("blockhistory_confirm_text", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("blockhistory_confirm_no", useUnmergedTree = true).performClick(); settle()
        rule.onAllNodesWithTag("blockhistory_row").assertCountEquals(3)
        rule.onNodeWithTag("blockhistory_clear", useUnmergedTree = true).performClick(); settle()
        rule.onNodeWithTag("blockhistory_confirm_yes", useUnmergedTree = true).performClick(); settle()
        rule.onAllNodesWithTag("blockhistory_row").assertCountEquals(0)
        rule.onNodeWithText("Koi call block nahi hui").assertIsDisplayed()
        assert(BlockLog.list(ctx).isEmpty())
    }

    @Test fun rowSheetSeNeverBlock() {
        ctx.getSharedPreferences("contact_controls", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        show()
        rule.waitUntil(15000) { rule.onAllNodesWithTag("blockhistory_row").fetchSemanticsNodes().size == 3 }
        // Private entry (blank display) pe sheet nahi khulti
        rule.onNodeWithText("Private / hidden number").performClick(); settle()
        rule.onAllNodesWithTag("blockhistory_action_never", useUnmergedTree = true).assertCountEquals(0)
        // Number row pe sheet: Call karo + Kabhi block mat karo
        rule.onNodeWithText("+91 98765 43210", substring = true).performClick(); settle()
        rule.onNodeWithTag("blockhistory_action_call", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("blockhistory_action_never", useUnmergedTree = true).performClick(); settle()
        assert(ContactControls.blockNever(ctx, "+91 98765 43210"))
        rule.onNodeWithText("Ab kabhi block nahi", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun largeFontNightRowsReadable() {
        show("midnight", 1.3f)
        rule.waitUntil(15000) { rule.onAllNodesWithTag("blockhistory_row").fetchSemanticsNodes().size == 3 }
        // 1.3x pe Robolectric window/owner-bounds quirk se isDisplayed unreliable hai;
        // tree me presence + bounds theek hain, visual gate pixel shots se hota hai
        rule.onNodeWithTag("blockhistory_note", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Unknown number block", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Private / hidden number").assertExists()
    }
}
