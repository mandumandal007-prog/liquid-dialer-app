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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NotesSearchUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
    @Test fun searchClearAndNoMatchPreserveNotes() = check("daylight",1f,"day")
    @Test fun contactSearchAtLargeFontNight() = check("midnight",1.3f,"night-large")
    private fun check(theme:String,font:Float,suffix:String) {
        rule.mainClock.autoAdvance=false;settle()
        val ctx=ApplicationProvider.getApplicationContext<LdApp>()
        runBlocking {ctx.ldStore.edit {it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}}
        rule.waitUntil(15000){LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font}
        android.util.AtomicFile(java.io.File(ctx.filesDir,"notes.json")).delete()
        Repo.addNote(ctx,"123","Papa","दवाई कल लाना 💚")
        Repo.addNote(ctx,"123","Papa","Meeting Friday")
        Repo.addNote(ctx,"456","Other","Other contact")
        val before=Repo.notes(ctx)
        rule.runOnUiThread {rule.activity.setContent {LdThemeHost {Page(if(font>1f) "notes:123" else "notes",rule.activity.state,rule.activity,{}, {},null,"",{})}}}
        settle()
        rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("notes_search")),useUnmergedTree=true).performTextInput("दवाई")
        settle()
        rule.onNodeWithText("दवाई कल लाना 💚",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithText("Meeting Friday",useUnmergedTree=true).assertDoesNotExist()
        rule.onNodeWithTag("notes_search_clear").assertIsDisplayed()
        saveShot(rule,"265-notes-search-"+suffix)
        rule.onNodeWithTag("notes_search_clear").performClick();settle()
        rule.onNodeWithText("Meeting Friday",useUnmergedTree=true).assertExists()
        rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("notes_search")),useUnmergedTree=true).performTextInput("zzzz")
        settle()
        rule.onNodeWithText("Koi matching note nahi",useUnmergedTree=true).assertIsDisplayed()
        saveShot(rule,"265-notes-empty-"+suffix)
        assertEquals(before,Repo.notes(ctx))
    }
    @Test fun filteredDeleteNeedsConfirmationAndCancelsSafely() = deleteCheck("daylight",1f,"day")
    @Test fun filteredDeleteDialogLargeNight() = deleteCheck("midnight",1.5f,"night-large")
    private fun deleteCheck(theme:String,font:Float,suffix:String) {
        rule.mainClock.autoAdvance=false;settle()
        val ctx=ApplicationProvider.getApplicationContext<LdApp>()
        runBlocking {ctx.ldStore.edit {it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}}
        rule.waitUntil(15000){LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font}
        android.util.AtomicFile(java.io.File(ctx.filesDir,"notes.json")).delete()
        Repo.addNote(ctx,"123","Papa","दवाई कल लाना 💚")
        Repo.addNote(ctx,"456","Other","Keep this note")
        val before=Repo.notes(ctx);val selected=before.first {it.number=="123"}
        if(font>1f) rule.runOnUiThread {
            val config=android.content.res.Configuration(rule.activity.resources.configuration)
            config.fontScale=font
            rule.activity.resources.updateConfiguration(config,rule.activity.resources.displayMetrics)
        }

        rule.runOnUiThread {rule.activity.setContent {LdThemeHost {Page("notes",rule.activity.state,rule.activity,{}, {},null,"",{})}}}
        settle()
        rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("notes_search")),useUnmergedTree=true).performTextInput("दवाई")
        settle()
        rule.onNodeWithTag("note_delete_${selected.id}").performClick();settle()
        assertEquals(before,Repo.notes(ctx))
        rule.onNodeWithTag("note_delete_confirm").assertIsDisplayed()
        rule.onNodeWithTag("note_delete_cancel").assertIsDisplayed()
        saveDialogShot("267-note-delete-"+suffix, if(theme=="daylight") 0xFFF1F3FF else 0xFF101725)
        rule.onNodeWithTag("note_delete_cancel").performClick();settle()
        assertEquals(before,Repo.notes(ctx))
        rule.onNodeWithTag("note_delete_${selected.id}").performClick();settle()
        rule.onNodeWithTag("note_delete_confirm").performClick();settle()
        assertEquals(listOf(before.first {it.number=="456"}),Repo.notes(ctx))
        rule.onNodeWithText("Koi matching note nahi",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithTag("notes_search_clear").performClick();settle()
        rule.onNodeWithText("Keep this note",useUnmergedTree=true).assertIsDisplayed()
    }

}
