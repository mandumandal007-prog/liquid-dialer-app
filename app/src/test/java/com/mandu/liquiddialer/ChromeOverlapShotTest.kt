package com.mandu.liquiddialer

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Bottom chrome overlap regression: bottom_bar / dialer_fab KABHI bhi row ke
 *  call buttons (call_mini) ko cover nahi karte - normal aur fontScale 1.3, dono tabs.
 *  Pixel shots: 57-chrome-contacts / 57b-chrome-recents / 57c-chrome-fontscale. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChromeOverlapShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @After fun tearDown() = runBlocking {
        ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { it[K.fontScale] = 1f }
        Unit
    }

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun seed(fs: Float) {
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(
            android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_CALL_LOG)
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
                p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f; p[K.fontScale] = fs
            }
        }
        rule.waitForIdle()
        repeat(15) {
            if (rule.onAllNodesWithText("Baad me", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
                rule.onNodeWithText("Baad me", useUnmergedTree = true).performClick(); settle(1200); return@repeat
            }
            if (rule.onAllNodesWithTag("drawer_open", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return@repeat
            rule.mainClock.advanceTimeBy(400); rule.waitForIdle()
        }
        settle(1500)
        val names = listOf("Aman", "Bablu", "Chotu", "Deepak", "Esha", "Farhan", "Golu", "Hari", "Ishaan", "Jatin", "Karan", "Lalu")
        val fake = names.mapIndexed { i, n -> Contact(i + 1L, "lk$i", n, n, "90000000%02d".format(i), "Mobile", false, null) }
        val rec = names.take(8).mapIndexed { i, n -> Recent(i + 1L, "90000000%02d".format(i), n, 1, 1000L + i, 30_000L, null, null) }
        rule.runOnUiThread { rule.activity.state.contacts = fake; rule.activity.state.recents = rec }
        settle(1500)
    }

    private fun assertNoChromeOverlap(where: String) {
        val fab = rule.onAllNodesWithTag("dialer_fab", useUnmergedTree = true).fetchSemanticsNodes()
        val bar = rule.onAllNodesWithTag("bottom_bar", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("$where: dialer_fab render hona chahiye", fab.isNotEmpty())
        assertTrue("$where: bottom_bar render hona chahiye", bar.isNotEmpty())
        val fb = fab[0].boundsInRoot; val bb = bar[0].boundsInRoot
        assertTrue("$where: fab aur nav pill ek dusre ko overlap nahi kare (fab=$fb bar=$bb)", !fb.overlaps(bb))
        val calls = rule.onAllNodesWithTag("call_mini", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("$where: call buttons composed hone chahiye", calls.isNotEmpty())
        calls.forEachIndexed { i, n ->
            val b = n.boundsInRoot
            assertTrue("$where: call button #$i FAB se overlap nahi (btn=$b fab=$fb)", !b.overlaps(fb))
            assertTrue("$where: call button #$i bottom_bar se overlap nahi (btn=$b bar=$bb)", !b.overlaps(bb))
        }
    }

    @Test
    fun contactsTabChromeClear() {
        seed(1f)
        rule.runOnUiThread { rule.activity.state.tab = "contacts" }
        settle(1800)
        assertNoChromeOverlap("contacts")
        saveShot(rule, "57-chrome-contacts")
    }

    @Test
    fun recentsTabChromeClear() {
        seed(1f)
        rule.runOnUiThread { rule.activity.state.tab = "recents" }
        settle(1800)
        assertNoChromeOverlap("recents")
        saveShot(rule, "57b-chrome-recents")
    }

    @Test
    fun fontScale13ChromeClear() {
        seed(1.3f)
        rule.runOnUiThread { rule.activity.state.tab = "contacts" }
        settle(1800)
        assertNoChromeOverlap("contacts@fs1.3")
        saveShot(rule, "57c-chrome-fontscale")
    }
}
