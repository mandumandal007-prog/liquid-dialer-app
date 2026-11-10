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
class HomePolishUiTest {
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
 private fun viewportClear(bottom:Boolean){val v=bounds("home_content_viewport");val control=bounds(if(bottom)"top_bar"else"bottom_bar");assertTrue("list viewport must end above controls",v.bottom<=control.top)}
 private fun labelsFit(){for(i in 0..3){val results=mutableListOf<TextLayoutResult>();rule.mainClock.autoAdvance=true;node("recent_filter_$i").performScrollTo();settle();node("recent_filter_label_$i").performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(results)};assertTrue(results.isNotEmpty());assertFalse("filter $i must not ellipsize",results.any{r->(0 until r.lineCount).any{r.isLineEllipsized(it)}});node("recent_filter_$i").assertIsDisplayed()}}
 private fun lastRow(text:String,bottom:Boolean){val list=node("main_list");repeat(9){list.performTouchInput{swipeUp()};settle()};rule.onNodeWithText(text,useUnmergedTree=true).assertIsDisplayed();val row=rule.onNodeWithText(text,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot;assertTrue(row.bottom<=bounds(if(bottom)"top_bar"else"bottom_bar").top)}
 @Test fun recentsDayFitsFiltersAndLastCallAboveNav(){setup();labelsFit();viewportClear(false);saveShot(rule,"HOME383-recents-day");lastRow("Caller 20",false);saveShot(rule,"HOME383-recents-last");assertNull(rule.activity.state.pendingCall)}
 @Test fun nightLargeFiltersKeepLabelsAndTypes(){setup("midnight",1.5f);labelsFit();viewportClear(false);saveShot(rule,"HOME383-recents-night-large");rule.mainClock.autoAdvance=true;node("recent_filter_1").performScrollTo().performClick();settle();rule.onNodeWithText("Suman Mama",useUnmergedTree=true).assertIsDisplayed();rule.onNodeWithText("Railways",useUnmergedTree=true).assertDoesNotExist();rule.mainClock.autoAdvance=true;node("recent_filter_3").performScrollTo().performClick();settle();rule.onNodeWithText("Caller 4",useUnmergedTree=true).assertIsDisplayed();assertNull(rule.activity.state.pendingCall)}
 @Test fun bottomSearchViewportAndLastRowStayClear(){setup(bottom=true);viewportClear(true);labelsFit();saveShot(rule,"HOME383-bottom-search");lastRow("Caller 20",true);saveShot(rule,"HOME383-bottom-last")}
 @Test fun contactsDayRetainsHeaderAndDetailsWithoutCall(){setup(contacts=true);viewportClear(false);node("contacts_add").assertIsDisplayed();saveShot(rule,"HOME383-contacts-day");rule.onNodeWithText("Person 01",useUnmergedTree=true).performClick();settle();rule.onNodeWithText("Number details",useUnmergedTree=true).assertIsDisplayed();assertNull(rule.activity.state.pendingCall)}
 @Test fun contactsNightLargeLastRowStaysClear(){setup("midnight",1.5f,contacts=true);viewportClear(false);saveShot(rule,"HOME383-contacts-night-large");lastRow("Person 20",false);saveShot(rule,"HOME383-contacts-last")}
 @Test fun flatStyleMatchesPhoneLayoutAndRetainsActions(){setup(style="google");val first=bounds("recent_filter_0");val strip=bounds("recent_filters");assertTrue(first.left>=strip.left);saveShot(rule,"AP387-recents-initial");labelsFit();viewportClear(false);saveShot(rule,"HOME383-recents-flat-phone");rule.onAllNodesWithTag("video_mini",true).onFirst().assertHasClickAction();rule.onAllNodesWithTag("call_mini",true).onFirst().assertHasClickAction();lastRow("Caller 20",false)}
 @Test fun contactsBottomSearchAndLastRowStayClear(){setup(bottom=true,contacts=true);viewportClear(true);lastRow("Person 20",true);saveShot(rule,"HOME383-contacts-bottom-last");assertNull(rule.activity.state.pendingCall)}
}
