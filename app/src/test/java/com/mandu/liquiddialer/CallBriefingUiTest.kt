package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
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
class CallBriefingUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val n="+919876543210"
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(tag:String){rule.waitUntil(15000){settle();rule.onAllNodesWithTag(tag,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;val node=rule.onNodeWithTag(tag,useUnmergedTree=true);node.performScrollTo();rule.mainClock.autoAdvance=false;node.performClick();settle()}
 @Before fun seed(){listOf("notes.json","manual_call_lists.json","call_checklists.json","call_estimates.json").forEach{f->listOf("",".bak",".new").forEach{File(ctx.filesDir,f+it).delete()}}
  Repo.addNote(ctx,n,"Papa","Delivery: 2 rice bags. Long saved note must wrap without hiding words.")
  Repo.addNote(ctx,"+449876543210","Other","FOREIGN SECRET")
  CallChecklist.add(ctx,n,"Confirm delivery time");CallEstimate.save(ctx,n,0,CallEstimate.Draft(lines=listOf(CallEstimate.line("Rice bags","2","500"))))
  val id=CallLists.create(ctx,"Family follow-ups").single().id;CallLists.add(ctx,id,n,"Papa")
 }
 private fun show(night:Boolean=false,sheet:Boolean=false){
  settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f}}
  rule.waitUntil(15000){LdApp.settings.value.theme==(if(night)"midnight"else"daylight")}
  rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{if(sheet)Column(Modifier.fillMaxSize().background(LocalLd.current.bg).verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Papa",{})}else CallBriefingPage(n,{})}}};settle()
  if(!sheet)await("brief_time")
 }
 @Test fun readOnlySearchTabsRefreshAndExactIdentity(){
  show();rule.onNodeWithText("FOREIGN SECRET").assertDoesNotExist();click("brief_tab_topics");rule.onNodeWithTag("brief_matches",useUnmergedTree=true).assertTextEquals("1 matching topics")
  click("brief_tab_estimate");rule.onNodeWithTag("brief_estimate_total",useUnmergedTree=true).performScrollTo().assertTextEquals("Total: 1000.00")
  click("brief_tab_lists");rule.onNodeWithTag("brief_matches",useUnmergedTree=true).assertTextEquals("1 matching entries")
  click("brief_tab_notes");rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("not found");settle();rule.onNodeWithTag("brief_matches",useUnmergedTree=true).assertTextEquals("0 matching saved notes")
  rule.onNode(hasSetTextAction()).performTextClearance();Repo.addNote(ctx,n,"Papa","New saved note")
  click("brief_refresh");await("brief_time");rule.waitUntil(15000){settle();rule.onAllNodesWithText("New saved note").fetchSemanticsNodes().isNotEmpty()}
  assertEquals(3,Repo.notesChecked(ctx).size);assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun dayPixels(){show();saveShot(rule,"BR392-day");click("brief_tab_estimate");rule.onNodeWithTag("brief_estimate_total",useUnmergedTree=true).performScrollTo();settle();saveShot(rule,"BR392-estimate")}
 @Test fun nightLargePixels(){show(true);saveShot(rule,"BR392-night-large");click("brief_tab_topics");rule.onNodeWithTag("brief_pending").performScrollTo();settle();saveShot(rule,"BR392-topics-large")}
 @Test fun callNotesEntryAndBackPreserveDraft(){show(sheet=true);rule.onNode(hasSetTextAction()).performTextInput("Unsaved draft");click("call_brief_open");await("brief_time");saveShot(rule,"BR392-call");click("brief_notes_back");rule.onNodeWithText("Unsaved draft").assertExists();assertNull(rule.activity.state.confirmNumber)}
 @Test fun detailsEntryRoutesWithoutCallAndFourTilesFit(){
  show(true)
  rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{var opened by androidx.compose.runtime.remember{androidx.compose.runtime.mutableStateOf(false)};if(opened)CallBriefingPage(n,{opened=false})else PageFrame("Number details",{}){NumberSheet(n,null,rule.activity.state,rule.activity,{}, {}, {},onBriefing={opened=true},onChecklist={},onEstimate={},onTimeline={})}}}};settle()
  rule.mainClock.autoAdvance=true;rule.onNodeWithTag("details_tools",true).performScrollTo();rule.onNodeWithTag("details_brief",true).performScrollTo();settle();rule.onNodeWithTag("details_brief",true).assertIsDisplayed()
  val tags=listOf("details_checklist","details_estimate","details_timeline","details_brief")
  val bs=tags.map{rule.onNodeWithTag(it,true).assertHasClickAction().fetchSemanticsNode().boundsInRoot}
  bs.zipWithNext().forEach{(a,b)->assertFalse(a.overlaps(b))};rule.mainClock.autoAdvance=true;rule.onNodeWithTag("details_brief",true).performScrollTo();settle();saveShot(rule,"BR392-details-large")
  click("details_brief");await("brief_time");assertNull(rule.activity.state.confirmNumber);assertNull(rule.activity.state.pendingCall)
 }
 @Test fun callToolsLargeAndCorruptSectionPixels(){
  show(true,true);saveShot(rule,"BR392-call-tools-initial");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("call_brief_open",true).performScrollTo();settle();saveShot(rule,"BR392-call-tools-large")
  click("call_brief_open");await("brief_time");saveShot(rule,"BR392-call-large")
  File(ctx.filesDir,"call_checklists.json").writeText("broken");click("brief_refresh");click("brief_tab_topics");await("brief_section_error")
  rule.onNodeWithTag("brief_section_error",true).performScrollTo().assertIsDisplayed();saveShot(rule,"BR392-section-error");assertEquals("broken",File(ctx.filesDir,"call_checklists.json").readText())
 }
}
