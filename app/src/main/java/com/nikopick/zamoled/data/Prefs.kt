package com.nikopick.zamoled.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nikopick.zamoled.gen.WallpaperSpec

/** Where the daily wallpaper picks from. */
const val AUTO_SOURCE_ALL = "all"
const val AUTO_SOURCE_FAVORITES = "favorites"

/** Small settings and the list of recently viewed wallpapers, kept across launches. */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("zamoled", Context.MODE_PRIVATE)

    /** Keys of recently opened wallpapers, newest first. */
    var history: List<String> by mutableStateOf(prefs.getString(HISTORY, "").orEmpty().split('\n').filter { it.isNotBlank() })
        private set

    var onboarded: Boolean by mutableStateOf(prefs.getBoolean(ONBOARDED, false))
        private set

    /** Names of the worlds picked at first launch; the feed leans toward them. */
    var likes: Set<String> by mutableStateOf(prefs.getStringSet(LIKES, emptySet())?.toSet() ?: emptySet())
        private set

    var autoEnabled: Boolean by mutableStateOf(prefs.getBoolean(AUTO_ON, false))
        private set
    var autoSource: String by mutableStateOf(prefs.getString(AUTO_SOURCE, AUTO_SOURCE_ALL) ?: AUTO_SOURCE_ALL)
        private set
    var autoTarget: WallpaperTarget by mutableStateOf(
        WallpaperTarget.entries.getOrElse(prefs.getInt(AUTO_TARGET, WallpaperTarget.BOTH.ordinal)) { WallpaperTarget.BOTH },
    )
        private set

    /** Check for new versions on launch and in the background. */
    var autoUpdate: Boolean by mutableStateOf(prefs.getBoolean(AUTO_UPDATE, true))
        private set

    /** A version the user chose not to be reminded about. */
    var skippedVersion: String by mutableStateOf(prefs.getString(SKIPPED, "").orEmpty())
        private set

    fun changeAutoUpdate(on: Boolean) {
        autoUpdate = on
        prefs.edit().putBoolean(AUTO_UPDATE, on).apply()
    }

    fun skip(version: String) {
        skippedVersion = version
        prefs.edit().putString(SKIPPED, version).apply()
    }

    fun remember(spec: WallpaperSpec) {
        history = (listOf(spec.key) + history.filter { it != spec.key }).take(MAX_HISTORY)
        prefs.edit().putString(HISTORY, history.joinToString("\n")).apply()
    }

    fun finishOnboarding(picked: Set<String>) {
        likes = picked
        onboarded = true
        prefs.edit().putStringSet(LIKES, picked).putBoolean(ONBOARDED, true).apply()
    }

    fun setAuto(enabled: Boolean, source: String, target: WallpaperTarget) {
        autoEnabled = enabled
        autoSource = source
        autoTarget = target
        prefs.edit().putBoolean(AUTO_ON, enabled).putString(AUTO_SOURCE, source).putInt(AUTO_TARGET, target.ordinal).apply()
    }

    companion object {
        private const val HISTORY = "history"
        private const val ONBOARDED = "onboarded"
        private const val LIKES = "likes"
        const val AUTO_UPDATE = "auto_update"
        const val SKIPPED = "skipped_version"
        const val NOTIFIED = "notified_version"
        const val AUTO_ON = "auto_on"
        const val AUTO_SOURCE = "auto_source"
        const val AUTO_TARGET = "auto_target"
        private const val MAX_HISTORY = 24
    }
}
