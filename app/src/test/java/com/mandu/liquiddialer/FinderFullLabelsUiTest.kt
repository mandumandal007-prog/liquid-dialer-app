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
abstract class FinderFullLabelsFixture:ConverterOptionsFixture(){
 @Before fun resetFullLabels(){labels(false);runBlocking{ctx.repo.set(K.finderGroupCounts,false)}}
 @After fun cleanFullLabels(){labels(false);runBlocking{ctx.repo.set(K.finderGroupCounts,false)}}
 protected fun labels(on:Boolean=true){runBlocking{ctx.repo.set(K.finderFullLabels,on)};await{LdApp.settings.value.finderFullLabels==on};settle()}
 protected fun finder(night:Boolean=false,onOpen:(String)->Unit={}){seed(night=night);mount{SettingsFinderPage({},onOpen)}}
 protected fun query(q:String){input("settings_find_query",q)}
 protected fun layout(id:String,title:Boolean=false):TextLayoutResult{val e=SettingsFinder.all.single{it.id==id};val out=mutableListOf<TextLayoutResult>();rule.onNode(hasText(if(title)e.title else e.detail)and hasAnyAncestor(hasTestTag("settings_result_$id")),true).performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(out)};return out.single()}
 protected fun full(id:String,title:Boolean=false){val l=layout(id,title);assertEquals(l.layoutInput.text.length,l.getLineEnd(l.lineCount-1));for(i in 0 until l.lineCount){assertFalse(l.isLineEllipsized(i));assertTrue(l.getLineRight(i)<=l.size.width+1f);assertTrue(l.getLineBottom(i)<=l.size.height+1f)}}
 protected fun fullAll(){SettingsFinder.search("speaker").forEach{full(it.id,true);full(it.id)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderFullLabelsUiTest:FinderFullLabelsFixture(){
 @Test fun defaultOffAndOwnSwitchOnly(){assertFalse(LdSettings().finderFullLabels);finder();val before=LdApp.settings.value;hit("finder_full_labels_switch");await{LdApp.settings.value.finderFullLabels};assertEquals(before.copy(finderFullLabels=true),LdApp.settings.value);move("finder_full_labels_switch");saveShot(rule,"FINDER526-settings-on");hit("finder_full_labels_switch");await{!LdApp.settings.value.finderFullLabels};assertEquals(before,LdApp.settings.value);assertEquals("settingsfinder",SettingsFinder.all.single{it.id=="finder-full-labels"}.route)}
 @Test fun dayOnOffFullTextAndOriginalLimits(){finder();query("speaker");labels();fullAll();move("settings_result_recording-58");saveShot(rule,"FINDER526-day-on");labels(false);SettingsFinder.search("speaker").forEach{assertEquals(2,layout(it.id,true).layoutInput.maxLines);assertEquals(3,layout(it.id).layoutInput.maxLines)};move("settings_result_recording-58");saveShot(rule,"FINDER526-day-off")}
 @Test fun sameQueryOrderRoutesAndNoOtherEffects(){val opened=mutableListOf<String>();finder(onOpen={opened+=it});query("speaker");val results=SettingsFinder.search("speaker");val before=results.map{rule.onNodeWithTag("settings_result_"+it.id,true).getUnclippedBoundsInRoot().top.value};assertEquals(before.sorted(),before);labels();val after=results.map{rule.onNodeWithTag("settings_result_"+it.id,true).getUnclippedBoundsInRoot().top.value};assertEquals(after.sorted(),after);rule.onNode(hasSetTextAction(),true).assertTextEquals("speaker");val prefs=LdApp.settings.value;results.forEach{hit("settings_result_"+it.id);assertEquals(SettingsFinder.route(it),opened.last())};assertEquals(prefs,LdApp.settings.value);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun noMatchesAndClearStillWork(){finder();labels();query("not_a_control_526");rule.onNodeWithTag("settings_find_count",true).assertTextEquals("0 results");rule.onNodeWithText("Is naam ka control nahi mila",true).assertExists();hit("settings_find_clear");rule.onNodeWithTag("settings_find_count",true).assertTextEquals("${SettingsFinder.all.size} results");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun countsIndependentAndBothTogglesKeepSearch(){finder();query("speaker");labels();runBlocking{ctx.repo.set(K.finderGroupCounts,true)};await{LdApp.settings.value.finderGroupCounts};SettingsFinder.search("speaker").groupBy{it.group}.forEach{(g,items)->rule.onNodeWithTag("settings_find_group_count_"+g,true).assertTextEquals("Matching controls: ${items.size}")};labels(false);assertTrue(LdApp.settings.value.finderGroupCounts);rule.onNode(hasSetTextAction(),true).assertTextEquals("speaker")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderFullLabelsNightUiTest:FinderFullLabelsFixture(){
 @Test fun nightLargeFullTextReversibleAndLastReachable(){finder(night=true);query("speaker");val last=SettingsFinder.search("speaker").last();val old=layout(last.id);assertEquals(3,old.layoutInput.maxLines);labels();fullAll();move("settings_result_"+last.id);rule.onNodeWithTag("settings_result_"+last.id,true).assertIsDisplayed();saveShot(rule,"FINDER526-night-on");labels(false);assertEquals(3,layout(last.id).layoutInput.maxLines);move("settings_result_"+last.id);saveShot(rule,"FINDER526-night-off")}
}
