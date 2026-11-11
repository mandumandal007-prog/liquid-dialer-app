package com.mandu.liquiddialer.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun LookProfilesPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope()
 val parsed=remember(s.lookProfiles){runCatching{LookProfiles.decode(s.lookProfiles)}};val rows=parsed.getOrDefault(emptyList())
 var name by remember{mutableStateOf("")};var editor by remember{mutableStateOf<String?>(null)}
 var preview by remember{mutableStateOf<LookProfile?>(null)};var delete by remember{mutableStateOf<LookProfile?>(null)}
 var undo by remember{mutableStateOf<Pair<Look,Look>?>(null)};var error by remember{mutableStateOf("")};var notice by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
 fun library(change:(List<LookProfile>)->List<LookProfile>){if(busy)return;val expected=s.lookProfiles;busy=true;scope.launch{try{LdApp.instance.ldStore.edit{prefs->val raw=prefs[K.lookProfiles]?:"";require(raw==expected){"Profiles badal gaye. Dobara kholo."};prefs[K.lookProfiles]=LookProfiles.encode(change(LookProfiles.decode(raw)))};editor=null;name="";delete=null;error="";notice="Profile library save hui."}catch(e:Exception){error=e.message?:"Profile save nahi hua."}finally{busy=false}}}
 fun apply(look:Look,expected:Look,restore:Boolean=false){if(busy)return;busy=true;scope.launch{try{LdApp.instance.ldStore.edit{prefs->require(Look.capture(prefs.toSettings())==expected){"Appearance badal chuki hai. Preview dobara kholo."};look.apply(prefs)};undo=if(restore)null else expected to look;preview=null;error="";notice=if(restore)"Pichhla look wapas aa gaya."else"Look apply hua. Sirf listed appearance controls badle."}catch(e:Exception){error=e.message?:"Look apply nahi hua."}finally{busy=false}}}
 var previewBase by remember{mutableStateOf<Look?>(null)}
 PageFrame("Look profiles",onBack){
  LdText("Apna look sambhal ke rakho",20f,FontWeight.Bold,maxLines=4)
  LdText("Pehle preview. Apply se theme, accent, glass, text, app icons, shared rows/headers, airy/confirmation finish aur dialer-key shape badlenge. Current call/Recents style, button order/visibility, call-icon overrides aur feature switches wahi rahenge. Koi call, recording, alert ya permission change nahi.",12.5f,color=p.muted,maxLines=14)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=5,modifier=Modifier.testTag("look_error"))
  if(notice.isNotEmpty())LdText(notice,13f,color=p.accent,maxLines=5,modifier=Modifier.testTag("look_notice"))
  if(parsed.isFailure)LdText("Saved profiles load nahi hue. Library overwrite nahi hogi. Built-in previews abhi bhi khol sakte ho.",13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("look_library_error"))
  undo?.let{pair->GhostButton("Pichhla look wapas",Modifier.testTag("look_undo")){apply(pair.first,pair.second,true)}}
  SecTitle("Coordinated previews")
  GlassCard{LookProfiles.presets(s).forEachIndexed{i,r->SRow(r.name,when(r.id){"calm"->"Light glass, outline icons, quieter rows aur headers";"royal"->"Dark glass, duotone icons aur refined headers";else->"Light, larger bold text, static opaque glass"},LdIcons.Palette,last=i==2){previewBase=Look.capture(s);preview=r;error=""}}}
  SecTitle("My saved looks · ${rows.size}/${LookProfiles.MAX}")
  if(parsed.isSuccess){
   if(editor==null)PrimaryButton("Current look save karo",enabled=!busy&&rows.size<LookProfiles.MAX,modifier=Modifier.testTag("look_save_current")){editor="";name="";notice=""}
   editor?.let{id->
    GlassCard{
     LdField(name,{name=it.take(32)},"Look ka naam")
     PrimaryButton(if(id.isEmpty())"Naam ke saath save"else"Naam badlo",enabled=!busy,modifier=Modifier.testTag("look_save_name")){if(id.isEmpty()){val current=Look.capture(s);library{LookProfiles.add(it,name,current)}}else library{LookProfiles.rename(it,id,name)}}
     GhostButton("Rehne do",Modifier.testTag("look_edit_cancel")){editor=null;name=""}
    }
   }
   if(rows.isEmpty())LdText("Abhi koi saved look nahi. Current look save karke naya preview try karo.",13f,color=p.muted,maxLines=4)
   rows.forEach{r->GlassCard(Modifier.padding(top=10.dp)){
    SRow(r.name,"${r.look.theme} · text ${r.look.font}× · app icons ${LdIcons.ICON_STYLES[r.look.icons]}",LdIcons.Palette,last=true){previewBase=Look.capture(s);preview=r;error=""}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
     Box(Modifier.weight(1f)){GhostButton("Naam badlo",Modifier.testTag("look_rename_${r.id}")){editor=r.id;name=r.name}}
     Box(Modifier.weight(1f)){GhostButton("Delete",Modifier.testTag("look_delete_${r.id}")){delete=r}}
    }
   }}
  }
  Spacer(Modifier.height(14.dp));LdText("Profiles isi phone par hain, backup nahi. App data clear/uninstall se ja sakte hain. Delete profile se applied look ya personal data nahi badlega. Individual call-icon overrides ko profile replace nahi karta.",12f,color=p.muted,maxLines=10)
 }
 preview?.let{r->Sheet(onDismiss={if(!busy)preview=null},opaqueBacking=true){
  SheetTitle(r.name+" · preview only")
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("look_preview_error"))
  LdText("Sample mein koi call ya data action nahi. Apply se pehle current appearance dobara check hoti hai.",12.5f,color=p.muted,maxLines=5)
  Spacer(Modifier.height(10.dp));LdThemeContent(r.look.settings(s)) {LookPreviewCard()}
  Spacer(Modifier.height(12.dp));PrimaryButton("Yeh look apply karo",enabled=!busy,modifier=Modifier.testTag("look_apply")){previewBase?.let{apply(r.look,it)}}
  Spacer(Modifier.height(10.dp));GhostButton("Bina apply kiye band",Modifier.testTag("look_preview_close")){preview=null}
 }}
 delete?.let{r->Sheet(onDismiss={delete=null}){SheetTitle("Profile delete karein?");LdText(r.name+" sirf saved library se hatega. Current appearance wahi rahegi.",14f,maxLines=5);PrimaryButton("Haan, saved profile delete",enabled=!busy,danger=true,modifier=Modifier.testTag("look_delete_confirm")){library{LookProfiles.remove(it,r.id)}};GhostButton("Rehne do"){delete=null}}}
}

@Composable internal fun LookPreviewCard(){
 val p=LocalLd.current
 Column(Modifier.fillMaxWidth().background(p.bg,RoundedCornerShape(22.dp)).border(1.dp,p.stroke,RoundedCornerShape(22.dp)).padding(14.dp).testTag("look_preview"),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(Modifier.fillMaxWidth().background(p.bg2,RoundedCornerShape(16.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
   LdIcon(LdIcons.ArrowLeft,22.dp,if(LocalRefinedHeaders.current)p.accent else p.text)
   LdText("Workspace",18f,FontWeight.Bold,maxLines=3)
  }
  GlassCard(padding=12.dp){SRow("Sample contact","Icons aur shared-row finish",LdIcons.User,last=true)}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){LdIcon(LdIcons.Phone,26.dp,p.accent);LdIcon(LdIcons.Search,26.dp,p.accent);LdIcon(LdIcons.Settings,26.dp,p.accent);LdIcon(LdIcons.Checklist,26.dp,p.accent)}
  LdText("Aa · 123 · घर",18f,FontWeight.SemiBold,maxLines=3)
  LdText("Sirf listed appearance controls. Current call/Recents style aur individual call icons preserve honge.",11.5f,color=p.muted,maxLines=7)
 }
}
