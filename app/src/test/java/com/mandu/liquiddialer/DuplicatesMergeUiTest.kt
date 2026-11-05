package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.ContactMerge
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import android.provider.ContactsContract.AggregationExceptions
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Duplicates sheet ka SAFE merge (link) + alag karo: confirm flow, fake exec capture, linked section. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DuplicatesMergeUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val captured = mutableListOf<ContactMerge.MergeOp>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight" }
        captured.clear()
        ContactMerge.execOverride = object : ContactMerge.Exec {
            override fun apply(ops: List<ContactMerge.MergeOp>): Boolean { captured.addAll(ops); return true }
        }
        ContactMerge.rawIdsOverride = { _, cid -> listOf(100 + cid) }
        ContactMerge.linkedReaderOverride = { emptyList() }
        Unit
    }

    @After
    fun cleanup() {
        ContactMerge.execOverride = null
        ContactMerge.rawIdsOverride = null
        ContactMerge.linkedReaderOverride = null
    }

    private fun openDups(grantWrite: Boolean = true) {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        val perms = mutableListOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_CALL_LOG)
        if (grantWrite) perms.add(android.Manifest.permission.WRITE_CONTACTS)
        Shadows.shadowOf(ctx).grantPermissions(*perms.toTypedArray())
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
    fun mergeNeedsConfirmThenSendsKeepTogether() {
        openDups()
        rule.onNodeWithTag("dup_merge_9876543210", useUnmergedTree = true).assertExists().performClick()
        settle(400)
        rule.onNodeWithText("Pakka jodo?", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "282-dups-merge-day")
        rule.onNodeWithTag("dup_merge_yes_9876543210", useUnmergedTree = true).performClick()
        settle()
        assertTrue(captured.isNotEmpty())
        assertTrue(captured.all { it.type == AggregationExceptions.TYPE_KEEP_TOGETHER })
        assertEquals(111L, captured[0].raw1)
        assertEquals(112L, captured[0].raw2)
        rule.onNodeWithTag("dups_merge_msg", useUnmergedTree = true).assertExists()
    }

    @Test
    fun cancelConfirmSendsNothing() {
        openDups()
        rule.onNodeWithTag("dup_merge_9876543210", useUnmergedTree = true).performClick()
        settle(400)
        rule.onNodeWithTag("dup_merge_no_9876543210", useUnmergedTree = true).performClick()
        settle(400)
        assertTrue(captured.isEmpty())
    }

    @Test
    fun linkedSectionUnlinksWithKeepSeparate() {
        ContactMerge.linkedReaderOverride = { listOf(ContactMerge.LinkedGroup(11, listOf(111L, 222L))) }
        openDups()
        rule.onNodeWithTag("dups_linked_head", useUnmergedTree = true).assertExists()
        saveShot(rule, "282-dups-linked-day")
        rule.onNodeWithTag("dup_unlink_11", useUnmergedTree = true).assertExists().performClick()
        settle()
        assertTrue(captured.isNotEmpty())
        assertTrue(captured.all { it.type == AggregationExceptions.TYPE_KEEP_SEPARATE })
    }

    @Test
    fun largeFontMergeButtonsFit() {
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p -> p[K.fontScale] = 1.3f }
        }
        openDups()
        rule.onNodeWithTag("dup_merge_9876543210", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("dup_merge_9876543210", useUnmergedTree = true).performClick()
        settle(400)
        rule.onNodeWithText("Pakka jodo?", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "282-dups-merge-large")
    }

    @Test
    fun noWritePermissionShowsAskRowNoMergeButtons() {
        openDups(grantWrite = false)
        rule.onNodeWithText("Permission do", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("dup_merge_9876543210", useUnmergedTree = true).assertDoesNotExist()
    }
}
