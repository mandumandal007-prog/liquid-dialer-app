package com.mandu.liquiddialer.ui
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mandu.liquiddialer.LdApp
import com.mandu.liquiddialer.data.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable fun CallReferencesPage(onBack:()->Unit){
 val s by LdApp.settings.collectAsState();val scope=rememberCoroutineScope();val p=LocalLd.current
 var number by remember{mutableStateOf("")};var opened by remember{mutableStateOf<String?>(null)};var error by remember{mutableStateOf("")}
 if(opened!=null){key(opened){CallReferencesWorkspace(opened!!,{opened=null})};return}
 PageFrame("Call reference cards",onBack){
  LdText("Zaroori baat, call par paas",20f,FontWeight.Bold,maxLines=3)
  LdText("Customer/order reference, address ya office hours jaise ordinary facts. Number ke saath local cards; value kholne ya copy karne ke liye alag tap.",13f,color=p.muted,maxLines=7)
  SwitchRow("Show in call Notes","Default OFF. Entry hide karne se saved cards delete nahi hote.",s.callReferences,last=true){scope.launch{try{LdApp.instance.repo.set(K.callReferences,it)}catch(_:Exception){error="Setting save nahi hui"}}}
  Column(Modifier.testTag("reference_options_settings")){
   SwitchRow("Review reference draft before save","Default OFF. Review ordinary title/value in a separate layout, then Save reviewed card. OFF returns to edit without saving.",s.referenceDraftReview,last=true,subMaxLines=7){scope.launch{try{LdApp.instance.repo.set(K.referenceDraftReview,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
   SwitchRow("Sectioned reference editor layout","Default OFF. Separate draft title/value panels with length counts. Save/Cancel stay explicit; layout changes do not save.",s.referenceEditorSections,last=true,subMaxLines=7){scope.launch{try{LdApp.instance.repo.set(K.referenceEditorSections,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
   SwitchRow("Fold reference view controls","Default OFF. Search/sort open on demand. Matched count, filter status and order stay visible; saved cards unchanged.",s.referenceViewFold,last=true,subMaxLines=7){scope.launch{try{LdApp.instance.repo.set(K.referenceViewFold,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
   SwitchRow("Sectioned reference card layout","Default OFF. Saved-position header, value panel and labeled actions. Glass stays; no saved data changes.",s.referenceSections,last=true,subMaxLines=6){scope.launch{try{LdApp.instance.repo.set(K.referenceSections,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
   SwitchRow("Sort reference titles","Default OFF. Saved order, Title A-Z or Z-A view; saved cards unchanged.",s.referenceSort,last=true,subMaxLines=5){scope.launch{try{LdApp.instance.repo.set(K.referenceSort,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
   SwitchRow("Fold card management layout","Default OFF. Edit and Delete open per card on demand. Reveal/Copy stay separate.",s.referenceManageFold,last=true,subMaxLines=5){scope.launch{try{LdApp.instance.repo.set(K.referenceManageFold,it);error=""}catch(_:Exception){error="Setting save nahi hui"}}}
  }
  LdText("Title sorting is case-insensitive text order, not a locale dictionary. Ties keep saved order. Search/sort/layout changes hide revealed values and fold management. No values searched or sorted. Sort OFF restores saved view; fold OFF restores Edit/Delete. No saved order/value changes from view options. Delete still needs confirmation. Closing resets the temporary view.",13f,color=p.muted,maxLines=19)
  LdText("PIN, OTP, password, card details ya secrets mat rakho. Plain local storage, encrypted vault nahi. Screen/clipboard dekhne wale value padh sakte hain. Appearance backup mein include nahi.",13f,color=p.danger,maxLines=9)
  SecTitle("Kis number ke cards?")
  LdField(number,{number=it.take(40)},"Full phone number",modifier=Modifier.testTag("ref_number_input"))
  PrimaryButton("Open reference cards",modifier=Modifier.testTag("ref_open")){runCatching{CallReferences.key(number)}.fold({opened=number.trim();error=""},{error="Sahi full number likho; country code ke saath behtar."})}
  if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=4,modifier=Modifier.testTag("ref_error"))
  LdText("Call ke Notes mein entry milegi jab option ON ho. Save/reveal/copy se koi call, SMS, tone ya network request nahi hoti.",12f,color=p.muted,maxLines=7)
 }
}
@Composable fun CallReferencesWorkspace(number:String,onBack:()->Unit,inSheet:Boolean=false){
 val ctx=LocalContext.current;val p=LocalLd.current;val settings by LdApp.settings.collectAsState()
 var snap by remember(number){mutableStateOf<CallReferences.Snapshot?>(null)};var loadFailed by remember(number){mutableStateOf(false)}
 var error by remember(number){mutableStateOf("")};var status by remember(number){mutableStateOf("")};var query by remember(number){mutableStateOf("")}
 var revealed by remember(number){mutableStateOf(setOf<String>())};var editing by remember(number){mutableStateOf<CallReferences.Card?>(null)};var adding by remember(number){mutableStateOf(false)}
 var title by remember(number){mutableStateOf("")};var value by remember(number){mutableStateOf("")};var removing by remember(number){mutableStateOf<CallReferences.Card?>(null)}
 var reviewing by remember(number){mutableStateOf(false)}
 var viewControls by remember(number){mutableStateOf(false)}
 var order by remember(number){mutableStateOf(ReferenceCardView.Order.SAVED)};var management by remember(number){mutableStateOf(setOf<String>())}
 LaunchedEffect(settings.referenceDraftReview){reviewing=false}
 LaunchedEffect(settings.referenceSort){if(!settings.referenceSort)order=ReferenceCardView.Order.SAVED;revealed=emptySet();management=emptySet()}
 LaunchedEffect(settings.referenceManageFold){revealed=emptySet();management=emptySet()}
 LaunchedEffect(settings.referenceSections){revealed=emptySet();management=emptySet()}
 LaunchedEffect(settings.referenceViewFold){viewControls=false;revealed=emptySet();management=emptySet()}
 fun reload(){runCatching{CallReferences.read(ctx,number)}.fold({snap=it;loadFailed=false;error=""},{snap=null;loadFailed=true;error="Saved cards load nahi hue. Data preserve hai; overwrite/delete nahi hoga."});revealed=emptySet();management=emptySet();editing=null;adding=false;removing=null;status="";viewControls=false;reviewing=false}
 LaunchedEffect(number){reload()}
 val owner=LocalLifecycleOwner.current
 DisposableEffect(owner,number){val obs=LifecycleEventObserver{_,e->if(e==Lifecycle.Event.ON_PAUSE)revealed=emptySet()};owner.lifecycle.addObserver(obs);onDispose{owner.lifecycle.removeObserver(obs)}}
 fun save(cards:List<CallReferences.Card>,done:String){val old=snap?:return;runCatching{CallReferences.save(ctx,number,old.raw,cards)}.fold({snap=it;adding=false;editing=null;removing=null;reviewing=false;revealed=emptySet();error="";status=done},{error="Save nahi hua. Title/value ki limit check karo; data doosri jagah badla ho to Reload karo. Draft yahin hai."})}
 val content:@Composable ColumnScope.()->Unit={
  if(inSheet){SheetTitle("Reference cards");GhostButton("Back to Notes",Modifier.testTag("ref_back"),onClick=onBack)}
  LdText(number,16f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("ref_identity"))
  LdText("Plain local facts, secrets nahi. Values hidden by default; Hide password protection nahi hai. Copy ki value system clipboard par rehti hai jab tak badle/clear ho.",12.5f,color=p.muted,maxLines=8)
  if(loadFailed)LdText("Load error · saved file nahi badli",13f,color=p.danger,maxLines=4,modifier=Modifier.testTag("ref_corrupt"))
  snap?.let{snapshot->
   if(adding||editing!=null){
    SecTitle(if(adding)"New card draft"else"Edit card draft")
    LdText("Draft save tak saved cards nahi badlenge. PIN/OTP/password yahan na likho.",12f,color=p.muted,maxLines=5)
    if(reviewing&&settings.referenceDraftReview){
     GlassCard(Modifier.padding(top=10.dp).testTag("reference_review_panel"),radius=20.dp,padding=14.dp){
      LdText("REVIEW · NOT SAVED",12f,FontWeight.Bold,color=p.accent,maxLines=4,modifier=Modifier.testTag("reference_review_notice"))
      LdText("Ordinary facts are visible here. Only Save reviewed card writes. Back to edit keeps the draft; Cancel discards it.",12f,color=p.muted,maxLines=8)
      Spacer(Modifier.height(12.dp))
      LdText("TITLE TO SAVE",11f,FontWeight.Bold,color=p.muted,maxLines=3)
      LdText(title.trim(),18f,FontWeight.Bold,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("reference_review_title"))
      Spacer(Modifier.height(12.dp))
      LdText("VALUE TO SAVE",11f,FontWeight.Bold,color=p.muted,maxLines=3)
      LdText(value.trim(),15f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("reference_review_value"))
     }
     PrimaryButton("Save reviewed card",modifier=Modifier.testTag("reference_review_save")){
      runCatching{CallReferences.Card(editing?.id?:UUID.randomUUID().toString(),title.trim(),value.trim()).also{it.validate()}}.fold({card->save(if(adding)snapshot.cards+card else snapshot.cards.map{if(it.id==card.id)card else it},"Card saved · value hidden")},{error="Title 1-40 aur value 1-500 letters rakho. Control characters nahi."})
     }
     GhostButton("Back to edit",Modifier.testTag("reference_review_back")){reviewing=false;error=""}
    }else{
    if(settings.referenceEditorSections){
     Column(Modifier.fillMaxWidth().background(p.accent.copy(alpha=.09f),RoundedCornerShape(16.dp)).padding(12.dp).testTag("reference_draft_status")){
      LdText("UNSAVED DRAFT",11f,FontWeight.Bold,color=p.accent,maxLines=3)
      LdText(if(settings.referenceDraftReview)"Review draft does not save. Save reviewed card writes; Cancel discards."else"Only Save card writes this draft. Cancel discards it.",12f,color=p.muted,maxLines=5)
     }
     GlassCard(Modifier.padding(top=10.dp).testTag("reference_draft_title_panel"),radius=18.dp,padding=12.dp){
      LdText("CARD TITLE",11f,FontWeight.Bold,color=p.accent,maxLines=3)
      LdField(title,{title=it.take(40)},"Title (max 40)",modifier=Modifier.testTag("ref_title"))
      LdText("${title.length}/40 title length",11f,color=p.muted,maxLines=3,modifier=Modifier.testTag("reference_draft_title_count"))
     }
     GlassCard(Modifier.padding(top=10.dp).testTag("reference_draft_value_panel"),radius=18.dp,padding=12.dp){
      LdText("ORDINARY REFERENCE VALUE",11f,FontWeight.Bold,color=p.accent,maxLines=4)
      LdField(value,{value=it.take(500)},"Ordinary reference value (max 500)",minLines=3,modifier=Modifier.testTag("ref_value_input"))
      LdText("${value.length}/500 value length",11f,color=p.muted,maxLines=3,modifier=Modifier.testTag("reference_draft_value_count"))
     }
    }else{
    LdField(title,{title=it.take(40)},"Title (max 40)",modifier=Modifier.testTag("ref_title"))
    LdField(value,{value=it.take(500)},"Ordinary reference value (max 500)",minLines=3,modifier=Modifier.testTag("ref_value_input"))
    LdText("${value.length}/500",11f,color=p.muted)
    }
    PrimaryButton(if(settings.referenceDraftReview)"Review draft"else"Save card",modifier=Modifier.testTag("ref_save")){
     runCatching{CallReferences.Card(editing?.id?:UUID.randomUUID().toString(),title.trim(),value.trim()).also{it.validate()}}.fold({card->if(settings.referenceDraftReview){reviewing=true;error=""}else save(if(adding)snapshot.cards+card else snapshot.cards.map{if(it.id==card.id)card else it},"Card saved · value hidden")},{error="Title 1-40 aur value 1-500 letters rakho. Control characters nahi."})
    }
    }
    GhostButton("Cancel draft",Modifier.testTag("ref_cancel")){adding=false;editing=null;reviewing=false;title="";value="";error=""}
   }else{
    val effectiveOrder=if(settings.referenceSort)order else ReferenceCardView.Order.SAVED
    val rows=ReferenceCardView.rows(snapshot.cards,query,effectiveOrder)
    if(settings.referenceViewFold){
     GlassCard(Modifier.padding(top=8.dp).testTag("reference_view_summary"),radius=18.dp,padding=12.dp){
      LdText("${rows.size} of ${snapshot.cards.size} saved cards shown",13f,FontWeight.Bold,maxLines=4,modifier=Modifier.testTag("reference_view_count"))
      LdText(if(query.trim().isEmpty())"Title filter: none" else "Title filter active",12f,color=p.muted,maxLines=4,modifier=Modifier.testTag("reference_view_filter"))
      LdText("View: ${effectiveOrder.label}",12f,color=p.muted,maxLines=4,modifier=Modifier.testTag("reference_view_order"))
      GhostButton(if(viewControls)"Hide search / sort controls"else"Search / sort controls",Modifier.testTag("reference_view_toggle")){viewControls=!viewControls;revealed=emptySet();management=emptySet();status=""}
     }
    }
    if(!settings.referenceViewFold||viewControls){
     LdField(query,{query=it.take(80);revealed=emptySet();management=emptySet()},"Search titles only",modifier=Modifier.testTag("ref_search"))
     LdText("${snapshot.cards.size}/${CallReferences.MAX_CARDS} saved cards · values search nahi hote",12f,color=p.muted,maxLines=4)
     if(settings.referenceSort){
      LdText("Title view only. Saved cards/order unchanged; values stay separate.",12f,color=p.muted,maxLines=5)
      ReferenceCardView.Order.entries.forEach{v->GhostButton((if(order==v)"✓ "else"")+v.label,Modifier.fillMaxWidth().testTag("reference_sort_${v.name.lowercase()}")){order=v;revealed=emptySet();management=emptySet();status=""}}
     }
    }
    if(rows.isEmpty())LdText(if(snapshot.cards.isEmpty())"Abhi is number ka card nahi."else"Is title ka card nahi mila.",13f,color=p.muted,maxLines=4,modifier=Modifier.testTag("ref_empty"))
    rows.forEach{c->GlassCard(Modifier.padding(top=12.dp).testTag("ref_card_${c.id}")){
     if(settings.referenceSections){
      Column(Modifier.fillMaxWidth().background(p.accent.copy(alpha=.09f),RoundedCornerShape(16.dp)).padding(12.dp).testTag("ref_section_header_${c.id}")){
       LdText("Saved card ${snapshot.cards.indexOfFirst{it.id==c.id}+1} of ${snapshot.cards.size}",11.5f,FontWeight.Bold,color=p.accent,maxLines=4,modifier=Modifier.testTag("ref_section_position_${c.id}"))
       Spacer(Modifier.height(6.dp))
       LdText(c.title,19f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
      }
      Spacer(Modifier.height(10.dp))
      Column(Modifier.fillMaxWidth().border(1.dp,p.muted.copy(alpha=.25f),RoundedCornerShape(14.dp)).padding(12.dp).testTag("ref_section_value_${c.id}")){
       LdText("REFERENCE VALUE",10.5f,FontWeight.Bold,color=p.muted,maxLines=3)
       Spacer(Modifier.height(6.dp))
       LdText(if(c.id in revealed)c.value else "Value hidden",15f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("ref_value_${c.id}"))
      }
      Spacer(Modifier.height(12.dp))
      LdText("CARD ACTIONS",10.5f,FontWeight.Bold,color=p.muted,maxLines=3,modifier=Modifier.testTag("ref_section_actions_${c.id}"))
     }else{
      LdText(c.title,17f,FontWeight.Bold,maxLines=Int.MAX_VALUE)
      LdText(if(c.id in revealed)c.value else "Value hidden",15f,maxLines=Int.MAX_VALUE,modifier=Modifier.testTag("ref_value_${c.id}"))
     }
     GhostButton(if(c.id in revealed)"Hide value"else"Reveal value",Modifier.testTag("ref_reveal_${c.id}")){revealed=if(c.id in revealed)revealed-c.id else revealed+c.id;status=""}
     if(c.id in revealed)GhostButton("Copy value",Modifier.testTag("ref_copy_${c.id}")){
      runCatching{val clip=ClipData.newPlainText("Reference value",c.value);clip.description.extras=android.os.PersistableBundle().apply{putBoolean("android.content.extra.IS_SENSITIVE",true)};(ctx.getSystemService(Context.CLIPBOARD_SERVICE)as ClipboardManager).setPrimaryClip(clip)}.fold({status="Value copied to system clipboard. Paste sirf jahan aap chaho."},{error="Copy nahi hua"})
     }
     if(settings.referenceManageFold)GhostButton(if(c.id in management)"Hide card management"else"Manage card",Modifier.testTag("ref_manage_${c.id}")){management=if(c.id in management)management-c.id else management+c.id;revealed=emptySet();status=""}
     if(!settings.referenceManageFold||c.id in management){
     GhostButton("Edit card",Modifier.testTag("ref_edit_${c.id}")){editing=c;title=c.title;value=c.value;revealed=emptySet();status="";error=""}
     GhostButton("Delete card",Modifier.testTag("ref_delete_${c.id}")){removing=c;revealed=emptySet();status=""}
     }
    }}
    if(snapshot.cards.size<CallReferences.MAX_CARDS)PrimaryButton("Add reference card",modifier=Modifier.testTag("ref_add")){adding=true;title="";value="";revealed=emptySet();status="";error=""}
   }
   removing?.let{c->GlassCard(Modifier.testTag("ref_delete_confirm")){
    LdText("Delete ${c.title}?",15f,FontWeight.Bold,maxLines=4)
    LdText("Sirf yeh saved card mitega. Clipboard ki copy clear nahi hogi.",12f,color=p.muted,maxLines=5)
    PrimaryButton("Yes, delete card",modifier=Modifier.testTag("ref_delete_yes")){save(snapshot.cards.filterNot{it.id==c.id},"Card deleted")}
    GhostButton("Cancel delete",Modifier.testTag("ref_delete_no")){removing=null}
   }}
  }
  if(error.isNotBlank())LdText(error,13f,color=p.danger,maxLines=7,modifier=Modifier.testTag("ref_error"))
  if(status.isNotBlank())LdText(status,12.5f,color=p.accent,maxLines=5,modifier=Modifier.testTag("ref_status"))
  GhostButton("Discard draft / Reload",Modifier.testTag("ref_reload")){reload()}
  LdText("100 numbers tak, har number par 12 cards. Local only; koi message, call, tone ya form auto-fill nahi. Backup/restore se alag.",12f,color=p.muted,maxLines=7)
 }
 if(inSheet)Column{content()}else PageFrame("Reference cards",onBack,content=content)
}
