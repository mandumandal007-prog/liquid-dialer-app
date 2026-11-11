package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallListsUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun reveal(tag:String,unmerged:Boolean=false):SemanticsNodeInteraction {
  repeat(20) {
   val node=rule.onNodeWithTag(tag,useUnmergedTree=unmerged)
   if(node.isDisplayed())return node
   val top=node.fetchSemanticsNode().boundsInRoot.top
   rule.onRoot().performTouchInput {if(top<0)swipeDown(durationMillis=120) else swipeUp(durationMillis=120)};settle()
  };error("Not visible: $tag")
 }
 private fun click(tag:String){reveal(tag).performClick();settle()}
 private fun awaitData(check:()->Boolean){rule.waitUntil(15000){settle();check()};settle()}
 @Before fun clean(){listOf("",".bak",".new").forEach {File(ctx.filesDir,"manual_call_lists.json$it").delete()}}
 private fun show(theme:String="daylight",font:Float=1f){
  rule.mainClock.autoAdvance=false;settle()
  runBlocking{ctx.ldStore.edit {it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.confirmCall]=true}}
  rule.waitUntil(15000){LdApp.settings.value.theme==theme && LdApp.settings.value.fontScale==font && LdApp.settings.value.confirmCall}
  rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallListsPage(rule.activity.state,rule.activity,{})}}};settle()
  // Background file read publishes into a frozen Compose clock. Pump composition while waiting.
  rule.waitUntil(15000){settle();rule.onAllNodesWithTag("lists_create").fetchSemanticsNodes().isNotEmpty()};settle()
 }
 @Test fun createAddCallManuallyDoneResumeRenameAndDelete() {
  show();click("lists_create");rule.onNodeWithTag("list_title").performTextInput("Family");click("list_save")
  awaitData{CallLists.read(ctx).size==1};val id=CallLists.read(ctx).single().id
  click("list_add");rule.onNodeWithTag("list_number").performTextInput("+91 98765 43210");rule.onNodeWithTag("entry_name").performTextInput("Papa");click("entry_save")
  awaitData{CallLists.read(ctx).single().entries.size==1};val e=CallLists.read(ctx).single().entries.single()
  assertNull(rule.activity.state.confirmNumber)
  click("list_call_${e.id}");assertEquals(e.number,rule.activity.state.confirmNumber);assertFalse(CallLists.read(ctx).single().entries.single().done)
  click("list_done_${e.id}");awaitData{CallLists.read(ctx).single().entries.single().done}
  click("list_filter_Done");reveal("list_entry_${e.id}",true).assertIsDisplayed()
  click("list_done_${e.id}");awaitData{!CallLists.read(ctx).single().entries.single().done}
  click("list_filter_Pending")
  rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallListsPage(rule.activity.state,rule.activity,{})}}};settle()
  rule.waitUntil(15000){settle();rule.onAllNodesWithTag("list_open_$id").fetchSemanticsNodes().isNotEmpty()};click("list_open_$id")
  click("list_rename");rule.onNodeWithTag("list_title").performTextReplacement("Weekend calls");click("list_save");awaitData{CallLists.read(ctx).single().title=="Weekend calls"}
  click("list_remove_${e.id}");rule.onNodeWithTag("list_cancel_delete").performClick();settle();assertEquals(1,CallLists.read(ctx).single().entries.size)
  click("list_delete");rule.onNodeWithTag("list_cancel_delete").performClick();settle();assertEquals(1,CallLists.read(ctx).size)
  click("list_delete");rule.onNodeWithTag("list_confirm_delete").performClick();awaitData{CallLists.read(ctx).isEmpty()}
 }
 @Test fun contactSearchAddsExactNumberWithoutCallAndDuplicateIsDisabled() {
  val id=CallLists.create(ctx,"Work").single().id
  org.robolectric.Shadows.shadowOf(ctx).grantPermissions(android.Manifest.permission.READ_CONTACTS)
  rule.runOnUiThread{rule.activity.state.contacts=listOf(Contact(1,"","Office","","+44 2125551234","",false,null),Contact(2,"","Other","","+1 2125551234","",false,null))}
  show();click("list_open_$id");click("list_contacts");rule.onNodeWithTag("list_contact_search").performTextInput("Office");settle()
  rule.onNodeWithTag("list_contact_2").assertDoesNotExist();click("list_contact_1");awaitData{CallLists.read(ctx).single().entries.size==1}
  assertEquals("+44 2125551234",CallLists.read(ctx).single().entries.single().number);assertNull(rule.activity.state.confirmNumber)
  rule.onNodeWithTag("list_contact_1").assertIsNotEnabled()
 }
 @Test fun invalidNumberAndMissingContactsPermissionKeepManualPath() {
  val id=CallLists.create(ctx,"Manual").single().id
  org.robolectric.Shadows.shadowOf(ctx).denyPermissions(android.Manifest.permission.READ_CONTACTS)
  show();click("list_open_$id");click("list_contacts")
  rule.onNodeWithText("Contacts permission nahi hai. Number jodo se manually add kar sakte ho.",useUnmergedTree=true).assertIsDisplayed()
  click("list_form_close");click("list_add")
  rule.onNodeWithTag("list_number").performTextInput("*123#");click("entry_save")
  rule.waitUntil(15000){settle();rule.onAllNodesWithTag("lists_error",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()};settle()
  assertTrue(CallLists.read(ctx).single().entries.isEmpty());assertNull(rule.activity.state.confirmNumber)
  rule.onNodeWithTag("list_number").performTextReplacement("9876543210");click("entry_save")
  awaitData{CallLists.read(ctx).single().entries.size==1}
 }
 @Test fun corruptStorageOffersNoMutationsOrCalls() {
  File(ctx.filesDir,"manual_call_lists.json").writeText("broken")
  rule.mainClock.autoAdvance=false;settle()
  rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{CallListsPage(rule.activity.state,rule.activity,{})}}};settle()
  rule.waitUntil(15000){settle();rule.onAllNodesWithTag("lists_error",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()};settle()
  rule.onNodeWithTag("lists_create").assertDoesNotExist();assertNull(rule.activity.state.confirmNumber)
  assertEquals("broken",File(ctx.filesDir,"manual_call_lists.json").readText())
 }
 @Test fun dayListPixels()=pixels("daylight",1f,"day")
 @Test fun largeNightListPixels()=pixels("midnight",1.3f,"night-large")
 private fun pixels(theme:String,font:Float,suffix:String){
  val id=CallLists.create(ctx,"Family calls").single().id
  CallLists.add(ctx,id,"+91 98765 43210","Papa");CallLists.add(ctx,id,"+91 91234 56780","Mummy")
  show(theme,font);saveShot(rule,"269-lists-$suffix")
  click("list_open_$id");val e=CallLists.read(ctx).single().entries.first()
  reveal("list_call_${e.id}").assertIsDisplayed();rule.onNodeWithTag("list_done_${e.id}").assertIsDisplayed()
  saveShot(rule,"269-list-detail-$suffix")
  click("list_contacts");saveShot(rule,"269-list-contacts-$suffix")
 }
}
