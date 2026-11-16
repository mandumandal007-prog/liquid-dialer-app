package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class DeskFinderTest{
 private val n="+919876543210"
 private val full=LdSettings(deskFinder=true,liveCallDesk=true,callTally=true,callDates=true,callConverter=true,callCanvas=true,messageDrafts=true,callReferences=true,callNoteReader=true)
 private fun ids(s:LdSettings=full,number:String=n)=DeskFinder.available(s,number).map{it.id}
 @Test fun defaultsOff(){assertFalse(LdSettings().deskFinder)}
 @Test fun elevenUniqueIds(){assertEquals(11,DeskFinder.all.size);assertEquals(11,DeskFinder.all.map{it.id}.toSet().size)}
 @Test fun metadataComplete(){assertTrue(DeskFinder.all.all{it.title.isNotBlank()&&it.detail.isNotBlank()&&it.aliases.isNotBlank()&&it.group in DeskFinder.groups&&it.group!="All"})}
 @Test fun offNoTools(){assertTrue(ids(full.copy(deskFinder=false)).isEmpty())}
 @Test fun deskOffNoTools(){assertTrue(ids(full.copy(liveCallDesk=false)).isEmpty())}
 @Test fun notesHiddenNoTools(){assertTrue(ids(full.copy(hiddenFeatures=setOf("notes"))).isEmpty())}
 @Test fun coreFourOnlyDefaultOthersOff(){assertEquals(listOf("checklist","estimate","timeline","brief"),ids(LdSettings(deskFinder=true,liveCallDesk=true)))}
 @Test fun everyOptionalOffExcluded(){val tests=listOf("tally" to full.copy(callTally=false),"dates" to full.copy(callDates=false),"converter" to full.copy(callConverter=false),"canvas" to full.copy(callCanvas=false),"drafts" to full.copy(messageDrafts=false),"references" to full.copy(callReferences=false),"reader" to full.copy(callNoteReader=false));for((id,s)in tests){assertFalse(id in ids(s));assertEquals(10,ids(s).size)}}
 @Test fun hiddenCoreExcluded(){for(id in listOf("checklist","estimate","timeline","brief")){assertFalse(id in ids(full.copy(hiddenFeatures=setOf(id))));assertEquals(10,ids(full.copy(hiddenFeatures=setOf(id))).size)}}
 @Test fun allEnabledHasAll(){assertEquals(DeskFinder.all.map{it.id},ids())}
 @Test fun invalidNumberNoNumberTools(){val result=ids(number="bad");assertFalse(result.any{it in listOf("checklist","estimate","timeline","brief","canvas","drafts","references","reader")});assertEquals(listOf("tally","dates","converter"),result)}
 @Test fun emptyNumberNoNumberTools(){assertEquals(listOf("tally","dates","converter"),ids(number=""))}
 @Test fun emptyQueryStableOrder(){assertEquals(DeskFinder.all,DeskFinder.search(DeskFinder.all,""))}
 @Test fun whitespaceQueryAll(){assertEquals(DeskFinder.all,DeskFinder.search(DeskFinder.all," \t \n "))}
 @Test fun titleCaseInsensitive(){assertEquals(listOf("estimate"),DeskFinder.search(DeskFinder.all,"CALCULATOR rate").map{it.id})}
 @Test fun aliasesMatch(){assertEquals(listOf("converter"),DeskFinder.search(DeskFinder.all,"kg").map{it.id})}
 @Test fun hindiAliases(){assertEquals(listOf("tally"),DeskFinder.search(DeskFinder.all,"गिनती").map{it.id});assertEquals(listOf("dates"),DeskFinder.search(DeskFinder.all,"तारीख").map{it.id})}
 @Test fun multipleTermsAllNeeded(){assertEquals(listOf("reader"),DeskFinder.search(DeskFinder.all,"saved notes read").map{it.id});assertTrue(DeskFinder.search(DeskFinder.all,"kg checklist").isEmpty())}
 @Test fun noPrivateContentSource(){assertTrue(DeskFinder.search(DeskFinder.all,"secret order 998877").isEmpty())}
 @Test fun prepareCategory(){assertEquals(listOf("checklist","brief","canvas","drafts"),DeskFinder.search(DeskFinder.all,"","Prepare").map{it.id})}
 @Test fun calculateCategory(){assertEquals(listOf("estimate","tally","dates","converter"),DeskFinder.search(DeskFinder.all,"","Calculate").map{it.id})}
 @Test fun referenceCategory(){assertEquals(listOf("timeline","references","reader"),DeskFinder.search(DeskFinder.all,"","Reference").map{it.id})}
 @Test fun categoryAndQueryIntersect(){assertTrue(DeskFinder.search(DeskFinder.all,"kg","Prepare").isEmpty());assertEquals(listOf("converter"),DeskFinder.search(DeskFinder.all,"kg","Calculate").map{it.id})}
 @Test fun unknownGroupRefused(){try{DeskFinder.search(DeskFinder.all,"","Other");fail("Refuse")}catch(_:IllegalArgumentException){}}
 @Test fun cappedQuery(){assertEquals(DeskFinder.search(DeskFinder.all,"x".repeat(100)),DeskFinder.search(DeskFinder.all,"x".repeat(100)+"calculator"))}
 @Test fun noAvailabilityMutation(){val s=full.copy(hiddenFeatures=setOf("estimate"));val before=s.copy();DeskFinder.available(s,n);assertEquals(before,s)}
 @Test fun searchCannotResurrectHidden(){val a=DeskFinder.available(full.copy(hiddenFeatures=setOf("estimate")),n);assertTrue(DeskFinder.search(a,"calculator rate").isEmpty())}
 @Test fun finderSettingsRoute(){assertTrue(SettingsFinder.search("desk finder").any{it.id=="desk-finder"&&it.route=="deskfinder"})}
}
