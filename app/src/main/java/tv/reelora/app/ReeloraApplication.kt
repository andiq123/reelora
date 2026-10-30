package tv.reelora.app

import android.app.Application
import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import okio.Path.Companion.toOkioPath
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import java.util.concurrent.TimeUnit

internal fun artworkMemoryBudget(heapBytes: Long, lowRam: Boolean): Long =
    minOf(heapBytes / 12, (if (lowRam) 8L else 24L) * 1024 * 1024)

class ReeloraApplication : Application(), SingletonImageLoader.Factory {
    private var artworkLoader: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        initHttpCache(cacheDir)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) CatalogRepository.clearDetailCaches()
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) artworkLoader?.memoryCache?.clear()
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizeBytes(artworkMemoryBudget(Runtime.getRuntime().maxMemory(), getSystemService(ActivityManager::class.java).isLowRamDevice))
                .build()
        }
        // Share the API client's connection pool and threads; Coil keeps its own disk cache.
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = {
                apiClient.newBuilder().cache(null).readTimeout(15, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
            }))
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("artwork").toOkioPath())
                .maxSizeBytes(96L * 1024 * 1024)
                .build()
        }
        .build().also { artworkLoader = it }
}
