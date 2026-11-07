package com.mandu.liquiddialer.data

import android.content.Context
import android.graphics.*
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.OutputStream

/** A private local draft. No contact lookup, network, profile autofill or sending. */
data class VisitingCard(
    val business:String="",val tagline:String="",val name:String="",val role:String="",
    val phone:String="",val email:String="",val website:String="",val address:String=""
) {
    val invalidFields get()=values().mapIndexedNotNull{i,v->if(v.length>LIMITS[i])i else null}
    fun values()=listOf(business,tagline,name,role,phone,email,website,address)
    fun json()=JSONObject().also {o->KEYS.zip(values()).forEach{(k,v)->o.put(k,v)}}.toString()
    val hasContent get()=values().any{it.isNotBlank()}
    companion object {
        val KEYS=listOf("business","tagline","name","role","phone","email","website","address")
        val LIMITS=listOf(60,70,60,60,40,80,100,160)
        fun fromJson(raw:String):VisitingCard {
            val o=JSONObject(raw)
            val v=KEYS.mapIndexed{i,k->o.optString(k,"")}
            return VisitingCard(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7])
        }
        private fun file(ctx:Context)=AtomicFile(File(ctx.noBackupFilesDir,"visiting_card_draft.json"))
        fun load(ctx:Context):VisitingCard=try{fromJson(file(ctx).openRead().bufferedReader().use{it.readText()})}catch(_:Exception){VisitingCard()}
        fun save(ctx:Context,card:VisitingCard){
            val f=file(ctx);val out=f.startWrite()
            try{out.write(card.json().toByteArray(Charsets.UTF_8));f.finishWrite(out)}catch(e:Exception){f.failWrite(out);throw e}
        }
        fun delete(ctx:Context)=file(ctx).delete()
    }
}

/** Preview and PNG share this renderer. All user text is wrapped, never ellipsized. */
object VisitingCardArt {
    const val WIDTH=1200;const val HEIGHT=720
    val GOLD=Color.rgb(213,184,117)
    data class Line(val text:String,val x:Float,val baseline:Float,val size:Float,val gold:Boolean,val serif:Boolean=false)
    private fun paint(size:Float,serif:Boolean=false)=Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize=size;typeface=Typeface.create(if(serif)"serif" else "sans-serif-medium",Typeface.NORMAL)
    }
    internal fun wrap(text:String,size:Float,serif:Boolean=false,width:Float=1000f):List<String> {
        if(text.isBlank())return emptyList()
        val p=paint(size,serif);val result=mutableListOf<String>()
        for(paragraph in text.replace('\n',' ').split('\r')){
            var rest=paragraph.trim()
            while(rest.isNotEmpty()){
                var count=p.breakText(rest,true,width,null).coerceAtLeast(1)
                // Keep surrogate pairs intact when a long unbroken address reaches the edge.
                if(count<rest.length && count>1 && Character.isHighSurrogate(rest[count-1]))count--
                if(count<rest.length){val space=rest.lastIndexOf(' ',count-1);if(space>count/2)count=space}
                result+=rest.substring(0,count).trim();rest=rest.substring(count).trimStart()
            }
        }
        return result
    }
    fun lines(card:VisitingCard):List<Line> {
        val out=mutableListOf<Line>();var y=0f
        fun group(text:String,size:Float,gold:Boolean,serif:Boolean=false,gap:Float=12f){
            val rows=wrap(text,size,serif)
            rows.forEach{line->y+=size*1.22f;val p=paint(size,serif);out+=Line(line,(WIDTH-p.measureText(line))/2,y,size,gold,serif)}
            if(rows.isNotEmpty())y+=gap
        }
        group(card.business,50f,true,gap=4f)
        group(card.tagline,24f,true,gap=24f)
        group(card.name,40f,false,true,gap=4f)
        group(card.role,25f,true,gap=22f)
        group(card.phone,27f,false,gap=5f)
        group(card.email,26f,false,gap=5f)
        group(card.website,26f,false,gap=5f)
        group(card.address,25f,false,gap=0f)
        val scale=minOf(1f,540f/y.coerceAtLeast(1f))
        val top=100f+(540f-y*scale)/2f
        return out.map{it.copy(x=WIDTH/2f+(it.x-WIDTH/2f)*scale,baseline=top+it.baseline*scale,size=it.size*scale)}
    }
    fun draw(canvas:Canvas,card:VisitingCard,width:Float=WIDTH.toFloat(),height:Float=HEIGHT.toFloat()) {
        canvas.save();canvas.scale(width/WIDTH,height/HEIGHT)
        canvas.drawColor(Color.rgb(12,15,14))
        val border=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=GOLD;style=Paint.Style.STROKE;strokeWidth=2f}
        canvas.drawRoundRect(24f,24f,1176f,696f,12f,12f,border)
        border.alpha=90;canvas.drawLine(70f,670f,1130f,670f,border)
        border.alpha=255
        val diamond=Path().apply{moveTo(600f,43f);lineTo(616f,59f);lineTo(600f,75f);lineTo(584f,59f);close()}
        canvas.drawPath(diamond,border)
        val text=lines(card)
        for(line in text){val p=paint(line.size,line.serif);p.color=if(line.gold)GOLD else Color.rgb(239,237,229);canvas.drawText(line.text,line.x,line.baseline,p)}
        canvas.restore()
    }
    fun writePng(card:VisitingCard,out:OutputStream){
        require(card.invalidFields.isEmpty()){ "Shorten over-limit fields before export" }
        val bitmap=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888)
        try{draw(Canvas(bitmap),card);check(bitmap.compress(Bitmap.CompressFormat.PNG,100,out)){"PNG save failed"}}
        finally{bitmap.recycle()}
    }
}
