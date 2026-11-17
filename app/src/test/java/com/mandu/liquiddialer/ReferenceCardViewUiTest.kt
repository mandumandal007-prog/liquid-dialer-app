package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class ReferenceCardViewFixture:ConverterOptionsFixture(){
 protected val num="+919876543210"
 protected val cards=listOf(CallReferences.Card("z","Zulu reference","z value"),CallReferences.Card("a","Alpha reference","a value"))
 protected fun choices(sort:Boolean=true,fold:Boolean=true){runBlocking{ctx.repo.set(K.referenceSort,sort);ctx.repo.set(K.referenceManageFold,fold)};await{LdApp.settings.value.let{it.referenceSort==sort&&it.referenceManageFold==fold}}}
 protected fun saveCards(){java.io.File(ctx.filesDir,"call_references.json").delete();CallReferences.save(ctx,num,"",cards)}
 protected fun start(){seed();choices();saveCards();mount{CallReferencesWorkspace(num,{})};await{rule.onAllNodesWithTag("ref_card_a",true).fetchSemanticsNodes().size==1}}
 protected fun field(tag:String)=rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag(tag)),true)
 protected fun assertAz(){val a=rule.onNodeWithTag("ref_card_a",true).getUnclippedBoundsInRoot();val z=rule.onNodeWithTag("ref_card_z",true).getUnclippedBoundsInRoot();assertTrue(a.top<z.top)}
 protected fun liveRefs(night:Boolean=false){show(night=night);choices();saveCards();runBlocking{ctx.repo.set(K.callReferences,true)};await{LdApp.settings.value.callReferences};open();hit("call_references_open");await{rule.onAllNodesWithTag("ref_card_a",true).fetchSemanticsNodes().size==1}}
 @After fun cleanupRefs(){choices(false,false);runBlocking{ctx.repo.set(K.callReferences,false)};java.io.File(ctx.filesDir,"call_references.json").delete()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceCardViewPart0UiTest:ReferenceCardViewFixture(){
 @Test fun sortTitleViewOnly(){start();val raw=CallReferences.read(ctx,num).raw;hit("reference_sort_az");assertAz();rule.onNodeWithTag("ref_value_a",true).assertTextEquals("Value hidden");assertEquals(raw,CallReferences.read(ctx,num).raw)}
 @Test fun managementFoldsAndOpensPerCard(){start();rule.onNodeWithTag("ref_edit_z",true).assertDoesNotExist();hit("ref_manage_z");rule.onNodeWithTag("ref_edit_z",true).assertExists();rule.onNodeWithTag("ref_edit_a",true).assertDoesNotExist();hit("ref_manage_z");rule.onNodeWithTag("ref_edit_z",true).assertDoesNotExist()}
 @Test fun sortHidesValuesAndManagement(){start();hit("ref_manage_z");hit("ref_reveal_z");rule.onNodeWithTag("ref_value_z",true).assertTextEquals("z value");hit("reference_sort_az");rule.onNodeWithTag("ref_value_z",true).assertTextEquals("Value hidden");rule.onNodeWithTag("ref_edit_z",true).assertDoesNotExist();rule.onNodeWithTag("ref_copy_z",true).assertDoesNotExist()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceCardViewPart1UiTest:ReferenceCardViewFixture(){
 @Test fun sortedEditTargetsOriginalId(){start();hit("reference_sort_az");hit("ref_manage_a");hit("ref_edit_a");field("ref_title").assertTextEquals("Alpha reference");field("ref_value_input").assertTextEquals("a value");input("ref_title","Changed Alpha");hit("ref_save");assertEquals("Changed Alpha",CallReferences.read(ctx,num).cards.single{it.id=="a"}.title);assertEquals(cards[0],CallReferences.read(ctx,num).cards[0])}
 @Test fun deleteStillConfirmed(){start();hit("reference_sort_az");hit("ref_manage_a");hit("ref_delete_a");assertEquals(cards,CallReferences.read(ctx,num).cards);hit("ref_delete_no");assertEquals(cards,CallReferences.read(ctx,num).cards);hit("ref_delete_a");hit("ref_delete_yes");assertEquals(listOf(cards[0]),CallReferences.read(ctx,num).cards)}
 @Test fun searchOnlyTitlesAndHidesValues(){start();hit("ref_reveal_z");input("ref_search","z value");rule.onNodeWithTag("ref_card_z",true).assertDoesNotExist();input("ref_search","");rule.onNodeWithTag("ref_value_z",true).assertTextEquals("Value hidden")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceCardViewSafetyUiTest:ReferenceCardViewFixture(){
 @Test fun sortOffRestoresSavedView(){start();hit("reference_sort_az");choices(sort=false);await{rule.onAllNodesWithTag("reference_sort_az",true).fetchSemanticsNodes().isEmpty()};assertTrue(rule.onNodeWithTag("ref_card_z",true).getUnclippedBoundsInRoot().top<rule.onNodeWithTag("ref_card_a",true).getUnclippedBoundsInRoot().top);assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun layoutOffRestoresManagementHidesValue(){start();hit("ref_reveal_a");choices(fold=false);await{rule.onAllNodesWithTag("ref_edit_a",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("ref_value_a",true).assertTextEquals("Value hidden");rule.onNodeWithTag("ref_manage_a",true).assertDoesNotExist();assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun noClipboardOrExternalAction(){start();val clip=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("Before","unchanged"));hit("reference_sort_za");hit("ref_manage_z");assertEquals("unchanged",clip.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceCardViewLiveUiTest:ReferenceCardViewFixture(){
 @Test fun daySortAndManagement(){liveRefs();move("reference_sort_za");saveShot(rule,"REF480-day-sort");hit("reference_sort_az");move("ref_manage_a");saveShot(rule,"REF480-day-folded");hit("ref_manage_a");move("ref_delete_a");saveShot(rule,"REF480-day-management");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("ref_value_a",true).assertTextEquals("Value hidden")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceCardViewPixelsUiTest:ReferenceCardViewFixture(){
 @Test fun nightLongTitleManagement(){liveRefs(true);val old=CallReferences.read(ctx,num);CallReferences.save(ctx,num,old.raw,cards.map{if(it.id=="a")it.copy(title="Alpha order delivery reference office")else it});hit("ref_reload");hit("reference_sort_az");move("ref_manage_a");saveShot(rule,"REF480-night-folded");hit("ref_manage_a");move("ref_delete_a");saveShot(rule,"REF480-night-management");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("ref_value_a",true).assertTextEquals("Value hidden")}
 @Test fun settingsOff(){seed(night=true);choices(false,false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{CallReferencesPage{}};move("reference_options_settings");saveShot(rule,"REF480-settings-off")}
}
