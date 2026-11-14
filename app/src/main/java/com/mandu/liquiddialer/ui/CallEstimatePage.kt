package com.mandu.liquiddialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.CallEstimate
import com.mandu.liquiddialer.data.FeatureControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun CallEstimatePage(number:String,onBack:()->Unit)=CallEstimateWorkspace(number,onBack)
@Composable fun CallEstimateWorkspace(number:String,onBack:()->Unit,inSheet:Boolean=false){
 val features by com.mandu.liquiddialer.LdApp.settings.collectAsState()
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var saved by remember(number){mutableStateOf(CallEstimate.Draft())};var draft by remember(number){mutableStateOf(CallEstimate.Draft())}
 var loaded by remember(number){mutableStateOf(false)};var busy by remember(number){mutableStateOf(false)};var error by remember(number){mutableStateOf("")}
 var comparing by remember(number){mutableStateOf(false)};var splitting by remember(number){mutableStateOf(false)}
 var editor by remember(number){mutableStateOf<String?>(null)};var label by remember(number){mutableStateOf("")};var qty by remember(number){mutableStateOf("1")};var rate by remember(number){mutableStateOf("")}
 var discard by remember(number){mutableStateOf(false)};var discardReload by remember(number){mutableStateOf(false)};var clear by remember(number){mutableStateOf(false)};var remove by remember(number){mutableStateOf<String?>(null)}
 val dirty=draft!=saved||editor!=null
 fun back(){if(busy)return;if(dirty){discardReload=false;discard=true}else onBack()}
 fun load(){if(busy)return;busy=true;scope.launch{try{val d=withContext(Dispatchers.IO){CallEstimate.read(ctx,number)};saved=d;draft=d;loaded=true;error="";editor=null}catch(_:Exception){loaded=false;error="Saved estimate khul nahi raha. Purani file nahi badli."}finally{busy=false}}}
 LaunchedEffect(number){load()};BackHandler(enabled=!comparing&&!splitting){back()}
 if(comparing){EstimateCompareWorkspace(number,draft,{comparing=false},{next->draft=next;comparing=false},inSheet);return}
 if(splitting){EstimateSplitWorkspace(number,draft,{splitting=false},inSheet);return}
 val totals=runCatching{CallEstimate.totals(draft)}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,16f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("estimate_number"))
  LdText("Call par quantity aur rate ka hisab. Sirf estimate, invoice/payment ya tax advice nahi. Sab amounts ek hi currency mein bharo; koi conversion nahi.",13f,color=p.muted,maxLines=8)
  LdText("Local unencrypted draft. App backup mein nahi jaata; uninstall/data clear se mit jaata hai. 20 items per number, 100 numbers tak. Save dabane par hi draft rakha jaayega.",12f,color=p.muted,maxLines=8)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("estimate_error"))
  if(!loaded)PrimaryButton("Dobara load karo",enabled=!busy,modifier=Modifier.testTag("estimate_retry")){load()}
  if(loaded){
   if(editor!=null){
    SecTitle(if(editor=="new")"Naya item"else "Item badlo")
    LdField(label,{if(!busy)label=it},"Item ka naam",modifier=Modifier.testTag("estimate_label"))
    LdText("Quantity (3 decimals tak)",12f,color=p.muted)
    LdField(qty,{if(!busy)qty=it},"Quantity (3 decimals tak)",modifier=Modifier.testTag("estimate_quantity"))
    LdText("Ek unit ka rate (2 decimals)",12f,color=p.muted)
    LdField(rate,{if(!busy)rate=it},"Ek unit ka rate (2 decimals)",modifier=Modifier.testTag("estimate_rate"))
    val valid=runCatching{CallEstimate.line(label,qty,rate)}
    if(valid.isFailure)LdText("Naam 1-80 letters; quantity > 0 aur 10,00,000 tak; rate 0-1,00,00,00,000. Decimal ke liye dot, comma nahi.",12f,color=p.muted,maxLines=6)
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Item rakho",enabled=!busy&&valid.isSuccess,modifier=Modifier.testTag("estimate_item_apply")){
     val id=editor!!;val row=CallEstimate.line(label,qty,rate,if(id=="new")java.util.UUID.randomUUID().toString()else id)
     draft=draft.copy(lines=if(id=="new")draft.lines+row else draft.lines.map{if(it.id==id)row else it});editor=null
    }
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Item badlav chhodo",enabled=!busy,modifier=Modifier.testTag("estimate_item_cancel")){editor=null}
   }else{
    Spacer(Modifier.height(8.dp))
    LdText(if(draft==saved)"Saved draft"else "Unsaved changes",13f,color=p.muted,modifier=Modifier.testTag("estimate_status"))
    draft.lines.forEachIndexed{i,row->GlassCard(Modifier.padding(top=8.dp).testTag("estimate_item_${row.id}")){
     LdText(row.label,16f,FontWeight.SemiBold,maxLines=Int.MAX_VALUE)
     val amount=CallEstimate.money(java.math.BigDecimal(row.quantity).multiply(java.math.BigDecimal(row.rate)))
     LdText("${row.quantity} × ${row.rate} = $amount",14f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("estimate_amount_$i"))
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
      PrimaryButton("Badlo",enabled=!busy,modifier=Modifier.weight(1f).testTag("estimate_edit_${row.id}")){editor=row.id;label=row.label;qty=row.quantity;rate=row.rate}
      PrimaryButton("Mitao",enabled=!busy,modifier=Modifier.weight(1f).testTag("estimate_remove_${row.id}")){remove=row.id}
     }
    }}
    PrimaryButton("Item jodo",enabled=!busy&&draft.lines.size<CallEstimate.MAX_LINES,modifier=Modifier.testTag("estimate_add")){editor="new";label="";qty="1";rate=""}
    if(draft.lines.isEmpty())LdText("Abhi koi item nahi. Jaise: Chawal, quantity 2.5, rate 40.",13f,color=p.muted,maxLines=4)
    Spacer(Modifier.height(8.dp))
    LdText("Flat discount (amount, percent nahi)",12f,color=p.muted)
    LdField(draft.discount,{if(!busy)draft=draft.copy(discount=it)},"Flat discount (amount, percent nahi)",modifier=Modifier.testTag("estimate_discount"))
    LdText("Tax % (0-100, discount ke baad)",12f,color=p.muted)
    LdField(draft.tax,{if(!busy)draft=draft.copy(tax=it)},"Tax % (0-100, discount ke baad)",modifier=Modifier.testTag("estimate_tax"))
    totals.getOrNull()?.let{t->GlassCard(Modifier.padding(vertical=8.dp)){
     LdText("Subtotal: ${CallEstimate.money(t.subtotal)}",15f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("estimate_subtotal"))
     LdText("Discount: ${CallEstimate.money(t.discount)}",14f,maxLines=Int.MAX_VALUE)
     LdText("Tax base: ${CallEstimate.money(t.taxable)}",14f,maxLines=Int.MAX_VALUE)
     LdText("Tax: ${CallEstimate.money(t.tax)}",14f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("estimate_tax_amount"))
     LdText("Total: ${CallEstimate.money(t.total)}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("estimate_total"))
    }}?:LdText(totals.exceptionOrNull()?.message?:"Amounts sahi bharo",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("estimate_invalid"))
    if(FeatureControls.enabled(features,"compare")) PrimaryButton("What-if compare",enabled=!busy&&totals.isSuccess&&draft.lines.isNotEmpty(),modifier=Modifier.testTag("estimate_compare")){comparing=true}
    if(features.estimateSplit)PrimaryButton("Split estimate",enabled=!busy&&totals.isSuccess&&draft.lines.isNotEmpty(),modifier=Modifier.testTag("estimate_split")){splitting=true}
    LdText("Har item ka amount 2 decimals tak half-up round hota hai. Tax discount ke baad bachi rakam par, phir 2 decimals tak round. Apni tax rate khud check karo.",12f,color=p.muted,maxLines=6)
    Spacer(Modifier.height(8.dp))
    PrimaryButton(if(busy)"Save ho raha hai..."else "Draft save karo",enabled=!busy&&totals.isSuccess&&draft.lines.isNotEmpty()&&draft!=saved,modifier=Modifier.testTag("estimate_save")){
     busy=true;val value=draft;scope.launch{try{val d=withContext(Dispatchers.IO){CallEstimate.save(ctx,number,saved.revision,value)};saved=d;draft=d;error=""}catch(e:Exception){error=e.message?:"Save nahi hua. Purana draft nahi badla."}finally{busy=false}}
    }
    if(error.isNotEmpty())PrimaryButton("Saved draft dobara load karo",enabled=!busy,modifier=Modifier.testTag("estimate_reload")){if(dirty){discardReload=true;discard=true}else load()}
    Spacer(Modifier.height(8.dp))
    if(saved.lines.isNotEmpty()||draft.lines.isNotEmpty())PrimaryButton("Is number ka estimate mitao",enabled=!busy,modifier=Modifier.testTag("estimate_clear")){clear=true}
   }
  }
 }
 if(inSheet)Column{SheetTitle("Call calculator");PrimaryButton("Notes par wapas",enabled=!busy,modifier=Modifier.testTag("estimate_notes_back")){back()};content()}else PageFrame("Call calculator",{back()},content=content)
 if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Unsaved changes chhod dein?")},text={Text("Is baar ke badlav save nahi honge. Pehle se saved draft rahega.")},confirmButton={TextButton(onClick={discard=false;draft=saved;editor=null;if(discardReload){discardReload=false;load()}else onBack()},modifier=Modifier.testTag("estimate_discard")){Text("Chhod do")}},dismissButton={TextButton(onClick={discard=false},modifier=Modifier.testTag("estimate_keep")){Text("Ruko")}})
 remove?.let{id->AlertDialog(onDismissRequest={remove=null},title={Text("Item mitayein?")},text={Text("Item draft se hatega. Saved estimate tabhi badlega jab Draft save karo dabao.")},confirmButton={TextButton(onClick={draft=draft.copy(lines=draft.lines.filterNot{it.id==id});remove=null},modifier=Modifier.testTag("estimate_remove_confirm")){Text("Mitao")}},dismissButton={TextButton(onClick={remove=null}){Text("Rehne do")}})}
 if(clear)AlertDialog(onDismissRequest={if(!busy)clear=false},title={Text("Poora estimate mitayein?")},text={Text("Sirf is number ka saved draft aur unsaved badlav mitenge. Wapas nahi la sakte.")},confirmButton={TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{val d=withContext(Dispatchers.IO){CallEstimate.clear(ctx,number,saved.revision)};saved=d;draft=d;error="";clear=false}catch(e:Exception){error=e.message?:"Mita nahi";clear=false}finally{busy=false}}},modifier=Modifier.testTag("estimate_clear_confirm")){Text("Mitao")}},dismissButton={TextButton(enabled=!busy,onClick={clear=false},modifier=Modifier.testTag("estimate_clear_cancel")){Text("Rehne do")}})
}
