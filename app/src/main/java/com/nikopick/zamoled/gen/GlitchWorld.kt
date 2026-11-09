package com.nikopick.zamoled.gen

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** The Glitch world: corrupted signal, misprinted comics and a city that is coming apart. */

/** Draws into a transparent layer the same size as the picture, so it can be torn up afterwards. */
private inline fun Scene.layer(draw: (Scene) -> Unit): Bitmap {
    val bmp = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val k = canvas.width / Renderer.VIRTUAL_WIDTH
    c.scale(k, k)
    draw(Scene(c, w, h, rnd, palette, density, noise, layout))
    return bmp
}

private val RED = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
private val GREEN = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
private val BLUE = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)

/**
 * Lays [bmp] on the page torn into horizontal strips, some shifted sideways, with the red, green
 * and blue channels pulled apart by [split] units: the look of a signal failing.
 */
private fun Scene.glitchComposite(bmp: Bitmap, split: Float, tears: Int, maxShift: Float) {
    val paints = listOf(RED, GREEN, BLUE).map { m ->
        Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(m))
            blendMode = BlendMode.PLUS
        }
    }
    val sx = bmp.width / w
    val sy = bmp.height / h
    var y = 0f
    val tearAt = BooleanArray(1000)
    repeat(tears) { tearAt[i(0, 1000)] = true }
    while (y < h) {
        val band = if (chance(0.5f)) f(h * 0.004f, h * 0.02f) else f(h * 0.03f, h * 0.09f)
        val y1 = min(h, y + band)
        val shift = if (tearAt[(y / h * 999f).toInt().coerceIn(0, 999)] || chance(tears / 90f)) f(-maxShift, maxShift) else 0f
        val src = Rect(0, (y * sy).toInt(), bmp.width, (y1 * sy).toInt().coerceAtLeast((y * sy).toInt() + 1))
        for ((k, p) in paints.withIndex()) {
            val d = shift + (k - 1) * split
            canvas.drawBitmap(bmp, src, RectF(d, y, w + d, y1), p)
        }
        y = y1
    }
    bmp.recycle()
}

// ---------------------------------------------------------------------------------------------

/** Characters falling in columns. Six looks, and Detail packs more of them onto the screen. */
object DataRain : Style("data_rain", "Data Rain", Category.GLITCH) {
    override val autoPalettes = listOf("aurora", "acid", "cyan", "violet", "lava", "mono", "graffiti")
    private const val KANA = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロ0123456789"

    /** One field of falling columns. [activity] is the chance a column is raining at density 1. */
    private fun Scene.columns(
        glyphs: String, size: Float, activity: Float, lenMin: Int, lenMax: Int,
        tint: (Float) -> Int, alphaScale: Float = 1f, sway: Float = 0f, glow: Boolean = true,
    ) {
        // More detail means smaller type, more columns, and longer tails.
        val sz = size / (0.75f + 0.25f * density.coerceIn(0.4f, 1.8f))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sz
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val chance = (activity * Math.pow(density.toDouble(), 0.85).toFloat()).coerceIn(0.05f, 0.98f)
        var x = sz / 2f
        while (x < w) {
            if (chance(chance)) {
                val len = (i(lenMin, lenMax) * (0.65f + 0.45f * density)).toInt().coerceAtLeast(4)
                val headY = f(0f, h + len * sz)
                val color = tint(x / w)
                val phase = f(0f, (2 * PI).toFloat())
                for (k in 0 until len) {
                    val y = headY - k * sz
                    if (y < -sz || y > h + sz) continue
                    val ch = glyphs[i(0, glyphs.length)].toString()
                    val px = x + sin(y * 0.01f + phase) * sway
                    if (k == 0) {
                        paint.color = Color.WHITE
                        paint.alpha = (255 * alphaScale).toInt()
                        if (glow) paint.setShadowLayer(sz * 0.5f, 0f, 0f, color) else paint.clearShadowLayer()
                    } else {
                        paint.color = if (k < 3) lighten(color, 0.4f) else color
                        paint.alpha = ((230 * (1f - k / len.toFloat())).toInt().coerceAtLeast(18) * alphaScale).toInt()
                        paint.clearShadowLayer()
                    }
                    canvas.drawText(ch, px, y, paint)
                }
            }
            x += sz * 1.08f
        }
    }

    override fun draw(s: Scene) {
        val c0 = s.color(0)
        val c1 = s.color(1)
        when (s.i(0, 6)) {
            // The classic: kana and digits, white heads, a tail that fades.
            0 -> s.columns(KANA, 26f, 0.7f, 10, 40, { s.grad(it) })
            // Binary: two characters, one tint, long unbroken streams.
            1 -> s.columns("01", 30f, 0.65f, 16, 52, { c0 })
            // Hex: the digits of a dump, falling.
            2 -> s.columns("0123456789ABCDEF", 24f, 0.7f, 10, 36, { lighten(s.grad(it), 0.1f) })
            // Depth: three planes, small and dim at the back, large and bright in front.
            3 -> {
                s.columns(KANA, 14f, 0.8f, 12, 46, { darken(c0, 0.35f) }, 0.55f, glow = false)
                s.columns(KANA, 26f, 0.5f, 10, 34, { s.grad(it) }, 0.9f)
                s.columns("01", 54f, 0.22f, 5, 16, { lighten(c1, 0.2f) }, 1f)
            }
            // Pixel snow: square flakes falling in streaks, torn sideways in places.
            4 -> {
                val n = s.count(260)
                repeat(n) {
                    val x = s.f(0f, s.w)
                    val y = s.f(0f, s.h)
                    val sz = s.f(3f, 13f)
                    val col = s.grad(s.f())
                    val streak = s.f(20f, 160f)
                    s.canvas.drawRect(x, y - streak, x + sz, y, s.fill(col, s.i(15, 60)))
                    s.canvas.drawRect(x, y, x + sz, y + sz, s.fill(if (s.chance(0.2f)) Color.WHITE else col, s.i(160, 255)))
                }
                repeat(s.count(7)) {
                    val y = s.f(0f, s.h)
                    s.canvas.drawRect(s.f(0f, s.w * 0.4f), y, s.f(s.w * 0.5f, s.w), y + s.f(2f, 9f), s.fill(s.grad(s.f()), s.i(40, 110)))
                }
            }
            // Cascade: the columns lean and sway, the tail running from bright to the second colour.
            else -> s.columns(KANA, 24f, 0.65f, 12, 42, { mix(c0, c1, it) }, sway = 16f)
        }
        // Past normal detail, another plane of small, dim characters falls behind the rest.
        if (s.density > 1.1f) s.columns(KANA, 13f, (s.density - 1f) * 0.9f, 10, 40, { darken(s.grad(it), 0.4f) }, 0.5f, glow = false)
    }
}

// ---------------------------------------------------------------------------------------------

/**
 * Comic-book dimensions colliding: halftone dots, a starburst, speed lines, a shouted word, and
 * the printing plates knocked out of register, so every edge splits into cyan, magenta and yellow.
 */
object DimensionSplit : Style("dimension_split", "Dimension Split", Category.GLITCH) {
    override val autoPalettes = listOf("graffiti", "candy", "cyan", "hue", "sakura")
    private val shouts = arrayOf("ZAP!", "BAM!", "WHAM!", "GLITCH!", "CRACK!", "WOW!", "SNAP!")

    override fun draw(s: Scene) {
        val c0 = s.color(0)
        val c1 = s.color(1)
        val c2 = s.color(2)
        val cx = s.cx + s.f(-120f, 120f)
        val cy = s.h * s.f(0.36f, 0.48f)
        val radius = s.w * s.f(0.3f, 0.4f)
        val word = shouts[s.i(0, shouts.size)]
        val bmp = s.layer { l ->
            // Ben-Day dots filling one corner, fading out as they run across the page.
            val step = 24f / sqrt(l.density.coerceIn(0.5f, 1.8f))
            var gy = 0
            var y = 0f
            while (y < l.h) {
                var x = if (gy % 2 == 0) 0f else step / 2f
                while (x < l.w) {
                    val t = (1f - (x / l.w * 0.6f + y / l.h * 0.55f)).coerceIn(0f, 1f)
                    if (t > 0.05f) l.canvas.drawCircle(x, y, step * 0.5f * t, l.fill(c2, 220))
                    x += step
                }
                y += step * 0.866f
                gy++
            }
            // Speed lines out from behind the burst.
            repeat(l.count(70)) {
                val a = l.f(0f, (2 * PI).toFloat())
                val r0 = radius * l.f(1.05f, 1.5f)
                val r1 = radius * l.f(1.7f, 3.4f)
                l.canvas.drawLine(cx + cos(a) * r0, cy + sin(a) * r0, cx + cos(a) * r1, cy + sin(a) * r1, l.stroke(Color.WHITE, l.f(2f, 8f), l.i(70, 230)))
            }
            // The starburst.
            val burst = Path()
            val spikes = 22
            for (k in 0 until spikes * 2) {
                val r = radius * if (k % 2 == 0) l.f(1.15f, 1.4f) else 0.95f
                val a = (PI * k / spikes).toFloat()
                if (k == 0) burst.moveTo(cx + cos(a) * r, cy + sin(a) * r) else burst.lineTo(cx + cos(a) * r, cy + sin(a) * r)
            }
            burst.close()
            l.canvas.drawPath(burst, l.fill(c1))
            l.canvas.drawPath(burst, l.stroke(Color.WHITE, 7f))
            // A halftone sphere in the middle: dot size follows the light.
            val ds = 17f
            var by = cy - radius * 0.85f
            var row = 0
            while (by < cy + radius * 0.85f) {
                var bx = cx - radius * 0.85f + if (row % 2 == 0) 0f else ds / 2f
                while (bx < cx + radius * 0.85f) {
                    val dx = (bx - cx) / (radius * 0.85f)
                    val dy = (by - cy) / (radius * 0.85f)
                    val d = dx * dx + dy * dy
                    if (d < 1f) {
                        val shade = (0.5f - dx * 0.35f - dy * 0.4f + sqrt(1f - d) * 0.35f).coerceIn(0f, 1f)
                        l.canvas.drawCircle(bx, by, ds * 0.5f * shade, l.fill(Color.WHITE))
                    }
                    bx += ds
                }
                by += ds * 0.866f
                row++
            }
            l.canvas.drawCircle(cx, cy, radius * 0.85f, l.stroke(Color.WHITE, 6f))
            // The shout.
            val tp = l.textPaint(min(radius * 0.62f, l.w * 1.15f / word.length), Color.WHITE, Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC))
            l.canvas.save()
            l.canvas.rotate(l.f(-14f, 8f), cx, cy + radius * 1.55f)
            l.canvas.drawText(word, cx + 7f, cy + radius * 1.62f + 7f, Paint(tp).apply { color = c0 })
            l.canvas.drawText(word, cx, cy + radius * 1.62f, tp)
            l.canvas.restore()
        }
        s.glitchComposite(bmp, split = 13f, tears = 3 + (s.density * 5f).toInt(), maxShift = 80f)
    }
}

// ---------------------------------------------------------------------------------------------

/** A megacity in the rain: towers edged in neon, glitching billboards, a targeting HUD and scanlines. */
object NeonDystopia : Style("neon_dystopia", "Neon Dystopia", Category.GLITCH) {
    override val autoPalettes = listOf("graffiti", "cyan", "violet", "lava", "sakura", "hue")
    private val signs = arrayOf("SYSTEM FAILURE", "NO SIGNAL", "404", "DREAM.EXE", "OVERRIDE", "LOGIN", "SECTOR 7")

    override fun draw(s: Scene) {
        val c0 = s.color(0)
        val c1 = s.color(1)
        val c2 = s.color(2)
        val sign = signs[s.i(0, signs.size)]
        val bmp = s.layer { l ->
            val horizon = l.h * l.f(0.72f, 0.82f)
            // The sky glows behind the city, so the towers read as black slabs against it.
            l.softGlow(l.w * l.f(0.3f, 0.7f), horizon - l.h * 0.12f, l.w * 0.7f, c1, 110)
            l.softGlow(l.w * l.f(0.2f, 0.8f), horizon - l.h * 0.05f, l.w * 0.5f, c0, 80)
            // Towers, far to near: each a dark slab edged in neon, with lit windows.
            for (layer in 0..2) {
                val depth = layer / 2f
                var x = -20f
                while (x < l.w) {
                    val wd = l.f(50f, 130f) * (0.7f + 0.6f * depth)
                    val ht = l.f(l.h * 0.12f, l.h * 0.5f) * (0.55f + 0.6f * depth)
                    val top = horizon - ht
                    val edge = l.color(l.i(0, 3))
                    l.canvas.drawRect(x, top, x + wd, l.h, l.fill(Color.BLACK))
                    l.canvas.drawRect(x, top, x + wd, l.h, l.stroke(edge, 2f + 1.4f * depth, (150 + 100 * depth).toInt(), 4f))
                    val chance = 0.26f * l.density.coerceIn(0.4f, 1.8f)
                    var wy = top + 14f
                    while (wy < horizon - 8f) {
                        var wx = x + 8f
                        while (wx < x + wd - 8f) {
                            if (l.chance(chance)) l.canvas.drawRect(wx, wy, wx + 6f, wy + 11f, l.fill(if (l.chance(0.15f)) Color.WHITE else lighten(l.color(l.i(0, 3)), 0.25f), (170 + 85 * depth).toInt()))
                            wx += 13f
                        }
                        wy += 20f
                    }
                    x += wd + l.f(-10f, 24f)
                }
            }
            // Billboards, each one failing in its own way.
            repeat(l.count(2) + 1) {
                val bw = l.f(260f, 430f)
                val bh = l.f(150f, 270f)
                val bx = l.f(20f, l.w - bw - 20f)
                val by = l.f(l.h * 0.2f, l.h * 0.55f)
                val col = l.color(l.i(0, 3))
                l.canvas.drawRect(bx, by, bx + bw, by + bh, l.vGradient(by, by + bh, withAlpha(col, 200), withAlpha(darken(col, 0.6f), 120)))
                l.canvas.drawRect(bx, by, bx + bw, by + bh, l.stroke(lighten(col, 0.5f), 2.4f))
                repeat(7) {
                    val gy = l.f(by, by + bh)
                    l.canvas.drawRect(bx, gy, bx + bw * l.f(0.2f, 1f), gy + l.f(2f, 9f), l.fill(if (l.chance(0.5f)) Color.BLACK else Color.WHITE, 120))
                }
                l.canvas.drawText(sign, bx + bw / 2f, by + bh / 2f + 10f, l.textPaint(bh * 0.18f, Color.WHITE, Typeface.MONOSPACE))
            }
            // Rain, slanting.
            repeat(l.count(230)) {
                val rx = l.f(-60f, l.w)
                val ry = l.f(0f, l.h)
                l.canvas.drawLine(rx, ry, rx + 16f, ry + 54f, l.stroke(Color.WHITE, 1.2f, l.i(20, 80)))
            }
            // Targeting HUD: corner brackets, a reticle, a column of readouts and bars.
            val m = 46f
            val len = 80f
            for ((px, py, dx, dy) in listOf(floatArrayOf(m, m, 1f, 1f), floatArrayOf(l.w - m, m, -1f, 1f), floatArrayOf(m, l.h - m, 1f, -1f), floatArrayOf(l.w - m, l.h - m, -1f, -1f)).map { it.toList() }) {
                l.canvas.drawLine(px, py, px + dx * len, py, l.stroke(c0, 3f))
                l.canvas.drawLine(px, py, px, py + dy * len, l.stroke(c0, 3f))
            }
            val rx = l.cx + l.f(-100f, 100f)
            val ry = l.h * l.f(0.3f, 0.5f)
            l.canvas.drawCircle(rx, ry, 120f, l.stroke(c1, 2.4f, 200))
            l.canvas.drawCircle(rx, ry, 70f, l.stroke(c1, 1.4f, 140))
            for (k in 0 until 4) {
                val a = (PI / 2 * k).toFloat()
                l.canvas.drawLine(rx + cos(a) * 100f, ry + sin(a) * 100f, rx + cos(a) * 150f, ry + sin(a) * 150f, l.stroke(c1, 2.4f))
            }
            val tp = l.textPaint(22f, c2, Typeface.MONOSPACE, Paint.Align.LEFT)
            repeat(l.count(7)) { k -> l.canvas.drawText("ERR 0x%04X // %s".format(l.i(0, 65535), if (l.chance(0.5f)) "LINK LOST" else "RETRY"), m + 10f, m + 130f + k * 30f, tp) }
            repeat(4) { k ->
                val by = l.h - m - 40f - k * 26f
                l.canvas.drawRect(l.w - m - 220f, by, l.w - m, by + 12f, l.stroke(c0, 1.6f, 180))
                l.canvas.drawRect(l.w - m - 220f, by, l.w - m - 220f + 220f * l.f(0.15f, 1f), by + 12f, l.fill(c0, 200))
            }
        }
        s.glitchComposite(bmp, split = 7f, tears = 2 + (s.density * 4f).toInt(), maxShift = 46f)
        // Scanlines, drawn last, thin and dark.
        var y = 0f
        val line = s.fill(Color.BLACK, 70)
        while (y < s.h) {
            s.canvas.drawRect(0f, y, s.w, y + 2.2f, line)
            y += 6f
        }
    }
}

object GlitchWorld {
    val styles: List<Style> = listOf(DimensionSplit, NeonDystopia)
}
