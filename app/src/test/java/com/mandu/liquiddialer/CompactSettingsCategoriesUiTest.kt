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
class CompactSettingsCategoriesUiTest:OrganizedSettingsFixture(){
 @Before fun reset(){runBlocking{ctx.ldStore.edit{it[K.compactSettingsCategories]=false;it[K.settingsCategorySearch]=false;it[K.settingsToolSearch]=false}};await{LdApp.settings.value.let{!it.compactSettingsCategories&&!it.settingsCategorySearch&&!it.settingsToolSearch}}}
 private fun enable(){runBlocking{ctx.ldStore.edit{it[K.compactSettingsCategories]=true}};await{LdApp.settings.value.compactSettingsCategories}}
 @Test fun defaultOffKeepsDescriptions(){assertFalse(LdSettings().compactSettingsCategories);start();text(ORGANIZED_CATEGORIES.first().detail).assertExists();show("settingshub:look");text(ORGANIZED_DESTINATIONS.first().detail).assertExists()}
 @Test fun onRemovesOnlyCategoryDescriptions(){start();enable();for(c in ORGANIZED_CATEGORIES)text(c.detail).assertDoesNotExist();for(c in ORGANIZED_CATEGORIES){show("settingshub:"+c.id);for(d in ORGANIZED_DESTINATIONS.filter{it.category==c.id}){rule.onNodeWithText(d.title,false,true).assertExists();text(d.detail).assertDoesNotExist()}}}
 @Test fun everyDestinationRouteAndAllHomeRoutesRemain(){start();enable();val before=LdApp.settings.value;for(c in ORGANIZED_CATEGORIES){show("settings");tapTag("settings_category_"+c.id);assertEquals("settingshub:"+c.id,opened.last());for(d in ORGANIZED_DESTINATIONS.filter{it.category==c.id}){tapTag("settings_destination_"+d.id);assertEquals(d.route,opened.last());show("settingshub:"+c.id)}};assertEquals(before,LdApp.settings.value);assertNull(rule.activity.state.pendingCall)}
 @Test fun switchReversibleChangesOnlyOnePreference(){start();val before=LdApp.settings.value;tapTag("compact_settings_categories_switch");await{LdApp.settings.value.compactSettingsCategories};assertEquals(before.copy(compactSettingsCategories=true),LdApp.settings.value);show("settings");text(ORGANIZED_CATEGORIES.first().detail).assertDoesNotExist();tapTag("compact_settings_categories_switch");await{!LdApp.settings.value.compactSettingsCategories&&rule.onAllNodesWithText(ORGANIZED_CATEGORIES.first().detail,true).fetchSemanticsNodes().size==1};assertEquals(before,LdApp.settings.value)}
 @Test fun categorySearchDescriptionsStay(){start();enable();runBlocking{ctx.ldStore.edit{it[K.settingsCategorySearch]=true}};await{LdApp.settings.value.settingsCategorySearch};rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings_category_query")),true).performTextReplacement("number details");settle();text(ORGANIZED_DESTINATIONS.first{it.id=="numbercard"}.detail).assertExists()}
 @Test fun toolSearchDescriptionsAndToolListsStay(){start();enable();show("settingstools:live");text(ORGANIZED_TOOLS.first{it.section=="live"}.detail).assertExists();runBlocking{ctx.ldStore.edit{it[K.settingsToolSearch]=true}};await{LdApp.settings.value.settingsToolSearch};show("settingshub:tools");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings_tool_query")),true).performTextReplacement("IVR");settle();text(ORGANIZED_TOOLS.first{it.route=="ivrmenus"}.detail).assertExists()}
 @Test fun classicAndIndividualControlsUnchanged(){start();enable();show("settingshub:classic");text(SETTINGS_GROUPS.first().summary).assertExists();show("settings:recents");text("Home pe favourite contacts dikhao/chhupao").assertExists();assertTrue(SettingsFinder.search("Compact Settings category rows").any{it.id=="compact-settings-categories"})}
 @Test fun compactRowActuallyShorterAndTapHeightRetained(){start();val before=rule.onNodeWithTag("settings_category_look",true).fetchSemanticsNode().boundsInRoot.height;enable();settle();val after=rule.onNodeWithTag("settings_category_look",true).fetchSemanticsNode().boundsInRoot.height;assertTrue(after<before);assertTrue(after>=96f)}
 @Test fun dayCompactHomeSubpageAndOffPixels(){start();enable();shot("SET505-day-home");show("settingshub:look");shot("SET505-day-look");show("settings");tapTag("compact_settings_categories_switch");await{!LdApp.settings.value.compactSettingsCategories};rule.mainClock.autoAdvance=true;rule.onNodeWithTag("compact_settings_categories_switch",true).performScrollTo();settle();shot("SET505-off")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompactSettingsCategoriesNightUiTest:OrganizedSettingsFixture(){
 @Test fun nightLargeCompactHomeAndPhone(){start(night=true);runBlocking{ctx.ldStore.edit{it[K.compactSettingsCategories]=true}};await{LdApp.settings.value.compactSettingsCategories};shot("SET505-night-home");show("settingshub:phone");shot("SET505-night-phone");for(d in ORGANIZED_DESTINATIONS.filter{it.category=="phone"})rule.onNodeWithText(d.title,false,true).assertExists();assertNull(rule.activity.state.pendingCall)}
}
