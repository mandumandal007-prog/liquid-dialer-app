package com.mandu.liquiddialer.data

import android.content.Context
import android.provider.CallLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Read-only, bounded keyset pages. Null cursor/errors never become an empty-history claim. */
object HistoryExplorer {
    data class Filter(val start:Long,val end:Long,val type:Int=0,val account:String?=null,val connected:Boolean=false)
    data class Anchor(val date:Long,val id:Long)
    data class Page(val rows:List<Recent>,val next:Anchor?)
    data class Selection(val clause:String,val args:Array<String>)
    fun dates(from:String,to:String):Pair<Long,Long> {
        val f=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply {isLenient=false}
        require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(from) && Regex("\\d{4}-\\d{2}-\\d{2}").matches(to)) {"Date YYYY-MM-DD mein daalo"}
        val a=f.parse(from)?.time ?: error("date");val b=f.parse(to)?.time ?: error("date")
        require(a<=b) {"Shuru ki date aakhri date ke baad nahi ho sakti"}
        val end=Calendar.getInstance().apply {timeInMillis=b;add(Calendar.DAY_OF_YEAR,1)}.timeInMillis
        return a to end
    }
    fun selection(f:Filter,after:Anchor?):Selection {
        require(f.start<f.end && f.type in setOf(0,1,2,3,5,6))
        val clauses=mutableListOf("date>=?","date<?");val args=mutableListOf("${f.start}","${f.end}")
        if(f.type!=0){clauses+="type=?";args+="${f.type}"}
        if(f.account!=null){clauses+="${CallLog.Calls.PHONE_ACCOUNT_ID}=?";args+=f.account}
        if(f.connected){clauses+="type IN (1,2) AND duration>0"}
        if(after!=null){clauses+="(date<? OR (date=? AND _id<?))";args+=listOf("${after.date}","${after.date}","${after.id}")}
        return Selection(clauses.joinToString(" AND "){"($it)"},args.toTypedArray())
    }
    fun matches(r:Recent,query:String):Boolean {
        val q=query.trim();if(q.isEmpty())return true
        val digits=q.filter {it.isDigit()}
        return r.name?.contains(q,true)==true || (digits.isNotEmpty() && q.all {it.isDigit() || it in "+-(). "} && r.number.filter {it.isDigit()}.contains(digits))
    }
    fun page(ctx:Context,f:Filter,after:Anchor?):Page {
        val s=selection(f,after)
        val columns=arrayOf(CallLog.Calls._ID,CallLog.Calls.NUMBER,CallLog.Calls.CACHED_NAME,CallLog.Calls.TYPE,CallLog.Calls.DATE,CallLog.Calls.DURATION,CallLog.Calls.PHONE_ACCOUNT_ID,CallLog.Calls.CACHED_PHOTO_URI,CallLog.Calls.NUMBER_PRESENTATION)
        val uri=CallLog.Calls.CONTENT_URI.buildUpon().appendQueryParameter("limit","101").build()
        val out=mutableListOf<Recent>()
        val cursor=ctx.contentResolver.query(uri,columns,s.clause,s.args,"date DESC, _id DESC") ?: error("Call log provider ne result nahi diya")
        cursor.use {c ->
            while(out.size<101 && c.moveToNext()) {
                val allowed=c.getInt(8)==android.telecom.TelecomManager.PRESENTATION_ALLOWED
                out+=Recent(c.getLong(0),if(allowed)c.getString(1).orEmpty() else "",if(allowed)c.getString(2)?.takeIf {it.isNotBlank()} else null,c.getInt(3),c.getLong(4),c.getLong(5),c.getString(6),if(allowed)c.getString(7) else null)
            }
        }
        val rows=out.take(100)
        // Refuse provider order errors rather than silently skip or duplicate history.
        require(out.zipWithNext().all {(a,b)->a.date>b.date || (a.date==b.date && a.id>b.id)}) {"Call log ka order galat mila"}
        require(out.all {it.date>=f.start && it.date<f.end && (f.type==0 || it.type==f.type) && (f.account==null || it.accountId==f.account) && (!f.connected || (it.type in listOf(1,2) && it.duration>0)) && (after==null || it.date<after.date || (it.date==after.date && it.id<after.id))}) {"Call log ka range galat mila"}
        return Page(rows,if(out.size>100)rows.last().let {Anchor(it.date,it.id)} else null)
    }
}
