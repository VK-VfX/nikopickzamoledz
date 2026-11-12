package com.nikopick.zamoled.gen

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
// ---------- Nature ----------

object Aurora : Style("aurora", "Aurora", Category.NATURE) {
    override fun draw(s: Scene) {
        s.faintStars(220, s.h * 0.7f)
        val bands = s.i(2, 4)
        for (b in 0 until bands) {
            val base = s.h * s.f(0.35f, 0.6f)
            val amp = s.f(70f, 160f)
            val len = s.f(220f, 420f)
            val c1 = s.color(b)
            val c2 = s.color(b + 1)
            val off = s.f(0f, 100f)
            var x = -10f
            while (x < s.w + 10f) {
                val y = base + s.noise.at(x * 0.0025f + off, b * 3.1f) * amp * 2f
                val l = len * (0.55f + 0.45f * ((s.noise.at(x * 0.01f + off, 7f + b) + 1f) / 2f))
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 4.2f
                    blendMode = BlendMode.PLUS
                    shader = LinearGradient(
                        x, y, x, y - l,
                        intArrayOf(withAlpha(c1, 85), withAlpha(c2, 35), withAlpha(c2, 0)),
                        floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP,
                    )
                }
                s.canvas.drawLine(x, y + 6f, x, y - l, p)
                x += 3.2f
            }
        }
        // Mountain silhouette in front.
        val ridge = Path()
        val land = Path()
        land.moveTo(-10f, s.h + 10f)
        var x = -10f
        while (x <= s.w + 10f) {
            val y = s.h * 0.84f - (1f - abs(s.noise.fbm(x * 0.004f, 50f))) * 110f
            if (x == -10f) ridge.moveTo(x, y) else ridge.lineTo(x, y)
            land.lineTo(x, y)
            x += 5f
        }
        land.lineTo(s.w + 10f, s.h + 10f)
        land.close()
        s.canvas.drawPath(land, s.fill(Color.BLACK))
        s.canvas.drawPath(ridge, s.stroke(s.color(0), 1.6f, 110))
    }
}

object Topographic : Style("topographic", "Topographic", Category.NATURE) {
    override fun draw(s: Scene) {
        val step = 8f
        val scale = s.f(0.0025f, 0.0045f)
        val nx = (s.w / step).toInt() + 2
        val ny = (s.h / step).toInt() + 2
        val v = Array(ny) { j -> FloatArray(nx) { i -> s.noise.fbm(i * step * scale, j * step * scale, 4) } }
        val levels = s.count(14).coerceIn(4, 30)
        for (l in 0 until levels) {
            val t = l / (levels - 1f).coerceAtLeast(1f)
            val level = -0.55f + 1.1f * t
            val segs = ArrayList<Float>()
            for (j in 0 until ny - 1) for (i in 0 until nx - 1) {
                val a = v[j][i]; val b = v[j][i + 1]; val c = v[j + 1][i + 1]; val d = v[j + 1][i]
                var idx = 0
                if (a > level) idx = idx or 8
                if (b > level) idx = idx or 4
                if (c > level) idx = idx or 2
                if (d > level) idx = idx or 1
                if (idx == 0 || idx == 15) continue
                val x0 = i * step; val y0 = j * step
                fun lerpT(p: Float, q: Float) = ((level - p) / (q - p)).coerceIn(0f, 1f)
                val top = floatArrayOf(x0 + step * lerpT(a, b), y0)
                val right = floatArrayOf(x0 + step, y0 + step * lerpT(b, c))
                val bottom = floatArrayOf(x0 + step * lerpT(d, c), y0 + step)
                val left = floatArrayOf(x0, y0 + step * lerpT(a, d))
                fun seg(p: FloatArray, q: FloatArray) { segs.add(p[0]); segs.add(p[1]); segs.add(q[0]); segs.add(q[1]) }
                when (idx) {
                    1, 14 -> seg(left, bottom)
                    2, 13 -> seg(bottom, right)
                    3, 12 -> seg(left, right)
                    4, 11 -> seg(top, right)
                    6, 9 -> seg(top, bottom)
                    7, 8 -> seg(left, top)
                    5 -> { seg(left, top); seg(bottom, right) }
                    10 -> { seg(top, right); seg(left, bottom) }
                }
            }
            val width = if (l % 5 == 0) 2.6f else 1.5f
            s.canvas.drawLines(segs.toFloatArray(), s.stroke(s.grad(t), width, 200))
        }
    }
}

object Mountains : Style("mountains", "Mountains", Category.NATURE) {
    override fun draw(s: Scene) {
        s.faintStars(150, s.h * 0.5f)
        val mx = s.w * s.f(0.2f, 0.8f)
        val my = s.h * s.f(0.14f, 0.28f)
        s.glowDot(mx, my, s.f(40f, 60f), s.color(0), 40f, 230)
        val layers = 5
        for (i in 0 until layers) {
            val t = i / (layers - 1f)
            val baseY = s.h * (0.45f + 0.1f * i)
            val amp = 170f - 22f * i
            val freq = 0.004f * (1f + i * 0.3f)
            val ridge = Path()
            val land = Path()
            land.moveTo(-10f, s.h + 10f)
            var x = -10f
            while (x <= s.w + 10f) {
                val y = baseY - (1f - abs(s.noise.fbm(x * freq, i * 10f + 3f))) * amp
                if (x == -10f) ridge.moveTo(x, y) else ridge.lineTo(x, y)
                land.lineTo(x, y)
                x += 6f
            }
            land.lineTo(s.w + 10f, s.h + 10f)
            land.close()
            s.canvas.drawPath(land, s.fill(Color.BLACK))
            s.canvas.drawPath(ridge, s.stroke(s.grad(1f - t), 1.6f + t, (70 + 185 * t).toInt()))
        }
    }
}
