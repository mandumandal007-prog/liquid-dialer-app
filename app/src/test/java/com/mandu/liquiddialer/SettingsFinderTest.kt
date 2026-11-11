package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.SettingsFinder as F
import org.junit.Test
import org.junit.Assert.*
class SettingsFinderTest{
 @Test fun indexHasUniqueStableIdsAndNoBrokenMetadata(){assertEquals(F.all.size,F.all.map{it.id}.toSet().size);F.all.forEach{assertTrue(it.title.isNotBlank());assertFalse(it.detail.contains('$'));assertFalse(it.detail.contains("}/"));assertFalse(it.detail.contains("//"));assertTrue(it.group.isNotBlank())}}
 @Test fun everyTitleFindsItsEntry(){F.all.forEach{assertTrue(it.id,F.search(it.title).contains(it))}}
 @Test fun caseInsensitiveAndTermsWorkAcrossTitleAndDescription(){assertTrue(F.search("BLUETOOTH speaker").any{it.title=="Connect pe speaker"});assertTrue(F.search("call silent").any{it.title=="Quiet hours"});assertTrue(F.search("font").any{it.route=="appearance"})}
 @Test fun emptyAndUnknownQueries(){assertEquals(F.all,F.search("  "));assertTrue(F.search("neverexistingxyz").isEmpty());assertEquals(F.all,F.search("..."))}
 @Test fun routeRoundTripsEveryRowWithoutTargetAction(){F.all.filter{it.target.isNotEmpty()}.forEach{assertEquals(it,F.resolve(F.route(it)));assertTrue(F.route(it).startsWith("settings:"));assertEquals(it.route,F.route(it).substringBefore('?'))}}
 @Test fun workspaceRoutesStayPlain(){F.all.filter{it.target.isEmpty()}.forEach{assertEquals(it.route,F.route(it));assertNull(F.resolve(F.route(it)))}}
 @Test fun rejectsUnknownAndCrossGroupAndMalformedTargets(){assertNull(F.resolve("settings:calls?find=display-0"));assertNull(F.resolve("settings:display?find=missing"));assertNull(F.resolve("settings:display?find=%GG"));assertNull(F.resolve("settings:display"));assertNull(F.resolve("settings:display?find=display-0&run=true"))}
 @Test fun noPausedFullBackupOrConditionalTargets(){assertTrue(F.all.none{it.route=="fullbackup"||it.title=="Poora app backup"||it.title=="Screening role nahi mila"||it.title=="Full screen notification permission"})}
}
