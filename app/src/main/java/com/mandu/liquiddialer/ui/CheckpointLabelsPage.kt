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
@Composable fun CheckpointLabelsPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Checkpoint label editor",onBack){
  LdText("Moment abhi. Label baad mein.",22f,FontWeight.Bold,maxLines=4)
  LdText("Call par jaldi mein? Blank label ke saath Mark now se moment rakho. Phir usi mark par Edit label se reference ID, faisla ya apna short label likho. Captured elapsed time nahi badlega.",14f,color=p.muted,maxLines=10)
  GlassCard{SwitchRow("Checkpoint label editor","Default OFF. Call checkpoints aur Live call desk bhi ON chahiye.",s.checkpointLabels,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.checkpointLabels]=v};error=false}catch(_:Exception){error=true}}}}
  if(!s.callCheckpoints||!s.liveCallDesk)LdText("Settings > Call checkpoints aur Live call desk enable karo. Yeh switch unhe automatically ON nahi karta.",13f,color=p.accent,maxLines=6)
  SecTitle("Pehle apply, phir preview")
  GlassCard{
   LdText("1. Live call desk > Call checkpoints kholo aur Mark now karo. Har unsaved row ka Edit label exact number, mark index aur purana elapsed time dikhata hai.",13f,maxLines=8)
   Spacer(Modifier.height(12.dp));LdText("2. Label badlo, Apply label dabao. Keep original se draft label discard. Edit ke dauran mark/remove/preview blocked rehte hain. 80 characters tak; blank = Checkpoint. Time, row order aur doosre marks same rehte hain.",13f,maxLines=9)
   Spacer(Modifier.height(12.dp));LdText("3. Preview note summary mein applied labels padho, phir Save one note. Apply khud note save nahi karta. Saved board locked hai; yeh saved notes ko edit nahi karta.",13f,maxLines=8)
  }
  SecTitle("Session only")
  LdText("Editor OFF karne par unfinished edit discard, pehle se applied session labels aur marks rehte hain. Checkpoints OFF, call/desk change ya recreation par unsaved board kho sakta hai. Existing saved notes stay. No audio, automatic recognition, transcription, clipboard, message, cloud ya permission action.",13f,color=p.muted,maxLines=14)
  if(error)LdText("Setting save nahi hui",13f,color=p.danger)
 }
}
