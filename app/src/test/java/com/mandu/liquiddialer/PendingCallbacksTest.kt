package com.mandu.liquiddialer
import android.provider.CallLog.Calls.*
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class PendingCallbacksTest {
    private fun r(id:Long,n:String,t:Int,d:Long=0)=Recent(id,n,null,t,id*1000,d,null,null)
    @Test fun connectedCallClosesOnlyOlderMissesAndFailedAttemptDoesNot() {
        val rows=listOf(r(1,"123",MISSED_TYPE),r(2,"123",OUTGOING_TYPE),r(3,"123",MISSED_TYPE),r(4,"456",MISSED_TYPE))
        val p=PendingCallbacks.from(rows)
        assertEquals(listOf("456","123"),p.map {it.latest.number});assertEquals(2,p.last().missed)
        assertEquals(listOf("456"),PendingCallbacks.from(rows+r(5,"123",INCOMING_TYPE,10)).map {it.latest.number})
        val again=PendingCallbacks.from(rows+r(5,"123",OUTGOING_TYPE,10)+r(6,"123",MISSED_TYPE))
        assertEquals(1,again.first().missed);assertEquals(6L,again.first().latest.id)
    }
    @Test fun fullInternationalIdentityAndUnorderedEventsStaySeparate() {
        val rows=listOf(r(8,"+1 2125551234",MISSED_TYPE),r(7,"+44 2125551234",MISSED_TYPE),r(9,"0012125551234",MISSED_TYPE))
        val p=PendingCallbacks.from(rows.reversed())
        assertEquals(2,p.size);assertEquals(2,p.first().missed);assertEquals(9L,p.first().latest.id)
        assertTrue(PendingCallbacks.from(listOf(r(1,"",MISSED_TYPE),r(2,"private",MISSED_TYPE),r(3,"123",REJECTED_TYPE),r(4,"456",BLOCKED_TYPE))).isEmpty())
    }
}
