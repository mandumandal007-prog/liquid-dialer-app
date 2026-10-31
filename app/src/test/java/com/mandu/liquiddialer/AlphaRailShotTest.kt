package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Contacts A-Z rail: seeded contacts par rail dikhta hai, letter tap par list us contact tak jump karti hai.
 *  Pixel shot: 56-alpha-rail. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlphaRailShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Test
    fun railShowsAndJumps() {
        // permission activity launch se PEHLE (loader empty contacts na bhare dobara)
        Shadows.shadowOf(ApplicationProvider.getApplicationContext<LdApp>()).grantPermissions(
            android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.READ_CALL_LOG)
        runBlocking {
            ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
                p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f
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

        // fake contacts seed (12 contacts, 8 alag letters) - in-memory UI state only
        val names = listOf("Aman", "Arjun", "Bablu", "Deepak", "Golu", "Karan", "Mandu", "Nikhil", "Rahul", "Sonu", "Vikash", "Zoya")
        val fake = names.mapIndexed { i, n -> Contact(i + 1L, "lk$i", n, n, "90000000%02d".format(i), "Mobile", false, null) }
        rule.runOnUiThread { rule.activity.state.contacts = fake }
        rule.runOnUiThread { rule.activity.state.tab = "contacts" }
        settle(1800)

        // rail dikhna chahiye
        val rail = rule.onAllNodesWithTag("alpha_rail", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("alpha rail render hona chahiye", rail.isNotEmpty())
        saveShot(rule, "56-alpha-rail")

        // "V" tap -> Vikash viewport me aana chahiye (jump ka proof)
        rule.onNodeWithText("V", useUnmergedTree = true).performClick()
        settle(1500)
        val v = rule.onAllNodesWithText("Vikash", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("jump ke baad Vikash composed hona chahiye", v.isNotEmpty())
        val h = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        val b = v[0].boundsInRoot
        assertTrue("Vikash viewport me visible hona chahiye (jump hua)", b.bottom > 0 && b.top < h)
    }
}
