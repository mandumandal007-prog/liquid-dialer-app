package com.mandu.liquiddialer
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],qualifiers="w360dp-h800dp-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
class CsvCallImportUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 private val ctx get()=ApplicationProvider.getApplicationContext<LdApp>()
 private var saved="";private var back=false
 private val csv="Name,Phone,Area\n\"Office, A\",+44 2025550123,UK\nFamily,+1 2025550123,US\nCopy,0044 2025550123,UK\nBad,1.23E+10,India\nघर 👪,09876543210,India"
 private fun settle(){rule.mainClock.advanceTimeBy(800);rule.waitForIdle()}
 private fun reveal(tag:String):SemanticsNodeInteraction{
  repeat(35){val n=rule.onNodeWithTag(tag,true);val r=n.getUnclippedBoundsInRoot();val sc=rule.onNodeWithTag("page_content_scroll",true);val v=sc.getUnclippedBoundsInRoot()
   if(r.bottom>r.top&&r.top>=v.top&&r.bottom<=v.bottom)return n
   sc.performTouchInput{if(r.top<v.top)swipeDown()else swipeUp()};settle()
  };saveShot(rule,"CSV375-failure");error("Not visible $tag")
 }
 private fun click(tag:String){reveal(tag).performClick();settle()}
 @Before fun clear(){android.util.AtomicFile(java.io.File(ctx.filesDir,"manual_call_lists.json")).delete()}
 private fun show(text:String?=csv,theme:String="daylight",font:Float=1f,workspace:Boolean=false){
  rule.mainClock.autoAdvance=false;settle();runBlocking{ctx.ldStore.edit{it[K.onboarded]=true;it[K.theme]=theme;it[K.fontScale]=font;it[K.confirmCall]=true}}
  rule.waitUntil(15000){LdApp.settings.value.let{it.theme==theme&&it.fontScale==font&&it.confirmCall}}
  rule.runOnUiThread{rule.activity.setContent{LdThemeHost{if(workspace)CallListsPage(rule.activity.state,rule.activity,{back=true})else CsvCallImportPage({back=true},{saved=it},text)}}};settle()
 }
 private fun mapAndReview(){click("csv_column_phone");rule.onNodeWithTag("csv_phone_1").performClick();settle();click("csv_column_name");rule.onNodeWithTag("csv_name_0").performClick();settle();click("csv_review")}
 @Test fun mapReviewSubsetSaveAndPreserveExisting(){
  val old=CallLists.create(ctx,"Old");show();saveShot(rule,"CSV375-file-day");mapAndReview()
  reveal("csv_counts").assertTextEquals("3 valid · 1 invalid · 1 duplicate");saveShot(rule,"CSV375-review-day")
  click("csv_select_valid");click("csv_check_3");reveal("csv_selected").assertTextEquals("2 rows chuni · max 200")
  reveal("csv_save").assertIsNotEnabled();reveal("csv_title").performTextInput("Imported calls");settle();saveShot(rule,"CSV375-save-day");click("csv_save")
  rule.waitUntil(15000){saved.isNotEmpty()};assertEquals(old.single(),CallLists.read(ctx).first());val list=CallLists.read(ctx).last();assertEquals(listOf("Office, A","घर 👪"),list.entries.map{it.name});assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun remapClearsReviewAndSelectionAndLargeNightPixels(){
  show(theme="midnight",font=1.3f);mapAndReview();click("csv_select_valid");saveShot(rule,"CSV375-review-night-large");click("csv_remap");click("csv_column_phone");rule.onNodeWithTag("csv_phone_0").performClick();settle();click("csv_review");reveal("csv_error").assertTextEquals("Naam aur number ke liye alag columns chuno");assertTrue(CallLists.read(ctx).isEmpty())
 }
 @Test fun delimiterAndHeaderCanBeChangedNoAutomaticSave(){
  show("123;One\n456;Two");click("csv_sep_59");click("csv_header");click("csv_column_phone");rule.onNodeWithTag("csv_phone_0").performClick();settle();click("csv_review");reveal("csv_counts").assertTextEquals("2 valid · 0 invalid · 0 duplicate");reveal("csv_selected").assertTextEquals("0 rows chuni · max 200");assertTrue(CallLists.read(ctx).isEmpty())
 }
 @Test fun malformedFileShowsErrorWithoutReviewOrWrite(){show("\"broken");reveal("csv_error").assertTextEquals("CSV mein quote band nahi hai");rule.onNodeWithTag("csv_review").assertDoesNotExist();assertTrue(CallLists.read(ctx).isEmpty());saveShot(rule,"CSV375-invalid-day")}
 @Test fun moreRowsAndSelectionCapAreExplicit(){
  show((1..205).joinToString("\n"){"123$it"});click("csv_header");click("csv_column_phone");rule.onNodeWithTag("csv_phone_0").performClick();settle();click("csv_review");click("csv_select_valid");reveal("csv_selected").assertTextEquals("200 rows chuni · max 200");click("csv_more");rule.onNodeWithTag("csv_check_21").assertExists();click("csv_clear");reveal("csv_selected").assertTextEquals("0 rows chuni · max 200");assertTrue(CallLists.read(ctx).isEmpty())
 }
 private fun pick(bytes:ByteArray?,cancel:Boolean=false){
  click("csv_pick")
  val activity=org.robolectric.Shadows.shadowOf(rule.activity)
  val started=activity.nextStartedActivityForResult
  assertEquals(android.content.Intent.ACTION_OPEN_DOCUMENT,started.intent.action)
  assertEquals("*/*",started.intent.type)
  assertTrue(started.intent.getStringArrayExtra(android.content.Intent.EXTRA_MIME_TYPES)!!.contains("text/*"))
  val uri=android.net.Uri.parse("content://csv-tests/rows.csv")
  if(bytes!=null)org.robolectric.Shadows.shadowOf(ctx.contentResolver).registerInputStream(uri,bytes.inputStream())
  rule.runOnUiThread{activity.receiveResult(started.intent,if(cancel)android.app.Activity.RESULT_CANCELED else android.app.Activity.RESULT_OK,android.content.Intent().setData(uri))};settle()
 }
 @Test fun documentPickerToWorkspaceSaveManualCallAndProgress(){
  val old=CallLists.create(ctx,"Existing").single();show(null,workspace=true)
  rule.waitUntil(15000){settle();rule.onAllNodesWithTag("lists_csv").fetchSemanticsNodes().isNotEmpty()};click("lists_csv")
  pick(csv.toByteArray());rule.waitUntil(15000){settle();rule.onAllNodesWithTag("csv_shape",true).fetchSemanticsNodes().isNotEmpty()}
  mapAndReview();click("csv_check_2");reveal("csv_title").performTextInput("Imported through picker");settle();click("csv_save")
  rule.waitUntil(15000){settle();CallLists.read(ctx).size==2&&rule.onAllNodesWithTag("list_add").fetchSemanticsNodes().isNotEmpty()}
  val list=CallLists.read(ctx).last();assertEquals(old,CallLists.read(ctx).first());val entry=list.entries.single();assertEquals("+44 2025550123",entry.number);assertEquals("Office, A",entry.name)
  assertNull(rule.activity.state.confirmNumber);assertFalse(entry.done);saveShot(rule,"CSV375-saved-workspace")
  click("list_call_${entry.id}");assertEquals(entry.number,rule.activity.state.confirmNumber);assertEquals(entry,CallLists.read(ctx).last().entries.single())
  click("list_session");reveal("session_number").assertTextEquals(entry.number);reveal("session_name").assertTextEquals(entry.name)
  click("session_call");assertEquals(entry.number,rule.activity.state.confirmNumber);assertFalse(CallLists.read(ctx).last().entries.single().done)
  click("session_outcome_later");reveal("session_note").performTextInput("Try tomorrow");settle();click("session_save")
  rule.waitUntil(15000){settle();CallLists.read(ctx).last().entries.single().outcome=="later"};val changed=CallLists.read(ctx).last().entries.single()
  assertEquals("Try tomorrow",changed.note);assertEquals(entry.number,changed.number);assertFalse(changed.done);assertEquals(old,CallLists.read(ctx).first());saveShot(rule,"CSV375-manual-session")
 }
 @Test fun documentPickerCancellationAndReadFailureNeverWrite(){
  show(null);pick(null,cancel=true);rule.onNodeWithTag("csv_shape",true).assertDoesNotExist();assertTrue(CallLists.read(ctx).isEmpty())
  pick(byteArrayOf(0xC3.toByte(),0x28));rule.waitUntil(15000){settle();rule.onAllNodesWithTag("csv_error",true).fetchSemanticsNodes().isNotEmpty()}
  reveal("csv_error").assertTextContains("UTF-8 CSV khul nahi paayi",substring=true);assertTrue(CallLists.read(ctx).isEmpty());assertNull(rule.activity.state.confirmNumber)
 }
 @Test fun storageCapacityErrorStaysVisibleAndSelectionPreserved(){
  repeat(20){CallLists.create(ctx,"Keep $it")};val bytes=java.io.File(ctx.filesDir,"manual_call_lists.json").readBytes()
  show();mapAndReview();click("csv_check_2");reveal("csv_title").performTextInput("Too many");settle();click("csv_save")
  rule.waitUntil(15000){settle();rule.onAllNodesWithTag("csv_error",true).fetchSemanticsNodes().isNotEmpty()}
  reveal("csv_error").assertTextEquals("Abhi 20 lists ki limit hai");reveal("csv_selected").assertTextEquals("1 rows chuni · max 200");assertArrayEquals(bytes,java.io.File(ctx.filesDir,"manual_call_lists.json").readBytes());saveShot(rule,"CSV375-save-error")
 }

}
