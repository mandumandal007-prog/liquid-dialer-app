package com.mandu.liquiddialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.CallChecklist
import com.mandu.liquiddialer.data.FeatureControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun CallChecklistPage(number:String,onBack:()->Unit) {
 CallChecklistWorkspace(number,onBack)
}

/** Shared by number-details page and the existing call-time Notes sheet. All durable IO is off Main. */
@Composable fun CallChecklistWorkspace(number:String,onBack:()->Unit,inSheet:Boolean=false) {
 val features by com.mandu.liquiddialer.LdApp.settings.collectAsState()
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var rows by remember(number){mutableStateOf<List<CallChecklist.Item>>(emptyList())}
 var loaded by remember(number){mutableStateOf(false)};var busy by remember(number){mutableStateOf(false)};var error by remember(number){mutableStateOf("")}
 var editor by remember(number){mutableStateOf<String?>(null)};var draft by remember(number){mutableStateOf("")};var original by remember(number){mutableStateOf("")}
 var focus by remember(number){mutableStateOf(false)}
 var templates by remember(number){mutableStateOf(false)}
 var discard by remember(number){mutableStateOf(false)};var confirm by remember(number){mutableStateOf<String?>(null)}
 fun update(action:()->List<CallChecklist.Item>,after:()->Unit={}) {
  if(busy)return;busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){action()};rows=next;loaded=true;error="";after()}catch(e:Exception){error=(e as? IllegalArgumentException)?.message ?: "Checklist save/load nahi hui. Purana data nahi badla; dobara load karo."}finally{busy=false}}
 }
 LaunchedEffect(number){busy=true;try{rows=withContext(Dispatchers.IO){CallChecklist.read(ctx,number)};loaded=true}catch(_:Exception){error="Checklist khul nahi rahi. Saved file nahi badli."}finally{busy=false}}
 fun closeEditor(){if(draft!=original)discard=true else editor=null}
 fun back(){if(editor!=null)closeEditor()else onBack()}
 BackHandler(enabled=!templates&&!focus){back()}
 val content: @Composable ColumnScope.()->Unit = {
 LdText(number,16f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("checklist_number"))
 LdText("Is number se baat ke topics. Pehle taiyar karo, call ke dauran tick karo. Tick sirf tumhara mark hai, call ya awaaz se automatic nahi badlega.",13f,color=p.muted,maxLines=8)
 LdText("Phone par private, unencrypted storage. Contacts/calls nahi badlenge. App backup mein abhi ye topics nahi jaate; uninstall/data clear se mitenge. 30 topics per number, 100 numbers tak.",12f,color=p.muted,maxLines=8)
 if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("checklist_error"))
 if(!loaded||error.isNotEmpty())PrimaryButton("Dobara load karo",enabled=!busy,modifier=Modifier.testTag("checklist_retry")){update({CallChecklist.read(ctx,number)})}
 if(editor!=null){
  val id=editor!!
  SecTitle(if(id=="new")"Naya topic"else "Topic badlo")
  LdField(draft,{if(!busy)draft=it},"Baat ka topic",minLines=3,modifier=Modifier.testTag("checklist_draft"))
  LdText("${draft.trim().length} / 250 letters",12f,color=if(draft.trim().length>250)p.danger else p.muted,maxLines=2)
  Spacer(Modifier.height(8.dp))
  PrimaryButton("Save",enabled=!busy&&draft.trim().length in 1..250,onClick={val value=draft;update({if(id=="new")CallChecklist.add(ctx,number,value)else CallChecklist.edit(ctx,number,id,value)}){editor=null}},modifier=Modifier.testTag("checklist_editor_save"))
  PrimaryButton("Wapas",enabled=!busy,onClick={closeEditor()},modifier=Modifier.testTag("checklist_editor_cancel"))
 } else  if(loaded){
  Spacer(Modifier.height(8.dp))
  LdText("${rows.count{it.done}} / ${rows.size} discussed",14f,FontWeight.Bold,modifier=Modifier.testTag("checklist_progress"))
  Spacer(Modifier.height(8.dp))
  PrimaryButton(if(busy)"Save/load ho raha hai..."else "Topic jodo",enabled=!busy&&error.isEmpty()&&rows.size<CallChecklist.MAX_ITEMS,modifier=Modifier.testTag("checklist_add")){editor="new";draft="";original=""}
  Spacer(Modifier.height(8.dp))
  if(FeatureControls.enabled(features,"templates")) PrimaryButton("Reusable templates",enabled=!busy&&error.isEmpty(),modifier=Modifier.testTag("checklist_templates")){templates=true}
  Spacer(Modifier.height(8.dp))
  if(FeatureControls.enabled(features,"focus")) PrimaryButton("Ek-ek topic · Focus",enabled=!busy&&error.isEmpty()&&rows.isNotEmpty(),modifier=Modifier.testTag("checklist_focus")){focus=true}
  if(rows.isEmpty())LdText("Abhi koi topic nahi. Agli call ke liye sawaal, order ya follow-up likho.",13f,color=p.muted,maxLines=4)
  rows.forEach{item->GlassCard(Modifier.padding(top=10.dp).testTag("checklist_item_${item.id}")){
   Row(verticalAlignment=Alignment.Top){Checkbox(item.done,{on->update({CallChecklist.done(ctx,number,item.id,on)})},enabled=!busy&&error.isEmpty(),modifier=Modifier.testTag("checklist_tick_${item.id}"));Column(Modifier.weight(1f).padding(top=10.dp)){
    LdText(item.text,16f,FontWeight.SemiBold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("checklist_text_${item.id}"))
    LdText(if(item.done)"Discussed"else "Baat baaki",12f,color=p.muted)
   }}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Badlo",enabled=!busy&&error.isEmpty(),modifier=Modifier.weight(1f).testTag("checklist_edit_${item.id}")){editor=item.id;draft=item.text;original=item.text}
    PrimaryButton("Mitao",enabled=!busy&&error.isEmpty(),modifier=Modifier.weight(1f).testTag("checklist_remove_${item.id}")){confirm=item.id}
   }
  }}
  if(rows.isNotEmpty()){
   PrimaryButton("Agli baat ke liye sab Pending",enabled=!busy&&error.isEmpty()&&rows.any{it.done},modifier=Modifier.testTag("checklist_reset")){confirm="reset"}
   Spacer(Modifier.height(8.dp))
   PrimaryButton("Is number ki poori checklist mitao",enabled=!busy&&error.isEmpty(),modifier=Modifier.testTag("checklist_clear")){confirm="clear"}
  }
 }

 }
 if(focus){ChecklistFocusWorkspace(number,{focus=false},{next->rows=next},inSheet);return}
 if(templates){ChecklistTemplatesWorkspace(number,{templates=false},{next->rows=next},inSheet);return}
 if(inSheet)Column{SheetTitle("Call checklist");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("checklist_notes_back")){back()};content()}else PageFrame("Call checklist",{back()},content=content)
 if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Badlav save nahi hue")},text={Text("Likha hua topic chhodna hai? Saved checklist nahi badlegi.")},confirmButton={TextButton(onClick={discard=false;editor=null},modifier=Modifier.testTag("checklist_discard")){Text("Chhod do")}},dismissButton={TextButton(onClick={discard=false},modifier=Modifier.testTag("checklist_keep")){Text("Likhte raho")}})
 confirm?.let{action->AlertDialog(onDismissRequest={if(!busy)confirm=null},title={Text(if(action=="reset")"Sab topics Pending karein?"else "Checklist se mitayein?")},text={Text(if(action=="reset")"Topics rahenge, discussed ticks saaf honge. Isse koi call nahi lagegi."else if(action=="clear")"Sirf is number ke saare topics aur ticks mitenge. Wapas nahi la sakte."else "Sirf yeh topic aur iska tick mitega. Wapas nahi la sakte.")},confirmButton={TextButton(enabled=!busy,onClick={update({when(action){"reset"->CallChecklist.reset(ctx,number);"clear"->CallChecklist.clear(ctx,number);else->CallChecklist.remove(ctx,number,action)}}){confirm=null}},modifier=Modifier.testTag("checklist_confirm")){Text(if(action=="reset")"Pending karo"else "Mitao")}},dismissButton={TextButton(enabled=!busy,onClick={confirm=null},modifier=Modifier.testTag("checklist_cancel_confirm")){Text("Rehne do")}})}
}
