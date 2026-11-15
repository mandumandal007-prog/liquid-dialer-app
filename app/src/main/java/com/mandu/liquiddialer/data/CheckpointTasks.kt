package com.mandu.liquiddialer.data
import android.content.Context
import com.mandu.liquiddialer.LdApp

/** User-selected text only. No automatic task extraction, notes or Telecom command. */
object CheckpointTasks {
 data class Preview(val board:CallCheckpoints.Board,val selected:Set<Int>,val existing:List<CallChecklist.Item>,val topics:List<String>,val skipped:Int)
 fun enabled(s:LdSettings)=s.checkpointTasks&&s.callCheckpoints&&FeatureControls.enabled(s,"checklist")
 fun prepare(board:CallCheckpoints.Board,selected:Set<Int>,existing:List<CallChecklist.Item>):Preview {
  board.summary()
  require(selected.isNotEmpty()&&selected.all{it in board.marks.indices}){"Choose at least one checkpoint"}
  val seen=existing.map{ChecklistTemplates.normalized(it.text)}.toMutableSet()
  val topics=board.marks.filterIndexed{i,_->i in selected}.map{it.label}.filter{seen.add(ChecklistTemplates.normalized(it))}
  require(existing.size+topics.size<=CallChecklist.MAX_ITEMS){"Not enough room. Nothing added; choose fewer topics."}
  return Preview(board.copy(marks=board.marks.toList()),selected.toSet(),existing.toList(),topics,selected.size-topics.size)
 }
 fun preview(ctx:Context,session:LiveCallDesk.Session,board:CallCheckpoints.Board,selected:Set<Int>):Preview {
  require(enabled(LdApp.settings.value)&&session.current(LdApp.settings.value)){"Call or setting changed"}
  return prepare(board,selected,CallChecklist.read(ctx,session.number))
 }
 class Commit {
  private var completed=false
  @Synchronized fun save(ctx:Context,session:LiveCallDesk.Session,expected:Preview,current:CallCheckpoints.Board):List<CallChecklist.Item> {
   require(!completed&&enabled(LdApp.settings.value)&&session.current(LdApp.settings.value)){"Call or setting changed. Preview again."}
   require(expected.board==current&&expected==prepare(current,expected.selected,expected.existing)){"Board changed. Preview again."}
   require(expected.topics.isNotEmpty()){"No new topics to add"}
   val result=CallChecklist.appendChecked(ctx,session.number,expected.existing,expected.topics)
   completed=true;return result
  }
 }
}
