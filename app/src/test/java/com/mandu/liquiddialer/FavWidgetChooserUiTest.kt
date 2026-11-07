package com.mandu.liquiddialer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver

/** Widget multi-number favorite chooser dialog: fill-only selection, cancel no-op,
 *  deleted contact ka safe feedback. Day theme. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class FavWidgetChooserUiTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>(effectContext = kotlinx.coroutines.test.StandardTestDispatcher())

    internal open class FakeDetailProvider(var phones: List<Pair<String, Int>> = listOf("+91 90000 11111" to 2, "+91 90000 22222" to 2)) : ContentProvider() {
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            val id = selectionArgs?.firstOrNull()?.toLongOrNull()
            val cols = projection ?: return null
            val mc = MatrixCursor(cols)
            when (uri) {
                ContactsContract.Contacts.CONTENT_URI -> if (id == 11L) {
                    mc.addRow(cols.map { c -> when (c) {
                        "display_name" -> "Rahul Sharma"; "lookup" -> "lk11"; "starred" -> 1; "photo_uri" -> null; else -> null } })
                }
                ContactsContract.RawContacts.CONTENT_URI -> if (id == 11L) mc.addRow(cols.map { 1101L })
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI -> if (id == 11L) {
                    phones.forEach { (n, t) ->
                        mc.addRow(cols.map { c -> when (c) { "data1" -> n; "data2" -> t; "contact_id" -> 11L; else -> null } })
                    }
                }
                else -> return null
            }
            return mc
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun getType(uri: Uri): String? = null
    }

    private val validationStarted = java.util.concurrent.CountDownLatch(1)
    private val validationRelease = java.util.concurrent.CountDownLatch(1)
    private val validationDone = java.util.concurrent.CountDownLatch(1)
    @Volatile private var blockValidation = false
    private val queryTrace = java.util.concurrent.CopyOnWriteArrayList<String>()
    internal val fakeProvider = object : FakeDetailProvider() {
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
            if (blockValidation && uri == ContactsContract.CommonDataKinds.Phone.CONTENT_URI) {
                validationStarted.countDown()
                check(validationRelease.await(10, java.util.concurrent.TimeUnit.SECONDS)) { "validation release timed out" }
            }
            queryTrace.add("start ${uri.path} ids=${selectionArgs?.toList()} thread=${Thread.currentThread().name}")
            return super.query(uri, projection, selection, selectionArgs, sortOrder).also {
                queryTrace.add("done ${uri.path} rows=${it?.count} at=${System.currentTimeMillis() % 1000000}")
                if (blockValidation) validationDone.countDown()
            }
        }
    }

    @Before fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight" }
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY, fakeProvider)
        Unit
    }

    internal fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    /** Click ke baad IO->Main coroutine real-time leti hai; virtual clock kaafi nahi. */
    internal fun awaitNodes(tag: String, count: Int) {
        // Advance snapshot notifications and UI frames as well as the looper. A completed producer
        // assignment alone does not prove the dialog has observed that snapshot. Assertions unchanged.
        val t0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - t0 < 60_000 && rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size != count) {
            rule.runOnUiThread { androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications() }
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(150)
            rule.mainClock.autoAdvance = false
            rule.mainClock.advanceTimeBy(200)
            rule.waitForIdle()
        }
        val st = rule.activity.state
        assertEquals("await $tag; waitedMs=${System.currentTimeMillis() - t0}; request=${st.numberChoiceRequest}, nonce=${st.numberChoiceNonce}, onboarding=${st.showOnboarding}, settings=${LdApp.settings.value}, queries=$queryTrace; loadTrace=${org.robolectric.shadows.ShadowLog.getLogsForTag("ChooserLoad")}; flags=loading:${rule.onAllNodesWithTag("chooser_loading", useUnmergedTree = true).fetchSemanticsNodes().size},missing:${rule.onAllNodesWithTag("chooser_missing", useUnmergedTree = true).fetchSemanticsNodes().size},title:${rule.onAllNodesWithTag("chooser_title", useUnmergedTree = true).fetchSemanticsNodes().size}; tree=" + rule.onAllNodes(isRoot(), useUnmergedTree = true).printToString(maxDepth = 12),
            count, rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    private fun openChooser(id: Long = 11L, name: String = "Rahul Sharma", expectNumbers: Int = 2) {
        val act = rule.activity
        act.state.showOnboarding = false
        org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>())
            .grantPermissions(android.Manifest.permission.READ_CONTACTS)
        if (expectNumbers > 0) assertEquals("provider fixture before chooser", expectNumbers,
            com.mandu.liquiddialer.data.Repo.contactDetail(act, id)?.phones?.size)
        act.state.numberChoiceRequest = id to name
        settle()
        // produceState ka IO real-time chalta hai: fixed settle busy CI JVM par race karta hai (run 194 flake) - content ka poll karo
        if (expectNumbers > 0) awaitNodes("chooser_num", expectNumbers)
        else awaitNodes("chooser_missing", 1)
    }

    @Test fun chooserShowsBothNumbersAndFillsOnlySelected() {
        settle(2500)
        openChooser()
        rule.onNodeWithTag("chooser_title", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true).assertCountEquals(2)
        saveDialogShot("W6-chooser-dialog-day", 0xFFF4F6FA)
        // Number 2 chuno -> SIRF fill, koi call nahi
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true)[1].performClick()
        settle()
        val act = rule.activity
        assertEquals("+91 90000 22222", act.state.dial)
        assertEquals("dialer", act.state.tab)
        assertNull("chooser kabhi call nahi karta", act.state.pendingCall)
        assertNull(act.state.confirmNumber)
        assertNull(act.state.numberChoiceRequest)
    }

    @Test fun dismissedChooserCannotFillAfterSlowValidation() {
        settle(2500)
        openChooser()
        blockValidation = true
        try {
            rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true)[0].performClick()
            org.junit.Assert.assertTrue("click validation started", validationStarted.await(5, java.util.concurrent.TimeUnit.SECONDS))
            rule.onNodeWithTag("chooser_cancel", useUnmergedTree = true).performClick()
            settle()
            assertNull(rule.activity.state.numberChoiceRequest)
            validationRelease.countDown()
            org.junit.Assert.assertTrue("query completed", validationDone.await(5, java.util.concurrent.TimeUnit.SECONDS))
            // Let the IO continuation and its Main publication run after dismissal.
            repeat(8) { Thread.sleep(100); settle(200) }
            assertEquals("dismissed dialog must not fill later", "", rule.activity.state.dial)
            assertNull(rule.activity.state.pendingCall)
        } finally { validationRelease.countDown() }
    }

    @Test fun replacedChooserCannotBeOverwrittenBySlowValidation() {
        settle(2500)
        openChooser()
        blockValidation = true
        try {
            rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true)[0].performClick()
            org.junit.Assert.assertTrue("click validation started", validationStarted.await(5, java.util.concurrent.TimeUnit.SECONDS))
            rule.runOnUiThread { rule.activity.state.numberChoiceRequest = 99L to "Different contact" }
            settle()
            awaitNodes("chooser_missing", 1)
            validationRelease.countDown()
            org.junit.Assert.assertTrue("query completed", validationDone.await(5, java.util.concurrent.TimeUnit.SECONDS))
            repeat(8) { Thread.sleep(100); settle(200) }
            assertEquals("new request must remain open", 99L to "Different contact", rule.activity.state.numberChoiceRequest)
            assertEquals("old choice must not fill", "", rule.activity.state.dial)
            assertNull(rule.activity.state.pendingCall)
        } finally { validationRelease.countDown() }
    }

    @Test fun cancelIsNoOp() {
        settle(2500)
        openChooser()
        rule.onNodeWithTag("chooser_cancel", useUnmergedTree = true).performClick()
        settle()
        val act = rule.activity
        assertNull(act.state.numberChoiceRequest)
        assertEquals("", act.state.dial)
        assertNull(act.state.pendingCall)
    }

    @Test fun chooserDialogNightThemeShot() {
        settle(2500)
        // Theme midnight me switch (day JVM me produceState reliably deliver hota hai)
        runBlocking { ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p -> p[K.theme] = "midnight" } }
        settle()
        openChooser()
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true).assertCountEquals(2)
        rule.onNodeWithTag("chooser_title", useUnmergedTree = true).assertExists()
        saveDialogShot("W7-chooser-dialog-night", 0xFF141824)
    }

    @Test fun clickTimeRevalidationAfterContactEdited() {
        settle(2500)
        openChooser()
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true).assertCountEquals(2)
        // Dialog khulne ke BAAD contact se number 2 hata diya
        fakeProvider.phones = listOf("+91 90000 11111" to 2)
        // Purana (stale) number 2 par click -> fill NAHI hona chahiye; list refresh + feedback
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true)[1].performClick()
        awaitNodes("chooser_num", 1)
        val act = rule.activity
        assertEquals("stale number kabhi fill nahi", "", act.state.dial)
        assertNull(act.state.pendingCall)
        // Dialog khula hai aur nayi list (sirf 1 number) dikhata hai
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true).assertCountEquals(1)
        // Nayi list ka valid number click karne par fill hota hai
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true)[0].performClick()
        awaitNodes("chooser_num", 0)  // dialog band
        assertEquals("+91 90000 11111", act.state.dial)
        assertEquals("dialer", act.state.tab)
        assertNull(act.state.pendingCall)
    }

    @Test fun clickTimeRevalidationAfterPermissionRevoked() {
        settle(2500)
        openChooser()
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true).assertCountEquals(2)
        // Dialog khula, AB permission revoke (device par query SecurityException deti hai - wahi simulate)
        org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>())
            .denyPermissions(android.Manifest.permission.READ_CONTACTS)
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY,
            object : ContentProvider() {
                override fun onCreate() = true
                override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? =
                    throw SecurityException("READ_CONTACTS denied")
                override fun insert(uri: Uri, values: ContentValues?): Uri? = null
                override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
                override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
                override fun getType(uri: Uri): String? = null
            })
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true)[0].performClick()
        awaitNodes("chooser_missing", 1)
        val act = rule.activity
        assertEquals("revoked permission par kabhi fill nahi", "", act.state.dial)
        assertNull(act.state.pendingCall)
        // Safe feedback state (missing message) ya refresh - koi crash/stale fill nahi
        rule.onNodeWithTag("chooser_missing", useUnmergedTree = true).assertExists()
    }

    @Test fun deletedContactShowsSafeFeedback() {
        settle(2500)
        openChooser(id = 99L, name = "Deleted Person", expectNumbers = 0)
        rule.onNodeWithTag("chooser_missing", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithTag("chooser_num", useUnmergedTree = true).assertCountEquals(0)
        saveDialogShot("W8-chooser-missing-day", 0xFFF4F6FA)
        rule.onNodeWithTag("chooser_cancel", useUnmergedTree = true).performClick()
        settle()
        assertNull(rule.activity.state.numberChoiceRequest)
    }

    @Test fun refreshResetsCompositionBeforeNewProviderRead() {
        settle(2500)
        openChooser()
        fakeProvider.phones = listOf("+91 90000 33333" to 2)
        blockValidation = true
        val loadingBeforeRead = java.util.concurrent.atomic.AtomicBoolean(false)
        // Robolectric can execute provider calls on its SDK Main Thread. Observe the already
        // committed composition trace off-thread, then release the provider instead of blocking UI queries.
        val observer = Thread {
            if (validationStarted.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                val composed = org.robolectric.shadows.ShadowLog.getLogsForTag("ChooserLoad")
                    .lastOrNull { it.msg.contains("composed rows=") }?.msg.orEmpty()
                loadingBeforeRead.set(composed.contains("composed rows=null"))
            }
            validationRelease.countDown()
        }
        observer.start()
        try {
            rule.runOnUiThread { rule.activity.state.numberChoiceNonce++ }
            settle()
            observer.join(6000)
            org.junit.Assert.assertFalse("provider observer completed", observer.isAlive)
            org.junit.Assert.assertTrue("refresh starts with no old rows in composition", loadingBeforeRead.get())
            awaitNodes("chooser_num", 1)
            rule.onNodeWithText("+91 90000 33333", useUnmergedTree = true).assertExists()
            rule.onNodeWithText("+91 90000 11111", useUnmergedTree = true).assertDoesNotExist()
            rule.onNodeWithText("+91 90000 22222", useUnmergedTree = true).assertDoesNotExist()
            rule.onNodeWithTag("chooser_loading", useUnmergedTree = true).assertDoesNotExist()
            assertEquals("refresh cannot fill", "", rule.activity.state.dial)
            rule.onNodeWithTag("chooser_cancel", useUnmergedTree = true).performClick()
            settle()
            assertNull(rule.activity.state.numberChoiceRequest)
            assertNull(rule.activity.state.pendingCall)
        } finally { validationRelease.countDown(); observer.join(6000) }
    }
}
