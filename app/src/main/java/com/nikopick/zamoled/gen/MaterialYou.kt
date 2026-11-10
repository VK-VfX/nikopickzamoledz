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


/** A column of Material components in tonal colour: card, switches, slider, chips, buttons, progress. */
object M3Components : Style("m3_components", "Components", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val o = s.i(0, 3)
        val k = 1f / sqrt(s.density.coerceIn(0.5f, 1.6f))      // more detail packs the components closer
        val left = 70f
        val right = s.w - 70f
        val width = right - left
        var y = s.h * 0.3f
        val order = (0 until 7).shuffled(s.rnd)
        var n = 0
        while (y < s.h - 60f) {
            val gap = 54f * k
            val used = when (order[n++ % order.size]) {
                0 -> {                                                        // list card
                    val h = 220f * k
                    s.tonal(rrectPath(left, y, right, y + h, 56f * k), t.pick(o, 24))
                    s.tonal(M3Shapes.circle(left + 100f * k, y + h / 2f, 56f * k), t.pick(o + 1, 72))
                    s.tonal(M3Shapes.pill(left + 330f * k + 120f * k, y + h / 2f - 26f * k, 340f * k, 28f * k), t.pick(o, 90))
                    s.tonal(M3Shapes.pill(left + 330f * k + 70f * k, y + h / 2f + 24f * k, 240f * k, 20f * k), t.pick(o, 58))
                    s.tonal(M3Shapes.circle(right - 80f * k, y + h / 2f, 44f * k), t.pick(o + 2, 76))
                    h
                }
                1 -> {                                                        // switches
                    val h = 96f * k
                    s.tonal(M3Shapes.pill(left + 90f * k, y + h / 2f, 170f * k, h), t.pick(o, 72))
                    s.tonal(M3Shapes.circle(left + 130f * k, y + h / 2f, 34f * k), t.pick(o, 14))
                    s.canvas.drawPath(M3Shapes.pill(left + 330f * k, y + h / 2f, 170f * k, h), s.stroke(t.p(46), 5f))
                    s.tonal(M3Shapes.circle(left + 292f * k, y + h / 2f, 24f * k), t.p(56))
                    s.tonal(M3Shapes.pill(left + 600f * k, y + h / 2f, 170f * k, h), t.pick(o + 1, 72))
                    s.tonal(M3Shapes.circle(left + 640f * k, y + h / 2f, 34f * k), t.pick(o + 1, 14))
                    h
                }
                2 -> {                                                        // slider
                    val h = 70f * k
                    val cy = y + h / 2f
                    s.tonal(M3Shapes.pill(left + width / 2f, cy, width, 18f * k), t.pick(o, 26))
                    val fill = width * s.f(0.3f, 0.75f)
                    s.tonal(M3Shapes.pill(left + fill / 2f, cy, fill, 18f * k), t.pick(o, 76))
                    s.tonal(M3Shapes.pill(left + fill, cy, 14f * k, 62f * k), t.pick(o, 88))
                    h
                }
                3 -> {                                                        // chips
                    val h = 84f * k
                    var x = left
                    for (c in 0 until 4) {
                        val w = s.f(150f, 230f) * k
                        if (x + w > right) break
                        val path = rrectPath(x, y, x + w, y + h, 26f * k)
                        if (c == 0) s.tonal(path, t.pick(o, 72)) else s.canvas.drawPath(path, s.stroke(t.pick(o + c, 44), 5f))
                        s.tonal(M3Shapes.pill(x + w / 2f, y + h / 2f, w * 0.5f, 16f * k), if (c == 0) t.pick(o, 14) else t.pick(o + c, 70))
                        x += w + 26f * k
                    }
                    h
                }
                4 -> {                                                        // buttons
                    val h = 100f * k
                    s.tonal(M3Shapes.pill(left + 170f * k, y + h / 2f, 340f * k, h), t.pick(o, 80))
                    s.tonal(M3Shapes.pill(left + 220f * k + 340f * k, y + h / 2f, 280f * k, h), t.pick(o + 1, 30))
                    s.tonal(M3Shapes.circle(right - 60f * k, y + h / 2f, h / 2f), t.pick(o + 2, 72))
                    h
                }
                5 -> {                                                        // wavy progress
                    val h = 70f * k
                    val cy = y + h / 2f
                    val path = Path()
                    var x = left
                    while (x <= right) {
                        val yy = cy + sin((x - left) / 46f) * 14f
                        if (x == left) path.moveTo(x, yy) else path.lineTo(x, yy)
                        x += 6f
                    }
                    s.canvas.drawPath(path, s.stroke(t.pick(o, 76), 20f * k))
                    s.canvas.drawLine(left + width * 0.62f, cy, right, cy, s.stroke(t.pick(o, 28), 20f * k))
                    h
                }
                else -> {                                                     // FAB and radio buttons
                    val h = 170f * k
                    s.tonal(rrectPath(left, y, left + h, y + h, 52f * k), t.pick(o + 1, 74))
                    s.tonal(rrectPath(left + h / 2f - 8f * k, y + h * 0.28f, left + h / 2f + 8f * k, y + h * 0.72f, 6f * k), t.pick(o + 1, 14))
                    s.tonal(rrectPath(left + h * 0.28f, y + h / 2f - 8f * k, left + h * 0.72f, y + h / 2f + 8f * k, 6f * k), t.pick(o + 1, 14))
                    for (c in 0..2) {
                        val cx = left + h + 120f * k + c * 130f * k
                        s.canvas.drawCircle(cx, y + h / 2f, 40f * k, s.stroke(t.pick(o + c, if (c == 0) 78 else 46), 6f))
                        if (c == 0) s.tonal(M3Shapes.circle(cx, y + h / 2f, 20f * k), t.pick(o, 78))
                    }
                    h
                }
            }
            y += used + gap
        }
    }
}

/** Soft glowing orbs in tonal colour, overlapping where they meet: the look of a Pixel gradient wallpaper. */
object M3Aura : Style("m3_aura", "Aura", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val o = s.i(0, 3)
        val n = s.count(4) + 1
        for (i in 0 until n) {
            val r = s.f(240f, 520f)
            val x = s.f(s.w * 0.05f, s.w * 0.95f)
            val y = s.f(s.h * 0.38f, s.h * 0.98f)
            val core = t.pick(o + i, 62 + s.i(0, 20))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.RadialGradient(
                    x, y, r,
                    intArrayOf(withAlpha(core, 230), withAlpha(tone(core, 40), 110), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
                )
                blendMode = android.graphics.BlendMode.PLUS
            }
            s.canvas.drawCircle(x, y, r, paint)
        }
        // A thin ring around the brightest orb, the way Material marks focus.
        s.canvas.drawCircle(s.cx, s.h * 0.66f, s.w * 0.36f, s.stroke(withAlpha(t.p(80), 50), 2f))
    }
}

/** Three big circles in the palette's three colours, mixing where they overlap. */
object M3Overlap : Style("m3_overlap", "Overlap", Category.MATERIAL) {
    override val autoPalettes = listOf("you")

    override fun draw(s: Scene) {
        val t = Tonal(s)
        val cx = s.cx
        val cy = s.h * s.f(0.6f, 0.68f)
        val r = s.w * s.f(0.3f, 0.36f)
        val spread = r * s.f(0.62f, 0.82f)
        val rot = s.f(0f, TAU2)
        val extra = s.count(2) - 2
        for (ring in 0..extra) {
            val k = 1f - ring * 0.24f
            for (i in 0 until 3) {
                val a = rot + i * TAU2 / 3f
                val x = cx + cos(a) * spread * k
                val y = cy + sin(a) * spread * k
                val col = t.pick(i, 60)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = withAlpha(col, if (ring == 0) 235 else 120)
                    blendMode = android.graphics.BlendMode.PLUS
                }
                s.canvas.drawCircle(x, y, r * k, paint)
            }
        }
        for (i in 0 until 3) {
            val a = rot + i * TAU2 / 3f
            s.canvas.drawCircle(cx + cos(a) * spread, cy + sin(a) * spread, r * 1.08f, s.stroke(withAlpha(t.pick(i, 80), 70), 2f))
        }
    }
}

private fun rrectPath(l: Float, t: Float, r: Float, b: Float, rad: Float): Path =
    Path().apply { addRoundRect(RectF(l, t, r, b), rad, rad, Path.Direction.CW) }

object MaterialYou {
    val styles: List<Style> = listOf(
        M3Bloom, M3Cookies, M3Waves, M3Pills, M3Arches, M3Blobs, M3Ribbons,
        M3Components, M3Aura, M3Overlap,
    )
}
