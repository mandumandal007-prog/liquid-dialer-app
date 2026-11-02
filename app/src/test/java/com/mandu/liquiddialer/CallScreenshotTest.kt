package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
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

    @Test fun paintedCallControlsStayCircularWhenInactiveAndActive() {
        for(active in listOf(false,true)) {
            rule.activity.setContent {
                com.mandu.liquiddialer.ui.LdThemeHost {
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF284A6C))) {
                        com.mandu.liquiddialer.ui.CallBtn(com.mandu.liquiddialer.ui.LdIcons.Mic,"Shape",on=active,size=100.dp,onClick={})
                    }
                }
            }
            settle(1200)
            saveShot(rule,"G262-circle-"+active)
            val bmp=android.graphics.BitmapFactory.decodeFile((System.getenv("SHOT_DIR") ?: "build/shots")+"/G262-circle-"+active+".png")
            val label=rule.onNodeWithText("Shape").fetchSemanticsNode().boundsInRoot
            // The disk is 100dp across, centered directly above its label.
            val density=rule.activity.resources.displayMetrics.density
            val r=50f*density;val cx=label.center.x;val cy=r
            fun at(dx:Float,dy:Float)=bmp.getPixel((cx+dx*r).toInt(),(cy+dy*r).toInt())
            val bg=0xff284a6c.toInt()
            org.junit.Assert.assertEquals("outside circular corner stays transparent",bg,at(.92f,.92f))
            org.junit.Assert.assertNotEquals("near top-right arc must remain filled, not an inner octagon",bg,at(.63f,-.63f))
            org.junit.Assert.assertNotEquals("inside disc must be glass",bg,at(.65f,0f))
            bmp.recycle()
        }
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
        rule.onNodeWithTag("end_call").assertIsDisplayed()
        val endBounds = rule.onNodeWithTag("end_call").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("Ongoing hangup stays a wide pill", endBounds.width > endBounds.height * 1.5f)
        saveShot(rule, "08-ongoing-call")
    }

    @Test fun noPhotoLongNameBothThemes() {
        for(theme in listOf("daylight","midnight")) {
            seed { this[K.theme]=theme;this[K.callBgStyle]="blobs" }
            rule.waitUntil(5000) { LdApp.settings.value.theme==theme }
            CallManager.calls.value=listOf(FakeCall.make("+919876543210","Rahul Sharma Chattopadhyay",Call.STATE_ACTIVE,System.currentTimeMillis()-75000));CallManager.bump();settle(2500)
            rule.onNodeWithTag("call_name").assertIsDisplayed();rule.onNodeWithTag("call_timer").assertIsDisplayed();rule.onNodeWithTag("end_call").assertIsDisplayed()
            saveShot(rule,"G256-no-photo-"+theme)
        }
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
