package com.mandu.liquiddialer
import android.Manifest
import android.content.*
import android.database.*
import android.graphics.*
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherContactPhotosTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val target=LauncherContacts.Target("Father","+919876543210")
 private val photo="content://com.android.contacts/contacts/4/photo"
 private var rows=listOf(arrayOf<Any?>(4L,"+91 98765 43210",photo))
 private var queries=0;private var opens=0;private var denied=false
 private val image get()=File(ctx.cacheDir,"shortcut-test.png")
 @Before fun setup(){Shadows.shadowOf(ctx).grantPermissions(Manifest.permission.READ_CONTACTS);queries=0;opens=0;denied=false
  val bitmap=Bitmap.createBitmap(240,120,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.BLUE);Canvas(bitmap).drawRect(80f,0f,160f,120f,Paint().apply{color=Color.RED});image.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
  val provider=object:ContentProvider(){override fun onCreate()=true
   override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor {queries++;if(denied)throw SecurityException();assertTrue(u.toString().startsWith(ContactsContract.PhoneLookup.CONTENT_FILTER_URI.toString()));return MatrixCursor(p).apply{rows.forEach{addRow(it)}}}
   override fun openFile(u:Uri,mode:String):ParcelFileDescriptor{opens++;assertEquals(photo,u.toString());return ParcelFileDescriptor.open(image,ParcelFileDescriptor.MODE_READ_ONLY)}
   override fun getType(u:Uri):String?=null;override fun insert(u:Uri,v:ContentValues?):Uri?=null;override fun delete(u:Uri,s:String?,a:Array<out String>?)=0;override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0
  };provider.attachInfo(ctx,android.content.pm.ProviderInfo().apply{authority=ContactsContract.AUTHORITY});ShadowContentResolver.registerProviderInternal(ContactsContract.AUTHORITY,provider)
 }
 @Test fun offNeverQueriesAndOldConfigDefaultsOff(){assertFalse(LauncherContactPhotos.icon(ctx,target,false).photo);assertEquals(0,queries);val old="""{"version":1,"enabled":true,"targets":[{"name":"Father","number":"+919876543210"}]}""";assertFalse(LauncherContacts.decode(old).photos);val c=LauncherContacts.decode(old).copy(photos=true);assertEquals(c,LauncherContacts.decode(LauncherContacts.encode(c)))}
 @Test fun permissionDeniedNeverQueries(){Shadows.shadowOf(ctx).denyPermissions(Manifest.permission.READ_CONTACTS);assertFalse(LauncherContactPhotos.icon(ctx,target,true).photo);assertEquals(0,queries);assertEquals(0,opens)}
 @Test fun exactPhotoCroppedCircleAndNoStoredBytes(){val icon=LauncherContactPhotos.icon(ctx,target,true);assertTrue(icon.detail+" queries="+queries+" opens="+opens,icon.photo);assertEquals(108,icon.bitmap.width);assertEquals(108,icon.bitmap.height);assertEquals(Color.RED,icon.bitmap.getPixel(54,54));assertEquals(0,Color.alpha(icon.bitmap.getPixel(0,0)));assertEquals(1,opens);val raw=LauncherContacts.encode(LauncherContacts.Config(true,listOf(target),true));assertFalse(raw.contains("content:"));assertFalse(raw.contains("base64"))}
 @Test fun suffixAndForeignIdentityNeverReadPhoto(){listOf("+449876543210","1234567890","139").forEach{rows=listOf(arrayOf(4L,it,photo));assertFalse(LauncherContactPhotos.icon(ctx,target,true).photo)};assertEquals(0,opens)}
 @Test fun conflictingContactsAndPhotosFallback(){rows=listOf(arrayOf(4L,target.number,photo),arrayOf(5L,target.number,photo));assertFalse(LauncherContactPhotos.icon(ctx,target,true).photo);rows=listOf(arrayOf(4L,target.number,photo),arrayOf(4L,target.number,null));assertFalse(LauncherContactPhotos.icon(ctx,target,true).photo);assertEquals(0,opens)}
 @Test fun duplicateSameContactRowsStillExact(){rows=listOf(rows.first(),rows.first());val result=LauncherContactPhotos.icon(ctx,target,true);assertTrue(result.detail+" queries="+queries+" opens="+opens,result.photo)}
 @Test fun missingAndUnsafeUrisNeverOpen(){listOf(null,"https://example.com/photo.jpg","file:///private/photo.png","content://other.app/photo").forEach{rows=listOf(arrayOf(4L,target.number,it));assertFalse(LauncherContactPhotos.icon(ctx,target,true).photo)};assertEquals(0,opens)}
 @Test fun unavailableAndOversizedPhotosFallback(){image.writeText("bad image");assertFalse(LauncherContactPhotos.icon(ctx,target,true).photo);image.writeBytes(ByteArray(LauncherContactPhotos.MAX_BYTES+1));assertFalse(LauncherContactPhotos.icon(ctx,target,true).photo);denied=true;assertFalse(LauncherContactPhotos.icon(ctx,target,true).photo)}
 @Test fun oversizedDimensionsAndBytesReject(){val b=Bitmap.createBitmap(2049,1,Bitmap.Config.ARGB_8888);val out=java.io.ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,out);b.recycle();assertNull(LauncherContactPhotos.decode(out.toByteArray()));assertNull(LauncherContactPhotos.decode(ByteArray(LauncherContactPhotos.MAX_BYTES+1)))}
 @Test fun boundedProviderAndStrictOptionalType(){rows=List(33){arrayOf(4L,target.number,photo)};assertFalse(LauncherContactPhotos.icon(ctx,target,true).photo);assertEquals(0,opens);val raw=LauncherContacts.encode(LauncherContacts.Config());try{LauncherContacts.decode(raw.replace("\"photos\":false","\"photos\":1"));fail()}catch(_:Exception){}}
 @Test fun actualShortcutManagerRefreshReplacesPhotoWithInitial(){val manager=ctx.getSystemService(android.content.pm.ShortcutManager::class.java);manager.removeAllDynamicShortcuts();ctx.getSharedPreferences("launcher_contacts",0).edit().clear().commit();val saved=LauncherContacts.save(ctx,"",LauncherContacts.Config(true,listOf(target),true));LauncherContacts.publish(ctx);val first=manager.dynamicShortcuts.single{it.id==LauncherContacts.id(target)};assertEquals(LauncherContacts.ACTION,first.intent!!.action);assertNull(first.intent!!.getStringExtra("number"));assertEquals("Father",first.shortLabel.toString());LauncherContacts.save(ctx,saved.raw,saved.config.copy(photos=false));val before=queries;LauncherContacts.publish(ctx);assertEquals(before,queries);assertEquals(first.id,manager.dynamicShortcuts.single{it.id==first.id}.id)}
}
