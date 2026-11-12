package com.nikopick.zamoled.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nikopick.zamoled.BuildConfig
import com.nikopick.zamoled.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class UpdateInfo(val version: String, val apkUrl: String, val pageUrl: String, val notes: String = "", val size: Long = 0L)

/** What a check for a new version came back with. */
sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data object Failed : UpdateResult
    data class Available(val info: UpdateInfo) : UpdateResult
}

/**
 * Over-the-air updates: look at the latest GitHub release, download its APK inside the app with
 * progress, and hand it to the system installer. Nothing leaves the phone except the download.
 */
object Updates {
    private const val LATEST = "https://api.github.com/repos/VK-VfX/nikopickzamoledz/releases/latest"
    private const val WORK = "zamoled-update-check"
    private const val CHANNEL = "updates"

    val installedVersion: String get() = BuildConfig.VERSION_NAME.substringBefore('-')

    /** The debug build installs next to the release build, so each follows its own APK. */
    private val assetName: String get() = if (BuildConfig.DEBUG) "zamoled-debug.apk" else "zamoled-release.apk"

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val body = try {
                if (conn.responseCode != 200) return@withContext UpdateResult.Failed
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val json = JSONObject(body)
            val version = json.getString("tag_name").removePrefix("v")
            if (!isNewer(version, installedVersion)) return@withContext UpdateResult.UpToDate
            val assets = json.getJSONArray("assets")
            var apk: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name") == assetName) {
                    apk = a.getString("browser_download_url")
                    size = a.optLong("size")
                }
            }
            val page = json.optString("html_url")
            UpdateResult.Available(UpdateInfo(version, apk ?: page, page, json.optString("body").trim(), size))
        } catch (e: Exception) {
            UpdateResult.Failed
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

    fun apkFile(context: Context, version: String): File = File(File(context.cacheDir, "updates").apply { mkdirs() }, "zamoled-$version.apk")

    /** True when this version's file is already downloaded in full. */
    fun isDownloaded(context: Context, info: UpdateInfo): Boolean {
        val f = apkFile(context, info.version)
        return f.exists() && (info.size == 0L || f.length() == info.size)
    }

    /** Downloads the APK into the app's cache, reporting 0..1 as it goes. Returns the file, or null on failure. */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        val target = apkFile(context, info.version)
        if (isDownloaded(context, info)) return@withContext target
        val part = File(target.parentFile, target.name + ".part")
        try {
            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
                instanceFollowRedirects = true
            }
            try {
                if (conn.responseCode != 200) return@withContext null
                val total = if (conn.contentLengthLong > 0) conn.contentLengthLong else info.size
                var done = 0L
                conn.inputStream.use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            if (!part.renameTo(target)) return@withContext null
            target
        } catch (e: Exception) {
            part.delete()
            null
        }
    }

    /** Opens the system installer on [file]. Returns false when the app first needs permission to install apps. */
    fun install(context: Context, file: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }

    /** Removes downloaded installers for versions that are no longer newer than the installed one. */
    fun cleanOld(context: Context) {
        File(context.cacheDir, "updates").listFiles()?.forEach { f ->
            val v = f.name.removePrefix("zamoled-").removeSuffix(".apk").removeSuffix(".apk.part")
            if (!isNewer(v, installedVersion)) f.delete()
        }
    }

    /** Checks about twice a day in the background, on any network, and posts a notification when there is something new. */
    fun schedule(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    internal fun notify(context: Context, info: UpdateInfo) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "App updates", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Zamoled ${info.version} is ready")
            .setContentText("Open the app to download and install it.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(7, n)
    }
}

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("zamoled", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Prefs.AUTO_UPDATE, true)) return Result.success()
        val found = Updates.check()
        if (found is UpdateResult.Available) {
            val v = found.info.version
            // Say it once per version.
            if (prefs.getString(Prefs.NOTIFIED, "") != v && prefs.getString(Prefs.SKIPPED, "") != v) {
                Updates.notify(applicationContext, found.info)
                prefs.edit().putString(Prefs.NOTIFIED, v).apply()
            }
        }
        return Result.success()
    }
}
