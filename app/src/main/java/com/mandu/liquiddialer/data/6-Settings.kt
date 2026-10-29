package com.mandu.liquiddialer.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.ldStore by preferencesDataStore(name = "ld_settings")

data class LdSettings(
    val theme: String = "midnight",
    val accent: Long = 0xFF5B8CFF,
    val blur: Int = 22,
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
    val onboarded: Boolean = false,
    val incomingFull: Boolean = true,
    val swipeAnswer: Boolean = true,
    val callCardScale: Float = 1f,
    val callBtnSize: Int = 68,
    val dialerScale: Float = 1f,
    val callBtns: String = DEFAULT_CALL_BTNS,
    val profilePhoto: String = "",
)

val DEFAULT_CALL_BTNS = "mute,keypad,record,hold,addcall,audio"

val DEFAULT_REPLIES = listOf(
    "Abhi baat nahi kar sakta, baad mein call karta hoon.",
    "Meeting mein hoon, thodi der mein call karunga.",
    "Gaadi chala raha hoon, baad mein baat karte hain.",
    "Kya hua? Message kar do.",
)

object K {
    val theme = stringPreferencesKey("theme")
    val accent = longPreferencesKey("accent")
    val blur = intPreferencesKey("blur")
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
    val voicemail = stringPreferencesKey("voicemail")
    val quickReplies = stringPreferencesKey("quickReplies")
    val speedDial = stringPreferencesKey("speedDial")
    val onboarded = booleanPreferencesKey("onboarded")
    val incomingFull = booleanPreferencesKey("incomingFull")
    val swipeAnswer = booleanPreferencesKey("swipeAnswer")
    val callCardScale = floatPreferencesKey("callCardScale")
    val callBtnSize = intPreferencesKey("callBtnSize")
    val dialerScale = floatPreferencesKey("dialerScale")
    val callBtns = stringPreferencesKey("callBtns")
    val profilePhoto = stringPreferencesKey("profilePhoto")
}

fun Preferences.toSettings(): LdSettings {
    val d = LdSettings()
    return LdSettings(
        theme = this[K.theme] ?: d.theme,
        accent = this[K.accent] ?: d.accent,
        blur = this[K.blur] ?: d.blur,
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
        voicemail = this[K.voicemail] ?: d.voicemail,
        quickReplies = this[K.quickReplies]?.split("\n")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: d.quickReplies,
        speedDial = this[K.speedDial]?.split(";")?.mapNotNull { s ->
            val p = s.split("=", limit = 2); p.getOrNull(0)?.toIntOrNull()?.let { k -> p.getOrNull(1)?.takeIf { it.isNotBlank() }?.let { k to it } }
        }?.toMap() ?: emptyMap(),
        onboarded = this[K.onboarded] ?: false,
        incomingFull = this[K.incomingFull] ?: true,
        swipeAnswer = this[K.swipeAnswer] ?: true,
        callCardScale = this[K.callCardScale] ?: d.callCardScale,
        callBtnSize = this[K.callBtnSize] ?: d.callBtnSize,
        dialerScale = this[K.dialerScale] ?: d.dialerScale,
        callBtns = this[K.callBtns] ?: d.callBtns,
        profilePhoto = this[K.profilePhoto] ?: d.profilePhoto,
    )
}

class SettingsRepo(private val ctx: Context) {
    val flow: Flow<LdSettings> = ctx.ldStore.data.map { it.toSettings() }
    suspend fun <T> set(key: Preferences.Key<T>, value: T) { ctx.ldStore.edit { it[key] = value } }
    suspend fun setReplies(list: List<String>) = set(K.quickReplies, list.joinToString("\n"))
    suspend fun setSpeedDial(map: Map<Int, String>) = set(K.speedDial, map.entries.joinToString(";") { "${it.key}=${it.value}" })
}
