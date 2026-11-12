package com.mandu.liquiddialer
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class RecordingLibraryUiTest {
 @get:Rule(order=0) val initialSettings=object:org.junit.rules.ExternalResource(){override fun before(){
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false}}
 }}
 @get:Rule(order=1) val rule=createAndroidComposeRule<ComponentActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 private fun ComponentActivity.mountTestPage(content:@Composable ()->Unit){val host=ComposeView(this);host.setContent(content=content);setContentView(host)}
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val root get()=Repo.recordingsDir(ctx)
 private fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun reveal(node:SemanticsNodeInteraction){
  rule.mainClock.autoAdvance=true
  repeat(6){val n=node.fetchSemanticsNode();val root=rule.onRoot().fetchSemanticsNode();if(n.positionInRoot.y>=100&&n.positionInRoot.y+n.size.height<=root.size.height-20){settle();return}
   val delta=n.positionInRoot.y+n.size.height/2f-root.size.height/2f
   rule.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,delta)};rule.waitForIdle()
  };settle();node.assertIsDisplayed()
 }
 private fun click(tag:String){val node=rule.onNodeWithTag(tag,useUnmergedTree=true);if(!tag.startsWith("recording_rename_confirm")&&!tag.startsWith("recording_rename_cancel"))reveal(node);node.assertIsDisplayed().performClick();settle()}
 private fun await(tag:String){rule.waitUntil(15000){settle();rule.onAllNodesWithTag(tag,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}}
 @Before fun clear(){CallManager.recording.value=null;root.listFiles()?.forEach{it.delete()}}
 private fun add(name:String,body:String="test audio"){File(root,name).writeText(body)}
 private fun show(night:Boolean=false){
  rule.mainClock.autoAdvance=false;settle()
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight" else "daylight";it[K.fontScale]=if(night)1.5f else 1f}}
  rule.waitUntil(15000){LdApp.settings.value.let{it.onboarded&&it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.5f else 1f)}}
  if(night)rule.runOnUiThread{val c=android.content.res.Configuration(rule.activity.resources.configuration);c.fontScale=1.5f;rule.activity.resources.updateConfiguration(c,rule.activity.resources.displayMetrics)}
  rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{RecordingsPage({})}}};settle();await("recordings_count")
 }
 private fun deliver(uri:Uri,result:Int=Activity.RESULT_OK){val started=Shadows.shadowOf(rule.activity).nextStartedActivityForResult;assertNotNull(started);rule.runOnUiThread{Shadows.shadowOf(rule.activity).receiveResult(started!!.intent,result,Intent().setData(uri))};settle()}
 @Test fun renameExplicitlyAndDeleteConfirmationCancelKeepsAudio(){
  add("Papa.m4a");add("Work.m4a","keep");show()
  click("recording_rename_Papa.m4a");rule.onNodeWithTag("recording_name",useUnmergedTree=true).performTextReplacement("नई meeting")
  click("recording_rename_cancel");assertTrue(File(root,"Papa.m4a").exists())
  click("recording_rename_Papa.m4a");rule.onNodeWithTag("recording_name",useUnmergedTree=true).performTextReplacement("नई meeting")
  click("recording_rename_confirm");await("recording_नई meeting.m4a");assertEquals("test audio",File(root,"नई meeting.m4a").readText())
  click("recording_delete_नई meeting.m4a");rule.onNodeWithTag("recording_delete_cancel",useUnmergedTree=true).performClick();settle();assertTrue(File(root,"नई meeting.m4a").exists())
  click("recording_delete_नई meeting.m4a");rule.onNodeWithTag("recording_delete_confirm",useUnmergedTree=true).performClick();settle();await("recordings_notice")
  rule.waitUntil(15000){!File(root,"नई meeting.m4a").exists()};assertEquals("keep",File(root,"Work.m4a").readText())
 }
 @Test fun copyPickerWritesExactBytesAndCancelLeavesOriginal(){
  add("Papa.m4a","audio bytes 123");show();click("recording_export_Papa.m4a")
  val out=File(ctx.cacheDir,"copy.m4a");deliver(Uri.fromFile(out));await("recordings_notice")
  assertEquals("audio bytes 123",out.readText());assertEquals("audio bytes 123",File(root,"Papa.m4a").readText())
  click("recording_export_Papa.m4a");deliver(Uri.fromFile(File(ctx.cacheDir,"cancel.m4a")),Activity.RESULT_CANCELED)
  assertFalse(File(ctx.cacheDir,"cancel.m4a").exists());assertTrue(File(root,"Papa.m4a").exists())
 }
 @Test fun searchAndPagesStayBoundedWithoutChangingFiles(){
  val base=System.currentTimeMillis()
 repeat(24){val n="Memo%02d.m4a".format(it);add(n);File(root,n).setLastModified(base-it*60_000L)};show()
  rule.onNodeWithTag("recording_Memo20.m4a",useUnmergedTree=true).assertDoesNotExist();click("recordings_next")
  rule.onNodeWithTag("recording_Memo20.m4a",useUnmergedTree=true).assertExists();rule.onNodeWithTag("recording_Memo00.m4a",useUnmergedTree=true).assertDoesNotExist()
  click("recordings_previous")
  val search=rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("recordings_search")),useUnmergedTree=true);reveal(search);search.performTextInput("Memo23")
  settle();rule.onNodeWithTag("recording_Memo23.m4a",useUnmergedTree=true).assertExists();rule.onNodeWithTag("recording_Memo00.m4a",useUnmergedTree=true).assertDoesNotExist();assertEquals(24,RecordingLibrary.list(ctx).size)
  click("recordings_clear");rule.onNodeWithTag("recording_Memo00.m4a",useUnmergedTree=true).assertExists()
 }
 @Test fun playbackPauseResumeStopAndShareChooserDoNotChangeAudio(){
  add("Papa.m4a");show();val before=File(root,"Papa.m4a").readBytes()
  org.robolectric.shadows.ShadowMediaPlayer.addMediaInfo(org.robolectric.shadows.util.DataSource.toDataSource(File(root,"Papa.m4a").absolutePath),org.robolectric.shadows.ShadowMediaPlayer.MediaInfo(30000,0))
  click("recording_play_Papa.m4a");settle();rule.onNodeWithText("Pause",useUnmergedTree=true).assertExists()
  click("recording_play_Papa.m4a");rule.onNodeWithText("Aage suno",useUnmergedTree=true).assertExists()
  click("recording_play_Papa.m4a");rule.onNodeWithText("Pause",useUnmergedTree=true).assertExists()
  click("recording_stop");rule.onNodeWithTag("recording_stop").assertDoesNotExist()
  click("recording_share_Papa.m4a");val chooser=Shadows.shadowOf(rule.activity).nextStartedActivity
  assertEquals(Intent.ACTION_CHOOSER,chooser.action)
  @Suppress("DEPRECATION") val send=chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
  assertEquals(Intent.ACTION_SEND,send.action);assertEquals("audio/mp4",send.type)
  @Suppress("DEPRECATION") val uri=send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
  assertArrayEquals(before,ctx.contentResolver.openInputStream(uri)!!.use{it.readBytes()});assertArrayEquals(before,File(root,"Papa.m4a").readBytes())
 }
 @Test fun activeRecordingActionsDisabledAndDayLibraryPixels(){
  add("Papa.m4a");CallManager.recording.value=CallManager.RecInfo(File(root,"Papa.m4a"),1000,"MIC");show()
  rule.onNodeWithTag("recording_play_Papa.m4a",useUnmergedTree=true).assertIsNotEnabled();rule.onNodeWithTag("recording_rename_Papa.m4a",useUnmergedTree=true).assertIsNotEnabled();rule.onNodeWithTag("recording_export_Papa.m4a",useUnmergedTree=true).assertIsNotEnabled();rule.onNodeWithTag("recording_delete_Papa.m4a",useUnmergedTree=true).assertIsNotEnabled()
  saveShot(rule,"277-recordings-day-active");CallManager.recording.value=null;settle();rule.onNodeWithTag("recording_play_Papa.m4a",useUnmergedTree=true).assertIsEnabled()
 }
 @Test fun largeFontNightLibraryRenameAndDeletePixels(){
  add("Papa meeting.m4a");show(true);
  reveal(rule.onNodeWithTag("recording_rename_Papa meeting.m4a",useUnmergedTree=true));settle();saveShot(rule,"277-recordings-night-large")
  click("recording_rename_Papa meeting.m4a");saveShot(rule,"277-recording-rename-night-large")
  rule.onNodeWithTag("recording_rename_cancel",useUnmergedTree=true).performClick();settle()
  click("recording_delete_Papa meeting.m4a");saveDialogShot("277-recording-delete-night-large",0xFF101725)
  rule.onNodeWithTag("recording_delete_cancel",useUnmergedTree=true).assertIsDisplayed();rule.onNodeWithTag("recording_delete_confirm",useUnmergedTree=true).assertIsDisplayed();assertTrue(File(root,"Papa meeting.m4a").exists())
 }
}
