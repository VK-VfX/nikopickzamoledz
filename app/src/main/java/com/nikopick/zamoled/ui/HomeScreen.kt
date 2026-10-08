package com.nikopick.zamoled.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikopick.zamoled.data.FavoritesStore
import com.nikopick.zamoled.data.Screen
import com.nikopick.zamoled.gen.Category
import com.nikopick.zamoled.gen.Styles
import com.nikopick.zamoled.gen.WallpaperSpec
import kotlin.random.Random

private const val TAB_ALL = "all"
private const val TAB_FAV = "fav"
private const val PAGE = 30
private const val MAX_PAGES = 20

private data class Tab(val id: String, val label: String, val icon: ImageVector)

private val tabs: List<Tab> = listOf(
    Tab(TAB_ALL, "All", Icons.Rounded.GridView),
    Tab(TAB_FAV, "Favorites", Icons.Rounded.Favorite),
) + Category.entries.map { Tab(it.name, it.label, it.icon) }

val Category.icon: ImageVector
    get() = when (this) {
        Category.DOODLE -> Icons.Rounded.Gesture
        Category.MINIMAL -> Icons.Rounded.RadioButtonUnchecked
        Category.GEOMETRIC -> Icons.Rounded.Category
        Category.SPACE -> Icons.Rounded.NightsStay
        Category.NATURE -> Icons.Rounded.Landscape
        Category.ABSTRACT -> Icons.Rounded.BubbleChart
        Category.NEON -> Icons.Rounded.Bolt
    }

private fun buildSpecs(tab: String, batch: Long, count: Int): List<WallpaperSpec> {
    val styles = if (tab == TAB_ALL) Styles.all else Styles.of(Category.valueOf(tab))
    val rnd = Random(batch xor tab.hashCode().toLong())
    return List(count) { i ->
        val style = if (tab == TAB_ALL) styles[rnd.nextInt(styles.size)] else styles[i % styles.size]
        WallpaperSpec(style.id, rnd.nextLong())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(favorites: FavoritesStore, onOpen: (WallpaperSpec) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(TAB_ALL) }
    var batch by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var pages by remember(tab, batch) { mutableIntStateOf(1) }
    val favKeys = favorites.keys
    val specs = remember(tab, batch, pages, favKeys) {
        if (tab == TAB_FAV) favorites.specs else buildSpecs(tab, batch, pages * PAGE)
    }
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(gridState, specs.size, tab) {
        if (tab == TAB_FAV) return@LaunchedEffect
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last > specs.size - 12 && pages < MAX_PAGES) pages++ }
    }
    LaunchedEffect(tab, batch) { gridState.scrollToItem(0) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Black,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Zamoled", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Pitch-black wallpapers, generated on your phone",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { batch = System.currentTimeMillis() }) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = "New batch")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Black,
                    scrolledContainerColor = Color.Black,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val style = Styles.all.random()
                    onOpen(WallpaperSpec(style.id, Random.nextLong()))
                },
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                text = { Text("Surprise me") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tabs, key = { it.id }) { t ->
                    FilterChip(
                        selected = tab == t.id,
                        onClick = { tab = t.id },
                        label = { Text(t.label) },
                        leadingIcon = { Icon(t.icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                    )
                }
            }
            if (tab == TAB_FAV && specs.isEmpty()) {
                EmptyFavorites()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(112.dp),
                    state = gridState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(specs, key = { it.key }) { spec ->
                        WallpaperCard(spec, favorite = spec.key in favKeys, onClick = { onOpen(spec) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperCard(spec: WallpaperSpec, favorite: Boolean, onClick: () -> Unit) {
    val bitmap by rememberThumbnail(spec)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().aspectRatio(Screen.aspect),
    ) {
        Box(Modifier.fillMaxSize()) {
            Crossfade(targetState = bitmap, label = "thumb") { b ->
                if (b != null) {
                    Image(
                        bitmap = b.asImageBitmap(),
                        contentDescription = spec.style.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize())
                }
            }
            if (favorite) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(16.dp),
                )
            }
            Text(
                spec.style.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
            )
        }
    }
}

@Composable
private fun EmptyFavorites() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(
            Icons.Rounded.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text("No favorites yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Open a wallpaper and tap the heart to keep it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
