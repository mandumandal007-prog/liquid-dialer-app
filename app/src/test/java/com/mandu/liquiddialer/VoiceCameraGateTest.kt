package com.mandu.liquiddialer

import android.telecom.Call
import android.telecom.VideoProfile
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.telecom.videoActiveOrPending
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** USER BUG regression: voice call pe camera indicator on hota tha, kyun ki har call par
 *  videoCall.setCamera() chala jata tha (MediaTek IMS audio call me bhi provider deta hai).
 *  Camera selection ka gate: sirf video call ya user-initiated video request. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceCameraGateTest {

    @After fun reset() { CallManager.pendingVideoRequest = false }

    @Test fun plainVoiceCallDoesNotArmCamera() {
        val c = FakeCall.make("+91 98765 43210", "Rahul", Call.STATE_ACTIVE, videoState = VideoProfile.STATE_AUDIO_ONLY)
        CallManager.pendingVideoRequest = false
        assertFalse("voice call par camera arm nahi hona chahiye", videoActiveOrPending(c))
    }

    @Test fun ringingVoiceCallDoesNotArmCamera() {
        val c = FakeCall.make("+91 98765 43210", "Rahul", Call.STATE_RINGING, videoState = VideoProfile.STATE_AUDIO_ONLY)
        assertFalse(videoActiveOrPending(c))
    }

    @Test fun videoCallArmsCamera() {
        val c = FakeCall.make("+91 98765 43210", "Rahul", Call.STATE_ACTIVE, videoState = VideoProfile.STATE_BIDIRECTIONAL)
        CallManager.pendingVideoRequest = false
        assertTrue(videoActiveOrPending(c))
    }

    @Test fun incomingVideoCallArmsCamera() {
        val c = FakeCall.make("+91 98765 43210", "Rahul", Call.STATE_RINGING, videoState = VideoProfile.STATE_BIDIRECTIONAL)
        assertTrue(videoActiveOrPending(c))
    }

    @Test fun userInitiatedVideoRequestArmsCamera() {
        val c = FakeCall.make("+91 98765 43210", "Rahul", Call.STATE_ACTIVE, videoState = VideoProfile.STATE_AUDIO_ONLY)
        CallManager.pendingVideoRequest = true   // place(video=true) / user ne video maanga
        assertTrue(videoActiveOrPending(c))
    }
}
