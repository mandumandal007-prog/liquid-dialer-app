package com.mandu.liquiddialer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

val LocalCallIcons=staticCompositionLocalOf<CallIconConfig?>{null}
val CALL_ICON_COLORS=listOf("Original","Blue","Violet","Emerald","Rose","Amber","Mono")
fun callIconColor(index:Int,p:LdPalette,fallback:Color):Color=when(index){
 1->if(p.isLight)Color(0xFF2049AD)else Color(0xFFA9C8FF)
 2->if(p.isLight)Color(0xFF63349E)else Color(0xFFD3B3FF)
 3->if(p.isLight)Color(0xFF126648)else Color(0xFF89E5BB)
 4->if(p.isLight)Color(0xFF9F315F)else Color(0xFFFFB2D2)
 5->if(p.isLight)Color(0xFF845500)else Color(0xFFFFD585)
 6->p.text
 else->fallback
}
val CALL_ICON_ITEMS:List<Pair<String,ImageVector>> get()=listOf(
 "Minimize / chevron" to LdIcons.ChevronDown,"Video" to LdIcons.Video,"Notes / edit" to LdIcons.Edit,
 "Mute / microphone" to LdIcons.Mic,"Keypad" to LdIcons.Dialpad,"Recording" to LdIcons.Record,"Hold / resume" to LdIcons.Pause,
 "Add call" to LdIcons.Plus,"Phone / earpiece / answer" to LdIcons.Phone,"Audio route" to LdIcons.Volume,"Speaker" to LdIcons.SpeakerFill,
 "Hang up / decline" to LdIcons.CallEnd,"Bluetooth" to LdIcons.Bluetooth,"Merge" to LdIcons.Merge,"Swap" to LdIcons.Swap,
 "Conference" to LdIcons.Users,"Close / cancel" to LdIcons.X,"Message / reply" to LdIcons.Message,"SIM" to LdIcons.Sim,"Selected mark" to LdIcons.Check,"Split call" to LdIcons.PhoneOut)

@Composable fun CallIconStudioPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope()
 val config=remember(s.callIconConfig){CallIconConfig.decode(s.callIconConfig)}
 var selected by remember {mutableStateOf<String?>(null)}
 var menu by remember{mutableStateOf(false)}
 fun save(c:CallIconConfig){scope.launch{LdApp.instance.repo.set(K.callIconConfig,c.encode())}}
 val current=selected?.let{config.choice(it)} ?: config.base
 fun choose(v:CallIconChoice){save(if(selected==null)config.copy(base=v)else config.copy(overrides=config.overrides+(selected!! to v)))}
 PageFrame("UI aur call icons",onBack){
  Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){LdText("Clean glass · poora app",16f,FontWeight.Bold,maxLines=2);LdText("Kam effects, saaf glass cards. Theme ke rang wahi; background motion aur blur band.",12f,color=p.muted,maxLines=5)}
   Switch(s.cleanGlass,{on->scope.launch{LdApp.instance.repo.set(K.cleanGlass,on)}},modifier=Modifier.testTag("clean_glass"))
  }
  LdText("Band karne par apni saved glass aur motion settings wapas.",12f,color=p.muted,maxLines=4)

  SecTitle("Call screen")
  LdText("Yahan ke controls call screen aur uske sheets ke liye hain.",12f,color=p.muted,maxLines=3)
  Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){LdText("Premium glass polish",16f,FontWeight.Bold,maxLines=2);LdText("Avatar ring, soft rim aur saaf labels. Call actions wahi.",12f,color=p.muted,maxLines=3)}
   Switch(config.polish,{save(config.copy(polish=it))},modifier=Modifier.testTag("call_icons_polish"))
  }
  LdText("Sab icons ka style/rang chuno, ya ek icon alag set karo.",12f,color=p.muted,maxLines=3)
  LdText("Live preview · koi call nahi",12f,color=p.muted,maxLines=2)
  CompositionLocalProvider(LocalCallIcons provides config){
   GlassCard {CALL_ICON_ITEMS.take(12).chunked(6).forEach{row->Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.SpaceAround){row.forEach{(_,ic)->LdIcon(ic,26.dp,if(ic==LdIcons.CallEnd)Color.White else p.text,Modifier.background(if(ic==LdIcons.CallEnd)Color(0xFFD93025)else Color.Transparent,RoundedCornerShape(8.dp)).padding(4.dp))}}}}
  }
  Spacer(Modifier.height(12.dp))
  Box {
   PrimaryButton("Badlo: "+(selected?.let{id->CALL_ICON_ITEMS.firstOrNull{it.second.name==id}?.first} ?: "Sab icons"),modifier=Modifier.testTag("call_icons_target")){menu=true}
   DropdownMenu(menu,{menu=false}){
    DropdownMenuItem(text={Text("Sab icons")},modifier=Modifier.testTag("call_icons_all"),onClick={selected=null;menu=false})
    CALL_ICON_ITEMS.forEach{(label,ic)->DropdownMenuItem(text={Text(label)},modifier=Modifier.testTag("call_icons_pick_${ic.name}"),onClick={selected=ic.name;menu=false})}
   }
  }
  LdText(if(selected==null)"Sabka default. Alag set kiye icons apna override rakhenge." else if(selected in config.overrides)"Is icon ka alag override hai." else "Abhi sabka default use ho raha hai.",12f,color=p.muted,maxLines=3)
  SecTitle("Icon style")
  (listOf(-1 to "App jaisa")+LdIcons.ICON_STYLES.mapIndexed{i,x->i to x}).chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{(i,label)->PrimaryButton((if(current.style==i)"✓ "else "")+label,modifier=Modifier.weight(1f).testTag("call_icons_style_$i")){choose(current.copy(style=i))}};if(row.size==1)Spacer(Modifier.weight(1f))}}
  SecTitle("Icon rang")
  CALL_ICON_COLORS.mapIndexed{i,x->i to x}.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{(i,label)->PrimaryButton((if(current.color==i)"✓ "else "")+label,modifier=Modifier.weight(1f).testTag("call_icons_color_$i")){choose(current.copy(color=i))}};if(row.size==1)Spacer(Modifier.weight(1f))}}
  LdText("Rang light/dark background ke liye adjust hoga. Active controls ke white icons aur red hang-up/decline par white glyph suraksha ke liye rahenge; unka style badal sakte ho. Phone ke status/navigation-bar icons app ke control mein nahi.",12f,color=p.muted,maxLines=7)
  if(selected!=null)PrimaryButton("Is icon ko sabke default par lao",modifier=Modifier.testTag("call_icons_reset_one")){save(config.copy(overrides=config.overrides-selected!!))}
  PrimaryButton("Call icons aur polish reset",modifier=Modifier.testTag("call_icons_reset")){save(CallIconConfig())}
  LdText("Changes isi phone par save hote hain. Appearance-only backup abhi in naye controls ko include nahi karta.",11.5f,color=p.muted,maxLines=4)
 }
}
