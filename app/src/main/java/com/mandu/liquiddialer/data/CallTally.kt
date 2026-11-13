package com.mandu.liquiddialer.data
import java.util.Locale
import java.util.UUID

/** Manual session counters only, never stock, orders, pricing or call audio. */
object CallTally {
 const val MAX_ROWS=12
 const val MAX_VALUE=1_000_000_000L
 data class Item(val id:String,val label:String,val value:Long)
 fun label(raw:String)=raw.trim().replace(Regex("\\s+")," ").also{require(it.length in 1..60&&it.none{c->c.isISOControl()}){"Naam 1-60 letters ka rakho; control characters nahi"}}
 fun whole(raw:String,max:Long=MAX_VALUE):Long {require(Regex("[0-9]{1,10}").matches(raw)){"Whole digits only, no sign/comma/decimal"};return raw.toLong().also{require(it<=max){"Value 0 se $max tak"}}}
 fun step(raw:String)=whole(raw,1_000_000).also{require(it>0){"Step 1 se 1000000 tak"}}
 fun validate(rows:List<Item>){require(rows.size<=MAX_ROWS){"12 counters ki limit"};require(rows.map{it.id}.distinct().size==rows.size);val names=hashSetOf<String>();rows.forEach{require(it.id.isNotBlank()&&it.id.length<=80);require(label(it.label)==it.label);require(names.add(it.label.lowercase(Locale.ROOT))){"Har counter ka alag naam rakho"};require(it.value in 0..MAX_VALUE)}}
 fun add(rows:List<Item>,name:String,value:String,id:String=UUID.randomUUID().toString()):List<Item> = (rows+Item(id,label(name),whole(value))).also{validate(it)}
 fun edit(rows:List<Item>,id:String,name:String,value:String):List<Item>{require(rows.any{it.id==id}){"Counter nahi mila"};return rows.map{if(it.id==id)it.copy(label=label(name),value=whole(value))else it}.also{validate(it)}}
 fun adjust(rows:List<Item>,id:String,delta:Long):List<Item>{require(delta in -1_000_000..1_000_000&&delta!=0L);require(rows.any{it.id==id}){"Counter nahi mila"};return rows.map{if(it.id==id)it.copy(value=(it.value+delta).also{v->require(v in 0..MAX_VALUE){"0 se $MAX_VALUE ke bahar nahi ja sakte"}})else it}.also{validate(it)}}
 fun remove(rows:List<Item>,id:String):List<Item>{require(rows.any{it.id==id});return rows.filterNot{it.id==id}.also{validate(it)}}
 fun zero(rows:List<Item>)=rows.map{it.copy(value=0)}.also{validate(it)}
 fun total(rows:List<Item>):Long {validate(rows);return rows.sumOf{it.value}}
 class Board(initial:List<Item> = emptyList()){
  var rows=initial.toList();private set
  private val past=mutableListOf<List<Item>>();private val future=mutableListOf<List<Item>>()
  init{validate(rows)}
  val canUndo get()=past.isNotEmpty();val canRedo get()=future.isNotEmpty()
  val undoCount get()=past.size;val redoCount get()=future.size
  fun change(next:List<Item>):Boolean {validate(next);if(next==rows)return false;past.add(rows);if(past.size>40)past.removeAt(0);rows=next.toList();future.clear();return true}
  fun undo(){if(canUndo){future.add(rows);rows=past.removeAt(past.lastIndex)}}
  fun redo(){if(canRedo){past.add(rows);rows=future.removeAt(future.lastIndex)}}
 }
}
