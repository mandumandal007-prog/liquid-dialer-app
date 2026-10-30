package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
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
class CodesShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
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
        rule.onNodeWithTag("dialer_fab").performClick(); settle(2500)
    }

    private fun tapKey(k: String) {
        rule.onNode(hasText(k) and hasNoClickAction() and !hasAnyAncestor(hasTestTag("bottom_bar")) and !hasSetTextAction(), useUnmergedTree = true)
            .performTouchInput { click(center) }
        settle(250)
    }

    private fun dial(s: String) { s.forEach { tapKey(it.toString()) }; settle() }

    @Test
    fun secretCard() {
        load()
        dial("*#*#4636#*#*")
        saveShot(rule, "code-secret-card")
    }

    @Test
    fun oemCard() {
        load()
        dial("*#899#")
        saveShot(rule, "code-oem-card")
    }

    @Test
    fun radioPage() {
        load()
        rule.onNodeWithTag("dial_menu").performClick(); settle()
        saveShot(rule, "dial-menu-radio-entry")
        rule.onNodeWithTag("radio_menu_item").performClick(); settle(1500)
        saveShot(rule, "radio-permission-state")
    }
}
