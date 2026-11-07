package com.mandu.liquiddialer

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
class CallListTransferTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @Before fun clear(){listOf("",".bak",".new").forEach {File(ctx.filesDir,"manual_call_lists.json$it").delete()}}
 @Test fun bulkPreviewPreservesFullIdentityNamesAndSkipsOnlyExplicitDuplicates(){
  val p=CallListTransfer.batch("+1 2025550123,Home, work\n00442025550123\tघर 👪\n+44 2025550123,Duplicate\n\n9876543210,Papa",listOf("+919876543210"))
  assertEquals(listOf("+1 2025550123","00442025550123"),p.entries.map{it.number});assertEquals("Home, work",p.entries[0].name)
  assertEquals("घर 👪",p.entries[1].name);assertEquals(2,p.duplicateCount);assertTrue(p.invalidLines.isEmpty())
 }
 @Test fun invalidRowsPreventAnyWriteAndOversizeRejects(){
  val id=CallLists.create(ctx,"Work").single().id;val before=CallLists.read(ctx)
  val text="9876543210,Papa\n*123#,Code\n++442025550123,Wrong\n123,"+"n".repeat(101)
  assertEquals(listOf(2,3,4),CallListTransfer.batch(text).invalidLines)
  try{CallLists.addBatch(ctx,id,text);fail()}catch(_:IllegalArgumentException){}
  assertEquals(before,CallLists.read(ctx))
  for(s in listOf("9".repeat(65537),(1..501).joinToString("\n"){"123"}))try{CallListTransfer.batch(s);fail()}catch(_:IllegalArgumentException){}
 }
 @Test fun batchAddsAtomicallyAndRechecksExistingDataAtCommit(){
  val id=CallLists.create(ctx,"Work").single().id
  CallLists.add(ctx,id,"9876543210","Saved");val e=CallLists.read(ctx).single().entries.single();CallLists.done(ctx,id,e.id,true)
  val text="9876543210,Do not replace\n+442025550123,New"
  val preview=CallListTransfer.batch(text,CallLists.read(ctx).single().entries.map{it.number});assertEquals(1,preview.duplicateCount)
  CallLists.addBatch(ctx,id,text);val all=CallLists.read(ctx).single().entries
  assertEquals(2,all.size);assertEquals(e.id,all.first().id);assertTrue(all.first().done);assertEquals("Saved",all.first().name)
  assertFalse(all.last().done)
  try{CallLists.addBatch(ctx,id,text);fail()}catch(_:IllegalArgumentException){}
  assertEquals(all,CallLists.read(ctx).single().entries)
 }
 @Test fun backupCopiesPreserveProgressUnicodeAndNeverOverwriteOriginalIds(){
  val id=CallLists.create(ctx,"घर | Work").single().id;CallLists.add(ctx,id,"+442025550123","Office, A\nB")
  val e=CallLists.read(ctx).single().entries.single();CallLists.done(ctx,id,e.id,true)
  val original=CallLists.read(ctx);val text=CallListTransfer.backup(original)
  assertEquals(original,CallListTransfer.preview(text));val all=CallLists.importCopies(ctx,text)
  assertEquals(original.single(),all.first());assertNotEquals(id,all.last().id);assertNotEquals(e.id,all.last().entries.single().id)
  assertTrue(all.last().entries.single().done);assertEquals(e.name,all.last().entries.single().name)
 }
 @Test fun invalidBackupLimitAndCorruptionPreserveExisting(){
  val id=CallLists.create(ctx,"Keep").single().id;val original=CallLists.read(ctx)
  for(s in listOf("broken","{\"version\":2,\"lists\":[]}","{\"version\":1,\"lists\":[]}"))try{CallLists.importCopies(ctx,s);fail()}catch(_:Exception){}
  assertEquals(original,CallLists.read(ctx))
  val text=CallListTransfer.backup(original)
  repeat(19){CallLists.create(ctx,"More $it")}
  val full=CallLists.read(ctx);try{CallLists.importCopies(ctx,text);fail()}catch(_:IllegalArgumentException){};assertEquals(full,CallLists.read(ctx))
  try{CallListTransfer.read(ByteArray(1024*1024+1).inputStream());fail()}catch(_:IllegalArgumentException){}
  try{CallListTransfer.read(byteArrayOf(0xC3.toByte(),0x28).inputStream());fail()}catch(_:java.nio.charset.CharacterCodingException){}
 }
 @Test fun hostileTypesDuplicateIdsAndEntryLimitNeverPartiallyWrite(){
  val id=CallLists.create(ctx,"Keep").single().id;CallLists.add(ctx,id,"9876543210","Papa")
  val original=CallLists.read(ctx);val root=org.json.JSONObject(CallListTransfer.backup(original))
  val list=root.getJSONArray("lists").getJSONObject(0);val e=list.getJSONArray("entries").getJSONObject(0)
  val cases=listOf(root.toString().replace("\"version\":2","\"version\":\"2\""),
    root.toString().replace("\"done\":false","\"done\":\"false\""),
    root.toString().replace("\"number\":\"9876543210\"","\"number\":9876543210"))
  for(text in cases){try{CallLists.importCopies(ctx,text);fail("Accepted wrong type")}catch(_:Exception){};assertEquals(original,CallLists.read(ctx))}
  list.getJSONArray("entries").put(e)
  try{CallLists.importCopies(ctx,root.toString());fail("Accepted duplicate")}catch(_:Exception){}
  assertEquals(original,CallLists.read(ctx))
  val tooMany=(0..200).joinToString("\n"){"+1202555"+it.toString().padStart(4,'0')}
  try{CallLists.addBatch(ctx,id,tooMany);fail("Accepted too many")}catch(_:IllegalArgumentException){}
  assertEquals(original,CallLists.read(ctx))
 }

}
