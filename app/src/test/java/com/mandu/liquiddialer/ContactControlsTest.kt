package com.mandu.liquiddialer

import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.ContactControls
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Contact Control Center: per-number controls sab default OFF, koi silent migration nahi.
 *  Record/announce = global rule YA explicit per-number pin; pin kabhi global ko band nahi karta. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactControlsTest {

    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @After
    fun tearDown() = ctx.getSharedPreferences("contact_controls", android.content.Context.MODE_PRIVATE)
        .edit().clear().apply()

    @Test
    fun blockNeverRoundTripAurAliases() {
        assertFalse(ContactControls.blockNever(ctx, "+91 98765 43210"))
        ContactControls.setBlockNever(ctx, "98765 43210", true)
        assertTrue(ContactControls.blockNever(ctx, "+91 98765 43210"))
        assertTrue(ContactControls.blockNever(ctx, "919876543210"))
        assertTrue(ContactControls.blockNever(ctx, "09876543210"))
        ContactControls.setBlockNever(ctx, "+919876543210", false)
        assertFalse(ContactControls.blockNever(ctx, "9876543210"))
    }

    @Test
    fun blockNeverListAurForeignZero() {
        assertTrue(ContactControls.blockNeverList(ctx).isEmpty())
        ContactControls.setBlockNever(ctx, "0044 20 7946 0018", true)
        ContactControls.setBlockNever(ctx, "0120 456789", true)  // Indian mobile pattern nahi: local namespace
        val l = ContactControls.blockNeverList(ctx)
        assertEquals(2, l.size)
        assertTrue(l.contains("+442079460018"))
        assertTrue(l.any { it.startsWith("local:") })
        ContactControls.setBlockNever(ctx, "+44 20 7946 0018", false)
        assertFalse(ContactControls.blockNeverList(ctx).contains("+442079460018"))
        assertEquals(1, ContactControls.blockNeverList(ctx).size)
    }

    @Test
    fun sabDefaultsOff() {
        val n = "+91 98765 43210"
        assertNull(ContactControls.simPin(ctx, n))
        assertFalse(ContactControls.quietAllow(ctx, n))
        assertFalse(ContactControls.recordAlways(ctx, n))
        assertFalse(ContactControls.announceAlways(ctx, n))
    }

    @Test
    fun prefsRoundTripNumberKeyed() {
        val n = "9876543210"
        ContactControls.setSimPin(ctx, n, "simY")
        ContactControls.setQuietAllow(ctx, n, true)
        ContactControls.setRecordAlways(ctx, n, true)
        ContactControls.setAnnounceAlways(ctx, n, true)
        // formatting alag ho to bhi same 10-digit key
        assertEquals("simY", ContactControls.simPin(ctx, "+91 98765 43210"))
        assertTrue(ContactControls.quietAllow(ctx, "(98765) 43210"))
        assertTrue(ContactControls.recordAlways(ctx, n))
        assertTrue(ContactControls.announceAlways(ctx, n))
        // doosra number touch nahi hota
        assertNull(ContactControls.simPin(ctx, "9123456780"))
        assertFalse(ContactControls.quietAllow(ctx, "9123456780"))
        // pin hatao -> wapas auto (null)
        ContactControls.setSimPin(ctx, n, null)
        assertNull(ContactControls.simPin(ctx, n))
        assertTrue(ContactControls.quietAllow(ctx, n))
    }

    @Test
    fun blankNumberKuchNahiKarta() {
        ContactControls.setSimPin(ctx, "", "simX")
        ContactControls.setQuietAllow(ctx, "  ", true)
        assertNull(ContactControls.simPin(ctx, ""))
        assertFalse(ContactControls.quietAllow(ctx, ""))
        assertFalse(ContactControls.recordAlways(ctx, ""))
        assertFalse(ContactControls.announceAlways(ctx, ""))
    }

    @Test
    fun keySuffixCollisionNahi() {
        // Indian formats same number -> same key
        assertEquals(com.mandu.liquiddialer.data.ContactControls.k("9876543210"), com.mandu.liquiddialer.data.ContactControls.k("+91 98765 43210"))
        assertEquals(com.mandu.liquiddialer.data.ContactControls.k("9876543210"), com.mandu.liquiddialer.data.ContactControls.k("09876543210"))
        // alag desh, same last-10 suffix -> ALAG keys (koi shared controls nahi)
        val us = com.mandu.liquiddialer.data.ContactControls.k("+1 202 555 9876")
        val uk = com.mandu.liquiddialer.data.ContactControls.k("+44 20 2555 9876")
        assertTrue(us != uk)
        assertEquals("+12025559876", us)
        // 00 international prefix bhi same identity
        assertEquals(com.mandu.liquiddialer.data.ContactControls.k("+91 98765 43210"), com.mandu.liquiddialer.data.ContactControls.k("0091 98765 43210"))
        // foreign leading zero (Italy style) kabhi strip nahi hota, collision nahi
        val it = com.mandu.liquiddialer.data.ContactControls.k("+39 06 1234 5678")
        assertEquals("+390612345678", it)
        assertEquals("local:0612345678", com.mandu.liquiddialer.data.ContactControls.k("06 1234 5678"))
        assertEquals("+390612345678", com.mandu.liquiddialer.data.ContactControls.k("0039 06 1234 5678"))
        // prefs bhi alag: ek pe pin, doosre pe kuch nahi
        com.mandu.liquiddialer.data.ContactControls.setQuietAllow(ctx, "+1 202 555 9876", true)
        assertTrue(com.mandu.liquiddialer.data.ContactControls.quietAllow(ctx, "+1 202 555 9876"))
        assertFalse(com.mandu.liquiddialer.data.ContactControls.quietAllow(ctx, "+44 20 2555 9876"))
    }

    @Test
    fun recordDecisionMatrix() {
        // global off: sirf explicit per-number pin record karta hai
        assertFalse(ContactControls.shouldAutoRecord(globalAuto = false, unknownOnly = false, isKnown = false, contactRecord = false))
        assertTrue(ContactControls.shouldAutoRecord(false, false, isKnown = true, contactRecord = true))
        // global on, all calls
        assertTrue(ContactControls.shouldAutoRecord(true, false, isKnown = true, contactRecord = false))
        assertTrue(ContactControls.shouldAutoRecord(true, false, isKnown = false, contactRecord = false))
        // global unknown-only: known number record nahi, unknown record
        assertFalse(ContactControls.shouldAutoRecord(true, true, isKnown = true, contactRecord = false))
        assertTrue(ContactControls.shouldAutoRecord(true, true, isKnown = false, contactRecord = false))
        // per-number pin known number ko bhi unknown-only mode me record karata hai
        assertTrue(ContactControls.shouldAutoRecord(true, true, isKnown = true, contactRecord = true))
    }

    @Test
    fun announceDecisionMatrix() {
        // global off: sirf explicit per-number pin announce karta hai
        assertFalse(ContactControls.shouldAnnounce(globalAnnounce = false, onlyFav = false, isFav = false, contactAnnounce = false))
        assertTrue(ContactControls.shouldAnnounce(false, false, isFav = false, contactAnnounce = true))
        // global on, sab
        assertTrue(ContactControls.shouldAnnounce(true, false, isFav = false, contactAnnounce = false))
        // global fav-only: fav announce, non-fav nahi
        assertTrue(ContactControls.shouldAnnounce(true, true, isFav = true, contactAnnounce = false))
        assertFalse(ContactControls.shouldAnnounce(true, true, isFav = false, contactAnnounce = false))
        // per-number pin non-fav ko bhi fav-only mode me announce karata hai
        assertTrue(ContactControls.shouldAnnounce(true, true, isFav = false, contactAnnounce = true))
    }
}
