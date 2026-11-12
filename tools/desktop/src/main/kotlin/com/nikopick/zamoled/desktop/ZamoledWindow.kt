package com.nikopick.zamoled.desktop

import com.nikopick.zamoled.gen.Category
import com.nikopick.zamoled.gen.DoodleLayout
import com.nikopick.zamoled.gen.Palettes
import com.nikopick.zamoled.gen.Style
import com.nikopick.zamoled.gen.Styles
import com.nikopick.zamoled.gen.WallpaperSpec
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JSlider
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.random.Random

private val EMBER = Color(255, 106, 61)
private val PANEL = Color(10, 10, 12)
private val FIELD = Color(24, 24, 28)
private val MUTED = Color(150, 152, 162)

/** The size of the picture to make: the screen itself, or a fixed size. */
private enum class Size(val label: String, val w: Int, val h: Int) {
    SCREEN("This screen", 0, 0),
    FHD("1920 × 1080", 1920, 1080),
    QHD("2560 × 1440", 2560, 1440),
    UHD("3840 × 2160", 3840, 2160),
}

/** A row in the right-hand list: a style to start from, or a saved favourite. */
private class Item(val label: String, val style: Style?, val spec: WallpaperSpec?) {
    override fun toString() = label
}

private class Favorites {
    private val file = File(System.getProperty("user.home"), ".zamoled/favorites.txt")
    val keys: MutableList<String> = (if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()).toMutableList()
    private fun save() = runCatching { file.parentFile.mkdirs(); file.writeText(keys.joinToString("\n")) }
    fun has(spec: WallpaperSpec) = spec.key in keys
    fun toggle(spec: WallpaperSpec): Boolean {
        val on = if (has(spec)) { keys.remove(spec.key); false } else { keys.add(0, spec.key); true }
        save()
        return on
    }
    val specs: List<WallpaperSpec> get() = keys.mapNotNull { WallpaperSpec.fromKey(it) }
}

private class PreviewPanel : JPanel() {
    var image: BufferedImage? = null
        set(v) { field = v; repaint() }
    var message: String = "Drawing…"

    init { background = Color.BLACK }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val img = image
        val g2 = g as java.awt.Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        if (img == null) {
            g2.color = MUTED
            g2.drawString(message, 24, 36)
            return
        }
        val pad = 14
        val k = minOf((width - pad * 2f) / img.width, (height - pad * 2f) / img.height)
        val w = (img.width * k).toInt()
        val h = (img.height * k).toInt()
        val x = (width - w) / 2
        val y = (height - h) / 2
        g2.drawImage(img, x, y, w, h, null)
        g2.color = Color(255, 255, 255, 36)
        g2.drawRect(x, y, w - 1, h - 1)
    }
}

/** The Zamoled desktop window: browse the worlds, tune a wallpaper, save it or put it on the desktop. */
class ZamoledWindow {
    private val frame = JFrame("Zamoled")
    private val closed = CountDownLatch(1)
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "zamoled-render").apply { isDaemon = true } }
    private val favorites = Favorites()
    private val preview = PreviewPanel()
    private val status = JLabel(" ")

    private var spec = WallpaperSpec(Styles.all.random().id, Random.nextLong())
    private var size = Size.SCREEN
    @Volatile private var generation = 0
    private var building = false

    private val categories = DefaultListModel<Item>()
    private val categoryList = JList(categories)
    private val styleModel = DefaultListModel<Item>()
    private val styleList = JList(styleModel)

    private val paletteBox = JComboBox<String>()
    private val layoutBox = JComboBox<String>()
    private val detail = JSlider(4, 18, 10)
    private val black = JSlider(0, 100, 0)
    private val wordsField = JTextField()
    private val redRadio = JRadioButton("Red", true)
    private val whiteRadio = JRadioButton("White")
    private val alignBox = JComboBox(arrayOf("Left", "Center", "Right"))
    private val vposBox = JComboBox(arrayOf("Top", "Middle", "Bottom"))
    private val sizeBox = JComboBox(Size.entries.map { it.label }.toTypedArray())
    private val favButton = JButton("♥  Favorite")

    private lateinit var paletteRow: JComponent
    private lateinit var layoutRow: JComponent
    private lateinit var detailRow: JComponent
    private lateinit var wordsRow: JComponent

    private val redraw = Timer(200) { rerender() }.apply { isRepeats = false }

    fun open() = SwingUtilities.invokeAndWait {
        build()
        frame.isVisible = true
        rerender()
    }

    fun awaitClose() = closed.await()

    // ---------------------------------------------------------------- building the window

    private fun heading(text: String) = JLabel(text.uppercase()).apply {
        foreground = MUTED
        font = Font("Monospaced", Font.BOLD, 11)
        border = EmptyBorder(14, 0, 4, 0)
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private fun row(vararg parts: Component): JComponent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        alignmentX = Component.LEFT_ALIGNMENT
        parts.forEach { (it as? JComponent)?.alignmentX = Component.LEFT_ALIGNMENT; add(it) }
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height + 8)
    }

    private fun build() {
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                frame.dispose()
                closed.countDown()
            }
        })
        frame.layout = BorderLayout()

        // Left: worlds, then the wallpapers in the world picked.
        categories.addElement(Item("♥  Favorites", null, null))
        Styles.categories.forEach { c -> categories.addElement(Item(c.label, null, null).also { categoryByItem[it] = c }) }
        categoryList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        styleList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        categoryList.addListSelectionListener { if (!it.valueIsAdjusting && !building) fillStyles() }
        styleList.addListSelectionListener { if (!it.valueIsAdjusting && !building) pickItem() }
        val left = JPanel(BorderLayout()).apply {
            preferredSize = Dimension(230, 100)
            add(JScrollPane(categoryList).apply { preferredSize = Dimension(230, 330); border = null }, BorderLayout.NORTH)
            add(JScrollPane(styleList).apply { border = null }, BorderLayout.CENTER)
        }

        // Right: the controls.
        paletteBox.addItem("Auto")
        Palettes.all.forEach { paletteBox.addItem(it.name) }
        DoodleLayout.choices.forEach { layoutBox.addItem(it.label) }
        paletteBox.addActionListener { if (!building) { spec = spec.copy(paletteId = if (paletteBox.selectedIndex <= 0) "auto" else Palettes.all[paletteBox.selectedIndex - 1].id); rerender() } }
        layoutBox.addActionListener { if (!building) { spec = spec.copy(layoutId = DoodleLayout.choices[layoutBox.selectedIndex].id); rerender() } }
        detail.addChangeListener { if (!building) { spec = spec.copy(density = detail.value / 10f); redraw.restart() } }
        black.addChangeListener { if (!building) { spec = spec.copy(black = black.value / 100f); redraw.restart() } }
        wordsField.document.addDocumentListener(object : DocumentListener {
            private fun changed() { if (!building) { spec = spec.copy(text = wordsField.text.take(80).replace('|', ' ')); redraw.restart() } }
            override fun insertUpdate(e: DocumentEvent) = changed()
            override fun removeUpdate(e: DocumentEvent) = changed()
            override fun changedUpdate(e: DocumentEvent) = changed()
        })
        ButtonGroup().apply { add(redRadio); add(whiteRadio) }
        redRadio.addActionListener { spec = spec.copy(white = false); rerender() }
        whiteRadio.addActionListener { spec = spec.copy(white = true); rerender() }
        alignBox.selectedIndex = 1
        vposBox.selectedIndex = 1
        alignBox.addActionListener { if (!building) { spec = spec.copy(align = alignBox.selectedIndex); rerender() } }
        vposBox.addActionListener { if (!building) { spec = spec.copy(vpos = vposBox.selectedIndex); rerender() } }
        sizeBox.addActionListener { size = Size.entries[sizeBox.selectedIndex] }

        val shuffle = JButton("New variation").apply { addActionListener { spec = spec.copy(seed = Random.nextLong()); updateFav(); rerender() } }
        val surprise = JButton("Surprise me").apply { addActionListener { surpriseMe() } }
        favButton.addActionListener { favorites.toggle(spec); updateFav() }
        val save = JButton("Save PNG…").apply { addActionListener { savePng() } }
        val setWall = JButton("Set as desktop wallpaper").apply { addActionListener { setWallpaper() } }

        paletteRow = row(heading("Palette"), paletteBox)
        layoutRow = row(heading("Arrangement"), layoutBox)
        detailRow = row(heading("Detail"), detail)
        wordsRow = row(
            heading("Your words"), wordsField,
            JPanel().apply { isOpaque = false; add(JLabel("Highlight")); add(redRadio); add(whiteRadio) },
            JPanel().apply { isOpaque = false; add(alignBox); add(vposBox) },
        )
        val controls = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = EmptyBorder(14, 14, 14, 14)
            add(row(shuffle)); add(Box.createVerticalStrut(6)); add(row(surprise)); add(Box.createVerticalStrut(6)); add(row(favButton))
            add(paletteRow); add(layoutRow); add(detailRow)
            add(row(heading("Black level"), black))
            add(wordsRow)
            add(row(heading("Size"), sizeBox))
            add(Box.createVerticalStrut(14))
            add(row(save)); add(Box.createVerticalStrut(6)); add(row(setWall))
            add(Box.createVerticalGlue())
        }
        val right = JScrollPane(controls).apply { preferredSize = Dimension(300, 100); border = null; horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER }

        status.border = EmptyBorder(6, 12, 6, 12)
        status.foreground = MUTED
        frame.add(left, BorderLayout.WEST)
        frame.add(preview, BorderLayout.CENTER)
        frame.add(right, BorderLayout.EAST)
        frame.add(status, BorderLayout.SOUTH)
        dark(frame.contentPane)
        frame.size = Dimension(1240, 860)
        frame.setLocationRelativeTo(null)

        building = true
        categoryList.selectedIndex = 1.coerceAtMost(categories.size() - 1)
        building = false
        selectCategoryOf(spec.style)
        syncControls()
    }

    private val categoryByItem = HashMap<Item, Category>()

    private fun selectCategoryOf(style: Style) {
        building = true
        val idx = (0 until categories.size()).first { categoryByItem[categories[it]] == style.category }
        categoryList.selectedIndex = idx
        fillStylesQuiet()
        val k = (0 until styleModel.size()).firstOrNull { styleModel[it].style?.id == style.id } ?: 0
        styleList.selectedIndex = k
        building = false
    }

    private fun fillStylesQuiet() {
        styleModel.clear()
        val item = categoryList.selectedValue ?: return
        val cat = categoryByItem[item]
        if (cat == null) favorites.specs.forEach { styleModel.addElement(Item("${it.style.name}  #${(it.seed and 0xFFFF)}", null, it)) }
        else Styles.of(cat).forEach { styleModel.addElement(Item(it.name, it, null)) }
    }

    private fun fillStyles() {
        building = true
        fillStylesQuiet()
        building = false
        if (styleModel.size() > 0) {
            building = true
            styleList.selectedIndex = 0
            building = false
            pickItem()
        }
    }

    /** A style or favourite was chosen in the list. */
    private fun pickItem() {
        val item = styleList.selectedValue ?: return
        spec = when {
            item.spec != null -> item.spec
            item.style != null -> spec.copy(styleId = item.style.id, seed = Random.nextLong(), paletteId = "auto", layoutId = "auto")
            else -> return
        }
        syncControls()
        rerender()
    }

    private fun surpriseMe() {
        spec = spec.copy(styleId = Styles.all.random().id, seed = Random.nextLong(), paletteId = "auto", layoutId = "auto")
        selectCategoryOf(spec.style)
        syncControls()
        rerender()
    }

    /** Shows only the controls that do something for the style on screen, and puts the spec's values in them. */
    private fun syncControls() {
        building = true
        val style = spec.style
        paletteRow.isVisible = style.usesPalette
        layoutRow.isVisible = style.category == Category.DOODLE || style.category == Category.LAB
        detailRow.isVisible = style.usesDetail
        wordsRow.isVisible = style.usesText
        paletteBox.selectedIndex = if (spec.paletteId == "auto") 0 else (Palettes.all.indexOfFirst { it.id == spec.paletteId } + 1).coerceAtLeast(0)
        layoutBox.selectedIndex = DoodleLayout.choices.indexOfFirst { it.id == spec.layoutId }.coerceAtLeast(0)
        detail.value = (spec.density * 10).toInt().coerceIn(4, 18)
        black.value = (spec.black * 100).toInt().coerceIn(0, 100)
        wordsField.text = spec.text
        redRadio.isSelected = !spec.white
        whiteRadio.isSelected = spec.white
        alignBox.selectedIndex = spec.align
        vposBox.selectedIndex = spec.vpos
        building = false
        updateFav()
    }

    private fun updateFav() {
        favButton.text = if (favorites.has(spec)) "♥  Saved to favorites" else "♡  Add to favorites"
    }

    // ---------------------------------------------------------------- drawing

    /** The size of the picture to make, in real pixels. */
    private fun outputSize(): Pair<Int, Int> {
        if (size != Size.SCREEN) return size.w to size.h
        return runCatching {
            val gc = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
            val b = gc.bounds
            (b.width * gc.defaultTransform.scaleX).toInt() to (b.height * gc.defaultTransform.scaleY).toInt()
        }.getOrDefault(1920 to 1080)
    }

    private fun rerender() {
        val gen = ++generation
        val s = spec
        status.text = "Drawing…"
        val (ow, oh) = outputSize()
        worker.execute {
            if (gen != generation) return@execute
            try {
                val pw = 900
                val ph = (pw.toFloat() * oh / ow).toInt().coerceAtLeast(300)
                val r = Engine.render(s, pw, ph)
                SwingUtilities.invokeLater {
                    if (gen == generation) {
                        preview.image = r.image
                        status.text = "${s.style.name}  ·  ${s.style.category.label}  ·  ${(r.blackShare * 100).toInt()}% pure black  ·  ${ow} × ${oh}"
                        shotIfAsked()
                    }
                }
            } catch (t: Throwable) {
                SwingUtilities.invokeLater { status.text = "Could not draw this one: ${t.message}" }
            }
        }
    }

    // ---------------------------------------------------------------- saving

    private fun savePng() {
        val dir = File(System.getProperty("user.home"), "Pictures").takeIf { it.isDirectory } ?: File(System.getProperty("user.home"))
        val chooser = JFileChooser(dir).apply { selectedFile = File(dir, "zamoled-${spec.styleId}.png") }
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return
        val target = chooser.selectedFile.let { if (it.name.endsWith(".png", true)) it else File(it.parentFile, it.name + ".png") }
        val s = spec
        val (w, h) = outputSize()
        status.text = "Saving ${w} × ${h}…"
        worker.execute {
            val msg = try {
                ImageIO.write(Engine.render(s, w, h).image, "png", target)
                "Saved ${target.name}"
            } catch (t: Throwable) {
                "Could not save: ${t.message}"
            }
            SwingUtilities.invokeLater { status.text = msg }
        }
    }

    private fun setWallpaper() {
        val s = spec
        val (w, h) = outputSize()
        status.text = "Setting the wallpaper…"
        worker.execute {
            val msg = try {
                val home = System.getenv("LOCALAPPDATA")?.let { File(it, "Zamoled") } ?: File(System.getProperty("user.home"), ".zamoled")
                home.mkdirs()
                // A new name each time, so Windows never shows a cached copy.
                home.listFiles { f -> f.name.startsWith("wallpaper-") }?.forEach { it.delete() }
                val file = File(home, "wallpaper-${System.currentTimeMillis()}.png")
                ImageIO.write(Engine.render(s, w, h).image, "png", file)
                if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
                    applyOnWindows(file)
                    "Wallpaper set."
                } else {
                    "Saved to ${file.path} (setting the wallpaper is only built for Windows)."
                }
            } catch (t: Throwable) {
                "Could not set the wallpaper: ${t.message}"
            }
            SwingUtilities.invokeLater { status.text = msg }
        }
    }

    private fun applyOnWindows(file: File) {
        fun run(vararg cmd: String) = ProcessBuilder(*cmd).redirectErrorStream(true).start().also { it.inputStream.readBytes() }.waitFor()
        run("reg", "add", "HKCU\\Control Panel\\Desktop", "/v", "WallpaperStyle", "/t", "REG_SZ", "/d", "10", "/f")
        run("reg", "add", "HKCU\\Control Panel\\Desktop", "/v", "TileWallpaper", "/t", "REG_SZ", "/d", "0", "/f")
        val path = file.absolutePath.replace("'", "''")
        val script = """
            ${'$'}code = @'
            using System.Runtime.InteropServices;
            public class ZamoledWallpaper { [DllImport("user32.dll", CharSet = CharSet.Auto)] public static extern int SystemParametersInfo(int a, int b, string c, int d); }
            '@
            Add-Type -TypeDefinition ${'$'}code
            [ZamoledWallpaper]::SystemParametersInfo(20, 0, '$path', 3) | Out-Null
        """.trimIndent()
        run("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script)
    }

    // ---------------------------------------------------------------- looks

    private fun dark(c: Component) {
        val ui = Font(if (Font("Segoe UI", Font.PLAIN, 12).family == "Segoe UI") "Segoe UI" else "Dialog", Font.PLAIN, 13)
        c.font = ui
        when (c) {
            is JScrollPane -> { c.background = PANEL; c.viewport.background = PANEL }
            is JList<*> -> { c.background = PANEL; c.foreground = Color(225, 225, 232); c.selectionBackground = EMBER; c.selectionForeground = Color.BLACK; c.border = EmptyBorder(4, 8, 4, 8); c.fixedCellHeight = 26 }
            is JButton -> { c.background = Color(34, 34, 40); c.foreground = Color.WHITE; c.isFocusPainted = false }
            is JComboBox<*> -> { c.background = FIELD; c.foreground = Color.WHITE }
            is JTextField -> { c.background = FIELD; c.foreground = Color.WHITE; c.caretColor = Color.WHITE }
            is JRadioButton -> { c.background = PANEL; c.foreground = Color(225, 225, 232) }
            is JSlider -> { c.background = PANEL; c.foreground = EMBER }
            is PreviewPanel -> Unit
            is JLabel -> if (c.foreground == null || c.foreground == Color.BLACK || c.foreground == Color(51, 51, 51)) c.foreground = Color(225, 225, 232)
            is JPanel -> c.background = PANEL
        }
        if (c is Container) c.components.forEach { dark(it) }
    }

    /** For checking the layout without a person looking: with -Dzamoled.shot=file.png, save the window once and quit. */
    private fun shotIfAsked() {
        val path = System.getProperty("zamoled.shot") ?: return
        System.clearProperty("zamoled.shot")
        Timer(400) {
            val img = BufferedImage(frame.contentPane.width, frame.contentPane.height, BufferedImage.TYPE_INT_RGB)
            frame.contentPane.paint(img.createGraphics())
            ImageIO.write(img, "png", File(path))
            frame.dispose()
            closed.countDown()
        }.apply { isRepeats = false }.start()
    }
}
