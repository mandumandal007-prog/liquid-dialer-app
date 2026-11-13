package com.mandu.liquiddialer.data
import java.math.BigDecimal
import java.math.RoundingMode

/** Explicit decimal measurement arithmetic, no phone/storage/network access. */
object CallConverter {
 enum class Kind(val label:String){LENGTH("Length"),MASS("Mass"),VOLUME("Volume"),AREA("Area"),TEMPERATURE("Temp")}
 data class UnitDef(val id:String,val label:String,val kind:Kind,val factor:String)
 val units=listOf(
  UnitDef("mm","Millimeter (mm)",Kind.LENGTH,"0.001"),UnitDef("cm","Centimeter (cm)",Kind.LENGTH,"0.01"),UnitDef("m","Meter (m)",Kind.LENGTH,"1"),UnitDef("km","Kilometer (km)",Kind.LENGTH,"1000"),UnitDef("in","Inch (in)",Kind.LENGTH,"0.0254"),UnitDef("ft","Int'l foot (ft)",Kind.LENGTH,"0.3048"),
  UnitDef("mg","Milligram (mg)",Kind.MASS,"0.001"),UnitDef("g","Gram (g)",Kind.MASS,"1"),UnitDef("kg","Kilogram (kg)",Kind.MASS,"1000"),UnitDef("t","Metric tonne (t)",Kind.MASS,"1000000"),
  UnitDef("ml","Milliliter (mL)",Kind.VOLUME,"0.001"),UnitDef("l","Liter (L)",Kind.VOLUME,"1"),UnitDef("m3","Cubic meter (m³)",Kind.VOLUME,"1000"),
  UnitDef("cm2","Square cm (cm²)",Kind.AREA,"0.0001"),UnitDef("m2","Square meter (m²)",Kind.AREA,"1"),UnitDef("ft2","Int'l square foot (ft²)",Kind.AREA,"0.09290304"),UnitDef("ha","Hectare (ha)",Kind.AREA,"10000"),
  UnitDef("c","Celsius (°C)",Kind.TEMPERATURE,"1"),UnitDef("f","Fahrenheit (°F)",Kind.TEMPERATURE,"1"),UnitDef("k","Kelvin (K)",Kind.TEMPERATURE,"1")
 )
 fun of(kind:Kind)=units.filter{it.kind==kind}
 fun unit(id:String)=units.firstOrNull{it.id==id}?:error("Unknown unit")
 data class Result(val input:String,val from:UnitDef,val to:UnitDef,val value:String)
 private fun divide(a:BigDecimal,b:BigDecimal)=a.divide(b,40,RoundingMode.HALF_UP)
 fun convert(raw:String,fromId:String,toId:String):Result{
  val from=unit(fromId);val to=unit(toId);require(from.kind==to.kind){"Same category ke units chuno"}
  require(raw.length<=24&&Regex("-?[0-9]{1,10}(\\.[0-9]{1,6})?").matches(raw)){"Digits aur dot decimal, 6 decimals tak; comma/exponent nahi"}
  val n=BigDecimal(raw);require(n.abs()<=BigDecimal("1000000000")){"Absolute value 1,000,000,000 tak"}
  val value=if(from.kind==Kind.TEMPERATURE){
   val kelvin=when(from.id){"c"->n+BigDecimal("273.15");"f"->divide((n+BigDecimal("459.67"))*BigDecimal(5),BigDecimal(9));else->n}
   require(kelvin>=BigDecimal.ZERO){"Absolute zero se neeche temperature nahi"}
   when(to.id){"c"->kelvin-BigDecimal("273.15");"f"->divide(kelvin*BigDecimal(9),BigDecimal(5))-BigDecimal("459.67");else->kelvin}
  }else{require(n>=BigDecimal.ZERO){"Measurement 0 ya usse bada rakho"};divide(n*BigDecimal(from.factor),BigDecimal(to.factor))}
  val rounded=value.setScale(18,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
  return Result(n.stripTrailingZeros().toPlainString(),from,to,if(BigDecimal(rounded).signum()==0)"0"else rounded)
 }
 class History {private val saved=mutableListOf<Result>();val rows get()=saved.toList()
  fun add(result:Result){saved.add(0,result);while(saved.size>10)saved.removeAt(saved.lastIndex)}
  fun clear(){saved.clear()}
 }
}
