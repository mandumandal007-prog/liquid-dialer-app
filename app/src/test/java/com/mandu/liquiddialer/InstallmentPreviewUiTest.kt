package com.mandu.liquiddialer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class InstallmentFixture:LiveCallDeskFixture(){
 protected val number="+919876543210"
 protected val sample=CallEstimate.Draft(lines=listOf(CallEstimate.line("Rice bags","1","10.00","a")))
 protected val source=mutableStateOf(sample);protected val identity=mutableStateOf(number)
 protected fun move(tag:String){repeat(35){val node=rule.onNodeWithTag(tag,true);val b=node.getUnclippedBoundsInRoot();val desk=rule.onAllNodesWithTag("call_desk_tools",true).fetchSemanticsNodes().isNotEmpty();val margin=if(desk)8.dp else 0.dp;val sc=if(desk)rule.onNodeWithTag("call_desk_tools",true)else rule.onAllNodes(hasScrollAction(),true)[0];val v=sc.getUnclippedBoundsInRoot();if(b.top>=v.top+margin&&b.bottom<=v.bottom-margin){settle();node.assertIsDisplayed();return};val dy=if(b.top<v.top+margin)(b.top-v.top-margin).value else(b.bottom-v.bottom+margin).value;sc.performSemanticsAction(SemanticsActions.ScrollBy){it(0f,dy*rule.density.density)};settle()};error("Not visible $tag")}
 protected fun hit(tag:String){move(tag);click(tag)}
 protected fun input(tag:String,text:String){move(tag);rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(text);settle();rule.runOnUiThread{rule.activity.currentFocus?.clearFocus()};settle()}

 protected fun ready(night:Boolean=false,on:Boolean=true){seed(night=night);android.util.AtomicFile(java.io.File(ctx.filesDir,"call_estimates.json")).delete();CallEstimate.save(ctx,number,0,sample);runBlocking{ctx.repo.set(K.installmentPreview,on)};await{LdApp.settings.value.installmentPreview==on}}
 protected fun direct(){ready();mount{InstallmentPreviewWorkspace(identity.value,source.value,{})}}
 protected fun calc(){input("installment_first","2026-11-16");hit("installment_preview")}
 protected fun integrated(){ready();mount{CallEstimateWorkspace(number,{})};await{rule.onAllNodesWithTag("estimate_total",true).fetchSemanticsNodes().size==1}}
 protected fun live(night:Boolean=false){ready(night);show(night=night);runBlocking{ctx.repo.set(K.installmentPreview,true)};await{LdApp.settings.value.installmentPreview};open();hit("call_estimate_open");await{rule.onAllNodesWithTag("estimate_installments",true).fetchSemanticsNodes().size==1};hit("estimate_installments")}
 @After fun cleanupInstallment(){runBlocking{ctx.repo.set(K.installmentPreview,false)};android.util.AtomicFile(java.io.File(ctx.filesDir,"call_estimates.json")).delete()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstallmentPreviewUiTest:InstallmentFixture(){
 @Test fun explicitPreviewReadOnly(){direct();val bytes=java.io.File(ctx.filesDir,"call_estimates.json").readBytes();rule.onNodeWithTag("installment_preview",true).assertIsNotEnabled();calc();rule.onNodeWithTag("installment_amount_1",true).assertTextEquals("3.34");rule.onNodeWithTag("installment_value",true).assertTextEquals("3 dated parts");assertArrayEquals(bytes,java.io.File(ctx.filesDir,"call_estimates.json").readBytes());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun everyEditInvalidates(){direct();calc();input("installment_upfront","1");rule.onNodeWithTag("installment_result",true).assertDoesNotExist();hit("installment_preview");rule.onNodeWithTag("installment_amount_1",true).assertTextEquals("3.00");input("installment_interval","7");rule.onNodeWithTag("installment_result",true).assertDoesNotExist()}
 @Test fun invalidRefusesNoStale(){direct();calc();input("installment_count","0");hit("installment_preview");rule.onNodeWithTag("installment_error",true).assertExists();rule.onNodeWithTag("installment_result",true).assertDoesNotExist()}
 @Test fun clearResets(){direct();calc();hit("installment_clear");rule.onNodeWithTag("installment_result",true).assertDoesNotExist();rule.onNodeWithTag("installment_preview",true).assertIsNotEnabled()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstallmentPreviewSafetyUiTest:InstallmentFixture(){
 @Test fun sourceChangeResets(){direct();calc();rule.runOnUiThread{source.value=sample.copy(discount="1")};settle();rule.onNodeWithTag("installment_result",true).assertDoesNotExist();rule.onNodeWithTag("installment_preview",true).assertIsNotEnabled()}
 @Test fun numberChangeResets(){direct();calc();rule.runOnUiThread{identity.value="+449876543210"};settle();rule.onNodeWithTag("installment_result",true).assertDoesNotExist()}
 @Test fun directOffClears(){direct();calc();runBlocking{ctx.repo.set(K.installmentPreview,false)};await{rule.onAllNodesWithTag("installment_result",true).fetchSemanticsNodes().isEmpty()};runBlocking{ctx.repo.set(K.installmentPreview,true)};await{rule.onAllNodesWithTag("installment_preview",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("installment_preview",true).assertIsNotEnabled()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstallmentPreviewIntegrationUiTest:InstallmentFixture(){
 @Test fun returnPreservesUnsavedEstimate(){integrated();input("estimate_discount","1");hit("estimate_installments");calc();hit("installment_back");rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 9.00");rule.onNodeWithTag("estimate_status",true).assertTextEquals("Unsaved changes");assertEquals("0",CallEstimate.read(ctx,number).discount);hit("estimate_installments");rule.onNodeWithTag("installment_preview",true).assertIsNotEnabled()}
 @Test fun offReturnsOriginalNoEntry(){integrated();input("estimate_discount","1");hit("estimate_installments");calc();runBlocking{ctx.repo.set(K.installmentPreview,false)};await{rule.onAllNodesWithTag("estimate_total",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("estimate_total",true).assertTextEquals("Total: 9.00");rule.onNodeWithTag("estimate_installments",true).assertDoesNotExist()}
 @Test fun liveDayPreviewPixels(){live();calc();move("installment_result");saveShot(rule,"INSTALL470-day-summary");move("installment_row_1");saveShot(rule,"INSTALL470-day-part");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,number).isEmpty())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class InstallmentPreviewPixelsUiTest:InstallmentFixture(){
 @Test fun nightResultAndInvalidPixels(){live(true);calc();move("installment_sum");saveShot(rule,"INSTALL470-night-summary");move("installment_row_1");saveShot(rule,"INSTALL470-night-part");input("installment_first","2199-12-31");hit("installment_preview");move("installment_error");saveShot(rule,"INSTALL470-night-error");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOffPixels(){ready(true,false);mount{InstallmentPreviewPage{}};saveShot(rule,"INSTALL470-settings-off")}
}
