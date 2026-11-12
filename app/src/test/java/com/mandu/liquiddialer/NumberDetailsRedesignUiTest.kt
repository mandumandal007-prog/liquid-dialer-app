package com.mandu.liquiddialer
import android.content.*
import android.database.*
import android.net.Uri
import android.provider.CallLog
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@OptIn(ExperimentalTestApi::class)
abstract class NumberDetailsRedesignFixture{
 protected val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @get:Rule(order=0) val initial=object:org.junit.rules.ExternalResource(){override fun before(){runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false};ctx.repo.flow.first{it.onboarded&&!it.motion}}}}
 protected val dispatcher=kotlinx.coroutines.test.StandardTestDispatcher()
 @get:Rule(order=1) val rule=createAndroidComposeRule<MainActivity>(effectContext=dispatcher)
 protected val number="+919876543210"
 protected var closed=0;protected var editCount=0;protected var edited:Long?=null;protected var notes=0;protected var longNumber="";protected var checklist=0;protected var estimate=0;protected var timeline=0;protected var briefing=0
 protected lateinit var act:MainActivity
 protected fun settle(){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();dispatcher.scheduler.runCurrent();rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(600);dispatcher.scheduler.runCurrent();rule.waitForIdle()}
 protected fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 protected fun show(night:Boolean=false,large:Boolean=false,saved:Boolean=true,long:Boolean=false,history:Boolean=false){
  settle();runBlocking{ctx.ldStore.edit{it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(large)1.4f else 1f;it[K.motion]=false;it[K.confirmCall]=true;it[K.refinedHeaders]=false}};await{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(large)1.4f else 1f)&&it.confirmCall}}
  act=rule.activity
  val c=if(saved)Contact(11,"lk1",if(long)"Rahul Sharma Customer Support and Delivery Office"else"Rahul Sharma","",number,"Mobile",false,null)else null
  act.state.showOnboarding=false;act.state.recents=emptyList();act.state.isDefault=true
  if(history){org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CALL_LOG);org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,object:ContentProvider(){
   override fun onCreate()=true
   override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor=MatrixCursor(p).apply{addRow(arrayOf(1L,number,"Rahul Sharma",1,System.currentTimeMillis()-60000,125L,null,null))}
   override fun insert(u:Uri,v:ContentValues?):Uri?=null;override fun delete(u:Uri,s:String?,a:Array<out String>?)=0;override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0;override fun getType(u:Uri):String?=null
  })}
  rule.runOnUiThread{act.mountTestPage{LdThemeHost{ContactDetailsFrame({closed++}){NumberSheet(number,c,act.state,act,{closed++},{edited=it;editCount++},{notes++},{longNumber=it},{checklist++},{estimate++},{timeline++},{briefing++})}}}};settle()
 }
 protected fun reveal(text:String){
  val node=rule.onNodeWithText(text,substring=true,useUnmergedTree=true)
  var visible=false
  repeat(16){if(!visible){visible=try{node.assertIsDisplayed();true}catch(_:AssertionError){false};if(!visible){val r=node.getUnclippedBoundsInRoot();val scroller=rule.onNodeWithTag("page_content_scroll",true);val view=scroller.getUnclippedBoundsInRoot();scroller.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,if(r.top<view.top)-250f else 250f)};settle()}}}
  try{node.assertIsDisplayed()}catch(t:AssertionError){throw AssertionError(rule.onRoot(true).printToString(maxDepth=8),t)}
 }
 protected fun clickText(text:String){reveal(text);rule.onNodeWithText(text,substring=true,useUnmergedTree=true).performClick();settle()}
 @After fun clean(){rule.runOnUiThread{act.findViewById<android.view.ViewGroup>(android.R.id.content).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f}}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsRedesignUiTest:NumberDetailsRedesignFixture(){
 @Test fun dayIdentityAndActionsPixels(){show(history=true);rule.onNodeWithTag("contact_details_frame",true).assertExists();rule.onNodeWithTag("details_name",true).assertTextEquals("Rahul Sharma");rule.onNodeWithTag("details_sms",true).assertIsDisplayed();rule.onNodeWithTag("details_number",true).assertExists();rule.onNodeWithText("Recent activity",useUnmergedTree=true).assertExists();assertEquals(1,Repo.historyFor(ctx,number).size);assertNull(act.state.confirmNumber);assertEquals(0,closed);saveShot(rule,"DETAILS414-day")}
 @Test fun longNameNightLargePixels(){show(night=true,large=true,long=true);rule.onNodeWithTag("details_name",true).assertTextEquals("Rahul Sharma Customer Support and Delivery Office");saveShot(rule,"DETAILS414-night-large");reveal("Number copy karo");saveShot(rule,"DETAILS414-night-scrolled")}
 @Test fun unknownNumberPixelsAndAdd(){show(saved=false);rule.onAllNodesWithText("Fav karo",true).assertCountEquals(0);rule.onNodeWithText("Contact me add karo",useUnmergedTree=true).assertExists();saveShot(rule,"DETAILS414-unknown");clickText("Contact me add karo");assertEquals(1,editCount);assertNull(edited);assertEquals(0,closed)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsRedesignActionsUiTest:NumberDetailsRedesignFixture(){
 @Test fun copyOnlyOnExplicitTap(){show();val cb=ctx.getSystemService(ClipboardManager::class.java);cb.setPrimaryClip(ClipData.newPlainText("before","unchanged"));assertEquals("unchanged",cb.primaryClip!!.getItemAt(0).text.toString());clickText("Number copy karo");assertEquals(number,cb.primaryClip!!.getItemAt(0).text.toString());assertEquals(0,closed)}
 @Test fun smsAndWhatsAppUseThisNumber(){show();rule.onNodeWithTag("details_sms",true).performClick();settle();val sms=org.robolectric.Shadows.shadowOf(act).nextStartedActivity;assertEquals(Intent.ACTION_SENDTO,sms.action);assertEquals("smsto:$number",sms.data.toString());rule.onNodeWithTag("details_whatsapp",true).performClick();settle();val wa=org.robolectric.Shadows.shadowOf(act).nextStartedActivity;assertEquals(WhatsAppLink.url(number),wa.data.toString());assertNull(act.state.confirmNumber)}
 @Test fun numberLongPressAndBack(){show();rule.onNodeWithTag("details_number_options",true).performTouchInput{longClick()};settle();assertEquals(number,longNumber);rule.onNodeWithTag("page_back",true).performClick();settle();assertEquals(1,closed);assertNull(act.state.confirmNumber)}
}

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsRedesignPreservationUiTest:NumberDetailsRedesignFixture(){
 @Test fun preferencesStayOffUntilTappedAndKeepFullIdentity(){
  val foreign="+449876543210";listOf(number,foreign).forEach{ContactControls.setQuietAllow(ctx,it,false);ContactControls.setRecordAlways(ctx,it,false);ContactControls.setAnnounceAlways(ctx,it,false)}
  show();assertFalse(ContactControls.quietAllow(ctx,number));assertFalse(ContactControls.recordAlways(ctx,number));assertFalse(ContactControls.announceAlways(ctx,number))
  clickText("Quiet hours me bajne do");assertTrue(ContactControls.quietAllow(ctx,number));assertFalse(ContactControls.quietAllow(ctx,foreign))
  clickText("Is number ki recording");assertTrue(ContactControls.recordAlways(ctx,number));assertFalse(ContactControls.recordAlways(ctx,foreign))
  clickText("Is number ka naam bolo");assertTrue(ContactControls.announceAlways(ctx,number));assertFalse(ContactControls.announceAlways(ctx,foreign));assertEquals(0,closed)
 }
 @Test fun toolsAndNotesStillDispatch(){show();clickText("Checklist");assertEquals(1,checklist);clickText("Calculator");assertEquals(1,estimate);clickText("Timeline");assertEquals(1,timeline)
  rule.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("details_tools")),true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(250f,0f)};settle()
  clickText("Briefing");assertEquals(1,briefing);clickText("Notes (");assertEquals(1,notes);assertEquals(0,closed);assertNull(act.state.confirmNumber)
 }
 @Test fun reminderAndDeleteNeedExplicitChoice(){CallReminders.cancel(ctx,number);show();clickText("Call reminder");rule.onNodeWithText("1 ghante baad",true,true).assertExists();assertNull(CallReminders.forNumber(ctx,number));clickText("1 ghante baad");assertNotNull(CallReminders.forNumber(ctx,number));clickText("Call reminder");clickText("Reminder hataye");assertNull(CallReminders.forNumber(ctx,number));clickText("Contact delete karo");reveal("Pakka delete karo?");assertEquals(0,closed);saveShot(rule,"DETAILS414-delete-confirm")}
 @Test fun editFavouriteAndShareStillDispatch(){show();clickText("Fav karo");rule.onNodeWithText("Favourite",true,true).assertExists();clickText("Contact edit karo");assertEquals(1,editCount);assertEquals(11L,edited);clickText("Number share karo");val chooser=org.robolectric.Shadows.shadowOf(act).nextStartedActivity;assertEquals(Intent.ACTION_CHOOSER,chooser.action);val share=chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!;assertEquals("Rahul Sharma: $number",share.getStringExtra(Intent.EXTRA_TEXT));assertEquals(0,closed)}
 @Test fun callTargetUsesSameNumberAndUndoSetting(){runBlocking{ctx.ldStore.edit{it[K.undoCall]=true;it[K.autoPrefix]=false;it[K.defaultSim]=""};ctx.repo.flow.first{it.undoCall&&!it.autoPrefix}};show();org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.CALL_PHONE);rule.onNodeWithTag("details_call",true).performClick();assertEquals(number,act.state.undoInfo?.first);assertEquals(1,closed)}
}
