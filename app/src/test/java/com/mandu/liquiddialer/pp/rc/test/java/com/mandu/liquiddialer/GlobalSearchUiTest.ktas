package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlobalSearchUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private lateinit var id:String
    private lateinit var first:CallLists.Entry
    private lateinit var second:CallLists.Entry
    private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
    private fun reveal(tag:String, unmerged:Boolean=false):SemanticsNodeInteraction {
        repeat(20) {
            val node=rule.onNodeWithTag(tag,useUnmergedTree=unmerged)
            if(node.isDisplayed())return node
            val top=node.fetchSemanticsNode().boundsInRoot.top
            rule.onRoot().performTouchInput {if(top<0) swipeDown(durationMillis=120) else swipeUp(durationMillis=120)}
            settle()
        }
        error("Not visible: $tag")
    }
    private fun click(tag:String){println("CLICK "+tag);reveal(tag).performClick();settle()}
    private fun await(check:()->Boolean){try{rule.waitUntil(10000){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};org.robolectric.shadows.ShadowLooper.idleMainLooper();settle();check()};settle()}catch(t:Throwable){throw AssertionError(rule.onRoot(useUnmergedTree=true).printToString(maxDepth=15),t)}}
    @Before fun clean(){listOf("",".bak",".new").forEach {File(ctx.filesDir,"manual_call_lists.json$it").delete()};ctx.getSharedPreferences("call_reminders",0).edit().clear().commit();File(ctx.filesDir,"notes.json").delete();File(ctx.filesDir,"notes.json.bak").delete()}
    private var noteId=0L
    private fun show(theme:String="daylight",font:Float=1f,corruptLists:Boolean=false,withExtras:Boolean=false){
        id=CallLists.create(ctx,"Shop").single().id;CallLists.add(ctx,id,"+442025550123","Office")
        first=CallLists.read(ctx).single().entries.first();CallLists.followUp(ctx,id,first.id,"busy","Delivery follow-up",123)
        Repo.addNote(ctx,"+12025550123","Family","Delivery private note");noteId=Repo.notesChecked(ctx).single().id
        if(corruptLists)File(ctx.filesDir,"manual_call_lists.json").writeText("broken")
        if(withExtras){
            org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CONTACTS)
            rule.activity.state.contacts=listOf(Contact(44,"lk44","Delivery contact","","+919876543210","Mobile",false,null))
            CallReminders.set(ctx,"+12025550001","Delivery reminder",System.currentTimeMillis()+7200000)
        }
        rule.mainClock.autoAdvance=false;settle()
        runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}}
        await{LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font}
        rule.runOnUiThread{rule.activity.setContent{LdThemeHost{GlobalSearchPage(rule.activity.state,rule.activity,{}, {rule.activity.state.dial=it})}}};settle()
        await{rule.onAllNodesWithTag("global_query",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        reveal("global_query").performTextInput("Delivery");settle()
        await{rule.onAllNodesWithTag("global_hit_NOTE_$noteId",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
    }
    @Test fun opensExactPrivateNoteRatherThanNumberWideList(){
        show();val before=Repo.notesChecked(ctx);click("global_open_NOTE_$noteId")
        reveal("note_full_text",true).assertTextEquals("Delivery private note");assertEquals(before,Repo.notesChecked(ctx));assertEquals("",rule.activity.state.dial)
    }
    @Test fun opensExactListEntryWithoutCallingOrWriting(){
        show();val before=CallLists.read(ctx);click("global_open_LIST_ENTRY_${first.id}")
        reveal("session_name",true).assertTextEquals("Office");reveal("session_note").assertTextContains("Delivery follow-up");assertEquals(before,CallLists.read(ctx));assertNull(rule.activity.state.pendingCall)
    }
    @Test fun corruptListSectionDoesNotHideMatchingNotes(){
        show(corruptLists=true);reveal("global_error",true).assertTextContains("Call Lists",substring=true)
        assertEquals("broken",File(ctx.filesDir,"manual_call_lists.json").readText())
        click("global_open_NOTE_$noteId");reveal("note_full_text",true).assertTextEquals("Delivery private note")
    }
    @Test fun contactResultOpensExactInternationalNumberWithoutCall(){
        show(withExtras=true);click("global_open_CONTACT_44")
        assertEquals("+919876543210",rule.activity.state.dial);assertNull(rule.activity.state.pendingCall)
    }
    @Test fun reminderResultOpensExistingGenerationWithoutChangingIt(){
        show(withExtras=true);val r=CallReminders.forNumber(ctx,"+12025550001")!!
        click("global_open_REMINDER_${r.token}");reveal("reminder_name").assertTextContains("Delivery reminder")
        assertEquals(r,CallReminders.forNumber(ctx,r.number));assertNull(rule.activity.state.pendingCall)
    }
    @Test fun clearingQueryRemovesPriorResultActions(){
        show();click("global_clear");settle()
        rule.onAllNodesWithTag("global_open_NOTE_$noteId",useUnmergedTree=true).assertCountEquals(0)
    }
    @Test fun daySearchPixels(){show();saveShot(rule,"global-search-day")}
    @Test fun nightLargeSearchPixels(){show("midnight",1.3f);saveShot(rule,"global-search-night-large");reveal("global_open_LIST_ENTRY_${first.id}");saveShot(rule,"global-search-night-large-lower")}
}
