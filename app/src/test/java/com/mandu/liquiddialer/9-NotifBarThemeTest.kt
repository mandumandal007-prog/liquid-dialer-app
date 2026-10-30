package com.mandu.liquiddialer

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Chronometer
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Adaptive text-color proof: same layout inflated under day vs night configuration. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotifBarThemeTest {

    private fun inflateThemed(night: Boolean, fontScale: Float = 1f): Pair<View, android.content.Context> {
        val base = ApplicationProvider.getApplicationContext<LdApp>()
        val cfg = Configuration(base.resources.configuration)
        cfg.uiMode = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        cfg.fontScale = fontScale
        val ctx = base.createConfigurationContext(cfg)
        val v = LayoutInflater.from(ctx).inflate(R.layout.notif_call_ongoing, null, false)
        return v to ctx
    }

    @Test fun adaptiveColors() {
        val (vd, _) = inflateThemed(night = false)
        val (vn, _) = inflateThemed(night = true)
        val dayTitle = vd.findViewById<TextView>(R.id.notif_call_name).currentTextColor
        val nightTitle = vn.findViewById<TextView>(R.id.notif_call_name).currentTextColor
        val daySub = vd.findViewById<TextView>(R.id.notif_call_status).currentTextColor
        val nightSub = vn.findViewById<TextView>(R.id.notif_call_status).currentTextColor
        println("THEME dayTitle=" + Integer.toHexString(dayTitle) + " nightTitle=" + Integer.toHexString(nightTitle))
        println("THEME daySub=" + Integer.toHexString(daySub) + " nightSub=" + Integer.toHexString(nightSub))
        fun lum(c: Int) = 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
        // day text must be dark, night text must be light (adaptive), or report exact values
        println("THEME dayLum=" + lum(dayTitle) + " nightLum=" + lum(nightTitle) + " daySubLum=" + lum(daySub) + " nightSubLum=" + lum(nightSub))
        assertTrue("day title dark hona chahiye, mila " + Integer.toHexString(dayTitle), lum(dayTitle) < 128)
        assertTrue("night title light hona chahiye, mila " + Integer.toHexString(nightTitle), lum(nightTitle) > 128)
        assertTrue("night status light hona chahiye, mila " + Integer.toHexString(nightSub), lum(nightSub) > 100)
    }

    private fun drawBar(v: View, ctx: android.content.Context, dark: Boolean, name: String, timer: String?, status: String?, out: String) {
        v.findViewById<TextView>(R.id.notif_call_name).text = name
        val st = v.findViewById<TextView>(R.id.notif_call_status)
        val tm = v.findViewById<Chronometer>(R.id.notif_call_timer)
        if (timer != null) { tm.visibility = View.VISIBLE; tm.text = timer; st.visibility = View.GONE }
        else { tm.visibility = View.GONE; st.visibility = View.VISIBLE; st.text = status }
        val w = 700
        val h = (48 * ctx.resources.displayMetrics.density).toInt()
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        val pad = 14; val header = 26
        val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2 + header, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(if (dark) Color.rgb(0x1B, 0x1C, 0x1E) else Color.rgb(0xF3, 0xF3, 0xF5))
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = if (dark) Color.rgb(0x2A, 0x2C, 0x30) else Color.WHITE }
        c.drawRoundRect(4f, 4f, (bmp.width - 4).toFloat(), (bmp.height - 4).toFloat(), 24f, 24f, paint)
        val hp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f; color = if (dark) Color.rgb(0xC8, 0xC8, 0xCC) else Color.rgb(0x5A, 0x5A, 0x5E) }
        c.drawText("Liquid Dialer \u00b7 now", pad.toFloat(), (pad + 12).toFloat(), hp)
        c.save(); c.translate(pad.toFloat(), (pad + header).toFloat()); v.draw(c); c.restore()
        val f = File((System.getenv("SHOT_DIR") ?: "build/shots") + "/$out.png")
        f.parentFile?.mkdirs()
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("SAVED $out ${bmp.width}x${bmp.height}")
    }

    @Test fun nightAndCollisionShots() {
        // night-inflated view on dark card: the real adaptive pairing
        val (vn, cn) = inflateThemed(night = true)
        drawBar(vn, cn, dark = true, name = "Railways", timer = "12:34", status = null, out = "19-notif-bar-night-real")
        // long name + long duration + 1.3x font: collision check against 3x48dp buttons
        val (vl, cl) = inflateThemed(night = false, fontScale = 1.3f)
        drawBar(vl, cl, dark = false, name = "Chandradev Mandal Railways Enquiry Office", timer = "12:34:56", status = null, out = "19-notif-bar-long-bigfont")
    }
}
