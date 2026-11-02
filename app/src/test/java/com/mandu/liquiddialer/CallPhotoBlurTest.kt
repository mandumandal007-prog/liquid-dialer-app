package com.mandu.liquiddialer
import android.graphics.Bitmap
import com.mandu.liquiddialer.ui.blurCallBitmap
import com.mandu.liquiddialer.data.*
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26])
class CallPhotoBlurTest {
 @Test fun clearIsExactAndBlurSoftensEdgesOnAndroid8() {
  val b=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
  for(y in 0 until 100)for(x in 0 until 100)b.setPixel(x,y,if(x<50)0xff000000.toInt() else 0xffffffff.toInt())
  assertSame(b,blurCallBitmap(b,0))
  val light=blurCallBitmap(b,1);val soft=blurCallBitmap(b,2)
  assertTrue((light.getPixel(48,50) and 255)>0)
  assertTrue((soft.getPixel(40,50) and 255)>(light.getPixel(40,50) and 255))
  assertEquals(0xff000000.toInt(),b.getPixel(48,50))
 }
 @Test fun blurMemoryIsBoundedAndSettingsPersistClamp() {
  val b=Bitmap.createBitmap(1000,1600,Bitmap.Config.ARGB_8888);b.eraseColor(0xff808080.toInt())
  val out=blurCallBitmap(b,1);assertTrue(maxOf(out.width,out.height)<=480);assertFalse(b.isRecycled)
  assertEquals(0,mutablePreferencesOf().toSettings().callPhotoBlur)
  assertEquals(1,mutablePreferencesOf(K.callPhotoBlur to 1).toSettings().callPhotoBlur)
  assertEquals(2,mutablePreferencesOf(K.callPhotoBlur to 99).toSettings().callPhotoBlur)
  assertEquals(0,mutablePreferencesOf(K.callPhotoBlur to -3).toSettings().callPhotoBlur)
 }
}
