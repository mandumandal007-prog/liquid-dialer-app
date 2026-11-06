package com.mandu.liquiddialer

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.ui.CALL_STYLES
import com.mandu.liquiddialer.ui.LdThemeHost
import com.mandu.liquiddialer.ui.StyleStudioPage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Style Studio page + styled calling card ke pixel shots.
 *  Day + night-large dono me: sab style rows render hoti hain, preview dikhta hai,
 *  aur har card style me end_call button screen ke andar rehta hai. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleStudioShotTest {

    @get:Rule val rule = createAndroidComposeRule<CallActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @After fun reset() { CallManager.calls.value = emptyList(); CallManager.bump() }

    private fun seed(theme: String, font: Float, cardStyle: String, recentsStyle: String) = runBlocking {
        ctx.ldStore.edit {
            it.clear()
            it[K.onboarded] = true; it[K.theme] = theme; it[K.fontScale] = font
            it[K.callCardStyle] = cardStyle; it[K.recentsStyle] = recentsStyle
        }
        rule.waitUntil(15000) {
            val v = LdApp.settings.value
            v.theme == theme && v.fontScale == font && v.callCardStyle == cardStyle && v.recentsStyle == recentsStyle
        }
        Unit
    }

    private fun activeCall() {
        CallManager.audio.value = CallAudioState(false, CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", Call.STATE_ACTIVE,
            System.currentTimeMillis() - 75_000))
        CallManager.bump()
    }

    @Test fun studioDayGoogleSelected() {
        seed("daylight", 1f, "google", "google")
        rule.runOnUiThread { rule.activity.setContent { LdThemeHost { StyleStudioPage({}) } } }
        settle()
        rule.waitUntil(15000) { rule.onAllNodesWithTag("card_style_preview", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("recents_style_preview", useUnmergedTree = true).assertIsDisplayed()
        saveShot(rule, "292-stylestudio-day")
    }

    @Test fun studioNightLargeAmoledSelected() {
        seed("midnight", 1.3f, "amoled", "amoled")
        rule.runOnUiThread { rule.activity.setContent { LdThemeHost { StyleStudioPage({}) } } }
        settle()
        rule.waitUntil(15000) { rule.onAllNodesWithTag("card_style_preview", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("card_style_amoled", useUnmergedTree = true).assertExists()
        saveShot(rule, "292-stylestudio-night-large")
    }

    /** Har card style me poora call screen render ho, end_call andar rahe. */
    @Test fun everyCardStyleRendersEndCallInsideScreen() {
        seed("midnight", 1f, "glass", "glass")
        val rootW = rule.activity.resources.displayMetrics.widthPixels.toFloat()
        val rootH = rule.activity.resources.displayMetrics.heightPixels.toFloat()
        CALL_STYLES.forEach { st ->
            runBlocking { ctx.ldStore.edit { it[K.callCardStyle] = st.key } }
            rule.waitUntil(15000) { LdApp.settings.value.callCardStyle == st.key }
            activeCall()
            rule.runOnUiThread { rule.activity.setContent { LdThemeHost { com.mandu.liquiddialer.ui.CallScreen({}, {}, {}, {}) } } }
            settle(1200)
            rule.waitUntil(15000) { rule.onAllNodesWithTag("end_call", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            val b = rule.onNodeWithTag("end_call", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue(st.key + ": end_call left >= 0 (tha ${b.left})", b.left >= -1f)
            assertTrue(st.key + ": end_call right <= w (tha ${b.right} / $rootW)", b.right <= rootW + 1f)
            assertTrue(st.key + ": end_call bottom <= h (tha ${b.bottom} / $rootH)", b.bottom <= rootH + 1f)
        }
    }
}
