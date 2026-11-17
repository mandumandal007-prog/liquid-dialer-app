package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class RecentsStackedActionsFixture:RecentsActionLabelsFixture(){
 protected fun stacked(on:Boolean=true,night:Boolean=false,labels:Boolean=true){runBlocking{ctx.repo.set(K.recentsStackedActions,on);ctx.repo.set(K.recentsContactNumber,true)};begin(on=labels,night=night);await{rule.onAllNodesWithTag("recent_stacked_card_1",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun stackOption(on:Boolean){runBlocking{ctx.repo.set(K.recentsStackedActions,on)};await{LdApp.settings.value.recentsStackedActions==on&&rule.onAllNodesWithTag("recent_stacked_card_1",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun geometry(id:Int=1){val card=rule.onNodeWithTag("recent_stacked_card_$id",true).getUnclippedBoundsInRoot();val identity=rule.onNodeWithTag("recent_stacked_identity_$id",true).getUnclippedBoundsInRoot();val actions=rule.onNodeWithTag("recent_stacked_actions_$id",true).getUnclippedBoundsInRoot();assertTrue(identity.bottom<actions.top);assertEquals((card.right-card.left).value,(identity.right-identity.left).value,.1f);assertEquals((card.right-card.left).value,(actions.right-actions.left).value,.1f);val v=rule.onNode(hasTestTag("video_mini")and hasAnyAncestor(hasTestTag("recent_stacked_actions_$id")),true).getUnclippedBoundsInRoot();val c=rule.onNode(hasTestTag("call_mini")and hasAnyAncestor(hasTestTag("recent_stacked_actions_$id")),true).getUnclippedBoundsInRoot();assertTrue(v.right<=c.left&&v.left>=card.left&&c.right<=card.right);assertTrue((v.right-v.left).value>=44f&&(v.bottom-v.top).value>=44f&&(c.right-c.left).value>=44f&&(c.bottom-c.top).value>=44f)}
 @After fun resetStack(){runBlocking{ctx.repo.set(K.recentsStackedActions,false);ctx.repo.set(K.recentsContactNumber,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsStackedActionsLayoutUiTest:RecentsStackedActionsFixture(){
 @Test fun offOriginalLayoutNoStackedRow(){stacked(false);rule.onNodeWithTag("recent_stacked_card_1",true).assertDoesNotExist();rule.onNodeWithTag("recent_voice_label_1",true).assertExists();assertEquals(3,state.recents.size)}
 @Test fun onIdentityAboveFullWidthActions(){stacked();geometry();rule.onNodeWithTag("recent_contact_number_1",true).assertTextEquals(Repo.pretty("+919876543210"));assertEquals(3,state.recents.size)}
 @Test fun iconsOnlyStillSeparateRow(){stacked(labels=false);geometry();rule.onNodeWithTag("recent_voice_label_1",true).assertDoesNotExist()}
 @Test fun togglePreservesDataNoCall(){stacked();val before=state.recents.toList();stackOption(false);stackOption(true);geometry();assertEquals(before,state.recents);assertNull(rule.activity.state.pendingCall);assertNull(rule.activity.state.confirmNumber)}
 @Test fun compactSplitAndOriginalFilterIndependent(){stacked();runBlocking{ctx.repo.set(K.recentsCompact,true);ctx.repo.set(K.recentsSplitMeta,true)};await{LdApp.settings.value.recentsCompact&&LdApp.settings.value.recentsSplitMeta};option(false);geometry();rule.onNodeWithTag("recent_filters",true).assertExists();rule.onNodeWithTag("recent_split_type_1",true).assertExists()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsStackedActionsSafetyUiTest:RecentsStackedActionsFixture(){
 @Test fun videoKeepsNumberAndVideoFlag(){stacked();videoSimChoice();rule.onNode(hasTestTag("video_mini")and hasAnyAncestor(hasTestTag("recent_stacked_actions_1")),true).performClick();settle();assertEquals("+919876543210" to true,rule.activity.state.pendingCall)}
 @Test fun callKeepsVoiceConfirmation(){stacked();runBlocking{ctx.repo.set(K.confirmCall,true)};await{LdApp.settings.value.confirmCall};rule.onNode(hasTestTag("call_mini")and hasAnyAncestor(hasTestTag("recent_stacked_actions_1")),true).performClick();settle();assertEquals("+919876543210",rule.activity.state.confirmNumber);assertNull(rule.activity.state.pendingCall)}
 @Test fun missedAndSearchPreserveResults(){stacked();hit(1);geometry(2);rule.onNodeWithTag("recent_stacked_card_1",true).assertDoesNotExist();hit(0);rule.onNode(hasSetTextAction(),true).performTextReplacement("Priya");settle();rule.onNodeWithTag("recent_stacked_card_3",true).assertExists();rule.onNodeWithTag("recent_stacked_card_1",true).assertDoesNotExist()}
 @Test fun defaultOffFinder(){assertFalse(LdSettings().recentsStackedActions);assertNotEquals(K.recentsStackedActions,K.recentsActionLabels);assertEquals("settings:display",SettingsFinder.all.single{it.id=="recents-stacked-actions"}.route)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsStackedActionsDayPixelsUiTest:RecentsStackedActionsFixture(){
 @Test fun dayAndMissed(){stacked();geometry();rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_voice_label_1",true).performScrollTo();settle();rule.mainClock.advanceTimeBy(1500);rule.waitForIdle();rule.onNodeWithTag("recent_voice_label_1",true).assertIsDisplayed();saveShot(rule,"HOME496-day-stacked");hit(1);geometry(2);rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_voice_label_2",true).performScrollTo();settle();rule.mainClock.advanceTimeBy(1500);rule.waitForIdle();rule.onNodeWithTag("recent_voice_label_2",true).assertIsDisplayed();saveShot(rule,"HOME496-day-missed");assertNull(rule.activity.state.pendingCall)}
 @Test fun settingsOffThenPersistOn(){stacked(false);mount(true);rule.mainClock.autoAdvance=true;rule.onNodeWithText("Recents stacked actions layout",true).performScrollTo();settle();saveShot(rule,"HOME496-settings-off");rule.onNodeWithText("Recents stacked actions layout",true).performClick();await{LdApp.settings.value.recentsStackedActions};mount();await{rule.onAllNodesWithTag("recent_stacked_card_1",true).fetchSemanticsNodes().size==1}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsStackedActionsNightPixelsUiTest:RecentsStackedActionsFixture(){
 @Test fun nightLargeFullWidthIdentity(){stacked(night=true);runBlocking{ctx.repo.set(K.recentsSplitMeta,true)};await{LdApp.settings.value.recentsSplitMeta};rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_stacked_actions_1",true).performScrollTo();settle();geometry();rule.onNodeWithTag("recent_voice_label_1",true).assertIsDisplayed();saveShot(rule,"HOME496-night-large-stacked");assertNull(rule.activity.state.pendingCall)}
}
