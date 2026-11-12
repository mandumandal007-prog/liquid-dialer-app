package com.mandu.liquiddialer
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class FullBackupTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val now get()=System.currentTimeMillis()
 @Before fun clean(){
  ctx.getSharedPreferences("call_reminders",0).edit().clear().commit()
  File(ctx.filesDir,"notes.json").delete();File(ctx.filesDir,"notes.json.bak").delete()
  File(ctx.filesDir,"manual_call_lists.json").delete();File(ctx.filesDir,"manual_call_lists.json.bak").delete()
  runBlocking{ctx.ldStore.edit{it.clear()}}
 }
 private fun prefixes():List<String> = runBlocking{ctx.ldStore.data.first()}[K.blockedPrefixes]?.split(";")?.filter{it.isNotBlank()} ?: emptyList()
 private fun populate(){
  Repo.addNote(ctx,"+91 98765 43210","Papa","dawa ki list")
  Repo.addNote(ctx,"+44 2025550123","","meeting notes")
  CallReminders.set(ctx,"+91 98765 43210","Papa",now+3600000)
  val lists=CallLists.create(ctx,"Bharosa");val id=lists.last().id
  CallLists.add(ctx,id,"+91 98765 43210","Papa")
  val eid=CallLists.read(ctx).first{it.id==id}.entries.first().id
  CallLists.done(ctx,id,eid,true)
  runBlocking{ctx.ldStore.edit{it[K.blockedPrefixes]="+91140";it[K.theme]="midnight"}}
 }
 @Test fun exportParseRoundTripKeepsEverySection(){
  populate()
  val text=FullBackup.export(ctx)
  assertFalse(text.contains("token"))
  val plan=FullBackup.parse(text)
  assertEquals(2,plan.notes.size)
  assertEquals(1,plan.reminders.size);assertEquals("+91 98765 43210",plan.reminders[0].number)
  assertEquals(1,plan.lists.size);assertEquals("Bharosa",plan.lists[0].title)
  assertTrue(plan.lists[0].entries.single().done)
  assertEquals(listOf("+91140"),plan.blockedPrefixes)
  assertNotNull(plan.settings)
 }
 @Test fun applyMergesWithoutDeletingOrOverwritingExisting(){
  populate();val backup=FullBackup.export(ctx);clean()
  Repo.addNote(ctx,"+91 9000000000","Local","naya local note")
  val localLists=CallLists.create(ctx,"Bharosa");val localId=localLists.last().id
  CallLists.add(ctx,localId,"+91 9111111111","LocalEntry")
  runBlocking{ctx.ldStore.edit{it[K.blockedPrefixes]="+9199"}}
  val s=FullBackup.apply(ctx,FullBackup.parse(backup))
  assertEquals(2,s.notesAdded);assertEquals(0,s.notesKept)
  assertEquals(1,s.remindersSet);assertEquals(0,s.remindersSkipped)
  assertEquals(0,s.listsAdded);assertEquals(1,s.listsKept)
  assertEquals(1,s.prefixesAdded);assertTrue(s.settingsApplied)
  val notes=Repo.notesChecked(ctx)
  assertEquals(3,notes.size);assertTrue(notes.any{it.text=="naya local note"});assertTrue(notes.any{it.text=="dawa ki list"})
  val kept=CallLists.read(ctx).single{it.title=="Bharosa"}
  assertEquals(listOf("+91 9111111111"),kept.entries.map{it.number});assertFalse(kept.entries.first().done)
  assertEquals(1,CallReminders.all(ctx).size)
  assertEquals(listOf("+9199","+91140"),prefixes())
  assertEquals("midnight",runBlocking{ctx.ldStore.data.first()}[K.theme])
 }
 @Test fun applySkipsSameIdNotesAndPastReminders(){
  Repo.addNote(ctx,"+91 98765 43210","Papa","original text")
  val localId=Repo.notesChecked(ctx).single().id
  val past=now-86400000
  val backup="""{"kind":"liquid-dialer-full-backup","version":1,"createdAt":$now,"notes":[{"id":$localId,"number":"+91 98765 43210","name":"X","text":"overwritten?","time":$now}],"reminders":[{"number":"+91 98765 43210","name":"Old","at":$past}],"callLists":{"version":1,"lists":[]},"blockedPrefixes":[]}"""
  val s=FullBackup.apply(ctx,FullBackup.parse(backup))
  assertEquals(0,s.notesAdded);assertEquals(1,s.notesKept)
  assertEquals(0,s.remindersSet);assertEquals(1,s.remindersSkipped)
  assertEquals("original text",Repo.notesChecked(ctx).single().text)
  assertTrue(CallReminders.all(ctx).isEmpty())
 }
 @Test fun strictParseRejectsBadFilesWithoutTouchingData(){
  populate();val before=FullBackup.export(ctx)
  fun bad(mutate:(JSONObject)->JSONObject){val t=mutate(JSONObject(before)).toString();try{FullBackup.parse(t);fail("parse should reject")}catch(_:Exception){} }
  bad{it.put("kind","other-app")}
  bad{it.put("version",2)}
  bad{it.getJSONArray("notes").put(JSONObject(it.getJSONArray("notes").getJSONObject(0).toString()));it}
  bad{it.getJSONArray("reminders").put(JSONObject().put("number","not a number").put("name","x").put("at",now+1000));it}
  bad{it.getJSONArray("blockedPrefixes").put("+91+140");it}
  bad{it.getJSONObject("settings").getJSONObject("values").put("theme","rainbow");it}
  try{FullBackup.parse(before+"{}");fail("trailing garbage")}catch(_:Exception){}
  assertEquals(2,Repo.notesChecked(ctx).size);assertEquals(1,CallReminders.allRaw(ctx).size)
  assertEquals(1,CallLists.read(ctx).size);assertEquals(listOf("+91140"),prefixes())
 }
 @Test fun exportReadsStoredSettingsNotStaleUiSnapshot(){
  runBlocking{ctx.ldStore.edit{it[K.blockedPrefixes]="+9199";it[K.theme]="daylight"}}
  LdApp.settings.value=LdSettings(theme="midnight",blockedPrefixes=listOf("+91140"))
  val plan=FullBackup.parse(FullBackup.export(ctx))
  assertEquals(listOf("+9199"),plan.blockedPrefixes)
  assertEquals("daylight",plan.settings!!["theme"])
 }
 @Test fun prefixMergeReadsTransactionAndPreservesStoredLocalPrefixes(){
  runBlocking{ctx.ldStore.edit{it[K.blockedPrefixes]="+9199"}}
  LdApp.settings.value=LdSettings(blockedPrefixes=listOf("+91140"))
  val plan=FullBackup.Plan(null,emptyList(),emptyList(),emptyList(),listOf("+91140"))
  val result=FullBackup.apply(ctx,plan)
  assertEquals(1,result.prefixesAdded);assertEquals(listOf("+9199","+91140"),prefixes())
  val repeated=FullBackup.apply(ctx,plan);assertEquals(0,repeated.prefixesAdded);assertEquals(listOf("+9199","+91140"),prefixes())
 }

}
