package com.nikopick.zamoled.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(initial: WallpaperSpec, favorites: FavoritesStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var spec by remember(initial.key) { mutableStateOf(initial) }
    val placeholder = remember(initial.key) { ThumbCache.peekBest(initial) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var blackShare by remember { mutableStateOf<Float?>(null) }
    var rendering by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var chrome by remember { mutableStateOf(true) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showApply by remember { mutableStateOf(false) }
    var celebration by remember { mutableStateOf<String?>(null) }
    var lastCelebration by remember { mutableStateOf("") }
    var resolution by rememberSaveable { mutableStateOf(Resolution.SCREEN) }
    var detail by remember(spec.styleId, spec.seed) { mutableFloatStateOf(spec.density) }
    val zoom = remember { Animatable(1f) }
    val shuffleSpin = remember { Animatable(0f) }

    BackHandler(onBack = onBack)

    LaunchedEffect(spec) {
        rendering = true
        blackShare = null
        val bmp = withContext(renderDispatcher) { Renderer.render(spec, Screen.width, Screen.height) }
        preview = bmp
        rendering = false
        blackShare = withContext(renderDispatcher) { Renderer.blackRatio(bmp) }
    }
    // Each new render settles in with a gentle zoom.
    LaunchedEffect(preview) {
        if (preview != null) {
            zoom.snapTo(1.06f)
            zoom.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(celebration) {
        if (celebration != null) {
            delay(1600)
            celebration = null
        }
    }

    fun celebrate(message: String) {
        lastCelebration = message
        celebration = message
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    suspend fun bitmapFor(res: Resolution): Bitmap {
        val current = preview
        if (res == Resolution.SCREEN && current != null) return current
        return withContext(renderDispatcher) { Renderer.render(spec, res.width, res.height) }
    }

    fun fileName() = "zamoled_${spec.styleId}_${spec.seed.toULong().toString(16)}"

    fun save() = scope.launch {
        busy = true
        val bmp = bitmapFor(resolution)
        val uri = WallpaperActions.saveToGallery(context, bmp, fileName())
        if (bmp !== preview) bmp.recycle()
        busy = false
        if (uri != null) celebrate("Saved in ${resolution.label}") else snackbar.showSnackbar("Couldn't save. Check storage space and try again.")
    }

    fun apply(target: WallpaperTarget) = scope.launch {
        busy = true
        val ok = WallpaperActions.setWallpaper(context, bitmapFor(Resolution.SCREEN), target)
        busy = false
        if (ok) celebrate("Wallpaper applied") else snackbar.showSnackbar("Couldn't set the wallpaper on this device.")
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Wallpaper preview: starts from the cached thumbnail so the shared-element flight has a picture,
        // then crossfades to the full-resolution render.
        Box(
            Modifier
                .fillMaxSize()
                .sharedWallpaper(initial.key, RectangleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { chrome = !chrome },
        ) {
            placeholder?.let {
                Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Crossfade(targetState = preview, animationSpec = tween(450), label = "preview") { bmp ->
                if (bmp != null) {
                    Image(
                        bmp.asImageBitmap(),
                        contentDescription = "${spec.style.name} wallpaper preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = zoom.value
                            scaleY = zoom.value
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = chrome,
            enter = slideInVertically(Motion.spatial()) { -it } + fadeIn(),
            exit = slideOutVertically(Motion.spatial()) { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopControls(
                spec = spec,
                favorite = favorites.isFavorite(spec),
                onBack = onBack,
                onFavorite = {
                    favorites.toggle(spec)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
        }

        AnimatedVisibility(
            visible = chrome,
            enter = slideInVertically(Motion.spatial()) { it } + fadeIn(),
            exit = slideOutVertically(Motion.spatial()) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .fillMaxWidth(),
            ) {
                Column(Modifier.animateContentSize(Motion.spatial()).padding(10.dp)) {
                    AnimatedVisibility(
                        visible = rendering || busy,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        LinearProgressIndicator(
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
                        )
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(Motion.spatial()) + fadeIn(),
                        exit = shrinkVertically(Motion.spatial()) + fadeOut(),
                    ) {
                        CustomizePanel(
                            spec = spec,
                            detail = detail,
                            resolution = resolution,
                            onPalette = { spec = spec.copy(paletteId = it) },
                            onDetail = { detail = it },
                            onDetailDone = { spec = spec.copy(density = (detail * 10).roundToInt() / 10f) },
                            onResolution = { resolution = it },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BlackMeter(blackShare) {
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(
                                    blackShare?.let { "${(it * 100).roundToInt()}% of pixels are pure black, so your OLED screen keeps them switched off." }
                                        ?: "Measuring how much of this wallpaper is pure black…",
                                )
                            }
                        }
                        IconButton(onClick = {
                            spec = spec.copy(seed = Random.nextLong())
                            scope.launch { shuffleSpin.animateTo(shuffleSpin.value + 180f, Motion.bouncy()) }
                        }) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                modifier = Modifier.graphicsLayer { rotationY = shuffleSpin.value },
                            )
                        }
                        FilledTonalIconToggleButton(checked = expanded, onCheckedChange = { expanded = it }) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Customize")
                        }
                        IconButton(onClick = { save() }, enabled = !busy && !rendering) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = "Save to gallery")
                        }
                        Spacer(Modifier.weight(1f))
                        val applyInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = { showApply = true },
                            enabled = !busy && !rendering,
                            interactionSource = applyInteraction,
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            modifier = Modifier.height(52.dp).pressScale(applyInteraction),
                        ) {
                            Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Apply")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = celebration != null,
            enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 450f), initialScale = 0.4f) + fadeIn(),
            exit = scaleOut(targetScale = 0.85f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Surface(shape = RoundedCornerShape(36.dp), color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 16.dp) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(52.dp),
                    )
                    Text(lastCelebration, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp),
        )
    }

    if (showApply) {
        ModalBottomSheet(
            onDismissRequest = { showApply = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                "Set wallpaper on",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            WallpaperTarget.entries.forEach { target ->
                val icon: ImageVector = when (target) {
                    WallpaperTarget.HOME -> Icons.Rounded.Home
                    WallpaperTarget.LOCK -> Icons.Rounded.Lock
                    WallpaperTarget.BOTH -> Icons.Rounded.Smartphone
                }
                ListItem(
                    headlineContent = { Text(target.label, style = MaterialTheme.typography.titleMedium) },
                    leadingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)),
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            showApply = false
                            apply(target)
                        },
                )
            }
            Spacer(Modifier.navigationBarsPadding().padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun CustomizePanel(
    spec: WallpaperSpec,
    detail: Float,
    resolution: Resolution,
    onPalette: (String) -> Unit,
    onDetail: (Float) -> Unit,
    onDetailDone: () -> Unit,
    onResolution: (Resolution) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 14.dp),
    ) {
        SectionLabel("Palette")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
            item(key = "auto") {
                PaletteSwatch(null, selected = spec.paletteId == "auto") { onPalette("auto") }
            }
            items(Palettes.choices, key = { it.id }) { p ->
                PaletteSwatch(p, selected = spec.paletteId == p.id) { onPalette(p.id) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Detail", Modifier.weight(1f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    "%.1f×".format(detail),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Slider(value = detail, onValueChange = onDetail, onValueChangeFinished = onDetailDone, valueRange = 0.4f..1.8f)
        SectionLabel("Save resolution")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Resolution.entries.forEachIndexed { index, res ->
                SegmentedButton(
                    selected = resolution == res,
                    onClick = { onResolution(res) },
                    shape = SegmentedButtonDefaults.itemShape(index, Resolution.entries.size),
                ) { Text(res.label) }
            }
        }
    }
}

@Composable
private fun BlackMeter(share: Float?, onClick: () -> Unit) {
    val progress by animateFloatAsState(share ?: 0f, tween(1000, easing = FastOutSlowInEasing), label = "black")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onClick),
    ) {
        if (share == null) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp), strokeWidth = 3.dp, strokeCap = StrokeCap.Round)
        } else {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
            )
            Text(
                "${(progress * 100).roundToInt()}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun TopControls(spec: WallpaperSpec, favorite: Boolean, onBack: () -> Unit, onFavorite: () -> Unit) {
    val glass = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val heart = remember { Animatable(1f) }
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(favorite) {
        if (firstRun) {
            firstRun = false
        } else {
            heart.snapTo(0.5f)
            heart.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        FilledTonalIconButton(onClick = onBack, colors = glass, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            ) {
                ShapeIcon(spec.style.category, active = false, size = 36.dp, spin = true)
                Column {
                    Text(spec.style.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        "${spec.style.category.label} · ${spec.palette.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        FilledTonalIconButton(onClick = onFavorite, colors = glass, modifier = Modifier.size(48.dp)) {
            Icon(
                if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    scaleX = heart.value
                    scaleY = heart.value
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
private fun PaletteSwatch(palette: Palette?, selected: Boolean, onClick: () -> Unit) {
    val ringWidth by animateDpAsState(if (selected) 3.dp else 0.dp, Motion.bouncy(), label = "ring")
    val corner by animateDpAsState(if (selected) 14.dp else 22.dp, Motion.bouncy(), label = "corner")
    val shape = RoundedCornerShape(corner)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .border(ringWidth, MaterialTheme.colorScheme.primary, shape)
            .padding(5.dp)
            .clip(shape)
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
                    drawArc(
                        c, startAngle = -90f + i * sweep, sweepAngle = sweep, useCenter = true,
                        topLeft = Offset(-size.width * 0.25f, -size.height * 0.25f),
                        size = Size(size.width * 1.5f, size.height * 1.5f),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = selected && palette != null,
            enter = scaleIn(Motion.bouncy()) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(20.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
