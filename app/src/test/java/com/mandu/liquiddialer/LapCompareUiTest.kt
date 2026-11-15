package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class LapCompareFixture:StopwatchFixture(){
 protected fun prepared(on:Boolean=true,night:Boolean=false,label:String="First task"){
  setup(night=night);runBlocking{ctx.repo.set(K.lapCompare,on)};await{LdApp.settings.value.lapCompare==on};start();enter(label);lap();enter("Second task");lap();pause()
 }
 protected fun compare(){move("lap_compare_toggle");click("lap_compare_toggle")}
 @After fun resetCompare(){runBlocking{ctx.repo.set(K.lapCompare,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LapCompareUiTest:LapCompareFixture(){
 @Test fun offNoEntryTimerUnaffected(){prepared(false);rule.onAllNodesWithTag("lap_compare_toggle",true).assertCountEquals(0);rule.onNodeWithTag("desk_watch_row_0",true).assertExists()}
 @Test fun selectionStartsEmptyAndReadOnly(){prepared();compare();rule.onNodeWithTag("lap_compare_row_0",true).assertIsOff();rule.onNodeWithTag("lap_compare_row_1",true).assertIsOff();rule.onNodeWithTag("lap_compare_empty",true).assertExists();assertTrue(Repo.notesFor(ctx,number).isEmpty());move("lap_compare_row_0");click("lap_compare_row_0");rule.onNodeWithTag("lap_compare_count",true).assertTextEquals("1 selected");assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 @Test fun allClearAndOrderKeepSelection(){prepared();compare();move("lap_compare_all");click("lap_compare_all");rule.onNodeWithTag("lap_compare_count",true).assertTextEquals("2 selected");move("lap_compare_order");click("lap_compare_order");rule.onNodeWithTag("lap_compare_order",true).onChildren().filter(hasText("Order: longest first")).assertCountEquals(1);rule.onNodeWithTag("lap_compare_row_0",true).assertIsOn();rule.onNodeWithTag("lap_compare_row_1",true).assertIsOn();move("lap_compare_clear");click("lap_compare_clear");rule.onAllNodesWithTag("lap_compare_count",true).assertCountEquals(0);rule.onNodeWithTag("lap_compare_empty",true).assertExists()}
 @Test fun resumeDiscardsComparison(){prepared();compare();move("lap_compare_all");click("lap_compare_all");start();rule.onAllNodesWithTag("lap_compare_toggle",true).assertCountEquals(0);pause();compare();rule.onNodeWithTag("lap_compare_row_0",true).assertIsOff();rule.onNodeWithTag("lap_compare_empty",true).assertExists()}
 @Test fun offReenableEmptyKeepsLaps(){prepared();compare();move("lap_compare_all");click("lap_compare_all");runBlocking{ctx.repo.set(K.lapCompare,false)};await{rule.onAllNodesWithTag("lap_compare_toggle",true).fetchSemanticsNodes().isEmpty()};runBlocking{ctx.repo.set(K.lapCompare,true)};await{rule.onAllNodesWithTag("lap_compare_toggle",true).fetchSemanticsNodes().size==1};compare();rule.onNodeWithTag("lap_compare_empty",true).assertExists();rule.onNodeWithTag("desk_watch_row_0",true).assertExists();assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 @Test fun hideReopenClearsSelection(){prepared();compare();move("lap_compare_all");click("lap_compare_all");move("lap_compare_toggle");click("lap_compare_toggle");click("lap_compare_toggle");rule.onNodeWithTag("lap_compare_empty",true).assertExists()}
 @Test fun saveOriginalChronologicalSummaryNotComparison(){prepared();compare();move("lap_compare_all");click("lap_compare_all");preview();val expected=rule.onNodeWithTag("desk_watch_preview_text",true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text;move("desk_watch_save");click("desk_watch_save");await{Repo.notesFor(ctx,number).size==1};assertEquals(expected,Repo.notesFor(ctx,number).single().text);assertFalse(expected.contains("Subtotal"));assertTrue(expected.indexOf("First task")<expected.indexOf("Second task"));compare();rule.onNodeWithTag("lap_compare_empty",true).assertExists()}
 @Test fun callChangeNoWrite(){prepared();compare();com.mandu.liquiddialer.telecom.CallManager.calls.value=listOf(deskShadowCall());settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 @Test fun daySelectedSummaryPixels(){prepared();compare();move("lap_compare_all");click("lap_compare_all");move("lap_compare_remainder");rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"COMPARE464-summary-day")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LapComparePixelsUiTest:LapCompareFixture(){
 @Test fun narrowNightRowsAndStats(){prepared(night=true,label="ऑर्डर तैयार करना");compare();move("lap_compare_all");click("lap_compare_all");move("lap_compare_row_0");saveShot(rule,"COMPARE464-selection-night-large");move("lap_compare_remainder");rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"COMPARE464-summary-night-large")}
 @Test fun settingOffNoAutoEnable(){seed(false,night=true);runBlocking{ctx.repo.set(K.lapCompare,false);ctx.repo.set(K.deskStopwatch,false)};await{!LdApp.settings.value.lapCompare&&!LdApp.settings.value.deskStopwatch};mount{LapComparePage{}};saveShot(rule,"COMPARE464-settings-night-large");rule.onAllNodesWithText("Stopwatch lap comparison",useUnmergedTree=true)[1].performClick();await{LdApp.settings.value.lapCompare};assertFalse(LdApp.settings.value.liveCallDesk);assertFalse(LdApp.settings.value.deskStopwatch)}
}
