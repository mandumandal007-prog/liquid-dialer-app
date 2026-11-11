package com.mandu.liquiddialer
import android.telecom.Call
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import com.mandu.liquiddialer.telecom.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class IvrMenusUiTest{
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(900);rule.waitForIdle()}
 private fun await(t:()->Boolean){rule.waitUntil(15000){settle();t()}}
 private val sample get()=IvrMenus.encode(listOf(IvrMenus.Menu("service","Service choices","12#")))
 private fun show(night:Boolean=false,raw:String=""){
  settle();ctx.getSharedPreferences("ivr_menus",0).edit().putString("data",raw).commit()
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.refinedHeaders]=true;it[K.ivrHelper]=false}}
  await{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.ivrHelper}}
  rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{IvrMenusPage({})}}};settle()
 }
 private fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(tag,true).performClick();settle()}
 private fun field(i:Int,text:String){rule.onAllNodes(hasSetTextAction())[i].performTextReplacement(text);settle()}
 @After fun clean(){ctx.getSharedPreferences("ivr_menus",0).edit().clear().commit();CallManager.calls.value=emptyList();runBlocking{ctx.ldStore.edit{it[K.ivrHelper]=false;it[K.fontScale]=1f;it[K.refinedHeaders]=false}}}
 @Test fun addDraftAndSaveNoHelperEnable(){show();click("ivr_add");field(0,"Shop support");field(1,"12#");assertEquals("",IvrMenus.raw(ctx));click("ivr_save");assertEquals("12#",IvrMenus.decode(IvrMenus.raw(ctx)).single().digits);assertFalse(LdApp.settings.value.ivrHelper)}
 @Test fun invalidSequenceNotSaved(){show();click("ivr_add");field(0,"Support");field(1,"12;3");click("ivr_save");rule.onNodeWithTag("ivr_error",true).assertExists();assertEquals("",IvrMenus.raw(ctx))}
 @Test fun cancelEditDoesNotWrite(){show(raw=sample);click("ivr_edit_service");field(0,"Changed");click("ivr_cancel");assertEquals(sample,IvrMenus.raw(ctx));rule.onNodeWithText("Service choices").assertExists()}
 @Test fun deleteRequiresExplicitConfirm(){show(raw=sample);click("ivr_delete_service");assertEquals(sample,IvrMenus.raw(ctx));click("ivr_delete_yes");assertTrue(IvrMenus.decode(IvrMenus.raw(ctx)).isEmpty())}
 @Test fun staleEditorCannotOverwrite(){show(raw=sample);click("ivr_edit_service");field(0,"Changed");IvrMenus.save(ctx,sample,emptyList());click("ivr_save");rule.onNodeWithTag("ivr_error",true).assertExists();assertTrue(IvrMenus.decode(IvrMenus.raw(ctx)).isEmpty())}
 @Test fun corruptDataPreserved(){show(raw="broken");rule.onNodeWithTag("ivr_corrupt",true).assertExists();rule.onNodeWithTag("ivr_add",true).assertDoesNotExist();assertEquals("broken",IvrMenus.raw(ctx))}
 @Test fun dayManagerPixels(){show(raw=sample);saveShot(rule,"IVR407-day")}
 @Test fun nightLargeEditorPixels(){show(true,sample);click("ivr_edit_service");saveShot(rule,"IVR407-editor-night-large")}
 @Test fun selectionAndResetDoNotSend(){show(raw=sample);runBlocking{ctx.repo.set(K.ivrHelper,true)};await{LdApp.settings.value.ivrHelper};val target=FakeCall.make("+919876543210","Support",Call.STATE_ACTIVE);FakeCall.setChildren(target,emptyList());CallManager.calls.value=listOf(target);rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Sheet({}){IvrCallSheet(target,{})}}}};settle();click("ivr_select_service");rule.onNodeWithTag("ivr_next",true).assertTextEquals("Next: 1");click("ivr_reset");rule.onNodeWithTag("ivr_next",true).assertTextEquals("Next: 1");saveShot(rule,"IVR407-call-board")}
 @Test fun changedCallCannotRetargetSelectedMenu(){show(raw=sample);runBlocking{ctx.repo.set(K.ivrHelper,true)};await{LdApp.settings.value.ivrHelper};val target=FakeCall.make("+919876543210","Support",Call.STATE_ACTIVE);FakeCall.setChildren(target,emptyList());CallManager.calls.value=listOf(target);rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Sheet({}){IvrCallSheet(target,{})}}}};settle();click("ivr_select_service");CallManager.calls.value=listOf(FakeCall.make("+919999999999","Other",Call.STATE_ACTIVE));settle();rule.onNodeWithTag("ivr_unavailable",true).assertExists();rule.onNodeWithTag("ivr_send",true).assertDoesNotExist();rule.onNodeWithTag("ivr_next",true).assertTextEquals("Next: 1")}
 @Test fun actualCallKeypadHelperPixels(){show(raw=sample);runBlocking{ctx.repo.set(K.ivrHelper,true)};await{LdApp.settings.value.ivrHelper};val target=FakeCall.make("+919876543210","Support",Call.STATE_ACTIVE);FakeCall.setChildren(target,emptyList());CallManager.calls.value=listOf(target);rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallScreen({},{},{},{})}}};settle();rule.onNodeWithTag("call_button_Keypad",true).performClick();settle();rule.onNodeWithTag("call_ivr_open",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"IVR407-keypad");rule.onNodeWithTag("call_ivr_open",true).performClick();settle();click("ivr_select_service");rule.onNodeWithTag("ivr_next",true).assertTextEquals("Next: 1");saveShot(rule,"IVR407-actual-call-board")}
}
