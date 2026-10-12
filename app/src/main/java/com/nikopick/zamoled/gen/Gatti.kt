package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import com.nikopick.zamoled.gen.Shade.coat
import com.nikopick.zamoled.gen.Shade.eye
import com.nikopick.zamoled.gen.Shade.fur
import com.nikopick.zamoled.gen.Shade.groundShadow
import com.nikopick.zamoled.gen.Shade.occlude
import com.nikopick.zamoled.gen.Shade.sheen
import kotlin.math.sqrt

/** Painted cats ("gatti carini") in their own coats, with yarn, fish bones and paw prints. */
object Gatti : Style("gatti", "Gatti", Category.DOODLE) {
    override val autoPalettes = listOf("candy", "candy", "sakura", "sunset", "ocean", "aurora")
    override fun draw(s: Scene) = GattiPage.draw(s, natural = true)
}

/** The same cats, but coated in the palette rather than in tabby and ginger. */
object GattiSketch : Style("gatti_sketch", "Gatti Colour", Category.DOODLE) {
    override val autoPalettes = listOf("hue", "sakura", "candy", "aurora", "graffiti", "violet")
    override fun draw(s: Scene) = GattiPage.draw(s, natural = false)
}

private object GattiPage {
    private val furs = intArrayOf(
        Color.rgb(255, 179, 107), // ginger
        Color.rgb(174, 178, 190), // grey
        Color.rgb(243, 221, 179), // cream
        Color.rgb(96, 92, 104), // black cat
        Color.rgb(242, 242, 242), // white
    )
    private val irises = intArrayOf(
        Color.rgb(126, 196, 84), Color.rgb(228, 178, 62), Color.rgb(96, 170, 222), Color.rgb(176, 140, 72),
    )
    private val blush = Color.rgb(247, 161, 181)
    private val cardboard = Color.rgb(217, 168, 108)
    private val notes = arrayOf("miao~", "meow", "nya~", "purr", "mrrp", "miao!", "\u2665")

    /** One cat's painting context. [w] is the line width in unit space. */
    class Kit(val s: Scene, val p: Pen, val ink: Int, val fur: Int, val iris: Int, val w: Float, val tabby: Boolean)

    fun draw(s: Scene, natural: Boolean) {
        val pal = s.palette.colors
        val cell = 205f / sqrt(s.density)
        for (p in s.spots(s.layout, 205f)) {
            val ink = Color.WHITE
            if (s.chance(0.72f)) {
                cat(s, p.x, p.y, s.f(58f, 78f) * p.scale, p.rot, natural, ink, pal)
            } else {
                accessory(s, p.x, p.y, true, ink, pal)
            }
            // Little extras around each cat.
            repeat(2) {
                val ex = p.x + s.f(-cell / 2f, cell / 2f)
                val ey = p.y + s.f(-cell / 2f, cell / 2f)
                when (s.i(0, 4)) {
                    0 -> Doodles.draw(s, 15, ex, ey, s.f(9f, 14f), s.f(-30f, 30f), ink, 2f, intArrayOf(pal[s.i(0, pal.size)]))
                    1 -> Doodles.draw(s, 1, ex, ey, s.f(8f, 12f), s.f(-20f, 20f), ink, 2f, intArrayOf(blush))
                    2 -> Doodles.draw(s, 10, ex, ey, s.f(7f, 11f), 0f, ink, 2f, intArrayOf(pal[s.i(0, pal.size)]))
                    else -> s.dot(ex, ey, s.f(2f, 3.5f), ink, 200)
                }
            }
        }
    }

    private fun cat(s: Scene, x: Float, y: Float, size: Float, rot: Float, natural: Boolean, ink: Int, pal: IntArray) {
        val fur = if (natural) furs[s.i(0, furs.size)] else pal[s.i(0, pal.size)]
        val darker = ColorUtils.blendARGB(fur, Color.BLACK, 0.25f)
        val boxColor = if (s.chance(0.5f)) cardboard else pal[s.i(0, pal.size)]
        val tabby = s.chance(0.55f)
        val iris = irises[s.i(0, irises.size)]
        val pose = s.i(0, 5)
        val mood = s.i(0, 3)
        s.unit(x, y, size, rot) {
            val w = 3f / size
            val pen = Pen(s, ink, w, intArrayOf(fur, blush, boxColor, Color.WHITE, darker), shaded = true)
            val k = Kit(s, pen, ink, fur, iris, w, tabby)
            when (pose) {
                0 -> sitting(k, mood)
                1 -> loaf(k, mood)
                2 -> sleeping(k)
                3 -> bigFace(k, mood)
                else -> boxCat(k, mood)
            }
        }
    }

    // ---------- Painting ----------

    private fun oval(l: Float, t: Float, r: Float, b: Float): Path =
        Path().apply { addOval(RectF(l, t, r, b), Path.Direction.CW) }

    /**
     * Paint one mass of cat: fur combed out along the silhouette, a lit body under it,
     * guard hairs across the coat, shadow where the light does not reach, and a soft sheen.
     */
    private fun body(k: Kit, vararg parts: Path) {
        val sil = Path()
        for (part in parts) sil.op(part, Path.Op.UNION)
        val box = RectF()
        sil.computeBounds(box, true)
        val c = k.s.canvas

        k.s.fur(sil, ColorUtils.blendARGB(k.fur, Color.BLACK, 0.3f), 0.075f, 0.03f)
        c.drawPath(sil, Shade.volume(sil, k.fur))
        c.save()
        c.clipPath(sil)
        k.s.coat(box, lighten(k.fur, 0.35f), 70, 0.1f)
        k.s.coat(box, darken(k.fur, 0.5f), 50, 0.13f)
        c.restore()
        k.s.occlude(sil, 0.16f, 150)
        k.s.sheen(box, 60)
        c.drawPath(sil, k.s.stroke(darken(k.fur, 0.55f), k.w * 1.1f, 210))
    }

    private fun tail(k: Kit, path: Path) {
        k.s.canvas.drawPath(path, k.s.stroke(darken(k.fur, 0.55f), 0.3f))
        k.s.canvas.drawPath(path, k.s.stroke(k.fur, 0.26f))
        k.s.canvas.drawPath(path, k.s.stroke(lighten(k.fur, 0.3f), 0.1f, 150))
        k.s.fur(path, darken(k.fur, 0.35f), 0.06f, 0.05f)
    }

    private fun ears(k: Kit, cx: Float, top: Float, half: Float, tilt: Float = 0f) {
        for (sgn in floatArrayOf(-1f, 1f)) {
            val bx = cx + sgn * half * 0.75f
            val outer = Path().apply {
                moveTo(bx - sgn * half * 0.05f, top + half * 0.55f)
                lineTo(bx + sgn * half * 0.18f + tilt * sgn, top - half * 0.35f)
                lineTo(bx - sgn * half * 0.55f, top + half * 0.18f)
                close()
            }
            k.s.fur(outer, darken(k.fur, 0.3f), 0.05f, 0.035f)
            k.s.canvas.drawPath(outer, Shade.volume(outer, k.fur))
            k.s.canvas.drawPath(outer, k.s.stroke(darken(k.fur, 0.55f), k.w, 210))
            val inner = Path().apply {
                moveTo(bx, top + half * 0.42f)
                lineTo(bx + sgn * half * 0.12f + tilt * sgn, top - half * 0.15f)
                lineTo(bx - sgn * half * 0.35f, top + half * 0.2f)
                close()
            }
            k.s.canvas.drawPath(inner, Shade.volume(inner, blush))
        }
    }

    private fun stripes(k: Kit, cx: Float, cy: Float, sc: Float) {
        if (!k.tabby) return
        val color = ColorUtils.blendARGB(k.fur, Color.BLACK, 0.42f)
        for (dx in floatArrayOf(-0.13f, 0f, 0.13f)) {
            val s = Path()
            s.moveTo(cx + dx * sc, cy - 0.34f * sc)
            s.quadTo(cx + dx * sc * 0.9f, cy - 0.26f * sc, cx + dx * sc * 0.75f, cy - 0.16f * sc)
            k.s.canvas.drawPath(s, k.s.stroke(color, 0.07f * sc, 190))
        }
    }

    /** Eyes, nose, mouth, blush and whiskers. mood: 0 open, 1 happy, 2 sleepy. */
    private fun face(k: Kit, cx: Float, cy: Float, sc: Float, mood: Int, whiskers: Boolean = true) {
        val ex = 0.24f * sc
        when (mood) {
            0 -> for (sgn in floatArrayOf(-1f, 1f)) k.s.eye(cx + sgn * ex, cy, 0.115f * sc, k.iris)
            1 -> for (sgn in floatArrayOf(-1f, 1f)) k.s.canvas.drawArc(
                RectF(cx + sgn * ex - 0.09f * sc, cy - 0.06f * sc, cx + sgn * ex + 0.09f * sc, cy + 0.1f * sc),
                200f, 140f, false, k.s.stroke(Color.rgb(28, 24, 30), 0.045f * sc),
            )
            else -> for (sgn in floatArrayOf(-1f, 1f)) k.s.canvas.drawArc(
                RectF(cx + sgn * ex - 0.09f * sc, cy - 0.1f * sc, cx + sgn * ex + 0.09f * sc, cy + 0.04f * sc),
                20f, 140f, false, k.s.stroke(Color.rgb(28, 24, 30), 0.045f * sc),
            )
        }
        // Muzzle: two soft pads under a shaded nose.
        for (sgn in floatArrayOf(-1f, 1f)) {
            k.s.canvas.drawOval(
                RectF(cx + sgn * 0.075f * sc - 0.08f * sc, cy + 0.1f * sc, cx + sgn * 0.075f * sc + 0.08f * sc, cy + 0.23f * sc),
                k.s.fill(lighten(k.fur, 0.3f), 160),
            )
        }
        val nose = Path().apply {
            moveTo(cx - 0.06f * sc, cy + 0.085f * sc)
            lineTo(cx + 0.06f * sc, cy + 0.085f * sc)
            lineTo(cx, cy + 0.165f * sc)
            close()
        }
        k.s.canvas.drawPath(nose, Shade.volume(nose, Color.rgb(226, 138, 150)))
        k.s.canvas.drawCircle(cx - 0.02f * sc, cy + 0.105f * sc, 0.016f * sc, k.s.fill(Color.WHITE, 200))
        val mouth = k.s.stroke(Color.rgb(40, 32, 38), 0.035f * sc, 200)
        k.s.canvas.drawArc(RectF(cx - 0.11f * sc, cy + 0.1f * sc, cx, cy + 0.22f * sc), 0f, 180f, false, mouth)
        k.s.canvas.drawArc(RectF(cx, cy + 0.1f * sc, cx + 0.11f * sc, cy + 0.22f * sc), 0f, 180f, false, mouth)
        for (sgn in floatArrayOf(-1f, 1f)) {
            val bx = cx + sgn * 0.38f * sc
            val by = cy + 0.13f * sc
            k.s.canvas.drawOval(
                RectF(bx - 0.1f * sc, by - 0.055f * sc, bx + 0.1f * sc, by + 0.055f * sc),
                k.s.fill(blush, 120, 0.04f * sc),
            )
            if (whiskers) for (j in 0 until 3) {
                val dy = (j - 1) * 0.07f * sc
                k.s.canvas.drawLine(
                    cx + sgn * 0.3f * sc, cy + 0.14f * sc + dy * 0.4f,
                    cx + sgn * 0.66f * sc, cy + 0.1f * sc + dy,
                    k.s.stroke(Color.WHITE, 0.018f * sc, 150),
                )
            }
        }
    }

    // ---------- Poses ----------

    private fun sitting(k: Kit, mood: Int) {
        k.s.groundShadow(0f, 1.0f, 0.62f, 0.12f, 130)
        val t = Path()
        t.moveTo(0.4f, 0.82f)
        t.cubicTo(1.05f, 0.8f, 1.02f, 0.05f, 0.72f, -0.12f)
        tail(k, t)
        ears(k, 0f, -0.85f, 0.6f)
        body(
            k,
            oval(-0.55f, -0.15f, 0.55f, 0.95f),
            oval(-0.4f, 0.78f, -0.08f, 0.98f),
            oval(0.08f, 0.78f, 0.4f, 0.98f),
            oval(-0.62f, -0.95f, 0.62f, 0.1f),
        )
        k.s.canvas.drawOval(RectF(-0.26f, 0.25f, 0.26f, 0.85f), k.s.fill(lighten(k.fur, 0.4f), 90, 0.08f))
        stripes(k, 0f, -0.45f, 1f)
        face(k, 0f, -0.45f, 1f, mood)
    }

    private fun loaf(k: Kit, mood: Int) {
        k.s.groundShadow(0f, 0.68f, 0.95f, 0.11f, 130)
        val t = Path()
        t.moveTo(0.9f, 0.4f)
        t.quadTo(0.35f, 0.82f, -0.25f, 0.6f)
        tail(k, t)
        ears(k, -0.42f, -0.62f, 0.5f)
        val loafBody = Path().apply { addRoundRect(RectF(-0.98f, -0.15f, 0.98f, 0.65f), 0.4f, 0.4f, Path.Direction.CW) }
        body(k, loafBody, oval(-0.95f, -0.75f, 0.1f, 0.22f))
        stripes(k, -0.42f, -0.28f, 0.85f)
        face(k, -0.42f, -0.28f, 0.85f, mood)
    }

    private fun sleeping(k: Kit) {
        k.s.groundShadow(0f, 0.7f, 1.0f, 0.1f, 130)
        ears(k, -0.4f, -0.45f, 0.45f, tilt = -0.1f)
        body(k, oval(-0.98f, -0.42f, 0.98f, 0.65f), oval(-0.88f, -0.5f, 0.08f, 0.32f))
        stripes(k, -0.4f, -0.08f, 0.8f)
        face(k, -0.4f, -0.08f, 0.8f, 2, whiskers = false)
        val t = Path()
        t.moveTo(0.88f, 0.2f)
        t.cubicTo(0.85f, 0.85f, -0.3f, 0.88f, -0.72f, 0.45f)
        tail(k, t)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = k.ink
            typeface = Typeface.DEFAULT_BOLD
        }
        tp.textSize = 0.24f
        k.s.canvas.drawText("z", 0.38f, -0.5f, tp)
        tp.textSize = 0.36f
        k.s.canvas.drawText("Z", 0.6f, -0.78f, tp)
    }

    private fun bigFace(k: Kit, mood: Int) {
        ears(k, 0f, -0.62f, 0.85f)
        body(k, oval(-0.92f, -0.72f, 0.92f, 0.8f))
        stripes(k, 0f, 0f, 1.5f)
        face(k, 0f, 0.02f, 1.5f, mood)
    }

    private fun boxCat(k: Kit, mood: Int) {
        k.s.groundShadow(0f, 1.0f, 0.9f, 0.1f, 130)
        k.p.poly(-0.85f, 0.15f, -1.05f, -0.15f, -0.4f, -0.15f, -0.25f, 0.15f, fill = 2)
        k.p.poly(0.85f, 0.15f, 1.05f, -0.15f, 0.4f, -0.15f, 0.25f, 0.15f, fill = 2)
        ears(k, 0f, -0.78f, 0.55f)
        body(k, oval(-0.58f, -0.9f, 0.58f, 0.2f))
        stripes(k, 0f, -0.38f, 0.95f)
        face(k, 0f, -0.38f, 0.95f, mood)
        k.p.rrect(-0.85f, 0.12f, 0.85f, 0.98f, 0.04f, 2)
        k.p.line(-0.85f, 0.3f, 0.85f, 0.3f)
        val paw = k.s.fill(lighten(k.fur, 0.2f))
        for (sgn in floatArrayOf(-1f, 1f)) {
            val px = sgn * 0.33f
            k.s.canvas.drawOval(RectF(px - 0.15f, 0.02f, px + 0.15f, 0.24f), paw)
            k.s.canvas.drawOval(RectF(px - 0.15f, 0.02f, px + 0.15f, 0.24f), k.s.stroke(darken(k.fur, 0.5f), k.w, 200))
        }
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = k.ink
            textSize = 0.24f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        k.s.canvas.drawText(if (k.s.chance(0.5f)) "MIAO" else "\u2665 CAT \u2665", 0f, 0.7f, tp)
    }

    // ---------- Accessories ----------

    private fun accessory(s: Scene, x: Float, y: Float, filled: Boolean, ink: Int, pal: IntArray) {
        val size = s.f(30f, 44f)
        s.unit(x, y, size, s.f(-25f, 25f)) {
            val w = 3f / size
            val p = Pen(s, ink, w, intArrayOf(pal[s.i(0, pal.size)], blush, Color.WHITE), shaded = true)
            when (s.i(0, 4)) {
                0 -> yarn(s, p, filled, ink, w)
                1 -> fishBone(p)
                2 -> mouse(p, ink, filled, s, w)
                else -> note(s, ink)
            }
        }
    }

    private fun yarn(s: Scene, p: Pen, filled: Boolean, ink: Int, w: Float) {
        val strand = Path()
        strand.moveTo(0.6f, 0.6f)
        strand.cubicTo(1.0f, 0.9f, 1.1f, 0.3f, 1.4f, 0.7f)
        s.canvas.drawPath(strand, s.stroke(ink, w))
        val ball = Path()
        ball.addCircle(0f, 0f, 0.85f, Path.Direction.CW)
        p.path(ball, 0)
        s.canvas.save()
        s.canvas.clipPath(ball)
        val line = s.stroke(if (filled) Color.WHITE else ink, w, 200)
        for (k in -2..2) {
            val o = k * 0.3f
            val arc = Path()
            arc.moveTo(-1f, o - 0.3f)
            arc.quadTo(0f, o + 0.4f, 1f, o - 0.2f)
            s.canvas.drawPath(arc, line)
        }
        s.canvas.restore()
    }

    private fun fishBone(p: Pen) {
        p.line(-0.6f, 0f, 0.55f, 0f)
        for (k in 0 until 4) {
            val x = -0.35f + k * 0.25f
            p.line(x, 0f, x + 0.1f, -0.28f)
            p.line(x, 0f, x + 0.1f, 0.28f)
        }
        p.oval(0.45f, -0.3f, 1.0f, 0.3f, 0)
        p.darkDot(0.78f, -0.06f, 0.06f)
        p.poly(-0.6f, 0f, -0.95f, -0.3f, -0.95f, 0.3f, fill = 0)
    }

    private fun mouse(p: Pen, ink: Int, filled: Boolean, s: Scene, w: Float) {
        val t = Path()
        t.moveTo(-0.7f, 0.2f)
        t.cubicTo(-1.1f, 0.3f, -1.0f, 0.75f, -1.3f, 0.6f)
        s.canvas.drawPath(t, s.stroke(ink, w))
        val body = Path()
        body.moveTo(0.9f, 0.15f)
        body.cubicTo(0.3f, -0.6f, -0.8f, -0.5f, -0.75f, 0.3f)
        body.lineTo(0.9f, 0.3f)
        body.close()
        p.path(body, 0)
        p.circle(0.35f, -0.32f, 0.18f, 1)
        p.darkDot(0.6f, 0.02f, 0.06f)
        if (!filled) p.dot(0.92f, 0.2f, 0.05f)
    }

    private fun note(s: Scene, ink: Int) {
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 0.55f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
        }
        s.canvas.drawText(notes[s.i(0, notes.size)], 0f, 0.2f, tp)
    }
}
