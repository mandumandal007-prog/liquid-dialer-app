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
import java.io.File
abstract class CheckpointTasksFixture:CheckpointFixture(){
 protected val number="+919876543210"
 protected fun setup(on:Boolean=true,night:Boolean=false,label:String="Confirm delivery"){File(ctx.filesDir,"call_checklists.json").delete();ready(night=night);runBlocking{ctx.repo.set(K.checkpointTasks,on)};await{LdApp.settings.value.checkpointTasks==on};input(label);mark()}
 protected fun choose(){move("checkpoint_tasks_open");click("checkpoint_tasks_open");move("checkpoint_task_0");click("checkpoint_task_0")}
 protected fun preview(){move("checkpoint_tasks_preview");click("checkpoint_tasks_preview");await{rule.onAllNodesWithTag("checkpoint_tasks_add",true).fetchSemanticsNodes().isNotEmpty()}}
 @After fun resetTasks(){File(ctx.filesDir,"call_checklists.json").delete();runBlocking{ctx.repo.set(K.checkpointTasks,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CheckpointTasksUiTest:CheckpointTasksFixture(){
 @Test fun offNoEntryMarksUnaffected(){setup(false);rule.onAllNodesWithTag("checkpoint_tasks_open",true).assertCountEquals(0);rule.onNodeWithTag("checkpoint_row_0",true).assertExists();assertTrue(CallChecklist.read(ctx,number).isEmpty())}
 @Test fun noPreselectionAndCancelNoWrite(){setup();move("checkpoint_tasks_open");click("checkpoint_tasks_open");rule.onNodeWithTag("checkpoint_task_0",true).assertIsOff();rule.onNodeWithTag("checkpoint_tasks_preview",true).assertIsNotEnabled();move("checkpoint_tasks_cancel");click("checkpoint_tasks_cancel");assertTrue(CallChecklist.read(ctx,number).isEmpty())}
 @Test fun explicitPreviewThenExactAdd(){setup();CallChecklist.add(ctx,number,"Keep");val old=CallChecklist.read(ctx,number);choose();preview();rule.onNodeWithTag("checkpoint_tasks_number",true).assertTextEquals(number);rule.onNodeWithTag("checkpoint_task_preview_0",true).assertTextEquals("1. Confirm delivery");assertEquals(old,CallChecklist.read(ctx,number));move("checkpoint_tasks_add");click("checkpoint_tasks_add");await{CallChecklist.read(ctx,number).size==2};assertEquals(old,CallChecklist.read(ctx,number).take(1));assertFalse(CallChecklist.read(ctx,number).last().done);assertTrue(Repo.notesFor(ctx,number).isEmpty());rule.onAllNodesWithTag("checkpoint_tasks_add",true).assertCountEquals(0);move("checkpoint_tasks_status");saveShot(rule,"TASK460-added-day");move("checkpoints_toggle");click("checkpoints_toggle");move("call_checklist_open");click("call_checklist_open");rule.onNodeWithText("Confirm delivery",useUnmergedTree=true).assertExists();rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun duplicateNotEnabled(){setup();CallChecklist.add(ctx,number,"confirm   DELIVERY");choose();preview();rule.onNodeWithTag("checkpoint_tasks_count",true).assertTextEquals("0 new · 1 duplicate skipped");rule.onNodeWithTag("checkpoint_tasks_add",true).assertIsNotEnabled();assertEquals(1,CallChecklist.read(ctx,number).size)}
 @Test fun changeSelectionDoesNotAdd(){setup();choose();preview();move("checkpoint_tasks_change");click("checkpoint_tasks_change");rule.onNodeWithTag("checkpoint_task_0",true).assertIsOn();click("checkpoint_task_0");rule.onNodeWithTag("checkpoint_tasks_preview",true).assertIsNotEnabled();assertTrue(CallChecklist.read(ctx,number).isEmpty())}
 @Test fun staleListRefusesAndKeepsOtherWrite(){setup();choose();preview();CallChecklist.add(ctx,number,"Elsewhere");move("checkpoint_tasks_add");click("checkpoint_tasks_add");assertEquals(listOf("Elsewhere"),CallChecklist.read(ctx,number).map{it.text});rule.onNodeWithTag("checkpoint_tasks_status",true).assertTextContains("Not added.",substring=true)}
 @Test fun offDiscardsSelectionReenableEmpty(){setup();choose();runBlocking{ctx.repo.set(K.checkpointTasks,false)};await{rule.onAllNodesWithTag("checkpoint_tasks_open",true).fetchSemanticsNodes().isEmpty()};runBlocking{ctx.repo.set(K.checkpointTasks,true)};await{rule.onAllNodesWithTag("checkpoint_tasks_open",true).fetchSemanticsNodes().size==1};move("checkpoint_tasks_open");click("checkpoint_tasks_open");rule.onNodeWithTag("checkpoint_task_0",true).assertIsOff();assertTrue(CallChecklist.read(ctx,number).isEmpty())}
 @Test fun hiddenChecklistHidesEntry(){setup();runBlocking{ctx.repo.set(K.hiddenFeatures,setOf("checklist"))};await{rule.onAllNodesWithTag("checkpoint_tasks_open",true).fetchSemanticsNodes().isEmpty()};assertTrue(CallChecklist.read(ctx,number).isEmpty())}
 @Test fun changedBoardDiscardsPreview(){setup();choose();preview();move("checkpoint_remove_0");click("checkpoint_remove_0");rule.onAllNodesWithTag("checkpoint_tasks_add",true).assertCountEquals(0);assertTrue(CallChecklist.read(ctx,number).isEmpty())}
 @Test fun callChangeCannotAdd(){setup();choose();preview();com.mandu.liquiddialer.telecom.CallManager.calls.value=listOf(deskShadowCall());settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);assertTrue(CallChecklist.read(ctx,number).isEmpty())}
 @Test fun selectionDayPixels(){setup();choose();move("checkpoint_tasks_preview");saveShot(rule,"TASK460-selection-day")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CheckpointTasksPixelsUiTest:CheckpointTasksFixture(){
 @Test fun narrowLargePreviewKeepsCallControls(){setup(night=true,label="ऑर्डर तैयार करना: Friday delivery");choose();preview();move("checkpoint_task_preview_0");saveShot(rule,"TASK460-preview-night-large");move("checkpoint_tasks_add");rule.onNodeWithTag("checkpoint_tasks_add",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"TASK460-add-night-large")}
 @Test fun settingOffDoesNotEnableDependencies(){seed(false,night=true);runBlocking{ctx.repo.set(K.checkpointTasks,false);ctx.repo.set(K.callCheckpoints,false)};await{!LdApp.settings.value.checkpointTasks&&!LdApp.settings.value.callCheckpoints};mount{CheckpointTasksPage{}};saveShot(rule,"TASK460-settings-night-large");rule.onAllNodesWithText("Checkpoint to checklist",useUnmergedTree=true)[1].performClick();await{LdApp.settings.value.checkpointTasks};assertFalse(LdApp.settings.value.liveCallDesk);assertFalse(LdApp.settings.value.callCheckpoints)}
}
