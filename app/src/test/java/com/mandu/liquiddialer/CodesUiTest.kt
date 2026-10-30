package com.mandu.liquiddialer

import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
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
class CodesUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f
        }
        com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        Unit
    }

    private fun load() {
        settle(2500)
        rule.activity.state.showOnboarding = false
        rule.activity.state.isDefault = true
        shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        settle()
    }

    private fun tapKey(k: String) {
        rule.onNode(hasText(k) and hasNoClickAction() and !hasAnyAncestor(hasTestTag("bottom_bar")) and !hasSetTextAction(), useUnmergedTree = true)
            .performTouchInput { click(center) }
        settle(300)
    }

    private fun dial(s: String) { s.forEach { tapKey(it.toString()) }; settle() }

    private fun openDialer() {
        rule.onNodeWithTag("dialer_fab").performClick(); settle(2500)
    }

    @Test
    fun secretCodeShowsCardWithExplicitRequestOnly() {
        load(); openDialer()
        dial("*#*#4636#*#*")
        rule.onNodeWithTag("code_card", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Request bhejo").assertIsDisplayed()
        // typing alone must not execute anything: no call, no activity launch
        val i = shadowOf(rule.activity.application).nextStartedActivity
        assertTrue("unexpected activity start: $i", i == null || i.action != Intent.ACTION_CALL)
    }

    @Test
    fun oemCodeShowsHonestNoRoute() {
        load(); openDialer()
        dial("*#899#")
        rule.onNodeWithTag("code_card", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("OEM code: *#899#", substring = true).assertIsDisplayed()
        rule.onNode(hasText("verified raasta nahi", substring = true), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("code_action", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun imeiCodeOffersLabeledSettingsHandoff() {
        load(); openDialer()
        dial("*#06#")
        rule.onNodeWithTag("code_card", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Settings kholo").performClick(); settle()
        val i = shadowOf(rule.activity.application).nextStartedActivity
        assertEquals(Settings.ACTION_DEVICE_INFO_SETTINGS, i?.action)
    }

    @Test
    fun radioInfoEntryFromDialMenuShowsPermissionAsk() {
        load(); openDialer()
        rule.onNodeWithTag("dial_menu").performClick(); settle()
        rule.onNodeWithTag("radio_menu_item").performClick(); settle(1500)
        rule.onNodeWithTag("radio_screen", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("radio_perm_needed", useUnmergedTree = true).assertIsDisplayed()
        rule.onNode(hasAnyAncestor(hasTestTag("page_back")) and hasClickAction(), useUnmergedTree = true).performClick(); settle()
        rule.onNodeWithTag("radio_screen", useUnmergedTree = true).assertDoesNotExist()
    }
}
