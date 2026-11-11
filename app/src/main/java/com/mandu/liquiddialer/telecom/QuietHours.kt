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

    fun forSettings(s:com.mandu.liquiddialer.data.LdSettings,sdkInt:Int,incoming:Boolean,day:Int,nowMin:Int,isFav:Boolean,blocked:Boolean=false,favouriteUnchecked:Boolean=false,contactBypass:Boolean=false):Boolean{
        if(blocked||favouriteUnchecked||contactBypass||!s.quietHours||sdkInt<29||!incoming||s.quietAllowFav&&isFav)return false
        return if(s.quietWeekMode)com.mandu.liquiddialer.data.QuietWeek.active(s.quietWeekRules,day,nowMin)else inWindow(nowMin,s.quietStart,s.quietEnd)
    }
    fun fmt(mins: Int): String = "%02d:%02d".format((mins / 60) % 24, mins % 60)
}
