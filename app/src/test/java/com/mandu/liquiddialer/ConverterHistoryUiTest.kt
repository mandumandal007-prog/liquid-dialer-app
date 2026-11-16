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
abstract class HistoryOptionsFixture:ConverterOptionsFixture(){
 protected fun recallOptions(reuse:Boolean=true,fold:Boolean=true){runBlocking{ctx.repo.set(K.converterRecall,reuse);ctx.repo.set(K.converterHistoryFold,fold)};await{LdApp.settings.value.converterRecall==reuse&&LdApp.settings.value.converterHistoryFold==fold}}
 protected fun start(){recallOptions();direct();input("converter_input","1");hit("converter_convert");input("converter_input","2");hit("converter_convert")}
 protected fun unfold(){hit("converter_history_toggle")}
 protected fun recall(i:Int=1){hit("converter_recall_$i")}
 protected fun value()=rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag("converter_input")),true)
 @After fun cleanupRecall(){runBlocking{ctx.repo.set(K.converterRecall,false);ctx.repo.set(K.converterHistoryFold,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterHistoryPart0UiTest:HistoryOptionsFixture(){
 @Test fun foldedCountAndExplicitUnfold(){start();rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 2 / 10");rule.onNodeWithTag("converter_history_0",true).assertDoesNotExist();unfold();rule.onNodeWithTag("converter_history_0",true).assertExists();unfold();rule.onNodeWithTag("converter_history_0",true).assertDoesNotExist()}
 @Test fun recallNeedsConfirmAndCancelKeeps(){start();unfold();recall();value().assertTextEquals("2");rule.onNodeWithTag("converter_value",true).assertTextContains("6.561",substring=true);click("converter_recall_cancel");value().assertTextEquals("2");rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 2 / 10")}
 @Test fun confirmedOriginalNoAutoCalculation(){start();unfold();recall();click("converter_recall_confirm");value().assertTextEquals("1");rule.onNodeWithTag("converter_result",true).assertDoesNotExist();rule.onNodeWithTag("converter_recalled",true).assertExists();rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 2 / 10");hit("converter_convert");rule.onNodeWithTag("converter_value",true).assertTextEquals("= 3.28083989501312336 Int'l foot (ft)");rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 3 / 10")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterHistoryPart1UiTest:HistoryOptionsFixture(){
 @Test fun differentCategoryRestored(){start();hit("converter_kind_mass");unfold();recall(1);click("converter_recall_confirm");rule.onNodeWithTag("converter_from_label",true).assertTextEquals("From: Meter (m)");rule.onNodeWithTag("converter_to_label",true).assertTextEquals("To: Int'l foot (ft)");value().assertTextEquals("1")}
 @Test fun reuseOffDismissesConfirmation(){start();unfold();recall();recallOptions(false);await{rule.onAllNodesWithTag("converter_recall_confirm",true).fetchSemanticsNodes().isEmpty()};value().assertTextEquals("2");rule.onNodeWithTag("converter_recall_0",true).assertDoesNotExist();rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 2 / 10")}
 @Test fun layoutOffRestoresRowsKeepsCurrentResult(){start();recallOptions(fold=false);await{rule.onAllNodesWithTag("converter_history_0",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("converter_history_toggle",true).assertDoesNotExist();rule.onNodeWithTag("converter_value",true).assertExists();recallOptions();await{rule.onAllNodesWithTag("converter_history_0",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 2 / 10")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterHistorySafetyUiTest:HistoryOptionsFixture(){
 @Test fun foldedClearHistoryStillAvailable(){start();hit("converter_history_clear");rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 0 / 10");rule.onNodeWithTag("converter_value",true).assertExists();value().assertTextEquals("2")}
 @Test fun recallClearsTableButHistoryRemains(){start();hit("converter_table_make");unfold();recall();click("converter_recall_confirm");rule.onNodeWithTag("converter_table_count",true).assertDoesNotExist();rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 2 / 10");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun editsClearRestoredNotice(){start();unfold();recall();click("converter_recall_confirm");input("converter_input","3");rule.onNodeWithTag("converter_recalled",true).assertDoesNotExist()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterHistoryLiveUiTest:HistoryOptionsFixture(){
 @Test fun dayHistoryAndConfirmPixels(){recallOptions();live();input("converter_input","1");hit("converter_convert");move("converter_history_toggle");saveShot(rule,"HISTORY473-day-folded");unfold();move("converter_recall_0");saveShot(rule,"HISTORY473-day-history");recall(0);rule.onNodeWithTag("converter_recall_confirm",true).assertIsDisplayed();saveDialogShot("HISTORY473-day-confirm",0xfff0f4ff);click("converter_recall_confirm");move("converter_recalled");saveShot(rule,"HISTORY473-day-restored");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterHistoryPixelsUiTest:HistoryOptionsFixture(){
 @Test fun nightHistoryAndConfirm(){recallOptions();live(true);input("converter_input","12345.123456");hit("converter_convert");unfold();move("converter_recall_0");saveShot(rule,"HISTORY473-night-history");recall(0);rule.onNodeWithTag("converter_recall_confirm",true).assertIsDisplayed();saveDialogShot("HISTORY473-night-confirm",0xff0d1320);click("converter_recall_cancel");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOffPixels(){seed(night=true);recallOptions(false,false);mount{ConverterHistoryPage{}};saveShot(rule,"HISTORY473-settings-off")}
}
