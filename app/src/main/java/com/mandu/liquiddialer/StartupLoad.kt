package com.mandu.liquiddialer

import com.mandu.liquiddialer.data.Contact
import com.mandu.liquiddialer.data.Recent
import com.mandu.liquiddialer.data.SimInfo
import kotlinx.coroutines.CoroutineDispatcher
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
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) = coroutineScope {
    val dContacts = async(ioDispatcher) { contacts() }
    val dRecents = async(ioDispatcher) { recents() }
    val dBlocked = async(ioDispatcher) { blocked() }
    val dSims = async(ioDispatcher) { sims() }
    publishRecents(dRecents.await())
    publishRest(dContacts.await(), dBlocked.await(), dSims.await())
}

/** Data and load outcome share the same generation gate, on the caller's UI context. */
internal fun publishRecentLoad(state: AppState, rows: List<Recent>, failed: Boolean, generation: Int, currentGeneration: Int) {
    if (generation != currentGeneration) return
    state.recents = rows
    state.recentsError = failed
    state.recentsLoaded = true
}
