package com.mandu.liquiddialer.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun CsvCallImportPage(onBack:()->Unit,onSaved:(String)->Unit,initialText:String?=null){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var raw by remember{mutableStateOf(initialText)};var separator by remember{mutableStateOf(',')}
 var table by remember{mutableStateOf<CsvCallImport.Table?>(null)};var phone by remember{mutableIntStateOf(-1)}
 var name by remember{mutableStateOf<Int?>(null)};var header by remember{mutableStateOf(true)}
 var review by remember{mutableStateOf<List<CsvCallImport.Row>?>(null)};var selected by remember{mutableStateOf<Set<Int>>(emptySet())}
 var title by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 var shown by remember{mutableIntStateOf(20)};var menu by remember{mutableStateOf("")}
 fun invalidate(){review=null;selected=emptySet();shown=20;error=""}
 fun parse(){invalidate();phone=-1;name=null;table=null;try{table=CsvCallImport.parse(raw.orEmpty(),separator)}catch(e:Exception){error=e.message ?: "CSV padh nahi paaye"}}
 LaunchedEffect(initialText){if(initialText!=null)parse()}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null){busy=true;raw=null;table=null;invalidate();scope.launch{
   try{raw=withContext(Dispatchers.IO){ctx.contentResolver.openInputStream(uri)?.use{CsvCallImport.read(it)} ?: error("File khul nahi paayi")};parse()}
   catch(_:Exception){error="UTF-8 CSV khul nahi paayi. 128 KB limit; Excel mein CSV UTF-8 export karo. Koi list nahi badli."}
   finally{busy=false}
  }}
 }
 BackHandler{if(!busy)onBack()}
 PageFrame("CSV se calling list",{if(!busy)onBack()}){
  if(review==null){
  LdText("Spreadsheet ke numbers ko ek nayi private Call List mein lao. File chuno, columns milao, har row dekho aur apni rows save karo. Koi automatic call, contact edit ya upload nahi.",13f,color=p.muted,maxLines=7)
  LdText("CSV UTF-8 only, XLSX/PDF nahi. 128 KB, 501 total CSV rows, 20 columns. Ek nayi list mein 200 rows tak. Number ko text rakho: Excel se ghate zero/digits app wapas nahi la sakta.",12f,color=p.muted,maxLines=7)
  PrimaryButton(if(busy)"Kaam chal raha hai..." else "CSV file chuno",enabled=!busy,modifier=Modifier.testTag("csv_pick")){picker.launch(arrayOf("text/*","application/csv","application/vnd.ms-excel","application/octet-stream"))}
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=7,modifier=Modifier.testTag("csv_error"))
  if(raw!=null){
   SecTitle("Separator")
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
    listOf(',' to "Comma",';' to "Semicolon",'\t' to "Tab").forEach{(c,label)->PrimaryButton((if(separator==c)"✓ "else "")+label,enabled=!busy,modifier=Modifier.weight(1f).testTag("csv_sep_${c.code}")){separator=c;parse()}}
   }
  }
  table?.let{t->
   LdText("${t.rows.size} CSV rows · ${t.columns} columns",13f,FontWeight.Bold,modifier=Modifier.testTag("csv_shape"))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){LdText("Pehli row headings hai",14f,modifier=Modifier.weight(1f));Switch(header,{header=it;invalidate()},enabled=!busy,modifier=Modifier.testTag("csv_header"))}
   LdText("Pehli 3 rows: column number se neeche sahi Phone/Naam chuno. Quoted comma aur quoted multi-line cells padhte hain; formulas chalate nahi.",12f,color=p.muted,maxLines=5)
   t.rows.take(3).forEachIndexed{i,row->GlassCard(Modifier.padding(top=6.dp)){LdText("CSV row ${i+1}",12f,FontWeight.Bold);row.forEachIndexed{j,v->LdText("${j+1}: "+v.ifBlank{"(khaali)"},12f,maxLines=3)}}}
   fun label(index:Int?):String=if(index==null)"Koi nahi" else if(index<0)"Chuno" else "${index+1}: "+(if(header)t.rows.first().getOrNull(index).orEmpty()else "Column ${index+1}").take(35)
   @Composable fun columnPicker(kind:String,value:Int?){
    Box{PrimaryButton((if(kind=="phone")"Phone: "else "Naam: ")+label(value),enabled=!busy,modifier=Modifier.testTag("csv_column_$kind")){menu=kind}
     DropdownMenu(menu==kind,{menu=""}){
      if(kind=="name")DropdownMenuItem(text={Text("Koi naam column nahi")},modifier=Modifier.testTag("csv_name_none"),onClick={name=null;menu="";invalidate()})
      (0 until t.columns).forEach{i->DropdownMenuItem(text={Text(label(i))},modifier=Modifier.testTag("csv_${kind}_$i"),onClick={if(kind=="phone")phone=i else name=i;menu="";invalidate()})}
     }
    }
   }
   columnPicker("phone",phone);columnPicker("name",name)
   PrimaryButton("Rows ki jaanch karo",enabled=!busy&&phone>=0,modifier=Modifier.testTag("csv_review")){
    try{review=CsvCallImport.preview(t,phone,name,header);selected=emptySet();shown=20;error=""}catch(e:Exception){review=null;error=e.message ?: "Columns sahi chuno"}
   }
  }
  }
  review?.let{rows->
   PrimaryButton("Columns / file dobara chuno",enabled=!busy,modifier=Modifier.testTag("csv_remap")){invalidate()}
   val ready=rows.filter{it.status==CsvCallImport.Status.READY}
   if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=7,modifier=Modifier.testTag("csv_error"))
   SecTitle("Save se pehle review")
   LdText("${ready.size} valid · ${rows.count{it.status==CsvCallImport.Status.INVALID}} invalid · ${rows.count{it.status==CsvCallImport.Status.DUPLICATE}} duplicate",14f,FontWeight.Bold,maxLines=3,modifier=Modifier.testTag("csv_counts"))
   LdText("Duplicate sirf isi file ke andar hain; purani lists se nahi hatate. Invalid/duplicate rows save nahi hoti. Sahi dikh raha number bhi active ya sahi insaan ka hai, yeh jaanch nahi.",12f,color=p.muted,maxLines=6)
   PrimaryButton("Pehle 200 valid chuno",enabled=!busy&&ready.isNotEmpty(),modifier=Modifier.testTag("csv_select_valid")){selected=ready.take(200).map{it.record}.toSet()}
   PrimaryButton("Selection saaf karo",enabled=!busy&&selected.isNotEmpty(),modifier=Modifier.testTag("csv_clear")){selected=emptySet()}
   LdText("${selected.size} rows chuni · max 200",14f,FontWeight.Bold,modifier=Modifier.testTag("csv_selected"))
   rows.take(shown).forEach{r->GlassCard(Modifier.padding(top=8.dp).testTag("csv_row_${r.record}")){
    LdText("Row ${r.record} · "+when(r.status){CsvCallImport.Status.READY->"Valid";CsvCallImport.Status.INVALID->"Invalid";CsvCallImport.Status.DUPLICATE->"Duplicate"},12f,FontWeight.Bold)
    LdText(r.number.ifBlank{"(number khaali)"},15f,FontWeight.Bold,maxLines=3);if(r.name.isNotBlank())LdText(r.name,13f,maxLines=3)
    if(r.reason.isNotEmpty())LdText(r.reason,12f,color=p.danger,maxLines=4)
    if(r.status==CsvCallImport.Status.READY)Row{Checkbox(r.record in selected,{on->selected=if(on)selected+r.record else selected-r.record},enabled=!busy&&(r.record in selected||selected.size<200),modifier=Modifier.testTag("csv_check_${r.record}"));LdText("Is row ko save karo",13f,modifier=Modifier.padding(top=14.dp))}
   }}
   if(rows.size>shown)PrimaryButton("20 aur rows dekho",enabled=!busy,modifier=Modifier.testTag("csv_more")){shown+=20}
   OutlinedTextField(title,{title=it.take(60)},label={Text("Nayi list ka naam")},enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("csv_title"))
   LdText("Save par sirf chuni rows ki ek nayi list banegi. Purani lists/progress aur phone contacts wahi rahenge. Back karoge to bina save ke chhod sakte ho.",12f,color=p.muted,maxLines=5)
   PrimaryButton("${selected.size} rows ki nayi list save karo",enabled=!busy&&title.isNotBlank()&&selected.size in 1..200,modifier=Modifier.testTag("csv_save")){
    if(!busy){val chosen=ready.filter{it.record in selected};val savedTitle=title;busy=true;error="";scope.launch{
     try{val id=withContext(Dispatchers.IO){CallLists.createFromCsv(ctx,savedTitle,chosen).last().id};onSaved(id)}catch(e:Exception){error=(e as? IllegalArgumentException)?.message ?: "List save nahi hui; purana data nahi badla"}finally{if(error.isNotEmpty())busy=false}
    }}
   }
  }
 }
}
