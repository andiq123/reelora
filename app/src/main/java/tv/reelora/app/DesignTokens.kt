package tv.reelora.app

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

// Shared TV palette, geometry, and bounded motion for remote-driven surfaces.
internal val Background = Color(0xFF090B0E)
internal val Surface = Color(0xFF15181D)
internal val ControlSurface = Color(0xFF24282E)
internal val ActionSurface = Color(0x1FFFFFFF)
internal val SelectedSurface = Color(0x33FFFFFF)
internal val GroupSurface = Color(0x26000000)
internal val DialogSurface = Brush.verticalGradient(listOf(Color(0xE6383E49), Color(0xF020252D)))
internal val FocusSurface = Color(0xFFF1F3F5)
internal val FocusContent = Color(0xFF12161C)
internal val Accent = Color(0xFFA7CCF5)
internal val WarmAccent = Color(0xFFEAC28A)
internal val SecondaryText = Color(0xFFC0C6CE)
internal val SubtleBorder = Color(0x24FFFFFF)
internal val DialogShape = RoundedCornerShape(36.dp)
internal val ControlShape = RoundedCornerShape(22.dp)
internal val ActionShape = RoundedCornerShape(percent = 50)
internal val AppTileCorner = 18.dp
internal val AppTileShape = RoundedCornerShape(AppTileCorner)
internal val PosterCorner = 12.dp
internal val PosterShape = RoundedCornerShape(PosterCorner)
// 16:9, matching Android TV banners; about five apps fit across like tvOS Home.
internal val AppTileWidth = 148.dp
internal val AppTileHeight = 84.dp
// Search and Settings are utilities, so they take less of the shelf than an app.
internal val ActionTileWidth = AppTileHeight
// The one horizontal margin shared by Home, shelves, details, and status content.
internal val ScreenMargin = 58.dp
internal val Gap = 12.dp
internal val GapLarge = 24.dp
internal val DialogPadding = 32.dp

// One variable file; each entry must request its own weight from the font's axis.
@OptIn(ExperimentalTextApi::class)
private fun interAt(weight: FontWeight) =
    Font(R.font.inter, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

internal val AppFont = FontFamily(
    interAt(FontWeight.Normal), interAt(FontWeight.Medium), interAt(FontWeight.SemiBold), interAt(FontWeight.Bold),
)

// The complete type scale. 12 sp is the floor for anything read from the couch.
internal object TvType {
    private fun style(size: Int, line: Int, weight: FontWeight, tracking: Float = 0f) = TextStyle(
        fontFamily = AppFont, fontSize = size.sp, lineHeight = line.sp,
        fontWeight = weight, letterSpacing = tracking.sp,
    )
    val Caption = style(12, 16, FontWeight.Normal)
    val Footnote = style(14, 20, FontWeight.Normal)
    val Body = style(16, 24, FontWeight.Normal)
    val Subhead = style(18, 24, FontWeight.Medium, -.1f)
    val Headline = style(22, 28, FontWeight.SemiBold, -.2f)
    val Title = style(28, 34, FontWeight.SemiBold, -.4f)
    val Hero = style(40, 46, FontWeight.Bold, -1f)
    val Display = style(52, 58, FontWeight.Bold, -1.4f)
}

internal val AppTypography = Typography(
    bodyLarge = TvType.Body, bodyMedium = TvType.Footnote, bodySmall = TvType.Caption,
    labelLarge = TvType.Body, labelMedium = TvType.Footnote, labelSmall = TvType.Caption,
)

private val ArtworkShadow = Shadow(Color.Black.copy(alpha = .6f), Offset(0f, 1f), 3f)
/** For text sitting directly on a photo or artwork, with no panel behind it. */
internal fun TextStyle.overArtwork() = copy(shadow = ArtworkShadow)

internal object TvMotion {
    // Unhurried, decelerating motion: movement starts promptly and settles softly.
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val BrowseMillis = 480
    const val HeroTextMillis = 480
    const val EnterMillis = 300
    const val InsertMillis = 180
    const val RemoveMillis = 140
    const val ArtworkMillis = 900
    val EnterDistance = 10.dp
    // Content (tiles, cards, cast) lifts by this much; controls invert in place instead.
    const val FocusScale = 1.06f
    const val PressScale = .97f
    const val LabelMillis = 160
    const val NumberMillis = 320
    const val MeterMillis = 600
}
