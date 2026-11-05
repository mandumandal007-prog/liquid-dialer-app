package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.ContactControls
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
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

/** Never-block page: note + entries + remove flow, day aur large-font night shots. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NeverBlockUiTest {

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
        rule.runOnUiThread { rule.activity.setContent { LdThemeHost { NeverBlockPage({}) } } }
        settle()
    }

    @Before fun seed() {
        ctx.getSharedPreferences("contact_controls", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        ContactControls.setBlockNever(ctx, "+91 98765 43210", true)
        ContactControls.setBlockNever(ctx, "0044 20 7946 0018", true)
    }

    @Test fun entriesAndRemoveFlow() {
        show()
        rule.waitUntil(15000) { rule.onAllNodesWithTag("neverblock_row").fetchSemanticsNodes().size == 2 }
        rule.onNodeWithTag("neverblock_note", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("2 numbers", substring = true).assertIsDisplayed()
        rule.onNodeWithText("+91 98765 43210", substring = true).assertIsDisplayed()
        rule.onAllNodesWithTag("neverblock_remove", useUnmergedTree = true).onFirst().performClick(); settle()
        rule.onAllNodesWithTag("neverblock_row").assertCountEquals(1)
        rule.onAllNodesWithTag("neverblock_remove", useUnmergedTree = true).onFirst().performClick(); settle()
        rule.onAllNodesWithTag("neverblock_row").assertCountEquals(0)
        rule.onNodeWithText("Koi number nahi", substring = true).assertIsDisplayed()
        assert(ContactControls.blockNeverList(ctx).isEmpty())
    }

    @Test fun largeFontNightRowsReadable() {
        show("midnight", 1.3f)
        rule.waitUntil(15000) { rule.onAllNodesWithTag("neverblock_row").fetchSemanticsNodes().size == 2 }
        rule.onNodeWithTag("neverblock_note", useUnmergedTree = true).assertIsDisplayed()
        rule.onAllNodesWithText("Hatao", useUnmergedTree = true).assertCountEquals(2)
    }
}
