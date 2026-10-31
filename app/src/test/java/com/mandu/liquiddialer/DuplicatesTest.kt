package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Duplicates math: pure, detection-only (koi merge/delete nahi). Matching = aakhri 10 digits (POSSIBLE match, pakka nahi). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DuplicatesMathTest {
    private fun c(id: Long, name: String, num: String) = Contact(id, "lk$id", name, "", num, "MOBILE", false, null)

    @Test
    fun emptyAndNoDups() {
        assertTrue(Repo.duplicateGroups(emptyList()).isEmpty())
        assertTrue(Repo.duplicateGroups(listOf(c(1, "A", "+91 98765 43210"), c(2, "B", "+91 91234 56789"))).isEmpty())
    }

    @Test
    fun sameNumberDifferentFormatsGroups() {
        val g = Repo.duplicateGroups(listOf(
            c(1, "Rahul", "+91 98765 43210"),
            c(2, "Rahul S", "09876543210"),
            c(3, "Rahul Sharma", "98765 43210"),
            c(4, "Papa", "+91 91234 56789"),
        ))
        assertEquals(1, g.size)
        assertEquals(3, g[0].count)
        assertEquals(3, g[0].contacts.map { it.id }.distinct().size) // har contact alag se milta hai
        assertEquals("9876543210", g[0].numberKey)
    }

    @Test
    fun countryCodeCollisionGroupsButIsOnlyPossible() {
        // +91 98765 43210 aur +1 (987) 654-3210: aakhri 10 digits same -> EK group (known approximation).
        // Full digit strings alag hain - isi liye UI "possible" bolta hai, "same number" pakka claim nahi.
        val g = Repo.duplicateGroups(listOf(
            c(1, "India Wala", "+91 98765 43210"),
            c(2, "US Wala", "+1 98765 43210"),
        ))
        assertEquals(1, g.size)
        assertEquals(2, g[0].count)
        val fullDigits = g[0].contacts.map { it.number.filter { ch -> ch.isDigit() } }
        assertNotEquals(fullDigits[0], fullDigits[1])
    }

    @Test
    fun sameContactTwiceIsNotDup() {
        assertTrue(Repo.duplicateGroups(listOf(c(1, "A", "+91 98765 43210"), c(1, "A", "+91 98765 43210"))).isEmpty())
    }

    @Test
    fun shortNumbersIgnored() {
        assertTrue(Repo.duplicateGroups(listOf(c(1, "A", "12345"), c(2, "B", "12345"))).isEmpty())
    }
}

/** Duplicates sheet: Contacts tab se "N dup" chip -> sheet, possible-label caveat + per-contact rows + expand. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DuplicatesSheetShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight" }
        Unit
    }

    private fun openContactsWithDups() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_CALL_LOG)
        act.state.showOnboarding = false
        act.state.isDefault = true
        act.state.contacts = listOf(
            Contact(11, "lk1", "Rahul", "", "+91 98765 43210", "MOBILE", false, null),
            Contact(12, "lk2", "Rahul Sharma", "", "98765 43210", "MOBILE", false, null),
            Contact(13, "lk3", "Papa", "", "+91 91234 56789", "MOBILE", false, null),
        )
        act.state.tab = "contacts"
        settle()
        rule.onNodeWithTag("dups_open", useUnmergedTree = true).assertExists().performClick()
        settle()
    }

    @Test
    fun sheetShowsPossibleLabelAndEachContactRow() {
        openContactsWithDups()
        rule.onNodeWithText("Possible duplicate contacts", substring = true, useUnmergedTree = true).assertExists()
        // approximation ka saaf dawa + detection-only
        rule.onNodeWithText("aakhri 10 digits same", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("delete ya merge nahi hoga", substring = true, useUnmergedTree = true).assertExists()
        // HAR contact ki apni row (sirf arbitrary first nahi)
        rule.onNodeWithTag("dup_contact_11", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("dup_contact_12", useUnmergedTree = true).assertExists()
        saveShot(rule, "49-duplicates-sheet")
    }

    @Test
    fun contactRowOpensThatContact() {
        openContactsWithDups()
        rule.onNodeWithTag("dup_contact_12", useUnmergedTree = true).performClick()
        settle()
        // dup sheet band, number sheet khula (Rahul Sharma wala)
        rule.onNodeWithText("Possible duplicate contacts", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun expandReachesRemainingGroups() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_CALL_LOG)
        act.state.showOnboarding = false
        act.state.isDefault = true
        // 32 groups (64 contacts) -> cap 30 + expand
        val list = mutableListOf<Contact>()
        var id = 1L
        for (g in 1..32) {
            val base = 9000000000L + g * 137
            list.add(Contact(id++, "lk${id}", "UserA$g", "", "+91 $base", "MOBILE", false, null))
            list.add(Contact(id++, "lk${id}", "UserB$g", "", "$base", "MOBILE", false, null))
        }
        act.state.contacts = list
        act.state.tab = "contacts"
        settle()
        rule.onNodeWithTag("dups_open", useUnmergedTree = true).assertExists().performClick()
        settle()
        rule.onNodeWithTag("dups_expand", useUnmergedTree = true).assertExists().performClick()
        settle()
        // expand ke baad sheet khuli rahti hai aur control gayab (showAll flip)
        rule.onNodeWithText("Possible duplicate contacts", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("dups_expand", useUnmergedTree = true).assertDoesNotExist()
        // 32nd group ka contact ab pahunch sakta hai
        val lastId = 2L * 32
        rule.onNodeWithTag("dup_contact_$lastId", useUnmergedTree = true).assertExists()
    }

    @Test
    fun noChipWhenNoDups() {
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.isDefault = true
        act.state.contacts = listOf(
            Contact(11, "lk1", "Rahul", "", "+91 98765 43210", "MOBILE", false, null),
            Contact(13, "lk3", "Papa", "", "+91 91234 56789", "MOBILE", false, null),
        )
        act.state.tab = "contacts"
        settle()
        rule.onNodeWithTag("dups_open", useUnmergedTree = true).assertDoesNotExist()
    }
}
