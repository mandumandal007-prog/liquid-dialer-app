package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class StopwatchFixture:LiveCallDeskFixture(){
 protected val number="+919876543210"
 protected fun setup(on:Boolean=true,night:Boolean=false){show(night=night);runBlocking{ctx.repo.set(K.deskStopwatch,on);ctx.repo.set(K.callCheckpoints,false)};await{LdApp.settings.value.deskStopwatch==on&&!LdApp.settings.value.callCheckpoints};open();if(on)click("desk_watch_toggle")}
 protected fun move(tag:String){repeat(40){val r=rule.onNodeWithTag(tag,true).getUnclippedBoundsInRoot();val v=rule.onNodeWithTag("call_desk_tools",true).getUnclippedBoundsInRoot();if(r.top>=v.top&&r.bottom<=v.bottom){settle();rule.onNodeWithTag(tag,true).assertIsDisplayed();return};rule.onNodeWithTag("call_desk_tools",true).performSemanticsAction(SemanticsActions.ScrollBy){it(0f,if(r.top<v.top)-150f else 150f)};settle()};error("Not fully visible $tag")}
 protected fun enter(s:String){move("desk_watch_label");rule.onNodeWithTag("desk_watch_label",true).onChildren().filter(hasSetTextAction())[0].performTextInput(s);settle()}
 protected fun start(){move("desk_watch_start");click("desk_watch_start")}
 protected fun lap(){org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(3));move("desk_watch_lap");click("desk_watch_lap")}
 protected fun pause(){move("desk_watch_start");click("desk_watch_start")}
 protected fun preview(){move("desk_watch_preview");click("desk_watch_preview")}
 @After fun resetWatch(){runBlocking{ctx.repo.set(K.deskStopwatch,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskStopwatchUiTest:StopwatchFixture(){
 @Test fun offNoEntry(){setup(false);rule.onAllNodesWithTag("desk_watch_toggle",true).assertCountEquals(0);rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun startsEmptyPauseAndLapControls(){setup();rule.onNodeWithTag("desk_watch_time",true).assertTextEquals("0:00:00.0");rule.onNodeWithTag("desk_watch_lap",true).assertIsNotEnabled();rule.onNodeWithTag("desk_watch_preview",true).assertIsNotEnabled();start();rule.onNodeWithTag("desk_watch_lap",true).assertIsEnabled();rule.onNodeWithTag("desk_watch_preview",true).assertIsNotEnabled();pause();rule.onNodeWithTag("desk_watch_lap",true).assertIsNotEnabled()}
 @Test fun explicitSaveExactPreviewAndLock(){setup();start();enter("Delivery discussion");lap();pause();preview();rule.onNodeWithTag("desk_watch_preview_number",true).assertTextEquals(number);val expected=rule.onNodeWithTag("desk_watch_preview_text",true).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text;assertTrue(Repo.notesFor(ctx,number).isEmpty());move("desk_watch_save");click("desk_watch_save");await{Repo.notesFor(ctx,number).size==1};assertEquals(expected,Repo.notesFor(ctx,number).single().text);rule.onAllNodesWithTag("desk_watch_start",true).assertCountEquals(0);rule.onAllNodesWithTag("desk_watch_lap",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,"+449876543210").isEmpty());move("desk_watch_state");saveShot(rule,"WATCH461-saved-day")}
 @Test fun cancelPreviewDoesNotSave(){setup();start();lap();pause();preview();move("desk_watch_cancel");click("desk_watch_cancel");rule.onAllNodesWithTag("desk_watch_save",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 @Test fun runningAfterPreviewStaleSaveRefused(){setup();start();lap();pause();preview();start();move("desk_watch_save");click("desk_watch_save");assertTrue(Repo.notesFor(ctx,number).isEmpty());rule.onNodeWithTag("desk_watch_error",true).assertTextContains("Not saved.",substring=true)}
 @Test fun resetCancelThenConfirm(){setup();start();enter("Keep label");lap();pause();move("desk_watch_reset");click("desk_watch_reset");click("desk_watch_reset_cancel");rule.onNodeWithTag("desk_watch_row_0",true).assertExists();click("desk_watch_reset");click("desk_watch_reset_confirm");rule.onAllNodesWithTag("desk_watch_row_0",true).assertCountEquals(0);move("desk_watch_time");rule.onNodeWithTag("desk_watch_time",true).assertTextEquals("0:00:00.0");assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 @Test fun hidePreservesTimerAndLaps(){setup();start();lap();move("desk_watch_toggle");click("desk_watch_toggle");org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(4));click("desk_watch_toggle");rule.onNodeWithTag("desk_watch_state",true).assertTextContains("Running",substring=true);rule.onNodeWithTag("desk_watch_row_0",true).assertExists();pause()}
 @Test fun ownOffClearsThenReenableEmpty(){setup();start();lap();runBlocking{ctx.repo.set(K.deskStopwatch,false)};await{rule.onAllNodesWithTag("desk_watch_toggle",true).fetchSemanticsNodes().isEmpty()};runBlocking{ctx.repo.set(K.deskStopwatch,true)};await{rule.onAllNodesWithTag("desk_watch_toggle",true).fetchSemanticsNodes().size==1};click("desk_watch_toggle");rule.onNodeWithTag("desk_watch_time",true).assertTextEquals("0:00:00.0");rule.onAllNodesWithTag("desk_watch_row_0",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 @Test fun invalidLabelDisablesLap(){setup();start();enter("x".repeat(81));rule.onNodeWithTag("desk_watch_lap",true).assertIsNotEnabled();pause()}
 @Test fun callReplacementClearsNoSave(){setup();start();lap();com.mandu.liquiddialer.telecom.CallManager.calls.value=listOf(deskShadowCall());settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,number).isEmpty())}
 @Test fun dayLabeledLapPixels(){setup();start();enter("Order preparation");lap();pause();move("desk_watch_row_0");saveShot(rule,"WATCH461-lap-day")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskStopwatchPixelsUiTest:StopwatchFixture(){
 @Test fun narrowNightPreviewWithControls(){setup(night=true);start();enter("ऑर्डर तैयार करना");lap();pause();preview();move("desk_watch_preview_number");saveShot(rule,"WATCH461-number-night-large");move("desk_watch_save");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();saveShot(rule,"WATCH461-preview-night-large");move("desk_watch_cancel");rule.onNodeWithTag("desk_watch_cancel",true).assertIsDisplayed();saveShot(rule,"WATCH461-cancel-night-large")}
 @Test fun settingDefaultOffNoAutoEnable(){seed(false,night=true);runBlocking{ctx.repo.set(K.deskStopwatch,false);ctx.repo.set(K.callCheckpoints,false)};await{!LdApp.settings.value.deskStopwatch};mount{DeskStopwatchPage{}};saveShot(rule,"WATCH461-settings-night-large");rule.onAllNodesWithText("Desk interval stopwatch",useUnmergedTree=true)[1].performClick();await{LdApp.settings.value.deskStopwatch};assertFalse(LdApp.settings.value.liveCallDesk);assertFalse(LdApp.settings.value.callCheckpoints)}
}
