package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun FeatureControlsPage(onBack:()->Unit,onOpen:(String)->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val scope=rememberCoroutineScope();var query by remember{mutableStateOf("")};var error by remember{mutableStateOf("")}
 PageFrame("Feature controls",onBack){
  GlassCard(Modifier.padding(bottom=12.dp)){
   LdText("Apna workspace chuno",20f,FontWeight.Bold,maxLines=3)
   LdText("Yeh optional pages aur shortcuts dikhate/chhupate hain. Off se saved data nahi mitata, permission revoke nahi hoti aur scheduled alerts/auto-record band nahi hote. Reminder cancel uske management page par; baaki behavior controls neeche hain.",12.5f,color=p.muted,maxLines=10)
   Spacer(Modifier.height(10.dp));LdText("${FeatureControls.all.count{FeatureControls.enabled(s,it.id)}} / ${FeatureControls.all.size} visible",13f,FontWeight.Bold,p.accent,modifier=Modifier.testTag("features_count"))
  }
  LdField(query,{query=it.take(80)},"Control khojo")
  if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=4,modifier=Modifier.testTag("features_error"))
  val results=FeatureControls.all.filter{(it.title+" "+it.group+" "+it.detail).contains(query.trim(),ignoreCase=true)}
  if(results.isEmpty())LdText("Koi control nahi mila",14f,maxLines=3)
  results.groupBy{it.group}.forEach{(group,items)->SecTitle(group);GlassCard{items.forEachIndexed{i,f->
   Box(Modifier.testTag("feature_"+f.id)){SwitchRow(f.title,f.detail,FeatureControls.enabled(s,f.id),last=i==items.lastIndex,subMaxLines=6){on->scope.launch{try{LdApp.instance.ldStore.edit{prefs->prefs[K.hiddenFeatures]=FeatureControls.toggled(prefs[K.hiddenFeatures].orEmpty(),f.id,on)};error=""}catch(_:Exception){error="Control save nahi hua. Dobara try karo."}}}}
  }}}
  SecTitle("Behavior & safety")
  GlassCard{
   SRow("Call behavior","SIM, confirm, undo, speaker, proximity",LdIcons.Phone){onOpen("settings:calls")}
   SRow("Notification behavior","Missed-call, birthday aur summary alerts",LdIcons.Clock){onOpen("settings:notifications")}
   SRow("Recording behavior","Auto-record aur unknown-only",LdIcons.Record){onOpen("settings:recording")}
   SRow("Blocking behavior","Screening behavior ko alag control karo",LdIcons.Block){onOpen("settings:blocking")}
   SRow("Quiet hours","Silent time aur favourite exceptions",LdIcons.Clock){onOpen("settings:quiet")}
   SRow("Layout, styles & icons","Theme, size, button order aur individual call controls",LdIcons.Palette,last=true){onOpen("appearancehub")}
  }
  Spacer(Modifier.height(12.dp));LdText("Answer/End, emergency calling, Settings aur permission recovery chhupaye nahi jaate. Android permission, default-dialer role, status/navigation icons aur carrier settings system ke control mein hain. Notes/library chhupana data lock ya encryption nahi hai.",12f,color=p.muted,maxLines=12,modifier=Modifier.testTag("features_safety"))
 }
}
@Composable fun FeatureDisabledPage(onBack:()->Unit,onOpen:(String)->Unit){PageFrame("Feature hidden",onBack){LdText("Yeh feature Settings mein hidden hai. Saved data wahi hai.",16f,maxLines=5);PrimaryButton("Feature controls kholo",modifier=Modifier.testTag("hidden_feature_settings")){onOpen("featurecontrols")}}}
