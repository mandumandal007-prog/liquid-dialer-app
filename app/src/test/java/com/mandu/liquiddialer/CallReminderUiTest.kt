package com.mandu.liquiddialer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
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

/** Call reminder flow ka decisive visual: tall viewport me poora preset section bina scroll
 *  dikhta hai - chips, approximate-timing note, aur notifications-band warning (fixture me
 *  notifications disabled). Koi performScrollTo nahi (wo harness me hang karta hai). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallReminderUiTest {

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
    fun reminderFlowCaveatsVisibleWithoutScroll() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.contacts = listOf(Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null))
        act.state.recents = listOf(Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null))
        act.state.isDefault = true
        com.mandu.liquiddialer.data.CallReminders.cancel(ctx, "+91 98765 43210")
        org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        // notifications band: warning row dikhni chahiye
        org.robolectric.Shadows.shadowOf(ctx.getSystemService(android.app.NotificationManager::class.java))
            .setNotificationsEnabled(false)
        settle()
        rule.onAllNodesWithText("Rahul Sharma", substring = true, useUnmergedTree = true).onFirst().performClick()
        settle()
        rule.onNodeWithText("Call reminder", substring = true, useUnmergedTree = true).assertIsDisplayed().performClick()
        settle(400)
        rule.onNodeWithText("30 min baad", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("1 ghante baad", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Shaam 6 baje", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Kal subah 9 baje", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Samay lagbhag hoga", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Notifications band hain", substring = true, useUnmergedTree = true).assertIsDisplayed()
        saveShot(rule, "39-reminder-caveats")
        com.mandu.liquiddialer.data.CallReminders.cancel(ctx, "+91 98765 43210")
    }
}
