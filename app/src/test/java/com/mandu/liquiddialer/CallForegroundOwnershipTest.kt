package com.mandu.liquiddialer

import android.app.Notification
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.telecom.Call
import android.telecom.TelecomManager
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.telecom.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** App lifecycle checks only. Does not simulate Realme shade or prove swipe resistance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33,34])
class CallForegroundOwnershipTest {
 private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val nm get()=Shadows.shadowOf(app.getSystemService(NotificationManager::class.java))
 @Before fun prepare(){
  CallManager.calls.value=emptyList();CallManager.service=null
  Shadows.shadowOf(app.getSystemService(TelecomManager::class.java)).setDefaultDialerPackage(app.packageName)
  CallNotifier.createChannels(app)
 }
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.service?.onDestroy();CallManager.service=null;CallNotifier.cancelAll(app)}
 @Test fun actualBoundServiceOwnsSameCustomRowAndStopsAtLastCall(){
  val svc=Robolectric.buildService(LdInCallService::class.java).create().get()
  val shadow=Shadows.shadowOf(svc)
  val a=FakeCall.make("+12025550123","Active",Call.STATE_ACTIVE)
  val b=FakeCall.make("+12025550124","Held",Call.STATE_HOLDING)
  CallManager.calls.value=listOf(a,b);CallNotifier.update(app)
  assertEquals(CallNotifier.ID_CALL,shadow.lastForegroundNotificationId)
  assertTrue(shadow.isLastForegroundNotificationAttached)
  assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL,svc.foregroundServiceType)
  val n=shadow.lastForegroundNotification
  assertNotNull(n.contentView);assertNull(n.deleteIntent)
  assertEquals("androidx.core.app.NotificationCompat\$DecoratedCustomViewStyle",n.extras.getString("androidx.core.app.extra.COMPAT_TEMPLATE"))
  assertEquals(0,n.flags and Notification.FLAG_AUTO_CANCEL)
  CallManager.calls.value=listOf(b);CallNotifier.update(app)
  assertFalse(shadow.isForegroundStopped)
  assertEquals("Held",shadow.lastForegroundNotification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
  CallManager.calls.value=emptyList();CallNotifier.update(app)
  assertTrue(shadow.isForegroundStopped);assertNull(nm.getNotification(CallNotifier.ID_CALL))
 }
 @Test fun nonDefaultDialerKeepsOrdinaryRowRatherThanLosingNotification(){
  val svc=Robolectric.buildService(LdInCallService::class.java).create().get()
  Shadows.shadowOf(app.getSystemService(TelecomManager::class.java)).setDefaultDialerPackage("other.dialer")
  CallManager.calls.value=listOf(FakeCall.make("+12025550123","Fallback",Call.STATE_ACTIVE))
  CallNotifier.update(app);CallNotifier.update(app)
  assertFalse(Shadows.shadowOf(svc).isLastForegroundNotificationAttached)
  val n=nm.getNotification(CallNotifier.ID_CALL);assertNotNull(n);assertNotNull(n.contentView)
  assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT!=0)
 }
 @Test fun serviceDestructionReleasesForegroundAndNoCallNeverPromotes(){
  val svc=Robolectric.buildService(LdInCallService::class.java).create().get()
  val shadow=Shadows.shadowOf(svc)
  assertFalse(svc.publishCallNotification(Notification()))
  CallManager.calls.value=listOf(FakeCall.make("+12025550123","Active",Call.STATE_ACTIVE));CallNotifier.update(app)
  assertTrue(shadow.isLastForegroundNotificationAttached)
  svc.onDestroy();assertTrue(shadow.isForegroundStopped);assertNull(CallManager.service)
 }
}
