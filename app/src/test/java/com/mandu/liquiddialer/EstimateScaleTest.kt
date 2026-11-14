package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class EstimateScaleTest {
 private val base=CallEstimate.Draft(7,listOf(CallEstimate.line("Rice","2","10","a"),CallEstimate.line("Oil","3","4","b")),"2","10")
 private fun p(d:CallEstimate.Draft=base,s:Set<String> =setOf("a"),n:String="3",q:String="2")=EstimateScale.preview(d,s,n,q)
 private fun refuse(f:()->Unit){try{f();fail("Should refuse")}catch(_:IllegalArgumentException){}}
 @Test fun selectedOnlyRatesIdsOrderAndRevisionKept(){val r=p().result;assertEquals("3",r.lines[0].quantity);assertEquals(base.lines[1],r.lines[1]);assertEquals(base.revision,r.revision);assertEquals(base.lines.map{it.id},r.lines.map{it.id});assertEquals(base.lines.map{it.rate},r.lines.map{it.rate})}
 @Test fun fullTotalsIncludeUnchangedDiscountAndTax(){val v=p();assertEquals("44.00",CallEstimate.money(CallEstimate.totals(v.result).total));assertEquals("2",v.result.discount);assertEquals("10",v.result.tax)}
 @Test fun allRowsScaleTogether(){val v=p(s=setOf("a","b"));assertEquals(listOf("3","4.5"),v.result.lines.map{it.quantity});assertEquals(2,v.changes.size)}
 @Test fun exactThreeDecimalsAllowed(){val b=base.copy(lines=listOf(CallEstimate.line("Fine","0.001","1","a")),discount="0");assertEquals("0.003",p(b,n="3",q="1").result.lines[0].quantity)}
 @Test fun fractionBeyondThreeDecimalsRefusesWhole(){val b=base.copy(lines=base.lines+CallEstimate.line("Fine","0.001","1","c"),discount="0");refuse{p(b,setOf("a","c"),"1","2")};assertEquals("2",b.lines[0].quantity)}
 @Test fun repeatingFractionRefuses(){refuse{p(n="1",q="3")}}
 @Test fun exactReductionAllowed(){assertEquals("1",p(n="1",q="2").result.lines[0].quantity)}
 @Test fun maxQuantityAllowed(){assertEquals("1000000",p(n="500000",q="1").result.lines[0].quantity)}
 @Test fun overflowAnySelectedRefusesWhole(){refuse{p(s=setOf("a","b"),n="1000000",q="1")};assertEquals("2",base.lines[0].quantity)}
 @Test fun zeroNegativeDecimalSignedSpaceAndHugeRatioRefuse(){for(s in listOf("0","-1","1.5","+2"," 2","1 ","1000001","99999999999999999","","1e2","2,0")){refuse{p(n=s)};refuse{p(q=s)}}}
 @Test fun equivalentRatioRefusesNoChange(){refuse{p(n="2",q="2")};refuse{p(n="0002",q="2")}}
 @Test fun emptyOrUnknownSelectionRefuses(){refuse{p(s=emptySet())};refuse{p(s=setOf("missing"))};refuse{p(s=setOf("a","missing"))}}
 @Test fun duplicateSourceIdRefuses(){refuse{p(base.copy(lines=base.lines+base.lines[0]))}}
 @Test fun emptyAndMalformedSourceRefuse(){refuse{p(CallEstimate.Draft())};refuse{p(base.copy(lines=listOf(base.lines[0].copy(quantity="bad"))))};refuse{p(base.copy(tax="101"))}}
 @Test fun discountExceedingNewSubtotalRefuses(){refuse{p(base.copy(discount="30"),setOf("a","b"),"1","2")}}
 @Test fun zeroRatesAllowed(){val b=CallEstimate.Draft(lines=listOf(CallEstimate.line("Free","1","0","a")));assertEquals("0.00",CallEstimate.money(CallEstimate.totals(p(b).result).total))}
 @Test fun correctPreviewAppliesNoFileApi(){val v=p();assertEquals(v.result,EstimateScale.apply(base,v));assertEquals("2",base.lines[0].quantity)}
 @Test fun staleAnySourceFieldRefuses(){val v=p();for(b in listOf(base.copy(revision=8),base.copy(discount="1"),base.copy(tax="0"),base.copy(lines=base.lines.reversed()),base.copy(lines=base.lines.map{it.copy(label="Other")}))){refuse{EstimateScale.apply(b,v)}}}
 @Test fun tamperedResultChangesAndRatioRefuse(){val v=p();refuse{EstimateScale.apply(base,v.copy(result=base))};refuse{EstimateScale.apply(base,v.copy(changes=emptyList()))};refuse{EstimateScale.apply(base,v.copy(numerator="4"))};refuse{EstimateScale.apply(base,v.copy(selected=setOf("b")))}}
 @Test fun twentyRowsAndLargeAmountsRemainExact(){val b=CallEstimate.Draft(lines=(1..20).map{CallEstimate.line("Row $it","500000","1000000000","$it")},tax="100");val v=p(b,b.lines.map{it.id}.toSet(),"2","1");assertEquals("40000000000000000.00",CallEstimate.money(CallEstimate.totals(v.result).total))}
 @Test fun lineRoundingUsesExistingCalculatorRules(){val b=CallEstimate.Draft(lines=listOf(CallEstimate.line("Fine","0.001","5","a")));val v=p(b,n="3",q="1");assertEquals("0.01",CallEstimate.money(CallEstimate.totals(b).total));assertEquals("0.02",CallEstimate.money(CallEstimate.totals(v.result).total))}
 @Test fun callerSelectionMutationCannotChangePreview(){val selected=mutableSetOf("a");val v=p(s=selected);selected.clear();assertEquals(setOf("a"),v.selected);assertEquals(v.result,EstimateScale.apply(base,v))}
}
