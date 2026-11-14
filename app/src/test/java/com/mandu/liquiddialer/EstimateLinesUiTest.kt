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
class EstimateLinesUiTest {
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false;it[K.estimateLines]=false}}
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
 private fun show(night:Boolean=false,notes:Boolean=false,enabled:Boolean=true,direct:Boolean=true){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.estimateLines]=enabled}};waitFor{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&it.estimateLines==enabled}};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{if(notes)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}else if(direct)EstimateLinesWorkspace(n,sample,{closed=true},{applied=it})else CallEstimatePage(n,{})}};rule.activity.setContentView(host)};if(!direct&&!notes)await("estimate_total")else settle()}
 private fun input(tag:String,s:String){scrollTo(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(s);settle()}
 @After fun reset(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};settle();rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.callTally]=false;it[K.estimateLines]=false}}}
 private fun dialog(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}


 @Before fun seed(){android.util.AtomicFile(File(ctx.filesDir,"call_estimates.json")).delete();CallEstimate.save(ctx,n,0,sample)}
 @Test fun defaultOffHiddenEmptyCalculatorEntryEnabled(){show(enabled=false,direct=false);assertFalse(LdSettings().estimateLines);rule.onAllNodesWithTag("estimate_lines",true).assertCountEquals(0);show(direct=false);click("estimate_lines");await("lines_capacity")}
 @Test fun previewAllItemsAndTotalsNoFileWrite(){show();val before=File(ctx.filesDir,"call_estimates.json").readBytes();input("lines_input","Rice | 2.5 | 40\nOil | 1 | 120");click("lines_preview");rule.onNodeWithTag("lines_name_0",true).assertTextEquals("Rice");rule.onNodeWithTag("lines_amount_0",true).assertTextEquals("2.5 × 40 = 100.00");rule.onNodeWithTag("lines_total",true).assertTextEquals("Total 10.00 → 230.00");assertArrayEquals(before,File(ctx.filesDir,"call_estimates.json").readBytes());assertNull(applied)}
 @Test fun editingInvalidatesAndInvalidLineRefusesAll(){show();input("lines_input","A|1|1");click("lines_preview");input("lines_input","A|1|1\nBad line");rule.onAllNodesWithTag("lines_result",true).assertCountEquals(0);click("lines_preview");rule.onNodeWithTag("lines_error",true).assertTextContains("Line 2",substring=true);assertNull(applied)}
 @Test fun blanksAndRepeatedNamesDisclosed(){show();input("lines_input","\nRice|1|2\n\nRice|3|4\n");click("lines_preview");rule.onNodeWithTag("lines_result",true).assertTextEquals("Preview · 2 new items");rule.onNodeWithTag("lines_blanks",true).assertTextContains("3 blank lines ignored",substring=true);rule.onNodeWithTag("lines_name_1",true).assertTextEquals("Rice")}
 @Test fun confirmCancelThenAppendWithoutSave(){show();val before=File(ctx.filesDir,"call_estimates.json").readBytes();input("lines_input","A|1|2\nB|3|4");click("lines_preview");click("lines_use");assertNull(applied);dialog("lines_cancel");assertNull(applied);click("lines_use");dialog("lines_confirm");assertEquals(listOf("Rice bags","A","B"),applied!!.lines.map{it.label});assertArrayEquals(before,File(ctx.filesDir,"call_estimates.json").readBytes())}
 @Test fun dirtyBackKeepDiscard(){show();input("lines_input","A|1|1");click("lines_back");dialog("lines_keep");assertFalse(closed);click("lines_back");dialog("lines_discard");assertTrue(closed);assertNull(applied)}
 @Test fun untouchedBackNoDiscard(){show();click("lines_back");assertTrue(closed)}
 @Test fun sourceChangeResetsRawPreviewAndConfirm(){show();val src=androidx.compose.runtime.mutableStateOf(sample);rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateLinesWorkspace(n,src.value,{},{})}};rule.activity.setContentView(host)};settle();input("lines_input","A|1|1");click("lines_preview");click("lines_use");rule.runOnUiThread{src.value=sample.copy(discount="1")};settle();rule.onAllNodesWithTag("lines_confirm",true).assertCountEquals(0);rule.onAllNodesWithTag("lines_result",true).assertCountEquals(0);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("lines_input")),true).assertTextEquals("")}
 @Test fun calculatorAppendStaysUnsavedUntilExplicitSave(){show(direct=false);val before=File(ctx.filesDir,"call_estimates.json").readBytes();input("estimate_discount","1");click("estimate_lines");input("lines_input","Oil|1|20");click("lines_preview");click("lines_use");dialog("lines_confirm");await("estimate_total");rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 29.00");rule.onNodeWithTag("estimate_status",true).assertTextEquals("Unsaved changes");assertArrayEquals(before,File(ctx.filesDir,"call_estimates.json").readBytes());click("estimate_save");waitFor{CallEstimate.read(ctx,n).lines.size==2};rule.onNodeWithTag("estimate_status",true).assertTextEquals("Saved draft")}
 @Test fun emptySourceAllowsItemsFullCapacityRefuses(){show();rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateLinesWorkspace(n,CallEstimate.Draft(),{},{applied=it})}};rule.activity.setContentView(host)};settle();input("lines_input","First|1|2");click("lines_preview");rule.onNodeWithTag("lines_total",true).assertTextEquals("Total 0.00 → 2.00");val b=sample.copy(lines=(1..19).map{CallEstimate.line("Old$it","1","1","$it")});rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateLinesWorkspace(n,b,{},{})}};rule.activity.setContentView(host)};settle();input("lines_input","A|1|1\nB|1|1");click("lines_preview");rule.onNodeWithTag("lines_error",true).assertTextContains("Whole batch refused",substring=true);assertNull(applied)}
 @Test fun invalidSourceDisablesPreview(){show();rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateLinesWorkspace(n,sample.copy(discount="99"),{},{})}};rule.activity.setContentView(host)};settle();input("lines_input","A|1|1");rule.onNodeWithTag("lines_source_error",true).assertExists();rule.onNodeWithTag("lines_preview",true).assertIsNotEnabled()}
 @Test fun noClipboardOrActivityEffects(){show();val clip=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("keep","Do not import this"));rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("lines_input")),true).assertTextEquals("");input("lines_input","A|1|2");click("lines_preview");click("lines_use");dialog("lines_confirm");assertEquals("Do not import this",clip.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun settingsFinderPersistencePixels(){show(enabled=false);assertTrue(SettingsFinder.all.any{it.id=="estimate-lines"&&it.route=="estimatelines"});rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{LdThemeHost{EstimateLinesPage({})}};rule.activity.setContentView(host)};settle();rule.onAllNodesWithText("Calculator multi-line items",substring=false).onLast().performClick();waitFor{LdApp.settings.value.estimateLines};saveShot(rule,"LINES446-settings")}
 @Test fun dayInputAndPreviewPixels(){show();input("lines_input","Rice bags | 2.5 | 40\nOil bottles | 3 | 120\nSpice packets | 5 | 12.50");scrollTo("lines_input");saveShot(rule,"LINES446-day-input");click("lines_preview");scrollTo("lines_total");saveShot(rule,"LINES446-day-preview")}
 @Test fun nightLargeUnicodePreviewConfirmAndErrorPixels(){show(night=true);input("lines_input","घर के सामान के लिए चावल के बड़े बैग | 2.5 | 123.45\nCooking oil and household supplies | 3 | 50");click("lines_preview");scrollTo("lines_name_0");saveShot(rule,"LINES446-night-items");scrollTo("lines_total");saveShot(rule,"LINES446-night-total");click("lines_use");saveDialogShot("LINES446-night-confirm",0xff0d1320);dialog("lines_cancel");input("lines_input","Rice | 1 | 2\nOil | 0 | 3");click("lines_preview");scrollTo("lines_error");saveShot(rule,"LINES446-night-error")}
 @Test fun callSheetNotesAndSavedDraftSurviveUnsavedAppend(){show(notes=true);rule.onNode(hasSetTextAction()).performTextInput("Keep note draft");click("call_estimate_open");await("estimate_lines");click("estimate_lines");input("lines_input","A|1|1");click("lines_preview");click("lines_use");dialog("lines_confirm");await("estimate_notes_back");click("estimate_notes_back");dialog("estimate_discard");rule.onNodeWithText("Keep note draft").assertExists();assertEquals(1,CallEstimate.read(ctx,n).lines.size)}
}
