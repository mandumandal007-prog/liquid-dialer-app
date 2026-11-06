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
import java.text.SimpleDateFormat
import java.util.*
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReminderCenterUiTest {
 @get:Rule(order = 0) val flakyRetry = FlakyRetryRule()
 @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun click(tag:String){rule.onNodeWithTag(tag).performScrollTo().performClick();settle()}
 @Before fun clean(){ctx.getSharedPreferences("call_reminders",0).edit().clear().commit()}
 private fun show(theme:String="daylight",font:Float=1f){
  rule.mainClock.autoAdvance=false;settle()
  runBlocking{ctx.ldStore.edit {it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}}
  rule.waitUntil(15000){LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font}
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{ReminderCenterPage({},{})}}};settle()
  rule.waitUntil(15000){rule.onAllNodesWithTag("reminder_new").fetchSemanticsNodes().isNotEmpty()};settle()
 }
 private fun awaitRows(count:Int){rule.waitUntil(15000){CallReminders.all(ctx).size==count};settle()}
 @Test fun createValidateRescheduleSearchAndConfirmedCancel(){
  show();click("reminder_new")
  rule.onNodeWithTag("reminder_number").performTextInput("*123#")
  rule.onNodeWithTag("reminder_save").performClick();settle()
  rule.onNodeWithTag("reminder_editor_error",useUnmergedTree=true).assertIsDisplayed();assertTrue(CallReminders.all(ctx).isEmpty())
  rule.onNodeWithTag("reminder_number").performTextReplacement("+44 2025550123")
  rule.onNodeWithTag("reminder_name").performTextInput("Family | Office")
  rule.onNodeWithTag("reminder_date").performTextReplacement("2026-02-30")
  rule.onNodeWithTag("reminder_save").performClick();settle();assertTrue(CallReminders.all(ctx).isEmpty())
  val later=Date(System.currentTimeMillis()+86400000)
  rule.onNodeWithTag("reminder_date").performTextReplacement(SimpleDateFormat("yyyy-MM-dd",Locale.US).format(later))
  rule.onNodeWithTag("reminder_time").performTextReplacement("13:45")
  rule.onNodeWithTag("reminder_save").performClick();awaitRows(1)
  val first=CallReminders.all(ctx).single();assertEquals("Family | Office",first.name)
  assertNull(rule.activity.state.confirmNumber)
  click("reminder_edit_${first.token}")
  rule.onNodeWithTag("reminder_time").performTextReplacement("15:30")
  rule.onNodeWithTag("reminder_save").performClick()
  rule.waitUntil(15000){CallReminders.all(ctx).single().token!=first.token};settle()
  val changed=CallReminders.all(ctx).single();assertEquals(first.number,changed.number)
  assertEquals("15:30",SimpleDateFormat("HH:mm",Locale.US).format(Date(changed.atMs)))
  rule.onNodeWithTag("reminder_search").performTextInput("Missing");settle()
  rule.onNodeWithTag("reminder_card_${changed.token}").assertDoesNotExist()
  rule.onNodeWithTag("reminder_search").performTextReplacement("Office");settle()
  click("reminder_cancel_${changed.token}");rule.onNodeWithText("Rehne do").performClick();settle();assertEquals(1,CallReminders.all(ctx).size)
  click("reminder_cancel_${changed.token}");rule.onNodeWithText("Reminder cancel karo").performClick();awaitRows(0)
  assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun dayCenterAndEditorPixels()=pixels("daylight",1f,"day")
 @Test fun largeNightCenterAndEditorPixels()=pixels("midnight",1.3f,"night-large")
 private fun pixels(theme:String,font:Float,suffix:String){
  CallReminders.set(ctx,"+91 98765 43210","Papa - medicine follow-up",System.currentTimeMillis()+3600000)
  CallReminders.set(ctx,"+44 2025550123","Family | Office",System.currentTimeMillis()+86400000)
  org.robolectric.Shadows.shadowOf(ctx.getSystemService(android.app.NotificationManager::class.java)).setNotificationsEnabled(false)
  show(theme,font)
  rule.onNodeWithTag("reminder_warning",useUnmergedTree=true).assertIsDisplayed()
  saveShot(rule,"272-reminder-center-$suffix")
  click("reminder_new");rule.waitUntil(60000){rule.onAllNodesWithTag("reminder_number").fetchSemanticsNodes().isNotEmpty()} // cold CI first-compose
  rule.onNodeWithTag("reminder_number").performTextInput("+91 98765 43210")
  rule.onNodeWithTag("reminder_name").performTextInput("Papa - follow-up");settle()
  saveShot(rule,"272-reminder-editor-$suffix")
 }
}
