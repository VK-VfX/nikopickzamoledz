package com.nikopick.zamoled.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nikopick.zamoled.data.FavoritesStore
import com.nikopick.zamoled.gen.WallpaperSpec

@Composable
fun ZamoledApp() {
    val context = LocalContext.current
    val favorites = remember { FavoritesStore(context.applicationContext) }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    // Keep the last opened spec around so the exit animation still has something to draw.
    var lastSpec by remember { mutableStateOf<WallpaperSpec?>(null) }
    val open = openKey?.let { WallpaperSpec.fromKey(it) }
    if (open != null) lastSpec = open

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HomeScreen(favorites = favorites, onOpen = { openKey = it.key })
        AnimatedVisibility(
            visible = open != null,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f),
        ) {
            lastSpec?.let { spec ->
                DetailScreen(
                    initial = spec,
                    favorites = favorites,
                    onBack = { openKey = null },
                )
            }
        }
    }
}
