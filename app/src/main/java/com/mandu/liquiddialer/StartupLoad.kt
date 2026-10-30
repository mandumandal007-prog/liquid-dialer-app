package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.SimInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Carrega as fontes da home EM PARALELO e publica os recents assim que a query
 * deles termina, sem esperar contacts/blocked/sims. Antes disso, tudo rodava em
 * serie num unico bloco e os recents so apareciam depois da carga completa dos
 * contatos - era o delay de 2-3s na abertura do app.
 */
internal suspend fun loadStartupSources(
    contacts: suspend () -> List<Contact>,
    recents: suspend () -> List<Recent>,
    blocked: suspend () -> List<String>,
    sims: suspend () -> List<SimInfo>,
    publishRecents: (List<Recent>) -> Unit,
    publishRest: (List<Contact>, List<String>, List<SimInfo>) -> Unit,
) = coroutineScope {
    val dContacts = async(Dispatchers.IO) { contacts() }
    val dRecents = async(Dispatchers.IO) { recents() }
    val dBlocked = async(Dispatchers.IO) { blocked() }
    val dSims = async(Dispatchers.IO) { sims() }
    publishRecents(dRecents.await())
    publishRest(dContacts.await(), dBlocked.await(), dSims.await())
}
