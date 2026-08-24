package tv.reelora.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.KeyEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Background = Color(0xFF080A0F)
private val Surface = Color(0xFF171A22)
private val Violet = Color(0xFF8DA2FF)
private val Coral = Color(0xFFFFB56B)
private val PanelBrush = Brush.verticalGradient(listOf(Color(0xF21D2029), Color(0xF20F1117)))
private val DialogShape = RoundedCornerShape(28.dp)
private val ControlShape = RoundedCornerShape(14.dp)
private val Gap = 12.dp
private val GapLarge = 24.dp
private val DialogPadding = 28.dp
private val RowBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val margin = 24f
        val end = offset + size
        if (offset >= margin && end <= containerSize - margin) return 0f
        return if (offset < margin) offset - margin else end - containerSize + margin
    }
}

private fun Modifier.activeTransform(
    scale: Float,
    translationY: Float = 0f,
    translationX: Float = 0f,
    alpha: Float = 1f,
) = if (scale == 1f && translationY == 0f && translationX == 0f && alpha == 1f) this else graphicsLayer {
    scaleX = scale
    scaleY = scale
    this.translationY = translationY
    this.translationX = translationX
    this.alpha = alpha
}

private data class TheaterFeature(val item: MediaItem, val trailer: Trailer)
internal enum class WeatherLoadState { Loading, Ready, Error }
@Immutable
private data class LauncherApp(
    val name: String,
    val component: ComponentName,
    val icon: android.graphics.drawable.Drawable,
    val banner: android.graphics.drawable.Drawable?,
)

class MainActivity : ComponentActivity() {
    private val inputEvents = Channel<Unit>(Channel.CONFLATED)
    private val foreground = MutableStateFlow(false)

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        inputEvents.trySend(Unit)
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReeloraApp(inputEvents, foreground) }
    }

    override fun onResume() {
        super.onResume()
        foreground.value = true
    }

    override fun onPause() {
        foreground.value = false
        super.onPause()
    }
}

@Composable
private fun ReeloraApp(inputEvents: Channel<Unit>, foreground: MutableStateFlow<Boolean>) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            secondary = Coral,
            background = Background,
            surface = Surface,
            onBackground = Color.White,
            onSurface = Color.White,
        )
    ) {
        var result by remember { mutableStateOf<CatalogResult?>(null) }
        var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
        var selected by remember { mutableStateOf<MediaItem?>(null) }
        var searching by remember { mutableStateOf(false) }
        var settingsOpen by remember { mutableStateOf(false) }
        var weatherLocationOpen by remember { mutableStateOf(false) }
        var hiddenAppsOpen by remember { mutableStateOf(false) }
        var configuredApp by remember { mutableStateOf<LauncherApp?>(null) }
        var editingApp by remember { mutableStateOf<LauncherApp?>(null) }
        var movingAppKey by remember { mutableStateOf<String?>(null) }
        var moveConfirmReady by remember { mutableStateOf(false) }
        var theater by remember { mutableStateOf<TheaterFeature?>(null) }
        var theaterReturn by remember { mutableStateOf<MediaItem?>(null) }
        var recentTheater by remember { mutableStateOf(emptyList<String>()) }
        var theaterOpen by remember { mutableStateOf(false) }
        val isForeground by foreground.collectAsState()
        val context = LocalContext.current
        val preferences = remember { context.getSharedPreferences("launcher", Context.MODE_PRIVATE) }
        var theaterEnabled by remember { mutableStateOf(preferences.getBoolean("theaterEnabled", true)) }
        var idleMinutes by remember {
            mutableStateOf(preferences.getInt("idleMinutes", 3).takeIf { it in THEATER_IDLE_OPTIONS } ?: 3)
        }
        var compactApps by remember { mutableStateOf(preferences.getBoolean("compactApps", false)) }
        var focusLift by remember { mutableStateOf(preferences.getBoolean("focusLift", true)) }
        var weatherLocation by remember { mutableStateOf(preferences.getString("weatherLocation", "Chișinău").orEmpty()) }
        var weatherCelsius by remember { mutableStateOf(preferences.getBoolean("weatherCelsius", true)) }
        var weatherLatitude by remember { mutableStateOf(preferences.getString("weatherLatitude", null)?.toDoubleOrNull()) }
        var weatherLongitude by remember { mutableStateOf(preferences.getString("weatherLongitude", null)?.toDoubleOrNull()) }
        var use24HourClock by remember { mutableStateOf(preferences.getBoolean("use24HourClock", true)) }
        var weather by remember { mutableStateOf<WeatherNow?>(null) }
        var weatherState by remember { mutableStateOf(WeatherLoadState.Loading) }
        var appOrder by remember {
            mutableStateOf(preferences.getString("appOrder", "").orEmpty().split(',').filter(String::isNotBlank))
        }
        var hiddenApps by remember {
            mutableStateOf(preferences.getStringSet("hiddenApps", emptySet()).orEmpty().toSet())
        }
        var customAppNames by remember {
            mutableStateOf(savedCustomAppNames(preferences.all))
        }
        val orderedApps = remember(apps, appOrder, customAppNames) {
            orderLauncherApps(apps, appOrder).map { app -> customAppNames[launcherAppKey(app)]?.let { app.copy(name = it) } ?: app }
        }
        val visibleApps = remember(orderedApps, hiddenApps) {
            orderedApps.filterNot { launcherAppKey(it) in hiddenApps }
        }
        fun currentAppOrder() = orderedAppKeys(apps.map(::launcherAppKey), appOrder)
        fun moveVisibleApp(app: LauncherApp, offset: Int): Int {
            val key = launcherAppKey(app)
            val fullOrder = currentAppOrder().toMutableList()
            val visibleKeys = fullOrder.filterNot { it in hiddenApps }
            val from = visibleKeys.indexOf(key)
            if (from < 0) return 0
            val destination = (from + offset).coerceIn(visibleKeys.indices)
            if (destination == from) return from
            val targetKey = visibleKeys[destination]
            val target = fullOrder.indexOf(targetKey)
            val source = fullOrder.indexOf(key)
            fullOrder[source] = targetKey
            fullOrder[target] = key
            appOrder = fullOrder
            preferences.edit().putString("appOrder", appOrder.joinToString(",")).apply()
            return destination
        }
        fun launchApp(app: LauncherApp) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_MAIN).setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure {
                android.widget.Toast.makeText(context, "Unable to open ${app.name}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        val theaterScope = rememberCoroutineScope()
        val theaterLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultCode ->
            theaterOpen = false
            val manualReturn = theaterReturn
            if (resultCode.resultCode == TrailerActivity.RESULT_UNAVAILABLE && manualReturn != null) {
                android.widget.Toast.makeText(context, "Trailer unavailable or blocked in your region", android.widget.Toast.LENGTH_LONG).show()
            }
            if (resultCode.resultCode in setOf(TrailerActivity.RESULT_FINISHED, TrailerActivity.RESULT_UNAVAILABLE) && manualReturn == null) {
                val catalog = result
                theaterScope.launch {
                    theater = catalog?.let {
                        findTheaterFeature(
                            launcherMovieSections(it).flatMap { section -> section.items },
                            recentTheater,
                        )
                    }
                    theater?.let { recentTheater = (recentTheater + mediaKey(it.item)).takeLast(10) }
                }
            } else {
                theater = null
                selected = manualReturn
                theaterReturn = null
            }
        }
        LaunchedEffect(Unit) {
            var retryDelay = 10_000L
            while (true) {
                val loaded = CatalogRepository.load()
                result = loaded
                if (!loaded.isDemo || !CatalogRepository.configured) break
                delay(retryDelay)
                retryDelay = nextCatalogRetryDelay(retryDelay)
            }
        }
        LaunchedEffect(weatherLocation, weatherCelsius, weatherLatitude, weatherLongitude) {
            weather = null
            weatherState = WeatherLoadState.Loading
            while (true) {
                val latest = WeatherRepository.current(weatherLocation, weatherCelsius, weatherLatitude, weatherLongitude)
                if (latest == null) weatherState = WeatherLoadState.Error else {
                    weather = latest
                    weatherState = WeatherLoadState.Ready
                }
                delay(if (latest == null) 60_000L else 30 * 60_000L)
            }
        }
        LaunchedEffect(isForeground) {
            if (isForeground) apps = withContext(Dispatchers.IO) { installedTvApps(context) }
        }
        LaunchedEffect(movingAppKey) {
            moveConfirmReady = false
            if (movingAppKey != null) {
                delay(300)
                moveConfirmReady = true
            }
        }

        LaunchedEffect(theater?.trailer?.key) {
            val feature = theater ?: return@LaunchedEffect
            val intent = Intent(context, TrailerActivity::class.java)
                .putExtra("videoId", feature.trailer.key)
                .putExtra("manual", theaterReturn != null)
                .putExtra("release", releaseLabel(feature.item.releaseDate).takeIf { it.startsWith("◷ COMING ") })
            if (theaterOpen) context.startActivity(intent) else {
                theaterOpen = true
                theaterLauncher.launch(intent)
            }
        }

        Box(
            Modifier.fillMaxSize().background(Background).onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val wasPlaying = theater != null
                theater = null
                if (wasPlaying) {
                    selected = theaterReturn
                    theaterReturn = null
                }
                wasPlaying
            },
        ) {
            val catalog = result
            if (catalog == null) {
                Loading()
            } else {
                Home(
                    catalog,
                    apps = visibleApps,
                    weather = weather,
                    weatherState = weatherState,
                    use24HourClock = use24HourClock,
                    compactApps = compactApps,
                    focusLift = focusLift,
                    onLaunch = ::launchApp,
                    onSearch = { searching = true },
                    onSettings = { settingsOpen = true },
                    onHiddenApps = { hiddenAppsOpen = true },
                    onConfigureApp = { configuredApp = it },
                    movingAppKey = movingAppKey,
                    moveConfirmReady = moveConfirmReady,
                    onMoveApp = ::moveVisibleApp,
                    onMoveDone = { movingAppKey = null },
                    onSelect = { selected = it },
                )
                if (searching) {
                    SearchDialog(
                        suggestions = catalog.sections.first().items.take(10),
                        onDismiss = { searching = false },
                        onSelect = {
                            searching = false
                            selected = it
                        },
                    )
                }
                selected?.let { item ->
                    DetailsDialog(
                        item = item,
                        similar = catalog.sections.flatMap { it.items }.distinctBy { it.id }.filter { it.id != item.id }.take(5),
                        onDismiss = { selected = null },
                        onSelect = { selected = it },
                        onPlayTrailer = { trailer ->
                            theaterReturn = item
                            theater = TheaterFeature(item, trailer)
                            recentTheater = (recentTheater + mediaKey(item)).takeLast(10)
                            selected = null
                        },
                    )
                }
                if (settingsOpen) SettingsDialog(
                    theaterEnabled = theaterEnabled,
                    idleMinutes = idleMinutes,
                    compactApps = compactApps,
                    focusLift = focusLift,
                    weatherLocation = weatherLocation,
                    weatherCelsius = weatherCelsius,
                    use24HourClock = use24HourClock,
                    hiddenAppCount = orderedApps.count { launcherAppKey(it) in hiddenApps },
                    onSearch = {
                        settingsOpen = false
                        searching = true
                    },
                    onTheaterEnabled = {
                        theaterEnabled = it
                        preferences.edit().putBoolean("theaterEnabled", it).apply()
                    },
                    onIdleMinutes = {
                        idleMinutes = it
                        preferences.edit().putInt("idleMinutes", it).apply()
                    },
                    onCompactApps = {
                        compactApps = it
                        preferences.edit().putBoolean("compactApps", it).apply()
                    },
                    onFocusLift = {
                        focusLift = it
                        preferences.edit().putBoolean("focusLift", it).apply()
                    },
                    onWeatherLocation = {
                        settingsOpen = false
                        weatherLocationOpen = true
                    },
                    onWeatherCelsius = {
                        weatherCelsius = it
                        preferences.edit().putBoolean("weatherCelsius", it).apply()
                    },
                    onClockFormat = {
                        use24HourClock = it
                        preferences.edit().putBoolean("use24HourClock", it).apply()
                    },
                    onHiddenApps = {
                        settingsOpen = false
                        hiddenAppsOpen = true
                    },
                    onSystemSettings = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) },
                    onHomeSettings = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                            .getOrElse { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    },
                    onDismiss = { settingsOpen = false },
                )
                if (weatherLocationOpen) WeatherLocationDialog(
                    location = weatherLocation,
                    onSave = { place ->
                        weatherLocation = place.label
                        weatherLatitude = place.latitude
                        weatherLongitude = place.longitude
                        preferences.edit()
                            .putString("weatherLocation", place.label)
                            .putString("weatherLatitude", place.latitude.toString())
                            .putString("weatherLongitude", place.longitude.toString())
                            .apply()
                        weatherLocationOpen = false
                        settingsOpen = true
                    },
                    onDismiss = {
                        weatherLocationOpen = false
                        settingsOpen = true
                    },
                )
                if (hiddenAppsOpen) HiddenAppsDialog(
                    apps = orderedApps.filter { launcherAppKey(it) in hiddenApps },
                    onLaunch = ::launchApp,
                    onRestore = { app ->
                        val key = launcherAppKey(app)
                        hiddenApps = hiddenApps - key
                        preferences.edit().putStringSet("hiddenApps", hiddenApps).apply()
                    },
                    onDismiss = { hiddenAppsOpen = false },
                )
                configuredApp?.let { app ->
                    AppOptionsDialog(
                        app = app,
                        onMove = {
                            movingAppKey = launcherAppKey(app)
                            configuredApp = null
                        },
                        onRename = {
                            editingApp = app
                            configuredApp = null
                        },
                        onAppInfo = {
                            configuredApp = null
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.component.packageName}")),
                            )
                        },
                        onHide = {
                            val key = launcherAppKey(app)
                            hiddenApps = hiddenApps + key
                            preferences.edit().putStringSet("hiddenApps", hiddenApps).apply()
                            configuredApp = null
                        },
                        onDismiss = { configuredApp = null },
                    )
                }
                editingApp?.let { app ->
                    AppRenameDialog(
                        app = app,
                        onSave = { name ->
                            val key = launcherAppKey(app)
                            customAppNames = customAppNames + (key to name)
                            preferences.edit().putString("customName:$key", name).apply()
                            editingApp = null
                        },
                        onReset = {
                            val key = launcherAppKey(app)
                            customAppNames = customAppNames - key
                            preferences.edit().remove("customName:$key").apply()
                            editingApp = null
                        },
                        onDismiss = { editingApp = null },
                    )
                }
            }
        }

        LaunchedEffect(result, theater, theaterEnabled, idleMinutes, isForeground) {
            val catalog = result ?: return@LaunchedEffect
            if (theater != null || !theaterEnabled || !isForeground) return@LaunchedEffect
            while (withTimeoutOrNull(idleMinutes * 60_000L) { inputEvents.receive() } != null) {}
            findTheaterFeature(
                launcherMovieSections(catalog).flatMap { it.items },
                recentTheater,
            )?.let {
                theater = it
                theaterReturn = null
                selected = null
                searching = false
                settingsOpen = false
                weatherLocationOpen = false
                hiddenAppsOpen = false
                recentTheater = (recentTheater + mediaKey(it.item)).takeLast(10)
            }
        }
    }
}

internal fun mediaKey(item: MediaItem) = "${item.mediaType}-${item.id}"
internal val THEATER_IDLE_OPTIONS = listOf(1, 3, 5, 10, 15, 30)
internal fun nextTheaterIdleMinutes(current: Int) =
    THEATER_IDLE_OPTIONS[(THEATER_IDLE_OPTIONS.indexOf(current) + 1).coerceAtLeast(0) % THEATER_IDLE_OPTIONS.size]

internal fun launcherMovieSections(catalog: CatalogResult): List<CatalogSection> {
    val preferred = listOf(
        "Now in cinemas",
        "Trending this week",
        "Top rated movies",
        "Popular series",
        "Popular animation",
        "Coming soon",
    )
        .mapNotNull { title -> catalog.sections.firstOrNull { it.title == title && it.items.isNotEmpty() } }
        .ifEmpty { catalog.sections.filter { it.items.isNotEmpty() }.take(6) }
    val seen = mutableSetOf<String>()
    return preferred.mapNotNull { section ->
        section.copy(items = section.items.filter { seen.add(mediaKey(it)) }).takeIf { it.items.isNotEmpty() }
    }
}

@Suppress("DEPRECATION")
private fun installedTvApps(context: Context): List<LauncherApp> {
    val manager = context.packageManager
    val intents = listOf(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
    )
    return intents.flatMap { manager.queryIntentActivities(it, 0) }
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map {
            LauncherApp(
                it.loadLabel(manager).toString(),
                ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                it.loadIcon(manager),
                it.activityInfo.loadBanner(manager) ?: it.activityInfo.applicationInfo.loadBanner(manager),
            )
        }
        .sortedBy { it.name.lowercase() }
}

private fun launcherAppKey(app: LauncherApp) = app.component.flattenToShortString()

private fun orderLauncherApps(apps: List<LauncherApp>, savedOrder: List<String>): List<LauncherApp> {
    val byKey = apps.associateBy(::launcherAppKey)
    return orderedAppKeys(byKey.keys.toList(), savedOrder).mapNotNull(byKey::get)
}

internal fun orderedAppKeys(installed: List<String>, saved: List<String>) =
    saved.filter { it in installed }.distinct() + installed.filterNot { it in saved }

internal fun savedCustomAppNames(values: Map<String, *>) = values.mapNotNull { (key, value) ->
    if (key.startsWith("customName:") && value is String) key.removePrefix("customName:") to value else null
}.toMap()

internal fun moveAppKey(order: List<String>, key: String, offset: Int): List<String> {
    val from = order.indexOf(key)
    if (from < 0) return order
    val to = (from + offset).coerceIn(order.indices)
    if (from == to) return order
    return order.toMutableList().apply { add(to, removeAt(from)) }
}

internal fun nextDiscoveryItem(items: List<MediaItem>, recent: List<String>): MediaItem? {
    val unseen = items.filterNot { mediaKey(it) in recent }
    return (unseen.ifEmpty { items.filterNot { mediaKey(it) == recent.lastOrNull() } }).randomOrNull()
        ?: items.firstOrNull()
}

internal fun adjacentRowIndex(index: Int, targetSize: Int) = index.coerceIn(0, targetSize - 1)
internal fun nextCatalogRetryDelay(current: Long) = (current * 2).coerceAtMost(5 * 60_000L)

private suspend fun findTheaterFeature(items: List<MediaItem>, recent: List<String>): TheaterFeature? {
    val candidates = items.distinctBy(::mediaKey)
    var attempted = recent
    repeat(minOf(8, candidates.size)) {
        val item = nextDiscoveryItem(candidates, attempted) ?: return null
        attempted = (attempted + mediaKey(item)).takeLast(candidates.size)
        CatalogRepository.details(item).trailer?.let { return TheaterFeature(item, it) }
    }
    return null
}

@SuppressLint("SetJavaScriptEnabled")
class TrailerActivity : Activity() {
    companion object {
        const val RESULT_FINISHED = RESULT_FIRST_USER
        const val RESULT_UNAVAILABLE = RESULT_FIRST_USER + 1
    }

    private lateinit var player: WebView
    private lateinit var releaseBadge: TextView
    private var manual = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    view.evaluateJavascript(
                        "MediaSource.isTypeSupported=(f=>t=>/av01|av1/i.test(t)?false:f(t))(MediaSource.isTypeSupported.bind(MediaSource))",
                        null,
                    )
                }

                override fun onPageFinished(view: WebView, url: String) {
                    view.evaluateJavascript(
                        """(()=>{if(window.reeloraWatching)return;window.reeloraWatching=true;let started=false;document.addEventListener('playing',()=>started=true,true);let timer=setInterval(()=>{let video=document.querySelector('video');if(video){clearInterval(timer);video.addEventListener('ended',()=>{if(!document.querySelector('.ad-showing'))Reelora.onEnded()})}},500);setTimeout(()=>{let video=document.querySelector('video');if(!started&&!(video&&video.currentTime>0))Reelora.onUnavailable()},15000)})()""",
                        null,
                    )
                }
            }
            addJavascriptInterface(PlayerBridge(), "Reelora")
            setBackgroundColor(android.graphics.Color.BLACK)
            isFocusable = false
            isFocusableInTouchMode = false
        }
        val density = resources.displayMetrics.density
        releaseBadge = TextView(this).apply {
            setTextColor(0xFFFF9A82.toInt())
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = .06f
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
            background = GradientDrawable().apply {
                setColor(0xE612121A.toInt())
                cornerRadius = 16 * density
                setStroke((density).toInt().coerceAtLeast(1), 0x88FF8064.toInt())
            }
            elevation = 8 * density
        }
        val content = FrameLayout(this).apply {
            addView(player, FrameLayout.LayoutParams(-1, -1))
            addView(releaseBadge, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
                topMargin = (96 * density).toInt()
                marginEnd = (32 * density).toInt()
            })
        }
        setContentView(content)
        content.translationX = 28 * density
        content.alpha = .92f
        content.animate().translationX(0f).alpha(1f).setDuration(220)
            .setInterpolator(android.view.animation.PathInterpolator(.2f, .8f, .2f, 1f)).start()
        play(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        play(intent)
    }

    private fun play(intent: Intent) {
        manual = intent.getBooleanExtra("manual", false)
        releaseBadge.text = intent.getStringExtra("release")
            ?.removePrefix("◷ COMING ")
            ?.let { "COMING  ·  $it" }
            .orEmpty()
        releaseBadge.animate().cancel()
        if (releaseBadge.text.isEmpty()) {
            releaseBadge.visibility = android.view.View.GONE
        } else {
            releaseBadge.visibility = android.view.View.VISIBLE
            releaseBadge.alpha = 0f
            releaseBadge.translationX = 24 * resources.displayMetrics.density
            releaseBadge.animate().translationX(0f).alpha(1f).setStartDelay(320).setDuration(240)
                .setInterpolator(android.view.animation.PathInterpolator(.2f, .8f, .2f, 1f)).start()
        }
        if (manual) android.widget.Toast.makeText(
            this,
            "← →  Seek 10s   •   OK  Play/Pause   •   Back  Close",
            android.widget.Toast.LENGTH_LONG,
        ).show()
        val videoId = intent.getStringExtra("videoId").orEmpty()
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        player.loadUrl(
            "https://www.youtube.com/embed/$videoId?autoplay=1&controls=1&rel=0&playsinline=1&origin=https%3A%2F%2Freelora.app",
            mapOf("Referer" to "https://reelora.app/"),
        )
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        if (!manual || event.keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(RESULT_OK)
            finish()
            return true
        }
        val script = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND ->
                "document.querySelector('video').currentTime=Math.max(0,document.querySelector('video').currentTime-10)"
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ->
                "document.querySelector('video').currentTime+=10"
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                "(()=>{let v=document.querySelector('video');v.paused?v.play():v.pause()})()"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "document.querySelector('video').play()"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "document.querySelector('video').pause()"
            else -> return super.dispatchKeyEvent(event)
        }
        player.evaluateJavascript(script, null)
        return true
    }

    private inner class PlayerBridge {
        @android.webkit.JavascriptInterface
        fun onEnded() = runOnUiThread {
            setResult(RESULT_FINISHED)
            finish()
        }

        @android.webkit.JavascriptInterface
        fun onUnavailable() = runOnUiThread {
            setResult(RESULT_UNAVAILABLE)
            finish()
        }
    }

    override fun onDestroy() {
        player.stopLoading()
        player.destroy()
        super.onDestroy()
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.reelora_mark),
            "Reelora TV",
            Modifier.size(88.dp).graphicsLayer { alpha = .82f },
        )
    }
}

@Composable
private fun AmbientBackdrop() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.size(560.dp).align(Alignment.TopEnd).background(
                Brush.radialGradient(listOf(Violet.copy(alpha = .17f), Color.Transparent)),
                CircleShape,
            ),
        )
        Box(
            Modifier.size(460.dp).align(Alignment.BottomStart).background(
                Brush.radialGradient(listOf(Coral.copy(alpha = .10f), Color.Transparent)),
                CircleShape,
            ),
        )
    }
}

@Composable
private fun LoadingBlock(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.width(width).height(height).clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .12f), Violet.copy(alpha = .06f)))),
    )
}

@Composable
private fun LoadingPosterRow() {
    Row(
        Modifier.height(132.dp).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(5) {
            Box(
                Modifier.width(196.dp).height(116.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .11f), Violet.copy(alpha = .055f)))),
            )
        }
    }
}

@Composable
private fun LoadingCastRow() {
    Row(Modifier.height(98.dp).padding(horizontal = 6.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(8) {
            Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(58.dp).background(Color.White.copy(alpha = .11f), CircleShape))
                Spacer(Modifier.height(7.dp))
                Box(Modifier.width(68.dp).height(8.dp).background(Color.White.copy(alpha = .11f), CircleShape))
            }
        }
    }
}

@Composable
private fun artworkModel(url: String): ImageRequest {
    val context = LocalContext.current
    return remember(url) { ImageRequest.Builder(context).data(url).build() }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Home(
    catalog: CatalogResult,
    apps: List<LauncherApp>,
    weather: WeatherNow?,
    weatherState: WeatherLoadState,
    use24HourClock: Boolean,
    compactApps: Boolean,
    focusLift: Boolean,
    onLaunch: (LauncherApp) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onHiddenApps: () -> Unit,
    onConfigureApp: (LauncherApp) -> Unit,
    movingAppKey: String?,
    moveConfirmReady: Boolean,
    onMoveApp: (LauncherApp, Int) -> Int,
    onMoveDone: () -> Unit,
    onSelect: (MediaItem) -> Unit,
) {
    val listState = rememberScrollState()
    val appListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val heroFocus = remember { FocusRequester() }
    val appFocus = remember { FocusRequester() }
    var lastAppRowFocus by remember { mutableStateOf<FocusRequester?>(null) }
    val sections = remember(catalog) { launcherMovieSections(catalog) }
    val movieRowFocus = remember(sections) { sections.map { section -> List(section.items.size) { FocusRequester() } } }
    val movieRowState = remember(sections) { sections.map { LazyListState() } }
    var lastFirstMovieFocus by remember { mutableStateOf<FocusRequester?>(null) }
    fun focusMovie(row: Int, item: Int) {
        val target = adjacentRowIndex(item, movieRowFocus[row].size)
        scope.launch {
            movieRowState[row].scrollToItem((target - 2).coerceAtLeast(0))
            delay(16)
            movieRowFocus[row][target].requestFocus()
        }
    }
    fun focusApps() {
        val target = lastAppRowFocus ?: appFocus
        scope.launch {
            listState.scrollTo(0)
            target.requestFocus()
        }
    }
    val installedAppKeys = remember(apps) { apps.map(::launcherAppKey).toSet() }
    val stableBringIntoView = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val end = offset + size
                if (offset >= 0f && end <= containerSize) return 0f
                if (offset < 0f && end > containerSize) return 0f
                return if (kotlin.math.abs(offset) < kotlin.math.abs(end - containerSize)) offset else end - containerSize
            }
        }
    }
    val featured = sections.first()
    var hero by remember(featured) { mutableStateOf(featured.items.first()) }
    var recent by remember(featured) { mutableStateOf(listOf(mediaKey(hero))) }
    LaunchedEffect(installedAppKeys) {
        listState.scrollTo(0)
        appListState.scrollToItem(0)
        delay(160)
        if (apps.isEmpty()) heroFocus.requestFocus() else appFocus.requestFocus()
    }
    LaunchedEffect(hero, featured) {
        launch { CatalogRepository.details(hero) }
        val next = nextDiscoveryItem(featured.items, recent)
        next?.backdropUrl?.let { url ->
            context.imageLoader.enqueue(ImageRequest.Builder(context).data(url).size(1920, 720).build())
        }
        delay(10_000)
        next?.let {
            hero = it
            recent = (recent + mediaKey(it)).takeLast(10)
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides stableBringIntoView) {
        Column(
            verticalArrangement = Arrangement.spacedBy(30.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(listState).padding(bottom = 48.dp).onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Menu -> { onSettings(); true }
                    Key.Search -> { onSearch(); true }
                    else -> false
                }
            },
        ) {
            Column {
                Hero(
                    hero,
                    weather,
                    weatherState,
                    use24HourClock,
                    onSelect,
                    Modifier
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                (lastAppRowFocus ?: appFocus).requestFocus()
                                true
                            } else false
                        }
                        .focusRequester(heroFocus),
                )
                Spacer(Modifier.height(4.dp))
                AppDock(
                    apps, appListState, heroFocus, appFocus, lastFirstMovieFocus ?: movieRowFocus.first().first(), compactApps,
                    focusLift, onLaunch, onConfigureApp, movingAppKey, moveConfirmReady, onMoveApp, onMoveDone, onHiddenApps, onSettings,
                    onRowFocused = { lastAppRowFocus = it },
                )
            }
            sections.forEachIndexed { index, section ->
                MediaRow(
                    section,
                    onSelect,
                    movieRowState[index],
                    movieRowFocus[index],
                    focusLift,
                    onUp = { itemIndex ->
                        if (index == 0) focusApps()
                        else focusMovie(index - 1, itemIndex)
                    },
                    onDown = if (index < sections.lastIndex) ({ itemIndex -> focusMovie(index + 1, itemIndex) }) else null,
                    onItemFocused = { itemIndex ->
                        if (index == 0) lastFirstMovieFocus = movieRowFocus[index][itemIndex]
                    },
                )
            }
            Text(
                "Movies by TMDB · Availability by JustWatch · Weather by Open-Meteo",
                color = Color.White.copy(alpha = .38f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 48.dp),
            )
        }
    }
}

@Composable
private fun AppDock(
    apps: List<LauncherApp>,
    listState: LazyListState,
    upFocus: FocusRequester,
    firstFocus: FocusRequester,
    downFocus: FocusRequester,
    compact: Boolean,
    focusLift: Boolean,
    onLaunch: (LauncherApp) -> Unit,
    onConfigureApp: (LauncherApp) -> Unit,
    movingAppKey: String?,
    moveConfirmReady: Boolean,
    onMoveApp: (LauncherApp, Int) -> Int,
    onMoveDone: () -> Unit,
    onHiddenApps: () -> Unit,
    onSettings: () -> Unit,
    onRowFocused: (FocusRequester) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val showStartFade by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    val showEndFade by remember {
        derivedStateOf { listState.canScrollForward }
    }
    Box(
        Modifier.fillMaxWidth().height(if (compact) 110.dp else 120.dp)
            .padding(horizontal = 48.dp),
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
            modifier = Modifier.fillMaxSize().focusGroup(),
        ) {
            itemsIndexed(
                apps,
                key = { _, app -> app.component.flattenToShortString() },
                contentType = { _, _ -> "app" },
            ) { index, app ->
                val itemFocus = if (index == 0) firstFocus else remember { FocusRequester() }
                AppCard(
                    app,
                    compact,
                    focusLift,
                    onLaunch,
                    onConfigureApp,
                    moving = launcherAppKey(app) == movingAppKey,
                    moveConfirmReady = moveConfirmReady,
                    movePosition = "${index + 1}/${apps.size}",
                    onMove = { offset ->
                        val destination = onMoveApp(app, offset)
                        scope.launch {
                            delay(16)
                            listState.animateScrollToItem((destination - 2).coerceAtLeast(0))
                        }
                    },
                    onMoveDone = onMoveDone,
                    onFocused = { onRowFocused(itemFocus) },
                    modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = tween(150), fadeOutSpec = null)
                        .focusRequester(itemFocus)
                        .focusProperties { up = upFocus; down = downFocus },
                )
            }
            item(key = "hidden") {
                val shelfFocus = remember { FocusRequester() }
                val itemFocus = if (apps.isEmpty()) firstFocus else shelfFocus
                ShelfActionCard(
                    "Hidden", Icons.Default.Delete, compact, focusLift, onHiddenApps,
                    Modifier.focusRequester(itemFocus).focusProperties { up = upFocus; down = downFocus },
                    onFocused = { onRowFocused(itemFocus) },
                )
            }
            item(key = "settings") {
                val itemFocus = remember { FocusRequester() }
                ShelfActionCard(
                    "Settings", Icons.Default.Settings, compact, focusLift, onSettings,
                    Modifier.focusRequester(itemFocus).focusProperties { up = upFocus; down = downFocus },
                    onFocused = { onRowFocused(itemFocus) },
                )
            }
        }
        if (showStartFade) Box(
            Modifier.align(Alignment.CenterStart).width(48.dp).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(Background, Background, Background.copy(alpha = 0f)))),
        )
        if (showEndFade) Box(
            Modifier.align(Alignment.CenterEnd).width(48.dp).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(Background.copy(alpha = 0f), Background))),
        )
    }
}

@Composable
private fun ShelfActionCard(
    label: String,
    icon: ImageVector,
    compact: Boolean,
    focusLift: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused && focusLift) 1.04f else 1f, tween(110), label = "$label focus")
    val width = if (compact) 116.dp else 136.dp
    val height = if (compact) 68.dp else 78.dp
    Column(
        modifier.width(width).activeTransform(scale)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }.clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.width(width).height(height).clip(RoundedCornerShape(if (compact) 16.dp else 19.dp))
                .background(PanelBrush)
                .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color.White.copy(alpha = .12f), RoundedCornerShape(if (compact) 16.dp else 19.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (focused) Color.White else Color.White.copy(alpha = .72f)),
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White.copy(alpha = if (focused) 1f else .60f), fontSize = if (compact) 11.sp else 12.sp)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppCard(
    app: LauncherApp,
    compact: Boolean,
    focusLift: Boolean,
    onLaunch: (LauncherApp) -> Unit,
    onConfigure: (LauncherApp) -> Unit,
    moving: Boolean,
    moveConfirmReady: Boolean,
    movePosition: String,
    onMove: (Int) -> Unit,
    onMoveDone: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var remotePressed by remember { mutableStateOf(false) }
    var remoteLongPress by remember { mutableStateOf(false) }
    val tileScale by animateFloatAsState(if (moving) 1.055f else if (focused && focusLift) 1.04f else 1f, tween(110), label = "app tile focus")
    val floatOffset = if (moving) {
        val motion = rememberInfiniteTransition(label = "moving app")
        val offset by motion.animateFloat(
            initialValue = -5f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
            label = "moving app float",
        )
        offset
    } else 0f
    val tileWidth = if (compact) 116.dp else 136.dp
    val tileHeight = if (compact) 68.dp else 78.dp
    val tileBackground = remember { Brush.linearGradient(listOf(Color(0xFF242936), Color(0xFF171A22))) }
    Column(
        modifier.width(tileWidth)
            .activeTransform(tileScale, floatOffset)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                if (moving) {
                    when {
                        event.type == KeyEventType.KeyDown && keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT -> onMove(-1)
                        event.type == KeyEventType.KeyDown && keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> onMove(1)
                        event.type == KeyEventType.KeyDown && keyCode == android.view.KeyEvent.KEYCODE_BACK -> onMoveDone()
                        keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER -> {
                            if (event.type == KeyEventType.KeyUp && moveConfirmReady) onMoveDone()
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                    return@onPreviewKeyEvent true
                }
                if (keyCode != android.view.KeyEvent.KEYCODE_DPAD_CENTER && keyCode != android.view.KeyEvent.KEYCODE_ENTER) {
                    return@onPreviewKeyEvent false
                }
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        remotePressed = true
                        if (event.nativeKeyEvent.repeatCount > 0 && !remoteLongPress) {
                            remoteLongPress = true
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (remotePressed) {
                            if (remoteLongPress) onConfigure(app) else onLaunch(app)
                        }
                        remotePressed = false
                        remoteLongPress = false
                        true
                    }
                    else -> false
                }
            }
            .combinedClickable(
                role = Role.Button,
                onClick = { if (moving) onMoveDone() else onLaunch(app) },
                onLongClick = { if (!moving) onConfigure(app) },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.width(tileWidth).height(tileHeight).clip(RoundedCornerShape(if (compact) 16.dp else 19.dp))
                .background(if (app.banner == null) tileBackground else SolidColor(Color(0xFF171720))),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = app.banner ?: app.icon,
                contentDescription = app.name,
                contentScale = if (app.banner == null) ContentScale.Fit else ContentScale.Crop,
                modifier = if (app.banner == null) {
                    Modifier.fillMaxWidth(.82f).fillMaxHeight(.78f).align(Alignment.Center)
                } else {
                    Modifier.fillMaxSize()
                },
            )
            if (focused || moving) {
                Box(
                    Modifier.fillMaxSize().border(
                        if (moving) 3.dp else 2.dp,
                        if (moving) Coral else Color.White.copy(alpha = .9f),
                        RoundedCornerShape(if (compact) 16.dp else 19.dp),
                    ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (moving) "←  MOVE $movePosition  →" else app.name,
            color = if (moving) Coral else Color.White.copy(alpha = if (focused) 1f else .60f),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = if (moving) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TvDialog(
    onDismiss: () -> Unit,
    modifier: Modifier,
    ambient: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.BoxScope.(() -> Unit) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(120), label = "dialog visibility")
    val close = {
        if (!closing) {
            closing = true
            visible = false
            scope.launch {
                delay(130)
                onDismiss()
            }
        }
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Background.copy(alpha = .9f)), contentAlignment = Alignment.Center) {
            if (ambient) AmbientBackdrop()
            Box(
                modifier.graphicsLayer {
                    this.alpha = alpha
                    translationX = (1f - alpha) * 18f
                }.clip(DialogShape).background(PanelBrush)
                    .border(1.dp, Color.White.copy(alpha = .12f), DialogShape),
            ) { content(close) }
        }
    }
    LaunchedEffect(Unit) { visible = true }
}

@Composable
private fun DialogHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GapLarge)) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.White.copy(alpha = .52f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        action?.invoke()
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .055f))
            .border(1.dp, Color.White.copy(alpha = .07f), RoundedCornerShape(20.dp)).padding(20.dp),
    ) {
        Text(title, color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        Text(subtitle, color = Color.White.copy(alpha = .48f), fontSize = 12.sp)
        Spacer(Modifier.height(Gap))
        content()
    }
}

@Composable
private fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp),
        cursorBrush = SolidColor(Violet),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = { field ->
            Row(
                Modifier.fillMaxWidth().height(60.dp).clip(ControlShape)
                    .background(Color.White.copy(alpha = if (focused) .1f else .055f))
                    .border(if (focused) 2.dp else 1.dp, if (focused) Violet else Color.White.copy(alpha = .1f), ControlShape)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value.isEmpty()) Text(placeholder, color = Color.White.copy(alpha = .4f), fontSize = 20.sp)
                field()
            }
        },
    )
}

@Composable
private fun AppOptionsDialog(
    app: LauncherApp,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onAppInfo: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(160); first.requestFocus() }
    TvDialog(onDismiss, Modifier.width(760.dp)) { close ->
        Column(Modifier.padding(DialogPadding)) {
            DialogHeader(
                app.name,
                "App options",
                leading = { AsyncImage(app.icon, app.name, Modifier.size(56.dp), contentScale = ContentScale.Fit) },
            )
                Spacer(Modifier.height(GapLarge))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                    AppOptionTile("Move", "Reorder on Home", Icons.AutoMirrored.Filled.List, Modifier.weight(1f).focusRequester(first), onMove)
                    AppOptionTile("Rename", "Shelf label", Icons.Default.Edit, Modifier.weight(1f), onRename)
                    AppOptionTile("App info", "Manage or uninstall", Icons.Default.Info, Modifier.weight(1f), onAppInfo)
                    AppOptionTile("Hide", "Remove from Home", Icons.Default.Delete, Modifier.weight(1f), onHide)
                }
                Spacer(Modifier.height(GapLarge))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ActionButton("Close", icon = Icons.Default.Close, onClick = close)
                }
        }
    }
}

@Composable
private fun AppOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.025f else 1f, tween(100), label = "$title focus")
    Column(
        modifier.activeTransform(scale)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(18.dp))
            .background(if (focused) Violet.copy(alpha = .18f) else Color.White.copy(alpha = .055f))
            .border(if (focused) 2.dp else 1.dp, if (focused) Violet else Color.White.copy(alpha = .09f), RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick).padding(18.dp),
    ) {
        Image(
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(if (focused) Color.White else Color.White.copy(alpha = .68f)),
            modifier = Modifier.size(27.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.White.copy(alpha = if (focused) .68f else .44f), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun AppRenameDialog(
    app: LauncherApp,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(app) { mutableStateOf(app.name) }
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { delay(180); fieldFocus.requestFocus(); keyboard?.show() }
    TvDialog(onDismiss, Modifier.width(640.dp)) { close ->
        Column(Modifier.padding(DialogPadding)) {
            DialogHeader(
                "Rename app",
                "Change the name shown on Home",
                leading = { AsyncImage(app.icon, null, Modifier.size(56.dp), contentScale = ContentScale.Fit) },
            )
                Spacer(Modifier.height(GapLarge))
                TvTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    placeholder = "App name",
                    modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus),
                )
                Spacer(Modifier.height(GapLarge))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ActionButton("Reset", icon = Icons.Default.Refresh, onClick = onReset)
                    Spacer(Modifier.width(Gap))
                    ActionButton("Cancel", icon = Icons.Default.Close, onClick = close)
                    Spacer(Modifier.width(Gap))
                    ActionButton("Save", icon = Icons.Default.Check) { name.trim().takeIf(String::isNotEmpty)?.let(onSave) }
                }
        }
    }
}

@Composable
private fun HiddenAppsDialog(
    apps: List<LauncherApp>,
    onLaunch: (LauncherApp) -> Unit,
    onRestore: (LauncherApp) -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(350); ready = true; first.requestFocus() }
    TvDialog(onDismiss, Modifier.width(960.dp).height(640.dp)) { close ->
        Column(Modifier.padding(DialogPadding)) {
                DialogHeader("Hidden apps", "Open an app or return it to Home")
                Spacer(Modifier.height(GapLarge))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Gap)) {
                    itemsIndexed(apps, key = { _, app -> launcherAppKey(app) }) { index, app ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = .065f)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AsyncImage(app.icon, app.name, Modifier.size(48.dp), contentScale = ContentScale.Fit)
                            Column(Modifier.weight(1f)) {
                                Text(app.name, color = Color.White.copy(alpha = .92f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text("Hidden from Home", color = Color.White.copy(alpha = .42f), fontSize = 11.sp)
                            }
                            ActionButton(
                                "Open",
                                modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                                icon = Icons.Default.PlayArrow,
                            ) { if (ready) onLaunch(app) }
                            ActionButton("Show on Home", icon = Icons.Default.Home) { if (ready) onRestore(app) }
                        }
                    }
                }
                if (apps.isEmpty()) Text("No hidden apps", color = Color.White.copy(alpha = .55f), modifier = Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ActionButton("Done", modifier = if (apps.isEmpty()) Modifier.focusRequester(first) else Modifier, icon = Icons.Default.Check, onClick = close)
                }
        }
    }
}

@Composable
private fun WeatherLocationDialog(location: String, onSave: (WeatherPlace) -> Unit, onDismiss: () -> Unit) {
    var value by remember(location) { mutableStateOf(location) }
    var selected by remember { mutableStateOf<WeatherPlace?>(null) }
    var suggestions by remember { mutableStateOf(emptyList<WeatherPlace>()) }
    var loading by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val confirm = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { delay(180); field.requestFocus(); keyboard?.show() }
    LaunchedEffect(selected) {
        if (selected != null) {
            keyboard?.hide()
            delay(80)
            confirm.requestFocus()
        }
    }
    LaunchedEffect(value) {
        if (value == selected?.label || value.trim().length < 2) {
            suggestions = emptyList()
            loading = false
            return@LaunchedEffect
        }
        selected = null
        loading = true
        delay(300)
        suggestions = WeatherRepository.locations(value)
        loading = false
    }
    TvDialog(onDismiss, Modifier.width(700.dp).height(620.dp)) { close ->
        Column(Modifier.padding(DialogPadding)) {
            DialogHeader("Weather location", "Search, choose, then confirm")
            Spacer(Modifier.height(GapLarge))
            TvTextField(value, { value = it.take(60) }, "City", Modifier.fillMaxWidth().focusRequester(field))
            Spacer(Modifier.height(Gap))
            Text(
                when {
                    loading -> "Searching…"
                    selected != null -> "Selected · ${selected?.label}"
                    value.trim().length < 2 -> "Type at least two letters"
                    suggestions.isEmpty() -> "No locations found"
                    else -> "Choose the correct location"
                },
                color = Color.White.copy(alpha = .52f),
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(Gap))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.take(5).forEach { place ->
                    ActionButton(place.label, Modifier.fillMaxWidth()) {
                        selected = place
                        value = place.label
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ActionButton("Cancel", icon = Icons.Default.Close, onClick = close)
                selected?.let { place ->
                    Spacer(Modifier.width(Gap))
                    ActionButton("Use location", Modifier.focusRequester(confirm), icon = Icons.Default.Check) { onSave(place) }
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    theaterEnabled: Boolean,
    idleMinutes: Int,
    compactApps: Boolean,
    focusLift: Boolean,
    weatherLocation: String,
    weatherCelsius: Boolean,
    use24HourClock: Boolean,
    hiddenAppCount: Int,
    onSearch: () -> Unit,
    onTheaterEnabled: (Boolean) -> Unit,
    onIdleMinutes: (Int) -> Unit,
    onCompactApps: (Boolean) -> Unit,
    onFocusLift: (Boolean) -> Unit,
    onWeatherLocation: () -> Unit,
    onWeatherCelsius: (Boolean) -> Unit,
    onClockFormat: (Boolean) -> Unit,
    onHiddenApps: () -> Unit,
    onSystemSettings: () -> Unit,
    onHomeSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(180); first.requestFocus() }
    TvDialog(onDismiss, Modifier.width(880.dp)) { close ->
        Column(Modifier.padding(DialogPadding)) {
                DialogHeader(
                    "Settings",
                    "A quiet home for apps and discovery",
                    action = { ActionButton("Done", icon = Icons.Default.Check, onClick = close) },
                )
                Spacer(Modifier.height(GapLarge))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsPanel("APP SHELF", "Find and size Home apps", Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            ActionButton("Search", modifier = Modifier.focusRequester(first), icon = Icons.Default.Search, onClick = onSearch)
                            ActionButton("Hidden${if (hiddenAppCount > 0) " · $hiddenAppCount" else ""}", icon = Icons.Default.Delete, onClick = onHiddenApps)
                        }
                        Spacer(Modifier.height(Gap))
                        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            ActionButton(if (compactApps) "Compact layout" else "Comfortable layout", icon = Icons.AutoMirrored.Filled.List) {
                                onCompactApps(!compactApps)
                            }
                            ActionButton(if (focusLift) "Lifted focus" else "Outline focus") {
                                onFocusLift(!focusLift)
                            }
                        }
                    }
                    SettingsPanel("THEATER", "Play trailers when the launcher rests", Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            ActionButton(if (theaterEnabled) "On" else "Off", icon = if (theaterEnabled) Icons.Default.PlayArrow else Icons.Default.Close) {
                                onTheaterEnabled(!theaterEnabled)
                            }
                            ActionButton("After $idleMinutes min") {
                                onIdleMinutes(nextTheaterIdleMinutes(idleMinutes))
                            }
                        }
                        Spacer(Modifier.height(Gap))
                        Text("Pauses while another app is open", color = Color.White.copy(alpha = .38f), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsPanel("WEATHER & TIME", "Location, temperature and clock", Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            ActionButton(weatherLocation.take(14), onClick = onWeatherLocation)
                            ActionButton(if (weatherCelsius) "°C" else "°F") { onWeatherCelsius(!weatherCelsius) }
                            ActionButton(if (use24HourClock) "24 h" else "12 h") { onClockFormat(!use24HourClock) }
                        }
                    }
                    SettingsPanel("SYSTEM", "Home and Android controls", Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            ActionButton("Default home", icon = Icons.Default.Home, onClick = onHomeSettings)
                            ActionButton("Android", icon = Icons.Default.Settings, onClick = onSystemSettings)
                        }
                    }
                }
        }
    }
}

@Composable
private fun SearchDialog(
    suggestions: List<MediaItem>,
    onDismiss: () -> Unit,
    onSelect: (MediaItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<MediaItem>()) }
    var loading by remember { mutableStateOf(false) }
    val inputRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val voiceIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Search movies and TV shows")
    }
    val voiceAvailable = remember { voiceIntent.resolveActivity(context.packageManager) != null }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { query = it }
        }
    }

    LaunchedEffect(Unit) {
        delay(120)
        inputRequester.requestFocus()
        keyboard?.show()
    }
    LaunchedEffect(query) {
        val term = query.trim()
        if (term.length < 2) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(350)
        results = CatalogRepository.search(term)
        loading = false
    }

    TvDialog(onDismiss, Modifier.fillMaxWidth(.92f).fillMaxHeight(.74f)) { close ->
        Column(Modifier.padding(DialogPadding)) {
            DialogHeader(
                "Search",
                "Movies, series and animation",
                action = { ActionButton("Close", icon = Icons.Default.Close, onClick = close) },
            )
            Spacer(Modifier.height(GapLarge))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Gap)) {
                TvTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    placeholder = "Type a title…",
                    imeAction = ImeAction.Search,
                    modifier = Modifier.weight(1f).focusRequester(inputRequester),
                )
                if (voiceAvailable) ActionButton("Voice") { voice.launch(voiceIntent) }
            }
            Spacer(Modifier.height(24.dp))
            val shown = if (query.trim().length < 2) suggestions else results
            Text(
                when {
                    query.trim().length < 2 -> "Popular now"
                    loading -> "Finding suggestions…"
                    shown.isEmpty() -> "No matches"
                    else -> "Suggestions"
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            if (loading) {
                LoadingPosterRow()
            } else {
                PosterStrip(shown, onSelect = { keyboard?.hide(); onSelect(it) })
            }
        }
    }
}

@Composable
private fun Hero(
    item: MediaItem,
    weather: WeatherNow?,
    weatherState: WeatherLoadState,
    use24HourClock: Boolean,
    onSelect: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var displayed by remember { mutableStateOf(item) }
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(mediaKey(item)) {
        if (mediaKey(displayed) == mediaKey(item)) return@LaunchedEffect
        reveal.animateTo(0f, tween(100, easing = LinearEasing))
        displayed = item
        delay(16)
        reveal.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(360.dp)
            .onFocusChanged { focused = it.isFocused }
            .background(Brush.linearGradient(listOf(Color(0xFF2D1760), Color(0xFF10101E))))
            .clickable(role = Role.Button) { onSelect(displayed) }
    ) {
        displayed.backdropUrl?.let {
            AsyncImage(
                model = artworkModel(it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().activeTransform(1f, alpha = reveal.value),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Background.copy(alpha = .96f),
                    .46f to Background.copy(alpha = .48f),
                    .82f to Background.copy(alpha = .08f),
                    1f to Background.copy(alpha = .72f),
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Background.copy(alpha = .62f),
                    .18f to Color.Transparent,
                    .76f to Color.Transparent,
                    1f to Background,
                )
            )
        )
        HomeStatus(weather, weatherState, use24HourClock, Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 58.dp))
        Column(
            Modifier.fillMaxHeight().width(620.dp).padding(start = 58.dp, top = 42.dp, end = 24.dp, bottom = 28.dp),
        ) {
            Box(
                Modifier.weight(1f).activeTransform(
                    scale = 1f,
                    translationX = (1f - reveal.value) * 10f,
                    alpha = reveal.value,
                ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column {
                    if (releaseLabel(displayed.releaseDate).startsWith("◷")) InfoBadge(releaseLabel(displayed.releaseDate), Coral)
                    Spacer(Modifier.height(6.dp))
                    Text(displayed.title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("${displayed.year}   ·   ★ ${"%.1f".format(displayed.score)}", color = Color.White.copy(alpha = .82f), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(displayed.overview, color = Color.White.copy(alpha = .68f), fontSize = 14.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 58.dp, bottom = 28.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (focused) Color.White else Violet)
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text("View", color = if (focused) Background else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HomeStatus(
    weather: WeatherNow?,
    weatherState: WeatherLoadState,
    use24HourClock: Boolean,
    modifier: Modifier = Modifier,
) {
    var time by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            time = LocalTime.now()
        }
    }
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(Background.copy(alpha = .68f))
            .border(1.dp, Color.White.copy(alpha = .1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(formatHomeTime(time, use24HourClock), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(
            weatherStatusText(weather, weatherState),
            color = if (weatherState == WeatherLoadState.Error) Coral else Color.White.copy(alpha = .82f),
            fontSize = 15.sp,
        )
    }
}

internal fun weatherStatusText(weather: WeatherNow?, state: WeatherLoadState) = when (state) {
    WeatherLoadState.Loading -> "◌  Weather"
    WeatherLoadState.Error -> weather?.let { "${weatherSymbol(it.code)}  ${it.temperature}°  ·  !" } ?: "!  Weather"
    WeatherLoadState.Ready -> weather?.let { "${weatherSymbol(it.code)}  ${it.temperature}°" } ?: "◌  Weather"
}

internal fun formatHomeTime(time: LocalTime, use24HourClock: Boolean) =
    time.format(DateTimeFormatter.ofPattern(if (use24HourClock) "HH:mm" else "h:mm a"))

internal fun weatherSymbol(code: Int) = when (code) {
    0 -> "☀"
    1, 2 -> "⛅"
    3, 45, 48 -> "☁"
    in 51..67, in 80..82 -> "☂"
    in 71..77, in 85..86 -> "❄"
    in 95..99 -> "ϟ"
    else -> "·"
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MediaRow(
    section: CatalogSection,
    onSelect: (MediaItem) -> Unit,
    listState: LazyListState,
    itemFocus: List<FocusRequester>,
    focusLift: Boolean,
    onUp: (Int) -> Unit,
    onDown: ((Int) -> Unit)?,
    onItemFocused: (Int) -> Unit,
) {
    Column {
        Text(section.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 48.dp))
        Spacer(Modifier.height(12.dp))
        CompositionLocalProvider(LocalBringIntoViewSpec provides RowBringIntoViewSpec) {
            PosterStrip(
                section.items,
                onSelect,
                state = listState,
                liftOnFocus = focusLift,
                modifier = Modifier.height(132.dp),
                contentPadding = PaddingValues(start = 48.dp, end = 72.dp, top = 8.dp, bottom = 8.dp),
                itemModifier = { index ->
                    Modifier.focusRequester(itemFocus[index]).onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionUp -> { onUp(index); true }
                            Key.DirectionDown -> onDown?.let { it(index); true } ?: false
                            else -> false
                        }
                    }.onFocusChanged {
                        if (it.isFocused) onItemFocused(index)
                    }
                },
            )
        }
    }
}

@Composable
private fun PosterStrip(
    items: List<MediaItem>,
    onSelect: (MediaItem) -> Unit,
    state: LazyListState? = null,
    liftOnFocus: Boolean = true,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 8.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
    firstModifier: Modifier = Modifier,
    itemModifier: (Int) -> Modifier = { Modifier },
) {
    val rowState = state ?: rememberLazyListState()
    LazyRow(
        state = rowState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.focusGroup(),
    ) {
        itemsIndexed(items, key = { _, item -> mediaKey(item) }, contentType = { _, _ -> "poster" }) { index, item ->
            PosterCard(item, onSelect, liftOnFocus, (if (index == 0) firstModifier else Modifier).then(itemModifier(index)))
        }
    }
}

@Composable
private fun PosterCard(item: MediaItem, onSelect: (MediaItem) -> Unit, liftOnFocus: Boolean, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(if (focused && liftOnFocus) 1.025f else 1f, tween(100), label = "movie card focus")
    Box(
        modifier
            .width(196.dp)
            .height(116.dp)
            .activeTransform(cardScale)
            .zIndex(if (focused) 1f else 0f)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF342065), Color(0xFF19192A))))
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) { onSelect(item) }
    ) {
        val artwork = item.backdropUrl ?: item.posterUrl
        if (artwork != null) {
            AsyncImage(
                artworkModel(artwork),
                item.title,
                Modifier.fillMaxSize(),
                error = painterResource(R.drawable.reelora_mark),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(painterResource(R.drawable.reelora_mark), null, Modifier.size(52.dp).align(Alignment.Center))
        }
        cardReleaseLabel(item.releaseDate)?.let {
            InfoBadge(it, Coral, Modifier.align(Alignment.TopStart).padding(7.dp))
        }
        Box(
            Modifier.fillMaxWidth().height(58.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Background.copy(alpha = .94f)))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.year}  ·  ★ ${"%.1f".format(item.score)}", color = Color.White.copy(alpha = .68f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun InfoBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Background.copy(alpha = .88f))
            .border(1.dp, color.copy(alpha = .7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, tween(95), label = "$text button focus")
    Box(
        modifier
            .activeTransform(scale)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clip(ControlShape)
            .background(if (focused) Violet.copy(alpha = .22f) else Color.White.copy(alpha = .07f))
            .border(if (focused) 2.dp else 1.dp, if (focused) Violet else Color.White.copy(alpha = .09f), ControlShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon?.let {
                Image(
                    imageVector = it,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(if (focused) Color.White else Color.White.copy(alpha = .78f)),
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun DetailsDialog(
    item: MediaItem,
    similar: List<MediaItem>,
    onDismiss: () -> Unit,
    onSelect: (MediaItem) -> Unit,
    onPlayTrailer: (Trailer) -> Unit,
) {
    val requester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val actorRowRequester = remember { FocusRequester() }
    val similarRowRequester = remember { FocusRequester() }
    var details by remember(item.id) { mutableStateOf<MediaDetails?>(null) }
    var selectedActor by remember(item.id) { mutableStateOf<CastMember?>(null) }
    var actorTitles by remember(item.id) { mutableStateOf<List<MediaItem>?>(null) }
    var actorLoading by remember(item.id) { mutableStateOf(false) }
    val artwork = details?.backdrops?.firstOrNull { it != item.backdropUrl } ?: item.backdropUrl
    val moreLike = details?.similar?.ifEmpty { similar } ?: similar
    val restoreTop: () -> Unit = { scope.launch { listState.animateScrollToItem(0) } }
    TvDialog(onDismiss, Modifier.fillMaxWidth(.9f).fillMaxHeight(.92f), ambient = false) { close ->
         Box(Modifier.fillMaxSize()) {
          Box(
              Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(Color(0xFF11111C)),
          ) {
           artwork?.let {
              AsyncImage(
                  model = artworkModel(it),
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
              )
           }
           Box(Modifier.fillMaxSize().background(Background.copy(alpha = .62f)))
           Box(
              Modifier.fillMaxWidth().height(280.dp).align(Alignment.BottomCenter)
                  .background(Background.copy(alpha = .24f)),
           )
          }
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { Column {
                Row(Modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(
                        "Back",
                        modifier = Modifier.focusRequester(requester),
                        onFocused = restoreTop,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = close,
                    )
                    details?.trailer?.let { trailer ->
                        ActionButton("Play trailer", onFocused = restoreTop, icon = Icons.Default.PlayArrow) { onPlayTrailer(trailer) }
                    }
                    if (details == null) LoadingBlock(132.dp, 42.dp, 10.dp)
                }
                Spacer(Modifier.height(18.dp))
                Row {
                    Box(
                        Modifier.width(136.dp).height(190.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF342065), Color(0xFF19192A))))
                    ) {
                        if (item.posterUrl != null) AsyncImage(artworkModel(item.posterUrl), item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Image(painterResource(R.drawable.reelora_mark), null, Modifier.size(82.dp).align(Alignment.Center))
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    val metadata = listOf(
                        item.year,
                        item.mediaType.uppercase(),
                        details?.runtime.orEmpty(),
                        "★ ${"%.1f".format(item.score)}",
                    ).filter { it.isNotBlank() }.joinToString("  ·  ")
                    Text(metadata, color = Coral, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    details?.genres?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color.White.copy(alpha = .58f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val release = releaseLabel(item.releaseDate)
                        InfoBadge(release, if (release.startsWith("✓") || release.startsWith("●")) Color(0xFF66D69A) else Coral)
                        details?.availability?.let { AvailabilityBadge(it) }
                    }
                    details?.availability?.let {
                        Text("Availability by JustWatch · ${it.region}", color = Color.White.copy(alpha = .38f), fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(item.overview, color = Color.White.copy(alpha = .78f), fontSize = 16.sp, lineHeight = 22.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    }
                }
            } }
            item { Column {
                Text("Cast", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                CastRow(
                    details?.cast,
                    selectedActor,
                    when {
                        selectedActor != null && actorTitles?.isNotEmpty() == true -> actorRowRequester
                        moreLike.isNotEmpty() -> similarRowRequester
                        else -> null
                    },
                    onSelect = { selectedActor = if (selectedActor?.id == it.id) null else it },
                )
            } }
            selectedActor?.let { actor ->
                val titles = actorTitles
                item(key = "actor-credits") { Column {
                    Text("${actor.name} · Movies & TV${if (actorLoading && titles != null) " · Updating…" else ""}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    when {
                        titles == null -> LoadingPosterRow()
                        titles.isEmpty() -> Text("No other titles found", color = Color.White.copy(alpha = .55f), fontSize = 14.sp)
                        else -> PosterStrip(titles, onSelect, firstModifier = Modifier.focusRequester(actorRowRequester))
                    }
                } }
            }
            item { Column {
                Text("More like this", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                PosterStrip(moreLike, onSelect, firstModifier = Modifier.focusRequester(similarRowRequester))
            } }
          }
         }
    }
    LaunchedEffect(item.id) {
        listState.scrollToItem(0)
        delay(140)
        requester.requestFocus()
        details = CatalogRepository.details(item)
    }
    LaunchedEffect(selectedActor?.id) {
        selectedActor?.let { actor ->
            actorLoading = true
            actorTitles = null
            actorTitles = CatalogRepository.credits(actor.id).filterNot { it.id == item.id && it.mediaType == item.mediaType }
            actorLoading = false
        }
    }
}

@Composable
private fun AvailabilityBadge(availability: WatchAvailability) {
    val providers = availability.streaming.ifEmpty { availability.rentOrBuy }
    val streaming = availability.streaming.isNotEmpty()
    val color = when {
        streaming -> Color(0xFF66D69A)
        providers.isNotEmpty() -> Violet
        else -> Color.White.copy(alpha = .5f)
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Background.copy(alpha = .88f))
            .border(1.dp, color.copy(alpha = .7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(if (streaming) "▶" else if (providers.isNotEmpty()) "\$" else "—", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        providers.take(3).forEach { provider ->
            provider.logoUrl?.let { AsyncImage(artworkModel(it), provider.name, Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))) }
        }
        Text(
            when {
                streaming -> "STREAMING"
                providers.isNotEmpty() -> "RENT / BUY"
                else -> "NO STREAMING LISTED"
            },
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CastRow(
    cast: List<CastMember>?,
    selected: CastMember?,
    downRequester: FocusRequester?,
    onSelect: (CastMember) -> Unit,
) {
    if (cast == null) {
        LoadingCastRow()
        return
    }
    if (cast.isEmpty()) {
        Text("Cast information unavailable", color = Color.White.copy(alpha = .55f), fontSize = 14.sp, modifier = Modifier.height(98.dp))
        return
    }
    LazyRow(
        contentPadding = PaddingValues(start = 6.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.focusGroup(),
    ) {
        items(cast, key = { "${it.id}-${it.name}" }, contentType = { "cast" }) { person ->
            CastCard(person, person.id == selected?.id, downRequester, onSelect)
        }
    }
}

@Composable
private fun CastCard(person: CastMember, selected: Boolean, downRequester: FocusRequester?, onSelect: (CastMember) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier
            .width(96.dp)
            .focusProperties { downRequester?.let { down = it } }
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) { onSelect(person) }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(58.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF4B2B86), Color(0xFF211B3A))))
                .border(if (focused) 3.dp else if (selected) 2.dp else 0.dp, if (focused) Color.White else Coral, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (person.profileUrl != null) {
                AsyncImage(artworkModel(person.profileUrl), person.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(person.name.take(1), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(person.name, color = if (selected) Coral else Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (person.character.isNotBlank()) {
            Text(person.character, color = Color.White.copy(alpha = .52f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}
