package com.mandu.liquiddialer
import android.provider.CallLog
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class RecentsCountBadgeFixture:RecentsOverviewFixture(){
 protected fun begin(on:Boolean=true,night:Boolean=false){runBlocking{ctx.repo.set(K.recentsCountBadge,on);ctx.repo.set(K.recentsContactNumber,false);ctx.repo.set(K.recentsStackedActions,false);ctx.repo.set(K.recentsSplitMeta,false);ctx.repo.set(K.recentsActionLabels,false)};start(night=night);rule.runOnUiThread{val a=state.recents[0];val b=state.recents[1];state.recents=listOf(a,a.copy(id=4,date=a.date-1000),a.copy(id=5,date=a.date-2000),b,b.copy(id=6,date=b.date-1000),state.recents[2])};await{rule.onAllNodesWithTag("recent_count_badge_1",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun optionBadge(on:Boolean){runBlocking{ctx.repo.set(K.recentsCountBadge,on)};await{LdApp.settings.value.recentsCountBadge==on&&rule.onAllNodesWithTag("recent_count_badge_1",true).fetchSemanticsNodes().isNotEmpty()==on}}
 @After fun resetBadge(){runBlocking{ctx.repo.set(K.recentsCountBadge,false);ctx.repo.set(K.recentsContactNumber,false);ctx.repo.set(K.recentsStackedActions,false);ctx.repo.set(K.recentsSplitMeta,false);ctx.repo.set(K.recentsActionLabels,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsCountBadgeLayoutUiTest:RecentsCountBadgeFixture(){
 @Test fun offOriginalNameSuffix(){begin(false);rule.onNodeWithText("Rahul Kumar Sharma (3)",true).assertExists();rule.onNodeWithText("Papa (2)",true).assertExists();rule.onNodeWithTag("recent_count_badge_1",true).assertDoesNotExist()}
 @Test fun onSeparateCountNameAndSingleNoBadge(){begin();rule.onNodeWithText("Rahul Kumar Sharma",true).assertExists();rule.onNodeWithText("Rahul Kumar Sharma (3)",true).assertDoesNotExist();rule.onNodeWithTag("recent_count_badge_1",true).assertTextEquals("3 calls");rule.onNodeWithTag("recent_count_badge_2",true).assertTextEquals("2 calls");rule.onNodeWithTag("recent_count_badge_3",true).assertDoesNotExist();assertEquals(6,state.recents.size)}
 @Test fun togglingKeepsDataNoCall(){begin();val before=state.recents.toList();optionBadge(false);rule.onNodeWithText("Papa (2)",true).assertExists();optionBadge(true);assertEquals(before,state.recents);assertNull(rule.activity.state.pendingCall)}
 @Test fun sameNumberDifferentTypeDoesNotMerge(){begin();rule.runOnUiThread{val a=state.recents[0];state.recents=listOf(a,a.copy(id=4,type=CallLog.Calls.INCOMING_TYPE,date=a.date-1000))};await{rule.onAllNodesWithTag("recent_count_badge_1",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("recent_count_badge_4",true).assertDoesNotExist();assertEquals(2,rule.onAllNodesWithText("Rahul Kumar Sharma",true).fetchSemanticsNodes().size)}
 @Test fun sameNameDifferentNumberDoesNotMerge(){begin();rule.runOnUiThread{val a=state.recents[0];state.recents=listOf(a,a.copy(id=4,number="+919000000004",date=a.date-1000))};await{rule.onAllNodesWithTag("recent_count_badge_1",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("recent_count_badge_4",true).assertDoesNotExist()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsCountBadgeSafetyUiTest:RecentsCountBadgeFixture(){
 @Test fun missedFilterAndSearchKeepCount(){begin();hit(1);rule.onNodeWithTag("recent_count_badge_2",true).assertTextEquals("2 calls");rule.onNodeWithTag("recent_count_badge_1",true).assertDoesNotExist();hit(0);rule.onNode(hasSetTextAction(),true).performTextReplacement("Rahul");settle();rule.onNodeWithTag("recent_count_badge_1",true).assertTextEquals("3 calls");rule.onNodeWithTag("recent_count_badge_2",true).assertDoesNotExist()}
 @Test fun dayBoundaryKeepsExistingGrouping(){begin();rule.runOnUiThread{val a=state.recents[0];state.recents=listOf(a,a.copy(id=4,date=a.date-86400000))};await{rule.onAllNodesWithTag("recent_count_badge_1",true).fetchSemanticsNodes().isEmpty()};runBlocking{ctx.repo.set(K.groupByDay,false)};await{!LdApp.settings.value.groupByDay&&rule.onAllNodesWithTag("recent_count_badge_1",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("recent_count_badge_1",true).assertTextEquals("2 calls")}
 @Test fun compactStackedAndMetadataIndependent(){begin();runBlocking{ctx.repo.set(K.recentsCompact,true);ctx.repo.set(K.recentsStackedActions,true);ctx.repo.set(K.recentsSplitMeta,true);ctx.repo.set(K.recentsContactNumber,true);ctx.repo.set(K.recentsActionLabels,true)};await{LdApp.settings.value.let{it.recentsCompact&&it.recentsStackedActions&&it.recentsSplitMeta&&it.recentsContactNumber&&it.recentsActionLabels}};rule.onNodeWithTag("recent_count_badge_1",true).assertExists();rule.onNodeWithTag("recent_stacked_card_1",true).assertExists();rule.onNodeWithTag("recent_split_type_1",true).assertExists();rule.onNodeWithTag("recent_contact_number_1",true).assertExists()}
 @Test fun defaultOffFinder(){assertFalse(LdSettings().recentsCountBadge);assertNotEquals(K.recentsCountBadge,K.recentsSplitMeta);assertEquals("settings:display",SettingsFinder.all.single{it.id=="recents-count-badge"}.route)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsCountBadgeDayPixelsUiTest:RecentsCountBadgeFixture(){
 @Test fun dayAndMissedBadge(){begin();rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_count_badge_1",true).performScrollTo();settle();rule.mainClock.advanceTimeBy(1500);rule.waitForIdle();saveShot(rule,"HOME497-day-badge");hit(1);rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_count_badge_2",true).performScrollTo();settle();rule.mainClock.advanceTimeBy(1500);rule.waitForIdle();saveShot(rule,"HOME497-day-missed");assertNull(rule.activity.state.pendingCall)}
 @Test fun settingsOffThenPersistOn(){begin(false);mount(true);rule.mainClock.autoAdvance=true;rule.onNodeWithText("Recents grouped count badge",true).performScrollTo();settle();saveShot(rule,"HOME497-settings-off");rule.onNodeWithText("Recents grouped count badge",true).performClick();await{LdApp.settings.value.recentsCountBadge};mount();await{rule.onAllNodesWithTag("recent_count_badge_1",true).fetchSemanticsNodes().size==1}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsCountBadgeNightPixelsUiTest:RecentsCountBadgeFixture(){
 @Test fun nightLargeWithStackedDetails(){begin(night=true);runBlocking{ctx.repo.set(K.recentsStackedActions,true);ctx.repo.set(K.recentsContactNumber,true);ctx.repo.set(K.recentsActionLabels,true)};await{LdApp.settings.value.let{it.recentsStackedActions&&it.recentsContactNumber&&it.recentsActionLabels}};rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_stacked_actions_1",true).performScrollTo();settle();rule.mainClock.advanceTimeBy(1500);rule.waitForIdle();rule.onNodeWithTag("recent_count_badge_1",true).assertIsDisplayed();saveShot(rule,"HOME497-night-large-badge");assertNull(rule.activity.state.pendingCall)}
}
