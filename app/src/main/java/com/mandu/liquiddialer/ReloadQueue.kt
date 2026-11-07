package com.mandu.liquiddialer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Main-thread owned. One provider batch at a time, with at most one newest pending reload.
 * A new request invalidates stale publication immediately, without spawning overlapping queries. */
internal class ReloadQueue(private val scope: CoroutineScope, private val load: suspend (Int) -> Unit) {
    var generation = 0
        private set
    private var running = false
    fun request() {
        generation++
        if (running) return
        running = true
        scope.launch {
            try {
                do {
                    val current = generation
                    load(current)
                } while (current != generation)
            } finally { running = false }
        }
    }
}
