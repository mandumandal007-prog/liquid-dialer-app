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
class CompactSettingsToolsUiTest:OrganizedSettingsFixture(){
 @Before fun reset(){runBlocking{ctx.ldStore.edit{it[K.compactSettingsTools]=false;it[K.compactSettingsCategories]=false;it[K.settingsToolSearch]=false}};await{LdApp.settings.value.let{!it.compactSettingsTools&&!it.compactSettingsCategories&&!it.settingsToolSearch}}}
 private fun enable(){runBlocking{ctx.ldStore.edit{it[K.compactSettingsTools]=true}};await{LdApp.settings.value.compactSettingsTools};settle()}
 @Test fun defaultOffKeepsAllDescriptions(){assertFalse(LdSettings().compactSettingsTools);start();for(section in listOf("live","notes","calculators")){show("settingstools:"+section);for(t in ORGANIZED_TOOLS.filter{it.section==section})rule.onNodeWithText(t.detail,false,true).assertExists()}}
 @Test fun onHidesOnlyToolRowDescriptions(){start();enable();for(section in listOf("live","notes","calculators")){show("settingstools:"+section);for(t in ORGANIZED_TOOLS.filter{it.section==section}){rule.onNodeWithText(t.title,false,true).assertExists();rule.onNodeWithText(t.detail,false,true).assertDoesNotExist()};text("Open a tool to adjust its own controls. Nothing starts from this list.").assertExists()}}
 @Test fun allThirtySevenRoutesNoSideEffect(){start();enable();val before=LdApp.settings.value;for(t in ORGANIZED_TOOLS){show("settingstools:"+t.section);tapTag("settings_tool_"+t.route);assertEquals(t.route,opened.last())};assertEquals(37,ORGANIZED_TOOLS.size);assertEquals(before,LdApp.settings.value);assertNull(rule.activity.state.pendingCall)}
 @Test fun switchReversibleOnlyOnePreference(){start();show("settingshub:tools");val before=LdApp.settings.value;tapTag("compact_settings_tools_switch");await{LdApp.settings.value.compactSettingsTools};assertEquals(before.copy(compactSettingsTools=true),LdApp.settings.value);show("settingstools:live");rule.onNodeWithText(ORGANIZED_TOOLS.first{it.section=="live"}.detail,false,true).assertDoesNotExist();show("settingshub:tools");tapTag("compact_settings_tools_switch");await{!LdApp.settings.value.compactSettingsTools};assertEquals(before,LdApp.settings.value);show("settingstools:live");rule.onNodeWithText(ORGANIZED_TOOLS.first{it.section=="live"}.detail,false,true).assertExists()}
 @Test fun searchRetainsDescriptionsAndCategoryLayoutIndependent(){start();enable();text(ORGANIZED_CATEGORIES.first().detail).assertExists();show("settingshub:tools");text(ORGANIZED_DESTINATIONS.first{it.category=="tools"}.detail).assertExists();runBlocking{ctx.ldStore.edit{it[K.settingsToolSearch]=true}};await{LdApp.settings.value.settingsToolSearch};rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings_tool_query")),true).performTextReplacement("IVR");settle();rule.onNodeWithText(ORGANIZED_TOOLS.first{it.route=="ivrmenus"}.detail,false,true).assertExists()}
 @Test fun finderAndClassicAndIndividualControlsRemain(){start();enable();assertTrue(SettingsFinder.search("Compact call-tool rows").any{it.id=="compact-settings-tools"&&it.route=="settingshub:tools"});show("settingshub:classic");text(SETTINGS_GROUPS.first().summary).assertExists();show("settings:recents");text("Home pe favourite contacts dikhao/chhupao").assertExists()}
 @Test fun rowHeightReducedAndTouchTargetKept(){start();show("settingstools:live");val tags=ORGANIZED_TOOLS.filter{it.section=="live"}.map{"settings_tool_"+it.route};val heights={tags.map{rule.onNodeWithTag(it,true).fetchSemanticsNode().boundsInRoot.height}};val before=heights();enable();val after=heights();assertTrue("ON total $after must be shorter than OFF $before",after.sum()<before.sum());assertTrue("Every row keeps48dp touch height: $after",after.all{it>=96f})}
 @Test fun dayCompactAndOffPixels(){start();enable();show("settingstools:live");shot("SET509-day-live");show("settingstools:notes");shot("SET509-day-notes");show("settingshub:tools");tapTag("compact_settings_tools_switch");await{!LdApp.settings.value.compactSettingsTools};rule.mainClock.autoAdvance=true;rule.onNodeWithTag("compact_settings_tools_switch",true).performScrollTo();settle();shot("SET509-off")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompactSettingsToolsNightUiTest:OrganizedSettingsFixture(){
 @Test fun nightLargeCompactLists(){start(night=true);runBlocking{ctx.ldStore.edit{it[K.compactSettingsTools]=true}};await{LdApp.settings.value.compactSettingsTools};show("settingstools:live");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("settings_tool_incomingstudio",true).performScrollTo();settle();shot("SET509-night-live");show("settingstools:calculators");shot("SET509-night-calculators");for(t in ORGANIZED_TOOLS.filter{it.section=="calculators"})rule.onNodeWithText(t.title,false,true).assertExists();assertNull(rule.activity.state.pendingCall)}
}
