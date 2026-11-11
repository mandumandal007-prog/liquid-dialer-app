package com.mandu.liquiddialer
import android.telecom.Call
import com.mandu.liquiddialer.data.LdSettings
import com.mandu.liquiddialer.telecom.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowCall
import org.robolectric.shadow.api.Shadow
import java.time.Duration
import kotlinx.coroutines.runBlocking
import com.mandu.liquiddialer.data.K
import androidx.test.core.app.ApplicationProvider
@Implements(Call::class) class IvrCallShadow:ShadowCall(){
 val tones=mutableListOf<Char>();var stops=0
 @Implementation fun playDtmfTone(c:Char){tones.add(c)}
 @Implementation fun stopDtmfTone(){stops++}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],shadows=[IvrCallShadow::class])
class IvrToneTest{
 private fun c(state:Int=Call.STATE_ACTIVE,props:Int=0):Call{val source=FakeCall.make("+919876543210","Test",state,props=props);val call=Shadow.newInstanceOf(Call::class.java);for(name in listOf("mDetails","mState","mChildren","mConferenceableCalls")){val f=Call::class.java.getDeclaredField(name);f.isAccessible=true;f.set(call,f.get(source))};FakeCall.setChildren(call,emptyList());return call}
 private fun shadow(c:Call)=Shadow.extract<IvrCallShadow>(c)
 @Before fun setup(){val ctx=ApplicationProvider.getApplicationContext<LdApp>();runBlocking{ctx.repo.set(K.ivrHelper,true)};LdApp.settings.value=LdSettings(ivrHelper=true);CallManager.calls.value=emptyList()}
 @After fun clean(){Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(200));CallManager.calls.value=emptyList();LdApp.settings.value=LdSettings()}
 @Test fun exactTargetGetsOneToneAndStops(){val target=c();CallManager.calls.value=listOf(target);assertTrue("allowed="+IvrTone.allowed(target)+" state="+CallManager.state(target)+" flag="+LdApp.settings.value.ivrHelper,IvrTone.send(target,'2'));assertEquals(listOf('2'),shadow(target).tones);assertEquals(0,shadow(target).stops);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(150));assertEquals(1,shadow(target).stops)}
 @Test fun rapidTapCannotOverlap(){val target=c();CallManager.calls.value=listOf(target);assertTrue(IvrTone.send(target,'1'));assertFalse(IvrTone.send(target,'2'));assertEquals(listOf('1'),shadow(target).tones)}
 @Test fun replacementCallNeverReceivesOldSessionTone(){val old=c();val next=c();CallManager.calls.value=listOf(next);assertFalse(IvrTone.send(old,'1'));assertTrue(shadow(next).tones.isEmpty());assertTrue(shadow(old).tones.isEmpty())}
 @Test fun noToneOnHoldOrRingingOrDialingOrEnded(){listOf(Call.STATE_HOLDING,Call.STATE_RINGING,Call.STATE_DIALING,Call.STATE_DISCONNECTED).forEach{val target=c(it);CallManager.calls.value=listOf(target);assertFalse(IvrTone.send(target,'1'));assertTrue(shadow(target).tones.isEmpty())}}
 @Test fun multipleCallsNeverSend(){val target=c();CallManager.calls.value=listOf(target,c(Call.STATE_HOLDING));assertFalse(IvrTone.send(target,'1'));assertTrue(shadow(target).tones.isEmpty())}
 @Test fun conferenceNeverSend(){val target=c(props=Call.Details.PROPERTY_CONFERENCE);CallManager.calls.value=listOf(target);assertFalse(IvrTone.send(target,'1'))}
 @Test fun childrenNeverSend(){val target=c();FakeCall.setChildren(target,listOf(c()));CallManager.calls.value=listOf(target);assertFalse(IvrTone.send(target,'1'))}
 @Test fun disabledOrInvalidDigitNeverSend(){val target=c();CallManager.calls.value=listOf(target);assertFalse(IvrTone.send(target,'A'));LdApp.settings.value=LdSettings(ivrHelper=false);assertFalse(IvrTone.send(target,'1'));assertTrue(shadow(target).tones.isEmpty())}
 @Test fun delayedStopStaysWithOldTarget(){val target=c();val next=c();CallManager.calls.value=listOf(target);assertTrue(IvrTone.send(target,'#'));CallManager.calls.value=listOf(next);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(150));assertEquals(1,shadow(target).stops);assertEquals(0,shadow(next).stops);assertTrue(shadow(next).tones.isEmpty())}
}
