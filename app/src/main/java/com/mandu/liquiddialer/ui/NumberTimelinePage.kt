package com.mandu.liquiddialer.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun NumberTimelinePage(number:String,onBack:()->Unit)=NumberTimelineWorkspace(number,onBack)
@Composable fun NumberTimelineWorkspace(number:String,onBack:()->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var allowed by remember{mutableStateOf(ContextCompat.checkSelfPermission(ctx,Manifest.permission.READ_CALL_LOG)==PackageManager.PERMISSION_GRANTED)}
 var days by remember(number){mutableStateOf(30)};var type by remember(number){mutableStateOf(0)};var revision by remember(number){mutableStateOf(0)}
 var rows by remember(number){mutableStateOf<List<Recent>>(emptyList())};var scanned by remember(number){mutableStateOf(0)};var pages by remember(number){mutableStateOf(0)}
 var next by remember(number){mutableStateOf<HistoryExplorer.Anchor?>(null)};var loaded by remember(number){mutableStateOf(false)};var busy by remember(number){mutableStateOf(false)};var error by remember(number){mutableStateOf("")}
 val filter=remember(number,days,type,revision){NumberTimeline.filter(days,type,System.currentTimeMillis())}
 val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){on->allowed=on;revision++}
 val df=remember{SimpleDateFormat("d MMM yyyy, h:mm a",Locale.getDefault())}
 LaunchedEffect(number,filter,allowed){
  rows=emptyList();scanned=0;pages=0;next=null;loaded=false;error=""
  if(allowed){busy=true;try{val b=withContext(Dispatchers.IO){NumberTimeline.page(ctx,number,filter,null)};rows=b.rows;scanned=b.scanned;pages=1;next=b.next;loaded=true}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){error="Call history load nahi hui. Khaali history ka matlab nahi; dobara try karo."}finally{busy=false}}
 }
 fun more(){if(busy||!loaded||next==null||pages>=NumberTimeline.MAX_PAGES)return;val anchor=next;busy=true;scope.launch{try{val b=withContext(Dispatchers.IO){NumberTimeline.page(ctx,number,filter,anchor)};require(b.rows.none{r->rows.any{it.id==r.id}});rows=rows+b.rows;scanned+=b.scanned;pages++;next=b.next;error=""}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){error="Purani calls load nahi hui. Dikh rahi calls nahi badli; dobara try karo."}finally{busy=false}}}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,16f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("timeline_number"))
  LdText("Isi exact number ki SIM-call history. Sirf padhna; koi call, message ya delete nahi.",13f,color=p.muted,maxLines=8)
  LdText("Har page: chune filter ki 100 phone-log rows check, neeche sirf is number ki calls. Load older se 2,000 rows tak. Summary loaded calls ki hai, poori history ki nahi.",12f,color=p.muted,maxLines=10)
  if(!allowed){LdText("Call log permission chahiye. Permission nahi hai, isliye yahan history ka nateeja nahi dikha rahe.",14f,color=p.muted,maxLines=5);PrimaryButton("Call log access do",modifier=Modifier.testTag("timeline_permission")){request.launch(Manifest.permission.READ_CALL_LOG)}}else{
   Spacer(Modifier.height(8.dp));SecTitle("Time window")
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(7 to "7 din",30 to "30 din",0 to "Sab dates").forEach{(d,title)->PrimaryButton(if(days==d)"✓ $title"else title,enabled=!busy,modifier=Modifier.weight(1f).testTag("timeline_days_$d")){days=d}}}
   SecTitle("Call type")
   listOf(listOf(0 to "Sab",1 to "Incoming",2 to "Outgoing"),listOf(3 to "Missed",5 to "Rejected",6 to "Blocked")).forEach{choices->Row(Modifier.fillMaxWidth().padding(bottom=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){choices.forEach{(t,title)->PrimaryButton(if(type==t)"✓ $title"else title,enabled=!busy,modifier=Modifier.weight(1f).testTag("timeline_type_$t")){type=t}}}}
   PrimaryButton(if(busy)"Load ho raha hai..."else "Fresh load",enabled=!busy,modifier=Modifier.testTag("timeline_refresh")){allowed=ContextCompat.checkSelfPermission(ctx,Manifest.permission.READ_CALL_LOG)==PackageManager.PERMISSION_GRANTED;revision++}
   if(error.isNotEmpty())Spacer(Modifier.height(8.dp))
   if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("timeline_error"))
   if(loaded){
    val sum=runCatching{NumberTimeline.summary(rows)}
    Spacer(Modifier.height(8.dp));GlassCard{
     LdText("${rows.size} matching calls · $scanned phone-log rows checked",14f,FontWeight.Bold,maxLines=5,modifier=Modifier.testTag("timeline_count"))
     sum.getOrNull()?.let{s->LdText("Loaded incoming ${s.incoming} · outgoing ${s.outgoing} · missed ${s.missed} · rejected ${s.rejected}",12f,color=p.muted,maxLines=5);LdText("Loaded connected talk: ${fmtDur(s.connectedSeconds)}",14f,maxLines=4,modifier=Modifier.testTag("timeline_duration"))}
     LdText("Window ends: ${df.format(Date(filter.end-1))}",11f,color=p.muted,maxLines=3)
    }
    if(rows.isEmpty())LdText(if(next!=null)"Abhi check ki gayi rows mein is number ki call nahi mili. Load older se aur rows dekho."else "Is filter aur available call log mein is number ki call nahi mili.",14f,color=p.muted,maxLines=6,modifier=Modifier.testTag("timeline_empty"))
    rows.forEach{r->GlassCard(Modifier.padding(top=8.dp).testTag("timeline_row_${r.id}")){
     LdText(typeLabel(r.type),15f,FontWeight.Bold,maxLines=3)
     LdText(df.format(Date(r.date)),14f,maxLines=4)
     LdText(if(r.type in listOf(1,2)&&r.duration>0)"Talk: ${fmtDur(r.duration)}"else "Connected talk nahi dikh rahi",13f,color=p.muted,maxLines=4)
    }}
    if(next!=null&&pages<NumberTimeline.MAX_PAGES)PrimaryButton(if(error.isEmpty())"Load older · next 100 rows"else "Load older dobara try karo",enabled=!busy,modifier=Modifier.testTag("timeline_more")){more()}
    else LdText(if(next!=null)"2,000 rows ki reading limit aa gayi. Aur purani calls ke liye time/type filter chuno. Yeh poori history nahi hai."else "Is filter mein available phone-log rows ka end.",12f,color=p.muted,maxLines=5,modifier=Modifier.testTag("timeline_end"))
   }
  }
 }
 if(inSheet)Column{SheetTitle("Call timeline");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("timeline_notes_back"),onClick=onBack);Spacer(Modifier.height(8.dp));content()}else PageFrame("Call timeline",onBack,content=content)
}
