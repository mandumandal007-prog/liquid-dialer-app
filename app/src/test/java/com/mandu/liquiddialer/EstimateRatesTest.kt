package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class EstimateRatesTest{
 private val base=CallEstimate.Draft(7,listOf(CallEstimate.line("Rice","2","10.00","a"),CallEstimate.line("Oil","3","4.00","b")),"2","10")
 private fun p(b:CallEstimate.Draft=base,s:Set<String> =setOf("a"),percent:String="10",down:Boolean=false)=EstimateRates.preview(b,s,percent,down)
 private fun bad(f:()->Unit){try{f();fail("Must refuse")}catch(_:IllegalArgumentException){}}
 @Test fun defaultsOff(){assertFalse(LdSettings().estimateRates)}
 @Test fun selectedOnly(){val v=p();assertEquals("11.00",v.result.lines[0].rate);assertEquals(base.lines[1],v.result.lines[1]);assertEquals("10.00",base.lines[0].rate)}
 @Test fun quantitiesNamesIdsOrderRevisionKept(){val r=p().result;assertEquals(base.revision,r.revision);assertEquals(base.lines.map{Triple(it.id,it.label,it.quantity)},r.lines.map{Triple(it.id,it.label,it.quantity)})}
 @Test fun wholeTotalsAndDiscountTaxKept(){val r=p().result;assertEquals("2",r.discount);assertEquals("10",r.tax);assertEquals("35.20",CallEstimate.money(CallEstimate.totals(r).total))}
 @Test fun decrease(){assertEquals("9.00",p(down=true).result.lines[0].rate)}
 @Test fun allSelected(){assertEquals(listOf("11.00","4.40"),p(s=setOf("a","b")).result.lines.map{it.rate})}
 @Test fun hundredPercentDecreaseZero(){val r=p(b=base.copy(discount="0"),s=setOf("a","b"),percent="100",down=true).result;assertEquals(listOf("0.00","0.00"),r.lines.map{it.rate});assertEquals("0.00",CallEstimate.money(CallEstimate.totals(r).total))}
 @Test fun maximumIncrease(){assertEquals("110.00",p(percent="1000").result.lines[0].rate)}
 @Test fun twoDecimalPercentExact(){assertEquals("100.01",p(base.copy(lines=listOf(CallEstimate.line("X","1","100","a")),discount="0"),percent="0.01").result.lines[0].rate)}
 @Test fun fractionalCentRefusesWhole(){bad{p(base.copy(lines=base.lines+CallEstimate.line("Tiny","1","0.01","c")),setOf("a","c"))};assertEquals("10.00",base.lines[0].rate)}
 @Test fun noSilentRounding(){bad{p(base.copy(lines=listOf(CallEstimate.line("X","1","0.05","a")),discount="0"),percent="10")}}
 @Test fun zeroRatesCanStayWithChangedRate(){val b=base.copy(lines=base.lines+CallEstimate.line("Free","1","0","c"));val r=p(b,setOf("a","c")).result;assertEquals("0.00",r.lines[2].rate)}
 @Test fun allZeroNoOpRefused(){bad{p(CallEstimate.Draft(lines=listOf(CallEstimate.line("Free","1","0","a"))))}}
 @Test fun percentageMalformedRefused(){for(x in listOf("","0","0.00","-1","+1"," 10","10 ","1e2","1,2",".5","1.","1.001","10000","１２","999999999"))bad{p(percent=x)}}
 @Test fun limitsRefused(){bad{p(percent="1000.01")};bad{p(percent="100.01",down=true)}}
 @Test fun discountUnderflowRefused(){bad{p(base.copy(discount="30"),setOf("a","b"),"50",true)}}
 @Test fun maximumRateAccepted(){val b=base.copy(lines=listOf(CallEstimate.line("X","1","500000000","a")),discount="0");assertEquals("1000000000.00",p(b,percent="100").result.lines[0].rate)}
 @Test fun rateOverflowRefuses(){val b=base.copy(lines=listOf(CallEstimate.line("X","1","1000000000","a")),discount="0");bad{p(b)}}
 @Test fun emptyUnknownSelectionRefused(){bad{p(s=emptySet())};bad{p(s=setOf("missing"))};bad{p(s=setOf("a","missing"))}}
 @Test fun invalidSourceRefused(){bad{p(CallEstimate.Draft())};bad{p(base.copy(lines=base.lines+base.lines[0]))};bad{p(base.copy(tax="101"))};bad{p(base.copy(lines=listOf(base.lines[0].copy(quantity="bad"))))}}
 @Test fun correctApplyNoStorageApi(){val v=p();assertEquals(v.result,EstimateRates.apply(base,v))}
 @Test fun staleAllSourceFieldsRefused(){val v=p();for(b in listOf(base.copy(revision=8),base.copy(discount="1"),base.copy(tax="0"),base.copy(lines=base.lines.reversed()),base.copy(lines=base.lines.map{it.copy(label="Changed")}))){bad{EstimateRates.apply(b,v)}}}
 @Test fun tamperedPreviewRefused(){val v=p();bad{EstimateRates.apply(base,v.copy(result=base))};bad{EstimateRates.apply(base,v.copy(changes=emptyList()))};bad{EstimateRates.apply(base,v.copy(percent="20"))};bad{EstimateRates.apply(base,v.copy(decrease=true))};bad{EstimateRates.apply(base,v.copy(selected=setOf("b")))}}
 @Test fun twentyLinesLargeTotals(){val b=CallEstimate.Draft(lines=(1..20).map{CallEstimate.line("Line$it","1000000","500000000","$it")},tax="100");val r=p(b,b.lines.map{it.id}.toSet(),"100").result;assertEquals("40000000000000000.00",CallEstimate.money(CallEstimate.totals(r).total))}
 @Test fun lineAndTaxRoundExistingRules(){val b=CallEstimate.Draft(lines=listOf(CallEstimate.line("Fine","0.001","5","a")),tax="100");val r=p(b,percent="200").result;assertEquals("0.04",CallEstimate.money(CallEstimate.totals(r).total))}
 @Test fun mutableSelectionCopied(){val selected=mutableSetOf("a");val v=p(s=selected);selected.clear();assertEquals(setOf("a"),v.selected);assertEquals(v.result,EstimateRates.apply(base,v))}
 @Test fun everyLineChangeListsQuantity(){val v=p(s=setOf("a","b"));assertEquals(base.lines.map{it.quantity},v.changes.map{it.quantity});assertEquals(base.lines.map{it.rate},v.changes.map{it.before})}
 @Test fun finderRoute(){assertTrue(SettingsFinder.search("rate percentage").any{it.id=="estimate-rates"&&it.route=="estimaterates"})}
}
