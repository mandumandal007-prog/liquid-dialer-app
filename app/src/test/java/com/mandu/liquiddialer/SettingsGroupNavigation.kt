package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule

internal fun openSettingsGroup(rule:AndroidComposeTestRule<ActivityScenarioRule<MainActivity>,MainActivity>,title:String) {
    var visible=false
    repeat(30) {
        val node=rule.onAllNodesWithText(title,useUnmergedTree=true).fetchSemanticsNodes().lastOrNull()
        if(node!=null && node.boundsInRoot.top>=110f && node.boundsInRoot.bottom<=1500f) visible=true
        if(!visible) {
            rule.onAllNodes(hasScrollAction(),useUnmergedTree=true).onLast().performTouchInput { swipe(center,center.copy(y=center.y-180f),350) }
            rule.mainClock.advanceTimeBy(350);rule.waitForIdle()
        }
    }
    org.junit.Assert.assertTrue("group reachable: "+title,visible)
    rule.onAllNodesWithText(title,useUnmergedTree=true).onLast().performClick()
    rule.mainClock.advanceTimeBy(500);rule.waitForIdle()
}
