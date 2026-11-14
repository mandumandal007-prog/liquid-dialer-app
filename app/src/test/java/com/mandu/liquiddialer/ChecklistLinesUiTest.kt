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
@OptIn(ExperimentalTestApi::class)
class ChecklistLinesUiTest {
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false}}
 }}
 private val dispatcher=kotlinx.coroutines.test.StandardTestDispatcher()
 @get:Rule(order=1) val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>(effectContext=dispatcher)
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private val n="+919876543210"
 private fun settle(){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);dispatcher.scheduler.runCurrent();rule.waitForIdle()}
 private fun await(tag:String){rule.waitUntil(15000){settle();rule.onAllNodesWithTag(tag,true).fetchSemanticsNodes().isNotEmpty()}}
 private fun scrollTo(tag:String){rule.mainClock.autoAdvance=true;repeat(5){val n=rule.onNodeWithTag(tag,true).fetchSemanticsNode();val root=rule.onRoot().fetchSemanticsNode();val delta=n.positionInRoot.y+n.size.height/2f-root.size.height/2f
 if(n.positionInRoot.y>=75&&n.positionInRoot.y+n.size.height<root.size.height-20){rule.mainClock.autoAdvance=false;settle();return}
 rule.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,delta)};rule.waitForIdle()};rule.mainClock.autoAdvance=false;settle()}
 private fun click(tag:String){scrollTo(tag);rule.onNodeWithTag(tag,true).assertIsEnabled().performClick();settle()}
 private var ids=listOf<String>()
 private fun waitFor(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 @Before fun seed(){android.util.AtomicFile(File(ctx.filesDir,"call_checklists.json")).delete();CallChecklist.add(ctx,n,"Confirm delivery time");CallChecklist.add(ctx,n,"Ask quantity and rate");val rows=CallChecklist.add(ctx,n,"घर के लिए सामान और delivery का समय confirm karo.");ids=rows.map{it.id};CallChecklist.done(ctx,n,ids[1],true)}
 private fun show(night:Boolean=false,entry:Boolean=false,broken:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.checklistLines]=entry}};waitFor{LdApp.settings.value.checklistLines==entry&&LdApp.settings.value.theme==(if(night)"midnight"else"daylight")};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{if(entry)CallChecklistPage(n,{})else ChecklistLinesWorkspace(n,{},{})}};rule.activity.setContentView(host)};await(if(broken)"lines_error"else if(entry)"checklist_lines_open"else"lines_capacity")}
 @After fun reset(){rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.checklistLines]=false}}}
 private fun input(text:String){scrollTo("lines_input");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("lines_input")),true).performTextReplacement(text);settle()}
 private fun dialog(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}
 private fun preview(text:String="New topic A\nNew topic B"){input(text);click("lines_preview")}
 @Test fun defaultOffAndEnabledEntry(){assertFalse(LdSettings().checklistLines);show(entry=true);click("checklist_lines_open");await("lines_capacity");rule.onNodeWithTag("lines_number",true).assertTextEquals(n)}
 @Test fun previewExplicitConfirmSavePreservesTicks(){show();val before=CallChecklist.read(ctx,n);preview();rule.onNodeWithTag("lines_count",true).assertTextEquals("2 new · 0 duplicate skips · 2 selected");assertEquals(before,CallChecklist.read(ctx,n));click("lines_save");dialog("lines_cancel");assertEquals(before,CallChecklist.read(ctx,n));click("lines_save");dialog("lines_confirm");waitFor{CallChecklist.read(ctx,n).size==5};val after=CallChecklist.read(ctx,n);assertEquals(before,after.take(3));assertTrue(after.drop(3).none{it.done});rule.onAllNodesWithTag("lines_save",true).assertCountEquals(0)}
 @Test fun duplicatePreviewCannotSelectSkipped(){show();preview("Confirm delivery time\nNew topic\nnew  topic");rule.onNodeWithTag("lines_count",true).assertTextEquals("1 new · 2 duplicate skips · 1 selected");rule.onAllNodesWithTag("lines_pick_1",true).assertCountEquals(0);rule.onAllNodesWithTag("lines_pick_3",true).assertCountEquals(0);click("lines_save");dialog("lines_confirm");waitFor{CallChecklist.read(ctx,n).size==4}}
 @Test fun selectedSubsetOnlyAndNoNewCase(){show();preview();click("lines_pick_1");click("lines_save");dialog("lines_confirm");waitFor{CallChecklist.read(ctx,n).size==4};assertEquals("New topic B",CallChecklist.read(ctx,n).last().text);preview("New topic B");rule.onNodeWithTag("lines_no_new",true).assertExists();rule.onNodeWithTag("lines_save",true).assertIsNotEnabled()}
 @Test fun editsInvalidateAndDeselectDisablesSave(){show();preview();input("Different");rule.onAllNodesWithTag("lines_save",true).assertCountEquals(0);click("lines_preview");click("lines_none");rule.onNodeWithTag("lines_save",true).assertIsNotEnabled();click("lines_all");rule.onNodeWithTag("lines_save",true).assertIsEnabled()}
 @Test fun capacitySelectSubsetNoPartialSave(){repeat(26){CallChecklist.add(ctx,n,"Extra $it")};show();preview();rule.onNodeWithTag("lines_limit",true).assertExists();rule.onNodeWithTag("lines_save",true).assertIsNotEnabled();assertEquals(29,CallChecklist.read(ctx,n).size);click("lines_pick_1");click("lines_save");dialog("lines_confirm");waitFor{CallChecklist.read(ctx,n).size==30};assertEquals("New topic B",CallChecklist.read(ctx,n).last().text)}
 @Test fun staleSaveRefusesAndReloadKeepsInput(){show();preview();CallChecklist.edit(ctx,n,ids[0],"Newer topic elsewhere");val bytes=File(ctx.filesDir,"call_checklists.json").readBytes();click("lines_save");dialog("lines_confirm");await("lines_error");assertArrayEquals(bytes,File(ctx.filesDir,"call_checklists.json").readBytes());rule.onAllNodesWithTag("lines_save",true).assertCountEquals(0);click("lines_reload");await("lines_capacity");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("lines_input")),true).assertTextEquals("New topic A\nNew topic B");click("lines_preview");click("lines_save");dialog("lines_confirm");waitFor{CallChecklist.read(ctx,n).size==5}}
 @Test fun corruptLoadNoFalseEmptyOrWrite(){File(ctx.filesDir,"call_checklists.json").writeText("broken");show(broken=true);rule.onAllNodesWithTag("lines_capacity",true).assertCountEquals(0);rule.onNodeWithTag("lines_preview",true).assertIsNotEnabled();assertEquals("broken",File(ctx.filesDir,"call_checklists.json").readText())}
 @Test fun malformedLongLineDoesNotChangeSavedFile(){show();val before=CallChecklist.read(ctx,n);preview("Valid\n"+"X".repeat(251));rule.onNodeWithTag("lines_error",true).assertTextContains("Line 2",substring=true);rule.onAllNodesWithTag("lines_save",true).assertCountEquals(0);assertEquals(before,CallChecklist.read(ctx,n))}
 @Test fun dirtyBackKeepThenDiscardNoFileEffects(){show();val before=CallChecklist.read(ctx,n);preview();click("page_back");dialog("lines_cancel");rule.onNodeWithTag("lines_save",true).assertExists();click("page_back");dialog("lines_confirm");assertEquals(before,CallChecklist.read(ctx,n))}
 @Test fun dayPreviewAndConfirmPixels(){show();preview("Confirm delivery time\nAsk for delivery location and the preferred time for next discussion\nNew question?\nnew question?");scrollTo("lines_input");saveShot(rule,"LINES440-day-input");scrollTo("lines_save");saveShot(rule,"LINES440-day-preview");click("lines_save");saveDialogShot("LINES440-day-confirm",0xffedf1ff)}
 @Test fun nightLargePreviewAndSettingsPixels(){show(true);preview("घर के लिए नया सवाल और अगली बात का समय confirm karo.\nAsk for delivery location and the preferred time for next discussion");scrollTo("lines_pick_1");saveShot(rule,"LINES440-night-preview");click("lines_save");saveDialogShot("LINES440-night-confirm",0xff0d1320);dialog("lines_cancel");rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{ChecklistOrganizerPage({})}};rule.activity.setContentView(host)};settle();saveShot(rule,"LINES440-settings");assertTrue(SettingsFinder.all.any{it.id=="checklist-lines"&&it.route=="checklistorganizer"})}
 @Test fun callNotesDraftReturnClipboardForeignPreserved(){show();val clipboard=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("keep","Keep clipboard"));CallChecklist.add(ctx,"+449876543210","Foreign");runBlocking{ctx.repo.set(K.checklistLines,true)};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}}};rule.activity.setContentView(host)};settle();rule.onNode(hasSetTextAction()).performTextInput("Unfinished note");click("call_checklist_open");await("checklist_lines_open");click("checklist_lines_open");await("lines_capacity");preview();click("lines_save");dialog("lines_confirm");waitFor{CallChecklist.read(ctx,n).size==5};click("lines_back");await("checklist_progress");rule.onNodeWithTag("checklist_progress",true).assertTextEquals("1 / 5 discussed");click("checklist_notes_back");rule.onNodeWithText("Unfinished note").assertExists();assertEquals("Keep clipboard",clipboard.primaryClip!!.getItemAt(0).text.toString());assertEquals("Foreign",CallChecklist.read(ctx,"+449876543210").single().text);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
}
