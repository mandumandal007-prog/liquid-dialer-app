package com.mandu.liquiddialer
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
abstract class RatesFixture:LiveCallDeskFixture(){
 protected val n="+919876543210"
 protected val sample=CallEstimate.Draft(lines=listOf(CallEstimate.line("Rice bags","2","10.00","a"),CallEstimate.line("Oil","1","20.00","b")))
 protected val source=mutableStateOf(sample);protected val identity=mutableStateOf(n)
 protected var applied:CallEstimate.Draft?=null;protected var closed=false
 protected fun ratesSeed(night:Boolean=false,on:Boolean=true){seed(false,night);android.util.AtomicFile(File(ctx.filesDir,"call_estimates.json")).delete();CallEstimate.save(ctx,n,0,sample);runBlocking{ctx.repo.set(K.estimateRates,on)};await{LdApp.settings.value.estimateRates==on}}
 protected fun ratesShow(night:Boolean=false,on:Boolean=true,direct:Boolean=true){ratesSeed(night,on);mount{if(direct)EstimateRatesWorkspace(identity.value,source.value,{closed=true},{applied=it})else CallEstimatePage(n,{closed=true})};if(!direct)await{rule.onAllNodesWithTag("estimate_total",true).fetchSemanticsNodes().size==1}}
 protected fun move(tag:String){repeat(35){val node=rule.onNodeWithTag(tag,true);val b=node.getUnclippedBoundsInRoot();val sc=rule.onAllNodes(hasScrollAction(),true).onLast();val v=sc.getUnclippedBoundsInRoot();if(b.top>=v.top+16.dp&&b.bottom<=v.bottom-32.dp){settle();node.assertIsDisplayed();return};val dy=if(b.top<v.top+16.dp)(b.top-v.top-16.dp).value else (b.bottom-v.bottom+32.dp).value;sc.performSemanticsAction(SemanticsActions.ScrollBy){it(0f,dy*rule.density.density)};settle()};error("Not visible $tag")}
 protected fun tap(tag:String){move(tag);click(tag)}
 protected fun input(tag:String,text:String){move(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(text);settle()}
 protected fun preview(){tap("rates_all");tap("rates_preview")}
 protected fun file()=File(ctx.filesDir,"call_estimates.json")
 @After fun ratesClean(){runBlocking{ctx.repo.set(K.estimateRates,false)};android.util.AtomicFile(file()).delete()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class EstimateRatesUiTest:RatesFixture(){
 @Test fun selectionEmptyPreviewReadOnly(){ratesShow();val before=file().readBytes();rule.onNodeWithTag("rates_count",true).assertTextEquals("0 / 2 items selected");rule.onNodeWithTag("rates_preview",true).assertIsNotEnabled();preview();rule.onNodeWithTag("rates_rate_a",true).assertTextEquals("Rate 10.00 → 11.00");rule.onNodeWithTag("rates_total",true).assertTextEquals("Total 40.00 → 44.00");assertArrayEquals(before,file().readBytes());assertNull(applied)}
 @Test fun selectionClearDirectionAndPercentInvalidate(){ratesShow();preview();tap("rates_decrease");rule.onAllNodesWithTag("rates_result",true).assertCountEquals(0);tap("rates_preview");rule.onNodeWithTag("rates_rate_a",true).assertTextEquals("Rate 10.00 → 9.00");input("rates_percent","20");rule.onAllNodesWithTag("rates_result",true).assertCountEquals(0);tap("rates_preview");tap("rates_none");rule.onAllNodesWithTag("rates_result",true).assertCountEquals(0);rule.onNodeWithTag("rates_count",true).assertTextEquals("0 / 2 items selected")}
 @Test fun fractionalRateErrorNoMutation(){source.value=sample.copy(lines=listOf(CallEstimate.line("Tiny","1","0.01","a")));ratesShow();preview();rule.onNodeWithTag("rates_error",true).assertTextContains("No rate was rounded",substring=true);assertNull(applied);assertEquals("10.00",CallEstimate.read(ctx,n).lines[0].rate)}
 @Test fun confirmationCancelThenUnsavedApply(){ratesShow();val before=file().readBytes();preview();tap("rates_use");assertNull(applied);click("rates_cancel");assertNull(applied);tap("rates_use");click("rates_confirm");assertEquals(listOf("11.00","22.00"),applied!!.lines.map{it.rate});assertEquals(sample.lines.map{it.quantity},applied!!.lines.map{it.quantity});assertArrayEquals(before,file().readBytes())}
 @Test fun dirtyBackKeepDiscard(){ratesShow();tap("rates_all");tap("rates_back");click("rates_keep");assertFalse(closed);tap("rates_back");click("rates_discard");assertTrue(closed);assertNull(applied)}
 @Test fun untouchedBack(){ratesShow();tap("rates_back");assertTrue(closed);assertNull(applied)}
 @Test fun sourceChangeDropsPreviewAndConfirm(){ratesShow();preview();tap("rates_use");rule.runOnUiThread{source.value=sample.copy(discount="1")};settle();rule.onAllNodesWithTag("rates_confirm",true).assertCountEquals(0);rule.onAllNodesWithTag("rates_result",true).assertCountEquals(0);rule.onNodeWithTag("rates_count",true).assertTextEquals("0 / 2 items selected");assertNull(applied)}
 @Test fun numberChangeDropsSelection(){ratesShow();preview();rule.runOnUiThread{identity.value="+449876543210"};settle();rule.onAllNodesWithTag("rates_result",true).assertCountEquals(0);rule.onNodeWithTag("rates_count",true).assertTextEquals("0 / 2 items selected");assertNull(applied)}
 @Test fun offRemovesConfirmationAndReenableEmpty(){ratesShow();preview();tap("rates_use");runBlocking{ctx.repo.set(K.estimateRates,false)};await{rule.onAllNodesWithTag("rates_confirm",true).fetchSemanticsNodes().isEmpty()};assertNull(applied);runBlocking{ctx.repo.set(K.estimateRates,true)};await{rule.onAllNodesWithTag("rates_count",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("rates_count",true).assertTextEquals("0 / 2 items selected")}
 @Test fun invalidSourceRefuses(){source.value=CallEstimate.Draft();ratesShow();rule.onNodeWithTag("rates_source_error",true).assertExists();rule.onNodeWithTag("rates_preview",true).assertIsNotEnabled()}
 @Test fun selectedOnlyRatePreview(){ratesShow();tap("rates_select_a");tap("rates_preview");rule.onNodeWithTag("rates_total",true).assertTextEquals("Total 40.00 → 42.00");tap("rates_use");click("rates_confirm");assertEquals(sample.lines[1],applied!!.lines[1])}
 @Test fun dayPreviewPixels(){ratesShow();preview();move("rates_rate_a");saveShot(rule,"RATES466-day-rates");move("rates_total");saveShot(rule,"RATES466-day-total")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class EstimateRatesIntegrationUiTest:RatesFixture(){
 @Test fun offNoEntry(){ratesShow(on=false,direct=false);rule.onAllNodesWithTag("estimate_rates",true).assertCountEquals(0);rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 40.00")}
 @Test fun applyUnsavedThenExplicitSaveExactNumber(){ratesShow(direct=false);val before=file().readBytes();input("estimate_discount","1");tap("estimate_rates");preview();rule.onNodeWithTag("rates_total",true).assertTextEquals("Total 39.00 → 43.00");tap("rates_use");click("rates_confirm");await{rule.onAllNodesWithTag("estimate_total",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 43.00");rule.onNodeWithTag("estimate_status",true).assertTextEquals("Unsaved changes");assertArrayEquals(before,file().readBytes());tap("estimate_save");await{CallEstimate.read(ctx,n).lines[0].rate=="11.00"};assertEquals("1",CallEstimate.read(ctx,n).discount);assertTrue(CallEstimate.read(ctx,"+449876543210").lines.isEmpty())}
 @Test fun offWhileAdjustingReturnsOriginalDraft(){ratesShow(direct=false);input("estimate_discount","1");tap("estimate_rates");preview();runBlocking{ctx.repo.set(K.estimateRates,false)};await{rule.onAllNodesWithTag("estimate_total",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 39.00");rule.onAllNodesWithTag("estimate_rates",true).assertCountEquals(0);assertEquals("10.00",CallEstimate.read(ctx,n).lines[0].rate)}
 @Test fun discardKeepsUnsavedDraftClipboardAndNoIntent(){ratesShow(direct=false);val clipboard=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("keep","Keep"));input("estimate_discount","1");tap("estimate_rates");preview();tap("rates_back");click("rates_discard");await{rule.onAllNodesWithTag("estimate_total",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 39.00");assertEquals("Keep",clipboard.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun settingEnablesOnlyOwnFeature(){ratesSeed(on=false);mount{EstimateRatesPage{}};assertFalse(LdApp.settings.value.estimateRates);rule.onAllNodesWithText("Calculator rate adjustment",useUnmergedTree=true).onLast().performClick();await{LdApp.settings.value.estimateRates};assertFalse(LdApp.settings.value.estimateScale||LdApp.settings.value.estimateSplit||LdApp.settings.value.liveCallDesk)}
 @Test fun liveDeskCalculatorPreservesFixedControls(){ratesSeed();show();runBlocking{ctx.repo.set(K.estimateRates,true)};await{LdApp.settings.value.estimateRates};open();click("call_estimate_open");await{rule.onAllNodesWithTag("estimate_rates",true).fetchSemanticsNodes().size==1};tap("estimate_rates");preview();move("rates_total");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();saveShot(rule,"RATES466-desk-preview-day")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class EstimateRatesPixelsUiTest:RatesFixture(){
 @Test fun nightHindiRateTotalAndConfirmation(){source.value=sample.copy(lines=listOf(CallEstimate.line("घर के सामान के लिए चावल के बड़े बैग","2.5","120.00","a"),CallEstimate.line("Cooking oil","3","50.00","b")),discount="10",tax="5");ratesShow(night=true);preview();move("rates_rate_a");saveShot(rule,"RATES466-night-rate-large");move("rates_total");saveShot(rule,"RATES466-night-total-large");tap("rates_use");rule.onNodeWithTag("rates_confirm",true).assertIsDisplayed();saveDialogShot("RATES466-night-confirm-large",0xff0d1320)}
 @Test fun nightSettingsOff(){ratesSeed(night=true,on=false);mount{EstimateRatesPage{}};saveShot(rule,"RATES466-settings-off-night-large");assertFalse(LdApp.settings.value.estimateRates)}
}
