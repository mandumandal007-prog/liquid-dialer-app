package com.mandu.liquiddialer
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.*
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
abstract class LiveCallDeskFixture{
 @get:Rule(order=0)val initial=object:org.junit.rules.ExternalResource(){override fun before(){org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,ComponentActivity::class.java));runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false};ctx.repo.flow.first{it.onboarded&&!it.motion}}}}
 @get:Rule(order=1)val rule=createAndroidComposeRule<ComponentActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 protected val ctx get()=ApplicationProvider.getApplicationContext<LdApp>();private var host:ComposeView?=null
 protected lateinit var target:Call
 protected fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(300);rule.waitForIdle()}
 protected fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 protected fun mount(content:@Composable ()->Unit){rule.runOnUiThread{host=ComposeView(rule.activity).also{it.setContent{LdThemeHost{content()}};rule.activity.setContentView(it)}};settle()}
 protected fun seed(on:Boolean=true,night:Boolean=false){runBlocking{ctx.ldStore.edit{it[K.liveCallDesk]=on;it[K.callTouchGuard]=false;it[K.callPace]=false;it[K.hiddenFeatures]=emptySet();it[K.layoutGrid]=DEFAULT_GRID;it[K.layoutBottom]=DEFAULT_BOTTOM;it[K.callTally]=false;it[K.callReferences]=false;it[K.callNoteReader]=false;it[K.callCanvas]=false;it[K.messageDrafts]=false;it[K.callDates]=false;it[K.callConverter]=false;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.confirmBeforeEnd]=true}};await{LdApp.settings.value.let{it.liveCallDesk==on&&it.fontScale==(if(night)1.4f else 1f)&&it.confirmBeforeEnd}};CallManager.videoUpgradeFrom.value=null;CallManager.recording.value=null;CallPace.status.value=null;CallManager.audio.value=null;File(ctx.filesDir,"notes.json").delete()}
 protected fun show(on:Boolean=true,night:Boolean=false,state:Int=Call.STATE_ACTIVE){seed(on,night);target=FakeCall.make("+919876543210","Support desk",state,System.currentTimeMillis()-92000);FakeCall.setChildren(target,emptyList());CallManager.calls.value=listOf(target);mount{CallScreen({},{},{},{})}}
 protected fun click(tag:String){rule.onNodeWithTag(tag,true).performClick();settle()}
 protected fun scroll(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false}
 protected fun open(){click("call_desk_open");rule.onNodeWithTag("call_desk",true).assertExists()}
 @After fun clean(){rule.runOnUiThread{host?.disposeComposition();rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content).removeAllViews()};CallManager.calls.value=emptyList();CallManager.videoUpgradeFrom.value=null;CallManager.audio.value=null;File(ctx.filesDir,"notes.json").delete();runBlocking{ctx.ldStore.edit{it[K.liveCallDesk]=false;it[K.fontScale]=1f;it[K.hiddenFeatures]=emptySet();it[K.confirmBeforeEnd]=false;it[K.layoutGrid]=DEFAULT_GRID;it[K.layoutBottom]=DEFAULT_BOTTOM}}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveCallDeskUiTest:LiveCallDeskFixture(){
 @Test fun offUnchangedNormalCard(){show(false);rule.onAllNodesWithTag("call_desk_open",true).assertCountEquals(0);rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_button_Mute",true).assertExists()}
 @Test fun openKeepsIdentityTimerAndControls(){show();saveShot(rule,"DESK454-card-day");open();rule.onNodeWithTag("call_desk_name",true).assertTextEquals("Support desk");rule.onNodeWithTag("call_desk_number",true).assertTextEquals("+919876543210");rule.onNodeWithTag("call_desk_timer",true).assertTextContains("Connected",substring=true);rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertEquals(Call.STATE_ACTIVE,CallManager.state(target));assertNull(CallManager.recording.value);saveShot(rule,"DESK454-desk-day")}
 @Test fun closeCancelPreservesDraftThenConfirmDiscards(){show();open();scroll("call_note_draft");rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].performTextInput("unsaved work");click("call_desk_close");click("call_desk_close_cancel");rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].assertTextContains("unsaved work");click("call_desk_close");click("call_desk_close_confirm");rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty());open();scroll("call_note_draft");rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].assertTextEquals("")}
 @Test fun noteExplicitSaveOnlyAndCorrectNumber(){show();open();scroll("call_note_draft");rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].performTextInput("Ask about delivery");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty());scroll("call_note_save");click("call_note_save");assertEquals("Ask about delivery",Repo.notesFor(ctx,"+919876543210").single().text);assertTrue(Repo.notesFor(ctx,"+449876543210").isEmpty());click("call_desk_close");click("call_desk_close_confirm");open();assertEquals(1,Repo.notesFor(ctx,"+919876543210").size)}
 @Test fun checklistToolKeepsEndAndReturns(){show();open();click("call_checklist_open");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_name",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty());saveShot(rule,"DESK454-checklist-day")}
 @Test fun calculatorToolKeepsEnd(){show();open();click("call_estimate_open");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_timer",true).assertIsDisplayed();saveShot(rule,"DESK454-calculator-day")}
 @Test fun endStillRequiresExistingConfirmation(){show();open();click("end_call");rule.onNodeWithTag("end_confirm",true).assertExists();assertEquals(Call.STATE_ACTIVE,CallManager.state(target));click("end_confirm_cancel");rule.onNodeWithTag("call_desk",true).assertExists()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveCallDeskSafetyUiTest:LiveCallDeskFixture(){
 @Test fun secondCallClearsDoesNotReopen(){show();open();val other=FakeCall.make("+919999999999","Waiting",Call.STATE_RINGING);CallManager.calls.value=listOf(target,other);settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);CallManager.calls.value=listOf(target);settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);rule.onNodeWithTag("call_desk_open",true).assertExists()}
 @Test fun sameNumberReplacementCannotInheritDraftOrEndConfirm(){show();open();click("end_call");target=FakeCall.make("+919876543210","New call same number",Call.STATE_ACTIVE,System.currentTimeMillis());CallManager.calls.value=listOf(target);settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);rule.onAllNodesWithTag("end_confirm",true).assertCountEquals(0)}
 @Test fun offClearsDesk(){show();open();runBlocking{ctx.repo.set(K.liveCallDesk,false)};await{rule.onAllNodesWithTag("call_desk",true).fetchSemanticsNodes().isEmpty()};rule.onAllNodesWithTag("call_desk_open",true).assertCountEquals(0)}
 @Test fun hiddenNotesClearsDeskAndEntry(){show();open();runBlocking{ctx.repo.set(K.hiddenFeatures,setOf("notes"))};await{rule.onAllNodesWithTag("call_desk",true).fetchSemanticsNodes().isEmpty()};rule.onAllNodesWithTag("call_desk_open",true).assertCountEquals(0)}
 @Test fun videoRequestClearsDesk(){show();open();CallManager.videoUpgradeFrom.value=target;settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);rule.onNodeWithTag("video_accept",true).assertExists()}
 @Test fun layoutHidesMuteSpeakerNotEnd(){show();runBlocking{ctx.ldStore.edit{it[K.layoutGrid]="!mute,keypad,record,hold,add,merge,!audio";it[K.layoutBottom]="!speaker,end,notes"}};await{LdApp.settings.value.layoutBottom.startsWith("!speaker")};open();rule.onAllNodesWithTag("call_desk_mute",true).assertCountEquals(0);rule.onAllNodesWithTag("call_desk_speaker",true).assertCountEquals(0);rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun holdClearsDeskAndDoesNotAutoReopen(){show();open();val details=target.details;val f=Call.Details::class.java.getDeclaredField("mState");f.isAccessible=true;f.set(details,Call.STATE_HOLDING);assertEquals(Call.STATE_HOLDING,CallManager.state(target));CallManager.bump();settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);f.set(details,Call.STATE_ACTIVE);CallManager.bump();settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0);rule.onNodeWithTag("call_desk_open",true).assertExists()}
 @Test fun disconnectedClearsDesk(){show();open();CallManager.calls.value=emptyList();settle();rule.onAllNodesWithTag("call_desk",true).assertCountEquals(0)}
 @Test fun audioStateUpdatesShown(){show();open();CallManager.audio.value=android.telecom.CallAudioState(true,android.telecom.CallAudioState.ROUTE_SPEAKER,android.telecom.CallAudioState.ROUTE_SPEAKER);settle();rule.onNodeWithTag("call_button_Unmute",true).assertExists();rule.onNodeWithTag("call_button_Speaker on",true).assertExists();rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun hiddenToolsStayHidden(){show();runBlocking{ctx.repo.set(K.hiddenFeatures,setOf("checklist","estimate","brief","timeline"))};await{LdApp.settings.value.hiddenFeatures.size==4};open();rule.onAllNodesWithTag("call_checklist_open",true).assertCountEquals(0);rule.onAllNodesWithTag("call_estimate_open",true).assertCountEquals(0);rule.onNodeWithTag("call_note_draft",true).assertExists()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveCallDeskPixelsUiTest:LiveCallDeskFixture(){
 @Test fun narrowNightLargeDeskAndClose(){show(night=true);open();rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();saveShot(rule,"DESK454-narrow-night-large");click("call_desk_close");rule.onNodeWithTag("call_desk_close_confirm",true).assertIsDisplayed();saveDialogShot("DESK454-narrow-confirm-night-large",0xFF10182BL)}
 @Test fun settingsDefaultReadThenEnable(){seed(false,night=true);mount{LiveCallDeskPage{}};assertFalse(LdApp.settings.value.liveCallDesk);saveShot(rule,"DESK454-settings-night-large");rule.onAllNodesWithText("Live call desk",useUnmergedTree=true)[1].performClick();await{LdApp.settings.value.liveCallDesk}}
}
