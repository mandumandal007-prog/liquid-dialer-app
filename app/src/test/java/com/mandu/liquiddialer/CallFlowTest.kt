package com.mandu.liquiddialer

import android.content.Intent
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
import org.junit.Assert.*
import org.junit.Before
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
class CallFlowTest {

    @get:Rule val rule = createAndroidComposeRule<CallActivity>()

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f
            p[K.gridScale] = 1f; p[K.bottomScale] = 1f; p[K.callCardScale] = 1f
        }
        CallManager.videoNotice.value = null
        Unit
    }

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun activeCall(caps: Int = 0) {
        CallManager.audio.value = CallAudioState(false, CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_ACTIVE,
            System.currentTimeMillis() - 75_000, caps = caps))
        CallManager.bump()
    }

    @Test
    fun videoEntryShownWithHonestReasonWhenNoVideoSupport() {
        settle(2500)
        activeCall()
        settle(2500)
        // entry gayab nahi hoti: tap par sheet me imaandari wajah + Meet raasta (koi cosmetic fake button nahi)
        rule.onNodeWithTag("video_call", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_call", useUnmergedTree = true).performClick(); settle()
        rule.onNodeWithText("Is SIM/network ne video call (ViLTE) support nahi bataaya", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_upgrade_go", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("meet_launch", useUnmergedTree = true).assertExists()
        // normal call: conference-manage chip nahi dikhta
        rule.onNodeWithTag("manage_conf", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun videoEntryShowsRemoteReasonWhenOnlyLocalSideSupportsVideo() {
        settle(2500)
        activeCall(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL)
        settle(2500)
        // local support alone must not promise a video upgrade the remote side cannot accept:
        // sheet khulti hai par reason saaf REMOTE side ko naam deta hai, upgrade button nahi
        rule.onNodeWithTag("video_call", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_call", useUnmergedTree = true).performClick(); settle()
        rule.onNodeWithText("Saamne wale ka phone/network video call support nahi bata raha", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_upgrade_go", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun videoButtonShownWhenSupportedAndTapNeverCalls() {
        settle(2500)
        activeCall(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL or Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL)
        settle(2500)
        rule.onNodeWithTag("video_call", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_call", useUnmergedTree = true).performClick(); settle()
        // sheet pehle: support batati hai, user khud upgrade dabata hai
        rule.onNodeWithText("Ye call SIM video (ViLTE) support bata rahi hai", substring = true, useUnmergedTree = true).assertExists()
        // camera permission granted maan kar upgrade dabao (Robolectric default me denied hoti hai)
        shadowOf(rule.activity.application).grantPermissions(android.Manifest.permission.CAMERA)
        rule.onNodeWithTag("video_upgrade_go", useUnmergedTree = true).assertExists().performClick(); settle()
        // fake call has no video provider -> honest notice, no crash, and no call placed
        assertNotNull("user must be told video is unavailable", CallManager.videoNotice.value)
        val i = shadowOf(rule.activity.application).nextStartedActivity
        assertTrue("unexpected activity start: $i", i == null || i.action != Intent.ACTION_CALL)
    }

    private fun conferenceCall(childCaps: Int) {
        CallManager.audio.value = CallAudioState(false, CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        val parent = FakeCall.make("+91 90000 00000", "Conference", Call.STATE_ACTIVE,
            System.currentTimeMillis() - 120_000, props = Call.Details.PROPERTY_CONFERENCE)
        val c1 = FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_ACTIVE, System.currentTimeMillis() - 100_000, caps = childCaps)
        val c2 = FakeCall.make("+91 91234 56789", "Papa", Call.STATE_ACTIVE, System.currentTimeMillis() - 90_000)
        FakeCall.setChildren(parent, listOf(c1, c2))
        CallManager.calls.value = listOf(parent, c1, c2)
        CallManager.bump()
    }

    @Test
    fun manageChipShownOnlyOnConference() {
        settle(2500)
        activeCall()
        settle(2500)
        rule.onNodeWithTag("manage_conf", useUnmergedTree = true).assertDoesNotExist()
        conferenceCall(0)
        settle(2500)
        rule.onNodeWithTag("manage_conf", useUnmergedTree = true).assertExists()
    }

    @Test
    fun conferenceManageListsChildrenWithCapabilityGatedControls() {
        settle(2500)
        conferenceCall(Call.Details.CAPABILITY_DISCONNECT_FROM_CONFERENCE)
        settle(2500)
        rule.onNodeWithTag("manage_conf", useUnmergedTree = true).performClick(); settle()
        // dono participants dikhte hain
        rule.onNodeWithText("Rahul Sharma", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Papa", useUnmergedTree = true).assertExists()
        // capable child: cut enabled; tap sirf us child ko disconnect karta hai, poori conference nahi
        rule.onNodeWithTag("conf_cut_0", useUnmergedTree = true).assertExists().performClick(); settle()
        assertEquals(Call.STATE_ACTIVE, CallManager.state(CallManager.calls.value.first()))
        // incapable child: honest disabled control + hint, koi fake action nahi
        rule.onNodeWithTag("conf_cut_1", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Gray option is call par SIM/phone support nahi kar raha.", substring = true, useUnmergedTree = true).assertExists()
    }
    @Test
    fun videoExtrasCarryBidirectionalStateOnlyForVideo() {
        val v = CallManager.buildPlaceExtras(null, true)
        org.junit.Assert.assertEquals(android.telecom.VideoProfile.STATE_BIDIRECTIONAL,
            v.getInt(android.telecom.TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, -1))
        val a = CallManager.buildPlaceExtras(null, false)
        org.junit.Assert.assertFalse(a.containsKey(android.telecom.TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE))
    }

    @Test
    fun videoUpgradeWithoutProviderIsHonest() {
        settle(2500)
        activeCall(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL or Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL)
        settle(2500)
        CallManager.videoNotice.value = null
        val ok = CallManager.requestVideo(CallManager.primary())
        org.junit.Assert.assertFalse("fake call has no video provider -> request must fail honestly", ok)
        org.junit.Assert.assertNotNull(CallManager.videoNotice.value)
    }

    @Test
    fun incomingVideoUpgradePromptNeedsUserChoice() {
        settle(2500)
        activeCall()
        settle(2500)
        val c = CallManager.primary()!!
        CallManager.videoUpgradeFrom.value = c
        CallManager.bump(); settle()
        rule.onNodeWithTag("video_accept", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_decline", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_decline", useUnmergedTree = true).performClick(); settle()
        org.junit.Assert.assertNull(CallManager.videoUpgradeFrom.value)
    }

    @Test
    fun videoLinkLateProviderGetsCurrentSurfaces() {
        val link = com.mandu.liquiddialer.telecom.VideoLink
        link.unbind()
        val fakeSurface = android.view.Surface(android.graphics.SurfaceTexture(0))
        // surface pehle bana, provider baad me aaya -> bind par turant attach hona chahiye
        link.setDisplay(fakeSurface)
        var got: Pair<android.view.Surface?, android.view.Surface?>? = null
        link.bind { d, p -> got = d to p }
        org.junit.Assert.assertSame("late provider must receive the already-created display surface", fakeSurface, got?.first)
        // provider replace: naye provider ko bhi current surface milna chahiye
        var got2: Pair<android.view.Surface?, android.view.Surface?>? = null
        link.bind { d, p -> got2 = d to p }
        org.junit.Assert.assertSame(fakeSurface, got2?.first)
        // teardown: unbind par provider ko null push + refs clear
        var cleared: Pair<android.view.Surface?, android.view.Surface?>? = null
        link.bind { d, p -> cleared = d to p }
        link.unbind()
        org.junit.Assert.assertNull(cleared?.first)
        org.junit.Assert.assertNull(link.display)
        fakeSurface.release()
    }

    @Test
    fun cameraPermissionDeclaredForVideoPreview() {
        // AOSP Dialer ka manifest CAMERA declare karta hai (android.googlesource.com/packages/apps/Dialer
        // AndroidManifest.xml se verify kiya gaya): video preview surface ke liye dialer ke paas permission
        // honi chahiye. runtime par video sheet se maanga jata hai.
        val pm = ApplicationProvider.getApplicationContext<LdApp>().packageManager
        val info = pm.getPackageInfo(ApplicationProvider.getApplicationContext<LdApp>().packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS)
        org.junit.Assert.assertTrue("CAMERA must be declared like AOSP Dialer for the video preview",
            info.requestedPermissions?.contains(android.Manifest.permission.CAMERA) == true)
    }

    @Test
    fun answerKeepsIncomingVideoState() {
        val vc = FakeCall.make("+91 98765 43210", "Rahul", Call.STATE_RINGING, videoState = android.telecom.VideoProfile.STATE_BIDIRECTIONAL)
        org.junit.Assert.assertEquals(android.telecom.VideoProfile.STATE_BIDIRECTIONAL, CallManager.answerVideoState(vc))
        val ac = FakeCall.make("+91 98765 43210", "Rahul", Call.STATE_RINGING)
        org.junit.Assert.assertEquals(android.telecom.VideoProfile.STATE_AUDIO_ONLY, CallManager.answerVideoState(ac))
    }
}
