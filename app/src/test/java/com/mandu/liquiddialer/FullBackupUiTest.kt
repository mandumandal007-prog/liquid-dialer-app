package com.mandu.liquiddialer
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class FullBackupUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,useUnmergedTree=true).performScrollTo().assertIsDisplayed().performClick();settle();rule.mainClock.autoAdvance=false}
 private fun await(tag:String){repeat(50){settle();if(rule.onAllNodesWithTag(tag,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty())return;Thread.sleep(30)};error("Missing $tag")}
 private fun deliver(uri:Uri,result:Int=Activity.RESULT_OK){val started=Shadows.shadowOf(rule.activity).nextStartedActivityForResult;assertNotNull(started);rule.runOnUiThread{Shadows.shadowOf(rule.activity).receiveResult(started!!.intent,result,Intent().setData(uri))};settle()}
 @Before fun clean(){
  ctx.getSharedPreferences("call_reminders",0).edit().clear().commit()
  File(ctx.filesDir,"notes.json").delete();File(ctx.filesDir,"notes.json.bak").delete()
  File(ctx.filesDir,"manual_call_lists.json").delete();File(ctx.filesDir,"manual_call_lists.json.bak").delete()
  runBlocking{ctx.ldStore.edit{it.clear()}}
 }
 private fun populate(){
  Repo.addNote(ctx,"+91 98765 43210","Papa","dawa ki list")
  CallReminders.set(ctx,"+91 98765 43210","Papa",System.currentTimeMillis()+3600000)
  val id=CallLists.create(ctx,"Bharosa").last().id
  CallLists.add(ctx,id,"+91 98765 43210","Papa")
  runBlocking{ctx.ldStore.edit{it[K.blockedPrefixes]="+91140"}}
 }
 private fun show(night:Boolean=false){
  rule.mainClock.autoAdvance=false;settle()
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight" else "daylight";it[K.fontScale]=if(night)1.4f else 1f}}
  rule.waitUntil(30000){LdApp.settings.value.theme==if(night)"midnight" else "daylight"}
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{BackupPage({})}}};settle()
  rule.waitUntil(30000){rule.onAllNodesWithTag("backup_export",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()};settle()
 }
 @Test fun exportWritesValidBackupAndCancelKeepsState(){
  populate();show()
  val out=File(ctx.cacheDir,"poora-backup.json")
  click("backup_export");deliver(Uri.fromFile(out));await("backup_notice")
  val plan=FullBackup.parse(out.readText())
  assertEquals(1,plan.notes.size);assertEquals(1,plan.reminders.size);assertEquals(1,plan.lists.size);assertEquals(listOf("+91140"),plan.blockedPrefixes);assertNotNull(plan.settings)
  click("backup_export");deliver(Uri.fromFile(File(ctx.cacheDir,"cancel.json")),Activity.RESULT_CANCELED)
  assertFalse(File(ctx.cacheDir,"cancel.json").exists())
 }
 @Test fun restorePreviewConfirmMergesAndCancelLeavesUntouched(){
  populate();val backupText=FullBackup.export(ctx);clean()
  Repo.addNote(ctx,"+91 9000000000","Local","naya local note")
  show()
  val file=File(ctx.cacheDir,"restore.json");file.writeText(backupText)
  click("backup_restore");deliver(Uri.fromFile(file));await("backup_preview_text")
  rule.onNodeWithTag("backup_restore_cancel",useUnmergedTree=true).performClick();settle()
  assertEquals(1,Repo.notesChecked(ctx).size);assertTrue(CallReminders.all(ctx).isEmpty())
  click("backup_restore");deliver(Uri.fromFile(file));await("backup_preview_text")
  rule.onNodeWithTag("backup_restore_confirm",useUnmergedTree=true).performClick();settle();await("backup_notice")
  val notes=Repo.notesChecked(ctx);assertEquals(2,notes.size);assertTrue(notes.any{it.text=="naya local note"});assertTrue(notes.any{it.text=="dawa ki list"})
  assertEquals(1,CallReminders.all(ctx).size)
  assertEquals(1,CallLists.read(ctx).size)
  assertEquals(listOf("+91140"),runBlocking{ctx.ldStore.data.first()}[K.blockedPrefixes]?.split(";"))
 }
 @Test fun badFileShowsErrorAndChangesNothing(){
  populate();show()
  val file=File(ctx.cacheDir,"kharab.json");file.writeText("{\"kind\":\"other\",\"version\":1}")
  click("backup_restore");deliver(Uri.fromFile(file));await("backup_error")
  assertEquals(1,Repo.notesChecked(ctx).size);assertEquals(1,CallReminders.all(ctx).size)
 }
 @Test fun dayAndLargeNightPixels(){
  populate();show();saveShot(rule,"279-backup-day")
  click("backup_restore");val file=File(ctx.cacheDir,"restore2.json");file.writeText(FullBackup.export(ctx));deliver(Uri.fromFile(file));await("backup_preview_text");saveShot(rule,"279-backup-preview-day")
  rule.onNodeWithTag("backup_restore_cancel",useUnmergedTree=true).performClick();settle()
  show(true);saveShot(rule,"279-backup-night-large")
 }
}
