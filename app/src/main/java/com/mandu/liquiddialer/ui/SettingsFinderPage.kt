package com.mandu.liquiddialer.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.SettingsFinder

@Composable fun SettingsFinderPage(onBack:()->Unit,onOpen:(String)->Unit){
 val p=LocalLd.current;var query by remember{mutableStateOf("")};val results=remember(query){SettingsFinder.search(query)}
 PageFrame("Settings finder",onBack){
  LdText("Control seedha dhoondo",20f,FontWeight.Bold,maxLines=4)
  LdText("Naam ya kaam likho, jaise speaker, record, SIM, swipe, font. Result kholne se setting change nahi hoti. Supported Settings rows par highlight milega; workspace results uska page kholenge.",12.5f,color=p.muted,maxLines=8)
  Spacer(Modifier.height(12.dp));LdField(query,{query=it.take(160)},"Setting ya feature khojo",modifier=Modifier.testTag("settings_find_query"))
  LdText("${results.size} results",12.5f,color=p.accent,modifier=Modifier.testTag("settings_find_count"))
  if(query.isNotBlank())GhostButton("Search saaf karo",Modifier.testTag("settings_find_clear")){query=""}
  if(results.isEmpty())GlassCard{LdText("Is naam ka control nahi mila",16f,FontWeight.Bold,maxLines=4);LdText("Chhota naam ya doosra word try karo. Har Android/carrier option is index mein nahi; Settings groups aur system settings alag available hain.",12.5f,color=p.muted,maxLines=8)}
  results.groupBy{it.group}.forEach{(g,items)->SecTitle(SETTINGS_GROUPS.firstOrNull{it.id==g}?.title?:g);GlassCard{items.forEachIndexed{i,e->Box(Modifier.testTag("settings_result_"+e.id)){SRow(e.title,e.detail.ifBlank{"Is control ka Settings section kholo"},settingsGroupIcon(e.group),last=i==items.lastIndex){onOpen(SettingsFinder.route(e))}}}}}
  Spacer(Modifier.height(16.dp));LdText("Yeh local names/descriptions ka index hai, internet search nahi. Setting khud tap karke badalni hogi. Android, permission aur carrier screens ke options phone par depend karte hain. Hidden feature page ko Settings se dobara visible kar sakte ho.",12f,color=p.muted,maxLines=10)
 }
}
