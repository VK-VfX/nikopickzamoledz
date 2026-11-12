package com.nikopick.zamoled.desktop

import com.nikopick.zamoled.gen.CodeSource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The window lives inside this "test" because the Android graphics classes only work inside the
 * Robolectric sandbox. The test ends when the window is closed.
 */
@RunWith(RobolectricTestRunner::class)
class DesktopApp {
    @Test
    fun run() {
        val code = System.getProperty("zamoled.home")?.let { File(it, "code") }
        if (code != null && code.isDirectory) CodeSource.init(code)
        val window = ZamoledWindow()
        window.open()
        System.setProperty("zamoled.ready", "1")
        window.awaitClose()
    }
}
