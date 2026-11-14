package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class EstimateLinesTest {
 private val base=CallEstimate.Draft(7,listOf(CallEstimate.line("Old item","2","10","a")),"2","10")
 private fun p(raw:String="Rice | 2.5 | 40\nOil | 1 | 120",b:CallEstimate.Draft=base)=EstimateLines.preview(b,raw)
 private fun refuse(f:()->Unit){try{f();fail("Should refuse")}catch(_:IllegalArgumentException){}}
 @Test fun twoLinesAppendInOrderKeepExistingFields(){val v=p();assertEquals(listOf("Old item","Rice","Oil"),v.result.lines.map{it.label});assertEquals(base.lines[0],v.result.lines[0]);assertEquals("2.5",v.rows[0].item.quantity);assertEquals("40",v.rows[0].item.rate);assertEquals(base.revision,v.result.revision);assertEquals(base.discount,v.result.discount);assertEquals(base.tax,v.result.tax)}
 @Test fun recalculatesWholeTotal(){assertEquals("261.80",CallEstimate.money(CallEstimate.totals(p().result).total))}
 @Test fun emptyCalculatorAccepted(){assertEquals("220.00",CallEstimate.money(CallEstimate.totals(p(b=CallEstimate.Draft()).result).total))}
 @Test fun blankLinesIgnoredButPhysicalNumbersKept(){val v=p("\n Rice | 2 | 4 \n \nOil|1|5\n");assertEquals(listOf(2,4),v.rows.map{it.sourceLine});assertEquals(3,v.blankLines)}
 @Test fun windowsAndCarriageLineEndingsAccepted(){assertEquals(3,p("A|1|1\r\nB|2|2\rC|3|3").rows.size)}
 @Test fun duplicateNamesKeptNotMerged(){val v=p("Rice|1|2\nRice|3|4");assertEquals(2,v.rows.size);assertNotEquals(v.rows[0].item.id,v.rows[1].item.id)}
 @Test fun oldNamesAlsoKeptSeparately(){assertEquals(2,p("Old item|1|2").result.lines.size)}
 @Test fun noHeaderOrExtraSeparatorOrMissingField(){for(s in listOf("Name|Quantity|Rate","A|1","A|1|2|3","A||2","|1|2","A|1|","A,1,2","A|1|2\nwrong"))refuse{p(s)}}
 @Test fun invalidQuantityRefusesWholeBatch(){for(s in listOf("0","-1","+2","1.0001","1000001","1e2","2kg","1,5"))refuse{p("Good|1|2\nBad|$s|3")};assertEquals(1,base.lines.size)}
 @Test fun invalidRatesRefuse(){for(s in listOf("-1","1.001","1000000001","Rs2","₹2","1,000","1e2"))refuse{p("A|1|$s")}}
 @Test fun maximumAndZeroRateAllowed(){val v=p("A|1000000|1000000000\nB|0.001|0");assertEquals(2,v.rows.size)}
 @Test fun halfUpLineAmountsUseExistingRules(){val v=p("A|0.001|5",CallEstimate.Draft());assertEquals("0.01",CallEstimate.money(CallEstimate.totals(v.result).total))}
 @Test fun longNamesAndControlsRefuse(){refuse{p("${"a".repeat(81)}|1|1")};refuse{p("bad\u0000|1|1")};refuse{p("bad\u001b|1|1")};refuse{p("bad\tname|1|1")}}
 @Test fun edgeTabsTrimButInternalQuantityTabsRefuse(){assertEquals("A",p("\tA\t|\t1\t|\t2\t").rows[0].item.label);refuse{p("A|1\t2|1")}}
 @Test fun emptyBlankAndInputBoundsRefuse(){for(s in listOf("","\n \n","x".repeat(8001),"\n".repeat(40)+"A|1|1"))refuse{p(s)}}
 @Test fun capacityExactThenOneExtraRefuses(){val b=CallEstimate.Draft(lines=(1..18).map{CallEstimate.line("Old$it","1","1","$it")});assertEquals(20,p(b=b).result.lines.size);refuse{p("A|1|1\nB|1|1\nC|1|1",b)}}
 @Test fun fullCalculatorRefusesNoTruncation(){val b=CallEstimate.Draft(lines=(1..20).map{CallEstimate.line("Old$it","1","1","$it")});refuse{p("A|1|1",b)}}
 @Test fun invalidSourceRefusesEvenValidNewItems(){refuse{p(b=base.copy(discount="21"))};refuse{p(b=base.copy(lines=base.lines+base.lines[0]))};refuse{p(b=base.copy(tax="101"))}}
 @Test fun applyValidNoBaseMutation(){val v=p();assertEquals(v.result,EstimateLines.apply(base,v));assertEquals(1,base.lines.size)}
 @Test fun staleSourceRefuses(){val v=p();for(b in listOf(base.copy(revision=8),base.copy(discount="1"),base.copy(tax="0"),base.copy(lines=base.lines.map{it.copy(quantity="3")}))){refuse{EstimateLines.apply(b,v)}}}
 @Test fun tamperedPreviewFieldsRefuse(){val v=p();refuse{EstimateLines.apply(base,v.copy(result=base))};refuse{EstimateLines.apply(base,v.copy(rows=emptyList()))};refuse{EstimateLines.apply(base,v.copy(blankLines=99))};refuse{EstimateLines.apply(base,v.copy(raw="A|1|1"))};refuse{EstimateLines.apply(base,v.copy(token="bad"))}}
 @Test fun repeatedPreviewUsesDifferentIdsButApplyStable(){val a=p();val b=p();assertNotEquals(a.token,b.token);assertEquals(a.result,EstimateLines.apply(base,a));assertEquals(a.rows.map{it.item.label},b.rows.map{it.item.label})}
 @Test fun maxPhysicalLinesAccepted(){assertEquals(39,p("A|1|1"+"\n".repeat(39)).blankLines)}
 @Test fun unicodeLabelsPreserved(){assertEquals("चावल के बैग",p("चावल के बैग | 2.5 | 45").rows[0].item.label)}
}
