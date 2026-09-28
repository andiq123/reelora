package tv.reelora.app

import java.time.LocalDate

private fun completeMovieMetadata(item: MediaItem): Boolean =
    (!item.posterUrl.isNullOrBlank() || !item.backdropUrl.isNullOrBlank()) &&
        item.overview.isNotBlank() && item.overview != "Discover this title on Reelora TV."

/** Shared by fetched rows, cached fallback rows, discovery, and the hero's source pool. */
internal fun curatedSectionItems(title: String, items: List<MediaItem>, today: LocalDate): List<MediaItem> {
    val candidates = filterSectionItems(title, items, today).filter(::completeMovieMetadata)
    if (title == "Coming soon") return curatedComingSoon(candidates)
    return candidates.filter { item ->
        val date = runCatching { LocalDate.parse(item.releaseDate) }.getOrNull()
        val released = date != null && !date.isAfter(today)
        val credible = item.score.isFinite() && item.score >= 6.0 && item.voteCount >= 20
        val newRelease = released && date != null && !date.isBefore(today.minusDays(45)) &&
            item.voteCount < 20 && item.popularity.isFinite() && item.popularity > 0
        released && when (title) {
            "Top rated movies" -> item.voteCount >= 200 && item.score.isFinite() && item.score >= 7.0
            "Popular series" -> credible && item.genreIds.none { it == 10763 || it == 10764 || it == 10766 || it == 10767 }
            else -> item.mediaType == "movie" && (credible || newRelease)
        }
    }.take(40)
}

/** A bounded editorial mix, not a claim of personalized watch-history recommendations. */
internal fun discoveryMovies(catalog: CatalogResult, today: LocalDate = LocalDate.now()): List<MediaItem> {
    val released = listOf("Popular movies", "Trending this week", "Now in cinemas", "Top rated movies").map { title ->
        curatedSectionItems(title, catalog.sections.firstOrNull { it.title == title }?.items.orEmpty(), today)
    }
    val current = (0 until 40).flatMap { index -> released.mapNotNull { it.getOrNull(index) } }
        .distinctBy(::mediaKey).take(32)
    val upcoming = curatedSectionItems("Coming soon",
        catalog.sections.firstOrNull { it.title == "Coming soon" }?.items.orEmpty(), today)
        .sortedByDescending(MediaItem::popularity).take(8)
    if (current.isEmpty()) return upcoming
    return buildList {
        current.chunked(4).forEachIndexed { index, group ->
            addAll(group)
            upcoming.getOrNull(index)?.let(::add)
        }
    }.distinctBy(::mediaKey)
}
