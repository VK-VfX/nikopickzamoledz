package com.nikopick.zamoled.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import com.nikopick.zamoled.gen.Category

val Category.icon: ImageVector
    get() = when (this) {
        Category.DOODLE -> Icons.Rounded.Gesture
        Category.MINIMAL -> Icons.Rounded.RadioButtonUnchecked
        Category.GEOMETRIC -> Icons.Rounded.Category
        Category.SPACE -> Icons.Rounded.NightsStay
        Category.NATURE -> Icons.Rounded.Landscape
        Category.ABSTRACT -> Icons.Rounded.BubbleChart
        Category.NEON -> Icons.Rounded.Bolt
    }

/** Each category gets its own expressive shape, in the style of Material 3's shape library. */
fun Category.polygon(): RoundedPolygon = when (this) {
    Category.DOODLE -> RoundedPolygon.star(8, radius = 1f, innerRadius = 0.78f, rounding = CornerRounding(0.18f))
    Category.MINIMAL -> RoundedPolygon.circle(numVertices = 10)
    Category.GEOMETRIC -> RoundedPolygon(6, rounding = CornerRounding(0.25f))
    Category.SPACE -> RoundedPolygon.star(5, radius = 1f, innerRadius = 0.58f, rounding = CornerRounding(0.2f))
    Category.NATURE -> RoundedPolygon(3, rounding = CornerRounding(0.35f))
    Category.ABSTRACT -> RoundedPolygon.star(12, radius = 1f, innerRadius = 0.86f, rounding = CornerRounding(0.12f))
    Category.NEON -> RoundedPolygon(4, rounding = CornerRounding(0.3f))
}.normalized()

private val Blob: RoundedPolygon = RoundedPolygon.star(4, radius = 1f, innerRadius = 0.9f, rounding = CornerRounding(0.5f)).normalized()

/** A Compose [Shape] drawn from a shape morph at a given progress (polygons normalised to 0..1). */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cubics = morph.asCubics(progress.coerceIn(0f, 1f))
        if (cubics.isEmpty()) return Outline.Rectangle(Rect(Offset.Zero, size))
        val w = size.width
        val h = size.height
        val path = Path()
        path.moveTo(cubics[0].anchor0X * w, cubics[0].anchor0Y * h)
        for (c in cubics) {
            path.cubicTo(c.control0X * w, c.control0Y * h, c.control1X * w, c.control1Y * h, c.anchor1X * w, c.anchor1Y * h)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Category icon inside its signature shape. It slowly rotates and morphs into a soft blob
 * when [active] (pressed or selected).
 */
@Composable
fun ShapeIcon(
    category: Category,
    active: Boolean,
    size: Dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    spin: Boolean = true,
) {
    val morph = remember(category) { Morph(category.polygon(), Blob) }
    val progress by animateFloatAsState(if (active) 1f else 0f, Motion.bouncy(), label = "morph")
    val rotation = if (spin) {
        val t = rememberInfiniteTransition(label = "spin")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "rot")
    } else {
        null
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation?.value ?: 0f }
            .clip(MorphShape(morph, progress))
            .background(container),
    ) {
        Icon(
            category.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier
                .size(size * 0.46f)
                .graphicsLayer { rotationZ = -(rotation?.value ?: 0f) },
        )
    }
}

/** App name in an animated Material You gradient. */
@OptIn(ExperimentalTextApi::class)
@Composable
fun GradientText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "title")
    val shift by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "titleShift",
    )
    val brush = Brush.linearGradient(
        listOf(cs.primary, cs.tertiary, cs.secondary, cs.primary),
        start = Offset(-700f * shift, 0f),
        end = Offset(-700f * shift + 900f, 140f),
        tileMode = TileMode.Mirror,
    )
    Text(text, style = style.copy(brush = brush), modifier = modifier)
}
