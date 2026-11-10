package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CallBriefingTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val n="+91 98765 43210"
 @Before fun clear(){listOf("notes.json","manual_call_lists.json","call_checklists.json","call_estimates.json").forEach{f->listOf("",".bak",".new").forEach{File(ctx.filesDir,f+it).delete()}}}
 @Test fun exactIdentityAndNewestFirst(){
  val notes=listOf(Note(1,"9876543210","Local","old",10),Note(2,"+919876543210","Papa","new",20),Note(3,"+449876543210","Foreign","foreign",40),Note(4,"*123#","Code","code",50))
  assertEquals(listOf(2L,1L),CallBriefing.notes(n,notes).map{it.id})
  val ls=listOf(CallLists.ListItem("a","List",listOf(CallLists.Entry("1",n,"Papa",true,updatedAt=40),CallLists.Entry("2","9876543210","Papa",updatedAt=10),CallLists.Entry("3","+449876543210","Foreign"))))
  assertEquals(listOf("2","1"),CallBriefing.lists(n,ls).map{it.entry.id})
 }
 @Test fun localReadsPreserveAllSavedBytesAndFilter(){
  Repo.addNote(ctx,n,"Papa","Bring MILK");CallChecklist.add(ctx,n,"Discuss delivery")
  val id=CallLists.create(ctx,"Work").single().id;CallLists.add(ctx,id,n,"Papa")
  val draft=CallEstimate.Draft(lines=listOf(CallEstimate.line("Rice","2","50")));CallEstimate.save(ctx,n,0,draft)
  val before=ctx.filesDir.listFiles()!!.filter{it.extension=="json"}.associate{it.name to it.readBytes().toList()}
  val s=CallBriefing.load(ctx,n)
  assertEquals(1,CallBriefing.visibleNotes(s,"milk").size);assertEquals(1,CallBriefing.visibleTopics(s,"delivery",true).size);assertTrue(CallBriefing.visibleTopics(s,"missing",false).isEmpty())
  assertEquals("100.00",CallEstimate.money(CallEstimate.totals(s.estimate.value!!).total));assertEquals(1,CallBriefing.visibleLists(s,"work").size)
  before.forEach{(f,b)->assertEquals(b,File(ctx.filesDir,f).readBytes().toList())}
 }
 @Test fun brokenSectionIsNotEmptyAndDoesNotHideHealthySections(){
  Repo.addNote(ctx,n,"Papa","Keep");File(ctx.filesDir,"call_checklists.json").writeText("broken")
  val s=CallBriefing.load(ctx,n);assertTrue(s.topics.error);assertNull(s.topics.value);assertFalse(s.notes.error);assertEquals("Keep",s.notes.value!!.single().text);assertFalse(s.estimate.error)
  assertEquals("broken",File(ctx.filesDir,"call_checklists.json").readText())
 }
 @Test fun noDataHasHonestEmptySectionsAndRejectsCodes(){
  val s=CallBriefing.load(ctx,n);assertTrue(s.notes.value!!.isEmpty());assertTrue(s.topics.value!!.isEmpty());assertTrue(s.lists.value!!.isEmpty());assertTrue(s.estimate.value!!.lines.isEmpty())
  assertThrows(IllegalArgumentException::class.java){CallBriefing.load(ctx,"*123#")}
 }
}
