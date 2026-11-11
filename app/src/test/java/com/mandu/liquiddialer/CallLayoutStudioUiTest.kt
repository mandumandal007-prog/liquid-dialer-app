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
class CallLayoutStudioUiTest{
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(900);rule.waitForIdle()}
 private fun await(t:()->Boolean){rule.waitUntil(15000){settle();t()}}
 private fun show(night:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.layoutGrid]=DEFAULT_GRID;it[K.layoutBottom]=DEFAULT_BOTTOM;it[K.gridScale]=1f;it[K.bottomScale]=1f;it[K.autoRecord]=false;it[K.refinedHeaders]=true}};await{LdApp.settings.value.let{it.onboarded&&it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&it.layoutGrid==DEFAULT_GRID&&it.layoutBottom==DEFAULT_BOTTOM&&it.gridScale==1f&&it.bottomScale==1f&&!it.autoRecord}};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallLayoutStudioPage({})}}};settle()}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(tag,true).performClick();settle()}
 private fun choose(t:String){rule.mainClock.autoAdvance=true;rule.onNodeWithText(t,true).performScrollTo();rule.mainClock.autoAdvance=false;rule.onNodeWithText(t,true).performClick();settle()}
 @After fun clean(){runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.refinedHeaders]=false;it[K.layoutGrid]=DEFAULT_GRID;it[K.layoutBottom]=DEFAULT_BOTTOM;it[K.gridScale]=1f;it[K.bottomScale]=1f}}}
 @Test fun presetAndDraftEditsAreReadOnlyUntilApply(){show();val before=LdApp.settings.value;choose("Essentials");click("studio_up_keypad");click("studio_toggle_record");assertEquals(before,LdApp.settings.value);rule.onNodeWithTag("studio_toggle_end",true).assertDoesNotExist();click("layout_studio_apply");await{LdApp.settings.value.layoutGrid!=DEFAULT_GRID};assertFalse(LdApp.settings.value.autoRecord);assertEquals(before.callCardStyle,LdApp.settings.value.callCardStyle)}
 @Test fun applyAndUndoKeepBehaviorAndIcons(){show();val before=LdApp.settings.value;choose("Conference");click("layout_studio_apply");await{CallLayoutDraft.capture(LdApp.settings.value)==CallLayoutDraft.presets()[2].second};click("layout_studio_undo");await{CallLayoutDraft.capture(LdApp.settings.value)==CallLayoutDraft.capture(before)};assertFalse(LdApp.settings.value.autoRecord);assertEquals(before.iconStyle,LdApp.settings.value.iconStyle)}
 @Test fun discardAndReloadDoNotWrite(){show();val before=LdApp.settings.value;choose("Essentials");click("layout_studio_discard");click("layout_studio_apply");await{rule.onAllNodesWithTag("layout_studio_status",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(before,LdApp.settings.value);click("layout_studio_reload");assertEquals(before,LdApp.settings.value)}
 @Test fun staleDraftCannotOverwriteChangedLayout(){show();choose("Essentials");runBlocking{ctx.ldStore.edit{it[K.gridScale]=1.3f}};await{LdApp.settings.value.gridScale==1.3f};click("layout_studio_apply");await{rule.onAllNodesWithTag("layout_studio_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(1.3f,LdApp.settings.value.gridScale);assertEquals(DEFAULT_GRID,LdApp.settings.value.layoutGrid);click("layout_studio_reload");rule.onNodeWithTag("layout_studio_error",true).assertDoesNotExist()}
 @Test fun dayStudioAndEssentialsPixels(){show();saveShot(rule,"STUDIO405-day");choose("Essentials");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("layout_studio_preview",true).performScrollTo();settle();saveShot(rule,"STUDIO405-essentials")}
 @Test fun nightLargeEditorPixels(){show(true);saveShot(rule,"STUDIO405-night-large");click("studio_up_keypad");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("studio_row_keypad",true).performScrollTo();settle();saveShot(rule,"STUDIO405-editor-night-large")}
}
