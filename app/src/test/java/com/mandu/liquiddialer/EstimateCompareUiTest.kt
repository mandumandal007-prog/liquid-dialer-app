package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class EstimateCompareUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private val n="+919876543210"
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(tag:String){rule.waitUntil(15000){settle();rule.onAllNodesWithTag(tag,true).fetchSemanticsNodes().isNotEmpty()}}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;val node=rule.onNodeWithTag(tag,true);node.performScrollTo();rule.mainClock.autoAdvance=false;node.performClick();settle()}
 private fun type(tag:String,text:String){rule.mainClock.autoAdvance=true;val node=rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true);node.performScrollTo();node.performTextReplacement(text);settle()}
 @Before fun seed(){android.util.AtomicFile(File(ctx.filesDir,"call_estimates.json")).delete();CallEstimate.save(ctx,n,0,CallEstimate.Draft(lines=listOf(CallEstimate.line("Rice bags","2","500"),CallEstimate.line("Tea packets","1.5","80")),discount="20",tax="5"))}
 private fun show(night:Boolean=false,sheet:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f}};rule.waitUntil(15000){LdApp.settings.value.theme==(if(night)"midnight"else"daylight")};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{if(sheet)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Papa",{})}else CallEstimatePage(n,{})}}};if(!sheet)await("estimate_compare")}
 @Test fun comparePreviewConfirmThenSaveIsExplicitAndOldRevisionPreserved(){show();val file=File(ctx.filesDir,"call_estimates.json");val before=file.readBytes();val rev=CallEstimate.read(ctx,n).revision;click("estimate_compare");type("compare_factor","2");type("compare_discount","100");type("compare_tax","10");rule.onNodeWithTag("compare_alt_total",true).assertTextEquals("Alternate total: 2354.00");rule.onNodeWithTag("compare_difference",true).assertTextEquals("Zyada: 1199.00");assertArrayEquals(before,file.readBytes());click("compare_apply");rule.onNodeWithTag("compare_cancel_apply",true).performClick();settle();assertArrayEquals(before,file.readBytes());click("compare_apply");rule.onNodeWithTag("compare_confirm",true).performClick();await("estimate_compare");rule.onNodeWithTag("estimate_status",true).assertTextEquals("Unsaved changes");assertEquals(rev,CallEstimate.read(ctx,n).revision);click("estimate_save");rule.waitUntil(15000){CallEstimate.read(ctx,n).revision!=rev};assertEquals("4",CallEstimate.read(ctx,n).lines.first().quantity);assertNull(rule.activity.state.confirmNumber)}
 @Test fun invalidQuantityAndBackDiscardLeaveSavedFileUnchanged(){show();val f=File(ctx.filesDir,"call_estimates.json");val before=f.readBytes();click("estimate_compare");type("compare_factor","0");rule.onNodeWithTag("compare_error",true).assertExists();rule.onNodeWithTag("compare_apply",true).assertDoesNotExist();click("compare_back");rule.onNodeWithTag("compare_keep",true).performClick();settle();type("compare_factor","0.5");click("compare_reset");rule.onNodeWithTag("compare_base_total",true).assertTextEquals("Base total: 1155.00");type("compare_discount","999999");click("compare_back");rule.onNodeWithTag("compare_discard",true).performClick();await("estimate_compare");assertArrayEquals(before,f.readBytes());rule.onNodeWithTag("estimate_status",true).assertTextEquals("Saved draft")}
 @Test fun dayInputAndComparisonPixels(){show();click("estimate_compare");type("compare_factor","2");type("compare_discount","100");type("compare_tax","10");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("compare_factor",true).performScrollTo();settle();saveShot(rule,"CMP394-input-day");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("compare_apply",true).performScrollTo();settle();saveShot(rule,"CMP394-summary-day");click("compare_apply");await("compare_confirm");rule.mainClock.advanceTimeBy(1200);rule.waitForIdle();saveDialogShot("CMP394-confirm-dialog",0xFFF1F3FF)}
 @Test fun nightLargeSummaryAndNoRoundingErrorPixels(){show(true);click("estimate_compare");type("compare_factor","0.5");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("compare_line_0",true).performScrollTo();settle();saveShot(rule,"CMP394-summary-large");type("compare_factor","0.001");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("compare_error",true).performScrollTo();settle();saveShot(rule,"CMP394-error-large");assertEquals("2",CallEstimate.read(ctx,n).lines.first().quantity)}
 @Test fun callTimePathKeepsUnsavedNoteAndAppliesOnlyToCalculatorDraft(){show(sheet=true);rule.onNode(hasSetTextAction()).performTextInput("Unsaved note");click("call_estimate_open");await("estimate_compare");click("estimate_compare");type("compare_factor","2");click("compare_apply");rule.onNodeWithTag("compare_confirm",true).performClick();await("estimate_compare");click("estimate_notes_back");rule.onNodeWithTag("estimate_discard",true).performClick();settle();rule.onNodeWithText("Unsaved note").assertExists();assertEquals("2",CallEstimate.read(ctx,n).lines.first().quantity)}
}
