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
abstract class ReferenceSectionsFixture:ReferenceCardViewFixture(){
 protected fun sections(on:Boolean){runBlocking{ctx.repo.set(K.referenceSections,on)};await{LdApp.settings.value.referenceSections==on}}
 protected fun begin(on:Boolean=true){seed();choices();sections(on);saveCards();mount{CallReferencesWorkspace(num,{})};await{rule.onAllNodesWithTag("ref_card_a",true).fetchSemanticsNodes().size==1}}
 protected fun position(id:String,value:String){rule.onNodeWithTag("ref_section_position_$id",true).assertTextEquals(value)}
 @After fun resetSections(){sections(false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceSectionsLayoutUiTest:ReferenceSectionsFixture(){
 @Test fun offRetainsOriginal(){begin(false);rule.onNodeWithTag("ref_section_header_z",true).assertDoesNotExist();rule.onNodeWithTag("ref_value_z",true).assertTextEquals("Value hidden");rule.onNodeWithTag("ref_manage_z",true).assertExists()}
 @Test fun onHasDistinctSectionsNoValueLeak(){begin();position("z","Saved card 1 of 2");position("a","Saved card 2 of 2");rule.onNodeWithTag("ref_section_value_z",true).assertExists();rule.onNodeWithTag("ref_section_actions_z",true).assertTextEquals("CARD ACTIONS");rule.onNodeWithTag("ref_value_z",true).assertTextEquals("Value hidden");rule.onNodeWithTag("ref_copy_z",true).assertDoesNotExist()}
 @Test fun sortedPositionIsSavedNotView(){begin();hit("reference_sort_az");assertAz();position("a","Saved card 2 of 2");position("z","Saved card 1 of 2");assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun filteredPositionRetainsFullSavedCount(){begin();input("ref_search","Alpha");position("a","Saved card 2 of 2");rule.onNodeWithTag("ref_card_z",true).assertDoesNotExist();assertEquals(cards,CallReferences.read(ctx,num).cards)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceSectionsActionsUiTest:ReferenceSectionsFixture(){
 @Test fun switchingOnHidesRevealedAndManagement(){begin(false);hit("ref_manage_z");hit("ref_reveal_z");sections(true);await{rule.onAllNodesWithTag("ref_section_header_z",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("ref_value_z",true).assertTextEquals("Value hidden");rule.onNodeWithTag("ref_edit_z",true).assertDoesNotExist();rule.onNodeWithTag("ref_copy_z",true).assertDoesNotExist()}
 @Test fun switchingOffHidesRevealedAndManagement(){begin();hit("ref_manage_a");hit("ref_reveal_a");sections(false);await{rule.onAllNodesWithTag("ref_section_header_a",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("ref_value_a",true).assertTextEquals("Value hidden");rule.onNodeWithTag("ref_edit_a",true).assertDoesNotExist()}
 @Test fun revealCopyStillExplicit(){begin();val clip=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("before","unchanged"));assertEquals("unchanged",clip.primaryClip!!.getItemAt(0).text.toString());hit("ref_reveal_a");rule.onNodeWithTag("ref_value_a",true).assertTextEquals("a value");assertEquals("unchanged",clip.primaryClip!!.getItemAt(0).text.toString());hit("ref_copy_a");assertEquals("a value",clip.primaryClip!!.getItemAt(0).text.toString())}
 @Test fun layoutChangeNoDataOrExternalEffect(){begin();val raw=CallReferences.read(ctx,num).raw;val clip=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("before","unchanged"));sections(false);sections(true);assertEquals(raw,CallReferences.read(ctx,num).raw);assertEquals("unchanged",clip.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceSectionsIdentityUiTest:ReferenceSectionsFixture(){
 @Test fun editAfterSortTargetsSavedCard(){begin();hit("reference_sort_az");hit("ref_manage_a");hit("ref_edit_a");input("ref_title","Updated Alpha");hit("ref_save");assertEquals("Updated Alpha",CallReferences.read(ctx,num).cards[1].title);assertEquals(cards[0],CallReferences.read(ctx,num).cards[0]);position("a","Saved card 2 of 2")}
 @Test fun deleteStillNeedsConfirmationThenRenumbers(){begin();hit("ref_manage_z");hit("ref_delete_z");assertEquals(cards,CallReferences.read(ctx,num).cards);hit("ref_delete_no");position("a","Saved card 2 of 2");hit("ref_delete_z");hit("ref_delete_yes");position("a","Saved card 1 of 1");assertEquals(listOf(cards[1]),CallReferences.read(ctx,num).cards)}
 @Test fun unfoldedManagementStillAvailable(){begin();choices(fold=false);await{rule.onAllNodesWithTag("ref_edit_z",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("ref_manage_z",true).assertDoesNotExist();rule.onNodeWithTag("ref_delete_z",true).assertExists();position("z","Saved card 1 of 2")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceSectionsSettingsUiTest:ReferenceSectionsFixture(){
 @Test fun defaultsOffAndFinderRoute(){assertFalse(LdSettings().referenceSections);assertNotEquals(K.referenceSort,K.referenceSections);val e=SettingsFinder.all.single{it.id=="reference-sections"};assertEquals("callreferences",e.route)}
 @Test fun settingsSwitchPersists(){seed();sections(false);mount{CallReferencesPage{}};rule.onNodeWithText("Sectioned reference card layout",true).performScrollTo();rule.onNodeWithText("Sectioned reference card layout",true).performClick();await{LdApp.settings.value.referenceSections};runBlocking{ctx.repo.set(K.referenceSections,false)};await{!LdApp.settings.value.referenceSections}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceSectionsDayPixelsUiTest:ReferenceSectionsFixture(){
 @Test fun liveDaySectionsAndExplicitReveal(){liveRefs();sections(true);hit("reference_sort_az");move("ref_section_header_a");saveShot(rule,"REF483-day-header");move("ref_manage_a");saveShot(rule,"REF483-day-actions");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("ref_value_a",true).assertTextEquals("Value hidden");position("a","Saved card 2 of 2")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceSectionsNightPixelsUiTest:ReferenceSectionsFixture(){
 @Test fun liveNightLongTitleAndManagement(){liveRefs(true);sections(true);val old=CallReferences.read(ctx,num);CallReferences.save(ctx,num,old.raw,cards.map{if(it.id=="a")it.copy(title="Alpha order delivery reference office")else it});hit("ref_reload");hit("reference_sort_az");move("ref_section_header_a");saveShot(rule,"REF483-night-header");hit("ref_manage_a");move("ref_delete_a");saveShot(rule,"REF483-night-management");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("ref_value_a",true).assertTextEquals("Value hidden")}
 @Test fun settingsOffPixels(){seed(night=true);sections(false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{CallReferencesPage{}};move("reference_options_settings");saveShot(rule,"REF483-settings-off")}
}
