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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real cold/warm activity fill path for widget taps. CI-only: is 2GB local sandbox me
 *  Compose cold-activity hang hota hai (resource constraint); CI runner pe poori suite ke saath chalti hai.
 *  Local verification = routeIntent pure tests (FavWidgetTest) + RemoteViews pipeline (FavWidgetExecTest). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1440dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FavWidgetFillPathActivityTest {

    @Test fun coldDialFillAndWarmNewIntentAndChooserExtra() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<LdApp>()
        app.ldStore.edit { p -> p[K.onboarded] = true }

        // Cold: widget person tap -> number fill, dialer tab, kabhi call nahi
        val cold = Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_DIAL).putExtra("number", "+91 98765 43210")
        val controller = Robolectric.buildActivity(MainActivity::class.java, cold).setup()
        val act = controller.get()
        assertEquals("+91 98765 43210", act.state.dial)
        assertEquals("dialer", act.state.tab)
        assertNull("fill kabhi call nahi karta", act.state.pendingCall)
        assertNull(act.state.confirmNumber)

        // Warm: naya tap naya number bharta hai (singleTask -> onNewIntent)
        controller.newIntent(Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_DIAL).putExtra("number", "5550001111"))
        assertEquals("5550001111", act.state.dial)
        assertEquals("dialer", act.state.tab)
        assertNull(act.state.pendingCall)

        // Warm hint: contacts tab, dial pad untouched
        controller.newIntent(Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN).putExtra("tab", "contacts"))
        assertEquals("contacts", act.state.tab)
        assertEquals("5550001111", act.state.dial)
        assertNull(act.state.pendingCall)

        // Warm chooser: multi-number favorite -> numberChoiceRequest set, koi fill nahi
        controller.newIntent(Intent(app, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN).putExtra("chooseNumber", 11L).putExtra("chooseName", "Rahul Sharma"))
        assertEquals(11L to "Rahul Sharma", act.state.numberChoiceRequest)
        assertEquals("5550001111", act.state.dial)
        assertNull(act.state.pendingCall)
        Unit
    }
}
