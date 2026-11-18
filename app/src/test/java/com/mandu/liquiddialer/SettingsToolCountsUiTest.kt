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
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsToolCountsUiTest:OrganizedSettingsFixture(){
 @Before fun reset(){runBlocking{ctx.ldStore.edit{it[K.settingsToolCounts]=false;it[K.compactSettingsCategories]=false;it[K.settingsToolSearch]=false;it[K.settingsCategoryCounts]=false}};await{LdApp.settings.value.let{!it.settingsToolCounts&&!it.compactSettingsCategories&&!it.settingsToolSearch&&!it.settingsCategoryCounts}}}
 private val sections get()=ORGANIZED_DESTINATIONS.filter{it.route.startsWith("settingstools:")}
 private fun enable(){runBlocking{ctx.ldStore.edit{it[K.settingsToolCounts]=true}};await{LdApp.settings.value.settingsToolCounts};settle()}
 private fun detail(d:SettingsDestination,compact:Boolean=false)="${ORGANIZED_TOOLS.count{it.section==d.id}} tools"+(if(compact)""else "\n"+d.detail)
 @Test fun defaultOffOriginalDescriptions(){assertFalse(LdSettings().settingsToolCounts);start();show("settingshub:tools");for(d in sections){rule.onNodeWithText(d.detail,false,true).assertExists();rule.onNodeWithText(detail(d),false,true).assertDoesNotExist()}}
 @Test fun countsActualCompleteCatalogNotPages(){start();enable();show("settingshub:tools");assertEquals(3,sections.size);assertEquals(37,sections.sumOf{d->ORGANIZED_TOOLS.count{it.section==d.id}});for(d in sections)rule.onNodeWithText(detail(d),false,true).assertExists();rule.onNodeWithText(ORGANIZED_DESTINATIONS.first{it.id=="featurecontrols"}.detail,false,true).assertExists()}
 @Test fun onePreferenceReversible(){start();show("settingshub:tools");val before=LdApp.settings.value;tapTag("settings_tool_counts_switch");await{LdApp.settings.value.settingsToolCounts};assertEquals(before.copy(settingsToolCounts=true),LdApp.settings.value);tapTag("settings_tool_counts_switch");await{!LdApp.settings.value.settingsToolCounts};assertEquals(before,LdApp.settings.value);for(d in sections)rule.onNodeWithText(d.detail,false,true).assertExists()}
 @Test fun allSectionRoutesAndAllToolsSameNoCall(){start();enable();val before=LdApp.settings.value;for(d in sections){show("settingshub:tools");tapTag("settings_destination_"+d.id);assertEquals(d.route,opened.last());for(t in ORGANIZED_TOOLS.filter{it.section==d.id}){tapTag("settings_tool_"+t.route);assertEquals(t.route,opened.last());show(d.route)}};assertEquals(before,LdApp.settings.value);assertNull(rule.activity.state.pendingCall)}
 @Test fun compactCategoryKeepsOnlyCountAndTitles(){start();enable();runBlocking{ctx.ldStore.edit{it[K.compactSettingsCategories]=true}};await{LdApp.settings.value.compactSettingsCategories};show("settingshub:tools");for(d in sections){rule.onNodeWithText(detail(d,true),false,true).assertExists();rule.onNodeWithText(d.detail,false,true).assertDoesNotExist();rule.onNodeWithText(d.title,false,true).assertExists()};shot("SET511-day-compact")}
 @Test fun categoryPageCountsRemainSeparate(){start();enable();for(c in ORGANIZED_CATEGORIES)rule.onNodeWithTag("settings_category_count_"+c.id,true).assertDoesNotExist();runBlocking{ctx.ldStore.edit{it[K.settingsCategoryCounts]=true}};await{LdApp.settings.value.settingsCategoryCounts};for(c in ORGANIZED_CATEGORIES)rule.onNodeWithTag("settings_category_count_"+c.id,true).assertTextEquals("${ORGANIZED_DESTINATIONS.count{it.category==c.id}} pages");show("settingshub:tools");for(d in sections)rule.onNodeWithText(detail(d),false,true).assertExists()}
 @Test fun searchAndOtherCategoriesFinderUnchanged(){start();enable();show("settingshub:look");for(d in ORGANIZED_DESTINATIONS.filter{it.category=="look"})rule.onNodeWithText(d.detail,false,true).assertExists();show("settingshub:tools");runBlocking{ctx.ldStore.edit{it[K.settingsToolSearch]=true}};await{LdApp.settings.value.settingsToolSearch};rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings_tool_query")),true).performTextReplacement("IVR");settle();rule.onNodeWithText(ORGANIZED_TOOLS.first{it.route=="ivrmenus"}.detail,false,true).assertExists();assertTrue(SettingsFinder.search("Call-tool section counts").any{it.id=="settings-tool-counts"&&it.route=="settingshub:tools"})}
 @Test fun dayAndOffPixels(){start();enable();show("settingshub:tools");shot("SET511-day");tapTag("settings_tool_counts_switch");await{!LdApp.settings.value.settingsToolCounts};rule.mainClock.autoAdvance=true;rule.onNodeWithTag("settings_tool_counts_switch",true).performScrollTo();settle();shot("SET511-off")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsToolCountsNightUiTest:OrganizedSettingsFixture(){
 @Test fun nightLargeCountsAndCompact(){start(night=true);runBlocking{ctx.ldStore.edit{it[K.settingsToolCounts]=true;it[K.compactSettingsCategories]=false}};await{LdApp.settings.value.settingsToolCounts&&!LdApp.settings.value.compactSettingsCategories};show("settingshub:tools");shot("SET511-night");runBlocking{ctx.ldStore.edit{it[K.compactSettingsCategories]=true}};await{LdApp.settings.value.compactSettingsCategories};show("settingshub:tools");shot("SET511-night-compact");for(d in ORGANIZED_DESTINATIONS.filter{it.route.startsWith("settingstools:")}){rule.mainClock.autoAdvance=true;rule.onNodeWithTag("settings_destination_"+d.id,true).performScrollTo();settle();rule.onNodeWithText(d.title,false,true).assertIsDisplayed();rule.onNodeWithText("${ORGANIZED_TOOLS.count{it.section==d.id}} tools",false,true).assertIsDisplayed()};assertNull(rule.activity.state.pendingCall)}
}
