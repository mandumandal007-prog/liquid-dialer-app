package com.mandu.liquiddialer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class CallPhotosTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private fun image(w:Int=100,h:Int=100):Uri {
        val f=File(ctx.cacheDir,"picked-${System.nanoTime()}.png")
        val bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bmp.eraseColor(android.graphics.Color.BLUE)
        f.outputStream().use { assertTrue(bmp.compress(Bitmap.CompressFormat.PNG,100,it)) };bmp.recycle()
        return Uri.fromFile(f)
    }
    @After fun clean(){ctx.getSharedPreferences("call_background_photos",0).edit().clear().commit();File(ctx.filesDir,"call_backgrounds").deleteRecursively()}
    @Test fun savePersistsSampledPrivateCopyWithoutDependingOnPickedSource() {
        val source=image(3000,1500)
        assertTrue(CallPhotos.save(ctx,"+12025559876",source))
        File(source.path!!).delete()
        val result=CallPhotos.get(ctx,"+12025559876")!!
        assertTrue(result.contains("/files/call_backgrounds/"))
        val bmp=BitmapFactory.decodeFile(Uri.parse(result).path)
        assertTrue(maxOf(bmp.width,bmp.height)<=1280);assertTrue(bmp.width>bmp.height);bmp.recycle()
        assertEquals(result,CallPhotos.get(ctx,"0012025559876"))
    }
    @Test fun differentForeignSuffixesRemainSeparateAndRemoveOnlyTarget() {
        val source=image()
        assertTrue(CallPhotos.save(ctx,"+12025559876",source));assertTrue(CallPhotos.save(ctx,"+442025559876",source))
        val uk=CallPhotos.get(ctx,"+442025559876");val us=CallPhotos.get(ctx,"+12025559876")!!
        CallPhotos.remove(ctx,"+12025559876")
        assertNull(CallPhotos.get(ctx,"+12025559876"));assertFalse(File(Uri.parse(us).path!!).exists())
        assertEquals(uk,CallPhotos.get(ctx,"+442025559876"))
    }
    @Test fun badReplacementKeepsOldAndSuccessfulReplacementDeletesOld() {
        assertTrue(CallPhotos.save(ctx,"9876543210",image()));val old=CallPhotos.get(ctx,"+919876543210")!!
        val bad=File(ctx.cacheDir,"bad-photo").apply {writeText("not an image")}
        assertFalse(CallPhotos.save(ctx,"9876543210",Uri.fromFile(bad)));assertEquals(old,CallPhotos.get(ctx,"9876543210"))
        assertTrue(CallPhotos.save(ctx,"9876543210",image()));assertNotEquals(old,CallPhotos.get(ctx,"9876543210"));assertFalse(File(Uri.parse(old).path!!).exists())
    }
    @Test fun invalidIdentityCannotStoreAndMissingFileFallsBack() {
        assertFalse(CallPhotos.save(ctx,"private",image()));assertNull(CallPhotos.get(ctx,"private"))
        assertTrue(CallPhotos.save(ctx,"123",image()));File(Uri.parse(CallPhotos.get(ctx,"123")).path!!).delete()
        assertNull(CallPhotos.get(ctx,"123"))
    }
    @Test fun photosAndLocksNeverAppearInAppearanceBackup() {
        assertTrue(CallPhotos.save(ctx,"9876543210",image()))
        val text=com.mandu.liquiddialer.ui.SettingsBackup.buildJson(LdSettings(glassDepth=0,glassFrost=2))
        assertFalse(text.contains("call_background"));assertFalse(text.contains("9876543210"));assertFalse(text.contains("appLock"))
        val ok=com.mandu.liquiddialer.ui.SettingsBackup.parse(text) as com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Ok
        assertEquals(0,ok.values["glassDepth"]);assertEquals(2,ok.values["glassFrost"])
    }
    @Test fun exifRotationAndReflectionAreAppliedBeforePrivateStorage() {
        for(orientation in listOf(6,8,5,7)) {
            val f=File(ctx.cacheDir,"rotated-$orientation.jpg")
            val b=Bitmap.createBitmap(160,80,Bitmap.Config.ARGB_8888);b.eraseColor(android.graphics.Color.RED)
            f.outputStream().use {b.compress(Bitmap.CompressFormat.JPEG,95,it)};b.recycle()
            android.media.ExifInterface(f.path).apply {setAttribute(android.media.ExifInterface.TAG_ORIENTATION,orientation.toString());saveAttributes()}
            assertTrue(CallPhotos.save(ctx,"9876543210",Uri.fromFile(f)))
            val decoded=BitmapFactory.decodeFile(Uri.parse(CallPhotos.get(ctx,"9876543210")).path)
            assertEquals(80,decoded.width);assertEquals(160,decoded.height);decoded.recycle()
        }
    }

}
