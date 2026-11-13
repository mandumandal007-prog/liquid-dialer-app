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
class CallConverterUiTest {
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
 private fun show(night:Boolean=false,notes:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.callConverter]=false}};waitFor{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.callConverter}};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{if(notes)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}else CallConverterWorkspace({})}};rule.activity.setContentView(host)};settle()}
 private fun input(s:String){scrollTo("converter_input");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("converter_input")),true).performTextReplacement(s);settle()}
 @After fun reset(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};settle();rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.callConverter]=false}}}
 @Test fun convertExplicitNotAutoAndStaleResultClears(){show();input("1");rule.onAllNodesWithTag("converter_result",true).assertCountEquals(0);click("converter_convert");rule.onNodeWithTag("converter_value",true).assertTextEquals("= 3.28083989501312336 Int'l foot (ft)");input("2");rule.onAllNodesWithTag("converter_result",true).assertCountEquals(0);rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 1 / 10");click("converter_convert");rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 2 / 10")}
 @Test fun categoryResetsUnitsButNotTypedInput(){show();input("2.5");click("converter_kind_mass");click("converter_from_kg");click("converter_to_g");click("converter_convert");rule.onNodeWithTag("converter_value",true).assertTextEquals("= 2500 Gram (g)");click("converter_kind_volume");rule.onNodeWithTag("converter_from_label",true).assertTextEquals("From: Milliliter (mL)");rule.onNodeWithTag("converter_to_label",true).assertTextEquals("To: Liter (L)");rule.onAllNodesWithTag("converter_result",true).assertCountEquals(0)}
 @Test fun swapKeepsValueAndClearsResult(){show();input("1");click("converter_convert");click("converter_swap");rule.onNodeWithTag("converter_from_label",true).assertTextEquals("From: Int'l foot (ft)");rule.onAllNodesWithTag("converter_result",true).assertCountEquals(0);click("converter_convert");rule.onNodeWithTag("converter_value",true).assertTextEquals("= 0.3048 Meter (m)")}
 @Test fun invalidInputErrorNoFakeHistory(){show();input("1,000");click("converter_convert");await("converter_error");rule.onAllNodesWithTag("converter_result",true).assertCountEquals(0);rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 0 / 10");input("0");click("converter_convert");rule.onAllNodesWithTag("converter_error",true).assertCountEquals(0);rule.onNodeWithTag("converter_value",true).assertTextEquals("= 0 Int'l foot (ft)")}
 @Test fun temperatureBoundaryAndNegativePhysicalValue(){show();click("converter_kind_temperature");click("converter_to_k");input("-273.15");click("converter_convert");rule.onNodeWithTag("converter_value",true).assertTextEquals("= 0 Kelvin (K)");input("-274");click("converter_convert");await("converter_error");click("converter_kind_length");input("-1");click("converter_convert");await("converter_error")}
 @Test fun areaAndVolumeUseRightFactors(){show();click("converter_kind_area");click("converter_from_ft2");click("converter_to_m2");input("10");click("converter_convert");rule.onNodeWithTag("converter_value",true).assertTextEquals("= 0.9290304 Square meter (m²)");click("converter_kind_volume");click("converter_from_m3");click("converter_to_l");click("converter_convert");rule.onNodeWithTag("converter_value",true).assertTextEquals("= 10000 Liter (L)")}
 @Test fun historyClearIndependentOfCurrentInput(){show();input("1");repeat(12){click("converter_convert")};rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 10 / 10");click("converter_history_clear");rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 0 / 10");rule.onNodeWithTag("converter_value",true).assertExists();click("converter_clear");rule.onAllNodesWithTag("converter_result",true).assertCountEquals(0);rule.onNodeWithTag("converter_convert",true).assertIsNotEnabled()}
 @Test fun sessionNotPersistedAfterReopenAndNoActivity(){show();input("1");click("converter_convert");show();rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 0 / 10");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun callEntryOffOnPreservesDraftAndNoFilesChanged(){show(notes=true);rule.onAllNodesWithTag("call_converter_open",true).assertCountEquals(0);rule.onNode(hasSetTextAction()).performTextInput("Unfinished call note");val files=ctx.filesDir.walkTopDown().filter{it.isFile&&it.extension=="json"}.associate{it.path to it.readBytes()};runBlocking{ctx.repo.set(K.callConverter,true)};await("call_converter_open");click("call_converter_open");input("2");click("converter_convert");click("converter_back");rule.onNodeWithText("Unfinished call note").assertExists();files.forEach{(path,bytes)->assertArrayEquals(bytes,File(path).readBytes())};assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun dayResultPixels(){show();click("converter_kind_mass");click("converter_from_kg");click("converter_to_g");input("2.5");click("converter_convert");scrollTo("converter_result");settle();saveShot(rule,"CONVERTER428-day-result");scrollTo("converter_from_label");settle();saveShot(rule,"CONVERTER428-day-units")}
 @Test fun nightLargeResultPixels(){show(true);click("converter_kind_temperature");click("converter_from_f");click("converter_to_c");input("98.6");click("converter_convert");scrollTo("converter_result");settle();saveShot(rule,"CONVERTER428-night-large")}
 @Test fun settingsAndFinderPixels(){show();assertFalse(LdSettings().callConverter);assertTrue(SettingsFinder.all.any{it.id=="call-converter"&&it.route=="callconverter"});rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{CallConverterPage({})}};rule.activity.setContentView(host)};settle();saveShot(rule,"CONVERTER428-settings");click("converter_settings_open");rule.onNodeWithTag("converter_input",true).assertExists()}
 @Test fun nightLargeLengthLabelsPixels(){show(true);input("1000000000");click("converter_from_km");click("converter_to_mm");click("converter_convert");scrollTo("converter_from_label");settle();saveShot(rule,"CONVERTER428-night-length");scrollTo("converter_result");settle();saveShot(rule,"CONVERTER428-night-big-result")}

}
