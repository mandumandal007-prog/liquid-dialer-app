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

/** Round D pixel evidence: incoming spam/verification badges (140 telemarketer, 160 verified, intl warning). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpamBadgeShotTest {

    @get:Rule val rule = createAndroidComposeRule<CallActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    @Before
    fun seed() = runBlocking {
        CallManager.calls.value = emptyList(); CallManager.bump()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu Mandal"; p[K.theme] = "daylight"
            p[K.gridScale] = 1f; p[K.bottomScale] = 1f; p[K.callCardScale] = 1f; p[K.dialerScale] = 1f
            p[K.callBgStyle] = "blobs"; p[K.callAvatarShape] = "circle"; p[K.callShowNumber] = true
        }
        CallManager.videoNotice.value = null
        CallManager.audio.value = CallAudioState(false, CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        Unit
    }

    private fun waitBadges() {
        rule.mainClock.autoAdvance = false
        val t0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - t0 < 6000) {
            rule.mainClock.advanceTimeBy(200); rule.waitForIdle()
            if (rule.onAllNodesWithTag("spam_badges").fetchSemanticsNodes().isNotEmpty()) return
            Thread.sleep(80)
        }
        throw AssertionError("spam_badges kabhi nahi dikha")
    }

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun show(number: String) {
        CallManager.calls.value = listOf(FakeCall.make(number, "", Call.STATE_RINGING))
        CallManager.bump()
    }

    @Test fun incomingTelemarketer140() {
        settle(2500); show("1409876543"); waitBadges()
        saveShot(rule, "D-spam-140-day")
    }

    @Test fun incomingVerified160() {
        settle(2500); show("1609876543"); waitBadges()
        saveShot(rule, "D-spam-160-day")
    }

    @Test fun incomingInternationalWarn() {
        settle(2500); show("+44 20 7946 0000"); waitBadges()
        saveShot(rule, "D-spam-intl-day")
    }

    @Test fun incomingTelemarketerLargeFont() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        try {
            settle(2500); show("1409876543"); waitBadges()
            saveShot(rule, "D-spam-140-largefont")
        } finally { org.robolectric.RuntimeEnvironment.setFontScale(1.0f) }
    }
}
