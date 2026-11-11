package com.mandu.liquiddialer
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import com.mandu.liquiddialer.telecom.CallManager
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class CallIconStudioUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1000);rule.waitForIdle()}
 private fun await(check:()->Boolean){rule.waitUntil(15000){settle();check()}}
 private fun reveal(tag:String):SemanticsNodeInteraction {
  await{rule.onAllNodesWithTag(tag,true).fetchSemanticsNodes().isNotEmpty()}
  repeat(40){
   val n=rule.onNodeWithTag(tag,true);val r=n.getUnclippedBoundsInRoot()
   val sc=rule.onAllNodes(hasScrollAction(),true).onLast();val v=sc.getUnclippedBoundsInRoot()
   if(r.bottom>r.top&&r.top>=v.top&&r.bottom<=v.bottom)return n
   sc.performTouchInput{if(r.top<v.top)swipeDown(durationMillis=120)else swipeUp(durationMillis=120)};settle()
  };error("Hidden $tag")
 }
 private fun click(tag:String){reveal(tag).performClick();settle()}
 private fun seed(theme:String="daylight",font:Float=1f){rule.mainClock.autoAdvance=false;settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.callCardStyle]="glass";it[K.callIconConfig]="";it[K.cleanGlass]=false}};await{LdApp.settings.value.let{it.theme==theme&&it.fontScale==font&&it.callIconConfig.isEmpty()}}}
 @After fun reset(){CallManager.calls.value=emptyList();CallManager.bump()}
 @Test fun studioPersistsGlobalAndSingleIconOverridesThenReset(){
  seed();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallIconStudioPage({})}}};settle();saveShot(rule,"ICONS364-studio-day")
  click("call_icons_style_6");await{CallIconConfig.decode(LdApp.settings.value.callIconConfig).base.style==6}
  click("call_icons_color_2");await{CallIconConfig.decode(LdApp.settings.value.callIconConfig).base.color==2}
  click("call_icons_target");rule.onNodeWithTag("call_icons_pick_mic").performClick();settle();click("call_icons_style_2");await{CallIconConfig.decode(LdApp.settings.value.callIconConfig).choice("mic").style==2};click("call_icons_color_3")
  await{CallIconConfig.decode(LdApp.settings.value.callIconConfig).choice("mic")==CallIconChoice(2,3)}
  val c=CallIconConfig.decode(LdApp.settings.value.callIconConfig);assertEquals(CallIconChoice(6,2),c.choice("video"));assertFalse(c.polish)
  click("call_icons_reset_one");await{CallIconConfig.decode(LdApp.settings.value.callIconConfig).overrides.isEmpty()}
  click("call_icons_reset");await{CallIconConfig.decode(LdApp.settings.value.callIconConfig)==CallIconConfig()};assertEquals(0,LdApp.settings.value.iconStyle)
 }
 @Test fun polishedCallDayNightLargeKeepTimerKeypadRedPill(){
  for(theme in listOf("daylight","midnight")){
   seed(theme,if(theme=="midnight")1.3f else 1f)
   val c=CallIconConfig(CallIconChoice(6,2),mapOf("mic" to CallIconChoice(2,3)),true);runBlocking{ctx.ldStore.edit{it[K.callIconConfig]=c.encode()}};await{CallIconConfig.decode(LdApp.settings.value.callIconConfig)==c}
   CallManager.audio.value=CallAudioState(false,CallAudioState.ROUTE_EARPIECE,CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
   CallManager.calls.value=listOf(FakeCall.make("139","Railway",Call.STATE_ACTIVE,System.currentTimeMillis()-42000));CallManager.bump()
   rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallScreen({},{},{},{})}}};settle();rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_timer",true).assertIsDisplayed();saveShot(rule,"ICONS364-call-$theme")
  }
 }
 @Test fun widerConfirmationLongNameCancelAndExplicitCallOnly(){
  var calls=0;var cancels=0
  for(theme in listOf("daylight","midnight")){
   seed(theme,if(theme=="midnight")1.3f else 1f)
   rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Box(Modifier.fillMaxSize()){VoiceConfirmCard(VoiceMatch("Railway information and enquiry service","139"),Alignment.Center,onCall={calls++},onCancel={cancels++})}}}};settle()
   val b=rule.onNodeWithTag("voice_confirm",true).fetchSemanticsNode().boundsInRoot;saveShot(rule,"ICONS364-confirm-width-$theme");assertTrue("width $b root ${rule.activity.resources.displayMetrics.widthPixels}",b.width>=rule.activity.resources.displayMetrics.widthPixels*.85f)
   rule.onNodeWithTag("voice_cancel",true).assertIsDisplayed();rule.onNodeWithTag("voice_call_now",true).assertIsDisplayed();saveShot(rule,"ICONS364-confirm-$theme")
   val old=calls;rule.onNodeWithTag("voice_cancel",true).performClick();settle();assertEquals(old,calls)
   rule.onNodeWithTag("voice_call_now",true).performClick();settle();assertEquals(old+1,calls)
  };assertEquals(2,cancels)
 }
}
