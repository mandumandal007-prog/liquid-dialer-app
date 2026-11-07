package com.mandu.liquiddialer

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceCallStyleTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1200);rule.waitForIdle()}
    @After fun reset(){CallManager.calls.value=emptyList();CallManager.bump()}
    private fun render(key:String,ringing:Boolean=false,large:Boolean=false){
        runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]="midnight";it[K.callCardStyle]=key;it[K.callBgStyle]="blobs";it[K.fontScale]=if(large)1.3f else 1f;it[K.swipeAnswer]=false}}
        settle()
        rule.waitUntil(15000){LdApp.settings.value.callCardStyle==key}
        CallManager.audio.value=CallAudioState(false,CallAudioState.ROUTE_EARPIECE,CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.calls.value=listOf(FakeCall.make("+91 90000 00000","Asha Example",if(ringing)Call.STATE_RINGING else Call.STATE_ACTIVE,System.currentTimeMillis()-42000))
        CallManager.bump()
        rule.runOnUiThread{rule.activity.setContent{LdThemeHost{CallScreen({},{},{},{})}}};settle()
    }
    private fun bounds(tag:String){
        val node=rule.onNodeWithTag(tag,useUnmergedTree=true).assertIsDisplayed().fetchSemanticsNode()
        val b=node.boundsInRoot
        assertTrue("$tag left",b.left>=0);assertTrue("$tag right",b.right<=rule.activity.resources.displayMetrics.widthPixels)
        assertTrue("$tag bottom",b.bottom<=rule.activity.resources.displayMetrics.heightPixels)
    }
    @Test fun sixActiveCallStylesKeepControlsAndRedPill(){
        REFERENCE_CALL_KEYS.forEach{key->render(key);bounds("end_call");bounds("call_name");rule.onNodeWithText("HD VOICE",useUnmergedTree=true).assertDoesNotExist();rule.onNodeWithText("HD",useUnmergedTree=true).assertDoesNotExist();saveShot(rule,"ref-call-$key")}
    }
    @Test fun sixIncomingLargeTextScreens(){
        REFERENCE_CALL_KEYS.forEach{key->render(key,true,true);bounds("call_name");saveShot(rule,"ref-incoming-$key")}
    }
    @Test fun selectableWithoutChangingRecentsOrDefault(){
        runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]="daylight";it[K.callCardStyle]="glass";it[K.recentsStyle]="mono"}}
        settle();rule.runOnUiThread{rule.activity.setContent{LdThemeHost{StyleStudioPage({})}}};settle()
        REFERENCE_CALL_KEYS.forEach{key->
            rule.mainClock.autoAdvance=true;rule.onNodeWithTag("card_style_$key",useUnmergedTree=true).performScrollTo().performClick();settle()
            rule.waitUntil(15000){LdApp.settings.value.callCardStyle==key}
            assertEquals("mono",LdApp.settings.value.recentsStyle)
        }
    }
    @Test fun activeLargeControlsKeypadAndHdTruth(){
        for(key in listOf("pearl","cyber")){
            render(key,large=true);bounds("end_call")
            rule.onNodeWithText("Keypad",useUnmergedTree=true).performTouchInput{click(androidx.compose.ui.geometry.Offset(center.x,center.y-45.dp.toPx()))};settle()
            rule.onNodeWithText("1",useUnmergedTree=true).assertIsDisplayed();settle()
            bounds("end_call");saveShot(rule,"ref-large-keypad-$key")
            rule.onNodeWithText("Keypad band karo",useUnmergedTree=true).performClick();settle()
        }
        render("pearl")
        CallManager.calls.value=listOf(FakeCall.make("+91 90000 00000","Asha Example",Call.STATE_ACTIVE,System.currentTimeMillis()-42000,props=Call.Details.PROPERTY_HIGH_DEF_AUDIO));CallManager.bump();settle()
        rule.onNodeWithText("HD",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithText("HD VOICE",useUnmergedTree=true).assertIsDisplayed()
    }
    @Test fun customPhotoRemainsOnLightAndDarkPresets(){
        val f=java.io.File(ctx.cacheDir,"reference-photo.jpg")
        val b=android.graphics.Bitmap.createBitmap(400,700,android.graphics.Bitmap.Config.ARGB_8888)
        val canvas=android.graphics.Canvas(b);canvas.drawColor(android.graphics.Color.rgb(80,120,165))
        val paint=android.graphics.Paint().apply{color=android.graphics.Color.WHITE};canvas.drawCircle(200f,200f,120f,paint)
        f.outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.JPEG,95,it)};b.recycle()
        assertTrue(CallPhotos.save(ctx,"+919000000000",android.net.Uri.fromFile(f)))
        try{
            for(key in listOf("pearl","jadegold")){
                render(key,large=true);bounds("end_call");bounds("call_name")
                rule.onNodeWithText("Mute",useUnmergedTree=true).assertIsDisplayed()
                saveShot(rule,"ref-photo-$key")
            }
        }finally{CallPhotos.remove(ctx,"+919000000000")}
    }

}
