package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.core.edit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GlassPhotoUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(600);rule.waitForIdle()}
    private fun await(check:()->Boolean){repeat(30){org.robolectric.shadows.ShadowLooper.idleMainLooper();settle();if(check())return;Thread.sleep(100)};assertTrue("photo="+CallPhotos.get(app,"9876543210")+" settings="+LdApp.settings.value+" tree="+rule.onAllNodes(isRoot(),useUnmergedTree=true).printToString(),check())}
    @Before fun seed(){rule.runOnUiThread { rule.activity.state.showOnboarding=false }}
    @After fun clean(){CallPhotos.remove(app,"9876543210");LdApp.settings.value=LdSettings()}
    private fun photo():Uri {
        val f=File(app.cacheDir,"test-background.png");val b=Bitmap.createBitmap(360,640,Bitmap.Config.ARGB_8888)
        val c=android.graphics.Canvas(b);val p=android.graphics.Paint().apply {shader=android.graphics.LinearGradient(0f,0f,360f,640f,intArrayOf(0xff173561.toInt(),0xffe4a76d.toInt(),0xff436b75.toInt()),null,android.graphics.Shader.TileMode.CLAMP)};c.drawRect(0f,0f,360f,640f,p)
        p.shader=null;p.color=0xffc8d1c2.toInt();c.drawCircle(260f,160f,72f,p);p.color=0xff28404c.toInt();c.drawOval(-100f,390f,500f,900f,p)
        f.outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle();return Uri.fromFile(f)
    }
    @Test fun callingCardBlurChoicePersistsAndRenders() {
        runBlocking { app.ldStore.edit { it[K.theme]="daylight";it[K.onboarded]=true;it[K.callPhotoBlur]=0 } }
        await { LdApp.settings.value.theme=="daylight" && LdApp.settings.value.callPhotoBlur==0 }
        rule.activity.setContent { LdThemeHost { CallLayoutPage {} } };settle()
        rule.mainClock.autoAdvance=true
        rule.onNodeWithText("Halka",useUnmergedTree=true).performScrollTo().performClick()
        await { LdApp.settings.value.callPhotoBlur==1 }
        rule.onAllNodes(hasScrollAction(),useUnmergedTree=true).onLast().performTouchInput { swipe(center,center.copy(y=center.y-120f),300) };settle()
        saveShot(rule,"G256-blur-setting")
        rule.onNodeWithText("Soft",useUnmergedTree=true).performClick()
        await { LdApp.settings.value.callPhotoBlur==2 }
        rule.onNodeWithText("Clear / Off",useUnmergedTree=true).performClick()
        await { LdApp.settings.value.callPhotoBlur==0 }
    }
    @Test fun selectedPhotoPreviewRemoveAndPickerCancelNoMutation() {
        assertTrue(CallPhotos.save(app,"9876543210",photo()))
        rule.activity.setContent { LdThemeHost { androidx.compose.foundation.layout.Column {CallPhotoControl("9876543210")} } }
        await {rule.onAllNodesWithTag("call_photo_loaded").fetchSemanticsNodes().size==1}
        saveShot(rule,"G220-photo-control-night")
        rule.onNodeWithText("Background photo badlo",useUnmergedTree=true).performClick();settle()
        val started=org.robolectric.Shadows.shadowOf(rule.activity).peekNextStartedActivityForResult()
        assertNotNull(started);assertNotNull(CallPhotos.get(app,"9876543210"))
        val before=CallPhotos.get(app,"9876543210")
        org.robolectric.Shadows.shadowOf(rule.activity).receiveResult(started!!.intent, android.app.Activity.RESULT_CANCELED, null)
        settle();assertEquals(before,CallPhotos.get(app,"9876543210"))
        rule.onNodeWithText("Background photo hatao",useUnmergedTree=true).performClick()
        // IO removes the file before the Main continuation increments the UI revision.
        // Await both halves of the action, not just its intermediate filesystem state.
        await { CallPhotos.get(app,"9876543210")==null &&
            rule.onAllNodesWithTag("call_photo_preview").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("call_photo_preview").assertDoesNotExist()
    }
    @Test fun appearanceControlsPersistAndRenderDayNightLargeFont() {
        runBlocking {app.ldStore.edit {it[K.onboarded]=true;it[K.theme]="daylight";it[K.motion]=false}}
        settle()
        await { LdApp.settings.value.let { it.onboarded && it.theme=="daylight" && !it.motion } }
        rule.runOnUiThread { rule.activity.state.showOnboarding=false }
        settle()
        rule.onNodeWithTag("drawer_open",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Display",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Theme & appearance",useUnmergedTree=true).performClick();settle()
        rule.mainClock.autoAdvance=true
        rule.onNodeWithText("Glass ki gehraai",useUnmergedTree=true).performScrollTo();settle()
        saveShot(rule,"G220-appearance-day")
        rule.onNodeWithText("Soft",useUnmergedTree=true).performClick()
        await {LdApp.settings.value.glassDepth==1}
        rule.onNodeWithText("Milky",useUnmergedTree=true).performClick()
        await {LdApp.settings.value.glassFrost==2}
        assertFalse(LdApp.settings.value.motion)
        runBlocking {app.ldStore.edit {it[K.theme]="midnight";it[K.fontScale]=1.3f}}
        await {LdApp.settings.value.theme=="midnight" && LdApp.settings.value.fontScale==1.3f}
        settle()
        rule.mainClock.autoAdvance=true
        rule.onNodeWithText("3D",useUnmergedTree=true).performScrollTo();settle()
        saveShot(rule,"G220-appearance-night-large")
        rule.onNodeWithText("3D",useUnmergedTree=true).assertIsDisplayed().performClick()
        await {LdApp.settings.value.glassDepth==2}
        rule.mainClock.autoAdvance=true
        rule.onNodeWithText("Blobs, floating avatar aur button press movement",useUnmergedTree=true).performScrollTo();settle()
        rule.onNodeWithText("Animations",useUnmergedTree=true).assertIsDisplayed()
        saveShot(rule,"G220-appearance-bottom-night-large")
    }

}
