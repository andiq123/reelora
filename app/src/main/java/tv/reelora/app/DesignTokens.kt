package tv.reelora.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Shared TV palette and geometry. Focus is a high-contrast state, not an extra effect.
internal val Background = Color(0xFF090B0E)
internal val Surface = Color(0xFF15181D)
internal val ControlSurface = Color(0xFF24282E)
internal val SelectedSurface = Color(0xFF2A3440)
internal val FocusSurface = Color(0xFFF1F3F5)
internal val FocusContent = Color(0xFF12161C)
internal val Accent = Color(0xFFA7CCF5)
internal val WarmAccent = Color(0xFFEAC28A)
internal val SecondaryText = Color(0xFFADB4BE)
internal val SubtleBorder = Color(0x24FFFFFF)
internal val DialogShape = RoundedCornerShape(28.dp)
internal val ControlShape = RoundedCornerShape(14.dp)
internal val Gap = 12.dp
internal val GapLarge = 24.dp
internal val DialogPadding = 28.dp

internal object TvMotion {
    const val EnterMillis = 140
    const val InsertMillis = 100
    const val RemoveMillis = 80
    val EnterDistance = 6.dp
}
