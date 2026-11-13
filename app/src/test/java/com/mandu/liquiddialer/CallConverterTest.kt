package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.CallConverter as C
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
class CallConverterTest {
 private fun c(n:String,a:String,b:String)=C.convert(n,a,b).value
 @Test fun metricLength(){assertEquals("1000",c("1","km","m"));assertEquals("0.01",c("10","mm","m"));assertEquals("100",c("1","m","cm"))}
 @Test fun exactInchAndFoot(){assertEquals("2.54",c("1","in","cm"));assertEquals("0.3048",c("1","ft","m"));assertEquals("12",c("1","ft","in"))}
 @Test fun metricMass(){assertEquals("1000",c("1","kg","g"));assertEquals("0.001",c("1","mg","g"));assertEquals("1000",c("1","t","kg"))}
 @Test fun metricVolume(){assertEquals("1000",c("1","l","ml"));assertEquals("1000",c("1","m3","l"));assertEquals("0.000001",c("1","ml","m3"))}
 @Test fun areaSquaresNotLengthFactors(){assertEquals("10000",c("1","m2","cm2"));assertEquals("0.09290304",c("1","ft2","m2"));assertEquals("10000",c("1","ha","m2"))}
 @Test fun temperatureFreezingAndBoiling(){assertEquals("32",c("0","c","f"));assertEquals("212",c("100","c","f"));assertEquals("0",c("32","f","c"));assertEquals("273.15",c("0","c","k"))}
 @Test fun kelvinAndNegativeTemperature(){assertEquals("-273.15",c("0","k","c"));assertEquals("-459.67",c("0","k","f"));assertEquals("-40",c("-40","c","f"));assertEquals("310.15",c("98.6","f","k"))}
 @Test fun absoluteZeroExactBoundary(){assertEquals("0",c("-273.15","c","k"));assertEquals("0",c("-459.67","f","k"));listOf(Triple("-273.150001","c","k"),Triple("-459.670001","f","k"),Triple("-0.000001","k","c")).forEach{assertTrue(runCatching{c(it.first,it.second,it.third)}.isFailure)}}
 @Test fun noNegativePhysicalMeasurements(){for(k in C.Kind.entries.filter{it!=C.Kind.TEMPERATURE}){val u=C.of(k);assertTrue(runCatching{c("-1",u[0].id,u[1].id)}.isFailure)}}
 @Test fun strictInputRejectsLocaleExponentAndWhitespace(){for(n in listOf(""," 1","1 ","1,000","1e3","NaN","Infinity","1.",".5","+1","--1","1.1234567","१"))assertTrue(n,runCatching{c(n,"m","ft")}.isFailure)}
 @Test fun magnitudeBoundedWithoutOverflow(){assertEquals("1000000000000",c("1000000000","km","m"));assertTrue(runCatching{c("1000000001","m","ft")}.isFailure);assertTrue(runCatching{c("9999999999999999999999999999","m","ft")}.isFailure)}
 @Test fun crossCategoryAndUnknownRejected(){assertTrue(runCatching{c("1","kg","l")}.isFailure);assertTrue(runCatching{c("1","cup","l")}.isFailure)}
 @Test fun decimalArithmeticNotBinaryFloat(){assertEquals("300",c("0.3","kg","g"));assertEquals("0.000000001",c("0.000001","mm","m"))}
 @Test fun repeatingOutputHas18PlacesHalfUp(){assertEquals("3.28083989501312336",c("1","m","ft"));assertEquals("0.555555555555555556",c("33","f","c"))}
 @Test fun sameUnitPreservesInputWithoutRounding(){for(u in C.units){val n=if(u.kind==C.Kind.TEMPERATURE)"123.123456"else"1.123456";assertEquals(n,c(n,u.id,u.id))}}
 @Test fun zeroCanonicalNotNegative(){assertEquals("0",c("-0.000000","m","ft"));assertEquals("0",c("0","g","kg"))}
 @Test fun unitIdsAndCategoryDefinitionsUnique(){assertEquals(C.units.size,C.units.map{it.id}.distinct().size);assertEquals(5,C.Kind.entries.size);C.Kind.entries.forEach{assertTrue(C.of(it).size>=2)};assertEquals(0,BigDecimal(C.unit("ft").factor).pow(2).compareTo(BigDecimal(C.unit("ft2").factor)))}
 @Test fun conversionsRoundTripAtInputPrecision(){for(k in C.Kind.entries){val us=C.of(k);for(a in us)for(b in us){val n=if(k==C.Kind.TEMPERATURE)"100"else"1";val value=BigDecimal(c(n,a.id,b.id));assertTrue(value.toPlainString().length<80);if(a.id==b.id)assertEquals(n,value.stripTrailingZeros().toPlainString())}}}
 @Test fun historyNewestFirstBoundedAndClear(){val h=C.History();repeat(13){h.add(C.convert(it.toString(),"m","cm"))};assertEquals(10,h.rows.size);assertEquals("12",h.rows.first().input);assertEquals("3",h.rows.last().input);val snapshot=h.rows;h.clear();assertTrue(h.rows.isEmpty());assertEquals(10,snapshot.size)}
 @Test fun leadingZerosNormalizeButInputStillBounded(){assertEquals("1500",c("0001.500000","kg","g"))}
}
