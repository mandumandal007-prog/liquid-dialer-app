package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.BlockLog
import com.mandu.liquiddialer.telecom.SpamGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** SpamGuard pure rules: TRAI 140/160 series, international pehchan, verification labels, reject decision. */
class SpamGuardTest {

    @Test fun nsnStripsCountryAndTrunk() {
        assertEquals("1409876543", SpamGuard.nsn("+91 14098 76543"))
        assertEquals("1409876543", SpamGuard.nsn("00911409876543"))
        assertEquals("1409876543", SpamGuard.nsn("911409876543"))
        assertEquals("1409876543", SpamGuard.nsn("01409876543"))
        assertEquals("9876543210", SpamGuard.nsn("98765 43210"))
    }

    @Test fun nsnRejectsUnrecognisedShapes() {
        assertNull(SpamGuard.nsn("14045551234"))      // 11 digit, 0 se shuru nahi: foreign-jaisa, Indian nahi
        assertNull(SpamGuard.nsn("+14045551234"))
        assertNull(SpamGuard.nsn("+9114098765"))       // +91 ke baad 10 digit nahi
        assertNull(SpamGuard.nsn("140987654"))         // 9 digit
        assertNull(SpamGuard.nsn("1409876543210"))     // 13 digit
        assertNull(SpamGuard.nsn(""))
    }

    @Test fun seriesDetects140And160() {
        assertEquals(140, SpamGuard.series("1409876543"))
        assertEquals(140, SpamGuard.series("+91 140 9876543"))
        assertEquals(160, SpamGuard.series("1609876543"))
        assertEquals(160, SpamGuard.series("00911609876543"))
        assertEquals(160, SpamGuard.series("911609876543"))
        assertEquals(140, SpamGuard.series("01409876543"))
    }

    @Test fun seriesIgnoresForeignShortAndOddLengths() {
        assertNull(SpamGuard.series("+14045551234"))      // US +1 404...
        assertNull(SpamGuard.series("14045551234"))       // bina plus ka US-jaisa 11 digit: 140 tag nahi
        assertNull(SpamGuard.series("16045551234"))       // bina plus ka Canada-jaisa 11 digit: 160 tag nahi
        assertNull(SpamGuard.series("+16045551234"))
        assertNull(SpamGuard.series("0044207946000"))
        assertNull(SpamGuard.series("14012345678901"))    // lambai galat
        assertNull(SpamGuard.series("140"))
        assertNull(SpamGuard.series("1609"))
        assertNull(SpamGuard.series("9876543210"))
        assertNull(SpamGuard.series("9140987654"))        // beech me 140 hai, prefix nahi
        assertNull(SpamGuard.series(""))
    }

    @Test fun internationalDetection() {
        assertTrue(SpamGuard.isInternational("+44 20 7946 0000"))
        assertTrue(SpamGuard.isInternational("0044207946000"))
        assertTrue(SpamGuard.isInternational("+1 404 555 1234"))
        assertFalse(SpamGuard.isInternational("+91 98765 43210"))
        assertFalse(SpamGuard.isInternational("00919876543210"))
        assertFalse(SpamGuard.isInternational("9876543210"))
        assertFalse(SpamGuard.isInternational(""))
    }

    @Test fun verificationLabels() {
        assertEquals("Network verified", SpamGuard.verificationLabel(SpamGuard.VERIFY_PASSED))
        assertEquals("Number verify nahi hua", SpamGuard.verificationLabel(SpamGuard.VERIFY_FAILED))
        assertNull(SpamGuard.verificationLabel(0))
        assertNull(SpamGuard.verificationLabel(99))
    }

    @Test fun rejectDecision() {
        // 140 promo block on -> reject; 160 kabhi reject nahi
        assertEquals(BlockLog.REASON_PROMO, SpamGuard.rejectReason("1409876543", known = false, never = false, promoBlock = true, intlBlock = false))
        assertNull(SpamGuard.rejectReason("1409876543", known = false, never = false, promoBlock = false, intlBlock = false))
        assertNull(SpamGuard.rejectReason("1609876543", known = false, never = false, promoBlock = true, intlBlock = true))
        // unknown intl
        assertEquals(BlockLog.REASON_INTL, SpamGuard.rejectReason("+442079460000", known = false, never = false, promoBlock = false, intlBlock = true))
        assertNull(SpamGuard.rejectReason("+442079460000", known = true, never = false, promoBlock = false, intlBlock = true))
        assertNull(SpamGuard.rejectReason("+919876543210", known = false, never = false, promoBlock = false, intlBlock = true))
        // never pin hamesha jeet-ta hai
        assertNull(SpamGuard.rejectReason("1409876543", known = false, never = true, promoBlock = true, intlBlock = true))
        assertNull(SpamGuard.rejectReason("+442079460000", known = false, never = true, promoBlock = true, intlBlock = true))
        assertNull(SpamGuard.rejectReason("", known = false, never = false, promoBlock = true, intlBlock = true))
    }
}
