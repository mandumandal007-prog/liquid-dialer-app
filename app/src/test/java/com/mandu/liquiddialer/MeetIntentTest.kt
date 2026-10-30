package com.mandu.liquiddialer

import com.mandu.liquiddialer.telecom.CallManager
import org.junit.Assert.*
import org.junit.Test

class MeetIntentTest {

    @Test fun cleanKeepsDigitsAndPlus() {
        assertEquals("+918388880466", CallManager.cleanMeetNumber("+91 83888-80466"))
        assertEquals("8388880466", CallManager.cleanMeetNumber("83888 80466"))
    }

    @Test fun cleanStripsPauseAndJunk() {
        assertEquals("1800123", CallManager.cleanMeetNumber("1800,123"))
        assertEquals("1800123", CallManager.cleanMeetNumber("(1800) 123"))
    }

    @Test fun cleanRejectsTooShort() {
        assertNull(CallManager.cleanMeetNumber(""))
        assertNull(CallManager.cleanMeetNumber("12"))
        assertNull(CallManager.cleanMeetNumber("*,#"))
    }
}
