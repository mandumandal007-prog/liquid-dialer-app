package com.mandu.liquiddialer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
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
class ScreenshotTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 1200) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true
            p[K.profileName] = "Mandu Mandal"
            p[K.profileNumber] = "+91 83888 80466"
            p[K.theme] = "daylight"
            p[K.dialerScale] = 1f
        }
        Unit
    }

    @Test
    fun dialerSheet() {
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.contacts = listOf(
            Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null),
            Contact(12, "lk2", "Papa", "", "+91 91234 56789", "MOBILE", false, null),
        )
        act.state.recents = listOf(
            Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null),
        )
        act.state.isDefault = true
        org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        act.state.recents = act.state.recents + Recent(2, "+91 91234 56789", "Papa", 2, System.currentTimeMillis() - 5 * 3600_000, 0, null, null)
        settle()
        saveShot(rule, "02-recents")
        act.state.recents = act.state.recents + (3..9).map { Recent(it.toLong(), "+91 90000 0000$it", "Zed $it", 1, System.currentTimeMillis() - it * 86_400_000L, 30, null, null) }
        settle()
        repeat(4) { rule.onRoot().performTouchInput { swipeUp(startY = height * 0.8f, endY = height * 0.2f) }; settle(400) }
        settle()
        saveShot(rule, "02b-recents-end")

        // open the dialer sheet via the FAB
        rule.onNodeWithTag("dialer_fab").performClick()
        settle()
        act.state.dial = "9876543210"
        settle()
        saveShot(rule, "03-dialer")

        act.state.dial = "9876"
        settle()
        saveShot(rule, "03d-chips")
        act.state.dial = "9876543210"
        settle()
        // open the three-dot menu (Paste / Pause / Wait)
        rule.onNodeWithTag("dial_menu").performClick()
        settle()
        saveShot(rule, "03b-dialer-menu")

        // add a pause -> number shows trailing comma
        rule.onNodeWithText("Pause add karo (,)").performClick()
        settle()
        saveShot(rule, "03c-dialer-pause")
    }
}
