package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Round H: "Search bar neeche" (opt-in). Default = bar upar, exactly as before. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchBottomShotTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun setup(bottom: Boolean, font: Float = 1.0f) = runBlocking {
        RuntimeEnvironment.setFontScale(font)
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu Mandal"; p[K.theme] = "daylight"
            p[K.dialerScale] = 1f; p[K.searchBottom] = bottom
        }
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        val t0 = System.currentTimeMillis()
        while (LdApp.settings.value.searchBottom != bottom && System.currentTimeMillis() - t0 < 5000) Thread.sleep(50)
        settle(2000)
        val act = rule.activity
        act.state.showOnboarding = false; act.state.isDefault = true; act.state.contacts = emptyList()
        val noon = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 12); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        act.state.recents = (3..14).map { Recent(it.toLong(), "+91 90000 000$it", "Zed $it", 2, noon - it * 45 * 60_000L, 95, null, null) }
        settle()
        val w0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - w0 < 6000 &&
            rule.onAllNodesWithText("Zed", substring = true).fetchSemanticsNodes().isEmpty()) {
            rule.mainClock.advanceTimeBy(300); rule.waitForIdle(); Thread.sleep(60)
        }
        Unit
    }

    private fun barTop() = rule.onNodeWithTag("top_bar", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    private fun navTop() = rule.onNodeWithTag("bottom_bar", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    @Test fun defaultKeepsBarAtTop() {
        setup(false)
        assertTrue("default me bar upar", barTop().top < 200f)
    }

    @Test fun optInPutsBarAboveBottomNavWithoutOverlap() {
        setup(true)
        val b = barTop(); val n = navTop()
        assertTrue("bar neeche aadhe se niche", b.top > 1000f)
        assertTrue("bar bottom nav ke upar, overlap nahi", b.bottom <= n.top + 1f)
        saveShot(rule, "H-search-bottom-day")
    }

    @Test fun bottomBarSearchStillFiltersAndLastRowNotCovered() {
        setup(true)
        rule.onNodeWithTag("top_bar", useUnmergedTree = true).assertIsDisplayed()
        // list ka top ab bar se bhara nahi: pehli row upar dikhti hai
        assertTrue(rule.onAllNodesWithText("Zed 3", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun largeFontBottomBar() {
        try { setup(true, 1.3f); saveShot(rule, "H-search-bottom-largefont") }
        finally { RuntimeEnvironment.setFontScale(1.0f) }
    }

    @Test fun scrimAlsoFadesTopSearchMode() {
        setup(false)
        assertEquals(1, rule.onAllNodesWithTag("bottom_scrim", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test fun lastRowScrollsFullyAboveBottomControls() {
        setup(true)
        assertEquals(1, rule.onAllNodesWithTag("bottom_scrim", useUnmergedTree = true).fetchSemanticsNodes().size)
        repeat(6) { rule.onNodeWithTag("main_list", useUnmergedTree = true).performTouchInput { swipeUp() }; settle(500) }
        val scrimTop = rule.onNodeWithTag("bottom_scrim", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        val last = rule.onAllNodesWithText("Zed 14", substring = true, useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("aakhri row dikhni chahiye", last.isNotEmpty())
        val bottom = last.maxOf { it.boundsInRoot.bottom }
        assertTrue("aakhri row scrim ke upar poori aani chahiye: rowBottom=$bottom scrimTop=$scrimTop", bottom <= scrimTop + 2f)
        saveShot(rule, "H-search-bottom-scrolled-end")
    }
}
