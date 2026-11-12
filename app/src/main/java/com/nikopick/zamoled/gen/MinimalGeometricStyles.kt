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
