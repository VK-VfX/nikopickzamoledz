package com.nikopick.zamoled.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Process
import com.nikopick.zamoled.BuildConfig
import java.io.File
import java.security.MessageDigest
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.nikopick.zamoled.data.Screen
import com.nikopick.zamoled.gen.Renderer
import com.nikopick.zamoled.gen.WallpaperSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Used for full-size renders the user asked for: the big preview, saving, and the matching pair. */
@OptIn(ExperimentalCoroutinesApi::class)
val renderDispatcher = Dispatchers.Default.limitedParallelism(3)

private fun pool(threads: Int, priority: Int, name: String) = Executors.newFixedThreadPool(threads) { r ->
    Thread({ Process.setThreadPriority(priority); r.run() }, name)
}.asCoroutineDispatcher()

object ThumbCache {
    const val SMALL = 360
    const val LARGE = 720
    private const val DISK_LIMIT = 160L * 1024 * 1024

    private var dir: File? = null

    // Thumbnails are drawn on two background threads, so scrolling never has to share a core with them.
    private val thumbPool = pool(2, Process.THREAD_PRIORITY_BACKGROUND, "thumb-render")
    private val prefetchPool = pool(1, Process.THREAD_PRIORITY_LOWEST, "thumb-prefetch")
    private val diskPool = Executors.newSingleThreadExecutor { r -> Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST); r.run() }, "thumb-disk") }

    /** Waiting this long before drawing a thumbnail means cards flung past never cost anything. */
    private const val RENDER_DELAY_MS = 90L

    /** True while the feed is being scrolled; prefetching waits for it to stop. */
    @Volatile
    var scrolling = false

    private val rendering = ConcurrentHashMap.newKeySet<String>()

    /** Thumbnails are also kept on disk, so favourites, history and covers are instant after the first launch. */
    fun init(context: Context) {
        dir = File(context.cacheDir, "thumbs").apply { mkdirs() }
        Thread {
            val files = dir?.listFiles() ?: return@Thread
            var total = files.sumOf { it.length() }
            if (total > DISK_LIMIT) for (f in files.sortedBy { it.lastModified() }) {
                total -= f.length(); f.delete()
                if (total < DISK_LIMIT * 0.7) break
            }
        }.start()
    }

    private fun diskFile(key: String): File? {
        val d = dir ?: return null
        val hash = MessageDigest.getInstance("MD5").digest("${BuildConfig.VERSION_CODE}|$key".toByteArray()).joinToString("") { "%02x".format(it) }
        return File(d, "$hash.webp")
    }

    private val cache = object : LruCache<String, Bitmap>(64 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private fun key(spec: WallpaperSpec, width: Int) = "${spec.key}@$width"

    fun peek(spec: WallpaperSpec, width: Int): Bitmap? = cache.get(key(spec, width))

    /** Best already-rendered version of a spec, used as an instant placeholder. */
    fun peekBest(spec: WallpaperSpec): Bitmap? = peek(spec, LARGE) ?: peek(spec, SMALL)

    suspend fun load(spec: WallpaperSpec, width: Int): Bitmap {
        val k = key(spec, width)
        cache.get(k)?.let { return it }
        val file = diskFile(k)
        if (file != null && file.exists()) {
            val stored = withContext(thumbPool) { runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() }
            if (stored != null) {
                file.setLastModified(System.currentTimeMillis())
                cache.put(k, stored)
                return stored
            }
        }
        // Not drawn before. If the card is gone by the end of this pause, nothing is drawn at all.
        delay(RENDER_DELAY_MS)
        return render(spec, width, k, file, thumbPool)
    }

    private suspend fun render(spec: WallpaperSpec, width: Int, k: String, file: File?, on: CoroutineDispatcher): Bitmap =
        withContext(on) {
            cache.get(k)?.let { return@withContext it }
            rendering += k
            try {
                Renderer.render(spec, width, (width / Screen.aspect).roundToInt()).also { bmp ->
                    cache.put(k, bmp)
                    if (file != null) diskPool.execute {
                        runCatching {
                            file.outputStream().use {
                                @Suppress("DEPRECATION")
                                bmp.compress(if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP, 90, it)
                            }
                        }
                    }
                }
            } finally {
                rendering -= k
            }
        }

    /** Draws the rest of a batch in the background while the screen is idle, so scrolling meets finished thumbnails. */
    suspend fun prefetch(specs: List<WallpaperSpec>, width: Int = SMALL) {
        for (spec in specs) {
            while (scrolling) delay(250)
            val k = key(spec, width)
            if (cache.get(k) != null || k in rendering) continue
            val file = diskFile(k)
            if (file != null && file.exists()) continue
            currentCoroutineContext().ensureActive()
            render(spec, width, k, file, prefetchPool)
        }
    }

    fun clearDisk(): Long {
        val files = dir?.listFiles() ?: return 0L
        val size = files.sumOf { it.length() }
        files.forEach { it.delete() }
        return size
    }

    fun diskSize(): Long = dir?.listFiles()?.sumOf { it.length() } ?: 0L
}

@Composable
fun rememberThumbnail(spec: WallpaperSpec, width: Int = ThumbCache.SMALL): State<Bitmap?> =
    produceState(initialValue = ThumbCache.peek(spec, width), spec.key, width) {
        if (value == null) value = ThumbCache.load(spec, width)
    }

/** A thumbnail that fades in once when it first arrives, and costs nothing if it was already there. */
@Composable
fun ThumbImage(bitmap: Bitmap?, description: String?, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val fade = remember { Animatable(if (bitmap != null) 1f else 0f) }
    LaunchedEffect(bitmap != null) { if (bitmap != null && fade.value < 1f) fade.animateTo(1f, tween(180)) }
    if (bitmap == null) {
        Box(modifier.shimmer())
    } else {
        val image = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            image, contentDescription = description, contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low,
            modifier = modifier.graphicsLayer { this.alpha = fade.value * alpha },
        )
    }
}
