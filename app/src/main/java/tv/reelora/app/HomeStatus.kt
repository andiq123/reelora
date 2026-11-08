package tv.reelora.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val StatusSecondary = Color.White.copy(alpha = .78f)

internal fun formatHomeDate(date: LocalDate, romanian: Boolean): String =
    date.format(DateTimeFormatter.ofPattern(if (romanian) "EEE, d MMM" else "EEE d MMM",
        if (romanian) Locale.forLanguageTag("ro") else Locale.ENGLISH)).replaceFirstChar(Char::uppercase)

/** Clock, then date and weather, then opt-in device stats. Problems surface even when stats are off. */
@Composable
internal fun HomeStatus(
    weather: WeatherNow?,
    weatherState: WeatherLoadState,
    use24HourClock: Boolean,
    modifier: Modifier = Modifier,
    deviceStatsEnabled: Boolean = LocalDeviceStatsEnabled.current,
    paused: Boolean = false,
    previewStats: DeviceStats? = null,
) {
    val now = rememberSystemTime()
    val romanian = LocalRomanian.current
    val stats = previewStats ?: rememberDeviceStats(active = !paused, detailed = deviceStatsEnabled)
    val date = remember(now.toLocalDate(), romanian) { formatHomeDate(now.toLocalDate(), romanian) }
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        HomeClock(now.toLocalTime(), use24HourClock)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            RollingText(date, TvType.Footnote.copy(fontWeight = FontWeight.Medium).overArtwork(), color = Color.White)
            AnimatedContent(
                targetState = weather?.let { weatherKind(it.code, it.isDay) },
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(100)) },
                label = "Weather icon",
            ) { kind ->
                if (kind == null) Icon(RefreshIcon, contentDescription = tr("Loading weather"), tint = Accent, modifier = Modifier.size(20.dp))
                else Icon(painterResource(kind.icon), contentDescription = tr(kind.label), tint = Color(kind.color), modifier = Modifier.size(20.dp))
            }
            HomeTemperature(weather?.temperature, TvType.Footnote.overArtwork().copy(color = Color.White, fontWeight = FontWeight.Medium))
            val high = weather?.high
            val low = weather?.low
            if (high != null && low != null) RollingText("$high° / $low°", TvType.Footnote.overArtwork(), color = StatusSecondary)
        }
        val connectionProblem = stats != null && (stats.network == NetworkKind.Offline || (stats.network == NetworkKind.Wifi && stats.wifiBars == 0))
        if (stats != null && (deviceStatsEnabled || connectionProblem)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (deviceStatsEnabled) {
                    stats.cpuUsage?.let { StatusReading(CpuIcon, "CPU in use", it.toString(), "%") }
                    if (stats.memoryTotal > 0) StatusReading(MemoryIcon, "Free memory", formatBytes(stats.memoryFree).substringBefore(' '), " GB")
                }
                if (connectionProblem) NetworkGlyph(stats, WarmAccent)
            }
        }
        if (stats?.runningHot == true) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(ThermometerIcon, contentDescription = null, tint = WarmAccent, modifier = Modifier.size(15.dp))
            Text(tr(if (thermalLabel(stats.thermalStatus) == "Hot") "Device is hot" else "Device is warm"), color = WarmAccent, style = TvType.Caption.overArtwork())
        }
        if (weather == null || weatherState == WeatherLoadState.Error) {
            Text(tr(if (!LocalInternet.current) "Waiting for internet" else if (weatherState == WeatherLoadState.Error) "Updating weather" else "Loading weather"),
                color = StatusSecondary, style = TvType.Caption.overArtwork())
        }
    }
}

@Composable
private fun StatusReading(icon: ImageVector, description: String, number: String, unit: String) {
    val spoken = "${tr(description)} $number$unit"
    Row(Modifier.clearAndSetSemantics { contentDescription = spoken }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = StatusSecondary, modifier = Modifier.size(16.dp))
        RollingText(number + unit, TvType.Footnote.overArtwork(), color = Color.White.copy(alpha = .88f))
    }
}

internal fun networkIcon(stats: DeviceStats): ImageVector = when (stats.network) {
    NetworkKind.Offline -> OfflineIcon
    NetworkKind.Ethernet -> EthernetIcon
    else -> WifiIcons[stats.wifiBars.coerceIn(WifiIcons.indices)]
}

private fun networkName(stats: DeviceStats) = when (stats.network) {
    NetworkKind.Offline -> "Offline"
    NetworkKind.Ethernet -> "Ethernet"
    NetworkKind.Wifi -> "Wi-Fi"
    NetworkKind.Other -> "Connected"
}

@Composable
private fun NetworkGlyph(stats: DeviceStats, tint: Color) {
    Icon(networkIcon(stats), contentDescription = tr(networkName(stats)), tint = tint, modifier = Modifier.size(16.dp))
}

/** Settings → Device. Every reading can take focus, so the remote can scroll at large text sizes. */
@Composable
internal fun DevicePanel(toggle: @Composable () -> Unit) {
    val stats = rememberDeviceStats(active = true, detailed = true)
    val speed = rememberDownloadSpeed(active = true)
    val display = rememberDisplaySummary()
    toggle()
    if (stats != null) {
        stats.cpuUsage?.let { DeviceReading(CpuIcon, "CPU", "$it% ${tr("in use")}", it / 100f) }
        DeviceReading(MemoryIcon, "Memory", "${formatBytes(stats.memoryFree)} ${tr("free of")} ${formatBytes(stats.memoryTotal)}", usedFraction(stats.memoryFree, stats.memoryTotal))
        DeviceReading(StorageIcon, "Storage", "${formatBytes(stats.storageFree)} ${tr("free of")} ${formatBytes(stats.storageTotal)}", usedFraction(stats.storageFree, stats.storageTotal))
        DeviceReading(networkIcon(stats), "Network", listOfNotNull(tr(networkName(stats)), speed?.let { "↓ ${formatSpeed(it)}" }).joinToString(" · "))
    }
    display?.let {
        DeviceReading(DisplayIcon, "Display", listOf(it.resolution, it.hdr, it.refresh).filter(String::isNotBlank).joinToString(" · "))
    }
    if (stats != null && thermalLabel(stats.thermalStatus) != "Normal") DeviceReading(ThermometerIcon, "Thermal", tr(thermalLabel(stats.thermalStatus)))
}

internal fun usedFraction(free: Long, total: Long): Float =
    if (total <= 0L) 0f else (1f - free.toFloat() / total).coerceIn(0f, 1f)

@Composable
private fun DeviceReading(icon: ImageVector, label: String, value: String, used: Float? = null) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val ink = if (focused) FocusContent else Color.White
    val secondary = if (focused) FocusContent.copy(alpha = .7f) else SecondaryText
    Column(
        Modifier.fillMaxWidth().clip(ControlShape).background(if (focused) FocusSurface else Color.White.copy(alpha = .075f))
            .focusable(interactionSource = interaction).padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = secondary, modifier = Modifier.size(18.dp))
            // The label keeps its width; a long value is what gives way.
            Text(tr(label), color = ink, style = TvType.Body, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
            RollingText(value, TvType.Footnote, Modifier.weight(1f), color = secondary)
        }
        if (used != null) {
            Spacer(Modifier.height(7.dp))
            AnimatedMeter(used, if (focused) FocusContent.copy(alpha = .16f) else Color.White.copy(alpha = .14f),
                if (focused) FocusContent else Color.White.copy(alpha = .85f))
        }
    }
}
