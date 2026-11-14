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

@Composable fun TallyTransferWorkspace(rows:List<CallTally.Item>,onBack:()->Unit,onApply:(TallyTransfer.Preview)->Unit,inSheet:Boolean=false){
 val p=LocalLd.current;val focus=LocalFocusManager.current
 var source by remember(rows){mutableStateOf("")};var allocations by remember(rows){mutableStateOf<Map<String,String>>(emptyMap())}
 var preview by remember(rows){mutableStateOf<TallyTransfer.Preview?>(null)};var confirm by remember(rows){mutableStateOf(false)};var error by remember(rows){mutableStateOf("")}
 fun invalidate(){preview=null;confirm=false;error=""}
 fun back(){focus.clearFocus();onBack()}
 BackHandler{back()}
 val from=rows.firstOrNull{it.id==source}
 val content:@Composable ColumnScope.()->Unit={
  LdText("Move quantities. Keep the same total.",21f,FontWeight.Bold,maxLines=4)
  LdText("Session counters only, not stock, orders or payments. Use matching units yourself: labels do not prove that units match. No conversion or automatic unit detection. Nothing changes until confirmed Apply.",13f,color=p.muted,maxLines=9)
  if(rows.size<2)LdText("Add at least two named counters on the board first.",15f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("transfer_empty"))
  LdText("1. Choose the source",17f,FontWeight.Bold,modifier=Modifier.testTag("transfer_source_title"))
  LdText("Changing the source clears all destination inputs and the preview.",12f,color=p.muted,maxLines=4)
  rows.forEachIndexed{i,row->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
   LdText(row.label,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
   LdText("${row.value}",22f,FontWeight.Bold)
   PrimaryButton(if(source==row.id)"Source selected"else"Use as source",enabled=rows.size>=2&&source!=row.id,modifier=Modifier.testTag("transfer_source_$i")){focus.clearFocus();source=row.id;allocations=emptyMap();invalidate()}
  }}
  if(from!=null){
   LdText("2. Allocate from ${from.label}",17f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("transfer_selected"))
   LdText("Available: ${from.value}. Blank means no transfer to that counter. Enter a positive whole quantity for each destination you want. The source loses the SUM of allocations; each destination gains only its entered amount.",13f,color=p.muted,maxLines=10)
   rows.forEachIndexed{i,row->if(row.id!=source)GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText(row.label,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
    LdText("Current: ${row.value}",14f,color=p.muted,maxLines=3)
    LdField(allocations[row.id].orEmpty(),{allocations=allocations+(row.id to it);invalidate()},"Quantity to receive",modifier=Modifier.testTag("transfer_input_$i"))
   }}
   PrimaryButton("Preview quantity transfer",enabled=allocations.values.any{it.isNotEmpty()},modifier=Modifier.testTag("transfer_preview")){focus.clearFocus();runCatching{TallyTransfer.preview(rows,source,allocations)}.fold({preview=it;error=""},{preview=null;error=it.message?:"Check allocations"})}
  }
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("transfer_error"))
  preview?.let{view->
   LdText("Preview only · ${view.moved} moved",18f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("transfer_preview_state"))
   LdText("${view.allocations.size} destinations · raw total ${CallTally.total(view.before)} → ${CallTally.total(view.after)}",14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("transfer_total"))
   LdText("Applying clears the unfinished counter editor; the shared +/- step stays. Same raw total does not validate mixed units.",13f,color=p.muted,maxLines=6)
   view.before.zip(view.after).forEachIndexed{i,(a,b)->if(a!=b)GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText((if(a.id==view.source)"From: "else"To: ")+a.label,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
    LdText("${a.value} → ${b.value}",22f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("transfer_change_$i"))
   }}
   PrimaryButton("Apply this transfer",modifier=Modifier.testTag("transfer_apply")){focus.clearFocus();confirm=true}
  }
  GhostButton("Return without transfer",Modifier.testTag("transfer_back")){back()}
  LdText("One transfer is one board Undo/Redo edit (last 40). No saved counts, template edits, clipboard, call, message or cloud action. This transfer draft is discarded on return; the board stays. App/process close can lose the whole session.",12f,color=p.muted,maxLines=10)
 }
 if(inSheet)Column{SheetTitle("Transfer quantities");content()}else PageFrame("Transfer quantities",{back()},content=content)
 if(confirm)Dialog(onDismissRequest={confirm=false}){GlassCard{
  LdText("Move ${preview?.moved} from the source to ${preview?.allocations?.size} destinations?",18f,FontWeight.Bold,maxLines=6)
  LdText("Applies the preview together and clears the unfinished editor. Shared step stays. Session only; one Undo restores counters. Units are your responsibility.",13f,color=p.muted,maxLines=8)
  PrimaryButton("Confirm transfer",modifier=Modifier.testTag("transfer_confirm")){val view=preview;confirm=false;if(view!=null)runCatching{TallyTransfer.apply(rows,view);onApply(view)}.onFailure{preview=null;error=it.message?:"Transfer not applied"}}
  GhostButton("Keep preview",Modifier.testTag("transfer_cancel")){confirm=false}
 }}
}
