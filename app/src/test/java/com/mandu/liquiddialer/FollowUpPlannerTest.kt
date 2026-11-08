package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.TimeZone
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class FollowUpPlannerTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @Before fun clean(){listOf("",".bak",".new").forEach{File(ctx.filesDir,"manual_call_lists.json$it").delete()}}
 @Test fun explicitDatePersistsAndDoesNotTouchOutcomeNoteOrOtherEntries(){
  val id=CallLists.create(ctx,"Work").single().id
  CallLists.add(ctx,id,"9876543210","Papa");CallLists.add(ctx,id,"+442025550123","Office")
  val e=CallLists.read(ctx).single().entries.first();CallLists.followUp(ctx,id,e.id,"later","Lunch",123)
  val before=CallLists.read(ctx).single();CallLists.schedule(ctx,id,e.id,2000000)
  val after=CallLists.read(ctx).single();assertEquals(before.entries.first().copy(dueAt=2000000),after.entries.first());assertEquals(before.entries.last(),after.entries.last())
  assertTrue(CallReminders.allRaw(ctx).isEmpty())
  val text=CallListTransfer.backup(listOf(after));assertEquals(listOf(after),CallListTransfer.preview(text))
  assertEquals(2000000,FullBackup.parse(FullBackup.export(ctx)).lists.single().entries.first().dueAt)
  CallLists.schedule(ctx,id,e.id,0);assertEquals(before,CallLists.read(ctx).single())
  val bytes=File(ctx.filesDir,"manual_call_lists.json").readBytes()
  var rejected=false;try{CallLists.schedule(ctx,id,e.id,-1)}catch(_:Exception){rejected=true};assertTrue(rejected);assertArrayEquals(bytes,File(ctx.filesDir,"manual_call_lists.json").readBytes())
 }
 @Test fun crossListViewsRespectLocalDayDoneAndUnscheduled(){
  val now=java.time.Instant.parse("2026-11-08T06:00:00Z").toEpochMilli();val zone=TimeZone.getTimeZone("Asia/Kolkata")
  val entries=listOf(CallLists.Entry("old","123","Old",dueAt=now-86400000),CallLists.Entry("today","124","Today",dueAt=now+1000),CallLists.Entry("future","125","Future",dueAt=now+86400000),CallLists.Entry("none","126","None"),CallLists.Entry("done","127","Done",done=true,dueAt=now))
  val lists=listOf(CallLists.ListItem("a","Work",entries.take(2)),CallLists.ListItem("b","Home",entries.drop(2)))
  fun ids(v:FollowUpPlanner.View)=FollowUpPlanner.find(lists,v,now=now,zone=zone).map{it.entry.id}
  assertEquals(listOf("today"),ids(FollowUpPlanner.View.TODAY));assertEquals(listOf("old"),ids(FollowUpPlanner.View.OVERDUE));assertEquals(listOf("future"),ids(FollowUpPlanner.View.UPCOMING));assertEquals(listOf("none"),ids(FollowUpPlanner.View.UNSCHEDULED));assertEquals(4,ids(FollowUpPlanner.View.ALL).size)
  assertEquals(listOf("future","none"),FollowUpPlanner.find(lists,FollowUpPlanner.View.ALL,"Home",now=now,zone=zone).map{it.entry.id})
 }
 @Test fun oldV1AndV2NeverAcquireDatesOrWriteDuringRead(){
  val f=File(ctx.filesDir,"manual_call_lists.json")
  for(version in 1..2){
   val text="""{"version":$version,"lists":[{"id":"l","title":"Old","entries":[{"id":"e","number":"123","name":"Test","done":false,"outcome":"later","note":"x","updatedAt":123}]}]}"""
   f.writeText(text);assertEquals(0,CallLists.read(ctx).single().entries.single().dueAt);assertEquals(text,f.readText())
  }
 }
 @Test fun malformedV3DatesNeverReplaceSavedLists(){
  val id=CallLists.create(ctx,"Keep").single().id;CallLists.add(ctx,id,"123456","Saved")
  val e=CallLists.read(ctx).single().entries.single();CallLists.schedule(ctx,id,e.id,2000000)
  val file=File(ctx.filesDir,"manual_call_lists.json");val bytes=file.readBytes()
  for(value in listOf<Any>(-1,"2000000",true,1.5,org.json.JSONObject.NULL)){
   val root=org.json.JSONObject(bytes.toString(Charsets.UTF_8))
   root.getJSONArray("lists").getJSONObject(0).getJSONArray("entries").getJSONObject(0).put("dueAt",value)
   var rejected=false;try{CallLists.importCopies(ctx,root.toString())}catch(_:Exception){rejected=true}
   assertTrue("Accepted dueAt=$value",rejected);assertArrayEquals(bytes,file.readBytes())
  }
  val root=org.json.JSONObject(bytes.toString(Charsets.UTF_8));root.getJSONArray("lists").getJSONObject(0).getJSONArray("entries").getJSONObject(0).remove("dueAt")
  var rejected=false;try{CallLists.importCopies(ctx,root.toString())}catch(_:Exception){rejected=true}
  assertTrue(rejected);assertArrayEquals(bytes,file.readBytes())
 }

}
