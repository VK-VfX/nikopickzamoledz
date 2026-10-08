package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import kotlin.math.sqrt

/** Cute cats ("gatti carini"): kawaii stickers or line art, with yarn, fish bones and paw prints. */
object Gatti : Style("gatti", "Gatti", Category.DOODLE) {
    override val autoPalettes = listOf("candy", "candy", "sakura", "sunset", "ocean", "aurora")
    override fun draw(s: Scene) = GattiPage.draw(s, filled = true)
}

object GattiSketch : Style("gatti_sketch", "Gatti Sketch", Category.DOODLE) {
    override val autoPalettes = listOf("hue", "sakura", "candy", "aurora", "graffiti", "violet")
    override fun draw(s: Scene) = GattiPage.draw(s, filled = false)
}

private object GattiPage {
    private val furs = intArrayOf(
        Color.rgb(255, 179, 107), // ginger
        Color.rgb(174, 178, 190), // grey
        Color.rgb(243, 221, 179), // cream
        Color.rgb(74, 74, 85), // black cat
        Color.rgb(242, 242, 242), // white
    )
    private val blush = Color.rgb(247, 161, 181)
    private val cardboard = Color.rgb(217, 168, 108)
    private val notes = arrayOf("miao~", "meow", "nya~", "purr", "mrrp", "miao!", "♥")

    /** One cat's drawing context. [w] is the outline width in unit space. */
    class Kit(val s: Scene, val p: Pen, val filled: Boolean, val ink: Int, val fur: Int, val w: Float, val tabby: Boolean)

    fun draw(s: Scene, filled: Boolean) {
        val pal = s.palette.colors
        val cell = 205f / sqrt(s.density)
        val cols = (s.w / cell).toInt() + 1
        val rows = (s.h / cell).toInt() + 1
        for (gy in 0..rows) for (gx in 0..cols) {
            val x = gx * cell + s.f(0.25f, 0.75f) * cell
            val y = gy * cell + s.f(0.25f, 0.75f) * cell
            val t = (x / s.w + y / s.h) / 2f
            val ink = if (filled) Color.WHITE else s.grad(t)
            if (s.chance(0.72f)) {
                cat(s, x, y, s.f(58f, 78f), s.f(-14f, 14f), filled, ink, pal)
            } else {
                accessory(s, x, y, filled, ink, pal)
            }
            // Little extras around each cell.
            repeat(2) {
                val ex = x + s.f(-cell / 2f, cell / 2f)
                val ey = y + s.f(-cell / 2f, cell / 2f)
                val c = if (filled) Color.WHITE else s.grad((ex / s.w + ey / s.h) / 2f)
                when (s.i(0, 4)) {
                    0 -> Doodles.draw(s, 15, ex, ey, s.f(9f, 14f), s.f(-30f, 30f), c, 2f, if (filled) intArrayOf(pal[s.i(0, pal.size)]) else null)
                    1 -> Doodles.draw(s, 1, ex, ey, s.f(8f, 12f), s.f(-20f, 20f), c, 2f, if (filled) intArrayOf(blush) else null)
                    2 -> Doodles.draw(s, 10, ex, ey, s.f(7f, 11f), 0f, c, 2f, if (filled) intArrayOf(pal[s.i(0, pal.size)]) else null)
                    else -> s.dot(ex, ey, s.f(2f, 3.5f), c, 200)
                }
            }
        }
    }

    private fun cat(s: Scene, x: Float, y: Float, size: Float, rot: Float, filled: Boolean, ink: Int, pal: IntArray) {
        val fur = if (s.chance(0.7f)) furs[s.i(0, furs.size)] else pal[s.i(0, pal.size)]
        val darker = ColorUtils.blendARGB(fur, Color.BLACK, 0.25f)
        val boxColor = if (s.chance(0.5f)) cardboard else pal[s.i(0, pal.size)]
        val tabby = (fur == furs[0] || fur == furs[1]) && s.chance(0.7f)
        val pose = s.i(0, 5)
        val mood = s.i(0, 3)
        s.unit(x, y, size, rot) {
            val w = 3f / size
            val pen = Pen(
                s, ink, w,
                if (filled) intArrayOf(fur, blush, boxColor, Color.WHITE, darker) else null,
                darkInk = !filled,
            )
            val k = Kit(s, pen, filled, ink, fur, w, tabby)
            when (pose) {
                0 -> sitting(k, mood)
                1 -> loaf(k, mood)
                2 -> sleeping(k)
                3 -> bigFace(k, mood)
                else -> boxCat(k, mood)
            }
        }
    }

    // ---------- Parts ----------

    private fun tail(k: Kit, path: Path) {
        if (k.filled) {
            k.s.canvas.drawPath(path, k.s.stroke(k.ink, 0.26f))
            k.s.canvas.drawPath(path, k.s.stroke(k.fur, 0.26f - 2f * k.w))
        } else {
            k.s.canvas.drawPath(path, k.s.stroke(k.ink, k.w))
        }
    }

    private fun ears(k: Kit, cx: Float, top: Float, half: Float, tilt: Float = 0f) {
        for (sgn in floatArrayOf(-1f, 1f)) {
            val bx = cx + sgn * half * 0.75f
            k.p.poly(
                bx - sgn * half * 0.05f, top + half * 0.55f,
                bx + sgn * half * 0.18f + tilt * sgn, top - half * 0.35f,
                bx - sgn * half * 0.55f, top + half * 0.18f,
                fill = 0,
            )
            k.p.poly(
                bx, top + half * 0.42f,
                bx + sgn * half * 0.12f + tilt * sgn, top - half * 0.15f,
                bx - sgn * half * 0.35f, top + half * 0.2f,
                fill = 1,
            )
        }
    }

    private fun stripes(k: Kit, cx: Float, cy: Float, sc: Float) {
        if (!k.tabby) return
        val color = if (k.filled) ColorUtils.blendARGB(k.fur, Color.BLACK, 0.3f) else k.ink
        val paint = k.s.stroke(color, if (k.filled) 0.06f * sc else k.w)
        for (dx in floatArrayOf(-0.12f, 0f, 0.12f)) {
            k.s.canvas.drawLine(cx + dx * sc, cy - 0.32f * sc, cx + dx * sc * 0.8f, cy - 0.18f * sc, paint)
        }
    }

    /** Eyes, nose, ω mouth, blush and whiskers. mood: 0 open eyes, 1 happy ^^, 2 sleepy. */
    private fun face(k: Kit, cx: Float, cy: Float, sc: Float, mood: Int, whiskers: Boolean = true) {
        val p = k.p
        val ex = 0.24f * sc
        when (mood) {
            0 -> for (sgn in floatArrayOf(-1f, 1f)) {
                p.darkDot(cx + sgn * ex, cy, 0.085f * sc)
                if (k.filled) p.dot(cx + sgn * ex - 0.025f * sc, cy - 0.03f * sc, 0.03f * sc)
            }
            1 -> for (sgn in floatArrayOf(-1f, 1f)) {
                p.arc(cx + sgn * ex - 0.09f * sc, cy - 0.06f * sc, cx + sgn * ex + 0.09f * sc, cy + 0.1f * sc, 200f, 140f)
            }
            else -> for (sgn in floatArrayOf(-1f, 1f)) {
                p.arc(cx + sgn * ex - 0.09f * sc, cy - 0.1f * sc, cx + sgn * ex + 0.09f * sc, cy + 0.04f * sc, 20f, 140f)
            }
        }
        p.poly(cx - 0.06f * sc, cy + 0.09f * sc, cx + 0.06f * sc, cy + 0.09f * sc, cx, cy + 0.16f * sc, fill = 1)
        p.arc(cx - 0.11f * sc, cy + 0.1f * sc, cx, cy + 0.22f * sc, 0f, 180f)
        p.arc(cx, cy + 0.1f * sc, cx + 0.11f * sc, cy + 0.22f * sc, 0f, 180f)
        for (sgn in floatArrayOf(-1f, 1f)) {
            val bx = cx + sgn * 0.38f * sc
            val by = cy + 0.13f * sc
            if (k.filled) {
                k.s.canvas.drawOval(RectF(bx - 0.09f * sc, by - 0.05f * sc, bx + 0.09f * sc, by + 0.05f * sc), k.s.fill(blush, 200))
            } else {
                for (j in 0 until 3) p.line(bx - 0.06f * sc + j * 0.05f * sc, by + 0.03f * sc, bx - 0.03f * sc + j * 0.05f * sc, by - 0.03f * sc)
            }
            if (whiskers) {
                p.line(cx + sgn * 0.3f * sc, cy + 0.12f * sc, cx + sgn * 0.62f * sc, cy + 0.05f * sc)
                p.line(cx + sgn * 0.3f * sc, cy + 0.18f * sc, cx + sgn * 0.62f * sc, cy + 0.22f * sc)
            }
        }
    }

    // ---------- Poses ----------

    private fun sitting(k: Kit, mood: Int) {
        val t = Path()
        t.moveTo(0.4f, 0.82f)
        t.cubicTo(1.05f, 0.8f, 1.02f, 0.05f, 0.72f, -0.12f)
        tail(k, t)
        k.p.oval(-0.55f, -0.15f, 0.55f, 0.95f, 0)
        if (k.filled && k.fur != Color.rgb(242, 242, 242)) k.p.oval(-0.26f, 0.2f, 0.26f, 0.82f, 3)
        k.p.oval(-0.4f, 0.78f, -0.08f, 0.98f, 0)
        k.p.oval(0.08f, 0.78f, 0.4f, 0.98f, 0)
        ears(k, 0f, -0.85f, 0.6f)
        k.p.oval(-0.62f, -0.95f, 0.62f, 0.1f, 0)
        stripes(k, 0f, -0.45f, 1f)
        face(k, 0f, -0.45f, 1f, mood)
    }

    private fun loaf(k: Kit, mood: Int) {
        k.p.rrect(-0.98f, -0.15f, 0.98f, 0.65f, 0.4f, 0)
        ears(k, -0.42f, -0.62f, 0.5f)
        k.p.oval(-0.95f, -0.75f, 0.1f, 0.22f, 0)
        stripes(k, -0.42f, -0.28f, 0.85f)
        face(k, -0.42f, -0.28f, 0.85f, mood)
        val t = Path()
        t.moveTo(0.9f, 0.4f)
        t.quadTo(0.35f, 0.82f, -0.25f, 0.6f)
        tail(k, t)
    }

    private fun sleeping(k: Kit) {
        k.p.oval(-0.98f, -0.42f, 0.98f, 0.65f, 0)
        ears(k, -0.4f, -0.45f, 0.45f, tilt = -0.1f)
        k.p.oval(-0.88f, -0.5f, 0.08f, 0.32f, 0)
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
        k.p.oval(-0.92f, -0.72f, 0.92f, 0.8f, 0)
        stripes(k, 0f, 0f, 1.5f)
        face(k, 0f, 0.02f, 1.5f, mood)
    }

    private fun boxCat(k: Kit, mood: Int) {
        k.p.poly(-0.85f, 0.15f, -1.05f, -0.15f, -0.4f, -0.15f, -0.25f, 0.15f, fill = 2)
        k.p.poly(0.85f, 0.15f, 1.05f, -0.15f, 0.4f, -0.15f, 0.25f, 0.15f, fill = 2)
        ears(k, 0f, -0.78f, 0.55f)
        k.p.oval(-0.58f, -0.9f, 0.58f, 0.2f, 0)
        stripes(k, 0f, -0.38f, 0.95f)
        face(k, 0f, -0.38f, 0.95f, mood)
        k.p.rrect(-0.85f, 0.12f, 0.85f, 0.98f, 0.04f, 2)
        k.p.line(-0.85f, 0.3f, 0.85f, 0.3f)
        k.p.oval(-0.48f, 0.02f, -0.18f, 0.24f, 0)
        k.p.oval(0.18f, 0.02f, 0.48f, 0.24f, 0)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = k.ink
            textSize = 0.24f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        k.s.canvas.drawText(if (k.s.chance(0.5f)) "MIAO" else "♥ CAT ♥", 0f, 0.7f, tp)
    }

    // ---------- Accessories ----------

    private fun accessory(s: Scene, x: Float, y: Float, filled: Boolean, ink: Int, pal: IntArray) {
        val size = s.f(30f, 44f)
        s.unit(x, y, size, s.f(-25f, 25f)) {
            val w = 3f / size
            val p = Pen(s, ink, w, if (filled) intArrayOf(pal[s.i(0, pal.size)], blush, Color.WHITE) else null, darkInk = !filled)
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
