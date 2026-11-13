package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun TallyTemplateLibrary(rows:List<CallTally.Item>,step:String,onBack:()->Unit,onLoad:(TallyTemplates.Template)->Unit,inSheet:Boolean=false){
 val p=LocalLd.current;val ctx=LdApp.instance;val scope=rememberCoroutineScope()
 var snapshot by remember{mutableStateOf<TallyTemplates.Snapshot?>(null)};var busy by remember{mutableStateOf(true)};var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var name by remember{mutableStateOf("")}
 var selected by remember{mutableStateOf<TallyTemplates.Template?>(null)};var action by remember{mutableStateOf("")}
 suspend fun reload(){busy=true;error="";try{snapshot=withContext(Dispatchers.IO){TallyTemplates.read(ctx)};selected=null}catch(_:Exception){snapshot=null;error="Templates could not be read. Nothing overwritten. Reload after fixing the local file."}finally{busy=false}}
 LaunchedEffect(Unit){reload()}
 BackHandler{if(!busy)onBack()}
 fun save(){val before=snapshot?:return;busy=true;scope.launch{try{snapshot=withContext(Dispatchers.IO){TallyTemplates.write(ctx,before.raw,TallyTemplates.create(before.templates,name,rows,step))};name="";error="";status="Template saved. Quantities were not stored."}catch(e:Exception){error=e.message?:"Save failed. Nothing replaced."}finally{busy=false}}}
 val content:@Composable ColumnScope.()->Unit={
  LdText("Reusable tally templates",23f,FontWeight.Bold,maxLines=4)
  LdText("Local labels + step only. No count values or linked phone identity. Labels stay as entered. Loading starts every counter at zero. Not orders or stock records.",13f,color=p.muted,maxLines=8)
  PrimaryButton("Reload saved templates",enabled=!busy,modifier=Modifier.testTag("tt_reload")){scope.launch{reload()}}
  if(busy)LdText("Reading / saving local templates…",13f,modifier=Modifier.testTag("tt_busy"))
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=8,modifier=Modifier.testTag("tt_error"))
  if(status.isNotEmpty())LdText(status,13f,color=p.muted,maxLines=6,modifier=Modifier.testTag("tt_status"))
  GlassCard(Modifier.fillMaxWidth().padding(vertical=8.dp)){
   LdText("Save this board as a new template",17f,FontWeight.Bold,maxLines=4)
   LdText("${rows.size} labels · step $step · loads at zero",13f,color=p.muted,maxLines=4,modifier=Modifier.testTag("tt_current"))
   rows.forEach{LdText("• ${it.label}",14f,maxLines=Int.MAX_VALUE)}
   LdField(name,{name=it.take(100);error=""},"Template name",modifier=Modifier.testTag("tt_name"))
   PrimaryButton("Save labels + step",enabled=!busy&&snapshot!=null&&rows.isNotEmpty()&&name.isNotBlank()&&runCatching{CallTally.step(step)}.isSuccess,modifier=Modifier.testTag("tt_save")){save()}
   LdText("Up to 20 saved templates, each with 1-12 labels. Names must be different. Existing templates are never overwritten. Counter edits and template changes are separate; saved deletion has no Undo.",12f,color=p.muted,maxLines=8)
  }
  LdText(if(snapshot==null)"Saved templates unavailable"else"Saved templates (${snapshot?.templates?.size}/20)",17f,FontWeight.Bold,modifier=Modifier.testTag("tt_count"))
  if(snapshot?.templates?.isEmpty()==true)LdText("No saved templates yet.",14f,color=p.muted,modifier=Modifier.testTag("tt_empty"))
  snapshot?.templates?.forEachIndexed{i,t->GlassCard(Modifier.fillMaxWidth().padding(top=8.dp)){
   LdText(t.name,19f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("tt_title_$i"))
   LdText("${t.labels.size} counters · step ${t.step} · starts at zero",13f,color=p.muted,maxLines=4)
   t.labels.forEachIndexed{j,label->LdText("${j+1}. $label",14f,maxLines=Int.MAX_VALUE)}
   PrimaryButton("Load fresh zero board",enabled=!busy,modifier=Modifier.testTag("tt_load_$i")){selected=t;action="load"}
   GhostButton("Delete saved template",Modifier.testTag("tt_delete_$i")){if(!busy){selected=t;action="delete"}}
  }}
 }
 if(inSheet)Column{SheetTitle("Tally templates");PrimaryButton("Back to tally",enabled=!busy,modifier=Modifier.testTag("tt_back")){onBack()};content()}else PageFrame("Tally templates",{if(!busy)onBack()},content=content)
 val t=selected
 if(t!=null&&action.isNotEmpty())Dialog(onDismissRequest={if(!busy){action="";selected=null}}){GlassCard{
  LdText(if(action=="load")"Load ${t.name} at zero?"else"Delete saved template ${t.name}?",18f,FontWeight.Bold,maxLines=8)
  LdText(if(action=="load")"Replaces this session's counters and unfinished counter form. Step becomes ${t.step}. Undo restores the old counters, not the step. The saved template stays."else"Only the saved template is deleted. Current session counters stay. Saved deletion cannot be undone.",13f,color=p.muted,maxLines=10)
  PrimaryButton(if(action=="load")"Load at zero"else"Delete template",enabled=!busy,danger=action=="delete",modifier=Modifier.testTag("tt_confirm")){
   if(action=="load"){action="";selected=null;onLoad(t)}else{val before=snapshot;if(before!=null){busy=true;scope.launch{try{snapshot=withContext(Dispatchers.IO){TallyTemplates.write(ctx,before.raw,TallyTemplates.remove(before.templates,t.id))};error="";status="Saved template deleted. Session board unchanged."}catch(e:Exception){error=e.message?:"Delete failed"}finally{busy=false;action="";selected=null}}}}
  }
  GhostButton("Cancel",Modifier.testTag("tt_cancel")){if(!busy){action="";selected=null}}
 }}
}
