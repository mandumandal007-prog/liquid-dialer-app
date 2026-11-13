package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class CallTallyUiTest {
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false}}
 }}
 private val dispatcher=kotlinx.coroutines.test.StandardTestDispatcher()
 @get:Rule(order=1) val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>(effectContext=dispatcher)
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private val n="+919876543210"
 private fun settle(){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);dispatcher.scheduler.runCurrent();rule.waitForIdle()}
 private fun await(tag:String){rule.waitUntil(15000){settle();rule.onAllNodesWithTag(tag,true).fetchSemanticsNodes().isNotEmpty()}}
 private fun scrollTo(tag:String){rule.mainClock.autoAdvance=true;repeat(5){val n=rule.onNodeWithTag(tag,true).fetchSemanticsNode();val root=rule.onRoot().fetchSemanticsNode();val delta=n.positionInRoot.y+n.size.height/2f-root.size.height/2f
 if(n.positionInRoot.y>=75&&n.positionInRoot.y+n.size.height<root.size.height-20){rule.mainClock.autoAdvance=false;settle();return}
 rule.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,delta)};rule.waitForIdle()};rule.mainClock.autoAdvance=false;settle()}
 private fun click(tag:String){scrollTo(tag);rule.onNodeWithTag(tag,true).assertIsEnabled().performClick();settle()}
 private var focus:androidx.compose.ui.focus.FocusManager?=null
 private fun waitFor(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 private fun show(night:Boolean=false,notes:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.callTally]=false}};waitFor{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.callTally}};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{if(notes)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}else CallTallyWorkspace({})}};rule.activity.setContentView(host)};settle()}
 private fun input(tag:String,s:String){scrollTo(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(s);settle()}
 @After fun reset(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};settle();rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.callTally]=false}}}
 private fun add(name:String,value:String="0"){input("tally_name",name);input("tally_value",value);click("tally_apply")}
 @Test fun namedCountersExactTotalsAndStep(){show();add("Rice bags","5");add("Sugar bags","7");rule.onNodeWithTag("tally_total",true).assertTextEquals("12");input("tally_step","5");click("tally_plus_0");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("10");click("tally_minus_1");rule.onNodeWithTag("tally_total",true).assertTextEquals("12")}
 @Test fun undoRedoAndNewEditBranch(){show();add("Bags","2");click("tally_plus_0");click("tally_undo");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("2");click("tally_redo");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("3");click("tally_undo");click("tally_plus_0");rule.onNodeWithTag("tally_redo",true).assertIsNotEnabled()}
 @Test fun boundsDisableButtonsNoClamping(){show();add("A","0");rule.onNodeWithTag("tally_minus_0",true).assertIsNotEnabled();add("B","1000000000");rule.onNodeWithTag("tally_plus_1",true).assertIsNotEnabled();input("tally_step","1000001");rule.onNodeWithTag("tally_plus_0",true).assertIsNotEnabled();rule.onNodeWithTag("tally_step_error",true).assertExists()}
 @Test fun duplicateAndMalformedQuantityDoNotChangeBoard(){show();add("Rice bags","5");add(" rice   BAGS ","10");rule.onNodeWithTag("tally_error",true).assertExists();rule.onNodeWithTag("tally_total",true).assertTextEquals("5");input("tally_name","Sugar");input("tally_value","1.5");click("tally_apply");rule.onNodeWithTag("tally_error",true).assertExists();rule.onNodeWithTag("tally_count",true).assertTextEquals("Raw sum · 1 /12 counters")}
 @Test fun editRenameAndQuantityRequiresApply(){show();add("Rice bags","5");click("tally_edit_0");input("tally_name","Rice sacks");input("tally_value","12");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("5");click("tally_apply");rule.onNodeWithTag("tally_label_0",true).assertTextEquals("Rice sacks");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("12");click("tally_undo");rule.onNodeWithTag("tally_label_0",true).assertTextEquals("Rice bags")}
 @Test fun cancelEditAndRemoveConfirmationUndo(){show();add("Bags","3");click("tally_edit_0");input("tally_value","99");click("tally_cancel_edit");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("3");click("tally_remove_0");rule.onNodeWithTag("tally_quantity_0",true).assertExists();rule.onNodeWithTag("tally_keep",true).performClick();settle();click("tally_remove_0");rule.onNodeWithTag("tally_confirm",true).performClick();settle();rule.onNodeWithTag("tally_empty",true).assertExists();click("tally_undo");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("3")}
 @Test fun zeroConfirmationKeepsNamesAndUndoRestores(){show();add("Rice","3");add("Sugar","7");click("tally_zero");rule.onNodeWithTag("tally_keep",true).performClick();settle();rule.onNodeWithTag("tally_total",true).assertTextEquals("10");click("tally_zero");rule.onNodeWithTag("tally_confirm",true).performClick();settle();rule.onNodeWithTag("tally_total",true).assertTextEquals("0");rule.onNodeWithTag("tally_label_0",true).assertTextEquals("Rice");click("tally_undo");rule.onNodeWithTag("tally_total",true).assertTextEquals("10")}
 @Test fun defaultOffCallEntryDraftReturnAndNoFileEffects(){show(notes=true);rule.onAllNodesWithTag("call_tally_open",true).assertCountEquals(0);rule.onNode(hasSetTextAction()).performTextInput("Unfinished count discussion");val files=ctx.filesDir.walkTopDown().filter{it.isFile&&it.extension=="json"}.associate{it.path to it.readBytes()};runBlocking{ctx.repo.set(K.callTally,true)};await("call_tally_open");click("call_tally_open");add("Bags","2");click("tally_back");rule.onNodeWithTag("tally_keep",true).performClick();settle();rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("2");click("tally_back");rule.onNodeWithTag("tally_confirm",true).performClick();settle();rule.onNodeWithText("Unfinished count discussion").assertExists();click("call_tally_open");rule.onNodeWithTag("tally_empty",true).assertExists();files.forEach{(path,bytes)->assertArrayEquals(bytes,File(path).readBytes())};assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun reopenResetsSessionAndNoExternalActivity(){show();add("Bags","4");show();rule.onNodeWithTag("tally_total",true).assertTextEquals("0");rule.onNodeWithTag("tally_empty",true).assertExists();assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun dayCounterPixels(){show();add("Rice bags","24");add("Sugar bags","12");input("tally_step","5");scrollTo("tally_item_0");settle();saveShot(rule,"TALLY430-day")}
 @Test fun nightLargeLongNameAndTotalPixels(){show(true);add("Rice premium large family pack bags with matching unit","1000000000");add("Sugar bags","1000000000");scrollTo("tally_total");settle();saveShot(rule,"TALLY430-night-total");scrollTo("tally_item_0");settle();saveShot(rule,"TALLY430-night-counter")}
 @Test fun settingsAndFinderPixels(){show();assertFalse(LdSettings().callTally);assertTrue(SettingsFinder.all.any{it.id=="call-tally"&&it.route=="calltally"});rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{CallTallyPage({})}};rule.activity.setContentView(host)};settle();saveShot(rule,"TALLY430-settings");click("tally_settings_open");rule.onNodeWithTag("tally_empty",true).assertExists()}
}
