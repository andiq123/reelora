package tv.reelora.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
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
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import coil3.compose.AsyncImage
import coil3.request.crossfade
import coil3.request.ImageRequest
import kotlinx.coroutines.Job
import android.os.SystemClock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LocalDialogReady = staticCompositionLocalOf { false }
internal val LocalInternet = staticCompositionLocalOf { false }
private val LocalForeground = staticCompositionLocalOf { true }

private val RowBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val margin = 24f
        val end = offset + size
        if (offset >= margin && end <= containerSize - margin) return 0f
        return if (offset < margin) offset - margin else end - containerSize + margin
    }
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

// System screens must not join the launcher's Home task: Home redirects can clear it.
private fun Context.openAndroidSettings(action: String = Settings.ACTION_SETTINGS, data: Uri? = null) {
    for (candidate in listOf(action, Settings.ACTION_SETTINGS).distinct()) {
        try {
            startActivity(Intent(candidate, if (candidate == action) data else null)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
            // Some TV vendors do not expose the requested settings page.
        } catch (_: SecurityException) {
            // Fall back to the public settings entry when a vendor restricts a page.
        }
    }
    android.widget.Toast.makeText(this, getString(R.string.device_settings_unavailable), android.widget.Toast.LENGTH_SHORT).show()
}

// App-icon launches enter the same Home task without constructing a second Compose tree.
class LauncherEntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .setClass(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}

class MainActivity : ComponentActivity() {
    private val inputEvents = Channel<Unit>(Channel.CONFLATED)
    private lateinit var updater: AppUpdater
    private val foreground = MutableStateFlow(false)
    private val appRevision = MutableStateFlow(0)
    private val packageChanges = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            appRevision.value += 1
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        inputEvents.trySend(Unit)
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updater = AppUpdater(this)
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageChanges, packageFilter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(packageChanges, packageFilter)
        setContent { ReeloraApp(inputEvents, foreground, appRevision, updater) }
    }

    override fun onResume() {
        super.onResume()
        foreground.value = true
        updater.resume()
    }

    override fun onPause() {
        foreground.value = false
        updater.pause()
        super.onPause()
    }
    override fun onDestroy() {
        updater.close()
        unregisterReceiver(packageChanges)
        inputEvents.close()
        super.onDestroy()
    }

}

@Composable
private fun ReeloraApp(inputEvents: Channel<Unit>, foreground: MutableStateFlow<Boolean>, appRevision: MutableStateFlow<Int>, updater: AppUpdater) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("launcher", Context.MODE_PRIVATE) }
    var romanian by remember { mutableStateOf(preferences.getBoolean("romanian", false)) }
    val isForeground by foreground.collectAsState()
    val online = rememberInternetAvailable(isForeground)
    val updateStatus by updater.status.collectAsState()
    CompositionLocalProvider(LocalRomanian provides romanian, LocalForeground provides isForeground, LocalInternet provides online) { MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = WarmAccent,
            background = Background,
            surface = Surface,
            onBackground = Color.White,
            onSurface = Color.White,
        )
    ) {
        var result by remember { mutableStateOf(CatalogRepository.freshCatalog()) }
        var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
        var loadedRevision by remember { mutableStateOf(-1) }
        val appsReady = loadedRevision >= 0
        ReportDrawnWhen { appsReady }
        var selected by remember { mutableStateOf<MediaItem?>(null) }
        var searching by remember { mutableStateOf(false) }
        var searchFromSettings by remember { mutableStateOf(false) }
        var hiddenFromSettings by remember { mutableStateOf(false) }
        var settingsSection by remember { mutableStateOf(0) }
        var settingsOpen by remember { mutableStateOf(false) }
        var weatherLocationOpen by remember { mutableStateOf(false) }
        var hiddenAppsOpen by remember { mutableStateOf(false) }
        var configuredApp by remember { mutableStateOf<LauncherApp?>(null) }
        var editingApp by remember { mutableStateOf<LauncherApp?>(null) }
        var movingAppKey by remember { mutableStateOf<String?>(null) }
        var dockFocusKey by remember { mutableStateOf<String?>(null) }
        var theater by remember { mutableStateOf<TheaterFeature?>(null) }
        var theaterReturn by remember { mutableStateOf<MediaItem?>(null) }
        var recentTheater by remember { mutableStateOf(emptyList<String>()) }
        var theaterOpen by remember { mutableStateOf(false) }
        var theaterEnabled by remember { mutableStateOf(preferences.getBoolean("theaterEnabled", false)) }
        var idleMinutes by remember {
            mutableStateOf(preferences.getInt("idleMinutes", 3).takeIf { it in THEATER_IDLE_OPTIONS } ?: 3)
        }
        var focusLift by remember { mutableStateOf(preferences.getBoolean("focusLift", true)) }
        var showAppLabels by remember { mutableStateOf(preferences.getBoolean("showAppLabels", true)) }
        var moviesEnabled by remember { mutableStateOf(preferences.getBoolean("moviesEnabled", true)) }
        var wallpaperSeed by remember { mutableStateOf(preferences.getInt("wallpaperSeed", 0)) }
        var footballWidgetEnabled by remember { mutableStateOf(preferences.getBoolean("footballWidgetEnabled", false)) }
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
                android.widget.Toast.makeText(context, if (romanian) "Nu se poate deschide ${app.name}" else "Unable to open ${app.name}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        val theaterScope = rememberCoroutineScope()
        val theaterLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultCode ->
            theaterOpen = false
            val manualReturn = theaterReturn
            if (resultCode.resultCode == TrailerActivity.RESULT_UNAVAILABLE && manualReturn != null) {
                android.widget.Toast.makeText(context, if (romanian) "Trailer indisponibil sau blocat în regiunea ta" else "Trailer unavailable or blocked in your region", android.widget.Toast.LENGTH_LONG).show()
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
        LaunchedEffect(isForeground, online, moviesEnabled, searching, appsReady) {
            if (!isForeground || !online || !appsReady || (!moviesEnabled && !searching)) return@LaunchedEffect
            withFrameNanos { }
            var retryDelay = 10_000L
            while (true) {
                val loaded = CatalogRepository.load()
                result = loaded
                if (!CatalogRepository.configured) break
                val untilMidnight = java.time.Duration.between(java.time.LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).toMillis().coerceAtLeast(1_000L)
                delay(if (loaded.isDemo) retryDelay else minOf(6 * 60 * 60_000L, untilMidnight))
                retryDelay = nextCatalogRetryDelay(retryDelay)
            }
        }
        var weatherRefreshAt by remember(weatherLocation, weatherCelsius, weatherLatitude, weatherLongitude) { mutableStateOf(0L) }
        LaunchedEffect(weatherLocation, weatherCelsius, weatherLatitude, weatherLongitude) {
            weather = null
            weatherState = WeatherLoadState.Loading
        }
        LaunchedEffect(isForeground, online, weatherLocation, weatherCelsius, weatherLatitude, weatherLongitude, appsReady) {
            if (!isForeground || !online || !appsReady) return@LaunchedEffect
            weatherRefreshAt = SystemClock.elapsedRealtime() + widgetRefreshDelay(SystemClock.elapsedRealtime(), weatherRefreshAt, weatherState != WeatherLoadState.Ready)
            withFrameNanos { }
            delay(250)
            while (true) {
                delay((weatherRefreshAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
                val latest = WeatherRepository.current(weatherLocation, weatherCelsius, weatherLatitude, weatherLongitude)
                if (latest == null) weatherState = WeatherLoadState.Error else {
                    weather = latest
                    weatherState = WeatherLoadState.Ready
                }
                weatherRefreshAt = SystemClock.elapsedRealtime() + if (latest == null) 60_000L else 30 * 60_000L
            }
        }
        val revision by appRevision.collectAsState()
        LaunchedEffect(isForeground, revision) {
            if (isForeground && revision != loadedRevision) {
                apps = withContext(Dispatchers.IO) { installedTvApps(context) }
                loadedRevision = revision
            }
        }
        LaunchedEffect(theater?.trailer?.key) {
            val feature = theater ?: return@LaunchedEffect
            val intent = Intent(context, TrailerActivity::class.java)
                .putExtra("videoId", feature.trailer.key)
                .putExtra("manual", theaterReturn != null)
                .putExtra("romanian", romanian)
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
            val catalog = result ?: remember { CatalogResult(emptyList(), isDemo = true) }
            run {
                if (moviesEnabled) {
                    Home(
                        catalog,
                        loading = result == null,
                        active = isForeground && !searching && !settingsOpen && selected == null && !weatherLocationOpen && !hiddenAppsOpen && configuredApp == null && editingApp == null,
                        apps = visibleApps,
                        appsReady = appsReady,
                        weather = weather,
                        weatherState = weatherState,
                        use24HourClock = use24HourClock,
                        focusLift = focusLift,
                        showAppLabels = showAppLabels,
                        onLaunch = ::launchApp,
                        onSearch = { dockFocusKey = null; searchFromSettings = false; searching = true },
                        onSettings = { dockFocusKey = null; settingsOpen = true },
                        onHiddenApps = { dockFocusKey = null; hiddenFromSettings = false; hiddenAppsOpen = true },
                        onConfigureApp = { dockFocusKey = launcherAppKey(it); configuredApp = it },
                        movingAppKey = movingAppKey,
                        dockFocusKey = if (searching || settingsOpen || selected != null || weatherLocationOpen || hiddenAppsOpen || configuredApp != null || editingApp != null) null else dockFocusKey,
                        onMoveApp = ::moveVisibleApp,
                        onMoveDone = { movingAppKey = null },
                        onSelect = { dockFocusKey = null; selected = it },
                    )
                } else {
                    AppsOnlyHome(
                        apps = visibleApps,
                        appsReady = appsReady,
                        weather = weather,
                        weatherState = weatherState,
                        use24HourClock = use24HourClock,
                        focusLift = focusLift,
                        showAppLabels = showAppLabels,
                        wallpaperSeed = wallpaperSeed,
                        footballWidgetEnabled = footballWidgetEnabled,
                        onLaunch = ::launchApp,
                        onSearch = { dockFocusKey = null; searchFromSettings = false; searching = true },
                        onSettings = { dockFocusKey = null; settingsOpen = true },
                        onHiddenApps = { dockFocusKey = null; hiddenFromSettings = false; hiddenAppsOpen = true },
                        onConfigureApp = { dockFocusKey = launcherAppKey(it); configuredApp = it },
                        movingAppKey = movingAppKey,
                        dockFocusKey = if (searching || settingsOpen || selected != null || weatherLocationOpen || hiddenAppsOpen || configuredApp != null || editingApp != null) null else dockFocusKey,
                        onMoveApp = ::moveVisibleApp,
                        onMoveDone = { movingAppKey = null },
                    )
                }
                if (searching) {
                    SearchDialog(
                        visible = selected == null,
                        suggestions = catalog.sections.firstOrNull()?.items.orEmpty().take(10),
                        onDismiss = { searching = false; settingsOpen = searchFromSettings; searchFromSettings = false },
                        onSelect = { dockFocusKey = null; selected = it },
                    )
                }
                selected?.let { item ->
                    DetailsDialog(
                        item = item,
                        similar = catalog.sections.flatMap { it.items }.distinctBy(::mediaKey).filter { mediaKey(it) != mediaKey(item) }.take(5),
                        onDismiss = { selected = null },
                        onSelect = { dockFocusKey = null; selected = it },
                        onPlayTrailer = { trailer ->
                            theaterReturn = item
                            theater = TheaterFeature(item, trailer)
                            recentTheater = (recentTheater + mediaKey(item)).takeLast(10)
                            selected = null
                        },
                    )
                }
                if (settingsOpen) SettingsDialog(
                    section = settingsSection,
                    onSectionChange = { settingsSection = it },
                    updateStatus = updateStatus,
                    onUpdate = updater::check,
                    theaterEnabled = theaterEnabled,
                    idleMinutes = idleMinutes,
                    focusLift = focusLift,
                    showAppLabels = showAppLabels,
                    moviesEnabled = moviesEnabled,
                    footballWidgetEnabled = footballWidgetEnabled,
                    romanian = romanian,
                    weatherLocation = weatherLocation,
                    weatherCelsius = weatherCelsius,
                    use24HourClock = use24HourClock,
                    hiddenAppCount = orderedApps.count { launcherAppKey(it) in hiddenApps },
                    onSearch = {
                        settingsOpen = false
                        searchFromSettings = true
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
                    onFocusLift = {
                        focusLift = it
                        preferences.edit().putBoolean("focusLift", it).apply()
                    },
                    onShowAppLabels = {
                        showAppLabels = it
                        preferences.edit().putBoolean("showAppLabels", it).apply()
                    },
                    onMoviesEnabled = {
                        moviesEnabled = it
                        theater = null
                        searching = false
                        selected = null
                        preferences.edit().putBoolean("moviesEnabled", it).apply()
                    },
                    onNextWallpaper = {
                        wallpaperSeed += 1
                        preferences.edit().putInt("wallpaperSeed", wallpaperSeed).apply()
                    },
                    onFootballWidget = {
                        footballWidgetEnabled = it
                        preferences.edit().putBoolean("footballWidgetEnabled", it).apply()
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
                    onLanguage = {
                        romanian = it
                        preferences.edit().putBoolean("romanian", it).apply()
                    },
                    onHiddenApps = {
                        settingsOpen = false
                        hiddenFromSettings = true
                        hiddenAppsOpen = true
                    },
                    onSystemSettings = { context.openAndroidSettings() },
                    onHomeSettings = { context.openAndroidSettings(Settings.ACTION_HOME_SETTINGS) },
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
                        dockFocusKey = key
                        hiddenApps = hiddenApps - key
                        preferences.edit().putStringSet("hiddenApps", hiddenApps).apply()
                    },
                    onDismiss = { hiddenAppsOpen = false; settingsOpen = hiddenFromSettings; hiddenFromSettings = false },
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
                            context.openAndroidSettings(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.component.packageName}"),
                            )
                        },
                        onHide = {
                            val key = launcherAppKey(app)
                            val index = visibleApps.indexOfFirst { launcherAppKey(it) == key }
                            dockFocusKey = visibleApps.getOrNull(index + 1)?.let(::launcherAppKey)
                                ?: visibleApps.getOrNull(index - 1)?.let(::launcherAppKey) ?: "hidden"
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
                        onDismiss = { editingApp = null; configuredApp = app },
                    )
                }
            }
        }

        val overlayOpen = searching || settingsOpen || weatherLocationOpen || hiddenAppsOpen || selected != null || configuredApp != null || editingApp != null || movingAppKey != null
        LaunchedEffect(result, theater, theaterEnabled, idleMinutes, isForeground, moviesEnabled, overlayOpen) {
            val catalog = result ?: return@LaunchedEffect
            if (theater != null || !theaterEnabled || !isForeground || !moviesEnabled || overlayOpen) return@LaunchedEffect
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
    } }
}

internal fun mediaKey(item: MediaItem) = "${item.mediaType}-${item.id}"
internal val THEATER_IDLE_OPTIONS = listOf(1, 3, 5, 10, 15, 30)
internal fun nextTheaterIdleMinutes(current: Int) =
    THEATER_IDLE_OPTIONS[(THEATER_IDLE_OPTIONS.indexOf(current) + 1).coerceAtLeast(0) % THEATER_IDLE_OPTIONS.size]

internal fun launcherMovieSections(catalog: CatalogResult): List<CatalogSection> {
    val preferred = listOf(
        "Popular movies",
        "Now in cinemas",
        "Trending this week",
        "Top rated movies",
        "Popular series",
        "Popular animation",
        "Coming soon",
    )
        .mapNotNull { title -> catalog.sections.firstOrNull { it.title == title && it.items.isNotEmpty() } }
        .ifEmpty { catalog.sections.filter { it.items.isNotEmpty() }.take(6) }
    val discovery = discoveryMovies(catalog)
    return listOfNotNull(discovery.takeIf { it.isNotEmpty() }?.let { CatalogSection(0, "Discover movies", it) }) +
        preferred.map { section -> section.copy(items = section.items.distinctBy(::mediaKey)) }
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
        .mapNotNull {
            runCatching { LauncherApp(
                it.loadLabel(manager).toString(),
                ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                it.loadIcon(manager),
                it.activityInfo.loadBanner(manager) ?: it.activityInfo.applicationInfo.loadBanner(manager),
            ) }.getOrNull()
        }
        .sortedBy { it.name.lowercase() }
}

internal fun dockEntryIndex(appKeys: List<String>, key: String?): Int = when (key) {
    "search" -> appKeys.size
    "hidden" -> appKeys.size + 1
    "settings" -> appKeys.size + 2
    else -> appKeys.indexOf(key).coerceAtLeast(0)
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
        val romanian = intent.getBooleanExtra("romanian", false)
        releaseBadge.text = intent.getStringExtra("release")
            ?.removePrefix("◷ COMING ")
            ?.let { "${if (romanian) "ÎN CURÂND" else "COMING"}  ·  $it" }
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
            if (romanian) "← →  Derulează 10s   •   OK  Redă/Pauză   •   Înapoi  Închide" else "← →  Seek 10s   •   OK  Play/Pause   •   Back  Close",
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

    override fun onResume() {
        super.onResume()
        player.onResume()
        player.resumeTimers()
    }

    override fun onPause() {
        player.evaluateJavascript("document.querySelectorAll('video').forEach(v=>v.pause())", null)
        player.onPause()
        player.pauseTimers()
        super.onPause()
    }

    override fun onDestroy() {
        player.stopLoading()
        (player.parent as? android.view.ViewGroup)?.removeView(player)
        player.removeJavascriptInterface("Reelora")
        player.destroy()
        super.onDestroy()
    }
}

@Composable
private fun AmbientBackdrop() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.size(560.dp).align(Alignment.TopEnd).background(
                Brush.radialGradient(listOf(Accent.copy(alpha = .17f), Color.Transparent)),
                CircleShape,
            ),
        )
        Box(
            Modifier.size(460.dp).align(Alignment.BottomStart).background(
                Brush.radialGradient(listOf(WarmAccent.copy(alpha = .10f), Color.Transparent)),
                CircleShape,
            ),
        )
    }
}

@Composable
private fun LoadingBlock(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.width(width).height(height).clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .12f), Accent.copy(alpha = .06f)))),
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
                    .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .11f), Accent.copy(alpha = .055f)))),
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
    return remember(context, url) { ImageRequest.Builder(context).data(url).build() }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Home(
    catalog: CatalogResult,
    loading: Boolean,
    active: Boolean,
    apps: List<LauncherApp>,
    appsReady: Boolean,
    weather: WeatherNow?,
    weatherState: WeatherLoadState,
    use24HourClock: Boolean,
    focusLift: Boolean,
    showAppLabels: Boolean,
    onLaunch: (LauncherApp) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onHiddenApps: () -> Unit,
    onConfigureApp: (LauncherApp) -> Unit,
    movingAppKey: String?,
    dockFocusKey: String?,
    onMoveApp: (LauncherApp, Int) -> Int,
    onMoveDone: () -> Unit,
    onSelect: (MediaItem) -> Unit,
) {
    val listState = rememberLazyListState()
    val appListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val heroFocus = remember { FocusRequester() }
    val appFocus = remember { FocusRequester() }
    val sections = remember(catalog) { launcherMovieSections(catalog) }
    val movieRowFocus = remember(sections) { sections.map { section -> List(section.items.size) { FocusRequester() } } }
    val movieRowState = remember(sections) { sections.map { LazyListState() } }
    val appKeys = remember(apps) { apps.map(::launcherAppKey) }
    var lastAppKey by remember { mutableStateOf<String?>(null) }
    var lastFirstMovieIndex by remember { mutableStateOf(0) }
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    fun focusMovie(row: Int, item: Int) {
        if (row !in movieRowFocus.indices) return
        val target = adjacentRowIndex(item, movieRowFocus[row].size)
        navigationJob?.cancel()
        navigationJob = scope.launch {
            listState.scrollToItem(row + 1)
            movieRowState[row].scrollToItem((target - 2).coerceAtLeast(0))
            withFrameNanos { }
            movieRowFocus[row][target].requestFocus()
        }
    }
    fun focusApps() {
        if (!appsReady) return
        navigationJob?.cancel()
        navigationJob = scope.launch {
            listState.scrollToItem(0)
            val target = dockEntryIndex(appKeys, lastAppKey)
            if (appListState.layoutInfo.visibleItemsInfo.none { it.index == target }) appListState.animateScrollToItem(target)
            withFrameNanos { }
            appFocus.requestFocus()
        }
    }
    BackHandler(enabled = active && movingAppKey == null) { focusApps() }
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
    val context = LocalContext.current
    val heroPreferences = remember(context) { context.getSharedPreferences("launcher", Context.MODE_PRIVATE) }
    val featured = remember(sections) { sections.firstOrNull { it.title == "Discover movies" }?.items
        ?: sections.take(2).flatMap { it.items }.distinctBy(::mediaKey) }
    var hero by remember { mutableStateOf<MediaItem?>(null) }
    var recent by remember { mutableStateOf(listOfNotNull(heroPreferences.getString("lastFeatured", null))) }
    LaunchedEffect(featured) {
        if (featured.none { mediaKey(it) == hero?.let(::mediaKey) }) {
            hero = nextDiscoveryItem(featured, recent)
        }
    }
    LaunchedEffect(hero) {
        hero?.let {
            recent = (recent + mediaKey(it)).takeLast(10)
            heroPreferences.edit().putString("lastFeatured", mediaKey(it)).apply()
        }
    }
    var initiallyFocused by remember { mutableStateOf(false) }
    LaunchedEffect(appsReady) {
        if (!appsReady || initiallyFocused) return@LaunchedEffect
        appListState.scrollToItem(0)
        withFrameNanos { }
        appFocus.requestFocus()
        initiallyFocused = true
    }
    val stageVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    LaunchedEffect(hero, featured, active, stageVisible) {
        if (!active || !stageVisible) return@LaunchedEffect
        delay(20_000)
        nextDiscoveryItem(featured, recent)?.let {
            hero = it
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides stableBringIntoView) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(30.dp),
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Menu -> { onSettings(); true }
                    Key.Search -> { onSearch(); true }
                    else -> false
                }
            },
        ) {
            item(key = "stage", contentType = "stage") { LauncherStage(
                hero,
                weather,
                weatherState,
                use24HourClock,
                modifier = Modifier.fillParentMaxHeight(),
                heroAction = {
                    if (hero == null) Text(tr(if (loading) "Loading discovery…" else "Discovery unavailable · your apps are ready"), color = SecondaryText, fontSize = 13.sp)
                    hero?.let { shown -> ActionButton(
                        "Explore",
                        Modifier.focusRequester(heroFocus).onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                focusApps()
                                true
                            } else false
                        },
                        icon = Icons.Default.Info,
                    ) { onSelect(shown) } }
                },
            ) {
                Column(Modifier.align(Alignment.BottomStart).padding(bottom = 24.dp)) {
                    AppDock(
                        apps, appsReady, appListState, if (hero == null) FocusRequester.Default else heroFocus, appFocus,
                        FocusRequester.Default,
                        focusLift, showAppLabels, onLaunch, onConfigureApp, movingAppKey, dockFocusKey, onMoveApp, onMoveDone, onHiddenApps, onSettings,
                        onRowFocused = { if (navigationJob?.isActive != true) lastAppKey = it },
                        entryKey = lastAppKey,
                        onSearch = onSearch,
                        onDown = if (sections.isNotEmpty()) ({ focusMovie(0, lastFirstMovieIndex) }) else null,
                    )
                }
            } }
            itemsIndexed(sections, key = { _, section -> section.title }, contentType = { _, _ -> "movie-row" }) { visibleIndex, section ->
                val index = visibleIndex
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
                        if (index == 0) lastFirstMovieIndex = itemIndex
                    },
                )
            }
            item(key = "attribution", contentType = "footer") { Text(
                tr("Movies by TMDB · Availability by JustWatch · Weather by Open-Meteo"),
                color = Color.White.copy(alpha = .38f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 48.dp).padding(bottom = 48.dp),
            ) }
        }
    }
}

@Composable
private fun AppsOnlyHome(
    apps: List<LauncherApp>,
    appsReady: Boolean,
    weather: WeatherNow?,
    weatherState: WeatherLoadState,
    use24HourClock: Boolean,
    focusLift: Boolean,
    showAppLabels: Boolean,
    wallpaperSeed: Int,
    footballWidgetEnabled: Boolean,
    onLaunch: (LauncherApp) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onHiddenApps: () -> Unit,
    onConfigureApp: (LauncherApp) -> Unit,
    movingAppKey: String?,
    dockFocusKey: String?,
    onMoveApp: (LauncherApp, Int) -> Int,
    onMoveDone: () -> Unit,
) {
    val appListState = rememberLazyListState()
    val appFocus = remember { FocusRequester() }
    var football by remember { mutableStateOf<FootballSnapshot?>(null) }
    var footballState by remember { mutableStateOf(WeatherLoadState.Loading) }
    val wallpaper = remember(wallpaperSeed) {
        "https://picsum.photos/seed/reelora-${LocalDate.now().toEpochDay() + wallpaperSeed}/1920/1080"
    }
    var initiallyFocused by remember { mutableStateOf(false) }
    BackHandler(enabled = movingAppKey == null) { }
    LaunchedEffect(appsReady) {
        if (!appsReady || initiallyFocused) return@LaunchedEffect
        appListState.scrollToItem(0)
        withFrameNanos { }
        appFocus.requestFocus()
        initiallyFocused = true
    }
    val isForeground = LocalForeground.current
    val online = LocalInternet.current
    var footballRefreshAt by remember { mutableStateOf(0L) }
    LaunchedEffect(footballWidgetEnabled, isForeground, online, appsReady) {
        if (!footballWidgetEnabled || !isForeground || !online || !appsReady) return@LaunchedEffect
        footballRefreshAt = SystemClock.elapsedRealtime() + widgetRefreshDelay(SystemClock.elapsedRealtime(), footballRefreshAt, footballState != WeatherLoadState.Ready)
        withFrameNanos { }
        delay(250)
        while (true) {
            delay((footballRefreshAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            val latest = FootballRepository.load()
            if (latest != null) football = latest
            footballState = if (latest == null) WeatherLoadState.Error else WeatherLoadState.Ready
            footballRefreshAt = SystemClock.elapsedRealtime() + if (latest == null) 60_000L else footballRefreshInterval(latest)
        }
    }
    Box(
        Modifier.fillMaxSize().clipToBounds().background(Background).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                Key.Menu -> { onSettings(); true }
                Key.Search -> { onSearch(); true }
                else -> false
            }
        },
    ) {
        AsyncImage(
            model = artworkModel(wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Background.copy(alpha = .70f), .40f to Background.copy(alpha = .42f), .70f to Color.Transparent, 1f to Background.copy(alpha = .68f))))
        HomeStatus(weather, weatherState, use24HourClock, Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 58.dp))
        if (footballWidgetEnabled) FootballWidget(
            football,
            footballState,
            Modifier.align(Alignment.TopStart).padding(top = 28.dp, start = 58.dp),
        )
        AppDock(
            apps = apps,
            listState = appListState,
            upFocus = FocusRequester.Default,
            firstFocus = appFocus,
            downFocus = FocusRequester.Default,
            focusLift = focusLift,
            showLabels = showAppLabels,
            onLaunch = onLaunch,
            onConfigureApp = onConfigureApp,
            movingAppKey = movingAppKey,
            dockFocusKey = dockFocusKey,
            onMoveApp = onMoveApp,
            onMoveDone = onMoveDone,
            onHiddenApps = onHiddenApps,
            onSettings = onSettings,
            onSearch = onSearch,
            appsReady = appsReady,
            onRowFocused = {},
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
        )
    }
}

private val FootballTextStyle = androidx.compose.ui.text.TextStyle(
    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = .6f), androidx.compose.ui.geometry.Offset(0f, 1f), 3f),
)

@Composable
internal fun FootballWidget(snapshot: FootballSnapshot?, state: WeatherLoadState, modifier: Modifier = Modifier) {
    val softWhite = Color(0xFFE9E8E3)
    Column(modifier.widthIn(max = 490.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(androidx.compose.ui.res.painterResource(R.drawable.football_ball), contentDescription = null,
                tint = softWhite.copy(alpha = .8f), modifier = Modifier.size(20.dp))
            Text(tr("Football"), color = softWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium, style = FootballTextStyle)
        }
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = snapshot,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(100)) },
            label = "football update",
        ) { shown ->
            if (shown == null) {
                Text(tr(if (!LocalInternet.current) "Waiting for internet" else if (state == WeatherLoadState.Error) "Fixtures unavailable · retrying" else "Loading fixtures…"),
                    color = softWhite.copy(alpha = .85f), fontSize = 17.sp, style = FootballTextStyle)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (shown.live != null) {
                        FootballMatchSummary("Live", shown.live, live = true)
                        if (shown.next != null) {
                            FootballGroupDivider()
                            FootballMatchSummary("Upcoming", shown.next)
                        }
                    } else {
                        if (shown.next != null) FootballMatchSummary("Upcoming", shown.next)
                        if (shown.previous != null) {
                            if (shown.next != null) FootballGroupDivider()
                            FootballMatchSummary("Previous", shown.previous)
                        }
                    }
                    if (state == WeatherLoadState.Error || shown.hint != null) {
                        Text(
                            if (state == WeatherLoadState.Error) tr("Fixtures unavailable · retrying") else footballHintText(shown.hint, LocalRomanian.current),
                            color = softWhite.copy(alpha = .72f), fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2, style = FootballTextStyle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FootballGroupDivider() {
    Box(Modifier.fillMaxWidth(.78f).height(1.dp).background(
        Brush.horizontalGradient(listOf(Color.White.copy(alpha = .18f), Color.White.copy(alpha = .03f))),
    ))
}

@Composable
private fun FootballMatchSummary(label: String, match: FootballMatch, live: Boolean = false) {
    val accent = if (live) Color(0xFFA8E4C3) else Color(0xFFE9E8E3)
    val secondary = Color(0xFFD1D5D6)
    val statusColor = if (live) accent else if (label == "Upcoming") Color(0xFFB6D6F4) else Color(0xFFD0C6E4)
    val locale = if (LocalRomanian.current) java.util.Locale.forLanguageTag("ro") else java.util.Locale.ENGLISH
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(7.dp)).background(statusColor.copy(alpha = .10f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                when {
                    live -> Box(Modifier.size(6.dp).background(statusColor, CircleShape))
                    label == "Previous" -> Icon(Icons.Default.Check, contentDescription = null, tint = statusColor, modifier = Modifier.size(13.dp))
                    else -> Icon(androidx.compose.ui.res.painterResource(R.drawable.fixture_calendar), contentDescription = null,
                        tint = statusColor, modifier = Modifier.size(13.dp))
                }
                Text(tr(label), color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, style = FootballTextStyle)
            }
            if (match.competition.isNotBlank()) {
                Text("·", color = secondary, fontSize = 12.sp)
                Text(match.competition.lowercase().split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
                    color = secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, style = FootballTextStyle)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FootballTeam(match.home, match.homeId, match.homeCountry, Modifier.weight(1f, fill = false))
            Text(footballScore(match), Modifier.widthIn(min = 32.dp).clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = if (live) .12f else if (match.homeScore != null) .06f else 0f)).padding(horizontal = 8.dp, vertical = 3.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = accent,
                fontSize = if (match.homeScore == null || match.awayScore == null) 16.sp else 22.sp,
                fontWeight = FontWeight.SemiBold, style = FootballTextStyle)
            FootballTeam(match.away, match.awayId, match.awayCountry, Modifier.weight(1f, fill = false))
        }
        Text(footballSchedule(match, locale), color = secondary.copy(alpha = .85f), fontSize = 12.sp,
            lineHeight = 16.sp, maxLines = 2, style = FootballTextStyle)
    }
}

@Composable
private fun FootballTeam(name: String, id: String, knownCountry: String, modifier: Modifier) {
    val online = LocalInternet.current
    val country by androidx.compose.runtime.produceState(knownCountry, id, knownCountry, online) {
        value = knownCountry
        if (knownCountry.isBlank() && online) value = FootballRepository.teamCountry(id)
    }
    val flag = remember(country) { footballCountryFlag(country) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        // Reserve the tiny flag slot so a network result never shifts the team name.
        Box(Modifier.width(with(androidx.compose.ui.platform.LocalDensity.current) { 20.sp.toDp() }), contentAlignment = Alignment.Center) {
            if (flag != null) Text(flag, fontSize = 17.sp, maxLines = 1,
                modifier = Modifier.semantics { contentDescription = country })
        }
        Text(name, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp,
            fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, style = FootballTextStyle)
    }
}

internal fun footballScore(match: FootballMatch) =
    if (match.homeScore != null && match.awayScore != null) "${match.homeScore}–${match.awayScore}" else "vs"

internal fun footballSchedule(match: FootballMatch, locale: java.util.Locale = java.util.Locale.ENGLISH): String {
    val date = runCatching { LocalDate.parse(match.date) }.getOrNull()
        ?.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)) ?: match.date
    return listOf(date, match.time).filter(String::isNotBlank).joinToString(" · ")
}

internal fun footballHintText(hint: FootballHint?, romanian: Boolean): String {
    if (hint == null) return if (romanian) "Urmărim World Cup, Champions League și Premier League" else "Following World Cup, Champions League and Premier League"
    val competition = hint.competition.lowercase().split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    return when (hint.days) {
        0 -> if (romanian) "$competition · azi" else "$competition · today"
        1 -> if (romanian) "$competition · mâine" else "$competition · tomorrow"
        else -> if (romanian) "$competition · în ${hint.days} zile" else "$competition · in ${hint.days} days"
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppDock(
    apps: List<LauncherApp>,
    appsReady: Boolean,
    listState: LazyListState,
    upFocus: FocusRequester,
    firstFocus: FocusRequester,
    downFocus: FocusRequester,
    focusLift: Boolean,
    showLabels: Boolean,
    onLaunch: (LauncherApp) -> Unit,
    onConfigureApp: (LauncherApp) -> Unit,
    movingAppKey: String?,
    dockFocusKey: String?,
    onMoveApp: (LauncherApp, Int) -> Int,
    onMoveDone: () -> Unit,
    onHiddenApps: () -> Unit,
    onSettings: () -> Unit,
    onRowFocused: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onDown: (() -> Unit)? = null,
    entryKey: String? = null,
) {
    val appKeys = remember(apps) { apps.map(::launcherAppKey) }
    val entry = dockEntryIndex(appKeys, entryKey)
    val reveal = remember { Animatable(if (appsReady) 1f else 0f) }
    LaunchedEffect(appsReady) {
        if (appsReady && reveal.value < 1f) reveal.animateTo(1f, tween(TvMotion.EnterMillis, easing = LinearOutSlowInEasing))
    }
    val scope = rememberCoroutineScope()
    var moveJob by remember { mutableStateOf<Job?>(null) }
    val returnFocus = remember { FocusRequester() }
    LaunchedEffect(dockFocusKey, appsReady) {
        if (!appsReady || dockFocusKey == null) return@LaunchedEffect
        val index = apps.indexOfFirst { launcherAppKey(it) == dockFocusKey }
            .takeIf { it >= 0 } ?: if (dockFocusKey == "hidden") apps.size + 1 else return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.scrollToItem(index)
        withFrameNanos { }
        returnFocus.requestFocus()
    }
    Box(
        modifier.fillMaxWidth().height(if (showLabels || movingAppKey != null) 116.dp else 92.dp)
            .padding(horizontal = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xD4141820)),
    ) {
        if (!appsReady) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(5) {
                    Box(Modifier.size(116.dp, 68.dp).background(Color.White.copy(alpha = .05f), RoundedCornerShape(14.dp)))
                }
            }
            return@Box
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides RowBringIntoViewSpec) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().graphicsLayer {
                // One layer for the shelf: drawing-only motion, no per-frame layout or per-app stagger.
                alpha = reveal.value
                translationY = (1f - reveal.value) * TvMotion.EnterDistance.toPx()
            }.focusGroup().onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && onDown != null && movingAppKey == null) {
                    onDown()
                    true
                } else false
            },
        ) {
            itemsIndexed(
                apps,
                key = { _, app -> app.component.flattenToShortString() },
                contentType = { _, _ -> "app" },
            ) { index, app ->
                AppCard(
                    app,
                    focusLift,
                    showLabels,
                    onLaunch,
                    onConfigureApp,
                    moving = launcherAppKey(app) == movingAppKey,
                    movePosition = "${index + 1}/${apps.size}",
                    onMove = { offset ->
                        val destination = onMoveApp(app, offset)
                        moveJob?.cancel()
                        moveJob = scope.launch {
                            withFrameNanos { }
                            if (listState.layoutInfo.visibleItemsInfo.none { it.index == destination }) listState.scrollToItem(destination)
                        }
                    },
                    onMoveDone = onMoveDone,
                    onFocused = { onRowFocused(launcherAppKey(app)) },
                    modifier = Modifier.animateItem(fadeInSpec = tween(TvMotion.InsertMillis), placementSpec = tween(TvMotion.EnterMillis, easing = LinearOutSlowInEasing), fadeOutSpec = tween(TvMotion.RemoveMillis))
                        .then(if (index == entry) Modifier.focusRequester(firstFocus) else Modifier)
                        .then(if (launcherAppKey(app) == dockFocusKey) Modifier.focusRequester(returnFocus) else Modifier)
                        .focusProperties { up = upFocus; down = downFocus },
                )
            }
            item(key = "search", contentType = "action") {
                ShelfActionCard(
                    "Search", Icons.Default.Search, focusLift, showLabels, onSearch,
                    Modifier.then(if (entry == apps.size) Modifier.focusRequester(firstFocus) else Modifier)
                        .focusProperties { up = upFocus; down = downFocus },
                    onFocused = { onRowFocused("search") },
                )
            }
            item(key = "hidden") {
                ShelfActionCard(
                    "Hidden", HideAppIcon, focusLift, showLabels, onHiddenApps,
                    Modifier.then(if (dockFocusKey == "hidden") Modifier.focusRequester(returnFocus) else Modifier)
                        .then(if (entry == apps.size + 1) Modifier.focusRequester(firstFocus) else Modifier).focusProperties { up = upFocus; down = downFocus },
                    onFocused = { onRowFocused("hidden") },
                )
            }
            item(key = "settings") {
                ShelfActionCard(
                    "Settings", Icons.Default.Settings, focusLift, showLabels, onSettings,
                    Modifier.then(if (entry == apps.size + 2) Modifier.focusRequester(firstFocus) else Modifier).focusProperties { up = upFocus; down = downFocus },
                    onFocused = { onRowFocused("settings") },
                )
            }
        }
        }
    }
}

@Composable
private fun ShelfActionCard(
    label: String,
    icon: ImageVector,
    focusLift: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused() }
    val width = 116.dp
    val height = 68.dp
    Card(
        onClick = onClick,
        modifier = modifier.width(width).zIndex(if (focused) 1f else 0f),
        colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
        scale = CardDefaults.scale(focusedScale = if (focusLift) 1.065f else 1f, pressedScale = .99f),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None, pressedBorder = Border.None),
        interactionSource = interaction,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.width(width).height(height).clip(RoundedCornerShape(16.dp))
                    .background(if (focused) FocusSurface else ControlSurface)
                    .border(1.dp, if (focused) FocusSurface else SubtleBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = if (focused) FocusContent else SecondaryText, modifier = Modifier.size(28.dp))
            }
            if (showLabel) {
                Spacer(Modifier.height(8.dp))
                Text(tr(label), color = Color.White.copy(alpha = if (focused) 1f else .76f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppCard(
    app: LauncherApp,
    focusLift: Boolean,
    showLabel: Boolean,
    onLaunch: (LauncherApp) -> Unit,
    onConfigure: (LauncherApp) -> Unit,
    moving: Boolean,
    movePosition: String,
    onMove: (Int) -> Unit,
    onMoveDone: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val movePress = remember(moving) { RemotePressGate() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused() }
    val tileWidth = 116.dp
    val tileHeight = 68.dp
    val tileBackground = remember { Brush.linearGradient(listOf(Color(0xFF242936), Color(0xFF171A22))) }
    Card(
        onClick = { if (moving) onMoveDone() else onLaunch(app) },
        onLongClick = { if (!moving) onConfigure(app) },
        modifier = modifier.width(tileWidth)
            .zIndex(if (focused || moving) 1f else 0f)
            .onPreviewKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                if (moving) {
                    when {
                        event.type == KeyEventType.KeyDown && keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT -> onMove(-1)
                        event.type == KeyEventType.KeyDown && keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> onMove(1)
                        event.type == KeyEventType.KeyDown && keyCode == android.view.KeyEvent.KEYCODE_BACK -> onMoveDone()
                        keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP || keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> Unit
                        keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
                            keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == android.view.KeyEvent.KEYCODE_BUTTON_A -> {
                            val consumed = movePress.consume(keyCode, event.type == KeyEventType.KeyDown, event.nativeKeyEvent.repeatCount, event.nativeKeyEvent.downTime)
                            if (!consumed && event.type == KeyEventType.KeyUp) onMoveDone()
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                    return@onPreviewKeyEvent true
                }
                false
            },
        colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
        scale = CardDefaults.scale(
            scale = if (moving) 1.065f else 1f,
            focusedScale = if (moving) 1.065f else if (focusLift) 1.065f else 1f,
            pressedScale = if (moving) 1.065f else .99f,
        ),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None, pressedBorder = Border.None),
        interactionSource = interaction,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.width(tileWidth).height(tileHeight).clip(RoundedCornerShape(16.dp))
                    .background(if (app.banner == null) tileBackground else SolidColor(Color(0xFF171720))),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = app.banner ?: app.icon,
                    contentDescription = app.name,
                    contentScale = if (app.banner == null) ContentScale.Fit else ContentScale.Crop,
                    modifier = if (app.banner == null) Modifier.fillMaxWidth(.82f).fillMaxHeight(.78f) else Modifier.fillMaxSize(),
                )
                if (focused || moving) Box(
                    Modifier.fillMaxSize().border(
                        if (moving) 3.dp else 2.dp,
                        if (moving) WarmAccent else FocusSurface,
                        RoundedCornerShape(16.dp),
                    )
                )
            }
            if (showLabel || moving) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (moving) (if (LocalRomanian.current) "←  MUTĂ $movePosition  →" else "←  MOVE $movePosition  →") else app.name,
                    color = if (moving) WarmAccent else Color.White.copy(alpha = if (focused) 1f else .76f),
                    fontSize = 11.sp,
                    fontWeight = if (moving) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TvDialog(
    onDismiss: () -> Unit,
    modifier: Modifier,
    ambient: Boolean = false,
    previewBackground: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.BoxScope.(() -> Unit) -> Unit,
) {
    val confirmPress = remember { RemotePressGate() }
    val reveal = remember { Animatable(0f) }
    var ready by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val close = {
        if (!closing) {
            closing = true
            scope.launch {
                reveal.animateTo(0f, tween(100))
                onDismiss()
            }
        }
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            window?.setWindowAnimations(0)
            window?.setDimAmount(0f)
            onDispose { }
        }
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Background.copy(alpha = (if (previewBackground) .56f else .9f) * reveal.value))
            }.onPreviewKeyEvent { event ->
                val native = event.nativeKeyEvent
                if (closing) true
                else when (native.keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER,
                    android.view.KeyEvent.KEYCODE_NUMPAD_ENTER, android.view.KeyEvent.KEYCODE_BUTTON_A ->
                        confirmPress.consume(native.keyCode, event.type == KeyEventType.KeyDown, native.repeatCount, native.downTime, ready)
                    else -> false
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            if (ambient) AmbientBackdrop()
            Box(
                modifier.graphicsLayer {
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * TvMotion.EnterDistance.toPx()
                }.clip(DialogShape).background(Surface)
                    .border(1.dp, Color.White.copy(alpha = .08f), DialogShape),
            ) {
                CompositionLocalProvider(LocalDialogReady provides ready) { content(close) }
            }
        }
        LaunchedEffect(Unit) {
            withFrameNanos { }
            if (closing) return@LaunchedEffect
            reveal.animateTo(1f, tween(TvMotion.EnterMillis, easing = LinearOutSlowInEasing))
            ready = true
        }
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
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        modifier = modifier.onFocusChanged { focused = it.isFocused }.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                keyboard?.hide()
                focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
            } else false
        },
        decorationBox = { field ->
            Box(
                Modifier.fillMaxWidth().height(60.dp).clip(ControlShape)
                    .background(Color.White.copy(alpha = if (focused) .1f else .055f))
                    .border(if (focused) 2.dp else 1.dp, if (focused) Accent else Color.White.copy(alpha = .1f), ControlShape)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(tr(placeholder), color = Color.White.copy(alpha = .4f), fontSize = 20.sp)
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
    var action by remember { mutableStateOf<(() -> Unit)?>(null) }
    TvDialog({ action?.invoke() ?: onDismiss() }, Modifier.fillMaxWidth(.72f)) { close ->
        val ready = LocalDialogReady.current
        LaunchedEffect(ready) { if (ready) first.requestFocus() }
        fun choose(next: () -> Unit) { action = next; close() }
        Column(Modifier.padding(DialogPadding)) {
            DialogHeader(app.name, "App options",
                leading = { AsyncImage(app.icon, null, Modifier.size(48.dp), contentScale = ContentScale.Fit) },
                action = { ActionButton("Close", icon = Icons.Default.Close, onClick = close) },
            )
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppOptionTile("Move", "Reorder on Home", Icons.AutoMirrored.Filled.List, Modifier.focusRequester(first)) { choose(onMove) }
                AppOptionTile("Rename", "Change the name shown on Home", Icons.Default.Edit) { choose(onRename) }
                AppOptionTile("App info", "Manage or uninstall", Icons.Default.Info) { choose(onAppInfo) }
                AppOptionTile("Hide", "Restore anytime from Hidden apps", HideAppIcon) { choose(onHide) }
            }
        }
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
    var action by remember { mutableStateOf<(() -> Unit)?>(null) }
    TvDialog({ action?.invoke() ?: onDismiss() }, Modifier.fillMaxWidth(.72f)) { close ->
        val ready = LocalDialogReady.current
        LaunchedEffect(ready) { if (ready) { fieldFocus.requestFocus(); keyboard?.show() } }
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
                    ActionButton("Reset", icon = Icons.Default.Refresh, onClick = { keyboard?.hide(); action = onReset; close() })
                    Spacer(Modifier.width(Gap))
                    ActionButton("Cancel", icon = Icons.Default.Close, onClick = close)
                    Spacer(Modifier.width(Gap))
                    ActionButton("Save", icon = Icons.Default.Check, enabled = name.isNotBlank()) {
                        keyboard?.hide()
                        action = { onSave(name.trim()) }
                        close()
                    }
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
    val done = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val restoreFocus = remember(apps) { List(apps.size) { FocusRequester() } }
    var restoredIndex by remember { mutableStateOf<Int?>(null) }
    TvDialog(onDismiss, Modifier.fillMaxWidth(.86f).fillMaxHeight(.82f)) { close ->
        val ready = LocalDialogReady.current
        LaunchedEffect(ready, apps) {
            if (!ready) return@LaunchedEffect
            val index = restoredIndex
            if (apps.isEmpty() || index == null) done.requestFocus() else {
                val target = index.coerceAtMost(apps.lastIndex)
                listState.scrollToItem(target)
                withFrameNanos { }
                restoreFocus[target].requestFocus()
            }
            restoredIndex = null
        }
        Column(Modifier.padding(DialogPadding)) {
            DialogHeader("Hidden apps", "Open an app or return it to Home", action = {
                ActionButton("Done", Modifier.focusRequester(done), icon = Icons.Default.Check, onClick = close)
            })
            Spacer(Modifier.height(GapLarge))
            if (apps.isEmpty()) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Home, null, tint = Accent, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(tr("No hidden apps"), fontSize = 22.sp, color = Color.White)
                    Text(tr("All your apps are on Home"), fontSize = 13.sp, color = Color.White.copy(alpha = .55f))
                }
            } else LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(apps, key = { _, app -> launcherAppKey(app) }, contentType = { _, _ -> "hidden-app" }) { index, app ->
                    Row(Modifier.animateItem(fadeInSpec = tween(TvMotion.InsertMillis), placementSpec = tween(TvMotion.EnterMillis, easing = LinearOutSlowInEasing), fadeOutSpec = tween(TvMotion.RemoveMillis)).fillMaxWidth().clip(ControlShape).background(Color.White.copy(alpha = .035f)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AsyncImage(app.icon, null, Modifier.size(42.dp), contentScale = ContentScale.Fit)
                        Text(app.name, modifier = Modifier.weight(1f), fontSize = 17.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        ActionButton("Open", icon = Icons.Default.PlayArrow) { onLaunch(app) }
                        ActionButton("Show on Home", Modifier.focusRequester(restoreFocus[index]), icon = Icons.Default.Home) {
                            restoredIndex = index
                            onRestore(app)
                        }
                    }
                }
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
    TvDialog(onDismiss, Modifier.fillMaxWidth(.86f).fillMaxHeight(.88f)) { close ->
        val ready = LocalDialogReady.current
        LaunchedEffect(ready) { if (ready) { field.requestFocus(); keyboard?.show() } }
        Column(Modifier.padding(DialogPadding)) {
            DialogHeader("Weather location", "Search, choose, then confirm")
            Spacer(Modifier.height(GapLarge))
            TvTextField(value, { value = it.take(60) }, "City", Modifier.fillMaxWidth().focusRequester(field))
            Spacer(Modifier.height(Gap))
            Text(
                tr(when {
                    loading -> "Searching…"
                    selected != null -> "Selected · ${selected?.label}"
                    value.trim().length < 2 -> "Type at least two letters"
                    suggestions.isEmpty() -> "No locations found"
                    else -> "Choose the correct location"
                }),
                color = SecondaryText,
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
    section: Int,
    onSectionChange: (Int) -> Unit,
    updateStatus: UpdateStatus,
    onUpdate: () -> Unit,
    theaterEnabled: Boolean,
    idleMinutes: Int,
    focusLift: Boolean,
    showAppLabels: Boolean,
    moviesEnabled: Boolean,
    footballWidgetEnabled: Boolean,
    romanian: Boolean,
    weatherLocation: String,
    weatherCelsius: Boolean,
    use24HourClock: Boolean,
    hiddenAppCount: Int,
    onSearch: () -> Unit,
    onTheaterEnabled: (Boolean) -> Unit,
    onIdleMinutes: (Int) -> Unit,
    onFocusLift: (Boolean) -> Unit,
    onShowAppLabels: (Boolean) -> Unit,
    onMoviesEnabled: (Boolean) -> Unit,
    onNextWallpaper: () -> Unit,
    onFootballWidget: (Boolean) -> Unit,
    onWeatherLocation: () -> Unit,
    onWeatherCelsius: (Boolean) -> Unit,
    onClockFormat: (Boolean) -> Unit,
    onLanguage: (Boolean) -> Unit,
    onHiddenApps: () -> Unit,
    onSystemSettings: () -> Unit,
    onHomeSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val categoryFocus = remember { List(4) { FocusRequester() } }
    val titles = listOf("Home", "Apps", "Weather & time", "System")
    val contentScroll = rememberScrollState()
    LaunchedEffect(section) { contentScroll.scrollTo(0) }
    TvDialog(onDismiss, Modifier.fillMaxWidth(.9f).fillMaxHeight(.88f)) { close ->
        val ready = LocalDialogReady.current
        LaunchedEffect(ready) { if (ready) categoryFocus[section].requestFocus() }
        Column(Modifier.padding(DialogPadding)) {
            DialogHeader("Settings", "A quiet home for apps and discovery", action = {
                ActionButton("Done", icon = Icons.Default.Check, onClick = close)
            })
            Spacer(Modifier.height(24.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(Modifier.width(184.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    titles.forEachIndexed { index, title ->
                        ActionButton(title, Modifier.fillMaxWidth().focusRequester(categoryFocus[index]), onFocused = { onSectionChange(index) }, isSelected = section == index) { onSectionChange(index) }
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = .08f)))
                Column(Modifier.weight(1f).verticalScroll(contentScroll).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(tr(titles[section]), color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    when (section) {
                        0 -> {
                            Text(tr("Featured movies or a calm wallpaper"), color = Color.White, fontSize = 21.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                                ActionButton(if (moviesEnabled) "Movies on" else "Apps only") { onMoviesEnabled(!moviesEnabled) }
                                ActionButton("Search movies & TV", icon = Icons.Default.Search, onClick = onSearch)
                            }
                            if (!moviesEnabled) ActionButton("New wallpaper", icon = Icons.Default.Refresh, onClick = onNextWallpaper)
                            if (moviesEnabled) {
                                Text(tr("Ambient trailers after a quiet moment"), color = SecondaryText, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                                    ActionButton(if (theaterEnabled) "Theater on" else "Theater off") { onTheaterEnabled(!theaterEnabled) }
                                    ActionButton("After $idleMinutes min", enabled = theaterEnabled) { onIdleMinutes(nextTheaterIdleMinutes(idleMinutes)) }
                                }
                            } else {
                                ActionButton(if (footballWidgetEnabled) "Football on" else "Football off") { onFootballWidget(!footballWidgetEnabled) }
                                Text(tr("Daily wallpaper · Picsum"), color = SecondaryText, fontSize = 13.sp)
                            }
                        }
                        1 -> {
                            Text(tr("Manage Home apps"), color = Color.White, fontSize = 21.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                                ActionButton("Hidden · $hiddenAppCount", icon = Icons.Default.Home, onClick = onHiddenApps)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                                ActionButton(if (showAppLabels) "Labels on" else "Labels off") { onShowAppLabels(!showAppLabels) }
                                ActionButton(if (focusLift) "Lifted focus" else "Outline focus") { onFocusLift(!focusLift) }
                            }
                            Text(tr("Hold an app to move, rename or hide it"), color = SecondaryText, fontSize = 13.sp)
                        }
                        2 -> {
                            Text(tr("Location, temperature and clock"), color = Color.White, fontSize = 21.sp)
                            ActionButton(weatherLocation, Modifier.fillMaxWidth(), onClick = onWeatherLocation)
                            Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                                ActionButton(if (weatherCelsius) "°C" else "°F") { onWeatherCelsius(!weatherCelsius) }
                                ActionButton(if (use24HourClock) "24 h" else "12 h") { onClockFormat(!use24HourClock) }
                            }
                        }
                        3 -> {
                            Text(tr("Home and Android controls"), color = Color.White, fontSize = 21.sp)
                            ActionButton("Device settings", Modifier.fillMaxWidth(), icon = Icons.Default.Settings, onClick = onSystemSettings)
                            Text(tr("Network, display, sound and Android system"), color = SecondaryText, fontSize = 13.sp)
                            ActionButton("Default home", icon = Icons.Default.Home, onClick = onHomeSettings)
                            ActionButton(if (romanian) "Română" else "English") { onLanguage(!romanian) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .08f)))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Gap)) {
                Text("v${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = .44f), fontSize = 12.sp)
                AnimatedContent(
                    targetState = if (updateStatus.busy) updateStatus.label else updateStatus.message,
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentAlignment = Alignment.CenterEnd,
                    transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(90)) }, label = "update status",
                ) { message -> Text(tr(message), color = if (updateStatus.busy) Accent else Color.White.copy(alpha = .68f), fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.End) }
                ActionButton(if (updateStatus.label == "Install update") "Install update" else "Check for updates", Modifier.widthIn(min = 214.dp), icon = Icons.Default.Refresh, enabled = !updateStatus.busy) {
                    if (!updateStatus.busy) onUpdate()
                }
            }
        }
    }
}

@Composable
private fun SearchDialog(
    visible: Boolean,
    suggestions: List<MediaItem>,
    onDismiss: () -> Unit,
    onSelect: (MediaItem) -> Unit,
) {
    val isForeground = LocalForeground.current
    var completedQuery by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<MediaItem>()) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    val resultState = rememberLazyListState()
    var openedOnce by remember { mutableStateOf(false) }
    val inputRequester = remember { FocusRequester() }
    val resultRequester = remember { FocusRequester() }
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
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { query = it.take(80) }
        }
    }

    LaunchedEffect(query.trim(), retry, isForeground) {
        if (!isForeground) return@LaunchedEffect
        if (completedQuery == query.trim() && !failed) { loading = false; return@LaunchedEffect }
        resultState.scrollToItem(0)
        val term = query.trim()
        failed = false
        if (term.length < 2) {
            results = emptyList()
            completedQuery = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(350)
        CatalogRepository.searchResult(term).fold(
            onSuccess = { results = it; completedQuery = term },
            onFailure = { results = emptyList(); failed = true },
        )
        loading = false
    }

    if (!visible) return
    val shown = if (query.trim().length < 2) suggestions else results
    TvDialog(onDismiss, Modifier.fillMaxWidth(.9f).fillMaxHeight(.78f)) { close ->
        val ready = LocalDialogReady.current
        LaunchedEffect(ready) {
            if (ready) {
                if (openedOnce && shown.isNotEmpty() && !failed) {
                    resultRequester.requestFocus()
                } else {
                    inputRequester.requestFocus()
                    if (!openedOnce) keyboard?.show()
                }
                openedOnce = true
            }
        }
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
                ActionButton("Clear", icon = Icons.Default.Close, enabled = query.isNotEmpty()) {
                    query = ""
                    inputRequester.requestFocus()
                    keyboard?.show()
                }
                if (voiceAvailable) ActionButton("Voice") { voice.launch(voiceIntent) }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                tr(when {
                    query.trim().length == 1 -> "Type at least two letters"
                    query.isBlank() && suggestions.isEmpty() -> "Find your next movie"
                    query.isBlank() -> "Popular now"
                    failed -> "Search unavailable · try again"
                    loading -> "Finding suggestions…"
                    shown.isEmpty() -> "No matches"
                    else -> "Suggestions"
                }),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                if (shown.isNotEmpty() && !failed) PosterStrip(
                    shown, state = resultState, modifier = Modifier.graphicsLayer { alpha = if (loading) .35f else 1f },
                    itemModifier = { if (it == resultState.firstVisibleItemIndex) Modifier.focusRequester(resultRequester) else Modifier },
                    onSelect = { if (!loading) { keyboard?.hide(); onSelect(it) } },
                )
                else if (loading) LoadingPosterRow()
                else Column(Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr(if (failed) "Check your connection and search again" else if (query.trim().length >= 2) "Try a different title" else "Search movies, series and animation"),
                    color = SecondaryText, fontSize = 14.sp)
                    if (failed) ActionButton("Retry", icon = Icons.Default.Refresh) { retry += 1 }
                }
            }
        }
    }
}

@Composable
private fun LauncherStage(
    item: MediaItem?,
    weather: WeatherNow?,
    weatherState: WeatherLoadState,
    use24HourClock: Boolean,
    modifier: Modifier = Modifier,
    heroAction: @Composable () -> Unit,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    var displayed by remember { mutableStateOf(item) }
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(item) {
        if (displayed == item) return@LaunchedEffect
        reveal.snapTo(0f)
        displayed = item
        reveal.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(548.dp)
            .clipToBounds()
            .background(Background)
    ) {
        displayed?.backdropUrl?.let {
            AsyncImage(
                model = artworkModel(it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = reveal.value
                },
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Background.copy(alpha = .84f),
                    .5f to Background.copy(alpha = .42f),
                    1f to Background.copy(alpha = .18f),
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Background.copy(alpha = .55f),
                    .28f to Color.Transparent,
                    .68f to Color.Transparent,
                    1f to Background,
                )
            )
        )
        Column(
            Modifier.align(Alignment.TopStart).width(550.dp).padding(start = 58.dp, top = 104.dp, end = 24.dp)
                .graphicsLayer {
                    translationX = (1f - reveal.value) * 10f
                    alpha = reveal.value
                },
        ) {
            Text(
                displayed?.title ?: tr("Your apps, ready"),
                color = Color.White,
                fontSize = 32.sp,
                lineHeight = 37.sp,
                letterSpacing = (-.7).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                displayed?.let { movie ->
                    releaseLabel(movie.releaseDate).takeIf { it.startsWith("◷ COMING ") }?.let { tr(it) }
                        ?: "${movie.year}   ·   ${tr(mediaRating(movie))}"
                } ?: "",
                color = Accent.copy(alpha = .95f),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                displayed?.overview.orEmpty(),
                color = Color.White.copy(alpha = .76f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(18.dp))
            heroAction()
        }
        HomeStatus(weather, weatherState, use24HourClock, Modifier.align(Alignment.TopEnd).padding(top = 28.dp, end = 58.dp))
        content()
    }
}

@Composable
internal fun HomeStatus(
    weather: WeatherNow?,
    weatherState: WeatherLoadState,
    use24HourClock: Boolean,
    modifier: Modifier = Modifier,
) {
    var time by remember { mutableStateOf(LocalTime.now()) }
    val isForeground = LocalForeground.current
    LaunchedEffect(isForeground) {
        if (!isForeground) return@LaunchedEffect
        time = LocalTime.now()
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            time = LocalTime.now()
        }
    }
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(
            formatHomeTime(time, use24HourClock), color = Color.White, fontSize = 28.sp,
            fontWeight = FontWeight.Medium, letterSpacing = (-.5).sp,
        )
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = weather,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(100)) },
            label = "weather update",
        ) { shown ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                val kind = shown?.let { weatherKind(it.code, it.isDay) } ?: WeatherKind.Unknown
                if (shown == null) {
                    Icon(Icons.Default.Refresh, contentDescription = tr("Loading weather"), tint = Color(0xFFB4D8FF), modifier = Modifier.size(32.dp))
                } else {
                    Icon(androidx.compose.ui.res.painterResource(kind.icon), contentDescription = tr(kind.label),
                        tint = Color(kind.color), modifier = Modifier.size(32.dp))
                }
                Text(shown?.let { "${it.temperature}°" } ?: "—°", color = Color.White, fontSize = 27.sp,
                    fontWeight = FontWeight.Medium, style = FootballTextStyle)
            }
        }
        if (weather == null || weatherState == WeatherLoadState.Error) {
            Text(tr(if (!LocalInternet.current) "Waiting for internet" else if (weatherState == WeatherLoadState.Error) "Updating weather" else "Loading weather"),
                color = Color.White.copy(alpha = .85f), fontSize = 13.sp, style = FootballTextStyle)
        }
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
        Text(tr(section.title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 48.dp))
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
                            Key.DirectionLeft -> index == 0
                            Key.DirectionRight -> index == section.items.lastIndex
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
    itemWidth: androidx.compose.ui.unit.Dp = 196.dp,
    itemHeight: androidx.compose.ui.unit.Dp = 116.dp,
) {
    val rowState = state ?: rememberLazyListState()
    LazyRow(
        state = rowState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.focusGroup(),
    ) {
        itemsIndexed(items, key = { _, item -> mediaKey(item) }, contentType = { _, _ -> "poster" }) { index, item ->
            PosterCard(
                item,
                onSelect,
                liftOnFocus,
                (if (index == 0) firstModifier else Modifier).then(itemModifier(index)),
                itemWidth,
                itemHeight,
            )
        }
    }
}

@Composable
private fun PosterCard(
    item: MediaItem,
    onSelect: (MediaItem) -> Unit,
    liftOnFocus: Boolean,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 196.dp,
    height: androidx.compose.ui.unit.Dp = 116.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    Card(
        onClick = { onSelect(item) },
        modifier = modifier.width(width).height(height).zIndex(if (focused) 1f else 0f),
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
        scale = CardDefaults.scale(focusedScale = if (liftOnFocus) 1.025f else 1f, pressedScale = .99f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(2.dp, FocusSurface), shape = shape)),
        interactionSource = interaction,
    ) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(ControlSurface, Background)))) {
            val artwork = item.backdropUrl?.replace("/w1280/", "/w780/") ?: item.posterUrl
            if (artwork != null) AsyncImage(
                artworkModel(artwork), item.title, Modifier.fillMaxSize(),
                error = painterResource(R.drawable.reelora_mark), contentScale = ContentScale.Crop,
            )
            else Image(painterResource(R.drawable.reelora_mark), null, Modifier.size(52.dp).align(Alignment.Center))
            cardReleaseLabel(item.releaseDate)?.let { InfoBadge(it, WarmAccent, Modifier.align(Alignment.TopStart).padding(7.dp)) }
            Box(Modifier.fillMaxWidth().height(58.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Background.copy(alpha = .94f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(item.year, primaryGenre(item)?.let { tr(it) }, tr(mediaRating(item))).joinToString(" · "),
                    color = Color.White.copy(alpha = .75f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
    val isForeground = LocalForeground.current
    val requester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val actorRowRequester = remember { FocusRequester() }
    val similarRowRequester = remember { FocusRequester() }
    var details by remember(mediaKey(item)) { mutableStateOf<MediaDetails?>(null) }
    var selectedActor by remember(mediaKey(item)) { mutableStateOf<CastMember?>(null) }
    var actorTitles by remember(mediaKey(item), selectedActor?.id) { mutableStateOf<List<MediaItem>?>(null) }
    var actorLoading by remember(mediaKey(item)) { mutableStateOf(false) }
    val context = LocalContext.current
    val artwork = remember(context, item.backdropUrl) { item.backdropUrl?.let { ImageRequest.Builder(context).data(it).crossfade(120).build() } }
    val moreLike = details?.similar?.ifEmpty { similar } ?: similar
    val restoreTop: () -> Unit = { if (listState.firstVisibleItemIndex != 0) scope.launch { listState.animateScrollToItem(0) } }
    TvDialog(onDismiss, Modifier.fillMaxWidth(.9f).fillMaxHeight(.92f), ambient = false) { close ->
        val ready = LocalDialogReady.current
        LaunchedEffect(ready, mediaKey(item)) {
            if (ready) {
                listState.scrollToItem(0)
                withFrameNanos { }
                requester.requestFocus()
            }
        }
         Box(Modifier.fillMaxSize()) {
          Box(
              Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(Color(0xFF11111C)),
          ) {
           artwork?.let {
              AsyncImage(
                  model = it,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
              )
           }
           Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
               listOf(Background.copy(alpha = .76f), Background.copy(alpha = .94f)),
           )))
          }
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { Column {
                Row(Modifier.fillMaxWidth().height(48.dp).focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            .background(Brush.linearGradient(listOf(ControlSurface, Background)))
                    ) {
                        if (item.posterUrl != null) AsyncImage(artworkModel(item.posterUrl), item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Image(painterResource(R.drawable.reelora_mark), null, Modifier.size(82.dp).align(Alignment.Center))
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    val metadata = listOf(
                        item.year,
                        item.mediaType.uppercase(),
                        details?.runtime.orEmpty(),
                        tr(mediaRating(item)),
                    ).filter { it.isNotBlank() }.joinToString("  ·  ")
                    Text(metadata, color = WarmAccent, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(details?.genres.orEmpty(), modifier = Modifier.height(18.dp), color = Color.White.copy(alpha = .58f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.height(30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val release = releaseLabel(item.releaseDate)
                        InfoBadge(release, if (release.startsWith("✓") || release.startsWith("●")) Color(0xFF66D69A) else WarmAccent)
                        details?.availability?.let { AvailabilityBadge(it) }
                    }
                    Text(details?.availability?.let { tr("Availability by JustWatch · ${it.region}") }.orEmpty(), modifier = Modifier.height(14.dp), color = Color.White.copy(alpha = .38f), fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(item.overview, color = Color.White.copy(alpha = .78f), fontSize = 16.sp, lineHeight = 22.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    }
                }
            } }
            item { Column {
                Text(tr("Cast"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.height(132.dp)) { CastRow(
                    details?.cast,
                    selectedActor,
                    onDown = {
                        val hasActorTitles = selectedActor != null && actorTitles?.isNotEmpty() == true
                        val target = if (hasActorTitles) actorRowRequester else similarRowRequester
                        if (hasActorTitles || moreLike.isNotEmpty()) scope.launch {
                            listState.scrollToItem(if (selectedActor != null && !hasActorTitles) 3 else 2)
                            withFrameNanos { }
                            target.requestFocus()
                        }
                    },
                    onSelect = { selectedActor = if (selectedActor?.id == it.id) null else it },
                ) }
            } }
            selectedActor?.let { actor ->
                val titles = actorTitles
                item(key = "actor-credits") { Column {
                    Text(tr("${actor.name} · Movies & TV${if (actorLoading && titles != null) " · Updating…" else ""}"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    when {
                        titles == null -> LoadingPosterRow()
                        titles.isEmpty() -> Text(tr("No other titles found"), color = Color.White.copy(alpha = .55f), fontSize = 14.sp)
                        else -> PosterStrip(titles, onSelect, firstModifier = Modifier.focusRequester(actorRowRequester))
                    }
                } }
            }
            item { Column {
                Text(tr("More like this"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                PosterStrip(moreLike, onSelect, firstModifier = Modifier.focusRequester(similarRowRequester))
            } }
          }
         }
    }
    LaunchedEffect(mediaKey(item), isForeground) {
        if (isForeground && details == null) details = CatalogRepository.details(item)
    }
    LaunchedEffect(selectedActor?.id, isForeground) {
        if (!isForeground || actorTitles != null) return@LaunchedEffect
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
        providers.isNotEmpty() -> Accent
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
            tr(when {
                streaming -> "STREAMING"
                providers.isNotEmpty() -> "RENT / BUY"
                else -> "NO STREAMING LISTED"
            }),
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
    onDown: () -> Unit,
    onSelect: (CastMember) -> Unit,
) {
    if (cast == null) {
        LoadingCastRow()
        return
    }
    if (cast.isEmpty()) {
        Text(tr("Cast information unavailable"), color = Color.White.copy(alpha = .55f), fontSize = 14.sp, modifier = Modifier.height(98.dp))
        return
    }
    LazyRow(
        contentPadding = PaddingValues(start = 6.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.focusGroup(),
    ) {
        items(cast, key = { "${it.id}-${it.name}" }, contentType = { "cast" }) { person ->
            CastCard(person, person.id == selected?.id, onDown, onSelect)
        }
    }
}

@Composable
private fun CastCard(person: CastMember, selected: Boolean, onDown: () -> Unit, onSelect: (CastMember) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Card(
        onClick = { onSelect(person) },
        modifier = Modifier.width(96.dp).zIndex(if (focused) 1f else 0f)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                    onDown()
                    true
                } else false
            },
        colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
        scale = CardDefaults.scale(focusedScale = 1.04f, pressedScale = .98f),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None, pressedBorder = Border.None),
        interactionSource = interaction,
    ) {
        Column(
            Modifier.padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(58.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF4B2B86), Color(0xFF211B3A))))
                    .border(if (focused) 3.dp else if (selected) 2.dp else 0.dp, if (focused) Accent else WarmAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (person.profileUrl != null) AsyncImage(artworkModel(person.profileUrl), person.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(person.name.take(1), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            Text(person.name, color = if (selected) WarmAccent else Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (person.character.isNotBlank()) Text(person.character, color = SecondaryText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}
