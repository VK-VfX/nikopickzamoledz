package com.nikopick.zamoled.gen

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// =====================================================================
// AUDIO: music gear
// =====================================================================

private fun Scene.floatingNotes(n: Int, x0: Float, x1: Float, y0: Float, y1: Float) {
    repeat(n) {
        Doodles.draw(this, 8, f(x0, x1), f(y0, y1), f(16f, 30f), f(-25f, 25f), anyColor(), 2.6f, alpha = i(150, 255))
    }
}

object Vinyl : Style("vinyl", "Vinyl", Category.AUDIO) {
    override val autoPalettes = listOf("sunset", "lava", "graffiti", "gold", "cyan")

    override fun draw(s: Scene) {
        val cx = s.w * s.f(0.42f, 0.5f)
        val cy = s.h * s.f(0.4f, 0.46f)
        val r = s.f(320f, 360f)
        // Platter with strobe dots around the rim.
        s.canvas.drawCircle(cx, cy, r + 30f, s.fill(Color.rgb(18, 18, 22)))
        s.canvas.drawCircle(cx, cy, r + 30f, s.stroke(Color.rgb(140, 140, 150), 2f))
        for (k in 0 until 120) {
            val a = k * TAU / 120f
            s.dot(cx + cos(a) * (r + 18f), cy + sin(a) * (r + 18f), 2f, Color.rgb(180, 180, 190), 200)
        }
        // Record grooves and sheen.
        s.canvas.drawCircle(cx, cy, r, s.fill(Color.BLACK))
        var gr = r - 6f
        while (gr > r * 0.36f) {
            s.canvas.drawCircle(cx, cy, gr, s.stroke(Color.rgb(70, 70, 80), 1f, if (s.chance(0.1f)) 40 else 120))
            gr -= s.f(2.5f, 4.5f)
        }
        val sheen = SweepGradient(cx, cy, intArrayOf(Color.TRANSPARENT, withAlpha(Color.WHITE, 70), Color.TRANSPARENT, Color.TRANSPARENT, withAlpha(Color.WHITE, 60), Color.TRANSPARENT), floatArrayOf(0f, 0.08f, 0.16f, 0.5f, 0.58f, 0.66f))
        sheen.setLocalMatrix(Matrix().apply { setRotate(s.f(0f, 360f), cx, cy) })
        s.canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = sheen })
        // Label.
        val lr = r * 0.33f
        s.canvas.drawCircle(cx, cy, lr, s.fill(s.color(0)))
        s.canvas.drawCircle(cx, cy, lr * 0.75f, s.stroke(lighten(s.color(0), 0.4f), 1.4f, 200))
        s.canvas.drawText("ZAMOLED", cx, cy - lr * 0.35f, s.textPaint(lr * 0.24f, Color.BLACK))
        s.canvas.drawText("SIDE A · 33⅓", cx, cy + lr * 0.5f, s.textPaint(lr * 0.14f, Color.BLACK))
        s.canvas.drawCircle(cx, cy, 8f, s.fill(Color.rgb(200, 200, 210)))
        // Tonearm.
        val px = s.w * 0.88f
        val py = cy - r - 40f
        s.canvas.drawCircle(px, py, 46f, s.fill(Color.rgb(30, 30, 36)))
        s.canvas.drawCircle(px, py, 46f, s.stroke(Color.rgb(170, 170, 180), 2.4f))
        s.canvas.drawCircle(px, py, 18f, s.fill(Color.rgb(170, 170, 180)))
        val endX = cx + r * 0.62f
        val endY = cy + r * 0.25f
        val arm = Path()
        arm.moveTo(px, py); arm.lineTo(px - 10f, cy - r * 0.2f); arm.lineTo(endX, endY)
        s.canvas.drawPath(arm, s.stroke(Color.rgb(200, 200, 210), 7f))
        s.canvas.save()
        s.canvas.rotate(-35f, endX, endY)
        s.canvas.drawRoundRect(RectF(endX - 34f, endY - 14f, endX + 10f, endY + 14f), 4f, 4f, s.fill(Color.rgb(200, 200, 210)))
        s.canvas.restore()
        // Sound waves and notes floating off the record.
        for (k in 1..4) {
            s.canvas.drawArc(RectF(cx - r - k * 50f, cy - r - k * 50f, cx + r + k * 50f, cy + r + k * 50f), 200f, 60f, false, s.stroke(s.color(1), 3f, 200 - k * 40))
        }
        s.floatingNotes(s.i(5, 9), 40f, s.w - 40f, s.h * 0.68f, s.h * 0.95f)
    }
}

object Boombox : Style("boombox", "Boombox", Category.AUDIO) {
    override val autoPalettes = listOf("graffiti", "candy", "acid", "sunset", "cyan")

    override fun draw(s: Scene) {
        val cx = s.cx
        val cy = s.h * s.f(0.44f, 0.5f)
        val bw = 420f
        val bh = 250f
        val c = s.color(0)
        val body = RectF(cx - bw, cy - bh, cx + bw, cy + bh)
        s.softGlow(cx, cy, 700f, c, 40)
        // Antenna and handle.
        s.canvas.drawLine(cx + bw * 0.6f, body.top, cx + bw * 1.05f, body.top - 380f, s.stroke(Color.rgb(200, 200, 210), 4f))
        s.glowDot(cx + bw * 1.05f, body.top - 380f, 6f, s.color(1), 14f)
        val handle = RectF(cx - bw * 0.6f, body.top - 120f, cx + bw * 0.6f, body.top + 40f)
        s.canvas.drawRoundRect(handle, 60f, 60f, s.stroke(c, 14f))
        s.canvas.drawRoundRect(body, 50f, 50f, s.fill(Color.rgb(16, 16, 20)))
        s.canvas.drawRoundRect(body, 50f, 50f, s.stroke(c, 5f))
        // Speakers with cones.
        for (sgn in floatArrayOf(-1f, 1f)) {
            val sx = cx + sgn * bw * 0.58f
            val sy = cy + 30f
            val sr = 170f
            s.canvas.drawCircle(sx, sy, sr, s.fill(Color.BLACK))
            for (k in 0 until 6) s.canvas.drawCircle(sx, sy, sr * (1f - k * 0.13f), s.stroke(if (k == 0) c else Color.rgb(90, 90, 100), if (k == 0) 5f else 2f))
            s.canvas.drawCircle(sx, sy, sr * 0.22f, s.fill(c))
            s.canvas.drawCircle(sx - 12f, sy - 12f, sr * 0.06f, s.fill(Color.WHITE, 180))
            // Pulses.
            for (k in 1..3) {
                val pr = sr + k * 70f
                s.canvas.drawArc(RectF(sx - pr, sy - pr, sx + pr, sy + pr), if (sgn < 0) 130f else -50f, 100f, false, s.stroke(s.color(k), 4f, 220 - k * 50))
            }
        }
        // Cassette deck in the middle.
        val deck = RectF(cx - 140f, cy - 80f, cx + 140f, cy + 120f)
        s.canvas.drawRoundRect(deck, 14f, 14f, s.fill(Color.rgb(28, 28, 34)))
        s.canvas.drawRoundRect(deck, 14f, 14f, s.stroke(Color.rgb(170, 170, 180), 2.4f))
        val tape = RectF(deck.left + 24f, deck.top + 30f, deck.right - 24f, deck.bottom - 30f)
        s.canvas.drawRoundRect(tape, 8f, 8f, s.fill(darken(s.color(1), 0.4f)))
        s.canvas.drawRoundRect(tape, 8f, 8f, s.stroke(s.color(1), 2f))
        for (dx in floatArrayOf(-55f, 55f)) {
            s.canvas.drawCircle(cx + dx, deck.centerY(), 24f, s.fill(Color.BLACK))
            for (k in 0 until 6) {
                val a = k * TAU / 6f
                s.canvas.drawLine(cx + dx, deck.centerY(), cx + dx + cos(a) * 18f, deck.centerY() + sin(a) * 18f, s.stroke(Color.WHITE, 2f, 200))
            }
        }
        // Buttons, level meter and brand.
        for (k in 0 until 6) {
            val x = cx - 170f + k * 68f
            s.canvas.drawRoundRect(RectF(x, body.top - 26f, x + 48f, body.top + 4f), 6f, 6f, s.fill(if (k == 2) Color.rgb(230, 60, 60) else Color.rgb(170, 170, 180)))
        }
        for (k in 0 until 12) {
            val lit = k < s.i(5, 12)
            val color = when { k > 9 -> Color.rgb(255, 70, 70); k > 6 -> Color.rgb(255, 200, 60); else -> Color.rgb(90, 230, 120) }
            s.canvas.drawRect(cx - 120f + k * 20f, body.top + 40f, cx - 106f + k * 20f, body.top + 70f, s.fill(color, if (lit) 255 else 50))
        }
        s.canvas.drawText("ZAMOLED · BEATBOX 3000", cx, body.bottom - 26f, s.textPaint(26f, c).apply { letterSpacing = 0.15f })
        s.floatingNotes(s.i(6, 11), 40f, s.w - 40f, s.h * 0.08f, s.h * 0.3f)
        s.floatingNotes(s.i(4, 8), 40f, s.w - 40f, s.h * 0.72f, s.h * 0.95f)
    }
}

object StudioMic : Style("studio_mic", "Studio Mic", Category.AUDIO) {
    override val autoPalettes = listOf("gold", "lava", "violet", "cyan", "sunset")

    override fun draw(s: Scene) {
        val cx = s.cx + s.f(-40f, 40f)
        val cy = s.h * s.f(0.4f, 0.46f)
        val c = s.color(0)
        // Spotlight cone from above.
        val cone = Path()
        cone.moveTo(cx - 40f, 0f); cone.lineTo(cx + 40f, 0f); cone.lineTo(cx + 420f, s.h * 0.9f); cone.lineTo(cx - 420f, s.h * 0.9f); cone.close()
        s.canvas.drawPath(cone, s.vGradient(0f, s.h * 0.9f, withAlpha(c, 60), Color.TRANSPARENT))
        // ON AIR sign.
        val sign = RectF(s.cx - 150f, s.h * 0.07f, s.cx + 150f, s.h * 0.07f + 90f)
        val frame = Path(); frame.addRoundRect(sign, 20f, 20f, Path.Direction.CW)
        s.canvas.drawRoundRect(sign, 20f, 20f, s.fill(Color.rgb(60, 6, 10)))
        s.neonPath(frame, Color.rgb(255, 50, 60), 3f)
        s.neonText("ON AIR", sign.centerX(), sign.centerY() + 22f, 60f, Color.rgb(255, 60, 70), Typeface.DEFAULT_BOLD)
        // Stand and arm.
        s.canvas.drawLine(cx, cy + 300f, cx + 40f, s.h + 10f, s.stroke(Color.rgb(150, 150, 160), 12f))
        s.canvas.drawLine(cx, cy + 300f, cx, cy + 180f, s.stroke(Color.rgb(150, 150, 160), 10f))
        // Shock mount: ring with elastic cords.
        val ring = RectF(cx - 150f, cy - 40f, cx + 150f, cy + 60f)
        s.canvas.drawOval(ring, s.stroke(Color.rgb(170, 170, 180), 6f))
        for (k in 0 until 8) {
            val a = k * TAU / 8f
            s.canvas.drawLine(cx + cos(a) * 150f, cy + 10f + sin(a) * 50f, cx + cos(a + 0.8f) * 70f, cy + 10f + sin(a + 0.8f) * 120f, s.stroke(c, 2f, 200))
        }
        s.canvas.drawLine(cx, ring.bottom, cx, cy + 180f, s.stroke(Color.rgb(150, 150, 160), 10f))
        // Microphone body and grille.
        val capsule = RectF(cx - 95f, cy - 330f, cx + 95f, cy + 10f)
        val grille = Path(); grille.addRoundRect(RectF(capsule.left, capsule.top, capsule.right, cy - 110f), 95f, 95f, Path.Direction.CW)
        s.canvas.drawRoundRect(capsule, 95f, 95f, s.vGradient(capsule.top, capsule.bottom, Color.rgb(200, 200, 210), Color.rgb(60, 60, 70)))
        s.canvas.drawPath(grille, s.fill(Color.rgb(30, 30, 36)))
        s.canvas.save()
        s.canvas.clipPath(grille)
        for (k in -20..20) {
            s.canvas.drawLine(cx + k * 12f - 200f, capsule.top, cx + k * 12f + 200f, cy - 110f, s.stroke(Color.rgb(170, 170, 180), 1.4f, 200))
            s.canvas.drawLine(cx + k * 12f + 200f, capsule.top, cx + k * 12f - 200f, cy - 110f, s.stroke(Color.rgb(170, 170, 180), 1.4f, 200))
        }
        s.canvas.restore()
        s.canvas.drawPath(grille, s.stroke(Color.rgb(220, 220, 230), 3f))
        s.canvas.drawRect(capsule.left, cy - 115f, capsule.right, cy - 95f, s.fill(c))
        s.canvas.drawRoundRect(capsule, 95f, 95f, s.stroke(Color.rgb(230, 230, 240), 3f))
        s.canvas.drawArc(RectF(capsule.left + 18f, capsule.top + 20f, capsule.left + 80f, capsule.top + 160f), 180f, 70f, false, s.stroke(Color.WHITE, 6f, 160))
        // Pop filter.
        val px = cx - 270f
        val py = cy - 200f
        s.canvas.drawCircle(px, py, 110f, s.fill(Color.rgb(20, 20, 26), 220))
        s.canvas.save()
        val disc = Path(); disc.addCircle(px, py, 110f, Path.Direction.CW)
        s.canvas.clipPath(disc)
        var gx = px - 110f
        while (gx < px + 110f) { s.canvas.drawLine(gx, py - 110f, gx, py + 110f, s.stroke(Color.rgb(90, 90, 100), 1f)); gx += 8f }
        s.canvas.restore()
        s.canvas.drawCircle(px, py, 110f, s.stroke(Color.rgb(170, 170, 180), 4f))
        val goose = Path(); goose.moveTo(px + 60f, py + 95f); goose.cubicTo(px + 80f, py + 240f, cx - 80f, cy + 200f, cx, cy + 240f)
        s.canvas.drawPath(goose, s.stroke(Color.rgb(150, 150, 160), 4f))
        // Sound ribbon.
        for (line in 0 until 3) {
            val wave = Path()
            var x = 0f
            wave.moveTo(0f, s.h * 0.8f)
            while (x <= s.w) {
                val env = sin(PI.toFloat() * x / s.w)
                wave.lineTo(x, s.h * 0.8f + sin(x * 0.04f + line) * 60f * env * (1f + 0.5f * s.noise.at(x * 0.01f, line.toFloat())))
                x += 6f
            }
            s.canvas.drawPath(wave, s.stroke(s.color(line), 2.4f, 220).apply { blendMode = BlendMode.PLUS })
        }
    }
}

// =====================================================================
// MYSTIC: occult and parapsychology
// =====================================================================

object TarotMoon : Style("tarot_moon", "The Moon", Category.MYSTIC) {
    override val autoPalettes = listOf("gold", "gold", "violet", "sakura", "cyan")

    override fun draw(s: Scene) {
        val g = s.color(0)
        val line = s.stroke(g, 2.4f)
        val thin = s.stroke(g, 1.2f, 170)
        s.starSky(120)
        val card = RectF(s.w * 0.1f, s.h * 0.1f, s.w * 0.9f, s.h * 0.88f)
        s.canvas.drawRoundRect(card, 30f, 30f, s.fill(Color.BLACK))
        s.canvas.drawRoundRect(card, 30f, 30f, line)
        val inner = RectF(card.left + 22f, card.top + 22f, card.right - 22f, card.bottom - 22f)
        s.canvas.drawRoundRect(inner, 16f, 16f, thin)
        // Corner flourishes.
        for ((x, y) in listOf(inner.left to inner.top, inner.right to inner.top, inner.left to inner.bottom, inner.right to inner.bottom)) {
            for (k in 1..3) s.canvas.drawCircle(x, y, k * 9f, thin)
        }
        val tp = s.textPaint(44f, g, Typeface.create(Typeface.SERIF, Typeface.BOLD)).apply { letterSpacing = 0.25f }
        s.canvas.drawText("XVIII", card.centerX(), inner.top + 70f, tp)
        val banner = RectF(inner.left + 40f, inner.bottom - 110f, inner.right - 40f, inner.bottom - 40f)
        s.canvas.drawRect(banner, thin)
        s.canvas.drawText("THE MOON", card.centerX(), banner.centerY() + 15f, tp)

        val cx = card.centerX()
        val my = card.top + card.height() * 0.3f
        val mr = 120f
        // Sun-and-moon rays.
        for (k in 0 until 32) {
            val a = k * TAU / 32f
            val len = if (k % 2 == 0) mr * 0.75f else mr * 0.4f
            s.canvas.drawLine(cx + cos(a) * (mr + 10f), my + sin(a) * (mr + 10f), cx + cos(a) * (mr + 10f + len), my + sin(a) * (mr + 10f + len), thin)
        }
        s.softGlow(cx, my, mr * 2f, g, 70)
        s.canvas.drawCircle(cx, my, mr, s.fill(darken(g, 0.6f)))
        // Crescent face in profile.
        val crescent = Path(); crescent.addCircle(cx, my, mr * 0.92f, Path.Direction.CW)
        val cut = Path(); cut.addCircle(cx + mr * 0.42f, my, mr * 0.78f, Path.Direction.CW)
        crescent.op(cut, Path.Op.DIFFERENCE)
        s.canvas.drawPath(crescent, s.fill(lighten(g, 0.3f)))
        s.canvas.drawArc(RectF(cx - mr * 0.58f, my - mr * 0.3f, cx - mr * 0.28f, my - mr * 0.1f), 0f, 180f, false, s.stroke(Color.BLACK, 3f))
        s.canvas.drawCircle(cx, my, mr, line)
        // Falling drops.
        repeat(12) {
            val dx = cx + s.f(-200f, 200f)
            val dy = my + mr + s.f(40f, 220f)
            val drop = Path()
            drop.moveTo(dx, dy - 14f); drop.quadTo(dx + 9f, dy + 4f, dx, dy + 8f); drop.quadTo(dx - 9f, dy + 4f, dx, dy - 14f)
            s.canvas.drawPath(drop, s.fill(g, 200))
        }
        // Twin towers and the path to the horizon.
        val horizon = card.top + card.height() * 0.62f
        for (sgn in floatArrayOf(-1f, 1f)) {
            val tx = cx + sgn * card.width() * 0.33f
            val tower = RectF(tx - 36f, horizon - 220f, tx + 36f, horizon + 60f)
            s.canvas.drawRect(tower, s.fill(Color.BLACK))
            s.canvas.drawRect(tower, line)
            for (k in 0 until 4) s.canvas.drawRect(tower.left + k * 18f, tower.top - 16f, tower.left + k * 18f + 10f, tower.top, s.fill(g))
            s.canvas.drawRect(tx - 8f, tower.top + 50f, tx + 8f, tower.top + 80f, s.fill(lighten(g, 0.3f)))
        }
        val road = Path()
        road.moveTo(cx - 10f, horizon - 30f)
        road.cubicTo(cx + 120f, horizon + 80f, cx - 160f, horizon + 160f, cx - 40f, banner.top - 80f)
        road.lineTo(cx + 60f, banner.top - 80f)
        road.cubicTo(cx - 60f, horizon + 160f, cx + 160f, horizon + 80f, cx + 10f, horizon - 30f)
        road.close()
        s.canvas.drawPath(road, s.fill(darken(g, 0.75f)))
        s.canvas.drawPath(road, thin)
        // The pool.
        val pool = RectF(inner.left + 40f, banner.top - 90f, inner.right - 40f, banner.top - 20f)
        s.canvas.drawOval(pool, s.fill(darken(s.color(1), 0.6f)))
        s.canvas.drawOval(pool, thin)
        for (k in 0 until 3) s.canvas.drawLine(pool.left + 60f + k * 50f, pool.centerY() + k * 6f, pool.left + 120f + k * 50f, pool.centerY() + k * 6f, thin)
    }
}

object AllSeeingEye : Style("all_seeing_eye", "All-Seeing Eye", Category.MYSTIC) {
    override val autoPalettes = listOf("gold", "violet", "cyan", "aurora")

    override fun draw(s: Scene) {
        s.starSky(260)
        val g = s.color(0)
        val cx = s.cx
        val cy = s.h * s.f(0.4f, 0.46f)
        val side = 520f
        val top = floatArrayOf(cx, cy - side * 0.62f)
        val left = floatArrayOf(cx - side * 0.55f, cy + side * 0.34f)
        val right = floatArrayOf(cx + side * 0.55f, cy + side * 0.34f)
        // Rays.
        for (k in 0 until 72) {
            val a = k * TAU / 72f
            val r0 = side * 0.62f
            val r1 = r0 + if (k % 2 == 0) s.f(300f, 520f) else s.f(140f, 260f)
            s.canvas.drawLine(cx + cos(a) * r0, cy + sin(a) * r0, cx + cos(a) * r1, cy + sin(a) * r1, s.stroke(g, 1.4f, if (k % 2 == 0) 150 else 80))
        }
        s.softGlow(cx, cy, side, g, 70)
        // Orbit ring of planet glyphs.
        val glyphs = "☉☽☿♀♂♃♄♅♆"
        val orbitR = side * 0.85f
        s.canvas.drawCircle(cx, cy, orbitR, s.stroke(g, 1.4f, 120))
        for ((i, ch) in glyphs.withIndex()) {
            val a = -PI.toFloat() / 2f + i * TAU / glyphs.length
            s.canvas.drawCircle(cx + cos(a) * orbitR, cy + sin(a) * orbitR, 26f, s.fill(Color.BLACK))
            s.canvas.drawCircle(cx + cos(a) * orbitR, cy + sin(a) * orbitR, 26f, s.stroke(g, 1.6f))
            s.canvas.drawText(ch.toString(), cx + cos(a) * orbitR, cy + sin(a) * orbitR + 11f, s.textPaint(30f, g))
        }
        // Pyramid.
        val tri = Path()
        tri.moveTo(top[0], top[1]); tri.lineTo(right[0], right[1]); tri.lineTo(left[0], left[1]); tri.close()
        s.canvas.drawPath(tri, s.fill(Color.BLACK))
        s.canvas.drawPath(tri, s.stroke(g, 3f))
        for (k in 1..4) {
            val y = top[1] + (left[1] - top[1]) * (0.45f + k * 0.11f)
            val half = (y - top[1]) / (left[1] - top[1]) * side * 0.55f
            s.canvas.drawLine(cx - half, y, cx + half, y, s.stroke(g, 1.2f, 150))
        }
        // The eye.
        val ey = top[1] + (left[1] - top[1]) * 0.42f
        val ew = 120f
        val eye = Path()
        eye.moveTo(cx - ew, ey); eye.quadTo(cx, ey - ew * 0.75f, cx + ew, ey); eye.quadTo(cx, ey + ew * 0.75f, cx - ew, ey); eye.close()
        s.canvas.drawPath(eye, s.fill(darken(g, 0.85f)))
        s.canvas.drawPath(eye, s.stroke(g, 2.6f))
        s.canvas.drawCircle(cx, ey, 42f, s.fill(s.color(1)))
        for (k in 0 until 24) {
            val a = k * TAU / 24f
            s.canvas.drawLine(cx + cos(a) * 18f, ey + sin(a) * 18f, cx + cos(a) * 40f, ey + sin(a) * 40f, s.stroke(lighten(s.color(1), 0.4f), 1.2f, 200))
        }
        s.canvas.drawCircle(cx, ey, 17f, s.fill(Color.BLACK))
        s.canvas.drawCircle(cx - 10f, ey - 10f, 6f, s.fill(Color.WHITE))
        for (k in -3..3) s.canvas.drawLine(cx + k * 32f, ey - ew * 0.42f + kotlin.math.abs(k) * 6f, cx + k * 40f, ey - ew * 0.65f + kotlin.math.abs(k) * 8f, s.stroke(g, 2f))
        // Moon phases beneath.
        val py = cy + side * 0.34f + 160f
        for (k in 0 until 5) {
            val x = cx + (k - 2) * 110f
            val lit = 1f - kotlin.math.abs(k - 2) / 2f
            s.canvas.drawCircle(x, py, 30f, s.stroke(g, 1.6f, 160))
            if (lit > 0f) {
                s.canvas.drawCircle(x, py, 30f, s.fill(lighten(g, 0.3f)))
                if (lit < 1f) s.canvas.drawCircle(x + (if (k < 2) -1f else 1f) * lit * 60f, py, 30.5f, s.fill(Color.BLACK))
            }
        }
    }
}

object CrystalBall : Style("crystal_ball", "Crystal Ball", Category.MYSTIC) {
    override val autoPalettes = listOf("violet", "cyan", "hue", "sakura", "aurora")

    override fun draw(s: Scene) {
        s.starSky(120, s.h * 0.6f)
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.48f)
        val r = s.f(230f, 260f)
        val table = cy + r + 120f
        // Table edge and fanned tarot cards.
        s.canvas.drawLine(0f, table, s.w, table, s.stroke(s.color(0), 2f, 160))
        for (k in 0 until 5) {
            s.canvas.save()
            s.canvas.rotate(-30f + k * 15f, s.w * 0.2f, table + 260f)
            val card = RectF(s.w * 0.2f - 50f, table + 60f, s.w * 0.2f + 50f, table + 230f)
            s.canvas.drawRoundRect(card, 10f, 10f, s.fill(darken(s.color(1), 0.7f)))
            s.canvas.drawRoundRect(card, 10f, 10f, s.stroke(s.color(0), 2f))
            s.canvas.drawCircle(card.centerX(), card.centerY(), 22f, s.stroke(s.color(0), 1.4f))
            s.canvas.restore()
        }
        // Candles.
        for ((x, hgt) in listOf(cx - r - 120f to 220f, cx + r + 100f to 160f, cx + r + 190f to 110f)) {
            candle(s, x, table, hgt, 26f)
        }
        // Stand.
        val stand = Path()
        stand.moveTo(cx - 170f, table); stand.lineTo(cx - 110f, cy + r * 0.82f); stand.lineTo(cx + 110f, cy + r * 0.82f); stand.lineTo(cx + 170f, table); stand.close()
        s.canvas.drawPath(stand, s.vGradient(cy + r * 0.8f, table, darken(s.color(0), 0.4f), Color.BLACK))
        s.canvas.drawPath(stand, s.stroke(s.color(0), 2.4f))
        for (k in 0 until 3) s.canvas.drawLine(cx - 150f + k * 10f, table - 30f - k * 30f, cx + 150f - k * 10f, table - 30f - k * 30f, s.stroke(s.color(0), 1.4f, 150))
        // The ball with a nebula swirling inside.
        s.softGlow(cx, cy, r * 1.8f, s.color(1), 90)
        val ball = Path(); ball.addCircle(cx, cy, r, Path.Direction.CW)
        s.canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r, intArrayOf(darken(s.color(1), 0.4f), darken(s.color(2), 0.75f), Color.BLACK), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        })
        s.canvas.save()
        s.canvas.clipPath(ball)
        repeat(s.count(900)) {
            val arm = s.i(0, 3)
            val t = s.f()
            val a = arm * TAU / 3f + t * 5f
            val d = t * r * 0.9f
            val x = cx + cos(a) * d + s.gauss() * 18f
            val y = cy + sin(a) * d * 0.7f + s.gauss() * 14f
            s.dot(x, y, s.f(0.6f, 2.2f), lighten(s.color(arm), 0.3f), s.i(80, 230))
        }
        s.glowDot(cx, cy, 10f, Color.WHITE, 30f)
        s.canvas.restore()
        s.canvas.drawCircle(cx, cy, r, s.stroke(lighten(s.color(1), 0.5f), 3f, 220))
        s.canvas.drawArc(RectF(cx - r * 0.8f, cy - r * 0.8f, cx + r * 0.2f, cy + r * 0.2f), 190f, 70f, false, s.stroke(Color.WHITE, 10f, 140, 4f))
        s.canvas.drawCircle(cx - r * 0.45f, cy - r * 0.5f, 12f, s.fill(Color.WHITE, 200))
    }

    private fun candle(s: Scene, x: Float, base: Float, hgt: Float, half: Float) {
        val top = base - hgt
        val body = Path()
        body.moveTo(x - half, base); body.lineTo(x - half, top + 6f)
        body.quadTo(x - half * 0.5f, top - 4f, x, top + 4f)
        body.quadTo(x + half * 0.5f, top - 4f, x + half, top + 6f)
        body.lineTo(x + half, base); body.close()
        s.canvas.drawPath(body, s.vGradient(top, base, Color.rgb(240, 225, 200), Color.rgb(90, 70, 60)))
        val drip = Path(); drip.moveTo(x - half, top + 10f); drip.quadTo(x - half - 4f, top + 40f, x - half + 2f, top + 50f)
        s.canvas.drawPath(drip, s.stroke(Color.rgb(250, 240, 220), 5f))
        s.canvas.drawLine(x, top + 2f, x, top - 10f, s.stroke(Color.rgb(40, 30, 30), 2f))
        flame(s, x, top - 12f, 26f)
    }
}

/** Teardrop candle flame with a hot core and a soft halo. */
fun flame(s: Scene, x: Float, y: Float, size: Float) {
    s.softGlow(x, y - size * 0.5f, size * 6f, Color.rgb(255, 170, 60), 80)
    val f = Path()
    f.moveTo(x, y - size * 1.6f)
    f.cubicTo(x + size * 0.7f, y - size * 0.6f, x + size * 0.55f, y, x, y)
    f.cubicTo(x - size * 0.55f, y, x - size * 0.7f, y - size * 0.6f, x, y - size * 1.6f)
    f.close()
    s.canvas.drawPath(f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(x, y - size * 0.35f, size * 1.3f, intArrayOf(Color.WHITE, Color.rgb(255, 220, 120), Color.rgb(255, 120, 40)), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
    })
    s.canvas.drawOval(RectF(x - size * 0.18f, y - size * 0.45f, x + size * 0.18f, y - size * 0.05f), s.fill(Color.rgb(90, 130, 255), 180))
}

// =====================================================================
// CITY: rooftop nights
// =====================================================================

object Rooftops : Style("rooftops", "Rooftops", Category.CITY) {
    override val autoPalettes = listOf("gold", "sunset", "cyan", "sakura", "graffiti")

    override fun draw(s: Scene) {
        s.starSky(140, s.h * 0.45f)
        val moon = lighten(s.color(0), 0.6f)
        val mx = s.w * s.f(0.6f, 0.85f)
        val my = s.h * s.f(0.12f, 0.2f)
        s.softGlow(mx, my, 280f, s.color(0), 80)
        s.canvas.drawCircle(mx, my, 70f, s.fill(moon))
        // Blinking plane.
        val plx = s.f(80f, s.w - 80f)
        val ply = s.h * s.f(0.08f, 0.3f)
        s.glowDot(plx, ply, 3f, Color.rgb(255, 60, 60), 10f)
        s.dot(plx + 14f, ply, 2f, Color.WHITE)
        // Distant skyline with lit windows.
        val far = s.h * 0.62f
        var x = -10f
        while (x < s.w + 10f) {
            val bw = s.f(40f, 100f)
            val bh = s.f(120f, 420f)
            s.canvas.drawRect(x, far - bh, x + bw, far + 300f, s.fill(darken(s.color(1), 0.88f)))
            s.canvas.drawRect(x, far - bh, x + bw, far + 300f, s.stroke(s.color(1), 1f, 70))
            var wy = far - bh + 10f
            while (wy < far + 280f) {
                var wx = x + 6f
                while (wx < x + bw - 8f) {
                    if (s.chance(0.12f)) s.canvas.drawRect(wx, wy, wx + 5f, wy + 7f, s.fill(lighten(s.color(0), 0.3f), s.i(120, 240)))
                    wx += 11f
                }
                wy += 14f
            }
            x += bw + s.f(0f, 10f)
        }
        // Foreground roof.
        val roof = s.h * 0.8f
        val rim = lighten(s.color(0), 0.4f)
        s.canvas.drawRect(-10f, roof, s.w + 10f, s.h + 10f, s.fill(Color.BLACK))
        s.canvas.drawLine(0f, roof, s.w, roof, s.stroke(rim, 3f))
        s.canvas.drawLine(0f, roof + 24f, s.w, roof + 24f, s.stroke(rim, 1.4f, 140))
        // Water tower.
        val tx = s.w * s.f(0.12f, 0.28f)
        val tankTop = roof - 380f
        for (dx in floatArrayOf(-60f, -20f, 20f, 60f)) s.canvas.drawLine(tx + dx, tankTop + 200f, tx + dx * 1.3f, roof, s.stroke(rim, 4f, 220))
        s.canvas.drawLine(tx - 78f, roof - 90f, tx + 78f, roof - 60f, s.stroke(rim, 2f, 160))
        val tank = RectF(tx - 80f, tankTop, tx + 80f, tankTop + 200f)
        s.canvas.drawRect(tank, s.fill(Color.BLACK))
        s.canvas.drawRect(tank, s.stroke(rim, 3f))
        for (k in 1..3) s.canvas.drawLine(tank.left, tank.top + k * 50f, tank.right, tank.top + k * 50f, s.stroke(rim, 1.4f, 150))
        val cap = Path(); cap.moveTo(tank.left - 10f, tank.top); cap.lineTo(tx, tank.top - 90f); cap.lineTo(tank.right + 10f, tank.top); cap.close()
        s.canvas.drawPath(cap, s.fill(Color.BLACK))
        s.canvas.drawPath(cap, s.stroke(rim, 3f))
        // Antenna and dish.
        val ax = s.w * s.f(0.7f, 0.88f)
        s.canvas.drawLine(ax, roof, ax, roof - 260f, s.stroke(rim, 3f))
        for (k in 0 until 4) s.canvas.drawLine(ax - 40f + k * 6f, roof - 200f + k * 40f, ax + 40f - k * 6f, roof - 200f + k * 40f, s.stroke(rim, 2f))
        s.glowDot(ax, roof - 262f, 3f, Color.rgb(255, 60, 60), 10f)
        // String lights between the tower and the antenna.
        val cable = Path()
        val sx = tx + 80f
        val sy = tankTop + 120f
        val ex = ax
        val ey = roof - 220f
        cable.moveTo(sx, sy); cable.quadTo((sx + ex) / 2f, maxOf(sy, ey) + 140f, ex, ey)
        s.canvas.drawPath(cable, s.stroke(Color.rgb(120, 120, 130), 1.6f, 200))
        for (k in 1 until 14) {
            val t = k / 14f
            val bx = (1 - t) * (1 - t) * sx + 2 * (1 - t) * t * (sx + ex) / 2f + t * t * ex
            val by = (1 - t) * (1 - t) * sy + 2 * (1 - t) * t * (maxOf(sy, ey) + 140f) + t * t * ey
            s.glowDot(bx, by + 10f, 5f, s.color(k % 3), 14f)
        }
        // A cat on the ledge, watching the city.
        val cx = s.w * s.f(0.42f, 0.58f)
        val cat = Path()
        cat.moveTo(cx - 34f, roof)
        cat.cubicTo(cx - 44f, roof - 60f, cx - 30f, roof - 90f, cx - 18f, roof - 100f)
        cat.lineTo(cx - 26f, roof - 136f); cat.lineTo(cx - 6f, roof - 118f); cat.lineTo(cx + 8f, roof - 118f); cat.lineTo(cx + 26f, roof - 136f); cat.lineTo(cx + 20f, roof - 100f)
        cat.cubicTo(cx + 32f, roof - 90f, cx + 44f, roof - 60f, cx + 34f, roof)
        cat.close()
        s.canvas.drawPath(cat, s.fill(Color.BLACK))
        s.canvas.drawPath(cat, s.stroke(rim, 2.4f))
        val tail = Path(); tail.moveTo(cx + 30f, roof - 4f); tail.cubicTo(cx + 80f, roof, cx + 90f, roof + 40f, cx + 60f, roof + 70f)
        s.canvas.drawPath(tail, s.stroke(Color.BLACK, 9f))
        s.canvas.drawPath(tail, s.stroke(rim, 2f))
    }
}
