package com.mandu.liquiddialer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Per-contact ringtone: user pick par hi provider write (lookup URI + CUSTOM_RINGTONE),
 *  failure par kabhi false-success nahi, aur row tap system picker kholta hai. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContactRingtoneTest {

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

    @Test
    fun updateBuildsLookupUriAndValues() {
        val (setUri, setV) = Repo.contactRingtoneUpdate(11, "lk1", "content://media/ring/42")
        assertEquals("content://com.android.contacts/contacts/lookup/lk1/11", setUri.toString())
        assertEquals("content://media/ring/42", setV.get(ContactsContract.Contacts.CUSTOM_RINGTONE))

        val (silentUri, silentV) = Repo.contactRingtoneUpdate(11, "lk1", "")
        assertEquals(setUri, silentUri)
        assertEquals("", silentV.get(ContactsContract.Contacts.CUSTOM_RINGTONE))

        // null = provider default wapas: key aati hai lekin value null
        val (_, clearV) = Repo.contactRingtoneUpdate(11, "lk1", null)
        assertTrue(clearV.containsKey(ContactsContract.Contacts.CUSTOM_RINGTONE))
        assertNull(clearV.get(ContactsContract.Contacts.CUSTOM_RINGTONE))
    }

    /** Fake contacts provider: rows = kitni rows update hui; deny = SecurityException (permission deny). */
    private class FakeContactsProvider(private val rows: Int, private val deny: Boolean = false) : ContentProvider() {
        override fun onCreate() = true
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            if (deny) throw SecurityException("WRITE_CONTACTS denied")
            return rows
        }
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?) = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun getType(uri: Uri): String? = null
    }

    @Test
    fun saveFailsClosedOnZeroRowsOrDenial() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY, FakeContactsProvider(0))
        // provider ne 0 rows update ki -> false, kabhi false-success nahi
        assertFalse(Repo.setContactRingtone(ctx, 11, "lk1", "content://media/ring/42"))
        assertFalse(Repo.setContactRingtone(ctx, 11, "lk1", null))
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY, FakeContactsProvider(0, deny = true))
        // WRITE_CONTACTS deny -> exception swallow karke false (crash nahi)
        assertFalse(Repo.setContactRingtone(ctx, 11, "lk1", "content://media/ring/42"))
    }

    @Test
    fun saveTrueOnlyWhenProviderUpdatesARow() {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY, FakeContactsProvider(1))
        assertTrue(Repo.setContactRingtone(ctx, 11, "lk1", "content://media/ring/42"))
        // real resolver path par sahi lookup URI + value gayi
        val stmt = shadowOf(ctx.contentResolver).updateStatements.last()
        assertEquals("content://com.android.contacts/contacts/lookup/lk1/11", stmt.uri.toString())
        assertEquals("content://media/ring/42", stmt.contentValues.get(ContactsContract.Contacts.CUSTOM_RINGTONE))
        // read fail-safe: provider query null de to null (= "Default" dikhaya jayega)
        assertNull(Repo.contactRingtone(ctx, 11))
    }

    @Test
    fun rowShowsDefaultAndOpensSystemPicker() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.contacts = listOf(Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", false, null))
        act.state.recents = listOf(Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null))
        shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS)
        settle()
        rule.onNodeWithText("Rahul Sharma", substring = true, useUnmergedTree = true).performClick()
        settle()
        // row + default subtitle (provider empty -> Default)
        rule.onNodeWithText("Ringtone", substring = false, useUnmergedTree = true).performScrollTo()
        settle(300)
        rule.onNodeWithText("Is contact ki ringtone", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "50-contact-ringtone-row")
        // tap -> system ringtone picker intent (TYPE_RINGTONE), koi provider write nahi
        rule.onNodeWithText("Ringtone", substring = false, useUnmergedTree = true).performClick()
        settle(400)
        val holder = shadowOf(act).peekNextStartedActivityForResult()
        assertTrue("ringtone picker intent launch hona chahiye", holder != null)
        assertEquals(RingtoneManager.ACTION_RINGTONE_PICKER, holder!!.intent.action)
        assertEquals(RingtoneManager.TYPE_RINGTONE, holder.intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1))
        assertEquals("Ringtone - Rahul Sharma", holder.intent.getStringExtra(RingtoneManager.EXTRA_RINGTONE_TITLE))
        saveShot(rule, "50b-contact-ringtone-picker-fired")
    }
}
