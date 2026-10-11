package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Typographic wallpapers for the King of Kings world: words on black, nothing else.
 *
 * These are the strictest wallpapers in the app. No glows, no blurred haloes, no gradient
 * washes — anything that lifts a pixel off #000000 without being part of a letter is gone,
 * so the screen stays switched off everywhere the type is not.
 */
private object Type {
    val BOLD: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    val LIGHT: Typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)

    /** Break [text] into lines that each fit inside [maxWidth] at the paint's current size. */
    fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var line = StringBuilder()
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth || line.isEmpty()) {
                line = StringBuilder(candidate)
            } else {
                lines += line.toString()
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) lines += line.toString()
        return lines
    }

    /** The largest text size at which [text] wraps into at most [maxLines] lines inside [maxWidth]. */
    fun fit(text: String, paint: Paint, maxWidth: Float, maxLines: Int, start: Float, min: Float): Pair<Float, List<String>> {
        var size = start
        while (size > min) {
            paint.textSize = size
            val lines = wrap(text, paint, maxWidth)
            // A long single word can overrun the width even when the line count fits.
            if (lines.size <= maxLines && lines.all { paint.measureText(it) <= maxWidth }) return size to lines
            size -= start * 0.04f
        }
        paint.textSize = min
        return min to wrap(text, paint, maxWidth)
    }

    /** The square plate: a hairline border with a second rule inside it and ticks at the corners. */
    fun Scene.plate(cx: Float, cy: Float, half: Float, color: Int) {
        val outer = RectF(cx - half, cy - half, cx + half, cy + half)
        val inner = RectF(cx - half * 0.94f, cy - half * 0.94f, cx + half * 0.94f, cy + half * 0.94f)
        canvas.drawRect(outer, stroke(color, half * 0.012f))
        canvas.drawRect(inner, stroke(color, half * 0.004f, 150))
        val tick = half * 0.1f
        for (sx in floatArrayOf(-1f, 1f)) for (sy in floatArrayOf(-1f, 1f)) {
            val x = cx + sx * half
            val y = cy + sy * half
            canvas.drawLine(x - sx * tick, y, x + sx * tick * 0.35f, y, stroke(color, half * 0.022f))
            canvas.drawLine(x, y - sy * tick, x, y + sy * tick * 0.35f, stroke(color, half * 0.022f))
        }
    }

    /** Flat condensed caps. One pass, no halo: the letters are the only lit pixels. */
    fun Scene.flatText(text: String, x: Float, baseline: Float, size: Float, color: Int, spacing: Float, face: Typeface = BOLD, align: Paint.Align = Paint.Align.CENTER) {
        canvas.drawText(text, x, baseline, textPaint(size, color, face, align).apply { letterSpacing = spacing })
    }

    /** Words Christ speaks in the Gospels, with the book they come from. */
    val VERSES: List<Pair<String, String>> = listOf(
        "I AM THE LIGHT OF THE WORLD" to "JOHN 8:12",
        "I AM THE WAY AND THE TRUTH AND THE LIFE" to "JOHN 14:6",
        "I AM THE RESURRECTION AND THE LIFE" to "JOHN 11:25",
        "I AM THE GOOD SHEPHERD" to "JOHN 10:11",
        "I AM THE BREAD OF LIFE" to "JOHN 6:35",
        "I AM THE TRUE VINE" to "JOHN 15:1",
        "I AM THE DOOR" to "JOHN 10:9",
        "THE TRUTH WILL SET YOU FREE" to "JOHN 8:32",
        "PEACE I LEAVE WITH YOU" to "JOHN 14:27",
        "IT IS FINISHED" to "JOHN 19:30",
        "LET NOT YOUR HEART BE TROUBLED" to "JOHN 14:1",
        "I AM WITH YOU ALWAYS" to "MATTHEW 28:20",
        "COME FOLLOW ME" to "MATTHEW 4:19",
        "TAKE HEART IT IS I DO NOT BE AFRAID" to "MATTHEW 14:27",
        "LET YOUR LIGHT SHINE BEFORE OTHERS" to "MATTHEW 5:16",
        "BLESSED ARE THE PURE IN HEART" to "MATTHEW 5:8",
        "COME TO ME ALL WHO ARE WEARY" to "MATTHEW 11:28",
        "WITH GOD ALL THINGS ARE POSSIBLE" to "MATTHEW 19:26",
        "YOUR FAITH HAS MADE YOU WELL" to "MARK 5:34",
        "DO NOT FEAR ONLY BELIEVE" to "MARK 5:36",
        "ALL THINGS ARE POSSIBLE FOR ONE WHO BELIEVES" to "MARK 9:23",
        "THE KINGDOM OF GOD IS IN THE MIDST OF YOU" to "LUKE 17:21",
        "FATHER FORGIVE THEM" to "LUKE 23:34",
        "WHY ARE YOU AFRAID WHERE IS YOUR FAITH" to "LUKE 8:25",
    )
}

/** The name alone: KING OF KINGS, condensed, squared in the middle of the black. */
object TheName : Style("kok_name", "The Name", Category.KINGDOM) {
    override val autoPalettes = listOf("mono", "sacred", "divine", "gold", "violet")

    override fun draw(s: Scene) {
        val rule = s.color(0)
        val ink = Color.WHITE
        val half = s.w * 0.37f
        val cy = s.h * 0.46f

        Type.run {
            s.plate(s.cx, cy, half, rule)

            // KING / OF / KINGS, sized so the long word just clears the plate.
            val inner = half * 1.44f
            val probe = s.textPaint(100f, ink, Type.BOLD).apply { letterSpacing = 0.1f }
            val big = 100f * inner / probe.measureText("KINGS")
            val small = big * 0.3f

            s.flatText("KING", s.cx, cy - big * 0.42f, big, ink, 0.1f)
            s.flatText("KINGS", s.cx, cy + big * 1.0f, big, ink, 0.1f)

            // The small OF, with a hairline rule running out to each side of it.
            val ofY = cy + big * 0.26f
            s.flatText("OF", s.cx, ofY, small, ink, 0.42f, Type.LIGHT)
            val gap = small * 1.5f
            for (sgn in floatArrayOf(-1f, 1f)) {
                s.canvas.drawLine(s.cx + sgn * gap, ofY - small * 0.3f, s.cx + sgn * inner * 0.5f, ofY - small * 0.3f, s.stroke(rule, 2f))
            }
        }
    }
}

/** Red letters: the words of Christ set large across the whole screen, and nothing else. */
object RedLetters : Style("kok_redletters", "Red Letters", Category.KINGDOM) {
    override val autoPalettes = listOf("lava", "mono", "sacred", "gold", "divine")

    override fun draw(s: Scene) {
        val (verse, ref) = Type.VERSES[s.i(0, Type.VERSES.size)]
        val margin = s.w * 0.11f
        val ink = Color.WHITE
        // The lit line: crimson by default, or the palette's own colour at full strength.
        val accent = if (s.chance(0.55f)) Color.rgb(214, 36, 48) else s.color(0)

        val probe = s.textPaint(100f, ink, Type.BOLD).apply { letterSpacing = 0.02f }
        val (size, lines) = Type.fit(verse, probe, s.w - margin * 2f, 5, s.w * 0.3f, s.w * 0.08f)
        val lead = size * 1.02f
        // Centre the verse and its reference as one block so neither runs off the screen.
        val groupH = (lines.size - 1) * lead + size * 1.75f
        val top = (s.h - groupH) * 0.5f + size * 0.78f

        // One line carries the accent colour.
        val lit = s.i(0, lines.size)
        var y = top
        for ((k, line) in lines.withIndex()) {
            Type.run { s.flatText(line, margin, y, size, if (k == lit) accent else ink, 0.02f, Type.BOLD, Paint.Align.LEFT) }
            y += lead
        }

        // Reference under a short rule, below the last line.
        val baseY = top + (lines.size - 1) * lead + size * 0.55f
        s.canvas.drawLine(margin, baseY, margin + s.w * 0.14f, baseY, s.stroke(accent, 3f))
        Type.run { s.flatText(ref, margin, baseY + size * 0.4f, s.w * 0.035f, ink, 0.5f, Type.LIGHT, Paint.Align.LEFT) }
    }
}
