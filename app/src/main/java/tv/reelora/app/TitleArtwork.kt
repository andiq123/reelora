package tv.reelora.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.withTimeoutOrNull

/** Opacity-weighted mean luminance, 0..1, of ARGB pixels; null when fully transparent. */
internal fun logoLuminance(pixels: List<Int>): Float? {
    var weight = 0f
    var sum = 0f
    for (pixel in pixels) {
        val alpha = (pixel ushr 24) / 255f
        val luminance = (.2126f * ((pixel shr 16) and 255) + .7152f * ((pixel shr 8) and 255) + .0722f * (pixel and 255)) / 255f
        weight += alpha
        sum += luminance * alpha
    }
    return if (weight <= 0f) null else sum / weight
}

private const val DarkLogoLuminance = .3f
private const val LogoSamples = 24
private const val LogoLookupMillis = 1_500L

/** A studio title logo where TMDB has one, falling back to the plain title in the same space. */
@Composable
internal fun TitleArtwork(item: MediaItem, style: TextStyle, logoWidth: Dp, logoHeight: Dp, modifier: Modifier = Modifier) {
    val key = mediaKey(item)
    val context = LocalContext.current
    // Null while unresolved, empty when the title has no usable logo.
    var logo by remember(key) { mutableStateOf(CatalogRepository.cachedTitleLogo(item)) }
    var failed by remember(key) { mutableStateOf(false) }
    var loaded by remember(key) { mutableStateOf(false) }
    var whiten by remember(key) { mutableStateOf(false) }
    val online = LocalInternet.current
    LaunchedEffect(key, online) {
        if (logo == null) {
            logo = if (online) withTimeoutOrNull(LogoLookupMillis) { CatalogRepository.titleLogo(item) }.orEmpty() else ""
        }
    }
    val reveal by animateFloatAsState(if (loaded) 1f else 0f, tween(TvMotion.EnterMillis), label = "Title logo")
    val url = logo
    Box(modifier.heightIn(min = logoHeight).widthIn(max = logoWidth * 1.5f), contentAlignment = Alignment.BottomStart) {
        if (url.isNullOrEmpty() || failed) {
            if (url != null) Text(item.title, color = Color.White, style = style, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            // Software bitmaps can be sampled; a dark logo would vanish on the dark scrim.
            val request = remember(context, url) { ImageRequest.Builder(context).data(url).allowHardware(false).build() }
            AsyncImage(
                request, item.title,
                Modifier.size(logoWidth, logoHeight).graphicsLayer { alpha = reveal },
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                colorFilter = if (whiten) ColorFilter.tint(Color.White) else null,
                onSuccess = { success ->
                    val bitmap = success.result.image.toBitmap()
                    val pixels = buildList {
                        for (x in 0 until LogoSamples) for (y in 0 until LogoSamples) {
                            add(bitmap.getPixel(x * bitmap.width / LogoSamples, y * bitmap.height / LogoSamples))
                        }
                    }
                    whiten = (logoLuminance(pixels) ?: 1f) < DarkLogoLuminance
                    loaded = true
                },
                onError = { failed = true },
            )
        }
    }
}
