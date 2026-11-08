package com.mandu.liquiddialer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mandu.liquiddialer.data.NoteFind

/** Read-only find tools. Uses the parent page's scroll, never another scroll container. */
@OptIn(ExperimentalFoundationApi::class)
@Composable fun NoteReader(text:String,query:String,matches:List<NoteFind.Match>,current:Int,jump:Int,onQuery:(String)->Unit) {
 val p=LocalLd.current
 var layout by remember(text){mutableStateOf<TextLayoutResult?>(null)}
 val bring=remember{BringIntoViewRequester()}
 LaunchedEffect(jump){if(jump>0){val m=matches.getOrNull(current);val l=layout
  if(m!=null && l!=null && m.start<l.layoutInput.text.length)bring.bringIntoView(l.getBoundingBox(m.start))}}
 GlassCard(Modifier.fillMaxWidth().padding(vertical=10.dp)) {
  OutlinedTextField(query,onQuery,label={Text("Is note mein khojo")},singleLine=true,
   modifier=Modifier.fillMaxWidth().testTag("note_find_query"))
  if(query.isNotEmpty()) LdText("Neeche Pichhla/Agla se match par jao. Khoj note ko badalti nahi.",11.5f,color=p.muted,maxLines=3,modifier=Modifier.padding(top=8.dp))
 }

 val marked=remember(text,matches,current,p){
  AnnotatedString.Builder(text).apply {matches.forEachIndexed {i,m->addStyle(SpanStyle(
   background=if(i==current)p.accent.pct(.5f) else p.accent.pct(.18f),
   color=p.text,fontWeight=if(i==current)FontWeight.Bold else FontWeight.Normal),m.start,m.end)}}.toAnnotatedString()
 }
 SelectionContainer {Text(marked,color=p.text,modifier=Modifier.fillMaxWidth().bringIntoViewRequester(bring).testTag("note_full_text"),onTextLayout={layout=it})}
}

/** Pinned by NoteDetailPage, outside the scrolling note. */
@Composable fun NoteFindFooter(count:Int,current:Int,onPrev:()->Unit,onNext:()->Unit,onClear:()->Unit){
 val p=LocalLd.current
 GlassCard(Modifier.fillMaxWidth()) {
  LdText(if(count==0)"Koi match nahi" else "${current+1} / $count matches",12f,FontWeight.SemiBold,
   modifier=Modifier.testTag("note_find_count"))
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   TextButton(onClick=onPrev,enabled=count>0,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("note_find_prev")){Text("Pichhla")}
   TextButton(onClick=onNext,enabled=count>0,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("note_find_next")){Text("Agla")}
   TextButton(onClick=onClear,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("note_find_clear")){Text("Saaf")}
  }
 }
}
