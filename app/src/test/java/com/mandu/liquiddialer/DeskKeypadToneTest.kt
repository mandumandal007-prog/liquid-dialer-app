package com.mandu.liquiddialer
import android.telecom.Call
import android.os.Looper
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import java.time.Duration
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],shadows=[IvrCallShadow::class])
class DeskKeypadToneTest{
 private val s=LdSettings(liveCallDesk=true,deskKeypad=true)
 private fun call(number:String="+919876543210",state:Int=Call.STATE_ACTIVE,props:Int=0,video:Int=0):Call=deskShadowCall(number,state,props,video)
 private fun shadow(c:Call)=Shadow.extract<IvrCallShadow>(c)
 private fun open(c:Call=call()):LiveCallDesk.Session{CallManager.calls.value=listOf(c);return LiveCallDesk.open(s,c)!!}
 @Before fun seed(){val ctx=androidx.test.core.app.ApplicationProvider.getApplicationContext<LdApp>();kotlinx.coroutines.runBlocking{ctx.ldStore.edit{it[K.liveCallDesk]=true;it[K.deskKeypad]=true;it[K.hiddenFeatures]=emptySet();it[K.layoutGrid]=DEFAULT_GRID};ctx.repo.flow.first{it.liveCallDesk&&it.deskKeypad}};LdApp.settings.value=s;CallManager.videoUpgradeFrom.value=null}
 @After fun clean(){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));CallManager.calls.value=emptyList();LdApp.settings.value=LdSettings();CallManager.videoUpgradeFrom.value=null}
 @Test fun defaultOffFinder(){assertFalse(LdSettings().deskKeypad);assertEquals("deskkeypad",SettingsFinder.all.single{it.id=="desk-keypad"}.route)}
 @Test fun explicitKeyReachesExactCallThenStops(){val x=open();assertTrue(IvrTone.sendDesk(x,'7'));assertEquals(listOf('7'),shadow(x.target).tones);assertEquals(0,shadow(x.target).stops);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150));assertEquals(1,shadow(x.target).stops)}
 @Test fun ordinaryHelperCanStayOff(){val x=open();assertFalse(LdApp.settings.value.ivrHelper);assertTrue(IvrTone.sendDesk(x,'*'))}
 @Test fun oldHelperCannotBypassDeskOff(){val x=open();LdApp.settings.value=s.copy(deskKeypad=false,ivrHelper=true);assertFalse(IvrTone.sendDesk(x,'1'));assertTrue(shadow(x.target).tones.isEmpty())}
 @Test fun rapidTapRefused(){val x=open();assertTrue(IvrTone.sendDesk(x,'1'));assertFalse(IvrTone.sendDesk(x,'2'));assertEquals(listOf('1'),shadow(x.target).tones)}
 @Test fun singleStarAndHashWork(){val x=open();for(d in "*#0"){assertTrue(IvrTone.sendDesk(x,d));Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))};assertEquals(listOf('*','#','0'),shadow(x.target).tones)}
 @Test fun invalidKeysNeverSent(){val x=open();for(d in "Aa,; +\n"){assertFalse(IvrTone.sendDesk(x,d))};assertTrue(shadow(x.target).tones.isEmpty())}
 @Test fun masterOffRefuses(){val x=open();LdApp.settings.value=s.copy(liveCallDesk=false);assertFalse(IvrTone.sendDesk(x,'1'))}
 @Test fun keypadLayoutHiddenRefuses(){val x=open();LdApp.settings.value=s.copy(layoutGrid="mute,!keypad,record,hold,add,merge,!audio");assertFalse(IvrTone.sendDesk(x,'1'))}
 @Test fun notesHiddenRefuses(){val x=open();LdApp.settings.value=s.copy(hiddenFeatures=setOf("notes"));assertFalse(IvrTone.sendDesk(x,'1'))}
 @Test fun replacementSameNumberRefusesBothTargets(){val x=open();val n=call();CallManager.calls.value=listOf(n);assertFalse(IvrTone.sendDesk(x,'1'));assertTrue(shadow(x.target).tones.isEmpty());assertTrue(shadow(n).tones.isEmpty())}
 @Test fun secondCallRefuses(){val x=open();CallManager.calls.value=listOf(x.target,call("+919999999999"));assertFalse(IvrTone.sendDesk(x,'1'))}
 @Test fun holdRefuses(){val x=open();setDeskState(x.target,Call.STATE_HOLDING);assertEquals(Call.STATE_HOLDING,CallManager.state(x.target));assertFalse(IvrTone.sendDesk(x,'1'))}
 @Test fun endRefuses(){val x=open();CallManager.calls.value=emptyList();assertFalse(IvrTone.sendDesk(x,'1'))}
 @Test fun videoRequestRefuses(){val x=open();CallManager.videoUpgradeFrom.value=x.target;assertFalse(IvrTone.sendDesk(x,'1'))}
 @Test fun delayedStopNeverSwitchesCall(){val x=open();assertTrue(IvrTone.sendDesk(x,'#'));val n=call();CallManager.calls.value=listOf(n);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150));assertEquals(1,shadow(x.target).stops);assertEquals(0,shadow(n).stops);assertTrue(shadow(n).tones.isEmpty())}
 @Test fun sharedBusyGuardWithOldHelper(){val x=open();LdApp.settings.value=s.copy(ivrHelper=true);assertTrue(IvrTone.sendDesk(x,'1'));assertFalse(IvrTone.send(x.target,'2'));assertEquals(listOf('1'),shadow(x.target).tones)}
}
internal fun deskShadowCall(number:String="+919876543210",state:Int=Call.STATE_ACTIVE,props:Int=0,video:Int=0):Call{
 val source=FakeCall.make(number,"Support desk",state,System.currentTimeMillis()-92000,props=props,videoState=video);val c=Shadow.newInstanceOf(Call::class.java)
 for(name in listOf("mDetails","mState","mChildren","mConferenceableCalls")){val f=Call::class.java.getDeclaredField(name);f.isAccessible=true;f.set(c,f.get(source))};FakeCall.setChildren(c,emptyList());return c
}
internal fun setDeskState(c:Call,state:Int){val f=Call.Details::class.java.getDeclaredField("mState");f.isAccessible=true;f.set(c.details,state);CallManager.bump()}
