package tv.reelora.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class CatalogRepositoryTest {
    @Test
    fun catalogRoutesCoverDistinctTvSections() {
        val specs = CatalogRepository.specs
        assertEquals(5, CatalogRepository.pageTitles.size)
        assertEquals(20, specs.map { it.title }.distinct().size)
        assertTrue(CatalogRepository.pageTitles.indices.all { page -> specs.count { it.page == page } == 4 })
        assertTrue(specs.single { it.title == "Popular animation" }.path.contains("with_genres=16"))
        assertTrue(specs.any { it.mediaType == "tv" })
    }

    @Test
    fun detailsAlwaysProvideUsefulFallbackContent() = runBlocking {
        val item = MediaItem(1, "Test", "Description", "2026", 8.0, 100, "movie", null, null)
        val details = CatalogRepository.details(item)
        assertTrue(details.runtime.isNotBlank())
        assertTrue(details.genres.isNotBlank())
        assertTrue(details.cast.isNotEmpty())
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
    fun launcherDiscoveryRowsAreDiverseAndNeverRepeatTitles() {
        fun item(id: Int) = MediaItem(id, "Title $id", "", "2026", 8.0, 1, "movie", null, null)
        val catalog = CatalogResult(
            listOf(
                CatalogSection(0, "Now in cinemas", listOf(item(1), item(2))),
                CatalogSection(0, "Trending this week", listOf(item(1), item(3))),
                CatalogSection(0, "Top rated movies", listOf(item(2), item(4))),
                CatalogSection(0, "Popular series", listOf(item(5))),
                CatalogSection(0, "Popular animation", listOf(item(6))),
                CatalogSection(0, "Coming soon", listOf(item(6), item(7))),
            ),
            false,
        )
        val sections = launcherMovieSections(catalog)
        val keys = sections.flatMap { it.items }.map(::mediaKey)

        assertEquals(6, sections.size)
        assertEquals(keys.distinct(), keys)
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
}
