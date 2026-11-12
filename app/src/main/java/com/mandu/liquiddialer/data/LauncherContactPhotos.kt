package com.mandu.liquiddialer.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** Small launcher snapshots. Never persists a photo URI, network URL or full-size image. */
object LauncherContactPhotos {
 const val MAX_BYTES=512*1024
 data class Result(val bitmap:Bitmap,val photo:Boolean,val detail:String)
 fun icon(ctx:Context,target:LauncherContacts.Target,photos:Boolean):Result {
  fun fallback(why:String)=Result(DialShortcuts.initialIcon(target.name),false,why)
  if(!photos)return fallback("Initial icon")
  if(ContextCompat.checkSelfPermission(ctx,Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)return fallback("Contacts permission needed; using initial")
  return try {
   target.validate()
   val uri=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(target.number))
   val matches=linkedMapOf<Long,String?>();var seen=0
   ctx.contentResolver.query(uri,arrayOf(ContactsContract.PhoneLookup._ID,ContactsContract.PhoneLookup.NUMBER,ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI),null,null,null)?.use{c->
    while(c.moveToNext()){
     if(++seen>32)return fallback("Too many matches; using initial")
     val number=c.getString(1)?:continue
     // PhoneLookup may return suffix matches. Never accept those as an identity.
     if(runCatching{LauncherContacts.Target("Photo",number).validate()}.isFailure||ContactControls.k(number)!=ContactControls.k(target.number))continue
     val id=c.getLong(0);if(id<=0)continue
     val photo=c.getString(2)
     if(matches.containsKey(id)&&matches[id]!=photo)return fallback("Conflicting photos; using initial")
     matches[id]=photo
    }
   }?:return fallback("Contacts unavailable; using initial")
   if(matches.size!=1)return fallback(if(matches.isEmpty())"No exact saved contact; using initial"else"Number shared by contacts; using initial")
   val photo=matches.values.single()?.let{Uri.parse(it)}?:return fallback("No contact photo; using initial")
   if(photo.scheme!="content"||photo.authority!=ContactsContract.AUTHORITY)return fallback("Unsupported photo; using initial")
   val bytes=ctx.contentResolver.openInputStream(photo)?.use{input->val out=java.io.ByteArrayOutputStream();val b=ByteArray(4096);while(true){val n=input.read(b);if(n<0)break;if(out.size()+n>MAX_BYTES)return fallback("Photo too large; using initial");out.write(b,0,n)};out.toByteArray()}?:return fallback("Photo unavailable; using initial")
   val bitmap=decode(bytes)?:return fallback("Photo unreadable; using initial")
   Result(bitmap,true,"Contact photo snapshot")
  }catch(_:Exception){fallback("Photo unavailable; using initial")}
 }
 internal fun decode(bytes:ByteArray):Bitmap? {
  if(bytes.isEmpty()||bytes.size>MAX_BYTES)return null
  val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
  if(bounds.outWidth !in 1..2048||bounds.outHeight !in 1..2048)return null
  var sample=1;while(minOf(bounds.outWidth,bounds.outHeight)/sample>216)sample*=2
  val source=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample})?:return null
  try {
   val size=108;val result=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);val canvas=Canvas(result)
   val shader=BitmapShader(source,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);val scale=size.toFloat()/minOf(source.width,source.height)
   val matrix=Matrix();matrix.setScale(scale,scale);matrix.postTranslate((size-source.width*scale)/2,(size-source.height*scale)/2);shader.setLocalMatrix(matrix)
   canvas.drawCircle(size/2f,size/2f,size/2f,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{this.shader=shader})
   return result
  }finally{source.recycle()}
 }
}
