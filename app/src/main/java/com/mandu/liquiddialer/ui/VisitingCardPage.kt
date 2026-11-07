package com.mandu.liquiddialer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.mandu.liquiddialer.data.VisitingCard
import com.mandu.liquiddialer.data.VisitingCardArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun VisitingCardPage(onBack:()->Unit) {
    val ctx=LocalContext.current;val scope=rememberCoroutineScope()
    var raw by rememberSaveable {mutableStateOf(VisitingCard.load(ctx).json())}
    val card=remember(raw){VisitingCard.fromJson(raw)}
    val valid=card.invalidFields.isEmpty()
    var pending by rememberSaveable {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var clear by remember {mutableStateOf(false)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")){uri->
        val snapshot=pending;pending=null
        if(uri!=null && snapshot!=null){busy=true;message="";scope.launch {
            try {
                withContext(Dispatchers.IO){ctx.contentResolver.openOutputStream(uri,"wt")?.use{VisitingCardArt.writePng(VisitingCard.fromJson(snapshot),it)} ?: error("Output unavailable")}
                message="PNG save ho gaya. Kisi ko bheja nahi gaya."
            }catch(_:Exception){message="PNG save nahi hua. Adhuri destination file hata kar dobara try karo."}
            finally{busy=false}
        }}
    }
    PageFrame("Visiting card",onBack) {
        LdText("Black & gold",20f,FontWeight.Medium)
        LdText("Apni details bharo. Card offline banta hai; contacts se koi data nahi liya jata.",12.5f,maxLines=4)
        Spacer(Modifier.height(14.dp))
        Canvas(Modifier.fillMaxWidth().aspectRatio(1200f/720f).clip(RoundedCornerShape(12.dp)).testTag("card_preview").semantics {contentDescription="Card preview: "+card.values().filter{it.isNotBlank()}.joinToString(", ")} ){
            drawIntoCanvas {VisitingCardArt.draw(it.nativeCanvas,if(valid)card else VisitingCard(),size.width,size.height)}
        }
        Spacer(Modifier.height(8.dp))
        LdText(if(valid) "Preview · PNG 1200 × 720" else "Preview roka gaya: neeche laal fields chhote karo. Pura text editor mein hai; draft save kar sakte ho. PNG abhi save nahi hogi.",11.5f,color=if(valid)LocalLd.current.muted else LocalLd.current.danger,maxLines=5,modifier=Modifier.testTag("card_validation"))
        Spacer(Modifier.height(16.dp))
        val labels=listOf("Business / shop name","Tagline (optional)","Your name","Role (optional)","Phone","Email (optional)","Website (optional)","Address (optional)")
        card.values().forEachIndexed{i,value->
            OutlinedTextField(value=value,onValueChange={next->
                val values=card.values().toMutableList();values[i]=next.replace('\n',' ').replace('\r',' ')
                raw=VisitingCard(values[0],values[1],values[2],values[3],values[4],values[5],values[6],values[7]).json();message=""
            },label={Text(labels[i])},isError=i in card.invalidFields,supportingText={Text("${value.length}/${VisitingCard.LIMITS[i]}"+if(i in card.invalidFields) " · PNG ke liye text chhota karo" else "")},singleLine=i!=7,maxLines=if(i==7)3 else 1,modifier=Modifier.fillMaxWidth().padding(bottom=10.dp).testTag("card_field_$i"))
        }
        LdText("Draft sirf is phone ke app storage mein rahega. PNG ke liye tum jagah chuno; cloud provider chuna to uski policy lagegi.",12f,maxLines=6)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("PNG save karo",enabled=valid && card.hasContent && !busy && pending==null,modifier=Modifier.testTag("card_export")){
            pending=raw
            try{export.launch("visiting-card.png")}catch(_:Exception){pending=null;message="Save picker nahi khula. Dobara try karo."}
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Draft phone par rakho",enabled=!busy,modifier=Modifier.testTag("card_save")){
            try{VisitingCard.save(ctx,card);message="Private draft save ho gaya."}catch(_:Exception){message="Draft save nahi hua."}
        }
        Spacer(Modifier.height(8.dp))
        GhostButton("Details saaf karo",modifier=Modifier.testTag("card_clear")){clear=true}
        if(message.isNotBlank()){Spacer(Modifier.height(10.dp));LdText(message,13f,maxLines=6,modifier=Modifier.testTag("card_notice"))}
        if(busy)LdText("PNG save ho raha hai…",13f)
    }
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("Details saaf karein?")},text={Text("Editor aur private draft saaf honge. Pehle save ki hui PNG files nahi hatengi.")},confirmButton={TextButton(onClick={
        try{VisitingCard.delete(ctx);raw=VisitingCard().json();message="Editor aur private draft saaf ho gaye."}catch(_:Exception){message="Draft nahi hata. Dobara try karo."};clear=false
    },modifier=Modifier.testTag("card_clear_confirm")){Text("Haan, saaf karo")}},dismissButton={TextButton(onClick={clear=false}){Text("Nahi")}})
}
