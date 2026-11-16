package com.mandu.liquiddialer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import com.mandu.liquiddialer.LdApp
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
abstract class ReviewFixture:LiveCallDeskFixture(){
 protected val n="+919876543210"
 protected fun snap(count:Int=1)=CallBriefing.Snapshot(n,CallBriefing.Section(emptyList()),CallBriefing.Section((1..count).map{CallChecklist.Item("t$it",if(it==1)"कल सामान भेजना - Confirm delivery time"else"Topic $it",false)}),CallBriefing.Section(CallEstimate.Draft()),CallBriefing.Section(listOf(CallBriefing.ListEntry("l","Rice orders",CallLists.Entry("e",n,"Papa",note="Confirm 2 bags",dueAt=1700000000000)))))
 protected fun ready(night:Boolean=false){seed(night=night);runBlocking{ctx.repo.set(K.briefingReview,true)};await{LdApp.settings.value.briefingReview}}
 protected fun direct(s:CallBriefing.Snapshot=snap(),night:Boolean=false){ready(night);mount{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){BriefingReviewWorkspace(s,false)}};click("review_toggle")}
 protected fun move(tag:String){repeat(35){val node=rule.onNodeWithTag(tag,true);val b=node.getUnclippedBoundsInRoot();val margin=if(rule.onAllNodesWithTag("call_desk_tools",true).fetchSemanticsNodes().isNotEmpty())8.dp else 0.dp;val sc=if(rule.onAllNodesWithTag("call_desk_tools",true).fetchSemanticsNodes().isNotEmpty())rule.onNodeWithTag("call_desk_tools",true)else rule.onAllNodes(hasScrollAction(),true)[0];val v=sc.getUnclippedBoundsInRoot();if(b.top>=v.top+margin&&b.bottom<=v.bottom-margin){settle();node.assertIsDisplayed();return};val dy=if(b.top<v.top+margin)(b.top-v.top-margin).value else(b.bottom-v.bottom+margin).value;sc.performSemanticsAction(SemanticsActions.ScrollBy){it(0f,dy*rule.density.density)};settle()};error("Not visible $tag")}
 protected fun hit(tag:String){move(tag);click(tag)}
 protected fun input(text:String){move("review_query");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("review_query")),true).performTextInput(text);settle()}
 protected fun mark()=hit("review_mark_topic:t1")
 protected fun live(night:Boolean=false){
  listOf("call_checklists.json","manual_call_lists.json").forEach{f->listOf("",".bak",".new").forEach{File(ctx.filesDir,f+it).delete()}}
  CallChecklist.add(ctx,n,"कल सामान भेजना - Confirm delivery time");val id=CallLists.create(ctx,"Rice orders").single().id;CallLists.add(ctx,id,n,"Papa")
  println("REVIEW STEP show");show(night=night);println("REVIEW STEP shown");runBlocking{ctx.repo.set(K.briefingReview,true)};await{LdApp.settings.value.briefingReview};open();println("REVIEW STEP desk");if(night){rule.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("call_tools")),true).performSemanticsAction(SemanticsActions.ScrollBy){it(2000f,0f)};settle()};hit("call_brief_open");println("REVIEW STEP briefing");await{rule.onAllNodesWithTag("brief_time",true).fetchSemanticsNodes().isNotEmpty()};println("REVIEW STEP loaded");hit("review_toggle");println("REVIEW STEP queue")
 }
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class BriefingReviewPart0UiTest:ReviewFixture(){
 @Test fun initiallyCollapsed(){ready();mount{Column{BriefingReviewWorkspace(snap(),false)}};rule.onNodeWithTag("review_queue",true).assertDoesNotExist();click("review_toggle");rule.onNodeWithTag("review_progress",true).assertTextEquals("0 / 2 reviewed in this snapshot")}
 @Test fun markUndoProgress(){direct();mark();rule.onNodeWithTag("review_progress",true).assertTextEquals("1 / 2 reviewed in this snapshot");mark();rule.onNodeWithTag("review_progress",true).assertTextEquals("0 / 2 reviewed in this snapshot")}
 @Test fun onlyUnreviewedAndClear(){direct();mark();hit("review_only");rule.onNodeWithTag("review_count",true).assertTextEquals("1 matching pending items");hit("review_clear");rule.onNodeWithTag("review_count",true).assertTextEquals("2 matching pending items")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class BriefingReviewPart1UiTest:ReviewFixture(){
 @Test fun sourceSearchIntersects(){direct();hit("review_source_calllists");input("confirm bags");rule.onNodeWithTag("review_count",true).assertTextEquals("1 matching pending items");hit("review_source_topics");rule.onNodeWithTag("review_empty",true).assertExists()}
 @Test fun hindiFindsTopic(){direct();input("सामान");rule.onNodeWithTag("review_count",true).assertTextEquals("1 matching pending items")}
 @Test fun resetNeedsConfirmation(){direct();mark();hit("review_reset");hit("review_reset_cancel");rule.onNodeWithTag("review_progress",true).assertTextEquals("1 / 2 reviewed in this snapshot");hit("review_reset");hit("review_reset_confirm");rule.onNodeWithTag("review_progress",true).assertTextEquals("0 / 2 reviewed in this snapshot")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class BriefingReviewPart2UiTest:ReviewFixture(){
 @Test fun hidingRetainsSession(){direct();mark();hit("review_toggle");rule.onNodeWithTag("review_queue",true).assertDoesNotExist();hit("review_toggle");rule.onNodeWithTag("review_progress",true).assertTextEquals("1 / 2 reviewed in this snapshot")}
 @Test fun errorsAreNotZero(){direct(snap().copy(topics=CallBriefing.Section(error=true)));rule.onNodeWithTag("review_error",true).assertTextContains("incomplete",substring=true);rule.onNodeWithTag("review_progress",true).assertTextEquals("0 / 1 reviewed in this snapshot")}
 @Test fun moreDoesNotHideRowsPermanently(){direct(snap(12));rule.onNodeWithTag("review_mark_topic:t12",true).assertDoesNotExist();hit("review_more");move("review_mark_topic:t12");rule.onNodeWithTag("review_mark_topic:t12",true).assertIsDisplayed()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class BriefingReviewPart3UiTest:ReviewFixture(){
 @Test fun refreshedSnapshotClearsReview(){ready();var s by mutableStateOf(snap());mount{Column(Modifier.verticalScroll(rememberScrollState())){BriefingReviewWorkspace(s,false)}};hit("review_toggle");mark();rule.runOnUiThread{s=snap(2)};settle();rule.onNodeWithTag("review_queue",true).assertDoesNotExist();hit("review_toggle");rule.onNodeWithTag("review_progress",true).assertTextEquals("0 / 3 reviewed in this snapshot")}
 @Test fun offUnmountsReviewAndOnStartsFresh(){ready();mount{val s by LdApp.settings.collectAsState();Column(Modifier.verticalScroll(rememberScrollState())){if(s.briefingReview)BriefingReviewWorkspace(snap(),false)}};hit("review_toggle");mark();runBlocking{ctx.repo.set(K.briefingReview,false)};await{rule.onAllNodesWithTag("review_queue",true).fetchSemanticsNodes().isEmpty()};runBlocking{ctx.repo.set(K.briefingReview,true)};await{rule.onAllNodesWithTag("review_toggle",true).fetchSemanticsNodes().isNotEmpty()};hit("review_toggle");rule.onNodeWithTag("review_progress",true).assertTextEquals("0 / 2 reviewed in this snapshot")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class BriefingReviewLiveUiTest:ReviewFixture(){
 @Test fun liveReadReviewDoesNotWriteSources(){live();val before=listOf("call_checklists.json","manual_call_lists.json").associateWith{File(ctx.filesDir,it).readBytes().toList()};val id=CallChecklist.read(ctx,n).single().id;hit("review_mark_topic:$id");before.forEach{(f,b)->assertEquals(b,File(ctx.filesDir,f).readBytes().toList())};rule.onNodeWithTag("end_call",true).assertIsDisplayed();hit("brief_notes_back");rule.onNodeWithTag("call_note_draft",true).assertExists()}
 @Test fun dayDeskPixels(){live();saveShot(rule,"REVIEW468-day");val id=CallChecklist.read(ctx,n).single().id;hit("review_mark_topic:$id");hit("review_reset");move("review_reset_confirm");saveShot(rule,"REVIEW468-reset-day")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class BriefingReviewPixelsUiTest:ReviewFixture(){
 @Test fun nightLargeLiveAndSourcePixels(){live(true);saveShot(rule,"REVIEW468-night-large");hit("review_source_topics");val id=CallChecklist.read(ctx,n).single().id;move("review_mark_topic:$id");saveShot(rule,"REVIEW468-topic-night-large");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOffPixels(){ready(true);runBlocking{ctx.repo.set(K.briefingReview,false)};await{!LdApp.settings.value.briefingReview};mount{BriefingReviewPage{}};saveShot(rule,"REVIEW468-settings-off-night-large")}
}
