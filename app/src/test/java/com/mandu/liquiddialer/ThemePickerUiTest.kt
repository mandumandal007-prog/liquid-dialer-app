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

/** New complete themes: picker lists every option, taps persist, home renders per theme normal + large font,
 *  and appearance backup accepts the new theme values. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemePickerUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private val app get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
    private fun await(c:()->Boolean){repeat(40){org.robolectric.shadows.ShadowLooper.idleMainLooper();settle();if(c())return;Thread.sleep(50)};error("timeout settings="+LdApp.settings.value)}

    @Before fun seed(){
        runBlocking{app.ldStore.edit{p->
            p[K.onboarded]=true;p[K.profileName]="Mandu Mandal";p[K.profileNumber]="+91 83888 80466"
            p[K.theme]="daylight";p[K.fontScale]=1f;p[K.motion]=false;p[K.dialerScale]=1f}}
        com.mandu.liquiddialer.telecom.CallManager.calls.value=emptyList()
        com.mandu.liquiddialer.telecom.CallManager.bump()
        rule.runOnUiThread{
            rule.activity.state.showOnboarding=false
            rule.activity.state.contacts=listOf(
                Contact(11,"lk1","Rahul Sharma","","+91 98765 43210","MOBILE",true,null),
                Contact(12,"lk2","Papa","","+91 91234 56789","MOBILE",false,null))
            rule.activity.state.recents=listOf(
                Recent(1,"+91 98765 43210","Rahul Sharma",1,System.currentTimeMillis()-42*60_000,245,null,null),
                Recent(2,"+91 91234 56789","Papa",2,System.currentTimeMillis()-5*3600_000,0,null,null))
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

    @Test fun pickerListsEveryThemeAndTapsPersist(){
        openAppearance()
        for(n in listOf("Midnight","Aurora","Sunset","Ocean","Rose","Daylight","Sand","Auto"))
            rule.onNodeWithText(n,useUnmergedTree=true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Auto",useUnmergedTree=true).performScrollTo();settle()
        saveShot(rule,"280-themes-picker-day")
        rule.onNodeWithText("Ocean",useUnmergedTree=true).performScrollTo().performClick()
        await{LdApp.settings.value.theme=="ocean"}
        rule.onNodeWithText("Rose",useUnmergedTree=true).performScrollTo().performClick()
        await{LdApp.settings.value.theme=="rose"}
        runBlocking{app.ldStore.edit{it[K.fontScale]=1.3f}}
        await{LdApp.settings.value.fontScale==1.3f};settle()
        rule.onNodeWithText("Sand",useUnmergedTree=true).performScrollTo();settle()
        saveShot(rule,"280-themes-picker-rose-large")
        rule.onNodeWithText("Sand",useUnmergedTree=true).performScrollTo().performClick()
        await{LdApp.settings.value.theme=="sand"}
        rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle()
        runBlocking{app.ldStore.edit{it[K.theme]="daylight";it[K.fontScale]=1f}}
    }

    @Test fun newThemesRenderHomeNormalAndLarge(){
        for(t in listOf("ocean","rose","sand")){
            runBlocking{app.ldStore.edit{it[K.theme]=t;it[K.fontScale]=1f}}
            await{LdApp.settings.value.theme==t && LdApp.settings.value.fontScale==1f};settle()
            saveShot(rule,"280-theme-$t")
            runBlocking{app.ldStore.edit{it[K.fontScale]=1.3f}}
            await{LdApp.settings.value.fontScale==1.3f};settle()
            saveShot(rule,"280-theme-$t-large")
        }
    }

    @Test fun appearanceBackupAcceptsNewThemeValues(){
        for(t in listOf("ocean","rose","sand")){
            val r=SettingsBackup.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"theme":"$t"}}""")
            assertTrue("theme $t rejected",r is SettingsBackup.ParseResult.Ok)
            assertEquals(t,(r as SettingsBackup.ParseResult.Ok).values["theme"])
        }
    }
}
