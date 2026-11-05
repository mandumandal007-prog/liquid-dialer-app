package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.telecom.AutoRedial
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Auto-redial UI: number sheet ka button, session card, Rok do, large-font fit. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AutoRedialUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val dialed = mutableListOf<String>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight" }
        dialed.clear()
        AutoRedial.session.value = null
        AutoRedial.placeCall = { _, num -> dialed.add(num); true }
        Unit
    }

    @After
    fun cleanup() {
        AutoRedial.session.value = null
        AutoRedial.placeCall = { c, n -> com.mandu.liquiddialer.telecom.CallManager.place(c, n, null) }
    }

    private fun openNumberSheet(fontScale: Float = 1f) {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        if (fontScale != 1f) runBlocking { ctx.ldStore.edit { p -> p[K.fontScale] = fontScale } }
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_CALL_LOG)
        act.state.showOnboarding = false
        act.state.isDefault = true
        act.state.contacts = listOf(Contact(11, "lk1", "Rahul", "", "+91 98765 43210", "MOBILE", false, null))
        act.state.tab = "contacts"
        settle()
        settle(800)
        rule.onAllNodesWithText("Rahul", substring = true, useUnmergedTree = true).onFirst().performClick()
        settle()
    }

    @Test
    fun sheetHasAutoRedialThatStartsSession() {
        openNumberSheet()
        rule.onNodeWithTag("autoredial_start", useUnmergedTree = true).assertExists()
        saveShot(rule, "283-autoredial-sheet-day")
        rule.onNodeWithTag("autoredial_start", useUnmergedTree = true).performClick()
        settle()
        assertEquals(listOf("+91 98765 43210"), dialed)
        rule.onNodeWithTag("autoredial_card", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Auto-redial · Rahul", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "283-autoredial-day")
    }

    @Test
    fun rokDoCancelsSession() {
        openNumberSheet()
        rule.onNodeWithTag("autoredial_start", useUnmergedTree = true).performClick()
        settle()
        rule.onNodeWithTag("autoredial_stop", useUnmergedTree = true).assertExists().performClick()
        settle()
        rule.onNodeWithTag("autoredial_card", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun largeFontCardFits() {
        openNumberSheet(fontScale = 1.3f)
        rule.onNodeWithTag("autoredial_start", useUnmergedTree = true).performClick()
        settle()
        rule.onNodeWithTag("autoredial_card", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("autoredial_stop", useUnmergedTree = true).assertExists()
        saveShot(rule, "283-autoredial-large")
    }
}
