package tv.reelora.app

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// One listener shared by foreground content; no service or connectivity polling.
@Composable
internal fun rememberInternetAvailable(active: Boolean): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(ConnectivityManager::class.java) }
    // Seed from the current network so returning Home doesn't briefly report offline
    // and restart every network effect.
    val online by produceState(remember(manager) { manager.isValidatedOnline() }, manager, active) {
        value = active && manager.isValidatedOnline()
        if (!active) return@produceState
        val callback = object : ConnectivityManager.NetworkCallback() {
            private var validated = false
            private var blocked = false
            override fun onAvailable(network: Network) {
                validated = false
                blocked = false
                value = false
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                value = validated && !blocked
            }
            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                this.blocked = blocked
                value = validated && !blocked
            }
            override fun onLost(network: Network) { value = false }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitDispose { manager.unregisterNetworkCallback(callback) }
    }
    return online
}

internal fun widgetRefreshDelay(now: Long, deadline: Long, failed: Boolean): Long =
    if (failed) 0L else (deadline - now).coerceAtLeast(0L)

private fun ConnectivityManager.isValidatedOnline(): Boolean =
    getNetworkCapabilities(activeNetwork)?.let {
        it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } == true
