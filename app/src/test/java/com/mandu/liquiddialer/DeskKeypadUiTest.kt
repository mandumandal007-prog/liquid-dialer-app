package com.mandu.liquiddialer
import android.telecom.Call
import androidx.compose.ui.test.*
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.*
import org.robolectric.shadow.api.Shadow
import java.time.Duration
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi",shadows=[IvrCallShadow::class]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskKeypadUiTest:LiveCallDeskFixture(){
 private fun showKeys(on:Boolean=true,night:Boolean=false){show(night=night);runBlocking{ctx.ldStore.edit{it[K.deskKeypad]=on;it[K.ivrHelper]=false}};await{LdApp.settings.value.deskKeypad==on};target=deskShadowCall();CallManager.calls.value=listOf(target);settle();open()}
 private fun tones()=Shadow.extract<IvrCallShadow>(target).tones
 @After fun resetKeypad(){runBlocking{ctx.repo.set(K.deskKeypad,false)};Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(200))}
 @Test fun offNoEntryNormalDeskWorks(){showKeys(false);rule.onAllNodesWithTag("desk_keypad_toggle",true).assertCountEquals(0);rule.onNodeWithTag("call_note_draft",true).assertExists();rule.onNodeWithTag("end_call",true).assertIsDisplayed();assertTrue(tones().isEmpty())}
 @Test fun openHideDoesNotSend(){showKeys();click("desk_keypad_toggle");rule.onNodeWithTag("desk_keypad",true).assertExists();assertTrue(tones().isEmpty());click("desk_keypad_toggle");rule.onAllNodesWithTag("desk_keypad",true).assertCountEquals(0);assertTrue(tones().isEmpty())}
 @Test fun actualKeyRequestsExactlyOneTone(){showKeys();click("desk_keypad_toggle");click("desk_key_2");assertEquals(listOf('2'),tones());rule.onNodeWithTag("desk_key_notice",true).assertTextEquals("One tone requested. Listen for the IVR response.");rule.onNodeWithTag("end_call",true).assertIsDisplayed();Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(150));assertEquals(1,Shadow.extract<IvrCallShadow>(target).stops);saveShot(rule,"KEY455-tone-requested-day")}
 @Test fun draftSurvivesKeypadShowHide(){showKeys();scroll("call_note_draft");rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].performTextInput("Keep this draft");click("desk_keypad_toggle");click("desk_keypad_toggle");rule.onNodeWithTag("call_note_draft",true).onChildren().filter(hasSetTextAction())[0].assertTextEquals("Keep this draft");assertTrue(Repo.notesFor(ctx,"+919876543210").isEmpty());assertTrue(tones().isEmpty())}
 @Test fun unfinishedChecklistSurvivesKeypadToggle(){showKeys();click("call_checklist_open");await{rule.onAllNodesWithTag("checklist_add",true).fetchSemanticsNodes().size==1};scroll("checklist_add");click("checklist_add");scroll("checklist_draft");rule.onNodeWithTag("checklist_draft",true).onChildren().filter(hasSetTextAction())[0].performTextInput("Question for support");click("desk_keypad_toggle");click("desk_keypad_toggle");rule.onNodeWithTag("checklist_draft",true).onChildren().filter(hasSetTextAction())[0].assertTextEquals("Question for support");assertTrue(tones().isEmpty());rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun keypadHiddenByLayout(){showKeys();runBlocking{ctx.repo.set(K.layoutGrid,"mute,!keypad,record,hold,add,merge,!audio")};await{rule.onAllNodesWithTag("desk_keypad_toggle",true).fetchSemanticsNodes().isEmpty()};assertTrue(tones().isEmpty())}
 @Test fun offWhileOpenClearsKeypadKeepsDeskDraft(){showKeys();click("desk_keypad_toggle");runBlocking{ctx.repo.set(K.deskKeypad,false)};await{rule.onAllNodesWithTag("desk_keypad",true).fetchSemanticsNodes().isEmpty()};rule.onNodeWithTag("call_desk",true).assertExists();rule.onNodeWithTag("call_note_draft",true).assertExists();assertTrue(tones().isEmpty())}
 @Test fun callWaitingClosesDeskNoTone(){showKeys();click("desk_keypad_toggle");CallManager.calls.value=listOf(target,deskShadowCall("+919999999999",Call.STATE_RINGING));settle();rule.onAllNodesWithTag("desk_key_2",true).assertCountEquals(0);assertTrue(tones().isEmpty())}
 @Test fun replacingSameNumberHasNoOldKeypad(){showKeys();click("desk_keypad_toggle");val old=target;target=deskShadowCall();CallManager.calls.value=listOf(target);settle();rule.onAllNodesWithTag("desk_keypad",true).assertCountEquals(0);assertTrue(tones().isEmpty());assertTrue(Shadow.extract<IvrCallShadow>(old).tones.isEmpty())}
 @Test fun dayKeypadPixels(){showKeys();click("desk_keypad_toggle");rule.onNodeWithTag("call_desk_mute",true).assertIsDisplayed();rule.onNodeWithTag("call_desk_speaker",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"KEY455-keypad-day")}
}
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w320dp-h640dp-xhdpi",shadows=[IvrCallShadow::class]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeskKeypadPixelsUiTest:LiveCallDeskFixture(){
 @After fun resetKeypad(){runBlocking{ctx.repo.set(K.deskKeypad,false)}}
 @Test fun narrowNightKeypadAndFixedEnd(){show(night=true);runBlocking{ctx.repo.set(K.deskKeypad,true)};await{LdApp.settings.value.deskKeypad};target=deskShadowCall();CallManager.calls.value=listOf(target);settle();open();click("desk_keypad_toggle");rule.onNodeWithTag("end_call",true).assertIsDisplayed();saveShot(rule,"KEY455-narrow-night-large");rule.onNodeWithTag("call_desk_tools",true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy){it(0f,220f)};settle();saveShot(rule,"KEY455-narrow-scrolled-night-large");rule.onNodeWithTag("desk_key_#",true).assertIsDisplayed();rule.onNodeWithTag("end_call",true).assertIsDisplayed()}
 @Test fun settingsOffThenOnNoDeskAutoEnable(){seed(false,night=true);runBlocking{ctx.repo.set(K.deskKeypad,false)};await{!LdApp.settings.value.deskKeypad};mount{DeskKeypadPage{}};assertFalse(LdApp.settings.value.deskKeypad);saveShot(rule,"KEY455-settings-night-large");rule.onAllNodesWithText("Live desk keypad",useUnmergedTree=true)[1].performClick();await{LdApp.settings.value.deskKeypad};assertFalse(LdApp.settings.value.liveCallDesk)}
}
