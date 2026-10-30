package com.mandu.liquiddialer

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

/** Number/contact details ab bottom sheet nahi, poori screen: back button hai, sheet ka
 *  drag-handle nahi, aur saare actions (Call/SMS/WhatsApp/Fav + copy/share/notes/block) wahi hain. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsFullScreenTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight" }
        Unit
    }

    @Test
    fun detailsOpenFullScreenWithBack() {
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.contacts = listOf(Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null))
        act.state.recents = listOf(Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null))
        act.state.isDefault = true
        org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>())
            .grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        settle()
        rule.onNodeWithText("Rahul Sharma", substring = true, useUnmergedTree = true).performClick()
        settle()
        // poori screen page: back button hai, bottom-sheet ka handle NAHI
        rule.onNodeWithTag("page_back", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithTag("sheet_handle", useUnmergedTree = true).assertCountEquals(0)
        // actions + rows sab maujood
        rule.onNodeWithText("Number copy karo", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Number share karo", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Notes (", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("WhatsApp", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "04-number-details-full")
        // back se wapas recents (bottom bar dobara dikhe, page band)
        rule.onNodeWithTag("page_back", useUnmergedTree = true).performClick()
        settle()
        rule.onAllNodesWithTag("page_back", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithTag("bottom_bar", useUnmergedTree = true).assertExists()
    }
}
