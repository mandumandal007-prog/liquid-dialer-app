package com.mandu.liquiddialer

import androidx.compose.ui.test.*
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

/** Reminder presets and both caveats stay visible together after explicit scroll reveal. */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallReminderUiTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())

    private fun settle(ms: Long = 1500) {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        rule.runOnUiThread { androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications() }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    /** The redesigned page keeps the toolbar fixed. Reveal the whole target before taps. */
    private fun revealDetails(text:String) {
        val node=rule.onNodeWithText(text,substring=true,useUnmergedTree=true)
        repeat(30) {
            val r=node.getUnclippedBoundsInRoot()
            val scroll=rule.onNodeWithTag("page_content_scroll",true)
            val view=scroll.getUnclippedBoundsInRoot()
            if(r.top<view.top+androidx.compose.ui.unit.Dp(20f)||r.bottom>view.bottom-androidx.compose.ui.unit.Dp(80f)) {
                scroll.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,if(r.top<view.top+androidx.compose.ui.unit.Dp(20f))-100f else 100f)}
                settle(100)
            }
        }
        node.assertIsDisplayed()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight" }
        Unit
    }

    @Test
    fun reminderFlowCaveatsVisibleAfterReveal() {
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
        rule.runOnUiThread {act.mountTestPage {com.mandu.liquiddialer.ui.LdThemeHost {
            com.mandu.liquiddialer.ui.ContactDetailsFrame({}) {
                com.mandu.liquiddialer.ui.NumberSheet("+91 98765 43210",act.state.contacts.first(),act.state,act,{}, {}, {})
            }
        }}}
        settle()
        revealDetails("Call reminder")
        rule.onNodeWithText("Call reminder", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNode(hasClickAction() and hasAnyDescendant(hasText("Call reminder")),useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick){it()}
        settle(400)
        revealDetails("Notifications band hain")
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
