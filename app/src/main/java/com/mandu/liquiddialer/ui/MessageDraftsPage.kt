package com.mandu.liquiddialer.ui
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable fun MessageDraftsPage(onBack:()->Unit){
 val p=LocalLd.current;val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope()
 var libraryBack by remember{mutableStateOf<(() -> Unit)?>(null)}
 var number by remember{mutableStateOf("")};var opened by remember{mutableStateOf<String?>(null)};var library by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 if(opened!=null){key(opened){MessageDraftsWorkspace(opened!!,{opened=null})};return}
 if(library){PageFrame("Saved message drafts",{libraryBack?.invoke()}){MessageDraftLibrary(null,{library=false},{libraryBack=it})};return}
 PageFrame("Message drafts",onBack){
  LdText("Pehle likho, phir khud bhejo",21f,FontWeight.Bold,maxLines=3)
  LdText("Reusable ordinary text. Preview/edit ke baad SMS ya WhatsApp mein compose kholo. App apne aap message nahi bhejta aur call reject nahi karta.",13f,color=p.muted,maxLines=8)
  SwitchRow("Show in call Notes","Default OFF. Entry hide karne se saved drafts delete nahi hote.",s.messageDrafts,last=true){scope.launch{try{LdApp.instance.repo.set(K.messageDrafts,it)}catch(_:Exception){error="Setting save nahi hui"}}}
  PrimaryButton("Manage saved drafts",modifier=Modifier.testTag("drafts_library")){library=true}
  LdText("Phone par plain local storage; encrypted vault nahi. Password, OTP, card details ya secrets yahan mat rakho. Backup mein include nahi.",12f,color=p.danger,maxLines=7)
  SecTitle("Kis number ke liye?")
  LdField(number,{number=it.take(40)},"Full phone number",modifier=Modifier.testTag("drafts_target_input"))
  PrimaryButton("Open message preview",modifier=Modifier.testTag("drafts_open")){runCatching{MessageDrafts.target(number)}.fold({opened=it;error=""},{error="Sahi full number likho; country code ke saath behtar."})}
  if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=4,modifier=Modifier.testTag("drafts_error"))
 }
}

@Composable private fun MessageDraftLibrary(onUse:((String)->Unit)?,onBack:()->Unit,onBindBack:((()->Unit)->Unit)={}){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var snap by remember{mutableStateOf<MessageDrafts.Snapshot?>(null)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var query by remember{mutableStateOf("")}
 var editId by remember{mutableStateOf<String?>(null)};var editing by remember{mutableStateOf(false)};var title by remember{mutableStateOf("")};var body by remember{mutableStateOf("")};var originalTitle by remember{mutableStateOf("")};var originalBody by remember{mutableStateOf("")};var discard by remember{mutableStateOf(false)};var remove by remember{mutableStateOf<String?>(null)}
 fun dirty()=editing&&(title!=originalTitle||body!=originalBody)
 fun back(){if(dirty())discard=true else if(editing)editing=false else onBack()}
 fun load(){busy=true;scope.launch{runCatching{withContext(Dispatchers.IO){MessageDrafts.read(ctx)}}.fold({snap=it;error=""},{snap=null;error="Saved drafts load nahi hue. Data preserve hai; overwrite/delete nahi hoga."});busy=false}}
 fun commit(rows:List<MessageDrafts.Draft>){val expected=snap?.raw?:return;busy=true;scope.launch{runCatching{withContext(Dispatchers.IO){MessageDrafts.save(ctx,expected,rows)}}.fold({snap=it;editing=false;remove=null;error=""},{error=it.message?:"Save nahi hua"});busy=false}}
 SideEffect{onBindBack{back()}}
 LaunchedEffect(Unit){load()};BackHandler{back()}
 PrimaryButton(if(editing)"Draft editor se wapas"else"Preview par wapas",enabled=!busy,modifier=Modifier.testTag("draft_library_back")){back()}
 if(discard){GlassCard{LdText("Unsaved changes hatao?",17f,FontWeight.Bold);PrimaryButton("Discard draft edits",modifier=Modifier.testTag("draft_edit_discard")){discard=false;editing=false};GhostButton("Editing jaari rakho"){discard=false}}}
 if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=7,modifier=Modifier.testTag("draft_library_error"))
 if(busy)LdText("Load/save ho raha hai...",13f,color=p.muted)
 val current=snap
 if(current==null){PrimaryButton("Reload saved drafts",enabled=!busy,modifier=Modifier.testTag("draft_library_reload")){load()};return}
 if(editing){
  SecTitle(if(editId==null)"New saved draft"else"Edit saved draft")
  LdField(title,{title=it.take(50)},"Draft title",modifier=Modifier.testTag("draft_title"))
  LdField(body,{body=it.take(MessageDrafts.MAX_BODY)},"Saved message text",minLines=4,modifier=Modifier.testTag("draft_body"))
  LdText("${body.length}/2000 · Plain local text, not a sent message",12f,color=p.muted,maxLines=4)
  PrimaryButton("Save reusable draft",enabled=!busy&&runCatching{MessageDrafts.Draft(editId?:"new",title,body).validate()}.isSuccess,modifier=Modifier.testTag("draft_save")){
   val row=MessageDrafts.Draft(editId?:UUID.randomUUID().toString(),title,body)
   commit(if(editId==null)current.rows+row else current.rows.map{if(it.id==editId)row else it})
  };return
 }
 GhostButton("Reload saved drafts",Modifier.testTag("draft_reload")){if(!busy)load()}
 LdText("${current.rows.size}/${MessageDrafts.MAX} saved drafts",13f,color=p.muted,modifier=Modifier.testTag("draft_count"))
 LdField(query,{query=it},"Draft title/text mein khojo",modifier=Modifier.testTag("draft_search"))
 PrimaryButton("New reusable draft",enabled=!busy&&current.rows.size<MessageDrafts.MAX,modifier=Modifier.testTag("draft_add")){editId=null;title="";body="";originalTitle="";originalBody="";editing=true}
 if(current.rows.isEmpty())LdText("Abhi koi saved draft nahi. Naya ordinary message text jodo.",13f,color=p.muted,maxLines=5)
 MessageDrafts.matching(current.rows,query).forEach{row->key(row.id){GlassCard(Modifier.padding(top=10.dp).testTag("saved_draft_${row.id}")){
  LdText(row.title,17f,FontWeight.Bold,maxLines=Int.MAX_VALUE);LdText(row.body,14f,maxLines=Int.MAX_VALUE)
  if(onUse!=null)PrimaryButton("Use in preview",enabled=!busy,modifier=Modifier.testTag("draft_use_${row.id}")){onUse(row.body)}
  GhostButton("Edit",Modifier.testTag("draft_edit_${row.id}")){editId=row.id;title=row.title;body=row.body;originalTitle=title;originalBody=body;editing=true}
  GhostButton("Delete saved draft",Modifier.testTag("draft_delete_${row.id}")){remove=row.id}
  if(remove==row.id){LdText("Sirf saved draft delete hoga; messages nahi.",12f,color=p.danger,maxLines=4);PrimaryButton("Confirm draft delete",danger=true,enabled=!busy,modifier=Modifier.testTag("draft_delete_confirm")){commit(current.rows.filter{it.id!=row.id})};GhostButton("Cancel delete"){remove=null}}
 }}}
}

@Composable fun MessageDraftsWorkspace(number:String,onBack:()->Unit,inSheet:Boolean=false,onCompose:((android.content.Intent)->Unit)?=null){
 val ctx=LocalContext.current;val p=LocalLd.current
 val valid=remember(number){runCatching{MessageDrafts.target(number)}.isSuccess}
 var libraryBack by remember(number){mutableStateOf<(() -> Unit)?>(null)}
 var text by remember(number){mutableStateOf("")};var library by remember(number){mutableStateOf(false)};var replacement by remember(number){mutableStateOf<String?>(null)};var discard by remember(number){mutableStateOf(false)};var error by remember(number){mutableStateOf("")};var status by remember(number){mutableStateOf("")}
 fun back(){if(library){libraryBack?.invoke();return};if(text.isNotEmpty())discard=true else onBack()}
 fun open(whatsapp:Boolean){if(!valid)return;runCatching{if(whatsapp)MessageDrafts.whatsapp(number,text)else MessageDrafts.sms(number,text)}.fold({intent->try{if(onCompose==null)ctx.startActivity(intent)else onCompose(intent);status="Compose app khola. Message bhejne ke liye wahan review/send karo.";error=""}catch(_:Exception){error="Compose app nahi khul paya. Text yahin hai; Copy use kar sakte ho."}},{error=it.message?:"Number/text check karo"})}
 BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  if(library){MessageDraftLibrary({next->if(text.isNotEmpty()&&text!=next){replacement=next;library=false}else{text=next;library=false;status=""}},{library=false},{libraryBack=it})}
  else{
   LdText("Recipient",12f,color=p.muted);LdText(number,18f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("draft_recipient"))
   LdText("Sirf preview. Koi auto-send, call reject ya call change nahi. SMS provider charges ya WhatsApp data plan lag sakta hai; send un apps mein tum karoge.",12f,color=p.muted,maxLines=8)
   if(!valid)LdText("Recipient valid nahi. Compose/copy band hain.",13f,color=p.danger,maxLines=4)
   PrimaryButton("Choose saved draft",enabled=valid,modifier=Modifier.testTag("draft_choose")){library=true}
   replacement?.let{next->GlassCard{LdText("Current preview replace karo?",16f,FontWeight.Bold);PrimaryButton("Replace preview text",modifier=Modifier.testTag("draft_replace")){text=next;replacement=null;status=""};GhostButton("Keep current text"){replacement=null}}}
   LdField(text,{text=it.take(MessageDrafts.MAX_BODY);status=""},"Message preview/edit",minLines=5,modifier=Modifier.testTag("draft_preview"))
   LdText("${text.length}/2000 · Preview is session-only; saved draft is unchanged.",12f,color=p.muted,maxLines=4)
   val ready=valid&&runCatching{MessageDrafts.validateBody(text)}.isSuccess
   PrimaryButton("Copy preview text",enabled=ready,modifier=Modifier.testTag("draft_copy")){ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("message draft",text));status="Text clipboard par copy hua. Bheja nahi."}
   PrimaryButton("Open SMS compose",enabled=ready,modifier=Modifier.testTag("draft_sms")){open(false)}
   PrimaryButton("Open WhatsApp compose",enabled=ready&&WhatsAppLink.url(number)!=null,modifier=Modifier.testTag("draft_whatsapp")){open(true)}
   if(status.isNotBlank())LdText(status,13f,color=p.muted,maxLines=5,modifier=Modifier.testTag("draft_status"))
   if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("draft_compose_error"))
   if(discard){GlassCard{LdText("Unsaved preview text chhod do?",16f,FontWeight.Bold);PrimaryButton("Discard preview and go back",modifier=Modifier.testTag("draft_discard")){text="";discard=false;onBack()};GhostButton("Preview par raho"){discard=false}}}
  }
 }
 if(inSheet)Column{SheetTitle("Message drafts");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("draft_back")){back()};content()}
 else PageFrame("Message preview",{back()},content=content)
}
