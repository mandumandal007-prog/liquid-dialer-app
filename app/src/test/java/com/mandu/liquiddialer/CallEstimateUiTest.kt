package com.mandu.liquiddialer
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
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
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class CallEstimateUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+44 2025550123"
 private var backs=0
 private var generation=0
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()};settle()}
 private fun reveal(tag:String):SemanticsNodeInteraction{
  repeat(45){val n=rule.onNodeWithTag(tag,true);val r=n.getUnclippedBoundsInRoot();val sc=rule.onAllNodes(hasScrollAction(),true).onLast();val v=sc.getUnclippedBoundsInRoot();if(r.bottom>r.top&&r.top>=v.top&&r.bottom<=v.bottom)return n;sc.performTouchInput{if(r.top<v.top)swipeDown()else swipeUp()};settle()};saveShot(rule,"EST379-failure");error("Not visible $tag")
 }
 private fun click(tag:String){reveal(tag).performClick();settle()}
 private fun input(tag:String,value:String){reveal(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(value);settle()}
 @Before fun clear(){android.util.AtomicFile(File(ctx.filesDir,"call_estimates.json")).delete();backs=0}
 private fun sample()=CallEstimate.Draft(lines=listOf(CallEstimate.line("Chawal","2.5","40"),CallEstimate.line("Daal","3","80.25")),discount="10.75",tax="5")
 private fun show(theme:String="daylight",font:Float=1f,notes:Boolean=false,details:Boolean=false){
  rule.mainClock.autoAdvance=false;settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}}
  await{LdApp.settings.value.let{it.theme==theme&&it.fontScale==font}}
  val g=++generation
  rule.runOnUiThread{rule.activity.setContent{key(g){LdThemeHost{
   var page by remember{mutableStateOf(!details)}
   if(details&&!page)PageFrame("Number details",{}){NumberSheet(number,null,rule.activity.state,rule.activity,{},{},{},onEstimate={page=true})}
   else if(notes)Sheet({}){NotesSheet(number,"Office",{})}
   else CallEstimatePage(number,{backs++})
  }}}};settle();if(!notes&&!details)await{rule.onAllNodesWithTag("estimate_add",true).fetchSemanticsNodes().isNotEmpty()||rule.onAllNodesWithTag("estimate_error",true).fetchSemanticsNodes().isNotEmpty()}
 }
 private fun add(label:String="Chawal",quantity:String="2.5",rate:String="40"){
  click("estimate_add");input("estimate_label",label);input("estimate_quantity",quantity);input("estimate_rate",rate);saveShot(rule,"EST379-editor-day");click("estimate_item_apply")
 }
 @Test fun numberDetailsAddCalculateSaveAndReopenWithoutCalling(){
  show(details=true);click("details_estimate");await{rule.onAllNodesWithTag("estimate_add",true).fetchSemanticsNodes().isNotEmpty()};add();add("Daal","3","80.25");input("estimate_discount","10.75");input("estimate_tax","5");reveal("estimate_total").assertTextEquals("Total: 346.50");saveShot(rule,"EST379-day-summary")
  assertTrue(CallEstimate.read(ctx,number).lines.isEmpty());click("estimate_save");await{CallEstimate.read(ctx,number).lines.size==2};assertNull(rule.activity.state.confirmNumber)
  show();reveal("estimate_total").assertTextEquals("Total: 346.50");assertTrue(CallEstimate.read(ctx,"+1 2025550123").lines.isEmpty())
 }
 @Test fun invalidAndOverDiscountNeverSaveOrShowStaleTotal(){
  show();click("estimate_add");input("estimate_label","Test");input("estimate_quantity","1e2");input("estimate_rate","2");reveal("estimate_item_apply").assertIsNotEnabled();input("estimate_quantity","1");click("estimate_item_apply");input("estimate_discount","3");reveal("estimate_save").assertIsNotEnabled();rule.onNodeWithTag("estimate_total").assertDoesNotExist();input("estimate_discount","0");input("estimate_tax","101");reveal("estimate_save").assertIsNotEnabled();assertTrue(CallEstimate.read(ctx,number).lines.isEmpty())
 }
 @Test fun dirtyBackKeepAndDiscardProtectSavedDraft(){
  val d=CallEstimate.save(ctx,number,0,sample());show();input("estimate_tax","10");rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle();rule.onNodeWithTag("estimate_keep").performClick();settle();assertEquals(0,backs);assertEquals(d,CallEstimate.read(ctx,number));rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle();rule.onNodeWithTag("estimate_discard").performClick();settle();assertEquals(1,backs);assertEquals(d,CallEstimate.read(ctx,number))
 }
 @Test fun removeAndClearConfirmationsPreserveOtherNumbers(){
  val d=CallEstimate.save(ctx,number,0,sample());val other=CallEstimate.save(ctx,"+1 2025550123",0,sample());show();click("estimate_remove_${d.lines.first().id}");rule.onNodeWithText("Rehne do").performClick();settle();assertEquals(2,CallEstimate.read(ctx,number).lines.size);click("estimate_remove_${d.lines.first().id}");rule.onNodeWithTag("estimate_remove_confirm").performClick();settle();assertEquals(2,CallEstimate.read(ctx,number).lines.size);click("estimate_save");await{CallEstimate.read(ctx,number).lines.size==1}
  click("estimate_clear");rule.onNodeWithTag("estimate_clear_cancel").performClick();settle();assertEquals(1,CallEstimate.read(ctx,number).lines.size);click("estimate_clear");rule.onNodeWithTag("estimate_clear_confirm").performClick();await{CallEstimate.read(ctx,number).lines.isEmpty()};assertEquals(other,CallEstimate.read(ctx,"+1 2025550123"))
 }
 @Test fun notesEntryPreservesUnsavedNoteAndExactNumber(){
  CallEstimate.save(ctx,number,0,sample());show(notes=true);rule.onNode(hasSetTextAction()).performTextInput("Unsaved note");settle();click("call_estimate_open");await{rule.onAllNodesWithTag("estimate_add",true).fetchSemanticsNodes().isNotEmpty()};reveal("estimate_number").assertTextEquals(number);input("estimate_tax","0");click("estimate_save");await{CallEstimate.read(ctx,number).tax=="0"};saveShot(rule,"EST379-notes-sheet");click("estimate_notes_back");rule.onNode(hasSetTextAction()).assertTextEquals("Unsaved note");assertTrue(CallEstimate.read(ctx,"+1 2025550123").lines.isEmpty())
 }
 @Test fun staleEditorOffersExplicitReloadAndCorruptionNeverOverwrites(){
  val old=CallEstimate.save(ctx,number,0,sample());show();CallEstimate.save(ctx,number,old.revision,old.copy(tax="20"));input("estimate_tax","10");click("estimate_save");await{rule.onAllNodesWithTag("estimate_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals("20",CallEstimate.read(ctx,number).tax);click("estimate_reload");rule.onNodeWithTag("estimate_discard").performClick();settle();await{rule.onAllNodesWithTag("estimate_error",true).fetchSemanticsNodes().isEmpty()};reveal("estimate_tax");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("estimate_tax")),true).assertTextEquals("20");assertEquals(0,backs)
  File(ctx.filesDir,"call_estimates.json").writeText("broken");show();rule.onNodeWithTag("estimate_add").assertDoesNotExist();click("estimate_retry");assertEquals("broken",File(ctx.filesDir,"call_estimates.json").readText());saveShot(rule,"EST379-corrupt")
 }
 @Test fun longUnicodeNightLargeAmountsWrapAndEditorCancelDoesNotChangeSaved(){
  val line=CallEstimate.line("दुकान का सामान और अगली डिलीवरी · "+"Long item name ".repeat(2),"1000000","1000000000");val d=CallEstimate.save(ctx,number,0,CallEstimate.Draft(lines=listOf(line),tax="18"));show("midnight",1.3f);reveal("estimate_total").assertTextEquals("Total: 1180000000000000.00");saveShot(rule,"EST379-night-large");click("estimate_edit_${line.id}");input("estimate_label","Discard this");click("estimate_item_cancel");assertEquals(d,CallEstimate.read(ctx,number))
 }
 @Test fun simulatedActiveCallCalculatorRetainsCallEndAndMakesNoAutomaticChanges(){
  val manager=com.mandu.liquiddialer.telecom.CallManager;val call=FakeCall.make(number,"Office",android.telecom.Call.STATE_ACTIVE,System.currentTimeMillis()-60000);val d=CallEstimate.save(ctx,number,0,sample());show();manager.calls.value=listOf(call);manager.bump()
  try{rule.runOnUiThread{rule.activity.setContent{LdThemeHost{CallScreen({},{},{},{})}}};settle();rule.onNodeWithTag("end_call",true).assertIsDisplayed()
   val label=rule.onNodeWithText("Notes",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot;val controls=rule.onAllNodes(hasClickAction(),true);val nodes=controls.fetchSemanticsNodes();val index=nodes.indices.filter{val b=nodes[it].boundsInRoot;b.left<=label.center.x&&b.right>=label.center.x&&b.bottom<=label.top}.maxBy{nodes[it].boundsInRoot.bottom};controls[index].performClick();settle();await{rule.onAllNodesWithTag("call_estimate_open",true).fetchSemanticsNodes().isNotEmpty()};click("call_estimate_open");await{rule.onAllNodesWithTag("estimate_add",true).fetchSemanticsNodes().isNotEmpty()};reveal("estimate_number").assertTextEquals(number);saveShot(rule,"EST379-active-top");reveal("estimate_total").assertTextEquals("Total: 346.50");assertEquals(d,CallEstimate.read(ctx,number));saveShot(rule,"EST379-active-calculator");click("estimate_notes_back");rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle();rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertEquals(android.telecom.Call.STATE_ACTIVE,manager.state(call));assertNull(rule.activity.state.confirmNumber)
  }finally{manager.calls.value=emptyList();manager.bump()}
 }
}
