package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.Recent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prova do fix de startup: com contacts/blocked/sims LENTOS, os recents sao
 * publicados ASSIM QUE a query deles termina, sem esperar as fontes lentas.
 * (No codigo antigo, tudo rodava em serie e os recents so saiam no fim.)
 */
class StartupLoadTest {

    private fun r(id: Long) = Recent(id, "+91 90000 0000$id", "Zed $id", 1, 1000L, 0L, null, null)
    private fun c(id: Long) = Contact(id, "", "Ana $id", "", "+91 80000 0000$id", "Mobile", false, null)

    @Test
    fun recentsPublicadosAntesDeFontesLentas() = runBlocking {
        val events = mutableListOf<String>()
        loadStartupSources(
            contacts = { delay(400); events += "contacts-done"; listOf(c(1)) },
            recents = { delay(50); listOf(r(1), r(2)) },
            blocked = { delay(400); events += "blocked-done"; listOf("+91 1") },
            sims = { delay(400); events += "sims-done"; emptyList() },
            publishRecents = { events += "recents-published" },
            publishRest = { _, _, _ -> events += "rest-published" },
        )
        // recents publicados PRIMEIRO, antes de qualquer fonte lenta terminar
        assertEquals("recents-published", events.first())
        assertTrue(events.indexOf("recents-published") < events.indexOf("contacts-done"))
        assertTrue(events.indexOf("recents-published") < events.indexOf("blocked-done"))
        assertTrue(events.indexOf("recents-published") < events.indexOf("sims-done"))
        // e o restante publicado por ultimo, com tudo completo
        assertEquals("rest-published", events.last())
    }

    @Test
    fun todosOsResultadosEntregues() = runBlocking {
        var gotRecents: List<Recent>? = null
        var gotContacts: List<Contact>? = null
        var gotBlocked: List<String>? = null
        var gotSims: Int? = null
        loadStartupSources(
            contacts = { delay(200); listOf(c(1), c(2)) },
            recents = { delay(10); listOf(r(1)) },
            blocked = { delay(200); listOf("+91 1", "+91 2") },
            sims = { delay(200); emptyList() },
            publishRecents = { gotRecents = it },
            publishRest = { c, b, s -> gotContacts = c; gotBlocked = b; gotSims = s.size },
        )
        assertEquals(1, gotRecents!!.size)
        assertEquals(2, gotContacts!!.size)
        assertEquals(listOf("+91 1", "+91 2"), gotBlocked)
        assertEquals(0, gotSims)
    }
}
