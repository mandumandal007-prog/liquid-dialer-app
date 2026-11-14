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
class IncomingProfilesApplyUiTest {
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
 @Test fun staleCompareRefusesAfterChoiceChange(){val id=seedProfiles();page();click("studio_compare_"+id);val changed=IncomingCard.encode(IncomingCard.Config(enabled=true,style="cyber"));runBlocking{ctx.ldStore.edit{it[K.incomingCard]=changed}};await{LdApp.settings.value.incomingCard==changed};dialog("studio_apply");dialog("studio_apply_confirm");await{rule.onAllNodesWithTag("studio_compare_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(changed,LdApp.settings.value.incomingCard)}
 @Test fun staleProfileRenameRefusesApply(){val id=seedProfiles();page();click("studio_compare_"+id);runBlocking{ctx.ldStore.edit{it[K.incomingProfiles]=IncomingProfiles.encode(IncomingProfiles.rename(rows(),id,"New name"))}};await{rows()[0].name=="New name"};dialog("studio_apply");dialog("studio_apply_confirm");await{rule.onAllNodesWithTag("studio_compare_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals("",LdApp.settings.value.incomingCard)}
 @Test fun undoRefusesAfterLaterIncomingChange(){val id=seedProfiles();page();click("studio_compare_"+id);dialog("studio_apply");dialog("studio_apply_confirm");await{IncomingCard.decode(LdApp.settings.value.incomingCard).style=="pearl"};val later=IncomingCard.encode(IncomingCard.Config(style="cyber"));runBlocking{ctx.ldStore.edit{it[K.incomingCard]=later}};await{LdApp.settings.value.incomingCard==later};click("studio_undo");await{rule.onAllNodesWithTag("studio_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(later,LdApp.settings.value.incomingCard)}
 @Test fun deleteRefusesLibraryChangedAfterDialogOpened(){val id=seedProfiles();page();click("studio_delete_"+id);val changed=IncomingProfiles.encode(IncomingProfiles.rename(rows(),id,"Changed elsewhere"));runBlocking{ctx.ldStore.edit{it[K.incomingProfiles]=changed}};await{LdApp.settings.value.incomingProfiles==changed};dialog("studio_delete_confirm");await{rule.onAllNodesWithTag("studio_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals(changed,LdApp.settings.value.incomingProfiles)}
 @Test fun fourteenDiffsLargeFontControlsReachable(){val c=IncomingCard.Config(style="pearl",background="solid",avatar="squircle",avatarSize=75,nameSize=85,number=false,contactType=false,lastCall=false,message=false,whatsapp=false,swap=true,swipe=false,travel=70,confirmCut=true);val id=seedProfiles(c);runBlocking{ctx.ldStore.edit{it[K.fontScale]=1.3f;it[K.theme]="midnight"}};await{LdApp.settings.value.fontScale==1.3f};page();click("studio_compare_"+id);settle();rule.onNodeWithTag("studio_diff_count",true).assertTextEquals("Incoming master stays OFF. 14 changed controls. All other settings stay unchanged.");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("studio_apply",true).performScrollTo();rule.mainClock.autoAdvance=false;settle();rule.onNodeWithTag("studio_apply",true).assertIsDisplayed();saveShot(rule,"ST449-all-diffs-night-large");dialog("studio_apply");dialog("studio_apply_cancel");assertEquals("",LdApp.settings.value.incomingCard)}
 @Test fun studioSwitchOffAndOnKeepsLibrary(){val id=seedProfiles();page();scroll("studio_enable");rule.onAllNodesWithText("Incoming try-out and profiles",useUnmergedTree=true).onLast().performClick();await{!LdApp.settings.value.incomingStudio};rule.onNodeWithTag("studio_off",true).assertExists();assertEquals(id,rows()[0].id);rule.onAllNodesWithText("Incoming try-out and profiles",useUnmergedTree=true).onLast().performClick();await{LdApp.settings.value.incomingStudio};assertEquals(id,rows()[0].id)}
}
