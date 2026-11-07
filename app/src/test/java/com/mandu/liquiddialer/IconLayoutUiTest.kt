package com.mandu.liquiddialer

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconLayoutUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1200);rule.waitForIdle()}
    private fun seed(theme:String, font:Float, layout:Int, icon:Int) {
        runBlocking {app.ldStore.edit {it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font
            it[K.dialerLayout]=layout;it[K.iconStyle]=icon;it[K.motion]=false;it[K.hapticKeys]=false}}
        rule.waitUntil(15000){val s=LdApp.settings.value;s.theme==theme&&s.fontScale==font&&s.dialerLayout==layout&&s.iconStyle==icon}
        rule.runOnUiThread {rule.activity.state.showOnboarding=false;rule.activity.state.isDefault=true}
        settle()
    }
    private fun dialer(theme:String,font:Float,layout:Int,icon:Int,name:String){
        seed(theme,font,layout,icon)
        rule.onNodeWithTag("dialer_fab",useUnmergedTree=true).performClick();settle()
        rule.runOnUiThread {rule.activity.state.dial="9876543210"};repeat(3){settle()}
        for(d in listOf("1","2","3","4","5","6","7","8","9","*","0","#")) {
            val n=rule.onNodeWithTag("key_$d",useUnmergedTree=true)
            n.assertIsDisplayed()
            val b=n.fetchSemanticsNode().boundsInRoot
            assertTrue("key $d target height",b.height>=96f)
        }
        rule.onNodeWithTag("call_btn",useUnmergedTree=true).assertIsDisplayed()
        saveShot(rule,name)
        rule.onNodeWithTag("key_5",useUnmergedTree=true).performTouchInput{click()};settle()
        assertEquals("98765432105",rule.activity.state.dial)
        rule.onNodeWithTag("key_0",useUnmergedTree=true).performTouchInput{longClick()};settle()
        assertEquals("98765432105+",rule.activity.state.dial)
        assertNull(rule.activity.state.pendingCall)
        rule.runOnUiThread {rule.activity.state.dial="+441234567890123"};repeat(3){settle()}
        saveShot(rule,name+"-long")
    }
    @Test fun circlesDay(){dialer("daylight",1f,0,0,"IL-circles-day")}
    @Test fun tilesOutlineDay(){dialer("daylight",1f,1,6,"IL-tiles-outline-day")}
    @Test fun pillsDuotoneNightLarge(){dialer("midnight",1.3f,2,7,"IL-pills-duotone-night-large")}
    @Test fun newIconPackKeepsActiveHangup() {
        seed("midnight",1.3f,2,7)
        val act=rule.activity
        com.mandu.liquiddialer.telecom.CallManager.calls.value=listOf(FakeCall.make("+91 98765 43210","Rahul Sharma",android.telecom.Call.STATE_ACTIVE,System.currentTimeMillis()-60000))
        com.mandu.liquiddialer.telecom.CallManager.bump()
        try {
            rule.runOnUiThread {act.setContent {LdThemeHost {CallScreen({}, {}, {}, {})}}};repeat(3){settle()}
            rule.onNodeWithTag("end_call",useUnmergedTree=true).assertIsDisplayed()
            val b=rule.onNodeWithTag("end_call",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertTrue("elongated hangup stays wide",b.width>b.height*1.5f)
            saveShot(rule,"IL-active-duotone-night-large")
        } finally {com.mandu.liquiddialer.telecom.CallManager.calls.value=emptyList();com.mandu.liquiddialer.telecom.CallManager.bump()}
    }
    private fun reveal(tag:String) {
        repeat(12) {
            val n=rule.onAllNodesWithTag(tag,useUnmergedTree=true).fetchSemanticsNodes()
            if(n.size==1 && n[0].boundsInRoot.height>30f && n[0].boundsInRoot.top>100f && n[0].boundsInRoot.bottom<1450f) return
            rule.onRoot().performTouchInput {swipeUp(startY=height*.8f,endY=height*.3f)};settle()
        }
        error("picker not revealed: $tag")
    }
    @Test fun selectorsPersistAndBackUp(){
        seed("daylight",1f,0,0)
        
        rule.onNodeWithTag("drawer_open",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Theme & appearance",useUnmergedTree=true).performClick();settle()
        assertEquals(1,rule.onAllNodesWithTag("dialer_layout_2",useUnmergedTree=true).fetchSemanticsNodes().size)
        reveal("dialer_layout_2")
        rule.onNodeWithTag("dialer_layout_2",useUnmergedTree=true).performClick();settle()
        rule.waitUntil(15000){LdApp.settings.value.dialerLayout==2}
        saveShot(rule,"IL-layout-picker")
        reveal("icon_style_7")
        rule.onNodeWithTag("icon_style_7",useUnmergedTree=true).performClick();settle()
        rule.waitUntil(15000){LdApp.settings.value.iconStyle==7}
        saveShot(rule,"IL-icon-picker")
        val parsed=SettingsBackup.parse(SettingsBackup.buildJson(LdApp.settings.value)) as SettingsBackup.ParseResult.Ok
        assertEquals(2,parsed.values["dialerLayout"]);assertEquals(7,parsed.values["iconStyle"])
        val bad=SettingsBackup.buildJson(LdSettings()).replace("\"dialerLayout\": 0","\"dialerLayout\": 3")
        assertTrue(SettingsBackup.parse(bad) is SettingsBackup.ParseResult.Err)
        assertSame(LdIcons.Phone,LdIcons.styled(LdIcons.Phone,999))
        assertNotEquals(LdIcons.styled(LdIcons.Phone,6),LdIcons.styled(LdIcons.Phone,7))
    }
}
