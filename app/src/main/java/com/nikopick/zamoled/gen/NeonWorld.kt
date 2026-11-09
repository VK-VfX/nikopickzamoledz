package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Neon that is the whole picture: tubes of light on black, with no wall or street behind them. */

/** Rings of neon rushing toward a vanishing point: a tunnel you fall down. */
object NeonTunnel : Style("neon_tunnel", "Neon Tunnel", Category.NEON) {
    override val autoPalettes = listOf("graffiti", "cyan", "violet", "sakura", "hue", "lava")

    override fun draw(s: Scene) {
        val vx = s.cx + s.f(-160f, 160f)
        val vy = s.h * s.f(0.36f, 0.5f)
        val kind = s.i(0, 3)
        val rings = s.count(20)
        val drift = s.f(-60f, 60f)
        // Far rings first, so the near ones lie over them.
        for (k in rings - 1 downTo 0) {
            val t = k / (rings - 1f)                                    // 0 near .. 1 far
            val r = s.w * 1.15f * (1f - t).pow(2.2f) + 8f
            val cx = s.cx + (vx - s.cx) * t + sin(t * 5f) * drift * (1f - t)
            val cy = s.h * 0.5f + (vy - s.h * 0.5f) * t
            val ry = r * 1.9f
            val path = Path()
            when (kind) {
                0 -> path.addRoundRect(RectF(cx - r, cy - ry, cx + r, cy + ry), r * 0.28f, r * 0.28f, Path.Direction.CW)
                1 -> {
                    for (i in 0 until 6) {
                        val a = (PI / 3.0 * i - PI / 2.0).toFloat()
                        val x = cx + cos(a) * r * 1.1f
                        val y = cy + sin(a) * ry * 0.95f
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                }
                else -> path.addOval(RectF(cx - r, cy - ry * 0.7f, cx + r, cy + ry * 0.7f), Path.Direction.CW)
            }
            s.neonPath(path, s.grad(t), 1.6f + 5f * (1f - t), dim = t > 0.8f)
        }
        s.softGlow(vx, vy, 160f, s.color(1), 120)
        s.canvas.drawCircle(vx, vy, 5f, s.fill(Color.WHITE))
    }
}

/** Light painting: ribbons of neon strands that twist as they cross the screen. */
object NeonRibbons : Style("neon_ribbons", "Neon Ribbons", Category.NEON) {
    override val autoPalettes = listOf("hue", "graffiti", "cyan", "sakura", "aurora", "violet")

    override fun draw(s: Scene) {
        val ribbons = s.count(4)
        val strands = (8 + s.density * 4f).toInt().coerceIn(6, 16)
        for (j in 0 until ribbons) {
            val color = s.color(j)
            val baseY = s.h * (0.16f + 0.7f * (j + s.f(0.2f, 0.8f)) / ribbons)
            val amp = s.f(120f, 300f)
            val freq = s.f(0.003f, 0.007f)
            val phase = s.f(0f, (2 * PI).toFloat())
            val width = s.f(80f, 190f)
            val twistF = s.f(0.004f, 0.01f)
            val twistP = s.f(0f, (2 * PI).toFloat())
            val tilt = s.f(-0.35f, 0.35f)
            for (st in 0 until strands) {
                val u = st / (strands - 1f) - 0.5f
                val path = Path()
                var x = -30f
                var first = true
                while (x <= s.w + 30f) {
                    val c = baseY + sin(x * freq + phase) * amp + x * tilt
                    val y = c + u * width * 2f * sin(x * twistF + twistP)
                    if (first) path.moveTo(x, y) else path.lineTo(x, y)
                    first = false
                    x += 9f
                }
                s.neonPath(path, if (st % 3 == 0) lighten(color, 0.3f) else color, 1.6f, dim = abs(u) > 0.35f)
            }
        }
    }
}

/** Branching lightning, each bolt a neon tube, striking from the top of the frame. */
object NeonStorm : Style("neon_storm", "Neon Storm", Category.NEON) {
    override val autoPalettes = listOf("violet", "cyan", "acid", "lava", "graffiti", "sakura")

    private fun Scene.bolt(path: Path, x1: Float, y1: Float, x2: Float, y2: Float, sway: Float, depth: Int, branches: MutableList<FloatArray>) {
        if (depth == 0) {
            path.lineTo(x2, y2)
            return
        }
        val mx = (x1 + x2) / 2f + f(-sway, sway)
        val my = (y1 + y2) / 2f + f(-sway * 0.3f, sway * 0.3f)
        if (depth in 2..4 && chance(0.3f)) branches += floatArrayOf(mx, my, mx + f(-260f, 260f), my + f(120f, 380f), sway * 0.7f)
        bolt(path, x1, y1, mx, my, sway * 0.55f, depth - 1, branches)
        bolt(path, mx, my, x2, y2, sway * 0.55f, depth - 1, branches)
    }

    override fun draw(s: Scene) {
        val bolts = s.count(3)
        val flash = s.color(0)
        for (b in 0 until bolts) {
            val x1 = s.f(s.w * 0.1f, s.w * 0.9f)
            val x2 = x1 + s.f(-240f, 240f)
            val y2 = s.h * s.f(0.55f, 0.9f)
            val color = s.color(b)
            val branches = ArrayList<FloatArray>()
            val main = Path().apply { moveTo(x1, -10f) }
            s.bolt(main, x1, -10f, x2, y2, 150f, 7, branches)
            s.softGlow(x2, y2, 240f, color, 90)
            for (br in branches) {
                val p = Path().apply { moveTo(br[0], br[1]) }
                s.bolt(p, br[0], br[1], br[2], br[3], br[4], 4, ArrayList())
                s.neonPath(p, color, 1.6f, dim = true)
            }
            s.neonPath(main, color, 3.2f)
            // Where it lands, the ground rings.
            s.canvas.drawOval(RectF(x2 - 150f, y2 - 14f, x2 + 150f, y2 + 14f), s.stroke(withAlpha(lighten(color, 0.4f), 120), 2f, 120, 6f))
        }
        s.softGlow(s.cx, 0f, s.w * 0.8f, flash, 40)
    }
}

object NeonWorld {
    val styles: List<Style> = listOf(NeonTunnel, NeonRibbons, NeonStorm)
}
