package com.mandu.liquiddialer.data

import android.content.Context
import com.mandu.liquiddialer.telecom.CallManager
import java.io.File
import java.io.OutputStream
import java.nio.file.Files

/** Only completed, direct .m4a children of this app's recording folder. No capture changes. */
object RecordingLibrary {
    data class Item(val fileName:String,val bytes:Long,val modified:Long) {
        val title get()=fileName.removeSuffix(".m4a").removePrefix("Call_").replace('_',' ')
    }
    fun active(name:String):Boolean=CallManager.recording.value?.file?.name==name
    private fun root(ctx:Context)=Repo.recordingsDir(ctx).canonicalFile
    private fun file(ctx:Context,name:String):File {
        require(name.isNotBlank() && name==File(name).name && name.endsWith(".m4a",true)){"Recording ka naam sahi nahi hai"}
        val root=root(ctx);val f=File(root,name)
        require(f.canonicalFile.parentFile==root && !Files.isSymbolicLink(f.toPath())){"Recording path sahi nahi hai"}
        require(f.isFile && f.length()>0){"Recording ab maujood nahi hai"}
        return f
    }
    @Synchronized fun list(ctx:Context):List<Item> {
        val root=root(ctx);val all=root.listFiles() ?: error("Recordings folder nahi khula")
        require(all.size<=5000){"Folder mein bahut files hain; system Files app se manage karo"}
        return all.filter {it.isFile && it.name.endsWith(".m4a",true) && !Files.isSymbolicLink(it.toPath()) && it.canonicalFile.parentFile==root && it.length()>0}
            .map{Item(it.name,it.length(),it.lastModified())}.sortedWith(compareByDescending<Item>{it.modified}.thenBy{it.fileName})
    }
    fun search(items:List<Item>,query:String):List<Item> = items.filter{query.trim().isEmpty() || it.title.contains(query.trim(),true) || it.fileName.contains(query.trim(),true)}
    @Synchronized fun resolve(ctx:Context,item:Item):File {
        require(!active(item.fileName)){"Recording abhi chal rahi hai; pehle band karo"}
        val f=file(ctx,item.fileName);require(f.length()==item.bytes && f.lastModified()==item.modified){"Recording badal gayi; dobara load karo"}
        return f
    }
    @Synchronized fun rename(ctx:Context,item:Item,title:String) {
        val f=resolve(ctx,item);val clean=title.trim()
        require(clean.length in 1..100 && clean!="." && clean!=".." && clean.none{it=='/' || it=='\\' || it.isISOControl()}){"Naam 1-100 letters ka rakho; slash mat lagao"}
        val target=File(root(ctx),"$clean.m4a")
        if(target.name==f.name)return
        require(!target.exists() && !active(target.name)){"Is naam ki recording pehle se hai"}
        Files.move(f.toPath(),target.toPath()) // no REPLACE_EXISTING: never overwrite a different recording
    }
    @Synchronized fun delete(ctx:Context,item:Item) {val f=resolve(ctx,item);check(f.delete()){"Recording delete nahi hui"}}
    @Synchronized fun copy(ctx:Context,item:Item,out:OutputStream) {
        val f=resolve(ctx,item);f.inputStream().use{it.copyTo(out,64*1024)}
    }
}
