package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class SingleRowFixture:RoundedHomeFixture(){
 protected fun row(on:Boolean=true){runBlocking{ctx.repo.set(K.recentsSingleRow,on)};await{LdApp.settings.value.recentsSingleRow==on&&rule.onAllNodesWithTag("recent_filter_single_row",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun visibleRow(){
  val rowBounds=bounds("recent_filter_single_row")
  var top:Float?=null;var bottom:Float?=null
  for(i in 0..3){
   rule.mainClock.autoAdvance=true;node("recent_filter_$i").performScrollTo();settle();node("recent_filter_$i").assertIsDisplayed()
   val b=bounds("recent_filter_$i")
   if(top==null){top=b.top;bottom=b.bottom}else{assertEquals(top!!,b.top,.5f);assertEquals(bottom!!,b.bottom,.5f)}
   assertTrue(b.left>=rowBounds.left-.5f&&b.right<=rowBounds.right+.5f);assertTrue(b.bottom-b.top>=44*rule.density.density)
   val results=mutableListOf<TextLayoutResult>();node("recent_filter_label_$i").performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(results)}
   assertTrue(results.isNotEmpty());for(t in results){assertEquals(1,t.lineCount);assertFalse("label $i: ${t.layoutInput.text}",t.hasVisualOverflow);assertFalse(t.isLineEllipsized(0))}
  }
  rule.mainClock.autoAdvance=true;node("recent_filter_0").performScrollTo();settle()
 }
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsSingleRowUiTest:SingleRowFixture(){
 @Test fun defaultOnAndFinder(){assertTrue(LdSettings().recentsSingleRow);assertTrue(SettingsFinder.search("One-row call filters").any{it.id=="recents-single-row"&&it.route=="settings:recents"});assertNotEquals(K.recentsSingleRow,K.roundedHomeButtons);assertNotEquals(K.recentsSingleRow,K.recentsOverview);start();row();visibleRow();shot("ROW520-day")}
 @Test fun switchChangesOnlyOnePreferenceAndRestoresGrid(){start();mount(true);rule.mainClock.autoAdvance=true;node("recents_single_row_switch").performScrollTo();settle();val before=LdApp.settings.value;node("recents_single_row_switch").performClick();await{LdApp.settings.value.recentsSingleRow};assertEquals(before.copy(recentsSingleRow=true),LdApp.settings.value);shot("ROW520-settings-on");node("recents_single_row_switch").performClick();await{!LdApp.settings.value.recentsSingleRow};assertEquals(before,LdApp.settings.value);mount();visibleGrid();shot("ROW520-day-off")}
 @Test fun selectionSearchDataAndRoundedChoiceSurvive(){start();row();val old=state.recents.toList();for((i,name)in listOf(1 to "Papa",2 to "Rahul Kumar Sharma",3 to "Priya Verma")){hit(i);node("recent_filter_$i").assertIsSelected();rule.onNodeWithText(name,true).assertExists();listOf("Papa","Rahul Kumar Sharma","Priya Verma").filter{it!=name}.forEach{rule.onNodeWithText(it,true).assertDoesNotExist()}};row(false);visibleGrid();node("recent_filter_3").assertIsSelected();rounded();row();visibleRow();node("recent_filter_3").assertIsSelected();hit(0);rule.onNode(hasSetTextAction(),true).performTextReplacement("Priya");settle();rule.onNodeWithText("Papa",true).assertDoesNotExist();row(false);row();rule.onNodeWithText("Priya Verma",true).assertExists();node("search_clear").performClick();settle();visibleRow();assertEquals(old,state.recents);assertTrue(LdApp.settings.value.roundedHomeButtons);assertNull(state.pendingCall)}
 @Test fun originalPanelAndEmptyKeepOneRow(){start(false);row();visibleRow();node("recent_filter_grid").assertDoesNotExist();rule.runOnUiThread{state.recents=emptyList()};settle();visibleRow();node("recent_clear").assertDoesNotExist();node("insights_open").assertDoesNotExist();assertNull(state.pendingCall)}
 @Test fun roundedAndBottomSearchPreserveBounds(){start();row();val before=(0..3).associateWith{bounds("recent_filter_$it")};val top=bounds("top_bar");rounded();visibleRow();for(i in 0..3)assertEquals(before[i],bounds("recent_filter_$i"));assertEquals(top,bounds("top_bar"));shot("ROW520-day-rounded");runBlocking{ctx.repo.set(K.searchBottom,true)};await{LdApp.settings.value.searchBottom};visibleRow();hit(3);node("recent_filter_3").assertIsSelected();assertNull(state.pendingCall)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsSingleRowNightUiTest:SingleRowFixture(){
 @Test fun smallNightLargeAllLabelsAndBothShapes(){start(night=true);row();visibleRow();shot("ROW520-night");rounded();visibleRow();shot("ROW520-night-rounded");node("recent_filter_single_row").performTouchInput{swipeLeft()};settle();saveShot(rule,"ROW520-gesture-debug");assertEquals("recents",state.tab);node("recent_filter_3").assertIsDisplayed();val incoming=bounds("recent_filter_3");val viewport=bounds("recent_filter_single_row");assertTrue(incoming.left>=viewport.left&&incoming.right<=viewport.right+.5f);shot("ROW520-night-swiped");hit(3);visibleRow();node("recent_filter_3").assertIsSelected();row(false);visibleGrid();shot("ROW520-night-off");row();visibleRow();runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};visibleRow();shot("ROW520-night-normal");assertNull(state.pendingCall)}
 @Test fun smallDayLargeWithoutOverview(){start(false,night=true);runBlocking{ctx.repo.set(K.theme,"daylight")};await{LdApp.settings.value.theme=="daylight"};row();visibleRow();shot("ROW520-day-small-large");rounded();visibleRow();shot("ROW520-day-small-large-rounded");node("recent_filter_single_row").performTouchInput{swipeLeft()};settle();saveShot(rule,"ROW520-gesture-debug");assertEquals("recents",state.tab);node("recent_filter_3").assertIsDisplayed();shot("ROW520-day-large-swiped");assertNull(state.pendingCall)}
}
