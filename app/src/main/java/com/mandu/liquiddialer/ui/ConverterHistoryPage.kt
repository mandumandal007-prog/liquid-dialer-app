package com.mandu.liquiddialer.ui
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch
@Composable fun ConverterHistoryPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf("")}
 PageFrame("Converter history controls",onBack){
  LdText("Dobara typing kam. Choice saaf.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Reuse conversion inputs","Default OFF. Confirm restoring an earlier original value and units.",s.converterRecall,last=true){v->scope.launch{try{LdApp.instance.repo.set(K.converterRecall,v);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Convert karo, phir session history mein Reuse inputs kholo. Preview mein original value, From and To units dekho. Keep current cancels. Confirm replaces the current value/category/unit choices and clears current result/table. Convert dobara tap karne par hi new result/history entry banti hai. Rounded old result is never used as input.",14f,color=p.muted,maxLines=20)
  SwitchRow("Fold conversion history layout","Default OFF. Count visible, history rows open on demand.",s.converterHistoryFold,last=true){v->scope.launch{try{LdApp.instance.repo.set(K.converterHistoryFold,v);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Show session history se complete entries khulti hain, Hide folds them. OFF restores the original expanded history. Style change folds the panel but preserves its10 entries and current input/result. Clear session history stays available even while folded. Glass style unchanged, no animation or call-control changes.",14f,color=p.muted,maxLines=16)
  GlassCard{LdText("History belongs only to this open converter. Back/close/app restart clears it. These controls do not save or delete files, copy values, share, call, message or access network. Reuse OFF dismisses an open replacement confirmation. No automatic calculation or conversion chaining. Existing measurement precision and supported-unit limits apply.",13f,maxLines=18)}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5)
 }
}
