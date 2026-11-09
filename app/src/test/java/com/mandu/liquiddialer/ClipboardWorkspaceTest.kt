package com.mandu.liquiddialer
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
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
@OptIn(ExperimentalTestApi::class)
class ClipboardWorkspaceTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1000);rule.waitForIdle()}
 private fun open(theme:String="daylight",font:Float=1f,text:String="Rahul: +91 98765 43210\nPriya: 91234-56789"){
  rule.mainClock.autoAdvance=false
  runBlocking {app.ldStore.edit {it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.motion]=false}}
  rule.waitUntil(15000){LdApp.settings.value.let{it.onboarded&&it.theme==theme&&it.fontScale==font}}
  rule.runOnUiThread {rule.activity.state.showOnboarding=false;rule.activity.state.tab="recents"}
  settle();rule.onNodeWithTag("drawer_open",true).assertIsDisplayed()
  rule.runOnUiThread {rule.activity.state.showOnboarding=false;rule.activity.state.tab="dialer";rule.activity.state.dial="12345";app.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("copy",text))}
  println("PASTE: dialer open");settle();println("PASTE: menu tap");rule.onNodeWithTag("dial_menu",true).performClick();settle();println("PASTE: paste tap");rule.onNodeWithTag("dial_paste",true).performClick();settle();println("PASTE: preview ready")
 }
 @Before fun clear(){java.io.File(app.filesDir,"manual_call_lists.json").delete()}
 @Test fun dayPreviewCancelAndFillWithoutCalling(){
  open();saveDialogShot("PASTE362-day",0xFFF3F5FF);assertEquals("12345",rule.activity.state.dial)
  rule.onNodeWithTag("paste_cancel",true).performClick();settle();assertEquals("12345",rule.activity.state.dial);assertTrue(CallLists.read(app).isEmpty())
  rule.onNodeWithTag("dial_menu",true).performClick();settle();rule.onNodeWithTag("dial_paste",true).performClick();settle()
  rule.onNodeWithTag("paste_number_1",true).performClick();settle();assertEquals("9123456789",rule.activity.state.dial)
  assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)
 }
 @Test fun nightLargeSelectedListSavedAndExistingListUntouched(){
  val old=CallLists.createFromNumbers(app,"Existing",listOf("9000000001")).single()
  open("midnight",1.3f);saveDialogShot("PASTE362-night-large",0xFF0B1020)
  rule.onNodeWithTag("paste_select_1",true).performClick();settle()
  rule.onNodeWithTag("paste_save_list",true).performClick();settle()
  rule.waitUntil(15000){CallLists.read(app).size==2};val lists=CallLists.read(app);assertEquals(old,lists.first())
  assertEquals(listOf("+919876543210"),lists.last().entries.map{it.number});assertFalse(lists.last().entries.single().done)
  assertEquals("12345",rule.activity.state.dial)
 }
 @Test fun invalidClipboardPreservesNumber(){open(text="No phone here");saveDialogShot("PASTE362-empty",0xFFF3F5FF);rule.onNodeWithTag("paste_save_list",true).assertDoesNotExist();rule.onNodeWithTag("paste_cancel",true).performClick();settle();assertEquals("12345",rule.activity.state.dial)}
 @Test fun largeFontTwelveRowsKeepActionsPinnedAndScroll(){
  open("midnight",1.3f,(1..12).joinToString("\n"){"+44207946"+it.toString().padStart(4,'0')})
  rule.onNodeWithTag("paste_select_0",true).assertIsToggleable().assertIsOn().performClick();settle()
  rule.onNodeWithTag("paste_select_0",true).assertIsOff()
  rule.onNodeWithTag("paste_save_list",true).assertIsDisplayed();rule.onNodeWithTag("paste_cancel",true).assertIsDisplayed()
  saveDialogShot("PASTE362-long-large-top",0xFF0B1020)
  repeat(5){rule.onNodeWithTag("paste_numbers_scroll",true).performTouchInput {swipeUp(durationMillis=120)};settle()}
  rule.onNodeWithTag("paste_number_11",true).assertIsDisplayed();rule.onNodeWithTag("paste_save_list",true).assertIsDisplayed();rule.onNodeWithTag("paste_cancel",true).assertIsDisplayed()
  saveDialogShot("PASTE362-long-large-bottom",0xFF0B1020)
  rule.onNodeWithTag("paste_cancel",true).performClick();settle();assertEquals("12345",rule.activity.state.dial);assertTrue(CallLists.read(app).isEmpty())
 }

 @Test fun fullStorageShowsErrorAndKeepsAllLists(){
  repeat(20){CallLists.createFromNumbers(app,"List $it",listOf("9000000001"))};val before=CallLists.read(app)
  open();rule.onNodeWithTag("paste_save_list",true).performClick();settle()
  rule.waitUntil(15000){settle();rule.onAllNodesWithTag("paste_error",true).fetchSemanticsNodes().isNotEmpty()}
  assertEquals(before,CallLists.read(app));assertEquals("12345",rule.activity.state.dial)
  rule.onNodeWithTag("paste_cancel",true).assertIsDisplayed().performClick();settle()
 }

}
