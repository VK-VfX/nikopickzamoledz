package com.nikopick.zamoled.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.nikopick.zamoled.data.UpdateInfo
import com.nikopick.zamoled.data.Updates
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import com.nikopick.zamoled.data.AUTO_SOURCE_ALL
import com.nikopick.zamoled.data.AUTO_SOURCE_FAVORITES
import com.nikopick.zamoled.data.AutoWallpaper
import com.nikopick.zamoled.data.Prefs
import com.nikopick.zamoled.data.ShareLink
import com.nikopick.zamoled.data.WallpaperTarget
import com.nikopick.zamoled.gen.Style
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
private const val COVER_SEED = -7046029254386353131L + 12345L
/** The feed is finite: this many wallpapers per batch, and Shuffle brings a fresh batch. */
private const val FEED_SIZE = 24

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
    val ota: OtaState,
) {
    var showSettings by mutableStateOf(false)
    var tab by mutableStateOf(HomeTab.EXPLORE)
    var filter by mutableStateOf(FILTER_ALL)
    var subcategory by mutableStateOf<String?>(null)

    /** When set, the feed shows only this style: its own page of variations. */
    var styleId by mutableStateOf<String?>(null)
    var searching by mutableStateOf(false)
    var query by mutableStateOf("")
    var showAuto by mutableStateOf(false)

    fun showStyle(id: String) {
        styleId = id
        searching = false
        query = ""
        tab = HomeTab.EXPLORE
        batch = System.currentTimeMillis()
    }
    var batch by mutableLongStateOf(System.currentTimeMillis())
}

@Composable
fun rememberHomeState(prefs: Prefs): HomeState {
    val explore = rememberLazyGridState()
    val categories = rememberLazyGridState()
    val favorites = rememberLazyGridState()
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val ota = remember { OtaState(context, scope, prefs) }
    val state = remember { HomeState(explore, categories, favorites, ota) }
    LaunchedEffect(Unit) { if (prefs.autoUpdate) ota.check() }
    return state
}

/** Styles whose name, world or sub-category contains [query]. */
private fun searchStyles(query: String): List<Style> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return Styles.all.filter {
        it.name.lowercase().contains(q) || it.category.label.lowercase().contains(q) || Styles.subcategoryOf(it).lowercase().contains(q)
    }
}

/**
 * One batch of wallpapers. A style page shows only that style; a search shows its matches. With
 * no filter every card is a different style, starting with the worlds picked at first launch;
 * inside a category the styles repeat only as often as they have to to fill the batch.
 */
private fun buildSpecs(filter: String, subcategory: String?, batch: Long, styleId: String?, query: String, likes: Set<String>): List<WallpaperSpec> {
    val rnd = Random(batch xor filter.hashCode().toLong() xor subcategory.hashCode().toLong() xor styleId.hashCode().toLong())
    if (styleId != null) return List(FEED_SIZE) { WallpaperSpec(styleId, rnd.nextLong()) }
    if (query.isNotBlank()) {
        val found = searchStyles(query)
        if (found.isEmpty()) return emptyList()
        val per = if (found.size >= 8) 2 else if (found.size >= 4) 3 else 6
        return found.take(FEED_SIZE).flatMap { style -> List(per) { WallpaperSpec(style.id, rnd.nextLong()) } }
    }
    val styles = if (filter == FILTER_ALL) Styles.all else Styles.of(Category.valueOf(filter), subcategory)
    if (styles.isEmpty()) return emptyList()
    val shuffled = styles.shuffled(rnd)
    // Liked worlds fill the first two thirds of an unfiltered feed; the rest stays a surprise.
    val order = if (filter == FILTER_ALL && likes.isNotEmpty()) {
        val liked = shuffled.filter { it.category.name in likes }.take(FEED_SIZE * 2 / 3)
        (liked + shuffled.filter { it !in liked }).let { mixed -> mixed.take(FEED_SIZE).shuffled(rnd) + mixed.drop(FEED_SIZE) }
    } else {
        shuffled
    }
    val count = if (filter == FILTER_ALL) minOf(FEED_SIZE, order.size) else minOf(FEED_SIZE, maxOf(order.size * 3, 6))
    return List(count) { i -> WallpaperSpec(order[i % order.size].id, rnd.nextLong()) }
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
fun HomeScreen(state: HomeState, favorites: FavoritesStore, prefs: Prefs, onOpen: (WallpaperSpec) -> Unit) {
    val fabExpanded by remember { derivedStateOf { state.exploreGrid.firstVisibleItemIndex < 3 } }
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Black,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // The dock floats over the feed rather than pushing it up.
                Box(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 4.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FloatingDock(state.tab, onTab = { state.tab = it }, favoriteCount = favorites.keys.size)
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
                        shape = LeafSm,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            },
        ) { _ ->
            AnimatedContent(
                targetState = state.tab,
                transitionSpec = {
                    // "Fade through": the old tab leaves quickly, the new one eases in.
                    (fadeIn(tween(220, delayMillis = 80)) + scaleIn(tween(220, delayMillis = 80), initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(80)))
                },
                label = "tabs",
            ) { tab ->
                when (tab) {
                    HomeTab.EXPLORE -> ExploreTab(state, favorites, prefs, onOpen)
                    HomeTab.CATEGORIES -> CategoriesTab(state) { category ->
                        state.filter = category.name
                        state.subcategory = null
                        state.styleId = null
                        state.query = ""
                        state.searching = false
                        state.tab = HomeTab.EXPLORE
                    }
                    HomeTab.FAVORITES -> FavoritesTab(state, favorites, onOpen)
                }
            }
        }
        AnimatedVisibility(
            visible = state.showSettings,
            enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 14 },
            exit = fadeOut(tween(160)),
        ) {
            SettingsScreen(state.ota, prefs, onBack = { state.showSettings = false }, onDaily = { state.showAuto = true })
        }
        if (state.showAuto) {
            AutoWallpaperSheet(prefs, favoriteCount = favorites.keys.size, onDismiss = { state.showAuto = false }) { enabled, source, target ->
                prefs.setAuto(enabled, source, target)
                AutoWallpaper.schedule(context, enabled)
            }
        }
    }
}

// ---------- Explore ----------

@Composable
private fun ExploreTab(state: HomeState, favorites: FavoritesStore, prefs: Prefs, onOpen: (WallpaperSpec) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val specs = remember(state.filter, state.subcategory, state.batch, state.styleId, state.query, prefs.likes) {
        buildSpecs(state.filter, state.subcategory, state.batch, state.styleId, state.query, prefs.likes)
    }
    val focused = state.styleId != null || state.query.isNotBlank()
    val shared = remember(state.query) { ShareLink.parse(state.query) }
    val recent = remember(prefs.history) { prefs.history.mapNotNull { WallpaperSpec.fromKey(it) } }
    val featured = remember(state.batch) { featuredSpecs(state.batch) }
    val showFeatured = state.filter == FILTER_ALL && !focused && !state.searching
    val category = if (state.filter == FILTER_ALL) null else Category.valueOf(state.filter)
    val subcategories = remember(state.filter) { category?.let { Styles.subcategories(it) }.orEmpty() }
    val showSubs = subcategories.size > 1 && !focused
    val grid = state.exploreGrid
    // Drawing ahead waits for the feed to stop moving, then fills in the rest of the batch quietly.
    LaunchedEffect(grid) { snapshotFlow { grid.isScrollInProgress }.collect { ThumbCache.scrolling = it } }
    LaunchedEffect(specs) {
        delay(700)
        ThumbCache.prefetch(specs)
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
            Column {
                HeroHeader(
                    updateReady = state.ota.showBanner,
                    onShuffle = { state.batch = System.currentTimeMillis() },
                    onSearch = {
                        state.searching = !state.searching
                        if (!state.searching) state.query = ""
                    },
                    onSettings = { state.showSettings = true },
                )
                UpdateBanner(state.ota)
                AnimatedVisibility(
                    visible = state.searching,
                    enter = expandVertically(Motion.spatial()) + fadeIn(),
                    exit = shrinkVertically(Motion.spatial()) + fadeOut(),
                ) {
                    SearchField(
                        query = state.query,
                        onQuery = {
                            state.query = it
                            state.styleId = null
                        },
                        onClose = {
                            state.searching = false
                            state.query = ""
                        },
                    )
                }
                if (shared != null) {
                    Button(onClick = { onOpen(shared) }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text("Open shared wallpaper: ${shared.style.name}")
                    }
                }
                state.styleId?.let { id ->
                    StylePageHeader(Styles.byId(id), onClose = { state.styleId = null })
                }
                if (state.query.isNotBlank() && shared == null) {
                    Text(
                        if (specs.isEmpty()) "Nothing matches \"${state.query.trim()}\". Try a world like Space, or a style like Aura." else "${specs.size} wallpapers match",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                    )
                }
            }
        }
        if (showFeatured && recent.isNotEmpty()) {
            item(key = "recent", span = { GridItemSpan(maxLineSpan) }) {
                RecentRow(recent, onOpen)
            }
        }
        if (showFeatured) {
            item(key = "featured", span = { GridItemSpan(maxLineSpan) }) {
                FeaturedCarousel(featured, onOpen)
            }
        }
        if (!focused) item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            FilterRow(selected = state.filter) { f ->
                if (f != state.filter) {
                    state.filter = f
                    state.subcategory = null
                    scope.launch { if (grid.firstVisibleItemIndex > 1) grid.animateScrollToItem(if (f == FILTER_ALL) 2 else 1) }
                }
            }
        }
        if (showSubs && category != null) {
            item(key = "subfilters", span = { GridItemSpan(maxLineSpan) }) {
                SubFilterRow(category, subcategories, state.subcategory) { s ->
                    if (s != state.subcategory) {
                        state.subcategory = s
                    }
                }
            }
        }
        itemsIndexed(specs, key = { _, s -> s.key }) { i, spec ->
            WallpaperCard(
                spec = spec,
                index = i,
                favorite = spec.key in favorites.keys,
                onClick = { onOpen(spec) },
            )
        }
        if (specs.isNotEmpty()) item(key = "feed-end", span = { GridItemSpan(maxLineSpan) }) {
            FeedEnd(count = specs.size) {
                state.batch = System.currentTimeMillis()
                scope.launch { grid.animateScrollToItem(0) }
            }
        }
    }
}

/** The search box. It also accepts a pasted share link or code. */
@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        placeholder = { Text("Search styles and worlds, or paste a shared link") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close search") } },
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}

/** The heading of a style's own page. */
@Composable
private fun StylePageHeader(style: Style, onClose: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)) {
            Icon(style.category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(style.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${style.category.label} · every card is a different variation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Leave this style") }
        }
    }
}

/** Wallpapers opened lately, newest first, so one scrolled past is not lost. */
@Composable
private fun RecentRow(specs: List<WallpaperSpec>, onOpen: (WallpaperSpec) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        SectionTitle("Recently viewed")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
            items(specs, key = { it.key }) { spec ->
                val bitmap by rememberThumbnail(spec)
                Card(
                    onClick = { onOpen(spec) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.width(84.dp).aspectRatio(Screen.aspect),
                ) {
                    val b = bitmap
                    if (b != null) {
                        Image(remember(b) { b.asImageBitmap() }, contentDescription = spec.style.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize().shimmer())
                    }
                }
            }
        }
    }
}

/** Settings for the wallpaper that changes by itself once a day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoWallpaperSheet(
    prefs: Prefs,
    favoriteCount: Int,
    onDismiss: () -> Unit,
    onChange: (Boolean, String, WallpaperTarget) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Daily wallpaper", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "A new wallpaper about once a day, set in the background.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = prefs.autoEnabled, onCheckedChange = { onChange(it, prefs.autoSource, prefs.autoTarget) })
            }
            Text("Pick from", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val sources = listOf(AUTO_SOURCE_ALL to "Everything", AUTO_SOURCE_FAVORITES to "Favorites ($favoriteCount)") +
                Styles.categories.map { it.name to it.label }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sources, key = { it.first }) { (id, label) ->
                    FilterChip(
                        selected = prefs.autoSource == id,
                        onClick = { onChange(prefs.autoEnabled, id, prefs.autoTarget) },
                        label = { Text(label) },
                        shape = CircleShape,
                    )
                }
            }
            Text("Set on", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WallpaperTarget.entries.forEach { target ->
                    FilterChip(
                        selected = prefs.autoTarget == target,
                        onClick = { onChange(prefs.autoEnabled, prefs.autoSource, target) },
                        label = { Text(if (target == WallpaperTarget.BOTH) "Both" else target.label) },
                        shape = CircleShape,
                    )
                }
            }
            Text(
                "Android decides the exact time, usually while the phone is idle. Favorites falls back to everything while you have none saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.navigationBarsPadding().height(12.dp))
        }
    }
}

/** The end of a batch: shuffle for a fresh one instead of scrolling for more. */
@Composable
private fun FeedEnd(count: Int, onShuffle: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
    ) {
        MonoLabel("That is $count")
        OutlinedButton(onClick = onShuffle, shape = LeafSm) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Shuffle for a new set")
        }
    }
}

/** A small rounded label for a headline number. */
@Composable
private fun StatPill(text: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun HeroHeader(updateReady: Boolean, onShuffle: () -> Unit, onSearch: () -> Unit, onSettings: () -> Unit) {
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(0f) }
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                MonoLabel(greeting())
                Wordmark()
            }
            IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, contentDescription = "Search") }
            IconButton(onClick = {
                onShuffle()
                scope.launch { spin.animateTo(spin.value + 360f, Motion.bouncy()) }
            }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "New batch", modifier = Modifier.graphicsLayer { rotationZ = spin.value })
            }
            IconButton(onClick = onSettings) {
                BadgedBox(badge = { if (updateReady) Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings and updates")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
            MonoLabel("${Styles.all.size} wallpapers")
            MonoLabel("/", color = MaterialTheme.colorScheme.primary)
            MonoLabel("${Styles.categories.size} worlds")
            MonoLabel("/", color = MaterialTheme.colorScheme.primary)
            MonoLabel("pitch black")
        }
        Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(1.dp).background(Hairline))
    }
}

@Composable
private fun UpdateBanner(ota: OtaState) {
    // Keep the last value so the exit animation still has text to show.
    var shown by remember { mutableStateOf<UpdateInfo?>(null) }
    if (ota.info != null) shown = ota.info
    AnimatedVisibility(
        visible = ota.showBanner,
        enter = expandVertically(Motion.spatial()) + fadeIn(),
        exit = shrinkVertically(Motion.spatial()) + fadeOut(),
    ) {
        val info = shown ?: return@AnimatedVisibility
        LeafCard(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    MonoLabel("Update  ${Updates.installedVersion} → ${info.version}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp).weight(1f))
                    IconButton(onClick = { ota.dismissed = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                    }
                }
                Text("A new version is ready", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Download opens it in your browser; tap the finished file to install it over this version. Your favorites stay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { ota.download() }) { Text("Download" + if (info.size > 0) "  ·  %.1f MB".format(info.size / 1_048_576f) else "") }
                    TextButton(onClick = { ota.dismissed = true }) { Text("Not now") }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.padding(start = 2.dp)) {
        Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Text(text, style = MaterialTheme.typography.titleLarge)
        Box(Modifier.weight(1f).height(1.dp).background(Hairline))
    }
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
    val shape = Leaf
    val palette = spec.palette
    Card(
        onClick = onClick,
        shape = shape,
        interactionSource = interaction,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = hairline(0.12f),
        modifier = modifier.fillMaxWidth().height(420.dp).pressScale(interaction, 0.96f),
    ) {
        Box(Modifier.fillMaxSize().sharedWallpaper(spec.key, shape)) {
            ThumbImage(bitmap, spec.style.name, Modifier.fillMaxSize())
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
                MonoLabel(spec.style.category.label, color = MaterialTheme.colorScheme.primary)
                Text(spec.style.name, style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    palette.colors.forEach { c -> Box(Modifier.size(12.dp).background(Color(c), CircleShape)) }
                }
            }
        }
    }
}

/** Sub-categories inside the chosen category: a second, narrower pass over the same feed. */
@Composable
private fun SubFilterRow(category: Category, subcategories: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        MonoLabel("In ${category.label}", modifier = Modifier.padding(start = 2.dp))
        NoirPills(listOf<Pair<String?, String>>(null to "Everything") + subcategories.map { it to it }, selected, onSelect)
    }
}

@Composable
private fun FilterRow(selected: String, onSelect: (String) -> Unit) {
    val items = listOf(FILTER_ALL to "All") + Styles.categories.map { it.name to it.label }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        SectionHead("01", "Browse")
        NoirTabs(items, selected, onSelect)
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
    val shape = LeafSm
    Card(
        onClick = onClick,
        shape = shape,
        interactionSource = interaction,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = hairline(0.1f),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(Screen.aspect)
            .enterOnce(spec.key, index)
            .pressScale(interaction),
    ) {
        Box(Modifier.fillMaxSize().sharedWallpaper(spec.key, shape)) {
            ThumbImage(bitmap, spec.style.name, Modifier.fillMaxSize())
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))),
            )
            MonoLabel(
                spec.style.name,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 11.dp),
            )
            MonoLabel(
                "%02d".format(index + 1),
                color = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 10.dp),
            )
            androidx.compose.animation.AnimatedVisibility(
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
                MonoLabel("${Styles.categories.size} worlds / ${Styles.all.size} wallpapers")
                Text("Categories", style = MaterialTheme.typography.displaySmall)
            }
        }
        itemsIndexed(Styles.categories, key = { _, c -> c.name }) { i, category ->
            CategoryCard(category, i) { onPick(category) }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, index: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val styles = Styles.of(category)
    // A fixed seed that was checked on every world's first style, so no card comes up nearly empty.
    val sample = remember(category) { WallpaperSpec(styles[0].id, COVER_SEED) }
    val bitmap by rememberThumbnail(sample)
    val containers = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val (container, onContainer) = containers[index % containers.size]
    Card(
        onClick = onClick,
        shape = Leaf,
        interactionSource = interaction,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = hairline(0.1f),
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .enterOnce("cat-${category.name}", index)
            .pressScale(interaction, 0.95f),
    ) {
        Box(Modifier.fillMaxSize()) {
            ThumbImage(bitmap, null, Modifier.fillMaxSize(), alpha = 0.75f)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.85f)))),
            )
            Column(Modifier.fillMaxHeight().padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("%02d".format(index + 1), style = MaterialTheme.typography.displaySmall, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.weight(1f))
                    ShapeIcon(category, active = pressed, size = 40.dp, container = container, content = onContainer, spin = false)
                }
                Spacer(Modifier.weight(1f))
                Text(category.label, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                MonoLabel("${styles.size} wallpapers", color = Color.White.copy(alpha = 0.55f))
                Text(
                    Styles.subcategories(category).joinToString(" · "),
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
                MonoLabel("${specs.size} saved")
                Text("Favorites", style = MaterialTheme.typography.displaySmall)
            }
        }
        itemsIndexed(specs, key = { _, s -> s.key }) { i, spec ->
            WallpaperCard(spec, i, favorite = true, onClick = { onOpen(spec) })
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
