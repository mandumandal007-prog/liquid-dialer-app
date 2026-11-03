package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.HistoryExplorer
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26])
class HistoryExplorerApi26Test {
 @Test fun datesAndAccountSelectionUseSupportedBaseColumns(){
  val (a,b)=HistoryExplorer.dates("2026-11-03","2026-11-04")
  val s=HistoryExplorer.selection(HistoryExplorer.Filter(a,b,2,"sim",true),HistoryExplorer.Anchor(b-1,22))
  assertEquals(7,s.args.size);assertTrue(s.clause.contains(android.provider.CallLog.Calls.PHONE_ACCOUNT_ID))
 }
}
