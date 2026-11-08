package com.mandu.liquiddialer.data

/** Immutable per-contact-snapshot search index. No provider calls or persistent cache.
 * Rebuilt when contacts change, so edits and permission-driven clearing take effect immediately. */
class ContactSearch(contacts: List<Contact>) {
    private data class T9(val words:List<String>,val joined:String,val initials:String)
    internal var t9Builds = 0
        private set
    private inner class Entry(val contact: Contact) {
        val name by lazy(LazyThreadSafetyMode.NONE) { contact.name.lowercase() }
        val altName by lazy(LazyThreadSafetyMode.NONE) { contact.altName.lowercase() }
        val number by lazy(LazyThreadSafetyMode.NONE) { SearchQuery.digits(contact.number) }
        val t9 by lazy(LazyThreadSafetyMode.NONE) {
            t9Builds++
            val words=contact.name.split(" ","-",".").filter { it.isNotBlank() }
            T9(words.map(Repo::t9Of),Repo.t9Of(contact.name.replace(" ","")),words.joinToString("") { Repo.t9Of(it.take(1)) })
        }
    }
    private val entries=contacts.map(::Entry)
    internal var lastExamined: Int=0
        private set
    fun find(query: String, limit: Int=Int.MAX_VALUE): List<Contact> {
        lastExamined=0
        if(limit<=0) return emptyList()
        if(query.isBlank()) return entries.asSequence().take(limit).map { it.contact }.toList()
        val q=query.trim().lowercase()
        val digits=SearchQuery.number(q)
        val out=ArrayList<Contact>()
        for(e in entries) {
            lastExamined++
            if(e.name.contains(q) || e.altName.contains(q) || digits != null && (e.number.contains(digits) || e.t9.let { t -> t.words.any { it.startsWith(digits) } || t.joined.contains(digits) || t.initials.startsWith(digits) })) {
                out+=e.contact
                if(out.size==limit) break
            }
        }
        return out
    }
}
