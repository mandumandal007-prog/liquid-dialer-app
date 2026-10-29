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
    view.draw(android.graphics.Canvas(bmp))
    val f = File((System.getenv("SHOT_DIR") ?: "build/shots") + "/$name.png")
    f.parentFile?.mkdirs()
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    println("SAVED $name ${bmp.width}x${bmp.height}")
}
