package com.mandu.liquiddialer

import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.ui.DialerCodes
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialerCodesTest {

    @Test
    fun detectsKnownCodes() {
        assertEquals(DialerCodes.Detected.Imei, DialerCodes.detect("*#06#"))
        assertEquals(DialerCodes.Detected.Secret("4636"), DialerCodes.detect("*#*#4636#*#*"))
        assertEquals(DialerCodes.Detected.Oem("*#899#"), DialerCodes.detect("*#899#"))
        assertEquals(DialerCodes.Detected.Oem("*#800#"), DialerCodes.detect("*#800#"))
    }

    @Test
    fun leavesCarrierUssdAndPlainNumbersAlone() {
        assertNull(DialerCodes.detect("*123#"))     // carrier USSD - normal call path
        assertNull(DialerCodes.detect("*99*1#"))
        assertNull(DialerCodes.detect("9876543210"))
        assertNull(DialerCodes.detect(""))
        assertNull(DialerCodes.detect("*#*#4636"))  // incomplete secret
        assertNull(DialerCodes.detect("*#*#4636#")) // still incomplete
        assertNull(DialerCodes.detect("*#06"))      // incomplete IMEI
        assertNull(DialerCodes.detect("*#8"))       // too short
    }

    @Test
    fun ignoresWhitespaceAndPauses() {
        assertEquals(DialerCodes.Detected.Imei, DialerCodes.detect("* #06#"))
    }

    @Test
    fun sendSecretNeedsDefaultDialerRole() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        // role missing -> null (caller asks for role), no exception, nothing sent
        assertNull(DialerCodes.sendSecret(ctx, "4636"))
    }
}
