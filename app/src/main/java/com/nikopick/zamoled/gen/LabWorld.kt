package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The Lab: science emoji. Every icon is drawn once in unit space and then put on the page as a
 * die-cut sticker, a gouache painting or a chalk drawing. Fills are indexed:
 * 0, 1, 2 are the palette's colours, 3 is cream, 4 is metal and 5 is near-black.
 */

private val CREAM = Color.rgb(255, 247, 238)
private val METAL = Color.rgb(176, 180, 200)
private val NIGHT = Color.rgb(40, 34, 56)
private val LAB_INK = Color.rgb(44, 30, 46)

private typealias LabIcon = (Pen) -> Unit

/** Text at emoji scale. It is drawn big and scaled down, since tiny text sizes collapse into blocks. */
private fun Pen.label(text: String, x: Float, y: Float, size: Float) {
    if (!detailed) return
    c.save()
    c.translate(x, y)
    c.scale(0.01f, 0.01f)
    c.drawText(text, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (look == PenLook.CHALK) CREAM else LAB_INK
        textSize = size * 100f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    })
    c.restore()
}

private fun union(vararg parts: Path): Path {
    val out = Path(parts[0])
    for (i in 1 until parts.size) out.op(parts[i], Path.Op.UNION)
    return out
}

private fun rrectPath(l: Float, t: Float, r: Float, b: Float, rad: Float): Path =
    Path().apply { addRoundRect(RectF(l, t, r, b), rad, rad, Path.Direction.CW) }

private fun ovalPath(l: Float, t: Float, r: Float, b: Float): Path =
    Path().apply { addOval(RectF(l, t, r, b), Path.Direction.CW) }

/** A sector of a ring between radii [r0] and [r1], starting at [start] degrees and sweeping [sweep]. */
private fun sector(r0: Float, r1: Float, start: Float, sweep: Float): Path = Path().apply {
    arcTo(RectF(-r1, -r1, r1, r1), start, sweep)
    arcTo(RectF(-r0, -r0, r0, r0), start + sweep, -sweep)
    close()
}

private object LabIcons {
    // ---------------------------------------------------------------- Physics

    private fun atom(p: Pen) {
        for (a in intArrayOf(0, 60, 120)) {
            p.c.save()
            p.c.rotate(a.toFloat())
            p.oval(-0.95f, -0.34f, 0.95f, 0.34f, -1)
            p.circle(0.95f, 0f, 0.1f, 2)
            p.c.restore()
        }
        p.circle(0f, 0f, 0.24f, 1)
        p.circle(-0.07f, -0.07f, 0.06f, 3)
    }

    private fun magnet(p: Pen) {
        val u = Path().apply {
            moveTo(-0.7f, -0.75f); lineTo(-0.7f, 0.05f)
            arcTo(RectF(-0.7f, -0.65f, 0.7f, 0.75f), 180f, -180f)
            lineTo(0.7f, -0.75f); lineTo(0.3f, -0.75f); lineTo(0.3f, 0.05f)
            arcTo(RectF(-0.3f, -0.25f, 0.3f, 0.35f), 0f, 180f)
            lineTo(-0.3f, -0.75f); close()
        }
        p.path(u, 0)
        p.rrect(-0.7f, -0.78f, -0.3f, -0.45f, 0.02f, 3)
        p.rrect(0.3f, -0.78f, 0.7f, -0.45f, 0.02f, 3)
        p.line(-0.52f, 0.0f, -0.5f, 0.3f)
    }

    private fun bulb(p: Pen) {
        p.circle(0f, -0.18f, 0.55f, 1)
        p.rrect(-0.27f, 0.3f, 0.27f, 0.72f, 0.07f, 4)
        p.line(-0.22f, 0.48f, 0.22f, 0.44f)
        p.line(-0.12f, 0.3f, -0.12f, -0.02f)
        p.line(0.12f, 0.3f, 0.12f, -0.02f)
        p.line(-0.12f, -0.02f, 0f, -0.14f)
        p.line(0.12f, -0.02f, 0f, -0.14f)
        for (a in intArrayOf(-150, -110, -70, -30)) {
            val r = a * PI.toFloat() / 180f
            p.line(cos(r) * 0.72f, -0.18f + sin(r) * 0.72f, cos(r) * 0.92f, -0.18f + sin(r) * 0.92f)
        }
        p.circle(-0.2f, -0.32f, 0.09f, 3)
    }

    private fun cradle(p: Pen) {
        p.rrect(-0.9f, -0.85f, 0.9f, -0.7f, 0.05f, 4)
        for (k in -2..2) {
            val x = k * 0.36f
            if (k == -2) {
                p.line(x, -0.7f, x - 0.35f, 0.3f)
                p.circle(x - 0.4f, 0.4f, 0.18f, 1)
            } else {
                p.line(x, -0.7f, x, 0.45f)
                p.circle(x, 0.55f, 0.18f, 4)
            }
        }
    }

    private fun apple(p: Pen) {
        p.oval(-0.65f, -0.35f, 0.65f, 0.8f, 0)
        p.line(0f, -0.35f, 0.08f, -0.68f)
        p.c.save(); p.c.rotate(-30f, 0.1f, -0.6f)
        p.oval(0.1f, -0.78f, 0.5f, -0.58f, 2)
        p.c.restore()
        p.circle(-0.3f, -0.05f, 0.1f, 3)
        p.line(-0.85f, -0.7f, -0.85f, -0.35f)
        p.line(0.85f, -0.75f, 0.85f, -0.45f)
    }

    private fun prism(p: Pen) {
        p.poly(-0.5f, 0.6f, 0f, -0.6f, 0.5f, 0.6f, fill = 3)
        p.poly(-0.95f, 0.02f, -0.22f, 0.0f, -0.22f, 0.1f, -0.95f, 0.08f, fill = 3)
        p.poly(0.12f, 0.06f, 0.95f, -0.3f, 0.95f, -0.12f, 0.2f, 0.18f, fill = 0)
        p.poly(0.2f, 0.18f, 0.95f, -0.12f, 0.95f, 0.06f, 0.26f, 0.28f, fill = 1)
        p.poly(0.26f, 0.28f, 0.95f, 0.06f, 0.95f, 0.24f, 0.3f, 0.38f, fill = 2)
    }

    private fun bolt(p: Pen) {
        p.poly(0.2f, -0.95f, -0.55f, 0.1f, -0.08f, 0.1f, -0.25f, 0.95f, 0.6f, -0.2f, 0.1f, -0.2f, fill = 1)
    }

    // ---------------------------------------------------------------- Maths

    private fun pi(p: Pen) {
        val shape = union(
            rrectPath(-0.78f, -0.55f, 0.78f, -0.3f, 0.1f),
            rrectPath(-0.5f, -0.4f, -0.26f, 0.72f, 0.08f),
            rrectPath(0.28f, -0.4f, 0.52f, 0.55f, 0.08f),
            ovalPath(0.28f, 0.4f, 0.74f, 0.74f),
        )
        p.path(shape, 1)
    }

    private fun sigma(p: Pen) {
        p.poly(0.65f, -0.82f, -0.62f, -0.82f, 0.08f, 0f, -0.62f, 0.82f, 0.65f, 0.82f, 0.65f, 0.56f, -0.2f, 0.56f, 0.34f, 0f, -0.2f, -0.56f, 0.65f, -0.56f, fill = 0)
    }

    private fun infinity(p: Pen) {
        val left = Path(ovalPath(-0.95f, -0.42f, 0.05f, 0.42f)).also { it.op(ovalPath(-0.7f, -0.2f, -0.2f, 0.2f), Path.Op.DIFFERENCE) }
        val right = Path(ovalPath(-0.05f, -0.42f, 0.95f, 0.42f)).also { it.op(ovalPath(0.2f, -0.2f, 0.7f, 0.2f), Path.Op.DIFFERENCE) }
        p.path(union(left, right), 2)
    }

    private fun triangle(p: Pen) {
        p.poly(-0.8f, 0.7f, 0.8f, 0.7f, -0.8f, -0.7f, fill = 1)
        p.poly(-0.8f, 0.45f, -0.55f, 0.45f, -0.55f, 0.7f, -0.8f, 0.7f, fill = 3)
        p.label("a", -0.95f, 0.0f, 0.26f)
        p.label("b", 0.0f, 0.95f, 0.26f)
        p.label("c", 0.2f, -0.12f, 0.26f)
    }

    private fun calculator(p: Pen) {
        p.rrect(-0.6f, -0.88f, 0.6f, 0.88f, 0.14f, 0)
        p.rrect(-0.44f, -0.72f, 0.44f, -0.3f, 0.06f, 3)
        p.label("42", 0.3f, -0.36f, 0.34f)
        for (r in 0..2) for (k in 0..2) p.circle(-0.28f + k * 0.28f, -0.04f + r * 0.28f, 0.1f, if (k == 2) 1 else 3)
    }

    private fun dice(p: Pen) {
        p.rrect(-0.7f, -0.7f, 0.7f, 0.7f, 0.2f, 3)
        for ((x, y) in listOf(-0.38f to -0.38f, 0.38f to -0.38f, 0f to 0f, -0.38f to 0.38f, 0.38f to 0.38f)) p.circle(x, y, 0.1f, 5)
    }

    private fun compass(p: Pen) {
        p.poly(-0.1f, -0.65f, 0.05f, -0.7f, 0.58f, 0.8f, 0.44f, 0.86f, fill = 4)
        p.poly(0.1f, -0.65f, -0.05f, -0.7f, -0.58f, 0.8f, -0.44f, 0.86f, fill = 4)
        p.circle(0f, -0.7f, 0.15f, 1)
        p.rrect(-0.06f, -1.0f, 0.06f, -0.82f, 0.03f, 2)
        p.line(-0.5f, 0.86f, -0.5f, 0.96f)
    }

    private fun graph(p: Pen) {
        p.rrect(-0.85f, -0.8f, 0.85f, 0.8f, 0.12f, 3)
        p.line(-0.65f, 0.55f, 0.65f, 0.55f)
        p.line(-0.5f, -0.65f, -0.5f, 0.65f)
        val curve = FloatArray(26)
        for (k in 0 until 13) {
            val t = k / 12f
            curve[k * 2] = -0.5f + t * 1.1f
            curve[k * 2 + 1] = 0.1f - sin(t * 6.28f) * 0.38f
        }
        p.poly(*curve, fill = -1, close = false)
        p.circle(0.1f, 0.1f, 0.09f, 1)
    }

    // ---------------------------------------------------------------- Chemistry

    private fun flask(p: Pen) {
        val glass = Path().apply {
            moveTo(-0.22f, -0.85f); lineTo(-0.22f, -0.3f); lineTo(-0.8f, 0.6f)
            quadTo(-0.85f, 0.82f, -0.6f, 0.82f); lineTo(0.6f, 0.82f)
            quadTo(0.85f, 0.82f, 0.8f, 0.6f); lineTo(0.22f, -0.3f); lineTo(0.22f, -0.85f); close()
        }
        p.path(glass, 3)
        val liquid = Path().apply {
            moveTo(-0.52f, 0.12f); lineTo(0.52f, 0.12f); lineTo(0.8f, 0.6f)
            quadTo(0.85f, 0.82f, 0.6f, 0.82f); lineTo(-0.6f, 0.82f)
            quadTo(-0.85f, 0.82f, -0.8f, 0.6f); close()
        }
        p.path(liquid, 1)
        p.rrect(-0.3f, -0.95f, 0.3f, -0.8f, 0.04f, 4)
        p.circle(0.12f, 0.35f, 0.09f, 3)
        p.circle(-0.18f, 0.5f, 0.06f, 3)
        p.circle(0.02f, -0.1f, 0.07f, 3)
    }

    private fun tubes(p: Pen) {
        p.c.save(); p.c.rotate(-16f)
        p.rrect(-0.55f, -0.8f, -0.15f, 0.72f, 0.2f, 3)
        p.rrect(-0.53f, 0.05f, -0.17f, 0.7f, 0.18f, 0)
        p.c.restore()
        p.c.save(); p.c.rotate(14f)
        p.rrect(0.15f, -0.8f, 0.55f, 0.72f, 0.2f, 3)
        p.rrect(0.17f, -0.2f, 0.53f, 0.7f, 0.18f, 1)
        p.c.restore()
        p.circle(0f, -0.2f, 0.07f, 3)
    }

    private fun beaker(p: Pen) {
        p.rrect(-0.55f, -0.75f, 0.55f, 0.82f, 0.1f, 3)
        p.rrect(-0.5f, -0.05f, 0.5f, 0.77f, 0.08f, 2)
        for (k in 0..3) p.line(0.3f, -0.55f + k * 0.2f, 0.55f, -0.55f + k * 0.2f)
        p.circle(-0.15f, 0.25f, 0.1f, 3)
        p.circle(0.15f, 0.05f, 0.07f, 3)
    }

    private fun molecule(p: Pen) {
        val pts = listOf(-0.62f to -0.5f, 0.64f to -0.4f, 0.1f to 0.68f, -0.7f to 0.4f)
        for ((x, y) in pts) p.line(0f, 0f, x, y)
        p.circle(0f, 0f, 0.3f, 1)
        p.circle(pts[0].first, pts[0].second, 0.2f, 0)
        p.circle(pts[1].first, pts[1].second, 0.2f, 2)
        p.circle(pts[2].first, pts[2].second, 0.2f, 0)
        p.circle(pts[3].first, pts[3].second, 0.16f, 3)
    }

    private fun burner(p: Pen) {
        p.rrect(-0.45f, 0.62f, 0.45f, 0.84f, 0.05f, 4)
        p.rrect(-0.13f, -0.1f, 0.13f, 0.64f, 0.03f, 4)
        val flame = Path().apply { moveTo(0f, -0.9f); cubicTo(0.5f, -0.45f, 0.4f, -0.1f, 0f, -0.1f); cubicTo(-0.4f, -0.1f, -0.5f, -0.45f, 0f, -0.9f); close() }
        p.path(flame, 1)
        val inner = Path().apply { moveTo(0f, -0.5f); cubicTo(0.22f, -0.3f, 0.16f, -0.14f, 0f, -0.14f); cubicTo(-0.16f, -0.14f, -0.22f, -0.3f, 0f, -0.5f); close() }
        p.path(inner, 3)
    }

    private fun element(p: Pen) {
        p.rrect(-0.75f, -0.75f, 0.75f, 0.75f, 0.12f, 1)
        p.label("79", -0.48f, -0.46f, 0.2f)
        p.label("Au", 0.0f, 0.25f, 0.62f)
        p.label("197", 0.0f, 0.62f, 0.16f)
    }

    // ---------------------------------------------------------------- Biology

    private fun dna(p: Pen) {
        val a = FloatArray(34)
        val b = FloatArray(34)
        for (k in 0 until 17) {
            val t = k / 16f
            val y = -0.9f + t * 1.8f
            val x = sin(t * 9.4f) * 0.38f
            a[k * 2] = x; a[k * 2 + 1] = y
            b[k * 2] = -x; b[k * 2 + 1] = y
        }
        for (k in 1 until 16 step 2) p.line(a[k * 2], a[k * 2 + 1], b[k * 2], b[k * 2 + 1])
        p.poly(*a, fill = -1, close = false)
        p.poly(*b, fill = -1, close = false)
        for (k in 0 until 17 step 2) { p.circle(a[k * 2], a[k * 2 + 1], 0.1f, 0); p.circle(b[k * 2], b[k * 2 + 1], 0.1f, 1) }
    }

    private fun microscope(p: Pen) {
        p.rrect(-0.6f, 0.68f, 0.6f, 0.86f, 0.06f, 4)
        p.rrect(0.22f, -0.1f, 0.44f, 0.72f, 0.05f, 4)
        p.c.save(); p.c.rotate(-24f)
        p.rrect(-0.2f, -0.82f, 0.2f, 0.12f, 0.05f, 0)
        p.rrect(-0.12f, -0.98f, 0.12f, -0.8f, 0.03f, 4)
        p.rrect(-0.15f, 0.1f, 0.15f, 0.3f, 0.03f, 2)
        p.c.restore()
        p.rrect(-0.55f, 0.45f, 0.1f, 0.55f, 0.03f, 4)
        p.circle(0.33f, 0.1f, 0.14f, 1)
    }

    private fun cell(p: Pen) {
        p.oval(-0.92f, -0.7f, 0.92f, 0.7f, 3)
        p.circle(0.12f, -0.02f, 0.34f, 1)
        p.circle(0.18f, 0.02f, 0.12f, 2)
        p.oval(-0.7f, -0.35f, -0.3f, -0.12f, 0)
        p.oval(-0.6f, 0.2f, -0.2f, 0.42f, 0)
        p.oval(0.42f, 0.28f, 0.78f, 0.48f, 2)
        p.circle(-0.1f, 0.5f, 0.06f, 2)
        p.circle(0.5f, -0.4f, 0.07f, 2)
    }

    private fun petri(p: Pen) {
        p.oval(-0.92f, -0.55f, 0.92f, 0.75f, 3)
        p.oval(-0.78f, -0.4f, 0.78f, 0.6f, -1)
        p.circle(-0.35f, -0.05f, 0.18f, 0)
        p.circle(0.15f, -0.2f, 0.13f, 1)
        p.circle(0.4f, 0.2f, 0.2f, 2)
        p.circle(-0.1f, 0.3f, 0.1f, 1)
        p.circle(0.62f, -0.12f, 0.07f, 0)
    }

    private fun seedling(p: Pen) {
        p.poly(-0.5f, 0.3f, 0.5f, 0.3f, 0.36f, 0.88f, -0.36f, 0.88f, fill = 0)
        p.rrect(-0.58f, 0.2f, 0.58f, 0.36f, 0.05f, 0)
        p.line(0f, 0.2f, 0f, -0.45f)
        p.c.save(); p.c.rotate(-35f, 0f, -0.2f)
        p.oval(-0.78f, -0.55f, -0.04f, -0.2f, 2)
        p.c.restore()
        p.c.save(); p.c.rotate(35f, 0f, -0.35f)
        p.oval(0.04f, -0.8f, 0.8f, -0.42f, 1)
        p.c.restore()
    }

    private fun ladybug(p: Pen) {
        for (sgn in floatArrayOf(-1f, 1f)) for (k in 0..2) p.line(sgn * 0.55f, -0.1f + k * 0.3f, sgn * 0.92f, -0.25f + k * 0.4f)
        p.circle(0f, -0.62f, 0.3f, 5)
        p.circle(0f, 0.12f, 0.66f, 0)
        p.line(0f, -0.54f, 0f, 0.78f)
        for ((x, y) in listOf(-0.3f to -0.05f, 0.3f to -0.05f, -0.34f to 0.4f, 0.34f to 0.4f)) p.darkDot(x, y, 0.1f)
        p.circle(-0.1f, -0.68f, 0.06f, 3)
        p.circle(0.1f, -0.68f, 0.06f, 3)
    }

    // ---------------------------------------------------------------- Psychology

    private fun brain(p: Pen) {
        val shape = union(ovalPath(-0.9f, -0.58f, 0.05f, 0.5f), ovalPath(-0.05f, -0.58f, 0.9f, 0.5f), rrectPath(-0.14f, 0.3f, 0.14f, 0.82f, 0.1f))
        p.path(shape, 1)
        p.line(0f, -0.55f, 0f, 0.3f)
        p.arc(-0.7f, -0.35f, -0.2f, 0.05f, 180f, 200f)
        p.arc(0.2f, -0.35f, 0.7f, 0.05f, 160f, 200f)
        p.arc(-0.6f, 0.0f, -0.15f, 0.4f, 0f, 180f)
        p.arc(0.15f, 0.0f, 0.6f, 0.4f, 0f, 180f)
    }

    private fun psi(p: Pen) {
        val u = Path(ovalPath(-0.58f, -0.7f, 0.58f, 0.6f)).also { it.op(ovalPath(-0.4f, -0.7f, 0.4f, 0.42f), Path.Op.DIFFERENCE) }
        u.op(rrectPath(-0.7f, 0.0f, 0.7f, 0.8f, 0.0f), Path.Op.INTERSECT)
        val shape = union(
            rrectPath(-0.09f, -0.85f, 0.09f, 0.9f, 0.06f),
            u,
            rrectPath(-0.58f, -0.78f, -0.4f, 0.0f, 0.06f),
            rrectPath(0.4f, -0.78f, 0.58f, 0.0f, 0.06f),
        )
        p.path(shape, 2)
    }

    private fun inkblot(p: Pen) {
        p.rrect(-0.75f, -0.9f, 0.75f, 0.9f, 0.08f, 3)
        val half = floatArrayOf(0f, -0.62f, 0.3f, -0.66f, 0.48f, -0.4f, 0.36f, -0.12f, 0.58f, 0.1f, 0.42f, 0.36f, 0.2f, 0.62f, 0f, 0.5f)
        val pts = FloatArray(half.size * 2 - 4)
        var n = 0
        for (i in half.indices step 2) { pts[n++] = half[i]; pts[n++] = half[i + 1] }
        for (i in half.size - 4 downTo 2 step 2) { pts[n++] = -half[i]; pts[n++] = half[i + 1] }
        p.path(smoothPath(pts, true, 0.5f), 5)
        p.circle(0.2f, -0.2f, 0.07f, 3)
        p.circle(-0.2f, -0.2f, 0.07f, 3)
    }

    private fun thought(p: Pen) {
        val cloud = union(ovalPath(-0.85f, -0.35f, -0.1f, 0.3f), ovalPath(-0.45f, -0.75f, 0.35f, 0.1f), ovalPath(0.0f, -0.5f, 0.85f, 0.25f), ovalPath(-0.6f, -0.15f, 0.7f, 0.4f))
        p.path(cloud, 3)
        p.circle(-0.35f, 0.62f, 0.12f, 3)
        p.circle(-0.6f, 0.82f, 0.07f, 3)
        p.label("?", 0.0f, 0.1f, 0.8f)
    }

    private fun puzzle(p: Pen) {
        val shape = union(
            rrectPath(-0.6f, -0.5f, 0.6f, 0.7f, 0.06f),
            ovalPath(-0.2f, -0.85f, 0.2f, -0.4f),
            ovalPath(0.5f, -0.2f, 0.95f, 0.2f),
        )
        shape.op(ovalPath(-0.95f, 0.0f, -0.5f, 0.4f), Path.Op.DIFFERENCE)
        p.path(shape, 0)
    }

    // ---------------------------------------------------------------- Philosophy

    private fun column(p: Pen) {
        p.rrect(-0.55f, -0.82f, 0.55f, -0.6f, 0.04f, 3)
        p.circle(-0.5f, -0.5f, 0.12f, 3)
        p.circle(0.5f, -0.5f, 0.12f, 3)
        p.rrect(-0.38f, -0.6f, 0.38f, 0.6f, 0.03f, 3)
        for (x in floatArrayOf(-0.2f, 0f, 0.2f)) p.line(x, -0.52f, x, 0.52f)
        p.rrect(-0.62f, 0.6f, 0.62f, 0.84f, 0.04f, 3)
    }

    private fun owl(p: Pen) {
        p.poly(-0.52f, -0.62f, -0.28f, -0.88f, -0.2f, -0.55f, fill = 0)
        p.poly(0.52f, -0.62f, 0.28f, -0.88f, 0.2f, -0.55f, fill = 0)
        p.oval(-0.62f, -0.7f, 0.62f, 0.8f, 0)
        p.oval(-0.38f, 0.1f, 0.38f, 0.72f, 3)
        p.circle(-0.27f, -0.25f, 0.25f, 3)
        p.circle(0.27f, -0.25f, 0.25f, 3)
        p.darkDot(-0.25f, -0.23f, 0.1f)
        p.darkDot(0.25f, -0.23f, 0.1f)
        p.poly(-0.08f, -0.08f, 0.08f, -0.08f, 0f, 0.1f, fill = 1)
        p.oval(-0.78f, -0.1f, -0.5f, 0.55f, 2)
        p.oval(0.5f, -0.1f, 0.78f, 0.55f, 2)
    }

    private fun yinYang(p: Pen) {
        p.circle(0f, 0f, 0.85f, 3)
        val dark = Path().apply {
            moveTo(0f, -0.85f)
            arcTo(RectF(-0.85f, -0.85f, 0.85f, 0.85f), 270f, 180f)
            arcTo(RectF(-0.425f, 0f, 0.425f, 0.85f), 90f, 180f)
            arcTo(RectF(-0.425f, -0.85f, 0.425f, 0f), 90f, -180f)
            close()
        }
        p.path(dark, 5)
        p.circle(0f, -0.425f, 0.12f, 5)
        p.circle(0f, 0.425f, 0.12f, 3)
    }

    private fun scroll(p: Pen) {
        p.rrect(-0.6f, -0.55f, 0.6f, 0.55f, 0.06f, 3)
        p.oval(-0.78f, -0.7f, -0.5f, 0.7f, 4)
        p.oval(0.5f, -0.7f, 0.78f, 0.7f, 4)
        for (k in 0..3) p.line(-0.38f, -0.32f + k * 0.2f, 0.38f, -0.32f + k * 0.2f)
        p.label("?", 0.0f, 0.1f, 0.5f)
    }

    private fun scales(p: Pen) {
        p.line(0f, -0.8f, 0f, 0.7f)
        p.rrect(-0.35f, 0.7f, 0.35f, 0.86f, 0.05f, 4)
        p.line(-0.75f, -0.5f, 0.75f, -0.5f)
        p.circle(0f, -0.82f, 0.1f, 1)
        for (sgn in floatArrayOf(-1f, 1f)) {
            val x = sgn * 0.75f
            p.line(x, -0.5f, x - 0.25f, 0.1f)
            p.line(x, -0.5f, x + 0.25f, 0.1f)
            p.path(Path().apply { moveTo(x - 0.3f, 0.1f); lineTo(x + 0.3f, 0.1f); quadTo(x + 0.28f, 0.42f, x, 0.42f); quadTo(x - 0.28f, 0.42f, x - 0.3f, 0.1f); close() }, if (sgn < 0f) 0 else 2)
        }
    }

    // ---------------------------------------------------------------- History

    private fun pyramid(p: Pen) {
        p.circle(0.62f, -0.55f, 0.22f, 1)
        p.poly(-0.9f, 0.7f, 0f, -0.75f, 0.9f, 0.7f, fill = 0)
        p.poly(0f, -0.75f, 0.9f, 0.7f, 0.12f, 0.7f, fill = 1)
        p.line(-0.45f, -0.02f, 0.45f, -0.02f)
        p.line(-0.7f, 0.34f, 0.7f, 0.34f)
        p.line(-1.0f, 0.78f, 1.0f, 0.78f)
    }

    private fun hourglass(p: Pen) {
        p.rrect(-0.6f, -0.9f, 0.6f, -0.74f, 0.05f, 4)
        p.rrect(-0.6f, 0.74f, 0.6f, 0.9f, 0.05f, 4)
        p.poly(-0.5f, -0.74f, 0.5f, -0.74f, 0.06f, 0f, 0.5f, 0.74f, -0.5f, 0.74f, -0.06f, 0f, fill = 3)
        p.poly(-0.34f, -0.74f, 0.34f, -0.74f, 0.04f, -0.2f, fill = 1)
        p.poly(-0.42f, 0.74f, 0.42f, 0.74f, 0.0f, 0.3f, fill = 1)
    }

    private fun globe(p: Pen) {
        p.circle(0f, -0.08f, 0.62f, 1)
        p.arc(-0.62f, -0.7f, 0.62f, 0.54f, 20f, 140f)
        p.oval(-0.3f, -0.7f, 0.3f, 0.54f, -1)
        p.line(-0.62f, -0.08f, 0.62f, -0.08f)
        p.arc(-0.75f, -0.9f, 0.75f, 0.9f, 60f, 60f)
        p.rrect(-0.35f, 0.7f, 0.35f, 0.86f, 0.05f, 4)
        p.line(0f, 0.54f, 0f, 0.7f)
        p.oval(-0.35f, -0.3f, -0.05f, 0.05f, 2)
    }

    private fun ship(p: Pen) {
        p.line(0f, -0.85f, 0f, 0.3f)
        p.poly(0.04f, -0.8f, 0.6f, 0.1f, 0.04f, 0.1f, fill = 3)
        p.poly(-0.04f, -0.55f, -0.5f, 0.1f, -0.04f, 0.1f, fill = 3)
        p.poly(-0.85f, 0.3f, 0.85f, 0.3f, 0.6f, 0.68f, -0.6f, 0.68f, fill = 0)
        p.poly(0f, -0.85f, 0.3f, -0.76f, 0f, -0.68f, fill = 1)
        p.arc(-0.9f, 0.62f, -0.4f, 0.9f, 180f, 180f)
        p.arc(-0.4f, 0.62f, 0.1f, 0.9f, 180f, 180f)
        p.arc(0.1f, 0.62f, 0.6f, 0.9f, 180f, 180f)
        p.arc(0.6f, 0.62f, 1.1f, 0.9f, 180f, 180f)
    }

    private fun amphora(p: Pen) {
        val half = floatArrayOf(0.2f, -0.88f, 0.24f, -0.6f, 0.52f, -0.2f, 0.62f, 0.2f, 0.4f, 0.62f, 0.12f, 0.88f)
        val pts = FloatArray(half.size * 2)
        var n = 0
        for (i in half.indices step 2) { pts[n++] = half[i]; pts[n++] = half[i + 1] }
        for (i in half.size - 2 downTo 0 step 2) { pts[n++] = -half[i]; pts[n++] = half[i + 1] }
        p.path(smoothPath(pts, true, 0.5f), 0)
        p.oval(-0.78f, -0.3f, -0.46f, 0.15f, -1)
        p.oval(0.46f, -0.3f, 0.78f, 0.15f, -1)
        p.rrect(-0.5f, 0.0f, 0.5f, 0.2f, 0.0f, 1)
        p.rrect(-0.28f, -0.96f, 0.28f, -0.84f, 0.04f, 1)
    }

    private fun castle(p: Pen) {
        p.rrect(-0.7f, -0.2f, 0.7f, 0.82f, 0.04f, 4)
        for (k in 0..3) p.rrect(-0.7f + k * 0.4f, -0.4f, -0.5f + k * 0.4f, -0.2f, 0.0f, 4)
        p.rrect(-0.28f, -0.82f, 0.28f, -0.2f, 0.03f, 4)
        for (k in 0..2) p.rrect(-0.28f + k * 0.22f, -0.98f, -0.14f + k * 0.22f, -0.82f, 0.0f, 4)
        p.path(Path().apply { moveTo(-0.2f, 0.82f); lineTo(-0.2f, 0.45f); quadTo(0f, 0.25f, 0.2f, 0.45f); lineTo(0.2f, 0.82f); close() }, 5)
        p.rrect(-0.04f, -0.6f, 0.04f, -0.4f, 0.0f, 5)
        p.line(0f, -0.98f, 0f, -1.12f)
    }

    private fun quill(p: Pen) {
        p.oval(-0.7f, 0.3f, 0.2f, 0.85f, 5)
        p.rrect(-0.4f, 0.2f, -0.1f, 0.4f, 0.03f, 4)
        p.c.save(); p.c.rotate(35f, -0.25f, 0.3f)
        p.path(Path().apply { moveTo(-0.25f, 0.3f); cubicTo(-0.8f, -0.3f, -0.2f, -0.95f, 0.1f, -0.98f); cubicTo(0.3f, -0.5f, 0.1f, -0.05f, -0.25f, 0.3f); close() }, 3)
        p.line(-0.25f, 0.3f, 0.02f, -0.8f)
        p.c.restore()
    }

    // ---------------------------------------------------------------- Space

    private fun ringedPlanet(p: Pen) {
        p.circle(0f, 0f, 0.55f, 0)
        p.arc(-0.35f, -0.35f, 0.15f, 0.15f, 180f, 90f)
        p.c.save(); p.c.rotate(-22f)
        p.oval(-0.98f, -0.26f, 0.98f, 0.26f, -1)
        p.oval(-0.85f, -0.2f, 0.85f, 0.2f, -1)
        p.c.restore()
        p.circle(-0.18f, -0.2f, 0.07f, 3)
    }

    private fun rocket(p: Pen) {
        p.poly(-0.24f, 0.2f, -0.62f, 0.62f, -0.2f, 0.58f, fill = 1)
        p.poly(0.24f, 0.2f, 0.62f, 0.62f, 0.2f, 0.58f, fill = 1)
        val body = Path().apply { moveTo(0f, -0.95f); cubicTo(0.5f, -0.5f, 0.38f, 0.2f, 0.26f, 0.6f); lineTo(-0.26f, 0.6f); cubicTo(-0.38f, 0.2f, -0.5f, -0.5f, 0f, -0.95f); close() }
        p.path(body, 3)
        p.circle(0f, -0.2f, 0.16f, 4)
        p.poly(-0.2f, 0.6f, 0.2f, 0.6f, 0f, 0.95f, fill = 0)
        p.rrect(-0.3f, 0.32f, 0.3f, 0.46f, 0.0f, 1)
    }

    private fun telescope(p: Pen) {
        p.line(0f, 0.15f, -0.4f, 0.9f)
        p.line(0f, 0.15f, 0.4f, 0.9f)
        p.line(0f, 0.15f, 0f, 0.9f)
        p.c.save(); p.c.rotate(-28f, 0f, 0f)
        p.rrect(-0.85f, -0.2f, 0.45f, 0.2f, 0.04f, 0)
        p.rrect(0.35f, -0.3f, 0.85f, 0.3f, 0.05f, 4)
        p.rrect(-0.95f, -0.12f, -0.8f, 0.12f, 0.03f, 2)
        p.c.restore()
    }

    private fun satellite(p: Pen) {
        p.rrect(-0.28f, -0.28f, 0.28f, 0.28f, 0.06f, 4)
        for (sgn in floatArrayOf(-1f, 1f)) {
            p.rrect(if (sgn < 0f) -0.98f else 0.42f, -0.34f, if (sgn < 0f) -0.42f else 0.98f, 0.34f, 0.03f, 1)
            p.line(sgn * 0.28f, 0f, sgn * 0.42f, 0f)
            for (k in 1..2) p.line(sgn * (0.42f + k * 0.186f), -0.34f, sgn * (0.42f + k * 0.186f), 0.34f)
        }
        p.line(0f, -0.28f, 0f, -0.6f)
        p.circle(0f, -0.66f, 0.1f, 2)
        p.arc(-0.2f, 0.28f, 0.2f, 0.7f, 0f, 180f)
    }

    private fun helmet(p: Pen) {
        p.circle(0f, 0f, 0.82f, 3)
        p.oval(-0.58f, -0.42f, 0.58f, 0.42f, 5)
        p.oval(-0.4f, -0.3f, -0.1f, -0.1f, 4)
        p.rrect(-0.3f, 0.72f, 0.3f, 0.92f, 0.06f, 4)
        p.circle(-0.78f, 0.05f, 0.1f, 1)
        p.circle(0.78f, 0.05f, 0.1f, 1)
        p.line(0.5f, -0.7f, 0.66f, -0.95f)
        p.circle(0.68f, -0.98f, 0.07f, 1)
    }

    private fun comet(p: Pen) {
        p.poly(-0.1f, -0.15f, -0.95f, -0.75f, -0.5f, -0.35f, -0.9f, -0.1f, -0.3f, 0.1f, -0.7f, 0.5f, 0.0f, 0.2f, fill = 1)
        p.poly(-0.15f, -0.05f, -0.7f, -0.5f, -0.4f, -0.1f, -0.6f, 0.1f, -0.15f, 0.1f, fill = 3)
        p.circle(0.3f, 0.2f, 0.45f, 4)
        p.circle(0.18f, 0.1f, 0.1f, 3)
        p.circle(0.45f, 0.38f, 0.08f, 3)
    }

    // ---------------------------------------------------------------- Spirals and radiation

    private fun radiation(p: Pen) {
        p.circle(0f, 0f, 0.9f, 1)
        for (a in floatArrayOf(-120f, 0f, 120f)) {
            p.path(sector(0.28f, 0.74f, a - 30f, 60f), 5)
        }
        p.circle(0f, 0f, 0.15f, 5)
    }

    private fun biohazard(p: Pen) {
        p.circle(0f, 0f, 0.9f, 1)
        val ring = floatArrayOf(0f, -0.44f, 0.38f, 0.22f, -0.38f, 0.22f)
        for (k in 0..2) {
            p.circle(ring[k * 2], ring[k * 2 + 1], 0.34f, 5)
            p.circle(ring[k * 2], ring[k * 2 + 1] + (if (k == 0) -0.08f else 0.04f), 0.2f, 1)
        }
        p.circle(0f, 0f, 0.14f, 5)
        p.circle(0f, 0f, 0.07f, 1)
    }

    private fun galaxy(p: Pen) {
        for (arm in 0..1) {
            for (k in 0..14) {
                val t = k / 14f
                val a = arm * PI.toFloat() + t * 5.2f
                val r = 0.14f + 0.8f * t
                p.circle(cos(a) * r, sin(a) * r, 0.14f - 0.07f * t, if (k % 3 == 0) 3 else if (arm == 0) 0 else 1)
            }
        }
        p.circle(0f, 0f, 0.2f, 3)
    }

    private fun hypno(p: Pen) {
        p.circle(0f, 0f, 0.9f, 3)
        val pts = FloatArray(2 * 70)
        for (k in 0 until 70) {
            val t = k / 69f
            val a = t * 6.28f * 3.6f
            val r = 0.04f + 0.8f * t
            pts[k * 2] = cos(a) * r
            pts[k * 2 + 1] = sin(a) * r
        }
        p.poly(*pts, fill = -1, close = false)
        p.circle(0f, 0f, 0.07f, 2)
    }

    private fun snail(p: Pen) {
        p.oval(-0.95f, 0.3f, 0.75f, 0.8f, 2)
        p.line(0.55f, 0.35f, 0.72f, 0.05f)
        p.line(0.7f, 0.4f, 0.92f, 0.12f)
        p.circle(0.72f, 0.03f, 0.07f, 3)
        p.circle(0.93f, 0.1f, 0.07f, 3)
        p.circle(-0.05f, -0.1f, 0.62f, 1)
        val pts = FloatArray(2 * 40)
        for (k in 0 until 40) {
            val t = k / 39f
            val a = t * 6.28f * 2.2f
            val r = 0.04f + 0.5f * t
            pts[k * 2] = -0.05f + cos(a) * r
            pts[k * 2 + 1] = -0.1f + sin(a) * r
        }
        p.poly(*pts, fill = -1, close = false)
    }

    private fun warning(p: Pen) {
        p.path(Path().apply { moveTo(0f, -0.85f); lineTo(0.9f, 0.7f); lineTo(-0.9f, 0.7f); close() }, 1)
        p.rrect(-0.07f, -0.35f, 0.07f, 0.28f, 0.04f, 5)
        p.circle(0f, 0.48f, 0.08f, 5)
    }

    val physics: List<LabIcon> = listOf(::atom, ::magnet, ::bulb, ::cradle, ::apple, ::prism, ::bolt)
    val maths: List<LabIcon> = listOf(::pi, ::sigma, ::infinity, ::triangle, ::calculator, ::dice, ::compass, ::graph)
    val chemistry: List<LabIcon> = listOf(::flask, ::tubes, ::beaker, ::molecule, ::burner, ::element)
    val biology: List<LabIcon> = listOf(::dna, ::microscope, ::cell, ::petri, ::seedling, ::ladybug)
    val psychology: List<LabIcon> = listOf(::brain, ::psi, ::inkblot, ::thought, ::puzzle, ::hypno)
    val philosophy: List<LabIcon> = listOf(::column, ::owl, ::yinYang, ::scroll, ::scales)
    val history: List<LabIcon> = listOf(::pyramid, ::hourglass, ::globe, ::ship, ::amphora, ::castle, ::quill)
    val space: List<LabIcon> = listOf(::ringedPlanet, ::rocket, ::telescope, ::satellite, ::helmet, ::comet)
    val hazard: List<LabIcon> = listOf(::radiation, ::biohazard, ::galaxy, ::hypno, ::snail, ::warning)
}

/** One topic's icons, scattered, stuck, painted or chalked depending on the seed. */
private class LabStyle(id: String, name: String, private val icons: List<LabIcon>) : Style(id, name, Category.LAB) {
    override val autoPalettes = listOf("candy", "graffiti", "aurora", "ocean", "sunset", "hue", "sakura")

    override fun draw(s: Scene) {
        val roll = s.i(0, 100)
        val kind = if (roll < 52) 0 else if (roll < 78) 1 else 2
        val colors = s.palette.colors
        val order = icons.shuffled(s.rnd)
        var n = 0
        val cell = 160f / sqrt(s.density)
        for (p in s.spots(s.layout, 160f)) {
            val icon = order[n++ % order.size]
            val shift = s.i(0, colors.size)
            val fills = intArrayOf(colors[shift % colors.size], colors[(shift + 1) % colors.size], colors[(shift + 2) % colors.size], CREAM, METAL, NIGHT)
            val size = s.f(60f, 92f) * p.scale
            s.unit(p.x, p.y, size, p.rot + s.f(-10f, 10f)) {
                when (kind) {
                    0 -> {
                        val w = 3.4f / size
                        for (look in arrayOf(PenLook.SHADOW, PenLook.DIECUT, PenLook.COMIC)) icon(Pen(s, LAB_INK, w, fills, look = look))
                    }
                    1 -> icon(Pen(s, LAB_INK, 3.6f / size, fills, shaded = true, look = PenLook.PAINTED))
                    else -> icon(Pen(s, Color.rgb(244, 242, 236), 3.2f / size, fills, look = PenLook.CHALK))
                }
            }
            // A speck of something between icons: a dot, a plus or a tiny ring.
            repeat(2) {
                val x = p.x + s.f(-cell / 2f, cell / 2f)
                val y = p.y + s.f(-cell / 2f, cell / 2f)
                val c = lighten(colors[s.i(0, colors.size)], 0.35f)
                when (s.i(0, 3)) {
                    0 -> s.dot(x, y, s.f(2f, 3.6f), c, 200)
                    1 -> { s.canvas.drawLine(x - 6f, y, x + 6f, y, s.stroke(c, 2f, 200)); s.canvas.drawLine(x, y - 6f, x, y + 6f, s.stroke(c, 2f, 200)) }
                    else -> s.canvas.drawCircle(x, y, s.f(4f, 7f), s.stroke(c, 1.8f, 200))
                }
            }
        }
    }
}

object LabWorld {
    val styles: List<Style> = listOf(
        LabStyle("lab_physics", "Physics", LabIcons.physics),
        LabStyle("lab_maths", "Maths", LabIcons.maths),
        LabStyle("lab_chemistry", "Chemistry", LabIcons.chemistry),
        LabStyle("lab_biology", "Biology", LabIcons.biology),
        LabStyle("lab_psychology", "Psychology", LabIcons.psychology),
        LabStyle("lab_philosophy", "Philosophy", LabIcons.philosophy),
        LabStyle("lab_history", "History", LabIcons.history),
        LabStyle("lab_space", "Space", LabIcons.space),
        LabStyle("lab_hazard", "Spirals & Radiation", LabIcons.hazard),
        LabStyle(
            "lab_mix", "Everything",
            LabIcons.physics + LabIcons.maths + LabIcons.chemistry + LabIcons.biology + LabIcons.psychology +
                LabIcons.philosophy + LabIcons.history + LabIcons.space + LabIcons.hazard,
        ),
    )
}
