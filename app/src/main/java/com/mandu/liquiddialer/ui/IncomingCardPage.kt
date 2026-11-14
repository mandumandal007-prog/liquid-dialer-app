package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable internal fun IncomingCardActions(config:IncomingCard.Config,video:Boolean,onReject:()->Unit,onAnswer:()->Unit,onAudio:()->Unit,onMessage:()->Unit){
 val p=LocalLd.current;var confirm by remember(config){mutableStateOf(false)};var epoch by remember{mutableIntStateOf(0)}
 val reject:@Composable ()->Unit={key(epoch,config){SwipeCallBtn(true,"Cut",config.swipe,config.travel,config.enabled){if(config.enabled&&config.confirmCut)confirm=true else onReject()}}}
 val answer:@Composable ()->Unit={Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
  key(config){SwipeCallBtn(false,if(video)"Video answer"else"Answer",config.swipe,config.travel,config.enabled,onAnswer)}
  if(video)Box(Modifier.widthIn(max=112.dp).clip(RoundedCornerShape(999.dp)).glass(p,RoundedCornerShape(999.dp),0.dp).clickable(onClick=onAudio).padding(horizontal=8.dp,vertical=6.dp).testTag("answer_audio")){LdText("Sirf audio me lo",10.5f,FontWeight.SemiBold,p.muted,maxLines=3)}
 }}
 Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=6.dp).testTag(if(config.enabled&&config.swap)"incoming_swapped"else"incoming_normal"),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom){
  if(config.enabled&&config.swap)answer()else reject()
  if(!config.enabled||config.message)Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
   Box(Modifier.size(52.dp).callGlass(p,CircleShape).clickable(onClick=onMessage).testTag("incoming_message"),contentAlignment=Alignment.Center){LdIcon(LdIcons.Message,22.dp)}
   LdText("Message",11f,FontWeight.SemiBold,p.muted);Spacer(Modifier.height(18.dp))
  }
  if(config.enabled&&config.swap)reject()else answer()
 }
 if(confirm)Dialog(onDismissRequest={confirm=false;epoch++}){GlassCard{
  LdText("Decline this incoming call?",18f,FontWeight.Bold,maxLines=5)
  LdText("Call is still ringing. Confirm sends decline; Cancel keeps it ringing. Message replies have their own send action and do not use this confirmation.",13f,color=p.muted,maxLines=8)
  PrimaryButton("Decline call",danger=true,modifier=Modifier.testTag("incoming_decline_confirm")){confirm=false;onReject()}
  GhostButton("Keep ringing",Modifier.testTag("incoming_decline_cancel")){confirm=false;epoch++}
 }}
}

@Composable fun IncomingCardPage(onBack:()->Unit){
 val ctx=LocalContext.current;val settings by LdApp.settings.collectAsState();val c=IncomingCard.decode(settings.incomingCard);val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf("")};var reset by remember{mutableStateOf(false)}
 fun save(change:(IncomingCard.Config)->IncomingCard.Config){scope.launch{try{ctx.ldStore.edit{prefs->prefs[K.incomingCard]=IncomingCard.encode(change(IncomingCard.decode(prefs[K.incomingCard]?:"")))};error=""}catch(_:Exception){error="Setting save failed; try again"}}}
 @Composable fun toggle(title:String,desc:String,value:Boolean,tag:String,change:(IncomingCard.Config,Boolean)->IncomingCard.Config){
  Box(Modifier.testTag(tag)){SwitchRow(title,desc,value,last=true,subMaxLines=8){v->save{change(it,v)}}}
 }
 @Composable fun choice(title:String,values:List<Pair<String,String>>,current:String,tag:String,change:(IncomingCard.Config,String)->IncomingCard.Config){
  LdText(title,16f,FontWeight.Bold,maxLines=4)
  GlassCard { values.forEach{(value,label)->GhostButton((if(value==current)"✓ "else"")+label,Modifier.testTag("${tag}_$value")){save{change(it,value)}}} }
 }
 PageFrame("Incoming call card controls",onBack){
  LdText("Your incoming screen. Separate controls.",23f,FontWeight.Bold,maxLines=5)
  toggle("Use incoming-only controls","Default OFF. OFF restores the existing shared call-card settings; does not erase choices.",c.enabled,"incoming_enable"){x,v->x.copy(enabled=v)}
  LdText(if(c.enabled)"Incoming-only overrides ON"else"Overrides OFF · choices below are saved but not applied",14f,FontWeight.Bold,maxLines=5,modifier=Modifier.testTag("incoming_settings_state"))
  LdText("Only the app's ringing screen changes. Active/outgoing call cards, long red hang-up pill, ringtone, call audio, notification/full-screen permissions and automatic call handling are unchanged. Answer/Cut and caller identity cannot be hidden.",13f,color=p.muted,maxLines=12)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5)
  SecTitle("Look while ringing")
  choice("Incoming card style",CALL_STYLES.filter{it.key in IncomingCard.styles}.map{it.key to it.label},c.style,"incoming_style"){x,v->x.copy(style=v)}
  choice("Background",listOf("inherit" to "Follow existing style/photo rules","blobs" to "Liquid blobs","solid" to "Plain gradient","photo" to "Caller photo (dark fallback if unavailable)"),c.background,"incoming_bg"){x,v->x.copy(background=v)}
  LdText("An explicit background overrides style and custom-photo background choice while ringing. Photo uses the existing blur level and dark contrast wash. With an explicit liquid/plain background the current theme keeps text readable; style still controls decoration and buttons; no photo is downloaded.",12f,color=p.muted,maxLines=9)
  choice("Avatar shape",listOf("circle" to "Circle","squircle" to "Rounded square"),c.avatar,"incoming_avatar"){x,v->x.copy(avatar=v)}
  choice("Avatar size",listOf("75" to "Compact 75%","100" to "Standard 100%","125" to "Large 125%"),c.avatarSize.toString(),"incoming_avatar_size"){x,v->x.copy(avatarSize=v.toInt())}
  choice("Caller name size",listOf("85" to "Compact 85%","100" to "Standard 100%","115" to "Large 115%"),c.nameSize.toString(),"incoming_name_size"){x,v->x.copy(nameSize=v.toInt())}
  LdText("Sizing uses the existing shared card scale too. Incoming-only screen scrolls vertically if needed; system font scale still applies. Unknown callers always keep their number as the main identity.",12f,color=p.muted,maxLines=8)
  SecTitle("Identity and shortcuts")
  toggle("Show extra phone number","Below a known caller's name. Does not hide the main identity or SIM status.",c.number,"incoming_number_toggle"){x,v->x.copy(number=v)}
  toggle("Show contact type","Existing Mobile/Work label when available.",c.contactType,"incoming_type_toggle"){x,v->x.copy(contactType=v)}
  toggle("Show last call","Existing last-call information when available.",c.lastCall,"incoming_last_toggle"){x,v->x.copy(lastCall=v)}
  toggle("Show Message shortcut","Hides only the incoming Message shortcut. Sending a quick reply still declines the call as before.",c.message,"incoming_message_toggle"){x,v->x.copy(message=v)}
  toggle("Show WhatsApp shortcut","Only when existing unknown-number/valid-link rules allow it. Opens chat; never auto-sends.",c.whatsapp,"incoming_whatsapp_toggle"){x,v->x.copy(whatsapp=v)}
  SecTitle("Answer and Cut")
  toggle("Answer on left, Cut on right","OFF keeps Cut on left and Answer on right. Red/green meaning and labels stay visible.",c.swap,"incoming_swap_toggle"){x,v->x.copy(swap=v)}
  toggle("Swipe up to Answer/Cut","OFF uses one tap. Applies to both Answer and Cut on app ringing screen only.",c.swipe,"incoming_swipe_toggle"){x,v->x.copy(swipe=v)}
  choice("Swipe travel",listOf("70" to "Short 70dp","110" to "Standard 110dp","150" to "Long 150dp"),c.travel.toString(),"incoming_travel"){x,v->x.copy(travel=v.toInt())}
  LdText("Travel is used only in Swipe mode. A short drag does nothing and returns the button. These controls do not alter notification answer buttons or lock-screen behaviour.",12f,color=p.muted,maxLines=8)
  toggle("Confirm incoming Cut","Shows a decline confirmation after Cut. Cancel keeps ringing. Does not apply to Message replies, Answer, audio-only video answer or active-call End.",c.confirmCut,"incoming_confirm_toggle"){x,v->x.copy(confirmCut=v)}
  GhostButton("Reset incoming-only choices",Modifier.testTag("incoming_reset")){reset=true}
  LdText("Reset turns the override OFF and restores its defaults. Shared settings, contacts, photos, notes and call history stay unchanged. No automatic answer, reject, recording, messages or volume changes are added.",12f,color=p.muted,maxLines=9)
 }
 if(reset)Dialog(onDismissRequest={reset=false}){GlassCard{
  LdText("Reset incoming-only controls?",18f,FontWeight.Bold,maxLines=5)
  PrimaryButton("Reset and turn OFF",modifier=Modifier.testTag("incoming_reset_confirm")){save{IncomingCard.Config()};reset=false}
  GhostButton("Keep choices",Modifier.testTag("incoming_reset_cancel")){reset=false}
 }}
}
