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
        saveShot(rule, "20-recents-density-default")

        ctx.ldStore.edit { it[K.recentsCompact] = true }
        // LdApp collector runs on Dispatchers.Default: wait until the value actually lands
        waitFlag { LdApp.settings.value.recentsCompact }
        settle()
        val compactCount = waitRows(defCount)
        saveShot(rule, "20-recents-density-compact")

        println("DENSITY default=$defCount compact=$compactCount")
        assertTrue("compact mode me zyada rows dikhni chahiye ($defCount -> $compactCount)", compactCount > defCount)
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
}
