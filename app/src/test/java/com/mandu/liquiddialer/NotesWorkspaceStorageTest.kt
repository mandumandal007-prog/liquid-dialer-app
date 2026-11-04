package com.mandu.liquiddialer
import android.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class NotesWorkspaceStorageTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private val file get()=File(ctx.filesDir,"notes.json")
    @Before fun clear(){AtomicFile(file).delete()}
    private fun rejected(block:()->Unit){try{block();fail("must reject")}catch(_:Exception){}}
    @Test fun fullNumbersDoNotShareSuffixAndInternationalAliasesMatch(){
        Repo.addNote(ctx,"+12025550123","US","USA")
        Repo.addNote(ctx,"+442025550123","UK","UK")
        assertEquals(listOf("USA"),Repo.notesFor(ctx,"0012025550123").map{it.text})
        assertEquals(listOf("UK"),Repo.notesFor(ctx,"+44 20 2555 0123").map{it.text})
        assertTrue(Repo.notesFor(ctx,"").isEmpty())
        Repo.addNote(ctx,"+39 06 12345678","Italy","Rome")
        assertTrue(Repo.notesFor(ctx,"+39612345678").isEmpty())
    }
    @Test fun malformedStorageBlocksAllWritesWithoutReplacingBytes(){
        for(data in listOf("[{not-complete","[] trailing","[{\"id\":1.5,\"time\":1,\"number\":\"1\",\"text\":\"x\"}]","[{\"id\":1,\"time\":1,\"number\":12,\"text\":\"x\"}]")){
            file.writeText(data);val before=file.readBytes()
            rejected{Repo.notesChecked(ctx)};rejected{Repo.addNote(ctx,"123","","new")};rejected{Repo.deleteNote(ctx,1)}
            assertArrayEquals(before,file.readBytes())
        }
    }
    @Test fun editIsExplicitPreservesIdentityTimestampAndOthers(){
        Repo.addNote(ctx,"123","Papa","old");Repo.addNote(ctx,"456","Other","keep")
        val before=Repo.notesChecked(ctx);val old=before.first{it.number=="123"}
        Repo.updateNote(ctx,old,"  पूरा text 💚\nsecond line  ")
        val after=Repo.notesChecked(ctx)
        assertEquals(old.copy(text="पूरा text 💚\nsecond line"),after.first{it.id==old.id})
        assertEquals(before.first{it.number=="456"},after.first{it.number=="456"})
        rejected{Repo.updateNote(ctx,old,"stale")};assertEquals(after,Repo.notesChecked(ctx))
    }
    @Test fun invalidEditsAndOversizedAddsLeaveOldNotesUntouched(){
        Repo.addNote(ctx,"123","","saved");val old=Repo.notesChecked(ctx).single();val before=file.readBytes()
        rejected{Repo.updateNote(ctx,old,"   ")};rejected{Repo.updateNote(ctx,old,"x".repeat(50001))};rejected{Repo.addNote(ctx,"123","","x".repeat(50001))}
        assertArrayEquals(before,file.readBytes())
        Repo.deleteNote(ctx,old.id);rejected{Repo.updateNote(ctx,old,"resurrect")};assertTrue(Repo.notesChecked(ctx).isEmpty())
    }
    @Test fun malformedUtf8AndDuplicateIdsBlockWrites(){
        file.writeBytes(byteArrayOf(0xc3.toByte(),0x28));val bytes=file.readBytes();rejected{Repo.addNote(ctx,"123","","new")};assertArrayEquals(bytes,file.readBytes())
        file.writeText("""[{"id":1,"time":1,"number":"123","text":"a"},{"id":1,"time":2,"number":"456","text":"b"}]""")
        val duplicate=file.readBytes();rejected{Repo.deleteNote(ctx,1)};assertArrayEquals(duplicate,file.readBytes())
    }
}
