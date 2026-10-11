package com.nikopick.zamoled.gen

import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.nikopick.zamoled.gen.Sacred.crownOfThorns
import com.nikopick.zamoled.gen.Sacred.goldPaint
import com.nikopick.zamoled.gen.Sacred.halo
import com.nikopick.zamoled.gen.Sacred.lightShafts
import com.nikopick.zamoled.gen.Sacred.motes
import com.nikopick.zamoled.gen.Sacred.rays
import com.nikopick.zamoled.gen.Sacred.robedFigure
import com.nikopick.zamoled.gen.Sacred.sacredFrame
import com.nikopick.zamoled.gen.Sacred.thornBranch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Shared defaults for the King of Kings world. */
private abstract class SacredStyle(id: String, name: String) : Style(id, name, Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "sacred", "divine", "gold", "sunset", "lava")
}

/** Three crosses on the hill at Golgotha, against a dawn sky. */
object Calvary : Style("kok_calvary", "Calvary", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "divine", "gold", "sunset", "lava", "violet")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val sunY = s.h * s.f(0.36f, 0.44f)
        s.starSky(140, sunY - 120f)
        s.softGlow(s.cx, sunY, s.h * 0.55f, glow, 110)
        Sacred.run { s.rays(s.cx, sunY, 28, 40f, s.h * 0.75f, glow, 60, s.f(0f, 0.3f), 0.03f) }
        s.canvas.drawCircle(s.cx, sunY, s.f(80f, 120f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(s.cx, sunY, 120f, intArrayOf(Color.WHITE, lighten(glow, 0.5f), withAlpha(glow, 0)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        })

        // Hill.
        val baseY = s.h * s.f(0.78f, 0.84f)
        val (land, ridgeLine) = s.ridge(baseY + 90f, 130f, 0.0032f, s.f(0f, 40f), ridged = false, step = 7f)
        s.canvas.drawPath(land, s.fill(Color.BLACK))
        s.canvas.drawPath(ridgeLine, s.stroke(lighten(glow, 0.3f), 2.4f, 210))

        // Crosses: the middle one taller and nearer.
        val gap = s.f(150f, 200f)
        for ((dx, scale) in listOf(-gap to 0.74f, gap * 0.95f to 0.7f, 0f to 1f)) {
            val hgt = s.f(330f, 380f) * scale
            val x = s.cx + dx
            val y = baseY + (1f - scale) * 60f
            val cross = Sacred.crossPath(x, y, hgt, hgt * 0.085f, hgt * 0.52f, 0.28f)
            s.canvas.drawPath(cross, s.fill(Color.BLACK))
            s.canvas.drawPath(cross, s.stroke(lighten(glow, 0.45f), 3f * scale, 240))
            // Rim light on the side facing the sun.
            s.canvas.save()
            s.canvas.clipPath(cross)
            s.canvas.drawRect(x - hgt, y - hgt, x, y, s.fill(withAlpha(lighten(glow, 0.5f), 45)))
            s.canvas.restore()
        }
        s.mist(baseY - 60f, baseY + 120f, glow, 45)
        Sacred.run { s.motes(40, glow) }
    }
}

/** The Chi-Rho monogram between Alpha and Omega, as gold leaf on black. */
object ChiRho : Style("kok_chirho", "Chi-Rho", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "gold", "divine", "violet")

    override fun draw(s: Scene) {
        val gold = s.color(0)
        val cx = s.cx
        val cy = s.h * 0.44f
        val r = s.f(230f, 265f)
        s.softGlow(cx, cy, r * 2.6f, gold, 90)
        Sacred.run { s.rays(cx, cy, 24, r * 0.9f, r * 3.1f, gold, 50, s.f(0f, 0.3f)) }

        // Laurel ring.
        s.canvas.drawCircle(cx, cy, r * 1.08f, s.stroke(darken(gold, 0.45f), 10f, 220))
        s.canvas.drawCircle(cx, cy, r * 1.08f, s.stroke(lighten(gold, 0.3f), 2f, 200))
        for (k in 0 until 40) {
            val a = k * TAU / 40
            val lx = cx + cos(a) * r * 1.08f
            val ly = cy + sin(a) * r * 1.08f
            s.canvas.save()
            s.canvas.rotate(Math.toDegrees(a.toDouble()).toFloat() + 70f, lx, ly)
            s.canvas.drawOval(RectF(lx - 16f, ly - 6f, lx + 16f, ly + 6f), s.fill(darken(gold, 0.2f), 230))
            s.canvas.drawOval(RectF(lx - 16f, ly - 6f, lx + 16f, ly + 6f), s.stroke(lighten(gold, 0.4f), 1.2f, 180))
            s.canvas.restore()
        }

        val paint = Sacred.run { s.goldPaint(cy - r, cy + r, gold) }
        val edge = s.stroke(lighten(gold, 0.6f), 3f, 230)
        val thick = r * 0.14f
        // Chi: the X.
        for (sgn in floatArrayOf(-1f, 1f)) {
            val bar = Path()
            val a = sgn * 0.72f
            val dx = cos(PI.toFloat() / 2f - a) * r * 0.78f
            val dy = sin(PI.toFloat() / 2f - a) * r * 0.78f
            bar.moveTo(cx - dx - thick / 2f, cy - dy)
            bar.lineTo(cx - dx + thick / 2f, cy - dy)
            bar.lineTo(cx + dx + thick / 2f, cy + dy)
            bar.lineTo(cx + dx - thick / 2f, cy + dy)
            bar.close()
            s.canvas.drawPath(bar, paint)
            s.canvas.drawPath(bar, edge)
        }
        // Rho: the vertical stem with a loop at the top.
        val stem = Path()
        stem.addRect(cx - thick / 2f, cy - r * 0.95f, cx + thick / 2f, cy + r * 0.82f, Path.Direction.CW)
        s.canvas.drawPath(stem, paint)
        s.canvas.drawPath(stem, edge)
        val loopR = r * 0.3f
        val loop = Path()
        loop.addCircle(cx + loopR * 0.62f, cy - r * 0.62f, loopR, Path.Direction.CW)
        val hole = Path()
        hole.addCircle(cx + loopR * 0.62f, cy - r * 0.62f, loopR - thick, Path.Direction.CW)
        loop.op(hole, Path.Op.DIFFERENCE)
        s.canvas.drawPath(loop, paint)
        s.canvas.drawPath(loop, edge)

        // Alpha and Omega.
        val tp = s.textPaint(r * 0.42f, gold, Typeface.create(Typeface.SERIF, Typeface.BOLD))
        tp.shader = Sacred.run { s.goldPaint(cy - r * 0.2f, cy + r * 0.3f, gold) }.shader
        s.canvas.drawText("Α", cx - r * 1.55f, cy + r * 0.16f, tp)
        s.canvas.drawText("Ω", cx + r * 1.55f, cy + r * 0.16f, tp)
        val name = s.textPaint(42f, lighten(gold, 0.3f), Typeface.create(Typeface.SERIF, Typeface.BOLD)).apply { letterSpacing = 0.35f }
        s.canvas.drawText("KING OF KINGS", cx, s.h * 0.88f, name)
        Sacred.run { s.sacredFrame(42f, darken(gold, 0.3f)) }
        Sacred.run { s.motes(26, gold) }
    }
}

/** The crown of thorns, woven and lit from above. */
object ThornCrown : Style("kok_thorns", "Crown of Thorns", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "lava", "gold", "sunset")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.48f)
        val r = s.f(300f, 340f)
        Sacred.run { s.lightShafts(cx, -s.h * 0.1f, glow, 6, 0.5f, 45) }
        s.softGlow(cx, cy, r * 1.9f, glow, 85)
        // The halo of light the crown encircles.
        s.canvas.drawCircle(cx, cy, r * 0.78f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy - r * 0.2f, r * 0.9f, intArrayOf(withAlpha(lighten(glow, 0.6f), 52), withAlpha(glow, 14), Color.TRANSPARENT), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        })
        Sacred.run { s.crownOfThorns(cx, cy, r, mix(Color.rgb(104, 72, 42), glow, 0.25f), 4) }
        // A few drops at the lowest thorns.
        repeat(s.i(2, 5)) {
            val a = s.f(0.3f, PI.toFloat() - 0.3f)
            val dx = cx + cos(a) * r * s.f(0.75f, 1f)
            val dy = cy + sin(a) * r * 0.42f + r * 0.1f
            val drop = Path()
            drop.moveTo(dx, dy); drop.quadTo(dx + 9f, dy + 16f, dx, dy + 28f); drop.quadTo(dx - 9f, dy + 16f, dx, dy)
            s.softGlow(dx, dy + 16f, 40f, Sacred.CRIMSON, 120)
            s.canvas.drawPath(drop, s.fill(Sacred.CRIMSON))
            s.canvas.drawPath(drop, s.stroke(lighten(Sacred.CRIMSON, 0.4f), 1.4f, 200))
        }
        Sacred.run { s.motes(30, glow) }
    }
}

/** The Sacred Heart: crowned with thorns, crossed and burning. */
object SacredHeart : Style("kok_heart", "Sacred Heart", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "lava", "sunset", "gold")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val cx = s.cx
        val cy = s.h * s.f(0.46f, 0.52f)
        val r = s.f(190f, 215f)
        s.softGlow(cx, cy, r * 3.2f, glow, 100)
        Sacred.run { s.rays(cx, cy - r * 0.2f, 32, r * 1.1f, r * 3.6f, glow, 55, s.f(0f, 0.2f)) }

        // Flames: overlapping tongues that merge into one body of fire.
        val fireTop = cy - r * 0.82f
        repeat(18) { k ->
            val lean = s.f(-0.5f, 0.5f)
            val baseX = cx + s.gauss() * r * 0.55f
            val fh = r * s.f(0.75f, 1.75f) * (1f - abs(baseX - cx) / (r * 1.3f)).coerceIn(0.4f, 1f)
            val wid = r * s.f(0.13f, 0.26f)
            val flame = Path()
            flame.moveTo(baseX - wid, fireTop + r * 0.12f)
            flame.cubicTo(baseX - wid * 1.25f, fireTop - fh * 0.45f, baseX + lean * wid - wid * 0.5f, fireTop - fh * 0.75f, baseX + lean * wid * 1.6f, fireTop - fh)
            flame.cubicTo(baseX + lean * wid + wid * 0.8f, fireTop - fh * 0.6f, baseX + wid * 1.25f, fireTop - fh * 0.3f, baseX + wid, fireTop + r * 0.12f)
            flame.close()
            s.canvas.drawPath(flame, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, fireTop - fh, 0f, fireTop + r * 0.12f,
                    intArrayOf(withAlpha(Color.WHITE, 230), withAlpha(lighten(glow, 0.5f), 210), withAlpha(Sacred.CRIMSON, 120), withAlpha(Sacred.CRIMSON, 0)),
                    floatArrayOf(0f, 0.3f, 0.75f, 1f), Shader.TileMode.CLAMP)
                blendMode = BlendMode.PLUS
            })
        }
        s.softGlow(cx, fireTop - r * 0.45f, r * 1.3f, glow, 120)

        // The heart.
        val heart = Path()
        heart.moveTo(cx, cy + r * 0.95f)
        heart.cubicTo(cx - r * 1.5f, cy + r * 0.05f, cx - r * 0.78f, cy - r * 1.05f, cx, cy - r * 0.38f)
        heart.cubicTo(cx + r * 0.78f, cy - r * 1.05f, cx + r * 1.5f, cy + r * 0.05f, cx, cy + r * 0.95f)
        heart.close()
        s.canvas.drawPath(heart, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx - r * 0.3f, cy - r * 0.3f, r * 1.7f, intArrayOf(lighten(Sacred.CRIMSON, 0.45f), Sacred.CRIMSON, darken(Sacred.CRIMSON, 0.6f)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        })
        s.canvas.drawPath(heart, s.stroke(lighten(glow, 0.5f), 3.5f, 235))
        s.canvas.save()
        s.canvas.clipPath(heart)
        s.softGlow(cx - r * 0.35f, cy - r * 0.25f, r * 0.7f, Color.WHITE, 90)
        // The wound.
        val wound = Path()
        wound.moveTo(cx + r * 0.3f, cy - r * 0.05f)
        wound.quadTo(cx + r * 0.48f, cy + r * 0.12f, cx + r * 0.3f, cy + r * 0.3f)
        wound.quadTo(cx + r * 0.22f, cy + r * 0.12f, cx + r * 0.3f, cy - r * 0.05f)
        s.canvas.drawPath(wound, s.fill(darken(Sacred.CRIMSON, 0.65f)))
        s.canvas.drawPath(wound, s.stroke(lighten(glow, 0.3f), 2f, 200))
        s.canvas.restore()

        // Thorns around it and a small cross above the flames.
        Sacred.run { s.crownOfThorns(cx, cy + r * 0.08f, r * 0.98f, mix(Color.rgb(104, 72, 42), glow, 0.3f), 2) }
        val crossH = r * 0.62f
        val cross = Sacred.crossPath(cx, cy - r * 2.05f, crossH, crossH * 0.13f, crossH * 0.52f, 0.3f)
        s.softGlow(cx, cy - r * 2.05f - crossH * 0.5f, crossH * 1.3f, Color.WHITE, 110)
        s.canvas.drawPath(cross, Sacred.run { s.goldPaint(cy - r * 2.05f - crossH, cy - r * 2.05f, glow) })
        s.canvas.drawPath(cross, s.stroke(lighten(glow, 0.6f), 2f))
        Sacred.run { s.motes(24, glow) }
    }
}

/** The descending dove, wings spread, inside a burst of light. */
object HolyDove : Style("kok_dove", "The Dove", Category.KINGDOM) {
    override val autoPalettes = listOf("divine", "sacred", "gold", "cyan")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.48f)
        s.starSky(120, s.h * 0.75f)
        s.softGlow(cx, cy, s.h * 0.45f, glow, 110)
        Sacred.run { s.rays(cx, cy, 36, 60f, s.h * 0.62f, glow, 62, s.f(0f, 0.2f), 0.022f) }

        val size = s.f(210f, 245f)
        val white = Color.rgb(252, 250, 244)
        // Wings: long and swept, with notched primaries at the trailing edge.
        for (sgn in floatArrayOf(-1f, 1f)) {
            val wing = smoothPath(
                floatArrayOf(
                    cx + sgn * size * 0.1f, cy - size * 0.12f,
                    cx + sgn * size * 0.52f, cy - size * 0.62f,
                    cx + sgn * size * 1.05f, cy - size * 1.0f,
                    cx + sgn * size * 1.62f, cy - size * 1.2f,
                    cx + sgn * size * 1.8f, cy - size * 0.98f,
                    cx + sgn * size * 1.32f, cy - size * 0.62f,
                    cx + sgn * size * 0.86f, cy - size * 0.2f,
                    cx + sgn * size * 0.42f, cy + size * 0.04f,
                ),
                closed = true, tension = 0.4f,
            )
            s.canvas.drawPath(wing, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(cx, cy - size * 0.95f, cx, cy + size * 0.05f, white, darken(white, 0.5f), Shader.TileMode.CLAMP)
            })
            s.canvas.save()
            s.canvas.clipPath(wing)
            // Feather separations run along the wing.
            for (k in 0 until 7) {
                val t = k / 6f
                s.canvas.drawPath(
                    smoothPath(
                        floatArrayOf(
                            cx + sgn * size * (0.25f + t * 0.3f), cy - size * (0.1f + t * 0.35f),
                            cx + sgn * size * (0.75f + t * 0.45f), cy - size * (0.38f + t * 0.3f),
                            cx + sgn * size * (1.15f + t * 0.55f), cy - size * (0.5f + t * 0.33f),
                        ),
                        closed = false,
                    ),
                    s.stroke(withAlpha(darken(white, 0.6f), 150), size * 0.012f),
                )
            }
            s.canvas.restore()
            s.canvas.drawPath(wing, s.stroke(lighten(glow, 0.55f), 2.4f, 225))
        }

        // Body and tail.
        val body = smoothPath(
            floatArrayOf(
                cx, cy - size * 0.42f, cx + size * 0.16f, cy - size * 0.2f, cx + size * 0.14f, cy + size * 0.28f,
                cx + size * 0.05f, cy + size * 0.72f, cx - size * 0.05f, cy + size * 0.72f, cx - size * 0.14f, cy + size * 0.28f,
                cx - size * 0.16f, cy - size * 0.2f,
            ),
            closed = true,
        )
        s.canvas.drawPath(body, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - size * 0.16f, 0f, cx + size * 0.16f, 0f, white, darken(white, 0.4f), Shader.TileMode.CLAMP)
        })
        s.canvas.drawPath(body, s.stroke(lighten(glow, 0.5f), 2.2f, 210))
        val tail = Path()
        tail.moveTo(cx - size * 0.1f, cy + size * 0.62f)
        tail.lineTo(cx - size * 0.22f, cy + size * 1.05f)
        tail.lineTo(cx, cy + size * 0.92f)
        tail.lineTo(cx + size * 0.22f, cy + size * 1.05f)
        tail.lineTo(cx + size * 0.1f, cy + size * 0.62f)
        tail.close()
        s.canvas.drawPath(tail, s.fill(darken(white, 0.18f)))
        s.canvas.drawPath(tail, s.stroke(lighten(glow, 0.5f), 2f, 200))
        // Head and beak.
        s.canvas.drawCircle(cx, cy - size * 0.5f, size * 0.13f, s.fill(white))
        s.canvas.drawCircle(cx, cy - size * 0.5f, size * 0.13f, s.stroke(lighten(glow, 0.5f), 2f, 200))
        val beak = Path()
        beak.moveTo(cx + size * 0.11f, cy - size * 0.52f)
        beak.lineTo(cx + size * 0.25f, cy - size * 0.47f)
        beak.lineTo(cx + size * 0.11f, cy - size * 0.43f)
        beak.close()
        s.canvas.drawPath(beak, s.fill(lighten(glow, 0.2f)))
        s.canvas.drawCircle(cx + size * 0.05f, cy - size * 0.53f, size * 0.022f, s.fill(Color.BLACK))
        // Olive sprig in the beak.
        val stemPts = floatArrayOf(cx + size * 0.24f, cy - size * 0.47f, cx + size * 0.58f, cy - size * 0.36f, cx + size * 0.92f, cy - size * 0.44f)
        s.canvas.drawPath(smoothPath(stemPts, closed = false), s.stroke(Color.rgb(108, 158, 98), size * 0.022f))
        for (k in 0 until 5) {
            val t = 0.2f + k * 0.18f
            val lx = cx + size * (0.24f + t * 0.7f)
            val ly = cy - size * (0.42f + (if (k % 2 == 0) 0.06f else -0.03f))
            s.canvas.save()
            s.canvas.rotate(if (k % 2 == 0) -34f else 30f, lx, ly)
            s.canvas.drawOval(RectF(lx - size * 0.085f, ly - size * 0.034f, lx + size * 0.085f, ly + size * 0.034f), s.fill(Color.rgb(126, 182, 112)))
            s.canvas.drawOval(RectF(lx - size * 0.085f, ly - size * 0.034f, lx + size * 0.085f, ly + size * 0.034f), s.stroke(Color.rgb(176, 220, 150), size * 0.008f, 200))
            s.canvas.restore()
        }
        Sacred.run { s.motes(30, glow) }
    }
}

/** The risen Christ, arms open, in a mandorla of light. */
object TheRisen : Style("kok_risen", "The Risen", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "divine", "gold", "sunset")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val cx = s.cx
        val baseY = s.h * s.f(0.86f, 0.9f)
        val height = s.h * s.f(0.56f, 0.62f)
        val midY = baseY - height * 0.55f
        s.starSky(110, s.h * 0.6f)
        // Mandorla: the almond of light around the whole figure.
        val mand = Path()
        mand.moveTo(cx, midY - height * 0.72f)
        mand.quadTo(cx + height * 0.46f, midY, cx, midY + height * 0.72f)
        mand.quadTo(cx - height * 0.46f, midY, cx, midY - height * 0.72f)
        mand.close()
        s.canvas.drawPath(mand, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, midY, height * 0.75f, intArrayOf(withAlpha(glow, 90), withAlpha(glow, 40), Color.TRANSPARENT), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        })
        s.canvas.drawPath(mand, s.stroke(lighten(glow, 0.5f), 3f, 200))
        s.canvas.drawPath(mand, s.stroke(glow, 14f, 60, 14f))
        Sacred.run { s.rays(cx, midY, 30, height * 0.3f, height * 1.15f, glow, 48, s.f(0f, 0.2f)) }

        Sacred.run { s.robedFigure(cx, baseY, height, mix(Sacred.CRIMSON, Color.rgb(40, 30, 60), 0.45f), lighten(glow, 0.45f), true, glow) }

        // Clouds at the hem.
        repeat(s.i(3, 6)) {
            val ccx = s.f(0f, s.w)
            val ccy = baseY + s.f(-20f, 90f)
            for (k in 0 until 4) s.canvas.drawCircle(ccx + k * 46f - 69f, ccy - if (k % 3 == 0) 0f else 22f, if (k % 3 == 0) 30f else 42f, s.fill(darken(glow, 0.72f), 190))
        }
        s.canvas.drawRect(0f, baseY + 40f, s.w, s.h, s.vGradient(baseY + 40f, s.h, Color.TRANSPARENT, Color.BLACK))
        Sacred.run { s.motes(34, glow) }
    }
}

/** Agnus Dei: the Lamb with the banner of victory. */
object LambOfGod : Style("kok_lamb", "Lamb of God", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "divine", "gold")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val u = s.f(108f, 120f)
        val cx = s.cx - u * 0.58f + s.f(-12f, 12f)
        val cy = s.h * s.f(0.48f, 0.54f)
        s.starSky(120, s.h * 0.7f)
        s.softGlow(cx, cy - u, u * 5.5f, glow, 95)
        Sacred.run { s.rays(cx, cy - u * 0.9f, 26, u * 1.6f, u * 6f, glow, 45, s.f(0f, 0.3f)) }
        val wool = Color.rgb(248, 244, 236)

        // Banner on a slender cross-staff.
        val staffX = cx + u * 1.15f
        s.canvas.drawLine(staffX, cy + u * 1.25f, staffX, cy - u * 3.1f, s.stroke(lighten(glow, 0.3f), u * 0.07f))
        s.canvas.drawLine(staffX - u * 0.3f, cy - u * 2.55f, staffX + u * 0.3f, cy - u * 2.55f, s.stroke(lighten(glow, 0.3f), u * 0.07f))
        val flag = smoothPath(
            floatArrayOf(staffX, cy - u * 2.9f, staffX + u * 1.5f, cy - u * 2.75f, staffX + u * 1.35f, cy - u * 1.75f, staffX, cy - u * 1.6f),
            closed = true, tension = 0.3f,
        )
        s.canvas.drawPath(flag, s.fill(Color.rgb(250, 248, 242)))
        s.canvas.drawPath(flag, s.stroke(lighten(glow, 0.45f), 2.2f, 220))
        val bar = s.fill(Sacred.CRIMSON)
        s.canvas.drawRect(staffX + u * 0.6f, cy - u * 2.85f, staffX + u * 0.82f, cy - u * 1.66f, bar)
        s.canvas.drawRect(staffX + u * 0.18f, cy - u * 2.4f, staffX + u * 1.4f, cy - u * 2.18f, bar)

        // Lamb: a fleece of overlapping curls on four slim legs.
        for (dx in floatArrayOf(-0.55f, -0.2f, 0.45f, 0.8f)) {
            s.canvas.drawLine(cx + u * dx, cy + u * 0.35f, cx + u * dx * 1.05f, cy + u * 1.25f, s.stroke(darken(wool, 0.65f), u * 0.11f))
            s.canvas.drawLine(cx + u * dx * 1.05f, cy + u * 1.2f, cx + u * dx * 1.05f, cy + u * 1.3f, s.stroke(darken(wool, 0.8f), u * 0.15f))
        }
        val body = Path()
        body.addOval(RectF(cx - u * 1.1f, cy - u * 0.65f, cx + u * 1.05f, cy + u * 0.55f), Path.Direction.CW)
        s.canvas.drawPath(body, s.fill(wool))
        s.canvas.save()
        s.canvas.clipPath(body)
        for (k in 0 until 26) {
            val ax = cx + s.f(-1.05f, 1.0f) * u
            val ay = cy + s.f(-0.6f, 0.5f) * u
            s.canvas.drawCircle(ax, ay, u * s.f(0.14f, 0.24f), s.stroke(darken(wool, 0.22f), u * 0.035f, 220))
        }
        s.canvas.restore()
        s.canvas.drawPath(body, s.stroke(lighten(glow, 0.4f), 2.4f, 215))
        // Head turned toward us, with the cruciform halo behind.
        val hx = cx - u * 1.05f
        val hy = cy - u * 0.55f
        Sacred.run { s.halo(hx, hy, u * 0.72f, glow, crossed = true) }
        val head = Path()
        head.addOval(RectF(hx - u * 0.42f, hy - u * 0.34f, hx + u * 0.4f, hy + u * 0.42f), Path.Direction.CW)
        s.canvas.drawPath(head, s.fill(wool))
        s.canvas.drawPath(head, s.stroke(lighten(glow, 0.4f), 2f, 210))
        for (sgn in floatArrayOf(-1f, 1f)) {
            s.canvas.drawOval(RectF(hx - u * 0.6f + (if (sgn > 0) u * 0.78f else 0f), hy - u * 0.12f, hx - u * 0.3f + (if (sgn > 0) u * 0.78f else 0f), hy + u * 0.08f), s.fill(darken(wool, 0.25f)))
        }
        s.canvas.drawCircle(hx - u * 0.12f, hy - u * 0.02f, u * 0.055f, s.fill(Color.rgb(50, 40, 34)))
        s.canvas.drawCircle(hx + u * 0.18f, hy - u * 0.02f, u * 0.055f, s.fill(Color.rgb(50, 40, 34)))
        Sacred.run { s.motes(26, glow) }
    }
}

/** An open Bible with light rising from the pages. */
object OpenWord : Style("kok_word", "The Word", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "gold", "divine")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val cx = s.cx
        val cy = s.h * s.f(0.6f, 0.66f)
        val half = s.f(320f, 360f)
        Sacred.run { s.rays(cx, cy - half * 0.1f, 30, half * 0.2f, half * 3.2f, glow, 55, s.f(0f, 0.3f), 0.025f) }
        s.softGlow(cx, cy - half * 0.15f, half * 1.9f, glow, 110)

        val page = Color.rgb(250, 244, 230)
        for (sgn in floatArrayOf(-1f, 1f)) {
            val pagePath = smoothPath(
                floatArrayOf(
                    cx, cy - half * 0.12f,
                    cx + sgn * half * 0.55f, cy - half * 0.3f,
                    cx + sgn * half, cy - half * 0.18f,
                    cx + sgn * half * 0.96f, cy + half * 0.26f,
                    cx + sgn * half * 0.5f, cy + half * 0.36f,
                    cx, cy + half * 0.2f,
                ),
                closed = true, tension = 0.35f,
            )
            s.canvas.drawPath(pagePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(cx, 0f, cx + sgn * half, 0f, page, darken(page, 0.4f), Shader.TileMode.CLAMP)
            })
            s.canvas.save()
            s.canvas.clipPath(pagePath)
            for (k in 0 until 11) {
                val t = k / 10f
                val ly = cy - half * 0.18f + t * half * 0.44f
                val inset = half * (0.1f + 0.06f * kotlin.math.sin(t * 3f))
                s.canvas.drawLine(cx + sgn * inset, ly, cx + sgn * (half * 0.88f - inset * 0.5f), ly - half * 0.06f * (1f - t), s.stroke(Color.rgb(120, 104, 86), 2.2f, 190))
            }
            s.canvas.restore()
            s.canvas.drawPath(pagePath, s.stroke(darken(glow, 0.2f), 2.6f, 230))
            // Gilt page edges.
            s.canvas.drawPath(
                smoothPath(floatArrayOf(cx + sgn * half, cy - half * 0.18f, cx + sgn * half * 0.96f, cy + half * 0.26f, cx + sgn * half * 0.5f, cy + half * 0.36f), closed = false),
                s.stroke(lighten(glow, 0.3f), 7f, 230),
            )
        }
        // Spine and ribbon.
        s.canvas.drawPath(smoothPath(floatArrayOf(cx, cy - half * 0.12f, cx, cy + half * 0.2f), closed = false), s.stroke(darken(glow, 0.45f), 5f))
        val ribbon = smoothPath(floatArrayOf(cx + half * 0.12f, cy + half * 0.1f, cx + half * 0.2f, cy + half * 0.45f, cx + half * 0.1f, cy + half * 0.66f), closed = false)
        s.canvas.drawPath(ribbon, s.stroke(Sacred.CRIMSON, 9f))

        // A cross of light standing on the open page.
        val ch = half * 0.95f
        val cross = Sacred.crossPath(cx, cy - half * 0.12f, ch, ch * 0.1f, ch * 0.48f, 0.3f)
        s.softGlow(cx, cy - half * 0.5f, half * 0.9f, Color.WHITE, 90)
        s.canvas.drawPath(cross, Sacred.run { s.goldPaint(cy - half * 0.12f - ch, cy - half * 0.12f, glow) })
        s.canvas.drawPath(cross, s.stroke(lighten(glow, 0.6f), 2.6f, 235))
        Sacred.run { s.motes(30, glow) }
    }
}

/** Hands joined in prayer, lit from above. */
object PrayingHands : Style("kok_hands", "Praying Hands", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "divine", "gold", "violet")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val cx = s.cx
        val baseY = s.h * s.f(0.74f, 0.79f)
        val len = s.f(540f, 590f)
        val hw = len * 0.23f                   // half-width of one hand
        val skin = Color.rgb(238, 214, 188)
        Sacred.run { s.lightShafts(cx + s.f(-80f, 80f), -s.h * 0.05f, glow, 6, 0.45f, 50) }
        s.softGlow(cx, baseY - len * 0.6f, len * 1.15f, glow, 85)

        fun palm(sgn: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx + sgn * hw * 0.1f, 0f, cx + sgn * hw * 2.2f, 0f,
                lighten(skin, 0.18f), darken(skin, 0.62f), Shader.TileMode.CLAMP,
            )
        }

        for (sgn in floatArrayOf(-1f, 1f)) {
            val hx = cx + sgn * hw          // centre of this hand
            s.canvas.save()
            s.canvas.rotate(-sgn * 9f, hx, baseY)     // tip the pair together

            // Palm and wrist.
            val palmTop = baseY - len * 0.46f
            s.canvas.drawRoundRect(
                RectF(hx - hw * 0.98f, palmTop, hx + hw * 0.98f, baseY + len * 0.04f),
                hw * 0.42f, hw * 0.42f, palm(sgn),
            )

            // Four fingers, longest in the middle, each a rounded capsule.
            val lengths = floatArrayOf(0.37f, 0.46f, 0.43f, 0.32f)
            for (k in 0 until 4) {
                val t = if (sgn > 0) k else 3 - k            // little finger to the outside
                val fw = hw * 0.44f
                val fxp = hx + (t - 1.5f) * sgn * hw * 0.48f
                val top = palmTop - len * lengths[k]
                val r = RectF(fxp - fw * 0.5f, top, fxp + fw * 0.5f, palmTop + len * 0.08f)
                s.canvas.drawRoundRect(r, fw * 0.5f, fw * 0.5f, palm(sgn))
                s.canvas.drawRoundRect(r, fw * 0.5f, fw * 0.5f, s.stroke(darken(skin, 0.62f), 2.2f, 150))
                // Knuckle creases.
                for (j in 1..2) {
                    val ky = top + (palmTop - top) * (0.34f * j)
                    s.canvas.drawLine(fxp - fw * 0.3f, ky, fxp + fw * 0.3f, ky, s.stroke(darken(skin, 0.55f), 1.8f, 110))
                }
            }

            // Thumb, folded across the base of the palm.
            val thumb = smoothPath(
                floatArrayOf(
                    hx + sgn * hw * 0.9f, baseY - len * 0.4f,
                    hx + sgn * hw * 0.55f, baseY - len * 0.26f,
                    hx - sgn * hw * 0.25f, baseY - len * 0.17f,
                    hx - sgn * hw * 0.3f, baseY - len * 0.05f,
                    hx + sgn * hw * 0.7f, baseY - len * 0.1f,
                    hx + sgn * hw * 1.0f, baseY - len * 0.24f,
                ),
                closed = true, tension = 0.4f,
            )
            s.canvas.drawPath(thumb, palm(sgn))
            s.canvas.drawPath(thumb, s.stroke(darken(skin, 0.6f), 2.4f, 170))

            // Outer rim light down the back of the hand.
            s.canvas.drawLine(
                hx + sgn * hw * 0.95f, palmTop - len * 0.05f, hx + sgn * hw * 0.95f, baseY - len * 0.02f,
                s.stroke(lighten(glow, 0.45f), 3f, 180),
            )

            // Sleeve cuff at the wrist.
            val cuff = smoothPath(
                floatArrayOf(
                    cx + sgn * len * 0.01f, baseY - len * 0.02f, hx + sgn * hw * 1.2f, baseY - len * 0.06f,
                    hx + sgn * hw * 1.65f, baseY + len * 0.2f, cx + sgn * len * 0.01f, baseY + len * 0.22f,
                ),
                closed = true, tension = 0.3f,
            )
            s.canvas.drawPath(cuff, s.fill(darken(mix(glow, Color.rgb(40, 34, 60), 0.6f), 0.3f)))
            s.canvas.drawPath(cuff, s.stroke(lighten(glow, 0.4f), 2.4f, 215))
            s.canvas.restore()
        }

        // A rosary hanging from the hands.
        if (s.chance(0.6f)) {
            val beads = smoothPath(floatArrayOf(cx + len * 0.16f, baseY + len * 0.06f, cx + len * 0.32f, baseY + len * 0.24f, cx + len * 0.22f, baseY + len * 0.42f), closed = false)
            s.canvas.drawPath(beads, s.stroke(darken(glow, 0.4f), 2f, 190))
            for (k in 0 until 7) {
                val t = k / 6f
                val bx = cx + len * (0.16f + t * 0.18f - t * t * 0.12f)
                val by = baseY + len * 0.06f + t * len * 0.36f
                s.canvas.drawCircle(bx, by, 5f, s.fill(lighten(glow, 0.2f)))
            }
        }
        s.canvas.drawRect(0f, baseY + len * 0.2f, s.w, s.h, s.vGradient(baseY + len * 0.2f, s.h, Color.TRANSPARENT, Color.BLACK))
        Sacred.run { s.motes(28, glow) }
    }
}

/** The empty tomb at dawn: the stone rolled away and light pouring out. */
object EmptyTomb : Style("kok_tomb", "Empty Tomb", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "divine", "gold", "sunset")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        s.starSky(140, s.h * 0.45f)
        val groundY = s.h * s.f(0.76f, 0.82f)
        // The rock face.
        val face = Path()
        face.moveTo(-10f, groundY + 40f)
        face.lineTo(-10f, s.h * 0.2f)
        var x = -10f
        while (x < s.w + 10f) {
            face.lineTo(x, s.h * 0.2f + s.noise.fbm(x * 0.004f, 3f) * 90f)
            x += 14f
        }
        face.lineTo(s.w + 10f, groundY + 40f)
        face.close()
        s.canvas.drawPath(face, s.vGradient(s.h * 0.2f, groundY * 0.8f, darken(glow, 0.93f), Color.BLACK))
        s.canvas.drawPath(face, s.stroke(darken(glow, 0.5f), 2f, 140))

        // The tomb mouth, with light spilling from inside.
        val mx = s.cx + s.f(-40f, 40f)
        val mw = s.f(150f, 180f)
        val mh = s.f(260f, 300f)
        val mouth = Path()
        mouth.moveTo(mx - mw, groundY)
        mouth.lineTo(mx - mw, groundY - mh * 0.45f)
        mouth.cubicTo(mx - mw, groundY - mh, mx + mw, groundY - mh, mx + mw, groundY - mh * 0.45f)
        mouth.lineTo(mx + mw, groundY)
        mouth.close()
        s.softGlow(mx, groundY - mh * 0.35f, mh * 2.4f, glow, 120)
        s.canvas.drawPath(mouth, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(mx, groundY - mh * 0.3f, mh, intArrayOf(Color.WHITE, lighten(glow, 0.4f), darken(glow, 0.3f)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        })
        s.canvas.drawPath(mouth, s.stroke(lighten(glow, 0.5f), 3f, 230))
        // Light spilling onto the ground.
        val spill = Path()
        spill.moveTo(mx - mw, groundY)
        spill.lineTo(mx - mw * 2.6f, s.h)
        spill.lineTo(mx + mw * 2.6f, s.h)
        spill.lineTo(mx + mw, groundY)
        spill.close()
        s.canvas.drawPath(spill, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, groundY, 0f, s.h, withAlpha(glow, 150), withAlpha(glow, 0), Shader.TileMode.CLAMP)
            blendMode = BlendMode.PLUS
        })

        // The great stone, rolled aside.
        val sx = mx + mw * s.f(2.0f, 2.5f) * (if (s.chance(0.5f)) 1f else -1f)
        val sr = mh * 0.52f
        s.canvas.drawOval(RectF(sx - sr * 1.15f, groundY - sr * 0.2f, sx + sr * 1.15f, groundY + sr * 0.28f), s.fill(Color.BLACK, 200))
        s.canvas.drawCircle(sx, groundY - sr * 0.65f, sr, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(sx - sr * 0.4f, groundY - sr, sr * 1.8f, intArrayOf(Color.rgb(96, 88, 78), Color.rgb(34, 30, 28), Color.BLACK), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        })
        s.canvas.drawCircle(sx, groundY - sr * 0.65f, sr, s.stroke(lighten(glow, 0.2f), 2.4f, 200))
        repeat(7) {
            val a = s.f(0f, TAU)
            val d = s.f(sr * 0.2f, sr * 0.75f)
            s.canvas.drawArc(RectF(sx + cos(a) * d - sr * 0.2f, groundY - sr * 0.65f + sin(a) * d - sr * 0.12f, sx + cos(a) * d + sr * 0.2f, groundY - sr * 0.65f + sin(a) * d + sr * 0.12f), s.f(0f, 360f), 150f, false, s.stroke(Color.rgb(70, 64, 58), 2f, 180))
        }
        // Linen cloths left behind, catching the light.
        val lx = mx + s.f(-40f, 40f)
        s.canvas.drawPath(
            smoothPath(floatArrayOf(lx - 70f, groundY + 24f, lx - 20f, groundY + 6f, lx + 40f, groundY + 20f, lx + 80f, groundY + 44f, lx, groundY + 52f, lx - 60f, groundY + 44f), closed = true),
            s.fill(Color.rgb(242, 238, 228), 230),
        )
        s.mist(groundY - 30f, s.h, glow, 50)
        Sacred.run { s.motes(26, glow) }
    }
}

/** A rose window blazing above a cross. */
object RoseWindow : Style("kok_window", "Rose Window", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "divine", "violet", "ocean", "gold")

    override fun draw(s: Scene) {
        val cx = s.cx
        val cy = s.h * s.f(0.36f, 0.42f)
        val r = s.f(330f, 380f)
        s.softGlow(cx, cy, r * 2.0f, s.color(0), 72)
        val lead = Color.BLACK

        // Petals in two rings.
        for (ring in 0 until 2) {
            val n = if (ring == 0) 8 else 16
            val r0 = if (ring == 0) r * 0.3f else r * 0.62f
            val r1 = if (ring == 0) r * 0.62f else r * 0.95f
            for (k in 0 until n) {
                val a0 = k * TAU / n
                val a1 = (k + 1) * TAU / n
                val pane = Path()
                pane.moveTo(cx + cos(a0) * r0, cy + sin(a0) * r0)
                pane.arcTo(RectF(cx - r1, cy - r1, cx + r1, cy + r1), Math.toDegrees(a0.toDouble()).toFloat(), Math.toDegrees((a1 - a0).toDouble()).toFloat(), false)
                pane.lineTo(cx + cos(a1) * r0, cy + sin(a1) * r0)
                pane.arcTo(RectF(cx - r0, cy - r0, cx + r0, cy + r0), Math.toDegrees(a1.toDouble()).toFloat(), -Math.toDegrees((a1 - a0).toDouble()).toFloat(), false)
                pane.close()
                val col = s.color(k + ring)
                s.canvas.drawPath(pane, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(cx, cy, r1, intArrayOf(lighten(col, 0.35f), col, darken(col, 0.45f)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                })
                s.canvas.drawPath(pane, s.stroke(lead, r * 0.022f))
            }
        }
        // Tracery rings and spokes.
        for (rr in floatArrayOf(0.3f, 0.62f, 0.95f, 1.02f)) s.canvas.drawCircle(cx, cy, r * rr, s.stroke(lead, r * 0.03f))
        s.canvas.drawCircle(cx, cy, r * 1.02f, s.stroke(darken(s.color(0), 0.2f), r * 0.012f, 220))
        // The heart of the window: a cross of white light.
        s.canvas.drawCircle(cx, cy, r * 0.3f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 0.3f, intArrayOf(Color.WHITE, lighten(s.color(0), 0.5f), darken(s.color(0), 0.2f)), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        })
        val ch = r * 0.46f
        val cross = Sacred.crossPath(cx, cy + ch * 0.5f, ch, ch * 0.16f, ch * 0.62f, 0.32f)
        s.canvas.drawPath(cross, s.fill(lead))
        s.canvas.drawPath(cross, s.stroke(lighten(s.color(0), 0.6f), 2.4f, 200))

        // Light thrown down the wall below.
        val beamTop = cy + r * 1.02f
        s.softGlow(cx, beamTop + r * 0.5f, r * 1.5f, s.color(0), 46)
        s.softGlow(cx, beamTop + r * 1.6f, r * 1.2f, s.color(1), 26)
        Sacred.run { s.motes(30, s.color(0)) }
    }
}

/** The Good Shepherd on the hillside with his flock. */
object GoodShepherd : Style("kok_shepherd", "Good Shepherd", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "divine", "gold", "sunset")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        s.starSky(200, s.h * 0.62f)
        val moonX = s.w * s.f(0.2f, 0.8f)
        val moonY = s.h * s.f(0.15f, 0.22f)
        s.softGlow(moonX, moonY, 300f, glow, 90)
        s.canvas.drawCircle(moonX, moonY, 70f, s.fill(lighten(glow, 0.55f)))

        val baseY = s.h * s.f(0.78f, 0.84f)
        val (land, line) = s.ridge(baseY + 80f, 120f, 0.003f, s.f(0f, 30f), ridged = false, step = 8f)
        s.canvas.drawPath(land, s.fill(Color.BLACK))
        s.canvas.drawPath(line, s.stroke(lighten(glow, 0.3f), 2.2f, 200))

        // The flock.
        repeat(s.i(4, 8)) {
            val sx = s.f(60f, s.w - 60f)
            val sy = baseY + s.f(-10f, 60f)
            val u = s.f(26f, 40f)
            s.canvas.drawOval(RectF(sx - u, sy - u * 0.6f, sx + u, sy + u * 0.5f), s.fill(Color.rgb(236, 232, 224), 235))
            s.canvas.drawOval(RectF(sx - u, sy - u * 0.6f, sx + u, sy + u * 0.5f), s.stroke(lighten(glow, 0.3f), 1.6f, 200))
            s.canvas.drawCircle(sx - u * 0.95f, sy - u * 0.45f, u * 0.34f, s.fill(Color.rgb(60, 54, 48)))
            for (dx in floatArrayOf(-0.5f, 0.45f)) s.canvas.drawLine(sx + u * dx, sy + u * 0.4f, sx + u * dx, sy + u * 0.9f, s.stroke(Color.rgb(60, 54, 48), u * 0.1f))
        }

        // The shepherd, staff in hand, a lamb across his shoulders.
        val fx = s.cx + s.f(-120f, 120f)
        val height = s.h * 0.3f
        val u = height / 7.2f
        Sacred.run { s.robedFigure(fx, baseY + 30f, height, mix(Color.rgb(60, 70, 110), glow, 0.2f), lighten(glow, 0.4f), false, glow) }
        val staff = smoothPath(
            floatArrayOf(
                fx + u * 1.5f, baseY + 40f, fx + u * 1.68f, baseY - height * 0.55f, fx + u * 1.75f, baseY - height * 0.95f,
                fx + u * 2.25f, baseY - height * 1.04f, fx + u * 2.42f, baseY - height * 0.88f, fx + u * 2.1f, baseY - height * 0.79f,
            ),
            closed = false,
        )
        s.canvas.drawPath(staff, s.stroke(Color.rgb(96, 68, 42), u * 0.17f))
        s.canvas.drawPath(staff, s.stroke(lighten(glow, 0.35f), u * 0.05f, 210))
        // The lamb he carries.
        val lx = fx - u * 0.25f
        val ly = baseY - height + u * 1.62f
        val lamb = RectF(lx - u * 0.92f, ly - u * 0.3f, lx + u * 0.82f, ly + u * 0.34f)
        s.canvas.drawOval(lamb, s.fill(Color.rgb(242, 238, 230)))
        s.canvas.drawOval(lamb, s.stroke(lighten(glow, 0.4f), 1.8f, 215))
        for (k in 0 until 9) {
            s.canvas.drawCircle(lx + s.f(-0.85f, 0.75f) * u, ly + s.f(-0.24f, 0.26f) * u, u * 0.14f, s.stroke(Color.rgb(206, 200, 188), u * 0.035f, 200))
        }
        // Head and dangling forelegs, hanging over the near shoulder.
        s.canvas.drawCircle(lx - u * 1.02f, ly + u * 0.16f, u * 0.27f, s.fill(Color.rgb(58, 50, 44)))
        for (dx in floatArrayOf(-0.72f, -0.42f)) {
            s.canvas.drawLine(lx + u * dx, ly + u * 0.26f, lx + u * (dx - 0.08f), ly + u * 0.78f, s.stroke(Color.rgb(58, 50, 44), u * 0.09f))
        }
        s.mist(baseY - 40f, s.h, glow, 45)
        Sacred.run { s.motes(24, glow) }
    }
}

/** The ichthys, the early Christian fish, cut from light. */
object Ichthys : Style("kok_fish", "Ichthys", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "divine", "ocean", "gold", "cyan")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val cx = s.cx
        val cy = s.h * s.f(0.42f, 0.48f)
        val len = s.f(360f, 410f)
        s.softGlow(cx, cy, len * 1.6f, glow, 100)
        Sacred.run { s.rays(cx, cy, 22, len * 0.5f, len * 2.6f, glow, 42, s.f(0f, 0.3f)) }

        // The two crossing arcs of the ichthys.
        val body = Path()
        body.moveTo(cx - len * 0.95f, cy - len * 0.34f)
        body.quadTo(cx + len * 0.35f, cy - len * 0.62f, cx + len * 0.98f, cy)
        body.quadTo(cx + len * 0.35f, cy + len * 0.62f, cx - len * 0.95f, cy + len * 0.34f)
        body.quadTo(cx - len * 0.38f, cy, cx - len * 0.95f, cy - len * 0.34f)
        body.close()
        s.canvas.drawPath(body, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - len, cy - len * 0.5f, cx + len, cy + len * 0.5f, intArrayOf(withAlpha(lighten(glow, 0.5f), 70), withAlpha(glow, 30), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        })
        s.canvas.drawPath(body, s.stroke(glow, 22f, 60, 22f))
        s.canvas.drawPath(body, s.stroke(lighten(glow, 0.45f), 9f, 235))
        s.canvas.drawPath(body, s.stroke(Color.WHITE, 3f, 220))
        s.canvas.drawCircle(cx + len * 0.55f, cy - len * 0.1f, 9f, s.fill(Color.WHITE))

        // ΙΧΘΥΣ along the body.
        val tp = s.textPaint(len * 0.17f, lighten(glow, 0.3f), Typeface.create(Typeface.SERIF, Typeface.BOLD)).apply { letterSpacing = 0.3f }
        s.canvas.drawText("ΙΧΘΥΣ", cx - len * 0.1f, cy + len * 0.06f, tp)

        // Water below.
        for (k in 0 until 7) {
            val wy = s.h * 0.74f + k * 28f
            val wave = Path()
            var x = -20f
            wave.moveTo(x, wy)
            while (x < s.w + 20f) {
                x += 14f
                wave.lineTo(x, wy + sin(x * 0.012f + k * 0.8f) * 11f)
            }
            s.canvas.drawPath(wave, s.stroke(mix(glow, Color.rgb(90, 150, 220), 0.5f), 2.4f, 190 - k * 20))
        }
        Sacred.run { s.motes(26, glow) }
    }
}

object KingOfKings {
    val styles: List<Style> = listOf(
        TheRisen, Calvary, ChiRho, ThornCrown, SacredHeart, HolyDove,
        LambOfGod, OpenWord, PrayingHands, EmptyTomb, RoseWindow, GoodShepherd, Ichthys,
        TheName, RedLetters,
    )
}
