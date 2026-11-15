package com.mandu.liquiddialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch

internal fun gridLVisible(s:LdSettings,id:String)=parseLayout(s.layoutGrid,GRID_IDS).any{it.first==id&&it.second}
internal fun bottomLVisible(s:LdSettings,id:String)=parseLayout(s.layoutBottom,BOTTOM_IDS).any{it.first==id&&it.second}

@Composable fun LiveCallDeskPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val scope=rememberCoroutineScope();val p=LocalLd.current
 var error by remember{mutableStateOf(false)}
 PageFrame("Live call desk",onBack){
  LdText("Baat bhi. Kaam bhi.",23f,FontWeight.Bold,maxLines=3)
  LdText("Call tools kholne par bhi caller, connected timer aur call controls saamne. Notes, checklist, calculator aur enabled call tools ke saath ek working screen.",14f,color=p.muted,maxLines=8)
  GlassCard{SwitchRow("Live call desk","Default OFF. Single connected voice call mein manually kholo.",s.liveCallDesk,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.liveCallDesk]=v};error=false}catch(_:Exception){error=true}}}}
  SecTitle("Call mein kaise kholein")
  GlassCard{
   LdText("1. Connected calling card par Live call desk dabao. Saved notes feature visible hona chahiye. Normal card OFF/close par waise hi rahega.",13f,maxLines=8)
   Spacer(Modifier.height(12.dp))
   LdText("2. Beech ka tools area scroll karo. Mute, speaker aur long red End neeche rehte hain. Layout studio mein hidden mute/speaker yahan bhi hidden. End confirmation setting lagu hai. Bluetooth ho to Speaker se route chooser khulta hai.",13f,maxLines=10)
   Spacer(Modifier.height(12.dp))
   LdText("3. Card dabao, phir Return to card confirm karo. Tool mein apna Back us tool se notes par laata hai. Phone Back active tool se wapas aata hai, notes par desk close ka confirmation kholta hai.",13f,maxLines=8)
  }
  SecTitle("Save pehle. Desk recording nahi hai.")
  LdText("Notes aur calculator ke Save alag hain; desk kholna kuch save nahi karta. Tool close/switch, call end/change, Hold, doosri call, conference, video/request, OFF ya screen recreation par unsaved work kho sakta hai. Pehle Save karo. Incoming call aur normal controls ko desk kabhi cover nahi rakhta.",13f,color=p.muted,maxLines=15)
  GlassCard{LdText("Existing tools ke storage aur privacy rules wahi hain. No new permission, recording, transcription, automatic mute, speaker, hold, call, message ya cloud action. Timer connection se elapsed time hai, billed minutes nahi. Touch guard ke liye pehle normal card par lauto.",13f,maxLines=12)}
  if(error)LdText("Setting save nahi hui",13f,color=p.danger,modifier=Modifier.testTag("call_desk_setting_error"))
 }
}

@Composable fun LiveCallDeskView(name:String,number:String,timer:String,muted:Boolean,speaker:Boolean,bluetooth:Boolean,
 showMute:Boolean,showSpeaker:Boolean,onClose:()->Unit,onMute:()->Unit,onSpeaker:()->Unit,onEnd:()->Unit){
 val p=LocalLd.current;var close by remember{mutableStateOf(false)}
 BackHandler{close=true}
 Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal=16.dp,vertical=10.dp).testTag("call_desk")){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
   Column(Modifier.weight(1f)){
    LdText("LIVE CALL DESK",10f,FontWeight.Bold,p.accent,letter=1f)
    LdText(name.ifBlank{number},18f,FontWeight.Bold,maxLines=2,modifier=Modifier.testTag("call_desk_name"))
    LdText("Connected · $timer",13f,FontWeight.SemiBold,p.accent,modifier=Modifier.testTag("call_desk_timer"))
   }
   GhostButton("Card",Modifier.width(80.dp).testTag("call_desk_close")){close=true}
  }
  LdText(number,12f,color=p.muted,maxLines=2,modifier=Modifier.padding(top=4.dp).testTag("call_desk_number"))
  LdText("Save before leaving. Call changes clear unsaved work.",11f,color=p.muted,maxLines=3,modifier=Modifier.padding(vertical=6.dp))
  Column(Modifier.weight(1f).fillMaxWidth().glass(p,RoundedCornerShape(22.dp)).verticalScroll(rememberScrollState()).padding(14.dp).testTag("call_desk_tools")){
   NotesSheet(number,name,deskMode=true){}
  }
  Row(Modifier.fillMaxWidth().padding(top=10.dp,bottom=6.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
   if(showMute)Box(Modifier.testTag("call_desk_mute")){CallBtn(LdIcons.Mic,if(muted)"Unmute"else"Mute",muted,size=52.dp,onClick=onMute)}
   if(showSpeaker)Box(Modifier.testTag("call_desk_speaker")){CallBtn(if(bluetooth)LdIcons.Bluetooth else LdIcons.SpeakerFill,if(bluetooth)"Bluetooth"else if(speaker)"Speaker on"else"Speaker",speaker,size=52.dp,onClick=onSpeaker)}
  }
  Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){EndCallBtn(onClick=onEnd)}
 }
 if(close)AlertDialog(onDismissRequest={close=false},title={LdText("Return to calling card?",19f,FontWeight.Bold,maxLines=3)},text={LdText("Unsaved tool inputs will be discarded. Saved notes and saved tool data stay. This does not end the call.",14f,maxLines=8)},confirmButton={TextButton(onClick=onClose,modifier=Modifier.testTag("call_desk_close_confirm")){LdText("Return to card",14f,FontWeight.SemiBold)}},dismissButton={TextButton(onClick={close=false},modifier=Modifier.testTag("call_desk_close_cancel")){LdText("Keep working",14f)}})
}
