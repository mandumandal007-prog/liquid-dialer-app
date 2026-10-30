package com.mandu.liquiddialer

import android.app.Notification
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.View
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.telecom.CallNotifier
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

/** Ongoing-call notification bar: real CallNotifier.update() build + applied RemoteViews pixels.
 *  Single test method: Robolectric sandbox reuse makes cross-method shadow state unreliable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotifBarShotTest {

    @After fun reset() {
        CallManager.calls.value = emptyList()
        CallManager.audio.value = null
    }

    private var seq = 0

    private fun posted(state: Int, name: String, muted: Boolean, route: Int, connect: Long): Notification {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        CallNotifier.createChannels(ctx)
        seq += 1
        CallManager.audio.value = CallAudioState(muted, route, CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        CallManager.calls.value = listOf(FakeCall.make("+91 11234 56$seq", name, state, connectMillis = connect))
        CallManager.bump()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        CallNotifier.update(ctx)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val nm = Shadows.shadowOf(ctx.getSystemService(NotificationManager::class.java))
        val n = nm.allNotifications.filterNotNull().last()
        assertEquals("posted bar ka naam $name hona chahiye", name, n.extras.getCharSequence("android.title").toString())
        return n
    }

    private fun applied(n: Notification): View {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        return n.contentView.apply(ctx, null)
    }

    private fun shot(n: Notification, dark: Boolean, out: String) {
        val ctx = ApplicationProvider.getApplicationContext<LdApp>()
        val v = applied(n)
        val w = 700
        val h = (48 * ctx.resources.displayMetrics.density).toInt()
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        val pad = 14
        val header = 26
        val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2 + header, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(if (dark) Color.rgb(0x1B, 0x1C, 0x1E) else Color.rgb(0xF3, 0xF3, 0xF5))
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dark) Color.rgb(0x2A, 0x2C, 0x30) else Color.WHITE
        }
        c.drawRoundRect(4f, 4f, (bmp.width - 4).toFloat(), (bmp.height - 4).toFloat(), 24f, 24f, paint)
        val hp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f; color = if (dark) Color.rgb(0xC8, 0xC8, 0xCC) else Color.rgb(0x5A, 0x5A, 0x5E)
        }
        c.drawText("Liquid Dialer \u00b7 now", pad.toFloat(), (pad + 12).toFloat(), hp)
        c.save(); c.translate(pad.toFloat(), (pad + header).toFloat()); v.draw(c); c.restore()
        val f = File((System.getenv("SHOT_DIR") ?: "build/shots") + "/$out.png")
        f.parentFile?.mkdirs()
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("SAVED $out ${bmp.width}x${bmp.height}")
    }

    @Test fun barStructureAndPixels() {
        val connect = System.currentTimeMillis() - 12_000

        // 1) ACTIVE: structure + timer mode
        val n1 = posted(Call.STATE_ACTIVE, "Railways", muted = false, route = CallAudioState.ROUTE_EARPIECE, connect = connect)
        assertEquals(CallNotifier.CH_ONGOING, n1.channelId)
        assertNotNull("custom content view set hona chahiye", n1.contentView)
        assertNotNull("big content view bhi set hona chahiye (expanded = same bar)", n1.bigContentView)
        assertTrue("koi text action button nahi (duplicate text avoid)", n1.actions == null || n1.actions.isEmpty())
        val v1 = applied(n1)
        assertEquals("Railways", v1.findViewById<TextView>(R.id.notif_call_name).text.toString())
        assertEquals(View.VISIBLE, v1.findViewById<Chronometer>(R.id.notif_call_timer).visibility)
        assertEquals(View.GONE, v1.findViewById<TextView>(R.id.notif_call_status).visibility)
        assertEquals("Mute", v1.findViewById<ImageButton>(R.id.notif_btn_mute).contentDescription.toString())
        assertEquals("Speaker", v1.findViewById<ImageButton>(R.id.notif_btn_speaker).contentDescription.toString())
        assertEquals("Call kaato", v1.findViewById<ImageButton>(R.id.notif_btn_end).contentDescription.toString())
        shot(n1, dark = false, out = "18-notif-bar-active-light")
        shot(n1, dark = true, out = "18-notif-bar-active-dark")

        // 2) HOLDING + toggles: status text, active bg labels
        val n2 = posted(Call.STATE_HOLDING, "Mummy ji", muted = true, route = CallAudioState.ROUTE_SPEAKER, connect = 0)
        val v2 = applied(n2)
        assertEquals(View.GONE, v2.findViewById<Chronometer>(R.id.notif_call_timer).visibility)
        assertEquals(View.VISIBLE, v2.findViewById<TextView>(R.id.notif_call_status).visibility)
        assertEquals("On hold", v2.findViewById<TextView>(R.id.notif_call_status).text.toString())
        assertEquals("Unmute", v2.findViewById<ImageButton>(R.id.notif_btn_mute).contentDescription.toString())
        assertEquals("Speaker off", v2.findViewById<ImageButton>(R.id.notif_btn_speaker).contentDescription.toString())
        shot(n2, dark = false, out = "18-notif-bar-toggled")

        // 3) DIALING
        val n3 = posted(Call.STATE_DIALING, "Bhaiya", muted = false, route = CallAudioState.ROUTE_EARPIECE, connect = 0)
        val v3 = applied(n3)
        assertEquals("Calling\u2026", v3.findViewById<TextView>(R.id.notif_call_status).text.toString())
        assertEquals(View.GONE, v3.findViewById<Chronometer>(R.id.notif_call_timer).visibility)
        shot(n3, dark = false, out = "18-notif-bar-dialing")
    }
}
