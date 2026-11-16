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
abstract class TallySortFixture:TallyFindFixture(){
 protected fun sortChoices(sort:Boolean=true,paired:Boolean=true){runBlocking{ctx.repo.set(K.tallySort,sort);ctx.repo.set(K.tallyPaired,paired)};await{LdApp.settings.value.let{it.tallySort==sort&&it.tallyPaired==paired}}}
 protected fun start(){sortChoices();sample();add("Wheat bags","5")}
 protected fun order(mode:String){hit("tally_sort_"+mode.lowercase().replace(" ","_"))}
 protected fun before(a:Int,b:Int){assertTrue(rule.onNodeWithTag("tally_item_$a",true).getUnclippedBoundsInRoot().top<rule.onNodeWithTag("tally_item_$b",true).getUnclippedBoundsInRoot().top)}
 @After fun cleanupSort(){sortChoices(false,false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallySortPart0UiTest:TallySortFixture(){
 @Test fun highLowStableAndNoBoardEdit(){start();order("Quantity high-low");before(0,2);before(2,1);rule.onNodeWithTag("tally_total",true).assertTextEquals("10");hit("tally_undo");rule.onNodeWithTag("tally_count",true).assertTextEquals("Raw sum · 2 /12 counters");order("Quantity low-high");before(1,0)}
 @Test fun nameThenBoardOrder(){start();order("Name A-Z");before(0,1);before(1,2);order("Quantity low-high");before(1,0);order("Board order");before(0,1);before(1,2)}
 @Test fun filterCombinesWithOrder(){start();order("Quantity high-low");hit("tally_find_nonzero");rule.onNodeWithTag("tally_item_1",true).assertDoesNotExist();before(0,2);query("wheat");rule.onNodeWithTag("tally_find_count",true).assertTextEquals("1 shown / 3 counters · Nonzero");rule.onNodeWithTag("tally_quantity_2",true).assertTextEquals("5")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallySortPart1UiTest:TallySortFixture(){
 @Test fun sortedActionTargetsOriginalIdentity(){start();order("Quantity low-high");hit("tally_plus_1");rule.onNodeWithTag("tally_quantity_1",true).assertTextEquals("1");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("5");hit("tally_actions_1");hit("tally_remove_1");click("tally_confirm");rule.onNodeWithTag("tally_label_0",true).assertTextEquals("Rice bags");rule.onNodeWithTag("tally_label_1",true).assertTextEquals("Wheat bags");hit("tally_undo");rule.onNodeWithTag("tally_label_1",true).assertTextEquals("Sugar bags")}
 @Test fun sortOffResetsOnlyOrder(){start();query("bags");order("Quantity low-high");sortChoices(sort=false);await{rule.onAllNodesWithTag("tally_sort_order",true).fetchSemanticsNodes().isEmpty()};before(0,1);rule.onNodeWithTag("tally_find_count",true).assertTextEquals("3 shown / 3 counters · All");sortChoices();await{rule.onAllNodesWithTag("tally_sort_order",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("tally_sort_order",true).assertTextEquals("View order: Board order")}
 @Test fun editValueReorders(){start();order("Quantity high-low");hit("tally_actions_1");hit("tally_edit_1");input("tally_value","9");hit("tally_apply");before(1,0);before(0,2);rule.onNodeWithTag("tally_label_1",true).assertTextEquals("Sugar bags")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallySortLayoutUiTest:TallySortFixture(){
 @Test fun pairedOffKeepsViewAndTypedEdit(){start();query("bags");order("Quantity low-high");hit("tally_actions_0");hit("tally_edit_0");input("tally_name","Rice sacks");sortChoices(paired=false);before(1,0);rule.onNodeWithTag("tally_sort_order",true).assertTextEquals("View order: Quantity low-high");rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag("tally_name")),true).assertTextEquals("Rice sacks");rule.onNodeWithTag("tally_find_count",true).assertTextEquals("3 shown / 3 counters · All")}
 @Test fun sortWorksWithoutFinder(){start();choices(find=false);await{rule.onAllNodesWithTag("tally_find_query",true).fetchSemanticsNodes().isEmpty()};order("Quantity low-high");before(1,0);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun reopenResetsSort(){start();order("Quantity high-low");mount{CallTallyWorkspace({})};rule.onNodeWithTag("tally_sort_order",true).assertTextEquals("View order: Board order")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallySortLiveUiTest:TallySortFixture(){
 @Test fun dayControlsAndSortedRows(){show();choices();sortChoices();runBlocking{ctx.repo.set(K.callTally,true)};await{LdApp.settings.value.callTally};open();hit("call_tally_open");add("Rice bags","5");add("Sugar bags","0");order("Quantity low-high");move("tally_sort_quantity_low-high");saveShot(rule,"SORT476-day-controls");move("tally_quantity_1");saveShot(rule,"SORT476-day-sorted");before(1,0);rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallySortPixelsUiTest:TallySortFixture(){
 @Test fun nightWrappedControls(){show(night=true);choices();sortChoices();runBlocking{ctx.repo.set(K.callTally,true)};await{LdApp.settings.value.callTally};open();hit("call_tally_open");add("Rice premium large family pack bags with matching unit","1000000000");order("Quantity high-low");move("tally_sort_quantity_low-high");saveShot(rule,"SORT476-night-controls");move("tally_actions_0");saveShot(rule,"SORT476-night-counter");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOff(){seed(night=true);choices(false,false);sortChoices(false,false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{CallTallyPage{}};move("tally_sort_settings");saveShot(rule,"SORT476-settings-off")}
}
