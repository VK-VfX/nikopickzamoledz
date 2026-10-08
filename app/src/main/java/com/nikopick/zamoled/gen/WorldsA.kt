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
        repeat(s.i(3, 7)) {
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
            repeat(s.i(4, 9)) {
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
        repeat(s.i(3, 6)) {
            val rx = s.f(0f, s.w)
            val ry = s.f(0f, s.h)
            for (k in 1..5) s.canvas.drawCircle(rx, ry, k * s.f(22f, 30f), s.stroke(Color.WHITE, 1.2f, 70 - k * 10))
        }
        // Lily pads and a lotus.
        val padColor = darken(mix(s.color(2), Color.rgb(60, 160, 90), 0.6f), 0.35f)
        repeat(s.i(4, 8)) {
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
            val wd = maxW * sin(PI.toFloat() * t).pow(0.7f) * (0.25f + 0.75f * t)
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

// =====================================================================
// GEOMETRIC: crystals and architecture
// =====================================================================

object CrystalCave : Style("crystal_cave", "Crystal Cave", Category.GEOMETRIC) {
    override val autoPalettes = listOf("violet", "cyan", "aurora", "sakura", "hue")

    override fun draw(s: Scene) {
        // Cave walls.
        for (side in 0..1) {
            val wall = Path()
            val x0 = if (side == 0) -10f else s.w + 10f
            wall.moveTo(x0, -10f)
            var y = -10f
            while (y < s.h + 20f) {
                val depth = 60f + (s.noise.fbm(y * 0.004f, side * 9f) + 1f) * 90f
                wall.lineTo(if (side == 0) depth else s.w - depth, y)
                y += 20f
            }
            wall.lineTo(x0, s.h + 10f)
            wall.close()
            s.canvas.drawPath(wall, s.fill(Color.BLACK))
            s.canvas.drawPath(wall, s.stroke(s.color(2), 1.6f, 110))
        }
        s.softGlow(s.cx, s.h * 0.85f, 520f, s.color(0), 90)
        // Hanging crystals from the ceiling.
        repeat(s.i(5, 9)) {
            crystal(s, s.f(80f, s.w - 80f), s.f(-10f, 40f), s.f(80f, 200f), s.f(16f, 34f), 180f + s.f(-20f, 20f), s.anyColor())
        }
        // Main cluster on the floor.
        val baseY = s.h * s.f(0.86f, 0.9f)
        repeat(s.count(9).coerceIn(5, 16)) {
            val x = s.cx + s.gauss() * 220f
            crystal(s, x, baseY + s.f(-10f, 30f), s.f(160f, 430f), s.f(34f, 66f), s.f(-30f, 30f), s.anyColor())
        }
        repeat(s.i(6, 12)) {
            crystal(s, s.f(60f, s.w - 60f), s.h * s.f(0.88f, 0.98f), s.f(60f, 140f), s.f(14f, 26f), s.f(-40f, 40f), s.anyColor())
        }
        // Floating sparkles.
        repeat(s.count(70)) {
            s.glowDot(s.f(0f, s.w), s.f(s.h * 0.2f, s.h), s.f(0.8f, 2.4f), lighten(s.anyColor(), 0.4f), 5f, s.i(120, 255))
        }
    }

    private fun crystal(s: Scene, x: Float, y: Float, len: Float, wd: Float, angle: Float, color: Int) {
        s.canvas.save()
        s.canvas.rotate(angle, x, y)
        val tipY = y - len
        val shoulder = y - len * 0.78f
        val leftFace = Path()
        leftFace.moveTo(x - wd, y); leftFace.lineTo(x - wd * 0.92f, shoulder); leftFace.lineTo(x, tipY); leftFace.lineTo(x + wd * 0.15f, y); leftFace.close()
        val rightFace = Path()
        rightFace.moveTo(x + wd * 0.15f, y); rightFace.lineTo(x, tipY); rightFace.lineTo(x + wd * 0.92f, shoulder); rightFace.lineTo(x + wd, y); rightFace.close()
        s.canvas.drawPath(leftFace, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(x, y, x, tipY, withAlpha(color, 230), withAlpha(darken(color, 0.55f), 200), Shader.TileMode.CLAMP)
        })
        s.canvas.drawPath(rightFace, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(x, y, x, tipY, withAlpha(lighten(color, 0.35f), 240), withAlpha(darken(color, 0.3f), 200), Shader.TileMode.CLAMP)
        })
        val outline = s.stroke(lighten(color, 0.6f), 1.6f, 230)
        s.canvas.drawPath(leftFace, outline)
        s.canvas.drawPath(rightFace, outline)
        s.canvas.drawLine(x - wd * 0.5f, y - len * 0.3f, x - wd * 0.3f, y - len * 0.6f, s.stroke(Color.WHITE, 1.2f, 160))
        s.canvas.restore()
    }
}

object ArtDecoTower : Style("art_deco", "Art Deco", Category.GEOMETRIC) {
    override val autoPalettes = listOf("gold", "gold", "mono", "cyan", "sunset")

    override fun draw(s: Scene) {
        val gold = s.color(0)
        val line = s.stroke(gold, 2f, 230)
        val thin = s.stroke(gold, 1.2f, 140)
        val horizon = s.h * 0.82f
        // Sunburst behind the tower.
        for (k in 0..36) {
            val a = PI.toFloat() + k * PI.toFloat() / 36f
            s.canvas.drawLine(s.cx, horizon, s.cx + cos(a) * 1400f, horizon + sin(a) * 1400f, s.stroke(gold, 1.2f, if (k % 2 == 0) 70 else 35))
        }
        for (r in 1..6) s.canvas.drawArc(RectF(s.cx - r * 120f, horizon - r * 120f, s.cx + r * 120f, horizon + r * 120f), 180f, 180f, false, s.stroke(gold, 1.4f, 60))

        // Stepped tower.
        val tiers = 6
        var top = horizon
        var half = s.f(170f, 210f)
        for (t in 0 until tiers) {
            val hgt = s.f(120f, 190f) * (1f - t * 0.06f)
            val rect = RectF(s.cx - half, top - hgt, s.cx + half, top)
            s.canvas.drawRect(rect, s.fill(Color.BLACK))
            s.canvas.drawRect(rect, line)
            var px = rect.left + 14f
            while (px < rect.right - 8f) {
                s.canvas.drawLine(px, rect.top + 10f, px, rect.bottom - 6f, thin)
                px += 18f
            }
            // Lit windows.
            repeat((half / 18f).toInt()) {
                if (s.chance(0.3f)) {
                    val wx = rect.left + 14f + 18f * s.i(0, (half * 2f / 18f).toInt().coerceAtLeast(1))
                    val wy = s.f(rect.top + 16f, rect.bottom - 20f)
                    s.canvas.drawRect(wx - 3f, wy, wx + 3f, wy + 12f, s.fill(lighten(gold, 0.4f), 230))
                }
            }
            top -= hgt
            half *= 0.78f
        }
        // Crown: stacked arches with radiating triangles, then the spire.
        for (k in 0 until 4) {
            val r = half * (1.1f - k * 0.22f)
            val rect = RectF(s.cx - r, top - r, s.cx + r, top + r)
            s.canvas.drawArc(rect, 180f, 180f, true, s.fill(Color.BLACK))
            s.canvas.drawArc(rect, 180f, 180f, false, line)
            for (j in 1 until 8) {
                val a = PI.toFloat() + j * PI.toFloat() / 8f
                s.canvas.drawLine(s.cx + cos(a) * r * 0.55f, top + sin(a) * r * 0.55f, s.cx + cos(a) * r, top + sin(a) * r, thin)
            }
            top -= r * 0.35f
        }
        val spireTop = top - s.f(160f, 240f)
        s.canvas.drawLine(s.cx, top, s.cx, spireTop, s.stroke(gold, 3f))
        s.glowDot(s.cx, spireTop, 4f, Color.WHITE, 16f)

        // Poster frame and title.
        val m = 40f
        s.canvas.drawRect(m, m, s.w - m, s.h - m, line)
        s.canvas.drawRect(m + 12f, m + 12f, s.w - m - 12f, s.h - m - 12f, thin)
        for ((cx, cy) in listOf(m to m, s.w - m to m, m to s.h - m, s.w - m to s.h - m)) {
            for (k in 1..3) s.canvas.drawCircle(cx, cy, k * 10f, thin)
        }
        val tp = s.textPaint(46f, gold, Typeface.create(Typeface.SERIF, Typeface.BOLD)).apply { letterSpacing = 0.4f }
        s.canvas.drawLine(s.w * 0.2f, horizon + 60f, s.w * 0.8f, horizon + 60f, thin)
        s.canvas.drawText(arrayOf("METROPOLIS", "MIDNIGHT", "GRAND HOTEL", "ZAMOLED").let { it[s.i(0, it.size)] }, s.cx, horizon + 130f, tp)
        s.canvas.drawLine(s.w * 0.2f, horizon + 160f, s.w * 0.8f, horizon + 160f, thin)
    }
}

object FloatingIsle : Style("floating_isle", "Floating Isle", Category.GEOMETRIC) {
    override val autoPalettes = listOf("aurora", "ocean", "candy", "sunset", "gold")

    override fun draw(s: Scene) {
        s.starSky(180)
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.48f)
        val u = s.f(30f, 36f)
        fun iso(x: Float, y: Float, z: Float): FloatArray =
            floatArrayOf(cx + (x - y) * 0.866f * u, cy + (x + y) * 0.5f * u - z * u)

        val grass = mix(s.color(0), Color.rgb(90, 200, 120), 0.5f)
        val earth = Color.rgb(150, 100, 70)
        val n = 6f
        val thick = 2.2f
        // Hanging rock cone under the island.
        val bl = iso(-n, n, -thick); val br = iso(n, -n, -thick); val fr = iso(n, n, -thick)
        val tip = floatArrayOf(cx + s.f(-30f, 30f), cy + n * u * 1.9f)
        val cone = Path()
        cone.moveTo(bl[0], bl[1]); cone.lineTo(fr[0], fr[1]); cone.lineTo(br[0], br[1]); cone.lineTo(tip[0], tip[1]); cone.close()
        s.canvas.drawPath(cone, s.vGradient(fr[1], tip[1], darken(earth, 0.5f), Color.BLACK))
        s.canvas.drawPath(cone, s.stroke(darken(earth, 0.1f), 2f))
        repeat(10) {
            val rx = s.f(bl[0], br[0])
            val ry = fr[1] + s.f(0f, 80f)
            val root = Path()
            root.moveTo(rx, ry); root.quadTo(rx + s.f(-20f, 20f), ry + 60f, rx + s.f(-10f, 10f), ry + s.f(80f, 160f))
            s.canvas.drawPath(root, s.stroke(darken(earth, 0.2f), 1.4f, 180))
        }
        // Side faces.
        fun quad(a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray, fill: Paint, edge: Int) {
            val p = Path(); p.moveTo(a[0], a[1]); p.lineTo(b[0], b[1]); p.lineTo(c[0], c[1]); p.lineTo(d[0], d[1]); p.close()
            s.canvas.drawPath(p, fill); s.canvas.drawPath(p, s.stroke(edge, 2f))
        }
        quad(iso(-n, n, 0f), iso(n, n, 0f), iso(n, n, -thick), iso(-n, n, -thick), s.fill(darken(earth, 0.35f)), earth)
        quad(iso(n, n, 0f), iso(n, -n, 0f), iso(n, -n, -thick), iso(n, n, -thick), s.fill(darken(earth, 0.15f)), earth)
        for (k in 1..3) {
            val z = -thick * k / 4f
            val a = iso(-n, n, z); val b = iso(n, n, z); val c = iso(n, -n, z)
            s.canvas.drawLine(a[0], a[1], b[0], b[1], s.stroke(earth, 1.2f, 140))
            s.canvas.drawLine(b[0], b[1], c[0], c[1], s.stroke(earth, 1.2f, 140))
        }
        // Grass top.
        quad(iso(-n, -n, 0f), iso(n, -n, 0f), iso(n, n, 0f), iso(-n, n, 0f), s.fill(darken(grass, 0.45f)), grass)

        // Waterfall off the right edge.
        val wf = iso(n, 1.5f, 0f)
        repeat(18) { k ->
            val x = wf[0] + k * 2.4f - 20f
            s.canvas.drawLine(x, wf[1], x + s.f(-4f, 4f), wf[1] + s.f(380f, 620f), s.stroke(lighten(s.color(1), 0.5f), 2f, s.i(60, 170)))
        }
        s.softGlow(wf[0], wf[1] + 520f, 120f, s.color(1), 70)

        // Cottage: cube with a pitched roof and a warm window.
        val hx = -2.5f; val hy = -1.5f; val hs = 2.4f; val hh = 2.2f
        quad(iso(hx, hy + hs, 0f), iso(hx + hs, hy + hs, 0f), iso(hx + hs, hy + hs, hh), iso(hx, hy + hs, hh), s.fill(Color.rgb(40, 30, 30)), Color.rgb(240, 220, 200))
        quad(iso(hx + hs, hy + hs, 0f), iso(hx + hs, hy, 0f), iso(hx + hs, hy, hh), iso(hx + hs, hy + hs, hh), s.fill(Color.rgb(60, 45, 40)), Color.rgb(240, 220, 200))
        val ridgeA = iso(hx + hs / 2f, hy, hh + 1.3f); val ridgeB = iso(hx + hs / 2f, hy + hs, hh + 1.3f)
        quad(iso(hx, hy + hs, hh), iso(hx + hs, hy + hs, hh), ridgeB, ridgeB, s.fill(darken(s.color(0), 0.3f)), s.color(0))
        quad(iso(hx + hs, hy + hs, hh), iso(hx + hs, hy, hh), ridgeA, ridgeB, s.fill(darken(s.color(0), 0.5f)), s.color(0))
        val win = iso(hx + hs, hy + hs * 0.5f, hh * 0.5f)
        s.softGlow(win[0], win[1], 60f, Color.rgb(255, 200, 100), 160)
        s.canvas.drawRect(win[0] - 9f, win[1] - 12f, win[0] + 9f, win[1] + 12f, s.fill(Color.rgb(255, 214, 120)))
        val door = iso(hx + hs * 0.5f, hy + hs, 0f)
        s.canvas.drawRect(door[0] - 10f, door[1] - 34f, door[0] + 10f, door[1] - 2f, s.fill(Color.rgb(120, 80, 50)))

        // Round tree.
        val tr = iso(3f, 2.5f, 0f)
        s.canvas.drawLine(tr[0], tr[1], tr[0], tr[1] - 60f, s.stroke(Color.rgb(140, 95, 60), 8f))
        for ((dx, dy, rr) in listOf(Triple(0f, -90f, 46f), Triple(-28f, -70f, 32f), Triple(28f, -72f, 34f))) {
            s.canvas.drawCircle(tr[0] + dx, tr[1] + dy, rr, s.fill(darken(grass, 0.3f)))
            s.canvas.drawCircle(tr[0] + dx, tr[1] + dy, rr, s.stroke(grass, 2f))
        }

        // Pebble islands and clouds.
        repeat(s.i(3, 6)) {
            val px = s.f(60f, s.w - 60f)
            val py = s.f(s.h * 0.15f, s.h * 0.85f)
            if (abs(px - cx) < 260f && abs(py - cy) < 340f) return@repeat
            val r = s.f(14f, 30f)
            val rock = Path()
            rock.moveTo(px - r, py); rock.lineTo(px + r, py); rock.lineTo(px + s.f(-5f, 5f), py + r * 1.6f); rock.close()
            s.canvas.drawPath(rock, s.fill(darken(earth, 0.5f)))
            s.canvas.drawLine(px - r, py, px + r, py, s.stroke(grass, 4f))
        }
        repeat(s.i(2, 4)) {
            val ccx = s.f(80f, s.w - 80f)
            val ccy = s.f(s.h * 0.1f, s.h * 0.9f)
            for (k in 0 until 3) s.canvas.drawCircle(ccx + k * 34f - 34f, ccy - if (k == 1) 16f else 0f, if (k == 1) 34f else 24f, s.stroke(Color.WHITE, 1.6f, 110))
        }
    }
}

// =====================================================================
// SPACE: illustrated sci-fi
// =====================================================================

object AstronautDrift : Style("astronaut_drift", "Astronaut", Category.SPACE) {
    override val autoPalettes = listOf("ocean", "cyan", "gold", "sunset")

    override fun draw(s: Scene) {
        s.starSky(260)
        // Earth limb glowing at the bottom.
        val er = 1250f
        val ecx = s.cx + s.f(-200f, 200f)
        val ecy = s.h + er * 0.62f
        s.canvas.drawCircle(ecx, ecy, er + 40f, s.stroke(s.color(1), 70f, 60, 60f))
        val earth = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(ecx - er * 0.3f, ecy - er * 0.8f, er * 1.4f, intArrayOf(darken(s.color(0), 0.2f), darken(s.color(0), 0.75f), Color.BLACK), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        s.canvas.drawCircle(ecx, ecy, er, earth)
        val clip = Path(); clip.addCircle(ecx, ecy, er, Path.Direction.CW)
        s.canvas.save(); s.canvas.clipPath(clip)
        repeat(26) {
            val y = ecy - er + s.f(0f, 340f)
            val x = s.f(-100f, s.w + 100f)
            val cloud = Path()
            cloud.moveTo(x, y); cloud.cubicTo(x + 60f, y - 20f, x + 140f, y + 20f, x + s.f(180f, 320f), y + s.f(-10f, 10f))
            s.canvas.drawPath(cloud, s.stroke(Color.WHITE, s.f(4f, 12f), s.i(40, 120), 4f))
        }
        s.canvas.restore()
        s.canvas.drawCircle(ecx, ecy, er, s.stroke(lighten(s.color(1), 0.4f), 3f, 220))

        // Astronaut.
        val ax = s.w * s.f(0.4f, 0.6f)
        val ay = s.h * s.f(0.36f, 0.44f)
        val size = s.f(1.0f, 1.25f)
        s.canvas.save()
        s.canvas.rotate(s.f(-35f, 35f), ax, ay)
        s.canvas.translate(ax, ay)
        s.canvas.scale(size, size)
        val suit = Color.rgb(232, 232, 238)
        val shade = Color.rgb(150, 152, 165)
        // Tether.
        val tether = Path()
        tether.moveTo(60f, -10f)
        tether.cubicTo(220f, -60f, 260f, 200f, 520f, 120f)
        s.canvas.drawPath(tether, s.stroke(Color.WHITE, 2.4f, 200))
        // Backpack.
        s.canvas.drawRoundRect(RectF(-80f, -95f, 80f, 70f), 26f, 26f, s.fill(shade))
        s.canvas.drawRoundRect(RectF(-80f, -95f, 80f, 70f), 26f, 26f, s.stroke(Color.WHITE, 2.4f))
        // Legs.
        for ((dx, bend) in listOf(-30f to -40f, 30f to 30f)) {
            val leg = Path()
            leg.moveTo(dx, 60f); leg.quadTo(dx + bend, 130f, dx + bend * 0.6f, 190f)
            s.limb(leg, suit, shade, 46f, 3f)
            s.canvas.drawRoundRect(RectF(dx + bend * 0.6f - 30f, 180f, dx + bend * 0.6f + 28f, 215f), 14f, 14f, s.fill(shade))
        }
        // Torso.
        s.canvas.drawRoundRect(RectF(-70f, -80f, 70f, 85f), 40f, 40f, s.fill(suit))
        s.canvas.drawRoundRect(RectF(-70f, -80f, 70f, 85f), 40f, 40f, s.stroke(shade, 3f))
        s.canvas.drawRoundRect(RectF(-36f, -10f, 36f, 40f), 8f, 8f, s.fill(shade))
        for (k in 0 until 3) s.canvas.drawCircle(-18f + k * 18f, 15f, 5f, s.fill(arrayOf(Color.RED, Color.GREEN, s.color(1))[k]))
        s.canvas.drawCircle(-55f, -50f, 11f, s.fill(s.color(0)))
        // Arms.
        for ((dx, dir) in listOf(-60f to -1f, 60f to 1f)) {
            val arm = Path()
            arm.moveTo(dx, -50f); arm.quadTo(dx + dir * 70f, -20f, dx + dir * 80f, 50f)
            s.limb(arm, suit, shade, 38f, 3f)
            s.canvas.drawCircle(dx + dir * 80f, 62f, 20f, s.fill(shade))
        }
        // Helmet with a reflective visor.
        s.canvas.drawCircle(0f, -130f, 72f, s.fill(suit))
        s.canvas.drawCircle(0f, -130f, 72f, s.stroke(shade, 3f))
        val visor = RectF(-54f, -175f, 54f, -92f)
        s.canvas.drawRoundRect(visor, 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, visor.top, 0f, visor.bottom, intArrayOf(lighten(s.color(0), 0.3f), darken(s.color(0), 0.4f), Color.BLACK), null, Shader.TileMode.CLAMP)
        })
        s.canvas.drawArc(RectF(-40f, -110f, 40f, -40f), 200f, 140f, false, s.stroke(s.color(1), 4f, 220))
        s.canvas.drawArc(RectF(-44f, -170f, 10f, -120f), 190f, 70f, false, s.stroke(Color.WHITE, 5f, 230))
        s.canvas.restore()
        s.softGlow(ax, ay - 80f, 300f, s.color(1), 30)
    }
}

object RingedWorld : Style("ringed_world", "Ringed World", Category.SPACE) {
    override val autoPalettes = listOf("sunset", "gold", "candy", "violet", "ocean")

    override fun draw(s: Scene) {
        s.starSky(240, s.h * 0.75f)
        val px = s.w * s.f(0.4f, 0.6f)
        val py = s.h * s.f(0.28f, 0.36f)
        val pr = s.f(230f, 290f)
        val tilt = s.f(-25f, -8f)
        val ringRect = { k: Int -> val rx = pr * (1.35f + k * 0.06f); RectF(px - rx, py - rx * 0.24f, px + rx, py + rx * 0.24f) }
        // Back half of the rings.
        s.canvas.save(); s.canvas.rotate(tilt, px, py)
        for (k in 0 until 14) s.canvas.drawArc(ringRect(k), 180f, 180f, false, s.stroke(mix(s.color(1), s.color(2), k / 14f), 5f, if (k % 4 == 3) 60 else 170))
        s.canvas.restore()
        // Planet with banded clouds and a shadow terminator.
        val clip = Path(); clip.addCircle(px, py, pr, Path.Direction.CW)
        s.canvas.drawCircle(px, py, pr, s.fill(darken(s.color(0), 0.3f)))
        s.canvas.save(); s.canvas.clipPath(clip); s.canvas.rotate(tilt, px, py)
        var by = py - pr
        var k = 0
        while (by < py + pr) {
            val band = Path()
            val bh = s.f(18f, 46f)
            band.moveTo(px - pr, by)
            var x = px - pr
            while (x <= px + pr) { band.lineTo(x, by + s.noise.at(x * 0.01f, by * 0.01f) * 12f); x += 10f }
            band.lineTo(px + pr, by + bh); band.lineTo(px - pr, by + bh); band.close()
            s.canvas.drawPath(band, s.fill(mix(s.color(k % 3), Color.WHITE, s.f(0f, 0.25f)), 200))
            by += bh; k++
        }
        s.canvas.restore()
        s.canvas.save(); s.canvas.clipPath(clip)
        s.canvas.drawCircle(px, py, pr, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(px - pr, py - pr, px + pr, py + pr, intArrayOf(Color.TRANSPARENT, withAlpha(Color.BLACK, 120), Color.BLACK), floatArrayOf(0f, 0.55f, 0.85f), Shader.TileMode.CLAMP)
        })
        s.canvas.restore()
        s.canvas.drawCircle(px, py, pr, s.stroke(lighten(s.color(0), 0.4f), 2f, 160))
        // Front half of the rings.
        s.canvas.save(); s.canvas.rotate(tilt, px, py)
        for (j in 0 until 14) s.canvas.drawArc(ringRect(j), 0f, 180f, false, s.stroke(mix(s.color(1), s.color(2), j / 14f), 5f, if (j % 4 == 3) 60 else 200))
        s.canvas.restore()
        // A small moon.
        s.canvas.drawCircle(px + pr * 1.6f, py - pr * 0.9f, 26f, s.fill(Color.rgb(200, 200, 210)))
        s.canvas.drawCircle(px + pr * 1.6f + 8f, py - pr * 0.9f - 6f, 24f, s.fill(Color.BLACK))

        // Cratered moonscape in the foreground.
        val (land, ridgeLine) = s.ridge(s.h * 0.8f, 70f, 0.003f, 12f, ridged = false, step = 10f)
        s.canvas.drawPath(land, s.vGradient(s.h * 0.72f, s.h, Color.rgb(70, 70, 80), Color.BLACK))
        s.canvas.drawPath(ridgeLine, s.stroke(Color.rgb(220, 220, 230), 2.4f))
        repeat(s.i(6, 11)) {
            val cxr = s.f(0f, s.w)
            val cyr = s.h * s.f(0.84f, 0.97f)
            val rr = s.f(20f, 70f) * ((cyr - s.h * 0.8f) / (s.h * 0.2f)).coerceIn(0.4f, 1.2f)
            val rect = RectF(cxr - rr, cyr - rr * 0.32f, cxr + rr, cyr + rr * 0.32f)
            s.canvas.drawOval(rect, s.fill(Color.rgb(25, 25, 30)))
            s.canvas.drawArc(rect, 0f, 180f, false, s.stroke(Color.rgb(200, 200, 210), 2f, 200))
        }
        // A planted flag.
        val fx = s.w * s.f(0.65f, 0.85f)
        val fy = s.h * 0.82f
        s.canvas.drawLine(fx, fy, fx, fy - 110f, s.stroke(Color.WHITE, 3f))
        s.canvas.drawRect(fx, fy - 110f, fx + 60f, fy - 74f, s.fill(s.color(0)))
        s.canvas.drawRect(fx, fy - 110f, fx + 60f, fy - 74f, s.stroke(Color.WHITE, 1.6f))
    }
}

object BlackHole : Style("black_hole", "Black Hole", Category.SPACE) {
    override val autoPalettes = listOf("sunset", "gold", "lava", "violet")

    override fun draw(s: Scene) {
        s.starSky(300, twinkles = 8)
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.5f)
        val r = s.f(130f, 170f)
        val tilt = s.f(-12f, 12f)
        val hot = lighten(s.color(1), 0.5f)
        fun disk(start: Float) {
            s.canvas.save(); s.canvas.rotate(tilt, cx, cy)
            for (k in 0 until 46) {
                val rx = r * 1.35f + k * 9f
                val ry = rx * 0.17f
                val c = mix(hot, mix(s.color(0), s.color(2), k / 46f), (k / 18f).coerceAtMost(1f))
                val a = (230 * (1f - k / 46f)).toInt().coerceAtLeast(20)
                s.canvas.drawArc(RectF(cx - rx, cy - ry, cx + rx, cy + ry), start, 180f, false,
                    s.stroke(c, 5f, a).apply { blendMode = BlendMode.PLUS })
            }
            s.canvas.restore()
        }
        disk(180f)
        // Gravitationally lensed light wrapping over and under the shadow.
        for (k in 0 until 26) {
            val rr = r * 1.08f + k * 3.2f
            s.canvas.drawCircle(cx, cy, rr, s.stroke(mix(hot, s.color(0), k / 26f), 3f, (180 * (1f - k / 26f)).toInt()).apply { blendMode = BlendMode.PLUS })
        }
        s.canvas.drawCircle(cx, cy, r, s.fill(Color.BLACK))
        s.canvas.drawCircle(cx, cy, r * 1.03f, s.stroke(Color.WHITE, 2.4f, 230, 3f))
        disk(0f)
        // Infalling dust.
        repeat(s.count(80)) {
            val a = s.f(0f, TAU)
            val d = s.f(r * 2.2f, r * 5f)
            s.dot(cx + cos(a) * d, cy + sin(a) * d * 0.35f, s.f(0.6f, 1.6f), hot, s.i(60, 200))
        }
    }
}
