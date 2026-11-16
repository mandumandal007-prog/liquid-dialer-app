package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class ConverterRecallTest {
 private fun row(raw:String="1",from:String="m",to:String="ft")=CallConverter.convert(raw,from,to)
 @Test fun defaultsOff(){assertFalse(LdSettings().converterRecall);assertFalse(LdSettings().converterHistoryFold)}
 @Test fun originalNotRoundedResult(){val r=row();val x=ConverterRecall.input(r);assertEquals("1",x.value);assertNotEquals(r.value,x.value);assertEquals("m",x.from);assertEquals("ft",x.to)}
 @Test fun canonicalOriginal(){assertEquals("1.5",ConverterRecall.input(row("001.500000")).value)}
 @Test fun allSupportedPairs(){for(k in CallConverter.Kind.entries)for(a in CallConverter.of(k))for(b in CallConverter.of(k)){val x=ConverterRecall.input(row("17.123456",a.id,b.id));assertEquals(k,x.kind);assertEquals(a.id,x.from);assertEquals(b.id,x.to);assertEquals("17.123456",x.value)}}
 @Test fun negativeTemperature(){assertEquals("-40",ConverterRecall.input(row("-40","c","f")).value)}
 @Test fun absoluteZero(){assertEquals("-273.15",ConverterRecall.input(row("-273.15","c","k")).value)}
 @Test fun maxInput(){assertEquals("1000000000",ConverterRecall.input(row("1000000000","km","mm")).value)}
 @Test fun zeroKept(){assertEquals("0",ConverterRecall.input(row("0","kg","g")).value)}
 @Test fun fakeDisplayedResultNeverUsed(){assertEquals("1",ConverterRecall.input(row().copy(value="999999")).value)}
 @Test fun invalidOriginalRefused(){for(v in listOf("","1,000","+1","1e2","1.1234567","-1","1".repeat(25)))assertTrue(runCatching{ConverterRecall.input(row().copy(input=v))}.isFailure)}
 @Test fun invalidOriginalTemperatureRefused(){assertTrue(runCatching{ConverterRecall.input(row("0","c","k").copy(input="-274"))}.isFailure)}
 @Test fun mismatchedCategoryRefused(){assertTrue(runCatching{ConverterRecall.input(row().copy(to=CallConverter.unit("kg")))}.isFailure)}
 @Test fun unknownUnitRefused(){assertTrue(runCatching{ConverterRecall.input(row().copy(from=CallConverter.unit("m").copy(id="bad")))}.isFailure)}
 @Test fun changedUnitDefinitionRefused(){assertTrue(runCatching{ConverterRecall.input(row().copy(from=CallConverter.unit("m").copy(factor="2")))}.isFailure)}
 @Test fun rowAndHistoryNotMutated(){val h=CallConverter.History();val r=row();h.add(r);val before=h.rows;ConverterRecall.input(r);assertEquals(before,h.rows);assertEquals("3.28083989501312336",r.value)}
 @Test fun sameUnitsPreserved(){val x=ConverterRecall.input(row("2.5","kg","kg"));assertEquals("kg",x.from);assertEquals("kg",x.to)}
 @Test fun finderBothRoutes(){assertTrue(SettingsFinder.search("reuse conversion inputs").any{it.route=="converterhistory"});assertTrue(SettingsFinder.search("fold conversion history layout").any{it.route=="converterhistory"})}
}
