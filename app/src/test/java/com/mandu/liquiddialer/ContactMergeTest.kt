package com.mandu.liquiddialer

import android.provider.ContactsContract.AggregationExceptions
import com.mandu.liquiddialer.data.ContactMerge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** ContactMerge ke op-building rules: adjacent chain, sahi type, koi self-pair nahi. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactMergeOpsTest {

    @Test
    fun linkOpsChainsAdjacentPairsKeepTogether() {
        val ops = ContactMerge.linkOps(listOf(listOf(101L), listOf(102L), listOf(103L)))
        assertEquals(2, ops.size) // 3 contacts -> 2 adjacent pairs
        assertEquals(ContactMerge.MergeOp(AggregationExceptions.TYPE_KEEP_TOGETHER, 101L, 102L), ops[0])
        assertEquals(ContactMerge.MergeOp(AggregationExceptions.TYPE_KEEP_TOGETHER, 102L, 103L), ops[1])
    }

    @Test
    fun unlinkOpsUsesKeepSeparate() {
        val ops = ContactMerge.unlinkOps(listOf(listOf(201L, 202L), listOf(203L)))
        // flatten+distinct: 3 raws -> 2 pairs, sab KEEP_SEPARATE
        assertEquals(2, ops.size)
        assertTrue(ops.all { it.type == AggregationExceptions.TYPE_KEEP_SEPARATE })
    }

    @Test
    fun singleRawContactNoOps() {
        assertTrue(ContactMerge.linkOps(listOf(listOf(101L))).isEmpty())
        assertTrue(ContactMerge.unlinkOps(listOf(listOf(101L))).isEmpty())
    }

    @Test
    fun multiRawContactContactKaPehlaChainHota() {
        // ek contact ke 2 raw entries hain (SIM + Google) - flatten ke baad bhi chain
        val ops = ContactMerge.linkOps(listOf(listOf(11L, 12L), listOf(21L)))
        assertEquals(2, ops.size) // 11->12, 12->21
    }
}
