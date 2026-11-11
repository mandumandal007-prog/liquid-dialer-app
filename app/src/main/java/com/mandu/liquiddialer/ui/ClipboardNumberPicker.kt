package com.mandu.liquiddialer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mandu.liquiddialer.data.ClipboardNumbers
import com.mandu.liquiddialer.data.CallLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ClipboardNumberPicker(preview: ClipboardNumbers.Preview, replacing: Boolean, onChoose: (String) -> Unit, onClose: () -> Unit, onSaved: () -> Unit) {
    val features by com.mandu.liquiddialer.LdApp.settings.collectAsState()
    val listsOn=com.mandu.liquiddialer.data.FeatureControls.enabled(features,"calllists")
    val p=LocalLd.current;val ctx=LocalContext.current;val scope=rememberCoroutineScope()
    var selected by remember(preview) {mutableStateOf(preview.numbers.toSet())}
    val title="Pasted numbers"
    val density=LocalDensity.current
    var error by remember {mutableStateOf("")};var busy by remember {mutableStateOf(false)}
    Dialog(onDismissRequest={if(!busy)onClose()}) {
      CompositionLocalProvider(LocalDensity provides density) {
        Column(Modifier.fillMaxWidth().heightIn(max=600.dp).background(p.bg2,RoundedCornerShape(24.dp))
            .border(1.dp,p.stroke,RoundedCornerShape(24.dp)).padding(18.dp).testTag("paste_picker"),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            LdText("Copied numbers",20f,FontWeight.Bold)
            LdText("List ke tick alag chuno. Number dabao to sirf keypad bharega. Koi auto-call ya contact change nahi.",13f,color=p.muted,maxLines=4)
            if(preview.numbers.isEmpty()) {
                LdText("Saaf phone number nahi mila",16f,FontWeight.SemiBold)
                LdText("7-15 digits wala number alag line mein copy karo. URL, email, extension aur service code yahan nahi chunte; keypad par khud likho.",13f,color=p.muted,maxLines=5)
            } else {
                if(listsOn) LdText("Nayi list: Pasted numbers. Save ke baad Call Lists mein naam badal sakte ho.",12f,color=p.muted,maxLines=3)
                if(listsOn) LdText("${selected.size} / ${preview.numbers.size} selected",12f,color=p.muted)
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()).testTag("paste_numbers_scroll"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    preview.numbers.forEachIndexed { i, number ->
                        Row(Modifier.fillMaxWidth().buttonGlass(p,RoundedCornerShape(16.dp),p.accent).padding(4.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            if(listsOn) Column(Modifier.width(64.dp).heightIn(min=64.dp).toggleable(value=number in selected,enabled=!busy,role=Role.Checkbox,onValueChange={checked->selected=if(checked)selected+number else selected-number}).semantics {contentDescription="List mein $number"}.testTag("paste_select_$i"),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                                LdText(if(number in selected) "✓" else "○",20f,color=p.accent)
                                LdText("List",11f,color=p.accent)
                            }
                            Column(Modifier.weight(1f).heightIn(min=56.dp).clickable(enabled=!busy) {onChoose(number)}.padding(vertical=8.dp).testTag("paste_number_$i")) {
                                LdText(number,16f,FontWeight.SemiBold,maxLines=2)
                                LdText("Keypad mein bharo",11.5f,color=p.muted)
                            }
                        }
                    }
                }
                if(listsOn) Box(Modifier.fillMaxWidth().heightIn(min=48.dp).buttonGlass(p,RoundedCornerShape(14.dp),p.accent)
                    .clickable(enabled=!busy && selected.isNotEmpty() && title.isNotBlank()) {
                        busy=true;error="";val numbers=preview.numbers.filter {it in selected};val name=title
                        scope.launch {try {withContext(Dispatchers.IO) {CallLists.createFromNumbers(ctx,name,numbers)};onSaved()}
                            catch(e:Exception){error=(e as? IllegalArgumentException)?.message ?: "List save nahi hui. Purani lists nahi badli."}
                            finally {busy=false}}
                    }.padding(12.dp).testTag("paste_save_list")) {LdText(if(busy)"Save ho raha hai..." else "${selected.size} numbers ki list save karo",14f,FontWeight.SemiBold)}
            }
            if(preview.limited)LdText("Sirf pehla hissa / pehle 12 number dikhaye hain. Baaki alag copy karo.",12f,color=p.muted,maxLines=3)
            if(error.isNotEmpty())LdText(error,12f,color=p.danger,maxLines=4,modifier=Modifier.testTag("paste_error"))
            Box(Modifier.fillMaxWidth().heightIn(min=48.dp).buttonGlass(p,RoundedCornerShape(14.dp))
                .clickable(enabled=!busy,onClick=onClose).padding(12.dp).testTag("paste_cancel")) {LdText("Rehne do",14f,FontWeight.SemiBold)}
        }
      }
    }
}
