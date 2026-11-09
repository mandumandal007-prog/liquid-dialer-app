package com.mandu.liquiddialer.data

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Bounded UTF-8 CSV, never evaluates cells or changes contacts. Records use 1-based CSV row numbers. */
object CsvCallImport {
 const val MAX_BYTES=128*1024
 const val MAX_ROWS=501 // header + 500 data rows
 const val MAX_COLUMNS=20
 data class Table(val rows:List<List<String>>,val columns:Int)
 enum class Status { READY, INVALID, DUPLICATE }
 data class Row(val record:Int,val number:String,val name:String,val status:Status,val reason:String="")
 fun read(input:InputStream):String {
  val out=java.io.ByteArrayOutputStream();val buf=ByteArray(8192)
  while(true){val n=input.read(buf);if(n<0)break;require(out.size()+n<=MAX_BYTES){"CSV 128 KB se badi hai"};out.write(buf,0,n)}
  return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(out.toByteArray())).toString()
 }
 fun parse(raw:String,separator:Char=','):Table {
  require(separator in listOf(',',';','\t'))
  require(raw.toByteArray(Charsets.UTF_8).size<=MAX_BYTES){"CSV 128 KB se badi hai"}
  val text=raw.removePrefix("\uFEFF");require('\u0000' !in text){"CSV mein NUL nahi chalega"}
  val rows=mutableListOf<List<String>>();val row=mutableListOf<String>();val cell=StringBuilder()
  var quoted=false;var closed=false;var i=0
  fun field(){require(row.size<MAX_COLUMNS){"20 columns tak CSV kholo"};row+=cell.toString();cell.clear();closed=false}
  fun record(){field();require(rows.size<MAX_ROWS){"Header ke saath 501 CSV rows tak kholo"};rows+=row.toList();row.clear()}
  while(i<text.length){val c=text[i]
   if(quoted){if(c=='"'){if(i+1<text.length&&text[i+1]=='"'){cell.append('"');i++}else{quoted=false;closed=true}}else cell.append(c)}
   else when {
    c==separator->field()
    c=='\n'||c=='\r'->{record();if(c=='\r'&&i+1<text.length&&text[i+1]=='\n')i++}
    closed->require(c==' '){"Quote band hone ke baad separator chahiye"}
    c=='"'->{require(cell.isEmpty()){"Quote sirf cell ke shuru mein ho"};quoted=true}
    else->cell.append(c)
   }
   require(cell.length<=4096){"Ek CSV cell 4096 letters tak rakho"};i++
  }
  require(!quoted){"CSV mein quote band nahi hai"}
  if(cell.isNotEmpty()||row.isNotEmpty()||closed)record()
  require(rows.isNotEmpty()&&rows.any{r->r.any{it.isNotBlank()}}){"CSV khaali hai"}
  return Table(rows,rows.maxOf{it.size})
 }
 fun preview(table:Table,phoneColumn:Int,nameColumn:Int?=null,header:Boolean=true):List<Row>{
  require(phoneColumn in 0 until table.columns && (nameColumn==null || nameColumn in 0 until table.columns))
  require(phoneColumn!=nameColumn){"Naam aur number ke liye alag columns chuno"}
  val seen=hashSetOf<String>()
  return table.rows.mapIndexedNotNull{index,cols->
   if((header&&index==0)||cols.all{it.isBlank()})return@mapIndexedNotNull null
   val n=cols.getOrNull(phoneColumn).orEmpty().trim();val name=nameColumn?.let{cols.getOrNull(it).orEmpty().trim()}.orEmpty()
   val reason=when{
    name.length>100->"Naam 100 letters se bada"
    n.startsWith("=") || n.startsWith("@") || n.startsWith("'")->"Formula/text marker: number ko plain text mein sahi karo"
    else->try{CallLists.number(n);""}catch(_:IllegalArgumentException){"Plain phone number chahiye; codes/formula/scientific notation nahi"}
   }
   if(reason.isNotEmpty())Row(index+1,n,name,Status.INVALID,reason)
   else if(!seen.add(ContactControls.k(n)))Row(index+1,n,name,Status.DUPLICATE,"Is file mein pehle aa chuka hai")
   else Row(index+1,n,name,Status.READY)
  }
 }
}
