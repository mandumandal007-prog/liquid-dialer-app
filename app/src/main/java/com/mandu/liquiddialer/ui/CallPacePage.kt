package com.mandu.liquiddialer.ui
import android.app.NotificationManager
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import com.mandu.liquiddialer.telecom.CallNotifier
import com.mandu.liquiddialer.telecom.CallPace
import kotlinx.coroutines.launch

@Composable fun CallPacePage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 var error by remember{mutableStateOf("")};var status by remember{mutableStateOf("")}
 fun save(minutes:Int?=null,enabled:Boolean?=null,repeat:Boolean?=null){scope.launch{try{require(minutes==null||minutes in CallPace.intervals);ctx.ldStore.edit{if(enabled!=null)it[K.callPace]=enabled;if(minutes!=null)it[K.callPaceMinutes]=minutes;if(repeat!=null)it[K.callPaceRepeat]=repeat};error="";status="Saved. Active stretch ka timer naya shuru hoga agar timing badli."}catch(_:Exception){error="Setting save nahi hui"}}}
 PageFrame("Call pace reminders",onBack){
  LdText("Baat lambi ho rahi hai?",20f,FontWeight.Bold,maxLines=3)
  LdText("Ek lagatar active SIM-call stretch ke baad halki yaad. Ye total call duration, billed minutes ya call limit nahi. Call khud nahi kategi, mic/speaker nahi badlenge.",13f,color=p.muted,maxLines=7)
  GlassCard{SwitchRow("Call pace reminders","Default OFF. Call service se countdown aur generic notification.",s.callPace,last=true){save(enabled=it)}}
  SecTitle("Har kitne active minutes?")
  GlassCard{CallPace.intervals.forEachIndexed{i,m->SRow("$m min",if(s.callPaceMinutes==m)"Selected"else null,LdIcons.Clock,last=i==CallPace.intervals.lastIndex){save(minutes=m)}}}
  GlassCard(Modifier.padding(top=12.dp)){SwitchRow("Repeat during same stretch","Off: ek baar. On: har interval par; missed intervals ki spam catch-up nahi.",s.callPaceRepeat,last=true){save(repeat=it)}}
  SecTitle("Timer ka matlab")
  GlassCard{
   LdText("1. Call ek hi aur active ho tab timer shuru. Enable/service restart par yahin se fresh interval.",13f,maxLines=5)
   Spacer(Modifier.height(10.dp));LdText("2. Hold, doosri call, conference, call change ya OFF par countdown aur notice clear. Wapas eligible hone par zero se naya stretch.",13f,maxLines=6)
   Spacer(Modifier.height(10.dp));LdText("3. Screen band ho tab bhi existing call service deadline rakhti hai. Phone sleep/battery se late ho sakta hai. Ye alarm ya exact-time guarantee nahi.",13f,maxLines=6)
  }
  SecTitle("Read-only example")
  LdText("${s.callPaceMinutes} min active → reminder${if(s.callPaceRepeat)" → agle ${s.callPaceMinutes} min baad phir"else" → us stretch mein dobara nahi"}. Hold par reset.",14f,FontWeight.SemiBold,maxLines=5,modifier=Modifier.testTag("pace_example"))
  val default=ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage==ctx.packageName
  if(!default)LdText("Liquid Dialer default Phone app hona chahiye.",13f,color=p.danger,maxLines=4,modifier=Modifier.testTag("pace_default_warning"))
  val notices=NotificationManagerCompat.from(ctx).areNotificationsEnabled()&&ctx.getSystemService(NotificationManager::class.java).getNotificationChannel(CallNotifier.CH_ASSIST)?.importance!=NotificationManager.IMPORTANCE_NONE
  if(!notices)LdText("Notifications / Call-time help channel band hai. Countdown dikhega, notification nahi.",13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("pace_notification_warning"))
  if(error.isNotBlank())LdText(error,12.5f,color=p.danger,maxLines=4,modifier=Modifier.testTag("pace_error"))
  if(status.isNotBlank())LdText(status,12.5f,color=p.accent,maxLines=4,modifier=Modifier.testTag("pace_status"))
  LdText("Notification mein naam/number nahi. Koi call history ya pace log save nahi. Settings aur appearance backup alag hain; ye option us backup se restore nahi hota.",12f,color=p.muted,maxLines=7)
 }
         }
