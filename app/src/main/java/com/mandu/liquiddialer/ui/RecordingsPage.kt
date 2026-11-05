package com.mandu.liquiddialer.ui

import android.content.Intent
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.mandu.liquiddialer.data.RecordingLibrary as Library
import com.mandu.liquiddialer.telecom.CallManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable fun RecordingsPage(onBack:()->Unit) {
    val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope()
    var items by remember {mutableStateOf<List<Library.Item>>(emptyList())}
    var loaded by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")};var notice by remember {mutableStateOf("")}
    var query by rememberSaveable {mutableStateOf("")};var page by remember {mutableIntStateOf(0)}
    var rename by remember {mutableStateOf<Library.Item?>(null)};var title by rememberSaveable {mutableStateOf("")}
    var deleting by remember {mutableStateOf<Library.Item?>(null)}
    var export by rememberSaveable {mutableStateOf<String?>(null)}
    var exportBytes by rememberSaveable {mutableLongStateOf(0)};var exportTime by rememberSaveable {mutableLongStateOf(0)}
    var player by remember {mutableStateOf<MediaPlayer?>(null)};var selected by remember {mutableStateOf<Library.Item?>(null)}
    var playing by remember {mutableStateOf(false)};var preparing by remember {mutableStateOf(false)}
    val active by CallManager.recording.collectAsState()
    fun stop(){player?.release();player=null;selected=null;playing=false;preparing=false}
    fun refresh(){if(busy)return;busy=true;error="";scope.launch {
        try{items=withContext(Dispatchers.IO){Library.list(ctx)};loaded=true;page=0}
        catch(_:Exception){error="Recordings folder khul nahi raha. Dobara load karo."}
        finally{busy=false}
    }}
    fun mutate(action:()->Unit,done:String){if(busy)return;stop();busy=true;error="";notice="";scope.launch{
        try{withContext(Dispatchers.IO){action()};items=withContext(Dispatchers.IO){Library.list(ctx)};page=0;notice=done}
        catch(e:Exception){error=e.message ?: "Badlav nahi hua. Dobara load karo."}
        finally{busy=false}
    }}
    val saveCopy=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")){uri->
        val name=export;export=null
        if(uri!=null && name!=null){val snapshot=Library.Item(name,exportBytes,exportTime);busy=true;error="";scope.launch{
            try{withContext(Dispatchers.IO){Library.resolve(ctx,snapshot);ctx.contentResolver.openOutputStream(uri,"wt")?.use {Library.copy(ctx,snapshot,it)} ?: error("Output nahi khula")};notice="Recording ki copy save hui; original wahi hai"}
            catch(_:Exception){error="Copy save nahi hui. Adhuri destination file use mat karo; original recording nahi badli."}
            finally{busy=false}
        }}
    }
    DisposableEffect(Unit){onDispose{player?.release()}}
    LaunchedEffect(Unit){refresh()}
    LaunchedEffect(query){page=0}
    LaunchedEffect(active?.file?.name){if(active?.file?.name==selected?.fileName)stop()}
    val found=remember(items,query){Library.search(items,query)}
    val df=remember{SimpleDateFormat("d MMM yyyy, HH:mm",Locale.getDefault())}
    rename?.let{item->
        PageFrame("Recording ka naam",{rename=null}) {
            LdText(item.fileName,14f,FontWeight.Bold,maxLines=4)
            LdText("Sirf file ka naam badlega. Audio nahi badlega. .m4a apne aap rahega. 1-100 letters; slash mat lagao.",13f,color=p.muted,maxLines=6)
            OutlinedTextField(title,{title=it},label={Text("Naya naam")},maxLines=3,modifier=Modifier.fillMaxWidth().testTag("recording_name"))
            PrimaryButton("Naam save karo",enabled=title.isNotBlank(),modifier=Modifier.testTag("recording_rename_confirm")){val value=title;rename=null;mutate({Library.rename(ctx,item,value)},"Naam save hua")}
            PrimaryButton("Rehne do",modifier=Modifier.testTag("recording_rename_cancel")){rename=null}
        }
        return
    }
    PageFrame("Recordings",{if(!busy)onBack()}) {
        LdText("Saved audio ko khojo, naam do aur file ki copy save karo. Android/device ke hisab se dono taraf ki awaaz record hona guaranteed nahi hai. Ye page recording start nahi karta.",12f,color=p.muted,maxLines=7)
        LdText("Private audio hai. Copy/share tabhi karo jab sahi ho. App uninstall karne se originals ja sakte hain; copy alag jagah save karo.",12f,color=p.muted,maxLines=6,modifier=Modifier.padding(vertical=10.dp))
        if(error.isNotEmpty())LdText(error,13f,color=p.danger,maxLines=6,modifier=Modifier.testTag("recordings_error"))
        if(notice.isNotEmpty())LdText(notice,13f,color=p.accent,maxLines=4,modifier=Modifier.testTag("recordings_notice"))
        PrimaryButton("Dobara load karo",enabled=!busy,modifier=Modifier.testTag("recordings_refresh")){refresh()}
        if(!loaded)LdText(if(busy)"Recordings load ho rahi hain..." else "Load nahi hua",13f,color=p.muted,maxLines=3)
        if(loaded && items.isEmpty())EmptyState(LdIcons.Record,"Koi saved recording nahi","Call ke dauran khud Record dabane par bani files yahan dikhti hain")
        if(items.isNotEmpty()) {
            LdField(query,{query=it},"File ke naam se khojo",modifier=Modifier.testTag("recordings_search"))
            if(query.isNotBlank())TextButton(onClick={query=""},modifier=Modifier.testTag("recordings_clear")){Text("Search saaf karo")}
            LdText("${found.size} / ${items.size} files",12f,color=p.muted,modifier=Modifier.testTag("recordings_count"))
            if(found.isEmpty())LdText("Koi matching recording nahi",14f,maxLines=3)
            if(found.size>20){
                LdText("${page*20+1}-${minOf((page+1)*20,found.size)} / ${found.size}",12f,color=p.muted)
                if(page>0)PrimaryButton("Pichhli 20 files",enabled=!busy,modifier=Modifier.testTag("recordings_previous")){stop();page--}
                if((page+1)*20<found.size)PrimaryButton("Agli 20 files",enabled=!busy,modifier=Modifier.testTag("recordings_next")){stop();page++}
            }
        }
        found.drop(page*20).take(20).forEach {item->
            val inUse=active?.file?.name==item.fileName;val chosen=selected==item
            GlassCard(Modifier.padding(top=12.dp).testTag("recording_${item.fileName}")) {
                LdText(item.title.ifBlank{"Recording"},16f,FontWeight.Bold,maxLines=3)
                LdText(item.fileName,11f,color=p.muted,maxLines=3)
                LdText("${df.format(Date(item.modified))} · ${item.bytes/1024} KB",12f,color=p.muted,maxLines=3)
                if(inUse)LdText("Abhi recording chal rahi hai. Band hone ke baad dobara load karo.",13f,color=p.danger,maxLines=4)
                PrimaryButton(if(chosen && preparing)"Audio khul raha hai..." else if(chosen && playing)"Pause" else if(chosen)"Aage suno" else "Suno",enabled=!busy && !inUse && !preparing,modifier=Modifier.testTag("recording_play_${item.fileName}")) {
                    error=""
                    if(chosen && player!=null){try{if(playing){player?.pause();playing=false}else{player?.start();playing=true}}catch(_:Exception){error="Playback ruk gaya. Dobara kholo.";stop()}}
                    else {stop();try{
                        val f=Library.resolve(ctx,item);val mp=MediaPlayer();player=mp;selected=item;preparing=true
                        mp.setOnPreparedListener{if(player===it){preparing=false;it.start();playing=true}}
                        mp.setOnCompletionListener{if(player===it)stop()}
                        mp.setOnErrorListener{_,_,_->error="Audio play nahi hua; file format ya device support check karo";stop();true}
                        mp.setDataSource(f.absolutePath);mp.prepareAsync()
                    }catch(_:Exception){error="Audio play nahi hua. Dobara load karo.";stop()}}
                }
                if(chosen)PrimaryButton("Suno band karo",modifier=Modifier.testTag("recording_stop")){stop()}
                PrimaryButton("Naam badlo",enabled=!busy && !inUse,modifier=Modifier.testTag("recording_rename_${item.fileName}")){rename=item;title=item.fileName.substringBeforeLast('.')}
                PrimaryButton("Copy file save karo",enabled=!busy && !inUse,modifier=Modifier.testTag("recording_export_${item.fileName}")){
                    stop();export=item.fileName;exportBytes=item.bytes;exportTime=item.modified;saveCopy.launch(item.fileName)
                }
                PrimaryButton("Share options",enabled=!busy && !inUse,modifier=Modifier.testTag("recording_share_${item.fileName}")){
                    stop();try{val f=Library.resolve(ctx,item);val uri=FileProvider.getUriForFile(ctx,"com.mandu.liquiddialer.files",f)
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("audio/mp4").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Audio kahan share karna hai?"))
                    }catch(_:Exception){error="Share options nahi khule. Koi audio nahi bheja gaya."}
                }
                PrimaryButton("Recording delete karo",danger=true,enabled=!busy && !inUse,modifier=Modifier.testTag("recording_delete_${item.fileName}")){deleting=item}
            }
        }
        if(busy)LdText("Kaam ho raha hai...",12f,color=p.muted)
    }
    deleting?.let{item->AlertDialog(onDismissRequest={deleting=null},title={Text("Recording delete karein?")},text={Text("${item.fileName}\nYe original audio wapas nahi aayega. Pehle copy save kar sakte ho.")},confirmButton={TextButton(onClick={deleting=null;mutate({Library.delete(ctx,item)},"Recording delete hui")},modifier=Modifier.testTag("recording_delete_confirm")){Text("Delete")}},dismissButton={TextButton(onClick={deleting=null},modifier=Modifier.testTag("recording_delete_cancel")){Text("Rehne do")}})}
}
