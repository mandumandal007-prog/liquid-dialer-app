package com.mandu.liquiddialer.data
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

/** Hypothetical arithmetic only. No loan, payment, clock, calendar or persistence access. */
object InstallmentPreview {
 data class Part(val index:Int,val date:LocalDate,val amount:BigDecimal,val extraCent:Boolean)
 data class Preview(val total:BigDecimal,val upfront:BigDecimal,val remaining:BigDecimal,val interval:Int,val parts:List<Part>)
 fun preview(source:CallEstimate.Draft,upfront:String,count:String,first:String,interval:String):Preview {
  require(source.lines.isNotEmpty()){"Add an estimate item first"}
  val total=CallEstimate.totals(source).total
  require(Regex("[0-9]{1,17}(\\.[0-9]{1,2})?").matches(upfront)){"Upfront: digits and dot, up to 2 decimals; no comma or sign"}
  val initial=BigDecimal(upfront).setScale(2)
  require(initial<total){"Upfront must be less than estimate total"}
  require(Regex("[0-9]{1,2}").matches(count)&&count.toInt() in 1..36){"Use 1-36 remaining parts"}
  require(Regex("[0-9]{1,4}").matches(interval)&&interval.toInt() in 1..3650){"Interval: 1-3650 calendar days"}
  val n=count.toInt();val gap=interval.toInt();val start=CallDates.date(first)
  val remaining=total-initial;val cents=remaining.movePointRight(2).toBigIntegerExact()
  require(cents>=BigInteger.valueOf(n.toLong())){"Each remaining part must be at least 0.01"}
  val division=cents.divideAndRemainder(BigInteger.valueOf(n.toLong()));val extra=division[1].intValueExact()
  val end=start.plusDays((n-1).toLong()*gap)
  require(end.year in 1900..2199){"Last part is outside 1900-2199; reduce interval or count"}
  val parts=(0 until n).map{i->Part(i+1,start.plusDays(i.toLong()*gap),BigDecimal(division[0]+if(i<extra)BigInteger.ONE else BigInteger.ZERO,2),i<extra)}
  check(parts.fold(initial){a,b->a+b.amount}==total)
  return Preview(total,initial,remaining,gap,parts)
 }
}
