package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class ConverterTableTest {
 private fun value(raw:String,from:String,to:String)=ConverterTable.create(raw,from).rows.single{it.to.id==to}.value
 @Test fun defaultsOff(){assertFalse(LdSettings().converterTable);assertFalse(LdSettings().converterFold)}
 @Test fun allAndOnlyCategory(){for(u in CallConverter.units){val x=ConverterTable.create("1",u.id);assertEquals(CallConverter.of(u.kind).map{it.id},x.rows.map{it.to.id});assertTrue(x.rows.all{it.from==u&&it.to.kind==u.kind})}}
 @Test fun sourceIncluded(){assertEquals("2.5",value("2.500000","m","m"))}
 @Test fun massExact(){assertEquals("2500",value("2.5","kg","g"));assertEquals("0.0025",value("2.5","kg","t"))}
 @Test fun lengthInternationalFoot(){assertEquals("0.3048",value("1","ft","m"));assertEquals("12",value("1","ft","in"))}
 @Test fun volumeExact(){assertEquals("1000",value("1","m3","l"))}
 @Test fun areaExact(){assertEquals("0.09290304",value("1","ft2","m2"))}
 @Test fun temperatureAll(){val x=ConverterTable.create("0","c");assertEquals(listOf("0","32","273.15"),x.rows.map{it.value})}
 @Test fun fahrenheit(){assertEquals("37",value("98.6","f","c"))}
 @Test fun absoluteZeroAllowed(){assertEquals("0",value("-273.15","c","k"))}
 @Test fun belowAbsoluteZeroRefused(){assertTrue(runCatching{ConverterTable.create("-273.150001","c")}.isFailure)}
 @Test fun negativeNonTempRefused(){for(u in listOf("m","kg","l","m2"))assertTrue(runCatching{ConverterTable.create("-1",u)}.isFailure)}
 @Test fun zeroNoNegativeZero(){for(u in listOf("m","kg","l","m2"))assertTrue(ConverterTable.create("-0",u).rows.all{it.value=="0"})}
 @Test fun badNumericRefusedWhole(){for(v in listOf(""," 1","1 ","+1",".1","1.","1,000","1e2","NaN","१","1.1234567"))assertTrue(v,runCatching{ConverterTable.create(v,"m")}.isFailure)}
 @Test fun maxInputAndNoOverflow(){assertEquals("1000000000000000",value("1000000000","km","mm"));assertTrue(runCatching{ConverterTable.create("1000000001","m")}.isFailure)}
 @Test fun longInputNotTruncated(){assertTrue(runCatching{ConverterTable.create("1".repeat(25),"m")}.isFailure)}
 @Test fun unknownUnitRefused(){for(u in listOf("","gallon","bigha","usd","KG"))assertTrue(runCatching{ConverterTable.create("1",u)}.isFailure)}
 @Test fun rowsExactlyExistingConverter(){for(u in CallConverter.units){val x=ConverterTable.create("17.123456",u.id);for(r in x.rows)assertEquals(CallConverter.convert("17.123456",u.id,r.to.id),r)}}
 @Test fun tableInputCanonical(){assertEquals("1.5",ConverterTable.create("001.500000","m").input)}
 @Test fun roundedEighteenDecimals(){assertEquals("3.28083989501312336",value("1","m","ft"))}
 @Test fun uniqueBoundedRows(){for(k in CallConverter.Kind.entries){val x=ConverterTable.create("1",CallConverter.of(k).first().id);assertEquals(x.rows.size,x.rows.map{it.to.id}.distinct().size);assertTrue(x.rows.size in 3..6)}}
 @Test fun finderRoutes(){assertTrue(SettingsFinder.search("converter all-units table").any{it.route=="converteroptions"});assertTrue(SettingsFinder.search("fold unit chooser layout").any{it.route=="converteroptions"})}
}
