package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.Test
import org.junit.Assert.*
class CallNoteReaderTest {
 private fun n(id:Long,number:String="+919876543210",text:String="text",time:Long=id)=Note(id,number,"Name",text,time)
 @Test fun exactFullIdentityNotSuffix(){assertEquals(listOf(1L),CallNoteReader.notes(listOf(n(1),n(2,"+449876543210")),"+919876543210").map{it.id})}
 @Test fun formattingEquivalentStillSameIdentity(){assertEquals(1,CallNoteReader.notes(listOf(n(1)),"+91 98765 43210").size)}
 @Test fun emptyAndPrivateTargetsRejected(){listOf("","private","unknown","+","*123#").forEach{assertFalse(CallNoteReader.eligible(it))}}
 @Test(expected=IllegalArgumentException::class) fun invalidTargetCannotReadAll(){CallNoteReader.notes(listOf(n(1)),"")}
 @Test fun searchOnlyMatchesSavedText(){val list=listOf(n(1,text="Delivery gate"),n(2,text="Invoice"));assertEquals(listOf(1L),CallNoteReader.notes(list,"+919876543210","GATE").map{it.id});assertTrue(CallNoteReader.notes(list,"+919876543210","Name").isEmpty())}
 @Test fun searchLiteralNotRegex(){assertEquals(1,CallNoteReader.notes(listOf(n(1,text="a.*b")),"+919876543210",".*").size)}
 @Test fun unicodeSearchRetainsOriginal(){val a=n(1,text="दूध कल देना");assertEquals(listOf(a),CallNoteReader.notes(listOf(a),a.number,"दूध"))}
 @Test fun mostRecentFirstWithStableTieBreak(){assertEquals(listOf(3L,2L,1L),CallNoteReader.notes(listOf(n(1,time=1),n(2,time=2),n(3,time=2)),"+919876543210").map{it.id})}
 @Test fun pagesBoundedAndNoOmissions(){val list=(1L..29L).map{n(it)};assertEquals(list, (0..2).flatMap{CallNoteReader.page(list,it)});assertEquals(12,CallNoteReader.page(list,0).size);assertEquals(5,CallNoteReader.page(list,99).size)}
 @Test fun negativePageClamped(){val list=listOf(n(1));assertEquals(list,CallNoteReader.page(list,-1));assertTrue(CallNoteReader.page(emptyList(),1).isEmpty())}
 @Test fun previewBoundedWithoutMutatingText(){val s="z".repeat(50000);assertEquals(221,CallNoteReader.preview(s).length);assertEquals(s,n(1,text=s).text);assertEquals("short",CallNoteReader.preview("short"))}
 @Test fun blankQueryPreservesAllEligible(){val list=listOf(n(1),n(2));assertEquals(2,CallNoteReader.notes(list,"+919876543210"," ").size)}
}
