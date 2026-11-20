package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class FinderGroupFilterFixture:FinderFullLabelsFixture(){
 @Before fun resetFilter(){filter(false)}
 @After fun cleanFilter(){filter(false)}
 protected fun filter(on:Boolean=true){runBlocking{ctx.repo.set(K.finderGroupFilter,on)};await{LdApp.settings.value.finderGroupFilter==on};settle()}
 protected fun choose(id:String){move("settings_find_groups");repeat(35){val node=rule.onNodeWithTag("settings_find_group_"+id,true);val b=node.getUnclippedBoundsInRoot();val row=rule.onNodeWithTag("settings_find_groups",true);val v=row.getUnclippedBoundsInRoot();if(b.left>=v.left&&b.right<=v.right){node.assertIsDisplayed();node.performSemanticsAction(SemanticsActions.OnClick){it()};settle();node.assertIsSelected();return};val dx=if(b.left<v.left)(b.left-v.left).value else(b.right-v.right).value;row.performSemanticsAction(SemanticsActions.ScrollBy){it(dx*rule.density.density,0f)};settle()};error("Group not visible $id")}
 protected fun count(q:String,g:String){rule.onNodeWithTag("settings_find_count",true).assertTextEquals("${SettingsFinder.searchGroup(q,g).size} results")}
 protected fun pixels(night:Boolean){finder(night);query("speaker");move("settings_find_count");saveShot(rule,"FILTER536-${if(night)"night"else"day"}-off");filter();choose("calls");move("settings_find_count");saveShot(rule,"FILTER536-${if(night)"night"else"day"}-on");choose("notifications");rule.onNodeWithTag("settings_find_group_notifications",true).assertIsDisplayed();val out=mutableListOf<TextLayoutResult>();rule.onNode(hasText("✓ Notifications"),true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(out)};val l=out.single();assertEquals(l.layoutInput.text.length,l.getLineEnd(l.lineCount-1));assertFalse(l.isLineEllipsized(0));assertTrue(l.getLineRight(0)<=l.size.width+1);saveShot(rule,"FILTER536-${if(night)"night"else"day"}-swiped");filter(false);rule.onNodeWithTag("settings_find_groups",true).assertDoesNotExist();count("speaker","");rule.onNode(hasSetTextAction(),true).assertTextEquals("speaker")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderGroupFilterUiTest:FinderGroupFilterFixture(){
 @Test fun defaultOffSwitchOnlyAndIndex(){assertFalse(LdSettings().finderGroupFilter);finder();val before=LdApp.settings.value;hit("finder_group_filter_switch");await{LdApp.settings.value.finderGroupFilter};assertEquals(before.copy(finderGroupFilter=true),LdApp.settings.value);move("finder_group_filter_switch");saveShot(rule,"FILTER536-settings-on");hit("finder_group_filter_switch");await{!LdApp.settings.value.finderGroupFilter};assertEquals(before,LdApp.settings.value);assertEquals("settingsfinder",SettingsFinder.all.single{it.id=="finder-group-filter"}.route)}
 @Test fun intersectKeepQueryAndExactRoutes(){val opened=mutableListOf<String>();finder(onOpen={opened+=it});filter();query("speaker");choose("calls");count("speaker","calls");val expected=SettingsFinder.searchGroup("speaker","calls");assertTrue(expected.isNotEmpty());val tops=expected.map{rule.onNodeWithTag("settings_result_"+it.id,true).getUnclippedBoundsInRoot().top.value};assertEquals(tops.sorted(),tops);val prefs=LdApp.settings.value;expected.forEach{hit("settings_result_"+it.id);assertEquals(SettingsFinder.route(it),opened.last())};SettingsFinder.search("speaker").filter{it.group!="calls"}.forEach{rule.onNodeWithTag("settings_result_"+it.id,true).assertDoesNotExist()};assertEquals(prefs,LdApp.settings.value);rule.onNode(hasSetTextAction(),true).assertTextEquals("speaker");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun emptyResetAndClearAreSeparate(){finder();filter();query("speaker");choose("backup");count("speaker","backup");rule.onNodeWithText("Is naam ka control nahi mila",true).assertExists();hit("settings_find_group_reset");count("speaker","");choose("calls");hit("settings_find_clear");count("","calls");rule.onNodeWithTag("settings_find_active_group",true).assertTextEquals("Group: Calls");choose("all");count("","")}
 @Test fun offKeepsQueryResetsGroupAndNoSideEffects(){finder();query("speaker");filter();choose("calls");filter(false);count("speaker","");rule.onNodeWithTag("settings_find_groups",true).assertDoesNotExist();filter();count("speaker","");rule.onNodeWithTag("settings_find_active_group",true).assertTextEquals("Group: All groups");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun dayPixels(){pixels(false)}
 @Test fun countsAndFullLabelsCombine(){finder();labels();runBlocking{ctx.repo.set(K.finderGroupCounts,true)};await{LdApp.settings.value.finderGroupCounts};filter();query("speaker");choose("calls");SettingsFinder.searchGroup("speaker","calls").forEach{full(it.id);full(it.id,true)};rule.onNodeWithTag("settings_find_group_count_calls",true).assertTextEquals("Matching controls: ${SettingsFinder.searchGroup("speaker","calls").size}");rule.onNodeWithTag("settings_find_group_count_recording",true).assertDoesNotExist()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderGroupFilterNightUiTest:FinderGroupFilterFixture(){
 @Test fun nightLargePixels(){pixels(true)}
 @Test fun lastCategoryAndLastResultReachable(){finder(true);filter();query("");choose("about");count("","about");choose("backup");count("","backup");val last=SettingsFinder.searchGroup("","backup").last();hit("settings_result_"+last.id);choose("all");count("","")}
}
class FinderGroupFilterTest{
 @Test fun everyGroupIsStableQueryIntersection(){for(q in listOf("","speaker","call","privacy","no_control_536")){assertEquals(SettingsFinder.search(q),SettingsFinder.searchGroup(q,""));SettingsFinder.all.map{it.group}.distinct().forEach{g->assertEquals(SettingsFinder.search(q).filter{it.group==g},SettingsFinder.searchGroup(q,g))}}}
 @Test fun indexedOnceAndNoUnknownGroupFallback(){assertEquals(1,SettingsFinder.all.count{it.id=="finder-group-filter"});assertTrue(SettingsFinder.searchGroup("","not-a-group").isEmpty())}
}
