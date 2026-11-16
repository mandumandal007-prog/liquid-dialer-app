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
abstract class InstallmentPartViewFixture:InstallmentFixture(){
 protected fun choices(find:Boolean=true,fold:Boolean=true){runBlocking{ctx.repo.set(K.installmentFind,find);ctx.repo.set(K.installmentPartsFold,fold)};await{LdApp.settings.value.let{it.installmentFind==find&&it.installmentPartsFold==fold}}}
 protected fun start(){ready();choices();mount{InstallmentPreviewWorkspace(identity.value,source.value,{})};calc()}
 protected fun findPart(q:String="2"){input("installment_find_input",q);hit("installment_find_apply")}
 protected fun field(tag:String)=rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag(tag)),true)
 @After fun cleanupView(){choices(false,false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstallmentPartViewPart0UiTest:InstallmentPartViewFixture(){
 @Test fun foldKeepsTotalsCountFinder(){start();rule.onNodeWithTag("installment_row_1",true).assertDoesNotExist();rule.onNodeWithTag("installment_sum",true).assertTextContains("10.00",substring=true);rule.onNodeWithTag("installment_view_count",true).assertTextEquals("View: all 3 dated parts");hit("installment_parts_toggle");rule.onNodeWithTag("installment_row_1",true).assertExists();hit("installment_parts_toggle");rule.onNodeWithTag("installment_row_1",true).assertDoesNotExist()}
 @Test fun findOpensOnlyOriginalRow(){start();findPart();rule.onNodeWithTag("installment_row_1",true).assertDoesNotExist();rule.onNodeWithTag("installment_row_3",true).assertDoesNotExist();rule.onNodeWithTag("installment_amount_2",true).assertTextEquals("3.33");rule.onNodeWithTag("installment_view_count",true).assertTextContains("Totals include ALL parts",substring=true);rule.onNodeWithTag("installment_sum",true).assertTextContains("10.00",substring=true)}
 @Test fun showAllClearsFilter(){start();findPart();hit("installment_find_clear");(1..3).forEach{rule.onNodeWithTag("installment_row_$it",true).assertExists()};field("installment_find_input").assertTextEquals("")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstallmentPartViewPart1UiTest:InstallmentPartViewFixture(){
 @Test fun invalidShowsAllNoStaleSelection(){start();findPart();findPart("100");rule.onNodeWithTag("installment_find_error",true).assertExists();(1..3).forEach{rule.onNodeWithTag("installment_row_$it",true).assertExists()};field("installment_find_input").assertTextEquals("100");rule.onNodeWithTag("installment_value",true).assertTextEquals("3 dated parts")}
 @Test fun finderOffPreservesPreviewShowsAll(){start();findPart();choices(find=false);await{rule.onAllNodesWithTag("installment_find_input",true).fetchSemanticsNodes().isEmpty()};(1..3).forEach{rule.onNodeWithTag("installment_row_$it",true).assertExists()};rule.onNodeWithTag("installment_sum",true).assertTextContains("10.00",substring=true)}
 @Test fun foldOffPreservesFilterAndResult(){start();findPart();hit("installment_parts_toggle");choices(fold=false);await{rule.onAllNodesWithTag("installment_parts_toggle",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("installment_row_2",true).assertExists();rule.onNodeWithTag("installment_row_1",true).assertDoesNotExist();rule.onNodeWithTag("installment_sum",true).assertTextContains("10.00",substring=true)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstallmentPartViewSafetyUiTest:InstallmentPartViewFixture(){
 @Test fun editsResetViewAndRepreviewFolded(){start();findPart();input("installment_count","2");rule.onNodeWithTag("installment_result",true).assertDoesNotExist();hit("installment_preview");field("installment_find_input").assertTextEquals("");rule.onNodeWithTag("installment_view_count",true).assertTextEquals("View: all 2 dated parts");rule.onNodeWithTag("installment_row_1",true).assertDoesNotExist()}
 @Test fun viewReadOnlyNoExternalAction(){start();val b=java.io.File(ctx.filesDir,"call_estimates.json").readBytes();findPart();hit("installment_parts_toggle");hit("installment_find_clear");assertArrayEquals(b,java.io.File(ctx.filesDir,"call_estimates.json").readBytes());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity);assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 @Test fun finderEditClearsSelectedOnly(){start();findPart();input("installment_find_input","3");rule.onNodeWithTag("installment_view_count",true).assertTextEquals("View: all 3 dated parts");rule.onNodeWithTag("installment_row_1",true).assertExists();rule.onNodeWithTag("installment_value",true).assertTextEquals("3 dated parts")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstallmentPartViewLiveUiTest:InstallmentPartViewFixture(){
 @Test fun dayFoldFindPixels(){live();choices();calc();move("installment_parts_toggle");saveShot(rule,"INSTALL479-day-folded");findPart();move("installment_row_2");saveShot(rule,"INSTALL479-day-found");rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,number).isEmpty())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstallmentPartViewPixelsUiTest:InstallmentPartViewFixture(){
 @Test fun nightFinderAndResult(){live(true);choices();calc();findPart();move("installment_view_count");saveShot(rule,"INSTALL479-night-filter");move("installment_row_2");saveShot(rule,"INSTALL479-night-row");findPart("0");move("installment_find_error");saveShot(rule,"INSTALL479-night-error");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOff(){ready(true);choices(false,false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{InstallmentPreviewPage{}};move("installment_options_settings");saveShot(rule,"INSTALL479-settings-off")}
}
