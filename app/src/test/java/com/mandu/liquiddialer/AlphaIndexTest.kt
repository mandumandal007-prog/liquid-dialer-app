package com.mandu.liquiddialer

import com.mandu.liquiddialer.ui.AlphaIndex
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Alpha rail index helper: letter bucketing + first-occurrence index mapping. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlphaIndexTest {

    @Test fun letterBuckets() {
        assertEquals("A", AlphaIndex.letterOf("arjun"))
        assertEquals("A", AlphaIndex.letterOf("Arjun"))
        assertEquals("  R".let { AlphaIndex.letterOf("  ramesh") }, "R")
        assertEquals("म", AlphaIndex.letterOf("मम्मी"))
        assertEquals("क", AlphaIndex.letterOf("किशन"))
        assertEquals("#", AlphaIndex.letterOf("911234567890"))
        assertEquals("#", AlphaIndex.letterOf("+91 83888"))
        assertEquals("#", AlphaIndex.letterOf(""))
        assertEquals("#", AlphaIndex.letterOf("   "))
    }

    @Test fun firstOccurrenceIndexWithHeadOffset() {
        val names = listOf("Aman", "Arjun", "Bablu", "Mandu", "मम्मी", "Rahul")
        val idx = AlphaIndex.indexOf(names, headItems = 1)
        assertEquals(listOf("A" to 1, "B" to 3, "M" to 4, "म" to 5, "R" to 6), idx)
    }

    @Test fun distinctOrderPreservedNoDuplicates() {
        val names = listOf("Aman", "Amit", "Anil", "Bablu")
        val idx = AlphaIndex.indexOf(names, headItems = 1)
        assertEquals(2, idx.size)          // A, B sirf
        assertEquals("A", idx[0].first)
        assertEquals(1, idx[0].second)     // pehla A contact = item 1 (head ke baad)
    }

    @Test fun emptyListGivesEmptyRail() {
        assertEquals(emptyList<Pair<String, Int>>(), AlphaIndex.indexOf(emptyList(), headItems = 1))
    }

    @Test fun zeroHeadOffsetWhenNoHeader() {
        val idx = AlphaIndex.indexOf(listOf("Aman", "Bablu"), headItems = 0)
        assertEquals(listOf("A" to 0, "B" to 1), idx)
    }
}
