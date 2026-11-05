package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DialSaveContactUiTest {
    // The default unconfined recomposer can resume inline on DataStore's worker thread.
    // Queue recomposition on the controlled test scheduler instead of the emitter thread.
    @get:Rule val rule=createAndroidComposeRule<MainActivity>(kotlinx.coroutines.test.StandardTestDispatcher())
    private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
    @Before fun seed(){
        runBlocking { ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit{it[K.onboarded]=true;it[K.theme]="daylight";it[K.fontScale]=1f} }
        settle()
        rule.waitUntil(15000) { LdApp.settings.value.let { it.onboarded && it.theme=="daylight" && it.fontScale==1f } }
        rule.runOnUiThread { rule.activity.state.showOnboarding=false }
        settle()
        rule.onNodeWithTag("drawer_open").assertIsDisplayed()
    }
    @Test fun explicitSaveEntryPrefillsNumberAndRequiresUserNameAndSave(){
        settle()
        rule.runOnUiThread { rule.activity.state.showOnboarding=false;rule.activity.state.dial="9876543210";rule.activity.state.tab="dialer" }
        settle()
        rule.onNodeWithTag("dial_save_contact",useUnmergedTree=true).assertExists()
        saveShot(rule,"N201-dial-save-day")
        runBlocking{ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit{it[K.theme]="midnight"}}
        settle();rule.waitUntil(15000) { LdApp.settings.value.theme=="midnight" };saveShot(rule,"N201-dial-save-night")
        rule.onNodeWithTag("dial_save_contact",useUnmergedTree=true).performClick();settle()
        val launch=org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity
        org.junit.Assert.assertEquals(android.content.Intent.ACTION_CHOOSER,launch.action)
        val target=launch.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)!!
        org.junit.Assert.assertEquals(android.content.Intent.ACTION_INSERT,target.action)
        org.junit.Assert.assertEquals(android.provider.ContactsContract.Contacts.CONTENT_TYPE,target.type)
        org.junit.Assert.assertEquals("9876543210",target.getStringExtra(android.provider.ContactsContract.Intents.Insert.PHONE))
        org.junit.Assert.assertNull(target.component)
        rule.onNodeWithText("Naya contact",useUnmergedTree=true).assertExists()
        rule.onNodeWithText("9876543210",useUnmergedTree=true).assertExists()
        rule.onNodeWithText("Save",useUnmergedTree=true).assertExists()
        saveShot(rule,"G220-editor-night")
        runBlocking{ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit{it[K.theme]="daylight";it[K.fontScale]=1.3f}}
        settle();rule.waitUntil(15000) { LdApp.settings.value.let { it.theme=="daylight" && it.fontScale==1.3f } };saveShot(rule,"G220-editor-day-large")
        // No name entered, no Save tap. Opening this editor alone must not create a contact.
        org.junit.Assert.assertTrue(rule.activity.state.contacts.isEmpty())
    }
    @Test fun largeFontKeypadSaveDoesNotHideCallControl() {
        settle();rule.runOnUiThread { rule.activity.state.showOnboarding=false }
        runBlocking{ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit{it[K.fontScale]=1.3f}}
        rule.runOnUiThread { rule.activity.state.dial="9876543210";rule.activity.state.tab="dialer" };settle()
        rule.onNodeWithTag("dial_save_contact",useUnmergedTree=true).assertIsDisplayed()
        for(tag in listOf("dial_save_contact","dial_pause","dial_meet","call_btn")) rule.onNodeWithTag(tag,useUnmergedTree=true).assertIsDisplayed()
        for((tag,text) in listOf("dial_save_contact" to "Contact save karo","dial_pause" to "+ Pause (,)","dial_meet" to "Meet call")) {
            val button=rule.onNodeWithTag(tag,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val label=rule.onNodeWithText(text,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("$tag label contained at large font",label.left>=button.left && label.right<=button.right && label.top>=button.top && label.bottom<=button.bottom)
        }
        saveShot(rule,"N201-dial-save-large")
    }
    @After fun reset()=runBlocking{ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit{it[K.fontScale]=1f};Unit}

}
