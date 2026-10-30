package com.mandu.liquiddialer

import com.mandu.liquiddialer.telecom.QuietHours
import org.junit.Assert.*
import org.junit.Test

class QuietHoursTest {

    @Test fun sameDayWindow() {
        assertTrue(QuietHours.inWindow(13 * 60, 9 * 60, 17 * 60))
        assertFalse(QuietHours.inWindow(8 * 60, 9 * 60, 17 * 60))
        assertFalse(QuietHours.inWindow(17 * 60, 9 * 60, 17 * 60))  // end exclusive
        assertTrue(QuietHours.inWindow(9 * 60, 9 * 60, 17 * 60))    // start inclusive
    }

    @Test fun overnightWindow() {
        assertTrue(QuietHours.inWindow(23 * 60, 1320, 420))   // 23:00 in 22:00-07:00
        assertTrue(QuietHours.inWindow(3 * 60, 1320, 420))    // 03:00
        assertTrue(QuietHours.inWindow(0, 1320, 420))         // midnight
        assertFalse(QuietHours.inWindow(12 * 60, 1320, 420))  // noon
        assertFalse(QuietHours.inWindow(7 * 60, 1320, 420))   // 07:00 end exclusive
    }

    @Test fun silenceNeedsEverything() {
        // happy path: 23:30, window 22-07, not fav
        assertTrue(QuietHours.shouldSilence(true, 30, true, 1410, 1320, 420, true, false))
        // switch off -> never silence
        assertFalse(QuietHours.shouldSilence(false, 30, true, 1410, 1320, 420, true, false))
        // pre-29 cannot silence
        assertFalse(QuietHours.shouldSilence(true, 28, true, 1410, 1320, 420, true, false))
        // outgoing never silenced
        assertFalse(QuietHours.shouldSilence(true, 30, false, 1410, 1320, 420, true, false))
        // outside window
        assertFalse(QuietHours.shouldSilence(true, 30, true, 720, 1320, 420, true, false))
        // favourite bypass on -> favourite rings
        assertFalse(QuietHours.shouldSilence(true, 30, true, 1410, 1320, 420, true, true))
        // favourite bypass off -> favourite silenced too
        assertTrue(QuietHours.shouldSilence(true, 30, true, 1410, 1320, 420, false, true))
    }

    @Test fun formatting() {
        assertEquals("22:00", QuietHours.fmt(1320))
        assertEquals("07:00", QuietHours.fmt(420))
        assertEquals("00:00", QuietHours.fmt(0))
        assertEquals("23:30", QuietHours.fmt(1410))
    }
}
