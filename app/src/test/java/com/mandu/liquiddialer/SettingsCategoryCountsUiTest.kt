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
class SettingsCategoryCountsUiTest:OrganizedSettingsFixture(){
 @Before fun reset(){runBlocking{ctx.ldStore.edit{it[K.settingsCategoryCounts]=false;it[K.compactSettingsCategories]=false;it[K.settingsCategorySearch]=false}};await{LdApp.settings.value.let{!it.settingsCategoryCounts&&!it.compactSettingsCategories&&!it.settingsCategorySearch}}}
 private fun enable(){runBlocking{ctx.ldStore.edit{it[K.settingsCategoryCounts]=true}};await{LdApp.settings.value.settingsCategoryCounts};settle()}
 @Test fun defaultOffAndOriginalDetails(){assertFalse(LdSettings().settingsCategoryCounts);start();for(c in ORGANIZED_CATEGORIES){rule.onNodeWithTag("settings_category_count_"+c.id,true).assertDoesNotExist();text(c.detail).assertExists()}}
 @Test fun countsMatchActualDestinationPages(){start();enable();for(c in ORGANIZED_CATEGORIES)rule.onNodeWithTag("settings_category_count_"+c.id,true).assertTextEquals("${ORGANIZED_DESTINATIONS.count{it.category==c.id}} pages");assertEquals(23,ORGANIZED_CATEGORIES.sumOf{c->ORGANIZED_DESTINATIONS.count{it.category==c.id}})}
 @Test fun switchOnlyChangesThisPreferenceAndRestores(){start();val before=LdApp.settings.value;tapTag("settings_category_counts_switch");await{LdApp.settings.value.settingsCategoryCounts&&ORGANIZED_CATEGORIES.all{c->rule.onAllNodesWithTag("settings_category_count_"+c.id,true).fetchSemanticsNodes().size==1}};assertEquals(before.copy(settingsCategoryCounts=true),LdApp.settings.value);show("settings");tapTag("settings_category_counts_switch");await{!LdApp.settings.value.settingsCategoryCounts&&ORGANIZED_CATEGORIES.all{c->rule.onAllNodesWithTag("settings_category_count_"+c.id,true).fetchSemanticsNodes().isEmpty()}};assertEquals(before,LdApp.settings.value);for(c in ORGANIZED_CATEGORIES)rule.onNodeWithTag("settings_category_count_"+c.id,true).assertDoesNotExist()}
 @Test fun allRoutesAndNoCallOrSettingSideEffect(){start();enable();val before=LdApp.settings.value;for(c in ORGANIZED_CATEGORIES){show("settings");tapTag("settings_category_"+c.id);assertEquals("settingshub:"+c.id,opened.last());for(d in ORGANIZED_DESTINATIONS.filter{it.category==c.id}){tapTag("settings_destination_"+d.id);assertEquals(d.route,opened.last());show("settingshub:"+c.id)}};assertEquals(before,LdApp.settings.value);assertNull(rule.activity.state.pendingCall)}
 @Test fun compactKeepsCountsAndHidesOnlyDescriptions(){start();enable();runBlocking{ctx.ldStore.edit{it[K.compactSettingsCategories]=true}};await{LdApp.settings.value.compactSettingsCategories};for(c in ORGANIZED_CATEGORIES){text(c.detail).assertDoesNotExist();rule.onNodeWithTag("settings_category_count_"+c.id,true).assertExists()};shot("SET507-day-compact")}
 @Test fun searchResultsKeepDescriptionsWithoutCategoryBadges(){start();enable();runBlocking{ctx.ldStore.edit{it[K.settingsCategorySearch]=true}};await{LdApp.settings.value.settingsCategorySearch};rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings_category_query")),true).performTextReplacement("number details");settle();text(ORGANIZED_DESTINATIONS.first{it.id=="numbercard"}.detail).assertExists();for(c in ORGANIZED_CATEGORIES)rule.onNodeWithTag("settings_category_count_"+c.id,true).assertDoesNotExist()}
 @Test fun classicAndFinderUnaffected(){start();enable();show("settingshub:classic");text(SETTINGS_GROUPS.first().summary).assertExists();assertTrue(SettingsFinder.search("category page counts").any{it.id=="settings-category-counts"});show("settings:recents");text("Home pe favourite contacts dikhao/chhupao").assertExists()}
 @Test fun dayAndOffPixels(){start();enable();shot("SET507-day");tapTag("settings_category_counts_switch");await{!LdApp.settings.value.settingsCategoryCounts};rule.mainClock.autoAdvance=true;rule.onNodeWithTag("settings_category_counts_switch",true).performScrollTo();settle();shot("SET507-off")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsCategoryCountsNightUiTest:OrganizedSettingsFixture(){
 @Test fun nightLargeCountAndCompactPixels(){start(night=true);runBlocking{ctx.ldStore.edit{it[K.settingsCategoryCounts]=true;it[K.compactSettingsCategories]=false}};await{LdApp.settings.value.settingsCategoryCounts&&!LdApp.settings.value.compactSettingsCategories};shot("SET507-night-large");runBlocking{ctx.ldStore.edit{it[K.compactSettingsCategories]=true}};await{LdApp.settings.value.compactSettingsCategories};show("settings");shot("SET507-night-compact");for(c in ORGANIZED_CATEGORIES){rule.mainClock.autoAdvance=true;rule.onNodeWithTag("settings_category_"+c.id,true).performScrollTo();settle();rule.onNodeWithText(c.title,false,true).assertIsDisplayed();rule.onNodeWithTag("settings_category_count_"+c.id,true).assertIsDisplayed()};assertNull(rule.activity.state.pendingCall)}
}
