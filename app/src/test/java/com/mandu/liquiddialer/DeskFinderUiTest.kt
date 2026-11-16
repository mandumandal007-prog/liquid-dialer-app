package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class DeskFinderFixture:LiveCallDeskFixture(){
 protected fun prepared(on:Boolean=true,night:Boolean=false,all:Boolean=false){show(night=night);runBlocking{ctx.repo.set(K.deskFinder,on);ctx.repo.set(K.callTally,all);ctx.repo.set(K.callDates,all);ctx.repo.set(K.callConverter,all);ctx.repo.set(K.callCanvas,all);ctx.repo.set(K.messageDrafts,all);ctx.repo.set(K.callReferences,all);ctx.repo.set(K.callNoteReader,all)};await{LdApp.settings.value.let{it.deskFinder==on&&it.callConverter==all&&it.callNoteReader==all}};open()}
 protected fun move(tag:String){repeat(35){val node=rule.onNodeWithTag(tag,true);val b=node.getUnclippedBoundsInRoot();val sc=rule.onNodeWithTag("call_desk_tools",true);val v=sc.getUnclippedBoundsInRoot();if(b.top>=v.top+8.dp&&b.bottom<=v.bottom-8.dp){settle();node.assertIsDisplayed();return};val dy=if(b.top<v.top+8.dp)(b.top-v.top-8.dp).value else (b.bottom-v.bottom+8.dp).value;sc.performSemanticsAction(SemanticsActions.ScrollBy){it(0f,dy*rule.density.density)};settle()};error("Not visible $tag")}
 protected fun tap(tag:String){move(tag);click(tag)}
 protected fun finder(){tap("desk_finder_toggle")}
 protected fun query(s:String){move("desk_finder_query");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("desk_finder_query")),true).performTextReplacement(s);settle();rule.runOnUiThread{rule.activity.currentFocus?.clearFocus()};settle()}
 @After fun finderClean(){runBlocking{ctx.repo.set(K.deskFinder,false);ctx.repo.set(K.callTally,false);ctx.repo.set(K.callDates,false);ctx.repo.set(K.callConverter,false);ctx.repo.set(K.callCanvas,false);ctx.repo.set(K.messageDrafts,false);ctx.repo.set(K.callReferences,false);ctx.repo.set(K.callNoteReader,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskFinderUiTest:DeskFinderFixture(){
 @Test fun offOriginalLaunchers(){prepared(false);rule.onAllNodesWithTag("desk_finder_toggle",true).assertCountEquals(0);rule.onNodeWithTag("call_estimate_open",true).assertExists();rule.onNodeWithTag("call_note_draft",true).assertExists()}
 @Test fun startsCollapsedThenShowsEnabledOnly(){prepared();rule.onAllNodesWithTag("desk_finder_query",true).assertCountEquals(0);finder();rule.onNodeWithTag("desk_finder_count",true).assertTextEquals("4 matching / 4 available tools");rule.onAllNodesWithTag("desk_finder_open_converter",true).assertCountEquals(0);rule.onAllNodesWithTag("call_estimate_open",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun queryAndCategoryThenClear(){prepared(all=true);finder();query("kg");rule.onNodeWithTag("desk_finder_count",true).assertTextEquals("1 matching / 11 available tools");tap("desk_finder_group_prepare");rule.onNodeWithTag("desk_finder_empty",true).assertExists();tap("desk_finder_clear");rule.onNodeWithTag("desk_finder_count",true).assertTextEquals("11 matching / 11 available tools")}
 @Test fun hiddenFeatureRemovedImmediately(){prepared();finder();query("calculator");runBlocking{ctx.repo.set(K.hiddenFeatures,setOf("estimate"))};await{rule.onAllNodesWithTag("desk_finder_open_estimate",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("desk_finder_empty",true).assertExists();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun optionalOffRemovedImmediately(){prepared(all=true);finder();query("kg");runBlocking{ctx.repo.set(K.callConverter,false)};await{rule.onAllNodesWithTag("desk_finder_open_converter",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("desk_finder_empty",true).assertExists()}
 @Test fun calculatorOpenRetainsControls(){prepared();finder();query("calculator");tap("desk_finder_open_estimate");await{rule.onAllNodesWithTag("estimate_number",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("estimate_number",true).assertTextEquals("+919876543210");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed()}
 @Test fun checklistOpen(){prepared();finder();query("topics");tap("desk_finder_open_checklist");rule.onAllNodesWithTag("desk_finder",true).assertCountEquals(0);rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun converterOpenNotAction(){prepared(all=true);finder();query("kg");tap("desk_finder_open_converter");rule.onAllNodesWithTag("desk_finder",true).assertCountEquals(0);rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity);assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun offRestoresLaunchersKeepsNoteDraft(){prepared();move("call_note_draft");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("call_note_draft")),true).performTextInput("Keep unsaved note");finder();query("calculator");runBlocking{ctx.repo.set(K.deskFinder,false)};await{rule.onAllNodesWithTag("desk_finder_toggle",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("call_estimate_open",true).assertExists();rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("call_note_draft")),true).assertTextContains("Keep unsaved note");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun hideReopenResetsQueryAndCategory(){prepared();finder();query("calculator");tap("desk_finder_group_calculate");tap("desk_finder_toggle");finder();rule.onNodeWithTag("desk_finder_count",true).assertTextEquals("4 matching / 4 available tools");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("desk_finder_query")),true).assertTextEquals("")}
 @Test fun callChangeClosesFinder(){prepared();finder();com.mandu.liquiddialer.telecom.CallManager.calls.value=listOf(deskShadowCall());settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0)}
 @Test fun dayFilteredToolPixels(){prepared(all=true);finder();query("calculator");move("desk_finder_open_estimate");rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"FINDER467-filtered-day")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskFinderPixelsUiTest:DeskFinderFixture(){
 @Test fun nightLargeFinderAndNoMatch(){prepared(night=true,all=true);finder();query("गिनती");move("desk_finder_query");saveShot(rule,"FINDER467-query-night-large");move("desk_finder_open_tally");rule.onNodeWithTag("call_desk_name",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_timer",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"FINDER467-result-night-large");query("no such tool");move("desk_finder_empty");saveShot(rule,"FINDER467-empty-night-large")}
 @Test fun offSettingNoOtherEnable(){seed(false,night=true);runBlocking{ctx.repo.set(K.deskFinder,false)};await{!LdApp.settings.value.deskFinder};mount{DeskFinderPage{}};saveShot(rule,"FINDER467-settings-off-night-large");rule.onAllNodesWithText("Desk tool finder",useUnmergedTree=true).onLast().performClick();await{LdApp.settings.value.deskFinder};assertFalse(LdApp.settings.value.liveCallDesk||LdApp.settings.value.callTally||LdApp.settings.value.callConverter)}
}
