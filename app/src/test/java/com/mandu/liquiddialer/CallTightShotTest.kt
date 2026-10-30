package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.data.K
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Worst case: chhoti height (640dp) + fontScale 1.25. Realme-style tight screen pe
 *  timer/avatar kabhi clip nahi hona chahiye (user device defect regression). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi")
class CallTightShotTest {

    @get:Rule val rule = createAndroidComposeRule<CallActivity>()

    @Before
    fun seedCalls() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.theme] = "daylight"
            p[K.gridScale] = 1f; p[K.bottomScale] = 1f; p[K.callCardScale] = 1f
            p[K.callShowNumber] = true
        }
        CallManager.audio.value = CallAudioState(false, CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_ACTIVE,
            System.currentTimeMillis() - 75_000))
        CallManager.bump()
        Unit
    }

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Test
    fun tightHeightBigFontNoClip() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.25f)
        try {
            settle(2500)
            rule.onNodeWithTag("badge_strip").assertIsDisplayed()
            rule.onNodeWithTag("call_timer").assertIsDisplayed()
            val strip = rule.onNodeWithTag("badge_strip").fetchSemanticsNode().boundsInRoot
            val heroBox = rule.onNodeWithTag("hero_box").fetchSemanticsNode().boundsInRoot
            val timer = rule.onNodeWithTag("call_timer").fetchSemanticsNode().boundsInRoot
            val avatar = rule.onNodeWithTag("hero_avatar").fetchSemanticsNode().boundsInRoot
            // asli defect hero_box ke bottom edge pe PIXEL clip hota hai (scroll container), isliye
            // timer/avatar ke layout bounds hero_box ke ANDAR hone chahiye - sirf controls se compare kaafi nahi
            org.junit.Assert.assertTrue("strip hero ke upar", strip.bottom <= heroBox.top + 2)
            org.junit.Assert.assertTrue("timer hero box ke andar poora (top) ${timer} vs ${heroBox}", timer.top >= heroBox.top)
            org.junit.Assert.assertTrue("timer hero box ke andar poora (bottom - pixel clip guard) ${timer} vs ${heroBox}", timer.bottom <= heroBox.bottom + 1)
            org.junit.Assert.assertTrue("avatar hero box ke andar poora ${avatar} vs ${heroBox}",
                avatar.top >= heroBox.top && avatar.bottom <= heroBox.bottom + 1)
            saveShot(rule, "34-ongoing-tight-bigfont")
        } finally {
            org.robolectric.RuntimeEnvironment.setFontScale(1.0f)
        }
    }
}
