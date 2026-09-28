package tv.reelora.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Small local outline icon: hide is reversible, unlike deleting an app.
internal val HideAppIcon = ImageVector.Builder("Hide app", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(3f, 3f); lineTo(21f, 21f)
        moveTo(8f, 5.8f); curveTo(13f, 3.8f, 19f, 7f, 22f, 12f)
        curveTo(21f, 13.8f, 19.8f, 15.2f, 18.5f, 16.2f)
        moveTo(15.7f, 18f); curveTo(10f, 20.2f, 4.8f, 16.9f, 2f, 12f)
        curveTo(3f, 10.3f, 4.2f, 8.8f, 5.5f, 7.8f)
        moveTo(9.4f, 10f); curveTo(7.4f, 13.4f, 10.7f, 16.5f, 14f, 14.6f)
    }
}.build()
