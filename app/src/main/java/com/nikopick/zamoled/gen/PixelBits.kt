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


// ---------------------------------------------------------------------------------------------
// The newer Pixel Bits scenes are composed around a phone's home screen: the launcher clock and weather
// sit across the top quarter, so that is only faint sky; the hero goes in the clear band below them,
// about a quarter to a half of the way down; app icons cover the rest, so that stays dark and quiet.
// ---------------------------------------------------------------------------------------------

private abstract class PixelScene(id: String, name: String) : Style(id, name, Category.PIXEL) {
    override val widgetSafe: Boolean get() = true
}

private fun PixelBoard.stars(s: Scene, n: Int, fromRow: Int, toRow: Int, tint: Int = Color.WHITE) {
    repeat(s.count(n)) {
        cell(s.i(0, cols), s.i(fromRow, toRow.coerceAtLeast(fromRow + 1)), if (s.chance(0.8f)) Color.WHITE else lighten(tint, 0.5f), s.i(70, 230))
    }
}

/** A 3x5 pixel face for the few letters the arcade needs. */
private val FONT = mapOf(
    'A' to arrayOf("010", "101", "111", "101", "101"), 'C' to arrayOf("011", "100", "100", "100", "011"),
    'D' to arrayOf("110", "101", "101", "101", "110"), 'E' to arrayOf("111", "100", "110", "100", "111"),
    'G' to arrayOf("011", "100", "101", "101", "011"), 'H' to arrayOf("101", "101", "111", "101", "101"),
    'I' to arrayOf("111", "010", "010", "010", "111"), 'L' to arrayOf("100", "100", "100", "100", "111"),
    'M' to arrayOf("101", "111", "111", "101", "101"), 'N' to arrayOf("110", "101", "101", "101", "101"),
    'O' to arrayOf("010", "101", "101", "101", "010"), 'P' to arrayOf("110", "101", "110", "100", "100"),
    'R' to arrayOf("110", "101", "110", "101", "101"), 'S' to arrayOf("011", "100", "010", "001", "110"),
    'T' to arrayOf("111", "010", "010", "010", "010"), 'V' to arrayOf("101", "101", "101", "101", "010"),
    'Y' to arrayOf("101", "101", "010", "010", "010"),
    'U' to arrayOf("101", "101", "101", "101", "111"), 'F' to arrayOf("111", "100", "110", "100", "100"),
    'B' to arrayOf("110", "101", "110", "101", "110"), 'K' to arrayOf("101", "101", "110", "101", "101"),
    'W' to arrayOf("101", "101", "111", "111", "101"), 'X' to arrayOf("101", "101", "010", "101", "101"),
    '0' to arrayOf("111", "101", "101", "101", "111"), '1' to arrayOf("010", "110", "010", "010", "111"),
    '2' to arrayOf("110", "001", "010", "100", "111"), '3' to arrayOf("110", "001", "010", "001", "110"),
    '4' to arrayOf("101", "101", "111", "001", "001"), '5' to arrayOf("111", "100", "110", "001", "110"),
    '6' to arrayOf("011", "100", "110", "101", "010"), '7' to arrayOf("111", "001", "010", "010", "010"),
    '8' to arrayOf("010", "101", "010", "101", "010"), '9' to arrayOf("010", "101", "011", "001", "110"),
    '-' to arrayOf("000", "000", "111", "000", "000"), 'Z' to arrayOf("111", "001", "010", "100", "111"),
)

private fun pixelTextWidth(text: String): Int = text.map { if (it == ' ') 2 else 4 }.sum() - 1

private fun PixelBoard.text(text: String, gx: Int, gy: Int, color: Int, alpha: Int = 255) {
    var x = gx
    for (ch in text) {
        if (ch == ' ') { x += 2; continue }
        val g = FONT[ch]
        if (g != null) for (r in 0 until 5) for (c in 0 until 3) if (g[r][c] == '1') cell(x + c, gy + r, color, alpha)
        x += 4
    }
}

/** A big dithered sun going down over a black sea, with its reflection broken into glittering bars. */
object PixelSunset : Style("pixel_sunset", "Pixel Sunset", Category.PIXEL) {
    override val autoPalettes = listOf("sunset", "lava", "sakura", "gold", "violet", "candy")
    override val widgetSafe: Boolean get() = true

    override fun draw(s: Scene) {
        val b = PixelBoard(s, 8f)
        val c0 = s.color(0); val c1 = s.color(1); val c2 = s.color(2)
        val hz = (b.rows * 0.44f).toInt()
        val sx = b.cols / 2 + s.i(-14, 15)
        val r = s.i(15, 19)
        val sy = hz - r / 3
        // Sky: dithered bands that brighten toward the horizon and die out to black overhead.
        for (gy in 0 until hz) {
            val t = gy / hz.toFloat()
            for (gx in 0 until b.cols) {
                if (b.dither(gx, gy, t * t * 1.15f)) b.cell(gx, gy, darken(c1, 0.8f))
                if (b.dither(gx, gy, t * t * t * t * 1.3f)) b.cell(gx, gy, darken(c0, 0.55f))
                if (b.dither(gx, gy, t.pow8() * 1.2f)) b.cell(gx, gy, darken(c0, 0.2f))
            }
        }
        b.stars(s, 110, 0, (hz * 0.6f).toInt(), c2)
        // Thin clouds catching the light from underneath.
        repeat(s.count(7)) {
            val gy = s.i((hz * 0.5f).toInt(), hz - 4)
            val gx = s.i(-6, b.cols - 10)
            val len = s.i(14, 42)
            for (k in 0 until len) {
                b.cell(gx + k, gy, darken(c1, 0.72f), 230)
                if (k in 3 until len - 3) b.cell(gx + k, gy + 1, lighten(c0, 0.1f), 190)
            }
        }
        // The sun: banded top to bottom, with stripes cut out of its lower half and a halo around it.
        for (gy in sy - r - 10..hz) for (gx in sx - r - 10..sx + r + 10) {
            val dx = gx - sx; val dy = gy - sy
            val d = sqrt((dx * dx + dy * dy).toFloat())
            if (d > r) {
                if (d < r + 9 && b.dither(gx, gy, (1f - (d - r) / 9f) * 0.55f)) b.cell(gx, gy, darken(c0, 0.35f))
                continue
            }
            val t = (gy - (sy - r)) / (2f * r)
            if (dy > 2 && (dy % 4) < dy / 5 + 1) continue
            val col = when {
                t < 0.3f -> lighten(c2, 0.55f)
                t < 0.55f -> lighten(c0, 0.2f)
                t < 0.8f -> c0
                else -> c1
            }
            b.cell(gx, gy, col)
        }
        // Horizon: a bright line, a sailboat and a low island cutting the light.
        for (gx in 0 until b.cols) b.cell(gx, hz, darken(c0, 0.25f))
        val bx = (sx + s.i(18, 30) * (if (s.chance(0.5f)) 1 else -1)).coerceIn(6, b.cols - 8)
        for (k in 0 until 6) b.rect(bx, hz - 7 + k, bx + k / 2, hz - 7 + k, Color.BLACK)
        b.rect(bx - 2, hz - 1, bx + 4, hz - 1, Color.BLACK)
        val ix = if (bx > sx) s.i(2, 20) else b.cols - s.i(22, 40)
        for (dx in 0..18) b.rect(ix + dx, hz - (1 + ((1f - abs(dx - 9) / 9.5f) * 4f).toInt()), ix + dx, hz - 1, Color.BLACK)
        // Birds.
        repeat(3) {
            val bx2 = sx + s.i(-30, 30); val by = s.i((hz * 0.55f).toInt(), hz - 12)
            b.cell(bx2, by, Color.BLACK); b.cell(bx2 + 1, by + 1, Color.BLACK); b.cell(bx2 + 2, by, Color.BLACK)
            b.cell(bx2 + 3, by + 1, Color.BLACK); b.cell(bx2 + 4, by, Color.BLACK)
        }
        // Reflection: bars that narrow and thin out as they fall away, each row nudged sideways.
        val depth = (b.rows * 0.17f).toInt()
        for (k in 1..depth) {
            if (k % 3 == 2) continue
            val reach = r * (1.05f - k / (depth * 1.3f))
            val shift = (sin(k * 1.7f) * (2f + k * 0.12f)).toInt()
            for (gx in (sx - reach).toInt()..(sx + reach).toInt()) {
                val p = (1f - k / depth.toFloat()) * (1f - abs(gx - sx) / (reach + 1f)) * 1.5f
                if (b.dither(gx, hz + k, p)) b.cell(gx + shift, hz + k, if (k < depth / 3) lighten(c0, 0.2f) else darken(c0, 0.1f + k / depth.toFloat() * 0.5f))
            }
        }
    }

    private fun Float.pow8(): Float { val a = this * this; val c = a * a; return c * c }
}

/** A lighthouse on a cliff sweeping its beam across a dark sea, with the moon laid out on the water. */
object PixelLighthouse : Style("pixel_lighthouse", "Pixel Lighthouse", Category.PIXEL) {
    override val autoPalettes = listOf("ocean", "violet", "gold", "sakura", "sunset")
    override val widgetSafe: Boolean get() = true

    override fun draw(s: Scene) {
        val b = PixelBoard(s, 8f)
        val c0 = s.color(0); val c1 = s.color(1); val c2 = s.color(2)
        val right = s.chance(0.5f)
        fun px(x: Int) = if (right) x else b.cols - 1 - x
        val base = (b.rows * 0.47f).toInt()
        val sea = (b.rows * 0.53f).toInt()
        val tx = b.cols - 22                         // tower's centre in the mirrored space
        val towerH = 36
        b.stars(s, 170, 0, (base * 0.75f).toInt(), c2)
        // Moon on the open-sea side, with its lane of light.
        val mx = s.i(14, 40); val my = (b.rows * 0.3f).toInt()
        b.disc(px(mx), my, 6, lighten(c2, 0.8f)); b.disc(px(mx - 2), my - 2, 2, lighten(c2, 0.92f))
        for (gy in sea..sea + (b.rows * 0.1f).toInt()) {
            val k = gy - sea
            for (gx in mx - 6 + k / 4..mx + 6 - k / 4) if (b.dither(gx, gy, 0.65f - k * 0.02f) && (k % 2 == 0 || s.chance(0.4f))) b.cell(px(gx + (sin(k * 1.9f) * 2f).toInt()), gy, lighten(c2, 0.5f), 200 - k * 5)
        }
        // The beam: a dithered wedge from the lantern across the sky toward the open sea.
        val ly = base - towerH - 3
        for (k in 1..70) {
            val spread = 1 + k / 7
            for (dy in -spread..spread) {
                val p = 0.62f * (1f - k / 70f) * (1f - abs(dy) / (spread + 1f))
                if (b.dither(tx - k, ly + dy + k / 14, p)) b.cell(px(tx - k), ly + dy + k / 14, Color.rgb(255, 240, 170), 150)
            }
        }
        for (k in 1..16) { val spread = 1 + k / 6; for (dy in -spread..spread) if (b.dither(tx + k, ly + dy, 0.4f * (1f - k / 16f))) b.cell(px(tx + k), ly + dy, Color.rgb(255, 240, 170), 120) }
        // The cliff, sloping to the sea, shaded by a dither and black at the foot.
        for (gx in 0 until b.cols) {
            val top = if (gx >= tx - 10) base + ((gx - tx).coerceAtLeast(0) / 8) else base + (tx - 10 - gx) * 6 / 5
            if (top > sea + 2) continue
            val edge = (s.noise.fbm(gx * 0.12f, 9f) * 3f).toInt()
            for (gy in top + edge until b.rows) {
                val depth = gy - top - edge
                if (depth > 18) { b.cell(px(gx), gy, Color.BLACK); continue }
                val tone = if (b.dither(gx, gy, 0.5f - depth * 0.03f)) darken(c1, 0.62f) else darken(c1, 0.86f)
                b.cell(px(gx), gy, tone)
            }
        }
        // The tower: red and white bands, a gallery, the lantern and a cap.
        for (k in 0 until towerH) {
            val half = 4 - k * 2 / towerH
            val col = if ((k / 5) % 2 == 0) Color.rgb(235, 235, 240) else lighten(c0, 0.05f)
            for (x in -half..half) b.cell(px(tx + x), base - k, if (x > half - 2) darken(col, 0.45f) else col)
        }
        b.rect(px(tx + 5), base - towerH, px(tx - 5), base - towerH + 1, Color.rgb(30, 30, 36))
        for (gy in ly - 1..ly + 3) for (x in -2..2) b.cell(px(tx + x), gy, Color.rgb(255, 238, 150))
        b.disc(px(tx), ly + 1, 6, Color.rgb(255, 238, 150).let { withAlpha(it, 40) })
        for (k in 0 until 4) for (x in -(3 - k)..(3 - k)) b.cell(px(tx + x), ly - 2 - k, c0)
        b.cell(px(tx), ly - 6, Color.rgb(200, 200, 210))
        b.rect(px(tx + 1), base - 6, px(tx - 1), base - 1, Color.rgb(24, 24, 30))     // door
        // Sea: dashes of foam that thin out downward, a boat on the horizon.
        for (gy in sea until sea + (b.rows * 0.12f).toInt()) {
            val k = gy - sea
            repeat(s.count(7 - k / 8).coerceAtLeast(1)) {
                val gx = s.i(0, b.cols - 8); val len = s.i(2, 8)
                for (j in 0 until len) b.cell(px(gx + j), gy, lighten(c1, 0.35f), (170 - k * 4).coerceAtLeast(20))
            }
        }
        val bx = s.i(10, 36)
        b.rect(px(bx), sea - 2, px(bx + 6), sea - 1, Color.BLACK)
        for (k in 0 until 4) b.cell(px(bx + 3), sea - 3 - k, Color.BLACK)
        b.cell(px(bx + 6), sea - 3, Color.rgb(255, 210, 110))
    }
}



// ---------------------------------------------------------------------------------------------

private val GAL_BEE = arrayOf(
    "00B00000B00", "000B000B000", "00YYYYYYY00", "0YYRYYYRYY0", "YYYYYYYYYYY",
    "YB0YYYYY0BY", "B00YYYYY00B", "0000Y0Y0000", "000YY0YY000",
)
private val GAL_BUTTERFLY = arrayOf(
    "R000000000R", "RR0R000R0RR", "RRRRB0BRRRR", "RRRRBBBRRRR", "0RRRYBYRRR0", "00RR0B0RR00", "000R000R000",
)
private val GAL_BOSS = arrayOf(
    "000P000P000", "00PPP0PPP00", "0GGPGGGPGG0", "GGGGGWGGGGG", "GGKGGGGGKGG", "GGGGGGGGGGG", "0GG0GGG0GG0", "G0G00G00G0G",
)
private val GAL_SHIP = arrayOf(
    "0000W0000", "0000W0000", "000WWW000", "000WRW000", "0W0WWW0W0", "0WWWWWWW0", "WWRWWWRWW", "WBWWWWWBW", "WB0WBW0BW",
)
private val GAL_BOOM = arrayOf(
    "000R000R000", "R00R0Y0R00R", "0RYY0Y0YYR0", "00YYWWWYY00", "RYYWWWWWYYR", "00YYWWWYY00", "0RYY0Y0YYR0", "R00R0Y0R00R", "000R000R000",
)

/**
 * A Galaga-style space battle: a formation of bugs and bosses across the clear band, a fighter below
 * firing up, a boss pulling with its tractor beam, divers trailing dots, and planets far behind.
 */
object PixelGalaxy : Style("pixel_planet", "Pixel Galaxy", Category.PIXEL) {
    override val autoPalettes = listOf("violet", "ocean", "sunset", "aurora", "graffiti", "sakura")
    override val widgetSafe: Boolean get() = true

    private fun planet(b: PixelBoard, s: Scene, cx: Int, cy: Int, r: Int, a: Int, c: Int, dim: Float) {
        for (y in -r..r) for (x in -r..r) {
            val dx = x / r.toFloat(); val dy = y / r.toFloat()
            val d = dx * dx + dy * dy
            if (d > 1f) continue
            val light = dx * -0.6f + dy * -0.55f + sqrt(1f - d) * 0.55f
            val band = sin(dy * 7f + s.noise.fbm(x * 0.12f, y * 0.12f) * 2.4f)
            val base = if (band > 0.2f) a else if (band > -0.4f) c else darken(a, 0.3f)
            val tone = when {
                light > 0.5f -> lighten(base, 0.2f)
                light > 0.1f -> base
                light > -0.2f -> if (b.dither(x, y, (light + 0.2f) / 0.3f)) base else darken(base, 0.55f)
                else -> darken(base, 0.8f)
            }
            b.cell(cx + x, cy + y, darken(tone, dim))
        }
    }

    override fun draw(s: Scene) {
        val b = PixelBoard(s, 8f)
        val c0 = s.color(0); val c1 = s.color(1); val c2 = s.color(2)
        val R = b.rows; val C = b.cols
        val ink = mapOf(
            'Y' to Color.rgb(255, 214, 60), 'B' to Color.rgb(70, 120, 255), 'R' to Color.rgb(240, 52, 66), 'W' to Color.rgb(236, 240, 252),
            'G' to Color.rgb(70, 220, 110), 'P' to Color.rgb(180, 90, 255), 'K' to Color.rgb(20, 20, 30),
        )
        // Stars in two depths, and short streaks for the scroll.
        repeat(s.count(170)) { b.cell(s.i(0, C), s.i(0, R), Color.WHITE, s.i(40, 130)) }
        repeat(s.count(70)) { b.cell(s.i(0, C), s.i(0, R), if (s.chance(0.6f)) Color.WHITE else lighten(s.anyColor(), 0.5f), s.i(140, 255)) }
        repeat(s.count(26)) {
            val gx = s.i(0, C); val gy = s.i(0, R); val len = s.i(3, 8)
            for (k in 0 until len) b.cell(gx, gy + k, lighten(c1, 0.4f), 150 - k * 18)
        }
        // Worlds far behind: dark, low, and cropped by the edges.
        planet(b, s, (C * 0.1f).toInt(), (R * 0.6f).toInt(), s.i(22, 30), c0, c2, 0.5f)
        planet(b, s, (C * 0.9f).toInt(), (R * 0.51f).toInt(), s.i(7, 10), c1, c0, 0.4f)
        // The formation.
        val n = 9
        val dx = 12; val dy = 10
        val left = (C - n * dx) / 2 + 1
        val top = (R * 0.255f).toInt()
        val sway = s.i(-2, 3)
        var boom = 0
        val gaps = HashSet<Int>()
        repeat(s.i(3, 7)) { gaps += s.i(0, 5 * n) }
        for (row in 0 until 5) for (col in 0 until n) {
            val idx = row * n + col
            if (row == 0 && (col == 0 || col == n - 1 || col == 3 || col == 5) && false) continue
            val gx = left + col * dx + sway
            val gy = top + row * dy
            if (idx in gaps) {
                if (boom < 2) { b.sprite(GAL_BOOM, gx, gy - 1, ink); boom++ }
                continue
            }
            val sprite = when (row) { 0 -> GAL_BOSS; 1, 2 -> GAL_BUTTERFLY; else -> GAL_BEE }
            // A boss that has been hit once turns purple, and the whole row blinks in two tones.
            val tone = if (row == 0 && s.chance(0.3f)) ink + mapOf('G' to Color.rgb(150, 90, 255)) else ink
            b.sprite(sprite, gx, gy, tone)
        }
        // Divers: three bugs peeling out of the formation along curves, leaving dotted trails.
        repeat(3) {
            val sx = left + s.i(0, n) * dx + 5; val sy = top + s.i(1, 5) * dy
            val dir = if (s.chance(0.5f)) 1 else -1
            val len = s.i(30, 46)
            for (k in 1..len) {
                val t = k / len.toFloat()
                val x = sx + dir * (sin(t * 5.2f) * 18f * t + t * 6f)
                val y = sy + t * (R * 0.2f)
                if (k < len && k % 3 == 0) b.cell(x.toInt(), y.toInt(), lighten(c1, 0.4f), (200 * (1f - t * 0.6f)).toInt())
                if (k == len) b.sprite(if (s.chance(0.5f)) GAL_BEE else GAL_BUTTERFLY, x.toInt() - 5, y.toInt() - 4, ink)
            }
        }
        // The player's fighter (sometimes a pair), its shots and the reserve ships.
        val shipY = (R * 0.52f).toInt()
        val shipX = s.i(24, C - 33)
        b.sprite(GAL_SHIP, shipX, shipY, ink)
        if (s.chance(0.45f)) b.sprite(GAL_SHIP, shipX + 10, shipY, ink)
        val gunX = shipX + 4
        for (k in 0 until s.i(2, 4)) for (r in 0 until 4) b.cell(gunX, shipY - 8 - k * 14 - r, if (r < 2) Color.WHITE else lighten(c1, 0.3f))
        repeat(s.i(3, 6)) {
            val gx = left + s.i(0, n) * dx + 5; val gy = top + 5 * dy + s.i(0, (R * 0.18f).toInt())
            for (r in 0 until 3) b.cell(gx, gy + r, Color.rgb(255, 120 + r * 40, 60))
        }
        for (k in 0 until s.i(1, 3)) b.sprite(GAL_SHIP, 8 + k * 11, (R * 0.585f).toInt(), ink.mapValues { darken(it.value, 0.35f) })
        // A boss's tractor beam, dithered blue, reaching down toward the fighter.
        if (s.chance(0.4f)) {
            val bx = left + 4 * dx + 5 + sway; val by = top + 9
            for (k in 0 until (shipY - by - 4)) {
                val half = 2 + k / 4
                for (x in -half..half) if (b.dither(bx + x, by + k, 0.55f) && ((k / 2) % 2 == 0)) b.cell(bx + x, by + k, Color.rgb(90, 160, 255), 170)
            }
        }
        // The scoreboard, inside the margins.
        val white = Color.rgb(236, 240, 252)
        val side = (C * 0.12f).toInt()
        b.text("1UP", side, 13, Color.rgb(240, 52, 66))
        b.text("00", side, 20, white)
        b.text("HIGH SCORE", C / 2 - pixelTextWidth("HIGH SCORE") / 2, 13, Color.rgb(240, 52, 66))
        b.text("30000", C / 2 - pixelTextWidth("30000") / 2, 20, white)
    }
}
