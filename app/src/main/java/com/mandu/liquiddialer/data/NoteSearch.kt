package com.mandu.liquiddialer.data

/** Local-only filtering. Keeps note order and never changes stored text or IDs. */
object NoteSearch {
    fun find(notes: List<Note>, query: String): List<Note> {
        val q = query.trim()
        if (q.isEmpty()) return notes
        val digits = q.filter { it.isDigit() }
        val isNumber = digits.isNotEmpty() && q.all { it.isDigit() || it in "+()- ." }
        return notes.filter { n ->
            n.text.contains(q, ignoreCase = true) || n.name.contains(q, ignoreCase = true) ||
                n.number.contains(q, ignoreCase = true) ||
                (isNumber && n.number.filter { it.isDigit() }.contains(digits))
        }
    }
}
