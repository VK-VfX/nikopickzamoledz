package com.nikopick.zamoled.ui

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.nikopick.zamoled.data.Screen
import com.nikopick.zamoled.gen.Renderer
import com.nikopick.zamoled.gen.WallpaperSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
val renderDispatcher = Dispatchers.Default.limitedParallelism(3)

object ThumbCache {
    const val SMALL = 360
    const val LARGE = 720

    private val cache = object : LruCache<String, Bitmap>(64 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private fun key(spec: WallpaperSpec, width: Int) = "${spec.key}@$width"

    fun peek(spec: WallpaperSpec, width: Int): Bitmap? = cache.get(key(spec, width))

    /** Best already-rendered version of a spec, used as an instant placeholder. */
    fun peekBest(spec: WallpaperSpec): Bitmap? = peek(spec, LARGE) ?: peek(spec, SMALL)

    suspend fun load(spec: WallpaperSpec, width: Int): Bitmap = withContext(renderDispatcher) {
        cache.get(key(spec, width)) ?: Renderer.render(spec, width, (width / Screen.aspect).roundToInt())
            .also { cache.put(key(spec, width), it) }
    }
}

@Composable
fun rememberThumbnail(spec: WallpaperSpec, width: Int = ThumbCache.SMALL): State<Bitmap?> =
    produceState(initialValue = ThumbCache.peek(spec, width), spec.key, width) {
        if (value == null) value = ThumbCache.load(spec, width)
    }
