package com.mandu.liquiddialer
import android.app.NotificationManager
import android.app.Notification
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.Duration
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CallPaceServiceTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val nm get()=Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java))
 private lateinit var service:LdInCallService
 private fun tick(ms:Long){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))}
 private fun settleSettings(){repeat(40){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle()}}
 private fun active(state:Int=Call.STATE_ACTIVE,props:Int=0)=FakeCall.make("+919876543210","Private Name",state,props=props).also{FakeCall.setChildren(it,emptyList())}
 private fun refresh(){service.onCallAudioStateChanged(CallAudioState(false,CallAudioState.ROUTE_EARPIECE,CallAudioState.ROUTE_EARPIECE))}
 @Before fun setup(){runBlocking{ctx.repo.set(K.callPace,true);ctx.repo.set(K.callPaceMinutes,1);ctx.repo.set(K.callPaceRepeat,false)};CallManager.calls.value=emptyList();service=Robolectric.buildService(LdInCallService::class.java).create().get();settleSettings()}
 @After fun clean(){service.onDestroy();CallManager.calls.value=emptyList();CallPace.status.value=null;runBlocking{ctx.repo.set(K.callPace,false)};tick(10)}
 @Test fun actualServicePostsAndHoldClears(){val c=active();CallManager.calls.value=listOf(c);refresh();tick(59999);assertNull(nm.getNotification(95));tick(1);assertNotNull(nm.getNotification(95));CallManager.calls.value=listOf(active(Call.STATE_HOLDING));refresh();assertNull(nm.getNotification(95));assertNull(CallPace.status.value)}
 @Test fun defaultsDisabledStateCannotSchedule(){runBlocking{ctx.repo.set(K.callPace,false)};settleSettings();CallManager.calls.value=listOf(active());refresh();tick(60000);assertNull(nm.getNotification(95));assertNull(CallPace.status.value)}
 @Test fun serviceDestroyClearsCountdownAndNotification(){CallManager.calls.value=listOf(active());refresh();tick(60000);assertNotNull(nm.getNotification(95));service.onDestroy();assertNull(nm.getNotification(95));assertNull(CallPace.status.value);tick(180000);assertNull(nm.getNotification(95))}
 @Test fun notificationHasNoContactDataAndSafeTap(){CallNotifier.pace(ctx,5);val n=nm.getNotification(95)!!;val body=n.extras.toString();assertFalse(body.contains("9876543210"));assertFalse(body.contains("Private Name"));assertEquals(Notification.VISIBILITY_PRIVATE,n.visibility);assertNotNull(n.publicVersion);assertNotNull(n.contentIntent);assertEquals("Call-time help",n.publicVersion.extras.getString(Notification.EXTRA_TITLE));assertTrue(n.actions.isNullOrEmpty())}
 @Test fun unsupportedStatesAndMultiCallFailClosed(){for(xs in listOf(listOf(active(Call.STATE_RINGING)),listOf(active(Call.STATE_HOLDING)),listOf(active(props=Call.Details.PROPERTY_CONFERENCE)),listOf(active(),active()))){CallManager.calls.value=xs;refresh();tick(60000);assertNull(nm.getNotification(95));assertNull(CallPace.status.value)}}
 @Test fun freshLiveStateAtDeadlinePreventsStaleNotice(){CallManager.calls.value=listOf(active());refresh();tick(30000);CallManager.calls.value=emptyList();tick(30000);assertNull(nm.getNotification(95));assertNull(CallPace.status.value)}
 @Test fun liveDisableCancelsNoticeWithoutAudioMutation(){val c=active();CallManager.calls.value=listOf(c);refresh();tick(60000);assertNotNull(nm.getNotification(95));runBlocking{ctx.repo.set(K.callPace,false)};settleSettings();assertNull(nm.getNotification(95));assertNull(CallPace.status.value);assertEquals(Call.STATE_ACTIVE,CallManager.state(c));assertFalse(CallManager.audio.value!!.isMuted);assertNull(CallManager.recording.value)}
 @Test fun genericNotificationPixels(){CallNotifier.pace(ctx,5);val n=nm.getNotification(95)!!;val view=android.app.Notification.Builder.recoverBuilder(ctx,n).createBigContentView().apply(ctx,android.widget.FrameLayout(ctx));view.measure(android.view.View.MeasureSpec.makeMeasureSpec(720,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));view.layout(0,0,720,view.measuredHeight);val bmp=android.graphics.Bitmap.createBitmap(720,view.measuredHeight,android.graphics.Bitmap.Config.ARGB_8888);val canvas=android.graphics.Canvas(bmp);canvas.drawColor(android.graphics.Color.WHITE);view.draw(canvas);val file=java.io.File("build/shots/PACE408-notification.png");file.parentFile!!.mkdirs();file.outputStream().use{bmp.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
}
