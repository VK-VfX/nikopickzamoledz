package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ---------- Minimal ----------

object Halo : Style("halo", "Halo", Category.MINIMAL) {
    override fun draw(s: Scene) {
        val x = s.cx
        val y = s.h * s.f(0.35f, 0.5f)
        val r = s.f(230f, 330f)
        val sweep = SweepGradient(
            x, y,
            intArrayOf(s.color(0), s.color(1), withAlpha(s.color(2), 0), s.color(0)),
            null,
        )
        sweep.setLocalMatrix(Matrix().apply { setRotate(s.f(0f, 360f), x, y) })
        val glow = s.stroke(Color.WHITE, 14f, 150, 30f).apply { shader = sweep }
        val core = s.stroke(Color.WHITE, 3f).apply { shader = sweep }
        s.canvas.drawCircle(x, y, r, glow)
        s.canvas.drawCircle(x, y, r, core)
        if (s.chance(0.6f)) {
            val a = s.f(0f, TAU)
            s.glowDot(x + cos(a) * r, y + sin(a) * r, 5f, s.color(0), 16f)
        }
    }
}

object Horizon : Style("horizon", "Horizon", Category.MINIMAL) {
    override fun draw(s: Scene) {
        val big = s.f(900f, 1500f)
        val edge = s.h * s.f(0.62f, 0.78f)
        val cy = edge + big
        s.faintStars(70, edge - 40f, 120)
        val fade = LinearGradient(
            0f, 0f, s.w, 0f,
            intArrayOf(withAlpha(s.color(1), 0), s.color(0), s.color(1), withAlpha(s.color(0), 0)),
            floatArrayOf(0f, 0.35f, 0.65f, 1f), Shader.TileMode.CLAMP,
        )
        s.canvas.drawCircle(s.cx, cy, big + 14f, s.stroke(Color.WHITE, 30f, 60, 50f).apply { shader = fade })
        s.canvas.drawCircle(s.cx, cy, big, s.stroke(Color.WHITE, 10f, 170, 18f).apply { shader = fade })
        s.canvas.drawCircle(s.cx, cy, big, s.stroke(Color.WHITE, 2.4f).apply { shader = fade })
    }
}

object Silk : Style("silk", "Silk", Category.MINIMAL) {
    override fun draw(s: Scene) {
        val lines = s.count(7)
        val y1 = s.h * s.f(0.25f, 0.55f)
        val y2 = s.h * s.f(0.45f, 0.8f)
        val d1 = s.f(-400f, 400f)
        val d2 = s.f(-400f, 400f)
        val shader = LinearGradient(
            0f, 0f, s.w, 0f,
            intArrayOf(withAlpha(s.color(0), 0), s.color(0), s.color(1), withAlpha(s.color(2), 0)),
            floatArrayOf(0f, 0.25f, 0.75f, 1f), Shader.TileMode.CLAMP,
        )
        for (k in 0 until lines) {
            val o = k * 7f
            val p = Path()
            p.moveTo(-60f, y1 + o)
            p.cubicTo(s.w * 0.3f, y1 + d1 + o * 1.8f, s.w * 0.7f, y2 + d2 - o * 1.2f, s.w + 60f, y2 + o)
            val a = (255 * (1f - k / lines.toFloat() * 0.8f)).toInt()
            if (k == 0) s.canvas.drawPath(p, s.stroke(Color.WHITE, 8f, 120, 22f).apply { this.shader = shader })
            s.canvas.drawPath(p, s.stroke(Color.WHITE, 2f, a).apply { this.shader = shader })
        }
    }
}

// ---------- Geometric ----------

object PolygonTunnel : Style("polygon_tunnel", "Polygon Tunnel", Category.GEOMETRIC) {
    override fun draw(s: Scene) {
        val sides = s.i(3, 9)
        val x = s.cx + s.f(-80f, 80f)
        val y = s.h * s.f(0.4f, 0.6f)
        val n = s.count(26)
        val twist = s.f(2f, 9f)
        val rMax = max(s.w, s.h) * 0.75f
        for (k in 0 until n) {
            val t = if (n == 1) 0f else k / (n - 1f)
            val r = 18f + rMax * t.pow(1.6f)
            val rot = Math.toRadians((k * twist).toDouble()).toFloat()
            val p = Path()
            for (j in 0..sides) {
                val a = rot + j * TAU / sides
                val px = x + cos(a) * r
                val py = y + sin(a) * r
                if (j == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.close()
            val alpha = (255 * (1f - t * 0.85f)).toInt()
            s.canvas.drawPath(p, s.stroke(s.grad(t), 2.2f, alpha))
        }
        s.glowDot(x, y, 4f, s.color(0), 12f)
    }
}

object DotMatrix : Style("dot_matrix", "Dot Matrix", Category.GEOMETRIC) {
    override fun draw(s: Scene) {
        val gap = 26f / sqrt(s.density)
        val scale = s.f(0.0025f, 0.005f)
        var y = gap / 2f
        while (y < s.h) {
            var x = gap / 2f
            while (x < s.w) {
                val n = ((s.noise.fbm(x * scale, y * scale) * 1.6f) + 1f) / 2f
                val v = n.coerceIn(0f, 1f)
                val r = 7f * v.pow(2.5f)
                if (r >= 0.8f) s.dot(x, y, r, s.grad(v), (90 + 165 * v).toInt())
                x += gap
            }
            y += gap
        }
    }
}

object Isometric : Style("isometric", "Isometric", Category.GEOMETRIC) {
    override fun draw(s: Scene) {
        val size = s.f(38f, 60f) / sqrt(s.density)
        val dx = size * sqrt(3f)
        val dy = size * 1.5f
        val scale = s.f(0.002f, 0.004f)
        var row = 0
        var y = 0f
        while (y < s.h + size) {
            var x = if (row % 2 == 1) dx / 2f else 0f
            while (x < s.w + size) {
                val v = (s.noise.at(x * scale, y * scale) + 1f) / 2f
                if (v > 0.52f || s.chance(0.06f)) {
                    val color = s.grad(v)
                    val alpha = (110 + 145 * v).toInt().coerceAtMost(255)
                    val pts = FloatArray(12)
                    for (k in 0 until 6) {
                        val a = Math.toRadians((-90 + 60 * k).toDouble()).toFloat()
                        pts[k * 2] = x + cos(a) * size * 0.95f
                        pts[k * 2 + 1] = y + sin(a) * size * 0.95f
                    }
                    val hex = Path()
                    hex.moveTo(pts[0], pts[1])
                    for (k in 1 until 6) hex.lineTo(pts[k * 2], pts[k * 2 + 1])
                    hex.close()
                    if (s.chance(0.25f)) {
                        val top = Path()
                        top.moveTo(pts[0], pts[1]); top.lineTo(pts[2], pts[3])
                        top.lineTo(x, y); top.lineTo(pts[10], pts[11]); top.close()
                        s.canvas.drawPath(top, s.fill(color, 45))
                    }
                    val paint = s.stroke(color, 1.8f, alpha)
                    s.canvas.drawPath(hex, paint)
                    for (k in intArrayOf(1, 3, 5)) s.canvas.drawLine(x, y, pts[k * 2], pts[k * 2 + 1], paint)
                }
                x += dx
            }
            y += dy
            row++
        }
    }
}
