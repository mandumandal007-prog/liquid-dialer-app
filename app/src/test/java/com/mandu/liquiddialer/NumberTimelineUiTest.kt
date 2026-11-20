package com.mandu.liquiddialer
import android.Manifest
import android.content.*
import android.database.*
import android.net.Uri
import android.provider.CallLog
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class NumberTimelineUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 @get:Rule val diagnosticName=org.junit.rules.TestName()
 @get:Rule val diagnosticOrder=object:org.junit.rules.TestWatcher(){
  override fun starting(d:org.junit.runner.Description){println("DIAG529 START ${d.className}.${d.methodName} nanos=${System.nanoTime()}")}
  override fun finished(d:org.junit.runner.Description){println("DIAG529 FINISH ${d.className}.${d.methodName} nanos=${System.nanoTime()}")}
 }
 @Volatile private var diagnosticQueries=0
 @Volatile private var diagnosticQueryPhase="not-started"
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+44 2025550123"
 private var data:List<Recent> = emptyList();private var fail=false;private var writes=0;private var generation=0
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()};settle()}
 private fun exists(tag:String)=rule.onAllNodesWithTag(tag,true).fetchSemanticsNodes().isNotEmpty()
 private fun reveal(tag:String):SemanticsNodeInteraction{
  repeat(65){val n=rule.onNodeWithTag(tag,true);val r=n.getUnclippedBoundsInRoot();val sc=rule.onAllNodes(hasScrollAction(),true).onLast();val v=sc.getUnclippedBoundsInRoot();if(r.bottom>r.top&&r.top>=v.top&&r.bottom<=v.bottom)return n;sc.performTouchInput{if(r.top<v.top)swipeDown()else swipeUp()};settle()};saveShot(rule,"TIME380-failure");error("Not visible $tag")
 }
 private fun click(tag:String){reveal(tag).performClick();settle()}
 @Before fun setup(){
  shadowOf(ctx).grantPermissions(Manifest.permission.READ_CALL_LOG);fail=false;writes=0
  val now=System.currentTimeMillis();data=listOf(Recent(3,number,"Office",1,now-60000,65,"sim-a",null),Recent(2,number,"Office",3,now-3600000,0,"sim-a",null),Recent(1,"+1 2025550123","Wrong country",2,now-7200000,999,"sim-a",null))
  ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,object:ContentProvider(){
   override fun onCreate()=true
   override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor?{
    diagnosticQueries++;diagnosticQueryPhase="entered"
    if(fail){diagnosticQueryPhase="denied";throw SecurityException("Denied")}
    var rows=data.sortedWith(compareByDescending<Recent>{it.date}.thenByDescending{it.id})
    if(a!=null&&s?.contains("date>=?")==true){val start=a[0].toLong();val end=a[1].toLong();rows=rows.filter{it.date>=start&&it.date<end};var index=2
     if(s.contains("type=?")){val type=a[index++].toInt();rows=rows.filter{it.type==type}}
     if(s.contains("_id<?")){val date=a[index].toLong();val id=a[index+2].toLong();rows=rows.filter{it.date<date||it.date==date&&it.id<id}}
    }
    return MatrixCursor(p).apply{rows.take(u.getQueryParameter("limit")?.toInt()?:2000).forEach{r->addRow(p!!.map{col->when(col){"_id"->r.id;"number"->r.number;"name"->r.name;"type"->r.type;"date"->r.date;"duration"->r.duration;"subscription_id"->r.accountId;"photo_uri"->r.photo;"presentation"->1;else->null}}.toTypedArray())}}.also{diagnosticQueryPhase="returned-cursor"}
   }
   override fun insert(u:Uri,v:ContentValues?):Uri?{writes++;error("No writes")}
   override fun delete(u:Uri,s:String?,a:Array<out String>?):Int{writes++;error("No deletes")}
   override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?):Int{writes++;error("No updates")}
   override fun getType(u:Uri):String?=null
  })
 }
 private fun show(theme:String="daylight",font:Float=1f,notes:Boolean=false,details:Boolean=false){
  rule.mainClock.autoAdvance=false;settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}};await{LdApp.settings.value.let{it.theme==theme&&it.fontScale==font}}
  val g=++generation;rule.runOnUiThread{rule.activity.mountTestPage{key(g){LdThemeHost{var page by remember{mutableStateOf(!details)}
   if(details&&!page)PageFrame("Number details",{}){NumberSheet(number,null,rule.activity.state,rule.activity,{},{},{},onTimeline={page=true})}
   else if(notes)Sheet({}){NotesSheet(number,"Office",{})}else NumberTimelinePage(number,{})
  }}}};settle();if(!notes&&!details)try{await{exists("timeline_count")||exists("timeline_error")||exists("timeline_permission")}}catch(f:Throwable){
   println("DIAG529 readiness failure test=${diagnosticName.methodName} nanos=${System.nanoTime()} generation=$generation requestedTheme=$theme requestedFont=$font queries=$diagnosticQueries queryPhase=$diagnosticQueryPhase fail=$fail rows=${data.size} writes=$writes actualTheme=${LdApp.settings.value.theme} actualFont=${LdApp.settings.value.fontScale} converterFullUnits=${LdApp.settings.value.converterFullUnits}")
   runCatching{rule.runOnUiThread{val a=rule.activity;val v=a.window.decorView;println("DIAG529 activity lifecycle=${a.lifecycle.currentState} finishing=${a.isFinishing} destroyed=${a.isDestroyed} decorAttached=${v.isAttachedToWindow} windowFocus=${v.hasWindowFocus()} shown=${v.isShown}")}}.onFailure{println("DIAG529 lifecycle capture failed: $it")}
   runCatching{println(rule.onRoot(true).printToString())}.onFailure{println("DIAG529 semantics capture failed: $it")}
   runCatching{saveShot(rule,"DIAG529-timeline-readiness-failure")}.onFailure{println("DIAG529 pixel capture failed: $it")}
   throw f
  }
 }
 @Test fun detailsEntryShowsExactHistoryAndLoadedSummaryNoWrites(){
  show(details=true);click("details_timeline");await{exists("timeline_count")};reveal("timeline_count").assertTextEquals("2 matching calls · 3 phone-log rows checked");reveal("timeline_duration").assertTextEquals("Loaded connected talk: 01:05");rule.onNodeWithTag("timeline_row_1",true).assertDoesNotExist();reveal("timeline_row_3");saveShot(rule,"TIME380-day");assertEquals(0,writes);assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun typeAndTimeFiltersResetLoadedSummaryAndRefreshUsesNewData(){
  data=data+Recent(4,number,"Office",2,System.currentTimeMillis()-20L*86400000,120,"sim-a",null);show();click("timeline_type_2");await{exists("timeline_count")};reveal("timeline_count").assertTextEquals("1 matching calls · 2 phone-log rows checked");click("timeline_days_7");await{exists("timeline_count")};reveal("timeline_count").assertTextEquals("0 matching calls · 1 phone-log rows checked");reveal("timeline_empty").assertExists()
  data=data+Recent(5,number,"Office",2,System.currentTimeMillis()-1000,30,"sim-a",null);click("timeline_refresh");await{exists("timeline_row_5")};reveal("timeline_count").assertTextEquals("1 matching calls · 2 phone-log rows checked");assertEquals(0,writes)
 }
 @Test fun emptyFirstPageCanLoadOlderExactMatchWithoutClaimingNoHistory(){
  val now=System.currentTimeMillis();data=(1L..101L).map{i->Recent(i,if(i==101L)number else "+12025550123",null,2,now-i*1000,10,null,null)};show();reveal("timeline_count").assertTextEquals("0 matching calls · 100 phone-log rows checked");reveal("timeline_empty").assertTextContains("Load older",substring=true);click("timeline_more");await{exists("timeline_row_101")};reveal("timeline_count").assertTextEquals("1 matching calls · 101 phone-log rows checked");rule.onNodeWithTag("timeline_more").assertDoesNotExist();assertEquals(0,writes)
 }
 @Test fun errorsAndRetryDoNotBecomeEmptyHistory(){
  fail=true;show();rule.onNodeWithTag("timeline_count").assertDoesNotExist();rule.onNodeWithTag("timeline_empty").assertDoesNotExist();saveShot(rule,"TIME380-error");fail=false;click("timeline_refresh");await{exists("timeline_count")};reveal("timeline_count").assertTextEquals("2 matching calls · 3 phone-log rows checked")
 }
 @Test fun permissionDeniedOffersAccessWithoutHistoryClaim(){
  shadowOf(ctx).denyPermissions(Manifest.permission.READ_CALL_LOG);show();rule.onNodeWithTag("timeline_permission").assertExists();rule.onNodeWithTag("timeline_count").assertDoesNotExist();saveShot(rule,"TIME380-permission");assertEquals(0,writes)
 }
 @Test fun notesEntryRetainsUnsavedNoteAndNumber(){
  show(notes=true);rule.onNode(hasSetTextAction()).performTextInput("Keep unsaved note");settle();click("call_timeline_open");await{exists("timeline_count")};reveal("timeline_number").assertTextEquals(number);saveShot(rule,"TIME380-notes");click("timeline_notes_back");rule.onNode(hasSetTextAction()).assertTextEquals("Keep unsaved note");assertEquals(0,writes)
 }
 @Test fun largeNightLayoutAndRealLoadOlderFailureKeepsLoadedRows(){
  val now=System.currentTimeMillis();data=(1L..101L).map{i->Recent(i,if(i==100L)number else "+12025550123",null,2,now-i*1000,90061,null,null)};show("midnight",1.3f);reveal("timeline_count").assertTextEquals("1 matching calls · 100 phone-log rows checked");saveShot(rule,"TIME380-night");fail=true;click("timeline_more");await{exists("timeline_error")};reveal("timeline_count").assertTextEquals("1 matching calls · 100 phone-log rows checked");assertTrue(exists("timeline_row_100"));fail=false;click("timeline_more");await{!exists("timeline_more")};reveal("timeline_count").assertTextEquals("1 matching calls · 101 phone-log rows checked")
 }
 @Test fun simulatedActiveCallHandoffRetainsEndAndDoesNotChangeLog(){
  val manager=com.mandu.liquiddialer.telecom.CallManager;val call=FakeCall.make(number,"Office",android.telecom.Call.STATE_ACTIVE,System.currentTimeMillis()-60000);show();manager.calls.value=listOf(call);manager.bump()
  try{rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallScreen({},{},{},{})}}};settle();rule.onNodeWithTag("end_call",true).assertIsDisplayed()
   val label=rule.onNodeWithText("Notes",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot;val controls=rule.onAllNodes(hasClickAction(),true);val nodes=controls.fetchSemanticsNodes();val index=nodes.indices.filter{val b=nodes[it].boundsInRoot;b.left<=label.center.x&&b.right>=label.center.x&&b.bottom<=label.top}.maxBy{nodes[it].boundsInRoot.bottom};controls[index].performClick();settle();await{exists("call_timeline_open")};click("call_timeline_open");await{exists("timeline_count")};reveal("timeline_number").assertTextEquals(number);saveShot(rule,"TIME380-active");click("timeline_notes_back");rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle();rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertEquals(android.telecom.Call.STATE_ACTIVE,manager.state(call));assertEquals(0,writes);assertNull(rule.activity.state.confirmNumber)
  }finally{manager.calls.value=emptyList();manager.bump()}
 }
}
