package tv.reelora.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

// Shared TV palette, geometry, and bounded motion for remote-driven surfaces.
internal val Background = Color(0xFF090B0E)
internal val Surface = Color(0xFF15181D)
internal val ControlSurface = Color(0xFF24282E)
internal val ActionSurface = Color(0x1FFFFFFF)
internal val SelectedSurface = Color(0x33FFFFFF)
internal val GroupSurface = Color(0x26000000)
internal val DialogSurface = Brush.verticalGradient(listOf(Color(0xE6383E49), Color(0xF020252D)))
internal val DockSurface = Color(0x8F30343C)
internal val FocusSurface = Color(0xFFF1F3F5)
internal val FocusContent = Color(0xFF12161C)
internal val Accent = Color(0xFFA7CCF5)
internal val WarmAccent = Color(0xFFEAC28A)
internal val SecondaryText = Color(0xFFC0C6CE)
internal val SubtleBorder = Color(0x24FFFFFF)
internal val DialogShape = RoundedCornerShape(36.dp)
internal val ControlShape = RoundedCornerShape(22.dp)
internal val ActionShape = RoundedCornerShape(percent = 50)
internal val DockShape = RoundedCornerShape(32.dp)
internal val AppTileShape = RoundedCornerShape(18.dp)
internal val AppTileWidth = 128.dp
internal val AppTileHeight = 76.dp
internal val Gap = 12.dp
internal val GapLarge = 24.dp
internal val DialogPadding = 32.dp

internal object TvMotion {
    const val BrowseMillis = 360
    const val EnterMillis = 240
    const val InsertMillis = 180
    const val RemoveMillis = 140
    const val ArtworkMillis = 700
    val EnterDistance = 10.dp
}
