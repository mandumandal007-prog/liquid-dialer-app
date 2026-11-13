package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun ChecklistOrganizerPage(onBack:()->Unit){
 val p=LocalLd.current;val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();var error by remember{mutableStateOf("")}
 PageFrame("Checklist organizer",onBack){
  LdText("Topics ko call se pehle taiyar rakho.",23f,FontWeight.Bold,maxLines=4)
  SwitchRow("Show organizer inside call checklist","Default OFF. Hiding tools never deletes topics.",s.checklistOrganizer,last=true){scope.launch{try{LdApp.instance.repo.set(K.checklistOrganizer,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  LdText("Checklist kholo, phir Organize. Search / All / Pending / Discussed, visible selection se batch marks. Full unfiltered list mein Upar/Neeche. Badlav pehle local preview hain: Undo ya explicit Save. Save se pehle original snapshot check hota hai.",14f,color=p.muted,maxLines=12)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
  LdText("Koi topic add/delete/text edit, call, message ya cloud nahi. Existing checklist local file hai, encrypted vault nahi. Unsaved preview screen/app close par kho sakta hai.",13f,color=p.muted,maxLines=9)
 }
}

@Composable fun ChecklistOrganizerWorkspace(number:String,onBack:()->Unit,onSaved:(List<CallChecklist.Item>)->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var original by remember(number){mutableStateOf<List<CallChecklist.Item>?>(null)};var draft by remember(number){mutableStateOf(ChecklistOrganizer.Draft(emptyList()))};var revision by remember{mutableIntStateOf(0)}
 var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
 var query by remember{mutableStateOf("")};var filter by remember{mutableStateOf(ChecklistOrganizer.Filter.ALL)};var selected by remember{mutableStateOf<Set<String>>(emptySet())}
 val rows=remember(draft,revision){draft.rows};val dirty=original!=null&&rows!=original
 val shown=remember(rows,query,filter){ChecklistOrganizer.visible(rows,query,filter)}
 fun load(){if(busy)return;busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){CallChecklist.read(ctx,number)};original=next;draft=ChecklistOrganizer.Draft(next);revision++;selected=emptySet();error="";status="Saved checklist loaded"}catch(_:Exception){original=null;error="Checklist load nahi hui. Saved file nahi badli."}finally{busy=false}}}
 fun change(next:List<CallChecklist.Item>){if(!busy&&error.isEmpty()&&draft.change(next)){revision++;status="Unsaved preview · Undo available"}}
 fun back(){if(!busy){if(dirty)confirm="back"else onBack()}}
 LaunchedEffect(number){load()};BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("organizer_number"))
  LdText("One explicit Save. Batch marks sirf tumhare discussed ticks hain; call/awaaz se automatic nahi badalte.",12f,color=p.muted,maxLines=5)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("organizer_error"))
  if(busy)LdText("Loading / saving...",12f,modifier=Modifier.testTag("organizer_busy"))
  if(original!=null){
   LdText(if(dirty)"Unsaved preview"else"Saved checklist",14f,FontWeight.Bold,modifier=Modifier.testTag("organizer_state"))
   LdField(query,{query=it.take(200);selected=emptySet()},"Topic text mein khojo",modifier=Modifier.testTag("organizer_query"))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){ChecklistOrganizer.Filter.entries.forEach{f->GhostButton((if(filter==f)"✓ "else"")+f.label,Modifier.weight(1f).testTag("organizer_filter_${f.name.lowercase()}")){filter=f;selected=emptySet()}}}
   LdText("${shown.size} visible / ${rows.size} topics · ${selected.size} selected · ${rows.count{it.done}} discussed",12f,color=p.muted,maxLines=4,modifier=Modifier.testTag("organizer_count"))
   LdText("Search/filter badalne par selection clear. Reorder sirf All + blank search mein; position poori list ki hai.",12f,color=p.muted,maxLines=5)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Select visible",enabled=!busy&&error.isEmpty()&&shown.isNotEmpty(),modifier=Modifier.weight(1f).testTag("organizer_select_all")){selected=shown.map{it.id}.toSet()}
    PrimaryButton("Clear selection",enabled=selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("organizer_clear_selection")){selected=emptySet()}
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Discussed",enabled=!busy&&error.isEmpty()&&selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("organizer_done")){change(ChecklistOrganizer.mark(rows,selected,true));selected=emptySet()}
    PrimaryButton("Pending",enabled=!busy&&error.isEmpty()&&selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("organizer_pending")){change(ChecklistOrganizer.mark(rows,selected,false));selected=emptySet()}
   }
   PrimaryButton("Undo preview edit",enabled=!busy&&error.isEmpty()&&draft.canUndo,modifier=Modifier.testTag("organizer_undo")){draft.undo();selected=emptySet();revision++;status="Preview edit undone"}
   PrimaryButton("Save checklist changes",enabled=dirty&&!busy&&error.isEmpty(),modifier=Modifier.testTag("organizer_save")){
    val expected=original!!;val next=rows.toList();busy=true;scope.launch{try{val saved=withContext(Dispatchers.IO){CallChecklist.organizeChecked(ctx,number,expected,next)};original=saved;draft=ChecklistOrganizer.Draft(saved);revision++;selected=emptySet();status="Checklist saved";onSaved(saved)}catch(e:Exception){error=e.message?:"Save nahi hui. Reload karo; preview abhi yahin hai."}finally{busy=false}}
   }
   if(shown.isEmpty())LdText(if(rows.isEmpty())"Abhi koi topic nahi. Checklist se topic jodo."else"Is filter/search mein koi topic nahi",13f,color=p.muted,maxLines=5,modifier=Modifier.testTag("organizer_empty"))
   shown.forEach{item->val position=rows.indexOfFirst{it.id==item.id};GlassCard(Modifier.fillMaxWidth().padding(top=8.dp).testTag("organizer_item_${item.id}")){
    LdText("${position+1}. ${if(item.done)"Discussed"else"Pending"}",12f,color=p.muted)
    LdText(item.text,16f,FontWeight.SemiBold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("organizer_text_${item.id}"))
    GhostButton(if(item.id in selected)"✓ Selected"else"Select topic",Modifier.testTag("organizer_select_${item.id}")){if(!busy&&error.isEmpty())selected=if(item.id in selected)selected-item.id else selected+item.id}
    if(filter==ChecklistOrganizer.Filter.ALL&&query.isBlank())Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
     PrimaryButton("Upar",enabled=position>0&&!busy&&error.isEmpty(),modifier=Modifier.weight(1f).testTag("organizer_up_${item.id}")){change(ChecklistOrganizer.move(rows,item.id,-1))}
     PrimaryButton("Neeche",enabled=position<rows.lastIndex&&!busy&&error.isEmpty(),modifier=Modifier.weight(1f).testTag("organizer_down_${item.id}")){change(ChecklistOrganizer.move(rows,item.id,1))}
    }
   }}
  }
  if(status.isNotEmpty())LdText(status,12f,color=p.muted,maxLines=5,modifier=Modifier.testTag("organizer_status"))
  GhostButton("Reload saved checklist",Modifier.testTag("organizer_reload")){if(!busy){if(dirty)confirm="reload"else load()}}
 }
 if(inSheet)Column{SheetTitle("Checklist organizer");PrimaryButton("Checklist par wapas",enabled=!busy,modifier=Modifier.testTag("organizer_back")){back()};content()}else PageFrame("Checklist organizer",{back()},content=content)
 if(confirm.isNotEmpty())Dialog(onDismissRequest={confirm=""}){GlassCard{
  LdText("Unsaved preview chhod do? Saved checklist nahi badlegi.",16f,FontWeight.Bold,maxLines=5)
  PrimaryButton("Discard preview",danger=true,modifier=Modifier.testTag("organizer_discard")){val action=confirm;confirm="";if(action=="back")onBack()else load()}
  GhostButton("Keep preview",Modifier.testTag("organizer_keep")){confirm=""}
 }}
}
