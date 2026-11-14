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

@Composable fun EstimateScaleWorkspace(number:String,base:CallEstimate.Draft,onBack:()->Unit,onApply:(CallEstimate.Draft)->Unit,inSheet:Boolean=false){
 val p=LocalLd.current;val focus=LocalFocusManager.current
 var selected by remember(number,base){mutableStateOf(emptySet<String>())}
 var numerator by remember(number,base){mutableStateOf("2")};var denominator by remember(number,base){mutableStateOf("1")}
 var preview by remember(number,base){mutableStateOf<EstimateScale.Preview?>(null)}
 var error by remember(number,base){mutableStateOf("")};var dirty by remember(number,base){mutableStateOf(false)}
 var discard by remember(number,base){mutableStateOf(false)};var confirm by remember(number,base){mutableStateOf(false)}
 fun invalidate(){preview=null;confirm=false;error="";dirty=true}
 fun back(){focus.clearFocus();if(dirty)discard=true else onBack()};BackHandler{back()}
 val valid=runCatching{require(base.lines.isNotEmpty()){"Add an estimate item first"};CallEstimate.totals(base)}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4)
  LdText("Scale quantities. Keep rates.",22f,FontWeight.Bold,maxLines=4)
  LdText("Uses the current calculator draft, which may be unsaved. Select items and multiply each quantity by a whole-number ratio. Example: 3 / 2 changes 2 to 3. No unit conversion or stock adjustment.",13f,color=p.muted,maxLines=10)
  LdText("Rates, item names, flat discount and tax percentage stay unchanged. Preview recalculates rounded line amounts and the full estimate total. A reduced subtotal below the flat discount is refused. Save in the calculator is still separate.",13f,color=p.muted,maxLines=10)
  if(valid.isFailure)LdText(valid.exceptionOrNull()?.message?:"Invalid estimate",13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("scale_source_error"))
  LdText("${selected.size} / ${base.lines.size} items selected",16f,FontWeight.Bold,modifier=Modifier.testTag("scale_count"))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   PrimaryButton("Select all",enabled=valid.isSuccess,modifier=Modifier.weight(1f).testTag("scale_all")){selected=base.lines.map{it.id}.toSet();invalidate()}
   PrimaryButton("Clear selection",modifier=Modifier.weight(1f).testTag("scale_none")){selected=emptySet();invalidate()}
  }
  base.lines.forEach{r->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
   LdText(r.label,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
   LdText("${r.quantity} × rate ${r.rate}",14f,maxLines=Int.MAX_VALUE)
   PrimaryButton(if(r.id in selected)"✓ Selected"else"Select item",enabled=valid.isSuccess,modifier=Modifier.testTag("scale_select_${r.id}")){selected=if(r.id in selected)selected-r.id else selected+r.id;invalidate()}
  }}
  LdText("Multiply selected quantities by numerator / denominator",15f,FontWeight.Bold,maxLines=5)
  LdText("Numerator (multiply by)",12f,color=p.muted)
  LdField(numerator,{numerator=it;invalidate()},"Whole numerator",modifier=Modifier.testTag("scale_numerator"))
  LdText("Denominator (divide by)",12f,color=p.muted)
  LdField(denominator,{denominator=it;invalidate()},"Whole denominator",modifier=Modifier.testTag("scale_denominator"))
  LdText("Both ratio numbers: 1-1000000. New quantities must be positive, at most 1000000, and exact within 3 decimal places. No quantity rounding: one invalid item refuses the whole change.",12f,color=p.muted,maxLines=9)
  PrimaryButton("Preview quantity changes",enabled=valid.isSuccess&&selected.isNotEmpty(),modifier=Modifier.testTag("scale_preview")){focus.clearFocus();runCatching{EstimateScale.preview(base,selected,numerator,denominator)}.fold({preview=it;error=""},{preview=null;error=it.message?:"Check selection and ratio"})}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("scale_error"))
  preview?.let{view->
   val before=CallEstimate.totals(view.source);val after=CallEstimate.totals(view.result)
   LdText("Preview · ${view.changes.size} selected items",18f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("scale_result"))
   view.changes.forEach{r->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText(r.label,17f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
    LdText("Quantity ${r.before} → ${r.after}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("scale_quantity_${r.id}"))
    LdText("Rate ${r.rate} unchanged",13f,color=p.muted,maxLines=Int.MAX_VALUE)
    val old=java.math.BigDecimal(r.before)*java.math.BigDecimal(r.rate);val next=java.math.BigDecimal(r.after)*java.math.BigDecimal(r.rate)
    LdText("Line amount ${CallEstimate.money(old)} → ${CallEstimate.money(next)}",15f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("scale_amount_${r.id}"))
   }}
   GlassCard(Modifier.padding(top=8.dp)){
    LdText("Whole estimate before → after",17f,FontWeight.Bold,maxLines=4)
    LdText("Subtotal ${CallEstimate.money(before.subtotal)} → ${CallEstimate.money(after.subtotal)}",14f,maxLines=Int.MAX_VALUE)
    LdText("Flat discount ${CallEstimate.money(before.discount)} unchanged",14f,maxLines=Int.MAX_VALUE)
    LdText("Tax ${view.source.tax}%: ${CallEstimate.money(before.tax)} → ${CallEstimate.money(after.tax)}",14f,maxLines=Int.MAX_VALUE)
    LdText("Total ${CallEstimate.money(before.total)} → ${CallEstimate.money(after.total)}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("scale_total"))
   }
   PrimaryButton("Use in unsaved calculator",modifier=Modifier.testTag("scale_use")){confirm=true}
  }
  GhostButton("Return to calculator",Modifier.testTag("scale_back")){back()}
  LdText("Confirm changes only the calculator's in-memory draft. Nothing is saved until you press its Draft save button. No saved Undo here: return without confirming to keep the original. No invoice, debt, payment, clipboard, contact, message or call action.",12f,color=p.muted,maxLines=10)
 }
 if(inSheet)Column{SheetTitle("Scale estimate quantities");content()}else PageFrame("Scale estimate quantities",{back()},content=content)
 if(discard)Dialog(onDismissRequest={discard=false}){GlassCard{
  LdText("Discard quantity scaling inputs?",18f,FontWeight.Bold,maxLines=5)
  LdText("Calculator and saved draft stay unchanged.",13f,color=p.muted,maxLines=4)
  PrimaryButton("Discard scaling",modifier=Modifier.testTag("scale_discard")){discard=false;onBack()}
  GhostButton("Keep editing",Modifier.testTag("scale_keep")){discard=false}
 }}
 if(confirm&&preview!=null)Dialog(onDismissRequest={confirm=false}){GlassCard{
  LdText("Replace selected draft quantities?",18f,FontWeight.Bold,maxLines=5)
  LdText("${preview!!.changes.size} selected items. This only changes the unsaved calculator. Rates, discount and tax percentage are unchanged. No saved Undo; Save is separate.",13f,color=p.muted,maxLines=8)
  PrimaryButton("Confirm unsaved changes",modifier=Modifier.testTag("scale_confirm")){runCatching{EstimateScale.apply(base,preview!!)}.fold({confirm=false;onApply(it)},{confirm=false;preview=null;error=it.message?:"Preview again"})}
  GhostButton("Cancel",Modifier.testTag("scale_cancel")){confirm=false}
 }}
}
@Composable fun EstimateScalePage(onBack:()->Unit){
 val settings by com.mandu.liquiddialer.LdApp.settings.collectAsState();val scope=rememberCoroutineScope();val p=LocalLd.current;var error by remember{mutableStateOf("")}
 PageFrame("Calculator quantity scaling",onBack){
  LdText("Resize selected quantities, not prices.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Calculator quantity scaling","Default OFF. Preview a ratio change on selected estimate quantities.",settings.estimateScale,last=true){scope.launch{try{com.mandu.liquiddialer.LdApp.instance.repo.set(K.estimateScale,it);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Open a number's Call calculator with valid items, then Scale quantities. Select items, enter whole numerator / denominator, preview each quantity and full total, and confirm into the unsaved draft. Save remains separate. Exact 3-decimal quantities only; invalid fractions or bounds refuse the entire change. Rates, names, flat discount and tax percentage are kept. Same units per item and same currency; no conversion, stock, invoice, payment, contact or clipboard action.",14f,color=p.muted,maxLines=22)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}
