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
abstract class DatesRecallFixture:ConverterOptionsFixture(){
 protected fun choices(reuse:Boolean=true,fold:Boolean=true){runBlocking{ctx.repo.set(K.datesRecall,reuse);ctx.repo.set(K.datesHistoryFold,fold)};await{LdApp.settings.value.let{it.datesRecall==reuse&&it.datesHistoryFold==fold}}}
 protected fun dates(){seed();choices();mount{CallDatesWorkspace({})}}
 protected fun calc(){input("dates_first","2026-11-16");input("dates_days","7");hit("dates_calculate")}
 protected fun sample(){dates();calc();input("dates_first","2026-12-01");hit("dates_calculate")}
 protected fun reuse(i:Int=1){hit("dates_history_toggle");hit("dates_recall_$i")}
 protected fun field(tag:String)=rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag(tag)),true)
 @After fun cleanupDates(){choices(false,false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DatesRecallPart0UiTest:DatesRecallFixture(){
 @Test fun foldOnlyCountClearVisible(){sample();rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 2 /10");rule.onNodeWithTag("dates_history_0",true).assertDoesNotExist();hit("dates_history_toggle");rule.onNodeWithTag("dates_history_0",true).assertExists();hit("dates_history_toggle");rule.onNodeWithTag("dates_history_0",true).assertDoesNotExist()}
 @Test fun cancelKeepsCurrentFormResult(){sample();reuse();field("dates_first").assertTextEquals("2026-12-01");click("dates_recall_cancel");field("dates_first").assertTextEquals("2026-12-01");rule.onNodeWithTag("dates_value",true).assertTextContains("2026-12-08",substring=true)}
 @Test fun confirmOriginalNoAutoResult(){sample();reuse();click("dates_recall_confirm");field("dates_first").assertTextEquals("2026-11-16");field("dates_days").assertTextEquals("7");rule.onNodeWithTag("dates_result",true).assertDoesNotExist();rule.onNodeWithTag("dates_restored",true).assertExists();rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 2 /10");hit("dates_calculate");rule.onNodeWithTag("dates_restored",true).assertDoesNotExist();rule.onNodeWithTag("dates_value",true).assertTextContains("2026-11-23",substring=true);rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 3 /10")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DatesRecallPart1UiTest:DatesRecallFixture(){
 @Test fun gapModeDatesRestored(){dates();hit("dates_mode_gap");input("dates_first","2026-11-17");input("dates_last","2026-11-16");hit("dates_calculate");hit("dates_mode_shift");input("dates_first","2026-12-01");reuse(0);click("dates_recall_confirm");field("dates_first").assertTextEquals("2026-11-17");field("dates_last").assertTextEquals("2026-11-16");rule.onNodeWithTag("dates_days",true).assertDoesNotExist();hit("dates_calculate");rule.onNodeWithTag("dates_value",true).assertTextEquals("-1 calendar days")}
 @Test fun reuseOffDismissesDialog(){sample();reuse();choices(reuse=false);await{rule.onAllNodesWithTag("dates_recall_confirm",true).fetchSemanticsNodes().isEmpty()};field("dates_first").assertTextEquals("2026-12-01");rule.onNodeWithTag("dates_value",true).assertExists();rule.onNodeWithTag("dates_recall_0",true).assertDoesNotExist()}
 @Test fun foldOffKeepsFormResultHistory(){sample();choices(fold=false);await{rule.onAllNodesWithTag("dates_history_0",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("dates_history_toggle",true).assertDoesNotExist();field("dates_first").assertTextEquals("2026-12-01");rule.onNodeWithTag("dates_value",true).assertExists();choices();await{rule.onAllNodesWithTag("dates_history_0",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 2 /10")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DatesRecallSafetyUiTest:DatesRecallFixture(){
 @Test fun clearFoldedHistoryKeepsCurrent(){sample();hit("dates_history_clear");rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 0 /10");rule.onNodeWithTag("dates_value",true).assertExists();field("dates_first").assertTextEquals("2026-12-01");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun editsClearRestoredNotice(){sample();reuse();click("dates_recall_confirm");input("dates_days","8");rule.onNodeWithTag("dates_restored",true).assertDoesNotExist();rule.onNodeWithTag("dates_result",true).assertDoesNotExist()}
 @Test fun reopenClearsHistory(){sample();mount{CallDatesWorkspace({})};rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 0 /10")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DatesRecallLiveUiTest:DatesRecallFixture(){
 @Test fun dayHistoryConfirmRestore(){show();choices();runBlocking{ctx.repo.set(K.callDates,true)};await{LdApp.settings.value.callDates};open();hit("call_dates_open");calc();move("dates_history_toggle");saveShot(rule,"DATES477-day-folded");reuse(0);rule.onNodeWithTag("dates_recall_confirm",true).assertIsDisplayed();saveDialogShot("DATES477-day-confirm",0xfff0f4ff);click("dates_recall_confirm");move("dates_restored");saveShot(rule,"DATES477-day-restored");rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DatesRecallPixelsUiTest:DatesRecallFixture(){
 @Test fun nightHistoryAndDialog(){show(night=true);choices();runBlocking{ctx.repo.set(K.callDates,true)};await{LdApp.settings.value.callDates};open();hit("call_dates_open");calc();hit("dates_history_toggle");move("dates_recall_0");saveShot(rule,"DATES477-night-history");hit("dates_recall_0");rule.onNodeWithTag("dates_recall_confirm",true).assertIsDisplayed();saveDialogShot("DATES477-night-confirm",0xff0d1320);click("dates_recall_cancel");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOff(){seed(night=true);choices(false,false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{CallDatesPage{}};move("dates_history_settings");saveShot(rule,"DATES477-settings-off")}
}
