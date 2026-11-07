package com.mandu.liquiddialer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke

/** Static vector accents only. No bitmap assets, timers, or downloaded effects. */
@Composable fun CallStyleDecoration(style:CallCardStyle,modifier:Modifier=Modifier) {
    if(style.key !in REFERENCE_CALL_KEYS)return
    Canvas(modifier){
        val w=size.width;val h=size.height;val pad=12.dp.toPx();val gold=Color(0xFFDEBF7D)
        when(style.key){
            "filigree"->{
                drawRoundRect(gold.copy(alpha=.7f),Offset(pad,pad),Size(w-2*pad,h-2*pad),CornerRadius(22.dp.toPx()),style=Stroke(1.dp.toPx()))
                drawRoundRect(gold.copy(alpha=.22f),Offset(pad+5,pad+5),Size(w-2*pad-10,h-2*pad-10),CornerRadius(19.dp.toPx()),style=Stroke(.5.dp.toPx()))
                for(x in listOf(pad+10,w-pad-10))for(y in listOf(pad+10,h-pad-10)){
                    drawCircle(gold.copy(alpha=.6f),5.dp.toPx(),Offset(x,y),style=Stroke(.7.dp.toPx()))
                    drawCircle(gold.copy(alpha=.35f),10.dp.toPx(),Offset(x,y),style=Stroke(.5.dp.toPx()))
                }
            }
            "cyber"->{
                for(right in listOf(false,true)){
                    fun x(v:Float)=if(right)w-v else v
                    val path=Path().apply{moveTo(x(pad),h*.05f);lineTo(x(pad),h*.25f);lineTo(x(pad+10),h*.28f);lineTo(x(pad+10),h*.43f);lineTo(x(pad),h*.46f);lineTo(x(pad),h*.72f);lineTo(x(pad+8),h*.75f);lineTo(x(pad+8),h*.93f)}
                    drawPath(path,if(right)Color(0xFFD47AFF) else Color(0xFF55DAFF),style=Stroke(1.3.dp.toPx()))
                }
                drawLine(Color(0xFFAF5AFF),Offset(w*.28f,pad),Offset(w*.72f,pad),2.dp.toPx())
            }
            "jadegold"->{
                drawRoundRect(gold.copy(alpha=.55f),Offset(pad,pad),Size(w-2*pad,h-2*pad),CornerRadius(22.dp.toPx()),style=Stroke(.8.dp.toPx()))
                repeat(9){i->val r=(28+i*13).dp.toPx();drawArc(gold.copy(alpha=.045f),0f,270f,false,Offset(-r*.66f,-r*.3f),Size(r*2,r*2),style=Stroke(1.dp.toPx()))}
            }
            "pearl"->{
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha=.8f),Color.Transparent),center=Offset(w*.72f,h*.3f),radius=w*.7f),w*.7f,Offset(w*.72f,h*.3f))
            }
            "pastel"->{
                val centers=listOf(Offset(w*.1f,h*.22f),Offset(w*.95f,h*.55f),Offset(w*.2f,h*.9f))
                val colors=listOf(Color(0xFFDCBEF7),Color(0xFFFFCFDB),Color(0xFFB8E9DE))
                centers.zip(colors).forEach{(c,col)->drawCircle(Brush.radialGradient(listOf(col.copy(alpha=.65f),Color.Transparent),c,w*.85f),w*.85f,c)}
            }
            "aurorapanel"->{
                for((c,col) in listOf(Offset(w*.15f,h*.22f) to Color(0xFF973CEB),Offset(w*.9f,h*.38f) to Color(0xFF00B1C7))){drawCircle(Brush.radialGradient(listOf(col.copy(alpha=.65f),Color.Transparent),c,w*.8f),w*.8f,c)}
                drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha=.13f),Color.White.copy(alpha=.04f))),Offset(pad*2,h*.16f),Size(w-pad*4,h*.78f),CornerRadius(22.dp.toPx()))
                drawRoundRect(Color.White.copy(alpha=.2f),Offset(pad*2,h*.16f),Size(w-pad*4,h*.78f),CornerRadius(22.dp.toPx()),style=Stroke(.8.dp.toPx()))
            }
        }
    }
}
