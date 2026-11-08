package tv.reelora.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.tv.material3.Text

internal data class TextRun(val value: String, val numeric: Boolean)

/** Splits text so numbers can roll while the words around them keep their kerning. */
internal fun textRuns(text: String): List<TextRun> {
    fun digitAt(index: Int) = text.getOrNull(index)?.isDigit() == true
    fun numericAt(index: Int) = when (text[index]) {
        in '0'..'9' -> true
        '.', ',', ':' -> digitAt(index - 1) && digitAt(index + 1)
        '-', '−' -> digitAt(index + 1) && (index == 0 || text[index - 1] == ' ')
        else -> false
    }
    val runs = mutableListOf<TextRun>()
    var start = 0
    for (index in 1..text.length) {
        if (index == text.length || numericAt(index) != numericAt(start)) {
            if (start < text.length) runs += TextRun(text.substring(start, index), numericAt(start))
            start = index
        }
    }
    return runs
}

/** True when the value went up or stayed level, so digits roll in the direction of the change. */
internal fun numberRose(previous: String, current: String): Boolean {
    fun magnitude(value: String) = value.filter(Char::isDigit).toDoubleOrNull()?.let { if (value.first() in "-−") -it else it }
    return (magnitude(current) ?: 0.0) >= (magnitude(previous) ?: 0.0)
}

private class RollState(var value: String) {
    var rising = true
    var composed = false
}

private fun crossfade(): ContentTransform =
    ContentTransform(fadeIn(tween(TvMotion.NumberMillis)), fadeOut(tween(TvMotion.NumberMillis / 2)), sizeTransform = null)

private fun roll(rising: Boolean): ContentTransform {
    val motion = tween<IntOffset>(TvMotion.NumberMillis, easing = TvMotion.Emphasized)
    val direction = if (rising) 1 else -1
    return ContentTransform(
        slideInVertically(motion) { direction * it / 2 } + fadeIn(tween(TvMotion.NumberMillis)),
        slideOutVertically(motion) { -direction * it / 2 } + fadeOut(tween(TvMotion.NumberMillis / 2)),
        sizeTransform = null,
    )
}

/**
 * The one way changing text moves. Only digits that differ roll, words cross-fade, and the
 * first render is static. Follows the system animator scale, like all Compose motion.
 */
@Composable
internal fun RollingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    arrangement: Arrangement.Horizontal = Arrangement.End,
) {
    val runs = remember(text) { textRuns(text) }
    AnimatedContent(
        targetState = runs,
        modifier = modifier.clearAndSetSemantics { contentDescription = text },
        contentKey = { shown -> shown.map(TextRun::numeric) },
        contentAlignment = Alignment.CenterEnd,
        transitionSpec = { crossfade() },
        label = "Rolling text",
    ) { shown ->
        Row(horizontalArrangement = arrangement, verticalAlignment = Alignment.CenterVertically) {
            shown.forEachIndexed { index, run ->
                key(index) {
                    if (run.numeric) NumberRun(run.value, style, color)
                    else AnimatedContent(run.value, transitionSpec = { crossfade() }, contentAlignment = Alignment.CenterEnd, label = "Rolling word") { value ->
                        Text(value, color = color, style = style, textAlign = textAlign, maxLines = maxLines,
                            softWrap = maxLines > 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberRun(value: String, style: TextStyle, color: Color) {
    val state = remember { RollState(value) }
    if (state.value != value) {
        state.rising = numberRose(state.value, value)
        state.value = value
    }
    val digits = remember(style) { style.copy(fontFeatureSettings = "tnum") }
    SideEffect { state.composed = true }
    Row {
        value.forEachIndexed { index, character ->
            // Keyed from the right, so 9 to 10 adds a digit without disturbing the units.
            key(value.length - index) { DigitSlot(character, digits, color, state) }
        }
    }
}

@Composable
private fun DigitSlot(character: Char, style: TextStyle, color: Color, state: RollState) {
    val entrance = remember { Animatable(if (state.composed) 0f else 1f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(TvMotion.NumberMillis)) }
    AnimatedContent(
        targetState = character,
        modifier = Modifier.graphicsLayer { alpha = entrance.value },
        contentAlignment = Alignment.Center,
        transitionSpec = {
            if (initialState.isDigit() && targetState.isDigit()) roll(state.rising) else crossfade()
        },
        label = "Rolling digit",
    ) { shown -> Text(shown.toString(), color = color, style = style, maxLines = 1, softWrap = false) }
}
