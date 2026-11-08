package tv.reelora.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// One local icon family: 24-unit grid, 1.75 stroke, round caps and joins.
private fun lineIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { data ->
            addPath(
                PathParser().parsePathString(data).toNodes(),
                stroke = SolidColor(Color.White), strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private fun solidIcon(name: String, data: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(PathParser().parsePathString(data).toNodes(), fill = SolidColor(Color.White))
    }.build()

private fun circle(x: Float, y: Float, r: Float) = "M${x - r} ${y}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

// Eight rounded teeth; the path is computed so every tooth is identical.
private val gearOutline = buildString {
    val teeth = 8
    val step = 2 * PI / teeth
    fun point(angle: Double, radius: Double) = "${12 + radius * cos(angle)} ${12 + radius * sin(angle)}"
    for (index in 0 until teeth) {
        val start = index * step - step / 2
        append(if (index == 0) "M" else "L").append(point(start + step * .14, 7.2))
        append("L").append(point(start + step * .30, 10.0))
        append("L").append(point(start + step * .70, 10.0))
        append("L").append(point(start + step * .86, 7.2))
    }
    append("Z")
}

internal val ChevronRightIcon = lineIcon("Forward", "M9 5l7 7-7 7")
internal val BackIcon = lineIcon("Back", "M20 12H4.5", "M10.5 6l-6 6 6 6")
internal val CheckIcon = lineIcon("Done", "M5 12.5l4.6 4.6L19 7.5")
internal val CloseIcon = lineIcon("Close", "M6 6l12 12", "M18 6L6 18")
internal val RefreshIcon = lineIcon("Refresh", "M19.5 12a7.5 7.5 0 1 1-2.2-5.3L20 9.4", "M20 4.5v4.9h-4.9")
internal val SearchIcon = lineIcon("Search", circle(10.75f, 10.75f, 6.5f), "M15.6 15.6l4.9 4.9")
internal val HomeIcon = lineIcon("Home", "M3.5 11.2L12 4l8.5 7.2", "M6 9.4V19a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V9.4", "M10 20v-5.5h4V20")
internal val AppsIcon = lineIcon(
    "Apps",
    "M5 4h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
    "M15 4h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
    "M5 14h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-4a1 1 0 0 1 1-1z",
    "M15 14h4a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1v-4a1 1 0 0 1 1-1z",
)
internal val SettingsIcon = lineIcon("Settings", gearOutline, circle(12f, 12f, 3f))
internal val EditIcon = lineIcon("Edit", "M4 20l1-4.2L16.2 4.6a2 2 0 0 1 2.9 0l.3.3a2 2 0 0 1 0 2.9L8.2 19z", "M14.5 6.5l3 3")
internal val InfoIcon = lineIcon("Info", circle(12f, 12f, 8.75f), "M12 11.2v5.3", "M12 7.8v.01")
internal val MoveIcon = lineIcon("Move", "M3.5 12h17", "M8 7.5L3.5 12 8 16.5", "M16 7.5l4.5 4.5-4.5 4.5")
internal val PlayIcon = solidIcon("Play", "M8 5.2c0-.8.9-1.3 1.6-.9l10 6.2c.6.4.6 1.3 0 1.7l-10 6.2c-.7.4-1.6-.1-1.6-.9z")
internal val StarIcon = solidIcon("Rating", "M12 3.6l2.6 5.3 5.8.8-4.2 4.1 1 5.8-5.2-2.7-5.2 2.7 1-5.8-4.2-4.1 5.8-.8z")
internal val PriceIcon = lineIcon("Rent or buy", "M4 5.5v5.7a1 1 0 0 0 .3.7l8 8a1 1 0 0 0 1.4 0l5.7-5.7a1 1 0 0 0 0-1.4l-8-8a1 1 0 0 0-.7-.3H5.5A1.5 1.5 0 0 0 4 5.5z", "M8.5 8.5v.01")
internal val ClockIcon = lineIcon("Coming", circle(12f, 12f, 8.75f), "M12 7.5V12l3 2")
internal val HideAppIcon = lineIcon(
    "Hide app", "M3 3l18 18",
    "M9.5 5.4C14 4.2 19 7 21.8 12c-.8 1.4-1.7 2.6-2.8 3.5",
    "M15.8 18.2C10.6 20.2 5 17.4 2.2 12c.9-1.6 2-3 3.3-4",
    "M9.9 9.9a3 3 0 0 0 4.2 4.2",
)

// Device status glyphs.
internal val MemoryIcon = lineIcon("Memory", "M4.5 7h15A1.5 1.5 0 0 1 21 8.5v6a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 14.5v-6A1.5 1.5 0 0 1 4.5 7z", "M7.5 16v3", "M12 16v3", "M16.5 16v3", "M8 11v1", "M12 11v1", "M16 11v1")
internal val StorageIcon = lineIcon("Storage", "M5 13.5L7.3 6a1.5 1.5 0 0 1 1.4-1h6.6a1.5 1.5 0 0 1 1.4 1l2.3 7.5", "M4.5 13.5h15a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18v-3a1.5 1.5 0 0 1 1.5-1.5z", "M7.5 16.5v.01")
internal val EthernetIcon = lineIcon("Ethernet", "M9.5 3.5h5a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-4a1 1 0 0 1 1-1z", "M12 9.5v4.5", "M5 20.5v-4.5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v4.5")
internal val OfflineIcon = lineIcon("Offline", "M3 3l18 18", "M8.8 5.6A13 13 0 0 1 21.5 9.3", "M2.5 9.3a13 13 0 0 1 3.1-2.2", "M5.8 12.8a8.5 8.5 0 0 1 3.4-2", "M9.2 16.2a4 4 0 0 1 4.7-.6", "M12 19.8v.01")
internal val ThermometerIcon = lineIcon("Temperature", "M10 13.5V5.5a2 2 0 1 1 4 0v8a4 4 0 1 1-4 0z", "M12 17v-6")
internal val DisplayIcon = lineIcon("Display", "M5 4.5h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2z", "M8.5 20.5h7")

private const val WifiDot = "M12 19.5v.01"
private const val WifiNear = "M9 16.2a4.5 4.5 0 0 1 6 0"
private const val WifiMiddle = "M5.9 12.9a9 9 0 0 1 12.2 0"
private const val WifiFar = "M2.8 9.5a13.5 13.5 0 0 1 18.4 0"
/** Index by signal bars, 0 to 3. The unlit arcs are simply absent. */
internal val WifiIcons = listOf(
    lineIcon("Wi-Fi", WifiDot),
    lineIcon("Wi-Fi", WifiDot, WifiNear),
    lineIcon("Wi-Fi", WifiDot, WifiNear, WifiMiddle),
    lineIcon("Wi-Fi", WifiDot, WifiNear, WifiMiddle, WifiFar),
)
