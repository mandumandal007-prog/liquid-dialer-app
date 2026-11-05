package com.mandu.liquiddialer
import android.telecom.Call
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallPhotoScreenTest {
 @get:Rule val rule=createAndroidComposeRule<CallActivity>()
 private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1800);rule.waitForIdle();Thread.sleep(150);rule.mainClock.advanceTimeBy(300);rule.waitForIdle()}
 @Before fun setup() {
  runBlocking { app.ldStore.edit { it[K.callPhotoBlur]=0 } }
  val f=File(app.cacheDir,"landscape.jpg");val b=Bitmap.createBitmap(400,700,Bitmap.Config.ARGB_8888);val c=android.graphics.Canvas(b)
  val p=android.graphics.Paint().apply{shader=android.graphics.LinearGradient(0f,0f,400f,700f,intArrayOf(0xff213a61.toInt(),0xffcf9d75.toInt(),0xff426f71.toInt()),null,android.graphics.Shader.TileMode.CLAMP)};c.drawRect(0f,0f,400f,700f,p);p.shader=null;p.color=0xffede2bf.toInt();c.drawCircle(300f,170f,65f,p);p.color=0xff284049.toInt();c.drawOval(-100f,440f,500f,900f,p)
  f.outputStream().use {b.compress(Bitmap.CompressFormat.JPEG,90,it)};b.recycle();assertTrue(CallPhotos.save(app,"9876543210",Uri.fromFile(f)))
  CallManager.audio.value=android.telecom.CallAudioState(false,1,9)
 }
 @After fun clean(){CallManager.calls.value=emptyList();CallPhotos.remove(app,"9876543210");org.robolectric.RuntimeEnvironment.setFontScale(1f)}
 @Test fun customBackgroundIncomingOutgoingOngoingBothThemesLargeFont() {
  for(theme in listOf("daylight","midnight")) {
   runBlocking {app.ldStore.edit {it[K.theme]=theme;it[K.callBgStyle]="solid";it[K.fontScale]=1.3f;it[K.motion]=false}}
   for(state in listOf(Call.STATE_RINGING,Call.STATE_DIALING,Call.STATE_ACTIVE)) {
    CallManager.calls.value=listOf(FakeCall.make("+91 98765 43210","Rahul Sharma",state,if(state==Call.STATE_ACTIVE)System.currentTimeMillis()-70000 else 0));CallManager.bump();settle()
    rule.onNodeWithTag("hero_avatar").assertIsDisplayed()
    if(state==Call.STATE_ACTIVE) {rule.onNodeWithTag("call_timer").assertIsDisplayed();rule.onNodeWithText("Mute").assertIsDisplayed()}
    saveShot(rule,"G220-call-$theme-$state-large")
   }
  }
 }
 @Test fun brightBusyPhotoKeepsCallActionsAndIdentityReadable() {
  val f=File(app.cacheDir,"busy.jpg");val b=Bitmap.createBitmap(400,700,Bitmap.Config.ARGB_8888);val canvas=android.graphics.Canvas(b);val p=android.graphics.Paint()
  for(y in 0 until 700 step 20) for(x in 0 until 400 step 20){p.color=if((x/20+y/20)%2==0)android.graphics.Color.WHITE else android.graphics.Color.YELLOW;canvas.drawRect(x.toFloat(),y.toFloat(),x+20f,y+20f,p)}
  f.outputStream().use {b.compress(Bitmap.CompressFormat.JPEG,90,it)};b.recycle();assertTrue(CallPhotos.save(app,"9876543210",Uri.fromFile(f)))
  for(theme in listOf("daylight","midnight")) {
   runBlocking{app.ldStore.edit {it[K.theme]=theme;it[K.fontScale]=1.3f;it[K.motion]=false}}
   CallManager.calls.value=listOf(FakeCall.make("+919876543210","Rahul Sharma",Call.STATE_ACTIVE,System.currentTimeMillis()-70000));CallManager.bump();settle()
   rule.onNodeWithText("Mute").assertIsDisplayed();rule.onNodeWithTag("call_timer").assertIsDisplayed()
   saveShot(rule,"G220-call-busy-$theme-large")
  }
 }

 @Test fun photoBlurLevelsRenderAndKeepCallControlsVisible() {
  for(level in 0..2) {
   runBlocking {app.ldStore.edit {it[K.theme]="daylight";it[K.callPhotoBlur]=level;it[K.motion]=false}}
   rule.waitUntil(15000) { LdApp.settings.value.callPhotoBlur==level }
   CallManager.calls.value=listOf(FakeCall.make("+919876543210","Rahul Sharma",Call.STATE_ACTIVE,System.currentTimeMillis()-70000));CallManager.bump();settle()
   rule.onNodeWithTag("call_timer").assertIsDisplayed();rule.onNodeWithTag("end_call").assertIsDisplayed()
   saveShot(rule,"G256-photo-blur-"+level)
  }
 }

}
