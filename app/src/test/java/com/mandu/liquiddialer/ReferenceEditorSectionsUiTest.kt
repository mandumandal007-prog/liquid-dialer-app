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
abstract class ReferenceEditorSectionsFixture:ReferenceSectionsFixture(){
 protected fun editor(on:Boolean){runBlocking{ctx.repo.set(K.referenceEditorSections,on)};await{LdApp.settings.value.referenceEditorSections==on}}
 protected fun beginEditor(on:Boolean=true){seed();choices();editor(on);saveCards();mount{CallReferencesWorkspace(num,{})};await{rule.onAllNodesWithTag("ref_card_a",true).fetchSemanticsNodes().size==1};hit("ref_add")}
 @After fun resetEditor(){editor(false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceEditorSectionsLayoutUiTest:ReferenceEditorSectionsFixture(){
 @Test fun offOriginalFields(){beginEditor(false);rule.onNodeWithTag("reference_draft_status",true).assertDoesNotExist();field("ref_title").assertTextEquals("");field("ref_value_input").assertTextEquals("")}
 @Test fun onSeparatePanelsAndEmptyCounts(){beginEditor();rule.onNodeWithTag("reference_draft_status",true).assertExists();rule.onNodeWithTag("reference_draft_title_panel",true).assertExists();rule.onNodeWithTag("reference_draft_value_panel",true).assertExists();rule.onNodeWithTag("reference_draft_title_count",true).assertTextEquals("0/40 title length");rule.onNodeWithTag("reference_draft_value_count",true).assertTextEquals("0/500 value length");assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun liveCountsMatchRawDraft(){beginEditor();input("ref_title","New title");input("ref_value_input","ordinary fact");rule.onNodeWithTag("reference_draft_title_count",true).assertTextEquals("9/40 title length");rule.onNodeWithTag("reference_draft_value_count",true).assertTextEquals("13/500 value length");assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun toggleKeepsUnsavedDraftNoWrite(){beginEditor();input("ref_title","Draft");input("ref_value_input","Only a draft");editor(false);await{rule.onAllNodesWithTag("reference_draft_status",true).fetchSemanticsNodes().isEmpty()};field("ref_title").assertTextEquals("Draft");editor(true);await{rule.onAllNodesWithTag("reference_draft_status",true).fetchSemanticsNodes().size==1};field("ref_value_input").assertTextEquals("Only a draft");assertEquals(cards,CallReferences.read(ctx,num).cards)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceEditorSectionsSaveUiTest:ReferenceEditorSectionsFixture(){
 @Test fun cancelNeverSaves(){beginEditor();input("ref_title","Discard");input("ref_value_input","Discard value");hit("ref_cancel");assertEquals(cards,CallReferences.read(ctx,num).cards);rule.onNodeWithTag("reference_draft_status",true).assertDoesNotExist()}
 @Test fun explicitSaveAddsAndHides(){beginEditor();input("ref_title","New card");input("ref_value_input","New fact");hit("ref_save");val saved=CallReferences.read(ctx,num).cards;assertEquals(3,saved.size);assertEquals("New card",saved.last().title);rule.onNodeWithTag("ref_value_${saved.last().id}",true).assertTextEquals("Value hidden");assertEquals(cards,saved.take(2))}
 @Test fun invalidBlankStaysDraftNoWrite(){beginEditor();hit("ref_save");rule.onNodeWithTag("ref_error",true).assertExists();rule.onNodeWithTag("reference_draft_status",true).assertExists();assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun staleWriteRefusedRetainsDraft(){beginEditor();input("ref_title","Draft");input("ref_value_input","Draft fact");val old=CallReferences.read(ctx,num);val changed=cards.map{if(it.id=="a")it.copy(title="Changed elsewhere")else it};CallReferences.save(ctx,num,old.raw,changed);hit("ref_save");rule.onNodeWithTag("ref_error",true).assertExists();field("ref_title").assertTextEquals("Draft");assertEquals(changed,CallReferences.read(ctx,num).cards)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceEditorSectionsIdentityUiTest:ReferenceEditorSectionsFixture(){
 @Test fun editUsesOriginalIdAndCounts(){beginEditor();hit("ref_cancel");hit("reference_sort_az");hit("ref_manage_a");hit("ref_edit_a");field("ref_title").assertTextEquals("Alpha reference");rule.onNodeWithTag("reference_draft_title_count",true).assertTextEquals("15/40 title length");input("ref_title","Changed");hit("ref_save");val saved=CallReferences.read(ctx,num).cards;assertEquals(cards[0],saved[0]);assertEquals("a",saved[1].id);assertEquals("Changed",saved[1].title)}
 @Test fun noCopyOrExternalEffectFromDraft(){beginEditor();val clip=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("before","unchanged"));input("ref_title","Draft");input("ref_value_input","Not copied");editor(false);editor(true);assertEquals("unchanged",clip.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity);assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun defaultsOffAndFinderRoute(){assertFalse(LdSettings().referenceEditorSections);assertNotEquals(K.referenceSections,K.referenceEditorSections);assertEquals("callreferences",SettingsFinder.all.single{it.id=="reference-editor-sections"}.route)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceEditorSectionsDayPixelsUiTest:ReferenceEditorSectionsFixture(){
 @Test fun liveDayDraftPanels(){liveRefs();editor(true);hit("ref_add");input("ref_title","Delivery desk");input("ref_value_input","Office opens at 9 AM");move("reference_draft_title_panel");saveShot(rule,"REF487-day-title");move("ref_save");saveShot(rule,"REF487-day-save");rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertEquals(cards,CallReferences.read(ctx,num).cards)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceEditorSectionsNightPixelsUiTest:ReferenceEditorSectionsFixture(){
 @Test fun nightLongTitleAndValue(){liveRefs(true);editor(true);hit("ref_add");input("ref_title","Order delivery reference office title");input("ref_value_input","Ordinary office hours and delivery desk");move("reference_draft_status");saveShot(rule,"REF487-night-status");move("ref_cancel");saveShot(rule,"REF487-night-save");rule.onNodeWithTag("ref_cancel",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun settingsOffPersists(){seed(night=true);editor(false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{CallReferencesPage{}};move("reference_options_settings");saveShot(rule,"REF487-settings-off");rule.onNodeWithText("Sectioned reference editor layout",true).performScrollTo().performClick();await{LdApp.settings.value.referenceEditorSections};editor(false)}
}
