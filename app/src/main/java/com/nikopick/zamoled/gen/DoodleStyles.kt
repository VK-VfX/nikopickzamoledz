package com.nikopick.zamoled.gen

import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Hand-drawn icons in unit space (radius ~1, centred on 0,0). */
object DoodleKit {
    private val shapes: List<(Path) -> Unit> = listOf(
        ::star, ::heart, ::spiral, ::planet, ::bolt, ::cloud, ::flower,
        ::smiley, ::note, ::squiggle, ::moon, ::sparkle, ::triangle, ::dots,
    )

    val count: Int get() = shapes.size

    private fun unit(index: Int): Path = Path().also { shapes[index % shapes.size](it) }

    private fun placed(index: Int, x: Float, y: Float, size: Float, rot: Float): Path {
        val p = unit(index)
        val m = Matrix()
        m.setScale(size, size)
        m.postRotate(rot)
        m.postTranslate(x, y)
        p.transform(m)
        return p
    }

    fun draw(
        s: Scene, index: Int, x: Float, y: Float, size: Float, rot: Float,
        color: Int, width: Float, alpha: Int = 255, glow: Float = 0f,
    ) {
        val p = placed(index, x, y, size, rot)
        if (glow > 0f) s.canvas.drawPath(p, s.stroke(color, width * 2.2f, alpha / 2, glow))
        s.canvas.drawPath(p, s.stroke(color, width, alpha))
        // A second, slightly offset pass gives the wobbly felt-pen look.
        val echo = placed(index, x + s.f(-1.6f, 1.6f), y + s.f(-1.6f, 1.6f), size * 1.03f, rot + s.f(-4f, 4f))
        s.canvas.drawPath(echo, s.stroke(color, width * 0.5f, alpha / 3))
    }

    private fun star(p: Path) {
        for (k in 0 until 10) {
            val r = if (k % 2 == 0) 1f else 0.45f
            val a = -PI / 2 + k * PI / 5
            val x = (cos(a) * r).toFloat()
            val y = (sin(a) * r).toFloat()
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
    }

    private fun heart(p: Path) {
        p.moveTo(0f, 0.85f)
        p.cubicTo(-1.4f, -0.05f, -0.65f, -1.05f, 0f, -0.4f)
        p.cubicTo(0.65f, -1.05f, 1.4f, -0.05f, 0f, 0.85f)
        p.close()
    }

    private fun spiral(p: Path) {
        val n = 60
        for (k in 0..n) {
            val t = k / n.toFloat()
            val a = t * TAU * 2.5f
            val x = cos(a) * t
            val y = sin(a) * t
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
    }

    private fun planet(p: Path) {
        p.addCircle(0f, 0f, 0.55f, Path.Direction.CW)
        p.addOval(RectF(-1.05f, -0.28f, 1.05f, 0.28f), Path.Direction.CW)
    }

    private fun bolt(p: Path) {
        p.moveTo(0.25f, -1f)
        p.lineTo(-0.45f, 0.12f)
        p.lineTo(0.02f, 0.12f)
        p.lineTo(-0.22f, 1f)
        p.lineTo(0.5f, -0.22f)
        p.lineTo(0.05f, -0.22f)
        p.close()
    }

    private fun cloud(p: Path) {
        p.moveTo(-0.55f, 0.37f)
        p.arcTo(RectF(-0.87f, -0.27f, -0.23f, 0.37f), 90f, 180f, false)
        p.arcTo(RectF(-0.45f, -0.65f, 0.45f, 0.25f), 200f, 140f, false)
        p.arcTo(RectF(0.15f, -0.35f, 0.85f, 0.35f), 250f, 200f, false)
        p.close()
    }

    private fun flower(p: Path) {
        val m = Matrix()
        for (k in 0 until 5) {
            val petal = Path()
            petal.addOval(RectF(-0.24f, -1f, 0.24f, -0.3f), Path.Direction.CW)
            m.setRotate(k * 72f)
            petal.transform(m)
            p.addPath(petal)
        }
        p.addCircle(0f, 0f, 0.24f, Path.Direction.CW)
    }

    private fun smiley(p: Path) {
        p.addCircle(0f, 0f, 1f, Path.Direction.CW)
        p.addCircle(-0.35f, -0.25f, 0.08f, Path.Direction.CW)
        p.addCircle(0.35f, -0.25f, 0.08f, Path.Direction.CW)
        p.addArc(RectF(-0.55f, -0.4f, 0.55f, 0.55f), 20f, 140f)
    }

    private fun note(p: Path) {
        p.addOval(RectF(-0.7f, 0.4f, -0.1f, 0.85f), Path.Direction.CW)
        p.moveTo(-0.12f, 0.6f)
        p.lineTo(-0.12f, -0.9f)
        p.quadTo(0.5f, -0.7f, 0.55f, -0.2f)
    }

    private fun squiggle(p: Path) {
        val n = 40
        for (k in 0..n) {
            val x = -1f + 2f * k / n
            val y = sin(x * 3f * PI.toFloat()) * 0.35f
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
    }

    private fun moon(p: Path) {
        p.arcTo(RectF(-1f, -1f, 1f, 1f), 70f, 220f, true)
        p.arcTo(RectF(-0.5f, -0.95f, 1.4f, 0.95f), 263f, -167f, false)
        p.close()
    }

    private fun sparkle(p: Path) {
        p.moveTo(0f, -1f)
        p.quadTo(0f, 0f, 1f, 0f)
        p.quadTo(0f, 0f, 0f, 1f)
        p.quadTo(0f, 0f, -1f, 0f)
        p.quadTo(0f, 0f, 0f, -1f)
        p.close()
    }

    private fun triangle(p: Path) {
        p.moveTo(0f, -1f)
        p.lineTo(0.87f, 0.5f)
        p.lineTo(-0.87f, 0.5f)
        p.close()
    }

    private fun dots(p: Path) {
        p.addCircle(-0.6f, 0.2f, 0.18f, Path.Direction.CW)
        p.addCircle(0f, -0.3f, 0.18f, Path.Direction.CW)
        p.addCircle(0.6f, 0.2f, 0.18f, Path.Direction.CW)
    }
}

object DoodleScatter : Style("doodle_scatter", "Doodle Scatter", Category.DOODLE) {
    override fun draw(s: Scene) {
        val cell = 150f / sqrt(s.density)
        val cols = (s.w / cell).toInt() + 1
        val rows = (s.h / cell).toInt() + 1
        for (gy in 0..rows) for (gx in 0..cols) {
            if (!s.chance(0.72f)) continue
            val x = gx * cell + s.f(0.15f, 0.85f) * cell
            val y = gy * cell + s.f(0.15f, 0.85f) * cell
            DoodleKit.draw(
                s, s.i(0, DoodleKit.count), x, y, s.f(24f, 48f), s.f(-35f, 35f),
                s.anyColor(), 3.2f,
            )
            if (s.chance(0.3f)) {
                repeat(3) { s.dot(x + s.f(-50f, 50f), y + s.f(-50f, 50f), s.f(1.5f, 3f), s.anyColor(), 160) }
            }
        }
    }
}

object DoodlePattern : Style("doodle_pattern", "Doodle Pattern", Category.DOODLE) {
    override fun draw(s: Scene) {
        val cell = 95f / sqrt(s.density)
        val color = s.color(0)
        val cols = (s.w / cell).toInt() + 1
        val rows = (s.h / cell).toInt() + 1
        for (gy in 0..rows) for (gx in 0..cols) {
            val off = if (gy % 2 == 0) 0f else cell / 2f
            val x = gx * cell + off + s.f(-10f, 10f)
            val y = gy * cell + s.f(-10f, 10f)
            DoodleKit.draw(s, s.i(0, DoodleKit.count), x, y, s.f(15f, 24f), s.f(-30f, 30f), color, 2.4f, 130)
            s.dot(x + cell / 2f, y + cell / 2f, 1.6f, color, 70)
        }
    }
}

object DoodleSpotlight : Style("doodle_spotlight", "Doodle Spotlight", Category.DOODLE) {
    override fun draw(s: Scene) {
        val x = s.cx
        val y = s.h * s.f(0.38f, 0.5f)
        DoodleKit.draw(s, s.i(0, DoodleKit.count), x, y, s.f(170f, 230f), s.f(-15f, 15f), s.color(0), 6f, 255, 18f)

        val orbit = s.f(300f, 360f)
        val dash = s.stroke(s.color(1), 2f, 70).apply { pathEffect = DashPathEffect(floatArrayOf(6f, 14f), 0f) }
        s.canvas.drawCircle(x, y, orbit, dash)

        repeat(s.count(14)) {
            val a = s.f(0f, TAU)
            val d = s.f(orbit - 40f, orbit + 280f)
            val px = x + cos(a) * d
            val py = y + sin(a) * d
            val icon = if (s.chance(0.6f)) 11 else s.i(0, DoodleKit.count)
            DoodleKit.draw(s, icon, px, py, s.f(10f, 24f), s.f(0f, 90f), s.anyColor(), 2.4f, 200)
        }
    }
}
