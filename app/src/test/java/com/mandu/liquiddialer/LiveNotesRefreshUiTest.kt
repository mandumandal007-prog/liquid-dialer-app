package com.mandu.liquiddialer
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
abstract class RefreshFixture:LiveCallDeskFixture(){
 protected val number="+919876543210"
 protected fun setup(on:Boolean=true,night:Boolean=false){show(night=night);runBlocking{ctx.repo.set(K.liveNotesRefresh,on)};await{LdApp.settings.value.liveNotesRefresh==on};open()}
 protected fun draft(text:String){move("call_note_draft");rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].performTextInput(text);settle()}
 protected fun assertDraft(text:String){rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].assertTextEquals(text)}
 protected fun move(tag:String){repeat(30){val r=rule.onNodeWithTag(tag,true).getUnclippedBoundsInRoot();val v=rule.onNodeWithTag("call_desk_tools",true).getUnclippedBoundsInRoot();if(r.top>=v.top&&r.bottom<=v.bottom){settle();rule.onNodeWithTag(tag,true).assertIsDisplayed();return};rule.onNodeWithTag("call_desk_tools",true).performSemanticsAction(SemanticsActions.ScrollBy){it(0f,if(r.top<v.top)-150f else 150f)};settle()};error("Not fully visible $tag")}
 protected fun has(text:String)=rule.onAllNodesWithText(text,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()
 @After fun reset(){runBlocking{ctx.repo.set(K.liveNotesRefresh,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveNotesRefreshUiTest:RefreshFixture(){
 @Test fun offNoEntryAndNoAutoRefresh(){setup(false);Repo.addNote(ctx,number,"","Off snapshot");settle();rule.onAllNodesWithTag("call_notes_reload",true).assertCountEquals(0);assertFalse(has("Off snapshot"));assertEquals(1,Repo.notesForChecked(ctx,number).size)}
 @Test fun addRefreshesPreservesDraft(){setup();draft("Do not lose me");Repo.addNote(ctx,number,"","Saved elsewhere");await{has("Saved elsewhere")};assertDraft("Do not lose me");assertEquals(1,Repo.notesForChecked(ctx,number).size)}
 @Test fun editAndDeleteRefresh(){setup();Repo.addNote(ctx,number,"","Before");await{has("Before")};val n=Repo.notesForChecked(ctx,number).single();Repo.updateNote(ctx,n,"After");await{has("After")&&!has("Before")};Repo.deleteNote(ctx,n.id);await{!has("After")};assertTrue(Repo.notesForChecked(ctx,number).isEmpty())}
 @Test fun otherCountryNeverShown(){setup();Repo.addNote(ctx,"+449876543210","","Foreign note");await{has("Saved notes reloaded. Draft unchanged.")};assertFalse(has("Foreign note"));assertEquals(1,Repo.notesForChecked(ctx,"+449876543210").size)}
 @Test fun reloadReadsUnsignalledDiskChange(){setup();draft("Keep draft");val raw="""[{"id":12,"number":"+919876543210","name":"","text":"Imported externally","time":12}]""";File(ctx.filesDir,"notes.json").writeText(raw);settle();assertFalse(has("Imported externally"));move("call_notes_reload");click("call_notes_reload");await{has("Imported externally")};assertDraft("Keep draft");assertEquals(raw,File(ctx.filesDir,"notes.json").readText())}
 @Test fun corruptReloadHidesStaleAndPreservesFileDraft(){setup();draft("Keep draft");Repo.addNote(ctx,number,"","Old visible");await{has("Old visible")};File(ctx.filesDir,"notes.json").writeText("broken");move("call_notes_reload");click("call_notes_reload");await{has("Saved notes load nahi hue. Purani list hide ki; file aur draft nahi badle.")};assertFalse(has("Old visible"));assertDraft("Keep draft");assertEquals("broken",File(ctx.filesDir,"notes.json").readText())}
 @Test fun offStopsChangesThenReenableCatchesUp(){setup();draft("Keep draft");runBlocking{ctx.repo.set(K.liveNotesRefresh,false)};await{!LdApp.settings.value.liveNotesRefresh};Repo.addNote(ctx,number,"","While off");settle();assertFalse(has("While off"));runBlocking{ctx.repo.set(K.liveNotesRefresh,true)};await{has("While off")};assertDraft("Keep draft")}
 @Test fun normalSaveStillSingleNoteAndClearsItsDraft(){setup();draft("Explicit save");move("call_note_save");click("call_note_save");await{has("Explicit save")};assertDraft("");assertEquals(1,Repo.notesForChecked(ctx,number).size)}
 @Test fun checkpointSaveAppearsWithoutReopen(){setup();runBlocking{ctx.repo.set(K.callCheckpoints,true)};await{LdApp.settings.value.callCheckpoints};move("checkpoints_toggle");click("checkpoints_toggle");move("checkpoint_mark");click("checkpoint_mark");move("checkpoint_preview");click("checkpoint_preview");await{rule.onAllNodesWithTag("checkpoint_save_confirm",true).fetchSemanticsNodes().size==1};click("checkpoint_save_confirm");await{Repo.notesForChecked(ctx,number).size==1};val n=Repo.notesForChecked(ctx,number).single();await{rule.onAllNodesWithTag("call_saved_note_${n.id}",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("call_desk",true).assertExists();assertEquals(1,Repo.notesForChecked(ctx,number).size);runBlocking{ctx.repo.set(K.callCheckpoints,false)}}

 @Test fun dayRefreshedListPixels(){setup();draft("Unfinished call note");Repo.addNote(ctx,number,"","Checkpoint summary saved while this desk stayed open");await{has("Checkpoint summary saved while this desk stayed open")};move("call_notes_reload");rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"REFRESH459-list-day")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveNotesRefreshPixelsUiTest:RefreshFixture(){
 @Test fun narrowNightRefreshKeepsCallControls(){setup(night=true);draft("Draft stays");Repo.addNote(ctx,number,"","ऑर्डर 42: saved summary");await{has("ऑर्डर 42: saved summary")};move("call_notes_reload");rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"REFRESH459-narrow-night-large");val id=Repo.notesForChecked(ctx,number).single().id;move("call_saved_note_$id");rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"REFRESH459-saved-note-night-large");assertDraft("Draft stays")}
 @Test fun settingOffThenOnNoOtherFeatureChange(){seed(false,night=true);runBlocking{ctx.repo.set(K.liveNotesRefresh,false)};await{!LdApp.settings.value.liveNotesRefresh};mount{LiveNotesRefreshPage{}};saveShot(rule,"REFRESH459-settings-night-large");rule.onAllNodesWithText("Live saved notes refresh",useUnmergedTree=true)[1].performClick();await{LdApp.settings.value.liveNotesRefresh};assertFalse(LdApp.settings.value.liveCallDesk)}
}

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveNotesRefreshOrdinaryUiTest:RefreshFixture(){
 @Test fun ordinaryNotesSheetRefreshes(){seed();runBlocking{ctx.repo.set(K.liveNotesRefresh,true)};await{LdApp.settings.value.liveNotesRefresh};mount{androidx.compose.foundation.layout.Column(Modifier){NotesSheet(number,"Support",{})}};Repo.addNote(ctx,number,"","Ordinary notes live");await{has("Ordinary notes live")};rule.onNodeWithTag("call_notes_reload",true).assertExists()}
}
