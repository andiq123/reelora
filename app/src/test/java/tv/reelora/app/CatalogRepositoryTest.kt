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
        assertEquals(5, specs.map { it.title }.distinct().size)
        assertTrue(specs.single { it.title == "Animation" }.path.contains("with_genres=16"))
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
    fun releaseBadgeDistinguishesUpcomingAndReleasedTitles() {
        val today = LocalDate.of(2026, 8, 18)
        assertTrue(releaseLabel("2026-09-04", today).startsWith("◷ COMING"))
        assertEquals("● RELEASES TODAY", releaseLabel("2026-08-18", today))
        assertTrue(releaseLabel("2026-06-17", today).startsWith("✓ RELEASED"))
        assertEquals("◷ SEP 4", cardReleaseLabel("2026-09-04", today))
        assertNull(cardReleaseLabel("2026-06-17", today))
    }
}
