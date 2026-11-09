package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The Mystic world: occult circles, star charts, tarot, crystals and the moon's phases, drawn as
 * fine glowing line on black.
 */

/** Appends one invented rune to [path]: a stem with a few branches, a different one each call. */
private fun Path.rune(s: Scene, cx: Float, cy: Float, size: Float, rot: Float) {
    fun pt(x: Float, y: Float): FloatArray {
        val c = cos(rot); val sn = sin(rot)
        return floatArrayOf(cx + (x * c - y * sn) * size, cy + (x * sn + y * c) * size)
    }
    val top = pt(0f, -1f)
    val bot = pt(0f, 1f)
    moveTo(top[0], top[1]); lineTo(bot[0], bot[1])
    repeat(s.i(1, 4)) {
        val fromY = s.f(-0.9f, 0.5f)
        val side = if (s.chance(0.5f)) 1f else -1f
        val a = pt(0f, fromY)
        val b = pt(side * s.f(0.4f, 0.8f), fromY + s.f(-0.7f, 0.7f))
        moveTo(a[0], a[1]); lineTo(b[0], b[1])
        if (s.chance(0.3f)) { val c = pt(side * s.f(0.2f, 0.5f), s.f(0.6f, 1f)); lineTo(c[0], c[1]) }
    }
}

/** A circle drawn at [r] around (cx, cy). */
private fun Path.ring(cx: Float, cy: Float, r: Float) = addCircle(cx, cy, r, Path.Direction.CW)

/** A star polygon {points/step}: each vertex joined to the one [step] along. */
private fun Path.starPoly(cx: Float, cy: Float, r: Float, points: Int, step: Int, rot: Float) {
    for (k in 0 until points) {
        val a1 = rot + TAU * k / points
        val a2 = rot + TAU * ((k + step) % points) / points
        moveTo(cx + cos(a1) * r, cy + sin(a1) * r)
        lineTo(cx + cos(a2) * r, cy + sin(a2) * r)
    }
}

// ---------------------------------------------------------------------------------------------

/** A summoning circle: runes between double rings, a star polygon, and a sigil at its heart. */
object ArcaneCircle : Style("arcane_circle", "Arcane Circle", Category.MYSTIC) {
    override val autoPalettes = listOf("gold", "violet", "lava", "aurora", "cyan", "sakura")

    override fun draw(s: Scene) {
        val c0 = s.color(0)
        val c1 = s.color(1)
        val c2 = s.color(2)
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.5f)
        val r = s.w * s.f(0.4f, 0.44f)
        val rot = s.f(0f, TAU)
        s.starSky(120, s.h, 4)
        s.softGlow(cx, cy, r * 1.6f, c1, 60)

        // Outer rings with a ring of tick marks between them.
        s.neonPath(Path().apply { ring(cx, cy, r); ring(cx, cy, r * 0.93f) }, c0, 2.2f)
        val ticks = Path()
        for (k in 0 until s.count(96)) {
            val a = TAU * k / s.count(96)
            val long = k % 4 == 0
            val r0 = r * 0.93f
            val r1 = r * (if (long) 0.985f else 0.96f)
            ticks.moveTo(cx + cos(a) * r0, cy + sin(a) * r0); ticks.lineTo(cx + cos(a) * r1, cy + sin(a) * r1)
        }
        s.canvas.drawPath(ticks, s.stroke(c0, 1.4f, 190))

        // The rune band.
        val bandR = r * 0.83f
        s.neonPath(Path().apply { ring(cx, cy, r * 0.76f) }, c1, 1.8f, dim = true)
        val runes = Path()
        val n = s.count(34)
        for (k in 0 until n) {
            val a = TAU * k / n + rot
            runes.rune(s, cx + cos(a) * bandR, cy + sin(a) * bandR, r * 0.045f, a + (PI / 2).toFloat())
        }
        s.neonPath(runes, c2, 2f)

        // The star polygon and the circles at its points.
        val points = intArrayOf(5, 6, 7, 8, 9)[s.i(0, 5)]
        val step = if (points >= 7 && s.chance(0.6f)) 3 else 2
        val sr = r * 0.74f
        s.neonPath(Path().apply { starPoly(cx, cy, sr, points, step, rot - (PI / 2).toFloat()) }, c0, 2.6f)
        val nodes = Path()
        for (k in 0 until points) {
            val a = rot - (PI / 2).toFloat() + TAU * k / points
            nodes.ring(cx + cos(a) * sr, cy + sin(a) * sr, r * 0.045f)
        }
        s.neonPath(nodes, c1, 2f)
        // Inner rings and the sigil.
        s.neonPath(Path().apply { ring(cx, cy, r * 0.42f); ring(cx, cy, r * 0.36f) }, c2, 2f, dim = true)
        s.neonPath(Path().apply { starPoly(cx, cy, r * 0.34f, 6, 2, rot) }, c1, 2f, dim = true)
        val sigil = Path()
        sigil.rune(s, cx, cy, r * 0.2f, s.f(-0.3f, 0.3f))
        sigil.rune(s, cx, cy, r * 0.13f, s.f(0f, TAU))
        s.neonPath(sigil, lighten(c0, 0.3f), 3.4f)
        // Embers lifting off the circle.
        repeat(s.count(40)) {
            val a = s.f(0f, TAU)
            val d = s.f(0.2f, 1.1f) * r
            s.glowDot(cx + cos(a) * d, cy + sin(a) * d - s.f(0f, 60f), s.f(1f, 2.6f), if (s.chance(0.5f)) c0 else c1, 9f)
        }
    }
}

// ---------------------------------------------------------------------------------------------

/** An astrolabe: a wheel of twelve constellations, degree scales and a sun at its centre. */
object ZodiacWheel : Style("zodiac_wheel", "Zodiac Wheel", Category.MYSTIC) {
    override val autoPalettes = listOf("gold", "violet", "aurora", "ocean", "sakura", "cyan")

    override fun draw(s: Scene) {
        val c0 = s.color(0)
        val c1 = s.color(1)
        val c2 = s.color(2)
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.5f)
        val r = s.w * s.f(0.43f, 0.47f)
        s.starSky(160, s.h, 5)
        s.softGlow(cx, cy, r * 1.5f, c1, 50)
        // Degree scale: a tick every 5 degrees, a long one every 30.
        val scale = Path()
        for (d in 0 until 360 step 2) {
            val a = d * PI.toFloat() / 180f
            val len = if (d % 30 == 0) 0.075f else if (d % 10 == 0) 0.045f else 0.025f
            scale.moveTo(cx + cos(a) * r, cy + sin(a) * r)
            scale.lineTo(cx + cos(a) * r * (1f - len), cy + sin(a) * r * (1f - len))
        }
        s.neonPath(Path().apply { ring(cx, cy, r) }, c0, 2.2f)
        s.canvas.drawPath(scale, s.stroke(c0, 1.3f, 200))
        // Twelve houses divided by spokes, each with a constellation of its own.
        val inner = r * 0.52f
        val outer = r * 0.9f
        s.neonPath(Path().apply { ring(cx, cy, outer); ring(cx, cy, inner) }, c1, 1.8f, dim = true)
        val spokes = Path()
        for (k in 0 until 12) {
            val a = TAU * k / 12f
            spokes.moveTo(cx + cos(a) * inner, cy + sin(a) * inner); spokes.lineTo(cx + cos(a) * outer, cy + sin(a) * outer)
        }
        s.canvas.drawPath(spokes, s.stroke(c1, 1.5f, 150))
        for (k in 0 until 12) {
            val mid = TAU * (k + 0.5f) / 12f
            val sx = cx + cos(mid) * (inner + outer) / 2f
            val sy = cy + sin(mid) * (inner + outer) / 2f
            val stars = ArrayList<FloatArray>()
            val count = s.i(4, 7)
            repeat(count) {
                val ox = s.f(-0.08f, 0.08f) * r
                val oy = s.f(-0.08f, 0.08f) * r
                stars += floatArrayOf(sx + ox, sy + oy)
            }
            val lines = Path()
            for ((j, p) in stars.withIndex()) if (j == 0) lines.moveTo(p[0], p[1]) else lines.lineTo(p[0], p[1])
            s.canvas.drawPath(lines, s.stroke(c2, 1.3f, 170))
            for (p in stars) s.glowDot(p[0], p[1], s.f(2f, 4.4f), Color.WHITE, 12f)
        }
        // The planets' ring, with the sun and moon marked, and the pointer across the middle.
        val planets = r * 0.4f
        s.neonPath(Path().apply { ring(cx, cy, planets) }, c2, 1.6f, dim = true)
        repeat(s.count(7)) {
            val a = s.f(0f, TAU)
            s.glowDot(cx + cos(a) * planets, cy + sin(a) * planets, s.f(4f, 8f), s.color(s.i(0, 3)), 14f)
        }
        val pa = s.f(0f, TAU)
        val pointer = Path().apply {
            moveTo(cx + cos(pa) * r * 0.97f, cy + sin(pa) * r * 0.97f)
            lineTo(cx + cos(pa + 0.05f) * r * 0.1f, cy + sin(pa + 0.05f) * r * 0.1f)
            lineTo(cx + cos(pa - 0.05f) * r * 0.1f, cy + sin(pa - 0.05f) * r * 0.1f)
            close()
        }
        s.neonPath(pointer, lighten(c0, 0.3f), 2f, dim = true)
        // A sun at the centre, rays alternating long and short.
        val sun = Path()
        for (k in 0 until 24) {
            val a = TAU * k / 24f
            val len = if (k % 2 == 0) 0.16f else 0.1f
            sun.moveTo(cx + cos(a) * r * 0.08f, cy + sin(a) * r * 0.08f); sun.lineTo(cx + cos(a) * r * len, cy + sin(a) * r * len)
        }
        s.neonPath(sun.also { it.ring(cx, cy, r * 0.065f) }, c0, 2.4f)
    }
}

// ---------------------------------------------------------------------------------------------

/** An ornate tarot card: a double border, a numeral, a title and one of three arcana in gold line. */
object TarotCard : Style("tarot_card", "Tarot", Category.MYSTIC) {
    override val autoPalettes = listOf("gold", "gold", "violet", "sakura", "aurora")
    override val usesDetail: Boolean get() = false      // a card is a fixed composition

    override fun draw(s: Scene) {
        val gold = s.color(0)
        val glow = s.color(1)
        val cardW = s.w * 0.78f
        val cardH = cardW * 1.7f
        val cx = s.cx
        val cy = s.h * 0.5f
        val l = cx - cardW / 2f
        val t = cy - cardH / 2f
        val rect = RectF(l, t, l + cardW, t + cardH)
        s.softGlow(cx, cy, cardW * 0.9f, glow, 40)
        // The card face: black with a faint wash of its own colour toward the middle.
        s.canvas.drawRoundRect(rect, 26f, 26f, s.vGradient(t, t + cardH, withAlpha(darken(glow, 0.88f), 255), Color.BLACK))
        s.neonPath(Path().apply { addRoundRect(rect, 26f, 26f, Path.Direction.CW) }, gold, 2.4f)
        val inset = RectF(l + 22f, t + 22f, l + cardW - 22f, t + cardH - 22f)
        s.canvas.drawRoundRect(inset, 14f, 14f, s.stroke(gold, 1.4f, 190))
        // Corner ornaments: a quarter-circle and a dot.
        val orn = Path()
        for ((px, py, dx, dy) in listOf(floatArrayOf(inset.left, inset.top, 1f, 1f), floatArrayOf(inset.right, inset.top, -1f, 1f), floatArrayOf(inset.left, inset.bottom, 1f, -1f), floatArrayOf(inset.right, inset.bottom, -1f, -1f)).map { it.toList() }) {
            orn.moveTo(px + dx * 12f, py + dy * 62f); orn.quadTo(px + dx * 12f, py + dy * 12f, px + dx * 62f, py + dy * 12f)
            orn.addCircle(px + dx * 30f, py + dy * 30f, 5f, Path.Direction.CW)
        }
        s.canvas.drawPath(orn, s.stroke(gold, 1.6f, 220))

        val which = s.i(0, 3)
        val numerals = arrayOf("XVIII", "XVII", "XIX")
        val titles = arrayOf("THE MOON", "THE STAR", "THE SUN")
        val np = s.textPaint(cardH * 0.05f, gold, Typeface.SERIF, Paint.Align.CENTER).apply { letterSpacing = 0.3f }
        s.canvas.drawText(numerals[which], cx, t + cardH * 0.1f, np)
        s.canvas.drawText(titles[which], cx, t + cardH * 0.935f, np)
        s.canvas.drawLine(l + cardW * 0.28f, t + cardH * 0.12f, l + cardW * 0.72f, t + cardH * 0.12f, s.stroke(gold, 1.4f, 180))
        s.canvas.drawLine(l + cardW * 0.28f, t + cardH * 0.885f, l + cardW * 0.72f, t + cardH * 0.885f, s.stroke(gold, 1.4f, 180))

        val ax = cx
        val ay = cy + cardH * 0.0f
        val aw = cardW * 0.78f
        s.canvas.save()
        s.canvas.clipRect(l + 30f, t + cardH * 0.14f, l + cardW - 30f, t + cardH * 0.87f)
        when (which) {
            0 -> moon(s, ax, ay, aw, cardH, gold, glow)
            1 -> star(s, ax, ay, aw, cardH, gold, glow)
            else -> sun(s, ax, ay, aw, cardH, gold, glow)
        }
        s.canvas.restore()
    }

    private fun moon(s: Scene, cx: Float, cy: Float, aw: Float, ch: Float, gold: Int, glow: Int) {
        val my = cy - ch * 0.17f
        val mr = aw * 0.27f
        s.softGlow(cx, my, mr * 2.2f, glow, 90)
        // Full moon with a crescent profile cut into it, and rays.
        val rays = Path()
        for (k in 0 until 32) {
            val a = TAU * k / 32f
            val r0 = mr * 1.15f
            val r1 = mr * (if (k % 2 == 0) 1.6f else 1.35f)
            rays.moveTo(cx + cos(a) * r0, my + sin(a) * r0); rays.lineTo(cx + cos(a) * r1, my + sin(a) * r1)
        }
        s.canvas.drawPath(rays, s.stroke(gold, 1.6f, 200))
        s.canvas.drawCircle(cx, my, mr, s.stroke(gold, 2.6f))
        val crescent = Path().apply { addCircle(cx, my, mr * 0.78f, Path.Direction.CW) }
        crescent.op(Path().apply { addCircle(cx + mr * 0.3f, my, mr * 0.72f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
        s.canvas.drawPath(crescent, s.fill(lighten(gold, 0.5f), 220))
        // Two towers, a winding path, and the wolf.
        for (sgn in floatArrayOf(-1f, 1f)) {
            val tx = cx + sgn * aw * 0.4f
            val ty = cy + ch * 0.1f
            val tower = Path().apply { addRect(tx - 34f, ty - 120f, tx + 34f, ty + 10f, Path.Direction.CW) }
            for (k in 0 until 3) tower.addRect(tx - 34f + k * 24f, ty - 140f, tx - 34f + k * 24f + 20f, ty - 120f, Path.Direction.CW)
            s.canvas.drawPath(tower, s.fill(Color.BLACK))
            s.canvas.drawPath(tower, s.stroke(gold, 2f, 230))
        }
        val path = Path().apply {
            moveTo(cx - 24f, cy + ch * 0.38f)
            cubicTo(cx + 150f, cy + ch * 0.3f, cx - 160f, cy + ch * 0.22f, cx - 8f, cy + ch * 0.1f)
            lineTo(cx + 8f, cy + ch * 0.1f)
            cubicTo(cx - 140f, cy + ch * 0.22f, cx + 170f, cy + ch * 0.3f, cx + 24f, cy + ch * 0.38f)
            close()
        }
        s.canvas.drawPath(path, s.stroke(gold, 2f, 220))
        val wolf = wolfPath(cx - aw * 0.26f, cy + ch * 0.37f, ch * 0.13f, false)
        s.canvas.drawPath(wolf, s.fill(Color.BLACK))
        s.canvas.drawPath(wolf, s.stroke(gold, 1.8f, 230))
        // Drops falling from the moon.
        repeat(9) {
            val dx = cx + s.f(-aw * 0.3f, aw * 0.3f)
            val dy = my + mr + s.f(40f, 340f)
            s.canvas.drawPath(Path().apply { moveTo(dx, dy - 12f); quadTo(dx + 8f, dy + 4f, dx, dy + 10f); quadTo(dx - 8f, dy + 4f, dx, dy - 12f) }, s.stroke(gold, 1.5f, 200))
        }
    }

    private fun star(s: Scene, cx: Float, cy: Float, aw: Float, ch: Float, gold: Int, glow: Int) {
        val sy = cy - ch * 0.15f
        s.softGlow(cx, sy, aw * 0.6f, glow, 90)
        val big = Path()
        for (k in 0 until 16) {
            val a = TAU * k / 16f - PI.toFloat() / 2f
            val r = aw * if (k % 2 == 0) (if (k % 4 == 0) 0.3f else 0.2f) else 0.07f
            if (k == 0) big.moveTo(cx + cos(a) * r, sy + sin(a) * r) else big.lineTo(cx + cos(a) * r, sy + sin(a) * r)
        }
        big.close()
        s.canvas.drawPath(big, s.fill(withAlpha(gold, 60)))
        s.neonPath(big, gold, 2.4f)
        repeat(7) { k ->
            val a = TAU * k / 7f - PI.toFloat() / 2f
            val d = aw * 0.42f
            val x = cx + cos(a) * d
            val y = sy + sin(a) * d
            val p = Path()
            for (j in 0 until 8) {
                val aa = TAU * j / 8f
                val rr = aw * if (j % 2 == 0) 0.05f else 0.02f
                if (j == 0) p.moveTo(x + cos(aa) * rr, y + sin(aa) * rr) else p.lineTo(x + cos(aa) * rr, y + sin(aa) * rr)
            }
            p.close()
            s.neonPath(p, lighten(gold, 0.2f), 1.8f)
        }
        // Water below: three waves, and a path of small rings where it is poured.
        for (k in 0 until 4) {
            val y0 = cy + ch * 0.18f + k * 50f
            val w = Path()
            var x = cx - aw * 0.48f
            w.moveTo(x, y0)
            while (x < cx + aw * 0.48f) { x += 10f; w.lineTo(x, y0 + sin(x * 0.04f + k) * 10f) }
            s.canvas.drawPath(w, s.stroke(gold, 1.8f, 230 - k * 30))
        }
    }

    private fun sun(s: Scene, cx: Float, cy: Float, aw: Float, ch: Float, gold: Int, glow: Int) {
        val sy = cy - ch * 0.12f
        val sr = aw * 0.24f
        s.softGlow(cx, sy, sr * 3f, glow, 100)
        val rays = Path()
        for (k in 0 until 24) {
            val a = TAU * k / 24f
            val r0 = sr * 1.1f
            if (k % 2 == 0) {
                rays.moveTo(cx + cos(a) * r0, sy + sin(a) * r0); rays.lineTo(cx + cos(a) * sr * 1.95f, sy + sin(a) * sr * 1.95f)
            } else {
                rays.moveTo(cx + cos(a) * r0, sy + sin(a) * r0)
                for (j in 1..8) {
                    val rr = r0 + j * sr * 0.1f
                    val aa = a + sin(j * 1.6f) * 0.045f
                    rays.lineTo(cx + cos(aa) * rr, sy + sin(aa) * rr)
                }
            }
        }
        s.neonPath(rays, gold, 2.2f)
        s.canvas.drawCircle(cx, sy, sr, s.fill(withAlpha(gold, 70)))
        s.neonPath(Path().apply { ring(cx, sy, sr) }, lighten(gold, 0.2f), 2.6f)
        // A calm face.
        s.canvas.drawArc(RectF(cx - sr * 0.5f, sy - sr * 0.28f, cx - sr * 0.15f, sy), 200f, 140f, false, s.stroke(gold, 2.4f))
        s.canvas.drawArc(RectF(cx + sr * 0.15f, sy - sr * 0.28f, cx + sr * 0.5f, sy), 200f, 140f, false, s.stroke(gold, 2.4f))
        s.canvas.drawArc(RectF(cx - sr * 0.3f, sy + sr * 0.05f, cx + sr * 0.3f, sy + sr * 0.5f), 20f, 140f, false, s.stroke(gold, 2.4f))
        // Hills and a row of flowers.
        val hill = Path().apply { moveTo(cx - aw * 0.55f, cy + ch * 0.36f); quadTo(cx - aw * 0.2f, cy + ch * 0.16f, cx + aw * 0.05f, cy + ch * 0.28f); quadTo(cx + aw * 0.3f, cy + ch * 0.34f, cx + aw * 0.55f, cy + ch * 0.2f) }
        s.canvas.drawPath(hill, s.stroke(gold, 2f, 230))
        for (k in 0 until 7) {
            val fx = cx - aw * 0.42f + k * aw * 0.14f
            val fy = cy + ch * (0.3f + 0.04f * sin(k * 1.7f))
            s.canvas.drawLine(fx, fy, fx, fy + 70f, s.stroke(gold, 1.6f, 200))
            for (j in 0 until 8) {
                val a = TAU * j / 8f
                s.canvas.drawCircle(fx + cos(a) * 15f, fy + sin(a) * 15f, 6f, s.stroke(gold, 1.4f, 210))
            }
            s.canvas.drawCircle(fx, fy, 7f, s.fill(gold, 200))
        }
    }
}

// ---------------------------------------------------------------------------------------------

/** A cluster of faceted crystals growing from the dark, each face lit differently. */
object CrystalCluster : Style("crystal_cluster", "Crystal Cluster", Category.MYSTIC) {
    override val autoPalettes = listOf("violet", "aurora", "ocean", "sakura", "cyan", "gold")

    private fun Scene.crystal(bx: Float, by: Float, len: Float, wid: Float, ang: Float, color: Int) {
        canvas.save()
        canvas.translate(bx, by)
        canvas.rotate(ang)
        // A hexagonal prism seen from the side: three faces and a pointed tip.
        val x0 = -wid / 2f; val x1 = -wid / 6f; val x2 = wid / 6f; val x3 = wid / 2f
        val body = -len * 0.78f
        val tip = -len
        fun face(pts: FloatArray, c: Int) {
            val p = Path().apply {
                moveTo(pts[0], pts[1])
                var i = 2
                while (i < pts.size) { lineTo(pts[i], pts[i + 1]); i += 2 }
                close()
            }
            canvas.drawPath(p, vGradient(tip, 0f, withAlpha(lighten(c, 0.2f), 230), withAlpha(darken(c, 0.55f), 190)))
            canvas.drawPath(p, stroke(lighten(c, 0.55f), 1.6f, 200))
        }
        face(floatArrayOf(x0, 0f, x1, 0f, x1, body, x0, body), darken(color, 0.1f))
        face(floatArrayOf(x1, 0f, x2, 0f, x2, body, x1, body), lighten(color, 0.1f))
        face(floatArrayOf(x2, 0f, x3, 0f, x3, body, x2, body), darken(color, 0.45f))
        face(floatArrayOf(x0, body, x1, body, 0f, tip), color)
        face(floatArrayOf(x1, body, x2, body, 0f, tip), lighten(color, 0.3f))
        face(floatArrayOf(x2, body, x3, body, 0f, tip), darken(color, 0.3f))
        // A bright streak where the light catches the middle face.
        canvas.drawLine(-wid * 0.02f, -len * 0.08f, -wid * 0.02f, body * 0.9f, stroke(Color.WHITE, 3f, 110, 4f))
        canvas.restore()
    }

    override fun draw(s: Scene) {
        val c0 = s.color(0)
        val c1 = s.color(1)
        val c2 = s.color(2)
        val cx = s.cx
        val baseY = s.h * s.f(0.66f, 0.76f)
        s.starSky(140, baseY, 5)
        s.softGlow(cx, baseY - s.h * 0.1f, s.w * 0.9f, c0, 70)
        // Taller crystals at the middle, leaning outward, shorter ones at the edges.
        val n = s.count(9) + 3
        val order = (0 until n).sortedBy { -abs(it - n / 2) }
        for (k in order) {
            val u = (k - (n - 1) / 2f) / (n / 2f)
            val len = s.h * s.f(0.14f, 0.2f) * (1.25f - abs(u) * 0.55f) * s.f(0.8f, 1.2f)
            val wid = s.w * s.f(0.07f, 0.12f)
            s.crystal(cx + u * s.w * 0.36f + s.f(-20f, 20f), baseY + s.f(-20f, 20f), len, wid, u * 34f + s.f(-8f, 8f), s.color(k))
        }
        // The rock they grow from, and a few loose crystals floating above.
        val rock = Path()
        rock.moveTo(0f, s.h); rock.lineTo(0f, baseY + 40f)
        var x = 0f
        while (x < s.w) { x += s.f(40f, 90f); rock.lineTo(x, baseY + s.f(20f, 60f) + abs(x - cx) * 0.08f) }
        rock.lineTo(s.w, s.h); rock.close()
        s.canvas.drawPath(rock, s.fill(Color.BLACK))
        s.canvas.drawPath(rock, s.stroke(withAlpha(c1, 120), 1.6f))
        repeat(s.count(7)) {
            s.crystal(s.f(60f, s.w - 60f), s.f(s.h * 0.12f, baseY - s.h * 0.28f), s.f(50f, 120f), s.f(16f, 34f), s.f(-40f, 40f), s.color(s.i(0, 3)))
        }
        repeat(s.count(36)) { s.glowDot(s.f(0f, s.w), s.f(s.h * 0.1f, baseY), s.f(1f, 2.8f), lighten(c2, 0.5f), 10f) }
    }
}

// ---------------------------------------------------------------------------------------------

/** The moon's eight phases in a ring around a sun, with orbits, constellations and dust. */
object LunarCycle : Style("lunar_cycle", "Lunar Cycle", Category.MYSTIC) {
    override val autoPalettes = listOf("gold", "mono", "violet", "ocean", "sakura")

    /** One phase: a dark disc, and a lit part shaped by the terminator. [p] runs 0 new .. 0.5 full .. 1 new. */
    private fun Scene.phase(x: Float, y: Float, r: Float, p: Float, color: Int) {
        val rect = RectF(x - r, y - r, x + r, y + r)
        canvas.drawCircle(x, y, r, fill(Color.rgb(14, 15, 22)))
        val waxing = p < 0.5f
        val k = cos(p * TAU)                                        // 1 new .. -1 full
        val lit = Path()
        lit.addArc(rect, if (waxing) -90f else 90f, 180f)
        val ellipse = Path().apply { addOval(RectF(x - r * abs(k), y - r, x + r * abs(k), y + r), Path.Direction.CW) }
        if (k > 0f) lit.op(ellipse, Path.Op.DIFFERENCE) else lit.op(ellipse, Path.Op.UNION)
        canvas.drawPath(lit, fill(color))
        canvas.drawCircle(x, y, r, stroke(withAlpha(lighten(color, 0.3f), 200), 2f))
    }

    override fun draw(s: Scene) {
        val c0 = s.lighten0()
        val c1 = s.color(1)
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.5f)
        val R = s.w * 0.36f
        s.starSky(180, s.h, 6)
        s.softGlow(cx, cy, R * 1.5f, c1, 50)
        // Orbits: more rings as Detail goes up.
        val rings = Path()
        for (k in 1..(2 + (s.density * 2f).toInt())) rings.ring(cx, cy, R * (0.45f + 0.28f * k))
        s.canvas.drawPath(rings, s.stroke(withAlpha(c1, 110), 1.4f))
        // The phases, a constellation line joining them.
        val phases = 8
        val pr = s.w * 0.075f
        val joined = Path()
        for (k in 0 until phases) {
            val a = TAU * k / phases - PI.toFloat() / 2f
            val x = cx + cos(a) * R
            val y = cy + sin(a) * R
            if (k == 0) joined.moveTo(x, y) else joined.lineTo(x, y)
        }
        joined.close()
        s.canvas.drawPath(joined, s.stroke(withAlpha(c1, 140), 1.5f))
        for (k in 0 until phases) {
            val a = TAU * k / phases - PI.toFloat() / 2f
            s.softGlow(cx + cos(a) * R, cy + sin(a) * R, pr * 2f, c0, 40)
            s.phase(cx + cos(a) * R, cy + sin(a) * R, pr, k / phases.toFloat(), c0)
        }
        // A sun at the heart: a disc, a corona of rays and a ring.
        val sr = R * 0.2f
        val rays = Path()
        for (k in 0 until 32) {
            val a = TAU * k / 32f
            rays.moveTo(cx + cos(a) * sr * 1.2f, cy + sin(a) * sr * 1.2f)
            val l = if (k % 2 == 0) 2.0f else 1.6f
            rays.lineTo(cx + cos(a) * sr * l, cy + sin(a) * sr * l)
        }
        s.neonPath(rays, s.color(0), 2f, dim = true)
        s.canvas.drawCircle(cx, cy, sr, s.fill(lighten(s.color(0), 0.55f)))
        s.neonPath(Path().apply { ring(cx, cy, sr * 1.1f) }, s.color(0), 2.4f)
        s.canvas.drawCircle(cx, cy, sr * 0.55f, s.fill(Color.BLACK, 30))
        // Dust.
        repeat(s.count(70)) { s.glowDot(s.f(0f, s.w), s.f(0f, s.h), s.f(0.8f, 2.4f), lighten(c1, 0.5f), 8f) }
    }

    private fun Scene.lighten0(): Int = lighten(color(0), 0.62f)
}

object MysticWorld {
    val styles: List<Style> = listOf(ArcaneCircle, ZodiacWheel, TarotCard, CrystalCluster, LunarCycle)
}
