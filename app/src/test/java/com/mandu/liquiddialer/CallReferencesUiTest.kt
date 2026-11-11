package com.mandu.liquiddialer
import android.content.ClipboardManager
import android.content.Context
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
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class CallReferencesUiTest{
 @get:Rule(order=0) val initialSettings=object:org.junit.rules.ExternalResource(){override fun before(){
  // Seed before MainActivity launches, so onboarding/background animation cannot
  // become the scene under an isolated workspace test.
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false};ctx.repo.flow.first{it.onboarded&&!it.motion}}
 }}
 @get:Rule(order=1) val rule=createAndroidComposeRule<MainActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+919876543210"
 private val c=CallReferences.Card("order","Delivery reference","ORDER-204 • two cartons\nShop rear entrance")
 private fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 private fun seed(night:Boolean=false){settle();File(ctx.filesDir,"call_references.json").delete();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.refinedHeaders]=true;it[K.callReferences]=false}};await{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.callReferences}}}
 private fun show(night:Boolean=false,empty:Boolean=false){seed(night);if(!empty)CallReferences.save(ctx,number,"",listOf(c));rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallReferencesWorkspace(number,{})}}};await{rule.onAllNodesWithTag("ref_search",true).fetchSemanticsNodes().isNotEmpty()}}
 private fun field(tag:String)=rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true)
 private fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(tag,true).performClick();settle()}
 @After fun clean(){runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.refinedHeaders]=false;it[K.callReferences]=false}}}
 @Test fun openingHiddenReadOnly(){show();val before=CallReferences.read(ctx,number);rule.onNodeWithTag("ref_value_order",true).assertTextEquals("Value hidden");rule.onAllNodesWithText(c.value,true).assertCountEquals(0);assertEquals(before,CallReferences.read(ctx,number));assertFalse(LdApp.settings.value.callReferences)}
 @Test fun explicitRevealHideAndCopyOnly(){show();val clipboard=ctx.getSystemService(Context.CLIPBOARD_SERVICE)as ClipboardManager;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Before","unchanged"));click("ref_reveal_order");saveShot(rule,"REF410-debug-reveal");rule.onNodeWithTag("ref_value_order",true).assertTextEquals(c.value);assertEquals("unchanged",clipboard.primaryClip!!.getItemAt(0).text.toString());click("ref_copy_order");assertEquals(c.value,clipboard.primaryClip!!.getItemAt(0).text.toString());click("ref_reveal_order");rule.onNodeWithTag("ref_value_order",true).assertTextEquals("Value hidden");rule.onAllNodesWithTag("ref_copy_order",true).assertCountEquals(0)}
 @Test fun searchTitlesHidesAndDoesNotSearchValues(){show();click("ref_reveal_order");field("ref_search").performTextInput("204");settle();rule.onAllNodesWithTag("ref_card_order",true).assertCountEquals(0);field("ref_search").performTextClearance();settle();rule.onNodeWithTag("ref_value_order",true).assertTextEquals("Value hidden")}
 @Test fun draftCancelDoesNotWrite(){show();click("ref_edit_order");field("ref_title").performTextReplacement("Changed");click("ref_cancel");assertEquals(c,CallReferences.read(ctx,number).cards.single())}
 @Test fun addSaveReturnsHidden(){show(empty=true);click("ref_add");field("ref_title").performTextInput("Office hours");field("ref_value_input").performTextInput("Weekdays 9-5");click("ref_save");val saved=CallReferences.read(ctx,number).cards.single();assertEquals("Office hours",saved.title);rule.onNodeWithTag("ref_value_${saved.id}",true).assertTextEquals("Value hidden");saveShot(rule,"REF410-saved")}
 @Test fun deletionRequiresConfirmation(){show();click("ref_delete_order");assertEquals(listOf(c),CallReferences.read(ctx,number).cards);click("ref_delete_no");assertEquals(listOf(c),CallReferences.read(ctx,number).cards);click("ref_delete_order");click("ref_delete_yes");assertTrue(CallReferences.read(ctx,number).cards.isEmpty())}
 @Test fun staleDraftCannotOverwriteNewerCard(){show();click("ref_edit_order");val old=CallReferences.read(ctx,number);CallReferences.save(ctx,number,old.raw,listOf(c.copy(value="Updated elsewhere")));field("ref_value_input").performTextReplacement("Old draft edit");click("ref_save");assertEquals("Updated elsewhere",CallReferences.read(ctx,number).cards.single().value);rule.onNodeWithTag("ref_error",true).assertExists();rule.onNodeWithTag("ref_value_input",true).assertExists()}
 @Test fun corruptPreservedAndNoAdd(){seed();File(ctx.filesDir,"call_references.json").writeText("broken");rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallReferencesWorkspace(number,{})}}};await{rule.onAllNodesWithTag("ref_corrupt",true).fetchSemanticsNodes().isNotEmpty()};rule.onAllNodesWithTag("ref_add",true).assertCountEquals(0);assertEquals("broken",File(ctx.filesDir,"call_references.json").readText())}
 @Test fun settingsEntryAndCallToggleGate(){seed();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Sheet({}){NotesSheet(number,"Support",{})}}}};settle();rule.onAllNodesWithTag("call_references_open",true).assertCountEquals(0);runBlocking{ctx.repo.set(K.callReferences,true)};await{LdApp.settings.value.callReferences};click("call_references_open");rule.onNodeWithTag("ref_identity",true).assertTextEquals(number)}
 @Test fun dayHiddenAndRevealedPixels(){show();saveShot(rule,"REF410-day-hidden");click("ref_reveal_order");saveShot(rule,"REF410-day-revealed")}
 @Test fun nightLargeAndDraftPixels(){show(true);saveShot(rule,"REF410-night-large");click("ref_edit_order");saveShot(rule,"REF410-draft-large")}
 @Test fun settingsOverviewPixels(){seed();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallReferencesPage({})}}};settle();saveShot(rule,"REF410-overview");assertFalse(LdApp.settings.value.callReferences)}
 @Test fun backgroundHidesRevealedValue(){show();click("ref_reveal_order");rule.onNodeWithTag("ref_value_order",true).assertTextEquals(c.value);rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);settle();rule.onNodeWithTag("ref_value_order",true).assertTextEquals("Value hidden")}
 @Test fun callNotesSheetPixels(){show();runBlocking{ctx.repo.set(K.callReferences,true)};await{LdApp.settings.value.callReferences};rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Sheet({}){NotesSheet(number,"Support",{})}}}};settle();click("call_references_open");await{rule.onAllNodesWithTag("ref_value_order",true).fetchSemanticsNodes().isNotEmpty()};saveShot(rule,"REF410-call-sheet");rule.onNodeWithTag("ref_identity",true).assertTextEquals(number);rule.onNodeWithTag("ref_value_order",true).assertTextEquals("Value hidden")}
}
