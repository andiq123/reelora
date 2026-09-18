package tv.reelora.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherPresentationTest {
    @Test fun supportedWeatherCodesHaveSpecificIcons() {
        val codes = mapOf(
            WeatherKind.Sun to listOf(0, 1), WeatherKind.PartlyCloudy to listOf(2), WeatherKind.Cloud to listOf(3),
            WeatherKind.Fog to listOf(45, 48), WeatherKind.Rain to listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82),
            WeatherKind.Snow to listOf(71, 73, 75, 77, 85, 86), WeatherKind.Storm to listOf(95, 96, 99),
        )
        codes.forEach { (kind, values) -> values.forEach { assertEquals(kind, weatherKind(it)) } }
        assertEquals(WeatherKind.Moon, weatherKind(0, false))
        assertEquals(WeatherKind.Moon, weatherKind(1, false))
        assertEquals(WeatherKind.PartlyCloudyNight, weatherKind(2, false))
        assertEquals(WeatherKind.Unknown, weatherKind(-1))
        assertEquals(WeatherKind.Unknown, weatherKind(100))
    }
    @Test fun incompleteScoresNeverInventAResult() {
        val match = FootballMatch("Long home team", "Long away team", "", "", null, null)
        assertEquals("vs", footballScore(match))
        assertEquals("vs", footballScore(match.copy(homeScore = 1)))
        assertEquals("0–0", footballScore(match.copy(homeScore = 0, awayScore = 0)))
        assertEquals("12–10", footballScore(match.copy(homeScore = 12, awayScore = 10)))
    }
}
