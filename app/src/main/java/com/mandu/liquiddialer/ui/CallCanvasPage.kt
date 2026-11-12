package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable fun CallCanvasPage(onBack:()->Unit){
 val p=LocalLd.current;val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();var number by remember{mutableStateOf("")};var open by remember{mutableStateOf<String?>(null)};var error by remember{mutableStateOf("")}
 open?.let{key(it){CallCanvasWorkspace(it,{open=null})};return}
 PageFrame("Call canvas",onBack){
  LdText("Baat suno. Ungli se idea banao.",23f,FontWeight.Bold,maxLines=4)
  LdText("Pen, line, arrow, box aur oval se diagram banao. Select se ink ko move, copy ya remove karo. Har full number ka apna saved canvas. Save dabao. App ya Notes sheet band hone par unsaved ink jaa sakti hai.",14f,color=p.muted,maxLines=7)
  SwitchRow("Show canvas in call Notes","Default OFF. Entry hide karne se saved drawings delete nahi hote.",s.callCanvas,last=true){scope.launch{try{LdApp.instance.repo.set(K.callCanvas,it)}catch(_:Exception){error="Setting save nahi hui"}}}
  SwitchRow("Canvas resize, rotate & mirror","Default OFF. Select ink to show tools. Each edit has Undo; Save stays explicit.",s.canvasTransform,last=true){scope.launch{try{LdApp.instance.repo.set(K.canvasTransform,it)}catch(_:Exception){error="Setting save nahi hui"}}}
  LdText("Size ±10%, rotate ±15°, mirror left/right or up/down around selected ink center. Paper edge par edit refuse hota hai, clip nahi. Pen width same rahegi. One stroke/shape at a time.",12f,color=p.muted,maxLines=8)
  LdField(number,{number=it.take(40)},"Full phone number",modifier=Modifier.testTag("canvas_number_input"))
  PrimaryButton("Open this number's canvas",modifier=Modifier.testTag("canvas_open")){runCatching{CallCanvas.key(number)}.fold({open=number;error=""},{error="Sahi full phone number daalo"})}
  LdText("Offline finger sketch only. Koi AI/handwriting recognition, call recording, auto-send ya cloud sync nahi. Plain local data, encrypted vault nahi; secrets mat likho. Backup/transfer mein include nahi.",12f,color=p.muted,maxLines=10)
  if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=4)
 }
}

@Composable fun CallCanvasWorkspace(number:String,onBack:()->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();val settings by LdApp.settings.collectAsState()
 var paperAspect by remember{mutableDoubleStateOf(0.0)}
 var snapshot by remember(number){mutableStateOf<CallCanvas.Snapshot?>(null)};var history by remember(number){mutableStateOf(CallCanvas.History(emptyList()))};var revision by remember(number){mutableIntStateOf(0)}
 var busy by remember(number){mutableStateOf(false)};var error by remember(number){mutableStateOf("")};var status by remember(number){mutableStateOf("")};var ink by remember(number){mutableIntStateOf(0)};var width by remember(number){mutableIntStateOf(2)}
 var selecting by remember(number){mutableStateOf(false)};var selected by remember(number){mutableStateOf<Int?>(null)}
 var tool by remember(number){mutableStateOf(CanvasShapes.Tool.PEN)}
 var confirm by remember(number){mutableStateOf("")};var drawing by remember(number){mutableStateOf(false)}
 val strokes=remember(revision,history){history.strokes};val dirty=snapshot!=null&&strokes!=snapshot!!.strokes
 fun transform(edit:CanvasTransform.Edit){
  if(busy||drawing||!settings.canvasTransform||paperAspect<=0)return
  val i=selected?:return;val original=strokes.getOrNull(i)?:return
  val next=CanvasTransform.apply(original,edit,paperAspect)
  status=when{next==null->"Paper edge: edit nahi hua. Ink ko center ki taraf move karo ya chhota karo."
   next==original->"Is stroke par is edit se koi badlav nahi hua."
   history.replace(i,next)->{revision++;"${edit.label} · Unsaved · Undo available"}
   else->"Edit nahi hua. Drawing unchanged."}
 }
 fun load(){busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){CallCanvas.read(ctx,number)};selected=null;snapshot=next;history=CallCanvas.History(next.strokes);revision++;error="";status="Saved canvas loaded"}catch(_:Exception){snapshot=null;error="Canvas load nahi hua. Saved file preserve hai; drawing/save band hain."}finally{busy=false}}}
 fun back(){if(busy||drawing)return;if(dirty)confirm="back"else onBack()}
 LaunchedEffect(number){load()};BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("canvas_identity"))
  LdText("Ungli canvas ke andar chalao. Bahar ki jagah se page scroll karo. Explicit Save · call/awaaz par koi asar nahi.",12f,color=p.muted,maxLines=5)
  if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("canvas_error"))
  if(snapshot!=null){
   LdText(if(dirty)"Unsaved drawing"else"Saved · ${strokes.size} strokes",12f,FontWeight.Bold,color=if(dirty)p.accent else p.muted,modifier=Modifier.testTag("canvas_state"))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Ink","Blue","Red","Green").forEachIndexed{i,label->GhostButton((if(ink==i)"✓ "else"")+label,Modifier.weight(1f).testTag("canvas_ink_$i")){if(!drawing)ink=i}}}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Fine","Pen","Bold").forEachIndexed{i,label->GhostButton((if(width==i+1)"✓ "else"")+label,Modifier.weight(1f).testTag("canvas_width_${i+1}")){if(!drawing)width=i+1}}}
   LdText("Drawing tools",12f,color=p.muted,maxLines=2)
   Column(Modifier.fillMaxWidth().testTag("canvas_tools"),verticalArrangement=Arrangement.spacedBy(6.dp)){(CanvasShapes.Tool.entries.map{it.label}+"Select").chunked(3).forEach{labels->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){labels.forEach{label->val t=CanvasShapes.Tool.entries.firstOrNull{it.label==label};val active=if(t==null)selecting else !selecting&&tool==t;GhostButton((if(active)"✓ "else"")+label,Modifier.weight(1f).testTag("canvas_tool_${label.lowercase()}")){if(!busy&&!drawing){selecting=t==null;selected=null;if(t!=null)tool=t}}}}}}
   LdText(if(selecting)"Tap ink to select; drag ink to move. Empty area clears selection."else if(tool==CanvasShapes.Tool.PEN)"Pen: draw freely"else "${tool.label}: drag start to end, then lift. One shape = one Undo.",12f,color=p.muted,maxLines=4,modifier=Modifier.testTag("canvas_tool_hint"))
   if(selecting){
    LdText(selected?.let{"Selected stroke ${it+1} of ${strokes.size}. Move/copy/remove stays unsaved until Save."}?:"Nothing selected. Tap a line, not the empty middle of a shape.",12f,color=p.muted,maxLines=4,modifier=Modifier.testTag("canvas_selection"))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
     PrimaryButton("Copy",enabled=selected!=null&&!busy&&!drawing,modifier=Modifier.weight(1f).testTag("canvas_copy")){selected?.let{i->if(history.add(CanvasArrange.duplicate(strokes[i]))){selected=history.strokes.lastIndex;revision++;status="Copy added · Undo available"}else status="Canvas full. Copy not added."}}
     PrimaryButton("Remove",enabled=selected!=null&&!busy&&!drawing,danger=true,modifier=Modifier.weight(1f).testTag("canvas_remove")){selected?.let{i->if(history.remove(i)){selected=null;revision++;status="Stroke removed · Undo available"}}}
    }
    if(settings.canvasTransform){
     Column(Modifier.fillMaxWidth().testTag("canvas_transforms"),verticalArrangement=Arrangement.spacedBy(6.dp)){
      LdText("Resize · rotate · mirror",14f,FontWeight.Bold,maxLines=3)
      LdText("Center pivot · pen width same · edge crossing refused. Har edit ka Undo; Save dabane tak local file nahi badlegi.",12f,color=p.muted,maxLines=5)
      CanvasTransform.Edit.entries.chunked(2).forEach{edits->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){edits.forEach{edit->PrimaryButton(edit.label,enabled=selected!=null&&!busy&&!drawing&&paperAspect>0,modifier=Modifier.weight(1f).testTag("canvas_transform_${edit.name.lowercase()}")){transform(edit)}}}}
      if(status.isNotEmpty())LdText(status,12f,color=p.muted,maxLines=5,modifier=Modifier.testTag("canvas_transform_status"))
     }
    }
   }
   CanvasInk(strokes,ink,width,tool,selecting,selected,!busy,{paperAspect=it},{drawing=it},{selected=it},{i,stroke->if(history.replace(i,stroke)){selected=i;revision++;status="Stroke moved · Undo available"}}){stroke->if(history.add(stroke)){revision++;status=""}else status="Canvas full. Undo/clear karke jagah banao; Save se limit reset nahi hoti."}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Undo",enabled=!busy&&!drawing&&history.canUndo,modifier=Modifier.weight(1f).testTag("canvas_undo")){history.undo();selected=null;revision++;status=""}
    PrimaryButton("Redo",enabled=!busy&&!drawing&&history.canRedo,modifier=Modifier.weight(1f).testTag("canvas_redo")){history.redo();selected=null;revision++;status=""}
   }
   PrimaryButton(if(busy)"Saving/loading..."else"Save this number's canvas",enabled=!busy&&!drawing&&dirty,modifier=Modifier.testTag("canvas_save")){
    val expected=snapshot!!.raw;val next=strokes.toList();busy=true;scope.launch{try{snapshot=withContext(Dispatchers.IO){CallCanvas.save(ctx,number,expected,next)};error="";status="Canvas saved locally"}catch(e:Exception){error=e.message?:"Save nahi hua. Drawing abhi yahin hai."}finally{busy=false}}
   }
   GhostButton("Clear drawing",Modifier.testTag("canvas_clear")){if(!busy&&!drawing&&strokes.isNotEmpty())confirm="clear"}
   LdText("${strokes.size}/80 strokes · ${strokes.sumOf{it.points.size}}/12000 points. 30 numbers tak. Canvas redraw size ke saath fit hota hai.",11f,color=p.muted,maxLines=4)
  }
  GhostButton("Reload saved canvas",Modifier.testTag("canvas_reload")){if(!busy&&!drawing){if(dirty)confirm="reload"else load()}}
  if(status.isNotBlank())LdText(status,12f,color=p.muted,maxLines=5,modifier=Modifier.testTag("canvas_status"))
  if(confirm.isNotEmpty())Dialog(onDismissRequest={confirm=""}){GlassCard(Modifier.testTag("canvas_confirm")){
   LdText(if(confirm=="clear")"Canvas khali karo? Save dabane tak saved drawing nahi badlegi. Undo/redo history bhi clear hogi."else"Unsaved drawing chhod do?",15f,FontWeight.Bold,maxLines=6)
   PrimaryButton(if(confirm=="clear")"Haan, clear drawing"else"Discard unsaved drawing",danger=true,modifier=Modifier.testTag("canvas_confirm_yes")){val action=confirm;confirm="";when(action){"clear"->{history.clear();selected=null;revision++};"reload"->load();"back"->onBack()}}
   GhostButton("Keep drawing",Modifier.testTag("canvas_confirm_no")){confirm=""}
  }}
 }
 if(inSheet)Column{SheetTitle("Call canvas");PrimaryButton("Notes par wapas",enabled=!busy&&!drawing,modifier=Modifier.testTag("canvas_back")){back()};content()}
 else PageFrame("Call canvas",{back()},content=content)
}

private val CanvasColors=listOf(Color(0xff263449),Color(0xff245be0),Color(0xffd83d51),Color(0xff168054))
@Composable private fun CanvasInk(strokes:List<CallCanvas.Stroke>,ink:Int,width:Int,tool:CanvasShapes.Tool,selecting:Boolean,selected:Int?,enabled:Boolean,onAspect:(Double)->Unit,onDrawing:(Boolean)->Unit,onSelect:(Int?)->Unit,onMove:(Int,CallCanvas.Stroke)->Unit,onStroke:(CallCanvas.Stroke)->Unit){
 var preview by remember{mutableStateOf<List<CallCanvas.Point>>(emptyList())}
 var movingIndex by remember{mutableStateOf<Int?>(null)};var moved by remember{mutableStateOf<CallCanvas.Stroke?>(null)}
 val latestStrokes by rememberUpdatedState(strokes);val latestSelect by rememberUpdatedState(onSelect);val latestMove by rememberUpdatedState(onMove)
 val latestStroke by rememberUpdatedState(onStroke);val latestDrawing by rememberUpdatedState(onDrawing)
 Canvas(Modifier.fillMaxWidth().aspectRatio(1.1f).padding(vertical=8.dp).onSizeChanged{if(it.height>0)onAspect(it.width.toDouble()/it.height)}.clip(RoundedCornerShape(20.dp)).background(Color(0xfffffbef)).testTag("canvas_paper").semantics{contentDescription="Finger drawing canvas, ${strokes.size} strokes. Use text Notes if finger drawing is not accessible."}.pointerInput(ink,width,tool,selecting,enabled){
  if(!enabled)return@pointerInput
  awaitEachGesture{
   val first=awaitFirstDown(requireUnconsumed=true);first.consume();latestDrawing(true)
   fun point(o:Offset)=CallCanvas.Point((o.x/size.width*10000).roundToInt().coerceIn(0,10000),(o.y/size.height*10000).roundToInt().coerceIn(0,10000))
   val start=point(first.position);val hit=if(selecting)CanvasArrange.hit(latestStrokes,start,size.width.toFloat(),size.height.toFloat(),24.dp.toPx())else null;val original=hit?.let{latestStrokes[it]};var end=start;movingIndex=hit
   val points=mutableListOf(start);preview=if(!selecting&&tool==CanvasShapes.Tool.PEN)points.toList()else emptyList();var cancelled=false
   try{
    while(true){val event=awaitPointerEvent();if(event.changes.size!=1){cancelled=true;event.changes.forEach{it.consume()};break};val change=event.changes.first();if(change.isConsumed){cancelled=true;break};change.consume();val next=point(change.position)
     end=next
     if(selecting){moved=original?.let{CanvasArrange.move(it,next.x-start.x,next.y-start.y)}}
     else if(tool==CanvasShapes.Tool.PEN){if(next!=points.last()){CallCanvas.appendSample(points,next);preview=points.toList()}}
     else{points.clear();points.addAll(CanvasShapes.points(tool,start,next));preview=points.toList()}
     if(!change.pressed)break
    }
    if(!cancelled&&selecting){latestSelect(hit);if(hit!=null&&original!=null&&end!=start)latestMove(hit,CanvasArrange.move(original,end.x-start.x,end.y-start.y))}
    if(!selecting&&!cancelled&&points.isNotEmpty()&&(tool==CanvasShapes.Tool.PEN||preview.isNotEmpty()))latestStroke(CallCanvas.Stroke(ink,width,points.toList()))
   }finally{preview=emptyList();moved=null;movingIndex=null;latestDrawing(false)}
  }
 }){
  val grid=Color(0xffdcdccf);val step=size.width/12
  for(i in 1..11)drawLine(grid,Offset(i*step,0f),Offset(i*step,size.height),1f)
  for(i in 1..((size.height/step).toInt()))drawLine(grid,Offset(0f,i*step),Offset(size.width,i*step),1f)
  fun line(stroke:CallCanvas.Stroke){val ps=stroke.points;val color=CanvasColors[stroke.ink];val thickness=size.width*(when(stroke.width){1->0.005f;2->0.009f;else->0.016f});fun xy(p:CallCanvas.Point)=Offset(p.x/10000f*size.width,p.y/10000f*size.height)
   if(ps.size==1)drawCircle(color,thickness/2,xy(ps.first()))else{val path=Path().apply{moveTo(xy(ps[0]).x,xy(ps[0]).y);ps.drop(1).forEach{lineTo(xy(it).x,xy(it).y)}};drawPath(path,color,style=Stroke(thickness,cap=StrokeCap.Round,join=androidx.compose.ui.graphics.StrokeJoin.Round))}
  }
  strokes.forEachIndexed{i,s->line(if(i==movingIndex)moved?:s else s)};if(preview.isNotEmpty())line(CallCanvas.Stroke(ink,width,preview))
  if(selecting){val index=movingIndex?:selected;val s=if(movingIndex!=null)moved?:index?.let{strokes.getOrNull(it)}else index?.let{strokes.getOrNull(it)}
   s?.let{val ps=it.points;val left=ps.minOf{v->v.x}/10000f*size.width;val top=ps.minOf{v->v.y}/10000f*size.height;val right=ps.maxOf{v->v.x}/10000f*size.width;val bottom=ps.maxOf{v->v.y}/10000f*size.height;drawRect(Color(0xff245be0),Offset(left-5,top-5),androidx.compose.ui.geometry.Size((right-left+10).coerceAtLeast(10f),(bottom-top+10).coerceAtLeast(10f)),style=Stroke(2.dp.toPx(),pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f,7f))))}
  }
 }
}
