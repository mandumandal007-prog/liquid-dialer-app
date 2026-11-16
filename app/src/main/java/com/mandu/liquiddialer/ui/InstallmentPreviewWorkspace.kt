package com.mandu.liquiddialer.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun InstallmentPreviewPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Calculator installment preview",onBack){
  LdText("Estimate ko manual parts mein dekho.",22f,FontWeight.Bold,maxLines=5)
  SwitchRow("Calculator installment preview","Default OFF. A temporary arithmetic preview inside Call calculator.",s.installmentPreview,last=true){v->scope.launch{try{LdApp.instance.repo.set(K.installmentPreview,v);error=false}catch(_:Exception){error=true}}}
  Column(Modifier.testTag("installment_options_settings")){
   SwitchRow("Find preview part","Default OFF. Show one hypothetical dated part by number; totals stay unchanged.",s.installmentFind,last=true,subMaxLines=5){scope.launch{try{LdApp.instance.repo.set(K.installmentFind,it);error=false}catch(_:Exception){error=true}}}
   SwitchRow("Fold dated parts layout","Default OFF. Totals and finder stay visible; dated rows open on demand.",s.installmentPartsFold,last=true,subMaxLines=5){scope.launch{try{LdApp.instance.repo.set(K.installmentPartsFold,it);error=false}catch(_:Exception){error=true}}}
  }
  LdText("Find part only narrows the visible rows. Preview amounts/dates, totals and calculator stay unchanged. Edit the finder or Show all parts to see every row; invalid part number shows an error and all rows. Finder OFF clears its view. Folding keeps totals/count/finder available and preserves the preview. Find opens a folded list. Input edits or a new preview reset the finder and fold rows. No paid status, agreement, payment or scheduling.",13f,color=p.muted,maxLines=20)
  LdText("Call calculator mein items jodo, phir Installment preview kholo. Hypothetical upfront amount, 1-36 remaining parts, first date and fixed calendar-day interval enter karo. Exact two-decimal amounts plus weekdays; total unchanged.",14f,color=p.muted,maxLines=14)
  GlassCard{LdText("No actual paid/unpaid status, interest, fees, loan/EMI advice or agreement. Not monthly installments: every N calendar days, weekends/holidays included. No automatic today, saved debt, calendar, reminder, message, payment, clipboard or network. Same currency only. Inputs/preview temporary; return, close or OFF clears them.",13f,maxLines=20)}
  if(error)LdText("Setting save failed",13f,color=p.danger)
 }
}
@Composable fun InstallmentPreviewWorkspace(number:String,base:CallEstimate.Draft,onBack:()->Unit,inSheet:Boolean=false){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val focus=androidx.compose.ui.platform.LocalFocusManager.current
 if(!s.installmentPreview){if(inSheet)Column{LdText("Installment preview is OFF",14f);PrimaryButton("Back",onClick=onBack)}else PageFrame("Installment preview",onBack){LdText("Installment preview is OFF",14f)};return}
 var upfront by remember(number,base){mutableStateOf("0")};var count by remember(number,base){mutableStateOf("3")};var first by remember(number,base){mutableStateOf("")};var interval by remember(number,base){mutableStateOf("30")}
 var result by remember(number,base){mutableStateOf<InstallmentPreview.Preview?>(null)};var error by remember(number,base){mutableStateOf("")}
 var partQuery by remember(number,base){mutableStateOf("")};var selectedPart by remember(number,base){mutableStateOf<Int?>(null)};var findError by remember(number,base){mutableStateOf("")};var partsOpen by remember(number,base){mutableStateOf(false)}
 fun resetView(){partQuery="";selectedPart=null;findError="";partsOpen=false}
 LaunchedEffect(s.installmentFind){if(!s.installmentFind){partQuery="";selectedPart=null;findError=""}}
 LaunchedEffect(s.installmentPartsFold){partsOpen=false}
 fun changed(){result=null;error="";resetView()}
 fun back(){focus.clearFocus();onBack()}
 BackHandler{back()}
 val content:@Composable ColumnScope.()->Unit={
  PrimaryButton("Return to calculator",modifier=Modifier.testTag("installment_back")){back()}
  LdText("INSTALLMENT PREVIEW",16f,FontWeight.Bold,p.accent,maxLines=4)
  LdText(number,14f,maxLines=4)
  LdText("Hypothetical arithmetic only, not a payment agreement or paid status. No interest or fees included. Every N calendar days, NOT monthly; weekends and holidays included.",13f,color=p.muted,maxLines=10)
  runCatching{CallEstimate.totals(base).total}.getOrNull()?.let{LdText("Estimate: ${CallEstimate.money(it)}",20f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("installment_base"))}
  LdText("Hypothetical upfront amount",13f,FontWeight.Bold,maxLines=4)
  LdField(upfront,{upfront=it;changed()},"Hypothetical upfront amount",modifier=Modifier.testTag("installment_upfront"))
  LdText("Remaining parts: 1-36",13f,FontWeight.Bold,maxLines=4)
  LdField(count,{count=it;changed()},"Remaining parts: 1-36",modifier=Modifier.testTag("installment_count"))
  LdText("First part date YYYY-MM-DD",13f,FontWeight.Bold,maxLines=4)
  LdField(first,{first=it;changed()},"First part date YYYY-MM-DD",modifier=Modifier.testTag("installment_first"))
  LdText("Interval in calendar days: 1-3650",13f,FontWeight.Bold,maxLines=4)
  LdField(interval,{interval=it;changed()},"Interval in calendar days: 1-3650",modifier=Modifier.testTag("installment_interval"))
  LdText("First date is part 1, not an upfront date. No automatic today. All dates 1900-2199. Upfront must be below total; each remaining part at least 0.01. Inputs are temporary.",12f,color=p.muted,maxLines=9)
  PrimaryButton("Preview dated parts",enabled=first.isNotBlank()&&upfront.isNotBlank()&&count.isNotBlank()&&interval.isNotBlank(),modifier=Modifier.testTag("installment_preview")){
   focus.clearFocus();resetView();if(LdApp.settings.value.installmentPreview)runCatching{InstallmentPreview.preview(base,upfront,count,first,interval)}.fold({result=it;error=""},{result=null;error=it.message?:"Check inputs"})
  }
  if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("installment_error"))
  result?.let{r->
   GlassCard(Modifier.testTag("installment_result")){
    LdText("${r.parts.size} dated parts",22f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("installment_value"))
    LdText("Hypothetical upfront: ${CallEstimate.money(r.upfront)}\nRemaining: ${CallEstimate.money(r.remaining)}\nUpfront + all parts = ${CallEstimate.money(r.total)}",15f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("installment_sum"))
    LdText("Every ${r.interval} calendar days. Rounded down to 2 decimals; leftover 0.01 units go to earliest parts. No interest/fees, weekend or holiday adjustment.",12f,color=p.muted,maxLines=9)
   }
   LdText(if(selectedPart==null)"View: all ${r.parts.size} dated parts"else"View: part $selectedPart of ${r.parts.size}. Totals include ALL parts.",14f,FontWeight.Bold,maxLines=6,modifier=Modifier.testTag("installment_view_count"))
   if(s.installmentFind){
    LdText("View one part only. No paid status; amounts, dates and totals do not change.",12f,color=p.muted,maxLines=6)
    LdField(partQuery,{partQuery=it.take(20);selectedPart=null;findError=""},"Part number 1-${r.parts.size}",modifier=Modifier.testTag("installment_find_input"))
    GhostButton("Find part",Modifier.testTag("installment_find_apply")){
     focus.clearFocus();runCatching{InstallmentPartView.index(partQuery,r.parts.size)}.fold({selectedPart=it;findError="";partsOpen=true},{selectedPart=null;findError=it.message?:"Check part number";partsOpen=true})
    }
    GhostButton("Show all parts",Modifier.testTag("installment_find_clear")){focus.clearFocus();partQuery="";selectedPart=null;findError="";partsOpen=true}
    if(findError.isNotEmpty())LdText(findError+". Showing all rows; preview unchanged.",13f,color=p.danger,maxLines=7,modifier=Modifier.testTag("installment_find_error"))
   }
   if(s.installmentPartsFold)GhostButton(if(partsOpen)"Hide dated parts"else"Show dated parts",Modifier.testTag("installment_parts_toggle")){partsOpen=!partsOpen}
   if(!s.installmentPartsFold||partsOpen)InstallmentPartView.rows(r.parts,selectedPart).forEach{row->GlassCard(Modifier.testTag("installment_row_${row.index}")){
    LdText("Part ${row.index} · ${CallDates.label(row.date)}",16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
    LdText(CallEstimate.money(row.amount),22f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("installment_amount_${row.index}"))
    if(row.extraCent)LdText("Includes one leftover 0.01",12f,color=p.muted,maxLines=4)
   }}
  }
  GhostButton("Clear preview and inputs",Modifier.testTag("installment_clear")){focus.clearFocus();upfront="0";count="3";first="";interval="30";changed()}
  LdText("No Save or Apply. Calculator items, unfinished edits, discount and tax stay unchanged. Nothing is scheduled, sent, charged or copied. Return/close/OFF clears this preview.",12f,color=p.muted,maxLines=9)
 }
 if(inSheet)Column{SheetTitle("Installment preview");content()}else PageFrame("Installment preview",onBack,content=content)
}
