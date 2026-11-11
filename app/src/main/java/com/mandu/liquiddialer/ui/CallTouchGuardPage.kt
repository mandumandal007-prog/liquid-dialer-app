package com.mandu.liquiddialer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit

@Composable fun CallTouchGuardPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var error by remember{mutableStateOf(false)}
 PageFrame("Call touch guard",onBack){
  LdText("Accidental taps se bachao",21f,FontWeight.Bold,maxLines=3)
  LdText("Ek active audio call mein Touch guard dabao. Mute, speaker, keypad, record aur doosre on-screen controls temporarily hat jayenge. Call aur audio waise hi chalte rahenge.",13f,color=p.muted,maxLines=8)
  GlassCard{SwitchRow("Call touch guard","Default OFF. Sirf button dikhata hai; guard khud ON nahi hota.",s.callTouchGuard,last=true){value->scope.launch{try{ctx.ldStore.edit{it[K.callTouchGuard]=value};error=false}catch(_:Exception){error=true}}}}
  SecTitle("Wapas controls kaise laayein")
  GlassCard{
   LdText("1. Unlock controls dabao. Phir alag jagah Continue dabao, 5 seconds ke andar. Cancel ya wait karne par guard laga rahega.",13f,maxLines=7)
   Spacer(Modifier.height(12.dp));LdText("2. Laal End button hamesha milega. Cut pe confirm setting bhi lagu rahegi. Guard call kaatne se nahi bachata.",13f,maxLines=6)
  }
  SecTitle("Ye phone lock nahi hai")
  LdText("Notification, headset, volume/power buttons aur doosre apps ko ye guard nahi rokta. Privacy lock, proximity sensor ya pocket detection nahi. Naam, number, timer aur current badges dikhte rahenge.",13f,color=p.muted,maxLines=9)
  GlassCard{LdText("Hold, doosri call, conference, video/request, call change ya OFF par guard clear. App background mein jaane ya screen close/recreate hone par bhi dobara manually lagana hoga. Sirf ek active audio call mein available.",13f,maxLines=10)}
  if(error)LdText("Setting save nahi hui",13f,color=p.danger,modifier=Modifier.testTag("touch_guard_error"))
 }
}

@Composable fun CallTouchGuardView(name:String,number:String,timer:String,badges:List<Pair<String,androidx.compose.ui.graphics.Color>>,armed:Boolean,onArm:()->Unit,onUnlock:()->Unit,onCancel:()->Unit,onEnd:()->Unit){
 val p=LocalLd.current
 Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp).testTag("touch_guard_view"),horizontalAlignment=Alignment.CenterHorizontally){
  Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)){
   Spacer(Modifier.height(12.dp))
   LdText("TOUCH GUARD",12f,FontWeight.Bold,color=p.accent)
   Avatar(name,null,76.dp,26f)
   LdText(name,25f,FontWeight.Bold,maxLines=3,modifier=Modifier.testTag("touch_guard_name"))
   LdText(number,14f,color=p.muted,maxLines=3)
   LdText(timer,24f,FontWeight.SemiBold,modifier=Modifier.testTag("touch_guard_timer"))
   badges.forEach{(text,color)->LdText(text,12f,FontWeight.SemiBold,color=color)}
   GlassCard{
    LdText(if(armed)"Continue se controls wapas"else"Call controls guarded",18f,FontWeight.Bold,maxLines=3)
    Spacer(Modifier.height(8.dp))
    LdText(if(armed)"5 seconds mein Continue dabao. Cancel ya wait se guard laga rahega."else"Mute, speaker aur keypad par accidental tap nahi lagega. Call aur current audio badle nahi hain.",13f,color=p.muted,maxLines=7)
    Spacer(Modifier.height(14.dp))
    GhostButton(if(armed)"Cancel unlock"else"Unlock controls",Modifier.fillMaxWidth().testTag(if(armed)"touch_guard_cancel"else"touch_guard_arm")){if(armed)onCancel()else onArm()}
   }
   if(armed){Spacer(Modifier.height(16.dp));GhostButton("Continue",Modifier.fillMaxWidth().testTag("touch_guard_continue"),onUnlock)}
   LdText("End button active hai. Ye phone lock nahi hai.",12f,color=p.muted,maxLines=3)
  }
  Spacer(Modifier.height(16.dp));EndCallBtn(onClick=onEnd);Spacer(Modifier.height(6.dp))
 }
}
