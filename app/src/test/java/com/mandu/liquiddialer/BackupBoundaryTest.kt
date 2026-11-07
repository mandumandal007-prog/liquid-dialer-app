package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.FullBackup
import com.mandu.liquiddialer.ui.SettingsBackup
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class BackupBoundaryTest {
 private val appearance="""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"theme":"midnight"}}"""
 private val full="""{"kind":"liquid-dialer-full-backup","version":1}"""
 @Test fun appearanceRejectsEveryTrailingPayloadAndRawNul(){
  for(suffix in listOf("garbage","{}","[]","null","0","\u0000","\u0000trailer")){
   assertTrue("reject suffix $suffix",SettingsBackup.parse(appearance+suffix) is SettingsBackup.ParseResult.Err)
  }
  assertTrue(SettingsBackup.parse("\u0000"+appearance) is SettingsBackup.ParseResult.Err)
 }
 @Test fun fullBackupRejectsRawNulRatherThanTreatingItAsEndOfInput(){
  for(text in listOf(full+"\u0000",full+"\u0000{}","\u0000"+full)){
   var rejected=false;try{FullBackup.parse(text)}catch(_:Exception){rejected=true}
   assertTrue("reject raw NUL",rejected)
  }
 }
 @Test fun validWhitespaceAndEscapedUnicodeStillParse(){
  assertTrue(SettingsBackup.parse(" \r\n\t"+appearance+" \r\n\t") is SettingsBackup.ParseResult.Ok)
  assertEquals(FullBackup.parse(full),FullBackup.parse(" \r\n\t"+full+" \r\n\t"))
  // An escaped NUL is data in a JSON string, not a raw input terminator.
  val escaped=full.dropLast(1)+",\"extra\":\"\\u0000\"}"
  assertEquals(FullBackup.parse(full),FullBackup.parse(escaped))
 }
}
