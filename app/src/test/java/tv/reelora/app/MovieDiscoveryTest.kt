package tv.reelora.app

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class MovieDiscoveryTest {
    private val today = LocalDate.of(2026, 9, 28)
    private fun movie(id: Int, date: String = "2026-09-01", popularity: Double = 1.0) =
        MediaItem(id, "Movie $id", "", date.take(4), 8.0, 20, "movie", "poster", "backdrop",
            releaseDate = date, popularity = popularity)

    @Test fun discoveryBalancesSourcesAndSpacesUpcomingHighlights() {
        val current = (1..20).map { movie(it) }
        val future = (21..30).map { movie(it, "2026-12-18", it.toDouble()) }
        val catalog = CatalogResult(listOf(
            CatalogSection(2, "Popular movies", current),
            CatalogSection(0, "Trending this week", current.reversed()),
            CatalogSection(1, "Coming soon", future),
        ), false)
        val mix = discoveryMovies(catalog, today)
        assertEquals(20, mix.size)
        assertEquals(20, mix.distinctBy(::mediaKey).size)
        assertEquals(listOf(30, 29, 28, 27), listOf(4, 9, 14, 19).map { mix[it].id })
        assertEquals(listOf(1, 20, 2, 19), mix.take(4).map { it.id })
    }

    @Test fun comingSoonFetchesOnlyPopularPagesWithoutLosingLaterMovies() {
        val spec = CatalogRepository.specs.single { it.title == "Coming soon" }
        val paths = catalogPaths(spec, today)
        assertEquals(3, paths.size)
        assertTrue(paths.all { it.contains("sort_by=popularity.desc") })
        assertEquals(3, paths.distinct().size)
        val items = (1..70).map { movie(it, today.plusDays(it.toLong()).toString()) } + movie(99, "2026-12-18")
        val filtered = filterSectionItems("Coming soon", items.reversed() + items.take(10), today)
        assertEquals(71, filtered.size)
        assertEquals(99, filtered.last().id)
        assertEquals(filtered.map { it.releaseDate }.sorted(), filtered.map { it.releaseDate })
    }

    @Test fun shortlistPrioritizesInterestBeforeDateWithoutRequiringEarlyVotes() {
        val films = (1..60).map { movie(it, today.plusDays(it.toLong()).toString(), it.toDouble()) }
        val anticipated = movie(99, "2026-12-18", 100.0).copy(voteCount = 0, score = 0.0)
        val noArtwork = anticipated.copy(id = 100, posterUrl = null, backdropUrl = null)
        val noInterest = anticipated.copy(id = 101, popularity = 0.0)
        val selected = curatedComingSoon(films + anticipated + anticipated + noArtwork + noInterest)
        assertEquals(40, selected.size)
        assertEquals(40, selected.map { it.id }.distinct().size)
        assertTrue(anticipated in selected)
        assertFalse(noArtwork in selected)
        assertFalse(noInterest in selected)
        assertFalse(films.first() in selected)
        assertEquals(selected.map { it.releaseDate }.sorted(), selected.map { it.releaseDate })
    }

    @Test fun discoveryHandlesMissingArtDatesAndSingleSource() {
        val items = listOf(movie(1), movie(2).copy(releaseDate = ""), movie(3).copy(backdropUrl = null, posterUrl = null), movie(4, "2026-12-18"))
        assertEquals(listOf(movie(1)), discoveryMovies(CatalogResult(listOf(CatalogSection(2, "Popular movies", items)), false), today))
        assertTrue(discoveryMovies(CatalogResult(emptyList(), true), today).isEmpty())
    }
}
