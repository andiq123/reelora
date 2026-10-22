package tv.reelora.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleTheaterTest {
    @Test fun remoteInputCancelsLookupBeforeStartingAFreshIdlePeriod() = runBlocking {
        withTimeout(2_000) {
            val input = Channel<Unit>(Channel.CONFLATED)
            val started = CompletableDeferred<Unit>()
            var cancelled = false
            var attempts = 0
            val result = async {
                awaitIdleResult(input, 1) {
                    attempts++
                    if (attempts == 1) {
                        started.complete(Unit)
                        try { awaitCancellation() } finally { cancelled = true }
                    } else "fresh trailer"
                }
            }
            started.await()
            input.send(Unit)
            assertEquals("fresh trailer", result.await())
            assertTrue(cancelled)
            assertEquals(2, attempts)
        }
    }

    @Test fun unavailableTrailerRetriesAfterAnotherIdlePeriod() = runBlocking {
        withTimeout(2_000) {
            var attempts = 0
            val result = awaitIdleResult(Channel<Unit>(Channel.CONFLATED), 1) {
                if (++attempts == 1) null else "available"
            }
            assertEquals("available", result)
            assertEquals(2, attempts)
        }
    }

    @Test fun leavingHomeCancelsPendingLookup() = runBlocking {
        withTimeout(2_000) {
            val started = CompletableDeferred<Unit>()
            var cancelled = false
            val result = async {
                awaitIdleResult(Channel<Unit>(Channel.CONFLATED), 1) {
                    started.complete(Unit)
                    try { awaitCancellation() } finally { cancelled = true }
                }
            }
            started.await()
            result.cancel()
            result.join()
            assertTrue(cancelled)
        }
    }
}
