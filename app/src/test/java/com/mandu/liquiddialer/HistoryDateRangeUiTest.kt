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
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class HistoryDateRangeUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private var failQuery=false
 private var opened=""
 private var paged=false
 private var queries=0
 private var lastSelection=""
 private var lastArgs=emptyList<String>()
 private val pageDate=System.currentTimeMillis()-10000
 private fun settle(){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun click(t:String){rule.waitUntil(15000){settle();rule.onAllNodesWithTag(t).fetchSemanticsNodes().isNotEmpty()};rule.mainClock.autoAdvance=true;rule.onNodeWithTag(t).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(t).performClick();settle()}
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
  rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{HistoryExplorerPage(rule.activity.state,rule.activity,{}, {opened=it})}}};settle()
 }
 @Test fun rangeButtonsFillDatesOnlyAndKeepLoadedResultsHonest(){
  show()
  val before=queries
  click("history_type_2");click("history_connected");click("history_range_7")
  val range=HistoryDateRange.recent(7)
  assertEquals(range.from,rule.onNodeWithTag("history_from").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
  assertEquals(range.to,rule.onNodeWithTag("history_to").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
  assertEquals(before,queries);assertNull(rule.activity.state.confirmNumber)
  rule.onNodeWithTag("history_type_2").assertTextContains("[Outgoing]")
  rule.onNodeWithTag("history_connected").assertTextContains("Connected only: ON")
  click("history_search");rule.waitUntil(15000){rule.onAllNodesWithTag("history_count",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()};settle()
  val applied=rule.onNodeWithTag("history_applied",useUnmergedTree=true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString{it.text}
  assertTrue(lastSelection.contains("duration>0"));assertEquals("2",lastArgs[2])
  val dates=HistoryExplorer.dates(range.from,range.to)
  assertEquals(listOf(dates.first.toString(),dates.second.toString()),lastArgs.take(2))
  val loaded=queries
  click("history_range_1")
  assertEquals(loaded,queries)
  rule.onNodeWithTag("history_pending",useUnmergedTree=true).assertExists()
  rule.onNodeWithTag("history_applied",useUnmergedTree=true).assertTextEquals(applied)
  rule.onNodeWithTag("history_pending",useUnmergedTree=true).performScrollTo();saveShot(rule,"RANGE354-pending")
  rule.onNodeWithTag("history_range_1").performScrollTo();saveShot(rule,"RANGE354-day")
 }
 @Test fun nightLargePresetsFitAndThirtyDaysRequireSearch(){
  show("midnight",1.3f);val before=queries;click("history_range_30")
  assertEquals(before,queries)
  val tags=listOf("history_range_1","history_range_7","history_range_30")
  val bounds=tags.map{rule.onNodeWithTag(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot}
  for(i in bounds.indices)for(j in i+1 until bounds.size)assertFalse(bounds[i].overlaps(bounds[j]))
  saveShot(rule,"RANGE354-night-large");assertNull(rule.activity.state.confirmNumber)
 }
}
