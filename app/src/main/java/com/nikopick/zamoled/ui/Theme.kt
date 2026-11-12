package com.nikopick.zamoled.ui

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikopick.zamoled.gen.Palette
import com.nikopick.zamoled.gen.Palettes

// Used below Android 12, where there is no Material You: a single hot accent on black.
private val Fallback = darkColorScheme(
    primary = Color(0xFFFF6A3D),
    onPrimary = Color(0xFF1A0600),
    primaryContainer = Color(0xFF5A1D0B),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFFFB59E),
    secondaryContainer = Color(0xFF3F1E12),
    onSecondaryContainer = Color(0xFFFFDBD0),
    tertiary = Color(0xFFE9C46A),
    tertiaryContainer = Color(0xFF4A3A0B),
    onTertiaryContainer = Color(0xFFFFE9A8),
)

/** Condensed bold for headings, monospace for captions, the system face for reading. */
private val Condensed = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Bold),
    Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Normal),
)
private val Mono = FontFamily.Monospace

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp),
        displayMedium = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 48.sp, letterSpacing = (-1.6).sp),
        displaySmall = displaySmall.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        headlineLarge = headlineLarge.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        titleMedium = titleMedium.copy(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 0.2.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = Mono, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
        labelSmall = labelSmall.copy(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 10.5.sp),
        bodyMedium = bodyMedium.copy(letterSpacing = 0.15.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
    large = RoundedCornerShape(topStart = 26.dp, topEnd = 7.dp, bottomEnd = 26.dp, bottomStart = 7.dp),
    extraLarge = RoundedCornerShape(topStart = 34.dp, topEnd = 8.dp, bottomEnd = 34.dp, bottomStart = 8.dp),
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
            surfaceContainerLow = Color(0xFF070709),
            surfaceContainer = Color(0xFF0D0D10),
            surfaceContainerHigh = Color(0xFF151519),
            surfaceContainerHighest = Color(0xFF1E1E24),
            outlineVariant = Color.White.copy(alpha = 0.14f),
        ).also {
            Palettes.you = Palette(
                "you", "Material You",
                intArrayOf(it.primary.toArgb(), it.tertiary.toArgb(), it.secondary.toArgb()),
            )
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
