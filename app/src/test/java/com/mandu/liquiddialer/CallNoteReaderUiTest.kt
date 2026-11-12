package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.LdApp
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallNoteReaderUiTest:CallCanvasUiFixture(){
 private fun mount(night:Boolean=false,many:Boolean=false,enabled:Boolean=true){seed(night);runBlocking{ctx.repo.set(K.callNoteReader,enabled)};await{LdApp.settings.value.callNoteReader==enabled};File(ctx.filesDir,"notes.json").delete();File(ctx.filesDir,"notes.json.bak").delete();Repo.addNote(ctx,number,"Support","Gate Alpha\n"+"details line\n".repeat(30)+"Tail marker Alpha");Repo.addNote(ctx,"+449876543210","Other","private other person");if(many)repeat(14){Repo.addNote(ctx,number,"Support","Memo $it")};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Sheet({}){NotesSheet(number,"Support",{})}}}};await{rule.onAllNodesWithTag(if(enabled)"call_notes_reader_open"else"notes_identity_name",true).fetchSemanticsNodes().isNotEmpty()}}
 private fun open(){rule.onNodeWithTag("call_notes_reader_open",true).performClick();await{rule.onAllNodesWithTag("call_reader_count",true).fetchSemanticsNodes().isNotEmpty()&&rule.onAllNodesWithTag("call_reader_loading",true).fetchSemanticsNodes().isEmpty()}}
 private fun q(value:String){field("call_reader_query").performTextReplacement(value);settle()}
 private fun scroll(tag:String){rule.mainClock.autoAdvance=true;repeat(5){val node=rule.onNodeWithTag(tag,true).fetchSemanticsNode();val root=rule.onRoot().fetchSemanticsNode();val d=node.positionInRoot.y+node.size.height/2f-root.size.height/2f;if(kotlin.math.abs(d)<4)return@repeat;rule.onNodeWithTag("call_reader_scroll",true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,d)};rule.waitForIdle()};settle()}
 @Test fun fullNoteSearchFindAndNoFilesOrCallsChanged(){mount();val before=Repo.notesChecked(ctx);open();q("Alpha");val id=before.first{it.number==number}.id;rule.onNodeWithTag("call_reader_open_$id",true).performClick();await{rule.onAllNodesWithTag("note_full_text",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("note_full_text",true).assertTextContains("Tail marker Alpha",substring=true);rule.onNodeWithTag("note_find_count",true).assertTextEquals("1 / 2 matches");rule.onNodeWithTag("note_find_next",true).performClick();settle();rule.onNodeWithTag("note_find_count",true).assertTextEquals("2 / 2 matches");saveShot(rule,"READER425-tail-match");rule.onNodeWithTag("note_find_prev",true).performClick();settle();rule.onNodeWithTag("note_find_count",true).assertTextEquals("1 / 2 matches");assertEquals(before,Repo.notesChecked(ctx));assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity);saveShot(rule,"READER425-full")}
 @Test fun otherNumberNeverShownAndNoMatchIsHonest(){mount();open();q("private other");rule.onNodeWithTag("call_reader_count",true).assertTextEquals("Koi saved text match nahi");q("");rule.onNodeWithTag("call_reader_count",true).assertTextEquals("1-1 / 1 notes")}
 @Test fun unfinishedDraftSurvivesReaderRoundTrip(){mount();rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("call_reader_query")),true).assertDoesNotExist();val input=rule.onAllNodes(hasSetTextAction(),true);input[0].performTextInput("Do not lose my draft");open();rule.onNodeWithTag("call_reader_back",true).performClick();settle();rule.onNode(hasSetTextAction(),true).assertTextContains("Do not lose my draft",substring=true)}
 @Test fun boundedPagesAndSearchReset(){mount(many=true);open();rule.onNodeWithTag("call_reader_count",true).assertTextEquals("1-12 / 15 notes");scroll("call_reader_next");rule.onNodeWithTag("call_reader_next",true).performClick();settle();rule.onNodeWithTag("call_reader_count",true).assertTextEquals("13-15 / 15 notes");scroll("call_reader_query");q("Gate");rule.onNodeWithTag("call_reader_count",true).assertTextEquals("1-1 / 1 notes")}
 @Test fun defaultOffAndExplicitToggleDoesNotDeleteNotes(){assertFalse(LdSettings().callNoteReader);mount(enabled=false);rule.onAllNodesWithTag("call_notes_reader_open",true).assertCountEquals(0);val before=Repo.notesChecked(ctx);runBlocking{ctx.repo.set(K.callNoteReader,true)};await{rule.onAllNodesWithTag("call_notes_reader_open",true).fetchSemanticsNodes().isNotEmpty()};open();assertEquals(before,Repo.notesChecked(ctx))}
 @Test fun corruptNotesShowErrorNotFalseEmptyAndPreserveFile(){mount();File(ctx.filesDir,"notes.json").writeText("broken");open();rule.onNodeWithTag("call_reader_error",true).assertExists();rule.onNodeWithTag("call_reader_count",true).assertTextEquals("Notes unavailable");assertEquals("broken",File(ctx.filesDir,"notes.json").readText())}
 @Test fun backToResultsKeepsSearchAndNeverEditsSnapshot(){mount();open();q("Alpha");val id=Repo.notesChecked(ctx).first{it.number==number}.id;rule.onNodeWithTag("call_reader_open_$id",true).performClick();settle();rule.onNodeWithTag("call_reader_back",true).performClick();settle();field("call_reader_query").assertTextEquals("Alpha");rule.onNodeWithTag("call_reader_count",true).assertTextEquals("1-1 / 1 notes")}
 @Test fun settingsPixelsAndFinderRoute(){seed();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallNoteReaderPage({})}}};settle();saveShot(rule,"READER425-settings");assertTrue(SettingsFinder.all.any{it.route=="callnotereader"})}
 @Test fun dayListPixels(){mount();open();settle();saveShot(rule,"READER425-day")}
 @Test fun nightLargeReaderPixels(){mount(true);open();q("Gate");settle();saveShot(rule,"READER425-night-large")}
}
