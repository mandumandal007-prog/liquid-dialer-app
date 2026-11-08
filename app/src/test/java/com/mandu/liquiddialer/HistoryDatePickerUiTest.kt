package com.mandu.liquiddialer
import android.content.*
import android.database.*
import android.net.Uri
import android.provider.CallLog
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
import org.robolectric.shadows.ShadowContentResolver
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryDatePickerUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private var failQuery=false
 private var opened=""
 private var paged=false
 private var queries=0
 private var lastSelection=""
 private var lastArgs=emptyList<String>()
 private val pageDate=System.currentTimeMillis()-10000
 private fun settle(){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun click(t:String){rule.onNodeWithTag(t).performScrollTo().performClick();settle()}
 private fun show(theme:String="daylight",font:Float=1f){
  rule.mainClock.autoAdvance=false;settle()
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.confirmCall]=true}}
  rule.waitUntil(15000){LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font && LdApp.settings.value.confirmCall}
  org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
  ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,object:ContentProvider(){
   override fun onCreate()=true
   override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor? {
    if(failQuery)return null
    queries++;lastSelection=s.orEmpty();lastArgs=a?.toList().orEmpty()
    if(paged) {
     val before=if(a!=null && a.size>=5)a.last().toLong() else 1001L
     return MatrixCursor(p).apply {(before-1 downTo maxOf(1,before-101)).forEach {id -> addRow(arrayOf(id,"+919876543210","Person$id",2,pageDate,90L,null,null,1))}}
    }
    return MatrixCursor(p).apply {addRow(arrayOf(1L,"+919876543210","Papa",2,System.currentTimeMillis()-10000,90L,null,null,1))}
   }
   override fun insert(u:Uri,v:ContentValues?):Uri?=null
   override fun delete(u:Uri,s:String?,a:Array<out String>?)=0
   override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0
   override fun getType(u:Uri):String?=null
  })
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{HistoryExplorerPage(rule.activity.state,rule.activity,{}, {opened=it})}}};settle()
 }
 private fun dialog():android.app.DatePickerDialog = org.robolectric.shadows.ShadowDialog.getLatestDialog() as android.app.DatePickerDialog
 private fun pickerShot(d:android.app.DatePickerDialog){
  val v=d.window!!.decorView
  val w=720
  v.measure(android.view.View.MeasureSpec.makeMeasureSpec(w,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(1400,android.view.View.MeasureSpec.AT_MOST));v.layout(0,0,w,v.measuredHeight)
  val b=android.graphics.Bitmap.createBitmap(w,v.measuredHeight,android.graphics.Bitmap.Config.ARGB_8888);v.draw(android.graphics.Canvas(b))
  val f=java.io.File("build/shots/DATE358-picker.png");f.parentFile?.mkdirs();f.outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
 }
 private fun field(tag:String)=rule.onNodeWithTag(tag).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text
 @Test fun calendarChangesOnlyChosenFieldAndCancelKeepsDates(){
  show();val originalTo=field("history_to");val reads=queries
  rule.onNodeWithTag("history_from").performTextReplacement("2024-02-29")
  click("history_type_2");click("history_connected");click("history_pick_from")
  var d=dialog();pickerShot(d);assertEquals(2024,d.datePicker.year);assertEquals(1,d.datePicker.month);assertEquals(29,d.datePicker.dayOfMonth)
  rule.runOnUiThread {d.datePicker.updateDate(2025,11,31);d.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick()};settle()
  assertEquals("2024-02-29",field("history_from"));assertEquals(originalTo,field("history_to"));assertEquals(reads,queries)
  click("history_pick_from");d=dialog()
  rule.runOnUiThread {d.datePicker.updateDate(2025,11,31);d.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()};settle()
  assertEquals("2025-12-31",field("history_from"));assertEquals(originalTo,field("history_to"));assertEquals(reads,queries)
  click("history_pick_to");d=dialog()
  rule.runOnUiThread {d.datePicker.updateDate(2026,0,2);d.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()};settle()
  assertEquals("2026-01-02",field("history_to"));assertEquals(reads,queries)
  rule.onNodeWithTag("history_type_2").assertTextContains("[Outgoing]");rule.onNodeWithTag("history_connected").assertTextContains("Connected only: ON")
  assertNull(rule.activity.state.confirmNumber);saveShot(rule,"DATE358-day")
 }
 @Test fun invalidTypedDateCanBeRepairedWithoutReadingAndLargeNightFits(){
  show("midnight",1.3f);val reads=queries
  rule.onNodeWithTag("history_from").performTextReplacement("invalid")
  click("history_pick_from");val d=dialog()
  rule.runOnUiThread {d.datePicker.updateDate(2024,1,29);d.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()};settle()
  assertEquals("2024-02-29",field("history_from"));assertEquals(reads,queries)
  rule.onNodeWithTag("history_pick_from").assertIsDisplayed();rule.onNodeWithTag("history_pick_to").assertIsDisplayed()
  saveShot(rule,"DATE358-night-large")
 }
}
