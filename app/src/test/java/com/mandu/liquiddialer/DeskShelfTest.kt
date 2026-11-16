package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class DeskShelfTest{
 private val all=DeskFinder.all
 private val four=all.take(4).map{it.id}
 @Test fun defaultsOff(){assertFalse(LdSettings().deskShelf);assertFalse(LdSettings().deskFinderCompact)}
 @Test fun addOnlyRequested(){assertEquals(listOf("estimate"),DeskShelf.toggle(emptyList(),"estimate",all))}
 @Test fun unknownRefused(){assertTrue(DeskShelf.toggle(emptyList(),"secret",all).isEmpty())}
 @Test fun unavailableRefused(){assertTrue(DeskShelf.toggle(emptyList(),"estimate",all.filter{it.id!="estimate"}).isEmpty())}
 @Test fun fullDoesNotReplace(){assertEquals(four,DeskShelf.toggle(four,"tally",all))}
 @Test fun fullCanUnpin(){assertEquals(four.drop(1),DeskShelf.toggle(four,four[0],all))}
 @Test fun stableOrder(){assertEquals(four,DeskShelf.visible(four,all.reversed()))}
 @Test fun duplicateRemoved(){assertEquals(listOf("estimate"),DeskShelf.visible(listOf("estimate","estimate"),all))}
 @Test fun capFour(){assertEquals(four,DeskShelf.visible(all.map{it.id},all))}
 @Test fun removedToolPruned(){assertEquals(four.drop(1),DeskShelf.visible(four,all.filter{it.id!=four[0]}))}
 @Test fun prunedDoesNotReappear(){val x=DeskShelf.visible(four,all.filter{it.id!=four[0]});assertEquals(four.drop(1),DeskShelf.visible(x,all))}
 @Test fun emptyAvailableClears(){assertTrue(DeskShelf.visible(four,emptyList()).isEmpty())}
 @Test fun sourceNotMutated(){val x=four.toMutableList();DeskShelf.toggle(x,"tally",all);assertEquals(four,x)}
 @Test fun sourceCopyNotShared(){val x=mutableListOf("estimate");val y=DeskShelf.visible(x,all);x.clear();assertEquals(listOf("estimate"),y)}
 @Test fun hiddenCoreCannotPin(){val a=DeskFinder.available(LdSettings(deskShelf=true,deskFinder=true,liveCallDesk=true,hiddenFeatures=setOf("estimate")),"+919876543210");assertTrue(DeskShelf.toggle(emptyList(),"estimate",a).isEmpty())}
 @Test fun optionDoesNotEnableTools(){assertTrue(DeskFinder.available(LdSettings(deskShelf=true,deskFinderCompact=true),"+919876543210").isEmpty())}
 @Test fun settingsRoutes(){for(q in listOf("quick shelf","compact finder"))assertTrue(SettingsFinder.search(q).any{it.route=="deskfinder"})}
}
