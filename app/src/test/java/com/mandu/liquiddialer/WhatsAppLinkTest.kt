package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.WhatsAppLink
import org.junit.Assert.*
import org.junit.Test

class WhatsAppLinkTest {
    @Test fun indianShapes() {
        assertEquals("919876543210", WhatsAppLink.digits("9876543210"))
        assertEquals("919876543210", WhatsAppLink.digits("09876543210"))
        assertEquals("919876543210", WhatsAppLink.digits("919876543210"))
        assertEquals("919876543210", WhatsAppLink.digits("+91 98765 43210"))
        assertEquals("919876543210", WhatsAppLink.digits("0091 9876543210"))
    }
    @Test fun internationalKeepsOwnCode() {
        assertEquals("442079460000", WhatsAppLink.digits("+44 20 7946 0000"))
        assertEquals("442079460000", WhatsAppLink.digits("0044 20 7946 0000"))
    }
    @Test fun unrecognisedGivesNull() {
        assertNull(WhatsAppLink.digits(""))
        assertNull(WhatsAppLink.digits("1234567890"))      // 10 digit, mobile series nahi
        assertNull(WhatsAppLink.digits("12345"))
        assertNull(WhatsAppLink.digits("1409876543"))
        assertNull(WhatsAppLink.digits("+0123456789"))
        assertEquals("https://wa.me/919876543210", WhatsAppLink.url("9876543210"))
        assertNull(WhatsAppLink.url("*#06#"))
    }
}
