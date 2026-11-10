package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/** Your own words on black. Type them in the editor; left empty, the style picks a phrase. */
private object Lettering {
    val BOLD: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    val LIGHT: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    val SERIF: Typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)

    val PHRASES = listOf(
        "stay curious", "less but better", "make it count", "breathe", "one thing at a time",
        "do it anyway", "slow is smooth", "begin again", "trust the process", "be here now", "keep going",
    )

    fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = ArrayList<String>()
        var line = ""
        for (word in text.split(' ').filter { it.isNotBlank() }) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (line.isEmpty() || paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                lines += line
                line = word
            }
        }
        if (line.isNotEmpty()) lines += line
        return lines
    }

    /** The largest size at which [text] fits [maxWidth] in at most [maxLines] lines. */
    fun fit(text: String, paint: Paint, maxWidth: Float, maxLines: Int, start: Float, min: Float): Pair<Float, List<String>> {
        var size = start
        while (size > min) {
            paint.textSize = size
            val lines = wrap(text, paint, maxWidth)
            if (lines.size <= maxLines && lines.all { paint.measureText(it) <= maxWidth }) return size to lines
            size *= 0.94f
        }
        paint.textSize = min
        return min to wrap(text, paint, maxWidth)
    }
}

private fun Scene.words(): String = text.trim().take(80).ifEmpty { Lettering.PHRASES[i(0, Lettering.PHRASES.size)] }

private abstract class WordStyle(id: String, name: String) : Style(id, name, Category.WORDS) {
    override val usesText: Boolean get() = true
    override val usesDetail: Boolean get() = false
    override val autoPalettes = listOf("mono", "gold", "lava", "ocean", "violet", "sakura", "aurora")
}

/** Big condensed capitals stacked down the left, one line in colour. */
private object WordsBold : WordStyle("words_bold", "Bold") {
    override fun draw(s: Scene) {
        val text = s.words().uppercase()
        val margin = s.w * 0.1f
        val paint = s.textPaint(100f, Color.WHITE, Lettering.BOLD, Paint.Align.LEFT)
        val (size, lines) = Lettering.fit(text, paint, s.w - margin * 2f, 6, s.w * 0.34f, s.w * 0.07f)
        val lead = size * 1.0f
        var y = s.h * 0.56f - (lines.size - 1) * lead / 2f
        val lit = s.i(0, lines.size)
        for ((k, line) in lines.withIndex()) {
            paint.color = if (k == lit) s.color(0) else Color.WHITE
            s.canvas.drawText(line, margin, y, paint)
            y += lead
        }
        s.canvas.drawLine(margin, y - lead * 0.55f, margin + s.w * 0.14f, y - lead * 0.55f, s.stroke(s.color(0), 4f))
    }
}

/** Small light type in the middle with wide spacing, between two hairlines. */
private object WordsQuiet : WordStyle("words_quiet", "Quiet") {
    override fun draw(s: Scene) {
        val text = s.words().lowercase()
        val paint = s.textPaint(100f, Color.WHITE, Lettering.LIGHT).apply { letterSpacing = 0.32f }
        val (size, lines) = Lettering.fit(text, paint, s.w * 0.76f, 4, s.w * 0.07f, s.w * 0.035f)
        val lead = size * 1.7f
        var y = s.h * 0.58f - (lines.size - 1) * lead / 2f
        val top = y - lead
        for (line in lines) {
            s.canvas.drawText(line, s.cx, y, paint)
            y += lead
        }
        val rule = s.stroke(s.color(0), 1.6f, 200)
        s.canvas.drawLine(s.cx - 40f, top, s.cx + 40f, top, rule)
        s.canvas.drawLine(s.cx - 40f, y - lead * 0.45f, s.cx + 40f, y - lead * 0.45f, rule)
    }
}

/** An italic serif, centred, with an oversized opening quotation mark in colour. */
private object WordsQuote : WordStyle("words_quote", "Quote") {
    override fun draw(s: Scene) {
        val text = s.words()
        val paint = s.textPaint(100f, Color.WHITE, Lettering.SERIF)
        val (size, lines) = Lettering.fit(text, paint, s.w * 0.8f, 5, s.w * 0.15f, s.w * 0.05f)
        val lead = size * 1.25f
        var y = s.h * 0.58f - (lines.size - 1) * lead / 2f
        s.canvas.drawText("“", s.cx, y - lead * 0.7f, s.textPaint(size * 2.6f, s.color(0), Lettering.SERIF))
        for (line in lines) {
            s.canvas.drawText(line, s.cx, y, paint)
            y += lead
        }
    }
}

/** Outlined capitals: the words as a hollow stroke, repeated and fading down the page. */
private object WordsEcho : WordStyle("words_echo", "Echo") {
    override fun draw(s: Scene) {
        val text = s.words().uppercase()
        val paint = s.textPaint(100f, Color.WHITE, Lettering.BOLD)
        val (size, lines) = Lettering.fit(text, paint, s.w * 0.86f, 2, s.w * 0.3f, s.w * 0.08f)
        val block = lines.size * size
        var y = s.h * 0.34f
        var k = 0
        while (y < s.h * 0.95f && k < 7) {
            val p = Paint(paint)
            if (k == 0) {
                p.color = s.color(0)
            } else {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2.4f
                p.color = Color.WHITE
                p.alpha = (230 - k * 34).coerceAtLeast(30)
            }
            for ((i, line) in lines.withIndex()) s.canvas.drawText(line, s.cx, y + i * size, p)
            y += block + size * 0.12f
            k++
        }
    }
}

object WordsWorld {
    val styles: List<Style> = listOf(WordsBold, WordsQuiet, WordsQuote, WordsEcho)
}
