package com.mandu.liquiddialer.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.K
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.ldStore

internal data class SettingsDestination(val id:String,val title:String,val detail:String,val route:String,val category:String)
internal val ORGANIZED_DESTINATIONS=listOf(
 SettingsDestination("look","Theme & personal style","Profile, theme, language, icons and calling-card styles","settings:look","look"),
 SettingsDestination("recents","Home & Recents","History rows, filters, count, numbers and actions","settings:recents","look"),
 SettingsDestination("numbercard","Number details","Contact card and recent activity layouts","settings:numbercard","look"),
 SettingsDestination("navigation","Navigation & search","Tab gestures and search-bar position","settings:navigation","look"),
 SettingsDestination("keycontacts","Keypad & contacts","Letters, haptics, sorting and name format","settings:keycontacts","look"),
 SettingsDestination("callcontrols","Calling controls","SIM, confirmation, incoming behavior and system options","settings:callcontrols","phone"),
 SettingsDestination("identity","Caller identity","Previous call and contact labels","settings:identity","phone"),
 SettingsDestination("speed","Speed dial & voicemail","Shortcut numbers and voicemail","settings:speed","phone"),
 SettingsDestination("default","Default app & permissions","Phone role and permission access","settings:default","phone"),
 SettingsDestination("accessibility","Accessibility","TTY and hearing-aid system controls","settings:accessibility","phone"),
 SettingsDestination("privacy","Privacy & lock","App access gate and relock delay","settings:privacy","safety"),
 SettingsDestination("blocking","Blocking","Blocked numbers, exceptions and spam series","settings:blocking","safety"),
 SettingsDestination("quiet","Quiet hours","Silence windows and favourite exceptions","settings:quiet","safety"),
 SettingsDestination("sounds","Sounds & vibration","Ringtone, vibration and announcements","settings:sounds","alerts"),
 SettingsDestination("volume","Volume & boost","Ring, media and call volume","settings:volume","alerts"),
 SettingsDestination("notifications","Notifications","Missed call, birthday and summary alerts","settings:notifications","alerts"),
 SettingsDestination("live","Live call & incoming tools","Call desk, controls, incoming card and IVR","settingstools:live","tools"),
 SettingsDestination("notes","Notes, lists & references","Saved notes, checklists, drafts and reference cards","settingstools:notes","tools"),
 SettingsDestination("calculators","Calculators & counters","Dates, units, estimates, timing and tally tools","settingstools:calculators","tools"),
 SettingsDestination("featurecontrols","Feature visibility","Choose optional tools, pages and shortcuts","featurecontrols","tools"),
 SettingsDestination("recording","Recording","Recording options and saved recordings","settings:recording","storage"),
 SettingsDestination("backup","Backup & restore","Contacts and appearance export/import","settings:backup","storage"),
 SettingsDestination("about","About Liquid Dialer","App version and information","settings:about","storage")
)
internal data class SettingsCategory(val id:String,val title:String,val detail:String)
internal val ORGANIZED_CATEGORIES=listOf(
 SettingsCategory("look","Look & home","Theme, Recents, number cards, keypad and contacts"),
 SettingsCategory("phone","Phone & calling","SIM, incoming calls, permissions and accessibility"),
 SettingsCategory("safety","Privacy & protection","App lock, blocking and quiet hours"),
 SettingsCategory("alerts","Sound & alerts","Ringtone, vibration, volume and notifications"),
 SettingsCategory("tools","Call tools","Live controls, notes, lists, calculators and counters"),
 SettingsCategory("storage","Files & app","Recordings, backup, restore and version")
)
internal data class SettingsTool(val route:String,val title:String,val detail:String,val section:String)
internal val ORGANIZED_TOOLS=listOf(
    SettingsTool("weekdayshift","Selected-weekday date shift","Manual weekday choices and excluded dates","calculators"),
    SettingsTool("reviewoptions","Review priorities & row layout","Session priorities and optional slim glass rows","notes"),
    SettingsTool("briefingreview","Briefing review queue","Pending topics and Call List entries in one session view","notes"),
    SettingsTool("deskfinder","Desk tool finder","Search enabled desk tool names","live"),
    SettingsTool("desksections","Desk section layout","Order notes and optional desk entries","live"),
    SettingsTool("lapcompare","Stopwatch lap comparison","Compare selected measured laps, read only","calculators"),
    SettingsTool("deskstopwatch","Desk interval stopwatch","Start, pause and label task laps during a call","calculators"),
    SettingsTool("checkpointtasks","Checkpoint to checklist","Turn selected moments into unfinished topics","notes"),
    SettingsTool("livenotesrefresh","Live saved notes refresh","See local saves without closing the desk","notes"),
    SettingsTool("checkpointlabels","Checkpoint label editor","Mark now, name the moment later","notes"),
    SettingsTool("callcheckpoints","Call checkpoints","Manual marks, preview and explicit note summary","notes"),
    SettingsTool("deskkeypad","Live desk keypad","Manual tones without closing desk tools","live"),
    SettingsTool("livecalldesk","Live call desk","Call tools and live controls together","live"),
    SettingsTool("calltouchguard","Call touch guard","Manual on-screen control guard, End stays available","live"),
    SettingsTool("launchershortcuts","Icon long-press shortcuts","New contact and 3 chosen numbers","live"),
    SettingsTool("callnotereader","Call notes reader","Read full saved notes during a call","notes"),
    SettingsTool("calltally","Call quantity tally","Session-only named counters","calculators"),
    SettingsTool("incomingstudio","Incoming card try-out and profiles","Safe rehearsal and saved incoming-only choices","live"),
    SettingsTool("incomingcard","Incoming call card controls","Incoming-only look and Answer/Cut controls","live"),
    SettingsTool("checklistduplicates","Checklist duplicate review","Review selected repeated topics","notes"),
    SettingsTool("tallyboards","Saved tally boards","Save quantities and preview session replacement","calculators"),
    SettingsTool("estimatebundles","Calculator item bundles","Save and reuse selected calculator rows","calculators"),
    SettingsTool("estimatelines","Calculator multi-line items","Preview manual item lines before appending","calculators"),
    SettingsTool("estimaterates","Calculator rate adjustment","Preview selected rate changes","calculators"),
    SettingsTool("estimatescale","Calculator quantity scaling","Preview selected quantity ratio changes","calculators"),
    SettingsTool("installmentpreview","Calculator installment preview","Manual dated parts, no payment or reminder","calculators"),
    SettingsTool("estimatesplit","Calculator split estimate","Preview equal or weighted shares","calculators"),
    SettingsTool("calldates","Call date calculator","Offline date arithmetic","calculators"),
    SettingsTool("converterhistory","Converter history controls","Reuse original inputs and fold history panel","calculators"),
    SettingsTool("converteroptions","Converter table & chooser layout","All-unit comparison and optional folded choosers","calculators"),
    SettingsTool("callconverter","Call unit converter","Offline measurement conversion","calculators"),
    SettingsTool("checklistorganizer","Checklist organizer","Batch marks and topic order","notes"),
    SettingsTool("callcanvas","Call canvas","Finger sketch, saved separately for each number","notes"),
    SettingsTool("messagedrafts","Message drafts","Reusable local text, explicit preview and compose","notes"),
    SettingsTool("callreferences","Call reference cards","Per-number facts, explicit reveal and copy","notes"),
    SettingsTool("callpace","Call pace reminders","Opt-in countdown aur active-stretch reminder","live"),
    SettingsTool("ivrmenus","IVR menu helper","Saved menu choices, one digit per explicit tap","live"),
)
internal fun searchSettingsDestinations(query:String):List<SettingsDestination>{
 val words=query.trim().lowercase(java.util.Locale.ROOT).split(Regex("\\s+")).filter{it.isNotEmpty()}
 if(words.isEmpty())return ORGANIZED_DESTINATIONS
 return ORGANIZED_DESTINATIONS.filter{d->
  val c=ORGANIZED_CATEGORIES.first{it.id==d.category}
  val text=(d.title+" "+d.detail+" "+c.title).lowercase(java.util.Locale.ROOT)
  words.all{text.contains(it)}
 }
}
private fun organizedIcon(id:String)=when(id){
 "look"->LdIcons.Palette;"phone","callcontrols","live"->LdIcons.Phone;"safety"->LdIcons.Block
 "alerts"->LdIcons.Volume;"tools","notes"->LdIcons.Checklist;"storage"->LdIcons.Record
 "recents"->LdIcons.Clock;"numbercard"->LdIcons.User;"navigation"->LdIcons.Search
 "keycontacts","calculators"->LdIcons.Dialpad;else->settingsGroupIcon(id)
}
@Composable
internal fun OrganizedSettingsSwitch(){
 val s by LdApp.settings.collectAsState()
 val scope=rememberCoroutineScope()
 GlassCard{Box(Modifier.testTag("organized_settings_switch")){SwitchRow("Organized Settings layout","ON shows clear categories and smaller pages. OFF restores the original Settings list. Existing values stay the same.",s.organizedSettings,last=true,subMaxLines=6){scope.launch{LdApp.instance.repo.set(K.organizedSettings,it)}}}}
}
@Composable
internal fun OrganizedSettingsHome(onBack:()->Unit,onOpen:(String)->Unit){
 val p=LocalLd.current
 val s by LdApp.settings.collectAsState()
 val scope=rememberCoroutineScope()
 var query by remember{mutableStateOf("")}
 val searching=s.settingsCategorySearch&&query.isNotBlank()
 val results=remember(query){searchSettingsDestinations(query)}
 PageFrame("Settings",onBack){
  Column(Modifier.fillMaxWidth().glass(p,RoundedCornerShape(24.dp),0.dp).padding(16.dp).testTag("organized_settings_intro")){
   LdText("Settings by category",17f,FontWeight.Bold,maxLines=3)
   LdText("Find a control without the long mixed lists.",12.5f,color=p.muted,maxLines=4,modifier=Modifier.padding(top=6.dp))
  }
  Spacer(Modifier.height(10.dp))
  GlassCard{SRow("Find a setting","Search all controls, including the original direct routes",LdIcons.Search,last=true){onOpen("settingsfinder")}}
  Spacer(Modifier.height(10.dp))
  if(s.settingsCategorySearch){
   GlassCard{
    LdText("Find a category page",16f,FontWeight.Bold,maxLines=3)
    LdText("Search category pages here. For individual controls, use Find a setting above.",12f,color=p.muted,maxLines=5)
    Spacer(Modifier.height(8.dp))
    LdField(query,{query=it.take(100)},"Try keypad, volume or number details",modifier=Modifier.testTag("settings_category_query"))
    if(searching){
     LdText("Category pages: ${results.size}",12.5f,color=p.accent,modifier=Modifier.testTag("settings_category_count"))
     GhostButton("Clear category search",Modifier.testTag("settings_category_clear")){query=""}
    }
   }
   Spacer(Modifier.height(10.dp))
  }
  if(!searching){GlassCard{ORGANIZED_CATEGORIES.forEachIndexed{i,c->
   Box(Modifier.testTag("settings_category_"+c.id)){SRow(c.title,if(s.compactSettingsCategories)null else c.detail,organizedIcon(c.id),last=i==ORGANIZED_CATEGORIES.lastIndex,subMaxLines=if(s.settingsCategoryCounts)8 else 4,trailing={
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
     if(s.settingsCategoryCounts)LdText("${ORGANIZED_DESTINATIONS.count{it.category==c.id}} pages",11f,FontWeight.SemiBold,color=p.accent,maxLines=1,modifier=Modifier.testTag("settings_category_count_"+c.id))
     LdIcon(LdIcons.Chevron,16.dp,p.muted.copy(alpha=.5f))
    }
   }){onOpen("settingshub:"+c.id)}}
  }}}else{
   if(results.isEmpty())GlassCard{LdText("No category page found",16f,FontWeight.Bold,maxLines=3);LdText("Try another word, clear this search, or use Find a setting for individual controls.",12.5f,color=p.muted,maxLines=5)}
   results.groupBy{it.category}.forEach{(id,items)->
    SecTitle(ORGANIZED_CATEGORIES.first{it.id==id}.title)
    GlassCard{items.forEachIndexed{i,d->Box(Modifier.testTag("settings_category_result_"+d.id)){SRow(d.title,d.detail,organizedIcon(d.id),last=i==items.lastIndex,subMaxLines=5){onOpen(d.route)}}}}
   }
  }
  SecTitle("Layout choice")
  OrganizedSettingsSwitch()
  GlassCard{Box(Modifier.testTag("settings_category_counts_switch")){SwitchRow("Settings category page counts","Default OFF. Show how many destination pages each category contains, not controls or enabled features.",s.settingsCategoryCounts,last=true,subMaxLines=6){scope.launch{LdApp.instance.repo.set(K.settingsCategoryCounts,it)}}}}
  GlassCard{Box(Modifier.testTag("compact_settings_categories_switch")){SwitchRow("Compact Settings category rows","Default OFF. Show titles without descriptions for category choices. Search results and individual controls stay the same.",s.compactSettingsCategories,last=true,subMaxLines=6){scope.launch{LdApp.instance.repo.set(K.compactSettingsCategories,it)}}}}
  GlassCard{Box(Modifier.testTag("settings_category_search_switch")){SwitchRow("Settings category-page search","Default OFF. Add search for category pages to this organized home. Individual controls stay in Find a setting.",s.settingsCategorySearch,last=true,subMaxLines=6){scope.launch{LdApp.instance.repo.set(K.settingsCategorySearch,it)}}}}
  GlassCard{SRow("Original category list","Open the classic list without changing this layout",LdIcons.Info,last=true){onOpen("settingshub:classic")}}
 }
}
internal fun recentSettingsTool(route:String):SettingsTool?=ORGANIZED_TOOLS.firstOrNull{it.route==route}
@Composable
private fun settingsToolOpener(onOpen:(String)->Unit):(String)->Unit{
 val s by LdApp.settings.collectAsState()
 val scope=rememberCoroutineScope()
 return {route->
  if(s.recentSettingsTool&&recentSettingsTool(route)!=null)scope.launch{
   LdApp.instance.ldStore.edit{prefs->if(prefs[K.recentSettingsTool]==true)prefs[K.lastSettingsTool]=route}
   onOpen(route)
  }else onOpen(route)
 }
}

@Composable
internal fun OrganizedCategoryPage(id:String,onBack:()->Unit,onOpen:(String)->Unit){
 if(id=="classic"){
  PageFrame("Original categories",onBack){
   GlassCard{SETTINGS_GROUPS.forEachIndexed{i,g->SRow(g.title,g.summary,settingsGroupIcon(g.id),last=i==SETTINGS_GROUPS.lastIndex){onOpen("settings:"+g.id)}}}
  };return
 }
 val c=ORGANIZED_CATEGORIES.firstOrNull{it.id==id}
 val s by LdApp.settings.collectAsState()
 val scope=rememberCoroutineScope()
 var query by remember(id){mutableStateOf("")}
 val searching=id=="tools"&&s.settingsToolSearch&&query.isNotBlank()
 val results=remember(query){searchSettingsTools(query)}
 val openTool=settingsToolOpener(onOpen)
 PageFrame(c?.title?:"Settings categories",onBack){
  LdText(c?.detail?:"Choose a category from Settings",13f,color=LocalLd.current.muted,maxLines=4,modifier=Modifier.padding(bottom=12.dp))
  if(id=="tools"&&s.recentSettingsTool){
   GlassCard{
    LdText("Recent tool controls",16f,FontWeight.Bold,maxLines=3)
    val last=recentSettingsTool(s.lastSettingsTool)
    if(last!=null){
     CompositionLocalProvider(LocalSettingsCategoryRows provides true){Box(Modifier.testTag("recent_settings_tool_open")){SRow(last.title,"Return to controls only. Nothing starts automatically.",organizedIcon(last.section),last=true,subMaxLines=5){openTool(last.route)}}}
    }else LdText("Open a tool from these sections or their search results to keep a return shortcut here. No phone number or tool content is saved.",12.5f,color=LocalLd.current.muted,maxLines=7,modifier=Modifier.testTag("recent_settings_tool_empty"))
    if(s.lastSettingsTool.isNotEmpty())GhostButton("Clear recent shortcut",Modifier.testTag("recent_settings_tool_clear")){scope.launch{LdApp.instance.repo.set(K.lastSettingsTool,"")}}
   }
   Spacer(Modifier.height(10.dp))
  }
  if(id=="tools"&&s.settingsToolSearch){
   GlassCard{
    LdText("Find a call tool",16f,FontWeight.Bold,maxLines=3)
    LdText("Search the 37 tools in these sections. Opening a result does not start it.",12f,color=LocalLd.current.muted,maxLines=5)
    Spacer(Modifier.height(8.dp))
    LdField(query,{query=it.take(100)},"Try notes, IVR or calculator",modifier=Modifier.testTag("settings_tool_query"))
    if(searching){LdText("Tools: ${results.size}",12.5f,color=LocalLd.current.accent,modifier=Modifier.testTag("settings_tool_count"));GhostButton("Clear tool search",Modifier.testTag("settings_tool_clear")){query=""}}
   }
   Spacer(Modifier.height(10.dp))
  }
  if(!searching){
   val items=ORGANIZED_DESTINATIONS.filter{it.category==id}
   GlassCard{items.forEachIndexed{i,d->
    val toolCount=if(s.settingsToolCounts&&d.route.startsWith("settingstools:"))ORGANIZED_TOOLS.count{it.section==d.id}else null
    val detail=if(toolCount!=null)"$toolCount tools"+(if(s.compactSettingsCategories)"" else "\n"+d.detail)else if(s.compactSettingsCategories)null else d.detail
    CompositionLocalProvider(LocalSettingsCategoryRows provides (toolCount!=null||LocalSettingsCategoryRows.current)){
    Box(Modifier.testTag("settings_destination_"+d.id)){SRow(d.title,detail,organizedIcon(d.id),last=i==items.lastIndex,subMaxLines=if(toolCount!=null)8 else 5){onOpen(d.route)}}
    }
   }}
  }else{
   if(results.isEmpty())GlassCard{LdText("No call tool found",16f,FontWeight.Bold,maxLines=3);LdText("Try another name or clear search to browse all sections. Individual Settings controls stay in Find a setting.",12.5f,color=LocalLd.current.muted,maxLines=6)}
   results.groupBy{it.section}.forEach{(section,items)->
    LdText(ORGANIZED_DESTINATIONS.first{it.route=="settingstools:$section"}.title,13f,FontWeight.Bold,color=LocalLd.current.muted,maxLines=4,modifier=Modifier.padding(start=6.dp,end=6.dp,top=22.dp,bottom=10.dp))
    GlassCard{items.forEachIndexed{i,t->Box(Modifier.testTag("settings_tool_result_"+t.route)){SRow(t.title,t.detail,organizedIcon(section),last=i==items.lastIndex,subMaxLines=5){openTool(t.route)}}}}
   }
  }
  if(id=="tools"){
   SecTitle("Return shortcut")
   GlassCard{Box(Modifier.testTag("recent_settings_tool_switch")){SwitchRow("Recent call-tool shortcut","Default OFF. While ON, remember the last tool controls opened from these lists or search. OFF hides the shortcut and stops recording. Clear removes it; no number or tool content is stored.",s.recentSettingsTool,last=true,subMaxLines=9){scope.launch{LdApp.instance.repo.set(K.recentSettingsTool,it)}}}}
   SecTitle("Tool list layout")
   GlassCard{Box(Modifier.testTag("settings_tool_counts_switch")){SwitchRow("Call-tool section counts","Default OFF. Show the number of tool choices in each section, not enabled or running tools. Category page counts stay separate.",s.settingsToolCounts,last=true,subMaxLines=7){scope.launch{LdApp.instance.repo.set(K.settingsToolCounts,it)}}}}
   GlassCard{Box(Modifier.testTag("alphabetical_settings_tools_switch")){SwitchRow("Call tools A-Z order","Default OFF. Sort choices by title inside each of the three tool sections. OFF restores the original order. Search results stay unchanged.",s.alphabeticalSettingsTools,last=true,subMaxLines=7){scope.launch{LdApp.instance.repo.set(K.alphabeticalSettingsTools,it)}}}}
   GlassCard{Box(Modifier.testTag("compact_settings_tools_switch")){SwitchRow("Compact call-tool rows","Default OFF. Hide descriptions only inside the three tool-section lists. Search results and each tool's own controls stay the same.",s.compactSettingsTools,last=true,subMaxLines=7){scope.launch{LdApp.instance.repo.set(K.compactSettingsTools,it)}}}}
   SecTitle("Search choice")
   GlassCard{Box(Modifier.testTag("settings_section_search_switch")){SwitchRow("Search inside call-tool sections","Default OFF. Add a name and description filter inside each tool section. The search stays within that section; tool controls do not change.",s.settingsSectionSearch,last=true,subMaxLines=7){scope.launch{LdApp.instance.repo.set(K.settingsSectionSearch,it)}}}}
   GlassCard{Box(Modifier.testTag("settings_tool_search_switch")){SwitchRow("Call tools search","Default OFF. Search tool names and descriptions in this category. OFF keeps the original section list.",s.settingsToolSearch,last=true,subMaxLines=6){scope.launch{LdApp.instance.repo.set(K.settingsToolSearch,it)}}}}
  }
 }
}
internal fun searchSettingsTools(query:String):List<SettingsTool>{
 val words=query.trim().lowercase(java.util.Locale.ROOT).split(Regex("\\s+")).filter{it.isNotEmpty()}
 if(words.isEmpty())return ORGANIZED_TOOLS
 return ORGANIZED_TOOLS.filter{t->val text=(t.title+" "+t.detail).lowercase(java.util.Locale.ROOT);words.all{text.contains(it)}}
}

internal fun settingsSectionTools(id:String,alphabetical:Boolean,query:String=""):List<SettingsTool>{
 val words=query.trim().lowercase(java.util.Locale.ROOT).split(Regex("\\s+")).filter{it.isNotEmpty()}
 val tools=ORGANIZED_TOOLS.filter{t->t.section==id&&words.all{word->(t.title+" "+t.detail).lowercase(java.util.Locale.ROOT).contains(word)}}
 return if(alphabetical)tools.sortedBy{it.title.lowercase(java.util.Locale.ROOT)}else tools
}

@Composable
internal fun OrganizedToolsPage(id:String,onBack:()->Unit,onOpen:(String)->Unit){
 val s by LdApp.settings.collectAsState()
 var query by remember(id,s.settingsSectionSearch){mutableStateOf("")}
 val openTool=settingsToolOpener(onOpen)
 val d=ORGANIZED_DESTINATIONS.firstOrNull{it.route=="settingstools:$id"}
 PageFrame(d?.title?:"Call tools",onBack){
  LdText("Open a tool to adjust its own controls. Nothing starts from this list.",13f,color=LocalLd.current.muted,maxLines=4,modifier=Modifier.padding(bottom=12.dp))
  val tools=remember(id,s.alphabeticalSettingsTools,query,s.settingsSectionSearch){settingsSectionTools(id,s.alphabeticalSettingsTools,if(s.settingsSectionSearch)query else "")}
  if(s.settingsSectionSearch){
   GlassCard{
    LdText("Find in this section",16f,FontWeight.Bold,maxLines=3)
    LdText("Search tool names and descriptions here. Opening a tool does not start it.",12f,color=LocalLd.current.muted,maxLines=5)
    Spacer(Modifier.height(8.dp))
    LdField(query,{query=it.take(100)},"Tool name or description",modifier=Modifier.testTag("settings_section_query"))
    if(query.isNotBlank()){
     LdText("Tools: ${tools.size}",12.5f,color=LocalLd.current.accent,modifier=Modifier.testTag("settings_section_count"))
     GhostButton("Clear section search",Modifier.testTag("settings_section_clear")){query=""}
    }
   }
   Spacer(Modifier.height(10.dp))
   if(tools.isEmpty())GlassCard{LdText("No tool found in this section",16f,FontWeight.Bold,maxLines=4);LdText("Try another word or clear this search to see all tools in this section.",12.5f,color=LocalLd.current.muted,maxLines=6)}
  }
  if(s.alphabeticalSettingsTools)LdText("A-Z order",12f,FontWeight.SemiBold,color=LocalLd.current.accent,maxLines=2,modifier=Modifier.testTag("settings_tools_az_label").padding(bottom=8.dp))
  CompositionLocalProvider(LocalSettingsCategoryRows provides (s.compactSettingsTools||LocalSettingsCategoryRows.current)){
  if(tools.isNotEmpty())GlassCard{tools.forEachIndexed{i,t->Box(Modifier.testTag("settings_tool_"+t.route)){SRow(t.title,if(s.compactSettingsTools)null else t.detail,LdIcons.Edit,last=i==tools.lastIndex,subMaxLines=4){openTool(t.route)}}}}
  }
 }
}
