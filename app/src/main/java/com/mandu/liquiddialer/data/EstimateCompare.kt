package com.mandu.liquiddialer.data

import java.math.BigDecimal

/** Pure what-if calculation. No file, contacts, call, or payment access. */
object EstimateCompare {
 data class Result(val draft:CallEstimate.Draft,val base:CallEstimate.Totals,val alternative:CallEstimate.Totals,val difference:BigDecimal)
 fun compare(base:CallEstimate.Draft,multiplier:String,discount:String,tax:String):Result {
  require(base.lines.isNotEmpty()){"Pehle ek item jodo"}
  val original=CallEstimate.totals(base)
  require(multiplier.length<=12&&Regex("[0-9]{1,7}(\\.[0-9]{1,3})?").matches(multiplier)){"Quantity multiplier digits aur dot mein, 3 decimals tak rakho"}
  val factor=BigDecimal(multiplier);require(factor>BigDecimal.ZERO&&factor<=BigDecimal("1000000")){"Multiplier 0 se bada aur 10,00,000 tak rakho"}
  val lines=base.lines.map{line->val q=BigDecimal(line.quantity)*factor
   require(q.stripTrailingZeros().scale()<=3){"${line.label}: nayi quantity mein 3 se zyada decimals hain. Rounding nahi ki gayi."}
   CallEstimate.line(line.label,q.stripTrailingZeros().toPlainString(),line.rate,line.id)
  }
  val next=base.copy(lines=lines,discount=discount,tax=tax)
  val totals=CallEstimate.totals(next)
  return Result(next,original,totals,totals.total-original.total)
 }
 fun direction(result:Result)=when(result.difference.signum()){-1->"Kam";1->"Zyada";else->"Barabar"}
}
