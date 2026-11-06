package com.mandu.liquiddialer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.AutoBackup
import com.mandu.liquiddialer.data.FullBackup
import com.mandu.liquiddialer.data.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Poora app backup page: user-picked JSON file me export, aur preview+confirm ke baad hi restore.
 * Restore hamesha merge karta hai - phone ka existing data kabhi delete/overwrite nahi hota.
 * Recordings, photos aur call history jaan-boojh kar is scope ke bahar hain (recordings ka apna
 * copy button hai; call log phone ka system sambhalta hai).
 */
@Composable fun BackupPage(onBack:()->Unit) {
    val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
    val s by LdApp.settings.collectAsState()
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")};var notice by remember {mutableStateOf("")}
    var preview by remember {mutableStateOf<FullBackup.Plan?>(null)}
    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null){busy=true;error="";notice="";scope.launch{
            try{
                val text=withContext(Dispatchers.IO){FullBackup.export(ctx)}
                withContext(Dispatchers.IO){ctx.contentResolver.openOutputStream(uri)?.use{it.write(text.toByteArray(Charsets.UTF_8))} ?: error("File nahi khuli")}
                notice="Poora backup save ho gaya. Is file me private data hai - safe jagah rakho."
            }catch(e:Exception){error=e.message ?: "Backup save nahi hua. Adhuri banayi gayi file use mat karo; app ka data nahi badla."}
            finally{busy=false}
        }}
    }
    val restoreLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){busy=true;error="";notice="";scope.launch{
            try{
                val text=withContext(Dispatchers.IO){FullBackup.read(ctx,uri)}
                preview=withContext(Dispatchers.IO){FullBackup.parse(text)}
            }catch(e:Exception){error="Backup file theek nahi hai: "+(e.message ?: "padhi nahi ja saki")}
            finally{busy=false}
        }}
    }
    PageFrame("Poora backup",{if(!busy)onBack()}) {
        LdText("Appearance settings, notes, call reminders, call lists aur blocked number prefixes ek JSON file me save karo. Restore se pehle poora preview dikhta hai aur tumhari existing cheezein kabhi delete nahi hoti - sirf nayi judti hain.",12f,color=p.muted,maxLines=8)
        LdText("Isme recordings, photos aur call history NAHI hain: recordings ka apna copy button hai, call log phone ka system sambhalta hai. Backup file me private data hai; kisi ko bhejne se pehle socho.",12f,color=p.muted,maxLines=8,modifier=Modifier.padding(vertical=10.dp))
        if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("backup_error"))
        if(notice.isNotEmpty())LdText(notice,13f,color=p.accent,maxLines=6,modifier=Modifier.testTag("backup_notice"))
        PrimaryButton(if(busy)"Kaam chal raha hai..." else "Backup file save karo",enabled=!busy,modifier=Modifier.testTag("backup_export")){
            error="";notice=""
            exportLauncher.launch("liquid-dialer-backup-${SimpleDateFormat("yyyyMMdd",Locale.US).format(Date())}.json")
        }
        PrimaryButton("Backup file se restore karo",enabled=!busy,modifier=Modifier.padding(top=10.dp).testTag("backup_restore")){
            error="";notice=""
            restoreLauncher.launch(arrayOf("application/json","text/plain","*/*"))
        }
        // Opt-in khud backup: switch + status; files sirf is phone ke private folder me rehti hain
        GlassCard(Modifier.padding(top=12.dp).testTag("autobackup_card")) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    LdText("Khud backup har hafte",15f,FontWeight.Bold)
                    LdText("App khulne par, pichla backup 7 din se purana ho to khud phone ke private folder me save hota hai (aakhri 3 files rakhta hai). Sirf isi phone par rehta hai. Recordings, photos aur call history isme nahi hain.",12f,color=p.muted,maxLines=8,modifier=Modifier.padding(top=4.dp))
                }
                LdSwitch(s.autoBackup){on->scope.launch{LdApp.instance.repo.set(K.autoBackup,on)}}
            }
            if(s.autoBackup) {
                val autoFiles=remember(s.lastAutoBackupAt){AutoBackup.files(ctx)}
                LdText(if(s.lastAutoBackupAt<=0L) "Abhi tak auto-backup nahi bana - agli baar app khulne par ban jayega." else "Aakhri auto-backup: ${SimpleDateFormat("d MMM, h:mm a",Locale.getDefault()).format(Date(s.lastAutoBackupAt))} · ${autoFiles.size} ${if(autoFiles.size==1)"file" else "files"} phone me saved",12f,color=p.accent,maxLines=4,modifier=Modifier.padding(top=8.dp).testTag("autobackup_status"))
            }
        }
        preview?.let{plan->
            GlassCard(Modifier.padding(top=12.dp).testTag("backup_preview")) {
                LdText("Ye backup restore karein?",15f,FontWeight.Bold)
                LdText("Notes: ${plan.notes.size} | Reminders: ${plan.reminders.size} | Lists: ${plan.lists.size} | Blocked prefixes: ${plan.blockedPrefixes.size}"+(if(plan.settings!=null)" | Appearance settings: haan" else ""),13f,color=p.muted,maxLines=6,modifier=Modifier.padding(vertical=6.dp).testTag("backup_preview_text"))
                LdText("Sirf nayi cheezein judengi; phone ka existing data delete ya overwrite nahi hoga.",12f,color=p.muted,maxLines=4)
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.padding(top=10.dp)) {
                    PrimaryButton("Restore karo",enabled=!busy,modifier=Modifier.weight(1f).testTag("backup_restore_confirm")){
                        val pl=plan;preview=null;busy=true;error="";notice="";scope.launch{
                            try{
                                val s=withContext(Dispatchers.IO){FullBackup.apply(ctx,pl)}
                                notice="Restore ho gaya: ${s.notesAdded} naye notes, ${s.remindersSet} reminders, ${s.listsAdded} lists, ${s.prefixesAdded} prefixes"+(if(s.settingsApplied)"; appearance settings bhi" else "")
                            }catch(e:Exception){error="Restore poora nahi hua: "+(e.message ?: "Dobara koshish karo")}
                            finally{busy=false}
                        }
                    }
                    PrimaryButton("Rehne do",enabled=!busy,modifier=Modifier.weight(1f).testTag("backup_restore_cancel")){preview=null}
                }
            }
        }
    }
}
