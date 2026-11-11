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
class CallTouchGuardTest{
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.videoUpgradeFrom.value=null}
 private fun eligible(enabled:Boolean=true,active:Boolean=true,only:Boolean=true,conference:Boolean=false,parent:Boolean=false,children:Boolean=false,video:Boolean=false,upgrade:Boolean=false)=CallTouchGuard.eligible(enabled,active,only,conference,parent,children,video,upgrade)
 private fun call(state:Int=Call.STATE_ACTIVE,props:Int=0,video:Int=0)=FakeCall.make("+919876543210","Support",state,props=props,videoState=video).also{FakeCall.setChildren(it,emptyList())}
 @Test fun eligibleOnlyWhenSingleActiveAudio(){assertTrue(eligible())}
 @Test fun disabledRejected(){assertFalse(eligible(enabled=false))}
 @Test fun inactiveRejected(){assertFalse(eligible(active=false))}
 @Test fun secondCallRejected(){assertFalse(eligible(only=false))}
 @Test fun conferenceRejected(){assertFalse(eligible(conference=true))}
 @Test fun childCallRejected(){assertFalse(eligible(parent=true))}
 @Test fun parentCallRejected(){assertFalse(eligible(children=true))}
 @Test fun videoRejected(){assertFalse(eligible(video=true))}
 @Test fun upgradeRejected(){assertFalse(eligible(upgrade=true))}
 @Test fun startsUnlocked(){assertEquals(CallTouchGuard.State(),CallTouchGuard.State());assertFalse(CallTouchGuard.State().locked)}
 @Test fun explicitLockOnly(){assertTrue(CallTouchGuard.State().lock().locked);assertNull(CallTouchGuard.State().lock().armedAt)}
 @Test fun unlockWithoutArmCannotUnlock(){assertTrue(CallTouchGuard.State().lock().unlock(10).locked)}
 @Test fun armDoesNotUnlock(){val s=CallTouchGuard.State().lock().arm(10);assertTrue(s.locked);assertEquals(10L,s.armedAt)}
 @Test fun secondTapWithinWindowUnlocks(){assertFalse(CallTouchGuard.State().lock().arm(10).unlock(5009).locked)}
 @Test fun exactExpiryRejected(){val s=CallTouchGuard.State().lock().arm(10).unlock(5010);assertTrue(s.locked);assertNull(s.armedAt)}
 @Test fun clockBackRejected(){assertTrue(CallTouchGuard.State().lock().arm(10).unlock(9).locked)}
 @Test fun cancelRetainsGuard(){val s=CallTouchGuard.State().lock().arm(10).cancelUnlock();assertTrue(s.locked);assertNull(s.armedAt)}
 @Test fun armUnlockedNoEffect(){assertEquals(CallTouchGuard.State(),CallTouchGuard.State().arm(10))}
 @Test fun negativeTimeCannotArm(){assertNull(CallTouchGuard.State().lock().arm(-1).armedAt)}
 @Test fun sameIdentityRequired(){val a=call();val b=call();CallManager.calls.value=listOf(a);assertTrue(CallTouchGuard.eligibleCall(true,a));assertFalse(CallTouchGuard.eligibleCall(true,b))}
 @Test fun noCallRejected(){assertFalse(CallTouchGuard.eligibleCall(true,call()))}
 @Test fun disconnectedOtherCallStillClears(){val a=call();CallManager.calls.value=listOf(a,call(Call.STATE_DISCONNECTED));assertFalse(CallTouchGuard.eligibleCall(true,a))}
 @Test fun holdingCallRejected(){val a=call(Call.STATE_HOLDING);CallManager.calls.value=listOf(a);assertFalse(CallTouchGuard.eligibleCall(true,a))}
 @Test fun videoAdapterRejected(){val a=call(video=3);CallManager.calls.value=listOf(a);assertFalse(CallTouchGuard.eligibleCall(true,a))}
 @Test fun conferenceAdapterRejected(){val a=call(props=Call.Details.PROPERTY_CONFERENCE);CallManager.calls.value=listOf(a);assertFalse(CallTouchGuard.eligibleCall(true,a))}
 @Test fun childListAdapterRejected(){val a=call();FakeCall.setChildren(a,listOf(call()));CallManager.calls.value=listOf(a);assertFalse(CallTouchGuard.eligibleCall(true,a))}
 @Test fun requestAdapterRejected(){val a=call();CallManager.calls.value=listOf(a);CallManager.videoUpgradeFrom.value=a;assertFalse(CallTouchGuard.eligibleCall(true,a))}
 @Test fun settingDefaultOffAndFinder(){assertFalse(LdSettings().callTouchGuard);val e=SettingsFinder.all.single{it.id=="call-touch-guard"};assertEquals("calltouchguard",e.route)}
}
