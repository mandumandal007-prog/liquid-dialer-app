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
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class NoteFindUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun setup(night:Boolean=false,long:Boolean=false):List<Note>{
  settle();runBlocking{app.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight" else "daylight";it[K.fontScale]=if(night)1.3f else 1f;it[K.motion]=false}}
  rule.waitUntil(15000){LdApp.settings.value.theme==if(night)"midnight" else "daylight"}
  android.util.AtomicFile(File(app.filesDir,"notes.json")).delete()
  val text="Report [ok] ready. कल फोन करना\n"+(1..if(long)32 else 2).joinToString("\n"){"Line $it: meeting details aur taiyari"}+"\nReport [ok] final. कल फोन करना"
  Repo.addNote(app,"+12025550123","Riya",text);val saved=Repo.notesChecked(app)
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{NoteDetailPage(saved.single(),{}, {})}}};settle();return saved
 }
 @Test fun literalFindWrapClearAndNoMatchNeverChangeSavedNote(){
  val before=setup()
  rule.onNodeWithTag("note_find_query").performScrollTo().performTextInput("[ok]");settle()
  rule.onNodeWithTag("note_find_count",useUnmergedTree=true).assertTextEquals("1 / 2 matches")
  saveShot(rule,"NOTE352-find-day")
  rule.onNodeWithTag("note_find_next").performClick();settle()
  rule.onNodeWithTag("note_find_count",useUnmergedTree=true).assertTextEquals("2 / 2 matches")
  rule.onNodeWithTag("note_find_next").performClick();settle()
  rule.onNodeWithTag("note_find_count",useUnmergedTree=true).assertTextEquals("1 / 2 matches")
  rule.onNodeWithTag("note_find_query").performScrollTo().performTextReplacement("missing text");settle()
  rule.onNodeWithTag("note_find_count",useUnmergedTree=true).assertTextEquals("Koi match nahi")
  rule.onNodeWithTag("note_find_next").assertIsNotEnabled()
  rule.onNodeWithTag("note_find_clear").performClick();settle()
  assertEquals("",rule.onNodeWithTag("note_find_query").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
  rule.onNodeWithTag("note_full_text",useUnmergedTree=true).assertTextEquals(before.single().text)
  assertEquals(before,Repo.notesChecked(app))
 }
 @Test fun nextMatchScrollsIntoViewInLongNote(){
  val before=setup(long=true)
  rule.onNodeWithTag("note_find_query").performScrollTo().performTextInput("[ok]");settle()
  rule.onNodeWithTag("note_find_next").performClick();settle()
  val top=rule.onNodeWithTag("note_full_text",useUnmergedTree=true).getUnclippedBoundsInRoot().top
  assertTrue("next brings lower occurrence into view",top.value<0f)
  rule.onNodeWithTag("note_find_next").assertIsDisplayed()
  rule.onNodeWithTag("note_find_prev").assertIsDisplayed()
  rule.onNodeWithTag("note_find_count",useUnmergedTree=true).assertTextEquals("2 / 2 matches")
  saveShot(rule,"NOTE352-jump-final")
  rule.onNodeWithTag("note_find_next").performClick();settle()
  rule.onNodeWithTag("note_find_count",useUnmergedTree=true).assertTextEquals("1 / 2 matches")
  rule.onNodeWithTag("note_find_prev").performClick();settle()
  rule.onNodeWithTag("note_find_count",useUnmergedTree=true).assertTextEquals("2 / 2 matches")
  rule.onNodeWithTag("note_find_next").assertIsDisplayed()
  assertEquals(before,Repo.notesChecked(app))
 }
 @Test fun hindiAtLargeFontAndExistingEditSaveRemainExplicit(){
  val before=setup(true)
  rule.onNodeWithTag("note_find_query").performScrollTo().performTextInput("कल फोन");settle()
  rule.onNodeWithTag("note_find_count",useUnmergedTree=true).assertTextEquals("1 / 2 matches")
  saveShot(rule,"NOTE352-find-night-large")
  rule.onNodeWithTag("note_find_prev").performClick();settle()
  rule.onNodeWithTag("note_find_count",useUnmergedTree=true).assertTextEquals("2 / 2 matches")
  assertEquals(before,Repo.notesChecked(app))
  rule.onNodeWithTag("note_edit").performScrollTo().performClick();settle()
  rule.onNodeWithTag("note_find_query").assertDoesNotExist()
  rule.onNodeWithTag("note_edit_text").performTextReplacement("Saved only explicitly")
  assertEquals(before,Repo.notesChecked(app))
  rule.onNodeWithTag("note_edit_save").performScrollTo().performClick();settle()
  val saved=Repo.notesChecked(app).single();assertEquals("Saved only explicitly",saved.text)
  assertEquals(before.single().number,saved.number);assertEquals(before.single().time,saved.time)
 }
}
