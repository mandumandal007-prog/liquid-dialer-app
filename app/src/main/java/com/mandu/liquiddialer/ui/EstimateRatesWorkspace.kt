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

@Composable fun EstimateRatesWorkspace(number:String,base:CallEstimate.Draft,onBack:()->Unit,onApply:(CallEstimate.Draft)->Unit,inSheet:Boolean=false){
 val enabled by com.mandu.liquiddialer.LdApp.settings.collectAsState()
 val p=LocalLd.current;val focus=LocalFocusManager.current
 var selected by remember(number,base){mutableStateOf(emptySet<String>())}
 var percent by remember(number,base){mutableStateOf("10")};var decrease by remember(number,base){mutableStateOf(false)}
 var preview by remember(number,base){mutableStateOf<EstimateRates.Preview?>(null)}
 var error by remember(number,base){mutableStateOf("")};var dirty by remember(number,base){mutableStateOf(false)}
 var discard by remember(number,base){mutableStateOf(false)};var confirm by remember(number,base){mutableStateOf(false)}
 LaunchedEffect(enabled.estimateRates){if(!enabled.estimateRates){selected=emptySet();percent="10";decrease=false;preview=null;confirm=false;discard=false;dirty=false;error=""}}
 if(!enabled.estimateRates){Column{LdText("Rate adjustment is OFF. No changes applied.",14f,maxLines=4);GhostButton("Return to calculator"){onBack()}};return}
 fun invalidate(){preview=null;confirm=false;error="";dirty=true}
 fun back(){focus.clearFocus();if(dirty)discard=true else onBack()};BackHandler{back()}
 val valid=runCatching{require(base.lines.isNotEmpty()){"Add an estimate item first"};CallEstimate.totals(base)}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4)
  LdText("Adjust rates. Keep quantities.",22f,FontWeight.Bold,maxLines=4)
  LdText("Uses your manual calculator rates, not live prices. Select items, choose Increase or Decrease and enter a percentage. Example: Increase 10% changes rate 10.00 to 11.00. Current draft may be unsaved.",13f,color=p.muted,maxLines=10)
  LdText("Quantities, item names, flat discount and tax percentage stay unchanged. New rates must be exact to 2 decimals, with no silent cent rounding. Preview recalculates line amounts and full total. Reduced subtotal below flat discount is refused. Save remains separate.",13f,color=p.muted,maxLines=10)
  if(valid.isFailure)LdText(valid.exceptionOrNull()?.message?:"Invalid estimate",13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("rates_source_error"))
  LdText("${selected.size} / ${base.lines.size} items selected",16f,FontWeight.Bold,modifier=Modifier.testTag("rates_count"))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   PrimaryButton("Select all",enabled=valid.isSuccess,modifier=Modifier.weight(1f).testTag("rates_all")){selected=base.lines.map{it.id}.toSet();invalidate()}
   PrimaryButton("Clear selection",modifier=Modifier.weight(1f).testTag("rates_none")){selected=emptySet();invalidate()}
  }
  base.lines.forEach{r->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
   LdText(r.label,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
   LdText("${r.quantity} × rate ${r.rate}",14f,maxLines=Int.MAX_VALUE)
   PrimaryButton(if(r.id in selected)"✓ Selected"else"Select item",enabled=valid.isSuccess,modifier=Modifier.testTag("rates_select_${r.id}")){selected=if(r.id in selected)selected-r.id else selected+r.id;invalidate()}
  }}
  LdText("Direction",15f,FontWeight.Bold)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   PrimaryButton(if(!decrease)"Increase ✓"else"Increase",modifier=Modifier.weight(1f).testTag("rates_increase")){decrease=false;invalidate()}
   PrimaryButton(if(decrease)"Decrease ✓"else"Decrease",modifier=Modifier.weight(1f).testTag("rates_decrease")){decrease=true;invalidate()}
  }
  LdText("Percentage (at most 2 decimals)",12f,color=p.muted)
  LdField(percent,{percent=it;invalidate()},"Percentage",modifier=Modifier.testTag("rates_percent"))
  LdText("Increase: >0 to 1000%. Decrease: >0 to 100%. Rate limit 1000000000. A 100% decrease sets selected rates to zero. One invalid rate refuses the entire change. Quantities and units stay unchanged.",12f,color=p.muted,maxLines=10)
  PrimaryButton("Preview rate changes",enabled=valid.isSuccess&&selected.isNotEmpty(),modifier=Modifier.testTag("rates_preview")){focus.clearFocus();runCatching{EstimateRates.preview(base,selected,percent,decrease)}.fold({preview=it;error=""},{preview=null;error=it.message?:"Check selection and percentage"})}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("rates_error"))
  preview?.let{view->
   val before=CallEstimate.totals(view.source);val after=CallEstimate.totals(view.result)
   LdText("Preview · ${view.changes.size} selected items",18f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("rates_result"))
   view.changes.forEach{r->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText(r.label,17f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
    LdText("Rate ${r.before} → ${r.after}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("rates_rate_${r.id}"))
    LdText("Quantity ${r.quantity} unchanged",13f,color=p.muted,maxLines=Int.MAX_VALUE)
    val old=java.math.BigDecimal(r.quantity)*java.math.BigDecimal(r.before);val next=java.math.BigDecimal(r.quantity)*java.math.BigDecimal(r.after)
    LdText("Line amount ${CallEstimate.money(old)} → ${CallEstimate.money(next)}",15f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("rates_amount_${r.id}"))
   }}
   GlassCard(Modifier.padding(top=8.dp)){
    LdText("Whole estimate before → after",17f,FontWeight.Bold,maxLines=4)
    LdText("Subtotal ${CallEstimate.money(before.subtotal)} → ${CallEstimate.money(after.subtotal)}",14f,maxLines=Int.MAX_VALUE)
    LdText("Flat discount ${CallEstimate.money(before.discount)} unchanged",14f,maxLines=Int.MAX_VALUE)
    LdText("Tax ${view.source.tax}%: ${CallEstimate.money(before.tax)} → ${CallEstimate.money(after.tax)}",14f,maxLines=Int.MAX_VALUE)
    LdText("Total ${CallEstimate.money(before.total)} → ${CallEstimate.money(after.total)}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("rates_total"))
   }
   PrimaryButton("Use in unsaved calculator",modifier=Modifier.testTag("rates_use")){confirm=true}
  }
  GhostButton("Return to calculator",Modifier.testTag("rates_back")){back()}
  LdText("Confirm changes only the calculator's in-memory draft. Nothing is saved until you press its Draft save button. No saved Undo here: return without confirming to keep the original. No invoice, debt, payment, clipboard, contact, message or call action.",12f,color=p.muted,maxLines=10)
 }
 if(inSheet)Column{SheetTitle("Adjust estimate rates");content()}else PageFrame("Adjust estimate rates",{back()},content=content)
 if(discard)Dialog(onDismissRequest={discard=false}){GlassCard{
  LdText("Discard rate adjustment inputs?",18f,FontWeight.Bold,maxLines=5)
  LdText("Calculator and saved draft stay unchanged.",13f,color=p.muted,maxLines=4)
  PrimaryButton("Discard adjustment",modifier=Modifier.testTag("rates_discard")){discard=false;onBack()}
  GhostButton("Keep editing",Modifier.testTag("rates_keep")){discard=false}
 }}
 if(confirm&&preview!=null)Dialog(onDismissRequest={confirm=false}){GlassCard{
  LdText("Replace selected draft rates?",18f,FontWeight.Bold,maxLines=5)
  LdText("${preview!!.changes.size} selected items. This only changes the unsaved calculator. Quantities, discount and tax percentage are unchanged. No saved Undo; Save is separate.",13f,color=p.muted,maxLines=8)
  PrimaryButton("Confirm unsaved changes",modifier=Modifier.testTag("rates_confirm")){runCatching{require(com.mandu.liquiddialer.LdApp.settings.value.estimateRates){"Rate adjustment is OFF"};EstimateRates.apply(base,preview!!)}.fold({confirm=false;onApply(it)},{confirm=false;preview=null;error=it.message?:"Preview again"})}
  GhostButton("Cancel",Modifier.testTag("rates_cancel")){confirm=false}
 }}
}
@Composable fun EstimateRatesPage(onBack:()->Unit){
 val settings by com.mandu.liquiddialer.LdApp.settings.collectAsState();val scope=rememberCoroutineScope();val p=LocalLd.current;var error by remember{mutableStateOf("")}
 PageFrame("Calculator rate adjustment",onBack){
  LdText("One percentage. Chosen rates.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Calculator rate adjustment","Default OFF. Preview percentage changes to selected manual rates.",settings.estimateRates,last=true){scope.launch{try{com.mandu.liquiddialer.LdApp.instance.repo.set(K.estimateRates,it);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Open a number's Call calculator with valid items, then Adjust selected rates. Nothing preselected. Choose items, Increase/Decrease and a positive percentage with at most 2 decimals. Increase up to 1000%, decrease up to 100%. Preview each old/new rate, line amount and whole total. Confirm into the unsaved calculator; Draft save remains separate.",14f,color=p.muted,maxLines=22)
  GlassCard{LdText("No silent rate rounding. If any new rate needs more than 2 decimal places or exceeds the rate limit, the entire change is refused. Quantities, names, flat discount and tax percentage stay unchanged. A 100% decrease makes selected rates zero; a total below the flat discount is refused. Calculator line/tax totals still use its existing half-up cent rounding.",13f,maxLines=20)}
  LdText("No saved Undo. Cancel or discard before confirmation to keep the calculator unchanged. OFF removes rate adjustment without changing an already applied unsaved draft. Local manual arithmetic, not live prices, tax advice, an invoice, debt or payment. No recording, network, clipboard, contact, message, call or new permission.",13f,color=p.muted,maxLines=18)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}
