package com.mandu.liquiddialer.telecom

/** TRAI spam series + international pehchan. Pure offline rules - koi network lookup nahi.
 * Screening service (auto-reject, opt-in) aur incoming UI (badges) dono yahi logic use karte hain. */
object SpamGuard {

    fun digits(s: String): String = s.filter { it.isDigit() }

    /** Country/trunk code hatakar national number: +91 / 0091 / 91 / leading 0 sab handle. */
    fun nsn(number: String): String {
        var d = digits(number)
        if (d.startsWith("00")) d = d.drop(2)
        if (d.startsWith("91") && d.length > 10) d = d.drop(2)
        if (d.startsWith("0") && d.length > 10) d = d.drop(1)
        return d
    }

    /** 140 = promotional telemarketing, 160 = service/transactional (TRAI series, regulated entities).
     * Sirf Indian context me apply hota hai: explicit foreign number (+1 404...) pe kabhi nahi.
     * 160 ko kabhi block mat karo - verified service calls ki series hai. */
    fun series(number: String): Int? {
        val raw = number.trim()
        if (raw.startsWith("+") || raw.startsWith("00")) {
            var d = digits(raw)
            if (d.startsWith("00")) d = d.drop(2)
            if (!d.startsWith("91")) return null
        }
        val n = nsn(number)
        return when {
            n.length >= 10 && n.startsWith("140") -> 140
            n.length >= 10 && n.startsWith("160") -> 160
            else -> null
        }
    }

    /** +/00 wala foreign number (India +91/0091 ke alawa). Plain 10-digit local number kabhi intl nahi. */
    fun isInternational(number: String): Boolean {
        val raw = number.trim()
        if (!raw.startsWith("+") && !raw.startsWith("00")) return false
        var d = digits(raw)
        if (d.startsWith("00")) d = d.drop(2)
        return d.isNotEmpty() && !d.startsWith("91")
    }

    // Call.Details.VERIFICATION_STATUS_PASSED/FAILED (API 30+) - int constants inline hote hain, purane Android pe safe
    const val VERIFY_PASSED = 1
    const val VERIFY_FAILED = 2

    /** STIR/SHAKEN network-verification ka badge text; null = koi badge nahi. */
    fun verificationLabel(status: Int): String? = when (status) {
        VERIFY_PASSED -> "Network verified"
        VERIFY_FAILED -> "Number verify nahi hua"
        else -> null
    }

    /** Screening decision: BlockLog reason ya null. `never` (kabhi block mat karo pin) hamesha jeet-ta hai.
     * intlBlock ke liye caller side pe contacts permission ka dhyan rakhna zaroori (known=false tab). */
    fun rejectReason(number: String, known: Boolean, never: Boolean, promoBlock: Boolean, intlBlock: Boolean): String? {
        if (never || number.isBlank()) return null
        if (promoBlock && series(number) == 140) return com.mandu.liquiddialer.data.BlockLog.REASON_PROMO
        if (intlBlock && !known && isInternational(number)) return com.mandu.liquiddialer.data.BlockLog.REASON_INTL
        return null
    }
}
