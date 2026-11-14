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
class IncomingProfilesUiTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java));runBlocking{ctx.ldStore.edit{it.clear();it[K.onboarded]=true;it[K.motion]=false;it[K.theme]="daylight";it[K.incomingStudio]=true}}}}
 private val dispatcher=kotlinx.coroutines.test.StandardTestDispatcher()
 @get:Rule(order=1) val rule=createAndroidComposeRule<androidx.activity.ComponentActivity>(effectContext=dispatcher)
 private fun settle(){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);dispatcher.scheduler.runCurrent();rule.waitForIdle()}
 private var hostView:androidx.compose.ui.platform.ComposeView?=null
 private fun mount(content:@Composable ()->Unit){await{!LdApp.settings.value.motion&&LdApp.settings.value.onboarded};rule.runOnUiThread{val host=androidx.compose.ui.platform.ComposeView(rule.activity);hostView=host;host.setContent{LdThemeHost{content()}};rule.activity.setContentView(host)};settle()}
 private fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 private fun config()=IncomingCard.decode(LdApp.settings.value.incomingCard)
 private fun scroll(tag:String){rule.mainClock.autoAdvance=true;repeat(7){val n=rule.onNodeWithTag(tag,true).fetchSemanticsNode();val root=rule.onRoot().fetchSemanticsNode();val delta=n.positionInRoot.y+n.size.height/2f-root.size.height/2f
 if(n.positionInRoot.y>=75&&n.positionInRoot.y+n.size.height<root.size.height-20){rule.mainClock.autoAdvance=false;settle();return}
 rule.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,delta)};rule.waitForIdle()};rule.mainClock.autoAdvance=false;settle()}
 private fun click(tag:String){scroll(tag);rule.onNodeWithTag(tag,true).performClick();settle()}
 private var cuts=0;private var answers=0;private var audio=0;private var messages=0
 private fun actions(c:IncomingCard.Config=IncomingCard.Config(enabled=true,swipe=false),video:Boolean=false){mount{Column(Modifier.fillMaxWidth().padding(16.dp)){IncomingCardActions(c,video,{cuts++},{answers++},{audio++},{messages++})}}}
 @Before fun ready(){await{!LdApp.settings.value.motion&&LdApp.settings.value.onboarded&&LdApp.settings.value.incomingStudio}}
 private fun page(){mount{IncomingStudioPage({})}}
 @After fun clean(){rule.runOnUiThread{hostView?.disposeComposition();(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};runBlocking{ctx.ldStore.edit{it[K.incomingCard]="";it[K.fontScale]=1f}};org.robolectric.RuntimeEnvironment.setFontScale(1f)}
 private fun rows()=IncomingProfiles.decode(LdApp.settings.value.incomingProfiles)
 private fun seedProfiles(c:IncomingCard.Config=IncomingCard.Config(style="pearl",swipe=false,swap=true,number=false,confirmCut=true)):String{val r=IncomingProfiles.capture(emptyList(),"Evening",c);runBlocking{ctx.ldStore.edit{it[K.incomingProfiles]=IncomingProfiles.encode(r)}};await{rows().size==1};return r[0].id}
 private fun dialog(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}
 @Test fun defaultOffRetainsLibraryAndChoices(){val id=seedProfiles();val before=LdApp.settings.value.incomingCard;runBlocking{ctx.ldStore.edit{it[K.incomingStudio]=false}};await{!LdApp.settings.value.incomingStudio};page();rule.onNodeWithTag("studio_off",true).assertExists();rule.onAllNodesWithTag("studio_compare_"+id).assertCountEquals(0);assertEquals(before,LdApp.settings.value.incomingCard);assertEquals(id,rows()[0].id)}
 @Test fun saveCurrentAndNoMasterCapture(){runBlocking{ctx.ldStore.edit{it[K.incomingCard]=IncomingCard.encode(IncomingCard.Config(enabled=true,swipe=false,swap=true))}};await{IncomingCard.decode(LdApp.settings.value.incomingCard).enabled};page();click("studio_save_current");rule.onNode(hasSetTextAction()).performTextInput("My glass");click("studio_save_name");await{rows().size==1};assertEquals("My glass",rows()[0].name);assertFalse(rows()[0].choices.enabled);assertTrue(rows()[0].choices.swap);assertTrue(IncomingCard.decode(LdApp.settings.value.incomingCard).enabled)}
 @Test fun blankAndDuplicateNamesDoNotWrite(){seedProfiles();page();val before=LdApp.settings.value.incomingProfiles;click("studio_save_current");click("studio_save_name");await{rule.onAllNodesWithTag("studio_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(before,LdApp.settings.value.incomingProfiles);rule.onNode(hasSetTextAction()).performTextInput("EVENING");click("studio_save_name");settle();assertEquals(before,LdApp.settings.value.incomingProfiles)}
 @Test fun compareCancelDoesNotApply(){val id=seedProfiles();page();val before=LdApp.settings.value.incomingCard;click("studio_compare_"+id);rule.onNodeWithTag("studio_diff_count",true).assertExists();dialog("studio_apply");dialog("studio_apply_cancel");assertEquals(before,LdApp.settings.value.incomingCard);dialog("studio_compare_close");assertEquals(before,LdApp.settings.value.incomingCard)}
 @Test fun confirmedApplyPreservesOffThenUndoExact(){val id=seedProfiles();page();val before=LdApp.settings.value.incomingCard;click("studio_compare_"+id);dialog("studio_apply");dialog("studio_apply_confirm");await{IncomingCard.decode(LdApp.settings.value.incomingCard).style=="pearl"};assertFalse(IncomingCard.decode(LdApp.settings.value.incomingCard).enabled);click("studio_undo");await{LdApp.settings.value.incomingCard==before};assertEquals(id,rows()[0].id)}
 @Test fun renameAndDeleteCancelThenConfirm(){val id=seedProfiles();page();click("studio_rename_"+id);rule.onNode(hasSetTextAction()).performTextReplacement("Night Hindi घर");click("studio_save_name");await{rows()[0].name=="Night Hindi घर"};click("studio_delete_"+id);dialog("studio_delete_cancel");assertEquals(1,rows().size);click("studio_delete_"+id);dialog("studio_delete_confirm");await{rows().isEmpty()};assertEquals("",LdApp.settings.value.incomingCard)}
 @Test fun corruptLibraryHasNoSaveApplyOrOverwrite(){runBlocking{ctx.ldStore.edit{it[K.incomingProfiles]="broken"}};await{LdApp.settings.value.incomingProfiles=="broken"};page();rule.onNodeWithTag("studio_library_error",true).assertExists();rule.onAllNodesWithTag("studio_save_current",true).assertCountEquals(0);click("studio_trial_current");rule.onNodeWithTag("studio_demo_label",true).assertExists();assertEquals("broken",LdApp.settings.value.incomingProfiles)}
 @Test fun corruptCurrentDisablesProfileApply(){seedProfiles();runBlocking{ctx.ldStore.edit{it[K.incomingCard]="broken"}};await{LdApp.settings.value.incomingCard=="broken"};page();rule.onNodeWithTag("studio_current_error",true).assertExists();scroll("studio_save_current");rule.onNodeWithTag("studio_save_current",true).assertIsNotEnabled();assertEquals("broken",LdApp.settings.value.incomingCard)}
 @Test fun currentCorruptsWhileNameEditorOpen(){page();click("studio_save_current");rule.onNode(hasSetTextAction()).performTextInput("Do not save");runBlocking{ctx.ldStore.edit{it[K.incomingCard]="broken"}};await{LdApp.settings.value.incomingCard=="broken"};scroll("studio_save_name");rule.onNodeWithTag("studio_save_name",true).assertIsNotEnabled();assertEquals("",LdApp.settings.value.incomingProfiles);assertEquals("broken",LdApp.settings.value.incomingCard)}
 @Test fun savedProfileRehearsalDoesNotApplyAndReturns(){val id=seedProfiles();page();val before=LdApp.settings.value;click("studio_compare_"+id);dialog("studio_trial_profile");rule.onNodeWithTag("studio_demo_label",true).assertExists();click("studio_demo_close");rule.onNodeWithTag("studio_diff_count",true).assertExists();assertEquals(before,LdApp.settings.value);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun dayLibraryAndComparisonPixels(){val id=seedProfiles();page();scroll("studio_profile_"+id);saveShot(rule,"ST449-library-day");click("studio_compare_"+id);settle();rule.onNodeWithTag("studio_diff_count",true).assertIsDisplayed();saveShot(rule,"ST449-compare-day")}
 @Test fun nightLargeApplyDialogPixels(){val id=seedProfiles();runBlocking{ctx.ldStore.edit{it[K.theme]="midnight";it[K.fontScale]=1.3f}};await{LdApp.settings.value.fontScale==1.3f};page();click("studio_compare_"+id);dialog("studio_apply");saveDialogShot("ST449-confirm-night-large",0xff0d1320);dialog("studio_apply_cancel")}
 @Test fun existingSettingsEntryAndFinder(){mount{IncomingCardPage({})};click("incoming_studio_open");rule.onNodeWithTag("studio_enable",true).assertExists();assertTrue(SettingsFinder.search("incoming profiles compare").any{it.route=="incomingstudio"})}
}
