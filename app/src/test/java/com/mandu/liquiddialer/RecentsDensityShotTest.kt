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
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true
            p[K.profileName] = "Mandu Mandal"
            p[K.theme] = "daylight"
            p[K.dialerScale] = 1f
        }
        com.mandu.liquiddialer.telecom.CallManager.calls.value = emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        Unit
    }

    private fun visibleRows(): Int =
        rule.onAllNodesWithText("Zed", substring = true).fetchSemanticsNodes()
            .count { it.layoutInfo.isPlaced }

    @Test
    fun density() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        settle(2500)
        val act = rule.activity
        act.state.showOnboarding = false
        act.state.isDefault = true
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        act.state.contacts = emptyList()
        // 3-line cards (name + type/time + duration), worst case height like user's screenshot
        act.state.recents = (3..14).map {
            Recent(it.toLong(), "+91 90000 000$it", "Zed $it", 2, System.currentTimeMillis() - it * 3_600_000L, 95, null, null)
        }
        settle()

        val defCount = visibleRows()
        saveShot(rule, "20-recents-density-default")

        ctx.ldStore.edit { it[K.recentsCompact] = true }
        // LdApp collector runs on Dispatchers.Default: wait until the value actually lands
        var waited = 0
        while (!LdApp.settings.value.recentsCompact && waited < 100) { Thread.sleep(50); waited++ }
        settle()
        val compactCount = visibleRows()
        saveShot(rule, "20-recents-density-compact")

        println("DENSITY default=$defCount compact=$compactCount")
        assertTrue("compact mode me zyada rows dikhni chahiye ($defCount -> $compactCount)", compactCount > defCount)
        Unit
    }
}
