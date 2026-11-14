package com.mandu.liquiddialer
import android.telecom.Call
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class IncomingCardScreenTest {
 @get:Rule val rule=createAndroidComposeRule<CallActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1500);rule.waitForIdle();Thread.sleep(100);rule.mainClock.advanceTimeBy(300);rule.waitForIdle()}
 private fun seed(c:IncomingCard.Config=IncomingCard.Config(enabled=true,swipe=false),theme:String="midnight",font:Float=1f){runBlocking{ctx.ldStore.edit{it.clear();it[K.incomingCard]=IncomingCard.encode(c);it[K.theme]=theme;it[K.motion]=false;it[K.fontScale]=font;it[K.waPill]=false}};rule.waitUntil(15000){LdApp.settings.value.incomingCard==IncomingCard.encode(c)&&LdApp.settings.value.fontScale==font};CallManager.audio.value=android.telecom.CallAudioState(false,1,9)}
 private fun call(state:Int=Call.STATE_RINGING,name:String="Rahul Sharma",video:Int=0):Call {val c=FakeCall.make("+919876543210",name,state,if(state==Call.STATE_ACTIVE)System.currentTimeMillis()-70000 else 0,videoState=video);CallManager.calls.value=listOf(c);CallManager.bump();settle();return c}
 private fun reveal(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false;settle()}
 @After fun clean(){CallManager.calls.value=emptyList();CallManager.bump();runBlocking{ctx.ldStore.edit{it[K.incomingCard]="";it[K.fontScale]=1f}};org.robolectric.RuntimeEnvironment.setFontScale(1f)}
 @Test fun defaultOffUsesExistingIncomingAndSharedSwipe(){seed(IncomingCard.Config(enabled=false,swipe=false,message=false,swap=true));call();rule.onNodeWithTag("incoming_default").assertExists();rule.onNodeWithTag("incoming_message").assertExists();rule.onNodeWithTag("incoming_normal").assertExists();rule.onAllNodesWithText("Upar khiskao").assertCountEquals(2);saveShot(rule,"IN448-default-off")}
 @Test fun incomingCustomIdentityButtonsAndNoMessage(){seed(IncomingCard.Config(enabled=true,swipe=false,message=false,number=false,whatsapp=false,style="pearl",avatar="squircle",avatarSize=125,nameSize=115),"daylight");call();rule.onNodeWithTag("incoming_custom").assertExists();rule.onNodeWithTag("call_name").assertTextEquals("Rahul Sharma");rule.onAllNodesWithTag("incoming_number").assertCountEquals(0);rule.onAllNodesWithTag("incoming_message").assertCountEquals(0);reveal("incoming_answer");rule.onNodeWithTag("incoming_answer").assertIsDisplayed();rule.onNodeWithTag("incoming_cut").assertIsDisplayed();saveShot(rule,"IN448-pearl-incoming")}
 @Test fun unknownCallerNumberRemainsIdentity(){seed(IncomingCard.Config(enabled=true,swipe=false,number=false));call(name="");rule.onNodeWithTag("call_name").assertExists();assertTrue(rule.onNodeWithTag("call_name").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString().contains("98765"))}
 @Test fun activeAndOutgoingIgnoreOverridesAndKeepEndPill(){seed(IncomingCard.Config(enabled=true,swap=true,style="pearl",avatarSize=125,nameSize=115,number=false,confirmCut=true));for(state in listOf(Call.STATE_DIALING,Call.STATE_ACTIVE)){call(state);rule.onAllNodesWithTag("incoming_custom").assertCountEquals(0);rule.onNodeWithTag("end_call").assertIsDisplayed();rule.onAllNodesWithTag("incoming_decline_confirm").assertCountEquals(0);if(state==Call.STATE_ACTIVE)rule.onNodeWithTag("call_timer").assertIsDisplayed();saveShot(rule,"IN448-unchanged-$state")}}
 @Test fun declineConfirmationDisappearsOnActiveAndDifferentCall(){seed(IncomingCard.Config(enabled=true,swipe=false,confirmCut=true));call();reveal("incoming_cut");rule.onNodeWithTag("incoming_cut").performClick();settle();rule.onNodeWithTag("incoming_decline_confirm").assertExists();saveDialogShot("IN448-decline-confirm",0xff0d1320);call(Call.STATE_ACTIVE);rule.onAllNodesWithTag("incoming_decline_confirm").assertCountEquals(0);rule.onNodeWithTag("end_call").assertIsDisplayed();call(name="Second caller");rule.onAllNodesWithTag("incoming_decline_confirm").assertCountEquals(0)}
 @Test @Config(qualifiers="w320dp-h640dp-xhdpi") fun narrowLargeFontSwappedVideoActionsReachable(){seed(IncomingCard.Config(enabled=true,swipe=false,swap=true,avatarSize=125,nameSize=115,background="solid",style="pearl"),"midnight",1.5f);call(name="बहुत लंबा नाम Rahul Sharma support",video=3);rule.onNodeWithTag("call_name").assertExists();saveShot(rule,"IN448-narrow-large-identity");reveal("incoming_answer");rule.onNodeWithTag("incoming_answer").assertIsDisplayed();rule.onNodeWithTag("incoming_cut").assertIsDisplayed();rule.onNodeWithTag("answer_audio").assertIsDisplayed();rule.onNodeWithTag("incoming_message").assertIsDisplayed();val a=rule.onNodeWithTag("incoming_answer").fetchSemanticsNode();val c=rule.onNodeWithTag("incoming_cut").fetchSemanticsNode();assertTrue(a.positionInRoot.x+a.size.width<=c.positionInRoot.x);saveShot(rule,"IN448-narrow-large-video-actions")}
 @Test fun brightPhotoIncomingRemainsReadableAndNoExternalOpen(){
  val f=java.io.File(ctx.cacheDir,"incoming-bright.png");val b=android.graphics.Bitmap.createBitmap(320,640,android.graphics.Bitmap.Config.ARGB_8888);b.eraseColor(android.graphics.Color.YELLOW);f.outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle();assertTrue(CallPhotos.save(ctx,"+919876543210",android.net.Uri.fromFile(f)))
  try{seed(IncomingCard.Config(enabled=true,style="pearl",background="photo",swipe=false),"daylight");call();rule.onNodeWithTag("call_name").assertIsDisplayed();reveal("incoming_answer");rule.onNodeWithTag("incoming_cut").assertIsDisplayed();saveShot(rule,"IN448-bright-photo");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}finally{CallPhotos.remove(ctx,"+919876543210")}
 }
 @Test fun photoFallbackAndAllStyleBackgroundPairsRender(){for((style,bg)in listOf("glass" to "blobs","cyber" to "solid","pearl" to "photo","filigree" to "inherit")){seed(IncomingCard.Config(enabled=true,style=style,background=bg,swipe=true,travel=150,swap=true),"daylight");call();rule.onNodeWithTag("hero_avatar").assertIsDisplayed();reveal("incoming_answer");rule.onNodeWithTag("incoming_answer").assertIsDisplayed();rule.onNodeWithTag("incoming_cut").assertIsDisplayed();saveShot(rule,"IN448-$style-$bg")}}
}
