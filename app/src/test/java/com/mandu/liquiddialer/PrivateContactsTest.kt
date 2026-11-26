package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.LdThemeHost
import com.mandu.liquiddialer.ui.NumberSheet
import com.mandu.liquiddialer.ui.PrivateNumbersPage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Private contacts: app-local hide, default OFF, identity match, "Make private" action only when ON. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1000dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class PrivateContactsTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()
    private val a = "+91 98765 43210"
    private val b = "+44 20 7946 0018"

    private fun settle(ms: Long = 1500) { rule.mainClock.autoAdvance = false; rule.mainClock.advanceTimeBy(ms); rule.waitForIdle() }

    @After fun clean() {
        ctx.getSharedPreferences("private_numbers", 0).edit().clear().apply()
        runBlocking { ctx.ldStore.edit { it[K.privateContacts] = false }; Unit }
    }

    private fun prep(theme: String, on: Boolean) {
        runBlocking { ctx.ldStore.edit { it[K.onboarded] = true; it[K.theme] = theme; it[K.privateContacts] = on } }
        rule.waitUntil(15000) { LdApp.settings.value.theme == theme && LdApp.settings.value.privateContacts == on }
    }

    @Test fun defaultOffAndIdentityFilters() {
        assertFalse(LdSettings().privateContacts)
        val c1 = Contact(1, "k1", "Rahul", "", a, "MOBILE", false, null)
        val c2 = Contact(2, "k2", "Asha", "", b, "MOBILE", false, null)
        val hid = setOf(PrivateNumbers.id(a))
        assertEquals(listOf(c2), PrivateNumbers.hideContacts(hid, listOf(c1, c2)))
        // same number in another written format is still the same identity
        assertEquals(listOf(c2), PrivateNumbers.hideContacts(hid, listOf(Contact(3, "k3", "Rahul2", "", "0091 98765 43210", "MOBILE", false, null), c2)))
        assertEquals(2, PrivateNumbers.hideContacts(emptySet(), listOf(c1, c2)).size)
        val r = Recent(1, a, "Rahul", 1, 1000L, 10, null, null)
        val r2 = Recent(2, b, "Asha", 1, 1000L, 10, null, null)
        assertEquals(listOf(r2), PrivateNumbers.hideRecents(hid, listOf(r, r2)))
        // invalid number has empty identity and is never hidden or saved
        assertEquals("", PrivateNumbers.id("abc"))
        PrivateNumbers.set(ctx, "abc", true)
        assertTrue(PrivateNumbers.list(ctx).isEmpty())
        assertEquals(1, PrivateNumbers.hideContacts(hid, listOf(Contact(4, "k4", "X", "", "abc", "MOBILE", false, null))).size)
    }

    @Test fun switchOffHidesNothingAndKeepsList() {
        PrivateNumbers.set(ctx, a, true)
        assertEquals(1, PrivateNumbers.list(ctx).size)
        assertTrue(PrivateNumbers.hidden(ctx).isEmpty())
        prep("daylight", true)
        assertEquals(setOf(PrivateNumbers.id(a)), PrivateNumbers.hidden(ctx))
        prep("daylight", false)
        assertTrue(PrivateNumbers.hidden(ctx).isEmpty())
        assertEquals(1, PrivateNumbers.list(ctx).size)
    }

    private fun showSheet() {
        rule.runOnUiThread { rule.activity.setContent { LdThemeHost { NumberSheet(a, null, rule.activity.state, rule.activity, {}, {}, {}) } } }
        settle()
    }

    @Test fun makePrivateActionHiddenWhenSwitchOff() {
        prep("daylight", false)
        showSheet()
        rule.onAllNodesWithTag("sheet_make_private", useUnmergedTree = true).assertCountEquals(0)
        rule.onAllNodesWithText("Make private", substring = true, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun makePrivateActionShownWhenOnAndToggles() {
        prep("daylight", true)
        showSheet()
        rule.onAllNodesWithTag("sheet_make_private", useUnmergedTree = true).assertCountEquals(1)
        rule.onNodeWithText("Make private", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "PRIVATE540-sheet-before-tap")
        rule.onNodeWithTag("sheet_make_private", useUnmergedTree = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); settle(400)
        assertTrue(PrivateNumbers.isPrivate(ctx, a))
        // The real sheet closes after the tap. Re-open it fresh: the action now offers to undo.
        showSheet()
        rule.onNodeWithText("Private se hatao", substring = true, useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Make private", substring = true, useUnmergedTree = true).assertCountEquals(0)
        saveShot(rule, "PRIVATE540-sheet-reopened-after-private")
        rule.onNodeWithTag("sheet_make_private", useUnmergedTree = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick); settle(400)
        assertFalse(PrivateNumbers.isPrivate(ctx, a))
    }

    private fun showPage(theme: String, on: Boolean) {
        prep(theme, on)
        rule.runOnUiThread { rule.activity.setContent { LdThemeHost { PrivateNumbersPage(null, {}) } } }
        settle()
    }

    @Test fun pageNoteListShowAgainAndShots() {
        PrivateNumbers.set(ctx, a, true); PrivateNumbers.set(ctx, b, true)
        showPage("daylight", true)
        rule.onNodeWithTag("private_contacts_note", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("system-wide privacy nahi", substring = true, useUnmergedTree = true).assertExists()
        rule.onAllNodesWithTag("private_row", useUnmergedTree = true).assertCountEquals(2)
        saveShot(rule, "PRIVATE540-page-day")
        showPage("midnight", true)
        saveShot(rule, "PRIVATE540-page-night")
        rule.onAllNodesWithTag("private_show_again", useUnmergedTree = true).onFirst().performClick(); settle(400)
        assertEquals(1, PrivateNumbers.list(ctx).size)
        rule.onAllNodesWithTag("private_row", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test fun pageSwitchOffNoteAndEmpty() {
        showPage("daylight", false)
        rule.onNodeWithTag("private_contacts_note", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Koi private number nahi", useUnmergedTree = true).assertExists()
        assertFalse(LdApp.settings.value.privateContacts)
    }
}
