package com.mandu.liquiddialer.data

/** Exact session-only changes. No units are inferred and no partial batch is returned. */
object TallyBatch {
 enum class Operation(val title:String){ADD("Add"),SUBTRACT("Subtract"),SET("Set"),MULTIPLY("Multiply")}
 data class Preview(val before:List<CallTally.Item>,val after:List<CallTally.Item>,val selected:Set<String>,val operation:Operation,val operand:Long){val changed get()=before.zip(after).count{it.first!=it.second}}
 fun preview(rows:List<CallTally.Item>,selected:Set<String>,operation:Operation,raw:String):Preview {
  CallTally.validate(rows);require(selected.isNotEmpty()&&selected.all{id->rows.any{it.id==id}}){"Select existing counters first"}
  val n=CallTally.whole(raw,if(operation==Operation.MULTIPLY)1_000_000 else CallTally.MAX_VALUE)
  val next=rows.map{item->if(item.id !in selected)item else{
   val value=when(operation){Operation.ADD->item.value+n;Operation.SUBTRACT->item.value-n;Operation.SET->n;Operation.MULTIPLY->item.value*n}
   require(value in 0..CallTally.MAX_VALUE){"${item.label}: result outside 0-1000000000. Whole batch refused; no clamping."};item.copy(value=value)
  }}
  CallTally.validate(next);return Preview(rows.toList(),next,selected.toSet(),operation,n)
 }
 fun apply(current:List<CallTally.Item>,p:Preview):List<CallTally.Item>{
  require(current==p.before){"Board changed. Preview again before applying."}
  val checked=preview(current,p.selected,p.operation,p.operand.toString());require(checked==p){"Preview changed. Preview again."};return checked.after
 }
}
