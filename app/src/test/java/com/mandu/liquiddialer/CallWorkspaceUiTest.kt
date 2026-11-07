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
class CallWorkspaceUiTest {
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
    private fun await(check:()->Boolean){rule.waitUntil(10000){settle();check()};settle()}
    @Before fun clean(){listOf("",".bak",".new").forEach {File(ctx.filesDir,"manual_call_lists.json$it").delete()}}
    private fun show(theme:String="daylight",font:Float=1f) {
        id=CallLists.create(ctx,"Weekend follow-ups").single().id
        CallLists.add(ctx,id,"+44 2025 550123","Office appointment follow-up")
        CallLists.add(ctx,id,"+1 2025 550123","Family overseas")
        first=CallLists.read(ctx).single().entries[0];second=CallLists.read(ctx).single().entries[1]
        rule.mainClock.autoAdvance=false;settle()
        runBlocking {ctx.ldStore.edit {it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.confirmCall]=true}}
        await {LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font && LdApp.settings.value.confirmCall}
        rule.runOnUiThread {rule.activity.setContent {LdThemeHost {CallListsPage(rule.activity.state,rule.activity,{})}}};settle()
        await {rule.onAllNodesWithTag("list_open_$id").fetchSemanticsNodes().isNotEmpty()}
        click("list_open_$id")
    }
    @Test fun sessionCallDoesNotWriteThenSaveOutcomeNoteAndAdvanceExplicitly() {
        show();click("list_session")
        click("session_call");assertEquals(first.number,rule.activity.state.confirmNumber)
        assertEquals("pending",CallLists.read(ctx).single().entries.first().outcome)
        click("session_outcome_busy");reveal("session_note").performTextInput("Try tomorrow after lunch")
        assertEquals("",CallLists.read(ctx).single().entries.first().note)
        click("session_save_next");await {CallLists.read(ctx).single().entries.first().outcome=="busy"}
        assertEquals("Try tomorrow after lunch",CallLists.read(ctx).single().entries.first().note)
        reveal("session_name",true).assertTextEquals(second.name)
        assertFalse(CallLists.read(ctx).single().entries.first().done)
        assertEquals("pending",CallLists.read(ctx).single().entries.last().outcome)
        assertEquals(first.number,rule.activity.state.confirmNumber)
        click("session_outcome_done");click("session_save")
        await {CallLists.read(ctx).single().entries.last().done}
        click("session_back");click("list_filter_Done")
        reveal("list_entry_${second.id}",true).assertIsDisplayed()
        click("list_workspace_${second.id}");reveal("session_outcome_done").assertIsDisplayed()
    }
    @Test fun dirtyNavigationHasDiscardGuardAndNeverSavesByOpeningOrSkipping() {
        show();click("list_session");click("session_outcome_later")
        reveal("session_note").performTextInput("Unsaved")
        click("session_next");rule.onNodeWithTag("session_keep_editing").performClick();settle()
        assertEquals("",CallLists.read(ctx).single().entries.first().note)
        click("session_back");rule.onNodeWithTag("session_discard").performClick();settle()
        click("list_session");reveal("session_note").assertTextContains("Follow-up note")
        click("session_next");click("session_back")
        assertTrue(CallLists.read(ctx).single().entries.all {it.outcome=="pending" && it.note.isEmpty() && it.updatedAt==0L})
        assertNull(rule.activity.state.confirmNumber)
    }
    @Test fun reportPickerWritesExactTextAndCancelDoesNotChangeLists() {
        show()
        val before=CallLists.read(ctx)
        click("list_report")
        val file=File(ctx.cacheDir,"progress.csv")
        var started=org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivityForResult
        assertEquals("text/csv",started.intent.type)
        rule.runOnUiThread {org.robolectric.Shadows.shadowOf(rule.activity).receiveResult(started.intent,android.app.Activity.RESULT_OK,android.content.Intent().setData(android.net.Uri.fromFile(file)))}
        await {file.exists() && file.length()>0}
        assertEquals(CallListTransfer.report(before.single()),file.readText());assertEquals(before,CallLists.read(ctx))
        click("list_report");started=org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivityForResult
        rule.runOnUiThread {org.robolectric.Shadows.shadowOf(rule.activity).receiveResult(started.intent,android.app.Activity.RESULT_CANCELED,android.content.Intent())};settle()
        assertEquals(before,CallLists.read(ctx));assertNull(rule.activity.state.confirmNumber)
    }
    @Test fun oversizeNoteIsNotTruncatedAndCannotSave() {
        show();click("list_session")
        reveal("session_note").performTextInput("x".repeat(2001));settle()
        reveal("session_note_limit",true).assertIsDisplayed()
        reveal("session_save").assertIsNotEnabled();reveal("session_save_next").assertIsNotEnabled()
        reveal("session_note").assertTextContains("x".repeat(2001))
        assertTrue(CallLists.read(ctx).single().entries.first().note.isEmpty())
    }
    @Test fun dayWorkspacePixels()=pixels("daylight",1f,"day")
    @Test fun nightLargeWorkspacePixels()=pixels("midnight",1.3f,"night-large")
    private fun pixels(theme:String,font:Float,suffix:String) {
        show(theme,font)
        reveal("workspace_summary",true);settle();saveShot(rule,"workspace-overview-$suffix")
        click("list_session");reveal("session_person",true);settle();saveShot(rule,"workspace-person-$suffix")
        click("session_outcome_later")
        reveal("session_note").performTextInput("Discuss the appointment and call back after lunch.")
        reveal("session_back");settle();saveShot(rule,"workspace-note-$suffix")
        click("session_back");saveDialogShot("workspace-discard-$suffix",if(theme=="daylight")0xFFF0F4FF else 0xFF080D18)
        rule.onNodeWithTag("session_keep_editing").performClick();settle()
    }
}
