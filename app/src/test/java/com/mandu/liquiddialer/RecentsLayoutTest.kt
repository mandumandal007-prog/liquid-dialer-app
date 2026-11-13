package com.mandu.liquiddialer

import android.Manifest
import android.provider.CallLog
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class RecentsLayoutTest {
    // Select the test's settings before MainActivity installs its single real scene.
    // This is a layout test, not a live theme-switch test. Late settings emissions
    // are an unnecessary source of resize races while opening the sheet.
    private var fixtureTheme="daylight"
    private var fixtureLarge=false
    @get:Rule(order=0) val initial=object:org.junit.rules.TestWatcher(){override fun starting(d:org.junit.runner.Description){
        fixtureLarge=d.methodName.contains("night",ignoreCase=true)||d.methodName=="contactsTypographyAndAddAction"
        fixtureTheme=if(fixtureLarge)"midnight"else"daylight"
        runBlocking {ctx.ldStore.edit {p->p[K.onboarded]=true;p[K.theme]=fixtureTheme;p[K.fontScale]=if(fixtureLarge)1.3f else 1f;p[K.motion]=false;p[K.showFavourites]=false;p[K.showTodayStats]=true;p[K.groupByDay]=true}}
    }}
    @get:Rule(order=1) val rule = createAndroidComposeRule<MainActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle() {
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        rule.runOnUiThread {androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()}
        rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
    }
    private fun await(check:()->Boolean){rule.waitUntil(10000){settle();check()}}
    private fun ready(theme:String, large:Boolean) {
        settle()
        assertEquals(fixtureTheme,theme);assertEquals(fixtureLarge,large)
        await {LdApp.settings.value.let{it.theme==theme&&it.fontScale==(if(large)1.3f else 1f)&&!it.motion}}
        Shadows.shadowOf(ctx).grantPermissions(Manifest.permission.READ_CALL_LOG)
        rule.runOnUiThread {
            rule.activity.state.showOnboarding=false
            rule.activity.state.isDefault=true
            rule.activity.state.contacts=emptyList()
            val now=System.currentTimeMillis()
            rule.activity.state.recents=listOf(
                Recent(1,"+91 98765 43210","Rahul Sharma",CallLog.Calls.OUTGOING_TYPE,now-60_000,245,null,null),
                Recent(2,"+91 91234 56789","Papa",CallLog.Calls.MISSED_TYPE,now-120_000,0,null,null),
                Recent(3,"+91 90000 00003","Priya Verma",CallLog.Calls.INCOMING_TYPE,now-180_000,120,null,null)
            )
        }
        await {rule.onAllNodesWithTag("recent_controls",true).fetchSemanticsNodes().isNotEmpty()}
    }
    private fun controls() {
        rule.onNodeWithTag("recent_controls",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithTag("insights_open",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithTag("recent_clear",useUnmergedTree=true).assertIsDisplayed()
        val filters=rule.onNodeWithTag("recent_filters",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val stats=rule.onNodeWithTag("today_stats",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        assertTrue("filters stay above optional summary strips",filters.bottom<=stats.top)
        for(i in 0..3){rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_filter_$i",true).performScrollTo().assertIsDisplayed();settle()}
        rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_filter_1",true).performScrollTo().performClick();settle()
        rule.onNodeWithText("Papa",useUnmergedTree=true).assertExists()
        rule.onNodeWithText("Rahul Sharma",useUnmergedTree=true).assertDoesNotExist()
        rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_filter_0",useUnmergedTree=true).performScrollTo().performClick();settle()
        rule.onNodeWithText("Rahul Sharma",useUnmergedTree=true).assertExists()
    }
    @Test fun dayLayoutAndFilters(){ready("daylight",false);controls();saveShot(rule,"L1-recents-day")}
    @Test fun nightLargeLayoutAndFilters(){ready("midnight",true);controls();saveShot(rule,"L2-recents-night-large")}
    private fun dialer(theme:String,large:Boolean,shot:String) {
        ready(theme,large)
        rule.onNodeWithTag("dialer_fab",useUnmergedTree=true).performClick();settle()
        rule.runOnUiThread {rule.activity.state.dial="9876543210"};repeat(4){settle()}
        // Wait for the rendered sheet, not a fixed number of frames after the click.
        assertEquals("keypad FAB opens dialer state","dialer",rule.activity.state.tab)
        try{await {rule.onNodeWithTag("dial_toolbar",true).isDisplayed()}}catch(t:Throwable){
            println("DIALER_FIXTURE tab="+rule.activity.state.tab+" settings="+LdApp.settings.value)
            println(rule.onAllNodes(isRoot(),useUnmergedTree=true).printToString())
            saveShot(rule,shot+"-failed")
            throw t
        }
        rule.onNodeWithTag("dial_toolbar",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithTag("voice_mic",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithTag("dial_menu",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithContentDescription("Dialer menu",useUnmergedTree=true).assertExists()
        rule.onNodeWithTag("dial_number_panel",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithTag("dial_save_contact",useUnmergedTree=true).assertExists()
        rule.onNodeWithTag("dial_pause",useUnmergedTree=true).assertExists()
        rule.onNodeWithTag("dial_meet",useUnmergedTree=true).assertExists()
        saveShot(rule,shot)
        // Long number and a known contact name are separate visual edge cases.
        rule.runOnUiThread {rule.activity.state.dial="+441234567890123"};settle()
        saveShot(rule,shot+"-long")
        rule.runOnUiThread {
            rule.activity.state.contacts=listOf(Contact(11,"lk","Rahul Kumar Sharma","","+91 98765 43210","MOBILE",false,null))
            rule.activity.state.dial="9876543210"
        };settle()
        rule.onNodeWithText("Rahul Kumar Sharma",useUnmergedTree=true).assertIsDisplayed()
        saveShot(rule,shot+"-named")
        rule.runOnUiThread {rule.activity.state.contacts=emptyList()};repeat(4){settle()}
        rule.onNodeWithTag("dial_pause",useUnmergedTree=true).performClick();settle()
        assertEquals("9876543210,",rule.activity.state.dial)
        rule.onNodeWithTag("dial_menu",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithTag("dial_menu_popup",useUnmergedTree=true).assertExists()
        assertNull("layout actions must not start a call",rule.activity.state.pendingCall)
    }
    @Test fun dialerDayLayoutKeepsActions(){dialer("daylight",false,"L3-dialer-day")}
    @Test fun dialerNightLargeLayoutKeepsActions(){dialer("midnight",true,"L4-dialer-night-large")}

    @Test fun contactsTypographyAndAddAction() {
        ready("midnight",true)
        Shadows.shadowOf(ctx).grantPermissions(Manifest.permission.READ_CONTACTS)
        rule.runOnUiThread {
            rule.activity.state.contacts=listOf(
                Contact(11,"lk11","Rahul Kumar Sharma","","+91 98765 43210","MOBILE",false,null),
                Contact(12,"lk12","Priya Verma","","+44 12345 67890","WORK",false,null))
            rule.activity.state.tab="contacts"
        };settle()
        rule.onNodeWithText("Rahul Kumar Sharma",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithTag("contacts_add",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithContentDescription("Add contact",useUnmergedTree=true).assertExists()
        saveShot(rule,"C5-contacts-night-large")
        rule.onNodeWithTag("contacts_add",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Naya contact",useUnmergedTree=true).assertExists()
        assertEquals("opening add must not create a contact",2,rule.activity.state.contacts.size)
    }

    @Test fun clearStillRequiresConfirmation(){
        ready("daylight",false)
        rule.onNodeWithTag("recent_clear",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Saari call history delete karein?",substring=true,useUnmergedTree=true).assertExists()
        assertEquals("opening confirmation cannot delete history",3,rule.activity.state.recents.size)
        rule.onNodeWithText("Rehne do",substring=true,useUnmergedTree=true).performClick();settle()
        assertEquals(3,rule.activity.state.recents.size)
    }
}
