package com.mandu.liquiddialer

import android.telecom.Call
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.AppLock
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.telecom.CallManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Regression: app lock ON + locked session me CallActivity (incoming/ongoing/emergency call UI) gated NAHI. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallLockRegressionTest {

    @org.junit.After fun tearDownLockState() = kotlinx.coroutines.runBlocking {
        // JVM-shared sandbox: persisted lock state agle test classes ko gate mat dikhao
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = com.mandu.liquiddialer.data.SettingsRepo(ctx)
        repo.set(com.mandu.liquiddialer.data.K.appLock, false)
        repo.set(com.mandu.liquiddialer.data.K.appLockDelaySec, 0)
        com.mandu.liquiddialer.data.AppLock.resetForTests()
    }

    @get:Rule val rule = createAndroidComposeRule<CallActivity>()

    @After fun tearDown() { CallManager.calls.value = emptyList(); AppLock.resetForTests() }

    @Test
    fun callScreenNotGatedWhenAppLockOn() {
        // Persisted: lock ON. Session: locked (unlock kabhi nahi hua = cold start state)
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit {
                it[K.onboarded] = true
                it[K.appLock] = true
            }
        }
        AppLock.resetForTests()
        // Ongoing call inject karo
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_ACTIVE, System.currentTimeMillis() - 75_000))
        CallManager.bump()
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(2000)
        rule.waitForIdle()
        // Gate ka koi hissa call screen pe nahi
        assertTrue(rule.onAllNodesWithText("Unlock karo", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithText("Liquid Dialer lock", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        // Call UI poora dikhta hai
        rule.onNodeWithText("Rahul Sharma", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }
}
