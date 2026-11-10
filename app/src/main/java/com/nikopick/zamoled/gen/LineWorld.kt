package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Line wallpapers: a few thin strokes and nothing else, so nearly every pixel stays off.
 * These are the most AMOLED-friendly pictures in the app.
 */

private fun Scene.ink(): Int = lighten(color(0), 0.55f)

/** Invented constellations: a handful of stars joined by hairlines, with a faint field behind. */
object LineConstellation : Style("line_constellation", "Constellation", Category.MINIMAL) {
    override val autoPalettes = listOf("mono", "gold", "ocean", "violet", "sakura")

    override fun draw(s: Scene) {
        val ink = s.ink()
        repeat(s.count(70)) { s.dot(s.f(0f, s.w), s.f(0f, s.h), s.f(0.5f, 1.3f), Color.WHITE, s.i(40, 150)) }
        val groups = s.count(2).coerceIn(1, 4)
        for (g in 0 until groups) {
            val cx = s.f(s.w * 0.25f, s.w * 0.75f)
            val cy = s.h * (0.3f + 0.5f * (g + s.f(0.2f, 0.8f)) / groups)
            val n = s.i(5, 9)
            val pts = ArrayList<FloatArray>()
            var x = cx - s.w * 0.28f
            var y = cy + s.f(-80f, 80f)
            repeat(n) {
                pts += floatArrayOf(x, y)
                x += s.f(50f, 130f)
                y += s.f(-110f, 110f)
            }
            val line = Path()
            for ((i, p) in pts.withIndex()) if (i == 0) line.moveTo(p[0], p[1]) else line.lineTo(p[0], p[1])
            // One branch off the main chain, the way real figures fork.
            val fork = pts[s.i(1, n - 1)]
            line.moveTo(fork[0], fork[1]); line.lineTo(fork[0] + s.f(-90f, 90f), fork[1] + s.f(90f, 170f))
            s.canvas.drawPath(line, s.stroke(ink, 1.6f, 170))
            for (p in pts) {
                val r = s.f(3f, 7f)
                s.canvas.drawCircle(p[0], p[1], r * 2.2f, s.fill(Color.BLACK))
                s.glowDot(p[0], p[1], r, Color.WHITE, r * 2.5f)
            }
        }
    }
}

/** A whole landscape in one unbroken stroke: water, mountains and a sun looped into the line. */
object OneLine : Style("one_line", "One Line", Category.MINIMAL) {
    override val autoPalettes = listOf("mono", "gold", "sunset", "ocean", "sakura")

    override fun draw(s: Scene) {
        val y0 = s.h * s.f(0.56f, 0.66f)
        val p = Path()
        var x = -20f
        p.moveTo(x, y0)
        // Water in.
        while (x < s.w * 0.2f) { x += 8f; p.lineTo(x, y0 + sin(x * 0.05f) * 9f) }
        // Mountains: rises and falls, each a pair of straight-ish runs.
        val peaks = s.i(2, 4) + (s.density - 1f).toInt().coerceAtLeast(0)
        val span = s.w * 0.6f / peaks
        var sunDone = false
        for (k in 0 until peaks) {
            val top = y0 - s.h * s.f(0.1f, 0.24f)
            val mid = x + span * s.f(0.4f, 0.6f)
            p.lineTo(mid, top)
            if (!sunDone && k == peaks / 2) {
                // At the summit the line leaves the ground, loops a sun and returns.
                val r = s.w * s.f(0.07f, 0.1f)
                val sx = mid + r * 1.2f
                val sy = top - s.h * 0.1f
                p.quadTo(mid + r * 0.2f, sy + r, sx - r, sy)
                p.arcTo(RectF(sx - r, sy - r, sx + r, sy + r), 180f, 359f)
                p.quadTo(mid + r * 0.4f, sy + r * 1.6f, mid + 6f, top + 10f)
                sunDone = true
            }
            x += span
            p.lineTo(x, y0 - s.f(0f, s.h * 0.04f))
        }
        p.lineTo(x + 10f, y0)
        // Water out.
        while (x < s.w + 20f) { x += 8f; p.lineTo(x, y0 + sin(x * 0.05f) * 9f) }
        s.canvas.drawPath(p, s.stroke(s.ink(), 3.2f))
        // Its reflection: the same idea, broken into dashes.
        var ry = y0 + 40f
        var k = 0
        while (ry < y0 + 40f + s.count(5) * 34f) {
            val inset = 60f + k * 50f
            var rx = inset
            while (rx < s.w - inset) {
                val len = s.f(20f, 70f)
                s.canvas.drawLine(rx, ry, rx + len, ry, s.stroke(s.ink(), 2f, 150 - k * 22))
                rx += len + s.f(16f, 50f)
            }
            ry += 34f
            k++
        }
    }
}

/** Contour lines around a couple of summits, as on a walking map. */
object Contours : Style("contours", "Contours", Category.MINIMAL) {
    override val autoPalettes = listOf("mono", "aurora", "ocean", "gold", "violet")

    override fun draw(s: Scene) {
        val ink = s.ink()
        val peaks = s.i(1, 3)
        for (k in 0 until peaks) {
            val cx = s.w * s.f(0.2f, 0.8f)
            val cy = s.h * s.f(0.35f, 0.8f)
            val rings = s.count(9)
            val step = s.f(30f, 44f)
            val off = s.f(0f, 100f)
            for (r in 1..rings) {
                val path = Path()
                for (i in 0..120) {
                    val a = (2.0 * PI * i / 120).toFloat()
                    // The noise changes slowly from ring to ring, so neighbours stay roughly parallel.
                    val wob = s.noise.fbm(cos(a) * 1.3f + off, sin(a) * 1.3f + r * 0.11f) * 0.42f
                    val rr = r * step * (1f + wob)
                    val x = cx + cos(a) * rr
                    val y = cy + sin(a) * rr * 0.86f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                s.canvas.drawPath(path, s.stroke(ink, if (r % 4 == 0) 2.2f else 1.2f, (210 - r * 12).coerceAtLeast(50)))
            }
            s.canvas.drawLine(cx - 8f, cy, cx + 8f, cy, s.stroke(s.color(1), 2f))
            s.canvas.drawLine(cx, cy - 8f, cx, cy + 8f, s.stroke(s.color(1), 2f))
        }
    }
}

/** An orrery: a small sun, hairline orbits at a tilt, and a planet on each. */
object Orbits : Style("orbits", "Orbits", Category.MINIMAL) {
    override val autoPalettes = listOf("mono", "gold", "ocean", "violet", "sunset")

    override fun draw(s: Scene) {
        val ink = s.ink()
        val cx = s.cx + s.f(-60f, 60f)
        val cy = s.h * s.f(0.5f, 0.62f)
        val tilt = s.f(-28f, 28f)
        val squash = s.f(0.3f, 0.5f)
        val n = s.count(5).coerceIn(2, 10)
        s.canvas.save()
        s.canvas.rotate(tilt, cx, cy)
        for (k in 1..n) {
            val r = 60f + k * (s.w * 0.44f - 60f) / n
            val rect = RectF(cx - r, cy - r * squash, cx + r, cy + r * squash)
            s.canvas.drawOval(rect, s.stroke(ink, 1.3f, 150))
            val a = s.f(0f, (2 * PI).toFloat())
            val px = cx + cos(a) * r
            val py = cy + sin(a) * r * squash
            val pr = s.f(4f, 11f)
            s.canvas.drawCircle(px, py, pr + 5f, s.fill(Color.BLACK))
            s.canvas.drawCircle(px, py, pr, s.fill(s.color(k)))
            if (s.chance(0.3f)) s.canvas.drawOval(RectF(px - pr * 2f, py - pr * 0.5f, px + pr * 2f, py + pr * 0.5f), s.stroke(ink, 1.2f, 200))
        }
        s.canvas.restore()
        s.glowDot(cx, cy, 13f, lighten(s.color(0), 0.4f), 34f)
    }
}

/**
 * For the lock screen and always-on display: one small, dim mark in the lower middle of an
 * otherwise black screen. More than 99% of the pixels are off.
 */
object Glance : Style("glance", "Glance", Category.MINIMAL) {
    override val autoPalettes = listOf("mono", "gold", "ocean", "violet", "sakura", "aurora")

    override fun draw(s: Scene) {
        val ink = darken(lighten(s.color(0), 0.5f), 0.15f)
        val cx = s.cx
        val cy = s.h * 0.66f
        val u = s.w * 0.075f * (0.8f + 0.2f * s.density)
        val line = s.stroke(ink, 2.4f)
        when (s.i(0, 7)) {
            0 -> { // crescent
                val moon = Path().apply { addCircle(cx, cy, u, Path.Direction.CW) }
                moon.op(Path().apply { addCircle(cx + u * 0.45f, cy - u * 0.15f, u * 0.9f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
                s.canvas.drawPath(moon, s.fill(ink))
            }
            1 -> { // ring and satellite
                s.canvas.drawCircle(cx, cy, u, line)
                s.canvas.drawCircle(cx + u * 0.72f, cy - u * 0.72f, u * 0.14f, s.fill(s.color(1)))
            }
            2 -> { // three stars joined
                val pts = floatArrayOf(-1.2f, 0.3f, -0.2f, -0.5f, 0.6f, 0.2f, 1.3f, -0.4f)
                val p = Path()
                for (i in 0 until 4) if (i == 0) p.moveTo(cx + pts[0] * u, cy + pts[1] * u) else p.lineTo(cx + pts[i * 2] * u, cy + pts[i * 2 + 1] * u)
                s.canvas.drawPath(p, s.stroke(ink, 1.4f, 160))
                for (i in 0 until 4) s.canvas.drawCircle(cx + pts[i * 2] * u, cy + pts[i * 2 + 1] * u, 3.6f, s.fill(Color.WHITE))
            }
            3 -> { // heartbeat
                val p = Path().apply {
                    moveTo(cx - u * 2f, cy); lineTo(cx - u * 0.7f, cy); lineTo(cx - u * 0.35f, cy - u * 0.9f)
                    lineTo(cx + u * 0.05f, cy + u * 0.7f); lineTo(cx + u * 0.4f, cy); lineTo(cx + u * 2f, cy)
                }
                s.canvas.drawPath(p, line)
            }
            4 -> { // sunrise
                s.canvas.drawArc(RectF(cx - u, cy - u, cx + u, cy + u), 180f, 180f, false, line)
                s.canvas.drawLine(cx - u * 1.8f, cy, cx + u * 1.8f, cy, line)
                for (k in 0 until 5) {
                    val a = PI.toFloat() + (k + 0.5f) * PI.toFloat() / 5f
                    s.canvas.drawLine(cx + cos(a) * u * 1.3f, cy + sin(a) * u * 1.3f, cx + cos(a) * u * 1.6f, cy + sin(a) * u * 1.6f, s.stroke(ink, 1.6f))
                }
            }
            5 -> { // a little flower of circles
                for (k in 0 until 6) {
                    val a = (PI / 3 * k).toFloat()
                    s.canvas.drawCircle(cx + cos(a) * u * 0.5f, cy + sin(a) * u * 0.5f, u * 0.5f, s.stroke(ink, 1.3f, 190))
                }
            }
            else -> { // three dots, breathing in size
                for (k in -1..1) s.canvas.drawCircle(cx + k * u * 0.8f, cy, u * (0.14f + 0.05f * (1 - k * k)), s.fill(ink))
            }
        }
    }
}

object LineWorld {
    val styles: List<Style> = listOf(LineConstellation, OneLine, Contours, Orbits, Glance)
}
