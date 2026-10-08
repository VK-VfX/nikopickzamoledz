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
import kotlin.math.sin

/**
 * Procedural gothic portraits. Everything is drawn in "head units": the canvas is moved and scaled so
 * the head is about 1 unit tall, face centre at the origin, eye line near y = 0, chin at y = 0.5.
 * Lighting comes from the upper left; shadow falls to the right.
 */
class Look(
    val skin: Int,
    val skinShade: Int,
    val skinLight: Int,
    val hair: Int,
    val hairHi: Int,
    val lip: Int,
    val eye: Int,
    val accent: Int,
    val accent2: Int,
    val cloth: Int,
    val lace: Int,
    val tabby: Boolean = false,
)

object GothLook {
    private val skins = intArrayOf(
        Color.rgb(247, 234, 232), // porcelain
        Color.rgb(240, 222, 212), // ivory
        Color.rgb(222, 228, 238), // moonlit blue-white
        Color.rgb(224, 208, 226), // lavender pale
        Color.rgb(222, 184, 150), // warm sand
        Color.rgb(186, 134, 100), // caramel
        Color.rgb(132, 88, 66), // deep brown
        Color.rgb(98, 64, 50), // espresso
    )
    private val hairs = intArrayOf(
        Color.rgb(14, 12, 20), Color.rgb(14, 12, 20), Color.rgb(12, 16, 34), Color.rgb(18, 10, 16), Color.rgb(78, 10, 34),
        Color.rgb(206, 210, 230), Color.rgb(62, 28, 98), Color.rgb(16, 62, 74),
    )
    private val lips = intArrayOf(
        Color.rgb(42, 10, 34), Color.rgb(112, 12, 44), Color.rgb(78, 22, 112), Color.rgb(172, 18, 38), Color.rgb(24, 70, 78),
    )

    fun make(s: Scene): Look {
        val pal = s.palette.colors
        val skin = skins[if (s.chance(0.62f)) s.i(0, 4) else s.i(4, skins.size)]
        val hair = hairs[s.i(0, hairs.size)]
        val accent = pal[0]
        val accent2 = pal[1 % pal.size]
        return Look(
            skin = skin,
            skinShade = mix(skin, Color.rgb(70, 30, 70), 0.42f),
            skinLight = mix(skin, Color.WHITE, 0.45f),
            hair = hair,
            hairHi = mix(lighten(hair, 0.22f), accent, 0.55f),
            lip = lips[s.i(0, lips.size)],
            eye = mix(accent2, Color.WHITE, 0.25f),
            accent = accent,
            accent2 = accent2,
            cloth = mix(Color.rgb(14, 10, 20), accent, 0.1f),
            lace = mix(Color.WHITE, accent, 0.2f),
            tabby = s.chance(0.5f),
        )
    }
}

// ---------- tiny drawing helpers (all in head units) ----------

internal fun Scene.gFill(c: Int, a: Int = 255) = fill(c, a)
internal fun Scene.gLine(c: Int, w: Float, a: Int = 255) = stroke(c, w, a)

internal fun Scene.softOval(cx: Float, cy: Float, rx: Float, ry: Float, color: Int, alpha: Int) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f, intArrayOf(withAlpha(color, alpha), withAlpha(color, alpha / 2), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
    }
    canvas.save()
    canvas.translate(cx, cy)
    canvas.scale(rx, ry)
    canvas.drawCircle(0f, 0f, 1f, p)
    canvas.restore()
}

/** A closed shape drawn from a mirrored half outline. */
internal fun mirrorPath(half: FloatArray, cx: Float = 0f): Path = smoothPath(mirroredOutline(half, cx), closed = true)

/** Hair strand: a tapered curved stroke drawn as two overlapping strokes. */
internal fun Scene.strand(pts: FloatArray, color: Int, width: Float, alpha: Int) {
    val p = smoothPath(pts, closed = false)
    canvas.drawPath(p, stroke(color, width, alpha))
}

// ---------- the face ----------

object GothFace {
    private val FACE_HALF = floatArrayOf(
        0f, -0.5f, 0.2f, -0.47f, 0.3f, -0.34f, 0.325f, -0.12f, 0.32f, 0.05f, 0.285f, 0.2f, 0.22f, 0.34f, 0.12f, 0.45f, 0f, 0.5f,
    )

    fun neck(s: Scene, L: Look) {
        val n = smoothPath(floatArrayOf(-0.14f, 0.28f, -0.125f, 0.55f, -0.16f, 0.9f, 0.16f, 0.9f, 0.125f, 0.55f, 0.14f, 0.28f), closed = true)
        s.canvas.drawPath(n, s.gFill(L.skinShade))
        s.canvas.save()
        s.canvas.clipPath(n)
        s.softOval(-0.04f, 0.58f, 0.11f, 0.28f, L.skin, 255)
        // Shadow cast by the jaw.
        s.softOval(0.03f, 0.44f, 0.2f, 0.1f, Color.rgb(40, 10, 40), 150)
        s.canvas.restore()
    }

    /** Draws the face skin, then all features. [look] decides colours; [mood] 0 calm, 1 smirk, 2 sad. */
    fun face(s: Scene, L: Look, mood: Int = 0) {
        val c = s.canvas
        val outline = mirrorPath(FACE_HALF)
        c.drawPath(outline, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(-0.1f, -0.1f, 0.62f, intArrayOf(L.skinLight, L.skin, L.skinShade), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        })
        c.save()
        c.clipPath(outline)
        // Cheek hollows and temples read as sculpted cheekbones.
        s.softOval(0.27f, 0.12f, 0.1f, 0.2f, Color.rgb(60, 20, 60), 70)
        s.softOval(-0.27f, 0.12f, 0.07f, 0.18f, Color.rgb(60, 20, 60), 40)
        s.softOval(0.0f, 0.43f, 0.18f, 0.07f, Color.rgb(60, 20, 60), 60)
        // Blush.
        s.softOval(-0.2f, 0.12f, 0.1f, 0.06f, mix(L.accent, Color.rgb(220, 70, 110), 0.5f), 70)
        s.softOval(0.2f, 0.12f, 0.1f, 0.06f, mix(L.accent, Color.rgb(220, 70, 110), 0.5f), 55)
        // Forehead and nose bridge highlight.
        s.softOval(-0.03f, -0.28f, 0.12f, 0.1f, Color.WHITE, 60)
        s.softOval(-0.01f, 0.06f, 0.025f, 0.1f, Color.WHITE, 70)
        c.restore()

        eyes(s, L, mood)
        nose(s, L)
        lips(s, L, mood)
    }

    private fun eyes(s: Scene, L: Look, mood: Int) {
        val c = s.canvas
        for (sgn in floatArrayOf(-1f, 1f)) {
            val ex = sgn * 0.152f
            val ey = -0.018f
            val hw = 0.082f
            val tilt = if (mood == 2) 0.012f else 0f
            // Smoky shadow above the lid.
            s.softOval(ex + sgn * 0.01f, ey - 0.035f, 0.12f, 0.06f, mix(L.accent, Color.rgb(30, 10, 50), 0.6f), 190)
            val eye = Path()
            eye.moveTo(ex - sgn * hw, ey + 0.006f)
            eye.cubicTo(ex - sgn * hw * 0.35f, ey - 0.06f, ex + sgn * hw * 0.55f, ey - 0.062f - tilt, ex + sgn * hw * 1.08f, ey - 0.014f)
            eye.cubicTo(ex + sgn * hw * 0.5f, ey + 0.036f, ex - sgn * hw * 0.4f, ey + 0.04f, ex - sgn * hw, ey + 0.006f)
            eye.close()
            c.drawPath(eye, s.gFill(Color.rgb(238, 236, 246)))
            c.save()
            c.clipPath(eye)
            val ix = ex + sgn * 0.004f
            c.drawCircle(ix, ey - 0.006f, 0.05f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(ix, ey - 0.006f, 0.05f, intArrayOf(lighten(L.eye, 0.5f), L.eye, darken(L.eye, 0.7f)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            })
            c.drawCircle(ix, ey - 0.006f, 0.02f, s.gFill(Color.BLACK))
            c.drawCircle(ix - 0.016f, ey - 0.02f, 0.011f, s.gFill(Color.WHITE))
            c.drawCircle(ix + 0.014f, ey + 0.008f, 0.006f, s.gFill(Color.WHITE, 170))
            // Upper lid shadow over the iris.
            s.softOval(ex, ey - 0.052f, 0.1f, 0.03f, Color.BLACK, 210)
            c.restore()
            // Heavy upper line with a winged tip, plus lashes.
            val upper = smoothPath(
                floatArrayOf(ex - sgn * hw, ey + 0.006f, ex - sgn * hw * 0.35f, ey - 0.058f, ex + sgn * hw * 0.55f, ey - 0.062f - tilt, ex + sgn * hw * 1.08f, ey - 0.014f),
                closed = false,
            )
            c.drawPath(upper, s.gLine(Color.rgb(8, 4, 12), 0.016f))
            val wing = Path()
            wing.moveTo(ex + sgn * hw * 0.85f, ey - 0.03f)
            wing.lineTo(ex + sgn * hw * 1.55f, ey - 0.062f)
            wing.lineTo(ex + sgn * hw * 1.1f, ey - 0.008f)
            wing.close()
            c.drawPath(wing, s.gFill(Color.rgb(8, 4, 12)))
            for (k in 0 until 4) {
                val t = 0.45f + k * 0.14f
                val bx = ex + sgn * hw * (t * 1.15f - 0.1f)
                val by = ey - 0.062f + (1f - t) * 0.01f
                c.drawLine(bx, by, bx + sgn * 0.022f, by - 0.032f, s.gLine(Color.rgb(8, 4, 12), 0.006f))
            }
            c.drawPath(
                smoothPath(floatArrayOf(ex - sgn * hw * 0.8f, ey + 0.012f, ex, ey + 0.038f, ex + sgn * hw, ey + 0.006f), closed = false),
                s.gLine(darken(L.skinShade, 0.5f), 0.006f, 170),
            )
            // Brow.
            val bx0 = ex - sgn * 0.085f
            val bx1 = ex + sgn * 0.115f
            val brow = smoothPath(floatArrayOf(bx0, -0.098f, ex + sgn * 0.01f, -0.138f - (if (mood == 1) 0.015f * sgn * -1f else 0f), bx1, -0.108f), closed = false)
            c.drawPath(brow, s.gLine(darken(L.hair, 0.1f), 0.014f, 235))
        }
    }

    private fun nose(s: Scene, L: Look) {
        val c = s.canvas
        val shade = darken(L.skinShade, 0.25f)
        c.drawPath(smoothPath(floatArrayOf(0.028f, -0.02f, 0.036f, 0.07f, 0.044f, 0.15f), closed = false), s.gLine(shade, 0.008f, 120))
        c.drawPath(smoothPath(floatArrayOf(-0.05f, 0.17f, -0.03f, 0.2f, 0f, 0.205f, 0.03f, 0.2f, 0.05f, 0.17f), closed = false), s.gLine(shade, 0.008f, 190))
        c.drawCircle(-0.03f, 0.19f, 0.007f, s.gFill(darken(L.skinShade, 0.5f), 200))
        c.drawCircle(0.03f, 0.19f, 0.007f, s.gFill(darken(L.skinShade, 0.5f), 200))
        s.softOval(-0.012f, 0.165f, 0.022f, 0.016f, Color.WHITE, 90)
    }

    private fun lips(s: Scene, L: Look, mood: Int) {
        val c = s.canvas
        val y = 0.315f
        val w = 0.088f
        val lift = if (mood == 1) -0.012f else if (mood == 2) 0.01f else 0f
        val up = Path()
        up.moveTo(-w, y + lift)
        up.quadTo(-w * 0.5f, y - 0.04f, -0.014f, y - 0.022f)
        up.quadTo(0f, y - 0.014f, 0.014f, y - 0.022f)
        up.quadTo(w * 0.5f, y - 0.04f, w, y + lift * 0.4f)
        up.quadTo(0f, y + 0.012f, -w, y + lift)
        up.close()
        val low = Path()
        low.moveTo(-w, y + lift)
        low.quadTo(0f, y + 0.012f, w, y + lift * 0.4f)
        low.quadTo(w * 0.5f, y + 0.07f, 0f, y + 0.074f)
        low.quadTo(-w * 0.5f, y + 0.07f, -w, y + lift)
        low.close()
        c.drawPath(low, s.gFill(lighten(L.lip, 0.08f)))
        c.drawPath(up, s.gFill(darken(L.lip, 0.12f)))
        c.drawPath(smoothPath(floatArrayOf(-w, y + lift, 0f, y + 0.012f, w, y + lift * 0.4f), closed = false), s.gLine(Color.rgb(10, 2, 8), 0.007f, 230))
        // Gloss highlight on the lower lip.
        c.drawPath(smoothPath(floatArrayOf(-0.035f, y + 0.04f, 0f, y + 0.05f, 0.02f, y + 0.045f), closed = false), s.gLine(Color.WHITE, 0.008f, 120))
    }
}

// ---------- hair ----------

object GothHair {
    /** Large mass behind the head and shoulders. [spread] widens it, [length] in head units below the chin. */
    fun back(s: Scene, L: Look, spread: Float, length: Float, wavy: Float) {
        val half = floatArrayOf(
            0f, -0.7f, 0.28f, -0.66f, 0.44f, -0.47f, 0.5f, -0.18f, 0.52f * spread, 0.2f, 0.58f * spread, 0.6f,
            0.62f * spread, 0.9f + length * 0.4f, 0.55f * spread + wavy * 0.05f, 0.5f + length, 0.4f * spread, 0.45f + length + wavy * 0.1f,
            0.22f, 0.4f + length, 0f, 0.35f + length,
        )
        val p = mirrorPath(half)
        s.canvas.drawPath(p, s.gFill(L.hair))
        s.canvas.save()
        s.canvas.clipPath(p)
        // Light falls off to the right; a few soft highlights give volume.
        s.softOval(-0.3f, -0.2f, 0.18f, 0.5f, L.hairHi, 90)
        s.softOval(0.38f, 0.6f, 0.12f, 0.6f, L.hairHi, 50)
        for (k in 0 until 9) {
            val x0 = -0.5f + k * 0.12f
            s.strand(floatArrayOf(x0, -0.3f, x0 * 1.1f - 0.02f, 0.3f, x0 * 1.25f, 0.8f + length * 0.5f), L.hairHi, 0.006f, 70)
        }
        s.canvas.restore()
        s.canvas.drawPath(p, s.gLine(withAlpha(L.accent, 150), 0.006f))
    }

    /** Hair framing the face: a centre or side part with long curtains. */
    fun curtains(s: Scene, L: Look, part: Float, bangs: Boolean) {
        val c = s.canvas
        for (sgn in floatArrayOf(-1f, 1f)) {
            val widthK = if (sgn * part > 0) 0.88f else 1.1f
            val outer = floatArrayOf(
                part, -0.52f, sgn * 0.2f + part * 0.4f, -0.5f, sgn * 0.31f, -0.36f, sgn * 0.37f, -0.1f, sgn * 0.37f, 0.2f,
                sgn * 0.42f, 0.55f, sgn * 0.5f * widthK, 0.95f,
            )
            val inner = floatArrayOf(
                sgn * 0.41f * widthK, 1.0f, sgn * 0.33f, 0.55f, sgn * 0.3f, 0.22f, sgn * 0.3f, -0.1f, sgn * 0.24f, -0.32f,
                sgn * 0.12f + part * 0.5f, -0.4f,
            )
            val pts = outer + inner
            val p = smoothPath(pts, closed = true)
            c.drawPath(p, s.gFill(L.hair))
            c.save()
            c.clipPath(p)
            s.softOval(sgn * 0.33f - 0.04f, 0.0f, 0.06f, 0.55f, L.hairHi, 110)
            for (k in 0 until 4) s.strand(floatArrayOf(sgn * (0.2f + k * 0.04f), -0.45f, sgn * (0.33f + k * 0.015f), -0.1f, sgn * (0.36f + k * 0.03f), 0.5f), L.hairHi, 0.005f, 100)
            c.restore()
            c.drawPath(p, s.gLine(withAlpha(L.accent, 130), 0.005f))
        }
        if (bangs) {
            // Curtain bangs sweeping from the part to the temples, with wispy tips.
            for (sgn in floatArrayOf(-1f, 1f)) {
                val sweep = smoothPath(
                    floatArrayOf(part, -0.58f, sgn * 0.22f + part, -0.56f, sgn * 0.34f, -0.4f, sgn * 0.35f, -0.22f, sgn * 0.27f, -0.2f, sgn * 0.16f + part * 0.6f, -0.3f, part * 0.8f, -0.42f),
                    closed = true,
                )
                c.drawPath(sweep, s.gFill(L.hair))
                c.save(); c.clipPath(sweep)
                s.softOval(sgn * 0.2f, -0.45f, 0.14f, 0.06f, L.hairHi, 120)
                for (k in 0 until 5) c.drawLine(part + sgn * k * 0.016f, -0.56f, sgn * (0.17f + k * 0.035f), -0.2f - k * 0.012f, s.gLine(L.hairHi, 0.004f, 80))
                c.restore()
                c.drawPath(sweep, s.gLine(withAlpha(L.accent, 120), 0.004f))
            }
        } else {
            // Top of the head where the part sits.
            val top = smoothPath(floatArrayOf(-0.33f, -0.34f, -0.24f, -0.52f, 0f, -0.6f, 0.24f, -0.52f, 0.33f, -0.34f, 0.2f, -0.46f, part, -0.5f, -0.2f, -0.46f), closed = true)
            c.drawPath(top, s.gFill(L.hair))
            s.canvas.drawPath(smoothPath(floatArrayOf(part, -0.6f, part * 0.8f, -0.54f, part, -0.48f), closed = false), s.gLine(L.skinShade, 0.005f, 180))
        }
    }
}

// ---------- clothing and jewellery (front bust) ----------

object GothDress {
    /** Victorian bodice: sloping shoulders, puffed sleeve caps, corset front and a lace bertha collar. */
    fun bust(s: Scene, L: Look, bottom: Float) {
        val c = s.canvas
        val torso = mirrorPath(floatArrayOf(0.14f, 0.88f, 0.42f, 0.95f, 0.68f, 1.08f, 0.8f, 1.32f, 0.84f, 1.8f, 0.9f, bottom))
        val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(-0.8f, 0.9f, 0.8f, 1.8f, lighten(L.cloth, 0.09f), darken(L.cloth, 0.4f), Shader.TileMode.CLAMP)
        }
        c.drawPath(torso, shade)
        c.drawPath(torso, s.gLine(withAlpha(L.accent, 170), 0.007f))

        // Puffed sleeve caps at the shoulders, catching rim light.
        for (sgn in floatArrayOf(-1f, 1f)) {
            val puff = smoothPath(floatArrayOf(sgn * 0.5f, 0.98f, sgn * 0.78f, 0.98f, sgn * 0.98f, 1.2f, sgn * 1.0f, 1.55f, sgn * 0.8f, 1.7f, sgn * 0.58f, 1.5f, sgn * 0.52f, 1.2f), closed = true)
            c.drawPath(puff, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(sgn * 0.7f, 1.1f, 0.6f, lighten(L.cloth, 0.14f), darken(L.cloth, 0.3f), Shader.TileMode.CLAMP)
            })
            c.save(); c.clipPath(puff)
            for (k in 0 until 4) c.drawArc(RectF(sgn * 0.6f - 0.2f, 1.0f + k * 0.12f, sgn * 0.6f + 0.2f, 1.3f + k * 0.12f), 200f, 140f, false, s.gLine(withAlpha(L.accent, 70), 0.005f))
            c.restore()
            c.drawPath(puff, s.gLine(withAlpha(L.accent, 200), 0.007f))
            // Lace cuff at the bottom of the puff.
            for (k in 0..6) {
                val a = 0.62f + k * 0.07f
                c.drawArc(RectF(sgn * (0.5f + k * 0.075f) - 0.03f, 1.62f - k * 0.02f, sgn * (0.5f + k * 0.075f) + 0.03f, 1.7f - k * 0.02f), 0f, 180f, false, s.gLine(withAlpha(L.lace, 210), 0.006f))
                if (a < 0f) break
            }
        }

        // Corset panel down the front.
        val panel = mirrorPath(floatArrayOf(0.09f, 1.28f, 0.3f, 1.35f, 0.36f, 1.9f, 0.3f, 2.5f, 0.34f, bottom))
        c.drawPath(panel, s.gFill(darken(L.cloth, 0.2f)))
        c.save(); c.clipPath(panel)
        var y = 1.3f
        while (y < bottom) {
            c.drawLine(-0.07f, y, 0.07f, y + 0.07f, s.gLine(withAlpha(L.lace, 200), 0.006f))
            c.drawLine(0.07f, y, -0.07f, y + 0.07f, s.gLine(withAlpha(L.lace, 200), 0.006f))
            y += 0.12f
        }
        for (sgn in floatArrayOf(-1f, 1f)) for (k in 0 until 4) {
            val x = sgn * (0.13f + k * 0.045f)
            c.drawLine(x, 1.32f, x + sgn * 0.02f, bottom, s.gLine(withAlpha(L.accent, 60), 0.004f))
        }
        c.restore()
        c.drawPath(panel, s.gLine(withAlpha(L.accent, 150), 0.006f))

        // Lace bertha collar over the shoulders, scalloped and pinned with a brooch.
        val bertha = mirrorPath(floatArrayOf(0.16f, 0.9f, 0.45f, 0.98f, 0.7f, 1.14f, 0.74f, 1.3f, 0.5f, 1.34f, 0.25f, 1.5f, 0.0f, 1.62f))
        c.drawPath(bertha, s.gFill(darken(L.cloth, 0.1f), 245))
        c.save(); c.clipPath(bertha)
        for (ring in 0 until 5) {
            val k = 0.3f + ring * 0.17f
            c.drawPath(mirrorPath(floatArrayOf(0.16f * k + 0.04f, 0.9f, 0.45f * k + 0.04f, 0.98f, 0.7f * k, 1.14f, 0.74f * k, 1.3f, 0.5f * k, 1.34f, 0.25f * k, 1.5f, 0.0f, 1.62f)), s.gLine(withAlpha(L.lace, 70), 0.004f))
        }
        var t = 0f
        while (t < 1f) {
            for (sgn in floatArrayOf(-1f, 1f)) {
                val x = sgn * (0.2f + t * 0.5f)
                val yy = 0.95f + t * 0.3f
                c.drawCircle(x, yy + 0.07f, 0.012f, s.gFill(withAlpha(L.lace, 160)))
            }
            t += 0.1f
        }
        c.restore()
        c.drawPath(bertha, s.gLine(withAlpha(L.lace, 230), 0.008f))
        // Scalloped lace edge following the bertha outline.
        for (sgn in floatArrayOf(-1f, 1f)) {
            val edge = floatArrayOf(sgn * 0.7f, 1.14f, sgn * 0.74f, 1.3f, sgn * 0.5f, 1.34f, sgn * 0.25f, 1.5f, 0f, 1.62f)
            for (i in 0 until edge.size / 2 - 1) {
                for (j in 0..3) {
                    val u = j / 4f
                    val x = edge[i * 2] + (edge[i * 2 + 2] - edge[i * 2]) * u
                    val yv = edge[i * 2 + 1] + (edge[i * 2 + 3] - edge[i * 2 + 1]) * u
                    c.drawCircle(x, yv + 0.012f, 0.014f, s.gLine(withAlpha(L.lace, 200), 0.005f))
                }
            }
        }
        // Brooch.
        val gem = Path()
        gem.moveTo(0f, 1.42f); gem.lineTo(-0.04f, 1.48f); gem.lineTo(0f, 1.57f); gem.lineTo(0.04f, 1.48f); gem.close()
        s.softOval(0f, 1.49f, 0.1f, 0.1f, L.accent, 140)
        c.drawPath(gem, s.gFill(L.accent))
        c.drawPath(gem, s.gLine(Color.WHITE, 0.005f, 220))
    }

    /** Stand-up lace collar ringing the neck. */
    fun collar(s: Scene, L: Look) {
        val c = s.canvas
        val ring = mirrorPath(floatArrayOf(0f, 0.7f, 0.11f, 0.7f, 0.16f, 0.76f, 0.19f, 0.9f, 0.17f, 0.97f, 0f, 1.0f))
        c.drawPath(ring, s.gFill(L.cloth))
        c.drawPath(ring, s.gLine(withAlpha(L.lace, 230), 0.007f))
        for (k in -4..4) {
            val x = k * 0.04f
            c.drawArc(RectF(x - 0.02f, 0.96f, x + 0.02f, 1.01f), 0f, 180f, false, s.gLine(withAlpha(L.lace, 220), 0.005f))
        }
        c.drawLine(-0.13f, 0.8f, 0.13f, 0.8f, s.gLine(withAlpha(L.accent, 100), 0.004f))
    }

    fun choker(s: Scene, L: Look, pendant: Boolean) {
        val c = s.canvas
        val band = smoothPath(floatArrayOf(-0.118f, 0.6f, 0f, 0.65f, 0.118f, 0.6f), closed = false)
        c.drawPath(band, s.gLine(Color.rgb(12, 6, 16), 0.04f))
        c.drawPath(band, s.gLine(withAlpha(L.accent, 210), 0.006f))
        if (pendant) {
            val gem = Path()
            gem.moveTo(0f, 0.65f); gem.lineTo(-0.028f, 0.69f); gem.lineTo(0f, 0.77f); gem.lineTo(0.028f, 0.69f); gem.close()
            c.drawPath(gem, s.gFill(L.accent))
            c.drawPath(gem, s.gLine(Color.WHITE, 0.004f, 200))
            s.softOval(0f, 0.69f, 0.07f, 0.07f, L.accent, 120)
        }
    }

    /** Dangling earring on each side of the face. */
    fun earrings(s: Scene, L: Look) {
        for (sgn in floatArrayOf(-1f, 1f)) {
            val x = sgn * 0.325f
            s.canvas.drawLine(x, 0.06f, x, 0.2f, s.gLine(Color.rgb(200, 190, 210), 0.004f))
            val drop = Path()
            drop.moveTo(x, 0.2f); drop.quadTo(x + 0.03f, 0.27f, x, 0.32f); drop.quadTo(x - 0.03f, 0.27f, x, 0.2f)
            s.canvas.drawPath(drop, s.gFill(L.accent))
            s.canvas.drawPath(drop, s.gLine(Color.WHITE, 0.004f, 180))
        }
    }
}

// ---------- shared props ----------

object GothProps {
    /** A rose seen from above: layered petal arcs around a spiral heart. */
    fun rose(s: Scene, x: Float, y: Float, r: Float, color: Int, rot: Float = 0f) {
        s.canvas.save()
        s.canvas.translate(x, y)
        s.canvas.rotate(rot)
        s.canvas.scale(r, r)
        val pal = Paint(Paint.ANTI_ALIAS_FLAG)
        s.softOval(0f, 0f, 1.5f, 1.5f, color, 60)
        for (layer in 0 until 3) {
            val rr = 1f - layer * 0.28f
            val n = 5
            for (k in 0 until n) {
                val a = k * TAU / n + layer * 0.6f
                val px = cos(a) * rr * 0.35f
                val py = sin(a) * rr * 0.35f
                s.canvas.drawCircle(px, py, rr * 0.62f, pal.apply { this.color = mix(darken(color, 0.5f), color, layer / 2f + 0.15f); style = Paint.Style.FILL; alpha = 255; shader = null })
                s.canvas.drawCircle(px, py, rr * 0.62f, s.stroke(lighten(color, 0.4f), 0.04f, 160))
            }
        }
        val sp = Path()
        for (k in 0..40) {
            val t = k / 40f
            val a = t * TAU * 1.6f
            val rr2 = 0.05f + t * 0.3f
            if (k == 0) sp.moveTo(cos(a) * rr2, sin(a) * rr2) else sp.lineTo(cos(a) * rr2, sin(a) * rr2)
        }
        s.canvas.drawPath(sp, s.stroke(lighten(color, 0.5f), 0.04f, 200))
        s.canvas.restore()
    }

    fun bat(s: Scene, x: Float, y: Float, size: Float, color: Int, rot: Float = 0f) {
        s.canvas.save()
        s.canvas.translate(x, y)
        s.canvas.rotate(rot)
        s.canvas.scale(size, size)
        val half = floatArrayOf(0f, -0.18f, 0.06f, -0.3f, 0.14f, -0.18f, 0.5f, -0.4f, 1f, -0.12f, 0.85f, 0.02f, 0.7f, -0.04f, 0.55f, 0.1f, 0.42f, 0.0f, 0.26f, 0.18f, 0.1f, 0.12f, 0f, 0.24f)
        val p = mirrorPath(half)
        s.canvas.drawPath(p, s.fill(color))
        s.canvas.restore()
    }

    /** Five-pointed pentacle-less sparkle star used for stars around the moon. */
    fun sparkle(s: Scene, x: Float, y: Float, r: Float, color: Int) {
        val p = Path()
        p.moveTo(x, y - r); p.quadTo(x, y, x + r, y); p.quadTo(x, y, x, y + r); p.quadTo(x, y, x - r, y); p.quadTo(x, y, x, y - r); p.close()
        s.canvas.drawPath(p, s.fill(color))
    }

    /** Gothic pointed arch outline centred at cx whose apex sits at the top. */
    fun arch(cx: Float, top: Float, w: Float, bottom: Float): Path {
        val p = Path()
        p.moveTo(cx - w, bottom)
        p.lineTo(cx - w, top + w * 1.6f)
        p.cubicTo(cx - w, top + w * 0.6f, cx - w * 0.4f, top + w * 0.15f, cx, top)
        p.cubicTo(cx + w * 0.4f, top + w * 0.15f, cx + w, top + w * 0.6f, cx + w, top + w * 1.6f)
        p.lineTo(cx + w, bottom)
        return p
    }
}

internal fun degToRad(d: Float): Float = (d * PI / 180.0).toFloat()
internal fun absF(v: Float) = abs(v)
internal fun sinF(v: Float) = sin(v)

object GothHead {
    /** Translucent lace veil falling from the crown over the shoulders. */
    fun veil(s: Scene, L: Look, alpha: Int = 70) {
        val c = s.canvas
        val v = smoothPath(floatArrayOf(0f, -0.72f, 0.3f, -0.66f, 0.5f, -0.3f, 0.62f, 0.4f, 0.8f, 1.3f, 0.35f, 1.4f, 0f, 1.32f, -0.35f, 1.4f, -0.8f, 1.3f, -0.62f, 0.4f, -0.5f, -0.3f, -0.3f, -0.66f), closed = true)
        c.drawPath(v, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, -0.7f, 0f, 1.4f, withAlpha(Color.rgb(10, 8, 18), alpha), withAlpha(Color.rgb(10, 8, 18), alpha / 2), Shader.TileMode.CLAMP)
        })
        c.save(); c.clipPath(v)
        // Fine floral lace: tiny dots in a diamond lattice, fading toward the face.
        var y = -0.6f
        var row = 0
        while (y < 1.4f) {
            var x = -0.9f + if (row % 2 == 0) 0f else 0.03f
            while (x < 0.9f) {
                c.drawCircle(x, y, 0.004f, s.gFill(withAlpha(L.lace, 70)))
                x += 0.06f
            }
            y += 0.04f; row++
        }
        c.restore()
        c.drawPath(v, s.gLine(withAlpha(L.lace, 110), 0.004f))
        for (k in 0..14) {
            val x2 = -0.78f + k * 0.112f
            c.drawArc(RectF(x2 - 0.04f, 1.3f, x2 + 0.04f, 1.42f), 0f, 180f, false, s.gLine(withAlpha(L.lace, 140), 0.005f))
        }
    }

    /** A wreath of roses and thorny vines resting on the head. */
    fun roseCrown(s: Scene, L: Look) {
        val c = s.canvas
        val vine = smoothPath(floatArrayOf(-0.4f, -0.2f, -0.34f, -0.45f, -0.15f, -0.62f, 0.15f, -0.62f, 0.34f, -0.45f, 0.4f, -0.2f), closed = false)
        c.drawPath(vine, s.gLine(Color.rgb(20, 40, 28), 0.03f))
        c.drawPath(vine, s.gLine(Color.rgb(70, 130, 90), 0.008f, 200))
        for (k in 0 until 9) {
            val t = k / 8f
            val a = PI.toFloat() * (1.1f + t * 0.8f)
            val x = cos(a) * 0.42f
            val y = -0.28f + sin(a) * 0.42f
            c.drawLine(x, y, x + cos(a) * 0.05f, y + sin(a) * 0.05f - 0.02f, s.gLine(Color.rgb(90, 150, 100), 0.006f, 220))
        }
        val spots = floatArrayOf(-0.36f, -0.34f, -0.2f, -0.56f, 0f, -0.64f, 0.2f, -0.56f, 0.36f, -0.34f)
        for (i in 0 until spots.size / 2) {
            GothProps.rose(s, spots[i * 2], spots[i * 2 + 1], if (i == 2) 0.11f else 0.08f, mix(L.accent, Color.rgb(160, 20, 50), 0.4f), i * 40f)
        }
    }

    /** Small tilted top hat with a ribbon, feathers and a rose. */
    fun miniHat(s: Scene, L: Look) {
        val c = s.canvas
        c.save()
        c.translate(0.22f, -0.58f)
        c.rotate(-18f)
        val crown = smoothPath(floatArrayOf(-0.13f, 0.02f, -0.12f, -0.2f, 0.12f, -0.2f, 0.13f, 0.02f), closed = true, tension = 0.2f)
        c.drawPath(crown, s.gFill(Color.rgb(12, 8, 16)))
        c.drawPath(crown, s.gLine(withAlpha(L.accent, 200), 0.006f))
        c.drawOval(RectF(-0.2f, -0.02f, 0.2f, 0.07f), s.gFill(Color.rgb(12, 8, 16)))
        c.drawOval(RectF(-0.2f, -0.02f, 0.2f, 0.07f), s.gLine(withAlpha(L.accent, 220), 0.006f))
        c.drawRect(-0.125f, -0.04f, 0.125f, 0.0f, s.gFill(L.accent, 200))
        for (k in 0 until 3) {
            val f = smoothPath(floatArrayOf(0.1f, -0.15f, 0.2f + k * 0.04f, -0.34f - k * 0.02f, 0.3f + k * 0.05f, -0.4f - k * 0.04f), closed = false)
            c.drawPath(f, s.gLine(Color.rgb(20, 14, 26), 0.02f))
            c.drawPath(f, s.gLine(withAlpha(L.accent, 150), 0.004f))
        }
        GothProps.rose(s, -0.08f, -0.03f, 0.06f, L.accent, 20f)
        c.restore()
    }

    /** Crescent-and-spike silver tiara. */
    fun tiara(s: Scene, L: Look) {
        val c = s.canvas
        val band = smoothPath(floatArrayOf(-0.3f, -0.36f, -0.2f, -0.5f, 0f, -0.55f, 0.2f, -0.5f, 0.3f, -0.36f), closed = false)
        c.drawPath(band, s.gLine(Color.rgb(205, 205, 222), 0.012f))
        for (k in -2..2) {
            val x = k * 0.1f
            val y = -0.52f + abs(k) * 0.04f
            val sp = Path()
            sp.moveTo(x - 0.025f, y); sp.lineTo(x, y - (0.14f - abs(k) * 0.03f)); sp.lineTo(x + 0.025f, y); sp.close()
            c.drawPath(sp, s.gFill(Color.rgb(205, 205, 222)))
        }
        s.softOval(0f, -0.62f, 0.1f, 0.1f, L.accent, 120)
        c.drawCircle(0f, -0.58f, 0.022f, s.gFill(L.accent))
    }
}

// ---------- profile (facing right) ----------

object GothProfile {
    private val FACE = floatArrayOf(
        0.02f, -0.5f, 0.2f, -0.44f, 0.29f, -0.3f, 0.305f, -0.17f, 0.285f, -0.08f, 0.32f, 0.03f, 0.4f, 0.13f, 0.4f, 0.13f, 0.335f, 0.17f,
        0.35f, 0.22f, 0.37f, 0.255f, 0.34f, 0.29f, 0.36f, 0.32f, 0.315f, 0.355f, 0.33f, 0.43f, 0.265f, 0.5f, 0.12f, 0.47f, 0.0f, 0.4f,
        -0.1f, 0.2f, -0.2f, -0.1f, -0.15f, -0.4f,
    )

    fun neck(s: Scene, L: Look) {
        val n = smoothPath(floatArrayOf(0.2f, 0.42f, 0.17f, 0.6f, 0.2f, 0.95f, -0.2f, 0.95f, -0.16f, 0.5f, -0.1f, 0.2f), closed = true)
        s.canvas.drawPath(n, s.gFill(L.skinShade))
        s.canvas.save(); s.canvas.clipPath(n)
        s.softOval(0.08f, 0.6f, 0.12f, 0.3f, L.skin, 255)
        s.softOval(0.12f, 0.5f, 0.2f, 0.07f, Color.rgb(40, 10, 40), 90)
        s.canvas.restore()
    }

    fun face(s: Scene, L: Look) {
        val c = s.canvas
        val outline = smoothPath(FACE, closed = true)
        c.drawPath(outline, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(0.3f, -0.05f, 0.6f, intArrayOf(L.skinLight, L.skin, L.skinShade), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        })
        c.save(); c.clipPath(outline)
        s.softOval(0.16f, 0.13f, 0.1f, 0.07f, mix(L.accent, Color.rgb(220, 70, 110), 0.5f), 80)
        s.softOval(-0.08f, 0.1f, 0.12f, 0.25f, Color.rgb(60, 20, 60), 60)
        s.softOval(0.27f, -0.3f, 0.06f, 0.1f, Color.WHITE, 70)
        s.softOval(0.26f, 0.43f, 0.1f, 0.05f, Color.rgb(60, 20, 60), 25)
        c.restore()
        c.drawPath(outline, s.gLine(darken(L.skinShade, 0.3f), 0.004f, 130))

        // Eye: almond pointing forward, with a heavy lash and wing.
        val ex = 0.2f
        val ey = -0.035f
        s.softOval(ex + 0.02f, ey - 0.04f, 0.1f, 0.05f, mix(L.accent, Color.rgb(30, 10, 50), 0.6f), 190)
        val eye = Path()
        eye.moveTo(ex - 0.07f, ey + 0.012f)
        eye.cubicTo(ex - 0.03f, ey - 0.05f, ex + 0.05f, ey - 0.052f, ex + 0.095f, ey - 0.005f)
        eye.cubicTo(ex + 0.05f, ey + 0.03f, ex - 0.03f, ey + 0.035f, ex - 0.07f, ey + 0.012f)
        eye.close()
        c.drawPath(eye, s.gFill(Color.rgb(236, 234, 244)))
        c.save(); c.clipPath(eye)
        c.drawCircle(ex + 0.055f, ey - 0.004f, 0.042f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(ex + 0.055f, ey - 0.004f, 0.042f, intArrayOf(lighten(L.eye, 0.5f), L.eye, darken(L.eye, 0.7f)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        })
        c.drawCircle(ex + 0.066f, ey - 0.004f, 0.017f, s.gFill(Color.BLACK))
        c.drawCircle(ex + 0.05f, ey - 0.016f, 0.01f, s.gFill(Color.WHITE))
        s.softOval(ex, ey - 0.05f, 0.1f, 0.03f, Color.BLACK, 210)
        c.restore()
        val upper = smoothPath(floatArrayOf(ex - 0.07f, ey + 0.012f, ex - 0.03f, ey - 0.05f, ex + 0.05f, ey - 0.052f, ex + 0.095f, ey - 0.005f), closed = false)
        c.drawPath(upper, s.gLine(Color.rgb(8, 4, 12), 0.016f))
        for (k in 0 until 4) {
            val bx = ex + 0.0f + k * 0.03f
            c.drawLine(bx, ey - 0.052f, bx + 0.03f, ey - 0.088f, s.gLine(Color.rgb(8, 4, 12), 0.006f))
        }
        val wing = Path()
        wing.moveTo(ex - 0.05f, ey - 0.03f); wing.lineTo(ex - 0.12f, ey - 0.07f); wing.lineTo(ex - 0.06f, ey + 0.0f); wing.close()
        c.drawPath(wing, s.gFill(Color.rgb(8, 4, 12)))
        c.drawPath(smoothPath(floatArrayOf(ex - 0.03f, ey - 0.12f, ex + 0.07f, ey - 0.15f, ex + 0.12f, ey - 0.12f), closed = false), s.gLine(darken(L.hair, 0.1f), 0.013f, 235))

        // Nose shadow and nostril.
        c.drawPath(smoothPath(floatArrayOf(0.36f, 0.15f, 0.34f, 0.165f, 0.31f, 0.15f), closed = false), s.gLine(darken(L.skinShade, 0.5f), 0.007f, 200))
        s.softOval(0.37f, 0.1f, 0.012f, 0.02f, Color.WHITE, 120)

        // Lips.
        val up = Path()
        up.moveTo(0.335f, 0.255f); up.quadTo(0.385f, 0.232f, 0.378f, 0.282f); up.lineTo(0.33f, 0.298f); up.close()
        val low = Path()
        low.moveTo(0.33f, 0.298f); low.quadTo(0.382f, 0.288f, 0.37f, 0.335f); low.quadTo(0.34f, 0.358f, 0.31f, 0.345f); low.close()
        c.drawPath(low, s.gFill(lighten(L.lip, 0.08f)))
        c.drawPath(up, s.gFill(darken(L.lip, 0.12f)))
        c.drawLine(0.3f, 0.3f, 0.337f, 0.296f, s.gLine(Color.rgb(10, 2, 8), 0.006f, 230))
        c.drawCircle(0.352f, 0.318f, 0.006f, s.gFill(Color.WHITE, 140))

        // Ear peeking from the hair, with a drop earring.
        val ear = smoothPath(floatArrayOf(0.02f, 0.0f, 0.05f, -0.06f, 0.1f, -0.02f, 0.09f, 0.1f, 0.04f, 0.16f, 0.0f, 0.1f), closed = true)
        c.drawPath(ear, s.gFill(L.skinShade))
        c.drawPath(smoothPath(floatArrayOf(0.04f, 0.0f, 0.075f, 0.0f, 0.065f, 0.09f), closed = false), s.gLine(darken(L.skinShade, 0.4f), 0.005f, 180))
        c.drawLine(0.04f, 0.15f, 0.04f, 0.3f, s.gLine(Color.rgb(200, 190, 210), 0.004f))
        val drop = Path(); drop.moveTo(0.04f, 0.3f); drop.quadTo(0.07f, 0.37f, 0.04f, 0.42f); drop.quadTo(0.01f, 0.37f, 0.04f, 0.3f)
        c.drawPath(drop, s.gFill(L.accent)); c.drawPath(drop, s.gLine(Color.WHITE, 0.004f, 180))
    }

    /** Long hair flowing backwards (to the left), drawn behind the neck and shoulders. */
    fun hairBack(s: Scene, L: Look, length: Float) {
        val c = s.canvas
        val mass = smoothPath(
            floatArrayOf(
                0.0f, -0.62f, -0.3f, -0.55f, -0.5f, -0.3f, -0.58f, 0.1f, -0.64f, 0.6f, -0.74f, 1.0f + length * 0.3f,
                -0.66f, 1.4f + length * 0.6f, -0.5f, 1.3f + length, -0.36f, 1.4f + length, -0.26f, 1.0f + length * 0.4f, -0.2f, 0.6f,
                -0.18f, 0.2f, -0.05f, 0.0f,
            ),
            closed = true,
        )
        c.drawPath(mass, s.gFill(L.hair))
        c.save(); c.clipPath(mass)
        s.softOval(-0.45f, 0.5f, 0.1f, 0.6f, L.hairHi, 70)
        for (k in 0 until 8) {
            val t = k / 7f
            s.strand(floatArrayOf(-0.1f - t * 0.4f, 0.0f, -0.25f - t * 0.35f, 0.4f, -0.3f - t * 0.3f, 0.9f, -0.35f - t * 0.2f + 0.05f, 1.2f + length * 0.6f), L.hairHi, 0.005f, 80)
        }
        c.restore()
        c.drawPath(mass, s.gLine(withAlpha(L.accent, 150), 0.005f))
    }

    /** Hair over the skull: hairline at the forehead, tucked behind the ear. */
    fun hairCap(s: Scene, L: Look) {
        val c = s.canvas
        val cap = smoothPath(
            floatArrayOf(
                0.285f, -0.38f, 0.2f, -0.52f, 0.0f, -0.63f, -0.3f, -0.55f, -0.46f, -0.28f, -0.44f, 0.12f, -0.3f, 0.36f, -0.12f, 0.3f,
                -0.04f, 0.1f, 0.0f, -0.06f, 0.04f, -0.12f, 0.12f, -0.2f, 0.19f, -0.3f,
            ),
            closed = true,
        )
        c.drawPath(cap, s.gFill(L.hair))
        c.save(); c.clipPath(cap)
        s.softOval(-0.05f, -0.5f, 0.3f, 0.1f, L.hairHi, 140)
        for (k in 0 until 9) {
            val t = k / 8f
            s.strand(floatArrayOf(0.26f - t * 0.05f, -0.4f + t * 0.1f, 0.05f - t * 0.1f, -0.55f + t * 0.2f, -0.3f - t * 0.1f, -0.35f + t * 0.4f), L.hairHi, 0.004f, 90)
        }
        c.restore()
        c.drawPath(cap, s.gLine(withAlpha(L.accent, 140), 0.005f))
        // A loose wisp falling in front of the ear.
        s.strand(floatArrayOf(0.1f, -0.2f, 0.07f, 0.0f, 0.1f, 0.2f, 0.06f, 0.34f), L.hair, 0.012f, 255)
    }

    fun bust(s: Scene, L: Look, bottom: Float) {
        val c = s.canvas
        val torso = smoothPath(floatArrayOf(0.2f, 0.88f, 0.46f, 1.0f, 0.52f, 1.5f, 0.56f, bottom, -0.7f, bottom, -0.62f, 1.4f, -0.4f, 1.0f, -0.2f, 0.88f), closed = true, tension = 0.4f)
        c.drawPath(torso, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0.5f, 0.9f, -0.5f, 1.8f, lighten(L.cloth, 0.1f), darken(L.cloth, 0.4f), Shader.TileMode.CLAMP)
        })
        c.drawPath(torso, s.gLine(withAlpha(L.accent, 190), 0.007f))
        // Corset lacing down the front edge.
        var yy = 1.3f
        while (yy < bottom) { c.drawLine(0.3f, yy, 0.42f, yy + 0.06f, s.gLine(withAlpha(L.lace, 180), 0.005f)); c.drawLine(0.42f, yy, 0.3f, yy + 0.06f, s.gLine(withAlpha(L.lace, 180), 0.005f)); yy += 0.11f }
        // Puffed sleeve cap on the shoulder, then the arm hanging to a lace cuff.
        val arm = smoothPath(floatArrayOf(-0.32f, 1.35f, 0.14f, 1.35f, 0.17f, 1.9f, 0.1f, 2.6f, -0.3f, 2.6f, -0.36f, 1.9f), closed = true, tension = 0.4f)
        c.drawPath(arm, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(-0.3f, 1.4f, 0.15f, 2.0f, lighten(L.cloth, 0.12f), darken(L.cloth, 0.3f), Shader.TileMode.CLAMP)
        })
        c.drawPath(arm, s.gLine(withAlpha(L.accent, 200), 0.007f))
        val puff = smoothPath(floatArrayOf(-0.32f, 1.02f, -0.05f, 0.94f, 0.2f, 1.04f, 0.28f, 1.3f, 0.16f, 1.52f, -0.08f, 1.58f, -0.3f, 1.5f, -0.4f, 1.25f), closed = true)
        c.drawPath(puff, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(-0.05f, 1.15f, 0.5f, lighten(L.cloth, 0.16f), darken(L.cloth, 0.3f), Shader.TileMode.CLAMP)
        })
        c.save(); c.clipPath(puff)
        for (k in 0 until 4) c.drawArc(RectF(-0.3f, 0.98f + k * 0.1f, 0.2f, 1.3f + k * 0.1f), 200f, 140f, false, s.gLine(withAlpha(L.accent, 80), 0.005f))
        c.restore()
        c.drawPath(puff, s.gLine(withAlpha(L.accent, 215), 0.007f))
        for (k in 0..5) c.drawArc(RectF(-0.3f + k * 0.085f - 0.03f, 1.5f, -0.3f + k * 0.085f + 0.03f, 1.6f), 0f, 180f, false, s.gLine(withAlpha(L.lace, 210), 0.006f))
        // Stand collar and choker.
        val col = smoothPath(floatArrayOf(0.17f, 0.66f, 0.2f, 0.9f, 0.17f, 0.98f, -0.19f, 0.98f, -0.17f, 0.9f, -0.15f, 0.66f), closed = true, tension = 0.3f)
        c.drawPath(col, s.gFill(L.cloth))
        c.drawPath(col, s.gLine(withAlpha(L.lace, 230), 0.007f))
        for (k in -4..4) c.drawArc(RectF(k * 0.043f - 0.02f, 0.96f, k * 0.043f + 0.02f, 1.02f), 0f, 180f, false, s.gLine(withAlpha(L.lace, 210), 0.005f))
        val ch = smoothPath(floatArrayOf(-0.15f, 0.56f, 0.02f, 0.6f, 0.19f, 0.54f), closed = false)
        c.drawPath(ch, s.gLine(Color.rgb(12, 6, 16), 0.04f))
        c.drawPath(ch, s.gLine(withAlpha(L.accent, 210), 0.006f))
        val gem = Path(); gem.moveTo(0.1f, 0.58f); gem.lineTo(0.075f, 0.63f); gem.lineTo(0.1f, 0.71f); gem.lineTo(0.125f, 0.63f); gem.close()
        s.softOval(0.1f, 0.64f, 0.07f, 0.07f, L.accent, 120)
        c.drawPath(gem, s.gFill(L.accent)); c.drawPath(gem, s.gLine(Color.WHITE, 0.004f, 200))
    }
}

object GothRaven {
    /** A raven in profile facing right; origin at its feet. [size] scales the whole bird. */
    fun draw(s: Scene, L: Look, x: Float, y: Float, size: Float, rot: Float = 0f) {
        val c = s.canvas
        c.save(); c.translate(x, y); c.rotate(rot); c.scale(size, size)
        val body = smoothPath(
            floatArrayOf(
                0.4f, -0.5f, 0.4f, -0.5f, 0.3f, -0.62f, 0.2f, -0.6f, 0.14f, -0.5f, 0.06f, -0.4f, -0.1f, -0.32f, -0.3f, -0.18f, -0.6f, 0.0f, -0.9f, 0.12f, -0.9f, 0.12f,
                -0.86f, 0.2f, -0.5f, 0.12f, -0.3f, 0.02f, -0.1f, 0.02f, 0.08f, -0.1f, 0.18f, -0.3f, 0.24f, -0.42f, 0.3f, -0.48f,
            ),
            closed = true,
        )
        s.softOval(0f, -0.2f, 0.7f, 0.5f, L.accent, 40)
        c.drawPath(body, s.gFill(Color.rgb(10, 8, 16)))
        c.save(); c.clipPath(body)
        for (k in 0 until 6) {
            val t = k * 0.1f
            c.drawPath(smoothPath(floatArrayOf(0.05f - t, -0.28f + t * 0.5f, -0.18f - t, -0.18f + t * 0.5f, -0.45f - t, -0.05f + t * 0.5f), closed = false), s.gLine(withAlpha(L.hairHi, 150 - k * 15), 0.012f))
        }
        s.softOval(0.1f, -0.52f, 0.12f, 0.06f, L.hairHi, 120)
        c.restore()
        c.drawPath(body, s.gLine(withAlpha(L.accent, 210), 0.014f))
        // Beak, eye, legs.
        val beak = Path(); beak.moveTo(0.3f, -0.62f); beak.lineTo(0.46f, -0.5f); beak.lineTo(0.3f, -0.52f); beak.close()
        c.drawPath(beak, s.gFill(Color.rgb(24, 20, 30))); c.drawPath(beak, s.gLine(withAlpha(L.accent, 220), 0.01f))
        c.drawCircle(0.25f, -0.57f, 0.025f, s.gFill(L.eye)); c.drawCircle(0.255f, -0.57f, 0.011f, s.gFill(Color.BLACK))
        c.drawLine(0.0f, 0.0f, 0.0f, 0.1f, s.gLine(Color.rgb(180, 170, 190), 0.016f))
        c.drawLine(0.1f, -0.02f, 0.1f, 0.1f, s.gLine(Color.rgb(180, 170, 190), 0.016f))
        for (dx in floatArrayOf(-0.04f, 0.0f, 0.04f)) { c.drawLine(0f, 0.1f, dx - 0.04f, 0.12f, s.gLine(Color.rgb(180, 170, 190), 0.01f)); c.drawLine(0.1f, 0.1f, dx + 0.06f, 0.12f, s.gLine(Color.rgb(180, 170, 190), 0.01f)) }
        c.restore()
    }
}
