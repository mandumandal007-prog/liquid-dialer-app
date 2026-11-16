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
abstract class ShelfFixture:DeskFinderFixture(){
 protected fun choices(shelf:Boolean=true,compact:Boolean=true){runBlocking{ctx.repo.set(K.deskShelf,shelf);ctx.repo.set(K.deskFinderCompact,compact)};await{LdApp.settings.value.let{it.deskShelf==shelf&&it.deskFinderCompact==compact}}}
 protected fun begin(night:Boolean=false){choices();prepared(night=night,all=true);finder()}
 protected fun pin(id:String,q:String){query(q);tap("desk_finder_pin_$id")}
 protected fun count(n:Int){rule.onNodeWithTag("desk_shelf_count",true).assertTextEquals("Quick-tool shelf: $n / 4")}
 @After fun cleanupShelf(){choices(false,false)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskShelfPart0UiTest:ShelfFixture(){
 @Test fun manualPinSearchIndependent(){begin();count(0);pin("estimate","calculator");count(1);query("no matching tool");rule.onNodeWithTag("desk_finder_empty",true).assertExists();rule.onNodeWithTag("desk_shelf_open_estimate",true).assertExists();tap("desk_finder_toggle");count(1);rule.onNodeWithTag("desk_finder",true).assertDoesNotExist();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun unpinAndClearOnlyShelf(){begin();pin("estimate","calculator");pin("converter","kg");count(2);tap("desk_shelf_unpin_estimate");count(1);tap("desk_shelf_clear");count(0);rule.onNodeWithTag("desk_finder_count",true).assertTextEquals("1 matching / 11 available tools")}
 @Test fun fullNoReplacement(){begin();pin("estimate","calculator");pin("converter","kg");pin("tally","pieces");pin("dates","calendar");count(4);pin("reader","padho");count(4);rule.onNodeWithTag("desk_shelf_open_reader",true).assertDoesNotExist();rule.onNodeWithTag("desk_shelf_open_estimate",true).assertExists()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskShelfPart1UiTest:ShelfFixture(){
 @Test fun openAndReturnPreservesPins(){begin();pin("converter","kg");tap("desk_shelf_open_converter");rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 0 / 10");rule.onNodeWithTag("end_call",true).assertIsDisplayed();tap("converter_back");count(1);rule.onNodeWithTag("desk_shelf_open_converter",true).assertExists();assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun disabledToolRemovedNeverRestored(){begin();pin("converter","kg");runBlocking{ctx.repo.set(K.callConverter,false)};await{rule.onAllNodesWithTag("desk_shelf_open_converter",true).fetchSemanticsNodes().isEmpty()};count(0);runBlocking{ctx.repo.set(K.callConverter,true)};await{LdApp.settings.value.callConverter};count(0)}
 @Test fun offClearsShelfKeepsQuery(){begin();pin("estimate","calculator");choices(shelf=false);await{rule.onAllNodesWithTag("desk_shelf",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("desk_finder_count",true).assertTextEquals("2 matching / 11 available tools");choices();await{rule.onAllNodesWithTag("desk_shelf_count",true).fetchSemanticsNodes().size==1};count(0)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskShelfLayoutUiTest:ShelfFixture(){
 @Test fun compactDetailsAndOffKeepsShelfQuery(){begin();pin("estimate","calculator");rule.onNodeWithTag("desk_finder_detail_estimate",true).assertDoesNotExist();tap("desk_finder_details_estimate");rule.onNodeWithTag("desk_finder_detail_estimate",true).assertExists();choices(compact=false);await{rule.onAllNodesWithTag("desk_finder_details_estimate",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("desk_finder_detail_estimate",true).assertExists();count(1);rule.onNodeWithTag("desk_finder_count",true).assertTextEquals("2 matching / 11 available tools");choices();await{rule.onAllNodesWithTag("desk_finder_detail_estimate",true).fetchSemanticsNodes().isEmpty()}}
 @Test fun closeDeskClearsSession(){begin();pin("estimate","calculator");click("call_desk_close");click("call_desk_close_confirm");open();count(0)}
 @Test fun finderOffClearsShelf(){begin();pin("estimate","calculator");runBlocking{ctx.repo.set(K.deskFinder,false)};await{rule.onAllNodesWithTag("desk_shelf",true).fetchSemanticsNodes().isEmpty()};runBlocking{ctx.repo.set(K.deskFinder,true)};await{rule.onAllNodesWithTag("desk_shelf_count",true).fetchSemanticsNodes().size==1};count(0)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskShelfLiveUiTest:ShelfFixture(){
 @Test fun dayShelfAndCompact(){begin();pin("estimate","calculator");pin("converter","kg");tap("desk_finder_toggle");move("desk_shelf_open_converter");saveShot(rule,"SHELF474-day-shelf");finder();query("calculator");move("desk_finder_open_estimate");saveShot(rule,"SHELF474-day-compact");tap("desk_finder_details_estimate");move("desk_finder_detail_estimate");saveShot(rule,"SHELF474-day-details");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskShelfPixelsUiTest:ShelfFixture(){
 @Test fun nightShelfAndDetails(){begin(true);pin("reader","padho");tap("desk_finder_toggle");move("desk_shelf_open_reader");saveShot(rule,"SHELF474-night-shelf");finder();query("padho");tap("desk_finder_details_reader");move("desk_finder_open_reader");saveShot(rule,"SHELF474-night-details");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
 @Test fun settingsBothOff(){seed(night=true);choices(false,false);runBlocking{ctx.repo.set(K.fontScale,1f)};await{LdApp.settings.value.fontScale==1f};mount{DeskFinderPage{}};rule.onAllNodesWithText("Quick-tool shelf",true).assertCountEquals(1);saveShot(rule,"SHELF474-settings-off")}
}
