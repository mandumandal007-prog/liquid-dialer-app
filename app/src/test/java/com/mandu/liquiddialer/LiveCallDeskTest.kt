package com.mandu.liquiddialer
import android.telecom.Call
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class LiveCallDeskTest{
 private val s=LdSettings(liveCallDesk=true)
 private fun call(number:String="+919876543210",state:Int=Call.STATE_ACTIVE,props:Int=0,video:Int=0,connected:Long=100)=FakeCall.make(number,"Support",state,connected,props=props,videoState=video).also{FakeCall.setChildren(it,emptyList())}
 private fun ready(c:Call=call()):Call{CallManager.calls.value=listOf(c);return c}
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.videoUpgradeFrom.value=null}
 @Test fun defaultOffFinder(){assertFalse(LdSettings().liveCallDesk);assertEquals("livecalldesk",SettingsFinder.all.single{it.id=="live-call-desk"}.route)}
 @Test fun singleConnectedAudioAllowed(){val c=ready();assertTrue(LiveCallDesk.eligible(s,c));assertNotNull(LiveCallDesk.open(s,c))}
 @Test fun offRefuses(){assertNull(LiveCallDesk.open(LdSettings(),ready()))}
 @Test fun hiddenNotesRefuses(){assertNull(LiveCallDesk.open(s.copy(hiddenFeatures=setOf("notes")),ready()))}
 @Test fun privateNumberRefuses(){assertNull(LiveCallDesk.open(s,ready(call(number=""))))}
 @Test fun codesRefuse(){assertNull(LiveCallDesk.open(s,ready(call(number="*123#"))))}
 @Test fun missingConnectedTimeRefuses(){assertNull(LiveCallDesk.open(s,ready(call(connected=0))))}
 @Test fun dialingRefuses(){assertNull(LiveCallDesk.open(s,ready(call(state=Call.STATE_DIALING))))}
 @Test fun holdRefuses(){assertNull(LiveCallDesk.open(s,ready(call(state=Call.STATE_HOLDING))))}
 @Test fun ringingRefuses(){assertNull(LiveCallDesk.open(s,ready(call(state=Call.STATE_RINGING))))}
 @Test fun disconnectedRefuses(){assertNull(LiveCallDesk.open(s,ready(call(state=Call.STATE_DISCONNECTED))))}
 @Test fun conferenceRefuses(){assertNull(LiveCallDesk.open(s,ready(call(props=Call.Details.PROPERTY_CONFERENCE))))}
 @Test fun childrenRefuse(){val c=ready();FakeCall.setChildren(c,listOf(call()));assertNull(LiveCallDesk.open(s,c))}
 @Test fun videoRefuses(){assertNull(LiveCallDesk.open(s,ready(call(video=3))))}
 @Test fun requestRefuses(){val c=ready();CallManager.videoUpgradeFrom.value=c;assertNull(LiveCallDesk.open(s,c))}
 @Test fun secondCallRefuses(){val c=ready();CallManager.calls.value=listOf(c,call());assertNull(LiveCallDesk.open(s,c))}
 @Test fun identityNotNumberRequired(){val c=ready();assertNull(LiveCallDesk.open(s,call()));val session=LiveCallDesk.open(s,c)!!;CallManager.calls.value=listOf(call());assertFalse(session.current(s))}
 @Test fun noCallRefuses(){assertNull(LiveCallDesk.open(s,call()))}
 @Test fun dispatchExactlyOnceWhenCurrent(){val session=LiveCallDesk.open(s,ready())!!;var n=0;assertTrue(session.dispatch(s){n++});assertEquals(1,n)}
 @Test fun dispatchRefusesStale(){val session=LiveCallDesk.open(s,ready())!!;CallManager.calls.value=listOf(call());var n=0;assertFalse(session.dispatch(s){n++});assertEquals(0,n)}
 @Test fun dispatchRefusesOff(){val session=LiveCallDesk.open(s,ready())!!;var n=0;assertFalse(session.dispatch(s.copy(liveCallDesk=false)){n++});assertEquals(0,n)}
 @Test fun dispatchRefusesHiddenNotes(){val session=LiveCallDesk.open(s,ready())!!;var n=0;assertFalse(session.dispatch(s.copy(hiddenFeatures=setOf("notes"))){n++});assertEquals(0,n)}
 @Test fun dispatchRefusesCallWaiting(){val c=ready();val session=LiveCallDesk.open(s,c)!!;CallManager.calls.value=listOf(c,call(state=Call.STATE_RINGING));var n=0;assertFalse(session.dispatch(s){n++});assertEquals(0,n)}
 @Test fun layoutVisibilityRespected(){assertTrue(com.mandu.liquiddialer.ui.gridLVisible(s,"mute"));assertFalse(com.mandu.liquiddialer.ui.gridLVisible(s.copy(layoutGrid="!mute,keypad,record,hold,add,merge,!audio"),"mute"));assertTrue(com.mandu.liquiddialer.ui.bottomLVisible(s,"speaker"));assertFalse(com.mandu.liquiddialer.ui.bottomLVisible(s.copy(layoutBottom="!speaker,end,notes"),"speaker"))}
}
