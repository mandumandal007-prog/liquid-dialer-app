package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun EstimateLinesWorkspace(number:String,base:CallEstimate.Draft,onBack:()->Unit,onApply:(CallEstimate.Draft)->Unit,inSheet:Boolean=false){
 val p=LocalLd.current;val focus=LocalFocusManager.current
 var raw by remember(number,base){mutableStateOf("")};var preview by remember(number,base){mutableStateOf<EstimateLines.Preview?>(null)}
 var error by remember(number,base){mutableStateOf("")};var discard by remember(number,base){mutableStateOf(false)};var confirm by remember(number,base){mutableStateOf(false)}
 fun back(){focus.clearFocus();if(raw.isNotEmpty())discard=true else onBack()};BackHandler{back()}
 val valid=runCatching{CallEstimate.totals(base)}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4)
  LdText("Several items. One preview.",22f,FontWeight.Bold,maxLines=4)
  LdText("Append to the calculator's current draft, which may be unsaved. Existing items are kept in order. Flat discount and tax percentage stay unchanged; the full total is recalculated. Save in the calculator is still separate.",13f,color=p.muted,maxLines=10)
  LdText("${base.lines.size} /20 existing items · ${20-base.lines.size} spaces",16f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("lines_capacity"))
  LdText("One real line per item: Name | Quantity | Rate",16f,FontWeight.Bold,maxLines=5)
  LdText("Example format only (not automatically added):\nRice | 2.5 | 40\nOil | 1 | 120",13f,color=p.muted,maxLines=7)
  LdText("Use the | separator, not commas. No headings, currency symbols or unit suffixes. Quantity: >0 to 1000000, up to 3 decimals. Rate: 0-1000000000, up to 2 decimals. Dot for decimals. Names: 1-80 characters, no |. Same units per item and same currency, no conversion.",12f,color=p.muted,maxLines=12)
  LdText("Manual item lines",13f,FontWeight.Bold)
  LdField(raw,{raw=it;preview=null;confirm=false;error=""},"Name | Quantity | Rate",minLines=4,modifier=Modifier.fillMaxWidth().testTag("lines_input"))
  LdText("${raw.length} /8000 characters. At most 40 physical lines, including blanks. Blank lines are ignored. Repeated names are separate items, NOT merged or skipped. All new items must fit the 20-item calculator limit; any invalid line refuses the whole batch.",12f,color=p.muted,maxLines=10)
  if(valid.isFailure)LdText(valid.exceptionOrNull()?.message?:"Invalid calculator draft",13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("lines_source_error"))
  PrimaryButton("Preview item lines",enabled=valid.isSuccess&&base.lines.size<20&&raw.isNotBlank(),modifier=Modifier.testTag("lines_preview")){focus.clearFocus();runCatching{EstimateLines.preview(base,raw)}.fold({preview=it;error=""},{preview=null;error=it.message?:"Check item lines"})}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("lines_error"))
  preview?.let{view->
   val before=CallEstimate.totals(view.source);val after=CallEstimate.totals(view.result)
   LdText("Preview · ${view.rows.size} new items",18f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("lines_result"))
   LdText("${view.blankLines} blank lines ignored. Every shown item will be appended after the existing ${base.lines.size}. Repeated names stay separate.",12f,color=p.muted,maxLines=6,modifier=Modifier.testTag("lines_blanks"))
   view.rows.forEachIndexed{i,r->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText("Input line ${r.sourceLine}",12f,color=p.muted)
    LdText(r.item.label,17f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("lines_name_$i"))
    val amount=java.math.BigDecimal(r.item.quantity)*java.math.BigDecimal(r.item.rate)
    LdText("${r.item.quantity} × ${r.item.rate} = ${CallEstimate.money(amount)}",19f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("lines_amount_$i"))
   }}
   GlassCard(Modifier.padding(top=8.dp)){
    LdText("Whole estimate before → after",17f,FontWeight.Bold,maxLines=4)
    LdText("Subtotal ${CallEstimate.money(before.subtotal)} → ${CallEstimate.money(after.subtotal)}",14f,maxLines=Int.MAX_VALUE)
    LdText("Flat discount ${CallEstimate.money(before.discount)} unchanged",14f,maxLines=Int.MAX_VALUE)
    LdText("Tax ${view.source.tax}%: ${CallEstimate.money(before.tax)} → ${CallEstimate.money(after.tax)}",14f,maxLines=Int.MAX_VALUE)
    LdText("Total ${CallEstimate.money(before.total)} → ${CallEstimate.money(after.total)}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("lines_total"))
   }
   PrimaryButton("Append to unsaved calculator",modifier=Modifier.testTag("lines_use")){confirm=true}
  }
  GhostButton("Return to calculator",Modifier.testTag("lines_back")){back()}
  LdText("No automatic clipboard reading or file import. Nothing is saved here. Confirm only changes the in-memory calculator; its Save button remains separate. No saved Undo: return without confirming to keep the draft. No stock, invoice, debt, payment, contact, message, call or cloud action.",12f,color=p.muted,maxLines=11)
 }
 if(inSheet)Column{SheetTitle("Calculator item lines");content()}else PageFrame("Calculator item lines",{back()},content=content)
 if(discard)Dialog(onDismissRequest={discard=false}){GlassCard{
  LdText("Discard these item lines?",18f,FontWeight.Bold,maxLines=5)
  LdText("Calculator and saved estimate stay unchanged.",13f,color=p.muted,maxLines=4)
  PrimaryButton("Discard item lines",modifier=Modifier.testTag("lines_discard")){discard=false;onBack()}
  GhostButton("Keep editing",Modifier.testTag("lines_keep")){discard=false}
 }}
 if(confirm&&preview!=null)Dialog(onDismissRequest={confirm=false}){GlassCard{
  LdText("Append ${preview!!.rows.size} new items?",18f,FontWeight.Bold,maxLines=5)
  LdText("Keep all ${base.lines.size} existing items and append the previewed items in order. Only the unsaved calculator changes. No saved Undo; Save remains separate.",13f,color=p.muted,maxLines=8)
  PrimaryButton("Confirm unsaved append",modifier=Modifier.testTag("lines_confirm")){runCatching{EstimateLines.apply(base,preview!!)}.fold({confirm=false;onApply(it)},{confirm=false;preview=null;error=it.message?:"Preview again"})}
  GhostButton("Cancel",Modifier.testTag("lines_cancel")){confirm=false}
 }}
}
@Composable fun EstimateLinesPage(onBack:()->Unit){
 val settings by com.mandu.liquiddialer.LdApp.settings.collectAsState();val scope=rememberCoroutineScope();val p=LocalLd.current;var error by remember{mutableStateOf("")}
 PageFrame("Calculator multi-line items",onBack){
  LdText("Add several manual items in one pass.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Calculator multi-line items","Default OFF. Validate and preview item lines before an unsaved append.",settings.estimateLines,last=true){scope.launch{try{com.mandu.liquiddialer.LdApp.instance.repo.set(K.estimateLines,it);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Open a number's Call calculator, then Add item lines. Format: Name | Quantity | Rate, one item per actual line. Preview every item and full total, then confirm append to the current unsaved draft. Existing items, discount and tax percentage are kept; calculatorSave is separate. Blank lines ignored, repeated names added separately, no truncation or automatic merging.8000 characters, 40 physical lines, 20 total calculator items. Invalid line or capacity refuses the entire batch. No automatic clipboard, file import, stock, invoice, payment, contact, message or cloud action.",14f,color=p.muted,maxLines=24)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}
