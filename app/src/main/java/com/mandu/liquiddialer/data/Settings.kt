package com.mandu.liquiddialer.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.ldStore by preferencesDataStore(name = "ld_settings")

data class LdSettings(
    val hiddenFeatures: Set<String> = emptySet(),
    val quietRows: Boolean = false,
    val refinedHeaders: Boolean = false,
    val lookProfiles: String = "",
    val lang: String = "hinglish",
    val theme: String = "midnight",
    val accent: Long = 0xFF5B8CFF,
    val blur: Int = 22,
    val glassDepth: Int = 2, // 0=flat,1=soft,2=sculpted
    val glassFrost: Int = 1, // 0=clear,1=balanced,2=milky
    val motion: Boolean = true,
    val fontScale: Float = 1f,
    val sortByLast: Boolean = false,
    val lastNameFirst: Boolean = false,
    val profileName: String = "",
    val profileNumber: String = "",
    val defaultSim: String = "ask",          // "ask" or PhoneAccountHandle id
    val proximity: Boolean = true,
    val vibrateAnswer: Boolean = true,
    val vibrateEnd: Boolean = false,
    val keyTones: Boolean = true,
    val flipToMute: Boolean = true,
    val announceCaller: Boolean = false,
    val autoRecord: Boolean = false,
    val banner: Boolean = true,
    val missedAlerts: Boolean = true,
    val blockUnknown: Boolean = false,
    val blockPrivate: Boolean = false,
    val voicemail: String = "",
    val quickReplies: List<String> = DEFAULT_REPLIES,
    val speedDial: Map<Int, String> = emptyMap(),
    val recentsSeenAt: Long = 0L,
    val onboarded: Boolean = false,
    val incomingFull: Boolean = true,
    val swipeAnswer: Boolean = true,
    val profilePhoto: String = "",
    val layoutGrid: String = DEFAULT_GRID,
    val layoutBottom: String = DEFAULT_BOTTOM,
    val gridScale: Float = 1f,
    val bottomScale: Float = 1f,
    val tabSwipe: Boolean = true,
    val boldText: Boolean = false,
    val fontStyle: Int = 0,      // 0=Default, 1=Serif, 2=Monospace, 3=Cursive
    val dialerLayout: Int = 0, // 0 circles, 1 soft tiles, 2 wide pills; dialer only
    val cleanGlass: Boolean = false,
    val airyCall: Boolean = false,
    val premiumConfirm: Boolean = false,
    val callIconConfig: String = "",
    val iconStyle: Int = 0,      // LdIcons.ICON_STYLES index (0=Default, 6=Outline, 7=Duotone)
    val callCardScale: Float = 1f,       // calling card: naam + photo ka size
    val dialerScale: Float = 1f,         // dialer keypad ka size
    val callAvatarShape: String = "circle",   // "circle" | "squircle"
    val callShowNumber: Boolean = true,
    val callBgStyle: String = "blobs",
    val callPhotoBlur: Int = 0, // 0 clear,1 light,2 soft; cached bitmap blur on Android8+
    val showFavourites: Boolean = true,
    // ---- v1.0.52: calling & caller-pehchan pack ----
    val flashOnRing: Boolean = false,        // opt-in: torch blink while ringing
    val announceRepeat: Int = 1,             // 1..3 baar caller naam bolo
    val showLastCall: Boolean = true,        // incoming screen pe pichli call ki jankari
    val showContactType: Boolean = true,     // Mobile/Work label incoming pe
    val favStarBadge: Boolean = true,        // favourite star incoming pe
    val confirmCall: Boolean = false,        // recents/contacts tap pe confirm card
    val autoPrefix: Boolean = false,         // 10-digit number ke aage +91
    val formatNumber: Boolean = true,        // dialer me live number formatting
    val redialSuggest: Boolean = true,       // busy/fail pe "Dobara call" notification
    val postCallSave: Boolean = true,        // unknown number ke baad Save/Block prompt
    val missedRemind: Boolean = false,       // opt-in: missed call reminder 5 min x3
    val birthdayRemind: Boolean = false,     // opt-in: birthday wale din subah 9 baje notification
    val callPace: Boolean = false,
    val callPaceMinutes: Int = 5,
    val callPaceRepeat: Boolean = false,
    val callReferences: Boolean = false,
    val ivrHelper: Boolean = false,
    val quietWeekMode: Boolean = false,
    val quietWeekRules: String = "",
    val quietHours: Boolean = false,         // opt-in: quiet hours me incoming call silent (sirf silence, reject nahi)
    val quietStart: Int = 1320,              // quiet hours shuru (minute of day, default 22:00)
    val quietEnd: Int = 420,                 // quiet hours khatam (minute of day, default 07:00)
    val quietAllowFav: Boolean = true,       // favourite contacts quiet hours me bhi bajenge
    val blockedPrefixes: List<String> = emptyList(), // spam series block: user-added prefixes, default khali = off
    val spamPromoBlock: Boolean = false,     // opt-in: 140 telemarketing series auto-reject (screening role chahiye)
    val intlUnknownBlock: Boolean = false,   // opt-in: unknown international calls reject (contacts permission chahiye)
    val intlUnknownWarn: Boolean = true,     // unknown international pe sirf warning badge (block nahi)
    val spamBadges: Boolean = true,          // incoming pe 140/160/international/network-verification badges
    val waPill: Boolean = true,              // unknown number ki incoming screen pe "WhatsApp kholo" pill
    val callSummary: Boolean = false,        // opt-in: call end par naam+duration notification (lock screen private)
    val keypadLetters: Boolean = true,       // keypad pe ABC letters
    val defaultTab: String = "recents",      // "recents" | "dialer"
    val undoCall: Boolean = false,           // opt-in: 3s undo before dialing
    val simMemory: Boolean = true,           // contact-wise pichli SIM yaad rakho
    val muteReminder: Boolean = false,       // opt-in call-time warning, never unmutes
    val audioRouteAlert: Boolean = false,    // opt-in headset-to-phone route notice
    val minuteBeep: Int = 0,                 // 0=off else har N minute pe beep
    val callVolumeMax: Boolean = false,      // opt-in: call connect pe volume system max
    val ringVolumeMax: Boolean = false,      // opt-in: ring pe volume system max
    val groupByDay: Boolean = true,           // recents me din ke headers
    val showTodayStats: Boolean = false,      // daily strip opt-in from drawer
    val hapticKeys: Boolean = false,         // keypad tap pe haptic
    val recordUnknownOnly: Boolean = false,  // sirf unknown numbers record
    val exactTime: Boolean = true,           // recents me exact samay vs relative
    // ---- v1.0.68: 10 naye user controls ----
    val autoSpeaker: Boolean = false,        // opt-in: outgoing connect pe speaker on
    val confirmBeforeEnd: Boolean = false,   // opt-in: end button pe confirm card
    val recentsShowDuration: Boolean = true, // recents me call ki duration
    val showSimInRecents: Boolean = true,    // recents me SIM label
    val recentsCompact: Boolean = false,     // recents ki choti rows
    val searchBottom: Boolean = false,       // search/menu/profile bar neeche (angutha pahunch) - opt-in
    val rowSwipe: Boolean = false,           // recents row swipe: right = call, left = SMS (opt-in)
    val recentLimit: Int = 300,              // recents me kitni entries (100/300/500)
    val announceOnlyFav: Boolean = false,    // sirf favourite ka naam bolo
    val flashOnlyFav: Boolean = false,       // sirf favourite pe flash
    val recentsHideBlocked: Boolean = false, // blocked calls recents me na dikhao
    // ---- v1.0.113: volume section ----
    val volBoost: Boolean = false,           // opt-in: media loudness boost (session 0, GOODEV-style); call audio ko kabhi nahi chhoota
    val volBoostGain: Int = 25,              // boost 0..100% -> LoudnessEnhancer gain; shuruaat jaan-boojh kar LOW
    val boostAck: Boolean = false,           // user ne distortion/hearing/speaker risk acknowledge kiya
    val simUse: Map<String, String> = emptyMap(), // number -> SIM handle id (memory me; call dispatch pe disk read nahi)
    val appLock: Boolean = false,            // opt-in: app kholne pe device credential (phone ka PIN/pattern/fingerprint)
    val appLockDelaySec: Int = 0,            // background ke kitne sec baad relock (0 = turant)
    val callCardStyle: String = "glass",      // CALL_STYLES key (ui/CallStyles.kt); unknown -> glass fallback
    val recentsStyle: String = "glass",       // RECENTS_STYLES key; unknown -> glass fallback
    val autoBackup: Boolean = false,          // app khulne par hafte-me-ek khud backup (device-local)
    val lastAutoBackupAt: Long = 0L,          // aakhri safal auto-backup ka time
    val lastAutoBackupTry: Long = 0L,         // aakhri koshish ka time (retry-storm rokne ke liye)
)

const val DEFAULT_GRID = "mute,keypad,record,hold,add,merge,!audio"
const val DEFAULT_BOTTOM = "speaker,end,notes"

val DEFAULT_REPLIES = listOf(
    "Abhi baat nahi kar sakta, baad mein call karta hoon.",
    "Meeting mein hoon, thodi der mein call karunga.",
    "Gaadi chala raha hoon, baad mein baat karte hain.",
    "Kya hua? Message kar do.",
)

object K {
    val hiddenFeatures = stringSetPreferencesKey("hiddenFeatures")
    val quietRows = booleanPreferencesKey("quietRows")
    val refinedHeaders = booleanPreferencesKey("refinedHeaders")
    val lookProfiles = stringPreferencesKey("lookProfiles")
    val lang = stringPreferencesKey("lang")
    val theme = stringPreferencesKey("theme")
    val accent = longPreferencesKey("accent")
    val blur = intPreferencesKey("blur")
    val glassDepth = intPreferencesKey("glass_depth")
    val glassFrost = intPreferencesKey("glass_frost")
    val motion = booleanPreferencesKey("motion")
    val fontScale = floatPreferencesKey("fontScale")
    val sortByLast = booleanPreferencesKey("sortByLast")
    val lastNameFirst = booleanPreferencesKey("lastNameFirst")
    val profileName = stringPreferencesKey("profileName")
    val profileNumber = stringPreferencesKey("profileNumber")
    val defaultSim = stringPreferencesKey("defaultSim")
    val proximity = booleanPreferencesKey("proximity")
    val vibrateAnswer = booleanPreferencesKey("vibrateAnswer")
    val vibrateEnd = booleanPreferencesKey("vibrateEnd")
    val keyTones = booleanPreferencesKey("keyTones")
    val flipToMute = booleanPreferencesKey("flipToMute")
    val announceCaller = booleanPreferencesKey("announceCaller")
    val autoRecord = booleanPreferencesKey("autoRecord")
    val banner = booleanPreferencesKey("banner")
    val missedAlerts = booleanPreferencesKey("missedAlerts")
    val blockUnknown = booleanPreferencesKey("blockUnknown")
    val blockPrivate = booleanPreferencesKey("blockPrivate")
    val spamPromoBlock = booleanPreferencesKey("spamPromoBlock")
    val intlUnknownBlock = booleanPreferencesKey("intlUnknownBlock")
    val intlUnknownWarn = booleanPreferencesKey("intlUnknownWarn")
    val spamBadges = booleanPreferencesKey("spamBadges")
    val waPill = booleanPreferencesKey("waPill")
    val voicemail = stringPreferencesKey("voicemail")
    val quickReplies = stringPreferencesKey("quickReplies")
    val speedDial = stringPreferencesKey("speedDial")
    val recentsSeenAt = longPreferencesKey("recentsSeenAt")
    val onboarded = booleanPreferencesKey("onboarded")
    val incomingFull = booleanPreferencesKey("incomingFull")
    val swipeAnswer = booleanPreferencesKey("swipeAnswer")
    val profilePhoto = stringPreferencesKey("profilePhoto")
    val layoutGrid = stringPreferencesKey("layoutGrid")
    val layoutBottom = stringPreferencesKey("layoutBottom")
    val gridScale = floatPreferencesKey("gridScale")
    val bottomScale = floatPreferencesKey("bottomScale")
    val tabSwipe = booleanPreferencesKey("tabSwipe")
    val boldText = booleanPreferencesKey("boldText")
    val fontStyle = intPreferencesKey("font_style")
    val dialerLayout = intPreferencesKey("dialerLayout")
    val cleanGlass = booleanPreferencesKey("cleanGlass")
    val airyCall = booleanPreferencesKey("airyCall")
    val premiumConfirm = booleanPreferencesKey("premiumConfirm")
    val callIconConfig = stringPreferencesKey("callIconConfig")
    val iconStyle = intPreferencesKey("icon_style")
    val callCardScale = floatPreferencesKey("callCardScale")
    val dialerScale = floatPreferencesKey("dialerScale")
    val callAvatarShape = stringPreferencesKey("callAvatarShape")
    val callShowNumber = booleanPreferencesKey("callShowNumber")
    val callPhotoBlur = intPreferencesKey("callPhotoBlur")
    val callBgStyle = stringPreferencesKey("callBgStyle")
    val showFavourites = booleanPreferencesKey("showFavourites")
    val flashOnRing = booleanPreferencesKey("flashOnRing")
    val announceRepeat = intPreferencesKey("announceRepeat")
    val showLastCall = booleanPreferencesKey("showLastCall")
    val showContactType = booleanPreferencesKey("showContactType")
    val favStarBadge = booleanPreferencesKey("favStarBadge")
    val confirmCall = booleanPreferencesKey("confirmCall")
    val autoPrefix = booleanPreferencesKey("autoPrefix")
    val formatNumber = booleanPreferencesKey("formatNumber")
    val redialSuggest = booleanPreferencesKey("redialSuggest")
    val postCallSave = booleanPreferencesKey("postCallSave")
    val missedRemind = booleanPreferencesKey("missedRemind")
    val birthdayRemind = booleanPreferencesKey("birthdayRemind")
    val callPace = booleanPreferencesKey("call_pace")
    val callPaceMinutes = intPreferencesKey("call_pace_minutes")
    val callPaceRepeat = booleanPreferencesKey("call_pace_repeat")
    val callReferences = booleanPreferencesKey("call_references")
    val ivrHelper = booleanPreferencesKey("ivr_helper")
    val quietWeekMode = booleanPreferencesKey("quietWeekMode")
    val quietWeekRules = stringPreferencesKey("quietWeekRules")
    val quietHours = booleanPreferencesKey("quietHours")
    val quietStart = intPreferencesKey("quietStart")
    val quietEnd = intPreferencesKey("quietEnd")
    val quietAllowFav = booleanPreferencesKey("quietAllowFav")
    val blockedPrefixes = stringPreferencesKey("blockedPrefixes")
    val callSummary = booleanPreferencesKey("callSummary")
    val keypadLetters = booleanPreferencesKey("keypadLetters")
    val defaultTab = stringPreferencesKey("defaultTab")
    val undoCall = booleanPreferencesKey("undoCall")
    val simMemory = booleanPreferencesKey("simMemory")
    val muteReminder = booleanPreferencesKey("muteReminder")
    val audioRouteAlert = booleanPreferencesKey("audioRouteAlert")
    val minuteBeep = intPreferencesKey("minuteBeep")
    val callVolumeMax = booleanPreferencesKey("callVolumeMax")
    val ringVolumeMax = booleanPreferencesKey("ringVolumeMax")
    val groupByDay = booleanPreferencesKey("groupByDay")
    val showTodayStats = booleanPreferencesKey("show_today_stats")
    val hapticKeys = booleanPreferencesKey("hapticKeys")
    val recordUnknownOnly = booleanPreferencesKey("recordUnknownOnly")
    val exactTime = booleanPreferencesKey("exactTime")
    val autoSpeaker = booleanPreferencesKey("autoSpeaker")
    val confirmBeforeEnd = booleanPreferencesKey("confirmBeforeEnd")
    val recentsShowDuration = booleanPreferencesKey("recentsShowDuration")
    val showSimInRecents = booleanPreferencesKey("showSimInRecents")
    val recentsCompact = booleanPreferencesKey("recentsCompact")
    val rowSwipe = booleanPreferencesKey("rowSwipe")
    val searchBottom = booleanPreferencesKey("searchBottom")
    val recentLimit = intPreferencesKey("recentLimit")
    val announceOnlyFav = booleanPreferencesKey("announceOnlyFav")
    val flashOnlyFav = booleanPreferencesKey("flashOnlyFav")
    val recentsHideBlocked = booleanPreferencesKey("recentsHideBlocked")
    val volBoost = booleanPreferencesKey("volBoost")
    val volBoostGain = intPreferencesKey("volBoostGain")
    val boostAck = booleanPreferencesKey("boostAck")
    val simUse = stringPreferencesKey("simUse")   // "digits=handleId;digits=handleId"
    val appLock = booleanPreferencesKey("appLock")
    val appLockDelaySec = intPreferencesKey("appLockDelaySec")
    val callCardStyle = stringPreferencesKey("callCardStyle")
    val recentsStyle = stringPreferencesKey("recentsStyle")
    val autoBackup = booleanPreferencesKey("autoBackup")
    val lastAutoBackupAt = longPreferencesKey("lastAutoBackupAt")
    val lastAutoBackupTry = longPreferencesKey("lastAutoBackupTry")
}

internal fun simUseMapOf(p: Preferences): Map<String, String> = (p[K.simUse] ?: "").split(";").mapNotNull {
    val kv = it.split("=", limit = 2)
    if (kv.size == 2 && kv[0].isNotBlank()) kv[0] to kv[1] else null
}.toMap()

fun Preferences.toSettings(): LdSettings {
    val d = LdSettings()
    return LdSettings(
        quietRows = this[K.quietRows] ?: false,
        refinedHeaders = this[K.refinedHeaders] ?: false,
        lookProfiles = this[K.lookProfiles] ?: "",
        hiddenFeatures = (this[K.hiddenFeatures] ?: emptySet()).filter{it in FeatureControls.ids}.toSet(),
        lang = this[K.lang] ?: d.lang,
        theme = this[K.theme] ?: d.theme,
        accent = this[K.accent] ?: d.accent,
        blur = this[K.blur] ?: d.blur,
        glassDepth = (this[K.glassDepth] ?: d.glassDepth).coerceIn(0,2),
        glassFrost = (this[K.glassFrost] ?: d.glassFrost).coerceIn(0,2),
        motion = this[K.motion] ?: d.motion,
        fontScale = this[K.fontScale] ?: d.fontScale,
        sortByLast = this[K.sortByLast] ?: d.sortByLast,
        lastNameFirst = this[K.lastNameFirst] ?: d.lastNameFirst,
        profileName = this[K.profileName] ?: d.profileName,
        profileNumber = this[K.profileNumber] ?: d.profileNumber,
        defaultSim = this[K.defaultSim] ?: d.defaultSim,
        proximity = this[K.proximity] ?: d.proximity,
        vibrateAnswer = this[K.vibrateAnswer] ?: d.vibrateAnswer,
        vibrateEnd = this[K.vibrateEnd] ?: d.vibrateEnd,
        keyTones = this[K.keyTones] ?: d.keyTones,
        flipToMute = this[K.flipToMute] ?: d.flipToMute,
        announceCaller = this[K.announceCaller] ?: d.announceCaller,
        autoRecord = this[K.autoRecord] ?: d.autoRecord,
        banner = this[K.banner] ?: d.banner,
        missedAlerts = this[K.missedAlerts] ?: d.missedAlerts,
        blockUnknown = this[K.blockUnknown] ?: d.blockUnknown,
        blockPrivate = this[K.blockPrivate] ?: d.blockPrivate,
        spamPromoBlock = this[K.spamPromoBlock] ?: d.spamPromoBlock,
        intlUnknownBlock = this[K.intlUnknownBlock] ?: d.intlUnknownBlock,
        intlUnknownWarn = this[K.intlUnknownWarn] ?: d.intlUnknownWarn,
        spamBadges = this[K.spamBadges] ?: d.spamBadges,
        waPill = this[K.waPill] ?: d.waPill,
        voicemail = this[K.voicemail] ?: d.voicemail,
        quickReplies = this[K.quickReplies]?.split("\n")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: d.quickReplies,
        speedDial = this[K.speedDial]?.split(";")?.mapNotNull { s ->
            val p = s.split("=", limit = 2); p.getOrNull(0)?.toIntOrNull()?.let { k -> p.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { k to it } }
        }?.toMap() ?: emptyMap(),
        recentsSeenAt = this[K.recentsSeenAt] ?: 0L,
        onboarded = this[K.onboarded] ?: false,
        incomingFull = this[K.incomingFull] ?: true,
        swipeAnswer = this[K.swipeAnswer] ?: true,
        profilePhoto = this[K.profilePhoto] ?: "",
        layoutGrid = this[K.layoutGrid] ?: DEFAULT_GRID,
        layoutBottom = this[K.layoutBottom] ?: DEFAULT_BOTTOM,
        gridScale = this[K.gridScale] ?: 1f,
        bottomScale = this[K.bottomScale] ?: 1f,
        tabSwipe = this[K.tabSwipe] ?: true,
        boldText = this[K.boldText] ?: false,
        fontStyle = (this[K.fontStyle] ?: 0).coerceIn(0, 3),
        cleanGlass = this[K.cleanGlass] ?: false,
        airyCall = this[K.airyCall] ?: false,
        premiumConfirm = this[K.premiumConfirm] ?: false,
        callIconConfig = (this[K.callIconConfig] ?: "").take(6000),
        iconStyle = (this[K.iconStyle] ?: 0).coerceIn(0, 7),
        dialerLayout = (this[K.dialerLayout] ?: 0).coerceIn(0, 2),
        callCardScale = this[K.callCardScale] ?: 1f,
        dialerScale = this[K.dialerScale] ?: 1f,
        callAvatarShape = this[K.callAvatarShape] ?: "circle",
        callShowNumber = this[K.callShowNumber] ?: true,
        showFavourites = this[K.showFavourites] ?: true,
        flashOnRing = this[K.flashOnRing] ?: false,
        announceRepeat = this[K.announceRepeat] ?: 1,
        showLastCall = this[K.showLastCall] ?: true,
        showContactType = this[K.showContactType] ?: true,
        favStarBadge = this[K.favStarBadge] ?: true,
        confirmCall = this[K.confirmCall] ?: false,
        autoPrefix = this[K.autoPrefix] ?: false,
        formatNumber = this[K.formatNumber] ?: true,
        redialSuggest = this[K.redialSuggest] ?: true,
        postCallSave = this[K.postCallSave] ?: true,
        missedRemind = this[K.missedRemind] ?: false,
        birthdayRemind = this[K.birthdayRemind] ?: false,
        callPace = this[K.callPace] ?: false,
        callPaceMinutes = (this[K.callPaceMinutes] ?: 5).takeIf{it in listOf(1,3,5,10,15)} ?: 5,
        callPaceRepeat = this[K.callPaceRepeat] ?: false,
        callReferences = this[K.callReferences] ?: false,
        ivrHelper = this[K.ivrHelper] ?: false,
    quietWeekMode = this[K.quietWeekMode] ?: false,
        quietWeekRules = this[K.quietWeekRules] ?: "",
        quietHours = this[K.quietHours] ?: false,
        quietStart = this[K.quietStart] ?: 1320,
        quietEnd = this[K.quietEnd] ?: 420,
        quietAllowFav = this[K.quietAllowFav] ?: true,
        blockedPrefixes = this[K.blockedPrefixes]?.split(";")?.filter { it.isNotBlank() } ?: emptyList(),
        callSummary = this[K.callSummary] ?: false,
        keypadLetters = this[K.keypadLetters] ?: true,
        defaultTab = this[K.defaultTab] ?: "recents",
        undoCall = this[K.undoCall] ?: false,
        simMemory = this[K.simMemory] ?: true,
        muteReminder = this[K.muteReminder] ?: false,
        audioRouteAlert = this[K.audioRouteAlert] ?: false,
        minuteBeep = this[K.minuteBeep] ?: 0,
        callVolumeMax = this[K.callVolumeMax] ?: false,
        ringVolumeMax = this[K.ringVolumeMax] ?: false,
        groupByDay = this[K.groupByDay] ?: true,
        showTodayStats = this[K.showTodayStats] ?: false,
        hapticKeys = this[K.hapticKeys] ?: false,
        recordUnknownOnly = this[K.recordUnknownOnly] ?: false,
        exactTime = this[K.exactTime] ?: true,
        autoSpeaker = this[K.autoSpeaker] ?: false,
        confirmBeforeEnd = this[K.confirmBeforeEnd] ?: false,
        recentsShowDuration = this[K.recentsShowDuration] ?: true,
        showSimInRecents = this[K.showSimInRecents] ?: true,
        recentsCompact = this[K.recentsCompact] ?: false,
        rowSwipe = this[K.rowSwipe] ?: false,
        searchBottom = this[K.searchBottom] ?: false,
        recentLimit = (this[K.recentLimit] ?: 300).let { if (it == 100 || it == 300 || it == 500) it else 300 },
        announceOnlyFav = this[K.announceOnlyFav] ?: false,
        flashOnlyFav = this[K.flashOnlyFav] ?: false,
        recentsHideBlocked = this[K.recentsHideBlocked] ?: false,
        volBoost = this[K.volBoost] ?: false,
        volBoostGain = (this[K.volBoostGain] ?: 25).coerceIn(0, 100),
        boostAck = this[K.boostAck] ?: false,
        callPhotoBlur = (this[K.callPhotoBlur] ?: 0).coerceIn(0,2),
        callBgStyle = this[K.callBgStyle] ?: "blobs",
        simUse = simUseMapOf(this),
        appLock = this[K.appLock] ?: false,
        appLockDelaySec = (this[K.appLockDelaySec] ?: 0).coerceIn(0, 300),
        callCardStyle = this[K.callCardStyle] ?: d.callCardStyle,
        recentsStyle = this[K.recentsStyle] ?: d.recentsStyle,
        autoBackup = this[K.autoBackup] ?: false,
        lastAutoBackupAt = this[K.lastAutoBackupAt] ?: 0L,
        lastAutoBackupTry = this[K.lastAutoBackupTry] ?: 0L,
    )
}

class SettingsRepo(private val ctx: Context) {
    val flow: Flow<LdSettings> = ctx.ldStore.data.map { it.toSettings() }

    // Cold-start ke liye app-lock ke 2 fields ka sync mirror - DataStore main thread pe nahi padhna
    private val mirrorSp by lazy { ctx.getSharedPreferences("applock_mirror", Context.MODE_PRIVATE) }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        ctx.ldStore.edit { it[key] = value }
        when (key) {
            K.appLock -> mirrorSp.edit().putBoolean("appLock", value as Boolean).apply()
            K.appLockDelaySec -> mirrorSp.edit().putInt("appLockDelaySec", value as Int).apply()
        }
    }

    /** Bounded cold-start read: sirf 2 primitives, koi DataStore/deserialization nahi. */
    fun lockMirror(): Pair<Boolean, Int> =
        mirrorSp.getBoolean("appLock", false) to mirrorSp.getInt("appLockDelaySec", 0)
    suspend fun resetCallLayout() { ctx.ldStore.edit { it.remove(K.layoutGrid); it.remove(K.layoutBottom); it.remove(K.gridScale); it.remove(K.bottomScale); it.remove(K.callCardScale); it.remove(K.dialerScale); it.remove(K.callAvatarShape); it.remove(K.callShowNumber); it.remove(K.callBgStyle) } }
    suspend fun setReplies(list: List<String>) = set(K.quickReplies, list.joinToString("\n"))
    suspend fun setBlockedPrefixes(list: List<String>) = set(K.blockedPrefixes, list.joinToString(";"))
    suspend fun setSpeedDial(map: Map<Int, String>) = set(K.speedDial, map.entries.joinToString(";") { "${it.key}=${it.value}" })
    suspend fun setRecentsSeen(v: Long) = set(K.recentsSeenAt, v)

    suspend fun simFor(digits: String): String? {
        if (digits.isBlank()) return null
        val raw = ctx.ldStore.data.first()[K.simUse] ?: return null
        return raw.split(";").mapNotNull { val kv = it.split("=", limit = 2); if (kv.size == 2) kv[0] to kv[1] else null }.toMap()[digits]
    }

    // contact-wise last-used SIM ("digits=handleId;...")
    fun simUseMap(p: Preferences): Map<String, String> = (p[K.simUse] ?: "").split(";").mapNotNull {
        val kv = it.split("=", limit = 2); if (kv.size == 2 && kv[0].isNotBlank()) kv[0] to kv[1] else null
    }.toMap()
    suspend fun rememberSim(digits: String, handleId: String) { ctx.ldStore.edit { p ->
        val m = (p[K.simUse] ?: "").split(";").filter { e -> e.isNotBlank() && !e.startsWith("$digits=") }
        p[K.simUse] = (m + "$digits=$handleId").takeLast(80).joinToString(";")
    } }
}
