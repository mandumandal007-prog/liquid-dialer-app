package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.NoteFind
import org.junit.Assert.*
import org.junit.Test
class NoteFindTest {
 @Test fun caseInsensitiveLiteralPunctuationAndOffsets(){
  val text="Call [abc]. CALL [abc]."
  assertEquals(listOf(NoteFind.Match(0,4),NoteFind.Match(12,16)),NoteFind.matches(text,"call"))
  assertEquals(listOf(NoteFind.Match(5,10),NoteFind.Match(17,22)),NoteFind.matches(text,"[abc]"))
  assertTrue(NoteFind.matches(text,".*").isEmpty())
 }
 @Test fun emptyNoMatchAndNonOverlapping(){
  assertTrue(NoteFind.matches("abc","").isEmpty());assertTrue(NoteFind.matches("abc","   ").isEmpty())
  assertTrue(NoteFind.matches("a","abc").isEmpty());assertEquals(listOf(NoteFind.Match(0,2),NoteFind.Match(2,4)),NoteFind.matches("aaaaa","aa"))
 }
 @Test fun hindiEmojiAndNewlinePreserveOriginalIndices(){
  val text="📞 कल फोन\nकल फोन"
  val matches=NoteFind.matches(text,"कल फोन");assertEquals(2,matches.size)
  for(m in matches)assertEquals("कल फोन",text.substring(m.start,m.end))
  assertEquals(1,NoteFind.matches(text,"फोन\nकल").size)
 }
 @Test fun fullLimitNoteKeepsAllMatchesAndWraps(){
  val text="word ".repeat(10000);val ms=NoteFind.matches(text,"word")
  assertEquals(10000,ms.size);assertEquals(49995,ms.last().start)
  assertEquals(0,NoteFind.next(9999,10000,1));assertEquals(9999,NoteFind.next(0,10000,-1));assertEquals(0,NoteFind.next(0,0,1))
  assertEquals(50000,text.length)
 }
}
