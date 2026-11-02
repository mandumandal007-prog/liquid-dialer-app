package com.mandu.liquiddialer

import androidx.compose.runtime.*
import androidx.activity.compose.setContent
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsGroupsTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun everyGroupOpensAndBackReturnsWithoutChangingSettings() = groups("daylight",1f,"day")
    @Test fun everyGroupOpensWithLargeFontAtNight() = groups("midnight",1.3f,"night-large")
    private fun groups(theme:String,font:Float,suffix:String) {
        rule.mainClock.autoAdvance=false
        rule.mainClock.advanceTimeBy(1200);rule.waitForIdle()
        runBlocking { ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { it[K.onboarded]=true;it[K.lang]="hi-en";it[K.theme]=theme;it[K.fontScale]=font } }
        rule.waitUntil(5000) { LdApp.settings.value.let { it.theme==theme && it.fontScale==font && it.onboarded && it.lang=="hi-en" } }
        rule.runOnUiThread { rule.activity.state.showOnboarding=false }
        rule.mainClock.advanceTimeBy(600);rule.waitForIdle()
        rule.waitUntil(5000) {
            !rule.activity.state.showOnboarding &&
                rule.onAllNodesWithTag("drawer_open",useUnmergedTree=true).fetchSemanticsNodes().size==1
        }
        rule.onNodeWithTag("drawer_open").assertIsDisplayed()
        println("MAIN_READY settings="+LdApp.settings.value+" onboarding="+rule.activity.state.showOnboarding)
        println(rule.onNodeWithTag("drawer_open").printToString())
        val before=LdApp.settings.value
        rule.onNodeWithTag("drawer_open").performClick()
        rule.mainClock.advanceTimeBy(1200);rule.waitForIdle()
        try {
            rule.waitUntil(5000) { rule.onAllNodesWithText("Settings",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            println("DRAWER_NOT_READY settings="+LdApp.settings.value+" onboarding="+rule.activity.state.showOnboarding)
            println(rule.onAllNodes(isRoot(),useUnmergedTree=true).printToString())
            saveShot(rule,"G-drawer-not-ready-"+suffix)
            throw e
        }
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        rule.mainClock.advanceTimeBy(600);rule.waitForIdle()
        saveShot(rule,"G-settings-home-"+suffix)
        for(g in SETTINGS_GROUPS) {
            println("GTEST open "+g.id)
            var visible=false
            repeat(12) {
                val node=rule.onAllNodesWithText(g.title,useUnmergedTree=true).fetchSemanticsNodes().lastOrNull()
                if(node!=null && node.boundsInRoot.top>=110f && node.boundsInRoot.bottom<=1500f) visible=true
                if(!visible) {
                    rule.onAllNodes(hasScrollAction(),useUnmergedTree=true).onLast().performTouchInput { swipe(center,center.copy(y=center.y-180f),350) }
                    rule.mainClock.advanceTimeBy(350);rule.waitForIdle()
                }
            }
            assertTrue("group visible: "+g.id,visible)
            rule.onAllNodesWithText(g.title,useUnmergedTree=true).onLast().performClick()
            rule.mainClock.advanceTimeBy(400);rule.waitForIdle()
            rule.onAllNodesWithText(g.title,useUnmergedTree=true).onLast().assertIsDisplayed()
            if(g.id in listOf("calls","display","volume","about")) saveShot(rule,"G-settings-"+g.id+"-"+suffix)
            // Header back is tested at the visible page top; hardware back also works from the bottom.
            rule.onAllNodesWithTag("page_back",useUnmergedTree=true).onLast().performClick()
            rule.mainClock.advanceTimeBy(400);rule.waitForIdle()
            rule.onNodeWithText("Group kholo, phir uski settings chuno",useUnmergedTree=true).assertExists()
            rule.onAllNodesWithText(g.title,useUnmergedTree=true).onLast().performClick()
            rule.mainClock.advanceTimeBy(400);rule.waitForIdle()
            val bottom=mapOf("display" to "Last, First","calls" to "System call settings","sounds" to "Sirf favourite pe flash","privacy" to "App lock","default" to "Permissions","volume" to "Volume & boost","identity" to "Contact ka label","blocking" to "Private / hidden numbers block","quiet" to "Quiet hours","recording" to "Storage","speed" to "Voicemail call karo","accessibility" to "Hearing aid (HAC)","notifications" to "Notification settings","backup" to "Settings restore","about" to "Made for Mandu").getValue(g.id)
            var reached=false
            repeat(45) {
                val ns=rule.onAllNodesWithText(bottom,useUnmergedTree=true).fetchSemanticsNodes()
                if(ns.any { it.boundsInRoot.top>=110f && it.boundsInRoot.bottom<=1500f }) reached=true
                if(!reached) {
                    rule.onAllNodes(hasScrollAction(),useUnmergedTree=true).onLast().performTouchInput { swipe(center,center.copy(y=center.y-180f),350) }
                    rule.mainClock.advanceTimeBy(250);rule.waitForIdle()
                }
            }
            if(!reached) {
                saveShot(rule,"G-bottom-fail-"+g.id+"-"+suffix)
                println("BOTTOMFAIL "+rule.onAllNodesWithText(bottom,useUnmergedTree=true).fetchSemanticsNodes().map { it.boundsInRoot })
            }
            assertTrue("bottom control reachable: "+g.id,reached)
            repeat(2) {
                rule.onAllNodes(hasScrollAction(),useUnmergedTree=true).onLast().performTouchInput { swipe(center,center.copy(y=center.y-180f),350) }
                rule.mainClock.advanceTimeBy(250);rule.waitForIdle()
            }
            if(g.id in listOf("calls","display","backup")) saveShot(rule,"G-bottom-"+g.id+"-"+suffix)
            rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
            rule.mainClock.advanceTimeBy(400);rule.waitForIdle()
            rule.onNodeWithText("Group kholo, phir uski settings chuno",useUnmergedTree=true).assertExists()
        }
        assertEquals(before,LdApp.settings.value)
        saveShot(rule,"G-settings-groups-"+suffix)
        runBlocking { ApplicationProvider.getApplicationContext<LdApp>().ldStore.edit { it[K.fontScale]=1f } }
    }
}
