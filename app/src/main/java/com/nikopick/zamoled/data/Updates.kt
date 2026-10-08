package com.nikopick.zamoled.data

import com.nikopick.zamoled.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val apkUrl: String, val pageUrl: String)

/** Checks the GitHub releases of this repo for a newer version than the one installed. */
object Updates {
    private const val LATEST = "https://api.github.com/repos/VK-VfX/nikopickzamoledz/releases/latest"

    val installedVersion: String get() = BuildConfig.VERSION_NAME.substringBefore('-')

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val body = try {
                if (conn.responseCode != 200) return@withContext null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val json = JSONObject(body)
            val version = json.getString("tag_name").removePrefix("v")
            if (!isNewer(version, installedVersion)) return@withContext null
            val assets = json.getJSONArray("assets")
            var apk: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name") == "zamoled-release.apk") apk = a.getString("browser_download_url")
            }
            val page = json.optString("html_url")
            UpdateInfo(version, apk ?: page, page)
        } catch (e: Exception) {
            null
        }
    }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
