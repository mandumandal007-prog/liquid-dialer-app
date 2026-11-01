package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Recents card density: default (reduced) vs Compact recents mode. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsDensityShotTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 1200) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        // har test clean slate pe: cross-test leak na ho (datastore + fontScale dono reset)
        RuntimeEnvironment.setFontScale(1.0f)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true
            p[K.profileName] = "Mandu Mandal"
            p[K.theme] = "daylight"
            p[K.dialerScale] = 1f
            p[K.recentsCompact] = false
            p[K.showSimInRecents] = true
        }
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        Unit
    }

    private fun visibleRows() =
        rule.onAllNodesWithText("Zed", substring = true).fetchSemanticsNodes()
            .count { it.layoutInfo.isPlaced }

    /** rows render hone ka wait (async recomposition/reload ke baad), warna shot jaldi uth jata hai */
    private fun waitRows(min: Int = 1, timeoutMs: Long = 6000): Int {
        val t0 = System.currentTimeMillis()
        var n = visibleRows()
        while (n < min && System.currentTimeMillis() - t0 < timeoutMs) {
            rule.mainClock.advanceTimeBy(300); rule.waitForIdle(); Thread.sleep(60)
            n = visibleRows()
        }
        return n
    }

    private fun waitFlag(flag: () -> Boolean) {
        var waited = 0
        while (!flag() && waited < 100) { Thread.sleep(50); waited++ }
    }

    @Test
    fun density() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.isDefault = true
        act.state.contacts = emptyList()
        // 2-line cards (name + type/time/duration/SIM merged line)
        // noon-anchor: now-offset se raat 12 baje ke baad rows do din me bat jaate the
        // (extra day header + chhupi aaj-stats strip) aur compact-count assertion tut jaata tha
        val noon = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 12); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        act.state.recents = (3..14).map {
            Recent(it.toLong(), "+91 90000 000$it", "Zed $it", 2, noon - it * 45 * 60_000L, 95, null, null)
        }
        settle()
        val defCount = waitRows()
        val defTops = rule.onAllNodesWithTag("call_mini", useUnmergedTree = true).fetchSemanticsNodes()
            .map { it.boundsInRoot.top }.sorted()
        assertTrue("kam se kam 2 rows chahiye pitch naapne ke liye", defTops.size >= 2)
        val defPitch = defTops[1] - defTops[0]
        saveShot(rule, "20-recents-density-default")

        ctx.ldStore.edit { it[K.recentsCompact] = true }
        // LdApp collector runs on Dispatchers.Default: wait until the value actually lands
        waitFlag { LdApp.settings.value.recentsCompact }
        settle()
        val compactCount = waitRows(defCount)
        val compTops = rule.onAllNodesWithTag("call_mini", useUnmergedTree = true).fetchSemanticsNodes()
            .map { it.boundsInRoot.top }.sorted()
        val compPitch = compTops[1] - compTops[0]
        saveShot(rule, "20-recents-density-compact")

        println("DENSITY default=$defCount compact=$compactCount pitch=$defPitch->$compPitch")
        // Compact ka asli guarantee = chhoti row pitch (vertical padding 6dp -> 3dp).
        // Row COUNT viewport boundary par luck hai (full-height list me dono 8 compose ho
        // sakte hain), isliye count ab sirf non-decreasing hona chahiye.
        assertTrue("compact rows tight honi chahiye (pitch $defPitch -> $compPitch)", compPitch < defPitch)
        assertTrue("compact mode me kam rows nahi dikhni chahiye ($defCount -> $compactCount)", compactCount >= defCount)
        Unit
    }

    /** Worst case merged-line check: dual-SIM labels, lambe naam, bada font scale (1.25).
     *  Pixels 21-*.png inspect karne hain: SIM label/duration truncate nahi hona chahiye. */
    @Test
    fun densityDualSimLongNamesFontScale() = runBlocking {
        RuntimeEnvironment.setFontScale(1.25f)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.isDefault = true
        act.state.contacts = emptyList()
        val cn = android.content.ComponentName("com.android.server.telecom", ".TelecomService")
        act.state.sims = listOf(
            com.mandu.liquiddialer.data.SimInfo(android.telecom.PhoneAccountHandle(cn, "sim0"), "Jio", 1),
            com.mandu.liquiddialer.data.SimInfo(android.telecom.PhoneAccountHandle(cn, "sim1"), "Airtel", 2)
        )
        val noonH = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 12); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        act.state.recents = (3..14).map {
            Recent(it.toLong(), "+91 90000 000$it", "Zedd Singh Chauhan Verylongname $it", 2,
                noonH - it * 45 * 60_000L, 95, if (it % 2 == 0) "sim0" else "sim1", null)
        }
        settle()
        val defCount = waitRows()
        saveShot(rule, "21-recents-density-hard-default")

        ctx.ldStore.edit { it[K.recentsCompact] = true }
        waitFlag { LdApp.settings.value.recentsCompact }
        settle()
        val compactCount = waitRows(defCount)
        saveShot(rule, "21-recents-density-hard-compact")

        println("DENSITY-HARD default=$defCount compact=$compactCount")
        assertTrue("hard mode me compact kam rows nahi dikhana chahiye ($defCount -> $compactCount)", compactCount >= defCount)
        Unit
    }

    /** User-like DARK fixture (midnight theme): stats strip + Aaj/Kal day groups, scroll 0.
     *  05:44 user feedback ke baad density verification dark mode me bhi chahiye thi.
     *  Full-visible rows (call buttons pura viewport ke andar, chrome se no overlap) print hoti hain
     *  taaki before/after layout compare exact ho, andaj se nahi. */
    @Test
    fun darkUserLikeScroll0() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { it[K.theme] = "midnight" }
        waitFlag { LdApp.settings.value.theme == "midnight" }
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.isDefault = true
        act.state.contacts = emptyList()
        val noon = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 12); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val day = 24 * 60 * 60_000L
        act.state.recents = listOf(
            Recent(1L, "+91 90000 00001", "Railways", 2, noon - 5 * 60_000L, 60, null, null),
            Recent(2L, "+91 90000 00002", "Father", 1, noon - 40 * 60_000L, 95, null, null),
            Recent(3L, "+91 90000 00003", "Railways", 2, noon - 90 * 60_000L, 30, null, null),
            Recent(4L, "+91 90000 00004", "Jio Balance Check", 2, noon - 3 * 60 * 60_000L, 0, null, null),
            Recent(5L, "+91 90000 00005", "Father", 1, noon - day, 145, null, null),
            Recent(6L, "+91 90000 00006", "Railways", 2, noon - day - 2 * 60 * 60_000L, 75, null, null),
            Recent(7L, "+91 90000 00007", "Jio Balance Check", 2, noon - day - 5 * 60 * 60_000L, 0, null, null),
            Recent(8L, "+91 90000 00008", "Mummy", 1, noon - 2 * day, 200, null, null)
        )
        settle()
        waitRows(1)
        settle(600)
        saveShot(rule, "22-dark-userlike-scroll0")
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val chrome = (rule.onAllNodesWithTag("bottom_bar", useUnmergedTree = true).fetchSemanticsNodes() +
            rule.onAllNodesWithTag("dialer_fab", useUnmergedTree = true).fetchSemanticsNodes() +
            rule.onAllNodesWithTag("top_bar", useUnmergedTree = true).fetchSemanticsNodes()).map { it.boundsInRoot }
        // Bounds-based regression floor, NOT a pixel-visibility proof. Rows ab floating
        // bottom bar ke peeche BHI dikhti hain (full-viewport list, user request 06:17) -
        // isliye chrome-exclusion nahi, sirf viewport ke andar count. Reachability alag se
        // neeche scroll-clear check se guarded hai.
        val buttonsInViewport = rule.onAllNodesWithTag("call_mini", useUnmergedTree = true).fetchSemanticsNodes()
            .count { n -> val b = n.boundsInRoot
                b.top >= 0 && b.bottom <= root.height }
        println("DARK-USERLIKE buttonsInViewport=$buttonsInViewport")
        assertTrue("dark user-like fixture: density regressed (buttonsInViewport=$buttonsInViewport < 6)", buttonsInViewport >= 6)
        // Reachability: list ke end tak scroll karne par last call button bottom chrome se clear.
        rule.onAllNodesWithTag("main_list", useUnmergedTree = true)[0].performTouchInput {
            repeat(10) { swipeUp(durationMillis = 200); advanceEventTime(100) }
        }
        settle(1000)
        val chromeAfter = (rule.onAllNodesWithTag("bottom_bar", useUnmergedTree = true).fetchSemanticsNodes() +
            rule.onAllNodesWithTag("dialer_fab", useUnmergedTree = true).fetchSemanticsNodes()).map { it.boundsInRoot }
        val lastBtn = rule.onAllNodesWithTag("call_mini", useUnmergedTree = true).fetchSemanticsNodes()
            .maxByOrNull { it.boundsInRoot.top }
        assertTrue("scroll-end par last call button hona chahiye", lastBtn != null)
        val lb = lastBtn!!.boundsInRoot
        assertTrue("scroll-end last call button bottom chrome se clear nahi (btn=$lb chrome=$chromeAfter)",
            chromeAfter.all { c -> !lb.overlaps(c) })
        Unit
    }
}
