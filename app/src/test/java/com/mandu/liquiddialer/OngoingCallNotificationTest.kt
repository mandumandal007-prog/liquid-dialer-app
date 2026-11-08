package com.mandu.liquiddialer

import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.telecom.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** Posting prerequisites and lifecycle are app evidence, not a physical OEM swipe/chip test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[30,31,34])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class OngoingCallNotificationTest {
 private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val nm get()=Shadows.shadowOf(app.getSystemService(NotificationManager::class.java))
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.audio.value=null;CallNotifier.cancelAll(app)}
 @Test fun compatibleStyleFlagsReturnAndActions(){
  CallNotifier.createChannels(app)
  val connect=System.currentTimeMillis()-45000
  for(state in listOf(Call.STATE_DIALING,Call.STATE_ACTIVE,Call.STATE_HOLDING)){
   CallManager.calls.value=listOf(FakeCall.make("+12025550123","Test caller",state,connectMillis=connect))
   CallManager.audio.value=CallAudioState(true,CallAudioState.ROUTE_SPEAKER,15)
   CallNotifier.update(app);val n=nm.getNotification(CallNotifier.ID_CALL);assertNotNull(n)
   assertEquals(Notification.CATEGORY_CALL,n.category);assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0);assertEquals(0,n.flags and Notification.FLAG_AUTO_CANCEL)
   val i=Shadows.shadowOf(n.contentIntent).savedIntent
   assertEquals(CallActivity::class.java.name,i.component!!.className);assertFalse(i.hasExtra("action"));assertNull(n.deleteIntent)
   assertEquals(state==Call.STATE_ACTIVE,n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
   assertNotNull(n.contentView)
   assertNotEquals("android.app.Notification\$CallStyle",n.extras.getString(Notification.EXTRA_TEMPLATE))
   // Model Android system-server admission, missing from Robolectric's NotificationManager.
   val nativeStyle=n.extras.getString(Notification.EXTRA_TEMPLATE)=="android.app.Notification\$CallStyle"
   assertTrue("CallStyle requires real FGS or fullscreen intent",!nativeStyle || n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0 || n.fullScreenIntent!=null)

  }
  CallManager.calls.value=listOf(FakeCall.make("123","Ended",Call.STATE_DISCONNECTED));CallNotifier.update(app);assertNull(nm.getNotification(CallNotifier.ID_CALL))
 }
 @org.robolectric.annotation.Implements(NotificationManager::class)
 class RejectingNotificationManager: org.robolectric.shadows.ShadowNotificationManager(){
  @org.robolectric.annotation.Implementation
  override fun notify(tag:String?,id:Int,n:Notification){throw IllegalArgumentException("Test system rejection")}
 }
 @Test @Config(sdk=[34],shadows=[RejectingNotificationManager::class]) fun rejectedPostIsLogged(){
  CallNotifier.createChannels(app)
  CallManager.calls.value=listOf(FakeCall.make("+12025550123","Test",Call.STATE_ACTIVE))
  org.robolectric.shadows.ShadowLog.clear()
  CallNotifier.update(app)
  assertTrue(org.robolectric.shadows.ShadowLog.getLogsForTag("CallNotifier").any{it.msg=="Call notification rejected" && it.throwable is IllegalArgumentException})
 }
 @Test fun serviceKeepsReturnNotificationUntilLastCallEnds(){
  CallNotifier.createChannels(app)
  val svc=Robolectric.buildService(LdInCallService::class.java).create().get()
  try {
   val a=FakeCall.make("+12025550123","First",Call.STATE_ACTIVE)
   val b=FakeCall.make("+12025550124","Second",Call.STATE_HOLDING)
   for(c in listOf(a,b)){val f=Call::class.java.getDeclaredField("mCallbackRecords");f.isAccessible=true;f.set(c,java.util.concurrent.CopyOnWriteArrayList<Any>())}
   CallManager.calls.value=listOf(a,b)
   svc.onCallAudioStateChanged(CallAudioState(false,CallAudioState.ROUTE_EARPIECE,15))
   assertNotNull(nm.getNotification(CallNotifier.ID_CALL))
   svc.onCallRemoved(a);assertNotNull(nm.getNotification(CallNotifier.ID_CALL));assertEquals("Second",nm.getNotification(CallNotifier.ID_CALL).extras.getCharSequence(Notification.EXTRA_TITLE).toString())
   svc.onCallRemoved(b);assertNull(nm.getNotification(CallNotifier.ID_CALL))
  }finally{svc.onDestroy()}
 }
}
