package com.mandu.liquiddialer.data

import android.provider.CallLog.Calls

/** Screen-local index. Rebuilt when the source list changes; no static cache or retained history. */
class RecentSearch(private val source: List<Recent>) {
    private val digits = source.map { it.number.filter(Char::isDigit) }
    internal val normalizedCharacterCount: Int get() = digits.sumOf { it.length }
    fun find(query: String, filter: Int = 0, hideBlocked: Boolean = false): List<Recent> {
        val blank = query.isBlank()
        val needle = query.filter(Char::isDigit).ifEmpty { "\u0000" }
        return source.filterIndexed { i, r ->
            val typeMatches = when (filter) {
                1 -> r.type == Calls.MISSED_TYPE || r.type == Calls.REJECTED_TYPE || r.type == Calls.BLOCKED_TYPE
                2 -> r.type == Calls.OUTGOING_TYPE
                3 -> r.type == Calls.INCOMING_TYPE || r.type == Calls.VOICEMAIL_TYPE
                else -> true
            }
            typeMatches && (!hideBlocked || r.type != Calls.BLOCKED_TYPE) &&
                (blank || (r.name ?: "").contains(query, true) || digits[i].contains(needle))
        }
    }
}
