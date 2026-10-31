package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.AppLock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** App lock gate policy: kab lock, kab nahi. Session state disk pe kabhi nahi. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLockTest {

    @After fun tearDown() = AppLock.resetForTests()

    @Test
    fun lockMatrix() {
        // disabled = kabhi lock nahi
        assertFalse(AppLock.shouldLock(enabled = false, unlocked = false, lastBgAt = 0, now = 100_000, delaySec = 0))
        assertFalse(AppLock.shouldLock(false, true, 50_000, 100_000, 0))
        // enabled + kabhi unlock nahi hua (cold start / process death) = lock
        assertTrue(AppLock.shouldLock(true, unlocked = false, lastBgAt = 0, now = 100_000, delaySec = 0))
        // enabled + unlocked + background hua hi nahi = lock nahi
        assertFalse(AppLock.shouldLock(true, true, lastBgAt = 0, now = 100_000, delaySec = 0))
        // turant (0): koi bhi real background = lock
        assertTrue(AppLock.shouldLock(true, true, lastBgAt = 95_000, now = 100_000, delaySec = 0))
        // 30 sec: andar = khula, paar = lock
        assertFalse(AppLock.shouldLock(true, true, lastBgAt = 90_000, now = 100_000, delaySec = 30))
        assertTrue(AppLock.shouldLock(true, true, lastBgAt = 69_000, now = 100_000, delaySec = 30))
        // 1 min boundary
        assertFalse(AppLock.shouldLock(true, true, lastBgAt = 41_000, now = 100_000, delaySec = 60))
        assertTrue(AppLock.shouldLock(true, true, lastBgAt = 39_000, now = 100_000, delaySec = 60))
    }

    @Test
    fun settingsMirrorRoundTrip() = kotlinx.coroutines.runBlocking {
        // Cold-start mirror: set() ke saath sync likha, lockMirror() bina DataStore ke wapas deta hai
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = com.mandu.liquiddialer.data.SettingsRepo(ctx)
        assertFalse(repo.lockMirror().first)          // default OFF
        repo.set(com.mandu.liquiddialer.data.K.appLock, true)
        repo.set(com.mandu.liquiddialer.data.K.appLockDelaySec, 60)
        assertEquals(true to 60, repo.lockMirror())
        repo.set(com.mandu.liquiddialer.data.K.appLock, false)
        repo.set(com.mandu.liquiddialer.data.K.appLockDelaySec, 0)
        assertEquals(false to 0, repo.lockMirror())
    }

    @Test
    fun credentialResultHandling() {
        assertFalse(AppLock.handleCredentialResult(9999, android.app.Activity.RESULT_OK))
        var got: Boolean? = null
        // pending callback ko realAuth ke bina seed karne ke liye authenticator seam se nahi - direct field test
        // CRED_REQ result: OK = true, CANCELED = false, ek hi baar consume hota hai
        val f = AppLock::class.java.getDeclaredField("pendingCredCallback").apply { isAccessible = true }
        f.set(AppLock) { ok: Boolean -> got = ok }
        assertTrue(AppLock.handleCredentialResult(AppLock.CRED_REQ, android.app.Activity.RESULT_OK))
        assertEquals(true, got)
        // dobara call = koi pending nahi, sirf true return (consume ho chuka)
        got = null
        assertTrue(AppLock.handleCredentialResult(AppLock.CRED_REQ, android.app.Activity.RESULT_CANCELED))
        assertEquals(null, got)
    }

    @Test
    fun onUnlockedClearsBackgroundMarker() {
        AppLock.lastBackgroundAt = 50_000
        AppLock.onUnlocked()
        assertTrue(AppLock.unlockedThisSession)
        assertEquals(0L, AppLock.lastBackgroundAt)
    }
}
