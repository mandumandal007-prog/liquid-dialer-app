package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.platform.ComposeView
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.CallReminders
import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.ui.BirthdayStrip
import com.mandu.liquiddialer.ui.LdThemeHost
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Birthday date math: pure, device-free. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BirthdayMathTest {
    private val today = java.time.LocalDate.of(2026, 10, 31)

    @Test
    fun formatsAndWindows() {
        assertEquals(0, Repo.daysUntilBirthday("1990-10-31", today))   // aaj
        assertEquals(1, Repo.daysUntilBirthday("1985-11-01", today))   // kal
        assertEquals(4, Repo.daysUntilBirthday("--11-04", today))      // --MM-dd format
        assertEquals(62, Repo.daysUntilBirthday("2000-01-01", today))  // saal wrap
        assertEquals(120, Repo.daysUntilBirthday("2024-02-29", today)) // 29 Feb -> 28 Feb (non-leap fallback)
        assertNull(Repo.daysUntilBirthday("kuch bhi", today))
        assertNull(Repo.daysUntilBirthday("", today))
        assertNull(Repo.daysUntilBirthday("2024-13-40", today))
    }
}

/** Birthday strip: isolated render + overflow expander + REAL integrated home shot. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BirthdayStripShotTest {
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

    /** Activity ka apna content already set hota hai; sirf strip alag dikhane ke liye decor swap (production touch nahi). */
    private fun swapToStrip(items: List<Repo.Birthday>) {
        val act = rule.activity
        val cv = ComposeView(act)
        act.runOnUiThread {
            cv.setContent { LdThemeHost { BirthdayStrip(items) {} } }
            act.setContentView(cv)
        }
        rule.waitForIdle()
        Thread.sleep(400)
        rule.waitForIdle()
    }

    private val three = listOf(
        Repo.Birthday("Rahul Sharma", "+91 98765 43210", 0),
        Repo.Birthday("Papa", "+91 91234 56789", 1),
        Repo.Birthday("Anjali Verma", "+91 99887 76655", 4),
    )

    @Test
    fun stripRenders() {
        swapToStrip(three)
        rule.onNodeWithText("BIRTHDAYS", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Rahul Sharma ka birthday", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("aaj hai!", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("kal hai", substring = true, useUnmergedTree = true).assertExists()
        rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).assertCountEquals(3)
        // wrap-height guard: strip LazyColumn item hai, poori screen nahi bhar sakti
        val rows = rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).fetchSemanticsNodes()
        val rootH = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        assertTrue("strip wrap-height nahi: bottom=${rows.last().boundsInRoot.bottom} rootH=$rootH", rows.last().boundsInRoot.bottom < rootH * 0.25f)
        saveShot(rule, "44-birthday-strip")
    }

    @Test
    fun stripOverflowExpands() {
        swapToStrip(three + listOf(
            Repo.Birthday("Neha Gupta", "+91 98000 11111", 5),
            Repo.Birthday("Arjun Mehta", "+91 97000 22222", 7),
        ))
        rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).assertCountEquals(3)
        rule.onNodeWithText("+2 aur dikho", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("birthdays_expand", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).assertCountEquals(5)
        rule.onNodeWithText("Kam dikho", substring = true, useUnmergedTree = true).assertExists()
        val rows = rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).fetchSemanticsNodes()
        val rootH = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        assertTrue("expanded strip wrap-height nahi: bottom=${rows.last().boundsInRoot.bottom} rootH=$rootH", rows.last().boundsInRoot.bottom < rootH * 0.3f)
        saveShot(rule, "44b-birthday-expanded")
    }

    /** ASLI home screen: reminders + birthdays + chips + recents sab ek saath, strip panel-size hi rahe. */
    @Test
    fun homeIntegrated() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        act.state.showOnboarding = false
        act.state.isDefault = true
        act.state.contacts = listOf(Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null))
        val noonToday = java.time.LocalDate.now().atTime(13, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        act.state.recents = listOf(Recent(1, "+91 90000 11111", "Vikram Singh", 1, noonToday - 42 * 60_000, 245, null, null))
        val tomorrow10 = java.time.LocalDate.now().plusDays(1).atTime(10, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        CallReminders.set(ctx, "+91 98765 43210", "Rahul Sharma", tomorrow10)
        act.state.birthdays = three
        settle()
        rule.onAllNodesWithTag("reminder_row", useUnmergedTree = true).assertCountEquals(1)
        rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).assertCountEquals(3)
        rule.onNodeWithText("Missed", useUnmergedTree = true).assertExists()  // filter chip
        rule.onNodeWithText("Vikram Singh", substring = true, useUnmergedTree = true).assertExists()  // recents row
        // geometry: strip chhota panel, recents row uske NEECHE bina scroll dikhti hai
        val bRows = rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).fetchSemanticsNodes()
        val stripBottom = bRows.last().boundsInRoot.bottom
        val rootH = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        val recentTop = rule.onAllNodesWithText("Vikram Singh", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().maxOf { it.boundsInRoot.top }
        assertTrue("birthday strip bahut bada panel: bottom=$stripBottom rootH=$rootH", stripBottom < rootH * 0.5f)
        assertTrue("recents row strip ke neeche nahi: recentTop=$recentTop stripBottom=$stripBottom", recentTop > stripBottom)
        saveShot(rule, "45-home-birthday-integrated")
        CallReminders.cancel(ctx, "+91 98765 43210")
    }
}

/** NORMAL phone viewport (360x800dp): panels fit karke Recents scroll pe milte hain - asli phone geometry. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BirthdayPhoneShotTest {
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
    fun homePhoneViewport() {
        settle(2500)
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val act = rule.activity
        Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS)
        act.state.showOnboarding = false
        act.state.isDefault = true
        act.state.contacts = listOf(Contact(11, "lk1", "Rahul Sharma", "", "+91 98765 43210", "MOBILE", true, null))
        val noonToday = java.time.LocalDate.now().atTime(13, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        act.state.recents = listOf(Recent(1, "+91 90000 11111", "Vikram Singh", 1, noonToday - 42 * 60_000, 245, null, null))
        val tomorrow10 = java.time.LocalDate.now().plusDays(1).atTime(10, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        CallReminders.set(ctx, "+91 98765 43210", "Rahul Sharma", tomorrow10)
        act.state.birthdays = listOf(
            Repo.Birthday("Rahul Sharma", "+91 98765 43210", 0),
            Repo.Birthday("Papa", "+91 91234 56789", 1),
            Repo.Birthday("Anjali Verma", "+91 99887 76655", 4),
        )
        settle()
        rule.onNodeWithText("BIRTHDAYS", substring = true, useUnmergedTree = true).assertExists()
        rule.onAllNodesWithTag("reminder_row", useUnmergedTree = true).assertCountEquals(1)
        val rows = rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("phone viewport pe koi birthday row compose hi nahi hui", rows.isNotEmpty())
        saveShot(rule, "46-home-phone-viewport")
        // Recents scroll pe reachable: SINGLE swipe (performScrollTo chain hang karta hai is harness me)
        rule.mainClock.autoAdvance = true
        rule.onRoot().performTouchInput { swipeUp() }
        settle(1500)
        rule.onNodeWithText("Vikram Singh", substring = true, useUnmergedTree = true).assertExists()
        saveShot(rule, "46b-home-phone-scrolled")
        CallReminders.cancel(ctx, "+91 98765 43210")
    }
}
