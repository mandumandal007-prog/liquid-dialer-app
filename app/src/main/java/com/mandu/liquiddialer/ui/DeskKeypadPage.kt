package com.mandu.liquiddialer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch

@Composable fun DeskKeypadPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Live desk keypad",onBack){
  LdText("Menu key dabao. Kaam khula rakho.",22f,FontWeight.Bold,maxLines=4)
  LdText("Support/IVR par baat karte waqt desk mein notes aur tools band kiye bina 0-9, * aur # tone request bhejo. Har key par ek manual tap, koi automatic sequence nahi.",14f,color=p.muted,maxLines=9)
  GlassCard{SwitchRow("Live desk keypad","Default OFF. Live call desk bhi ON hona chahiye.",s.deskKeypad,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.deskKeypad]=v};error=false}catch(_:Exception){error=true}}}}
  if(!s.liveCallDesk)LdText("Pehle Settings > Live call desk enable karo.",13f,color=p.accent,maxLines=4)
  SecTitle("Connected call mein")
  GlassCard{
   LdText("1. Live call desk kholo, phir Desk keypad. Keys tools ke upar milengi. Notes/tools neeche scroll karo; keypad show/hide unka unsaved draft clear nahi karta.",13f,maxLines=8)
   Spacer(Modifier.height(12.dp));LdText("2. IVR ka jawab sun kar agla key tap karo. Rapid taps refuse ho sakte hain. Hide keypad se koi tone nahi jaati. Call card ka keypad alag rahega.",13f,maxLines=8)
   Spacer(Modifier.height(12.dp));LdText("3. Caller, timer, mute/speaker aur red End fixed rehte hain. Layout studio mein Keypad hidden hai to desk keypad bhi hidden. Hold, doosri call, conference, video, call change ya OFF par sending rukti hai.",13f,maxLines=10)
  }
  SecTitle("Digits save ya display nahi hote")
  LdText("No key history, clipboard, paste, saved sequence, auto dial or call action. Sirf generic requested/refused notice dikhta hai. Ye secure PIN pad nahi: saamne wale/IVR ko pressed digit sunai de sakta hai. Sirf bharosemand call par keys use karo. Phone/provider DTMF support par delivery depend karti hai; requested ka matlab IVR ne accept kiya, aisa nahi.",13f,color=p.muted,maxLines=15)
  if(error)LdText("Setting save nahi hui",13f,color=p.danger,modifier=Modifier.testTag("desk_keypad_error"))
 }
}

@Composable internal fun DeskKeypad(send:(Char)->Boolean){
 val p=LocalLd.current;var notice by remember{mutableStateOf("")}
 Column(Modifier.fillMaxWidth().testTag("desk_keypad"),verticalArrangement=Arrangement.spacedBy(8.dp)){
  LdText("ONE KEY PER TAP",11f,FontWeight.Bold,p.accent)
  LdText("Digits are not shown or saved. Listen to the IVR before the next tap.",12f,color=p.muted,maxLines=4)
  listOf("123","456","789","*0#").forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   row.forEach{digit->Box(Modifier.weight(1f).heightIn(min=52.dp).buttonGlass(p,RoundedCornerShape(18.dp))
    .semantics{contentDescription="Send keypad key $digit"}.clickable(role=Role.Button){notice=if(send(digit))"One tone requested. Listen for the IVR response."else"No tone requested. Check call status or wait before tapping again."}.testTag("desk_key_$digit").padding(vertical=10.dp),contentAlignment=Alignment.Center){LdText(digit.toString(),24f,FontWeight.SemiBold)}}
  }}
  if(notice.isNotEmpty())LdText(notice,12f,color=p.accent,maxLines=5,modifier=Modifier.testTag("desk_key_notice"))
  Spacer(Modifier.height(10.dp))
 }
}
