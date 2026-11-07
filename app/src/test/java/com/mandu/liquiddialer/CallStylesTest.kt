package com.mandu.liquiddialer

import com.mandu.liquiddialer.ui.BtnKind
import com.mandu.liquiddialer.ui.CALL_STYLES
import com.mandu.liquiddialer.ui.RECENTS_STYLES
import com.mandu.liquiddialer.ui.callStyleFor
import com.mandu.liquiddialer.ui.paletteFor
import com.mandu.liquiddialer.ui.recentsStyleFor
import org.junit.Assert.*
import org.junit.Test

/** Style presets ka structure: 11 calling-card + 11 recents, glass default, fallback safe. */
class CallStylesTest {

    @Test fun seventeenCardStylesUniqueKeysGlassFirst() {
        assertEquals(17, CALL_STYLES.size)
        assertEquals(CALL_STYLES.size, CALL_STYLES.map { it.key }.toSet().size)
        assertEquals("glass", CALL_STYLES[0].key)
        assertTrue(CALL_STYLES.any { it.key == "google" })
        assertTrue(CALL_STYLES.any { it.key == "odialer" })
        assertTrue(CALL_STYLES.any { it.key == "oneplus" })
    }

    @Test fun elevenRecentsStylesUniqueKeysGlassFirst() {
        assertEquals(11, RECENTS_STYLES.size)
        assertEquals(RECENTS_STYLES.size, RECENTS_STYLES.map { it.key }.toSet().size)
        assertEquals("glass", RECENTS_STYLES[0].key)
    }

    @Test fun existingCardAndRecentsKeysRemainAvailable() {
        assertTrue(CALL_STYLES.map { it.key }.containsAll(RECENTS_STYLES.map { it.key }))
        assertEquals(6, com.mandu.liquiddialer.ui.REFERENCE_CALL_KEYS.size)
    }

    @Test fun unknownKeyFallsBackToGlass() {
        assertEquals("glass", callStyleFor("kuch-bhi").key)
        assertEquals("glass", callStyleFor("").key)
        assertEquals("glass", recentsStyleFor("kuch-bhi").key)
        assertEquals("glass", recentsStyleFor("").key)
    }

    @Test fun everyKeyResolves() {
        CALL_STYLES.forEach { assertSame(it, callStyleFor(it.key)) }
        RECENTS_STYLES.forEach { assertSame(it, recentsStyleFor(it.key)) }
    }

    @Test fun forcedBgStylesStayReadableOnLightTheme() {
        // jo style apna dark background force karta hai, uska palette text light hona chahiye
        // warna daylight theme pe dark-text-on-dark-bg ho jata
        val day = paletteFor("daylight", androidx.compose.ui.graphics.Color(0xFF5B8CFF), false)
        CALL_STYLES.filter { it.bgMode == "force" }.forEach { st ->
            val sp = st.restyle(day)
            assertFalse(st.key + " force-bg par isLight true nahi rah sakta", sp.isLight)
        }
    }

    @Test fun glassStyleIsIdentity() {
        val mid = paletteFor("midnight", androidx.compose.ui.graphics.Color(0xFF5B8CFF), true)
        val glass = callStyleFor("glass")
        assertSame(mid, glass.restyle(mid))
        assertEquals("keep", glass.bgMode)
        assertEquals(BtnKind.GLASS, glass.btnKind)
    }
}
