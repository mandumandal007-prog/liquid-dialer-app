package com.mandu.liquiddialer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class ReviewOptionsFixture:ReviewFixture(){
 protected fun options(priority:Boolean=true,slim:Boolean=true){runBlocking{ctx.repo.set(K.reviewPriority,priority);ctx.repo.set(K.reviewSlim,slim)};await{LdApp.settings.value.reviewPriority==priority&&LdApp.settings.value.reviewSlim==slim}}
 protected fun start(priority:Boolean=true,slim:Boolean=true){options(priority,slim);direct()}
 protected fun star(id:String="topic:t1")=hit("priority_mark_$id")
 @After fun clearOptions(){runBlocking{ctx.repo.set(K.reviewPriority,false);ctx.repo.set(K.reviewSlim,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewOptionsPart0UiTest:ReviewOptionsFixture(){
 @Test fun slimDetailsExplicitAndFullRestored(){start();rule.onNodeWithTag("review_detail_topic:t1",true).assertDoesNotExist();hit("review_expand_topic:t1");rule.onNodeWithTag("review_detail_topic:t1",true).assertExists();hit("review_expand_topic:t1");rule.onNodeWithTag("review_detail_topic:t1",true).assertDoesNotExist();options(slim=false);await{rule.onAllNodesWithTag("review_detail_topic:t1",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("review_detail_topic:t1",true).assertExists();rule.onNodeWithTag("review_expand_topic:t1",true).assertDoesNotExist()}
 @Test fun starUndoAndOnlyFilter(){start();star();rule.onNodeWithTag("priority_count",true).assertTextEquals("1 /10 session priorities");hit("priority_only");rule.onNodeWithTag("review_count",true).assertTextEquals("1 matching pending items");star();rule.onNodeWithTag("review_count",true).assertTextEquals("0 matching pending items");hit("review_clear");rule.onNodeWithTag("review_count",true).assertTextEquals("2 matching pending items")}
 @Test fun reviewedSeparateAndLayoutDoesNotClear(){start();star();mark();options(slim=false);rule.onNodeWithTag("priority_count",true).assertTextEquals("1 /10 session priorities");rule.onNodeWithTag("review_progress",true).assertTextEquals("1 / 2 reviewed in this snapshot");hit("review_only");hit("priority_only");rule.onNodeWithTag("review_count",true).assertTextEquals("0 matching pending items")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewOptionsPart1UiTest:ReviewOptionsFixture(){
 @Test fun priorityOffClearsStarsNotReview(){start();star();mark();options(false);await{rule.onAllNodesWithTag("priority_count",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("priority_count",true).assertDoesNotExist();rule.onNodeWithTag("review_progress",true).assertTextEquals("1 / 2 reviewed in this snapshot");options();await{rule.onAllNodesWithTag("priority_count",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("priority_count",true).assertTextEquals("0 /10 session priorities")}
 @Test fun hideRetainsStarsAndDetails(){start();star();hit("review_expand_topic:t1");hit("review_toggle");hit("review_toggle");rule.onNodeWithTag("priority_count",true).assertTextEquals("1 /10 session priorities");rule.onNodeWithTag("review_detail_topic:t1",true).assertExists()}
 @Test fun sourceFilterAndPriorityIntersect(){start();star();hit("priority_only");hit("review_source_calllists");rule.onNodeWithTag("review_count",true).assertTextEquals("0 matching pending items");hit("review_source_topics");rule.onNodeWithTag("review_count",true).assertTextEquals("1 matching pending items")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewOptionsSafetyUiTest:ReviewOptionsFixture(){
 @Test fun refreshClearsStarsAndExpansion(){options();ready();var s by mutableStateOf(snap());mount{Column(Modifier.verticalScroll(rememberScrollState())){BriefingReviewWorkspace(s,false)}};hit("review_toggle");star();hit("review_expand_topic:t1");rule.runOnUiThread{s=snap(2)};settle();hit("review_toggle");rule.onNodeWithTag("priority_count",true).assertTextEquals("0 /10 session priorities");rule.onNodeWithTag("review_detail_topic:t1",true).assertDoesNotExist()}
 @Test fun optionsOffOriginalRows(){start(false,false);rule.onNodeWithTag("priority_count",true).assertDoesNotExist();rule.onNodeWithTag("priority_mark_topic:t1",true).assertDoesNotExist();rule.onNodeWithTag("review_expand_topic:t1",true).assertDoesNotExist();rule.onNodeWithTag("review_detail_topic:t1",true).assertExists()}
 @Test fun busyCannotPrioritize(){options();ready();mount{Column(Modifier.verticalScroll(rememberScrollState())){BriefingReviewWorkspace(snap(),true)}};hit("review_toggle");star();rule.onNodeWithTag("priority_count",true).assertTextEquals("0 /10 session priorities")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewOptionsLiveUiTest:ReviewOptionsFixture(){
 @Test fun dayPriorityAndSlimPixelsReadOnly(){options();live();val files=listOf("call_checklists.json","manual_call_lists.json").associateWith{java.io.File(ctx.filesDir,it).readBytes().toList()};val id="topic:"+CallChecklist.read(ctx,n).single().id;star(id);move("priority_count");saveShot(rule,"OPTIONS471-day-priority");move("review_mark_$id");saveShot(rule,"OPTIONS471-day-slim");hit("review_expand_$id");move("review_mark_$id");saveShot(rule,"OPTIONS471-day-expanded");files.forEach{(f,b)->assertEquals(b,java.io.File(ctx.filesDir,f).readBytes().toList())};rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewOptionsPixelsUiTest:ReviewOptionsFixture(){
 @Test fun nightSlimFullTitle(){options();live(true);val id="topic:"+CallChecklist.read(ctx,n).single().id;star(id);move("review_mark_$id");saveShot(rule,"OPTIONS471-night-slim");hit("review_expand_$id");move("review_mark_$id");saveShot(rule,"OPTIONS471-night-expanded");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsBothOffPixels(){ready(true);options(false,false);mount{ReviewOptionsPage{}};saveShot(rule,"OPTIONS471-settings-off")}
}
