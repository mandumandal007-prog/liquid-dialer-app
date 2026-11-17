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
abstract class ReferenceDraftReviewFixture:ReferenceEditorSectionsFixture(){
 protected fun review(on:Boolean){runBlocking{ctx.repo.set(K.referenceDraftReview,on)};await{LdApp.settings.value.referenceDraftReview==on}}
 protected fun beginReview(on:Boolean=true){seed();choices();editor(true);review(on);saveCards();mount{CallReferencesWorkspace(num,{})};await{rule.onAllNodesWithTag("ref_card_a",true).fetchSemanticsNodes().size==1};hit("ref_add")}
 protected fun draft(){input("ref_title","  Draft title  ");input("ref_value_input","  Ordinary fact  ")}
 @After fun resetReview(){review(false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceDraftReviewLayoutUiTest:ReferenceDraftReviewFixture(){
 @Test fun offSavesInOriginalOneStep(){beginReview(false);draft();hit("ref_save");assertEquals(3,CallReferences.read(ctx,num).cards.size);rule.onNodeWithTag("reference_review_panel",true).assertDoesNotExist()}
 @Test fun reviewShowsTrimmedUnsavedValues(){beginReview();draft();hit("ref_save");rule.onNodeWithTag("reference_review_title",true).assertTextEquals("Draft title");rule.onNodeWithTag("reference_review_value",true).assertTextEquals("Ordinary fact");rule.onNodeWithTag("ref_title",true).assertDoesNotExist();assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun backKeepsRawDraftAndNextReviewUpdates(){beginReview();draft();hit("ref_save");hit("reference_review_back");field("ref_title").assertTextEquals("  Draft title  ");input("ref_title","Changed draft");hit("ref_save");rule.onNodeWithTag("reference_review_title",true).assertTextEquals("Changed draft");assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun disablingReviewReturnsToEditNoSave(){beginReview();draft();hit("ref_save");review(false);await{rule.onAllNodesWithTag("reference_review_panel",true).fetchSemanticsNodes().isEmpty()};field("ref_value_input").assertTextEquals("  Ordinary fact  ");assertEquals(cards,CallReferences.read(ctx,num).cards)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceDraftReviewSaveUiTest:ReferenceDraftReviewFixture(){
 @Test fun confirmSavesOnlyReviewedTrimmedData(){beginReview();draft();hit("ref_save");assertEquals(cards,CallReferences.read(ctx,num).cards);hit("reference_review_save");val saved=CallReferences.read(ctx,num).cards;assertEquals(3,saved.size);assertEquals("Draft title",saved.last().title);assertEquals("Ordinary fact",saved.last().value);rule.onNodeWithTag("ref_value_${saved.last().id}",true).assertTextEquals("Value hidden")}
 @Test fun cancelFromReviewDiscardsNoWrite(){beginReview();draft();hit("ref_save");hit("ref_cancel");assertEquals(cards,CallReferences.read(ctx,num).cards);rule.onNodeWithTag("reference_review_panel",true).assertDoesNotExist()}
 @Test fun invalidNeverEntersReview(){beginReview();hit("ref_save");rule.onNodeWithTag("ref_error",true).assertExists();rule.onNodeWithTag("reference_review_panel",true).assertDoesNotExist();assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun staleConfirmRefusesAndBackKeepsDraft(){beginReview();draft();hit("ref_save");val old=CallReferences.read(ctx,num);val changed=cards.map{if(it.id=="a")it.copy(title="Changed elsewhere")else it};CallReferences.save(ctx,num,old.raw,changed);hit("reference_review_save");rule.onNodeWithTag("ref_error",true).assertExists();assertEquals(changed,CallReferences.read(ctx,num).cards);hit("reference_review_back");field("ref_title").assertTextEquals("  Draft title  ")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceDraftReviewSafetyUiTest:ReferenceDraftReviewFixture(){
 @Test fun sortedEditConfirmUsesOriginalId(){beginReview();hit("ref_cancel");hit("reference_sort_az");hit("ref_manage_a");hit("ref_edit_a");input("ref_title","Edited Alpha");hit("ref_save");assertEquals(cards,CallReferences.read(ctx,num).cards);hit("reference_review_save");val saved=CallReferences.read(ctx,num).cards;assertEquals(cards[0],saved[0]);assertEquals("a",saved[1].id);assertEquals("Edited Alpha",saved[1].title)}
 @Test fun reviewDoesNotCopyOrOpenExternal(){beginReview();val clip=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("before","unchanged"));draft();hit("ref_save");assertEquals("unchanged",clip.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity);assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun defaultOffAndFinderRoute(){assertFalse(LdSettings().referenceDraftReview);assertNotEquals(K.referenceDraftReview,K.referenceEditorSections);assertEquals("callreferences",SettingsFinder.all.single{it.id=="reference-draft-review"}.route)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceDraftReviewDayPixelsUiTest:ReferenceDraftReviewFixture(){
 @Test fun dayReviewAndActions(){liveRefs();editor(true);review(true);hit("ref_add");input("ref_title","Delivery desk");input("ref_value_input","Office opens at 9 AM");hit("ref_save");move("reference_review_panel");saveShot(rule,"REF488-day-review");move("ref_cancel");saveShot(rule,"REF488-day-actions");rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertEquals(cards,CallReferences.read(ctx,num).cards)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceDraftReviewNightPixelsUiTest:ReferenceDraftReviewFixture(){
 @Test fun nightLongReviewAndActions(){liveRefs(true);editor(true);review(true);hit("ref_add");input("ref_title","Order delivery reference office title");input("ref_value_input","Ordinary office hours and delivery desk");hit("ref_save");move("reference_review_notice");saveShot(rule,"REF488-night-review");move("ref_cancel");saveShot(rule,"REF488-night-actions");rule.onNodeWithTag("reference_review_save",true).assertIsDisplayed();rule.onNodeWithTag("reference_review_back",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun settingsOffPersists(){seed(night=true);review(false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{CallReferencesPage{}};rule.onNodeWithText("Review reference draft before save",true).performScrollTo();saveShot(rule,"REF488-settings-off");rule.onNodeWithText("Review reference draft before save",true).performClick();await{LdApp.settings.value.referenceDraftReview};review(false)}
}
