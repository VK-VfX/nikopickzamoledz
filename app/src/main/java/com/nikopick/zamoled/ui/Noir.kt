package com.nikopick.zamoled.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The look of the app: pure black, hairlines instead of fills, condensed display type over small
 * monospaced captions, and one shape used everywhere: a "leaf" with two sharp corners.
 */

/** Large cards: soft on one diagonal, nearly square on the other. */
val Leaf = RoundedCornerShape(topStart = 30.dp, topEnd = 7.dp, bottomEnd = 30.dp, bottomStart = 7.dp)

/** Thumbnails and small cards. */
val LeafSm = RoundedCornerShape(topStart = 20.dp, topEnd = 5.dp, bottomEnd = 20.dp, bottomStart = 5.dp)

val Hairline: Color = Color.White.copy(alpha = 0.12f)

fun hairline(alpha: Float = 0.12f) = BorderStroke(1.dp, Color.White.copy(alpha = alpha))

/** A small monospaced caption in capitals. */
@Composable
fun MonoLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines: Int = 1) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** The name of the app, with a single accent dot. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier) {
        Text("zamoled", style = MaterialTheme.typography.displayMedium, color = Color.White)
        Box(
            Modifier
                .padding(start = 3.dp, bottom = 9.dp)
                .size(10.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}

/** A numbered section heading: "01  Browse". */
@Composable
fun SectionHead(index: String, title: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.padding(start = 2.dp)) {
        MonoLabel(index, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Box(Modifier.height(1.dp).fillMaxWidth().background(Hairline))
    }
}


/** Text tabs with an underline that grows under the one picked. */
@Composable
fun NoirTabs(items: List<Pair<String, String>>, selected: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(items, key = { it.first }) { (id, label) ->
            val on = id == selected
            val line by animateDpAsState(if (on) 22.dp else 0.dp, Motion.bouncy(), label = "tab")
            Column(
                Modifier
                    .clickable(interactionSource = MutableInteractionSource(), indication = null) { onSelect(id) }
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
                Box(Modifier.padding(top = 4.dp).height(2.dp).width(line).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}

/** Small outlined pills for sub-categories. */
@Composable
fun NoirPills(items: List<Pair<String?, String>>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(items, key = { it.first ?: "__all" }) { (id, label) ->
            val on = id == selected
            Surface(
                onClick = { onSelect(id) },
                shape = CircleShape,
                color = if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                border = BorderStroke(1.dp, if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.14f)),
            ) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** The navigation: a floating capsule where the picked tab opens to show its name. */
@Composable
fun FloatingDock(tab: HomeTab, onTab: (HomeTab) -> Unit, favoriteCount: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF0C0C0F).copy(alpha = 0.94f),
        border = hairline(0.14f),
        shadowElevation = 12.dp,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(6.dp)) {
            HomeTab.entries.forEach { t ->
                val on = t == tab
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .animateContentSize(Motion.spatial())
                        .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent, CircleShape)
                        .clickable(interactionSource = MutableInteractionSource(), indication = null) { onTab(t) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    val icon = if (on) t.selectedIcon else t.icon
                    if (t == HomeTab.FAVORITES && favoriteCount > 0) {
                        BadgedBox(badge = { Badge { Text("$favoriteCount") } }) {
                            Icon(icon, contentDescription = t.label, tint = if (on) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f))
                        }
                    } else {
                        Icon(icon, contentDescription = t.label, tint = if (on) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f))
                    }
                    if (on) Text(t.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** A card that is only a hairline outline and a leaf shape. */
@Composable
fun LeafCard(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = Leaf, content: @Composable () -> Unit) {
    Surface(shape = shape, color = Color(0xFF08080A), border = hairline(0.1f), modifier = modifier) { content() }
}

/** Quick settings for a slider: tap a name instead of dragging. */
@Composable
fun PresetRow(items: List<Pair<String, Float>>, value: Float, onPick: (Float) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        items.forEach { (label, v) ->
            val on = kotlin.math.abs(value - v) < 0.07f
            Surface(
                onClick = { onPick(v) },
                shape = CircleShape,
                color = if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                border = BorderStroke(1.dp, if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 9.dp).fillMaxWidth(),
                )
            }
        }
    }
}
