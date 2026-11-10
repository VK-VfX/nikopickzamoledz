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

/** A banded ringed planet, big and centred in the clear band, with moons and a ship crossing below it. */
object PixelPlanet : Style("pixel_planet", "Pixel Planet", Category.PIXEL) {
    override val autoPalettes = listOf("violet", "ocean", "sunset", "aurora", "graffiti", "sakura")
    override val widgetSafe: Boolean get() = true

    override fun draw(s: Scene) {
        val b = PixelBoard(s, 8f)
        val c0 = s.color(0); val c1 = s.color(1); val c2 = s.color(2)
        // A faint nebula low on the page, well behind where the icons sit.
        val off = s.f(0f, 100f)
        for (gy in (b.rows * 0.5f).toInt() until b.rows) for (gx in 0 until b.cols) {
            val fadeIn = ((gy / b.rows.toFloat() - 0.5f) * 4f).coerceIn(0f, 1f)
            val v = (s.noise.fbm(gx * 0.035f + off, gy * 0.03f) * 0.5f + 0.5f) - 0.5f
            if (v > 0f && b.dither(gx, gy, v * 5f * fadeIn)) b.cell(gx, gy, darken(if (v > 0.12f) c2 else c0, 0.82f))
        }
        b.stars(s, 260, 0, b.rows, c1)
        val pr = s.i(22, 27)
        val pcx = b.cols / 2 + s.i(-10, 11)
        val pcy = (b.rows * 0.37f).toInt()
        val tilt = s.f(0.2f, 0.3f)
        val rx = pr * 1.95f
        val ry = rx * tilt
        fun ring(front: Boolean) {
            for (y in -ry.toInt()..ry.toInt()) for (x in -rx.toInt()..rx.toInt()) {
                val d = (x / rx) * (x / rx) + (y / ry) * (y / ry)
                if (d < 0.5f || d > 1f) continue
                if ((y >= 0) != front) continue
                if (!front && x * x + y * y < pr * pr) continue
                val col = when { d < 0.66f -> lighten(c1, 0.2f); d < 0.82f -> darken(c1, 0.25f); else -> lighten(c1, 0.35f) }
                b.cell(pcx + x, pcy + y, col)
            }
        }
        ring(false)
        for (y in -pr..pr) for (x in -pr..pr) {
            val dx = x / pr.toFloat(); val dy = y / pr.toFloat()
            val d = dx * dx + dy * dy
            if (d > 1f) continue
            val dz = sqrt(1f - d)
            val light = dx * -0.6f + dy * -0.55f + dz * 0.55f
            val band = sin(dy * 8f + s.noise.fbm(x * 0.12f, y * 0.12f) * 2.4f)
            val base = if (band > 0.3f) c0 else if (band > -0.35f) c2 else darken(c0, 0.3f)
            val tone = when {
                light > 0.55f -> lighten(base, 0.35f)
                light > 0.15f -> base
                light > -0.2f -> if (b.dither(x, y, (light + 0.2f) / 0.35f)) base else darken(base, 0.55f)
                else -> darken(base, 0.78f)
            }
            b.cell(pcx + x, pcy + y, tone)
        }
        ring(true)
        // Two moons, a ship climbing past, and rocks drifting low.
        b.disc(pcx + (pr * s.f(1.5f, 1.9f)).toInt(), pcy - pr + 2, 3, Color.rgb(190, 190, 205))
        b.disc(pcx - (pr * s.f(1.5f, 1.8f)).toInt(), pcy + pr - 2, 2, Color.rgb(150, 140, 135))
        val colors = mapOf('A' to Color.rgb(205, 210, 225), 'B' to Color.rgb(120, 220, 255), 'C' to c0, 'D' to Color.rgb(90, 95, 110),
            'E' to Color.rgb(255, 170, 60), 'F' to Color.rgb(255, 230, 120))
        b.sprite(SHIP, (b.cols * s.f(0.12f, 0.7f)).toInt(), (b.rows * s.f(0.52f, 0.56f)).toInt(), colors)
        repeat(s.count(18)) {
            val gx = s.i(0, b.cols); val gy = s.i((b.rows * 0.6f).toInt(), b.rows)
            val rr = s.i(1, 4)
            b.disc(gx, gy, rr, Color.rgb(100 + s.i(0, 40), 92 + s.i(0, 30), 88 + s.i(0, 30)))
            if (rr > 1) b.cell(gx - 1, gy - 1, Color.rgb(165, 155, 145))
        }
        repeat(1 + (s.density * 1.4f).toInt()) {
            val gx = s.i(10, b.cols - 6); val gy = s.i((b.rows * 0.07f).toInt(), (b.rows * 0.2f).toInt())
            for (k in 0 until 9) b.cell(gx - k, gy - k / 2, Color.WHITE, 255 - k * 26)
        }
    }
}

/** A night train crossing the clear band: lit windows, steam, a moon behind the hills and poles with wires. */
object PixelTrain : Style("pixel_train", "Pixel Night Train", Category.PIXEL) {
    override val autoPalettes = listOf("ocean", "violet", "sunset", "gold", "sakura")
    override val widgetSafe: Boolean get() = true

    override fun draw(s: Scene) {
        val b = PixelBoard(s, 8f)
        val c0 = s.color(0); val c1 = s.color(1); val c2 = s.color(2)
        val g = (b.rows * 0.45f).toInt()                       // the rail
        val dir = if (s.chance(0.5f)) 1 else -1
        b.stars(s, 150, 0, (g * 0.7f).toInt(), c2)
        // The moon, and hills dithered against a faint glow.
        val mx = (b.cols * s.f(0.22f, 0.78f)).toInt(); val my = (b.rows * 0.31f).toInt()
        b.disc(mx, my, 9, lighten(c2, 0.78f)); b.disc(mx - 3, my - 2, 3, lighten(c2, 0.92f)); b.disc(mx + 3, my + 3, 2, lighten(c2, 0.55f))
        for (gy in my - 20..g) for (gx in 0 until b.cols) {
            val d = sqrt(((gx - mx) * (gx - mx) + (gy - my) * (gy - my)).toFloat())
            if (d > 9f && d < 22f && b.dither(gx, gy, (1f - (d - 9f) / 13f) * 0.4f)) b.cell(gx, gy, darken(c2, 0.78f))
        }
        for (gx in 0 until b.cols) {
            val top = g - 6 - ((s.noise.fbm(gx * 0.04f, 3f) + 0.5f).coerceIn(0f, 1.2f) * 17f).toInt()
            for (gy in top..g) b.cell(gx, gy, if (b.dither(gx, gy, 0.25f) && gy - top < 3) darken(c1, 0.7f) else darken(c1, 0.9f))
        }
        // Poles and sagging wires.
        val poleXs = (-5 until b.cols step 38).map { it + (s.noise.at(it * 0.1f, 1f) * 4f).toInt() }
        for (px in poleXs) { for (gy in g - 25..g) b.cell(px, gy, Color.rgb(44, 40, 38)); b.rect(px - 3, g - 25, px + 3, g - 25, Color.rgb(44, 40, 38)) }
        for (k in 0 until poleXs.size - 1) {
            val x0 = poleXs[k]; val x1 = poleXs[k + 1]
            for (gx in x0..x1) {
                val t = (gx - x0) / (x1 - x0).toFloat()
                b.cell(gx, g - 25 + (sin(t * 3.1416f) * 4f).toInt(), Color.rgb(70, 66, 66), 200)
            }
        }
        // Wet ground: ballast, sleepers and the rails.
        for (gy in g + 1..g + 7) for (gx in 0 until b.cols) if (b.dither(gx, gy, 0.55f - (gy - g) * 0.07f)) b.cell(gx, gy, Color.rgb(58, 56, 60))
        for (gx in 0 until b.cols step 4) b.cell(gx, g + 1, Color.rgb(70, 52, 40))
        for (gx in 0 until b.cols) { b.cell(gx, g, Color.rgb(150, 155, 165)); b.cell(gx, g + 2, Color.rgb(90, 95, 105)) }

        // The train: a locomotive and carriages, drawn facing right, then flipped by [dir].
        fun px(x: Int) = if (dir > 0) x else b.cols - 1 - x
        val body = darken(c0, 0.72f)
        val stripe = c1
        val glass = Color.rgb(255, 205, 120)
        var x = -4
        val carriages = 4
        val loco = 26
        // Locomotive.
        fun block(x0: Int, x1: Int, y0: Int, y1: Int, col: Int, alpha: Int = 255) {
            for (xx in x0..x1) for (yy in y0..y1) b.cell(px(xx), yy, col, alpha)
        }
        block(x, x + 17, g - 9, g - 3, darken(c0, 0.78f))                       // boiler
        block(x + 18, x + loco - 1, g - 14, g - 3, body)                       // cab
        block(x + 17, x + loco, g - 15, g - 14, darken(body, 0.4f))             // cab roof
        block(x + 3, x + 6, g - 14, g - 10, darken(body, 0.5f))                 // chimney
        block(x + 2, x + 7, g - 15, g - 14, darken(body, 0.3f))
        block(x + 10, x + 12, g - 12, g - 10, darken(body, 0.5f))               // dome
        block(x + 20, x + 23, g - 12, g - 9, glass)                             // cab window
        block(x, x + 17, g - 6, g - 6, stripe)
        block(x - 1, x - 1, g - 8, g - 4, Color.rgb(255, 240, 160))             // headlamp
        for (wx in intArrayOf(x + 2, x + 8, x + 14, x + 20)) { b.disc(px(wx), g - 2, 2, Color.rgb(30, 30, 36)); b.cell(px(wx), g - 2, c1) }
        // Headlamp beam along the rails, and smoke streaming back.
        for (k in 1..22) for (dy in -(k / 5)..(k / 5)) if (b.dither(px(x - 1 - k), g - 6 + dy, 0.5f * (1f - k / 22f))) b.cell(px(x - 1 - k), g - 6 + dy, Color.rgb(255, 240, 170), 140)
        for (k in 0 until 6) {
            val sxp = x + 5 + k * 5
            val syp = g - 18 - k * 2 - (sin(k * 1.3f) * 1.5f).toInt()
            b.disc(px(sxp), syp, 2 + k / 2, Color.rgb(150, 150, 165))
        }
        x += loco + 1
        repeat(carriages + 2) {
            val len = 25
            block(x, x + len - 1, g - 12, g - 3, body)
            block(x, x + len - 1, g - 13, g - 13, darken(body, 0.35f))
            block(x, x + len - 1, g - 6, g - 6, stripe)
            block(x, x, g - 12, g - 3, darken(body, 0.5f)); block(x + len - 1, x + len - 1, g - 12, g - 3, darken(body, 0.5f))
            var wx = x + 2
            while (wx < x + len - 4) {
                if (s.chance(0.86f)) {
                    block(wx, wx + 2, g - 11, g - 8, glass)
                    if (s.chance(0.35f)) block(wx + 1, wx + 1, g - 10, g - 9, Color.rgb(40, 28, 24))             // a passenger
                }
                wx += 4
            }
            for (w in intArrayOf(x + 3, x + len - 5)) { b.disc(px(w), g - 2, 2, Color.rgb(30, 30, 36)); b.cell(px(w), g - 2, stripe) }
            x += len + 1
        }
        // Light spilling onto the wet ballast below the windows.
        for (gx in 0 until b.cols) if (s.chance(0.35f)) for (k in 3..6) if (b.dither(gx, g + k, 0.45f - k * 0.05f)) b.cell(gx, g + k, glass, 70)
    }
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

/** An arcade cabinet in the dark, its marquee and screen lit, flanked by two more fading away. */
object PixelArcade : Style("pixel_arcade", "Pixel Arcade", Category.PIXEL) {
    override val autoPalettes = listOf("cyber", "violet", "ocean", "graffiti", "sunset", "lava")
    override val widgetSafe: Boolean get() = true

    private val SAYINGS = listOf("INSERT" to "COIN", "GAME" to "OVER", "PLAY" to "AGAIN", "HI" to "SCORE")
    private val ALIEN = arrayOf("0100010", "0010100", "0111110", "1101011", "1111111", "1011101", "0100010")

    private fun cabinet(b: PixelBoard, s: Scene, cx: Int, y0: Int, w: Int, a: Float, hero: Boolean) {
        val c0 = s.color(0); val c1 = s.color(1); val c2 = s.color(2)
        fun c(x: Int, y: Int, col: Int, alpha: Int = 255) = b.cell(x, y, col, (alpha * a).toInt().coerceIn(0, 255))
        fun r(x0: Int, y0_: Int, x1: Int, y1: Int, col: Int, alpha: Int = 255) { for (yy in y0_..y1) for (xx in x0..x1) c(xx, yy, col, alpha) }
        val l = cx - w / 2; val rr = cx + w / 2 - 1
        val body = Color.rgb(10, 10, 16)
        val total = 56
        r(l, y0, rr, y0 + total, body)
        // Neon trim down both sides, and a side-art stripe.
        for (yy in y0 until y0 + total) { c(l, yy, c0); c(rr, yy, c1); c(l + 2, yy, darken(c0, 0.5f), 150); c(rr - 2, yy, darken(c1, 0.5f), 150) }
        // Marquee.
        r(l + 3, y0 + 1, rr - 3, y0 + 6, Color.BLACK)
        for (x in l + 3..rr - 3) { c(x, y0 + 1, lighten(c0, 0.3f)); c(x, y0 + 6, lighten(c1, 0.3f)) }
        if (hero) b.text("ARCADE", cx - pixelTextWidth("ARCADE") / 2, y0 + 2, Color.WHITE)
        else r(cx - 9, y0 + 3, cx + 9, y0 + 4, lighten(c2, 0.2f), 140)
        // Screen: a bezel, a CRT glow, and the game.
        val sx0 = l + 4; val sx1 = rr - 4; val sy0 = y0 + 9; val sy1 = y0 + 31
        r(sx0 - 1, sy0 - 1, sx1 + 1, sy1 + 1, Color.rgb(28, 28, 36))
        r(sx0, sy0, sx1, sy1, Color.BLACK)
        val mx = (sx0 + sx1) / 2f; val my = (sy0 + sy1) / 2f
        for (yy in sy0..sy1) for (xx in sx0..sx1) {
            val d = sqrt((xx - mx) * (xx - mx) / 160f + (yy - my) * (yy - my) / 130f)
            if (b.dither(xx, yy, (1f - d) * 0.7f)) c(xx, yy, darken(c1, 0.72f))
        }
        if (hero) {
            val (top, bottom) = SAYINGS[s.i(0, SAYINGS.size)]
            val lit = Color.WHITE
            b.text(top, cx - pixelTextWidth(top) / 2, sy0 + 8, lit)
            b.text(bottom, cx - pixelTextWidth(bottom) / 2, sy0 + 15, lighten(c0, 0.4f))
            for (k in 0 until 3) b.sprite(ALIEN, sx0 + 2 + k * 9, sy0 + 1 - 0, mapOf('1' to withAlpha(lighten(c2, 0.2f), 150)))
            r(cx - 1, sy1 - 2, cx + 1, sy1 - 2, Color.rgb(120, 255, 160)); c(cx, sy1 - 3, Color.rgb(120, 255, 160))
            for (yy in sy0..sy1 step 2) for (xx in sx0..sx1) c(xx, yy, Color.BLACK, 70)
        } else {
            r(sx0 + 2, sy0 + 2, sx1 - 2, sy1 - 2, darken(c2, 0.78f), 90)
        }
        // Control panel: a lit lip, a stick, three buttons and the coin door.
        r(l + 1, y0 + 33, rr - 1, y0 + 35, Color.rgb(20, 20, 30))
        for (x in l + 1..rr - 1) c(x, y0 + 33, lighten(c0, 0.2f), 200)
        r(l + 5, y0 + 36, rr - 5, y0 + 40, Color.rgb(16, 16, 24))
        c(l + 9, y0 + 38, Color.rgb(230, 40, 60)); c(l + 10, y0 + 38, Color.rgb(230, 40, 60)); c(l + 9, y0 + 37, Color.rgb(230, 40, 60)); c(l + 10, y0 + 37, Color.rgb(230, 40, 60))
        c(l + 9, y0 + 39, Color.rgb(90, 90, 100))
        for ((k, col) in listOf(c0, c1, c2).withIndex()) { val bx = cx + 2 + k * 4; r(bx, y0 + 37, bx + 1, y0 + 38, col) }
        r(cx - 3, y0 + 44, cx + 3, y0 + 49, Color.rgb(8, 8, 12))
        r(cx - 1, y0 + 45, cx + 1, y0 + 46, Color.rgb(255, 200, 80)); r(cx - 1, y0 + 48, cx + 1, y0 + 48, Color.rgb(255, 80, 70), 220)
        // Light on the floor in front of it.
        if (hero) for (k in 1..9) for (xx in l - 4..rr + 4) if (b.dither(xx, y0 + total + k, (0.42f - k * 0.045f) * (1f - abs(xx - cx) / (w * 0.75f)))) b.cell(xx, y0 + total + k, darken(c1, 0.55f))
    }

    override fun draw(s: Scene) {
        val b = PixelBoard(s, 8f)
        val c1 = s.color(1)
        val w = 38
        val y0 = (b.rows * 0.275f).toInt()
        // A back wall: one neon strip and a few stars in the window of the dark.
        b.stars(s, 60, 0, (b.rows * 0.25f).toInt())
        cabinet(b, s, b.cols / 2 - w - 8, y0 + 6, w, 0.3f, false)
        cabinet(b, s, b.cols / 2 + w + 8, y0 + 6, w, 0.3f, false)
        cabinet(b, s, b.cols / 2, y0, w, 1f, true)
        for (gx in 0 until b.cols) b.cell(gx, y0 + 62, darken(c1, 0.6f), 130)
    }
}
