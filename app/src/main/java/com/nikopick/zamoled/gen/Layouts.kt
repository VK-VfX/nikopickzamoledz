package com.nikopick.zamoled.gen

import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** How doodles are arranged on the page. Styles ask for placements and draw one doodle per spot. */
enum class DoodleLayout(val id: String, val label: String) {
    AUTO("auto", "Auto"),
    SCATTER("scatter", "Scatter"),
    GRID("grid", "Grid"),
    RINGS("rings", "Rings"),
    SPIRAL("spiral", "Spiral"),
    WAVE("wave", "Wave"),
    MOSAIC("mosaic", "Mosaic"),
    BURST("burst", "Burst"),
    DIAGONAL("diagonal", "Diagonal"),
    FRAME("frame", "Frame"),
    SHAPE("shape", "Shape");

    companion object {
        fun byId(id: String): DoodleLayout = entries.firstOrNull { it.id == id } ?: AUTO
        /** Everything the picker offers. */
        val choices: List<DoodleLayout> get() = entries
        private val random = entries.filter { it != AUTO }
        fun resolve(id: String, seed: Long): DoodleLayout {
            val l = byId(id)
            return if (l != AUTO) l else random[(seed xor 0x9E3779B9L).toULong().mod(random.size.toUInt()).toInt()]
        }
    }
}

/**
 * One doodle slot. [scale] multiplies the style's own size range, [rot] is an extra rotation in degrees
 * and [t] runs 0..1 along the arrangement, for gradients.
 */
class Spot(val x: Float, val y: Float, val scale: Float = 1f, val rot: Float = 0f, val t: Float = 0.5f)

/**
 * Builds the spots for [layout]. [baseCell] is the style's natural spacing at density 1, so a style with
 * big doodles passes a big cell. Spots come back in draw order and already account for scene density.
 */
fun Scene.spots(layout: DoodleLayout, baseCell: Float): List<Spot> {
    // Structured arrangements need negative space, or the pattern disappears into texture.
    val breathe = when (layout) {
        DoodleLayout.BURST -> 1.5f
        DoodleLayout.SHAPE -> 0.58f      // needs a fine grid, or the silhouette breaks up
        DoodleLayout.FRAME -> 1.15f
        DoodleLayout.DIAGONAL -> 1.1f
        else -> 1f
    }
    val cell = baseCell * breathe / sqrt(density)
    return when (layout) {
        DoodleLayout.AUTO, DoodleLayout.SCATTER -> scatterSpots(cell)
        DoodleLayout.GRID -> gridSpots(cell)
        DoodleLayout.RINGS -> ringSpots(cell)
        DoodleLayout.SPIRAL -> spiralSpots(cell)
        DoodleLayout.WAVE -> waveSpots(cell)
        DoodleLayout.MOSAIC -> mosaicSpots(cell)
        DoodleLayout.BURST -> burstSpots(cell)
        DoodleLayout.DIAGONAL -> diagonalSpots(cell)
        DoodleLayout.FRAME -> frameSpots(cell)
        DoodleLayout.SHAPE -> shapeSpots(cell)
    }
}

private fun Scene.target(cell: Float, fill: Float = 1f): Int =
    ((w + cell) * (h + cell) / (cell * cell) * fill).toInt().coerceIn(6, 1400)

/** Loose random placement that keeps a minimum distance, so nothing overlaps badly. */
private fun Scene.scatterSpots(cell: Float): List<Spot> {
    val n = target(cell, 1.05f)
    val minDist = cell * 0.68f
    val gridSize = minDist
    val cols = (w / gridSize).toInt() + 2
    val taken = HashMap<Int, MutableList<Spot>>()
    val out = ArrayList<Spot>(n)
    var tries = 0
    while (out.size < n && tries < n * 14) {
        tries++
        val x = f(-cell * 0.3f, w + cell * 0.3f)
        val y = f(-cell * 0.3f, h + cell * 0.3f)
        val gx = (x / gridSize).toInt()
        val gy = (y / gridSize).toInt()
        var ok = true
        loop@ for (dy in -1..1) for (dx in -1..1) {
            val near = taken[(gy + dy) * cols + (gx + dx)] ?: continue
            for (p in near) {
                val ddx = p.x - x
                val ddy = p.y - y
                if (ddx * ddx + ddy * ddy < minDist * minDist) { ok = false; break@loop }
            }
        }
        if (!ok) continue
        val s = Spot(x, y, f(0.75f, 1.3f), f(-28f, 28f), (x / w + y / h) / 2f)
        out.add(s)
        taken.getOrPut(gy * cols + gx) { ArrayList() }.add(s)
    }
    return out
}

/** The original staggered rows. */
private fun Scene.gridSpots(cell: Float): List<Spot> {
    val out = ArrayList<Spot>()
    val cols = (w / cell).toInt() + 1
    val rows = (h / cell).toInt() + 1
    val stagger = chance(0.5f)
    for (gy in 0..rows) for (gx in 0..cols) {
        val offset = if (stagger && gy % 2 == 1) cell / 2f else 0f
        val x = gx * cell + offset + f(0.2f, 0.8f) * cell
        val y = gy * cell + f(0.2f, 0.8f) * cell
        out.add(Spot(x, y, f(0.85f, 1.15f), f(-22f, 22f), (x / w + y / h) / 2f))
    }
    return out
}

/** Concentric rings: doodles sit shoulder to shoulder along each ring, with wide gaps between rings. */
private fun Scene.ringSpots(cell: Float): List<Spot> {
    val out = ArrayList<Spot>()
    val along = cell * 0.82f
    val step = cell * 1.95f
    val cx = this.cx
    val cy = h * f(0.4f, 0.5f)
    val maxR = sqrt(max(cx, w - cx).pow(2) + max(cy, h - cy).pow(2)) + cell
    val turn = f(0f, 360f)
    var r = step * 0.5f
    var ring = 0
    out.add(Spot(cx, cy, 1.15f, 0f, 0f))
    while (r < maxR) {
        val count = max(5, (TAU * r / along).toInt())
        val offset = if (ring % 2 == 0) 0f else TAU / count / 2f
        for (k in 0 until count) {
            val a = turn + offset + k * TAU / count
            val x = cx + cos(a) * r
            val y = cy + sin(a) * r
            if (x < -cell || x > w + cell || y < -cell || y > h + cell) continue
            out.add(Spot(x, y, f(0.85f, 1.05f), Math.toDegrees(a.toDouble()).toFloat() + 90f, (r / maxR).coerceIn(0f, 1f)))
        }
        r += step
        ring++
    }
    return out
}

/** One long Archimedean spiral: evenly spaced along the curve with a wide gap between turns. */
private fun Scene.spiralSpots(cell: Float): List<Spot> {
    val out = ArrayList<Spot>()
    val along = cell * 0.8f
    val cx = this.cx
    val cy = h * f(0.42f, 0.52f)
    val maxR = sqrt(max(cx, w - cx).pow(2) + max(cy, h - cy).pow(2)) + cell
    val b = cell * 2.1f / TAU          // radial growth per radian
    val dir = if (chance(0.5f)) 1f else -1f
    val a0 = f(0f, TAU)
    var a = a0
    var guard = 0
    while (guard++ < 4000) {
        val r = b * (a - a0)
        if (r > maxR) break
        val x = cx + cos(a * dir) * r
        val y = cy + sin(a * dir) * r
        val t = (r / maxR).coerceIn(0f, 1f)
        if (x > -cell && x < w + cell && y > -cell && y < h + cell) {
            out.add(Spot(x, y, 0.7f + t * 0.5f, Math.toDegrees((a * dir).toDouble()).toFloat() + 90f, t))
        }
        a += along / max(r, along)
    }
    return out
}

/** Rows that ripple, like a flag. */
private fun Scene.waveSpots(cell: Float): List<Spot> {
    val out = ArrayList<Spot>()
    val amp = cell * f(0.7f, 1.5f)
    val freq = f(0.004f, 0.009f)
    val rowStep = cell * 0.92f
    var y = -cell * 0.5f
    var row = 0
    while (y < h + cell) {
        val phase = row * f(0.3f, 0.7f)
        var x = -cell * 0.5f + if (row % 2 == 1) cell / 2f else 0f
        while (x < w + cell) {
            val yy = y + sin(x * freq + phase) * amp
            val slope = cos(x * freq + phase) * amp * freq
            out.add(Spot(x, yy, f(0.85f, 1.1f), Math.toDegrees(kotlin.math.atan(slope).toDouble()).toFloat(), (x / w + y / h) / 2f))
            x += cell
        }
        y += rowStep
        row++
    }
    return out
}

/** Recursive split of the page: big doodles next to little ones. */
private fun Scene.mosaicSpots(cell: Float): List<Spot> {
    val out = ArrayList<Spot>()
    val minSide = cell * 0.42f
    fun split(r: RectF, depth: Int) {
        val canSplit = depth < 9 && (r.width() > minSide * 2f || r.height() > minSide * 2f)
        if (!canSplit || (depth >= 3 && chance(0.2f))) {
            val side = min(r.width(), r.height())
            out.add(
                Spot(
                    r.centerX() + f(-0.06f, 0.06f) * r.width(),
                    r.centerY() + f(-0.06f, 0.06f) * r.height(),
                    (side / cell).coerceIn(0.4f, 2.4f),
                    f(-12f, 12f),
                    (r.centerX() / w + r.centerY() / h) / 2f,
                ),
            )
            return
        }
        if (r.width() > r.height()) {
            val x = r.left + r.width() * f(0.38f, 0.62f)
            split(RectF(r.left, r.top, x, r.bottom), depth + 1)
            split(RectF(x, r.top, r.right, r.bottom), depth + 1)
        } else {
            val y = r.top + r.height() * f(0.38f, 0.62f)
            split(RectF(r.left, r.top, r.right, y), depth + 1)
            split(RectF(r.left, y, r.right, r.bottom), depth + 1)
        }
    }
    split(RectF(0f, 0f, w, h), 0)
    return out
}

/** Rays shooting out of one point, doodles growing along each ray. */
private fun Scene.burstSpots(cell: Float): List<Spot> {
    val out = ArrayList<Spot>()
    val cx = this.cx + f(-0.1f, 0.1f) * w
    val cy = h * f(0.38f, 0.52f)
    val maxR = sqrt(max(cx, w - cx).pow(2) + max(cy, h - cy).pow(2)) + cell
    val rays = (TAU * maxR * 0.42f / cell).toInt().coerceIn(8, 30)
    val turn = f(0f, TAU)
    out.add(Spot(cx, cy, 1.3f, 0f, 0f))
    for (k in 0 until rays) {
        val a = turn + k * TAU / rays + f(-0.03f, 0.03f)
        var r = cell * 0.9f
        var step = cell * 0.85f
        while (r < maxR) {
            val x = cx + cos(a) * r
            val y = cy + sin(a) * r
            val t = (r / maxR).coerceIn(0f, 1f)
            if (x > -cell && x < w + cell && y > -cell && y < h + cell) {
                out.add(Spot(x, y, 0.55f + t * 0.95f, Math.toDegrees(a.toDouble()).toFloat() + 90f, t))
            }
            r += step
            step *= 1.1f     // spacing opens up toward the edge
        }
    }
    return out
}

/** Rows marching along a diagonal. */
private fun Scene.diagonalSpots(cell: Float): List<Spot> {
    val out = ArrayList<Spot>()
    val angle = (if (chance(0.5f)) 1f else -1f) * f(28f, 52f)
    val a = Math.toRadians(angle.toDouble()).toFloat()
    val ux = cos(a)
    val uy = sin(a)
    val span = (abs(w * ux) + abs(h * uy)) / 2f + cell * 2f
    val lines = ((abs(w * uy) + abs(h * ux)) / cell).toInt() + 3
    for (l in -1..lines) {
        val off = (l - lines / 2f) * cell
        var d = -span
        var i = 0
        while (d < span) {
            val x = cx + ux * d - uy * off
            val y = cy + uy * d + ux * off
            if (x > -cell && x < w + cell && y > -cell && y < h + cell) {
                out.add(Spot(x, y, f(0.85f, 1.12f), angle, (x / w + y / h) / 2f))
            }
            d += cell * (if (i++ % 2 == 0) 1f else 0.95f)
        }
    }
    return out
}

/** A thick band around the edges, leaving the middle free for the clock. */
private fun Scene.frameSpots(cell: Float): List<Spot> {
    val out = ArrayList<Spot>()
    val bandX = w * f(0.2f, 0.28f)
    val bandY = h * f(0.16f, 0.22f)
    val inner = RectF(bandX, bandY, w - bandX, h - bandY)
    val cols = (w / cell).toInt() + 1
    val rows = (h / cell).toInt() + 1
    for (gy in 0..rows) for (gx in 0..cols) {
        val x = gx * cell + f(0.25f, 0.75f) * cell
        val y = gy * cell + f(0.25f, 0.75f) * cell
        if (inner.contains(x, y)) continue
        // Fade out as spots approach the opening.
        val dx = max(0f, max(inner.left - x, x - inner.right)) / bandX
        val dy = max(0f, max(inner.top - y, y - inner.bottom)) / bandY
        val edge = max(dx, dy).coerceIn(0f, 1f)
        if (chance(0.3f - edge * 0.25f)) continue
        out.add(Spot(x, y, 0.6f + edge * 0.6f, f(-25f, 25f), edge))
    }
    return out
}

/** Doodles packed inside one big silhouette: heart, star, moon or circle. */
private fun Scene.shapeSpots(cell: Float): List<Spot> {
    val out = ArrayList<Spot>()
    val kind = i(0, 4)
    val cx = this.cx
    val cy = h * f(0.4f, 0.48f)
    val rad = min(w * 0.47f, h * 0.34f)
    // Five-point star outline, built once in normalised space (y grows downward, tip at the top).
    val star = FloatArray(20)
    for (k in 0 until 10) {
        val r = if (k % 2 == 0) 1f else 0.42f
        val a = -PI.toFloat() / 2f + k * PI.toFloat() / 5f
        star[k * 2] = cos(a) * r
        star[k * 2 + 1] = sin(a) * r
    }
    fun inStar(px: Float, py: Float): Boolean {
        var inside = false
        var j = 9
        for (i2 in 0 until 10) {
            val xi = star[i2 * 2]; val yi = star[i2 * 2 + 1]
            val xj = star[j * 2]; val yj = star[j * 2 + 1]
            if ((yi > py) != (yj > py) && px < (xj - xi) * (py - yi) / (yj - yi) + xi) inside = !inside
            j = i2
        }
        return inside
    }
    fun inside(x: Float, y: Float): Boolean {
        val nx = (x - cx) / rad
        val ny = (y - cy) / rad
        return when (kind) {
            0 -> {   // heart: lobes at the top, point at the bottom
                val hx = nx * 1.25f
                val hy = -ny * 1.22f + 0.28f
                val q = hx * hx + hy * hy - 1f
                q * q * q - hx * hx * hy * hy * hy <= 0f
            }
            1 -> inStar(nx, ny)
            2 -> nx * nx + ny * ny <= 1f && (nx - 0.5f) * (nx - 0.5f) + ny * ny > 0.85f * 0.85f   // crescent
            else -> nx * nx + ny * ny <= 1f
        }
    }
    val step = cell
    var y = cy - rad * 1.3f
    var row = 0
    while (y < cy + rad * 1.35f) {
        var x = cx - rad * 1.35f + if (row % 2 == 1) step / 2f else 0f
        while (x < cx + rad * 1.35f) {
            if (inside(x, y)) {
                val d = sqrt(((x - cx) / rad).pow(2) + ((y - cy) / rad).pow(2)).coerceIn(0f, 1f)
                out.add(Spot(x + f(-0.08f, 0.08f) * step, y + f(-0.08f, 0.08f) * step, f(0.85f, 1.05f), f(-18f, 18f), d))
            }
            x += step
        }
        y += step * 0.9f
        row++
    }
    // A few strays drifting away from the silhouette.
    repeat(count(8)) {
        out.add(Spot(f(0f, w), f(0f, h), f(0.3f, 0.5f), f(-30f, 30f), 1f))
    }
    return out
}
