package com.mandu.liquiddialer.ui
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch
@Composable fun ReviewOptionsPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf("")}
 PageFrame("Review priorities & row layout",onBack){
  LdText("Apni reading order. Apna row style.",22f,FontWeight.Bold,maxLines=6)
  SwitchRow("Review session priorities","Default OFF. Manually prioritize up to10 loaded pending items.",s.reviewPriority,last=true){v->scope.launch{try{LdApp.instance.repo.set(K.reviewPriority,v);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Briefing review queue mein ☆ Prioritize tap karo. Priorities pehle aate hain, original order stays within each group. Priorities-only filter combines with search, source and Unreviewed filters. Reviewed status and priority are separate. No automatic urgency judgement. Undo each star to remove it.",14f,color=p.muted,maxLines=16)
  SwitchRow("Slim review rows layout","Default OFF. Smaller glass row titles and details collapsed until Show details.",s.reviewSlim,last=true){v->scope.launch{try{LdApp.instance.repo.set(K.reviewSlim,v);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Slim layout keeps complete wrapped titles and all review actions. Show details reveals full source text and saved plan date; Hide details folds it again. OFF restores the original full detail cards. No animation, no change to caller, timer or call controls.",14f,color=p.muted,maxLines=14)
  GlassCard{LdText("Both options work inside Briefing review queue, which must also be enabled. Session priorities are not saved Done/Discussed, obligations or reminders. Hide queue keeps session progress; Refresh, close, number change or priority OFF clears stars. Layout change only folds details, not review marks or stars. Sources remain unchanged; errors still mean incomplete data. No call/message, clipboard, storage of priorities, notification or network.",13f,maxLines=24)}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5)
 }
}
