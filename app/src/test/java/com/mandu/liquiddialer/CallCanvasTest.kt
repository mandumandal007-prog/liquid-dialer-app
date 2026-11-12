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
class CallCanvasTest{
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val number="+919876543210"
 private val row=CallCanvas.Stroke(1,2,listOf(CallCanvas.Point(0,0),CallCanvas.Point(5000,3000),CallCanvas.Point(10000,10000)))
 private val file get()=File(ctx.filesDir,"call_canvas.json")
 private fun bad(f:()->Unit){try{f();fail("must reject")}catch(_:Exception){}}
 @Before fun clean(){android.util.AtomicFile(file).delete()}
 @Test fun longGestureRetainsStartAndFinalPointWithinCap(){val points=mutableListOf(CallCanvas.Point(0,0));repeat(10000){i->CallCanvas.appendSample(points,CallCanvas.Point(i+1,(i+1)%10001));assertTrue(points.size<=400)};assertEquals(CallCanvas.Point(0,0),points.first());assertEquals(CallCanvas.Point(10000,10000),points.last());CallCanvas.Stroke(0,1,points).validate()}
 @Test fun oversizedAndEmptySchemaNeverOverwrite(){file.writeBytes(ByteArray(CallCanvas.MAX_BYTES+1){32});bad{CallCanvas.read(ctx,number)};assertEquals(CallCanvas.MAX_BYTES+1,file.length().toInt());bad{CallCanvas.encode(mapOf(CallCanvas.key(number) to emptyList()))}}
 @Test fun defaultOffAndSetting(){assertFalse(LdSettings().callCanvas);runBlocking{ctx.repo.set(K.callCanvas,true)};assertTrue(runBlocking{ctx.repo.flow.first()}.callCanvas)}
 @Test fun emptyReadDoesNotWrite(){assertTrue(CallCanvas.read(ctx,number).strokes.isEmpty());assertFalse(file.exists())}
 @Test fun exactRoundTripAndInternationalIsolation(){val a=CallCanvas.save(ctx,number,"",listOf(row));assertEquals(a,CallCanvas.read(ctx,"+91 98765 43210"));assertTrue(CallCanvas.read(ctx,"+449876543210").strokes.isEmpty());assertEquals(a.raw,CallCanvas.encode(CallCanvas.decode(a.raw)))}
 @Test fun localShortSeparateFromInternational(){CallCanvas.save(ctx,"139","",listOf(row));assertTrue(CallCanvas.read(ctx,"+91139").strokes.isEmpty())}
 @Test fun invalidTargetsNeverWrite(){listOf("","private","12","+91x98765","+९१९८७६५४३२१०").forEach{bad{CallCanvas.read(ctx,it)}};assertFalse(file.exists())}
 @Test fun boundsStrictNumbersAndShapes(){bad{CallCanvas.Point(-1,0)};bad{CallCanvas.Point(0,10001)};listOf(row.copy(ink=4),row.copy(width=0),row.copy(points=emptyList()),row.copy(points=List(401){CallCanvas.Point(0,0)})).forEach{bad{it.validate()}}}
 @Test fun staleSavePreservesNewerDrawing(){val a=CallCanvas.save(ctx,number,"",listOf(row));val b=CallCanvas.save(ctx,number,a.raw,listOf(row.copy(ink=2)));bad{CallCanvas.save(ctx,number,a.raw,emptyList())};assertEquals(b,CallCanvas.read(ctx,number))}
 @Test fun corruptAndUtf8Preserved(){listOf("broken".toByteArray(),byteArrayOf(0xc3.toByte(),0x28)).forEach{bytes->file.writeBytes(bytes);bad{CallCanvas.save(ctx,number,"",listOf(row))};assertArrayEquals(bytes,file.readBytes())}}
 @Test fun schemaRejectsTrailingTypesAndDuplicates(){val raw=CallCanvas.encode(mapOf(CallCanvas.key(number)to listOf(row)));listOf(raw+"x",raw.replace("\"version\":1","\"version\":2"),raw.replace("\"ink\":1","\"ink\":1.5"),raw.replace("\"width\":2","\"width\":\"2\"")).forEach{bad{CallCanvas.decode(it)}};val o=org.json.JSONObject(raw);o.getJSONArray("numbers").put(o.getJSONArray("numbers").getJSONObject(0));bad{CallCanvas.decode(o.toString())}}
 @Test fun numberAndPointCapsPreserveStore(){val all=(1000000..1000029).associate{"local:$it" to listOf(row)};file.writeText(CallCanvas.encode(all));val before=file.readText();bad{CallCanvas.save(ctx,number,before,listOf(row))};assertEquals(before,file.readText());bad{CallCanvas.validate(List(81){row})};val dense=row.copy(points=List(400){CallCanvas.Point(5000,5000)});CallCanvas.validate(List(30){dense});bad{CallCanvas.validate(List(31){dense})}}
 @Test fun clearSaveRemovesOnlyChosenNumber(){val a=CallCanvas.save(ctx,number,"",listOf(row));val b=CallCanvas.save(ctx,"+449876543210",a.raw,listOf(row.copy(ink=3)));CallCanvas.save(ctx,number,b.raw,emptyList());assertTrue(CallCanvas.read(ctx,number).strokes.isEmpty());assertEquals(3,CallCanvas.read(ctx,"+449876543210").strokes.single().ink)}
 @Test fun historyUndoRedoBranchAndClear(){val h=CallCanvas.History(emptyList());assertFalse(h.canUndo);assertTrue(h.add(row));assertTrue(h.add(row.copy(ink=2)));h.undo();assertEquals(listOf(row),h.strokes);h.redo();assertEquals(2,h.strokes.size);h.undo();assertTrue(h.add(row.copy(ink=3)));assertFalse(h.canRedo);h.clear();assertTrue(h.strokes.isEmpty());assertFalse(h.canRedo)}
 @Test fun historyRejectsOverflowWithoutMutation(){val h=CallCanvas.History(List(80){row});assertFalse(h.add(row));assertEquals(80,h.strokes.size);h.undo();assertTrue(h.canRedo);assertFalse(h.add(row.copy(ink=9)));assertTrue(h.canRedo)}
 @Test fun backupAndTransferExcludeDrawingSidecars(){listOf("backup_rules","data_extraction_rules").forEach{name->val parser=ctx.resources.getXml(ctx.resources.getIdentifier(name,"xml",ctx.packageName));val paths=mutableListOf<String>();while(parser.next()!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT){if(parser.eventType==org.xmlpull.v1.XmlPullParser.START_TAG&&parser.name=="exclude"&&parser.getAttributeValue(null,"domain")=="file")paths+=parser.getAttributeValue(null,"path")};listOf("call_canvas.json","call_canvas.json.bak","call_canvas.json.new").forEach{path->assertEquals(if(name=="data_extraction_rules")2 else 1,paths.count{it==path})}}}
 @Test fun settingsFinderHasDirectRoute(){assertTrue(SettingsFinder.search("drawing sketch").any{it.route=="callcanvas"});assertEquals("callcanvas",SettingsFinder.route(SettingsFinder.all.single{it.id=="call-canvas"}))}
}
