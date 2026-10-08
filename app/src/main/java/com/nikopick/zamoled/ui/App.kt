package com.nikopick.zamoled.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ZamoledApp() {
    val context = LocalContext.current
    val favorites = remember { FavoritesStore(context.applicationContext) }
    val home = rememberHomeState()
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }

    SharedTransitionLayout(Modifier.fillMaxSize().background(Color.Black)) {
        AnimatedContent(
            targetState = openKey,
            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(320)) },
            label = "screen",
        ) { key ->
            CompositionLocalProvider(
                LocalSharedScope provides this@SharedTransitionLayout,
                LocalNavAnimScope provides this,
            ) {
                val spec = key?.let { WallpaperSpec.fromKey(it) }
                if (spec == null) {
                    HomeScreen(state = home, favorites = favorites, onOpen = { openKey = it.key })
                } else {
                    DetailScreen(initial = spec, favorites = favorites, onBack = { openKey = null })
                }
            }
        }
    }
}
