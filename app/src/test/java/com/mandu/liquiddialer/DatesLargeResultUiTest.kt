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
abstract class DatesLargeResultFixture:DatesRecallFixture(){
 @Before fun resetLarge(){large(false)}
 @After fun cleanLarge(){large(false)}
 protected fun large(on:Boolean=true){runBlocking{ctx.repo.set(K.datesLargeResult,on)};await{LdApp.settings.value.datesLargeResult==on};rule.waitForIdle()}
 protected fun height(tag:String)=rule.onNodeWithTag(tag,true).getUnclippedBoundsInRoot().let{(it.bottom-it.top).value}
 protected fun verifyResult(r:CallDates.Result){rule.onNodeWithTag("dates_value",true).assertTextEquals(r.title);rule.onNodeWithTag("dates_result_detail",true).assertTextEquals(r.detail)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DatesLargeResultUiTest:DatesLargeResultFixture(){
 @Test fun defaultOffAndLargerThenOriginalSameResult(){assertFalse(LdSettings().datesLargeResult);dates();calc();verifyResult(CallDates.shift("2026-11-16","7"));val old=height("dates_value");large();await{height("dates_value")>old};verifyResult(CallDates.shift("2026-11-16","7"));rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 1 /10");large(false);await{height("dates_value")==old};verifyResult(CallDates.shift("2026-11-16","7"));move("dates_result");saveShot(rule,"RESULT522-day-off")}
 @Test fun threeModesSameMathFullDetailAndNoActions(){dates();large();calc();verifyResult(CallDates.shift("2026-11-16","7"));move("dates_result");saveShot(rule,"RESULT522-day-on");hit("dates_mode_gap");input("dates_last","2026-11-15");hit("dates_calculate");verifyResult(CallDates.gap("2026-11-16","2026-11-15"));hit("dates_mode_weekdays");input("dates_last","2026-11-23");hit("dates_calculate");verifyResult(CallDates.weekdays("2026-11-16","2026-11-23"));rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 3 /10");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun layoutKeepsFieldsHistoryAndRecallConfirmation(){sample();reuse(1);large();rule.onNodeWithTag("dates_recall_confirm",true).assertExists();click("dates_recall_confirm");field("dates_first").assertTextEquals("2026-11-16");rule.onNodeWithTag("dates_restored",true).assertExists();rule.onNodeWithTag("dates_result",true).assertDoesNotExist();large(false);field("dates_first").assertTextEquals("2026-11-16");rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 2 /10");hit("dates_calculate");verifyResult(CallDates.shift("2026-11-16","7"));input("dates_days","-4");large();field("dates_days").assertTextEquals("-4");rule.onNodeWithTag("dates_result",true).assertDoesNotExist()}
 @Test fun invalidInputAndClearUnchanged(){dates();large();input("dates_first","2026-02-30");hit("dates_calculate");rule.onNodeWithTag("dates_error",true).assertExists();large(false);rule.onNodeWithTag("dates_error",true).assertExists();rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 0 /10");hit("dates_clear");field("dates_first").assertTextEquals("");field("dates_days").assertTextEquals("7");rule.onNodeWithTag("dates_result",true).assertDoesNotExist()}
 @Test fun settingsOnePreferenceReversibleFinder(){seed();mount{CallDatesPage{}};val before=LdApp.settings.value;hit("dates_large_result_switch");await{LdApp.settings.value.datesLargeResult};assertEquals(before.copy(datesLargeResult=true),LdApp.settings.value);move("dates_large_result_switch");saveShot(rule,"RESULT522-settings-on");hit("dates_large_result_switch");await{!LdApp.settings.value.datesLargeResult};assertEquals(before,LdApp.settings.value);assertTrue(SettingsFinder.search("Larger date result").any{it.id=="dates-large-result"&&it.route=="calldates"})}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DatesLargeResultNightUiTest:DatesLargeResultFixture(){
 @Test fun nightLargeFullResultAndEndCallReachable(){show(night=true);choices();large();runBlocking{ctx.repo.set(K.callDates,true)};await{LdApp.settings.value.callDates};open();hit("call_dates_open");calc();verifyResult(CallDates.shift("2026-11-16","7"));move("dates_result");saveShot(rule,"RESULT522-night-large");move("dates_result_detail");saveShot(rule,"RESULT522-night-detail");rule.onNodeWithTag("dates_result_detail",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();hit("dates_clear");field("dates_first").assertTextEquals("");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
}
