package com.mandu.liquiddialer

import android.media.RingtoneManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Normal phone viewport (360x800): Ringtone row initial viewport me dikhti hai aur
 *  bina scroll ke tap se system picker khulta hai. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContactRingtoneViewportTest {

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
    fun rowVisibleAndTappableWithoutScrollAt360x800() {
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
        // normal viewport: row screen par actually displayed (scroll nahi chahiye)
        rule.onNodeWithText("Ringtone", substring = false, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Is contact ki ringtone", substring = true, useUnmergedTree = true).assertIsDisplayed()
        saveShot(rule, "51-ringtone-viewport-360x800")
        // bina scroll tap -> system picker intent
        rule.onNodeWithText("Ringtone", substring = false, useUnmergedTree = true).performClick()
        settle(400)
        val holder = shadowOf(act).peekNextStartedActivityForResult()
        assertTrue("ringtone picker intent launch hona chahiye", holder != null)
        assertEquals(RingtoneManager.ACTION_RINGTONE_PICKER, holder!!.intent.action)
        assertEquals(RingtoneManager.TYPE_RINGTONE, holder.intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1))
    }
}
