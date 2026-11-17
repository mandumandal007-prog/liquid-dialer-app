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
abstract class ReferenceViewFoldFixture:ReferenceSectionsFixture(){
 protected fun foldView(on:Boolean){runBlocking{ctx.repo.set(K.referenceViewFold,on)};await{LdApp.settings.value.referenceViewFold==on}}
 protected fun beginView(on:Boolean=true){seed();choices();sections(true);foldView(on);saveCards();mount{CallReferencesWorkspace(num,{})};await{rule.onAllNodesWithTag("ref_card_a",true).fetchSemanticsNodes().size==1}}
 @After fun resetView(){foldView(false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceViewFoldLayoutUiTest:ReferenceViewFoldFixture(){
 @Test fun offRetainsVisibleControls(){beginView(false);rule.onNodeWithTag("ref_search",true).assertExists();rule.onNodeWithTag("reference_sort_az",true).assertExists();rule.onNodeWithTag("reference_view_summary",true).assertDoesNotExist()}
 @Test fun onStartsFoldedWithAccurateSummary(){beginView();rule.onNodeWithTag("ref_search",true).assertDoesNotExist();rule.onNodeWithTag("reference_sort_az",true).assertDoesNotExist();rule.onNodeWithTag("reference_view_count",true).assertTextEquals("2 of 2 saved cards shown");rule.onNodeWithTag("reference_view_filter",true).assertTextEquals("Title filter: none");rule.onNodeWithTag("reference_view_order",true).assertTextEquals("View: Saved order");position("a","Saved card 2 of 2")}
 @Test fun openCloseRetainsFilteredView(){beginView();hit("reference_view_toggle");input("ref_search","Alpha");hit("reference_view_toggle");rule.onNodeWithTag("reference_view_count",true).assertTextEquals("1 of 2 saved cards shown");rule.onNodeWithTag("reference_view_filter",true).assertTextEquals("Title filter active");rule.onNodeWithTag("ref_card_z",true).assertDoesNotExist();hit("reference_view_toggle");rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag("ref_search")),true).assertTextEquals("Alpha")}
 @Test fun foldedOrderSummaryMatchesSortedIdentity(){beginView();hit("reference_view_toggle");hit("reference_sort_az");hit("reference_view_toggle");assertAz();rule.onNodeWithTag("reference_view_order",true).assertTextEquals("View: Title A-Z");position("a","Saved card 2 of 2");assertEquals(cards,CallReferences.read(ctx,num).cards)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceViewFoldSafetyUiTest:ReferenceViewFoldFixture(){
 @Test fun openingHidesValueAndManagement(){beginView();hit("ref_manage_z");hit("ref_reveal_z");hit("reference_view_toggle");rule.onNodeWithTag("ref_value_z",true).assertTextEquals("Value hidden");rule.onNodeWithTag("ref_edit_z",true).assertDoesNotExist();rule.onNodeWithTag("ref_copy_z",true).assertDoesNotExist()}
 @Test fun closingHidesValueAndManagement(){beginView();hit("reference_view_toggle");hit("ref_manage_a");hit("ref_reveal_a");hit("reference_view_toggle");rule.onNodeWithTag("ref_value_a",true).assertTextEquals("Value hidden");rule.onNodeWithTag("ref_edit_a",true).assertDoesNotExist()}
 @Test fun disablingRestoresControlsKeepsFilterAndOrder(){beginView();hit("reference_view_toggle");input("ref_search","Alpha");hit("reference_sort_za");hit("reference_view_toggle");foldView(false);await{rule.onAllNodesWithTag("ref_search",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("reference_view_summary",true).assertDoesNotExist();rule.onNodeWithTag("ref_card_z",true).assertDoesNotExist();rule.onNode(hasText("✓ Title Z-A")and hasAnyAncestor(hasTestTag("reference_sort_za")),true).assertExists();assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun sortDisabledSummaryReturnsSavedOrder(){beginView();hit("reference_view_toggle");hit("reference_sort_az");hit("reference_view_toggle");choices(sort=false);await{rule.onAllNodesWithText("View: Saved order",true).fetchSemanticsNodes().size==1};assertEquals(cards,CallReferences.read(ctx,num).cards)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceViewFoldStateUiTest:ReferenceViewFoldFixture(){
 @Test fun reloadResetsExpandedStateButRetainsView(){beginView();hit("reference_view_toggle");input("ref_search","Alpha");hit("ref_reload");rule.onNodeWithTag("ref_search",true).assertDoesNotExist();rule.onNodeWithTag("reference_view_filter",true).assertTextEquals("Title filter active");assertEquals(cards,CallReferences.read(ctx,num).cards)}
 @Test fun noMatchesIsVisibleWhenFolded(){beginView();hit("reference_view_toggle");input("ref_search","missing");hit("reference_view_toggle");rule.onNodeWithTag("reference_view_count",true).assertTextEquals("0 of 2 saved cards shown");rule.onNodeWithTag("ref_empty",true).assertExists();rule.onNodeWithTag("ref_card_z",true).assertDoesNotExist()}
 @Test fun noStorageClipboardOrExternalEffects(){beginView();val raw=CallReferences.read(ctx,num).raw;val clip=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)as android.content.ClipboardManager;clip.setPrimaryClip(android.content.ClipData.newPlainText("before","unchanged"));hit("reference_view_toggle");hit("reference_sort_az");hit("reference_view_toggle");foldView(false);assertEquals(raw,CallReferences.read(ctx,num).raw);assertEquals("unchanged",clip.primaryClip!!.getItemAt(0).text.toString());assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun defaultOffAndFinderRoute(){assertFalse(LdSettings().referenceViewFold);assertNotEquals(K.referenceManageFold,K.referenceViewFold);assertEquals("callreferences",SettingsFinder.all.single{it.id=="reference-view-fold"}.route)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceViewFoldDayPixelsUiTest:ReferenceViewFoldFixture(){
 @Test fun dayFoldedAndExpanded(){liveRefs();sections(true);foldView(true);move("reference_view_summary");saveShot(rule,"REF486-day-folded");hit("reference_view_toggle");move("reference_sort_za");saveShot(rule,"REF486-day-expanded");input("ref_search","Alpha");hit("reference_sort_az");hit("reference_view_toggle");move("reference_view_summary");saveShot(rule,"REF486-day-filtered");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("ref_value_a",true).assertTextEquals("Value hidden")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReferenceViewFoldNightPixelsUiTest:ReferenceViewFoldFixture(){
 @Test fun nightFoldedSummary(){liveRefs(true);sections(true);foldView(true);hit("reference_view_toggle");input("ref_search","Alpha");hit("reference_sort_za");hit("reference_view_toggle");move("reference_view_summary");saveShot(rule,"REF486-night-filtered");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("reference_view_filter",true).assertTextEquals("Title filter active")}
 @Test fun settingsSwitchOffAndPersists(){seed(night=true);foldView(false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{CallReferencesPage{}};move("reference_options_settings");saveShot(rule,"REF486-settings-off");rule.onNodeWithText("Fold reference view controls",true).performScrollTo().performClick();await{LdApp.settings.value.referenceViewFold};foldView(false)}
}
