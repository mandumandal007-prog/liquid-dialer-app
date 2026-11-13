package com.mandu.liquiddialer
import android.telecom.Call
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import com.mandu.liquiddialer.telecom.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallPaceUiTest{
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(900);rule.waitForIdle()}
 private fun await(t:()->Boolean){rule.waitUntil(15000){settle();t()}}
 private fun show(night:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.refinedHeaders]=true;it[K.callPace]=false;it[K.callPaceMinutes]=5;it[K.callPaceRepeat]=false}};await{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.callPace&&it.callPaceMinutes==5&&!it.callPaceRepeat}};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallPacePage({})}}};settle()}
 private fun text(t:String){rule.mainClock.autoAdvance=true;rule.onNodeWithText(t,useUnmergedTree=true).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithText(t,useUnmergedTree=true).performClick();settle()}
 @After fun clean(){CallManager.calls.value=emptyList();CallPace.status.value=null;runBlocking{ctx.ldStore.edit{it[K.callPace]=false;it[K.callPaceRepeat]=false;it[K.fontScale]=1f;it[K.refinedHeaders]=false}}}
 @Test fun openingIsReadOnly(){show();assertFalse(LdApp.settings.value.callPace);assertEquals(5,LdApp.settings.value.callPaceMinutes);assertNull(CallPace.status.value)}
 @Test fun intervalChangeDoesNotEnable(){show();text("10 min");await{LdApp.settings.value.callPaceMinutes==10};assertFalse(LdApp.settings.value.callPace);assertFalse(LdApp.settings.value.callPaceRepeat)}
 @Test fun repeatDoesNotEnableOrChangeAudio(){show();text("Repeat during same stretch");await{LdApp.settings.value.callPaceRepeat};assertFalse(LdApp.settings.value.callPace);assertNull(CallManager.recording.value)}
 @Test fun exampleTracksSelectedInterval(){show();text("3 min");await{LdApp.settings.value.callPaceMinutes==3};
 // The settings flow update precedes collectAsState/recomposition. Await the
 // actual rendered example, not only the repository's value.
 await{rule.onAllNodesWithTag("pace_example",true).fetchSemanticsNodes().any{n->n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text){emptyList()}.any{it.text.contains("3 min active")}}};rule.onNodeWithTag("pace_example",true).assertTextContains("3 min active",substring=true)}
 @Test fun daySettingsPixels(){show();saveShot(rule,"PACE408-day");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("pace_example",true).performScrollTo();rule.waitForIdle();settle();saveShot(rule,"PACE408-explanation")}
 @Test fun nightLargePixels(){show(true);saveShot(rule,"PACE408-night-large")}
 @Test fun actualCountdownCallPixels(){show();val target=FakeCall.make("+919876543210","Support",Call.STATE_ACTIVE,System.currentTimeMillis()-90000);FakeCall.setChildren(target,emptyList());CallManager.calls.value=listOf(target);CallPace.status.value=CallPace.Status(target,android.os.SystemClock.uptimeMillis()+59000,1,false);rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallScreen({},{},{},{})}}};settle();rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithText("PACE 00:59",useUnmergedTree=true).assertExists();saveShot(rule,"PACE408-active-countdown")}
 @Test fun differentCallNeverShowsOldCountdown(){show();val target=FakeCall.make("+919876543210","Support",Call.STATE_ACTIVE);val old=FakeCall.make("+919999999999","Other",Call.STATE_ACTIVE);CallManager.calls.value=listOf(target);CallPace.status.value=CallPace.Status(old,android.os.SystemClock.uptimeMillis()+60000,1,false);rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallScreen({},{},{},{})}}};settle();rule.onAllNodesWithText("PACE",substring=true).assertCountEquals(0)}
}
