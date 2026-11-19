package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
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
  Column(Modifier.testTag("dates_history_settings")){
   SwitchRow("Reuse date inputs","Default OFF. Confirm restoring original mode, dates and signed days from session history.",s.datesRecall,last=true,subMaxLines=5){scope.launch{try{LdApp.instance.repo.set(K.datesRecall,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
   SwitchRow("Fold date history layout","Default OFF. Count and Clear visible, results open on demand.",s.datesHistoryFold,last=true){scope.launch{try{LdApp.instance.repo.set(K.datesHistoryFold,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  }
  Box(Modifier.testTag("dates_paired_switch")){SwitchRow("Paired date-mode buttons","Default OFF. Show the first two mode buttons side by side and Mon-Fri below. Layout only; switching this option keeps current fields, result and history.",s.datesPaired,last=true,subMaxLines=7){scope.launch{try{LdApp.instance.repo.set(K.datesPaired,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}}
  Box(Modifier.testTag("dates_large_result_switch")){SwitchRow("Larger date result","Default OFF. Enlarge the current result and its detail only. Full text wraps; history keeps its original size. No new calculation or history entry.",s.datesLargeResult,last=true,subMaxLines=7){scope.launch{try{LdApp.instance.repo.set(K.datesLargeResult,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}}
  LdText("Reuse inputs previews the original mode and relevant fields. Confirm replaces current form and clears result. No calculation or new history until you tap Calculate. Result text is never used as input. Cancel keeps current form. Reuse OFF dismisses confirmation. Fold history keeps count/Clear visible; OFF restores original expanded results. Layout changes fold entries without clearing form/result/history. Last10 entries only in this open calculator. Back/close/restart clears them. No calendar, reminder, clipboard, file, call or message action.",13f,color=p.muted,maxLines=22)
  PrimaryButton("Open date calculator",modifier=Modifier.testTag("dates_settings_open")){open=true}
  LdText("Add/subtract calendar days, signed days between dates, Monday-Friday date count. YYYY-MM-DD, years1900-2199. Start/end inclusion is stated with each result. Mon-Fri does NOT exclude holidays and is NOT a business-day/deadline guarantee.",14f,color=p.muted,maxLines=12)
  LdText("No automatic today, timezone/elapsed-hour calculation, calendar edits, reminders, messages, clipboard or cloud. Last10 results only in this open screen's memory. Back/close clears them. Check actual delivery/holiday rules yourself.",13f,color=p.muted,maxLines=10)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}

@Composable fun CallDatesWorkspace(onBack:()->Unit,inSheet:Boolean=false){
 val s by LdApp.settings.collectAsState();var weekdayOpen by remember{mutableStateOf(false)}
 LaunchedEffect(s.weekdayShift){if(!s.weekdayShift)weekdayOpen=false}
 val p=LocalLd.current;val focus=androidx.compose.ui.platform.LocalFocusManager.current
 var mode by remember{mutableStateOf(CallDates.Mode.SHIFT)};var first by remember{mutableStateOf("")};var last by remember{mutableStateOf("")};var days by remember{mutableStateOf("7")}
 var result by remember{mutableStateOf<CallDates.Result?>(null)};var error by remember{mutableStateOf("")};val history=remember{DatesRecall.History()};var revision by remember{mutableIntStateOf(0)};val rows=remember(revision){history.rows}
 if(weekdayOpen&&s.weekdayShift){WeekdayShiftWorkspace({weekdayOpen=false},inSheet);return}
 var recall by remember{mutableStateOf<DatesRecall.Entry?>(null)};var restored by remember{mutableStateOf(false)};var historyOpen by remember{mutableStateOf(false)}
 LaunchedEffect(s.datesRecall){if(!s.datesRecall){recall=null;restored=false}}
 LaunchedEffect(s.datesHistoryFold){historyOpen=false}
 fun changed(){result=null;error="";recall=null;restored=false}
 BackHandler{onBack()}
 val content:@Composable ColumnScope.()->Unit={
  if(s.weekdayShift)GhostButton("Selected-weekday shift",Modifier.testTag("dates_weekday_open")){weekdayOpen=true}
  LdText("Civil date arithmetic · offline",16f,FontWeight.Bold,maxLines=3)
  LdText("Manually enter YYYY-MM-DD. No automatic today. Dates1900-2199, calendar days not elapsed hours.",12f,color=p.muted,maxLines=5)
  val selectMode:(CallDates.Mode)->Unit={m->focus.clearFocus();mode=m;changed()}
  if(!s.datesPaired)Column(Modifier.testTag("dates_modes_original"),verticalArrangement=Arrangement.spacedBy(5.dp)){CallDates.Mode.entries.forEach{m->GhostButton((if(mode==m)"✓ "else"")+m.label,Modifier.fillMaxWidth().testTag("dates_mode_${m.name.lowercase()}")){selectMode(m)}}}
  else Column(Modifier.testTag("dates_modes_paired"),verticalArrangement=Arrangement.spacedBy(8.dp)){
   CallDates.Mode.entries.chunked(2).forEach{pair->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    pair.forEach{m->Box(Modifier.weight(1f).buttonGlass(p,RoundedCornerShape(16.dp)).testTag("dates_mode_${m.name.lowercase()}").clickable{selectMode(m)}.padding(horizontal=8.dp,vertical=13.dp),contentAlignment=Alignment.Center){LdText((if(mode==m)"✓ "else"")+m.label,14f,FontWeight.SemiBold,p.accent,maxLines=Int.MAX_VALUE)}}
   }}
  }
  if(restored)LdText("Original date inputs restored. Check fields and rules, then tap Calculate. No new result yet.",13f,color=p.accent,maxLines=7,modifier=Modifier.testTag("dates_restored"))
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
   focus.clearFocus();restored=false;runCatching{DatesRecall.create(mode,first,last,days)}.fold({result=it.result;history.add(it);revision++;error=""},{result=null;error=it.message?:"Dates check karo"})
  }
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("dates_error"))
  result?.let{r->GlassCard(Modifier.fillMaxWidth().padding(vertical=8.dp).testTag("dates_result")){
   LdText(r.title,if(s.datesLargeResult)28f else 22f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("dates_value"));if(s.datesLargeResult)Spacer(Modifier.height(8.dp));LdText(r.detail,if(s.datesLargeResult)16f else 14f,color=p.muted,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("dates_result_detail"))
  }}
  GhostButton("Clear inputs & result",Modifier.testTag("dates_clear")){focus.clearFocus();first="";last="";days="7";changed()}
  LdText("Session history: ${rows.size} /10",14f,FontWeight.Bold,modifier=Modifier.testTag("dates_history_count"))
  LdText("Memory only. Back/close clears history; no calendar/reminder saved.",12f,color=p.muted,maxLines=4)
  if(s.datesHistoryFold)GhostButton(if(historyOpen)"Hide date history"else"Show date history",Modifier.testTag("dates_history_toggle")){historyOpen=!historyOpen}
  if(!s.datesHistoryFold||historyOpen)rows.forEachIndexed{i,e->val r=e.result;GlassCard(Modifier.fillMaxWidth().padding(top=6.dp).testTag("dates_history_$i")){
   LdText(r.title,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE);LdText(r.detail,13f,color=p.muted,maxLines=Int.MAX_VALUE)
   if(s.datesRecall){Spacer(Modifier.height(8.dp));GhostButton("Reuse inputs",Modifier.testTag("dates_recall_$i")){focus.clearFocus();recall=e}}
  }}
  GhostButton("Clear session history",Modifier.testTag("dates_history_clear")){history.clear();revision++;recall=null}
 }
 if(s.datesRecall)recall?.let{e->val v=e.input;androidx.compose.ui.window.Dialog(onDismissRequest={recall=null}){GlassCard{
  LdText("Replace current date inputs?",19f,FontWeight.Bold,maxLines=6)
  LdText("${v.mode.label}. Start: ${v.first}. "+if(v.mode==CallDates.Mode.SHIFT)"Signed calendar days: ${v.days}."else"End: ${v.last}.",14f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("dates_recall_summary"))
  LdText("Replaces current mode and fields, clears result. Nothing calculates automatically. History stays. Check inclusion/holiday rules before Calculate.",13f,color=p.muted,maxLines=10)
  PrimaryButton("Use original inputs",modifier=Modifier.testTag("dates_recall_confirm")){
   if(LdApp.settings.value.datesRecall&&e in history.rows)runCatching{DatesRecall.input(e)}.fold({x->focus.clearFocus();mode=x.mode;first=x.first;last=x.last;days=x.days;changed();restored=true},{recall=null;error=it.message?:"History invalid"})else recall=null
  }
  GhostButton("Keep current inputs",Modifier.testTag("dates_recall_cancel")){recall=null}
 }}}
 if(inSheet)Column{SheetTitle("Date calculator");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("dates_back")){onBack()};content()}else PageFrame("Date calculator",onBack,content=content)
}
