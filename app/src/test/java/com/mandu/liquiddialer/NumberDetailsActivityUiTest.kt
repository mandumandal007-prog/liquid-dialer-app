package com.mandu.liquiddialer
import android.Manifest
import android.content.*
import android.database.*
import android.net.Uri
import android.provider.CallLog
import androidx.compose.ui.test.*
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
abstract class NumberDetailsActivityFixture:NumberDetailsVideoFixture(){
 protected var deletes=0;protected var queries=0
 protected val historyNumber="+919876543210"
 protected val records=listOf(
  Recent(1,historyNumber,"Rahul Sharma",CallLog.Calls.OUTGOING_TYPE,1794944400000L,3661,null,null),
  Recent(2,historyNumber,"Rahul Sharma",CallLog.Calls.MISSED_TYPE,1794940800000L,0,null,null),
  Recent(3,historyNumber,"Rahul Sharma",CallLog.Calls.INCOMING_TYPE,1794937200000L,125,null,null))
 protected fun provider(items:List<Recent>){Shadows.shadowOf(ctx).grantPermissions(Manifest.permission.READ_CALL_LOG);org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY,object:ContentProvider(){
  override fun onCreate()=true
  override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor{queries++;return MatrixCursor(p).apply{items.forEach{r->addRow(arrayOf(r.id,r.number,r.name,r.type,r.date,r.duration,r.accountId,r.photo))}}}
  override fun insert(u:Uri,v:ContentValues?):Uri?=null
  override fun delete(u:Uri,s:String?,a:Array<out String>?):Int{deletes++;return 0}
  override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0
  override fun getType(u:Uri):String?=null
 })}
 protected fun activity(on:Boolean=true,night:Boolean=false,large:Boolean=false,glass:Boolean=true,items:List<Recent> = records){provider(items);runBlocking{ctx.repo.set(K.numberDetailsActivity,on)};await{LdApp.settings.value.numberDetailsActivity==on};show(glass=glass,night=night,large=large);await{rule.onAllNodesWithText("Recent activity",true,true).fetchSemanticsNodes().size==if(items.any{Repo.key(it.number)==Repo.key(historyNumber)})1 else 0}}
 protected fun activityOption(on:Boolean){runBlocking{ctx.repo.set(K.numberDetailsActivity,on)};await{LdApp.settings.value.numberDetailsActivity==on&&rule.onAllNodesWithTag("details_activity_row_0",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun metadata(r:Recent)=java.text.SimpleDateFormat("d MMM, h:mm a",java.util.Locale.getDefault()).format(java.util.Date(r.date))+if(r.duration>0)"  ·  "+fmtDur(r.duration)else""
 @After fun cleanActivity(){runBlocking{ctx.repo.set(K.numberDetailsActivity,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsActivityLayoutUiTest:NumberDetailsActivityFixture(){
 @Test fun offRetainsOriginalRows(){activity(false);rule.onNodeWithTag("details_activity_row_0",true).assertDoesNotExist();rule.onNodeWithText("Outgoing",true,true).assertExists();rule.onNodeWithText(metadata(records[0]),true,true).assertExists();assertEquals(0,deletes)}
 @Test fun separateTypeAndMetaRetainExactDataOrder(){activity();for((i,r)in records.withIndex()){rule.onNodeWithTag("details_activity_type_$i",true).assertTextEquals(typeLabel(r.type));rule.onNodeWithTag("details_activity_meta_$i",true).assertTextEquals(metadata(r));val type=rule.onNodeWithTag("details_activity_type_$i",true).getUnclippedBoundsInRoot();val meta=rule.onNodeWithTag("details_activity_meta_$i",true).getUnclippedBoundsInRoot();assertTrue(type.bottom<meta.top)};assertEquals(2,rule.onAllNodes(hasTestTag("details_activity_divider_0")or hasTestTag("details_activity_divider_1"),true).fetchSemanticsNodes().size);assertEquals(0,deletes)}
 @Test fun missingHistoryHasNoPanelOrDelete(){activity(items=emptyList());rule.onNodeWithTag("details_activity_row_0",true).assertDoesNotExist();rule.onNodeWithTag("details_history_delete",true).assertDoesNotExist();rule.onNodeWithTag("details_video",true).assertExists()}
 @Test fun filtersOtherNumberAndCapsFifteenRows(){val items=(0..16).map{records[0].copy(id=it.toLong())}+records[0].copy(id=100,number="+449876543210");activity(items=items);rule.onNodeWithTag("details_activity_row_14",true).assertExists();rule.onNodeWithTag("details_activity_row_15",true).assertDoesNotExist();assertEquals(0,deletes)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsActivitySafetyUiTest:NumberDetailsActivityFixture(){
 @Test fun togglesNoQueryDeleteCallOrClose(){activity();val n=queries;activityOption(false);activityOption(true);assertEquals(n,queries);assertEquals(0,deletes);assertEquals(0,closed);assertNull(rule.activity.state.pendingCall);rule.onNodeWithTag("details_activity_meta_0",true).assertTextEquals(metadata(records[0]))}
 @Test fun glassOffStillHasActivityLayoutAndVideo(){activity(glass=false);rule.onNodeWithTag("details_glass_header",true).assertDoesNotExist();rule.onNodeWithTag("details_activity_row_0",true).assertExists();rule.onNodeWithTag("details_video",true).assertExists();simChoice();hit("details_video");assertEquals(selected to true,rule.activity.state.pendingCall)}
 @Test fun zeroDurationNoFakeTalkTime(){activity();rule.onNodeWithTag("details_activity_meta_1",true).assertTextEquals(metadata(records[1]));assertFalse(metadata(records[1]).contains("·"));assertEquals(0,deletes)}
 @Test fun defaultOffFinderRoute(){assertFalse(LdSettings().numberDetailsActivity);assertNotEquals(K.numberDetailsActivity,K.numberDetailsGlass);assertEquals("settings:display",SettingsFinder.all.single{it.id=="number-details-activity"}.route)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsActivityDayPixelsUiTest:NumberDetailsActivityFixture(){
 @Test fun dayHeaderAndActivity(){activity();saveShot(rule,"DETAILS492-day-header");move("details_activity_row_2");rule.onNodeWithTag("details_history_delete",true).assertIsDisplayed();saveShot(rule,"DETAILS492-day-activity");assertEquals(0,deletes)}
 @Test fun settingsOffThenPersistOn(){show();runBlocking{ctx.repo.set(K.numberDetailsActivity,false)};await{!LdApp.settings.value.numberDetailsActivity};mount{Page("settings:display",rule.activity.state,rule.activity,{},{},null,"",{})};rule.mainClock.autoAdvance=true;rule.onNodeWithText("Number details activity layout",true).performScrollTo();settle();saveShot(rule,"DETAILS492-settings-off");rule.onNodeWithText("Number details activity layout",true).performClick();await{LdApp.settings.value.numberDetailsActivity}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberDetailsActivityNightPixelsUiTest:NumberDetailsActivityFixture(){
 @Test fun nightLargeActivityFits(){activity(night=true,large=true);move("details_activity_row_2");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("page_content_scroll",true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,180f)};settle();for(i in 0..2){rule.onNodeWithTag("details_activity_type_$i",true).assertIsDisplayed();rule.onNodeWithTag("details_activity_meta_$i",true).assertIsDisplayed()};saveShot(rule,"DETAILS492-night-large-activity");assertNull(rule.activity.state.pendingCall);assertEquals(0,deletes)}
 @Test fun originalIdentityActivityStillFits(){activity(night=true,large=true,glass=false);move("details_activity_row_2");rule.mainClock.autoAdvance=true;rule.onNodeWithTag("page_content_scroll",true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,180f)};settle();rule.onNodeWithTag("details_activity_type_0",true).assertIsDisplayed();saveShot(rule,"DETAILS492-night-original-activity");assertEquals(0,deletes)}
}
