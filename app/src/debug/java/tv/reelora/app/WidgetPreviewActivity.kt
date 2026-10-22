package tv.reelora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import java.time.LocalTime
import kotlinx.coroutines.delay

/** Local visual fixtures only; excluded from release APKs. */
class WidgetPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = intent.getStringExtra("scenario") ?: "ready"
        val next = FootballMatch("Brentford", "Chelsea", "2026-09-18", "20:00", null, null, competition = "PREMIER LEAGUE", homeCountry = "England", awayCountry = "England")
        val previous = FootballMatch("Leeds United", "Newcastle United", "2026-09-14", "20:00", 4, 1, competition = "PREMIER LEAGUE", homeCountry = "England", awayCountry = "England")
        val snapshot = when (scenario) {
            "loading", "offline" -> null
            "long" -> FootballSnapshot(null, next.copy(home = "Borussia Mönchengladbach", away = "Wolverhampton Wanderers", homeCountry = "Germany"), previous)
            "live" -> FootballSnapshot(next.copy(home = "Barcelona", away = "Real Madrid", homeScore = 2, awayScore = 1, status = "67′", competition = "CHAMPIONS LEAGUE", homeCountry = "Spain", awayCountry = "Spain"), next, previous)
            else -> FootballSnapshot(null, next, previous, FootballHint("CHAMPIONS LEAGUE", 3))
        }
        val state = when (scenario) { "offline", "stale" -> WeatherLoadState.Error; "loading" -> WeatherLoadState.Loading; else -> WeatherLoadState.Ready }
        val weather = when (scenario) {
            "loading", "offline" -> null
            "night" -> WeatherNow(18, 0, false)
            "rain" -> WeatherNow(12, 63)
            "snow" -> WeatherNow(-12, 75)
            else -> WeatherNow(21, 2)
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalRomanian provides intent.getBooleanExtra("romanian", false), LocalInternet provides (scenario != "offline")) {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(
                        if (scenario == "bright") listOf(Color(0xFF7B817E), Color(0xFF323F43), Color(0xFF272E32))
                        else listOf(Color(0xFF17272F), Color(0xFF293D43), Color(0xFF111A24))))) {
                        if (scenario == "photo") {
                            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.preview_wallpaper),
                                contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0xB3090D13), .40f to Color(0x6B090D13), .70f to Color.Transparent, 1f to Color(0xAD090D13))))
                        }
                        FootballWidget(snapshot, state, Modifier.align(Alignment.TopStart).padding(start = 58.dp, top = 28.dp))
                        if (scenario == "numbers") {
                            NumberMotionPreview(Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 58.dp))
                        } else {
                            HomeStatus(weather, state, true, Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 58.dp))
                        }
                        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
                            .background(Color(0xB4141820), RoundedCornerShape(24.dp)).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            listOf("YouTube", "Netflix", "Stremio", "Apps", "Search", "Settings").forEach { label ->
                                Box(Modifier.size(116.dp, 68.dp).background(Color(0xFF28333C), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                                    Text(label, color = Color.White, fontSize = 17.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberMotionPreview(modifier: Modifier) {
    // Exercise actual Home controls without changing the device clock, weather, or preferences.
    val values = remember {
        listOf(LocalTime.of(23, 59) to 9, LocalTime.MIDNIGHT to 10,
            LocalTime.of(9, 59) to -1, LocalTime.of(10, 0) to 0,
            LocalTime.of(11, 59) to -9, LocalTime.NOON to -10,
            LocalTime.of(18, 5) to null, LocalTime.of(18, 6) to 21)
    }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_500)
            index = (index + 1) % values.size
        }
    }
    val (time, temperature) = values[index]
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HomeClock(time, true)
        HomeClock(time, false)
        HomeTemperature(temperature, TextStyle(color = Color.White, fontSize = 27.sp,
            lineHeight = 34.sp, fontWeight = FontWeight.Medium))
    }
}
