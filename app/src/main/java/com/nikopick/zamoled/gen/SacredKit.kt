package com.nikopick.zamoled.gen

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Shared pieces for the King of Kings world: crosses, haloes, light and lettering. */
object Sacred {
    /** Warm gold used for most of the sacred artwork when the palette has no strong opinion. */
    val GOLD = Color.rgb(255, 205, 110)
    val DEEP_GOLD = Color.rgb(196, 140, 48)
    val PARCHMENT = Color.rgb(246, 232, 200)
    val CRIMSON = Color.rgb(176, 32, 48)

    /**
     * A Latin cross standing on (cx, baseY). [height] is the full height; [thick] the beam width;
     * [span] the total width of the crossbar; [armT] where the bar sits, 0 at the top, 1 at the base.
     */
    fun crossPath(cx: Float, baseY: Float, height: Float, thick: Float, span: Float, armT: Float = 0.3f): Path {
        val top = baseY - height
        val armY = top + height * armT
        val h = thick / 2f
        val s = span / 2f
        val p = Path()
        p.moveTo(cx - h, baseY)
        p.lineTo(cx - h, armY + h)
        p.lineTo(cx - s, armY + h)
        p.lineTo(cx - s, armY - h)
        p.lineTo(cx - h, armY - h)
        p.lineTo(cx - h, top)
        p.lineTo(cx + h, top)
        p.lineTo(cx + h, armY - h)
        p.lineTo(cx + s, armY - h)
        p.lineTo(cx + s, armY + h)
        p.lineTo(cx + h, armY + h)
        p.lineTo(cx + h, baseY)
        p.close()
        return p
    }

    /** Straight beams of light fanning out from a point, alternating long and short. */
    fun Scene.rays(x: Float, y: Float, count: Int, r0: Float, r1: Float, color: Int, alpha: Int, spin: Float = 0f, taper: Float = 0.02f) {
        for (k in 0 until count) {
            val a = spin + k * TAU / count
            val long = k % 2 == 0
            val len = if (long) r1 else r1 * 0.55f
            val wedge = taper * (if (long) 1f else 0.6f)
            val p = Path()
            p.moveTo(x + cos(a - wedge) * r0, y + sin(a - wedge) * r0)
            p.lineTo(x + cos(a) * len, y + sin(a) * len)
            p.lineTo(x + cos(a + wedge) * r0, y + sin(a + wedge) * r0)
            p.close()
            canvas.drawPath(
                p,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        x, y, len,
                        intArrayOf(withAlpha(color, alpha), withAlpha(color, (alpha * 0.45f).toInt()), Color.TRANSPARENT),
                        floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
                    )
                    blendMode = BlendMode.PLUS
                },
            )
        }
    }

    /** A halo: filled disc of light plus a bright ring, the way it is painted on an icon. */
    fun Scene.halo(x: Float, y: Float, r: Float, color: Int, crossed: Boolean = false) {
        softGlow(x, y, r * 2.2f, color, 120)
        canvas.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                x, y, r,
                intArrayOf(withAlpha(color, 70), withAlpha(color, 110), withAlpha(lighten(color, 0.4f), 220), withAlpha(color, 0)),
                floatArrayOf(0f, 0.72f, 0.92f, 1f), Shader.TileMode.CLAMP,
            )
        })
        canvas.drawCircle(x, y, r * 0.92f, stroke(lighten(color, 0.55f), r * 0.035f, 230))
        if (crossed) {
            // The cruciform halo: three bars behind the head.
            val bar = stroke(lighten(color, 0.3f), r * 0.1f, 190)
            canvas.drawLine(x, y - r * 0.92f, x, y - r * 0.2f, bar)
            canvas.drawLine(x - r * 0.92f, y, x - r * 0.25f, y, bar)
            canvas.drawLine(x + r * 0.92f, y, x + r * 0.25f, y, bar)
        }
    }

    /** Soft shafts of light falling from above, as through a high window. */
    fun Scene.lightShafts(x: Float, y: Float, color: Int, count: Int = 5, spread: Float = 0.9f, alpha: Int = 55) {
        repeat(count) {
            val a = -PI.toFloat() / 2f + f(-spread, spread)
            val len = h * f(0.9f, 1.4f)
            val wide = f(0.02f, 0.07f)
            val p = Path()
            p.moveTo(x, y)
            p.lineTo(x + cos(a - wide) * len, y + sin(a - wide) * len)
            p.lineTo(x + cos(a + wide) * len, y + sin(a + wide) * len)
            p.close()
            canvas.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    x, y, len,
                    intArrayOf(withAlpha(color, alpha), withAlpha(color, alpha / 3), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
                )
                blendMode = BlendMode.PLUS
            })
        }
    }

    /** Specks of light drifting in the air. */
    fun Scene.motes(n: Int, color: Int) {
        repeat(count(n)) {
            val r = f(0.8f, 2.6f)
            glowDot(f(0f, w), f(0f, h), r, lighten(color, f(0f, 0.5f)), r * 4f, i(60, 200))
        }
    }

    /** A thorned branch following [path]; thorns alternate sides. */
    fun Scene.thornBranch(pts: FloatArray, color: Int, width: Float, thornLen: Float) {
        val path = smoothPath(pts, closed = false)
        canvas.drawPath(path, stroke(darken(color, 0.35f), width * 1.6f, 200))
        canvas.drawPath(path, stroke(color, width))
        var i = 2
        var side = 1f
        while (i < pts.size - 2) {
            val px = pts[i]
            val py = pts[i + 1]
            val dx = pts[i + 2] - pts[i - 2]
            val dy = pts[i + 3] - pts[i - 1]
            val len = max(1e-3f, kotlin.math.sqrt(dx * dx + dy * dy))
            val nx = -dy / len * side
            val ny = dx / len * side
            val tip = Path()
            tip.moveTo(px - dx / len * width, py - dy / len * width)
            tip.lineTo(px + nx * thornLen + dx / len * thornLen * 0.4f, py + ny * thornLen + dy / len * thornLen * 0.4f)
            tip.lineTo(px + dx / len * width, py + dy / len * width)
            tip.close()
            canvas.drawPath(tip, fill(color))
            canvas.drawPath(tip, stroke(darken(color, 0.4f), width * 0.3f, 180))
            side = -side
            i += 2
        }
    }

    /** Woven crown of thorns: several thorned loops around one centre. */
    fun Scene.crownOfThorns(cx: Float, cy: Float, radius: Float, color: Int, loops: Int = 3) {
        for (l in 0 until loops) {
            val n = 26
            val pts = FloatArray((n + 1) * 2)
            val wobble = radius * 0.07f
            val tilt = f(-0.18f, 0.18f)
            val squash = f(0.72f, 0.86f)
            for (k in 0..n) {
                val a = k * TAU / n
                val rr = radius * (1f + 0.1f * sin(a * 3f + l * 2f)) + f(-wobble, wobble)
                val px = cos(a) * rr
                val py = sin(a) * rr * squash
                pts[k * 2] = cx + px * cos(tilt) - py * sin(tilt)
                pts[k * 2 + 1] = cy + px * sin(tilt) + py * cos(tilt) + (l - loops / 2f) * radius * 0.05f
            }
            thornBranch(pts, color, radius * 0.028f, radius * 0.13f)
        }
    }

    /** Gold leaf: a gradient fill that reads as beaten metal. */
    fun Scene.goldPaint(top: Float, bottom: Float, base: Int = GOLD): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, top, 0f, bottom,
            intArrayOf(lighten(base, 0.55f), base, darken(base, 0.35f), lighten(base, 0.25f), darken(base, 0.45f)),
            floatArrayOf(0f, 0.3f, 0.52f, 0.74f, 1f), Shader.TileMode.CLAMP,
        )
    }

    /** Illuminated-manuscript border: a double rule with corner studs. */
    fun Scene.sacredFrame(inset: Float, color: Int) {
        val r = RectF(inset, inset, w - inset, h - inset)
        canvas.drawRect(r, stroke(color, 3f, 220))
        val r2 = RectF(inset + 12f, inset + 12f, w - inset - 12f, h - inset - 12f)
        canvas.drawRect(r2, stroke(color, 1.2f, 150))
        for ((x, y) in listOf(r.left to r.top, r.right to r.top, r.left to r.bottom, r.right to r.bottom)) {
            canvas.drawCircle(x, y, 9f, fill(Color.BLACK))
            canvas.drawCircle(x, y, 9f, stroke(color, 2.4f))
            canvas.drawCircle(x, y, 3.5f, fill(color))
        }
    }

    /**
     * A standing robed figure seen from the front, drawn as a lit silhouette.
     * Origin is between the feet; [height] is head to foot. [armsOut] spreads the arms wide.
     */
    fun Scene.robedFigure(x: Float, baseY: Float, height: Float, robe: Int, rim: Int, armsOut: Boolean, haloColor: Int) {
        val u = height / 7.2f                 // one head unit
        val headR = u * 0.5f
        val headY = baseY - height + headR
        val shoulderY = headY + headR * 2.2f
        val hipY = baseY - height * 0.4f

        halo(x, headY, headR * 2.5f, haloColor, crossed = true)

        canvas.drawRect(x - headR * 0.32f, headY + headR * 0.4f, x + headR * 0.32f, shoulderY + headR * 0.3f, fill(darken(PARCHMENT, 0.3f)))

        // Robe: shoulders falling to a softly flared hem.
        val robePath = smoothPath(
            floatArrayOf(
                x - u * 0.8f, shoulderY, x - u * 0.92f, hipY, x - u * 1.12f, baseY - u * 0.5f, x - u * 1.3f, baseY,
                x + u * 1.3f, baseY, x + u * 1.12f, baseY - u * 0.5f, x + u * 0.92f, hipY, x + u * 0.8f, shoulderY,
            ),
            closed = true, tension = 0.42f,
        )
        canvas.drawPath(robePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(x - u * 1.3f, 0f, x + u * 1.3f, 0f, lighten(robe, 0.22f), darken(robe, 0.6f), Shader.TileMode.CLAMP)
        })
        canvas.save()
        canvas.clipPath(robePath)
        for (k in -3..3) {
            val fx = x + k * u * 0.32f
            canvas.drawPath(
                smoothPath(floatArrayOf(fx * 0.55f + x * 0.45f, shoulderY + u * 0.5f, fx, hipY, fx * 1.2f - x * 0.2f, baseY), closed = false),
                stroke(withAlpha(rim, 60), u * 0.06f),
            )
        }
        softGlow(x - u * 0.5f, hipY, u * 1.2f, rim, 30)
        canvas.restore()
        canvas.drawPath(robePath, stroke(rim, u * 0.065f, 230))

        // Arms: raised in blessing, with wide sleeves that flare at the cuff.
        for (sgn in floatArrayOf(-1f, 1f)) {
            val elbowX = x + sgn * u * 1.35f
            val elbowY = shoulderY + u * 0.55f
            val handX = if (armsOut) x + sgn * u * 2.25f else x + sgn * u * 1.1f
            val handY = if (armsOut) shoulderY - u * 0.55f else hipY + u * 0.35f
            val sleeve = smoothPath(
                floatArrayOf(
                    x + sgn * u * 0.72f, shoulderY - u * 0.05f,
                    elbowX, elbowY - u * 0.3f,
                    handX + sgn * u * 0.1f, handY - u * 0.3f,
                    handX - sgn * u * 0.05f, handY + u * 0.3f,
                    elbowX - sgn * u * 0.12f, elbowY + u * 0.42f,
                    x + sgn * u * 0.6f, shoulderY + u * 0.55f,
                ),
                closed = true, tension = 0.4f,
            )
            canvas.drawPath(sleeve, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, shoulderY - u, 0f, handY + u, lighten(robe, 0.1f), darken(robe, 0.5f), Shader.TileMode.CLAMP)
            })
            canvas.drawPath(sleeve, stroke(rim, u * 0.06f, 220))
            // Hand: a small palm turned outward.
            canvas.save()
            canvas.rotate(sgn * -28f, handX, handY)
            canvas.drawRoundRect(RectF(handX - u * 0.12f, handY - u * 0.16f, handX + u * 0.12f, handY + u * 0.14f), u * 0.1f, u * 0.1f, fill(PARCHMENT))
            canvas.drawRoundRect(RectF(handX - u * 0.12f, handY - u * 0.16f, handX + u * 0.12f, handY + u * 0.14f), u * 0.1f, u * 0.1f, stroke(darken(PARCHMENT, 0.45f), u * 0.03f, 170))
            canvas.restore()
        }

        // Mantle draped over one shoulder.
        val mantle = smoothPath(
            floatArrayOf(
                x - u * 0.82f, shoulderY - u * 0.05f, x - u * 0.35f, shoulderY + u * 0.3f, x + u * 0.3f, shoulderY + u * 0.22f,
                x + u * 0.84f, shoulderY - u * 0.05f, x + u * 0.95f, hipY + u * 0.3f, x + u * 0.55f, hipY + u * 0.1f,
                x, shoulderY + u * 0.95f, x - u * 0.55f, hipY + u * 0.2f, x - u * 0.95f, hipY + u * 0.35f,
            ),
            closed = true, tension = 0.4f,
        )
        canvas.drawPath(mantle, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(x - u, shoulderY, x + u, hipY, mix(robe, rim, 0.45f), darken(mix(robe, rim, 0.3f), 0.45f), Shader.TileMode.CLAMP)
        })
        canvas.drawPath(mantle, stroke(rim, u * 0.06f, 235))

        // Neck, head, hair and beard: reverent shapes, not a portrait.
        canvas.drawCircle(x, headY, headR, fill(PARCHMENT))
        val hair = smoothPath(
            floatArrayOf(
                x - headR * 1.08f, headY + headR * 1.55f, x - headR * 1.15f, headY - headR * 0.2f, x - headR * 0.78f, headY - headR * 1.02f,
                x, headY - headR * 1.18f, x + headR * 0.78f, headY - headR * 1.02f, x + headR * 1.15f, headY - headR * 0.2f,
                x + headR * 1.08f, headY + headR * 1.55f, x + headR * 0.6f, headY + headR * 1.0f, x + headR * 0.55f, headY + headR * 0.05f,
                x, headY - headR * 0.58f, x - headR * 0.55f, headY + headR * 0.05f, x - headR * 0.6f, headY + headR * 1.0f,
            ),
            closed = true,
        )
        canvas.drawPath(hair, fill(Color.rgb(74, 46, 28)))
        canvas.drawPath(hair, stroke(rim, headR * 0.09f, 160))
        val beard = smoothPath(
            floatArrayOf(x - headR * 0.55f, headY + headR * 0.22f, x - headR * 0.46f, headY + headR * 1.0f, x, headY + headR * 1.42f, x + headR * 0.46f, headY + headR * 1.0f, x + headR * 0.55f, headY + headR * 0.22f, x, headY + headR * 0.52f),
            closed = true,
        )
        canvas.drawPath(beard, fill(Color.rgb(66, 40, 24)))
        canvas.drawPath(beard, stroke(rim, headR * 0.07f, 150))
        canvas.drawArc(RectF(x - headR * 0.42f, headY - headR * 0.22f, x - headR * 0.1f, headY + headR * 0.08f), 0f, 180f, false, stroke(Color.rgb(52, 34, 26), headR * 0.075f, 210))
        canvas.drawArc(RectF(x + headR * 0.1f, headY - headR * 0.22f, x + headR * 0.42f, headY + headR * 0.08f), 0f, 180f, false, stroke(Color.rgb(52, 34, 26), headR * 0.075f, 210))
    }
}
