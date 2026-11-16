package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class TallyFindTest{
 private val rows=listOf(CallTally.Item("a","Rice bags",5),CallTally.Item("b","Sugar bags",0),CallTally.Item("c","Rice sacks",10))
 @Test fun defaultsOff(){assertFalse(LdSettings().tallyFind);assertFalse(LdSettings().tallyFold)}
 @Test fun emptyAllStable(){assertEquals(rows,TallyFind.rows(rows,"","All"))}
 @Test fun whitespaceAll(){assertEquals(rows,TallyFind.rows(rows," \t ","All"))}
 @Test fun caseInsensitive(){assertEquals(listOf(rows[0],rows[2]),TallyFind.rows(rows,"RICE","All"))}
 @Test fun multipleTermsIntersect(){assertEquals(listOf(rows[0]),TallyFind.rows(rows,"rice bags","All"))}
 @Test fun zeroOnly(){assertEquals(listOf(rows[1]),TallyFind.rows(rows,"","Zero"))}
 @Test fun nonzeroOnly(){assertEquals(listOf(rows[0],rows[2]),TallyFind.rows(rows,"","Nonzero"))}
 @Test fun nameAndModeIntersect(){assertTrue(TallyFind.rows(rows,"rice","Zero").isEmpty())}
 @Test fun noMatchDoesNotDelete(){assertTrue(TallyFind.rows(rows,"notes private","All").isEmpty());assertEquals(15,CallTally.total(rows))}
 @Test fun quantitiesNotSearched(){assertTrue(TallyFind.rows(rows,"10","All").isEmpty())}
 @Test fun idsNotSearched(){assertTrue(TallyFind.rows(listOf(CallTally.Item("secret-id","Rice",1)),"secret-id","All").isEmpty())}
 @Test fun hindiName(){val r=listOf(CallTally.Item("a","चावल बोरी",0));assertEquals(r,TallyFind.rows(r,"चावल","Zero"))}
 @Test fun queryBounded(){assertEquals(TallyFind.rows(rows,"x".repeat(100),"All"),TallyFind.rows(rows,"x".repeat(100)+"rice","All"))}
 @Test fun invalidModeRefused(){try{TallyFind.rows(rows,"","Other");fail()}catch(_:IllegalArgumentException){}}
 @Test fun invalidBoardRefused(){try{TallyFind.rows(rows+rows[0],"","All");fail()}catch(_:IllegalArgumentException){}}
 @Test fun filterNoHistoryEdit(){val b=CallTally.Board(rows);TallyFind.rows(b.rows,"rice","Nonzero");assertFalse(b.canUndo);assertEquals(rows,b.rows)}
 @Test fun sourceImmutable(){val r=rows.toMutableList();val x=TallyFind.rows(r,"","All");r.clear();assertEquals(rows,x)}
 @Test fun settingsRoute(){for(q in listOf("find session counters","fold counter actions"))assertTrue(SettingsFinder.search(q).any{it.route=="calltally"})}
}
