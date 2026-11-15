package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch
@Composable fun LiveNotesRefreshPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Live saved notes refresh",onBack){
  LdText("Save kiya. List mein dekho.",22f,FontWeight.Bold,maxLines=4)
  LdText("Call notes khule hon aur checkpoint summary ya doosre local note tool se Save karo: isi number ki saved-notes list refresh hogi, desk close kiye bina. Naya note draft apni jagah rahega.",14f,color=p.muted,maxLines=10)
  GlassCard{SwitchRow("Live saved notes refresh","Default OFF. Sirf visible call notes ki read-only list.",s.liveNotesRefresh,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.liveNotesRefresh]=v};error=false}catch(_:Exception){error=true}}}}
  SecTitle("Kya badlega")
  GlassCard{
   LdText("Live call desk aur ordinary call Notes dono mein local saves/edits/deletes ke baad list current hoti hai. Yeh note khud save nahi karta. Save note aur doosre tools ke Save alag hain.",13f,maxLines=9)
   Spacer(Modifier.height(12.dp));LdText("Reload saved notes se manually dobara padho. Unsaved draft same rahega. Load fail par stale list hide hoti hai, error dikhta hai; saved file aur draft nahi badalte.",13f,maxLines=9)
  }
  SecTitle("Local refresh, cloud sync nahi")
  LdText("Sirf is app process ke successful local note writes ki signal. File bahar se badli ho to Reload dabao. OFF par live refresh aur Reload button band; existing list aur draft rehte hain. Call/number change par naya number hi load hota hai. Tool ke andar ho to saved list notes par lautne ke baad dikhegi.",13f,color=p.muted,maxLines=14)
  GlassCard{LdText("No background polling, network, clipboard, message, audio ya new permission. Notes existing unencrypted phone storage mein hain; backup rules same. Notes feature visible hona chahiye. Yeh switch Live call desk ya checkpoints automatically enable nahi karta.",13f,maxLines=12)}
  if(error)LdText("Setting save nahi hui",13f,color=p.danger)
 }
}
