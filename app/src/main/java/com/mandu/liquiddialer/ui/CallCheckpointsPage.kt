package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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

@Composable fun CallCheckpointsPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Call checkpoints",onBack){
  LdText("Baat ka moment yaad rakho",22f,FontWeight.Bold,maxLines=4)
  LdText("Live call desk mein apna short label likho, Mark now dabao. Call connection se guzra waqt us mark par lagta hai. Baad mein poori list preview karke ek note save kar sakte ho.",14f,color=p.muted,maxLines=10)
  GlassCard{SwitchRow("Call checkpoints","Default OFF. Live call desk bhi ON hona chahiye.",s.callCheckpoints,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.callCheckpoints]=v};error=false}catch(_:Exception){error=true}}}}
  if(!s.liveCallDesk)LdText("Settings > Live call desk bhi enable karo.",13f,color=p.accent,maxLines=4)
  SecTitle("Manual, recording nahi")
  GlassCard{
   LdText("Up to 12 marks; label 80 characters tak, blank = Checkpoint. Har Mark now ek naya mark. Koi automatic topic detection, audio, transcription ya caller ki baat capture nahi hoti.",13f,maxLines=9)
   Spacer(Modifier.height(12.dp));LdText("Time connection se elapsed hai, active/billed minutes ya audio seek position nahi. Clock badle to time galat ho sakta hai; backwards time par mark refuse hota hai. 7 din se lambi elapsed duration par unavailable.",13f,maxLines=9)
  }
  SecTitle("Preview, phir Save")
  LdText("Mark aur Remove sirf session list badalte hain. Preview mein exact number, saare marks aur note text padho. Confirm save ek naya ordinary saved note banata hai; existing notes nahi badalte. Ek board ek baar save. Save ke baad marks locked; nayi list ke liye explicit New board chahiye.",13f,color=p.muted,maxLines=12)
  GlassCard{LdText("Unsaved marks aur label desk close/recreate, call change, Hold, doosri call, video ya OFF par kho sakte hain. OFF saved note nahi mitata. Notes phone par unencrypted hain; ordinary notes ke backup/storage rules lagu. No message, call, clipboard, cloud ya permission action.",13f,maxLines=12)}
  if(error)LdText("Setting save nahi hui",13f,color=p.danger)
 }
}

@Composable fun CallCheckpointsWorkspace(session:LiveCallDesk.Session){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val ctx=LocalContext.current;val focus=androidx.compose.ui.platform.LocalFocusManager.current
 var board by remember(session){mutableStateOf(CallCheckpoints.Board())};var label by remember(session){mutableStateOf("")};var open by remember(session){mutableStateOf(false)}
 var preview by remember(session){mutableStateOf<CallCheckpoints.Board?>(null)};var clear by remember(session){mutableStateOf(false)};var error by remember(session){mutableStateOf("")}
 var editing by remember(session){mutableStateOf<Pair<CallCheckpoints.Board,Int>?>(null)};var edited by remember(session){mutableStateOf("")};var editError by remember(session){mutableStateOf(false)}
 LaunchedEffect(s.checkpointLabels,s.callCheckpoints){if(!s.checkpointLabels||!s.callCheckpoints){editing=null;edited="";editError=false}}
 var commit by remember(session){mutableStateOf(CallCheckpoints.Commit())}
 LaunchedEffect(s.callCheckpoints){if(!s.callCheckpoints){board=CallCheckpoints.Board();label="";preview=null;open=false;clear=false;commit=CallCheckpoints.Commit()}}
 if(!s.callCheckpoints)return
 GhostButton(if(open)"Hide checkpoints"else"Call checkpoints",Modifier.fillMaxWidth().testTag("checkpoints_toggle")){open=!open}
 if(open)Column(Modifier.fillMaxWidth().padding(vertical=10.dp).testTag("checkpoints_board"),verticalArrangement=Arrangement.spacedBy(8.dp)){
  LdText("MANUAL CHECKPOINTS · ${board.marks.size}/12",12f,FontWeight.Bold,p.accent)
  LdText("Elapsed-time labels, not audio. Nothing saved until preview + confirm.",12f,color=p.muted,maxLines=4)
  if(!board.saved){
   LdField(label,{label=it},"Short label (optional, 80 max)",modifier=Modifier.testTag("checkpoint_label"))
   PrimaryButton("Mark now",enabled=editing==null&&board.marks.size<12&&label.trim().length<=80&&label.none{it.isISOControl()},modifier=Modifier.testTag("checkpoint_mark")){
    try{val seconds=CallCheckpoints.elapsed(session,System.currentTimeMillis())?:error("Call/time unavailable");board=board.add(seconds,label);label="";error="";focus.clearFocus()}catch(_:Exception){error="Mark nahi joda. Call/time ya label check karo."}
   }
  }
  board.marks.forEachIndexed{i,m->GlassCard(Modifier.testTag("checkpoint_row_$i")){
   LdText(CallCheckpoints.time(m.seconds),16f,FontWeight.Bold,color=p.accent)
   LdText(m.label,14f,maxLines=Int.MAX_VALUE)
   if(!board.saved&&s.checkpointLabels&&editing==null)GhostButton("Edit label ${i+1}",Modifier.testTag("checkpoint_edit_$i")){editing=board to i;edited=m.label;editError=false;focus.clearFocus()}
   if(editing?.second==i){
    val expected=editing!!.first
    Column(Modifier.fillMaxWidth().padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     LdText("Edit checkpoint label",17f,FontWeight.Bold,maxLines=3)
     LdText(session.number,13f,maxLines=4,modifier=Modifier.testTag("checkpoint_edit_number"))
     LdText("Mark ${i+1} · ${CallCheckpoints.time(expected.marks[i].seconds)}",14f,FontWeight.Bold,color=p.accent,modifier=Modifier.testTag("checkpoint_edit_time"))
     LdText("Time and order stay. Apply changes only this session label. Blank = Checkpoint. Keep original discards this edit.",12f,color=p.muted,maxLines=8)
     LdField(edited,{edited=it},"Label, 80 max",modifier=Modifier.testTag("checkpoint_edit_label"))
     LdText("${edited.trim().length}/80",12f,color=if(edited.trim().length>80)p.danger else p.muted)
     if(editError)LdText("Not applied. Call, setting or board changed. Cancel and reopen.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("checkpoint_edit_error"))
     PrimaryButton("Apply label",enabled=edited.trim().length<=80&&edited.none{it.isISOControl()},modifier=Modifier.testTag("checkpoint_edit_apply")){val changed=CallCheckpoints.rename(session,expected,board,i,edited);if(changed==null)editError=true else{board=changed;editing=null;edited="";error="";focus.clearFocus()}}
     GhostButton("Keep original",Modifier.testTag("checkpoint_edit_cancel")){editing=null;edited="";focus.clearFocus()}
    }
   }
   if(!board.saved&&editing==null&&s.checkpointLabels)Spacer(Modifier.height(8.dp))
   if(!board.saved&&editing==null)GhostButton("Remove ${i+1}",Modifier.testTag("checkpoint_remove_$i")){board=board.remove(i);error=""}

  }}
  if(s.checkpointTasks&&FeatureControls.enabled(s,"checklist")&&editing==null&&board.marks.isNotEmpty())CheckpointTasksWorkspace(session,board)
  if(board.saved)LdText("Summary saved as one note. Marks locked.",13f,FontWeight.SemiBold,p.accent,modifier=Modifier.testTag("checkpoint_saved"))
  if(board.marks.isNotEmpty()&&!board.saved)PrimaryButton("Preview note summary",enabled=editing==null,modifier=Modifier.testTag("checkpoint_preview")){preview=board;error=""}
  if(board.marks.isNotEmpty()&&editing==null)GhostButton("New empty board",Modifier.testTag("checkpoint_new")){clear=true}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("checkpoint_error"))
 }
 preview?.let{expected->AlertDialog(onDismissRequest={preview=null},title={LdText("Save checkpoint note?",19f,FontWeight.Bold,maxLines=3)},text={Column(Modifier.verticalScroll(rememberScrollState())){
  LdText(session.number,14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("checkpoint_preview_number"))
  LdText(expected.summary(),14f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("checkpoint_preview_text"))
  LdText("Only marked rows are included, not the unfinished label. Creates one new unencrypted note. Existing notes stay. No recording or message.",12f,color=p.muted,maxLines=6)
 }},confirmButton={TextButton(onClick={try{if(commit.save(ctx,session,expected,board)){board=board.copy(saved=true);preview=null;error=""}else{preview=null;error="Not saved. Call, setting or board changed."}}catch(_:Exception){preview=null;error="Note save nahi hui. Saved data preserve kiya; check Notes before retry."}},modifier=Modifier.testTag("checkpoint_save_confirm")){LdText("Save one note",14f,FontWeight.SemiBold)}},dismissButton={TextButton(onClick={preview=null},modifier=Modifier.testTag("checkpoint_save_cancel")){LdText("Keep editing",14f)}})}
 if(clear)AlertDialog(onDismissRequest={clear=false},title={LdText("Start empty board?",19f,FontWeight.Bold,maxLines=3)},text={LdText("Current session marks and unfinished label will be cleared. Saved notes stay.",14f,maxLines=6)},confirmButton={TextButton(onClick={board=CallCheckpoints.Board();label="";preview=null;clear=false;commit=CallCheckpoints.Commit()},modifier=Modifier.testTag("checkpoint_new_confirm")){LdText("Clear session board",14f)}},dismissButton={TextButton(onClick={clear=false},modifier=Modifier.testTag("checkpoint_new_cancel")){LdText("Keep marks",14f)}})
}
