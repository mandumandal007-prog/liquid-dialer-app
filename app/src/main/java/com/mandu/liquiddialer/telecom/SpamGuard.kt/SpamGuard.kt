package com.mandu.liquiddialer.telecom

/** TRAI spam series + international pehchan. Pure offline rules - koi network lookup nahi.
 * Screening service (auto-reject, opt-in) aur incoming UI (badges) dono yahi logic use karte hain. */
object SpamGuard {

    fun digits(s: String): String = s.filter { it.isDigit() }

    /** Indian 10-digit national number, ya null agar number is format me pehchana nahi gaya.
     * Sirf ye shapes: +91/0091 + 10 digit, 91 + 10 digit (12), 0 + 10 digit (11), seedha 10 digit.
     * Koi bhi aur lambai ya +/00 ke saath non-91 country code -> null (foreign/ajeeb numbers kabhi Indian nahi maane jaate). */
    fun nsn(number: String): String? {
        val raw = number.trim()
        var d = digits(raw)
        if (d.isEmpty()) return null
        val explicitIntl = raw.startsWith("+") || raw.startsWith("00")
        if (raw.startsWith("00")) d = d.drop(2)
        if (explicitIntl) return if (d.length == 12 && d.startsWith("91")) d.drop(2) else null
        return when {
            d.length == 10 -> d
            d.length == 11 && d.startsWith("0") -> d.drop(1)
            d.length == 12 && d.startsWith("91") -> d.drop(2)
            else -> null
        }
    }

    /** 140 = promotional telemarketing, 160 = service/transactional series (TRAI numbering, sirf PREFIX).
     * Ye caller ki verification NAHI hai - koi bhi is prefix ka dawa nahi kar sakta ye hum jaanch nahi sakte.
     * Sirf 10-digit Indian national number pe lagta hai; foreign (+1 404...) ya ajeeb lambai pe kabhi nahi.
     * 160 ko kabhi block mat karo. */
    fun series(number: String): Int? {
        val n = nsn(number) ?: return null
        return when {
            n.startsWith("140") -> 140
            n.startsWith("160") -> 160
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
