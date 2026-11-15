package com.mandu.liquiddialer.data

/** Placement preferences only. Notes and fixed call controls cannot be hidden here. */
object DeskSections {
 const val DEFAULT="watch:1,marks:1,notes:1"
 data class Section(val id:String,val visible:Boolean=true)
 val ids=listOf("watch","marks","notes")
 fun title(id:String):String=when(id){"watch"->"Interval stopwatch";"marks"->"Call checkpoints";"notes"->"Notes and call tools";else->error("Unknown section")}
 fun parse(raw:String):List<Section> = runCatching{
  require(raw.length<=100)
  val rows=raw.split(',').map{token->val parts=token.split(':');require(parts.size==2&&parts[0] in ids&&parts[1] in listOf("0","1"));Section(parts[0],parts[1]=="1")}
  validate(rows);rows
 }.getOrElse{ids.map{Section(it)}}
 fun validate(rows:List<Section>){require(rows.size==3&&rows.map{it.id}.toSet()==ids.toSet()&&rows.single{it.id=="notes"}.visible)}
 fun encode(rows:List<Section>):String {validate(rows);return rows.joinToString(","){"${it.id}:${if(it.visible)1 else 0}"}}
 fun move(rows:List<Section>,id:String,delta:Int):List<Section>{
  validate(rows);require(delta==-1||delta==1);val from=rows.indexOfFirst{it.id==id};require(from>=0);val to=from+delta;require(to in rows.indices)
  return rows.toMutableList().also{val item=it.removeAt(from);it.add(to,item)}
 }
 fun show(rows:List<Section>,id:String,visible:Boolean):List<Section>{
  validate(rows);require(id in ids&&(id!="notes"||visible));return rows.map{if(it.id==id)it.copy(visible=visible)else it}
 }
 fun snapshot(s:LdSettings):List<Section> = if(s.deskSections)parse(s.deskSectionLayout)else parse(DEFAULT)
}
