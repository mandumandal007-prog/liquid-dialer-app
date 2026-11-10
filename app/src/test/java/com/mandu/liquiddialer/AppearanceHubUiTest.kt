package com.mandu.liquiddialer
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class AppearanceHubUiTest{
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1200);rule.waitForIdle()}
 private fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 private fun seed(theme:String="daylight",font:Float=1f,premium:Boolean=true){settle();runBlocking{app.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.motion]=false;it[K.airyCall]=true;it[K.premiumConfirm]=premium;it[K.callCardStyle]="glass";it[K.callIconConfig]=""}};await{LdApp.settings.value.let{it.theme==theme&&it.fontScale==font&&it.premiumConfirm==premium&&it.airyCall}}}
 private fun mount(content:@androidx.compose.runtime.Composable ()->Unit){rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{content()}}};settle()}
 private fun revealNode(n:SemanticsNodeInteraction):SemanticsNodeInteraction {repeat(25){val r=n.getUnclippedBoundsInRoot();val sc=rule.onNodeWithTag("page_content_scroll",true);val v=sc.getUnclippedBoundsInRoot();if(r.bottom>r.top&&r.top>=v.top&&r.bottom<=v.bottom)return n;sc.performTouchInput{val delta=if(r.top<v.top)height*.18f else -height*.18f;swipe(center,center.copy(y=center.y+delta),120)};settle()};error("unreachable")}
 private fun reveal(text:String)=revealNode(rule.onNodeWithText(text,true,true))
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.bump()}
 @Test fun defaultsRemainOff(){assertFalse(LdSettings().airyCall);assertFalse(LdSettings().premiumConfirm)}
 @Test fun hubRoutesReachExistingControls(){seed();val routes=mutableListOf<String>();mount{AppearanceHubPage({},{routes+=it})};saveShot(rule,"AP387-hub-day");reveal("Theme, icons & text").performClick();settle();assertEquals("appearance",routes.last());reveal("Calling card layout").performClick();settle();assertEquals("calllayout",routes.last())}
 @Test fun premiumSettingPersistsAndPreviewNeverCalls(){seed(premium=false);mount{AppearanceHubPage({},{})};reveal("Premium confirmation finish").performClick();await{LdApp.settings.value.premiumConfirm};revealNode(rule.onNodeWithTag("confirm_finish_preview",true));settle();saveShot(rule,"AP387-hub-confirm");rule.onNodeWithTag("voice_call_now",true).performClick();settle();assertNull(rule.activity.state.pendingCall)}
 @Test fun airySettingPersists(){seed();mount{AppearanceHubPage({},{})};reveal("Airy call controls").performClick();await{!LdApp.settings.value.airyCall};runBlocking{assertFalse(app.ldStore.data.first()[K.airyCall]!!)}}
 @Test fun sameSizeConfirmCancelAndExplicitCallDayNightLarge(){var calls=0;var cancels=0
  for(theme in listOf("daylight","midnight")){seed(theme,if(theme=="midnight")1.3f else 1f,false);mount{Box(Modifier.fillMaxSize()){VoiceConfirmCard(VoiceMatch("Railway information and enquiry service","139"),Alignment.Center,{calls++},{cancels++})}};val before=rule.onNodeWithTag("voice_confirm",true).fetchSemanticsNode().boundsInRoot
   runBlocking{app.repo.set(K.premiumConfirm,true)};await{LdApp.settings.value.premiumConfirm};val after=rule.onNodeWithTag("voice_confirm",true).fetchSemanticsNode().boundsInRoot;assertEquals(before,after);rule.onNodeWithTag("voice_call_now",true).assertIsDisplayed();rule.onNodeWithTag("voice_cancel",true).assertIsDisplayed();saveShot(rule,"AP387-confirm-$theme");val n=calls;rule.onNodeWithTag("voice_cancel",true).performClick();settle();assertEquals(n,calls);rule.onNodeWithTag("voice_call_now",true).performClick();settle();assertEquals(n+1,calls)
  };assertEquals(2,cancels)
 }
 @Test fun airyRealCallRetainsEndTimerAndKeypadDayNightLarge(){for(theme in listOf("daylight","midnight")){seed(theme,if(theme=="midnight")1.3f else 1f);CallManager.audio.value=CallAudioState(false,CallAudioState.ROUTE_EARPIECE,9);CallManager.calls.value=listOf(FakeCall.make("139","Railway",Call.STATE_ACTIVE,System.currentTimeMillis()-42000));CallManager.bump();mount{CallScreen({},{},{},{})};rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_timer",true).assertIsDisplayed();rule.onNodeWithText("Mute",true,true).assertIsDisplayed();saveShot(rule,"AP387-call-$theme");rule.onNodeWithText("Keypad",true,true).performClick();settle();rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"AP387-keypad-$theme")}}
 @Test fun hubLargeFontControlsRemainReachable(){seed("midnight",1.3f);mount{AppearanceHubPage({},{})};saveShot(rule,"AP387-hub-night-large");reveal("Airy call controls").assertIsDisplayed();saveShot(rule,"AP387-hub-airy-large");reveal("Premium confirmation finish").assertIsDisplayed();revealNode(rule.onNodeWithTag("confirm_finish_preview",true));settle();saveShot(rule,"AP387-hub-preview-large")}
}
