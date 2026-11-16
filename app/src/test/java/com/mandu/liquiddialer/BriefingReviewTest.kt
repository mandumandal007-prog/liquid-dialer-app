package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class BriefingReviewTest {
 private val n="+919876543210"
 private fun snapshot(topics:List<CallChecklist.Item> = listOf(CallChecklist.Item("t1","Delivery time",false)),lists:List<CallBriefing.ListEntry> = listOf(CallBriefing.ListEntry("l1","Rice orders",CallLists.Entry("e1",n,"Papa",note="Confirm 2 bags",dueAt=1700000000000))))=CallBriefing.Snapshot(n,CallBriefing.Section(emptyList()),CallBriefing.Section(topics),CallBriefing.Section(CallEstimate.Draft()),CallBriefing.Section(lists))
 @Test fun defaultOff(){assertFalse(LdSettings().briefingReview)}
 @Test fun combinedPending(){val q=BriefingReview.from(snapshot());assertEquals(listOf("Topics","Call Lists"),q.rows.map{it.source});assertTrue(q.errors.isEmpty())}
 @Test fun discussedExcluded(){assertTrue(BriefingReview.from(snapshot(topics=listOf(CallChecklist.Item("d","Old",true)),lists=emptyList())).rows.isEmpty())}
 @Test fun doneListsExcluded(){assertTrue(BriefingReview.from(snapshot(topics=emptyList(),lists=listOf(CallBriefing.ListEntry("l","Done",CallLists.Entry("e",n,"",true))))).rows.isEmpty())}
 @Test fun foreignIdentityExcluded(){assertEquals(1,BriefingReview.from(snapshot(lists=listOf(CallBriefing.ListEntry("l","Foreign",CallLists.Entry("e","+449876543210",""))))).rows.size)}
 @Test fun punctuationIdentityIncluded(){assertEquals(2,BriefingReview.from(snapshot(lists=listOf(CallBriefing.ListEntry("l","Local",CallLists.Entry("e","+91 98765 43210",""))))).rows.size)}
 @Test fun errorSectionNotEmptyClaim(){val q=BriefingReview.from(snapshot().copy(topics=CallBriefing.Section(error=true)));assertEquals(listOf("Topics"),q.errors);assertEquals(1,q.rows.size)}
 @Test fun bothErrors(){val q=BriefingReview.from(snapshot().copy(topics=CallBriefing.Section(error=true),lists=CallBriefing.Section(error=true)));assertEquals(2,q.errors.size);assertTrue(q.rows.isEmpty())}
 @Test fun failingValueNotLeaked(){val s=snapshot();assertEquals(1,BriefingReview.from(s.copy(topics=s.topics.copy(error=true))).rows.size)}
 @Test fun markUndo(){val q=BriefingReview.from(snapshot());val id=q.rows.first().id;val one=BriefingReview.toggle(q,emptySet(),id);assertEquals(setOf(id),one);assertTrue(BriefingReview.toggle(q,one,id).isEmpty())}
 @Test fun staleMarksIgnored(){val q=BriefingReview.from(snapshot());assertEquals(0,BriefingReview.count(q,setOf("stale")));assertEquals(1,BriefingReview.toggle(q,setOf("stale"),q.rows[0].id).size)}
 @Test(expected=IllegalArgumentException::class)fun unknownIdRefused(){BriefingReview.toggle(BriefingReview.from(snapshot()),emptySet(),"no")}
 @Test fun sources(){val q=BriefingReview.from(snapshot());assertEquals(1,BriefingReview.visible(q,"","Topics",false,emptySet()).size);assertEquals(1,BriefingReview.visible(q,"","Call Lists",false,emptySet()).size)}
 @Test fun allTermsCaseInsensitive(){assertEquals(1,BriefingReview.visible(BriefingReview.from(snapshot())," BAGS  confirm ","All",false,emptySet()).size)}
 @Test fun unreviewedIntersection(){val q=BriefingReview.from(snapshot());assertTrue(BriefingReview.visible(q,"","Topics",true,setOf(q.rows[0].id)).isEmpty());assertEquals(1,BriefingReview.visible(q,"","Call Lists",true,setOf(q.rows[0].id)).size)}
 @Test fun marksDoNotChangeSource(){val s=snapshot();val q=BriefingReview.from(s);BriefingReview.toggle(q,emptySet(),q.rows[0].id);assertFalse(s.topics.value!![0].done);assertFalse(s.lists.value!![0].entry.done)}
 @Test fun plannedValuePreserved(){assertEquals(1700000000000,BriefingReview.from(snapshot()).rows[1].dueAt)}
 @Test fun hindiSearch(){val q=BriefingReview.from(snapshot(topics=listOf(CallChecklist.Item("h","कल सामान भेजना",false))));assertEquals(1,BriefingReview.visible(q,"सामान कल","All",false,emptySet()).size)}
 @Test(expected=IllegalArgumentException::class)fun invalidSource(){BriefingReview.visible(BriefingReview.from(snapshot()),"","bad",false,emptySet())}
 @Test(expected=IllegalArgumentException::class)fun invalidNumber(){BriefingReview.from(snapshot().copy(number="*123#"))}
 @Test fun stableIds(){val s=snapshot();assertEquals(BriefingReview.from(s),BriefingReview.from(s));assertEquals(2,BriefingReview.from(s).rows.map{it.id}.distinct().size)}
 @Test fun idsKeepListScope(){val e=CallLists.Entry("same",n,"");val q=BriefingReview.from(snapshot(topics=emptyList(),lists=listOf(CallBriefing.ListEntry("a","A",e),CallBriefing.ListEntry("b","B",e))));assertEquals(2,q.rows.map{it.id}.distinct().size)}
 @Test fun finderRoute(){val e=SettingsFinder.search("briefing review queue").first();assertEquals("briefingreview",e.route)}
 @Test fun queryCapped(){val q=BriefingReview.from(snapshot());assertEquals(BriefingReview.visible(q,"x".repeat(100),"All",false,emptySet()),BriefingReview.visible(q,"x".repeat(100)+"Delivery","All",false,emptySet()))}
}
