package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun CallDatesPage(onBack:()->Unit){
 val p=LocalLd.current;val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();var error by remember{mutableStateOf("")};var open by remember{mutableStateOf(false)}
 if(open){CallDatesWorkspace({open=false});return}
 PageFrame("Call date calculator",onBack){
  LdText("Date bolte waqt weekday bhi dekho.",23f,FontWeight.Bold,maxLines=4)
  SwitchRow("Show date calculator in call Notes","Default OFF. Manual civil dates only, not call audio or a calendar connection.",s.callDates,last=true){scope.launch{try{LdApp.instance.repo.set(K.callDates,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  PrimaryButton("Open date calculator",modifier=Modifier.testTag("dates_settings_open")){open=true}
  LdText("Add/subtract calendar days, signed days between dates, Monday-Friday date count. YYYY-MM-DD, years1900-2199. Start/end inclusion is stated with each result. Mon-Fri does NOT exclude holidays and is NOT a business-day/deadline guarantee.",14f,color=p.muted,maxLines=12)
  LdText("No automatic today, timezone/elapsed-hour calculation, calendar edits, reminders, messages, clipboard or cloud. Last10 results only in this open screen's memory. Back/close clears them. Check actual delivery/holiday rules yourself.",13f,color=p.muted,maxLines=10)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}

@Composable fun CallDatesWorkspace(onBack:()->Unit,inSheet:Boolean=false){
 val p=LocalLd.current;val focus=androidx.compose.ui.platform.LocalFocusManager.current
 var mode by remember{mutableStateOf(CallDates.Mode.SHIFT)};var first by remember{mutableStateOf("")};var last by remember{mutableStateOf("")};var days by remember{mutableStateOf("7")}
 var result by remember{mutableStateOf<CallDates.Result?>(null)};var error by remember{mutableStateOf("")};val history=remember{CallDates.History()};var revision by remember{mutableIntStateOf(0)};val rows=remember(revision){history.rows}
 fun changed(){result=null;error=""}
 BackHandler{onBack()}
 val content:@Composable ColumnScope.()->Unit={
  LdText("Civil date arithmetic · offline",16f,FontWeight.Bold,maxLines=3)
  LdText("Manually enter YYYY-MM-DD. No automatic today. Dates1900-2199, calendar days not elapsed hours.",12f,color=p.muted,maxLines=5)
  Column(verticalArrangement=Arrangement.spacedBy(5.dp)){CallDates.Mode.entries.forEach{m->GhostButton((if(mode==m)"✓ "else"")+m.label,Modifier.fillMaxWidth().testTag("dates_mode_${m.name.lowercase()}")){focus.clearFocus();mode=m;changed()}}}
  LdText(if(mode==CallDates.Mode.SHIFT)"Start date (day 0)"else"Start date",13f,FontWeight.Bold)
  LdField(first,{first=it.take(10);changed()},"YYYY-MM-DD",modifier=Modifier.testTag("dates_first"))
  if(mode==CallDates.Mode.SHIFT){
   LdText("Signed whole days: 7 adds, -7 subtracts",12f,color=p.muted,maxLines=3)
   LdField(days,{days=it.take(6);changed()},"Whole days",modifier=Modifier.testTag("dates_days"))
  }else{
   LdText("End date",13f,FontWeight.Bold)
   LdField(last,{last=it.take(10);changed()},"YYYY-MM-DD",modifier=Modifier.testTag("dates_last"))
   GhostButton("Swap dates",Modifier.testTag("dates_swap")){focus.clearFocus();val old=first;first=last;last=old;changed()}
  }
  LdText(when(mode){CallDates.Mode.SHIFT->"Start = day0. Weekends INCLUDED. Shift limited to ±36500 days.";CallDates.Mode.GAP->"End minus start, signed. Same date =0. Not inclusive counting.";CallDates.Mode.WEEKDAYS->"Start INCLUDED; end EXCLUDED. Monday-Friday only. Holidays NOT excluded; not business days."},13f,color=p.muted,maxLines=6,modifier=Modifier.testTag("dates_rules"))
  PrimaryButton("Calculate",enabled=first.isNotEmpty()&&(if(mode==CallDates.Mode.SHIFT)days.isNotEmpty()else last.isNotEmpty()),modifier=Modifier.testTag("dates_calculate")){
   focus.clearFocus();runCatching{CallDates.calculate(mode,first,last,days)}.fold({result=it;history.add(it);revision++;error=""},{result=null;error=it.message?:"Dates check karo"})
  }
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("dates_error"))
  result?.let{r->GlassCard(Modifier.fillMaxWidth().padding(vertical=8.dp).testTag("dates_result")){
   LdText(r.title,22f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("dates_value"));LdText(r.detail,14f,color=p.muted,maxLines=Int.MAX_VALUE)
  }}
  GhostButton("Clear inputs & result",Modifier.testTag("dates_clear")){focus.clearFocus();first="";last="";days="7";changed()}
  LdText("Session history: ${rows.size} /10",14f,FontWeight.Bold,modifier=Modifier.testTag("dates_history_count"))
  LdText("Memory only. Back/close clears history; no calendar/reminder saved.",12f,color=p.muted,maxLines=4)
  rows.forEachIndexed{i,r->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp).testTag("dates_history_$i")){LdText(r.title,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE);LdText(r.detail,13f,color=p.muted,maxLines=Int.MAX_VALUE)}}
  GhostButton("Clear session history",Modifier.testTag("dates_history_clear")){history.clear();revision++}
 }
 if(inSheet)Column{SheetTitle("Date calculator");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("dates_back")){onBack()};content()}else PageFrame("Date calculator",onBack,content=content)
}
