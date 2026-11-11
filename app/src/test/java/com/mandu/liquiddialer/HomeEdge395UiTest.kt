package com.mandu.liquiddialer
import android.Manifest
import android.provider.CallLog
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class HomeEdge395UiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1400);rule.waitForIdle()}
 private fun setup(theme:String="daylight",font:Float=1f,bottom:Boolean=false,contacts:Boolean=false,style:String="glass"){
  settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.lang]="english";it[K.searchBottom]=bottom;it[K.showFavourites]=false;it[K.showTodayStats]=false;it[K.groupByDay]=true;it[K.recentsStyle]=style}}
  rule.waitUntil(15000){LdApp.settings.value.let{it.theme==theme&&it.fontScale==font&&it.searchBottom==bottom&&it.lang=="english"}};settle()
  Shadows.shadowOf(ctx).grantPermissions(Manifest.permission.READ_CALL_LOG,Manifest.permission.READ_CONTACTS)
  rule.runOnUiThread{val st=rule.activity.state;st.showOnboarding=false;st.isDefault=true;st.tab=if(contacts)"contacts"else"recents"
   st.contacts=(1..20).map{Contact(it.toLong(),"lk$it",if(it==2)"बहुत लंबा दुकान का नाम · Supplier Delivery Office" else "Person ${it.toString().padStart(2,'0')}","","+44 202555${it.toString().padStart(4,'0')}","MOBILE",false,null)}
   val now=System.currentTimeMillis();st.recents=(1..20).map{Recent(it.toLong(),"+44 202555${it.toString().padStart(4,'0')}",when(it){1->"Railways";2->"Jio Balance Check Message";3->"Suman Mama";else->"Caller $it"},if(it==3)CallLog.Calls.MISSED_TYPE else if(it==4)CallLog.Calls.INCOMING_TYPE else CallLog.Calls.OUTGOING_TYPE,now-it*60000,if(it==3)0 else 70,null,null)}
  };repeat(4){settle()}
  println("HOME-STATE theme=${LdApp.settings.value.theme} bottom=${LdApp.settings.value.searchBottom} tab=${rule.activity.state.tab} bar=${bounds("top_bar")} viewport=${bounds("home_content_viewport")}")
 }
 private fun node(tag:String)=rule.onNodeWithTag(tag,true)
 private fun bounds(tag:String)=node(tag).fetchSemanticsNode().boundsInRoot
 private fun viewportClear(bottom:Boolean){val v=bounds("home_content_viewport");val control=bounds(if(bottom)"top_bar"else"bottom_bar");assertTrue("content must scroll behind a fading scrim",v.bottom>control.top);assertTrue(bounds("bottom_scrim").top<control.top)}
 private fun labelsFit(){for(i in 0..3){val results=mutableListOf<TextLayoutResult>();rule.mainClock.autoAdvance=true;node("recent_filter_$i").performScrollTo();settle();node("recent_filter_label_$i").performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(results)};assertTrue(results.isNotEmpty());assertFalse("filter $i must not ellipsize",results.any{r->(0 until r.lineCount).any{r.isLineEllipsized(it)}});node("recent_filter_$i").assertIsDisplayed()}}
 private fun lastRow(text:String,bottom:Boolean){val list=node("main_list");rule.mainClock.autoAdvance=true;list.performScrollToNode(hasText(text));settle();repeat(2){list.performTouchInput{swipeUp()};settle()};rule.onNodeWithText(text,useUnmergedTree=true).assertIsDisplayed();val row=rule.onNodeWithText(text,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot;assertTrue(row.bottom<=bounds(if(bottom)"top_bar"else"bottom_bar").top)}
 @Test fun darkMidScrollAndEnd(){setup("midnight",style="google");saveShot(rule,"EDGE395-dark-start");rule.mainClock.autoAdvance=true;node("main_list").performTouchInput{swipeUp(startY=centerY+160f,endY=centerY-115f,durationMillis=750)};settle();saveShot(rule,"EDGE395-dark-mid");viewportClear(false);lastRow("Caller 20",false);val row=rule.onNodeWithText("Caller 20",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot;assertTrue("last row clears fade",row.bottom<=bounds("bottom_scrim").top);saveShot(rule,"EDGE395-dark-end");assertNull(rule.activity.state.pendingCall)}
 @Test fun dayAndLargeBottomSearch(){setup("daylight",1.3f,true);saveShot(rule,"EDGE395-day-bottom");lastRow("Caller 20",true);saveShot(rule,"EDGE395-day-bottom-end");node("drawer_open").assertHasClickAction();node("search_mic").assertHasClickAction();assertNull(rule.activity.state.pendingCall)}
 @Test fun contactsDarkMidAndEnd(){setup("midnight",1.3f,contacts=true);node("main_list").performTouchInput{swipeUp(startY=centerY+140f,endY=centerY-75f,durationMillis=750)};settle();saveShot(rule,"EDGE395-contacts-mid");lastRow("Person 20",false);saveShot(rule,"EDGE395-contacts-end")}
 @Test fun pillFollowsFingerButPagesFollowContent(){setup("midnight");rule.mainClock.autoAdvance=true;node("bottom_bar").performTouchInput{swipeRight(durationMillis=750)};repeat(3){settle()};assertEquals("contacts",rule.activity.state.tab);assertTrue("indicator must follow selected slot: indicator=${bounds("nav_indicator")} contacts=${bounds("nav_contacts")}",bounds("nav_indicator").left>=bounds("nav_contacts").left-2f);rule.mainClock.autoAdvance=true;node("bottom_bar").performTouchInput{swipeLeft(durationMillis=750)};repeat(3){settle()};assertEquals("recents",rule.activity.state.tab);node("nav_contacts").performClick();repeat(3){settle()};assertEquals("contacts",rule.activity.state.tab);node("nav_recents").performClick();repeat(3){settle()};assertEquals("recents",rule.activity.state.tab);rule.mainClock.autoAdvance=true;node("home_content_viewport").performTouchInput{swipeLeft(durationMillis=750)};repeat(3){settle()};assertEquals("contacts",rule.activity.state.tab);rule.mainClock.autoAdvance=true;node("home_content_viewport").performTouchInput{swipeRight(durationMillis=750)};repeat(3){settle()};assertEquals("recents",rule.activity.state.tab);assertNull(rule.activity.state.pendingCall);saveShot(rule,"EDGE395-pill-final")}
}
