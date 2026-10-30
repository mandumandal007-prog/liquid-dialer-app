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

/** Video-stage aur upgrade-prompt ke layout pixels. Robolectric me TextureViews render nahi hote
 *  (SIMULATED placeholder boxes) - yeh sirf layout/positioning ka pramaan hai, asli video nahi. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VideoStageScreenshotTest {

    @get:Rule val rule = createAndroidComposeRule<CallActivity>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f
            p[K.gridScale] = 1f; p[K.bottomScale] = 1f; p[K.callCardScale] = 1f
        }
        CallManager.videoNotice.value = null
        CallManager.videoUpgradeFrom.value = null
        Unit
    }

    private fun videoCall() {
        CallManager.audio.value = CallAudioState(false, CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_ACTIVE,
            System.currentTimeMillis() - 30_000, videoState = android.telecom.VideoProfile.STATE_BIDIRECTIONAL))
        CallManager.bump()
    }

    @Test
    fun videoStageShowsDisplayAndPreviewSurfaces() {
        settle(2500)
        videoCall()
        settle(2500)
        rule.onNodeWithTag("video_display", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_preview", useUnmergedTree = true).assertExists()
        saveShot(rule, "09-video-stage")
    }

    @Test
    fun incomingUpgradePromptRenders() {
        settle(2500)
        videoCall()
        settle(2500)
        CallManager.videoUpgradeFrom.value = CallManager.primary()
        CallManager.bump()
        settle()
        rule.onNodeWithTag("video_accept", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_decline", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Video on karna chahta hai", useUnmergedTree = true).assertExists()
        saveShot(rule, "09b-video-upgrade-prompt")
    }
}
