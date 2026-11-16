package com.mandu.liquiddialer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class ConverterOptionsFixture:LiveCallDeskFixture(){
 protected fun move(tag:String){repeat(35){val node=rule.onNodeWithTag(tag,true);val b=node.getUnclippedBoundsInRoot();val desk=rule.onAllNodesWithTag("call_desk_tools",true).fetchSemanticsNodes().isNotEmpty();val margin=if(desk)8.dp else 0.dp;val sc=if(desk)rule.onNodeWithTag("call_desk_tools",true)else rule.onAllNodes(hasScrollAction(),true)[0];val v=sc.getUnclippedBoundsInRoot();if(b.top>=v.top+margin&&b.bottom<=v.bottom-margin){settle();node.assertIsDisplayed();return};val dy=if(b.top<v.top+margin)(b.top-v.top-margin).value else(b.bottom-v.bottom+margin).value;sc.performSemanticsAction(SemanticsActions.ScrollBy){it(0f,dy*rule.density.density)};settle()};error("Not visible $tag")}
 protected fun hit(tag:String){move(tag);click(tag)}
 protected fun input(tag:String,text:String){move(tag);rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(text);settle();rule.runOnUiThread{rule.activity.currentFocus?.clearFocus()};settle()}


 protected fun options(table:Boolean=true,fold:Boolean=true){runBlocking{ctx.repo.set(K.converterTable,table);ctx.repo.set(K.converterFold,fold)};await{LdApp.settings.value.converterTable==table&&LdApp.settings.value.converterFold==fold}}
 protected fun direct(){seed();options();mount{CallConverterWorkspace({})}}
 protected fun table(){input("converter_input","1");hit("converter_table_make")}
 protected fun live(night:Boolean=false){show(night=night);options();runBlocking{ctx.repo.set(K.callConverter,true)};await{LdApp.settings.value.callConverter};open();hit("call_converter_open")}
 @After fun cleanupOptions(){runBlocking{ctx.repo.set(K.converterTable,false);ctx.repo.set(K.converterFold,false);ctx.repo.set(K.callConverter,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterOptionsPart0UiTest:ConverterOptionsFixture(){
 @Test fun tableExplicitAndSeparateHistory(){direct();rule.onNodeWithTag("converter_table_make",true).assertIsNotEnabled();table();rule.onNodeWithTag("converter_table_count",true).assertTextEquals("6 equivalent unit values");rule.onNodeWithTag("converter_table_value_ft",true).assertTextEquals("3.28083989501312336");rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 0 / 10");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun foldKeepsSelectedLabelsAndExplicitUnits(){direct();rule.onNodeWithTag("converter_from_m",true).assertDoesNotExist();rule.onNodeWithTag("converter_from_label",true).assertTextEquals("From: Meter (m)");hit("converter_from_toggle");hit("converter_from_km");hit("converter_from_toggle");rule.onNodeWithTag("converter_from_m",true).assertDoesNotExist();rule.onNodeWithTag("converter_from_label",true).assertTextEquals("From: Kilometer (km)");table();rule.onNodeWithTag("converter_table_value_m",true).assertTextEquals("1000")}
 @Test fun editAndCategoryClearTable(){direct();table();input("converter_input","2");rule.onNodeWithTag("converter_table_count",true).assertDoesNotExist();hit("converter_table_make");hit("converter_kind_mass");rule.onNodeWithTag("converter_table_count",true).assertDoesNotExist();hit("converter_table_make");rule.onNodeWithTag("converter_table_count",true).assertTextEquals("4 equivalent unit values")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterOptionsPart1UiTest:ConverterOptionsFixture(){
 @Test fun invalidNoTableOrHistory(){direct();table();input("converter_input","1,000");hit("converter_table_make");rule.onNodeWithTag("converter_table_count",true).assertDoesNotExist();rule.onNodeWithTag("converter_table_error",true).assertExists();rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 0 / 10")}
 @Test fun layoutOffRestoresButtonsKeepsResult(){direct();table();options(fold=false);await{rule.onAllNodesWithTag("converter_from_m",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("converter_from_toggle",true).assertDoesNotExist();rule.onNodeWithTag("converter_table_value_ft",true).assertTextEquals("3.28083989501312336");options();await{rule.onAllNodesWithTag("converter_from_m",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("converter_table_count",true).assertExists()}
 @Test fun tableOffClearsOnlyTable(){direct();table();hit("converter_convert");options(table=false);await{rule.onAllNodesWithTag("converter_table_count",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("converter_value",true).assertExists();rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 1 / 10");options();await{rule.onAllNodesWithTag("converter_table_make",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("converter_table_count",true).assertDoesNotExist()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterOptionsSafetyUiTest:ConverterOptionsFixture(){
 @Test fun clearInputClearsTableNotHistory(){direct();table();hit("converter_convert");hit("converter_clear");rule.onNodeWithTag("converter_table_count",true).assertDoesNotExist();rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 1 / 10")}
 @Test fun swapClearsTableAndKeepsInput(){direct();table();hit("converter_swap");rule.onNodeWithTag("converter_table_count",true).assertDoesNotExist();hit("converter_table_make");rule.onNodeWithTag("converter_table_value_m",true).assertTextEquals("0.3048")}
 @Test fun longInputRefusedNotTruncated(){direct();input("converter_input","1000000000"+"0".repeat(20));hit("converter_table_make");rule.onNodeWithTag("converter_table_error",true).assertExists();rule.onNodeWithTag("converter_table_count",true).assertDoesNotExist()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterOptionsLiveUiTest:ConverterOptionsFixture(){
 @Test fun dayFoldAndTablePixels(){live();input("converter_input","2.5");move("converter_from_toggle");saveShot(rule,"CONV472-day-folded");hit("converter_from_toggle");move("converter_from_m");saveShot(rule,"CONV472-day-expanded");hit("converter_from_toggle");hit("converter_table_make");move("converter_table_row_m");saveShot(rule,"CONV472-day-table");rule.onNodeWithTag("end_call",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterOptionsPixelsUiTest:ConverterOptionsFixture(){
 @Test fun nightFullLabelsAndHugeValue(){live(true);hit("converter_from_toggle");hit("converter_from_km");hit("converter_from_toggle");move("converter_from_label");saveShot(rule,"CONV472-night-folded");input("converter_input","1000000000");hit("converter_table_make");move("converter_table_row_mm");saveShot(rule,"CONV472-night-table");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsBothOff(){seed(night=true);options(false,false);mount{ConverterOptionsPage{}};saveShot(rule,"CONV472-settings-off")}
}
