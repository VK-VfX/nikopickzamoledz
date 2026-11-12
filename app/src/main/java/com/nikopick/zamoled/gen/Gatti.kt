package com.nikopick.zamoled.gen

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cats of many breeds, drawn as comic characters: big round heads, huge eyes, flat colour under a
 * bold outline, each with the markings that make its breed what it is (tabby stripes, bengal
 * rosettes, siamese points, calico patches). There is no fur: they are cartoons, not portraits.
 */

private enum class Mark { TABBY, BENGAL, SIAMESE, CALICO, TUXEDO, RAGDOLL, PLAIN, SPHYNX, MAINE }
private enum class EarKind { NORMAL, ROUND, TALL, FOLD, BIG }

private class Breed(
    val name: String,
    val base: Int,
    val alt: Int,
    val dark: Int,
    val iris: Int,
    val mark: Mark,
    val ears: EarKind,
    val hw: Float,
    val hh: Float,
    val puffy: Boolean = false,
    val pointy: Boolean = false,
)

private val CREAM = Color.rgb(255, 247, 238)
private val PINK = Color.rgb(255, 150, 165)
private val INK = Color.rgb(44, 30, 46)
private val SCLERA = Color.rgb(255, 255, 250)

private val BREEDS = listOf(
    Breed("Tabby", Color.rgb(238, 150, 70), Color.rgb(255, 204, 150), Color.rgb(172, 92, 38), Color.rgb(118, 200, 92), Mark.TABBY, EarKind.NORMAL, 0.7f, 0.56f),
    Breed("British Shorthair", Color.rgb(142, 158, 190), Color.rgb(176, 190, 214), Color.rgb(96, 110, 142), Color.rgb(255, 170, 40), Mark.PLAIN, EarKind.ROUND, 0.8f, 0.58f, puffy = true),
    Breed("Bengal", Color.rgb(232, 170, 72), Color.rgb(255, 226, 172), Color.rgb(92, 52, 28), Color.rgb(104, 204, 124), Mark.BENGAL, EarKind.NORMAL, 0.7f, 0.55f),
    Breed("Siamese", Color.rgb(246, 230, 204), Color.rgb(220, 194, 154), Color.rgb(112, 76, 58), Color.rgb(90, 172, 255), Mark.SIAMESE, EarKind.BIG, 0.64f, 0.56f, pointy = true),
    Breed("Calico", Color.rgb(252, 248, 242), Color.rgb(240, 152, 62), Color.rgb(52, 46, 54), Color.rgb(112, 172, 92), Mark.CALICO, EarKind.NORMAL, 0.7f, 0.56f),
    Breed("Black Cat", Color.rgb(60, 58, 74), Color.rgb(92, 90, 110), Color.rgb(34, 32, 44), Color.rgb(255, 214, 60), Mark.PLAIN, EarKind.NORMAL, 0.68f, 0.56f),
    Breed("Tuxedo", Color.rgb(60, 58, 74), Color.rgb(252, 250, 246), Color.rgb(34, 32, 44), Color.rgb(200, 226, 90), Mark.TUXEDO, EarKind.NORMAL, 0.7f, 0.56f),
    Breed("Sphynx", Color.rgb(236, 192, 172), Color.rgb(214, 160, 142), Color.rgb(176, 124, 114), Color.rgb(130, 214, 204), Mark.SPHYNX, EarKind.BIG, 0.62f, 0.54f),
    Breed("Persian", Color.rgb(255, 250, 238), Color.rgb(244, 224, 192), Color.rgb(196, 168, 140), Color.rgb(255, 152, 54), Mark.PLAIN, EarKind.ROUND, 0.82f, 0.6f, puffy = true),
    Breed("Maine Coon", Color.rgb(152, 132, 114), Color.rgb(204, 188, 164), Color.rgb(90, 74, 64), Color.rgb(232, 192, 62), Mark.MAINE, EarKind.TALL, 0.74f, 0.58f),
    Breed("Scottish Fold", Color.rgb(216, 208, 198), Color.rgb(252, 250, 246), Color.rgb(152, 142, 132), Color.rgb(255, 182, 62), Mark.PLAIN, EarKind.FOLD, 0.76f, 0.58f, puffy = true),
    Breed("Ragdoll", Color.rgb(248, 240, 228), Color.rgb(255, 255, 250), Color.rgb(108, 90, 90), Color.rgb(104, 174, 255), Mark.RAGDOLL, EarKind.NORMAL, 0.72f, 0.57f),
)

private object CatArt {
    private fun paint(color: Int, alpha: Int = 255) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.alpha = alpha }

    private fun line(color: Int, width: Float, alpha: Int = 255, round: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.alpha = alpha
        style = Paint.Style.STROKE
        strokeWidth = width
        if (round) strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // ---- one cat ----

    /** Draws [breed] at (x, y), [size] units wide. [pose]: 0 sitting, 1 loaf, 2 big face. */
    fun cat(s: Scene, x: Float, y: Float, size: Float, rot: Float, b: Breed, pose: Int, mood: Int, caption: Boolean) {
        val w = 3.6f / size
        val fills = intArrayOf(b.base, b.alt, b.dark, CREAM, PINK, b.iris)
        s.unit(x, y, size, rot) {
            for (look in arrayOf(PenLook.SHADOW, PenLook.DIECUT, PenLook.COMIC)) {
                val pen = Pen(s, INK, w, fills, look = look)
                when (pose) {
                    0 -> sit(pen, b, mood)
                    1 -> loaf(pen, b, mood)
                    else -> face(pen, b, mood)
                }
            }
            if (caption && pose != 2) {
                // Text this small has to be drawn big and scaled down, or the glyphs collapse into blocks.
                val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(230, 255, 247, 238)
                    textSize = 24f
                    typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    letterSpacing = 0.1f
                }
                s.canvas.save()
                s.canvas.scale(0.01f, 0.01f)
                s.canvas.drawText(b.name.uppercase(), 0f, 148f, tp)
                s.canvas.restore()
            }
        }
    }

    private fun pawFill(b: Breed) = when (b.mark) {
        Mark.SIAMESE -> 2
        Mark.RAGDOLL, Mark.TUXEDO -> 3
        else -> 0
    }

    private fun sit(p: Pen, b: Breed, mood: Int) {
        val thick = when (b.mark) { Mark.MAINE -> 0.32f; Mark.SPHYNX -> 0.1f; else -> 0.22f }
        tail(p, b, Path().apply { moveTo(0.5f, 0.88f); cubicTo(1.15f, 0.92f, 1.2f, 0.3f, 0.92f, 0.02f) }, thick)
        val body = smoothPath(floatArrayOf(-0.34f, -0.1f, -0.6f, 0.3f, -0.64f, 0.75f, -0.32f, 0.96f, 0.32f, 0.96f, 0.64f, 0.75f, 0.6f, 0.3f, 0.34f, -0.1f), true, 0.5f)
        p.path(body, 0)
        bodyMarks(p, b, body)
        p.oval(-0.5f, 0.8f, -0.1f, 1.06f, pawFill(b))
        p.oval(0.1f, 0.8f, 0.5f, 1.06f, pawFill(b))
        p.c.save()
        p.c.translate(0f, -0.58f)
        head(p, b, mood)
        p.c.restore()
    }

    private fun loaf(p: Pen, b: Breed, mood: Int) {
        val thick = when (b.mark) { Mark.MAINE -> 0.3f; Mark.SPHYNX -> 0.1f; else -> 0.2f }
        tail(p, b, Path().apply { moveTo(0.84f, 0.5f); cubicTo(1.4f, 0.5f, 1.4f, -0.1f, 1.05f, -0.12f) }, thick)
        val body = Path().apply { addRoundRect(RectF(-0.98f, -0.14f, 0.98f, 0.7f), 0.42f, 0.42f, Path.Direction.CW) }
        p.path(body, 0)
        bodyMarks(p, b, body)
        p.oval(-0.7f, 0.56f, -0.28f, 0.8f, pawFill(b))
        p.c.save()
        p.c.translate(-0.4f, -0.22f)
        p.c.scale(0.92f, 0.92f)
        head(p, b, mood)
        p.c.restore()
    }

    private fun face(p: Pen, b: Breed, mood: Int) {
        p.c.save()
        p.c.translate(0f, 0.08f)
        p.c.scale(1.45f, 1.45f)
        head(p, b, mood)
        p.c.restore()
    }

    private fun tail(p: Pen, b: Breed, path: Path, thick: Float) {
        val c = p.c
        val lw = p.lw
        val color = if (b.mark == Mark.SIAMESE || b.mark == Mark.RAGDOLL) b.dark else b.base
        when (p.look) {
            PenLook.SHADOW -> {
                c.save()
                c.translate(lw * 0.9f, lw * 1.6f)
                c.drawPath(path, line(Color.argb(150, 0, 0, 0), thick + 4.6f * lw).apply { maskFilter = BlurMaskFilter(lw * 2.6f, BlurMaskFilter.Blur.NORMAL) })
                c.restore()
            }
            PenLook.DIECUT -> c.drawPath(path, line(CREAM, thick + 4.6f * lw))
            else -> {
                c.drawPath(path, line(INK, thick + 2.3f * lw))
                c.drawPath(path, line(color, thick))
                if (b.mark == Mark.TABBY || b.mark == Mark.BENGAL || b.mark == Mark.MAINE) {
                    // Rings round the tail.
                    val m = android.graphics.PathMeasure(path, false)
                    val pos = FloatArray(2)
                    val tan = FloatArray(2)
                    var d = m.length * 0.25f
                    while (d < m.length * 0.95f) {
                        m.getPosTan(d, pos, tan)
                        c.drawLine(pos[0] - tan[1] * thick * 0.5f, pos[1] + tan[0] * thick * 0.5f, pos[0] + tan[1] * thick * 0.5f, pos[1] - tan[0] * thick * 0.5f, line(b.dark, lw * 1.9f))
                        d += m.length * 0.15f
                    }
                }
            }
        }
    }

    // ---- head ----

    private fun headPath(b: Breed): Path {
        val hw = b.hw
        val hh = b.hh
        if (b.pointy) {
            return smoothPath(floatArrayOf(0f, -hh, hw * 0.8f, -hh * 0.55f, hw * 0.95f, 0f, hw * 0.6f, hh * 0.68f, 0f, hh * 1.05f, -hw * 0.6f, hh * 0.68f, -hw * 0.95f, 0f, -hw * 0.8f, -hh * 0.55f), true, 0.5f)
        }
        val oval = Path().apply { addOval(RectF(-hw, -hh, hw, hh), Path.Direction.CW) }
        if (b.puffy) {
            oval.op(Path().apply { addCircle(-hw * 0.82f, hh * 0.34f, hh * 0.46f, Path.Direction.CW) }, Path.Op.UNION)
            oval.op(Path().apply { addCircle(hw * 0.82f, hh * 0.34f, hh * 0.46f, Path.Direction.CW) }, Path.Op.UNION)
        }
        return oval
    }

    private fun earFill(b: Breed, sgn: Float): Int = when (b.mark) {
        Mark.SIAMESE, Mark.RAGDOLL -> 2
        Mark.CALICO -> if (sgn < 0f) 1 else 2
        else -> 0
    }

    private fun ears(p: Pen, b: Breed) {
        val hw = b.hw
        val hh = b.hh
        for (sgn in floatArrayOf(-1f, 1f)) {
            val fill = earFill(b, sgn)
            when (b.ears) {
                EarKind.NORMAL -> {
                    p.path(Path().apply { moveTo(sgn * hw * 0.15f, -hh * 0.75f); lineTo(sgn * hw * 0.62f, -hh * 1.4f); lineTo(sgn * hw * 0.94f, -hh * 0.3f); close() }, fill)
                    p.path(Path().apply { moveTo(sgn * hw * 0.34f, -hh * 0.82f); lineTo(sgn * hw * 0.62f, -hh * 1.14f); lineTo(sgn * hw * 0.78f, -hh * 0.55f); close() }, 4)
                }
                EarKind.ROUND -> {
                    p.path(Path().apply { moveTo(sgn * hw * 0.2f, -hh * 0.8f); quadTo(sgn * hw * 0.62f, -hh * 1.5f, sgn * hw * 0.96f, -hh * 0.4f); close() }, fill)
                    p.path(Path().apply { moveTo(sgn * hw * 0.38f, -hh * 0.84f); quadTo(sgn * hw * 0.62f, -hh * 1.2f, sgn * hw * 0.8f, -hh * 0.58f); close() }, 4)
                }
                EarKind.TALL -> {
                    p.path(Path().apply { moveTo(sgn * hw * 0.18f, -hh * 0.75f); lineTo(sgn * hw * 0.5f, -hh * 1.7f); lineTo(sgn * hw * 0.62f, -hh * 2.0f); lineTo(sgn * hw * 0.74f, -hh * 1.6f); lineTo(sgn * hw * 0.95f, -hh * 0.35f); close() }, fill)
                    p.path(Path().apply { moveTo(sgn * hw * 0.36f, -hh * 0.82f); lineTo(sgn * hw * 0.6f, -hh * 1.5f); lineTo(sgn * hw * 0.78f, -hh * 0.55f); close() }, 4)
                }
                EarKind.FOLD -> {
                    p.path(Path().apply { addOval(RectF(sgn * hw * 0.72f - hw * 0.26f, -hh * 0.98f, sgn * hw * 0.72f + hw * 0.26f, -hh * 0.56f), Path.Direction.CW) }, fill)
                }
                EarKind.BIG -> {
                    p.path(Path().apply { moveTo(sgn * hw * 0.08f, -hh * 0.7f); lineTo(sgn * hw * 0.78f, -hh * 1.95f); lineTo(sgn * hw * 1.02f, -hh * 0.15f); close() }, fill)
                    p.path(Path().apply { moveTo(sgn * hw * 0.3f, -hh * 0.78f); lineTo(sgn * hw * 0.76f, -hh * 1.6f); lineTo(sgn * hw * 0.88f, -hh * 0.4f); close() }, 4)
                }
            }
        }
    }

    private fun head(p: Pen, b: Breed, mood: Int) {
        ears(p, b)
        val shape = headPath(b)
        p.path(shape, 0)
        if (!p.detailed) return
        headMarks(p, b, shape)
        faceDetail(p, b, mood)
    }

    private fun headMarks(p: Pen, b: Breed, shape: Path) {
        val c = p.c
        val lw = p.lw
        val hw = b.hw
        val hh = b.hh
        val stripe = line(b.dark, lw * 2.1f)
        c.save()
        c.clipPath(shape)
        when (b.mark) {
            Mark.TABBY, Mark.MAINE -> {
                for (dx in floatArrayOf(-0.16f, 0f, 0.16f)) {
                    val len = if (dx == 0f) 0.3f else 0.22f
                    c.drawLine(dx * hw * 1.6f, -hh * 1.0f, dx * hw * 1.2f, -hh * (1.0f - len), stripe)
                }
                for (sgn in floatArrayOf(-1f, 1f)) for (k in 0..1) {
                    c.drawLine(sgn * hw * 1.02f, hh * (0.0f + k * 0.26f), sgn * hw * 0.72f, hh * (0.06f + k * 0.26f), stripe)
                }
            }
            Mark.BENGAL -> {
                for (dx in floatArrayOf(-0.16f, 0f, 0.16f)) c.drawLine(dx * hw * 1.6f, -hh * 1.0f, dx * hw * 1.2f, -hh * 0.68f, stripe)
                for ((sx, sy) in listOf(-0.6f to 0.05f, 0.6f to 0.05f, -0.52f to 0.52f, 0.52f to 0.52f, -0.3f to -0.5f, 0.3f to -0.5f)) {
                    c.drawOval(RectF(sx * hw - hw * 0.12f, sy * hh - hh * 0.1f, sx * hw + hw * 0.12f, sy * hh + hh * 0.1f), paint(b.dark))
                    c.drawOval(RectF(sx * hw - hw * 0.05f, sy * hh - hh * 0.04f, sx * hw + hw * 0.05f, sy * hh + hh * 0.04f), paint(b.alt))
                }
            }
            Mark.SIAMESE -> c.drawOval(RectF(-hw * 0.62f, -hh * 0.2f, hw * 0.62f, hh * 1.1f), paint(b.dark, 230))
            Mark.CALICO -> {
                c.drawOval(RectF(-hw * 1.15f, -hh * 1.25f, -hw * 0.1f, -hh * 0.1f), paint(b.alt))
                c.drawOval(RectF(hw * 0.2f, -hh * 1.2f, hw * 1.1f, -hh * 0.2f), paint(b.dark))
            }
            Mark.TUXEDO -> {
                c.drawOval(RectF(-hw * 0.66f, hh * 0.05f, hw * 0.66f, hh * 1.15f), paint(b.alt))
                c.drawPath(Path().apply { moveTo(-hw * 0.1f, -hh * 1.05f); lineTo(hw * 0.1f, -hh * 1.05f); lineTo(hw * 0.2f, hh * 0.2f); lineTo(-hw * 0.2f, hh * 0.2f); close() }, paint(b.alt))
            }
            Mark.RAGDOLL -> {
                c.drawOval(RectF(-hw * 0.84f, -hh * 0.75f, hw * 0.84f, hh * 0.85f), paint(b.dark, 230))
                c.drawOval(RectF(-hw * 0.4f, hh * 0.05f, hw * 0.4f, hh * 0.92f), paint(b.base))
                c.drawPath(Path().apply { moveTo(0f, -hh * 0.78f); lineTo(hw * 0.28f, hh * 0.1f); lineTo(-hw * 0.28f, hh * 0.1f); close() }, paint(b.base))
            }
            Mark.SPHYNX -> {
                for (k in 0..2) c.drawArc(RectF(-hw * 0.5f, -hh * (0.92f - k * 0.2f), hw * 0.5f, -hh * (0.4f - k * 0.2f)), 205f, 130f, false, line(b.dark, lw * 1.4f, 200))
            }
            Mark.PLAIN -> Unit
        }
        c.restore()
    }

    private fun faceDetail(p: Pen, b: Breed, mood: Int) {
        val c = p.c
        val lw = p.lw
        val hw = b.hw
        val hh = b.hh
        val er = hw * 0.27f
        val ex = hw * 0.43f
        val ey = -hh * 0.02f
        val outline = line(INK, lw * 1.2f)
        for (sgn in floatArrayOf(-1f, 1f)) {
            val cx = sgn * ex
            val oval = RectF(cx - er, ey - er, cx + er, ey + er)
            when (mood) {
                4 -> c.drawArc(RectF(cx - er, ey - er * 0.55f, cx + er, ey + er * 0.55f), 200f, 140f, false, line(INK, lw * 2.2f))
                3 -> {
                    c.drawOval(RectF(cx - er * 1.12f, ey - er * 1.12f, cx + er * 1.12f, ey + er * 1.12f), paint(SCLERA))
                    c.drawOval(RectF(cx - er * 1.12f, ey - er * 1.12f, cx + er * 1.12f, ey + er * 1.12f), outline)
                    c.drawCircle(cx, ey, er * 0.17f, paint(INK))
                }
                1 -> {
                    c.drawOval(oval, paint(SCLERA)); c.drawOval(oval, outline)
                    c.drawCircle(cx - sgn * er * 0.4f, ey + er * 0.22f, er * 0.3f, paint(INK))
                }
                else -> {
                    c.drawOval(oval, paint(SCLERA)); c.drawOval(oval, outline)
                    c.drawCircle(cx, ey + er * 0.06f, er * 0.7f, paint(b.iris))
                    c.drawCircle(cx, ey + er * 0.06f, er * 0.4f, paint(INK))
                    c.drawCircle(cx - er * 0.22f, ey - er * 0.2f, er * 0.2f, paint(SCLERA))
                    if (mood == 2 || mood == 5) {
                        // A heavy lid: slanted and cross for grumpy, level and half shut for smug.
                        val lid = Path()
                        if (mood == 2) {
                            lid.moveTo(cx + sgn * er * 1.2f, ey - er * 1.3f); lid.lineTo(cx - sgn * er * 1.2f, ey - er * 1.3f)
                            lid.lineTo(cx - sgn * er * 1.2f, ey + er * 0.05f); lid.lineTo(cx + sgn * er * 1.2f, ey - er * 0.55f)
                        } else {
                            lid.moveTo(cx - er * 1.2f, ey - er * 1.3f); lid.lineTo(cx + er * 1.2f, ey - er * 1.3f)
                            lid.lineTo(cx + er * 1.2f, ey - er * 0.05f); lid.lineTo(cx - er * 1.2f, ey - er * 0.05f)
                        }
                        c.save()
                        c.clipPath(Path().apply { addOval(oval, Path.Direction.CW) })
                        c.drawPath(lid, paint(b.base))
                        c.drawPath(lid, outline)
                        c.restore()
                        c.drawOval(oval, outline)
                    }
                }
            }
        }
        // Nose, mouth, tongue, blush.
        val flat = b.puffy && b.name == "Persian"
        val ny = hh * (if (flat) 0.12f else 0.22f)
        val nose = Path().apply { moveTo(-hw * 0.1f, ny - hh * 0.07f); lineTo(hw * 0.1f, ny - hh * 0.07f); lineTo(0f, ny + hh * 0.06f); close() }
        c.drawPath(nose, paint(PINK))
        c.drawPath(nose, outline)
        val mouth = line(INK, lw * 1.2f)
        c.drawArc(RectF(-hw * 0.22f, ny, 0f, ny + hh * 0.22f), 0f, 180f, false, mouth)
        c.drawArc(RectF(0f, ny, hw * 0.22f, ny + hh * 0.22f), 0f, 180f, false, mouth)
        if (mood == 1 || mood == 5) {
            val tongue = RectF(hw * 0.02f, ny + hh * 0.16f, hw * 0.2f, ny + hh * 0.38f)
            c.drawOval(tongue, paint(PINK))
            c.drawOval(tongue, outline)
        }
        for (sgn in floatArrayOf(-1f, 1f)) {
            c.drawOval(RectF(sgn * hw * 0.74f - hw * 0.17f, hh * 0.2f, sgn * hw * 0.74f + hw * 0.17f, hh * 0.4f), paint(PINK, 150))
            if (b.mark != Mark.SPHYNX) for (k in -1..1) {
                c.drawLine(sgn * hw * 0.66f, hh * 0.3f + k * hh * 0.06f, sgn * hw * 1.28f, hh * 0.24f + k * hh * 0.17f, line(CREAM, lw * 0.8f, 215))
            }
        }
    }

    // ---- body ----

    private fun bodyMarks(p: Pen, b: Breed, body: Path) {
        if (!p.detailed) return
        val c = p.c
        val lw = p.lw
        c.save()
        c.clipPath(body)
        val stripe = line(b.dark, lw * 2.2f)
        when (b.mark) {
            Mark.TABBY, Mark.MAINE -> for (i in 0..3) for (sgn in floatArrayOf(-1f, 1f)) {
                c.drawLine(sgn * 0.72f, 0.1f + i * 0.2f, sgn * 0.3f, 0.18f + i * 0.2f, stripe)
            }
            Mark.BENGAL -> for ((sx, sy) in listOf(-0.4f to 0.2f, 0.36f to 0.22f, -0.12f to 0.55f, 0.42f to 0.68f, -0.46f to 0.78f)) {
                c.drawOval(RectF(sx - 0.13f, sy - 0.1f, sx + 0.13f, sy + 0.1f), paint(b.dark))
                c.drawOval(RectF(sx - 0.06f, sy - 0.04f, sx + 0.06f, sy + 0.04f), paint(b.alt))
            }
            Mark.CALICO -> {
                c.drawOval(RectF(-0.8f, 0.0f, -0.05f, 0.5f), paint(b.alt))
                c.drawOval(RectF(0.1f, 0.4f, 0.9f, 1.0f), paint(b.dark))
            }
            Mark.TUXEDO, Mark.RAGDOLL -> c.drawOval(RectF(-0.34f, 0.05f, 0.34f, 1.0f), paint(if (b.mark == Mark.TUXEDO) b.alt else CREAM))
            Mark.SPHYNX -> for (k in 0..1) c.drawArc(RectF(-0.4f, 0.25f + k * 0.28f, 0.4f, 0.55f + k * 0.28f), 20f, 140f, false, line(b.dark, lw * 1.4f, 200))
            Mark.PLAIN -> c.drawOval(RectF(-0.3f, 0.2f, 0.3f, 0.95f), paint(b.alt, 120))
            Mark.SIAMESE -> Unit
        }
        c.restore()
    }

    fun breeds(): List<Breed> = BREEDS
}

/** Cat things: yarn, fish, mice, bowls, bells, paw trails, tins, cushions and hearts. Fills come from the palette. */
private fun Scene.catExtras(x: Float, y: Float, size: Float, kind: Int = i(0, 9)) {
    val pal = palette.colors
    val c = pal[i(0, pal.size)]
    val c2 = pal[i(0, pal.size)]
    val ink = INK
    val r = size
    canvas.save()
    canvas.translate(x, y)
    canvas.rotate(f(-24f, 24f))
    fun outline(p: Path, fillColor: Int, w: Float = 3f) { canvas.drawPath(p, fill(fillColor)); canvas.drawPath(p, stroke(ink, w)) }
    when (kind) {
        0 -> {   // yarn ball with a thread trailing off
            canvas.drawCircle(0f, 0f, r, fill(c)); canvas.drawCircle(0f, 0f, r, stroke(ink, 3f))
            for (k in -1..1) canvas.drawArc(RectF(-r * 0.9f, -r * 0.9f + k * r * 0.35f, r * 0.9f, r * 0.9f + k * r * 0.35f), 200f, 140f, false, stroke(lighten(c, 0.5f), 2.4f))
            canvas.drawArc(RectF(-r * 0.6f, -r * 0.95f, r * 1.1f, r * 0.7f), 300f, 120f, false, stroke(lighten(c, 0.5f), 2.4f))
            val thread = Path().apply { moveTo(r * 0.8f, r * 0.55f); cubicTo(r * 1.6f, r * 0.9f, r * 1.2f, r * 1.7f, r * 2.2f, r * 1.4f) }
            canvas.drawPath(thread, stroke(ink, 5.4f)); canvas.drawPath(thread, stroke(c, 2.6f))
        }
        1 -> {   // fish
            val body = Path().apply { moveTo(-r, 0f); cubicTo(-r * 0.4f, -r * 0.8f, r * 0.5f, -r * 0.6f, r * 0.9f, 0f); cubicTo(r * 0.5f, r * 0.6f, -r * 0.4f, r * 0.8f, -r, 0f); close() }
            val tail = Path().apply { moveTo(r * 0.8f, 0f); lineTo(r * 1.5f, -r * 0.55f); lineTo(r * 1.5f, r * 0.55f); close() }
            outline(tail, c2); outline(body, c)
            canvas.drawCircle(-r * 0.55f, -r * 0.1f, r * 0.11f, fill(ink)); canvas.drawCircle(-r * 0.58f, -r * 0.14f, r * 0.04f, fill(Color.WHITE))
            canvas.drawArc(RectF(-r * 0.2f, -r * 0.5f, r * 0.5f, r * 0.5f), 250f, 140f, false, stroke(ink, 2.2f))
        }
        2 -> {   // mouse
            val tail = Path().apply { moveTo(r * 0.9f, r * 0.15f); cubicTo(r * 1.7f, r * 0.4f, r * 1.3f, -r * 0.6f, r * 2f, -r * 0.5f) }
            canvas.drawPath(tail, stroke(ink, 5f)); canvas.drawPath(tail, stroke(Color.rgb(255, 170, 185), 2.4f))
            for (sx in intArrayOf(-1, 1)) { canvas.drawCircle(sx * r * 0.35f - r * 0.2f, -r * 0.55f, r * 0.34f, fill(Color.rgb(255, 170, 185))); canvas.drawCircle(sx * r * 0.35f - r * 0.2f, -r * 0.55f, r * 0.34f, stroke(ink, 3f)) }
            val body = Path().apply { moveTo(-r, r * 0.3f); cubicTo(-r * 0.9f, -r * 0.6f, r * 0.5f, -r * 0.7f, r, r * 0.3f); close() }
            outline(body, mix(c, Color.rgb(200, 200, 215), 0.6f))
            canvas.drawCircle(-r * 0.85f, r * 0.25f, r * 0.1f, fill(Color.rgb(255, 120, 150)))
            canvas.drawCircle(-r * 0.5f, -r * 0.05f, r * 0.08f, fill(ink))
        }
        3 -> {   // bowl of kibble
            repeat(5) { canvas.drawCircle(f(-r * 0.7f, r * 0.7f), -r * f(0.1f, 0.4f), r * 0.17f, fill(Color.rgb(214, 150, 90))) }
            val bowl = Path().apply { moveTo(-r, -r * 0.05f); lineTo(r, -r * 0.05f); cubicTo(r * 0.9f, r * 0.9f, -r * 0.9f, r * 0.9f, -r, -r * 0.05f); close() }
            outline(bowl, c)
            canvas.drawLine(-r * 0.5f, r * 0.35f, r * 0.5f, r * 0.35f, stroke(lighten(c, 0.5f), 2.4f))
            canvas.drawRect(-r * 0.12f, r * 0.0f, r * 0.12f, r * 0.2f, fill(lighten(c, 0.6f)))
        }
        4 -> {   // bell on a ribbon
            canvas.drawRect(-r * 1.1f, -r * 1.05f, r * 1.1f, -r * 0.8f, fill(c2)); canvas.drawRect(-r * 1.1f, -r * 1.05f, r * 1.1f, -r * 0.8f, stroke(ink, 3f))
            val bell = Path().apply { moveTo(-r * 0.75f, r * 0.55f); cubicTo(-r * 0.75f, -r * 0.9f, r * 0.75f, -r * 0.9f, r * 0.75f, r * 0.55f); close() }
            outline(bell, Color.rgb(255, 208, 90))
            canvas.drawLine(-r * 0.9f, r * 0.55f, r * 0.9f, r * 0.55f, stroke(ink, 3f))
            canvas.drawCircle(0f, r * 0.8f, r * 0.15f, fill(Color.rgb(255, 208, 90))); canvas.drawCircle(0f, r * 0.8f, r * 0.15f, stroke(ink, 2.4f))
        }
        5 -> {   // a trail of paw prints
            for (k in 0 until 4) {
                val px = k * r * 0.9f - r * 1.4f; val py = (if (k % 2 == 0) -r * 0.3f else r * 0.3f)
                canvas.drawOval(RectF(px - r * 0.22f, py - r * 0.12f, px + r * 0.22f, py + r * 0.26f), fill(c))
                for (t in -1..2) canvas.drawCircle(px + (t - 0.5f) * r * 0.2f, py - r * 0.26f - (if (t == 0 || t == 1) r * 0.07f else 0f), r * 0.08f, fill(c))
            }
        }
        6 -> {   // tin of tuna
            canvas.drawRoundRect(RectF(-r * 0.9f, -r * 0.5f, r * 0.9f, r * 0.6f), r * 0.2f, r * 0.2f, fill(Color.rgb(205, 210, 225)))
            canvas.drawRoundRect(RectF(-r * 0.9f, -r * 0.5f, r * 0.9f, r * 0.6f), r * 0.2f, r * 0.2f, stroke(ink, 3f))
            canvas.drawRect(-r * 0.9f, -r * 0.2f, r * 0.9f, r * 0.3f, fill(c))
            canvas.drawLine(-r * 0.9f, -r * 0.2f, r * 0.9f, -r * 0.2f, stroke(ink, 2.4f)); canvas.drawLine(-r * 0.9f, r * 0.3f, r * 0.9f, r * 0.3f, stroke(ink, 2.4f))
            canvas.drawOval(RectF(-r * 0.3f, -r * 0.12f, r * 0.3f, r * 0.22f), fill(lighten(c, 0.6f)))
            canvas.drawCircle(r * 0.7f, -r * 0.55f, r * 0.2f, stroke(ink, 3f))
        }
        7 -> {   // heart
            val heart = Path().apply { moveTo(0f, r * 0.9f); cubicTo(-r * 1.5f, -r * 0.1f, -r * 0.7f, -r * 1.1f, 0f, -r * 0.4f); cubicTo(r * 0.7f, -r * 1.1f, r * 1.5f, -r * 0.1f, 0f, r * 0.9f); close() }
            outline(heart, Color.rgb(255, 120, 150))
            canvas.drawArc(RectF(-r * 0.8f, -r * 0.6f, -r * 0.2f, -r * 0.1f), 200f, 80f, false, stroke(Color.WHITE, 2.4f, 200))
        }
        else -> {   // a cushion with a button
            canvas.drawRoundRect(RectF(-r, -r * 0.7f, r, r * 0.7f), r * 0.35f, r * 0.35f, fill(c))
            canvas.drawRoundRect(RectF(-r, -r * 0.7f, r, r * 0.7f), r * 0.35f, r * 0.35f, stroke(ink, 3f))
            for (a in 0 until 4) { val ang = a * 1.5708f + 0.785f; canvas.drawLine(0f, 0f, cos(ang) * r * 0.8f, sin(ang) * r * 0.5f, stroke(darken(c, 0.25f), 2f)) }
            canvas.drawCircle(0f, 0f, r * 0.16f, fill(c2)); canvas.drawCircle(0f, 0f, r * 0.16f, stroke(ink, 2.4f))
        }
    }
    canvas.restore()
}

/** Tiny paw prints, hearts and stars between the cats so the page is never bare at high Detail. */
private fun Scene.catTexture(n: Int) {
    repeat(count(n)) {
        val x = f(0f, w); val y = f(0f, h); val r = f(5f, 9f)
        when (i(0, 3)) {
            0 -> { canvas.drawOval(RectF(x - r, y - r * 0.6f, x + r, y + r), fill(Color.WHITE, 70)); for (t in -1..1) canvas.drawCircle(x + t * r * 0.8f, y - r * 1.2f, r * 0.34f, fill(Color.WHITE, 70)) }
            1 -> canvas.drawCircle(x, y, r * 0.3f, fill(Color.WHITE, 90))
            else -> { val p = stroke(Color.WHITE, 1.6f, 110); canvas.drawLine(x - r, y, x + r, y, p); canvas.drawLine(x, y - r, x, y + r, p) }
        }
    }
}

/** A club of cats: a different breed in every spot, each with a different mood. */
object Gatti : Style("gatti", "Cat Club", Category.DOODLE) {
    override val autoPalettes = listOf("candy", "sakura", "sunset", "ocean", "aurora", "graffiti")

    override fun draw(s: Scene) {
        val breeds = CatArt.breeds().shuffled(s.rnd)
        val cell = 215f / sqrt(s.density)
        s.catTexture(90)
        var n = 0
        for (p in s.spots(s.layout, 215f)) {
            if (s.chance(0.86f)) {
                // Cats scale with the cell, so more Detail means more, smaller cats rather than a pile-up.
                val hero = s.chance(0.16f)
                val size = cell * s.f(0.31f, 0.38f) * p.scale * (if (hero) 1.3f else 1f)
                val b = breeds[n++ % breeds.size]
                val pose = if (s.chance(0.6f)) 0 else 1
                CatArt.cat(s, p.x, p.y, size, p.rot + s.f(-6f, 6f), b, pose, s.i(0, 6), size > 52f && s.chance(0.8f))
                // Sometimes a smaller friend loafs beside it.
                if (!hero && s.chance(0.2f)) {
                    val friend = breeds[n++ % breeds.size]
                    CatArt.cat(s, p.x + size * 0.85f, p.y + size * 0.3f, size * 0.62f, p.rot + s.f(-10f, 10f), friend, 1, s.i(0, 6), false)
                }
            } else {
                s.catExtras(p.x, p.y, cell * s.f(0.16f, 0.22f))
            }
            // Cat things placed around it, never on top of it.
            repeat(if (s.density > 1.1f) s.i(2, 4) else s.i(1, 3)) {
                val a = s.f(0f, TAU); val d = cell * s.f(0.42f, 0.56f)
                s.catExtras(p.x + cos(a) * d, p.y + sin(a) * d * 0.85f, cell * s.f(0.07f, 0.115f))
            }
        }
    }
}

