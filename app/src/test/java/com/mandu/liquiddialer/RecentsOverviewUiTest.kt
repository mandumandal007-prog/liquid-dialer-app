package com.mandu.liquiddialer
import android.Manifest
import android.provider.CallLog
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
@OptIn(ExperimentalTestApi::class)
abstract class RecentsOverviewFixture {
 protected val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @get:Rule(order=0)val initial=object:org.junit.rules.ExternalResource(){override fun before(){runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.recentsOverview]=false;it[K.recentsSingleRow]=false;it[K.motion]=false;it[K.theme]="daylight";it[K.fontScale]=1f};ctx.repo.flow.first{it.onboarded&&!it.recentsOverview&&!it.recentsSingleRow&&!it.motion&&it.theme=="daylight"&&it.fontScale==1f}}}}
 @get:Rule(order=1)val rule=createAndroidComposeRule<MainActivity>()
 protected lateinit var state:AppState
 private var host:ComposeView?=null
 protected fun settle(){rule.mainClock.autoAdvance=false;rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(400);rule.waitForIdle()}
 protected fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 protected fun option(on:Boolean){runBlocking{ctx.repo.set(K.recentsOverview,on)};await{LdApp.settings.value.recentsOverview==on&&rule.onAllNodesWithTag("recent_filter_grid",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun mount(settings:Boolean=false){rule.runOnUiThread{host?.disposeComposition();host=ComposeView(rule.activity).also{it.setContent{LdThemeHost{if(settings)Page("settings:display",state,rule.activity,{},{},null,"",{})else MainScreen(state,rule.activity)}};rule.activity.setContentView(it)}};settle()}
 protected fun start(on:Boolean=true,night:Boolean=false){
  settle();Shadows.shadowOf(ctx).grantPermissions(Manifest.permission.READ_CALL_LOG)
  runBlocking{ctx.ldStore.edit{it[K.recentsOverview]=on;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.showFavourites]=false;it[K.showTodayStats]=false;it[K.groupByDay]=true;it[K.recentsCompact]=false;it[K.recentsHideBlocked]=false;it[K.hiddenFeatures]=emptySet();it[K.searchBottom]=false;it[K.lang]="hi-en";it[K.recentsStyle]="glass"}}
  await{LdApp.settings.value.let{it.recentsOverview==on&&it.fontScale==(if(night)1.4f else 1f)&&!it.showFavourites&&!it.showTodayStats&&it.groupByDay&&it.recentsStyle=="glass"}}
  val now=System.currentTimeMillis()
  state=AppState().also{it.showOnboarding=false;it.isDefault=true;it.recents=listOf(
   Recent(1,"+919876543210","Rahul Kumar Sharma",CallLog.Calls.OUTGOING_TYPE,now-60_000,245,null,null),
   Recent(2,"+919123456789","Papa",CallLog.Calls.MISSED_TYPE,now-120_000,0,null,null),
   Recent(3,"+919000000003","Priya Verma",CallLog.Calls.INCOMING_TYPE,now-180_000,120,null,null))}
  com.mandu.liquiddialer.telecom.CallManager.calls.value=emptyList();mount();await{rule.onAllNodesWithTag("recent_controls",true).fetchSemanticsNodes().size==1}
 }
 protected fun hit(i:Int){rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_filter_$i",true).performScrollTo().performClick();settle()}
 protected fun visibleGrid(){
  val panel=rule.onNodeWithTag("recent_overview_panel",true).fetchSemanticsNode().boundsInRoot
  val nodes=(0..3).map{rule.onNodeWithTag("recent_filter_$it",true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot}
  for(b in nodes){assertTrue(b.left>=panel.left&&b.right<=panel.right);assertTrue(b.height>=48*rule.density.density)}
  assertEquals(nodes[0].top,nodes[1].top,.5f);assertEquals(nodes[2].top,nodes[3].top,.5f);assertTrue(nodes[0].bottom<nodes[2].top)
 }
 @After fun cleanup(){rule.runOnUiThread{host?.disposeComposition();rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content).removeAllViews()};runBlocking{ctx.repo.set(K.recentsOverview,false);ctx.repo.set(K.fontScale,1f)};ctx.scope.cancel()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsOverviewLayoutUiTest:RecentsOverviewFixture(){
 @Test fun defaultOffFinderRoute(){assertFalse(LdSettings().recentsOverview);assertNotEquals(K.recentsOverview,K.recentsCompact);assertEquals("settings:display",SettingsFinder.all.single{it.id=="recents-overview"}.route)}
 @Test fun offOriginalStripNoPanel(){start(false);rule.onNodeWithTag("recent_filters",true).assertExists();rule.onNodeWithTag("recent_overview_panel",true).assertDoesNotExist();rule.onNodeWithTag("recent_day_separator",true).assertDoesNotExist()}
 @Test fun gridShowsAllFourWithoutHorizontalScroll(){start();visibleGrid();rule.onNodeWithTag("recent_filters",true).assertDoesNotExist();rule.onNodeWithTag("recent_day_separator",true).assertExists();assertEquals(3,state.recents.size)}
 @Test fun eachFilterKeepsOriginalMeaning(){start();for((i,name)in listOf(1 to "Papa",2 to "Rahul Kumar Sharma",3 to "Priya Verma")){hit(i);rule.onNodeWithText(name,true).assertExists();listOf("Papa","Rahul Kumar Sharma","Priya Verma").filter{it!=name}.forEach{rule.onNodeWithText(it,true).assertDoesNotExist()};rule.onNodeWithTag("recent_filter_$i",true).assertIsSelected()};hit(0);assertEquals(3,state.recents.size);rule.onNodeWithText("Papa",true).assertExists()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsOverviewSafetyUiTest:RecentsOverviewFixture(){
 @Test fun layoutSwitchKeepsSelectedFilterAndData(){start();hit(1);val before=state.recents.toList();option(false);rule.onNodeWithTag("recent_filters",true).assertExists();rule.onNodeWithText("Rahul Kumar Sharma",true).assertDoesNotExist();option(true);rule.onNodeWithTag("recent_filter_1",true).assertIsSelected();rule.onNodeWithText("Papa",true).assertExists();assertEquals(before,state.recents)}
 @Test fun searchAndClearKeepGrid(){start();rule.onNode(hasSetTextAction(),true).performTextReplacement("Priya");settle();rule.onNodeWithText("Priya Verma",true).assertExists();rule.onNodeWithText("Papa",true).assertDoesNotExist();rule.onNodeWithTag("search_clear",true).performClick();settle();visibleGrid();rule.onNodeWithText("Papa",true).assertExists();assertNull(state.pendingCall)}
 @Test fun clearStillNeedsConfirmationAndCancelPreserves(){start();val before=state.recents.toList();rule.onNodeWithTag("recent_clear",true).performClick();settle();rule.onNodeWithText("Saari call history delete karein?",true).assertExists();assertEquals(before,state.recents);rule.onNodeWithText("Rehne do",true).performClick();settle();assertEquals(before,state.recents);assertNull(state.pendingCall)}
 @Test fun groupingOffRemovesSeparatorsWithoutChangingRows(){start();runBlocking{ctx.repo.set(K.groupByDay,false)};await{!LdApp.settings.value.groupByDay&&rule.onAllNodesWithTag("recent_day_separator",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithText("Papa",true).assertExists();assertEquals(3,state.recents.size)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsOverviewDayPixelsUiTest:RecentsOverviewFixture(){
 @Test fun dayFullHome(){start();visibleGrid();rule.onNodeWithTag("dialer_fab",true).assertIsDisplayed();saveShot(rule,"HOME490-day-overview");hit(1);saveShot(rule,"HOME490-day-missed");assertNull(state.pendingCall)}
 @Test fun settingsOffThenPersistOnAndOff(){start(false);mount(true);rule.mainClock.autoAdvance=true;rule.onNodeWithText("Recents overview layout",true).performScrollTo();settle();saveShot(rule,"HOME490-settings-off");rule.onNodeWithText("Recents overview layout",true).performClick();await{LdApp.settings.value.recentsOverview};mount();await{rule.onAllNodesWithTag("recent_filter_grid",true).fetchSemanticsNodes().size==1};option(false);assertEquals(3,state.recents.size)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsOverviewNightPixelsUiTest:RecentsOverviewFixture(){
 @Test fun nightLargeLabelsAndRows(){start(night=true);visibleGrid();saveShot(rule,"HOME490-night-large-overview");rule.mainClock.autoAdvance=true;rule.onNodeWithText("Rahul Kumar Sharma",true).performScrollTo();settle();rule.onNodeWithText("Rahul Kumar Sharma",true).assertIsDisplayed();rule.onAllNodesWithTag("call_mini",true).onFirst().assertIsDisplayed();saveShot(rule,"HOME490-night-large-row");assertNull(state.pendingCall)}
 @Test fun emptyStateKeepsAllFiltersNoActions(){start(night=true);rule.runOnUiThread{state.recents=emptyList()};settle();visibleGrid();rule.onNodeWithTag("recent_clear",true).assertDoesNotExist();rule.onNodeWithTag("insights_open",true).assertDoesNotExist();rule.onNodeWithText("Abhi koi call nahi",true).assertExists();saveShot(rule,"HOME490-night-empty")}
}
