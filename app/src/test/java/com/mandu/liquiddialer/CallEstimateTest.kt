package com.mandu.liquiddialer
import android.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CallEstimateTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+44 2025550123"
 private val f get()=File(ctx.filesDir,"call_estimates.json")
 @Before fun clear(){AtomicFile(f).delete()}
 private fun draft()=CallEstimate.Draft(lines=listOf(CallEstimate.line("Chawal","2.5","40"),CallEstimate.line("Daal","3","80.25")),discount="10.75",tax="5")
 private fun bad(block:()->Unit){try{block();fail("Must reject")}catch(_:IllegalArgumentException){}}
 @Test fun exactDecimalRoundingDiscountAndTaxAfterDiscount(){
  val t=CallEstimate.totals(draft());assertEquals("340.75",CallEstimate.money(t.subtotal));assertEquals("330.00",CallEstimate.money(t.taxable));assertEquals("16.50",CallEstimate.money(t.tax));assertEquals("346.50",CallEstimate.money(t.total))
  val d=CallEstimate.Draft(lines=listOf(CallEstimate.line("Fraction","0.005","1"),CallEstimate.line("Fraction2","0.005","1")),tax="25")
  val r=CallEstimate.totals(d);assertEquals("0.02",CallEstimate.money(r.subtotal));assertEquals("0.01",CallEstimate.money(r.tax));assertEquals("0.03",CallEstimate.money(r.total))
  val single=CallEstimate.Draft(lines=listOf(CallEstimate.line("Exact tax","1","0.99")),tax="0.5");assertEquals("0.00",CallEstimate.money(CallEstimate.totals(single).tax))
 }
 @Test fun invalidAmountsLabelsAndOverDiscountReject(){
  listOf("-1","1,000","1e2","NaN"," 1",".5","1.","1.2345","0","1000001").forEach{bad{CallEstimate.line("Item",it,"1")}}
  listOf("-1","1.001","1000000001","","Infinity").forEach{bad{CallEstimate.line("Item","1",it)}}
  bad{CallEstimate.line(" ","1","1")};bad{CallEstimate.line("x".repeat(81),"1","1")}
  bad{CallEstimate.totals(draft().copy(discount="999"))};bad{CallEstimate.totals(draft().copy(tax="100.01"))};bad{CallEstimate.totals(draft().copy(tax="1.234"))}
 }
 @Test fun saveReopenIsolationAliasesAndStaleWrites(){
  val d=CallEstimate.save(ctx,number,0,draft());assertEquals(d,CallEstimate.read(ctx,number));assertEquals(CallEstimate.Draft(),CallEstimate.read(ctx,"+1 2025550123"))
  val india=CallEstimate.save(ctx,"9876543210",0,draft());assertEquals(india,CallEstimate.read(ctx,"+91 98765 43210"))
  val next=CallEstimate.save(ctx,number,d.revision,d.copy(tax="0"));val bytes=f.readBytes();bad{CallEstimate.save(ctx,number,d.revision,draft())};bad{CallEstimate.clear(ctx,number,d.revision)};assertArrayEquals(bytes,f.readBytes());assertEquals(next,CallEstimate.read(ctx,number))
 }
 @Test fun clearRecreateNeverReusesRevisionAndOtherNumberSurvives(){
  val d=CallEstimate.save(ctx,number,0,draft());val other=CallEstimate.save(ctx,"+1 2025550123",0,draft());CallEstimate.clear(ctx,number,d.revision)
  val again=CallEstimate.save(ctx,number,0,draft());assertTrue(again.revision>d.revision);bad{CallEstimate.save(ctx,number,d.revision,draft())};assertEquals(other,CallEstimate.read(ctx,"+1 2025550123"))
 }
 @Test fun lineNumberCapacityAndInvalidNumberNoOverwrite(){
  val lines=(1..20).map{CallEstimate.line("Item$it","1","1")};CallEstimate.save(ctx,number,0,CallEstimate.Draft(lines=lines));val bytes=f.readBytes();bad{CallEstimate.save(ctx,number,CallEstimate.read(ctx,number).revision,CallEstimate.Draft(lines=lines+CallEstimate.line("Too many","1","1")))};bad{CallEstimate.save(ctx,"*123#",0,draft())};assertArrayEquals(bytes,f.readBytes())
  for(i in 1..99)CallEstimate.save(ctx,"+1202555"+i.toString().padStart(4,'0'),0,draft());val full=f.readBytes();bad{CallEstimate.save(ctx,"+33123456789",0,draft())};assertArrayEquals(full,f.readBytes());CallEstimate.clear(ctx,number,CallEstimate.read(ctx,number).revision);CallEstimate.save(ctx,"+33123456789",0,draft())
 }
 @Test fun corruptUnknownVersionTrailingNulOversizeAndMalformedUtf8FailClosed(){
  val d=CallEstimate.save(ctx,number,0,draft());val valid=f.readText()
  listOf("broken",valid+"{}",valid+"\u0000",valid.replace("\"version\":1","\"version\":2"),"x".repeat(1024*1024+1)).forEach{s->f.writeText(s);val bytes=f.readBytes();try{CallEstimate.save(ctx,number,d.revision,draft());fail("Must reject")}catch(_:Exception){};assertArrayEquals(bytes,f.readBytes())}
  f.writeBytes(byteArrayOf(0xC3.toByte(),0x28));val bytes=f.readBytes();try{CallEstimate.read(ctx,number);fail("Must reject")}catch(_:Exception){};assertArrayEquals(bytes,f.readBytes())
 }
 @Test fun atomicBackupRecoveryAndConcurrentEditorsOnlyOneWins(){
  val d=CallEstimate.save(ctx,number,0,draft());f.renameTo(File(f.path+".bak"));f.writeText("unfinished");assertEquals(d,CallEstimate.read(ctx,number))
  val wins=java.util.concurrent.atomic.AtomicInteger();val errors=java.util.Collections.synchronizedList(mutableListOf<Throwable>())
  val threads=(1..2).map{i->Thread{try{CallEstimate.save(ctx,number,d.revision,d.copy(discount=i.toString()));wins.incrementAndGet()}catch(t:Throwable){errors.add(t)}}};threads.forEach{it.start()};threads.forEach{it.join()};assertEquals(1,wins.get());assertEquals(1,errors.size);assertTrue(errors.single() is IllegalArgumentException)
 }
 @Test fun largestValuesStayExactAndDuplicateIdsReject(){
  val r=CallEstimate.line("Large","1000000","1000000000");val t=CallEstimate.totals(CallEstimate.Draft(lines=(1..20).map{r.copy(id="$it")},tax="100"));assertEquals("40000000000000000.00",CallEstimate.money(t.total));bad{CallEstimate.totals(CallEstimate.Draft(lines=listOf(r,r)))}
 }
}
