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
class CsvCallImportTest {
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 @Before fun clear(){android.util.AtomicFile(File(ctx.filesDir,"manual_call_lists.json")).delete()}
 private fun reject(f:()->Unit){var rejected=false;try{f()}catch(_:Exception){rejected=true};assertTrue("Must reject",rejected)}
 @Test fun quotedCommasEscapesMultilineBomAndTrailingEmptyCells(){
  val t=CsvCallImport.parse("\uFEFFName,Phone,Note\r\n\"Office, A\",\"+44 2025550123\",\"line 1\r\nline \"\"2\"\"\"\r\nघर 👪,0091 9876543210,\r\n")
  assertEquals(3,t.rows.size);assertEquals(3,t.columns);assertEquals("line 1\r\nline \"2\"",t.rows[1][2]);assertEquals("",t.rows[2][2])
  val p=CsvCallImport.preview(t,1,0);assertEquals(2,p.size);assertEquals("Office, A",p[0].name);assertEquals("0091 9876543210",p[1].number)
 }
 @Test fun explicitDelimitersRaggedBlankAndHeaderless(){
  val t=CsvCallImport.parse("123;First\n\n456\n789;Third;Extra",';');assertEquals(3,t.columns)
  val p=CsvCallImport.preview(t,0,1,false);assertEquals(listOf(1,3,4),p.map{it.record});assertEquals("",p[1].name)
  assertEquals(listOf("123","456"),CsvCallImport.preview(CsvCallImport.parse("123\tA\n456\tB",'\t'),0,1,false).map{it.number})
  assertEquals(1,CsvCallImport.parse("123\r456").columns)
 }
 @Test fun strictMalformedQuotesNulAndLimitsRejectWholeFile(){
  listOf("\"123","ab\"cd,123","\"a\"bad,123","123\u0000", "", "\n  \n", "x".repeat(4097),(1..21).joinToString(","){"x"},(1..502).joinToString("\n"){"123"},"x".repeat(131073)).forEach{reject{CsvCallImport.parse(it)}}
  reject{CsvCallImport.parse("123",'|')};reject{CsvCallImport.read(ByteArray(131073).inputStream())};reject{CsvCallImport.read(byteArrayOf(0xC3.toByte(),0x28).inputStream())}
 }
 @Test fun preservesZerosPlusAndFullCountryIdentityWithoutExcelGuessing(){
  val p=CsvCallImport.preview(CsvCallImport.parse("+1 2025550123\n+44 2025550123\n001 2025550123\n012345\n=123\n1.23E+10\n*123#\n'9876543210\n++123\n123abc\n+919876543210\n9876543210"),0,null,false)
  assertEquals(CsvCallImport.Status.DUPLICATE,p[2].status);assertEquals("012345",p[3].number)
  assertEquals(6,p.count{it.status==CsvCallImport.Status.INVALID});assertEquals(CsvCallImport.Status.DUPLICATE,p.last().status)
  assertEquals(CsvCallImport.Status.READY,p[1].status)
 }
 @Test fun mappingRejectsSameAndMissingColumnsAndReportsLongNames(){
  val t=CsvCallImport.parse("Phone,Name\n123,"+"x".repeat(101)+"\n,Missing")
  reject{CsvCallImport.preview(t,0,0)};reject{CsvCallImport.preview(t,4,null)}
  assertTrue(CsvCallImport.preview(t,0,1).all{it.status==CsvCallImport.Status.INVALID})
 }
 @Test fun selectedSavePreservesOldProgressNamesIdentityAndNoAutomaticCompletion(){
  val id=CallLists.create(ctx,"Keep").single().id;CallLists.add(ctx,id,"123","Saved");val entry=CallLists.read(ctx).single().entries.single();CallLists.followUp(ctx,id,entry.id,"done","Private note",1234);CallLists.schedule(ctx,id,entry.id,2345)
  val before=CallLists.read(ctx)
  val p=CsvCallImport.preview(CsvCallImport.parse("Phone,Name\n+44 2025550123,Office\n+1 2025550123,Family\n123,Keep copy"),0,1)
  CallLists.createFromCsv(ctx,"Imported",listOf(p[1],p[2]));val after=CallLists.read(ctx)
  assertEquals(before.single(),after.first());assertEquals(listOf("Family","Keep copy"),after.last().entries.map{it.name})
  assertTrue(after.last().entries.all{!it.done&&it.outcome=="pending"&&it.note==""&&it.dueAt==0L});assertNotEquals(before.single().id,after.last().id)
 }
 @Test fun failedSaveRevalidationAndListCapacityKeepExactStoredBytes(){
  CallLists.create(ctx,"Keep");val stored=File(ctx.filesDir,"manual_call_lists.json");val bytes=stored.readBytes()
  val r=CsvCallImport.Row(1,"123","Name",CsvCallImport.Status.READY)
  for(rows in listOf(emptyList(),listOf(r.copy(number="*123#")),listOf(r,r),List(201){r.copy(number="123$it")},listOf(r.copy(status=CsvCallImport.Status.INVALID)),listOf(r.copy(name="n".repeat(101))))) {reject{CallLists.createFromCsv(ctx,"Bad",rows)};assertArrayEquals(bytes,stored.readBytes())}
  reject{CallLists.createFromCsv(ctx," ",listOf(r))};assertArrayEquals(bytes,stored.readBytes())
  repeat(19){CallLists.create(ctx,"More $it")};val full=stored.readBytes();reject{CallLists.createFromCsv(ctx,"Extra",listOf(r))};assertArrayEquals(full,stored.readBytes())
 }
 @Test fun exactMaxBoundsAndTextFormulaNamesAreData(){
  val t=CsvCallImport.parse((0..500).joinToString("\n"){"123$it"});assertEquals(501,t.rows.size)
  val r=CsvCallImport.preview(CsvCallImport.parse("123,=SUM(A1)"),0,1,false).single();CallLists.createFromCsv(ctx,"Data",listOf(r));assertEquals("=SUM(A1)",CallLists.read(ctx).single().entries.single().name)
  assertEquals("abc",CsvCallImport.read("abc".byteInputStream()))
 }
}
