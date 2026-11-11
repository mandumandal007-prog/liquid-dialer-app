package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsFinderUiTest{
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1000);rule.waitForIdle()}
 private fun show(night:Boolean=false,onOpen:(String)->Unit={}){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.refinedHeaders]=true;it[K.autoSpeaker]=false;it[K.confirmCall]=false}};rule.waitUntil(15000){LdApp.settings.value.theme==(if(night)"midnight"else"daylight")&&LdApp.settings.value.fontScale==(if(night)1.4f else 1f)&&!LdApp.settings.value.autoSpeaker&&!LdApp.settings.value.confirmCall};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{SettingsFinderPage({},onOpen)}}};settle()}
 private fun query(q:String){rule.onNode(hasSetTextAction()).performTextReplacement(q);settle()}
 private fun openRow(id:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag("settings_result_"+id,true).performScrollTo();rule.mainClock.autoAdvance=false;rule.onNodeWithTag("settings_result_"+id,true).performClick();settle()}
 @After fun clean(){runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.refinedHeaders]=false}}}
 @Test fun searchAndOpenOnlyNavigateNoPreferenceMutation(){var opened="";show(onOpen={opened=it});val before=LdApp.settings.value;query("speaker");openRow("calls-29");assertEquals("settings:calls?find=calls-29",opened);assertEquals(before,LdApp.settings.value)}
 @Test fun emptyResultAndClear(){show();query("neverexistingxyz");rule.onNodeWithText("Is naam ka control nahi mila",true).assertExists();rule.onNodeWithTag("settings_find_clear",true).performClick();settle();rule.onNodeWithTag("settings_find_count",true).assertTextEquals("${SettingsFinder.all.size} results")}
 @Test fun highlightScrollsToDeepSwitchWithoutTurningItOn(){show();val before=LdApp.settings.value;rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Page("settings:calls?find=calls-29",rule.activity.state,rule.activity,{}, {},null,"",{})}}};settle();rule.onNodeWithTag("settings_found_row",true).assertIsDisplayed();rule.onNodeWithText("Connect pe speaker",true).assertIsDisplayed();assertEquals(before,LdApp.settings.value);saveShot(rule,"FINDER403-highlight-day")}
 @Test fun openingCarrierResultDoesNotLaunchCarrierAction(){show();val before=LdApp.settings.value;rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Page("settings:calls?find=calls-21",rule.activity.state,rule.activity,{}, {},null,"",{})}}};settle();rule.onNodeWithTag("settings_found_row",true).assertIsDisplayed();assertEquals(before,LdApp.settings.value);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun daylightSearchPixels(){show();query("call");saveShot(rule,"FINDER403-day")}
 @Test fun largeNightSearchAndHighlightPixels(){show(true);query("speaker");saveShot(rule,"FINDER403-night-large");rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Page("settings:calls?find=calls-30",rule.activity.state,rule.activity,{}, {},null,"",{})}}};settle();rule.onNodeWithTag("settings_found_row",true).assertIsDisplayed();rule.onNodeWithText("Cut pe confirm",true).assertIsDisplayed();saveShot(rule,"FINDER403-highlight-night-large")}
}
