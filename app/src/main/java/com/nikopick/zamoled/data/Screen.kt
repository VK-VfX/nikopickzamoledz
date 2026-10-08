package com.nikopick.zamoled.data

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.min

/** Physical screen size in portrait pixels. */
object Screen {
    var width = 1080
        private set
    var height = 2400
        private set

    val aspect: Float get() = width / height.toFloat()

    fun init(context: Context) {
        val wm = context.getSystemService(WindowManager::class.java) ?: return
        val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val m = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
        width = min(w, h)
        height = max(w, h)
    }
}

enum class Resolution(val label: String, private val w: Int, private val h: Int) {
    SCREEN("Screen", 0, 0),
    QHD("QHD+", 1440, 3200),
    UHD("4K", 2160, 3840);

    val width: Int get() = if (this == SCREEN) Screen.width else w
    val height: Int get() = if (this == SCREEN) Screen.height else h
}
