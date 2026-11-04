package com.mandu.liquiddialer.data

import java.io.InputStream

/** Portable private backup and a bounded, plain-text batch preview. No contact or call effects. */
object CallListTransfer {
    const val MAX_BYTES=1024*1024
    data class Row(val number:String,val name:String)
    data class Batch(val entries:List<Row>,val duplicateCount:Int,val invalidLines:List<Int>)
    fun batch(text:String,existing:List<String> = emptyList()):Batch {
        require(text.toByteArray(Charsets.UTF_8).size<=64*1024) {"Paste bahut bada hai (64 KB limit)"}
        val lines=text.lineSequence().toList();require(lines.size<=500) {"Ek baar mein 500 lines tak paste karo"}
        val seen=existing.map {ContactControls.k(it)}.toMutableSet()
        val entries=mutableListOf<Row>();val invalid=mutableListOf<Int>();var duplicates=0
        lines.forEachIndexed {i,line->
            if(line.isBlank())return@forEachIndexed
            val split=line.indexOfAny(charArrayOf('\t',','));val n=(if(split<0)line else line.substring(0,split)).trim()
            val name=(if(split<0)"" else line.substring(split+1)).trim()
            val valid=try{CallLists.number(n);name.length<=100}catch(_:IllegalArgumentException){false}
            if(!valid)invalid+=i+1
            else if(!seen.add(ContactControls.k(n)))duplicates++
            else entries+=Row(n,name)
        }
        return Batch(entries,duplicates,invalid)
    }
    fun backup(lists:List<CallLists.ListItem>):String=CallLists.encode(lists).also {CallLists.decode(it)}
    fun preview(text:String):List<CallLists.ListItem> = CallLists.decode(text).also {require(it.isNotEmpty()) {"Backup mein koi list nahi hai"}}
    fun read(input:InputStream):String {
        val out=java.io.ByteArrayOutputStream();val bytes=ByteArray(8192)
        while(true){val n=input.read(bytes);if(n<0)break;require(out.size()+n<=MAX_BYTES) {"Backup 1 MB se bada hai"};out.write(bytes,0,n)}
        return Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(out.toByteArray())).toString()
    }
}
