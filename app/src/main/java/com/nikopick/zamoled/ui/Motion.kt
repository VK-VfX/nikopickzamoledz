package com.nikopick.zamoled.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/** Spring tokens in the spirit of the Material 3 expressive motion scheme. */
object Motion {
    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = 420f)
    fun <T> effects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
}

fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** Cards squish slightly while pressed and spring back on release. */
@Composable
fun Modifier.pressScale(interaction: InteractionSource, pressed: Float = 0.94f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, Motion.bouncy(), label = "press")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private object SeenItems {
    val keys = HashSet<String>()
}

/** Pops an item in (rise + fade + grow) the first time it appears, staggered by column. */
@Composable
fun Modifier.enterOnce(key: String, index: Int): Modifier {
    val seen = remember(key) { key in SeenItems.keys }
    val progress = remember(key) { Animatable(if (seen) 1f else 0f) }
    LaunchedEffect(key) {
        if (!seen) {
            SeenItems.keys += key
            delay((index % 6) * 40L)
            progress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 260f))
        }
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 80f
        val s = 0.9f + 0.1f * p
        scaleX = s
        scaleY = s
    }
}

/** Animated loading placeholder. */
@Composable
val LocalShimmer = androidx.compose.runtime.compositionLocalOf<androidx.compose.runtime.State<Float>?> { null }

/**
 * Placeholder shimmer. There is one clock for the whole screen, read only while drawing, so
 * thirty loading cards cost one animation and no recomposition rather than thirty of each.
 */
@Composable
fun ProvideShimmer(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x = transition.animateFloat(
        initialValue = -500f,
        targetValue = 1300f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmerX",
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalShimmer provides x, content = content)
}

@Composable
fun Modifier.shimmer(): Modifier {
    val clock = LocalShimmer.current
    val base = MaterialTheme.colorScheme.surfaceContainer
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val colors = remember(base, highlight) { listOf(base, highlight, base) }
    return drawBehind {
        val x = clock?.value ?: 0f
        drawRect(Brush.linearGradient(colors, start = Offset(x, 0f), end = Offset(x + 500f, 500f)))
    }
}
