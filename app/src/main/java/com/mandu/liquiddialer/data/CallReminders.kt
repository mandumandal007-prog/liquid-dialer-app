package com.mandu.liquiddialer.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mandu.liquiddialer.telecom.CallActionReceiver
import java.util.Calendar
import java.util.UUID
import org.json.JSONObject

/** Manual approximate reminders. No automatic calls; every scheduled generation is checked. */
object CallReminders {
    data class Reminder(val number:String,val name:String,val atMs:Long,val token:String="")
    private val _tick=kotlinx.coroutines.flow.MutableStateFlow(0)
    val tick:kotlinx.coroutines.flow.StateFlow<Int> = _tick
    private fun bump(){_tick.value+=1}
    private fun prefs(ctx:Context)=ctx.getSharedPreferences("call_reminders",Context.MODE_PRIVATE)
    internal fun validNumber(n:String):Boolean {
        val text=n.trim();val digits=text.count {it in '0'..'9'}
        return text.length<=64 && digits in 1..20 && text.all {it in '0'..'9' || it in "+-(). "} &&
            text.count {it=='+'}<=1 && (!text.contains('+') || text.startsWith('+')) && ContactControls.k(text).isNotEmpty()
    }
    private fun key(n:String)=ContactControls.k(n).also {require(validNumber(n)) {"Sahi phone number chahiye"}}
    private fun encode(r:Reminder)=JSONObject().put("number",r.number).put("name",r.name).put("at",r.atMs).put("token",r.token).toString()
    private fun decode(text:String):Reminder?=try {
        val o=JSONObject(text);val n=o.getString("number");val t=o.getString("token")
        if(!validNumber(n) || t.isBlank())null else Reminder(n,o.getString("name"),o.getLong("at"),t)
    }catch(_:Exception){null}
    private fun legacy(text:String):Reminder? {
        if(text.trimStart().startsWith("{"))return null
        val a=text.indexOf('|');val b=text.lastIndexOf('|');if(a<1 || b<=a)return null
        val n=text.substring(0,a);val at=text.substring(b+1).toLongOrNull() ?: return null
        if(!validNumber(n))return null
        return Reminder(n,text.substring(a+1,b),at,UUID.randomUUID().toString())
    }
    private fun raw(ctx:Context)=prefs(ctx).all.mapNotNull {(_,v)->(v as? String)?.let {decode(it)}}.sortedBy {it.atMs}
    private fun pi(ctx:Context,r:Reminder,flags:Int=PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE):PendingIntent? {
        val i=Intent(ctx,CallActionReceiver::class.java).setAction(CallActionReceiver.CALL_REMIND)
            .setData(Uri.Builder().scheme("liquiddialer").authority("call-reminder-v3").appendPath(key(r.number)).appendPath(r.token).build())
            .putExtra("number",r.number).putExtra("token",r.token)
        return PendingIntent.getBroadcast(ctx,700,i,flags)
    }
    private fun arm(ctx:Context,r:Reminder){ctx.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC_WAKEUP,r.atMs,pi(ctx,r)!!)}
    private fun retire(ctx:Context,r:Reminder){
        val operation=pi(ctx,r,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        try{ctx.getSystemService(AlarmManager::class.java).cancel(operation)}finally{operation.cancel()}
    }
    /** Convert all recoverable old values in one durable edit; delimiter-containing names survive.
     * The durable pending flag makes interrupted re-arm retryable. Old broadcasts cannot consume v3. */
    @Synchronized private fun migrate(ctx:Context) {
        val p=prefs(ctx)
        val old=p.all.mapNotNull {(k,v)->val text=v as? String ?: return@mapNotNull null
            if(decode(text)!=null)return@mapNotNull null
            legacy(text)?.let {k to it}}
        if(old.isNotEmpty()) {
            val edit=p.edit()
            old.forEach {(k,_)->edit.remove(k)}
            old.forEach {(_,r)->edit.putString(key(r.number),encode(r))}
            check(edit.putBoolean("v3RearmPending",true).commit()) {"Purane reminders save nahi hue"}
        }
        if(!p.getBoolean("v3RearmPending",false))return
        val am=ctx.getSystemService(AlarmManager::class.java)
        raw(ctx).forEach {r->
            // Past reminders remain for a delayed alarm, up to the existing boot recovery window.
            if(r.atMs>System.currentTimeMillis()-24*60*60_000L)arm(ctx,r)
            val i=Intent(ctx,CallActionReceiver::class.java).setAction(CallActionReceiver.CALL_REMIND)
                .setData(Uri.Builder().scheme("liquiddialer").authority("call-reminder").appendPath(Repo.key(r.number)).build())
            PendingIntent.getBroadcast(ctx,700,i,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {am.cancel(it);it.cancel()}
            val bucket=Intent(ctx,CallActionReceiver::class.java).setAction(CallActionReceiver.CALL_REMIND)
            PendingIntent.getBroadcast(ctx,700+(Repo.key(r.number).hashCode() and 255),bucket,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {am.cancel(it);it.cancel()}
        }
        check(p.edit().putBoolean("v3RearmPending",false).commit())
    }
    @Synchronized fun allRaw(ctx:Context):List<Reminder>{migrate(ctx);return raw(ctx)}
    fun all(ctx:Context)=allRaw(ctx).filter {it.atMs>System.currentTimeMillis()}
    fun forNumber(ctx:Context,number:String)=all(ctx).firstOrNull {ContactControls.k(it.number)==ContactControls.k(number)}
    /** preset -> trigger time. evening: aaj 18:00, nikal gayi to kal 18:00. morning: kal 09:00. */
    fun presetAt(preset: String, nowMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        return when (preset) {
            "30min" -> nowMs + 30 * 60_000L
            "1h" -> nowMs + 60 * 60_000L
            "evening" -> {
                cal.apply { set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                if (cal.timeInMillis <= nowMs) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            else -> { // "morning" = kal subah 9 baje
                cal.apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                cal.timeInMillis
            }
        }
    }


    @Synchronized fun set(ctx:Context,number:String,name:String,atMs:Long) {
        migrate(ctx)
        val k=key(number);val previous=prefs(ctx).getString(k,null)?.let {decode(it)}
        val r=Reminder(number,name,atMs,UUID.randomUUID().toString())
        // New alarm has a separate identity. Failed scheduling cannot corrupt the old generation.
        try {arm(ctx,r)}catch(e:Exception){try{retire(ctx,r)}catch(_:Exception){};throw e}
        if(!prefs(ctx).edit().putString(k,encode(r)).commit()) {
            try{retire(ctx,r)}catch(_:Exception){};error("Reminder save nahi hua")
        }
        if(previous!=null)try{retire(ctx,previous)}catch(_:Exception){} // old token is already invalid
        bump()
    }
    @Synchronized fun cancel(ctx:Context,number:String) {
        migrate(ctx);val k=key(number);val r=prefs(ctx).getString(k,null)?.let {decode(it)} ?: return
        check(prefs(ctx).edit().remove(k).commit()) {"Reminder cancel nahi hua"}
        try{retire(ctx,r)}catch(_:Exception){};bump() // stale token can no longer notify
    }
    fun remove(ctx:Context,number:String)=cancel(ctx,number)
    @Synchronized fun consume(ctx:Context,number:String,token:String):Reminder? {
        migrate(ctx);val k=ContactControls.k(number);if(k.isEmpty() || token.isEmpty())return null
        val r=prefs(ctx).getString(k,null)?.let {decode(it)} ?: return null
        if(r.token!=token)return null
        check(prefs(ctx).edit().remove(k).commit());bump();return r
    }
}
