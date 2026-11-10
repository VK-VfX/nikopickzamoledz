package com.nikopick.zamoled.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nikopick.zamoled.gen.Category
import com.nikopick.zamoled.gen.Renderer
import com.nikopick.zamoled.gen.Styles
import com.nikopick.zamoled.gen.WallpaperSpec
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Sets a fresh wallpaper once a day, in the background, from the source the user chose. */
class AutoWallpaperWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences("zamoled", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Prefs.AUTO_ON, false)) return Result.success()
        val source = prefs.getString(Prefs.AUTO_SOURCE, AUTO_SOURCE_ALL) ?: AUTO_SOURCE_ALL
        val target = WallpaperTarget.entries.getOrElse(prefs.getInt(Prefs.AUTO_TARGET, WallpaperTarget.BOTH.ordinal)) { WallpaperTarget.BOTH }
        val spec = AutoWallpaper.pick(source, prefs.getStringSet("favorites", emptySet()).orEmpty()) ?: return Result.success()
        Screen.init(context)
        com.nikopick.zamoled.gen.CodeSource.init(context.assets)
        return try {
            val bitmap = Renderer.render(spec, Screen.width, Screen.height)
            val ok = WallpaperActions.setWallpaper(context, bitmap, target)
            bitmap.recycle()
            if (ok) Result.success() else Result.retry()
        } catch (e: Throwable) {
            // Out of memory or a drawing failure: try again later rather than crash the worker.
            Result.retry()
        }
    }
}

object AutoWallpaper {
    private const val WORK = "zamoled-daily-wallpaper"

    /** A wallpaper from [source]: "all", "favorites", or a category name. */
    fun pick(source: String, favorites: Set<String>): WallpaperSpec? {
        if (source == AUTO_SOURCE_FAVORITES) {
            val saved = favorites.mapNotNull { WallpaperSpec.fromKey(it) }
            if (saved.isNotEmpty()) return saved.random()
        }
        val category = Category.entries.firstOrNull { it.name == source }
        // Styles that set typed words are skipped: a random phrase is not what anyone wants overnight.
        val pool = (if (category != null) Styles.of(category) else Styles.all).filter { !it.usesText }.ifEmpty { Styles.all }
        return WallpaperSpec(pool.random().id, Random.nextLong())
    }

    /** Starts or stops the daily job to match [enabled]. */
    fun schedule(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            manager.cancelUniqueWork(WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(24, TimeUnit.HOURS).build()
        manager.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
