package com.mandu.liquiddialer.data

/** One actual line is one topic. Never strips bullets/numbers or reads a clipboard. */
object ChecklistLines {
 const val MAX_INPUT=16000
 const val MAX_LINES=60
 enum class Kind { NEW, EXISTING, REPEATED }
 data class Line(val line:Int,val text:String,val kind:Kind)
 data class Preview(val source:List<CallChecklist.Item>,val lines:List<Line>){val added get()=lines.filter{it.kind==Kind.NEW};val skipped get()=lines.size-added.size}
 fun preview(source:List<CallChecklist.Item>,raw:String):Preview{
  require(raw.length<=MAX_INPUT){"Input limit: 16000 characters. Nothing was added."}
  require(raw.none{it.isISOControl()&&it!='\n'&&it!='\r'&&it!='\t'}){"Control characters refused. Nothing was added."}
  val parts=raw.replace("\r\n","\n").replace('\r','\n').split('\n')
  require(parts.size<=MAX_LINES){"Use at most 60 lines including blank lines."}
  val old=source.map{ChecklistTemplates.normalized(it.text)}.toSet();val seen=hashSetOf<String>()
  val lines=parts.mapIndexedNotNull{i,s->val t=s.trim();if(t.isEmpty())null else{
   require(t.length<=CallChecklist.MAX_TEXT){"Line ${i+1}: topic exceeds 250 characters. Nothing was added."}
   val k=ChecklistTemplates.normalized(t);val kind=when{ k in old->Kind.EXISTING;!seen.add(k)->Kind.REPEATED;else->Kind.NEW};Line(i+1,t,kind)
  }}
  require(lines.isNotEmpty()){"Write at least one nonblank topic line."}
  return Preview(source.toList(),lines)
 }
 fun selected(p:Preview,lines:Set<Int>):List<String>{
  require(lines.isNotEmpty()&&lines.all{n->p.added.any{it.line==n}}){"Select new topic lines first."}
  val chosen=p.added.filter{it.line in lines}.map{it.text}
  require(p.source.size+chosen.size<=CallChecklist.MAX_ITEMS){"Not enough room: ${chosen.size} selected, ${CallChecklist.MAX_ITEMS-p.source.size} spaces. Whole batch refused."}
  return chosen
 }
}
