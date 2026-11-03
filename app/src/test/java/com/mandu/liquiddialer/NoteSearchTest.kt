package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class NoteSearchTest {
    private val notes = listOf(Note(3,"+91 98765 43210","Rahul","दवाई कल लाना 💚",3),Note(2,"0123-456","Papa","Meeting Friday",2),Note(1,"555","Rahul","old bill",1))
    @Test fun textNameUnicodeAndFormattedNumbersKeepOrder() {
        assertEquals(listOf(notes[0]),NoteSearch.find(notes,"दवाई"))
        assertEquals(listOf(notes[0]),NoteSearch.find(notes,"💚"))
        assertEquals(listOf(notes[1]),NoteSearch.find(notes," meeting "))
        assertEquals(listOf(notes[0],notes[2]),NoteSearch.find(notes,"RAHUL"))
        assertEquals(listOf(notes[0]),NoteSearch.find(notes,"+91 (98765) 43210"))
        assertEquals(listOf(notes[1]),NoteSearch.find(notes,"0123456"))
    }
    @Test fun blankResetNoMatchAndEmptyDoNotChangeStoredNotes() {
        assertEquals(notes,NoteSearch.find(notes,"  "))
        assertTrue(NoteSearch.find(notes,"not found").isEmpty())
        assertTrue(NoteSearch.find(emptyList(),"Rahul").isEmpty())
        assertEquals(listOf(notes[0]),NoteSearch.find(notes,"+"))
        assertEquals(3,notes.size)
        assertEquals(listOf(3L,2L,1L),notes.map { it.id })
    }
}
