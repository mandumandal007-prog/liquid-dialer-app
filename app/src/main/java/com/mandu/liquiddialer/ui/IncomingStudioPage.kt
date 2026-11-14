package com.mandu.liquiddialer.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun IncomingStudioPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
 val parsed=remember(s.incomingProfiles){runCatching{IncomingProfiles.decode(s.incomingProfiles)}};val rows=parsed.getOrDefault(emptyList())
 val current=remember(s.incomingCard){runCatching{if(s.incomingCard.isEmpty())IncomingCard.Config()else IncomingCard.decodeStrict(s.incomingCard)}}
 var editor by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")};var error by remember{mutableStateOf("")};var notice by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
 var editorLibrary by remember{mutableStateOf("")};var deleteLibrary by remember{mutableStateOf("")}
 var preview by remember{mutableStateOf<IncomingProfiles.Plan?>(null)};var confirmApply by remember{mutableStateOf(false)};var delete by remember{mutableStateOf<IncomingProfiles.Profile?>(null)};var undo by remember{mutableStateOf<IncomingProfiles.Plan?>(null)}
 var trial by remember{mutableStateOf<IncomingCard.Config?>(null)};var trialTitle by remember{mutableStateOf("")}
 LaunchedEffect(s.incomingStudio){if(!s.incomingStudio){trial=null;preview=null;confirmApply=false;delete=null;editor=null}}
 fun library(before:String,expectedCurrent:String?=null,change:(List<IncomingProfiles.Profile>)->List<IncomingProfiles.Profile>){if(busy)return;busy=true;scope.launch{try{ctx.ldStore.edit{prefs->require(prefs[K.incomingStudio]==true){"Studio is OFF"};require((prefs[K.incomingProfiles]?:"")==before){"Profiles changed. Try again"};if(expectedCurrent!=null)require((prefs[K.incomingCard]?:"")==expectedCurrent){"Incoming choices changed. Try saving again"};prefs[K.incomingProfiles]=IncomingProfiles.encode(change(IncomingProfiles.decode(before)))};editor=null;name="";delete=null;error="";notice="Profile library saved. Incoming settings unchanged"}catch(e:Exception){error=e.message?:"Profile save failed"}finally{busy=false}}}
 fun apply(plan:IncomingProfiles.Plan,restore:Boolean=false){if(busy)return;busy=true;scope.launch{try{ctx.ldStore.edit{prefs->require(prefs[K.incomingStudio]==true){"Studio is OFF"};val raw=prefs[K.incomingCard]?:"";prefs[K.incomingCard]=if(restore)IncomingProfiles.undo(plan,raw)else IncomingProfiles.apply(plan,raw,prefs[K.incomingProfiles]?:"")};undo=if(restore)null else plan;preview=null;confirmApply=false;error="";notice=if(restore)"Previous incoming choices restored"else"Incoming choices applied. Master ON/OFF was preserved"}catch(e:Exception){error=e.message?:"Apply failed";confirmApply=false}finally{busy=false}}}
 trial?.let{c->IncomingRehearsalPage(c,trialTitle,{trial=null});return}
 PageFrame("Incoming try-out and profiles",onBack){
  Box(Modifier.testTag("studio_enable")){SwitchRow("Incoming try-out and profiles","Default OFF. Hide this studio without deleting profiles or changing current incoming choices.",s.incomingStudio,last=true,subMaxLines=5){v->scope.launch{ctx.ldStore.edit{it[K.incomingStudio]=v}}}}
  if(!s.incomingStudio){LdText("Studio OFF. Enable to rehearse controls and manage saved profiles. The existing incoming master is separate.",14f,color=p.muted,maxLines=6,modifier=Modifier.testTag("studio_off"));return@PageFrame}
  LdText("Try first. Keep your choices.",23f,FontWeight.Bold,maxLines=4)
  LdText("Rehearsal uses a fictional caller and the same Answer/Cut controls, but cannot call, answer, decline, send, record or open another app. Sample identity/background is illustrative, not a real caller-photo preview. No contact, photo, notes or history lookup. Close to return.",13f,color=p.muted,maxLines=12)
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=8,modifier=Modifier.testTag("studio_error"))
  if(notice.isNotEmpty())LdText(notice,13f,color=p.accent,maxLines=5,modifier=Modifier.testTag("studio_notice"))
  if(current.isFailure)LdText("Current incoming choices could not be read. Save/apply disabled; existing data will not be overwritten.",13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("studio_current_error"))
  if(current.isSuccess){val c=current.getOrThrow();LdText("Incoming master is ${if(c.enabled)"ON"else"OFF"}. Saved profiles exclude that switch, shared theme/scale/icons and personal data.",13f,color=p.muted,maxLines=6)
   PrimaryButton("Rehearse current saved choices",enabled=!busy,modifier=Modifier.testTag("studio_trial_current")){trial=c.copy(enabled=true);trialTitle="Current incoming choices"}
   LdText("Rehearsal treats incoming overrides as ON only in the sample, even if the real master is OFF. No setting changes. Voice/video sample switch and retry are inside.",12f,color=p.muted,maxLines=7)
  }
  undo?.let{plan->GhostButton("Undo last profile apply",Modifier.testTag("studio_undo")){apply(plan,true)};LdText("Undo is available only on this open page and only if incoming choices have not changed since Apply. Profiles are not removed by Undo.",12f,color=p.muted,maxLines=6)}
  SecTitle("My incoming profiles · ${rows.size}/${IncomingProfiles.MAX}")
  if(parsed.isFailure)LdText("Saved profiles could not be read. Library is locked; nothing will overwrite it. Current-choice rehearsal still works if its data is valid.",13f,color=p.danger,maxLines=8,modifier=Modifier.testTag("studio_library_error"))
  if(parsed.isSuccess){
   if(editor==null)PrimaryButton("Save current choices as profile",enabled=!busy&&rows.size<IncomingProfiles.MAX&&current.isSuccess,modifier=Modifier.testTag("studio_save_current")){editorLibrary=s.incomingProfiles;editor="";name=""}
   editor?.let{id->GlassCard{
    LdField(name,{name=it.take(40)},"Profile name (1-32 characters)",modifier=Modifier.testTag("studio_name"))
    PrimaryButton(if(id.isEmpty())"Save named choices"else"Rename profile",enabled=!busy&&(id.isNotEmpty()||current.isSuccess),modifier=Modifier.testTag("studio_save_name")){if(id.isEmpty()){val c=current.getOrNull();if(c==null)error="Current incoming choices could not be read"else library(editorLibrary,s.incomingCard){IncomingProfiles.capture(it,name,c)}}else library(editorLibrary){IncomingProfiles.rename(it,id,name)}}
    GhostButton("Cancel edit",Modifier.testTag("studio_edit_cancel")){editor=null;name=""}
   }}
   if(rows.isEmpty())LdText("No saved incoming profiles yet. Choices are saved on this phone only, not in backup.",13f,color=p.muted,maxLines=5,modifier=Modifier.testTag("studio_empty"))
   rows.forEach{r->GlassCard(Modifier.padding(top=10.dp)){
    LdText(r.name,18f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("studio_profile_${r.id}"))
    LdText("${callStyleFor(r.choices.style).label} · ${if(r.choices.swipe)"Swipe ${r.choices.travel}dp"else"Tap"} · Answer ${if(r.choices.swap)"left"else"right"}",13f,color=p.muted,maxLines=6)
    PrimaryButton("Compare / try",enabled=!busy&&current.isSuccess,modifier=Modifier.testTag("studio_compare_${r.id}")){try{preview=IncomingProfiles.plan(s.incomingCard,s.incomingProfiles,r.id);error=""}catch(e:Exception){error=e.message?:"Comparison failed"}}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
     Box(Modifier.weight(1f)){GhostButton("Rename",Modifier.testTag("studio_rename_${r.id}")){editorLibrary=s.incomingProfiles;editor=r.id;name=r.name}}
     Box(Modifier.weight(1f)){GhostButton("Delete",Modifier.testTag("studio_delete_${r.id}")){deleteLibrary=s.incomingProfiles;delete=r}}
    }
   }}
  }
  LdText("Profiles hold incoming-only choices, including swipe/tap, side swap, shortcuts and Cut confirmation. Apply never changes the incoming master, active/outgoing controls, contacts, photos, messages, permissions, sound or call history. Delete removes only the saved profile; uninstall/data clear can remove the library.",12f,color=p.muted,maxLines=12)
 }
 preview?.let{plan->val target=IncomingCard.decodeStrict(plan.after);val diffs=IncomingProfiles.differences(if(plan.before.isEmpty())IncomingCard.Config()else IncomingCard.decodeStrict(plan.before),target)
  Sheet(onDismiss={if(!busy){preview=null;confirmApply=false}},opaqueBacking=true){
   SheetTitle("Compare incoming choices")
   LdText("Incoming master stays ${if(target.enabled)"ON"else"OFF"}. ${diffs.size} changed controls. All other settings stay unchanged.",14f,FontWeight.Bold,maxLines=6,modifier=Modifier.testTag("studio_diff_count"))
   if(diffs.isEmpty())LdText("No choice changes",14f,maxLines=4)
   diffs.forEachIndexed{i,line->LdText(line,13f,maxLines=5,modifier=Modifier.testTag("studio_diff_$i"))}
   LdText("true = ON, false = OFF. Full before/after shown for changes only. Apply rechecks both incoming settings and library before saving.",12f,color=p.muted,maxLines=7)
   if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("studio_compare_error"))
   GhostButton("Rehearse this profile",Modifier.testTag("studio_trial_profile")){trial=target.copy(enabled=true);trialTitle="Saved incoming profile"}
   PrimaryButton("Apply profile choices",enabled=!busy&&diffs.isNotEmpty(),modifier=Modifier.testTag("studio_apply")){confirmApply=true}
   GhostButton("Close comparison",Modifier.testTag("studio_compare_close")){preview=null;confirmApply=false}
  }
 }
 if(confirmApply)Dialog(onDismissRequest={if(!busy)confirmApply=false}){GlassCard{
  LdText("Apply incoming profile choices?",18f,FontWeight.Bold,maxLines=5)
  LdText("Changes the listed incoming-only controls. The incoming master stays as it was; active/outgoing controls stay unchanged. No call action happens.",13f,color=p.muted,maxLines=8)
  PrimaryButton("Confirm apply",enabled=!busy,modifier=Modifier.testTag("studio_apply_confirm")){preview?.let{apply(it)}}
  GhostButton("Keep current",Modifier.testTag("studio_apply_cancel")){confirmApply=false}
 }}
 delete?.let{r->Dialog(onDismissRequest={if(!busy)delete=null}){GlassCard{
  LdText("Delete saved profile?",18f,FontWeight.Bold,maxLines=5);LdText(r.name+" will be removed only from the library. Incoming settings stay unchanged.",13f,color=p.muted,maxLines=8)
  PrimaryButton("Delete saved profile",enabled=!busy,danger=true,modifier=Modifier.testTag("studio_delete_confirm")){library(deleteLibrary){IncomingProfiles.remove(it,r.id)}}
  GhostButton("Keep profile",Modifier.testTag("studio_delete_cancel")){delete=null}
 }}}
}

@Composable internal fun IncomingRehearsalPage(choices:IncomingCard.Config,title:String,onBack:()->Unit){
 val c=choices.copy(enabled=true);val base=LocalLd.current;val style=callStyleFor(c.style)
 val p=if(c.background in setOf("blobs","solid"))base else if(c.background=="photo")base.copy(bg=Color(0xFF101A2B),bg2=Color(0xFF18263D),text=Color.White,muted=Color(0xFFD4DDED),accent=Color(0xFFB7D2FF),isLight=false)else style.restyle(base)
 var video by remember{mutableStateOf(false)};var epoch by remember{mutableIntStateOf(0)};var action by remember{mutableStateOf("No demo action yet")}
 CompositionLocalProvider(LocalLd provides p,LocalCallStyle provides style,LocalCallGlass provides true,LocalCallPhoto provides false){
 PageFrame("Incoming control rehearsal",onBack){
  LdText("SIMULATION ONLY",17f,FontWeight.ExtraBold,color=p.accent,modifier=Modifier.testTag("studio_demo_label"))
  LdText(title,15f,FontWeight.Bold,maxLines=5)
  LdText("Fictional sample. No real call, microphone, camera, ringing, vibration, notification or message. Closing does not apply choices. Shared theme, scale and icon settings can make the live card look different; contact data and photos are never loaded here.",12f,color=p.muted,maxLines=10)
  Box(Modifier.testTag("studio_video_toggle")){SwitchRow("Video call sample","Show audio-only fallback without opening a camera",video,last=true,subMaxLines=4){video=it;epoch++;action="No demo action yet"}}
  GlassCard(Modifier.testTag("studio_sample")){
   Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){
    Avatar("Demo caller",null,(80*c.avatarSize/100f).dp,28f,seed="demo",shape=if(c.avatar=="circle")CircleShape else RoundedCornerShape(28))
    LdText("Demo caller",23*c.nameSize/100f,FontWeight.Bold,maxLines=2,modifier=Modifier.testTag("studio_demo_name"))
    if(c.number)LdText("Fictional number · 000 000",13f,color=p.muted,modifier=Modifier.testTag("studio_demo_number"))
    if(c.contactType)LdText("Sample contact type: Mobile",12f,color=p.muted)
    if(c.lastCall)LdText("Sample last call: yesterday",12f,color=p.muted)
    LdText(if(video)"Simulated incoming video"else"Simulated incoming voice",13f,color=p.accent,maxLines=4)
    if(c.whatsapp){GhostButton("WhatsApp demo",Modifier.testTag("studio_demo_whatsapp")){action="WhatsApp rehearsal only. Nothing opened or sent"};LdText("Demo only · does not open WhatsApp",12f,color=p.muted,maxLines=3)}
   }
  }
  LdText("Background choice: ${c.background}. Photo/inherit use an illustrative neutral sample, not an actual caller background.",12f,color=p.muted,maxLines=6)
  LdText(action,15f,FontWeight.Bold,maxLines=6,modifier=Modifier.testTag("studio_demo_action"))
  key(epoch,c,video){IncomingCardActions(c,video,{action="Cut rehearsal complete. No call declined"},{action="Answer rehearsal complete. No call answered"},{action="Audio-only rehearsal complete. No camera or call opened"},{action="Message rehearsal only. Nothing opened, sent or declined"},rehearsal=true)}
  GhostButton("Try again",Modifier.testTag("studio_demo_retry")){epoch++;action="No demo action yet"}
  GhostButton("Close without applying",Modifier.testTag("studio_demo_close"),onBack)
 }
 }
}
