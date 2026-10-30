package com.mandu.liquiddialer

import com.mandu.liquiddialer.telecom.SeriesBlock
import org.junit.Assert.*
import org.junit.Test

class SeriesBlockTest {
    @Test fun prefixMatch() {
        assertTrue(SeriesBlock.matches("140123456", listOf("140")))
        assertTrue(SeriesBlock.matches("+91 140 123456", listOf("91140")))
        assertTrue(SeriesBlock.matches("1800123456", listOf("+91 1800")))
        assertFalse(SeriesBlock.matches("9140123456", listOf("140")))   // prefix, substring nahi
        assertFalse(SeriesBlock.matches("9876543210", listOf("140")))
    }

    @Test fun safetyRules() {
        assertFalse(SeriesBlock.matches("140123456", emptyList()))              // default khali = off
        assertFalse(SeriesBlock.matches("140123456", listOf("14")))             // 3 digits se chhota prefix ignore
        assertFalse(SeriesBlock.matches("140123456", listOf("")))               // blank ignore
        assertFalse(SeriesBlock.matches("", listOf("140")))                     // private/hidden unaffected
    }
}
