import com.nikopick.zamoled.desktop.Engine
import com.nikopick.zamoled.gen.CodeSource
import com.nikopick.zamoled.gen.Styles
import com.nikopick.zamoled.gen.WallpaperSpec
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import javax.imageio.ImageIO

/** Draws a handful of wallpapers on this machine's own graphics stack and checks they are not blank. */
@RunWith(RobolectricTestRunner::class)
class SmokeTest {
    @Test
    fun draws() {
        CodeSource.init(File(System.getProperty("zamoled.code")!!))
        val out = File(System.getProperty("zamoled.out")!!).apply { mkdirs() }
        val ids = listOf("wet_street", "source_code", "words_bold", "pixel_planet", "fire_camp", "gatti", "data_rain")
        for ((k, id) in ids.withIndex()) {
            val r = Engine.render(WallpaperSpec(id, 1234L + k, text = "start before you're ready"), 960, 540)
            val lit = (0 until r.image.height step 4).sumOf { y -> (0 until r.image.width step 4).count { x -> (r.image.getRGB(x, y) and 0xFFFFFF) != 0 } }
            ImageIO.write(r.image, "png", File(out, "$id.png"))
            println("drew $id: $lit lit samples, ${(r.blackShare * 100).toInt()}% black")
            assertTrue("$id came out blank", lit > 50)
        }
        assertTrue(Styles.all.size > 40)
    }
}

@RunWith(RobolectricTestRunner::class)
class AllStylesSheet {
    /** With -Dzamoled.all=1, draws every style at desktop shape so the whole catalogue can be looked over. */
    @Test
    fun everything() {
        if (System.getProperty("zamoled.all").isNullOrEmpty()) return
        CodeSource.init(File(System.getProperty("zamoled.code")!!))
        val out = File(System.getProperty("zamoled.out")!!, "all").apply { mkdirs() }
        for (style in Styles.all) {
            val r = Engine.render(WallpaperSpec(style.id, 7L, text = ""), 640, 360)
            ImageIO.write(r.image, "png", File(out, "${style.category.name}_${style.id}.png"))
        }
    }
}
