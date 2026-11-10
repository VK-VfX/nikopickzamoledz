package com.nikopick.zamoled.gen

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Your own words on black, with the one word that matters picked out. Type them in the editor;
 * left empty, the style picks a phrase. The key word is red unless the editor's toggle says white.
 */
internal val RED_WORD: Int = Color.rgb(229, 35, 47)

/** A phrase and the word(s) in it that carry the weight. */
private class Phrase(val text: String, val key: String)

/** What a style is about to set: the text and which of its words to highlight. */
private class Words(val text: String, val keys: Set<String>)

private object Lettering {
    val BOLD: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    val LIGHT: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    val SERIF: Typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)

    val PHRASES = listOf(
        Phrase("not yet, not never", "yet"),
        Phrase("be dangerous, be kind", "dangerous"),
        Phrase("start before you're ready", "start"),
        Phrase("choose the harder yes", "yes"),
        Phrase("everything you want is on the other side of fear", "fear"),
        Phrase("what if it works?", "works"),
        Phrase("the long way home", "home"),
        Phrase("be the reason", "reason"),
        Phrase("one more try", "try"),
    )

    fun clean(word: String): String = word.lowercase().filter { it.isLetterOrDigit() }

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

/** Typed words get their longest word picked out (the last one on a tie); a phrase uses its own key. */
private fun Scene.words(): Words {
    val typed = text.trim().take(80)
    if (typed.isEmpty()) {
        val p = Lettering.PHRASES[i(0, Lettering.PHRASES.size)]
        return Words(p.text, p.key.split(' ').map(Lettering::clean).toSet())
    }
    val tokens = typed.split(' ').filter { it.isNotBlank() }.map(Lettering::clean).filter { it.isNotEmpty() }
    val key = tokens.withIndex().maxWithOrNull(compareBy({ it.value.length }, { it.index }))?.value
    return Words(typed, setOfNotNull(key))
}

private fun Scene.hi(): Int = if (whiteWords) Color.WHITE else RED_WORD
private fun Scene.base(): Int = if (whiteWords) Color.rgb(125, 125, 125) else Color.WHITE

/** Draws [line] word by word so the key words can take their own colour. Returns each key word's box. */
private fun Scene.setLine(
    line: String, x: Float, y: Float, paint: Paint, align: Paint.Align, words: Words,
    hiColor: Int, baseColor: Int, alpha: Int = 255,
): List<RectF> {
    val parts = line.split(' ').filter { it.isNotEmpty() }
    val space = paint.measureText(" ")
    val widths = parts.map { paint.measureText(it) }
    val total = widths.sum() + space * (parts.size - 1)
    var cursor = when (align) {
        Paint.Align.LEFT -> x
        Paint.Align.RIGHT -> x - total
        else -> x - total / 2f
    }
    val saved = paint.textAlign
    paint.textAlign = Paint.Align.LEFT
    val fm = paint.fontMetrics
    val boxes = ArrayList<RectF>()
    for ((k, part) in parts.withIndex()) {
        val key = Lettering.clean(part) in words.keys
        paint.color = if (key) hiColor else baseColor
        paint.alpha = alpha
        canvas.drawText(part, cursor, y, paint)
        if (key) boxes += RectF(cursor, y + fm.ascent, cursor + widths[k], y + fm.descent)
        cursor += widths[k] + space
    }
    paint.textAlign = saved
    return boxes
}

private abstract class WordStyle(id: String, name: String) : Style(id, name, Category.WORDS) {
    override val usesText: Boolean get() = true
    override val usesDetail: Boolean get() = false
    override val usesPalette: Boolean get() = false

    // The words are set in the clear band between the launcher widgets and the app icons.
    override val widgetSafe: Boolean get() = true
}

/** Big condensed capitals stacked down the left, the key word in colour. */
private object WordsBold : WordStyle("words_bold", "Bold") {
    override fun draw(s: Scene) {
        val w = s.words()
        val margin = s.w * 0.1f
        val paint = s.textPaint(100f, Color.WHITE, Lettering.BOLD, Paint.Align.LEFT)
        val (size, lines) = Lettering.fit(w.text.uppercase(), paint, s.w - margin * 2f, 6, s.w * 0.34f, s.w * 0.07f)
        val lead = size * 1.0f
        var y = s.h * 0.4f - (lines.size - 1) * lead / 2f
        for (line in lines) {
            s.setLine(line, margin, y, paint, Paint.Align.LEFT, w, s.hi(), s.base())
            y += lead
        }
        s.canvas.drawLine(margin, y - lead * 0.55f, margin + s.w * 0.14f, y - lead * 0.55f, s.stroke(s.hi(), 4f))
    }
}

/** Small light type in the middle with wide spacing, between two hairlines. */
private object WordsQuiet : WordStyle("words_quiet", "Quiet") {
    override fun draw(s: Scene) {
        val w = s.words()
        val paint = s.textPaint(100f, Color.WHITE, Lettering.LIGHT).apply { letterSpacing = 0.32f }
        val (size, lines) = Lettering.fit(w.text.lowercase(), paint, s.w * 0.76f, 4, s.w * 0.07f, s.w * 0.035f)
        val lead = size * 1.7f
        var y = s.h * 0.4f - (lines.size - 1) * lead / 2f
        val top = y - lead
        for (line in lines) {
            s.setLine(line, s.cx, y, paint, Paint.Align.CENTER, w, s.hi(), s.base())
            y += lead
        }
        val rule = s.stroke(s.hi(), 1.8f, 220)
        s.canvas.drawLine(s.cx - 40f, top, s.cx + 40f, top, rule)
        s.canvas.drawLine(s.cx - 40f, y - lead * 0.45f, s.cx + 40f, y - lead * 0.45f, rule)
    }
}

/** An italic serif, centred, with an oversized opening quotation mark in colour. */
private object WordsQuote : WordStyle("words_quote", "Quote") {
    override fun draw(s: Scene) {
        val w = s.words()
        val paint = s.textPaint(100f, Color.WHITE, Lettering.SERIF)
        val (size, lines) = Lettering.fit(w.text, paint, s.w * 0.8f, 5, s.w * 0.15f, s.w * 0.05f)
        val lead = size * 1.25f
        var y = s.h * 0.4f - (lines.size - 1) * lead / 2f
        s.canvas.drawText("“", s.cx, y - lead * 0.7f, s.textPaint(size * 2.6f, s.hi(), Lettering.SERIF))
        for (line in lines) {
            s.setLine(line, s.cx, y, paint, Paint.Align.CENTER, w, s.hi(), s.base())
            y += lead
        }
    }
}

/** Outlined capitals: the words repeated as a hollow stroke, fading down the page; the key word stays lit. */
private object WordsEcho : WordStyle("words_echo", "Echo") {
    override fun draw(s: Scene) {
        val w = s.words()
        val paint = s.textPaint(100f, Color.WHITE, Lettering.BOLD)
        val (size, lines) = Lettering.fit(w.text.uppercase(), paint, s.w * 0.86f, 2, s.w * 0.3f, s.w * 0.08f)
        val block = lines.size * size
        var y = s.h * 0.3f
        var k = 0
        while (y < s.h * 0.88f && k < 7) {
            val p = Paint(paint)
            if (k > 0) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2.4f
            }
            val alpha = if (k == 0) 255 else (230 - k * 34).coerceAtLeast(30)
            val low = if (s.whiteWords) Color.rgb(125, 125, 125) else Color.WHITE
            for ((i, line) in lines.withIndex()) s.setLine(line, s.cx, y + i * size, p, Paint.Align.CENTER, w, s.hi(), low, alpha)
            y += block + size * 0.12f
            k++
        }
    }
}

/** Stacked capitals with the key word on a solid bar, like a highlighter or a stamped label. */
private object WordsMarker : WordStyle("words_marker", "Marker") {
    override fun draw(s: Scene) {
        val w = s.words()
        val paint = s.textPaint(100f, Color.WHITE, Lettering.BOLD)
        val (size, lines) = Lettering.fit(w.text.uppercase(), paint, s.w * 0.78f, 5, s.w * 0.2f, s.w * 0.06f)
        val lead = size * 1.22f
        var y = s.h * 0.4f - (lines.size - 1) * lead / 2f
        for (line in lines) {
            // Bars first, so the type sits on top of them.
            val boxes = s.setLine(line, s.cx, y, paint, Paint.Align.CENTER, w, Color.TRANSPARENT, Color.TRANSPARENT, 0)
            val pad = size * 0.12f
            for (b in boxes) s.canvas.drawRect(b.left - pad, b.top + size * 0.12f, b.right + pad, b.bottom, s.fill(s.hi()))
            s.setLine(line, s.cx, y, paint, Paint.Align.CENTER, w, Color.BLACK, s.base())
            y += lead
        }
    }
}

object WordsWorld {
    val styles: List<Style> = listOf(WordsBold, WordsQuiet, WordsQuote, WordsEcho, WordsMarker)
}
