package tv.reelora.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import java.time.LocalTime

@Composable
internal fun HomeClock(time: LocalTime, use24HourClock: Boolean) {
    val formatted = formatHomeTime(time, use24HourClock)
    val style = TextStyle(
        color = Color.White, fontSize = 28.sp, lineHeight = 34.sp,
        fontWeight = FontWeight.Medium, letterSpacing = (-.5).sp, fontFeatureSettings = "tnum",
    )
    Row(Modifier.clearAndSetSemantics { contentDescription = formatted }, verticalAlignment = Alignment.CenterVertically) {
        StatusNumber(formatted.substringBefore(':'), style)
        Text(":", style = style)
        StatusNumber(formatted.substringAfter(':').take(2), style)
        val period = formatted.substringAfter(' ', "")
        if (period.isNotEmpty()) Text(" $period", style = style)
    }
}

@Composable
internal fun HomeTemperature(temperature: Int?, style: TextStyle) {
    val value = temperature?.toString() ?: "—"
    Row(Modifier.clearAndSetSemantics { contentDescription = "$value°" }, verticalAlignment = Alignment.CenterVertically) {
        StatusNumber(value, style.copy(fontFeatureSettings = "tnum"))
        Text("°", style = style)
    }
}

/** Native transitions honor system duration scale and run only when a value changes. */
@Composable
private fun StatusNumber(value: String, style: TextStyle) {
    AnimatedContent(
        targetState = value,
        modifier = Modifier.clipToBounds(),
        contentAlignment = Alignment.CenterEnd,
        transitionSpec = {
            val incoming = fadeIn(tween(200, delayMillis = 30))
            val outgoing = fadeOut(tween(100))
            // Loading placeholders fade in place; numeric updates move only a quarter line.
            if (initialState.toIntOrNull() == null || targetState.toIntOrNull() == null) {
                (incoming togetherWith outgoing).using(null)
            } else {
                ((incoming + slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 4 }) togetherWith
                    (outgoing + slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it / 4 })).using(null)
            }
        },
        label = "Status number",
    ) { shown ->
        Text(shown, style = style, maxLines = 1, softWrap = false)
    }
}
