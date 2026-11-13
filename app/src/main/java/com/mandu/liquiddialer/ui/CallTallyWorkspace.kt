package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun CallTallyPage(onBack:()->Unit){
 val p=LocalLd.current;val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();var open by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 if(open){CallTallyWorkspace({open=false});return}
 PageFrame("Call quantity tally",onBack){
  LdText("Count items while you talk.",23f,FontWeight.Bold,maxLines=4)
  SwitchRow("Show quantity tally in call Notes","Default OFF. Manual counters, no audio recognition or stock connection.",s.callTally,last=true){scope.launch{try{LdApp.instance.repo.set(K.callTally,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  SwitchRow("Reusable tally templates","Default OFF. Save labels and step only, never quantities or phone numbers.",s.tallyTemplates,last=true){scope.launch{try{LdApp.instance.repo.set(K.tallyTemplates,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  PrimaryButton("Open quantity tally",modifier=Modifier.testTag("tally_settings_open")){open=true}
  LdText("Up to 12 named whole-number counters. Add, subtract, set a quantity, rename and remove. Choose a shared step from 1 to 1000000. Each count stays between 0 and 1000000000. Total is only meaningful when all counters use the same unit.",14f,color=p.muted,maxLines=12)
  LdText("Undo/Redo keeps the last 40 edits in this open board. Counts are session only: Back/close discards the board after confirmation. Optional templates save labels and step, not counts. Nothing is sent to notes, orders, stock, clipboard or cloud. App/process close can lose this scratch work. No price or payment calculation.",13f,color=p.muted,maxLines=12)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}
@Composable fun CallTallyWorkspace(onBack:()->Unit,inSheet:Boolean=false){
 val settings by LdApp.settings.collectAsState();var library by remember{mutableStateOf(false)}
 val p=LocalLd.current;val focus=androidx.compose.ui.platform.LocalFocusManager.current
 val board=remember{CallTally.Board()};var rev by remember{mutableIntStateOf(0)};val rows=remember(rev){board.rows}
 var selected by remember{mutableStateOf("")};var name by remember{mutableStateOf("")};var value by remember{mutableStateOf("0")};var step by remember{mutableStateOf("1")}
 var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
 val chosen=rows.firstOrNull{it.id==selected};val n=runCatching{CallTally.step(step)}.getOrNull()
 fun clearForm(){selected="";name="";value="0";error=""}
 fun runEdit(action:()->List<CallTally.Item>){focus.clearFocus();runCatching{action()}.fold({if(board.change(it)){rev++;status="Session changed · Undo available"};error=""},{error=it.message?:"Input check karo"})}
 fun history(redo:Boolean){focus.clearFocus();if(redo)board.redo()else board.undo();rev++;clearForm();status=if(redo)"Edit redone"else"Edit undone"}
 fun back(){focus.clearFocus();if(rows.isNotEmpty()||name.isNotBlank()||board.canUndo||board.canRedo)confirm="back"else onBack()}
 if(library&&settings.tallyTemplates){TallyTemplateLibrary(rows,step,{library=false},{t->runEdit{TallyTemplates.board(t)};step=t.step.toString();clearForm();status="Template loaded at zero · Undo restores previous counters (step stays selected)";library=false},inSheet);return}
 BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  if(settings.tallyTemplates)PrimaryButton("Tally templates",modifier=Modifier.testTag("tally_templates_open")){focus.clearFocus();library=true}
  LdText("Manual session counters",17f,FontWeight.Bold,maxLines=3)
  LdText("Not stock or an order. Back/close clears this board. Whole quantities only; label each counter with its unit.",12f,color=p.muted,maxLines=6)
  GlassCard(Modifier.fillMaxWidth().padding(vertical=6.dp)){
   LdText(CallTally.total(rows).toString(),30f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("tally_total"))
   LdText("Raw sum · ${rows.size} /12 counters",14f,FontWeight.Bold,modifier=Modifier.testTag("tally_count"))
   LdText("Compare/add only the same units. Mixed units make this sum meaningless.",12f,color=p.muted,maxLines=4)
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   PrimaryButton("Undo (${board.undoCount})",enabled=board.canUndo,modifier=Modifier.weight(1f).testTag("tally_undo")){history(false)}
   PrimaryButton("Redo (${board.redoCount})",enabled=board.canRedo,modifier=Modifier.weight(1f).testTag("tally_redo")){history(true)}
  }
  LdText(if(chosen==null)"New counter"else"Edit: ${chosen.label}",15f,FontWeight.Bold,maxLines=5,modifier=Modifier.testTag("tally_editor"))
  LdField(name,{name=it.take(100);error=""},"Item + unit, e.g. Rice bags",modifier=Modifier.testTag("tally_name"))
  LdField(value,{value=it.take(20);error=""},"Whole quantity",modifier=Modifier.testTag("tally_value"))
  PrimaryButton(if(chosen==null)"Add counter"else"Apply name & quantity",enabled=name.isNotBlank()&&value.isNotEmpty(),modifier=Modifier.testTag("tally_apply")){
   val before=rev;runEdit{if(chosen==null)CallTally.add(rows,name,value)else CallTally.edit(rows,chosen.id,name,value)};if(error.isEmpty()){clearForm();if(rev==before)status="No change"}
  }
  if(selected.isNotEmpty())GhostButton("Cancel edit",Modifier.testTag("tally_cancel_edit")){focus.clearFocus();clearForm()}
  LdText("Shared +/- step (1-1000000)",13f,FontWeight.Bold)
  LdField(step,{step=it.take(20);error=""},"Step",modifier=Modifier.testTag("tally_step"))
  if(n==null)LdText("Step must be a whole number from 1 to 1000000",12f,color=p.danger,maxLines=4,modifier=Modifier.testTag("tally_step_error"))
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("tally_error"))
  if(status.isNotEmpty())LdText(status,12f,color=p.muted,maxLines=4,modifier=Modifier.testTag("tally_status"))
  if(rows.isEmpty())LdText("No counters yet. Add a named quantity above.",14f,color=p.muted,maxLines=4,modifier=Modifier.testTag("tally_empty"))
  rows.forEachIndexed{i,item->GlassCard(Modifier.fillMaxWidth().padding(top=8.dp).testTag("tally_item_$i")){
   LdText(item.label,18f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("tally_label_$i"))
   LdText(item.value.toString(),30f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("tally_quantity_$i"))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("- ${n?:"?"}",enabled=n!=null&&item.value>=(n?:Long.MAX_VALUE),modifier=Modifier.weight(1f).testTag("tally_minus_$i")){runEdit{CallTally.adjust(rows,item.id,-n!!)}}
    PrimaryButton("+ ${n?:"?"}",enabled=n!=null&&item.value<=CallTally.MAX_VALUE-(n?:Long.MAX_VALUE),modifier=Modifier.weight(1f).testTag("tally_plus_$i")){runEdit{CallTally.adjust(rows,item.id,n!!)}}
   }
   GhostButton("Edit name / set quantity",Modifier.testTag("tally_edit_$i")){focus.clearFocus();selected=item.id;name=item.label;value=item.value.toString();error="";status="Editing ${item.label} above"}
   GhostButton("Remove counter",Modifier.testTag("tally_remove_$i")){focus.clearFocus();confirm="remove:${item.id}"}
  }}
  PrimaryButton("Zero all quantities",enabled=rows.any{it.value!=0L},modifier=Modifier.testTag("tally_zero")){focus.clearFocus();confirm="zero"}
  LdText("Counts are session only. Undo/Redo: last 40 counter edits, not step changes. Optional templates save only labels and step. No copy, share or call action.",12f,color=p.muted,maxLines=5)
 }
 if(inSheet)Column{SheetTitle("Quantity tally");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("tally_back")){back()};content()}else PageFrame("Quantity tally",{back()},content=content)
 if(confirm.isNotEmpty())Dialog(onDismissRequest={confirm=""}){GlassCard{
  LdText(when{confirm=="back"->"Leave and discard this session board?";confirm=="zero"->"Set every quantity to zero? Names stay. Undo available.";else->"Remove ${rows.firstOrNull{it.id==confirm.removePrefix("remove:") }?.label.orEmpty()}? Undo available."},17f,FontWeight.Bold,maxLines=8)
  PrimaryButton(if(confirm=="back")"Discard & leave"else"Confirm change",danger=true,modifier=Modifier.testTag("tally_confirm")){val action=confirm;confirm="";when{action=="back"->onBack();action=="zero"->{runEdit{CallTally.zero(rows)};clearForm()};else->{runEdit{CallTally.remove(rows,action.removePrefix("remove:"))};clearForm()}}}
  GhostButton("Keep board",Modifier.testTag("tally_keep")){confirm=""}
 }}
}
