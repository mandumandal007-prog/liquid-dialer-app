package com.mandu.liquiddialer.ui
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun DeskStopwatchPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Desk interval stopwatch",onBack){
  LdText("Ek kaam. Uska apna waqt.",22f,FontWeight.Bold,maxLines=4)
  LdText("Call connection timer se alag: apni task timing manually Start karo, Pause/Resume karo aur short labels ke saath laps mark karo. Audio sunta ya record nahi karta.",14f,color=p.muted,maxLines=10)
  GlassCard{SwitchRow("Desk interval stopwatch","Default OFF. Live call desk bhi ON chahiye.",s.deskStopwatch,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.deskStopwatch]=v};error=false}catch(_:Exception){error=true}}}}
  SecTitle("Manual timing")
  GlassCard{
   LdText("Connected single voice call ke Live call desk mein kholo. Start se waqt ginta hai, connection se nahi. Manual pauses exclude hote hain. Lap mark interval aur total rakhta hai; 12 laps, 80-character labels tak. Blank label = Interval number.",13f,maxLines=12)
   Spacer(Modifier.height(12.dp));LdText("Display roughly har second refresh hota hai; stored times monotonic clock par hain, wall-clock/date change se nahi badalte. Display/note 0.1-second precision par truncate hota hai. Billed, speaking ya active-call duration nahi. 7-day total limit.",13f,maxLines=11)
  }
  SecTitle("Pause. Preview. Save.")
  LdText("Paused timer ki exact number + summary preview ke baad Save one note dabao. Laps ke baad ka waqt Unmarked remainder dikhata hai; unfinished label include nahi hota. Existing notes stay. Successful save board lock karta hai; Reset confirmation se naya empty timer shuru kar sakte ho.",13f,color=p.muted,maxLines=12)
  GlassCard{LdText("Tool hide/reopen while desk stays open: timer continues, even when not visible. Pause before leaving it. Desk close/recreate, call change, Hold, second call, video, feature OFF ya hidden Notes par unsaved timer/laps clear. Saved note stays in existing unencrypted storage. No background service, audio, network, message, clipboard or new permission. This switch does not enable desk/checkpoints.",13f,maxLines=17)}
  if(error)LdText("Setting save nahi hui",13f,color=p.danger)
 }
}

@Composable fun DeskStopwatchWorkspace(session:LiveCallDesk.Session){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val ctx=LocalContext.current;val focus=LocalFocusManager.current
 var board by remember(session){mutableStateOf(DeskStopwatch.Board())};var label by remember(session){mutableStateOf("")};var open by remember(session){mutableStateOf(false)}
 var preview by remember(session){mutableStateOf<DeskStopwatch.Board?>(null)};var reset by remember(session){mutableStateOf(false)};var error by remember(session){mutableStateOf("")};var now by remember(session){mutableLongStateOf(SystemClock.elapsedRealtime())}
 var commit by remember(session){mutableStateOf(DeskStopwatch.Commit())}
 LaunchedEffect(s.deskStopwatch){if(!s.deskStopwatch){board=DeskStopwatch.Board();label="";open=false;preview=null;reset=false;error="";commit=DeskStopwatch.Commit()}}
 LaunchedEffect(s.deskStopwatch,open,board.runningSince){if(s.deskStopwatch&&open&&board.runningSince!=null)while(true){now=SystemClock.elapsedRealtime();delay(1000)}}
 if(!s.deskStopwatch)return
 fun change(action:String){try{board=DeskStopwatch.change(session,board,SystemClock.elapsedRealtime(),action,label);now=SystemClock.elapsedRealtime();if(action=="lap")label="";error="";focus.clearFocus()}catch(_:Exception){error="Timer not changed. Check call, label, clock or limit."}}
 GhostButton(if(open)"Hide stopwatch"else"Interval stopwatch",Modifier.testTag("desk_watch_toggle")){open=!open;now=SystemClock.elapsedRealtime()}
 if(open)Column(Modifier.fillMaxWidth().padding(vertical=10.dp).testTag("desk_watch_panel"),verticalArrangement=Arrangement.spacedBy(8.dp)){
  LdText("MANUAL INTERVAL STOPWATCH",12f,FontWeight.Bold,p.accent,maxLines=3)
  val elapsed=runCatching{board.elapsed(now)}.getOrNull()
  LdText(elapsed?.let{DeskStopwatch.time(it)}?:"Timer unavailable",24f,FontWeight.Bold,p.accent,maxLines=3,modifier=Modifier.testTag("desk_watch_time"))
  LdText(if(board.saved)"Saved. Timer locked."else if(board.runningSince!=null)"Running. Manual pauses excluded."else"Paused / not started. Not call duration.",12f,color=p.muted,maxLines=4,modifier=Modifier.testTag("desk_watch_state"))
  if(!board.saved){
   PrimaryButton(if(board.runningSince!=null)"Pause stopwatch"else if(board.accumulatedMs>0)"Resume stopwatch"else"Start stopwatch",modifier=Modifier.testTag("desk_watch_start")){change(if(board.runningSince!=null)"pause"else"start")}
   LdField(label,{label=it},"Lap label (optional, 80 max)",modifier=Modifier.testTag("desk_watch_label"))
   PrimaryButton("Mark lap (${board.laps.size}/12)",enabled=board.runningSince!=null&&board.laps.size<12&&label.trim().length<=80&&label.none{it.isISOControl()},modifier=Modifier.testTag("desk_watch_lap")){change("lap")}
  }
  board.laps.forEachIndexed{i,l->GlassCard(Modifier.testTag("desk_watch_row_$i")){
   LdText(l.label,14f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
   LdText("Lap ${DeskStopwatch.time(l.totalMs-(board.laps.getOrNull(i-1)?.totalMs?:0))} · total ${DeskStopwatch.time(l.totalMs)}",13f,color=p.muted,maxLines=5)
  }}
  if(s.lapCompare&&board.runningSince==null&&board.laps.isNotEmpty())LapCompareWorkspace(session,board)
  if(!board.saved)PrimaryButton("Preview stopwatch note",enabled=board.runningSince==null&&board.accumulatedMs>0,modifier=Modifier.testTag("desk_watch_preview")){preview=board;error=""}
  GhostButton("Reset stopwatch",Modifier.testTag("desk_watch_reset")){reset=true}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("desk_watch_error"))
 }
 // Inline preview preserves all fixed call controls on narrow screens.
 preview?.let{expected->if(open)GlassCard(Modifier.testTag("desk_watch_preview_panel")){
  LdText("Save stopwatch note?",18f,FontWeight.Bold,maxLines=3)
  LdText(session.number,14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("desk_watch_preview_number"))
  LdText(expected.summary(),14f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("desk_watch_preview_text"))
  PrimaryButton("Save one note",modifier=Modifier.testTag("desk_watch_save")){
   try{if(commit.save(ctx,session,expected,board)){board=board.copy(saved=true);preview=null;error=""}else{preview=null;error="Not saved. Call, timer or setting changed. Pause and preview again."}}catch(_:Exception){preview=null;error="Note not saved. Existing file kept. Check Notes before retry."}
  }
  GhostButton("Cancel save",Modifier.testTag("desk_watch_cancel")){preview=null}
 }}
 if(reset)AlertDialog(onDismissRequest={reset=false},title={LdText("Reset stopwatch?",19f,FontWeight.Bold,maxLines=3)},text={LdText("Discard this timer, laps and unfinished label? Saved notes stay. Reset does not start the new timer or change the call.",14f,maxLines=8)},confirmButton={TextButton(onClick={board=DeskStopwatch.Board();label="";preview=null;reset=false;error="";commit=DeskStopwatch.Commit();now=SystemClock.elapsedRealtime()},modifier=Modifier.testTag("desk_watch_reset_confirm")){LdText("Reset timer",14f)}},dismissButton={TextButton(onClick={reset=false},modifier=Modifier.testTag("desk_watch_reset_cancel")){LdText("Keep timer",14f)}})
}
