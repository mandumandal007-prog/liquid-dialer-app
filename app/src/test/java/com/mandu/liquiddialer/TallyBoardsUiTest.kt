package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
class TallyBoardsUiTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java));runBlocking{ctx.ldStore.edit{it.clear();it[K.onboarded]=true;it[K.motion]=false;it[K.theme]="daylight";it[K.tallyBoards]=true}}}}
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
 @Before fun ready(){await{LdApp.settings.value.tallyBoards&&!LdApp.settings.value.motion}}
 @After fun clean(){rule.runOnUiThread{hostView?.disposeComposition();(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};org.robolectric.RuntimeEnvironment.setFontScale(1f)}
 private val base=listOf(CallTally.Item("a","Rice bags",12),CallTally.Item("b","Oil tins",7))
 private val other=listOf(CallTally.Item("c","Empty crates",30))
 private var applied:TallyBoards.Plan?=null;private var backs=0
 private fun all()=TallyBoards.decode(LdApp.settings.value.tallyBoardLibrary)
 private fun seed():String{val a=TallyBoards.capture(emptyList(),"Morning",base);runBlocking{ctx.ldStore.edit{it[K.tallyBoardLibrary]=TallyBoards.encode(a)}};await{all().size==1};return a[0].id}
 private fun page(rows:List<CallTally.Item> = other){mount{TallyBoardsWorkspace(rows,{backs++},{applied=it})}}
 private fun direct(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}
 @Test fun offRetainsLibraryNoLoad(){val id=seed();runBlocking{ctx.ldStore.edit{it[K.tallyBoards]=false}};await{!LdApp.settings.value.tallyBoards};page();rule.onNodeWithTag("boards_off",true).assertExists();rule.onAllNodesWithTag("boards_preview_"+id,true).assertCountEquals(0);assertNull(applied);assertEquals(1,all().size)}
 @Test fun explicitSaveCountsAndNoSessionEffect(){page(base);click("boards_capture");rule.onNode(hasSetTextAction()).performTextInput("My saved board");click("boards_save");await{all().size==1};assertEquals(listOf(12L,7L),all()[0].rows.map{it.value});assertEquals("My saved board",all()[0].name);assertNull(applied)}
 @Test fun emptySessionCannotSave(){page(emptyList());scroll("boards_capture");rule.onNodeWithTag("boards_capture",true).assertIsNotEnabled();assertTrue(all().isEmpty())}
 @Test fun blankDuplicateNamesRefuse(){seed();page();click("boards_capture");click("boards_save");await{rule.onAllNodesWithTag("boards_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(1,all().size);rule.onNode(hasSetTextAction()).performTextInput("MORNING");click("boards_save");settle();assertEquals(1,all().size)}
 @Test fun renameDeleteCancelAndConfirm(){val id=seed();page();click("boards_rename_"+id);rule.onNode(hasSetTextAction()).performTextReplacement("सुबह वाला");click("boards_save");await{all()[0].name=="सुबह वाला"};click("boards_delete_"+id);direct("boards_delete_cancel");assertEquals(1,all().size);click("boards_delete_"+id);direct("boards_delete_confirm");await{all().isEmpty()};assertNull(applied)}
 @Test fun compareCancelThenConfirmedReplacement(){val id=seed();page();click("boards_preview_"+id);scroll("boards_total");rule.onNodeWithTag("boards_total",true).assertTextEquals("Raw sum 30 → 19. Matching units are your responsibility; this sum does not validate them.");click("boards_apply");direct("boards_cancel");assertNull(applied);click("boards_apply");direct("boards_confirm");await{applied!=null};assertEquals(other,applied!!.before);assertEquals(listOf(12L,7L),applied!!.after.map{it.value});assertEquals(1,all().size);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun emptySessionCanLoad(){val id=seed();page(emptyList());click("boards_preview_"+id);click("boards_apply");direct("boards_confirm");await{applied!=null};assertTrue(applied!!.before.isEmpty());assertEquals(2,applied!!.after.size)}
 @Test fun corruptLibraryLocked(){runBlocking{ctx.ldStore.edit{it[K.tallyBoardLibrary]="broken"}};await{LdApp.settings.value.tallyBoardLibrary=="broken"};page();rule.onNodeWithTag("boards_corrupt",true).assertExists();rule.onAllNodesWithTag("boards_capture",true).assertCountEquals(0);assertNull(applied);assertEquals("broken",LdApp.settings.value.tallyBoardLibrary)}
 @Test fun changedLibraryAfterPreviewRefuses(){val id=seed();page();click("boards_preview_"+id);click("boards_apply");runBlocking{ctx.ldStore.edit{it[K.tallyBoardLibrary]=TallyBoards.encode(TallyBoards.rename(all(),id,"Later"))}};await{all()[0].name=="Later"};direct("boards_confirm");await{rule.onAllNodesWithTag("boards_error",true).fetchSemanticsNodes().isNotEmpty()};assertNull(applied)}
 @Test fun staleDeleteRefuses(){val id=seed();page();click("boards_delete_"+id);runBlocking{ctx.ldStore.edit{it[K.tallyBoardLibrary]=TallyBoards.encode(TallyBoards.rename(all(),id,"Later"))}};await{all()[0].name=="Later"};direct("boards_delete_confirm");await{rule.onAllNodesWithTag("boards_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals("Later",all()[0].name)}
 @Test fun staleCaptureSessionRefuses(){var rows by mutableStateOf(base);mount{TallyBoardsWorkspace(rows,{},{applied=it})};click("boards_capture");rule.onNode(hasSetTextAction()).performTextInput("Old");rule.runOnUiThread{rows=other};settle();click("boards_save");await{rule.onAllNodesWithTag("boards_error",true).fetchSemanticsNodes().isNotEmpty()};assertTrue(all().isEmpty())}
 @Test fun staleCaptureLibraryRefuses(){val id=seed();page();click("boards_capture");rule.onNode(hasSetTextAction()).performTextInput("New");runBlocking{ctx.ldStore.edit{it[K.tallyBoardLibrary]=TallyBoards.encode(TallyBoards.rename(all(),id,"Later"))}};await{all()[0].name=="Later"};click("boards_save");await{rule.onAllNodesWithTag("boards_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(1,all().size)}
 @Test fun offDuringConfirmWaitsForRenderedOff(){val id=seed();page();click("boards_preview_"+id);click("boards_apply");runBlocking{ctx.ldStore.edit{it[K.tallyBoards]=false}};await{!LdApp.settings.value.tallyBoards&&rule.onAllNodesWithTag("boards_off",true).fetchSemanticsNodes().size==1&&rule.onAllNodesWithTag("boards_confirm",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("boards_off",true).assertExists();rule.onAllNodesWithTag("boards_confirm",true).assertCountEquals(0);assertNull(applied);assertEquals(1,all().size)}
 @Test fun changedSessionClosesPreview(){val id=seed();var rows by mutableStateOf(other);mount{TallyBoardsWorkspace(rows,{},{applied=it})};click("boards_preview_"+id);click("boards_apply");rule.runOnUiThread{rows=base};await{rule.onAllNodesWithTag("boards_confirm",true).fetchSemanticsNodes().isEmpty()};assertNull(applied)}
 @Test fun editorBackDiscard(){page();click("boards_capture");rule.onNode(hasSetTextAction()).performTextInput("Unsaved");click("boards_back");direct("boards_keep");assertEquals(0,backs);click("boards_back");direct("boards_discard");assertEquals(1,backs);assertTrue(all().isEmpty())}
 @Test fun sixBoardsDisableSave(){var a=emptyList<TallyBoards.Saved>();repeat(6){a=TallyBoards.capture(a,"B$it",base)};runBlocking{ctx.ldStore.edit{it[K.tallyBoardLibrary]=TallyBoards.encode(a)}};await{all().size==6};page();scroll("boards_capture");rule.onNodeWithTag("boards_capture",true).assertIsNotEnabled()}
 @Test fun settingsToggleKeepsLibrary(){seed();mount{TallyBoardsPage({})};val toggle=hasText("Saved tally boards") and hasClickAction();rule.onAllNodes(toggle).onLast().performClick();await{!LdApp.settings.value.tallyBoards};assertEquals(1,all().size);rule.onAllNodes(toggle).onLast().performClick();await{LdApp.settings.value.tallyBoards};assertEquals(1,all().size)}
 @Test fun dayLibraryAndComparePixels(){val id=seed();page();scroll("boards_preview_"+id);saveShot(rule,"TB452-library-day");click("boards_preview_"+id);scroll("boards_before_0");saveShot(rule,"TB452-before-day");scroll("boards_apply");saveShot(rule,"TB452-after-day");click("boards_apply");saveDialogShot("TB452-confirm-day",0xfff1f5fa);direct("boards_cancel")}
 @Test @Config(qualifiers="w320dp-h640dp-xhdpi") fun narrowNightLargePixels(){val id=seed();runBlocking{ctx.ldStore.edit{it[K.theme]="midnight";it[K.fontScale]=1.5f}};await{LdApp.settings.value.fontScale==1.5f};page();click("boards_preview_"+id);scroll("boards_apply");rule.onNodeWithTag("boards_apply",true).assertIsDisplayed();saveShot(rule,"TB452-narrow-after-night-large");click("boards_apply");saveDialogShot("TB452-narrow-confirm-night-large",0xff0d1320);direct("boards_cancel")}
 @Test fun largeNightSettingsPixels(){runBlocking{ctx.ldStore.edit{it[K.theme]="midnight";it[K.fontScale]=1.5f}};await{LdApp.settings.value.fontScale==1.5f};mount{TallyBoardsPage({})};saveShot(rule,"TB452-settings-night-large")}
 private fun input(tag:String,value:String){scroll(tag);rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(value);settle()}
 @Test fun realBoardLoadUndoKeepsStepAndLibrary(){val id=seed();mount{CallTallyWorkspace({})};input("tally_name","Existing crates");input("tally_value","30");click("tally_apply");input("tally_step","9");click("tally_boards_open");click("boards_preview_"+id);click("boards_apply");direct("boards_confirm");await{rule.onAllNodesWithTag("tally_total",true).fetchSemanticsNodes().isNotEmpty()};scroll("tally_total");rule.onNodeWithTag("tally_total",true).assertTextEquals("19");scroll("tally_step");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("tally_step")),true).assertTextEquals("9");click("tally_undo");scroll("tally_total");rule.onNodeWithTag("tally_total",true).assertTextEquals("30");scroll("tally_step");rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("tally_step")),true).assertTextEquals("9");assertEquals(listOf(12L,7L),all()[0].rows.map{it.value})}
 @Test fun notesSheetIntegrationLoad(){val id=seed();mount{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){TallyBoardsWorkspace(emptyList(),{},{applied=it},inSheet=true)}};click("boards_preview_"+id);click("boards_apply");direct("boards_confirm");await{applied!=null};assertEquals(19L,CallTally.total(applied!!.after));assertTrue(SettingsFinder.search("saved tally board").any{it.route=="tallyboards"})}

}
