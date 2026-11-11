package com.mandu.liquiddialer
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CallReferencesTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+919876543210"
 private val card=CallReferences.Card("order","Order reference","ORDER-204 • two cartons")
 private fun bad(f:()->Unit){try{f();fail("must reject")}catch(_:IllegalArgumentException){}}
 @Before fun clean(){File(ctx.filesDir,"call_references.json").delete();File(ctx.filesDir,"call_references.json.bak").delete()}
 @Test fun defaultOffAndSettingsRoundTrip(){assertFalse(LdSettings().callReferences);runBlocking{ctx.repo.set(K.callReferences,true)};assertTrue(runBlocking{ ctx.repo.flow.first() }.callReferences)}
 @Test fun emptyReadDoesNotCreateFile(){assertTrue(CallReferences.read(ctx,number).cards.isEmpty());assertFalse(File(ctx.filesDir,"call_references.json").exists())}
 @Test fun ordinaryMultilineUnicodeRoundTrips(){val c=card.copy(value="Office 2\nDelhi • सुबह 9 बजे");val s=CallReferences.save(ctx,number,"",listOf(c));assertEquals(listOf(c),CallReferences.read(ctx,number).cards);assertEquals(s.raw,CallReferences.encode(CallReferences.decode(s.raw)))}
 @Test fun fullIdentityNeverUsesLastTen(){CallReferences.save(ctx,number,"",listOf(card));assertTrue(CallReferences.read(ctx,"+449876543210").cards.isEmpty());assertEquals(listOf(card),CallReferences.read(ctx,"+91 98765 43210").cards)}
 @Test fun localShortNumberIsSeparate(){CallReferences.save(ctx,"139","",listOf(card));assertTrue(CallReferences.read(ctx,"+91139").cards.isEmpty())}
 @Test fun invalidNumbersFailBeforeWriting(){listOf("","private","12","+91x9876543210").forEach{bad{CallReferences.read(ctx,it)}};assertFalse(File(ctx.filesDir,"call_references.json").exists())}
 @Test fun validationRejectsBlankLongAndControls(){listOf(card.copy(title=""),card.copy(title="x".repeat(41)),card.copy(value=""),card.copy(value="x".repeat(501)),card.copy(value="bad\tvalue"),card.copy(title="bad\nname"),card.copy(id="bad/id")).forEach{bad{it.validate()}}}
 @Test fun duplicateIdsRejectWithoutMutation(){val s=CallReferences.save(ctx,number,"",listOf(card));bad{CallReferences.save(ctx,number,s.raw,listOf(card,card))};assertEquals(s,CallReferences.read(ctx,number))}
 @Test fun twelveCardsCapAndBoundaryValues(){val rows=(1..12).map{card.copy(id="id$it",title="x".repeat(40),value="a".repeat(500))};val s=CallReferences.save(ctx,number,"",rows);bad{CallReferences.save(ctx,number,s.raw,rows+card)};assertEquals(rows,CallReferences.read(ctx,number).cards)}
 @Test fun staleSaveAndDeletePreserveCurrent(){val a=CallReferences.save(ctx,number,"",listOf(card));val b=CallReferences.save(ctx,number,a.raw,listOf(card.copy(value="Changed")));bad{CallReferences.save(ctx,number,a.raw,emptyList())};assertEquals(b,CallReferences.read(ctx,number))}
 @Test fun deletionRemovesOnlyChosenNumber(){val a=CallReferences.save(ctx,number,"",listOf(card));val b=CallReferences.save(ctx,"+919999999999",a.raw,listOf(card.copy(value="Other")));val c=CallReferences.save(ctx,number,b.raw,emptyList());assertTrue(c.cards.isEmpty());assertEquals("Other",CallReferences.read(ctx,"+919999999999").cards.single().value)}
 @Test fun corruptDataCannotBeOverwritten(){val f=File(ctx.filesDir,"call_references.json");f.writeText("broken");try{CallReferences.save(ctx,number,"",listOf(card));fail()}catch(_:IllegalStateException){};assertEquals("broken",f.readText())}
 @Test fun invalidUtf8CannotBeOverwritten(){val f=File(ctx.filesDir,"call_references.json");val bytes=byteArrayOf(0xC3.toByte(),0x28);f.writeBytes(bytes);try{CallReferences.save(ctx,number,"",listOf(card));fail()}catch(_:Exception){};assertArrayEquals(bytes,f.readBytes())}
 @Test fun strictSchemaRejectsTrailingVersionAndWrongTypes(){val good=CallReferences.encode(mapOf(CallReferences.key(number) to listOf(card)));listOf(good+"x",good.replace("\"version\":1","\"version\":2"),good.replace("\"value\":\"ORDER-204 • two cartons\"","\"value\":123")).forEach{bad{CallReferences.decode(it)}}}
 @Test fun titleSearchNeverMatchesHiddenValue(){assertEquals(listOf(card),CallReferences.search(listOf(card)," order "));assertTrue(CallReferences.search(listOf(card),"204").isEmpty())}
 @Test fun hundredNumberCapPreservesStore(){val all=(1000000..1000099).associate{"local:$it" to listOf(card)};val raw=CallReferences.encode(all);File(ctx.filesDir,"call_references.json").writeText(raw);bad{CallReferences.save(ctx,number,raw,listOf(card))};assertEquals(raw,File(ctx.filesDir,"call_references.json").readText())}
}
