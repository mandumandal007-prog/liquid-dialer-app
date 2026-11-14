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
class EstimateSplitUiTest {
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false;it[K.estimateSplit]=false}}
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
 private var closed=false
 private val sample=CallEstimate.Draft(lines=listOf(CallEstimate.line("Rice bags","1","10.00","a")))
 private fun show(night:Boolean=false,notes:Boolean=false,enabled:Boolean=true,direct:Boolean=true){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.estimateSplit]=enabled}};waitFor{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&it.estimateSplit==enabled}};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{if(notes)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}else if(direct)EstimateSplitWorkspace(n,sample,{closed=true})else CallEstimatePage(n,{})}};rule.activity.setContentView(host)};if(!direct&&!notes)await("estimate_total")else settle()}
 private fun input(tag:String,s:String){scrollTo(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(s);settle()}
 @After fun reset(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};settle();rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.callTally]=false;it[K.estimateSplit]=false}}}
 private fun dialog(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}


 @Before fun seed(){android.util.AtomicFile(File(ctx.filesDir,"call_estimates.json")).delete();CallEstimate.save(ctx,n,0,sample)}
 @Test fun defaultOffEntryHiddenAndEmptyDisabled(){show(enabled=false,direct=false);assertFalse(LdSettings().estimateSplit);rule.onAllNodesWithTag("estimate_split",true).assertCountEquals(0);show(direct=false);click("estimate_split");await("split_base")}
 @Test fun equalPreviewRemainderAndNoFileWrite(){show();val before=File(ctx.filesDir,"call_estimates.json").readBytes();click("split_add");click("split_preview");rule.onNodeWithTag("split_amount_0",true).assertTextEquals("3.34");rule.onNodeWithTag("split_amount_1",true).assertTextEquals("3.33");rule.onNodeWithTag("split_extra_0",true).assertExists();rule.onNodeWithTag("split_sum",true).assertTextEquals("Share sum: 10.00 · matches 10.00");assertArrayEquals(before,File(ctx.filesDir,"call_estimates.json").readBytes())}
 @Test fun weightsRatiosPreviewAndModeInvalidates(){show();click("split_add");click("split_weighted");input("split_weight_1","2");click("split_preview");rule.onNodeWithTag("split_amount_1",true).assertTextEquals("5.00");click("split_equal");rule.onAllNodesWithTag("split_result",true).assertCountEquals(0);click("split_weighted");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("split_weight_1")),true).assertTextEquals("2")}
 @Test fun nameAndWeightEditInvalidatesAndDuplicatesRefuse(){show();click("split_preview");input("split_name_1","Share 1");rule.onAllNodesWithTag("split_result",true).assertCountEquals(0);click("split_preview");rule.onNodeWithTag("split_error",true).assertExists();input("split_name_1","Other");click("split_weighted");input("split_weight_1","0");click("split_preview");rule.onNodeWithTag("split_error",true).assertExists();assertFalse(closed)}
 @Test fun removeAndAddInvalidateAndBounds(){show();rule.onNodeWithTag("split_remove_0",true).assertIsNotEnabled();repeat(10){click("split_add")};rule.onNodeWithTag("split_add",true).assertIsNotEnabled();input("split_name_11","Last share");click("split_preview");rule.onNodeWithTag("split_count",true).assertTextEquals("12 /12 named shares");rule.onNodeWithTag("split_amount_11",true).assertExists();click("split_remove_11");rule.onAllNodesWithTag("split_result",true).assertCountEquals(0);rule.onNodeWithTag("split_count",true).assertTextEquals("11 /12 named shares")}
 @Test fun dirtyBackKeepThenDiscard(){show();click("split_weighted");click("split_back");dialog("split_keep");assertFalse(closed);click("split_back");dialog("split_discard");assertTrue(closed)}
 @Test fun untouchedBackNoDiscard(){show();click("split_back");assertTrue(closed);rule.onAllNodesWithTag("split_discard",true).assertCountEquals(0)}
 @Test fun sourceChangeResetsPreviewInputsAndDialog(){show();val src=androidx.compose.runtime.mutableStateOf(sample);rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateSplitWorkspace(n,src.value,{})}};rule.activity.setContentView(host)};settle();input("split_name_0","Changed");click("split_preview");click("split_back");rule.runOnUiThread{src.value=sample.copy(discount="2")};settle();rule.onAllNodesWithTag("split_result",true).assertCountEquals(0);rule.onAllNodesWithTag("split_discard",true).assertCountEquals(0);rule.onNodeWithTag("split_base",true).assertTextEquals("Estimate total: 8.00");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("split_name_0")),true).assertTextEquals("Share 1")}
 @Test fun calculatorUnsavedDraftRetainedNoSaveAndClipboardEffects(){show(direct=false);val f=File(ctx.filesDir,"call_estimates.json");val before=f.readBytes();val clipboard=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("keep","Keep clipboard"));input("estimate_discount","1");click("estimate_split");rule.onNodeWithTag("split_base",true).assertTextEquals("Estimate total: 9.00");click("split_preview");click("split_back");await("estimate_total");rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 9.00");rule.onNodeWithTag("estimate_status",true).assertTextEquals("Unsaved changes");assertArrayEquals(before,f.readBytes());assertEquals("Keep clipboard",clipboard.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun invalidSourceNoPreview(){show();rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateSplitWorkspace(n,CallEstimate.Draft(),{})}};rule.activity.setContentView(host)};settle();rule.onNodeWithTag("split_source_error",true).assertExists();rule.onNodeWithTag("split_preview",true).assertIsNotEnabled()}
 @Test fun settingsFinderPersistPixels(){show(enabled=false);assertTrue(SettingsFinder.all.any{it.id=="estimate-split"&&it.route=="estimatesplit"});rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateSplitPage({})}};rule.activity.setContentView(host)};settle();rule.onAllNodesWithText("Calculator split estimate",substring=false).onLast().performClick();waitFor{LdApp.settings.value.estimateSplit};saveShot(rule,"SPLIT442-settings");}
 @Test fun dayInputAndResultsPixels(){show();click("split_add");input("split_name_0","Household grocery share for this month");scrollTo("split_base");saveShot(rule,"SPLIT442-day-input");click("split_preview");scrollTo("split_amount_1");saveShot(rule,"SPLIT442-day-result")}
 @Test fun nightLargeWeightedAndDiscardPixels(){show(night=true);click("split_add");input("split_name_0","घर के सामान की हिस्सेदारी और अगली चर्चा");click("split_weighted");input("split_weight_1","2");click("split_preview");scrollTo("split_amount_1");saveShot(rule,"SPLIT442-night-result");click("split_back");saveDialogShot("SPLIT442-night-discard",0xff0d1320)}
 @Test fun callSheetReturnsWithNotesDraftAndSavedEstimateIntact(){show(notes=true);rule.onNode(hasSetTextAction()).performTextInput("Keep note draft");click("call_estimate_open");await("estimate_split");click("estimate_split");click("split_preview");click("split_back");await("estimate_notes_back");click("estimate_notes_back");rule.onNodeWithText("Keep note draft").assertExists();assertEquals("10.00",CallEstimate.read(ctx,n).lines.single().rate)}
}
