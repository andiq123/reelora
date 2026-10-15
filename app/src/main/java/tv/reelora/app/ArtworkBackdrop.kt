package tv.reelora.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

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
