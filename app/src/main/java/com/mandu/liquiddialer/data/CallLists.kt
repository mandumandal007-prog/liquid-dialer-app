package com.mandu.liquiddialer.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.UUID

/** Private manual checklist, never a call queue. A call attempt does not change progress. */
object CallLists {
    data class Entry(val id:String, val number:String, val name:String, val done:Boolean=false,
        val outcome:String=if(done) "done" else "pending", val note:String="", val updatedAt:Long=0)
    val outcomes = linkedMapOf("pending" to "Pending", "done" to "Done", "no_answer" to "No answer", "busy" to "Busy", "later" to "Call later")
    data class ListItem(val id:String, val title:String, val entries:List<Entry> = emptyList())
    private const val MAX_LISTS=20
    private const val MAX_ENTRIES=200
    private fun file(ctx:Context)=AtomicFile(File(ctx.filesDir,"manual_call_lists.json"))
    private fun title(s:String)=s.trim().also { require(it.isNotEmpty() && it.length<=60) {"Naam 1 se 60 letters ka rakho"} }
    internal fun number(s:String)=s.trim().also {
        require(it.length in 1..40 && it.count { c -> c.isDigit() } in 3..20 &&
            it.all { c -> c.isDigit() || c in "+-(). " } &&
            it.count { c -> c=='+' }<=1 && ('+' !in it || it.startsWith('+')) &&
            ContactControls.k(it).isNotEmpty()) {"Sahi phone number daalo (3-20 digits); codes yahan nahi"}
    }
    @Synchronized fun read(ctx:Context):List<ListItem> {
        val f=file(ctx)
        if(!f.baseFile.exists() && !File(f.baseFile.path+".bak").exists()) return emptyList()
        val text=f.openRead().use { stream ->
            val bytes=stream.takeBounded();require(bytes.size<=1024*1024) {"Saved lists bahut badi hain"};bytes.toString(Charsets.UTF_8)
        }
        return decode(text)
    }
    internal fun decode(text:String):List<ListItem> {
        require(text.toByteArray().size<=1024*1024) {"Saved lists bahut badi hain"}
        // Do not silently import the first object of a damaged/concatenated backup.
        // JSONTokener uses NUL as its end marker; raw NUL is never valid JSON text.
        require('\u0000' !in text) {"Backup format sahi nahi hai"}
        val tokener=JSONTokener(text)
        val root=tokener.nextValue() as? JSONObject ?: error("Backup format sahi nahi hai")
        require(tokener.nextClean()=='\u0000') {"Backup format sahi nahi hai"}
        require(root.get("version") is Int && root.getInt("version") in 1..2)
        val a=root.getJSONArray("lists");require(a.length()<=MAX_LISTS)
        val ids=HashSet<String>()
        return (0 until a.length()).map { i ->
            val o=a.getJSONObject(i);require(o.get("id") is String && o.get("title") is String);val id=o.getString("id");require(id.isNotBlank() && ids.add(id))
            val es=o.getJSONArray("entries");require(es.length()<=MAX_ENTRIES)
            val eids=HashSet<String>();val nums=HashSet<String>()
            ListItem(id,title(o.getString("title")),(0 until es.length()).map { j ->
                val e=es.getJSONObject(j);require(e.get("id") is String && e.get("number") is String && e.get("name") is String);val eid=e.getString("id");val n=number(e.getString("number"));val name=e.getString("name")
                require(eid.isNotBlank() && eids.add(eid) && nums.add(ContactControls.k(n)) && name.length<=100)
                require(e.get("done") is Boolean)
                val done=e.getBoolean("done")
                val v=root.getInt("version")
                val outcome=if(v==1) {if(done) "done" else "pending"} else {
                    require(e.get("outcome") is String);e.getString("outcome")
                }
                val note=if(v==1) "" else {require(e.get("note") is String);e.getString("note")}
                val at=if(v==1) 0L else {require(e.get("updatedAt") is Long || e.get("updatedAt") is Int);e.getLong("updatedAt")}
                require(outcome in outcomes && done==(outcome=="done") && note.length<=2000 && at>=0)
                Entry(eid,n,name,done,outcome,note,at)
            })
        }
    }
    private fun java.io.InputStream.takeBounded():ByteArray {
        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) {val n=read(buffer);if(n<0)break;require(out.size()+n<=1024*1024) {"Saved lists bahut badi hain"};out.write(buffer,0,n)}
        return out.toByteArray()
    }
    private fun write(ctx:Context, lists:List<ListItem>):List<ListItem> {
        val bytes=encode(lists).toByteArray()
        require(bytes.size<=1024*1024) {"Lists storage ki limit aa gayi; kuch entries hatao"}
        val f=file(ctx);val stream=f.startWrite()
        try {stream.write(bytes);f.finishWrite(stream)}
        catch(t:Throwable){f.failWrite(stream);throw t}
        return lists
    }
    internal fun encode(lists:List<ListItem>):String {
        val a=JSONArray()
        lists.forEach { l ->
            val es=JSONArray();l.entries.forEach {e -> es.put(JSONObject().put("id",e.id).put("number",e.number).put("name",e.name).put("done",e.done).put("outcome",e.outcome).put("note",e.note).put("updatedAt",e.updatedAt)) }
            a.put(JSONObject().put("id",l.id).put("title",l.title).put("entries",es))
        }
        return JSONObject().put("version",2).put("lists",a).toString()
    }
    @Synchronized fun create(ctx:Context,name:String):List<ListItem> {
        val xs=read(ctx);require(xs.size<MAX_LISTS) {"Abhi 20 lists ki limit hai"}
        return write(ctx,xs+ListItem(UUID.randomUUID().toString(),title(name)))
    }
    private fun change(ctx:Context,id:String,f:(ListItem)->ListItem):List<ListItem> {
        val xs=read(ctx);require(xs.any {it.id==id}) {"List ab nahi mili; dobara kholo"}
        return write(ctx,xs.map {if(it.id==id) f(it) else it})
    }
    @Synchronized fun rename(ctx:Context,id:String,name:String)=change(ctx,id){it.copy(title=title(name))}
    @Synchronized fun add(ctx:Context,id:String,n:String,name:String)=change(ctx,id){l ->
        val num=number(n);require(name.trim().length<=100) {"Contact naam bahut lamba hai"}
        require(l.entries.size<MAX_ENTRIES) {"Ek list mein 200 numbers ki limit hai"}
        require(l.entries.none {ContactControls.k(it.number)==ContactControls.k(num)}) {"Ye number list mein pehle se hai"}
        l.copy(entries=l.entries+Entry(UUID.randomUUID().toString(),num,name.trim()))
    }
    @Synchronized fun done(ctx:Context,id:String,eid:String,done:Boolean)=change(ctx,id){l ->
        require(l.entries.any {it.id==eid});l.copy(entries=l.entries.map {if(it.id==eid) it.copy(done=done,outcome=if(done) "done" else "pending",updatedAt=System.currentTimeMillis()) else it})
    }
    /** Explicit user-entered outcome only. Calling, opening, or moving through a session never writes. */
    @Synchronized fun followUp(ctx:Context,id:String,eid:String,outcome:String,note:String,updatedAt:Long=System.currentTimeMillis())=change(ctx,id){l ->
        require(outcome in outcomes) {"Sahi outcome chuno"}
        require(updatedAt>=0)
        require(note.length<=2000) {"Note 2,000 letters tak rakho"}
        require(l.entries.any {it.id==eid}) {"Entry ab nahi mili; list dobara kholo"}
        l.copy(entries=l.entries.map {if(it.id==eid) it.copy(done=outcome=="done",outcome=outcome,note=note,updatedAt=updatedAt) else it})
    }
    @Synchronized fun remove(ctx:Context,id:String,eid:String)=change(ctx,id){l ->
        require(l.entries.any {it.id==eid});l.copy(entries=l.entries.filterNot {it.id==eid})
    }
    @Synchronized fun delete(ctx:Context,id:String):List<ListItem> {
        val xs=read(ctx);require(xs.any {it.id==id});return write(ctx,xs.filterNot {it.id==id})
    }
    /** Import only fresh copies. Existing IDs, entries and Done flags never change. */
    @Synchronized fun importCopies(ctx:Context,text:String):List<ListItem> {
        val source=decode(text);require(source.isNotEmpty()) {"Backup mein koi list nahi hai"}
        val current=read(ctx);require(current.size+source.size<=MAX_LISTS) {"Import ke baad 20 lists se zyada hongi; pehle jagah banao"}
        val copies=source.map {l->l.copy(id=UUID.randomUUID().toString(),entries=l.entries.map {it.copy(id=UUID.randomUUID().toString())})}
        return write(ctx,current+copies)
    }
    @Synchronized fun addBatch(ctx:Context,id:String,text:String):List<ListItem> = change(ctx,id){l->
        val preview=CallListTransfer.batch(text,l.entries.map {it.number})
        require(preview.invalidLines.isEmpty()) {"Galat lines sahi karo: "+preview.invalidLines.joinToString(", ")}
        require(preview.entries.isNotEmpty()) {"Jodne ke liye koi naya number nahi"}
        require(l.entries.size+preview.entries.size<=MAX_ENTRIES) {"Ek list mein 200 numbers ki limit hai"}
        l.copy(entries=l.entries+preview.entries.map {Entry(UUID.randomUUID().toString(),it.number,it.name)})
    }

}
