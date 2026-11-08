package tv.reelora.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.time.LocalTime

@Composable
internal fun HomeClock(time: LocalTime, use24HourClock: Boolean) {
    RollingText(formatHomeTime(time, use24HourClock), TvType.Title.copy(color = Color.White).overArtwork())
}

@Composable
internal fun HomeTemperature(temperature: Int?, style: TextStyle) {
    RollingText("${temperature?.toString() ?: "—"}°", style)
}
