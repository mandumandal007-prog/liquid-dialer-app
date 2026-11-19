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
abstract class TallyShownSumFixture:TallyFindFixture(){
 @Before fun resetSum(){runBlocking{ctx.repo.set(K.tallyShownSum,false);ctx.repo.set(K.tallySort,false);ctx.repo.set(K.tallyPaired,false)};await{LdApp.settings.value.let{!it.tallyShownSum&&!it.tallySort&&!it.tallyPaired}}}
 protected fun summary(on:Boolean=true){runBlocking{ctx.repo.set(K.tallyShownSum,on)};await{LdApp.settings.value.tallyShownSum==on}}
 protected fun example(){sample();add("Wheat bags","7");summary()}
 protected fun sum(value:String){rule.onNodeWithTag("tally_shown_sum",true).assertTextEquals(value)}
 @After fun cleanSum(){summary(false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallyShownSumUiTest:TallyShownSumFixture(){
 @Test fun defaultOffKeepsOriginalBoard(){assertFalse(LdSettings().tallyShownSum);sample();rule.onNodeWithTag("tally_shown_sum",true).assertDoesNotExist();rule.onNodeWithTag("tally_total",true).assertTextEquals("5");rule.onNodeWithTag("tally_find_count",true).assertTextEquals("2 shown / 2 counters · All")}
 @Test fun nameFiltersAndEmptyViewHaveExactScope(){example();sum("12");query("rice");sum("5");rule.onNodeWithTag("tally_shown_sum_scope",true).assertTextEquals("Only 1 shown of 3 counters. Whole-board raw sum: 12.");query("no match");sum("0");rule.onNodeWithTag("tally_shown_sum_scope",true).assertTextEquals("Only 0 shown of 3 counters. Whole-board raw sum: 12.");hit("tally_find_clear");sum("12");rule.onNodeWithTag("tally_total",true).assertTextEquals("12")}
 @Test fun zeroNonzeroEditAndUndoRefresh(){example();hit("tally_find_zero");sum("0");hit("tally_plus_1");sum("0");rule.onNodeWithTag("tally_shown_sum_scope",true).assertTextEquals("Only 0 shown of 3 counters. Whole-board raw sum: 13.");hit("tally_find_nonzero");sum("13");hit("tally_undo");sum("12");rule.onNodeWithTag("tally_find_count",true).assertTextEquals("2 shown / 3 counters · Nonzero")}
 @Test fun viewDoesNotAddUndoAndZeroStillUsesHiddenRows(){example();query("rice");val before=LdApp.settings.value;summary(false);summary();sum("5");assertEquals(before,LdApp.settings.value);hit("tally_zero");click("tally_confirm");sum("0");rule.onNodeWithTag("tally_total",true).assertTextEquals("0");hit("tally_undo");sum("5");rule.onNodeWithTag("tally_total",true).assertTextEquals("12");hit("tally_undo");hit("tally_find_clear");rule.onNodeWithTag("tally_count",true).assertTextEquals("Raw sum · 2 /12 counters")}
 @Test fun offAndFinderOffKeepBoardAndTypedEdit(){example();query("rice");hit("tally_actions_0");hit("tally_edit_0");input("tally_name","Rice sacks");summary(false);await{rule.onAllNodesWithTag("tally_shown_sum",true).fetchSemanticsNodes().isEmpty()};rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag("tally_name")),true).assertTextEquals("Rice sacks");summary();await{rule.onAllNodesWithTag("tally_shown_sum",true).fetchSemanticsNodes().size==1};sum("5");choices(find=false);await{rule.onAllNodesWithTag("tally_shown_sum",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("tally_total",true).assertTextEquals("12");choices();await{rule.onAllNodesWithTag("tally_shown_sum",true).fetchSemanticsNodes().size==1};sum("12")}
 @Test fun sortAndMaximumBoardSumAreViewOnly(){board();summary();for(i in 0..11)add("Bags $i","1000000000");sum("12000000000");runBlocking{ctx.repo.set(K.tallySort,true)};await{LdApp.settings.value.tallySort};hit("tally_sort_quantity_high-low");sum("12000000000");query("Bags 11");sum("1000000000");rule.onNodeWithTag("tally_total",true).assertTextEquals("12000000000");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun settingsSwitchOnlyOnePreferenceAndFinder(){seed();summary(false);mount{CallTallyPage{}};val before=LdApp.settings.value;hit("tally_shown_sum_switch");await{LdApp.settings.value.tallyShownSum};assertEquals(before.copy(tallyShownSum=true),LdApp.settings.value);hit("tally_shown_sum_switch");await{!LdApp.settings.value.tallyShownSum};assertEquals(before,LdApp.settings.value);assertTrue(SettingsFinder.search("Shown-counter raw sum").any{it.id=="tally-shown-sum"&&it.route=="calltally"});move("tally_shown_sum_switch");saveShot(rule,"SUM517-settings-off")}
 @Test fun dayAndEmptyPixels(){example();query("rice");move("tally_shown_sum_panel");saveShot(rule,"SUM517-day");query("no match");move("tally_shown_sum_panel");saveShot(rule,"SUM517-empty");summary(false);await{rule.onAllNodesWithTag("tally_shown_sum_panel",true).fetchSemanticsNodes().isEmpty()};move("tally_find_count");saveShot(rule,"SUM517-off")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallyShownSumNightUiTest:TallyShownSumFixture(){
 @Test fun nightLargeInCallPanel(){show(night=true);choices();summary();runBlocking{ctx.repo.set(K.callTally,true)};await{LdApp.settings.value.callTally};open();hit("call_tally_open");add("Rice bags","1000000000");add("Wheat bags","1000000000");query("rice");move("tally_shown_sum_panel");saveShot(rule,"SUM517-night");sum("1000000000");rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
}
