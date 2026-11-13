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
class TallyBatchUiTest {
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false;it[K.tallyBatch]=false}}
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
 private var focus:androidx.compose.ui.focus.FocusManager?=null
 private fun waitFor(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 private fun show(night:Boolean=false,notes:Boolean=false,enabled:Boolean=true,direct:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.callTally]=false;it[K.tallyBatch]=enabled}};waitFor{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.callTally&&it.tallyBatch==enabled}};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{if(notes)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}else if(direct)TallyBatchWorkspace(sample,{closed=true},{applied=it})else CallTallyWorkspace({})}};rule.activity.setContentView(host)};settle()}
 private fun input(tag:String,s:String){scrollTo(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(s);settle()}
 @After fun reset(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};settle();rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.callTally]=false;it[K.tallyBatch]=false}}}
 private fun add(name:String,value:String="0"){input("tally_name",name);input("tally_value",value);click("tally_apply")}
 private val sample=listOf(CallTally.Item("a","Rice premium large family pack bags with matching unit",24),CallTally.Item("b","Sugar bags",12),CallTally.Item("c","Other kg",5))
 private var applied:TallyBatch.Preview?=null;private var closed=false
 private fun dialog(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}
 @Test fun defaultOffHidesEntry(){show(enabled=false);rule.onAllNodesWithTag("tally_batch_open",true).assertCountEquals(0);assertFalse(LdSettings().tallyBatch)}
 @Test fun emptyBoardCannotPreview(){show();click("tally_batch_open");await("batch_empty");rule.onNodeWithTag("batch_all",true).assertIsNotEnabled();rule.onNodeWithTag("batch_preview",true).assertIsNotEnabled()}
 @Test fun selectedPreviewOnlyThenCancelAndConfirm(){show(direct=true);click("batch_pick_0");click("batch_pick_1");input("batch_operand","3");click("batch_preview");rule.onNodeWithTag("batch_change_0",true).assertTextEquals("24 → 27");rule.onAllNodesWithTag("batch_change_2",true).assertCountEquals(0);assertNull(applied);click("batch_apply");dialog("batch_cancel");assertNull(applied);click("batch_apply");dialog("batch_confirm");assertEquals(listOf(27L,15L,5L),applied!!.after.map{it.value})}
 @Test fun changingOperandOrSelectionInvalidatesPreview(){show(direct=true);click("batch_all");click("batch_preview");input("batch_operand","2");rule.onAllNodesWithTag("batch_apply",true).assertCountEquals(0);click("batch_preview");click("batch_pick_1");rule.onAllNodesWithTag("batch_apply",true).assertCountEquals(0);click("batch_preview");click("batch_op_SET");rule.onAllNodesWithTag("batch_apply",true).assertCountEquals(0)}
 @Test fun subtractAtomicRefusalAndMalformedInput(){show(direct=true);click("batch_all");click("batch_op_SUBTRACT");input("batch_operand","10");click("batch_preview");rule.onNodeWithTag("batch_error",true).assertTextContains("Whole batch refused",substring=true);rule.onAllNodesWithTag("batch_apply",true).assertCountEquals(0);assertNull(applied);input("batch_operand","1.5");click("batch_preview");rule.onNodeWithTag("batch_error",true).assertExists()}
 @Test fun multiplyAndSetPreviewAllSelected(){show(direct=true);click("batch_all");click("batch_op_MULTIPLY");input("batch_operand","2");click("batch_preview");rule.onNodeWithTag("batch_change_0",true).assertTextEquals("24 → 48");rule.onNodeWithTag("batch_change_2",true).assertTextEquals("5 → 10");click("batch_op_SET");input("batch_operand","0");click("batch_preview");rule.onNodeWithTag("batch_change_2",true).assertTextEquals("5 → 0")}
 @Test fun noOpCannotApply(){show(direct=true);click("batch_all");input("batch_operand","0");click("batch_preview");rule.onNodeWithTag("batch_no_change",true).assertExists();rule.onNodeWithTag("batch_apply",true).assertIsNotEnabled();assertNull(applied)}
 @Test fun returnNeverAppliesPreview(){show(direct=true);click("batch_all");click("batch_preview");click("batch_back");assertTrue(closed);assertNull(applied)}
 @Test fun batchOneUndoAndRedoKeepsStepAndClearsEditor(){show();add("Rice bags","24");add("Sugar bags","12");input("tally_step","5");input("tally_name","Unfinished form");click("tally_batch_open");click("batch_all");click("batch_op_MULTIPLY");input("batch_operand","2");click("batch_preview");click("batch_apply");dialog("batch_confirm");rule.onNodeWithTag("tally_total",true).assertTextEquals("72");rule.onNodeWithTag("tally_editor",true).assertTextEquals("New counter");click("tally_undo");rule.onNodeWithTag("tally_total",true).assertTextEquals("36");click("tally_redo");rule.onNodeWithTag("tally_total",true).assertTextEquals("72");click("tally_plus_0");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("53")}
 @Test fun deselectAndBackPreservesUnfinishedEditor(){show();add("Bags","12");input("tally_name","Keep this draft");click("tally_batch_open");click("batch_all");click("batch_none");rule.onNodeWithTag("batch_count",true).assertTextEquals("0 / 1 selected");click("batch_back");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("tally_name")),true).assertTextEquals("Keep this draft");rule.onNodeWithTag("tally_total",true).assertTextEquals("12")}
 @Test fun noFilesClipboardOrCallEffects(){show();val clipboard=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("existing","Keep clipboard"));val before=ctx.filesDir.walkTopDown().filter{it.isFile&&it.extension=="json"}.associate{it.path to it.readBytes()};add("Bags","7");click("tally_batch_open");click("batch_all");click("batch_preview");click("batch_apply");dialog("batch_confirm");before.forEach{(f,b)->assertArrayEquals(b,File(f).readBytes())};assertEquals("Keep clipboard",clipboard.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity);show();rule.onNodeWithTag("tally_total",true).assertTextEquals("0")}
 @Test fun settingsSwitchPersistsAndFinder(){show(enabled=false);assertTrue(SettingsFinder.all.any{it.id=="tally-batch"&&it.route=="calltally"});rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{CallTallyPage({})}};rule.activity.setContentView(host)};settle();rule.onNodeWithText("Tally batch quantities").performClick();waitFor{LdApp.settings.value.tallyBatch};saveShot(rule,"BATCH439-settings");click("tally_settings_open");rule.onNodeWithTag("tally_batch_open",true).assertExists()}
 @Test fun daySelectionAndPreviewPixels(){show(direct=true);click("batch_pick_0");click("batch_pick_1");scrollTo("batch_count");saveShot(rule,"BATCH439-day-selection");input("batch_operand","6");click("batch_preview");scrollTo("batch_change_0");saveShot(rule,"BATCH439-day-preview");click("batch_apply");saveDialogShot("BATCH439-day-confirm",0xffedf1ff)}
 @Test fun nightLargePreviewPixels(){show(night=true,direct=true);click("batch_all");click("batch_op_MULTIPLY");input("batch_operand","2");click("batch_preview");scrollTo("batch_preview_state");saveShot(rule,"BATCH439-night-preview");scrollTo("batch_apply");saveShot(rule,"BATCH439-night-apply");click("batch_apply");saveDialogShot("BATCH439-night-confirm",0xff0d1320)}
 @Test fun sourceChangeInvalidatesPreviewAndRequiresNewSelection(){show(direct=true);val source=androidx.compose.runtime.mutableStateOf(sample);rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{TallyBatchWorkspace(source.value,{}, {applied=it})}};rule.activity.setContentView(host)};settle();click("batch_all");click("batch_preview");rule.runOnUiThread{source.value=sample.drop(1)};settle();rule.onAllNodesWithTag("batch_apply",true).assertCountEquals(0);rule.onNodeWithTag("batch_count",true).assertTextEquals("0 / 2 selected");click("batch_all");click("batch_preview");rule.onNodeWithTag("batch_change_0",true).assertTextEquals("12 → 13");assertNull(applied)}
 @Test fun callSheetBatchPreservesNoteDraftAndNeverSavesCounts(){show(notes=true);rule.onNode(hasSetTextAction()).performTextInput("Keep note draft");runBlocking{ctx.repo.set(K.callTally,true)};await("call_tally_open");click("call_tally_open");add("Bags","7");click("tally_batch_open");click("batch_all");click("batch_preview");click("batch_apply");dialog("batch_confirm");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("8");click("tally_back");dialog("tally_confirm");rule.onNodeWithText("Keep note draft").assertExists();click("call_tally_open");rule.onNodeWithTag("tally_total",true).assertTextEquals("0")}

}
