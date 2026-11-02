package com.mandu.liquiddialer
import android.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.Repo
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class NotesStorageTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
    private val file get()=File(ctx.filesDir,"notes.json")
    @Before fun clear(){AtomicFile(file).delete()}
    @Test fun oldPlainFileLoadsAndAtomicEditsPreserveUnicode() {
        file.writeText("""[{"id":7,"number":"123","name":"Papa","text":"पुरानी नोट","time":1}]""")
        Repo.addNote(ctx,"456","घर","नई बात 💚")
        val notes=Repo.notes(ctx)
        assertEquals(2,notes.size);assertEquals("नई बात 💚",notes.first().text)
        assertEquals("पुरानी नोट",notes.last().text)
        Repo.deleteNote(ctx,notes.first().id)
        assertEquals("पुरानी नोट",Repo.notes(ctx).single().text)
    }
    @Test fun interruptedAtomicReplacementRecoversPreviousCompleteNotes() {
        Repo.addNote(ctx,"123","Name","saved")
        val before=Repo.notes(ctx)
        val atomic=AtomicFile(file)
        val stream=atomic.startWrite();stream.write("[{broken".toByteArray());stream.close()
        // Simulate a writer disappearing before finishWrite. Next reader must keep old data.
        assertEquals(before,Repo.notes(ctx))
    }
    @Test fun futureExistingIdAndFastAddsNeverDeleteAnotherNote() {
        val future=System.currentTimeMillis()+3600000
        file.writeText("""[{"id":$future,"number":"123","name":"Old","text":"keep","time":1}]""")
        repeat(8){Repo.addNote(ctx,"123","New","note$it")}
        val all=Repo.notes(ctx);assertEquals(9,all.size);assertEquals(9,all.map { it.id }.toSet().size)
        assertTrue(all.filter { it.text!="keep" }.all { it.id>future })
        Repo.deleteNote(ctx,all.first { it.text=="note3" }.id)
        assertEquals(8,Repo.notes(ctx).size);assertTrue(Repo.notes(ctx).any { it.text=="keep" })
    }
}
