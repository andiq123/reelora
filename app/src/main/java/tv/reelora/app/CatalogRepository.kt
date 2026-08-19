package tv.reelora.app

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class MediaItem(
    val id: Int,
    val title: String,
    val overview: String,
    val year: String,
    val score: Double,
    val voteCount: Int,
    val mediaType: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String = "",
)

@Immutable data class CastMember(val id: Int, val name: String, val character: String, val profileUrl: String?)
@Immutable data class Trailer(val key: String, val name: String, val official: Boolean)
@Immutable data class WatchProvider(val name: String, val logoUrl: String?)
@Immutable
data class WatchAvailability(
    val region: String,
    val streaming: List<WatchProvider>,
    val rentOrBuy: List<WatchProvider>,
)
@Immutable
data class MediaDetails(
    val runtime: String,
    val genres: String,
    val cast: List<CastMember>,
    val similar: List<MediaItem>,
    val trailer: Trailer?,
    val availability: WatchAvailability?,
    val backdrops: List<String>,
)
@Immutable data class CatalogSection(val page: Int, val title: String, val items: List<MediaItem>)
@Immutable data class CatalogResult(val sections: List<CatalogSection>, val isDemo: Boolean)
@Immutable data class CatalogSpec(val page: Int, val title: String, val path: String, val mediaType: String)

object CatalogRepository {
    private val personCredits = ConcurrentHashMap<Int, List<MediaItem>>()
    private val mediaDetails = ConcurrentHashMap<String, MediaDetails>()
    private val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    private val recentDate = LocalDate.now().minusMonths(8).format(DateTimeFormatter.ISO_DATE)

    val pageTitles = listOf("Discover", "In cinemas", "Movies", "TV series", "Animation")

    val specs = listOf(
        CatalogSpec(0, "Trending today", "/trending/all/day", "movie"),
        CatalogSpec(0, "Trending this week", "/trending/all/week", "movie"),
        CatalogSpec(0, "Highly rated", "/discover/movie?sort_by=vote_average.desc&vote_count.gte=1000", "movie"),
        CatalogSpec(0, "Hidden gems", "/discover/movie?sort_by=vote_average.desc&vote_count.gte=250&vote_count.lte=1500", "movie"),

        CatalogSpec(1, "Now in cinemas", "/movie/now_playing", "movie"),
        CatalogSpec(1, "Coming soon", "/movie/upcoming", "movie"),
        CatalogSpec(1, "Popular new releases", "/discover/movie?sort_by=popularity.desc&primary_release_date.gte=$recentDate&primary_release_date.lte=$today", "movie"),
        CatalogSpec(1, "More coming soon", "/movie/upcoming?page=2", "movie"),

        CatalogSpec(2, "Popular movies", "/movie/popular", "movie"),
        CatalogSpec(2, "Top rated movies", "/movie/top_rated", "movie"),
        CatalogSpec(2, "Action & adventure", "/discover/movie?with_genres=28|12&sort_by=popularity.desc", "movie"),
        CatalogSpec(2, "Science fiction", "/discover/movie?with_genres=878&sort_by=popularity.desc", "movie"),

        CatalogSpec(3, "Popular series", "/tv/popular", "tv"),
        CatalogSpec(3, "Airing today", "/tv/airing_today", "tv"),
        CatalogSpec(3, "Currently on air", "/tv/on_the_air", "tv"),
        CatalogSpec(3, "Top rated series", "/tv/top_rated", "tv"),

        CatalogSpec(4, "Popular animation", "/discover/movie?with_genres=16&sort_by=popularity.desc", "movie"),
        CatalogSpec(4, "For the whole family", "/discover/movie?with_genres=16,10751&sort_by=popularity.desc", "movie"),
        CatalogSpec(4, "Anime movies", "/discover/movie?with_genres=16&with_origin_country=JP&sort_by=popularity.desc", "movie"),
        CatalogSpec(4, "Animated series", "/discover/tv?with_genres=16&sort_by=popularity.desc", "tv"),
    )

    suspend fun load(): CatalogResult = withContext(Dispatchers.Default) {
        val token = BuildConfig.TMDB_TOKEN.trim()
        if (token.isEmpty()) return@withContext fallback()

        runCatching {
            val sections = coroutineScope {
                specs.map { spec -> async { runCatching { fetch(spec, token) }.getOrNull() } }.awaitAll().filterNotNull()
            }
            val demo = fallback().sections
            CatalogResult(
                pageTitles.indices.flatMap { page -> sections.filter { it.page == page }.ifEmpty { demo.filter { it.page == page } } },
                sections.isEmpty(),
            )
        }.getOrElse { fallback() }
    }

    suspend fun details(item: MediaItem): MediaDetails {
        val key = "${item.mediaType}-${item.id}"
        mediaDetails[key]?.let { return it }
        return withContext(Dispatchers.Default) {
        val token = BuildConfig.TMDB_TOKEN.trim()
        if (token.isEmpty()) return@withContext fallbackDetails(item)

        runCatching {
            val creditsKey = if (item.mediaType == "tv") "aggregate_credits" else "credits"
            val json = getJson("/${item.mediaType}/${item.id}?append_to_response=$creditsKey,recommendations,videos,watch/providers,images&include_image_language=en,null", token)
            val runtimeMinutes = if (item.mediaType == "tv") {
                json.optJSONArray("episode_run_time")?.optInt(0) ?: 0
            } else {
                json.optInt("runtime")
            }
            val genres = json.optJSONArray("genres")?.let { values ->
                buildList { for (index in 0 until values.length()) add(values.getJSONObject(index).getString("name")) }
                    .take(3)
                    .joinToString(" · ")
            }.orEmpty()
            val castValues = json.optJSONObject(creditsKey)?.optJSONArray("cast")
            val cast = buildList {
                if (castValues != null) for (index in 0 until minOf(castValues.length(), 12)) {
                    val person = castValues.getJSONObject(index)
                    val character = person.optString("character").ifBlank {
                        person.optJSONArray("roles")?.optJSONObject(0)?.optString("character").orEmpty()
                    }
                    add(CastMember(person.getInt("id"), person.getString("name"), character, person.image("profile_path", "w185")))
                }
            }
            val trailers = buildList {
                val videos = json.optJSONObject("videos")?.optJSONArray("results")
                if (videos != null) for (index in 0 until videos.length()) {
                    val video = videos.getJSONObject(index)
                    if (video.optString("site") == "YouTube" && video.optString("type") == "Trailer") {
                        add(Trailer(video.optString("key"), video.optString("name", "Official trailer"), video.optBoolean("official")))
                    }
                }
            }.filter { it.key.isNotBlank() }
            val region = Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"
            val providerRegion = json.optJSONObject("watch/providers")?.optJSONObject("results")?.optJSONObject(region)
            fun providers(vararg keys: String) = keys.flatMap { key ->
                providerRegion?.optJSONArray(key)?.let { values ->
                    buildList {
                        for (index in 0 until values.length()) {
                            val provider = values.getJSONObject(index)
                            add(WatchProvider(provider.optString("provider_name"), provider.image("logo_path", "w92")))
                        }
                    }
                }.orEmpty()
            }.distinctBy { it.name }.take(4)
            MediaDetails(
                runtime = if (runtimeMinutes > 0) "${runtimeMinutes} min" else "",
                genres = genres,
                cast = cast,
                similar = json.optJSONObject("recommendations")?.optJSONArray("results")
                    ?.let { parseItems(it, item.mediaType, 12) }.orEmpty(),
                trailer = trailers.firstOrNull { it.official } ?: trailers.firstOrNull(),
                availability = WatchAvailability(region, providers("flatrate", "free", "ads"), providers("rent", "buy")),
                backdrops = json.optJSONObject("images")?.optJSONArray("backdrops")?.let { images ->
                    buildList {
                        for (index in 0 until minOf(images.length(), 6)) {
                            images.getJSONObject(index).image("file_path", "w1280")?.let(::add)
                        }
                    }
                }.orEmpty(),
            )
        }.getOrElse { fallbackDetails(item) }
        }.also { mediaDetails[key] = it }
    }

    suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.Default) {
        val term = query.trim()
        val token = BuildConfig.TMDB_TOKEN.trim()
        if (term.length < 2 || token.isEmpty()) return@withContext emptyList()

        runCatching {
            val encoded = URLEncoder.encode(term, StandardCharsets.UTF_8.toString())
            parseItems(getJson("/search/multi?query=$encoded", token).getJSONArray("results"), "movie")
        }.getOrDefault(emptyList())
    }

    suspend fun credits(personId: Int): List<MediaItem> = withContext(Dispatchers.Default) {
        if (personId <= 0) return@withContext emptyList()
        personCredits[personId]?.let { return@withContext it }
        val token = BuildConfig.TMDB_TOKEN.trim()
        if (token.isEmpty()) return@withContext emptyList()

        val result = runCatching {
            parseItems(getJson("/person/$personId/combined_credits", token).getJSONArray("cast"), "movie", 100)
                .distinctBy { "${it.mediaType}-${it.id}" }
                .sortedByDescending { it.voteCount }
                .take(12)
        }.getOrDefault(emptyList())
        personCredits[personId] = result
        result
    }

    private suspend fun fetch(spec: CatalogSpec, token: String): CatalogSection {
        val results = getJson(spec.path, token).getJSONArray("results")
        return CatalogSection(spec.page, spec.title, parseItems(results, spec.mediaType))
    }

    private fun parseItems(results: JSONArray, defaultType: String, limit: Int = 18) = buildList {
            for (index in 0 until results.length()) {
                val item = results.getJSONObject(index)
                val type = item.optString("media_type", defaultType)
                if (type == "person") continue
                val title = item.optString("title").ifBlank { item.optString("name") }
                if (title.isBlank()) continue
                val date = item.optString("release_date").ifBlank { item.optString("first_air_date") }
                add(
                    MediaItem(
                        id = item.getInt("id"),
                        title = title,
                        overview = item.optString("overview").ifBlank { "Discover this title on Reelora TV." },
                        year = date.take(4).ifBlank { "New" },
                        score = item.optDouble("vote_average", 0.0),
                        voteCount = item.optInt("vote_count"),
                        mediaType = type,
                        posterUrl = item.image("poster_path", "w342"),
                        backdropUrl = item.image("backdrop_path", "w1280"),
                        releaseDate = date,
                    )
                )
                if (size == limit) break
            }
        }

    private suspend fun getJson(path: String, token: String) = withContext(Dispatchers.IO) {
        val separator = if ('?' in path) '&' else '?'
        val connection = URL("https://api.themoviedb.org/3$path${separator}language=en-US&include_adult=false")
            .openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")

        try {
            check(connection.responseCode in 200..299) { "TMDB returned ${connection.responseCode}" }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.image(key: String, size: String) =
        optString(key).takeIf { it.isNotBlank() && it != "null" }?.let { "https://image.tmdb.org/t/p/$size$it" }

    private fun fallback(): CatalogResult {
        val demos = listOf(
            MediaItem(1, "Midnight Signal", "A mysterious transmission pulls a quiet coastal town into an impossible adventure.", "2026", 8.4, 1842, "movie", null, null),
            MediaItem(2, "Orbital", "A rescue crew races the sunrise above Earth.", "2026", 8.1, 936, "movie", null, null),
            MediaItem(3, "The Last Archive", "Two investigators uncover stories that were meant to disappear.", "2025", 7.9, 2411, "tv", null, null),
            MediaItem(4, "Paper Kingdom", "An inventive animated journey through a world folded from memories.", "2026", 8.7, 3204, "movie", null, null),
            MediaItem(5, "Northbound", "A family road trip becomes a warm, unexpected second chance.", "2025", 7.8, 714, "movie", null, null),
            MediaItem(6, "Afterlight", "Survivors build a new city where night never fully arrives.", "2026", 8.2, 1630, "tv", null, null),
            MediaItem(7, "Static Summer", "Old friends reunite when their forgotten radio show returns on air.", "2025", 7.6, 583, "movie", null, null),
            MediaItem(8, "Tiny Giants", "Small creatures embark on a very big animated expedition.", "2026", 8.5, 1206, "movie", null, null),
        )
        return CatalogResult(
            specs.mapIndexed { index, spec -> CatalogSection(spec.page, spec.title, List(demos.size) { demos[(it + index) % demos.size] }) },
            true,
        )
    }

    private fun fallbackDetails(item: MediaItem) = MediaDetails(
        runtime = if (item.mediaType == "tv") "45 min episodes" else "112 min",
        genres = if (item.mediaType == "tv") "Drama · Mystery" else "Adventure · Drama",
        cast = listOf(
            CastMember(0, "Alex Morgan", "Lead", null),
            CastMember(0, "Maya Chen", "Co-star", null),
            CastMember(0, "Noah Williams", "Supporting", null),
            CastMember(0, "Sofia Reyes", "Supporting", null),
            CastMember(0, "Leo Martin", "Guest", null),
        ),
        similar = emptyList(),
        trailer = null,
        availability = null,
        backdrops = emptyList(),
    )
}

internal fun releaseLabel(date: String, today: LocalDate = LocalDate.now()): String {
    val release = runCatching { LocalDate.parse(date) }.getOrNull() ?: return "DATE TBA"
    val formatted = release.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)).uppercase(Locale.US)
    return when {
        release.isAfter(today) -> "◷ COMING $formatted"
        release == today -> "● RELEASES TODAY"
        else -> "✓ RELEASED $formatted"
    }
}

internal fun cardReleaseLabel(date: String, today: LocalDate = LocalDate.now()): String? =
    when (val label = releaseLabel(date, today)) {
        "DATE TBA" -> label
        else -> label.takeIf { it.startsWith("◷ COMING ") }
            ?.removePrefix("◷ COMING ")
            ?.substringBefore(',')
            ?.let { "◷ $it" }
    }
