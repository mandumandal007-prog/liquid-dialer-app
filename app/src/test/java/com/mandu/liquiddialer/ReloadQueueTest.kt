package com.mandu.liquiddialer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReloadQueueTest {
    @Test fun burstRunsOneBatchAndOneLatestFollowUpWithoutOverlap()=runTest {
        val gate=CompletableDeferred<Unit>()
        val runs=mutableListOf<Int>();var active=0;var peak=0
        val q=ReloadQueue(this){g->runs+=g;active++;peak=maxOf(peak,active);if(runs.size==1)gate.await();delay(10);active--}
        q.request();runCurrent()
        repeat(100){q.request()};runCurrent()
        assertEquals(listOf(1),runs);assertEquals(101,q.generation)
        gate.complete(Unit);testScheduler.advanceUntilIdle()
        assertEquals(listOf(1,101),runs);assertEquals(1,peak)
        println("PERF reload burst: 101 requests -> ${runs.size} batches, peak active=$peak")
    }
    @Test fun idleReloadStartsImmediatelyAndLaterRequestStillRuns()=runTest {
        val runs=mutableListOf<Int>();val q=ReloadQueue(this){runs+=it}
        q.request();runCurrent();assertEquals(listOf(1),runs)
        q.request();runCurrent();assertEquals(listOf(1,2),runs)
    }
    @Test fun slowContactsCannotHoldNewRecentsAndStalePublicationIsSkipped()=runTest {
        val gate=CompletableDeferred<Unit>();val published=mutableListOf<Int>()
        lateinit var recents:ReloadQueue
        val contacts=ReloadQueue(this){gate.await()}
        recents=ReloadQueue(this){g->delay(5);if(g==recents.generation)published+=g}
        contacts.request();recents.request();runCurrent()
        repeat(10){contacts.request();recents.request()}
        testScheduler.advanceTimeBy(20);runCurrent()
        assertEquals(listOf(11),published)
        assertFalse(gate.isCompleted)
        gate.complete(Unit);testScheduler.advanceUntilIdle()
    }
}
