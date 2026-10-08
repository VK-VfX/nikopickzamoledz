package com.nikopick.zamoled.gen

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// =====================================================================
// GLITCH: vaporwave
// =====================================================================

/** Classical bust in profile, facing left (unit space, y down). */
private val BUST = floatArrayOf(
    0.55f, 0.55f, 0.6f, 0.1f, 0.62f, -0.2f, 0.55f, -0.5f, 0.4f, -0.72f, 0.2f, -0.82f, 0f, -0.83f,
    -0.15f, -0.75f, -0.25f, -0.6f, -0.28f, -0.45f, -0.33f, -0.38f, -0.32f, -0.3f, -0.45f, -0.12f,
    -0.36f, -0.08f, -0.36f, 0f, -0.33f, 0.04f, -0.35f, 0.08f, -0.3f, 0.12f, -0.32f, 0.22f, -0.25f, 0.3f,
    -0.05f, 0.33f, 0f, 0.5f, -0.3f, 0.75f, -0.55f, 0.95f, -0.6f, 1.1f, 0.9f, 1.1f, 0.85f, 0.8f, 0.6f, 0.65f,
)

object GlitchBust : Style("glitch_bust", "Aesthetic", Category.GLITCH) {
    override val autoPalettes = listOf("sakura", "graffiti", "candy", "violet")

    override fun draw(s: Scene) {
        val pink = s.color(0)
        val cyan = Color.rgb(0, 230, 255)
        val horizon = s.h * 0.66f
        // Striped sun.
        val sx = s.cx
        val sy = horizon - 200f
        val sr = 260f
        val sun = Path(); sun.addCircle(sx, sy, sr, Path.Direction.CW)
        s.canvas.save(); s.canvas.clipPath(sun)
        var y = sy - sr
        var gap = 3f
        while (y < sy + sr) {
            s.canvas.drawRect(sx - sr, y, sx + sr, y + 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, sy - sr, 0f, sy + sr, Color.rgb(255, 220, 90), pink, Shader.TileMode.CLAMP)
            })
            y += 16f + gap
            if (y > sy - sr * 0.2f) gap += 3f
        }
        s.canvas.restore()
        // Perspective grid floor.
        s.canvas.drawLine(0f, horizon, s.w, horizon, s.stroke(pink, 3f, 230, 6f))
        for (k in 1..14) {
            val t = (k / 14f).pow(2f)
            val gy = horizon + (s.h - horizon) * t
            s.canvas.drawLine(0f, gy, s.w, gy, s.stroke(pink, 1.4f + t * 2f, (90 + 160 * t).toInt()))
        }
        for (k in -12..12) s.canvas.drawLine(s.cx + k * 10f, horizon, s.cx + k * 160f, s.h, s.stroke(pink, 1.6f, 170))

        // Bust with RGB split.
        val size = s.f(330f, 380f)
        val bx = s.w * s.f(0.5f, 0.6f)
        val by = s.h * s.f(0.4f, 0.46f)
        val bust = Path()
        for (i in BUST.indices step 2) {
            val px = bx + BUST[i] * size
            val py = by + BUST[i + 1] * size
            if (i == 0) bust.moveTo(px, py) else bust.lineTo(px, py)
        }
        bust.close()
        for ((dx, c) in listOf(-10f to Color.rgb(255, 0, 120), 10f to cyan)) {
            s.canvas.save(); s.canvas.translate(dx, 0f)
            s.canvas.drawPath(bust, s.fill(c, 150).apply { blendMode = BlendMode.PLUS })
            s.canvas.restore()
        }
        s.canvas.drawPath(bust, s.vGradient(by - size, by + size, Color.rgb(235, 235, 240), Color.rgb(120, 120, 135)))
        s.canvas.drawPath(bust, s.stroke(Color.WHITE, 2f))
        // Sculpted details: hair curls, eye, ear.
        repeat(14) {
            val a = s.f(-2.6f, -0.3f)
            val d = s.f(0.35f, 0.7f)
            val hx = bx + 0.2f * size + cos(a) * d * size * 0.6f
            val hy = by - 0.45f * size + sin(a) * d * size * 0.45f
            s.canvas.drawArc(RectF(hx - 16f, hy - 16f, hx + 16f, hy + 16f), s.f(0f, 360f), 260f, false, s.stroke(Color.rgb(140, 140, 155), 2.4f))
        }
        s.canvas.drawArc(RectF(bx - 0.24f * size, by - 0.34f * size, bx - 0.12f * size, by - 0.27f * size), 0f, 180f, false, s.stroke(Color.rgb(90, 90, 105), 3f))
        s.canvas.drawArc(RectF(bx + 0.1f * size, by - 0.32f * size, bx + 0.24f * size, by - 0.12f * size), 270f, 220f, false, s.stroke(Color.rgb(110, 110, 125), 3f))
        // Torn horizontal slices across the bust.
        repeat(s.count(9)) {
            val ty = by + s.f(-size, size)
            val th = s.f(6f, 26f)
            val dx = s.f(-70f, 70f)
            s.canvas.save()
            s.canvas.clipRect(0f, ty, s.w, ty + th)
            s.canvas.drawRect(0f, ty, s.w, ty + th, s.fill(Color.BLACK))
            s.canvas.translate(dx, 0f)
            s.canvas.drawPath(bust, s.fill(if (s.chance(0.5f)) cyan else Color.rgb(230, 230, 240)))
            s.canvas.restore()
        }
        val tp = s.textPaint(62f, Color.WHITE, Typeface.DEFAULT_BOLD).apply { letterSpacing = 0.2f }
        s.canvas.drawText("ＡＥＳＴＨＥＴＩＣ", s.cx + 5f, s.h * 0.88f, Paint(tp).apply { color = cyan })
        s.canvas.drawText("ＡＥＳＴＨＥＴＩＣ", s.cx - 5f, s.h * 0.88f, Paint(tp).apply { color = pink })
        s.canvas.drawText("ＡＥＳＴＨＥＴＩＣ", s.cx, s.h * 0.88f, tp)
    }
}

object ErrorWindows : Style("error_windows", "Error.exe", Category.GLITCH) {
    override val autoPalettes = listOf("graffiti", "cyan", "sakura", "acid", "hue")
    private val titles = arrayOf("ERROR.EXE", "SYSTEM.DLL", "REALITY.SYS", "MEMORY.BIN", "DREAM.TMP", "SIGNAL.LOG")
    private val messages = arrayOf("Reality not found.", "Signal lost.", "Out of memory.", "Error 404: sleep", "Too many tabs open.", "Vibes overflow.", "Wake up?")

    override fun draw(s: Scene) {
        val n = s.i(7, 12)
        val startX = s.f(40f, 160f)
        val startY = s.h * s.f(0.08f, 0.16f)
        val stepX = s.f(26f, 44f)
        val stepY = s.f(110f, 150f)
        for (k in 0 until n) {
            val wx = startX + k * stepX
            val wy = startY + k * stepY
            val ww = s.f(560f, 700f)
            val wh = 230f
            val color = s.color(k)
            val win = RectF(wx, wy, (wx + ww).coerceAtMost(s.w - 30f), wy + wh)
            s.canvas.drawRect(win, s.fill(Color.BLACK))
            s.canvas.drawRect(win, s.stroke(color, 3f))
            val bar = RectF(win.left, win.top, win.right, win.top + 46f)
            s.canvas.drawRect(bar, s.fill(color))
            s.canvas.drawText(titles[s.i(0, titles.size)], bar.left + 18f, bar.bottom - 14f, s.textPaint(26f, Color.BLACK, Typeface.MONOSPACE, Paint.Align.LEFT))
            for (b in 0 until 3) {
                val bx = bar.right - 40f - b * 40f
                s.canvas.drawRect(bx, bar.top + 9f, bx + 28f, bar.bottom - 9f, s.fill(Color.BLACK))
                s.canvas.drawRect(bx, bar.top + 9f, bx + 28f, bar.bottom - 9f, s.stroke(color, 1.6f))
            }
            s.canvas.drawText("✕", bar.right - 26f, bar.bottom - 15f, s.textPaint(22f, color))
            // Warning icon.
            val ix = win.left + 60f
            val iy = win.top + 120f
            val tri = Path(); tri.moveTo(ix, iy - 36f); tri.lineTo(ix + 38f, iy + 30f); tri.lineTo(ix - 38f, iy + 30f); tri.close()
            s.canvas.drawPath(tri, s.fill(Color.rgb(255, 210, 60)))
            s.canvas.drawText("!", ix, iy + 22f, s.textPaint(44f, Color.BLACK))
            s.canvas.drawText(messages[s.i(0, messages.size)], ix + 70f, iy + 10f, s.textPaint(30f, Color.WHITE, Typeface.MONOSPACE, Paint.Align.LEFT))
            val ok = RectF(win.right - 150f, win.bottom - 64f, win.right - 30f, win.bottom - 20f)
            s.canvas.drawRect(ok, s.stroke(color, 2f))
            s.canvas.drawText("OK", ok.centerX(), ok.centerY() + 10f, s.textPaint(26f, color, Typeface.MONOSPACE))
            // Occasional torn slice.
            if (s.chance(0.35f)) {
                val ty = s.f(win.top, win.bottom)
                val dx = s.f(-60f, 60f)
                s.canvas.save()
                s.canvas.clipRect(0f, ty, s.w, ty + s.f(8f, 22f))
                s.canvas.translate(dx, 0f)
                s.canvas.drawRect(win, s.fill(color, 160))
                s.canvas.restore()
            }
        }
        // Pixel cursor.
        val cursor = arrayOf("1", "11", "121", "1221", "12221", "122221", "1222221", "12222221", "1221111", "11021", "1002210", "0001221", "000111")
        val px = 6f
        val cx = s.w * s.f(0.55f, 0.8f)
        val cy = s.h * s.f(0.75f, 0.85f)
        for ((r, row) in cursor.withIndex()) for ((c, ch) in row.withIndex()) {
            if (ch == '0') continue
            s.canvas.drawRect(cx + c * px, cy + r * px, cx + (c + 1) * px, cy + (r + 1) * px, s.fill(if (ch == '1') Color.BLACK else Color.WHITE))
            if (ch == '1') s.canvas.drawRect(cx + c * px, cy + r * px, cx + (c + 1) * px, cy + (r + 1) * px, s.stroke(Color.WHITE, 1f, 120))
        }
        repeat(s.count(30)) {
            val bx = s.f(0f, s.w)
            val by = s.f(0f, s.h)
            s.canvas.drawRect(bx, by, bx + s.f(10f, 70f), by + s.f(3f, 10f), s.fill(s.anyColor(), s.i(80, 200)))
        }
    }
}

// =====================================================================
// PIXEL: 8-bit worlds
// =====================================================================

private class PixelPen(val s: Scene, val px: Float) {
    fun cell(gx: Int, gy: Int, color: Int, alpha: Int = 255) {
        s.canvas.drawRect(gx * px, gy * px, (gx + 1) * px - 0.6f, (gy + 1) * px - 0.6f, s.fill(color, alpha))
    }

    fun sprite(rows: Array<String>, gx: Int, gy: Int, colors: Map<Char, Int>) {
        for ((r, row) in rows.withIndex()) for ((c, ch) in row.withIndex()) {
            val color = colors[ch] ?: continue
            cell(gx + c, gy + r, color)
        }
    }

    fun stars(n: Int, maxGy: Int) {
        val cols = (s.w / px).toInt()
        repeat(s.count(n)) {
            cell(s.i(0, cols), s.i(0, maxGy.coerceAtLeast(1)), if (s.chance(0.8f)) Color.WHITE else s.anyColor(), s.i(80, 256))
        }
    }

    fun disc(cgx: Int, cgy: Int, r: Int, color: Int, ring: Boolean = false) {
        for (y in -r..r) for (x in -r..r) {
            val d = sqrt((x * x + y * y).toFloat())
            if (d <= r + 0.3f && (!ring || d >= r - 0.9f)) cell(cgx + x, cgy + y, color)
        }
    }
}

object PixelCastle : Style("pixel_castle", "Pixel Castle", Category.PIXEL) {
    override val autoPalettes = listOf("violet", "ocean", "gold", "aurora", "sunset")

    override fun draw(s: Scene) {
        val pp = PixelPen(s, 10f)
        val cols = (s.w / pp.px).toInt() + 1
        val rows = (s.h / pp.px).toInt() + 1
        pp.stars(160, rows / 2)
        pp.disc((cols * s.f(0.65f, 0.85f)).toInt(), (rows * 0.15f).toInt(), 7, lighten(s.color(0), 0.6f))
        // Hill.
        val hillTop = IntArray(cols) { x -> (rows * 0.66f - (1f - abs(s.noise.fbm(x * 0.03f, 4f))) * rows * 0.14f).toInt() }
        val grass = mix(s.color(2), Color.rgb(80, 180, 100), 0.6f)
        for (x in 0 until cols) {
            pp.cell(x, hillTop[x], grass)
            for (y in hillTop[x] + 1 until rows) if ((x + y) % 7 == 0) pp.cell(x, y, darken(grass, 0.6f))
        }
        // Castle.
        val stone = Color.rgb(170, 170, 185)
        val cx = cols / 2
        val base = hillTop[cx]
        fun tower(x0: Int, w: Int, h: Int) {
            for (y in base - h until base) {
                pp.cell(x0, y, stone); pp.cell(x0 + w - 1, y, stone)
            }
            for (x in x0 until x0 + w) {
                pp.cell(x, base - h, stone)
                if ((x - x0) % 2 == 0) pp.cell(x, base - h - 1, stone)
            }
            for (y in base - h + 3 until base - 2 step 4) pp.cell(x0 + w / 2, y, Color.rgb(255, 210, 90))
        }
        tower(cx - 16, 7, 26)
        tower(cx + 10, 7, 26)
        tower(cx - 6, 13, 36)
        for (x in cx - 10 until cx + 11) {
            pp.cell(x, base - 14, stone)
            if (x % 2 == 0) pp.cell(x, base - 15, stone)
        }
        for (y in base - 5 until base) for (x in cx - 2..cx + 2) if (y > base - 5 || abs(x - cx) < 2) pp.cell(x, y, Color.rgb(90, 60, 40))
        // Flags.
        for (fx in intArrayOf(cx - 13, cx, cx + 13)) {
            val top = if (fx == cx) base - 44 else base - 34
            for (y in top until top + 6) pp.cell(fx, y, Color.WHITE)
            pp.cell(fx + 1, top, s.color(0)); pp.cell(fx + 2, top, s.color(0)); pp.cell(fx + 1, top + 1, s.color(0))
        }
        // Torches.
        for (tx in intArrayOf(cx - 4, cx + 4)) {
            s.softGlow((tx + 0.5f) * pp.px, (base - 6) * pp.px, 60f, Color.rgb(255, 150, 50), 120)
            pp.cell(tx, base - 6, Color.rgb(255, 200, 80)); pp.cell(tx, base - 5, Color.rgb(140, 90, 50))
        }
        // Pixel pine trees along the hill.
        for (k in 0 until s.i(5, 10)) {
            val tx = s.i(1, cols - 1)
            if (abs(tx - cx) < 20) continue
            val ty = hillTop[tx]
            for (layer in 0 until 4) for (dx in -layer..layer) pp.cell(tx + dx, ty - 7 + layer + (layer / 2) * 1, darken(grass, 0.2f))
            pp.cell(tx, ty - 1, Color.rgb(120, 80, 50)); pp.cell(tx, ty - 2, Color.rgb(120, 80, 50))
        }
    }
}

object PixelQuest : Style("pixel_quest", "Pixel Quest", Category.PIXEL) {
    override val autoPalettes = listOf("candy", "aurora", "graffiti", "gold", "acid")

    private val hero = arrayOf(
        "0011110000",
        "0122221000",
        "0123232100",
        "0122222100",
        "0011110000",
        "0444444000",
        "4544445400",
        "0444444066",
        "0440044006",
        "0110011000",
    )
    private val slime = arrayOf(
        "0001111000",
        "0011111100",
        "0117117110",
        "0118118110",
        "1111111111",
        "1111111111",
        "0111111110",
    )
    private val chest = arrayOf(
        "0333333330",
        "3999999993",
        "3333333333",
        "3999339993",
        "3999339993",
        "3333333333",
    )
    private val heart = arrayOf("0110110", "1111111", "1111111", "0111110", "0011100", "0001000")

    override fun draw(s: Scene) {
        val pp = PixelPen(s, 12f)
        val cols = (s.w / pp.px).toInt() + 1
        val rows = (s.h / pp.px).toInt() + 1
        pp.stars(90, rows / 2)
        // Distant pixel mountains and clouds.
        for (x in 0 until cols) {
            val top = (rows * 0.6f - (1f - abs(s.noise.fbm(x * 0.05f, 2f))) * rows * 0.2f).toInt()
            pp.cell(x, top, darken(s.color(2), 0.3f))
        }
        repeat(s.i(2, 5)) {
            val cgx = s.i(2, cols - 10)
            val cgy = s.i(4, rows / 3)
            for (dx in 0 until 8) for (dy in 0 until 3) if (!(dy == 0 && (dx == 0 || dx == 7))) pp.cell(cgx + dx, cgy + dy, Color.WHITE, 150)
        }
        // Ground with grass and dirt.
        val ground = (rows * 0.8f).toInt()
        val grass = mix(s.color(2), Color.rgb(80, 200, 100), 0.5f)
        for (x in 0 until cols) {
            pp.cell(x, ground, grass)
            for (y in ground + 1 until rows) if ((x * 3 + y * 5) % 11 == 0) pp.cell(x, y, Color.rgb(150, 100, 60))
        }
        // Floating platforms with coins.
        repeat(s.i(2, 4)) {
            val pgx = s.i(4, cols - 14)
            val pgy = s.i((rows * 0.45f).toInt(), (rows * 0.7f).toInt())
            for (dx in 0 until 10) { pp.cell(pgx + dx, pgy, grass); pp.cell(pgx + dx, pgy + 1, Color.rgb(150, 100, 60)) }
            for (c in 0 until 3) {
                val coinX = pgx + 2 + c * 3
                pp.cell(coinX, pgy - 3, Color.rgb(255, 215, 70)); pp.cell(coinX, pgy - 4, Color.rgb(255, 215, 70))
                s.softGlow((coinX + 0.5f) * pp.px, (pgy - 3) * pp.px, 24f, Color.rgb(255, 215, 70), 90)
            }
        }
        val colors = mapOf(
            '1' to Color.rgb(60, 40, 30), '2' to Color.rgb(250, 200, 160), '3' to Color.BLACK,
            '4' to s.color(0), '5' to Color.rgb(255, 215, 70), '6' to Color.rgb(200, 200, 220),
        )
        pp.sprite(hero, (cols * 0.25f).toInt(), ground - 10, colors)
        pp.sprite(slime, (cols * 0.62f).toInt(), ground - 7, mapOf('1' to s.color(1), '7' to Color.WHITE, '8' to Color.BLACK))
        pp.sprite(chest, (cols * 0.82f).toInt().coerceAtMost(cols - 11), ground - 6, mapOf('3' to Color.rgb(120, 80, 40), '9' to Color.rgb(255, 200, 60)))
        // HUD.
        for (k in 0 until 3) pp.sprite(heart, 2 + k * 9, 6, mapOf('1' to Color.rgb(255, 70, 90)))
        s.canvas.drawText("SCORE 004200", s.w - 40f, 6 * pp.px + 60f, s.textPaint(40f, Color.WHITE, Typeface.MONOSPACE, Paint.Align.RIGHT))
        s.canvas.drawText("✦ x 42", s.w - 40f, 6 * pp.px + 110f, s.textPaint(36f, Color.rgb(255, 215, 70), Typeface.MONOSPACE, Paint.Align.RIGHT))
        s.canvas.drawText("PRESS START", s.cx, s.h * 0.93f, s.textPaint(44f, Color.WHITE, Typeface.MONOSPACE))
    }
}

// =====================================================================
// LIGHT: warm glow
// =====================================================================

object LanternFestival : Style("lantern_festival", "Lanterns", Category.LIGHT) {
    override val autoPalettes = listOf("sunset", "gold", "lava", "sakura")

    override fun draw(s: Scene) {
        val horizon = s.h * s.f(0.68f, 0.74f)
        s.starSky(120, horizon)
        // Far mountains.
        val (land, line) = s.ridge(horizon, 140f, 0.004f, 3f)
        s.canvas.drawPath(land, s.fill(Color.BLACK))
        s.canvas.drawPath(line, s.stroke(s.color(0), 1.6f, 120))
        s.canvas.drawRect(0f, horizon, s.w, s.h, s.fill(Color.BLACK))
        val warm = Color.rgb(255, 170, 70)
        val lanterns = ArrayList<FloatArray>()
        repeat(s.count(40)) {
            val depth = s.f().pow(1.6f) // 0 far .. 1 near
            val size = 10f + depth * 50f
            val x = s.f(0f, s.w)
            val y = horizon - 40f - (1f - depth) * horizon * 0.85f + s.f(-60f, 60f)
            lanterns.add(floatArrayOf(x, y, size))
        }
        lanterns.sortBy { it[2] }
        for (l in lanterns) lantern(s, l[0], l[1], l[2], mix(warm, s.color(0), s.f(0f, 0.4f)))
        // Reflections on the lake.
        for (l in lanterns) {
            val ry = horizon + (horizon - l[1]) * 0.35f
            if (ry > s.h) continue
            repeat(3) {
                val y = ry + s.f(-l[2], l[2])
                s.canvas.drawLine(l[0] - l[2] * 0.8f, y, l[0] + l[2] * 0.8f, y, s.stroke(warm, 2f, s.i(40, 140)))
            }
        }
        // Pier with two figures releasing a lantern.
        val px = s.w * s.f(0.1f, 0.35f)
        val py = horizon + 140f
        s.canvas.drawRect(px - 140f, py, px + 220f, py + 14f, s.fill(Color.BLACK))
        s.canvas.drawLine(px - 140f, py, px + 220f, py, s.stroke(warm, 2f, 200))
        for (k in 0 until 4) s.canvas.drawLine(px - 120f + k * 100f, py + 14f, px - 120f + k * 100f, py + 80f, s.stroke(warm, 2f, 120))
        for ((dx, hgt) in listOf(0f to 92f, 60f to 80f)) {
            s.canvas.drawRoundRect(RectF(px + dx - 14f, py - hgt + 22f, px + dx + 14f, py), 10f, 10f, s.fill(Color.BLACK))
            s.canvas.drawRoundRect(RectF(px + dx - 14f, py - hgt + 22f, px + dx + 14f, py), 10f, 10f, s.stroke(warm, 1.6f))
            s.canvas.drawCircle(px + dx, py - hgt + 8f, 12f, s.fill(Color.BLACK))
            s.canvas.drawCircle(px + dx, py - hgt + 8f, 12f, s.stroke(warm, 1.6f))
        }
        lantern(s, px + 30f, py - 150f, 34f, warm)
    }

    private fun lantern(s: Scene, x: Float, y: Float, size: Float, color: Int) {
        s.softGlow(x, y, size * 3.2f, color, 70)
        val body = Path()
        body.moveTo(x - size * 0.55f, y - size)
        body.lineTo(x + size * 0.55f, y - size)
        body.quadTo(x + size * 0.62f, y, x + size * 0.42f, y + size * 0.8f)
        body.lineTo(x - size * 0.42f, y + size * 0.8f)
        body.quadTo(x - size * 0.62f, y, x - size * 0.55f, y - size)
        body.close()
        s.canvas.drawPath(body, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(x, y - size, x, y + size * 0.8f, darken(color, 0.35f), lighten(color, 0.45f), Shader.TileMode.CLAMP)
        })
        if (size > 25f) {
            s.canvas.drawLine(x, y - size, x, y + size * 0.8f, s.stroke(darken(color, 0.4f), 1f, 120))
            s.canvas.drawLine(x - size * 0.6f, y - size * 0.1f, x + size * 0.6f, y - size * 0.1f, s.stroke(darken(color, 0.4f), 1f, 100))
        }
        s.glowDot(x, y + size * 0.6f, size * 0.12f, Color.WHITE, size * 0.3f)
    }
}

object Candles : Style("candles", "Candlelight", Category.LIGHT) {
    override val autoPalettes = listOf("gold", "sunset", "lava", "sakura")

    override fun draw(s: Scene) {
        val table = s.h * s.f(0.72f, 0.78f)
        val n = s.i(5, 8)
        val placed = ArrayList<FloatArray>()
        repeat(n) {
            val x = s.cx + s.gauss() * 280f
            val hgt = s.f(180f, 520f)
            val half = s.f(34f, 60f)
            val y = table + s.f(-30f, 40f)
            placed.add(floatArrayOf(x, y, hgt, half))
        }
        placed.sortBy { it[1] }
        val wax = lighten(s.color(1), 0.75f)
        for (c in placed) {
            val x = c[0]; val base = c[1]; val hgt = c[2]; val half = c[3]
            val top = base - hgt
            val body = Path()
            body.moveTo(x - half, base); body.lineTo(x - half, top + 10f)
            body.quadTo(x - half * 0.6f, top - 8f, x - half * 0.2f, top + 6f)
            body.quadTo(x + half * 0.3f, top + 14f, x + half, top + 4f)
            body.lineTo(x + half, base); body.close()
            s.canvas.drawPath(body, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(x, top, x, base, intArrayOf(lighten(wax, 0.4f), wax, darken(wax, 0.85f)), floatArrayOf(0f, 0.2f, 1f), Shader.TileMode.CLAMP)
            })
            repeat(s.i(1, 4)) {
                val dx = x + s.f(-half, half)
                val drip = Path()
                drip.moveTo(dx, top + 6f); drip.quadTo(dx + s.f(-4f, 4f), top + s.f(30f, 90f), dx + s.f(-2f, 2f), top + s.f(40f, 120f))
                s.canvas.drawPath(drip, s.stroke(lighten(wax, 0.3f), s.f(5f, 9f)))
            }
            s.canvas.drawLine(x, top + 6f, x, top - 10f, s.stroke(Color.rgb(40, 30, 25), 3f))
            flame(s, x, top - 12f, s.f(26f, 36f))
            // Smoke wisp.
            if (s.chance(0.4f)) {
                val wisp = Path()
                wisp.moveTo(x, top - 70f)
                wisp.cubicTo(x + 30f, top - 140f, x - 30f, top - 200f, x + 10f, top - 280f)
                s.canvas.drawPath(wisp, s.stroke(Color.rgb(200, 200, 210), 2f, 50, 3f))
            }
        }
        // Reflection on the polished table.
        s.canvas.drawLine(0f, table + 50f, s.w, table + 50f, s.stroke(Color.rgb(120, 90, 60), 1.4f, 120))
        for (c in placed) {
            s.softGlow(c[0], c[1] + 60f, 90f, Color.rgb(255, 170, 70), 60)
        }
    }
}

object Jellyfish : Style("jellyfish", "Jellyfish", Category.LIGHT) {
    override val autoPalettes = listOf("hue", "cyan", "violet", "sakura", "aurora")

    override fun draw(s: Scene) {
        // Faint light rays from the surface.
        repeat(5) {
            val x = s.f(0f, s.w)
            val ray = Path()
            ray.moveTo(x - 20f, 0f); ray.lineTo(x + 20f, 0f); ray.lineTo(x + 160f, s.h * 0.7f); ray.lineTo(x + 60f, s.h * 0.7f); ray.close()
            s.canvas.drawPath(ray, s.vGradient(0f, s.h * 0.7f, withAlpha(s.color(1), 30), Color.TRANSPARENT))
        }
        // Marine snow.
        repeat(s.count(160)) { s.dot(s.f(0f, s.w), s.f(0f, s.h), s.f(0.6f, 1.8f), Color.WHITE, s.i(30, 140)) }
        val n = s.i(3, 6)
        repeat(n) {
            jelly(s, s.f(120f, s.w - 120f), s.f(s.h * 0.1f, s.h * 0.75f), s.f(60f, 150f), s.anyColor(), s.f(-25f, 25f))
        }
    }

    private fun jelly(s: Scene, x: Float, y: Float, r: Float, color: Int, tilt: Float) {
        s.canvas.save()
        s.canvas.rotate(tilt, x, y)
        s.softGlow(x, y, r * 2.6f, color, 60)
        // Long trailing tentacles.
        repeat(s.i(7, 12)) {
            val tx = x + s.f(-r * 0.85f, r * 0.85f)
            val len = s.f(r * 2.5f, r * 5f)
            val p = Path()
            p.moveTo(tx, y + r * 0.1f)
            var yy = y + r * 0.1f
            val phase = s.f(0f, TAU)
            while (yy < y + len) {
                yy += 8f
                p.lineTo(tx + sin(yy * 0.03f + phase) * r * 0.18f, yy)
            }
            s.canvas.drawPath(p, s.stroke(lighten(color, 0.3f), 1.4f, s.i(80, 180)).apply { blendMode = BlendMode.PLUS })
        }
        // Frilly oral arms.
        for (k in 0 until 4) {
            val ax = x + (k - 1.5f) * r * 0.22f
            val arm = Path()
            arm.moveTo(ax, y)
            var yy = y
            while (yy < y + r * 2f) {
                yy += 6f
                arm.lineTo(ax + sin(yy * 0.08f + k) * r * 0.12f, yy)
            }
            s.canvas.drawPath(arm, s.stroke(lighten(color, 0.5f), r * 0.07f, 140).apply { blendMode = BlendMode.PLUS })
        }
        // Translucent bell.
        val bell = Path()
        bell.moveTo(x - r, y + r * 0.1f)
        bell.cubicTo(x - r, y - r * 1.1f, x + r, y - r * 1.1f, x + r, y + r * 0.1f)
        var sx = x + r
        var up = true
        while (sx > x - r) {
            val nx = sx - r * 0.2f
            bell.quadTo((sx + nx) / 2f, y + r * (if (up) 0.25f else 0.05f), nx, y + r * 0.1f)
            sx = nx
            up = !up
        }
        bell.close()
        s.canvas.drawPath(bell, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x, y - r * 0.4f, r * 1.2f, intArrayOf(withAlpha(lighten(color, 0.6f), 200), withAlpha(color, 110), withAlpha(color, 40)), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            blendMode = BlendMode.PLUS
        })
        s.canvas.drawPath(bell, s.stroke(lighten(color, 0.6f), 2f, 220))
        // Inner organs: four loops.
        for (k in 0 until 4) {
            val a = k * PI.toFloat() / 2f + PI.toFloat() / 4f
            s.canvas.drawCircle(x + cos(a) * r * 0.25f, y - r * 0.35f + sin(a) * r * 0.15f, r * 0.14f, s.stroke(lighten(color, 0.7f), 2f, 200))
        }
        s.canvas.restore()
    }
}
