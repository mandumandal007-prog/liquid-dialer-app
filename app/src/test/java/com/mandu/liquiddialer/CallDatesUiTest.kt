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
class CallDatesUiTest {
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
 private fun show(night:Boolean=false,notes:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.callDates]=false}};waitFor{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.callDates}};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{if(notes)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}else CallDatesWorkspace({})}};rule.activity.setContentView(host)};settle()}
 private fun input(tag:String,s:String){scrollTo(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(s);settle()}
 @After fun reset(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};settle();rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.callDates]=false}}}
 @Test fun explicitShiftAndStaleResultClears(){show();input("dates_first","2026-11-13");rule.onAllNodesWithTag("dates_result",true).assertCountEquals(0);click("dates_calculate");rule.onNodeWithTag("dates_value",true).assertTextEquals("2026-11-20 · Friday");input("dates_days","-1");rule.onAllNodesWithTag("dates_result",true).assertCountEquals(0);click("dates_calculate");rule.onNodeWithTag("dates_value",true).assertTextEquals("2026-11-12 · Thursday")}
 @Test fun signedGapSwapAndSameDate(){show();click("dates_mode_gap");input("dates_first","2026-11-13");input("dates_last","2026-11-20");click("dates_calculate");rule.onNodeWithTag("dates_value",true).assertTextEquals("7 calendar days");click("dates_swap");rule.onAllNodesWithTag("dates_result",true).assertCountEquals(0);click("dates_calculate");rule.onNodeWithTag("dates_value",true).assertTextEquals("-7 calendar days");input("dates_last","2026-11-20");click("dates_calculate");rule.onNodeWithTag("dates_value",true).assertTextEquals("0 calendar days")}
 @Test fun weekdaysExcludesEndAndNotHolidays(){show();click("dates_mode_weekdays");input("dates_first","2026-12-25");input("dates_last","2026-12-26");click("dates_calculate");rule.onNodeWithTag("dates_value",true).assertTextEquals("1 Monday-Friday dates");rule.onNodeWithTag("dates_rules",true).assertTextContains("Holidays NOT excluded",substring=true);click("dates_swap");click("dates_calculate");await("dates_error");rule.onAllNodesWithTag("dates_result",true).assertCountEquals(0)}
 @Test fun invalidDateDoesNotNormalizeOrAddHistory(){show();input("dates_first","2026-02-30");click("dates_calculate");await("dates_error");rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 0 /10");input("dates_first","2024-02-28");input("dates_days","1");click("dates_calculate");rule.onNodeWithTag("dates_value",true).assertTextEquals("2024-02-29 · Thursday");rule.onAllNodesWithTag("dates_error",true).assertCountEquals(0)}
 @Test fun shiftBoundariesAndNoAutomaticToday(){show();rule.onNodeWithTag("dates_calculate",true).assertIsNotEnabled();input("dates_first","2199-12-31");input("dates_days","1");click("dates_calculate");await("dates_error");input("dates_first","1900-01-01");input("dates_days","-1");click("dates_calculate");await("dates_error")}
 @Test fun modeClearsResultAndKeepsTypedDates(){show();input("dates_first","2026-11-13");click("dates_calculate");click("dates_mode_gap");rule.onAllNodesWithTag("dates_result",true).assertCountEquals(0);input("dates_last","2026-11-20");click("dates_calculate");rule.onNodeWithTag("dates_value",true).assertTextEquals("7 calendar days")}
 @Test fun historyBoundedAndIndependentClear(){show();input("dates_first","2026-11-13");repeat(12){click("dates_calculate")};rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 10 /10");click("dates_history_clear");rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 0 /10");rule.onNodeWithTag("dates_value",true).assertExists();click("dates_clear");rule.onAllNodesWithTag("dates_result",true).assertCountEquals(0);rule.onNodeWithTag("dates_calculate",true).assertIsNotEnabled()}
 @Test fun sessionReopenClearsAndNoExternalActivity(){show();input("dates_first","2026-11-13");click("dates_calculate");show();rule.onNodeWithTag("dates_history_count",true).assertTextEquals("Session history: 0 /10");rule.onNodeWithTag("dates_calculate",true).assertIsNotEnabled();assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun callEntryOffOnDraftAndFilesUnchanged(){show(notes=true);rule.onAllNodesWithTag("call_dates_open",true).assertCountEquals(0);rule.onNode(hasSetTextAction()).performTextInput("Unfinished date discussion");val files=ctx.filesDir.walkTopDown().filter{it.isFile&&it.extension=="json"}.associate{it.path to it.readBytes()};runBlocking{ctx.repo.set(K.callDates,true)};await("call_dates_open");click("call_dates_open");input("dates_first","2026-11-13");click("dates_calculate");click("dates_back");rule.onNodeWithText("Unfinished date discussion").assertExists();files.forEach{(path,bytes)->assertArrayEquals(bytes,File(path).readBytes())};assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun dayShiftPixels(){show();input("dates_first","2026-11-13");click("dates_calculate");scrollTo("dates_result");settle();saveShot(rule,"DATES429-day")}
 @Test fun nightLargeWeekdaysAndErrorPixels(){show(true);click("dates_mode_weekdays");input("dates_first","2026-11-13");input("dates_last","2026-11-20");click("dates_calculate");scrollTo("dates_result");settle();saveShot(rule,"DATES429-night-large");click("dates_swap");click("dates_calculate");scrollTo("dates_error");settle();saveShot(rule,"DATES429-error")}
 @Test fun settingsAndFinderPixels(){show();assertFalse(LdSettings().callDates);assertTrue(SettingsFinder.all.any{it.id=="call-dates"&&it.route=="calldates"});rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{CallDatesPage({})}};rule.activity.setContentView(host)};settle();saveShot(rule,"DATES429-settings");click("dates_settings_open");rule.onNodeWithTag("dates_first",true).assertExists()}
}
