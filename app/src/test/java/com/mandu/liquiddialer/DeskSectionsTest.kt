package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class DeskSectionsTest {
 private val base get()=DeskSections.parse(DeskSections.DEFAULT)
 private fun bad(action:()->Unit){try{action();fail("Must refuse")}catch(_:IllegalArgumentException){}}
 @Test fun defaultsOff(){assertFalse(LdSettings().deskSections);assertEquals(DeskSections.DEFAULT,LdSettings().deskSectionLayout)}
 @Test fun defaultOrder(){assertEquals(listOf("watch","marks","notes"),base.map{it.id});assertTrue(base.all{it.visible})}
 @Test fun validCustomRoundTrip(){val raw="notes:1,marks:0,watch:1";assertEquals(raw,DeskSections.encode(DeskSections.parse(raw)))}
 @Test fun emptyFallback(){assertEquals(base,DeskSections.parse(""))}
 @Test fun unknownFallback(){assertEquals(base,DeskSections.parse("other:1,marks:1,notes:1"))}
 @Test fun duplicateFallback(){assertEquals(base,DeskSections.parse("watch:1,watch:1,notes:1"))}
 @Test fun missingFallback(){assertEquals(base,DeskSections.parse("watch:1,notes:1"))}
 @Test fun extraFallback(){assertEquals(base,DeskSections.parse(DeskSections.DEFAULT+",notes:1"))}
 @Test fun invalidFlagFallback(){assertEquals(base,DeskSections.parse("watch:2,marks:1,notes:1"))}
 @Test fun whitespaceFallback(){assertEquals(base,DeskSections.parse("watch:1, marks:1,notes:1"))}
 @Test fun hiddenNotesFallback(){assertEquals(base,DeskSections.parse("watch:0,marks:0,notes:0"))}
 @Test fun oversizedFallback(){assertEquals(base,DeskSections.parse("x".repeat(101)))}
 @Test fun nestedTokenFallback(){assertEquals(base,DeskSections.parse("watch:1:1,marks:1,notes:1"))}
 @Test fun moveUp(){assertEquals(listOf("watch","notes","marks"),DeskSections.move(base,"notes",-1).map{it.id})}
 @Test fun moveDown(){assertEquals(listOf("marks","watch","notes"),DeskSections.move(base,"watch",1).map{it.id})}
 @Test fun moveReversible(){assertEquals(base,DeskSections.move(DeskSections.move(base,"notes",-1),"notes",1))}
 @Test fun moveBoundaryRefused(){bad{DeskSections.move(base,"watch",-1)};bad{DeskSections.move(base,"notes",1)}}
 @Test fun badDirectionRefused(){bad{DeskSections.move(base,"marks",0)};bad{DeskSections.move(base,"marks",2)}}
 @Test fun unknownRefused(){bad{DeskSections.move(base,"end",1)};bad{DeskSections.show(base,"end",false)}}
 @Test fun notesCannotHide(){bad{DeskSections.show(base,"notes",false)}}
 @Test fun hideWatchOnly(){val v=DeskSections.show(base,"watch",false);assertFalse(v[0].visible);assertTrue(v[1].visible&&v[2].visible);assertTrue(base[0].visible)}
 @Test fun bothOptionalHiddenNotesStays(){val v=DeskSections.show(DeskSections.show(base,"watch",false),"marks",false);assertEquals(listOf("notes"),v.filter{it.visible}.map{it.id});assertEquals(v,DeskSections.parse(DeskSections.encode(v)))}
 @Test fun showingNotesIdempotent(){assertEquals(base,DeskSections.show(base,"notes",true))}
 @Test fun offIgnoresCustomWithoutDeleting(){val s=LdSettings(deskSections=false,deskSectionLayout="notes:1,watch:0,marks:0");assertEquals(base,DeskSections.snapshot(s));assertEquals("notes:1,watch:0,marks:0",s.deskSectionLayout)}
 @Test fun onUsesCustom(){val s=LdSettings(deskSections=true,deskSectionLayout="notes:1,watch:0,marks:0");assertEquals(listOf("notes","watch","marks"),DeskSections.snapshot(s).map{it.id});assertFalse(s.liveCallDesk||s.deskStopwatch||s.callCheckpoints||s.deskKeypad)}
 @Test fun everyPermutationAndVisibilityRoundTrip(){fun permutations(x:List<String>):List<List<String>> = if(x.isEmpty())listOf(emptyList())else x.flatMap{i->permutations(x-i).map{listOf(i)+it}}
 for(p in permutations(DeskSections.ids))for(w in listOf(false,true))for(m in listOf(false,true)){val rows=p.map{DeskSections.Section(it,when(it){"watch"->w;"marks"->m;else->true})};assertEquals(rows,DeskSections.parse(DeskSections.encode(rows)))}}
 @Test fun invalidListRefused(){bad{DeskSections.encode(emptyList())};bad{DeskSections.encode(listOf(DeskSections.Section("end"),DeskSections.Section("marks"),DeskSections.Section("notes")))}}
 @Test fun titlesDefined(){assertEquals("Notes and call tools",DeskSections.title("notes"));assertEquals("Call checkpoints",DeskSections.title("marks"));assertEquals("Interval stopwatch",DeskSections.title("watch"))}
 @Test fun finderRoute(){assertTrue(SettingsFinder.search("desk section order").any{it.id=="desk-sections"&&it.route=="desksections"})}
}
