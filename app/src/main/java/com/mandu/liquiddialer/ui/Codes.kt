package com.mandu.liquiddialer.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Secret/OEM dialer codes. Detection is display-only: nothing runs while typing.
 * The only execution path is the explicit "Request bhejo" tap, which calls
 * TelephonyManager.sendDialerSpecialCode (public API, since 26 == minSdk, default-dialer role required).
 * OEM-style codes (*#899#, *#800#) have no verified third-party route, so they get an honest note and no action.
 */
object DialerCodes {
    sealed interface Detected {
        data object Imei : Detected
        data class Secret(val code: String) : Detected
        data class Oem(val raw: String) : Detected
    }

    fun normalize(dial: String): String = dial.filter { it.isDigit() || it in "*#" }

    fun detect(dial: String): Detected? {
        val d = normalize(dial)
        if (d == "*#06#") return Detected.Imei
        // Android secret code format: *#*#<digits>#*#* (e.g. *#*#4636#*#*)
        if (d.length > 9 && d.startsWith("*#*#") && d.endsWith("#*#*")) {
            val code = d.substring(4, d.length - 4)
            if (code.isNotEmpty() && code.all { it.isDigit() }) return Detected.Secret(code)
            return null
        }
        // OEM-style: *#<digits># (e.g. *#899#, *#800#) - no verified third-party route
        if (d.length >= 5 && d.startsWith("*#") && d.endsWith("#") && !d.startsWith("*#*#")) {
            val mid = d.substring(2, d.length - 1)
            if (mid.length >= 2 && mid.all { it.isDigit() }) return Detected.Oem(d)
        }
        return null
    }

    /** null = default-dialer role missing (caller should ask for it), otherwise a user-visible status */
    fun sendSecret(ctx: Context, code: String): String? {
        val tm = ctx.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (tm?.defaultDialerPackage != ctx.packageName) return null
        return try {
            (ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).sendDialerSpecialCode(code)
            "Request bhej di gayi. Kuch na khule to is phone par ye code support nahi hota."
        } catch (e: SecurityException) {
            "Permission nahi mili - app ko default phone app hona chahiye"
        }
    }

    fun openImeiSettings(ctx: Context): Boolean = runCatching {
        ctx.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}

@Composable
fun DialerCodeCard(det: DialerCodes.Detected, isDefault: Boolean, onRequestRole: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    var status by remember(det) { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp)
            .glass(p, RoundedCornerShape(18.dp)).background(p.accent2.pct(.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp).testTag("code_card"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (det) {
            is DialerCodes.Detected.Imei -> {
                LdText("IMEI check (*#06#)", 14.5f, FontWeight.Bold, maxLines = 2)
                LdText("IMEI system settings me dikhta hai - wahan khol deta hoon", 12.5f, color = p.muted, maxLines = 3)
                CodeAction("Settings kholo", "code_action") {
                    if (!DialerCodes.openImeiSettings(ctx)) status = "Settings nahi khul payi"
                }
            }
            is DialerCodes.Detected.Secret -> {
                LdText("Secret code pakda: *#*#${det.code}#*#*", 14.5f, FontWeight.Bold, maxLines = 2)
                LdText("Ye testing-menu ka code hai. Request bhej dunga; menu khule ya na khule phone par nirbhar hai.", 12.5f, color = p.muted, maxLines = 4)
                CodeAction("Request bhejo", "code_action") {
                    if (!isDefault) onRequestRole()
                    else DialerCodes.sendSecret(ctx, det.code)?.let { status = it }
                }
            }
            is DialerCodes.Detected.Oem -> {
                LdText("OEM code: ${det.raw}", 14.5f, FontWeight.Bold, maxLines = 2)
                LdText("Jaise *#899# / *#800# - inke liye third-party dialer se koi verified raasta nahi hai, isliye hum request nahi bhejte. Ye sirf phone ke apne dialer se khulte hain.", 12.5f, color = p.muted, maxLines = 5)
            }
        }
        status?.let { LdText(it, 12f, FontWeight.SemiBold, p.accent, modifier = Modifier.testTag("code_status"), maxLines = 3) }
    }
}

@Composable
internal fun CodeAction(label: String, tag: String, onClick: () -> Unit) {
    val p = LocalLd.current
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(p.accent.pct(.18f)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 7.dp).testTag(tag)) {
        LdText(label, 12.5f, FontWeight.Bold, p.accent)
    }
}
