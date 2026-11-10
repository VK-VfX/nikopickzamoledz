package com.nikopick.zamoled.gen

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
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
        Palette("candy", "Candy", intArrayOf(c(0xFFF47C7C), c(0xFFFFC97A), c(0xFF7FD8A8), c(0xFFC98BD6), c(0xFF9ED9E0))),
        Palette("graffiti", "Graffiti", intArrayOf(c(0xFFFF5E7E), c(0xFFFFA552), c(0xFF3EE6C1), c(0xFF4C8DFF), c(0xFFB36BFF))),
        Palette("hue", "Hue Shift", intArrayOf(c(0xFF00E5A0), c(0xFF2D7BFF), c(0xFF8C5CFF), c(0xFFFF3D7F))),
        Palette("cyber", "Cyberpunk", intArrayOf(c(0xFF00E5FF), c(0xFFFF2BD6), c(0xFFFFB300), c(0xFF7C4DFF))),
    )

    /** Material You colours from the system theme; set by the app theme at startup. */
    @Volatile
    var you: Palette = Palette("you", "Material You", intArrayOf(c(0xFFA8C7FA), c(0xFFFFB0CB), c(0xFFC2E7FF)))

    val choices: List<Palette> get() = listOf(you) + all

    fun byId(id: String): Palette = all.firstOrNull { it.id == id } ?: all[0]

    fun resolve(id: String, seed: Long, style: Style? = null): Palette = when (id) {
        "auto" -> {
            val r = Random(seed xor 0x5DEECE66DL)
            val preferred = style?.autoPalettes
            if (preferred.isNullOrEmpty()) all[r.nextInt(all.size)]
            else preferred[r.nextInt(preferred.size)].let { if (it == "you") you else byId(it) }
        }
        "you" -> you
        else -> byId(id)
    }
}

enum class Category(val label: String) {
    DOODLE("Doodle"),
    LAB("Lab"),
    WORDS("Words"),
    MATERIAL("For Pixel"),
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
    PIXEL("Pixel Bits"),
    LIGHT("Light"),
    GOTHIC("Gothic"),
}

data class WallpaperSpec(
    val styleId: String,
    val seed: Long,
    val paletteId: String = "auto",
    val density: Float = 1f,
    val layoutId: String = "auto",
    /** Black level, 0..1: how hard dim glow and haze are crushed to pure #000000 after drawing. */
    val black: Float = 0f,
    /** The words to set, for styles that draw text. Empty means the style picks its own. */
    val text: String = "",
    /** Widget-safe: the picture is laid out below the launcher clock and weather widgets. */
    val safe: Boolean = false,
    /** Words styles: highlight the key word in white instead of red. */
    val white: Boolean = false,
) {
    val key: String get() =
        "$styleId|$seed|$paletteId|$density|$layoutId|$black|${text.replace('|', ' ').replace('\n', ' ')}|${(if (safe) "s" else "") + (if (white) "w" else "")}"
    val style: Style get() = Styles.byId(styleId)
    val palette: Palette get() = Palettes.resolve(paletteId, seed, style)
    val layout: DoodleLayout get() = DoodleLayout.resolve(layoutId, seed)

    companion object {
        fun fromKey(key: String): WallpaperSpec? {
            // Four fields is the oldest format, five added arrangements, seven added black level and text,
            // eight added the option letters (s = widget-safe, w = white highlight).
            val p = key.split('|', limit = 8)
            if (p.size != 4 && p.size != 5 && p.size != 7 && p.size != 8) return null
            val seed = p[1].toLongOrNull() ?: return null
            val density = p[3].toFloatOrNull() ?: return null
            // Favourites saved before arrangements existed used the jittered grid.
            val layout = if (p.size >= 5) p[4] else DoodleLayout.GRID.id
            val black = if (p.size >= 7) p[5].toFloatOrNull() ?: return null else 0f
            val opts = if (p.size == 8) p[7] else ""
            return WallpaperSpec(p[0], seed, p[2], density, layout, black, if (p.size >= 7) p[6] else "", 's' in opts, 'w' in opts)
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
    /** Arrangement for styles that lay doodles out across the page. */
    val layout: DoodleLayout = DoodleLayout.SCATTER,
    /** Custom words for styles that draw text; empty when the style should choose. */
    val text: String = "",
    /** Words styles: the highlighted word is white (the rest dimmed) instead of red. */
    val whiteWords: Boolean = false,
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
    /** Palettes "Auto" picks from for this style; null means any palette. */
    open val autoPalettes: List<String>? = null

    /** False for styles the Detail slider does nothing to. */
    open val usesDetail: Boolean get() = true

    /** True for styles that set the user's own words. */
    open val usesText: Boolean get() = false

    /** False for styles whose colours are fixed, so the Palette row does nothing to them. */
    open val usesPalette: Boolean get() = true

    /** True for styles already composed around the launcher widgets, so Widget-safe has nothing to add. */
    open val widgetSafe: Boolean get() = false
    abstract fun draw(s: Scene)
}

object Renderer {
    const val VIRTUAL_WIDTH = 1000f

    /** Share of the screen height kept clear for the launcher clock and weather widgets. */
    const val SAFE_TOP = 0.2f

    fun render(spec: WallpaperSpec, widthPx: Int, heightPx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.BLACK)
        val scale = widthPx / VIRTUAL_WIDTH
        // Widget-safe: the picture is composed on the lower part of the screen and fades in under the widgets.
        val shift = spec.safe && !spec.style.widgetSafe
        val top = if (shift) (heightPx * SAFE_TOP).toInt() else 0
        val artPx = heightPx - top
        val layer = if (shift) canvas.saveLayer(0f, top.toFloat(), widthPx.toFloat(), heightPx.toFloat(), null) else -1
        canvas.save()
        canvas.translate(0f, top.toFloat())
        canvas.scale(scale, scale)
        val scene = Scene(
            canvas = canvas,
            w = VIRTUAL_WIDTH,
            h = artPx / scale,
            rnd = Random(spec.seed),
            palette = spec.palette,
            density = spec.density,
            noise = Noise(spec.seed),
            layout = spec.layout,
            text = spec.text,
            whiteWords = spec.white,
        )
        spec.style.draw(scene)
        canvas.restore()
        if (shift) {
            val feather = artPx * 0.12f
            val fade = Paint().apply {
                shader = LinearGradient(0f, top.toFloat(), 0f, top + feather, 0x00000000, -0x1000000, Shader.TileMode.CLAMP)
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.drawRect(0f, top.toFloat(), widthPx.toFloat(), top + feather, fade)
            canvas.restoreToCount(layer)
        }
        if (spec.black > 0.01f) crush(bmp, spec.black)
        return bmp
    }

    /**
     * Pushes dim pixels to pure black. Anything darker than the threshold switches off completely,
     * and the band just above it fades in, so glows end in a soft edge instead of a hard cut.
     */
    fun crush(bmp: Bitmap, level: Float) {
        // Squared, so the first half of the slider only trims faint haze and the top end is ruthless.
        val l = level.coerceIn(0f, 1f)
        val t = (l * l * 64f).toInt().coerceAtLeast(1)
        val knee = t + t / 2 + 8
        val w = bmp.width
        val row = IntArray(w)
        for (y in 0 until bmp.height) {
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            var changed = false
            for (x in 0 until w) {
                val p = row[x]
                val r = p shr 16 and 0xFF
                val g = p shr 8 and 0xFF
                val b = p and 0xFF
                val m = if (r > g) (if (r > b) r else b) else (if (g > b) g else b)
                if (m == 0 || m >= knee) continue
                changed = true
                if (m <= t) {
                    row[x] = -0x1000000
                } else {
                    val f = (m - t).toFloat() / (knee - t)
                    val k = f * f * (3f - 2f * f)
                    row[x] = -0x1000000 or ((r * k).toInt() shl 16) or ((g * k).toInt() shl 8) or (b * k).toInt()
                }
            }
            if (changed) bmp.setPixels(row, 0, w, 0, y, w, 1)
        }
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
    /** Styles shown in the app, grouped by world. */
    val all: List<Style> = listOf(
        StickerBomb, Gouache, Chalk, SpaceDoodles, Gatti, GattiSketch,
        M3Bloom, M3Cookies, M3Waves, M3Pills, M3Arches, M3Blobs, M3Ribbons, M3Components, M3Aura, M3Overlap,
        LoneTree, KoiPond, MoonFisher, LineConstellation, OneLine, Contours, Orbits, Glance,
        CrystalCave, ArtDecoTower, FloatingIsle,
        AstronautDrift, RingedWorld, BlackHole, Eclipse,
        StarrySwirl, LiquidMarble, InkSmoke,
        NeonSign, Synthwave,
        Rooftops, Skyline, NightHighway,
        DataRain,
        PixelCastle, PixelQuest, PixelSunset, PixelPlanet, PixelTrain, PixelLighthouse, PixelArcade,
        LanternFestival, Candles, Jellyfish, Fireflies,
    ) + NatureWorld.styles + NeonWorld.styles + GlitchWorld.styles + MysticWorld.styles + LabWorld.styles + WordsWorld.styles

    /** Retired styles: hidden from browsing but still able to open saved favourites. */
    private val retired: List<Style> = listOf(
        Halo, Horizon, Silk, PolygonTunnel, DotMatrix, Isometric, Starfield, Constellations,
        Topographic, Mountains, FlowField, Waves, Mandala, NeonShapes, Circuit,
        SpectrumBars, CircularSpectrum, Oscilloscope, FlowerOfLife, Metatron,
        GlitchBlocks, PixelDrip, PixelHearts, Bokeh, LightTrails, NeonRain,
        GothMoonQueen, GothRavenLady, GothWanderer, Vinyl, Boombox, StudioMic,
        DoodleScatter, DoodlePattern, DoodleSpotlight, DoodleStickers,
        Aurora, MoonlitForest, MountainLake, NightBloom, NeonAlley, GlitchBust, ErrorWindows,
        TarotMoon, AllSeeingEye, CrystalBall, MoonPhases,
        PixelSpace, PixelCity, PixelLandscape,
    )

    /** Retired ids that now open as their replacement. */
    private val aliases = mapOf("neon_dystopia" to "wet_street")

    /**
     * Sub-categories. Styles that share a label are browsed together; a style that is not
     * listed here is its own sub-category, under its own name. Used by the chips that
     * appear under a category so the feed can be narrowed without endless scrolling.
     */
    private val subcategories: Map<String, String> = mapOf(
        // Doodle
        "sticker_bomb" to "Sticker", "gouache" to "Painted", "chalk" to "Chalk",
        // Minimal
        "lone_tree" to "Ink", "koi_pond" to "Ink", "moon_fisher" to "Ink",
        "line_constellation" to "Lines", "one_line" to "Lines", "contours" to "Lines", "orbits" to "Lines",
        "glance" to "Always-on",
        // Nature
        "aurora_night" to "Sky", "starry_pines" to "Sky", "moon_path" to "Water", "moonlit_peaks" to "Land",
        // Neon
        "neon_tunnel" to "Tubes", "neon_ribbons" to "Tubes", "neon_storm" to "Electric", "neon_sign" to "Signs", "synthwave" to "Retro",
        // Pixel Bits
        "pixel_castle" to "Games", "pixel_quest" to "Games", "pixel_sunset" to "Scenes", "pixel_planet" to "Scenes",
        "pixel_train" to "Scenes", "pixel_lighthouse" to "Scenes", "pixel_arcade" to "Games",
        // Glitch
        "data_rain" to "Signal", "wet_street" to "Cyberpunk", "source_code" to "Code", "code_diff" to "Code", "code_panic" to "Code", "code_hex" to "Code", "code_fall" to "Code",
        "code_poster" to "Code", "code_corrupt" to "Code", "code_build" to "Code",
        // Mystic
        "arcane_circle" to "Occult", "zodiac_wheel" to "Stars", "lunar_cycle" to "Stars", "tarot_card" to "Cards", "crystal_cluster" to "Crystals",
        // For Pixel
        "m3_bloom" to "Shapes", "m3_cookies" to "Shapes", "m3_blobs" to "Shapes",
        "m3_waves" to "Flow", "m3_ribbons" to "Flow",
        "m3_pills" to "Pattern", "m3_arches" to "Pattern",
        "m3_components" to "Interface", "m3_aura" to "Glow", "m3_overlap" to "Glow",
        "gatti" to "Cats", "gatti_sketch" to "Cats",
        "space_doodles" to "Space",
    )

    fun byId(id: String): Style {
        val key = aliases[id] ?: id
        return all.firstOrNull { it.id == key } ?: retired.firstOrNull { it.id == key } ?: all[0]
    }
    fun of(category: Category): List<Style> = all.filter { it.category == category }

    /** The sub-category a style is browsed under. */
    fun subcategoryOf(style: Style): String = subcategories[style.id] ?: style.name

    /** The sub-categories within a category, in the order their styles are registered. */
    fun subcategories(category: Category): List<String> = of(category).map(::subcategoryOf).distinct()

    /** Styles in a category, narrowed to one sub-category when [subcategory] is given. */
    fun of(category: Category, subcategory: String?): List<Style> =
        of(category).filter { subcategory == null || subcategoryOf(it) == subcategory }

    /** Categories that currently have at least one style to show. */
    val categories: List<Category> get() = Category.entries.filter { c -> all.any { it.category == c } }
}
