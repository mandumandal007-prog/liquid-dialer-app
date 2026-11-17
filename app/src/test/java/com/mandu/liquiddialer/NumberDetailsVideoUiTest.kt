package com.mandu.liquiddialer
import android.Manifest
import android.content.*
import android.telecom.PhoneAccountHandle
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
abstract class NumberDetailsVideoFixture{
 protected val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @get:Rule(order=0)val initial=object:org.junit.rules.ExternalResource(){override fun before(){runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false;it[K.numberDetailsGlass]=false};ctx.repo.flow.first{it.onboarded&&!it.motion&&!it.numberDetailsGlass}}}}
 @get:Rule(order=1)val rule=createAndroidComposeRule<MainActivity>()
 protected var closed=0;protected var longNumber="";protected var selected="+919876543210"
 private var host:ComposeView?=null
 protected fun settle(){rule.mainClock.autoAdvance=false;rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.advanceTimeBy(400);rule.waitForIdle()}
 protected fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 protected fun mount(content:@androidx.compose.runtime.Composable ()->Unit){rule.runOnUiThread{host?.disposeComposition();host=ComposeView(rule.activity).also{it.setContent{LdThemeHost{content()}};rule.activity.setContentView(it)}};settle()}
 protected fun show(glass:Boolean=true,night:Boolean=false,large:Boolean=false,saved:Boolean=true,long:Boolean=false,short:Boolean=false){
  settle();selected=if(short)"139"else"+919876543210"
  runBlocking{ctx.ldStore.edit{it[K.numberDetailsGlass]=glass;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(large)1.4f else 1f;it[K.motion]=false;it[K.autoPrefix]=false;it[K.defaultSim]="";it[K.simMemory]=false;it[K.undoCall]=false}}
  await{LdApp.settings.value.let{it.numberDetailsGlass==glass&&it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(large)1.4f else 1f)&&!it.autoPrefix&&!it.simMemory}}
  val c=if(saved)Contact(11,"lk1",if(short)"Railway"else if(long)"Rahul Sharma Customer Support and Delivery Office"else"Rahul Sharma","",selected,"Mobile",false,null)else null
  mount{ContactDetailsFrame({closed++}){NumberSheet(selected,c,rule.activity.state,rule.activity,{closed++},{},{},{longNumber=it})}}
  await{rule.onAllNodesWithTag("details_video",true).fetchSemanticsNodes().size==1}
 }
 protected fun move(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();settle()}
 protected fun hit(tag:String){move(tag);rule.onNodeWithTag(tag,true).performClick();settle()}
 protected fun simChoice(){Shadows.shadowOf(ctx).grantPermissions(Manifest.permission.CALL_PHONE);val cn=ComponentName("com.mandu","Phone");rule.runOnUiThread{rule.activity.state.sims=listOf(SimInfo(PhoneAccountHandle(cn,"sim0"),"Jio",1),SimInfo(PhoneAccountHandle(cn,"sim1"),"Airtel",2))};ContactControls.setSimPin(ctx,selected,null)}
 protected fun fits(){val grid=rule.onNodeWithTag("details_action_grid",true).getUnclippedBoundsInRoot();for(tag in listOf("details_call","details_video","details_sms","details_whatsapp")){val b=rule.onNodeWithTag(tag,true).getUnclippedBoundsInRoot();assertTrue("$tag fits",b.left>=grid.left&&b.right<=grid.right);assertTrue((b.bottom-b.top).value>=48f)};val a=rule.onNodeWithTag("details_call",true).getUnclippedBoundsInRoot();val v=rule.onNodeWithTag("details_video",true).getUnclippedBoundsInRoot();val sms=rule.onNodeWithTag("details_sms",true).getUnclippedBoundsInRoot();assertTrue(a.right<=v.left&&v.right<=sms.left)}
 @After fun cleanup(){rule.runOnUiThread{host?.disposeComposition();rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content).removeAllViews()};runBlocking{ctx.repo.set(K.numberDetailsGlass,false);ctx.repo.set(K.fontScale,1f)};ctx.scope.cancel()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsVideoActionsUiTest:NumberDetailsVideoFixture(){
 @Test fun videoUsesSelectedNumberAndVideoFlag(){show();simChoice();hit("details_video");assertEquals(selected to true,rule.activity.state.pendingCall);assertEquals(1,closed);assertNull(rule.activity.state.undoInfo)}
 @Test fun voiceRemainsVoice(){show();simChoice();hit("details_call");assertEquals(selected to false,rule.activity.state.pendingCall);assertEquals(1,closed)}
 @Test fun short139VideoUsesSameRouteNoRewrittenDigits(){show(short=true);simChoice();hit("details_video");assertEquals("139" to true,rule.activity.state.pendingCall);assertEquals(1,closed)}
 @Test fun originalLayoutAlsoHasVideo(){show(glass=false);rule.onNodeWithTag("details_glass_header",true).assertDoesNotExist();fits();simChoice();hit("details_video");assertEquals(selected to true,rule.activity.state.pendingCall)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsVideoSafetyUiTest:NumberDetailsVideoFixture(){
 @Test fun smsWhatsAppAndLongPressKeepSameNumber(){show();hit("details_sms");assertEquals("smsto:$selected",Shadows.shadowOf(rule.activity).nextStartedActivity.data.toString());hit("details_whatsapp");assertEquals(WhatsAppLink.url(selected),Shadows.shadowOf(rule.activity).nextStartedActivity.data.toString());move("details_number_options");rule.onNodeWithTag("details_number_options",true).performTouchInput{longClick()};settle();assertEquals(selected,longNumber);assertEquals(0,closed);assertNull(rule.activity.state.pendingCall)}
 @Test fun switchChangesLayoutNotNumberOrCall(){show();runBlocking{ctx.repo.set(K.numberDetailsGlass,false)};await{rule.onAllNodesWithTag("details_glass_header",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("details_video",true).assertExists();runBlocking{ctx.repo.set(K.numberDetailsGlass,true)};await{rule.onAllNodesWithTag("details_glass_header",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("details_number",true).assertTextEquals(Repo.pretty(selected));assertNull(rule.activity.state.pendingCall);assertEquals(0,closed)}
 @Test fun unknownNoFavouriteButVideoPresent(){show(saved=false);rule.onNodeWithTag("details_favourite",true).assertDoesNotExist();fits();rule.onNodeWithTag("details_video",true).assertExists();assertNull(rule.activity.state.pendingCall)}
 @Test fun defaultOffAndFinderRoute(){assertFalse(LdSettings().numberDetailsGlass);assertNotEquals(K.numberDetailsGlass,K.recentsOverview);assertEquals("settings:display",SettingsFinder.all.single{it.id=="number-details-glass"}.route)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsVideoDayPixelsUiTest:NumberDetailsVideoFixture(){
 @Test fun railwayDayGlass(){show(short=true);fits();rule.onNodeWithTag("details_favourite",true).assertIsDisplayed();saveShot(rule,"DETAILS491-day-railway");assertEquals(0,closed);assertNull(rule.activity.state.pendingCall)}
 @Test fun settingsOffTogglePersists(){show(glass=false);mount{Page("settings:display",rule.activity.state,rule.activity,{},{},null,"",{})};rule.mainClock.autoAdvance=true;rule.onNodeWithText("Number details glass layout",true).performScrollTo();settle();saveShot(rule,"DETAILS491-settings-off");rule.onNodeWithText("Number details glass layout",true).performClick();await{LdApp.settings.value.numberDetailsGlass};runBlocking{ctx.repo.set(K.numberDetailsGlass,false)};await{!LdApp.settings.value.numberDetailsGlass}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsVideoNightPixelsUiTest:NumberDetailsVideoFixture(){
 @Test fun nightLargeLongNameActions(){show(night=true,large=true,long=true);fits();saveShot(rule,"DETAILS491-night-large-header");move("details_favourite");rule.onNodeWithTag("details_video",true).assertIsDisplayed();rule.onNodeWithTag("details_favourite",true).assertIsDisplayed();saveShot(rule,"DETAILS491-night-large-actions");assertNull(rule.activity.state.pendingCall)}
 @Test fun originalNightStillFitsVideo(){show(glass=false,night=true,large=true,short=true);fits();move("details_video");rule.onNodeWithTag("details_video",true).assertIsDisplayed();saveShot(rule,"DETAILS491-night-original-video");assertEquals(0,closed)}
}
