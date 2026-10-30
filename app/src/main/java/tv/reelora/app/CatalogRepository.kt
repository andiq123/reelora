package tv.reelora.app

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Collections
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

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
    val genreIds: List<Int> = emptyList(),
    val popularity: Double = 0.0,
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
@Immutable data class WeatherNow(val temperature: Int, val code: Int, val isDay: Boolean = true)
@Immutable data class FootballMatch(val home: String, val away: String, val date: String, val time: String, val homeScore: Int?, val awayScore: Int?, val status: String = "", val competition: String = "", val priority: Int = 99, val homeId: String = "", val awayId: String = "", val homeCountry: String = "", val awayCountry: String = "", val kickoff: Instant? = null)
@Immutable data class FootballHint(val competition: String, val days: Int)
@Immutable data class FootballSnapshot(val live: FootballMatch?, val next: FootballMatch?, val previous: FootballMatch?, val hint: FootballHint? = null)
@Immutable
data class WeatherPlace(
    val name: String,
    val area: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
) {
    val label = listOf(name, area, country).filter(String::isNotBlank).distinct().joinToString(", ")
}

object WeatherRepository {
    private val coordinates = boundedCache<String, Pair<Double, Double>>(8)
    suspend fun locations(query: String): Result<List<WeatherPlace>> = requestResult {
        val name = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val results = readJson("https://geocoding-api.open-meteo.com/v1/search?name=$name&count=5&language=en")
            .optJSONArray("results") ?: return@requestResult emptyList()
        buildList {
            for (index in 0 until results.length()) results.getJSONObject(index).let { place ->
                add(
                    WeatherPlace(
                        place.optString("name"),
                        place.optString("admin1"),
                        place.optString("country"),
                        place.getDouble("latitude"),
                        place.getDouble("longitude"),
                    )
                )
            }
        }
    }

    suspend fun current(location: String, celsius: Boolean, latitude: Double? = null, longitude: Double? = null): WeatherNow? = requestResult {
        val place = if (latitude != null && longitude != null) latitude to longitude else {
            coordinates[location] ?: locations(location).getOrNull()?.firstOrNull()?.let {
                (it.latitude to it.longitude).also { point -> coordinates[location] = point }
            } ?: return null
        }
        val unit = if (celsius) "celsius" else "fahrenheit"
        val current = readJson(
            "https://api.open-meteo.com/v1/forecast?latitude=${place.first}&longitude=${place.second}&current=temperature_2m,weather_code,is_day&temperature_unit=$unit",
        ).getJSONObject("current")
        WeatherNow(current.getDouble("temperature_2m").toInt(), current.getInt("weather_code"), current.optInt("is_day", 1) == 1)
    }.getOrNull()
}

object FootballRepository {
    private const val BASE = "https://www.thesportsdb.com/api/v1/json/123"
    private val countries = boundedCache<String, String>(256)

    suspend fun teamCountry(id: String): String {
        if (id.isBlank() || !id.all(Char::isDigit)) return ""
        countries[id]?.let { return it }
        return kotlinx.coroutines.withTimeoutOrNull(2_000L) {
            requestResult {
                readJson("$BASE/lookupteam.php?id=$id").optJSONArray("teams")
                    ?.optJSONObject(0)?.optString("strCountry").orEmpty().takeIf { it != "null" }.orEmpty()
            }.getOrNull()?.takeIf(String::isNotBlank)?.also { countries[id] = it }
        }.orEmpty()
    }

    private val leagues = listOf("4429" to "WORLD CUP", "4480" to "CHAMPIONS LEAGUE", "4328" to "PREMIER LEAGUE")

    suspend fun load(): FootballSnapshot? = withContext(Dispatchers.Default) {
        val now = Instant.now()
        val today = now.atOffset(ZoneOffset.UTC).toLocalDate()
        // A match that kicked off before UTC midnight can still be in play.
        val liveDays = if (now.atOffset(ZoneOffset.UTC).hour < 4) listOf(today.minusDays(1), today) else listOf(today)
        val matches = leagues.mapIndexed { priority, (id, name) ->
            async {
                coroutineScope {
                    val live = async {
                        liveDays.map { day -> async {
                            requestResult { readJson("$BASE/eventsday.php?d=$day&l=$id").footballMatches(name, priority) }.getOrNull().orEmpty()
                        } }.awaitAll().flatten()
                    }
                    val next = async { requestResult { readJson("$BASE/eventsnextleague.php?id=$id").footballMatches(name, priority) }.getOrNull().orEmpty() }
                    val previous = async { requestResult { readJson("$BASE/eventspastleague.php?id=$id").footballMatches(name, priority) }.getOrNull().orEmpty() }
                    listOf(live.await(), next.await(), previous.await())
                }
            }
        }.awaitAll()
        val live = matches.flatMap { it[0] }.filter { isLiveFootballMatch(it, now) }
            .minByOrNull(FootballMatch::priority)
        val upcoming = matches.flatMap { it[0] + it[1] }.filter { isUpcomingFootballMatch(it, now) }
        val next = selectNextFootballMatch(upcoming)
        val previous = selectPreviousFootballMatch(matches.flatMap { it[0] + it[2] }, now)
        val hint = upcoming.mapNotNull { match ->
            footballMatchDay(match)?.let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it) }
                ?.takeIf { it in 0..90 }?.let { FootballHint(match.competition, it.toInt()) }
        }.minWithOrNull(compareBy<FootballHint> { hint -> leagues.indexOfFirst { it.second == hint.competition } }.thenBy(FootballHint::days))
        FootballSnapshot(live, next, previous, hint).takeIf { it.live != null || it.next != null || it.previous != null }
    }
}

// Poll every two minutes only around play: from 15 minutes before a known kickoff until
// three hours after it. Before that, sleep until the window opens (at most 30 minutes).
internal fun footballRefreshInterval(snapshot: FootballSnapshot, now: Instant = Instant.now()): Long {
    val fast = 2 * 60_000L
    if (snapshot.live != null) return fast
    snapshot.next?.kickoff?.let { kickoff ->
        val untilWindow = java.time.Duration.between(now, kickoff.minusSeconds(15 * 60)).toMillis()
        val sinceKickoff = java.time.Duration.between(kickoff, now).toMillis()
        return when {
            untilWindow > 0 -> untilWindow.coerceIn(fast, 30 * 60_000L)
            sinceKickoff < 3 * 60 * 60_000L -> fast
            else -> 30 * 60_000L
        }
    }
    return if (snapshot.next?.let(::footballMatchDay) == LocalDate.now()) fast else 10 * 60_000L
}

internal fun JSONObject.footballMatches(competition: String = "", priority: Int = 99): List<FootballMatch> {
    val events = optJSONArray("events") ?: return emptyList()
    return (0 until events.length()).mapNotNull { events.optJSONObject(it)?.footballMatch(competition, priority) }
}

private fun JSONObject.footballMatch(competition: String, priority: Int): FootballMatch? {
    val home = text("strHomeTeam")
    val away = text("strAwayTeam")
    if (home.isBlank() || away.isBlank()) return null
    val kickoff = footballKickoff(text("strTimestamp"), text("dateEvent"), text("strTime"))
    return FootballMatch(
        home = home,
        away = away,
        date = kickoff?.atOffset(ZoneOffset.UTC)?.toLocalDate()?.toString()
            ?: runCatching { LocalDate.parse(text("dateEvent")) }.getOrNull()?.toString().orEmpty(),
        time = kickoff?.atOffset(ZoneOffset.UTC)?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty(),
        homeScore = text("intHomeScore").toIntOrNull()?.takeIf { it >= 0 },
        awayScore = text("intAwayScore").toIntOrNull()?.takeIf { it >= 0 },
        status = text("strStatus").ifBlank { text("strProgress") },
        competition = competition,
        priority = priority,
        homeId = text("idHomeTeam"),
        awayId = text("idAwayTeam"),
        homeCountry = text("strHomeTeamCountry"),
        awayCountry = text("strAwayTeamCountry"),
        kickoff = kickoff,
    )
}

internal fun footballKickoff(timestamp: String, date: String, time: String): Instant? =
    runCatching { OffsetDateTime.parse(timestamp).toInstant() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(timestamp).toInstant(ZoneOffset.UTC) }.getOrNull()
        ?: runCatching { LocalDate.parse(date).atTime(LocalTime.parse(time)).toInstant(ZoneOffset.UTC) }.getOrNull()

internal fun footballMatchDay(match: FootballMatch, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
    match.kickoff?.atZone(zone)?.toLocalDate() ?: runCatching { LocalDate.parse(match.date) }.getOrNull()

private fun footballMatchOrder(match: FootballMatch): Instant? = match.kickoff
    ?: runCatching { LocalDate.parse(match.date).atTime(LocalTime.parse(match.time.ifBlank { "00:00" })).toInstant(ZoneOffset.UTC) }.getOrNull()

private fun isUpcomingFootballMatch(match: FootballMatch, now: Instant): Boolean =
    match.status.trim().lowercase(Locale.ROOT) in setOf("", "ns", "not started", "tbd", "time to be defined") &&
        (match.kickoff?.let { !it.isBefore(now) }
            ?: (footballMatchDay(match)?.let { !it.isBefore(now.atZone(ZoneId.systemDefault()).toLocalDate()) } == true))

internal fun selectNextFootballMatch(matches: List<FootballMatch>, now: Instant = Instant.now()) =
    matches.filter { isUpcomingFootballMatch(it, now) }
        .minWithOrNull(compareBy<FootballMatch> { footballMatchOrder(it) }.thenBy(FootballMatch::priority))

internal fun selectPreviousFootballMatch(matches: List<FootballMatch>, now: Instant = Instant.now()) =
    matches.filter {
        val status = it.status.trim().lowercase(Locale.ROOT)
        val finished = status in setOf("ft", "aet", "pen", "finished", "match finished", "final") ||
            (status.isBlank() && it.homeScore != null && it.awayScore != null)
        finished && footballMatchOrder(it)?.let { kickoff -> !kickoff.isAfter(now) } == true
    }.maxWithOrNull(compareBy<FootballMatch> { footballMatchOrder(it) }.thenBy { -it.priority })

internal fun isLiveFootballStatus(status: String): Boolean {
    val value = status.trim().lowercase(Locale.ROOT)
    // Soccer states: https://www.thesportsdb.com/docs_api_data_guide.php
    val minute = Regex("(\\d{1,3})(?:\\+\\d{1,2})?['′]?").matchEntire(value)?.groupValues?.get(1)?.toIntOrNull()
    return value in setOf("1h", "ht", "2h", "et", "bt", "p", "live", "in progress", "first half", "second half", "half time", "halftime", "extra time") ||
        minute in 1..150
}

internal fun isLiveFootballMatch(match: FootballMatch, now: Instant): Boolean = isLiveFootballStatus(match.status) &&
    (match.kickoff?.let { java.time.Duration.between(it, now).seconds in 0..(4 * 60 * 60) }
        ?: (runCatching { LocalDate.parse(match.date) }.getOrNull() == now.atOffset(ZoneOffset.UTC).toLocalDate()))

object CatalogRepository {
    private val personCredits = boundedCache<Int, List<MediaItem>>(24)
    private val mediaDetails = boundedCache<String, MediaDetails>(48)
    @Volatile private var cachedCatalog: Pair<LocalDate, CatalogResult>? = null
    @Volatile private var cachedAt = 0L
    private val catalogRequests = Semaphore(4)
    val pageTitles = listOf("Discover", "In cinemas", "Movies", "TV series", "Animation")
    val configured get() = BuildConfig.TMDB_TOKEN.isNotBlank()

    val specs = listOf(
        CatalogSpec(0, "Trending this week", "/trending/movie/week", "movie"),
        CatalogSpec(1, "Now in cinemas", "/movie/now_playing", "movie"),
        CatalogSpec(1, "Coming soon", "/movie/upcoming", "movie"),
        CatalogSpec(2, "Popular movies", "/movie/popular", "movie"),
        CatalogSpec(2, "Top rated movies", "/movie/top_rated", "movie"),
        CatalogSpec(3, "Popular series", "/tv/popular", "tv"),
        CatalogSpec(4, "Popular animation", "/discover/movie?with_genres=16&sort_by=popularity.desc", "movie"),
    )

    fun clearDetailCaches() {
        personCredits.clear()
        mediaDetails.clear()
    }

    fun freshCatalog(): CatalogResult? = cachedCatalog?.takeIf {
        it.first == LocalDate.now() && System.nanoTime() - cachedAt < TimeUnit.HOURS.toNanos(6)
    }?.second

    suspend fun load(): CatalogResult = withContext(Dispatchers.Default) {
        val today = LocalDate.now()
        freshCatalog()?.let { return@withContext it }
        val token = BuildConfig.TMDB_TOKEN.trim()
        if (token.isEmpty()) return@withContext fallback()

        requestResult {
            val sections = coroutineScope {
                specs.map { spec -> async { requestResult { fetch(spec, token, today) }.getOrNull() } }.awaitAll().filterNotNull()
            }
            CatalogResult(
                specs.mapNotNull { spec -> sections.firstOrNull { it.title == spec.title }
                    ?: cachedCatalog?.second?.sections?.firstOrNull { it.title == spec.title }?.let { it.copy(items = curatedSectionItems(it.title, it.items, today)) } },
                sections.size < specs.size,
            )
        }.getOrElse { fallback() }.also { if (!it.isDemo) { cachedCatalog = today to it; cachedAt = System.nanoTime() } }
    }

    suspend fun details(item: MediaItem): MediaDetails = detailsOrNull(item) ?: fallbackDetails()

    /** Null means the request failed and is worth retrying; it is never cached. */
    suspend fun detailsOrNull(item: MediaItem): MediaDetails? {
        val key = "${item.mediaType}-${item.id}"
        mediaDetails[key]?.let { return it }
        return withContext(Dispatchers.Default) {
        val token = BuildConfig.TMDB_TOKEN.trim()
        if (token.isEmpty()) return@withContext fallbackDetails()

        requestResult {
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
                runtime = formatRuntime(runtimeMinutes),
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
        }.onSuccess { mediaDetails[key] = it }.getOrNull()
        }
    }

    suspend fun search(query: String): List<MediaItem> = searchResult(query).getOrDefault(emptyList())

    suspend fun searchResult(query: String): Result<List<MediaItem>> = withContext(Dispatchers.Default) {
        val term = query.trim()
        if (term.length < 2) return@withContext Result.success(emptyList())
        requestResult {
            val token = BuildConfig.TMDB_TOKEN.trim()
            check(token.isNotEmpty()) { "Search is not configured" }
            val encoded = URLEncoder.encode(term, StandardCharsets.UTF_8.toString())
            parseItems(getJson("/search/multi?query=$encoded", token).getJSONArray("results"), "movie")
        }
    }

    suspend fun credits(personId: Int): List<MediaItem> = withContext(Dispatchers.Default) {
        if (personId <= 0) return@withContext emptyList()
        personCredits[personId]?.let { return@withContext it }
        val token = BuildConfig.TMDB_TOKEN.trim()
        if (token.isEmpty()) return@withContext emptyList()

        val result = requestResult {
            parseItems(getJson("/person/$personId/combined_credits", token).getJSONArray("cast"), "movie", 100)
                .distinctBy { "${it.mediaType}-${it.id}" }
                .sortedByDescending { it.voteCount }
                .take(12)
        }.onSuccess { personCredits[personId] = it }.getOrDefault(emptyList())
        result
    }

    private suspend fun fetch(spec: CatalogSpec, token: String, today: LocalDate): CatalogSection {
        val paths = catalogPaths(spec, today)
        val items = coroutineScope {
            paths.map { path -> async {
                catalogRequests.withPermit {
                    parseItems(getJson(path, token).getJSONArray("results"), spec.mediaType, Int.MAX_VALUE)
                }
            } }.awaitAll().flatten()
        }
        return CatalogSection(spec.page, spec.title, curatedSectionItems(spec.title, items, today))
    }

    private fun parseItems(results: JSONArray, defaultType: String, limit: Int = 18) = buildList<MediaItem> {
            for (index in 0 until results.length()) {
                val item = results.getJSONObject(index)
                val type = item.optString("media_type", defaultType)
                if (type !in setOf("movie", "tv") || item.optBoolean("adult")) continue
                val title = item.text("title").ifBlank { item.text("name") }
                if (title.isBlank()) continue
                val date = item.text("release_date").ifBlank { item.text("first_air_date") }
                if (any { it.id == item.optInt("id") && it.mediaType == type }) continue
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
                        popularity = item.optDouble("popularity", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                        genreIds = item.optJSONArray("genre_ids")?.let { ids ->
                            (0 until ids.length()).map { ids.optInt(it) }.filter { it > 0 }.distinct()
                        }.orEmpty(),
                    )
                )
                if (size == limit) break
            }
        }

    private suspend fun getJson(path: String, token: String) = readJson(
        "https://api.themoviedb.org/3$path${if ('?' in path) '&' else '?'}language=en-US&include_adult=false",
        token,
    )

    private fun JSONObject.image(key: String, size: String) =
        optString(key).takeIf { it.isNotBlank() && it != "null" }?.let { "https://image.tmdb.org/t/p/$size$it" }

    private fun fallback() = CatalogResult(emptyList(), true)

    private fun fallbackDetails() = MediaDetails("", "", emptyList(), emptyList(), null, null, emptyList())

}

private var httpCacheDirectory: java.io.File? = null

/** Called once from Application before any request; tests run without a disk cache. */
internal fun initHttpCache(cacheDirectory: java.io.File) {
    httpCacheDirectory = cacheDirectory
}

// Reuse connections and cancel the socket when a search or screen is abandoned.
// TMDB responses are public for about six hours, so a relaunch serves Home from disk.
internal val apiClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .apply { httpCacheDirectory?.let { cache(Cache(it.resolve("http"), 10L * 1024 * 1024)) } }
        .build()
}

internal suspend fun readJson(url: String, token: String? = null): JSONObject =
    suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url).header("Accept", "application/json")
        token?.let { request.header("Authorization", "Bearer $it") }
        val call = apiClient.newCall(request.build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resumeWith(requestResult {
                    response.use {
                        check(it.isSuccessful) { "Request failed: ${it.code}" }
                        JSONObject(checkNotNull(it.body).string())
                    }
                })
            }
        })
    }

internal inline fun <T> requestResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}

// Entry counts bound metadata retained by this long-lived launcher process.
internal fun <K, V> boundedCache(capacity: Int): MutableMap<K, V> =
    Collections.synchronizedMap(object : LinkedHashMap<K, V>(capacity, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > capacity
    })

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

private fun JSONObject.text(key: String) = optString(key).takeUnless { it == "null" }.orEmpty()

internal fun catalogPath(spec: CatalogSpec, today: LocalDate): String = if (spec.title == "Coming soon") {
    "/discover/movie?primary_release_date.gte=${today.plusDays(1)}&primary_release_date.lte=${today.plusMonths(12)}&sort_by=popularity.desc&include_video=false"
} else spec.path

internal fun catalogPaths(spec: CatalogSpec, today: LocalDate): List<String> {
    val path = catalogPath(spec, today)
    // Walk the 360 highest-rated films over six days instead of repeating pages 1-3.
    val first = if (spec.title == "Top rated movies") 1 + Math.floorMod(today.toEpochDay(), 6L).toInt() * 3 else 1
    return (first until first + 3).map { "$path${if ('?' in path) '&' else '?'}page=$it" }
}

// Rank first, then arrange the shortlist chronologically. Upcoming films need no votes yet.
internal fun curatedComingSoon(items: List<MediaItem>): List<MediaItem> = items
    .distinctBy { "${it.mediaType}-${it.id}" }
    .filter { it.popularity.isFinite() && it.popularity > 0 &&
        (!it.posterUrl.isNullOrBlank() || !it.backdropUrl.isNullOrBlank()) }
    .sortedByDescending(MediaItem::popularity)
    .take(40)
    .sortedBy(MediaItem::releaseDate)

internal fun filterSectionItems(title: String, items: List<MediaItem>, today: LocalDate): List<MediaItem> =
    items.distinctBy { "${it.mediaType}-${it.id}" }.filter { item ->
        val date = runCatching { LocalDate.parse(item.releaseDate) }.getOrNull()
        when (title) {
            "Coming soon" -> item.mediaType == "movie" && date != null && date.isAfter(today) && !date.isAfter(today.plusMonths(12))
            // TMDB's now-playing feed includes anniversary re-releases of decades-old films.
            "Now in cinemas" -> item.mediaType == "movie" && date != null && !date.isAfter(today) &&
                !date.isBefore(today.minusDays(100))
            "Top rated movies", "Popular movies" -> item.mediaType == "movie" && date != null && !date.isAfter(today)
            "Popular series" -> item.mediaType == "tv"
            "Popular animation" -> item.mediaType == "movie" && 16 in item.genreIds
            else -> true
        }
    }.let { filtered ->
        if (title == "Coming soon") filtered.sortedBy(MediaItem::releaseDate) else filtered
    }

internal fun mediaRating(item: MediaItem): String =
    // A handful of early votes produces misleading 9+ averages.
    if (item.voteCount < 20 || !item.score.isFinite() || item.score <= 0.0) "Not rated"
    else "★ ${"%.1f".format(Locale.US, item.score)}"

internal fun formatRuntime(minutes: Int): String = when {
    minutes <= 0 -> ""
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
