package com.mandu.liquiddialer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/** App-private, user-picked call backgrounds. Never changes the contact's avatar/provider. */
object CallPhotos {
    private fun prefs(c: Context) = c.getSharedPreferences("call_background_photos", Context.MODE_PRIVATE)
    private fun dir(c: Context) = File(c.filesDir, "call_backgrounds").apply { mkdirs() }
    fun get(c: Context, number: String): String? {
        val key = ContactControls.k(number).takeIf { it.isNotEmpty() } ?: return null
        val name = prefs(c).getString(key, null) ?: return null
        if (!name.matches(Regex("[a-f0-9-]+\\.jpg"))) return null
        return File(dir(c), name).takeIf { it.isFile }?.let { Uri.fromFile(it).toString() }
    }
    @Synchronized fun remove(c: Context, number: String) {
        val key = ContactControls.k(number); if (key.isEmpty()) return
        val old = get(c, number)
        if (prefs(c).edit().remove(key).commit()) old?.let { File(Uri.parse(it).path!!).delete() }
    }
    /** Run on IO. Sample decode to <=1280px, reject giant/corrupt inputs, keep old on failure. */
    @Synchronized fun save(c: Context, number: String, uri: Uri): Boolean {
        val key = ContactControls.k(number); if (key.isEmpty()) return false
        var output: File? = null
        return try {
            val opt = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opt) }
            if (opt.outWidth <= 0 || opt.outHeight <= 0 || opt.outWidth.toLong()*opt.outHeight > 160_000_000L) return false
            opt.inJustDecodeBounds = false
            var sample = 1
            while (maxOf(opt.outWidth, opt.outHeight) / sample > 1280) sample *= 2
            opt.inSampleSize = sample
            val decoded = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opt) } ?: return false
            val orientation = try { c.contentResolver.openInputStream(uri)?.use { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,1) } ?: 1 } catch (_: Exception) { 1 }
            val matrix = android.graphics.Matrix().apply {
                when(orientation) {
                    2 -> setScale(-1f,1f)
                    3 -> setRotate(180f)
                    4 -> setScale(1f,-1f)
                    5 -> { setRotate(90f); postScale(-1f,1f) }
                    6 -> setRotate(90f)
                    7 -> { setRotate(270f); postScale(-1f,1f) }
                    8 -> setRotate(270f)
                }
            }
            val bmp = if(orientation in 2..8) Bitmap.createBitmap(decoded,0,0,decoded.width,decoded.height,matrix,true).also { if(it !== decoded) decoded.recycle() } else decoded
            output = File(dir(c), UUID.randomUUID().toString()+".jpg")
            val ok = try { output.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) } } finally { bmp.recycle() }
            val old = get(c, number)
            if (!ok || !prefs(c).edit().putString(key, output.name).commit()) { output.delete(); false }
            else { old?.let { File(Uri.parse(it).path!!).delete() }; true }
        } catch (_: Exception) { output?.delete(); false }
    }
}
