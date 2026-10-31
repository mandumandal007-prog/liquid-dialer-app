package com.mandu.liquiddialer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.data.simUseMapOf
import com.mandu.liquiddialer.data.toSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Call-dispatch fix: SIM memory ab settings flow ke saath MEMORY me aati hai.
 * Dispatch (call tap) pe DataStore ka disk read / runBlocking nahi - woh startup pe ho chuka.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SimDispatchTest {

    @Test
    fun simMemoryFlowKeSaathMemoryMe() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.ldStore.edit { it[K.simUse] = "9000000001=simA;9000000002=simB" }
        val s = ctx.ldStore.data.first().toSettings()
        // dispatch yahi map padhta hai: in-memory lookup, koi disk read nahi
        assertEquals("simA", s.simUse[Repo.key("+91 90000 00001")])
        assertEquals("simB", s.simUse[Repo.key("+91 90000 00002")])
        assertNull(s.simUse["9999999999"])
    }

    @Test
    fun simUseMapParsing() {
        val m = simUseMapOf(preferencesOf(K.simUse to "a=1;;badentry;=x;b=2;c=d=e"))
        assertEquals(mapOf("a" to "1", "b" to "2", "c" to "d=e"), m)
        assertEquals(emptyMap<String, String>(), simUseMapOf(preferencesOf(K.simUse to "")))
        assertEquals(emptyMap<String, String>(), simUseMapOf(preferencesOf()))
    }

    @Test
    fun staleHandlePickerPeGirtaHai() {
        // remembered/default handle cached list me NAHI -> chooseSim null (picker), kabhi galat SIM nahi
        val sims = listOf("simX", "simY")
        assertEquals(null, CallPrep.chooseSim(null, "simGaya", null, sims) { it })
        assertEquals(null, CallPrep.chooseSim(null, null, "simGaya", sims) { it })
        assertEquals("simX", CallPrep.chooseSim(null, "simX", null, sims) { it })
        assertEquals("simY", CallPrep.chooseSim(null, null, "simY", sims) { it })
        assertEquals("simX", CallPrep.chooseSim(null, "simGaya", null, listOf("simX")) { it }) // single SIM auto
    }
}
