package com.mandu.liquiddialer.data
/** Session tool IDs only. The caller owns lifetime, never saved or shared. */
object DeskShelf {
 const val MAX=4
 fun visible(ids:List<String>,available:List<DeskFinder.Tool>):List<String> = ids.distinct().filter{id->available.any{it.id==id}}.take(MAX)
 fun toggle(ids:List<String>,id:String,available:List<DeskFinder.Tool>):List<String>{
  val current=visible(ids,available)
  if(id in current)return current-id
  if(current.size>=MAX||available.none{it.id==id})return current
  return current+id
 }
}
