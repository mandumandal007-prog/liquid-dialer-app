package com.mandu.liquiddialer

import android.os.Handler
import android.os.Looper
import android.telecom.CallAudioState as A
import com.mandu.liquiddialer.telecom.CallAssistMonitor
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CallAssistMonitorTest {
 private val call=Any()
 private var input=CallAssistMonitor.Input(call,true,A.ROUTE_EARPIECE,true,true)
 private val alerts=mutableListOf<Pair<CallAssistMonitor.Kind,Int>>()
 private val cancels=mutableListOf<CallAssistMonitor.Kind?>()
 private lateinit var monitor:CallAssistMonitor
 @Before fun setup(){monitor=CallAssistMonitor(Handler(Looper.getMainLooper()),{input},{kind,r->alerts+=kind to r},{cancels+=it})}
 @After fun close(){monitor.stop()}
 private fun tick(ms:Long){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))}
 @Test fun muteWarnsAfterThirtySecondsOnlyOncePerStretch(){
  monitor.refresh();tick(29999);assertTrue(alerts.isEmpty());tick(1);assertEquals(1,alerts.size)
  repeat(5){monitor.refresh();tick(30_000)};assertEquals(1,alerts.size)
  input=input.copy(muted=false);monitor.refresh();input=input.copy(muted=true);monitor.refresh();tick(30_000);assertEquals(2,alerts.size)
 }
 @Test fun defaultsAndDisabledStateNeverAlert(){input=input.copy(muteEnabled=false,routeEnabled=false);monitor.refresh();tick(60_000);input=input.copy(route=A.ROUTE_BLUETOOTH);monitor.refresh();input=input.copy(route=A.ROUTE_SPEAKER);monitor.refresh();assertTrue(alerts.isEmpty())}
 @Test fun unmuteOffHoldOrEndCancelsPendingAndExistingNotice(){
  for(change in listOf<(CallAssistMonitor.Input)->CallAssistMonitor.Input>({it.copy(muted=false)},{it.copy(muteEnabled=false)},{it.copy(activeSingleCall=null)})){
   input=CallAssistMonitor.Input(call,true,A.ROUTE_EARPIECE,true,true);monitor.refresh();tick(10000);input=change(input);monitor.refresh();tick(30000)
   assertTrue(alerts.isEmpty());assertTrue(CallAssistMonitor.Kind.MUTE in cancels)
  }
 }
 @Test fun fireRechecksFreshOptInAndCallEvenWithoutCallback(){monitor.refresh();input=input.copy(muteEnabled=false);tick(30000);assertTrue(alerts.isEmpty());input=input.copy(muteEnabled=true);monitor.refresh();input=input.copy(activeSingleCall=Any());tick(30000);assertTrue(alerts.isEmpty())}
 @Test fun changedCallerGetsFreshTimer(){monitor.refresh();tick(20000);input=input.copy(activeSingleCall=Any());monitor.refresh();tick(10000);assertTrue(alerts.isEmpty());tick(20000);assertEquals(1,alerts.size)}
 @Test fun headsetToPhoneWarnsOnlyOnTransitionAndReconnectionClears(){
  input=input.copy(muted=false,route=A.ROUTE_BLUETOOTH);monitor.refresh();assertTrue(alerts.isEmpty())
  input=input.copy(route=A.ROUTE_SPEAKER);monitor.refresh();assertEquals(CallAssistMonitor.Kind.ROUTE to A.ROUTE_SPEAKER,alerts.single())
  repeat(5){monitor.refresh()};assertEquals(1,alerts.size)
  input=input.copy(route=A.ROUTE_WIRED_HEADSET);monitor.refresh();assertTrue(CallAssistMonitor.Kind.ROUTE in cancels)
  input=input.copy(route=A.ROUTE_EARPIECE);monitor.refresh();assertEquals(2,alerts.size)
 }
 @Test fun initialPhoneAndOrdinarySpeakerSwitchNeverPretendHeadsetLoss(){input=input.copy(muted=false);monitor.refresh();input=input.copy(route=A.ROUTE_SPEAKER);monitor.refresh();assertTrue(alerts.isEmpty())}
 @Test fun holdOrNewCallHasNoStaleHeadsetTransition(){input=input.copy(muted=false,route=A.ROUTE_BLUETOOTH);monitor.refresh();input=input.copy(activeSingleCall=null);monitor.refresh();input=input.copy(activeSingleCall=Any(),route=A.ROUTE_SPEAKER);monitor.refresh();assertTrue(alerts.isEmpty())}
}
