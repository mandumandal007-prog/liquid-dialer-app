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
abstract class TallyFindFixture:ConverterOptionsFixture(){
 protected fun choices(find:Boolean=true,fold:Boolean=true){runBlocking{ctx.repo.set(K.tallyFind,find);ctx.repo.set(K.tallyFold,fold)};await{LdApp.settings.value.let{it.tallyFind==find&&it.tallyFold==fold}}}
 protected fun board(){seed();choices();mount{CallTallyWorkspace({})}}
 protected fun add(label:String,value:String){input("tally_name",label);input("tally_value",value);hit("tally_apply")}
 protected fun sample(){board();add("Rice bags","5");add("Sugar bags","0")}
 protected fun query(q:String){input("tally_find_query",q)}
 @After fun cleanupFind(){choices(false,false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallyFindPart0UiTest:TallyFindFixture(){
 @Test fun queryIsViewNotEdit(){sample();query("rice");rule.onNodeWithTag("tally_find_count",true).assertTextEquals("1 shown / 2 counters · All");rule.onNodeWithTag("tally_item_1",true).assertDoesNotExist();rule.onNodeWithTag("tally_total",true).assertTextEquals("5");hit("tally_find_clear");rule.onNodeWithTag("tally_quantity_1",true).assertTextEquals("0");hit("tally_undo");rule.onNodeWithTag("tally_count",true).assertTextEquals("Raw sum · 1 /12 counters")}
 @Test fun zeroNonzeroAndLiveAdjustment(){sample();hit("tally_find_zero");rule.onNodeWithTag("tally_item_0",true).assertDoesNotExist();hit("tally_plus_1");rule.onNodeWithTag("tally_find_empty",true).assertExists();rule.onNodeWithTag("tally_total",true).assertTextEquals("6");hit("tally_find_nonzero");rule.onNodeWithTag("tally_find_count",true).assertTextEquals("2 shown / 2 counters · Nonzero")}
 @Test fun noMatchClearKeepsBoard(){sample();query("no matching");rule.onNodeWithTag("tally_find_empty",true).assertExists();rule.onNodeWithTag("tally_total",true).assertTextEquals("5");hit("tally_find_clear");rule.onNodeWithTag("tally_find_count",true).assertTextEquals("2 shown / 2 counters · All");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallyFindPart1UiTest:TallyFindFixture(){
 @Test fun zeroWholeBoardIncludesHiddenUndoRestores(){sample();add("Other bags","7");query("rice");hit("tally_zero");click("tally_confirm");rule.onNodeWithTag("tally_total",true).assertTextEquals("0");hit("tally_undo");rule.onNodeWithTag("tally_total",true).assertTextEquals("12");hit("tally_find_clear");rule.onNodeWithTag("tally_quantity_2",true).assertTextEquals("7")}
 @Test fun finderOffResetsViewNotBoard(){sample();query("rice");choices(find=false);await{rule.onAllNodesWithTag("tally_find_query",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("tally_item_1",true).assertExists();choices();await{rule.onAllNodesWithTag("tally_find_count",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("tally_find_count",true).assertTextEquals("2 shown / 2 counters · All")}
 @Test fun foldedActionsRequireExplicitOpen(){sample();rule.onNodeWithTag("tally_edit_0",true).assertDoesNotExist();hit("tally_actions_0");rule.onNodeWithTag("tally_edit_0",true).assertExists();hit("tally_actions_0");rule.onNodeWithTag("tally_edit_0",true).assertDoesNotExist();rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("5")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallyFindLayoutUiTest:TallyFindFixture(){
 @Test fun layoutOffKeepsQueryBoardAndTypedEdit(){sample();query("rice");hit("tally_actions_0");hit("tally_edit_0");input("tally_name","Rice sacks");choices(fold=false);await{rule.onAllNodesWithTag("tally_actions_0",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("tally_edit_0",true).assertExists();rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag("tally_name")),true).assertTextEquals("Rice sacks");rule.onNodeWithTag("tally_label_0",true).assertTextEquals("Rice bags");rule.onNodeWithTag("tally_find_count",true).assertTextEquals("1 shown / 2 counters · All");choices();await{rule.onAllNodesWithTag("tally_edit_0",true).fetchSemanticsNodes().isEmpty()}}
 @Test fun filteredRemoveStillConfirms(){sample();query("rice");hit("tally_actions_0");hit("tally_remove_0");click("tally_keep");rule.onNodeWithTag("tally_total",true).assertTextEquals("5");hit("tally_remove_0");click("tally_confirm");rule.onNodeWithTag("tally_find_empty",true).assertExists();hit("tally_undo");rule.onNodeWithTag("tally_quantity_0",true).assertTextEquals("5")}
 @Test fun reopenClearsTemporaryView(){sample();query("rice");mount{CallTallyWorkspace({})};rule.onNodeWithTag("tally_find_count",true).assertTextEquals("0 shown / 0 counters · All")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallyFindLiveUiTest:TallyFindFixture(){
 @Test fun dayFilterAndActions(){show();choices();runBlocking{ctx.repo.set(K.callTally,true)};await{LdApp.settings.value.callTally};open();hit("call_tally_open");add("Rice bags","5");add("Sugar bags","0");query("rice");move("tally_find_count");saveShot(rule,"FIND475-day-filter");move("tally_actions_0");saveShot(rule,"FIND475-day-folded");hit("tally_actions_0");move("tally_remove_0");saveShot(rule,"FIND475-day-actions");rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class TallyFindPixelsUiTest:TallyFindFixture(){
 @Test fun nightLongCounter(){show(night=true);choices();runBlocking{ctx.repo.set(K.callTally,true)};await{LdApp.settings.value.callTally};open();hit("call_tally_open");add("Rice premium large family pack bags with matching unit","1000000000");query("rice");move("tally_actions_0");saveShot(rule,"FIND475-night-folded");query("none");move("tally_find_empty");saveShot(rule,"FIND475-night-empty");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOff(){seed(night=true);choices(false,false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{CallTallyPage{}};move("tally_settings_options");saveShot(rule,"FIND475-settings-off")}
}
