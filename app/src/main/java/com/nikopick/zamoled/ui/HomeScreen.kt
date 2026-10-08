package com.nikopick.zamoled.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikopick.zamoled.data.FavoritesStore
import com.nikopick.zamoled.data.Screen
import com.nikopick.zamoled.gen.Category
import com.nikopick.zamoled.gen.Palettes
import com.nikopick.zamoled.gen.Styles
import com.nikopick.zamoled.gen.WallpaperSpec
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.random.Random

private const val FILTER_ALL = "all"
private const val PAGE = 30
private const val MAX_PAGES = 20

enum class HomeTab(val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    EXPLORE("Explore", Icons.Rounded.Explore, Icons.Outlined.Explore),
    CATEGORIES("Categories", Icons.Rounded.Category, Icons.Outlined.Category),
    FAVORITES("Favorites", Icons.Rounded.Favorite, Icons.Rounded.FavoriteBorder),
}

/** Home state lives above the screen switch so scroll positions survive opening a wallpaper. */
@Stable
class HomeState(
    val exploreGrid: LazyGridState,
    val categoryGrid: LazyGridState,
    val favoriteGrid: LazyGridState,
) {
    var tab by mutableStateOf(HomeTab.EXPLORE)
    var filter by mutableStateOf(FILTER_ALL)
    var batch by mutableLongStateOf(System.currentTimeMillis())
    var pages by mutableIntStateOf(1)
}

@Composable
fun rememberHomeState(): HomeState {
    val explore = rememberLazyGridState()
    val categories = rememberLazyGridState()
    val favorites = rememberLazyGridState()
    return remember { HomeState(explore, categories, favorites) }
}

private fun buildSpecs(filter: String, batch: Long, count: Int): List<WallpaperSpec> {
    val styles = if (filter == FILTER_ALL) Styles.all else Styles.of(Category.valueOf(filter))
    val rnd = Random(batch xor filter.hashCode().toLong())
    return List(count) { i ->
        val style = if (filter == FILTER_ALL) styles[rnd.nextInt(styles.size)] else styles[i % styles.size]
        WallpaperSpec(style.id, rnd.nextLong())
    }
}

private fun featuredSpecs(batch: Long): List<WallpaperSpec> {
    val rnd = Random(batch * 31 + 7)
    return Styles.all.shuffled(rnd).take(6).map { WallpaperSpec(it.id, rnd.nextLong()) }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun topInset() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
fun HomeScreen(state: HomeState, favorites: FavoritesStore, onOpen: (WallpaperSpec) -> Unit) {
    val fabExpanded by remember { derivedStateOf { state.exploreGrid.firstVisibleItemIndex < 3 } }
    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                HomeTab.entries.forEach { t ->
                    val selected = state.tab == t
                    NavigationBarItem(
                        selected = selected,
                        onClick = { state.tab = t },
                        icon = {
                            val icon = if (selected) t.selectedIcon else t.icon
                            val count = favorites.keys.size
                            if (t == HomeTab.FAVORITES && count > 0) {
                                BadgedBox(badge = { Badge { Text("$count") } }) { Icon(icon, contentDescription = null) }
                            } else {
                                Icon(icon, contentDescription = null)
                            }
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.tab == HomeTab.EXPLORE,
                enter = scaleIn(Motion.bouncy()) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onOpen(WallpaperSpec(Styles.all.random().id, Random.nextLong())) },
                    expanded = fabExpanded,
                    icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                    text = { Text("Surprise me") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = state.tab,
            transitionSpec = {
                // Material "fade through": outgoing fades fast, incoming fades and grows in.
                (fadeIn(tween(240, delayMillis = 90)) + scaleIn(tween(240, delayMillis = 90), initialScale = 0.94f))
                    .togetherWith(fadeOut(tween(90)))
            },
            label = "tabs",
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) { tab ->
            when (tab) {
                HomeTab.EXPLORE -> ExploreTab(state, favorites, onOpen)
                HomeTab.CATEGORIES -> CategoriesTab(state) { category ->
                    state.filter = category.name
                    state.pages = 1
                    state.tab = HomeTab.EXPLORE
                }
                HomeTab.FAVORITES -> FavoritesTab(state, favorites, onOpen)
            }
        }
    }
}

// ---------- Explore ----------

@Composable
private fun ExploreTab(state: HomeState, favorites: FavoritesStore, onOpen: (WallpaperSpec) -> Unit) {
    val scope = rememberCoroutineScope()
    val specs = remember(state.filter, state.batch, state.pages) { buildSpecs(state.filter, state.batch, state.pages * PAGE) }
    val featured = remember(state.batch) { featuredSpecs(state.batch) }
    val showFeatured = state.filter == FILTER_ALL
    val headerCount = if (showFeatured) 3 else 2
    val grid = state.exploreGrid

    LaunchedEffect(specs.size, headerCount) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last > headerCount + specs.size - 12 && state.pages < MAX_PAGES) state.pages++ }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(112.dp),
        state = grid,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset() + 12.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            HeroHeader(onShuffle = {
                state.batch = System.currentTimeMillis()
                state.pages = 1
            })
        }
        if (showFeatured) {
            item(key = "featured", span = { GridItemSpan(maxLineSpan) }) {
                FeaturedCarousel(featured, onOpen)
            }
        }
        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            FilterRow(selected = state.filter) { f ->
                if (f != state.filter) {
                    state.filter = f
                    state.pages = 1
                    scope.launch { if (grid.firstVisibleItemIndex > 1) grid.animateScrollToItem(if (f == FILTER_ALL) 2 else 1) }
                }
            }
        }
        itemsIndexed(specs, key = { _, s -> s.key }) { i, spec ->
            WallpaperCard(
                spec = spec,
                index = i,
                favorite = spec.key in favorites.keys,
                onClick = { onOpen(spec) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun HeroHeader(onShuffle: () -> Unit) {
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(0f) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${greeting()} · ${Styles.all.size} styles",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GradientText("Zamoled", style = MaterialTheme.typography.displaySmall)
            Text(
                "Pitch-black wallpapers, drawn on your phone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(
            onClick = {
                onShuffle()
                scope.launch { spin.animateTo(spin.value + 360f, Motion.bouncy()) }
            },
            modifier = Modifier.size(52.dp),
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "New batch",
                modifier = Modifier.graphicsLayer { rotationZ = spin.value },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.padding(start = 4.dp))
}

@Composable
private fun FeaturedCarousel(specs: List<WallpaperSpec>, onOpen: (WallpaperSpec) -> Unit) {
    val pager = rememberPagerState { specs.size }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        SectionTitle("Today's picks")
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 36.dp),
            pageSpacing = 12.dp,
            key = { specs[it].key },
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val spec = specs[page]
            FeaturedCard(
                spec = spec,
                onClick = { onOpen(spec) },
                modifier = Modifier.graphicsLayer {
                    val o = abs((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                    val s = lerpF(1f, 0.86f, o)
                    scaleX = s
                    scaleY = s
                    alpha = lerpF(1f, 0.5f, o)
                },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(specs.size) { i ->
                val selected = pager.currentPage == i
                val width by animateDpAsState(if (selected) 24.dp else 8.dp, Motion.bouncy(), label = "dot")
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    label = "dotColor",
                )
                Box(Modifier.height(8.dp).width(width).background(color, CircleShape))
            }
        }
    }
}

@Composable
private fun FeaturedCard(spec: WallpaperSpec, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val bitmap by rememberThumbnail(spec, ThumbCache.LARGE)
    val shape = RoundedCornerShape(32.dp)
    val palette = Palettes.resolve(spec.paletteId, spec.seed)
    Card(
        onClick = onClick,
        shape = shape,
        interactionSource = interaction,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth().height(420.dp).pressScale(interaction, 0.96f),
    ) {
        Box(Modifier.fillMaxSize().sharedWallpaper(spec.key, shape)) {
            Crossfade(targetState = bitmap, label = "featured") { b ->
                if (b != null) {
                    Image(b.asImageBitmap(), contentDescription = spec.style.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().shimmer())
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(spec.style.category.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(spec.style.category.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(spec.style.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    palette.colors.forEach { c -> Box(Modifier.size(12.dp).background(Color(c), CircleShape)) }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: String, onSelect: (String) -> Unit) {
    val filters = listOf(Triple(FILTER_ALL, "All", Icons.Rounded.GridView)) +
        Category.entries.map { Triple(it.name, it.label, it.icon) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        SectionTitle("Browse")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(filters, key = { it.first }) { (id, label, icon) ->
                val isSelected = selected == id
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(id) },
                    label = { Text(label) },
                    leadingIcon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
fun WallpaperCard(
    spec: WallpaperSpec,
    index: Int,
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val bitmap by rememberThumbnail(spec)
    val shape = RoundedCornerShape(22.dp)
    Card(
        onClick = onClick,
        shape = shape,
        interactionSource = interaction,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(Screen.aspect)
            .enterOnce(spec.key, index)
            .pressScale(interaction),
    ) {
        Box(Modifier.fillMaxSize().sharedWallpaper(spec.key, shape)) {
            Crossfade(targetState = bitmap, label = "thumb") { b ->
                if (b != null) {
                    Image(b.asImageBitmap(), contentDescription = spec.style.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize().shimmer())
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))),
            )
            Text(
                spec.style.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
            )
            AnimatedVisibility(
                visible = favorite,
                enter = scaleIn(Motion.bouncy()) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(5.dp).size(12.dp),
                    )
                }
            }
        }
    }
}

// ---------- Categories ----------

@Composable
private fun CategoriesTab(state: HomeState, onPick: (Category) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        state = state.categoryGrid,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset() + 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                Text("Categories", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${Category.entries.size} worlds, ${Styles.all.size} generators. Tap one to browse it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(Category.entries, key = { _, c -> c.name }) { i, category ->
            CategoryCard(category, i) { onPick(category) }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, index: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val styles = Styles.of(category)
    val sample = remember(category) { WallpaperSpec(styles[0].id, category.ordinal * 7919L + 42L) }
    val bitmap by rememberThumbnail(sample)
    val containers = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val (container, onContainer) = containers[index % containers.size]
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        interactionSource = interaction,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .enterOnce("cat-${category.name}", index)
            .pressScale(interaction, 0.95f),
    ) {
        Box(Modifier.fillMaxSize()) {
            Crossfade(targetState = bitmap, label = "catThumb") { b ->
                if (b != null) {
                    Image(
                        b.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.75f },
                    )
                } else {
                    Box(Modifier.fillMaxSize().shimmer())
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.85f)))),
            )
            Column(Modifier.fillMaxHeight().padding(16.dp)) {
                ShapeIcon(category, active = pressed, size = 52.dp, container = container, content = onContainer)
                Spacer(Modifier.weight(1f))
                Text(category.label, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    styles.joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------- Favorites ----------

@Composable
private fun FavoritesTab(state: HomeState, favorites: FavoritesStore, onOpen: (WallpaperSpec) -> Unit) {
    val specs = remember(favorites.keys) { favorites.specs }
    if (specs.isEmpty()) {
        EmptyFavorites()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(112.dp),
        state = state.favoriteGrid,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset() + 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                Text("Favorites", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${specs.size} saved ${if (specs.size == 1) "wallpaper" else "wallpapers"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(specs, key = { _, s -> s.key }) { i, spec ->
            WallpaperCard(spec, i, favorite = true, onClick = { onOpen(spec) }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun EmptyFavorites() {
    val t = rememberInfiniteTransition(label = "float")
    val bob by t.animateFloat(-10f, 10f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bob")
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer { translationY = bob }
                .size(96.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(32.dp)),
        ) {
            Icon(
                Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(44.dp),
            )
        }
        Text("No favorites yet", style = MaterialTheme.typography.titleLarge)
        Text(
            "Open a wallpaper and tap the heart to keep it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
