package com.mandu.liquiddialer
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
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
@OptIn(ExperimentalTestApi::class)
abstract class MessageDraftsUiFixture{
 @get:Rule(order=0) val initialSettings=object:org.junit.rules.ExternalResource(){override fun before(){
  // These workspace tests own a plain host. Do not launch MainActivity startup
  // loads, onboarding or animations before installing the test composition.
  org.robolectric.Shadows.shadowOf(ctx.packageManager).addActivityIfNotPresent(android.content.ComponentName(ctx,ComponentActivity::class.java))
  runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.motion]=false};ctx.repo.flow.first{it.onboarded&&!it.motion}}
 }}
 @get:Rule(order=1) val rule=createAndroidComposeRule<ComponentActivity>(effectContext=kotlinx.coroutines.test.StandardTestDispatcher())
 protected val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 protected val number="+919876543210"
 protected val draft=MessageDrafts.Draft("delivery","Delivery update","सामान तैयार है • Office rear entrance\nCollect 5pm & later?")
 private var focus:androidx.compose.ui.focus.FocusManager?=null
 protected fun ComponentActivity.mountTestPage(content:@Composable ()->Unit){val host=ComposeView(this);host.setContent{focus=androidx.compose.ui.platform.LocalFocusManager.current;content()};setContentView(host)}
 protected fun settle(){rule.runOnUiThread{androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 protected fun await(f:()->Boolean){rule.waitUntil(15000){settle();f()}}
 protected fun seed(night:Boolean=false){settle();File(ctx.filesDir,"message_drafts.json").delete();File(ctx.filesDir,"message_drafts.json.bak").delete();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=if(night)"midnight"else"daylight";it[K.fontScale]=if(night)1.4f else 1f;it[K.refinedHeaders]=true;it[K.messageDrafts]=false}};await{LdApp.settings.value.let{it.theme==(if(night)"midnight"else"daylight")&&it.fontScale==(if(night)1.4f else 1f)&&!it.messageDrafts}}}
 protected fun show(night:Boolean=false){seed(night);MessageDrafts.save(ctx,"",listOf(draft));rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{MessageDraftsWorkspace(number,{})}}};settle()}
 protected fun choose(){click("draft_choose");await{rule.onAllNodesWithTag("draft_use_delivery",true).fetchSemanticsNodes().isNotEmpty()};click("draft_use_delivery")}
 protected fun field(tag:String)=rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),true)
 protected fun click(tag:String){rule.mainClock.autoAdvance=true;rule.onNodeWithTag(tag,true).performScrollTo();rule.waitForIdle();rule.mainClock.autoAdvance=false;rule.onNodeWithTag(tag,true).performClick();settle()}
 @After fun clean(){rule.runOnUiThread{focus?.clearFocus(force=true);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()};rule.mainClock.autoAdvance=false;rule.mainClock.advanceTimeBy(1000);rule.runOnUiThread{(rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)).removeAllViews()};runBlocking{ctx.ldStore.edit{it[K.fontScale]=1f;it[K.refinedHeaders]=false;it[K.messageDrafts]=false}}}
}


@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class MessageDraftsUiTest:MessageDraftsUiFixture(){
 @Test fun openingHasExactRecipientNoAutoCopyOrIntent(){show();val cb=ctx.getSystemService(ClipboardManager::class.java);cb.setPrimaryClip(android.content.ClipData.newPlainText("Before","unchanged"));rule.onNodeWithTag("draft_recipient",true).assertTextEquals(number);choose();assertEquals("unchanged",cb.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity);assertEquals(draft,MessageDrafts.read(ctx).rows.single());saveShot(rule,"DRAFT416-day-preview")}
 @Test fun explicitCopyAndSmsComposeUseEditedPreview(){show();choose();field("draft_preview").performTextReplacement("Changed & safe + line\nनमस्ते");settle();click("draft_copy");assertEquals("Changed & safe + line\nनमस्ते",ctx.getSystemService(ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString());click("draft_sms");val i=org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity;assertEquals(android.content.Intent.ACTION_SENDTO,i.action);assertEquals(number,i.data!!.schemeSpecificPart);assertEquals("Changed & safe + line\nनमस्ते",i.getStringExtra("sms_body"));assertEquals(draft,MessageDrafts.read(ctx).rows.single())}
 @Test fun whatsappComposeFullInternationalRecipientAndUnicode(){show();choose();click("draft_whatsapp");val i=org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity;assertEquals(android.content.Intent.ACTION_VIEW,i.action);assertEquals("/919876543210",i.data!!.path);assertEquals(draft.body,i.data!!.getQueryParameter("text"))}
 @Test fun replaceNeedsConfirmationAndPreviewBackNeedsDiscard(){show();field("draft_preview").performTextInput("My text");choose();field("draft_preview").assertTextContains("My text");click("draft_replace");field("draft_preview").assertTextContains(draft.body);rule.runOnUiThread{rule.activity.onBackPressedDispatcher.onBackPressed()};settle();rule.onNodeWithTag("draft_discard",true).assertExists();assertEquals(draft,MessageDrafts.read(ctx).rows.single())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class MessageDraftsLibraryUiTest:MessageDraftsUiFixture(){
 @Test fun addSaveEditAndDeleteConfirmation(){show();click("draft_choose");await{rule.onAllNodesWithTag("draft_add",true).fetchSemanticsNodes().isNotEmpty()};click("draft_add");field("draft_title").performTextInput("Office hours");field("draft_body").performTextInput("Weekdays 9-5");click("draft_save");await{MessageDrafts.read(ctx).rows.size==2};click("draft_delete_delivery");assertEquals(2,MessageDrafts.read(ctx).rows.size);click("draft_delete_confirm");await{MessageDrafts.read(ctx).rows.size==1};assertEquals("Office hours",MessageDrafts.read(ctx).rows.single().title);saveShot(rule,"DRAFT416-library")}
 @Test fun editDiscardPreservesAndStaleSaveFails(){show();click("draft_choose");await{rule.onAllNodesWithTag("draft_edit_delivery",true).fetchSemanticsNodes().isNotEmpty()};click("draft_edit_delivery");field("draft_body").performTextReplacement("Unsaved");click("draft_library_back");click("draft_edit_discard");assertEquals(draft,MessageDrafts.read(ctx).rows.single());click("draft_edit_delivery");val old=MessageDrafts.read(ctx);MessageDrafts.save(ctx,old.raw,listOf(draft.copy(body="New elsewhere")));field("draft_body").performTextReplacement("Stale");click("draft_save");await{rule.onAllNodesWithTag("draft_library_error",true).fetchSemanticsNodes().isNotEmpty()};assertEquals("New elsewhere",MessageDrafts.read(ctx).rows.single().body)}
 @Test fun corruptStorePreservedNoAddOrDelete(){seed();File(ctx.filesDir,"message_drafts.json").writeText("broken");rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{MessageDraftsWorkspace(number,{})}}};settle();click("draft_choose");await{rule.onAllNodesWithTag("draft_library_error",true).fetchSemanticsNodes().isNotEmpty()};rule.onAllNodesWithTag("draft_add",true).assertCountEquals(0);assertEquals("broken",File(ctx.filesDir,"message_drafts.json").readText())}
 @Test fun nightLargePreviewAndLibraryPixels(){show(true);choose();saveShot(rule,"DRAFT416-night-large-preview");click("draft_choose");await{rule.onAllNodesWithTag("draft_edit_delivery",true).fetchSemanticsNodes().isNotEmpty()};click("draft_edit_delivery");saveShot(rule,"DRAFT416-night-large-editor")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class MessageDraftsEntryUiTest:MessageDraftsUiFixture(){
 @Test fun callNotesEntryDefaultOffAndOptIn(){seed();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{Sheet({}){NotesSheet(number,"Support",{})}}}};settle();rule.onAllNodesWithTag("call_message_drafts",true).assertCountEquals(0);runBlocking{ctx.repo.set(K.messageDrafts,true)};await{LdApp.settings.value.messageDrafts};click("call_message_drafts");rule.onNodeWithTag("draft_recipient",true).assertTextEquals(number);saveShot(rule,"DRAFT416-call-notes")}
 @Test fun headerBackPreservesDirtyLibraryEdit(){show();choose();click("draft_choose");await{rule.onAllNodesWithTag("draft_edit_delivery",true).fetchSemanticsNodes().isNotEmpty()};click("draft_edit_delivery");field("draft_body").performTextReplacement("Unsaved via header");rule.onNodeWithTag("page_back",true).performClick();settle();rule.onNodeWithTag("draft_edit_discard",true).assertExists();assertEquals(draft,MessageDrafts.read(ctx).rows.single())}
 @Test fun missingComposeAppKeepsPreview(){seed();var launches=0;rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{MessageDraftsWorkspace(number,{},onCompose={launches++;throw android.content.ActivityNotFoundException()})}}};settle();assertEquals(0,launches);field("draft_preview").performTextInput("Keep this message");click("draft_sms");assertEquals(1,launches);field("draft_preview").assertTextContains("Keep this message");rule.onNodeWithTag("draft_compose_error",true).assertExists()}
 @Test fun settingsOverviewPixels(){seed();rule.runOnUiThread{rule.activity.mountTestPage{LdThemeHost{MessageDraftsPage({})}}};settle();saveShot(rule,"DRAFT416-overview");assertFalse(LdApp.settings.value.messageDrafts)}
}
