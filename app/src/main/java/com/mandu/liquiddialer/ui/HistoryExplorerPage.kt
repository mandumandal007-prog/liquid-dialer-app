package com.mandu.liquiddialer.ui

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.AppState
import com.mandu.liquiddialer.MainActivity
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable fun HistoryExplorerPage(st:AppState,act:MainActivity,onBack:()->Unit,onOpenNumber:(String)->Unit) {
    val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
    val df=remember {SimpleDateFormat("yyyy-MM-dd",Locale.US)}
    val initial=remember {Calendar.getInstance()}
    var from by remember {mutableStateOf(df.format(initial.clone().let {it as Calendar}.apply {add(Calendar.DAY_OF_YEAR,-30)}.time))}
    var to by remember {mutableStateOf(df.format(initial.time))}
    var type by remember {mutableIntStateOf(0)}
    var sim by remember {mutableStateOf<String?>(null)}
    var connected by remember {mutableStateOf(false)}
    var query by remember {mutableStateOf("")}
    var visible by remember(query) {mutableIntStateOf(20)}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var applied by remember {mutableStateOf<HistoryExplorer.Filter?>(null)}
    var next by remember {mutableStateOf<HistoryExplorer.Anchor?>(null)}
    var rows by remember {mutableStateOf<List<Recent>>(emptyList())}
    var searched by remember {mutableStateOf(false)}
    fun load(more:Boolean) {
        if(busy)return
        val f=try {if(more)applied!! else HistoryExplorer.dates(from,to).let {HistoryExplorer.Filter(it.first,it.second,type,sim,connected)}}
        catch(_:Exception){error="Date/range sahi karo: YYYY-MM-DD, shuru se aakhri date tak.";return}
        val anchor=if(more) next else null
        if(more && anchor==null)return
        busy=true;error=""
        scope.launch {
            try {
                val result=withContext(Dispatchers.IO){HistoryExplorer.page(ctx,f,anchor)}
                rows=if(more)rows+result.rows else result.rows;if(!more)visible=20;applied=f;next=result.next;searched=true
            } catch(_:SecurityException){error="Call log permission nahi mili. Permission check karke dobara khojo."}
            catch(_:Exception){error="History load nahi hui. Pehle ke results bache hain; dobara try karo."}
            finally {busy=false}
        }
    }
    val shown=remember(rows,query){rows.filter {HistoryExplorer.matches(it,query)}}
    PageFrame("History Explorer",onBack) {
        LdText("Date, call type, SIM aur connected calls ek saath khojo. Phone ke call log se 100 records ek baar mein padhe jaate hain. Naam/number search sirf neeche load hue records par lagti hai.",12f,color=p.muted,maxLines=6)
        if(!act.has(Manifest.permission.READ_CALL_LOG)) {
            EmptyState(LdIcons.Clock,"Call history ki permission chahiye","Explorer ko call log access do","Permission do"){act.askPermission(Manifest.permission.READ_CALL_LOG)}
            return@PageFrame
        }
        LdText("Date format: YYYY-MM-DD",12f,color=p.muted,modifier=Modifier.padding(top=12.dp,bottom=12.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(from,{if(!busy)from=it},label={Text("Se")},singleLine=true,modifier=Modifier.weight(1f).testTag("history_from"))
            OutlinedTextField(to,{if(!busy)to=it},label={Text("Tak")},singleLine=true,modifier=Modifier.weight(1f).testTag("history_to"))
        }
        LdText("Call type",12f,color=p.muted,modifier=Modifier.padding(top=10.dp))
        val types=listOf(0 to "Sab",1 to "Incoming",2 to "Outgoing",3 to "Missed",5 to "Rejected",6 to "Blocked")
        types.chunked(3).forEach { group -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            group.forEach {(v,label)->PrimaryButton(if(type==v) "[$label]" else label,enabled=!busy,modifier=Modifier.weight(1f).testTag("history_type_$v")){type=v}}
        }}
        PrimaryButton(if(connected) "Connected only: ON" else "Connected only: OFF",enabled=!busy,modifier=Modifier.testTag("history_connected")){connected=!connected}
        LdText("Connected = incoming/outgoing aur duration > 0. Carrier data hai; baat hone ka proof nahi.",11f,color=p.muted,maxLines=3)
        PrimaryButton(if(sim==null) "SIM: sab [selected]" else "Sab SIM chuno",enabled=!busy,modifier=Modifier.testTag("history_sim_all")){sim=null}
        st.sims.forEach {s -> PrimaryButton(if(sim==s.handle.id) "[${s.label}]" else s.label,enabled=!busy,modifier=Modifier.testTag("history_sim_${s.slot}")){sim=s.handle.id}}
        PrimaryButton(if(busy) "Load ho raha hai..." else "Nayi khoj / Refresh",enabled=!busy,modifier=Modifier.testTag("history_search")){load(false)}
        if(error.isNotEmpty()) LdText(error,13f,color=p.danger,maxLines=4,modifier=Modifier.testTag("history_error"))
        if(searched) {
            val f=applied!!
            val end=Calendar.getInstance().apply {timeInMillis=f.end;add(Calendar.DAY_OF_YEAR,-1)}
            LdText("Results: ${df.format(Date(f.start))} se ${df.format(end.time)} · ${types.first {it.first==f.type}.second} · ${if(f.connected) "connected" else "sab durations"} · ${if(f.account==null) "sab SIM" else st.sims.firstOrNull {it.handle.id==f.account}?.label ?: "selected SIM"}",12f,color=p.muted,maxLines=5,modifier=Modifier.testTag("history_applied"))
            OutlinedTextField(query,{query=it.take(100)},label={Text("Loaded records mein naam / digits")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("history_query"))
            LdText("${rows.size} records load hue · ${shown.size} matches",13f,FontWeight.Bold,modifier=Modifier.testTag("history_count"))
            if(next!=null) LdText("Aur records baaki hain. Purana call dhoondhne ke liye agle 100 load karo.",12f,color=p.muted,maxLines=3)
            else LdText("Is khoj ka available provider result khatam. Deleted/hidden records aur doosre apps ki calls nahi mil sakti.",12f,color=p.muted,maxLines=4)
            if(shown.isEmpty()) LdText(if(rows.isEmpty())"Is range/filter mein provider ne koi record nahi diya." else "Load hue records mein match nahi. Search badlo ya aur records load karo.",13f,color=p.muted,maxLines=4)
            shown.take(visible).forEach {r -> GlassCard(Modifier.padding(top=10.dp).testTag("history_row_${r.id}")) {
                val saved=st.contacts.firstOrNull {ContactControls.k(it.number).isNotEmpty() && ContactControls.k(it.number)==ContactControls.k(r.number)}?.name
                LdText(saved ?: r.name ?: r.number.ifBlank {"Private / unknown"},15f,FontWeight.Bold,maxLines=2)
                if(r.number.isNotBlank())LdText(r.number,12f,color=p.muted,maxLines=2)
                LdText("${SimpleDateFormat("d MMM yyyy, h:mm a",Locale.getDefault()).format(Date(r.date))} · ${types.firstOrNull {it.first==r.type}?.second ?: "Other"} · ${r.duration}s",12f,color=p.muted,maxLines=3)
                if(ContactControls.k(r.number).isNotEmpty())Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Call karo",enabled=!busy,modifier=Modifier.weight(1f).testTag("history_call_${r.id}")){act.callUi(r.number)}
                    PrimaryButton("Details",enabled=!busy,modifier=Modifier.weight(1f).testTag("history_details_${r.id}")){onOpenNumber(r.number)}
                }
            }}
            if(shown.size>visible)PrimaryButton("Aur ${minOf(20,shown.size-visible)} matches dikhao",enabled=!busy,modifier=Modifier.testTag("history_show_more")){visible+=20}
            if(next!=null && rows.size<500)PrimaryButton("Agle 100 records",enabled=!busy,modifier=Modifier.testTag("history_more")){load(true)}
            if(next!=null && rows.size>=500)LdText("500-record session limit. Chhota date range/type/SIM chun kar nayi khoj karo. Puri history scan nahi hui.",13f,color=p.muted,maxLines=4)
        }
    }
}
