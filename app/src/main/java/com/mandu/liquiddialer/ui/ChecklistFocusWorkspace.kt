package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun ChecklistFocusWorkspace(number:String,onBack:()->Unit,onChanged:(List<CallChecklist.Item>)->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var rows by remember(number){mutableStateOf<List<CallChecklist.Item>>(emptyList())};var loaded by remember(number){mutableStateOf(false)};var busy by remember(number){mutableStateOf(false)};var error by remember(number){mutableStateOf("")}
 var pending by remember(number){mutableStateOf(true)};var selected by remember(number){mutableStateOf<String?>(null)}
 var undo by remember(number){mutableStateOf<Pair<CallChecklist.Item,CallChecklist.Item>?>(null)}
 fun load(){if(busy)return;busy=true;scope.launch{try{rows=withContext(Dispatchers.IO){CallChecklist.read(ctx,number)};loaded=true;error="";undo=null;onChanged(rows)}catch(e:CancellationException){throw e}catch(_:Exception){error="Checklist load nahi hui. Purane topics neeche ho sakte hain; ticks band hain. Dobara load karo."}finally{busy=false}}}
 LaunchedEffect(number){load()};BackHandler{if(!busy)onBack()}
 val shown=ChecklistFocus.visible(rows,pending);val item=ChecklistFocus.selected(shown,selected);val index=shown.indexOf(item)
 fun mark(original:CallChecklist.Item,value:Boolean){if(busy||error.isNotEmpty())return;busy=true;val oldShown=shown;scope.launch{try{val next=withContext(Dispatchers.IO){CallChecklist.markChecked(ctx,number,original,value)};rows=next;onChanged(next);selected=ChecklistFocus.afterMark(oldShown,ChecklistFocus.visible(next,pending),original.id);undo=original to original.copy(done=value);error=""}catch(e:CancellationException){throw e}catch(e:Exception){error=(e as? IllegalArgumentException)?.message?:"Tick save nahi hua. Dobara load karo; purani file nahi badli."}finally{busy=false}}}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,15f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("focus_number"))
  LdText("Ek waqt ek topic. Aage/peechhe sirf padhna hai; Discussed tum khud mark karte ho. Call, awaaz ya timer se kuch automatic nahi hota.",13f,color=p.muted,maxLines=7)
  Spacer(Modifier.height(8.dp))
  PrimaryButton("Dobara load karo",enabled=!busy,modifier=Modifier.testTag("focus_reload")){load()}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=8,modifier=Modifier.testTag("focus_error"))
  if(loaded){
   LdText("${rows.count{it.done}} / ${rows.size} discussed · ${rows.count{!it.done}} Pending",14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("focus_progress"))
   Spacer(Modifier.height(8.dp))
   PrimaryButton(if(pending)"Sirf Pending · sab dekho"else "Sab topics · sirf Pending",enabled=!busy,modifier=Modifier.testTag("focus_filter")){pending=!pending;selected=null}
   item?.let{r->
    GlassCard(Modifier.padding(vertical=12.dp).testTag("focus_card")){
     LdText("${index+1} / ${shown.size} · ${if(r.done)"Discussed"else "Pending"}",13f,color=p.accent,maxLines=4,modifier=Modifier.testTag("focus_position"))
     Spacer(Modifier.height(10.dp))
     LdText(r.text,23f,FontWeight.SemiBold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("focus_text"))
    }
    PrimaryButton(if(r.done)"Pending rakho"else "Discussed mark karo",enabled=!busy&&error.isEmpty(),modifier=Modifier.testTag("focus_mark")){mark(r,!r.done)}
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
     PrimaryButton("Peechhe",enabled=!busy&&index>0,modifier=Modifier.weight(1f).testTag("focus_previous")){selected=ChecklistFocus.move(shown,r.id,-1)}
     PrimaryButton("Aage",enabled=!busy&&index<shown.lastIndex,modifier=Modifier.weight(1f).testTag("focus_next")){selected=ChecklistFocus.move(shown,r.id,1)}
    }
   }?:GlassCard(Modifier.padding(vertical=12.dp)){LdText(if(rows.isEmpty())"Checklist mein abhi topic nahi."else "Sab topics discussed hain.",20f,FontWeight.Bold,maxLines=6,modifier=Modifier.testTag("focus_empty"));LdText("Wapas jaakar topics jodo ya Sab topics chuno. Koi call/message nahi hua.",13f,color=p.muted,maxLines=5)}
   undo?.let{(before,expected)->
    Spacer(Modifier.height(12.dp))
    LdText("Aakhri mark: ${before.text}",12f,color=p.muted,maxLines=Int.MAX_VALUE)
    PrimaryButton("Aakhri mark wapas",enabled=!busy&&error.isEmpty(),modifier=Modifier.testTag("focus_undo")){
     busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){CallChecklist.markChecked(ctx,number,expected,before.done)};rows=next;onChanged(next);selected=before.id;undo=null;error=""}catch(e:CancellationException){throw e}catch(e:Exception){error=(e as? IllegalArgumentException)?.message?:"Mark wapas nahi hua. Dobara load karo."}finally{busy=false}}
    }
   }
   LdText("Ticks turant local checklist mein save hote hain. Yeh is number ki wahi checklist hai; nayi call par automatic reset nahi hoti. Undo sirf aakhri mark, jab tak topic doosri jagah nahi badla.",12f,color=p.muted,maxLines=8)
  }else if(busy)LdText("Topics load ho rahe hain...",13f,color=p.muted)
 }
 if(inSheet)Column{SheetTitle("Checklist focus");PrimaryButton("Checklist par wapas",enabled=!busy,modifier=Modifier.testTag("focus_back")){onBack()};content()}else PageFrame("Checklist focus",{if(!busy)onBack()}){PrimaryButton("Checklist par wapas",enabled=!busy,modifier=Modifier.testTag("focus_back")){onBack()};content()}
}
