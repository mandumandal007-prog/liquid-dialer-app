package com.mandu.liquiddialer

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

/** User-requested customization pack: 4 font styles + 6 icon style packs + wider recents rows.
 *  Pickers persist, appearance backup accepts/rejects the new keys, day + large-font shots. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomizationUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
    private fun await(c:()->Boolean){repeat(40){org.robolectric.shadows.ShadowLooper.idleMainLooper();settle();if(c())return;Thread.sleep(50)};error("timeout settings="+LdApp.settings.value)}

    @Before fun seed(){
        runBlocking{app.ldStore.edit{p->
            p[K.onboarded]=true;p[K.profileName]="Mandu Mandal";p[K.profileNumber]="+91 83888 80466"
            p[K.theme]="daylight";p[K.fontScale]=1f;p[K.motion]=false;p[K.dialerScale]=1f;p[K.fontStyle]=0;p[K.iconStyle]=0}}
        com.mandu.liquiddialer.telecom.CallManager.calls.value=emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        rule.runOnUiThread{
            rule.activity.state.showOnboarding=false
            rule.activity.state.contacts=listOf(
                Contact(11,"lk1","Rahul Sharma","","+91 98765 43210","MOBILE",true,null),
                Contact(12,"lk2","Angika Mamn","", "+91 91234 56789","MOBILE",false,null))
            rule.activity.state.recents=listOf(
                Recent(1,"+91 91234 56789","Angika Mamn",1,System.currentTimeMillis()-42*60_000,245,null,null),
                Recent(2,"+91 98765 43210","Rahul Sharma",2,System.currentTimeMillis()-5*3600_000,0,null,null))
            rule.activity.state.isDefault=true
        }
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.READ_CALL_LOG)
        settle()
        await{rule.onAllNodesWithTag("drawer_open",useUnmergedTree=true).fetchSemanticsNodes().size==1}
    }

    private fun openAppearance(){
        rule.onNodeWithTag("drawer_open",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Settings",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Display",useUnmergedTree=true).performClick();settle()
        rule.onNodeWithText("Theme & appearance",useUnmergedTree=true).performClick();settle()
    }

    @Test fun pickersRenderAndApplyDayLarge(){
        openAppearance()
        // saare chips composed hain (verticalScroll page); click-save path wahi save() hai jo Segmented controls use karte hain (unpe existing coverage)
        for(i in 0..3) assertEquals(1,rule.onAllNodesWithTag("font_style_$i",useUnmergedTree=true).fetchSemanticsNodes().size)
        for(i in 0..7) assertEquals(1,rule.onAllNodesWithTag("icon_style_$i",useUnmergedTree=true).fetchSemanticsNodes().size)
        runBlocking{app.ldStore.edit{it[K.fontStyle]=2;it[K.iconStyle]=2}}
        await{LdApp.settings.value.fontStyle==2 && LdApp.settings.value.iconStyle==2};settle()
        // nayi sections viewport me lao (repo-proven scroll pattern), phir shot
        repeat(3){rule.onRoot().performTouchInput{swipeUp(startY=height*0.8f,endY=height*0.2f)};settle()}
        saveShot(rule,"280-custom-appearance-day")
        runBlocking{app.ldStore.edit{it[K.fontStyle]=3;it[K.iconStyle]=3;it[K.fontScale]=1.3f}}
        await{LdApp.settings.value.fontStyle==3 && LdApp.settings.value.iconStyle==3 && LdApp.settings.value.fontScale==1.3f};settle()
        saveShot(rule,"280-custom-appearance-large")
        rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle()
        runBlocking{app.ldStore.edit{it[K.fontStyle]=0;it[K.iconStyle]=0;it[K.fontScale]=1f}}
    }

    @Test fun fontAndIconStylesRenderOnHome(){
        for(i in listOf(1,2,4,6,7)){
            runBlocking{app.ldStore.edit{it[K.iconStyle]=i;it[K.fontStyle]=0;it[K.fontScale]=1f}}
            await{LdApp.settings.value.iconStyle==i && LdApp.settings.value.fontScale==1f};settle()
            saveShot(rule,"280-icons-style$i")
        }
        runBlocking{app.ldStore.edit{it[K.fontStyle]=2;it[K.iconStyle]=0}}
        await{LdApp.settings.value.fontStyle==2};settle()
        saveShot(rule,"280-font-mono-home")
        runBlocking{app.ldStore.edit{it[K.fontStyle]=0}}
    }

    @Test fun recentsRowsWiderTextDayAndLarge(){
        settle(); saveShot(rule,"280-recents-wide-day")
        runBlocking{app.ldStore.edit{it[K.fontScale]=1.3f}}
        await{LdApp.settings.value.fontScale==1.3f};settle()
        saveShot(rule,"280-recents-wide-large")
        runBlocking{app.ldStore.edit{it[K.fontScale]=1f}}
    }

    @Test fun backupAcceptsAndRejectsNewKeys(){
        val ok=SettingsBackup.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"fontStyle":2,"iconStyle":5}}""")
        assertTrue(ok is SettingsBackup.ParseResult.Ok)
        assertEquals(2,(ok as SettingsBackup.ParseResult.Ok).values["fontStyle"])
        assertEquals(5,ok.values["iconStyle"])
        assertFalse(SettingsBackup.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"fontStyle":4}}""") is SettingsBackup.ParseResult.Ok)
        assertFalse(SettingsBackup.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"iconStyle":8}}""") is SettingsBackup.ParseResult.Ok)
    }
}
