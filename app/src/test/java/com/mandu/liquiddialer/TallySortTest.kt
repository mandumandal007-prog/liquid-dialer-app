package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class TallySortTest{
 private val rows=listOf(CallTally.Item("a","Sugar bags",5),CallTally.Item("b","Rice bags",0),CallTally.Item("c","Wheat bags",5))
 private fun ids(mode:String)=TallySort.rows(rows,mode).map{it.id}
 @Test fun defaultsOff(){assertFalse(LdSettings().tallySort);assertFalse(LdSettings().tallyPaired)}
 @Test fun originalBoardOrder(){assertEquals(listOf("a","b","c"),ids("Board order"))}
 @Test fun nameOrder(){assertEquals(listOf("b","a","c"),ids("Name A-Z"))}
 @Test fun highStableTies(){assertEquals(listOf("a","c","b"),ids("Quantity high-low"))}
 @Test fun lowStableTies(){assertEquals(listOf("b","a","c"),ids("Quantity low-high"))}
 @Test fun nameIgnoresCase(){val x=listOf(CallTally.Item("a","zebra",1),CallTally.Item("b","Apple",2));assertEquals(listOf("b","a"),TallySort.rows(x,"Name A-Z").map{it.id})}
 @Test fun emptyBoard(){for(m in TallySort.modes)assertTrue(TallySort.rows(emptyList(),m).isEmpty())}
 @Test fun maximumValuesExact(){val x=listOf(CallTally.Item("a","A",1000000000),CallTally.Item("b","B",999999999));assertEquals(x,TallySort.rows(x,"Quantity high-low"))}
 @Test fun quantitiesUnchanged(){for(m in TallySort.modes)assertEquals(rows.associate{it.id to it.value},TallySort.rows(rows,m).associate{it.id to it.value})}
 @Test fun labelsUnchanged(){for(m in TallySort.modes)assertEquals(rows.associate{it.id to it.label},TallySort.rows(rows,m).associate{it.id to it.label})}
 @Test fun invalidModeRefused(){try{TallySort.rows(rows,"unknown");fail()}catch(_:IllegalArgumentException){}}
 @Test fun invalidBoardRefused(){try{TallySort.rows(rows+rows[0],"Board order");fail()}catch(_:IllegalArgumentException){}}
 @Test fun noUndoEdit(){val b=CallTally.Board(rows);TallySort.rows(b.rows,"Quantity high-low");assertFalse(b.canUndo);assertEquals(rows,b.rows)}
 @Test fun sourceNotMutated(){val x=rows.toMutableList();TallySort.rows(x,"Name A-Z");assertEquals(rows,x)}
 @Test fun copiedBoardOrder(){val x=rows.toMutableList();val y=TallySort.rows(x,"Board order");x.clear();assertEquals(rows,y)}
 @Test fun composeFilter(){val f=TallyFind.rows(rows,"bags","Nonzero");assertEquals(listOf("a","c"),TallySort.rows(f,"Quantity low-high").map{it.id})}
 @Test fun adjustedValueReordersSameId(){val x=CallTally.adjust(rows,"b",6);assertEquals(listOf("b","a","c"),TallySort.rows(x,"Quantity high-low").map{it.id})}
 @Test fun settingsRoute(){for(q in listOf("sort session counter","paired tally view"))assertTrue(SettingsFinder.search(q).any{it.route=="calltally"})}
}
