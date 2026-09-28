package tv.reelora.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime

class CatalogRepositoryTest {
    @Test fun featuredSelectionAvoidsPreviousTitleAndHandlesSmallCatalogs() {
        val first = MediaItem(1, "First", "", "2026", 7.0, 10, "movie", null, null)
        val second = first.copy(id = 2, title = "Second")
        assertEquals(second, nextDiscoveryItem(listOf(first, second), listOf(mediaKey(first))))
        assertEquals(first, nextDiscoveryItem(listOf(first, second), listOf(mediaKey(first), mediaKey(second))))
        assertEquals(first, nextDiscoveryItem(listOf(first), listOf(mediaKey(first))))
        assertNull(nextDiscoveryItem(emptyList(), emptyList()))
    }

    @Test
    fun unreleasedTitlesWithoutVotesAreNotZeroRated() {
        val movie = MediaItem(1, "Future movie", "", "2026", 0.0, 0, "movie", null, null)
        assertEquals("Not rated", mediaRating(movie))
        assertEquals("★ 7.4", mediaRating(movie.copy(score = 7.4, voteCount = 20)))
        assertEquals("Not rated", mediaRating(movie.copy(score = Double.NaN, voteCount = 20)))
    }

    @Test
    fun comingSoonRequiresAFutureMovieDate() {
        val today = LocalDate.of(2026, 9, 9)
        fun movie(id: Int, date: String) = MediaItem(id, "Title", "", "2026", 7.0, 10, "movie", null, null, date)
        val past = movie(1, "2026-09-08")
        val now = movie(2, "2026-09-09")
        val future = movie(3, "2026-09-10")
        val unknown = movie(4, "")
        val bad = movie(5, "2026-99-99")
        val series = movie(6, "2026-10-01").copy(mediaType = "tv")
        val items = listOf(past, now, future, future, unknown, bad, series)
        assertEquals(listOf(future), filterSectionItems("Coming soon", items, today))
        assertEquals(listOf(past, now), filterSectionItems("Now in cinemas", items, today))
        assertTrue(filterSectionItems("Coming soon", items, today.plusDays(1)).isEmpty())
        val path = catalogPath(CatalogRepository.specs.single { it.title == "Coming soon" }, today)
        assertTrue(path.startsWith("/discover/movie?"))
        assertTrue(path.contains("primary_release_date.gte=2026-09-10"))
        assertTrue(path.contains("primary_release_date.lte=2027-09-09"))
    }

    @Test fun categoryFiltersAndOrderingPreserveTheirMeaning() {
        val today = LocalDate.of(2026, 9, 28)
        fun movie(id: Int, date: String) = MediaItem(id, "Title", "", "2026", 7.0, 10, "movie", null, null, date)
        val later = movie(1, "2026-11-01")
        val sooner = movie(2, "2026-10-01")
        val released = movie(3, "2026-09-20")
        val farFuture = movie(4, "2028-01-01")
        val series = released.copy(id = 5, mediaType = "tv")
        val animation = released.copy(id = 6, genreIds = listOf(16))
        val items = listOf(later, sooner, released, farFuture, series, animation, sooner)
        assertEquals(listOf(sooner, later), filterSectionItems("Coming soon", items, today))
        assertEquals(listOf(released, animation), filterSectionItems("Top rated movies", items, today))
        assertEquals(listOf(series), filterSectionItems("Popular series", items, today))
        assertEquals(listOf(animation), filterSectionItems("Popular animation", items, today))
        assertTrue(catalogPath(CatalogRepository.specs.single { it.title == "Coming soon" }, today)
            .contains("sort_by=popularity.desc"))
        assertEquals("Horror", primaryGenre(released.copy(genreIds = listOf(27, 53))))
        assertEquals("Sci-Fi & Fantasy", primaryGenre(series.copy(genreIds = listOf(10765))))
        assertEquals("Comedy", primaryGenre(released.copy(genreIds = listOf(-1, 35))))
        assertNull(primaryGenre(released))
    }

    @Test
    fun metadataCacheEvictsLeastRecentlyUsedEntries() {
        val cache = boundedCache<String, Int>(2)
        cache["first"] = 1
        cache["second"] = 2
        assertEquals(1, cache["first"])
        cache["third"] = 3
        assertEquals(setOf("first", "third"), cache.keys)
        repeat(100) { cache["item-$it"] = it }
        assertEquals(2, cache.size)
    }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun cancelledRequestsMustNotBecomeFallbackResults() {
        requestResult<Unit> { throw kotlinx.coroutines.CancellationException("Screen closed") }
            .getOrDefault(Unit)
    }

    @Test
    fun failedRequestsStillAllowAnOfflineFallback() {
        assertEquals("offline", requestResult<String> { throw java.io.IOException("Disconnected") }.getOrDefault("offline"))
    }

    @Test
    fun movieAndSeriesWithSameIdKeepSeparateIdentity() {
        val movie = MediaItem(42, "Movie", "", "2026", 8.0, 1, "movie", null, null)
        val series = movie.copy(mediaType = "tv", title = "Series")
        assertTrue(mediaKey(movie) != mediaKey(series))
        assertEquals(2, launcherMovieSections(CatalogResult(listOf(CatalogSection(0, "Trending this week", listOf(movie, series))), false)).single().items.size)
        assertTrue(launcherMovieSections(CatalogResult(emptyList(), false)).isEmpty())
    }

    @Test
    fun catalogRoutesCoverDistinctTvSections() {
        val specs = CatalogRepository.specs
        assertEquals(5, CatalogRepository.pageTitles.size)
        assertEquals(7, specs.map { it.title }.distinct().size)
        assertTrue(CatalogRepository.pageTitles.indices.all { page -> specs.any { it.page == page } })
        assertTrue(specs.single { it.title == "Popular animation" }.path.contains("with_genres=16"))
        assertTrue(specs.any { it.mediaType == "tv" })
    }

    @Test
    fun weatherCodesUseCompactTvSymbols() {
        assertEquals("☀", weatherSymbol(0))
        assertEquals("☂", weatherSymbol(61))
        assertEquals("❄", weatherSymbol(71))
        assertEquals("◌  Weather", weatherStatusText(null, WeatherLoadState.Loading))
        assertEquals("!  Weather", weatherStatusText(null, WeatherLoadState.Error))
        assertEquals("☀  24°", weatherStatusText(WeatherNow(24, 0), WeatherLoadState.Ready))
    }

    @Test
    fun clockDefaultsCanRenderEitherTvFormat() {
        val time = LocalTime.of(18, 5)
        assertEquals("18:05", formatHomeTime(time, true))
        assertEquals("6:05 PM", formatHomeTime(time, false))
    }

    @Test
    fun failedDetailsDoNotInventCastOrRuntime() = runBlocking {
        val item = MediaItem(1, "Test", "Description", "2026", 8.0, 100, "movie", null, null)
        val details = CatalogRepository.details(item)
        assertTrue(details.runtime.isBlank())
        assertTrue(details.genres.isBlank())
        assertTrue(details.cast.isEmpty())
        assertTrue(details.trailer == null)
        assertTrue(details.backdrops.isEmpty())
    }

    @Test
    fun blankSearchNeverCallsTheNetwork() = runBlocking {
        assertTrue(CatalogRepository.search(" ").isEmpty())
    }

    @Test
    fun invalidCastIdNeverCallsTheNetwork() = runBlocking {
        assertTrue(CatalogRepository.credits(0).isEmpty())
    }

    @Test
    fun discoveryAvoidsTheLastTenTitles() {
        val items = (1..3).map { MediaItem(it, "Title $it", "", "2026", 8.0, 1, "movie", null, null) }
        val next = nextDiscoveryItem(items, items.take(2).map(::mediaKey))
        assertEquals(items.last(), next)
    }

    @Test
    fun theaterIdleDelayCyclesThroughEveryChoice() {
        val cycled = generateSequence(THEATER_IDLE_OPTIONS.first(), ::nextTheaterIdleMinutes)
            .drop(1)
            .take(THEATER_IDLE_OPTIONS.size)
            .toList()
        assertEquals(THEATER_IDLE_OPTIONS.drop(1) + THEATER_IDLE_OPTIONS.first(), cycled)
        assertEquals(THEATER_IDLE_OPTIONS.first(), nextTheaterIdleMinutes(999))
    }

    @Test
    fun launcherOrderKeepsSavedAppsAndAppendsNewInstalls() {
        assertEquals(listOf("c", "a", "b"), orderedAppKeys(listOf("a", "b", "c"), listOf("missing", "c", "c", "a")))
        assertEquals(listOf("a", "c", "b"), moveAppKey(listOf("a", "b", "c"), "b", 1))
        assertEquals(listOf("a", "b", "c"), moveAppKey(listOf("a", "b", "c"), "a", -1))
        assertEquals(mapOf("app" to "Cinema"), savedCustomAppNames(mapOf("customName:app" to "Cinema", "other" to "ignored")))
    }

    @Test
    fun categoriesKeepTheirOwnTitlesAndDeduplicateWithinEachRow() {
        fun item(id: Int) = MediaItem(id, "Title $id", "", "2026", 8.0, 1, "movie", null, null)
        val catalog = CatalogResult(
            listOf(
                CatalogSection(0, "Now in cinemas", listOf(item(1), item(2), item(1))),
                CatalogSection(0, "Trending this week", listOf(item(1), item(3))),
                CatalogSection(0, "Top rated movies", listOf(item(2), item(4))),
                CatalogSection(0, "Popular series", listOf(item(5))),
                CatalogSection(0, "Popular animation", listOf(item(6))),
                CatalogSection(0, "Coming soon", listOf(item(6), item(7))),
            ),
            false,
        )
        val sections = launcherMovieSections(catalog)

        assertEquals(6, sections.size)
        assertEquals(listOf(1, 2), sections.first().items.map { it.id })
        assertEquals(listOf(1, 3), sections.single { it.title == "Trending this week" }.items.map { it.id })
        assertEquals(listOf(6), sections.single { it.title == "Popular animation" }.items.map { it.id })
        assertEquals(listOf(6, 7), sections.single { it.title == "Coming soon" }.items.map { it.id })
    }

    @Test
    fun verticalNavigationKeepsColumnAndClampsAtShorterRows() {
        assertEquals(0, adjacentRowIndex(0, 6))
        assertEquals(3, adjacentRowIndex(3, 6))
        assertEquals(5, adjacentRowIndex(9, 6))
        assertEquals(20_000L, nextCatalogRetryDelay(10_000L))
        assertEquals(300_000L, nextCatalogRetryDelay(300_000L))
    }

    @Test
    fun releaseBadgeDistinguishesUpcomingAndReleasedTitles() {
        val today = LocalDate.of(2026, 8, 18)
        assertTrue(releaseLabel("2026-09-04", today).startsWith("◷ COMING"))
        assertEquals("● RELEASES TODAY", releaseLabel("2026-08-18", today))
        assertTrue(releaseLabel("2026-06-17", today).startsWith("✓ RELEASED"))
        assertEquals("◷ SEP 4", cardReleaseLabel("2026-09-04", today))
        assertNull(cardReleaseLabel("2026-06-17", today))
    }

    @Test
    fun footballWidgetFormatsScheduledAndCompletedMatches() {
        val completed = FootballMatch("Fulham", "Chelsea", "2026-08-24", "19:00", 2, 3)
        val scheduled = completed.copy(homeScore = null, awayScore = null)

        assertEquals("2–3", footballScore(completed))
        assertEquals("vs", footballScore(scheduled))
        assertEquals("Mon, 24 Aug · 19:00", footballSchedule(completed))
        assertTrue(isLiveFootballStatus("67'"))
        assertTrue(!isLiveFootballStatus("FT"))
        val premierLeague = scheduled.copy(date = "2026-08-28", priority = 2)
        val worldCup = scheduled.copy(date = "2030-06-08", priority = 0)
        assertEquals(premierLeague, selectNextFootballMatch(listOf(worldCup, premierLeague)))
        assertEquals("World Cup · mâine", footballHintText(FootballHint("WORLD CUP", 1), true))
        assertEquals("CHAMPIONS LEAGUE", localizeUi("CHAMPIONS LEAGUE", true))
    }
}
