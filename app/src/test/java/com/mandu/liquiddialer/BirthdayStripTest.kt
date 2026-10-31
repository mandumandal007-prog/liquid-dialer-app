package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.ui.BirthdayStrip
import com.mandu.liquiddialer.ui.LdThemeHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
        assertEquals(120, Repo.daysUntilBirthday("2024-02-29", today)) // 29 Feb -> 28 Feb 2027... non-leap handling
        assertNull(Repo.daysUntilBirthday("kuch bhi", today))
        assertNull(Repo.daysUntilBirthday("", today))
        assertNull(Repo.daysUntilBirthday("2024-13-40", today))
    }
}

/** Birthday strip UI: fixture data se render + pixels (provider ki zaroorat nahi). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BirthdayStripShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun stripRenders() {
        // activity ka apna content set ho chuka hota hai, isliye sirf strip alag se dikhane ke liye
        // decor content swap karte hain (production code touch nahi hota)
        val act = rule.activity
        val cv = ComposeView(act)
        act.runOnUiThread {
            cv.setContent {
                LdThemeHost {
                    BirthdayStrip(listOf(
                        Repo.Birthday("Rahul Sharma", "+91 98765 43210", 0),
                        Repo.Birthday("Papa", "+91 91234 56789", 1),
                        Repo.Birthday("Anjali Verma", "+91 99887 76655", 4),
                    )) {}
                }
            }
            act.setContentView(cv)
        }
        rule.waitForIdle()
        Thread.sleep(400)
        rule.waitForIdle()
        rule.onNodeWithText("BIRTHDAYS", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Rahul Sharma ka birthday", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("aaj hai!", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("kal hai", substring = true, useUnmergedTree = true).assertExists()
        rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).assertCountEquals(3)
        // strip wrap-height honi chahiye (LazyColumn item hai): aakhri row screen ke top quarter me hi khatam
        val rows = rule.onAllNodesWithTag("birthday_row", useUnmergedTree = true).fetchSemanticsNodes()
        val lastBottom = rows.last().boundsInRoot.bottom
        val rootH = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        org.junit.Assert.assertTrue("strip wrap-height nahi hai: lastBottom=$lastBottom rootH=$rootH", lastBottom < rootH * 0.25f)
        saveShot(rule, "44-birthday-strip")
    }
}
