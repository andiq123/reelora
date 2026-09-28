package tv.reelora.app

/** Coordinates are relative to the row viewport, not positions in the catalog. */
internal data class VisibleMovie(val index: Int, val offset: Int, val size: Int)

internal fun nearestVisibleMovie(
    anchor: Int,
    viewportStart: Int,
    viewportEnd: Int,
    items: List<VisibleMovie>,
): Int? {
    val visible = items.filter { it.offset < viewportEnd && it.offset + it.size > viewportStart }
    val complete = visible.filter { it.offset >= viewportStart && it.offset + it.size <= viewportEnd }
    return complete.ifEmpty { visible }.minByOrNull {
        kotlin.math.abs(it.offset + it.size / 2 - anchor)
    }?.index
}
