package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

val LocalAiryCall=staticCompositionLocalOf{false}
val LocalConfirmPreview=staticCompositionLocalOf<Boolean?>{null}

@Composable fun AppearanceHubPage(onBack:()->Unit,onOpen:(String)->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope()
 fun set(k:androidx.datastore.preferences.core.Preferences.Key<Boolean>,v:Boolean){scope.launch{LdApp.instance.repo.set(k,v)}}
 PageFrame("Appearance control center",onBack){
  LdText("Apna look, apne controls",18f,FontWeight.Bold,maxLines=3)
  LdText("Har section ka scope neeche hai. Choices isi phone par save hoti hain; call ya contact khud nahi badalta.",12f,color=p.muted,maxLines=6)
  SecTitle("App-wide")
  GlassCard{
   SRow("Theme, icons & text","Theme, accent, app icon style, font, glass aur motion",LdIcons.Palette){onOpen("appearance")}
   SRow("Clean UI & call icons","App ka clean-glass mode; call icons ke alag style/rang",LdIcons.Edit,last=true){onOpen("callicons")}
  }
  SecTitle("Screen layouts")
  GlassCard{
   SRow("Home & dialer layout","Recents density, search position, contact names; dialer layout Theme section mein",LdIcons.Dialpad){onOpen("settings:display")}
   SRow("Call & Recents styles","Saved style aur live preview",LdIcons.Palette){onOpen("stylestudio")}
   SRow("Calling card layout","Photo, background, button order, size aur visibility; End hamesha available",LdIcons.Phone,last=true){onOpen("calllayout")}
  }
  SecTitle("Lighter calling card")
  GlassCard{
   SwitchRow("Airy call controls","Liquid Glass style ke inactive call buttons par softer fill aur patli rim. Active controls aur red End wahi.",s.airyCall){set(K.airyCall,it)}
   LdText("Preview · koi call nahi",11f,color=p.muted,maxLines=2)
   CompositionLocalProvider(LocalAiryCall provides s.airyCall,LocalCallStyle provides callStyleFor("glass")){
    Row(Modifier.fillMaxWidth().padding(vertical=12.dp).testTag("airy_call_preview"),horizontalArrangement=Arrangement.SpaceEvenly){CallBtn(LdIcons.Mic,"Mute",size=54.dp){};CallBtn(LdIcons.Dialpad,"Keypad",size=54.dp){};CallBtn(LdIcons.Volume,"Speaker",on=true,size=54.dp){}}
   }
   LdText("Sirf Liquid Glass inactive buttons par. Doosre selected call styles, icon overrides aur saved layout nahi badlenge.",11.5f,color=p.muted,maxLines=6)
  }
  SecTitle("Place-call confirmation")
  GlassCard{
   SwitchRow("Premium confirmation finish","Same size. Soft tinted glass aur quieter Cancel button. Call action sirf aapke tap se.",s.premiumConfirm){set(K.premiumConfirm,it)}
   SwitchRow("List call confirmation","Recents/Contacts call tap par pehle poochho. Voice call hamesha confirm hoti hai.",s.confirmCall){set(K.confirmCall,it)}
  }
  LdText("Live preview · sample number, buttons koi call nahi lagate",11.5f,color=p.muted,maxLines=3)
  Box(Modifier.fillMaxWidth().heightIn(min=300.dp).testTag("confirm_finish_preview")){
   CompositionLocalProvider(LocalConfirmPreview provides s.premiumConfirm){VoiceConfirmCard(VoiceMatch("Railway","139"),androidx.compose.ui.Alignment.Center,onCall={},onCancel={})}
  }
  Spacer(Modifier.height(12.dp))
  LdText("Yeh tested presets aur settings hain, har element ko freely drag karne wala editor nahi. Android status/navigation icons aur carrier UI phone ke control mein hain. Airy/confirmation finish local hain; appearance backup abhi in dono ko include nahi karta.",11.5f,color=p.muted,maxLines=12)
 }
}
