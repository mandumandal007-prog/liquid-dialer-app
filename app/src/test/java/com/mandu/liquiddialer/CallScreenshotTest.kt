package com.mandu.liquiddialer

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.telecom.CallManager
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallScreenshotTest {

    @get:Rule val rule = createAndroidComposeRule<CallActivity>()

    private fun seed(prefs: androidx.datastore.preferences.core.MutablePreferences.() -> Unit) = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true
            p[K.profileName] = "Mandu Mandal"
            p[K.theme] = "daylight"
            p[K.gridScale] = 1f; p[K.bottomScale] = 1f; p[K.callCardScale] = 1f; p[K.dialerScale] = 1f
            p[K.callBgStyle] = "blobs"; p[K.callAvatarShape] = "circle"; p[K.callShowNumber] = true
            p.prefs()
        }
        Unit
    }

    @Before
    fun seedCalls() {
        seed {}
        CallManager.audio.value = CallAudioState(false, CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_RINGING))
        CallManager.bump()
    }

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun activeCall() {
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_ACTIVE,
            System.currentTimeMillis() - 75_000))
        CallManager.bump()
    }

    @Test
    fun incomingCall() {
        settle(2500)
        saveShot(rule, "07-incoming")
    }

    @Test
    fun ongoingCall() {
        settle(2000)
        activeCall()
        settle(2500)
        saveShot(rule, "08-ongoing-call")
    }

    @Test
    fun ongoingCallVideoCapable() {
        seed { this[K.theme] = "midnight" }
        settle(2000)
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_ACTIVE,
            System.currentTimeMillis() - 75_000,
            caps = Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL or Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL))
        CallManager.bump()
        settle(2500)
        saveShot(rule, "08b-ongoing-video-dark")
    }

    @Test
    fun inCallKeypad() {
        settle(2000)
        activeCall()
        settle(2000)
        rule.onNodeWithText("Keypad").performClick()
        settle()
        saveShot(rule, "09-incall-keypad")
    }

    @Test
    fun ongoingConferenceManageSheet() {
        seed { this[K.theme] = "midnight" }
        settle(2000)
        val parent = FakeCall.make("+91 90000 00000", "Conference", Call.STATE_ACTIVE,
            System.currentTimeMillis() - 300_000, props = Call.Details.PROPERTY_CONFERENCE)
        val c1 = FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_ACTIVE, System.currentTimeMillis() - 280_000,
            caps = Call.Details.CAPABILITY_DISCONNECT_FROM_CONFERENCE or Call.Details.CAPABILITY_SEPARATE_FROM_CONFERENCE)
        val c2 = FakeCall.make("+91 91234 56789", "Papa", Call.STATE_ACTIVE, System.currentTimeMillis() - 200_000)
        FakeCall.setChildren(parent, listOf(c1, c2))
        CallManager.calls.value = listOf(parent, c1, c2)
        CallManager.bump()
        settle(2500)
        rule.onNodeWithTag("manage_conf", useUnmergedTree = true).performClick()
        settle()
        saveShot(rule, "08c-conference-manage-dark")
    }
}
