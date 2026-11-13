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
class TallyTemplatesUiTest {
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false;it[K.tallyTemplates]=false;File(ctx.filesDir,"tally_templates.json").delete()}}
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
 private fun show(night:Boolean=false,notes:Boolean=false,enabled:Boolean=true,library:Boolean=false){settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.callTally]=false;it[K.tallyTemplates]=enabled}};waitFor{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.callTally&&it.tallyTemplates==enabled}};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{if(notes)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){NotesSheet(n,"Support",{})}else if(library)TallyTemplateLibrary(listOf(CallTally.Item("a","Rice premium large family pack bags with matching unit",24),CallTally.Item("b","Sugar bags",12)),"5",{}, {loaded=it})else CallTallyWorkspace({})}};rule.activity.setContentView(host)};settle()}
 private fun input(tag:String,s:String){scrollTo(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(s);settle()}
 @After fun reset(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};settle();rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.callTally]=false;it[K.tallyTemplates]=false}}}
 private fun add(name:String,value:String="0"){input("tally_name",name);input("tally_value",value);click("tally_apply")}
 private var loaded:TallyTemplates.Template?=null
 private fun template(name:String="Weekly count",labels:List<String> = listOf("Rice bags","Sugar bags"))=TallyTemplates.Template("saved",name,labels,5)
 private fun seed(t:TallyTemplates.Template=template()){val snap=TallyTemplates.read(ctx);TallyTemplates.write(ctx,snap.raw,listOf(t))}
 private fun save(name:String="My order labels"){input("tt_name",name);click("tt_save");waitFor{TallyTemplates.read(ctx).templates.any{it.name==name}};await("tt_title_0")}
 @Test fun defaultOffHidesEntry(){show(enabled=false);rule.onAllNodesWithTag("tally_templates_open",true).assertCountEquals(0)}
 @Test fun enableOpensLocalEmptyLibrary(){show();click("tally_templates_open");await("tt_empty");rule.onNodeWithTag("tt_save",true).assertIsNotEnabled();assertFalse(File(ctx.filesDir,"tally_templates.json").exists())}
 @Test fun saveExplicitLabelsAndStepNeverQuantities(){show(library=true);await("tt_empty");save();val t=TallyTemplates.read(ctx).templates.single();assertEquals(5,t.step.toInt());assertEquals(2,t.labels.size);assertFalse(TallyTemplates.read(ctx).raw.contains("\"value\""));rule.onNodeWithTag("tt_status",true).assertTextEquals("Template saved. Quantities were not stored.")}
 @Test fun duplicateNamesShowErrorWithoutOverwrite(){seed();show(library=true);await("tt_title_0");input("tt_name"," WEEKLY count ");click("tt_save");await("tt_error");assertEquals(1,TallyTemplates.read(ctx).templates.size)}
 @Test fun loadCancelDoesNotCallConsumerOrChangeSavedData(){seed();val before=TallyTemplates.read(ctx);show(library=true);await("tt_title_0");click("tt_load_0");assertNull(loaded);rule.onNodeWithTag("tt_cancel",true).performClick();settle();assertNull(loaded);assertEquals(before,TallyTemplates.read(ctx))}
 @Test fun confirmLoadAndUndoRestoreCounters(){seed();show();add("Current boxes","17");click("tally_templates_open");await("tt_title_0");click("tt_load_0");rule.onNodeWithTag("tt_confirm",true).performClick();settle();rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("0");rule.onNodeWithTag("tally_label_0",true).assertTextEquals("Rice bags");click("tally_undo");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("17");rule.onNodeWithTag("tally_label_0",true).assertTextEquals("Current boxes");click("tally_redo");rule.onNodeWithTag("tally_total",true).assertTextEquals("0");input("tally_step","5");click("tally_plus_0");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("5")}
 @Test fun deleteCancelAndConfirmNeverChangeSession(){seed();show();add("Current boxes","17");click("tally_templates_open");await("tt_title_0");click("tt_delete_0");rule.onNodeWithTag("tt_cancel",true).performClick();settle();assertEquals(1,TallyTemplates.read(ctx).templates.size);click("tt_delete_0");rule.onNodeWithTag("tt_confirm",true).performClick();await("tt_empty");assertTrue(TallyTemplates.read(ctx).templates.isEmpty());rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle();rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("17")}
 @Test fun staleWriteShowsErrorAndReloadRecovers(){seed();show(library=true);await("tt_title_0");val old=TallyTemplates.read(ctx);TallyTemplates.write(ctx,old.raw,old.templates+template("Other").copy(id="other"));input("tt_name","New");click("tt_save");await("tt_error");assertEquals(2,TallyTemplates.read(ctx).templates.size);click("tt_reload");await("tt_title_1");rule.onNodeWithTag("tt_error",true).assertDoesNotExist()}
 @Test fun corruptReadNeverOffersSave(){File(ctx.filesDir,"tally_templates.json").writeText("broken");show(library=true);await("tt_error");input("tt_name","New");rule.onNodeWithTag("tt_save",true).assertIsNotEnabled();assertEquals("broken",File(ctx.filesDir,"tally_templates.json").readText())}
 @Test fun savedLibrarySurvivesReopenCountsStayFresh(){show(library=true);await("tt_empty");save();show(library=true);await("tt_title_0");click("tt_load_0");rule.onNodeWithTag("tt_confirm",true).performClick();settle();assertNotNull(loaded);assertTrue(TallyTemplates.board(loaded!!).all{it.value==0L});assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun dayLibraryPixels(){seed();show(library=true);await("tt_title_0");scrollTo("tt_current");saveShot(rule,"TEMPLATES432-day-save");scrollTo("tt_load_0");saveShot(rule,"TEMPLATES432-day-library")}
 @Test fun nightLargeLibraryAndConfirmPixels(){seed(template("Weekly supplies count for the whole family store",listOf("Rice premium large family pack bags with matching unit","Sugar bags")));show(night=true,library=true);await("tt_title_0");scrollTo("tt_load_0");saveShot(rule,"TEMPLATES432-night-library");click("tt_load_0");settle();saveDialogShot("TEMPLATES432-night-confirm",0xff0d1320)}
 @Test fun settingsSwitchPersistsAndPreviewOpens(){show(enabled=false);assertFalse(LdSettings().tallyTemplates);rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;LdThemeHost{CallTallyPage({})}};rule.activity.setContentView(host)};settle();rule.onNodeWithText("Reusable tally templates").performClick();waitFor{LdApp.settings.value.tallyTemplates};saveShot(rule,"TEMPLATES432-settings");click("tally_settings_open");rule.onNodeWithTag("tally_templates_open",true).assertExists()}
 @Test fun callSheetTemplateLoadPreservesNoteDraft(){seed();show(notes=true);rule.onNode(hasSetTextAction()).performTextInput("Call note draft");runBlocking{ctx.repo.set(K.callTally,true)};await("call_tally_open");click("call_tally_open");click("tally_templates_open");await("tt_title_0");click("tt_load_0");rule.onNodeWithTag("tt_confirm",true).performClick();settle();rule.onNodeWithTag("tally_total",true).assertTextEquals("0");click("tally_back");rule.onNodeWithTag("tally_confirm",true).performClick();settle();rule.onNodeWithText("Call note draft").assertExists();assertEquals(listOf("Rice bags","Sugar bags"),TallyTemplates.read(ctx).templates.single().labels)}

}
