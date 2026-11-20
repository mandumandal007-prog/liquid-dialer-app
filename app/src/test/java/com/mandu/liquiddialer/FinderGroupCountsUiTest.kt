package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class FinderGroupCountsFixture:ConverterOptionsFixture(){
 @Before fun resetCounts(){counts(false)}
 @After fun cleanCounts(){counts(false)}
 protected fun counts(on:Boolean=true){runBlocking{ctx.repo.set(K.finderGroupCounts,on)};await{LdApp.settings.value.finderGroupCounts==on};settle()}
 protected fun finder(night:Boolean=false,onOpen:(String)->Unit={}){seed(night=night);mount{SettingsFinderPage({},onOpen)}}
 protected fun query(text:String){input("settings_find_query",text)}
 protected fun verify(q:String,on:Boolean=true){val results=SettingsFinder.search(q);rule.onNodeWithTag("settings_find_count",true).assertTextEquals("${results.size} results");val groups=results.groupBy{it.group};for(g in SettingsFinder.all.map{it.group}.distinct()){val node=rule.onNodeWithTag("settings_find_group_count_"+g,true);if(on&&g in groups)node.assertTextEquals("Matching controls: ${groups.getValue(g).size}")else node.assertDoesNotExist()};results.forEach{rule.onNodeWithTag("settings_result_"+it.id,true).assertExists()}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderGroupCountsUiTest:FinderGroupCountsFixture(){
 @Test fun defaultOffAndCurrentSearchCounts(){assertFalse(LdSettings().finderGroupCounts);finder();query("speaker");verify("speaker",false);counts();await{rule.onAllNodesWithTag("settings_find_group_count_calls",true).fetchSemanticsNodes().isNotEmpty()};verify("speaker");move("settings_find_count");saveShot(rule,"FINDER525-day-on");counts(false);await{rule.onAllNodesWithTag("settings_find_group_count_calls",true).fetchSemanticsNodes().isEmpty()};verify("speaker",false);move("settings_find_count");saveShot(rule,"FINDER525-day-off")}
 @Test fun allQueriesCountsEqualCurrentMatches(){finder();counts();for(q in listOf("","call","speaker","date","neverexistingxyz")){query(q);verify(q)};rule.onNodeWithText("Is naam ka control nahi mila",true).assertExists();hit("settings_find_clear");verify("")}
 @Test fun layoutKeepsQueryResultOrderAndRoutes(){val opened=mutableListOf<String>();finder(onOpen={opened+=it});query("speaker");val results=SettingsFinder.search("speaker");val before=results.map{rule.onNodeWithTag("settings_result_"+it.id,true).getUnclippedBoundsInRoot().top.value};assertEquals(before.sorted(),before);counts();verify("speaker");val after=results.map{rule.onNodeWithTag("settings_result_"+it.id,true).getUnclippedBoundsInRoot().top.value};assertEquals(after.sorted(),after);rule.onNode(hasSetTextAction(),true).assertTextEquals("speaker");val prefs=LdApp.settings.value;results.forEach{hit("settings_result_"+it.id);assertEquals(SettingsFinder.route(it),opened.last())};assertEquals(prefs,LdApp.settings.value);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun toggleChangesOnlyOwnPreferenceAndFinderSelfRoute(){finder();val before=LdApp.settings.value;hit("finder_group_counts_switch");await{LdApp.settings.value.finderGroupCounts};assertEquals(before.copy(finderGroupCounts=true),LdApp.settings.value);move("finder_group_counts_switch");saveShot(rule,"FINDER525-settings-on");hit("finder_group_counts_switch");await{!LdApp.settings.value.finderGroupCounts};assertEquals(before,LdApp.settings.value);assertEquals("settingsfinder",SettingsFinder.all.single{it.id=="finder-group-counts"}.route)}
 @Test fun noMatchesNoPhantomGroupsOrActions(){finder();counts();query("not_a_control_525");verify("not_a_control_525");rule.onNodeWithText("Is naam ka control nahi mila",true).assertExists();assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderGroupCountsNightUiTest:FinderGroupCountsFixture(){
 @Test fun nightLargeCountAndLastResultReachable(){finder(night=true);counts();query("speaker");verify("speaker");move("settings_find_count");saveShot(rule,"FINDER525-night-large");val last=SettingsFinder.search("speaker").last();move("settings_result_"+last.id);rule.onNodeWithTag("settings_result_"+last.id,true).assertIsDisplayed();saveShot(rule,"FINDER525-night-last")}
}
