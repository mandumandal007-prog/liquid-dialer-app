package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class FinderPinsFixture:FinderGroupFilterFixture(){
 @Before fun resetPins(){pins(false);runBlocking{ctx.repo.set(K.finderPinned,emptySet())}}
 @After fun cleanPins(){pins(false);runBlocking{ctx.repo.set(K.finderPinned,emptySet())}}
 protected fun pins(on:Boolean=true){runBlocking{ctx.repo.set(K.finderPins,on)};await{LdApp.settings.value.finderPins==on};settle()}
 protected fun pin(id:String){hit("settings_pin_"+id);await{id in LdApp.settings.value.finderPinned};settle()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderPinsUiTest:FinderPinsFixture(){
 @Test fun defaultOffSwitchOnlyAndNoChipsOrSection(){assertFalse(LdSettings().finderPins);assertTrue(LdSettings().finderPinned.isEmpty());finder();val before=LdApp.settings.value;SettingsFinder.all.take(6).forEach{rule.onAllNodesWithTag("settings_pin_"+it.id,true).assertCountEquals(0)};rule.onAllNodesWithTag("settings_pinned_count",true).assertCountEquals(0);rule.onAllNodesWithTag("settings_pin_full",true).assertCountEquals(0);hit("finder_pins_switch");await{LdApp.settings.value.finderPins};assertEquals(before.copy(finderPins=true),LdApp.settings.value);move("finder_pins_switch");saveShot(rule,"PINS538-settings-on");hit("finder_pins_switch");await{!LdApp.settings.value.finderPins};assertEquals(before,LdApp.settings.value);assertEquals("settingsfinder",SettingsFinder.all.single{it.id=="finder-pins"}.route)}
 @Test fun pinUnpinKeepsRoutesAndNoSettingMutation(){val opened=mutableListOf<String>();finder(onOpen={opened+=it});pins();pin("calls-29");assertEquals(setOf("calls-29"),LdApp.settings.value.finderPinned);move("settings_pinned_count");rule.onNodeWithTag("settings_pinned_count",true).assertIsDisplayed();hit("settings_pinned_calls-29");assertEquals(SettingsFinder.route(SettingsFinder.all.single{it.id=="calls-29"}),opened.last());val prefs=LdApp.settings.value;hit("settings_unpin_calls-29");await{LdApp.settings.value.finderPinned.isEmpty()};rule.onAllNodesWithTag("settings_pinned_count",true).assertCountEquals(0);assertEquals(prefs.copy(finderPinned=emptySet()),LdApp.settings.value);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun sectionHidesWhileSearchingAndChipDoesNotOpen(){val opened=mutableListOf<String>();finder(onOpen={opened+=it});pins();query("speaker");rule.onAllNodesWithTag("settings_pinned_count",true).assertCountEquals(0);pin("calls-29");assertTrue(opened.isEmpty());count("speaker","");rule.onNode(hasSetTextAction(),true).assertTextEquals("speaker");hit("settings_find_clear");move("settings_pinned_count");rule.onNodeWithTag("settings_pinned_count",true).assertIsDisplayed();rule.onNodeWithTag("settings_result_calls-29",true).assertExists();assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun capOfEightShowsMessageAndKeepsPins(){finder();pins();listOf("calls-29","calls-30","recording-58","display-8","sounds-34","speed-61","blocking-51","backup-71").forEach{pin(it)};assertEquals(8,LdApp.settings.value.finderPinned.size);hit("settings_pin_quiet-56");settle();assertEquals(8,LdApp.settings.value.finderPinned.size);assertFalse("quiet-56" in LdApp.settings.value.finderPinned);move("settings_pin_full");rule.onNodeWithTag("settings_pin_full",true).assertIsDisplayed();hit("settings_pin_calls-29");await{LdApp.settings.value.finderPinned.size==7};hit("settings_pin_quiet-56");await{"quiet-56" in LdApp.settings.value.finderPinned};rule.onAllNodesWithTag("settings_pin_full",true).assertCountEquals(0)}
 @Test fun dayPixels(){finder();query("speaker");move("settings_find_count");saveShot(rule,"PINS538-day-off");pins();pin("calls-29");pin("recording-58");hit("settings_find_clear");move("settings_pinned_count");rule.onNodeWithTag("settings_pinned_calls-29",true).assertIsDisplayed();saveShot(rule,"PINS538-day-on");move("settings_result_recording-58");rule.onNodeWithTag("settings_pin_recording-58",true).assertIsDisplayed();saveShot(rule,"PINS538-day-results")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderPinsNightUiTest:FinderPinsFixture(){
 @Test fun offHidesPinsAndOnRestoresThem(){finder(true);pins();pin("calls-29");pins(false);rule.onAllNodesWithTag("settings_pinned_count",true).assertCountEquals(0);rule.onAllNodesWithTag("settings_pin_calls-29",true).assertCountEquals(0);assertEquals(setOf("calls-29"),LdApp.settings.value.finderPinned);pins();move("settings_pinned_count");rule.onNodeWithTag("settings_pinned_calls-29",true).assertExists();hit("settings_unpin_calls-29");await{LdApp.settings.value.finderPinned.isEmpty()}}
 @Test fun nightPixels(){finder(true);query("speaker");move("settings_find_count");saveShot(rule,"PINS538-night-off");pins();pin("calls-29");pin("recording-58");hit("settings_find_clear");move("settings_pinned_count");saveShot(rule,"PINS538-night-on");move("settings_result_recording-58");saveShot(rule,"PINS538-night-results")}
 @Test fun pinnedRowsOpenRoutesAtLargeText(){val opened=mutableListOf<String>();finder(true,onOpen={opened+=it});pins();pin("backup-71");move("settings_pinned_backup-71");hit("settings_pinned_backup-71");assertEquals(SettingsFinder.route(SettingsFinder.all.single{it.id=="backup-71"}),opened.last());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
}
class FinderPinsTest{
 @Test fun defaultsOffAndEmpty(){assertFalse(LdSettings().finderPins);assertTrue(LdSettings().finderPinned.isEmpty())}
 @Test fun toggleCapCleanAndUnknown(){var pins=setOf<String>();val ids=SettingsFinder.all.map{it.id};ids.take(8).forEach{pins=SettingsFinder.togglePin(pins,it)};assertEquals(8,pins.size);try{SettingsFinder.togglePin(pins,ids[8]);fail("cap")}catch(e:IllegalArgumentException){};assertEquals(8,pins.size);pins=SettingsFinder.togglePin(pins,ids[0]);assertFalse(ids[0] in pins);pins=SettingsFinder.togglePin(pins,ids[8]);assertTrue(ids[8] in pins);try{SettingsFinder.togglePin(pins,"not-a-control");fail("unknown")}catch(e:IllegalArgumentException){}}
 @Test fun cleanDropsUnknownAndEntriesFollowIndexOrder(){val ids=SettingsFinder.all.map{it.id};val cleaned=SettingsFinder.cleanPins(setOf("not-a-control",ids[1],ids[0]));assertEquals(setOf(ids[0],ids[1]),cleaned);assertEquals(listOf(SettingsFinder.all[0],SettingsFinder.all[1]),SettingsFinder.pinnedEntries(cleaned));assertTrue(SettingsFinder.pinnedEntries(setOf("not-a-control")).isEmpty())}
 @Test fun indexedOnceWithFinderRoute(){assertEquals(1,SettingsFinder.all.count{it.id=="finder-pins"});assertEquals("settingsfinder",SettingsFinder.all.single{it.id=="finder-pins"}.route);assertTrue(SettingsFinder.search("pin").any{it.id=="finder-pins"})}
}
