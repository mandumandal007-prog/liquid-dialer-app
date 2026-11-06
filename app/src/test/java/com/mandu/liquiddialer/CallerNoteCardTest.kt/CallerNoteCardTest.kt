package com.mandu.liquiddialer

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.Repo
import com.mandu.liquiddialer.data.ldStore
import com.mandu.liquiddialer.telecom.CallManager
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Round C: saved note for the calling number auto-dikhta hai incoming + ongoing call pe (floating glass card). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallerNoteCardTest {

    @get:Rule val rule = createAndroidComposeRule<CallActivity>()

    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    @Before
    fun seed() = runBlocking {
        CallManager.calls.value = emptyList(); CallManager.bump()
        rule.activity.resources.configuration.fontScale = 1f
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu"; p[K.theme] = "daylight"; p[K.dialerScale] = 1f
            p[K.gridScale] = 1f; p[K.bottomScale] = 1f; p[K.callCardScale] = 1f
        }
        CallManager.videoNotice.value = null
        Repo.notes(ctx).forEach { Repo.deleteNote(ctx, it.id) }
        Unit
    }

    private fun waitCard() {
        rule.mainClock.autoAdvance = false
        val t0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - t0 < 6000) {
            rule.mainClock.advanceTimeBy(200); rule.waitForIdle()
            if (rule.onAllNodesWithTag("caller_note_card").fetchSemanticsNodes().isNotEmpty()) return
            Thread.sleep(80)
        }
        throw AssertionError("caller_note_card kabhi nahi dikha")
    }

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun showCall(state: Int, number: String = "+91 98765 43210") {
        CallManager.audio.value = CallAudioState(false, CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.calls.value = listOf(FakeCall.make(number, "Rahul Sharma", state,
            if (state == Call.STATE_ACTIVE) System.currentTimeMillis() - 75_000 else 0L))
        CallManager.bump()
    }

    @Test
    fun incomingCallShowsSavedNoteCard() {
        Repo.addNote(ctx, "+91 98765 43210", "Rahul Sharma", "payment ki baat karni hai")
        settle(2500)
        showCall(Call.STATE_RINGING)
        waitCard()
        rule.onNodeWithTag("caller_note_card").assertIsDisplayed()
        rule.onNodeWithText("payment ki baat karni hai", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun ongoingCallShowsSavedNoteCard() {
        Repo.addNote(ctx, "+91 98765 43210", "Rahul Sharma", "docs bhejne hain")
        settle(2500)
        showCall(Call.STATE_ACTIVE)
        waitCard()
        rule.onNodeWithTag("caller_note_card").assertIsDisplayed()
        rule.onNodeWithText("docs bhejne hain", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun noNoteNoCard() {
        settle(2500)
        showCall(Call.STATE_RINGING, "+91 90000 00000")
        settle(2500)
        rule.onNodeWithTag("caller_note_card").assertDoesNotExist()
    }

    @Test
    fun noteKeyedByFullIdentityNotLastDigits() {
        // +91 98765 43210 ka note kisi aur country code ke same digits pe NAHI dikhna chahiye
        Repo.addNote(ctx, "+91 98765 43210", "Rahul Sharma", "secret plan")
        settle(2500)
        showCall(Call.STATE_RINGING, "+1 98765 43210")
        settle(2500)
        rule.onNodeWithTag("caller_note_card").assertDoesNotExist()
    }
}
