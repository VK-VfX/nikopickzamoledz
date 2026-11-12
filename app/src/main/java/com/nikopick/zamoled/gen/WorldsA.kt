package com.nikopick.zamoled.gen

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// =====================================================================
// MINIMAL: ink painting, lots of empty black
// =====================================================================

/** A windswept sakura tree on a cliff under a big sun. */
object LoneTree : Style("lone_tree", "Lone Tree", Category.MINIMAL) {
    override val autoPalettes = listOf("sakura", "lava", "sunset", "gold")

    override fun draw(s: Scene) {
        val sunColor = s.color(0)
        val blossom = s.color(1)
        val sx = s.w * s.f(0.55f, 0.7f)
        val sy = s.h * s.f(0.25f, 0.33f)
        val sr = s.f(150f, 200f)
        s.softGlow(sx, sy, sr * 1.8f, sunColor, 70)
        s.canvas.drawCircle(sx, sy, sr, s.fill(sunColor, 235))

        // Birds crossing the sun.
        repeat(s.i(3, 7) + s.count(4) - 4) {
            val bx = sx + s.f(-sr * 1.4f, sr * 1.2f)
            val by = sy + s.f(-sr * 0.9f, sr * 0.6f)
            val bw = s.f(8f, 16f)
            val p = Path()
            p.moveTo(bx - bw, by - bw * 0.3f)
            p.quadTo(bx - bw * 0.4f, by - bw * 0.6f, bx, by)
            p.quadTo(bx + bw * 0.4f, by - bw * 0.6f, bx + bw, by - bw * 0.3f)
            s.canvas.drawPath(p, s.stroke(Color.WHITE, 2f, 220))
        }

        // Cliff on the left.
        val cliffTop = s.h * s.f(0.68f, 0.74f)
        val edgeX = s.w * s.f(0.55f, 0.68f)
        val cliff = Path()
        cliff.moveTo(-10f, s.h + 10f)
        cliff.lineTo(-10f, cliffTop + 20f)
        var x = -10f
        while (x < edgeX) {
            cliff.lineTo(x, cliffTop + s.noise.at(x * 0.01f, 3f) * 18f)
            x += 12f
        }
        cliff.lineTo(edgeX + 20f, cliffTop + 40f)
        cliff.lineTo(edgeX - 10f, cliffTop + 160f)
        cliff.lineTo(edgeX + 30f, cliffTop + 300f)
        cliff.lineTo(edgeX - 30f, s.h + 10f)
        cliff.close()
        s.canvas.drawPath(cliff, s.fill(Color.BLACK))
        s.canvas.drawPath(cliff, s.stroke(Color.WHITE, 2.4f, 200))
        repeat(40) {
            val gx = s.f(0f, edgeX)
            val gy = cliffTop + s.noise.at(gx * 0.01f, 3f) * 18f
            s.canvas.drawLine(gx, gy, gx + s.f(-4f, 6f), gy - s.f(6f, 16f), s.stroke(Color.WHITE, 1.2f, 150))
        }

        // The tree: tapering recursive branches leaning with the wind.
        val tx = edgeX * s.f(0.45f, 0.7f)
        val ty = cliffTop + s.noise.at(tx * 0.01f, 3f) * 18f
        branch(s, tx, ty, -PI.toFloat() / 2f + 0.25f, s.f(230f, 290f), 22f, 9, blossom)

        // Petals drifting on the wind.
        repeat(s.count(40)) {
            val px = s.f(tx, s.w)
            val py = s.f(s.h * 0.3f, s.h * 0.9f)
            s.canvas.save()
            s.canvas.rotate(s.f(0f, 180f), px, py)
            s.canvas.drawOval(RectF(px - 4f, py - 2f, px + 4f, py + 2f), s.fill(blossom, s.i(140, 240)))
            s.canvas.restore()
        }

        // Hanko seal in the corner.
        val hx = s.w - 120f
        val hy = s.h - 200f
        s.canvas.drawRoundRect(RectF(hx, hy, hx + 56f, hy + 56f), 6f, 6f, s.fill(sunColor, 230))
        s.canvas.drawText("夜", hx + 28f, hy + 42f, s.textPaint(40f, Color.BLACK))
    }

    private fun branch(s: Scene, x: Float, y: Float, angle: Float, len: Float, width: Float, depth: Int, blossom: Int) {
        val wind = 0.12f
        val a = angle + wind
        val ex = x + cos(a) * len
        val ey = y + sin(a) * len
        val p = Path()
        p.moveTo(x, y)
        p.quadTo(x + cos(a + 0.3f) * len * 0.5f, y + sin(a + 0.3f) * len * 0.5f, ex, ey)
        s.canvas.drawPath(p, s.stroke(Color.WHITE, width, 235))
        if (depth == 0 || len < 14f) {
            repeat((s.i(4, 9) * s.density.coerceIn(0.5f, 2f)).toInt().coerceAtLeast(2)) {
                val r = s.f(3f, 8f)
                s.dot(ex + s.f(-16f, 16f), ey + s.f(-14f, 12f), r, if (s.chance(0.8f)) blossom else Color.WHITE, s.i(150, 250))
            }
            return
        }
        val kids = if (s.chance(0.3f)) 3 else 2
        repeat(kids) {
            val spread = s.f(0.25f, 0.6f) * if (it % 2 == 0) 1f else -1f
            branch(s, ex, ey, angle + spread, len * s.f(0.66f, 0.8f), width * 0.68f, depth - 1, blossom)
        }
    }
}

/** Two koi circling each other, lily pads and ripples. */
object KoiPond : Style("koi_pond", "Koi Pond", Category.MINIMAL) {
    override val autoPalettes = listOf("lava", "sunset", "gold", "sakura")

    override fun draw(s: Scene) {
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.52f)
        // Ripples.
        repeat(s.i(3, 6) + s.count(4) - 4) {
            val rx = s.f(0f, s.w)
            val ry = s.f(0f, s.h)
            for (k in 1..5) s.canvas.drawCircle(rx, ry, k * s.f(22f, 30f), s.stroke(Color.WHITE, 1.2f, 70 - k * 10))
        }
        // Lily pads and a lotus.
        val padColor = darken(mix(s.color(2), Color.rgb(60, 160, 90), 0.6f), 0.35f)
        repeat(s.i(4, 8) + s.count(5) - 5) {
            val px = s.f(0f, s.w)
            val py = s.f(0f, s.h)
            if (abs(py - cy) < 360f && abs(px - cx) < 360f) return@repeat
            lilyPad(s, px, py, s.f(50f, 95f), s.f(0f, 360f), padColor)
        }
        lotus(s, s.w * s.f(0.15f, 0.85f), cy + s.f(450f, 620f) * if (s.chance(0.5f)) 1f else -1f, s.f(40f, 55f))

        // The pair of koi chasing each other around a circle.
        val r = s.f(190f, 240f)
        val start = s.f(0f, TAU)
        koi(s, cx, cy, r, start, Color.WHITE, s.color(0))
        koi(s, cx, cy, r, start + PI.toFloat(), s.color(0), Color.WHITE)
    }

    private fun lilyPad(s: Scene, x: Float, y: Float, r: Float, rot: Float, color: Int) {
        val p = Path()
        p.moveTo(x, y)
        p.arcTo(RectF(x - r, y - r, x + r, y + r), rot + 15f, 330f, false)
        p.close()
        s.canvas.drawPath(p, s.fill(color, 230))
        s.canvas.drawPath(p, s.stroke(lighten(color, 0.4f), 2f, 200))
        for (k in 0 until 7) {
            val a = Math.toRadians((rot + 40f + k * 45f).toDouble()).toFloat()
            s.canvas.drawLine(x, y, x + cos(a) * r * 0.85f, y + sin(a) * r * 0.85f, s.stroke(lighten(color, 0.3f), 1.2f, 140))
        }
    }

    private fun lotus(s: Scene, x: Float, y: Float, size: Float) {
        s.softGlow(x, y, size * 2.5f, Color.rgb(255, 170, 200), 80)
        for (layer in 0 until 2) for (k in 0 until 8) {
            s.canvas.save()
            s.canvas.rotate(k * 45f + layer * 22.5f, x, y)
            val len = size * (1f - layer * 0.3f)
            val petal = Path()
            petal.moveTo(x, y)
            petal.quadTo(x - len * 0.35f, y - len * 0.5f, x, y - len)
            petal.quadTo(x + len * 0.35f, y - len * 0.5f, x, y)
            s.canvas.drawPath(petal, s.fill(if (layer == 0) Color.rgb(250, 160, 190) else Color.rgb(255, 225, 235), 235))
            s.canvas.drawPath(petal, s.stroke(Color.WHITE, 1.2f, 180))
            s.canvas.restore()
        }
        s.dot(x, y, size * 0.15f, Color.rgb(255, 210, 90))
    }

    /** A koi whose spine follows a circle, swimming clockwise. */
    private fun koi(s: Scene, cx: Float, cy: Float, r: Float, a0: Float, body: Int, spots: Int) {
        val span = 1.6f
        val n = 34
        val maxW = r * 0.22f
        val left = ArrayList<FloatArray>()
        val right = ArrayList<FloatArray>()
        val spine = ArrayList<FloatArray>()
        for (i in 0..n) {
            val t = i / n.toFloat() // 0 = tail, 1 = nose
            val a = a0 + span * t
            val px = cx + cos(a) * r
            val py = cy + sin(a) * r
            val wd = maxW * sin(PI.toFloat() * t).coerceAtLeast(0f).pow(0.7f) * (0.25f + 0.75f * t)
            spine.add(floatArrayOf(px, py, a))
            left.add(floatArrayOf(px + cos(a) * wd, py + sin(a) * wd))
            right.add(floatArrayOf(px - cos(a) * wd, py - sin(a) * wd))
        }
        // Tail fan.
        val tail = spine[0]
        val ta = tail[2]
        val back = floatArrayOf(sin(ta), -cos(ta)) // tangent pointing backwards (counter-clockwise)
        val fan = Path()
        fan.moveTo(tail[0], tail[1])
        fan.quadTo(tail[0] + back[0] * maxW * 1.2f + cos(ta) * maxW * 1.1f, tail[1] + back[1] * maxW * 1.2f + sin(ta) * maxW * 1.1f, tail[0] + back[0] * maxW * 2f + cos(ta) * maxW * 0.9f, tail[1] + back[1] * maxW * 2f + sin(ta) * maxW * 0.9f)
        fan.quadTo(tail[0] + back[0] * maxW * 1.3f, tail[1] + back[1] * maxW * 1.3f, tail[0] + back[0] * maxW * 2f - cos(ta) * maxW * 0.9f, tail[1] + back[1] * maxW * 2f - sin(ta) * maxW * 0.9f)
        fan.quadTo(tail[0] + back[0] * maxW * 1.2f - cos(ta) * maxW * 1.1f, tail[1] + back[1] * maxW * 1.2f - sin(ta) * maxW * 1.1f, tail[0], tail[1])
        fan.close()
        s.canvas.drawPath(fan, s.fill(body, 170))
        s.canvas.drawPath(fan, s.stroke(Color.WHITE, 1.4f, 160))
        // Pectoral fins.
        val fin = spine[(n * 0.7f).toInt()]
        for (sgn in floatArrayOf(1f, -1f)) {
            val fx = fin[0] + cos(fin[2]) * maxW * 0.8f * sgn
            val fy = fin[1] + sin(fin[2]) * maxW * 0.8f * sgn
            s.canvas.save()
            s.canvas.rotate(Math.toDegrees(fin[2].toDouble()).toFloat() + 60f * sgn, fx, fy)
            s.canvas.drawOval(RectF(fx - maxW * 0.6f, fy - maxW * 0.25f, fx + maxW * 0.6f, fy + maxW * 0.25f), s.fill(body, 150))
            s.canvas.restore()
        }
        // Body.
        val bodyPath = Path()
        bodyPath.moveTo(left[0][0], left[0][1])
        for (p in left) bodyPath.lineTo(p[0], p[1])
        for (p in right.asReversed()) bodyPath.lineTo(p[0], p[1])
        bodyPath.close()
        s.softGlow(spine[n / 2][0], spine[n / 2][1], r * 0.5f, body, 50)
        s.canvas.drawPath(bodyPath, s.fill(body))
        s.canvas.save()
        s.canvas.clipPath(bodyPath)
        repeat(s.i(3, 6)) {
            val sp = spine[s.i(4, n - 3)]
            val rr = maxW * s.f(0.5f, 1.1f)
            s.canvas.drawCircle(sp[0] + s.f(-maxW, maxW) * 0.5f, sp[1] + s.f(-maxW, maxW) * 0.5f, rr, s.fill(spots))
        }
        s.canvas.restore()
        s.canvas.drawPath(bodyPath, s.stroke(Color.WHITE, 1.6f, 200))
        // Eyes near the nose.
        val head = spine[n - 3]
        for (sgn in floatArrayOf(1f, -1f)) {
            s.dot(head[0] + cos(head[2]) * maxW * 0.35f * sgn, head[1] + sin(head[2]) * maxW * 0.35f * sgn, 3.2f, Color.BLACK)
        }
    }
}

/** A figure on a crescent moon, fishing a star out of the night. */
object MoonFisher : Style("moon_fisher", "Moon Fisher", Category.MINIMAL) {
    override val autoPalettes = listOf("gold", "mono", "sakura", "cyan", "sunset")

    override fun draw(s: Scene) {
        s.starSky(220)
        val mc = lighten(s.color(0), 0.35f)
        val mx = s.w * s.f(0.45f, 0.6f)
        val my = s.h * s.f(0.3f, 0.38f)
        val mr = s.f(210f, 250f)
        s.softGlow(mx, my, mr * 2f, mc, 60)
        val moon = Path()
        moon.addCircle(mx, my, mr, Path.Direction.CW)
        val bite = Path()
        bite.addCircle(mx + mr * 0.45f, my - mr * 0.25f, mr * 0.88f, Path.Direction.CW)
        moon.op(bite, Path.Op.DIFFERENCE)
        s.canvas.drawPath(moon, s.fill(mc))
        // Craters.
        s.canvas.save()
        s.canvas.clipPath(moon)
        repeat(8) { s.canvas.drawCircle(mx + s.f(-mr, 0f), my + s.f(-mr * 0.2f, mr), s.f(6f, 18f), s.fill(darken(mc, 0.15f))) }
        s.canvas.restore()

        // The fisher sits on the lower horn of the moon.
        val sitX = mx - mr * 0.15f
        val sitY = my + mr * 0.93f
        val ink = Color.BLACK
        val rim = mc
        val body = Path()
        body.addRoundRect(RectF(sitX - 16f, sitY - 70f, sitX + 16f, sitY), 14f, 14f, Path.Direction.CW)
        s.canvas.drawPath(body, s.fill(ink))
        s.canvas.drawPath(body, s.stroke(rim, 2f))
        s.canvas.drawCircle(sitX, sitY - 88f, 15f, s.fill(ink))
        s.canvas.drawCircle(sitX, sitY - 88f, 15f, s.stroke(rim, 2f))
        // Pointed hat.
        val hat = Path()
        hat.moveTo(sitX - 20f, sitY - 96f); hat.lineTo(sitX + 4f, sitY - 140f); hat.lineTo(sitX + 20f, sitY - 96f); hat.close()
        s.canvas.drawPath(hat, s.fill(ink))
        s.canvas.drawPath(hat, s.stroke(rim, 2f))
        // Legs dangling over the edge.
        for (dx in floatArrayOf(-8f, 8f)) {
            val leg = Path()
            leg.moveTo(sitX + dx, sitY - 4f)
            leg.quadTo(sitX + dx - 4f, sitY + 30f, sitX + dx + 6f, sitY + 52f)
            s.limb(leg, ink, rim, 7f, 2f)
        }
        // Rod and line down to a hooked star.
        val rodTipX = sitX - 160f
        val rodTipY = sitY - 170f
        s.canvas.drawLine(sitX - 6f, sitY - 40f, rodTipX, rodTipY, s.stroke(rim, 3f))
        val starX = rodTipX - s.f(0f, 40f)
        val starY = s.h * s.f(0.66f, 0.76f)
        val line = Path()
        line.moveTo(rodTipX, rodTipY)
        line.quadTo(rodTipX - 30f, (rodTipY + starY) / 2f, starX, starY - 26f)
        s.canvas.drawPath(line, s.stroke(Color.WHITE, 1.2f, 200))
        s.softGlow(starX, starY, 120f, s.color(1), 120)
        s.unit(starX, starY, 30f, s.f(-15f, 15f)) {
            s.canvas.drawPath(Doodles.starPath(5, 1f, 0.45f), s.fill(lighten(s.color(1), 0.4f)))
        }
        // Clouds drifting below.
        repeat(s.i(2, 4)) {
            val cx = s.f(80f, s.w - 80f)
            val cy = s.h * s.f(0.78f, 0.92f)
            val cloud = Path()
            for (k in 0 until 4) cloud.addCircle(cx + k * 38f - 57f, cy - if (k % 3 == 0) 0f else 18f, if (k % 3 == 0) 26f else 34f, Path.Direction.CW)
            s.canvas.drawPath(cloud, s.fill(Color.BLACK))
            s.canvas.drawPath(cloud, s.stroke(Color.WHITE, 1.6f, 120))
        }
    }
}
