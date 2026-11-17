package com.mandu.liquiddialer
import android.Manifest
import android.content.ComponentName
import android.telecom.PhoneAccountHandle
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
abstract class RecentsActionLabelsFixture:RecentsOverviewFixture(){
 protected fun begin(on:Boolean=true,night:Boolean=false,overview:Boolean=true){runBlocking{ctx.repo.set(K.recentsActionLabels,on);ctx.repo.set(K.recentsSplitMeta,false)};start(on=overview,night=night);await{rule.onAllNodesWithTag("recent_voice_label_1",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun labels(on:Boolean){runBlocking{ctx.repo.set(K.recentsActionLabels,on)};await{LdApp.settings.value.recentsActionLabels==on&&rule.onAllNodesWithTag("recent_voice_label_1",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun videoSimChoice(){Shadows.shadowOf(ctx).grantPermissions(Manifest.permission.CALL_PHONE);runBlocking{ctx.repo.set(K.autoPrefix,false);ctx.repo.set(K.simMemory,false);ctx.repo.set(K.defaultSim,"")};await{!LdApp.settings.value.autoPrefix&&!LdApp.settings.value.simMemory};val cn=ComponentName("com.mandu","Phone");rule.runOnUiThread{rule.activity.state.sims=listOf(SimInfo(PhoneAccountHandle(cn,"sim0"),"Jio",1),SimInfo(PhoneAccountHandle(cn,"sim1"),"Airtel",2))};ContactControls.setSimPin(ctx,"+919876543210",null)}
 @After fun resetLabels(){runBlocking{ctx.repo.set(K.recentsActionLabels,false);ctx.repo.set(K.recentsSplitMeta,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsActionLabelsLayoutUiTest:RecentsActionLabelsFixture(){
 @Test fun offHasOriginalButtonsWithoutLabels(){begin(false);rule.onNodeWithTag("recent_video_label_1",true).assertDoesNotExist();assertTrue(rule.onAllNodesWithTag("call_mini",true).fetchSemanticsNodes().isNotEmpty());assertTrue(rule.onAllNodesWithTag("video_mini",true).fetchSemanticsNodes().isNotEmpty())}
 @Test fun onSeparateLabelsBelowButtons(){begin();rule.onNodeWithTag("recent_video_label_1",true).assertTextEquals("Video");rule.onNodeWithTag("recent_voice_label_1",true).assertTextEquals("Call");val v=rule.onNodeWithTag("recent_video_action_1",true).getUnclippedBoundsInRoot();val c=rule.onNodeWithTag("recent_voice_action_1",true).getUnclippedBoundsInRoot();assertTrue(v.right<=c.left);for(tag in listOf("recent_video_label_1","recent_voice_label_1"))rule.onNodeWithTag(tag,true).assertIsDisplayed();assertEquals(3,state.recents.size)}
 @Test fun togglingRetainsRowsAndNoCall(){begin();val before=state.recents.toList();labels(false);labels(true);assertEquals(before,state.recents);assertNull(rule.activity.state.pendingCall);assertNull(rule.activity.state.confirmNumber)}
 @Test fun compactOriginalControlsAndSplitMetaIndependent(){begin(overview=false);runBlocking{ctx.repo.set(K.recentsCompact,true);ctx.repo.set(K.recentsSplitMeta,true)};await{LdApp.settings.value.recentsCompact&&LdApp.settings.value.recentsSplitMeta};rule.onNodeWithTag("recent_video_label_1",true).assertExists();rule.onNodeWithTag("recent_split_type_1",true).assertExists();rule.onNodeWithTag("recent_filters",true).assertExists()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsActionLabelsActionsUiTest:RecentsActionLabelsFixture(){
 @Test fun videoButtonKeepsNumberAndVideoFlag(){begin();videoSimChoice();rule.onNode(hasTestTag("video_mini")and hasAnyAncestor(hasTestTag("recent_video_action_1")),true).performClick();settle();assertEquals("+919876543210" to true,rule.activity.state.pendingCall)}
 @Test fun callButtonKeepsVoiceConfirmation(){begin();runBlocking{ctx.repo.set(K.confirmCall,true)};await{LdApp.settings.value.confirmCall};rule.onNode(hasTestTag("call_mini")and hasAnyAncestor(hasTestTag("recent_voice_action_1")),true).performClick();settle();assertEquals("+919876543210",rule.activity.state.confirmNumber);assertNull(rule.activity.state.pendingCall)}
 @Test fun defaultOffFinderRoute(){assertFalse(LdSettings().recentsActionLabels);assertNotEquals(K.recentsActionLabels,K.recentsSplitMeta);assertEquals("settings:display",SettingsFinder.all.single{it.id=="recents-action-labels"}.route)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsActionLabelsDayPixelsUiTest:RecentsActionLabelsFixture(){
 @Test fun dayAndMissedLabels(){begin();saveShot(rule,"HOME494-day-labels");hit(1);rule.onNodeWithTag("recent_video_label_2",true).assertIsDisplayed();saveShot(rule,"HOME494-day-missed");assertNull(rule.activity.state.pendingCall)}
 @Test fun settingsOffThenPersistOn(){begin(false);mount(true);rule.mainClock.autoAdvance=true;rule.onNodeWithText("Recents labeled call actions",true).performScrollTo();settle();saveShot(rule,"HOME494-settings-off");rule.onNodeWithText("Recents labeled call actions",true).performClick();await{LdApp.settings.value.recentsActionLabels};mount();await{rule.onAllNodesWithTag("recent_video_label_1",true).fetchSemanticsNodes().size==1}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsActionLabelsNightPixelsUiTest:RecentsActionLabelsFixture(){
 @Test fun nightLargeLabelsAndLongName(){begin(night=true);rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_video_label_1",true).performScrollTo();settle();rule.onNodeWithTag("recent_voice_label_1",true).assertIsDisplayed();saveShot(rule,"HOME494-night-large-labels");assertNull(rule.activity.state.pendingCall)}
}
