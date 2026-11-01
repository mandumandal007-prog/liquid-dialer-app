package com.mandu.liquiddialer

import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real cold/warm activity path with Compose-managed animation clock and lifecycle cleanup.
 * Raw ActivityController.setup drains infinite Choreographer frames and never becomes idle. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FavWidgetFillPathActivityTest {
    private val cold = Intent(ApplicationProvider.getApplicationContext<LdApp>(), MainActivity::class.java)
        .setAction(Intent.ACTION_DIAL).putExtra("number", "+91 98765 43210")
    @get:Rule val rule = AndroidComposeTestRule(ActivityScenarioRule<MainActivity>(cold)) { r ->
        var activity: MainActivity? = null
        r.scenario.onActivity { activity = it }
        activity!!
    }

    @Test fun coldDialFillAndWarmNewIntentAndChooserExtra() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<LdApp>()
        app.ldStore.edit { p -> p[K.onboarded] = true }

        // Cold: widget person tap -> number fill, dialer tab, kabhi call nahi
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(1000)
        rule.waitForIdle()
        val act = rule.activity
        assertEquals("+91 98765 43210", act.state.dial)
        assertEquals("dialer", act.state.tab)
        assertNull("fill kabhi call nahi karta", act.state.pendingCall)
        assertNull(act.state.confirmNumber)

        // Warm: naya tap naya number bharta hai (singleTask -> onNewIntent)
        rule.runOnUiThread { org.robolectric.util.ReflectionHelpers.callInstanceMethod<Unit>(act, "onNewIntent", org.robolectric.util.ReflectionHelpers.ClassParameter.from(Intent::class.java, Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_DIAL).putExtra("number", "5550001111"))) }
        assertEquals("5550001111", act.state.dial)
        assertEquals("dialer", act.state.tab)
        assertNull(act.state.pendingCall)

        // Warm hint: contacts tab, dial pad untouched
        rule.runOnUiThread { org.robolectric.util.ReflectionHelpers.callInstanceMethod<Unit>(act, "onNewIntent", org.robolectric.util.ReflectionHelpers.ClassParameter.from(Intent::class.java, Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN).putExtra("tab", "contacts"))) }
        assertEquals("contacts", act.state.tab)
        assertEquals("5550001111", act.state.dial)
        assertNull(act.state.pendingCall)

        // Warm chooser: multi-number favorite -> numberChoiceRequest set, koi fill nahi
        rule.runOnUiThread { org.robolectric.util.ReflectionHelpers.callInstanceMethod<Unit>(act, "onNewIntent", org.robolectric.util.ReflectionHelpers.ClassParameter.from(Intent::class.java, Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN).putExtra("chooseNumber", 11L).putExtra("chooseName", "Rahul Sharma"))) }
        assertEquals(11L to "Rahul Sharma", act.state.numberChoiceRequest)
        assertEquals("5550001111", act.state.dial)
        assertNull(act.state.pendingCall)
        Unit
    }
}
