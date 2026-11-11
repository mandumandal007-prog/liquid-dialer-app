package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable fun QuietWeekPage(act:MainActivity,onBack:()->Unit,onOpen:(String)->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope()
 var baseRaw by remember{mutableStateOf(s.quietWeekRules)};var baseMode by remember{mutableStateOf(s.quietWeekMode)}
 val parsed=remember(baseRaw){runCatching{QuietWeek.decode(baseRaw)}}
 var windows by remember(baseRaw){mutableStateOf(parsed.getOrDefault(emptyList()))};var mode by remember(baseMode){mutableStateOf(baseMode)}
 var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
 var editing by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")};var start by remember{mutableStateOf("22:00")};var end by remember{mutableStateOf("07:00")};var selectedDays by remember{mutableStateOf((0..6).toSet())};var enabled by remember{mutableStateOf(true)}
 fun reload(){baseRaw=s.quietWeekRules;baseMode=s.quietWeekMode;windows=runCatching{QuietWeek.decode(baseRaw)}.getOrDefault(emptyList());mode=baseMode;error="";status="Saved schedule loaded"}
 fun edit(w:QuietWeek.Window?){editing=w?.id?:"new";name=w?.name?:"";start=QuietWeek.fmt(w?.start?:1320);end=QuietWeek.fmt(w?.end?:420);selectedDays=w?.days?:(0..6).toSet();enabled=w?.enabled?:true;error="";status=""}
 if(editing!=null){
  PageFrame("Quiet window",{editing=null;error=""}){
   LdText("Draft window · start day chuno",18f,FontWeight.Bold,maxLines=3)
   LdText("22:00-07:00 Monday chunoge to Monday raat se Tuesday subah tak. Start aur end barabar nahi ho sakte. Time phone ke local timezone mein hai.",12.5f,color=p.muted,maxLines=8)
   LdField(name,{name=it.take(40)},"Naam (jaise Work night)",modifier=Modifier.testTag("quiet_week_name"))
   LdField(start,{start=it.take(5)},"Shuru HH:mm",modifier=Modifier.testTag("quiet_week_start"))
   LdField(end,{end=it.take(5)},"Khatam HH:mm",modifier=Modifier.testTag("quiet_week_end"))
   SecTitle("Start days")
   QuietWeek.days.forEachIndexed{i,d->SwitchRow(d,null,i in selectedDays,last=i==6){selectedDays=if(it)selectedDays+i else selectedDays-i}}
   SwitchRow("Window enabled","Weekly mode on hone par ye window lagegi",enabled,last=true){enabled=it}
   if(error.isNotBlank())LdText(error,12.5f,color=p.danger,maxLines=5,modifier=Modifier.testTag("quiet_week_error"))
   PrimaryButton("Draft mein rakho",modifier=Modifier.testTag("quiet_week_keep")){
    runCatching{QuietWeek.Window(if(editing=="new")UUID.randomUUID().toString()else editing!!,name.trim(),selectedDays,QuietWeek.time(start),QuietWeek.time(end),enabled).also{it.validate()}}.fold(onSuccess={w->windows=if(editing=="new")windows+w else windows.map{if(it.id==w.id)w else it};editing=null;error="";status="Draft badla; ab Save schedule dabao"},onFailure={error="Naam, start days aur HH:mm time check karo. Start aur end alag rakho."})
   }
   GhostButton("Cancel draft window"){editing=null;error=""}
  };return
 }
 PageFrame("Weekly Quiet hours",onBack){
  LdText("Alag din, alag silent time",19f,FontWeight.Bold,maxLines=3)
  LdText("Incoming calls ke liye. Sirf silence, reject nahi. Android 10+ aur call-screening role chahiye. Draft edit se active schedule nahi badlega; Save schedule ke baad hi lagega.",12.5f,color=p.muted,maxLines=8)
  GlassCard{
   LdText(if(s.quietHours)"Quiet hours master: ON"else"Quiet hours master: OFF",14f,FontWeight.Bold,maxLines=3)
   LdText(if(android.os.Build.VERSION.SDK_INT<29)"Is Android version par call silence supported nahi"else if(act.screeningRoleHeld())"Call-screening role: available"else"Call-screening role missing: silence kaam nahi karega",12f,color=p.muted,maxLines=5)
   SRow("Quiet hours settings","Master, screening role aur favourite exception wahin hain",LdIcons.Clock,last=true){onOpen("settings:quiet")}
  }
  if(parsed.isFailure){
   LdText("Saved schedule padh nahi saka. Weekly mode is data ke saath calls silent nahi karega. Purana data overwrite nahi kiya gaya.",13f,color=p.danger,maxLines=7,modifier=Modifier.testTag("quiet_week_corrupt"))
   PrimaryButton("Weekly mode off karo",modifier=Modifier.testTag("quiet_week_disable")){if(!busy){busy=true;scope.launch{try{LdApp.instance.ldStore.edit{QuietWeek.disable(it,baseRaw,baseMode)};baseMode=false;mode=false;status="Weekly mode off"}catch(_:Exception){error="Mode off nahi hua. Saved schedule badla ho to reload karo."}finally{busy=false}}}}
  }else{
   GlassCard{SwitchRow("Weekly mode (draft)","Off: purana daily time. On: neeche ke enabled windows. Master ON alag zaroori hai.",mode,last=true){if(!busy){mode=it;status=""}}}
   SecTitle("Windows · ${windows.size}/${QuietWeek.MAX}")
   if(windows.isEmpty())LdText("Abhi koi window nahi. Add karke din aur time chuno.",13f,color=p.muted,maxLines=4)
   windows.forEach{w->GlassCard(Modifier.padding(bottom=10.dp).testTag("quiet_window_${w.id}")){
    LdText(w.name,16f,FontWeight.Bold,maxLines=3)
    LdText("${w.days.sorted().joinToString(" "){QuietWeek.days[it]}} · ${QuietWeek.fmt(w.start)}-${QuietWeek.fmt(w.end)}${if(w.start>w.end)" (agle din tak)"else""} · ${if(w.enabled)"Enabled"else"Off"}",12.5f,color=p.muted,maxLines=5)
    GhostButton("Edit ${w.name}",Modifier.testTag("quiet_edit_${w.id}")){if(!busy)edit(w)}
    GhostButton("Draft se hatao",Modifier.testTag("quiet_remove_${w.id}")){if(!busy){windows=windows.filterNot{it.id==w.id};status="Draft se hata; saved schedule abhi wahi hai"}}
   }}
   if(windows.size<QuietWeek.MAX)PrimaryButton("Add quiet window",modifier=Modifier.testTag("quiet_week_add")){if(!busy)edit(null)}
   SecTitle("Draft week preview")
   LdText("Preview sirf windows ka hai, active silence ka promise nahi. Mode/master/role aur exceptions alag lagu hain. Overlap hua to bhi ek silence decision hoga.",11.5f,color=p.muted,maxLines=7)
   GlassCard{QuietWeek.days.forEachIndexed{i,d->SRow(d,QuietWeek.segments(windows,i).joinToString("\n").ifBlank{"Koi window nahi"},last=i==6,subMaxLines=12)}}
   if(s.quietWeekRules!=baseRaw||s.quietWeekMode!=baseMode)LdText("Saved schedule badal gaya. Reload karke dobara edit karo.",12.5f,color=p.danger,maxLines=5,modifier=Modifier.testTag("quiet_week_stale"))
   PrimaryButton(if(busy)"Saving..."else"Save schedule",modifier=Modifier.testTag("quiet_week_save")){if(!busy){busy=true;error="";val snapshot=windows;val selectedMode=mode;scope.launch{try{LdApp.instance.ldStore.edit{QuietWeek.save(it,baseRaw,baseMode,snapshot,selectedMode)};baseRaw=QuietWeek.encode(snapshot);baseMode=selectedMode;status="Schedule save hua; master aur role zaroor check karo"}catch(_:Exception){error="Save nahi hua. Enabled mode mein ek enabled window chahiye; saved schedule badla ho to reload karo."}finally{busy=false}}}}
  }
  if(error.isNotBlank())LdText(error,12.5f,color=p.danger,maxLines=6,modifier=Modifier.testTag("quiet_week_error"))
  if(status.isNotBlank())LdText(status,12.5f,color=p.accent,maxLines=5,modifier=Modifier.testTag("quiet_week_status"))
  GhostButton("Discard draft / reload",Modifier.testTag("quiet_week_reload")){if(!busy)reload()}
  Spacer(Modifier.height(12.dp));LdText("Favourite exception aur per-number quiet bypass wahi rahenge. Favourite exception ON aur Contacts access nahi mila to safe side par calls bajengi. Weekly rules phone ke current local day/time par har incoming screening ke waqt check hote hain; koi alarm/background scheduler nahi. Ye rules appearance backup mein include nahi hain.",11.5f,color=p.muted,maxLines=12)
 }
}
