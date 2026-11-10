package com.mandu.liquiddialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.*

@Composable fun EstimateCompareWorkspace(number:String,base:CallEstimate.Draft,onBack:()->Unit,onApply:(CallEstimate.Draft)->Unit,inSheet:Boolean=false){
 val p=LocalLd.current
 var factor by remember(number,base){mutableStateOf("1")};var discount by remember(number,base){mutableStateOf(base.discount)};var tax by remember(number,base){mutableStateOf(base.tax)}
 var discard by remember{mutableStateOf(false)};var confirm by remember{mutableStateOf(false)}
 val dirty=factor!="1"||discount!=base.discount||tax!=base.tax
 fun back(){if(dirty)discard=true else onBack()}
 BackHandler{back()}
 val result=remember(base,factor,discount,tax){runCatching{EstimateCompare.compare(base,factor,discount,tax)}}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4)
  LdText("What-if comparison. Base = calculator ka abhi wala draft, zaroori nahi saved ho. Yeh screen file nahi badalti; koi bill, payment ya message nahi.",13f,color=p.muted,maxLines=7)
  LdText("Har item ki quantity multiplier se badlegi; rates aur item names wahi rahenge. Flat discount aur tax neeche alag chuno. Currency conversion nahi.",12f,color=p.muted,maxLines=7)
  Spacer(Modifier.height(8.dp))
  LdText("Sab quantities × (multiplier)",12f,color=p.muted,maxLines=3)
  LdField(factor,{factor=it},"Sab quantities × (jaise 2 ya 0.5)",modifier=Modifier.testTag("compare_factor"))
  LdText("Alternate flat discount (amount)",12f,color=p.muted,maxLines=3)
  LdField(discount,{discount=it},"Alternate flat discount",modifier=Modifier.testTag("compare_discount"))
  LdText("Alternate tax % (0-100)",12f,color=p.muted,maxLines=3)
  LdField(tax,{tax=it},"Alternate tax %",modifier=Modifier.testTag("compare_tax"))
  result.getOrNull()?.let{r->
   GlassCard(Modifier.padding(vertical=10.dp).testTag("compare_summary")){
    SecTitle("Base / alternate")
    LdText("Base total: ${CallEstimate.money(r.base.total)}",16f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("compare_base_total"))
    LdText("Alternate total: ${CallEstimate.money(r.alternative.total)}",19f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("compare_alt_total"))
    LdText("${EstimateCompare.direction(r)}: ${CallEstimate.money(r.difference.abs())}",16f,FontWeight.Bold,color=p.accent,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("compare_difference"))
    Spacer(Modifier.height(8.dp))
    LdText("Subtotal: ${CallEstimate.money(r.base.subtotal)} / ${CallEstimate.money(r.alternative.subtotal)}",13f,maxLines=Int.MAX_VALUE)
    LdText("Discount: ${CallEstimate.money(r.base.discount)} / ${CallEstimate.money(r.alternative.discount)}",13f,maxLines=Int.MAX_VALUE)
    LdText("Tax: ${CallEstimate.money(r.base.tax)} / ${CallEstimate.money(r.alternative.tax)}",13f,maxLines=Int.MAX_VALUE)
   }
   PrimaryButton("Draft mein rakho",enabled=r.draft!=base,modifier=Modifier.testTag("compare_apply")){confirm=true}
   Spacer(Modifier.height(8.dp))
   LdText("Is button ke baad bhi saved estimate nahi badlega. Calculator mein Draft save karo dabana hoga.",12f,color=p.muted,maxLines=6)
   SecTitle("Har item: base / alternate")
   base.lines.zip(r.draft.lines).forEachIndexed{i,(a,b)->GlassCard(Modifier.padding(top=8.dp)){
    LdText(a.label,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
    LdText("Quantity: ${a.quantity} / ${b.quantity} · rate ${a.rate}",13f,maxLines=Int.MAX_VALUE)
    LdText("Amount: ${CallEstimate.money(r.base.lineAmounts[i])} / ${CallEstimate.money(r.alternative.lineAmounts[i])}",14f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("compare_line_$i"))
   }}
  }?:LdText(result.exceptionOrNull()?.message?:"Amounts sahi bharo",13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("compare_error"))
  Spacer(Modifier.height(10.dp))
  LdText("Quantity 3 decimals tak, rate 2. Multiplier se zyada decimals aayen to reject, chupke se round nahi. Item amount aur tax 2 decimals half-up round. Apni tax rate khud check karo.",12f,color=p.muted,maxLines=8)
  PrimaryButton("Base par reset",enabled=dirty,modifier=Modifier.testTag("compare_reset")){factor="1";discount=base.discount;tax=base.tax}
 }
 if(inSheet)Column{SheetTitle("Estimate compare");PrimaryButton("Calculator par wapas",modifier=Modifier.testTag("compare_back")){back()};content()}else PageFrame("Estimate compare",{back()}){PrimaryButton("Calculator par wapas",modifier=Modifier.testTag("compare_back")){back()};content()}
 if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Comparison chhod dein?")},text={Text("Alternate inputs chhoot jayenge. Calculator ka base draft aur saved estimate nahi badlenge.")},confirmButton={TextButton(onClick={discard=false;onBack()},modifier=Modifier.testTag("compare_discard")){Text("Chhod do")}},dismissButton={TextButton(onClick={discard=false},modifier=Modifier.testTag("compare_keep")){Text("Ruko")}})
 if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Alternate draft mein rakhein?")},text={Text("Calculator ka abhi wala unsaved draft replace hoga. Saved estimate nahi badlega jab tak Draft save karo nahi dabate.")},confirmButton={TextButton(onClick={result.getOrNull()?.let{confirm=false;onApply(it.draft)}},modifier=Modifier.testTag("compare_confirm")){Text("Draft mein rakho")}},dismissButton={TextButton(onClick={confirm=false},modifier=Modifier.testTag("compare_cancel_apply")){Text("Rehne do")}})
}
