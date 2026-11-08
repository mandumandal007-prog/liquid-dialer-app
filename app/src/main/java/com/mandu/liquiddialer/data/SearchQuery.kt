package com.mandu.liquiddialer.data

/** Local display search only. Never used for dialing, identity, blocking or contact merging. */
internal object SearchQuery {
    fun digits(value: String): String = value.mapNotNull { ch ->
        Character.digit(ch, 10).takeIf { it >= 0 }?.let { ('0'.code + it).toChar() }
    }.joinToString("")

    /** Formatting is allowed, but mixed prose, extensions and service codes are not numbers. */
    fun number(value: String): String? {
        val q = value.trim()
        if (q.isEmpty() || q.any { !it.isDigit() && !it.isWhitespace() && it !in "+-()." }) return null
        if (q.count { it == '+' } > 1 || ('+' in q && q.first() != '+')) return null
        return digits(q).takeIf { it.isNotEmpty() }
    }
}
