package com.nikopick.zamoled.ui

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nikopick.zamoled.data.Prefs
import com.nikopick.zamoled.data.UpdateInfo
import com.nikopick.zamoled.data.UpdateResult
import com.nikopick.zamoled.data.Updates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class OtaPhase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY, ERROR }

/** Where the update stands, shared by the banner on the home screen and the Settings screen. */
@Stable
class OtaState(private val context: Context, private val scope: CoroutineScope, private val prefs: Prefs) {
    var phase by mutableStateOf(OtaPhase.IDLE)
        private set
    var info by mutableStateOf<UpdateInfo?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    /** The banner was closed for this session. */
    var dismissed by mutableStateOf(false)

    val available: Boolean get() = info != null && phase in setOf(OtaPhase.AVAILABLE, OtaPhase.DOWNLOADING, OtaPhase.READY, OtaPhase.ERROR)

    /** True while the home banner should be showing. */
    val showBanner: Boolean get() = available && !dismissed && prefs.skippedVersion != info?.version

    fun check(manual: Boolean = false) {
        if (phase == OtaPhase.CHECKING || phase == OtaPhase.DOWNLOADING) return
        scope.launch {
            phase = OtaPhase.CHECKING
            message = null
            when (val r = Updates.check()) {
                is UpdateResult.Available -> {
                    info = r.info
                    phase = if (Updates.isDownloaded(context, r.info)) OtaPhase.READY else OtaPhase.AVAILABLE
                    if (manual) dismissed = false
                }
                UpdateResult.UpToDate -> { info = null; phase = if (manual) OtaPhase.UP_TO_DATE else OtaPhase.IDLE }
                UpdateResult.Failed -> {
                    phase = if (manual) OtaPhase.ERROR else OtaPhase.IDLE
                    if (manual) message = "Could not reach the update server. Check your connection."
                }
            }
        }
    }

    fun download() {
        val i = info ?: return
        if (phase == OtaPhase.DOWNLOADING) return
        scope.launch {
            phase = OtaPhase.DOWNLOADING
            progress = 0f
            val file = Updates.download(context, i) { progress = it }
            if (file != null) {
                phase = OtaPhase.READY
            } else {
                phase = OtaPhase.ERROR
                message = "The download did not finish. Try again."
            }
        }
    }

    fun install() {
        val i = info ?: return
        val ok = Updates.install(context, Updates.apkFile(context, i.version))
        message = if (ok) null else "Allow Zamoled to install apps, then press Install again."
    }

    fun skipVersion() {
        info?.let { prefs.skip(it.version) }
    }
}
