package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class ReviewPriorityTest {
 private fun rows(n:Int=12)=(1..n).map{BriefingReview.Row("$it","Topics","Item$it","Text$it")}
 @Test fun defaultsOff(){assertFalse(LdSettings().reviewPriority);assertFalse(LdSettings().reviewSlim)}
 @Test fun addAndUndo(){val r=rows();assertEquals(setOf("2"),ReviewPriority.toggle(r,emptySet(),"2"));assertTrue(ReviewPriority.toggle(r,setOf("2"),"2").isEmpty())}
 @Test fun maxTen(){val r=rows();val ten=(1..10).map{"$it"}.toSet();assertTrue(runCatching{ReviewPriority.toggle(r,ten,"11")}.isFailure);assertEquals(9,ReviewPriority.toggle(r,ten,"1").size)}
 @Test fun unknownRefused(){assertTrue(runCatching{ReviewPriority.toggle(rows(),emptySet(),"x")}.isFailure)}
 @Test fun duplicateIdentitiesRefused(){val r=rows(1);assertTrue(runCatching{ReviewPriority.toggle(r+r,emptySet(),"1")}.isFailure)}
 @Test fun staleIgnoredOnToggle(){assertEquals(setOf("1"),ReviewPriority.toggle(rows(),setOf("old"),"1"))}
 @Test fun stableGroupOrderNotSelectionOrder(){assertEquals(listOf("2","4","1","3"),ReviewPriority.order(rows(4),linkedSetOf("4","2"),false).map{it.id})}
 @Test fun emptySelectionOriginalOrder(){assertEquals(rows(),ReviewPriority.order(rows(),emptySet(),false))}
 @Test fun onlySelectionOrder(){assertEquals(listOf("2","4"),ReviewPriority.order(rows(4),setOf("4","2"),true).map{it.id})}
 @Test fun emptyPriorityOnlyHonestEmpty(){assertTrue(ReviewPriority.order(rows(),emptySet(),true).isEmpty())}
 @Test fun unknownPriorityNotInjected(){assertEquals(rows(),ReviewPriority.order(rows(),setOf("unknown"),false));assertTrue(ReviewPriority.order(rows(),setOf("unknown"),true).isEmpty())}
 @Test fun emptyRows(){assertTrue(ReviewPriority.order(emptyList(),setOf("1"),false).isEmpty())}
 @Test fun sourceUnchanged(){val r=rows();val saved=r.toList();ReviewPriority.toggle(r,emptySet(),"2");ReviewPriority.order(r,setOf("2"),false);assertEquals(saved,r)}
 @Test fun detailsAndDatesKept(){val r=listOf(BriefingReview.Row("id","Call Lists","घर के सामान","Full details",123));assertEquals(r,ReviewPriority.order(r,setOf("id"),true))}
 @Test fun reviewedAndPriorityIndependent(){val q=BriefingReview.Queue(rows(3),emptyList());val visible=BriefingReview.visible(q,"","All",true,setOf("2"));assertEquals(listOf("3","1"),ReviewPriority.order(visible,setOf("2","3"),false).map{it.id})}
 @Test fun queryAndSourceStillIntersect(){val r=listOf(BriefingReview.Row("a","Topics","Rice bags",""),BriefingReview.Row("b","Call Lists","Rice bags",""));val filtered=BriefingReview.visible(BriefingReview.Queue(r,emptyList()),"rice","Topics",false,emptySet());assertEquals(listOf("a"),ReviewPriority.order(filtered,setOf("a","b"),true).map{it.id})}
 @Test fun capacityReusableAfterUndo(){val r=rows();val ten=(1..10).map{"$it"}.toSet();assertEquals(10,ReviewPriority.toggle(r,ReviewPriority.toggle(r,ten,"1"),"11").size)}
 @Test fun finderBothRoutes(){assertTrue(SettingsFinder.search("slim review rows").any{it.route=="reviewoptions"});assertTrue(SettingsFinder.search("review session priorities").any{it.route=="reviewoptions"})}
 @Test fun sourceScopedIdsRemainDistinct(){val r=listOf(BriefingReview.Row("topic:x","Topics","A",""),BriefingReview.Row("list:x","Call Lists","B",""));assertEquals(listOf("list:x","topic:x"),ReviewPriority.order(r,setOf("list:x"),false).map{it.id})}
 @Test fun overCapStaleCleaned(){assertEquals(setOf("1"),ReviewPriority.toggle(rows(),(1..20).map{"stale$it"}.toSet(),"1"))}
}
