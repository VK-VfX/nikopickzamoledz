package com.nikopick.zamoled.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.nikopick.zamoled.BuildConfig
import java.io.File
import java.security.MessageDigest
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
    private const val DISK_LIMIT = 160L * 1024 * 1024

    private var dir: File? = null

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

    suspend fun load(spec: WallpaperSpec, width: Int): Bitmap = withContext(renderDispatcher) {
        val k = key(spec, width)
        cache.get(k) ?: run {
            val file = diskFile(k)
            val stored = file?.takeIf { it.exists() }?.let { runCatching { BitmapFactory.decodeFile(it.path) }.getOrNull() }
            if (stored != null) {
                file.setLastModified(System.currentTimeMillis())
                stored
            } else {
                Renderer.render(spec, width, (width / Screen.aspect).roundToInt()).also { bmp ->
                    runCatching {
                        file?.outputStream()?.use {
                            @Suppress("DEPRECATION")
                            bmp.compress(if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP, 90, it)
                        }
                    }
                }
            }.also { cache.put(k, it) }
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
