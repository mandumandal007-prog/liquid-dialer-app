package com.mandu.liquiddialer
import androidx.compose.ui.test.*
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
abstract class RecentsContactNumberFixture:RecentsOverviewFixture(){
 protected fun begin(on:Boolean=true,night:Boolean=false){runBlocking{ctx.repo.set(K.recentsContactNumber,on);ctx.repo.set(K.recentsActionLabels,false);ctx.repo.set(K.recentsSplitMeta,false)};start(night=night);await{rule.onAllNodesWithTag("recent_contact_number_1",true).fetchSemanticsNodes().isNotEmpty()==on}}
 protected fun numberLine(on:Boolean){runBlocking{ctx.repo.set(K.recentsContactNumber,on)};await{LdApp.settings.value.recentsContactNumber==on&&rule.onAllNodesWithTag("recent_contact_number_1",true).fetchSemanticsNodes().isNotEmpty()==on}}
 @After fun resetNumber(){runBlocking{ctx.repo.set(K.recentsContactNumber,false);ctx.repo.set(K.recentsActionLabels,false);ctx.repo.set(K.recentsSplitMeta,false)}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsContactNumberLayoutUiTest:RecentsContactNumberFixture(){
 @Test fun offKeepsOriginalRows(){begin(false);rule.onNodeWithTag("recent_contact_number_1",true).assertDoesNotExist();rule.onNodeWithText("Rahul Kumar Sharma",true).assertExists();assertEquals(3,state.recents.size)}
 @Test fun onSeparateExactNumberPerRow(){begin();for((id,n)in listOf(1 to "+919876543210",2 to "+919123456789",3 to "+919000000003"))rule.onNodeWithTag("recent_contact_number_$id",true).assertTextEquals(Repo.pretty(n));rule.onNodeWithTag("recent_contact_number_1",true).assertIsDisplayed()}
 @Test fun unnamedAndPrivateNoExtraLine(){begin();rule.runOnUiThread{state.recents=state.recents.map{if(it.id==1L)it.copy(name=null)else if(it.id==2L)it.copy(number="",name="Private number")else it}};await{rule.onAllNodesWithTag("recent_contact_number_1",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("recent_contact_number_2",true).assertDoesNotExist();rule.onNodeWithTag("recent_contact_number_3",true).assertExists();rule.onNodeWithText(Repo.pretty("+919876543210"),true).assertExists()}
 @Test fun sameNameRowsKeepTheirOwnNumber(){begin();rule.runOnUiThread{state.recents=state.recents.map{it.copy(name="Rahul")}};settle();rule.onNodeWithTag("recent_contact_number_1",true).assertTextEquals(Repo.pretty("+919876543210"));rule.onNodeWithTag("recent_contact_number_2",true).assertTextEquals(Repo.pretty("+919123456789"))}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsContactNumberSafetyUiTest:RecentsContactNumberFixture(){
 @Test fun togglingKeepsDataAndMissedFilter(){begin();hit(1);val before=state.recents.toList();runBlocking{ctx.repo.set(K.recentsContactNumber,false)};await{rule.onAllNodesWithTag("recent_contact_number_2",true).fetchSemanticsNodes().isEmpty()};runBlocking{ctx.repo.set(K.recentsContactNumber,true)};await{rule.onAllNodesWithTag("recent_contact_number_2",true).fetchSemanticsNodes().size==1};rule.onNodeWithTag("recent_filter_1",true).assertIsSelected();assertEquals(before,state.recents);assertNull(rule.activity.state.pendingCall)}
 @Test fun compactSplitAndLabelsIndependent(){begin();runBlocking{ctx.repo.set(K.recentsCompact,true);ctx.repo.set(K.recentsSplitMeta,true);ctx.repo.set(K.recentsActionLabels,true)};await{LdApp.settings.value.let{it.recentsCompact&&it.recentsSplitMeta&&it.recentsActionLabels}};rule.onNodeWithTag("recent_contact_number_1",true).assertExists();rule.onNodeWithTag("recent_split_type_1",true).assertExists();rule.onNodeWithTag("recent_video_label_1",true).assertExists()}
 @Test fun searchByNumberPreservesExistingResults(){begin();rule.onNode(hasSetTextAction(),true).performTextReplacement("9123456789");settle();rule.onNodeWithTag("recent_contact_number_2",true).assertExists();rule.onNodeWithTag("recent_contact_number_1",true).assertDoesNotExist();assertNull(rule.activity.state.pendingCall)}
 @Test fun defaultOffAndFinder(){assertFalse(LdSettings().recentsContactNumber);assertNotEquals(K.recentsContactNumber,K.recentsActionLabels);assertEquals("settings:display",SettingsFinder.all.single{it.id=="recents-contact-number"}.route)}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsContactNumberDayPixelsUiTest:RecentsContactNumberFixture(){
 @Test fun dayAndMissedNumbers(){begin();saveShot(rule,"HOME495-day-numbers");hit(1);saveShot(rule,"HOME495-day-missed");assertNull(rule.activity.state.pendingCall)}
 @Test fun settingsOffThenPersistOn(){begin(false);mount(true);rule.mainClock.autoAdvance=true;rule.onNodeWithText("Recents contact number line",true).performScrollTo();settle();saveShot(rule,"HOME495-settings-off");rule.onNodeWithText("Recents contact number line",true).performClick();await{LdApp.settings.value.recentsContactNumber};mount();await{rule.onAllNodesWithTag("recent_contact_number_1",true).fetchSemanticsNodes().size==1}}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecentsContactNumberNightPixelsUiTest:RecentsContactNumberFixture(){
 @Test fun nightLargeNumberAndLongName(){begin(night=true);runBlocking{ctx.repo.set(K.recentsActionLabels,true);ctx.repo.set(K.recentsSplitMeta,true)};await{LdApp.settings.value.recentsActionLabels&&LdApp.settings.value.recentsSplitMeta};rule.mainClock.autoAdvance=true;rule.onNodeWithTag("recent_contact_number_1",true).performScrollTo();settle();rule.onNodeWithTag("recent_contact_number_1",true).assertIsDisplayed();rule.onNodeWithTag("recent_video_label_1",true).assertIsDisplayed();saveShot(rule,"HOME495-night-large-numbers");assertNull(rule.activity.state.pendingCall)}
}
