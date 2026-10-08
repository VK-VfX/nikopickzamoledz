package com.nikopick.zamoled.gen

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Doodle Bomb: a sketchbook page crammed edge to edge with original cartoon characters,
 * pattern-filled gaps and snaking tubes, all in white ink, then washed with a colour gradient.
 * Kept the old "comic_pop" id so saved favourites still open.
 */
object ComicPop : Style("comic_pop", "Doodle Bomb", Category.DOODLE) {
    override val autoPalettes = listOf("graffiti", "graffiti", "graffiti", "hue", "sunset", "candy", "aurora")

    private const val INK = Color.WHITE
    private val words = arrayOf("WHOA!", "NOPE", "YEAH!", "BOO!", "LOL", "OMG", "ZAMOLED", "HMM?", "RAD!", "YIKES")

    override fun draw(s: Scene) {
        // 1. Patterned patches cover the whole page.
        val rects = ArrayList<RectF>()
        split(s, RectF(0f, 0f, s.w, s.h), rects)
        for (r in rects) pattern(s, r)

        // 2. Tubes and tentacles snake across.
        repeat(s.count(5).coerceIn(2, 10)) { tube(s) }

        // 3. Characters packed tightly on top, biggest first.
        val placed = ArrayList<FloatArray>()
        val tiers = floatArrayOf(150f, 115f, 90f, 70f, 52f, 38f)
        for (r in tiers) {
            val tries = (if (r > 80f) 70 else 160) * s.density.coerceIn(0.5f, 1.6f)
            repeat(tries.toInt()) {
                val x = s.f(-r * 0.3f, s.w + r * 0.3f)
                val y = s.f(-r * 0.3f, s.h + r * 0.3f)
                val rr = r * s.f(0.85f, 1.15f)
                val fits = placed.none { hypot(it[0] - x, it[1] - y) < (it[2] + rr) * 0.78f }
                if (fits) placed.add(floatArrayOf(x, y, rr))
            }
        }
        placed.shuffle(s.rnd)
        for (c in placed) character(s, c[0], c[1], c[2])

        // 4. Colour wash: multiplying a gradient over the white ink tints the lines and leaves black black.
        val pal = s.palette.colors
        val stops = IntArray(pal.size * 2 - 1) { i -> pal[if (i < pal.size) i else pal.size * 2 - 2 - i] }
        val a = s.f(0f, TAU)
        val len = hypot(s.w, s.h) / 2f
        val wash = Paint().apply {
            shader = LinearGradient(
                s.cx - cos(a) * len, s.cy - sin(a) * len, s.cx + cos(a) * len, s.cy + sin(a) * len,
                stops, null, Shader.TileMode.CLAMP,
            )
            blendMode = BlendMode.MULTIPLY
        }
        s.canvas.drawRect(0f, 0f, s.w, s.h, wash)
    }

    // ---------- Background patterns ----------

    private fun split(s: Scene, r: RectF, out: MutableList<RectF>) {
        val big = r.width() > 260f || r.height() > 260f
        if (!big && (r.width() < 140f || r.height() < 140f || s.chance(0.35f))) {
            out.add(r)
            return
        }
        if (r.width() > r.height()) {
            val x = r.left + r.width() * s.f(0.35f, 0.65f)
            split(s, RectF(r.left, r.top, x, r.bottom), out)
            split(s, RectF(x, r.top, r.right, r.bottom), out)
        } else {
            val y = r.top + r.height() * s.f(0.35f, 0.65f)
            split(s, RectF(r.left, r.top, r.right, y), out)
            split(s, RectF(r.left, y, r.right, r.bottom), out)
        }
    }

    private fun pattern(s: Scene, r: RectF) {
        val c = s.canvas
        val line = s.stroke(INK, 1.4f, s.i(110, 190))
        val fill = s.fill(INK, s.i(110, 190))
        c.save()
        c.clipRect(r)
        when (s.i(0, 9)) {
            0 -> { // diagonal stripes
                c.rotate(s.f(30f, 60f), r.centerX(), r.centerY())
                var x = r.centerX() - 400f
                while (x < r.centerX() + 400f) { c.drawLine(x, r.centerY() - 400f, x, r.centerY() + 400f, line); x += 9f }
            }
            1 -> { // checkerboard
                val sz = s.f(12f, 20f)
                var y = r.top; var row = 0
                while (y < r.bottom) {
                    var x = r.left + if (row % 2 == 0) 0f else sz
                    while (x < r.right) { c.drawRect(x, y, x + sz, y + sz, fill); x += sz * 2 }
                    y += sz; row++
                }
            }
            2 -> { // dot grid
                var y = r.top + 6f
                while (y < r.bottom) {
                    var x = r.left + 6f
                    while (x < r.right) { c.drawCircle(x, y, 2.2f, fill); x += 12f }
                    y += 12f
                }
            }
            3 -> { // little stars
                var y = r.top + 14f; var row = 0
                while (y < r.bottom + 14f) {
                    var x = r.left + if (row % 2 == 0) 10f else 26f
                    while (x < r.right + 14f) {
                        c.save(); c.translate(x, y); c.scale(8f, 8f)
                        c.drawPath(Doodles.starPath(5, 1f, 0.45f), s.stroke(INK, 1.4f / 8f, 170))
                        c.restore()
                        x += 32f
                    }
                    y += 26f; row++
                }
            }
            4 -> { // concentric rings
                val cx = s.f(r.left, r.right)
                val cy = s.f(r.top, r.bottom)
                var rad = 6f
                while (rad < 400f) { c.drawCircle(cx, cy, rad, line); rad += 10f }
            }
            5 -> { // fish scales
                val sz = 18f
                var y = r.top; var row = 0
                while (y < r.bottom + sz) {
                    var x = r.left + if (row % 2 == 0) 0f else sz / 2f
                    while (x < r.right + sz) { c.drawArc(RectF(x - sz / 2f, y - sz / 2f, x + sz / 2f, y + sz / 2f), 0f, 180f, false, line); x += sz }
                    y += sz * 0.5f; row++
                }
            }
            6 -> { // cross hatching
                for (ang in floatArrayOf(45f, -45f)) {
                    c.save(); c.rotate(ang, r.centerX(), r.centerY())
                    var x = r.centerX() - 400f
                    while (x < r.centerX() + 400f) { c.drawLine(x, r.centerY() - 400f, x, r.centerY() + 400f, line); x += 11f }
                    c.restore()
                }
            }
            7 -> { // zigzag rows
                var y = r.top + 8f
                while (y < r.bottom) {
                    val p = Path(); p.moveTo(r.left, y)
                    var x = r.left; var up = true
                    while (x < r.right) { x += 10f; p.lineTo(x, if (up) y - 7f else y); up = !up }
                    c.drawPath(p, line); y += 14f
                }
            }
            else -> { // bubbles
                repeat(30) { c.drawCircle(s.f(r.left, r.right), s.f(r.top, r.bottom), s.f(3f, 16f), line) }
            }
        }
        c.restore()
        c.drawRect(r, s.stroke(INK, 2f, 200))
    }

    private fun tube(s: Scene) {
        var x = s.f(0f, s.w)
        var y = s.f(0f, s.h)
        var a = s.f(0f, TAU)
        val p = Path()
        p.moveTo(x, y)
        val pts = ArrayList<FloatArray>()
        repeat(s.i(30, 70)) {
            a += s.noise.at(x * 0.004f, y * 0.004f) * 0.5f + s.f(-0.2f, 0.2f)
            x += cos(a) * 12f
            y += sin(a) * 12f
            p.lineTo(x, y)
            pts.add(floatArrayOf(x, y, a))
        }
        val w = s.f(18f, 34f)
        s.canvas.drawPath(p, s.stroke(INK, w + 5f))
        s.canvas.drawPath(p, s.stroke(Color.BLACK, w))
        // Rings or suckers along the tube.
        val suckers = s.chance(0.5f)
        for ((k, pt) in pts.withIndex()) {
            if (k % 3 != 0) continue
            if (suckers) {
                s.canvas.drawCircle(pt[0], pt[1], w * 0.18f, s.stroke(INK, 1.6f))
            } else {
                val nx = -sin(pt[2]) * w / 2f
                val ny = cos(pt[2]) * w / 2f
                s.canvas.drawLine(pt[0] - nx, pt[1] - ny, pt[0] + nx, pt[1] + ny, s.stroke(INK, 1.6f))
            }
        }
    }

    // ---------- Characters ----------

    private fun character(s: Scene, x: Float, y: Float, r: Float) {
        s.unit(x, y, r, s.f(-20f, 20f)) {
            val p = Pen(s, INK, 2.4f / r, intArrayOf(Color.BLACK), darkInk = true)
            if (r < 45f) {
                // Small fry: icons from the doodle library.
                Doodles.icons[s.i(0, Doodles.count)](p)
            } else {
                when (s.i(0, 12)) {
                    0 -> cyclops(s, p)
                    1 -> grinMonster(p)
                    2 -> tvRobot(p)
                    3 -> skullGuy(p)
                    4 -> eyeballs(s, p)
                    5 -> ghost(p)
                    6 -> carton(p)
                    7 -> pill(p)
                    8 -> octoFace(p)
                    9 -> mushroomDude(p)
                    10 -> bubble(s, p)
                    else -> blobCat(p)
                }
            }
        }
    }

    private fun eye(p: Pen, x: Float, y: Float, r: Float, lookX: Float = 0.3f, lookY: Float = 0.2f) {
        p.circle(x, y, r, 0)
        val ix = x + lookX * r * 0.4f
        val iy = y + lookY * r * 0.4f
        p.circle(ix, iy, r * 0.5f)
        p.dot(ix, iy, r * 0.28f)
        p.blackDot(ix - r * 0.1f, iy - r * 0.1f, r * 0.09f)
    }

    private fun teeth(p: Pen, x0: Float, x1: Float, y: Float, h: Float, n: Int) {
        val step = (x1 - x0) / n
        val pts = FloatArray((n * 2 + 1) * 2)
        for (k in 0..n * 2) {
            pts[k * 2] = x0 + step * k / 2f
            pts[k * 2 + 1] = if (k % 2 == 0) y else y + h
        }
        p.poly(*pts, close = false)
    }

    private fun cyclops(s: Scene, p: Pen) {
        p.poly(-0.5f, -0.6f, -0.35f, -1.05f, -0.2f, -0.68f, fill = 0)
        p.poly(0.5f, -0.6f, 0.35f, -1.05f, 0.2f, -0.68f, fill = 0)
        // Wobbly blob: a smooth curve through the midpoints of jittered radial points.
        val n = 12
        val px = FloatArray(n)
        val py = FloatArray(n)
        for (k in 0 until n) {
            val a = k * TAU / n
            val rr = 0.85f + s.f(-0.1f, 0.1f)
            px[k] = cos(a) * rr
            py[k] = sin(a) * rr
        }
        val body = Path()
        body.moveTo((px[n - 1] + px[0]) / 2f, (py[n - 1] + py[0]) / 2f)
        for (k in 0 until n) {
            val j = (k + 1) % n
            body.quadTo(px[k], py[k], (px[k] + px[j]) / 2f, (py[k] + py[j]) / 2f)
        }
        body.close()
        p.path(body, 0)
        eye(p, 0f, -0.2f, 0.38f)
        val mouth = Path()
        mouth.moveTo(-0.5f, 0.28f)
        mouth.quadTo(0f, 0.85f, 0.5f, 0.28f)
        mouth.close()
        p.path(mouth, 0)
        teeth(p, -0.45f, 0.45f, 0.3f, 0.13f, 6)
        for ((fx, fy) in listOf(-0.6f to 0.05f, -0.52f to 0.15f, 0.58f to 0.08f)) p.dot(fx, fy, 0.03f)
    }

    private fun grinMonster(p: Pen) {
        p.circle(-0.88f, -0.1f, 0.18f, 0)
        p.circle(0.88f, -0.1f, 0.18f, 0)
        p.rrect(-0.85f, -0.78f, 0.85f, 0.82f, 0.35f, 0)
        eye(p, -0.35f, -0.32f, 0.22f)
        eye(p, 0.37f, -0.38f, 0.15f, -0.3f)
        p.rrect(-0.62f, 0.05f, 0.62f, 0.58f, 0.2f, 0)
        p.line(-0.62f, 0.31f, 0.62f, 0.31f)
        var x = -0.46f
        while (x < 0.5f) { p.line(x, 0.05f, x, 0.58f); x += 0.155f }
        p.line(-0.2f, -0.7f, 0.25f, -0.62f)
        for (k in 0 until 4) {
            val sx = -0.15f + k * 0.12f
            val sy = -0.69f + k * 0.02f
            p.line(sx, sy - 0.05f, sx, sy + 0.05f)
        }
    }

    private fun tvRobot(p: Pen) {
        p.line(-0.4f, 0.7f, -0.45f, 1.0f)
        p.line(0.4f, 0.7f, 0.45f, 1.0f)
        p.line(-0.2f, -0.65f, -0.45f, -1.0f)
        p.line(0.2f, -0.65f, 0.4f, -0.95f)
        p.circle(-0.47f, -1.02f, 0.07f, 0)
        p.circle(0.42f, -0.97f, 0.07f, 0)
        p.rrect(-0.92f, -0.65f, 0.92f, 0.72f, 0.14f, 0)
        val screen = Path()
        screen.addRoundRect(RectF(-0.72f, -0.48f, 0.35f, 0.52f), 0.12f, 0.12f, Path.Direction.CW)
        p.path(screen, 0)
        p.dot(-0.38f, -0.08f, 0.07f)
        p.dot(0.02f, -0.08f, 0.07f)
        p.arc(-0.38f, -0.05f, 0.02f, 0.3f, 20f, 140f)
        p.circle(0.63f, -0.3f, 0.12f, 0)
        p.line(0.63f, -0.3f, 0.7f, -0.38f)
        p.circle(0.63f, 0.05f, 0.12f, 0)
        var gy = 0.3f
        while (gy < 0.6f) { p.line(0.5f, gy, 0.78f, gy); gy += 0.08f }
    }

    private fun skullGuy(p: Pen) {
        p.rrect(-0.42f, 0.25f, 0.42f, 0.85f, 0.14f, 0)
        p.circle(0f, -0.2f, 0.78f, 0)
        p.circle(-0.3f, -0.25f, 0.22f, 0)
        p.circle(0.3f, -0.25f, 0.22f, 0)
        p.dot(-0.3f, -0.22f, 0.07f)
        p.dot(0.3f, -0.22f, 0.07f)
        p.poly(0f, 0.02f, -0.1f, 0.22f, 0.1f, 0.22f, fill = 0)
        for (tx in floatArrayOf(-0.21f, -0.07f, 0.07f, 0.21f)) p.line(tx, 0.5f, tx, 0.85f)
        p.line(-0.42f, 0.66f, 0.42f, 0.66f)
        p.poly(0.25f, -0.95f, 0.18f, -0.75f, 0.3f, -0.65f, 0.22f, -0.5f, close = false)
    }

    private fun eyeballs(s: Scene, p: Pen) {
        val spots = listOf(floatArrayOf(-0.3f, -0.25f, 0.5f), floatArrayOf(0.45f, -0.4f, 0.35f), floatArrayOf(0.35f, 0.42f, 0.42f), floatArrayOf(-0.5f, 0.5f, 0.3f))
        for (e in spots) {
            eye(p, e[0], e[1], e[2], s.f(-1f, 1f), s.f(-1f, 1f))
            repeat(3) {
                // Veins creep in from the rim.
                val a = s.f(0f, TAU)
                val vein = Path()
                vein.moveTo(e[0] + cos(a) * e[2], e[1] + sin(a) * e[2])
                vein.lineTo(e[0] + cos(a + 0.2f) * e[2] * 0.8f, e[1] + sin(a + 0.2f) * e[2] * 0.8f)
                vein.lineTo(e[0] + cos(a - 0.1f) * e[2] * 0.68f, e[1] + sin(a - 0.1f) * e[2] * 0.68f)
                p.path(vein)
            }
        }
    }

    private fun ghost(p: Pen) {
        val g = Path()
        g.moveTo(-0.72f, 0.85f)
        g.lineTo(-0.72f, -0.1f)
        g.arcTo(RectF(-0.72f, -0.85f, 0.72f, 0.62f), 180f, 180f, false)
        g.lineTo(0.72f, 0.85f)
        g.quadTo(0.6f, 0.6f, 0.48f, 0.85f)
        g.quadTo(0.36f, 0.6f, 0.24f, 0.85f)
        g.quadTo(0.12f, 0.6f, 0f, 0.85f)
        g.quadTo(-0.12f, 0.6f, -0.24f, 0.85f)
        g.quadTo(-0.36f, 0.6f, -0.48f, 0.85f)
        g.quadTo(-0.6f, 0.6f, -0.72f, 0.85f)
        g.close()
        p.path(g, 0)
        p.oval(-0.4f, -0.4f, -0.1f, 0f, 0)
        p.oval(0.1f, -0.4f, 0.4f, 0f, 0)
        p.dot(-0.22f, -0.16f, 0.08f)
        p.dot(0.28f, -0.16f, 0.08f)
        p.oval(-0.2f, 0.08f, 0.2f, 0.35f, 0)
        val tongue = Path()
        tongue.moveTo(-0.1f, 0.28f)
        tongue.quadTo(0f, 0.6f, 0.1f, 0.28f)
        p.path(tongue, 0)
    }

    private fun carton(p: Pen) {
        p.poly(-0.75f, -0.4f, -0.25f, -0.85f, 0.75f, -0.85f, 0.25f, -0.4f, fill = 0)
        val side = Path()
        side.moveTo(0.25f, -0.4f); side.lineTo(0.75f, -0.85f); side.lineTo(0.75f, 0.5f); side.lineTo(0.25f, 0.95f); side.close()
        p.path(side, 0)
        p.hatch(side, 0.07f, 30f)
        p.rrect(-0.75f, -0.4f, 0.25f, 0.95f, 0.02f, 0)
        p.dot(-0.45f, 0.05f, 0.06f)
        p.dot(-0.05f, 0.05f, 0.06f)
        p.arc(-0.45f, 0.05f, -0.05f, 0.4f, 20f, 140f)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textSize = 0.2f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        p.c.drawText("ZAMOLED", -0.25f, -0.15f, tp)
        p.line(-0.65f, 0.7f, 0.15f, 0.7f)
        p.line(-0.65f, 0.8f, 0.0f, 0.8f)
    }

    private fun pill(p: Pen) {
        p.c.save()
        p.c.rotate(-30f)
        val left = Path()
        left.addRoundRect(RectF(-0.95f, -0.42f, 0f, 0.42f), floatArrayOf(0.42f, 0.42f, 0f, 0f, 0f, 0f, 0.42f, 0.42f), Path.Direction.CW)
        val right = Path()
        right.addRoundRect(RectF(0f, -0.42f, 0.95f, 0.42f), floatArrayOf(0f, 0f, 0.42f, 0.42f, 0.42f, 0.42f, 0f, 0f), Path.Direction.CW)
        p.path(left, 0)
        p.hatch(left, 0.08f, 45f)
        p.path(right, 0)
        p.dot(0.32f, -0.08f, 0.06f)
        p.dot(0.62f, -0.08f, 0.06f)
        p.arc(0.32f, -0.05f, 0.62f, 0.22f, 20f, 140f)
        p.c.restore()
    }

    private fun octoFace(p: Pen) {
        for (k in 0 until 5) {
            val x = -0.6f + k * 0.3f
            val dir = if (k < 2) -1f else if (k == 2) 0f else 1f
            val t = Path()
            t.moveTo(x, 0.1f)
            t.cubicTo(x + dir * 0.1f, 0.6f, x + dir * 0.5f, 0.7f, x + dir * 0.35f, 0.95f)
            p.c.drawPath(t, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = INK; strokeWidth = 0.24f; strokeCap = Paint.Cap.ROUND })
            p.c.drawPath(t, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = 0.16f; strokeCap = Paint.Cap.ROUND })
        }
        val head = Path()
        head.moveTo(-0.75f, 0.25f)
        head.arcTo(RectF(-0.75f, -0.95f, 0.75f, 0.75f), 180f, 180f, false)
        head.close()
        p.path(head, 0)
        eye(p, -0.28f, -0.2f, 0.18f)
        eye(p, 0.28f, -0.2f, 0.18f)
        p.arc(-0.2f, -0.05f, 0.2f, 0.2f, 20f, 140f)
        for ((sx, sy) in listOf(-0.45f to -0.6f, 0.1f to -0.72f, 0.5f to -0.5f)) p.circle(sx, sy, 0.06f)
    }

    private fun mushroomDude(p: Pen) {
        p.rrect(-0.38f, -0.15f, 0.38f, 0.9f, 0.2f, 0)
        p.dot(-0.15f, 0.25f, 0.06f)
        p.dot(0.15f, 0.25f, 0.06f)
        p.arc(-0.15f, 0.3f, 0.15f, 0.55f, 20f, 140f)
        val cap = Path()
        cap.moveTo(-0.95f, 0f)
        cap.arcTo(RectF(-0.95f, -0.9f, 0.95f, 0.85f), 180f, 180f, false)
        cap.quadTo(0f, 0.15f, -0.95f, 0f)
        cap.close()
        p.path(cap, 0)
        for ((sx, sy, sr) in listOf(Triple(-0.45f, -0.35f, 0.14f), Triple(0.1f, -0.6f, 0.12f), Triple(0.55f, -0.3f, 0.11f), Triple(-0.05f, -0.2f, 0.07f))) {
            p.circle(sx, sy, sr)
        }
    }

    private fun bubble(s: Scene, p: Pen) {
        val b = Path()
        b.addOval(RectF(-1f, -0.6f, 1f, 0.6f), Path.Direction.CW)
        val tail = Path()
        tail.moveTo(-0.3f, 0.5f); tail.lineTo(-0.7f, 1f); tail.lineTo(0f, 0.55f); tail.close()
        b.op(tail, Path.Op.UNION)
        p.path(b, 0)
        val word = words[s.i(0, words.size)]
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            textSize = if (word.length > 5) 0.34f else 0.46f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
        }
        p.c.drawText(word, 0f, tp.textSize * 0.35f, tp)
    }

    private fun blobCat(p: Pen) {
        p.poly(-0.75f, -0.2f, -0.62f, -0.98f, -0.2f, -0.58f, fill = 0)
        p.poly(0.75f, -0.2f, 0.62f, -0.98f, 0.2f, -0.58f, fill = 0)
        p.oval(-0.85f, -0.65f, 0.85f, 0.85f, 0)
        eye(p, -0.32f, -0.1f, 0.2f)
        eye(p, 0.32f, -0.1f, 0.2f)
        p.poly(-0.08f, 0.15f, 0.08f, 0.15f, 0f, 0.26f, fill = 0)
        val mouth = Path()
        mouth.moveTo(-0.3f, 0.35f)
        mouth.quadTo(0f, 0.7f, 0.3f, 0.35f)
        mouth.close()
        p.path(mouth, 0)
        teeth(p, -0.25f, 0.25f, 0.37f, 0.1f, 4)
        for (sgn in floatArrayOf(-1f, 1f)) {
            p.line(sgn * 0.4f, 0.22f, sgn * 1.0f, 0.12f)
            p.line(sgn * 0.4f, 0.3f, sgn * 1.0f, 0.36f)
        }
    }
}
