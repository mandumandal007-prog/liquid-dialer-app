package com.mandu.liquiddialer
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
class NotesWorkspaceUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle(){rule.mainClock.advanceTimeBy(700);rule.waitForIdle()}
    private fun setup(night:Boolean=false){
        rule.mainClock.autoAdvance=false;settle()
        runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight" else "daylight";it[K.fontScale]=if(night)1.5f else 1f}}
        rule.waitUntil(15000){LdApp.settings.value.theme==if(night)"midnight" else "daylight"}
        if(night)rule.runOnUiThread{val c=android.content.res.Configuration(rule.activity.resources.configuration);c.fontScale=1.5f;rule.activity.resources.updateConfiguration(c,rule.activity.resources.displayMetrics)}
        android.util.AtomicFile(File(ctx.filesDir,"notes.json")).delete()
    }
    private fun open(page:String="notes"){rule.runOnUiThread{rule.activity.setContent{LdThemeHost{Page(page,rule.activity.state,rule.activity,{}, {},null,"",{})}}};settle()}
    @Test fun completeLongNoteOpensAndExplicitEditSavesWithoutChangingOtherNotes(){
        setup();val long=(1..24).joinToString("\n"){"Line $it: Meeting ki taiyari और यादें 💚"}+"\nFINAL LINE"
        Repo.addNote(ctx,"+12025550123","Riya",long);Repo.addNote(ctx,"+442025550123","Work","Do not change")
        val selected=Repo.notes(ctx).first{it.name=="Riya"};open("notes:+12025550123")
        rule.onNodeWithTag("note_open_${selected.id}").performScrollTo().performClick();settle()
        rule.onNodeWithTag("note_full_text",useUnmergedTree=true).assertTextEquals(long);saveShot(rule,"276-note-full-day")
        rule.onNodeWithTag("note_edit").performScrollTo().performClick();settle();
        rule.onNodeWithTag("note_edit_text").performTextReplacement("नई meeting\nBring report 💚");
        assertEquals(long,Repo.notes(ctx).first{it.id==selected.id}.text)
        rule.onNodeWithTag("note_edit_save").performScrollTo().performClick();settle();
        assertEquals("नई meeting\nBring report 💚",Repo.notes(ctx).first{it.id==selected.id}.text)
        assertEquals("Do not change",Repo.notes(ctx).first{it.name=="Work"}.text)
    }
    @Test fun largeFontEditorDiscardCanReturnToEditAndNeverImplicitlySave(){
        setup(true);Repo.addNote(ctx,"123","Papa","दवाई कल लाना 💚\nDoctor ko report dikhana")
        val before=Repo.notes(ctx);open();rule.onNodeWithTag("note_open_${before.single().id}").performScrollTo().performClick();settle()
        rule.onNodeWithTag("note_edit").performScrollTo().performClick();settle();
        rule.onNodeWithTag("note_edit_text").performTextReplacement("Unsaved change\nदवाई लाना 💚")
        rule.onNodeWithTag("note_edit_save").performScrollTo();settle();saveShot(rule,"276-note-editor-night-large")
        rule.onNodeWithTag("page_back",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithTag("note_discard_cancel").assertIsDisplayed().performClick();settle()
        rule.onNodeWithTag("note_edit_text").assertTextContains("Unsaved change",substring=true)
        rule.onNodeWithTag("page_back",useUnmergedTree=true).performClick();settle();saveDialogShot("276-note-discard-night-large",0xFF101725)
        rule.onNodeWithTag("note_discard_confirm").performClick();settle();assertEquals(before,Repo.notes(ctx))
    }
    @Test fun unreadableFileShowsHonestErrorBlocksWritesAndRetryRecovers(){
        setup();val f=File(ctx.filesDir,"notes.json");f.writeText("[{broken");open("notes:123")
        rule.onNodeWithTag("notes_load_error",useUnmergedTree=true).assertIsDisplayed();rule.onNodeWithText("Koi note nahi").assertDoesNotExist()
        saveShot(rule,"276-notes-safe-error-day");assertEquals("[{broken",f.readText())
        f.writeText("""[{"id":7,"number":"123","name":"Papa","text":"Recovered note","time":1}]""")
        rule.onNodeWithTag("notes_retry",useUnmergedTree=true).performClick();settle();rule.onNodeWithText("Recovered note").assertExists()
        rule.onNodeWithTag("notes_load_error",useUnmergedTree=true).assertDoesNotExist()
    }
    @Test(timeout=90000) fun largeCollectionPagesStayBoundedAndSearchFindsBeyondVisibleBatch(){
        setup();repeat(24){Repo.addNote(ctx,"123","Papa","Memo $it")};val all=Repo.notes(ctx);open();
        rule.onAllNodes(hasTestTag("note_open_${all[20].id}")).assertCountEquals(0)
        rule.onNodeWithTag("notes_more").performClick();settle();
        rule.onNodeWithTag("note_open_${all[20].id}").assertExists()
        rule.onNodeWithTag("note_open_${all[0].id}").assertDoesNotExist()
        rule.onNodeWithTag("notes_page_count",useUnmergedTree=true).assertTextEquals("21-24 / 24 notes");saveShot(rule,"276-notes-paging-day")
        rule.onNodeWithTag("notes_previous").performClick();settle()
        rule.onNodeWithTag("note_open_${all[0].id}").assertExists()
        rule.onNodeWithTag("note_open_${all[20].id}").assertDoesNotExist();
        rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("notes_search")),useUnmergedTree=true).performScrollTo().performTextInput("Memo 0")
        settle();rule.onNode(hasText("Memo 0") and !hasSetTextAction(),useUnmergedTree=true).assertExists();assertEquals(all,Repo.notes(ctx))
    }
}
