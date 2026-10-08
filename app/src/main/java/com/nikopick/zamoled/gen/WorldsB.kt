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
// ABSTRACT: painterly
// =====================================================================

object StarrySwirl : Style("starry_swirl", "Starry Swirl", Category.ABSTRACT) {
    override val autoPalettes = listOf("ocean", "ocean", "cyan", "violet")

    override fun draw(s: Scene) {
        val vortices = List(s.i(2, 4)) { floatArrayOf(s.f(150f, s.w - 150f), s.f(s.h * 0.12f, s.h * 0.5f), s.f(120f, 220f) * if (s.chance(0.5f)) 1f else -1f) }
        val stars = List(s.i(7, 11)) { floatArrayOf(s.f(40f, s.w - 40f), s.f(s.h * 0.05f, s.h * 0.6f), s.f(10f, 22f)) }
        val moonX = s.w * s.f(0.65f, 0.85f)
        val moonY = s.h * s.f(0.08f, 0.15f)
        val yellow = Color.rgb(255, 214, 90)

        // Sky made of short directional brush strokes.
        val skyBottom = s.h * 0.75f
        repeat(s.count(2600)) {
            val x = s.f(0f, s.w)
            val y = s.f(0f, skyBottom)
            var vx = 1f
            var vy = 0.15f * sin(x * 0.01f)
            for (v in vortices) {
                val dx = x - v[0]
                val dy = y - v[1]
                val d2 = dx * dx + dy * dy + 400f
                val k = v[2] * 900f / d2
                vx += -dy * k / 30f
                vy += dx * k / 30f
            }
            val a = atan2(vy, vx)
            val len = s.f(10f, 22f)
            var nearStar = 0f
            for (st in stars) nearStar = maxOf(nearStar, 1f - (sqrt((x - st[0]) * (x - st[0]) + (y - st[1]) * (y - st[1])) / 110f).coerceIn(0f, 1f))
            val base = mix(s.grad(((x * 0.4f + y) / 900f) % 1f), lighten(s.color(1), 0.2f), s.f(0f, 0.5f))
            val c = mix(base, yellow, nearStar)
            s.canvas.drawLine(x, y, x + cos(a) * len, y + sin(a) * len, s.stroke(c, s.f(3f, 5.5f), s.i(110, 220)))
        }
        // Stars and moon with halo rings.
        for (st in stars) {
            for (k in 4 downTo 1) s.canvas.drawCircle(st[0], st[1], st[2] * (1f + k * 0.7f), s.stroke(mix(yellow, s.color(1), k / 5f), 4f, 120))
            s.canvas.drawCircle(st[0], st[1], st[2], s.fill(lighten(yellow, 0.4f)))
        }
        for (k in 5 downTo 1) s.canvas.drawCircle(moonX, moonY, 44f * (1f + k * 0.45f), s.stroke(yellow, 5f, 90 + k * 10))
        val moon = Path(); moon.addCircle(moonX, moonY, 46f, Path.Direction.CW)
        val bite = Path(); bite.addCircle(moonX + 22f, moonY - 10f, 40f, Path.Direction.CW)
        moon.op(bite, Path.Op.DIFFERENCE)
        s.canvas.drawPath(moon, s.fill(yellow))

        // Village rooftops and a church spire.
        val ground = s.h * 0.82f
        val (land, line) = s.ridge(ground, 70f, 0.003f, 7f, ridged = false, step = 10f)
        s.canvas.drawPath(land, s.fill(Color.BLACK))
        s.canvas.drawPath(line, s.stroke(s.color(0), 3f, 200))
        var hx = s.w * 0.3f
        while (hx < s.w - 40f) {
            val hw = s.f(40f, 70f)
            val hh = s.f(40f, 70f)
            val top = ground + s.f(0f, 30f) - hh
            val house = Path()
            house.moveTo(hx, top + hh); house.lineTo(hx, top); house.lineTo(hx + hw / 2f, top - hh * 0.5f); house.lineTo(hx + hw, top); house.lineTo(hx + hw, top + hh); house.close()
            s.canvas.drawPath(house, s.fill(Color.BLACK))
            s.canvas.drawPath(house, s.stroke(s.color(0), 2f, 220))
            if (s.chance(0.7f)) s.canvas.drawRect(hx + hw * 0.35f, top + hh * 0.3f, hx + hw * 0.55f, top + hh * 0.6f, s.fill(yellow))
            hx += hw + s.f(6f, 20f)
        }
        val chx = s.w * s.f(0.55f, 0.75f)
        val spire = Path()
        spire.moveTo(chx - 12f, ground); spire.lineTo(chx - 12f, ground - 120f); spire.lineTo(chx, ground - 220f); spire.lineTo(chx + 12f, ground - 120f); spire.lineTo(chx + 12f, ground); spire.close()
        s.canvas.drawPath(spire, s.fill(Color.BLACK))
        s.canvas.drawPath(spire, s.stroke(s.color(0), 2f))

        // A flame-shaped cypress in the foreground.
        val cx = s.w * s.f(0.08f, 0.22f)
        val cypress = Path()
        cypress.moveTo(cx - 70f, s.h + 10f)
        cypress.cubicTo(cx - 120f, s.h * 0.6f, cx - 40f, s.h * 0.4f, cx + 10f, s.h * 0.22f)
        cypress.cubicTo(cx + 40f, s.h * 0.42f, cx + 110f, s.h * 0.62f, cx + 70f, s.h + 10f)
        cypress.close()
        s.canvas.drawPath(cypress, s.fill(Color.BLACK))
        s.canvas.save()
        s.canvas.clipPath(cypress)
        repeat(160) {
            val yy = s.f(s.h * 0.22f, s.h)
            val xx = cx + s.f(-90f, 90f)
            val stroke = Path()
            stroke.moveTo(xx, yy); stroke.quadTo(xx + 12f, yy - 30f, xx + s.f(-6f, 6f), yy - 60f)
            s.canvas.drawPath(stroke, s.stroke(darken(mix(s.color(2), Color.rgb(60, 140, 90), 0.5f), 0.3f), 4f, 200))
        }
        s.canvas.restore()
    }
}

object LiquidMarble : Style("liquid_marble", "Liquid Marble", Category.ABSTRACT) {
    override val autoPalettes = listOf("gold", "sakura", "ocean", "violet", "candy", "hue")

    override fun draw(s: Scene) {
        val step = 7f
        val nx = (s.w / step).toInt() + 2
        val ny = (s.h / step).toInt() + 2
        val sc = s.f(0.0012f, 0.002f)
        val warp = s.f(2.5f, 4.5f)
        val v = Array(ny) { j ->
            FloatArray(nx) { i ->
                val x = i * step * sc
                val y = j * step * sc
                val qx = s.noise.fbm(x, y, 3)
                val qy = s.noise.fbm(x + 5.2f, y + 1.3f, 3)
                s.noise.fbm(x + warp * qx, y + warp * qy, 3)
            }
        }
        val levels = 18
        val gold = Color.rgb(255, 214, 120)
        for (l in 0 until levels) {
            val level = -0.6f + 1.2f * l / (levels - 1f)
            val segs = ArrayList<Float>()
            for (j in 0 until ny - 1) for (i in 0 until nx - 1) {
                val a = v[j][i]; val b = v[j][i + 1]; val c = v[j + 1][i + 1]; val d = v[j + 1][i]
                var idx = 0
                if (a > level) idx = idx or 8
                if (b > level) idx = idx or 4
                if (c > level) idx = idx or 2
                if (d > level) idx = idx or 1
                if (idx == 0 || idx == 15) continue
                val x0 = i * step; val y0 = j * step
                fun t(p: Float, q: Float) = ((level - p) / (q - p)).coerceIn(0f, 1f)
                val top = floatArrayOf(x0 + step * t(a, b), y0)
                val right = floatArrayOf(x0 + step, y0 + step * t(b, c))
                val bottom = floatArrayOf(x0 + step * t(d, c), y0 + step)
                val left = floatArrayOf(x0, y0 + step * t(a, d))
                fun seg(p: FloatArray, q: FloatArray) { segs.add(p[0]); segs.add(p[1]); segs.add(q[0]); segs.add(q[1]) }
                when (idx) {
                    1, 14 -> seg(left, bottom)
                    2, 13 -> seg(bottom, right)
                    3, 12 -> seg(left, right)
                    4, 11 -> seg(top, right)
                    6, 9 -> seg(top, bottom)
                    7, 8 -> seg(left, top)
                    5 -> { seg(left, top); seg(bottom, right) }
                    10 -> { seg(top, right); seg(left, bottom) }
                }
            }
            val arr = segs.toFloatArray()
            if (l % 5 == 2) {
                s.canvas.drawLines(arr, s.stroke(gold, 4f, 90, 6f))
                s.canvas.drawLines(arr, s.stroke(gold, 1.8f))
            } else {
                s.canvas.drawLines(arr, s.stroke(s.grad((l % 7) / 6f), if (l % 2 == 0) 3.2f else 1.4f, 215))
            }
        }
    }
}

object InkSmoke : Style("ink_smoke", "Ink Smoke", Category.ABSTRACT) {
    override val autoPalettes = listOf("hue", "graffiti", "violet", "sunset", "aurora", "cyan")

    override fun draw(s: Scene) {
        val plumes = s.i(2, 4)
        for (pl in 0 until plumes) {
            val sx = s.f(s.w * 0.2f, s.w * 0.8f)
            val sy = s.h * s.f(0.88f, 1.02f)
            val color = s.color(pl)
            val off = s.f(0f, 100f)
            s.softGlow(sx, sy, 260f, color, 80)
            repeat(s.count(520)) {
                var x = sx + s.gauss() * 55f
                var y = sy + s.gauss() * 10f
                val p = Path()
                p.moveTo(x, y)
                val drift = s.f(-0.6f, 0.6f)
                for (k in 0 until 240) {
                    val rise = k / 240f
                    val n = s.noise.fbm(x * 0.0042f + off, y * 0.0042f + off * 0.5f, 3)
                    val a = -PI.toFloat() / 2f + n * 3.4f * (0.35f + rise) + drift * rise
                    x += cos(a) * 5f
                    y += sin(a) * 5f
                    p.lineTo(x, y)
                }
                s.canvas.drawPath(p, s.stroke(mix(color, Color.WHITE, s.f(0f, 0.35f)), s.f(0.8f, 1.9f), s.i(14, 36)).apply { blendMode = BlendMode.PLUS })
            }
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

object NeonSign : Style("neon_sign", "Neon Sign", Category.NEON) {
    override val autoPalettes = listOf("graffiti", "cyan", "sakura", "lava", "acid", "violet")
    private val words = arrayOf("open late", "stay weird", "good vibes", "dream", "24/7", "night owl", "miao", "play loud")

    override fun draw(s: Scene) {
        s.bricks(40)
        val c1 = s.color(0)
        val c2 = s.color(1)
        val cx = s.cx
        val cy = s.h * s.f(0.36f, 0.44f)
        s.softGlow(cx, cy, 520f, c1, 50)
        // Mounting wires.
        s.canvas.drawLine(cx - 180f, 0f, cx - 180f, cy - 260f, s.stroke(Color.rgb(120, 120, 130), 1.6f, 150))
        s.canvas.drawLine(cx + 180f, 0f, cx + 180f, cy - 260f, s.stroke(Color.rgb(120, 120, 130), 1.6f, 150))
        val p = Path()
        when (s.i(0, 5)) {
            0 -> { // cocktail glass with olive
                p.moveTo(cx - 170f, cy - 190f); p.lineTo(cx + 170f, cy - 190f); p.lineTo(cx, cy); p.close()
                p.moveTo(cx, cy); p.lineTo(cx, cy + 160f)
                p.moveTo(cx - 90f, cy + 170f); p.lineTo(cx + 90f, cy + 170f)
                s.neonPath(p, c1)
                val o = Path(); o.addCircle(cx + 60f, cy - 140f, 26f, Path.Direction.CW); o.moveTo(cx + 30f, cy - 220f); o.lineTo(cx + 80f, cy - 110f)
                s.neonPath(o, c2)
            }
            1 -> { // sitting cat
                p.moveTo(cx - 90f, cy + 160f)
                p.cubicTo(cx - 160f, cy + 40f, cx - 120f, cy - 80f, cx - 70f, cy - 110f)
                p.lineTo(cx - 90f, cy - 210f); p.lineTo(cx - 30f, cy - 150f); p.lineTo(cx + 30f, cy - 150f); p.lineTo(cx + 90f, cy - 210f); p.lineTo(cx + 70f, cy - 110f)
                p.cubicTo(cx + 120f, cy - 80f, cx + 160f, cy + 40f, cx + 90f, cy + 160f)
                p.close()
                p.moveTo(cx + 90f, cy + 150f); p.cubicTo(cx + 220f, cy + 150f, cx + 220f, cy, cx + 170f, cy - 40f)
                s.neonPath(p, c1)
                val eyes = Path(); eyes.addCircle(cx - 35f, cy - 90f, 10f, Path.Direction.CW); eyes.addCircle(cx + 35f, cy - 90f, 10f, Path.Direction.CW)
                s.neonPath(eyes, c2, 3f)
            }
            2 -> { // palm tree and sun
                val sun = Path(); sun.addCircle(cx + 40f, cy - 40f, 130f, Path.Direction.CW)
                s.neonPath(sun, c2, 3f)
                p.moveTo(cx - 40f, cy + 170f); p.quadTo(cx - 10f, cy, cx - 60f, cy - 150f)
                for (k in 0 until 5) {
                    val a = PI.toFloat() + k * PI.toFloat() / 4f
                    p.moveTo(cx - 60f, cy - 150f)
                    p.quadTo(cx - 60f + cos(a - 0.3f) * 90f, cy - 150f + sin(a - 0.3f) * 90f - 30f, cx - 60f + cos(a) * 170f, cy - 150f + sin(a) * 120f + 40f)
                }
                s.neonPath(p, c1)
            }
            3 -> { // heart pierced by an arrow
                p.moveTo(cx, cy + 150f)
                p.cubicTo(cx - 260f, cy - 20f, cx - 130f, cy - 230f, cx, cy - 100f)
                p.cubicTo(cx + 130f, cy - 230f, cx + 260f, cy - 20f, cx, cy + 150f)
                p.close()
                s.neonPath(p, c1)
                val arrow = Path()
                arrow.moveTo(cx - 230f, cy + 80f); arrow.lineTo(cx + 230f, cy - 160f)
                arrow.moveTo(cx + 230f, cy - 160f); arrow.lineTo(cx + 190f, cy - 160f)
                arrow.moveTo(cx + 230f, cy - 160f); arrow.lineTo(cx + 215f, cy - 125f)
                s.neonPath(arrow, c2, 3f)
            }
            else -> { // ringed planet
                p.addCircle(cx, cy - 30f, 120f, Path.Direction.CW)
                s.neonPath(p, c1)
                val ring = Path()
                s.canvas.save(); s.canvas.rotate(-18f, cx, cy - 30f)
                ring.addOval(RectF(cx - 230f, cy - 80f, cx + 230f, cy + 20f), Path.Direction.CW)
                s.neonPath(ring, c2, 3f)
                s.canvas.restore()
            }
        }
        val word = words[s.i(0, words.size)]
        s.neonText(word, cx, cy + 340f, 120f, c2, neonScript())
        // Reflections on the wet street.
        val street = s.h * 0.86f
        s.canvas.drawLine(0f, street, s.w, street, s.stroke(Color.rgb(90, 90, 100), 1.4f, 160))
        repeat(40) {
            val y = street + s.f(10f, s.h - street)
            val x = cx + s.gauss() * 200f
            s.canvas.drawLine(x - s.f(10f, 60f), y, x + s.f(10f, 60f), y, s.stroke(if (s.chance(0.5f)) c1 else c2, 2f, s.i(40, 140)))
        }
    }
}

object NeonAlley : Style("neon_alley", "Neon Alley", Category.NEON) {
    override val autoPalettes = listOf("graffiti", "cyan", "sakura", "violet", "lava")
    private val signs = arrayOf("ラーメン", "バー", "カラオケ", "寿司", "ホテル", "夜", "酒", "OPEN", "喫茶")

    override fun draw(s: Scene) {
        val vx = s.cx + s.f(-60f, 60f)
        val vy = s.h * s.f(0.36f, 0.44f)
        val ground = s.h * 0.78f
        val wallP = s.stroke(Color.rgb(110, 110, 125), 1.6f, 170)
        // Walls converging on the vanishing point.
        for (side in 0..1) {
            val x0 = if (side == 0) 0f else s.w
            val near = if (side == 0) s.w * 0.28f else s.w * 0.72f
            s.canvas.drawLine(near, 0f, near, ground + 60f, wallP)
            for (k in 0..8) {
                val y = k * s.h / 8f
                val t = 0.55f
                s.canvas.drawLine(x0, y, x0 + (vx - x0) * t, y + (vy - y) * t, s.stroke(Color.rgb(80, 80, 95), 1f, 70))
            }
        }
        // Cables strung across.
        repeat(s.i(4, 8)) {
            val y1 = s.f(s.h * 0.05f, vy - 60f)
            val y2 = y1 + s.f(-80f, 80f)
            val cable = Path()
            cable.moveTo(s.w * 0.28f, y1); cable.quadTo(s.cx, (y1 + y2) / 2f + s.f(40f, 120f), s.w * 0.72f, y2)
            s.canvas.drawPath(cable, s.stroke(Color.rgb(140, 140, 150), 1.6f, 170))
        }
        // Vertical signs on both walls.
        val placed = ArrayList<Triple<Float, Float, Int>>()
        for (side in 0..1) {
            var y = s.f(60f, 160f)
            while (y < ground - 200f) {
                val text = signs[s.i(0, signs.size)]
                val vertical = text.all { it.code > 0x3000 } && text.length > 1
                val color = s.anyColor()
                val sz = s.f(46f, 68f)
                val bw = if (vertical) sz * 1.4f else sz * text.length * 0.7f + 30f
                val bh = if (vertical) sz * text.length * 1.1f + 30f else sz * 1.6f
                val x = if (side == 0) s.w * 0.28f - bw - s.f(10f, 30f) else s.w * 0.72f + s.f(10f, 30f)
                val box = RectF(x, y, x + bw, y + bh)
                val frame = Path(); frame.addRoundRect(box, 10f, 10f, Path.Direction.CW)
                s.canvas.drawRoundRect(box, 10f, 10f, s.fill(darken(color, 0.85f)))
                s.neonPath(frame, color, 2.4f)
                if (vertical) {
                    text.forEachIndexed { i, ch -> s.neonText(ch.toString(), box.centerX(), box.top + 20f + sz * (i + 0.85f) * 1.1f, sz, color, Typeface.DEFAULT_BOLD, flicker = s.chance(0.06f)) }
                } else {
                    s.neonText(text, box.centerX(), box.centerY() + sz * 0.35f, sz, color, Typeface.DEFAULT_BOLD)
                }
                placed.add(Triple(box.centerX(), box.centerY(), color))
                y += bh + s.f(30f, 90f)
            }
        }
        // Wet street with reflections and rain.
        s.canvas.drawLine(0f, ground, s.w, ground, s.stroke(Color.rgb(100, 100, 110), 1.6f, 180))
        for ((px, py, color) in placed) {
            val ry = ground + (ground - py) * 0.3f
            if (ry > s.h) continue
            repeat(6) {
                val y = ry + s.f(-40f, 40f)
                s.canvas.drawLine(px - s.f(20f, 70f), y, px + s.f(20f, 70f), y, s.stroke(color, 2.4f, s.i(40, 140)))
            }
        }
        repeat(s.count(200)) {
            val x = s.f(0f, s.w)
            val y = s.f(0f, s.h)
            s.canvas.drawLine(x, y, x - 4f, y + s.f(18f, 40f), s.stroke(Color.WHITE, 1f, s.i(30, 90)))
        }
    }
}
