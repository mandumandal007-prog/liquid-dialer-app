package com.mandu.liquiddialer.telecom

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.mandu.liquiddialer.data.Repo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Call recording. Android 10+ does not let normal apps capture the downlink (other person's voice):
 * VOICE_CALL needs a system-only permission, so we try VOICE_CALL -> VOICE_COMMUNICATION -> MIC.
 * With MIC, the other side is heard only if the speaker is on.
 */
object CallRecorder {
    private var rec: MediaRecorder? = null

    fun start(ctx: Context, number: String, name: String): Boolean {
        if (rec != null) return true
        val safe = (name.ifBlank { number }).replace(Regex("[^A-Za-z0-9+]+"), "_").take(40)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_CALL to "VOICE_CALL",
            MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION",
            MediaRecorder.AudioSource.MIC to "MIC",
        )
        for ((src, label) in sources) {
            val f = File(Repo.recordingsDir(ctx), "Call_${safe}_$stamp.m4a")
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()
            try {
                r.setAudioSource(src)
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioEncodingBitRate(64000)
                r.setAudioSamplingRate(44100)
                r.setOutputFile(f.absolutePath)
                r.prepare()
                r.start()
                rec = r
                CallManager.recording.value = CallManager.RecInfo(f, System.currentTimeMillis(), label)
                return true
            } catch (e: Exception) {
                try { r.reset(); r.release() } catch (_: Exception) { }
                f.delete()
            }
        }
        return false
    }

    fun stop() {
        val r = rec ?: return
        try { r.stop() } catch (_: Exception) { CallManager.recording.value?.file?.delete() }
        try { r.release() } catch (_: Exception) { }
        rec = null
        CallManager.recording.value = null
    }

    fun isRecording() = rec != null
}
