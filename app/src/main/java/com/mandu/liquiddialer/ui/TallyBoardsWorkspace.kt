package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun TallyBoardsWorkspace(rows:List<CallTally.Item>,onBack:()->Unit,onApply:(TallyBoards.Plan)->Unit,inSheet:Boolean=false){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();val latestRows by rememberUpdatedState(rows);val latestApply by rememberUpdatedState(onApply)
 val parsed=remember(s.tallyBoardLibrary){runCatching{TallyBoards.decode(s.tallyBoardLibrary)}};val all=parsed.getOrDefault(emptyList());val valid=runCatching{CallTally.validate(rows)}
 var editor by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")};var editorRaw by remember{mutableStateOf("")};var captured by remember{mutableStateOf(emptyList<CallTally.Item>())}
 var deleting by remember{mutableStateOf<TallyBoards.Saved?>(null)};var deleteRaw by remember{mutableStateOf("")};var preview by remember(rows){mutableStateOf<TallyBoards.Plan?>(null)}
 var confirm by remember(rows){mutableStateOf(false)};var discard by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var notice by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
 fun back(){if(!busy){if(editor!=null)discard=true else onBack()}};BackHandler{back()}
 LaunchedEffect(s.tallyBoards){if(!s.tallyBoards){preview=null;confirm=false;deleting=null;editor=null}}
 fun library(raw:String,checkRows:Boolean=false,change:(List<TallyBoards.Saved>)->List<TallyBoards.Saved>){if(busy)return;busy=true;scope.launch{try{ctx.ldStore.edit{prefs->require(prefs[K.tallyBoards]==true){"Saved tally boards are OFF"};require((prefs[K.tallyBoardLibrary]?:"")==raw){"Saved library changed. Open edit again"};if(checkRows)require(latestRows==captured){"Session changed while naming. Start Save again"};prefs[K.tallyBoardLibrary]=TallyBoards.encode(change(TallyBoards.decode(raw)))};editor=null;deleting=null;preview=null;confirm=false;name="";error="";notice="Saved library changed. Session counters and step unchanged"}catch(e:Exception){error=e.message?:"Library edit failed"}finally{busy=false}}}
 val content:@Composable ColumnScope.()->Unit={
  LdText("Keep a board. Resume after checking.",22f,FontWeight.Bold,maxLines=5)
  LdText("Unlike label-only templates, these explicit snapshots save counter labels AND quantities. Shared across tally sessions on this phone, no caller link. Not stock, an order or an automatic save. Counts may be old when reopened. Check each quantity and unit before loading.",13f,color=p.muted,maxLines=11)
  if(!s.tallyBoards)LdText("Saved tally boards OFF. Library kept; current session unchanged. Enable in Settings > Saved tally boards.",14f,maxLines=6,modifier=Modifier.testTag("boards_off"))else{
   if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=10,modifier=Modifier.testTag("boards_error"))
   if(notice.isNotEmpty())LdText(notice,13f,color=p.accent,maxLines=6,modifier=Modifier.testTag("boards_notice"))
   if(parsed.isFailure)LdText("Saved library unreadable. Save, rename, delete and load are locked; existing data will not be overwritten.",14f,color=p.danger,maxLines=8,modifier=Modifier.testTag("boards_corrupt"))
   if(valid.isFailure)LdText("Session counters are invalid; Save and Load disabled.",14f,color=p.danger,maxLines=5)
   if(parsed.isSuccess){
    SecTitle("Current session")
    LdText("${rows.size}/12 counters · ${all.size}/6 saved boards",14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("boards_capacity"))
    if(valid.isSuccess)LdText("Raw sum ${CallTally.total(rows)} · meaningful only for matching units",13f,color=p.muted,maxLines=5)
    if(editor==null)BoardAction("Save current quantities as named board",enabled=!busy&&valid.isSuccess&&rows.isNotEmpty()&&all.size<6,modifier=Modifier.testTag("boards_capture")){editorRaw=s.tallyBoardLibrary;captured=rows.toList();editor="";name=""}
    editor?.let{id->GlassCard{
     LdField(name,{name=it.take(40)},"Saved board name (1-32 characters)",modifier=Modifier.testTag("boards_name"))
     LdText(if(id.isEmpty())"Saves ${captured.size} counter labels and quantities only. Session +/-step and unfinished editor are not saved."else"Rename changes only the saved name, not quantities or the session.",12f,color=p.muted,maxLines=6)
     BoardAction(if(id.isEmpty())"Save snapshot library"else"Rename saved board",enabled=!busy,modifier=Modifier.testTag("boards_save")){if(id.isEmpty())library(editorRaw,true){TallyBoards.capture(it,name,captured)}else library(editorRaw){TallyBoards.rename(it,id,name)}}
     BoardAction("Cancel edit",modifier=Modifier.testTag("boards_edit_cancel")){editor=null;name=""}
    }}
    SecTitle("Saved quantity boards")
    if(all.isEmpty())LdText("No saved boards yet",14f,color=p.muted,modifier=Modifier.testTag("boards_empty"))
    all.forEach{b->GlassCard(Modifier.padding(top=8.dp)){
     LdText(b.name,18f,FontWeight.Bold,maxLines=5)
     LdText("${b.rows.size} counters · raw sum ${CallTally.total(b.rows)}",13f,color=p.muted,maxLines=5)
     BoardAction("Compare before loading",enabled=!busy&&valid.isSuccess,modifier=Modifier.testTag("boards_preview_${b.id}")){runCatching{TallyBoards.plan(rows,s.tallyBoardLibrary,b.id)}.fold({preview=it;confirm=false;error=""},{preview=null;error=it.message?:"Preview refused"})}
     BoardAction("Rename",modifier=Modifier.testTag("boards_rename_${b.id}")){editorRaw=s.tallyBoardLibrary;editor=b.id;name=b.name}
     BoardAction("Delete",modifier=Modifier.testTag("boards_delete_${b.id}")){deleteRaw=s.tallyBoardLibrary;deleting=b}
    }}
   }
   preview?.let{v->
    SecTitle("Replacement preview")
    LdText("Replaces ALL current session counters, not an append or merge. Unfinished counter editor clears on Load. Shared +/-step stays. Saved library is not changed by Load. One board Undo restores previous counters (last 40 edits).",13f,color=p.muted,maxLines=10)
    LdText("Raw sum ${CallTally.total(v.before)} → ${CallTally.total(v.after)}. Matching units are your responsibility; this sum does not validate them.",14f,FontWeight.Bold,maxLines=7,modifier=Modifier.testTag("boards_total"))
    LdText("Current session · will be replaced",17f,FontWeight.Bold,maxLines=5)
    if(v.before.isEmpty())LdText("No current counters",13f,color=p.muted)
    v.before.forEachIndexed{i,r->GlassCard(Modifier.padding(top=6.dp)){LdText(r.label,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE);LdText(r.value.toString(),21f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("boards_before_$i"))}}
    LdText("Saved snapshot · will become session",17f,FontWeight.Bold,maxLines=5)
    v.after.forEachIndexed{i,r->GlassCard(Modifier.padding(top=6.dp)){LdText(r.label,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE);LdText(r.value.toString(),21f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("boards_after_$i"))}}
    BoardAction("Replace session with this snapshot",enabled=!busy,modifier=Modifier.testTag("boards_apply")){confirm=true}
    BoardAction("Close comparison",modifier=Modifier.testTag("boards_preview_close")){preview=null;confirm=false}
   }
  }
  BoardAction("Return to quantity tally",modifier=Modifier.testTag("boards_back")){back()}
  LdText("Library is local, unencrypted and not in backup. Use names without private customer details. Uninstall/data clear can erase it. Saving later session changes requires a NEW named snapshot; existing snapshots are never silently updated. Deleting a saved board does not change a loaded session. No automatic save, stock, order, payment, call, message, clipboard or cloud action.",12f,color=p.muted,maxLines=13)
 }
 if(inSheet)Column{SheetTitle("Saved tally boards");content()}else PageFrame("Saved tally boards",{back()},content=content)
 if(confirm&&preview!=null)Dialog(onDismissRequest={if(!busy)confirm=false}){GlassCard{
  LdText("Replace current session counters?",18f,FontWeight.Bold,maxLines=6)
  LdText("Load the shown saved quantities, replacing every current counter and clearing unfinished counter input. Step stays. One board Undo restores previous counters. No saved library or external data changes.",13f,color=p.muted,maxLines=10)
  BoardAction("Confirm session replacement",enabled=!busy,modifier=Modifier.testTag("boards_confirm")){val v=preview!!;if(!busy){busy=true;scope.launch{try{ctx.ldStore.edit{prefs->require(prefs[K.tallyBoards]==true){"Saved tally boards are OFF"};TallyBoards.apply(v,latestRows,prefs[K.tallyBoardLibrary]?:"")};confirm=false;latestApply(v)}catch(e:Exception){confirm=false;preview=null;error=e.message?:"Preview again"}finally{busy=false}}}}
  BoardAction("Keep current session",enabled=!busy,modifier=Modifier.testTag("boards_cancel")){confirm=false}
 }}
 deleting?.let{b->Dialog(onDismissRequest={if(!busy)deleting=null}){GlassCard{
  LdText("Delete saved board?",18f,FontWeight.Bold,maxLines=5);LdText(b.name+" and its saved quantities will be removed only from the library. Session counters stay. Cannot undo this library deletion.",13f,color=p.muted,maxLines=8)
  BoardAction("Delete saved board",enabled=!busy,danger=true,modifier=Modifier.testTag("boards_delete_confirm")){library(deleteRaw){TallyBoards.remove(it,b.id)}}
  BoardAction("Keep saved board",enabled=!busy,modifier=Modifier.testTag("boards_delete_cancel")){deleting=null}
 }}}
 if(discard)Dialog(onDismissRequest={discard=false}){GlassCard{
  LdText("Discard saved-board name edit?",18f,FontWeight.Bold,maxLines=5)
  BoardAction("Discard edit",modifier=Modifier.testTag("boards_discard")){discard=false;onBack()}
  BoardAction("Keep editing",modifier=Modifier.testTag("boards_keep")){discard=false}
 }}
}
@Composable fun TallyBoardsPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf("")}
 PageFrame("Saved tally boards",onBack){
  LdText("Save quantities only when you choose.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Saved tally boards","Default OFF. Separate from label-only templates: explicit named snapshots include quantities.",s.tallyBoards,last=true,subMaxLines=7){v->scope.launch{try{LdApp.instance.repo.set(K.tallyBoards,v);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Open Call quantity tally > Saved tally boards. Up to six named snapshots, each with 1-12 counter labels and whole quantities. Save is explicit. Compare ALL current and saved counters, then confirm replacement of the session. Current +/-step stays; unfinished counter input clears. One board Undo restores previous counters. Loading never changes the saved snapshot. Rename/delete affect only the library, not a loaded session.",14f,color=p.muted,maxLines=19)
  LdText("OFF keeps the library and does not change the current session. Back/process close still discards unsaved session changes. Library is shared across tally sessions on this phone, unencrypted, not in backup and has no caller link. Names may contain private text: choose carefully. Quantities may be old; verify units and counts on reuse. No automatic save, stock, order, payment, call, message or cloud action.",13f,color=p.muted,maxLines=17)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5)
 }
}
@Composable private fun BoardAction(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,danger:Boolean=false,onClick:()->Unit){val p=LocalLd.current;Box(modifier.fillMaxWidth().alpha(if(enabled)1f else .5f).buttonGlass(p,RoundedCornerShape(16.dp)).clickable(enabled=enabled,onClick=onClick).padding(horizontal=12.dp,vertical=14.dp),contentAlignment=androidx.compose.ui.Alignment.Center){LdText(text,14.5f,FontWeight.SemiBold,color=if(danger)p.danger else p.text,maxLines=Int.MAX_VALUE)}}
