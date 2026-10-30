package com.mandu.liquiddialer.telecom

/** Quiet hours: sirf silence, kabhi auto-reject/block nahi. Sirf tab jab user ne on kiya ho. */
object QuietHours {
    /** Overnight window (22:00->07:00) bhi handle karta hai. */
    fun inWindow(nowMin: Int, start: Int, end: Int): Boolean =
        if (start <= end) nowMin in start until end else nowMin >= start || nowMin < end

    fun shouldSilence(
        enabled: Boolean, sdkInt: Int, incoming: Boolean,
        nowMin: Int, start: Int, end: Int,
        allowFav: Boolean, isFav: Boolean
    ): Boolean = enabled && sdkInt >= 29 && incoming && inWindow(nowMin, start, end) && !(allowFav && isFav)

    fun fmt(mins: Int): String = "%02d:%02d".format((mins / 60) % 24, mins % 60)
}
