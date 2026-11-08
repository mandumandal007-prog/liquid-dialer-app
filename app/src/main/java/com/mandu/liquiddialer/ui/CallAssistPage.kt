package com.mandu.liquiddialer.ui

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mandu.liquiddialer.telecom.CallNotifier
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.K
import kotlinx.coroutines.launch

@Composable fun CallAssistPage(onBack:()->Unit){
 val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();val s by LdApp.settings.collectAsState()
 var error by remember{mutableStateOf("")}
 var tick by remember{mutableIntStateOf(0)};val owner=LocalLifecycleOwner.current
 DisposableEffect(owner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)tick++};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
 val notifications=remember(tick){NotificationManagerCompat.from(ctx).areNotificationsEnabled() && ctx.getSystemService(NotificationManager::class.java).getNotificationChannel(CallNotifier.CH_ASSIST)?.importance!=NotificationManager.IMPORTANCE_NONE}
 PageFrame("Call-time help",onBack){
  LdText("Asli SIM call ke dauran, app se bahar ya screen band ho tab bhi call service soochna de sakti hai. Dono pehle se Off hain. Koi auto-unmute, speaker change, recording ya message nahi.",13f,color=p.muted,maxLines=7)
  Spacer(Modifier.height(12.dp))
  GlassCard(Modifier.testTag("assist_mute_card")){
   LdText("Mic mute reh gaya?",17f,FontWeight.Bold)
   LdText("Ek active call mein lagatar 30 sec mute rehne par ek notification. Unmute karoge tab saaf; dobara mute karne par nayi yaad. Phone sleep/battery se der ho sakti hai.",12.5f,color=p.muted,maxLines=7)
   Spacer(Modifier.height(8.dp))
   PrimaryButton(if(s.muteReminder)"Mute reminder: On" else "Mute reminder: Off",modifier=Modifier.testTag("assist_mute_toggle")){scope.launch{try{LdApp.instance.repo.set(K.muteReminder,!s.muteReminder)}catch(_:Exception){error="Setting save nahi hui"}}}
  }
  Spacer(Modifier.height(12.dp))
  GlassCard(Modifier.testTag("assist_route_card")){
   LdText("Headset se phone par audio",17f,FontWeight.Bold)
   LdText("Bluetooth/wired headset se earpiece ya speaker par call audio jaaye to notification. Khud route badloge tab bhi aa sakti hai. App audio route nahi badalta.",12.5f,color=p.muted,maxLines=7)
   Spacer(Modifier.height(8.dp))
   PrimaryButton(if(s.audioRouteAlert)"Audio route notice: On" else "Audio route notice: Off",modifier=Modifier.testTag("assist_route_toggle")){scope.launch{try{LdApp.instance.repo.set(K.audioRouteAlert,!s.audioRouteAlert)}catch(_:Exception){error="Setting save nahi hui"}}}
  }
  Spacer(Modifier.height(12.dp))
  LdText("Sirf ek active, non-conference SIM call. Ringing, hold, multiple calls, WhatsApp/Meet par nahi. Call khatam ya setting Off hote hi iski soochna hat jaati hai. Naam/number notification mein nahi aate.",12f,color=p.muted,maxLines=7)
  val default=ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage==ctx.packageName
  if(!default)LdText("Liquid Dialer ko default Phone app banana zaroori hai.",13f,color=p.danger,maxLines=3,modifier=Modifier.testTag("assist_default_warning"))
  if(!notifications)LdText("Notifications ya Call-time help channel band hai; ye alerts nahi dikhenge.",13f,color=p.danger,maxLines=3,modifier=Modifier.testTag("assist_notification_warning"))
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=3)
  PrimaryButton("Notification settings",modifier=Modifier.padding(top=8.dp).testTag("assist_notification_settings")){
   try{ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,ctx.packageName))}catch(_:Exception){error="Phone Settings mein notifications dekho"}
  }
 }
}
