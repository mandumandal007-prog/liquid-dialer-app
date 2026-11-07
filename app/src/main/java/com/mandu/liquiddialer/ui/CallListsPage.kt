package com.mandu.liquiddialer.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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

@Composable
fun CallListsPage(st:AppState,act:MainActivity,onBack:()->Unit) {
    val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
    var lists by remember {mutableStateOf<List<CallLists.ListItem>>(emptyList())}
    var loaded by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var notice by remember {mutableStateOf("")}
    var selected by remember {mutableStateOf<String?>(null)}
    var sessionId by remember {mutableStateOf<String?>(null)}
    var reportText by remember {mutableStateOf<String?>(null)}
    val reportFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->
        val text=reportText;reportText=null
        if(uri!=null && text!=null){busy=true;scope.launch {
            val ok=try{withContext(Dispatchers.IO){ctx.contentResolver.openOutputStream(uri,"wt")?.use {it.write(text.toByteArray(Charsets.UTF_8))} ?: error("No output")};true}catch(_:Exception){false}
            busy=false;if(ok)notice="Progress report save hua" else error="Report save nahi hua; adhuri file ko use mat karo"
        }}
    }
    var mode by remember {mutableStateOf("")}
    var name by remember {mutableStateOf("")}
    var number by remember {mutableStateOf("")}
    var query by remember {mutableStateOf("")}
    var visible by remember(selected) {mutableIntStateOf(20)}
    var filter by remember(selected) {mutableStateOf("Pending")}
    var confirm by remember {mutableStateOf<Pair<String,String>?>(null)}
    var batchText by remember {mutableStateOf("")}
    var batchReview by remember {mutableStateOf<CallListTransfer.Batch?>(null)}
    var importText by remember {mutableStateOf<String?>(null)}
    var importPreview by remember {mutableStateOf<List<CallLists.ListItem>?>(null)}
    var exportText by remember {mutableStateOf<String?>(null)}
    val exportFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        val text=exportText;exportText=null
        if(uri!=null && text!=null){busy=true;scope.launch {
            val ok=try{withContext(Dispatchers.IO){ctx.contentResolver.openOutputStream(uri,"wt")?.use {it.write(text.toByteArray(Charsets.UTF_8))} ?: error("No output");true}}catch(_:Exception){false}
            busy=false;if(ok)notice="Call Lists backup save hua" else error="Backup save nahi hua; adhuri file ko use mat karo"
        }}
    }
    val importFile=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){busy=true;error="";scope.launch {
            try {val text=withContext(Dispatchers.IO){ctx.contentResolver.openInputStream(uri)?.use {CallListTransfer.read(it)} ?: error("No input")}
                val preview=withContext(Dispatchers.IO){CallListTransfer.preview(text)};importText=text;importPreview=preview;mode="import-review"
            }catch(_:Exception){error="Backup khul nahi raha ya valid Call Lists file nahi hai. Koi saved list nahi badli."}
            finally{busy=false}
        }}
    }
    val current=lists.firstOrNull {it.id==selected}
    fun update(action:()->List<CallLists.ListItem>,after:()->Unit={}) {
        if(busy) return
        busy=true;error=""
        scope.launch {
            try {lists=withContext(Dispatchers.IO){action()};after()}
            catch(e:Exception){error=(e as? IllegalArgumentException)?.message ?: "Lists save nahi hui. Dobara try karo; purana data nahi hataya."}
            finally {busy=false}
        }
    }
    LaunchedEffect(Unit) {
        try {lists=withContext(Dispatchers.IO){CallLists.read(ctx)};loaded=true}
        catch(_:Exception){error="Saved lists khul nahi rahi. Data ko overwrite nahi kiya gaya."}
    }
    if(current!=null && sessionId!=null) {
        CallListSessionPage(current,sessionId!!,busy,error,onBack={sessionId=null;error=""},onCall={act.callUi(it)},
            onSave={eid,outcome,note,after->update({CallLists.followUp(ctx,current.id,eid,outcome,note)},after)})
        return
    }
    fun back() {if(busy)return; when {mode.isNotEmpty()->{mode="";batchReview=null;importPreview=null;importText=null};selected!=null->selected=null;else->onBack()}}
    PageFrame(current?.title ?: "Call Lists",{back()}) {
        LdText("Apni calling lists banao, ek-ek karke khud call karo aur Done lagao. Call dabane se entry apne aap Done nahi hogi. Lists isi app mein save hoti hain.",12f,color=p.muted,maxLines=6)
        Spacer(Modifier.height(12.dp))
        if(notice.isNotEmpty())LdText(notice,13f,color=p.accent,maxLines=3,modifier=Modifier.testTag("lists_notice"))
        if(error.isNotEmpty()) LdText(error,13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("lists_error"))
        if(!loaded) {
            LdText("Lists load ho rahi hain. Load fail ho toh wapas jaakar dobara kholo.",13f,color=p.muted,maxLines=4)
        } else if(mode=="import-review") {
            val preview=importPreview.orEmpty()
            LdText("Backup preview",18f,FontWeight.Bold)
            LdText("${preview.size} lists · ${preview.sumOf {it.entries.size}} entries · ${preview.sumOf {l->l.entries.count {it.done}}} Done",13f,color=p.accent,maxLines=3)
            LdText("Nayi copies judengi. Maujooda lists/progress nahi badlenge. Ek file dobara import karne par phir copies banengi. Phone contacts ya call history nahi badlegi. Koi call nahi lagega.",12f,color=p.muted,maxLines=7)
            preview.forEach {l->
                LdText("${l.title}: ${l.entries.size} entries · ${l.entries.count {it.done}} Done",14f,FontWeight.Bold,maxLines=3,modifier=Modifier.padding(top=10.dp))
                l.entries.take(5).forEach {e->LdText(e.number+if(e.name.isBlank())"" else " · "+e.name,12f,color=p.muted,maxLines=3)}
                if(l.entries.size>5)LdText("Aur ${l.entries.size-5} entries backup mein",12f,color=p.muted,maxLines=2)
            }
            if(lists.size+preview.size>20)LdText("20 lists ki limit: import se pehle jagah banao.",13f,color=p.danger,maxLines=3)
            PrimaryButton("Copies import karo",enabled=!busy && preview.isNotEmpty() && lists.size+preview.size<=20,modifier=Modifier.testTag("lists_import_confirm")) {
                val text=importText ?: return@PrimaryButton
                update({CallLists.importCopies(ctx,text)}){mode="";importPreview=null;importText=null;selected=null}
            }
            PrimaryButton("Rehne do",enabled=!busy,modifier=Modifier.testTag("lists_import_cancel")){mode="";importPreview=null;importText=null}
        } else if(current==null) {
            if(mode!="create") PrimaryButton("Nayi list banao",enabled=!busy,modifier=Modifier.testTag("lists_create")){mode="create";name=""}
            if(mode=="create") {
                ListField(name,{name=it.take(60)},"List ka naam", "list_title")
                PrimaryButton("List save karo",enabled=!busy && name.isNotBlank(),modifier=Modifier.testTag("list_save")) {
                    val savedName=name;update({CallLists.create(ctx,savedName)}) {selected=lists.last().id;mode="";name=""}
                }
                PrimaryButton("Rehne do",enabled=!busy){mode=""}
            }
            if(mode!="create") {
                LdText("Apni JSON backup file mein naam, numbers, outcomes aur private notes honge. Private jagah save karo. App automatic cloud backup nahi banata.",12f,color=p.muted,maxLines=5,modifier=Modifier.padding(top=10.dp))
                PrimaryButton("Backup file save karo",enabled=!busy && lists.isNotEmpty(),modifier=Modifier.testTag("lists_export")) {
                    try{exportText=CallListTransfer.backup(lists);exportFile.launch("call-lists-backup.json")}catch(_:Exception){error="Backup taiyar nahi hua. Koi saved list nahi badli."}
                }
                PrimaryButton("Backup file se import",enabled=!busy,modifier=Modifier.testTag("lists_import")){importFile.launch(arrayOf("application/json","text/plain"))}
            }
            if(lists.isEmpty() && mode!="create") EmptyState(LdIcons.Phone,"Abhi koi list nahi","Family, Work ya aaj ke calls ki list banao")
            lists.forEach { l ->
                GlassCard(Modifier.padding(top=10.dp).testTag("list_${l.id}")) {
                    LdText(l.title,17f,FontWeight.Bold,maxLines=2)
                    LdText("${l.entries.count {it.done}} / ${l.entries.size} Done",12f,color=p.muted)
                    PrimaryButton("List kholo",enabled=!busy,modifier=Modifier.testTag("list_open_${l.id}")){selected=l.id;mode=""}
                }
            }
        } else {
            val id=current.id
            LdText("${current.entries.count {it.done}} / ${current.entries.size} Done",14f,FontWeight.Bold,modifier=Modifier.testTag("list_progress"))
            Spacer(Modifier.height(10.dp))
            if(mode.isEmpty()) {
                GlassCard(Modifier.testTag("workspace_summary")) {
                    LdText("Calling workspace",18f,FontWeight.Bold)
                    LdText("Ek waqt par ek person. Outcome aur follow-up note save karo; agla call sirf tum lagate ho.",12f,color=p.muted,maxLines=5)
                    val followups=current.entries.count {it.outcome in listOf("busy","no_answer","later")}
                    LdText("${current.entries.count {!it.done}} Pending · $followups follow-ups",13f,FontWeight.SemiBold,p.accent,maxLines=3)
                    PrimaryButton("Session chalu karo",enabled=!busy && current.entries.isNotEmpty(),modifier=Modifier.testTag("list_session")) {
                        sessionId=(current.entries.firstOrNull {!it.done} ?: current.entries.first()).id
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Progress report save karo",enabled=!busy && current.entries.isNotEmpty(),modifier=Modifier.testTag("list_report")) {
                        reportText=CallListTransfer.report(current);reportFile.launch("call-list-progress.csv")
                    }
                    LdText("Report mein naam, numbers aur private notes honge. Sirf apni chuni jagah save hoga; kisi ko automatic nahi bhejega. CSV report backup/import file nahi hai. Numbers ko text rakhne aur formulas rokne ke liye kuch cells mein apostrophe lagega.",11.5f,color=p.muted,maxLines=6)
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Number jodo",enabled=!busy,modifier=Modifier.testTag("list_add")){mode="manual";name="";number=""}
                PrimaryButton("Numbers paste karo",enabled=!busy,modifier=Modifier.testTag("list_batch")){mode="batch";batchText="";batchReview=null}
                PrimaryButton("Contacts se jodo",enabled=!busy,modifier=Modifier.testTag("list_contacts")){mode="contacts";query=""}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Naam badlo",enabled=!busy,modifier=Modifier.weight(1f).testTag("list_rename")){mode="rename";name=current.title}
                    PrimaryButton("List mitao",enabled=!busy,modifier=Modifier.weight(1f).testTag("list_delete")){confirm="list" to id}
                }
            }
            when(mode) {
                "rename" -> {
                    ListField(name,{name=it.take(60)},"List ka naam","list_title")
                    PrimaryButton("Naam save karo",enabled=!busy && name.isNotBlank(),modifier=Modifier.testTag("list_save")){val savedName=name;update({CallLists.rename(ctx,id,savedName)}){mode=""}}
                }
                "manual" -> {
                    ListField(number,{number=it},"Phone number","list_number")
                    ListField(name,{name=it.take(100)},"Naam (optional)","entry_name")
                    PrimaryButton("Number save karo",enabled=!busy && number.isNotBlank(),modifier=Modifier.testTag("entry_save")) {
                        val savedNumber=number;val savedName=name;update({CallLists.add(ctx,id,savedNumber,savedName)}){mode="";number="";name=""}
                    }
                }
                "batch" -> {
                    LdText("Har line: number ya number, naam. Tab bhi separator hai. Naam mein comma reh sakta hai. Quotes/CSV headers support nahi; pehle preview dekho. Phone contacts nahi badlenge.",12f,color=p.muted,maxLines=6)
                    OutlinedTextField(batchText,{batchText=it;batchReview=null},label={Text("Numbers yahan paste karo")},minLines=5,maxLines=8,modifier=Modifier.fillMaxWidth().testTag("list_batch_text"))
                    PrimaryButton("Preview dekho",enabled=!busy && batchText.isNotBlank(),modifier=Modifier.testTag("list_batch_preview")) {
                        try{batchReview=CallListTransfer.batch(batchText,current.entries.map{it.number});error=""}catch(e:Exception){error=e.message ?: "Paste sahi karo"}
                    }
                    batchReview?.let {preview->
                        LdText("${preview.entries.size} naye · ${preview.duplicateCount} duplicate skip",14f,FontWeight.Bold,modifier=Modifier.testTag("list_batch_count"))
                        if(preview.invalidLines.isNotEmpty())LdText("Galat lines: ${preview.invalidLines.joinToString(", ")}. Pehle sab sahi karo; abhi kuch add nahi hua.",13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("list_batch_invalid"))
                        if(current.entries.size+preview.entries.size>200)LdText("200 entries ki limit se zyada; paste chhota karo.",13f,color=p.danger,maxLines=3)
                        preview.entries.take(20).forEach {row->LdText(row.number+if(row.name.isBlank())"" else " · "+row.name,13f,maxLines=3,modifier=Modifier.padding(vertical=4.dp))}
                        if(preview.entries.size>20)LdText("Preview mein pehle 20; confirm par sab ${preview.entries.size} naye numbers judenge.",12f,color=p.muted,maxLines=3)
                        PrimaryButton("${preview.entries.size} numbers jodo",enabled=!busy && preview.entries.isNotEmpty() && preview.invalidLines.isEmpty() && current.entries.size+preview.entries.size<=200,modifier=Modifier.testTag("list_batch_confirm")) {
                            val text=batchText;update({CallLists.addBatch(ctx,id,text)}){mode="";batchText="";batchReview=null}
                        }
                    }
                }
                "contacts" -> {
                    if(!act.has(Manifest.permission.READ_CONTACTS)) LdText("Contacts permission nahi hai. Number jodo se manually add kar sakte ho.",13f,color=p.muted,maxLines=4)
                    else {
                        ListField(query,{query=it.take(100)},"Naam ya number khojo","list_contact_search")
                        LdText("Yahan har contact ka loaded main number dikhta hai. Doosra number chahiye toh Number jodo use karo.",12f,color=p.muted,maxLines=4)
                        val matches=st.contacts.filter {it.name.contains(query,true) || it.number.contains(query,true)}
                        matches.take(20).forEach { c ->
                            val exists=current.entries.any {ContactControls.k(it.number)==ContactControls.k(c.number)}
                            GlassCard(Modifier.padding(top=8.dp)) {
                                LdText(c.name,14f,FontWeight.Bold,maxLines=2);LdText(c.number,12f,color=p.muted,maxLines=2)
                                PrimaryButton(if(exists) "List mein hai" else "Jodo",enabled=!busy && !exists,modifier=Modifier.testTag("list_contact_${c.id}")) {
                                    update({CallLists.add(ctx,id,c.number,c.name)})
                                }
                            }
                        }
                        if(matches.isEmpty()) LdText("Koi loaded contact nahi mila",13f,color=p.muted)
                        if(matches.size>20) LdText("Pehle 20 results. Aur sahi naam ya number likho.",12f,color=p.muted,maxLines=2)
                    }
                }
            }
            if(mode.isNotEmpty()) PrimaryButton("Wapas list par",enabled=!busy,modifier=Modifier.testTag("list_form_close")){mode="";error=""}
            if(mode.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("Pending","Done","Sab").forEach {f -> PrimaryButton(if(filter==f) "[$f]" else f,enabled=!busy,modifier=Modifier.weight(1f).testTag("list_filter_$f")){filter=f;visible=20}}
                }
                val entries=current.entries.filter {filter=="Sab" || it.done==(filter=="Done")}
                if(entries.isEmpty()) LdText(if(current.entries.isEmpty()) "List khaali hai. Contacts ya number jodo." else "Is view mein koi entry nahi.",13f,color=p.muted,maxLines=3,modifier=Modifier.padding(top=12.dp))
                entries.take(visible).forEach {e ->
                    GlassCard(Modifier.padding(top=10.dp).testTag("list_entry_${e.id}")) {
                        LdText(e.name.ifBlank {e.number},16f,FontWeight.Bold,maxLines=2)
                        if(e.name.isNotBlank()) LdText(e.number,12f,color=p.muted,maxLines=2)
                        LdText(CallLists.outcomes.getValue(e.outcome)+" · khud save kiya",12f,color=p.muted)
                        if(e.note.isNotBlank())LdText(e.note,12f,color=p.muted,maxLines=3,modifier=Modifier.padding(top=6.dp))
                        PrimaryButton("Outcome / note kholo",enabled=!busy,modifier=Modifier.testTag("list_workspace_${e.id}")){sessionId=e.id}
                        Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            PrimaryButton("Call karo",enabled=!busy,modifier=Modifier.weight(1f).testTag("list_call_${e.id}")){act.callUi(e.number)}
                            PrimaryButton(if(e.done) "Pending karo" else "Done karo",enabled=!busy,modifier=Modifier.weight(1f).testTag("list_done_${e.id}")){update({CallLists.done(ctx,id,e.id,!e.done)})}
                        }
                        PrimaryButton("Entry hatao",enabled=!busy,modifier=Modifier.testTag("list_remove_${e.id}")){confirm="entry" to e.id}
                    }
                }
                if(entries.size>visible) PrimaryButton("Aur ${minOf(20,entries.size-visible)} dikhao",enabled=!busy){visible+=20}
            }
        }
        if(busy) LdText("Save ho raha hai...",12f,color=p.muted)
    }
    BackHandler {back()}
    confirm?.let { (kind,target) ->
        val entry=current?.entries?.firstOrNull {it.id==target}
        AlertDialog(onDismissRequest={if(!busy)confirm=null},
            title={Text(if(kind=="list") "List mitani hai?" else "Entry hatani hai?")},
            text={Text(if(kind=="list") "${current?.title}\nIs list ki sab entries aur progress mit jayegi. Phone contacts/call history nahi mitegi. Wapas nahi la sakte." else "${entry?.name.orEmpty()}\n${entry?.number.orEmpty()}\nSirf is list se hatega. Phone contact nahi mitega.")},
            confirmButton={TextButton(enabled=!busy,onClick={
                val id=current?.id ?: return@TextButton
                update({if(kind=="list") CallLists.delete(ctx,id) else CallLists.remove(ctx,id,target)}){confirm=null;if(kind=="list")selected=null}
            },modifier=Modifier.testTag("list_confirm_delete")){Text("Mitao")}},
            dismissButton={TextButton(enabled=!busy,onClick={confirm=null},modifier=Modifier.testTag("list_cancel_delete")){Text("Rehne do")}})
    }
}

@Composable private fun ListField(value:String,onValue:(String)->Unit,label:String,tag:String) {
    OutlinedTextField(value,onValue,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth().padding(vertical=8.dp).testTag(tag))
}
