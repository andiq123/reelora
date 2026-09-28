package tv.reelora.app

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class MovieDiscoveryTest {
    private val today = LocalDate.of(2026, 9, 28)
    private fun movie(id: Int, date: String = "2026-09-01", popularity: Double = 1.0) =
        MediaItem(id, "Movie $id", "Synopsis", date.take(4), 8.0, 20, "movie", "poster", "backdrop",
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
        assertEquals(25, mix.size)
        assertEquals(25, mix.distinctBy(::mediaKey).size)
        assertEquals(listOf(30, 29, 28, 27, 26), listOf(4, 9, 14, 19, 24).map { mix[it].id })
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

    @Test fun sharedCurationRejectsWeakAndIncompleteEntriesButAllowsNewPremieres() {
        val good = movie(1).copy(voteCount = 100, score = 7.5)
        val weak = good.copy(id = 2, score = 4.0)
        val oldUnproven = good.copy(id = 3, releaseDate = "2000-01-01", voteCount = 1)
        val premiere = movie(4, "2026-09-27").copy(voteCount = 0, score = 0.0)
        val noSynopsis = good.copy(id = 5, overview = "")
        val noArt = good.copy(id = 6, posterUrl = null, backdropUrl = null)
        val future = good.copy(id = 7, releaseDate = "2026-12-18")
        assertEquals(listOf(good, premiere), curatedSectionItems("Popular movies",
            listOf(good, weak, oldUnproven, premiere, noSynopsis, noArt, future), today))
        assertEquals(listOf(good.copy(voteCount = 200)), curatedSectionItems("Top rated movies",
            listOf(good.copy(voteCount = 200)), today))
    }

    @Test fun seriesFocusesOnRatedShowsAndAllCategoriesHaveBoundedPaging() {
        val drama = movie(1).copy(mediaType = "tv", genreIds = listOf(18))
        val talk = drama.copy(id = 2, genreIds = listOf(10767))
        val soap = drama.copy(id = 3, genreIds = listOf(10766))
        assertEquals(listOf(drama), curatedSectionItems("Popular series", listOf(drama, talk, soap), today))
        CatalogRepository.specs.forEach { spec ->
            val paths = catalogPaths(spec, today)
            assertEquals(3, paths.size)
            assertEquals(3, paths.distinct().size)
            assertTrue(paths.all { it.count { c -> c == '?' } == 1 })
        }
        assertEquals(40, curatedSectionItems("Popular movies", (1..60).map { movie(it) }, today).size)
    }

    @Test fun discoveryHandlesMissingArtDatesAndSingleSource() {
        val items = listOf(movie(1), movie(2).copy(releaseDate = ""), movie(3).copy(backdropUrl = null, posterUrl = null), movie(4, "2026-12-18"))
        assertEquals(listOf(movie(1)), discoveryMovies(CatalogResult(listOf(CatalogSection(2, "Popular movies", items)), false), today))
        assertTrue(discoveryMovies(CatalogResult(emptyList(), true), today).isEmpty())
    }
}
