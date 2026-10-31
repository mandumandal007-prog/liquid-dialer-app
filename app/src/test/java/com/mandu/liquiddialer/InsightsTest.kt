package com.mandu.liquiddialer

import android.provider.CallLog
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Insights math: pure, anchored time (midnight roll se CI break na ho). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InsightsMathTest {
    private val base = java.time.LocalDate.now().atTime(13, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun r(id: Long, num: String, name: String?, type: Int, ageHrs: Long, dur: Long) =
        Recent(id, num, name, type, base - ageHrs * 3_600_000, dur, null, null)

    @Test
    fun emptyIsZero() {
        val i = Repo.insights(emptyList())
        assertEquals(0, i.calls); assertNull(i.topName); assertNull(i.busiestLabel)
    }

    @Test
    fun mixedWindow() {
        val list = listOf(
            r(1, "+91 11111 00001", "Rahul", CallLog.Calls.OUTGOING_TYPE, 2, 300),
            r(2, "+91 11111 00001", "Rahul", CallLog.Calls.INCOMING_TYPE, 5, 120),
            r(3, "+91 22222 00002", "Priya", CallLog.Calls.MISSED_TYPE, 7, 0),
            r(4, "+91 22222 00002", "Priya", CallLog.Calls.BLOCKED_TYPE, 8, 0), // blocked MISSED nahi
            r(5, "+91 11111 00001", "Rahul", CallLog.Calls.OUTGOING_TYPE, 26, 60),
            r(6, "+91 33333 00003", null, CallLog.Calls.OUTGOING_TYPE, 50, 900), // sabse lambi, bina naam
        )
        val i = Repo.insights(list)
        assertEquals(6, i.calls)
        assertEquals(300 + 120 + 60 + 900L, i.talkSecs)   // sirf incoming+outgoing durations
        assertEquals(1, i.missed)                          // sirf sachmuch MISSED_TYPE
        assertEquals("Rahul", i.topName); assertEquals(3, i.topCount)
        assertEquals(900L, i.longestSecs); assertEquals(Repo.pretty("+91 33333 00003"), i.longestName)
        assertEquals(4, i.busiestCount)                    // aaj 4 (2,5,7,8 hrs pehle), parso 2
    }
}

/** Insights sheet: real home se "Hisab" tap -> sheet + window label + pixels. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InsightsSheetShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    @Before
    fun seed() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        ctx.ldStore.edit { p -> p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight" }
        Unit
    }

    @Test
    fun sheetShowsHonestWindow() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        act.state.showOnboarding = false
        act.state.isDefault = true
        val noon = java.time.LocalDate.now().atTime(13, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        act.state.recents = listOf(
            Recent(1, "+91 98765 43210", "Rahul Sharma", CallLog.Calls.OUTGOING_TYPE, noon - 2 * 3_600_000, 300, null, null),
            Recent(2, "+91 98765 43210", "Rahul Sharma", CallLog.Calls.INCOMING_TYPE, noon - 5 * 3_600_000, 120, null, null),
            Recent(3, "+91 91234 56789", "Priya Verma", CallLog.Calls.MISSED_TYPE, noon - 7 * 3_600_000, 0, null, null),
        )
        settle()
        rule.onNodeWithTag("insights_open", useUnmergedTree = true).performClick()
        settle()
        rule.onNodeWithText("Call insights", substring = true, useUnmergedTree = true).assertExists()
        // window label: 3 calls < limit 300 -> sirf loaded window ka dawa, poori coverage ka nahi
        rule.onNodeWithText("Load hui 3 calls ka hisaab", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("sabse zyada baat (2 calls)", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("missed calls", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "47-insights-sheet")
    }

    /** REGRESSION: Clear confirm sheet pehle RecentsScreen (haze source ke ANDAR) me tha -> tap pe draw crash.
     *  Ab MainScreen overlay level pe hai; sheet draw honi chahiye. */
    @Test
    fun clearConfirmSheetDraws() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        act.state.showOnboarding = false
        act.state.isDefault = true
        val noon = java.time.LocalDate.now().atTime(13, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        act.state.recents = listOf(
            Recent(1, "+91 98765 43210", "Rahul Sharma", CallLog.Calls.OUTGOING_TYPE, noon - 2 * 3_600_000, 300, null, null),
        )
        settle()
        rule.onNodeWithText("Clear", substring = true, useUnmergedTree = true).performClick()
        settle()
        rule.onNodeWithText("Saari call history delete karein?", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Rehne do", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "48-confirm-clear-sheet")
    }
}
