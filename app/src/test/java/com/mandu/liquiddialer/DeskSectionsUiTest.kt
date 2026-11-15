package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class DeskSectionsFixture:LiveCallDeskFixture(){
 protected fun layout(on:Boolean,raw:String){runBlocking{ctx.repo.set(K.deskSections,on);ctx.repo.set(K.deskSectionLayout,raw)};await{LdApp.settings.value.let{it.deskSections==on&&it.deskSectionLayout==raw}}}
 protected fun prepared(on:Boolean=true,raw:String="notes:1,marks:1,watch:1",night:Boolean=false){show(night=night);runBlocking{ctx.repo.set(K.deskStopwatch,true);ctx.repo.set(K.callCheckpoints,true)};await{LdApp.settings.value.deskStopwatch&&LdApp.settings.value.callCheckpoints};layout(on,raw);open()}
 protected fun reopen(){click("call_desk_close");click("call_desk_close_confirm");open()}
 protected fun note(text:String){scroll("call_note_draft");rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].performTextInput(text);settle()}
 protected fun pageMove(tag:String){repeat(30){
  val node=rule.onNodeWithTag(tag,true);val b=node.getUnclippedBoundsInRoot();val v=rule.onNodeWithTag("page_content_scroll",true).getUnclippedBoundsInRoot()
  if(b.top>=v.top+16.dp&&b.bottom<=v.bottom-32.dp){settle();node.assertIsDisplayed();return}
  val dy=if(b.top<v.top+16.dp)(b.top-v.top-16.dp).value else (b.bottom-v.bottom+32.dp).value
  rule.onNodeWithTag("page_content_scroll",true).performSemanticsAction(SemanticsActions.ScrollBy){it(0f,dy*rule.density.density)};settle()
 };error("Not visible: $tag")}
 protected fun above(first:String,second:String){assertTrue(rule.onNodeWithTag(first,true).getUnclippedBoundsInRoot().top<rule.onNodeWithTag(second,true).getUnclippedBoundsInRoot().top)}
 @After fun resetLayout(){runBlocking{ctx.repo.set(K.deskSections,false);ctx.repo.set(K.deskSectionLayout,DeskSections.DEFAULT);ctx.repo.set(K.deskStopwatch,false);ctx.repo.set(K.callCheckpoints,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskSectionsUiTest:DeskSectionsFixture(){
 @Test fun offOriginalDespiteCustom(){prepared(false,"notes:1,marks:0,watch:0");rule.onNodeWithTag("desk_watch_toggle",true).assertExists();rule.onNodeWithTag("checkpoints_toggle",true).assertExists();above("desk_section_watch","desk_section_marks");above("desk_section_marks","desk_section_notes")}
 @Test fun customNotesFirstControlsUnchanged(){prepared();above("desk_section_notes","desk_section_marks");above("desk_section_marks","desk_section_watch");rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_number",true).assertTextEquals("+919876543210");saveShot(rule,"SECTIONS465-notes-first-day")}
 @Test fun hiddenOptionalEntriesKeepNotes(){prepared(raw="notes:1,watch:0,marks:0");rule.onAllNodesWithTag("desk_watch_toggle",true).assertCountEquals(0);rule.onAllNodesWithTag("checkpoints_toggle",true).assertCountEquals(0);rule.onNodeWithTag("call_note_draft",true).assertExists();assertTrue(LdApp.settings.value.deskStopwatch&&LdApp.settings.value.callCheckpoints);rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun editWaitsForReopenPreservesDraft(){prepared(raw=DeskSections.DEFAULT);note("Keep until close");layout(true,"notes:1,watch:0,marks:0");rule.onNodeWithTag("desk_watch_toggle",true).assertExists();rule.onNodeWithTag("checkpoints_toggle",true).assertExists();rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].assertTextContains("Keep until close");reopen();rule.onAllNodesWithTag("desk_watch_toggle",true).assertCountEquals(0);rule.onAllNodesWithTag("checkpoints_toggle",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun masterOffNextOpeningRestoresOriginal(){prepared(raw="notes:1,watch:0,marks:0");layout(false,"notes:1,watch:0,marks:0");rule.onAllNodesWithTag("desk_watch_toggle",true).assertCountEquals(0);reopen();rule.onNodeWithTag("desk_watch_toggle",true).assertExists();rule.onNodeWithTag("checkpoints_toggle",true).assertExists();above("desk_section_watch","desk_section_notes");assertEquals("notes:1,watch:0,marks:0",LdApp.settings.value.deskSectionLayout)}
 @Test fun featureOffStillImmediate(){prepared();runBlocking{ctx.repo.set(K.deskStopwatch,false);ctx.repo.set(K.callCheckpoints,false)};await{rule.onAllNodesWithTag("desk_watch_toggle",true).fetchSemanticsNodes().isEmpty()&&rule.onAllNodesWithTag("checkpoints_toggle",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("call_note_draft",true).assertExists();rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun explicitNoteSaveStillExact(){prepared(raw="notes:1,watch:0,marks:0");note("Delivery order १२");scroll("call_note_save");click("call_note_save");assertEquals("Delivery order १२",Repo.notesFor(ctx,"+919876543210").single().text);assertTrue(Repo.notesFor(ctx,"+449876543210").isEmpty());reopen();assertEquals(1,Repo.notesFor(ctx,"+919876543210").size)}
 @Test fun endConfirmationNotBypassed(){prepared();click("end_call");rule.onNodeWithTag("end_confirm",true).assertExists();click("end_confirm_cancel");rule.onNodeWithTag("call_desk",true).assertExists()}
 @Test fun callChangeClosesCustomDesk(){prepared();com.mandu.liquiddialer.telecom.CallManager.calls.value=listOf(deskShadowCall());settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskSectionsSettingsUiTest:DeskSectionsFixture(){
 private fun settings(){seed(false);layout(false,DeskSections.DEFAULT);mount{DeskSectionsPage{}}}
 @Test fun reorderSavesWithoutEnabling(){settings();pageMove("desk_layout_up_marks");click("desk_layout_up_marks");await{LdApp.settings.value.deskSectionLayout=="marks:1,watch:1,notes:1"};assertFalse(LdApp.settings.value.deskSections||LdApp.settings.value.liveCallDesk);pageMove("desk_layout_preview_watch");above("desk_layout_preview_watch","desk_layout_preview_marks")}
 @Test fun masterToggleDoesNotEnableTools(){settings();rule.onAllNodesWithText("Desk section layout",useUnmergedTree=true)[1].performClick();await{LdApp.settings.value.deskSections};assertFalse(LdApp.settings.value.liveCallDesk||LdApp.settings.value.deskStopwatch||LdApp.settings.value.callCheckpoints)}
 @Test fun hideSavedOnlyPreviewOffOriginal(){settings();pageMove("desk_layout_down_watch");rule.onAllNodes(hasText("Show desk entry") and hasAnyAncestor(hasTestTag("desk_layout_row_watch")),true)[0].performClick();await{LdApp.settings.value.deskSectionLayout=="watch:0,marks:1,notes:1"};pageMove("desk_layout_preview_watch");rule.onNodeWithTag("desk_layout_preview_watch",true).assertExists();assertFalse(LdApp.settings.value.deskStopwatch)}
 @Test fun resetCancelThenConfirmKeepsMaster(){settings();layout(true,"notes:1,watch:0,marks:0");pageMove("desk_layout_reset");click("desk_layout_reset");click("desk_layout_reset_cancel");assertEquals("notes:1,watch:0,marks:0",LdApp.settings.value.deskSectionLayout);click("desk_layout_reset");click("desk_layout_reset_confirm");await{LdApp.settings.value.deskSectionLayout==DeskSections.DEFAULT};assertTrue(LdApp.settings.value.deskSections)}
 @Test fun boundaryButtonsDisabled(){settings();rule.onNodeWithTag("desk_layout_up_watch",true).assertIsNotEnabled();pageMove("desk_layout_down_notes");rule.onNodeWithTag("desk_layout_down_notes",true).assertIsNotEnabled()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskSectionsPixelsUiTest:DeskSectionsFixture(){
 @Test fun narrowNightNotesFirst(){prepared(raw="notes:1,watch:0,marks:0",night=true);rule.onNodeWithTag("call_desk_name",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_timer",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"SECTIONS465-desk-night-large")}
 @Test fun narrowNightEditorAndPreview(){seed(false,night=true);layout(false,DeskSections.DEFAULT);mount{DeskSectionsPage{}};saveShot(rule,"SECTIONS465-settings-off-night-large");pageMove("desk_layout_up_marks");rule.onNodeWithTag("desk_layout_up_marks",true).assertIsDisplayed();saveShot(rule,"SECTIONS465-editor-night-large");layout(true,"notes:1,watch:0,marks:1");pageMove("desk_layout_reset");rule.onNodeWithTag("desk_layout_preview_notes",true).assertExists();rule.onAllNodesWithTag("desk_layout_preview_watch",true).assertCountEquals(0);saveShot(rule,"SECTIONS465-preview-night-large")}
}
