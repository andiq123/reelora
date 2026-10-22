package tv.reelora.app

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull

// Remote input cancels a pending lookup and starts a fresh idle period.
internal suspend fun <T : Any> awaitIdleResult(
    input: ReceiveChannel<Unit>,
    idleMillis: Long,
    load: suspend () -> T?,
): T {
    while (true) {
        while (withTimeoutOrNull(idleMillis) { input.receive() } != null) {}
        val result = coroutineScope {
            val pending = async { load() }
            try {
                select {
                    input.onReceive { null }
                    pending.onAwait { it }
                }
            } finally {
                pending.cancel()
            }
        }
        if (result != null) return result
    }
}
