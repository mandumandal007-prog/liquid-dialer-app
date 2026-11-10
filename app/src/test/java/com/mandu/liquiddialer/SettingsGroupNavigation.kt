package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule

internal fun openSettingsGroup(rule:AndroidComposeTestRule<ActivityScenarioRule<MainActivity>,MainActivity>,title:String) {
    rule.mainClock.autoAdvance=true
    rule.onAllNodesWithText(title,useUnmergedTree=true).onLast().performScrollTo().assertIsDisplayed().performClick()
    rule.waitForIdle()
    rule.mainClock.autoAdvance=false
    rule.mainClock.advanceTimeBy(700);rule.waitForIdle()
}
