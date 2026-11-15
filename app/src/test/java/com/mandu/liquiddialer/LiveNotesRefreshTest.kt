package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class LiveNotesRefreshTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val n="+919876543210"
 private val file get()=File(ctx.filesDir,"notes.json")
 @Before fun cleanBefore(){file.delete();File(ctx.filesDir,"notes.json.bak").delete()}
 @After fun clean(){cleanBefore()}
 @Test fun defaultOffFinder(){assertFalse(LdSettings().liveNotesRefresh);assertEquals("livenotesrefresh",SettingsFinder.all.single{it.id=="live-notes-refresh"}.route)}
 @Test fun successfulAddSignalsOnce(){val v=Repo.notesRevision.value;Repo.addNote(ctx,n,"Support","One");assertEquals(v+1,Repo.notesRevision.value);assertEquals("One",Repo.notesForChecked(ctx,n).single().text)}
 @Test fun blankAddNoSignal(){val v=Repo.notesRevision.value;Repo.addNote(ctx,n,"","  ");assertEquals(v,Repo.notesRevision.value);assertFalse(file.exists())}
 @Test fun tooLongNoSignal(){val v=Repo.notesRevision.value;try{Repo.addNote(ctx,n,"","x".repeat(50001));Assert.fail()}catch(_:IllegalArgumentException){};assertEquals(v,Repo.notesRevision.value);assertFalse(file.exists())}
 @Test fun corruptAddNoSignalNoOverwrite(){file.writeText("broken");val v=Repo.notesRevision.value;try{Repo.addNote(ctx,n,"","One");Assert.fail()}catch(_:Exception){};assertEquals(v,Repo.notesRevision.value);assertEquals("broken",file.readText())}
 @Test fun editSignalsAndReadsNew(){Repo.addNote(ctx,n,"","Old");val old=Repo.notesForChecked(ctx,n).single();val v=Repo.notesRevision.value;Repo.updateNote(ctx,old,"New");assertEquals(v+1,Repo.notesRevision.value);assertEquals("New",Repo.notesForChecked(ctx,n).single().text)}
 @Test fun staleEditNoSignal(){Repo.addNote(ctx,n,"","Old");val old=Repo.notesForChecked(ctx,n).single();Repo.updateNote(ctx,old,"New");val v=Repo.notesRevision.value;try{Repo.updateNote(ctx,old,"Stale");Assert.fail()}catch(_:Exception){};assertEquals(v,Repo.notesRevision.value);assertEquals("New",Repo.notesForChecked(ctx,n).single().text)}
 @Test fun deleteSignalsAndRemoves(){Repo.addNote(ctx,n,"","Old");val old=Repo.notesForChecked(ctx,n).single();val v=Repo.notesRevision.value;Repo.deleteNote(ctx,old.id);assertEquals(v+1,Repo.notesRevision.value);assertTrue(Repo.notesForChecked(ctx,n).isEmpty())}
 @Test fun importSignals(){val v=Repo.notesRevision.value;Repo.importMerged(ctx,listOf(Note(1,n,"","Import",1)));assertEquals(v+1,Repo.notesRevision.value);assertEquals("Import",Repo.notesForChecked(ctx,n).single().text)}
 @Test fun emptyImportNoSignal(){val v=Repo.notesRevision.value;Repo.importMerged(ctx,emptyList());assertEquals(v,Repo.notesRevision.value)}
 @Test fun checkedReadNeverSignalsOrWrites(){Repo.addNote(ctx,n,"","Old");val before=file.readBytes();val v=Repo.notesRevision.value;repeat(3){assertEquals(1,Repo.notesForChecked(ctx,n).size)};assertEquals(v,Repo.notesRevision.value);assertArrayEquals(before,file.readBytes())}
 @Test fun exactInternationalNoLastTenCollision(){Repo.addNote(ctx,n,"","India");Repo.addNote(ctx,"+449876543210","","UK");assertEquals("India",Repo.notesForChecked(ctx,n).single().text);assertEquals("UK",Repo.notesForChecked(ctx,"+449876543210").single().text)}
 @Test fun formattedSameIdentity(){Repo.addNote(ctx,n,"","India");assertEquals("India",Repo.notesForChecked(ctx,"+91 98765 43210").single().text)}
 @Test fun emptyNumberNeverMatches(){Repo.addNote(ctx,"","","No number");assertTrue(Repo.notesForChecked(ctx,"").isEmpty())}
 @Test fun checkedCorruptReadThrowsPreserves(){file.writeText("broken");val v=Repo.notesRevision.value;try{Repo.notesForChecked(ctx,n);Assert.fail()}catch(_:Exception){};assertEquals(v,Repo.notesRevision.value);assertEquals("broken",file.readText())}
 @Test fun checkedOrderNewestFirst(){Repo.importMerged(ctx,listOf(Note(1,n,"","Older",10),Note(2,n,"","Newer",20)));assertEquals(listOf("Newer","Older"),Repo.notesForChecked(ctx,n).map{it.text})}
}
