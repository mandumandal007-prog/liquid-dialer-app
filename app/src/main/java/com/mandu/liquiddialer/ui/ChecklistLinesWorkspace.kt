package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun ChecklistLinesWorkspace(number:String,onBack:()->Unit,onSaved:(List<CallChecklist.Item>)->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();val focus=LocalFocusManager.current
 var source by remember(number){mutableStateOf<List<CallChecklist.Item>?>(null)};var input by remember(number){mutableStateOf("")}
 var preview by remember(number){mutableStateOf<ChecklistLines.Preview?>(null)};var selected by remember(number){mutableStateOf<Set<Int>>(emptySet())}
 var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
 fun invalidate(){preview=null;selected=emptySet();error="";status=""}
 fun load(){if(busy)return;busy=true;scope.launch{try{source=withContext(Dispatchers.IO){CallChecklist.read(ctx,number)};invalidate();status="Saved checklist loaded. Input kept; preview again."}catch(_:Exception){source=null;preview=null;selected=emptySet();error="Checklist load failed. Saved file unchanged; Save disabled."}finally{busy=false}}}
 fun back(){focus.clearFocus();if(!busy){if(input.isNotBlank())confirm="back"else onBack()}}
 LaunchedEffect(number){load()};BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("lines_number"))
  LdText("Several topics. One checked Save.",21f,FontWeight.Bold,maxLines=4)
  LdText("One line = one topic. Blank lines ignored; bullets/numbers are NOT stripped. Existing and repeated topics are skipped by case and whitespace comparison. Text is kept with outer whitespace trimmed. No clipboard is read automatically.",13f,color=p.muted,maxLines=10)
  source?.let{LdText("${it.size} saved topics · ${CallChecklist.MAX_ITEMS-it.size} spaces",14f,FontWeight.Bold,modifier=Modifier.testTag("lines_capacity"))}
  LdField(input,{if(!busy){input=it;invalidate()}},"One topic per line",minLines=5,modifier=Modifier.testTag("lines_input"))
  LdText("${input.length} / 16000 characters · up to 60 lines, 250 characters per topic",12f,color=p.muted,maxLines=4)
  PrimaryButton("Preview topic lines",enabled=!busy&&source!=null&&input.isNotBlank(),modifier=Modifier.testTag("lines_preview")){focus.clearFocus();runCatching{ChecklistLines.preview(source!!,input)}.fold({preview=it;selected=it.added.map{it.line}.toSet();error="";status="Preview only. Saved checklist unchanged."},{preview=null;selected=emptySet();error=it.message?:"Check input"})}
  if(busy)LdText("Loading / saving...",12f,modifier=Modifier.testTag("lines_busy"))
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("lines_error"))
  if(status.isNotEmpty())LdText(status,13f,color=p.muted,maxLines=6,modifier=Modifier.testTag("lines_status"))
  preview?.let{view->
   LdText("${view.added.size} new · ${view.skipped} duplicate skips · ${selected.size} selected",15f,FontWeight.Bold,maxLines=5,modifier=Modifier.testTag("lines_count"))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Select new",enabled=!busy&&view.added.isNotEmpty(),modifier=Modifier.weight(1f).testTag("lines_all")){selected=view.added.map{it.line}.toSet()}
    PrimaryButton("Deselect",enabled=!busy&&selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("lines_none")){selected=emptySet()}
   }
   view.lines.forEach{line->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText("Line ${line.line} · "+when(line.kind){ChecklistLines.Kind.NEW->"New topic";ChecklistLines.Kind.EXISTING->"Already saved: skipped";ChecklistLines.Kind.REPEATED->"Repeated line: skipped"},12f,color=p.muted,maxLines=4)
    LdText(line.text,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("lines_text_${line.line}"))
    if(line.kind==ChecklistLines.Kind.NEW)GhostButton(if(line.line in selected)"Selected to add"else"Select to add",Modifier.fillMaxWidth().testTag("lines_pick_${line.line}")){if(!busy)selected=if(line.line in selected)selected-line.line else selected+line.line}
   }}
   val choice=runCatching{ChecklistLines.selected(view,selected)}
   if(choice.isFailure&&selected.isNotEmpty())LdText(choice.exceptionOrNull()?.message.orEmpty(),13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("lines_limit"))
   if(view.added.isEmpty())LdText("No new topics. Nothing to save.",13f,color=p.muted,maxLines=4,modifier=Modifier.testTag("lines_no_new"))
   PrimaryButton("Save selected new topics",enabled=!busy&&choice.isSuccess,modifier=Modifier.testTag("lines_save")){focus.clearFocus();confirm="save"}
  }
  GhostButton("Reload saved checklist (keep input)",Modifier.testTag("lines_reload")){focus.clearFocus();if(!busy)load()}
  if(inSheet)PrimaryButton("Checklist par wapas",enabled=!busy,modifier=Modifier.testTag("lines_back")){back()}
  LdText("Save appends selected new topics as Pending. Existing order, IDs and ticks stay. No Undo for saved additions; remove them from the normal checklist if needed. No template, contact, call, message or cloud action. Plain local data, not a vault. Unsaved input/preview may be lost on app/process close.",12f,color=p.muted,maxLines=12)
 }
 if(inSheet)Column{SheetTitle("Topic lines");content()}else PageFrame("Topic lines",{back()},content=content)
 if(confirm.isNotEmpty())Dialog(onDismissRequest={if(!busy)confirm=""}){GlassCard{
  val save=confirm=="save"
  LdText(if(save)"Append ${selected.size} selected topics as Pending?"else"Discard unsaved topic lines?",18f,FontWeight.Bold,maxLines=6)
  LdText(if(save)"Only this number. Existing topics stay. No saved-add Undo. Saved checklist must still match this preview."else"Saved checklist stays unchanged.",13f,color=p.muted,maxLines=7)
  PrimaryButton(if(save)"Confirm Save"else"Discard lines",enabled=!busy,modifier=Modifier.testTag("lines_confirm")){
   if(!save){confirm="";onBack()}else{
    val view=preview;confirm="";if(view!=null){val values=runCatching{ChecklistLines.selected(view,selected)};if(values.isFailure)error=values.exceptionOrNull()?.message.orEmpty()else{busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){CallChecklist.appendChecked(ctx,number,view.source,values.getOrThrow())};source=next;input="";invalidate();status="Selected topics saved as Pending";onSaved(next)}catch(e:Exception){preview=null;selected=emptySet();error=e.message?:"Save failed. Reload and preview again."}finally{busy=false}}}}
   }
  }
  GhostButton(if(save)"Keep preview"else"Keep input",Modifier.testTag("lines_cancel")){if(!busy)confirm=""}
 }}
}
