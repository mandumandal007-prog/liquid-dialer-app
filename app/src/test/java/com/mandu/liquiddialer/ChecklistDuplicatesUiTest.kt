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
class ChecklistDuplicatesUiTest {
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
 @Before fun seed(){android.util.AtomicFile(File(ctx.filesDir,"call_checklists.json")).delete();CallChecklist.add(ctx,n,"Confirm delivery time for household supplies");CallChecklist.add(ctx,n,"Unique question");CallChecklist.add(ctx,n,"CONFIRM   DELIVERY TIME FOR HOUSEHOLD SUPPLIES");CallChecklist.add(ctx,n,"घर के लिए सामान और delivery का समय confirm karo.");val rows=CallChecklist.add(ctx,n,"घर  के लिए सामान और delivery का समय confirm karo.");ids=rows.map{it.id};CallChecklist.done(ctx,n,ids[0],true);CallChecklist.done(ctx,n,ids[3],true);CallChecklist.done(ctx,n,ids[4],true)}
 private fun show(night:Boolean=false,entry:Boolean=false,broken:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.checklistDuplicates]=entry}};waitFor{LdApp.settings.value.checklistDuplicates==entry&&LdApp.settings.value.theme==(if(night)"midnight"else"daylight")};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{if(entry)CallChecklistPage(n,{})else ChecklistDuplicatesWorkspace(n,{},{})}};rule.activity.setContentView(host)};await(if(broken)"dupes_error"else if(entry)"checklist_duplicates_open"else"dupes_count")}
 @After fun reset(){rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.checklistDuplicates]=false}}}
 private fun dialog(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}
 private fun preview(){click("dupes_all");click("dupes_preview")}
 @Test fun defaultOffAndEntry(){assertFalse(LdSettings().checklistDuplicates);show(entry=true);click("checklist_duplicates_open");await("dupes_count");rule.onNodeWithTag("dupes_count",true).assertTextEquals("5 topics · 2 duplicate groups · 0 selected")}
 @Test fun previewDoesNotWriteAndConservativeTicks(){show();val before=File(ctx.filesDir,"call_checklists.json").readBytes();preview();rule.onNodeWithTag("dupes_totals",true).assertTextEquals("5 → 3 topics. Discussed 3 → 1. Any Pending copy stays Pending in its group.");rule.onNodeWithTag("dupes_kept_0",true).assertTextEquals("Pending · remove positions 3");assertArrayEquals(before,File(ctx.filesDir,"call_checklists.json").readBytes())}
 @Test fun cancelThenSaveSelectedOnly(){show();val old=CallChecklist.read(ctx,n);click("dupes_pick_0");click("dupes_preview");click("dupes_save");dialog("dupes_cancel");assertEquals(old,CallChecklist.read(ctx,n));click("dupes_save");dialog("dupes_confirm");waitFor{CallChecklist.read(ctx,n).size==4};val next=CallChecklist.read(ctx,n);assertEquals(old[0].id,next[0].id);assertFalse(next[0].done);assertEquals(old.drop(3),next.drop(2));rule.onAllNodesWithTag("dupes_result",true).assertCountEquals(0)}
 @Test fun selectClearAndChangeInvalidates(){show();preview();click("dupes_pick_0");rule.onAllNodesWithTag("dupes_result",true).assertCountEquals(0);click("dupes_preview");click("dupes_none");rule.onAllNodesWithTag("dupes_result",true).assertCountEquals(0);rule.onNodeWithTag("dupes_preview",true).assertIsNotEnabled()}
 @Test fun staleSaveRefusesAndReloadResets(){show();preview();CallChecklist.edit(ctx,n,ids[1],"Changed elsewhere");val f=File(ctx.filesDir,"call_checklists.json");val before=f.readBytes();click("dupes_save");dialog("dupes_confirm");await("dupes_error");assertArrayEquals(before,f.readBytes());rule.onAllNodesWithTag("dupes_save",true).assertCountEquals(0);click("dupes_reload");dialog("dupes_confirm");waitFor{rule.onAllNodesWithTag("dupes_error",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("dupes_count",true).assertTextEquals("5 topics · 2 duplicate groups · 0 selected")}
 @Test fun corruptLoadNoFalseEmptyOrWrite(){File(ctx.filesDir,"call_checklists.json").writeText("broken");show(broken=true);rule.onAllNodesWithTag("dupes_count",true).assertCountEquals(0);assertEquals("broken",File(ctx.filesDir,"call_checklists.json").readText())}
 @Test fun noDuplicatesClearEmptyState(){CallChecklist.clear(ctx,n);CallChecklist.add(ctx,n,"Unique");show();rule.onNodeWithTag("dupes_empty",true).assertExists();rule.onNodeWithTag("dupes_all",true).assertIsNotEnabled();rule.onNodeWithTag("dupes_preview",true).assertIsNotEnabled()}
 @Test fun emptyChecklistIsNotError(){CallChecklist.clear(ctx,n);show();rule.onNodeWithTag("dupes_count",true).assertTextEquals("0 topics · 0 duplicate groups · 0 selected");rule.onNodeWithTag("dupes_empty",true).assertExists()}
 @Test fun backKeepDiscardNoWrite(){show();val old=CallChecklist.read(ctx,n);preview();click("dupes_back");dialog("dupes_cancel");rule.onNodeWithTag("dupes_save",true).assertExists();click("dupes_back");dialog("dupes_confirm");assertEquals(old,CallChecklist.read(ctx,n))}
 @Test fun reloadConfirmationCancelKeepsPreview(){show();preview();click("dupes_reload");dialog("dupes_cancel");rule.onNodeWithTag("dupes_save",true).assertExists();click("dupes_reload");dialog("dupes_confirm");await("dupes_count");rule.onAllNodesWithTag("dupes_save",true).assertCountEquals(0)}
 @Test fun settingsFinderPersistencePixels(){show();assertTrue(SettingsFinder.all.any{it.id=="checklist-duplicates"&&it.route=="checklistduplicates"});rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{ChecklistDuplicatesPage({})}};rule.activity.setContentView(host)};settle();rule.onAllNodesWithText("Checklist duplicate review",substring=false).onLast().performClick();waitFor{LdApp.settings.value.checklistDuplicates};saveShot(rule,"DUPES447-settings")}
 @Test fun dayGroupsAndPreviewPixels(){show();scrollTo("dupes_original_0_0");saveShot(rule,"DUPES447-day-groups");preview();scrollTo("dupes_totals");saveShot(rule,"DUPES447-day-preview")}
 @Test fun nightLargeUnicodePreviewAndConfirmPixels(){show(night=true);click("dupes_all");scrollTo("dupes_original_1_0");saveShot(rule,"DUPES447-night-group");click("dupes_preview");scrollTo("dupes_kept_1");saveShot(rule,"DUPES447-night-preview");click("dupes_save");saveDialogShot("DUPES447-night-confirm",0xff0d1320)}
 @Test fun callNotesReturnClipboardOtherNumberAndTemplatesPreserved(){show();val clip=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("keep","Keep clipboard"));CallChecklist.add(ctx,"+449876543210","Foreign");runBlocking{ctx.repo.set(K.checklistDuplicates,true)};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}}};rule.activity.setContentView(host)};settle();rule.onNode(hasSetTextAction()).performTextInput("Unfinished note");click("call_checklist_open");await("checklist_duplicates_open");click("checklist_duplicates_open");await("dupes_count");preview();click("dupes_save");dialog("dupes_confirm");waitFor{CallChecklist.read(ctx,n).size==3};click("dupes_back");await("checklist_progress");rule.onNodeWithTag("checklist_progress",true).assertTextEquals("1 / 3 discussed");click("checklist_notes_back");rule.onNodeWithText("Unfinished note").assertExists();assertEquals("Keep clipboard",clip.primaryClip!!.getItemAt(0).text.toString());assertEquals("Foreign",CallChecklist.read(ctx,"+449876543210").single().text);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
}
