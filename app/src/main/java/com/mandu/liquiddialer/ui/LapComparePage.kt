package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun LapComparePage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Stopwatch lap comparison",onBack){
  LdText("Kis kaam mein kitna waqt?",22f,FontWeight.Bold,maxLines=4)
  LdText("Paused stopwatch ke measured laps chuno: sirf selected laps ka subtotal, average, shortest aur longest dekho. Original order ya longest-first view. Timer, labels aur saved notes nahi badalte.",14f,color=p.muted,maxLines=11)
  GlassCard{SwitchRow("Stopwatch lap comparison","Default OFF. Read-only timing review.",s.lapCompare,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.lapCompare]=v};error=false}catch(_:Exception){error=true}}}}
  SecTitle("Pause and choose")
  GlassCard{
   LdText("Live call desk + Desk interval stopwatch ON chahiye. Kam se kam ek lap mark karo, phir Pause. Compare measured laps kholo. Koi lap pehle se selected nahi; Select all optional hai. Saved/locked stopwatch bhi review kar sakte ho.",13f,maxLines=11)
   Spacer(Modifier.height(12.dp));LdText("Lap duration pichhle mark se next mark tak ka manually timed interval hai, cumulative total nahi. Manual pauses pehle hi excluded hain. Unmarked remainder alag dikhata hai aur selected subtotal/average mein include nahi hota.",13f,maxLines=11)
  }
  SecTitle("View, not a verdict")
  LdText("Average selected durations ka arithmetic mean hai, whole milliseconds tak rounded down; display 0.1-second precision par truncate hota hai, nonzero <0.1s separately. Same duration par original lap order stays. Longest ka matlab slow/wrong kaam nahi, sirf aapke manual marks ka comparison.",13f,color=p.muted,maxLines=12)
  GlassCard{LdText("Resume, new lap/board, call change, OFF ya close par review selection reset ho sakti hai. Comparison koi note, report, recording, bill, reminder ya message nahi banata. Existing Save stopwatch note original chronological laps save karta hai, comparison nahi. No network, audio, clipboard, storage write or new permission. This switch does not enable other features.",13f,maxLines=15)}
  if(error)LdText("Setting save nahi hui",13f,color=p.danger)
 }
}

@Composable fun LapCompareWorkspace(session:LiveCallDesk.Session,board:DeskStopwatch.Board){
 val p=LocalLd.current
 var open by remember(session,board){mutableStateOf(false)};var selected by remember(session,board){mutableStateOf(emptySet<Int>())};var longest by remember(session,board){mutableStateOf(false)}
 if(!LapCompare.current(session,board,board))return
 val rows=LapCompare.rows(board)
 GhostButton(if(open)"Hide lap comparison"else"Compare measured laps",Modifier.testTag("lap_compare_toggle")){open=!open;if(!open){selected=emptySet();longest=false}}
 if(open)GlassCard(Modifier.testTag("lap_compare_panel")){
  LdText("MEASURED LAPS ONLY",12f,FontWeight.Bold,p.accent,maxLines=3)
  LdText(session.number,14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("lap_compare_number"))
  LdText("Choose measured laps. Nothing preselected. This review changes no timing or saved data.",12f,color=p.muted,maxLines=6)
  GhostButton(if(longest)"Order: longest first"else"Order: original",Modifier.testTag("lap_compare_order")){longest=!longest}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   GhostButton("Select all",Modifier.weight(1f).testTag("lap_compare_all")){selected=rows.map{it.index}.toSet()}
   GhostButton("Clear",Modifier.weight(1f).testTag("lap_compare_clear")){selected=emptySet()}
  }
  LapCompare.ordered(rows,longest).forEach{row->
   Column(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value=row.index in selected,role=Role.Checkbox,onValueChange={checked->selected=if(checked)selected+row.index else selected-row.index}).padding(vertical=10.dp).testTag("lap_compare_row_${row.index}")){
    LdText("${if(row.index in selected)"[x]"else"[ ]"} Lap ${row.index+1} · ${DeskStopwatch.time(row.durationMs)}",14f,FontWeight.Bold,p.accent,maxLines=4)
    LdText(row.label,14f,maxLines=Int.MAX_VALUE)
   }
  }
  if(selected.isNotEmpty()){
   val result=LapCompare.analyze(board,selected)
   LdText("${result.rows.size} selected",15f,FontWeight.Bold,maxLines=3,modifier=Modifier.testTag("lap_compare_count"))
   LdText("Subtotal ${DeskStopwatch.time(result.subtotalMs)}",16f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("lap_compare_subtotal"))
   LdText("Average ${DeskStopwatch.time(result.averageMs)}",14f,maxLines=4,modifier=Modifier.testTag("lap_compare_average"))
   LdText("Shortest ${DeskStopwatch.time(result.shortestMs)} · Longest ${DeskStopwatch.time(result.longestMs)}",14f,maxLines=5,modifier=Modifier.testTag("lap_compare_range"))
  }else LdText("No laps selected. No subtotal yet.",13f,color=p.muted,maxLines=4,modifier=Modifier.testTag("lap_compare_empty"))
  val remainder=board.accumulatedMs-(board.laps.lastOrNull()?.totalMs?:0)
  LdText("Unmarked remainder ${DeskStopwatch.time(remainder)} - excluded from comparison.",12f,color=p.muted,maxLines=6,modifier=Modifier.testTag("lap_compare_remainder"))
  LdText("Read only. Original lap order and stopwatch note stay unchanged. Not billing, recording or a measure of work quality.",12f,color=p.muted,maxLines=7)
 }
}
