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
class SearchInputUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
    private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1200);rule.waitForIdle()}
    private fun seed(theme:String,font:Float,bottom:Boolean=false) {
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.READ_CALL_LOG,android.Manifest.permission.READ_CONTACTS)
        runBlocking { app.ldStore.edit {it[K.onboarded]=true;it[K.profileName]="Mandu";it[K.theme]=theme;it[K.fontScale]=font;it[K.searchBottom]=bottom;it[K.motion]=false;it[K.showFavourites]=false;it[K.exactTime]=true} }
        rule.waitUntil(10000){LdApp.settings.value.let{it.theme==theme&&it.fontScale==font&&it.searchBottom==bottom}}
        rule.runOnUiThread {
            val st=rule.activity.state;st.showOnboarding=false;st.isDefault=true;st.tab="recents"
            st.contacts=listOf(Contact(11,"rahul","Rahul Sharma","Sharma, Rahul","+91 98765 43210","Mobile",false,null),Contact(12,"priya","Priya Verma","","+91 91234 56789","Mobile",false,null))
            val now=System.currentTimeMillis()
            st.recents=listOf(Recent(1,"+91 98765 43210","Rahul Sharma",2,now-3600000,245,null,null),Recent(2,"+91 91234 56789","Priya Verma",3,now-7200000,0,null,null),Recent(3,"+91 90000 00001","Aman Kumar",1,now-10800000,130,null,null),Recent(4,"+91 90000 00002","Dr. Kavita Sharma Clinic Reception",1,now-14400000,45,null,null))
        };repeat(3){settle()}
    }
    @Test fun formattedPasteAndAlternateNameUseExistingSearchWithoutCalling(){
        seed("daylight",1f)
        rule.onNodeWithTag("nav_contacts",useUnmergedTree=true).performClick();settle()
        val input=rule.onNodeWithContentDescription("Search contacts or numbers",useUnmergedTree=true)
        input.performTextInput("+91 (98765) 43210");settle()
        rule.onNodeWithText("Rahul Sharma",useUnmergedTree=true).assertIsDisplayed()
        rule.onAllNodesWithText("Priya Verma",useUnmergedTree=true).assertCountEquals(0)
        saveShot(rule,"SEARCH349-formatted-paste")
        input.performTextReplacement("Sharma, Rahul");settle()
        rule.onNodeWithText("Rahul Sharma",useUnmergedTree=true).assertIsDisplayed()
        input.performTextReplacement("office98765");settle()
        rule.onAllNodesWithText("Rahul Sharma",useUnmergedTree=true).assertCountEquals(0)
        rule.onNodeWithTag("search_clear",useUnmergedTree=true).performClick();settle()
        input.assertTextEquals("")
        rule.onNodeWithText("Priya Verma",useUnmergedTree=true).assertIsDisplayed()
        assertEquals(2,rule.activity.state.contacts.size)
        val intent=Shadows.shadowOf(rule.activity).nextStartedActivity
        assertTrue(intent==null || intent.action!=android.content.Intent.ACTION_CALL)
    }
}
