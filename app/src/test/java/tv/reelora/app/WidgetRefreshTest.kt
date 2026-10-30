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
    @Test fun footballPollsFastOnlyAroundAKnownKickoff() {
        val now = java.time.Instant.parse("2026-10-30T12:00:00Z")
        fun next(kickoff: String) = FootballSnapshot(null,
            FootballMatch("A", "B", "2026-10-30", "", null, null, kickoff = java.time.Instant.parse(kickoff)), null)
        assertEquals(30 * 60_000L, footballRefreshInterval(next("2026-10-30T19:00:00Z"), now))
        assertEquals(5 * 60_000L, footballRefreshInterval(next("2026-10-30T12:20:00Z"), now))
        assertEquals(120_000L, footballRefreshInterval(next("2026-10-30T12:10:00Z"), now))
        assertEquals(120_000L, footballRefreshInterval(next("2026-10-30T11:00:00Z"), now))
        assertEquals(30 * 60_000L, footballRefreshInterval(next("2026-10-30T06:00:00Z"), now))
    }
}
