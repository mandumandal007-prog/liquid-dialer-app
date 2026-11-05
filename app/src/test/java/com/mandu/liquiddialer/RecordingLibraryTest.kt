package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallManager
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.*
import java.nio.file.Files
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class RecordingLibraryTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val root get()=Repo.recordingsDir(ctx)
 @Before fun setup(){CallManager.recording.value=null;root.listFiles()?.forEach{it.delete()}}
 private fun add(name:String,text:String="audio",time:Long=1000):RecordingLibrary.Item {File(root,name).apply{writeText(text);setLastModified(time)};return RecordingLibrary.list(ctx).first{it.fileName==name}}
 private fun rejects(action:()->Unit){try{action();fail("must reject")}catch(_:Exception){}}
 @Test fun listSearchSortAndNonAudioFiltering(){
  add("Call_Riya_20261104_120000.m4a",time=1000);add("दवाई घर.m4a",time=2000)
  File(root,"other.txt").writeText("skip");File(root,"empty.m4a").writeBytes(byteArrayOf());File(root,"folder.m4a").mkdir()
  val all=RecordingLibrary.list(ctx);assertEquals(2,all.size);assertEquals("दवाई घर.m4a",all.first().fileName)
  assertEquals("Call_Riya_20261104_120000.m4a",RecordingLibrary.search(all,"riya").single().fileName)
  assertEquals("दवाई घर.m4a",RecordingLibrary.search(all,"दवाई").single().fileName)
 }
 @Test fun renameKeepsBytesDateAndNeverOverwrites(){
  val a=add("a.m4a","one");add("b.m4a","two")
  rejects{RecordingLibrary.rename(ctx,a,"b")};assertEquals("one",File(root,"a.m4a").readText());assertEquals("two",File(root,"b.m4a").readText())
  RecordingLibrary.rename(ctx,a,"Papa नई बात");val f=File(root,"Papa नई बात.m4a");assertEquals("one",f.readText());assertEquals(1000,f.lastModified());assertFalse(File(root,"a.m4a").exists())
 }
 @Test fun pathTraversalSymlinkAndInvalidNamesRejected(){
  val a=add("a.m4a");for(n in listOf("../outside","a/b","a\\b"," ","x\ny","x".repeat(101)))rejects{RecordingLibrary.rename(ctx,a,n)}
  rejects{RecordingLibrary.resolve(ctx,a.copy(fileName="../a.m4a"))}
  val outside=File(ctx.filesDir,"outside.m4a");outside.writeText("private")
  Files.createSymbolicLink(File(root,"link.m4a").toPath(),outside.toPath());assertFalse(RecordingLibrary.list(ctx).any{it.fileName=="link.m4a"})
  rejects{RecordingLibrary.resolve(ctx,a.copy(fileName="link.m4a",bytes=outside.length(),modified=outside.lastModified()))};assertEquals("private",outside.readText())
 }
 @Test fun activeAndChangedFilesCannotBeModifiedOrCopied(){
  val a=add("a.m4a");CallManager.recording.value=CallManager.RecInfo(File(root,"a.m4a"),1000,"MIC")
  rejects{RecordingLibrary.rename(ctx,a,"new")};rejects{RecordingLibrary.delete(ctx,a)};rejects{RecordingLibrary.copy(ctx,a,ByteArrayOutputStream())}
  CallManager.recording.value=null;File(root,"a.m4a").appendText("changed")
  rejects{RecordingLibrary.delete(ctx,a)};rejects{RecordingLibrary.copy(ctx,a,ByteArrayOutputStream())};assertTrue(File(root,"a.m4a").exists())
 }
 @Test fun copyPreservesOriginalAndDeleteOnlyRequestedFile(){
  val a=add("a.m4a","one");add("b.m4a","two");val out=ByteArrayOutputStream();RecordingLibrary.copy(ctx,a,out);assertEquals("one",out.toString("UTF-8"));assertEquals("one",File(root,"a.m4a").readText())
  RecordingLibrary.delete(ctx,a);assertFalse(File(root,"a.m4a").exists());assertEquals("two",File(root,"b.m4a").readText());rejects{RecordingLibrary.delete(ctx,a)}
 }
}
