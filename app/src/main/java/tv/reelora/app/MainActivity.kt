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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow

private val Background = Color(0xFF07070F)
private val Surface = Color(0xFF151522)
private val Violet = Color(0xFFA978FF)
private val Coral = Color(0xFFFF8064)

private data class TheaterFeature(val item: MediaItem, val trailer: Trailer)
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
        var appManagerOpen by remember { mutableStateOf(false) }
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
        var appOrder by remember {
            mutableStateOf(preferences.getString("appOrder", "").orEmpty().split(',').filter(String::isNotBlank))
        }
        var hiddenApps by remember {
            mutableStateOf(preferences.getStringSet("hiddenApps", emptySet()).orEmpty().toSet())
        }
        val orderedApps = remember(apps, appOrder) { orderLauncherApps(apps, appOrder) }
        val visibleApps = remember(orderedApps, hiddenApps) {
            orderedApps.filterNot { launcherAppKey(it) in hiddenApps }
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
            result = CatalogRepository.load()
        }
        LaunchedEffect(isForeground) {
            if (isForeground) apps = withContext(Dispatchers.IO) { installedTvApps(context) }
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
                    compactApps = compactApps,
                    onLaunch = { app ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_MAIN).setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }.onFailure {
                            android.widget.Toast.makeText(context, "Unable to open ${app.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSearch = { searching = true },
                    onSettings = { settingsOpen = true },
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
                    onManageApps = {
                        settingsOpen = false
                        appManagerOpen = true
                    },
                    onSystemSettings = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) },
                    onHomeSettings = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                            .getOrElse { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    },
                    onDismiss = { settingsOpen = false },
                )
                if (appManagerOpen) AppManagerDialog(
                    apps = orderedApps,
                    hiddenApps = hiddenApps,
                    onMove = { app, offset ->
                        appOrder = moveAppKey(orderedApps.map(::launcherAppKey), launcherAppKey(app), offset)
                        preferences.edit().putString("appOrder", appOrder.joinToString(",")).apply()
                    },
                    onToggleHidden = { app ->
                        val key = launcherAppKey(app)
                        hiddenApps = if (key in hiddenApps) hiddenApps - key else hiddenApps + key
                        preferences.edit().putStringSet("hiddenApps", hiddenApps).apply()
                    },
                    onAppInfo = { app ->
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.component.packageName}")),
                        )
                    },
                    onDismiss = { appManagerOpen = false },
                )
            }
        }

        LaunchedEffect(result, theater, theaterEnabled, idleMinutes, isForeground) {
            val catalog = result ?: return@LaunchedEffect
            if (theater != null || !theaterEnabled || !isForeground) return@LaunchedEffect
            while (withTimeoutOrNull(idleMinutes * 60_000L) { inputEvents.receive() } != null) Unit
            findTheaterFeature(
                launcherMovieSections(catalog).flatMap { it.items },
                recentTheater,
            )?.let {
                theater = it
                theaterReturn = null
                selected = null
                searching = false
                settingsOpen = false
                appManagerOpen = false
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
        setContentView(FrameLayout(this).apply {
            addView(player, FrameLayout.LayoutParams(-1, -1))
            addView(releaseBadge, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
                topMargin = (96 * density).toInt()
                marginEnd = (32 * density).toInt()
            })
        })
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
        releaseBadge.visibility = if (releaseBadge.text.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
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
    val motion = rememberInfiniteTransition(label = "loading")
    val pulse by motion.animateFloat(
        initialValue = .94f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1_100), RepeatMode.Reverse),
        label = "loading pulse",
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.reelora_mark),
            "Reelora TV",
            Modifier.size(104.dp).graphicsLayer {
                scaleX = pulse
                scaleY = pulse
                alpha = .72f + (pulse - .94f) * 2.5f
            },
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
private fun artworkModel(url: String, fade: Boolean = false): ImageRequest {
    val context = LocalContext.current
    return remember(url, fade) {
        ImageRequest.Builder(context).data(url).apply { if (fade) crossfade(180) }.build()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Home(
    catalog: CatalogResult,
    apps: List<LauncherApp>,
    compactApps: Boolean,
    onLaunch: (LauncherApp) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onSelect: (MediaItem) -> Unit,
) {
    val listState = rememberLazyListState()
    val heroFocus = remember { FocusRequester() }
    val appFocus = remember { FocusRequester() }
    val sections = remember(catalog) { launcherMovieSections(catalog) }
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
    LaunchedEffect(apps) {
        listState.scrollToItem(0)
        delay(160)
        if (apps.isEmpty()) heroFocus.requestFocus() else appFocus.requestFocus()
    }
    LaunchedEffect(hero) {
        delay(10_000)
        nextDiscoveryItem(featured.items, recent)?.let {
            hero = it
            recent = (recent + mediaKey(it)).takeLast(10)
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides stableBringIntoView) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 48.dp),
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
            item(key = "home") {
                Column {
                    Hero(
                        hero,
                        onSelect,
                        Modifier
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                    appFocus.requestFocus()
                                    true
                                } else false
                            }
                            .focusRequester(heroFocus),
                    )
                    Spacer(Modifier.height(4.dp))
                    AppDock(apps, heroFocus, appFocus, compactApps, onLaunch)
                }
            }
            sections.forEach { section -> item(key = section.title) { MediaRow(section, onSelect) } }
            item {
                Text(
                    "Movie data and images by TMDB · Availability by JustWatch",
                    color = Color.White.copy(alpha = .38f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun AppDock(
    apps: List<LauncherApp>,
    upFocus: FocusRequester,
    firstFocus: FocusRequester,
    compact: Boolean,
    onLaunch: (LauncherApp) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
        modifier = Modifier.height(if (compact) 110.dp else 120.dp).focusGroup(),
    ) {
        itemsIndexed(apps, key = { _, app -> app.component.flattenToShortString() }) { index, app ->
            AppCard(
                app,
                compact,
                onLaunch,
                if (index == 0) Modifier.focusRequester(firstFocus).focusProperties { up = upFocus } else Modifier,
            )
        }
    }
}

@Composable
private fun AppCard(
    app: LauncherApp,
    compact: Boolean,
    onLaunch: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val tileScale by animateFloatAsState(if (focused) 1.04f else 1f, tween(110), label = "app tile focus")
    val tileWidth = if (compact) 116.dp else 136.dp
    val tileHeight = if (compact) 68.dp else 78.dp
    val tileHue = remember(app.component.packageName) { (app.component.packageName.hashCode() and 0x7fffffff) % 360f }
    val tileBackground = remember(tileHue) {
        Brush.linearGradient(
            listOf(Color.hsv(tileHue, .55f, .38f), Color.hsv((tileHue + 32f) % 360f, .62f, .22f)),
        )
    }
    Column(
        modifier.width(tileWidth)
            .graphicsLayer { scaleX = tileScale; scaleY = tileScale }
            .onFocusChanged { focused = it.isFocused }
            .clickable(role = Role.Button) { onLaunch(app) },
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
            if (focused) Box(Modifier.fillMaxSize().border(2.dp, Color.White.copy(alpha = .9f), RoundedCornerShape(if (compact) 16.dp else 19.dp)))
        }
        Spacer(Modifier.height(8.dp))
        Text(app.name, color = Color.White.copy(alpha = if (focused) 1f else .60f), fontSize = if (compact) 11.sp else 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AppManagerDialog(
    apps: List<LauncherApp>,
    hiddenApps: Set<String>,
    onMove: (LauncherApp, Int) -> Unit,
    onToggleHidden: (LauncherApp) -> Unit,
    onAppInfo: (LauncherApp) -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            delay(250)
            first.requestFocus()
        }
        Box(Modifier.fillMaxSize().background(Background.copy(alpha = .9f)), contentAlignment = Alignment.Center) {
            Column(
                Modifier.width(920.dp).height(620.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF242433), Color(0xFF11111A))))
                    .border(1.dp, Color.White.copy(alpha = .16f), RoundedCornerShape(28.dp)).padding(28.dp),
            ) {
                Text("Organize apps", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Move the Home shelf, hide clutter, or open Android app settings", color = Color.White.copy(alpha = .55f), fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(apps, key = { _, app -> launcherAppKey(app) }) { index, app ->
                        val hidden = launcherAppKey(app) in hiddenApps
                        Row(
                            Modifier.fillMaxWidth().animateItem().clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = if (hidden) .035f else .075f)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AsyncImage(app.icon, app.name, Modifier.size(48.dp), contentScale = ContentScale.Fit)
                            Column(Modifier.weight(1f)) {
                                Text(app.name, color = Color.White.copy(alpha = if (hidden) .48f else .92f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (hidden) "Hidden from Home" else "Position ${index + 1}", color = Color.White.copy(alpha = .42f), fontSize = 11.sp)
                            }
                            ActionButton(
                                "Left",
                                modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                                glyph = "‹",
                            ) { onMove(app, -1) }
                            ActionButton("Right", glyph = "›") { onMove(app, 1) }
                            ActionButton(
                                if (hidden) "Show" else "Hide",
                                glyph = if (hidden) "+" else "−",
                            ) { onToggleHidden(app) }
                            ActionButton("Info", glyph = "ⓘ") { onAppInfo(app) }
                        }
                    }
                }
                if (apps.isEmpty()) Text("No launchable apps found", color = Color.White.copy(alpha = .55f), modifier = Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ActionButton("Done", modifier = if (apps.isEmpty()) Modifier.focusRequester(first) else Modifier, glyph = "✓", onClick = onDismiss)
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
    hiddenAppCount: Int,
    onSearch: () -> Unit,
    onTheaterEnabled: (Boolean) -> Unit,
    onIdleMinutes: (Int) -> Unit,
    onCompactApps: (Boolean) -> Unit,
    onManageApps: () -> Unit,
    onSystemSettings: () -> Unit,
    onHomeSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            delay(350)
            first.requestFocus()
        }
        Box(Modifier.fillMaxSize().background(Background.copy(alpha = .88f)), contentAlignment = Alignment.Center) {
            Column(
                Modifier.width(840.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF242433), Color(0xFF12121B))))
                    .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(28.dp)).padding(28.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Settings", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("A quiet home for apps and discovery", color = Color.White.copy(alpha = .52f), fontSize = 13.sp)
                    }
                    ActionButton("Done", glyph = "✓", onClick = onDismiss)
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = .055f)).padding(18.dp),
                    ) {
                        Text("APP SHELF", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text("Find, arrange and size your Home apps", color = Color.White.copy(alpha = .48f), fontSize = 12.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionButton("Search", modifier = Modifier.focusRequester(first), glyph = "⌕", onClick = onSearch)
                            ActionButton("Organize${if (hiddenAppCount > 0) " · $hiddenAppCount" else ""}", glyph = "↔", onClick = onManageApps)
                        }
                        Spacer(Modifier.height(10.dp))
                        ActionButton(if (compactApps) "Compact layout" else "Comfortable layout", glyph = "▤") {
                            onCompactApps(!compactApps)
                        }
                    }
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = .055f)).padding(18.dp),
                    ) {
                        Text("THEATER", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text("Play trailers when the launcher rests", color = Color.White.copy(alpha = .48f), fontSize = 12.sp)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionButton(if (theaterEnabled) "On" else "Off", glyph = if (theaterEnabled) "●" else "○") {
                                onTheaterEnabled(!theaterEnabled)
                            }
                            ActionButton("After $idleMinutes min", glyph = "◷") {
                                onIdleMinutes(nextTheaterIdleMinutes(idleMinutes))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Pauses while another app is open", color = Color.White.copy(alpha = .38f), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = .045f)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("SYSTEM", color = Coral, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text("Home and Android controls", color = Color.White.copy(alpha = .48f), fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("Default home", glyph = "⌂", onClick = onHomeSettings)
                        ActionButton("Android", glyph = "⚙", onClick = onSystemSettings)
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
    var entered by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<MediaItem>()) }
    var loading by remember { mutableStateOf(false) }
    var inputFocused by remember { mutableStateOf(false) }
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
        entered = true
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Background.copy(alpha = .94f)), contentAlignment = Alignment.Center) {
          AmbientBackdrop()
          AnimatedVisibility(
            visible = entered,
            enter = fadeIn(tween(180)) + scaleIn(
                initialScale = .965f,
                animationSpec = spring(dampingRatio = .84f, stiffness = Spring.StiffnessMediumLow),
            ),
            label = "search dialog",
          ) {
           Column(
             Modifier
                .fillMaxWidth(.92f)
                .fillMaxHeight(.88f)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xF21B1B2B), Color(0xF20D0D16))))
                .border(1.dp, Color.White.copy(alpha = .16f), RoundedCornerShape(26.dp))
                .padding(30.dp),
           ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Search", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Movies, series and animation", color = Color.White.copy(alpha = .55f), fontSize = 14.sp)
                }
                ActionButton("Close", glyph = "×", onClick = onDismiss)
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 21.sp),
                    cursorBrush = SolidColor(Coral),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(inputRequester)
                        .onFocusChanged { inputFocused = it.isFocused },
                    decorationBox = { field ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(62.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = .07f))
                                .border(
                                    if (inputFocused) 2.dp else 1.dp,
                                    if (inputFocused) Violet else Color.White.copy(alpha = .16f),
                                    RoundedCornerShape(14.dp),
                                )
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (query.isEmpty()) Text("Type a title…", color = Color.White.copy(alpha = .42f), fontSize = 21.sp)
                            field()
                        }
                    },
                )
                if (voiceAvailable) ActionButton("Voice", glyph = "✦") { voice.launch(voiceIntent) }
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
                LazyRow(
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.focusGroup(),
                ) {
                    items(shown, key = { "${it.mediaType}-${it.id}" }) { item ->
                        PosterCard(item, onSelect = {
                            keyboard?.hide()
                            onSelect(item)
                        })
                    }
                }
            }
           }
          }
        }
    }
}

@Composable
private fun Hero(item: MediaItem, onSelect: (MediaItem) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxWidth()
            .height(360.dp)
            .onFocusChanged { focused = it.isFocused }
            .background(Brush.linearGradient(listOf(Color(0xFF2D1760), Color(0xFF10101E))))
            .clickable(role = Role.Button) { onSelect(item) }
    ) {
        item.backdropUrl?.let {
            AsyncImage(
                model = artworkModel(it, fade = true),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
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
        Column(
            Modifier.fillMaxHeight().width(620.dp).padding(start = 58.dp, top = 42.dp, end = 24.dp, bottom = 28.dp),
        ) {
            AnimatedContent(
                targetState = item,
                transitionSpec = {
                    (slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(320)) { -it / 5 } + fadeOut(tween(220)))
                },
                contentKey = { it.id },
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f),
                label = "featured details",
            ) { featured ->
                Column {
                    if (releaseLabel(featured.releaseDate).startsWith("◷")) InfoBadge(releaseLabel(featured.releaseDate), Coral)
                    Spacer(Modifier.height(6.dp))
                    Text(featured.title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("${featured.year}   ·   ★ ${"%.1f".format(featured.score)}", color = Color.White.copy(alpha = .82f), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(featured.overview, color = Color.White.copy(alpha = .68f), fontSize = 14.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
private fun MediaRow(section: CatalogSection, onSelect: (MediaItem) -> Unit) {
    Column {
        Text(section.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 48.dp))
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(132.dp).focusGroup(),
        ) {
            items(section.items, key = { "${section.title}-${it.id}" }) { item ->
                PosterCard(item, onSelect)
            }
        }
    }
}

@Composable
private fun PosterCard(item: MediaItem, onSelect: (MediaItem) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(if (focused) 1.025f else 1f, tween(100), label = "movie card focus")
    Box(
        modifier
            .width(196.dp)
            .height(116.dp)
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF342065), Color(0xFF19192A))))
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) { onSelect(item) }
    ) {
        val artwork = item.backdropUrl ?: item.posterUrl
        if (artwork != null) {
            AsyncImage(artworkModel(artwork), item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
    glyph: String? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color.White else Color.White.copy(alpha = .085f))
            .border(1.dp, if (focused) Color.White else Color.White.copy(alpha = .09f), RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            glyph?.let { Text(it, color = if (focused) Coral else Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            Text(text, color = if (focused) Background else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
    var entered by remember(item.id) { mutableStateOf(false) }
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
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
      Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
          visible = entered,
          enter = fadeIn(tween(140)),
          modifier = Modifier.fillMaxWidth(.9f).fillMaxHeight(.92f),
          label = "details dialog",
        ) {
         Box(Modifier.fillMaxSize()) {
          Box(
              Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(Color(0xFF11111C)),
          ) {
           artwork?.let {
              AsyncImage(
                  model = artworkModel(it, fade = true),
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
                        glyph = "‹",
                        onClick = onDismiss,
                    )
                    details?.trailer?.let { trailer ->
                        ActionButton("Play trailer", onFocused = restoreTop, glyph = "▶") { onPlayTrailer(trailer) }
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
                        else -> LazyRow(
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.focusGroup(),
                        ) {
                            itemsIndexed(titles, key = { _, title -> "actor-${actor.id}-${title.mediaType}-${title.id}" }) { index, title ->
                                PosterCard(
                                    title,
                                    onSelect = onSelect,
                                    modifier = if (index == 0) Modifier.focusRequester(actorRowRequester) else Modifier,
                                )
                            }
                        }
                    }
                } }
            }
            item { Column {
                Text("More like this", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.focusGroup(),
                ) {
                    itemsIndexed(moreLike, key = { _, suggestion -> suggestion.id }) { index, suggestion ->
                        PosterCard(
                            suggestion,
                            onSelect,
                            modifier = if (index == 0) Modifier.focusRequester(similarRowRequester) else Modifier,
                        )
                    }
                }
            } }
          }
          Box(
              Modifier.fillMaxSize().border(1.dp, Color.White.copy(alpha = .16f), RoundedCornerShape(26.dp)),
          )
         }
        }
      }
    }
    LaunchedEffect(item.id) {
        entered = true
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
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.focusGroup(),
    ) {
        items(cast, key = { "${it.id}-${it.name}" }) { person -> CastCard(person, person.id == selected?.id, downRequester, onSelect) }
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
