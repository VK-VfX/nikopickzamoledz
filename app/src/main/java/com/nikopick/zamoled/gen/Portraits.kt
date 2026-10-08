package com.nikopick.zamoled.gen

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max

/** Source of the bundled portrait bitmaps; installed by the app (assets) or the desktop preview (files). */
object PhotoLibrary {
    /** Decodes portrait [asset] at about [targetWidth] pixels wide (never smaller). Returns null if missing. */
    @Volatile
    var loader: ((asset: String, targetWidth: Int) -> Bitmap?)? = null
}

/**
 * One bundled portrait: a transparent-background cut-out sitting on a 1080x2400 canvas with its lower
 * edge already faded out. Head position is stored as fractions of that canvas, for the halo behind it.
 */
class Portrait(val asset: String, val name: String, val headX: Float, val headY: Float, val headR: Float)

/**
 * A portrait on pure AMOLED black. The seed picks one of three looks:
 * natural (just the photo), aura (palette-coloured halo and rim light) or noir (monochrome with the aura).
 */
class PortraitStyle(private val p: Portrait, index: Int) : Style("portrait_%02d".format(index + 1), p.name, Category.PORTRAIT) {
    override val autoPalettes = listOf("violet", "cyan", "sakura", "lava", "hue", "gold", "aurora", "candy")

    override fun draw(s: Scene) {
        // About 45% natural, 25% natural with an aura, 30% noir with an aura.
        val roll = s.rnd.nextInt(20)
        val look = if (roll < 9) 0 else if (roll < 14) 1 else 2
        val bmp = PhotoLibrary.loader?.invoke(p.asset, (s.w * s.pxPerUnit).toInt()) ?: return
        // Cover the screen, anchored to the bottom so the faded edge stays at the bottom of the display.
        val k = max(s.w / bmp.width, s.h / bmp.height)
        val dw = bmp.width * k
        val dh = bmp.height * k
        val ox = (s.w - dw) / 2f
        val oy = s.h - dh
        val hx = ox + p.headX * dw
        val hy = oy + p.headY * dh
        val hr = p.headR * dw
        val accent = s.palette.colors[0]
        val accent2 = s.palette.colors[1 % s.palette.colors.size]

        if (look != 0) {
            s.starSky(90, (hy - hr * 1.3f).coerceAtLeast(40f), 4)
            s.softGlow(hx, hy - hr * 0.3f, hr * 3.4f, accent, 110)
            s.canvas.drawCircle(hx, hy - hr * 0.3f, hr * 1.45f, s.stroke(accent, 2f, 140))
            s.canvas.drawCircle(hx, hy - hr * 0.3f, hr * 1.6f, s.stroke(accent2, 1.2f, 90))
            // Rim light: the silhouette blurred and tinted, drawn under the photo.
            val offset = IntArray(2)
            val blur = Paint().apply { maskFilter = BlurMaskFilter(bmp.width * 0.02f, BlurMaskFilter.Blur.NORMAL) }
            val rim = bmp.extractAlpha(blur, offset)
            s.canvas.save()
            s.canvas.translate(ox, oy)
            s.canvas.scale(k, k)
            s.canvas.drawBitmap(rim, offset[0].toFloat(), offset[1].toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; alpha = 190 })
            s.canvas.restore()
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        if (look == 2) {
            val m = ColorMatrix().apply { setSaturation(0f) }
            m.postConcat(ColorMatrix(floatArrayOf(1.18f, 0f, 0f, 0f, -14f, 0f, 1.18f, 0f, 0f, -14f, 0f, 0f, 1.18f, 0f, -14f, 0f, 0f, 0f, 1f, 0f)))
            paint.colorFilter = ColorMatrixColorFilter(m)
        }
        s.canvas.drawBitmap(bmp, null, RectF(ox, oy, ox + dw, oy + dh), paint)
    }
}

object Portraits {
    val all: List<Portrait> = listOf(
    Portrait("p01", "Reaching Out", 0.57f, 0.484f, 0.196f),
    Portrait("p02", "Standing Smile", 0.493f, 0.549f, 0.196f),
    Portrait("p03", "Doorway", 0.494f, 0.539f, 0.196f),
    Portrait("p04", "Hands Clasped", 0.486f, 0.529f, 0.196f),
    Portrait("p05", "Over the Shoulder", 0.432f, 0.519f, 0.196f),
    Portrait("p06", "Looking Back", 0.499f, 0.524f, 0.196f),
    Portrait("p07", "Golden Hour", 0.589f, 0.556f, 0.196f),
    Portrait("p08", "Chin Rest", 0.452f, 0.494f, 0.189f),
    Portrait("p09", "Soft Smile", 0.56f, 0.493f, 0.193f),
    Portrait("p10", "Booth Seat", 0.557f, 0.501f, 0.196f),
    Portrait("p11", "Cheek Rest", 0.373f, 0.515f, 0.19f),
    Portrait("p12", "Cafe Chair", 0.568f, 0.493f, 0.196f),
    Portrait("p13", "Evening Walk", 0.472f, 0.503f, 0.196f),
    Portrait("p14", "Two Cups", 0.504f, 0.513f, 0.196f),
    )

    val styles: List<Style> = all.mapIndexed { i, p -> PortraitStyle(p, i) }
}
