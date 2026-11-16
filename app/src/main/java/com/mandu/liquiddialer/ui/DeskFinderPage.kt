package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch

@Composable fun DeskFinderPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val ctx=LocalContext.current;val p=LocalLd.current;val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 PageFrame("Desk tool finder",onBack){
  LdText("Kaam ka tool, naam se.",22f,FontWeight.Bold,maxLines=4)
  LdText("Live call desk mein enabled tools ke naam khojo ya Prepare / Calculate / Reference category chuno. Long button stack ki jagah matching tools dikhte hain. Notes ka draft aur fixed call controls wahi rehte hain.",14f,color=p.muted,maxLines=12)
  GlassCard{SwitchRow("Desk tool finder","Default OFF. Live call desk mein launcher names only.",s.deskFinder,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.deskFinder]=v};error=false}catch(_:Exception){error=true}}}}
  GlassCard{
   SwitchRow("Quick-tool shelf","Default OFF. Pin up to4 enabled tool names in this desk session.",s.deskShelf){v->scope.launch{try{ctx.ldStore.edit{it[K.deskShelf]=v};error=false}catch(_:Exception){error=true}}}
   SwitchRow("Compact finder rows","Default OFF. One wrapped Open title; details on demand.",s.deskFinderCompact,last=true){v->scope.launch{try{ctx.ldStore.edit{it[K.deskFinderCompact]=v};error=false}catch(_:Exception){error=true}}}
  }
  LdText("Shelf ke liye Desk tool finder aur Live call desk bhi ON hon. Search result par Pin tap karo. Up to4 names, no automatic replacement when full. Shelf search/category se alag rehti hai. Open only launches the tool. Unpin or Clear shelf removes shortcuts only. Tool OFF/hidden removes its pin; returning ON does not restore it. Closing finder keeps pins; closing desk, call change or app restart clears them. Shelf OFF clears pins, no saved data changes.",13f,color=p.muted,maxLines=22)
  LdText("Compact rows keep the full tool title. Details opens its category/help, not saved data. OFF restores original cards. Layout change does not clear query, shelf or note draft. Glass style and fixed call controls stay unchanged.",13f,color=p.muted,maxLines=12)
  SecTitle("Find, then open")
  GlassCard{LdText("Calculator, checklist, timeline, briefing aur enabled tally, dates, converter, canvas, message drafts, reference cards aur notes reader milte hain. Hidden/OFF tools nahi dikhte. Yeh setting unhe ON nahi karti. Matching result dabane se sirf existing tool khulta hai; uske Save, discard, reveal, clipboard ya compose steps alag rehte hain.",13f,maxLines=18)}
  SecTitle("Not a private-data search")
  LdText("Query sirf built-in tool names, short help aur search aliases se match hoti hai. Notes, contacts, history, saved card contents ya messages search nahi hote. Saved notes reader kholne par uski apni search alag hai. Query temporary hai: tool close/return ya finder OFF par reset ho sakti hai.",13f,color=p.muted,maxLines=16)
  GlassCard{LdText("Desk only. Normal calling-card Notes aur number-details launchers unchanged. Stopwatch/checkpoints aur keypad apni desk entries par rehte hain. OFF se original launcher stack wapas, note draft stays. Call change/desk close par unsaved tool rules unchanged. No network, recording, new permission or automatic call/message/clipboard/storage action.",13f,maxLines=18)}
  if(error)LdText("Setting save nahi hui",13f,color=p.danger)
 }
}
@Composable fun DeskFinderWorkspace(number:String,pins:List<String>,onPins:(List<String>)->Unit,onOpen:(String)->Unit){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current;val focus=LocalFocusManager.current
 var open by remember(number){mutableStateOf(false)}
 var query by remember(number){mutableStateOf("")};var group by remember(number){mutableStateOf("All")}
 var details by remember(number){mutableStateOf(emptySet<String>())}
 LaunchedEffect(s.deskFinderCompact){details=emptySet()}
 if(!s.deskFinder)return
 val available=DeskFinder.available(s,number)
 val shelf=if(s.deskShelf)DeskShelf.visible(pins,available)else emptyList()
 fun launch(id:String){focus.clearFocus();if(DeskFinder.available(LdApp.settings.value,number).any{it.id==id})onOpen(id)}
 if(s.deskShelf)Column(Modifier.fillMaxWidth().padding(vertical=8.dp).testTag("desk_shelf"),verticalArrangement=Arrangement.spacedBy(8.dp)){
  LdText("Quick-tool shelf: ${shelf.size} / 4",14f,FontWeight.Bold,maxLines=3,modifier=Modifier.testTag("desk_shelf_count"))
  LdText(if(shelf.isEmpty())"Pin a result below. Session only, names only."else"Session only. Search/category does not hide these pins.",12f,color=p.muted,maxLines=5)
  shelf.forEach{id->val tool=available.first{it.id==id};GlassCard{
   FinderOpenButton("Open ${tool.title}",Modifier.testTag("desk_shelf_open_$id")){launch(id)}
   Spacer(Modifier.height(8.dp))
   GhostButton("Unpin",Modifier.testTag("desk_shelf_unpin_$id")){onPins(pins-id)}
  }}
  if(shelf.isNotEmpty())GhostButton("Clear shelf",Modifier.testTag("desk_shelf_clear")){onPins(emptyList())}
 }
 GhostButton(if(open)"Hide tool finder"else"Find a call tool",Modifier.testTag("desk_finder_toggle")){open=!open;if(!open){query="";group="All"};focus.clearFocus()}
 if(!open)return
 val results=DeskFinder.search(available,query,group)
 Column(Modifier.fillMaxWidth().padding(vertical=10.dp).testTag("desk_finder"),verticalArrangement=Arrangement.spacedBy(8.dp)){
  LdText("FIND A CALL TOOL",12f,FontWeight.Bold,p.accent,maxLines=3)
  LdText("Tool names only, not your saved data.",12f,color=p.muted,maxLines=4)
  LdField(query,{query=it.take(100)},"Search tools: calculator, notes, kg...",modifier=Modifier.testTag("desk_finder_query"))
  DeskFinder.groups.chunked(2).forEach{pair->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){pair.forEach{g->
   PrimaryButton(if(g==group)"$g ✓"else g,modifier=Modifier.weight(1f).testTag("desk_finder_group_${g.lowercase()}")){group=g;focus.clearFocus()}
  }}}
  GhostButton("Clear search and category",Modifier.testTag("desk_finder_clear")){query="";group="All";focus.clearFocus()}
  LdText("${results.size} matching / ${available.size} available tools",12f,color=p.muted,maxLines=4,modifier=Modifier.testTag("desk_finder_count"))
  if(results.isEmpty())LdText(if(available.isEmpty())"No tools available. Check their own settings."else"No matching tool. Clear search or choose All.",13f,color=p.muted,maxLines=5,modifier=Modifier.testTag("desk_finder_empty"))
  results.forEach{tool->GlassCard(Modifier.testTag("desk_finder_row_${tool.id}")){
   if(!s.deskFinderCompact)LdText(tool.title,16f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
   if(!s.deskFinderCompact||tool.id in details)LdText("${tool.group} · ${tool.detail}",12f,color=p.muted,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("desk_finder_detail_${tool.id}"))
   if(s.deskFinderCompact)FinderOpenButton("Open ${tool.title}",Modifier.testTag("desk_finder_open_${tool.id}")){launch(tool.id)}
   else PrimaryButton("Open ${tool.title}",modifier=Modifier.testTag("desk_finder_open_${tool.id}")){launch(tool.id)}
   Spacer(Modifier.height(8.dp))
   if(s.deskFinderCompact)GhostButton(if(tool.id in details)"Hide details"else"Details",Modifier.testTag("desk_finder_details_${tool.id}")){details=if(tool.id in details)details-tool.id else details+tool.id}
   if(s.deskShelf)Spacer(Modifier.height(8.dp))
   if(s.deskShelf)GhostButton(if(tool.id in shelf)"Unpin"else if(shelf.size>=DeskShelf.MAX)"Shelf full (4)"else"Pin to shelf",Modifier.testTag("desk_finder_pin_${tool.id}")){
    val now=LdApp.settings.value
    if(now.deskShelf)onPins(DeskShelf.toggle(pins,tool.id,DeskFinder.available(now,number)))
   }
  }}
 }
}

@Composable private fun FinderOpenButton(text:String,modifier:Modifier=Modifier,onClick:()->Unit){
 val p=LocalLd.current
 Box(modifier.fillMaxWidth().buttonGlass(p,RoundedCornerShape(16.dp),p.accent).clickable(onClick=onClick).padding(horizontal=12.dp,vertical=14.dp),contentAlignment=Alignment.Center){LdText(text,14.5f,FontWeight.Bold,maxLines=Int.MAX_VALUE)}
}
