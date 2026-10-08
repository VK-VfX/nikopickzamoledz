package com.nikopick.zamoled.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.nikopick.zamoled.gen.Palette
import com.nikopick.zamoled.gen.Palettes

private val Fallback = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFC2E7FF),
    secondaryContainer = Color(0xFF004A77),
    onSecondaryContainer = Color(0xFFC2E7FF),
    tertiary = Color(0xFFFFB0CB),
)

/** Material 3 dark scheme with every background surface forced to true black. */
@Composable
fun ZamoledTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else Fallback
    val scheme = remember(base) {
        base.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0C),
            surfaceContainer = Color(0xFF111114),
            surfaceContainerHigh = Color(0xFF18181C),
            surfaceContainerHighest = Color(0xFF212126),
        ).also {
            Palettes.you = Palette(
                "you", "Material You",
                intArrayOf(it.primary.toArgb(), it.tertiary.toArgb(), it.secondary.toArgb()),
            )
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
