package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ---------- Abstract ----------

object FlowField : Style("flow_field", "Flow Field", Category.ABSTRACT) {
    override fun draw(s: Scene) {
        val scale = s.f(0.0018f, 0.004f)
        val turns = s.f(1f, 2.2f)
        repeat(s.count(650)) {
            var x = s.f(0f, s.w)
            var y = s.f(0f, s.h)
            val color = s.grad(x / s.w)
            val p = Path()
            p.moveTo(x, y)
            repeat(70) {
                val a = s.noise.at(x * scale, y * scale) * TAU * turns
                x += cos(a) * 4f
                y += sin(a) * 4f
                p.lineTo(x, y)
            }
            s.canvas.drawPath(p, s.stroke(color, 1.3f, 120))
        }
    }
}

object Waves : Style("waves", "Waves", Category.ABSTRACT) {
    override fun draw(s: Scene) {
        val lines = s.count(42)
        val spacing = s.f(6f, 10f)
        val top = s.h * s.f(0.32f, 0.5f) - lines * spacing / 2f
        val amp = s.f(30f, 90f)
        val freq = s.f(0.004f, 0.01f)
        val phaseStep = s.f(0.05f, 0.2f)
        val half = lines / 2f
        for (i in 0 until lines) {
            val p = Path()
            var x = -10f
            while (x <= s.w + 10f) {
                val env = sin(PI.toFloat() * (x / s.w).coerceIn(0f, 1f))
                val y = top + i * spacing + sin(x * freq + i * phaseStep) * amp * env + s.noise.at(x * 0.005f, i * 0.1f) * 20f
                if (x == -10f) p.moveTo(x, y) else p.lineTo(x, y)
                x += 6f
            }
            val alpha = (255 * (1f - abs(i - half) / half).coerceIn(0f, 1f).pow(0.6f)).toInt()
            s.canvas.drawPath(p, s.stroke(s.grad(i / lines.toFloat()), 1.6f, alpha))
        }
    }
}

object Mandala : Style("mandala", "Mandala", Category.ABSTRACT) {
    override fun draw(s: Scene) {
        val x = s.cx
        val y = s.h * 0.45f
        val k = s.i(6, 15)
        val layers = s.i(4, 8)
        val sector = TAU / k
        var r0 = 24f
        s.glowDot(x, y, 6f, s.color(0), 14f)
        for (layer in 0 until layers) {
            val r1 = r0 + s.f(40f, 62f)
            val color = s.color(layer)
            val paint = s.stroke(color, 2f, 230)
            val type = s.i(0, 3)
            val offset = if (layer % 2 == 0) 0f else sector / 2f
            for (j in 0 until k) {
                val a = j * sector + offset
                when (type) {
                    0 -> {
                        val p = Path()
                        p.moveTo(x + cos(a) * r0, y + sin(a) * r0)
                        val cr = (r0 + r1) / 2f
                        p.quadTo(x + cos(a - sector / 2f) * cr, y + sin(a - sector / 2f) * cr, x + cos(a) * r1, y + sin(a) * r1)
                        p.quadTo(x + cos(a + sector / 2f) * cr, y + sin(a + sector / 2f) * cr, x + cos(a) * r0, y + sin(a) * r0)
                        s.canvas.drawPath(p, paint)
                    }
                    1 -> {
                        val cr = (r0 + r1) / 2f
                        s.canvas.drawCircle(x + cos(a) * cr, y + sin(a) * cr, (r1 - r0) / 2.4f, paint)
                    }
                    else -> {
                        for (d in 0 until 3) {
                            val rr = r0 + (r1 - r0) * (d + 0.5f) / 3f
                            s.dot(x + cos(a) * rr, y + sin(a) * rr, 2.6f - d * 0.5f, color, 230)
                        }
                    }
                }
            }
            if (s.chance(0.5f)) s.canvas.drawCircle(x, y, r1 + 6f, s.stroke(color, 1f, 120))
            r0 = r1 + 8f
        }
    }
}

// ---------- Neon ----------

object NeonShapes : Style("neon_shapes", "Neon Shapes", Category.NEON) {
    override fun draw(s: Scene) {
        repeat(s.i(4, 8)) {
            val color = s.anyColor()
            val x = s.f(80f, s.w - 80f)
            val y = s.f(s.h * 0.1f, s.h * 0.9f)
            val size = s.f(80f, 240f)
            val rot = s.f(0f, TAU)
            val p = Path()
            when (s.i(0, 5)) {
                0 -> p.addCircle(x, y, size, Path.Direction.CW)
                1, 2 -> {
                    val sides = if (s.chance(0.5f)) 3 else 4
                    for (j in 0..sides) {
                        val a = rot + j * TAU / sides
                        if (j == 0) p.moveTo(x + cos(a) * size, y + sin(a) * size) else p.lineTo(x + cos(a) * size, y + sin(a) * size)
                    }
                    p.close()
                }
                3 -> {
                    val n = 6
                    for (j in 0..n) {
                        val px = x - size + 2f * size * j / n
                        val py = y + if (j % 2 == 0) -size * 0.25f else size * 0.25f
                        if (j == 0) p.moveTo(px, py) else p.lineTo(px, py)
                    }
                }
                else -> p.addArc(RectF(x - size, y - size, x + size, y + size), s.f(0f, 360f), s.f(90f, 260f))
            }
            s.glowPath(p, color, 4f, 22f)
        }
    }
}

object Circuit : Style("circuit", "Circuit", Category.NEON) {
    private val dirs = arrayOf(
        intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 1), intArrayOf(-1, 1),
        intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1),
    )

    override fun draw(s: Scene) {
        val g = 36f
        val cols = (s.w / g).toInt() + 1
        val rows = (s.h / g).toInt() + 1
        repeat(s.count(55)) {
            var x = s.i(0, cols) * g
            var y = s.i(0, rows) * g
            val sx = x
            val sy = y
            val color = s.anyColor()
            val p = Path()
            p.moveTo(x, y)
            var dir = s.i(0, 4) * 2
            repeat(s.i(2, 5)) {
                val len = s.i(2, 8)
                x += dirs[dir][0] * g * len
                y += dirs[dir][1] * g * len
                p.lineTo(x, y)
                dir = (dir + if (s.chance(0.5f)) 1 else 7) % 8
            }
            s.canvas.drawPath(p, s.stroke(color, 5f, 70, 10f))
            s.canvas.drawPath(p, s.stroke(color, 2.2f, 210))
            for ((nx, ny) in listOf(sx to sy, x to y)) {
                s.canvas.drawCircle(nx, ny, 6f, s.fill(Color.BLACK))
                s.canvas.drawCircle(nx, ny, 6f, s.stroke(color, 2.2f))
            }
        }
    }
}

object Synthwave : Style("synthwave", "Synthwave", Category.NEON) {
    override fun draw(s: Scene) {
        s.faintStars(120, s.h * 0.5f)
        val horizon = s.h * s.f(0.55f, 0.65f)
        val sunR = s.f(170f, 230f)
        val sunY = horizon - sunR * 0.55f
        val sunShader = LinearGradient(0f, sunY - sunR, 0f, sunY + sunR, s.color(2), s.color(0), Shader.TileMode.CLAMP)

        // Sun drawn as horizontal scanlines, with the gaps growing towards the horizon.
        var yy = sunY - sunR
        var gap = 2f
        while (yy < horizon) {
            val dy = yy - sunY
            if (abs(dy) < sunR) {
                val half = sqrt(sunR * sunR - dy * dy)
                s.canvas.drawLine(s.cx - half, yy, s.cx + half, yy, s.stroke(Color.WHITE, 2f).apply { shader = sunShader })
            }
            yy += 4f + gap
            if (yy > sunY) gap += 1.4f
        }
        val glow = s.stroke(Color.WHITE, 10f, 90, 30f).apply { shader = sunShader }
        s.canvas.drawArc(RectF(s.cx - sunR, sunY - sunR, s.cx + sunR, sunY + sunR), 180f, 180f, false, glow)

        // Perspective floor grid.
        val gridColor = s.color(1)
        s.canvas.drawLine(0f, horizon, s.w, horizon, s.stroke(gridColor, 6f, 120, 18f))
        s.canvas.drawLine(0f, horizon, s.w, horizon, s.stroke(gridColor, 2f))
        val rows = s.count(16)
        for (k in 1..rows) {
            val t = (k / rows.toFloat()).pow(2f)
            val y = horizon + (s.h - horizon) * t
            s.canvas.drawLine(0f, y, s.w, y, s.stroke(gridColor, 1.2f + t * 1.5f, (60 + 195 * t).toInt()))
        }
        for (i in -14..14) {
            s.canvas.drawLine(s.cx + i * 10f, horizon, s.cx + i * 150f, s.h, s.stroke(gridColor, 1.6f, 170))
        }
    }
}
