package com.nikopick.zamoled.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nikopick.zamoled.data.Prefs
import com.nikopick.zamoled.data.UpdateInfo
import com.nikopick.zamoled.data.UpdateResult
import com.nikopick.zamoled.data.Updates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class OtaPhase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, ERROR }

/** Where the update check stands, shared by the banner on the home screen and the Settings screen. */
@Stable
class OtaState(private val context: Context, private val scope: CoroutineScope, private val prefs: Prefs) {
    var phase by mutableStateOf(OtaPhase.IDLE)
        private set
    var info by mutableStateOf<UpdateInfo?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    /** The banner was closed for this session. */
    var dismissed by mutableStateOf(false)

    val available: Boolean get() = info != null && phase == OtaPhase.AVAILABLE

    /** True while the home banner should be showing. */
    val showBanner: Boolean get() = available && !dismissed && prefs.skippedVersion != info?.version

    fun check(manual: Boolean = false) {
        if (phase == OtaPhase.CHECKING) return
        scope.launch {
            phase = OtaPhase.CHECKING
            message = null
            when (val r = Updates.check()) {
                is UpdateResult.Available -> {
                    info = r.info
                    phase = OtaPhase.AVAILABLE
                    if (manual) dismissed = false
                }
                UpdateResult.UpToDate -> { info = null; phase = if (manual) OtaPhase.UP_TO_DATE else OtaPhase.IDLE }
                UpdateResult.Failed -> {
                    phase = if (manual) OtaPhase.ERROR else OtaPhase.IDLE
                    if (manual) message = "Could not reach GitHub. Check your connection."
                }
            }
        }
    }

    private fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** Opens the new APK in the browser, which downloads it. Tapping the finished download installs it over this app. */
    fun download() {
        info?.let { open(it.apkUrl) }
    }

    fun openReleasePage() {
        info?.let { open(it.pageUrl) }
    }

    fun skipVersion() {
        info?.let { prefs.skip(it.version) }
    }
}
