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

/** Round C pixel evidence: caller-note card incoming + ongoing, day + large-font. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CallerNoteShotTest {

    @get:Rule val rule = createAndroidComposeRule<CallActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<LdApp>()

    @Before
    fun seed() = runBlocking {
        CallManager.calls.value = emptyList(); CallManager.bump()
        ctx.ldStore.edit { p ->
            p[K.onboarded] = true; p[K.profileName] = "Mandu Mandal"; p[K.theme] = "midnight"
            p[K.gridScale] = 1f; p[K.bottomScale] = 1f; p[K.callCardScale] = 1f; p[K.dialerScale] = 1f
            p[K.callBgStyle] = "blobs"; p[K.callAvatarShape] = "circle"; p[K.callShowNumber] = true
        }
        CallManager.videoNotice.value = null
        Repo.notes(ctx).forEach { Repo.deleteNote(ctx, it.id) }
        Repo.addNote(ctx, "+91 98765 43210", "Rahul Sharma", "Payment ki baat karni hai - 5000 ka settlement")
        CallManager.audio.value = CallAudioState(false, CallAudioState.ROUTE_EARPIECE,
            CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        Unit
    }

    private fun waitCard() { settle(2500);rule.onNodeWithTag("caller_note_card").assertDoesNotExist();org.junit.Assert.assertTrue(Repo.notesFor(ctx,"+91 98765 43210").isNotEmpty()) }

    private fun settle(ms: Long = 1500) {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    private fun show(state: Int) {
        CallManager.calls.value = listOf(FakeCall.make("+91 98765 43210", "Rahul Sharma", state,
            if (state == Call.STATE_ACTIVE) System.currentTimeMillis() - 75_000 else 0L))
        CallManager.bump()
    }

    @Test fun incomingDayNoteCard() {
        settle(2500); show(Call.STATE_RINGING); waitCard()
        saveShot(rule, "C-note-incoming-day")
    }

    @Test fun ongoingDayNoteCard() {
        settle(2500); show(Call.STATE_ACTIVE); waitCard()
        saveShot(rule, "C-note-ongoing-day")
    }

    @Test fun incomingLargeFontNoteCard() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        try {
        settle(2500); show(Call.STATE_RINGING)
        try { waitCard() } catch (t: Throwable) {
            rule.onAllNodesWithTag("caller_note_card", useUnmergedTree = true).fetchSemanticsNodes()
            val tree = rule.onAllNodes(isRoot(), useUnmergedTree = true)[0].printToString(200)
            throw AssertionError("card missing large-font. tree(head): " + tree.take(2500), t)
        }
        saveShot(rule, "C-note-incoming-largefont")
        } finally { org.robolectric.RuntimeEnvironment.setFontScale(1.0f) }
    }
}
