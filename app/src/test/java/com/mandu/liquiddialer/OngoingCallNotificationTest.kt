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

/** Flags and native template are app evidence, not a physical OEM swipe/chip test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[30,31,34])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class OngoingCallNotificationTest {
 private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val nm get()=Shadows.shadowOf(app.getSystemService(NotificationManager::class.java))
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.audio.value=null;CallNotifier.cancelAll(app)}
 @Test fun nativeStyleFlagsReturnAndActions(){
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
   if(Build.VERSION.SDK_INT>=31){
    assertEquals("android.app.Notification\$CallStyle",n.extras.getString(Notification.EXTRA_TEMPLATE))
    assertNull(n.contentView)
    val intents=n.actions.map{Shadows.shadowOf(it.actionIntent).savedIntent.action}.toSet()
    assertEquals(setOf(CallActionReceiver.MUTE,CallActionReceiver.SPEAKER,CallActionReceiver.HANGUP),intents)
    assertTrue(n.actions.any{it.title.toString()=="Unmute"});assertTrue(n.actions.any{it.title.toString()=="Speaker off"})
   }else assertNotNull(n.contentView)
  }
  CallManager.calls.value=listOf(FakeCall.make("123","Ended",Call.STATE_DISCONNECTED));CallNotifier.update(app);assertNull(nm.getNotification(CallNotifier.ID_CALL))
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
 @Test @Config(sdk=[34]) fun nativeTemplatePixels(){
  android.os.SystemClock.setCurrentTimeMillis(System.currentTimeMillis())
  CallNotifier.createChannels(app)
  for(state in listOf(Call.STATE_ACTIVE,Call.STATE_HOLDING,Call.STATE_DIALING)){
   CallManager.calls.value=listOf(FakeCall.make("+12025550123","Railways",state,connectMillis=System.currentTimeMillis()-12000))
   CallManager.audio.value=CallAudioState(false,CallAudioState.ROUTE_EARPIECE,15);CallNotifier.update(app)
   val n=nm.getNotification(CallNotifier.ID_CALL)
   val view=Notification.Builder.recoverBuilder(app,n).createBigContentView().apply(app,android.widget.FrameLayout(app))
   val host=Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
   host.setContentView(view)
   Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
   view.measure(android.view.View.MeasureSpec.makeMeasureSpec(720,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));view.layout(0,0,720,view.measuredHeight);view.viewTreeObserver.dispatchOnPreDraw()
   val bmp=android.graphics.Bitmap.createBitmap(720,view.measuredHeight,android.graphics.Bitmap.Config.ARGB_8888);val canvas=android.graphics.Canvas(bmp);canvas.drawColor(android.graphics.Color.WHITE);view.draw(canvas)
   val f=java.io.File("build/shots/HP-native-call-$state.png");f.parentFile!!.mkdirs();f.outputStream().use{bmp.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};host.finish()
  }
 }
}
