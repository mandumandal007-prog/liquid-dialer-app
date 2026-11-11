package com.mandu.liquiddialer
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@OptIn(ExperimentalTestApi::class)
abstract class CallTouchGuardFixture{
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,ComponentActivity::class.java));runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false};ctx.repo.flow.first{it.onboarded&&!it.motion}}}}
 @get:Rule(order=1) val rule=createAndroidComposeRule<ComponentActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 protected val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private var host:ComposeView?=null
 protected lateinit var target:Call
 protected fun mount(content:@Composable ()->Unit){rule.runOnUiThread{val view=ComposeView(rule.activity);host=view;view.setContent{LdThemeHost{content()}};rule.activity.setContentView(view)};settle()}
 protected fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(300);rule.waitForIdle()}
 protected fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 protected fun seed(enabled:Boolean=true,night:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.callTouchGuard]=enabled;it[K.callReferences]=false;it[K.callPace]=false;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.confirmBeforeEnd]=true;it[K.refinedHeaders]=true}};await{LdApp.settings.value.let{it.callTouchGuard==enabled&&it.fontScale==(if(night)1.4f else 1f)&&it.confirmBeforeEnd}};CallManager.videoUpgradeFrom.value=null;CallManager.recording.value=null;CallPace.status.value=null}
 protected fun show(enabled:Boolean=true,night:Boolean=false){seed(enabled,night);target=FakeCall.make("+919876543210","Support desk",Call.STATE_ACTIVE,System.currentTimeMillis()-92000);FakeCall.setChildren(target,emptyList());CallManager.calls.value=listOf(target);mount{CallScreen({},{},{},{})}}
 protected fun click(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}
 @After fun clean(){rule.runOnUiThread{host?.disposeComposition();rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content).removeAllViews()};CallManager.calls.value=emptyList();CallManager.videoUpgradeFrom.value=null;runBlocking{ctx.ldStore.edit{it[K.callTouchGuard]=false;it[K.fontScale]=1f;it[K.refinedHeaders]=false;it[K.confirmBeforeEnd]=false}}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallTouchGuardUiTest:CallTouchGuardFixture(){
 @Test fun offNoButtonOrGuard(){show(false);rule.onAllNodesWithTag("touch_guard_enable",true).assertCountEquals(0);rule.onAllNodesWithTag("touch_guard_view",true).assertCountEquals(0);rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun guardRemovesControlSemanticsKeepsEndAndIdentity(){show();saveShot(rule,"GUARD412-available");click("touch_guard_enable");rule.onNodeWithTag("touch_guard_view",true).assertExists();rule.onAllNodesWithTag("call_button_Mute",true).assertCountEquals(0);rule.onAllNodesWithTag("call_button_Keypad",true).assertCountEquals(0);rule.onAllNodesWithTag("call_button_Record",true).assertCountEquals(0);rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("touch_guard_name",true).assertTextEquals("Support desk");assertNull(CallManager.recording.value);assertEquals(Call.STATE_ACTIVE,CallManager.state(target));saveShot(rule,"GUARD412-guarded-day")}
 @Test fun twoDistinctTapsRequired(){show();click("touch_guard_enable");click("touch_guard_arm");rule.onNodeWithTag("touch_guard_view",true).assertExists();rule.onAllNodesWithTag("touch_guard_arm",true).assertCountEquals(0);saveShot(rule,"GUARD412-unlock-step");click("touch_guard_continue");rule.onAllNodesWithTag("touch_guard_view",true).assertCountEquals(0);rule.onNodeWithTag("call_button_Mute",true).assertExists()}
 @Test fun cancelRetainsGuard(){show();click("touch_guard_enable");click("touch_guard_arm");click("touch_guard_cancel");rule.onNodeWithTag("touch_guard_arm",true).assertExists();rule.onAllNodesWithTag("touch_guard_continue",true).assertCountEquals(0)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallTouchGuardResetUiTest:CallTouchGuardFixture(){
 @Test fun endRespectsExistingConfirmation(){show();click("touch_guard_enable");click("end_call");rule.onNodeWithTag("end_confirm",true).assertExists();assertEquals(Call.STATE_ACTIVE,CallManager.state(target));click("end_confirm_cancel");rule.onNodeWithTag("touch_guard_view",true).assertExists()}
 @Test fun secondCallClearsNeverAutoRelocks(){show();click("touch_guard_enable");val other=FakeCall.make("+919999999999","Other",Call.STATE_RINGING);CallManager.calls.value=listOf(target,other);settle();rule.onAllNodesWithTag("touch_guard_view",true).assertCountEquals(0);CallManager.calls.value=listOf(target);settle();rule.onNodeWithTag("touch_guard_enable",true).assertExists();rule.onAllNodesWithTag("touch_guard_view",true).assertCountEquals(0);click("touch_guard_enable");rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);settle();rule.onAllNodesWithTag("touch_guard_view",true).assertCountEquals(0)}
 @Test fun offClearsGuard(){show();click("touch_guard_enable");runBlocking{ctx.repo.set(K.callTouchGuard,false)};await{!LdApp.settings.value.callTouchGuard};rule.onAllNodesWithTag("touch_guard_view",true).assertCountEquals(0);rule.onAllNodesWithTag("touch_guard_enable",true).assertCountEquals(0)}
 @Test fun backgroundClearsGuard(){show();click("touch_guard_enable");rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);settle();rule.onAllNodesWithTag("touch_guard_view",true).assertCountEquals(0);rule.onNodeWithTag("touch_guard_enable",true).assertExists()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallTouchGuardPixelsUiTest:CallTouchGuardFixture(){
 @Test fun settingsReadOnlyAndEnable(){seed(false);mount{CallTouchGuardPage({})};assertFalse(LdApp.settings.value.callTouchGuard);saveShot(rule,"GUARD412-settings-day");rule.onAllNodesWithText("Call touch guard",useUnmergedTree=true)[1].performClick();await{LdApp.settings.value.callTouchGuard}}
 @Test fun nightLargeGuardPixels(){show(night=true);click("touch_guard_enable");rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"GUARD412-night-large");click("touch_guard_arm");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("touch_guard_continue",true).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false;saveShot(rule,"GUARD412-night-large-unlock");click("touch_guard_continue");rule.onAllNodesWithTag("touch_guard_view",true).assertCountEquals(0)}
 @Test fun replacementCannotInheritGuard(){show();click("touch_guard_enable");target=FakeCall.make("+919876543210","Same number new call",Call.STATE_ACTIVE);CallManager.calls.value=listOf(target);settle();rule.onAllNodesWithTag("touch_guard_view",true).assertCountEquals(0)}
 @Test fun unlockExpiryRetainsGuard(){show();click("touch_guard_enable");click("touch_guard_arm");rule.mainClock.advanceTimeBy(5200);settle();rule.onNodeWithTag("touch_guard_arm",true).assertExists();rule.onAllNodesWithTag("touch_guard_continue",true).assertCountEquals(0)}
}
