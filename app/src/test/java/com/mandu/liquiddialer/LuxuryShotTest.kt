package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Luxury/premium restyle ke pixel proofs: drawer chips, settings rows, recents/contacts buttons, dono themes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LuxuryShotTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 1200) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun boot(theme: String) {
        val ctx0 = ApplicationProvider.getApplicationContext<LdApp>()
        runBlocking { ctx0.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu Mandal"; p[K.profileNumber] = "+91 83888 80466"; p[K.theme] = theme; p[K.dialerScale] = 1f } }
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        rule.activity.state.showOnboarding = false
        rule.activity.state.isDefault = true
        rule.activity.state.contacts = listOf(
            Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null),
            Contact(12, "lk2", "Papa", "", "+91 91234 56789", "MOBILE", false, null),
            Contact(13, "lk3", "Priya Verma", "", "+91 90000 11111", "MOBILE", false, null),
        )
        rule.activity.state.recents = listOf(
            Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null),
            Recent(2, "+91 91234 56789", "Papa", 2, System.currentTimeMillis() - 5 * 3600_000, 0, null, null),
            Recent(3, "+91 90000 11111", "Priya Verma", 3, System.currentTimeMillis() - 26 * 3600_000, 90, null, null),
        )
        settle()
    }

    private fun openDrawer() {
        rule.onNodeWithTag("drawer_open", useUnmergedTree = true).performTouchInput { click(center) }
        settle()
    }

    @Test
    fun drawerDark() {
        boot("midnight")
        openDrawer()
        saveShot(rule, "L1-drawer-dark")
    }

    @Test
    fun settingsDark() {
        boot("midnight")
        openDrawer()
        rule.onNodeWithText("Settings", substring = true, useUnmergedTree = true).performClick()
        settle()
        saveShot(rule, "L2-settings-dark")
    }

    @Test
    fun recentsDark() {
        boot("midnight")
        saveShot(rule, "L3-recents-dark")
    }

    @Test
    fun recentsLight() {
        boot("daylight")
        saveShot(rule, "L4-recents-light")
    }

    @Test
    fun drawerScrollEndDark() {
        // drawer footer (Default phone app status) POORA scroll se reachable + nav inset ke upar
        boot("midnight")
        openDrawer()
        rule.mainClock.autoAdvance = true // performScrollTo ko animation frames chahiye (autoAdvance=false me hang)
        rule.onNodeWithText("Default phone app", substring = true, useUnmergedTree = true).performScrollTo()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        settle(400)
        rule.onNodeWithText("Default phone app", substring = true, useUnmergedTree = true).assertIsDisplayed()
        saveShot(rule, "L1b-drawer-end-dark")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi", fontScale = 1.3f)
    fun dialerBigfont() {
        // fontScale 1.3 pe bhi digits/sublabels ka contrast sheet ke neeche ke list se compromised nahi
        boot("midnight")
        rule.onAllNodes(hasContentDescription(""), useUnmergedTree = true)
        rule.activity.state.tab = "dialer"
        settle()
        saveShot(rule, "L7-dialer-bigfont")
    }

    @Test
    fun contactDetailDark() {
        boot("midnight")
        rule.onNodeWithText("Contacts", substring = true, useUnmergedTree = true).performTouchInput { click(center) }
        settle()
        rule.onNodeWithText("Rahul Sharma", substring = true, useUnmergedTree = true).performTouchInput { click(center) }
        settle()
        saveShot(rule, "L6-contact-detail-dark")
    }

    @Test
    fun redialHintDark() {
        // empty dial pe aakhri outgoing number ka hint dikhe (redial discoverability); tap kabhi auto-call nahi karta
        boot("midnight")
        rule.activity.state.tab = "dialer"
        settle()
        rule.onNodeWithText("Pichhla:", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("call button se bharo", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("91234 56789", substring = true, useUnmergedTree = true).assertIsDisplayed()
        saveShot(rule, "L10-redial-hint-dark")
    }

    @Test
    fun hisabDark() {
        boot("midnight")
        openDrawer()
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).performClick()
        settle()
        rule.onNodeWithText("CSV share karo", substring = true, useUnmergedTree = true).assertIsDisplayed()
        saveShot(rule, "L11-hisab-dark")
    }

    @Test
    fun hisabLight() {
        boot("daylight")
        openDrawer()
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).performClick()
        settle()
        rule.onNodeWithText("CSV share karo", substring = true, useUnmergedTree = true).assertIsDisplayed()
        saveShot(rule, "L12-hisab-light")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi", fontScale = 1.3f)
    fun hisabBigfont() {
        boot("midnight")
        openDrawer()
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).performClick()
        settle()
        rule.onNodeWithText("CSV share karo", substring = true, useUnmergedTree = true).assertIsDisplayed()
        saveShot(rule, "L13-hisab-bigfont")
    }

    @Test
    fun blockedDark() {
        boot("midnight")
        openDrawer()
        rule.onNodeWithText("Blocked numbers", substring = true, useUnmergedTree = true).performClick()
        settle()
        saveShot(rule, "L8-blocked-dark")
    }

    @Test
    fun voicemailSheetDark() {
        boot("midnight")
        openDrawer()
        rule.onNodeWithText("Settings", substring = true, useUnmergedTree = true).performClick()
        settle()
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("Voicemail number", substring = true, useUnmergedTree = true).performScrollTo()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        settle(400)
        rule.onNodeWithText("Voicemail number", substring = true, useUnmergedTree = true).performClick()
        settle()
        saveShot(rule, "L9-voicemail-sheet-dark")
    }

    @Test
    fun contactsLight() {
        boot("daylight")
        rule.onNodeWithText("Contacts", substring = true, useUnmergedTree = true).performTouchInput { click(center) }
        settle()
        saveShot(rule, "L5-contacts-light")
    }
}

/** Hisab CSV share ki wiring: content:// attachment + read grant + chooser (khud kuch auto-send nahi), empty window pe Toast. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class HisabShareUiTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 1200) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun boot(withRecents: Boolean, grantPermission: Boolean = true) {
        val ctx0 = ApplicationProvider.getApplicationContext<LdApp>()
        runBlocking { ctx0.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu Mandal"; p[K.profileNumber] = "+91 83888 80466"; p[K.theme] = "midnight"; p[K.dialerScale] = 1f } }
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        if (grantPermission) Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        rule.activity.state.showOnboarding = false
        rule.activity.state.isDefault = true
        rule.activity.state.recents = if (withRecents) listOf(
            Recent(1, "+91 98765 43210", "Rahul Sharma", 1, System.currentTimeMillis() - 42 * 60_000, 245, null, null),
            Recent(2, "+91 91234 56789", "Papa", 2, System.currentTimeMillis() - 5 * 3600_000, 0, null, null),
        ) else emptyList()
        settle()
        rule.onNodeWithTag("drawer_open", useUnmergedTree = true).performTouchInput { click(center) }
        settle()
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).performClick()
        settle()
    }

    @Test
    fun shareSendsCsvFileAttachmentWithReadGrantViaChooser() {
        boot(withRecents = true)
        rule.onNodeWithTag("hisab_csv_share", useUnmergedTree = true).performClick()
        settle(600)
        val chooser = Shadows.shadowOf(rule.activity).nextStartedActivity
        assert(chooser != null && chooser.action == android.content.Intent.ACTION_CHOOSER) { "chooser expected, got $chooser" }
        val inner: android.content.Intent? = chooser!!.getParcelableExtra(android.content.Intent.EXTRA_INTENT)
        assert(inner != null && inner.action == android.content.Intent.ACTION_SEND) { "send intent expected, got $inner" }
        assert(inner!!.type == "text/csv") { inner.type ?: "no type" }
        val uri: android.net.Uri? = inner.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
        assert(uri != null && uri.scheme == "content") { "content uri expected, got $uri" }
        assert(inner.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) { "read grant missing" }
        val f = java.io.File(ApplicationProvider.getApplicationContext<LdApp>().cacheDir, "hisab_calls.csv")
        assert(f.exists() && f.readText().startsWith("date,time,name,number,type,duration_sec\n")) { "csv file missing/bad" }
        assert(f.readText().contains("\t+91 98765 43210")) { "formula-prefix missing in exported file" }
    }

    @Test
    fun emptyWindowShowsHonestEmptyStateAndNoShareButton() {
        boot(withRecents = false)
        rule.onNodeWithTag("stats_empty", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Koi call history nahi", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onAllNodesWithTag("hisab_csv_share", useUnmergedTree = true).assertCountEquals(0)
        assert(Shadows.shadowOf(rule.activity).nextStartedActivity == null) { "empty window pe share nahi hona chahiye" }
    }

    @Test
    fun missingPermissionShowsAccessStateAndNoShareButton() {
        boot(withRecents = false, grantPermission = false)
        rule.onNodeWithTag("stats_empty", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("Call log ka access nahi", substring = true, useUnmergedTree = true).assertIsDisplayed()
        rule.onAllNodesWithTag("hisab_csv_share", useUnmergedTree = true).assertCountEquals(0)
    }
}
