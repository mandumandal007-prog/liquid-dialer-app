package com.mandu.liquiddialer
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.foundation.layout.*
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
class CallChecklistUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+44 2025550123"
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()};settle()}
 private fun reveal(tag:String):SemanticsNodeInteraction{
  repeat(40){val n=rule.onNodeWithTag(tag,true);val r=n.getUnclippedBoundsInRoot();val sc=rule.onAllNodes(hasScrollAction(),true).onLast();val v=sc.getUnclippedBoundsInRoot();if(r.bottom>r.top&&r.top>=v.top&&r.bottom<=v.bottom)return n;sc.performTouchInput{if(r.top<v.top)swipeDown()else swipeUp()};settle()};saveShot(rule,"CHECK378-failure");error("Not visible $tag")
 }
 private fun click(tag:String){reveal(tag).performClick();settle()}
 @Before fun clear(){android.util.AtomicFile(File(ctx.filesDir,"call_checklists.json")).delete()}
 private fun show(theme:String="daylight",font:Float=1f,notes:Boolean=false,details:Boolean=false){
  rule.mainClock.autoAdvance=false;settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}}
  await{LdApp.settings.value.let{it.theme==theme&&it.fontScale==font}}
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{
   var page by remember{mutableStateOf(!details)}
   if(details&&!page)PageFrame("Number details",{}){NumberSheet(number,null,rule.activity.state,rule.activity,{},{},{},onChecklist={page=true})}
   else if(notes)Sheet({}){NotesSheet(number,"Office",{})}
   else CallChecklistPage(number,{})
  }}};settle();if(!notes&&!details)await{rule.onAllNodesWithTag("checklist_add",true).fetchSemanticsNodes().isNotEmpty()||rule.onAllNodesWithTag("checklist_error",true).fetchSemanticsNodes().isNotEmpty()}
 }
 private fun add(value:String){click("checklist_add");rule.onNode(hasSetTextAction()).performTextInput(value);settle();saveShot(rule,"CHECK378-editor-day");rule.onNodeWithTag("checklist_editor_save").performClick();await{CallChecklist.read(ctx,number).any{it.text==value}}}
 @Test fun addEditTickDiscardResetAndDeletePreserveOtherNumber(){
  val other=CallChecklist.add(ctx,"+1 2025550123","Other")
  show();add("Order quantity and delivery day");val id=CallChecklist.read(ctx,number).single().id;click("checklist_tick_$id");await{CallChecklist.read(ctx,number).single().done};saveShot(rule,"CHECK378-day-discussed")
  click("checklist_edit_$id");rule.onNode(hasSetTextAction()).performTextReplacement("Changed but not saved");rule.onNodeWithTag("checklist_editor_cancel").performClick();settle();rule.onNodeWithTag("checklist_keep").performClick();settle();rule.onNodeWithTag("checklist_editor_save").performClick();await{CallChecklist.read(ctx,number).single().text=="Changed but not saved"};assertTrue(CallChecklist.read(ctx,number).single().done)
  click("checklist_reset");rule.onNodeWithTag("checklist_cancel_confirm").performClick();settle();assertTrue(CallChecklist.read(ctx,number).single().done);click("checklist_reset");rule.onNodeWithTag("checklist_confirm").performClick();await{!CallChecklist.read(ctx,number).single().done}
  click("checklist_remove_$id");rule.onNodeWithTag("checklist_cancel_confirm").performClick();settle();assertEquals(1,CallChecklist.read(ctx,number).size);click("checklist_clear");rule.onNodeWithTag("checklist_confirm").performClick();await{CallChecklist.read(ctx,number).isEmpty()};assertEquals(other,CallChecklist.read(ctx,"+1 2025550123"));assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun dirtyEditorDiscardAndLengthBoundDoNotSave(){
  show();click("checklist_add");rule.onNode(hasSetTextAction()).performTextInput("x".repeat(251));settle();rule.onNodeWithTag("checklist_editor_save").assertIsNotEnabled();rule.onNodeWithTag("checklist_editor_cancel").performClick();settle();rule.onNodeWithTag("checklist_discard").performClick();settle();assertTrue(CallChecklist.read(ctx,number).isEmpty());saveShot(rule,"CHECK378-empty-day")
 }
 @Test fun longUnicodeNightTopicsWrapAndPersistAfterReopen(){
  CallChecklist.add(ctx,number,"घर और दुकान का सामान: "+"Discuss order, delivery, payment and next visit. ".repeat(3));val item=CallChecklist.add(ctx,number,"Confirm correct customer number before sharing anything").last()
  show("midnight",1.3f);click("checklist_tick_${item.id}");await{CallChecklist.read(ctx,number).last().done};reveal("checklist_progress").assertTextEquals("1 / 2 discussed");saveShot(rule,"CHECK378-night-large")
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{CallChecklistPage(number,{})}}};settle();await{rule.onAllNodesWithTag("checklist_progress",true).fetchSemanticsNodes().isNotEmpty()};reveal("checklist_progress").assertTextEquals("1 / 2 discussed")
 }
 @Test fun callNotesEntryKeepsNoteDraftAndExactNumber(){
  CallChecklist.add(ctx,number,"Ask preferred delivery time");show(notes=true)
  rule.onNode(hasSetTextAction()).performTextInput("Unsaved call note");settle();click("call_checklist_open");await{rule.onAllNodesWithTag("checklist_add",true).fetchSemanticsNodes().isNotEmpty()};val id=CallChecklist.read(ctx,number).single().id;click("checklist_tick_$id");await{CallChecklist.read(ctx,number).single().done};saveShot(rule,"CHECK378-call-sheet")
  click("checklist_notes_back");rule.onNode(hasSetTextAction()).assertTextEquals("Unsaved call note");assertTrue(CallChecklist.read(ctx,"+1 2025550123").isEmpty());assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun numberDetailsEntryOpensChecklistWithoutCalling(){
  show(details=true);click("details_checklist");await{rule.onAllNodesWithTag("checklist_number",true).fetchSemanticsNodes().isNotEmpty()};reveal("checklist_number").assertTextEquals(number);add("Prepare before call");assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun corruptStorageOffersRetryNoOverwrite(){
  val f=File(ctx.filesDir,"call_checklists.json");f.writeText("broken");show();await{rule.onAllNodesWithTag("checklist_error",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("checklist_add").assertDoesNotExist();click("checklist_retry");assertEquals("broken",f.readText());saveShot(rule,"CHECK378-corrupt-day")
 }
 @Test fun simulatedActiveCallOpensExactChecklistKeepsEndAndNoAutomaticProgress(){
  val manager=com.mandu.liquiddialer.telecom.CallManager
  val call=FakeCall.make(number,"Office",android.telecom.Call.STATE_ACTIVE,System.currentTimeMillis()-60000)
  val item=CallChecklist.add(ctx,number,"Confirm delivery address").single()
  show();manager.calls.value=listOf(call);manager.bump()
  try{
   rule.runOnUiThread{rule.activity.setContent{LdThemeHost{CallScreen({},{},{},{})}}};settle()
   rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"CHECK378-active-before")
   val label=rule.onNodeWithText("Notes",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
   val controls=rule.onAllNodes(hasClickAction(),true);val nodes=controls.fetchSemanticsNodes()
   val notesIndex=nodes.indices.filter{val b=nodes[it].boundsInRoot;b.left<=label.center.x&&b.right>=label.center.x&&b.bottom<=label.top}.maxBy{nodes[it].boundsInRoot.bottom}
   controls[notesIndex].performClick();settle();await{rule.onAllNodesWithTag("call_checklist_open",true).fetchSemanticsNodes().isNotEmpty()};click("call_checklist_open")
   await{rule.onAllNodesWithTag("checklist_progress",true).fetchSemanticsNodes().isNotEmpty()};reveal("checklist_number").assertTextEquals(number)
   assertFalse(CallChecklist.read(ctx,number).single().done);click("checklist_tick_${item.id}");await{CallChecklist.read(ctx,number).single().done};saveShot(rule,"CHECK378-active-checklist")
   click("checklist_notes_back");rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle()
   rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertEquals(android.telecom.Call.STATE_ACTIVE,manager.state(call));assertNull(rule.activity.state.confirmNumber)
  }finally{manager.calls.value=emptyList();manager.bump()}
 }

}
