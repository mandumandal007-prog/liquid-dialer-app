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
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class HistoryExplorerTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val f=HistoryExplorer.Filter(100,2000)
 private open class Provider:ContentProvider(){
  override fun onCreate()=true
  override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor?=null
  override fun insert(u:Uri,v:ContentValues?):Uri?=null
  override fun delete(u:Uri,s:String?,a:Array<out String>?)=0
  override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0
  override fun getType(u:Uri):String?=null
 }
 private fun provider(rows:List<Array<Any?>>){ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,object:Provider(){override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor {
  assertEquals("101",u.getQueryParameter("limit"));assertEquals("date DESC, _id DESC",o)
  return MatrixCursor(p).apply{rows.forEach {addRow(it)}}
 }})}
 private fun row(id:Long,date:Long=1000,presentation:Int=1):Array<Any?> = arrayOf(id,"+441234567890","Cached",2,date,10L,"sim-a",null,presentation)
 @Test fun dateRangeIsInclusiveLocalDaysAndRejectsInvalidRanges(){
  val (a,b)=HistoryExplorer.dates("2026-11-03","2026-11-03");assertEquals(86400000L,b-a)
  listOf("2026-02-30","03-11-2026").forEach {d->try{HistoryExplorer.dates(d,d);fail()}catch(_:Exception){}}
  try{HistoryExplorer.dates("2026-11-04","2026-11-03");fail()}catch(_:Exception){}
 }
 @Test fun combinedSelectionUsesBoundArgsAndTieBreakAnchor(){
  val s=HistoryExplorer.selection(f.copy(type=2,account="SIM' OR 1=1",connected=true),HistoryExplorer.Anchor(500,7))
  assertArrayEquals(arrayOf("100","2000","2","SIM' OR 1=1","500","500","7"),s.args)
  assertFalse(s.clause.contains("SIM'"));assertTrue(s.clause.contains("_id<?"));assertTrue(s.clause.contains("duration>0"))
 }
 @Test fun hundredRowPageKeepsSameTimestampTieBreakAndDetectsMore(){
  provider((201L downTo 101L).map {row(it)})
  val p=HistoryExplorer.page(ctx,f,null);assertEquals(100,p.rows.size);assertEquals(HistoryExplorer.Anchor(1000,102),p.next)
  provider(listOf(row(101),row(100)));val q=HistoryExplorer.page(ctx,f,p.next);assertEquals(listOf(101L,100L),q.rows.map {it.id});assertNull(q.next)
 }
 @Test fun restrictedNumbersAndNamesNeverExposeActionsData(){
  provider(listOf(row(2,presentation=2),row(1,presentation=3)));val p=HistoryExplorer.page(ctx,f,null)
  assertTrue(p.rows.all{it.number.isEmpty() && it.name==null && it.photo==null})
 }
 @Test fun nullCursorAndMisorderedOrOutOfFilterResultsAreErrors(){
  ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,Provider())
  try{HistoryExplorer.page(ctx,f,null);fail()}catch(_:Exception){}
  listOf(listOf(row(1),row(2)),listOf(row(1,99)),listOf(row(1),row(1))).forEach {rows->provider(rows);try{HistoryExplorer.page(ctx,f,null);fail()}catch(_:IllegalArgumentException){}}
  provider(listOf(row(1)));try{HistoryExplorer.page(ctx,f.copy(type=3),null);fail()}catch(_:IllegalArgumentException){}
 }
 @Test fun loadedTextSearchPreservesDigitsAndNamesWithoutPretendingToScan(){
  val r=Recent(1,"+44 1234567890","Office",2,1000,10,"a",null)
  assertTrue(HistoryExplorer.matches(r,"off"));assertTrue(HistoryExplorer.matches(r,"123 456"));assertFalse(HistoryExplorer.matches(r,"office999"));assertFalse(HistoryExplorer.matches(r,"other"))
 }
}
