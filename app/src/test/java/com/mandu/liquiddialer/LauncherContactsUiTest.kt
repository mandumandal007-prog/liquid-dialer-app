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
abstract class LauncherContactsUiFixture{
 @get:Rule(order=0) val initialSettings=object:org.junit.rules.ExternalResource(){override fun before(){
  // These workspace tests own a plain host. Do not launch MainActivity startup
  // loads, onboarding or animations before installing the test composition.
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false};ctx.repo.flow.first{it.onboarded&&!it.motion}}
 }}
 private val dispatcher=kotlinx.coroutines.test.StandardTestDispatcher()
 @get:Rule(order=1) val rule=createAndroidComposeRule<ComponentActivity>(effectContext=dispatcher)
 protected val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 protected val number="+919876543210"
 private var focus:androidx.compose.ui.focus.FocusManager?=null
 protected fun ComponentActivity.mountTestPage(content:@Composable ()->Unit){val host=ComposeView(this);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;content()};setContentView(host)}
 protected fun settle(){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);dispatcher.scheduler.runCurrent();rule.waitForIdle()}
 protected fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 protected fun seed(night:Boolean=false){settle();ctx.getSharedPreferences("launcher_contacts",0).edit().clear().commit();runBlocking{ctx.ldStore.edit{it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.refinedHeaders]=true}};await{LdApp.settings.value.theme==(if(night)"midnight"else"daylight")}}
 protected val people=listOf(Contact(1,"a","Chhotu","", "+919876543210","Mobile",false,null),Contact(2,"b","Father","","+919999999999","Mobile",false,null),Contact(3,"c","Mandu 2","","+449876543210","Work",false,null))
 protected fun show(night:Boolean=false){seed(night);rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{LauncherContactsPage(null,{},people)}}};await{rule.onAllNodesWithTag("launcher_name_0",true).fetchSemanticsNodes().isNotEmpty()}}
 protected fun select(i:Int){click("launcher_tab_$i");click("launcher_choose_$i");click("launcher_pick_$i")}
 protected fun field(tag:String)=rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true)
 protected fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(tag,true).performClick();settle()}
 @After fun clean(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1000);rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.refinedHeaders]=false;it[K.messageDrafts]=false}}}
}



@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherContactsUiTest:LauncherContactsUiFixture(){
 @Test fun defaultOffPickThreeAndPreviewPixels(){show();select(0);select(1);select(2);assertFalse(LauncherContacts.read(ctx).config.enabled);rule.onNodeWithText("Icon shortcuts ON",useUnmergedTree=true).performScrollTo().performClick();settle();click("launcher_save");await{LauncherContacts.read(ctx).config.targets.size==3};assertTrue(LauncherContacts.read(ctx).config.enabled);assertEquals(listOf("Chhotu","Father","Mandu 2"),LauncherContacts.read(ctx).config.targets.map{it.name});rule.mainClock.autoAdvance=true;rule.onNode(hasScrollAction()).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,1500f)};settle();saveShot(rule,"LAUNCH418-preview");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun manualFullNumbersAndDuplicateInvalid(){show();field("launcher_name_0").performTextInput("Foreign");field("launcher_number_0").performTextInput("+449876543210");click("launcher_tab_1");field("launcher_name_1").performTextInput("Same");field("launcher_number_1").performTextInput("+44 98765 43210");settle();rule.onNodeWithTag("launcher_save",true).assertIsNotEnabled();click("launcher_clear_1");click("launcher_save");await{LauncherContacts.read(ctx).config.targets.size==1};assertEquals("+449876543210",LauncherContacts.read(ctx).config.targets.single().number)}
 @Test fun backAndReloadDiscardNeeded(){show();field("launcher_name_0").performTextInput("Unsaved");rule.onNodeWithTag("page_back",true).performClick();settle();rule.onNodeWithTag("launcher_discard",true).assertExists();assertTrue(LauncherContacts.read(ctx).config.targets.isEmpty())}
 @Test fun searchFindsContactAndDoesNotMutateUntilSave(){show();click("launcher_choose_0");field("launcher_search").performTextInput("Father");settle();click("launcher_pick_0");field("launcher_name_0").assertTextContains("Father");assertTrue(LauncherContacts.read(ctx).config.targets.isEmpty());saveShot(rule,"LAUNCH418-day-settings")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherContactsSafetyUiTest:LauncherContactsUiFixture(){
 @Test fun corruptDataPreserved(){seed();ctx.getSharedPreferences("launcher_contacts",0).edit().putString("data","broken").commit();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{LauncherContactsPage(null,{})}}};await{rule.onAllNodesWithTag("launcher_error",true).fetchSemanticsNodes().isNotEmpty()};rule.onAllNodesWithTag("launcher_save",true).assertCountEquals(0);assertEquals("broken",ctx.getSharedPreferences("launcher_contacts",0).getString("data",null))}
 @Test fun staleSaveDoesNotOverwrite(){show();select(0);LauncherContacts.save(ctx,"",LauncherContacts.Config(false,listOf(LauncherContacts.Target("Elsewhere","139"))));click("launcher_save");await{rule.onAllNodesWithTag("launcher_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals("Elsewhere",LauncherContacts.read(ctx).config.targets.single().name)}
 @Test fun nightLargePickerAndSettingsPixels(){show(true);click("launcher_choose_0");saveShot(rule,"LAUNCH418-night-picker");click("launcher_pick_0");saveShot(rule,"LAUNCH418-night-large")}
}
