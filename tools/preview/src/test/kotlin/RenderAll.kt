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
        val palette = System.getProperty("preview.palette").orEmpty().ifBlank { "auto" }
        val styles = if (only.isEmpty()) Styles.all else only.map { Styles.byId(it) }
        for (style in styles) for (seed in seeds) {
            val t0 = System.nanoTime()
            try {
                val bmp = Renderer.render(WallpaperSpec(style.id, seed * -7046029254386353131L + 12345L, palette), 540, 1170)
                File(out, "${style.id}_$seed.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                println("OK   ${style.id} seed=$seed ${(System.nanoTime() - t0) / 1_000_000}ms")
            } catch (e: Throwable) {
                println("FAIL ${style.id} seed=$seed ${e::class.simpleName}: ${e.message}")
                e.stackTrace.take(6).forEach { println("       at $it") }
            }
        }
    }
}
