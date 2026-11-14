package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
class EstimateSplitTest {
 private fun draft(total:String)=CallEstimate.Draft(7,listOf(CallEstimate.line("Bags","1",total,"a")))
 private fun names(vararg weights:String)=weights.mapIndexed{i,w->EstimateSplit.Input("Share ${i+1}",w)}
 private fun p(total:String="10.00",weights:List<EstimateSplit.Input> = names("1","1","1"),equal:Boolean=true)=EstimateSplit.preview(draft(total),weights,equal)
 @Test fun exactEvenSplit(){assertEquals(listOf("5.00","5.00"),p(weights=names("1","1")).shares.map{it.amount.toPlainString()})}
 @Test fun equalRemainderVisibleOrder(){val x=p();assertEquals(listOf("3.34","3.33","3.33"),x.shares.map{it.amount.toPlainString()});assertEquals(listOf(true,false,false),x.shares.map{it.extraCent})}
 @Test fun tinyAmountMoreShares(){val x=p("0.02");assertEquals(listOf("0.01","0.01","0.00"),x.shares.map{it.amount.toPlainString()})}
 @Test fun zeroTotalAllowed(){assertTrue(p("0.00").shares.all{it.amount==BigDecimal("0.00")&&!it.extraCent})}
 @Test fun weightedExactRatios(){assertEquals(listOf("2.50","5.00","2.50"),p(weights=names("1","2","1"),equal=false).shares.map{it.amount.toPlainString()})}
 @Test fun largestRemainderNotFirstAlways(){val x=p("0.02",names("1","2","4"),false);assertEquals(listOf("0.00","0.01","0.01"),x.shares.map{it.amount.toPlainString()});assertTrue(x.shares[1].extraCent);assertFalse(x.shares[2].extraCent)}
 @Test fun tiesFollowOrderNotName(){val x=p("0.01",listOf(EstimateSplit.Input("Z"),EstimateSplit.Input("A")),false);assertEquals("0.01",x.shares.first().amount.toPlainString())}
 @Test fun equalIgnoresInvalidWeights(){assertEquals(p().shares,p(weights=names("bad","","-1")).shares)}
 @Test fun weightedInvalidValuesRefused(){for(v in listOf("","0","-1","+1"," 1","1 ","1.5","1,000","१","1000001","99999999999"))assertTrue(v,runCatching{p(weights=names("1",v),equal=false)}.isFailure)}
 @Test fun weightMaximumAndLeadingZeroValid(){val x=p("1.00",names("1000000","0000000001"),false);assertEquals(listOf("1.00","0.00"),x.shares.map{it.amount.toPlainString()})}
 @Test fun countsOutside2to12Refused(){for(n in listOf(0,1,13))assertTrue(runCatching{p(weights=(1..n).map{EstimateSplit.Input("$it")})}.isFailure)}
 @Test fun namesNormalizeAndDuplicatesRefused(){val x=p(weights=listOf(EstimateSplit.Input("  A   bags "),EstimateSplit.Input("B")));assertEquals("A bags",x.shares.first().name);assertTrue(runCatching{p(weights=listOf(EstimateSplit.Input("A bags"),EstimateSplit.Input(" a  BAGS ")))}.isFailure)}
 @Test fun nameBoundariesAndControls(){for(n in listOf(""," ","x".repeat(61),"A\nB","A\tB","A\u0000B"))assertTrue(runCatching{p(weights=listOf(EstimateSplit.Input(n),EstimateSplit.Input("B")))}.isFailure);assertEquals(60,p(weights=listOf(EstimateSplit.Input("x".repeat(60)),EstimateSplit.Input("B"))).shares.first().name.length)}
 @Test fun unicodeNamesKept(){assertEquals("घर के सामान",p(weights=listOf(EstimateSplit.Input("घर के सामान"),EstimateSplit.Input("Other"))).shares.first().name)}
 @Test fun invalidOrEmptySourceRefused(){for(d in listOf(CallEstimate.Draft(),draft("10").copy(discount="11"),draft("10").copy(tax="101")))assertTrue(runCatching{EstimateSplit.preview(d,names("1","1"),true)}.isFailure)}
 @Test fun usesRoundedTotalAfterDiscountTax(){val d=CallEstimate.Draft(4,listOf(CallEstimate.line("A","0.333","1.00","a")),"0.03","10");val x=EstimateSplit.preview(d,names("1","1"),true);assertEquals("0.33",x.total.toPlainString());assertEquals(listOf("0.17","0.16"),x.shares.map{it.amount.toPlainString()})}
 @Test fun hugeLegalEstimateDoesNotOverflow(){val d=CallEstimate.Draft(9,(1..20).map{CallEstimate.line("Item $it","1000000","1000000000","$it")},"0","100");val x=EstimateSplit.preview(d,(1..12).map{EstimateSplit.Input("$it","1000000")},false);assertEquals("40000000000000000.00",x.total.toPlainString());assertEquals(x.total,x.shares.fold(BigDecimal("0.00")){a,b->a+b.amount})}
 @Test fun conservationAcrossCentsCountsAndWeights(){for(cents in 0..99)for(n in 2..12){val x=p(BigDecimal(cents).movePointLeft(2).toPlainString(),(1..n).map{EstimateSplit.Input("$it","${it*7}")},false);assertEquals(x.total,x.shares.fold(BigDecimal("0.00")){a,b->a+b.amount});assertTrue(x.shares.all{it.amount.scale()==2&&it.amount>=BigDecimal.ZERO})}}
 @Test fun sourceNeverMutatedAndIdentityPreserved(){val d=draft("10");val x=EstimateSplit.preview(d,names("1","2"),false);assertEquals(d,x.source);assertEquals(7L,d.revision);assertEquals("10",d.lines.single().rate)}
 @Test fun inputOrderPreservedAndWeightsNotPercents(){val x=p("12.00",names("3","2","1"),false);assertEquals(listOf("6.00","4.00","2.00"),x.shares.map{it.amount.toPlainString()});assertEquals(listOf(3,2,1),x.shares.map{it.weight})}
}
