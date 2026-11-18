package com.mandu.liquiddialer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@OptIn(ExperimentalTestApi::class)
abstract class OrganizedSettingsFixture{
 protected val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @get:Rule(order=0)val initial=object:org.junit.rules.ExternalResource(){override fun before(){runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false;it[K.theme]="daylight";it[K.fontScale]=1f;it[K.organizedSettings]=false};ctx.repo.flow.first{it.onboarded&&!it.motion&&!it.organizedSettings&&it.theme=="daylight"&&it.fontScale==1f}}}}
 @get:Rule(order=1)val rule=createAndroidComposeRule<MainActivity>()
 private var host:ComposeView?=null
 protected val route=mutableStateOf("settings")
 protected val opened=mutableListOf<String>()
 private val back=mutableListOf<String>()
 protected fun settle(){rule.mainClock.autoAdvance=false;rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.advanceTimeBy(500);rule.waitForIdle()}
 protected fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 protected fun start(on:Boolean=true,night:Boolean=false){runBlocking{ctx.ldStore.edit{it[K.organizedSettings]=on;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f}};await{LdApp.settings.value.let{it.organizedSettings==on&&it.fontScale==(if(night)1.4f else 1f)}};rule.runOnUiThread{val state=AppState().also{it.showOnboarding=false;it.isDefault=true};host=ComposeView(rule.activity).also{it.setContent{LdThemeHost{Page(route.value,state,rule.activity,{route.value=if(back.isEmpty())"settings"else back.removeAt(back.lastIndex)},{opened+=it;back+=route.value;route.value=it},null,"",{})}};rule.activity.setContentView(it)}};settle()}
 protected fun show(r:String){rule.runOnUiThread{route.value=r};settle()}
 protected fun tapTag(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo().performClick();settle()}
 protected fun text(t:String)=rule.onNodeWithText(t,true)
 protected fun shot(name:String){settle();rule.mainClock.advanceTimeBy(1500);rule.waitForIdle();saveShot(rule,name)}
 @After fun cleanup(){rule.runOnUiThread{host?.disposeComposition();rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content).removeAllViews()};ctx.scope.cancel()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrganizedSettingsCatalogUiTest:OrganizedSettingsFixture(){
 @Test fun defaultOffUniqueCategoriesAndDestinations(){assertFalse(LdSettings().organizedSettings);assertEquals(6,ORGANIZED_CATEGORIES.size);assertEquals(6,ORGANIZED_CATEGORIES.map{it.id}.toSet().size);assertEquals(23,ORGANIZED_DESTINATIONS.size);assertEquals(23,ORGANIZED_DESTINATIONS.map{it.id}.toSet().size);assertEquals(23,ORGANIZED_DESTINATIONS.map{it.route}.toSet().size);assertTrue(ORGANIZED_DESTINATIONS.all{d->ORGANIZED_CATEGORIES.any{it.id==d.category}})}
 @Test fun allOriginalGroupsRemainReachable(){val routes=ORGANIZED_DESTINATIONS.map{it.route}.toSet();for(g in SETTINGS_GROUPS.filter{it.id !in listOf("display","calls")})assertTrue(g.id,"settings:"+g.id in routes);assertTrue("settings:callcontrols" in routes);assertTrue("settings:recents" in routes)}
 @Test fun allThirtySevenToolRoutesExactlyOnce(){assertEquals(37,ORGANIZED_TOOLS.size);assertEquals(37,ORGANIZED_TOOLS.map{it.route}.toSet().size);assertTrue(ORGANIZED_TOOLS.all{it.section in listOf("live","notes","calculators")});val old=SettingsFinder.all.filter{it.group=="calls"}.map{it.route}.toSet();assertTrue(ORGANIZED_TOOLS.all{it.route in old||it.route=="callpace"||it.route=="ivrmenus"});assertTrue(ORGANIZED_TOOLS.any{it.route=="callcanvas"});assertTrue(ORGANIZED_TOOLS.any{it.route=="livecalldesk"})}
 @Test fun oldFinderRoutesRemainCompatible(){for(e in SettingsFinder.all.filter{it.target.isNotBlank()})assertEquals(e,SettingsFinder.resolve(SettingsFinder.route(e)));assertEquals("settings",SettingsFinder.all.single{it.id=="organized-settings"}.route);assertTrue(SettingsFinder.search("organized categories").any{it.id=="organized-settings"})}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrganizedSettingsNavigationUiTest:OrganizedSettingsFixture(){
 @Test fun offOriginalListOnCategoriesAndReversible(){start(false);text("Group kholo, phir uski settings chuno").assertExists();text("Organized Settings layout").performClick();await{LdApp.settings.value.organizedSettings&&rule.onAllNodesWithTag("organized_settings_intro",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("organized_settings_intro",true).assertExists();tapTag("organized_settings_switch");await{!LdApp.settings.value.organizedSettings&&rule.onAllNodesWithText("Group kholo, phir uski settings chuno",true).fetchSemanticsNodes().size==1};text("Group kholo, phir uski settings chuno").assertExists()}
 @Test fun everyCategoryOpensEveryDestinationWithoutMutation(){start();val before=LdApp.settings.value;for(c in ORGANIZED_CATEGORIES){show("settingshub:"+c.id);for(d in ORGANIZED_DESTINATIONS.filter{it.category==c.id}){tapTag("settings_destination_"+d.id);assertEquals(d.route,opened.last());show("settingshub:"+c.id)}};assertEquals(before,LdApp.settings.value);assertNull(rule.activity.state.pendingCall)}
 @Test fun headerAndHardwareBackReturnCategory(){start();tapTag("settings_category_look");assertEquals("settingshub:look",route.value);tapTag("settings_destination_recents");text("Compact recents").assertExists();rule.onNodeWithTag("page_back",true).performClick();settle();assertEquals("settingshub:look",route.value);rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle();assertEquals("settings",route.value)}
 @Test fun originalCategoryListStillOpens(){start();rule.mainClock.autoAdvance=true;text("Original category list").performScrollTo().performClick();settle();assertEquals("settingshub:classic",route.value);text("Display").performClick();settle();assertEquals("settings:display",route.value);text("Theme & appearance").assertExists();text("Recents grouped count badge").assertExists()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrganizedSettingsControlsUiTest:OrganizedSettingsFixture(){
 @Test fun recentsCategoryOnlyRecentsAndRealPreference(){start();show("settings:recents");text("Compact recents").assertExists();text("Theme & appearance").assertDoesNotExist();text("Keypad haptic").assertDoesNotExist();text("Number details glass layout").assertDoesNotExist();val before=LdApp.settings.value;rule.mainClock.autoAdvance=true;text("Recents overview layout").performScrollTo().performClick();await{LdApp.settings.value.recentsOverview};assertEquals(before.copy(recentsOverview=true),LdApp.settings.value)}
 @Test fun numberAndNavigationAndKeypadAreSeparated(){start();show("settings:numbercard");text("Number details glass layout").assertExists();text("Compact recents").assertDoesNotExist();text("Keypad haptic").assertDoesNotExist();show("settings:navigation");text("Swipe se tab badlo").assertExists();text("Search bar neeche").assertExists();text("Compact recents").assertDoesNotExist();show("settings:keycontacts");text("Keypad haptic").assertExists();text("Contact sort").assertExists();text("Naam ka format").assertExists();text("Recents overview layout").assertDoesNotExist()}
 @Test fun callingControlsNoToolWall(){start();show("settings:callcontrols");text("Default SIM").assertExists();text("Call confirm karo").assertExists();text("Call reference cards").assertDoesNotExist();text("Calculator item bundles").assertDoesNotExist();rule.mainClock.autoAdvance=true;text("Call confirm karo").performScrollTo().performClick();await{LdApp.settings.value.confirmCall};assertNull(rule.activity.state.pendingCall)}
 @Test fun legacyDisplayAndCallsKeepOldControls(){start();show("settings:display");for(t in listOf("Theme & appearance","Compact recents","Number details glass layout","Keypad haptic","Search bar neeche"))text(t).assertExists();show("settings:calls");text("Call reference cards").assertExists();text("Default SIM").assertExists()}
 @Test fun eachToolSectionContainsOnlyItsRoutes(){start();val before=LdApp.settings.value;for(section in listOf("live","notes","calculators")){show("settingstools:$section");for(t in ORGANIZED_TOOLS){val n=rule.onNodeWithTag("settings_tool_"+t.route,true);if(t.section==section)n.assertExists()else n.assertDoesNotExist()}};assertEquals(before,LdApp.settings.value)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrganizedSettingsDayPixelsUiTest:OrganizedSettingsFixture(){
 @Test fun homeAndLookCategory(){start();shot("SET498-day-home");tapTag("settings_category_look");shot("SET498-day-look");tapTag("settings_destination_recents");shot("SET498-day-recents");show("settingshub:tools");shot("SET498-day-tools")}
 @Test fun offChoiceAndToolSubcategory(){start(false);shot("SET498-settings-off");show("settingstools:notes");shot("SET498-day-notes")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrganizedSettingsNightPixelsUiTest:OrganizedSettingsFixture(){
 @Test fun largeTextHomeAndPhone(){start(night=true);shot("SET498-night-home");tapTag("settings_category_phone");shot("SET498-night-phone");tapTag("settings_destination_callcontrols");shot("SET498-night-calling");text("Default SIM").assertIsDisplayed();assertNull(rule.activity.state.pendingCall)}
}
