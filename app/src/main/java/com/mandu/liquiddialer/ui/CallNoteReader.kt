package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.LdApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/** Deliberately read-only. Never edits a note or starts a call, clipboard or share flow. */
@Composable fun CallNotesReader(number:String,onBack:()->Unit){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var all by remember(number){mutableStateOf<List<Note>>(emptyList())};var busy by remember(number){mutableStateOf(true)};var error by remember(number){mutableStateOf("")}
 var query by remember(number){mutableStateOf("")};var page by remember(number,query){mutableIntStateOf(0)};var selected by remember(number){mutableStateOf<Note?>(null)}
 var find by remember(selected?.id){mutableStateOf(query)};var current by remember(find,selected?.id){mutableIntStateOf(0)};var jump by remember(selected?.id){mutableIntStateOf(0)}
 val shown=remember(all,number,query){if(CallNoteReader.eligible(number))CallNoteReader.notes(all,number,query)else emptyList()}
 val df=remember{SimpleDateFormat("d MMM yyyy, h:mm a",Locale.getDefault())}
 fun load(){busy=true;error="";scope.launch{try{val rows=withContext(Dispatchers.IO){Repo.notesChecked(ctx)};all=CallNoteReader.notes(rows,number);page=0}catch(_:Exception){all=emptyList();error="Saved notes load nahi hue. File nahi badli; dobara load karo."}finally{busy=false}}}
 fun back(){if(selected!=null)selected=null else onBack()}
 LaunchedEffect(number){if(CallNoteReader.eligible(number))load()else{busy=false;error="Sahi full phone number chahiye"}}
 BackHandler{back()}
 Column(Modifier.fillMaxWidth().heightIn(max=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*.70f).dp).testTag("call_note_reader")){
  SheetTitle(if(selected==null)"Call notes reader"else"Poora saved note")
  PrimaryButton(if(selected==null)"Notes par wapas"else"Results par wapas",modifier=Modifier.testTag("call_reader_back")){back()}
  key(selected?.id,page){val scroll=rememberScrollState();Column(Modifier.weight(1f,fill=false).fillMaxWidth().verticalScroll(scroll).testTag("call_reader_scroll")){
   LdText(number,14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("call_reader_identity"))
   LdText("Read-only · sirf is full number ke notes. Call, mic, saved text aur naya-note draft nahi badlenge.",12f,color=p.muted,maxLines=5)
   if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("call_reader_error"))
   if(busy)LdText("Saved notes loading…",13f,modifier=Modifier.testTag("call_reader_loading"))
   val note=selected
   if(note==null){
    LdField(query,{query=it.take(200)},"Saved text mein khojo",modifier=Modifier.testTag("call_reader_query"))
    if(query.isNotEmpty())GhostButton("Search saaf karo",Modifier.testTag("call_reader_clear")){query=""}
    val safePage=page.coerceAtMost(CallNoteReader.lastPage(shown.size))
    LdText(if(busy)"Saved notes loading…"else if(error.isNotEmpty())"Notes unavailable"else if(shown.isEmpty())if(query.isBlank())"Is number ka koi saved note nahi"else"Koi saved text match nahi"else "${safePage*CallNoteReader.PAGE_SIZE+1}-${minOf((safePage+1)*CallNoteReader.PAGE_SIZE,shown.size)} / ${shown.size} notes",12f,color=p.muted,maxLines=3,modifier=Modifier.testTag("call_reader_count"))
    CallNoteReader.page(shown,safePage).forEach{n->GlassCard(Modifier.fillMaxWidth().padding(vertical=5.dp)){
     LdText(df.format(Date(n.time)),11f,color=p.muted,maxLines=2)
     LdText(CallNoteReader.preview(n.text),14f,maxLines=5)
     GhostButton("Poora note padho",Modifier.testTag("call_reader_open_${n.id}")){selected=n}
    }}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
     PrimaryButton("Pichhle",enabled=safePage>0,modifier=Modifier.weight(1f).testTag("call_reader_previous")){page=safePage-1}
     PrimaryButton("Agle",enabled=safePage<CallNoteReader.lastPage(shown.size),modifier=Modifier.weight(1f).testTag("call_reader_next")){page=safePage+1}
    }
    GhostButton("Saved notes dobara load",Modifier.testTag("call_reader_reload")){if(!busy)load()}
   }else{
    LdText(df.format(Date(note.time)),12f,color=p.muted,maxLines=3)
    val matches=remember(note.text,find){NoteFind.matches(note.text,find)}
    NoteReader(note.text,find,matches,current,jump){find=it.take(200)}
   }
  }}
  selected?.let{n->if(find.isNotEmpty()){val matches=NoteFind.matches(n.text,find);NoteFindFooter(matches.size,current,{current=NoteFind.next(current,matches.size,-1);jump++},{current=NoteFind.next(current,matches.size,1);jump++},{find=""})}}
 }
}

@Composable fun CallNoteReaderPage(onBack:()->Unit){
 val p=LocalLd.current;val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();var error by remember{mutableStateOf("")}
 PageFrame("Call notes reader",onBack){
  LdText("Call chalu. Purana note poora padho.",22f,FontWeight.Bold,maxLines=4)
  LdText("Call Notes se is full number ke saved text mein search, 12-note pages, poora note aur match par Pichhla/Agla. Naya-note draft par wapas aa sakte ho.",14f,color=p.muted,maxLines=8)
  SwitchRow("Show saved-note reader in call Notes","Default OFF. Notes feature control bhi entry hide kar sakta hai. Saved notes delete nahi hote.",s.callNoteReader,last=true){scope.launch{try{LdApp.instance.repo.set(K.callNoteReader,it)}catch(_:Exception){error="Setting save nahi hui"}}}
  LdText("Read-only snapshot. Koi edit, auto-call, share ya cloud nahi. Naye saved changes ke liye Results mein dobara load karo. Phone lock ya sheet close hone par naya unsaved draft bachne ki guarantee nahi.",13f,color=p.muted,maxLines=9)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}
