package com.mandu.liquiddialer

import android.content.Intent
import android.media.RingtoneManager
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.FakeCalls
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@org.robolectric.annotation.LooperMode(org.robolectric.annotation.LooperMode.Mode.PAUSED)
class FakeCallActivityTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @Before fun reset(){FakeCalls.cancel(ctx);ctx.getSystemService(android.media.AudioManager::class.java).ringerMode=android.media.AudioManager.RINGER_MODE_SILENT}
 @After fun end(){com.mandu.liquiddialer.telecom.CallManager.calls.value=emptyList();FakeCalls.cancel(ctx);Shadows.shadowOf(ctx.getSystemService(android.telecom.TelecomManager::class.java)).setIsInCall(false)}
 @Test fun opensOnlyValidTokenAndConsumesIt(){
  val p=FakeCalls.preview(ctx)
  val c=Robolectric.buildActivity(FakeCallActivity::class.java,Intent(ctx,FakeCallActivity::class.java).putExtra("token",p.token)).create()
  assertFalse(c.get().isFinishing);assertNull(FakeCalls.current(ctx))
  val replay=Robolectric.buildActivity(FakeCallActivity::class.java,Intent(ctx,FakeCallActivity::class.java).putExtra("token",p.token)).create()
  assertTrue(replay.get().isFinishing);replay.destroy();c.destroy()
 }
 @Test fun realCallPreventsPreviewAndAlert(){
  val p=FakeCalls.schedule(ctx,"Demo",30_000,100_000)
  Shadows.shadowOf(ctx.getSystemService(android.telecom.TelecomManager::class.java)).setIsInCall(true)
  assertFalse(FakeCalls.deliver(ctx,p.token,130_000));assertNull(FakeCalls.current(ctx))
  try{FakeCalls.preview(ctx);fail("real call takes precedence")}catch(_:IllegalStateException){}
 }
 @Test fun liveCallStopsSimulation(){
  val p=FakeCalls.preview(ctx)
  val c=Robolectric.buildActivity(FakeCallActivity::class.java,Intent(ctx,FakeCallActivity::class.java).putExtra("token",p.token)).create()
  com.mandu.liquiddialer.telecom.CallManager.calls.value=listOf(org.robolectric.util.ReflectionHelpers.callConstructor(android.telecom.Call::class.java))
  Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(1,TimeUnit.SECONDS)
  assertTrue(c.get().isFinishing);c.destroy()
 }
 @Test fun timeoutFinishesAndDoesNotRestore(){
  val p=FakeCalls.preview(ctx)
  val c=Robolectric.buildActivity(FakeCallActivity::class.java,Intent(ctx,FakeCallActivity::class.java).putExtra("token",p.token)).create()
  Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(46,TimeUnit.SECONDS)
  assertTrue(c.get().isFinishing);assertNull(FakeCalls.current(ctx));c.destroy()
 }
}
