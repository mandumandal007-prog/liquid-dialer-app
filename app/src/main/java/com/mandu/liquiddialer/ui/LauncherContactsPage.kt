package com.mandu.liquiddialer.ui
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun LauncherContactsPage(act:MainActivity?,onBack:()->Unit,contacts:List<Contact> = act?.state?.contacts.orEmpty()){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var saved by remember{mutableStateOf<LauncherContacts.Saved?>(null)};var enabled by remember{mutableStateOf(false)};var rows by remember{mutableStateOf(listOf(LauncherContacts.Target("",""),LauncherContacts.Target("",""),LauncherContacts.Target("","")))}
 var previewVersion by remember{mutableIntStateOf(0)}
 var photos by remember{mutableStateOf(false)}
 var previews by remember{mutableStateOf<List<Pair<LauncherContacts.Target,LauncherContactPhotos.Result>>>(emptyList())}
 var slot by remember{mutableIntStateOf(0)}
 var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")};var picker by remember{mutableStateOf<Int?>(null)};var query by remember{mutableStateOf("")};var discard by remember{mutableStateOf("")}
 fun config()=LauncherContacts.Config(enabled,rows.filter{it.name.isNotBlank()||it.number.isNotBlank()},photos)
 val dirty=saved!=null&&config()!=saved!!.config
 fun load(){busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){LauncherContacts.read(ctx)};saved=next;enabled=next.config.enabled;photos=next.config.photos;rows=next.config.targets+List(3-next.config.targets.size){LauncherContacts.Target("","")};error="";status="Saved settings loaded";previewVersion++}catch(_:Exception){saved=null;error="Shortcut settings load nahi hui. Saved data preserve hai."}finally{busy=false}}}
 fun back(){if(busy)return;if(picker!=null)picker=null else if(dirty)discard="back"else onBack()}
 fun publish(save:Boolean){val expected=saved?.raw?:return;val next=config();busy=true;scope.launch{try{val result=withContext(Dispatchers.IO){if(save)LauncherContacts.save(ctx,expected,next);LauncherContacts.publish(ctx)};saved=withContext(Dispatchers.IO){LauncherContacts.read(ctx)};previewVersion++;status=if(!result.enabled)"Icon shortcuts OFF. Launcher update requested."else"${result.requested} shortcuts Android ko diye. Home screen par icon ko tap-and-hold karo. Launcher kitne dikhaye, us par nirbhar.";error=""}catch(e:Exception){error=e.message?:"Update nahi hua. Saved settings reload karke Refresh try karo."}finally{busy=false}}}
 LaunchedEffect(Unit){load()};BackHandler{back()}
 LaunchedEffect(rows,photos,enabled,previewVersion){val valid=rows.filter{runCatching{it.validate()}.isSuccess};previews=emptyList();if(enabled){kotlinx.coroutines.delay(250);previews=withContext(Dispatchers.IO){valid.map{it to LauncherContactPhotos.icon(ctx,it,photos)}}}}
 PageFrame("Icon long-press shortcuts",{back()}){
  if(picker!=null){
   PrimaryButton("Selection par wapas",modifier=Modifier.testTag("launcher_picker_back")){picker=null}
   LdField(query,{query=it},"Contact name/number khojo",modifier=Modifier.testTag("launcher_search"))
   if(act!=null&&!act.has(Manifest.permission.READ_CONTACTS))PrimaryButton("Contacts permission do"){act.askPermission(Manifest.permission.READ_CONTACTS)}
   val matches=contacts.filter{it.name.contains(query,true)||it.number.contains(query,true)}
   LdText("${matches.size} matches · first 60 shown. Narrow search if needed.",12f,color=p.muted,maxLines=4)
   matches.take(60).forEachIndexed{i,c->GlassCard(Modifier.padding(top=8.dp)){
    LdText(c.name,16f,FontWeight.Bold,maxLines=4);LdText(c.number,13f,color=p.muted,maxLines=3)
    PrimaryButton("Use this number",modifier=Modifier.testTag("launcher_pick_$i")){val slot=picker!!;rows=rows.mapIndexed{j,r->if(j==slot)LauncherContacts.Target(c.name.take(40),c.number)else r};picker=null;status=""}
   }}
   if(matches.isEmpty())LdText("Contacts nahi mile. Wapas jaakar naam/number manually bhar sakte ho.",13f,color=p.muted,maxLines=5)
  }else{
   LdText("Home icon se apne log, seedhe keypad par",21f,FontWeight.Bold,maxLines=4)
   LdText("New contact + tumhare chune hue 3 numbers. Contact shortcut tap karne par number keypad mein bharega; call tum khud dabaaoge. New contact se contact editor khulega.",13f,color=p.muted,maxLines=7)
   if(saved!=null){
    SwitchRow("Icon shortcuts ON","Default OFF. Names/numbers launcher ko dikhte hain. Save & apply zaroor dabao.",enabled,last=true){if(!busy)enabled=it}
    SwitchRow("Contact photos on icon menu","Default OFF. Save & apply se contact photo launcher ko jaati hai. Missing photo = initial.",photos,last=true){if(!busy)photos=it}
    if(photos&&act!=null&&!act.has(Manifest.permission.READ_CONTACTS))PrimaryButton("Allow contact photos"){act.askPermission(Manifest.permission.READ_CONTACTS)}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){(0..2).forEach{i->GhostButton((if(slot==i)"✓ "else"")+"Slot ${i+1}",Modifier.weight(1f).testTag("launcher_tab_$i")){if(!busy)slot=i}}}
    listOf(slot to rows[slot]).forEach{(i,row)->GlassCard(Modifier.padding(top=10.dp).testTag("launcher_slot_$i")){
     LdText("Shortcut ${i+1}",15f,FontWeight.Bold)
     LdField(row.name,{v->rows=rows.mapIndexed{j,r->if(j==i)r.copy(name=v.take(40))else r}},"Name (max 40)",modifier=Modifier.testTag("launcher_name_$i"))
     LdField(row.number,{v->rows=rows.mapIndexed{j,r->if(j==i)r.copy(number=v.take(40))else r}},"Full phone number",modifier=Modifier.testTag("launcher_number_$i"))
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){GhostButton("Pick contact",Modifier.weight(1f).testTag("launcher_choose_$i")){if(!busy){picker=i;query=""}};GhostButton("Clear slot",Modifier.weight(1f).testTag("launcher_clear_$i")){if(!busy)rows=rows.mapIndexed{j,r->if(j==i)LauncherContacts.Target("","")else r}}}
    }}
    LdText(if(dirty)"Unsaved shortcut settings"else"Saved shortcut settings",12f,color=p.muted,modifier=Modifier.testTag("launcher_dirty"))
    PrimaryButton("Save & apply to icon",enabled=!busy&&runCatching{config().validate()}.isSuccess,modifier=Modifier.testTag("launcher_save")){publish(true)}
    PrimaryButton("Refresh saved shortcuts",enabled=!busy&&!dirty,modifier=Modifier.testTag("launcher_refresh")){publish(false)}
    SecTitle("Launcher menu preview")
    GlassCard(Modifier.testTag("launcher_preview")){
     LdText(if(enabled)"New contact"else"Shortcuts OFF",16f,FontWeight.Bold)
     if(enabled)previews.forEach{(target,icon)->Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Image(icon.bitmap.asImageBitmap(),if(icon.photo)"Contact photo"else"Initial icon",Modifier.size(42.dp).testTag("launcher_icon_"+LauncherContacts.id(target)));Column(Modifier.weight(1f)){LdText(target.name+" · "+target.number,14f,maxLines=4);LdText(icon.detail,11f,color=p.muted,maxLines=3)}}}
    }
   }
   if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=7,modifier=Modifier.testTag("launcher_error"))
   if(status.isNotBlank())LdText(status,13f,color=p.muted,maxLines=7,modifier=Modifier.testTag("launcher_status"))
   GhostButton("Reload saved settings",Modifier.testTag("launcher_reload")){if(!busy){if(dirty)discard="reload"else load()}}
   LdText("Launcher shortcut count/order/icons depend on your home app. Android may allow fewer than 4. Selected names/numbers are snapshots. Photos refresh only on Save/apply or Refresh saved shortcuts, not automatically; if permission/photo is gone the next refresh uses initials. Turning photos OFF needs Save & apply; already pinned copies may be cached by the launcher. A dragged pinned shortcut can remain on Home, but OFF/removed shortcuts won't fill a number. Existing direct-call pinned buttons are separate.",12f,color=p.muted,maxLines=10)
   LdText("The launcher's Info / Remove / Uninstall buttons are outside this app's control. This setting cannot hide Uninstall or turn this APK into a system app.",12f,color=p.muted,maxLines=6)
   if(discard.isNotBlank())GlassCard{LdText("Unsaved changes chhod do?",16f,FontWeight.Bold);PrimaryButton("Discard shortcut edits",modifier=Modifier.testTag("launcher_discard")){val action=discard;discard="";if(action=="reload")load()else onBack()};GhostButton("Keep editing"){discard=""}}
  }
 }
}
