package com.nikopick.zamoled.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nikopick.zamoled.data.FavoritesStore
import com.nikopick.zamoled.data.Resolution
import com.nikopick.zamoled.data.Screen
import com.nikopick.zamoled.data.WallpaperActions
import com.nikopick.zamoled.data.WallpaperTarget
import com.nikopick.zamoled.gen.Palette
import com.nikopick.zamoled.gen.Palettes
import com.nikopick.zamoled.gen.Renderer
import com.nikopick.zamoled.gen.WallpaperSpec
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(initial: WallpaperSpec, favorites: FavoritesStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var spec by remember(initial.key) { mutableStateOf(initial) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var blackShare by remember { mutableStateOf<Float?>(null) }
    var rendering by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    var chrome by remember { mutableStateOf(true) }
    var showApply by remember { mutableStateOf(false) }
    var resolution by rememberSaveable { mutableStateOf(Resolution.SCREEN) }
    var detail by remember(spec.styleId, spec.seed) { mutableFloatStateOf(spec.density) }

    BackHandler(onBack = onBack)

    LaunchedEffect(spec) {
        rendering = true
        val bmp = withContext(renderDispatcher) { Renderer.render(spec, Screen.width, Screen.height) }
        preview = bmp
        rendering = false
        blackShare = withContext(renderDispatcher) { Renderer.blackRatio(bmp) }
    }

    suspend fun bitmapFor(res: Resolution): Bitmap {
        val current = preview
        if (res == Resolution.SCREEN && current != null) return current
        return withContext(renderDispatcher) { Renderer.render(spec, res.width, res.height) }
    }

    fun fileName() = "zamoled_${spec.styleId}_${spec.seed.toULong().toString(16)}"

    fun save() = scope.launch {
        busy = "Rendering ${resolution.width}×${resolution.height}…"
        val bmp = bitmapFor(resolution)
        busy = "Saving…"
        val uri = WallpaperActions.saveToGallery(context, bmp, fileName())
        if (bmp !== preview) bmp.recycle()
        busy = null
        snackbar.showSnackbar(if (uri != null) "Saved to Pictures/${WallpaperActions.ALBUM}" else "Couldn't save. Check storage space and try again.")
    }

    fun apply(target: WallpaperTarget) = scope.launch {
        busy = "Setting wallpaper…"
        val bmp = bitmapFor(Resolution.SCREEN)
        val ok = WallpaperActions.setWallpaper(context, bmp, target)
        busy = null
        snackbar.showSnackbar(if (ok) "Wallpaper set on ${target.label.lowercase()}" else "Couldn't set the wallpaper on this device.")
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        preview?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "${spec.style.name} wallpaper preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { chrome = !chrome },
            )
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopControls(
                spec = spec,
                favorite = favorites.isFavorite(spec),
                onBack = onBack,
                onFavorite = { favorites.toggle(spec) },
            )
        }

        AnimatedVisibility(
            visible = chrome,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (rendering || busy != null) {
                        LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(blackShare?.let { "Pitch black ${(it * 100).roundToInt()}%" } ?: "Measuring black…")
                            },
                            leadingIcon = { Icon(Icons.Rounded.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                        Text(
                            busy ?: "Seed ${spec.seed.toULong().toString(16).take(8)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = if (busy == null) FontFamily.Monospace else FontFamily.Default,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    SectionLabel("Palette")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                        item(key = "auto") {
                            PaletteSwatch(null, selected = spec.paletteId == "auto") { spec = spec.copy(paletteId = "auto") }
                        }
                        items(Palettes.choices, key = { it.id }) { p ->
                            PaletteSwatch(p, selected = spec.paletteId == p.id) { spec = spec.copy(paletteId = p.id) }
                        }
                    }

                    SectionLabel("Detail  ·  ${"%.1f".format(detail)}×")
                    Slider(
                        value = detail,
                        onValueChange = { detail = it },
                        onValueChangeFinished = { spec = spec.copy(density = (detail * 10).roundToInt() / 10f) },
                        valueRange = 0.4f..1.8f,
                    )

                    SectionLabel("Save resolution")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Resolution.entries.forEachIndexed { index, res ->
                            SegmentedButton(
                                selected = resolution == res,
                                onClick = { resolution = res },
                                shape = SegmentedButtonDefaults.itemShape(index, Resolution.entries.size),
                            ) { Text(res.label) }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { spec = spec.copy(seed = Random.nextLong()) },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                        FilledTonalButton(
                            onClick = { save() },
                            enabled = busy == null && !rendering,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save")
                        }
                        Button(
                            onClick = { showApply = true },
                            enabled = busy == null && !rendering,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Apply")
                        }
                    }
                }
            }
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp),
        )
    }

    if (showApply) {
        ModalBottomSheet(
            onDismissRequest = { showApply = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                "Set wallpaper on",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            WallpaperTarget.entries.forEach { target ->
                val icon: ImageVector = when (target) {
                    WallpaperTarget.HOME -> Icons.Rounded.Home
                    WallpaperTarget.LOCK -> Icons.Rounded.Lock
                    WallpaperTarget.BOTH -> Icons.Rounded.Smartphone
                }
                ListItem(
                    headlineContent = { Text(target.label) },
                    leadingContent = { Icon(icon, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        showApply = false
                        apply(target)
                    },
                )
            }
            Spacer(Modifier.navigationBarsPadding().padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun TopControls(spec: WallpaperSpec, favorite: Boolean, onBack: () -> Unit, onFavorite: () -> Unit) {
    val glass = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        FilledTonalIconButton(onClick = onBack, colors = glass) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(spec.style.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                "${spec.style.category.label} · ${Palettes.resolve(spec.paletteId, spec.seed).name}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        FilledTonalIconButton(onClick = onFavorite, colors = glass) {
            Icon(
                if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PaletteSwatch(palette: Palette?, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .border(BorderStroke(2.dp, ring), CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
    ) {
        if (palette == null) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = "Auto palette", modifier = Modifier.size(18.dp))
        } else {
            val colors = palette.colors.map { Color(it) }
            Canvas(Modifier.fillMaxSize()) {
                val sweep = 360f / colors.size
                colors.forEachIndexed { i, c ->
                    drawArc(c, startAngle = -90f + i * sweep, sweepAngle = sweep, useCenter = true, topLeft = Offset.Zero, size = Size(size.width, size.height))
                }
            }
        }
    }
}
