package com.mandu.liquiddialer

import android.app.NotificationManager
import android.os.Looper
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CallAssistServiceTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private lateinit var service:LdInCallService
 private val nm get()=Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java))
 @Before fun setup(){
  runBlocking{ctx.ldStore.edit{it.clear();it[K.muteReminder]=true;it[K.audioRouteAlert]=true}}
  CallManager.calls.value=emptyList();CallManager.audio.value=null
  service=Robolectric.buildService(LdInCallService::class.java).create().get()
  repeat(40){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle()}
 }
 @After fun cleanup(){CallManager.calls.value=emptyList();service.onDestroy();CallManager.audio.value=null;CallNotifier.cancelAll(ctx)}
 private fun tick(ms:Long){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))}
 private fun audio(mute:Boolean,route:Int){service.onCallAudioStateChanged(CallAudioState(mute,route,15))}
 @Test fun screenOffServiceWithoutActivityPostsPrivateMuteThenClearsOnUnmute(){
  Shadows.shadowOf(ctx.getSystemService(PowerManager::class.java)).setIsInteractive(false)
  val c=FakeCall.make("+12025550123","Secret Name",Call.STATE_ACTIVE);CallManager.calls.value=listOf(c)
  audio(true,CallAudioState.ROUTE_EARPIECE);tick(30000)
  val n=nm.getNotification(CallNotifier.ID_MUTE_ASSIST);assertNotNull(n)
  assertEquals(android.app.Notification.VISIBILITY_PRIVATE,n.visibility)
  assertFalse(n.extras.toString().contains("Secret Name"));assertFalse(n.extras.toString().contains("202555"));assertNotNull(n.contentIntent)
  assertEquals(true,CallManager.audio.value!!.isMuted);assertNull(CallManager.recording.value)
  audio(false,CallAudioState.ROUTE_EARPIECE);assertNull(nm.getNotification(CallNotifier.ID_MUTE_ASSIST))
 }
 @Test fun routeNoticeUsesNewPhoneRouteAndStopsWithService(){
  CallManager.calls.value=listOf(FakeCall.make("123","Name",Call.STATE_ACTIVE))
  audio(false,CallAudioState.ROUTE_BLUETOOTH);audio(false,CallAudioState.ROUTE_SPEAKER)
  val n=nm.getNotification(CallNotifier.ID_ROUTE_ASSIST);assertNotNull(n);assertTrue(n.extras.getCharSequence("android.text").toString().contains("speaker"))
  assertEquals(CallAudioState.ROUTE_SPEAKER,CallManager.audio.value!!.route)
  service.onDestroy();assertNull(nm.getNotification(CallNotifier.ID_ROUTE_ASSIST));assertNull(nm.getNotification(CallNotifier.ID_MUTE_ASSIST))
 }
 @Test fun genericNotificationPixels(){
  for(kind in CallAssistMonitor.Kind.entries){
   CallNotifier.assist(ctx,kind,CallAudioState.ROUTE_SPEAKER)
   val id=if(kind==CallAssistMonitor.Kind.MUTE)CallNotifier.ID_MUTE_ASSIST else CallNotifier.ID_ROUTE_ASSIST
   val n=nm.getNotification(id)
   val view=android.app.Notification.Builder.recoverBuilder(ctx,n).createBigContentView().apply(ctx,android.widget.FrameLayout(ctx))
   view.measure(android.view.View.MeasureSpec.makeMeasureSpec(720,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));view.layout(0,0,720,view.measuredHeight)
   val bmp=android.graphics.Bitmap.createBitmap(720,view.measuredHeight,android.graphics.Bitmap.Config.ARGB_8888);view.draw(android.graphics.Canvas(bmp))
   val file=java.io.File("build/shots/call-assist-notification-${kind.name}.png");file.parentFile!!.mkdirs();file.outputStream().use{bmp.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
  }
 }
 @Test fun liveDisableClearsNoticeAndCancelsPendingWithoutStoppingCall(){
  CallManager.calls.value=listOf(FakeCall.make("123","Name",Call.STATE_ACTIVE))
  audio(true,CallAudioState.ROUTE_BLUETOOTH);tick(30000);assertNotNull(nm.getNotification(CallNotifier.ID_MUTE_ASSIST))
  audio(true,CallAudioState.ROUTE_SPEAKER);assertNotNull(nm.getNotification(CallNotifier.ID_ROUTE_ASSIST))
  runBlocking{ctx.ldStore.edit{it[K.muteReminder]=false;it[K.audioRouteAlert]=false}}
  repeat(40){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle()}
  assertNull(nm.getNotification(CallNotifier.ID_MUTE_ASSIST));assertNull(nm.getNotification(CallNotifier.ID_ROUTE_ASSIST))
  assertEquals(Call.STATE_ACTIVE,CallManager.state(CallManager.calls.value.single()))
  assertTrue(CallManager.audio.value!!.isMuted);assertNull(CallManager.recording.value)
 }
 @Test fun multipleHoldingRingingAndConferenceCallsNeverArmAssist(){
  val c=FakeCall.make("123","Name",Call.STATE_ACTIVE)
  for(calls in listOf(listOf(c,FakeCall.make("456","Other",Call.STATE_HOLDING)),listOf(FakeCall.make("123","Name",Call.STATE_HOLDING)),listOf(FakeCall.make("123","Name",Call.STATE_RINGING)),listOf(FakeCall.make("123","Name",Call.STATE_ACTIVE,props=Call.Details.PROPERTY_CONFERENCE)))){
   CallManager.calls.value=calls;audio(true,CallAudioState.ROUTE_EARPIECE);tick(30000);assertNull(nm.getNotification(CallNotifier.ID_MUTE_ASSIST))
  }
 }
}
