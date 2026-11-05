package com.mandu.liquiddialer

import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.BlockLog
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BlockLogTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    @Before fun clean() { BlockLog.clear(ctx) }

    @Test fun appendKeepsNewestFirstAndReason() {
        BlockLog.append(ctx, "+91 98765 43210", BlockLog.REASON_SERIES, ts = 1000L)
        BlockLog.append(ctx, "", BlockLog.REASON_PRIVATE, ts = 2000L)
        val list = BlockLog.list(ctx)
        assertEquals(2, list.size)
        assertEquals(BlockLog.REASON_PRIVATE, list[0].reason)
        assertEquals("", list[0].display)
        assertEquals(BlockLog.REASON_SERIES, list[1].reason)
        assertEquals(2000L, list[0].ts)
    }

    @Test fun capDropsOldest() {
        repeat(BlockLog.MAX + 25) { BlockLog.append(ctx, "98765${10000 + it}", BlockLog.REASON_UNKNOWN, ts = 1000L + it) }
        val list = BlockLog.list(ctx)
        assertEquals(BlockLog.MAX, list.size)
        assertEquals(1000L + BlockLog.MAX + 24, list.first().ts)
        assertEquals(1000L + 25, list.last().ts)
    }

    @Test fun internationalIdentityNormalized() {
        BlockLog.append(ctx, "+91 98765 43210", BlockLog.REASON_UNKNOWN, ts = 5000L)
        BlockLog.append(ctx, "0091 98765 43210", BlockLog.REASON_UNKNOWN, ts = 6000L)
        BlockLog.append(ctx, "098765 43210", BlockLog.REASON_UNKNOWN, ts = 7000L)
        val list = BlockLog.list(ctx)
        assertEquals(3, list.size)
        assertEquals("+919876543210", list[2].idn)
        assertEquals("+919876543210", list[1].idn)
        assertEquals("+919876543210", list[0].idn)
    }

    @Test fun foreignZeroAndLocalNumbersNotIndianised() {
        BlockLog.append(ctx, "+39 06 1234567", BlockLog.REASON_SERIES, ts = 1000L)
        BlockLog.append(ctx, "022 4890 1234", BlockLog.REASON_SERIES, ts = 1001L)
        val list = BlockLog.list(ctx)
        assertEquals("+39061234567", list[1].idn)   // significant zero bacha
        assertEquals("local:02248901234", list[0].idn) // landline local namespace, +91 nahi
    }

    @Test fun weirdCharsAndBadReasonRejected() {
        BlockLog.append(ctx, "call-me-maybe", BlockLog.REASON_UNKNOWN, ts = 1000L)
        BlockLog.append(ctx, "+91 98765 43210", "virus", ts = 1000L)
        assertTrue(BlockLog.list(ctx).isEmpty())
    }

    @Test fun clearEmpties() {
        BlockLog.append(ctx, "+91 98765 43210", BlockLog.REASON_SERIES, ts = 1000L)
        BlockLog.clear(ctx)
        assertTrue(BlockLog.list(ctx).isEmpty())
    }
}
