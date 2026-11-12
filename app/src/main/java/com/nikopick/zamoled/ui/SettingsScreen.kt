package com.nikopick.zamoled.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nikopick.zamoled.data.Prefs
import com.nikopick.zamoled.data.Updates
import kotlin.math.roundToInt

private fun mb(bytes: Long): String = if (bytes <= 0L) "" else "%.1f MB".format(bytes / 1_048_576f)

/** Updates, the daily wallpaper, storage and a line about the app. */
@Composable
fun SettingsScreen(ota: OtaState, prefs: Prefs, onBack: () -> Unit, onDaily: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var cacheSize by remember { mutableLongStateOf(ThumbCache.diskSize()) }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Column {
                MonoLabel("Zamoled")
                Text("Settings", style = MaterialTheme.typography.headlineLarge)
            }
        }
        Spacer(Modifier.height(12.dp))

        // ---- Updates ----
        SectionHead("01", "Updates")
        Spacer(Modifier.height(10.dp))
        LeafCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        MonoLabel("Installed")
                        Text(Updates.installedVersion, style = MaterialTheme.typography.displaySmall)
                    }
                    val i = ota.info
                    if (ota.available && i != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            MonoLabel("New", color = MaterialTheme.colorScheme.primary)
                            Text(i.version, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                val status = when (ota.phase) {
                    OtaPhase.CHECKING -> "Checking for a new version…"
                    OtaPhase.UP_TO_DATE -> "You are on the latest version."
                    OtaPhase.AVAILABLE -> "A new version is ready to download${ota.info?.size?.let { if (it > 0) " (${mb(it)})" else "" }.orEmpty()}."
                    OtaPhase.DOWNLOADING -> "Downloading… ${(ota.progress * 100).roundToInt()}%"
                    OtaPhase.READY -> "Downloaded. Install to update; your wallpapers and favorites stay."
                    OtaPhase.ERROR -> ota.message ?: "Something went wrong."
                    OtaPhase.IDLE -> "Tap check to look for a new version."
                }
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (ota.phase == OtaPhase.DOWNLOADING) {
                    LinearProgressIndicator(progress = { ota.progress }, modifier = Modifier.fillMaxWidth())
                } else if (ota.phase == OtaPhase.CHECKING) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (ota.phase != OtaPhase.READY && ota.phase != OtaPhase.DOWNLOADING && ota.message != null && ota.phase != OtaPhase.ERROR) {
                    Text(ota.message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (ota.phase) {
                        OtaPhase.AVAILABLE -> Button(onClick = { ota.download() }) { Text("Download") }
                        OtaPhase.READY -> Button(onClick = { ota.install() }) { Text("Install now") }
                        OtaPhase.DOWNLOADING -> OutlinedButton(onClick = {}, enabled = false) { Text("Downloading") }
                        OtaPhase.ERROR -> if (ota.info != null) Button(onClick = { ota.download() }) { Text("Try again") } else Button(onClick = { ota.check(manual = true) }) { Text("Check again") }
                        else -> Button(onClick = { ota.check(manual = true) }, enabled = ota.phase != OtaPhase.CHECKING) { Text("Check for updates") }
                    }
                    if (ota.available) TextButton(onClick = { ota.info?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.pageUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) { Text("Release page") }
                }
                val notes = ota.info?.notes.orEmpty()
                if (ota.available && notes.isNotBlank()) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                    MonoLabel("What's new")
                    Text(notes.take(700) + if (notes.length > 700) "…" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { ota.skipVersion(); ota.dismissed = true }) { Text("Skip this version") }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LeafCard(Modifier.fillMaxWidth(), shape = LeafSm) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Check automatically", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Looks for a new version when the app opens and about twice a day, and sends one notification when there is one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = prefs.autoUpdate,
                    onCheckedChange = {
                        prefs.changeAutoUpdate(it)
                        Updates.schedule(context, it)
                        if (it && Build.VERSION.SDK_INT >= 33) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }
        }

        // ---- Daily wallpaper ----
        Spacer(Modifier.height(22.dp))
        SectionHead("02", "Daily wallpaper")
        Spacer(Modifier.height(10.dp))
        LeafCard(Modifier.fillMaxWidth().clickable { onDaily() }, shape = LeafSm) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(18.dp)) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = if (prefs.autoEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text(if (prefs.autoEnabled) "On" else "Off", style = MaterialTheme.typography.titleMedium)
                    Text("A fresh wallpaper about once a day, set in the background.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                MonoLabel("Edit", color = MaterialTheme.colorScheme.primary)
            }
        }

        // ---- Storage ----
        Spacer(Modifier.height(22.dp))
        SectionHead("03", "Storage")
        Spacer(Modifier.height(10.dp))
        LeafCard(Modifier.fillMaxWidth(), shape = LeafSm) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Preview cache", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (cacheSize > 0) "${mb(cacheSize)} of saved previews keep the app instant. They are drawn again when needed." else "Empty.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = { ThumbCache.clearDisk(); cacheSize = ThumbCache.diskSize() }, enabled = cacheSize > 0) { Text("Clear") }
            }
        }

        // ---- About ----
        Spacer(Modifier.height(22.dp))
        SectionHead("04", "About")
        Spacer(Modifier.height(10.dp))
        Text(
            "Zamoled draws every wallpaper on your phone, so nothing is downloaded and every picture can be saved at your screen size, QHD+ or 4K. Pure black pixels are switched off on an AMOLED screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/VK-VfX/nikopickzamoledz/releases")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
            Text("All releases")
        }
        Spacer(Modifier.height(40.dp))
    }
}
