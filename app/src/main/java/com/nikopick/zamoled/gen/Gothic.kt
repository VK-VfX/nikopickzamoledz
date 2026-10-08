package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

private fun Scene.fadeBottom(from: Float) {
    canvas.drawRect(0f, h * from, w, h + 2f, vGradient(h * from, h, Color.TRANSPARENT, Color.BLACK))
}

/** Front-facing bust framed by a huge moon. */
object GothMoonQueen : Style("goth_moon", "Moon Queen", Category.GOTHIC) {
    override val autoPalettes = listOf("violet", "sakura", "lava", "cyan", "gold", "hue")

    override fun draw(s: Scene) {
        val L = GothLook.make(s)
        val H = 520f
        val cx = s.cx
        val cy = s.h * 0.33f
        s.starSky(220, s.h * 0.75f)
        val mcy = cy - H * 0.04f
        val mr = H * 0.86f
        val moon = mix(Color.rgb(214, 208, 232), L.accent, 0.22f)
        s.softGlow(cx, mcy, mr * 2.3f, L.accent, 90)
        s.canvas.drawCircle(cx, mcy, mr, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(cx - mr * 0.3f, mcy - mr * 0.35f, mr * 1.6f, intArrayOf(darken(moon, 0.25f), darken(moon, 0.6f), darken(L.accent, 0.82f)), floatArrayOf(0f, 0.6f, 1f), android.graphics.Shader.TileMode.CLAMP)
        })
        s.canvas.save()
        val disc = Path(); disc.addCircle(cx, mcy, mr, Path.Direction.CW)
        s.canvas.clipPath(disc)
        repeat(12) { s.canvas.drawCircle(cx + s.f(-mr, mr), mcy + s.f(-mr, mr), s.f(10f, 46f), s.fill(darken(moon, 0.75f), 60)) }
        s.canvas.restore()
        s.canvas.drawCircle(cx, mcy, mr, s.stroke(lighten(L.accent, 0.35f), 3f, 200, 2f))
        for (k in 1..3) s.canvas.drawCircle(cx, mcy, mr * (1.08f + k * 0.07f), s.stroke(L.accent, 1.4f, 140 - k * 35))
        repeat(s.i(3, 6)) {
            GothProps.bat(s, s.f(60f, s.w - 60f), s.f(s.h * 0.08f, s.h * 0.55f), s.f(22f, 46f), Color.BLACK, s.f(-25f, 25f))
        }

        s.canvas.save()
        s.canvas.translate(cx, cy)
        s.canvas.scale(H, H)
        s.canvas.rotate(s.f(-4f, 4f))
        val long = s.f(1.0f, 1.8f)
        val veil = s.chance(0.3f)
        GothHair.back(s, L, s.f(1.0f, 1.25f), long, s.f(0f, 1f))
        GothFace.neck(s, L)
        s.canvas.save()
        s.canvas.translate(0f, -0.13f)
        GothDress.bust(s, L, 3.4f)
        GothDress.collar(s, L)
        GothDress.choker(s, L, s.chance(0.7f))
        s.canvas.restore()
        GothFace.face(s, L, s.i(0, 3))
        GothHair.curtains(s, L, s.f(-0.05f, 0.05f), s.chance(0.5f))
        if (s.chance(0.7f)) GothDress.earrings(s, L)
        when (s.i(0, 5)) {
            0 -> GothHead.roseCrown(s, L)
            1 -> GothHead.miniHat(s, L)
            2 -> GothHead.tiara(s, L)
            else -> {}
        }
        if (veil) GothHead.veil(s, L)
        s.canvas.restore()
        s.fadeBottom(0.72f)
    }
}

/** Profile portrait with a raven on her shoulder, in front of a stained-glass window. */
object GothRavenLady : Style("goth_raven", "Raven Lady", Category.GOTHIC) {
    override val autoPalettes = listOf("violet", "sunset", "cyan", "lava", "hue", "gold")

    override fun draw(s: Scene) {
        val L = GothLook.make(s)
        val H = 560f
        val cx = s.cx
        val cy = s.h * 0.36f
        s.starSky(120, s.h * 0.4f)
        // Cathedral window: pointed arch, lancets and a rose window, lit from behind.
        val aw = s.w * 0.44f
        val top = s.h * 0.06f
        val bottom = s.h * 0.78f
        val arch = GothProps.arch(cx, top, aw, bottom)
        s.softGlow(cx, cy, H * 2.2f, L.accent, 80)
        s.canvas.drawPath(arch, s.fill(darken(L.accent, 0.9f)))
        s.canvas.save()
        s.canvas.clipPath(arch)
        // Rose window.
        val rcx = cx
        val rcy = top + aw * 1.5f
        val panes = 12
        for (k in 0 until panes) {
            val a0 = k * 360f / panes
            val col = if (k % 2 == 0) L.accent else L.accent2
            val pane = Path()
            pane.moveTo(rcx, rcy)
            pane.arcTo(RectF(rcx - aw * 0.78f, rcy - aw * 0.78f, rcx + aw * 0.78f, rcy + aw * 0.78f), a0, 360f / panes, false)
            pane.close()
            s.canvas.drawPath(pane, s.fill(darken(col, 0.55f), 190))
        }
        for (k in 1..3) s.canvas.drawCircle(rcx, rcy, aw * 0.26f * k, s.stroke(Color.BLACK, 6f))
        for (k in 0 until panes) { val a = Math.toRadians((k * 360f / panes).toDouble()).toFloat(); s.canvas.drawLine(rcx, rcy, rcx + cos(a) * aw * 0.78f, rcy + sin(a) * aw * 0.78f, s.stroke(Color.BLACK, 5f)) }
        // Lancets below.
        val lw = aw * 2f / 3f
        for (i in 0 until 3) {
            val lx0 = cx - aw + i * lw
            val col = s.color(i)
            s.canvas.drawRect(lx0, rcy + aw * 0.85f, lx0 + lw, bottom, s.fill(darken(col, 0.6f), 150))
            var yy = rcy + aw * 0.85f
            while (yy < bottom) { s.canvas.drawLine(lx0, yy, lx0 + lw, yy, s.stroke(Color.BLACK, 4f)); yy += 120f }
            s.canvas.drawLine(lx0, rcy + aw * 0.85f, lx0, bottom, s.stroke(Color.BLACK, 8f))
        }
        s.canvas.restore()
        s.canvas.drawPath(arch, s.stroke(lighten(L.accent, 0.3f), 5f, 220))
        s.canvas.drawPath(arch, s.stroke(L.accent, 14f, 60, 14f))

        s.canvas.save()
        s.canvas.translate(cx - H * 0.12f, cy)
        s.canvas.scale(H, H)
        s.canvas.rotate(s.f(-3f, 3f))
        GothProfile.hairBack(s, L, s.f(0.4f, 1.0f))
        GothProfile.neck(s, L)
        s.canvas.save()
        s.canvas.translate(0f, -0.1f)
        GothProfile.bust(s, L, 3.4f)
        s.canvas.restore()
        GothProfile.face(s, L)
        GothProfile.hairCap(s, L)
        // Raven perched on the shoulder, beak toward her ear.
        GothRaven.draw(s, L, -0.02f, 1.02f, 0.5f, s.f(-6f, 6f))
        s.canvas.restore()
        s.fadeBottom(0.7f)
    }
}

/** A scatter of leaning gravestones and iron fence posts along the ground line. */
internal fun Scene.graveyard(groundY: Float, rim: Int, density: Float = 1f) {
    repeat(count((9 * density).toInt().coerceAtLeast(3))) {
        val x = f(-40f, w + 40f)
        val gh = f(50f, 120f)
        val gw = gh * f(0.5f, 0.7f)
        canvas.save()
        canvas.translate(x, groundY + f(0f, 80f))
        canvas.rotate(f(-8f, 8f))
        val p = Path()
        when (i(0, 3)) {
            0 -> { p.moveTo(-gw / 2f, 0f); p.lineTo(-gw / 2f, -gh * 0.7f); p.quadTo(-gw / 2f, -gh, 0f, -gh); p.quadTo(gw / 2f, -gh, gw / 2f, -gh * 0.7f); p.lineTo(gw / 2f, 0f); p.close() }
            1 -> { // cross
                p.moveTo(-gw * 0.12f, 0f); p.lineTo(-gw * 0.12f, -gh * 0.65f); p.lineTo(-gw * 0.4f, -gh * 0.65f); p.lineTo(-gw * 0.4f, -gh * 0.8f); p.lineTo(-gw * 0.12f, -gh * 0.8f)
                p.lineTo(-gw * 0.12f, -gh); p.lineTo(gw * 0.12f, -gh); p.lineTo(gw * 0.12f, -gh * 0.8f); p.lineTo(gw * 0.4f, -gh * 0.8f); p.lineTo(gw * 0.4f, -gh * 0.65f); p.lineTo(gw * 0.12f, -gh * 0.65f); p.lineTo(gw * 0.12f, 0f); p.close()
            }
            else -> { p.moveTo(-gw / 2f, 0f); p.lineTo(-gw / 2f, -gh * 0.8f); p.lineTo(0f, -gh); p.lineTo(gw / 2f, -gh * 0.8f); p.lineTo(gw / 2f, 0f); p.close() }
        }
        canvas.drawPath(p, fill(Color.BLACK))
        canvas.drawPath(p, stroke(rim, 2f, 170))
        canvas.restore()
    }
}

/** Gnarled leafless tree silhouette. */
internal fun Scene.deadTree(x: Float, baseY: Float, height: Float, rim: Int) {
    fun branch(bx: Float, by: Float, ang: Float, len: Float, wd: Float, depth: Int) {
        val ex = bx + cos(ang) * len
        val ey = by + sin(ang) * len
        val p = Path()
        p.moveTo(bx, by)
        p.quadTo((bx + ex) / 2f + f(-12f, 12f), (by + ey) / 2f + f(-8f, 8f), ex, ey)
        canvas.drawPath(p, stroke(Color.BLACK, wd))
        canvas.drawPath(p, stroke(rim, 1.2f, 150))
        if (depth > 0) {
            branch(ex, ey, ang + f(0.3f, 0.7f), len * f(0.65f, 0.78f), wd * 0.68f, depth - 1)
            branch(ex, ey, ang - f(0.3f, 0.7f), len * f(0.65f, 0.78f), wd * 0.68f, depth - 1)
            if (chance(0.3f)) branch(ex, ey, ang + f(-0.2f, 0.2f), len * 0.6f, wd * 0.6f, depth - 1)
        }
    }
    branch(x, baseY, -PI_F / 2f + f(-0.12f, 0.12f), height * 0.32f, height * 0.05f, 5)
}

internal const val PI_F = 3.14159265f

/** Gothic castle silhouette: towers, spires, buttresses and lit windows on a hill. */
internal fun Scene.castle(cx: Float, baseY: Float, scale: Float, rim: Int, glowColor: Int) {
    canvas.save()
    canvas.translate(cx, baseY)
    canvas.scale(scale, scale)
    val body = Path()
    val towers = listOf(-230f to 260f, -150f to 400f, -60f to 300f, 40f to 520f, 130f to 340f, 220f to 430f, 300f to 250f)
    for ((tx, th) in towers) {
        val tw = 34f + (th % 5f) * 3f
        body.addRect(tx - tw, -th, tx + tw, 20f, Path.Direction.CW)
        val spire = Path()
        spire.moveTo(tx - tw - 8f, -th); spire.lineTo(tx, -th - tw * 3.2f); spire.lineTo(tx + tw + 8f, -th); spire.close()
        body.addPath(spire)
        // Battlements on shorter towers.
    }
    body.addRect(-260f, -190f, 330f, 20f, Path.Direction.CW)
    canvas.drawPath(body, fill(Color.BLACK))
    canvas.drawPath(body, stroke(rim, 3f, 220))
    repeat(f(7f, 12f).toInt()) {
        val (tx, th) = towers[i(0, towers.size)]
        val wy = -th + f(40f, th - 60f)
        val win = Path()
        win.moveTo(tx - 6f, wy + 26f); win.lineTo(tx - 6f, wy + 8f); win.quadTo(tx, wy - 8f, tx + 6f, wy + 8f); win.lineTo(tx + 6f, wy + 26f); win.close()
        softGlow(tx, wy + 14f, 40f, glowColor, 110)
        canvas.drawPath(win, fill(lighten(glowColor, 0.4f)))
    }
    canvas.restore()
}

/** Low-lying fog bands. */
internal fun Scene.mist(y0: Float, y1: Float, color: Int, strength: Int) {
    repeat(6) {
        val y = f(y0, y1)
        val rx = f(w * 0.3f, w * 0.7f)
        val x = f(0f, w)
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(rx, f(22f, 60f))
        canvas.drawCircle(0f, 0f, 1f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(0f, 0f, 1f, intArrayOf(withAlpha(color, strength), Color.TRANSPARENT), null, android.graphics.Shader.TileMode.CLAMP)
        })
        canvas.restore()
    }
}

/** Back view of a hooded wanderer carrying a lantern toward a moonlit castle. */
object GothWanderer : Style("goth_wanderer", "Wanderer", Category.GOTHIC) {
    override val autoPalettes = listOf("violet", "cyan", "lava", "sunset", "hue", "aurora")

    override fun draw(s: Scene) {
        val L = GothLook.make(s)
        val moon = mix(Color.rgb(222, 218, 240), L.accent, 0.3f)
        s.starSky(260, s.h * 0.55f)
        val mx = s.w * s.f(0.3f, 0.7f)
        val my = s.h * s.f(0.2f, 0.28f)
        val mr = s.f(170f, 210f)
        s.softGlow(mx, my, mr * 3.4f, L.accent, 110)
        s.canvas.drawCircle(mx, my, mr, s.fill(darken(moon, 0.1f)))
        repeat(7) { s.canvas.drawCircle(mx + s.f(-mr * 0.6f, mr * 0.6f), my + s.f(-mr * 0.6f, mr * 0.6f), s.f(8f, 26f), s.fill(darken(moon, 0.3f), 90)) }
        repeat(s.i(4, 8)) { GothProps.bat(s, mx + s.f(-380f, 380f), my + s.f(-250f, 330f), s.f(16f, 38f), Color.BLACK, s.f(-30f, 30f)) }

        // Hill and castle on the horizon.
        val horizon = s.h * 0.58f
        val (land, line) = s.ridge(horizon + 90f, 150f, 0.003f, s.f(0f, 50f), ridged = false, step = 8f)
        s.canvas.drawPath(land, s.fill(Color.BLACK))
        s.canvas.drawPath(line, s.stroke(L.accent, 2.4f, 190))
        s.castle(s.w * s.f(0.55f, 0.78f), horizon + 40f, s.f(0.6f, 0.8f), L.accent, lighten(L.accent2, 0.2f))
        s.mist(horizon + 20f, horizon + 140f, L.accent, 60)

        // Winding path toward the castle, gravestones and dead trees at the sides.
        val foot = s.h * 0.9f
        val path = Path()
        path.moveTo(s.cx - 180f, s.h + 10f)
        path.cubicTo(s.cx - 150f, foot, s.cx - 20f, horizon + 300f, s.w * 0.62f, horizon + 100f)
        path.lineTo(s.w * 0.62f + 10f, horizon + 100f)
        path.cubicTo(s.cx + 40f, horizon + 300f, s.cx + 190f, foot, s.cx + 260f, s.h + 10f)
        path.close()
        s.canvas.drawPath(path, s.vGradient(horizon + 100f, s.h, withAlpha(L.accent, 20), withAlpha(L.accent, 70)))
        s.graveyard(foot - 60f, L.accent2, 1.2f)
        s.deadTree(s.w * 0.1f, foot - 40f, 520f, L.accent)
        if (s.chance(0.7f)) s.deadTree(s.w * 0.92f, foot - 60f, 420f, L.accent)

        // The wanderer, seen from behind.
        val u = 170f
        val fx = s.cx + s.f(-60f, 20f)
        val headY = s.h * 0.55f
        s.canvas.save()
        s.canvas.translate(fx, headY)
        s.canvas.scale(u, u)
        val c = s.canvas
        val cloak = Color.rgb(10, 8, 16)
        val rim = lighten(L.accent, 0.3f)
        val lantern = s.chance(0.75f)
        val side = if (s.chance(0.5f)) 1f else -1f

        // Cloak: shoulders to a wavy hem near the ground.
        val hem = 6.4f
        val cloakPath = smoothPath(
            floatArrayOf(
                0f, 0.62f, 0.55f, 0.72f, 0.88f, 0.95f, 1.0f, 1.6f, 1.15f, 2.8f, 1.5f, 4.5f, 1.85f, hem - 0.2f, 1.2f, hem + 0.05f, 0.7f, hem - 0.1f, 0.2f, hem + 0.08f,
                -0.4f, hem - 0.08f, -0.95f, hem + 0.06f, -1.7f, hem - 0.15f, -1.5f, 4.5f, -1.15f, 2.8f, -1.0f, 1.6f, -0.88f, 0.95f, -0.55f, 0.72f,
            ),
            closed = true, tension = 0.4f,
        )
        c.drawPath(cloakPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(-1.5f, 0f, 1.5f, 0f, lighten(L.cloth, 0.12f), cloak, Shader.TileMode.CLAMP)
        })
        c.save(); c.clipPath(cloakPath)
        // Deep vertical folds.
        for (k in -4..4) {
            val x0 = k * 0.2f
            c.drawPath(smoothPath(floatArrayOf(x0 * 0.5f, 1.0f, x0 * 1.0f, 2.8f, x0 * 1.7f, 4.6f, x0 * 2.1f, hem), closed = false), s.gLine(withAlpha(L.accent, 55), 0.012f))
        }
        s.softOval(-side * 1.2f, 3.5f, 0.35f, 2.8f, L.accent, 50)
        c.restore()
        c.drawPath(cloakPath, s.gLine(withAlpha(rim, 210), 0.014f))
        // Hem lantern light on the ground.
        s.softOval(0f, hem + 0.1f, 2.2f, 0.25f, L.accent, 70)

        // Arm holding the lantern, if any.
        if (lantern) {
            val arm = smoothPath(floatArrayOf(side * 0.85f, 1.1f, side * 1.5f, 1.7f, side * 1.9f, 2.4f, side * 1.75f, 2.55f, side * 1.3f, 1.9f, side * 0.9f, 1.55f), closed = true, tension = 0.4f)
            c.drawPath(arm, s.gFill(cloak)); c.drawPath(arm, s.gLine(withAlpha(rim, 200), 0.012f))
            val lx = side * 1.82f
            val ly = 2.95f
            c.drawLine(lx, 2.5f, lx, ly - 0.2f, s.gLine(Color.rgb(160, 150, 170), 0.014f))
            s.softOval(lx, ly + 0.1f, 1.1f, 1.1f, Color.rgb(255, 190, 90), 120)
            val lan = Path()
            lan.moveTo(lx - 0.13f, ly - 0.2f); lan.lineTo(lx + 0.13f, ly - 0.2f); lan.lineTo(lx + 0.17f, ly + 0.3f); lan.lineTo(lx - 0.17f, ly + 0.3f); lan.close()
            c.drawPath(lan, s.gFill(Color.rgb(255, 205, 120), 235))
            c.drawPath(lan, s.gLine(Color.rgb(30, 22, 20), 0.02f))
            c.drawLine(lx, ly - 0.2f, lx, ly + 0.3f, s.gLine(Color.rgb(30, 22, 20), 0.012f))
            c.drawPath(smoothPath(floatArrayOf(lx - 0.15f, ly - 0.22f, lx, ly - 0.34f, lx + 0.15f, ly - 0.22f), closed = false), s.gLine(Color.rgb(30, 22, 20), 0.02f))
        }

        // Hair streaming down the back, then the head with a hood or hat.
        val hair = smoothPath(
            floatArrayOf(0f, -0.6f, 0.4f, -0.5f, 0.52f, -0.05f, 0.56f, 0.6f, 0.68f, 1.6f, 0.62f, 2.7f, 0.4f, 3.3f, 0.12f, 3.0f, -0.1f, 3.5f, -0.4f, 3.1f, -0.64f, 2.6f, -0.62f, 1.6f, -0.56f, 0.6f, -0.52f, -0.05f, -0.4f, -0.5f),
            closed = true,
        )
        c.drawPath(hair, s.gFill(L.hair))
        c.save(); c.clipPath(hair)
        for (k in -4..4) c.drawPath(smoothPath(floatArrayOf(k * 0.07f, -0.5f, k * 0.12f, 0.6f, k * 0.15f + 0.04f, 1.8f, k * 0.14f, 3.2f), closed = false), s.gLine(L.hairHi, 0.012f, 90))
        s.softOval(-0.2f, 0.4f, 0.18f, 0.9f, L.hairHi, 70)
        c.restore()
        c.drawPath(hair, s.gLine(withAlpha(rim, 200), 0.012f))
        // Nape of the neck, then a hood draped behind or a tilted witch hat.
        if (s.chance(0.5f)) {
            val hood = smoothPath(floatArrayOf(0f, -0.72f, 0.46f, -0.55f, 0.62f, 0.05f, 0.82f, 0.5f, 0.35f, 0.72f, 0f, 0.62f, -0.35f, 0.72f, -0.82f, 0.5f, -0.62f, 0.05f, -0.46f, -0.55f), closed = true)
            c.drawPath(hood, s.gFill(cloak)); c.drawPath(hood, s.gLine(withAlpha(rim, 220), 0.012f))
            c.drawPath(smoothPath(floatArrayOf(0f, -0.7f, 0.02f, 0.0f, 0f, 0.6f), closed = false), s.gLine(withAlpha(L.accent, 120), 0.008f))
        } else {
            val brim = RectF(-0.95f, -0.62f, 0.95f, -0.3f)
            c.drawOval(brim, s.gFill(cloak)); c.drawOval(brim, s.gLine(withAlpha(rim, 220), 0.012f))
            val cone = smoothPath(floatArrayOf(-0.5f, -0.5f, -0.3f, -1.1f, 0.1f, -1.7f, 0.3f, -2.0f, 0.2f, -1.5f, 0.5f, -1.05f, 0.5f, -0.5f), closed = true, tension = 0.3f)
            c.drawPath(cone, s.gFill(cloak)); c.drawPath(cone, s.gLine(withAlpha(rim, 220), 0.012f))
            c.drawRect(-0.5f, -0.62f, 0.5f, -0.5f, s.gFill(L.accent, 210))
        }
        c.restore()
        s.mist(foot - 120f, s.h, L.accent, 55)
        s.fadeBottom(0.9f)
    }
}
