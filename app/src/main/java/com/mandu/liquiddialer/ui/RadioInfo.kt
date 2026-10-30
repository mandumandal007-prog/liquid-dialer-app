package com.mandu.liquiddialer.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class CellRow(val type: String, val ageSec: Long, val fields: List<Pair<String, String>>)

/**
 * Read-only cell info from TelephonyManager.getAllCellInfo. One-shot read on open plus an explicit
 * Refresh button - no background watch. Android can return cached/rate-limited measurements, so every
 * card carries its measurement age, and unsupported values render as "unavailable" (never invented).
 * Field API levels verified against the SDK api-versions database: NR classes 29+, getBands() 30+,
 * getTimestampMillis() 30+.
 */
object RadioInfo {
    // unavailable signal fields come back as Int.MAX_VALUE on every supported API level
    private fun Int.disp(unit: String = "") = if (this == Int.MAX_VALUE) "unavailable" else if (unit.isEmpty()) "$this" else "$this $unit"

    fun ageSec(info: CellInfo): Long = if (Build.VERSION.SDK_INT >= 30)
        ((SystemClock.elapsedRealtime() - info.timestampMillis) / 1000L).coerceAtLeast(0L)
    else @Suppress("DEPRECATION") ((SystemClock.elapsedRealtimeNanos() - info.timeStamp) / 1_000_000_000L).coerceAtLeast(0L)

    fun readCells(ctx: Context): List<CellRow> {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return emptyList()
        val infos = try { tm.allCellInfo } catch (e: SecurityException) { null } ?: return emptyList()
        return infos.filter { it.isRegistered }.map { toRow(it) }
    }

    private fun bands(b: IntArray?) = if (b == null || b.isEmpty()) "unavailable" else b.joinToString(", ")

    private fun toRow(info: CellInfo): CellRow {
        val age = ageSec(info)
        return when {
            Build.VERSION.SDK_INT >= 29 && info is CellInfoNr -> {
                val id = info.cellIdentity as? CellIdentityNr
                val sig = info.cellSignalStrength as? CellSignalStrengthNr
                CellRow("5G (NR)", age, listOfNotNull(
                    if (Build.VERSION.SDK_INT >= 30) "Band" to bands(id?.bands) else null,
                    id?.let { "MCC / MNC" to "${it.mccString ?: "?"} / ${it.mncString ?: "?"}" },
                    id?.let { "TAC / PCI" to "${it.tac.disp()} / ${it.pci.disp()}" },
                    sig?.let { "SS-RSRP" to it.ssRsrp.disp("dBm") },
                    sig?.let { "SS-RSRQ" to it.ssRsrq.disp("dB") },
                    sig?.let { "SS-SINR" to it.ssSinr.disp("dB") },
                    sig?.let { "Level" to "${it.level} / 4" },
                ).ifEmpty { listOf("Info" to "unavailable") })
            }
            info is CellInfoLte -> {
                val id = info.cellIdentity
                val sig = info.cellSignalStrength
                CellRow("LTE", age, listOfNotNull(
                    if (Build.VERSION.SDK_INT >= 30) "Band" to bands(id.bands) else null,
                    "MCC / MNC" to "${id.mccString ?: "?"} / ${id.mncString ?: "?"}",
                    "TAC / CID / PCI" to "${id.tac.disp()} / ${id.ci.disp()} / ${id.pci.disp()}",
                    "RSRP" to sig.rsrp.disp("dBm"),
                    "RSRQ" to sig.rsrq.disp("dB"),
                    "RSSNR" to sig.rssnr.disp("dB"),
                    "Level" to "${sig.level} / 4",
                ))
            }
            info is CellInfoWcdma -> CellRow("WCDMA (3G)", age, listOf(
                "MCC / MNC" to "${info.cellIdentity.mccString ?: "?"} / ${info.cellIdentity.mncString ?: "?"}",
                "LAC / CID" to "${info.cellIdentity.lac.disp()} / ${info.cellIdentity.cid.disp()}",
                "Signal" to info.cellSignalStrength.dbm.disp("dBm"),
            ))
            info is CellInfoGsm -> CellRow("GSM (2G)", age, listOf(
                "MCC / MNC" to "${info.cellIdentity.mccString ?: "?"} / ${info.cellIdentity.mncString ?: "?"}",
                "LAC / CID" to "${info.cellIdentity.lac.disp()} / ${info.cellIdentity.cid.disp()}",
                "Signal" to info.cellSignalStrength.dbm.disp("dBm"),
            ))
            else -> CellRow(info.javaClass.simpleName.removePrefix("CellInfo"), age, listOf("Info" to "unavailable"))
        }
    }
}

@Composable
fun RadioInfoPage(onBack: () -> Unit) {
    val p = LocalLd.current
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var cells by remember { mutableStateOf<List<CellRow>>(emptyList()) }
    LaunchedEffect(granted) { if (granted) cells = RadioInfo.readCells(ctx) }
    PageFrame("Radio info", onBack = onBack) {
        Column(Modifier.fillMaxWidth().testTag("radio_screen"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LdText("Read-only cell info (getAllCellInfo). Kuch fields is device par 'unavailable' ho sakte hain. Android kabhi-kabhi cached/rate-limited data deta hai, isliye har card par measurement ki age likhi hai.", 12f, color = p.muted, maxLines = 4)
            if (!granted) {
                Column(Modifier.fillMaxWidth().glass(p, RoundedCornerShape(18.dp)).background(p.accent2.pct(.08f)).padding(16.dp).testTag("radio_perm_needed"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LdText("Location permission chahiye", 15f, FontWeight.Bold)
                    LdText("Cell tower ki jaankari ke liye Android precise location permission maangta hai. Sirf is screen ke liye use hogi; koi background tracking nahi.", 12.5f, color = p.muted, maxLines = 4)
                    CodeAction("Permission do", "radio_perm_btn") { ask.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    LdText("${cells.size} registered cell${if (cells.size == 1) "" else "s"}", 13f, FontWeight.SemiBold, p.accent)
                    CodeAction("Refresh", "radio_refresh") { cells = RadioInfo.readCells(ctx) }
                }
                if (cells.isEmpty()) LdText("Koi registered cell nahi mila", 13f, color = p.muted, modifier = Modifier.testTag("radio_empty"), maxLines = 2)
                cells.forEach { c ->
                    Column(Modifier.fillMaxWidth().glass(p, RoundedCornerShape(18.dp)).background(p.accent.pct(.07f)).padding(14.dp).testTag("radio_cell"), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            LdText(c.type, 14.5f, FontWeight.Bold, p.accent)
                            LdText(if (c.ageSec < 2) "abhi-abhi" else "${c.ageSec}s purana", 11.5f, color = p.muted, modifier = Modifier.testTag("radio_age"))
                        }
                        c.fields.forEach { (k, v) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                LdText(k, 12.5f, color = p.muted, maxLines = 2)
                                LdText(v, 12.5f, FontWeight.SemiBold, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}
