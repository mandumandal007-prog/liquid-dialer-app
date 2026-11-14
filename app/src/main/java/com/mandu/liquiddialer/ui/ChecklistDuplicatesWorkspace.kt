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

@Composable fun ChecklistDuplicatesWorkspace(number:String,onBack:()->Unit,onSaved:(List<CallChecklist.Item>)->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var source by remember(number){mutableStateOf<List<CallChecklist.Item>?>(null)};var selected by remember(number){mutableStateOf(emptySet<String>())}
 var preview by remember(number){mutableStateOf<ChecklistDuplicates.Preview?>(null)};var busy by remember(number){mutableStateOf(false)}
 var error by remember(number){mutableStateOf("")};var status by remember(number){mutableStateOf("")};var dialog by remember(number){mutableStateOf("")}
 val groups=remember(source){runCatching{source?.let{ChecklistDuplicates.groups(it)}.orEmpty()}}
 fun load(){if(busy)return;busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){CallChecklist.read(ctx,number).also{ChecklistDuplicates.groups(it)}};source=next;selected=emptySet();preview=null;dialog="";error="";status="Saved checklist loaded"}catch(_:Exception){source=null;selected=emptySet();preview=null;error="Checklist could not be loaded. Saved file was not changed."}finally{busy=false}}}
 fun back(){if(!busy){if(selected.isNotEmpty())dialog="back"else onBack()}}
 fun pick(next:Set<String>){selected=next;preview=null;status=""}
 LaunchedEffect(number){load()};BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4)
  LdText("Review repeats. Keep pending work.",22f,FontWeight.Bold,maxLines=5)
  LdText("Only case and whitespace are ignored when matching topic text. Punctuation, accents, digits and words must match. This does NOT detect similar meanings. Nothing is changed until preview and confirmation.",13f,color=p.muted,maxLines=10)
  LdText("For each selected group: keep the first topic's exact text and ID; keep the remaining topics in their original relative order. Remove its later copies. Keep Discussed only if ALL copies were Discussed; any Pending copy makes the kept topic Pending. Unselected topics stay untouched.",13f,color=p.muted,maxLines=11)
  if(busy)LdText("Loading / saving...",13f,color=p.muted,modifier=Modifier.testTag("dupes_busy"))
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("dupes_error"))
  if(source!=null&&groups.isSuccess){
   val rows=source!!;val list=groups.getOrThrow()
   LdText("${rows.size} topics · ${list.size} duplicate groups · ${selected.size} selected",16f,FontWeight.Bold,maxLines=5,modifier=Modifier.testTag("dupes_count"))
   if(list.isEmpty())LdText("No matching duplicate topics. Nothing to clean up.",15f,color=p.muted,maxLines=5,modifier=Modifier.testTag("dupes_empty"))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Select all groups",enabled=!busy&&error.isEmpty()&&list.isNotEmpty(),modifier=Modifier.weight(1f).testTag("dupes_all")){pick(list.map{it.key}.toSet())}
    PrimaryButton("Clear selection",enabled=!busy,modifier=Modifier.weight(1f).testTag("dupes_none")){pick(emptySet())}
   }
   list.forEachIndexed{i,g->GlassCard(Modifier.fillMaxWidth().padding(top=8.dp)){
    LdText("Group ${i+1} · ${g.items.size} copies",17f,FontWeight.Bold,maxLines=4)
    g.items.forEachIndexed{j,row->
     LdText("${g.positions[j]}. ${if(row.done)"Discussed"else"Pending"}${if(j==0)" · first topic kept"else" · later copy"}",12f,color=p.muted,maxLines=4)
     LdText(row.text,16f,FontWeight.SemiBold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("dupes_original_${i}_$j"))
    }
    LdText("Kept topic will be ${if(g.kept.done)"Discussed"else"Pending"}. ${g.items.size-1} later copies removed if selected.",13f,color=p.muted,maxLines=5,modifier=Modifier.testTag("dupes_outcome_$i"))
    PrimaryButton(if(g.key in selected)"✓ Selected group"else"Select group",enabled=!busy&&error.isEmpty(),modifier=Modifier.testTag("dupes_pick_$i")){pick(if(g.key in selected)selected-g.key else selected+g.key)}
   }}
   PrimaryButton("Preview selected cleanup",enabled=!busy&&error.isEmpty()&&selected.isNotEmpty(),modifier=Modifier.testTag("dupes_preview")){runCatching{ChecklistDuplicates.preview(rows,selected)}.fold({preview=it;status=""},{preview=null;error=it.message?:"Preview failed"})}
   preview?.let{view->
    LdText("Preview · ${view.removed} later copies removed",18f,FontWeight.Bold,maxLines=5,modifier=Modifier.testTag("dupes_result"))
    LdText("${view.source.size} → ${view.result.size} topics. Discussed ${view.source.count{it.done}} → ${view.result.count{it.done}}. Any Pending copy stays Pending in its group.",15f,FontWeight.Bold,maxLines=6,modifier=Modifier.testTag("dupes_totals"))
    view.groups.forEachIndexed{i,g->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
     LdText("Keep topic originally at position ${g.positions.first()}",13f,color=p.muted,maxLines=4)
     LdText(g.kept.text,17f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
     LdText("${if(g.kept.done)"Discussed"else"Pending"} · remove positions ${g.positions.drop(1).joinToString()}",14f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("dupes_kept_$i"))
    }}
    PrimaryButton("Save selected cleanup",enabled=!busy&&error.isEmpty(),modifier=Modifier.testTag("dupes_save")){dialog="save"}
   }
  }
  if(status.isNotEmpty())LdText(status,13f,color=p.muted,maxLines=5,modifier=Modifier.testTag("dupes_status"))
  GhostButton("Reload saved checklist",Modifier.testTag("dupes_reload")){if(!busy){if(selected.isNotEmpty())dialog="reload"else load()}}
  GhostButton("Return to checklist",Modifier.testTag("dupes_back")){back()}
  LdText("Save removes later copies from this number's local checklist. No saved Undo. Check every selected group first. Saving checks the exact original topic texts, ticks and order; stale changes refuse the whole cleanup. Other numbers, templates, notes, calls, contacts, clipboard and cloud stay untouched.",12f,color=p.muted,maxLines=12)
 }
 if(inSheet)Column{SheetTitle("Checklist duplicate review");content()}else PageFrame("Checklist duplicate review",{back()},content=content)
 if(dialog.isNotEmpty())Dialog(onDismissRequest={if(!busy)dialog=""}){GlassCard{
  val saving=dialog=="save"
  LdText(if(saving)"Remove ${preview?.removed?:0} later copies?"else"Discard this cleanup selection?",18f,FontWeight.Bold,maxLines=5)
  LdText(if(saving)"Only selected groups change. First topic kept; any Pending copy keeps it Pending. Saved immediately, no saved Undo. Review before confirming."else"Saved checklist will not change. Reload also clears the selection and preview.",13f,color=p.muted,maxLines=8)
  PrimaryButton(if(saving)"Confirm saved cleanup"else"Discard preview",enabled=!busy,modifier=Modifier.testTag("dupes_confirm")){
   when(dialog){
    "save"->{val view=preview!!;busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){CallChecklist.consolidateChecked(ctx,number,view)};source=next;selected=emptySet();preview=null;dialog="";error="";status="Saved cleanup · ${view.removed} copies removed";onSaved(next)}catch(e:Exception){dialog="";preview=null;error=e.message?:"Cleanup failed. Reload and preview again."}finally{busy=false}}}
    "reload"->{dialog="";load()}
    else->{dialog="";onBack()}
   }
  }
  GhostButton(if(saving)"Cancel"else"Keep preview",Modifier.testTag("dupes_cancel")){if(!busy)dialog=""}
 }}
}
@Composable fun ChecklistDuplicatesPage(onBack:()->Unit){
 val settings by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();val p=LocalLd.current;var error by remember{mutableStateOf("")}
 PageFrame("Checklist duplicate review",onBack){
  LdText("Consolidate only the repeats you select.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Checklist duplicate review","Default OFF. Review matching topics before confirmed local cleanup.",settings.checklistDuplicates,last=true){scope.launch{try{LdApp.instance.repo.set(K.checklistDuplicates,it);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Open a number's Call checklist, then Review duplicate topics. Matching ignores case and whitespace only, not meaning. Select groups, review the exact originals and kept/removed preview, then confirm Save. First ID/text kept, remaining relative order kept, Discussed only if every copy was Discussed. Any Pending copy keeps the topic Pending. Unselected topics unchanged. No saved Undo; no automatic cleanup, contacts, call, message, clipboard, templates or cloud action. Local checklist is not encrypted and is not included in app backup.",14f,color=p.muted,maxLines=24)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}
