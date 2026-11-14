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
class EstimateScaleUiTest {
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false;it[K.estimateScale]=false}}
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
 private var applied:CallEstimate.Draft?=null
 private val sample=CallEstimate.Draft(lines=listOf(CallEstimate.line("Rice bags","1","10.00","a")))
 private fun show(night:Boolean=false,notes:Boolean=false,enabled:Boolean=true,direct:Boolean=true){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.estimateScale]=enabled}};waitFor{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&it.estimateScale==enabled}};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{if(notes)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}else if(direct)EstimateScaleWorkspace(n,sample,{closed=true},{applied=it})else CallEstimatePage(n,{})}};rule.activity.setContentView(host)};if(!direct&&!notes)await("estimate_total")else settle()}
 private fun input(tag:String,s:String){scrollTo(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(s);settle()}
 @After fun reset(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};settle();rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.callTally]=false;it[K.estimateScale]=false}}}
 private fun dialog(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}


 @Before fun seed(){android.util.AtomicFile(File(ctx.filesDir,"call_estimates.json")).delete();CallEstimate.save(ctx,n,0,sample)}
 @Test fun defaultOffEntryAndCalculatorOpen(){show(enabled=false,direct=false);assertFalse(LdSettings().estimateScale);rule.onAllNodesWithTag("estimate_scale",true).assertCountEquals(0);show(direct=false);click("estimate_scale");await("scale_count")}
 @Test fun selectionAndRatioPreviewNoFileWrite(){show();val before=File(ctx.filesDir,"call_estimates.json").readBytes();rule.onNodeWithTag("scale_preview",true).assertIsNotEnabled();click("scale_all");click("scale_preview");rule.onNodeWithTag("scale_quantity_a",true).assertTextEquals("Quantity 1 → 2");rule.onNodeWithTag("scale_total",true).assertTextEquals("Total 10.00 → 20.00");assertArrayEquals(before,File(ctx.filesDir,"call_estimates.json").readBytes());assertNull(applied)}
 @Test fun ratioEditInvalidatesAndInvalidFractionRefuses(){show();click("scale_all");click("scale_preview");input("scale_denominator","3");rule.onAllNodesWithTag("scale_result",true).assertCountEquals(0);click("scale_preview");rule.onNodeWithTag("scale_error",true).assertExists();assertNull(applied)}
 @Test fun clearAndToggleInvalidatePreview(){show();click("scale_all");click("scale_preview");click("scale_select_a");rule.onAllNodesWithTag("scale_result",true).assertCountEquals(0);rule.onNodeWithTag("scale_count",true).assertTextEquals("0 / 1 items selected");click("scale_all");click("scale_preview");click("scale_none");rule.onAllNodesWithTag("scale_result",true).assertCountEquals(0)}
 @Test fun confirmationCancelThenApplyUnsavedOnly(){show();val f=File(ctx.filesDir,"call_estimates.json");val before=f.readBytes();click("scale_all");click("scale_preview");click("scale_use");assertNull(applied);dialog("scale_cancel");assertNull(applied);click("scale_use");dialog("scale_confirm");assertEquals("2",applied!!.lines.single().quantity);assertArrayEquals(before,f.readBytes());assertFalse(closed)}
 @Test fun dirtyBackKeepAndDiscard(){show();click("scale_all");click("scale_back");dialog("scale_keep");assertFalse(closed);click("scale_back");dialog("scale_discard");assertTrue(closed);assertNull(applied)}
 @Test fun untouchedBackNoDialog(){show();click("scale_back");assertTrue(closed)}
 @Test fun sourceChangeResetsInputsPreviewAndConfirm(){show();val src=androidx.compose.runtime.mutableStateOf(sample);rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateScaleWorkspace(n,src.value,{},{})}};rule.activity.setContentView(host)};settle();click("scale_all");input("scale_numerator","3");click("scale_preview");click("scale_use");rule.runOnUiThread{src.value=sample.copy(discount="1")};settle();rule.onAllNodesWithTag("scale_confirm",true).assertCountEquals(0);rule.onAllNodesWithTag("scale_result",true).assertCountEquals(0);rule.onNodeWithTag("scale_count",true).assertTextEquals("0 / 1 items selected");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("scale_numerator")),true).assertTextEquals("2")}
 @Test fun calculatorApplyKeepsUnsavedStatusUntilExplicitSave(){show(direct=false);val f=File(ctx.filesDir,"call_estimates.json");val before=f.readBytes();input("estimate_discount","1");click("estimate_scale");click("scale_all");click("scale_preview");rule.onNodeWithTag("scale_total",true).assertTextEquals("Total 9.00 → 19.00");click("scale_use");dialog("scale_confirm");await("estimate_total");rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 19.00");rule.onNodeWithTag("estimate_status",true).assertTextEquals("Unsaved changes");assertArrayEquals(before,f.readBytes());click("estimate_save");waitFor{CallEstimate.read(ctx,n).lines.single().quantity=="2"};rule.onNodeWithTag("estimate_status",true).assertTextEquals("Saved draft")}
 @Test fun discardReturnsWithOriginalUnsavedCalculatorAndClipboard(){show(direct=false);val clip=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("keep","Keep"));input("estimate_discount","1");click("estimate_scale");click("scale_all");click("scale_preview");click("scale_back");dialog("scale_discard");await("estimate_total");rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 9.00");assertEquals("Keep",clip.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun invalidSourceNoPreview(){show();rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateScaleWorkspace(n,CallEstimate.Draft(),{},{})}};rule.activity.setContentView(host)};settle();rule.onNodeWithTag("scale_source_error",true).assertExists();rule.onNodeWithTag("scale_preview",true).assertIsNotEnabled()}
 @Test fun settingsPersistenceFinderPixels(){show(enabled=false);assertTrue(SettingsFinder.all.any{it.id=="estimate-scale"&&it.route=="estimatescale"});rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateScalePage({})}};rule.activity.setContentView(host)};settle();rule.onAllNodesWithText("Calculator quantity scaling",substring=false).onLast().performClick();waitFor{LdApp.settings.value.estimateScale};saveShot(rule,"SCALE445-settings")}
 @Test fun daySelectionAndPreviewPixels(){show();scrollTo("scale_count");saveShot(rule,"SCALE445-day-selection");click("scale_all");input("scale_numerator","3");input("scale_denominator","2");click("scale_preview");scrollTo("scale_total");saveShot(rule,"SCALE445-day-preview")}
 @Test fun nightLargeLongLabelsAndConfirmPixels(){show(night=true);val b=sample.copy(lines=listOf(CallEstimate.line("घर के सामान के लिए चावल के बड़े बैग की मात्रा","2.5","123.45","a"),CallEstimate.line("Cooking oil and other household supplies","3","50","b")),discount="10",tax="5");rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateScaleWorkspace(n,b,{},{})}};rule.activity.setContentView(host)};settle();click("scale_all");input("scale_numerator","3");input("scale_denominator","2");click("scale_preview");scrollTo("scale_quantity_a");saveShot(rule,"SCALE445-night-preview");scrollTo("scale_total");saveShot(rule,"SCALE445-night-total");click("scale_use");saveDialogShot("SCALE445-night-confirm",0xff0d1320)}
 @Test fun callSheetNotesDraftSurvivesApplyAndReturn(){show(notes=true);rule.onNode(hasSetTextAction()).performTextInput("Keep note draft");click("call_estimate_open");await("estimate_scale");click("estimate_scale");click("scale_all");click("scale_preview");click("scale_use");dialog("scale_confirm");await("estimate_notes_back");click("estimate_notes_back");rule.onNodeWithTag("estimate_discard",true).performClick();settle();rule.onNodeWithText("Keep note draft").assertExists();assertEquals("1",CallEstimate.read(ctx,n).lines.single().quantity)}
}
