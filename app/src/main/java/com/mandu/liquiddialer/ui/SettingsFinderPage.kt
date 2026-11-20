package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.SettingsFinder
import com.mandu.liquiddialer.data.K
import com.mandu.liquiddialer.LdApp
import kotlinx.coroutines.launch

@Composable fun SettingsFinderPage(onBack:()->Unit,onOpen:(String)->Unit){
 val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();var error by remember{mutableStateOf(false)}
 val p=LocalLd.current;var query by remember{mutableStateOf("")};var group by remember{mutableStateOf("")}
 val activeGroup=if(s.finderGroupFilter)group else ""
 val results=remember(query,activeGroup){SettingsFinder.searchGroup(query,activeGroup)}
 LaunchedEffect(s.finderGroupFilter){if(!s.finderGroupFilter)group=""}
 PageFrame("Settings finder",onBack){
  LdText("Control seedha dhoondo",20f,FontWeight.Bold,maxLines=4)
  LdText("Naam ya kaam likho, jaise speaker, record, SIM, swipe, font. Result kholne se setting change nahi hoti. Supported Settings rows par highlight milega; workspace results uska page kholenge.",12.5f,color=p.muted,maxLines=8)
  Box(Modifier.testTag("finder_group_counts_switch")){SwitchRow("Finder group match counts","Default OFF. Count current-search matches in each group, not enabled features. Results and routes stay the same.",s.finderGroupCounts,last=true,subMaxLines=6){on->scope.launch{try{LdApp.instance.repo.set(K.finderGroupCounts,on);error=false}catch(_:Exception){error=true}}}}
  Box(Modifier.testTag("finder_full_labels_switch")){SwitchRow("Full finder result labels","Default OFF. Wrap complete indexed result titles and descriptions. Search results and routes stay the same.",s.finderFullLabels,last=true,subMaxLines=6){on->scope.launch{try{LdApp.instance.repo.set(K.finderFullLabels,on);error=false}catch(_:Exception){error=true}}}}
  Box(Modifier.testTag("finder_group_filter_switch")){SwitchRow("Finder group filter","Default OFF. Narrow results to one Settings group. Swipe the full-name buttons sideways; All groups restores every match. Turning OFF keeps your search and removes the group filter.",s.finderGroupFilter,last=true,subMaxLines=8){on->scope.launch{try{LdApp.instance.repo.set(K.finderGroupFilter,on);error=false}catch(_:Exception){error=true}}}}
  if(error)LdText("Setting save nahi hui",12.5f,color=p.danger,maxLines=3)
  Spacer(Modifier.height(12.dp));LdField(query,{query=it.take(160)},"Setting ya feature khojo",modifier=Modifier.testTag("settings_find_query"))
  if(s.finderGroupFilter){
   LdText("Group: "+(SETTINGS_GROUPS.firstOrNull{it.id==group}?.title?:"All groups"),13f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("settings_find_active_group"))
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("settings_find_groups"),horizontalArrangement=Arrangement.spacedBy(8.dp)){
    (listOf("" to "All groups")+SETTINGS_GROUPS.map{it.id to it.title}).forEach{(id,title)->
     Box(Modifier.testTag("settings_find_group_"+id.ifEmpty{"all"}).semantics{selected=group==id}.buttonGlass(p,RoundedCornerShape(22.dp)).clickable{group=id}.padding(horizontal=14.dp,vertical=12.dp),contentAlignment=Alignment.Center){LdText((if(group==id)"✓ "else"")+title,14f,FontWeight.SemiBold,p.accent,maxLines=1)}
    }
   }
   LdText("Swipe groups sideways. Group choice only filters these results, not enabled controls.",12f,color=p.muted,maxLines=5)
  }
  LdText("${results.size} results",12.5f,color=p.accent,modifier=Modifier.testTag("settings_find_count"))
  if(s.finderGroupFilter&&group.isNotEmpty())GhostButton("Show all groups",Modifier.testTag("settings_find_group_reset")){group=""}
  if(query.isNotBlank())GhostButton("Search saaf karo",Modifier.testTag("settings_find_clear")){query=""}
  if(results.isEmpty())GlassCard{LdText("Is naam ka control nahi mila",16f,FontWeight.Bold,maxLines=4);LdText((if(activeGroup.isNotEmpty())"Show all groups ya doosra group try karo. "else"")+"Chhota naam ya doosra word try karo. Har Android/carrier option is index mein nahi; Settings groups aur system settings alag available hain.",12.5f,color=p.muted,maxLines=8)}
  CompositionLocalProvider(LocalFullFinderRows provides s.finderFullLabels){results.groupBy{it.group}.forEach{(g,items)->SecTitle(SETTINGS_GROUPS.firstOrNull{it.id==g}?.title?:g);if(s.finderGroupCounts)LdText("Matching controls: ${items.size}",12.5f,color=p.muted,maxLines=3,modifier=Modifier.testTag("settings_find_group_count_"+g));GlassCard{items.forEachIndexed{i,e->Box(Modifier.testTag("settings_result_"+e.id)){SRow(e.title,e.detail.ifBlank{"Is control ka Settings section kholo"},settingsGroupIcon(e.group),last=i==items.lastIndex){onOpen(SettingsFinder.route(e))}}}}}}
  Spacer(Modifier.height(16.dp));LdText("Yeh local names/descriptions ka index hai, internet search nahi. Setting khud tap karke badalni hogi. Android, permission aur carrier screens ke options phone par depend karte hain. Hidden feature page ko Settings se dobara visible kar sakte ho.",12f,color=p.muted,maxLines=10)
 }
}
