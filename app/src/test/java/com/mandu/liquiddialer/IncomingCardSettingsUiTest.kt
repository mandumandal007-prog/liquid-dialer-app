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
class IncomingCardSettingsUiTest {
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
 @Test fun settingsMasterPersistsAndOffKeepsChoices(){page();rule.onNodeWithText("Use incoming-only controls").performClick();await{config().enabled};click("incoming_avatar_squircle");await{config().avatar=="squircle"};scroll("incoming_enable");rule.onNodeWithText("Use incoming-only controls").performClick();await{!config().enabled};assertEquals("squircle",config().avatar)}
 @Test fun eachLookChoicePersists(){page();click("incoming_style_pearl");await{config().style=="pearl"};click("incoming_bg_solid");await{config().background=="solid"};click("incoming_avatar_size_125");await{config().avatarSize==125};click("incoming_name_size_115");await{config().nameSize==115};click("incoming_travel_70");await{config().travel==70}}
 @Test fun independentSwitchesPersist(){page();for((tag,title)in listOf("incoming_number_toggle" to "Show extra phone number","incoming_type_toggle" to "Show contact type","incoming_last_toggle" to "Show last call","incoming_message_toggle" to "Show Message shortcut","incoming_whatsapp_toggle" to "Show WhatsApp shortcut","incoming_swap_toggle" to "Answer on left, Cut on right","incoming_swipe_toggle" to "Swipe up to Answer/Cut","incoming_confirm_toggle" to "Confirm incoming Cut")){scroll(tag);rule.onNodeWithText(title).performClick();settle()};await{config().confirmCut};val c=config();assertFalse(c.number);assertFalse(c.contactType);assertFalse(c.lastCall);assertFalse(c.message);assertFalse(c.whatsapp);assertTrue(c.swap);assertFalse(c.swipe)}
 @Test fun resetCancelThenConfirmDoesNotTouchShared(){runBlocking{ctx.ldStore.edit{it[K.incomingCard]=IncomingCard.encode(IncomingCard.Config(enabled=true,swap=true));it[K.callCardStyle]="cyber";it[K.confirmBeforeEnd]=true}};await{config().enabled};page();click("incoming_reset");rule.onNodeWithTag("incoming_reset_cancel").performClick();settle();assertTrue(config().swap);click("incoming_reset");rule.onNodeWithTag("incoming_reset_confirm").performClick();await{config()==IncomingCard.Config()};assertEquals("cyber",LdApp.settings.value.callCardStyle);assertTrue(LdApp.settings.value.confirmBeforeEnd)}
 @Test fun settingsDayAndNightLargePixels(){page();saveShot(rule,"IN448-settings-day");click("incoming_style_pearl");scroll("incoming_bg_solid");saveShot(rule,"IN448-settings-look");runBlocking{ctx.ldStore.edit{it[K.theme]="midnight";it[K.fontScale]=1.3f}};await{LdApp.settings.value.fontScale==1.3f};scroll("incoming_confirm_toggle");saveShot(rule,"IN448-settings-night-actions")}
 @Test fun settingsRoutingAndFinderReachPage(){var route="";mount{SettingsFinderPage({}, {route=it})};rule.onNode(hasSetTextAction()).performTextInput("incoming calling card");settle();scroll("settings_result_incoming-card");rule.onNodeWithTag("settings_result_incoming-card",true).performClick();assertEquals("incomingcard",route);assertTrue(SettingsFinder.search("incoming swipe travel").any{it.route=="incomingcard"})}
}
