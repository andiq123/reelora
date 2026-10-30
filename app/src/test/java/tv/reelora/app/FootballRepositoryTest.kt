package tv.reelora.app

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FootballRepositoryTest {
    private val now = Instant.parse("2026-10-27T15:00:00Z")
    private fun event(fields: String) = JSONObject("""{"events":[{"strHomeTeam":"Arsenal","strAwayTeam":"Everton",$fields}]}""")
        .footballMatches().single()
    private fun match(kickoff: String, status: String = "NS", priority: Int = 2) = FootballMatch(
        "Home", "Away", kickoff.take(10), kickoff.substring(11, 16), null, null,
        status = status, priority = priority, kickoff = Instant.parse(kickoff),
    )

    @Test fun jsonNullsFallBackToUtcAndNeverBecomeLiveStatuses() {
        val parsed = event(""""dateEventLocal":null,"strTimeLocal":null,"dateEvent":"2026-10-27","strTime":"17:45:00","strProgress":null,"strStatus":"NS","intHomeScore":null,"intAwayScore":null""")
        assertEquals("NS", parsed.status)
        assertFalse(isLiveFootballStatus(parsed.status))
        assertNull(parsed.homeScore)
        assertEquals("Tue, 27 Oct · 19:45", footballSchedule(parsed, zone = ZoneId.of("Europe/Chisinau")))
        val missing = event(""""dateEvent":null,"strTime":null,"strProgress":null,"strStatus":null""")
        assertEquals("", footballSchedule(missing))
        assertFalse(isLiveFootballStatus(missing.status))
        val literalNull = event(""""dateEvent":"null","strTime":"null","strProgress":"null","strStatus":"null" """)
        assertEquals("", footballSchedule(literalNull))
        assertFalse(isLiveFootballStatus(literalNull.status))
    }

    @Test fun finalStatusWinsOverAnOldProgressMinute() {
        val parsed = event(""""strStatus":"FT","strProgress":"90'","dateEvent":"2026-10-27","strTime":"12:00:00" """)
        assertEquals("FT", parsed.status)
        assertFalse(isLiveFootballStatus(parsed.status))
        val fallback = event(""""strStatus":null,"strProgress":"67'" """)
        assertTrue(isLiveFootballStatus(fallback.status))
    }

    @Test fun onlyInPlayStatesAreLive() {
        listOf("1H", "HT", "2H", "ET", "BT", "P", "67", "67'", "90+5′").forEach {
            assertTrue(it, isLiveFootballStatus(it))
        }
        listOf("", "null", "NS", "TBD", "FT", "AET", "PEN", "Final", "PST", "CANC", "SUSP", "INT", "ABD", "AWD", "WO", "unknown", "2026").forEach {
            assertFalse(it, isLiveFootballStatus(it))
        }
        assertFalse(isLiveFootballMatch(match("2026-10-26T14:00:00Z", "2H"), now))
        assertFalse(isLiveFootballMatch(match("2026-10-27T16:00:00Z", "1H"), now))
        assertTrue(isLiveFootballMatch(match("2026-10-27T14:00:00Z", "2H"), now))
    }

    @Test fun viewerTimezoneOverridesVenueTimeAndHandlesMidnightAndDst() {
        val parsed = event(""""strTimestamp":"2026-10-27T23:45:00","dateEvent":"2026-10-27","strTime":"23:45:00","dateEventLocal":"2026-10-27","strTimeLocal":"17:45:00" """)
        assertEquals("Wed, 28 Oct · 01:45", footballSchedule(parsed, zone = ZoneId.of("Europe/Chisinau")))
        assertEquals("Tue, 27 Oct · 23:45", footballSchedule(parsed, zone = ZoneId.of("Europe/London")))
        assertEquals("Wed, 28 Oct · 01:45", footballSchedule(parsed.copy(kickoff = footballKickoff("2026-10-28T01:45:00+02:00", "", "")), zone = ZoneId.of("Europe/Chisinau")))
        val summer = parsed.copy(kickoff = Instant.parse("2026-10-24T12:00:00Z"))
        assertEquals("Sat, 24 Oct · 15:00", footballSchedule(summer, zone = ZoneId.of("Europe/Chisinau")))
        assertTrue(footballSchedule(parsed, Locale.forLanguageTag("ro"), ZoneId.of("Europe/Chisinau")).contains("01:45"))
    }

    @Test fun upcomingSelectionUsesKickoffBeforeCompetitionPriority() {
        val earlier = match("2026-10-27T16:00:00Z")
        val later = match("2026-10-27T20:00:00Z", priority = 0)
        val past = match("2026-10-27T14:00:00Z")
        val postponed = earlier.copy(status = "PST", priority = 0)
        val finished = earlier.copy(status = "FT", priority = 0)
        val invalid = earlier.copy(date = "null", time = "null", kickoff = null)
        assertEquals(earlier, selectNextFootballMatch(listOf(later, past, postponed, finished, invalid, earlier), now))
        assertNull(selectNextFootballMatch(listOf(past, postponed, finished, invalid), now))
    }

    @Test fun previousSelectionRejectsInvalidAndUnfinishedMatches() {
        val old = match("2026-10-26T20:00:00Z", "FT")
        val recent = match("2026-10-27T13:00:00Z", "AET")
        val invalid = recent.copy(date = "null", time = "null", kickoff = null)
        assertEquals(recent, selectPreviousFootballMatch(listOf(old, recent, invalid, recent.copy(status = "2H"), recent.copy(status = "NS")), now))
        assertNull(selectPreviousFootballMatch(listOf(invalid, recent.copy(status = "NS")), now))
    }

    @Test fun malformedRowsAreSkippedAndAllValidRowsAreParsed() {
        val parsed = JSONObject("""{"events":[null,{}, {"strHomeTeam":null,"strAwayTeam":"B"},
            {"strHomeTeam":"A","strAwayTeam":"B"}, {"strHomeTeam":"C","strAwayTeam":"D"}]}""").footballMatches()
        assertEquals(listOf("A", "C"), parsed.map { it.home })
        assertTrue(JSONObject("""{"events":null}""").footballMatches().isEmpty())
    }

    @Test fun missingSchedulesAndScheduledScoresHaveCleanPresentation() {
        val parsed = event(""""dateEvent":"2026-10-27","strTime":null,"intHomeScore":0,"intAwayScore":0,"strStatus":"NS" """)
        assertEquals("Tue, 27 Oct", footballSchedule(parsed))
        assertEquals("vs", footballScore(parsed))
        assertEquals("", footballSchedule(parsed.copy(date = "null", time = "null", kickoff = null)))
    }
}
