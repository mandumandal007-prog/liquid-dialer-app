package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import java.math.BigDecimal
import org.junit.Test
import org.junit.Assert.*
class InstallmentPreviewTest {
 private fun draft(total:String="10")=CallEstimate.Draft(7,listOf(CallEstimate.line("Rice","1",total,"a")))
 private fun p(total:String="10",up:String="0",n:String="3",first:String="2026-11-16",gap:String="30")=InstallmentPreview.preview(draft(total),up,n,first,gap)
 @Test fun balancedExactCents(){assertEquals(listOf("3.34","3.33","3.33"),p().parts.map{it.amount.toPlainString()})}
 @Test fun remainderFollowsEarliestParts(){assertEquals(listOf(true,true,false),p("0.05").parts.map{it.extraCent})}
 @Test fun upfrontTakenFromTotal(){val x=p(up="1");assertEquals("9.00",x.remaining.toPlainString());assertEquals(listOf("3.00","3.00","3.00"),x.parts.map{it.amount.toPlainString()})}
 @Test fun exactOneCentEach(){assertTrue(p("0.03").parts.all{it.amount==BigDecimal("0.01")})}
 @Test fun cannotCreateZeroParts(){assertTrue(runCatching{p("0.02")}.isFailure)}
 @Test fun zeroTotalRefused(){assertTrue(runCatching{p("0")}.isFailure)}
 @Test fun upfrontEqualOrMoreRefused(){for(v in listOf("10","11"))assertTrue(runCatching{p(up=v)}.isFailure)}
 @Test fun negativeAndSignsRefused(){for(v in listOf("-1","+1"," 1","1 "))assertTrue(runCatching{p(up=v)}.isFailure)}
 @Test fun decimalAndScientificRefused(){for(v in listOf(".1","1.","1.001","1,00","1e0","NaN","१"))assertTrue(runCatching{p(up=v)}.isFailure)}
 @Test fun inputLengthBounded(){for(v in listOf("", "0".repeat(18),"9".repeat(80)))assertTrue(runCatching{p(up=v)}.isFailure)}
 @Test fun leadingZeroAllowed(){assertEquals("1.50",p(up="001.50").upfront.toPlainString())}
 @Test fun countsBounded(){for(v in listOf("0","37","100","-1","+1","1.0",""," 2","२"))assertTrue(v,runCatching{p(n=v)}.isFailure)}
 @Test fun intervalBounded(){for(v in listOf("0","3651","10000","-1","+1","1.5",""," 2","२"))assertTrue(v,runCatching{p(gap=v)}.isFailure)}
 @Test fun onePartUsesFirstDate(){val x=p(n="1");assertEquals("10.00",x.parts.single().amount.toPlainString());assertEquals("2026-11-16",x.parts.single().date.toString())}
 @Test fun firstDateIsFirstPart(){assertEquals(listOf("2026-11-16","2026-12-16","2027-01-15"),p().parts.map{it.date.toString()})}
 @Test fun notMonthlyAndWeekendsIncluded(){assertEquals(listOf("2026-01-31","2026-03-02","2026-04-01"),p(first="2026-01-31").parts.map{it.date.toString()});assertEquals("2026-11-15",p(first="2026-11-14",gap="1").parts[1].date.toString())}
 @Test fun leapDayArithmetic(){assertEquals("2028-02-29",p(first="2028-02-28",gap="1").parts[1].date.toString())}
 @Test fun invalidDateRefused(){for(v in listOf("2026-02-29","2026-13-01","2026-2-01"," 2026-11-16","1899-12-31","2200-01-01"))assertTrue(v,runCatching{p(first=v)}.isFailure)}
 @Test fun lastDateRangeRefused(){assertTrue(runCatching{p(first="2199-12-31",gap="1")}.isFailure)}
 @Test fun upperBoundarySingleAllowed(){assertEquals("2199-12-31",p(first="2199-12-31",n="1",gap="3650").parts.single().date.toString())}
 @Test fun lowerBoundaryAllowed(){assertEquals("1900-01-01",p(first="1900-01-01").parts.first().date.toString())}
 @Test fun thirtySixParts(){val x=p(n="36",gap="3000",first="1900-01-01");assertEquals(36,x.parts.size);assertEquals((1..36).toList(),x.parts.map{it.index})}
 @Test fun hugeTotalNoOverflow(){val d=CallEstimate.Draft(lines=(1..20).map{CallEstimate.line("Item$it","1000000","1000000000","$it")},tax="100");val x=InstallmentPreview.preview(d,"0","36","2026-11-16","1");assertEquals("40000000000000000.00",x.total.toPlainString());assertEquals(x.total,x.parts.fold(x.upfront){a,b->a+b.amount})}
 @Test fun roundedDiscountTaxTotalUsed(){val d=CallEstimate.Draft(lines=listOf(CallEstimate.line("A","0.333","1","a")),discount="0.03",tax="10");assertEquals("0.33",InstallmentPreview.preview(d,"0","3","2026-11-16","1").total.toPlainString())}
 @Test fun sourceUnchanged(){val d=draft();val before=d.copy(lines=d.lines.toList());InstallmentPreview.preview(d,"1","3","2026-11-16","7");assertEquals(before,d)}
 @Test fun invalidSourceRefused(){for(d in listOf(CallEstimate.Draft(),draft().copy(discount="11"),draft().copy(tax="101")))assertTrue(runCatching{InstallmentPreview.preview(d,"0","3","2026-11-16","7")}.isFailure)}
 @Test fun conservationAndDifferenceAtMostOneCent(){for(c in 1..100)for(n in 1..minOf(c,36)){val x=p(BigDecimal(c).movePointLeft(2).toPlainString(),n="$n");assertEquals(x.total,x.parts.fold(x.upfront){a,b->a+b.amount});assertTrue(x.parts.first().amount-x.parts.last().amount<=BigDecimal("0.01"));assertTrue(x.parts.all{it.amount.scale()==2&&it.amount>BigDecimal.ZERO})}}
}
