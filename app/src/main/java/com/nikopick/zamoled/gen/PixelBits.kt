package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The Pixel Bits world: 8-bit scenes on a visible grid, built from dithered gradients and
 * hand-drawn sprites, the way old sprite hardware did it. Black is the default for every cell.
 */

/** One reusable paint for thousands of cells; a new Paint per cell would dominate the render. */
internal class PixelBoard(val s: Scene, val px: Float) {
    val cols = (s.w / px).toInt() + 1
    val rows = (s.h / px).toInt() + 1
    private val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }

    fun cell(gx: Int, gy: Int, color: Int, alpha: Int = 255) {
        if (gx < 0 || gy < 0 || gx >= cols || gy >= rows) return
        paint.color = color
        paint.alpha = alpha
        s.canvas.drawRect(gx * px, gy * px, (gx + 1) * px, (gy + 1) * px, paint)
    }

    fun rect(gx0: Int, gy0: Int, gx1: Int, gy1: Int, color: Int, alpha: Int = 255) {
        for (y in gy0..gy1) for (x in gx0..gx1) cell(x, y, color, alpha)
    }

    fun sprite(rows: Array<String>, gx: Int, gy: Int, colors: Map<Char, Int>, flip: Boolean = false) {
        for ((r, row) in rows.withIndex()) {
            val line = if (flip) row.reversed() else row
            for ((c, ch) in line.withIndex()) cell(gx + c, gy + r, colors[ch] ?: continue)
        }
    }

    fun disc(cgx: Int, cgy: Int, r: Int, color: Int) {
        for (y in -r..r) for (x in -r..r) if (x * x + y * y <= r * r + r * 0.4f) cell(cgx + x, cgy + y, color)
    }

    /** Ordered dithering: true where a cell at [t] (0..1) should take the lighter tone. */
    fun dither(gx: Int, gy: Int, t: Float): Boolean = t > (BAYER[(gy and 3) * 4 + (gx and 3)] + 0.5f) / 16f

    companion object {
        private val BAYER = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)
    }
}

private val SHIP = arrayOf(
    "00000A00000",
    "0000AAA0000",
    "0000ABA0000",
    "0000AAA0000",
    "000AAAAA000",
    "00CAAAAAC00",
    "0CCAAAAACC0",
    "CC0AAAAA0CC",
    "C000DDD000C",
    "00000E00000",
    "00000F00000",
)
private val UFO = arrayOf(
    "0000011100000",
    "0001111111000",
    "0022222222200",
    "0234323432320",
    "2222222222222",
    "0020002000200",
)
private val SATELLITE = arrayOf(
    "22200000222",
    "22200100222",
    "22211111222",
    "22200100222",
    "22200000222",
)
private val ASTRONAUT = arrayOf(
    "0011100",
    "0122210",
    "0123210",
    "0122210",
    "0011100",
    "0411140",
    "4411144",
    "0011100",
    "0010100",
)

/** A deep-space scene: dithered nebula, a banded ringed planet, ships, asteroids and shooting stars. */
object PixelSpace : Style("pixel_space", "Pixel Space", Category.PIXEL) {
    override val autoPalettes = listOf("violet", "ocean", "sunset", "aurora", "graffiti", "sakura")

    override fun draw(s: Scene) {
        val b = PixelBoard(s, 8f)
        val c0 = s.color(0)
        val c1 = s.color(1)
        val c2 = s.color(2)

        // Nebula: noise thresholded into three tones, dithered where one tone gives way to the next.
        val off = s.f(0f, 100f)
        for (gy in 0 until b.rows) for (gx in 0 until b.cols) {
            val n = s.noise.fbm(gx * 0.035f + off, gy * 0.028f) * 0.5f + 0.5f
            val m = s.noise.fbm(gx * 0.07f + 40f, gy * 0.06f + off) * 0.5f + 0.5f
            val v = n * 0.8f + m * 0.2f
            val level = (v - 0.55f) * 4f * s.density.coerceIn(0.5f, 1.6f)
            if (level <= 0f) continue
            val tone = when {
                level < 0.8f -> darken(c0, 0.82f)
                level < 1.6f -> darken(c2, 0.7f)
                else -> darken(c1, 0.55f)
            }
            if (b.dither(gx, gy, min(level, 1f))) b.cell(gx, gy, tone)
        }

        // Stars of three sizes, a few twinkling as plus signs.
        repeat(s.count(300)) {
            val gx = s.i(0, b.cols); val gy = s.i(0, b.rows)
            b.cell(gx, gy, if (s.chance(0.8f)) Color.WHITE else lighten(s.anyColor(), 0.5f), s.i(110, 256))
        }
        repeat(s.count(14)) {
            val gx = s.i(2, b.cols - 2); val gy = s.i(2, b.rows - 2)
            val col = lighten(s.anyColor(), 0.6f)
            b.cell(gx, gy, Color.WHITE)
            b.cell(gx - 1, gy, col, 190); b.cell(gx + 1, gy, col, 190); b.cell(gx, gy - 1, col, 190); b.cell(gx, gy + 1, col, 190)
        }

        // A ringed planet: bands, a dithered terminator, and a ring that passes in front and behind.
        val pr = s.i(18, 26)
        val pcx = (b.cols * s.f(0.3f, 0.7f)).toInt()
        val pcy = (b.rows * s.f(0.22f, 0.4f)).toInt()
        val tilt = s.f(0.28f, 0.42f)
        fun ringCell(x: Int, y: Int): Float {
            val rx = x / (pr * 1.9f)
            val ry = y / (pr * 1.9f * tilt)
            return rx * rx + ry * ry
        }
        fun drawRing(front: Boolean) {
            for (y in -pr..pr) for (x in -(pr * 2)..(pr * 2)) {
                val d = ringCell(x, y)
                if (d < 0.52f || d > 1f) continue
                val behind = y < 0
                if (behind == front) continue
                // Planet hides the back of the ring.
                if (!front && x * x + y * y * 0 < pr * pr && sqrt((x * x + (y / tilt / 1f) * 0f).toFloat()) < pr * 0.9f && abs(y) < pr) {
                    if (x * x + y * y < pr * pr) continue
                }
                val col = if (d < 0.7f) lighten(c1, 0.15f) else if (d < 0.85f) darken(c1, 0.2f) else lighten(c1, 0.3f)
                b.cell(pcx + x, pcy + (y * 0.9f).toInt(), col)
            }
        }
        drawRing(false)
        val lx = -0.6f
        val ly = -0.55f
        for (y in -pr..pr) for (x in -pr..pr) {
            val dx = x / pr.toFloat(); val dy = y / pr.toFloat()
            val d = dx * dx + dy * dy
            if (d > 1f) continue
            val dz = sqrt(1f - d)
            val light = dx * lx + dy * ly + dz * 0.55f
            val band = sin(dy * 9f + s.noise.fbm(x * 0.15f, y * 0.15f) * 2f)
            val base = if (band > 0.25f) c0 else if (band > -0.4f) c2 else darken(c0, 0.3f)
            val tone = when {
                light > 0.55f -> lighten(base, 0.35f)
                light > 0.15f -> base
                light > -0.2f -> if (b.dither(x, y, (light + 0.2f) / 0.35f)) base else darken(base, 0.55f)
                else -> darken(base, 0.75f)
            }
            b.cell(pcx + x, pcy + y, tone)
        }
        drawRing(true)
        // A moon, and the asteroid belt.
        b.disc(pcx + (pr * s.f(1.4f, 1.8f)).toInt(), pcy + pr + 6, 3, Color.rgb(190, 190, 205))
        repeat(s.count(26)) {
            val gx = s.i(0, b.cols); val gy = s.i((b.rows * 0.5f).toInt(), b.rows)
            val r = s.i(1, 4)
            b.disc(gx, gy, r, Color.rgb(110 + s.i(0, 40), 100 + s.i(0, 30), 95 + s.i(0, 30)))
            if (r > 1) b.cell(gx - 1, gy - 1, Color.rgb(170, 160, 150))
        }
        // Traffic: a ship climbing, a UFO with its beam, a satellite, an astronaut adrift.
        val colors = mapOf('A' to Color.rgb(205, 210, 225), 'B' to Color.rgb(120, 220, 255), 'C' to c0, 'D' to Color.rgb(90, 95, 110),
            'E' to Color.rgb(255, 170, 60), 'F' to Color.rgb(255, 230, 120))
        b.sprite(SHIP, (b.cols * s.f(0.2f, 0.6f)).toInt(), (b.rows * s.f(0.62f, 0.78f)).toInt(), colors)
        val ux = (b.cols * s.f(0.55f, 0.8f)).toInt(); val uy = (b.rows * s.f(0.5f, 0.62f)).toInt()
        b.sprite(UFO, ux, uy, mapOf('1' to Color.rgb(140, 230, 255), '2' to Color.rgb(150, 155, 175), '3' to Color.rgb(190, 195, 210), '4' to Color.rgb(255, 230, 90)))
        for (k in 1..9) for (x in -k / 2..k / 2) b.cell(ux + 6 + x, uy + 5 + k, Color.rgb(255, 240, 150), (170 - k * 14).coerceAtLeast(20))
        b.sprite(SATELLITE, s.i(3, b.cols - 14), (b.rows * s.f(0.1f, 0.45f)).toInt(), mapOf('1' to Color.WHITE, '2' to Color.rgb(70, 120, 230)))
        b.sprite(ASTRONAUT, (b.cols * s.f(0.1f, 0.8f)).toInt(), (b.rows * s.f(0.82f, 0.92f)).toInt(), mapOf('1' to Color.WHITE, '2' to Color.rgb(60, 70, 90), '3' to Color.rgb(110, 210, 255), '4' to Color.rgb(200, 205, 220)))
        // Shooting stars.
        repeat(s.i(1, 3) + (s.density * 1.5f).toInt()) {
            val gx = s.i(8, b.cols - 8); val gy = s.i(2, (b.rows * 0.5f).toInt())
            for (k in 0 until 9) b.cell(gx - k, gy - k / 2, Color.WHITE, 255 - k * 26)
        }
    }
}

/** A night skyline in pixels: layered towers with lit windows, a moon, antennas and a flying car. */
object PixelCity : Style("pixel_city", "Pixel City", Category.PIXEL) {
    override val autoPalettes = listOf("violet", "sunset", "ocean", "graffiti", "sakura", "cyan")

    override fun draw(s: Scene) {
        val b = PixelBoard(s, 8f)
        val c0 = s.color(0)
        val c1 = s.color(1)
        val horizon = (b.rows * s.f(0.62f, 0.7f)).toInt()
        // Sky: a dithered glow that rises from the skyline and gives out to black overhead.
        for (gy in 0 until horizon) {
            val t = gy / horizon.toFloat()                         // 0 overhead .. 1 at the skyline
            val glow = t * t * t * 0.9f
            for (gx in 0 until b.cols) if (b.dither(gx, gy, glow)) b.cell(gx, gy, darken(c0, 0.78f))
            for (gx in 0 until b.cols) if (b.dither(gx, gy, glow * glow * 1.1f)) b.cell(gx, gy, darken(c1, 0.6f))
        }
        repeat(s.count(150)) { b.cell(s.i(0, b.cols), s.i(0, (horizon * 0.7f).toInt()), Color.WHITE, s.i(100, 256)) }
        // The moon.
        val mgx = (b.cols * s.f(0.2f, 0.8f)).toInt(); val mgy = (horizon * s.f(0.2f, 0.45f)).toInt()
        b.disc(mgx, mgy, 8, lighten(c0, 0.8f))
        b.disc(mgx - 2, mgy - 2, 3, lighten(c0, 0.9f))
        b.disc(mgx + 3, mgy + 3, 2, lighten(c0, 0.55f))

        // Towers in three depths. Far towers are dim, near ones black with the brightest windows.
        val layers = 3
        for (l in 0 until layers) {
            val depth = l / (layers - 1f)                                // 0 far .. 1 near
            val body = darken(mix(c0, c1, 0.5f), 0.86f - 0.12f * (1f - depth))
            val window = lighten(mix(c1, Color.rgb(255, 214, 120), 0.5f), 0.1f)
            var gx = -2
            while (gx < b.cols) {
                val wd = s.i(6, 14 + (depth * 6f).toInt())
                val ht = (s.i(16, 46) * (0.75f + 0.8f * depth) + (b.rows * 0.05f)).toInt()
                val base = horizon + (depth * b.rows * 0.07f).toInt() + 6
                val top = base - ht
                val shade = if (l == layers - 1) Color.BLACK else body
                b.rect(gx, top, gx + wd - 1, base + b.rows, shade)
                // Roofline: a step, a water tank or an antenna.
                when (s.i(0, 4)) {
                    0 -> b.rect(gx + 1, top - 2, gx + wd / 2, top - 1, shade)
                    1 -> { b.rect(gx + wd / 2, top - 6, gx + wd / 2, top - 1, shade); if (s.chance(0.7f)) b.cell(gx + wd / 2, top - 7, Color.rgb(255, 60, 60)) }
                    2 -> b.rect(gx + 2, top - 3, gx + wd - 3, top - 1, shade)
                    else -> Unit
                }
                // Windows: a grid of 2x2 panes, lit at random; the near layer is lit more.
                val chance = (0.22f + 0.25f * depth) * s.density.coerceIn(0.5f, 1.8f)
                var wy = top + 2
                while (wy < base - 2 && wy < b.rows) {
                    var wx = gx + 1
                    while (wx < gx + wd - 2) {
                        if (s.chance(chance)) {
                            val lit = if (s.chance(0.2f)) lighten(c0, 0.4f) else window
                            b.rect(wx, wy, wx + 1, wy + 1, lit, if (l == 0) 150 else 255)
                        }
                        wx += 3
                    }
                    wy += 4
                }
                gx += wd + s.i(0, 3)
            }
        }
        // Ground: everything below the nearest row of towers is plain black.
        b.rect(0, horizon + (b.rows * 0.07f).toInt() + 6, b.cols, b.rows, Color.BLACK)
        // A flying car and its trail.
        val cx = (b.cols * s.f(0.2f, 0.75f)).toInt(); val cy = (horizon * s.f(0.5f, 0.85f)).toInt()
        b.rect(cx, cy, cx + 6, cy + 1, Color.rgb(230, 235, 245)); b.rect(cx + 1, cy - 1, cx + 4, cy - 1, Color.rgb(120, 220, 255))
        b.cell(cx + 7, cy, Color.rgb(255, 80, 70))
        for (k in 1..14) b.cell(cx - k, cy, Color.rgb(255, 80, 70), (200 - k * 13).coerceAtLeast(10))
    }
}

/** Snow-capped peaks in three layers with dithered shading, pine woods and a cabin with a lit window. */
object PixelLandscape : Style("pixel_landscape", "Pixel Peaks", Category.PIXEL) {
    override val autoPalettes = listOf("ocean", "violet", "sunset", "aurora", "sakura", "gold")

    override fun draw(s: Scene) {
        val b = PixelBoard(s, 8f)
        val c0 = s.color(0)
        val c1 = s.color(1)
        val c2 = s.color(2)
        repeat(s.count(190)) { b.cell(s.i(0, b.cols), s.i(0, (b.rows * 0.6f).toInt()), if (s.chance(0.8f)) Color.WHITE else lighten(s.anyColor(), 0.5f), s.i(90, 256)) }
        val mgx = (b.cols * s.f(0.2f, 0.8f)).toInt(); val mgy = (b.rows * s.f(0.14f, 0.26f)).toInt()
        b.disc(mgx, mgy, 7, lighten(c2, 0.75f))
        b.disc(mgx - 2, mgy - 2, 2, lighten(c2, 0.9f)); b.disc(mgx + 2, mgy + 3, 2, lighten(c2, 0.55f))
        // Birds against the moon.
        repeat(s.count(3)) { val bx = mgx + s.i(-18, 18); val by = mgy + s.i(-12, 14); b.cell(bx, by, Color.BLACK); b.cell(bx + 1, by + 1, Color.BLACK); b.cell(bx + 2, by, Color.BLACK) }

        var frontTop = IntArray(0)
        for (layer in 0..2) {
            val t = layer / 2f                                             // 0 far .. 1 near
            val top = IntArray(b.cols) { x ->
                val n = (s.noise.fbm(x * 0.03f * (1f + layer * 0.5f), layer * 7f + 2f) + 0.55f).coerceIn(0f, 1.2f)
                (b.rows * (0.64f + 0.1f * layer) - n * b.rows * (0.2f - 0.04f * layer)).toInt()
            }
            val rock = darken(mix(c0, c1, t), 0.55f + 0.3f * t)
            val lit = lighten(rock, 0.28f)
            for (x in 0 until b.cols) {
                val slope = (top[min(x + 1, b.cols - 1)] - top[max(x - 1, 0)]).toFloat()
                for (y in top[x] until b.rows) {
                    val depth = y - top[x]
                    // The side facing the moon is lit, shading off with a dither as it goes down the face.
                    val litness = (0.55f - slope * 0.12f) - depth / 22f
                    var col = if (b.dither(x, y, litness)) lit else rock
                    if (depth > 26) col = darken(rock, 0.4f + t * 0.3f)
                    // Snow on the high summits of the two far ranges.
                    if (layer < 2 && depth < 4 + (if (b.dither(x, y, 0.5f)) 1 else 0) && top[x] < b.rows * 0.5f) col = if (depth < 2) Color.WHITE else lighten(c2, 0.7f)
                    b.cell(x, y, col)
                }
            }
            if (layer == 2) frontTop = top
        }
        // Pines on the front range, and a cabin with smoke.
        val pine = darken(c1, 0.9f)
        repeat(s.count(16)) {
            val x = s.i(2, b.cols - 2)
            val base = frontTop[x] + 1
            val h = s.i(7, 13)
            for (r in 0 until h) for (dx in -(r / 3)..(r / 3)) b.cell(x + dx, base - h + r, if (dx < 0) pine else darken(pine, 0.4f))
            b.cell(x, base, Color.rgb(60, 40, 30))
        }
        val cx = s.i(8, b.cols - 16)
        val cb = frontTop[cx]
        b.rect(cx, cb - 6, cx + 7, cb - 1, Color.rgb(90, 55, 40)); b.rect(cx - 1, cb - 8, cx + 8, cb - 7, Color.rgb(60, 70, 85))
        b.rect(cx + 2, cb - 5, cx + 3, cb - 3, Color.rgb(255, 210, 100)); b.rect(cx + 5, cb - 10, cx + 5, cb - 8, Color.rgb(70, 60, 55))
        for (k in 0 until 6) b.cell(cx + 5 + (k % 2), cb - 11 - k, Color.WHITE, 200 - k * 30)
    }
}
