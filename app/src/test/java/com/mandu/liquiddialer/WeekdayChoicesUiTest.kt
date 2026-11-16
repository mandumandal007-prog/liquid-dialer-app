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
abstract class WeekdayChoicesFixture:WeekdayFixture(){
 protected fun choices(shortcuts:Boolean=true,fold:Boolean=true){runBlocking{ctx.repo.set(K.weekdayShortcuts,shortcuts);ctx.repo.set(K.weekdayExclusionsFold,fold)};await{LdApp.settings.value.let{it.weekdayShortcuts==shortcuts&&it.weekdayExclusionsFold==fold}}}
 protected fun start(){ready();choices();mount{WeekdayShiftWorkspace({})}}
 protected fun field(tag:String)=rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag(tag)),true)
 @After fun cleanupChoices(){choices(false,false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeekdayChoicesPart0UiTest:WeekdayChoicesFixture(){
 @Test fun shortcutsNoAutoAndPreserveFields(){start();calc();hit("weekday_shortcut_all");rule.onNodeWithTag("weekday_result",true).assertDoesNotExist();field("weekday_first").assertTextEquals("2026-11-13");field("weekday_count").assertTextEquals("1");hit("weekday_calculate");rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-14 · Saturday")}
 @Test fun weekendsReplaceNotToggle(){start();calc();hit("weekday_shortcut_weekends");hit("weekday_shortcut_weekends");hit("weekday_calculate");rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-14 · Saturday");hit("weekday_shortcut_weekdays");hit("weekday_calculate");rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-16 · Monday")}
 @Test fun foldedExclusionsStillApply(){start();input("weekday_first","2026-11-13");hit("weekday_exclusions_toggle");input("weekday_excluded","2026-11-16");hit("weekday_exclusions_toggle");rule.onNodeWithTag("weekday_excluded",true).assertDoesNotExist();input("weekday_count","1");hit("weekday_calculate");rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-17 · Tuesday");rule.onNodeWithTag("weekday_exclusions_summary",true).assertTextContains("1 unique",substring=true)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeekdayChoicesPart1UiTest:WeekdayChoicesFixture(){
 @Test fun foldOffKeepsExclusionsAndResult(){start();hit("weekday_exclusions_toggle");input("weekday_excluded","2026-11-16");calc();choices(fold=false);await{rule.onAllNodesWithTag("weekday_excluded",true).fetchSemanticsNodes().size==1};field("weekday_excluded").assertTextEquals("2026-11-16");rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-17 · Tuesday");choices();await{rule.onAllNodesWithTag("weekday_excluded",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("weekday_value",true).assertExists()}
 @Test fun shortcutsOffKeepsChosenDays(){start();calc();hit("weekday_shortcut_weekends");choices(shortcuts=false);await{rule.onAllNodesWithTag("weekday_shortcut_all",true).fetchSemanticsNodes().isEmpty()};hit("weekday_calculate");rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-14 · Saturday")}
 @Test fun invalidHiddenSummaryAndNoResult(){start();hit("weekday_exclusions_toggle");input("weekday_excluded","bad");hit("weekday_exclusions_toggle");rule.onNodeWithTag("weekday_exclusions_summary",true).assertTextContains("need checking",substring=true);calc();rule.onNodeWithTag("weekday_error",true).assertExists();rule.onNodeWithTag("weekday_result",true).assertDoesNotExist()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeekdayChoicesSafetyUiTest:WeekdayChoicesFixture(){
 @Test fun clearHiddenExclusions(){start();hit("weekday_exclusions_toggle");input("weekday_excluded","2026-11-16");hit("weekday_exclusions_toggle");calc();hit("weekday_clear");rule.onNodeWithTag("weekday_exclusions_summary",true).assertTextContains("No manual exclusions",substring=true);rule.onNodeWithTag("weekday_result",true).assertDoesNotExist();assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun shortcutPreservesHiddenExclusions(){start();hit("weekday_exclusions_toggle");input("weekday_excluded","2026-11-14");hit("weekday_exclusions_toggle");input("weekday_first","2026-11-13");input("weekday_count","1");hit("weekday_shortcut_all");hit("weekday_calculate");rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-15 · Sunday")}
 @Test fun reopenClearsSession(){start();calc();mount{WeekdayShiftWorkspace({})};rule.onNodeWithTag("weekday_result",true).assertDoesNotExist();rule.onNodeWithTag("weekday_excluded",true).assertDoesNotExist();rule.onNodeWithTag("weekday_exclusions_summary",true).assertTextContains("No manual exclusions",substring=true)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeekdayChoicesLiveUiTest:WeekdayChoicesFixture(){
 @Test fun dayShortcutsAndFold(){live();choices();move("weekday_shortcut_weekends");saveShot(rule,"WEEKDAY478-day-shortcuts");hit("weekday_exclusions_toggle");input("weekday_excluded","2026-11-16");hit("weekday_exclusions_toggle");move("weekday_exclusions_summary");saveShot(rule,"WEEKDAY478-day-folded");rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeekdayChoicesPixelsUiTest:WeekdayChoicesFixture(){
 @Test fun nightWrappedSummaryAndField(){live(true);choices();move("weekday_shortcut_weekends");saveShot(rule,"WEEKDAY478-night-shortcuts");hit("weekday_exclusions_toggle");input("weekday_excluded","2026-11-14 2026-11-15 2026-11-16");move("weekday_excluded");saveShot(rule,"WEEKDAY478-night-open");hit("weekday_exclusions_toggle");move("weekday_exclusions_summary");saveShot(rule,"WEEKDAY478-night-folded");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOff(){ready(true);choices(false,false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{WeekdayShiftPage{}};move("weekday_options_settings");saveShot(rule,"WEEKDAY478-settings-off")}
}
