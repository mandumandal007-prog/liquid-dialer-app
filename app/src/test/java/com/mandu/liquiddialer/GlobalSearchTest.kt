package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class GlobalSearchTest {
 private val contacts=listOf(Contact(1,"lk","Papa","पापा","+919876543210","Home",false,null),Contact(2,"lk2","Foreign","","+449876543210","Work",false,null))
 private val notes=listOf(Note(7,"+919876543210","Papa","Delivery after lunch",100),Note(8,"+449876543210","Foreign","Separate country",200))
 private val lists=listOf(CallLists.ListItem("l","Shop",listOf(CallLists.Entry("e","+919876543210","Papa",note="Delivery ready"))))
 private val reminders=listOf(CallReminders.Reminder("+919876543210","Papa",2000,"token"))
 @Test fun oneQueryKeepsSeparateSourceIdsAndNeverCallsOrChangesSources(){
  val hits=GlobalSearch.find("papa",contacts,notes,lists,reminders)
  assertEquals(GlobalSearch.Kind.entries.toSet(),hits.map{it.kind}.toSet());assertEquals(listOf("1","7","e","token"),hits.map{it.id});assertEquals("l",hits[2].parentId)
  assertEquals("Delivery ready",lists.single().entries.single().note);assertFalse(lists.single().entries.single().done)
 }
 @Test fun formattedNumberUnicodeAndNoteTextSearchWorkWithoutMergingCountries(){
  assertEquals(4,GlobalSearch.find("+91 98765 43210",contacts,notes,lists,reminders).size)
  assertEquals(listOf("2","8"),GlobalSearch.find("+44 98765 43210",contacts,notes,lists,reminders).map{it.id})
  assertEquals(1,GlobalSearch.find("पापा",contacts,notes,lists,reminders).size)
  assertEquals(setOf(GlobalSearch.Kind.NOTE,GlobalSearch.Kind.LIST_ENTRY),GlobalSearch.find("delivery",contacts,notes,lists,reminders).map{it.kind}.toSet())
  assertTrue(GlobalSearch.find(" ",contacts,notes,lists,reminders).isEmpty());assertTrue(GlobalSearch.find("missing",contacts,notes,lists,reminders).isEmpty())
 }
}
