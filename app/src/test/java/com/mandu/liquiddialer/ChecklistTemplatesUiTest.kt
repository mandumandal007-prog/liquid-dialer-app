package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import org.robolectric.annotation.*
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChecklistTemplatesUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private val n="+919876543210"
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(tag:String){try{rule.waitUntil(15000){settle();rule.onAllNodesWithTag(tag,true).fetchSemanticsNodes().isNotEmpty()}}catch(e:Exception){throw AssertionError("Missing $tag",e)}}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;val node=rule.onNodeWithTag(tag,true);node.performScrollTo();rule.mainClock.autoAdvance=false;node.performClick();settle()}
 private fun type(tag:String,text:String){rule.mainClock.autoAdvance=true;val node=rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true);node.performScrollTo();node.performTextReplacement(text);settle()}
 @Before fun seed(){listOf("call_checklists.json","checklist_templates.json").forEach{f->android.util.AtomicFile(File(ctx.filesDir,f)).delete()};val old=CallChecklist.add(ctx,n,"Kaunsa item aur kitni quantity?").single();CallChecklist.done(ctx,n,old.id,true)}
 private fun show(night:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f}};rule.waitUntil(15000){LdApp.settings.value.theme==(if(night)"midnight"else"daylight")};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallChecklistPage(n,{})}}};await("checklist_templates");click("checklist_templates");await("tpl_new")}
 @Test fun previewAppendPreservesTicksAndPreventsRepeat(){show();click("tpl_open_starter_order");rule.onNodeWithTag("tpl_preview_counts",true).assertTextEquals("3 naye topics · 1 pehle se maujood · 29 jagah");val bytes=File(ctx.filesDir,"call_checklists.json").readBytes();assertEquals(1,CallChecklist.read(ctx,n).size);click("tpl_apply");await("tpl_applied");assertEquals(4,CallChecklist.read(ctx,n).size);assertTrue(CallChecklist.read(ctx,n).first().done);rule.onNodeWithTag("tpl_apply",true).assertIsNotEnabled();click("tpl_back");click("tpl_back");await("checklist_progress");rule.onNodeWithTag("checklist_progress",true).assertTextEquals("1 / 4 discussed");assertNull(rule.activity.state.confirmNumber)}
 @Test fun createFromCurrentEditCancelAndDeleteNeverAffectChecklist(){show();click("tpl_from_current");type("tpl_title","My order");click("tpl_save");await("tpl_applied");val t=ChecklistTemplates.read(ctx).single();assertEquals(listOf("Kaunsa item aur kitni quantity?"),t.topics);click("tpl_edit_${t.id}");type("tpl_title","Unsaved change");click("tpl_back");rule.onNodeWithTag("tpl_keep",true).performClick();settle();rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("tpl_title")),true).assertTextContains("Unsaved change");click("tpl_back");rule.onNodeWithTag("tpl_discard",true).performClick();settle();assertEquals("My order",ChecklistTemplates.read(ctx).single().title);click("tpl_delete_${t.id}");rule.onNodeWithTag("tpl_delete_confirm",true).performClick();settle();rule.waitUntil(15000){ChecklistTemplates.read(ctx).isEmpty()};assertTrue(CallChecklist.read(ctx,n).single().done)}
 @Test fun dayLibraryAndPreviewPixels(){ChecklistTemplates.save(ctx,null,"Delivery follow-up",listOf("Confirm delivery time","Ask about missing items"));show();rule.onNodeWithTag("tpl_new",true).performScrollTo();settle();saveShot(rule,"TPL393-library-day");click("tpl_open_starter_order");rule.onNodeWithTag("tpl_preview_counts",true).performScrollTo();settle();saveShot(rule,"TPL393-preview-day");click("tpl_back");click("tpl_new");type("tpl_title","Weekly order");type("tpl_body","Confirm quantity\n\nConfirm pickup time");rule.onNodeWithTag("tpl_title",true).performScrollTo();settle();saveShot(rule,"TPL393-editor-day")}
 @Test fun nightLargeAndCapacityPixels(){repeat(28){CallChecklist.add(ctx,n,"Existing $it")};show(true);rule.onNodeWithTag("tpl_new",true).performScrollTo();settle();saveShot(rule,"TPL393-library-large");click("tpl_open_starter_order");rule.onNodeWithTag("tpl_preview_counts",true).performScrollTo();settle();rule.onNodeWithTag("tpl_apply",true).assertIsNotEnabled();saveShot(rule,"TPL393-capacity-large");assertEquals(29,CallChecklist.read(ctx,n).size)}
 @Test fun corruptTemplateAndStaleChecklistFailWithoutOverwriting(){File(ctx.filesDir,"checklist_templates.json").writeText("broken");settle();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{ChecklistTemplatesWorkspace(n,{},{})}}};await("tpl_error");rule.onNodeWithTag("tpl_new",true).assertDoesNotExist();click("tpl_retry");await("tpl_error");assertEquals("broken",File(ctx.filesDir,"checklist_templates.json").readText());File(ctx.filesDir,"checklist_templates.json").delete();click("tpl_retry");await("tpl_new");click("tpl_open_starter_order");File(ctx.filesDir,"call_checklists.json").writeText("broken checklist");click("tpl_apply");await("tpl_error");assertEquals("broken checklist",File(ctx.filesDir,"call_checklists.json").readText())}
 @Test fun callNotesDraftSurvivesTemplatesAndMultilineCurrentIsPreserved(){
  val text="Line one\n\nSame topic continuation";CallChecklist.add(ctx,n,text)
  settle();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Papa",{})}}}};settle()
  rule.onNode(hasSetTextAction()).performTextInput("Unsaved call note");click("call_checklist_open");await("checklist_templates");click("checklist_templates");await("tpl_new");click("tpl_from_current");type("tpl_title","Current call");click("tpl_save");await("tpl_applied");assertEquals(text,ChecklistTemplates.read(ctx).single().topics.last());click("tpl_back");click("checklist_notes_back");rule.onNodeWithText("Unsaved call note").assertExists();assertNull(rule.activity.state.confirmNumber)
 }

}
