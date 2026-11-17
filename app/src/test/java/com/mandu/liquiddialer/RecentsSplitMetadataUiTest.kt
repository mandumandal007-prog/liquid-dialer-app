package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class RecentsSplitMetadataFixture:RecentsOverviewFixture(){
 protected fun split(on:Boolean){runBlocking{ctx.repo.set(K.recentsSplitMeta,on)};await{LdApp.settings.value.recentsSplitMeta==on&&rule.onAllNodesWithTag("recent_split_type_1",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun begin(on:Boolean=true,night:Boolean=false,overview:Boolean=true){runBlocking{ctx.repo.set(K.recentsSplitMeta,on)};start(on=overview,night=night);await{rule.onAllNodesWithTag(if(on)"recent_split_meta_1"else"recent_original_meta_1",true).fetchSemanticsNodes().size==1}}
 @After fun resetSplit(){runBlocking{ctx.repo.set(K.recentsSplitMeta,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsSplitMetadataLayoutUiTest:RecentsSplitMetadataFixture(){
 @Test fun offOriginalMetadata(){begin(false);rule.onNodeWithTag("recent_original_meta_1",true).assertTextContains("Outgoing",true);rule.onNodeWithTag("recent_split_type_1",true).assertDoesNotExist()}
 @Test fun splitTypesSeparateAndDurationRetained(){begin();for((id,label)in listOf(1 to "Outgoing",2 to "Missed",3 to "Incoming"))rule.onNodeWithTag("recent_split_type_$id",true).assertTextEquals(label);rule.onNodeWithTag("recent_split_meta_1",true).assertTextContains("4:05",true);assertFalse(rule.onNodeWithTag("recent_split_meta_2",true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString().contains("0:00"))}
 @Test fun toggleKeepsRowsAndSelection(){begin();hit(1);val before=state.recents.toList();runBlocking{ctx.repo.set(K.recentsSplitMeta,false)};await{!LdApp.settings.value.recentsSplitMeta&&rule.onAllNodesWithTag("recent_split_type_2",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("recent_filter_1",true).assertIsSelected();runBlocking{ctx.repo.set(K.recentsSplitMeta,true)};await{rule.onAllNodesWithTag("recent_split_type_2",true).fetchSemanticsNodes().size==1};assertEquals(before,state.recents);assertNull(state.pendingCall)}
 @Test fun compactAndOriginalControlsIndependent(){begin(overview=false);runBlocking{ctx.repo.set(K.recentsCompact,true)};await{LdApp.settings.value.recentsCompact};rule.onNodeWithTag("recent_filters",true).assertExists();rule.onNodeWithTag("recent_split_type_1",true).assertTextEquals("Outgoing");assertEquals(3,state.recents.size)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsSplitMetadataSettingsUiTest:RecentsSplitMetadataFixture(){
 @Test fun defaultOffFinderRoute(){assertFalse(LdSettings().recentsSplitMeta);assertNotEquals(K.recentsSplitMeta,K.recentsCompact);assertEquals("settings:display",SettingsFinder.all.single{it.id=="recents-split-meta"}.route)}
 @Test fun settingsOffThenPersistOn(){begin(false);mount(true);rule.mainClock.autoAdvance=true;rule.onNodeWithText("Recents split metadata layout",true).performScrollTo();settle();saveShot(rule,"HOME493-settings-off");rule.onNodeWithText("Recents split metadata layout",true).performClick();await{LdApp.settings.value.recentsSplitMeta};mount();await{rule.onAllNodesWithTag("recent_split_type_1",true).fetchSemanticsNodes().size==1}}
 @Test fun durationOffDoesNotAddFakeTime(){begin();runBlocking{ctx.repo.set(K.recentsShowDuration,false)};await{!LdApp.settings.value.recentsShowDuration};assertFalse(rule.onNodeWithTag("recent_split_meta_1",true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString().contains("4:05"))}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsSplitMetadataDayPixelsUiTest:RecentsSplitMetadataFixture(){
 @Test fun dayRowsAndFilters(){begin();saveShot(rule,"HOME493-day-split");hit(1);rule.onNodeWithTag("recent_split_type_2",true).assertIsDisplayed();saveShot(rule,"HOME493-day-missed");assertNull(state.pendingCall)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsSplitMetadataNightPixelsUiTest:RecentsSplitMetadataFixture(){
 @Test fun nightLargeTypeMetadata(){begin(night=true);rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_split_meta_1",true).performScrollTo();settle();rule.onNodeWithTag("recent_split_type_1",true).assertIsDisplayed();saveShot(rule,"HOME493-night-large-split");assertNull(state.pendingCall)}
}
