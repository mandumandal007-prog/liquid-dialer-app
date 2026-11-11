package com.mandu.liquiddialer.ui
import android.telecom.Call
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.IvrMenus
import com.mandu.liquiddialer.telecom.CallManager
import com.mandu.liquiddialer.telecom.IvrTone
import java.util.UUID
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.K
import kotlinx.coroutines.launch

@Composable fun IvrMenusPage(onBack:()->Unit){
 val ctx=LocalContext.current;val p=LocalLd.current
 val settings by LdApp.settings.collectAsState();val scope=rememberCoroutineScope()
 var raw by remember{mutableStateOf(IvrMenus.raw(ctx))};val parsed=remember(raw){runCatching{IvrMenus.decode(raw)}}
 var editing by remember{mutableStateOf<IvrMenus.Menu?>(null)};var new by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")};var digits by remember{mutableStateOf("")};var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")}
 var removing by remember{mutableStateOf<IvrMenus.Menu?>(null)}
 PageFrame("IVR menu helper",onBack){
  LdText("Saved menu choices, ek-ek digit",19f,FontWeight.Bold,maxLines=3)
  LdText("Sirf ordinary menu options save karo, jaise 1 phir 2 phir #. PIN, OTP, card/account number ya secret mat rakho. Ye plain local data hai, encrypted vault nahi. Koi number dial nahi hota, automatic tones nahi bhejte. Appearance backup mein include nahi.",12.5f,color=p.muted,maxLines=10)
  SwitchRow("Show in call keypad","Default OFF. Sirf helper ka entry dikhata hai; tones apne aap nahi bhejta.",settings.ivrHelper,last=true){scope.launch{try{LdApp.instance.repo.set(K.ivrHelper,it)}catch(_:Exception){error="Setting save nahi hui"}}}
  if(parsed.isFailure)LdText("Saved data padh nahi saka. Preserve kiya hai; overwrite/delete nahi hoga.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("ivr_corrupt"))
  else{
   if(new||editing!=null){
    SecTitle("Draft menu")
    LdField(name,{name=it.take(40)},"Naam",modifier=Modifier.testTag("ivr_name"))
    LdField(digits,{digits=it.take(24)},"Digits: 0-9, * ya # (max 24)",modifier=Modifier.testTag("ivr_digits"))
    PrimaryButton("Save menu",modifier=Modifier.testTag("ivr_save")){
     runCatching{val m=IvrMenus.Menu(editing?.id?:UUID.randomUUID().toString(),name.trim(),digits);m.validate();val xs=parsed.getOrThrow();IvrMenus.save(ctx,raw,if(new)xs+m else xs.map{if(it.id==m.id)m else it})}.fold({raw=it;editing=null;new=false;error="";status="Menu saved"},{error="Save nahi hua. Naam aur sirf 0-9 * # digits check karo; data badla ho to Reload karo."})
    }
    GhostButton("Cancel draft",Modifier.testTag("ivr_cancel")){editing=null;new=false;error=""}
   }else{
    if(parsed.getOrThrow().isEmpty())LdText("Koi saved menu nahi. Call se pehle yahan taiyar karo.",13f,color=p.muted,maxLines=4)
    parsed.getOrThrow().forEach{m->GlassCard(Modifier.padding(top=12.dp).testTag("ivr_menu_${m.id}")){
     LdText(m.name,16f,FontWeight.Bold,maxLines=3);LdText(m.digits.toList().joinToString("  →  "),14f,maxLines=5)
     GhostButton("Edit ${m.name}",Modifier.testTag("ivr_edit_${m.id}")){editing=m;name=m.name;digits=m.digits;error="";status=""}
     GhostButton("Delete ${m.name}",Modifier.testTag("ivr_delete_${m.id}")){removing=m}
    }}
    if(parsed.getOrThrow().size<IvrMenus.MAX)PrimaryButton("Add menu",modifier=Modifier.testTag("ivr_add")){new=true;name="";digits="";error="";status=""}
   }
   removing?.let{m->GlassCard(Modifier.testTag("ivr_delete_confirm")){
    LdText("${m.name} delete karein?",14f,FontWeight.Bold,maxLines=3)
    PrimaryButton("Haan, delete",modifier=Modifier.testTag("ivr_delete_yes")){runCatching{IvrMenus.save(ctx,raw,parsed.getOrThrow().filterNot{it.id==m.id})}.fold({raw=it;removing=null;status="Menu deleted"},{error="Delete nahi hua. Reload karo."})}
    GhostButton("Cancel delete"){removing=null}
   }}
  }
  if(error.isNotBlank())LdText(error,12.5f,color=p.danger,maxLines=5,modifier=Modifier.testTag("ivr_error"))
  if(status.isNotBlank())LdText(status,12.5f,color=p.accent,maxLines=3,modifier=Modifier.testTag("ivr_status"))
  GhostButton("Discard draft / Reload",Modifier.testTag("ivr_reload")){raw=IvrMenus.raw(ctx);editing=null;new=false;removing=null;error="";status="Saved menus loaded"}
  LdText("Active SIM call mein keypad kholo, phir IVR menu helper. Menu select karne se kuch nahi bhejta; har agle digit par alag Send tap chahiye. Hold, conference, multiple calls ya call badalne par sending rukti hai. Service/provider tone ko accept kare, iski guarantee nahi.",12f,color=p.muted,maxLines=9)
 }
}

@Composable fun IvrCallSheet(target:Call,onClose:()->Unit){
 val ctx=LocalContext.current;val p=LocalLd.current;val version by CallManager.version.collectAsState();val calls by CallManager.calls.collectAsState();val settings by LdApp.settings.collectAsState()
 val menus=remember{runCatching{IvrMenus.decode(IvrMenus.raw(ctx))}}
 var session by remember(target){mutableStateOf<IvrMenus.Session?>(null)};var tick by remember{mutableIntStateOf(0)};var notice by remember{mutableStateOf("")}
 val allowed=remember(version,calls,target,settings.ivrHelper){settings.ivrHelper&&IvrTone.allowed(target)}
 SheetTitle("IVR · ek digit har tap")
 LdText("Menu select karna tones nahi bhejta. IVR bole tab next digit bhejo. PIN/OTP yahan mat rakho.",12.5f,color=p.muted,maxLines=5)
 if(!allowed)LdText("Sending unavailable: wahi ek active, non-conference call chahiye. Hold ya call change par band.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("ivr_unavailable"))
 if(menus.isFailure)LdText("Saved menus padh nahi saka. Settings mein check karo.",13f,color=p.danger,maxLines=4)
 else if(session==null){
  if(menus.getOrThrow().isEmpty())LdText("Pehle Settings > Calls > IVR menu helper mein menu save karo.",13f,color=p.muted,maxLines=4)
  menus.getOrThrow().forEach{m->GhostButton(m.name,Modifier.testTag("ivr_select_${m.id}")){session=IvrMenus.Session(m);notice=""}}
 }else{val current=session!!;val next=remember(tick,current){current.next()}
  LdText(current.menu.name,18f,FontWeight.Bold,maxLines=3)
  LdText("${current.index}/${current.menu.digits.length} tones requested",12.5f,color=p.muted,maxLines=3,modifier=Modifier.testTag("ivr_progress"))
  LdText(if(next==null)"Menu complete"else"Next: $next",28f,FontWeight.Bold,maxLines=2,modifier=Modifier.testTag("ivr_next"))
  if(next!=null&&allowed)PrimaryButton("Send only $next",modifier=Modifier.testTag("ivr_send")){if(current.sendOne{IvrTone.send(target,it)}){tick++;notice="Ek tone request bheji; IVR ka jawab suno"}else notice="Tone nahi bheji. Call status check karo; rapid taps se bacho."}
  GhostButton("Reset to first (no send)",Modifier.testTag("ivr_reset")){current.reset();tick++;notice="Position reset; koi tone nahi bheji"}
  GhostButton("Choose another menu",Modifier.testTag("ivr_choose")){session=null;notice=""}
 }
 if(notice.isNotBlank())LdText(notice,12f,color=p.muted,maxLines=5)
 GhostButton("Back to call",Modifier.testTag("ivr_close"),onClick=onClose)
}
