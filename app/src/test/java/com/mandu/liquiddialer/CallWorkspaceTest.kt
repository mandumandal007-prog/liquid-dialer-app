package com.mandu.liquiddialer

import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class CallWorkspaceTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    @Before fun clear(){listOf("",".bak",".new").forEach {File(ctx.filesDir,"manual_call_lists.json$it").delete()}}
    private fun seed():Pair<String,String> {
        val id=CallLists.create(ctx,"Work").single().id
        val eid=CallLists.add(ctx,id,"+442025550123","Office").single().entries.single().id
        return id to eid
    }
    @Test fun legacyListsOpenWithoutWritingAndMigrateOnlyOnExplicitSave() {
        val text="""{"version":1,"lists":[{"id":"old","title":"Family","entries":[{"id":"a","number":"9876543210","name":"Papa","done":true},{"id":"b","number":"+442025550123","name":"Office","done":false}]}]}"""
        val f=File(ctx.filesDir,"manual_call_lists.json");f.writeText(text)
        val old=CallLists.read(ctx).single();assertEquals(listOf("done","pending"),old.entries.map {it.outcome})
        assertEquals(text,f.readText());assertTrue(old.entries.all {it.note.isEmpty() && it.updatedAt==0L})
        CallLists.followUp(ctx,"old","b","busy","Kal baat karni hai",123L)
        val next=CallLists.read(ctx).single();assertEquals(old.entries.first(),next.entries.first())
        assertEquals("busy",next.entries.last().outcome);assertFalse(next.entries.last().done)
        assertEquals(3,JSONObject(f.readText()).getInt("version"))
    }
    @Test fun everyOutcomeAndNotesPersistWithoutChangingIdentityOrOtherEntries() {
        val (id,eid)=seed();CallLists.add(ctx,id,"+12025550123","Other")
        val other=CallLists.read(ctx).single().entries.last()
        for(outcome in CallLists.outcomes.keys) {
            CallLists.followUp(ctx,id,eid,outcome,"घर 👪, next\nline",456L)
            val list=CallLists.read(ctx).single();val e=list.entries.first()
            assertEquals(outcome,e.outcome);assertEquals(outcome=="done",e.done)
            assertEquals("घर 👪, next\nline",e.note);assertEquals(456L,e.updatedAt)
            assertEquals("+442025550123",e.number);assertEquals(other,list.entries.last())
        }
        CallLists.done(ctx,id,eid,false);val e=CallLists.read(ctx).single().entries.first()
        assertEquals("pending",e.outcome);assertEquals("घर 👪, next\nline",e.note)
    }
    @Test fun badAndStaleUpdatesNeverOverwriteSavedData() {
        val(id,eid)=seed();val before=CallLists.read(ctx)
        for(action in listOf<()->Unit>({CallLists.followUp(ctx,id,eid,"fake","")},{CallLists.followUp(ctx,id,eid,"done","x".repeat(2001))},{CallLists.followUp(ctx,id,"missing","done","")},{CallLists.followUp(ctx,id,eid,"done","",-1)})) {
            try{action();fail()}catch(_:IllegalArgumentException){};assertEquals(before,CallLists.read(ctx))
        }
    }
    @Test fun backupsImportFreshCopiesWithAllWorkspaceFieldsAndRejectInvalidCombinations() {
        val(id,eid)=seed();CallLists.followUp(ctx,id,eid,"later","Next Tuesday",789L)
        val original=CallLists.read(ctx);val text=CallListTransfer.backup(original)
        assertEquals(original,CallListTransfer.preview(text));val copy=CallLists.importCopies(ctx,text).last().entries.single()
        assertEquals("later",copy.outcome);assertEquals("Next Tuesday",copy.note);assertEquals(789L,copy.updatedAt);assertNotEquals(eid,copy.id)
        for(field in listOf("outcome","note","updatedAt","done")) {
            val root=JSONObject(text);val e=root.getJSONArray("lists").getJSONObject(0).getJSONArray("entries").getJSONObject(0)
            when(field){"outcome"->e.put(field,"unknown");"note"->e.put(field,4);"updatedAt"->e.put(field,1.5);"done"->e.put(field,true)}
            val before=CallLists.read(ctx);try{CallLists.importCopies(ctx,root.toString());fail(field)}catch(_:Exception){};assertEquals(before,CallLists.read(ctx))
        }
    }
    @Test fun reportQuotesMultilineAndFormulaCellsAndKeepsFullNumbersSeparate() {
        val list=CallLists.ListItem("l","=Bad",listOf(CallLists.Entry("a","+442025550123","@Name",false,"later","He said \"yes\",\nnext",12),CallLists.Entry("b","+12025550123","Other")))
        val report=CallListTransfer.report(list)
        assertTrue(report.startsWith("\"List\",\"Name\",\"Number\""))
        assertTrue(report.contains("\"'=Bad\",\"'@Name\",\"'+442025550123\""))
        assertTrue(report.contains("\"He said \"\"yes\"\",\nnext\""))
        assertTrue(report.contains("\"'+12025550123\""));assertTrue(report.contains("\"Call later\""))
    }
    @Test fun fullBackupRestorePreservesOutcomeNoteAndOriginalSaveTime() {
        val(id,eid)=seed();CallLists.followUp(ctx,id,eid,"no_answer","Keep this full note",555L)
        val plan=FullBackup.parse(FullBackup.export(ctx))
        listOf("",".bak",".new").forEach {File(ctx.filesDir,"manual_call_lists.json$it").delete()}
        FullBackup.apply(ctx,plan)
        val entry=CallLists.read(ctx).single().entries.single()
        assertEquals("no_answer",entry.outcome);assertEquals("Keep this full note",entry.note);assertEquals(555L,entry.updatedAt)
    }

}
