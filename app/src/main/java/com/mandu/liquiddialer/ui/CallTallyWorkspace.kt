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
  SwitchRow("Tally batch quantities","Default OFF. Select counters, preview and confirm one atomic edit.",s.tallyBatch,last=true){scope.launch{try{LdApp.instance.repo.set(K.tallyBatch,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  SwitchRow("Tally quantity transfer","Default OFF. Move one source into several destinations, with preview and one confirmed edit.",s.tallyTransfer,last=true){scope.launch{try{LdApp.instance.repo.set(K.tallyTransfer,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  SwitchRow("Saved tally boards","Default OFF. Explicit snapshots include quantities, unlike templates; manage in Saved tally boards Settings.",s.tallyBoards,last=true,subMaxLines=5){scope.launch{try{LdApp.instance.repo.set(K.tallyBoards,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  Column(Modifier.testTag("tally_settings_options")){
  SwitchRow("Find session counters","Default OFF. Temporary name search and All / Nonzero / Zero view.",s.tallyFind,last=true){scope.launch{try{LdApp.instance.repo.set(K.tallyFind,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  SwitchRow("Fold counter actions layout","Default OFF. Edit/Remove behind Actions; quantity and +/- stay visible.",s.tallyFold,last=true){scope.launch{try{LdApp.instance.repo.set(K.tallyFold,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  }
  LdText("Finder searches only current counter labels, not saved boards, templates, notes or calls. Query/filter change visibility only. Board raw sum and full count stay unchanged; shown count is separate. Hidden rows remain in Zero all, Undo/Redo, batch, transfer and saved-board/template actions. Clear view shows all again. Finder OFF resets query/filter. Fold layout OFF restores original Edit/Remove buttons; layout changes fold actions without changing board or typed edit. Back/close resets this temporary view.",13f,color=p.muted,maxLines=22)
  PrimaryButton("Open quantity tally",modifier=Modifier.testTag("tally_settings_open")){open=true}
  LdText("Up to 12 named whole-number counters. Add, subtract, set a quantity, rename and remove. Choose a shared step from 1 to 1000000. Each count stays between 0 and 1000000000. Total is only meaningful when all counters use the same unit.",14f,color=p.muted,maxLines=12)
  LdText("Undo/Redo keeps the last 40 edits in this open board. Working counts are session only: Back/close discards the board after confirmation. Optional templates save labels and step, not counts. Optional saved boards keep quantities only after an explicit library Save. Nothing is sent to notes, orders, stock, clipboard or cloud. App/process close can lose this scratch work. No price or payment calculation.",13f,color=p.muted,maxLines=12)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}
@Composable fun CallTallyWorkspace(onBack:()->Unit,inSheet:Boolean=false){
 val settings by LdApp.settings.collectAsState();var savedBoards by remember{mutableStateOf(false)};var library by remember{mutableStateOf(false)};var batch by remember{mutableStateOf(false)};var transfer by remember{mutableStateOf(false)}
 val p=LocalLd.current;val focus=androidx.compose.ui.platform.LocalFocusManager.current
 val board=remember{CallTally.Board()};var rev by remember{mutableIntStateOf(0)};val rows=remember(rev){board.rows}
 var selected by remember{mutableStateOf("")};var name by remember{mutableStateOf("")};var value by remember{mutableStateOf("0")};var step by remember{mutableStateOf("1")}
 var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
 var query by remember{mutableStateOf("")};var filter by remember{mutableStateOf("All")};var expanded by remember{mutableStateOf(emptySet<String>())}
 LaunchedEffect(settings.tallyFind){if(!settings.tallyFind){query="";filter="All"}}
 LaunchedEffect(settings.tallyFold){expanded=emptySet()}
 val visible=if(settings.tallyFind)TallyFind.rows(rows,query,filter)else rows
 val chosen=rows.firstOrNull{it.id==selected};val n=runCatching{CallTally.step(step)}.getOrNull()
 fun clearForm(){selected="";name="";value="0";error=""}
 fun runEdit(action:()->List<CallTally.Item>){focus.clearFocus();runCatching{action()}.fold({if(board.change(it)){rev++;status="Session changed · Undo available"};error=""},{error=it.message?:"Input check karo"})}
 fun history(redo:Boolean){focus.clearFocus();if(redo)board.redo()else board.undo();rev++;clearForm();status=if(redo)"Edit redone"else"Edit undone"}
 fun back(){focus.clearFocus();if(rows.isNotEmpty()||name.isNotBlank()||board.canUndo||board.canRedo)confirm="back"else onBack()}
 if(savedBoards&&settings.tallyBoards){TallyBoardsWorkspace(board.rows,{savedBoards=false},{view->runEdit{TallyBoards.apply(view,board.rows,LdApp.settings.value.tallyBoardLibrary)};if(error.isEmpty()){clearForm();status="Saved board loaded · one Undo restores previous counters; step unchanged";savedBoards=false}},inSheet);return}
 if(library&&settings.tallyTemplates){TallyTemplateLibrary(rows,step,{library=false},{t->runEdit{TallyTemplates.board(t)};step=t.step.toString();clearForm();status="Template loaded at zero · Undo restores previous counters (step stays selected)";library=false},inSheet);return}
 if(batch&&settings.tallyBatch){TallyBatchWorkspace(rows,{batch=false},{preview->runEdit{TallyBatch.apply(board.rows,preview)};if(error.isEmpty()){clearForm();status="Batch applied · one Undo restores counters; step unchanged";batch=false}},inSheet);return}
 if(transfer&&settings.tallyTransfer){TallyTransferWorkspace(rows,{transfer=false},{preview->runEdit{TallyTransfer.apply(board.rows,preview)};if(error.isEmpty()){clearForm();status="Transfer applied · one Undo restores counters; step unchanged";transfer=false}},inSheet);return}
 BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  if(settings.tallyBoards)PrimaryButton("Saved tally boards",modifier=Modifier.testTag("tally_boards_open")){focus.clearFocus();savedBoards=true}
  if(settings.tallyTemplates)PrimaryButton("Tally templates",modifier=Modifier.testTag("tally_templates_open")){focus.clearFocus();library=true}
  if(settings.tallyBatch)PrimaryButton("Batch quantities",modifier=Modifier.testTag("tally_batch_open")){focus.clearFocus();batch=true}
  if(settings.tallyTransfer)PrimaryButton("Transfer quantities",modifier=Modifier.testTag("tally_transfer_open")){focus.clearFocus();transfer=true}
  LdText("Manual session counters",17f,FontWeight.Bold,maxLines=3)
  LdText("Not stock or an order. Back/close clears this session. Optional saved boards require explicit Save. Whole quantities only; label each counter with its unit.",12f,color=p.muted,maxLines=6)
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
  if(settings.tallyFind){
   LdText("Find counters in this board",16f,FontWeight.Bold,maxLines=4)
   LdField(query,{query=it.take(100)},"Search counter names",modifier=Modifier.testTag("tally_find_query"))
   TallyFind.modes.forEach{m->Spacer(Modifier.height(8.dp));GhostButton(if(filter==m)"$m ✓"else m,Modifier.testTag("tally_find_${m.lowercase()}")){focus.clearFocus();filter=m}}
   Spacer(Modifier.height(8.dp))
   GhostButton("Show all counters",Modifier.testTag("tally_find_clear")){focus.clearFocus();query="";filter="All"}
   LdText("${visible.size} shown / ${rows.size} counters · $filter",13f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("tally_find_count"))
   LdText("View only. Raw sum, Zero all, Undo/Redo and library/batch/transfer actions use the WHOLE board, including hidden counters.",12f,color=p.muted,maxLines=8)
   if(visible.isEmpty()&&rows.isNotEmpty())LdText("No matching counter. Clear view to show all; quantities are unchanged.",13f,color=p.muted,maxLines=6,modifier=Modifier.testTag("tally_find_empty"))
  }
  rows.forEachIndexed{i,item->if(item in visible)GlassCard(Modifier.fillMaxWidth().padding(top=8.dp).testTag("tally_item_$i")){
   LdText(item.label,18f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("tally_label_$i"))
   LdText(item.value.toString(),30f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("tally_quantity_$i"))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("- ${n?:"?"}",enabled=n!=null&&item.value>=(n?:Long.MAX_VALUE),modifier=Modifier.weight(1f).testTag("tally_minus_$i")){runEdit{CallTally.adjust(rows,item.id,-n!!)}}
    PrimaryButton("+ ${n?:"?"}",enabled=n!=null&&item.value<=CallTally.MAX_VALUE-(n?:Long.MAX_VALUE),modifier=Modifier.weight(1f).testTag("tally_plus_$i")){runEdit{CallTally.adjust(rows,item.id,n!!)}}
   }
   if(settings.tallyFold){Spacer(Modifier.height(8.dp));GhostButton(if(item.id in expanded)"Hide actions"else"Actions",Modifier.testTag("tally_actions_$i")){expanded=if(item.id in expanded)expanded-item.id else expanded+item.id}}
   if(!settings.tallyFold||item.id in expanded){
   if(settings.tallyFold)Spacer(Modifier.height(8.dp))
   GhostButton("Edit name / set quantity",Modifier.testTag("tally_edit_$i")){focus.clearFocus();selected=item.id;name=item.label;value=item.value.toString();error="";status="Editing ${item.label} above"}
   if(settings.tallyFold)Spacer(Modifier.height(8.dp))
   GhostButton("Remove counter",Modifier.testTag("tally_remove_$i")){focus.clearFocus();confirm="remove:${item.id}"}
   }
  }}
  PrimaryButton(if(settings.tallyFind)"Zero whole board"else"Zero all quantities",enabled=rows.any{it.value!=0L},modifier=Modifier.testTag("tally_zero")){focus.clearFocus();confirm="zero"}
  LdText("Counts are session only. Undo/Redo: last 40 counter edits, not step changes. Optional templates save only labels and step. Saved boards, if enabled, save quantities only when you choose Save. No copy, share or call action.",12f,color=p.muted,maxLines=5)
 }
 if(inSheet)Column{SheetTitle("Quantity tally");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("tally_back")){back()};content()}else PageFrame("Quantity tally",{back()},content=content)
 if(confirm.isNotEmpty())Dialog(onDismissRequest={confirm=""}){GlassCard{
  LdText(when{confirm=="back"->"Leave and discard this session board?";confirm=="zero"->"Set every quantity to zero? Names stay. Undo available.";else->"Remove ${rows.firstOrNull{it.id==confirm.removePrefix("remove:") }?.label.orEmpty()}? Undo available."},17f,FontWeight.Bold,maxLines=8)
  PrimaryButton(if(confirm=="back")"Discard & leave"else"Confirm change",danger=true,modifier=Modifier.testTag("tally_confirm")){val action=confirm;confirm="";when{action=="back"->onBack();action=="zero"->{runEdit{CallTally.zero(rows)};clearForm()};else->{runEdit{CallTally.remove(rows,action.removePrefix("remove:"))};clearForm()}}}
  GhostButton("Keep board",Modifier.testTag("tally_keep")){confirm=""}
 }}
}
