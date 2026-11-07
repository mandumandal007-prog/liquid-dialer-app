package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisitingCardUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle(){rule.mainClock.autoAdvance=true;rule.mainClock.advanceTimeBy(1500);rule.waitForIdle()}
    private fun show(theme:String,large:Boolean,seed:Boolean){
        runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=if(large)1.3f else 1f}}
        VisitingCard.delete(ctx)
        if(seed)VisitingCard.save(ctx,VisitingCard("SAMPLE STUDIO","DESIGN · PRINT · CREATE","Asha Example","Creative designer","+91 90000 00000","hello@example.com","www.example.com","Sample Road, Kolkata"))
        settle()
        rule.runOnUiThread{rule.activity.setContent{LdThemeHost{VisitingCardPage({})}}};settle()
    }
    @Test fun blankEditorExportDisabledAndExplicitDraft(){
        show("daylight",false,false)
        saveShot(rule,"card-editor-blank-day");rule.onNodeWithTag("card_preview",useUnmergedTree=true).assertIsDisplayed()
        rule.mainClock.autoAdvance=true
        rule.onNodeWithTag("card_export").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("card_field_0").performScrollTo().performTextInput("My studio");settle()
        assertEquals("",VisitingCard.load(ctx).business)
        rule.onNodeWithTag("card_save").performScrollTo().performClick();settle()
        assertEquals("My studio",VisitingCard.load(ctx).business)
        rule.onNodeWithTag("card_export").assertIsEnabled()
        rule.onNodeWithTag("card_clear").performScrollTo().performClick();settle()
        rule.onNodeWithTag("card_clear_confirm").performClick();settle()
        assertFalse(VisitingCard.load(ctx).hasContent)
    }
    @Test fun filledNightLargePreviewAndActions(){
        show("midnight",true,true)
        saveShot(rule,"card-editor-night-large");rule.onNodeWithTag("card_preview",useUnmergedTree=true).assertIsDisplayed()
        rule.mainClock.autoAdvance=true
        rule.onNodeWithTag("card_export").performScrollTo().assertIsEnabled();rule.onNodeWithTag("card_clear").performScrollTo();saveShot(rule,"card-editor-actions-night-large")
        rule.onNodeWithTag("card_field_7").performScrollTo().assertExists()
    }
    @Test fun overflowPreservesDraftBlocksExportAndShowsWarning(){
        show("daylight",false,false)
        val long="A long business name that must remain fully preserved even when it is longer than the PNG field limit"
        rule.onNodeWithTag("card_field_0").performTextInput(long);settle()
        saveShot(rule,"card-editor-overflow-warning")
        rule.onNodeWithTag("card_export").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("card_save").performScrollTo().performClick();settle()
        assertEquals(long,VisitingCard.load(ctx).business)
        rule.onNodeWithTag("card_field_0").performScrollTo().performTextReplacement("Short name");settle()
        rule.onNodeWithTag("card_export").performScrollTo().assertIsEnabled()
    }
    @Test fun savePickerSnapshotPngCancelAndFailure(){
        show("daylight",false,true)
        fun clickExport(){
            rule.waitUntil(15000){rule.onAllNodesWithTag("card_export").fetchSemanticsNodes().singleOrNull()?.config?.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)==false}
            rule.onNodeWithTag("card_export").performScrollTo().assertIsEnabled().performClick();settle()
        }
        fun deliver(uri:android.net.Uri,result:Int=android.app.Activity.RESULT_OK){
            val shadow=org.robolectric.Shadows.shadowOf(rule.activity)
            val started=shadow.nextStartedActivityForResult
            assertNotNull("Explicit export opens Android save picker",started)
            assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT,started.intent.action)
            assertEquals("image/png",started.intent.type)
            rule.runOnUiThread{shadow.receiveResult(started.intent,result,android.content.Intent().setData(uri))};settle()
        }
        val file=java.io.File(ctx.cacheDir,"card-test.png");file.delete()
        clickExport();deliver(android.net.Uri.fromFile(file))
        rule.waitUntil(15000){file.exists() && file.length()>1000}
        rule.waitUntil(15000){rule.onAllNodesWithText("PNG save ho gaya. Kisi ko bheja nahi gaya.",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()};settle()
        val bmp=android.graphics.BitmapFactory.decodeFile(file.path);assertEquals(1200,bmp.width);assertEquals(720,bmp.height);bmp.recycle()
        clickExport();val cancel=java.io.File(ctx.cacheDir,"card-cancel.png");cancel.delete();deliver(android.net.Uri.fromFile(cancel),android.app.Activity.RESULT_CANCELED);assertFalse(cancel.exists())
        clickExport();deliver(android.net.Uri.parse("content://not.available/card.png"));settle()
        rule.waitUntil(15000){rule.onAllNodesWithText("PNG save nahi hua. Adhuri destination file hata kar dobara try karo.",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("card_notice",useUnmergedTree=true).performScrollTo();saveShot(rule,"card-editor-export-error")
    }

}
