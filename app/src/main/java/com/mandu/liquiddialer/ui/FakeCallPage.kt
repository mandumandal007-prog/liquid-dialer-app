package com.mandu.liquiddialer.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.FakeCallActivity
import com.mandu.liquiddialer.data.FakeCalls
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable fun FakeCallPage(onBack:()->Unit) {
    val ctx=LocalContext.current
    var name by remember { mutableStateOf("Demo caller") }
    var plan by remember { mutableStateOf(FakeCalls.current(ctx)) }
    var error by remember { mutableStateOf("") }
    var allowed by remember {mutableStateOf(FakeCalls.notificationsAllowed(ctx))}
    LaunchedEffect(Unit){while(true){FakeCalls.cleanExpired(ctx);plan=FakeCalls.current(ctx);allowed=FakeCalls.notificationsAllowed(ctx);delay(1000)}}
    PageFrame("Fake call",onBack) {
        GlassCard {
            LdText("Sirf simulation",18f,FontWeight.Bold)
            LdText("Koi asli call nahi lagegi. SIM, mic, recording ya call history use nahi hoti.",13f,maxLines=4)
            Spacer(Modifier.height(10.dp))
            LdText("Scheduled alert par tap karna pad sakta hai. Battery restrictions se timer late ho sakta hai; 5 minute se zyada late alert hata diya jayega. Yeh dependable safety / escape tool nahi hai.",12.5f,maxLines=8)
            LdText("Reboot/app update par cancel. Screen chhodne, 45 seconds hone ya asli call aane par simulation band.",12f,maxLines=5)
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(value=name,onValueChange={name=it.take(40)},label={Text("Display name (local only)")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("fake_name"))
        if(!allowed) GlassCard {
            LdText("Notifications band hain",15f,FontWeight.Bold)
            LdText("Scheduled alert ke liye notifications allow karo. Preview abhi bhi dekh sakte ho.",12.5f,maxLines=4)
            TextButton(onClick={ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,ctx.packageName))}){Text("Notification settings")}
        }
        val active=plan
        if(active!=null) GlassCard {
            LdText(if(active.ready) "Simulation alert ready" else "Pending simulation",15f,FontWeight.Bold)
            LdText(active.name+" · "+SimpleDateFormat("h:mm:ss a",Locale.getDefault()).format(Date(active.at)),13f,maxLines=3)
            LdText("Approximate timer. Ek waqt par sirf ek simulation.",12f,maxLines=3)
            TextButton(onClick={FakeCalls.cancel(ctx);plan=null},modifier=Modifier.buttonGlass(LocalLd.current,RoundedCornerShape(18.dp)).testTag("fake_cancel")){Text("Cancel simulation")}
        } else {
            TextButton(onClick={try {
                val p=FakeCalls.preview(ctx)
                ctx.startActivity(Intent(ctx,FakeCallActivity::class.java).putExtra("token",p.token))
                plan=FakeCalls.current(ctx);error=""
            }catch(e:Exception){error=e.message ?: "Preview nahi khula"}},modifier=Modifier.padding(vertical=8.dp).buttonGlass(LocalLd.current,RoundedCornerShape(18.dp)).testTag("fake_now")){Text("Preview now (Demo caller)")}
            LdText("Naam ke saath approximate alert:",14f,FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                listOf("30 sec" to 30_000L,"1 min" to 60_000L,"5 min" to 300_000L).forEach{(label,ms)->
                    TextButton(enabled=allowed,onClick={try{plan=FakeCalls.schedule(ctx,name,ms);error=""}catch(e:Exception){error=e.message ?: "Schedule nahi hua"}},modifier=Modifier.buttonGlass(LocalLd.current,RoundedCornerShape(18.dp)).testTag("fake_delay_$ms")){Text(label)}
                }
            }
        }
        if(error.isNotBlank())LdText(error,13f,color=LocalLd.current.danger,maxLines=5,modifier=Modifier.testTag("fake_error"))
    }
}

@Composable fun FakeCallView(name:String,onAnswer:()->Unit,onEnd:()->Unit) {
    var answered by remember {mutableStateOf(false)}
    var seconds by remember {mutableIntStateOf(0)}
    LaunchedEffect(answered){if(answered)while(true){delay(1000);seconds++}}
    BackHandler {onEnd()}
    Box(Modifier.fillMaxSize()) {
        BlobBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Spacer(Modifier.height(30.dp))
            LdText("Fake call · Simulation",15f,FontWeight.SemiBold,modifier=Modifier.testTag("fake_label"))
            Spacer(Modifier.height(45.dp))
            Avatar(name,null,100.dp,32f,glow=true,seed="fake")
            Spacer(Modifier.height(22.dp))
            LdText(name,28f,FontWeight.Bold,maxLines=2,modifier=Modifier.testTag("fake_caller"))
            LdText(if(answered) "Demo connected · ${seconds/60}:${(seconds%60).toString().padStart(2,'0')}" else "Simulated incoming call",15f,maxLines=2)
            Spacer(Modifier.height(16.dp))
            LdText("Asli call nahi · Mic band",12f,color=LocalLd.current.muted)
            Spacer(Modifier.weight(1f))
            if(!answered)TextButton(onClick={answered=true;onAnswer()},modifier=Modifier.buttonGlass(LocalLd.current,RoundedCornerShape(24.dp)).padding(horizontal=14.dp).testTag("fake_answer")){Text("Answer simulation")}
            Spacer(Modifier.height(18.dp))
            EndCallBtn(onClick=onEnd)
            Spacer(Modifier.height(12.dp))
            LdText("Simulation band karo",12f)
            Spacer(Modifier.height(30.dp))
        }
    }
}
