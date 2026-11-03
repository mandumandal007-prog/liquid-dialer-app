package com.mandu.liquiddialer.ui

import android.Manifest
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
    var selected by remember {mutableStateOf<String?>(null)}
    var mode by remember {mutableStateOf("")}
    var name by remember {mutableStateOf("")}
    var number by remember {mutableStateOf("")}
    var query by remember {mutableStateOf("")}
    var visible by remember(selected) {mutableIntStateOf(20)}
    var filter by remember(selected) {mutableStateOf("Pending")}
    var confirm by remember {mutableStateOf<Pair<String,String>?>(null)}
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
    fun back() {if(busy)return; when {mode.isNotEmpty()->mode="";selected!=null->selected=null;else->onBack()}}
    PageFrame(current?.title ?: "Call Lists",{back()}) {
        LdText("Apni calling lists banao, ek-ek karke khud call karo aur Done lagao. Call dabane se entry apne aap Done nahi hogi. Lists isi app mein save hoti hain.",12f,color=p.muted,maxLines=6)
        Spacer(Modifier.height(12.dp))
        if(error.isNotEmpty()) LdText(error,13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("lists_error"))
        if(!loaded) {
            LdText("Lists load ho rahi hain. Load fail ho toh wapas jaakar dobara kholo.",13f,color=p.muted,maxLines=4)
        } else if(current==null) {
            if(mode!="create") PrimaryButton("Nayi list banao",enabled=!busy,modifier=Modifier.testTag("lists_create")){mode="create";name=""}
            if(mode=="create") {
                ListField(name,{name=it.take(60)},"List ka naam", "list_title")
                PrimaryButton("List save karo",enabled=!busy && name.isNotBlank(),modifier=Modifier.testTag("list_save")) {
                    val savedName=name;update({CallLists.create(ctx,savedName)}) {selected=lists.last().id;mode="";name=""}
                }
                PrimaryButton("Rehne do",enabled=!busy){mode=""}
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
                PrimaryButton("Number jodo",enabled=!busy,modifier=Modifier.testTag("list_add")){mode="manual";name="";number=""}
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
                        LdText(if(e.done) "Done (khud mark kiya)" else "Pending",12f,color=p.muted)
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
