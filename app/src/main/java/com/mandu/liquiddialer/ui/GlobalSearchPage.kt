package com.mandu.liquiddialer.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.AppState
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable fun GlobalSearchPage(st:AppState,act:MainActivity,onBack:()->Unit,onOpenNumber:(String)->Unit){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var notes by remember{mutableStateOf<List<Note>>(emptyList())};var lists by remember{mutableStateOf<List<CallLists.ListItem>>(emptyList())};var reminders by remember{mutableStateOf<List<CallReminders.Reminder>>(emptyList())}
 var errors by remember{mutableStateOf<List<String>>(emptyList())};var loaded by remember{mutableStateOf(false)};var revision by remember{mutableIntStateOf(0)};var busy by remember{mutableStateOf(false)}
 var query by rememberSaveable{mutableStateOf("")};var visible by remember(query){mutableIntStateOf(20)};var selected by remember{mutableStateOf<GlobalSearch.Hit?>(null)}
 LaunchedEffect(revision){
  busy=true;val failed=mutableListOf<String>()
  notes=withContext(Dispatchers.IO){try{Repo.notesChecked(ctx)}catch(_:Exception){failed+="Notes";emptyList()}}
  lists=withContext(Dispatchers.IO){try{CallLists.read(ctx)}catch(_:Exception){failed+="Call Lists";emptyList()}}
  reminders=withContext(Dispatchers.IO){try{CallReminders.all(ctx)}catch(_:Exception){failed+="Reminders";emptyList()}}
  errors=failed;loaded=true;busy=false
 }
 val contactAllowed=act.has(Manifest.permission.READ_CONTACTS)
 var found by remember{mutableStateOf<List<GlobalSearch.Hit>>(emptyList())}
 var searching by remember{mutableStateOf(false)}
 var foundQuery by remember{mutableStateOf<String?>(null)}
 LaunchedEffect(st.contacts,contactAllowed,notes,lists,reminders,query){
  searching=true
  found=withContext(Dispatchers.Default){GlobalSearch.find(query,if(contactAllowed)st.contacts else emptyList(),notes,lists,reminders)}
  foundQuery=query;searching=false
 }
 val ready=foundQuery==query && !searching && !busy
 val results=if(ready)found else emptyList()
 fun close(){selected=null;revision++}
 selected?.let {hit->when(hit.kind){
  GlobalSearch.Kind.NOTE->{val note=notes.firstOrNull{it.id.toString()==hit.id};if(note!=null){NoteDetailPage(note,onBack={close()},onSaved={close()});return}}
  GlobalSearch.Kind.LIST_ENTRY->{val list=lists.firstOrNull{it.id==hit.parentId};if(list?.entries?.any{it.id==hit.id}==true){
   CallListSessionPage(list,hit.id,busy,errors.joinToString(),onBack={close()},onCall={act.callUi(it)},onSave={eid,status,note,after->
    if(!busy){busy=true;scope.launch{try{lists=withContext(Dispatchers.IO){CallLists.followUp(ctx,list.id,eid,status,note)};after()}catch(_:Exception){errors=listOf("Save nahi hua; dobara load karo.")}finally{busy=false}}}
   });return}}
  GlobalSearch.Kind.REMINDER->{val r=reminders.firstOrNull{it.token==hit.id};if(r!=null){ReminderEditor(r,onDismiss={close()},onSaved={close()});BackHandler{close()};return}}
  GlobalSearch.Kind.CONTACT->{}
 }
 selected=null}
 val labels=mapOf(GlobalSearch.Kind.CONTACT to "Contacts",GlobalSearch.Kind.NOTE to "Private notes",GlobalSearch.Kind.LIST_ENTRY to "Call Lists",GlobalSearch.Kind.REMINDER to "Upcoming reminders")
 val df=remember{SimpleDateFormat("d MMM yyyy, HH:mm z",Locale.getDefault())}
 PageFrame("Global Search",onBack){
  LdText("Ek khoj: loaded contacts, saved notes, Call Lists aur upcoming reminders. Sab phone par; audio, WhatsApp messages ya poori call history ki khoj nahi.",12f,color=p.muted,maxLines=6)
  LdText("Contacts mein app ke loaded main numbers hain; har alternate number ka index nahi. Lists/notes ka text khojta hai, naam ya digits bhi likh sakte ho.",11.5f,color=p.muted,maxLines=5)
  Spacer(Modifier.height(8.dp))
  if(!contactAllowed)LdText("Contacts permission nahi: contacts section shamil nahi.",12f,color=p.danger,maxLines=3)
  if(errors.isNotEmpty())LdText("Ye sections load nahi hue: "+errors.joinToString()+". Baaki results hi neeche hain; data nahi badla.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("global_error"))
  Spacer(Modifier.height(8.dp))
  PrimaryButton(if(busy)"Load ho raha hai..." else "Private data dobara load karo",enabled=!busy,modifier=Modifier.testTag("global_reload")){revision++}
  Spacer(Modifier.height(8.dp))
  OutlinedTextField(query,{query=it},label={Text("Naam, number ya note ke shabd")},modifier=Modifier.fillMaxWidth().testTag("global_query"))
  if(query.isNotEmpty())TextButton(onClick={query=""},modifier=Modifier.testTag("global_clear")){Text("Khoj saaf karo")}
  LdText(if(!loaded)"Private data load ho raha hai..." else if(query.isBlank())"Khojne ke liye likho" else if(!ready)"Khoj ho rahi hai..." else "${found.size} results",13f,FontWeight.Bold,modifier=Modifier.testTag("global_count"))
  labels.forEach{(kind,label)->val all=results.filter{it.kind==kind};val shown=all.take(visible)
   if(all.isNotEmpty()){
    SecTitle("$label · ${all.size}")
    shown.forEach{hit->GlassCard(Modifier.padding(top=10.dp).testTag("global_hit_${kind.name}_${hit.id}")){
     LdText(hit.title,17f,FontWeight.Bold,maxLines=3);LdText(hit.number,13f,color=p.muted,maxLines=2)
     if(hit.detail.isNotBlank())LdText(hit.detail,12f,color=p.muted,maxLines=3)
     if(kind==GlobalSearch.Kind.REMINDER)reminders.firstOrNull{it.token==hit.id}?.let{LdText(df.format(Date(it.atMs)),12f,color=p.accent,maxLines=3)}
     PrimaryButton(if(kind==GlobalSearch.Kind.CONTACT)"Number details kholo" else "Sahi item kholo",enabled=!busy,modifier=Modifier.testTag("global_open_${kind.name}_${hit.id}")){
      if(kind==GlobalSearch.Kind.CONTACT)onOpenNumber(hit.number) else selected=hit
     }
    }}
   }
  }
  if(labels.keys.any{k->results.count{it.kind==k}>visible})PrimaryButton("Har section mein 20 aur"){visible+=20}
  if(loaded && ready && query.isNotBlank() && results.isEmpty())LdText("Load hue sections mein match nahi. Naam, digits ya shabd badlo.",13f,color=p.muted,maxLines=4)
 }
}
