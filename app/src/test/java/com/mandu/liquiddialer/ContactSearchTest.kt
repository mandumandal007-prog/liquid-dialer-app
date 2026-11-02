package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class ContactSearchTest {
    private fun c(i:Long,n:String,p:String)=Contact(i,"",n,n,p,"Mobile",false,null)
    @Test fun indexedResultsMatchExistingRulesAndOrder() {
        val cs=listOf(c(1,"Rahul Sharma","+91 98765 43210"),c(2,"Papa-Singh","012345"),c(3,"A.B Test","+442020123456"),c(4,"घर","3333"))
        val ix=ContactSearch(cs)
        for(q in listOf(""," ","RAH","rahul","72","77","+91 98","0123","a.b","ghar","घर","call Rahul","+"," 987 "))
            for(limit in listOf(0,1,3,99)) assertEquals(q,cs.filter { Repo.matches(it,q) }.take(limit),ix.find(q,limit))
    }
    @Test fun boundedSearchStopsAfterEnoughResults() {
        val cs=(1L..10000L).map { c(it,"Rahul $it","9876543210") };val ix=ContactSearch(cs)
        assertEquals(6,ix.find("rah",6).size);assertEquals(6,ix.lastExamined)
        assertEquals(cs.take(3),ix.find("rah",6).take(3))
        assertTrue(ContactSearch(emptyList()).find("rah").isEmpty())
        assertTrue(ContactSearch(listOf(c(1,"Changed","123"))).find("rah").isEmpty())
    }
}
