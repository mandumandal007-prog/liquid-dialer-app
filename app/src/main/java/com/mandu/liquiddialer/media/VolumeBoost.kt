package com.mandu.liquiddialer.media

/**
 * Honest GOODEV-style media boost math.
 * Android lets apps amplify audio only through effects on MEDIA sessions; there is no
 * public API that amplifies SIM/voice-call audio. So the boost maps 0..100% onto
 * LoudnessEnhancer's target gain (millibels), hard-capped for speaker/hearing safety.
 */
object VolumeBoost {
    const val MAX_GAIN_MB = 1500 // gain is hard-capped; cap hona "safe" ka praman NAHI hai - risk warning UI me hai

    fun gainToMb(percent: Int): Int = (percent.coerceIn(0, 100) * MAX_GAIN_MB) / 100

    fun dbLabel(percent: Int): String = "+${gainToMb(percent) / 100} dB"
}
