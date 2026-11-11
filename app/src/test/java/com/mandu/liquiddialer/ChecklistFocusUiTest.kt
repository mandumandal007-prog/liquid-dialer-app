package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
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
class ChecklistFocusUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private val n="+919876543210"
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(tag:String){rule.waitUntil(15000){settle();rule.onAllNodesWithTag(tag,true).fetchSemanticsNodes().isNotEmpty()}}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;val node=rule.onNodeWithTag(tag,true);node.performScrollTo();rule.mainClock.autoAdvance=false;node.performClick();settle()}
 private fun expectTopic(text:String){rule.waitUntil(15000){settle();rule.onAllNodesWithTag("focus_text",true).fetchSemanticsNodes().any{n->n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text){emptyList()}.any{it.text==text}}};rule.onNodeWithTag("focus_text",true).assertTextEquals(text)}
 private fun waitRows(test:(List<CallChecklist.Item>)->Boolean){rule.waitUntil(15000){settle();test(CallChecklist.read(ctx,n))}}
 @Before fun seed(){android.util.AtomicFile(File(ctx.filesDir,"call_checklists.json")).delete();CallChecklist.add(ctx,n,"Confirm delivery time");CallChecklist.add(ctx,n,"Ask quantity and rate");CallChecklist.add(ctx,n,"घर के लिए सामान, अगले दिन की delivery और अगली बात का समय confirm karo.")}
 private fun show(night:Boolean=false,sheet:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f}};rule.waitUntil(15000){LdApp.settings.value.theme==(if(night)"midnight"else"daylight")};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{if(sheet)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Papa",{})}else CallChecklistPage(n,{})}}};if(!sheet){await("checklist_focus");click("checklist_focus");await("focus_text")}}
 @Test fun navigationNeverMarksAndPendingAutoAdvancesOnlyOnExplicitMark(){show();val f=File(ctx.filesDir,"call_checklists.json");val before=f.readBytes();click("focus_next");expectTopic("Ask quantity and rate");click("focus_previous");assertArrayEquals(before,f.readBytes());click("focus_mark");waitRows{it.first().done};expectTopic("Ask quantity and rate");rule.onNodeWithTag("focus_progress",true).assertTextEquals("1 / 3 discussed · 2 Pending");click("focus_undo");waitRows{it.none{it.done}};expectTopic("Confirm delivery time");click("focus_filter");click("focus_mark");waitRows{it.first().done};expectTopic("Confirm delivery time");click("focus_mark");waitRows{it.none{it.done}};assertNull(rule.activity.state.confirmNumber)}
 @Test fun allDiscussedEmptyStillOffersUndoAndBackUpdatesChecklist(){show();repeat(3){click("focus_mark");waitRows{rows->rows.count{it.done}==it+1}};await("focus_empty");click("focus_undo");waitRows{it.count{it.done}==2};await("focus_text");click("focus_back");await("checklist_progress");rule.onNodeWithTag("checklist_progress",true).assertTextEquals("2 / 3 discussed")}
 @Test fun staleEditAndCorruptStorageBlockMarksUntilReload(){show();val old=CallChecklist.read(ctx,n).first();CallChecklist.edit(ctx,n,old.id,"Edited elsewhere");click("focus_mark");await("focus_error");rule.onNodeWithTag("focus_mark",true).assertIsNotEnabled();assertFalse(CallChecklist.read(ctx,n).first().done);click("focus_reload");rule.waitUntil(15000){settle();rule.onAllNodesWithText("Edited elsewhere").fetchSemanticsNodes().isNotEmpty()};File(ctx.filesDir,"call_checklists.json").writeText("broken");click("focus_mark");await("focus_error");assertEquals("broken",File(ctx.filesDir,"call_checklists.json").readText())}
 @Test fun dayFocusAndAllDonePixels(){show();rule.mainClock.autoAdvance=true;rule.onNodeWithTag("focus_text",true).performScrollTo();settle();saveShot(rule,"FC395-day");repeat(3){click("focus_mark");waitRows{rows->rows.count{it.done}==it+1}};await("focus_empty");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("focus_undo",true).performScrollTo();settle();saveShot(rule,"FC395-done")}
 @Test fun nightLargeLongTopicAndErrorPixels(){show(true);click("focus_next");click("focus_next");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("focus_next",true).performScrollTo();settle();saveShot(rule,"FC395-large");val old=CallChecklist.read(ctx,n).last();CallChecklist.edit(ctx,n,old.id,"Changed in other view");click("focus_mark");await("focus_error");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("focus_error",true).performScrollTo();settle();saveShot(rule,"FC395-error-large")}
 @Test fun callNotesDraftAndOtherNumberStayUntouched(){CallChecklist.add(ctx,"+449876543210","FOREIGN SECRET");show(sheet=true);rule.onNode(hasSetTextAction()).performTextInput("Unsaved note");click("call_checklist_open");await("checklist_focus");click("checklist_focus");await("focus_text");rule.onNodeWithText("FOREIGN SECRET").assertDoesNotExist();click("focus_mark");waitRows{it.first().done};click("focus_back");click("checklist_notes_back");rule.onNodeWithText("Unsaved note").assertExists();assertFalse(CallChecklist.read(ctx,"+449876543210").single().done)}
}
