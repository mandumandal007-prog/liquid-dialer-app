package com.mandu.liquiddialer
import android.os.Handler
import android.os.Looper
import com.mandu.liquiddialer.telecom.*
import com.mandu.liquiddialer.data.*
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Duration
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CallPaceMonitorTest{
 private val call=Any();private var input=CallPaceMonitor.Input(call,true,1,false)
 private val alerts=mutableListOf<Int>();private val states=mutableListOf<CallPace.Status?>();private var cancels=0
 private lateinit var monitor:CallPaceMonitor
 @Before fun setup(){monitor=CallPaceMonitor(Handler(Looper.getMainLooper()),{input},{alerts.add(it)},{cancels++},publish={states.add(it)})}
 @After fun clean(){monitor.stop()}
 private fun tick(ms:Long){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))}
 @Test fun optInDefaultOff(){assertFalse(LdSettings().callPace);assertEquals(5,LdSettings().callPaceMinutes);assertFalse(LdSettings().callPaceRepeat)}
 @Test fun savedIntervalSanitized(){assertEquals(5,mutablePreferencesOf(K.callPaceMinutes to 0).toSettings().callPaceMinutes);assertEquals(15,mutablePreferencesOf(K.callPaceMinutes to 15).toSettings().callPaceMinutes)}
 @Test fun fullIntervalBeforeOneShot(){monitor.refresh();tick(59999);assertTrue(alerts.isEmpty());tick(1);assertEquals(listOf(1),alerts);assertTrue(states.last()!!.reminded);assertEquals(0,states.last()!!.deadline);repeat(4){monitor.refresh();tick(60000)};assertEquals(1,alerts.size)}
 @Test fun repeatingHasOneDeadlineAtATime(){input=input.copy(repeat=true);monitor.refresh();tick(60000);assertEquals(listOf(1),alerts);assertTrue(states.last()!!.deadline>0);tick(60000);assertEquals(listOf(1,1),alerts)}
 @Test fun refreshDoesNotPostponeDeadline(){monitor.refresh();tick(30000);repeat(5){monitor.refresh()};tick(30000);assertEquals(1,alerts.size)}
 @Test fun disabledNeverSchedules(){input=input.copy(enabled=false);monitor.refresh();tick(600000);assertTrue(alerts.isEmpty());assertNull(states.last())}
 @Test fun invalidIntervalFailsClosed(){input=input.copy(minutes=2);monitor.refresh();tick(600000);assertTrue(alerts.isEmpty())}
 @Test fun holdOrEndClearsCountdownAndNotice(){monitor.refresh();tick(30000);input=input.copy(target=null);monitor.refresh();tick(60000);assertTrue(alerts.isEmpty());assertNull(states.last());assertTrue(cancels>0)}
 @Test fun resumeStartsFreshStretch(){monitor.refresh();tick(40000);input=input.copy(target=null);monitor.refresh();input=input.copy(target=call);monitor.refresh();tick(20000);assertTrue(alerts.isEmpty());tick(40000);assertEquals(1,alerts.size)}
 @Test fun callReplacementCannotInheritTime(){monitor.refresh();tick(50000);input=input.copy(target=Any());monitor.refresh();tick(10000);assertTrue(alerts.isEmpty());tick(50000);assertEquals(1,alerts.size)}
 @Test fun timingChangeRestartsCountdown(){monitor.refresh();tick(50000);input=input.copy(minutes=3);monitor.refresh();tick(10000);assertTrue(alerts.isEmpty());tick(170000);assertEquals(listOf(3),alerts)}
 @Test fun repeatModeChangeStartsFresh(){monitor.refresh();tick(50000);input=input.copy(repeat=true);monitor.refresh();tick(10000);assertTrue(alerts.isEmpty());tick(50000);assertEquals(1,alerts.size)}
 @Test fun deadlineRechecksOffWithoutCallback(){monitor.refresh();input=input.copy(enabled=false);tick(60000);assertTrue(alerts.isEmpty());assertNull(states.last())}
 @Test fun deadlineRechecksNewCallWithoutCallback(){monitor.refresh();input=input.copy(target=Any());tick(60000);assertTrue(alerts.isEmpty());tick(60000);assertEquals(1,alerts.size)}
 @Test fun destroyCancelsAllFutureTicks(){input=input.copy(repeat=true);monitor.refresh();monitor.stop();tick(600000);assertTrue(alerts.isEmpty());assertNull(states.last())}
 @Test fun deadlineUsesProvidedMonotonicClock(){val m=CallPaceMonitor(Handler(Looper.getMainLooper()),{input},{},{},clock={1234L},publish={states.add(it)});m.refresh();assertEquals(61234L,states.last()!!.deadline);m.stop()}
}
