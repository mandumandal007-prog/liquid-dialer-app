package com.mandu.liquiddialer.ui

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** One bounded CPU blur per image/level, not a per-frame render effect. Android 8+. */
internal fun blurCallBitmap(source: Bitmap, level: Int): Bitmap {
    if (level<=0) return source
    val factor=minOf(1f,480f/maxOf(source.width,source.height))
    val w=(source.width*factor).roundToInt().coerceAtLeast(1)
    val h=(source.height*factor).roundToInt().coerceAtLeast(1)
    val scaled=Bitmap.createScaledBitmap(source,w,h,true)
    var src=IntArray(w*h);scaled.getPixels(src,0,w,0,0,w,h)
    if(scaled!==source) scaled.recycle()
    val radius=if(level==1) 3 else 7
    val n=radius*2+1
    repeat(3) {
        val temp=IntArray(w*h)
        for(y in 0 until h) {
            var r=0;var g=0;var b=0
            for(dx in -radius..radius){val c=src[y*w+dx.coerceIn(0,w-1)];r+=(c ushr 16) and 255;g+=(c ushr 8) and 255;b+=c and 255}
            for(x in 0 until w) {
                temp[y*w+x]=(255 shl 24) or ((r/n) shl 16) or ((g/n) shl 8) or (b/n)
                val old=src[y*w+(x-radius).coerceIn(0,w-1)];val next=src[y*w+(x+radius+1).coerceIn(0,w-1)]
                r+=((next ushr 16) and 255)-((old ushr 16) and 255);g+=((next ushr 8) and 255)-((old ushr 8) and 255);b+=(next and 255)-(old and 255)
            }
        }
        val out=IntArray(w*h)
        for(x in 0 until w) {
            var r=0;var g=0;var b=0
            for(dy in -radius..radius){val c=temp[dy.coerceIn(0,h-1)*w+x];r+=(c ushr 16) and 255;g+=(c ushr 8) and 255;b+=c and 255}
            for(y in 0 until h) {
                out[y*w+x]=(255 shl 24) or ((r/n) shl 16) or ((g/n) shl 8) or (b/n)
                val old=temp[(y-radius).coerceIn(0,h-1)*w+x];val next=temp[(y+radius+1).coerceIn(0,h-1)*w+x]
                r+=((next ushr 16) and 255)-((old ushr 16) and 255);g+=((next ushr 8) and 255)-((old ushr 8) and 255);b+=(next and 255)-(old and 255)
            }
        }
        src=out
    }
    return Bitmap.createBitmap(src,w,h,Bitmap.Config.ARGB_8888)
}

@Composable
internal fun rememberCallPhoto(uri:String?,level:Int):ImageBitmap? {
    val original=key(uri) { rememberPhoto(uri) }
    val result by key(uri,original,level) { produceState<ImageBitmap?>(null,original,level) {
        value=if(original==null || level==0) original else withContext(Dispatchers.Default) { blurCallBitmap(original.asAndroidBitmap(),level).asImageBitmap() }
    }
    }
    return if(level==0) original else result
}
