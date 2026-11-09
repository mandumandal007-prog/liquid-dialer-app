package com.mandu.liquiddialer
import androidx.activity.compose.setContent
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
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallListFocusUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private lateinit var list:CallLists.ListItem
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun reveal(tag:String,unmerged:Boolean=false):SemanticsNodeInteraction {repeat(24){val n=rule.onNodeWithTag(tag,unmerged);if(n.isDisplayed())return n;val top=n.fetchSemanticsNode().boundsInRoot.top;rule.onRoot().performTouchInput {if(top<0)swipeDown(durationMillis=120)else swipeUp(durationMillis=120)};settle()};saveShot(rule,"FOCUS363-failure");println(rule.onRoot().printToString());error("Not visible $tag")}
 private fun click(tag:String){reveal(tag).performClick();settle()}
 private fun await(check:()->Boolean){rule.waitUntil(15000){settle();check()}}
 @Before fun clean(){android.util.AtomicFile(java.io.File(ctx.filesDir,"manual_call_lists.json")).delete()}
 private fun show(theme:String="daylight",font:Float=1f){
  val id=CallLists.create(ctx,"Clinic follow-ups").single().id
  CallLists.add(ctx,id,"+44 2025 550123","Office report")
  CallLists.add(ctx,id,"+1 2025 550123","Family overseas")
  CallLists.add(ctx,id,"9876543210","Clinic report")
  list=CallLists.read(ctx).single()
  CallLists.followUp(ctx,id,list.entries[0].id,"busy","Bring blood report")
  CallLists.followUp(ctx,id,list.entries[1].id,"later","Other private note")
  CallLists.followUp(ctx,id,list.entries[2].id,"busy","Bring report tomorrow")
  list=CallLists.read(ctx).single()
  rule.mainClock.autoAdvance=false;settle()
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.confirmCall]=true}}
  await{LdApp.settings.value.let{it.theme==theme&&it.fontScale==font&&it.confirmCall}}
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{CallListsPage(rule.activity.state,rule.activity,{})}}};settle()
  await{rule.onAllNodesWithTag("list_open_$id").fetchSemanticsNodes().isNotEmpty()};click("list_open_$id")
 }
 @Test fun searchedSessionSkipsHiddenEntryAndRetainsSavedMatch(){
  show();reveal("list_focus_search").performTextInput("report");settle();saveShot(rule,"FOCUS363-day")
  click("list_session");reveal("session_name",true).assertTextEquals(list.entries[0].name)
  click("session_call");assertEquals(list.entries[0].number,rule.activity.state.confirmNumber);assertEquals(list,CallLists.read(ctx).single())
  click("session_outcome_done");click("session_save_next");await{CallLists.read(ctx).single().entries[0].done}
  reveal("session_name",true).assertTextEquals(list.entries[2].name)
  click("session_previous");reveal("session_name",true).assertTextEquals(list.entries[0].name)
  click("session_next");reveal("session_name",true).assertTextEquals(list.entries[2].name)
  click("session_back");assertEquals(list.entries[1],CallLists.read(ctx).single().entries[1])
 }
 @Test fun exactOutcomeSearchClearAndEmptyStateDoNotWrite(){
  show();click("list_focus_outcome");rule.onNodeWithTag("list_focus_later").performClick();settle()
  click("list_session");reveal("session_name",true).assertTextEquals(list.entries[1].name)
  reveal("session_previous").assertIsNotEnabled();reveal("session_next").assertIsNotEnabled();click("session_back")
  reveal("list_focus_search").performTextInput("missing");settle();reveal("list_session").assertIsNotEnabled()
  click("list_focus_clear");reveal("list_focus_search").assertTextContains("Naam, number ya private note")
  assertEquals(list,CallLists.read(ctx).single());assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun focusedDirtyNavigationStillNeedsDiscardAndLargePixels(){
  show("midnight",1.3f);click("list_focus_outcome");rule.onNodeWithTag("list_focus_busy").performClick();settle();saveShot(rule,"FOCUS363-night-large")
  click("list_session");saveShot(rule,"FOCUS363-session-large")
  reveal("session_note").performTextReplacement("Unsaved new private note");settle();click("session_next")
  rule.onNodeWithTag("session_keep_editing").performClick();settle();reveal("session_note").assertTextContains("Unsaved new private note")
  click("session_next");rule.onNodeWithTag("session_discard").performClick();settle();reveal("session_name",true).assertTextEquals(list.entries[2].name)
  assertEquals(list,CallLists.read(ctx).single());assertNull(rule.activity.state.confirmNumber)
 }
}
