package com.nikopick.zamoled.gen

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

// Shared painting helpers for the illustrated worlds.

fun mix(a: Int, b: Int, t: Float): Int = ColorUtils.blendARGB(a, b, t.coerceIn(0f, 1f))

fun darken(c: Int, t: Float): Int = mix(c, Color.BLACK, t)

fun lighten(c: Int, t: Float): Int = mix(c, Color.WHITE, t)

/** A soft radial pool of light. */
fun Scene.softGlow(x: Float, y: Float, r: Float, color: Int, alpha: Int = 150) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            x, y, r,
            intArrayOf(withAlpha(color, alpha), withAlpha(color, alpha / 3), Color.TRANSPARENT),
            floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP,
        )
    }
    canvas.drawCircle(x, y, r, p)
}

/** Fill paint with a vertical gradient between two colours. */
fun Scene.vGradient(top: Float, bottom: Float, c1: Int, c2: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    shader = LinearGradient(0f, top, 0f, bottom, c1, c2, Shader.TileMode.CLAMP)
}

/** Stars scattered above [maxY], a few of them twinkling with a cross flare. */
fun Scene.starSky(n: Int, maxY: Float = h, twinkles: Int = 6) {
    repeat(count(n)) {
        val r = 0.4f + f() * f() * 1.6f
        dot(f(0f, w), f(0f, maxY), r, if (chance(0.85f)) Color.WHITE else anyColor(), i(40, 220))
    }
    repeat(twinkles) {
        val x = f(0f, w)
        val y = f(0f, maxY)
        val len = f(8f, 18f)
        val p = stroke(Color.WHITE, 1f, 200)
        canvas.drawLine(x - len, y, x + len, y, p)
        canvas.drawLine(x, y - len, x, y + len, p)
        glowDot(x, y, 1.8f, Color.WHITE, 6f)
    }
}

/** A noise ridge line; returns the closed land shape and the open ridge line. */
fun Scene.ridge(baseY: Float, amp: Float, freq: Float, off: Float, ridged: Boolean = true, step: Float = 6f): Pair<Path, Path> {
    val land = Path()
    val line = Path()
    land.moveTo(-10f, h + 10f)
    var x = -10f
    var first = true
    while (x <= w + 10f) {
        val n = noise.fbm(x * freq, off)
        val y = baseY - (if (ridged) (1f - abs(n)) else (n + 1f) / 2f) * amp
        land.lineTo(x, y)
        if (first) line.moveTo(x, y) else line.lineTo(x, y)
        first = false
        x += step
    }
    land.lineTo(w + 10f, h + 10f)
    land.close()
    return land to line
}

fun Scene.textPaint(size: Float, color: Int, typeface: Typeface = Typeface.DEFAULT_BOLD, align: Paint.Align = Paint.Align.CENTER): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        this.typeface = typeface
        textAlign = align
    }

/** Neon text: blurred halo passes under a bright core. */
fun Scene.neonText(text: String, x: Float, y: Float, size: Float, color: Int, typeface: Typeface, flicker: Boolean = false) {
    val base = textPaint(size, color, typeface)
    val glow = Paint(base).apply {
        style = Paint.Style.STROKE
        strokeWidth = size * 0.08f
        strokeJoin = Paint.Join.ROUND
        maskFilter = BlurMaskFilter(size * 0.18f, BlurMaskFilter.Blur.NORMAL)
        alpha = if (flicker) 90 else 200
    }
    canvas.drawText(text, x, y, glow)
    val core = Paint(base).apply {
        style = Paint.Style.STROKE
        strokeWidth = size * 0.035f
        this.color = if (flicker) darken(color, 0.4f) else lighten(color, 0.55f)
    }
    canvas.drawText(text, x, y, core)
}

/** Glowing tube stroke for neon shapes. */
fun Scene.neonPath(path: Path, color: Int, width: Float = 4f, dim: Boolean = false) {
    canvas.drawPath(path, stroke(color, width * 4f, if (dim) 40 else 90, width * 5f))
    canvas.drawPath(path, stroke(color, width * 1.6f, if (dim) 90 else 200, width))
    canvas.drawPath(path, stroke(if (dim) darken(color, 0.3f) else lighten(color, 0.6f), width * 0.6f))
}

/** Thick limb drawn as an outlined capsule stroke. */
fun Scene.limb(path: Path, fill: Int, outline: Int, width: Float, outlineWidth: Float = 3f) {
    canvas.drawPath(path, stroke(outline, width + outlineWidth * 2f))
    canvas.drawPath(path, stroke(fill, width))
}
