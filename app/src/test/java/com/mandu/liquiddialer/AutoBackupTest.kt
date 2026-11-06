package com.mandu.liquiddialer

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.AutoBackup
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Auto-backup core: due logic, file likhna/rotation, maybe ka on/off + stamping. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoBackupTest {

    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    @Before fun clean() {
        runBlocking { ctx.ldStore.edit { it.clear() } }
        AutoBackup.dir(ctx).deleteRecursively()
    }

    @Test fun dueBoundaries() {
        assertTrue(AutoBackup.due(1000L, 0L))                                   // kabhi try nahi hua
        assertFalse(AutoBackup.due(AutoBackup.GAP_MS - 1, 1L))                  // abhi 7 din se kam
        assertTrue(AutoBackup.due(1L + AutoBackup.GAP_MS, 1L))                  // theek 7 din baad
    }

    @Test fun runNowWritesValidBackupFile() {
        val f = AutoBackup.runNow(ctx, 1730000000000L)
        assertTrue(f.exists())
        assertTrue(f.name.startsWith("auto-"))
        val o = JSONObject(f.readText())
        assertEquals("liquid-dialer-full-backup", o.getString("kind"))
        assertEquals(1, AutoBackup.files(ctx).size)
    }

    @Test fun runNowKeepsOnlyLastThree() {
        val stamps = (1..5).map { AutoBackup.runNow(ctx, 1730000000000L + it * 60000L).name }
        val kept = AutoBackup.files(ctx)
        assertEquals(AutoBackup.KEEP, kept.size)
        assertEquals(stamps[4], kept[0].name)
        assertEquals(stamps[3], kept[1].name)
        assertEquals(stamps[2], kept[2].name)
    }

    @Test fun maybeSkipsWhenDisabled() = runBlocking {
        assertFalse(AutoBackup.maybe(ctx, 10_000_000_000L))
        assertTrue(AutoBackup.files(ctx).isEmpty())
    }

    @Test fun maybeSkipsWhenLastTryRecent() = runBlocking {
        val now = 10_000_000_000L
        ctx.ldStore.edit { it[K.autoBackup] = true; it[K.lastAutoBackupTry] = now }
        assertFalse(AutoBackup.maybe(ctx, now + 60_000L))
        assertTrue(AutoBackup.files(ctx).isEmpty())
    }

    @Test fun maybeRunsWhenDueStampsAndThenSkips() = runBlocking {
        val now = 10_000_000_000L
        ctx.ldStore.edit { it[K.autoBackup] = true }
        assertTrue(AutoBackup.maybe(ctx, now))
        assertEquals(1, AutoBackup.files(ctx).size)
        assertEquals(now, LdApp.instance.repo.flow.first().lastAutoBackupAt)
        assertEquals(now, LdApp.instance.repo.flow.first().lastAutoBackupTry)
        // usi din dobara app khule: skip
        assertFalse(AutoBackup.maybe(ctx, now + 60_000L))
        assertEquals(1, AutoBackup.files(ctx).size)
    }
}
