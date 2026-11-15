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
abstract class LabelEditorFixture:CheckpointFixture(){
 protected fun labelReady(night:Boolean=false,on:Boolean=true){ready(night=night);runBlocking{ctx.repo.set(K.checkpointLabels,on)};await{LdApp.settings.value.checkpointLabels==on};mark();move("checkpoint_row_0")}
 protected fun edit(index:Int=0){move("checkpoint_edit_$index");click("checkpoint_edit_$index");await{rule.onAllNodesWithTag("checkpoint_edit_apply",true).fetchSemanticsNodes().isNotEmpty()}}
 protected fun replaceLabel(text:String){move("checkpoint_edit_label");rule.onNodeWithTag("checkpoint_edit_label",true).onChildren().filter(hasSetTextAction())[0].performTextReplacement(text);settle()}
 protected fun rowLabel(text:String){rule.onNodeWithTag("checkpoint_row_0",true).onChildren().filter(hasText(text)).assertCountEquals(1)}
 @After fun labelClean(){runBlocking{ctx.repo.set(K.checkpointLabels,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CheckpointLabelsUiTest:LabelEditorFixture(){
 @Test fun offNoEditorMarkStillWorks(){labelReady(on=false);rule.onAllNodesWithTag("checkpoint_edit_0",true).assertCountEquals(0);rowLabel("Checkpoint");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun cancelKeepsOriginalAndTime(){labelReady();edit();val time=rule.onNodeWithTag("checkpoint_edit_time",true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text];replaceLabel("Not applied");move("checkpoint_edit_cancel");click("checkpoint_edit_cancel");rowLabel("Checkpoint");edit();rule.onNodeWithTag("checkpoint_edit_time",true).assertTextEquals(time.single().text);rule.onNodeWithTag("checkpoint_edit_label",true).onChildren().filter(hasSetTextAction())[0].assertTextEquals("Checkpoint");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun applyOnlySessionAndPreviewExact(){labelReady();edit();rule.onNodeWithTag("checkpoint_edit_number",true).assertTextEquals("+919876543210");val time=rule.onNodeWithTag("checkpoint_edit_time",true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text;replaceLabel("Reference 7834");move("checkpoint_edit_apply");click("checkpoint_edit_apply");rowLabel("Reference 7834");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty());edit();rule.onNodeWithTag("checkpoint_edit_time",true).assertTextEquals(time);move("checkpoint_edit_cancel");click("checkpoint_edit_cancel");move("checkpoint_preview");click("checkpoint_preview");await{rule.onAllNodesWithTag("checkpoint_save_confirm",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("checkpoint_preview_text",true).assertTextContains("Reference 7834",substring=true);click("checkpoint_save_confirm");assertTrue(Repo.notesFor(ctx,"+919876543210").single().text.contains("Reference 7834"));rule.onAllNodesWithTag("checkpoint_edit_0",true).assertCountEquals(0)}
 @Test fun blankAppliedDefaults(){labelReady();edit();replaceLabel("  ");move("checkpoint_edit_apply");click("checkpoint_edit_apply");rowLabel("Checkpoint")}
 @Test fun invalidLabelDisabled(){labelReady();edit();replaceLabel("x".repeat(81));rule.onNodeWithTag("checkpoint_edit_apply",true).assertIsNotEnabled();replaceLabel("Valid");rule.onNodeWithTag("checkpoint_edit_apply",true).assertIsEnabled()}
 @Test fun offDuringEditDiscardsDraftNotMarks(){labelReady();edit();replaceLabel("Lost draft");runBlocking{ctx.repo.set(K.checkpointLabels,false)};await{rule.onAllNodesWithTag("checkpoint_edit_apply",true).fetchSemanticsNodes().isEmpty()};rowLabel("Checkpoint");runBlocking{ctx.repo.set(K.checkpointLabels,true)};await{rule.onAllNodesWithTag("checkpoint_edit_0",true).fetchSemanticsNodes().size==1};edit();rule.onNodeWithTag("checkpoint_edit_label",true).onChildren().filter(hasSetTextAction())[0].assertTextEquals("Checkpoint");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun hidePreservesAppliedLabel(){labelReady();edit();replaceLabel("Keep this");move("checkpoint_edit_apply");click("checkpoint_edit_apply");move("checkpoints_toggle");click("checkpoints_toggle");click("checkpoints_toggle");rowLabel("Keep this");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun editorBlocksBoardChanges(){labelReady();edit();rule.onNodeWithTag("checkpoint_mark",true).assertIsNotEnabled();rule.onNodeWithTag("checkpoint_preview",true).assertIsNotEnabled();rule.onAllNodesWithTag("checkpoint_remove_0",true).assertCountEquals(0);rule.onAllNodesWithTag("checkpoint_new",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun appliedLabelStaysWhenEditorOff(){labelReady();edit();replaceLabel("Applied");move("checkpoint_edit_apply");click("checkpoint_edit_apply");runBlocking{ctx.repo.set(K.checkpointLabels,false)};await{!LdApp.settings.value.checkpointLabels};rowLabel("Applied");rule.onAllNodesWithTag("checkpoint_edit_0",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun callChangeDiscardsEditWithoutSave(){labelReady();edit();replaceLabel("Old call draft");com.mandu.liquiddialer.telecom.CallManager.calls.value=listOf(deskShadowCall());settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);rule.onAllNodesWithTag("checkpoint_edit_apply",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun dayEditorPixels(){labelReady();edit();replaceLabel("Reference 7834");move("checkpoint_edit_apply");saveShot(rule,"LABEL458-editor-day")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CheckpointLabelsPixelsUiTest:LabelEditorFixture(){
 @Test fun narrowLargeEditorAndAppliedBoard(){labelReady(night=true);edit();replaceLabel("ऑर्डर 7834: delivery date");move("checkpoint_edit_apply");rule.onNodeWithTag("checkpoint_edit_apply",true).assertIsDisplayed();saveShot(rule,"LABEL458-editor-night-large");click("checkpoint_edit_apply");move("checkpoint_edit_0");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();saveShot(rule,"LABEL458-board-night-large")}
 @Test fun settingDefaultOffNoAutoEnable(){seed(false,night=true);runBlocking{ctx.repo.set(K.callCheckpoints,false);ctx.repo.set(K.checkpointLabels,false)};await{!LdApp.settings.value.callCheckpoints&&!LdApp.settings.value.checkpointLabels};mount{CheckpointLabelsPage{}};saveShot(rule,"LABEL458-settings-night-large");rule.onAllNodesWithText("Checkpoint label editor",useUnmergedTree=true)[1].performClick();await{LdApp.settings.value.checkpointLabels};assertFalse(LdApp.settings.value.liveCallDesk);assertFalse(LdApp.settings.value.callCheckpoints)}
}
