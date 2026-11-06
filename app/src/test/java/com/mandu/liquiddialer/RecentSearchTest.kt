package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
import kotlin.random.Random

class RecentSearchTest {
 private fun rows(count:Int):List<Recent> {
  val rng=Random(72)
  return (0 until count).map{i->Recent(i.toLong(),listOf("+91 ","0","+1 ","", "Private ")[i%5]+(0..9).map{rng.nextInt(10)}.joinToString(" "),listOf(null,"Rahul","देव","Alice Smith","")[i%5],i%9,1700000000000L-i,30,null,null)}
 }
 private fun old(rows:List<Recent>,q:String,f:Int,hide:Boolean)=rows.filter{r->
  val qok=q.isBlank() || (r.name?:"").contains(q,true) || r.number.filter{it.isDigit()}.contains(q.filter{it.isDigit()}.ifEmpty{"\u0000"})
  val fok=when(f){1->r.type==3||r.type==5||r.type==6;2->r.type==2;3->r.type==1||r.type==4;else->true}
  qok&&fok&&(!hide||r.type!=6)
 }
 @Test fun matchesExistingSearchForEveryFilterAndOrder(){
  val rows=rows(500);val index=RecentSearch(rows)
  for(q in listOf(""," ","Rah","rah","देव","Alice Smith","+91","91 12","123","Private","*#","٠١"))
   for(f in -1..4)for(h in listOf(false,true))assertEquals("query=$q filter=$f hidden=$h",old(rows,q,f,h),index.find(q,f,h))
 }
 @Test fun newSourceRebuildDoesNotKeepRemovedOrStaleNames(){
  val first=rows(20);val next=first.drop(10).map{it.copy(name="Changed")}
  assertEquals(next,RecentSearch(next).find("Changed"));assertTrue(RecentSearch(next).find("Rahul").isEmpty())
 }
 @Test fun refreshAndBlockedSettingsInvalidateResults(){
  val base=rows(40);val updated=base.map{it.copy(number="+91 77777 88888",name="Updated",type=6)}
  val index=RecentSearch(updated)
  assertEquals(updated,index.find("77777"));assertEquals(updated,index.find("Updated"))
  assertTrue(index.find("77777",hideBlocked=true).isEmpty())
  assertEquals(updated,index.find("",filter=1,hideBlocked=false))
 }
 @Test fun localBenchmarkPreservesResults(){
  for(size in listOf(500,5000,10000)) {
   val rows=rows(size);val queries=listOf("9","91","911","9112","Rah","rahul"," ")
   val buildStart=System.nanoTime();val index=RecentSearch(rows);val buildNs=System.nanoTime()-buildStart
   repeat(10){for(q in queries){old(rows,q,0,false);index.find(q)}}
   fun measure(block:()->Unit):Long{val start=System.nanoTime();repeat(50){block()};return System.nanoTime()-start}
   val oldNs=measure{for(q in queries)old(rows,q,0,false)}
   val newNs=measure{for(q in queries)index.find(q)}
   val refreshStart=System.nanoTime();val refreshed=RecentSearch(rows.map{it.copy(name="Refreshed",number="+91 77777 88888")});val refreshNs=System.nanoTime()-refreshStart
   println("RECENT_SEARCH_BENCH rows=$size queryBatches=50 legacyMs=${oldNs/1_000_000.0} indexedMs=${newNs/1_000_000.0} buildMs=${buildNs/1_000_000.0} refreshIncludingCopyMs=${refreshNs/1_000_000.0} retainedNormalizedChars=${index.normalizedCharacterCount}; local JVM, no heap-size/device claim")
   for(q in queries)for(f in 0..3)for(h in listOf(false,true))assertEquals(old(rows,q,f,h),index.find(q,f,h))
   assertEquals(size,refreshed.find("Refreshed").size);assertEquals(size,refreshed.find("77777").size)
  }
 }
}
