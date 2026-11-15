package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun DeskSectionsPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")};var reset by remember{mutableStateOf(false)}
 val rows=DeskSections.parse(s.deskSectionLayout)
 fun save(rows:List<DeskSections.Section>){if(busy)return;busy=true;val raw=DeskSections.encode(rows);scope.launch{try{ctx.ldStore.edit{it[K.deskSectionLayout]=raw};message="Saved for the next desk opening."}catch(_:Exception){message="Layout not saved. Try again."}finally{busy=false}}}
 PageFrame("Desk section layout",onBack){
  LdText("Apna desk. Apna order.",22f,FontWeight.Bold,maxLines=4)
  LdText("Notes and call tools, Interval stopwatch aur Call checkpoints ka vertical order chuno. Stopwatch/checkpoints ki desk entry hide bhi kar sakte ho. Notes aur fixed call controls yahan hide nahi hote.",14f,color=p.muted,maxLines=12)
  GlassCard{SwitchRow("Desk section layout","Default OFF. Saved placement applies on the next desk opening.",s.deskSections,last=true){v->if(!busy){busy=true;scope.launch{try{ctx.ldStore.edit{it[K.deskSections]=v};message="Saved for the next desk opening."}catch(_:Exception){message="Setting not saved. Try again."}finally{busy=false}}}}}
  GlassCard{LdText("Har edit turant phone par save hota hai. ON/OFF aur order/hide changes current open desk ko nahi badalte. Card par return karke desk phir kholo. Isse unsaved inputs discard honge: pehle apne tools mein Save karo, running stopwatch Pause karo. Tool ke apne OFF/safety rules abhi bhi turant apply hote hain.",13f,maxLines=17)}
  SecTitle("Saved section order")
  rows.forEachIndexed{i,row->GlassCard(Modifier.testTag("desk_layout_row_${row.id}")){
   LdText("${i+1}. ${DeskSections.title(row.id)}",16f,FontWeight.Bold,maxLines=4)
   if(row.id=="notes")LdText("Always available here. Existing feature controls still apply to individual call tools.",12f,color=p.muted,maxLines=6)
   else SwitchRow("Show desk entry",if(row.visible)"Shown only when its own feature is ON."else"Hidden here only. Saved data stays.",row.visible,last=true){if(!busy)save(DeskSections.show(rows,row.id,it))}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    PrimaryButton("Move up",enabled=i>0&&!busy,modifier=Modifier.weight(1f).testTag("desk_layout_up_${row.id}")){save(DeskSections.move(rows,row.id,-1))}
    PrimaryButton("Move down",enabled=i<rows.lastIndex&&!busy,modifier=Modifier.weight(1f).testTag("desk_layout_down_${row.id}")){save(DeskSections.move(rows,row.id,1))}
   }
  }}
  SecTitle("Next opening preview")
  GlassCard(Modifier.testTag("desk_layout_preview")){
   LdText("Caller + connected timer + Card",13f,FontWeight.Bold,maxLines=4)
   LdText("Optional keypad stays above the sections.",12f,color=p.muted,maxLines=4)
   DeskSections.snapshot(s).filter{it.visible}.forEachIndexed{i,row->LdText("${i+1}. ${DeskSections.title(row.id)}",14f,maxLines=4,modifier=Modifier.testTag("desk_layout_preview_${row.id}"))}
   LdText("Mute / Speaker + long red End stay fixed below.",13f,FontWeight.Bold,maxLines=4)
   LdText(if(s.deskSections)"Layout ON. Preview shows placement, not enabled tools."else"Layout OFF. Original order; saved custom choices kept.",12f,color=p.muted,maxLines=5)
  }
  GhostButton("Reset saved section order",Modifier.testTag("desk_layout_reset")){if(!busy)reset=true}
  if(message.isNotEmpty())LdText(message,13f,color=p.accent,maxLines=5,modifier=Modifier.testTag("desk_layout_message"))
  LdText("Local placement preferences only. Does not enable Live call desk, stopwatch, checkpoints or keypad. No note edits, audio, call actions, network, clipboard or new permission. Closing desk still follows its existing confirmation and unsaved-work rules.",13f,color=p.muted,maxLines=14)
 }
 if(reset)AlertDialog(onDismissRequest={reset=false},title={LdText("Reset saved section order?",19f,FontWeight.Bold,maxLines=4)},text={LdText("Restore Stopwatch, Checkpoints, Notes order with both optional entries shown. Master ON/OFF and other feature switches stay as they are. Current desk stays unchanged.",14f,maxLines=9)},confirmButton={TextButton(onClick={save(DeskSections.parse(DeskSections.DEFAULT));reset=false},modifier=Modifier.testTag("desk_layout_reset_confirm")){LdText("Reset order",14f)}},dismissButton={TextButton(onClick={reset=false},modifier=Modifier.testTag("desk_layout_reset_cancel")){LdText("Keep layout",14f)}})
}
