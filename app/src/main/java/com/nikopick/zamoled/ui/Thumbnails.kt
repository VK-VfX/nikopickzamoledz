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
    const val WIDTH = 360

    private val cache = object : LruCache<String, Bitmap>(48 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun peek(spec: WallpaperSpec): Bitmap? = cache.get(spec.key)

    suspend fun load(spec: WallpaperSpec): Bitmap = withContext(renderDispatcher) {
        cache.get(spec.key) ?: Renderer.render(spec, WIDTH, (WIDTH / Screen.aspect).roundToInt())
            .also { cache.put(spec.key, it) }
    }
}

@Composable
fun rememberThumbnail(spec: WallpaperSpec): State<Bitmap?> =
    produceState(initialValue = ThumbCache.peek(spec), spec.key) {
        if (value == null) value = ThumbCache.load(spec)
    }
