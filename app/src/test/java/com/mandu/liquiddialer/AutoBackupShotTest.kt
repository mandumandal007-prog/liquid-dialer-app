package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.AutoBackup
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.ui.BackupPage
import com.mandu.liquiddialer.ui.LdThemeHost
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Auto-backup card ke pixel shots: Poora-backup page day + night-large (switch on, status line ke saath). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AutoBackupShotTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before fun clean() {
        AutoBackup.dir(ctx).deleteRecursively()
    }

    private fun host(theme: String, font: Float) {
        runBlocking {
            ctx.ldStore.edit {
                it.clear()
                it[K.onboarded] = true; it[K.theme] = theme; it[K.fontScale] = font
                it[K.autoBackup] = true; it[K.lastAutoBackupAt] = 1730000000000L
            }
        }
        AutoBackup.runNow(ctx, 1730000000000L)
        rule.waitUntil(15000) { LdApp.settings.value.theme == theme && LdApp.settings.value.fontScale == font && LdApp.settings.value.autoBackup }
        rule.runOnUiThread { rule.activity.setContent { LdThemeHost { BackupPage({}) } } }
        settle()
        rule.waitUntil(15000) { rule.onAllNodesWithTag("autobackup_status", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        settle(600)
    }

    @Test fun shotDay() {
        host("daylight", 1f)
        saveShot(rule, "291-autobackup-day")
    }

    @Test fun shotNightLarge() {
        host("midnight", 1.3f)
        saveShot(rule, "291-autobackup-night-large")
    }
}
