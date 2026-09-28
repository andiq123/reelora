package tv.reelora.app

import java.time.LocalDate

/** A bounded editorial mix, not a claim of personalized watch-history recommendations. */
internal fun discoveryMovies(catalog: CatalogResult, today: LocalDate = LocalDate.now()): List<MediaItem> {
    fun usable(item: MediaItem) = item.mediaType == "movie" &&
        (item.backdropUrl != null || item.posterUrl != null)
    val released = listOf("Popular movies", "Trending this week", "Now in cinemas").map { title ->
        catalog.sections.firstOrNull { it.title == title }?.items.orEmpty().filter { item ->
            usable(item) && runCatching { !LocalDate.parse(item.releaseDate).isAfter(today) }.getOrDefault(false)
        }
    }
    val current = (0 until 20).flatMap { index -> released.mapNotNull { it.getOrNull(index) } }
        .distinctBy(::mediaKey).take(16)
    val upcoming = filterSectionItems("Coming soon",
        catalog.sections.firstOrNull { it.title == "Coming soon" }?.items.orEmpty(), today)
        .filter(::usable).sortedByDescending(MediaItem::popularity).take(4)
    if (current.isEmpty()) return upcoming
    return buildList {
        current.chunked(4).forEachIndexed { index, group ->
            addAll(group)
            upcoming.getOrNull(index)?.let(::add)
        }
    }.distinctBy(::mediaKey)
}
