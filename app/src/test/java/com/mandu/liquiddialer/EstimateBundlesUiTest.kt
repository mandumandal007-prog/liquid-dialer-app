package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class EstimateBundlesUiTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java));runBlocking{ctx.ldStore.edit{it.clear();it[K.onboarded]=true;it[K.motion]=false;it[K.theme]="daylight";it[K.estimateBundles]=true}}}}
 private val dispatcher=kotlinx.coroutines.test.StandardTestDispatcher()
 @get:Rule(order=1) val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>(effectContext=dispatcher)
 private fun settle(){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);dispatcher.scheduler.runCurrent();rule.waitForIdle()}
 private var hostView:androidx.compose.ui.platform.ComposeView?=null
 private fun mount(content:@Composable ()->Unit){await{!LdApp.settings.value.motion&&LdApp.settings.value.onboarded};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);hostView=host;host.setContent{LdThemeHost{content()}};rule.activity.setContentView(host)};settle()}
 private fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 private fun scroll(tag:String){rule.mainClock.autoAdvance=true;repeat(7){val n=rule.onNodeWithTag(tag,true).fetchSemanticsNode();val root=rule.onRoot().fetchSemanticsNode();val delta=n.positionInRoot.y+n.size.height/2f-root.size.height/2f
 if(n.positionInRoot.y>=75&&n.positionInRoot.y+n.size.height<root.size.height-20){rule.mainClock.autoAdvance=false;settle();return}
 rule.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,delta)};rule.waitForIdle()};rule.mainClock.autoAdvance=false;settle()}
 private fun click(tag:String){scroll(tag);rule.onNodeWithTag(tag,true).performClick();settle()}
 @Before fun ready(){await{LdApp.settings.value.estimateBundles&&!LdApp.settings.value.motion}}
 @After fun clean(){rule.runOnUiThread{hostView?.disposeComposition();(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};org.robolectric.RuntimeEnvironment.setFontScale(1f)}
 private val base=CallEstimate.Draft(4,listOf(CallEstimate.line("Rice","2.5","40","a"),CallEstimate.line("Oil","1","120","b")),"10","5")
 private var applied:CallEstimate.Draft?=null;private var backs=0
 private fun rows()=EstimateBundles.decode(LdApp.settings.value.estimateBundleLibrary)
 private fun seed():String{val r=EstimateBundles.capture(emptyList(),"Weekly",base,setOf("a","b"));runBlocking{ctx.ldStore.edit{it[K.estimateBundleLibrary]=EstimateBundles.encode(r)}};await{rows().size==1};return r[0].id}
 private fun page(d:CallEstimate.Draft=base){mount{EstimateBundlesWorkspace("+919876543210",d,{backs++},{applied=it})}}
 private fun direct(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}
 @Test fun defaultOffRetainsLibraryAndDraft(){val id=seed();runBlocking{ctx.ldStore.edit{it[K.estimateBundles]=false}};await{!LdApp.settings.value.estimateBundles};page();rule.onNodeWithTag("bundle_off",true).assertExists();rule.onAllNodesWithTag("bundle_preview_"+id,true).assertCountEquals(0);assertNull(applied);assertEquals(1,rows().size)}
 @Test fun saveSelectedOnlyNoEstimateEffect(){page();click("bundle_pick_b");click("bundle_capture");rule.onNode(hasSetTextAction()).performTextInput("Oil only");click("bundle_save");await{rows().size==1};assertEquals("Oil",rows()[0].lines.single().label);assertEquals("1",rows()[0].lines.single().quantity);assertNull(applied);assertEquals(0,backs)}
 @Test fun blankDuplicateNamesRefused(){seed();page();click("bundle_pick_a");click("bundle_capture");click("bundle_save");await{rule.onAllNodesWithTag("bundle_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(1,rows().size);rule.onNode(hasSetTextAction()).performTextInput("WEEKLY");click("bundle_save");settle();assertEquals(1,rows().size)}
 @Test fun renameDeleteCancelAndConfirm(){val id=seed();page();click("bundle_rename_"+id);rule.onNode(hasSetTextAction()).performTextReplacement("रोज वाला");click("bundle_save");await{rows()[0].name=="रोज वाला"};click("bundle_delete_"+id);direct("bundle_delete_cancel");assertEquals(1,rows().size);click("bundle_delete_"+id);direct("bundle_delete_confirm");await{rows().isEmpty()};assertNull(applied)}
 @Test fun previewCancelThenConfirmUnsaved(){val id=seed();page();click("bundle_preview_"+id);scroll("bundle_preview_total");rule.onNodeWithTag("bundle_preview_total",true).assertTextEquals("Total 220.50 → 451.50");click("bundle_apply");direct("bundle_cancel");assertNull(applied);click("bundle_apply");direct("bundle_confirm");await{applied!=null};assertEquals(4,applied!!.lines.size);assertEquals("10",applied!!.discount);assertEquals("5",applied!!.tax);assertEquals(4L,applied!!.revision);assertEquals(1,rows().size);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun libraryChangeAfterPreviewRefusesAppend(){val id=seed();page();click("bundle_preview_"+id);click("bundle_apply");runBlocking{ctx.ldStore.edit{it[K.estimateBundleLibrary]=EstimateBundles.encode(EstimateBundles.rename(rows(),id,"Later"))}};await{rows()[0].name=="Later"};direct("bundle_confirm");assertNull(applied);rule.onNodeWithTag("bundle_error",true).assertExists()}
 @Test fun deleteStaleLibraryRefused(){val id=seed();page();click("bundle_delete_"+id);runBlocking{ctx.ldStore.edit{it[K.estimateBundleLibrary]=EstimateBundles.encode(EstimateBundles.rename(rows(),id,"Later"))}};await{rows()[0].name=="Later"};direct("bundle_delete_confirm");settle();assertEquals("Later",rows()[0].name);assertNull(applied)}
 @Test fun corruptLibraryLocked(){runBlocking{ctx.ldStore.edit{it[K.estimateBundleLibrary]="broken"}};await{LdApp.settings.value.estimateBundleLibrary=="broken"};page();rule.onNodeWithTag("bundle_corrupt",true).assertExists();rule.onAllNodesWithTag("bundle_capture",true).assertCountEquals(0);assertNull(applied);assertEquals("broken",LdApp.settings.value.estimateBundleLibrary)}
 @Test fun fullDraftRefusesWholeBundle(){val id=seed();page(base.copy(lines=(0..18).map{CallEstimate.line("Existing","1","1","e$it")},discount="0"));click("bundle_preview_"+id);rule.onNodeWithTag("bundle_error",true).assertExists();rule.onAllNodesWithTag("bundle_apply",true).assertCountEquals(0);assertNull(applied)}
 @Test fun emptyDraftCanAppend(){val id=seed();page(CallEstimate.Draft());click("bundle_preview_"+id);click("bundle_apply");direct("bundle_confirm");await{applied!=null};assertEquals(2,applied!!.lines.size);assertEquals("0",applied!!.discount)}
 @Test fun editorBackRequiresDiscard(){page();click("bundle_pick_a");click("bundle_capture");rule.onNode(hasSetTextAction()).performTextInput("Unsaved");click("bundle_back");direct("bundle_keep");assertEquals(0,backs);click("bundle_back");direct("bundle_discard");assertEquals(1,backs);assertTrue(rows().isEmpty())}
 @Test fun offDuringPreviewNoApply(){val id=seed();page();click("bundle_preview_"+id);click("bundle_apply");runBlocking{ctx.ldStore.edit{it[K.estimateBundles]=false}};await{!LdApp.settings.value.estimateBundles&&rule.onAllNodesWithTag("bundle_off",true).fetchSemanticsNodes().size==1&&rule.onAllNodesWithTag("bundle_confirm",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("bundle_off",true).assertExists();rule.onAllNodesWithTag("bundle_confirm",true).assertCountEquals(0);assertNull(applied);assertEquals(1,rows().size)}
 @Test fun dayLibraryAndPreviewPixels(){val id=seed();page();scroll("bundle_preview_"+id);saveShot(rule,"EB450-library-day");click("bundle_preview_"+id);scroll("bundle_preview_name_0");saveShot(rule,"EB450-items-day");scroll("bundle_apply");saveShot(rule,"EB450-total-day");click("bundle_apply");saveDialogShot("EB450-confirm-day",0xfff1f5fa);direct("bundle_cancel")}
 @Test fun largeNightSettingsPixels(){runBlocking{ctx.ldStore.edit{it[K.theme]="midnight";it[K.fontScale]=1.5f}};await{LdApp.settings.value.fontScale==1.5f};mount{EstimateBundlesPage({})};saveShot(rule,"EB450-settings-night-large")}
 @Test fun calculatorEntryReusesBundleWithoutSaving(){val id=seed();mount{CallEstimateWorkspace("+919999887766",{})};await{rule.onAllNodesWithTag("estimate_bundles",true).fetchSemanticsNodes().isNotEmpty()};click("estimate_bundles");click("bundle_preview_"+id);click("bundle_apply");direct("bundle_confirm");await{rule.onAllNodesWithTag("estimate_status",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("estimate_status",true).assertTextEquals("Unsaved changes");assertTrue(CallEstimate.read(ctx,"+919999887766").lines.isEmpty());assertEquals(1,rows().size)}
 @Test fun settingsToggleAndFinder(){mount{EstimateBundlesPage({})};rule.onAllNodesWithText("Calculator item bundles").onLast().assertExists();assertTrue(SettingsFinder.search("reusable bundle").any{it.route=="estimatebundles"})}
 @Test @Config(qualifiers="w320dp-h640dp-xhdpi") fun narrowLargePreviewAndConfirmPixels(){val id=seed();runBlocking{ctx.ldStore.edit{it[K.theme]="midnight";it[K.fontScale]=1.5f}};await{LdApp.settings.value.fontScale==1.5f};page();click("bundle_preview_"+id);scroll("bundle_preview_name_0");rule.onNodeWithTag("bundle_preview_name_0",true).assertIsDisplayed();saveShot(rule,"EB450-narrow-items-night-large");scroll("bundle_apply");rule.onNodeWithTag("bundle_apply",true).assertIsDisplayed();saveShot(rule,"EB450-narrow-total-night-large");click("bundle_apply");saveDialogShot("EB450-narrow-confirm-night-large",0xff0d1320);direct("bundle_cancel")}

 @Test fun saveRefusesLibraryChangedWhileNaming(){val id=seed();page();click("bundle_pick_a");click("bundle_capture");rule.onNode(hasSetTextAction()).performTextInput("New");runBlocking{ctx.ldStore.edit{it[K.estimateBundleLibrary]=EstimateBundles.encode(EstimateBundles.rename(rows(),id,"Later"))}};await{rows()[0].name=="Later"};click("bundle_save");await{rule.onAllNodesWithTag("bundle_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(1,rows().size);assertEquals("Later",rows()[0].name)}
 @Test fun sixBundlesDisableCapture(){var r=emptyList<EstimateBundles.Bundle>();repeat(6){r=EstimateBundles.capture(r,"B$it",base,setOf("a"))};runBlocking{ctx.ldStore.edit{it[K.estimateBundleLibrary]=EstimateBundles.encode(r)}};await{rows().size==6};page();click("bundle_pick_a");scroll("bundle_capture");rule.onNodeWithTag("bundle_capture",true).assertIsNotEnabled();assertEquals(6,rows().size)}
 @Test fun changedBaseWhilePreviewOpenRefusesOldApply(){val id=seed();var d by mutableStateOf(base);mount{EstimateBundlesWorkspace("+919876543210",d,{},{applied=it})};click("bundle_preview_"+id);click("bundle_apply");rule.runOnUiThread{d=base.copy(tax="6")};settle();rule.onAllNodesWithTag("bundle_confirm",true).assertCountEquals(0);assertNull(applied)}
 @Test fun settingsOffOnKeepsLibrary(){seed();mount{EstimateBundlesPage({})};val toggle=hasText("Calculator item bundles") and hasClickAction();rule.onAllNodes(toggle).onLast().performClick();await{!LdApp.settings.value.estimateBundles};assertEquals(1,rows().size);rule.onAllNodes(toggle).onLast().performClick();await{LdApp.settings.value.estimateBundles};assertEquals(1,rows().size)}

}
