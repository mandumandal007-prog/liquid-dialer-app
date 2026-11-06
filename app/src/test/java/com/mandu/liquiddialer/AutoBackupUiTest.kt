package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.AutoBackup
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.ui.BackupPage
import com.mandu.liquiddialer.ui.LdThemeHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Poora-backup page ka auto-backup card: switch dikhta hai, toggle persist hota hai, status on pe dikhta hai. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AutoBackupUiTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    private fun settle() { rule.mainClock.advanceTimeBy(800); rule.waitForIdle() }

    @Before fun clean() {
        AutoBackup.dir(ctx).deleteRecursively()
        runBlocking { ctx.ldStore.edit { it.clear() } }
    }

    private fun show(on: Boolean, lastAt: Long = 0L) {
        rule.mainClock.autoAdvance = false
        runBlocking {
            ctx.ldStore.edit {
                it[K.onboarded] = true; it[K.theme] = "daylight"; it[K.fontScale] = 1f
                it[K.autoBackup] = on
                if (lastAt > 0) it[K.lastAutoBackupAt] = lastAt
            }
        }
        rule.waitUntil(30000) { LdApp.settings.value.autoBackup == on }
        rule.runOnUiThread { rule.activity.setContent { LdThemeHost { BackupPage({}) } } }
        settle()
        rule.waitUntil(30000) { rule.onAllNodesWithTag("autobackup_card", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        settle()
    }

    @Test fun cardVisibleStatusHiddenWhenOff() {
        show(false)
        rule.onNodeWithTag("autobackup_status", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun statusShownWhenOnWithLastTime() {
        show(true, 1730000000000L)
        rule.onNodeWithTag("autobackup_status", useUnmergedTree = true).performScrollTo().assertExists()
    }

    @Test fun switchTogglePersistsSetting() {
        show(false)
        rule.onNode(hasAnyAncestor(hasTestTag("autobackup_card")) and hasClickAction(), useUnmergedTree = true)
            .performScrollTo().performClick()
        settle()
        rule.waitUntil(15000) { runBlocking { LdApp.instance.repo.flow.first().autoBackup } }
    }
}
