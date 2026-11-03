package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.CallLists
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Executors
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class CallListsTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @Before fun clean(){listOf("",".bak",".new").forEach {File(ctx.filesDir,"manual_call_lists.json$it").delete()}}
 private fun fails(f:()->Unit){try {f();fail("must reject")}catch(_:IllegalArgumentException){}}
 @Test fun independentListsPersistProgressRenameRemoveAndResume() {
  val a=CallLists.create(ctx," Family ").single().id
  val b=CallLists.create(ctx,"Work").last().id
  val e=CallLists.add(ctx,a,"9876543210","Papa").first().entries.single()
  CallLists.add(ctx,b,"+44 9876543210","Office")
  CallLists.done(ctx,a,e.id,true);assertTrue(CallLists.read(ctx).first().entries.single().done)
  CallLists.rename(ctx,a,"Weekend");assertEquals("Weekend",CallLists.read(ctx).first().title)
  CallLists.done(ctx,a,e.id,false);assertFalse(CallLists.read(ctx).first().entries.single().done)
  CallLists.remove(ctx,a,e.id);assertTrue(CallLists.read(ctx).first().entries.isEmpty())
  CallLists.delete(ctx,a);assertEquals(b,CallLists.read(ctx).single().id)
 }
 @Test fun fullNumberIdentityAliasesInvalidAndServiceCodes() {
  val id=CallLists.create(ctx,"Test").single().id
  CallLists.add(ctx,id,"+1 2125551234","")
  fails{CallLists.add(ctx,id,"0012125551234","")}
  CallLists.add(ctx,id,"+44 2125551234","")
  CallLists.add(ctx,id,"9876543210","");fails{CallLists.add(ctx,id,"+919876543210","")}
  listOf("", "private", "*123#", "123;456", "1+234", "++123", "12").forEach {n->fails{CallLists.add(ctx,id,n,"")}}
  assertEquals(3,CallLists.read(ctx).single().entries.size)
 }
 @Test fun corruptUnknownVersionDataNeverOverwritten() {
  val f=File(ctx.filesDir,"manual_call_lists.json")
  listOf("broken","{\"version\":99,\"lists\":[]}").forEach {text ->
   f.writeText(text);try{CallLists.create(ctx,"X");fail()}catch(_:Exception){}
   assertEquals(text,f.readText())
  }
 }
 @Test fun concurrentAddsKeepEveryEntryAndStaleMutationsFail() {
  val id=CallLists.create(ctx,"Work").single().id
  val pool=Executors.newFixedThreadPool(4)
  try {val fs=(100..119).map {n->pool.submit {CallLists.add(ctx,id,n.toString(),"Entry$n")}};fs.forEach {it.get()}}finally{pool.shutdown()}
  assertEquals(20,CallLists.read(ctx).single().entries.size)
  fails{CallLists.done(ctx,id,"missing",true)}
  CallLists.delete(ctx,id);fails{CallLists.add(ctx,id,"123","")};assertTrue(CallLists.read(ctx).isEmpty())
 }
 @Test fun boundedListsAndEntriesRejectWithoutLosingData() {
  fails{CallLists.create(ctx," ")};fails{CallLists.create(ctx,"x".repeat(61))}
  val id=CallLists.create(ctx,"Main").single().id
  repeat(200){CallLists.add(ctx,id,(1000+it).toString(),"")}
  fails{CallLists.add(ctx,id,"9000","")};assertEquals(200,CallLists.read(ctx).single().entries.size)
  repeat(19){CallLists.create(ctx,"List $it")};fails{CallLists.create(ctx,"Extra")};assertEquals(20,CallLists.read(ctx).size)
 }
}
