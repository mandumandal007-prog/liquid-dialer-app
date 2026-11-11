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
class QuietWeekUiTest{
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(t:()->Boolean){rule.waitUntil(15000){settle();t()}}
 private val sample get()=QuietWeek.encode(listOf(QuietWeek.Window("work","Work night",setOf(0,1,2,3,4),1320,420)))
 private fun show(night:Boolean=false,raw:String=""){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.quietHours]=false;it[K.quietWeekMode]=false;it[K.quietWeekRules]=raw;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.refinedHeaders]=true}};await{LdApp.settings.value.let{it.quietWeekRules==raw&&!it.quietWeekMode&&!it.quietHours&&it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)}};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{QuietWeekPage(rule.activity,{}, {})}}};settle()}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(tag,true).performClick();settle()}
 private fun text(t:String){rule.mainClock.autoAdvance=true;rule.onNodeWithText(t,true).performScrollTo();rule.mainClock.autoAdvance=false;rule.onNodeWithText(t,true).performClick();settle()}
 private fun field(i:Int,value:String){rule.onAllNodes(hasSetTextAction())[i].performTextReplacement(value);settle()}
 @After fun clean(){runBlocking{ctx.ldStore.edit{it[K.quietWeekMode]=false;it[K.quietWeekRules]="";it[K.fontScale]=1f;it[K.refinedHeaders]=false}}}
 @Test fun addIsDraftAndSaveDoesNotTurnMasterOn(){show();click("quiet_week_add");field(0,"Sleep");click("quiet_week_keep");assertEquals("",LdApp.settings.value.quietWeekRules);text("Weekly mode (draft)");click("quiet_week_save");await{LdApp.settings.value.quietWeekMode};assertFalse(LdApp.settings.value.quietHours);assertEquals("Sleep",QuietWeek.decode(LdApp.settings.value.quietWeekRules).single().name)}
 @Test fun invalidTimeCannotEnterDraft(){show();click("quiet_week_add");field(0,"Sleep");field(1,"24:00");click("quiet_week_keep");rule.onNodeWithTag("quiet_week_error",true).assertExists();assertEquals("",LdApp.settings.value.quietWeekRules)}
 @Test fun removingAndReloadingDoNotWrite(){show(raw=sample);click("quiet_remove_work");assertEquals(sample,LdApp.settings.value.quietWeekRules);click("quiet_week_reload");rule.onNodeWithTag("quiet_window_work",true).assertExists()}
 @Test fun staleScheduleCannotBeOverwritten(){show(raw=sample);runBlocking{ctx.ldStore.edit{it[K.quietWeekRules]=""}};await{LdApp.settings.value.quietWeekRules.isEmpty()};click("quiet_week_save");await{rule.onAllNodesWithTag("quiet_week_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals("",LdApp.settings.value.quietWeekRules)}
 @Test fun corruptDataPreservedAndModeCanBeDisabled(){show(raw="broken");rule.onNodeWithTag("quiet_week_corrupt",true).assertExists();rule.onNodeWithTag("quiet_week_save",true).assertDoesNotExist();click("quiet_week_disable");await{rule.onAllNodesWithTag("quiet_week_status",true).fetchSemanticsNodes().isNotEmpty()};assertEquals("broken",LdApp.settings.value.quietWeekRules);assertFalse(LdApp.settings.value.quietWeekMode)}
 @Test fun dayPlannerPixels(){show(raw=sample);saveShot(rule,"QUIET406-day");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("quiet_week_save",true).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false;settle();saveShot(rule,"QUIET406-week-preview")}
 @Test fun nightLargeEditorPixels(){show(true, sample);saveShot(rule,"QUIET406-night-large");click("quiet_edit_work");saveShot(rule,"QUIET406-editor-night-large")}
}
