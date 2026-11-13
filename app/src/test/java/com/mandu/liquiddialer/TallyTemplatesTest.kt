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
class TallyTemplatesTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private val file get()=File(ctx.filesDir,"tally_templates.json")
 private fun t(id:String="a",name:String="Weekly",labels:List<String> = listOf("Rice bags","Sugar bags"),step:Long=5)=TallyTemplates.Template(id,name,labels,step)
 private val rows get()=listOf(CallTally.Item("first","Rice bags",99),CallTally.Item("second","Sugar bags",25))
 private fun bad(f:()->Unit){assertTrue(runCatching(f).isFailure)}
 @Before fun clean(){listOf("",".bak",".new").forEach{File(file.path+it).delete()}}
 @Test fun emptyLibrary(){assertEquals(emptyList<TallyTemplates.Template>(),TallyTemplates.read(ctx).templates)}
 @Test fun exactRoundTripWithUnicode(){val a=listOf(t(name="दुकान की गिनती",labels=listOf("चावल bags","Tea boxes")));assertEquals(a,TallyTemplates.decode(TallyTemplates.encode(a)))}
 @Test fun saveStripsQuantitiesAndAllSourceIds(){val a=TallyTemplates.create(emptyList()," Week ",rows,"5","new");val raw=TallyTemplates.encode(a);assertFalse(raw.contains("99"));assertFalse(raw.contains("25"));assertFalse(raw.contains("first"));assertFalse(raw.contains("value"));assertEquals("Week",a.single().name)}
 @Test fun loadingCreatesOnlyZeroCountersAndKeepsOrder(){val a=TallyTemplates.board(t());assertEquals(listOf("Rice bags","Sugar bags"),a.map{it.label});assertTrue(a.all{it.value==0L});CallTally.validate(a)}
 @Test fun noAliasToMutableInput(){val a=rows.toMutableList();val t=TallyTemplates.create(emptyList(),"A",a,"1").single();a.clear();assertEquals(2,t.labels.size)}
 @Test fun duplicateTemplateNamesRefused(){bad{TallyTemplates.create(listOf(t())," WEEKLY ",rows,"5")}}
 @Test fun duplicateIdsRefused(){bad{TallyTemplates.encode(listOf(t(),t(name="Other")))}}
 @Test fun duplicateLabelsRefusedIgnoringCase(){bad{TallyTemplates.encode(listOf(t(labels=listOf("Rice bags","rice BAGS"))))}}
 @Test fun labelBoundsAndControls(){listOf("","a".repeat(61),"A\u0001B").forEach{s->bad{TallyTemplates.encode(listOf(t(labels=listOf(s))))}}}
 @Test fun rowCountBounds(){bad{TallyTemplates.encode(listOf(t(labels=emptyList())))};bad{TallyTemplates.encode(listOf(t(labels=(1..13).map{"Item $it"})))}}
 @Test fun stepBounds(){listOf(0L,-1L,1_000_001L).forEach{n->bad{TallyTemplates.encode(listOf(t(step=n)))}};assertEquals(1_000_000,TallyTemplates.decode(TallyTemplates.encode(listOf(t(step=1_000_000)))).single().step.toInt())}
 @Test fun createRejectsInvalidSessionOrStep(){bad{TallyTemplates.create(emptyList(),"A",emptyList(),"1")};bad{TallyTemplates.create(emptyList(),"A",rows,"1.5")};bad{TallyTemplates.create(emptyList(),"A",rows.map{it.copy(value=-1)},"1")}}
 @Test fun templateLimit(){val all=(1..20).map{t(id="i$it",name="Board $it")};assertEquals(20,TallyTemplates.decode(TallyTemplates.encode(all)).size);bad{TallyTemplates.create(all,"21",rows,"1")};bad{TallyTemplates.encode(all+t(id="last",name="Extra"))}}
 @Test fun strictJsonTypesVersionTrailingAndDuplicates(){val raw=TallyTemplates.encode(listOf(t()));listOf(raw+"x",raw.replace("\"version\":1","\"version\":2"),raw.replace("\"step\":5","\"step\":\"5\""),raw.replace("\"step\":5","\"step\":5.5"),raw.replace("\"Rice bags\"","123")).forEach{s->bad{TallyTemplates.decode(s)}}}
 @Test fun badIdsAndNonCanonicalNames(){listOf("../a","a b","a".repeat(81)).forEach{id->bad{TallyTemplates.encode(listOf(t(id=id)))}};bad{TallyTemplates.encode(listOf(t(name=" A ")))}}
 @Test fun explicitWriteAndReopen(){val next=TallyTemplates.write(ctx,"",listOf(t()));assertEquals(next,TallyTemplates.read(ctx));assertTrue(file.exists())}
 @Test fun staleSavePreservesNewerLibrary(){val old=TallyTemplates.read(ctx);val current=TallyTemplates.write(ctx,old.raw,listOf(t()));bad{TallyTemplates.write(ctx,old.raw,listOf(t(id="b",name="New")))};assertEquals(current,TallyTemplates.read(ctx))}
 @Test fun staleDeletePreservesNewerLibrary(){val old=TallyTemplates.write(ctx,"",listOf(t()));val now=TallyTemplates.write(ctx,old.raw,old.templates+t(id="b",name="Other"));bad{TallyTemplates.write(ctx,old.raw,emptyList())};assertEquals(now,TallyTemplates.read(ctx))}
 @Test fun corruptFileNeverOverwritten(){file.writeText("broken");bad{TallyTemplates.read(ctx)};bad{TallyTemplates.write(ctx,"broken",listOf(t()))};assertEquals("broken",file.readText())}
 @Test fun oversizeAndInvalidUtf8Rejected(){file.writeBytes(ByteArray(TallyTemplates.MAX_BYTES+1){32});bad{TallyTemplates.read(ctx)};file.writeBytes(byteArrayOf(0xc3.toByte(),0x28));bad{TallyTemplates.read(ctx)}}
 @Test fun removeOnlyChosenAndMissingFails(){val all=listOf(t(),t(id="b",name="Other"));assertEquals(listOf(all[1]),TallyTemplates.remove(all,"a"));bad{TallyTemplates.remove(all,"missing")}}
 @Test fun loadIsOneUndoableCounterEdit(){val b=CallTally.Board(rows);val next=TallyTemplates.board(t());b.change(next);assertEquals(next,b.rows);b.undo();assertEquals(rows,b.rows);b.redo();assertEquals(next,b.rows)}
 @Test fun longTemplateIdLoadsValidCounters(){CallTally.validate(TallyTemplates.board(t(id="a".repeat(80))))}
 @Test fun defaultOffAndFinder(){assertFalse(LdSettings().tallyTemplates);assertTrue(SettingsFinder.all.any{it.id=="call-tally"&&it.aliases.contains("template")})}
}
