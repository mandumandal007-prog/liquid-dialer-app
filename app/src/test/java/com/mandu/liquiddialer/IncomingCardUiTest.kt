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
class IncomingCardUiTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,androidx.activity.ComponentActivity::class.java));runBlocking{ctx.ldStore.edit{it.clear();it[K.onboarded]=true;it[K.motion]=false;it[K.theme]="daylight"}}}}
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
 private fun page(){mount{IncomingCardPage({})}}
 @After fun clean(){rule.runOnUiThread{hostView?.disposeComposition();(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};runBlocking{ctx.ldStore.edit{it[K.incomingCard]="";it[K.fontScale]=1f}};org.robolectric.RuntimeEnvironment.setFontScale(1f)}
 @Test fun answerAndCutTapDispatchOnce(){actions();rule.onNodeWithTag("incoming_answer").performClick();rule.onNodeWithTag("incoming_answer").performClick();rule.onNodeWithTag("incoming_cut").performClick();rule.onNodeWithTag("incoming_cut").performClick();assertEquals(1,answers);assertEquals(1,cuts)}
 @Test fun messageToggleHidesOnlyMessage(){actions(IncomingCard.Config(enabled=true,swipe=false,message=false));rule.onAllNodesWithTag("incoming_message").assertCountEquals(0);rule.onNodeWithTag("incoming_cut").assertIsDisplayed();rule.onNodeWithTag("incoming_answer").assertIsDisplayed()}
 @Test fun offIgnoresMessageSwapAndConfirmation(){actions(IncomingCard.Config(enabled=false,swipe=false,message=false,swap=true,confirmCut=true));rule.onNodeWithTag("incoming_message").assertExists();rule.onNodeWithTag("incoming_normal").assertExists();rule.onNodeWithTag("incoming_cut").performClick();assertEquals(1,cuts);rule.onAllNodesWithTag("incoming_decline_confirm").assertCountEquals(0)}
 @Test fun swapChangesActualPositions(){actions(IncomingCard.Config(enabled=true,swipe=false,swap=true));val a=rule.onNodeWithTag("incoming_answer").fetchSemanticsNode().positionInRoot.x;val c=rule.onNodeWithTag("incoming_cut").fetchSemanticsNode().positionInRoot.x;assertTrue(a<c);rule.onNodeWithTag("incoming_answer").performClick();assertEquals(1,answers);assertEquals(0,cuts)}
 @Test fun confirmCancelCanRetryThenConfirmOnce(){actions(IncomingCard.Config(enabled=true,swipe=false,confirmCut=true));rule.onNodeWithTag("incoming_cut").performClick();settle();assertEquals(0,cuts);rule.onNodeWithTag("incoming_decline_cancel").performClick();settle();rule.onNodeWithTag("incoming_cut").performClick();settle();rule.onNodeWithTag("incoming_decline_confirm").performClick();settle();assertEquals(1,cuts)}
 @Test fun answerBypassesCutConfirmation(){actions(IncomingCard.Config(enabled=true,swipe=false,confirmCut=true));rule.onNodeWithTag("incoming_answer").performClick();assertEquals(1,answers);assertEquals(0,cuts);rule.onAllNodesWithTag("incoming_decline_confirm").assertCountEquals(0)}
 @Test fun videoAudioFallbackAlwaysPresentWithoutMessage(){actions(IncomingCard.Config(enabled=true,swipe=false,message=false,confirmCut=true,swap=true),true);rule.onNodeWithTag("answer_audio").assertIsDisplayed().performClick();assertEquals(1,audio);assertEquals(0,answers);assertEquals(0,cuts);saveShot(rule,"IN448-video-swapped")}
 @Test fun messageDispatchIsOnlyExplicitTap(){actions();assertEquals(0,messages);rule.onNodeWithTag("incoming_message").performClick();assertEquals(1,messages);assertEquals(0,cuts);assertEquals(0,answers)}
 @Test fun swipeModeDoesNotRespondToTap(){actions(IncomingCard.Config(enabled=true,swipe=true));rule.onNodeWithTag("incoming_answer").performTouchInput{click()};settle();assertEquals(0,answers)}
 @Test fun shortSwipeDoesNotDispatch(){actions(IncomingCard.Config(enabled=true,swipe=true,travel=150));rule.onNodeWithTag("incoming_answer").performTouchInput{swipe(center,center.copy(y=center.y-50f),300)};settle();assertEquals(0,answers)}
 @Test fun longSwipeDispatchesOnce(){actions(IncomingCard.Config(enabled=true,swipe=true,travel=70));rule.onNodeWithTag("incoming_answer").performTouchInput{swipe(center,center.copy(y=center.y-200f),500)};settle();assertEquals(1,answers);assertEquals(0,cuts)}
}
