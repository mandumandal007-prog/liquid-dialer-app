package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CallChecklistTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val file get()=File(ctx.filesDir,"call_checklists.json")
 private val a="+44 2025550123";private val b="+1 2025550123"
 @Before fun clean(){android.util.AtomicFile(file).delete()}
 private fun rejects(f:()->Unit){try{f();fail("must reject")}catch(_:IllegalArgumentException){}}
 @Test fun exactInternationalIdentityAndIndianAliasesNeverSuffixMatch(){
  val x=CallChecklist.add(ctx,a,"Ask about order").single();CallChecklist.done(ctx,a,x.id,true)
  CallChecklist.add(ctx,b,"Other person");assertEquals(listOf("Other person"),CallChecklist.read(ctx,b).map{it.text});assertTrue(CallChecklist.read(ctx,"0044 2025550123").single().done)
  CallChecklist.add(ctx,"9876543210","India");assertEquals("India",CallChecklist.read(ctx,"+919876543210").single().text)
  assertTrue(CallChecklist.read(ctx,"02025550123").isEmpty())
 }
 @Test fun editTickResetAndRemovePersistWithoutChangingNotesOrContacts(){
  val notes=File(ctx.filesDir,"notes.json");val before=if(notes.exists())notes.readBytes()else null
  val x=CallChecklist.add(ctx,a," First topic ").single();CallChecklist.add(ctx,a,"घर 👪 multi\nline");CallChecklist.done(ctx,a,x.id,true)
  val id=CallChecklist.edit(ctx,a,x.id,"Edited").first();assertEquals(x.id,id.id);assertTrue(id.done);assertEquals("Edited",id.text)
  val reset=CallChecklist.reset(ctx,a);assertEquals(2,reset.size);assertTrue(reset.none{it.done});assertEquals(x.id,reset.first().id)
  CallChecklist.remove(ctx,a,x.id);assertEquals("घर 👪 multi\nline",CallChecklist.read(ctx,a).single().text)
  CallChecklist.clear(ctx,a);assertTrue(CallChecklist.read(ctx,a).isEmpty());if(before!=null)assertArrayEquals(before,notes.readBytes())else assertFalse(notes.exists())
 }
 @Test fun emptyOversizeInvalidNumbersAndStaleIdsRejectWithoutWrite(){
  CallChecklist.add(ctx,a,"Keep");val bytes=file.readBytes()
  for(n in listOf("","Private","*123#","+","++123","12"))rejects{CallChecklist.add(ctx,n,"New")}
  rejects{CallChecklist.add(ctx,a," ")};rejects{CallChecklist.add(ctx,a,"x".repeat(251))};rejects{CallChecklist.edit(ctx,a,"missing","New")};rejects{CallChecklist.done(ctx,a,"missing",true)};rejects{CallChecklist.remove(ctx,a,"missing")};assertArrayEquals(bytes,file.readBytes())
 }
 @Test fun itemLimitAndNumberCapacityNeverDropExistingData(){
  repeat(30){CallChecklist.add(ctx,a,"Topic $it")};var bytes=file.readBytes();rejects{CallChecklist.add(ctx,a,"31")};assertArrayEquals(bytes,file.readBytes())
  repeat(99){CallChecklist.add(ctx,"123$it","Other $it")};bytes=file.readBytes();rejects{CallChecklist.add(ctx,b,"101")};assertArrayEquals(bytes,file.readBytes());CallChecklist.clear(ctx,"1230");CallChecklist.add(ctx,b,"Space freed");assertEquals(30,CallChecklist.read(ctx,a).size)
 }
 @Test fun corruptUnknownVersionConcatenatedAndOversizeDataNeverOverwritten(){
  for(raw in listOf("broken","{\"version\":99,\"checklists\":[]}","{\"version\":1,\"checklists\":[]}{}","{\"version\":1,\"checklists\":[]}\u0000","x".repeat(1024*1024+1))){file.writeText(raw);try{CallChecklist.add(ctx,a,"New");fail()}catch(_:Exception){};assertEquals(raw,file.readText())}
 }
 @Test fun concurrentTicksAndAddsPreserveEveryTopicAndOtherNumber(){
  val other=CallChecklist.add(ctx,b,"Unchanged");val pool=Executors.newFixedThreadPool(4)
  try{(0..19).map{i->pool.submit{val item=CallChecklist.add(ctx,a,"Item $i").last();CallChecklist.done(ctx,a,item.id,true)}}.forEach{it.get()}}finally{pool.shutdown()}
  assertEquals(20,CallChecklist.read(ctx,a).size);assertTrue(CallChecklist.read(ctx,a).all{it.done});assertEquals(other,CallChecklist.read(ctx,b))
 }
 @Test fun atomicBackupRecoveryAndLastRemovalFreeNumber(){
  val x=CallChecklist.add(ctx,a,"Keep").single();File(file.path+".bak").writeBytes(file.readBytes());file.writeText("partial")
  assertEquals(x,CallChecklist.read(ctx,a).single());CallChecklist.remove(ctx,a,x.id);assertTrue(CallChecklist.read(ctx,a).isEmpty());assertTrue(file.readText().contains("\"checklists\":[]"))
 }
}
