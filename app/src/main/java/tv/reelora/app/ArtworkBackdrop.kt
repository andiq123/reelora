package tv.reelora.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

// One RGBA texture is at most 8 MiB. Larger displays draw normally instead of
// retaining a 4K buffer. Keep changing controls outside this static layer.
internal fun retainBackdrop(width: Int, height: Int): Boolean =
    width > 0 && height > 0 && width.toLong() * height <= 1920L * 1080

@Composable
internal fun ArtworkBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier.fillMaxSize().onSizeChanged { size = it }.graphicsLayer {
        compositingStrategy = if (retainBackdrop(size.width, size.height)) {
            CompositingStrategy.Offscreen
        } else CompositingStrategy.Auto
    }, content = content)
}

// Keep the last successful image underneath until its replacement has loaded.
// Only the incoming image animates; a failed request leaves the artwork intact.
@Composable
internal fun CinematicArtwork(url: String?) {
    val context = LocalContext.current
    var previous by remember { mutableStateOf<Painter?>(null) }
    var loaded by remember(url) { mutableStateOf<Painter?>(null) }
    var complete by remember(url) { mutableStateOf(false) }
    val reveal = remember(url) { Animatable(0f) }
    val incoming = remember(context, url) { ImageRequest.Builder(context).data(url).build() }
    LaunchedEffect(url, loaded) {
        if (url != null && loaded != null) {
            reveal.animateTo(1f, tween(TvMotion.ArtworkMillis, easing = LinearOutSlowInEasing))
            previous = loaded
            complete = true
        }
    }
    Box(Modifier.fillMaxSize()) {
        previous?.takeIf { !complete }?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (url != null) {
            AsyncImage(
                incoming,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = it.painter },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = reveal.value
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
            )
        }
    }
}
