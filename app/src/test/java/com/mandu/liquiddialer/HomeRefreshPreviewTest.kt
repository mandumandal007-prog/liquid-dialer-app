package com.mandu.liquiddialer

import androidx.compose.ui.test.*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
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
class HomeRefreshPreviewTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
    private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1200);rule.waitForIdle()}
    private fun seed(theme:String,font:Float,bottom:Boolean=false) {
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.READ_CALL_LOG,android.Manifest.permission.READ_CONTACTS)
        runBlocking { app.ldStore.edit {it[K.onboarded]=true;it[K.profileName]="Mandu";it[K.theme]=theme;it[K.fontScale]=font;it[K.searchBottom]=bottom;it[K.motion]=false;it[K.showFavourites]=false;it[K.exactTime]=true} }
        rule.waitUntil(10000){LdApp.settings.value.let{it.theme==theme&&it.fontScale==font&&it.searchBottom==bottom}}
        rule.runOnUiThread {
            val st=rule.activity.state;st.showOnboarding=false;st.isDefault=true;st.tab="recents"
            st.contacts=listOf(Contact(11,"rahul","Rahul Sharma","","+91 98765 43210","Mobile",false,null),Contact(12,"priya","Priya Verma","","+91 91234 56789","Mobile",false,null))
            val now=System.currentTimeMillis()
            st.recents=listOf(Recent(1,"+91 98765 43210","Rahul Sharma",2,now-3600000,245,null,null),Recent(2,"+91 91234 56789","Priya Verma",3,now-7200000,0,null,null),Recent(3,"+91 90000 00001","Aman Kumar",1,now-10800000,130,null,null),Recent(4,"+91 90000 00002","Dr. Kavita Sharma Clinic Reception",1,now-14400000,45,null,null))
        };repeat(3){settle()}
    }
    private fun controls() {
        for(t in listOf("drawer_open","profile_open","search_mic","insights_open","recent_clear","dialer_fab")) {
            val n=rule.onNodeWithTag(t,useUnmergedTree=true);n.assertIsDisplayed();val b=n.fetchSemanticsNode().boundsInRoot
            assertTrue("$t target width",b.width>=87f);assertTrue("$t target height",b.height>=87f)
        }
        val tags=listOf("drawer_open","profile_open","search_mic")
        val b=tags.map{rule.onNodeWithTag(it,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot}
        for(i in b.indices)for(j in i+1 until b.size)assertFalse("header targets overlap",b[i].overlaps(b[j]))
        rule.onNodeWithContentDescription("Clear call history",useUnmergedTree=true).assertExists()
    }
    @Test fun dayRecentsAndNavigation(){
        seed("daylight",1f);controls();saveShot(rule,"UI361-day-recents")
        rule.onNodeWithTag("recent_filter_1",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Priya Verma",useUnmergedTree=true).assertIsDisplayed()
        rule.onAllNodesWithText("Rahul Sharma",useUnmergedTree=true).assertCountEquals(0)
        rule.onNodeWithTag("nav_contacts",useUnmergedTree=true).performClick();settle()
        assertEquals("contacts",rule.activity.state.tab);saveShot(rule,"UI361-day-contacts")
        rule.onNodeWithContentDescription("Search contacts or numbers",useUnmergedTree=true).performTextInput("Rahul Sharma 9876543210");settle()
        rule.onNodeWithTag("search_clear",useUnmergedTree=true).assertIsDisplayed();saveShot(rule,"UI361-day-search")
        rule.onNodeWithTag("search_clear",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithContentDescription("Search contacts or numbers",useUnmergedTree=true).assertTextEquals("")
        rule.onNodeWithContentDescription("Open keypad",useUnmergedTree=true).assertIsDisplayed()
        rule.onNodeWithTag("dialer_fab",useUnmergedTree=true).performClick();settle();assertEquals("dialer",rule.activity.state.tab)
    }
    @Test fun nightLarge(){seed("midnight",1.3f);controls();saveShot(rule,"UI361-night-large");rule.onNodeWithTag("recent_clear",useUnmergedTree=true).performClick();settle();rule.onNodeWithText("Saari call history delete karein?",substring=true,useUnmergedTree=true).assertExists();rule.onNodeWithText("Rehne do",useUnmergedTree=true).performClick();settle();assertEquals(4,rule.activity.state.recents.size)}
    @Test fun bottomSearchLarge(){seed("daylight",1.3f,true);controls();saveShot(rule,"UI361-bottom-large");val h=rule.onNodeWithTag("top_bar",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot;val b=rule.onNodeWithTag("bottom_bar",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot;assertFalse("search stays above nav",h.overlaps(b))}
}
