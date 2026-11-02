package com.mandu.liquiddialer.data

/** Immutable per-contact-snapshot search index. No provider calls or persistent cache.
 * Rebuilt when contacts change, so edits and permission-driven clearing take effect immediately. */
class ContactSearch(contacts: List<Contact>) {
    private data class Tokens(val name:String,val number:String,val words:List<String>,val joined:String,val initials:String)
    private class Entry(val contact: Contact) {
        val tokens by lazy(LazyThreadSafetyMode.NONE) {
            val words=contact.name.split(" ","-",".").filter { it.isNotBlank() }
            Tokens(contact.name.lowercase(),contact.number.filter { it.isDigit() },words.map(Repo::t9Of),Repo.t9Of(contact.name.replace(" ","")),words.joinToString("") { Repo.t9Of(it.take(1)) })
        }
    }
    private val entries=contacts.map(::Entry)
    internal var lastExamined: Int=0
        private set
    fun find(query: String, limit: Int=Int.MAX_VALUE): List<Contact> {
        lastExamined=0
        if(limit<=0) return emptyList()
        if(query.isBlank()) return entries.asSequence().take(limit).map { it.contact }.toList()
        val blank=query.isBlank()
        val q=query.trim().lowercase()
        val digits=q.filter { it.isDigit() }
        val numeric=digits.isNotEmpty() && digits.length==q.replace(" ","").replace("+","").length
        val out=ArrayList<Contact>()
        for(e in entries) {
            lastExamined++
            val t=e.tokens
            if(blank || t.name.contains(q) || numeric && (t.number.contains(digits) || t.words.any { it.startsWith(digits) } || t.joined.contains(digits) || t.initials.startsWith(digits))) {
                out+=e.contact
                if(out.size==limit) break
            }
        }
        return out
    }
}
