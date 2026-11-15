package com.mandu.liquiddialer.data
import java.math.BigDecimal
import java.math.RoundingMode

/** Manual arithmetic only. Rates must stay exact to cents; there is no silent rate rounding. */
object EstimateRates {
 data class Change(val id:String,val label:String,val before:String,val after:String,val quantity:String)
 data class Preview(val source:CallEstimate.Draft,val selected:Set<String>,val percent:String,val decrease:Boolean,val changes:List<Change>,val result:CallEstimate.Draft)
 fun preview(base:CallEstimate.Draft,selected:Set<String>,percent:String,decrease:Boolean):Preview {
  require(base.lines.isNotEmpty()){ "Add an estimate item first" };CallEstimate.totals(base)
  require(selected.isNotEmpty()&&selected.all{id->base.lines.any{it.id==id}}){"Select current estimate items"}
  require(percent.length<=7&&Regex("[0-9]{1,4}(\\.[0-9]{1,2})?").matches(percent)){"Use a positive percentage with at most 2 decimals"}
  val p=BigDecimal(percent);require(p>BigDecimal.ZERO&&p<=BigDecimal(if(decrease)"100"else"1000")){"Increase: >0 to 1000%. Decrease: >0 to 100%."}
  val factor=if(decrease)BigDecimal.ONE-p.movePointLeft(2)else BigDecimal.ONE+p.movePointLeft(2)
  val changes=mutableListOf<Change>()
  val lines=base.lines.map{row->if(row.id !in selected)row else{
   val rate=try{(BigDecimal(row.rate)*factor).setScale(2,RoundingMode.UNNECESSARY).toPlainString()}catch(_:ArithmeticException){throw IllegalArgumentException("${row.label}: new rate needs more than 2 decimals. No rate was rounded or applied.")}
   val next=CallEstimate.line(row.label,row.quantity,rate,row.id);changes+=Change(row.id,row.label,row.rate,next.rate,row.quantity);next
  }}
  require(lines.zip(base.lines).any{(a,b)->BigDecimal(a.rate).compareTo(BigDecimal(b.rate))!=0}){"All selected rates stay unchanged"}
  val result=base.copy(lines=lines);CallEstimate.totals(result)
  return Preview(base.copy(lines=base.lines.toList()),selected.toSet(),percent,decrease,changes.toList(),result)
 }
 fun apply(current:CallEstimate.Draft,view:Preview):CallEstimate.Draft {
  require(current==view.source){"Calculator changed. Preview again."}
  val verified=preview(current,view.selected,view.percent,view.decrease)
  require(verified==view){"Preview changed. Preview again."}
  return verified.result
 }
}
