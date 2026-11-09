package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.DiscretePathEffect
import android.graphics.Paint
import kotlin.math.sqrt

/**
 * The hand-made Doodle world. The old doodles were thin vector icons; these are the same icons
 * put on the page the way a person would: painted, cut out and stuck down, or chalked.
 */

/** Gouache and ink outline only: the painted look on its own. */
fun Doodles.painted(s: Scene, index: Int, x: Float, y: Float, size: Float, rot: Float, fills: IntArray, ink: Int) {
    s.unit(x, y, size, rot) {
        icons[index % icons.size](Pen(s, ink, 3.6f / size, fills, shaded = true, look = PenLook.PAINTED))
    }
}

/** One sticker: a soft shadow, the white die-cut border, then the painted doodle on top. */
fun Doodles.sticker(s: Scene, index: Int, x: Float, y: Float, size: Float, rot: Float, fills: IntArray, ink: Int) {
    val w = 3.4f / size
    s.unit(x, y, size, rot) {
        val icon = icons[index % icons.size]
        icon(Pen(s, ink, w, fills, look = PenLook.SHADOW))
        icon(Pen(s, ink, w, fills, look = PenLook.DIECUT))
        icon(Pen(s, ink, w, fills, shaded = true, look = PenLook.PAINTED))
    }
}

/** Chalk on a blackboard: broken white strokes with coloured hatching. */
fun Doodles.chalked(s: Scene, index: Int, x: Float, y: Float, size: Float, rot: Float, fills: IntArray, ink: Int) {
    s.unit(x, y, size, rot) {
        icons[index % icons.size](Pen(s, ink, 3.2f / size, fills, look = PenLook.CHALK))
    }
}

private val STICKER_INK = Color.rgb(30, 22, 42)
private val CREAM = Color.rgb(246, 238, 222)

/** A blob of paint that ran off the brush. */
private fun Scene.dab(x: Float, y: Float, color: Int) {
    val r = f(3f, 9f)
    canvas.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        pathEffect = DiscretePathEffect(r * 0.9f, r * 0.35f)
    })
}

/** Die-cut stickers slapped over one another until the wall is covered. */
object StickerBomb : Style("sticker_bomb", "Sticker Bomb", Category.DOODLE) {
    override val autoPalettes = listOf("candy", "candy", "sakura", "sunset", "ocean", "aurora", "graffiti")

    override fun draw(s: Scene) {
        val colors = s.palette.colors
        // A shuffled order means overlaps differ from sticker to sticker, as they do on a real wall.
        for (p in s.spots(s.layout, 128f).shuffled(s.rnd)) {
            val shift = s.i(0, colors.size)
            val fills = IntArray(colors.size + 2) { k -> if (k < colors.size) colors[(k + shift) % colors.size] else if (k == colors.size) CREAM else Color.rgb(255, 214, 120) }
            Doodles.sticker(s, s.i(0, Doodles.count), p.x, p.y, s.f(58f, 92f) * p.scale, p.rot + s.f(-14f, 14f), fills, STICKER_INK)
        }
    }
}

/** Loose gouache: big painted shapes, cream outlines, specks of spilled paint. */
object Gouache : Style("gouache", "Gouache", Category.DOODLE) {
    override val autoPalettes = listOf("sunset", "candy", "sakura", "aurora", "ocean", "gold")

    override fun draw(s: Scene) {
        val colors = s.palette.colors
        val cell = 190f / sqrt(s.density)
        for (p in s.spots(s.layout, 190f)) {
            if (s.chance(0.82f)) {
                val shift = s.i(0, colors.size)
                val fills = IntArray(colors.size + 1) { k -> if (k < colors.size) colors[(k + shift) % colors.size] else CREAM }
                Doodles.painted(s, s.i(0, Doodles.count), p.x, p.y, s.f(66f, 108f) * p.scale, p.rot, fills, CREAM)
            }
            repeat(3) { s.dab(p.x + s.f(-cell / 2f, cell / 2f), p.y + s.f(-cell / 2f, cell / 2f), colors[s.i(0, colors.size)]) }
        }
    }
}

/** Chalk drawings: a wall of small chalked doodles with coloured shading. */
object Chalk : Style("chalk", "Chalk", Category.DOODLE) {
    override val autoPalettes = listOf("mono", "candy", "sakura", "ocean", "aurora", "sunset")

    override fun draw(s: Scene) {
        val colors = s.palette.colors
        for (p in s.spots(s.layout, 132f)) {
            val ink = if (s.chance(0.7f)) Color.rgb(244, 242, 236) else lighten(colors[s.i(0, colors.size)], 0.35f)
            val fills = IntArray(colors.size) { colors[(it + s.i(0, colors.size)) % colors.size] }
            Doodles.chalked(s, s.i(0, Doodles.count), p.x, p.y, s.f(46f, 70f) * p.scale, p.rot, fills, ink)
        }
    }
}
