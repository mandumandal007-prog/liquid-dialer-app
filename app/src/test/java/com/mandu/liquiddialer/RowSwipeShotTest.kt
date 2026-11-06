package com.mandu.liquiddialer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Round F: Recents row swipe (opt-in). Right = Call (via callUi, so confirm setting applies), left = SMS compose. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RowSwipeShotTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun setup(swipe: Boolean, font: Float = 1.0f) = runBlocking {
        RuntimeEnvironment.setFontScale(font)
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu Mandal"; p[K.theme] = "daylight"
            p[K.dialerScale] = 1f; p[K.rowSwipe] = swipe; p[K.confirmCall] = true
        }
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        val t0 = System.currentTimeMillis()
        while (LdApp.settings.value.rowSwipe != swipe && System.currentTimeMillis() - t0 < 5000) Thread.sleep(50)
        settle(2000)
        val act = rule.activity
        act.state.showOnboarding = false; act.state.isDefault = true; act.state.contacts = emptyList()
        val noon = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 12); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        act.state.recents = (3..8).map { Recent(it.toLong(), "+91 90000 000$it", "Zed $it", 2, noon - it * 45 * 60_000L, 95, null, null) }
        settle()
        val w0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - w0 < 6000 &&
            rule.onAllNodesWithText("Zed", substring = true).fetchSemanticsNodes().isEmpty()) {
            rule.mainClock.advanceTimeBy(300); rule.waitForIdle(); Thread.sleep(60)
        }
        Unit
    }

    private fun rows() = rule.onAllNodesWithTag("swipe_row", useUnmergedTree = true)

    @Test fun offByDefaultHasNoSwipeWrapper() {
        setup(false)
        assertEquals(0, rows().fetchSemanticsNodes().size)
    }

    @Test fun rightSwipeAsksToCallThatRow() {
        setup(true)
        assertTrue(rows().fetchSemanticsNodes().isNotEmpty())
        rows()[0].performTouchInput { swipeRight() }
        settle(1200)
        assertEquals("+91 90000 0003", rule.activity.state.confirmNumber)
    }

    @Test fun leftSwipeOpensSmsCompose() {
        setup(true)
        rows()[1].performTouchInput { swipeLeft() }
        settle(1200)
        val i = Shadows.shadowOf(rule.activity).nextStartedActivity
        assertNotNull("SMS intent chahiye", i)
        assertEquals(android.content.Intent.ACTION_SENDTO, i.action)
        assertEquals("smsto:+91 90000 0004", i.data.toString())
        assertNull(rule.activity.state.confirmNumber)
    }

    @Test fun shortSwipeDoesNothing() {
        setup(true)
        rows()[0].performTouchInput { down(Offset(width * .2f, centerY)); moveBy(Offset(width * .12f, 0f)); up() }
        settle(1200)
        assertNull(rule.activity.state.confirmNumber)
        assertNull(Shadows.shadowOf(rule.activity).nextStartedActivity)
    }

    @Test fun midDragShowsActionBackgroundRight() {
        setup(true)
        rows()[0].performTouchInput { down(Offset(width * .15f, centerY)); moveBy(Offset(width * .42f, 0f)) }
        settle(300)
        saveShot(rule, "F-swipe-call-armed-day")
        rows()[0].performTouchInput { up() }
    }

    @Test fun midDragShowsActionBackgroundLeftLargeFont() {
        try {
            setup(true, 1.3f)
            rows()[0].performTouchInput { down(Offset(width * .85f, centerY)); moveBy(Offset(-width * .42f, 0f)) }
            settle(300)
            saveShot(rule, "F-swipe-sms-armed-largefont")
            rows()[0].performTouchInput { up() }
        } finally { RuntimeEnvironment.setFontScale(1.0f) }
    }

    @Test fun actionFiresOnReleaseOnlyNotWhileHoldingPastThreshold() {
        setup(true)
        rows()[0].performTouchInput { down(Offset(width * .1f, centerY)); moveBy(Offset(width * .5f, 0f)) }
        settle(600)
        assertNull("release se pehle koi action nahi", rule.activity.state.confirmNumber)
        rows()[0].performTouchInput { up() }
        settle(800)
        assertEquals("+91 90000 0003", rule.activity.state.confirmNumber)
        rule.activity.state.confirmNumber = null
        settle(1500)
        assertNull("release ke baad dobara action nahi", rule.activity.state.confirmNumber)
        assertNull(Shadows.shadowOf(rule.activity).nextStartedActivity)
    }

    @Test fun draggingPastThresholdThenBackUnderCancelsAction() {
        setup(true)
        rows()[0].performTouchInput {
            down(Offset(width * .1f, centerY)); moveBy(Offset(width * .5f, 0f)); moveBy(Offset(-width * .4f, 0f)); up()
        }
        settle(1200)
        assertNull(rule.activity.state.confirmNumber)
    }

    @Test fun verticalDragOnRowIsNotAnAction() {
        setup(true)
        rows()[0].performTouchInput { down(Offset(width * .5f, centerY)); moveBy(Offset(10f, -150f)); up() }
        settle(1200)
        assertNull(rule.activity.state.confirmNumber)
        assertNull(Shadows.shadowOf(rule.activity).nextStartedActivity)
    }

    @Test fun incomingFilterChipStaysReachableAtLargeFont() {
        try {
            setup(true, 1.3f)
            rule.onNodeWithText("Incoming", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        } finally { RuntimeEnvironment.setFontScale(1.0f) }
    }
}
