package com.mandu.liquiddialer
import android.telecom.Call
import android.telecom.CallScreeningService
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.LdScreeningService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class QuietWeekScreeningTest{
 private fun schedule():String{val c=java.util.Calendar.getInstance();val m=c.get(11)*60+c.get(12);return QuietWeek.encode(listOf(QuietWeek.Window("now","Now",(0..6).toSet(),(m+1439)%1440,(m+1)%1440)))}
 private fun screen(s:LdSettings,incoming:Boolean=true):CallScreeningService.CallResponse{
  val controller=Robolectric.buildService(LdScreeningService::class.java).create();val svc=controller.get()
  // This service fixture supplies the complete settings snapshot. Join the app's
  // asynchronous startup collector first so its default emission cannot race it.
  val app=androidx.test.core.app.ApplicationProvider.getApplicationContext<LdApp>()
  runBlocking{app.scope.coroutineContext[Job]!!.cancelAndJoin()}
  LdApp.settings.value=s
  val details=FakeCall.make("+919876543210","",Call.STATE_RINGING).details
  val f=Call.Details::class.java.getDeclaredField("mCallDirection");f.isAccessible=true;f.set(details,if(incoming)Call.Details.DIRECTION_INCOMING else Call.Details.DIRECTION_OUTGOING)
  svc.onScreenCall(details);val response=Shadows.shadowOf(svc as CallScreeningService).lastRespondToCallInput.get().callResponse;return response // Robolectric CallScreeningService shadow is not a ShadowService; sandbox handles teardown.
 }
 @After fun clean(){ContactControls.setQuietAllow(androidx.test.core.app.ApplicationProvider.getApplicationContext<LdApp>(),"+919876543210",false);LdApp.settings.value=LdSettings()}
 @Test fun actualServiceSilencesWithoutRejectOrSkip(){val r=screen(LdSettings(quietHours=true,quietWeekMode=true,quietWeekRules=schedule(),quietAllowFav=false));assertTrue(r.silenceCall);assertFalse(r.disallowCall);assertFalse(r.rejectCall);assertFalse(r.skipCallLog);assertFalse(r.skipNotification)}
 @Test fun actualServiceMasterOffAndOutgoingStayAudible(){val s=LdSettings(quietHours=true,quietWeekMode=true,quietWeekRules=schedule(),quietAllowFav=false);assertFalse(screen(s.copy(quietHours=false)).silenceCall);assertFalse(screen(s,false).silenceCall)}
 @Test fun actualServiceCorruptWeeklyDataFailsOpen(){assertFalse(screen(LdSettings(quietHours=true,quietWeekMode=true,quietWeekRules="broken",quietAllowFav=false)).silenceCall)}
 @Test fun favouriteUnknownWithoutPermissionStaysAudible(){assertFalse(screen(LdSettings(quietHours=true,quietWeekMode=true,quietWeekRules=schedule(),quietAllowFav=true)).silenceCall)}
 @Test fun perNumberBypassStillRings(){ContactControls.setQuietAllow(androidx.test.core.app.ApplicationProvider.getApplicationContext<LdApp>(),"+919876543210",true);assertFalse(screen(LdSettings(quietHours=true,quietWeekMode=true,quietWeekRules=schedule(),quietAllowFav=false)).silenceCall)}
 @Test fun explicitBlockWinsWithoutSilence(){val r=screen(LdSettings(quietHours=true,quietWeekMode=true,quietWeekRules=schedule(),quietAllowFav=false,blockedPrefixes=listOf("919876")));assertTrue(r.disallowCall);assertTrue(r.rejectCall);assertFalse(r.silenceCall)}
}
