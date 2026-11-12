package com.mandu.liquiddialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Details-only flat surface. Other pages and the approved call glass stay unchanged. */
val LocalContactDetails=staticCompositionLocalOf{false}
@Composable fun ContactDetailsFrame(onBack:()->Unit,content:@Composable ColumnScope.()->Unit){
 val p=LocalLd.current
 BackHandler{onBack()}
 Column(Modifier.fillMaxSize().background(p.bg2).systemBarsPadding().imePadding().testTag("contact_details_frame")){
  Row(Modifier.fillMaxWidth().heightIn(min=56.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
   Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick=onBack).testTag("page_back"),contentAlignment=Alignment.Center){LdIcon(LdIcons.ArrowLeft,24.dp,p.text)}
   LdText("Number details",18f,FontWeight.Medium,modifier=Modifier.weight(1f).testTag("page_title"),maxLines=2)
  }
  Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start=16.dp,end=16.dp,top=4.dp,bottom=32.dp).testTag("page_content_scroll")){content()}
 }
}
