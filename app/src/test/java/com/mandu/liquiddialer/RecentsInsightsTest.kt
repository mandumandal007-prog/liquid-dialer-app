package com.mandu.liquiddialer

import android.provider.CallLog
import androidx.datastore.preferences.core.edit
import com.mandu.liquiddialer.data.ldStore
import kotlinx.coroutines.flow.first
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.Repo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Recents insights: dayStats (aaj ka strip) + unseenMissed (tab badge) ka exact math. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentsInsightsTest {

    private fun r(id: Long, type: Int, ageMs: Long, now: Long, dur: Long = 0) =
        Recent(id, "+91 90000 0000$id", "Zed $id", type, now - ageMs, dur, null, null)

    // Ancora em 13:00 do dia corrente: offsets de segundos/minutos continuam "aaj" e
    // 2 dias atras cai em outro dia, em qualquer horario que o teste rodar (sem flake de meia-noite).
    private fun anchoredNow(): Long {
        val zone = java.time.ZoneId.systemDefault()
        return java.time.LocalDate.now(zone).atTime(13, 0).atZone(zone).toInstant().toEpochMilli()
    }

    @Test
    fun dayStatsMath() {
        val now = anchoredNow()
        val list = listOf(
            r(1, CallLog.Calls.OUTGOING_TYPE, 60_000, now, 245),   // aaj, 245s
            r(2, CallLog.Calls.INCOMING_TYPE, 120_000, now, 55),   // aaj, 55s
            r(3, CallLog.Calls.MISSED_TYPE, 180_000, now),         // aaj missed
            r(4, CallLog.Calls.OUTGOING_TYPE, 2 * 86_400_000L, now, 999) // parso - count nahi
        )
        val st = Repo.dayStats(list, now)
        assertEquals(3, st.calls)
        assertEquals(300, st.talkSecs)
        assertEquals(1, st.missed)
        // khali list = sab zero (strip hidden)
        assertEquals(Repo.DayStats(0, 0, 0), Repo.dayStats(emptyList(), now))
    }

    @Test
    fun unseenMissedMath() {
        val now = anchoredNow()
        val list = listOf(
            r(1, CallLog.Calls.MISSED_TYPE, 60_000, now),          // naya missed
            r(2, CallLog.Calls.MISSED_TYPE, 3 * 86_400_000L, now), // purana missed
            r(3, CallLog.Calls.REJECTED_TYPE, 30_000, now),        // user ne reject kiya = missed NAHI
            r(4, CallLog.Calls.OUTGOING_TYPE, 10_000, now)         // outgoing badge nahi
        )
        // seenAt 5 min pehle: sirf naya sacha missed (1); rejected count nahi hota
        assertEquals(1, Repo.unseenMissed(list, now - 300_000))
        // sab seen: 0
        assertEquals(0, Repo.unseenMissed(list, now))
        // kabhi nahi dekha: dono missed (rejected/outgoing nahi)
        assertEquals(2, Repo.unseenMissed(list, 0L))
    }
}


class HisabCsvTest {
    private fun r(id: Long, number: String, name: String?, type: Int, date: Long, dur: Long) =
        com.mandu.liquiddialer.data.Recent(id, number, name, type, date, dur, null, null)

    @org.junit.Test
    fun headerAndTypes() {
        val csv = com.mandu.liquiddialer.data.Repo.recentsCsv(listOf(
            r(1, "+91 98765 43210", "Rahul Sharma", 2, 1762000000000L, 245),
            r(2, "+91 90000 11111", null, 3, 1762000060000L, 0),
        ))
        val lines = csv.trim().split("\n")
        assert(lines[0] == "date,time,name,number,type,duration_sec") { lines[0] }
        assert(lines[1].endsWith(",Rahul Sharma,\t+91 98765 43210,Outgoing,245")) { lines[1] }
        assert(lines[2].endsWith(",,\t+91 90000 11111,Missed,0")) { lines[2] }
        assert(lines[1].matches(Regex("""\d{4}-\d{2}-\d{2},\d{2}:\d{2},.*"""))) { lines[1] }
    }

    @org.junit.Test
    fun commasInNameAreQuoted() {
        val csv = com.mandu.liquiddialer.data.Repo.recentsCsv(listOf(
            r(1, "123", "Mandal, Chandradev", 1, 1762000000000L, 5)))
        assert(csv.contains("\"Mandal, Chandradev\"")) { csv }
    }

    @org.junit.Test
    fun formulaLikeCellsArePrefixedSoTheyStayLiteral() {
        // =,+,-,@ se shuru cells Excel/Sheets me formula ban sakte hain - TAB prefix se text hi rehte hain
        val csv = com.mandu.liquiddialer.data.Repo.recentsCsv(listOf(
            r(1, "+91 91234 56789", "=cmd|calc", 1, 1762000000000L, 5),
            r(2, "-1234", "@mention", 2, 1762000060000L, 0)))
        assert(csv.contains("\t=cmd|calc")) { csv }
        assert(csv.contains("\t+91 91234 56789")) { csv }
        assert(csv.contains("\t-1234")) { csv }
        assert(csv.contains("\t@mention")) { csv }
    }

    @org.junit.Test
    fun plusAndLeadingZerosAndUnicodeAndNewlinesSurvive() {
        val csv = com.mandu.liquiddialer.data.Repo.recentsCsv(listOf(
            r(1, "+91 09876 54321", "Priya \u092a\u094d\u0930\u093f\u092f\u093e", 1, 1762000000000L, 5),
            r(2, "0088 111 222", "Line1\nLine2", 2, 1762000060000L, 0)))
        assert(csv.contains("\t+91 09876 54321")) { csv }          // + aur leading zero dono literal
        assert(csv.contains("0088 111 222")) { csv }                // 0 se shuru: koi prefix nahi, as-is
        assert(csv.contains("Priya \u092a\u094d\u0930\u093f\u092f\u093e")) { csv }  // unicode as-is
        assert(csv.contains("\"Line1\nLine2\"")) { csv }          // newline wala cell quoted
    }

    @org.junit.Test
    fun emptyWindowIsHeaderOnly() {
        assert(com.mandu.liquiddialer.data.Repo.recentsCsv(emptyList()) == "date,time,name,number,type,duration_sec\n")
    }
}

/** Settings backup (appearance-only whitelist): round-trip + hostile/corrupt/unsupported input + capped URI read + atomic apply. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsBackupTest {

    private val sb get() = com.mandu.liquiddialer.ui.SettingsBackup

    private fun sample(): com.mandu.liquiddialer.data.LdSettings =
        com.mandu.liquiddialer.data.LdSettings(
            lang = "hindi", theme = "daylight", accent = 0xFF112233, blur = 10, motion = false,
            fontScale = 1.1f, sortByLast = true, boldText = true, callAvatarShape = "squircle",
            callBgStyle = "solid", gridScale = 0.8f, bottomScale = 1.2f, callCardScale = 1.3f,
            dialerScale = 0.9f, showFavourites = false, keypadLetters = false, defaultTab = "dialer")

    @Test
    fun roundTripPreservesWhitelistedValues() {
        val s = sample()
        val res = sb.parse(sb.buildJson(s))
        assert(res is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Ok) { "$res" }
        val v = (res as com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Ok).values
        org.junit.Assert.assertEquals("hindi", v["lang"])
        org.junit.Assert.assertEquals("daylight", v["theme"])
        org.junit.Assert.assertEquals(0xFF112233, v["accent"])
        org.junit.Assert.assertEquals(10, v["blur"])
        org.junit.Assert.assertEquals(false, v["motion"])
        org.junit.Assert.assertEquals(1.1f, v["fontScale"] as Float, 0.001f)
        org.junit.Assert.assertEquals(true, v["sortByLast"])
        org.junit.Assert.assertEquals("squircle", v["callAvatarShape"])
        org.junit.Assert.assertEquals("solid", v["callBgStyle"])
        org.junit.Assert.assertEquals(0.8f, v["gridScale"] as Float, 0.001f)
        org.junit.Assert.assertEquals(1.2f, v["bottomScale"] as Float, 0.001f)
        org.junit.Assert.assertEquals("dialer", v["defaultTab"])
        org.junit.Assert.assertEquals(s.layoutGrid, v["layoutGrid"])
        org.junit.Assert.assertEquals(s.layoutBottom, v["layoutBottom"])
    }

    @Test
    fun exportNeverContainsPersonalOrBehavioralKeys() {
        val json = sb.buildJson(sample())
        listOf("profileName", "profileNumber", "profilePhoto", "voicemail", "quickReplies", "speedDial",
            "defaultSim", "blockUnknown", "blockPrivate", "blockedPrefixes", "autoRecord", "announceCaller",
            "quietHours", "flipToMute", "swipeAnswer", "onboarded", "recentsSeenAt").forEach {
            assert(!json.contains(it)) { "leaked $it" }
        }
    }

    @Test
    fun hostileAndCorruptFilesRejectedWithoutPartialParse() {
        // corrupt JSON
        assert(sb.parse("ye koi file nahi hai") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        // wrong kind
        assert(sb.parse("""{"app":"liquid-dialer","kind":"other","version":1,"values":{}}""") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        // future version
        assert(sb.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":99,"values":{"blur":10}}""") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        // type confusion: fontScale as string
        assert(sb.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"fontScale":"bada"}}""") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        // out of range
        assert(sb.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"blur":99}}""") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        assert(sb.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"fontScale":9.9}}""") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        // bad enum
        assert(sb.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"theme":"neon"}}""") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        // layout with unknown/missing ids
        assert(sb.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"layoutGrid":"mute,keypad"}}""") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        assert(sb.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"layoutGrid":"mute,keypad,record,hold,add,merge,hack"}}""") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        // bool as string
        assert(sb.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"motion":"true"}}""") is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
        // oversize
        val big = """{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"blur":10},"pad":"""" + "x".repeat(70 * 1024) + """"}"""
        assert(sb.parse(big) is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Err)
    }

    @Test
    fun unknownKeysIgnoredButWhitelistedApplied() {
        val res = sb.parse("""{"app":"liquid-dialer","kind":"appearance-settings","version":1,"values":{"blur":12,"kyaYeKeyHai":true,"blockUnknown":true}}""")
        assert(res is com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Ok) { "$res" }
        val ok = res as com.mandu.liquiddialer.ui.SettingsBackup.ParseResult.Ok
        org.junit.Assert.assertEquals(mapOf("blur" to 12), ok.values)   // blockUnknown jaisa dangerous key kabhi apply nahi hoga
        org.junit.Assert.assertEquals(2, ok.ignoredKeys)
    }

    @Test
    fun readCappedReadsFileUriAndRejectsOversize() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val small = java.io.File(ctx.cacheDir, "sb_test_small.json")
        small.writeText("""{"app":"liquid-dialer"}""")
        org.junit.Assert.assertEquals("""{"app":"liquid-dialer"}""", sb.readCapped(ctx, android.net.Uri.fromFile(small)))
        val big = java.io.File(ctx.cacheDir, "sb_test_big.json")
        big.writeText("x".repeat(80 * 1024))
        assert(sb.readCapped(ctx, android.net.Uri.fromFile(big)) == null) { "oversize file null honi chahiye" }
        small.delete(); big.delete()
    }

    @Test
    fun applyRestoredTouchesOnlyWhitelistAndLeavesBehavioralPrefsUntouched() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val K = com.mandu.liquiddialer.data.K
        kotlinx.coroutines.runBlocking {
            ctx.ldStore.edit { p ->
                p[K.voicemail] = "999"; p[K.blockUnknown] = true; p[K.blur] = 5; p[K.theme] = "midnight"
            }
            // map me "blockUnknown" jaise non-whitelist key ho to bhi applyRestored usse chhuta hi nahi
            com.mandu.liquiddialer.ui.SettingsBackup.applyRestored(ctx, mapOf("blur" to 30, "theme" to "daylight", "blockUnknown" to false))
            val p = ctx.ldStore.data.first()
            org.junit.Assert.assertEquals(30, p[K.blur])
            org.junit.Assert.assertEquals("daylight", p[K.theme])
            org.junit.Assert.assertEquals("999", p[K.voicemail])
            org.junit.Assert.assertEquals(true, p[K.blockUnknown])
            ctx.ldStore.edit { it.clear() }
        }
    }
}
