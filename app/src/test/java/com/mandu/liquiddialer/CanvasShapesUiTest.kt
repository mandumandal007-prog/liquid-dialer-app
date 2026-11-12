package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CanvasShapesUiTest:CallCanvasUiFixture(){
 private fun choose(name:String){scrollTo("canvas_tool_$name");rule.onNodeWithTag("canvas_tool_$name",true).performClick();settle()}
 private fun shapeDrag(){scrollTo("canvas_paper");rule.onNodeWithTag("canvas_paper",true).performTouchInput{down(androidx.compose.ui.geometry.Offset(width*.2f,height*.2f));moveTo(androidx.compose.ui.geometry.Offset(width*.5f,height*.5f));moveTo(androidx.compose.ui.geometry.Offset(width*.8f,height*.8f));up()};settle()}

 @Test fun lineSavesOnlyTwoPointsAndKeepsPalette(){show(empty=true);choose("line");click("canvas_ink_2");click("canvas_width_3");shapeDrag();click("canvas_save");await{CallCanvas.read(ctx,number).strokes.size==1};val s=CallCanvas.read(ctx,number).strokes.single();assertEquals(2,s.points.size);assertEquals(2,s.ink);assertEquals(3,s.width)}
 @Test fun boxIsOneUndoStepWithRedoAndExplicitSave(){show(empty=true);choose("box");shapeDrag();assertTrue(CallCanvas.read(ctx,number).strokes.isEmpty());click("canvas_undo");rule.onNodeWithTag("canvas_state",true).assertTextEquals("Saved · 0 strokes");click("canvas_redo");click("canvas_save");await{CallCanvas.read(ctx,number).strokes.size==1};val ps=CallCanvas.read(ctx,number).strokes.single().points;assertEquals(5,ps.size);assertEquals(ps.first(),ps.last())}
 @Test fun ovalAndArrowPersistAsOrdinaryInk(){show(empty=true);choose("oval");shapeDrag();choose("arrow");shapeDrag();click("canvas_save");await{CallCanvas.read(ctx,number).strokes.size==2};assertEquals(listOf(49,5),CallCanvas.read(ctx,number).strokes.map{it.points.size});rule.mainClock.autoAdvance=true;scrollTo("canvas_paper");settle();saveShot(rule,"SHAPES422-day")}
 @Test fun cancelledShapeAndTapDoNotCommit(){show(empty=true);choose("arrow");rule.mainClock.autoAdvance=true;scrollTo("canvas_paper");rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithTag("canvas_paper",true).performTouchInput{click()};settle();rule.onNodeWithTag("canvas_state",true).assertTextEquals("Saved · 0 strokes");rule.onNodeWithTag("canvas_paper",true).performTouchInput{down(androidx.compose.ui.geometry.Offset(width*.2f,height*.2f));moveTo(androidx.compose.ui.geometry.Offset(width*.7f,height*.7f));cancel()};settle();rule.onNodeWithTag("canvas_state",true).assertTextEquals("Saved · 0 strokes")}
 @Test fun penRemainsDefaultAndCanBeReselected(){show(empty=true);stroke();choose("line");shapeDrag();choose("pen");stroke();click("canvas_save");await{CallCanvas.read(ctx,number).strokes.size==3};val sizes=CallCanvas.read(ctx,number).strokes.map{it.points.size};assertTrue(sizes[0]>2);assertEquals(2,sizes[1]);assertTrue(sizes[2]>2)}
 @Test fun nightLargeToolRowCanReachEveryTool(){show(night=true,empty=true);choose("oval");rule.onNodeWithTag("canvas_tool_hint",true).assertTextContains("Oval",substring=true);shapeDrag();choose("box");shapeDrag();choose("arrow");rule.mainClock.autoAdvance=true;scrollTo("canvas_tools");settle();saveShot(rule,"SHAPES422-night-tools")}
}
