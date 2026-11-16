package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@Composable fun BriefingReviewPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Briefing review queue",onBack){
  LdText("Ek jagah pending baatein.",22f,FontWeight.Bold,maxLines=4)
  GlassCard{SwitchRow("Briefing review queue","Default OFF. Saved briefing ke andar session review.",s.briefingReview,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.briefingReview]=v};error=false}catch(_:Exception){error=true}}}}
  LdText("Call briefing kholkar Review pending items dabao. Saved pending topics aur unfinished Call List entries ek jagah dekho, source/text se filter karo, aur is session mein Reviewed mark karo. Saved notes aur estimate purane tabs par hain.",14f,color=p.muted,maxLines=16)
  GlassCard{LdText("Reviewed sirf temporary reading progress hai, Discussed/Done nahi. Koi saved checklist/list nahi badalti. Refresh, number change, briefing close ya setting OFF se progress reset. Snapshot purana ho sakta hai; Refresh nayi saved jankari leta hai. Source error ko empty nahi maana jata.",13f,maxLines=18)}
  LdText("No call, message, clipboard, note save, notification, network or new permission. Planned date sirf saved value hai, reminder nahi. OFF se original briefing rehta hai.",13f,color=p.muted,maxLines=12)
  if(error)LdText("Setting save nahi hui",13f,color=p.danger)
 }
}
@Composable fun BriefingReviewWorkspace(s:CallBriefing.Snapshot,busy:Boolean){
 val p=LocalLd.current
 val queue=remember(s){BriefingReview.from(s)}
 var open by remember(s){mutableStateOf(false)};var query by remember(s){mutableStateOf("")};var source by remember(s){mutableStateOf("All")};var only by remember(s){mutableStateOf(false)};var reviewed by remember(s){mutableStateOf(setOf<String>())};var reset by remember(s){mutableStateOf(false)};var limit by remember(s){mutableStateOf(10)}
 LaunchedEffect(query,source,only){limit=10}
 GhostButton(if(open)"Hide review queue"else"Review pending items",Modifier.testTag("review_toggle")){open=!open}
 if(!open)return
 val rows=BriefingReview.visible(queue,query,source,only,reviewed)
 Column(Modifier.fillMaxWidth().padding(vertical=8.dp).testTag("review_queue"),verticalArrangement=Arrangement.spacedBy(8.dp)){
  LdText("SESSION REVIEW",12f,FontWeight.Bold,p.accent,maxLines=3)
  LdText("${BriefingReview.count(queue,reviewed)} / ${queue.rows.size} reviewed in this snapshot",14f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("review_progress"))
  LdText("Reading progress only, not saved Done/Discussed. Refresh resets it.",12f,color=p.muted,maxLines=5)
  queue.errors.forEach{LdText("$it load error. Queue is incomplete; not zero pending items.",13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("review_error"))}
  LdField(query,{query=it.take(100)},"Search pending text",modifier=Modifier.testTag("review_query"))
  BriefingReview.sources.forEach{v->GhostButton(if(v==source)"$v ✓"else v,Modifier.testTag("review_source_${v.replace(" ","").lowercase()}")){source=v}}
  GhostButton(if(only)"Unreviewed only · show all"else"All items · show unreviewed",Modifier.testTag("review_only")){only=!only}
  GhostButton("Clear filters",Modifier.testTag("review_clear")){query="";source="All";only=false}
  if(reviewed.isNotEmpty())GhostButton("Reset session review",Modifier.testTag("review_reset")){reset=true}
  if(reset)GlassCard{LdText("Reset temporary review marks? Saved items stay unchanged.",13f,maxLines=5);GhostButton("Cancel reset",Modifier.testTag("review_reset_cancel")){reset=false};PrimaryButton("Confirm reset",modifier=Modifier.testTag("review_reset_confirm")){reviewed=emptySet();reset=false}}
  LdText("${rows.size} matching pending items",12f,color=p.muted,maxLines=4,modifier=Modifier.testTag("review_count"))
  if(rows.isEmpty())LdText(if(queue.errors.isNotEmpty())"No matching loaded items. One or more sources failed."else"No items in this filter. Clear filters or Refresh saved data.",13f,color=p.muted,maxLines=6,modifier=Modifier.testTag("review_empty"))
  rows.take(limit).forEach{r->GlassCard(Modifier.testTag("review_row_${r.id}")){
   LdText(r.source,11f,color=p.muted,maxLines=3);LdText(r.title,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE);LdText(r.detail,13f,color=p.muted,maxLines=Int.MAX_VALUE)
   if(r.dueAt>0)LdText("Saved plan: "+SimpleDateFormat("d MMM yyyy, h:mm a",Locale.getDefault()).format(Date(r.dueAt))+" · no reminder",12f,color=p.muted,maxLines=6)
   PrimaryButton(if(r.id in reviewed)"Reviewed · undo"else"Mark reviewed",enabled=!busy,modifier=Modifier.testTag("review_mark_${r.id}")){if(!busy&&LdApp.settings.value.briefingReview)reviewed=BriefingReview.toggle(queue,reviewed,r.id)}
  }}
  if(rows.size>limit)PrimaryButton("10 more pending items",modifier=Modifier.testTag("review_more")){limit+=10}
 }
}
