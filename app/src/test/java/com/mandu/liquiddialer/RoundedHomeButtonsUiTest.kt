package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class RoundedHomeFixture:RecentsOverviewFixture(){
 @Before fun resetRounded(){runBlocking{ctx.ldStore.edit{it[K.roundedHomeButtons]=false}};await{!LdApp.settings.value.roundedHomeButtons}}
 protected fun rounded(on:Boolean=true){runBlocking{ctx.repo.set(K.roundedHomeButtons,on)};await{LdApp.settings.value.roundedHomeButtons==on};settle()}
 protected fun node(tag:String)=rule.onNodeWithTag(tag,true)
 protected fun bounds(tag:String)=node(tag).fetchSemanticsNode().boundsInRoot
 protected fun shot(name:String){settle();rule.mainClock.advanceTimeBy(1500);rule.waitForIdle();saveShot(rule,name)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoundedHomeButtonsUiTest:RoundedHomeFixture(){
 @Test fun defaultOffAndFinder(){assertFalse(LdSettings().roundedHomeButtons);assertTrue(SettingsFinder.search("Rounded buttons").any{it.id=="rounded-home-buttons"&&it.route=="settings:recents"});assertNotEquals(K.roundedHomeButtons,K.recentsOverview);start();assertFalse(LdApp.settings.value.roundedHomeButtons);visibleGrid()}
 @Test fun reversibleChangesOnlyOnePreference(){start();mount(true);rule.mainClock.autoAdvance=true;node("rounded_home_buttons_switch").performScrollTo();settle();val before=LdApp.settings.value;node("rounded_home_buttons_switch").performClick();await{LdApp.settings.value.roundedHomeButtons};assertEquals(before.copy(roundedHomeButtons=true),LdApp.settings.value);node("rounded_home_buttons_switch").performClick();await{!LdApp.settings.value.roundedHomeButtons};assertEquals(before,LdApp.settings.value);assertTrue(LdApp.settings.value.recentsOverview)}
 @Test fun controlsAndSearchBarBoundsIdenticalOnOff(){start();val tags=listOf("top_bar","drawer_open","search_mic","profile_open")+(0..3).map{"recent_filter_$it"};val before=tags.associateWith{bounds(it)};rounded();for(t in tags)assertEquals(t,before[t],bounds(t));visibleGrid();rounded(false);for(t in tags)assertEquals(t,before[t],bounds(t));assertNull(state.pendingCall)}
 @Test fun filtersMeaningAndSelectedStateKept(){start();rounded();for((i,name)in listOf(1 to "Papa",2 to "Rahul Kumar Sharma",3 to "Priya Verma")){hit(i);node("recent_filter_$i").assertIsSelected();rule.onNodeWithText(name,true).assertExists();listOf("Papa","Rahul Kumar Sharma","Priya Verma").filter{it!=name}.forEach{rule.onNodeWithText(it,true).assertDoesNotExist()}};rounded(false);node("recent_filter_3").assertIsSelected();hit(0);assertEquals(3,state.recents.size);assertNull(state.pendingCall)}
 @Test fun originalStripAndBottomSearchStaySelected(){start(false);rounded();node("recent_filters").assertExists();node("recent_overview_panel").assertDoesNotExist();runBlocking{ctx.repo.set(K.searchBottom,true)};await{LdApp.settings.value.searchBottom};val b=bounds("top_bar");rounded(false);assertEquals(b,bounds("top_bar"));rounded();assertEquals(b,bounds("top_bar"));rule.mainClock.autoAdvance=true;node("recent_filter_3").performScrollTo().performClick();settle();rule.onNodeWithText("Priya Verma",true).assertExists();assertFalse(LdApp.settings.value.recentsOverview);shot("HOME512-day-strip-bottom")}
 @Test fun searchClearMenuAndProfileKeepActions(){start();rounded();rule.onNode(hasSetTextAction(),true).performTextReplacement("Priya");settle();rule.onNodeWithText("Priya Verma",true).assertExists();rule.onNodeWithText("Papa",true).assertDoesNotExist();node("search_clear").performClick();settle();rule.onNodeWithText("Papa",true).assertExists();node("drawer_open").performClick();settle();node("drawer_tool_settings").assertExists();assertNull(state.pendingCall)}
 @Test fun profileAndMicTargetsRemainAccessible(){start();rounded();node("search_mic").assertHasClickAction().assertIsDisplayed();node("profile_open").assertHasClickAction().performClick();settle();rule.onNodeWithTag("page_back",true).assertExists();rule.onNodeWithTag("page_back",true).performClick();settle();node("drawer_open").assertExists();assertNull(state.pendingCall)}
 @Test fun dayOnOffAndSettingsPixels(){start();rule.runOnUiThread{state.isDefault=false};settle();shot("HOME512-day-off");rounded();visibleGrid();shot("HOME512-day-on");mount(true);rule.mainClock.autoAdvance=true;node("rounded_home_buttons_switch").performScrollTo();settle();shot("HOME512-settings-on")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoundedHomeButtonsNightUiTest:RoundedHomeFixture(){
 @Test fun nightLargeGridAndOriginalStrip(){start(night=true);rounded();visibleGrid();shot("HOME512-night-on");for(i in 0..3){node("recent_filter_$i").assertIsDisplayed();val results=mutableListOf<androidx.compose.ui.text.TextLayoutResult>();node("recent_filter_label_$i").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult){it(results)};assertTrue(results.isNotEmpty());assertFalse(results.any{r->(0 until r.lineCount).any{r.isLineEllipsized(it)}})};option(false);node("recent_filters").assertExists();rule.mainClock.autoAdvance=true;node("recent_filter_3").performScrollTo();settle();node("recent_filter_3").assertIsDisplayed();shot("HOME512-night-strip");assertNull(state.pendingCall)}
}
