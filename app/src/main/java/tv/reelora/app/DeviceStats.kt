package tv.reelora.app

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal enum class NetworkKind { Offline, Wifi, Ethernet, Other }

// Sizes are rounded before they reach Compose, so an unchanged reading never recomposes.
@Immutable
internal data class DeviceStats(
    val memoryFree: Long = 0,
    val memoryTotal: Long = 0,
    val storageFree: Long = 0,
    val storageTotal: Long = 0,
    val network: NetworkKind = NetworkKind.Other,
    val wifiBars: Int = 3,
    val thermalStatus: Int = PowerManager.THERMAL_STATUS_NONE,
    val cpuUsage: Int? = null,
) {
    val runningHot get() = thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
}

private const val GIB = 1024L * 1024 * 1024

internal fun roundToTenthGib(bytes: Long): Long = (bytes * 10 + GIB / 2) / GIB * GIB / 10

internal fun formatBytes(bytes: Long): String = String.format(Locale.US, "%.1f GB", bytes.toDouble() / GIB)

/** RSSI to 0..3 bars. Devices that do not report signal show as full rather than as weak. */
internal fun wifiBars(rssi: Int): Int = when {
    rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED -> 3
    rssi >= -60 -> 3
    rssi >= -70 -> 2
    rssi >= -80 -> 1
    else -> 0
}

internal fun formatSpeed(bitsPerSecond: Long): String = when {
    bitsPerSecond >= 10_000_000 -> "${bitsPerSecond / 1_000_000} Mb/s"
    bitsPerSecond >= 1_000_000 -> String.format(Locale.US, "%.1f Mb/s", bitsPerSecond / 1_000_000.0)
    else -> "${bitsPerSecond / 1_000} kb/s"
}

internal fun thermalLabel(status: Int): String = when {
    status >= PowerManager.THERMAL_STATUS_SEVERE -> "Hot"
    status >= PowerManager.THERMAL_STATUS_MODERATE -> "Warm"
    else -> "Normal"
}

/** Share of core time spent busy over a window; null when the kernel reported no idle time. */
internal fun cpuUsagePercent(idleMicros: Long, wallMicros: Long, cores: Int): Int? {
    if (idleMicros <= 0L || wallMicros <= 0L || cores <= 0) return null
    return ((1.0 - idleMicros.toDouble() / (wallMicros * cores)) * 100).roundToInt().coerceIn(0, 100)
}

/**
 * Android denies apps /proc/stat, but each core's cpuidle residency stays world-readable.
 * On Fire TV and onn this tracked /proc/stat within about two points.
 */
private class CpuLoadMeter {
    private val cores = File("/sys/devices/system/cpu").listFiles { file -> file.name.matches(Regex("cpu\\d+")) }.orEmpty()
    private val idleTimes = cores.flatMap { core ->
        File(core, "cpuidle").listFiles { file -> file.name.startsWith("state") }.orEmpty().map { File(it, "time") }
    }
    private var lastIdle = -1L
    private var lastWall = 0L

    fun usage(): Int? {
        val idle = runCatching { idleTimes.sumOf { it.readText().trim().toLong() } }.getOrNull() ?: return null
        val wall = SystemClock.elapsedRealtimeNanos() / 1_000
        val result = if (lastIdle < 0) null else cpuUsagePercent(idle - lastIdle, wall - lastWall, cores.size)
        lastIdle = idle
        lastWall = wall
        return result
    }
}

private fun sampleDeviceStats(context: Context, detailed: Boolean, cpu: CpuLoadMeter): DeviceStats {
    val connectivity = context.getSystemService(ConnectivityManager::class.java)
    val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
    val network = when {
        capabilities == null -> NetworkKind.Offline
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkKind.Wifi
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkKind.Ethernet
        else -> NetworkKind.Other
    }
    val base = DeviceStats(
        network = network,
        wifiBars = capabilities?.takeIf { network == NetworkKind.Wifi }?.let { wifiBars(it.signalStrength) } ?: 3,
        thermalStatus = context.getSystemService(PowerManager::class.java).currentThermalStatus,
    )
    if (!detailed) return base
    val memory = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
    val storage = StatFs(Environment.getDataDirectory().path)
    return base.copy(
        memoryFree = roundToTenthGib(memory.availMem),
        memoryTotal = roundToTenthGib(memory.totalMem),
        storageFree = roundToTenthGib(storage.availableBytes),
        storageTotal = roundToTenthGib(storage.totalBytes),
        cpuUsage = cpu.usage(),
    )
}

/**
 * Foreground-only, off the main thread, and paused while [active] is false.
 * The light sample covers network and thermal; [detailed] adds memory and storage.
 */
@Composable
internal fun rememberDeviceStats(active: Boolean, detailed: Boolean): DeviceStats? {
    val context = LocalContext.current
    val foreground = LocalForeground.current
    val stats by produceState<DeviceStats?>(null, context, active && foreground, detailed) {
        if (!active || !foreground) return@produceState
        val cpu = CpuLoadMeter()
        // A short first window, so usage appears almost at once instead of a full period later.
        if (detailed) withContext(Dispatchers.IO) { cpu.usage() }.also { delay(1_000L) }
        while (true) {
            value = withContext(Dispatchers.IO) { runCatching { sampleDeviceStats(context, detailed, cpu) }.getOrNull() } ?: value
            delay(if (detailed) 3_000L else 30_000L)
        }
    }
    return stats
}

/** Device-wide download rate in bits per second; null where the kernel does not report it. */
@Composable
internal fun rememberDownloadSpeed(active: Boolean): Long? {
    val foreground = LocalForeground.current
    val speed by produceState<Long?>(null, active && foreground) {
        if (!active || !foreground) return@produceState
        var previous = TrafficStats.getTotalRxBytes()
        if (previous == TrafficStats.UNSUPPORTED.toLong()) return@produceState
        while (true) {
            delay(2_000L)
            val current = TrafficStats.getTotalRxBytes()
            value = (current - previous).coerceAtLeast(0L) * 8 / 2
            previous = current
        }
    }
    return speed
}

@Immutable internal data class DisplaySummary(val resolution: String, val hdr: String, val refresh: String)

internal fun resolutionLabel(width: Int, height: Int): String = when (maxOf(width, height)) {
    3840, 4096 -> "4K"
    2560 -> "1440p"
    1920 -> "1080p"
    1280 -> "720p"
    else -> "$width × $height"
}

// Fire TV reports every format it supports, so the row names only the best one.
@Suppress("DEPRECATION")
internal fun bestHdrLabel(types: IntArray): String = when {
    Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION in types -> "Dolby Vision"
    Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS in types -> "HDR10+"
    Display.HdrCapabilities.HDR_TYPE_HDR10 in types -> "HDR10"
    Display.HdrCapabilities.HDR_TYPE_HLG in types -> "HLG"
    else -> ""
}

@Composable
@Suppress("DEPRECATION")
internal fun rememberDisplaySummary(): DisplaySummary? {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val display = checkNotNull(context.display)
            val mode = display.mode
            DisplaySummary(
                resolutionLabel(mode.physicalWidth, mode.physicalHeight),
                bestHdrLabel(display.hdrCapabilities?.supportedHdrTypes ?: IntArray(0)),
                "${Math.round(mode.refreshRate)} Hz",
            )
        }.getOrNull()
    }
}
