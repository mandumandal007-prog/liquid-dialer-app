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

@Composable fun TallyBatchWorkspace(rows:List<CallTally.Item>,onBack:()->Unit,onApply:(TallyBatch.Preview)->Unit,inSheet:Boolean=false){
 val p=LocalLd.current;val focus=LocalFocusManager.current
 var selected by remember(rows){mutableStateOf<Set<String>>(emptySet())};var operation by remember{mutableStateOf(TallyBatch.Operation.ADD)};var operand by remember{mutableStateOf("1")}
 var preview by remember(rows){mutableStateOf<TallyBatch.Preview?>(null)};var confirm by remember(rows){mutableStateOf(false)};var error by remember{mutableStateOf("")}
 fun invalidate(){preview=null;confirm=false;error=""}
 BackHandler{focus.clearFocus();onBack()}
 val content:@Composable ColumnScope.()->Unit={
  LdText("One batch. Preview before applying.",20f,FontWeight.Bold,maxLines=4)
  LdText("Session counters only, not stock or orders. Select quantities with matching units yourself; this tool cannot detect units. No price or payment calculation.",13f,color=p.muted,maxLines=8)
  LdText("${selected.size} / ${rows.size} selected",15f,FontWeight.Bold,modifier=Modifier.testTag("batch_count"))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   PrimaryButton("Select all",enabled=rows.isNotEmpty(),modifier=Modifier.weight(1f).testTag("batch_all")){selected=rows.map{it.id}.toSet();invalidate()}
   PrimaryButton("Deselect",enabled=selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("batch_none")){selected=emptySet();invalidate()}
  }
  if(rows.isEmpty())LdText("No counters. Return and add named counters first.",14f,color=p.muted,maxLines=4,modifier=Modifier.testTag("batch_empty"))
  rows.forEachIndexed{i,item->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
   LdText(item.label,17f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
   LdText(item.value.toString(),23f,FontWeight.Bold,modifier=Modifier.testTag("batch_value_$i"))
   GhostButton(if(item.id in selected)"Selected"else"Select",Modifier.fillMaxWidth().testTag("batch_pick_$i")){selected=if(item.id in selected)selected-item.id else selected+item.id;invalidate()}
  }}
  LdText("Operation on each selected counter",15f,FontWeight.Bold,maxLines=3)
  TallyBatch.Operation.entries.chunked(2).forEach{pair->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){pair.forEach{op->PrimaryButton((if(operation==op)"✓ "else"")+op.title,modifier=Modifier.weight(1f).testTag("batch_op_${op.name}")){operation=op;invalidate()}}}}
  LdField(operand,{operand=it.take(20);invalidate()},if(operation==TallyBatch.Operation.MULTIPLY)"Whole multiplier (0-1000000)"else"Whole quantity (0-1000000000)",modifier=Modifier.testTag("batch_operand"))
  LdText("Add/subtract this amount to/from EACH selected counter. Set replaces each quantity; multiply scales each quantity, not the selection total. Whole numbers only. Any out-of-range result refuses the entire batch.",12f,color=p.muted,maxLines=10)
  PrimaryButton("Preview selected changes",enabled=selected.isNotEmpty()&&operand.isNotEmpty(),modifier=Modifier.testTag("batch_preview")){focus.clearFocus();runCatching{TallyBatch.preview(rows,selected,operation,operand)}.fold({preview=it;error=""},{preview=null;error=it.message?:"Check input"})}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("batch_error"))
  preview?.let{view->
   LdText("Preview only · ${view.changed} quantities change",17f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("batch_preview_state"))
   LdText("${view.operation.title} ${view.operand} per selected counter. Board unchanged until confirmed Apply. Applying also clears the unfinished counter editor, but keeps the shared +/- step.",13f,color=p.muted,maxLines=8)
   view.before.zip(view.after).forEachIndexed{i,(a,b)->if(a.id in view.selected)GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText(a.label,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
    LdText("${a.value} → ${b.value}",22f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("batch_change_$i"))
   }}
   PrimaryButton("Apply this batch",enabled=view.changed>0,modifier=Modifier.testTag("batch_apply")){focus.clearFocus();confirm=true}
   if(view.changed==0)LdText("No quantities would change. Nothing to apply.",13f,color=p.muted,maxLines=4,modifier=Modifier.testTag("batch_no_change"))
  }
  GhostButton("Return without changes",Modifier.testTag("batch_back")){focus.clearFocus();onBack()}
  LdText("One Apply is one board Undo/Redo edit (last 40). This screen never saves counts, changes templates, sends messages or places calls. Closing or app/process loss can discard this session.",12f,color=p.muted,maxLines=8)
 }
 if(inSheet)Column{SheetTitle("Batch quantities");content()}else PageFrame("Batch quantities",{focus.clearFocus();onBack()},content=content)
 if(confirm)Dialog(onDismissRequest={confirm=false}){GlassCard{
  LdText("Apply ${preview?.operation?.title.orEmpty()} ${preview?.operand} to ${preview?.selected?.size} counters?",18f,FontWeight.Bold,maxLines=6)
  LdText("Replaces selected quantities and clears the unfinished editor. Shared step stays. Session only; one Undo restores counters.",13f,color=p.muted,maxLines=7)
  PrimaryButton("Confirm batch",modifier=Modifier.testTag("batch_confirm")){val view=preview;confirm=false;if(view!=null)runCatching{TallyBatch.apply(rows,view);onApply(view)}.onFailure{error=it.message?:"Batch not applied";preview=null}}
  GhostButton("Keep preview",Modifier.testTag("batch_cancel")){confirm=false}
 }}
}
