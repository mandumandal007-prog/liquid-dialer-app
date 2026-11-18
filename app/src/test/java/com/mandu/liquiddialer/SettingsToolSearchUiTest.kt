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
class SettingsToolSearchUiTest:OrganizedSettingsFixture(){
 @Before fun reset(){runBlocking{ctx.ldStore.edit{it[K.settingsToolSearch]=false}};await{!LdApp.settings.value.settingsToolSearch}}
 private fun page(on:Boolean=true){start();runBlocking{ctx.ldStore.edit{it[K.settingsToolSearch]=on}};await{LdApp.settings.value.settingsToolSearch==on};show("settingshub:tools")}
 private fun query(q:String){rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings_tool_query")),true).performTextReplacement(q);settle()}
 @Test fun defaultOffAndCompleteEmptyCatalog(){assertFalse(LdSettings().settingsToolSearch);assertEquals(ORGANIZED_TOOLS,searchSettingsTools(" \t "));assertEquals(37,searchSettingsTools("").map{it.route}.toSet().size);page(false);rule.onNodeWithTag("settings_tool_query",true).assertDoesNotExist();for(id in listOf("live","notes","calculators","featurecontrols"))rule.onNodeWithTag("settings_destination_"+id,true).assertExists();rule.mainClock.autoAdvance=true;rule.onNodeWithTag("settings_tool_search_switch",true).performScrollTo();settle();shot("SET503-search-off")}
 @Test fun titleDescriptionTokensAndCase(){assertEquals(listOf("ivrmenus"),searchSettingsTools("IVR one digit").map{it.route});assertEquals(listOf("callcanvas"),searchSettingsTools("finger sketch").map{it.route});assertTrue(searchSettingsTools("xyz-none-347").isEmpty());for(t in ORGANIZED_TOOLS)assertTrue(t.route,searchSettingsTools(t.title).contains(t))}
 @Test fun reversibleSwitchChangesOnlyItsPreference(){page(false);val before=LdApp.settings.value;tapTag("settings_tool_search_switch");await{LdApp.settings.value.settingsToolSearch&&rule.onAllNodesWithTag("settings_tool_query",true).fetchSemanticsNodes().size==1};assertEquals(before.copy(settingsToolSearch=true),LdApp.settings.value);tapTag("settings_tool_search_switch");await{!LdApp.settings.value.settingsToolSearch&&rule.onAllNodesWithTag("settings_tool_query",true).fetchSemanticsNodes().isEmpty()};assertEquals(before,LdApp.settings.value)}
 @Test fun resultAndClearDoNotMutate(){page();val before=LdApp.settings.value;query("IVR");rule.onNodeWithTag("settings_tool_result_ivrmenus",true).assertExists();rule.onNodeWithText("Tools: 1",true).assertExists();rule.onNodeWithTag("settings_destination_live",true).assertDoesNotExist();tapTag("settings_tool_clear");rule.onNodeWithTag("settings_destination_live",true).assertExists();assertEquals(before,LdApp.settings.value)}
 @Test fun noMatchAndClearRestoreSections(){page();query("xyz-none-347");rule.onNodeWithText("No call tool found",true).assertExists();rule.onNodeWithText("Tools: 0",true).assertExists();tapTag("settings_tool_clear");rule.onNodeWithTag("settings_destination_featurecontrols",true).assertExists()}
 @Test fun everyResultUsesExistingRouteAndStartsNothing(){page();val before=LdApp.settings.value;for(t in ORGANIZED_TOOLS){query(t.title);tapTag("settings_tool_result_"+t.route);assertEquals(t.route,opened.last());show("settingshub:tools")};assertEquals(before,LdApp.settings.value);assertNull(rule.activity.state.pendingCall)}
 @Test fun otherCategoriesAndExistingToolSectionsUnchanged(){page();for(id in listOf("look","phone","safety","alerts","storage")){show("settingshub:$id");rule.onNodeWithTag("settings_tool_query",true).assertDoesNotExist();rule.onNodeWithTag("settings_tool_search_switch",true).assertDoesNotExist()};show("settingstools:notes");rule.onNodeWithTag("settings_tool_query",true).assertDoesNotExist();rule.onNodeWithTag("settings_tool_callcanvas",true).assertExists()}
 @Test fun finderEntryOpensCategoryAndAllSectionsRemain(){assertTrue(SettingsFinder.search("Call tools search").any{it.id=="settings-tool-search"&&it.route=="settingshub:tools"});page();for(id in listOf("live","notes","calculators","featurecontrols"))rule.onNodeWithTag("settings_destination_"+id,true).assertExists()}
 @Test fun dayGroupedResultsAndEmptyPixels(){page();query("notes");shot("SET503-day-results");query("xyz-none-347");shot("SET503-day-empty")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsToolSearchNightUiTest:OrganizedSettingsFixture(){
 @Test fun largeTextSearchResultAndOpen(){start(night=true);runBlocking{ctx.ldStore.edit{it[K.settingsToolSearch]=true}};await{LdApp.settings.value.settingsToolSearch};show("settingshub:tools");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings_tool_query")),true).performTextReplacement("IVR");settle();rule.mainClock.autoAdvance=true;rule.onNodeWithTag("settings_tool_result_ivrmenus",true).performScrollTo();settle();shot("SET503-night-result");tapTag("settings_tool_result_ivrmenus");assertEquals("ivrmenus",opened.last());assertNull(rule.activity.state.pendingCall)}
}
