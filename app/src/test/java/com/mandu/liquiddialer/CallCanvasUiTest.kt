package com.mandu.liquiddialer
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
@OptIn(ExperimentalTestApi::class)
abstract class CallCanvasUiFixture{
 @get:Rule(order=0) val initialSettings=object:org.junit.rules.ExternalResource(){override fun before(){
  // These workspace tests own a plain host. Do not launch MainActivity startup
  // loads, onboarding or animations before installing the test composition.
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false};ctx.repo.flow.first{it.onboarded&&!it.motion}}
 }}
 @get:Rule(order=1) val rule=createAndroidComposeRule<ComponentActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 protected val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 protected val number="+919876543210"
 protected val sketch=listOf(CallCanvas.Stroke(1,2,listOf(CallCanvas.Point(1800,6500),CallCanvas.Point(1800,3500),CallCanvas.Point(4500,1500),CallCanvas.Point(7200,3500),CallCanvas.Point(7200,6500),CallCanvas.Point(1800,6500))),CallCanvas.Stroke(2,2,listOf(CallCanvas.Point(4000,6500),CallCanvas.Point(4000,4600),CallCanvas.Point(5300,4600),CallCanvas.Point(5300,6500))),CallCanvas.Stroke(3,3,listOf(CallCanvas.Point(500,8500),CallCanvas.Point(8800,8500),CallCanvas.Point(7900,7500),CallCanvas.Point(8800,8500),CallCanvas.Point(7900,9500))))
 private var focus:androidx.compose.ui.focus.FocusManager?=null
 protected fun ComponentActivity.mountTestPage(content:@Composable ()->Unit){val host=ComposeView(this);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;content()};setContentView(host)}
 protected fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 protected fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 protected fun seed(night:Boolean=false){settle();File(ctx.filesDir,"call_canvas.json").delete();File(ctx.filesDir,"call_canvas.json.bak").delete();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.refinedHeaders]=true;it[K.callCanvas]=false}};await{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.callCanvas}}}
 protected fun show(night:Boolean=false,empty:Boolean=false){seed(night);if(!empty)CallCanvas.save(ctx,number,"",sketch);rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallCanvasWorkspace(number,{})}}};await{rule.onAllNodesWithTag("canvas_paper",true).fetchSemanticsNodes().isNotEmpty()}}
 protected fun scrollTo(tag:String){
  rule.mainClock.autoAdvance=true
  // Center the requested control with bounded scrolls. Compose performScrollTo
  // can loop forever when a tall canvas and enlarged text exceed the viewport.
  repeat(4){val n=rule.onNodeWithTag(tag,true).fetchSemanticsNode();val root=rule.onRoot().fetchSemanticsNode();val delta=n.positionInRoot.y+n.size.height/2f-root.size.height/2f
   if(n.positionInRoot.y>=70&&n.positionInRoot.y+n.size.height<=root.size.height-20){rule.mainClock.autoAdvance=false;settle();return}
   rule.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,delta)};rule.waitForIdle()
  }
  rule.mainClock.autoAdvance=false;settle()
 }
 protected fun stroke(){rule.mainClock.autoAdvance=true;scrollTo("canvas_paper");rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithTag("canvas_paper",true).performTouchInput{down(androidx.compose.ui.geometry.Offset(width*0.15f,height*0.7f));moveTo(androidx.compose.ui.geometry.Offset(width*0.35f,height*0.35f));moveTo(androidx.compose.ui.geometry.Offset(width*0.65f,height*0.7f));up()};settle()}
 protected fun field(tag:String)=rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true)
 protected fun click(tag:String){rule.mainClock.autoAdvance=true;if(!tag.startsWith("canvas_confirm"))scrollTo(tag);rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(tag,true).performClick();settle()}
 @After fun clean(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1000);rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.refinedHeaders]=false;it[K.callCanvas]=false}}}
}



@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallCanvasUiTest:CallCanvasUiFixture(){
 @Test fun actualFingerStrokeSaveUndoRedoAndReopen(){show(empty=true);stroke();rule.onNodeWithTag("canvas_state",true).assertTextEquals("Unsaved drawing");assertTrue(CallCanvas.read(ctx,number).strokes.isEmpty());click("canvas_save");await{CallCanvas.read(ctx,number).strokes.size==1};click("canvas_undo");rule.onNodeWithTag("canvas_state",true).assertTextEquals("Unsaved drawing");click("canvas_redo");click("canvas_reload");await{rule.onAllNodesWithTag("canvas_state",true).fetchSemanticsNodes().any{it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text){emptyList()}.any{it.text=="Saved · 1 strokes"}}};assertEquals(1,CallCanvas.read(ctx,number).strokes.size)}
 @Test fun cancelledFingerAndMultitouchDoNotCommitInk(){show(empty=true);rule.mainClock.autoAdvance=true;scrollTo("canvas_paper");rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithTag("canvas_paper",true).performTouchInput{down(androidx.compose.ui.geometry.Offset(width*.2f,height*.2f));moveTo(androidx.compose.ui.geometry.Offset(width*.5f,height*.5f));cancel()};settle();rule.onNodeWithTag("canvas_state",true).assertTextEquals("Saved · 0 strokes",includeEditableText=false);rule.onNodeWithTag("canvas_paper",true).performTouchInput{down(0,androidx.compose.ui.geometry.Offset(width*.2f,height*.2f));down(1,androidx.compose.ui.geometry.Offset(width*.6f,height*.6f));up(0);up(1)};settle();rule.onNodeWithTag("canvas_state",true).assertTextEquals("Saved · 0 strokes");assertTrue(CallCanvas.read(ctx,number).strokes.isEmpty());stroke();rule.onNodeWithTag("canvas_state",true).assertTextEquals("Unsaved drawing")}
 @Test fun savedInkSurvivesNewWorkspaceMount(){show(empty=true);stroke();click("canvas_save");await{CallCanvas.read(ctx,number).strokes.size==1};val saved=CallCanvas.read(ctx,number);rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallCanvasWorkspace(number,{})}}};await{rule.onAllNodesWithTag("canvas_paper",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("canvas_state",true).assertTextEquals("Saved · 1 strokes");assertEquals(saved,CallCanvas.read(ctx,number))}
 @Test fun clearNeedsConfirmationAndSaveIsExplicit(){show();click("canvas_clear");assertEquals(sketch,CallCanvas.read(ctx,number).strokes);click("canvas_confirm_no");click("canvas_clear");click("canvas_confirm_yes");assertEquals(sketch,CallCanvas.read(ctx,number).strokes);click("canvas_save");await{CallCanvas.read(ctx,number).strokes.isEmpty()}}
 @Test fun paletteWidthAndActualDrawingPixels(){show();click("canvas_ink_2");click("canvas_width_3");stroke();click("canvas_save");await{CallCanvas.read(ctx,number).strokes.size==4};val last=CallCanvas.read(ctx,number).strokes.last();assertEquals(2,last.ink);assertEquals(3,last.width);rule.mainClock.autoAdvance=true;scrollTo("canvas_paper");settle();saveShot(rule,"CANVAS418-day-drawn")}
 @Test fun backAndReloadRequireDiscard(){show();stroke();click("page_back");await{rule.onAllNodesWithTag("canvas_confirm_yes",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("canvas_confirm_yes",true).assertExists();click("canvas_confirm_no");click("canvas_reload");await{rule.onAllNodesWithTag("canvas_confirm_yes",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("canvas_confirm_yes",true).assertExists();assertEquals(sketch,CallCanvas.read(ctx,number).strokes);click("canvas_confirm_yes");await{rule.onAllNodesWithTag("canvas_state",true).fetchSemanticsNodes().any{it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text){emptyList()}.any{it.text=="Saved · 3 strokes"}}}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallCanvasSafetyUiTest:CallCanvasUiFixture(){
 @Test fun staleSaveKeepsNewerDiskAndUnsavedInk(){show();stroke();val old=CallCanvas.read(ctx,number);CallCanvas.save(ctx,number,old.raw,listOf(sketch.first()));click("canvas_save");await{rule.onAllNodesWithTag("canvas_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(listOf(sketch.first()),CallCanvas.read(ctx,number).strokes);rule.onNodeWithTag("canvas_state",true).assertTextEquals("Unsaved drawing")}
 @Test fun corruptFileDisablesCanvasPreservesData(){seed();File(ctx.filesDir,"call_canvas.json").writeText("broken");rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallCanvasWorkspace(number,{})}}};await{rule.onAllNodesWithTag("canvas_error",true).fetchSemanticsNodes().isNotEmpty()};rule.onAllNodesWithTag("canvas_paper",true).assertCountEquals(0);assertEquals("broken",File(ctx.filesDir,"call_canvas.json").readText())}
 @Test fun nightLargeDrawingPixels(){show(true);rule.mainClock.autoAdvance=true;scrollTo("canvas_paper");settle();saveShot(rule,"CANVAS418-night-large");click("canvas_clear");await{rule.onAllNodesWithTag("canvas_confirm_yes",true).fetchSemanticsNodes().isNotEmpty()};saveDialogShot("CANVAS419-night-confirm",0xff101a30)}
 @Test fun callEntryOffThenOnAndNoAutomaticCall(){seed();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Sheet({}){NotesSheet(number,"Support",{})}}}};settle();rule.onAllNodesWithTag("call_canvas_open",true).assertCountEquals(0);runBlocking{ctx.repo.set(K.callCanvas,true)};await{LdApp.settings.value.callCanvas};click("call_canvas_open");await{rule.onAllNodesWithTag("canvas_paper",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("canvas_identity",true).assertTextEquals(number);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity);saveShot(rule,"CANVAS418-call-sheet")}
 @Test fun overviewPixels(){seed();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallCanvasPage({})}}};settle();saveShot(rule,"CANVAS418-overview");assertFalse(LdApp.settings.value.callCanvas)}
}
