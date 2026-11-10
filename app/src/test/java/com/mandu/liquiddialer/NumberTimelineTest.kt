package com.mandu.liquiddialer
import android.content.*
import android.database.*
import android.net.Uri
import android.provider.CallLog
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class NumberTimelineTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+44 2025550123"
 private val filter=HistoryExplorer.Filter(100,2000)
 private open class Provider:ContentProvider(){
  override fun onCreate()=true
  override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor?=null
  override fun insert(u:Uri,v:ContentValues?):Uri?=error("No writes")
  override fun delete(u:Uri,s:String?,a:Array<out String>?)=error("No deletes")
  override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=error("No updates")
  override fun getType(u:Uri):String?=null
 }
 private fun row(id:Long,n:String=number,presentation:Int=1,date:Long=1000,duration:Long=10,type:Int=2):Array<Any?> = arrayOf(id,n,"Cached",type,date,duration,"sim-a",null,presentation)
 private fun provider(rows:List<Array<Any?>>){ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,object:Provider(){override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor{assertEquals("101",u.getQueryParameter("limit"));assertEquals("date DESC, _id DESC",o);return MatrixCursor(p).apply{rows.forEach{addRow(it)}}}})}
 private fun bad(action:()->Unit){try{action();fail("Must reject")}catch(_:Exception){}}
 @Test fun internationalIdentityNeverSuffixMatchesAndIndianAliasesMatch(){
  provider(listOf(row(4),row(3,"+1 2025550123"),row(2,"2025550123"),row(1,number,presentation=2)))
  val b=NumberTimeline.page(ctx,number,filter,null);assertEquals(listOf(4L),b.rows.map{it.id});assertEquals(4,b.scanned)
  val r=Recent(1,"9876543210",null,1,1000,1,null,null);assertEquals(listOf(r),NumberTimeline.select("+91 98765 43210",listOf(r)));assertTrue(NumberTimeline.select("+44 9876543210",listOf(r)).isEmpty())
 }
 @Test fun noMatchesPageStillHasNextAndTieBreakReadsOlder(){
  provider((201L downTo 101L).map{row(it,"+12025550123")});val a=NumberTimeline.page(ctx,number,filter,null);assertTrue(a.rows.isEmpty());assertEquals(100,a.scanned);assertEquals(HistoryExplorer.Anchor(1000,102),a.next)
  provider(listOf(row(101),row(100)));val b=NumberTimeline.page(ctx,number,filter,a.next);assertEquals(listOf(101L,100L),b.rows.map{it.id});assertNull(b.next)
 }
 @Test fun sameTimestampEndOfPageDoesNotLeakHiddenNumberOrName(){
  provider((101L downTo 1L).map{row(it,presentation=if(it==101L)2 else 1)});val a=NumberTimeline.page(ctx,number,filter,null);assertEquals(99,a.rows.size);assertFalse(a.rows.any{it.id==101L});assertEquals(HistoryExplorer.Anchor(1000,2),a.next)
 }
 @Test fun invalidNumbersFailBeforeQuery(){
  var queries=0;ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,object:Provider(){override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor?{queries++;return null}})
  listOf("","Private","*123#").forEach{bad{NumberTimeline.page(ctx,it,filter,null)}};assertEquals(0,queries)
 }
 @Test fun nullCursorWrongOrderOutOfRangeAndNegativeDurationAreErrors(){
  ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,Provider());bad{NumberTimeline.page(ctx,number,filter,null)}
  listOf(listOf(row(1),row(2)),listOf(row(1,date=99)),listOf(row(1,duration=-1))).forEach{provider(it);bad{NumberTimeline.page(ctx,number,filter,null)}}
 }
 @Test fun summaryCountsOnlyLoadedMatchingConnectedTalkNotMissedDurations(){
  val rows=listOf(Recent(1,number,null,1,1000,10,null,null),Recent(2,number,null,2,900,20,null,null),Recent(3,number,null,3,800,99,null,null),Recent(4,number,null,5,700,0,null,null))
  val s=NumberTimeline.summary(rows);assertEquals(4,s.calls);assertEquals(30L,s.connectedSeconds);assertEquals(1,s.incoming);assertEquals(1,s.outgoing);assertEquals(1,s.missed);assertEquals(1,s.rejected)
 }
 @Test fun filterUsesRollingCalendarWindowAndRejectsBadScope(){
  val now=java.util.Calendar.getInstance().apply{set(2026,10,10,9,30,0);set(java.util.Calendar.MILLISECOND,0)}.timeInMillis
  val f=NumberTimeline.filter(7,3,now);assertEquals(now+1,f.end);val expected=java.util.Calendar.getInstance().apply{timeInMillis=now;add(java.util.Calendar.DAY_OF_YEAR,-7)}.timeInMillis;assertEquals(expected,f.start);assertEquals(3,f.type);assertEquals(0L,NumberTimeline.filter(0,0,now).start)
  bad{NumberTimeline.filter(1,0,now)};bad{NumberTimeline.filter(7,99,now)};bad{NumberTimeline.filter(7,0,Long.MAX_VALUE)}
 }
 @Test fun summaryOverflowNeverTurnsNegative(){
  val r=Recent(1,number,null,1,1000,Long.MAX_VALUE,null,null);bad{NumberTimeline.summary(listOf(r,r.copy(id=2,duration=1)))};bad{NumberTimeline.summary(listOf(r.copy(duration=-1)))}
 }
}
