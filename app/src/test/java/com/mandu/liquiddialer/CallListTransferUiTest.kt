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
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallListTransferUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().performClick();rule.mainClock.autoAdvance=false;settle()}
 @Before fun clear(){listOf("",".bak",".new").forEach {File(ctx.filesDir,"manual_call_lists.json$it").delete()}}
 private fun show(theme:String="daylight",font:Float=1f){
  rule.mainClock.autoAdvance=false;settle()
  runBlocking{ctx.ldStore.edit {it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}}
  rule.waitUntil(5000){LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font}
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{CallListsPage(rule.activity.state,rule.activity,{})}}};settle()
  rule.waitUntil(5000){rule.onAllNodesWithTag("lists_create").fetchSemanticsNodes().isNotEmpty()};settle()
 }
 private fun awaitTag(tag:String){
  repeat(50){settle();if(rule.onAllNodesWithTag(tag,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty())return;Thread.sleep(50)}
  error("Missing $tag; tree="+rule.onAllNodes(isRoot(),useUnmergedTree=true).printToString())
 }
 private fun deliver(uri:Uri,result:Int=Activity.RESULT_OK){val started=Shadows.shadowOf(rule.activity).nextStartedActivityForResult;assertNotNull(started);rule.runOnUiThread{Shadows.shadowOf(rule.activity).receiveResult(started!!.intent,result,Intent().setData(uri))};settle()}
 @Test fun batchReviewRejectsInvalidRowsSkipsDuplicatesAndAddsPendingOnly(){
  val id=CallLists.create(ctx,"Work").single().id;CallLists.add(ctx,id,"9876543210","Keep")
  val original=CallLists.read(ctx).single().entries.single();CallLists.done(ctx,id,original.id,true)
  show();click("list_open_$id");click("list_batch")
  rule.onNodeWithTag("list_batch_text").performTextInput("9876543210,Replace\n*123#,Bad\n+442025550123,Office")
  click("list_batch_preview");rule.onNodeWithTag("list_batch_invalid",useUnmergedTree=true).performScrollTo().assertIsDisplayed()
  rule.onNodeWithTag("list_batch_confirm").assertIsNotEnabled();assertEquals(1,CallLists.read(ctx).single().entries.size)
  rule.onNodeWithTag("list_batch_text").performTextReplacement("9876543210,Replace\n+442025550123,Office")
  click("list_batch_preview");rule.onNodeWithTag("list_batch_count",useUnmergedTree=true).assertTextEquals("1 naye · 1 duplicate skip")
  click("list_batch_confirm");rule.waitUntil(5000){CallLists.read(ctx).single().entries.size==2};settle()
  val entries=CallLists.read(ctx).single().entries;assertEquals("Keep",entries.first().name);assertTrue(entries.first().done);assertFalse(entries.last().done)
  assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun exportPickerWritesExactBackupAndImportRequiresConfirmAndKeepsOriginal(){
  val id=CallLists.create(ctx,"घर | Work").single().id;CallLists.add(ctx,id,"+442025550123","Office")
  val e=CallLists.read(ctx).single().entries.single();CallLists.done(ctx,id,e.id,true)
  val original=CallLists.read(ctx);show();click("lists_export")
  val backup=File(ctx.cacheDir,"lists-backup.json");deliver(Uri.fromFile(backup))
  rule.waitUntil(5000){backup.exists() && backup.length()>0};awaitTag("lists_notice");settle()
  assertEquals(original,CallListTransfer.preview(backup.readText()))
  click("lists_import");deliver(Uri.fromFile(backup))
  awaitTag("lists_import_confirm");settle();assertEquals(original,CallLists.read(ctx))
  click("lists_import_cancel");assertEquals(original,CallLists.read(ctx))
  click("lists_import");deliver(Uri.fromFile(backup))
  awaitTag("lists_import_confirm");click("lists_import_confirm")
  rule.waitUntil(5000){CallLists.read(ctx).size==2};settle();val all=CallLists.read(ctx)
  assertEquals(original.single(),all.first());assertNotEquals(id,all.last().id);assertTrue(all.last().entries.single().done);assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun corruptImportAndPickerCancelNeverChangeSavedLists(){
  CallLists.create(ctx,"Keep");val original=CallLists.read(ctx);show();click("lists_import")
  val bad=File(ctx.cacheDir,"bad.json").apply{writeText("broken")};deliver(Uri.fromFile(bad))
  awaitTag("lists_error");settle()
  assertEquals(original,CallLists.read(ctx));rule.onNodeWithTag("lists_import_confirm").assertDoesNotExist()
  click("lists_import");deliver(Uri.fromFile(bad),Activity.RESULT_CANCELED);assertEquals(original,CallLists.read(ctx))
 }
 @Test fun dayTransferPixels()=pixels("daylight",1f,"day")
 @Test fun largeNightTransferPixels()=pixels("midnight",1.3f,"night-large")
 private fun pixels(theme:String,font:Float,suffix:String){
  val id=CallLists.create(ctx,"Family follow-ups").single().id;CallLists.add(ctx,id,"9876543210","Papa")
  show(theme,font);saveShot(rule,"274-list-backup-$suffix")
  click("list_open_$id");click("list_batch")
  rule.onNodeWithTag("list_batch_text").performTextInput("9876543210,Papa\n+442025550123,Office\n+12025550123,Family")
  click("list_batch_preview");saveShot(rule,"274-list-batch-$suffix")
  click("list_form_close");rule.onNodeWithTag("page_back",useUnmergedTree=true).performClick();settle()
  val backup=File(ctx.cacheDir,"preview.json").apply{writeText(CallListTransfer.backup(CallLists.read(ctx)))}
  click("lists_import");deliver(Uri.fromFile(backup))
  awaitTag("lists_import_confirm");settle();saveShot(rule,"274-list-import-$suffix")
 }
}
