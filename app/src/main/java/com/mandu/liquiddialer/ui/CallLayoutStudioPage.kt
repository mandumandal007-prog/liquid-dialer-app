package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun CallLayoutStudioPage(onBack:()->Unit){
 val saved by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope()
 var base by remember{mutableStateOf(CallLayoutDraft.capture(saved))};var draft by remember{mutableStateOf(base)}
 var undo by remember{mutableStateOf<CallLayoutDraft?>(null)};var applied by remember{mutableStateOf<CallLayoutDraft?>(null)}
 var error by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var status by remember{mutableStateOf("")}
 fun mutate(next:CallLayoutDraft){draft=next;status=""}
 fun commit(expected:CallLayoutDraft,next:CallLayoutDraft,isUndo:Boolean=false){if(busy)return;busy=true;error="";scope.launch{try{LdApp.instance.ldStore.edit{CallLayoutDraft.applyIfCurrent(it,expected,next)};base=next;draft=next;if(isUndo){undo=null;applied=null;status="Pichla layout wapas aa gaya"}else{undo=expected;applied=next;status="Layout apply ho gaya"}}catch(e:Exception){error="Layout apply nahi hua. Doosri jagah badlav hua ho sakta hai; saved layout reload karo."}finally{busy=false}}}
 PageFrame("Call layout studio",onBack){
  LdText("Pehle preview, phir Apply",19f,FontWeight.Bold,maxLines=3)
  LdText("Order, visibility aur button size ka draft banao. Preview buttons call nahi lagate. Apply se pehle kuch save nahi hota. End hamesha available rahega.",12.5f,color=p.muted,maxLines=8)
  SecTitle("Safe preview")
  GlassCard{Column(Modifier.fillMaxWidth().padding(vertical=12.dp).testTag("layout_studio_preview")){
   LdText("Sample call · 00:42",14f,FontWeight.SemiBold,maxLines=2)
   LdText("Koi live call nahi",11f,color=p.muted,maxLines=2);Spacer(Modifier.height(12.dp))
   CompositionLocalProvider(LocalCallStyle provides callStyleFor(saved.callCardStyle)){
    CallControlsLayout(parseLayout(draft.grid,GRID_IDS),parseLayout(draft.bottom,BOTTOM_IDS),draft.gridSize,draft.bottomSize,{id->BtnSpec(btnIcon(id),btnName(id)){}},{},compact=true)
   }
  }}
  LdText("Preview selected call style ke controls dikhata hai; asli call mein phone/call capability ke hisaab se buttons unavailable ho sakte hain.",11.5f,color=p.muted,maxLines=6)
  SecTitle("Starting layouts")
  GlassCard{CallLayoutDraft.presets().forEachIndexed{i,(name,value)->SRow(name,when(name){"Essentials"->"Mute, keypad, audio route aur speaker; baaki hidden";"Conference"->"Hold, add aur merge paas mein; record hidden";else->"Saare standard controls, normal size"},LdIcons.Dialpad,last=i==2){if(!busy)mutate(value)}}}
  SecTitle("Grid buttons")
  StudioRows(draft,false,busy){mutate(it)}
  StudioSize("Grid size",draft.gridSize,"layout_grid_size",busy){mutate(draft.copy(gridSize=it))}
  SecTitle("Bottom row")
  StudioRows(draft,true,busy){mutate(it)}
  StudioSize("Bottom size",draft.bottomSize,"layout_bottom_size",busy){mutate(draft.copy(bottomSize=it))}
  Spacer(Modifier.height(16.dp))
  if(CallLayoutDraft.capture(saved)!=base)LdText("Saved layout badal gaya. Draft apply karne se pehle reload karo.",12.5f,color=p.accent,maxLines=5,modifier=Modifier.testTag("layout_studio_stale"))
  if(error.isNotBlank())LdText(error,12.5f,color=p.accent,maxLines=5,modifier=Modifier.testTag("layout_studio_error"))
  if(status.isNotBlank())LdText(status,12.5f,color=p.accent,maxLines=5,modifier=Modifier.testTag("layout_studio_status"))
  PrimaryButton(if(busy)"Saving..."else"Apply layout",modifier=Modifier.testTag("layout_studio_apply")){if(!busy)commit(base,draft)}
  GhostButton("Saved layout reload karo",Modifier.testTag("layout_studio_reload")){if(!busy){base=CallLayoutDraft.capture(saved);draft=base;error="";status="Saved layout loaded"}}
  GhostButton("Draft discard karo",Modifier.testTag("layout_studio_discard")){if(!busy){draft=base;error="";status="Draft hata diya"}}
  if(undo!=null&&applied!=null)GhostButton("Undo last Apply",Modifier.testTag("layout_studio_undo")){if(!busy)commit(applied!!,undo!!,true)}
  Spacer(Modifier.height(10.dp));LdText("Sirf arrangement aur do button sizes badlenge. Theme, photo, individual icon overrides, call behavior, recording aur permissions wahi rahenge. Undo isi page session ke last Apply ke liye hai; bahar badla layout overwrite nahi hoga. Existing layout controls ab bhi alag available hain.",11.5f,color=p.muted,maxLines=12)
 }
}
@Composable private fun StudioRows(draft:CallLayoutDraft,bottom:Boolean,busy:Boolean,onChange:(CallLayoutDraft)->Unit){
 val list=(if(bottom)draft.bottom else draft.grid).split(',');val p=LocalLd.current
 GlassCard{list.forEachIndexed{i,t->val id=t.removePrefix("!");Column(Modifier.fillMaxWidth().padding(vertical=6.dp).testTag("studio_row_$id")){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){LdText(btnName(id),14f,FontWeight.SemiBold,maxLines=2);LdText(if(t.startsWith('!'))"Hidden"else"Visible",11.5f,color=p.muted)}
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
   if(i>0)GhostButton("Up",Modifier.weight(1f).testTag("studio_up_$id")){if(!busy)onChange(draft.move(bottom,id,-1))}
   if(i<list.lastIndex)GhostButton("Down",Modifier.weight(1f).testTag("studio_down_$id")){if(!busy)onChange(draft.move(bottom,id,1))}
   if(id!="end")GhostButton(if(t.startsWith('!'))"Show"else"Hide",Modifier.weight(1f).testTag("studio_toggle_$id")){if(!busy)onChange(draft.toggle(bottom,id))}
  }
 }}}
}
@Composable private fun StudioSize(title:String,value:Float,tag:String,busy:Boolean,onChange:(Float)->Unit){
 SecTitle(title);Row(Modifier.fillMaxWidth().testTag(tag),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(.85f,1f,1.15f,1.3f).forEach{v->GhostButton((v*100).toInt().toString()+"%"+(if(value==v)" ✓"else""),Modifier.weight(1f)){if(!busy)onChange(v)}}}
}
