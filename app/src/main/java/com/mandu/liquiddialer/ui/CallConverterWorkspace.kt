package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
  Box(Modifier.testTag("converter_large_result_switch")){SwitchRow("Larger conversion result","Default OFF. Larger text and spacing for the current result only: converted value, source and rounding note. Swipe the one-line number sideways; target unit stays below. Table/history keep their sizes. No new conversion or history entry.",s.converterLargeResult,last=true,subMaxLines=8){scope.launch{try{LdApp.instance.repo.set(K.converterLargeResult,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}}
  Box(Modifier.testTag("converter_full_units_switch")){SwitchRow("Full conversion unit buttons","Default OFF. Wrap full unit names and symbols in From/To choices. OFF keeps symbol-only buttons. Selection and calculations stay the same.",s.converterFullUnits,last=true,subMaxLines=7){scope.launch{try{LdApp.instance.repo.set(K.converterFullUnits,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}}
  PrimaryButton("Open converter",modifier=Modifier.testTag("converter_settings_open")){open=true}
  LdText("Length, mass, volume, area aur temperature. Feet international definition ke hain, survey foot nahi. Cup/gallon, local bigha aur mass-to-volume conversion nahi; unke meanings alag ho sakte hain.",14f,color=p.muted,maxLines=9)
  LdText("Numbers manually bharo. Decimal arithmetic; result 18 decimal places tak half-up round. No pricing, sensor reading, payment, cloud, clipboard ya saved conversion file. Last 10 conversions sirf is open screen ki memory mein; Back/close/app restart par clear.",13f,color=p.muted,maxLines=12)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4)
 }
}

@Composable fun CallConverterWorkspace(onBack:()->Unit,inSheet:Boolean=false){
 val settings by LdApp.settings.collectAsState()
 val focus=androidx.compose.ui.platform.LocalFocusManager.current
 val p=LocalLd.current;var kind by remember{mutableStateOf(CallConverter.Kind.LENGTH)};var from by remember{mutableStateOf("m")};var to by remember{mutableStateOf("ft")};var input by remember{mutableStateOf("")}
 var result by remember{mutableStateOf<CallConverter.Result?>(null)};var error by remember{mutableStateOf("")};val history=remember{CallConverter.History()};var revision by remember{mutableIntStateOf(0)}
 val rows=remember(revision){history.rows}
 var recall by remember{mutableStateOf<CallConverter.Result?>(null)};var historyOpen by remember{mutableStateOf(false)};var recalled by remember{mutableStateOf(false)}
 LaunchedEffect(settings.converterRecall){if(!settings.converterRecall){recall=null;recalled=false}}
 LaunchedEffect(settings.converterHistoryFold){historyOpen=false}
 var table by remember{mutableStateOf<ConverterTable.Table?>(null)};var tableError by remember{mutableStateOf("")};var fromOpen by remember{mutableStateOf(false)};var toOpen by remember{mutableStateOf(false)}
 LaunchedEffect(settings.converterTable){if(!settings.converterTable){table=null;tableError=""}}
 LaunchedEffect(settings.converterFold,kind){fromOpen=false;toOpen=false}
 fun changed(){result=null;error="";table=null;tableError="";recalled=false;recall=null}
 BackHandler{onBack()}
 val content:@Composable ColumnScope.()->Unit={
  LdText("Typed values only · offline",16f,FontWeight.Bold,maxLines=3)
  LdText("Mass is not volume: kg ko liter mein nahi badalta. Feet international hain. Negative values sirf temperature mein; absolute zero se neeche nahi.",12f,color=p.muted,maxLines=7)
  CallConverter.Kind.entries.chunked(3).forEach{group->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){group.forEach{k->GhostButton((if(kind==k)"✓ "else"")+k.label,Modifier.weight(1f).testTag("converter_kind_${k.name.lowercase()}")){focus.clearFocus();kind=k;val us=CallConverter.of(k);from=us[0].id;to=us[1].id;changed()}}}}
  if(recalled)LdText("Original input and units restored. Check them above/below, then tap Convert. No new result yet.",13f,color=p.accent,maxLines=7,modifier=Modifier.testTag("converter_recalled"))
  LdField(input,{input=it;changed()},"Value (dot decimal)",modifier=Modifier.testTag("converter_input"))
  LdText("From: ${CallConverter.unit(from).label}",14f,FontWeight.Bold,maxLines=3,modifier=Modifier.testTag("converter_from_label"))
  if(settings.converterFold)GhostButton(if(fromOpen)"Hide From units"else"Choose From unit",Modifier.testTag("converter_from_toggle")){fromOpen=!fromOpen}
  if(!settings.converterFold||fromOpen)CallConverter.of(kind).chunked(2).forEach{group->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){group.forEach{u->ConverterUnitButton((if(from==u.id)"✓ "else"")+(if(settings.converterFullUnits)u.label else u.label.substringAfter("(").substringBefore(")")),settings.converterFullUnits,Modifier.weight(1f).semantics{contentDescription=u.label}.testTag("converter_from_${u.id}")){focus.clearFocus();from=u.id;changed()}}}}
  LdText("To: ${CallConverter.unit(to).label}",14f,FontWeight.Bold,maxLines=3,modifier=Modifier.testTag("converter_to_label"))
  if(settings.converterFold)GhostButton(if(toOpen)"Hide To units"else"Choose To unit",Modifier.testTag("converter_to_toggle")){toOpen=!toOpen}
  if(!settings.converterFold||toOpen)CallConverter.of(kind).chunked(2).forEach{group->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){group.forEach{u->ConverterUnitButton((if(to==u.id)"✓ "else"")+(if(settings.converterFullUnits)u.label else u.label.substringAfter("(").substringBefore(")")),settings.converterFullUnits,Modifier.weight(1f).semantics{contentDescription=u.label}.testTag("converter_to_${u.id}")){focus.clearFocus();to=u.id;changed()}}}}
  PrimaryButton("Swap units",modifier=Modifier.testTag("converter_swap")){focus.clearFocus();val old=from;from=to;to=old;changed()}
  LdText("Swap keeps your typed number. It swaps units only, not the value/result.",12f,color=p.muted,maxLines=4)
  PrimaryButton("Convert",enabled=input.isNotEmpty(),modifier=Modifier.testTag("converter_convert")){
   focus.clearFocus();recalled=false
   runCatching{CallConverter.convert(input,from,to)}.fold({result=it;history.add(it);revision++;error=""},{result=null;error=it.message?:"Value sahi bharo"})
  }
  if(settings.converterTable){
   PrimaryButton("Compare all same-category units",enabled=input.isNotEmpty(),modifier=Modifier.testTag("converter_table_make")){
    focus.clearFocus();if(LdApp.settings.value.converterTable)runCatching{ConverterTable.create(input,from)}.fold({table=it;tableError=""},{table=null;tableError=it.message?:"Check value"})
   }
   LdText("Explicit table only, not added to session history. Uses From unit; To choice does not limit the table. Same-category units only, no pricing or mass-to-volume conversion.",12f,color=p.muted,maxLines=7)
   if(tableError.isNotEmpty())LdText(tableError,13f,color=p.danger,maxLines=7,modifier=Modifier.testTag("converter_table_error"))
   table?.let{t->
    LdText("${t.rows.size} equivalent unit values",18f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("converter_table_count"))
    LdText("${t.input} ${t.from.label}. Display up to18 decimals, half-up; check measurement precision. No new measurement or guarantee.",12f,color=p.muted,maxLines=7)
    t.rows.forEach{r->GlassCard(Modifier.fillMaxWidth().testTag("converter_table_row_${r.to.id}")){
     LdText(r.to.label,14f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
     LdText(r.value,21f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("converter_table_value_${r.to.id}"))
    }}
   }
  }
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("converter_error"))
  result?.let{r->GlassCard(Modifier.fillMaxWidth().padding(vertical=8.dp).testTag("converter_result")){
   LdText("${r.input} ${r.from.label}",if(settings.converterLargeResult)16f else 14f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("converter_result_source"))
   if(settings.converterLargeResult)Spacer(Modifier.height(8.dp))
   if(settings.converterLargeResult){
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("converter_value_scroll")){LdText("= ${r.value}",27f,FontWeight.Bold,maxLines=1,modifier=Modifier.testTag("converter_value"))}
    LdText(r.to.label,16f,FontWeight.SemiBold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("converter_result_unit"))
    LdText("Swipe number sideways if needed",12f,color=p.muted,maxLines=4)
   }else LdText("= ${r.value} ${r.to.label}",21f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("converter_value"))
   if(settings.converterLargeResult)Spacer(Modifier.height(8.dp))
   LdText("Display: up to 18 decimals, half-up rounded. Measurement precision is yours to check.",if(settings.converterLargeResult)14f else 12f,color=p.muted,maxLines=if(settings.converterLargeResult)Int.MAX_VALUE else 5,modifier=Modifier.testTag("converter_result_rounding"))
  }}
  GhostButton("Clear input & result",Modifier.testTag("converter_clear")){focus.clearFocus();input="";changed()}
  LdText("Session history: ${rows.size} / 10",14f,FontWeight.Bold,modifier=Modifier.testTag("converter_history_count"))
  LdText("Sirf is open screen ki memory. Back/close se ye entries chali jayengi. No clipboard, share, file ya call action.",12f,color=p.muted,maxLines=6)
  if(settings.converterHistoryFold)GhostButton(if(historyOpen)"Hide session history"else"Show session history",Modifier.testTag("converter_history_toggle")){historyOpen=!historyOpen}
  if(!settings.converterHistoryFold||historyOpen)rows.forEachIndexed{i,r->GlassCard(Modifier.fillMaxWidth().padding(top=6.dp).testTag("converter_history_$i")){
   LdText("${r.input} ${r.from.label} = ${r.value} ${r.to.label}",14f,maxLines=Int.MAX_VALUE)
   if(settings.converterRecall)GhostButton("Reuse inputs",Modifier.testTag("converter_recall_$i")){focus.clearFocus();recall=r}
  }}
  GhostButton("Clear session history",Modifier.testTag("converter_history_clear")){history.clear();revision++;recall=null}
 }
 if(settings.converterRecall)recall?.let{r->androidx.compose.ui.window.Dialog(onDismissRequest={recall=null}){GlassCard{
  LdText("Replace converter input and units?",19f,FontWeight.Bold,maxLines=6)
  LdText("Original: ${r.input} ${r.from.label} → ${r.to.label}. This replaces your current typed value and both unit choices. Current result/table will clear. History stays; nothing calculates automatically. The old rounded result is NOT used as input.",13f,color=p.muted,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("converter_recall_summary"))
  PrimaryButton("Use original input & units",modifier=Modifier.testTag("converter_recall_confirm")){
   if(LdApp.settings.value.converterRecall&&r in history.rows)runCatching{ConverterRecall.input(r)}.fold({v->focus.clearFocus();kind=v.kind;from=v.from;to=v.to;input=v.value;changed();recalled=true},{recall=null;error=it.message?:"History entry invalid"})else recall=null
  }
  GhostButton("Keep current input",Modifier.testTag("converter_recall_cancel")){recall=null}
 }}}
 if(inSheet)Column{SheetTitle("Unit converter");PrimaryButton("Notes par wapas",modifier=Modifier.testTag("converter_back")){onBack()};content()}else PageFrame("Unit converter",onBack,content=content)
}

@Composable private fun ConverterUnitButton(text:String,full:Boolean,modifier:Modifier,onClick:()->Unit){
 if(!full){GhostButton(text,modifier,onClick=onClick);return}
 val p=LocalLd.current
 androidx.compose.foundation.layout.Box(modifier.buttonGlass(p,androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).then(Modifier.clickable(onClick=onClick)).padding(horizontal=10.dp,vertical=13.dp),contentAlignment=androidx.compose.ui.Alignment.Center){LdText(text,14f,FontWeight.SemiBold,p.accent,maxLines=Int.MAX_VALUE)}
}
