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
class HistoryExplorerUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private var failQuery=false
 private var opened=""
 private var paged=false
 private var queries=0
 private val pageDate=System.currentTimeMillis()-10000
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
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
    queries++
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
 @Test fun explicitSearchCallDetailsAndFailedRefreshPreserveResults(){
  show();assertNull(rule.activity.state.confirmNumber);rule.onNodeWithTag("history_call_1").assertDoesNotExist()
  click("history_search");rule.waitUntil(15000){rule.onAllNodesWithTag("history_count",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()};settle()
  click("history_details_1");assertEquals("+919876543210",opened)
  click("history_call_1");assertEquals("+919876543210",rule.activity.state.confirmNumber)
  failQuery=true;click("history_search");rule.waitUntil(15000){rule.onAllNodesWithTag("history_error",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()};settle()
  rule.onNodeWithTag("history_call_1").performScrollTo().assertIsDisplayed()
 }
 @Test fun invalidDatesNeverReadOrCall(){
  show();rule.onNodeWithTag("history_from").performTextReplacement("2026-02-30");click("history_search")
  rule.onNodeWithTag("history_error",useUnmergedTree=true).performScrollTo().assertIsDisplayed();rule.onNodeWithTag("history_call_1").assertDoesNotExist();assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun pagingKeepsAppliedFiltersAndLoadedSearchShowsMoreHint(){
  paged=true;show();click("history_search")
  rule.waitUntil(15000){rule.onAllNodesWithTag("history_count",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()};settle()
  rule.onNodeWithTag("history_query").performScrollTo().performTextInput("Person777");settle()
  rule.onNodeWithTag("history_count",useUnmergedTree=true).assertTextEquals("100 records load hue · 0 matches")
  click("history_more")
  rule.waitUntil(15000){queries==2};settle()
  rule.onNodeWithTag("history_count",useUnmergedTree=true).performScrollTo().assertTextEquals("200 records load hue · 0 matches")
  assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun dayPixels()=pixels("daylight",1f,"day")
 @Test fun largeNightPixels()=pixels("midnight",1.3f,"night-large")
 private fun pixels(t:String,f:Float,suffix:String){
  show(t,f);saveShot(rule,"270-history-filters-$suffix");click("history_search")
  rule.waitUntil(15000){rule.onAllNodesWithTag("history_count",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()};settle()
  rule.onRoot().performTouchInput {swipeUp()};settle();rule.onNodeWithTag("history_call_1").assertIsDisplayed();saveShot(rule,"270-history-results-$suffix")
 }
}
