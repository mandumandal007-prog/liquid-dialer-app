package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch
@Composable fun WeekdayShiftPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Selected-weekday date shift",onBack){
  LdText("Apne weekdays, apni manual dates.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Selected-weekday date shift","Default OFF. Adds a separate tool inside date calculator.",s.weekdayShift,last=true){v->scope.launch{try{LdApp.instance.repo.set(K.weekdayShift,v);error=false}catch(_:Exception){error=true}}}
  Column(Modifier.testTag("weekday_options_settings")){
   SwitchRow("Weekday selection shortcuts","Default OFF. Mon-Fri, all seven or Sat-Sun; changes selection only, no calculation.",s.weekdayShortcuts,last=true,subMaxLines=5){scope.launch{try{LdApp.instance.repo.set(K.weekdayShortcuts,it);error=false}catch(_:Exception){error=true}}}
   SwitchRow("Fold manual exclusions layout","Default OFF. Manual dates open on demand; summary always visible.",s.weekdayExclusionsFold,last=true,subMaxLines=5){scope.launch{try{LdApp.instance.repo.set(K.weekdayExclusionsFold,it);error=false}catch(_:Exception){error=true}}}
  }
  LdText("Shortcuts replace selected weekdays only and clear stale result. Start, signed count and manual exclusions stay. No automatic Calculate. Folding hides the manual dates field but keeps its current validation summary visible; hidden dates still apply. OFF restores the original field. Fold changes never clear dates or result. No official holidays or business-day guarantee.",13f,color=p.muted,maxLines=18)
  LdText("Date calculator mein Selected-weekday shift kholo. Start date, signed count aur included weekdays chuno. Chaaho to20manual excluded dates daalo. Start never counted; target selected date counted. Existing date modes unchanged.",14f,color=p.muted,maxLines=16)
  GlassCard{LdText("Civil dates1900-2199, count1-3660 forward/backward. No automatic today, official holidays, timezone/elapsed hours, calendar, notification, call/message, clipboard or network. Not business-day, legal deadline or delivery guarantee. Manual excluded dates only. Inputs/result temporary; OFF/close clears this tool.",13f,maxLines=18)}
  if(error)LdText("Setting save nahi hui",13f,color=p.danger)
 }
}
@Composable fun WeekdayShiftWorkspace(onBack:()->Unit,inSheet:Boolean=false){
 androidx.activity.compose.BackHandler(onBack=onBack)
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val focus=androidx.compose.ui.platform.LocalFocusManager.current
 var first by remember{mutableStateOf("")};var count by remember{mutableStateOf("5")};var weekdays by remember{mutableStateOf(setOf(1,2,3,4,5))};var excluded by remember{mutableStateOf("")};var result by remember{mutableStateOf<WeekdayShift.Result?>(null)};var error by remember{mutableStateOf("")}
 var exclusionsOpen by remember{mutableStateOf(false)}
 LaunchedEffect(s.weekdayExclusionsFold){exclusionsOpen=false}
 fun changed(){result=null;error=""}
 val content:@Composable ColumnScope.()->Unit={
  PrimaryButton("Back to date calculator",modifier=Modifier.testTag("weekday_back"),onClick=onBack)
  LdText("SELECTED-WEEKDAY SHIFT",14f,FontWeight.Bold,p.accent,maxLines=4)
  LdText("Start EXCLUDED; target counted. Civil dates only, not business days or a delivery promise. No official holidays.",13f,color=p.muted,maxLines=7)
  LdField(first,{first=it.take(10);changed()},"Start YYYY-MM-DD",modifier=Modifier.testTag("weekday_first"))
  LdField(count,{count=it.take(5);changed()},"Count: 5 forward, -5 backward",modifier=Modifier.testTag("weekday_count"))
  LdText("Count only these weekdays",14f,FontWeight.Bold,maxLines=4)
  if(s.weekdayShortcuts){
   LdText("Replace weekday selection only. Dates/count stay; tap Calculate for a new result.",12f,color=p.muted,maxLines=5)
   WeekdayChoices.Shortcut.entries.forEach{v->GhostButton(v.label,Modifier.fillMaxWidth().testTag("weekday_shortcut_${v.name.lowercase()}")){focus.clearFocus();weekdays=v.days.toSet();changed()}}
  }
  listOf(1 to "Mon",2 to "Tue",3 to "Wed",4 to "Thu",5 to "Fri",6 to "Sat",7 to "Sun").chunked(2).forEach{pair->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){pair.forEach{(id,label)->PrimaryButton(if(id in weekdays)"$label ✓"else label,modifier=Modifier.weight(1f).testTag("weekday_day_$id")){weekdays=if(id in weekdays)weekdays-id else weekdays+id;focus.clearFocus();changed()}}}}
  if(s.weekdayExclusionsFold){
   LdText(WeekdayChoices.summary(excluded)+". Hidden dates still apply. Manual only, not official holidays.",13f,color=p.muted,maxLines=6,modifier=Modifier.testTag("weekday_exclusions_summary"))
   GhostButton(if(exclusionsOpen)"Hide manual exclusions"else"Show manual exclusions",Modifier.testTag("weekday_exclusions_toggle")){focus.clearFocus();exclusionsOpen=!exclusionsOpen}
  }
  if(!s.weekdayExclusionsFold||exclusionsOpen)LdField(excluded,{excluded=it.take(500);changed()},"Excluded dates: comma/space separated",modifier=Modifier.testTag("weekday_excluded"))
  LdText("Optional: up to20 dates YYYY-MM-DD. Only manual exclusions, no holiday lookup. Count±1..3660; years1900-2199.",12f,color=p.muted,maxLines=6)
  PrimaryButton("Calculate selected date",enabled=first.isNotBlank()&&count.isNotBlank()&&weekdays.isNotEmpty(),modifier=Modifier.testTag("weekday_calculate")){
   focus.clearFocus();if(LdApp.settings.value.weekdayShift)runCatching{WeekdayShift.calculate(first,count,weekdays,excluded)}.fold({result=it;error=""},{result=null;error=it.message?:"Check inputs"})
  }
  if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=7,modifier=Modifier.testTag("weekday_error"))
  result?.let{r->GlassCard(Modifier.testTag("weekday_result")){
   LdText(CallDates.label(r.target),22f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("weekday_value"));LdText(r.description().substringAfter('\n'),13f,color=p.muted,maxLines=Int.MAX_VALUE)
  }}
  GhostButton("Clear inputs and result",Modifier.testTag("weekday_clear")){focus.clearFocus();first="";count="5";weekdays=setOf(1,2,3,4,5);excluded="";changed()}
 }
 if(!s.weekdayShift){if(inSheet)Column{LdText("Selected-weekday shift is OFF",14f);PrimaryButton("Back",onClick=onBack)}else PageFrame("Selected-weekday shift",onBack){LdText("Selected-weekday shift is OFF",14f)};return}
 if(inSheet)Column{SheetTitle("Selected-weekday shift");content()}else PageFrame("Selected-weekday shift",onBack,content=content)
}
