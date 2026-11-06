package com.mandu.liquiddialer.data

/** wa.me link banane ka ek hi niyam (Sheets + incoming pill). Offline, koi network nahi.
 * wa.me ye NAHI batata ki number WhatsApp pe hai ya nahi - sirf chat kholne ki koshish hoti hai.
 * Pehchana na gaya / chhota / lamba number -> null (link kabhi galat country ke saath nahi banta). */
object WhatsAppLink {
    /** Sirf digits (country code ke saath, bina +) ya null. */
    fun digits(number: String): String? {
        val raw = number.trim()
        var d = raw.filter { it.isDigit() }
        if (d.isEmpty()) return null
        if (raw.startsWith("+") || raw.startsWith("00")) {
            if (raw.startsWith("00")) d = d.drop(2)
            return if (d.length in 8..15 && !d.startsWith("0")) d else null
        }
        return when {
            d.length == 10 && d[0] in '6'..'9' -> "91$d"      // Indian mobile (user ka default desh)
            d.length == 11 && d.startsWith("0") && d[1] in '6'..'9' -> "91" + d.drop(1)
            d.length == 12 && d.startsWith("91") && d[2] in '6'..'9' -> d
            else -> null
        }
    }

    fun url(number: String): String? = digits(number)?.let { "https://wa.me/$it" }
}
