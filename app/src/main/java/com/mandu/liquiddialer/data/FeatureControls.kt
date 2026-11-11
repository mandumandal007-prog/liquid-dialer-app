package com.mandu.liquiddialer.data

/** Optional UI capabilities only. Hiding never deletes data or changes background policies. */
object FeatureControls {
 data class Feature(val id:String,val title:String,val group:String,val detail:String)
 val all=listOf(
  Feature("globalsearch","Global Search","Workspaces","Menu mein cross-section search"),
  Feature("calllists","Call Lists","Workspaces","Manual calling workspace aur dialer paste-list entry"),
  Feature("planner","Follow-up Planner","Workspaces","Follow-up planning page"),
  Feature("callbacks","Callbacks","Workspaces","Missed calls ka callback workspace"),
  Feature("reminders","Reminder Center","Workspaces","Reminder management page; scheduled alerts alag controls se band honge"),
  Feature("notes","Saved notes","Number tools","Notes page aur call-time Notes entry. Call-time number tools bhi Notes se khulte hain; number details par alag available. Saved text nahi mitega"),
  Feature("checklist","Call checklist","Number tools","Number details aur call-time checklist entry"),
  Feature("focus","Checklist Focus","Number tools","Checklist ka ek-ek topic view"),
  Feature("templates","Checklist templates","Number tools","Reusable checklist templates entry"),
  Feature("estimate","Call calculator","Number tools","Per-number quantity/rate calculator entry"),
  Feature("compare","What-if comparison","Number tools","Calculator ka compare view"),
  Feature("timeline","Number timeline","Number tools","Per-number history timeline entry"),
  Feature("brief","Call briefing","Number tools","Saved preparation snapshot entry"),
  Feature("historyexplorer","History Explorer","History & extras","History search workspace"),
  Feature("stats","Call statistics","History & extras","Hisab page aur Recents insights shortcut"),
  Feature("recordings","Recording library","History & extras","Saved recording library only; recording behavior alag hai"),
  Feature("visitingcard","Visiting card","History & extras","Business-card creator"),
  Feature("fakecall","Fake-call demo","History & extras","Simulated call page only"),
  Feature("radio","Radio information","History & extras","Dialer ka radio-info shortcut"),
  Feature("searchvoice","Search microphone","Home shortcuts","Search bar ka voice-search button; mic permission nahi badlegi"),
  Feature("videomini","Row video shortcuts","Home shortcuts","Recents/Contacts rows ke video-call buttons"),
  Feature("recentsclear","Clear-history shortcut","Home shortcuts","Recents ka clear button; system log nahi badlega"))
 val ids=all.map{it.id}.toSet()
 fun enabled(s:LdSettings,id:String)=id !in ids || id !in s.hiddenFeatures
 fun pageId(page:String):String? {val id=page.substringBefore(':');return id.takeIf{it in ids}}
 fun toggled(hidden:Set<String>,id:String,on:Boolean):Set<String>{require(id in ids);return (if(on)hidden-id else hidden+id).filter{it in ids}.toSet()}
}
