package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class ConverterFullUnitsFixture:ConverterOptionsFixture(){
 @Before fun resetUnits(){units(false)}
 @After fun cleanUnits(){units(false)}
 protected fun units(on:Boolean=true){runBlocking{ctx.repo.set(K.converterFullUnits,on)};await{LdApp.settings.value.converterFullUnits==on};settle()}
 protected fun converter(night:Boolean=false,fold:Boolean=false){seed(night=night);options(fold=fold);mount{CallConverterWorkspace({})}}
 protected fun choice(which:String,id:String,full:Boolean=true,selected:Boolean=false){val u=CallConverter.unit(id);rule.onNodeWithTag("converter_${which}_$id",true).onChildren().filter(hasText((if(selected)"✓ "else"")+(if(full)u.label else u.label.substringAfter("(").substringBefore(")")))).assertCountEquals(1)}
 protected fun full(which:String,id:String){val n=rule.onNode(hasText(CallConverter.unit(id).label,substring=true)and hasAnyAncestor(hasTestTag("converter_${which}_$id")),true);val out=mutableListOf<TextLayoutResult>();n.performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(out)};val l=out.single();assertEquals(l.layoutInput.text.length,l.getLineEnd(l.lineCount-1));for(i in 0 until l.lineCount){assertFalse(l.isLineEllipsized(i));assertTrue(l.getLineRight(i)<=l.size.width+1f);assertTrue(l.getLineBottom(i)<=l.size.height+1f)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterFullUnitsUiTest:ConverterFullUnitsFixture(){
 @Test fun defaultAndOwnSwitchOnly(){assertFalse(LdSettings().converterFullUnits);seed();mount{CallConverterPage{}};val before=LdApp.settings.value;hit("converter_full_units_switch");await{LdApp.settings.value.converterFullUnits};assertEquals(before.copy(converterFullUnits=true),LdApp.settings.value);move("converter_full_units_switch");saveShot(rule,"UNITS527-settings-on");hit("converter_full_units_switch");await{!LdApp.settings.value.converterFullUnits};assertEquals(before,LdApp.settings.value);assertEquals("callconverter",SettingsFinder.all.single{it.id=="converter-full-units"}.route)}
 @Test fun labelsToggleWithoutChangingInputSelectionResultOrHistory(){converter();input("converter_input","2");hit("converter_convert");val old=rule.onNodeWithTag("converter_value",true).fetchSemanticsNode().config;units();choice("from","m",selected=true);choice("to","ft",selected=true);move("converter_from_m");saveShot(rule,"UNITS527-day-on");rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 1 / 10");assertEquals(old,rule.onNodeWithTag("converter_value",true).fetchSemanticsNode().config);units(false);choice("from","m",false,true);choice("to","ft",false,true);move("converter_from_m");saveShot(rule,"UNITS527-day-off");rule.onNode(hasSetTextAction(),true).assertTextEquals("2")}
 @Test fun fullNamesSelectAndConvertSameValues(){converter();units();hit("converter_from_km");hit("converter_to_m");input("converter_input","2");hit("converter_convert");rule.onNodeWithTag("converter_value",true).assertTextEquals("= 2000 Meter (m)");choice("from","km",selected=true);choice("to","m",selected=true);assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun foldAndCategoryStillUseOwnChoices(){converter(fold=true);units();rule.onNodeWithTag("converter_from_m",true).assertDoesNotExist();hit("converter_from_toggle");choice("from","m",selected=true);hit("converter_from_km");hit("converter_from_toggle");rule.onNodeWithTag("converter_from_km",true).assertDoesNotExist();hit("converter_kind_mass");hit("converter_from_toggle");CallConverter.of(CallConverter.Kind.MASS).forEach{full("from",it.id)};rule.onNodeWithTag("converter_from_km",true).assertDoesNotExist()}
 @Test fun tableAndHistorySurviveLayoutSwitch(){converter();input("converter_input","1");hit("converter_convert");hit("converter_table_make");units();rule.onNodeWithTag("converter_table_value_ft",true).assertTextEquals("3.28083989501312336");rule.onNodeWithTag("converter_history_count",true).assertTextEquals("Session history: 1 / 10");units(false);rule.onNodeWithTag("converter_table_count",true).assertTextEquals("6 equivalent unit values")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConverterFullUnitsNightUiTest:ConverterFullUnitsFixture(){
 @Test fun allCategoriesWrapAndLastChoiceReachable(){converter(night=true);units();for(k in CallConverter.Kind.entries){hit("converter_kind_${k.name.lowercase()}");for(w in listOf("from","to"))CallConverter.of(k).forEach{full(w,it.id)}};hit("converter_kind_length");move("converter_from_km");saveShot(rule,"UNITS527-night-on");val last=CallConverter.of(CallConverter.Kind.LENGTH).last();move("converter_to_"+last.id);rule.onNodeWithTag("converter_to_"+last.id,true).assertIsDisplayed();saveShot(rule,"UNITS527-night-last");units(false);move("converter_to_"+last.id);saveShot(rule,"UNITS527-night-off")}
}
