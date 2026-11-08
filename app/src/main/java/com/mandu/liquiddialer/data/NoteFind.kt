package com.mandu.liquiddialer.data

/** Literal, non-overlapping matches in the original text. No normalization or stored edits. */
object NoteFind {
 data class Match(val start:Int,val end:Int)
 fun matches(text:String,query:String):List<Match> {
  if(query.isBlank())return emptyList()
  val out=ArrayList<Match>();var from=0
  while(from<=text.length-query.length){
   val at=text.indexOf(query,from,ignoreCase=true);if(at<0)break
   out+=Match(at,at+query.length);from=at+query.length
  }
  return out
 }
 fun next(current:Int,count:Int,step:Int):Int = if(count<=0)0 else Math.floorMod(current.toLong()+step,count.toLong()).toInt()
}
