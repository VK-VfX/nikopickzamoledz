package com.nikopick.zamoled.desktop

import android.graphics.Bitmap
import com.nikopick.zamoled.gen.Renderer
import com.nikopick.zamoled.gen.WallpaperSpec
import java.awt.image.BufferedImage
import kotlin.math.sqrt

/** Draws wallpapers with the same generators the phone app uses, at the shape of a desktop screen. */
object Engine {
    /** A phone is about 0.45 as wide as it is tall; the scenes are tuned to that. */
    private const val PHONE_ASPECT = 0.45f

    class Result(val image: BufferedImage, val blackShare: Float)

    /**
     * How wide the drawing canvas is, in the generators' units. A phone is 1000 wide. A screen that is four
     * times wider relative to its height gets about twice that, so scenes spread across it with room to breathe
     * and the things in them keep a sensible size, rather than being squashed flat.
     */
    fun virtualWidth(width: Int, height: Int): Float = 1000f * sqrt((width.toFloat() / height / PHONE_ASPECT).coerceAtLeast(1f))

    private fun toImage(bmp: Bitmap): BufferedImage {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        return BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).also { it.setRGB(0, 0, w, h, px, 0, w) }
    }

    fun render(spec: WallpaperSpec, width: Int, height: Int): Result {
        val bmp = Renderer.render(spec, width, height, virtualWidth(width, height))
        return Result(toImage(bmp), Renderer.blackRatio(bmp))
    }
}
