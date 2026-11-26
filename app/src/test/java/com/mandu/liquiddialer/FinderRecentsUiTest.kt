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
abstract class FinderRecentsFixture:FinderPinsFixture(){
 @Before fun resetRecents(){recentsOn(false);runBlocking{ctx.repo.set(K.finderRecent,"")}}
 @After fun cleanRecents(){recentsOn(false);runBlocking{ctx.repo.set(K.finderRecent,"")}}
 protected fun recentsOn(on:Boolean=true){runBlocking{ctx.repo.set(K.finderRecents,on)};await{LdApp.settings.value.finderRecents==on};settle()}
 protected fun seedRecent(vararg w:String){runBlocking{ctx.repo.set(K.finderRecent,SettingsFinder.joinRecent(w.toList()))};await{LdApp.settings.value.finderRecent==w.toList()};settle()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderRecentsUiTest:FinderRecentsFixture(){
 @Test fun defaultOffSwitchOnlyAndNoChips(){assertFalse(LdSettings().finderRecents);assertTrue(LdSettings().finderRecent.isEmpty());finder();val before=LdApp.settings.value;rule.onAllNodesWithTag("settings_recent_row",true).assertCountEquals(0);hit("finder_recents_switch");await{LdApp.settings.value.finderRecents};assertEquals(before.copy(finderRecents=true),LdApp.settings.value);move("finder_recents_switch");saveShot(rule,"RECENTS539-settings-on");hit("finder_recents_switch");await{!LdApp.settings.value.finderRecents};assertEquals(before,LdApp.settings.value);assertEquals("settingsfinder",SettingsFinder.all.single{it.id=="finder-recents"}.route)}
 @Test fun openedResultSavesAndChipOnlyFillsQuery(){val opened=mutableListOf<String>();finder(onOpen={opened+=it});recentsOn();query("speaker");move("settings_result_calls-29");hit("settings_result_calls-29");await{LdApp.settings.value.finderRecent==listOf("speaker")};assertEquals(SettingsFinder.route(SettingsFinder.all.single{it.id=="calls-29"}),opened.last());val before=LdApp.settings.value;hit("settings_find_clear");move("settings_recent_row");rule.onNodeWithTag("settings_recent_0",true).assertIsDisplayed();opened.clear();hit("settings_recent_0");settle();rule.onNode(hasSetTextAction(),true).assertTextEquals("speaker");count("speaker","");assertTrue(opened.isEmpty());assertEquals(before,LdApp.settings.value);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun offAndShortQueryDoNotSave(){finder();query("speaker");move("settings_result_calls-29");hit("settings_result_calls-29");settle();assertTrue(LdApp.settings.value.finderRecent.isEmpty());recentsOn();query("s");val id=SettingsFinder.search("s").first().id;move("settings_result_"+id);hit("settings_result_"+id);settle();assertTrue(LdApp.settings.value.finderRecent.isEmpty())}
 @Test fun clearRecentDeletesListAndHidesChips(){finder();recentsOn();seedRecent("speaker","sim");move("settings_recent_row");rule.onNodeWithTag("settings_recent_1",true).assertIsDisplayed();hit("settings_recent_clear");await{LdApp.settings.value.finderRecent.isEmpty()};rule.onAllNodesWithTag("settings_recent_row",true).assertCountEquals(0);assertTrue(LdApp.settings.value.finderRecents)}
 @Test fun chipsHiddenWhileSearching(){finder();recentsOn();seedRecent("speaker");move("settings_recent_row");query("sim");rule.onAllNodesWithTag("settings_recent_row",true).assertCountEquals(0);hit("settings_find_clear");move("settings_recent_row");rule.onNodeWithTag("settings_recent_0",true).assertIsDisplayed()}
 @Test fun dayPixels(){finder();recentsOn();seedRecent("speaker","sim","font");move("settings_recent_row");saveShot(rule,"RECENTS539-day-on");pins();pin("calls-29");move("settings_recent_row");saveShot(rule,"RECENTS539-day-with-pins");query("speaker");move("settings_find_count");saveShot(rule,"RECENTS539-day-searching")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinderRecentsNightUiTest:FinderRecentsFixture(){
 @Test fun offHidesChipsAndKeepsList(){finder(true);recentsOn();seedRecent("speaker","sim");recentsOn(false);rule.onAllNodesWithTag("settings_recent_row",true).assertCountEquals(0);assertEquals(listOf("speaker","sim"),LdApp.settings.value.finderRecent);recentsOn();move("settings_recent_row");rule.onNodeWithTag("settings_recent_0",true).assertExists()}
 @Test fun nightPixels(){finder(true);recentsOn();seedRecent("speaker","recording","font size","swipe","sim");move("settings_recent_row");saveShot(rule,"RECENTS539-night-on");hit("settings_recent_clear");await{LdApp.settings.value.finderRecent.isEmpty()};saveShot(rule,"RECENTS539-night-cleared")}
}
class FinderRecentsTest{
 @Test fun defaultsOffAndEmpty(){assertFalse(LdSettings().finderRecents);assertTrue(LdSettings().finderRecent.isEmpty())}
 @Test fun pushDedupesCapsAndIgnoresShort(){var l=emptyList<String>();listOf("alpha","beta","gamma","delta","eps","zeta").forEach{l=SettingsFinder.pushRecent(l,it)};assertEquals(listOf("zeta","eps","delta","gamma","beta"),l);l=SettingsFinder.pushRecent(l,"  BETA  ");assertEquals(listOf("BETA","zeta","eps","delta","gamma"),l);assertEquals(l,SettingsFinder.pushRecent(l,"x"));assertEquals(l,SettingsFinder.pushRecent(l,"   "));assertEquals("two words",SettingsFinder.cleanRecent("  two \n  words "))}
 @Test fun splitJoinRoundTripAndDirtyInput(){val l=listOf("speaker","sim","font size");assertEquals(l,SettingsFinder.splitRecent(SettingsFinder.joinRecent(l)));assertEquals(listOf("a1","b2"),SettingsFinder.splitRecent("a1\n\nx\n b2 \nA1"));assertEquals(SettingsFinder.MAX_RECENT,SettingsFinder.splitRecent((1..9).joinToString("\n"){"item$it"}).size);assertTrue(SettingsFinder.splitRecent("").isEmpty())}
 @Test fun indexedOnceWithFinderRoute(){assertEquals(1,SettingsFinder.all.count{it.id=="finder-recents"});assertEquals("settingsfinder",SettingsFinder.all.single{it.id=="finder-recents"}.route);assertTrue(SettingsFinder.search("recent").any{it.id=="finder-recents"})}
}
