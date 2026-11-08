package com.mandu.liquiddialer.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable fun ReminderCenterPage(onBack:()->Unit,onOpenNumber:(String)->Unit) {
    val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
    val tick by CallReminders.tick.collectAsState()
    var refresh by remember {mutableIntStateOf(0)}
    var rows by remember {mutableStateOf<List<CallReminders.Reminder>>(emptyList())}
    var loaded by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var query by remember {mutableStateOf("")}
    var visible by remember(query) {mutableIntStateOf(20)}
    var editing by remember {mutableStateOf(false)}
    var selected by remember {mutableStateOf<CallReminders.Reminder?>(null)}
    var cancel by remember {mutableStateOf<CallReminders.Reminder?>(null)}
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_RESUME)refresh++}
        owner.lifecycle.addObserver(observer);onDispose {owner.lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(Unit){while(true){kotlinx.coroutines.delay(60_000);refresh++}}
    LaunchedEffect(tick,refresh) {
        try{rows=withContext(Dispatchers.IO){CallReminders.all(ctx)};loaded=true;error=""}
        catch(_:Exception){loaded=false;error="Reminders khul nahi rahe. Dobara load karo; saved data nahi mitaya gaya."}
    }
    if(editing) {
        ReminderEditor(selected,onDismiss={editing=false},onSaved={editing=false;refresh++})
        return
    }
    val shown=rows.filter {it.name.contains(query,true) || it.number.contains(query) || it.number.filter(Char::isDigit).contains(query.filter(Char::isDigit).takeIf {q->q.isNotEmpty()} ?: "#")}
    PageFrame("Reminder Center",onBack) {
        LdText("Samay lagbhag hoga; battery saver der kar sakta hai. Sirf notification, automatic call nahi. Ek number ka ek reminder; naya samay purane ko badlega.",12f,color=p.muted,maxLines=6)
        LdText("Isi phone par save; uninstall par reminders mitenge.",11.5f,color=p.muted,maxLines=3)
        Spacer(Modifier.height(10.dp))
        if(!androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
            LdText("Notifications band hain. Reminder nahi dikhega.",13f,FontWeight.Bold,p.danger,maxLines=3,modifier=Modifier.testTag("reminder_warning"))
            PrimaryButton("Notification settings") {try{ctx.startActivity(Intent("android.settings.APP_NOTIFICATION_SETTINGS").putExtra("android.provider.extra.APP_PACKAGE",ctx.packageName))}catch(_:Exception){error="Phone ki Settings mein app notifications chalu karo."}}
        }
        if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("reminder_error"))
        PrimaryButton("Naya reminder",enabled=loaded && !busy,modifier=Modifier.testTag("reminder_new")){selected=null;editing=true}
        PrimaryButton("Dobara load karo",enabled=!busy,modifier=Modifier.testTag("reminder_reload")){refresh++}
        ReminderField(query,{query=it.take(100)},"Naam / number dhoondo","reminder_search")
        LdText("${rows.size} upcoming · ${shown.size} match",12f,color=p.muted,modifier=Modifier.testTag("reminder_count"))
        if(loaded && rows.isEmpty())EmptyState(LdIcons.Clock,"Koi upcoming reminder nahi","Naya reminder banao ya number details ke quick presets use karo")
        shown.take(visible).forEach {r->
            GlassCard(Modifier.padding(top=10.dp).testTag("reminder_card_${r.token}")) {
                LdText(r.name.ifBlank {r.number},16f,FontWeight.Bold,maxLines=2)
                LdText(r.number,13f,color=p.muted,maxLines=2)
                LdText(SimpleDateFormat("EEE, d MMM yyyy · HH:mm z",Locale.getDefault()).format(Date(r.atMs)),13f,color=p.accent,maxLines=3)
                PrimaryButton("Samay badlo",enabled=!busy,modifier=Modifier.testTag("reminder_edit_${r.token}")){selected=r;editing=true}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Details",enabled=!busy,modifier=Modifier.weight(1f)){onOpenNumber(r.number)}
                    PrimaryButton("Cancel",enabled=!busy,danger=true,modifier=Modifier.weight(1f).testTag("reminder_cancel_${r.token}")){cancel=r}
                }
            }
        }
        if(shown.size>visible)PrimaryButton("20 aur dikhao"){visible+=20}
    }
    cancel?.let {r->AlertDialog(onDismissRequest={if(!busy)cancel=null},title={Text("Reminder cancel karein?")},text={Text("${r.name.ifBlank {r.number}}\n${r.number}\nSirf reminder hatega, contact nahi.")},confirmButton={TextButton(enabled=!busy,onClick={
        if(!busy){busy=true;scope.launch {try {withContext(Dispatchers.IO){CallReminders.cancel(ctx,r.number)};cancel=null;refresh++}catch(_:Exception){error="Reminder cancel nahi hua. Dobara try karo."}finally{busy=false}}}
    }){Text("Reminder cancel karo")}},dismissButton={TextButton(enabled=!busy,onClick={cancel=null}){Text("Rehne do")}})}
}

@Composable internal fun ReminderEditor(existing:CallReminders.Reminder?,onDismiss:()->Unit,onSaved:()->Unit,initialNumber:String="",initialName:String="",initialAt:Long?=null,contextNotice:String="") {
    val ctx=LocalContext.current;val scope=rememberCoroutineScope()
    val initial=remember {Calendar.getInstance().apply {timeInMillis=existing?.atMs ?: initialAt ?: System.currentTimeMillis()+3600000}}
    var number by remember {mutableStateOf(existing?.number ?: initialNumber)}
    var name by remember {mutableStateOf(existing?.name ?: initialName)}
    var date by remember {mutableStateOf(SimpleDateFormat("yyyy-MM-dd",Locale.US).format(initial.time))}
    var time by remember {mutableStateOf(SimpleDateFormat("HH:mm",Locale.US).format(initial.time))}
    var error by remember {mutableStateOf("")};var busy by remember {mutableStateOf(false)}
    PageFrame(if(existing==null)"Naya reminder" else "Reminder ka samay badlo",{if(!busy)onDismiss()}) {

            if(contextNotice.isNotEmpty())LdText(contextNotice,13f,color=LocalLd.current.accent,maxLines=8)
            LdText("${TimeZone.getDefault().id} · 24-hour time. Samay lagbhag hoga. Call apne aap nahi lagega.",12f,maxLines=4)
            if(existing==null)ReminderField(number,{number=it.take(64)},"Phone number","reminder_number") else LdText(number,14f,maxLines=2)
            ReminderField(name,{name=it.take(100)},"Naam (optional)","reminder_name")
            ReminderField(date,{date=it.take(10)},"Date YYYY-MM-DD","reminder_date")
            ReminderField(time,{time=it.take(5)},"Time HH:mm (24h)","reminder_time")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(enabled=!busy,onClick={DatePickerDialog(ctx,{_,y,m,d->date=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)},initial.get(Calendar.YEAR),initial.get(Calendar.MONTH),initial.get(Calendar.DAY_OF_MONTH)).show()}){Text("Date chuno")}
                TextButton(enabled=!busy,onClick={TimePickerDialog(ctx,{_,h,m->time=String.format(Locale.US,"%02d:%02d",h,m)},initial.get(Calendar.HOUR_OF_DAY),initial.get(Calendar.MINUTE),true).show()}){Text("Time chuno")}
            }
            if(error.isNotEmpty())LdText(error,12f,color=LocalLd.current.danger,maxLines=4,modifier=Modifier.testTag("reminder_editor_error"))
        PrimaryButton(if(busy)"Save ho raha hai…" else "Reminder save karo",enabled=!busy,modifier=Modifier.testTag("reminder_save")) {
        if(!busy) {
            val at=try{require(CallReminders.validNumber(number)){"Sahi phone number likho"};ReminderTime.parse(date,time)}catch(e:IllegalArgumentException){error=e.message ?: "Date/time sahi karo";return@PrimaryButton}
            busy=true;error="";val n=number;val label=name
            scope.launch {try{withContext(Dispatchers.IO){CallReminders.set(ctx,n,label,at)};onSaved()}catch(_:Exception){error="Reminder save nahi hua. Dobara try karo."}finally{busy=false}}
        }
        }
        if(contextNotice.isNotEmpty())Spacer(Modifier.height(8.dp))
        PrimaryButton("Rehne do",enabled=!busy,onClick=onDismiss)
    }
}

@Composable private fun ReminderField(value:String,onValue:(String)->Unit,label:String,tag:String) {
    OutlinedTextField(value=value,onValueChange=onValue,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth().padding(vertical=3.dp).testTag(tag))
}
