package tv.reelora.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Background = Color(0xFF07070F)
private val Surface = Color(0xFF151522)
private val Violet = Color(0xFFA978FF)
private val Coral = Color(0xFFFF8064)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReeloraApp() }
    }
}

@Composable
private fun ReeloraApp() {
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
        var selected by remember { mutableStateOf<MediaItem?>(null) }
        var searching by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { result = CatalogRepository.load() }

        Box(Modifier.fillMaxSize().background(Background)) {
            val catalog = result
            if (catalog == null) {
                Loading()
            } else {
                Home(catalog, onSearch = { searching = true }, onSelect = { selected = it })
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
                    )
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.reelora_mark), "Reelora TV", Modifier.size(88.dp))
        Spacer(Modifier.height(16.dp))
        Text("Finding something great…", color = Color.White.copy(alpha = .72f), fontSize = 18.sp)
    }
}

@Composable
private fun Home(catalog: CatalogResult, onSearch: () -> Unit, onSelect: (MediaItem) -> Unit) {
    var selectedCategory by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val restoreTop: () -> Unit = {
        scope.launch {
            delay(140)
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(selectedCategory) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize()) {
        Header(
            categories = catalog.sections.map { it.title },
            selectedCategory = selectedCategory,
            isDemo = catalog.isDemo,
            onCategory = { selectedCategory = it },
            onFocus = restoreTop,
            onSearch = onSearch,
        )
        CategoryPage(catalog.sections[selectedCategory], listState, onSelect)
    }
}

@Composable
private fun CategoryPage(section: CatalogSection, listState: LazyListState, onSelect: (MediaItem) -> Unit) {
    var hero by remember(section) { mutableStateOf(section.items.first()) }
    var focusedItem by remember(section) { mutableStateOf(hero) }

    LaunchedEffect(focusedItem) {
        delay(140)
        hero = focusedItem
    }
    LaunchedEffect(section, hero) {
        delay(10_000)
        val next = (section.items.indexOfFirst { it.id == hero.id } + 1).mod(section.items.size)
        hero = section.items[next]
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item { Hero(hero, onSelect) }
        item { MediaRow(section, onFocused = { focusedItem = it }, onSelect = onSelect) }
        item {
            Text(
                "Data and images by TMDB. This product uses the TMDB API but is not endorsed or certified by TMDB.",
                color = Color.White.copy(alpha = .42f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 48.dp),
            )
        }
    }
}

@Composable
private fun Header(
    categories: List<String>,
    selectedCategory: Int,
    isDemo: Boolean,
    onCategory: (Int) -> Unit,
    onFocus: () -> Unit,
    onSearch: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(140)
        firstFocus.requestFocus()
    }
    Column(Modifier.fillMaxWidth().background(Surface)) {
        Row(
            Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandSearch(Modifier.focusRequester(firstFocus), onFocus, onSearch)
            Spacer(Modifier.width(24.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).focusGroup()) {
                itemsIndexed(categories) { index, title ->
                    CategoryChip(title, selected = index == selectedCategory, onFocus = onFocus) { onCategory(index) }
                }
            }
            if (isDemo) {
                Text("DEMO", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Violet.copy(alpha = .22f)))
    }
}

@Composable
private fun BrandSearch(modifier: Modifier = Modifier, onFocus: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.035f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "search focus")
    Row(
        modifier
            .width(210.dp)
            .height(62.dp)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (focused) Color.White else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.reelora_mark), "Search Reelora", Modifier.size(46.dp))
        Spacer(Modifier.width(10.dp))
        AnimatedContent(
            targetState = focused,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
            label = "brand search",
        ) { isFocused ->
            Text(
                if (isFocused) "SEARCH" else "REELORA",
                color = if (isFocused) Background else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Composable
private fun CategoryChip(title: String, selected: Boolean, onFocus: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.045f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "category focus")
    val background = when {
        focused -> Color.White
        selected -> Violet.copy(alpha = .32f)
        else -> Color.White.copy(alpha = .08f)
    }
    Box(
        Modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .then(if (selected && !focused) Modifier.border(1.dp, Violet, RoundedCornerShape(20.dp)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 9.dp)
    ) {
        Text(title, color = if (focused) Background else Color.White.copy(alpha = .78f), fontSize = 14.sp)
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
        inputRequester.requestFocus()
        delay(120)
        keyboard?.show()
    }
    LaunchedEffect(query) {
        val term = query.trim()
        if (term.length < 2) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        delay(350)
        loading = true
        results = CatalogRepository.search(term)
        loading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(.92f)
                .fillMaxHeight(.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF10101B))
                .border(1.dp, Color.White.copy(alpha = .14f), RoundedCornerShape(24.dp))
                .padding(30.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Search", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Movies, series and animation", color = Color.White.copy(alpha = .55f), fontSize = 14.sp)
                }
                ActionButton("Close", onClick = onDismiss)
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
            LazyRow(
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.focusGroup(),
            ) {
                items(shown, key = { "${it.mediaType}-${it.id}" }) { item ->
                    PosterCard(item, onFocused = {}, onSelect = {
                        keyboard?.hide()
                        onSelect(item)
                    })
                }
            }
        }
    }
}

@Composable
private fun Hero(item: MediaItem, onSelect: (MediaItem) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(310.dp)
            .padding(horizontal = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2D1760), Color(0xFF10101E))))
            .border(if (focused) 3.dp else 0.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button) { onSelect(item) }
    ) {
        AnimatedContent(
            targetState = item,
            transitionSpec = {
                (slideInHorizontally(tween(360)) { it / 4 } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(360)) { -it / 4 } + fadeOut(tween(240)))
            },
            contentKey = { it.id },
            label = "featured artwork",
        ) { featured ->
            featured.backdropUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Background.copy(alpha = .96f),
                    .58f to Background.copy(alpha = .50f),
                    1f to Color.Transparent,
                )
            )
        )
        Column(
            Modifier.fillMaxHeight().width(560.dp).padding(34.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("FEATURED · ${featured.mediaType.uppercase()}", color = Coral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (releaseLabel(featured.releaseDate).startsWith("◷")) InfoBadge(releaseLabel(featured.releaseDate), Coral)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(featured.title, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("${featured.year}   ★ ${"%.1f".format(featured.score)}   ${featured.voteCount} ratings", color = Color.White.copy(alpha = .78f), fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(featured.overview, color = Color.White.copy(alpha = .70f), fontSize = 16.sp, lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (focused) Color.White else Violet)
                    .padding(horizontal = 22.dp, vertical = 11.dp)
            ) {
                Text("View details", color = if (focused) Background else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun MediaRow(section: CatalogSection, onFocused: (MediaItem) -> Unit, onSelect: (MediaItem) -> Unit) {
    Column {
        Text(section.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 48.dp))
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.focusGroup(),
        ) {
            items(section.items, key = { "${section.title}-${it.id}" }) { item ->
                PosterCard(item, onFocused, onSelect)
            }
        }
    }
}

@Composable
private fun PosterCard(item: MediaItem, onFocused: (MediaItem) -> Unit, onSelect: (MediaItem) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.045f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "poster focus")
    Box(
        modifier
            .width(176.dp)
            .height(248.dp)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (focused) 14.dp.toPx() else 0f
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused(item)
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF342065), Color(0xFF19192A))))
            .border(if (focused) 3.dp else 0.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button) { onSelect(item) }
    ) {
        if (item.posterUrl != null) {
            AsyncImage(item.posterUrl, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Image(painterResource(R.drawable.reelora_mark), null, Modifier.size(88.dp).align(Alignment.Center))
        }
        cardReleaseLabel(item.releaseDate)?.let {
            InfoBadge(it, Coral, Modifier.align(Alignment.TopStart).padding(9.dp))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Background.copy(alpha = .95f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(13.dp)) {
            Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${item.year}  ·  ★ ${"%.1f".format(item.score)}", color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
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
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (focused) 1.065f else 1f,
        spring(dampingRatio = .78f, stiffness = Spring.StiffnessMediumLow),
        label = "button focus",
    )
    Box(
        modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (focused) 16.dp.toPx() else 0f
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color.White else Violet)
            .border(if (focused) 2.dp else 0.dp, if (focused) Coral else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp)
    ) {
        Text(text, color = if (focused) Background else Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun DetailsDialog(
    item: MediaItem,
    similar: List<MediaItem>,
    onDismiss: () -> Unit,
    onSelect: (MediaItem) -> Unit,
) {
    val requester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val actorRowRequester = remember { FocusRequester() }
    var details by remember(item.id) { mutableStateOf<MediaDetails?>(null) }
    var selectedActor by remember(item.id) { mutableStateOf<CastMember?>(null) }
    var actorTitles by remember(item.id) { mutableStateOf<List<MediaItem>?>(null) }
    var actorLoading by remember(item.id) { mutableStateOf(false) }
    val artwork = details?.backdrops?.firstOrNull { it != item.backdropUrl } ?: details?.let { item.backdropUrl }
    var artworkReady by remember(artwork) { mutableStateOf(false) }
    val artworkAlpha by animateFloatAsState(if (artworkReady) .42f else 0f, tween(500), label = "details artwork")
    val restoreTop: () -> Unit = { scope.launch { listState.animateScrollToItem(0) } }
    val revealRow: (Int) -> Unit = { index -> scope.launch { delay(120); listState.animateScrollToItem(index) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
      Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        artwork?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { artworkReady = true },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = artworkAlpha
                    scaleX = 1.04f
                    scaleY = 1.04f
                },
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Background.copy(alpha = .35f),
                    .55f to Background.copy(alpha = .72f),
                    1f to Background,
                )
            )
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth(.9f)
                .fillMaxHeight(.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF12121E).copy(alpha = .82f))
                .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(24.dp)),
            contentPadding = PaddingValues(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 190.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { Column {
                Row(Modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton("Back", Modifier.focusRequester(requester), restoreTop, onDismiss)
                    details?.trailer?.let { trailer ->
                        ActionButton("Play trailer", onFocused = restoreTop) { openTrailer(context, trailer) }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row {
                    Box(
                        Modifier.width(136.dp).height(190.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF342065), Color(0xFF19192A))))
                    ) {
                        if (item.posterUrl != null) AsyncImage(item.posterUrl, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
                    actorRowRequester.takeIf { actorTitles?.isNotEmpty() == true },
                    onFocus = { revealRow(1) },
                    onSelect = { selectedActor = it },
                )
            } }
            selectedActor?.let { actor ->
                val titles = actorTitles
                item(key = "actor-credits") { Column {
                    Text("${actor.name} · Movies & TV${if (actorLoading && titles != null) " · Updating…" else ""}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    when {
                        titles == null -> Text("Finding titles…", color = Color.White.copy(alpha = .55f), fontSize = 14.sp, modifier = Modifier.height(248.dp))
                        titles.isEmpty() -> Text("No other titles found", color = Color.White.copy(alpha = .55f), fontSize = 14.sp)
                        else -> LazyRow(
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.focusGroup(),
                        ) {
                            itemsIndexed(titles, key = { _, title -> "actor-${actor.id}-${title.mediaType}-${title.id}" }) { index, title ->
                                PosterCard(
                                    title,
                                    onFocused = { revealRow(2) },
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
                    items(details?.similar?.ifEmpty { similar } ?: similar, key = { it.id }) { suggestion ->
                        PosterCard(suggestion, onFocused = { revealRow(if (selectedActor == null) 2 else 3) }, onSelect = onSelect)
                    }
                }
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
            provider.logoUrl?.let { AsyncImage(it, provider.name, Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))) }
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

private fun openTrailer(context: Context, trailer: Trailer) {
    val url = Uri.parse("https://www.youtube.com/watch?v=${trailer.key}")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url).setPackage("com.amazon.firetv.youtube"))
    }.getOrElse {
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    }
}

@Composable
private fun CastRow(
    cast: List<CastMember>?,
    selected: CastMember?,
    downRequester: FocusRequester?,
    onFocus: () -> Unit,
    onSelect: (CastMember) -> Unit,
) {
    if (cast == null) {
        Text("Loading cast…", color = Color.White.copy(alpha = .55f), fontSize = 14.sp, modifier = Modifier.height(98.dp))
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
        items(cast, key = { "${it.id}-${it.name}" }) { person -> CastCard(person, person.id == selected?.id, downRequester, onFocus, onSelect) }
    }
}

@Composable
private fun CastCard(person: CastMember, selected: Boolean, downRequester: FocusRequester?, onFocus: () -> Unit, onSelect: (CastMember) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "cast focus")
    Column(
        Modifier
            .width(96.dp)
            .focusProperties { downRequester?.let { down = it } }
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
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
                AsyncImage(person.profileUrl, person.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
