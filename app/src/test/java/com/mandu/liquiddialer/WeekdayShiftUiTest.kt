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
abstract class WeekdayFixture:LiveCallDeskFixture(){
 protected fun move(tag:String){repeat(35){val node=rule.onNodeWithTag(tag,true);val b=node.getUnclippedBoundsInRoot();val desk=rule.onAllNodesWithTag("call_desk_tools",true).fetchSemanticsNodes().isNotEmpty();val margin=if(desk)8.dp else 0.dp;val sc=if(desk)rule.onNodeWithTag("call_desk_tools",true)else rule.onAllNodes(hasScrollAction(),true)[0];val v=sc.getUnclippedBoundsInRoot();if(b.top>=v.top+margin&&b.bottom<=v.bottom-margin){settle();node.assertIsDisplayed();return};val dy=if(b.top<v.top+margin)(b.top-v.top-margin).value else(b.bottom-v.bottom+margin).value;sc.performSemanticsAction(SemanticsActions.ScrollBy){it(0f,dy*rule.density.density)};settle()};error("Not visible $tag")}
 protected fun hit(tag:String){move(tag);click(tag)}
 protected fun input(tag:String,text:String){move(tag);rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag(tag)),true).performTextReplacement(text);settle();rule.runOnUiThread{rule.activity.currentFocus?.clearFocus()};settle()}
 protected fun ready(night:Boolean=false){seed(night=night);runBlocking{ctx.repo.set(K.weekdayShift,true)};await{LdApp.settings.value.weekdayShift}}
 protected fun direct(){ready();mount{WeekdayShiftWorkspace({})}}
 protected fun calc(){input("weekday_first","2026-11-13");input("weekday_count","1");hit("weekday_calculate")}
 protected fun live(night:Boolean=false){show(night=night);runBlocking{ctx.repo.set(K.weekdayShift,true);ctx.repo.set(K.callDates,true)};await{LdApp.settings.value.weekdayShift&&LdApp.settings.value.callDates};open();hit("call_dates_open");hit("dates_weekday_open")}
 @After fun resetWeekday(){runBlocking{ctx.repo.set(K.weekdayShift,false);ctx.repo.set(K.callDates,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeekdayShiftUiTest:WeekdayFixture(){
 @Test fun explicitCalculateAndRules(){direct();rule.onNodeWithTag("weekday_calculate",true).assertIsNotEnabled();calc();rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-16 · Monday");assertNull(org.robolectric.Shadows.shadowOf(rule.activity).nextStartedActivity)}
 @Test fun changesInvalidateResult(){direct();calc();input("weekday_count","-1");rule.onNodeWithTag("weekday_result",true).assertDoesNotExist();hit("weekday_calculate");rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-12 · Thursday");hit("weekday_day_4");rule.onNodeWithTag("weekday_result",true).assertDoesNotExist()}
 @Test fun exclusionsAndClear(){direct();input("weekday_first","2026-11-13");input("weekday_count","1");input("weekday_excluded","2026-11-16");hit("weekday_calculate");rule.onNodeWithTag("weekday_value",true).assertTextEquals("2026-11-17 · Tuesday");hit("weekday_clear");rule.onNodeWithTag("weekday_result",true).assertDoesNotExist();rule.onNodeWithTag("weekday_calculate",true).assertIsNotEnabled()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeekdayShiftSafetyUiTest:WeekdayFixture(){
 @Test fun invalidNoStaleResult(){direct();calc();input("weekday_excluded","2026-02-30");hit("weekday_calculate");rule.onNodeWithTag("weekday_error",true).assertExists();rule.onNodeWithTag("weekday_result",true).assertDoesNotExist()}
 @Test fun noSelectedWeekdayDisables(){direct();input("weekday_first","2026-11-13");(1..5).forEach{hit("weekday_day_$it")};rule.onNodeWithTag("weekday_calculate",true).assertIsNotEnabled()}
 @Test fun settingsOffNoChildEntry(){seed();runBlocking{ctx.repo.set(K.weekdayShift,false)};await{!LdApp.settings.value.weekdayShift};mount{CallDatesWorkspace({})};rule.onNodeWithTag("dates_weekday_open",true).assertDoesNotExist()}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeekdayShiftLiveUiTest:WeekdayFixture(){
 @Test fun backPreservesOriginalInputs(){ready();mount{CallDatesWorkspace({})};input("dates_first","2026-11-13");hit("dates_weekday_open");calc();hit("weekday_back");rule.onNode(hasSetTextAction()and hasAnyAncestor(hasTestTag("dates_first")),true).assertTextEquals("2026-11-13")}
 @Test fun offReturnsOriginalAndClearsChild(){ready();mount{CallDatesWorkspace({})};hit("dates_weekday_open");calc();runBlocking{ctx.repo.set(K.weekdayShift,false)};await{rule.onAllNodesWithTag("dates_first",true).fetchSemanticsNodes().isNotEmpty()};rule.onNodeWithTag("weekday_result",true).assertDoesNotExist();runBlocking{ctx.repo.set(K.weekdayShift,true)};await{rule.onAllNodesWithTag("dates_weekday_open",true).fetchSemanticsNodes().isNotEmpty()};hit("dates_weekday_open");rule.onNodeWithTag("weekday_calculate",true).assertIsNotEnabled()}
 @Test fun dayLiveResultPixels(){live();calc();move("weekday_value");rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"WEEKDAY469-day-result");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty())}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeekdayShiftPixelsUiTest:WeekdayFixture(){
 @Test fun nightLargeWeekdaysAndResult(){live(true);move("weekday_day_5");saveShot(rule,"WEEKDAY469-night-choices");input("weekday_first","2026-11-13");input("weekday_count","-2");input("weekday_excluded","2026-11-12");hit("weekday_calculate");move("weekday_value");saveShot(rule,"WEEKDAY469-night-result");input("weekday_count","0");hit("weekday_calculate");move("weekday_error");saveShot(rule,"WEEKDAY469-night-error");rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOffPixels(){ready(true);runBlocking{ctx.repo.set(K.weekdayShift,false)};await{!LdApp.settings.value.weekdayShift};mount{WeekdayShiftPage{}};saveShot(rule,"WEEKDAY469-settings-off")}
}
