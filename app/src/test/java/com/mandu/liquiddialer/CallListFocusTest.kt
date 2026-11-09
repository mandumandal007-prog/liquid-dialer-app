package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class CallListFocusTest {
 private val a=CallLists.Entry("a","+44 2025 550123","Office",false,"busy","Bring REPORT")
 private val b=CallLists.Entry("b","+1 2025 550123","Family",true,"done","report shared")
 private val c=CallLists.Entry("c","9876543210","Clinic",false,"later","Tomorrow")
 private val entries=listOf(a,b,c)
 @Test fun nameNoteCaseAndFormattedNumberSearch(){assertEquals(listOf(a),CallListFocus.find(entries,"office"));assertEquals(listOf(a,b),CallListFocus.find(entries,"REPORT"));assertEquals(listOf(a),CallListFocus.find(entries,"+44 (2025) 550123"))}
 @Test fun stateAndExactOutcomeCombine(){assertEquals(listOf(a),CallListFocus.find(entries,"report","Pending"));assertEquals(listOf(b),CallListFocus.find(entries,"","Done"));assertEquals(listOf(c),CallListFocus.find(entries,outcome="later"));assertTrue(CallListFocus.find(entries,"", "Done","busy").isEmpty())}
 @Test fun blankQueryAndNoMatchDoNotMutate(){assertEquals(entries,CallListFocus.find(entries,"   "));assertTrue(CallListFocus.find(entries,"No match").isEmpty());assertEquals("busy",a.outcome)}
 @Test fun fullInternationalIdentityPreserved(){assertEquals(listOf(a,b),CallListFocus.find(entries,"2025550123"));assertEquals(listOf(b),CallListFocus.find(entries,"+1 2025"));assertEquals(a.number,CallListFocus.find(entries,"Office").single().number)}
 @Test fun scopeSnapshotRetainsOrderAndSavedOutcomeChanges(){val list=CallLists.ListItem("l","Work",entries);val ids=listOf("c","a");val changed=list.copy(entries=listOf(a.copy(outcome="done",done=true),b,c));assertEquals(listOf("c","a"),CallListFocus.session(changed,ids).entries.map{it.id});assertTrue(CallListFocus.session(changed,ids).entries.last().done)}
 @Test fun missingIdsAreNotReplacedWithUnselectedEntries(){val list=CallLists.ListItem("l","Work",entries);assertEquals(listOf(c),CallListFocus.session(list,listOf("missing","c","c")).entries);assertTrue(CallListFocus.session(list,emptyList()).entries.isEmpty())}
}
