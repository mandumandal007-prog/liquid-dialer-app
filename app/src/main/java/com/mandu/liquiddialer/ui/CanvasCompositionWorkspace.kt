package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun CanvasCompositionWorkspace(number:String,onBack:()->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var original by remember(number){mutableStateOf<CallCanvas.Snapshot?>(null)};var draft by remember(number){mutableStateOf(CanvasComposition.Draft(emptyList()))};var revision by remember{mutableIntStateOf(0)}
 var selected by remember{mutableStateOf<Set<Int>>(emptySet())};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
 val rows=remember(draft,revision){draft.rows};val dirty=original!=null&&rows!=original!!.strokes
 fun load(){if(busy)return;busy=true;scope.launch{try{val s=withContext(Dispatchers.IO){CallCanvas.read(ctx,number)};original=s;draft=CanvasComposition.Draft(s.strokes);revision++;selected=emptySet();error="";status="Saved ink loaded"}catch(_:Exception){original=null;error="Canvas load failed. Saved file unchanged; edits and Save disabled."}finally{busy=false}}}
 fun back(){if(!busy){if(dirty)confirm="back"else onBack()}}
 LaunchedEffect(number){load()};BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("composition_number"))
  LdText("Compose saved strokes",17f,FontWeight.Bold,maxLines=3)
  LdText("Change selected ink color or width, bring strokes to front/back, or add offset duplicates. Later stroke numbers draw on top. Each group keeps its internal order. No permanent groups.",12f,color=p.muted,maxLines=10)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("composition_error"))
  if(busy)LdText("Loading / saving...",12f,modifier=Modifier.testTag("composition_busy"))
  if(original!=null){
   LdText(if(dirty)"Unsaved composition preview"else"Saved canvas",14f,FontWeight.Bold,modifier=Modifier.testTag("composition_state"))
   LdText("${selected.size} selected / ${rows.size} strokes",13f,color=p.muted,modifier=Modifier.testTag("composition_count"))
   CanvasLayoutPreview(rows,selected,"composition_paper")
   LdText("Blue boxes mark selected stroke bounds. Styles apply to saved ink, not the drawing pen. Preview only until Save.",12f,color=p.muted,maxLines=5)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Select all",enabled=!busy&&error.isEmpty()&&rows.isNotEmpty(),modifier=Modifier.weight(1f).testTag("composition_all")){selected=rows.indices.toSet()}
    PrimaryButton("Deselect",enabled=!busy&&selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("composition_none")){selected=emptySet()}
   }
   fun edit(clear:Boolean=false,action:()->List<CallCanvas.Stroke>){runCatching(action).fold({next->status=if(draft.change(next)){revision++;if(clear)selected=emptySet();"Unsaved edit · Undo available"}else"No change"},{status=it.message?:"Edit refused. Canvas unchanged"})}
   LdText("Selected ink color",15f,FontWeight.Bold)
   listOf("Ink","Blue","Red","Green").chunked(2).forEachIndexed{r,labels->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){labels.forEachIndexed{c,label->val ink=r*2+c;PrimaryButton(label,enabled=!busy&&error.isEmpty()&&selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("composition_ink_$ink")){edit{CanvasComposition.style(rows,selected,ink=ink)}}}}}
   LdText("Selected pen width",15f,FontWeight.Bold)
   listOf("Fine","Pen","Bold").forEachIndexed{i,label->PrimaryButton(label,enabled=!busy&&error.isEmpty()&&selected.isNotEmpty(),modifier=Modifier.testTag("composition_width_${i+1}")){edit{CanvasComposition.style(rows,selected,width=i+1)}}}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("To front",enabled=!busy&&error.isEmpty()&&selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("composition_front")){edit(true){CanvasComposition.layer(rows,selected,CanvasComposition.Layer.FRONT)}}
    PrimaryButton("To back",enabled=!busy&&error.isEmpty()&&selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("composition_back_layer")){edit(true){CanvasComposition.layer(rows,selected,CanvasComposition.Layer.BACK)}}
   }
   LdText("Duplicate selected strokes",15f,FontWeight.Bold)
   LdText("Adds copies on top, offset 4% of page width or height. Originals stay. Paper-edge or capacity failure refuses the whole action, no clamping. Selection clears after draw-order, duplicate or history edits.",12f,color=p.muted,maxLines=8)
   listOf(Triple("Left 4%",-400,0),Triple("Right 4%",400,0),Triple("Up 4%",0,-400),Triple("Down 4%",0,400)).chunked(2).forEachIndexed{r,items->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){items.forEachIndexed{c,(label,dx,dy)->PrimaryButton(label,enabled=!busy&&error.isEmpty()&&selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("composition_duplicate_${r*2+c}")){edit(true){CanvasComposition.duplicate(rows,selected,dx,dy)}}}}}
   if(status.isNotEmpty())LdText(status,13f,color=p.muted,maxLines=6,modifier=Modifier.testTag("composition_status"))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Undo",enabled=!busy&&draft.canUndo,modifier=Modifier.weight(1f).testTag("composition_undo")){draft.undo();revision++;selected=emptySet();status="Composition edit undone"}
    PrimaryButton("Redo",enabled=!busy&&draft.canRedo,modifier=Modifier.weight(1f).testTag("composition_redo")){draft.redo();revision++;selected=emptySet();status="Composition edit redone"}
   }
   PrimaryButton("Save composition to this canvas",enabled=dirty&&!busy&&error.isEmpty(),modifier=Modifier.testTag("composition_save")){
    val expected=original!!.raw;val next=rows.toList();busy=true;scope.launch{try{original=withContext(Dispatchers.IO){CallCanvas.save(ctx,number,expected,next)};draft=CanvasComposition.Draft(next);revision++;error="";status="Composition saved locally"}catch(e:Exception){error=e.message?:"Save failed. Preview remains; saved file not replaced."}finally{busy=false}}
   }
   if(rows.isEmpty())LdText("No saved ink. Draw and Save in the canvas first.",14f,color=p.muted,maxLines=4,modifier=Modifier.testTag("composition_empty"))
   rows.forEachIndexed{i,s->val b=CanvasLayout.bounds(s);GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText("Stroke ${i+1} · ${s.points.size} points · ${listOf("Ink","Blue","Red","Green")[s.ink]} · ${listOf("Fine","Pen","Bold")[s.width-1]}",15f,FontWeight.Bold,maxLines=4)
    LdText("Bounds x${b.left}-${b.right} · y${b.top}-${b.bottom}",12f,color=p.muted,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("composition_bounds_$i"))
    GhostButton((if(i in selected)"Selected"else"Select")+" stroke ${i+1}",Modifier.fillMaxWidth().testTag("composition_pick_$i")){if(!busy)selected=if(i in selected)selected-i else selected+i}
   }}
  }
  GhostButton("Reload saved ink",Modifier.testTag("composition_reload")){if(!busy){if(dirty)confirm="reload"else load()}}
  LdText("Only this number's local canvas changes on explicit Save. No notes, call, share, clipboard or cloud actions. Undo/Redo: last 40 composition edits. Back discards unsaved preview after confirmation.",12f,color=p.muted,maxLines=7)
 }
 if(inSheet)Column{SheetTitle("Canvas composition");PrimaryButton("Canvas par wapas",enabled=!busy,modifier=Modifier.testTag("composition_back")){back()};content()}else PageFrame("Canvas composition",{back()},content=content)
 if(confirm.isNotEmpty())Dialog(onDismissRequest={confirm=""}){GlassCard{
  LdText("Discard unsaved composition preview? Saved canvas stays unchanged.",17f,FontWeight.Bold,maxLines=6)
  PrimaryButton("Discard preview",danger=true,modifier=Modifier.testTag("composition_discard")){val a=confirm;confirm="";if(a=="back")onBack()else load()}
  GhostButton("Keep preview",Modifier.testTag("composition_keep")){confirm=""}
 }}
}
