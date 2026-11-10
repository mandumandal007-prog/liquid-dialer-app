package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule

internal fun openSettingsGroup(rule:AndroidComposeTestRule<ActivityScenarioRule<MainActivity>,MainActivity>,title:String) {
    rule.mainClock.autoAdvance=true
    val row=rule.onAllNodesWithText(title,useUnmergedTree=true).onLast()
    row.performScrollTo().assertIsDisplayed()
    // Only scrolling needs an advancing clock. Freeze before destination animations start.
    rule.mainClock.autoAdvance=false
    row.performClick()
    rule.mainClock.advanceTimeBy(700);rule.waitForIdle()
}
