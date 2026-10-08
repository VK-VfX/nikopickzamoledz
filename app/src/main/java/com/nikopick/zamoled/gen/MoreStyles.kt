package com.nikopick.zamoled.gen

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ---------- Audio ----------

object SpectrumBars : Style("spectrum_bars", "Spectrum", Category.AUDIO) {
    override fun draw(s: Scene) {
        val n = s.count(44).coerceIn(12, 120)
        val gap = s.w / n
        val bw = gap * 0.56f
        val y0 = s.h * s.f(0.5f, 0.62f)
        val maxH = s.f(240f, 400f)
        val off = s.f(0f, 50f)
        for (i in 0 until n) {
            val x = gap * (i + 0.5f)
            val t = i / (n - 1f)
            val env = sin(PI.toFloat() * t).pow(0.8f)
            val v = ((s.noise.at(i * 0.21f + off, 3.3f) + 1f) / 2f).coerceIn(0f, 1f)
            val hgt = 10f + maxH * env * v.pow(1.4f)
            val color = s.grad(t)
            s.canvas.drawRoundRect(x - bw / 2f, y0 - hgt, x + bw / 2f, y0, bw / 2f, bw / 2f, s.fill(color, 70, 12f))
            s.canvas.drawRoundRect(x - bw / 2f, y0 - hgt, x + bw / 2f, y0, bw / 2f, bw / 2f, s.fill(color, 235))
            // Reflection under the baseline.
            s.canvas.drawRoundRect(x - bw / 2f, y0 + 8f, x + bw / 2f, y0 + 8f + hgt * 0.4f, bw / 2f, bw / 2f, s.fill(color, 45))
        }
    }
}

object CircularSpectrum : Style("circular_spectrum", "Radial Beat", Category.AUDIO) {
    override fun draw(s: Scene) {
        val x = s.cx
        val y = s.h * s.f(0.4f, 0.5f)
        val r = s.f(150f, 210f)
        val n = s.count(110).coerceIn(30, 240)
        val off = s.f(0f, 50f)
        for (j in 0 until n) {
            val t = j / n.toFloat()
            val a = t * TAU - PI.toFloat() / 2f
            // Mirror the noise so the ring joins seamlessly.
            val m = if (t < 0.5f) t else 1f - t
            val v = ((s.noise.at(m * 9f + off, 1.7f) + 1f) / 2f).coerceIn(0f, 1f)
            val len = 14f + v.pow(1.6f) * 190f
            val c = s.grad(m * 2f)
            s.canvas.drawLine(x + cos(a) * r, y + sin(a) * r, x + cos(a) * (r + len), y + sin(a) * (r + len), s.stroke(c, 4f, 230))
        }
        s.canvas.drawCircle(x, y, r - 14f, s.stroke(s.color(0), 6f, 120, 18f))
        s.canvas.drawCircle(x, y, r - 14f, s.stroke(s.color(0), 2f))
        s.glowDot(x, y, 10f, s.color(1), 24f)
    }
}

object Oscilloscope : Style("oscilloscope", "Oscilloscope", Category.AUDIO) {
    override fun draw(s: Scene) {
        val grid = s.stroke(s.color(2), 1f, 28)
        var gx = 0f
        while (gx <= s.w) { s.canvas.drawLine(gx, 0f, gx, s.h, grid); gx += 100f }
        var gy = 0f
        while (gy <= s.h) { s.canvas.drawLine(0f, gy, s.w, gy, grid); gy += 100f }

        val a = s.i(1, 6)
        var b = s.i(1, 6)
        if (b == a) b = a + 1
        val delta = s.f(0f, PI.toFloat())
        val ax = s.f(300f, 420f)
        val ay = ax * s.f(0.9f, 1.4f)
        val cx = s.cx
        val cy = s.h * 0.45f
        repeat(s.count(4).coerceIn(1, 8)) { k ->
            val p = Path()
            val steps = 900
            val d = delta + k * 0.06f
            for (i in 0..steps) {
                val t = i / steps.toFloat() * TAU
                val px = cx + ax * sin(a * t + d)
                val py = cy + ay * sin(b * t)
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            s.glowPath(p, s.color(k), 2f, 10f, (255 * (1f - k * 0.2f)).toInt().coerceAtLeast(60))
        }
    }
}

// ---------- Mystic ----------

object FlowerOfLife : Style("flower_of_life", "Flower of Life", Category.MYSTIC) {
    override fun draw(s: Scene) {
        s.faintStars(140)
        val r = s.f(62f, 88f)
        val x0 = s.cx
        val y0 = s.h * s.f(0.4f, 0.5f)
        val h3 = sqrt(3f) / 2f
        for (q in -3..3) for (rr in -3..3) {
            val x = x0 + r * (q + rr * 0.5f)
            val y = y0 + r * rr * h3
            val d = hypot(x - x0, y - y0)
            if (d > 2f * r + 0.5f) continue
            val c = s.grad(d / (2f * r))
            s.canvas.drawCircle(x, y, r, s.stroke(c, 5f, 70, 12f))
            s.canvas.drawCircle(x, y, r, s.stroke(c, 1.8f, 230))
        }
        s.canvas.drawCircle(x0, y0, 3f * r, s.stroke(s.color(0), 2.2f))
        s.canvas.drawCircle(x0, y0, 3f * r + 10f, s.stroke(s.color(1), 1.2f, 140))
    }
}

object Metatron : Style("metatron", "Metatron", Category.MYSTIC) {
    override fun draw(s: Scene) {
        s.faintStars(120)
        val r = s.f(44f, 58f)
        val x0 = s.cx
        val y0 = s.h * s.f(0.4f, 0.5f)
        val pts = ArrayList<FloatArray>()
        pts.add(floatArrayOf(x0, y0))
        for (ring in 1..2) for (k in 0 until 6) {
            val a = Math.toRadians((-90 + 60 * k).toDouble()).toFloat()
            pts.add(floatArrayOf(x0 + cos(a) * r * 2f * ring, y0 + sin(a) * r * 2f * ring))
        }
        val line = s.stroke(s.color(1), 1.3f, 120)
        for (i in pts.indices) for (j in i + 1 until pts.size) {
            s.canvas.drawLine(pts[i][0], pts[i][1], pts[j][0], pts[j][1], line)
        }
        for ((i, p) in pts.withIndex()) {
            val c = if (i == 0) s.color(2) else s.color(0)
            s.canvas.drawCircle(p[0], p[1], r, s.stroke(c, 5f, 60, 12f))
            s.canvas.drawCircle(p[0], p[1], r, s.stroke(c, 1.8f))
        }
    }
}

object MoonPhases : Style("moon_phases", "Moon Phases", Category.MYSTIC) {
    override fun draw(s: Scene) {
        s.faintStars(200)
        val n = 7
        val arcR = s.f(560f, 700f)
        val acx = s.cx
        val acy = s.h * s.f(0.45f, 0.55f) + arcR * 0.75f
        val span = 70f
        val orbit = Path()
        orbit.addArc(acx - arcR, acy - arcR, acx + arcR, acy + arcR, -90f - span / 2f - 8f, span + 16f)
        s.canvas.drawPath(orbit, s.stroke(s.color(1), 1.2f, 70))
        val moonR = 34f
        for (k in 0 until n) {
            val deg = -90f - span / 2f + span * k / (n - 1f)
            val a = Math.toRadians(deg.toDouble()).toFloat()
            val x = acx + cos(a) * arcR
            val y = acy + sin(a) * arcR
            val phase = k / (n - 1f) // 0 new -> 0.5 full -> 1 new
            val lit = 1f - abs(phase - 0.5f) * 2f
            val color = s.color(0)
            s.canvas.drawCircle(x, y, moonR, s.stroke(color, 1.2f, 60))
            if (lit <= 0.02f) continue
            s.canvas.drawCircle(x, y, moonR, s.fill(color, 60, 14f))
            s.canvas.drawCircle(x, y, moonR, s.fill(color))
            if (lit < 0.98f) {
                // A black disc slides across to carve the crescent or gibbous shape.
                val shift = lit * 2f * moonR
                val dir = if (phase < 0.5f) -1f else 1f
                s.canvas.drawCircle(x + dir * shift, y, moonR + 0.5f, s.fill(Color.BLACK))
                s.canvas.drawCircle(x, y, moonR, s.stroke(color, 1.2f, 60))
            }
        }
    }
}

// ---------- City ----------

object Skyline : Style("skyline", "Skyline", Category.CITY) {
    override fun draw(s: Scene) {
        s.faintStars(150, s.h * 0.5f)
        s.glowDot(s.w * s.f(0.15f, 0.85f), s.h * s.f(0.12f, 0.25f), 36f, s.color(2), 30f, 220)
        for (layer in 0..1) {
            var x = -10f
            val base = s.h * (0.86f + layer * 0.04f)
            val outlineAlpha = if (layer == 0) 70 else 150
            while (x < s.w + 10f) {
                val bw = s.f(50f, 120f)
                val bh = s.f(160f, 560f) * (if (layer == 0) 1.15f else 0.8f)
                val top = base - bh
                s.canvas.drawRect(x, top, x + bw, s.h + 10f, s.fill(Color.BLACK))
                s.canvas.drawRect(x, top, x + bw, s.h + 10f, s.stroke(s.color(0), 1.4f, outlineAlpha))
                var wy = top + 14f
                while (wy < s.h - 10f) {
                    var wx = x + 10f
                    while (wx < x + bw - 14f) {
                        if (s.chance(if (layer == 0) 0.06f else 0.14f)) {
                            s.canvas.drawRect(wx, wy, wx + 7f, wy + 10f, s.fill(s.anyColor(), s.i(120, 240)))
                        }
                        wx += 15f
                    }
                    wy += 20f
                }
                if (layer == 1 && s.chance(0.25f)) {
                    val ax = x + bw / 2f
                    s.canvas.drawLine(ax, top, ax, top - 40f, s.stroke(s.color(0), 1.4f, 150))
                    s.glowDot(ax, top - 42f, 2.5f, Color.rgb(255, 64, 64), 8f)
                }
                x += bw + s.f(-6f, 14f)
            }
        }
    }
}

object NeonRain : Style("neon_rain", "Neon Rain", Category.CITY) {
    override fun draw(s: Scene) {
        val ground = s.h * s.f(0.72f, 0.8f)
        val tilt = s.f(-0.15f, 0.15f)
        repeat(s.count(260)) {
            val x = s.f(-50f, s.w + 50f)
            val y = s.f(0f, ground)
            val len = s.f(20f, 80f)
            s.canvas.drawLine(x, y, x + tilt * len, y + len, s.stroke(s.anyColor(), 1.1f, s.i(30, 150)))
        }
        repeat(s.i(3, 6)) { k ->
            val color = s.color(k)
            val w = s.f(80f, 220f)
            val hh = s.f(26f, 70f)
            val x = s.f(30f, s.w - w - 30f)
            val y = ground - s.f(60f, 320f)
            val path = Path()
            if (s.chance(0.3f)) path.addCircle(x + w / 2f, y + hh / 2f, hh, Path.Direction.CW)
            else path.addRoundRect(x, y, x + w, y + hh, 12f, 12f, Path.Direction.CW)
            s.glowPath(path, color, 3.5f, 18f)
            // Broken reflection in the wet street.
            var ry = ground + (ground - y - hh)
            val bottom = ground + (ground - y)
            while (ry < bottom && ry < s.h) {
                val jitter = s.f(-14f, 14f)
                val fade = (1f - (ry - ground) / (s.h - ground + 1f)).coerceIn(0f, 1f)
                s.canvas.drawLine(x + jitter, ry, x + w + jitter, ry, s.stroke(color, 2f, (90 * fade).toInt()))
                ry += s.f(6f, 12f)
            }
        }
        s.canvas.drawLine(0f, ground, s.w, ground, s.stroke(s.color(0), 1.2f, 90))
    }
}

object NightHighway : Style("night_highway", "Night Highway", Category.CITY) {
    override fun draw(s: Scene) {
        val vx = s.cx + s.f(-80f, 80f)
        val vy = s.h * s.f(0.4f, 0.5f)
        s.faintStars(90, vy - 60f)
        val hill = Path()
        var x = -10f
        while (x <= s.w + 10f) {
            val y = vy - (1f - abs(s.noise.fbm(x * 0.004f, 9f))) * 70f
            if (x == -10f) hill.moveTo(x, y) else hill.lineTo(x, y)
            x += 8f
        }
        s.canvas.drawPath(hill, s.stroke(s.color(2), 1.4f, 90))
        val edge = s.stroke(s.color(2), 1.2f, 60)
        s.canvas.drawLine(vx, vy, -200f, s.h, edge)
        s.canvas.drawLine(vx, vy, s.w + 200f, s.h, edge)
        val warm = Color.rgb(255, 60, 60)
        for (side in 0..1) {
            val color = if (side == 0) warm else s.color(0)
            repeat(s.count(10)) {
                val bx = if (side == 0) s.f(-120f, s.w * 0.45f) else s.f(s.w * 0.55f, s.w + 120f)
                val bend = s.f(-120f, 120f)
                val p = Path()
                p.moveTo(bx, s.h + 20f)
                p.quadTo((bx + vx) / 2f + bend, (s.h + vy) / 2f, vx, vy)
                s.canvas.drawPath(p, s.stroke(color, 4f, 50, 10f))
                s.canvas.drawPath(p, s.stroke(color, s.f(1f, 2.4f), s.i(130, 240)))
            }
        }
        s.glowDot(vx, vy, 4f, Color.WHITE, 16f)
    }
}

// ---------- Glitch ----------

object GlitchBlocks : Style("glitch_blocks", "Glitch", Category.GLITCH) {
    override fun draw(s: Scene) {
        val x = s.cx
        val y = s.h * s.f(0.4f, 0.5f)
        val size = s.f(180f, 260f)
        val shape = Path()
        when (s.i(0, 3)) {
            0 -> shape.addCircle(x, y, size, Path.Direction.CW)
            1 -> {
                shape.moveTo(x, y - size); shape.lineTo(x + size * 0.87f, y + size * 0.5f)
                shape.lineTo(x - size * 0.87f, y + size * 0.5f); shape.close()
            }
            else -> shape.addRect(x - size * 0.8f, y - size * 0.8f, x + size * 0.8f, y + size * 0.8f, Path.Direction.CW)
        }
        val split = s.f(5f, 10f)
        val channels = listOf(s.color(0) to -split, s.color(1) to split, Color.WHITE to 0f)
        for ((c, dx) in channels) {
            s.canvas.save()
            s.canvas.translate(dx, 0f)
            s.canvas.drawPath(shape, s.stroke(c, 4f, if (c == Color.WHITE) 255 else 170).apply { blendMode = BlendMode.PLUS })
            s.canvas.restore()
        }
        // Torn horizontal slices.
        repeat(s.count(14)) {
            val by = y + s.f(-size * 1.3f, size * 1.3f)
            val bh = s.f(4f, 26f)
            val dx = s.f(-90f, 90f)
            s.canvas.save()
            s.canvas.clipRect(0f, by, s.w, by + bh)
            s.canvas.drawRect(0f, by, s.w, by + bh, s.fill(Color.BLACK))
            s.canvas.translate(dx, 0f)
            s.canvas.drawPath(shape, s.stroke(s.anyColor(), 4f))
            s.canvas.restore()
        }
        repeat(s.count(26)) {
            val bx = s.f(0f, s.w)
            val by = y + s.f(-size * 1.6f, size * 1.6f)
            s.canvas.drawRect(bx, by, bx + s.f(10f, 90f), by + s.f(3f, 12f), s.fill(s.anyColor(), s.i(60, 200)))
        }
    }
}

object DataRain : Style("data_rain", "Data Rain", Category.GLITCH) {
    private const val GLYPHS = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホ0123456789ZAMOLED"

    override fun draw(s: Scene) {
        val size = 20f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        var x = size / 2f
        while (x < s.w) {
            if (s.chance(0.55f * s.density.coerceAtMost(1.6f))) {
                val len = s.i(6, 30)
                val headY = s.f(0f, s.h + len * size)
                val color = s.grad(x / s.w)
                for (k in 0 until len) {
                    val y = headY - k * size
                    if (y < -size || y > s.h + size) continue
                    val ch = GLYPHS[s.i(0, GLYPHS.length)].toString()
                    if (k == 0) {
                        paint.color = Color.WHITE
                        paint.alpha = 255
                        paint.setShadowLayer(8f, 0f, 0f, color)
                    } else {
                        paint.color = color
                        paint.alpha = (230 * (1f - k / len.toFloat())).toInt().coerceAtLeast(20)
                        paint.clearShadowLayer()
                    }
                    s.canvas.drawText(ch, x, y, paint)
                }
            }
            x += size * 1.1f
        }
    }
}

object PixelDrip : Style("pixel_drip", "Pixel Drip", Category.GLITCH) {
    override fun draw(s: Scene) {
        val top = s.h * s.f(0.28f, 0.4f)
        val off = s.f(0f, 100f)
        var x = 0f
        while (x < s.w) {
            val y0 = top + s.noise.at(x * 0.006f + off, 0f) * 90f
            val v = ((s.noise.at(x * 0.02f + off, 5f) + 1f) / 2f).coerceIn(0f, 1f)
            if (v > 0.3f) {
                val len = 30f + v.pow(2f) * s.f(300f, 900f)
                val color = s.grad(x / s.w)
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 3.6f
                    shader = LinearGradient(0f, y0, 0f, y0 + len, intArrayOf(color, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
                }
                s.canvas.drawLine(x, y0, x, y0 + len, p)
                s.canvas.drawRect(x - 1.8f, y0 - 3f, x + 1.8f, y0 + 3f, s.fill(Color.WHITE, 220))
            }
            x += 4.4f
        }
    }
}

// ---------- Pixel ----------

private fun Scene.pixelGrid(): Float = 12f

private fun Scene.sprite(mask: Array<String>, x: Float, y: Float, px: Float, color: Int, alpha: Int = 255) {
    val paint = fill(color, alpha)
    for ((r, row) in mask.withIndex()) for ((c, ch) in row.withIndex()) {
        if (ch == '1') canvas.drawRect(x + c * px, y + r * px, x + (c + 1) * px - 1f, y + (r + 1) * px - 1f, paint)
    }
}

private val INVADER = arrayOf(
    "00100000100",
    "00010001000",
    "00111111100",
    "01101110110",
    "11111111111",
    "10111111101",
    "10100000101",
    "00011011000",
)

private val HEART = arrayOf(
    "0110110",
    "1111111",
    "1111111",
    "0111110",
    "0011100",
    "0001000",
)

private fun Scene.pixelStars(n: Int, px: Float) {
    repeat(count(n)) {
        val gx = (f(0f, w) / px).toInt() * px
        val gy = (f(0f, h) / px).toInt() * px
        val big = chance(0.15f)
        val c = if (chance(0.7f)) Color.WHITE else anyColor()
        val a = i(90, 256)
        canvas.drawRect(gx, gy, gx + px / 2f, gy + px / 2f, fill(c, a))
        if (big) {
            val q = px / 2f
            canvas.drawRect(gx - q, gy, gx, gy + q, fill(c, a / 2))
            canvas.drawRect(gx + q, gy, gx + 2 * q, gy + q, fill(c, a / 2))
            canvas.drawRect(gx, gy - q, gx + q, gy, fill(c, a / 2))
            canvas.drawRect(gx, gy + q, gx + q, gy + 2 * q, fill(c, a / 2))
        }
    }
}

object PixelSpace : Style("pixel_space", "Pixel Space", Category.PIXEL) {
    override fun draw(s: Scene) {
        val px = s.pixelGrid()
        s.pixelStars(160, px)
        // Rasterised planet, shaded in three palette tones.
        val cells = s.i(9, 15)
        val pcx = (s.w * s.f(0.3f, 0.7f) / px).toInt() * px
        val pcy = (s.h * s.f(0.25f, 0.45f) / px).toInt() * px
        val lx = -0.6f
        val ly = -0.6f
        for (gy in -cells..cells) for (gx in -cells..cells) {
            val dx = gx / cells.toFloat()
            val dy = gy / cells.toFloat()
            val d = dx * dx + dy * dy
            if (d > 1f) continue
            val dz = sqrt(1f - d)
            val light = (dx * lx + dy * ly + dz * 0.5f).coerceIn(-1f, 1f)
            val tone = when {
                light > 0.45f -> s.color(1)
                light > 0f -> s.color(0)
                light > -0.35f -> s.color(2)
                else -> continue
            }
            val x = pcx + gx * px
            val y = pcy + gy * px
            s.canvas.drawRect(x, y, x + px - 1f, y + px - 1f, s.fill(tone, 230))
        }
        repeat(s.i(3, 6)) {
            val ipx = s.f(5f, 8f)
            s.sprite(INVADER, s.f(40f, s.w - 120f), s.f(s.h * 0.55f, s.h * 0.9f), ipx, s.anyColor())
        }
    }
}

object PixelHearts : Style("pixel_hearts", "Pixel Hearts", Category.PIXEL) {
    override fun draw(s: Scene) {
        val px = s.pixelGrid()
        s.pixelStars(80, px)
        val cell = 140f / sqrt(s.density)
        var y = cell * 0.3f
        var row = 0
        while (y < s.h) {
            var x = if (row % 2 == 0) cell * 0.2f else cell * 0.7f
            while (x < s.w) {
                if (s.chance(0.55f)) {
                    val hp = s.f(4f, 9f)
                    s.sprite(HEART, x + s.f(-12f, 12f), y + s.f(-12f, 12f), hp, s.anyColor(), s.i(150, 256))
                }
                x += cell
            }
            y += cell * 0.8f
            row++
        }
    }
}

object PixelLandscape : Style("pixel_landscape", "Pixel Peaks", Category.PIXEL) {
    override fun draw(s: Scene) {
        val px = s.pixelGrid()
        s.pixelStars(120, px)
        // Pixel moon outline.
        val mr = s.i(5, 8)
        val mx = (s.w * s.f(0.2f, 0.8f) / px).toInt() * px
        val my = (s.h * s.f(0.12f, 0.25f) / px).toInt() * px
        for (gy in -mr..mr) for (gx in -mr..mr) {
            val d = sqrt((gx * gx + gy * gy).toFloat())
            if (d <= mr + 0.3f && d >= mr - 0.9f) {
                s.canvas.drawRect(mx + gx * px, my + gy * px, mx + (gx + 1) * px - 1f, my + (gy + 1) * px - 1f, s.fill(s.color(2)))
            }
        }
        for (layer in 0..2) {
            val base = s.h * (0.55f + layer * 0.12f)
            val amp = 260f - layer * 60f
            var prev = -1f
            var x = 0f
            val color = s.grad(layer / 2f)
            while (x < s.w) {
                val n = 1f - abs(s.noise.fbm(x * 0.003f * (1 + layer * 0.5f), layer * 7f + 2f))
                val top = (((base - n * amp) / px).toInt() * px)
                s.canvas.drawRect(x, top, x + px, s.h, s.fill(Color.BLACK))
                s.canvas.drawRect(x, top, x + px - 1f, top + px - 1f, s.fill(color, 120 + layer * 60))
                if (prev >= 0f && abs(prev - top) > px) {
                    val from = minOf(prev, top)
                    val to = maxOf(prev, top)
                    var yy = from
                    while (yy < to) {
                        val cx = if (prev < top) x - px else x
                        s.canvas.drawRect(cx.coerceAtLeast(0f), yy, cx.coerceAtLeast(0f) + px - 1f, yy + px - 1f, s.fill(color, 120 + layer * 60))
                        yy += px
                    }
                }
                prev = top
                x += px
            }
        }
    }
}

// ---------- Light ----------

object Bokeh : Style("bokeh", "Bokeh", Category.LIGHT) {
    override fun draw(s: Scene) {
        val a = s.f(-0.8f, 0.8f)
        val by = s.h * s.f(0.35f, 0.65f)
        repeat(s.count(42)) {
            val t = s.f(-1f, 1f) * s.h * 0.6f
            val off = s.gauss() * 260f
            val x = s.cx + cos(a) * t - sin(a) * off
            val y = by + sin(a) * t + cos(a) * off
            val r = s.f(14f, 80f)
            val c = s.anyColor()
            s.canvas.drawCircle(x, y, r, s.fill(c, s.i(18, 55), r * 0.08f))
            s.canvas.drawCircle(x, y, r, s.stroke(c, 1.6f, s.i(50, 120), 1.5f))
        }
    }
}

object Fireflies : Style("fireflies", "Fireflies", Category.LIGHT) {
    override fun draw(s: Scene) {
        val ground = s.h * 0.88f
        repeat(s.count(110)) {
            val y = s.h * (0.3f + 0.6f * sqrt(s.f()))
            val r = s.f(1.2f, 3.6f)
            s.glowDot(s.f(0f, s.w), y.coerceAtMost(ground), r, s.anyColor(), r * 5f, s.i(140, 256))
        }
        // Grass silhouette.
        var x = -10f
        while (x < s.w + 10f) {
            val hgt = s.f(40f, 150f)
            val bend = s.f(-30f, 30f)
            val p = Path()
            p.moveTo(x, s.h + 5f)
            p.quadTo(x + bend * 0.3f, s.h - hgt * 0.6f, x + bend, s.h - hgt)
            s.canvas.drawPath(p, s.stroke(s.color(0), 1.4f, s.i(40, 110)))
            x += s.f(5f, 12f)
        }
    }
}

object LightTrails : Style("light_trails", "Light Trails", Category.LIGHT) {
    override fun draw(s: Scene) {
        repeat(s.count(6).coerceIn(2, 12)) { k ->
            val color = s.color(k)
            val sx = s.f(-100f, s.w + 100f)
            val sy = s.f(0f, s.h)
            val ex = s.f(-100f, s.w + 100f)
            val ey = s.f(0f, s.h)
            val c1x = s.f(-200f, s.w + 200f)
            val c1y = s.f(0f, s.h)
            val c2x = s.f(-200f, s.w + 200f)
            val c2y = s.f(0f, s.h)
            for (line in 0 until 3) {
                val o = line * 5f
                val p = Path()
                p.moveTo(sx + o, sy)
                p.cubicTo(c1x + o, c1y + o, c2x - o, c2y + o, ex + o, ey)
                if (line == 0) s.canvas.drawPath(p, s.stroke(color, 9f, 70, 22f).apply { blendMode = BlendMode.PLUS })
                s.canvas.drawPath(p, s.stroke(color, 1.6f, 230 - line * 60).apply { blendMode = BlendMode.PLUS })
            }
            repeat(8) { s.glowDot(ex + s.f(-30f, 30f), ey + s.f(-30f, 30f), s.f(0.8f, 2f), color, 5f) }
        }
    }
}
