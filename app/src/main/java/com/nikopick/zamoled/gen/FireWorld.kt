package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Fire and lightning for the Nature world. Everything is drawn as light on pure black: flames, embers,
 * lava and bolts are the only pixels that switch on.
 */

private fun Scene.fireTones(): IntArray {
    val c3 = if (palette.colors.size > 3) color(3) else lighten(color(2), 0.7f)
    return intArrayOf(darken(color(0), 0.1f), color(1), color(2), c3)
}

/** A flame tongue: a teardrop that curls to a point, leaning by [lean] at the tip. */
private fun Path.tongue(x: Float, y: Float, height: Float, width: Float, lean: Float) {
    moveTo(x - width / 2f, y)
    cubicTo(x - width * 0.62f, y - height * 0.35f, x - width * 0.15f + lean * 0.4f, y - height * 0.66f, x + lean, y - height)
    cubicTo(x + width * 0.15f + lean * 0.5f, y - height * 0.62f, x + width * 0.64f, y - height * 0.33f, x + width / 2f, y)
    close()
}

/** One flame in four nested layers, deep red outside to near-white at the heart. */
private fun Scene.flame(x: Float, y: Float, height: Float, width: Float, lean: Float, t: IntArray) {
    val scales = floatArrayOf(1f, 0.76f, 0.52f, 0.28f)
    for (k in 0 until 4) {
        val p = Path().apply { tongue(x, y, height * scales[k], width * scales[k], lean * scales[k]) }
        canvas.drawPath(p, fill(t[k], if (k == 0) 170 else 235, if (k == 0) width * 0.35f else 0f))
    }
}

/** Sparks rising from a base: hot and bright low down, red and fading as they climb. */
private fun Scene.sparks(n: Int, x0: Float, x1: Float, baseY: Float, rise: Float, t: IntArray, drift: Float = 80f) {
    repeat(count(n)) {
        val u = f() * f()
        val x = f(x0, x1) + sin(u * 9f + f(0f, 6f)) * drift * u
        val y = baseY - u * rise * f(0.5f, 1f)
        val col = if (u < 0.35f) mix(t[3], t[2], u / 0.35f) else mix(t[2], t[0], (u - 0.35f) / 0.65f)
        val r = f(0.8f, 2.8f) * (1f - u * 0.55f)
        val a = ((1f - u) * 255f).toInt().coerceIn(30, 255)
        canvas.drawLine(x, y, x - drift * 0.05f, y + f(6f, 24f) * (1f - u), stroke(col, r * 0.8f, a / 2))
        glowDot(x, y, r, col, r * 3.5f, a)
    }
}

// ---------------------------------------------------------------------------------------------

/** A campfire on black ground: crossed logs, a bed of coals, a ring of lit stones and a column of sparks. */
object Campfire : Style("fire_camp", "Campfire", Category.NATURE) {
    override val autoPalettes = listOf("fire", "fire", "fire", "lava", "sunset")

    override fun draw(s: Scene) {
        val t = s.fireTones()
        val by = s.h * 0.56f
        val x = s.cx + s.f(-60f, 60f)
        // Light thrown on the ground.
        s.canvas.save(); s.canvas.scale(1f, 0.3f, 0f, by)
        s.softGlow(x, by, 520f, t[0], 120)
        s.canvas.restore()
        s.softGlow(x, by - s.h * 0.08f, 300f, t[1], 60)
        // Stones around the fire, lit on the side that faces it.
        for (k in 0 until 11) {
            val a = k / 11f * TAU
            val sx = x + cos(a) * 190f; val sy = by + 28f + sin(a) * 52f
            val r = s.f(20f, 32f)
            s.canvas.drawOval(RectF(sx - r, sy - r * 0.62f, sx + r, sy + r * 0.62f), s.fill(Color.rgb(10, 8, 8)))
            s.canvas.drawArc(RectF(sx - r, sy - r * 0.62f, sx + r, sy + r * 0.62f), 200f, 140f, false, s.stroke(t[1], 2.2f, 150, 3f))
        }
        // Logs.
        for (side in intArrayOf(-1, 1)) {
            val p = Path().apply {
                moveTo(x - side * 130f, by + 30f); lineTo(x + side * 120f, by - 6f); lineTo(x + side * 126f, by + 14f); lineTo(x - side * 124f, by + 52f); close()
            }
            s.canvas.drawPath(p, s.fill(Color.rgb(22, 12, 8)))
            s.canvas.drawLine(x - side * 126f, by + 31f, x + side * 118f, by - 6f, s.stroke(t[1], 3f, 200, 6f))
        }
        // Coals.
        repeat(s.count(18)) { s.glowDot(x + s.f(-90f, 90f), by + s.f(6f, 40f), s.f(2f, 6f), mix(t[0], t[2], s.f()), 12f, s.i(150, 255)) }
        // Flames.
        val hgt = s.h * 0.17f
        s.softGlow(x, by - hgt * 0.4f, hgt * 1.3f, t[0], 80)
        val n = s.count(6)
        for (k in 0 until n) {
            val fx = x + s.f(-80f, 80f)
            val ph = s.f(0.55f, 1f) * (1f - abs(fx - x) / 220f)
            s.flame(fx, by + s.f(0f, 20f), hgt * ph, s.f(70f, 120f), s.f(-30f, 30f), t)
        }
        s.flame(x, by + 8f, hgt * 1.08f, 150f, s.f(-16f, 16f), t)
        s.sparks(110, x - 90f, x + 90f, by - hgt * 0.5f, s.h * 0.34f, t)
    }
}

/** Embers rising through the dark from a bed of heat at the bottom of the screen. */
object Embers : Style("fire_embers", "Embers", Category.NATURE) {
    override val autoPalettes = listOf("fire", "fire", "lava", "sunset", "fire")

    override fun draw(s: Scene) {
        val t = s.fireTones()
        // Heat at the foot of the page.
        s.canvas.save(); s.canvas.scale(1f, 0.33f, 0f, s.h)
        s.softGlow(s.cx, s.h, 900f, t[0], 150)
        s.canvas.restore()
        repeat(s.count(420)) {
            val u = s.f().let { it * it }                  // most embers low, a few high
            val y = s.h * (1f - u * 0.92f)
            val x = s.f(-20f, s.w + 20f)
            val heat = 1f - u
            val col = if (heat > 0.6f) mix(t[2], t[3], (heat - 0.6f) / 0.4f) else mix(t[0], t[2], heat / 0.6f)
            val r = s.f(0.7f, 3.4f) * (0.5f + heat)
            val a = (60 + 195 * heat).toInt().coerceIn(40, 255)
            val lean = s.f(-14f, 40f)
            s.canvas.drawLine(x, y, x - lean, y + s.f(10f, 46f) * (0.4f + heat), s.stroke(col, r * 0.7f, a / 3))
            s.glowDot(x, y, r, col, r * 3.2f, a)
        }
        // A few big, soft embers close to the lens.
        repeat(s.count(9)) {
            val x = s.f(0f, s.w); val y = s.f(s.h * 0.2f, s.h * 0.95f)
            val r = s.f(6f, 18f)
            s.canvas.drawCircle(x, y, r * 2.4f, s.fill(t[0], 40, r * 1.6f))
            s.canvas.drawCircle(x, y, r, s.fill(mix(t[1], t[2], s.f()), 150, r * 0.7f))
        }
    }
}

/** A wall of flame licking up from the bottom edge, tall in the middle and guttering toward the sides. */
object Inferno : Style("fire_inferno", "Inferno", Category.NATURE) {
    override val autoPalettes = listOf("fire", "fire", "lava", "fire", "sunset")

    override fun draw(s: Scene) {
        val t = s.fireTones()
        val base = s.h + 20f
        s.canvas.save(); s.canvas.scale(1f, 0.4f, 0f, s.h)
        s.softGlow(s.cx, s.h, 800f, t[0], 140)
        s.canvas.restore()
        val n = s.count(15)
        val order = (0 until n).sortedBy { -abs(it - n / 2) }          // outer flames first, middle ones in front
        for (k in order) {
            val fx = (k + 0.5f) / n * s.w + s.f(-24f, 24f)
            val mid = 1f - abs(fx - s.cx) / (s.w * 0.62f)
            val noise = 0.65f + 0.35f * s.noise.at(fx * 0.01f, 3f)
            val hgt = s.h * 0.4f * mid * noise * s.f(0.8f, 1.15f) + 120f
            s.flame(fx, base, hgt, s.f(190f, 280f), s.f(-60f, 60f), t)
        }
        s.sparks(160, 0f, s.w, s.h * 0.82f, s.h * 0.55f, t, 120f)
    }
}

/** A volcano: lava down its flanks, a fountain of fire at the crater, an ash cloud lit from beneath and lightning in it. */
object Volcano : Style("fire_volcano", "Volcano", Category.NATURE) {
    override val autoPalettes = listOf("fire", "fire", "lava", "sunset")

    override fun draw(s: Scene) {
        val t = s.fireTones()
        val cx = s.cx + s.f(-120f, 120f)
        val crater = s.h * 0.43f
        val foot = s.h * 0.64f
        // Sky glow behind the mountain.
        s.softGlow(cx, crater - 40f, 620f, t[0], 90)
        // Ash cloud, lit orange from below.
        repeat(s.count(14)) {
            val k = it / 14f
            val y = crater - 60f - k * s.h * 0.2f
            val x = cx + s.f(-120f, 120f) * (0.4f + k)
            val r = s.f(70f, 150f) * (0.6f + k)
            s.softGlow(x, y, r, mix(t[0], Color.rgb(70, 50, 55), k), (90 * (1f - k * 0.6f)).toInt())
        }
        // Lightning inside the plume.
        repeat(2) {
            val x0 = cx + s.f(-100f, 100f); val y0 = crater - s.h * s.f(0.1f, 0.18f)
            val pts = s.boltPoints(x0, y0, x0 + s.f(-90f, 90f), y0 + s.f(120f, 200f), 40f)
            s.drawBolt(pts, 1.8f, Color.rgb(235, 225, 255), Color.rgb(150, 110, 255), 230)
        }
        // The mountain, black against the glow.
        val cone = Path().apply {
            moveTo(-40f, foot + 40f)
            var x = -40f
            while (x < cx - 70f) { lineTo(x, foot - (x + 40f) / (cx - 70f + 40f) * (foot - crater) + s.noise.at(x * 0.02f, 1f) * 14f); x += 20f }
            lineTo(cx - 60f, crater + 8f); lineTo(cx - 24f, crater - 8f); lineTo(cx + 24f, crater - 6f); lineTo(cx + 62f, crater + 8f)
            x = cx + 70f
            while (x < s.w + 40f) { lineTo(x, crater + (x - cx - 70f) / (s.w + 40f - cx - 70f) * (foot - crater) + s.noise.at(x * 0.02f, 5f) * 14f); x += 20f }
            lineTo(s.w + 40f, foot + 40f); close()
        }
        s.canvas.drawPath(cone, s.fill(Color.BLACK))
        s.canvas.drawPath(cone, s.stroke(withAlpha(t[0], 90), 2f, 255, 4f))
        // Lava rivers, wandering down the flanks and branching.
        repeat(s.i(4, 7)) {
            var x = cx + s.f(-26f, 26f); var y = crater + 4f
            val dir = if (s.chance(0.5f)) -1f else 1f
            val p = Path().apply { moveTo(x, y) }
            var wdt = s.f(5f, 9f)
            while (y < foot + 10f) {
                x += dir * s.f(2f, 18f) + s.f(-10f, 10f); y += s.f(14f, 34f); wdt *= 0.97f
                p.lineTo(x, y)
            }
            s.canvas.drawPath(p, s.stroke(t[0], wdt * 2.4f, 130, 10f))
            s.canvas.drawPath(p, s.stroke(mix(t[1], t[2], 0.5f), wdt, 240))
        }
        // The fountain: arcs of glowing rock thrown from the crater.
        repeat(s.count(60)) {
            val ang = s.f(-1.15f, 1.15f); val v = s.f(0.4f, 1f)
            val peakX = cx + ang * 260f * v; val peakY = crater - 180f * v - s.f(0f, 160f)
            val endX = cx + ang * 520f * v; val endY = crater + s.f(0f, 60f)
            val p = Path().apply { moveTo(cx + s.f(-14f, 14f), crater); quadTo(peakX, peakY, endX, endY) }
            val col = mix(t[2], t[0], s.f())
            s.canvas.drawPath(p, s.stroke(col, s.f(1f, 2.4f), s.i(50, 150), 2f))
            s.glowDot(peakX * 0.5f + (cx + endX) * 0.25f, (crater + peakY) * 0.5f, s.f(1.2f, 3f), t[3], 8f, 220)
        }
        s.softGlow(cx, crater - 10f, 120f, t[3], 200)
    }
}

/** A single flame hanging in the dark, the way a candle is seen up close. */
object Flame : Style("fire_flame", "Flame", Category.NATURE) {
    override val autoPalettes = listOf("fire", "fire", "sunset", "lava", "ocean")

    override fun draw(s: Scene) {
        val t = s.fireTones()
        val by = s.h * 0.5f
        val hgt = s.h * 0.2f
        s.softGlow(s.cx, by - hgt * 0.45f, hgt * 1.7f, t[0], 90)
        s.softGlow(s.cx, by - hgt * 0.4f, hgt * 0.8f, t[1], 70)
        // A wick and the dark core at the base of the flame.
        s.canvas.drawLine(s.cx, by + 40f, s.cx, by - 6f, s.stroke(Color.rgb(24, 18, 16), 5f))
        s.flame(s.cx, by, hgt, 120f, s.f(-26f, 26f), t)
        s.canvas.drawOval(RectF(s.cx - 20f, by - 70f, s.cx + 20f, by + 4f), s.fill(Color.rgb(40, 56, 150), 120, 10f))
        // Wisps leaving the tip, and a few sparks.
        repeat(5) {
            val p = Path().apply {
                moveTo(s.cx + s.f(-20f, 20f), by - hgt)
                cubicTo(s.cx + s.f(-60f, 60f), by - hgt - 70f, s.cx + s.f(-60f, 60f), by - hgt - 150f, s.cx + s.f(-40f, 40f), by - hgt - s.f(200f, 330f))
            }
            s.canvas.drawPath(p, s.stroke(t[0], s.f(1f, 2.2f), s.i(25, 70), 4f))
        }
        s.sparks(45, s.cx - 40f, s.cx + 40f, by - hgt * 0.7f, s.h * 0.3f, t, 60f)
    }
}

// ---------------------------------------------------------------------------------------------

/** A jagged line from one point to another, by repeatedly nudging midpoints sideways. */
private fun Scene.boltPoints(x1: Float, y1: Float, x2: Float, y2: Float, jag: Float): List<PointF> {
    var pts = listOf(PointF(x1, y1), PointF(x2, y2))
    var d = jag
    repeat(6) {
        val next = ArrayList<PointF>()
        for (k in 0 until pts.size - 1) {
            val a = pts[k]; val b = pts[k + 1]
            next += a
            next += PointF((a.x + b.x) / 2f + f(-d, d), (a.y + b.y) / 2f + f(-d, d) * 0.25f)
        }
        next += pts.last()
        pts = next
        d *= 0.55f
    }
    return pts
}

/** A bolt in three passes: a wide coloured bloom, a tighter glow and a white-hot core. */
private fun Scene.drawBolt(pts: List<PointF>, width: Float, color: Int, glow: Int, alpha: Int = 255) {
    val p = Path().apply { moveTo(pts[0].x, pts[0].y); for (k in 1 until pts.size) lineTo(pts[k].x, pts[k].y) }
    canvas.drawPath(p, stroke(glow, width * 7f, alpha / 4, width * 9f))
    canvas.drawPath(p, stroke(color, width * 2.6f, alpha / 2, width * 2.5f))
    canvas.drawPath(p, stroke(Color.WHITE, width, alpha))
}

/** The main bolt plus forks that peel off it, thinner at every generation. */
private fun Scene.forkedBolt(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int, glow: Int, depth: Int = 0) {
    val pts = boltPoints(x1, y1, x2, y2, hypot(x2 - x1, y2 - y1) * 0.22f)
    drawBolt(pts, width, color, glow, if (depth == 0) 255 else 200)
    if (depth >= 2) return
    for (k in 6 until pts.size - 6 step 5) {
        if (!chance(if (depth == 0) 0.2f else 0.1f)) continue
        val base = atan2(y2 - y1, x2 - x1) + f(0.35f, 0.95f) * (if (chance(0.5f)) 1f else -1f)
        val len = f(90f, 260f) / (1f + depth)
        forkedBolt(pts[k].x, pts[k].y, pts[k].x + cos(base) * len, pts[k].y + sin(base) * len, width * 0.5f, color, glow, depth + 1)
    }
}

/** Hills as a black shape with a thin rim of the flash's colour along the top. */
private fun Scene.hills(horizon: Float, amp: Float, rim: Int, seed: Float) {
    val land = Path().apply { moveTo(-10f, h + 10f) }
    val line = Path()
    var x = -10f
    while (x <= w + 10f) {
        val y = horizon + noise.fbm(x * 0.004f + seed, seed) * amp
        land.lineTo(x, y)
        if (x < 0f) line.moveTo(x, y) else line.lineTo(x, y)
        x += 8f
    }
    land.lineTo(w + 10f, h + 10f); land.close()
    canvas.drawPath(land, fill(Color.BLACK))
    canvas.drawPath(line, stroke(rim, 2f, 170, 3f))
}

/** A bolt from the clouds to the ground over dark hills, the whole sky lit for a moment. */
object LightningStrike : Style("storm_strike", "Lightning Strike", Category.NATURE) {
    override val autoPalettes = listOf("storm", "storm", "cyan", "violet", "ocean")

    override fun draw(s: Scene) {
        val c0 = s.color(0); val c1 = s.color(1); val c2 = s.color(2)
        val x0 = s.cx + s.f(-160f, 160f)
        val ground = s.h * 0.62f
        val x1 = x0 + s.f(-130f, 130f)
        // Cloud deck, lit around where the bolt leaves it.
        repeat(s.count(22)) {
            val x = s.f(-60f, s.w + 60f); val y = s.f(-20f, s.h * 0.18f)
            val near = 1f - (abs(x - x0) / s.w).coerceAtMost(1f)
            s.softGlow(x, y, s.f(140f, 300f), mix(darken(c1, 0.6f), c0, near * 0.6f), (30 + 60 * near).toInt())
        }
        s.softGlow(x0, s.h * 0.12f, 500f, c2, 80)
        s.softGlow((x0 + x1) / 2f, (s.h * 0.1f + ground) / 2f, 520f, c1, 40)
        s.forkedBolt(x0, s.h * 0.07f, x1, ground + 6f, 3.2f, lighten(c0, 0.5f), c1)
        // Rain, and the flash on the ground where it lands.
        repeat(s.count(180)) {
            val x = s.f(0f, s.w); val y = s.f(s.h * 0.15f, s.h)
            s.canvas.drawLine(x, y, x - 6f, y + s.f(22f, 60f), s.stroke(lighten(c0, 0.4f), 1f, s.i(14, 40)))
        }
        s.hills(ground, 40f, lighten(c0, 0.2f), s.f(0f, 50f))
        s.canvas.save(); s.canvas.scale(1f, 0.28f, 0f, ground)
        s.softGlow(x1, ground, 380f, c0, 160)
        s.canvas.restore()
        s.glowDot(x1, ground, 9f, Color.WHITE, 30f)
        repeat(14) {
            val a = s.f(PI.toFloat() * 1.05f, PI.toFloat() * 1.95f); val l = s.f(20f, 80f)
            s.canvas.drawLine(x1, ground, x1 + cos(a) * l, ground + sin(a) * l * 0.7f, s.stroke(Color.WHITE, 1.6f, 200, 2f))
        }
    }
}

/** A thunderstorm seen from far off: an anvil cloud flickering from inside, rain below it and a bolt to the horizon. */
object StormCell : Style("storm_cell", "Storm Cell", Category.NATURE) {
    override val autoPalettes = listOf("storm", "storm", "violet", "cyan", "ocean")

    override fun draw(s: Scene) {
        val c0 = s.color(0); val c1 = s.color(1); val c2 = s.color(2)
        val ground = s.h * 0.66f
        val base = s.h * 0.4f
        // The anvil: flat on top, wide, lumpy underneath.
        repeat(s.count(46)) {
            val x = s.f(-40f, s.w + 40f)
            val sag = s.noise.at(x * 0.006f, 2f) * 40f
            val y = base - s.f(0f, 95f) + sag
            s.softGlow(x, y, s.f(90f, 210f), darken(c1, 0.55f), s.i(30, 70))
        }
        s.canvas.save(); s.canvas.scale(1f, 0.22f, 0f, base - 70f)
        s.softGlow(s.cx, base - 70f, 620f, darken(c1, 0.5f), 70)
        s.canvas.restore()
        // Flashes inside it, and veins of lightning crawling across the cloud.
        repeat(s.i(3, 6)) {
            val x = s.f(s.w * 0.1f, s.w * 0.9f); val y = base - s.f(10f, 90f)
            s.softGlow(x, y, s.f(160f, 300f), lighten(c0, 0.45f), s.i(90, 170))
            if (s.chance(0.7f)) {
                val pts = s.boltPoints(x, y, x + s.f(-220f, 220f), y + s.f(-40f, 60f), 36f)
                s.drawBolt(pts, 1.3f, lighten(c0, 0.6f), c1, 220)
            }
        }
        // Cloud to ground.
        val bx = s.f(s.w * 0.25f, s.w * 0.75f)
        s.forkedBolt(bx, base + 10f, bx + s.f(-100f, 100f), ground + 4f, 2.8f, lighten(c0, 0.5f), c1)
        // A curtain of rain under the cloud, brighter near the bolt.
        repeat(s.count(520)) {
            val x = s.f(0f, s.w); val y = s.f(base + 20f, ground)
            val near = 1f - (abs(x - bx) / s.w).coerceAtMost(1f)
            s.canvas.drawLine(x, y, x - 10f, y + s.f(26f, 70f), s.stroke(lighten(c0, 0.35f), 1f, (10 + 50 * near * near).toInt()))
        }
        s.hills(ground, 26f, lighten(c0, 0.15f), s.f(0f, 50f))
    }
}

