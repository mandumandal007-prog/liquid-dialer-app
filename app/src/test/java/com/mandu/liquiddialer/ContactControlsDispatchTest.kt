package com.mandu.liquiddialer

import android.Manifest
import android.content.ComponentName
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.ContactControls
import com.mandu.liquiddialer.data.SimInfo
import com.mandu.liquiddialer.data.LdSettings
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContactControlsDispatchTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val number = "+91 98765 43210"
    private val remaining = PhoneAccountHandle(ComponentName("com.mandu", "A"), "remaining")
    private fun prepare() {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        shadowOf(rule.activity.application).grantPermissions(Manifest.permission.CALL_PHONE)
        shadowOf(rule.activity.getSystemService(TelecomManager::class.java)).setCallPhonePermission(true)
        LdApp.settings.value = LdSettings(undoCall = false, simMemory = false)
        ContactControls.setSimPin(rule.activity, number, "removed")
    }
    @After fun clear() {
        ApplicationProvider.getApplicationContext<LdApp>().getSharedPreferences("contact_controls", 0).edit().clear().apply()
    }
    @Test fun stalePinOneSimAsksWithoutPlacingCall() {
        prepare()
        rule.runOnUiThread {
            rule.activity.state.sims = listOf(SimInfo(remaining, "Remaining", 1))
            rule.activity.call(number)
        }
        assertEquals(number to false, rule.activity.state.pendingCall)
        assertTrue(shadowOf(rule.activity.getSystemService(TelecomManager::class.java)).allOutgoingCalls.isEmpty())
    }
    @Test fun stalePinNoSimNeverDispatches() {
        prepare()
        rule.runOnUiThread { rule.activity.state.sims = emptyList(); rule.activity.call(number) }
        assertNull(rule.activity.state.pendingCall)
        assertTrue(shadowOf(rule.activity.getSystemService(TelecomManager::class.java)).allOutgoingCalls.isEmpty())
    }
    @Test fun explicitCurrentCallChoiceBeatsStalePin() {
        prepare()
        rule.runOnUiThread {
            rule.activity.state.sims = listOf(SimInfo(remaining, "Remaining", 1))
            rule.activity.call(number, remaining)
        }
        val calls = shadowOf(rule.activity.getSystemService(TelecomManager::class.java)).allOutgoingCalls
        assertEquals(1, calls.size)
        assertEquals(remaining, calls.single().phoneAccount)
        assertNull(rule.activity.state.pendingCall)
    }
}
