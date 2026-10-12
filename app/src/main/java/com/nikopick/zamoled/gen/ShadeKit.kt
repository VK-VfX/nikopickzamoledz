package com.nikopick.zamoled.gen

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Shading: the difference between a drawing and a painting.
 *
 * Everything in this app is drawn with paths, so the way to make a shape read as a solid
 * object rather than an outline is to light it. One key light sits up and to the left; a
 * shape gets a graded body colour, a dark edge away from the light, a highlight toward it,
 * and, where it is meant to be soft, fur along its silhouette.
 *
 * These helpers work in whatever coordinate space the canvas is in, so they can be used
 * inside [unit] blocks as well as on the full canvas.
 */
object Shade {

    /** Direction of the key light, as a fraction of the shape's own box. */
    const val LIGHT_X = 0.32f
    const val LIGHT_Y = 0.26f

    /** A fill that reads as a lit volume instead of a flat colour. */
    fun volume(box: RectF, color: Int, strength: Float = 1f): Paint {
        val r = max(box.width(), box.height()) * 0.78f
        val lx = box.left + box.width() * LIGHT_X
        val ly = box.top + box.height() * LIGHT_Y
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                lx, ly, max(r, 0.0001f),
                intArrayOf(
                    lighten(color, 0.42f * strength),
                    lighten(color, 0.1f * strength),
                    color,
                    darken(color, 0.52f * strength),
                ),
                floatArrayOf(0f, 0.3f, 0.62f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
    }

    /** The same, for a shape given as a path. */
    fun volume(path: Path, color: Int, strength: Float = 1f): Paint {
        val box = RectF()
        path.computeBounds(box, true)
        return volume(box, color, strength)
    }

    /**
     * Fur along a silhouette: short tapered strokes leaning out from the edge. This is what
     * stops an animal reading as a cut-out oval.
     */
    fun Scene.fur(path: Path, color: Int, length: Float, step: Float, jitter: Float = 0.45f) {
        val m = PathMeasure(path, false)
        do {
            val total = m.length
            if (total <= 0f) continue
            val pos = FloatArray(2)
            val tan = FloatArray(2)
            var d = 0f
            while (d < total) {
                m.getPosTan(d, pos, tan)
                val len = length * (1f + f(-jitter, jitter))
                // Outward normal, tilted a little so the coat has a direction.
                val lean = f(-0.5f, 0.5f)
                val nx = tan[1] + tan[0] * lean
                val ny = -tan[0] + tan[1] * lean
                val n = kotlin.math.sqrt(nx * nx + ny * ny).coerceAtLeast(0.0001f)
                val x2 = pos[0] + nx / n * len
                val y2 = pos[1] + ny / n * len
                canvas.drawLine(pos[0], pos[1], x2, y2, stroke(color, length * 0.22f, 190))
                d += step * (1f + f(-0.3f, 0.3f))
            }
        } while (m.nextContour())
    }

    /** Fur across the body rather than along its edge: strokes that follow the light. */
    fun Scene.coat(box: RectF, color: Int, n: Int, length: Float) {
        canvas.save()
        val clip = Path().apply { addOval(box, Path.Direction.CW) }
        canvas.clipPath(clip)
        repeat(n) {
            val x = f(box.left, box.right)
            val y = f(box.top, box.bottom)
            val a = f(1.0f, 1.6f)                       // combed down and slightly out
            val len = length * f(0.6f, 1.4f)
            canvas.drawLine(x, y, x + cos(a) * len, y + sin(a) * len, stroke(color, length * 0.16f, 110))
        }
        canvas.restore()
    }

    /** A soft dark band inside the lower edge of a shape: where the light does not reach. */
    fun Scene.occlude(shape: Path, depth: Float, alpha: Int = 150) {
        val box = RectF()
        shape.computeBounds(box, true)
        canvas.save()
        canvas.clipPath(shape)
        canvas.drawRect(
            box.left - depth, box.bottom - depth * 2.4f, box.right + depth, box.bottom + depth,
            fill(Color.BLACK, alpha, depth * 0.9f),
        )
        canvas.restore()
    }

    /** The specular: a small bright smear where the key light strikes. */
    fun Scene.sheen(box: RectF, alpha: Int = 90) {
        val rx = box.width() * 0.26f
        val ry = box.height() * 0.17f
        val cx = box.left + box.width() * LIGHT_X
        val cy = box.top + box.height() * LIGHT_Y
        canvas.drawOval(
            RectF(cx - rx, cy - ry, cx + rx, cy + ry),
            fill(Color.WHITE, alpha, max(rx, ry) * 0.7f),
        )
    }

    /** An eye with depth: iris, pupil, a catchlight toward the key light and a wet glint under it. */
    fun Scene.eye(x: Float, y: Float, r: Float, iris: Int) {
        canvas.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                x - r * 0.3f, y - r * 0.35f, r * 1.5f,
                intArrayOf(lighten(iris, 0.5f), iris, darken(iris, 0.7f)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
            )
        })
        canvas.drawCircle(x, y, r * 0.52f, fill(Color.rgb(12, 10, 14)))
        canvas.drawCircle(x - r * 0.3f, y - r * 0.34f, r * 0.26f, fill(Color.WHITE, 235))
        canvas.drawCircle(x + r * 0.26f, y + r * 0.3f, r * 0.14f, fill(Color.WHITE, 120))
        canvas.drawCircle(x, y, r, stroke(Color.BLACK, r * 0.16f, 180))
    }

    /** The shadow an object drops on the ground beneath it. */
    fun Scene.groundShadow(cx: Float, cy: Float, rx: Float, ry: Float, alpha: Int = 120) {
        canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), fill(Color.BLACK, alpha, ry * 1.2f))
    }

    /** Fine tooth over a shape, so large flat areas do not look like plastic. */
    fun Scene.grain(shape: Path, n: Int, color: Int, size: Float) {
        val box = RectF()
        shape.computeBounds(box, true)
        canvas.save()
        canvas.clipPath(shape)
        repeat(n) {
            canvas.drawCircle(f(box.left, box.right), f(box.top, box.bottom), size * f(0.4f, 1f), fill(color, i(10, 40)))
        }
        canvas.restore()
    }
}

/** Lighten toward white by [t]. */
internal fun lightenC(color: Int, t: Float): Int = ColorUtils.blendARGB(color, Color.WHITE, t)

/** A blurred paint for soft edges. */
internal fun softPaint(color: Int, alpha: Int, blur: Float): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.alpha = alpha
        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
    }
