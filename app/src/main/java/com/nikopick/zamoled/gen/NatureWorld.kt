package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The Nature world: night landscapes where the land is a true-black silhouette against light
 * (a moon, an aurora, the Milky Way), so most of every picture is pixels the screen switches off.
 */

/** Pale moon colour from the palette, never fully saturated. */
private fun Scene.moonTone(): Int = lighten(color(0), 0.82f)

/** A moon: soft halo rings, the disc, and a few faint maria. */
private fun Scene.moon(x: Float, y: Float, r: Float, tint: Int) {
    softGlow(x, y, r * 4.2f, tint, 70)
    softGlow(x, y, r * 2.2f, tint, 90)
    canvas.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(x - r * 0.3f, y - r * 0.3f, r * 1.4f, lighten(tint, 0.35f), darken(tint, 0.1f), Shader.TileMode.CLAMP)
    })
    repeat(7) {
        val a = f(0f, (2 * PI).toFloat())
        val d = f(0f, r * 0.7f)
        canvas.drawCircle(x + cos(a) * d, y + sin(a) * d, f(r * 0.06f, r * 0.2f), fill(darken(tint, 0.35f), 40, r * 0.04f))
    }
}

/** A conifer: stacked triangles, base at (x, baseY). */
private fun Path.pine(x: Float, baseY: Float, height: Float, width: Float) {
    val tiers = 5
    moveTo(x, baseY - height)
    for (k in 1..tiers) {
        val y = baseY - height + height * k / tiers
        val half = width * k / tiers * 0.5f
        lineTo(x + half * 0.55f, y - height * 0.04f)
        lineTo(x + half, y)
    }
    lineTo(x + width * 0.06f, baseY)
    lineTo(x - width * 0.06f, baseY)
    for (k in tiers downTo 1) {
        val y = baseY - height + height * k / tiers
        val half = width * k / tiers * 0.5f
        lineTo(x - half, y)
        lineTo(x - half * 0.55f, y - height * 0.04f)
    }
    close()
}

/** A row of pines along a skyline, [color] filled down to the bottom of the screen. */
private fun Scene.treeLine(baseY: Float, minH: Float, maxH: Float, gap: Float, color: Int) {
    val p = Path()
    var x = -30f
    while (x < w + 30f) {
        val hgt = f(minH, maxH)
        p.pine(x, baseY + f(-10f, 14f), hgt, hgt * f(0.34f, 0.46f))
        x += gap * f(0.6f, 1.4f)
    }
    canvas.drawPath(p, fill(color))
    canvas.drawRect(0f, baseY, w, h, fill(color))
}

// ---------------------------------------------------------------------------------------------

/** The wolf, drawn as one outline in a 100-unit box (x right, y down, feet at y = 96), facing left. */
internal val HOWL_WOLF = floatArrayOf(
    8f, 16f, 11f, 12f, 17f, 10f, 23f, 10f, 27f, 13f, 30f, 11f,
    30f, 6f, 33f, -3f, 37f, 5f, 39f, 10f,
    41f, 14f, 44f, 20f, 48f, 26f, 54f, 31f, 61f, 35f, 68f, 37f, 76f, 39f, 84f, 40f, 90f, 43f, 93f, 47f,
    96f, 53f, 98f, 63f, 98f, 74f, 95f, 85f, 91f, 95f, 89f, 89f, 88f, 78f, 88f, 67f, 87f, 60f,
    86f, 64f, 84f, 72f, 86f, 80f, 85f, 90f, 88f, 96f, 80f, 96f, 79f, 90f, 78f, 82f, 77f, 74f, 73f, 67f,
    66f, 62f, 60f, 61f,
    59f, 68f, 58f, 80f, 59f, 90f, 62f, 96f, 53f, 96f, 52f, 90f, 52f, 80f, 50f, 70f, 47f, 64f,
    44f, 66f, 43f, 62f, 40f, 64f, 39f, 58f, 36f, 60f, 35f, 53f, 31f, 54f, 30f, 47f, 27f, 48f, 26f, 41f,
    24f, 33f, 22f, 27f, 20f, 23f,
    14f, 24f, 11f, 22f, 16f, 20f, 22f, 18.5f, 15f, 17.5f,
)

/** A wolf with its feet at (x, groundY), [height] tall, facing left (or right when [flip]). */
internal fun wolfPath(x: Float, groundY: Float, height: Float, flip: Boolean): Path {
    val k = height / 96f
    val sgn = if (flip) -1f else 1f
    val p = Path()
    for (i in 0 until HOWL_WOLF.size / 2) {
        val px = x + (HOWL_WOLF[i * 2] - 50f) * k * sgn
        val py = groundY + (HOWL_WOLF[i * 2 + 1] - 96f) * k
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
    }
    p.close()
    return p
}

/** A wolf howling at a big moon from a rocky ledge, with a pack on the far ridge as Detail goes up. */
object HowlingWolf : Style("howling_wolf", "Howling Wolf", Category.NATURE) {
    override val autoPalettes = listOf("mono", "ocean", "violet", "gold", "sunset", "sakura")

    override fun draw(s: Scene) {
        val tint = s.moonTone()
        val flip = s.chance(0.5f)
        val mr = s.w * s.f(0.2f, 0.27f)
        val wolfH = s.h * s.f(0.23f, 0.27f)
        val ground = s.h * s.f(0.66f, 0.72f)
        val wx = s.cx + (if (flip) -1f else 1f) * s.f(40f, 130f)
        val mx = wx + (if (flip) 1f else -1f) * s.f(20f, 110f)
        val my = ground - wolfH * s.f(0.85f, 1.1f)
        s.starSky(300, my + mr, 8)
        s.moon(mx, my, mr, tint)

        // The far ridge, lit from behind by the moon, with the pack along it.
        val packN = ((s.density - 0.5f) * 3f).toInt().coerceIn(0, 3)
        val far = ground - s.h * 0.05f
        val (farLand, farLine) = s.ridge(far, 90f, 0.004f, s.f(0f, 40f), ridged = false)
        s.canvas.drawPath(farLand, s.vGradient(far - 60f, far + 260f, darken(mix(tint, s.color(1), 0.4f), 0.9f), Color.BLACK))
        s.canvas.drawPath(farLine, s.stroke(withAlpha(tint, 80), 1.8f))
        repeat(packN) {
            val px = s.f(40f, s.w - 40f)
            s.canvas.drawPath(wolfPath(px, far + 6f, wolfH * s.f(0.2f, 0.3f), s.chance(0.5f)), s.fill(Color.BLACK))
        }

        // The ledge and the wolf on it.
        val ledge = Path()
        ledge.moveTo(-10f, s.h + 10f)
        var x = -10f
        while (x <= s.w + 10f) {
            val near = abs(x - wx) / (s.w * 0.28f)
            val y = ground + 6f + s.noise.fbm(x * 0.012f, 7f) * 22f + min(near, 1f) * min(near, 1f) * s.h * 0.05f
            ledge.lineTo(x, y)
            x += 8f
        }
        ledge.lineTo(s.w + 10f, s.h + 10f)
        ledge.close()
        val wolf = wolfPath(wx, ground + 6f, wolfH, flip)
        // A thin rim of moonlight on the side facing the moon.
        s.canvas.drawPath(wolf, s.stroke(withAlpha(lighten(tint, 0.3f), 150), 3.2f, 150, 4f))
        s.canvas.drawPath(wolf, s.fill(Color.BLACK))
        s.canvas.drawPath(wolf, s.stroke(Color.BLACK, 2.6f))
        s.canvas.drawPath(ledge, s.fill(Color.BLACK))
        s.canvas.drawPath(ledge, s.stroke(withAlpha(tint, 60), 1.6f))

        // Pines crowd both edges of the frame.
        s.treeLine(s.h * 0.86f, s.h * 0.1f, s.h * 0.2f, 64f / s.density.coerceAtLeast(0.5f), Color.BLACK)
    }
}

// ---------------------------------------------------------------------------------------------

/** Northern lights: curtains of vertical rays over a black ridge, mirrored in a still lake. */
object NorthernLights : Style("aurora_night", "Northern Lights", Category.NATURE) {
    override val autoPalettes = listOf("aurora", "aurora", "ocean", "violet", "sakura")

    override fun draw(s: Scene) {
        val horizon = s.h * s.f(0.6f, 0.68f)
        s.starSky(420, horizon, 6)
        val c1 = s.color(0)
        val c2 = s.color(1)
        val c3 = s.color(2)
        // One gradient, scaled per ray: bright at the foot, fading to nothing at the tip.
        val shader = LinearGradient(
            0f, 0f, 0f, -1f,
            intArrayOf(lighten(c1, 0.6f), c1, c2, withAlpha(c3, 120), Color.TRANSPARENT),
            floatArrayOf(0f, 0.12f, 0.5f, 0.82f, 1f), Shader.TileMode.CLAMP,
        )
        val curtains = s.i(2, 4)
        for (c in 0 until curtains) {
            val base = horizon * s.f(0.5f, 0.78f)
            val sway = s.f(0.003f, 0.007f)
            val tall = horizon * s.f(0.45f, 0.8f)
            val off = s.f(0f, 50f)
            val step = 2.2f / s.density.coerceIn(0.5f, 1.6f)
            var x = -10f
            while (x < s.w + 10f) {
                val foot = base + s.noise.fbm(x * sway, off + c * 9f) * horizon * 0.14f + sin(x * 0.006f + c) * horizon * 0.03f
                val ray = 0.4f + 0.6f * (s.noise.fbm(x * 0.09f, off + 31f) * 0.5f + 0.5f).coerceIn(0f, 1f)
                val reach = tall * (0.55f + 0.45f * (s.noise.fbm(x * 0.011f, off + 17f) * 0.5f + 0.5f)) * ray
                s.canvas.save()
                s.canvas.translate(x, foot)
                s.canvas.scale(1f, reach)
                s.canvas.drawLine(0f, 0f, 0f, -1f, Paint().apply {
                    this.shader = shader
                    strokeWidth = step * 1.15f
                    alpha = (150 * ray).toInt().coerceIn(30, 190)
                })
                s.canvas.restore()
                x += step
            }
        }
        // A black ridge, and the lake below with the sky laid over it.
        val (land, line) = s.ridge(horizon + 8f, s.h * 0.12f, 0.003f, s.f(0f, 30f), ridged = true)
        s.canvas.drawPath(land, s.fill(Color.BLACK))
        s.canvas.drawPath(line, s.stroke(withAlpha(c1, 60), 1.6f))
        s.canvas.drawRect(0f, horizon + 8f, s.w, horizon + 420f, s.vGradient(horizon, horizon + 420f, withAlpha(c1, 26), Color.TRANSPARENT))
        repeat(s.count(90)) {
            val y = s.f(horizon + 20f, s.h)
            val len = s.f(30f, 160f) * (1f - (y - horizon) / s.h * 0.6f)
            val x = s.f(0f, s.w)
            s.canvas.drawLine(x, y, x + len, y, s.stroke(withAlpha(c1, 55), 1.5f))
        }
        s.treeLine(s.h * 0.9f, s.h * 0.07f, s.h * 0.15f, 70f / s.density.coerceAtLeast(0.5f), Color.BLACK)
    }
}

// ---------------------------------------------------------------------------------------------

/** Ranges of mountains in layers, each paler than the one in front, under a large moon. */
object MoonlitPeaks : Style("moonlit_peaks", "Moonlit Peaks", Category.NATURE) {
    override val autoPalettes = listOf("ocean", "violet", "mono", "gold", "sunset", "aurora")

    override fun draw(s: Scene) {
        val tint = s.moonTone()
        val mr = s.w * s.f(0.12f, 0.2f)
        val mx = s.w * s.f(0.25f, 0.75f)
        val my = s.h * s.f(0.2f, 0.3f)
        s.starSky(260, s.h * 0.6f, 5)
        s.moon(mx, my, mr, tint)
        val layers = 4 + (s.density * 1.5f).toInt().coerceIn(0, 3)
        for (i in 0 until layers) {
            val t = i / (layers - 1f)                                // 0 far .. 1 near
            val base = s.h * (0.56f + 0.3f * t)
            val amp = s.h * (0.26f - 0.14f * t)
            // A real skyline: a few summits with saddles between them, each face roughened like rock.
            val land = Path()
            val line = Path()
            land.moveTo(-20f, s.h + 20f)
            var px = -40f
            var py = base - amp * s.f(0.1f, 0.4f)
            var first = true
            var peak = s.chance(0.5f)
            while (px <= s.w + 60f) {
                val nx = px + s.f(110f, 260f) * (1f - 0.3f * t)
                val ny = base - if (peak) amp * s.f(0.55f, 1f) else amp * s.f(0.12f, 0.42f)
                val steps = 7
                for (k in 0..steps) {
                    val u = k / steps.toFloat()
                    val rough = if (k == 0 || k == steps) 0f else s.noise.fbm((px + u * (nx - px)) * 0.05f, i * 7f) * amp * 0.07f
                    val x = px + (nx - px) * u
                    val y = py + (ny - py) * u + rough
                    land.lineTo(x, y)
                    if (first) line.moveTo(x, y) else line.lineTo(x, y)
                    first = false
                }
                px = nx; py = ny
                peak = !peak
            }
            land.lineTo(s.w + 60f, s.h + 20f)
            land.close()
            // Each range is lit at its crest and falls to black below it, so the next range reads in front of it.
            val crest = darken(mix(tint, s.color(1), 0.5f), 0.8f + 0.17f * t)
            s.canvas.drawPath(land, s.vGradient(base - amp, base + 110f + 120f * (1f - t), crest, Color.BLACK))
            s.canvas.drawPath(line, s.stroke(withAlpha(lighten(tint, 0.25f), (90 * (1f - t)).toInt()), 1.4f))
        }
        s.treeLine(s.h * 0.94f, s.h * 0.08f, s.h * 0.17f, 74f / s.density.coerceAtLeast(0.5f), Color.BLACK)
    }
}

// ---------------------------------------------------------------------------------------------

/** A pine forest in silhouette under the Milky Way, with a shooting star and fireflies. */
object StarryPines : Style("starry_pines", "Starry Pines", Category.NATURE) {
    override val autoPalettes = listOf("ocean", "violet", "aurora", "sunset", "mono")

    override fun draw(s: Scene) {
        val c = s.color(0)
        // The Milky Way: a continuous diagonal band of haze, with fine stars packed into it and a dust lane through it.
        val ang = s.f(-0.55f, -0.2f)
        val cy = s.h * 0.3f
        s.canvas.save()
        s.canvas.rotate(Math.toDegrees(ang.toDouble()).toFloat(), s.cx, cy)
        var bx = -s.w * 0.6f
        while (bx < s.w * 1.6f) {
            val wob = s.noise.fbm(bx * 0.004f, 3f) * 60f
            s.softGlow(bx, cy + wob, s.f(150f, 230f), mix(c, Color.WHITE, 0.55f), 40)
            bx += 70f
        }
        repeat(s.count(1600)) {
            val x = s.f(-s.w * 0.6f, s.w * 1.6f)
            val y = cy + s.gauss() * s.h * 0.075f
            s.dot(x, y, s.f(0.4f, 1.2f), if (s.chance(0.8f)) Color.WHITE else s.anyColor(), s.i(50, 210))
        }
        s.canvas.drawRect(-s.w, cy - 8f, s.w * 2f, cy + 16f, s.fill(Color.BLACK, 120, 22f))
        s.canvas.restore()
        s.starSky(160, s.h * 0.7f, 5)
        // A shooting star.
        val sx = s.f(s.w * 0.15f, s.w * 0.6f)
        val sy = s.f(s.h * 0.08f, s.h * 0.3f)
        s.canvas.drawLine(sx, sy, sx + 180f, sy + 70f, s.stroke(Color.WHITE, 2.2f, 200, 3f))
        s.canvas.drawLine(sx, sy, sx + 90f, sy + 35f, s.stroke(Color.WHITE, 1.4f))

        // Three bands of trees, the far ones lit faintly by the sky.
        s.treeLine(s.h * 0.66f, s.h * 0.1f, s.h * 0.2f, 46f / s.density.coerceAtLeast(0.5f), mix(darken(c, 0.9f), Color.BLACK, 0.2f))
        s.treeLine(s.h * 0.76f, s.h * 0.12f, s.h * 0.26f, 56f / s.density.coerceAtLeast(0.5f), darken(c, 0.955f))
        s.treeLine(s.h * 0.9f, s.h * 0.14f, s.h * 0.3f, 70f / s.density.coerceAtLeast(0.5f), Color.BLACK)
        repeat(s.count(26)) {
            s.glowDot(s.f(0f, s.w), s.f(s.h * 0.6f, s.h * 0.95f), s.f(1.2f, 2.4f), Color.rgb(255, 238, 150), 8f)
        }
    }
}

// ---------------------------------------------------------------------------------------------

/** The moon over open water, with its glittering road across the swell. */
object MoonPath : Style("moon_path", "Moon Path", Category.NATURE) {
    override val autoPalettes = listOf("mono", "ocean", "gold", "violet", "sunset")

    override fun draw(s: Scene) {
        val tint = s.moonTone()
        val horizon = s.h * s.f(0.5f, 0.58f)
        val mx = s.cx + s.f(-120f, 120f)
        val mr = s.w * s.f(0.1f, 0.16f)
        val my = horizon - s.h * s.f(0.14f, 0.26f)
        s.starSky(220, horizon, 5)
        s.moon(mx, my, mr, tint)
        // A far shore, drawn first so the water's glints sit in front of it.
        val (shore, _) = s.ridge(horizon + 2f, 36f, 0.01f, s.f(0f, 20f), ridged = false)
        s.canvas.drawPath(shore, s.fill(Color.BLACK))
        // The sea is black; the moon's reflection is a column of glints that widens toward us.
        s.canvas.drawRect(0f, horizon + 2f, s.w, s.h, s.fill(Color.BLACK))
        val glints = s.count(620)
        repeat(glints) {
            val t = s.f() * s.f() + 0.01f
            val y = horizon + (s.h - horizon) * t
            val spread = mr * (0.5f + t * 5.5f)
            val x = mx + s.gauss() * spread
            val len = s.f(8f, 46f) * (0.4f + t * 1.5f)
            val a = (200 * (1f - t * 0.55f)).toInt()
            s.canvas.drawLine(x - len, y, x + len, y, s.stroke(lighten(tint, 0.2f), 1.3f + t * 2.6f, a))
        }
        s.canvas.drawLine(0f, horizon, s.w, horizon, s.stroke(withAlpha(tint, 90), 1.4f))
        s.softGlow(mx, horizon, mr * 3f, tint, 60)
    }
}

object NatureWorld {
    val styles: List<Style> = listOf(HowlingWolf, NorthernLights, MoonlitPeaks, StarryPines, MoonPath)
}
