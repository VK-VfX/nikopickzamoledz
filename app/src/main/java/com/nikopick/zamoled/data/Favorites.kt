package com.nikopick.zamoled.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nikopick.zamoled.gen.WallpaperSpec

class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("zamoled", Context.MODE_PRIVATE)

    var keys: Set<String> by mutableStateOf(prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet())
        private set

    val specs: List<WallpaperSpec> get() = keys.sorted().mapNotNull { WallpaperSpec.fromKey(it) }

    fun isFavorite(spec: WallpaperSpec): Boolean = spec.key in keys

    fun toggle(spec: WallpaperSpec) {
        keys = if (spec.key in keys) keys - spec.key else keys + spec.key
        prefs.edit().putStringSet(KEY, keys).apply()
    }

    private companion object {
        const val KEY = "favorites"
    }
}
