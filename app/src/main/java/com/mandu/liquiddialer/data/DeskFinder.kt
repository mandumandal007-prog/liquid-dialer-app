package com.mandu.liquiddialer.data
import java.util.Locale

/** Searches static tool metadata, never notes, contacts, messages or call history. */
object DeskFinder {
 data class Tool(val id:String,val title:String,val group:String,val detail:String,val aliases:String)
 val groups=listOf("All","Prepare","Calculate","Reference")
 val all=listOf(
  Tool("checklist","Call checklist","Prepare","Manual discussion topics","topics pending discussed agenda baat सवाल"),
  Tool("estimate","Call calculator","Calculate","Manual quantities and rates","estimate quantity rate price hisab हिसाब"),
  Tool("timeline","Call timeline","Reference","Past calls for this number","history purani calls इतिहास"),
  Tool("brief","Call briefing","Prepare","Saved preparation snapshot","briefing taiyari preparation notes topics तैयारी"),
  Tool("tally","Quantity tally","Calculate","Session-only named counters","count counters plus minus pieces गिनती"),
  Tool("dates","Date calculator","Calculate","Manual calendar-day arithmetic","date days weekday calendar delivery तारीख"),
  Tool("converter","Unit converter","Calculate","Manual unit conversion","units kg gram litre feet temperature माप"),
  Tool("canvas","Call canvas","Prepare","Per-number drawing workspace","draw sketch pen ink map नक्शा"),
  Tool("drafts","Message drafts","Prepare","Saved text; compose remains explicit","sms whatsapp templates reply संदेश"),
  Tool("references","Reference cards","Reference","Saved facts with explicit reveal","reference address order facts पता"),
  Tool("reader","Saved notes reader","Reference","Read or search this number's notes","notes padho khojo read saved नोट्स")
 )
 fun available(s:LdSettings,number:String):List<Tool>{
  if(!s.deskFinder||!s.liveCallDesk||!FeatureControls.enabled(s,"notes"))return emptyList()
  return all.filter{t->when(t.id){
   "checklist","estimate","timeline","brief"->FeatureControls.enabled(s,t.id)&&runCatching{CallLists.number(number)}.isSuccess
   "tally"->s.callTally
   "dates"->s.callDates
   "converter"->s.callConverter
   "canvas"->s.callCanvas&&runCatching{CallCanvas.key(number)}.isSuccess
   "drafts"->s.messageDrafts&&runCatching{MessageDrafts.target(number)}.isSuccess
   "references"->s.callReferences&&runCatching{CallReferences.key(number)}.isSuccess
   "reader"->s.callNoteReader&&CallNoteReader.eligible(number)
   else->false
  }}
 }
 fun search(tools:List<Tool>,query:String,group:String="All"):List<Tool>{
  require(group in groups)
  val words=query.take(100).trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter{it.isNotBlank()}
  return tools.filter{t->(group=="All"||t.group==group)&&words.all{w->(t.title+" "+t.detail+" "+t.aliases).lowercase(Locale.ROOT).contains(w)}}
 }
}
