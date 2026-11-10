package com.nikopick.zamoled.gen

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Picture
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// =====================================================================
// NATURE: moonlit wilderness
// =====================================================================

private fun Scene.pine(x: Float, base: Float, height: Float, fill: Int, edge: Int, edgeAlpha: Int) {
    val tiers = 5
    val p = Path()
    p.moveTo(x, base - height)
    for (t in 1..tiers) {
        val y = base - height + height * t / tiers.toFloat() * 0.92f
        val half = height * 0.08f + height * 0.3f * t / tiers
        p.lineTo(x + half, y)
        p.lineTo(x + half * 0.45f, y - height * 0.03f)
    }
    p.lineTo(x + height * 0.04f, base)
    p.lineTo(x - height * 0.04f, base)
    for (t in tiers downTo 1) {
        val y = base - height + height * t / tiers.toFloat() * 0.92f
        val half = height * 0.08f + height * 0.3f * t / tiers
        p.lineTo(x - half * 0.45f, y - height * 0.03f)
        p.lineTo(x - half, y)
    }
    p.close()
    canvas.drawPath(p, fill(fill))
    canvas.drawPath(p, stroke(edge, 1.4f, edgeAlpha))
}

/** Howling wolf in profile facing left, in a 100-unit box (y down). Doubled points stay sharp. */
private val WOLF = floatArrayOf(
    9f, 7f, 9f, 7f, 15f, 3f, 22f, 8f, 26f, 5f, 28f, -5f, 28f, -5f, 33f, 6f, 38f, 12f, 46f, 26f, 58f, 40f, 72f, 44f, 90f, 44f,
    102f, 48f, 112f, 58f, 121f, 76f, 125f, 93f, 125f, 93f, 116f, 84f, 108f, 70f, 104f, 66f, 108f, 82f, 106f, 96f, 111f, 100f, 111f, 100f,
    92f, 100f, 92f, 100f, 94f, 92f, 92f, 80f, 84f, 70f, 62f, 72f, 56f, 82f, 56f, 100f, 56f, 100f, 42f, 100f, 42f, 100f, 44f, 86f,
    42f, 70f, 38f, 52f, 32f, 40f, 24f, 30f, 18f, 22f, 13f, 15f,
)

object MoonlitForest : Style("moonlit_forest", "Howling Woods", Category.NATURE) {
    override val autoPalettes = listOf("ocean", "aurora", "cyan", "violet", "mono")

    override fun draw(s: Scene) {
        s.starSky(220, s.h * 0.5f)
        val moon = lighten(s.color(0), 0.6f)
        val mx = s.w * s.f(0.4f, 0.6f)
        val my = s.h * s.f(0.36f, 0.4f)
        val mr = s.f(180f, 210f)
        s.softGlow(mx, my, mr * 3.2f, s.color(0), 100)
        s.canvas.drawCircle(mx, my, mr, s.fill(moon))
        repeat(7) { s.canvas.drawCircle(mx + s.f(-mr * 0.6f, mr * 0.6f), my + s.f(-mr * 0.6f, mr * 0.6f), s.f(8f, 24f), s.fill(darken(moon, 0.1f))) }

        // Forest layers from far to near, with mist between them.
        for (layer in 0 until 4) {
            val t = layer / 3f
            val base = s.h * (0.56f + layer * 0.07f)
            val fill = mix(darken(s.color(1), 0.85f), Color.BLACK, t)
            val edgeAlpha = (190 * (1f - t * 0.55f)).toInt()
            var x = -40f
            while (x < s.w + 40f) {
                val hgt = s.f(150f, 300f) * (1f + layer * 0.3f)
                s.pine(x, base + s.f(-14f, 14f), hgt, fill, s.color(0), edgeAlpha)
                x += s.f(34f, 70f) * (1f + layer * 0.3f)
            }
            s.canvas.drawRect(0f, base - 70f, s.w, base + 40f, s.vGradient(base - 70f, base + 40f, Color.TRANSPARENT, withAlpha(s.color(0), 30)))
        }

        // Rock crowned by a howling wolf whose head reaches into the moon.
        val rx = mx + s.f(-50f, 50f)
        val peak = my + mr * 1.02f
        val rock = Path()
        rock.moveTo(rx - 420f, s.h + 10f)
        rock.lineTo(rx - 300f, peak + 360f); rock.lineTo(rx - 170f, peak + 140f); rock.lineTo(rx - 90f, peak + 14f)
        rock.lineTo(rx - 30f, peak); rock.lineTo(rx + 120f, peak + 10f); rock.lineTo(rx + 200f, peak + 170f)
        rock.lineTo(rx + 300f, peak + 380f); rock.lineTo(rx + 420f, s.h + 10f); rock.close()
        s.canvas.drawPath(rock, s.fill(Color.BLACK))
        s.canvas.drawPath(rock, s.stroke(lighten(s.color(0), 0.3f), 2f, 150))

        val size = 330f
        val scale = size / 100f
        val ox = rx - 30f - 12f * scale
        val oy = peak - 100f * scale
        val pts = FloatArray(WOLF.size)
        for (i in WOLF.indices step 2) { pts[i] = ox + WOLF[i] * scale; pts[i + 1] = oy + WOLF[i + 1] * scale }
        val wolf = smoothPath(pts, closed = true, tension = 0.5f)
        s.canvas.drawPath(wolf, s.fill(Color.BLACK))
        s.canvas.drawPath(wolf, s.stroke(lighten(s.color(0), 0.2f), 1.8f, 120))
        // Breath rings drifting from the muzzle.
        val nx = ox + 9f * scale
        val ny = oy + 7f * scale
        for (k in 1..3) s.canvas.drawArc(RectF(nx - k * 30f - 30f, ny - k * 30f - 30f, nx - k * 30f + 30f, ny - k * 30f + 30f), 150f, 100f, false, s.stroke(Color.BLACK, 2.4f, 190 - k * 40))
    }
}

object MountainLake : Style("mountain_lake", "Mountain Lake", Category.NATURE) {
    override val autoPalettes = listOf("ocean", "sunset", "aurora", "violet", "gold")

    override fun draw(s: Scene) {
        val water = s.h * s.f(0.58f, 0.64f)
        // Paint the scenery once into a Picture so the reflection matches it exactly.
        val picture = Picture()
        val rec = Scene(picture.beginRecording(s.w.toInt() + 1, s.h.toInt() + 1), s.w, s.h, s.rnd, s.palette, s.density, s.noise)
        run {
            val s = rec
            s.starSky(160, water)
            val mx = s.w * s.f(0.2f, 0.8f)
            val my = water * s.f(0.25f, 0.4f)
            s.softGlow(mx, my, 260f, s.color(0), 90)
            s.canvas.drawCircle(mx, my, 60f, s.fill(lighten(s.color(0), 0.6f)))
            for (layer in 0..1) {
                val (land, line) = s.ridge(water, s.f(380f, 520f) * (1f - layer * 0.35f), 0.0028f * (1f + layer), 4f + layer * 9f)
                val tone = if (layer == 0) darken(s.color(1), 0.75f) else Color.BLACK
                s.canvas.drawPath(land, s.fill(tone))
                // Snow caps: the land clipped to its upper part.
                s.canvas.save()
                s.canvas.clipPath(land)
                val snowY = water - s.f(260f, 330f) * (1f - layer * 0.4f)
                s.canvas.drawRect(0f, 0f, s.w, snowY, s.vGradient(snowY - 220f, snowY, withAlpha(Color.WHITE, 220), withAlpha(Color.WHITE, 0)))
                s.canvas.restore()
                s.canvas.drawPath(line, s.stroke(lighten(s.color(0), 0.5f), 2f, if (layer == 0) 130 else 220))
            }
            var x = -20f
            while (x < s.w + 20f) {
                s.pine(x, water + 2f, s.f(40f, 90f), Color.BLACK, s.color(0), 160)
                x += s.f(16f, 40f)
            }
        }
        picture.endRecording()
        s.canvas.save()
        s.canvas.clipRect(0f, 0f, s.w, water)
        s.canvas.drawPicture(picture)
        s.canvas.restore()
        // Mirror image in the lake, broken up by ripples.
        s.canvas.save()
        s.canvas.clipRect(0f, water, s.w, s.h)
        s.canvas.scale(1f, -1f, 0f, water)
        s.canvas.drawPicture(picture)
        s.canvas.restore()
        s.canvas.drawRect(0f, water, s.w, s.h, s.vGradient(water, s.h, withAlpha(Color.BLACK, 90), withAlpha(Color.BLACK, 230)))
        var ry = water + 4f
        while (ry < s.h) {
            var rx = s.f(-40f, 0f)
            while (rx < s.w) {
                val len = s.f(20f, 120f)
                s.canvas.drawLine(rx, ry, rx + len, ry, s.stroke(Color.BLACK, 2.2f, 200))
                rx += len + s.f(30f, 160f)
            }
            ry += 5f + (ry - water) * 0.02f
        }
        s.canvas.drawLine(0f, water, s.w, water, s.stroke(lighten(s.color(0), 0.4f), 1.4f, 160))
        // A canoe with a paddler.
        val cx = s.w * s.f(0.25f, 0.7f)
        val cy = water + s.f(160f, 260f)
        val boat = Path()
        boat.moveTo(cx - 90f, cy); boat.quadTo(cx, cy + 26f, cx + 90f, cy); boat.close()
        s.canvas.drawPath(boat, s.fill(Color.BLACK))
        s.canvas.drawPath(boat, s.stroke(lighten(s.color(0), 0.5f), 2f))
        s.canvas.drawCircle(cx, cy - 38f, 9f, s.stroke(lighten(s.color(0), 0.5f), 2f))
        s.canvas.drawLine(cx, cy - 28f, cx, cy - 2f, s.stroke(lighten(s.color(0), 0.5f), 3f))
        s.canvas.drawLine(cx - 40f, cy - 40f, cx + 30f, cy + 20f, s.stroke(lighten(s.color(0), 0.5f), 2f))
        for (k in 1..3) s.canvas.drawOval(RectF(cx - 90f - k * 40f, cy + 6f - k * 4f, cx + 90f + k * 40f, cy + 10f + k * 4f), s.stroke(lighten(s.color(0), 0.4f), 1.2f, 140 - k * 35))
    }
}

object NightBloom : Style("night_bloom", "Night Bloom", Category.NATURE) {
    override val autoPalettes = listOf("sakura", "candy", "aurora", "violet", "sunset")

    override fun draw(s: Scene) {
        val leafEdge = mix(s.color(2), Color.rgb(120, 220, 150), 0.5f)
        // Monstera leaves reaching in from the edges.
        repeat(s.i(4, 7)) {
            val side = if (it % 2 == 0) 0f else s.w
            monstera(s, side + s.f(-60f, 60f), s.f(0f, s.h), s.f(200f, 330f), if (side == 0f) s.f(-40f, 40f) else s.f(140f, 220f), leafEdge)
        }
        repeat(s.i(3, 6)) { fern(s, s.f(0f, s.w), s.h + 20f, -PI.toFloat() / 2f + s.f(-0.5f, 0.5f), s.f(360f, 620f), leafEdge, 2) }
        // Glowing flowers.
        repeat(s.i(3, 6)) {
            flower(s, s.f(140f, s.w - 140f), s.f(s.h * 0.2f, s.h * 0.8f), s.f(70f, 140f), s.color(0), s.color(1))
        }
        repeat(s.count(80)) { s.glowDot(s.f(0f, s.w), s.f(0f, s.h), s.f(1f, 2.6f), lighten(s.anyColor(), 0.4f), 6f, s.i(100, 240)) }
    }

    private fun monstera(s: Scene, x: Float, y: Float, size: Float, rot: Float, edge: Int) {
        s.canvas.save()
        s.canvas.rotate(rot, x, y)
        // Heart-shaped blade pointing along +x from the stem at (x, y).
        val blade = Path()
        blade.moveTo(x, y)
        blade.cubicTo(x + size * 0.05f, y - size * 0.55f, x + size * 0.55f, y - size * 0.75f, x + size * 0.85f, y - size * 0.3f)
        blade.cubicTo(x + size * 1.0f, y - size * 0.12f, x + size * 1.1f, y - size * 0.03f, x + size * 1.2f, y)
        blade.cubicTo(x + size * 1.1f, y + size * 0.03f, x + size * 1.0f, y + size * 0.12f, x + size * 0.85f, y + size * 0.3f)
        blade.cubicTo(x + size * 0.55f, y + size * 0.75f, x + size * 0.05f, y + size * 0.55f, x, y)
        blade.close()
        s.canvas.drawPath(blade, s.fill(darken(edge, 0.8f)))
        s.canvas.drawPath(blade, s.stroke(edge, 2.6f, 230))
        // Splits: black wedges from the margin toward the midrib.
        for (k in 0 until 4) {
            val t = 0.22f + k * 0.17f
            for (sgn in floatArrayOf(-1f, 1f)) {
                val ex = x + size * (t + 0.12f)
                val ey = y + sgn * size * (0.62f - k * 0.1f)
                val wedge = Path()
                wedge.moveTo(ex - size * 0.03f, ey)
                wedge.lineTo(x + size * (t + 0.02f), y + sgn * size * 0.1f)
                wedge.lineTo(ex + size * 0.05f, ey - sgn * size * 0.02f)
                wedge.close()
                s.canvas.drawPath(wedge, s.fill(Color.BLACK))
                s.canvas.drawPath(wedge, s.stroke(edge, 1.4f, 160))
            }
        }
        s.canvas.drawLine(x, y, x + size * 1.15f, y, s.stroke(edge, 3f, 230))
        s.canvas.restore()
    }

    private fun fern(s: Scene, x: Float, y: Float, angle: Float, len: Float, edge: Int, depth: Int) {
        val steps = 18
        var px = x
        var py = y
        var a = angle
        val curl = s.f(-0.03f, 0.03f)
        val stem = Path()
        stem.moveTo(px, py)
        for (i in 0 until steps) {
            val seg = len / steps
            px += cos(a) * seg
            py += sin(a) * seg
            a += curl
            stem.lineTo(px, py)
            val leafLen = len * 0.2f * (1f - i / steps.toFloat())
            if (leafLen > 6f) {
                for (sgn in floatArrayOf(-1f, 1f)) {
                    val la = a + sgn * 1.1f
                    if (depth > 1 && i % 3 == 0) fern(s, px, py, la, leafLen * 2.2f, edge, depth - 1)
                    else s.canvas.drawLine(px, py, px + cos(la) * leafLen, py + sin(la) * leafLen, s.stroke(edge, 1.6f, 170))
                }
            }
        }
        s.canvas.drawPath(stem, s.stroke(edge, 2f, 200))
    }

    private fun flower(s: Scene, x: Float, y: Float, r: Float, c1: Int, c2: Int) {
        s.softGlow(x, y, r * 2.4f, c1, 70)
        val petals = s.i(6, 10)
        val rot = s.f(0f, 360f)
        for (layer in 0..1) for (k in 0 until petals) {
            s.canvas.save()
            s.canvas.rotate(rot + k * 360f / petals + layer * 180f / petals, x, y)
            val len = r * (1f - layer * 0.35f)
            val petal = Path()
            petal.moveTo(x, y)
            petal.cubicTo(x - len * 0.45f, y - len * 0.4f, x - len * 0.3f, y - len * 0.95f, x, y - len)
            petal.cubicTo(x + len * 0.3f, y - len * 0.95f, x + len * 0.45f, y - len * 0.4f, x, y)
            s.canvas.drawPath(petal, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(x, y, len, intArrayOf(lighten(c1, 0.5f), c1, darken(c2, 0.4f)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            })
            s.canvas.drawPath(petal, s.stroke(lighten(c1, 0.6f), 1.2f, 160))
            s.canvas.restore()
        }
        repeat(14) {
            val a = s.f(0f, TAU)
            val d = s.f(0f, r * 0.22f)
            s.glowDot(x + cos(a) * d, y + sin(a) * d, 2.2f, Color.rgb(255, 230, 140), 5f)
        }
    }
}
// =====================================================================
// NEON: nightlife signage
// =====================================================================

private fun Scene.bricks(alpha: Int) {
    val p = stroke(Color.rgb(90, 70, 70), 1.4f, alpha)
    var y = 0f
    var row = 0
    while (y < h) {
        canvas.drawLine(0f, y, w, y, p)
        var x = if (row % 2 == 0) 0f else 45f
        while (x < w) { canvas.drawLine(x, y, x, y + 40f, p); x += 90f }
        y += 40f; row++
    }
}

private fun neonScript(): Typeface = Typeface.create("cursive", Typeface.BOLD)
