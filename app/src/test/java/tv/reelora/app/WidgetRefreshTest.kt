package tv.reelora.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRefreshTest {
    @Test fun reconnectRetriesFailuresButPreservesFreshContent() {
        assertEquals(0L, widgetRefreshDelay(100L, 60_100L, failed = true))
        assertEquals(60_000L, widgetRefreshDelay(100L, 60_100L, failed = false))
        assertEquals(0L, widgetRefreshDelay(200L, 100L, failed = false))
    }
    @Test fun footballUsesFastRefreshOnlyForLiveOrMatchDay() {
        val match = FootballMatch("A", "B", LocalDate.now().plusDays(1).toString(), "18:00", null, null)
        assertEquals(600_000L, footballRefreshInterval(FootballSnapshot(null, match, null)))
        assertEquals(120_000L, footballRefreshInterval(FootballSnapshot(match, null, null)))
        assertEquals(120_000L, footballRefreshInterval(FootballSnapshot(null, match.copy(date = LocalDate.now().toString()), null)))
    }
}
