package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun CheckpointTasksPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Checkpoint to checklist",onBack){
  LdText("Baat se agla kaam",22f,FontWeight.Bold,maxLines=4)
  LdText("Live call desk ke marked moments mein se apne kaam ke labels chuno. Exact number aur nayi topics ki preview padho, phir Add confirm karo. Har added topic unfinished hota hai.",14f,color=p.muted,maxLines=10)
  GlassCard{SwitchRow("Checkpoint to checklist","Default OFF. No automatic tasks.",s.checkpointTasks,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.checkpointTasks]=v};error=false}catch(_:Exception){error=true}}}}
  SecTitle("Choose. Preview. Add.")
  GlassCard{
   LdText("Live call desk + Call checkpoints ON aur Checklist visible chahiye. Mark labels banao; saved-note board bhi use kar sakte ho. Choose topics mein koi row pehle se selected nahi hoti. Elapsed time sirf source dikhata hai, checklist text mein sirf label jata hai.",13f,maxLines=12)
   Spacer(Modifier.height(12.dp));LdText("Existing topics ke matching labels aur selected duplicate labels skip hote hain (case/whitespace ignored). Existing text, order aur done ticks same. 30-topic limit: jagah kam ho to poora batch refuse, partial add nahi.",13f,maxLines=10)
  }
  SecTitle("Your action, local only")
  LdText("Board/list/call/setting change par purani preview save nahi hogi. Cancel ya OFF unsaved selection hataata hai; already added topics stay. Checklist tool mein topics dekho. Summary note save alag hai: yeh note, reminder, deadline ya message nahi banata.",13f,color=p.muted,maxLines=12)
  GlassCard{LdText("No audio, recording, transcription, network, clipboard or new permission. Checklist existing unencrypted local storage mein hai; backup rules same. Call change/desk close/recreation par selection kho sakti hai. Yeh switch doosre features enable nahi karta.",13f,maxLines=12)}
  if(error)LdText("Setting save nahi hui",13f,color=p.danger)
 }
}

@Composable fun CheckpointTasksWorkspace(session:LiveCallDesk.Session,board:CallCheckpoints.Board){
 val ctx=LocalContext.current;val p=LocalLd.current
 var open by remember(session,board){mutableStateOf(false)}
 var selected by remember(session,board){mutableStateOf(emptySet<Int>())}
 var preview by remember(session,board){mutableStateOf<CheckpointTasks.Preview?>(null)}
 var commit by remember(session,board){mutableStateOf(CheckpointTasks.Commit())}
 var status by remember(session,board){mutableStateOf("")}
 var completed by remember(session,board){mutableStateOf(false)}
 GhostButton(if(open)"Hide topic handoff"else"Choose checklist topics",Modifier.testTag("checkpoint_tasks_open")){open=!open;if(!open){selected=emptySet();preview=null;status=""}}
 if(open)GlassCard(Modifier.testTag("checkpoint_tasks_panel")){
  LdText("CHECKPOINT TO CHECKLIST",12f,FontWeight.Bold,p.accent)
  LdText(session.number,14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("checkpoint_tasks_number"))
  if(!completed&&preview==null){
   LdText("Choose labels to add as unfinished topics. Nothing preselected. Times are source context only.",12f,color=p.muted,maxLines=6)
   board.marks.forEachIndexed{i,m->
    Column(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value=i in selected,role=Role.Checkbox,onValueChange={checked->selected=if(checked)selected+i else selected-i;status=""}).padding(vertical=10.dp).testTag("checkpoint_task_$i")){
     LdText("${if(i in selected)"[x] Selected"else"[ ] Choose"} · ${CallCheckpoints.time(m.seconds)}",12f,FontWeight.SemiBold,p.accent,maxLines=4)
     LdText(m.label,14f,maxLines=Int.MAX_VALUE)
    }
   }
   PrimaryButton("Preview topics (${selected.size})",enabled=selected.isNotEmpty(),modifier=Modifier.testTag("checkpoint_tasks_preview")){
    try{preview=CheckpointTasks.preview(ctx,session,board,selected);commit=CheckpointTasks.Commit();status=""}catch(e:Exception){status=e.message?:"Preview failed. Nothing added."}
   }
  }
  preview?.let{view->
   LdText("${view.topics.size} new · ${view.skipped} duplicate skipped",15f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("checkpoint_tasks_count"))
   LdText("Existing ${view.existing.size} topics stay unchanged. Add only the labels below, without timestamps.",12f,color=p.muted,maxLines=6)
   view.topics.forEachIndexed{i,text->LdText("${i+1}. $text",14f,maxLines=Int.MAX_VALUE,modifier=Modifier.padding(vertical=6.dp).testTag("checkpoint_task_preview_$i"))}
   if(view.topics.isEmpty())LdText("All selected labels already exist. Nothing to add.",13f,maxLines=5)
   PrimaryButton("Add ${view.topics.size} unfinished topics",enabled=view.topics.isNotEmpty(),modifier=Modifier.testTag("checkpoint_tasks_add")){
    try{commit.save(ctx,session,view,board);preview=null;completed=true;status="Added ${view.topics.size} unfinished topics. Open Checklist to see them."}catch(_:Exception){preview=null;status="Not added. Call, board, setting or checklist changed / storage failed. Preview again."}
   }
   GhostButton("Change selection",Modifier.testTag("checkpoint_tasks_change")){preview=null;status=""}
  }
  if(status.isNotEmpty())LdText(status,13f,color=if(completed)p.accent else p.danger,maxLines=8,modifier=Modifier.testTag("checkpoint_tasks_status"))
  GhostButton("Done / cancel handoff",Modifier.testTag("checkpoint_tasks_cancel")){open=false;selected=emptySet();preview=null;status=""}
 }
}
