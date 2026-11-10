package com.mandu.liquiddialer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun CallBriefingPage(number:String,onBack:()->Unit)=CallBriefingWorkspace(number,onBack)
@Composable fun CallBriefingWorkspace(number:String,onBack:()->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current
 var revision by remember(number){mutableStateOf(0)};var snapshot by remember(number){mutableStateOf<CallBriefing.Snapshot?>(null)}
 var busy by remember(number){mutableStateOf(false)};var error by remember(number){mutableStateOf(false)};var loadedAt by remember(number){mutableStateOf(0L)}
 var query by remember(number){mutableStateOf("")};var pendingOnly by remember(number){mutableStateOf(true)};var tab by remember(number){mutableStateOf("notes")};var limit by remember(number){mutableStateOf(10)}
 val df=remember{SimpleDateFormat("d MMM yyyy, h:mm a",Locale.getDefault())}
 LaunchedEffect(number,revision){busy=true;error=false;try{val next=withContext(Dispatchers.IO){CallBriefing.load(ctx,number)};snapshot=next;loadedAt=System.currentTimeMillis()}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){error=true}finally{busy=false}}
 LaunchedEffect(query,tab,pendingOnly){limit=10}
 val content:@Composable ColumnScope.()->Unit={
  LdText(number,16f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("brief_number"))
  LdText("Saved call taiyari, sirf padhna. Koi call/message ya data badlav nahi.",13f,color=p.muted,maxLines=6)
  LdText("Local snapshot, live sync nahi. Nayi jankari ke liye Refresh.",12f,color=p.muted,maxLines=5)
  Spacer(Modifier.height(8.dp))
  PrimaryButton(if(busy)"Load ho raha hai..."else "Refresh briefing",enabled=!busy,modifier=Modifier.testTag("brief_refresh")){revision++}
  if(error)LdText("Briefing load nahi hui. Purana snapshot ho to neeche rahega; Refresh dobara try karo.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("brief_error"))
  snapshot?.let{s->
   LdText("Snapshot: ${df.format(Date(loadedAt))}",11f,color=p.muted,maxLines=3,modifier=Modifier.testTag("brief_time"))
   GlassCard(Modifier.padding(vertical=8.dp)){
    LdText("${if(s.notes.error)"?"else s.notes.value.orEmpty().size} notes · ${if(s.topics.error)"?"else s.topics.value.orEmpty().count{!it.done}} pending topics",14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("brief_counts"))
    LdText("${if(s.lists.error)"?"else s.lists.value.orEmpty().size} Call List entries · estimate ${if(s.estimate.error)"load error"else if(s.estimate.value?.lines.isNullOrEmpty())"nahi"else "saved"}",12f,color=p.muted,maxLines=4)
   }
   LdField(query,{query=it},"Saved text mein khojo",modifier=Modifier.testTag("brief_search"))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("notes" to "Notes","topics" to "Topics").forEach{(id,label)->PrimaryButton(if(tab==id)"✓ $label"else label,modifier=Modifier.weight(1f).testTag("brief_tab_$id")){tab=id}}}
   Spacer(Modifier.height(8.dp))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("estimate" to "Estimate","lists" to "Call Lists").forEach{(id,label)->PrimaryButton(if(tab==id)"✓ $label"else label,modifier=Modifier.weight(1f).testTag("brief_tab_$id")){tab=id}}}
   Spacer(Modifier.height(8.dp))
   when(tab){
    "notes"->{SecTitle("Saved notes");if(s.notes.error)LdText("Notes load nahi hue. Iska matlab zero notes nahi. Refresh se dobara try karo.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("brief_section_error"))else{
     val rows=CallBriefing.visibleNotes(s,query);LdText("${rows.size} matching saved notes",12f,color=p.muted,modifier=Modifier.testTag("brief_matches"));if(rows.isEmpty())LdText(if(query.isBlank())"Is number ka saved note nahi mila."else "Notes mein yeh text nahi mila.",13f,color=p.muted,maxLines=4)
     rows.take(limit).forEach{n->GlassCard(Modifier.padding(top=8.dp).testTag("brief_note_${n.id}")){LdText(df.format(Date(n.time)),11f,color=p.muted);LdText(n.text,15f,maxLines=Int.MAX_VALUE)}}
     if(rows.size>limit)PrimaryButton("10 aur notes dikhao",modifier=Modifier.testTag("brief_more")){limit+=10}
    }}
    "topics"->{SecTitle("Conversation topics");PrimaryButton(if(pendingOnly)"Sirf Pending · sab dekho"else "Sab topics · sirf Pending dekho",modifier=Modifier.testTag("brief_pending")){pendingOnly=!pendingOnly}
     if(s.topics.error)LdText("Topics load nahi hue. Zero topics ka nateeja nahi; Refresh try karo.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("brief_section_error"))else{
      val rows=CallBriefing.visibleTopics(s,query,pendingOnly);LdText("${rows.size} matching topics",12f,color=p.muted,modifier=Modifier.testTag("brief_matches"));if(rows.isEmpty())LdText("Is filter mein koi topic nahi mila.",13f,color=p.muted)
      rows.forEach{r->GlassCard(Modifier.padding(top=8.dp).testTag("brief_topic_${r.id}")){LdText(if(r.done)"Discussed"else "Pending",11f,color=p.muted);LdText(r.text,15f,maxLines=Int.MAX_VALUE)}}
     }
    }
    "estimate"->{SecTitle("Saved estimate");if(s.estimate.error)LdText("Estimate load nahi hua. Refresh se dobara try karo.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("brief_section_error"))else{
     val d=s.estimate.value!!
     if(d.lines.isEmpty())LdText("Is number ka saved estimate nahi mila.",13f,color=p.muted)else{
      val t=CallEstimate.totals(d);GlassCard{LdText("Total: ${CallEstimate.money(t.total)}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("brief_estimate_total"));LdText("Subtotal ${CallEstimate.money(t.subtotal)} · discount ${CallEstimate.money(t.discount)} · tax ${CallEstimate.money(t.tax)} (${d.tax}%)",12f,color=p.muted,maxLines=6);LdText("Sirf saved estimate, bill/payment nahi. Sab amounts ek hi currency mein; currency conversion nahi.",12f,color=p.muted,maxLines=5)}
      val rows=d.lines.filter{CallBriefing.match(it.label+"\n"+it.quantity+"\n"+it.rate,query)};LdText("${rows.size} matching items",12f,color=p.muted,modifier=Modifier.testTag("brief_matches"));rows.forEach{r->GlassCard(Modifier.padding(top=8.dp)){LdText(r.label,15f,FontWeight.Bold,maxLines=Int.MAX_VALUE);LdText("${r.quantity} × ${r.rate}",13f,color=p.muted,maxLines=4)}}
     }
    }}
    "lists"->{SecTitle("Private Call List entries");if(s.lists.error)LdText("Call Lists load nahi hui. Refresh se dobara try karo.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("brief_section_error"))else{
     val rows=CallBriefing.visibleLists(s,query);LdText("${rows.size} matching entries",12f,color=p.muted,modifier=Modifier.testTag("brief_matches"));if(rows.isEmpty())LdText("Is number ki entry ya search text nahi mila.",13f,color=p.muted,maxLines=4)
     rows.take(limit).forEach{r->GlassCard(Modifier.padding(top=8.dp).testTag("brief_list_${r.listId}_${r.entry.id}")){LdText(r.title,15f,FontWeight.Bold,maxLines=Int.MAX_VALUE);LdText(CallLists.outcomes[r.entry.outcome]?:r.entry.outcome,12f,color=p.muted);if(r.entry.note.isNotBlank())LdText(r.entry.note,14f,maxLines=Int.MAX_VALUE);if(r.entry.dueAt>0)LdText("Planned: ${df.format(Date(r.entry.dueAt))} · yeh notification nahi",12f,color=p.muted,maxLines=5)}}
     if(rows.size>limit)PrimaryButton("10 aur entries dikhao",modifier=Modifier.testTag("brief_more")){limit+=10}
    }}
   }
  }
 }
 if(inSheet)Column{SheetTitle("Call briefing");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("brief_notes_back"),onClick=onBack);Spacer(Modifier.height(8.dp));content()}else PageFrame("Call briefing",onBack,content=content)
}
