package com.nikopick.zamoled.gen

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.ColorUtils
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

const val TAU = (PI * 2).toFloat()

fun withAlpha(color: Int, alpha: Int): Int = ColorUtils.setAlphaComponent(color, alpha.coerceIn(0, 255))

private fun c(hex: Long): Int = hex.toInt()

class Palette(val id: String, val name: String, val colors: IntArray)

object Palettes {
    val all: List<Palette> = listOf(
        Palette("mono", "Mono", intArrayOf(c(0xFFFFFFFF), c(0xFFC9C9C9), c(0xFF8A8A8A))),
        Palette("cyan", "Neon Cyan", intArrayOf(c(0xFF00E5FF), c(0xFF18FFFF), c(0xFF2979FF))),
        Palette("sunset", "Sunset", intArrayOf(c(0xFFFF6E40), c(0xFFFFAB40), c(0xFFFF4081))),
        Palette("aurora", "Aurora", intArrayOf(c(0xFF00E676), c(0xFF1DE9B6), c(0xFF7C4DFF))),
        Palette("sakura", "Sakura", intArrayOf(c(0xFFFF80AB), c(0xFFF8BBD0), c(0xFFCE93D8))),
        Palette("lava", "Lava", intArrayOf(c(0xFFFF3D00), c(0xFFFF1744), c(0xFFFFC400))),
        Palette("ocean", "Ocean", intArrayOf(c(0xFF00B0FF), c(0xFF64FFDA), c(0xFF536DFE))),
        Palette("acid", "Acid", intArrayOf(c(0xFFC6FF00), c(0xFF76FF03), c(0xFFF4FF81))),
        Palette("violet", "Violet", intArrayOf(c(0xFFB388FF), c(0xFF7C4DFF), c(0xFFEA80FC))),
        Palette("gold", "Gold", intArrayOf(c(0xFFFFD740), c(0xFFFFC107), c(0xFFFFE0B2))),
    )

    /** Material You colours from the system theme; set by the app theme at startup. */
    @Volatile
    var you: Palette = Palette("you", "Material You", intArrayOf(c(0xFFA8C7FA), c(0xFFFFB0CB), c(0xFFC2E7FF)))

    val choices: List<Palette> get() = listOf(you) + all

    fun resolve(id: String, seed: Long): Palette = when (id) {
        "auto" -> all[Random(seed xor 0x5DEECE66DL).nextInt(all.size)]
        "you" -> you
        else -> all.firstOrNull { it.id == id } ?: all[0]
    }
}

enum class Category(val label: String) {
    DOODLE("Doodle"),
    MINIMAL("Minimal"),
    GEOMETRIC("Geometric"),
    SPACE("Space"),
    NATURE("Nature"),
    ABSTRACT("Abstract"),
    NEON("Neon"),
    AUDIO("Audio"),
    MYSTIC("Mystic"),
    CITY("City"),
    GLITCH("Glitch"),
    PIXEL("Pixel"),
    LIGHT("Light"),
}

data class WallpaperSpec(
    val styleId: String,
    val seed: Long,
    val paletteId: String = "auto",
    val density: Float = 1f,
) {
    val key: String get() = "$styleId|$seed|$paletteId|$density"
    val style: Style get() = Styles.byId(styleId)

    companion object {
        fun fromKey(key: String): WallpaperSpec? {
            val p = key.split('|')
            if (p.size != 4) return null
            val seed = p[1].toLongOrNull() ?: return null
            val density = p[3].toFloatOrNull() ?: return null
            return WallpaperSpec(p[0], seed, p[2], density)
        }
    }
}

/** Seeded 2D Perlin noise, output roughly in [-1, 1]. */
class Noise(seed: Long) {
    private val perm = IntArray(512)

    init {
        val p = (0 until 256).toMutableList()
        p.shuffle(Random(seed))
        for (i in 0 until 512) perm[i] = p[i and 255]
    }

    private fun fade(t: Float) = t * t * t * (t * (t * 6f - 15f) + 10f)
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun grad(h: Int, x: Float, y: Float): Float = when (h and 7) {
        0 -> x + y
        1 -> -x + y
        2 -> x - y
        3 -> -x - y
        4 -> x
        5 -> -x
        6 -> y
        else -> -y
    }

    fun at(x: Float, y: Float): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val xf = x - xi
        val yf = y - yi
        val X = xi and 255
        val Y = yi and 255
        val u = fade(xf)
        val v = fade(yf)
        val aa = perm[perm[X] + Y]
        val ab = perm[perm[X] + Y + 1]
        val ba = perm[perm[X + 1] + Y]
        val bb = perm[perm[X + 1] + Y + 1]
        return lerp(
            lerp(grad(aa, xf, yf), grad(ba, xf - 1f, yf), u),
            lerp(grad(ab, xf, yf - 1f), grad(bb, xf - 1f, yf - 1f), u),
            v,
        )
    }

    fun fbm(x: Float, y: Float, octaves: Int = 4): Float {
        var sum = 0f
        var amp = 0.5f
        var freq = 1f
        var norm = 0f
        repeat(octaves) {
            sum += at(x * freq, y * freq) * amp
            norm += amp
            amp *= 0.5f
            freq *= 2f
        }
        return sum / norm
    }
}

/**
 * Drawing context handed to every style. Coordinates are virtual: the canvas is always
 * 1000 units wide, so the same seed renders the same picture at thumbnail size and at 4K.
 */
class Scene(
    val canvas: Canvas,
    val w: Float,
    val h: Float,
    val rnd: Random,
    val palette: Palette,
    val density: Float,
    val noise: Noise,
) {
    val cx: Float get() = w / 2f
    val cy: Float get() = h / 2f

    fun count(base: Int): Int = max(1, (base * density).roundToInt())
    fun f(): Float = rnd.nextFloat()
    fun f(a: Float, b: Float): Float = a + rnd.nextFloat() * (b - a)
    fun i(a: Int, bExclusive: Int): Int = rnd.nextInt(a, bExclusive)
    fun chance(p: Float): Boolean = rnd.nextFloat() < p

    /** Roughly gaussian in [-1, 1]. */
    fun gauss(): Float = (rnd.nextFloat() + rnd.nextFloat() + rnd.nextFloat() - 1.5f) / 1.5f

    fun color(index: Int): Int {
        val n = palette.colors.size
        return palette.colors[((index % n) + n) % n]
    }

    fun anyColor(): Int = palette.colors[rnd.nextInt(palette.colors.size)]

    /** Smooth blend across the whole palette, t in [0, 1]. */
    fun grad(t: Float): Int {
        val cols = palette.colors
        if (cols.size == 1) return cols[0]
        val x = t.coerceIn(0f, 1f) * (cols.size - 1)
        val k = min(x.toInt(), cols.size - 2)
        return ColorUtils.blendARGB(cols[k], cols[k + 1], x - k)
    }

    fun stroke(color: Int, width: Float, alpha: Int = 255, glow: Float = 0f): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.alpha = alpha.coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            if (glow > 0f) maskFilter = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
        }

    fun fill(color: Int, alpha: Int = 255, glow: Float = 0f): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.alpha = alpha.coerceIn(0, 255)
            style = Paint.Style.FILL
            if (glow > 0f) maskFilter = BlurMaskFilter(glow, BlurMaskFilter.Blur.NORMAL)
        }

    fun glowPath(path: Path, color: Int, width: Float, glow: Float = width * 4f, alpha: Int = 255) {
        canvas.drawPath(path, stroke(color, width * 2.2f, (alpha * 0.6f).toInt(), glow))
        canvas.drawPath(path, stroke(color, width, alpha))
    }

    fun glowDot(x: Float, y: Float, r: Float, color: Int, glow: Float = r * 3f, alpha: Int = 255) {
        canvas.drawCircle(x, y, r * 2.2f, fill(color, (alpha * 0.5f).toInt(), glow))
        canvas.drawCircle(x, y, r, fill(color, alpha))
    }

    fun dot(x: Float, y: Float, r: Float, color: Int, alpha: Int = 255) {
        canvas.drawCircle(x, y, r, fill(color, alpha))
    }

    fun faintStars(n: Int, maxY: Float = h, alphaMax: Int = 150) {
        repeat(count(n)) {
            val r = 0.4f + f() * f() * 1.4f
            dot(f(0f, w), f(0f, maxY), r, Color.WHITE, i(25, alphaMax))
        }
    }
}

abstract class Style(val id: String, val name: String, val category: Category) {
    abstract fun draw(s: Scene)
}

object Renderer {
    const val VIRTUAL_WIDTH = 1000f

    fun render(spec: WallpaperSpec, widthPx: Int, heightPx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.BLACK)
        val scale = widthPx / VIRTUAL_WIDTH
        canvas.scale(scale, scale)
        val scene = Scene(
            canvas = canvas,
            w = VIRTUAL_WIDTH,
            h = heightPx / scale,
            rnd = Random(spec.seed),
            palette = Palettes.resolve(spec.paletteId, spec.seed),
            density = spec.density,
            noise = Noise(spec.seed),
        )
        spec.style.draw(scene)
        return bmp
    }

    /** Share of pixels that are true black (every channel <= 3) — what an OLED panel switches off. */
    fun blackRatio(bmp: Bitmap): Float {
        val w = bmp.width
        val h = bmp.height
        val row = IntArray(w)
        var black = 0L
        for (y in 0 until h) {
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            for (p in row) {
                if ((p shr 16 and 0xFF) <= 3 && (p shr 8 and 0xFF) <= 3 && (p and 0xFF) <= 3) black++
            }
        }
        return black.toFloat() / (w.toLong() * h)
    }
}

object Styles {
    val all: List<Style> = listOf(
        DoodleScatter, DoodlePattern, DoodleSpotlight,
        Halo, Horizon, Silk,
        PolygonTunnel, DotMatrix, Isometric,
        Starfield, Constellations, Eclipse,
        Aurora, Topographic, Mountains,
        FlowField, Waves, Mandala,
        NeonShapes, Circuit, Synthwave,
        SpectrumBars, CircularSpectrum, Oscilloscope,
        FlowerOfLife, Metatron, MoonPhases,
        Skyline, NeonRain, NightHighway,
        GlitchBlocks, DataRain, PixelDrip,
        PixelSpace, PixelHearts, PixelLandscape,
        Bokeh, Fireflies, LightTrails,
    )

    fun byId(id: String): Style = all.firstOrNull { it.id == id } ?: all[0]
    fun of(category: Category): List<Style> = all.filter { it.category == category }
}
