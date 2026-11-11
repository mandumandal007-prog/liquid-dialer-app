package com.mandu.liquiddialer.data
import androidx.datastore.preferences.core.MutablePreferences

/** Button arrangement only. Never changes call actions, permissions, icons or theme. */
data class CallLayoutDraft(val grid:String,val bottom:String,val gridSize:Float,val bottomSize:Float){
 fun validate(){require(gridSize.isFinite()&&gridSize in .8f..1.3f&&bottomSize.isFinite()&&bottomSize in .8f..1.3f);require(valid(grid,GRID)&&valid(bottom,BOTTOM));require("end" in bottom.split(','))}
 fun move(isBottom:Boolean,id:String,delta:Int):CallLayoutDraft{require(delta==1||delta== -1);val raw=if(isBottom)bottom else grid;val list=raw.split(',').toMutableList();val i=list.indexOfFirst{it.removePrefix("!")==id};require(i>=0);val j=i+delta;if(j !in list.indices)return this;val t=list[i];list[i]=list[j];list[j]=t;return if(isBottom)copy(bottom=list.joinToString(","))else copy(grid=list.joinToString(","))}
 fun toggle(isBottom:Boolean,id:String):CallLayoutDraft{require(id!="end");val raw=if(isBottom)bottom else grid;require(raw.split(',').any{it.removePrefix("!")==id});val changed=raw.split(',').joinToString(","){if(it.removePrefix("!")==id){if(it.startsWith('!'))id else "!$id"}else it};return if(isBottom)copy(bottom=changed)else copy(grid=changed)}
 fun write(p:MutablePreferences){validate();p[K.layoutGrid]=grid;p[K.layoutBottom]=bottom;p[K.gridScale]=gridSize;p[K.bottomScale]=bottomSize}
 companion object{
  val GRID=listOf("mute","keypad","record","hold","add","merge","audio");val BOTTOM=listOf("speaker","end","notes")
  private fun valid(raw:String,ids:List<String>):Boolean{val tokens=raw.split(',');return tokens.size==ids.size&&tokens.map{it.removePrefix("!")}.toSet()==ids.toSet()&&tokens.all{it in ids||it.startsWith('!')&&it.drop(1)in ids}}
  private fun canonical(raw:String,ids:List<String>):String{val out=mutableListOf<String>();raw.split(',').map{it.trim()}.forEach{t->val id=t.removePrefix("!");if(id in ids&&out.none{it.removePrefix("!")==id})out+=if(id=="end")"end"else t};ids.filter{id->out.none{it.removePrefix("!")==id}}.forEach{out+=it};return out.joinToString(",")}
  fun capture(s:LdSettings)=CallLayoutDraft(canonical(s.layoutGrid,GRID),canonical(s.layoutBottom,BOTTOM),s.gridScale.takeIf{it.isFinite()&&it in .8f..1.3f}?:1f,s.bottomScale.takeIf{it.isFinite()&&it in .8f..1.3f}?:1f)
  fun presets()=listOf("Classic" to CallLayoutDraft(DEFAULT_GRID,DEFAULT_BOTTOM,1f,1f),"Essentials" to CallLayoutDraft("mute,keypad,audio,!record,!hold,!add,!merge","speaker,end,!notes",1.15f,1.15f),"Conference" to CallLayoutDraft("mute,keypad,audio,hold,add,merge,!record","speaker,end,notes",1f,1f))
  fun applyIfCurrent(p:MutablePreferences,expected:CallLayoutDraft,next:CallLayoutDraft){next.validate();require(capture(p.toSettings())==expected){"Layout changed elsewhere. Reload before applying."};next.write(p)}
 }
}
