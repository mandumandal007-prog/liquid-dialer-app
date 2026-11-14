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
class IncomingRehearsalUiTest {
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
 private fun show(c:IncomingCard.Config=IncomingCard.Config(enabled=true,swipe=false,swap=true,confirmCut=true),closed:()->Unit={}){mount{IncomingRehearsalPage(c,"Sample profile",closed)}}
 @Test fun explicitSimulationIdentityAndNoReadOrExternalAction(){show();rule.onNodeWithTag("studio_demo_label",true).assertTextEquals("SIMULATION ONLY");rule.onNodeWithTag("studio_demo_name",true).assertTextEquals("Demo caller");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun answerRecordsOnlyDemoThenRetryWorks(){val before=LdApp.settings.value;show();click("incoming_answer");rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("Answer rehearsal complete. No call answered");click("studio_demo_retry");rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("No demo action yet");click("incoming_answer");rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("Answer rehearsal complete. No call answered");assertEquals(before,LdApp.settings.value)}
 @Test fun cutUsesClearlySimulatedConfirmCancelAndConfirm(){show();click("incoming_cut");rule.onNodeWithText("Rehearse decline confirmation?").assertExists();rule.onAllNodesWithText("Decline this incoming call?").assertCountEquals(0);rule.onNodeWithTag("incoming_decline_cancel",true).performClick();settle();click("incoming_cut");rule.onNodeWithTag("incoming_decline_confirm",true).performClick();settle();rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("Cut rehearsal complete. No call declined")}
 @Test fun messageAndWhatsappOnlySetLocalNotice(){show();click("incoming_message");rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("Message rehearsal only. Nothing opened, sent or declined");click("studio_demo_whatsapp");rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("WhatsApp rehearsal only. Nothing opened or sent");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun sampleVisibilityChoicesActuallyApply(){show(IncomingCard.Config(enabled=true,swipe=false,number=false,message=false,whatsapp=false));rule.onAllNodesWithTag("studio_demo_number",true).assertCountEquals(0);rule.onAllNodesWithTag("incoming_message",true).assertCountEquals(0);rule.onAllNodesWithTag("studio_demo_whatsapp",true).assertCountEquals(0);click("incoming_answer");rule.onNodeWithTag("studio_demo_name",true).assertExists()}
 @Test fun videoToggleHasAudioFallbackWithoutCamera(){show();scroll("studio_video_toggle");rule.onNodeWithText("Video call sample",useUnmergedTree=true).performClick();settle();click("answer_audio");rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("Audio-only rehearsal complete. No camera or call opened");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity);saveShot(rule,"ST449-video-actions")}
 @Test fun sameSwipeControlsRejectShortDragAndAcceptLong(){show(IncomingCard.Config(enabled=true,swipe=true,travel=70,message=false));scroll("incoming_answer");rule.onNodeWithTag("incoming_answer",true).performTouchInput{swipe(center,center.copy(y=center.y-50f),300)};settle();rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("No demo action yet");rule.onNodeWithTag("incoming_answer",true).performTouchInput{swipe(center,center.copy(y=center.y-220f),500)};settle();rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("Answer rehearsal complete. No call answered")}
 @Test fun closeDoesNotMutateSettings(){var closed=false;val before=LdApp.settings.value;show(closed={closed=true});click("studio_demo_close");assertTrue(closed);assertEquals(before,LdApp.settings.value)}
 @Test fun dayAndNightRehearsalPixels(){show(IncomingCard.Config(enabled=true,swipe=false,style="pearl",avatar="squircle",avatarSize=125,nameSize=115));scroll("studio_demo_name");saveShot(rule,"ST449-rehearsal-day");runBlocking{ctx.ldStore.edit{it[K.theme]="midnight";it[K.fontScale]=1.3f}};await{LdApp.settings.value.fontScale==1.3f};scroll("incoming_answer");saveShot(rule,"ST449-rehearsal-night-large")}
 @Test @Config(qualifiers="w320dp-h640dp-xhdpi") fun narrowLargeVideoSampleControlsReachable(){runBlocking{ctx.ldStore.edit{it[K.fontScale]=1.5f;it[K.theme]="midnight"}};await{LdApp.settings.value.fontScale==1.5f};show(IncomingCard.Config(enabled=true,swipe=false,swap=true,confirmCut=true,nameSize=115,avatarSize=125,style="glass",background="solid"));scroll("studio_video_toggle");rule.onNodeWithText("Video call sample",useUnmergedTree=true).performClick();settle();scroll("incoming_answer");rule.onNodeWithTag("incoming_answer",true).assertIsDisplayed();rule.onNodeWithTag("incoming_cut",true).assertIsDisplayed();rule.onNodeWithTag("answer_audio",true).assertIsDisplayed();saveShot(rule,"ST449-narrow-video-large");click("answer_audio");rule.onNodeWithTag("studio_demo_action",true).assertTextEquals("Audio-only rehearsal complete. No camera or call opened")}
}
