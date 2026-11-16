package com.mandu.liquiddialer.ui
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch
@Composable fun ConverterOptionsPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf("")}
 PageFrame("Converter table & chooser layout",onBack){
  LdText("Ek value. Saare supported units.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Converter all-units table","Default OFF. Manual table inside the existing unit converter.",s.converterTable,last=true){v->scope.launch{try{LdApp.instance.repo.set(K.converterTable,v);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Unit converter kholo, category, From unit aur value chuno. Compare all same-category units tap karo. Table uses From unit, not To unit. Includes all supported units in that category, including source. Existing Convert button and history stay separate. Edits clear stale table; OFF/close clears it. No table saved to history or file.",14f,color=p.muted,maxLines=18)
  SwitchRow("Fold unit chooser layout","Default OFF. Open From/To choices only when needed.",s.converterFold,last=true){v->scope.launch{try{LdApp.instance.repo.set(K.converterFold,v);error=""}catch(_:Exception){error="Setting save failed"}}}
  LdText("Full selected From and To labels stay visible. Choose From/To unit opens that group; Hide folds it. OFF restores original always-visible unit buttons. Category or layout change folds groups, not your input, result, table or history. Glass style retained; no animation or call-control changes.",14f,color=p.muted,maxLines=16)
  GlassCard{LdText("Uses existing offline decimal arithmetic, up to18 displayed decimals half-up. International feet only; no local land-unit or cup/gallon assumptions. No pricing, currency, mass-to-volume, clock, sensor, clipboard, call/message, network or new permission. Negative values only for temperature above absolute zero. Measurement precision is yours to check.",13f,maxLines=20)}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5)
 }
}
