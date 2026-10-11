package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Typographic wallpapers for the King of Kings world: words on black, nothing else.
 * Everything is set in a condensed sans and centred inside a square plate.
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
        canvas.drawRect(outer, stroke(color, half * 0.012f, 225))
        canvas.drawRect(inner, stroke(color, half * 0.004f, 120))
        val tick = half * 0.1f
        for (sx in floatArrayOf(-1f, 1f)) for (sy in floatArrayOf(-1f, 1f)) {
            val x = cx + sx * half
            val y = cy + sy * half
            canvas.drawLine(x - sx * tick, y, x + sx * tick * 0.35f, y, stroke(color, half * 0.02f, 255))
            canvas.drawLine(x, y - sy * tick, x, y + sy * tick * 0.35f, stroke(color, half * 0.02f, 255))
        }
    }

    /** Condensed caps with a soft halo behind them, drawn centred on [cx]. */
    fun Scene.plateText(text: String, cx: Float, baseline: Float, size: Float, color: Int, spacing: Float, face: Typeface = BOLD, alpha: Int = 255) {
        val p = textPaint(size, color, face).apply { letterSpacing = spacing; this.alpha = alpha }
        canvas.drawText(text, cx, baseline, Paint(p).apply {
            this.alpha = (alpha * 0.32f).toInt()
            maskFilter = android.graphics.BlurMaskFilter(size * 0.3f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        })
        canvas.drawText(text, cx, baseline, p)
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
    override val autoPalettes = listOf("sacred", "divine", "gold", "mono", "violet")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val ink = mix(Color.WHITE, lighten(glow, 0.4f), 0.45f)
        val half = s.w * 0.37f
        val cy = s.h * 0.46f
        s.starSky(60, s.h, 2)
        s.softGlow(s.cx, cy, half * 2.1f, glow, 48)

        Type.run {
            s.plate(s.cx, cy, half, lighten(glow, 0.25f))

            // KING / OF / KINGS, sized so the long word just clears the plate.
            val inner = half * 1.44f
            val probe = s.textPaint(100f, ink, Type.BOLD).apply { letterSpacing = 0.1f }
            val big = 100f * inner / probe.measureText("KINGS")
            val small = big * 0.3f

            s.plateText("KING", s.cx, cy - big * 0.42f, big, ink, 0.1f)
            s.plateText("KINGS", s.cx, cy + big * 1.0f, big, ink, 0.1f)

            // The small OF, with a hairline rule running out to each side of it.
            val ofY = cy + big * 0.26f
            s.plateText("OF", s.cx, ofY, small, ink, 0.42f, Type.LIGHT, 215)
            val gap = small * 1.5f
            for (sgn in floatArrayOf(-1f, 1f)) {
                s.canvas.drawLine(s.cx + sgn * gap, ofY - small * 0.3f, s.cx + sgn * inner * 0.5f, ofY - small * 0.3f, s.stroke(lighten(glow, 0.3f), 2f, 150))
            }
        }
        Sacred.run { s.motes(18, glow) }
    }
}

/** A word of Christ from the Gospels, set in the same square plate. */
object GospelPlate : Style("kok_verse", "Gospel Plate", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "divine", "gold", "mono", "violet")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val ink = mix(Color.WHITE, lighten(glow, 0.4f), 0.4f)
        val (verse, ref) = Type.VERSES[s.i(0, Type.VERSES.size)]
        val half = s.w * 0.37f
        val cy = s.h * 0.46f
        s.starSky(60, s.h, 2)
        s.softGlow(s.cx, cy, half * 2.1f, glow, 46)

        Type.run {
            s.plate(s.cx, cy, half, lighten(glow, 0.25f))
            val inner = half * 1.46f
            val probe = s.textPaint(100f, ink, Type.BOLD).apply { letterSpacing = 0.06f }
            val (fitted, lines) = Type.fit(verse, probe, inner, 4, half * 0.42f, half * 0.12f)
            // The verse lives in the upper part of the plate; the reference owns the rest.
            val textRoom = half * 1.2f
            val size = minOf(fitted, textRoom / (lines.size * 1.12f))
            val lead = size * 1.12f
            val block = lines.size * lead
            var y = cy - half * 0.8f + (textRoom - block) * 0.5f + size * 0.82f
            for (line in lines) {
                s.plateText(line, s.cx, y, size, ink, 0.06f)
                y += lead
            }
            // A rule, then the reference in small spaced caps.
            val refY = cy + half * 0.68f
            s.canvas.drawLine(s.cx - inner * 0.16f, refY - half * 0.19f, s.cx + inner * 0.16f, refY - half * 0.19f, s.stroke(lighten(glow, 0.3f), 2.2f, 190))
            s.plateText(ref, s.cx, refY, half * 0.11f, lighten(glow, 0.35f), 0.5f, Type.LIGHT, 230)
        }
        Sacred.run { s.motes(18, glow) }
    }
}

/** Red letters: the words of Christ set large across the whole screen. */
object RedLetters : Style("kok_redletters", "Red Letters", Category.KINGDOM) {
    override val autoPalettes = listOf("sacred", "lava", "divine", "mono", "gold")

    override fun draw(s: Scene) {
        val glow = s.color(0)
        val (verse, ref) = Type.VERSES[s.i(0, Type.VERSES.size)]
        val margin = s.w * 0.11f
        val ink = mix(Color.WHITE, lighten(glow, 0.35f), 0.3f)
        val accent = if (s.chance(0.5f)) Sacred.CRIMSON else lighten(glow, 0.2f)
        s.starSky(70, s.h, 3)

        val probe = s.textPaint(100f, ink, Type.BOLD).apply { letterSpacing = 0.02f }
        val (size, lines) = Type.fit(verse, probe, s.w - margin * 2f, 5, s.w * 0.3f, s.w * 0.08f)
        val lead = size * 1.02f
        // Centre the verse and its reference as one block so neither runs off the screen.
        val groupH = (lines.size - 1) * lead + size * 1.75f
        val top = (s.h - groupH) * 0.5f + size * 0.78f

        // The quotation mark behind the words.
        s.canvas.drawText("“", margin * 0.3f, top - size * 0.1f, s.textPaint(size * 2.4f, withAlpha(glow, 42), Type.BOLD, Paint.Align.LEFT))
        s.softGlow(s.cx, s.h * 0.5f, s.w * 0.9f, glow, 42)

        // One line carries the accent colour.
        val lit = s.i(0, lines.size)
        var y = top
        for ((k, line) in lines.withIndex()) {
            val color = if (k == lit) accent else ink
            val p = s.textPaint(size, color, Type.BOLD, Paint.Align.LEFT).apply { letterSpacing = 0.02f }
            s.canvas.drawText(line, margin, y, Paint(p).apply {
                alpha = 90
                maskFilter = android.graphics.BlurMaskFilter(size * 0.26f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            })
            s.canvas.drawText(line, margin, y, p)
            y += lead
        }

        // Reference under a short rule, below the last line.
        val baseY = top + (lines.size - 1) * lead + size * 0.55f
        s.canvas.drawLine(margin, baseY, margin + s.w * 0.14f, baseY, s.stroke(lighten(glow, 0.3f), 2.4f, 200))
        s.canvas.drawText(
            ref, margin, baseY + size * 0.4f,
            s.textPaint(s.w * 0.035f, lighten(glow, 0.35f), Type.LIGHT, Paint.Align.LEFT).apply { letterSpacing = 0.5f },
        )

        // A thin bar of light down one edge.
        val edge = Path()
        edge.moveTo(margin * 0.42f, top - size)
        edge.lineTo(margin * 0.42f, y - lead + size * 0.3f)
        s.canvas.drawPath(edge, s.stroke(withAlpha(lighten(glow, 0.3f), 150), 3f, 150, 10f))
        Sacred.run { s.motes(20, glow) }
    }
}
