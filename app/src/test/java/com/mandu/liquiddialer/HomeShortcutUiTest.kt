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

/** Number details ka "Home screen par call button" row: text + tap-safe + shot.
 *  Apni alag class me hai - doosri details-test ke saath ek JVM me Compose idle
 *  kabhi settle nahi hota tha (purani alarm/notification shadow state ke saath). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeShortcutUiTest {

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
    fun homeScreenShortcutRow() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.contacts = listOf(Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null))
        act.state.recents = listOf(Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null))
        act.state.isDefault = true
        org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        // launcher support ON rakho taaki asli product text dikhe
        org.robolectric.Shadows.shadowOf(ctx.getSystemService(android.content.pm.ShortcutManager::class.java))
            .setIsRequestPinShortcutSupported(true)
        settle()
        rule.onNodeWithText("Rahul Sharma", substring = true, useUnmergedTree = true).performClick()
        settle()
        // tall viewport me row bina scroll ke dikhta hai (scroll-chain is harness me hang karta hai)
        rule.onNodeWithText("Home screen par call button", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Button dabate hi call lag jayega", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "42-home-shortcut-row")
        // tap -> pin attempt; Robolectric me request fail bhi ho to crash nahi, bas toast
        rule.onNodeWithText("Home screen par call button", substring = true, useUnmergedTree = true).performClick()
        settle(400)
    }
}
