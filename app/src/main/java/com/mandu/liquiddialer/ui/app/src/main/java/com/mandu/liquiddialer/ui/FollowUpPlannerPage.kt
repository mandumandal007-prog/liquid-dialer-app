package com.mandu.liquiddialer.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.AppState
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable fun FollowUpPlannerPage(st:AppState,act:MainActivity,onBack:()->Unit) {
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var lists by remember {mutableStateOf<List<CallLists.ListItem>>(emptyList())}
 var loaded by remember {mutableStateOf(false)};var busy by remember {mutableStateOf(false)};var error by remember {mutableStateOf("")}
 var revision by remember {mutableIntStateOf(0)};var now by remember {mutableLongStateOf(System.currentTimeMillis())}
 var view by remember {mutableStateOf(FollowUpPlanner.View.TODAY)};var query by remember {mutableStateOf("")};var outcome by remember {mutableStateOf<String?>(null)}
 var visible by remember(view,query,outcome){mutableIntStateOf(20)}
 var editing by remember {mutableStateOf<Pair<String,String>?>(null)};var session by remember {mutableStateOf<Pair<String,String>?>(null)}
 var reminder by remember {mutableStateOf<FollowUpPlanner.Item?>(null)};var currentReminder by remember {mutableStateOf<CallReminders.Reminder?>(null)}
 LaunchedEffect(revision){try{lists=withContext(Dispatchers.IO){CallLists.read(ctx)};loaded=true;now=System.currentTimeMillis();error=""}catch(_:Exception){error="Lists khul nahi rahi; saved data nahi badla."}}
 LaunchedEffect(Unit){while(true){kotlinx.coroutines.delay(60_000);now=System.currentTimeMillis()}}
 fun update(action:()->List<CallLists.ListItem>,after:()->Unit={}){if(busy)return;busy=true;error="";scope.launch{try{lists=withContext(Dispatchers.IO){action()};now=System.currentTimeMillis();after()}catch(e:Exception){error=e.message ?: "Save nahi hua; purana data bacha hai."}finally{busy=false}}}
 val df=remember{SimpleDateFormat("EEE, d MMM yyyy · HH:mm z",Locale.getDefault())}
 reminder?.let {item->
  ReminderEditor(currentReminder,onDismiss={reminder=null},onSaved={reminder=null;revision++},initialNumber=item.entry.number,initialName=item.entry.name,
   initialAt=item.entry.dueAt.takeIf{it>now},contextNotice=if(currentReminder!=null)"Is number ka reminder pehle se hai. Uska current time neeche hai. Save karoge tabhi badlega. Plan se alag reminder hai; Done ya plan hataane se cancel nahi hoga." else "Ye alag Reminder Center reminder hai. Save karoge tabhi notification banegi. Plan badalne, Done lagane ya plan hataane se ye cancel nahi hoga.")
  BackHandler{reminder=null};return
 }
 session?.let {target->val list=lists.firstOrNull{it.id==target.first};if(list!=null){
  CallListSessionPage(list,target.second,busy,error,onBack={session=null;error=""},onCall={act.callUi(it)},onSave={eid,status,note,after->update({CallLists.followUp(ctx,list.id,eid,status,note)},after)})
  return
 }else session=null}
 editing?.let {target->val list=lists.firstOrNull{it.id==target.first};val entry=list?.entries?.firstOrNull{it.id==target.second}
  if(list!=null && entry!=null){
   PlannerDateEditor(list.title,entry,busy,error,onBack={editing=null;error=""},onSave={at->update({CallLists.schedule(ctx,list.id,entry.id,at)}){editing=null}})
   return
  }else editing=null
 }
 val labels=listOf(FollowUpPlanner.View.TODAY to "Aaj",FollowUpPlanner.View.OVERDUE to "Late",FollowUpPlanner.View.UPCOMING to "Aage",FollowUpPlanner.View.UNSCHEDULED to "Bina date",FollowUpPlanner.View.ALL to "Sab Pending")
 val shown=remember(lists,view,query,outcome,now){FollowUpPlanner.find(lists,view,query,outcome,now)}
 PageFrame("Follow-up Planner",onBack){
  LdText("Saari Call Lists ke Pending follow-ups. Date sirf plan hai, notification nahi. Purane Call later ko apne aap date/reminder nahi diya gaya. Call aur outcome tum khud chunte ho.",12f,color=p.muted,maxLines=7)
  LdText("Late = chuna samay beet gaya. Aaj mein aaj ke late items bhi hain. Done entries yahan nahi dikhengi; Call Lists mein rahengi.",11.5f,color=p.muted,maxLines=5)
  PrimaryButton("Dobara load karo",enabled=!busy,modifier=Modifier.testTag("planner_reload")){revision++}
  Spacer(Modifier.height(8.dp))
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("planner_error"))
  labels.chunked(2).forEach{row->Row(Modifier.fillMaxWidth().padding(bottom=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{(v,label)->PrimaryButton(if(view==v)"✓ $label" else label,modifier=Modifier.weight(1f).semantics{selected=view==v}.then(if(view==v)Modifier.border(2.dp,p.accent,RoundedCornerShape(16.dp)) else Modifier).testTag("planner_view_${v.name}")){view=v}};if(row.size==1)Spacer(Modifier.weight(1f))}}
  OutlinedTextField(query,{query=it},label={Text("List, naam, number ya note")},modifier=Modifier.fillMaxWidth().testTag("planner_search"))
  Spacer(Modifier.height(8.dp))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   PrimaryButton(if(outcome==null)"Outcome: Sab" else CallLists.outcomes.getValue(outcome!!),modifier=Modifier.weight(1f).testTag("planner_outcome")){
    val options=listOf<String?>(null,"pending","no_answer","busy","later");outcome=options[(options.indexOf(outcome)+1)%options.size]
   }
  }
  LdText(if(!loaded)"Lists load ho rahi hain..." else "${shown.size} follow-ups",13f,FontWeight.Bold,modifier=Modifier.testTag("planner_count"))
  if(loaded && shown.isEmpty())LdText("Is view mein koi Pending follow-up nahi. Bina date dekho ya Call Lists mein entries jodo.",13f,color=p.muted,maxLines=4)
  shown.take(visible).forEach{item->val e=item.entry
   GlassCard(Modifier.padding(top=12.dp).testTag("planner_item_${e.id}")){
    LdText(item.title,12f,FontWeight.SemiBold,p.accent,maxLines=2)
    LdText(e.name.ifBlank{e.number},18f,FontWeight.Bold,maxLines=3);LdText(e.number,13f,color=p.muted,maxLines=2)
    LdText(if(e.dueAt==0L)"Date nahi chuni" else df.format(Date(e.dueAt)),13f,color=if(e.dueAt>0 && e.dueAt<now)p.danger else p.accent,maxLines=3)
    LdText(CallLists.outcomes.getValue(e.outcome),12f,color=p.muted)
    if(e.note.isNotBlank())LdText(e.note,12f,color=p.muted,maxLines=3)
    PrimaryButton("Date / time chuno",enabled=!busy,modifier=Modifier.testTag("planner_date_${e.id}")){editing=item.listId to e.id}
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Outcome / poora note kholo",enabled=!busy,modifier=Modifier.testTag("planner_open_${e.id}")){session=item.listId to e.id}
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Call karo",enabled=!busy,modifier=Modifier.testTag("planner_call_${e.id}")){act.callUi(e.number)}
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Alag reminder banao / dekho",enabled=!busy,modifier=Modifier.testTag("planner_reminder_${e.id}")){
     busy=true;scope.launch{try{currentReminder=withContext(Dispatchers.IO){CallReminders.forNumber(ctx,e.number)};reminder=item}catch(_:Exception){error="Reminder khul nahi raha; kuch nahi badla."}finally{busy=false}}
    }
   }
  }
  if(shown.size>visible)PrimaryButton("20 aur dikhao"){visible+=20}
 }
}

@Composable private fun PlannerDateEditor(title:String,entry:CallLists.Entry,busy:Boolean,error:String,onBack:()->Unit,onSave:(Long)->Unit){
 val ctx=LocalContext.current;val p=LocalLd.current
 val initial=remember(entry.id){Calendar.getInstance().apply{timeInMillis=entry.dueAt.takeIf{it>0} ?: System.currentTimeMillis()+3600000}}
 var date by remember(entry.id){mutableStateOf(SimpleDateFormat("yyyy-MM-dd",Locale.US).format(initial.time))};var time by remember(entry.id){mutableStateOf(SimpleDateFormat("HH:mm",Locale.US).format(initial.time))}
 var localError by remember{mutableStateOf("")}
 val initialDate=remember(entry.id){date};val initialTime=remember(entry.id){time}
 var discard by remember{mutableStateOf(false)}
 fun leave(){if(!busy){if(date!=initialDate || time!=initialTime)discard=true else onBack()}}
 PageFrame("Follow-up ka plan",{leave()}){
  LdText(title,12f,color=p.accent,maxLines=2);LdText(entry.name.ifBlank{entry.number},20f,FontWeight.Bold,maxLines=3);LdText(entry.number,13f,color=p.muted,maxLines=2)
  LdText("${TimeZone.getDefault().id} · Date sirf is entry ka plan hai. Koi reminder/automatic call nahi banega. Saved reminder alag hai; is screen se nahi badlega.",12f,color=p.muted,maxLines=6)
  OutlinedTextField(date,{date=it},label={Text("Date YYYY-MM-DD")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("planner_date"))
  OutlinedTextField(time,{time=it},label={Text("Time HH:mm (24h)")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("planner_time"))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TextButton(enabled=!busy,onClick={DatePickerDialog(ctx,{_,y,m,d->date=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)},initial.get(Calendar.YEAR),initial.get(Calendar.MONTH),initial.get(Calendar.DAY_OF_MONTH)).show()}){Text("Date chuno")}
   TextButton(enabled=!busy,onClick={TimePickerDialog(ctx,{_,h,m->time=String.format(Locale.US,"%02d:%02d",h,m)},initial.get(Calendar.HOUR_OF_DAY),initial.get(Calendar.MINUTE),true).show()}){Text("Time chuno")}
  }
  if(localError.isNotEmpty() || error.isNotEmpty())LdText(localError.ifEmpty{error},13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("planner_date_error"))
  PrimaryButton("Plan save karo",enabled=!busy,modifier=Modifier.testTag("planner_date_save")){
   val at=try{ReminderTime.parse(date,time)}catch(e:Exception){localError=e.message ?: "Date/time sahi karo";return@PrimaryButton};onSave(at)
  }
  Spacer(Modifier.height(8.dp))
  if(entry.dueAt>0)PrimaryButton("Sirf plan ki date hatao",enabled=!busy,modifier=Modifier.testTag("planner_date_clear")){onSave(0)}
  Spacer(Modifier.height(8.dp))
  PrimaryButton("Rehne do",enabled=!busy,modifier=Modifier.testTag("planner_date_cancel"),onClick={leave()})
 }
 if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Badlav save nahi hue")},text={Text("Badli date/time chhod kar wapas jaana hai?")},confirmButton={TextButton(onClick={discard=false;onBack()},modifier=Modifier.testTag("planner_date_discard")){Text("Badlav chhodo")}},dismissButton={TextButton(onClick={discard=false}){Text("Yahin raho")}})
 BackHandler{leave()}
}
