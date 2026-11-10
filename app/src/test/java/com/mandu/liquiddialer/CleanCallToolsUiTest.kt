package com.mandu.liquiddialer
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
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
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class CleanCallToolsUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+44 2025550123"
 private var selected="";private var generation=0
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()};settle()}
 private fun reveal(tag:String):SemanticsNodeInteraction{
  repeat(40){val n=rule.onNodeWithTag(tag,true);val r=n.getUnclippedBoundsInRoot();val sc=rule.onAllNodes(hasScrollAction(),true).onLast();val v=sc.getUnclippedBoundsInRoot();if(r.bottom>r.top&&r.top>=v.top&&r.bottom<=v.bottom)return n;sc.performTouchInput{if(r.top<v.top)swipeDown()else swipeUp()};settle()};error("Not visible $tag")
 }
 private fun show(theme:String="daylight",font:Float=1f,notes:Boolean=false,tools:Boolean=true){
  rule.mainClock.autoAdvance=false;settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font}};await{LdApp.settings.value.let{it.theme==theme&&it.fontScale==font}}
  val g=++generation;rule.runOnUiThread{rule.activity.setContent{key(g){LdThemeHost{
   if(notes)Sheet({}){NotesSheet(number,"बहुत लंबा दुकान का नाम · Supplier Office Delivery Department",{})}
   else PageFrame("Number details",{}){NumberSheet(number,null,rule.activity.state,rule.activity,{},{},{},onChecklist=if(tools){{selected="checklist"}}else null,onEstimate=if(tools){{selected="estimate"}}else null,onTimeline=if(tools){{selected="timeline"}}else null)}
  }}}};settle()
 }
 @Test fun compactRowTargetsRemainIndependentAndAtLeast48dp(){
  show();val tags=listOf("details_checklist","details_estimate","details_timeline");tags.forEach{reveal(it).assertIsDisplayed()}
  val bounds=tags.map{rule.onNodeWithTag(it,true).getUnclippedBoundsInRoot()};assertTrue(bounds.all{(it.right-it.left).value>=48&&(it.bottom-it.top).value>=48});assertTrue(bounds.zipWithNext().all{(a,b)->a.right<=b.left});assertEquals(bounds[0].top,bounds[1].top)
  tags.forEachIndexed{i,t->rule.onNodeWithTag(t,true).performClick();settle();assertEquals(listOf("checklist","estimate","timeline")[i],selected)};assertNull(rule.activity.state.confirmNumber);saveShot(rule,"CLEAN382-details-day")
 }
 @Test fun absentCallbacksNeverShowDeadTools(){show(tools=false);rule.onNodeWithTag("details_tools").assertDoesNotExist();rule.onNodeWithTag("details_checklist").assertDoesNotExist()}
 @Test fun longNameNightLargeFontNotesKeepsFieldAndSaveAccessible(){
  show("midnight",1.5f,notes=true);reveal("call_tools");saveShot(rule,"CLEAN382-notes-night-large");reveal("call_checklist_open").assertContentDescriptionEquals("Call checklist · baat ke topics");reveal("call_estimate_open").assertContentDescriptionEquals("Call calculator · quantity aur rate");reveal("call_timeline_open").assertContentDescriptionEquals("Call timeline · purani calls")
  rule.onNode(hasSetTextAction()).performTextInput("Private unsaved text");settle();val save=rule.onNodeWithText("Save note",useUnmergedTree=true);save.performScrollTo();settle();save.assertIsDisplayed();save.onParent().assertHasClickAction();saveShot(rule,"CLEAN382-notes-night-editor")
 }
 @Test fun notesDayToolRoutesRemainExactAndDraftRetained(){
  show(notes=true);rule.onNode(hasSetTextAction()).performTextInput("Keep my note");settle();saveShot(rule,"CLEAN382-notes-day");reveal("call_timeline_open").performClick();settle();await{rule.onAllNodesWithTag("timeline_number",true).fetchSemanticsNodes().isNotEmpty()};reveal("timeline_number").assertTextEquals(number);reveal("timeline_notes_back").performClick();settle();rule.onNode(hasSetTextAction()).assertTextEquals("Keep my note");assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun largeFontDetailsTilesWrapWithoutOverlap(){
  show("midnight",1.5f);reveal("details_tools");val tags=listOf("details_checklist","details_estimate","details_timeline");val b=tags.map{reveal(it).getUnclippedBoundsInRoot()};assertTrue(b.zipWithNext().all{(a,c)->a.right<=c.left});saveShot(rule,"CLEAN382-details-night-large")
 }
 @Test fun newIconsSupportEveryExistingStyle(){
  for(style in 0..7){val check=LdIcons.styled(LdIcons.Checklist,style);val calc=LdIcons.styled(LdIcons.Calculator,style);assertTrue(check.viewportWidth>0);assertTrue(calc.viewportWidth>0);assertNotEquals(check.name,calc.name)}
 }
}
