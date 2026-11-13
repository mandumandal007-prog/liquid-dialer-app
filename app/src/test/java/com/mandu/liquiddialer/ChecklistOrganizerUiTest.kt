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
class ChecklistOrganizerUiTest {
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
 private fun show(night:Boolean=false,entry:Boolean=false,broken:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.checklistOrganizer]=entry}};waitFor{LdApp.settings.value.checklistOrganizer==entry&&LdApp.settings.value.theme==(if(night)"midnight"else"daylight")};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{if(entry)CallChecklistPage(n,{})else ChecklistOrganizerWorkspace(n,{},{})}};rule.activity.setContentView(host)};await(if(broken)"organizer_error"else if(entry)"checklist_organizer_open"else"organizer_state")}
 @After fun reset(){rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.checklistOrganizer]=false}}}
 private fun query(text:String){rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("organizer_query")),true).performTextReplacement(text);settle()}
 @Test fun batchPreviewUndoAndExplicitSave(){show();val f=File(ctx.filesDir,"call_checklists.json");val bytes=f.readBytes();click("organizer_select_all");click("organizer_done");assertArrayEquals(bytes,f.readBytes());rule.onNodeWithTag("organizer_state",true).assertTextEquals("Unsaved preview");click("organizer_undo");rule.onNodeWithTag("organizer_state",true).assertTextEquals("Saved checklist");click("organizer_select_${ids[0]}");click("organizer_done");click("organizer_save");waitFor{CallChecklist.read(ctx,n).first().done&&rule.onAllNodesWithText("Saved checklist").fetchSemanticsNodes().isNotEmpty()};assertFalse(CallChecklist.read(ctx,n).last().done);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun reorderSaveAndReopenPreservesIdsAndTicks(){show();val b=CallChecklist.read(ctx,n);click("organizer_up_${ids[2]}");assertEquals(b,CallChecklist.read(ctx,n));click("organizer_save");waitFor{CallChecklist.read(ctx,n).map{it.id}==listOf(ids[0],ids[2],ids[1])};assertTrue(CallChecklist.read(ctx,n).last().done);show();rule.onNodeWithTag("organizer_state",true).assertTextEquals("Saved checklist")}
 @Test fun searchFilterSelectionAndHiddenRowsNotMarked(){show();click("organizer_select_all");query("delivery");rule.onNodeWithTag("organizer_count",true).assertTextContains("0 selected",substring=true);rule.onAllNodesWithTag("organizer_up_${ids[0]}",true).assertCountEquals(0);click("organizer_select_all");click("organizer_done");click("organizer_save");waitFor{CallChecklist.read(ctx,n).all{it.done}};click("organizer_filter_pending");await("organizer_empty");click("organizer_filter_done");rule.onNodeWithTag("organizer_count",true).assertTextContains("2 visible",substring=true);query(".*");await("organizer_empty")}
 @Test fun pendingOnlyBatchNeverTouchesOtherNumber(){CallChecklist.add(ctx,"+449876543210","Foreign secret");show();click("organizer_filter_done");click("organizer_select_all");click("organizer_pending");click("organizer_save");waitFor{CallChecklist.read(ctx,n).none{it.done}};assertFalse(CallChecklist.read(ctx,"+449876543210").single().done);rule.onNodeWithText("Foreign secret").assertDoesNotExist()}
 @Test fun staleSaveKeepsDiskAndPreviewUntilExplicitReload(){show();click("organizer_down_${ids[0]}");CallChecklist.edit(ctx,n,ids[0],"Changed elsewhere");val f=File(ctx.filesDir,"call_checklists.json");val bytes=f.readBytes();click("organizer_save");await("organizer_error");assertArrayEquals(bytes,f.readBytes());rule.onNodeWithTag("organizer_save",true).assertIsNotEnabled();rule.onNodeWithTag("organizer_state",true).assertTextEquals("Unsaved preview");click("organizer_reload");await("organizer_discard");rule.onNodeWithTag("organizer_discard",true).performClick();await("organizer_text_${ids[0]}");waitFor{rule.onAllNodesWithText("Changed elsewhere").fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("organizer_state",true).assertTextEquals("Saved checklist")}
 @Test fun corruptLoadPreservesFileNoFalseEmpty(){File(ctx.filesDir,"call_checklists.json").writeText("broken");show(broken=true);await("organizer_error");rule.onAllNodesWithTag("organizer_save",true).assertCountEquals(0);assertEquals("broken",File(ctx.filesDir,"call_checklists.json").readText())}
 @Test fun dirtyBackNeedsExplicitDiscard(){show();click("organizer_select_all");click("organizer_done");click("page_back");await("organizer_keep");rule.onNodeWithTag("organizer_keep",true).performClick();settle();rule.onNodeWithTag("organizer_state",true).assertTextEquals("Unsaved preview");assertEquals(1,CallChecklist.read(ctx,n).count{it.done})}
 @Test fun defaultOffAndEntryWhenEnabled(){assertFalse(LdSettings().checklistOrganizer);show(entry=true);click("checklist_organizer_open");await("organizer_state");rule.onNodeWithTag("organizer_number",true).assertTextEquals(n);click("organizer_select_all");click("organizer_done");click("organizer_save");waitFor{CallChecklist.read(ctx,n).all{it.done}};click("page_back");await("checklist_progress");rule.onNodeWithTag("checklist_progress",true).assertTextEquals("3 / 3 discussed")}
 @Test fun dayPreviewPixels(){show();click("organizer_select_${ids[0]}");scrollTo("organizer_count");settle();saveShot(rule,"ORGANIZER427-day");click("organizer_up_${ids[2]}");scrollTo("organizer_text_${ids[2]}");settle();saveShot(rule,"ORGANIZER427-order")}
 @Test fun nightLargeTopicAndSettingsPixels(){show(true);scrollTo("organizer_text_${ids[2]}");settle();saveShot(rule,"ORGANIZER427-night-large");rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{ChecklistOrganizerPage({})}};rule.activity.setContentView(host)};settle();saveShot(rule,"ORGANIZER427-settings")}
 @Test fun callNotesDraftReturnAndOffEntry(){show();runBlocking{ctx.repo.set(K.checklistOrganizer,false)};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}}};rule.activity.setContentView(host)};settle();rule.onNode(hasSetTextAction()).performTextInput("Unfinished call note");click("call_checklist_open");await("checklist_progress");rule.onAllNodesWithTag("checklist_organizer_open",true).assertCountEquals(0);runBlocking{ctx.repo.set(K.checklistOrganizer,true)};await("checklist_organizer_open");click("checklist_organizer_open");await("organizer_state");click("organizer_select_all");click("organizer_done");click("organizer_save");waitFor{CallChecklist.read(ctx,n).all{it.done}};click("organizer_back");await("checklist_progress");rule.onNodeWithTag("checklist_progress",true).assertTextEquals("3 / 3 discussed");click("checklist_notes_back");rule.onNodeWithText("Unfinished call note").assertExists();assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun nightLargeControlsPixels(){show(true);click("organizer_filter_pending");click("organizer_select_all");scrollTo("organizer_count");settle();saveShot(rule,"ORGANIZER427-night-controls");assertTrue(SettingsFinder.all.any{it.id=="checklist-organizer"&&it.route=="checklistorganizer"})}

}
