package com.nikopick.zamoled.gen

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF

/**
 * Source of the bundled pictures. The app installs a loader that reads them from its assets.
 * [targetWidth] is how wide the result will be drawn, so a thumbnail does not decode a full image.
 */
object ArtLibrary {
    @Volatile
    var loader: ((asset: String, targetWidth: Int) -> Bitmap?)? = null
}

/**
 * A finished picture instead of a drawing. Each one was rendered offline (see tools/art) with
 * lighting, optics and film-style tone mapping that a phone could not compute on the fly, then
 * bundled as a small WebP. The seed picks which picture, whether it is mirrored, and how it is
 * cropped, so one set of pictures yields many different wallpapers.
 */
class ArtStyle(id: String, name: String, category: Category, private val count: Int) : Style(id, name, category) {
    override val usesPalette: Boolean get() = false

    override fun draw(s: Scene) {
        val pick = s.rnd.nextInt(count) + 1
        val mirror = s.rnd.nextBoolean()
        val zoom = 1f + s.rnd.nextFloat() * 0.14f
        val panX = s.rnd.nextFloat()
        val panY = 0.35f + s.rnd.nextFloat() * 0.3f
        val bmp = ArtLibrary.loader?.invoke("%s/%02d".format(id, pick), s.canvas.width) ?: return

        // Cover the screen, then zoom in a little and slide the window around.
        val screenAspect = s.w / s.h
        val bmpAspect = bmp.width.toFloat() / bmp.height
        val baseW: Float
        val baseH: Float
        if (bmpAspect > screenAspect) {
            baseH = bmp.height.toFloat()
            baseW = baseH * screenAspect
        } else {
            baseW = bmp.width.toFloat()
            baseH = baseW / screenAspect
        }
        val srcW = baseW / zoom
        val srcH = baseH / zoom
        val left = (bmp.width - srcW) * panX
        val top = (bmp.height - srcH) * panY
        val src = android.graphics.Rect(left.toInt(), top.toInt(), (left + srcW).toInt(), (top + srcH).toInt())

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        s.canvas.save()
        if (mirror) s.canvas.scale(-1f, 1f, s.w / 2f, 0f)
        s.canvas.drawBitmap(bmp, src, RectF(0f, 0f, s.w, s.h), paint)
        s.canvas.restore()
    }
}

/** The picture styles, one per kind of image in assets/art. */
object ArtStyles {
    val styles: List<Style> = listOf(
        ArtStyle("art_nebula", "Nebula", Category.SPACE, 8),
        ArtStyle("art_galaxy", "Galaxy", Category.SPACE, 6),
        ArtStyle("art_giant", "Gas Giant", Category.SPACE, 6),
        ArtStyle("art_moon", "Moon", Category.SPACE, 5),
        ArtStyle("art_aurora", "Aurora", Category.NATURE, 6),
        ArtStyle("art_milkyway", "Milky Way", Category.NATURE, 4),
        ArtStyle("art_moonlit", "Moonlit Lake", Category.NATURE, 4),
        ArtStyle("art_ink", "Liquid Ink", Category.ABSTRACT, 8),
        ArtStyle("art_bokeh", "Bokeh", Category.LIGHT, 6),
        ArtStyle("art_highway", "Night Highway", Category.CITY, 6),
    )
}
