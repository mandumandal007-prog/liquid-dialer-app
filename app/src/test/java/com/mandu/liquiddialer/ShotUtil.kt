package com.mandu.liquiddialer

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import java.io.File

fun saveShot(rule: AndroidComposeTestRule<*, out androidx.activity.ComponentActivity>, name: String) {
    rule.waitForIdle()
    val view = rule.activity.window.decorView
    val w = if (view.width > 0) view.width else 720
    val h = if (view.height > 0) view.height else 1600
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    rule.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
    val f = File((System.getenv("SHOT_DIR") ?: "build/shots") + "/$name.png")
    f.parentFile?.mkdirs()
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    println("SAVED $name ${bmp.width}x${bmp.height}")
}

fun saveDialogShot(name: String, bg: Long) {
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog() ?: error("no dialog shown")
    val view = dialog.window!!.decorView
    if (view.width <= 0 || view.height <= 0) {
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(700, android.view.View.MeasureSpec.AT_MOST),
            android.view.View.MeasureSpec.makeMeasureSpec(1500, android.view.View.MeasureSpec.AT_MOST)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
    val bmp = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    c.drawColor(bg.toInt())
    view.draw(c)
    val f = java.io.File((System.getenv("SHOT_DIR") ?: "build/shots") + "/$name.png")
    f.parentFile?.mkdirs()
    f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    println("SAVED $name ${bmp.width}x${bmp.height}")
}
