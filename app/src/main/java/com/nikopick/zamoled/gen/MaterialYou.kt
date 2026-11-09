package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Wallpapers in Material 3 Expressive: big, soft, rounded shapes in layered tonal colour, with
 * nothing on the page but black. They follow the phone's own Material You colours, so each one
 * matches the theme the system picked from the user's wallpaper and icons.
 */

private const val TAU2 = (2.0 * PI).toFloat()

/**
 * A colour at tone [t] (0 black, 100 white), the way Material's tonal palettes are laid out.
 * Material uses HCT; this keeps the hue and saturation and sets the HSL lightness, which is close
 * enough to read as the same family of colour.
 */
internal fun tone(color: Int, t: Int): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color, hsl)
    hsl[1] = (hsl[1] * if (t < 40) 0.92f else 1f).coerceIn(0f, 1f)
    hsl[2] = (t / 100f).coerceIn(0.02f, 0.98f)
    return ColorUtils.HSLToColor(hsl)
}

/** The Material 3 shape library, as paths. Every shape is centred on (cx, cy) with outer radius r. */
internal object M3Shapes {
    private fun polar(cx: Float, cy: Float, r: Float, rot: Float, radius: (Float) -> Float): Path {
        val p = Path()
        val n = 360
        for (i in 0..n) {
            val th = i * TAU2 / n
            val rr = r * radius(th)
            val x = cx + cos(th + rot) * rr
            val y = cy + sin(th + rot) * rr
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    /** Round petals: the flower and the clover. */
    fun flower(cx: Float, cy: Float, r: Float, petals: Int, depth: Float, rot: Float = 0f): Path =
        polar(cx, cy, r, rot) { th -> 1f - depth + depth * (1f + cos(petals * th)) / 2f }

    /** Flat-topped scallops with soft notches between: the cookie. */
    fun cookie(cx: Float, cy: Float, r: Float, lobes: Int, depth: Float, rot: Float = 0f): Path {
        val k = 1.15f                       // low: soft rounded lobes, not cog teeth
        val norm = tanh(k)
        return polar(cx, cy, r, rot) { th -> 1f - depth / 2f + depth / 2f * tanh(k * cos(lobes * th)) / norm }
    }

    /** Pointed rays with rounded tips: the burst. */
    fun burst(cx: Float, cy: Float, r: Float, rays: Int, depth: Float, rot: Float = 0f): Path =
        polar(cx, cy, r, rot) { th -> 1f - depth + depth * abs(cos(rays * th / 2f)).pow(2.6f) }

    /** Between a square and a circle. */
    fun squircle(cx: Float, cy: Float, r: Float, rot: Float = 0f, p: Float = 4.2f): Path =
        polar(cx, cy, r, rot) { th -> (abs(cos(th)).pow(p) + abs(sin(th)).pow(p)).pow(-1f / p) }

    fun circle(cx: Float, cy: Float, r: Float): Path = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }

    fun pill(cx: Float, cy: Float, length: Float, thick: Float): Path =
        Path().apply { addRoundRect(RectF(cx - length / 2f, cy - thick / 2f, cx + length / 2f, cy + thick / 2f), thick / 2f, thick / 2f, Path.Direction.CW) }
}

/** The three source colours of the palette, and a way to ask for them at any tone. */
private class Tonal(s: Scene) {
    private val c = s.palette.colors
    val primary = c[0]
    val tertiary = c[1 % c.size]
    val secondary = c[2 % c.size]
    fun p(t: Int) = tone(primary, t)
    fun t(t: Int) = tone(tertiary, t)
    fun s(t: Int) = tone(secondary, t)
    fun pick(i: Int, t: Int) = when (((i % 3) + 3) % 3) { 0 -> p(t); 1 -> t(t); else -> s(t) }
}

/** Fill a shape with a tone that falls off gently from its upper left, as Material surfaces do. */
private fun Scene.tonal(path: Path, color: Int) {
    val box = RectF()
    path.computeBounds(box, true)
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            box.left, box.top, box.right, box.bottom,
            ColorUtils.blendARGB(color, Color.WHITE, 0.09f), ColorUtils.blendARGB(color, Color.BLACK, 0.1f), Shader.TileMode.CLAMP,
        )
    })
}

/** A big flower of nested tonal shapes, set low so the clock has the top of the screen. */
object M3Bloom : Style("m3_bloom", "Bloom", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val cx = s.cx + s.f(-80f, 80f)
        val cy = s.h * s.f(0.6f, 0.7f)
        val r = s.w * s.f(0.5f, 0.62f)
        val petals = s.i(6, 11) + ((s.density - 1f) * 3f).toInt()
        val rot = s.f(0f, TAU2)
        val o = s.i(0, 3)
        s.tonal(M3Shapes.flower(cx, cy, r, petals, s.f(0.2f, 0.3f), rot), t.pick(o, 30))
        s.tonal(M3Shapes.cookie(cx, cy, r * 0.7f, petals + 3, 0.2f, rot + 0.3f), t.pick(o + 1, 50))
        s.tonal(M3Shapes.flower(cx, cy, r * 0.42f, petals, 0.2f, rot + 0.6f), t.pick(o + 2, 70))
        s.tonal(M3Shapes.circle(cx, cy, r * 0.17f), t.pick(o, 90))
    }
}

/** Three cookies stacked and turned against each other, big to small. */
object M3Cookies : Style("m3_cookies", "Cookies", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val cx = s.cx + s.f(-60f, 60f)
        val cy = s.h * s.f(0.64f, 0.72f)
        val o = s.i(0, 3)
        val lobes = s.i(6, 10) + ((s.density - 1f) * 3f).toInt()
        val r = s.w * s.f(0.5f, 0.58f)
        s.tonal(M3Shapes.cookie(cx, cy, r, lobes, 0.2f, s.f(0f, TAU2)), t.pick(o, 30))
        s.tonal(M3Shapes.cookie(cx + s.f(-30f, 30f), cy - r * 0.05f, r * 0.68f, lobes + 2, 0.18f, s.f(0f, TAU2)), t.pick(o + 1, 55))
        s.tonal(M3Shapes.cookie(cx + s.f(-20f, 20f), cy - r * 0.08f, r * 0.38f, lobes - 2, 0.2f, s.f(0f, TAU2)), t.pick(o + 2, 80))
    }
}

/** Material's wavy progress indicator, as a wallpaper: thick rounded waves, each a different tone. */
object M3Waves : Style("m3_waves", "Waves", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val thick = s.f(34f, 50f) / (0.6f + 0.4f * s.density)
        val gap = thick * s.f(1.9f, 2.3f)
        val top = s.h * s.f(0.34f, 0.42f)
        val o = s.i(0, 3)
        val bands = ((s.h - top) / gap).toInt() + 2
        for (i in 0 until bands) {
            val grow = 0.6f + 1.1f * i / bands                   // calmer under the clock, livelier below
            val amp = s.f(14f, 30f) * grow
            val length = s.f(140f, 260f)
            val phase = s.f(0f, TAU2)
            val y0 = top + i * gap
            val path = Path()
            var x = -thick
            while (x <= s.w + thick) {
                val y = y0 + sin(x / length * TAU2 + phase) * amp
                if (x == -thick) path.moveTo(x, y) else path.lineTo(x, y)
                x += 6f
            }
            s.canvas.drawPath(path, s.stroke(t.pick(o + i, 40 + (i * 11) % 45), thick))
        }
    }
}

/** A garden of the library's shapes at different sizes, clear of the clock. */
object M3Garden : Style("m3_garden", "Shape Garden", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val placed = ArrayList<FloatArray>()
        val base = floatArrayOf(250f, 210f, 160f, 150f, 120f, 110f, 90f, 80f, 70f, 60f, 50f, 46f, 40f, 34f)
        val extra = FloatArray(s.count(14) * 2) { 24f + (it % 7) * 5f }
        val sizes = if (s.density >= 1f) base + extra.take(((s.density - 1f) * 26f).toInt().coerceAtLeast(0)) else base.take(max(5, (base.size * s.density).toInt())).toFloatArray()
        for ((idx, r) in sizes.withIndex()) {
            var tries = 0
            while (tries++ < 90) {
                val x = s.f(r * 0.3f, s.w - r * 0.3f)
                val y = s.f(s.h * 0.3f + r * 0.3f, s.h - r * 0.2f)
                if (placed.none { (x - it[0]) * (x - it[0]) + (y - it[1]) * (y - it[1]) < (r + it[2]) * (r + it[2]) * 0.86f }) {
                    placed += floatArrayOf(x, y, r)
                    val rot = s.f(0f, TAU2)
                    val shape = when (s.i(0, 6)) {
                        0 -> M3Shapes.flower(x, y, r, s.i(5, 9), 0.26f, rot)
                        1 -> M3Shapes.cookie(x, y, r, s.i(7, 11), 0.16f, rot)
                        2 -> M3Shapes.burst(x, y, r, s.i(8, 13), 0.24f, rot)
                        3 -> M3Shapes.flower(x, y, r, 4, 0.42f, rot)
                        4 -> M3Shapes.squircle(x, y, r, rot)
                        else -> M3Shapes.circle(x, y, r)
                    }
                    s.tonal(shape, t.pick(idx + s.i(0, 3), if (r > 100f) 35 + s.i(0, 20) else 55 + s.i(0, 30)))
                    break
                }
            }
        }
    }
}

/** Rounded pills stacked on a slant, like a column of Material chips. */
object M3Pills : Style("m3_pills", "Pills", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val thick = s.f(120f, 170f) / (0.7f + 0.3f * s.density)
        val gap = thick * 0.22f
        val o = s.i(0, 3)
        s.canvas.save()
        s.canvas.rotate(s.f(-24f, -12f), s.cx, s.h * 0.62f)
        var y = s.h * 0.3f
        var row = 0
        while (y < s.h * 1.05f) {
            var x = s.f(-260f, -120f)
            while (x < s.w + 200f) {
                val len = s.f(thick * 1.3f, thick * 3.2f)
                s.tonal(M3Shapes.pill(x + len / 2f, y, len, thick), t.pick(o + row + s.i(0, 3), 30 + s.i(0, 5) * 10))
                x += len + gap
            }
            y += thick + gap
            row++
        }
        s.canvas.restore()
    }
}

/** Material's circular progress rings, thick and round-capped, stacked around one centre. */
object M3Orbit : Style("m3_orbit", "Orbit", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val cx = s.cx + s.f(-100f, 100f)
        val cy = s.h * s.f(0.6f, 0.7f)
        val thick = s.f(38f, 56f)
        val gap = thick * s.f(0.55f, 0.8f)
        val rings = s.i(5, 8) + ((s.density - 1f) * 4f).toInt()
        val o = s.i(0, 3)
        for (i in 0 until rings) {
            val r = thick * 1.6f + i * (thick + gap)
            val paint = s.stroke(t.pick(o + i, 80 - i * 7), thick).apply { strokeCap = Paint.Cap.ROUND }
            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            val start = s.f(0f, 360f)
            val sweep = s.f(190f, 320f)
            // The unfilled part of each ring is its track: the same colour, much darker.
            s.canvas.drawArc(rect, 0f, 360f, false, s.stroke(t.pick(o + i, 14), thick))
            s.canvas.drawArc(rect, start, sweep, false, paint)
        }
    }
}

/** Concentric arches, like a rainbow built from Material's tonal steps. */
object M3Arches : Style("m3_arches", "Arches", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val cx = s.cx + s.f(-60f, 60f)
        val baseY = s.h * s.f(0.78f, 0.9f)
        val n = s.i(6, 9) + ((s.density - 1f) * 5f).toInt()
        val outer = s.w * s.f(0.55f, 0.62f)
        val o = s.i(0, 3)
        for (i in 0 until n) {
            val r = outer * (1f - i / n.toFloat())
            val path = Path().apply {
                moveTo(cx - r, baseY)
                lineTo(cx - r, baseY - r)
                arcTo(RectF(cx - r, baseY - 2f * r, cx + r, baseY), 180f, 180f)
                lineTo(cx + r, baseY)
                close()
            }
            s.tonal(path, t.pick(o + i, 30 + (i * 60 / n)))
        }
    }
}

/** A grid of dots whose size swells along a wave: the Material expressive dot field. */
object M3Dots : Style("m3_dots", "Dot Field", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val step = 70f / sqrt(s.density.coerceIn(0.5f, 2f))
        val kx = s.f(0.004f, 0.01f)
        val ky = s.f(0.004f, 0.01f)
        val ph = s.f(0f, TAU2)
        val o = s.i(0, 3)
        var y = s.h * 0.3f
        var row = 0
        while (y < s.h + step) {
            var x = if (row % 2 == 0) 0f else step / 2f
            while (x < s.w + step) {
                val v = (sin(x * kx + ph) + sin(y * ky - ph * 0.7f) + 2f) / 4f
                val r = step * 0.46f * v.pow(1.3f)
                if (r > 2f) s.canvas.drawCircle(x, y, r, s.fill(t.pick(o + (v * 3f).toInt(), 40 + (v * 50f).toInt())))
                x += step
            }
            y += step * 0.87f
            row++
        }
    }
}

/** A sunburst of rounded rays: the Material burst shape, repeated and shrinking toward the centre. */
object M3Sunburst : Style("m3_sunburst", "Sunburst", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val cx = s.cx
        val cy = s.h * s.f(0.6f, 0.7f)
        val layers = s.i(4, 6) + ((s.density - 1f) * 3f).toInt()
        val r0 = s.w * s.f(0.55f, 0.65f)
        val rays = s.i(10, 18)
        val o = s.i(0, 3)
        for (i in 0 until layers) {
            val r = r0 * (1f - i / layers.toFloat() * 0.85f)
            s.tonal(M3Shapes.burst(cx, cy, r, rays + i * 2, 0.22f - i * 0.02f, i * 0.25f), t.pick(o + i, 28 + i * 50 / layers))
        }
        s.tonal(M3Shapes.circle(cx, cy, r0 * 0.07f), t.pick(o, 92))
    }
}

/** A grid of tiles morphing from circle to squircle to cookie to flower across the page. */
object M3Morph : Style("m3_morph", "Morph Grid", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val cols = (4 + (s.density * 1.5f).toInt()).coerceIn(3, 9)
        val cell = s.w / cols
        val rowsN = ((s.h * 0.72f) / cell).toInt() + 1
        val top = s.h * 0.28f
        val o = s.i(0, 3)
        for (gy in 0 until rowsN) for (gx in 0 until cols) {
            val u = (gx / (cols - 1f) + gy / (rowsN - 1f).coerceAtLeast(1f)) / 2f
            val cx = cell * (gx + 0.5f)
            val cy = top + cell * (gy + 0.5f)
            val r = cell * 0.43f
            val shape = when ((u * 4f).toInt().coerceIn(0, 3)) {
                0 -> M3Shapes.circle(cx, cy, r)
                1 -> M3Shapes.squircle(cx, cy, r)
                2 -> M3Shapes.cookie(cx, cy, r, 8, 0.16f, 0f)
                else -> M3Shapes.flower(cx, cy, r, 6, 0.28f, 0f)
            }
            s.tonal(shape, t.pick(o + gx + gy, 34 + (u * 50f).toInt()))
        }
    }
}

/** Soft organic blobs overlapping in tonal layers, like the abstract art of Material illustrations. */
object M3Blobs : Style("m3_blobs", "Blobs", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val n = s.count(7)
        val o = s.i(0, 3)
        for (i in 0 until n) {
            val cx = s.f(s.w * 0.1f, s.w * 0.9f)
            val cy = s.f(s.h * 0.34f, s.h * 0.95f)
            val r = s.f(120f, 300f) / (0.8f + 0.2f * s.density)
            val lobes = s.i(3, 6)
            val rot = s.f(0f, TAU2)
            val path = Path()
            val ph = FloatArray(3) { s.f(0f, TAU2) }
            for (k in 0..180) {
                val a = TAU2 * k / 180f
                val rr = r * (1f + 0.18f * sin(lobes * a + ph[0]) + 0.1f * sin((lobes + 2) * a + ph[1]) + 0.05f * sin((lobes + 5) * a + ph[2]))
                val x = cx + cos(a + rot) * rr
                val y = cy + sin(a + rot) * rr
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            s.tonal(path, t.pick(o + i, 26 + s.i(0, 5) * 12))
        }
    }
}

/** Thick rounded ribbons sweeping across the page in different tones, the way Material backgrounds flow. */
object M3Ribbons : Style("m3_ribbons", "Flow", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val n = s.count(5) + 1
        val thick = s.f(90f, 160f) / (0.75f + 0.25f * s.density)
        val o = s.i(0, 3)
        for (i in 0 until n) {
            val y0 = s.h * (0.34f + 0.62f * i / n)
            val amp = s.f(60f, 190f)
            val k = s.f(0.003f, 0.007f)
            val ph = s.f(0f, TAU2)
            val tilt = s.f(-0.25f, 0.25f)
            val path = Path()
            var x = -thick
            while (x <= s.w + thick) {
                val y = y0 + sin(x * k + ph) * amp + x * tilt
                if (x == -thick) path.moveTo(x, y) else path.lineTo(x, y)
                x += 8f
            }
            s.canvas.drawPath(path, s.stroke(t.pick(o + i, 32 + (i * 53 / n)), thick))
        }
    }
}

/** A tall stack of squircles that shrink and rotate, like a Material component exploded into layers. */
object M3Stack : Style("m3_stack", "Stack", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val n = s.i(6, 9) + ((s.density - 1f) * 5f).toInt()
        val cx = s.cx
        val cy = s.h * s.f(0.6f, 0.68f)
        val r0 = s.w * s.f(0.48f, 0.56f)
        val twist = s.f(0.12f, 0.3f)
        val o = s.i(0, 3)
        for (i in 0 until n) {
            val k = i / n.toFloat()
            s.tonal(M3Shapes.squircle(cx + sin(i * 0.7f) * 24f, cy - i * 14f, r0 * (1f - k * 0.88f), i * twist, 3.4f + k * 2f), t.pick(o + i, 26 + (k * 62f).toInt()))
        }
    }
}

object MaterialYou {
    val styles: List<Style> = listOf(
        M3Bloom, M3Cookies, M3Waves, M3Garden, M3Pills, M3Orbit,
        M3Arches, M3Dots, M3Sunburst, M3Morph, M3Blobs, M3Ribbons, M3Stack,
    )
}
