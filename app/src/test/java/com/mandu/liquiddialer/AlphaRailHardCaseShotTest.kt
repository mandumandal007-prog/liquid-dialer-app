package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
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

/** Worst case: 26 Latin + 5 Devanagari + '#' = 32 rail letters, 360x800, fontScale 1.3.
 *  Rail ke saare letters placed/visible hone chahiye (zero/clip nahi), aur first/last
 *  letter taps EXACT jump karein (slot mapping). Pixel shots: 56b-alpha-rail-full / 56c-alpha-rail-fontscale. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlphaRailHardCaseShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val railTag = hasTestTag("alpha_rail")
    private val inRail = hasAnyAncestor(railTag)
    private val letters = ('A'..'Z').map { it.toString() } + listOf("अ","क","म","श","ह","#")

    @After fun tearDown() = runBlocking {
        ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { it[K.fontScale] = 1f }
        Unit
    }

    private fun settle(ms: Long = 800) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun seedAndOpen(fs: Float) {
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
        val names = ('A'..'Z').map { "${it}kumar" } + listOf("अजय", "किशन", "मम्मी", "श्याम", "हरी", "1800flowers")
        val fake = names.mapIndexed { i, n -> Contact(i + 1L, "lk$i", n, n, "90000000%02d".format(i), "Mobile", false, null) }
        rule.runOnUiThread { rule.activity.state.contacts = fake }
        rule.runOnUiThread { rule.activity.state.tab = "contacts" }
        settle(1800)
    }

    private fun assertAllLettersPlaced(rb: androidx.compose.ui.geometry.Rect) {
        letters.forEach { L ->
            val n = rule.onAllNodes(hasText(L) and inRail, useUnmergedTree = true).fetchSemanticsNodes()
            assertTrue("rail letter $L render hona chahiye", n.isNotEmpty())
            val b = n[0].boundsInRoot
            assertTrue("rail letter $L clip/zero nahi hona chahiye (b=$b, rail=$rb)",
                b.height > 0 && b.top >= rb.top - 1 && b.bottom <= rb.bottom + 1)
        }
    }

    @Test
    fun fullAlphabetFitsAndTapsMap() {
        seedAndOpen(1f)
        val rail = rule.onAllNodesWithTag("alpha_rail", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("32-letter rail render hona chahiye", rail.isNotEmpty())
        val rootH = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        val rb = rail[0].boundsInRoot
        assertTrue("rail top clip nahi honi chahiye (top=${rb.top})", rb.top >= 0)
        assertTrue("rail bottom clip nahi honi chahiye (bottom=${rb.bottom} <= $rootH)", rb.bottom <= rootH)
        assertAllLettersPlaced(rb)
        saveShot(rule, "56b-alpha-rail-full")

        rule.onNode(hasText("#") and inRail, useUnmergedTree = true).performClick()
        settle(1500)
        val t = rule.onAllNodesWithText("1800flowers", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("# tap par aakhri contact composed hona chahiye", t.isNotEmpty())
        val tb = t[0].boundsInRoot
        assertTrue("# tap par aakhri contact viewport me (jump sahi)", tb.bottom > 0 && tb.top < rootH)

        rule.onNode(hasText("A") and inRail, useUnmergedTree = true).performClick()
        settle(1500)
        val a = rule.onAllNodesWithText("Akumar", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("A tap par Akumar viewport me", a.isNotEmpty() && a[0].boundsInRoot.let { it.bottom > 0 && it.top < rootH })
    }

    @Test
    fun fontScale13StillFits() {
        seedAndOpen(1.3f)
        val rail = rule.onAllNodesWithTag("alpha_rail", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("fontScale 1.3 pe rail render hona chahiye", rail.isNotEmpty())
        val rootH = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        val rb = rail[0].boundsInRoot
        saveShot(rule, "56c-alpha-rail-fontscale")
        assertTrue("fontScale 1.3 pe rail top clip nahi (top=${rb.top})", rb.top >= 0)
        assertTrue("fontScale 1.3 pe rail bottom clip nahi (bottom=${rb.bottom} <= $rootH)", rb.bottom <= rootH)
        assertAllLettersPlaced(rb)
    }
}
