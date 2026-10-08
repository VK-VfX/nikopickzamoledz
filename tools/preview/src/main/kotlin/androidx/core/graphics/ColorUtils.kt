package androidx.core.graphics

/** Minimal stand-in for androidx.core.graphics.ColorUtils so the art code compiles on a desktop JVM. */
object ColorUtils {
    @JvmStatic fun setAlphaComponent(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    @JvmStatic fun blendARGB(c1: Int, c2: Int, ratio: Float): Int {
        val r = 1f - ratio
        fun ch(shift: Int) = ((((c1 ushr shift) and 255) * r + ((c2 ushr shift) and 255) * ratio) + 0.5f).toInt().coerceIn(0, 255)
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
