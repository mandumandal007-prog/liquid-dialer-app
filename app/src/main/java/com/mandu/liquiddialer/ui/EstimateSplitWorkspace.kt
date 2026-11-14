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

@Composable fun EstimateSplitWorkspace(number:String,base:CallEstimate.Draft,onBack:()->Unit,inSheet:Boolean=false){
 val p=LocalLd.current;val focus=LocalFocusManager.current
 var inputs by remember(number,base){mutableStateOf(listOf(EstimateSplit.Input("Share 1"),EstimateSplit.Input("Share 2")))}
 var equal by remember(number,base){mutableStateOf(true)};var preview by remember(number,base){mutableStateOf<EstimateSplit.Preview?>(null)}
 var error by remember(number,base){mutableStateOf("")};var dirty by remember(number,base){mutableStateOf(false)};var discard by remember(number,base){mutableStateOf(false)}
 fun invalidate(){preview=null;error="";dirty=true}
 fun back(){focus.clearFocus();if(dirty)discard=true else onBack()}
 BackHandler{back()}
 val total=runCatching{require(base.lines.isNotEmpty()){"Add an estimate item first"};CallEstimate.totals(base).total}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4)
  LdText("One estimate. Exact shares.",22f,FontWeight.Bold,maxLines=4)
  LdText("Uses the calculator's current draft, which may be unsaved. Preview only: no bill, debt, payment, invoice, message or saved split is created. Names are manual labels, not contacts. Same currency only; no conversion.",13f,color=p.muted,maxLines=10)
  if(total.isSuccess)LdText("Estimate total: ${CallEstimate.money(total.getOrThrow())}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("split_base"))else LdText(total.exceptionOrNull()?.message?:"Invalid estimate",13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("split_source_error"))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   PrimaryButton(if(equal)"✓ Equal"else"Equal",modifier=Modifier.weight(1f).testTag("split_equal")){equal=true;invalidate()}
   PrimaryButton(if(!equal)"✓ Weights"else"Weights",modifier=Modifier.weight(1f).testTag("split_weighted")){equal=false;invalidate()}
  }
  LdText(if(equal)"Each share has equal weight. Uneven last cents follow visible order."else"Weights are ratios, NOT percentages or amounts. Example: 1, 2, 1 splits into quarters, half, quarters. Use whole weights 1-1000000.",13f,color=p.muted,maxLines=8)
  LdText("${inputs.size} /12 named shares",15f,FontWeight.Bold,modifier=Modifier.testTag("split_count"))
  inputs.forEachIndexed{i,row->GlassCard(Modifier.fillMaxWidth().padding(top=8.dp)){
   LdText("Share ${i+1}",15f,FontWeight.Bold)
   LdField(row.name,{s->inputs=inputs.mapIndexed{j,v->if(j==i)v.copy(name=s)else v};invalidate()},"Share name",modifier=Modifier.testTag("split_name_$i"))
   if(!equal)LdField(row.weight,{s->inputs=inputs.mapIndexed{j,v->if(j==i)v.copy(weight=s)else v};invalidate()},"Whole weight",modifier=Modifier.testTag("split_weight_$i"))
   PrimaryButton("Remove share",enabled=inputs.size>2,modifier=Modifier.testTag("split_remove_$i")){if(inputs.size>2){focus.clearFocus();inputs=inputs.filterIndexed{j,_->j!=i};invalidate()}}
  }}
  LdText("At least 2 shares. Remove is inactive at 2.",12f,color=p.muted,maxLines=3)
  PrimaryButton("Add share",enabled=inputs.size<12,modifier=Modifier.testTag("split_add")){val name=(1..99).map{"Share $it"}.first{n->inputs.none{it.name.trim().equals(n,true)}};inputs=inputs+EstimateSplit.Input(name);invalidate()}
  PrimaryButton("Preview split",enabled=total.isSuccess,modifier=Modifier.testTag("split_preview")){focus.clearFocus();runCatching{EstimateSplit.preview(base,inputs,equal)}.fold({preview=it;error=""},{preview=null;error=it.message?:"Check names and weights"})}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("split_error"))
  preview?.let{view->
   LdText("Preview only · ${view.shares.size} shares",18f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("split_result"))
   LdText("Share sum: ${CallEstimate.money(view.shares.fold(java.math.BigDecimal.ZERO){a,b->a+b.amount})} · matches ${CallEstimate.money(view.total)}",16f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("split_sum"))
   view.shares.forEachIndexed{i,r->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText(r.name,17f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
    LdText("Weight ${r.weight}",13f,color=p.muted)
    LdText(CallEstimate.money(r.amount),24f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("split_amount_$i"))
    if(r.extraCent)LdText("Includes one leftover 0.01",12f,color=p.muted,maxLines=3,modifier=Modifier.testTag("split_extra_$i"))
   }}
  }
  LdText("Amounts are floored to 2 decimals, then leftover 0.01 units go to the largest fractional remainders. Ties use visible share order. This always preserves the displayed estimate total, not the unrounded item prices. Weight inputs are ignored in Equal mode and kept if you switch back.",12f,color=p.muted,maxLines=12)
  GhostButton("Return to calculator",Modifier.testTag("split_back")){back()}
  LdText("No Apply or Save: calculator items, discount, tax and its unfinished editor are untouched. Split inputs/preview are discarded on return or app/process close. Nothing goes to clipboard, calls, contacts or cloud.",12f,color=p.muted,maxLines=8)
 }
 if(inSheet)Column{SheetTitle("Split estimate");content()}else PageFrame("Split estimate",{back()},content=content)
 if(discard)Dialog(onDismissRequest={discard=false}){GlassCard{
  LdText("Discard this split preview and inputs?",18f,FontWeight.Bold,maxLines=5)
  LdText("Calculator draft and saved estimate stay unchanged.",13f,color=p.muted,maxLines=4)
  PrimaryButton("Discard split",modifier=Modifier.testTag("split_discard")){discard=false;onBack()}
  GhostButton("Keep split",Modifier.testTag("split_keep")){discard=false}
 }}
}

@Composable fun EstimateSplitPage(onBack:()->Unit){
 val settings by com.mandu.liquiddialer.LdApp.settings.collectAsState();val scope=rememberCoroutineScope();val p=LocalLd.current;var error by remember{mutableStateOf("")}
 PageFrame("Calculator split estimate",onBack){
  LdText("Preview shares, without changing the estimate.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Calculator split estimate","Default OFF. Read-only equal or weighted split of the current calculator total.",settings.estimateSplit,last=true){scope.launch{try{com.mandu.liquiddialer.LdApp.instance.repo.set(K.estimateSplit,it);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Open a number's Call calculator, add valid estimate items, then Split estimate. Choose 2-12 manual labels and Equal or whole Weights. Preview exact two-decimal shares. Leftover 0.01 units go by largest fractional remainder; ties follow share order. Inputs are temporary. No saved split, obligation, invoice, payment, contact, message or clipboard action. Same currency only.",14f,color=p.muted,maxLines=18)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}
