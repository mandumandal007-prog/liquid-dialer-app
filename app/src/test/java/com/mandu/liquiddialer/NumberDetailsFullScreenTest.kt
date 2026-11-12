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
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsFullScreenTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

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

    /** Content readiness, not a longer fixed sleep: navigation composition may still be pending. */
    private fun awaitDetails(text:String) {
        try {rule.waitUntil(10000) {
            rule.runOnUiThread {androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()}
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            settle(100)
            rule.onAllNodesWithText(text,substring=true,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()
        }}catch(t:Throwable){throw AssertionError(rule.onRoot(useUnmergedTree=true).printToString(maxDepth=15),t)}
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight" }
        Unit
    }

    @Test
    fun callReminderPresetsSetAndClear() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.contacts = listOf(Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null))
        act.state.recents = listOf(Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null))
        act.state.isDefault = true
        com.mandu.liquiddialer.data.CallReminders.cancel(ctx, "+91 98765 43210")
        org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        settle()
        rule.onNodeWithText("Rahul Sharma", substring = true, useUnmergedTree = true).performClick()
        settle()
        awaitDetails("Call reminder")
        // reminder row dikhe, presets folded
        // notifications band state me warning row dikhni chahiye
        val nmShadow = org.robolectric.Shadows.shadowOf(ctx.getSystemService(android.app.NotificationManager::class.java))
        nmShadow.setNotificationsEnabled(false)
        revealDetails("Call reminder")
        settle(300)
        rule.onNodeWithText("Call reminder", substring = true, useUnmergedTree = true).performClick()
        settle(400)
        revealDetails("1 ghante baad")
        rule.onNodeWithText("30 min baad", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("1 ghante baad", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Shaam 6 baje", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Kal subah 9 baje", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Samay lagbhag", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Notifications band hain", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "38-call-reminder-presets")
        // preset set -> alarm + row par "Set:" dikhe
        rule.onNode(hasClickAction() and hasAnyDescendant(hasText("1 ghante baad")),useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick){it()}
        settle(400)
        val am = org.robolectric.Shadows.shadowOf(ctx.getSystemService(android.app.AlarmManager::class.java))
        assert(am.nextScheduledAlarm != null) { "preset click par alarm schedule hona chahiye" }
        revealDetails("Call reminder")
        settle(300)
        rule.onNodeWithText("Set:", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "38b-call-reminder-active")
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
        // Extra tools can put the last row below even this tall viewport. Reveal it by
        // bounded actual swipes, then retain the original visibility assertion.
        val last=rule.onNodeWithText("Contact delete karo",substring=true,useUnmergedTree=true)
        var reached=false
        repeat(20){
            if(!reached){
                val r=last.getUnclippedBoundsInRoot()
                val scroller=rule.onAllNodes(hasScrollAction(),useUnmergedTree=true).onLast()
                val view=scroller.getUnclippedBoundsInRoot()
                if(r.bottom>r.top&&r.top>=view.top&&r.bottom<=view.bottom)reached=true
                else{scroller.performTouchInput{if(r.top<view.top)swipeDown()else swipeUp()};settle(100)}
            }
        }
        rule.onNodeWithText("Contact delete karo", substring = true, useUnmergedTree = true).assertIsDisplayed()
        saveShot(rule, "04b-number-details-scrolled")
        // back se wapas recents (bottom bar dobara dikhe, page band)
        rule.onNodeWithTag("page_back", useUnmergedTree = true).performClick()
        settle()
        rule.onAllNodesWithTag("page_back", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithTag("bottom_bar", useUnmergedTree = true).assertExists()
    }

    @Test
    fun notesSaveKeepsExistingTextAndGlassInDayAndNight() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        android.util.AtomicFile(java.io.File(ctx.filesDir,"notes.json")).delete()
        com.mandu.liquiddialer.data.Repo.addNote(ctx,"+91 98765 43210","Rahul Sharma","पहली बात सुरक्षित")
        act.state.showOnboarding = false
        act.state.contacts = listOf(Contact(11,"lk1","Rahul Sharma","","+91 98765 43210","MOBILE",true,null))
        act.state.recents = listOf(Recent(1,"+91 98765 43210","Rahul Sharma",1,System.currentTimeMillis(),25,null,null))
        act.state.isDefault = true
        org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG,android.Manifest.permission.READ_CONTACTS)
        settle()
        rule.onNodeWithText("Rahul Sharma",substring=true,useUnmergedTree=true).performClick()
        settle()
        rule.onNodeWithText("Notes (",substring=true,useUnmergedTree=true).performClick()
        settle()
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(3)
        rule.onAllNodes(hasSetTextAction())[1].performTextInput("दूसरी बात 💚")
        rule.onNodeWithText("Save",useUnmergedTree=true).performClick()
        settle()
        rule.onNodeWithText("पहली बात सुरक्षित",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithText("दूसरी बात 💚",useUnmergedTree=true).assertIsDisplayed()
        org.junit.Assert.assertEquals(2,com.mandu.liquiddialer.data.Repo.notes(ctx).size)
        saveShot(rule,"264-notes-day")
        runBlocking { ctx.ldStore.edit { it[K.theme]="midnight" } }
        settle()
        saveShot(rule,"264-notes-night")
    }

}
