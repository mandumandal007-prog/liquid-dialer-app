package com.mandu.liquiddialer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.FeatureControls
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Compact, labelled icon tiles. Entire tile remains a >=48dp accessible target. */
@Composable fun CallToolsRow(prefix:String,onChecklist:(()->Unit)?,onEstimate:(()->Unit)?,onTimeline:(()->Unit)?,onBriefing:(()->Unit)?=null){
 val s by LdApp.settings.collectAsState();val p=LocalLd.current
 data class Tool(val title:String,val description:String,val icon:ImageVector,val tag:String,val action:()->Unit)
 val tools=listOfNotNull(
  (onChecklist.takeIf{FeatureControls.enabled(s,"checklist")})?.let{Tool("Checklist","Call checklist · baat ke topics",LdIcons.Checklist,if(prefix=="call")"call_checklist_open"else"details_checklist",it)},
  (onEstimate.takeIf{FeatureControls.enabled(s,"estimate")})?.let{Tool("Calculator","Call calculator · quantity aur rate",LdIcons.Calculator,if(prefix=="call")"call_estimate_open"else"details_estimate",it)},
  (onTimeline.takeIf{FeatureControls.enabled(s,"timeline")})?.let{Tool("Timeline","Call timeline · purani calls",LdIcons.History,if(prefix=="call")"call_timeline_open"else"details_timeline",it)},
  (onBriefing.takeIf{FeatureControls.enabled(s,"brief")})?.let{Tool("Briefing","Call briefing · saved taiyari",LdIcons.Edit,if(prefix=="call")"call_brief_open"else"details_brief",it)})
 if(tools.isEmpty())return
 Column(Modifier.fillMaxWidth().padding(vertical=10.dp).testTag("${prefix}_tools"),verticalArrangement=Arrangement.spacedBy(10.dp)){
  LdText(if(tools.size>3)"CALL TOOLS · swipe"else "CALL TOOLS",10f,FontWeight.Bold,p.muted,letter=1f)
  Row(if(tools.size>3)Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   tools.forEach{tool->Column((if(tools.size>3)Modifier.width((80f*LocalDensity.current.fontScale.coerceAtLeast(1f)).dp)else Modifier.weight(1f)).heightIn(min=80.dp).buttonGlass(p,RoundedCornerShape(18.dp))
    .semantics{contentDescription=tool.description}.clickable(role=Role.Button,onClick=tool.action).testTag(tool.tag).padding(horizontal=6.dp,vertical=12.dp),
    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
     LdIcon(tool.icon,24.dp,p.accent)
     LdText(tool.title,11.5f,FontWeight.SemiBold,maxLines=Int.MAX_VALUE,textAlign=TextAlign.Center)
   }}
  }
 }
}
