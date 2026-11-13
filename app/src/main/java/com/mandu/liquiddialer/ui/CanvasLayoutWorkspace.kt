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

@Composable fun CanvasLayoutWorkspace(number:String,onBack:()->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var original by remember(number){mutableStateOf<CallCanvas.Snapshot?>(null)};var draft by remember(number){mutableStateOf(CanvasLayout.Draft(emptyList()))};var revision by remember{mutableIntStateOf(0)}
 var selected by remember{mutableStateOf<Set<Int>>(emptySet())};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
 val rows=remember(draft,revision){draft.rows};val dirty=original!=null&&rows!=original!!.strokes
 fun load(){if(busy)return;busy=true;scope.launch{try{val s=withContext(Dispatchers.IO){CallCanvas.read(ctx,number)};original=s;draft=CanvasLayout.Draft(s.strokes);revision++;selected=emptySet();error="";status="Saved ink loaded"}catch(_:Exception){original=null;error="Canvas load failed. Saved file unchanged; edits and Save disabled."}finally{busy=false}}}
 fun back(){if(!busy){if(dirty)confirm="back"else onBack()}}
 LaunchedEffect(number){load()};BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("layout_number"))
  LdText("Arrange saved ink · translation only",17f,FontWeight.Bold,maxLines=3)
  LdText("Selected strokes keep their shape, order, color and pen width. Alignment uses selection bounds; spacing uses centers, NOT equal gaps. Half-coordinate targets round to nearest integer; up to 1/10000 page tolerance. No permanent groups.",12f,color=p.muted,maxLines=10)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("layout_error"))
  if(busy)LdText("Loading / saving...",12f,modifier=Modifier.testTag("layout_busy"))
  if(original!=null){
   LdText(if(dirty)"Unsaved layout preview"else"Saved canvas",14f,FontWeight.Bold,modifier=Modifier.testTag("layout_state"))
   LdText("${selected.size} selected / ${rows.size} strokes",13f,color=p.muted,modifier=Modifier.testTag("layout_count"))
   CanvasLayoutPreview(rows,selected)
   LdText("Blue boxes mark selected stroke bounds. Ink can overlap after alignment. Preview only until Save.",12f,color=p.muted,maxLines=5)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Select all",enabled=!busy&&error.isEmpty()&&rows.isNotEmpty(),modifier=Modifier.weight(1f).testTag("layout_all")){selected=rows.indices.toSet()}
    PrimaryButton("Deselect",enabled=!busy&&selected.isNotEmpty(),modifier=Modifier.weight(1f).testTag("layout_none")){selected=emptySet()}
   }
   CanvasLayout.Edit.entries.chunked(2).forEach{edits->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){edits.forEach{edit->val required=if(edit.name.startsWith("NUDGE_"))1 else if(edit==CanvasLayout.Edit.SPACE_X||edit==CanvasLayout.Edit.SPACE_Y)3 else 2
    PrimaryButton(edit.label,enabled=!busy&&error.isEmpty()&&selected.size>=required,modifier=Modifier.weight(1f).testTag("layout_${edit.name.lowercase()}")){
     runCatching{CanvasLayout.apply(rows,selected,edit)}.fold({status=if(draft.change(it)){revision++;"${edit.label} · Unsaved · Undo available"}else"No position change"},{status=it.message?:"Edit refused. No ink moved"})
    }
   }}}
   LdText("Nudge = 2% of page width/height. Any paper-edge crossing refuses the whole edit; no clamping. Align needs 2+; spacing needs 3+ selected strokes.",12f,color=p.muted,maxLines=6)
   if(status.isNotEmpty())LdText(status,13f,color=p.muted,maxLines=6,modifier=Modifier.testTag("layout_status"))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Undo",enabled=!busy&&draft.canUndo,modifier=Modifier.weight(1f).testTag("layout_undo")){draft.undo();revision++;status="Layout edit undone"}
    PrimaryButton("Redo",enabled=!busy&&draft.canRedo,modifier=Modifier.weight(1f).testTag("layout_redo")){draft.redo();revision++;status="Layout edit redone"}
   }
   PrimaryButton("Save layout to this canvas",enabled=dirty&&!busy&&error.isEmpty(),modifier=Modifier.testTag("layout_save")){
    val expected=original!!.raw;val next=rows.toList();busy=true;scope.launch{try{original=withContext(Dispatchers.IO){CallCanvas.save(ctx,number,expected,next)};draft=CanvasLayout.Draft(next);revision++;error="";status="Layout saved locally"}catch(e:Exception){error=e.message?:"Save failed. Preview remains; saved file not replaced."}finally{busy=false}}
   }
   if(rows.isEmpty())LdText("No saved ink. Draw and Save in the canvas first.",14f,color=p.muted,maxLines=4,modifier=Modifier.testTag("layout_empty"))
   rows.forEachIndexed{i,s->val b=CanvasLayout.bounds(s);GlassCard(Modifier.fillMaxWidth().padding(top=6.dp)){
    LdText("Stroke ${i+1} · ${s.points.size} points",15f,FontWeight.Bold,maxLines=4)
    LdText("Bounds x${b.left}-${b.right} · y${b.top}-${b.bottom}",12f,color=p.muted,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("layout_bounds_$i"))
    GhostButton((if(i in selected)"Selected"else"Select")+" stroke ${i+1}",Modifier.fillMaxWidth().testTag("layout_pick_$i")){if(!busy)selected=if(i in selected)selected-i else selected+i}
   }}
  }
  GhostButton("Reload saved ink",Modifier.testTag("layout_reload")){if(!busy){if(dirty)confirm="reload"else load()}}
  LdText("Only this number's local canvas changes on explicit Save. No notes, call, share, clipboard or cloud actions. Undo/Redo: last40layout edits. Back discards unsaved preview after confirmation.",12f,color=p.muted,maxLines=7)
 }
 if(inSheet)Column{SheetTitle("Canvas layout");PrimaryButton("Canvas par wapas",enabled=!busy,modifier=Modifier.testTag("layout_back")){back()};content()}else PageFrame("Canvas layout",{back()},content=content)
 if(confirm.isNotEmpty())Dialog(onDismissRequest={confirm=""}){GlassCard{
  LdText("Discard unsaved layout preview? Saved canvas stays unchanged.",17f,FontWeight.Bold,maxLines=6)
  PrimaryButton("Discard preview",danger=true,modifier=Modifier.testTag("layout_discard")){val a=confirm;confirm="";if(a=="back")onBack()else load()}
  GhostButton("Keep preview",Modifier.testTag("layout_keep")){confirm=""}
 }}
}
@Composable internal fun CanvasLayoutPreview(rows:List<CallCanvas.Stroke>,selected:Set<Int>,tag:String="layout_paper"){
 Canvas(Modifier.fillMaxWidth().aspectRatio(1.1f).padding(vertical=8.dp).background(Color(0xfffffbef)).testTag(tag).semantics{contentDescription="Read-only canvas preview: ${rows.size} strokes; selected ${selected.sorted().joinToString{(it+1).toString()}}. Use stroke buttons below for selection."}){
  val colors=listOf(Color(0xff263449),Color(0xff245be0),Color(0xffd83d51),Color(0xff168054))
  fun xy(p:CallCanvas.Point)=Offset(p.x/10000f*size.width,p.y/10000f*size.height)
  rows.forEach{s->val thick=size.width*(when(s.width){1->0.005f;2->0.009f;else->0.016f});if(s.points.size==1)drawCircle(colors[s.ink],thick/2,xy(s.points.first()))else{val path=Path().apply{moveTo(xy(s.points.first()).x,xy(s.points.first()).y);s.points.drop(1).forEach{lineTo(xy(it).x,xy(it).y)}};drawPath(path,colors[s.ink],style=Stroke(thick,cap=StrokeCap.Round,join=androidx.compose.ui.graphics.StrokeJoin.Round))}}
  selected.forEach{i->rows.getOrNull(i)?.let{s->val b=CanvasLayout.bounds(s);val a=xy(CallCanvas.Point(b.left,b.top));val z=xy(CallCanvas.Point(b.right,b.bottom));drawRect(Color(0xff245be0),a,androidx.compose.ui.geometry.Size((z.x-a.x).coerceAtLeast(2f),(z.y-a.y).coerceAtLeast(2f)),style=Stroke(2.dp.toPx(),pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f,7f))))}}
 }
}
