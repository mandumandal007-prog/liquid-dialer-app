package com.mandu.liquiddialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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

/** Nested in the existing scroll host, both before and during calls. */
@Composable fun ChecklistTemplatesWorkspace(number:String,onBack:()->Unit,onApplied:(List<CallChecklist.Item>)->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var saved by remember(number){mutableStateOf<List<ChecklistTemplates.Template>>(emptyList())};var existing by remember(number){mutableStateOf<List<CallChecklist.Item>>(emptyList())}
 var loaded by remember(number){mutableStateOf(false)};var busy by remember(number){mutableStateOf(false)};var error by remember(number){mutableStateOf("")}
 var selected by remember(number){mutableStateOf<ChecklistTemplates.Template?>(null)};var query by remember(number){mutableStateOf("")}
 var editing by remember(number){mutableStateOf(false)};var editId by remember(number){mutableStateOf<String?>(null)};var title by remember(number){mutableStateOf("")};var body by remember(number){mutableStateOf("")};var original by remember(number){mutableStateOf("" to "")}
 var copiedTopics by remember(number){mutableStateOf<List<String>?>(null)}
 var discard by remember(number){mutableStateOf(false)};var delete by remember(number){mutableStateOf<ChecklistTemplates.Template?>(null)};var applied by remember(number){mutableStateOf("")}
 fun load(){if(busy)return;busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){ChecklistTemplates.read(ctx) to CallChecklist.read(ctx,number)};saved=next.first;existing=next.second;loaded=true;error=""}catch(e:CancellationException){throw e}catch(_:Exception){loaded=false;error="Templates/checklist load nahi hui. Saved files nahi badli. Dobara load karo."}finally{busy=false}}}
 LaunchedEffect(number){load()}
 fun back(){if(busy)return;if(editing){if(original!=(title to body))discard=true else editing=false}else if(selected!=null){selected=null;error=""}else onBack()}
 BackHandler{back()}
 fun edit(t:ChecklistTemplates.Template?=null,fromCurrent:Boolean=false){editId=t?.id;title=t?.title?:"";body=t?.topics?.joinToString("\n\n")?:if(fromCurrent)existing.map{it.text}.distinctBy{ChecklistTemplates.normalized(it)}.joinToString("\n\n")else "";copiedTopics=if(fromCurrent)existing.map{it.text}.distinctBy{ChecklistTemplates.normalized(it)}else t?.topics;original=title to body;editing=true;error="";applied=""}
 val content:@Composable ColumnScope.()->Unit={
 if(inSheet)SheetTitle("Checklist templates")
 PrimaryButton(if(editing||selected!=null)"Templates par wapas"else "Checklist par wapas",enabled=!busy,modifier=Modifier.testTag("tpl_back")){back()}
 LdText(number,14f,FontWeight.Bold,maxLines=4)
 LdText("Reusable topics, phone par private. Koi call/message nahi. Template lagane par sirf naye topics Pending judenge; purane topics aur ticks rahenge.",13f,color=p.muted,maxLines=8)
 LdText("Templates mein number ya ticks save nahi hote. Unencrypted local storage; app backup mein nahi. Uninstall/data clear se mitenge. 20 custom templates tak.",12f,color=p.muted,maxLines=8)
 if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=8,modifier=Modifier.testTag("tpl_error"))
 if(applied.isNotEmpty())LdText(applied,14f,FontWeight.Bold,maxLines=6,modifier=Modifier.testTag("tpl_applied"))
 if(!loaded){PrimaryButton("Dobara load karo",enabled=!busy,modifier=Modifier.testTag("tpl_retry")){load()}}
 else if(editing){
  SecTitle(if(editId==null)"Naya template"else "Template badlo")
  LdField(title,{if(!busy)title=it},"Template ka naam",modifier=Modifier.testTag("tpl_title"))
  LdField(body,{if(!busy)body=it},"Topics ke beech ek khali line",minLines=5,modifier=Modifier.testTag("tpl_body"))
  LdText("Naam 1-60 letters. 1-30 topics, har topic 1-250 letters. Topics ko ek khali line se alag karo; topic ke andar single newline reh sakti hai. Duplicate topics mat rakho.",12f,color=p.muted,maxLines=6)
  val valid=title.trim().length in 1..60&&runCatching{if(body==original.second&&copiedTopics!=null)ChecklistTemplates.topics(copiedTopics!!)else ChecklistTemplates.lines(body)}.isSuccess
  PrimaryButton("Template save karo",enabled=!busy&&valid,modifier=Modifier.testTag("tpl_save")){busy=true;val name=title;val values=if(body==original.second&&copiedTopics!=null)copiedTopics!!else ChecklistTemplates.lines(body);val id=editId;scope.launch{try{saved=withContext(Dispatchers.IO){ChecklistTemplates.save(ctx,id,name,values)};editing=false;selected=null;error="";applied="Template save hua. Kisi number ki checklist nahi badli."}catch(e:CancellationException){throw e}catch(e:Exception){error=(e as? IllegalArgumentException)?.message?:"Template save nahi hua. Purani file nahi badli."}finally{busy=false}}}
 }else if(selected!=null){
  val t=selected!!;val missing=ChecklistTemplates.missing(existing,t.topics);val space=CallChecklist.MAX_ITEMS-existing.size
  SecTitle(t.title)
  LdText("${missing.size} naye topics · ${t.topics.size-missing.size} pehle se maujood · $space jagah",14f,FontWeight.Bold,maxLines=5,modifier=Modifier.testTag("tpl_preview_counts"))
  LdText("Yeh preview hai. Judne se pehle checklist dobara padhi jayegi. Jagah kam hui to kuch nahi judega.",12f,color=p.muted,maxLines=5)
  PrimaryButton(if(missing.isEmpty())"Sab topics pehle se hain"else "${missing.size} topics Pending jodo",enabled=!busy&&missing.isNotEmpty()&&missing.size<=space,modifier=Modifier.testTag("tpl_apply")){
   busy=true;scope.launch{try{val next=withContext(Dispatchers.IO){val now=if(t.builtIn)t else ChecklistTemplates.read(ctx).firstOrNull{it.id==t.id}?:error("Template ab nahi mila");require(now==t){"Template badal gaya; wapas jaakar dobara kholo"};CallChecklist.appendTopics(ctx,number,t.topics)};existing=next;error="";applied="Checklist update hui. Purane topics/ticks rahe; naye topics Pending hain.";onApplied(next)}catch(e:CancellationException){throw e}catch(e:Exception){error=(e as? IllegalArgumentException)?.message?:"Topics nahi jude. Dobara load karo; saved data nahi badla."}finally{busy=false}}
  }
  if(missing.size>space)LdText("Jagah kam hai. Pehle checklist mein jagah banao; koi topic truncate ya replace nahi hoga.",13f,color=p.danger,maxLines=5)
  t.topics.forEachIndexed{i,text->GlassCard(Modifier.padding(top=8.dp).testTag("tpl_topic_$i")){LdText(if(missing.any{ChecklistTemplates.normalized(it)==ChecklistTemplates.normalized(text)})"Naya · Pending judega"else "Pehle se hai · nahi badlega",11f,color=p.muted,maxLines=3);LdText(text,15f,maxLines=Int.MAX_VALUE)}}
 }else{
  SecTitle("Apne templates")
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   PrimaryButton("Naya banao",enabled=!busy&&saved.size<ChecklistTemplates.MAX_TEMPLATES,modifier=Modifier.weight(1f).testTag("tpl_new")){edit()}
   PrimaryButton("Is checklist se",enabled=!busy&&existing.isNotEmpty()&&saved.size<ChecklistTemplates.MAX_TEMPLATES,modifier=Modifier.weight(1f).testTag("tpl_from_current")){edit(fromCurrent=true)}
  }
  LdField(query,{query=it},"Template / topic khojo",modifier=Modifier.testTag("tpl_search"))
  val all=saved+ChecklistTemplates.starters;val filtered=all.filter{query.isBlank()||(it.title+"\n"+it.topics.joinToString("\n")).contains(query.trim(),true)}
  if(filtered.isEmpty())LdText("Koi matching template nahi mila.",13f,color=p.muted,maxLines=4)
  filtered.forEach{t->GlassCard(Modifier.padding(top=10.dp).testTag("tpl_card_${t.id}")){
   LdText(t.title,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
   LdText("${t.topics.size} topics · ${if(t.builtIn)"Starter, badalne ke liye naya banao"else "Tumhara saved template"}",12f,color=p.muted,maxLines=5)
   PrimaryButton("Preview dekho",enabled=!busy,modifier=Modifier.testTag("tpl_open_${t.id}")){selected=t;applied="";error=""}
   if(!t.builtIn)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Badlo",enabled=!busy,modifier=Modifier.weight(1f).testTag("tpl_edit_${t.id}")){edit(t)}
    PrimaryButton("Mitao",enabled=!busy,modifier=Modifier.weight(1f).testTag("tpl_delete_${t.id}")){delete=t}
   }
  }}
 }
 if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Template save nahi hua")},text={Text("Likha hua chhodna hai? Saved template nahi badlega.")},confirmButton={TextButton(onClick={discard=false;editing=false},modifier=Modifier.testTag("tpl_discard")){Text("Chhod do")}},dismissButton={TextButton(onClick={discard=false},modifier=Modifier.testTag("tpl_keep")){Text("Likhte raho")}})
 delete?.let{t->AlertDialog(onDismissRequest={if(!busy)delete=null},title={Text("Template mitayein?")},text={Text("${t.title}: sirf template mitega. Kisi number ke pehle se jude topics/ticks nahi mitenge. Wapas nahi la sakte.")},confirmButton={TextButton(enabled=!busy,onClick={busy=true;scope.launch{try{saved=withContext(Dispatchers.IO){ChecklistTemplates.remove(ctx,t.id)};delete=null;error=""}catch(e:CancellationException){throw e}catch(_:Exception){error="Template nahi mita. Dobara load karo."}finally{busy=false}}},modifier=Modifier.testTag("tpl_delete_confirm")){Text("Mitao")}},dismissButton={TextButton(enabled=!busy,onClick={delete=null}){Text("Rehne do")}})}
 }
 if(inSheet)Column(content=content)else PageFrame("Checklist templates",{back()},content=content)
}
