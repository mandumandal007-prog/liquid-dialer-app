package com.mandu.liquiddialer

import android.provider.CallLog
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

/** Hisab stats page ke pixels: dark/light/bade font + empty/partial coverage states.
 *  Fixture calendar-day anchored hai (midnight roll se CI break na ho). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatsPageShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun seedBase(theme: String, limit: Int? = null) = runBlocking {
        ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = theme; p[K.dialerScale] = 1f
            if (limit != null) p[K.recentLimit] = limit
        }
        com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        Unit
    }

    private fun at(dayOffset: Int, hour: Int, min: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DAY_OF_YEAR, -dayOffset); c.add(Calendar.HOUR_OF_DAY, hour); c.add(Calendar.MINUTE, min)
        return c.timeInMillis
    }

    private fun rec(id: Long, num: String, name: String?, type: Int, date: Long, dur: Long) =
        Recent(id, num, name, type, date, dur, null, null)

    /** Saatoon din covered: oldest entry 6 din pehle raat ko. */
    private fun fullFixture() = listOf(
        rec(1, "+91 98765 43210", "Rahul Sharma", CallLog.Calls.OUTGOING_TYPE, at(0, 9, 15), 300),
        rec(2, "+91 98765 43210", "Rahul Sharma", CallLog.Calls.OUTGOING_TYPE, at(0, 11, 40), 60),
        rec(3, "+91 91234 56789", "Priya Verma", CallLog.Calls.INCOMING_TYPE, at(0, 13, 5), 120),
        rec(4, "+91 91234 56789", "Priya Verma", CallLog.Calls.MISSED_TYPE, at(0, 15, 20), 0),
        rec(5, "+91 98765 43210", "Rahul Sharma", CallLog.Calls.INCOMING_TYPE, at(1, 10, 0), 240),
        rec(6, "+91 80000 12345", "Spam Offer", CallLog.Calls.REJECTED_TYPE, at(1, 14, 30), 0),
        rec(7, "+91 99887 66554", "Amit Singh", CallLog.Calls.OUTGOING_TYPE, at(2, 12, 0), 900),
        rec(8, "+91 14011 12233", "Telemarketer", CallLog.Calls.BLOCKED_TYPE, at(2, 16, 45), 0),
        rec(9, "+91 91234 56789", "Priya Verma", CallLog.Calls.MISSED_TYPE, at(3, 9, 30), 0),
        rec(10, "+91 91234 56789", "Priya Verma", CallLog.Calls.INCOMING_TYPE, at(4, 18, 10), 45),
        // day -5: jaan boojh ke koi call nahi (covered din = sach me 0)
        rec(11, "+91 98765 43210", "Rahul Sharma", CallLog.Calls.OUTGOING_TYPE, at(6, 23, 55), 30),
    )

    private fun bootAndSeed(recents: List<Recent>, grantLog: Boolean = true, loaded: Boolean = true, loadError: Boolean = false) {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        if (grantLog) Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        rule.activity.state.showOnboarding = false
        rule.activity.state.isDefault = true
        rule.activity.state.recents = recents
        rule.activity.state.recentsLoaded = loaded
        rule.activity.state.recentsError = loadError
        settle()
    }

    private fun openStatsViaDrawer() {
        rule.onNodeWithTag("drawer_open", useUnmergedTree = true).performTouchInput { click(center) }
        settle()
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).performScrollTo()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        settle()
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).assertIsDisplayed().performClick()
        settle()
    }
    private fun openStats() {
        rule.onNodeWithTag("insights_open", useUnmergedTree = true).performTouchInput { click(center) }
        settle()
        rule.onNodeWithTag("stats_coverage", useUnmergedTree = true).assertExists()
    }

    private fun scrollPageDown() {
        val sc = rule.onAllNodes(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNodes().lastOrNull()
        sc?.config?.getOrElseNullable(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { null }?.action?.invoke(0f, 1400f)
        rule.waitForIdle(); settle(600)
    }

    @Test
    fun darkFullCoverage() {
        seedBase("midnight")
        bootAndSeed(fullFixture())
        openStats()
        // coverage caption honest: 11 calls < limit 300
        rule.onNodeWithTag("stats_coverage", useUnmergedTree = true).assertTextContains("Load hui 11 calls ka hisaab", substring = true)
        rule.onNodeWithText("PICHHLE 7 DIN", substring = true, useUnmergedTree = true).assertExists() // SecTitle uppercase karta hai
        saveShot(rule, "50-stats-dark-top")
        scrollPageDown()
        // background recents screen bhi tree me hai - page card ke TAG scope me assert karo
        rule.onNode(hasText("Rejected (kaati gayi)") and hasAnyAncestor(hasTestTag("stats_breakdown")), useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Blocked") and hasAnyAncestor(hasTestTag("stats_breakdown")), useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Amit Singh") and hasAnyAncestor(hasTestTag("stats_longest")), useUnmergedTree = true).assertExists()
        // duration ka unit cue: "28:15 min" clock-time nahi lagna chahiye
        rule.onNode(hasText("28:15 min", substring = true) and hasAnyAncestor(hasTestTag("stats_breakdown")), useUnmergedTree = true).assertExists()
        saveShot(rule, "50-stats-dark-bottom")
    }

    @Test
    fun lightFullCoverage() {
        seedBase("daylight")
        bootAndSeed(fullFixture())
        openStats()
        saveShot(rule, "51-stats-light-top")
        scrollPageDown()
        saveShot(rule, "51-stats-light-bottom")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi", fontScale = 1.3f)
    fun darkLargeFont() {
        seedBase("midnight")
        bootAndSeed(fullFixture())
        openStats()
        saveShot(rule, "52-stats-bigfont-top")
        scrollPageDown()
        saveShot(rule, "52-stats-bigfont-bottom")
        // bade font pe page lamba hai: POORE scroll ka end reachable hona chahiye (semantics nahi, pixels)
        repeat(4) { scrollPageDown() }
        val rootH = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        val lb = rule.onNodeWithTag("stats_longest", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assert(lb.bottom <= rootH && lb.bottom > 0f) { "bigfont scroll END nahi pahuncha: longest bottom=$lb rootH=$rootH" }
        saveShot(rule, "52-stats-bigfont-end")
    }

    @Test
    fun darkEmpty() {
        seedBase("midnight")
        bootAndSeed(emptyList())
        rule.onNodeWithTag("insights_open", useUnmergedTree = true).assertDoesNotExist() // Hisab sirf recents hone pe
        // empty page drawer ke "Hisab (call stats)" se kholo
        rule.onNodeWithTag("drawer_open", useUnmergedTree = true).performTouchInput { click(center) }
        settle()
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).performScrollTo()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        settle()
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).assertIsDisplayed().performClick()
        settle()
        rule.onNodeWithTag("stats_empty", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Koi call history nahi", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "53-stats-empty")
    }

    @Test
    fun darkEmptyNoPermission() {
        // empty array != "history khaali": permission ke bina ALAG state dikhni chahiye
        seedBase("midnight")
        bootAndSeed(emptyList(), grantLog = false)
        rule.onNodeWithTag("drawer_open", useUnmergedTree = true).performTouchInput { click(center) }
        settle()
        rule.mainClock.autoAdvance = true
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).performScrollTo()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        settle()
        rule.onNodeWithText("Hisab (call stats)", substring = true, useUnmergedTree = true).assertIsDisplayed().performClick()
        settle()
        rule.onNodeWithTag("stats_empty", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Call log ka access nahi", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Access do", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "55-stats-empty-noperm")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
    fun darkStatsLoading() {
        seedBase("midnight")
        bootAndSeed(emptyList(), loaded = false)
        openStatsViaDrawer()
        rule.onNodeWithTag("stats_empty", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Hisab taiyaar ho raha hai", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Koi call history nahi", useUnmergedTree = true).assertDoesNotExist()
        saveShot(rule, "56-stats-loading")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
    fun darkStatsLoadError() {
        seedBase("midnight")
        bootAndSeed(emptyList(), loaded = true, loadError = true)
        openStatsViaDrawer()
        rule.onNodeWithTag("stats_empty", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Call log load nahi ho paya", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Koi call history nahi", useUnmergedTree = true).assertDoesNotExist()
        saveShot(rule, "57-stats-loaderror")
    }

    @Test
    fun darkPartialCoverage() {
        seedBase("midnight", limit = 100)
        // 100 entries AAJ hi: limit lagi, oldest din = aaj -> aaj partial, baaki 6 din window ke bahar
        val recents = (1..100).map { i ->
            rec(i.toLong(), "+91 90000 0$i", if (i % 7 == 0) "Roz Caller" else null,
                if (i % 5 == 0) CallLog.Calls.MISSED_TYPE else CallLog.Calls.INCOMING_TYPE, at(0, 8, 0) + i * 60_000L, (i % 4) * 45L)
        }
        bootAndSeed(recents)
        openStats()
        rule.onNodeWithTag("stats_coverage", useUnmergedTree = true).assertTextContains("Aakhri 100 calls ka hisaab (limit: 100)", substring = true)
        rule.onNodeWithTag("stats_notes", useUnmergedTree = true).assertTextContains("adhoora", substring = true)
        rule.onNodeWithTag("stats_notes", useUnmergedTree = true).assertTextContains("0 calls NAHI maana", substring = true)
        // Aaj card + breakdown pe bhi loaded/partial cue (sirf door-top caption nahi)
        rule.onNodeWithTag("stats_aaj_cue", useUnmergedTree = true).assertTextContains("adhoora", substring = true)
        rule.onNodeWithTag("stats_breakdown_cue", useUnmergedTree = true).assertTextContains("adhoora", substring = true)
        saveShot(rule, "54-stats-partial")
    }
}
