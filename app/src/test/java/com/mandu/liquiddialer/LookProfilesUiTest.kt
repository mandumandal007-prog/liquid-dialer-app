package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
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
class LookProfilesUiTest{
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(test:()->Boolean){rule.waitUntil(15000){settle();test()}}
 private fun show(night:Boolean=false,raw:String=""){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.lookProfiles]=raw;it[K.refinedHeaders]=true;it[K.quietRows]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.autoRecord]=false;it[K.hiddenFeatures]=setOf("notes")}};await{LdApp.settings.value.lookProfiles==raw&&LdApp.settings.value.theme==(if(night)"midnight"else"daylight")};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{LookProfilesPage({})}}};settle()}
 private fun clickText(t:String){rule.mainClock.autoAdvance=true;rule.onNodeWithText(t).performScrollTo();rule.mainClock.autoAdvance=false;rule.onNodeWithText(t).performClick();settle()}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(tag,true).performClick();settle()}
 @After fun clean(){runBlocking{ctx.ldStore.edit{it[K.lookProfiles]="";it[K.refinedHeaders]=false;it[K.quietRows]=false;it[K.fontScale]=1f;it[K.hiddenFeatures]=emptySet()}}}
 @Test fun previewIsReadOnlyAndApplyUndoPreserveBehavior(){show();val before=Look.capture(LdApp.settings.value);clickText("Royal Night");rule.onNodeWithTag("look_preview",true).assertExists();assertEquals(before,Look.capture(LdApp.settings.value));click("look_apply");await{LdApp.settings.value.theme=="midnight"};assertEquals(setOf("notes"),LdApp.settings.value.hiddenFeatures);assertFalse(LdApp.settings.value.autoRecord);click("look_undo");await{Look.capture(LdApp.settings.value)==before}}
 @Test fun saveRenameAndDeleteAreLibraryOnly(){show();val before=Look.capture(LdApp.settings.value);click("look_save_current");rule.onNode(hasSetTextAction()).performTextInput("My work look");click("look_save_name");await{runCatching{LookProfiles.decode(LdApp.settings.value.lookProfiles).size==1}.getOrDefault(false)};val id=LookProfiles.decode(LdApp.settings.value.lookProfiles).single().id;click("look_rename_$id");rule.onNode(hasSetTextAction()).performTextReplacement("New name");click("look_save_name");await{LookProfiles.decode(LdApp.settings.value.lookProfiles).single().name=="New name"};click("look_delete_$id");click("look_delete_confirm");await{LookProfiles.decode(LdApp.settings.value.lookProfiles).isEmpty()};assertEquals(before,Look.capture(LdApp.settings.value))}
 @Test fun staleAppearanceBlocksApply(){show();clickText("Royal Night");runBlocking{ctx.ldStore.edit{it[K.blur]=17}};await{LdApp.settings.value.blur==17};click("look_apply");rule.onNodeWithTag("look_preview_error",true).assertExists();assertEquals(17,LdApp.settings.value.blur);assertEquals("daylight",LdApp.settings.value.theme)}
 @Test fun corruptLibraryIsPreservedAndCannotSave(){show(raw="broken");rule.onNodeWithTag("look_library_error",true).assertExists();rule.onNodeWithTag("look_save_current",true).assertDoesNotExist();assertEquals("broken",LdApp.settings.value.lookProfiles)}
 @Test fun nestedPreviewFontDoesNotMultiplyParentSetting(){show(true);var actual=0f;rule.runOnUiThread{rule.activity.mountTestPage{LdThemeContent(LdSettings(fontScale=1.4f)){LdThemeContent(LdSettings(fontScale=1.2f)){actual=LocalDensity.current.fontScale;LdText("Preview",14f)}}}};settle();assertEquals(1.2f,actual,.001f)}
 @Test fun dayProfilesAndRoyalPreviewPixels(){show();saveShot(rule,"LOOK400-day");clickText("Royal Night");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("look_preview",true).performScrollTo();settle();saveShot(rule,"LOOK400-royal")}
 @Test fun nightLargeAndEasyReadPreviewPixels(){show(true);saveShot(rule,"LOOK400-night-large");clickText("Easy Read");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("look_preview",true).performScrollTo();settle();saveShot(rule,"LOOK400-easy-read")}
}
