package com.mandu.liquiddialer.data
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

/** Read-only arithmetic over a calculator draft, never an actual obligation or payment. */
object EstimateSplit {
 const val MAX_SHARES=12
 data class Input(val name:String,val weight:String="1")
 data class Share(val name:String,val weight:Int,val amount:BigDecimal,val extraCent:Boolean)
 data class Preview(val source:CallEstimate.Draft,val equal:Boolean,val total:BigDecimal,val shares:List<Share>)
 fun preview(source:CallEstimate.Draft,inputs:List<Input>,equal:Boolean):Preview {
  require(source.lines.isNotEmpty()){"Add an estimate item first"}
  val total=CallEstimate.totals(source).total
  require(inputs.size in 2..MAX_SHARES){"Use 2-12 shares"}
  val names=hashSetOf<String>()
  val parsed=inputs.map{r->
   val name=r.name.trim().replace(Regex("\\s+")," ")
   require(r.name.none{it.isISOControl()}&&name.length in 1..60){"Each share needs a name of 1-60 characters, without controls"}
   require(names.add(name.lowercase(Locale.ROOT))){"Use a different name for each share"}
   val weight=if(equal)1 else CallTally.step(r.weight).toInt()
   name to weight
  }
  val cents=total.movePointRight(2).toBigIntegerExact();val denominator=BigInteger.valueOf(parsed.sumOf{it.second.toLong()})
  val division=parsed.map{(_,w)->(cents*BigInteger.valueOf(w.toLong())).divideAndRemainder(denominator)}
  val spare=(cents-division.fold(BigInteger.ZERO){a,b->a+b[0]}).intValueExact()
  require(spare in 0 until inputs.size)
  val extra=division.indices.sortedWith(compareByDescending<Int>{division[it][1]}.thenBy{it}).take(spare).toSet()
  val shares=parsed.mapIndexed{i,(name,w)->Share(name,w,BigDecimal(division[i][0]+if(i in extra)BigInteger.ONE else BigInteger.ZERO,2),i in extra)}
  require(shares.fold(BigDecimal("0.00")){a,b->a+b.amount}==total)
  return Preview(source.copy(lines=source.lines.toList()),equal,total,shares)
 }
}
