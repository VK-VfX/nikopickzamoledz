import android.graphics.Bitmap
import com.nikopick.zamoled.gen.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class RenderAll {
    @Test
    fun render() {
        val out = File(System.getProperty("preview.out")!!).apply { mkdirs() }
        val only = System.getProperty("preview.only").orEmpty().split(",").filter { it.isNotBlank() }
        val seeds = System.getProperty("preview.seed").orEmpty().split(",").mapNotNull { it.toLongOrNull() }.ifEmpty { listOf(1L) }
        val width = System.getProperty("preview.width")?.toIntOrNull() ?: 540
        val density = System.getProperty("preview.density")?.toFloatOrNull() ?: 1f
        val black = System.getProperty("preview.black")?.toFloatOrNull() ?: 0f
        val text = System.getProperty("preview.text").orEmpty()
        val palette = System.getProperty("preview.palette").orEmpty().ifBlank { "auto" }
        val layouts = System.getProperty("preview.layout").orEmpty().split(",").filter { it.isNotBlank() }.ifEmpty { listOf("auto") }
        val safe = System.getProperty("preview.safe") == "1"
        val white = System.getProperty("preview.white") == "1"
        val align = System.getProperty("preview.align")?.toIntOrNull() ?: 1
        val vpos = System.getProperty("preview.vpos")?.toIntOrNull() ?: 1
        CodeSource.init(File("../../app/src/main/java/com/nikopick/zamoled/gen"))
        val styles = if (only.isEmpty()) Styles.all else only.map { Styles.byId(it) }
        for (style in styles) for (seed in seeds) for (layout in layouts) {
            val t0 = System.nanoTime()
            try {
                val bmp = Renderer.render(WallpaperSpec(style.id, seed * -7046029254386353131L + 12345L, palette, density, layout, black, text, safe, white, align, vpos), width, (width * 1170 / 540))
                File(out, "${style.id}_${layout}_$seed${if (density != 1f) "_d$density" else ""}${if (safe) "_safe" else ""}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                println("OK   ${style.id} $layout seed=$seed ${(System.nanoTime() - t0) / 1_000_000}ms")
            } catch (e: Throwable) {
                println("FAIL ${style.id} $layout seed=$seed ${e::class.simpleName}: ${e.message}")
                e.stackTrace.take(6).forEach { println("       at $it") }
            }
        }
    }
}
