package com.mandu.liquiddialer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.Note
import com.mandu.liquiddialer.data.Repo
import java.text.SimpleDateFormat
import java.util.*

/** Full note, explicit edit/save, and a discard guard. Opening never changes stored data. */
@Composable fun NoteDetailPage(note:Note,onBack:()->Unit,onSaved:()->Unit) {
    val ctx=LocalContext.current;val p=LocalLd.current
    var editing by rememberSaveable(note.id){mutableStateOf(false)}
    var text by rememberSaveable(note.id){mutableStateOf(note.text)}
    var discard by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    fun back(){if(editing && text!=note.text)discard=true else onBack()}
    PageFrame("Poora note",{back()}) {
        LdText(note.name.ifBlank {note.number},18f,FontWeight.Bold,maxLines=3)
        LdText(note.number,13f,color=p.muted,maxLines=3)
        LdText(SimpleDateFormat("d MMM yyyy, HH:mm",Locale.getDefault()).format(Date(note.time)),12f,color=p.muted)
        LdText("Edit se sirf note ka text badlega; number aur purana time wahi rahenge.",12f,color=p.muted,maxLines=4,modifier=Modifier.padding(vertical=10.dp))
        if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4,modifier=Modifier.testTag("note_edit_error"))
        if(editing) {
            OutlinedTextField(text,{text=it},label={Text("Note text")},minLines=6,maxLines=10,modifier=Modifier.fillMaxWidth().testTag("note_edit_text"))
            LdText("${text.length} / 50,000 letters",12f,color=p.muted)
            PrimaryButton("Badlav save karo",enabled=text.isNotBlank() && text.trim().length<=50000,modifier=Modifier.testTag("note_edit_save")) {
                try{Repo.updateNote(ctx,note,text);onSaved()}catch(e:Exception){error=(e as? IllegalArgumentException)?.message ?: "Note save nahi hua. Purana note nahi badla."}
            }
            PrimaryButton("Badlav chhodo",modifier=Modifier.testTag("note_edit_cancel")){if(text!=note.text)discard=true else onBack()}
        }else {
            PrimaryButton("Note edit karo",modifier=Modifier.testTag("note_edit")){editing=true}
            SelectionContainer {Text(note.text,color=p.text,modifier=Modifier.testTag("note_full_text"))}
        }
    }
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Badlav chhod dein?")},text={Text("Save nahi kiye hue badlav mit jayenge. Purana note wahi rahega.")},confirmButton={TextButton(onClick=onBack,modifier=Modifier.testTag("note_discard_confirm")){Text("Badlav chhodo")}},dismissButton={TextButton(onClick={discard=false},modifier=Modifier.testTag("note_discard_cancel")){Text("Edit par wapas")}})
}
