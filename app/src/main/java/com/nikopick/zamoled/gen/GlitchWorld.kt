package com.nikopick.zamoled.gen

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.content.res.AssetManager
import java.util.regex.Pattern
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** The Glitch world: falling signal, a rain-soaked cyberpunk street and the source code that draws it all. */

/** Draws into a transparent layer the same size as the picture, so it can be torn up afterwards. */
private inline fun Scene.layer(draw: (Scene) -> Unit): Bitmap {
    val k = canvas.width / w
    val bmp = Bitmap.createBitmap(canvas.width, (h * k).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.scale(k, k)
    draw(Scene(c, w, h, rnd, palette, density, noise, layout))
    return bmp
}

private val RED = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
private val GREEN = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
private val BLUE = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)

/**
 * Lays [bmp] on the page torn into horizontal strips, some shifted sideways, with the red, green
 * and blue channels pulled apart by [split] units: the look of a signal failing.
 */
private fun Scene.glitchComposite(bmp: Bitmap, split: Float, tears: Int, maxShift: Float) {
    val paints = listOf(RED, GREEN, BLUE).map { m ->
        Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(m))
            blendMode = BlendMode.PLUS
        }
    }
    val sx = bmp.width / w
    val sy = bmp.height / h
    var y = 0f
    val tearAt = BooleanArray(1000)
    repeat(tears) { tearAt[i(0, 1000)] = true }
    while (y < h) {
        val band = if (chance(0.5f)) f(h * 0.004f, h * 0.02f) else f(h * 0.03f, h * 0.09f)
        val y1 = min(h, y + band)
        val shift = if (tearAt[(y / h * 999f).toInt().coerceIn(0, 999)] || chance(tears / 90f)) f(-maxShift, maxShift) else 0f
        val src = Rect(0, (y * sy).toInt(), bmp.width, (y1 * sy).toInt().coerceAtLeast((y * sy).toInt() + 1))
        for ((k, p) in paints.withIndex()) {
            val d = shift + (k - 1) * split
            canvas.drawBitmap(bmp, src, RectF(d, y, w + d, y1), p)
        }
        y = y1
    }
    bmp.recycle()
}

// ---------------------------------------------------------------------------------------------

/** Characters falling in columns. Six looks, and Detail packs more of them onto the screen. */
object DataRain : Style("data_rain", "Data Rain", Category.GLITCH) {
    override val autoPalettes = listOf("aurora", "acid", "cyan", "violet", "lava", "mono", "graffiti")
    private const val KANA = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロ0123456789"

    /** One field of falling columns. [activity] is the chance a column is raining at density 1. */
    private fun Scene.columns(
        glyphs: String, size: Float, activity: Float, lenMin: Int, lenMax: Int,
        tint: (Float) -> Int, alphaScale: Float = 1f, sway: Float = 0f, glow: Boolean = true,
    ) {
        // More detail means smaller type, more columns, and longer tails.
        val sz = size / (0.75f + 0.25f * density.coerceIn(0.4f, 1.8f))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sz
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val chance = (activity * Math.pow(density.toDouble(), 0.85).toFloat()).coerceIn(0.05f, 0.98f)
        var x = sz / 2f
        while (x < w) {
            if (chance(chance)) {
                val len = (i(lenMin, lenMax) * (0.65f + 0.45f * density)).toInt().coerceAtLeast(4)
                val headY = f(0f, h + len * sz)
                val color = tint(x / w)
                val phase = f(0f, (2 * PI).toFloat())
                for (k in 0 until len) {
                    val y = headY - k * sz
                    if (y < -sz || y > h + sz) continue
                    val ch = glyphs[i(0, glyphs.length)].toString()
                    val px = x + sin(y * 0.01f + phase) * sway
                    if (k == 0) {
                        paint.color = Color.WHITE
                        paint.alpha = (255 * alphaScale).toInt()
                        if (glow) paint.setShadowLayer(sz * 0.5f, 0f, 0f, color) else paint.clearShadowLayer()
                    } else {
                        paint.color = if (k < 3) lighten(color, 0.4f) else color
                        paint.alpha = ((230 * (1f - k / len.toFloat())).toInt().coerceAtLeast(18) * alphaScale).toInt()
                        paint.clearShadowLayer()
                    }
                    canvas.drawText(ch, px, y, paint)
                }
            }
            x += sz * 1.08f
        }
    }

    override fun draw(s: Scene) {
        val c0 = s.color(0)
        val c1 = s.color(1)
        when (s.i(0, 6)) {
            // The classic: kana and digits, white heads, a tail that fades.
            0 -> s.columns(KANA, 26f, 0.7f, 10, 40, { s.grad(it) })
            // Binary: two characters, one tint, long unbroken streams.
            1 -> s.columns("01", 30f, 0.65f, 16, 52, { c0 })
            // Hex: the digits of a dump, falling.
            2 -> s.columns("0123456789ABCDEF", 24f, 0.7f, 10, 36, { lighten(s.grad(it), 0.1f) })
            // Depth: three planes, small and dim at the back, large and bright in front.
            3 -> {
                s.columns(KANA, 14f, 0.8f, 12, 46, { darken(c0, 0.35f) }, 0.55f, glow = false)
                s.columns(KANA, 26f, 0.5f, 10, 34, { s.grad(it) }, 0.9f)
                s.columns("01", 54f, 0.22f, 5, 16, { lighten(c1, 0.2f) }, 1f)
            }
            // Pixel snow: square flakes falling in streaks, torn sideways in places.
            4 -> {
                val n = s.count(260)
                repeat(n) {
                    val x = s.f(0f, s.w)
                    val y = s.f(0f, s.h)
                    val sz = s.f(3f, 13f)
                    val col = s.grad(s.f())
                    val streak = s.f(20f, 160f)
                    s.canvas.drawRect(x, y - streak, x + sz, y, s.fill(col, s.i(15, 60)))
                    s.canvas.drawRect(x, y, x + sz, y + sz, s.fill(if (s.chance(0.2f)) Color.WHITE else col, s.i(160, 255)))
                }
                repeat(s.count(7)) {
                    val y = s.f(0f, s.h)
                    s.canvas.drawRect(s.f(0f, s.w * 0.4f), y, s.f(s.w * 0.5f, s.w), y + s.f(2f, 9f), s.fill(s.grad(s.f()), s.i(40, 110)))
                }
            }
            // Cascade: the columns lean and sway, the tail running from bright to the second colour.
            else -> s.columns(KANA, 24f, 0.65f, 12, 42, { mix(c0, c1, it) }, sway = 16f)
        }
        // Past normal detail, another plane of small, dim characters falls behind the rest.
        if (s.density > 1.1f) s.columns(KANA, 13f, (s.density - 1f) * 0.9f, 10, 40, { darken(s.grad(it), 0.4f) }, 0.5f, glow = false)
    }
}

// ---------------------------------------------------------------------------------------------

/**
 * A wet cyberpunk street in one-point perspective. Towers close in from both sides toward a haze at the
 * vanishing point, their edges lit in neon, vertical signs and windows in true perspective, a black road
 * that mirrors every light, a lone figure under an umbrella and a hologram ad overhead. Glitch is rare
 * and local: a couple of torn bands and a slight colour split, like a feed that is nearly stable.
 */
object WetStreet : Style("wet_street", "Wet Street", Category.GLITCH) {
    override val autoPalettes = listOf("cyber", "cyber", "cyber", "hue", "graffiti", "sunset", "violet")

    private const val F = 120f       // focal length: one world unit at depth 1 is this many canvas units
    private const val CAM = 1.6f     // camera height above the road, in world units
    private const val LAT = 5f       // lateral distance to the building faces

    private val SIGNS = listOf("ラーメン", "ホテル", "酒場", "電脳", "薬局", "夜市", "未来", "BAR", "NOODLE", "24H", "OPEN", "病院", "NEON", "HOTEL", "MOTEL")
    private val ADS = listOf("NEXUS", "VOLT", "HELIX", "ONYX", "LUMEN", "AXIOM", "KAIJU")

    private class Cam(val vx: Float, val vy: Float) {
        fun x(lat: Float, z: Float) = vx + lat / z * F
        fun y(height: Float, z: Float) = vy + (CAM - height) / z * F
    }

    private class Tower(val side: Int, val z0: Float, val z1: Float, val lat: Float, val ht: Float, val color: Int, val empty: Boolean)

    override fun draw(s: Scene) {
        val bmp = s.layer { street(it) }
        s.glitchComposite(bmp, split = 2.4f, tears = 1 + (s.density * 2f).toInt(), maxShift = 30f)
    }

    private fun fade(h: Float, y: Float): Float {
        // Detail dims toward the top of the screen, where the launcher puts its widgets.
        val t = ((y - h * 0.1f) / (h * 0.2f)).coerceIn(0f, 1f)
        return 0.25f + 0.75f * t * t * (3f - 2f * t)
    }

    private fun street(s: Scene) {
        val cam = Cam(s.cx + s.f(-26f, 26f), s.h * s.f(0.43f, 0.46f))
        val warm = s.color(2)
        val cL = s.color(0)
        val cR = s.color(1)
        val canvas = s.canvas

        // Haze at the end of the street: the one big soft light, and what the figure stands against.
        val haze = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cam.vx, cam.vy + 30f, 360f, intArrayOf(withAlpha(mix(cL, cR, 0.5f), 120), withAlpha(cR, 40), 0), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cam.vx, cam.vy + 30f, 360f, haze)
        canvas.drawCircle(cam.vx, cam.vy + 20f, 54f, s.fill(Color.WHITE, 85, 46f))

        // The two walls of towers, laid out from the camera outward.
        val towers = ArrayList<Tower>()
        for (side in intArrayOf(-1, 1)) {
            var z = 0.12f
            while (z < 90f) {
                val z1 = z + z * s.f(0.22f, 0.5f) + 0.05f
                towers += Tower(side, z, z1, LAT + s.f(0f, 1.1f), s.f(9f, 44f), if (side < 0) cL else cR, s.chance(0.1f))
                z = z1 + (if (s.chance(0.15f)) z * 0.1f else 0f)
            }
        }
        towers.sortByDescending { it.z0 }

        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(5, 6, 10) }
        val path = Path()
        val winPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val signs = ArrayList<Triple<Tower, Float, Float>>()      // tower, z centre, height

        for (t in towers) {
            if (t.empty) continue
            val lat = t.side * t.lat
            val xa = cam.x(lat, t.z0); val xb = cam.x(lat, t.z1)
            val ga = cam.y(0f, t.z0); val gb = cam.y(0f, t.z1)
            val ta = cam.y(t.ht, t.z0); val tb = cam.y(t.ht, t.z1)
            path.reset()
            path.moveTo(xa, ga); path.lineTo(xb, gb); path.lineTo(xb, tb); path.lineTo(xa, ta); path.close()
            canvas.drawPath(path, body)
            // Light from the street climbs the face a little way.
            val rise = (ga - ta).coerceAtMost(900f)
            val wash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, ga, 0f, ga - rise, withAlpha(t.color, 64), 0, Shader.TileMode.CLAMP)
            }
            canvas.drawPath(path, wash)

            // Windows, in perspective.
            val pitch = maxOf(0.38f, (t.z1 - t.z0) / 6f)
            val lit = 0.2f + 0.2f * s.density.coerceAtMost(1.6f)
            val fog = (1f - t.z0 / 100f).coerceIn(0.2f, 1f)
            var hg = 1.3f
            while (hg < t.ht - 0.9f) {
                var zc = t.z0 + pitch * 0.2f
                while (zc + pitch * 0.6f < t.z1) {
                    val cy = cam.y(hg + 0.3f, zc)
                    val cx = cam.x(lat, zc)
                    if (cx > -60f && cx < s.w + 60f && cy > -60f && cy < s.h + 60f && s.chance(lit)) {
                        val zw = zc + pitch * 0.6f
                        path.reset()
                        path.moveTo(cam.x(lat, zc), cam.y(hg, zc)); path.lineTo(cam.x(lat, zw), cam.y(hg, zw))
                        path.lineTo(cam.x(lat, zw), cam.y(hg + 0.6f, zw)); path.lineTo(cam.x(lat, zc), cam.y(hg + 0.6f, zc)); path.close()
                        val tint = when (s.i(0, 10)) { in 0..4 -> warm; in 5..7 -> t.color; else -> Color.rgb(150, 205, 255) }
                        winPaint.color = withAlpha(tint, (215 * fog * fade(s.h, cy)).toInt())
                        canvas.drawPath(path, winPaint)
                    }
                    zc += pitch
                }
                hg += 1.1f
            }

            // A neon tube along the face on some towers.
            if (s.chance(0.4f)) {
                val hg2 = s.f(3f, minOf(t.ht - 1f, 20f))
                val line = Path().apply { moveTo(cam.x(lat, t.z0), cam.y(hg2, t.z0)); lineTo(cam.x(lat, t.z1), cam.y(hg2, t.z1)) }
                s.glowPath(line, withAlpha(lighten(t.color, 0.3f), 255), 1.6f + 3f / (t.z0 + 0.5f), alpha = (230 * fade(s.h, cam.y(hg2, t.z0))).toInt())
            }

            // Lit edges: the corner nearest the camera and the roofline.
            val edge = 1.4f + 3.2f / (t.z0 + 0.6f)
            val alpha = (230 * fade(s.h, ta.coerceAtLeast(s.h * 0.3f))).toInt()
            if (xa in -300f..s.w + 300f) {
                val v = Path().apply { moveTo(xa, ga); lineTo(xa, ta) }
                canvas.drawPath(v, s.stroke(t.color, edge * 3.2f, 50, edge * 3f))
                canvas.drawPath(v, s.stroke(lighten(t.color, 0.35f), edge, alpha))
            }
            val roof = Path().apply { moveTo(xa, ta); lineTo(xb, tb) }
            canvas.drawPath(roof, s.stroke(t.color, edge * 2.4f, 40, edge * 2.4f))
            canvas.drawPath(roof, s.stroke(lighten(t.color, 0.25f), edge * 0.8f, (alpha * 0.8f).toInt()))

            if (t.z0 > 0.5f && t.z0 < 14f && s.chance(0.6f)) {
                signs += Triple(t, s.f(t.z0 + (t.z1 - t.z0) * 0.25f, t.z0 + (t.z1 - t.z0) * 0.55f), s.f(2.6f, minOf(t.ht - 6f, 15f).coerceAtLeast(3f)))
            }
        }

        // Vertical signs on the faces, drawn through a perspective matrix so their text leans with the wall.
        val reflections = ArrayList<FloatArray>()
        for ((t, zc, h0) in signs) {
            val lat = t.side * (t.lat - 0.45f)
            val sh = s.f(3f, 6.5f)
            val zw = (t.z1 - t.z0) * 0.22f
            val zL = if (t.side < 0) zc else zc + zw
            val zR = if (t.side < 0) zc + zw else zc
            val tl = floatArrayOf(cam.x(lat, zL), cam.y(h0 + sh, zL))
            val tr = floatArrayOf(cam.x(lat, zR), cam.y(h0 + sh, zR))
            val br = floatArrayOf(cam.x(lat, zR), cam.y(h0, zR))
            val bl = floatArrayOf(cam.x(lat, zL), cam.y(h0, zL))
            val cx = (tl[0] + br[0]) / 2f; val cy = (tl[1] + br[1]) / 2f
            if (cx < -80f || cx > s.w + 80f || cy < -80f || cy > s.h + 80f) continue
            val word = SIGNS[s.i(0, SIGNS.size)]
            val col = s.color(s.i(0, 4))
            val fa = fade(s.h, cy)
            val quad = Path().apply { moveTo(tl[0], tl[1]); lineTo(tr[0], tr[1]); lineTo(br[0], br[1]); lineTo(bl[0], bl[1]); close() }
            canvas.drawPath(quad, s.fill(col, (95 * fa).toInt(), 14f))
            canvas.drawPath(quad, s.fill(Color.rgb(4, 4, 8), 230))
            val tw = 70f
            val cell = tw * 0.95f
            val th = cell * word.length + 16f
            val m = Matrix()
            m.setPolyToPoly(floatArrayOf(0f, 0f, tw, 0f, tw, th, 0f, th), 0, floatArrayOf(tl[0], tl[1], tr[0], tr[1], br[0], br[1], bl[0], bl[1]), 0, 4)
            canvas.save()
            canvas.concat(m)
            canvas.drawRect(3f, 3f, tw - 3f, th - 3f, s.stroke(lighten(col, 0.3f), 3.2f, (235 * fa).toInt()))
            val tp = s.textPaint(tw * 0.7f, lighten(col, 0.6f), Typeface.DEFAULT_BOLD)
            tp.alpha = (255 * fa).toInt()
            for ((k, ch) in word.withIndex()) canvas.drawText(ch.toString(), tw / 2f, 8f + cell * (k + 0.78f), tp)
            canvas.restore()
            reflections += floatArrayOf(tl[0], tr[0], cam.y(-h0, zL), cam.y(-(h0 + sh), zL), cam.y(-h0, zR), cam.y(-(h0 + sh), zR), col.toFloat(), fa)
        }

        // The road: lane dashes, then everything above it mirrored and broken up by ripples.
        var zd = 0.4f
        while (zd < 60f) {
            val zn = zd * 1.22f
            val dash = Path().apply {
                moveTo(cam.x(-0.07f, zd), cam.y(0f, zd)); lineTo(cam.x(0.07f, zd), cam.y(0f, zd))
                lineTo(cam.x(0.07f, zn), cam.y(0f, zn)); lineTo(cam.x(-0.07f, zn), cam.y(0f, zn)); close()
            }
            canvas.drawPath(dash, s.fill(Color.WHITE, (70 / (1f + zd * 0.15f)).toInt()))
            zd = zn * 1.18f
        }
        for (t in towers) {
            if (t.empty || t.z0 < 0.3f) continue
            val lat = t.side * t.lat
            val rh = minOf(t.ht, 7f)
            val q = Path().apply {
                moveTo(cam.x(lat, t.z0), cam.y(0f, t.z0)); lineTo(cam.x(lat, t.z1), cam.y(0f, t.z1))
                lineTo(cam.x(lat, t.z1), cam.y(-rh, t.z1)); lineTo(cam.x(lat, t.z0), cam.y(-rh, t.z0)); close()
            }
            val y0 = cam.y(0f, t.z0); val y1 = cam.y(-rh, t.z0)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, y0, 0f, y1, withAlpha(t.color, 70), 0, Shader.TileMode.CLAMP)
                maskFilter = android.graphics.BlurMaskFilter(7f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawPath(q, p)
        }
        for (r in reflections) {
            val col = r[6].toInt()
            val q = Path().apply { moveTo(r[0], r[2]); lineTo(r[1], r[4]); lineTo(r[1], r[5]); lineTo(r[0], r[3]); close() }
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, r[2], 0f, r[3], withAlpha(col, (120 * r[7]).toInt()), 0, Shader.TileMode.CLAMP)
                maskFilter = android.graphics.BlurMaskFilter(9f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawPath(q, p)
            // Ripples cut dark lines through the mirror.
            repeat(7) {
                val yy = s.f(minOf(r[2], r[3]), maxOf(r[2], r[3]))
                val x0 = s.f(minOf(r[0], r[1]) - 14f, maxOf(r[0], r[1]))
                canvas.drawLine(x0, yy, x0 + s.f(20f, 70f), yy, s.stroke(Color.BLACK, s.f(1.5f, 3.5f), 210))
            }
        }

        // Cars far down the street: tail lights going away, headlights coming.
        repeat(s.count(4)) {
            val z = s.f(5f, 22f)
            val lane = if (s.chance(0.5f)) 1.5f else -1.5f
            val red = lane > 0f
            val c = if (red) Color.rgb(255, 40, 70) else Color.rgb(215, 238, 255)
            for (d in floatArrayOf(-0.62f, 0.62f)) {
                val x = cam.x(lane + d, z); val y = cam.y(0.7f, z); val r = 0.13f * F / z
                s.glowDot(x, y, r.coerceAtLeast(1.2f), c, r * 5f)
                canvas.drawLine(x, cam.y(-0.7f, z), x, cam.y(-0.7f, z) + r * 14f, s.stroke(c, r * 1.6f, 90, r * 3f))
            }
        }

        // A hologram ad hanging over the street.
        run {
            val z = s.f(3.1f, 4.2f)
            val l = cam.x(-2.9f, z); val r = cam.x(2.9f, z)
            val top = cam.y(8.6f, z); val bot = cam.y(5.4f, z)
            val col = s.color(s.i(0, 3))
            val panel = RectF(l, top, r, bot)
            canvas.drawRect(panel, s.fill(col, 30, 18f))
            canvas.drawRect(panel, s.stroke(lighten(col, 0.3f), 1.8f, 170))
            var yy = top + 3f
            while (yy < bot) { canvas.drawLine(l, yy, r, yy, s.stroke(col, 1f, 38)); yy += 5f }
            val word = ADS[s.i(0, ADS.size)]
            val tp = s.textPaint((r - l) / (word.length * 0.62f), lighten(col, 0.55f), Typeface.DEFAULT_BOLD)
            tp.setShadowLayer(14f, 0f, 0f, col)
            canvas.drawText(word, (l + r) / 2f, (top + bot) / 2f + tp.textSize * 0.34f, tp)
            val small = s.textPaint((r - l) * 0.075f, withAlpha(col, 200), Typeface.MONOSPACE)
            canvas.drawText("未来は今  //  2087", (l + r) / 2f, bot - 6f, small)
        }

        // The figure: a black coat and an umbrella against the haze, a pool of light at the feet.
        run {
            val z = s.f(2.3f, 2.9f)
            val gx = cam.x(s.f(-0.5f, 0.5f), z); val gy = cam.y(0f, z); val u = F / z
            canvas.drawOval(RectF(gx - 1.1f * u, gy - 0.1f * u, gx + 1.1f * u, gy + 0.22f * u), s.fill(mix(cL, cR, 0.5f), 70, u * 0.3f))
            val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            val coat = Path().apply {
                moveTo(gx - 0.3f * u, gy - 1.45f * u); lineTo(gx + 0.3f * u, gy - 1.45f * u)
                lineTo(gx + 0.36f * u, gy - 0.5f * u); lineTo(gx - 0.36f * u, gy - 0.5f * u); close()
            }
            canvas.drawPath(coat, ink)
            canvas.drawRect(gx - 0.2f * u, gy - 0.55f * u, gx - 0.05f * u, gy, ink)
            canvas.drawRect(gx + 0.05f * u, gy - 0.55f * u, gx + 0.2f * u, gy, ink)
            canvas.drawCircle(gx, gy - 1.6f * u, 0.12f * u, ink)
            val dome = Path().apply { addArc(RectF(gx - 0.7f * u, gy - 2.3f * u, gx + 0.7f * u, gy - 1.55f * u), 180f, 180f); close() }
            canvas.drawPath(dome, ink)
            canvas.drawPath(dome, s.stroke(lighten(cL, 0.2f), 2f, 210, 3f))
            canvas.drawLine(gx, gy - 1.93f * u, gx, gy - 1.5f * u, s.stroke(Color.BLACK, 0.04f * u))
            canvas.drawLine(gx + 0.3f * u, gy - 1.45f * u, gx + 0.36f * u, gy - 0.5f * u, s.stroke(cR, 1.8f, 170))
            repeat(3) {
                val rr = s.f(0.2f, 0.7f) * u
                canvas.drawOval(RectF(gx - rr + s.f(-0.5f, 0.5f) * u, gy + 0.02f * u, gx + rr + s.f(-0.5f, 0.5f) * u, gy + 0.02f * u + rr * 0.14f), s.stroke(Color.WHITE, 1.2f, 60))
            }
        }

        // Rain, slanting, and a mist along the road.
        repeat(s.count(300)) {
            val x = s.f(-20f, s.w); val y = s.f(0f, s.h); val len = s.f(16f, 46f)
            canvas.drawLine(x, y, x + len * 0.16f, y + len, s.stroke(Color.rgb(190, 225, 255), s.f(0.8f, 1.5f), s.i(18, 70)))
        }
        canvas.drawOval(RectF(cam.vx - 520f, cam.vy - 30f, cam.vx + 520f, cam.vy + 90f), s.fill(mix(cL, cR, 0.5f), 26, 70f))
    }
}

// ---------------------------------------------------------------------------------------------

/** Where Source Code finds real source: the app's own generator files, copied into the assets at build time. */
object CodeSource {
    @Volatile
    private var loader: ((String) -> String?)? = null

    @Volatile
    var names: List<String> = emptyList()
        private set

    private val cache = object : LinkedHashMap<String, String>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 4
    }

    fun init(assets: AssetManager) {
        if (loader != null) return
        val list = runCatching { assets.list("code")?.filter { it.endsWith(".kt") }?.sorted() }.getOrNull().orEmpty()
        names = list
        loader = { n -> runCatching { assets.open("code/$n").bufferedReader().use { it.readText() } }.getOrNull() }
    }

    /** For desktop previews: read the same files straight from a folder. */
    fun init(dir: java.io.File) {
        names = dir.listFiles { f -> f.name.endsWith(".kt") }?.map { it.name }?.sorted().orEmpty()
        loader = { n -> runCatching { java.io.File(dir, n).readText() }.getOrNull() }
    }

    fun read(name: String): String? = synchronized(cache) {
        cache[name] ?: loader?.invoke(name)?.also { cache[name] = it }
    }

    /** Used when the files are not available: the camera maths behind Wet Street. */
    val FALLBACK = """
        private class Cam(val vx: Float, val vy: Float) {
            fun x(lat: Float, z: Float) = vx + lat / z * F
            fun y(height: Float, z: Float) = vy + (CAM - height) / z * F
        }

        // One-point perspective: depth z shrinks everything toward the vanishing point.
        for (t in towers) {
            val lat = t.side * t.lat
            val xa = cam.x(lat, t.z0)
            val xb = cam.x(lat, t.z1)
            path.moveTo(xa, cam.y(0f, t.z0))
            path.lineTo(xb, cam.y(0f, t.z1))
            path.lineTo(xb, cam.y(t.ht, t.z1))
            path.lineTo(xa, cam.y(t.ht, t.z0))
            path.close()
            canvas.drawPath(path, body)
        }

        // The road is black glass: mirror every light about the ground line.
        val mirrored = cam.y(-height, z)
        canvas.drawLine(x, cam.y(-0.7f, z), x, mirrored, glow)
    """.trimIndent()
}

// ---------------------------------------------------------------------------------------------
// Code wallpapers: the app's own generator source, set in loud syntax colour and then torn up.
// ---------------------------------------------------------------------------------------------

/** Colours for one code wallpaper, pushed to full saturation so they stay striking on black. */
private class CodeTheme(
    val kw: Int, val str: Int, val num: Int, val type: Int, val fn: Int, val accent: Int,
    val comment: Int, val plain: Int, val punct: Int, val err: Int,
)

private fun vivid(c: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(c, hsv)
    if (hsv[1] < 0.15f) return c                      // greys stay grey (the Mono palette)
    hsv[1] = hsv[1].coerceAtLeast(0.78f)
    hsv[2] = 1f
    return Color.HSVToColor(hsv)
}

private fun turn(c: Int, degrees: Float): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(c, hsv)
    if (hsv[1] < 0.15f) return c
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    return Color.HSVToColor(hsv)
}

private fun Scene.codeTheme(): CodeTheme {
    val a = vivid(color(0)); val b = vivid(color(1)); val c = vivid(color(2))
    val d = if (palette.colors.size > 3) vivid(color(3)) else turn(a, 150f)
    return CodeTheme(
        kw = a, str = b, num = c, type = turn(a, -45f), fn = d, accent = a,
        comment = Color.rgb(104, 118, 135), plain = Color.rgb(236, 240, 250), punct = Color.rgb(146, 156, 176),
        err = Color.rgb(255, 40, 80),
    )
}

private val VIVID = listOf("cyber", "cyber", "hue", "graffiti", "aurora", "acid", "lava", "violet", "cyan")

private val KEYWORDS = setOf(
    "val", "var", "fun", "object", "class", "interface", "private", "internal", "override", "abstract", "open", "return",
    "if", "else", "for", "while", "when", "in", "is", "as", "import", "package", "repeat", "true", "false", "null",
    "const", "data", "enum", "companion", "this", "by", "get", "set", "init", "inline", "lateinit", "break", "continue", "do",
)
private val TOKEN: Pattern = Pattern.compile("(//.*)|(\"(?:\\\\.|[^\"\\\\])*\")|(0x[0-9A-Fa-f]+[LlUu]*|\\d[\\d_]*(?:\\.\\d+)?[fFLl]?)|([A-Za-z_][A-Za-z0-9_]*)|(\\s+)|(.)")
private val DECL = Regex("^\\s*(private |internal |override |abstract )*(object|fun|class)\\b")
private val FUN_NAME = Regex("\\bfun\\s+(?:[A-Za-z]+\\.)?([A-Za-z_][A-Za-z0-9_]*)")
private const val NOISE = "#@%&\$*+=<>/\\|01▓▒░█"

/** One line of the listing: code to be coloured, or ready-made coloured pieces. */
private class Row(
    val text: String = "",
    val segs: List<Pair<String, Int>>? = null,
    val bg: Int = 0,
    val gutter: String? = null,
    /** An easter egg line: lit in the accent colour so it can be found. */
    val egg: Boolean = text.trim() in EGG_SET,
    /** Column where the line starts to fade away into the dark; -1 for a line that stays whole. */
    val eat: Int = -1,
    /** A faint, offset second copy of the line, like a ghost image. */
    val echo: Boolean = false,
)

/** Little surprises hidden in every code wallpaper. */
private val EGGS: List<List<String>> = listOf(
    listOf("    // 🐱 meow"),
    listOf("    // TODO: feed the cat 🐈"),
    listOf("    val cat = \"=^.^=\"  // purr"),
    listOf("    // /\\_/\\", "    //( o.o )", "    // > ^ <"),
    listOf("    // 👻 boo"),
    listOf("    // it works on my machine 🤷"),
    listOf("    // sudo make me a sandwich 🥪"),
    listOf("    // here be dragons 🐉"),
    listOf("    // ☕ coffee.exe has stopped responding"),
    listOf("    // It's not a lake, it's an ocean."),
    listOf("    // 404: sleep not found 😴"),
    listOf("    val answer = 42  // obviously"),
    listOf("    // the cat sat on the keyboard: kkkkkkkkkkkk 🐈"),
    listOf("    // Scary monsters and nice sprites 🎧"),
    listOf("    // Altered item detected 🔦"),
    listOf("    // 🦆 quack (rubber duck debugging)"),
)
private val EGG_SET: Set<String> = EGGS.flatten().map { it.trim() }.toSet()

private class Src(val name: String, val lines: List<String>, val eggAt: List<Int>)

private fun Scene.source(): Src {
    val names = CodeSource.names
    val name = names.takeIf { it.isNotEmpty() }?.let { it[i(0, it.size)] }
    val text = name?.let { CodeSource.read(it) } ?: CodeSource.FALLBACK
    val lines = text.replace("\t", "    ").lines().toMutableList()
    // Hide three or four surprises somewhere in the file.
    val picks = EGGS.shuffled(java.util.Random(i(0, 1_000_000).toLong())).take(i(3, 5))
    for (egg in picks) lines.addAll(i(2, maxOf(3, lines.size - 2)), egg)
    val at = lines.indices.filter { lines[it].trim() in EGG_SET }
    return Src(name ?: "WetStreet.kt", lines, at)
}

/** The first line to show so that a declaration lands on the lit row. */
private fun Scene.windowStart(src: Src, rows: Int, focusRow: Int): Int {
    // Mostly, show one of the hidden surprises somewhere on the page.
    if (src.eggAt.isNotEmpty() && chance(0.75f)) {
        val egg = src.eggAt[i(0, src.eggAt.size)]
        val spot = if (chance(0.5f)) focusRow + i(3, 14) else i(4, rows - 5)
        return (egg - spot).coerceIn(0, maxOf(0, src.lines.size - rows))
    }
    val decls = src.lines.indices.filter { DECL.containsMatchIn(src.lines[it]) }
    val pick = if (decls.isEmpty()) 0 else decls[i(0, decls.size)]
    return (pick - focusRow).coerceIn(0, maxOf(0, src.lines.size - rows))
}

private fun highlight(line: String, th: CodeTheme): List<Pair<String, Int>> {
    val t = line.trimStart()
    if (t.startsWith("/**") || t.startsWith("*") || t.startsWith("//")) return listOf(line to th.comment)
    val out = ArrayList<Pair<String, Int>>()
    val m = TOKEN.matcher(line)
    while (m.find()) {
        val tok = m.group()
        val color = when {
            m.group(1) != null -> th.comment
            m.group(2) != null -> th.str
            m.group(3) != null -> th.num
            m.group(4) != null -> when {
                tok in KEYWORDS -> th.kw
                tok[0].isUpperCase() -> th.type
                line.getOrNull(m.end()) == '(' -> th.fn
                else -> th.plain
            }
            m.group(5) != null -> Color.TRANSPARENT
            else -> if (tok == "@") th.accent else th.punct
        }
        out += tok to color
    }
    return out
}

/**
 * Sets [rows] in monospace down the page, brightest around the lit row (the clear band between the
 * widgets and the icons) and dimmer away from it. [corrupt] is the share of characters swapped for noise.
 */
private fun Scene.drawRows(
    rows: List<Row>, th: CodeTheme, size: Float, lead: Float, gutterCols: Int, sigma: Float, floor: Float,
    corrupt: Float, focusRow: Int, cursor: Boolean, bar: Boolean, rot: Float = 0f,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
    val adv = paint.measureText("M")
    val margin = 26f
    val codeX = margin + gutterCols * adv
    val cols = ((w - codeX - margin) / adv).toInt()
    val focusY = h * 0.4f
    for ((r, row) in rows.withIndex()) {
        val y = margin + lead * (r + 1) - lead * 0.22f
        val lum = floor + (1f - floor) * Math.exp(-Math.pow(((y - focusY) / (h * sigma)).toDouble(), 2.0)).toFloat()
        if (row.bg != 0) canvas.drawRect(0f, y - lead * 0.78f, w, y + lead * 0.22f, fill(row.bg or -0x1000000, (Color.alpha(row.bg) * (0.4f + 0.6f * lum)).toInt()))
        if (bar && r == focusRow) canvas.drawRect(0f, y - lead * 0.78f, w, y + lead * 0.22f, fill(th.accent, 34))
        if (row.gutter != null) {
            paint.color = withAlpha(if (r == focusRow) th.accent else th.punct, (230 * lum).toInt().coerceAtLeast(50))
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(row.gutter, margin + gutterCols * adv - adv * 1.1f, y, paint)
            paint.textAlign = Paint.Align.LEFT
        }
        if (row.echo && row.segs == null) {
            paint.color = withAlpha(th.err, (70 * lum).toInt())
            paint.clearShadowLayer()
            canvas.drawText(row.text.take(cols), codeX + 9f, y + 4f, paint)
        }
        val segs = row.segs ?: if (row.egg) listOf(row.text to lighten(th.fn, 0.25f)) else highlight(row.text.take(cols), th)
        var col = 0
        for ((tok0, color) in segs) {
            if (col >= cols) break
            val tok = if (col + tok0.length > cols) tok0.substring(0, cols - col) else tok0
            if (color != Color.TRANSPARENT) {
                val chars = tok.toCharArray()
                var hit = false
                if (row.eat >= 0 && col + tok.length > row.eat) {
                    // The line is being consumed: each character dimmer than the one before it.
                    for (k in chars.indices) {
                        val fadeOut = 1f - ((col + k - row.eat) / 14f).coerceIn(0f, 1f)
                        if (fadeOut <= 0.02f || chars[k] == ' ') continue
                        paint.clearShadowLayer()
                        paint.color = withAlpha(color, (255 * lum * fadeOut).toInt())
                        canvas.drawText(chars[k].toString(), codeX + (col + k) * adv, y, paint)
                    }
                    col += tok0.length
                    continue
                }
                val rate = corrupt + rot * (1f - lum)
                if (rate > 0f) for (k in chars.indices) if (chars[k] != ' ' && chance(rate)) { chars[k] = NOISE[i(0, NOISE.length)]; hit = true }
                val hot = lum > 0.5f
                if (hot) paint.setShadowLayer(8f + 8f * lum, 0f, 0f, withAlpha(color, 230)) else paint.clearShadowLayer()
                if (!hit) {
                    paint.color = withAlpha(color, (255 * lum).toInt())
                    canvas.drawText(tok, codeX + col * adv, y, paint)
                } else {
                    for (k in chars.indices) {
                        val bad = chars[k] != tok[k]
                        paint.color = withAlpha(if (bad) (if (chance(0.5f)) th.err else lighten(th.accent, 0.4f)) else color, (255 * lum).toInt())
                        canvas.drawText(chars[k].toString(), codeX + (col + k) * adv, y, paint)
                    }
                }
            }
            col += tok0.length
        }
        paint.clearShadowLayer()
        if (cursor && r == focusRow) canvas.drawRect(codeX + col * adv + adv * 0.3f, y - size * 0.82f, codeX + col * adv + adv * 1.1f, y + 4f, fill(th.accent, 235, 5f))
    }
}

private fun Scene.rowCount(lead: Float): Int = ((h - 52f) / lead).toInt()
private fun Scene.focusRow(lead: Float): Int = ((h * 0.4f - 26f) / lead).toInt()

/** Base for the code styles: draw into a layer, then tear it with a colour split. */
private abstract class CodeStyle(id: String, name: String, val split: Float, val tears: Int, val shift: Float) :
    Style(id, name, Category.GLITCH) {
    override val autoPalettes = VIVID
    override val usesDetail: Boolean get() = false
    override val widgetSafe: Boolean get() = true
    abstract fun paintCode(s: Scene)
    override fun draw(s: Scene) {
        val bmp = s.layer { paintCode(it) }
        s.glitchComposite(bmp, split, tears, shift)
    }
}

/** An editor: line numbers, a lit current line with a cursor, and a file tab's worth of syntax colour. */
private object SourceCode : CodeStyle("source_code", "Source Code", 3.2f, 3, 38f) {
    override fun paintCode(s: Scene) {
        val th = s.codeTheme(); val src = s.source()
        val lead = 27.5f; val rows = s.rowCount(lead); val focus = s.focusRow(lead)
        val start = s.windowStart(src, rows, focus)
        val list = (0 until rows).mapNotNull { k -> src.lines.getOrNull(start + k)?.let { Row(it, gutter = (start + k + 1).toString()) } }
        s.drawRows(list, th, 19f, lead, 5, 0.2f, 0.4f, 0.004f, focus, cursor = true, bar = true)
    }
}

/** A git diff: hunks of added and removed lines with a loud green and red wash. */
private object CodeDiff : CodeStyle("code_diff", "Diff Glitch", 3.8f, 4, 46f) {
    override fun paintCode(s: Scene) {
        val th = s.codeTheme(); val src = s.source()
        val lead = 27.5f; val rows = s.rowCount(lead); val focus = s.focusRow(lead)
        val start = s.windowStart(src, rows, focus)
        val salt = s.i(0, 1_000_000)
        val add = Color.argb(84, 0, 255, 120); val del = Color.argb(84, 255, 40, 90)
        val list = (0 until rows).mapNotNull { k ->
            val line = src.lines.getOrNull(start + k) ?: return@mapNotNull null
            val roll = (((start + k + salt) * 2654435761L) ushr 7) % 100
            when {
                k == focus - 1 -> Row(segs = listOf("@@ -${start + k},7 +${start + k},9 @@ " to th.num, line.trim().take(40) to th.fn))
                roll < 22 -> Row(line, bg = add, gutter = "+")
                roll < 31 -> Row(line, bg = del, gutter = "-")
                else -> Row(line, gutter = (start + k + 1).toString())
            }
        }
        s.drawRows(list, th, 19f, lead, 5, 0.22f, 0.4f, 0.003f, focus, cursor = false, bar = false)
    }
}

/** A crash log built from real file names, functions and line numbers. */
private object CodePanic : CodeStyle("code_panic", "Kernel Panic", 6f, 7, 70f) {
    private val EXCEPTIONS = listOf(
        "java.lang.IndexOutOfBoundsException: index=7, size=3",
        "java.lang.IllegalStateException: palette is empty",
        "java.lang.OutOfMemoryError: bitmap too large for 4K",
        "java.lang.ArithmeticException: divide by zero",
        "kotlin.NotImplementedError: An operation is not implemented: more sky",
        "java.lang.NullPointerException: no pixel here",
        "java.lang.CatNotFoundException: 🐈 left the building",
        "java.lang.StackOverflowError: it is turtles 🐢 all the way down",
    )

    override fun paintCode(s: Scene) {
        val th = s.codeTheme()
        val lead = 27.5f; val rows = s.rowCount(lead); val focus = s.focusRow(lead)
        val files = (CodeSource.names.ifEmpty { listOf("WetStreet.kt") }).shuffled(java.util.Random(s.i(0, 1_000_000).toLong())).take(6)
        val frames = ArrayList<Triple<String, String, Int>>()
        for (f in files) {
            val lines = (CodeSource.read(f) ?: CodeSource.FALLBACK).lines()
            for ((n, l) in lines.withIndex()) FUN_NAME.find(l)?.let { frames += Triple(f.removeSuffix(".kt"), it.groupValues[1], n + 1) }
        }
        if (frames.isEmpty()) frames += Triple("WetStreet", "draw", 88)
        fun frame(): Row {
            val (cls, fn, ln) = frames[s.i(0, frames.size)]
            return Row(segs = listOf(
                "E/AndroidRuntime: " to Color.rgb(140, 60, 80), "    at " to th.punct, "com.nikopick.zamoled.gen." to th.comment,
                cls to th.type, "." to th.punct, fn to th.fn, "(" to th.punct, "$cls.kt:$ln" to th.num, ")" to th.punct,
            ))
        }
        val list = ArrayList<Row>()
        repeat((focus - 4).coerceAtLeast(0)) { list += frame() }
        list += Row(segs = listOf("--------- beginning of crash" to th.comment))
        list += Row(segs = listOf("E/AndroidRuntime: " to Color.rgb(140, 60, 80), "FATAL EXCEPTION: main" to th.err), bg = Color.argb(70, 255, 40, 80))
        list += Row(segs = listOf("E/AndroidRuntime: " to Color.rgb(140, 60, 80), "Process: com.nikopick.zamoled, PID: ${s.i(1000, 9999)}" to th.plain))
        list += Row(segs = listOf("E/AndroidRuntime: " to Color.rgb(140, 60, 80), EXCEPTIONS[s.i(0, EXCEPTIONS.size)] to th.err))
        while (list.size < rows) {
            list += if (s.chance(0.08f)) Row(segs = listOf("E/AndroidRuntime: " to Color.rgb(140, 60, 80), "Caused by: " to th.accent, EXCEPTIONS[s.i(0, EXCEPTIONS.size)] to th.err)) else frame()
        }
        s.drawRows(list.take(rows), th, 18f, lead, 0, 0.24f, 0.4f, 0.012f, focus, cursor = true, bar = false)
    }
}

/** A hex dump of the real file: offsets, bytes and the characters beside them. */
private object CodeHex : CodeStyle("code_hex", "Hex Dump", 4.2f, 6, 60f) {
    override fun paintCode(s: Scene) {
        val th = s.codeTheme(); val src = s.source()
        val bytes = src.lines.joinToString("\n").toByteArray()
        val lead = 27.5f; val rows = s.rowCount(lead); val focus = s.focusRow(lead)
        val first = if (bytes.size > rows * 16) s.i(0, (bytes.size - rows * 16) / 16) * 16 else 0
        fun tone(b: Int): Int = when {
            b == 0x20 || b == 0x0a -> th.comment
            b in 0x30..0x39 -> th.num
            b in 0x41..0x5a || b in 0x61..0x7a -> th.kw
            b in 0x21..0x7e -> th.str
            else -> th.err
        }
        val list = (0 until rows).mapNotNull { r ->
            val off = first + r * 16
            if (off >= bytes.size) return@mapNotNull null
            val segs = ArrayList<Pair<String, Int>>()
            segs += String.format("%08x  ", off) to th.punct
            val ascii = StringBuilder()
            for (k in 0 until 16) {
                val b = if (off + k < bytes.size) bytes[off + k].toInt() and 0xff else -1
                if (b < 0) { segs += "   " to th.comment; continue }
                val shown = if (s.chance(0.006f)) s.i(0, 256) else b
                segs += String.format("%02x ", shown) to (if (shown != b) th.err else tone(b))
                if (k == 7) segs += " " to th.comment
                ascii.append(if (b in 0x20..0x7e) b.toChar() else '.')
            }
            segs += " |" to th.punct
            segs += ascii.toString() to th.plain
            segs += "|" to th.punct
            Row(segs = segs)
        }
        s.drawRows(list, th, 18f, lead, 0, 0.22f, 0.4f, 0f, focus, cursor = false, bar = true)
    }
}

/** Code rain made of real code: each column is a run of characters from the file, reading downward. */
private object CodeFall : CodeStyle("code_fall", "Code Fall", 3f, 4, 44f) {
    override val widgetSafe: Boolean get() = false
    override fun paintCode(s: Scene) {
        val th = s.codeTheme(); val src = s.source()
        val stream = src.lines.joinToString(" ") { it.trim() }.filter { !it.isWhitespace() && !it.isSurrogate() }.ifEmpty { "fundrawscene" }
        val size = 22f; val lead = 25f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        val adv = paint.measureText("M") * 1.04f
        var x = adv / 2f
        while (x < s.w) {
            if (s.chance(0.7f)) {
                val len = s.i(10, 46)
                val head = s.f(0f, s.h / lead + len).toInt()
                val at = s.i(0, stream.length)
                for (k in 0 until len) {
                    val row = head - len + 1 + k
                    val y = row * lead
                    if (y < -lead || y > s.h + lead) continue
                    val ch = stream[(at + k) % stream.length]
                    val t = k / (len - 1f)
                    val base = when { ch.isDigit() -> th.num; ch.isLetter() -> if (ch.isUpperCase()) th.type else th.kw; else -> th.str }
                    if (k == len - 1) { paint.color = Color.WHITE; paint.setShadowLayer(14f, 0f, 0f, th.accent) }
                    else { paint.color = withAlpha(base, (70 + 185 * t).toInt()); paint.setShadowLayer(6f, 0f, 0f, withAlpha(base, (150 * t).toInt())) }
                    s.canvas.drawText(if (k < len - 1 && s.chance(0.004f)) "🐈" else ch.toString(), x, y, paint)
                }
            }
            x += adv
        }
    }
}

/** Poster type: a few lines of real code set huge, cropped at the edges, split hard. */
private object CodePoster : CodeStyle("code_poster", "Big Code", 9f, 4, 90f) {
    override fun paintCode(s: Scene) {
        val th = s.codeTheme(); val src = s.source()
        val lead = 66f; val rows = s.rowCount(lead); val focus = s.focusRow(lead)
        val start = s.windowStart(src, rows, focus)
        val list = (0 until rows).mapNotNull { k ->
            src.lines.getOrNull(start + k)?.trim()?.let { Row(it.take(40)) }
        }
        s.drawRows(list, th, 48f, lead, 0, 0.17f, 0.12f, 0.004f, focus, cursor = true, bar = false)
    }
}

/**
 * Code that is being eaten. Blood-red lines fade out mid-sentence, words go missing, whole lines are
 * left as ghosts, and the dark closes in from the edges. Now and then something is written in its place.
 */
private object CodeCorrupt : CodeStyle("code_corrupt", "Corrupted", 6f, 10, 70f) {
    override val usesPalette: Boolean get() = false
    private val DREAD = listOf(
        "HELP ME", "I SEE YOU", "DON'T LOOK BEHIND YOU", "IT IS INSIDE THE CODE", "WHO IS WRITING THIS",
        "I CAN HEAR YOU TYPING", "WAKE UP", "YOU SHOULD NOT HAVE RUN THIS", "NO ONE IS HERE", "LET ME OUT",
        "IT KNOWS YOUR NAME", "STOP SCROLLING", "BEHIND YOU", "THE CAT IS NOT A CAT 🐈",
    )

    override fun paintCode(s: Scene) {
        val th = CodeTheme(
            kw = Color.rgb(255, 34, 52), str = Color.rgb(255, 112, 92), num = Color.rgb(255, 196, 150), type = Color.rgb(210, 0, 40),
            fn = Color.rgb(255, 72, 72), accent = Color.rgb(255, 24, 44), comment = Color.rgb(126, 36, 48),
            plain = Color.rgb(240, 208, 208), punct = Color.rgb(156, 62, 72), err = Color.rgb(255, 0, 30),
        )
        val src = s.source()
        val lead = 27.5f; val rows = s.rowCount(lead); val focus = s.focusRow(lead)
        val start = s.windowStart(src, rows, focus)
        val list = (0 until rows).mapNotNull { k ->
            val line = src.lines.getOrNull(start + k) ?: return@mapNotNull null
            val num = (start + k + 1).toString()
            val dist = abs(k - focus) / rows.toFloat()                 // the further from the lit row, the more decayed
            when {
                k == focus -> Row(segs = listOf(DREAD[s.i(0, DREAD.size)].toList().joinToString(" ") to Color.WHITE), bg = Color.argb(110, 255, 0, 20))
                s.chance(0.04f) -> Row(segs = listOf(DREAD[s.i(0, DREAD.size)] to th.err), gutter = "!")
                // A line that just stops, the rest of it dissolving after the last whole word.
                s.chance(0.22f + dist * 0.5f) -> Row(line, gutter = num, eat = s.i(4, 38))
                // Words that have vanished from the middle of a line.
                s.chance(0.2f) -> {
                    val words = line.split(' ').toMutableList()
                    repeat(s.i(1, 3)) { val at = s.i(0, words.size); words[at] = " ".repeat(words[at].length) }
                    Row(words.joinToString(" "), gutter = num)
                }
                // A line that has gone, leaving the shape of its first few characters.
                s.chance(0.1f + dist * 0.2f) -> Row(line, gutter = num, eat = s.i(0, 4))
                s.chance(0.1f) -> Row(line.reversed().trimEnd(), gutter = num, echo = true)
                s.chance(0.14f) -> Row(line, gutter = num, echo = true)
                else -> Row(line, gutter = num)
            }
        }
        s.drawRows(list, th, 19f, lead, 5, 0.27f, 0.3f, 0.012f, focus, cursor = true, bar = false, rot = 0.16f)
        // Parts of the page the dark has already taken, soft on the side the text is dissolving from.
        repeat(s.i(5, 9)) {
            val y = s.rowCount(lead).let { n -> 26f + lead * s.i(0, n) }
            val x0 = s.f(0f, s.w * 0.7f); val len = s.f(260f, 640f); val hh = lead * s.i(1, 5)
            val p = Paint().apply { shader = LinearGradient(x0, 0f, x0 + len, 0f, intArrayOf(0, Color.BLACK, Color.BLACK), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP) }
            s.canvas.drawRect(x0, y, minOf(s.w, x0 + len), y + hh, p)
        }
        // The dark closing in from every side.
        val vignette = Paint().apply {
            shader = RadialGradient(s.cx, s.h * 0.4f, s.h * 0.62f, intArrayOf(0, 0, withAlpha(Color.BLACK, 215)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        }
        s.canvas.drawRect(0f, 0f, s.w, s.h, vignette)
    }
}

/** A Gradle build scrolling past, warnings and errors named after real files and lines. */
private object CodeBuild : CodeStyle("code_build", "Build Log", 3.6f, 4, 50f) {
    private val TASKS = listOf("preBuild", "compileDebugKotlin", "mergeDebugAssets", "dexBuilderDebug", "processDebugResources", "packageDebug", "lintVitalRelease", "minifyReleaseWithR8")
    private val WARNINGS = listOf("Variable '%s' is never used", "Unnecessary safe call on a non-null receiver", "Parameter '%s' is never used", "'%s' is deprecated")
    private val ERRORS = listOf("Unresolved reference: sparkle", "Unresolved reference: meow 🐈", "Type mismatch: inferred type is Int but Float was expected", "Expecting a top level declaration")

    override fun paintCode(s: Scene) {
        val th = s.codeTheme()
        val lead = 27.5f; val rows = s.rowCount(lead); val focus = s.focusRow(lead)
        val names = CodeSource.names.ifEmpty { listOf("WetStreet.kt") }
        val green = Color.rgb(60, 255, 140)
        val list = ArrayList<Row>()
        while (list.size < rows) {
            val f = names[s.i(0, names.size)]
            when (s.i(0, 10)) {
                in 0..3 -> list += Row(segs = listOf("> Task " to th.punct, ":app:${TASKS[s.i(0, TASKS.size)]}" to th.fn, (if (s.chance(0.3f)) " UP-TO-DATE" else "") to green))
                in 4..6 -> list += Row(segs = listOf("w: " to th.num, "file:///app/gen/$f:${s.i(8, 400)}:${s.i(3, 40)} " to th.comment, WARNINGS[s.i(0, WARNINGS.size)].replace("%s", listOf("size", "rot", "fill", "alpha")[s.i(0, 4)]) to th.plain))
                7 -> list += Row(segs = listOf("e: " to th.err, "file:///app/gen/$f:${s.i(8, 400)}:${s.i(3, 40)} " to th.comment, ERRORS[s.i(0, ERRORS.size)] to th.err), bg = Color.argb(60, 255, 40, 80))
                8 -> if (s.chance(0.3f)) list += Row(segs = listOf("> Task " to th.punct, ":app:feedTheCat 🐈" to th.fn)) else { val n = s.i(2, 20); list += Row(segs = listOf("[" to th.punct, "█".repeat(n) to th.accent, "░".repeat(20 - n) to th.comment, "] ${n * 5}%" to th.plain)) }
                else -> list += Row(segs = listOf((if (s.chance(0.5f)) "BUILD SUCCESSFUL" else "BUILD FAILED") to green, " in ${s.i(1, 3)}m ${s.i(1, 59)}s" to th.plain))
            }
        }
        s.drawRows(list.take(rows), th, 18f, lead, 0, 0.22f, 0.4f, 0.004f, focus, cursor = true, bar = false)
    }
}

object GlitchWorld {
    val styles: List<Style> = listOf(WetStreet, SourceCode, CodeDiff, CodePanic, CodeHex, CodeFall, CodePoster, CodeCorrupt, CodeBuild)
}
