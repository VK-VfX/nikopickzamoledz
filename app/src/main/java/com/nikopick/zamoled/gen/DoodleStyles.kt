package com.nikopick.zamoled.gen

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Draws doodles in a unit space (about -1..1). Outline mode gives line art in one ink colour;
 * filled mode gives flat cartoon stickers: pastel fills with an ink outline.
 */
class Pen(
    private val s: Scene,
    ink: Int,
    width: Float,
    fills: IntArray?,
    alpha: Int = 255,
    glow: Float = 0f,
) {
    val c: Canvas get() = s.canvas
    private val linePaint = s.stroke(ink, width, alpha, glow)
    private val inkPaint = s.fill(ink, alpha)
    private val darkPaint = s.fill(Color.BLACK)
    private val fillPaints: List<Paint>? = fills?.map { s.fill(it, alpha) }
    val filled: Boolean get() = fillPaints != null

    private fun fp(i: Int): Paint? {
        val f = fillPaints ?: return null
        return if (i < 0) null else f[i % f.size]
    }

    fun path(p: Path, fill: Int = -1) {
        fp(fill)?.let { c.drawPath(p, it) }
        c.drawPath(p, linePaint)
    }

    fun circle(x: Float, y: Float, r: Float, fill: Int = -1) {
        fp(fill)?.let { c.drawCircle(x, y, r, it) }
        c.drawCircle(x, y, r, linePaint)
    }

    fun oval(l: Float, t: Float, r: Float, b: Float, fill: Int = -1) {
        val rect = RectF(l, t, r, b)
        fp(fill)?.let { c.drawOval(rect, it) }
        c.drawOval(rect, linePaint)
    }

    fun rrect(l: Float, t: Float, r: Float, b: Float, rad: Float, fill: Int = -1) {
        val rect = RectF(l, t, r, b)
        fp(fill)?.let { c.drawRoundRect(rect, rad, rad, it) }
        c.drawRoundRect(rect, rad, rad, linePaint)
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = c.drawLine(x1, y1, x2, y2, linePaint)

    fun arc(l: Float, t: Float, r: Float, b: Float, start: Float, sweep: Float) =
        c.drawArc(RectF(l, t, r, b), start, sweep, false, linePaint)

    fun poly(vararg pts: Float, fill: Int = -1, close: Boolean = true) {
        val p = Path()
        p.moveTo(pts[0], pts[1])
        var i = 2
        while (i + 1 < pts.size) {
            p.lineTo(pts[i], pts[i + 1])
            i += 2
        }
        if (close) p.close()
        if (close) path(p, fill) else c.drawPath(p, linePaint)
    }

    fun dot(x: Float, y: Float, r: Float) = c.drawCircle(x, y, r, inkPaint)

    /** Eyes and noses: black on a filled sticker, ink on line art. */
    fun darkDot(x: Float, y: Float, r: Float) = c.drawCircle(x, y, r, if (filled) darkPaint else inkPaint)

    fun darkOval(l: Float, t: Float, r: Float, b: Float) = c.drawOval(RectF(l, t, r, b), if (filled) darkPaint else inkPaint)
}

/** Runs [block] with the canvas moved so (x, y) is the origin and one unit equals [size]. */
inline fun Scene.unit(x: Float, y: Float, size: Float, rot: Float, block: () -> Unit) {
    canvas.save()
    canvas.translate(x, y)
    canvas.rotate(rot)
    canvas.scale(size, size)
    block()
    canvas.restore()
}

/** The doodle library: everyday comic icons. */
object Doodles {
    val icons: List<(Pen) -> Unit> = listOf(
        ::star, ::heart, ::spiral, ::planet, ::bolt, ::cloud, ::flower, ::smiley, ::note, ::moon,
        ::sparkle, ::gem, ::ghost, ::skull, ::cat, ::paw, ::controller, ::dice, ::glasses, ::atom,
        ::rainbow, ::guitar, ::peace, ::wifi, ::octopus, ::spider, ::iceCream, ::coffee, ::rocket, ::ufo,
        ::comet, ::mushroom, ::headphones, ::pizza, ::eye, ::crown, ::cassette, ::alien, ::bone, ::sun,
    )

    val count: Int get() = icons.size

    /** Draws icon [index] centred at (x, y). [width] is the stroke width in canvas units. */
    fun draw(
        s: Scene, index: Int, x: Float, y: Float, size: Float, rot: Float,
        ink: Int, width: Float, fills: IntArray? = null, alpha: Int = 255, glow: Float = 0f,
    ) {
        s.unit(x, y, size, rot) {
            val pen = Pen(s, ink, width / size, fills, alpha, if (glow > 0f) glow / size else 0f)
            icons[index % icons.size](pen)
        }
    }

    fun starPath(points: Int, outer: Float, inner: Float): Path {
        val p = Path()
        for (k in 0 until points * 2) {
            val r = if (k % 2 == 0) outer else inner
            val a = -PI / 2 + k * PI / points
            val x = (cos(a) * r).toFloat()
            val y = (sin(a) * r).toFloat()
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    private fun star(p: Pen) = p.path(starPath(5, 1f, 0.45f), 1)

    private fun heart(p: Pen) {
        val h = Path()
        h.moveTo(0f, 0.85f)
        h.cubicTo(-1.4f, -0.05f, -0.65f, -1.05f, 0f, -0.4f)
        h.cubicTo(0.65f, -1.05f, 1.4f, -0.05f, 0f, 0.85f)
        h.close()
        p.path(h, 0)
    }

    private fun spiral(p: Pen) {
        val path = Path()
        for (k in 0..60) {
            val t = k / 60f
            val a = t * TAU * 2.5f
            if (k == 0) path.moveTo(cos(a) * t, sin(a) * t) else path.lineTo(cos(a) * t, sin(a) * t)
        }
        p.path(path)
    }

    private fun planet(p: Pen) {
        p.c.save()
        p.c.rotate(-20f)
        p.arc(-1.05f, -0.3f, 1.05f, 0.3f, 180f, 180f)
        p.circle(0f, 0f, 0.55f, 3)
        p.arc(-1.05f, -0.3f, 1.05f, 0.3f, 0f, 180f)
        p.c.restore()
    }

    private fun bolt(p: Pen) = p.poly(0.25f, -1f, -0.45f, 0.12f, 0.02f, 0.12f, -0.22f, 1f, 0.5f, -0.22f, 0.05f, -0.22f, fill = 1)

    private fun cloud(p: Pen) {
        val path = Path()
        path.moveTo(-0.55f, 0.37f)
        path.arcTo(RectF(-0.87f, -0.27f, -0.23f, 0.37f), 90f, 180f, false)
        path.arcTo(RectF(-0.45f, -0.65f, 0.45f, 0.25f), 200f, 140f, false)
        path.arcTo(RectF(0.15f, -0.35f, 0.85f, 0.35f), 250f, 200f, false)
        path.close()
        p.path(path, 4)
    }

    private fun flower(p: Pen) {
        for (k in 0 until 5) {
            p.c.save()
            p.c.rotate(k * 72f)
            p.oval(-0.26f, -1f, 0.26f, -0.28f, 0)
            p.c.restore()
        }
        p.circle(0f, 0f, 0.28f, 1)
    }

    private fun smiley(p: Pen) {
        p.circle(0f, 0f, 1f, 1)
        p.darkDot(-0.35f, -0.25f, 0.1f)
        p.darkDot(0.35f, -0.25f, 0.1f)
        p.arc(-0.55f, -0.4f, 0.55f, 0.55f, 20f, 140f)
    }

    private fun note(p: Pen) {
        p.darkOval(-0.85f, 0.45f, -0.3f, 0.85f)
        p.darkOval(0.15f, 0.25f, 0.7f, 0.65f)
        p.oval(-0.85f, 0.45f, -0.3f, 0.85f)
        p.oval(0.15f, 0.25f, 0.7f, 0.65f)
        p.line(-0.32f, 0.62f, -0.32f, -0.7f)
        p.line(0.68f, 0.42f, 0.68f, -0.9f)
        p.poly(-0.32f, -0.7f, 0.68f, -0.9f, 0.68f, -0.62f, -0.32f, -0.42f, fill = 2)
    }

    private fun moon(p: Pen) {
        val path = Path()
        path.arcTo(RectF(-1f, -1f, 1f, 1f), 70f, 220f, true)
        path.arcTo(RectF(-0.5f, -0.95f, 1.4f, 0.95f), 263f, -167f, false)
        path.close()
        p.path(path, 1)
    }

    private fun sparkle(p: Pen) {
        val path = Path()
        path.moveTo(0f, -1f)
        path.quadTo(0f, 0f, 1f, 0f)
        path.quadTo(0f, 0f, 0f, 1f)
        path.quadTo(0f, 0f, -1f, 0f)
        path.quadTo(0f, 0f, 0f, -1f)
        path.close()
        p.path(path, 4)
    }

    private fun gem(p: Pen) {
        p.poly(-0.9f, -0.3f, -0.5f, -0.75f, 0.5f, -0.75f, 0.9f, -0.3f, 0f, 0.9f, fill = 4)
        p.line(-0.9f, -0.3f, 0.9f, -0.3f)
        p.poly(-0.5f, -0.75f, -0.25f, -0.3f, 0f, 0.9f, close = false)
        p.poly(0.5f, -0.75f, 0.25f, -0.3f, 0f, 0.9f, close = false)
        p.poly(-0.25f, -0.3f, 0f, -0.75f, 0.25f, -0.3f, close = false)
    }

    private fun ghost(p: Pen) {
        val path = Path()
        path.moveTo(-0.7f, 0.8f)
        path.lineTo(-0.7f, -0.1f)
        path.arcTo(RectF(-0.7f, -0.8f, 0.7f, 0.6f), 180f, 180f, false)
        path.lineTo(0.7f, 0.8f)
        path.lineTo(0.47f, 0.6f)
        path.lineTo(0.23f, 0.8f)
        path.lineTo(0f, 0.6f)
        path.lineTo(-0.23f, 0.8f)
        path.lineTo(-0.47f, 0.6f)
        path.close()
        p.path(path, 4)
        p.darkDot(-0.25f, -0.15f, 0.09f)
        p.darkDot(0.25f, -0.15f, 0.09f)
        p.oval(-0.1f, 0.05f, 0.1f, 0.25f)
    }

    private fun skull(p: Pen) {
        p.rrect(-0.38f, 0.25f, 0.38f, 0.78f, 0.12f, 4)
        p.circle(0f, -0.2f, 0.72f, 4)
        p.darkOval(-0.45f, -0.35f, -0.1f, 0f)
        p.darkOval(0.1f, -0.35f, 0.45f, 0f)
        p.poly(0f, 0.08f, -0.09f, 0.26f, 0.09f, 0.26f, fill = 0)
        for (x in floatArrayOf(-0.19f, 0f, 0.19f)) p.line(x, 0.52f, x, 0.78f)
    }

    private fun cat(p: Pen) {
        p.poly(-0.78f, -0.1f, -0.66f, -0.95f, -0.18f, -0.48f, fill = 1)
        p.poly(0.78f, -0.1f, 0.66f, -0.95f, 0.18f, -0.48f, fill = 1)
        p.oval(-0.82f, -0.55f, 0.82f, 0.8f, 1)
        p.darkDot(-0.32f, 0.02f, 0.09f)
        p.darkDot(0.32f, 0.02f, 0.09f)
        p.poly(-0.09f, 0.22f, 0.09f, 0.22f, 0f, 0.33f, fill = 0)
        p.arc(-0.2f, 0.2f, 0f, 0.5f, 0f, 180f)
        p.arc(0f, 0.2f, 0.2f, 0.5f, 0f, 180f)
        for (sgn in floatArrayOf(-1f, 1f)) {
            p.line(sgn * 0.35f, 0.3f, sgn * 1.05f, 0.18f)
            p.line(sgn * 0.35f, 0.38f, sgn * 1.05f, 0.45f)
        }
    }

    private fun paw(p: Pen) {
        p.oval(-0.52f, -0.05f, 0.52f, 0.75f, 0)
        p.circle(-0.65f, -0.3f, 0.2f, 0)
        p.circle(-0.23f, -0.68f, 0.2f, 0)
        p.circle(0.23f, -0.68f, 0.2f, 0)
        p.circle(0.65f, -0.3f, 0.2f, 0)
    }

    private fun controller(p: Pen) {
        val wire = Path()
        wire.moveTo(0f, -0.45f)
        wire.quadTo(0.05f, -0.8f, 0.35f, -0.95f)
        p.path(wire)
        p.rrect(-0.98f, -0.45f, 0.98f, 0.5f, 0.42f, 3)
        p.rrect(-0.72f, -0.07f, -0.3f, 0.09f, 0.03f)
        p.rrect(-0.59f, -0.2f, -0.43f, 0.22f, 0.03f)
        p.circle(0.42f, -0.1f, 0.1f, 0)
        p.circle(0.66f, 0.1f, 0.1f, 1)
    }

    private fun dice(p: Pen) {
        p.rrect(-0.8f, -0.8f, 0.8f, 0.8f, 0.24f, 4)
        for ((x, y) in listOf(-0.4f to -0.4f, 0.4f to -0.4f, 0f to 0f, -0.4f to 0.4f, 0.4f to 0.4f)) p.darkDot(x, y, 0.12f)
    }

    private fun glasses(p: Pen) {
        p.rrect(-0.98f, -0.35f, -0.12f, 0.32f, 0.28f, 3)
        p.rrect(0.12f, -0.35f, 0.98f, 0.32f, 0.28f, 3)
        val bridge = Path()
        bridge.moveTo(-0.12f, -0.12f)
        bridge.quadTo(0f, -0.3f, 0.12f, -0.12f)
        p.path(bridge)
        p.line(-0.98f, -0.25f, -1.1f, -0.42f)
        p.line(0.98f, -0.25f, 1.1f, -0.42f)
    }

    private fun atom(p: Pen) {
        for (r in floatArrayOf(0f, 60f, 120f)) {
            p.c.save()
            p.c.rotate(r)
            p.oval(-1f, -0.34f, 1f, 0.34f)
            p.c.restore()
        }
        p.circle(0f, 0f, 0.15f, 0)
    }

    private fun rainbow(p: Pen) {
        val radii = floatArrayOf(0.95f, 0.75f, 0.55f)
        for (r in radii) p.arc(-r, 0.35f - r, r, 0.35f + r, 180f, 180f)
        p.circle(-0.78f, 0.38f, 0.2f, 4)
        p.circle(-0.55f, 0.42f, 0.16f, 4)
        p.circle(0.78f, 0.38f, 0.2f, 4)
        p.circle(0.55f, 0.42f, 0.16f, 4)
    }

    private fun guitar(p: Pen) {
        p.rrect(-0.09f, -1.0f, 0.09f, -0.2f, 0.03f, 4)
        p.rrect(-0.15f, -1.18f, 0.15f, -0.95f, 0.06f, 0)
        val body = Path()
        body.addCircle(0f, 0.45f, 0.5f, Path.Direction.CW)
        val upper = Path()
        upper.addCircle(0f, -0.08f, 0.37f, Path.Direction.CW)
        body.op(upper, Path.Op.UNION)
        p.path(body, 1)
        p.circle(0f, 0.18f, 0.14f)
        p.line(-0.03f, -1f, -0.03f, 0.62f)
        p.line(0.03f, -1f, 0.03f, 0.62f)
        p.line(-0.22f, 0.62f, 0.22f, 0.62f)
    }

    private fun peace(p: Pen) {
        p.circle(0f, 0f, 0.88f, 2)
        p.line(0f, -0.88f, 0f, 0.88f)
        p.line(0f, 0.05f, -0.62f, 0.62f)
        p.line(0f, 0.05f, 0.62f, 0.62f)
    }

    private fun wifi(p: Pen) {
        for (r in floatArrayOf(0.32f, 0.62f, 0.92f)) p.arc(-r, 0.6f - r, r, 0.6f + r, 225f, 90f)
        p.dot(0f, 0.6f, 0.11f)
    }

    private fun octopus(p: Pen) {
        for (k in 0 until 4) {
            val x = -0.45f + 0.3f * k
            val dir = if (k < 2) -1f else 1f
            val t = Path()
            t.moveTo(x, 0.2f)
            t.cubicTo(x, 0.7f, x + dir * 0.35f, 0.75f, x + dir * 0.25f, 0.5f)
            p.path(t)
        }
        val head = Path()
        head.moveTo(-0.6f, 0.25f)
        head.arcTo(RectF(-0.6f, -0.95f, 0.6f, 0.65f), 180f, 180f, false)
        head.close()
        p.path(head, 3)
        p.darkDot(-0.22f, -0.15f, 0.08f)
        p.darkDot(0.22f, -0.15f, 0.08f)
        p.arc(-0.15f, -0.05f, 0.15f, 0.15f, 20f, 140f)
    }

    private fun spider(p: Pen) {
        for (sgn in floatArrayOf(-1f, 1f)) for (i in 0 until 4) {
            p.poly(
                sgn * 0.3f, -0.18f + 0.13f * i,
                sgn * 0.72f, -0.62f + 0.28f * i,
                sgn * 0.98f, -0.2f + 0.3f * i,
                close = false,
            )
        }
        p.circle(0f, 0f, 0.45f, 0)
        p.circle(-0.15f, -0.08f, 0.1f, 4)
        p.circle(0.15f, -0.08f, 0.1f, 4)
        p.darkDot(-0.15f, -0.06f, 0.05f)
        p.darkDot(0.15f, -0.06f, 0.05f)
        p.arc(-0.18f, 0f, 0.18f, 0.28f, 20f, 140f)
    }

    private fun iceCream(p: Pen) {
        p.poly(-0.42f, -0.1f, 0.42f, -0.1f, 0f, 1f, fill = 1)
        p.line(-0.25f, 0.2f, 0.2f, 0.5f)
        p.line(0.25f, 0.2f, -0.2f, 0.5f)
        p.circle(0f, -0.42f, 0.5f, 3)
        p.circle(0f, -0.98f, 0.13f, 0)
    }

    private fun coffee(p: Pen) {
        p.arc(0.3f, -0.05f, 0.85f, 0.45f, -90f, 180f)
        p.poly(-0.55f, -0.2f, 0.55f, -0.2f, 0.42f, 0.72f, -0.42f, 0.72f, fill = 4)
        for (x in floatArrayOf(-0.2f, 0.15f)) {
            val st = Path()
            st.moveTo(x, -0.35f)
            st.cubicTo(x - 0.2f, -0.55f, x + 0.2f, -0.7f, x, -0.95f)
            p.path(st)
        }
    }

    private fun rocket(p: Pen) {
        val flame = Path()
        flame.moveTo(-0.18f, 0.58f)
        flame.quadTo(0f, 1.25f, 0.18f, 0.58f)
        flame.close()
        p.path(flame, 1)
        p.poly(-0.32f, 0.15f, -0.65f, 0.68f, -0.3f, 0.55f, fill = 0)
        p.poly(0.32f, 0.15f, 0.65f, 0.68f, 0.3f, 0.55f, fill = 0)
        val body = Path()
        body.moveTo(0f, -1f)
        body.cubicTo(0.48f, -0.6f, 0.44f, 0.2f, 0.32f, 0.58f)
        body.lineTo(-0.32f, 0.58f)
        body.cubicTo(-0.44f, 0.2f, -0.48f, -0.6f, 0f, -1f)
        body.close()
        p.path(body, 4)
        p.circle(0f, -0.3f, 0.17f, 4)
    }

    private fun ufo(p: Pen) {
        val dome = Path()
        dome.moveTo(-0.42f, -0.12f)
        dome.arcTo(RectF(-0.42f, -0.7f, 0.42f, 0.14f), 180f, 180f, false)
        dome.close()
        p.path(dome, 4)
        p.oval(-1f, -0.25f, 1f, 0.3f, 3)
        for (x in floatArrayOf(-0.55f, 0f, 0.55f)) p.circle(x, 0.04f, 0.07f, 1)
    }

    private fun comet(p: Pen) {
        for ((dx, len) in listOf(-0.12f to 1.3f, 0f to 1.6f, 0.12f to 1.2f)) {
            p.line(0.45f + dx, -0.45f - dx, 0.45f + dx - len * 0.7f, -0.45f - dx + len * 0.7f)
        }
        p.circle(0.5f, -0.5f, 0.3f, 2)
    }

    private fun mushroom(p: Pen) {
        p.rrect(-0.3f, -0.15f, 0.3f, 0.8f, 0.15f, 4)
        val cap = Path()
        cap.moveTo(-0.9f, -0.05f)
        cap.arcTo(RectF(-0.9f, -0.85f, 0.9f, 0.75f), 180f, 180f, false)
        cap.close()
        p.path(cap, 0)
        p.circle(-0.4f, -0.35f, 0.12f, 4)
        p.circle(0.15f, -0.55f, 0.1f, 4)
        p.circle(0.5f, -0.25f, 0.1f, 4)
    }

    private fun headphones(p: Pen) {
        p.arc(-0.8f, -0.9f, 0.8f, 0.7f, 180f, 180f)
        p.rrect(-0.98f, -0.1f, -0.58f, 0.65f, 0.18f, 3)
        p.rrect(0.58f, -0.1f, 0.98f, 0.65f, 0.18f, 3)
    }

    private fun pizza(p: Pen) {
        p.poly(0f, 0.95f, -0.72f, -0.6f, 0.72f, -0.6f, fill = 1)
        p.rrect(-0.85f, -0.85f, 0.85f, -0.55f, 0.15f, 0)
        p.circle(-0.2f, -0.25f, 0.13f, 0)
        p.circle(0.2f, -0.05f, 0.11f, 0)
        p.circle(0f, 0.38f, 0.1f, 0)
    }

    private fun eye(p: Pen) {
        val almond = Path()
        almond.moveTo(-1f, 0f)
        almond.quadTo(0f, -0.9f, 1f, 0f)
        almond.quadTo(0f, 0.9f, -1f, 0f)
        almond.close()
        p.path(almond, 4)
        p.circle(0f, 0f, 0.34f, 3)
        p.darkDot(0f, 0f, 0.14f)
        for (x in floatArrayOf(-0.5f, 0f, 0.5f)) p.line(x, -0.45f + kotlin.math.abs(x) * 0.25f, x * 1.2f, -0.72f + kotlin.math.abs(x) * 0.2f)
    }

    private fun crown(p: Pen) {
        p.poly(-0.85f, 0.5f, -0.85f, -0.4f, -0.42f, 0.05f, 0f, -0.62f, 0.42f, 0.05f, 0.85f, -0.4f, 0.85f, 0.5f, fill = 1)
        p.line(-0.85f, 0.3f, 0.85f, 0.3f)
        for (x in floatArrayOf(-0.85f, 0f, 0.85f)) p.circle(x, if (x == 0f) -0.62f else -0.4f, 0.09f, 0)
    }

    private fun cassette(p: Pen) {
        p.rrect(-0.98f, -0.62f, 0.98f, 0.62f, 0.1f, 3)
        p.rrect(-0.75f, -0.45f, 0.75f, 0.12f, 0.06f, 4)
        p.circle(-0.38f, -0.17f, 0.15f)
        p.circle(0.38f, -0.17f, 0.15f)
        p.poly(-0.55f, 0.62f, -0.42f, 0.3f, 0.42f, 0.3f, 0.55f, 0.62f, close = false)
    }

    private fun alien(p: Pen) {
        val head = Path()
        head.moveTo(0f, -0.95f)
        head.cubicTo(0.95f, -0.95f, 0.75f, 0.4f, 0f, 0.92f)
        head.cubicTo(-0.75f, 0.4f, -0.95f, -0.95f, 0f, -0.95f)
        head.close()
        p.path(head, 2)
        for (sgn in floatArrayOf(-1f, 1f)) {
            p.c.save()
            p.c.rotate(sgn * 28f, sgn * 0.3f, -0.05f)
            p.darkOval(sgn * 0.3f - 0.2f, -0.18f, sgn * 0.3f + 0.2f, 0.08f)
            p.c.restore()
        }
        p.line(-0.1f, 0.5f, 0.1f, 0.5f)
    }

    private fun bone(p: Pen) {
        val b = Path()
        b.addRect(-0.7f, -0.13f, 0.7f, 0.13f, Path.Direction.CW)
        for ((x, y) in listOf(-0.72f to -0.17f, -0.72f to 0.17f, 0.72f to -0.17f, 0.72f to 0.17f)) {
            val k = Path()
            k.addCircle(x, y, 0.2f, Path.Direction.CW)
            b.op(k, Path.Op.UNION)
        }
        p.path(b, 4)
    }

    private fun sun(p: Pen) {
        for (k in 0 until 8) {
            val a = k * TAU / 8f
            p.line(cos(a) * 0.62f, sin(a) * 0.62f, cos(a) * 0.98f, sin(a) * 0.98f)
        }
        p.circle(0f, 0f, 0.45f, 1)
    }
}

/** Small filler marks used between doodles: squiggles, zigzags, rings, dots and symbols. */
private fun Scene.filler(x: Float, y: Float, color: Int, width: Float, alpha: Int) {
    val paint = stroke(color, width, alpha)
    when (i(0, 6)) {
        0 -> canvas.drawCircle(x, y, f(4f, 8f), paint)
        1 -> dot(x, y, f(2f, 3.5f), color, alpha)
        2 -> {
            val p = Path()
            p.moveTo(x - 18f, y)
            for (k in 1..6) p.lineTo(x - 18f + k * 6f, y + if (k % 2 == 0) 0f else -6f)
            canvas.save(); canvas.rotate(f(-40f, 40f), x, y); canvas.drawPath(p, paint); canvas.restore()
        }
        3 -> {
            val p = Path()
            p.moveTo(x - 20f, y)
            p.cubicTo(x - 10f, y - 12f, x, y + 12f, x + 10f, y)
            p.cubicTo(x + 15f, y - 6f, x + 18f, y - 6f, x + 22f, y)
            canvas.save(); canvas.rotate(f(-60f, 60f), x, y); canvas.drawPath(p, paint); canvas.restore()
        }
        4 -> {
            val r = f(5f, 9f)
            canvas.drawLine(x - r, y, x + r, y, paint)
            canvas.drawLine(x, y - r, x, y + r, paint)
        }
        else -> {
            val symbols = arrayOf("π", "Ω", "zZz", "42", "♪", "∞", "✦", "♥", "?!", "★")
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.alpha = alpha
                textSize = f(16f, 24f)
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(symbols[i(0, symbols.size)], x, y + tp.textSize / 3f, tp)
        }
    }
}

// ---------- Styles ----------

/** Loose scatter of line-art doodles in the palette colours. */
object DoodleScatter : Style("doodle_scatter", "Doodle Scatter", Category.DOODLE) {
    override fun draw(s: Scene) {
        val cell = 150f / sqrt(s.density)
        val cols = (s.w / cell).toInt() + 1
        val rows = (s.h / cell).toInt() + 1
        for (gy in 0..rows) for (gx in 0..cols) {
            val x = gx * cell + s.f(0.2f, 0.8f) * cell
            val y = gy * cell + s.f(0.2f, 0.8f) * cell
            if (s.chance(0.78f)) {
                Doodles.draw(s, s.i(0, Doodles.count), x, y, s.f(24f, 42f), s.f(-30f, 30f), s.anyColor(), 3f)
            } else {
                s.filler(x, y, s.anyColor(), 2.4f, 200)
            }
        }
    }
}

/** Dense doodle wallpaper with a colour sweep across the screen, like a sticker-covered wall. */
object DoodlePattern : Style("doodle_pattern", "Doodle Wall", Category.DOODLE) {
    override val autoPalettes = listOf("hue", "hue", "aurora", "ocean", "violet", "sunset", "cyan")

    override fun draw(s: Scene) {
        val cell = 92f / sqrt(s.density)
        val cols = (s.w / cell).toInt() + 1
        val rows = (s.h / cell).toInt() + 1
        val diagonal = s.chance(0.5f)
        for (gy in 0..rows) for (gx in 0..cols) {
            val x = gx * cell + s.f(0.25f, 0.75f) * cell
            val y = gy * cell + s.f(0.25f, 0.75f) * cell
            val t = if (diagonal) (x / s.w + (1f - y / s.h)) / 2f else (x / s.w + y / s.h) / 2f
            val color = s.grad(t)
            if (s.chance(0.82f)) {
                Doodles.draw(s, s.i(0, Doodles.count), x, y, s.f(20f, 30f), s.f(-25f, 25f), color, 2.3f, alpha = 235)
            }
            repeat(2) { s.filler(x + s.f(-cell / 2f, cell / 2f), y + s.f(-cell / 2f, cell / 2f), color, 1.8f, 190) }
        }
    }
}

/** One big glowing doodle orbited by small ones. */
object DoodleSpotlight : Style("doodle_spotlight", "Doodle Spotlight", Category.DOODLE) {
    override fun draw(s: Scene) {
        val x = s.cx
        val y = s.h * s.f(0.38f, 0.5f)
        Doodles.draw(s, s.i(0, Doodles.count), x, y, s.f(170f, 220f), s.f(-12f, 12f), s.color(0), 6f, glow = 16f)

        val orbit = s.f(300f, 360f)
        val dash = s.stroke(s.color(1), 2f, 70).apply { pathEffect = DashPathEffect(floatArrayOf(6f, 14f), 0f) }
        s.canvas.drawCircle(x, y, orbit, dash)
        repeat(s.count(14)) {
            val a = s.f(0f, TAU)
            val d = s.f(orbit - 40f, orbit + 280f)
            Doodles.draw(s, s.i(0, Doodles.count), x + cos(a) * d, y + sin(a) * d, s.f(12f, 24f), s.f(-30f, 30f), s.anyColor(), 2.4f, alpha = 210)
        }
    }
}

/** Flat pastel doodle stickers with white outlines. */
object DoodleStickers : Style("doodle_stickers", "Doodle Stickers", Category.DOODLE) {
    override val autoPalettes = listOf("candy", "candy", "sakura", "sunset", "ocean", "aurora")

    override fun draw(s: Scene) {
        val cell = 175f / sqrt(s.density)
        val cols = (s.w / cell).toInt() + 1
        val rows = (s.h / cell).toInt() + 1
        val fills = s.palette.colors
        for (gy in 0..rows) for (gx in 0..cols) {
            val x = gx * cell + s.f(0.2f, 0.8f) * cell
            val y = gy * cell + s.f(0.2f, 0.8f) * cell
            if (s.chance(0.8f)) {
                // Rotate the fill order so neighbours differ.
                val shift = s.i(0, fills.size)
                val rotated = IntArray(fills.size) { fills[(it + shift) % fills.size] }
                Doodles.draw(s, s.i(0, Doodles.count), x, y, s.f(34f, 52f), s.f(-25f, 25f), Color.WHITE, 3.2f, rotated)
            }
            repeat(2) { s.filler(x + s.f(-cell / 2f, cell / 2f), y + s.f(-cell / 2f, cell / 2f), Color.WHITE, 2f, 200) }
        }
    }
}

/** Cartoon outer space: astronaut pets, banded planets, rockets, satellites and comets. */
object SpaceDoodles : Style("space_doodles", "Space Doodles", Category.DOODLE) {
    override val autoPalettes = listOf("candy", "candy", "candy", "sakura", "ocean")

    private const val INK = Color.WHITE

    override fun draw(s: Scene) {
        val pal = s.palette.colors
        // Background texture: dots, sparkles, swirls and motion lines.
        repeat(s.count(260)) {
            val c = if (s.chance(0.7f)) Color.WHITE else s.anyColor()
            s.dot(s.f(0f, s.w), s.f(0f, s.h), s.f(1f, 3.4f), c, s.i(150, 256))
        }
        repeat(s.count(26)) { motionLines(s, s.f(0f, s.w), s.f(0f, s.h)) }

        val cell = 230f / sqrt(s.density)
        val cols = (s.w / cell).toInt() + 1
        val rows = (s.h / cell).toInt() + 1
        for (gy in 0..rows) for (gx in 0..cols) {
            val x = gx * cell + s.f(0.2f, 0.8f) * cell
            val y = gy * cell + s.f(0.2f, 0.8f) * cell
            val rot = s.f(-25f, 25f)
            when (s.i(0, 11)) {
                0, 1 -> bandedPlanet(s, x, y, s.f(42f, 80f), pal)
                2 -> craterPlanet(s, x, y, s.f(40f, 70f), pal)
                3 -> ringPlanet(s, x, y, s.f(30f, 48f), pal)
                4 -> astronaut(s, x, y, s.f(55f, 72f), rot, s.i(0, 3), pal)
                5 -> Doodles.draw(s, 28, x, y, s.f(40f, 60f), s.f(-50f, 50f), INK, 3.2f, pal) // rocket
                6 -> Doodles.draw(s, 29, x, y, s.f(42f, 60f), rot, INK, 3.2f, pal) // ufo
                7 -> satellite(s, x, y, s.f(44f, 60f), rot)
                8 -> comet(s, x, y, s.f(60f, 110f), s.anyColor())
                9 -> starburst(s, x, y, s.f(26f, 42f), pal)
                else -> dotCluster(s, x, y, pal)
            }
            if (s.chance(0.6f)) sparkle(s, x + s.f(-cell / 2f, cell / 2f), y + s.f(-cell / 2f, cell / 2f))
        }
    }

    private fun sparkle(s: Scene, x: Float, y: Float) {
        val r = s.f(6f, 12f)
        val p = s.stroke(Color.WHITE, 2f, 230)
        for (k in 0 until 4) {
            val a = k * PI.toFloat() / 4f
            s.canvas.drawLine(x - cos(a) * r, y - sin(a) * r, x + cos(a) * r, y + sin(a) * r, p)
        }
    }

    private fun motionLines(s: Scene, x: Float, y: Float) {
        val p = s.stroke(Color.WHITE, 1.8f, s.i(120, 230))
        s.canvas.save()
        s.canvas.rotate(s.f(0f, 360f), x, y)
        if (s.chance(0.3f)) {
            // Zigzag lightning line.
            val path = Path()
            path.moveTo(x - 50f, y)
            for (k in 1..5) path.lineTo(x - 50f + k * 20f, y + if (k % 2 == 0) 0f else -22f)
            s.canvas.drawPath(path, p)
        } else {
            for (k in 0 until s.i(2, 4)) {
                val path = Path()
                val o = k * 9f
                path.moveTo(x - 50f, y + o)
                path.cubicTo(x - 20f, y - 25f + o, x + 15f, y + 25f + o, x + 50f, y + o)
                s.canvas.drawPath(path, p)
            }
        }
        s.canvas.restore()
    }

    private fun bandedPlanet(s: Scene, x: Float, y: Float, r: Float, pal: IntArray) {
        val base = pal[s.i(0, pal.size)]
        val band = pal[s.i(0, pal.size)]
        val clip = Path()
        clip.addCircle(x, y, r, Path.Direction.CW)
        s.canvas.drawCircle(x, y, r, s.fill(base))
        s.canvas.save()
        s.canvas.clipPath(clip)
        val tilt = s.f(-25f, 25f)
        s.canvas.rotate(tilt, x, y)
        for (k in 0 until s.i(2, 4)) {
            val by = y - r + (k + 0.5f) * 2f * r / 3f + s.f(-8f, 8f)
            val wave = Path()
            wave.moveTo(x - r * 1.2f, by)
            var wx = x - r * 1.2f
            while (wx < x + r * 1.2f) {
                wave.quadTo(wx + r * 0.15f, by - r * 0.12f, wx + r * 0.3f, by)
                wave.quadTo(wx + r * 0.45f, by + r * 0.12f, wx + r * 0.6f, by)
                wx += r * 0.6f
            }
            s.canvas.drawPath(wave, s.stroke(if (k % 2 == 0) band else Color.WHITE, r * 0.16f))
        }
        s.canvas.restore()
        s.canvas.drawCircle(x, y, r, s.stroke(INK, 3f))
        // Orbit squiggles around the planet.
        if (s.chance(0.6f)) {
            val p = s.stroke(pal[s.i(0, pal.size)], 2.4f, 220)
            val gap = r * 1.25f
            s.canvas.drawArc(RectF(x - gap, y - gap, x + gap, y + gap), 200f, 50f, false, p)
            s.canvas.drawArc(RectF(x - gap, y - gap, x + gap, y + gap), 20f, 50f, false, p)
        }
    }

    private fun craterPlanet(s: Scene, x: Float, y: Float, r: Float, pal: IntArray) {
        val base = pal[s.i(0, pal.size)]
        val spot = pal[s.i(0, pal.size)]
        s.canvas.drawCircle(x, y, r, s.fill(base))
        val clip = Path()
        clip.addCircle(x, y, r, Path.Direction.CW)
        s.canvas.save()
        s.canvas.clipPath(clip)
        repeat(s.i(3, 6)) {
            val a = s.f(0f, TAU)
            val d = s.f(0f, r * 0.8f)
            s.canvas.drawOval(
                RectF(x + cos(a) * d - r * 0.25f, y + sin(a) * d - r * 0.18f, x + cos(a) * d + r * 0.25f, y + sin(a) * d + r * 0.18f),
                s.fill(spot),
            )
        }
        s.canvas.restore()
        s.canvas.drawCircle(x, y, r, s.stroke(INK, 3f))
    }

    private fun ringPlanet(s: Scene, x: Float, y: Float, r: Float, pal: IntArray) {
        val ring = s.stroke(pal[s.i(0, pal.size)], 3.5f)
        val rect = RectF(x - r * 2f, y - r * 0.5f, x + r * 2f, y + r * 0.5f)
        val tilt = s.f(-25f, 25f)
        s.canvas.save()
        s.canvas.rotate(tilt, x, y)
        s.canvas.drawArc(rect, 180f, 180f, false, ring)
        s.canvas.drawCircle(x, y, r, s.fill(pal[s.i(0, pal.size)]))
        s.canvas.drawCircle(x, y, r, s.stroke(INK, 3f))
        s.canvas.drawArc(rect, 0f, 180f, false, ring)
        s.canvas.restore()
    }

    private fun satellite(s: Scene, x: Float, y: Float, size: Float, rot: Float) {
        s.unit(x, y, size, rot) {
            val p = Pen(s, INK, 3f / size, intArrayOf(Color.BLACK))
            for (sgn in floatArrayOf(-1f, 1f)) {
                p.line(sgn * 0.2f, 0f, sgn * 0.35f, 0f)
                p.rrect(sgn * 0.35f, -0.28f, sgn * 1.05f, 0.28f, 0.02f, 0)
                p.line(sgn * 0.7f, -0.28f, sgn * 0.7f, 0.28f)
                p.line(sgn * 0.35f, 0f, sgn * 1.05f, 0f)
            }
            p.rrect(-0.2f, -0.35f, 0.2f, 0.35f, 0.05f, 0)
            p.arc(-0.25f, -0.85f, 0.25f, -0.35f, 0f, 180f)
            p.line(0f, -0.6f, 0f, -0.82f)
            p.dot(0f, -0.85f, 0.06f)
        }
    }

    private fun comet(s: Scene, x: Float, y: Float, len: Float, color: Int) {
        val a = s.f(0f, TAU)
        val dx = cos(a)
        val dy = sin(a)
        val tail = Path()
        tail.moveTo(x - dy * 9f, y + dx * 9f)
        tail.lineTo(x - dx * len, y - dy * len)
        tail.lineTo(x + dy * 9f, y - dx * 9f)
        tail.close()
        s.canvas.drawPath(tail, s.fill(color, 200))
        s.canvas.drawLine(x, y, x - dx * len * 0.8f, y - dy * len * 0.8f, s.stroke(Color.WHITE, 2f, 220))
        s.canvas.drawCircle(x, y, 10f, s.fill(color))
        s.canvas.drawCircle(x, y, 10f, s.stroke(INK, 2.4f))
    }

    private fun starburst(s: Scene, x: Float, y: Float, r: Float, pal: IntArray) {
        s.unit(x, y, r, s.f(0f, 30f)) {
            s.canvas.drawPath(Doodles.starPath(12, 1f, 0.55f), s.fill(pal[0]))
            s.canvas.drawCircle(0f, 0f, 0.28f, s.fill(pal[pal.size - 1]))
        }
        val p = s.stroke(pal[0], 2f, 200)
        for (k in 0 until 6) {
            val a = k * TAU / 6f + 0.3f
            s.canvas.drawLine(x + cos(a) * r * 1.25f, y + sin(a) * r * 1.25f, x + cos(a) * r * 1.6f, y + sin(a) * r * 1.6f, p)
        }
    }

    private fun dotCluster(s: Scene, x: Float, y: Float, pal: IntArray) {
        val tilt = s.f(0f, PI.toFloat())
        val c = pal[s.i(0, pal.size)]
        val c2 = pal[s.i(0, pal.size)]
        for (row in -2..2) for (col in -5..5) {
            if (s.chance(0.25f)) continue
            val lx = col * 11f
            val ly = row * 11f + if (col % 2 == 0) 0f else 5.5f
            if ((lx / 60f) * (lx / 60f) + (ly / 24f) * (ly / 24f) > 1f) continue
            val px = x + lx * cos(tilt) - ly * sin(tilt)
            val py = y + lx * sin(tilt) + ly * cos(tilt)
            s.dot(px, py, 4f, if (s.chance(0.7f)) c else c2)
        }
    }

    /** A pet astronaut: 0 cat, 1 dog, 2 bunny. */
    private fun astronaut(s: Scene, x: Float, y: Float, size: Float, rot: Float, kind: Int, pal: IntArray) {
        val fur = when (kind) {
            0 -> Color.rgb(142, 142, 150)
            1 -> Color.rgb(245, 194, 122)
            else -> Color.rgb(242, 242, 242)
        }
        val ear = when (kind) {
            0 -> Color.rgb(110, 110, 118)
            1 -> Color.rgb(183, 121, 79)
            else -> Color.rgb(244, 160, 170)
        }
        s.unit(x, y, size, rot) {
            val p = Pen(s, INK, 3f / size, intArrayOf(fur, ear, pal[0], pal[pal.size - 1], Color.BLACK))
            // Jetpack and flame.
            val flame = Path()
            flame.moveTo(0.55f, 0.15f)
            flame.quadTo(1.35f, 0.05f, 1.15f, -0.25f)
            flame.quadTo(0.95f, -0.05f, 0.55f, -0.15f)
            flame.close()
            p.path(flame, 2)
            p.rrect(0.25f, -0.3f, 0.62f, 0.25f, 0.08f, 3)
            // Body and legs.
            for (lx in floatArrayOf(-0.45f, -0.2f, 0.15f, 0.4f)) p.rrect(lx, 0.45f, lx + 0.18f, 0.85f, 0.09f, 0)
            p.oval(-0.65f, -0.05f, 0.6f, 0.7f, 0)
            if (kind == 2) {
                p.oval(-0.38f, -1.45f, -0.12f, -0.65f, 0)
                p.oval(0.12f, -1.45f, 0.38f, -0.65f, 0)
            }
            // Helmet glass, face, then the helmet rim.
            p.circle(0f, -0.35f, 0.62f, 4)
            if (kind == 0) {
                p.poly(-0.4f, -0.4f, -0.35f, -0.8f, -0.1f, -0.6f, fill = 0)
                p.poly(0.4f, -0.4f, 0.35f, -0.8f, 0.1f, -0.6f, fill = 0)
            }
            p.oval(-0.42f, -0.7f, 0.42f, 0.0f, 0)
            if (kind == 1) {
                p.oval(-0.52f, -0.62f, -0.25f, -0.15f, 1)
                p.oval(0.25f, -0.62f, 0.52f, -0.15f, 1)
                p.oval(-0.18f, -0.32f, 0.18f, -0.08f, 0)
            }
            p.darkDot(-0.17f, -0.4f, 0.06f)
            p.darkDot(0.17f, -0.4f, 0.06f)
            p.oval(-0.06f, -0.3f, 0.06f, -0.22f, 1)
            if (kind == 0) {
                p.line(-0.15f, -0.25f, -0.45f, -0.3f)
                p.line(0.15f, -0.25f, 0.45f, -0.3f)
            }
            p.arc(-0.6f, -0.97f, 0.6f, 0.27f, 200f, 60f)
            p.rrect(-0.55f, 0.12f, 0.55f, 0.32f, 0.1f)
        }
    }
}

/** Comic-book pop: POW/BAM bursts, halftone dots, speech bubbles and action lines. */
object ComicPop : Style("comic_pop", "Comic Pop", Category.DOODLE) {
    override val autoPalettes = listOf("candy", "sunset", "lava", "acid", "cyan", "sakura")

    private val words = arrayOf("POW!", "BAM!", "ZAP!", "BOOM!", "WOW!", "KAPOW!", "WHAM!", "ZOOM!", "OMG!")
    private val bubbleText = arrayOf("?!", "♥", "...", "LOL", "HA!", "!!", "ZzZ")

    override fun draw(s: Scene) {
        val pal = s.palette.colors
        val bursts = s.i(3, 6)
        val placed = ArrayList<FloatArray>()
        repeat(bursts) {
            var x = 0f
            var y = 0f
            var r = 0f
            for (attempt in 0 until 12) {
                x = s.f(140f, s.w - 140f)
                y = s.f(140f, s.h - 140f)
                r = s.f(100f, 160f)
                if (placed.none { (it[0] - x) * (it[0] - x) + (it[1] - y) * (it[1] - y) < (it[2] + r) * (it[2] + r) }) break
            }
            placed.add(floatArrayOf(x, y, r))
            burst(s, x, y, r, pal[s.i(0, pal.size)], words[s.i(0, words.size)])
        }
        repeat(s.i(2, 4)) {
            bubble(s, s.f(100f, s.w - 100f), s.f(100f, s.h - 100f), s.f(50f, 80f), bubbleText[s.i(0, bubbleText.size)])
        }
        repeat(s.count(5)) {
            halftone(s, s.f(0f, s.w), s.f(0f, s.h), s.f(60f, 130f), pal[s.i(0, pal.size)])
        }
        // Little extras: bolts, stars, sparkles and hearts.
        val extras = intArrayOf(0, 1, 4, 10, 7)
        repeat(s.count(16)) {
            Doodles.draw(
                s, extras[s.i(0, extras.size)], s.f(0f, s.w), s.f(0f, s.h), s.f(16f, 30f), s.f(-30f, 30f),
                Color.WHITE, 2.6f, intArrayOf(pal[s.i(0, pal.size)]),
            )
        }
    }

    private fun burst(s: Scene, x: Float, y: Float, r: Float, color: Int, word: String) {
        // Action lines radiating out.
        val lines = s.stroke(Color.WHITE, 2.2f, 170)
        for (k in 0 until 18) {
            val a = k * TAU / 18f + s.f(-0.08f, 0.08f)
            val r1 = r * s.f(1.15f, 1.3f)
            val r2 = r * s.f(1.45f, 1.9f)
            s.canvas.drawLine(x + cos(a) * r1, y + sin(a) * r1, x + cos(a) * r2, y + sin(a) * r2, lines)
        }
        val spikes = s.i(10, 16)
        val path = Path()
        for (k in 0 until spikes * 2) {
            val rr = if (k % 2 == 0) r * s.f(0.92f, 1.1f) else r * s.f(0.6f, 0.72f)
            val a = k * PI.toFloat() / spikes
            val px = x + cos(a) * rr
            val py = y + sin(a) * rr
            if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        s.canvas.drawPath(path, s.fill(color))
        // Halftone shading inside the burst.
        s.canvas.save()
        s.canvas.clipPath(path)
        val shade = s.fill(Color.BLACK, 60)
        var hy = y - r
        while (hy < y + r) {
            var hx = x - r
            while (hx < x + r) {
                val t = ((hx - x) + (hy - y)) / (2f * r) + 0.5f
                val dr = 4.5f * t.coerceIn(0f, 1f)
                if (dr > 0.6f) s.canvas.drawCircle(hx, hy, dr, shade)
                hx += 12f
            }
            hy += 12f
        }
        s.canvas.restore()
        s.canvas.drawPath(path, s.stroke(Color.WHITE, 4f))

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = r * (if (word.length > 4) 0.42f else 0.55f)
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
        }
        val ty = y + tp.textSize * 0.35f
        s.canvas.save()
        s.canvas.rotate(s.f(-14f, 8f), x, y)
        tp.style = Paint.Style.STROKE
        tp.strokeWidth = tp.textSize * 0.16f
        tp.strokeJoin = Paint.Join.ROUND
        tp.color = Color.BLACK
        s.canvas.drawText(word, x + 4f, ty + 4f, tp)
        tp.color = Color.WHITE
        s.canvas.drawText(word, x, ty, tp)
        tp.style = Paint.Style.FILL
        tp.color = Color.BLACK
        s.canvas.drawText(word, x, ty, tp)
        s.canvas.restore()
    }

    private fun bubble(s: Scene, x: Float, y: Float, r: Float, text: String) {
        val path = Path()
        path.addOval(RectF(x - r * 1.3f, y - r, x + r * 1.3f, y + r), Path.Direction.CW)
        val tail = Path()
        val dir = if (s.chance(0.5f)) -1f else 1f
        tail.moveTo(x + dir * r * 0.3f, y + r * 0.8f)
        tail.lineTo(x + dir * r * 1.1f, y + r * 1.6f)
        tail.lineTo(x + dir * r * 0.75f, y + r * 0.6f)
        tail.close()
        path.op(tail, Path.Op.UNION)
        s.canvas.drawPath(path, s.fill(Color.BLACK))
        s.canvas.drawPath(path, s.stroke(Color.WHITE, 3.2f))
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = r * 0.7f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        s.canvas.drawText(text, x, y + tp.textSize * 0.35f, tp)
    }

    private fun halftone(s: Scene, x: Float, y: Float, r: Float, color: Int) {
        val paint = s.fill(color, 210)
        var hy = y - r
        while (hy <= y + r) {
            var hx = x - r
            while (hx <= x + r) {
                val d = sqrt((hx - x) * (hx - x) + (hy - y) * (hy - y)) / r
                if (d < 1f) s.canvas.drawCircle(hx, hy, 5f * (1f - d), paint)
                hx += 13f
            }
            hy += 13f
        }
    }
}
