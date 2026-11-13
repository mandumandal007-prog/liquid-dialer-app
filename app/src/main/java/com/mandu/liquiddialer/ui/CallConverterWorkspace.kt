package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun CallConverterPage(onBack:()->Unit){
 val p=LocalLd.current;val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();var error by remember{mutableStateOf("")};var open by remember{mutableStateOf(false)}
 if(open){CallConverterWorkspace({open=false});return}
 PageFrame("Call unit converter",onBack){
  LdText("Baat ke beech unit ka hisab.",23f,FontWeight.Bold,maxLines=4)
  SwitchRow("Show unit converter in call Notes","Default OFF. Offline typed values only; call audio nahi padhta.",s.callConverter,last=true){scope.launch{try{LdApp.instance.repo.set(K.callConverter,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  PrimaryButton("Open converter",modifier=Modifier.testTag("converter_settings_open")){open=true}
  LdText("Length, mass, volume, area aur temperature. Feet international definition ke hain, survey foot nahi. Cup/gallon, local bigha aur mass-to-volume conversion nahi; unke meanings alag ho sakte hain.",14f,color=p.muted,maxLines=9)
  LdText("Numbers manually bharo. Decimal arithmetic; result 18 decimal places tak half-up round. No pricing, sensor reading, payment, cloud, clipboard ya saved conversion file. Last 10 conversions sirf is open screen ki memory mein; Back/close/app restart par clear.",13f,color=p.muted,maxLines=12)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}

@Composable fun CallConverterWorkspace(onBack:()->Unit,inSheet:Boolean=false){
 val focus=androidx.compose.ui.platform.LocalFocusManager.current
 val p=LocalLd.current;var kind by remember{mutableStateOf(CallConverter.Kind.LENGTH)};var from by remember{mutableStateOf("m")};var to by remember{mutableStateOf("ft")};var input by remember{mutableStateOf("")}
 var result by remember{mutableStateOf<CallConverter.Result?>(null)};var error by remember{mutableStateOf("")};val history=remember{CallConverter.History()};var revision by remember{mutableIntStateOf(0)}
 val rows=remember(revision){history.rows}
 fun changed(){result=null;error=""}
 BackHandler{onBack()}
 val content:@Composable ColumnScope.()->Unit={
  LdText("Typed values only · offline",16f,FontWeight.Bold,maxLines=3)
  LdText("Mass is not volume: kg ko liter mein nahi badalta. Feet international hain. Negative values sirf temperature mein; absolute zero se neeche nahi.",12f,color=p.muted,maxLines=7)
  CallConverter.Kind.entries.chunked(3).forEach{group->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){group.forEach{k->GhostButton((if(kind==k)"✓ "else"")+k.label,Modifier.weight(1f).testTag("converter_kind_${k.name.lowercase()}")){focus.clearFocus();kind=k;val us=CallConverter.of(k);from=us[0].id;to=us[1].id;changed()}}}}
  LdField(input,{input=it.take(24);changed()},"Value (dot decimal)",modifier=Modifier.testTag("converter_input"))
  LdText("From: ${CallConverter.unit(from).label}",14f,FontWeight.Bold,maxLines=3,modifier=Modifier.testTag("converter_from_label"))
  CallConverter.of(kind).chunked(2).forEach{group->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){group.forEach{u->GhostButton((if(from==u.id)"✓ "else"")+u.label.substringAfter("(").substringBefore(")"),Modifier.weight(1f).semantics{contentDescription=u.label}.testTag("converter_from_${u.id}")){focus.clearFocus();from=u.id;changed()}}}}
  LdText("To: ${CallConverter.unit(to).label}",14f,FontWeight.Bold,maxLines=3,modifier=Modifier.testTag("converter_to_label"))
  CallConverter.of(kind).chunked(2).forEach{group->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){group.forEach{u->GhostButton((if(to==u.id)"✓ "else"")+u.label.substringAfter("(").substringBefore(")"),Modifier.weight(1f).semantics{contentDescription=u.label}.testTag("converter_to_${u.id}")){focus.clearFocus();to=u.id;changed()}}}}
  PrimaryButton("Swap units",modifier=Modifier.testTag("converter_swap")){focus.clearFocus();val old=from;from=to;to=old;changed()}
  LdText("Swap keeps your typed number. It swaps units only, not the value/result.",12f,color=p.muted,maxLines=4)
  PrimaryButton("Convert",enabled=input.isNotEmpty(),modifier=Modifier.testTag("converter_convert")){
   focus.clearFocus()
   runCatching{CallConverter.convert(input,from,to)}.fold({result=it;history.add(it);revision++;error=""},{result=null;error=it.message?:"Value sahi bharo"})
  }
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("converter_error"))
  result?.let{r->GlassCard(Modifier.fillMaxWidth().padding(vertical=8.dp).testTag("converter_result")){
   LdText("${r.input} ${r.from.label}",14f,maxLines=Int.MAX_VALUE)
   LdText("= ${r.value} ${r.to.label}",21f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("converter_value"))
   LdText("Display: up to 18 decimals, half-up rounded. Measurement precision is yours to check.",12f,color=p.muted,maxLines=5)
  }}
  GhostButton("Clear input & result",Modifier.testTag("converter_clear")){focus.clearFocus();input="";changed()}
  LdText("Session history: ${rows.size} / 10",14f,FontWeight.Bold,modifier=Modifier.testTag("converter_history_count"))
  LdText("Sirf is open screen ki memory. Back/close se ye entries chali jayengi. No clipboard, share, file ya call action.",12f,color=p.muted,maxLines=6)
  rows.forEachIndexed{i,r->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp).testTag("converter_history_$i")){
   LdText("${r.input} ${r.from.label} = ${r.value} ${r.to.label}",14f,maxLines=Int.MAX_VALUE)
  }}
  GhostButton("Clear session history",Modifier.testTag("converter_history_clear")){history.clear();revision++}
 }
 if(inSheet)Column{SheetTitle("Unit converter");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("converter_back")){onBack()};content()}else PageFrame("Unit converter",onBack,content=content)
}
