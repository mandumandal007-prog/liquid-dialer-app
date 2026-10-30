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

    /** Real-device defect (user screenshot, fontScale bada phone): hero overflow se timer/avatar clip.
     *  fontScale 1.25 pe: badge strip UPAR, timer controls ke UPAR poora visible, avatar chips ke neeche. */
    @Test
    fun ongoingCallLargeFontNoClip() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.25f)
        try {
            settle(2000)
            activeCall()
            settle(2500)

            // badge strip upar (HD VOICE + manage) aur timer se upar hona chahiye
            rule.onNodeWithTag("badge_strip").assertIsDisplayed()
            rule.onNodeWithText("HD VOICE").assertIsDisplayed()
            rule.onNodeWithTag("call_timer").assertIsDisplayed()
            val strip = rule.onNodeWithTag("badge_strip").fetchSemanticsNode().boundsInRoot
            val timer = rule.onNodeWithTag("call_timer").fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("badge strip timer ke upar honi chahiye", strip.bottom <= timer.top)

            // timer ka bottom controls (Mute) ke top ke upar - device pe timer button ke neeche clip ho raha tha
            val mute = rule.onNodeWithText("Mute").fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("timer controls ke upar poora dikhna chahiye (clipped nahi)", timer.bottom <= mute.top)

            // avatar top chips (SIM) ke neeche - device pe avatar chips ke neeche clip ho raha tha
            val avatar = rule.onNodeWithTag("hero_avatar").fetchSemanticsNode().boundsInRoot
            val sim = rule.onNodeWithText("SIM").fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("avatar top chips se overlap nahi hona chahiye", avatar.top >= sim.bottom)

            saveShot(rule, "30-ongoing-bigfont")
        } finally {
            org.robolectric.RuntimeEnvironment.setFontScale(1.0f)
        }
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

    /** Status dot correctness: dialing = dot (status "Calling..."), hold/ended = NO dot. */
    @Test
    fun statusDotStates() {
        // DIALING: timer "00:00" chalta hai -> static dot + "Calling..." (kabhi "Connected" nahi)
        settle(2000)
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_DIALING))
        CallManager.bump()
        settle(2000)
        rule.onAllNodesWithTag("status_dot").fetchSemanticsNodes().also {
            org.junit.Assert.assertTrue("dialing pe status dot dikhna chahiye", it.isNotEmpty())
        }
        rule.onAllNodesWithText("Connected").fetchSemanticsNodes().also {
            org.junit.Assert.assertTrue("dialing ko Connected label nahi hona chahiye", it.isEmpty())
        }
        saveShot(rule, "24a-dialing-dot")

        // HOLDING: timer null -> koi dot nahi
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_HOLDING,
            System.currentTimeMillis() - 75_000))
        CallManager.bump()
        settle(2000)
        rule.onAllNodesWithTag("status_dot").fetchSemanticsNodes().also {
            org.junit.Assert.assertTrue("hold pe status dot nahi hona chahiye", it.isEmpty())
        }
        saveShot(rule, "24b-holding-nodot")

        // DISCONNECTED: "Call ended" -> koi dot nahi
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_DISCONNECTED,
            System.currentTimeMillis() - 75_000))
        CallManager.bump()
        settle(1200)
        val endedDots = rule.onAllNodesWithTag("status_dot").fetchSemanticsNodes()
        org.junit.Assert.assertTrue("ended call pe status dot nahi hona chahiye", endedDots.isEmpty())
        saveShot(rule, "24c-ended-nodot")
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
