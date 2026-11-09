package com.mandu.liquiddialer

import android.app.Notification
import com.mandu.liquiddialer.telecom.CallNotificationLifecycle
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class CallNotificationLifecycleTest {
 @Test fun updatesSameOwnerAndEndsExactlyOnce() {
  val seen=mutableListOf<Notification>();var stops=0
  val host=CallNotificationLifecycle({seen+=it},{stops++})
  val a=Notification();val b=Notification()
  assertTrue(host.publish(a));assertTrue(host.publish(b));assertEquals(listOf(a,b),seen)
  host.end();host.end();assertEquals(1,stops)
 }
 @Test fun denialFallsBackWithoutRetryLoopThenNewCallCanTry() {
  var attempts=0;var stops=0
  val host=CallNotificationLifecycle({attempts++;throw SecurityException("Denied")},{stops++})
  repeat(4){assertFalse(host.publish(Notification()))};assertEquals(1,attempts)
  host.end();assertEquals(0,stops)
  assertFalse(host.publish(Notification()));assertEquals(2,attempts)
 }
 @Test fun rejectionDuringUpdateStillReleasesExistingOwnership() {
  var attempts=0;var stops=0
  val host=CallNotificationLifecycle({if(++attempts>1)throw IllegalStateException("Denied update")},{stops++})
  assertTrue(host.publish(Notification()));assertFalse(host.publish(Notification()))
  host.end();assertEquals(1,stops)
 }
 @Test fun cleanupFailureDoesNotCrashOrRemainOwned() {
  var stops=0
  val host=CallNotificationLifecycle({},{stops++;throw SecurityException("Denied stop")})
  assertTrue(host.publish(Notification()));host.end();host.end();assertEquals(1,stops)
 }
}
