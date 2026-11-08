package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.*
import org.junit.Assert.*
import org.junit.Test

class SearchInputTest {
 private val c=Contact(1,"lookup","Rahul Sharma","Sharma, Rahul","+91 (98765) 43210","Mobile",false,null)
 private val r=Recent(1,c.number,c.name,1,1700000000000L,60,null,null)
 @Test fun pastedFormattingWhitespaceAndIndianDigitsFindTheSameStoredNumber(){
  for(q in listOf("+91 (98765) 43210","98765-43210"," 98765\t43210 ","९८७६५४३२१०","٩٨٧٦٥٤٣٢١٠")){
   assertEquals(q,listOf(c),ContactSearch(listOf(c)).find(q));assertTrue(q,Repo.matches(c,q))
   assertEquals(q,listOf(r),RecentSearch(listOf(r)).find(q))
  }
  assertEquals("stored dialing number unchanged", "+91 (98765) 43210",c.number)
 }
 @Test fun alternateSavedNameAndTrimmedRecentNameAreSearchable(){
  assertEquals(listOf(c),ContactSearch(listOf(c)).find("Sharma, Rahul"));assertTrue(Repo.matches(c,"Sharma, Rahul"))
  assertEquals(listOf(r),RecentSearch(listOf(r)).find("  RAHUL  "))
 }
 @Test fun textWithDigitsServiceCodesAndExtensionsCannotMatchOnlyTheirDigits(){
  for(q in listOf("office98765","call 98765","98765 ext 3","*98765#","+91+98765","98+765")){
   assertTrue(q,ContactSearch(listOf(c)).find(q).isEmpty());assertFalse(q,Repo.matches(c,q))
   assertTrue(q,RecentSearch(listOf(r)).find(q).isEmpty())
  }
  val named=r.copy(name="Office98765")
  assertEquals(listOf(named),RecentSearch(listOf(named)).find("office98765"))
 }
 @Test fun t9OrderBoundsAndRebuiltSourcesRemainCorrect(){
  val contacts=listOf(c,c.copy(id=2,name="Rahul Second",altName="Second, Rahul"))
  val ix=ContactSearch(contacts)
  assertEquals(contacts,ix.find("72485"));assertEquals(listOf(c),ix.find("98765-43210",1));assertEquals(1,ix.lastExamined)
  assertTrue(ContactSearch(emptyList()).find("98765-43210").isEmpty())
  val changed=c.copy(name="Changed",altName="",number="111")
  assertTrue(ContactSearch(listOf(changed)).find("Sharma, Rahul").isEmpty())
  assertTrue(RecentSearch(emptyList()).find("98765-43210").isEmpty())
 }
 @Test fun recentFiltersStillApplyToFormattedNumberSearch(){
  val rows=listOf(r,r.copy(id=2,type=6),r.copy(id=3,type=2))
  val ix=RecentSearch(rows)
  assertEquals(listOf(rows[1]),ix.find("98765-43210",filter=1))
  assertEquals(listOf(rows[2]),ix.find("98765-43210",filter=2))
  assertEquals(listOf(r),ix.find("98765-43210",filter=3))
  assertEquals(listOf(r,rows[2]),ix.find("98765-43210",hideBlocked=true))
 }
}
