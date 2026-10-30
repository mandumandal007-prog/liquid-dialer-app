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
    fun videoButtonHiddenWhenCallHasNoVideoSupport() {
        settle(2500)
        activeCall()
        settle(2500)
        // no cosmetic fake button: without real video support nothing is shown
        rule.onNodeWithTag("video_call", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("manage_contact", useUnmergedTree = true).assertExists()
    }

    @Test
    fun videoButtonHiddenWhenOnlyLocalSideSupportsVideo() {
        settle(2500)
        activeCall(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL)
        settle(2500)
        // local support alone must not promise a video upgrade the remote side cannot accept
        rule.onNodeWithTag("video_call", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun videoButtonShownWhenSupportedAndTapNeverCalls() {
        settle(2500)
        activeCall(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL or Call.Details.CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL)
        settle(2500)
        rule.onNodeWithTag("video_call", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("video_call", useUnmergedTree = true).performClick(); settle()
        // fake call has no video provider -> honest notice, no crash, and no call placed
        assertNotNull("user must be told video is unavailable", CallManager.videoNotice.value)
        val i = shadowOf(rule.activity.application).nextStartedActivity
        assertTrue("unexpected activity start: $i", i == null || i.action != Intent.ACTION_CALL)
    }

    @Test
    fun manageContactChipOpensEditorRouteWithoutTouchingContacts() {
        settle(2500)
        activeCall()
        settle(2500)
        rule.onNodeWithTag("manage_contact", useUnmergedTree = true).performClick(); settle()
        val i = shadowOf(rule.activity.application).nextStartedActivity
        assertNotNull("manage chip must route to MainActivity", i)
        assertEquals(MainActivity::class.java.name, i!!.component?.className)
        assertEquals("+91 98765 43210", i.getStringExtra("editContact"))
    }
}
