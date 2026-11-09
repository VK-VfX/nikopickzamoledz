package androidx.core.graphics

/** Minimal stand-in for androidx.core.graphics.ColorUtils so the art code compiles on a desktop JVM. */
object ColorUtils {
    @JvmStatic fun setAlphaComponent(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    @JvmStatic fun blendARGB(c1: Int, c2: Int, ratio: Float): Int {
        val r = 1f - ratio
        fun ch(shift: Int) = ((((c1 ushr shift) and 255) * r + ((c2 ushr shift) and 255) * ratio) + 0.5f).toInt().coerceIn(0, 255)
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    @JvmStatic fun colorToHSL(color: Int, hsl: FloatArray) {
        val r = ((color shr 16) and 255) / 255f
        val g = ((color shr 8) and 255) / 255f
        val b = (color and 255) / 255f
        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        val d = mx - mn
        val l = (mx + mn) / 2f
        var h: Float
        val s: Float
        if (mx == mn) {
            h = 0f; s = 0f
        } else {
            h = when (mx) {
                r -> ((g - b) / d) % 6f
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
            s = d / (1f - Math.abs(2f * l - 1f))
        }
        h = (h * 60f) % 360f
        if (h < 0) h += 360f
        hsl[0] = h; hsl[1] = s.coerceIn(0f, 1f); hsl[2] = l.coerceIn(0f, 1f)
    }

    @JvmStatic fun HSLToColor(hsl: FloatArray): Int {
        val h = hsl[0]; val s = hsl[1]; val l = hsl[2]
        val c = (1f - Math.abs(2f * l - 1f)) * s
        val m = l - 0.5f * c
        val x = c * (1f - Math.abs((h / 60f) % 2f - 1f))
        val (r, g, b) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
        }
        fun ch(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (255 shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
