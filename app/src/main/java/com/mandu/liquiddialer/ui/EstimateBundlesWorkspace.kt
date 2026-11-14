package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun EstimateBundlesWorkspace(number:String,base:CallEstimate.Draft,onBack:()->Unit,onApply:(CallEstimate.Draft)->Unit,inSheet:Boolean=false){
 val latestBase by rememberUpdatedState(base);val latestApply by rememberUpdatedState(onApply)
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 val parsed=remember(s.estimateBundleLibrary){runCatching{EstimateBundles.decode(s.estimateBundleLibrary)}};val rows=parsed.getOrDefault(emptyList());val valid=runCatching{CallEstimate.totals(base)}
 var selected by remember(number,base){mutableStateOf(emptySet<String>())};var editor by remember(number){mutableStateOf<String?>(null)};var name by remember(number){mutableStateOf("")};var editorRaw by remember{mutableStateOf("")}
 var error by remember{mutableStateOf("")};var notice by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var discard by remember{mutableStateOf(false)}
 var preview by remember(number,base){mutableStateOf<EstimateBundles.Plan?>(null)};var confirm by remember{mutableStateOf(false)};var deleting by remember{mutableStateOf<EstimateBundles.Bundle?>(null)};var deleteRaw by remember{mutableStateOf("")}
 fun back(){if(!busy){if(editor!=null)discard=true else onBack()}};BackHandler{back()}
 LaunchedEffect(s.estimateBundles){if(!s.estimateBundles){preview=null;confirm=false;deleting=null;editor=null;selected=emptySet()}}
 fun library(raw:String,change:(List<EstimateBundles.Bundle>)->List<EstimateBundles.Bundle>){if(busy)return;busy=true;scope.launch{try{ctx.ldStore.edit{prefs->require(prefs[K.estimateBundles]==true){"Item bundles are OFF"};require((prefs[K.estimateBundleLibrary]?:"")==raw){"Library changed. Open edit again"};prefs[K.estimateBundleLibrary]=EstimateBundles.encode(change(EstimateBundles.decode(raw)))};editor=null;name="";deleting=null;preview=null;confirm=false;error="";notice="Library saved. Calculator and saved estimates unchanged"}catch(e:Exception){error=e.message?:"Library save failed"}finally{busy=false}}}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4)
  LdText("Build once. Reuse after checking.",22f,FontWeight.Bold,maxLines=5)
  LdText("Phone-only item bundles shared across this app's calculators. Store only selected item names, quantities and rates. No caller number, discount, tax or estimate revision is copied. Do not put private customer details in item names. Unencrypted, not in backup; uninstall/data clear can erase the library.",13f,color=p.muted,maxLines=12)
  if(!s.estimateBundles)LdText("Item bundles OFF. Current calculator and library are kept. Enable under Settings > Calculator item bundles.",14f,maxLines=6,modifier=Modifier.testTag("bundle_off"))else{
   if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=10,modifier=Modifier.testTag("bundle_error"))
   if(notice.isNotEmpty())LdText(notice,13f,color=p.accent,maxLines=5,modifier=Modifier.testTag("bundle_notice"))
   if(parsed.isFailure)LdText("Library unreadable. Save, rename, delete and reuse are locked; existing data will not be overwritten.",14f,color=p.danger,maxLines=8,modifier=Modifier.testTag("bundle_corrupt"))
   if(valid.isFailure)LdText("Current calculator is invalid. Return and check its amounts before saving or reusing items.",13f,color=p.danger,maxLines=7)
   if(parsed.isSuccess){
    SecTitle("Select current items to save")
    LdText("${selected.size} selected · ${rows.size}/6 bundles. Existing item order is kept. Library save is separate from calculator Save.",12f,color=p.muted,maxLines=6)
    if(base.lines.isEmpty())LdText("No current items. You can still preview a saved bundle below.",13f,color=p.muted,maxLines=5)
    base.lines.forEach{r->BundleAction((if(r.id in selected)"✓ "else"□ ")+r.label+" · "+r.quantity+" × "+r.rate,Modifier.testTag("bundle_pick_${r.id}")){if(!busy){selected=if(r.id in selected)selected-r.id else selected+r.id;preview=null;confirm=false}}}
    if(editor==null)BundleAction("Save selected as named bundle",enabled=!busy&&valid.isSuccess&&selected.isNotEmpty()&&rows.size<6,modifier=Modifier.testTag("bundle_capture")){editorRaw=s.estimateBundleLibrary;editor="";name=""}
    editor?.let{id->GlassCard{
     LdField(name,{name=it.take(40)},"Bundle name (1-32 characters)",modifier=Modifier.testTag("bundle_name"))
     BundleAction(if(id.isEmpty())"Save bundle library"else"Rename bundle",enabled=!busy&&(id.isNotEmpty()||valid.isSuccess&&selected.isNotEmpty()),modifier=Modifier.testTag("bundle_save")){if(id.isEmpty())library(editorRaw){EstimateBundles.capture(it,name,base,selected)}else library(editorRaw){EstimateBundles.rename(it,id,name)}}
     BundleAction("Cancel edit",Modifier.testTag("bundle_edit_cancel")){editor=null;name=""}
    }}
    SecTitle("My reusable bundles")
    if(rows.isEmpty())LdText("No saved bundles yet",14f,color=p.muted,modifier=Modifier.testTag("bundle_empty"))
    rows.forEach{r->GlassCard(Modifier.padding(top=8.dp)){
     LdText(r.name,18f,FontWeight.Bold,maxLines=4)
     LdText("${r.lines.size} items · subtotal ${CallEstimate.money(CallEstimate.totals(CallEstimate.Draft(lines=r.lines)).subtotal)}",13f,color=p.muted,maxLines=5)
     BundleAction("Preview append",enabled=!busy&&valid.isSuccess,modifier=Modifier.testTag("bundle_preview_${r.id}")){runCatching{EstimateBundles.plan(base,s.estimateBundleLibrary,r.id)}.fold({preview=it;confirm=false;error=""},{preview=null;error=it.message?:"Preview refused"})}
     BundleAction("Rename",Modifier.testTag("bundle_rename_${r.id}")){editorRaw=s.estimateBundleLibrary;editor=r.id;name=r.name}
     BundleAction("Delete",Modifier.testTag("bundle_delete_${r.id}")){deleteRaw=s.estimateBundleLibrary;deleting=r}
    }}
   }
   preview?.let{v->
    val a=CallEstimate.totals(v.before);val b=CallEstimate.totals(v.after)
    SecTitle("Preview · ${v.after.lines.size-v.before.lines.size} items to append")
    LdText("Keep all ${base.lines.size} existing items. Repeated names remain separate, never merged. Whole bundle must fit 20 items. Same currency and units only; no conversion. Saved rates may be old: check each rate before confirming.",13f,color=p.muted,maxLines=9)
    v.after.lines.drop(v.before.lines.size).forEachIndexed{i,r->GlassCard(Modifier.padding(top=6.dp)){
     LdText(r.label,17f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("bundle_preview_name_$i"))
     LdText("${r.quantity} × ${r.rate} = ${b.lineAmounts[base.lines.size+i].let{CallEstimate.money(it)}}",16f,maxLines=Int.MAX_VALUE)
    }}
    GlassCard(Modifier.padding(top=8.dp)){
     LdText("Whole estimate before → after",17f,FontWeight.Bold,maxLines=5)
     LdText("Subtotal ${CallEstimate.money(a.subtotal)} → ${CallEstimate.money(b.subtotal)}",14f,maxLines=Int.MAX_VALUE)
     LdText("Flat discount ${CallEstimate.money(a.discount)} unchanged · Tax ${v.before.tax}% unchanged",13f,maxLines=Int.MAX_VALUE)
     LdText("Tax amount ${CallEstimate.money(a.tax)} → ${CallEstimate.money(b.tax)}",14f,maxLines=Int.MAX_VALUE)
     LdText("Total ${CallEstimate.money(a.total)} → ${CallEstimate.money(b.total)}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("bundle_preview_total"))
    }
    BundleAction("Append to unsaved calculator",enabled=!busy,modifier=Modifier.testTag("bundle_apply")){confirm=true}
    BundleAction("Close preview",Modifier.testTag("bundle_preview_close")){preview=null;confirm=false}
   }
  }
  BundleAction("Return to calculator",Modifier.testTag("bundle_back")){back()}
  LdText("Confirm only appends to the in-memory calculator; Draft save remains separate. No saved Undo. Delete removes only a library bundle, not existing calculator rows. No stock, invoice, debt, payment, contact, call, message or cloud action.",12f,color=p.muted,maxLines=9)
 }
 if(inSheet)Column{SheetTitle("Calculator item bundles");content()}else PageFrame("Calculator item bundles",{back()},content=content)
 if(confirm&&preview!=null)Dialog(onDismissRequest={confirm=false}){GlassCard{
  LdText("Append previewed bundle items?",18f,FontWeight.Bold,maxLines=5)
  LdText("Only unsaved calculator changes. Existing rows, discount and tax percentage stay. Check stale rates yourself. Calculator Save is separate. No saved Undo.",13f,color=p.muted,maxLines=9)
  BundleAction("Confirm unsaved append",enabled=!busy,modifier=Modifier.testTag("bundle_confirm")){val v=preview!!;if(!busy){busy=true;scope.launch{try{var next:CallEstimate.Draft?=null;ctx.ldStore.edit{prefs->require(prefs[K.estimateBundles]==true){"Item bundles are OFF"};next=EstimateBundles.apply(v,latestBase,prefs[K.estimateBundleLibrary]?:"")};confirm=false;latestApply(requireNotNull(next))}catch(e:Exception){confirm=false;preview=null;error=e.message?:"Preview again"}finally{busy=false}}}}
  BundleAction("Cancel",Modifier.testTag("bundle_cancel")){confirm=false}
 }}
 deleting?.let{r->Dialog(onDismissRequest={if(!busy)deleting=null}){GlassCard{
  LdText("Delete saved bundle?",18f,FontWeight.Bold,maxLines=5);LdText(r.name+" will be removed only from the library. No estimate changes.",13f,color=p.muted,maxLines=7)
  BundleAction("Delete bundle",enabled=!busy,danger=true,modifier=Modifier.testTag("bundle_delete_confirm")){library(deleteRaw){EstimateBundles.remove(it,r.id)}}
  BundleAction("Keep bundle",Modifier.testTag("bundle_delete_cancel")){deleting=null}
 }}}
 if(discard)Dialog(onDismissRequest={discard=false}){GlassCard{
  LdText("Discard bundle name edit?",18f,FontWeight.Bold,maxLines=5)
  BundleAction("Discard edit",modifier=Modifier.testTag("bundle_discard")){discard=false;onBack()}
  BundleAction("Keep editing",Modifier.testTag("bundle_keep")){discard=false}
 }}
}
@Composable fun EstimateBundlesPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();val p=LocalLd.current;var error by remember{mutableStateOf("")}
 PageFrame("Calculator item bundles",onBack){
  LdText("Reusable items. Check every rate.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Calculator item bundles","Default OFF. Save selected rows in a named phone-only bundle, then preview reuse.",s.estimateBundles,last=true,subMaxLines=6){v->scope.launch{try{LdApp.instance.repo.set(K.estimateBundles,v);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Open any number's Call calculator > Reusable item bundles. Save selected item names, quantities and rates under one of six names. Reuse appends the entire bundle only after every item and whole-total preview plus confirmation. Existing items, flat discount and tax percentage are kept. Repeated names remain separate. All items must fit the 20-item calculator limit; otherwise nothing is appended. Save in calculator is separate. No saved Undo.",14f,color=p.muted,maxLines=18)
  LdText("Bundle library is shared across calculators on this phone, unencrypted and not in backup. No caller identity, discount or tax is captured. Item names may contain private text you entered: choose them carefully. Rates are manual snapshots, never live prices. Check currency, units and rates each reuse. Turning OFF keeps library and estimates. No stock, invoice, payment, contact, call, message or cloud action.",13f,color=p.muted,maxLines=17)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5)
 }
}

@Composable private fun BundleAction(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,danger:Boolean=false,onClick:()->Unit){
 val p=LocalLd.current
 Box(modifier.fillMaxWidth().alpha(if(enabled)1f else .5f).buttonGlass(p,RoundedCornerShape(16.dp)).clickable(enabled=enabled,onClick=onClick).padding(horizontal=12.dp,vertical=14.dp),contentAlignment=androidx.compose.ui.Alignment.Center){LdText(text,14.5f,FontWeight.SemiBold,color=if(danger)p.danger else p.text,maxLines=Int.MAX_VALUE)}
}
