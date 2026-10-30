package com.mandu.liquiddialer

import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.DialShortcuts
import com.mandu.liquiddialer.data.Repo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Home screen direct-dial shortcut: intent CALLBACK + number, stable id, naam fallback.
 *  Pin user ke explicit tap se hi hota hai; koi auto-shortcut nahi. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialShortcutsTest {

    @Test
    fun shortcutInfoCarriesCallbackIntent() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val i = DialShortcuts.info(ctx, "+91 98123 45678", "Rahul")
        assertEquals("Rahul", i.shortLabel.toString())
        assertEquals("dial_" + Repo.key("+91 98123 45678"), i.id)
        assertEquals(DialShortcuts.CALLBACK_ACTION, i.intent.action)
        assertEquals("+91 98123 45678", i.intent.getStringExtra("number"))
        // same number = same id: dobara pin karne par duplicate nahi, replace hota hai
        assertEquals(i.id, DialShortcuts.info(ctx, "9812345678", "Rahul").id)
    }

    @Test
    fun displayNameFallsBackToPrettyNumber() {
        assertEquals("Rahul", DialShortcuts.displayName("9812345678", "Rahul"))
        assertEquals(Repo.pretty("9812345678"), DialShortcuts.displayName("9812345678", ""))
    }

    @Test
    fun pinRejectsBlankNumber() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        assertFalse(DialShortcuts.pin(ctx, "", "Rahul"))
    }
}
