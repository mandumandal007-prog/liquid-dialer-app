package com.mandu.liquiddialer.telecom

/** Spam series block: user-added prefixes (min 3 digits) se shuru hone wale number reject. Default khali = kuch block nahi. */
object SeriesBlock {
    private fun digits(s: String): String = s.filter { it.isDigit() }

    fun matches(number: String, prefixes: List<String>): Boolean {
        val n = digits(number)
        if (n.isEmpty()) return false
        return prefixes.any { pre ->
            val d = digits(pre)
            if (d.length < 3) false
            else n.startsWith(d) || (d.startsWith("91") && d.length >= 5 && ("91" + n).startsWith(d))
        }
    }
}
