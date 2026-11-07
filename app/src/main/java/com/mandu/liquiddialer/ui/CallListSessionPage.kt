package com.mandu.liquiddialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.CallLists
import java.text.SimpleDateFormat
import java.util.*

/** Focused manual workspace. Navigation and call attempts never imply an outcome. */
@Composable fun CallListSessionPage(list:CallLists.ListItem, initialId:String, busy:Boolean, error:String,
    onBack:()->Unit, onCall:(String)->Unit, onSave:(String,String,String,()->Unit)->Unit) {
    val p=LocalLd.current
    var currentId by rememberSaveable(list.id) {mutableStateOf(initialId)}
    val entry=list.entries.firstOrNull {it.id==currentId}
    if(entry==null) {PageFrame("Call workspace",onBack){LdText("Entry ab list mein nahi hai. List dobara kholo.",14f,maxLines=4)};return}
    var outcome by rememberSaveable(entry.id) {mutableStateOf(entry.outcome)}
    var note by rememberSaveable(entry.id) {mutableStateOf(entry.note)}
    var destination by remember {mutableStateOf<String?>(null)}
    val dirty=outcome!=entry.outcome || note!=entry.note
    val index=list.entries.indexOf(entry)
    fun move(id:String) {if(busy)return;if(dirty)destination=id else if(id=="back")onBack() else currentId=id}
    fun next() {val n=list.entries.drop(index+1).firstOrNull {!it.done}; if(n!=null)currentId=n.id else onBack()}
    PageFrame("Call workspace",{move("back")}) {
        LdText(list.title,13f,FontWeight.SemiBold,p.accent,maxLines=2)
        LdText("${index+1} / ${list.entries.size} · ${list.entries.count {it.done}} Done",12f,color=p.muted,modifier=Modifier.testTag("session_progress"))
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.testTag("session_person")) {
            LdText(entry.name.ifBlank {entry.number},23f,FontWeight.Bold,maxLines=3,modifier=Modifier.testTag("session_name"))
            if(entry.name.isNotBlank())LdText(entry.number,15f,color=p.muted,maxLines=3,modifier=Modifier.padding(top=6.dp).testTag("session_number"))
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Call karo",enabled=!busy,modifier=Modifier.testTag("session_call")){onCall(entry.number)}
            LdText("Call tum khud lagate ho. Call karne ya agla number kholne se progress apne aap nahi badlegi.",12f,color=p.muted,maxLines=5)
        }
        SecTitle("Outcome khud chuno")
        CallLists.outcomes.entries.chunked(2).forEach {row->
            Row(Modifier.fillMaxWidth().padding(bottom=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach {(key,label)->
                    val chosen=outcome==key
                    PrimaryButton(if(chosen) "✓ $label" else label,enabled=!busy,
                        modifier=Modifier.weight(1f).semantics {selected=chosen}
                            .then(if(chosen) Modifier.border(2.dp,p.accent,RoundedCornerShape(16.dp)).background(p.accent.copy(alpha=.10f),RoundedCornerShape(16.dp)) else Modifier)
                            .testTag("session_outcome_$key")){outcome=key}
                }
                if(row.size==1)Spacer(Modifier.weight(1f))
            }
        }
        LdText("Busy, No answer aur Call later Pending mein rahenge. Ye khud likha outcome hai, phone ki call-history ka result nahi.",12f,color=p.muted,maxLines=5)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(note,{note=it},enabled=!busy,label={Text("Follow-up note")},minLines=3,maxLines=6,modifier=Modifier.fillMaxWidth().testTag("session_note"))
        LdText("${note.length} / 2,000 letters · isi list mein private",11.5f,color=if(note.length>2000)p.danger else p.muted,maxLines=3)
        if(note.length>2000)LdText("Note 2,000 letters se lamba hai. Text nahi kata; save ke liye chhota karo.",12f,color=p.danger,maxLines=4,modifier=Modifier.testTag("session_note_limit"))
        if(entry.updatedAt>0)LdText("Aakhri save: "+SimpleDateFormat("d MMM yyyy, HH:mm",Locale.getDefault()).format(Date(entry.updatedAt)),11.5f,color=p.muted,maxLines=3)
        if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("session_error"))
        PrimaryButton(if(busy) "Save ho raha hai..." else "Outcome aur note save karo",enabled=!busy && note.length<=2000 && dirty,modifier=Modifier.testTag("session_save")) {
            onSave(entry.id,outcome,note){}
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Save aur agla Pending",enabled=!busy && note.length<=2000,modifier=Modifier.testTag("session_save_next")) {
            onSave(entry.id,outcome,note){next()}
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Pichhla",enabled=!busy && index>0,modifier=Modifier.weight(1f).testTag("session_previous")){move(list.entries[index-1].id)}
            PrimaryButton("Agla",enabled=!busy && index<list.entries.lastIndex,modifier=Modifier.weight(1f).testTag("session_next")){move(list.entries[index+1].id)}
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("List par wapas",enabled=!busy,modifier=Modifier.testTag("session_back")){move("back")}
    }
    BackHandler {move("back")}
    destination?.let {target->AlertDialog(onDismissRequest={destination=null},title={Text("Badlav save nahi hue")},
        text={Text("Is outcome/note ke badlav chhodkar aage jaana hai? Purana saved data nahi badlega.")},
        confirmButton={TextButton(onClick={destination=null;if(target=="back")onBack() else currentId=target},modifier=Modifier.testTag("session_discard")){Text("Badlav chhodo")}},
        dismissButton={TextButton(onClick={destination=null},modifier=Modifier.testTag("session_keep_editing")){Text("Edit par wapas")}})}
}
